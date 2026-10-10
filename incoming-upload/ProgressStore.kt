package com.focus.app.data.persistence

import android.content.Context
import com.focus.app.data.ProgressMath
import com.focus.app.data.model.ProgressStats
import com.focus.app.data.model.SessionResult

/** XP, streak and today's earned minutes. Earned minutes expire at midnight (spec). */
class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("focus_progress", Context.MODE_PRIVATE)

    fun stats(today: Long): ProgressStats = ProgressStats(
        xp = prefs.getInt(KEY_XP, 0),
        streakDays = currentStreak(today),
        earnedTodayMin = if (prefs.getLong(KEY_EARNED_DAY, -1L) == today) prefs.getInt(KEY_EARNED_MIN, 0) else 0,
    )

    fun recordCompletedSession(subject: String, minutes: Int, today: Long): SessionResult {
        val lastDay = prefs.getLong(KEY_LAST_DAY, -1L).takeIf { it >= 0 }
        val streak = ProgressMath.nextStreak(lastDay, today, prefs.getInt(KEY_STREAK, 0))
        val xpGained = ProgressMath.xpFor(minutes)
        val earned = ProgressMath.earnedMinutes(minutes)
        val earnedBefore = stats(today).earnedTodayMin

        prefs.edit()
            .putInt(KEY_XP, prefs.getInt(KEY_XP, 0) + xpGained)
            .putInt(KEY_STREAK, streak)
            .putLong(KEY_LAST_DAY, today)
            .putLong(KEY_EARNED_DAY, today)
            .putInt(KEY_EARNED_MIN, earnedBefore + earned)
            .commit()
        return SessionResult(subject, minutes, streak, xpGained, earned)
    }

    /** A4: leaving early costs 50 XP and resets the streak. */
    fun applyEarlyExit() {
        prefs.edit()
            .putInt(KEY_XP, ProgressMath.xpAfterEarlyExit(prefs.getInt(KEY_XP, 0)))
            .putInt(KEY_STREAK, 0)
            .remove(KEY_LAST_DAY)
            .commit()
    }

    /** A streak only counts if the last session was today or yesterday. */
    private fun currentStreak(today: Long): Int {
        val lastDay = prefs.getLong(KEY_LAST_DAY, -1L)
        val streak = prefs.getInt(KEY_STREAK, 0)
        return if (lastDay >= 0 && lastDay >= today - 1) streak else 0
    }

    private companion object {
        const val KEY_XP = "xp"
        const val KEY_STREAK = "streak"
        const val KEY_LAST_DAY = "last_day"
        const val KEY_EARNED_DAY = "earned_day"
        const val KEY_EARNED_MIN = "earned_min"
    }
}
