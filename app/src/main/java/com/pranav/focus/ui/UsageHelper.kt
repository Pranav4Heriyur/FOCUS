package com.pranav.focus.ui

import android.app.AppOpsManager
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

    /** Foreground time per app since midnight, launchable apps only, biggest first. */
    fun today(ctx: Context): List<AppUse> {
        val usm = ctx.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val stats = usm.queryAndAggregateUsageStats(cal.timeInMillis, System.currentTimeMillis())
        val pm = ctx.packageManager
        val home = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
        val labels = Allowed.launchableApps(ctx).associate { it.pkg to it.label }
        return stats.values
            .filter { it.totalTimeInForeground >= 60_000 && it.packageName in labels && it.packageName != home && it.packageName != ctx.packageName }
            .sortedByDescending { it.totalTimeInForeground }
            .map { AppUse(it.packageName, labels.getValue(it.packageName), it.totalTimeInForeground) }
    }

    fun format(ms: Long): String {
        val m = ms / 60_000
        return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
    }
}
