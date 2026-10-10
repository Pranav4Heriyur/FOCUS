package com.focus.app

import com.focus.app.data.ProgressMath
import com.focus.app.util.formatClock
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressMathTest {
    @Test fun earnedMinutes_matchesSpecExample() {
        assertEquals(9, ProgressMath.earnedMinutes(45))
        assertEquals(5, ProgressMath.earnedMinutes(25))
        assertEquals(0, ProgressMath.earnedMinutes(4))
    }

    @Test fun streak_firstSessionStartsAtOne() {
        assertEquals(1, ProgressMath.nextStreak(null, today = 100, current = 0))
    }

    @Test fun streak_sameDayDoesNotIncrease() {
        assertEquals(5, ProgressMath.nextStreak(100, today = 100, current = 5))
    }

    @Test fun streak_consecutiveDayIncreases() {
        assertEquals(6, ProgressMath.nextStreak(99, today = 100, current = 5))
    }

    @Test fun streak_missedDayResets_noFreezes() {
        assertEquals(1, ProgressMath.nextStreak(98, today = 100, current = 5))
    }

    @Test fun earlyExit_costs50XpAndNeverGoesNegative() {
        assertEquals(70, ProgressMath.xpAfterEarlyExit(120))
        assertEquals(0, ProgressMath.xpAfterEarlyExit(30))
    }

    @Test fun clock_formatting() {
        assertEquals("45:00", formatClock(45 * 60_000L))
        assertEquals("1:05:00", formatClock(65 * 60_000L))
        assertEquals("00:01", formatClock(1))        // rounds up
        assertEquals("00:00", formatClock(0))
        assertEquals("00:00", formatClock(-5))
    }
}
