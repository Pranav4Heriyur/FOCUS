package com.pranav.focus.ui

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import com.pranav.focus.lock.Allowed
import java.util.Calendar

data class AppUse(val pkg: String, val label: String, val ms: Long)

object UsageHelper {
    fun hasAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java) ?: return false
        return ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName) ==
            AppOpsManager.MODE_ALLOWED
    }

    fun midnight(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private var cacheAt = 0L
    private var cache: Map<String, Long> = emptyMap()

    /** Exact foreground time per package since midnight from resume/pause events, cached ~3 s. */
    fun usedTodayMs(ctx: Context, force: Boolean = false): Map<String, Long> {
        val now = System.currentTimeMillis()
        if (!force && now - cacheAt < 3_000) return cache
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val ev = usm.queryEvents(midnight(), now) ?: return emptyMap()
        val e = UsageEvents.Event()
        val open = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        var lastFg: String? = null
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            val p = e.packageName ?: continue
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> { open[p] = e.timeStamp; lastFg = p }
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED ->
                    open.remove(p)?.let { total[p] = (total[p] ?: 0L) + (e.timeStamp - it).coerceAtLeast(0) }
            }
        }
        lastFg?.let { p -> open[p]?.let { total[p] = (total[p] ?: 0L) + (now - it).coerceAtLeast(0) } }
        cache = total
        cacheAt = now
        return total
    }

    fun usedMin(ctx: Context, pkg: String): Double = (usedTodayMs(ctx)[pkg] ?: 0L) / 60_000.0

    /** Foreground time per launchable app since midnight, biggest first. */
    fun today(ctx: Context): List<AppUse> {
        val pm = ctx.packageManager
        val home = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
        val labels = Allowed.launchableApps(ctx).associate { it.pkg to it.label }
        return usedTodayMs(ctx, true).entries
            .filter { it.value >= 60_000 && it.key in labels && it.key != home && it.key != ctx.packageName }
            .sortedByDescending { it.value }
            .map { AppUse(it.key, labels.getValue(it.key), it.value) }
    }

    fun format(ms: Long): String {
        val m = ms / 60_000
        return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
    }
}
