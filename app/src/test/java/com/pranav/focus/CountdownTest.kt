package com.pranav.focus

import com.pranav.focus.data.Countdown
import com.pranav.focus.data.CountdownLogic
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class CountdownTest {
    private val today = LocalDate.of(2026, 10, 13)
    private fun d(s: String) = LocalDate.parse(s)

    @Test fun unsetDateIsUnsetNotAPlaceholder() {
        val c = CountdownLogic.compute(null, today)
        assertTrue(c is Countdown.Unset)
        assertEquals(0, CountdownLogic.daysLeft(c))
        assertEquals("Set exam date", CountdownLogic.headline(c))
        assertEquals("NEET", CountdownLogic.title(c))     // no hardcoded year
        assertEquals(0, CountdownLogic.ringPct(c, null))
    }

    @Test fun futureDateCountsWholeCalendarDays() {
        val c = CountdownLogic.compute(d("2026-10-20"), today)
        assertEquals(7, CountdownLogic.daysLeft(c))
        assertEquals("7 days left", CountdownLogic.headline(c))
        assertEquals("NEET 2026", CountdownLogic.title(c))   // year derived from the configured date
    }

    @Test fun oneDayLeftIsSingular() {
        assertEquals("1 day left", CountdownLogic.headline(CountdownLogic.compute(d("2026-10-14"), today)))
    }

    @Test fun examDayIsZeroAndSaysExamDay() {
        val c = CountdownLogic.compute(today, today)
        assertTrue(c is Countdown.ExamDay)
        assertEquals(0, CountdownLogic.daysLeft(c))
        assertEquals("Exam day", CountdownLogic.headline(c))
        assertEquals(0, CountdownLogic.ringPct(c, d("2026-01-01")))
    }

    @Test fun pastDateNeverGoesNegative() {
        for (past in listOf("2026-10-12", "2026-01-01", "2020-05-05")) {
            val c = CountdownLogic.compute(d(past), today)
            assertTrue(c is Countdown.Passed)
            assertEquals(0, CountdownLogic.daysLeft(c))
            assertEquals("Exam date passed", CountdownLogic.headline(c))
        }
    }

    @Test fun dateBoundariesYearAndLeapDay() {
        assertEquals(1, CountdownLogic.daysLeft(CountdownLogic.compute(d("2027-01-01"), d("2026-12-31"))))
        assertEquals(2, CountdownLogic.daysLeft(CountdownLogic.compute(d("2028-03-01"), d("2028-02-28"))))   // 2028 is a leap year
        assertEquals(1, CountdownLogic.daysLeft(CountdownLogic.compute(d("2027-03-01"), d("2027-02-28"))))   // 2027 is not
        assertEquals(365, CountdownLogic.daysLeft(CountdownLogic.compute(d("2027-10-13"), today)))
    }

    @Test fun midnightRolloverDropsExactlyOneDay() {
        val exam = d("2027-05-02")
        val before = CountdownLogic.daysLeft(CountdownLogic.compute(exam, d("2026-10-13")))
        val after = CountdownLogic.daysLeft(CountdownLogic.compute(exam, d("2026-10-14")))     // the same date seen after midnight
        assertEquals(before - 1, after)
        assertTrue(CountdownLogic.compute(exam, exam.minusDays(1)) is Countdown.Upcoming)
        assertTrue(CountdownLogic.compute(exam, exam) is Countdown.ExamDay)
        assertTrue(CountdownLogic.compute(exam, exam.plusDays(1)) is Countdown.Passed)
    }

    @Test fun changingTheDateChangesEveryRenderedValue() {
        val a = CountdownLogic.compute(d("2027-05-02"), today)
        val b = CountdownLogic.compute(d("2027-06-01"), today)
        assertNotEquals(CountdownLogic.daysLeft(a), CountdownLogic.daysLeft(b))
        assertNotEquals(CountdownLogic.dateLine(a, Locale.ENGLISH), CountdownLogic.dateLine(b, Locale.ENGLISH))
    }

    @Test fun dateLineMatchesTheDesignFormat() {
        assertEquals("2 May 2027 · Sunday", CountdownLogic.dateLine(CountdownLogic.compute(d("2027-05-02"), today), Locale.ENGLISH))
        assertEquals("Tap to choose a date", CountdownLogic.dateLine(Countdown.Unset, Locale.ENGLISH))
    }

    /** Home and Study render from the same state through the same functions, so every field they show must agree. */
    @Test fun homeAndStudyShowTheSameNumberAndDate() {
        for (exam in listOf(null, d("2026-10-12"), today, d("2026-10-14"), d("2027-05-02"))) {
            val state = CountdownLogic.compute(exam, today)           // one state object...
            val home = Triple(CountdownLogic.title(state), CountdownLogic.daysLeft(state), CountdownLogic.dateLine(state, Locale.ENGLISH))
            val study = Triple(CountdownLogic.title(state), CountdownLogic.daysLeft(state), CountdownLogic.dateLine(state, Locale.ENGLISH))
            assertEquals(home, study)                                   // ...rendered by both cards
            assertEquals(CountdownLogic.examDate(state), exam)
        }
    }

    @Test fun ringIsHonestAboutTrackingStart() {
        val exam = d("2026-11-12")                                      // 30 days after today
        val c = CountdownLogic.compute(exam, today)
        assertEquals(100, CountdownLogic.ringPct(c, null))             // no start -> plain full ring, no invented percentage
        assertEquals(100, CountdownLogic.ringPct(c, today))            // just configured today -> all of it remaining
        assertEquals(30, CountdownLogic.ringPct(c, exam.minusDays(100)))   // 100 day window, 30 left
        assertEquals(100, CountdownLogic.ringPct(c, exam))             // degenerate start (not before exam) -> plain ring
        assertEquals(100, CountdownLogic.ringPct(c, d("2027-01-01")))
        assertEquals(1, CountdownLogic.ringPct(CountdownLogic.compute(d("2026-10-14"), today), d("2020-01-01")))   // never shows an empty ring while days remain
    }
}
