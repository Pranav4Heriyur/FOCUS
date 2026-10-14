package com.pranav.focus

import com.pranav.focus.data.*
import org.junit.Assert.*
import org.junit.Test

class WorkoutLogicTest {
    @Test fun verifiedRule() {
        assertTrue(WorkoutLogic.isVerified(5, 20)); assertFalse(WorkoutLogic.isVerified(4, 60))
        assertFalse(WorkoutLogic.isVerified(8, 19)); assertFalse(WorkoutLogic.isVerified(8, 60, viaFinish = false))
    }
    @Test fun xp() { assertEquals(40, WorkoutLogic.xpFor(0)); assertEquals(60, WorkoutLogic.xpFor(4)); assertEquals(60, WorkoutLogic.xpFor(9)) }
    @Test fun plateMathAddsUp() {
        val (p, rem) = WorkoutLogic.plates(100.0, 20.0)
        assertEquals(40.0, p.sum(), 1e-9); assertEquals(0.0, rem, 1e-9); assertEquals(listOf(25.0, 15.0), p)
    }
    @Test fun platesBelowBar() = assertTrue(WorkoutLogic.plates(15.0, 20.0).first.isEmpty())
    @Test fun prDetected() {
        val h = listOf(WorkoutLogic.SetLike("b", 60.0, 8, 8.0, SetType.NORMAL, 1))
        val c = listOf(WorkoutLogic.SetLike("b", 65.0, 8, 8.0, SetType.NORMAL, 2))
        val prs = WorkoutLogic.detectPrs(mapOf("b" to "Bench"), c, h)
        assertTrue(prs.any { it.kind == "e1RM" }); assertTrue(prs.any { it.kind == "Weight" })
        assertTrue(WorkoutLogic.detectPrs(mapOf("b" to "Bench"), c, emptyList()).isEmpty())
    }
    @Test fun unknownVsZero() {
        assertEquals(MuscleStatus.UNKNOWN, WorkoutLogic.toLoads(emptyMap(), false).getValue(MuscleGroup.CHEST).status)
        assertEquals(MuscleStatus.UNDER_TARGET, WorkoutLogic.toLoads(emptyMap(), true).getValue(MuscleGroup.CHEST).status)
    }
    @Test fun weeklySetsUseCredits() {
        val sets = List(3) { WorkoutLogic.SetLike("bench_press", 60.0, 8, 8.0, SetType.NORMAL, it.toLong()) }
        val credits = mapOf("bench_press" to EffectiveLoad.muscleCredit(emptyList(), "horizontal_push"))
        val h = WorkoutLogic.weeklyHardSets(sets, credits, emptyMap())
        assertEquals(1.8, h.getValue(MuscleGroup.CHEST), 0.01); assertEquals(0.9, h.getValue(MuscleGroup.TRICEPS), 0.01)
    }
    @Test fun warmupsIgnored() =
        assertTrue(WorkoutLogic.weeklyHardSets(listOf(WorkoutLogic.SetLike("x", 20.0, 10, null, SetType.WARMUP, 0)), mapOf("x" to mapOf("chest" to 1.0)), emptyMap()).isEmpty())
    @Test fun prescriptionValidation() {
        assertNull(WorkoutLogic.validatePrescription(3, 8, 12, null, null, null))
        assertNotNull(WorkoutLogic.validatePrescription(0, 8, 12, null, null, null))
        assertNotNull(WorkoutLogic.validatePrescription(3, 12, 8, null, null, null))
        assertNotNull(WorkoutLogic.validatePrescription(3, 8, 12, -1.0, null, null))
        assertNotNull(WorkoutLogic.validatePrescription(3, 8, 12, null, 5000, null))
        assertNotNull(WorkoutLogic.validatePrescription(3, 8, 12, null, null, 11.0))
    }
    @Test fun routineIssues() {
        val ok = listOf(WorkoutLogic.DayPlan("Push", 1, false, 1), WorkoutLogic.DayPlan("Rest", null, true, null))
        assertTrue(WorkoutLogic.routineIssues(ok).isEmpty())
        assertEquals(1, WorkoutLogic.routineIssues(listOf(WorkoutLogic.DayPlan("A", 1, false, null))).size)
        assertTrue(WorkoutLogic.routineIssues(listOf(WorkoutLogic.DayPlan("A", 2, false, 1), WorkoutLogic.DayPlan("B", 2, false, 2))).any { "Tue" in it })
        assertTrue(WorkoutLogic.routineIssues(emptyList()).isNotEmpty())
    }
    @Test fun plannedDay() {
        val d = listOf(RoutineDay(1, 1, 0, "Push", null, false, 10), RoutineDay(2, 1, 1, "Rest", null, true, null), RoutineDay(3, 1, 2, "Pull", null, false, 11))
        assertEquals("Push", WorkoutLogic.plannedFor(d, 3, 0)?.label); assertEquals("Pull", WorkoutLogic.plannedFor(d, 3, 1)?.label)
        assertEquals("Push", WorkoutLogic.plannedFor(d, 3, 2)?.label)
        val w = listOf(RoutineDay(1, 1, 0, "Push", 1, false, 10))
        assertEquals("Push", WorkoutLogic.plannedFor(w, 1, 0)?.label); assertNull(WorkoutLogic.plannedFor(w, 2, 0))
    }
    @Test fun adherenceUnknownWhenNothingPlanned() {
        assertNull(WorkoutLogic.adherence(0, 0)); assertEquals(0.5, WorkoutLogic.adherence(4, 2)!!, 1e-9); assertEquals(1.0, WorkoutLogic.adherence(2, 5)!!, 1e-9)
    }
}
