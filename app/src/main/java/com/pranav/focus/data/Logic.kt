package com.pranav.focus.data

/** Pure logic, no Android dependencies, so it is unit-testable. */
object TimeBank {
    /** Verified effort only: a completed locked session earns 1 minute per 2 focused minutes. */
    const val EARN_RATIO = 0.5

    fun earnedFromFocus(focusedMinutes: Int): Int =
        if (focusedMinutes <= 0) 0 else (focusedMinutes * EARN_RATIO).toInt()

    /** Manual entries are unverified and never earn time. */
    fun earnedFromManual(@Suppress("UNUSED_PARAMETER") minutes: Int): Int = 0
}

object Rewards {
    const val XP_PER_FOCUS_MINUTE = 2
    const val QUIT_XP_PENALTY = 50

    fun xpForSession(minutes: Int) = minutes.coerceAtLeast(0) * XP_PER_FOCUS_MINUTE
    fun xpAfterQuit(xp: Int) = (xp - QUIT_XP_PENALTY).coerceAtLeast(0)
}

object CallMatcher {
    /** Compare on the last 10 digits so +91 / 0 / spaces / dashes do not matter. */
    fun normalize(number: String?): String {
        val digits = number.orEmpty().filter { it.isDigit() }
        return if (digits.length > 10) digits.takeLast(10) else digits
    }

    fun matches(incoming: String?, saved: List<String>): Boolean {
        val n = normalize(incoming)
        if (n.length < 5) return false // private / unknown numbers are never allowed
        return saved.any { normalize(it) == n }
    }
}

object ExitRules {
    const val HOLD_SECONDS = 60
    const val PHRASE = "I choose distraction over my goals"

    fun phraseOk(input: String) = input.trim().equals(PHRASE, ignoreCase = true)
}
