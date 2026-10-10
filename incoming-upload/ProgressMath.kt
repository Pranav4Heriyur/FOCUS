package com.focus.app.data

/** Pure rules, kept free of Android so they are unit-testable. */
object ProgressMath {
    /**
     * PLACEHOLDER rates. The spec example (45 min -> +9 min earned) fits 1 earned minute per
     * 5 focus minutes. "Decide the default earn rates" is still an open item in the handoff.
     */
    const val FOCUS_MINUTES_PER_EARNED_MINUTE = 5
    const val XP_PER_FOCUS_MINUTE = 1
    /** From A4: "Leaving costs 50 XP and resets your streak". */
    const val EARLY_EXIT_XP_PENALTY = 50

    fun earnedMinutes(focusMinutes: Int): Int = focusMinutes / FOCUS_MINUTES_PER_EARNED_MINUTE

    fun xpFor(focusMinutes: Int): Int = focusMinutes * XP_PER_FOCUS_MINUTE

    /** No streak freezes (Goggins rule): a missed day resets the streak to 1 on the next session. */
    fun nextStreak(lastDay: Long?, today: Long, current: Int): Int = when {
        lastDay == null -> 1
        lastDay == today -> maxOf(current, 1)
        lastDay == today - 1 -> current + 1
        else -> 1
    }

    fun xpAfterEarlyExit(xp: Int): Int = (xp - EARLY_EXIT_XP_PENALTY).coerceAtLeast(0)
}
