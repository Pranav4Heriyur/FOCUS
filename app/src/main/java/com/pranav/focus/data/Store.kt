package com.pranav.focus.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Caller(val name: String, val number: String)

class Store private constructor(ctx: Context) {
    private val app = ctx.applicationContext
    private val sp: SharedPreferences = app.getSharedPreferences("focus", Context.MODE_PRIVATE)

    // ---- session -------------------------------------------------------------
    val sessionActive: Boolean get() = sp.getBoolean("s_active", false)
    val sessionMinutes: Int get() = sp.getInt("s_minutes", 0)

    private fun bootCount(): Int = Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1)

    val sessionName: String get() = sp.getString("s_name", "") ?: ""
    val sessionSubject: String get() = sp.getString("s_subject", "Other") ?: "Other"

    fun startSession(minutes: Int, name: String, subject: String) {
        val ms = minutes * 60_000L
        sp.edit()
            .putBoolean("s_active", true)
            .putString("s_name", name)
            .putString("s_subject", subject)
            .putInt("s_minutes", minutes)
            .putLong("s_end_wall", System.currentTimeMillis() + ms)
            .putLong("s_end_elapsed", SystemClock.elapsedRealtime() + ms)
            .putInt("s_boot", bootCount())
            .commit()
    }

    fun endSession() {
        sp.edit().putBoolean("s_active", false).commit()
    }

    /** Monotonic clock within a boot (immune to clock changes); wall clock after a reboot. */
    fun remainingMs(): Long {
        if (!sessionActive) return 0
        val sameBoot = bootCount() == sp.getInt("s_boot", -2)
        val rem = if (sameBoot) sp.getLong("s_end_elapsed", 0) - SystemClock.elapsedRealtime()
        else sp.getLong("s_end_wall", 0) - System.currentTimeMillis()
        return rem.coerceAtLeast(0)
    }

    // ---- settings ------------------------------------------------------------
    var allowedApps: Set<String>
        get() = sp.getStringSet("allowed_apps", emptySet()) ?: emptySet()
        set(v) = sp.edit().putStringSet("allowed_apps", v.toSet()).apply()

    var callersOnly: Boolean
        get() = sp.getBoolean("callers_only", true)
        set(v) = sp.edit().putBoolean("callers_only", v).apply()

    var callers: List<Caller>
        get() {
            val raw = sp.getString("callers", "[]") ?: "[]"
            val arr = JSONArray(raw)
            return (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Caller(o.getString("name"), o.getString("number"))
            }
        }
        set(v) {
            val arr = JSONArray()
            v.forEach { arr.put(JSONObject().put("name", it.name).put("number", it.number)) }
            sp.edit().putString("callers", arr.toString()).apply()
        }

    var strictLock: Boolean
        get() = sp.getBoolean("strict_lock", true)
        set(v) = sp.edit().putBoolean("strict_lock", v).apply()

    /** Who is wearing the watch: true = me, false = someone else (data ignored). */
    var watchMe: Boolean
        get() = sp.getBoolean("watch_me", true)
        set(v) = sp.edit().putBoolean("watch_me", v).apply()

    var lastName: String
        get() = sp.getString("last_name", "") ?: ""
        set(v) = sp.edit().putString("last_name", v).apply()

    var lastSubject: String
        get() = sp.getString("last_subject", "Physics") ?: "Physics"
        set(v) = sp.edit().putString("last_subject", v).apply()

    /** Verified focus minutes completed today. */
    val todayFocusMinutes: Int
        get() = if (sp.getString("focus_day", "") == today()) sp.getInt("focus_today", 0) else 0

    var lastMinutes: Int
        get() = sp.getInt("last_minutes", 25)
        set(v) = sp.edit().putInt("last_minutes", v).apply()

    fun ensureDefaults(defaultPackages: Set<String>) {
        if (!sp.getBoolean("defaults_done", false)) {
            allowedApps = defaultPackages
            sp.edit().putBoolean("defaults_done", true).apply()
        }
    }

    // ---- progress ------------------------------------------------------------
    var xp: Int
        get() = sp.getInt("xp", 0)
        set(v) = sp.edit().putInt("xp", v).apply()

    var streak: Int
        get() = sp.getInt("streak", 0)
        set(v) = sp.edit().putInt("streak", v).apply()

    var totalFocusMinutes: Int
        get() = sp.getInt("total_minutes", 0)
        set(v) = sp.edit().putInt("total_minutes", v).apply()

    private fun today() = LocalDate.now().toString()

    /** Time Bank balance; expires at midnight. */
    val bankMinutes: Int
        get() = if (sp.getString("bank_day", "") == today()) sp.getInt("bank_min", 0) else 0

    fun addBank(minutes: Int) {
        val base = bankMinutes
        sp.edit().putString("bank_day", today()).putInt("bank_min", base + minutes).apply()
    }

    fun recordCompleted(minutes: Int): Int {
        val earned = TimeBank.earnedFromFocus(minutes)
        xp += Rewards.xpForSession(minutes)
        totalFocusMinutes += minutes
        sp.edit().putString("focus_day", today()).putInt("focus_today", todayFocusMinutes + minutes).apply()
        val last = sp.getString("streak_day", "")
        val t = today()
        if (last != t) {
            val yesterday = LocalDate.now().minusDays(1).toString()
            streak = if (last == yesterday) streak + 1 else 1
            sp.edit().putString("streak_day", t).apply()
        }
        addBank(earned)
        return earned
    }

    fun recordQuit() {
        xp = Rewards.xpAfterQuit(xp)
        streak = 0
        sp.edit().putString("streak_day", "").apply()
    }

    companion object {
        @Volatile private var inst: Store? = null
        fun get(ctx: Context): Store =
            inst ?: synchronized(this) { inst ?: Store(ctx).also { inst = it } }
    }
}
