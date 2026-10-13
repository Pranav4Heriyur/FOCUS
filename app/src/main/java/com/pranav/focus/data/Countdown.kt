package com.pranav.focus.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the exam countdown currently means. Never carries a negative day count. */
sealed interface Countdown {
    object Unset : Countdown
    data class Upcoming(val date: LocalDate, val days: Int) : Countdown
    data class ExamDay(val date: LocalDate) : Countdown
    data class Passed(val date: LocalDate) : Countdown
}

/**
 * The one place the countdown is calculated and worded. Home's compact card and Study's full card both render
 * from these functions, so they cannot disagree. Pure (no Android) so it is unit-testable.
 */
object CountdownLogic {
    /** Whole calendar days between device-local [today] and [exam] (reuses [StudyLogic.daysTo]). */
    fun compute(exam: LocalDate?, today: LocalDate): Countdown = when {
        exam == null -> Countdown.Unset
        exam.isBefore(today) -> Countdown.Passed(exam)
        exam == today -> Countdown.ExamDay(exam)
        else -> Countdown.Upcoming(exam, StudyLogic.daysTo(exam, today).toInt())
    }

    fun examDate(c: Countdown): LocalDate? = when (c) {
        is Countdown.Upcoming -> c.date
        is Countdown.ExamDay -> c.date
        is Countdown.Passed -> c.date
        Countdown.Unset -> null
    }

    /** Whole days left; 0 on exam day and after it, never negative. */
    fun daysLeft(c: Countdown): Int = if (c is Countdown.Upcoming) c.days else 0

    /** "NEET 2027" - the year comes from the configured date, never hardcoded. */
    fun title(c: Countdown): String = examDate(c)?.let { "NEET ${it.year}" } ?: "NEET"

    fun headline(c: Countdown): String = when (c) {
        Countdown.Unset -> "Set exam date"
        is Countdown.Upcoming -> if (c.days == 1) "1 day left" else "${c.days} days left"
        is Countdown.ExamDay -> "Exam day"
        is Countdown.Passed -> "Exam date passed"
    }

    /** "2 May 2027 · Sunday", or a hint when no date is set. */
    fun dateLine(c: Countdown, locale: Locale = Locale.getDefault()): String =
        examDate(c)?.format(DateTimeFormatter.ofPattern("d MMM yyyy · EEEE", locale)) ?: "Tap to choose a date"

    /**
     * Ring fill, 0..100 = share of the tracked window still remaining. Only meaningful with an honest [start]
     * (the day the user first configured the date); without one it is a plain full ring rather than a made-up percentage.
     */
    fun ringPct(c: Countdown, start: LocalDate?): Int = when (c) {
        is Countdown.Upcoming -> {
            val total = if (start == null) 0L else StudyLogic.daysTo(c.date, start)
            if (total <= 0L) 100 else (c.days * 100L / total).coerceIn(1L, 100L).toInt()
        }
        is Countdown.ExamDay, is Countdown.Passed, Countdown.Unset -> 0
    }
}
