package com.focus.app.data.model

data class FocusSession(
    val name: String,
    val subject: String,
    val startMs: Long,
    val endMs: Long,
    /** Stored for the lock step. The spec does not yet define what strict mode changes. */
    val strict: Boolean,
) {
    val durationMs: Long get() = endMs - startMs
    val durationMinutes: Int get() = (durationMs / 60_000L).toInt()
    fun remainingMs(nowMs: Long): Long = (endMs - nowMs).coerceAtLeast(0L)
    fun progress(nowMs: Long): Float =
        if (durationMs <= 0L) 1f else ((nowMs - startMs).toFloat() / durationMs).coerceIn(0f, 1f)
}

data class ProgressStats(val xp: Int, val streakDays: Int, val earnedTodayMin: Int)

data class SessionResult(
    val subject: String,
    val minutes: Int,
    val streakDays: Int,
    val xpGained: Int,
    val earnedMin: Int,
)
