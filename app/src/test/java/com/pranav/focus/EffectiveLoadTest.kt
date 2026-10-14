package com.pranav.focus

import com.pranav.focus.data.EffectiveLoad as EL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Ground truth = docs/effective_load.py worked examples (model doc section 8). */
class EffectiveLoadTest {
    private fun near(exp: Double, act: Double, tol: Double = 0.01) = assertEquals(exp, act, tol)

    @Test fun bench60x8Rpe8() {
        val rir = EL.rirFrom(8.0); assertEquals(2, rir)
        val e = EL.e1rm(60.0, 8, rir)!!; near(80.0, e)
        near(0.6, EL.hardSetEquivalent(8, rir, 60.0, ownE1rm = e).hse)
    }
    @Test fun squat100x5Failure() {
        val e = EL.e1rm(100.0, 5, 0)!!; near(114.58, e)
        near(1.0, EL.hardSetEquivalent(5, 0, 100.0, ownE1rm = e).hse)
    }
    @Test fun unknownCrunchHighRep() {
        val rir = EL.rirFrom(); assertNull(EL.e1rm(40.0, 15, rir))
        val h = EL.hardSetEquivalent(15, rir, 40.0)
        near(0.6, h.hse); near(0.45, h.intensity); assertEquals("low", h.confidence)
        assertEquals(mapOf("core" to 1.0), EL.muscleCredit(listOf("core")))
    }
    @Test fun landmineVerticalPush() {
        val rir = EL.rirFrom(9.0); val e = EL.e1rm(30.0, 10, rir)!!; near(41.27, e)
        val hse = EL.hardSetEquivalent(10, rir, 30.0, ownE1rm = e).hse; near(0.8, hse)
        val c = EL.muscleCredit(emptyList(), "vertical_push").mapValues { it.value * hse }
        near(0.8, c["front_delts"]!!); near(0.4, c["triceps"]!!); near(0.4, c["chest"]!!)
    }
    @Test fun weightedPullup() {
        val load = EL.effectiveLoadKg(10.0, 75.0, 1.0); near(85.0, load)
        val e = EL.e1rm(load, 6, 1)!!; near(103.42, e)
        near(0.8, EL.hardSetEquivalent(6, 1, load, ownE1rm = e).hse)
    }
    @Test fun lightSetVsRef() = near(0.4, EL.hardSetEquivalent(10, 2, 20.0, e1rmRef = 100.0).hse)
    @Test fun watchOnlySession() {
        val met = EL.metFromKcal(400.0, 75.0, 40.0); near(9.0, met); near(6.43, EL.rpeFromMet(met))
        near(257.14, EL.sessionLoad(40.0, met = met))
        near(241.74, EL.sessionLoad(40.0, rpe = 6.0, trimp = EL.banisterTrimp(40.0, 150.0, 55.0, 190.0)))
    }
    @Test fun fitnessFatigueForm() {
        val l = EL.fitnessFatigue(listOf(300.0, 0.0, 350.0, 280.0, 0.0, 400.0, 320.0, 0.0, 300.0, 360.0, 0.0, 380.0, 300.0, 0.0).map { it })
        val last = l.takeLast(3)
        near(281.4, last[0].fitness, 0.11); near(243.2, last[0].fatigue, 0.11); near(38.2, last[0].form, 0.11)
        near(275.2, last[2].fitness, 0.11); near(217.3, last[2].fatigue, 0.11); near(57.9, last[2].form, 0.11)
    }
}
