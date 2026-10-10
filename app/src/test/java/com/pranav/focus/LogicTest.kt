package com.pranav.focus

import com.pranav.focus.data.CallMatcher
import com.pranav.focus.data.ExitRules
import com.pranav.focus.data.Rewards
import com.pranav.focus.data.TimeBank
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    @Test fun focusEarnsHalf() {
        assertEquals(12, TimeBank.earnedFromFocus(25))
        assertEquals(30, TimeBank.earnedFromFocus(60))
        assertEquals(0, TimeBank.earnedFromFocus(0))
    }

    @Test fun manualEarnsNothing() = assertEquals(0, TimeBank.earnedFromManual(120))

    @Test fun quitPenaltyNeverNegative() {
        assertEquals(50, Rewards.xpAfterQuit(100))
        assertEquals(0, Rewards.xpAfterQuit(20))
    }

    @Test fun callNumbersNormalise() {
        assertEquals("9876543210", CallMatcher.normalize("+91 98765-43210"))
        assertTrue(CallMatcher.matches("+919876543210", listOf("98765 43210")))
        assertFalse(CallMatcher.matches("9876500000", listOf("9876543210")))
        assertFalse(CallMatcher.matches(null, listOf("9876543210")))
        assertFalse(CallMatcher.matches("", listOf("")))
    }

    @Test fun phraseCheck() {
        assertTrue(ExitRules.phraseOk("  i am choosing to quit "))
        assertFalse(ExitRules.phraseOk("i quit"))
    }
}
