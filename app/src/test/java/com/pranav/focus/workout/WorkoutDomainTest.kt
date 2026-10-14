package com.pranav.focus.workout

import com.pranav.focus.workout.data.InMemoryWorkoutRepository
import com.pranav.focus.workout.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

class WorkoutDomainTest {

    private val catalogue = WorkoutCatalogue.BY_ID
    private val now = 1_000L
    private val mon = LocalDate.of(2026, 10, 12)   // Monday
    private val wed = LocalDate.of(2026, 10, 14)   // Wednesday

    private fun template(id: String, vararg exerciseIds: String): WorkoutTemplate {
        var t = WorkoutRules.newTemplate(id, "Push A", now)
        exerciseIds.forEachIndexed { i, e -> t = WorkoutRules.addExercise(t, "$id-row$i", e, now) }
        return t
    }

    private fun routine(id: String, vararg days: RoutineDay): WorkoutRoutine =
        WorkoutRoutine(id, "Split", "", null, null, RoutineStatus.DRAFT, days.toList(), now, now)

    private fun day(id: String, label: String, templateId: String?, weekday: Int?, rest: Boolean = false, order: Int = 0) =
        RoutineDay(id, order, label, rest, if (rest) null else templateId, weekday)

    private fun completed(id: String, date: LocalDate, t: WorkoutTemplate, sets: Int, type: SetType = SetType.NORMAL,
                          routineId: String? = null, dayId: String? = null, status: SessionStatus = SessionStatus.COMPLETED,
                          cat: Map<String, Exercise> = catalogue): WorkoutSession {
        var s = WorkoutSessions.startFromTemplate("$id", t, cat, date, now, rowIdFor = { "$id-${it.id}" }).getOrThrow()
        s = s.copy(routineId = routineId, routineDayId = dayId)
        s.exercises.forEach { row -> for (i in 0 until sets) {
            s = WorkoutSessions.recordSet(s, LoggedSet("$id-${row.id}-$i", row.id, i, type, 8, 60.0, 8.0, now)).getOrThrow()
        } }
        return s.copy(status = status, completedAtMs = now)
    }

    // ---------- set bands and e1RM ----------

    @Test fun setBandBoundaries() {
        assertEquals(SetBand.LOW, WorkoutMath.setBandFor(0))
        assertEquals(SetBand.LOW, WorkoutMath.setBandFor(9))
        assertEquals(SetBand.IN_RANGE, WorkoutMath.setBandFor(10))
        assertEquals(SetBand.IN_RANGE, WorkoutMath.setBandFor(20))
        assertEquals(SetBand.HIGH, WorkoutMath.setBandFor(21))
        assertNull(WorkoutMath.setBandFor(null))
    }

    @Test(expected = IllegalArgumentException::class) fun negativeSetCountRejected() { WorkoutMath.setBandFor(-1) }

    @Test fun e1rmEpleyForWorkingSets() {
        assertEquals(100.0 * (1 + 5 / 30.0), WorkoutMath.e1rm(100.0, 5, SetType.NORMAL)!!, 1e-9)
        assertEquals(100.0, WorkoutMath.e1rm(100.0, 1, SetType.FAILURE)!!, 1e-9)
    }

    @Test fun e1rmIsNullWhenNotEstimable() {
        assertNull(WorkoutMath.e1rm(100.0, 5, SetType.WARMUP))
        assertNull(WorkoutMath.e1rm(100.0, 16, SetType.NORMAL))
        assertNull(WorkoutMath.e1rm(null, 5, SetType.NORMAL))
        assertNull(WorkoutMath.e1rm(0.0, 5, SetType.NORMAL))
    }

    // ---------- statistics: completed-only, unknown is not zero ----------

    @Test fun hardSetsCountOnlyCompletedWorkingSets() {
        val t = template("t1", "builtin:bench_press")
        val done = completed("s1", mon, t, sets = 3)
        val abandoned = completed("s2", mon, t, sets = 5, status = SessionStatus.ABANDONED)
        val skipped = WorkoutSessions.skipRoutineDay("s3", routine("r", day("d", "Push", "t1", 1)), day("d", "Push", "t1", 1), mon, now)
        val warm = completed("s4", mon, t, sets = 2, type = SetType.WARMUP)
        val res = WorkoutMath.hardSetsByMuscle(listOf(done, abandoned, skipped, warm), catalogue, mon, mon)
        assertEquals(3, res.byMuscle[MuscleGroup.PECTORALS])
        assertEquals(0, res.byMuscle[MuscleGroup.QUADRICEPS])       // known zero, not unknown
        assertEquals(0, res.unmappedSets)
    }

    @Test fun customExerciseWithoutMusclesIsReportedAsUnmappedNotZero() {
        val custom = Exercise("custom:odd", "Odd lift", emptySet(), true)
        val cat = catalogue + (custom.id to custom)
        var t = WorkoutRules.newTemplate("tc", "Mixed", now)
        t = WorkoutRules.addExercise(t, "tc-r", custom.id, now)
        val s = completed("sc", mon, t, sets = 4, cat = cat)
        val res = WorkoutMath.hardSetsByMuscle(listOf(s), cat, mon, mon)
        assertEquals(4, res.unmappedSets)
        assertTrue(res.byMuscle.values.all { it == 0 })
    }

    @Test fun windowFiltersByDate() {
        val t = template("tw", "builtin:bench_press")
        val inWindow = completed("sw1", mon, t, sets = 2)
        val outside = completed("sw2", mon.minusDays(7), t, sets = 9)
        val res = WorkoutMath.hardSetsByMuscle(listOf(inWindow, outside), catalogue, mon, mon.plusDays(6))
        assertEquals(2, res.byMuscle[MuscleGroup.PECTORALS])
    }

    @Test fun bestE1rmIgnoresWarmupsAndUnfinishedSessions() {
        val t = template("tb", "builtin:bench_press")
        val done = completed("sb", mon, t, sets = 1)
        val withHeavy = done.copy(sets = done.sets + LoggedSet("heavy", done.exercises[0].id, 1, SetType.NORMAL, 5, 100.0, null, now))
        val warm = done.copy(sets = done.sets + LoggedSet("w", done.exercises[0].id, 2, SetType.WARMUP, 5, 500.0, null, now))
        val inProgress = withHeavy.copy(status = SessionStatus.IN_PROGRESS)
        assertEquals(100.0 * (1 + 5 / 30.0), WorkoutMath.bestE1rm("builtin:bench_press", listOf(warm, inProgress, withHeavy))!!, 1e-9)
        assertNull(WorkoutMath.bestE1rm("builtin:bench_press", listOf(inProgress)))
    }

    @Test fun tonnageSkipsUnknownValuesAndWarmups() {
        val t = template("tt", "builtin:bench_press")
        val s = completed("stt", mon, t, sets = 2)                  // 2 sets of 8 reps at 60 kg = 960 kg
        val withUnknown = s.copy(sets = s.sets + LoggedSet("u", s.exercises[0].id, 5, SetType.NORMAL, null, 100.0, null, now)
            + LoggedSet("w2", s.exercises[0].id, 6, SetType.WARMUP, 10, 40.0, null, now))
        assertEquals(960.0, WorkoutMath.tonnageKg(withUnknown), 1e-9)
    }

    @Test fun weekStartIsMonday() {
        assertEquals(mon, WorkoutMath.weekStart(wed))
        assertEquals(mon, WorkoutMath.weekStart(mon))
    }

    // ---------- scheduling and adherence ----------

    @Test fun fixedWeekdayScheduleAdherence() {
        val push = template("tp", "builtin:bench_press")
        val r = routine("r1",
            day("dMon", "Push", "tp", 1, order = 0),
            day("dWed", "Legs", "tl", 3, order = 1),
            day("dSat", "Rest", null, 6, rest = true, order = 2))
        val monDone = completed("sm", mon, push, sets = 1, routineId = "r1", dayId = "dMon")
        val wedSkipped = WorkoutSessions.skipRoutineDay("sk", r, r.days[1], wed, now)
        val adh = WorkoutMath.adherence(r, listOf(monDone, wedSkipped), mon, mon.plusDays(6), today = wed) as WorkoutMath.Adherence.Scheduled
        assertEquals(1, adh.completed)
        assertEquals(1, adh.skipped)
        assertEquals(0, adh.missed)
        assertEquals(0, adh.upcoming)
        assertEquals(2, adh.slots.size)  // rest day is not a training slot
    }

    @Test fun missedDayIsMissedNotFabricated() {
        val r = routine("r2", day("dMon2", "Push", "tp2", 1))
        val adh = WorkoutMath.adherence(r, emptyList(), mon, mon.plusDays(6), today = wed) as WorkoutMath.Adherence.Scheduled
        assertEquals(1, adh.missed)     // Monday is in the past with no record
        assertEquals(0, adh.completed)
    }

    @Test fun adHocCompletedSessionIsNotCountedAsPlanned() {
        val t = template("ta", "builtin:bench_press")
        val r = routine("r3", day("dMon3", "Push", "ta", 1))
        val adhoc = completed("sa", wed, t, sets = 1, routineId = "r3", dayId = "other")
        val adh = WorkoutMath.adherence(r, listOf(adhoc), mon, mon.plusDays(6), today = wed) as WorkoutMath.Adherence.Scheduled
        assertEquals(0, adh.completed)
        assertEquals(1, adh.unscheduledCompleted)
    }

    @Test fun flexibleRoutineReportsOnlyCompletedCount() {
        val t = template("tf", "builtin:bench_press")
        val r = routine("rf", day("a", "A", "tf", null, order = 0), day("b", "B", "tf", null, order = 1))
        val s = completed("sf", mon, t, sets = 1, routineId = "rf", dayId = "a")
        val adh = WorkoutMath.adherence(r, listOf(s), mon, mon.plusDays(6), today = wed)
        assertEquals(WorkoutMath.Adherence.Flexible(1), adh)
    }

    @Test fun flexibleRotationAdvancesAfterCompletedSession() {
        val t = template("tr", "builtin:bench_press")
        val r = routine("rr", day("a", "A", "tr", null, order = 0), day("b", "B", "tr", null, order = 1), day("c", "C", "tr", null, order = 2))
        assertEquals("a", WorkoutMath.nextFlexibleDay(r, emptyList())!!.id)
        val doneA = completed("sr1", mon, t, sets = 1, routineId = "rr", dayId = "a")
        assertEquals("b", WorkoutMath.nextFlexibleDay(r, listOf(doneA))!!.id)
        val doneC = completed("sr2", mon.plusDays(1), t, sets = 1, routineId = "rr", dayId = "c")
        assertEquals("a", WorkoutMath.nextFlexibleDay(r, listOf(doneA, doneC))!!.id)  // wraps after C
    }

    // ---------- validation ----------

    @Test fun prescriptionValidation() {
        val ok = Prescription()
        assertTrue(WorkoutRules.validatePrescription(ok).isEmpty())
        assertFalse(WorkoutRules.validatePrescription(ok.copy(targetSets = 0)).isEmpty())
        assertFalse(WorkoutRules.validatePrescription(ok.copy(repMin = 12, repMax = 8)).isEmpty())
        assertFalse(WorkoutRules.validatePrescription(ok.copy(targetLoadKg = 0.0)).isEmpty())
        assertFalse(WorkoutRules.validatePrescription(ok.copy(restSeconds = -5)).isEmpty())
        assertFalse(WorkoutRules.validatePrescription(ok.copy(targetRpe = 11.0)).isEmpty())
    }

    @Test fun nameValidation() {
        assertFalse(WorkoutRules.validateName("Workout name", "  ").isEmpty())
        assertFalse(WorkoutRules.validateName("Workout name", "x".repeat(61)).isEmpty())
        assertTrue(WorkoutRules.validateName("Workout name", "Push A").isEmpty())
    }

    // ---------- template editing ----------

    @Test fun templateReorderDuplicateRemoveKeepsContiguousOrder() {
        var t = template("te", "builtin:bench_press", "builtin:overhead_press", "builtin:pull_up")
        t = WorkoutRules.moveExercise(t, "te-row2", 0, now)
        assertEquals(listOf("builtin:pull_up", "builtin:bench_press", "builtin:overhead_press"), t.exercises.map { it.exerciseId })
        assertEquals(listOf(0, 1, 2), t.exercises.map { it.orderIndex })
        // After the move the bench row is at index 1 (id te-row0); its copy must land directly after it.
        t = WorkoutRules.duplicateExercise(t, "te-row0", "te-copy", now)
        assertEquals(4, t.exercises.size)
        assertEquals("te-copy", t.exercises[2].id)
        assertEquals("builtin:bench_press", t.exercises[2].exerciseId)
        assertEquals(listOf(0, 1, 2, 3), t.exercises.map { it.orderIndex })
        t = WorkoutRules.removeExercise(t, "te-row0", now)
        assertEquals(listOf(0, 1, 2), t.exercises.map { it.orderIndex })
    }

    @Test fun duplicateTemplateGetsNewIdsAndLeavesSourceUnchanged() {
        val src = template("ts", "builtin:bench_press", "builtin:pull_up")
        val copy = WorkoutRules.duplicateTemplate(src, "ts2", listOf("x1", "x2"), "Push copy", now)
        assertEquals("ts2", copy.id)
        assertEquals(listOf("x1", "x2"), copy.exercises.map { it.id })
        assertEquals(listOf("ts-row0", "ts-row1"), src.exercises.map { it.id })
        assertNull(copy.archivedAtMs)
    }

    @Test fun editingPrescriptionDoesNotChangeOtherRows() {
        val t = template("tp3", "builtin:bench_press", "builtin:pull_up")
        val edited = WorkoutRules.updatePrescription(t, "tp3-row0", Prescription(targetSets = 5), now)
        assertEquals(5, edited.exercises[0].prescription.targetSets)
        assertEquals(t.exercises[1], edited.exercises[1])
    }

    // ---------- routines ----------

    @Test fun activationRequiresTrainingDayAssignedAndUsable() {
        val t = template("ta1", "builtin:bench_press")
        val templates = mapOf("ta1" to t)
        val none = routine("ra")
        assertFalse(WorkoutRules.validateForActivation(none, templates).isEmpty())
        val unassigned = routine("rb", day("d1", "Push", null, 1))
        assertTrue(WorkoutRules.validateForActivation(unassigned, templates).any { it is WorkoutRules.Problem.UnassignedDay })
        val archived = WorkoutRules.archiveTemplate(t, now)
        val withArchived = routine("rc", day("d2", "Push", "ta1", 1))
        assertTrue(WorkoutRules.validateForActivation(withArchived, mapOf("ta1" to archived)).any { it is WorkoutRules.Problem.ArchivedTemplate })
        val ok = routine("rd", day("d3", "Push", "ta1", 1), day("d4", "Rest", null, 2, rest = true, order = 1))
        assertTrue(WorkoutRules.validateForActivation(ok, templates).isEmpty())
    }

    @Test fun duplicateWeekdayIsDetected() {
        val t = template("tw2", "builtin:bench_press")
        val r = routine("re", day("x", "A", "tw2", 1), day("y", "B", "tw2", 1, order = 1))
        assertTrue(WorkoutRules.validateRoutine(r, mapOf("tw2" to t)).any { it is WorkoutRules.Problem.DuplicateWeekday })
    }

    @Test fun arbitrarySplitWithCustomNamesAndRestDays() {
        var r = WorkoutRules.newRoutine("rs", "Hybrid", now)
        r = WorkoutRules.addDay(r, "d1", "Push A", false, "tid", 1, now)
        r = WorkoutRules.addDay(r, "d2", "Recovery walk", true, null, null, now)
        r = WorkoutRules.addDay(r, "d3", "Conditioning", false, "tid", null, now)
        assertEquals(2, WorkoutRules.trainingDayCount(r))
        assertTrue(WorkoutRules.isFixedSchedule(r))
        assertEquals("Recovery walk", r.days[1].label)
        assertNull(r.days[1].templateId)
    }

    @Test fun statusTransitionsAreRestricted() {
        val t = template("tst", "builtin:bench_press")
        val templates = mapOf("tst" to t)
        val r = routine("rst", day("dd", "Push", "tst", 1))
        val active = WorkoutRules.changeStatus(r, RoutineStatus.ACTIVE, templates, now).getOrThrow()
        assertEquals(RoutineStatus.ACTIVE, active.status)
        val paused = WorkoutRules.changeStatus(active, RoutineStatus.PAUSED, templates, now).getOrThrow()
        assertTrue(WorkoutRules.changeStatus(paused, RoutineStatus.DRAFT, templates, now).isFailure)
        val archived = WorkoutRules.changeStatus(paused, RoutineStatus.ARCHIVED, templates, now).getOrThrow()
        assertEquals(RoutineStatus.DRAFT, WorkoutRules.changeStatus(archived, RoutineStatus.DRAFT, templates, now).getOrThrow().status)
        assertTrue(WorkoutRules.changeStatus(routine("bad"), RoutineStatus.ACTIVE, templates, now).isFailure)
    }

    @Test fun activatingOneRoutinePausesOthers() {
        val target = routine("new").copy(status = RoutineStatus.ACTIVE)
        val old = routine("old").copy(status = RoutineStatus.ACTIVE)
        val paused = routine("p").copy(status = RoutineStatus.PAUSED)
        val changed = WorkoutRules.activateExclusively(target, listOf(old, paused), now)
        assertEquals(listOf("old"), changed.map { it.id })
        assertEquals(RoutineStatus.PAUSED, changed[0].status)
    }

    @Test fun removingTemplateReferencesLeavesRoutineNeedingFix() {
        val t = template("tu", "builtin:bench_press")
        val r = routine("ru", day("du", "Push", "tu", 1))
        assertEquals(listOf("ru"), WorkoutRules.routinesReferencing("tu", listOf(r)).map { it.id })
        val fixed = WorkoutRules.unassignTemplate("tu", listOf(r), now)[0]
        assertNull(fixed.days[0].templateId)
        assertTrue(WorkoutRules.validateForActivation(fixed, mapOf("tu" to t)).any { it is WorkoutRules.Problem.UnassignedDay })
    }

    @Test fun routineDuplicateGetsNewIdsAndKeepsTemplateReferences() {
        val r = routine("ro", day("d-a", "Push", "tx", 1), day("d-b", "Push again", "tx", null, order = 1))
        val copy = WorkoutRules.duplicateRoutine(r, "ro2", listOf("n1", "n2"), "Copy", now)
        assertEquals("ro2", copy.id)
        assertEquals(listOf("n1", "n2"), copy.days.map { it.id })
        assertEquals(listOf("tx", "tx"), copy.days.map { it.templateId })     // same template twice is allowed
        assertEquals(RoutineStatus.DRAFT, copy.status)
        assertEquals("d-a", r.days[0].id)
    }

    // ---------- sessions ----------

    @Test fun sessionSnapshotIsImmuneToTemplateEdits() {
        val t = template("tsn", "builtin:bench_press")
        val s = WorkoutSessions.startFromTemplate("sn", t, catalogue, mon, now, rowIdFor = { "sn-${it.id}" }).getOrThrow()
        val edited = WorkoutRules.updatePrescription(t, "tsn-row0", Prescription(targetSets = 9, repMin = 1, repMax = 2), now)
        assertEquals(3, s.exercises[0].prescription.targetSets)   // session keeps the original
        assertEquals(9, edited.exercises[0].prescription.targetSets)
    }

    @Test fun cannotStartEmptyOrArchivedTemplate() {
        val empty = WorkoutRules.newTemplate("te2", "Empty", now)
        assertTrue(WorkoutSessions.startFromTemplate("s", empty, catalogue, mon, now, rowIdFor = { "r" }).isFailure)
        val arch = WorkoutRules.archiveTemplate(template("ta2", "builtin:bench_press"), now)
        assertTrue(WorkoutSessions.startFromTemplate("s2", arch, catalogue, mon, now, rowIdFor = { "r" }).isFailure)
    }

    @Test fun recordSetRejectsInvalidAndDuplicateSets() {
        val t = template("tr2", "builtin:bench_press")
        var s = WorkoutSessions.startFromTemplate("sr", t, catalogue, mon, now, rowIdFor = { "sr-${it.id}" }).getOrThrow()
        val row = s.exercises[0].id
        s = WorkoutSessions.recordSet(s, LoggedSet("a", row, 0, SetType.NORMAL, 8, 60.0, 7.0, now)).getOrThrow()
        assertTrue(WorkoutSessions.recordSet(s, LoggedSet("b", row, 0, SetType.NORMAL, 8, 60.0, null, now)).isFailure)
        assertTrue(WorkoutSessions.recordSet(s, LoggedSet("c", row, 1, SetType.NORMAL, 0, 60.0, null, now)).isFailure)
        assertTrue(WorkoutSessions.recordSet(s, LoggedSet("d", row, 1, SetType.NORMAL, 8, -1.0, null, now)).isFailure)
        assertTrue(WorkoutSessions.recordSet(s, LoggedSet("e", row, 1, SetType.NORMAL, 8, 60.0, 11.0, now)).isFailure)
        assertTrue(WorkoutSessions.recordSet(s, LoggedSet("f", "nope", 0, SetType.NORMAL, 8, 60.0, null, now)).isFailure)
        val finished = WorkoutSessions.finish(s, now).getOrThrow()
        assertTrue(WorkoutSessions.recordSet(finished, LoggedSet("g", row, 1, SetType.NORMAL, 8, 60.0, null, now)).isFailure)
    }

    @Test fun finishNeedsWorkingSetAndWarmupDoesNotCount() {
        val t = template("tf2", "builtin:bench_press")
        var s = WorkoutSessions.startFromTemplate("sf2", t, catalogue, mon, now, rowIdFor = { "sf2-${it.id}" }).getOrThrow()
        val row = s.exercises[0].id
        s = WorkoutSessions.recordSet(s, LoggedSet("w", row, 0, SetType.WARMUP, 10, 20.0, null, now)).getOrThrow()
        assertTrue(WorkoutSessions.finish(s, now).isFailure)
        assertEquals(SessionStatus.ABANDONED, WorkoutSessions.abandon(s, now).getOrThrow().status)
    }

    // ---------- persistence contract (in-memory reference) ----------

    @Test fun repositoryRoundTripAndArchiveFiltering() {
        val repo = InMemoryWorkoutRepository()
        val t = template("rt", "builtin:bench_press")
        repo.saveTemplate(t)
        assertEquals(t, repo.template("rt"))
        repo.saveTemplate(WorkoutRules.archiveTemplate(t, now))
        assertTrue(repo.templates(includeArchived = false).isEmpty())
        assertEquals(1, repo.templates(includeArchived = true).size)
    }

    @Test fun templateCannotBeDeletedWhileRoutineUsesIt() {
        val repo = InMemoryWorkoutRepository()
        val t = template("rd1", "builtin:bench_press")
        repo.saveTemplate(t)
        repo.saveRoutine(routine("rr1", day("dd1", "Push", "rd1", 1)))
        try { repo.deleteTemplate("rd1"); fail("expected storage error") } catch (e: com.pranav.focus.workout.data.WorkoutStorageException) { }
        repo.saveRoutine(WorkoutRules.unassignTemplate("rd1", repo.routines(), now)[0])
        repo.deleteTemplate("rd1")
        assertNull(repo.template("rd1"))
    }

    @Test fun deletingRoutineKeepsSessionHistory() {
        val repo = InMemoryWorkoutRepository()
        val t = template("hs", "builtin:bench_press")
        repo.saveTemplate(t)
        val r = routine("hr", day("hd", "Push", "hs", 1))
        repo.saveRoutine(r)
        val s = completed("hsess", mon, t, sets = 2, routineId = "hr", dayId = "hd")
        repo.saveSession(s)
        repo.deleteRoutine("hr")
        assertNull(repo.routine("hr"))
        assertNotNull(repo.session("hsess"))
        assertEquals("hr", repo.session("hsess")!!.routineId)
    }

    @Test fun editingTemplateAfterSessionDoesNotRewriteSession() {
        val repo = InMemoryWorkoutRepository()
        val t = template("hi", "builtin:bench_press")
        repo.saveTemplate(t)
        val s = completed("hisess", mon, t, sets = 2)
        repo.saveSession(s)
        repo.saveTemplate(WorkoutRules.updatePrescription(t, "hi-row0", Prescription(targetSets = 8), now))
        assertEquals(3, repo.session("hisess")!!.exercises[0].prescription.targetSets)
    }

    @Test fun customExerciseMustBeCustomAndPersist() {
        val repo = InMemoryWorkoutRepository()
        val e = Exercise("custom:cable_kick", "Cable kickback", setOf(MuscleGroup.TRICEPS), true)
        repo.saveCustomExercise(e)
        assertEquals(e, repo.exercise("custom:cable_kick"))
        try { repo.saveCustomExercise(Exercise("builtin:x", "x", emptySet(), false)); fail("expected rejection") } catch (ex: IllegalArgumentException) { }
    }
}
