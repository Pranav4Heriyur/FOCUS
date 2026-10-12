package com.pranav.focus.data

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** All app data lives in one private SharedPreferences file. Tokens are encrypted separately (see Secure). */
class Store private constructor(ctx: Context) {
    private val app = ctx.applicationContext
    val sp: SharedPreferences = app.getSharedPreferences("focus", Context.MODE_PRIVATE)

    // ---- tiny helpers -----------------------------------------------------------
    fun str(k: String, d: String = ""): String = sp.getString(k, d) ?: d
    fun putStr(k: String, v: String) = sp.edit().putString(k, v).apply()
    fun int(k: String, d: Int = 0): Int = sp.getInt(k, d)
    fun putInt(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    fun long(k: String, d: Long = 0): Long = sp.getLong(k, d)
    fun putLong(k: String, v: Long) = sp.edit().putLong(k, v).apply()
    fun bool(k: String, d: Boolean = false): Boolean = sp.getBoolean(k, d)
    fun putBool(k: String, v: Boolean) = sp.edit().putBoolean(k, v).apply()
    fun strSet(k: String): Set<String> = sp.getStringSet(k, emptySet()) ?: emptySet()
    fun putSet(k: String, v: Set<String>) = sp.edit().putStringSet(k, v.toSet()).apply()

    fun today(): String = LocalDate.now().toString()

    // ---- session -------------------------------------------------------------
    val sessionActive: Boolean get() = bool("s_active")
    val sessionMinutes: Int get() = int("s_minutes")
    val sessionName: String get() = str("s_name")
    val sessionSubject: String get() = str("s_subject", "Other")

    private fun bootCount(): Int = Settings.Global.getInt(app.contentResolver, Settings.Global.BOOT_COUNT, -1)

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
        val sameBoot = bootCount() == int("s_boot", -2)
        val rem = if (sameBoot) long("s_end_elapsed") - SystemClock.elapsedRealtime()
        else long("s_end_wall") - System.currentTimeMillis()
        return rem.coerceAtLeast(0)
    }

    // ---- focus session settings ------------------------------------------------
    var allowedApps: Set<String>
        get() = strSet("allowed_apps")
        set(v) = putSet("allowed_apps", v)

    var strictLock: Boolean
        get() = bool("strict_lock", true)
        set(v) = putBool("strict_lock", v)

    var lastMinutes: Int
        get() = int("last_minutes", 25)
        set(v) = putInt("last_minutes", v)

    var lastName: String
        get() = str("last_name")
        set(v) = putStr("last_name", v)

    var lastSubject: String
        get() = str("last_subject", "Physics")
        set(v) = putStr("last_subject", v)

    fun ensureDefaults(defaultPackages: Set<String>) {
        if (!bool("defaults_done")) {
            allowedApps = defaultPackages
            putBool("defaults_done", true)
        }
    }

    // ---- callers ------------------------------------------------------------
    var callersOnly: Boolean
        get() = bool("callers_only", true)
        set(v) = putBool("callers_only", v)

    var callers: List<Caller>
        get() = Codec.callers(str("callers", "[]"))
        set(v) = putStr("callers", Codec.callers(v))

    private fun weekKey(): String {
        val d = LocalDate.now()
        return "blocked_" + d.minusDays((d.dayOfWeek.value - 1).toLong())
    }

    fun addBlockedCall() = putInt("${weekKey()}_${LocalDate.now().dayOfWeek.value}", int("${weekKey()}_${LocalDate.now().dayOfWeek.value}") + 1)
    fun blockedCallsThisWeek(): List<Int> = (1..7).map { int("${weekKey()}_$it") }

    // ---- rules ---------------------------------------------------------------
    var appRules: List<AppRule>
        get() = Codec.appRules(str("app_rules", "[]"))
        set(v) = putStr("app_rules", Codec.appRules(v))

    var unlockRules: List<UnlockRule>
        get() = Codec.unlockRules(str("unlock_rules", "[]"))
        set(v) = putStr("unlock_rules", Codec.unlockRules(v))

    var blockedDuring: Set<String>
        get() = strSet("blocked_during")
        set(v) = putSet("blocked_during", v)

    var blockedAlways: Set<String>
        get() = strSet("blocked_always")
        set(v) = putSet("blocked_always", v)

    /** Master switch ("Disable app lock" in Profile). */
    var restrictionsEnabled: Boolean
        get() = bool("restrictions_on", true)
        set(v) = putBool("restrictions_on", v)

    // ---- daily record (expires at midnight) -------------------------------------
    fun day(date: String = today()): DayRec = DayRec.fromJson(sp.getString("day_$date", null))

    fun saveDay(r: DayRec, date: String = today()) {
        val e = sp.edit().putString("day_$date", r.toJson())
        val cutoff = LocalDate.now().minusDays(45).toString()
        sp.all.keys.filter { it.startsWith("day_") && it.substring(4) < cutoff }.forEach { e.remove(it) }
        e.apply()
    }

    /** Minutes in the pool that can still be spent on unlocking apps today. */
    val bankMinutes: Int
        get() { val d = day(); return TimeBank.poolAvailable(d.pool, d.penalty, d.fromPool) }

    // ---- progress ------------------------------------------------------------
    var xp: Int
        get() = int("xp")
        set(v) = putInt("xp", v)

    var totalFocusMinutes: Int
        get() = int("total_minutes")
        set(v) = putInt("total_minutes", v)

    /** Focus minutes per day: date -> minutes. Kept for ~400 days. */
    fun focusDays(): Map<String, Int> = Codec.intMap(str("focus_days"))

    val todayFocusMinutes: Int get() = focusDays()[today()] ?: 0

    private fun addFocus(minutes: Int) {
        val m = Codec.intMap(str("focus_days"))
        m[today()] = (m[today()] ?: 0) + minutes
        val cutoff = LocalDate.now().minusDays(400).toString()
        m.keys.removeAll { it < cutoff }
        putStr("focus_days", Codec.intMap(m))
    }

    /** Current streak: consecutive days with a completed focus session, ending today or yesterday. */
    val streak: Int
        get() {
            val days = focusDays()
            var d = LocalDate.now()
            if ((days[d.toString()] ?: 0) <= 0) d = d.minusDays(1)
            var n = 0
            while ((days[d.toString()] ?: 0) > 0) { n++; d = d.minusDays(1) }
            return n
        }

    fun recordCompleted(minutes: Int): Int {
        val earned = TimeBank.earnedFromFocus(minutes)
        xp += Rewards.xpForSession(minutes)
        totalFocusMinutes += minutes
        addFocus(minutes)
        val d = day()
        d.pool += earned
        d.earnedTotal += earned
        saveDay(d)
        return earned
    }

    fun recordQuit() {
        xp = Rewards.xpAfterQuit(xp)
    }

    // ---- activities (Strava cache) -----------------------------------------------
    var activities: List<Activity>
        get() = Codec.activities(str("activities", "[]"))
        set(v) = putStr("activities", Codec.activities(v))

    fun activitiesOn(date: String): List<Activity> = activities.filter { it.startDate == date }

    val studyToday: Int get() = int("study_${today()}")

    // ---- preferences -----------------------------------------------------------
    var watchMe: Boolean
        get() = bool("watch_me", true)
        set(v) {
            if (v == watchMe && sp.contains("watch_me")) return
            putBool("watch_me", v)
            val now = System.currentTimeMillis()
            if (v) putLong("watch_open", now)
            else {
                val start = long("watch_open", 0)
                if (start > 0 && now - start > 60_000) {
                    val arr = JSONArray(str("watch_windows", "[]"))
                    arr.put(JSONObject().put("s", start).put("e", now))
                    val keep = JSONArray()
                    val cutoff = now - 60L * 86_400_000L
                    for (i in 0 until arr.length()) if (arr.getJSONObject(i).getLong("e") > cutoff) keep.put(arr.getJSONObject(i))
                    putStr("watch_windows", keep.toString())
                }
                putLong("watch_open", 0)
            }
        }

    /** Closed windows plus the currently open one. Pairs of (startMs, endMs). */
    fun watchWindows(): List<Pair<Long, Long>> {
        val out = ArrayList<Pair<Long, Long>>()
        runCatching {
            val arr = JSONArray(str("watch_windows", "[]"))
            for (i in 0 until arr.length()) out.add(arr.getJSONObject(i).getLong("s") to arr.getJSONObject(i).getLong("e"))
        }
        val open = long("watch_open", 0)
        if (watchMe && open > 0) out.add(open to System.currentTimeMillis())
        return out
    }

    var imperial: Boolean
        get() = bool("imperial")
        set(v) = putBool("imperial", v)

    /** 0 = Normal, 1 = Hard, 2 = Goggins */
    var tone: Int
        get() = int("tone", 2)
        set(v) = putInt("tone", v)

    var setupDone: Boolean
        get() = bool("setup_done")
        set(v) = putBool("setup_done", v)

    // ---- security --------------------------------------------------------------
    var biometricLock: Boolean
        get() = bool("sec_bio")
        set(v) = putBool("sec_bio", v)

    var lockOnExit: Boolean
        get() = bool("sec_exit")
        set(v) = putBool("sec_exit", v)

    var hideInRecents: Boolean
        get() = bool("sec_recents")
        set(v) = putBool("sec_recents", v)

    /** Seconds in the background before the app re-locks; 0 = immediately, -1 = never. */
    var autoLockSeconds: Int
        get() = int("sec_auto", 30)
        set(v) = putInt("sec_auto", v)

    // ---- cookie jar, mirror ------------------------------------------------------
    var jar: List<Entry>
        get() = Codec.entries(str("jar", "[]"))
        set(v) = putStr("jar", Codec.entries(v))

    var agenda: List<AgendaItem>
        get() = Codec.agenda(str("agenda", "")).ifEmpty { DEFAULT_AGENDA }
        set(v) = putStr("agenda", Codec.agenda(v))

    var avoided: List<Entry>
        get() = Codec.entries(str("avoided", "[]"))
        set(v) = putStr("avoided", Codec.entries(v))

    /** Per-day review answers: agenda item id -> done. Stored as ids marked done for the day. */
    fun reviewDone(date: String = today()): Set<String> = strSet("review_done_$date")
    fun putReviewDone(ids: Set<String>, date: String = today()) = putSet("review_done_$date", ids)
    fun reviewSubmitted(date: String = today()): Boolean = bool("review_submitted_$date")
    fun putReviewSubmitted(date: String = today()) = putBool("review_submitted_$date", true)

    // ---- weekly targets --------------------------------------------------------
    var targetScale: Float
        get() = sp.getFloat("target_scale", 1f)
        set(v) = sp.edit().putFloat("target_scale", v).apply()

    var targetWeek: String
        get() = str("target_week")
        set(v) = putStr("target_week", v)

    // ---- wipe ------------------------------------------------------------------
    fun resetAll() {
        sp.edit().clear().apply()
    }

    companion object {
        val DEFAULT_AGENDA = listOf(
            AgendaItem("run", "Run", "", "Run", "6:00 AM"),
            AgendaItem("phy", "Focus: Physics", "", "Physics", "9:00 AM"),
            AgendaItem("gym", "Gym: Push Day", "", "Workout", "1:00 PM"),
            AgendaItem("chem", "Focus: Chemistry", "", "Chemistry", "4:00 PM"),
            AgendaItem("mirror", "Accountability Mirror", "", "Mirror", "9:00 PM"),
        )

        @Volatile private var inst: Store? = null
        fun get(ctx: Context): Store =
            inst ?: synchronized(this) { inst ?: Store(ctx).also { inst = it } }
    }
}
