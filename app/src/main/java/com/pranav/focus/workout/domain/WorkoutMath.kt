package com.pranav.focus.workout.domain

import java.time.LocalDate

/** Set-count bands for the muscle view: 0–9 low, 10–20 in range, 21+ high. Null (unknown) never maps to a band. */
enum class SetBand(val label: String, val range: String) {
    LOW("Low", "0–9"),
    IN_RANGE("In range", "10–20"),
    HIGH("High", "21+"),
}

object WorkoutMath {

    fun setBandFor(completedSets: Int?): SetBand? {
        if (completedSets == null) return null
        require(completedSets >= 0) { "completedSets must be >= 0, was $completedSets" }
        return when {
            completedSets <= 9 -> SetBand.LOW
            completedSets <= 20 -> SetBand.IN_RANGE
            else -> SetBand.HIGH
        }
    }

    /**
     * Epley estimated one-rep max: load * (1 + reps / 30). Reps == 1 returns the load.
     * Returns null when the inputs can't support an estimate (no load, reps outside 1..15).
     * Only applied to NORMAL and FAILURE sets: warm-ups and drop sets are not maximal efforts.
     */
    fun e1rm(loadKg: Double?, reps: Int?, type: SetType): Double? {
        if (type != SetType.NORMAL && type != SetType.FAILURE) return null
        if (loadKg == null || reps == null) return null
        if (loadKg <= 0.0 || reps !in 1..15) return null
        return if (reps == 1) loadKg else loadKg * (1 + reps / 30.0)
    }

    /** Sets that count toward work and muscle statistics. Warm-ups never count. */
    fun isWorkingSet(s: LoggedSet): Boolean = s.type != SetType.WARMUP

    /** Only completed sessions count. Planned, skipped, abandoned and in-progress sessions never feed statistics. */
    fun completedIn(sessions: List<WorkoutSession>, from: LocalDate, to: LocalDate): List<WorkoutSession> =
        sessions.filter { it.status == SessionStatus.COMPLETED && !it.date.isBefore(from) && !it.date.isAfter(to) }

    data class MuscleSets(
        /** Working sets per muscle. A muscle with no sets is a KNOWN zero, because the sessions in the window are known. */
        val byMuscle: Map<MuscleGroup, Int>,
        /** Working sets whose exercise has no muscle mapping (e.g. a custom exercise with no muscles chosen). Not allocated. */
        val unmappedSets: Int,
    )

    /**
     * Counts working sets per primary muscle from COMPLETED sessions only. Each working set credits 1 to each primary
     * muscle of its exercise. The exercise definition comes from [catalogue]; a session's exercise snapshot does not
     * carry muscles, so the catalogue lookup is the single source for mapping.
     */
    fun hardSetsByMuscle(sessions: List<WorkoutSession>, catalogue: Map<String, Exercise>, from: LocalDate, to: LocalDate): MuscleSets {
        val counts = HashMap<MuscleGroup, Int>()
        var unmapped = 0
        for (session in completedIn(sessions, from, to)) {
            val exerciseByRow = session.exercises.associateBy { it.id }
            for (set in session.sets) {
                if (!isWorkingSet(set)) continue
                val row = exerciseByRow[set.sessionExerciseId] ?: continue
                val muscles = catalogue[row.exerciseId]?.primaryMuscles.orEmpty()
                if (muscles.isEmpty()) { unmapped++; continue }
                muscles.forEach { counts[it] = (counts[it] ?: 0) + 1 }
            }
        }
        return MuscleSets(MuscleGroup.values().associateWith { counts[it] ?: 0 }, unmapped)
    }

    /** Tonnage for one session: sum of load * reps over working sets that have both values. Unknown values are skipped, never zero-filled. */
    fun tonnageKg(s: WorkoutSession): Double = s.sets
        .filter { isWorkingSet(it) }
        .sumOf { set -> val l = set.loadKg; val r = set.reps; if (l != null && r != null) l * r else 0.0 }

    /** Monday of the week containing [date] (the app's week starts on Monday). */
    fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

    /** Best e1RM across completed sessions for one exercise. Null when there is no estimable set. */
    fun bestE1rm(exerciseId: String, sessions: List<WorkoutSession>): Double? {
        var best: Double? = null
        for (session in sessions.filter { it.status == SessionStatus.COMPLETED }) {
            val rows = session.exercises.filter { it.exerciseId == exerciseId }.map { it.id }.toSet()
            for (set in session.sets) {
                if (set.sessionExerciseId !in rows) continue
                val e = e1rm(set.loadKg, set.reps, set.type) ?: continue
                if (best == null || e > best) best = e
            }
        }
        return best
    }

    // ---- scheduling ----

    /** Fixed-weekday routines: the day scheduled on [date]. Null when the routine is flexible or has no day that weekday. */
    fun dayForDate(routine: WorkoutRoutine, date: LocalDate): RoutineDay? {
        if (!WorkoutRules.isFixedSchedule(routine)) return null
        return routine.days.firstOrNull { it.weekday == date.dayOfWeek.value }
    }

    /**
     * Flexible routines (no fixed weekdays): the next training day after the last completed routine session.
     * Wraps around. With no completed session yet, it is the first training day.
     */
    fun nextFlexibleDay(routine: WorkoutRoutine, sessions: List<WorkoutSession>): RoutineDay? {
        val training = routine.days.filter { !it.isRestDay }.sortedBy { it.orderIndex }
        if (training.isEmpty()) return null
        // Latest = most recent date, then most recent completion. Ties on timestamp are resolved by date, not by list order.
        val last = sessions
            .filter { it.routineId == routine.id && it.status == SessionStatus.COMPLETED }
            .maxWithOrNull(compareBy<WorkoutSession>({ it.date }, { it.completedAtMs ?: it.startedAtMs }))
        val idx = training.indexOfFirst { it.id == last?.routineDayId }
        return if (idx < 0) training.first() else training[(idx + 1) % training.size]
    }

    sealed class Adherence {
        /** Fixed-weekday routine: every scheduled training day in the window has a status. */
        data class Scheduled(
            val slots: List<Slot>,
            val completed: Int,
            val skipped: Int,
            val missed: Int,
            val upcoming: Int,
            /** Completed sessions that were not on a scheduled slot (ad-hoc). Reported, never counted as planned. */
            val unscheduledCompleted: Int,
        ) : Adherence()

        /** Flexible routine: there is no planned calendar, so only completed sessions are known. */
        data class Flexible(val completed: Int) : Adherence()
    }

    enum class SlotStatus { COMPLETED, SKIPPED, MISSED, UPCOMING }
    data class Slot(val date: LocalDate, val routineDayId: String, val label: String, val status: SlotStatus)

    /**
     * Planned-versus-completed adherence from real saved sessions only. A day that is in the past with no completed
     * or skipped session is MISSED. Today and future days are UPCOMING. Nothing is inferred.
     */
    fun adherence(routine: WorkoutRoutine, sessions: List<WorkoutSession>, from: LocalDate, to: LocalDate, today: LocalDate): Adherence {
        val routineSessions = sessions.filter { it.routineId == routine.id }
        if (!WorkoutRules.isFixedSchedule(routine)) {
            return Adherence.Flexible(routineSessions.count { it.status == SessionStatus.COMPLETED && !it.date.isBefore(from) && !it.date.isAfter(to) })
        }
        val slots = mutableListOf<Slot>()
        val scheduledKeys = HashSet<Pair<LocalDate, String>>()
        var d = from
        while (!d.isAfter(to)) {
            val day = dayForDate(routine, d)
            if (day != null && !day.isRestDay) {
                scheduledKeys.add(d to day.id)
                val sameSlot = routineSessions.filter { it.date == d && it.routineDayId == day.id }
                val status = when {
                    sameSlot.any { it.status == SessionStatus.COMPLETED } -> SlotStatus.COMPLETED
                    sameSlot.any { it.status == SessionStatus.SKIPPED } -> SlotStatus.SKIPPED
                    d.isBefore(today) -> SlotStatus.MISSED
                    else -> SlotStatus.UPCOMING
                }
                slots.add(Slot(d, day.id, day.label, status))
            }
            d = d.plusDays(1)
        }
        val unscheduled = routineSessions.count { s ->
            s.status == SessionStatus.COMPLETED && !s.date.isBefore(from) && !s.date.isAfter(to) &&
                (s.date to (s.routineDayId ?: "")) !in scheduledKeys
        }
        return Adherence.Scheduled(
            slots = slots,
            completed = slots.count { it.status == SlotStatus.COMPLETED },
            skipped = slots.count { it.status == SlotStatus.SKIPPED },
            missed = slots.count { it.status == SlotStatus.MISSED },
            upcoming = slots.count { it.status == SlotStatus.UPCOMING },
            unscheduledCompleted = unscheduled,
        )
    }
}

/** Session state machine. Each function returns a new session or a failure. Nothing is mutated. */
object WorkoutSessions {

    fun startFromTemplate(
        sessionId: String,
        template: WorkoutTemplate,
        catalogue: Map<String, Exercise>,
        date: LocalDate,
        now: Long,
        routine: WorkoutRoutine? = null,
        routineDay: RoutineDay? = null,
        rowIdFor: (TemplateExercise) -> String,
    ): Result<WorkoutSession> {
        WorkoutRules.canStart(template).firstOrNull()?.let { return Result.failure(IllegalStateException(it.message)) }
        val rows = mutableListOf<SessionExercise>()
        for (te in template.exercises.sortedBy { it.orderIndex }) {
            val ex = catalogue[te.exerciseId]
                ?: return Result.failure(IllegalStateException("An exercise in this workout is missing from the library"))
            // Snapshot: name and prescription are copied, so later template edits can't change this session.
            rows.add(SessionExercise(rowIdFor(te), ex.id, ex.name, rows.size, te.prescription))
        }
        return Result.success(WorkoutSession(
            id = sessionId, date = date, templateId = template.id, templateName = template.name,
            routineId = routine?.id, routineDayId = routineDay?.id,
            status = SessionStatus.IN_PROGRESS, startedAtMs = now, completedAtMs = null,
            exercises = rows, sets = emptyList(),
        ))
    }

    /** Creates a SKIPPED session so the day is recorded as skipped rather than missed. No sets. */
    fun skipRoutineDay(sessionId: String, routine: WorkoutRoutine, day: RoutineDay, date: LocalDate, now: Long): WorkoutSession =
        WorkoutSession(sessionId, date, day.templateId, day.label, routine.id, day.id,
            SessionStatus.SKIPPED, now, now, emptyList(), emptyList())

    fun recordSet(s: WorkoutSession, set: LoggedSet): Result<WorkoutSession> {
        if (s.status != SessionStatus.IN_PROGRESS) return Result.failure(IllegalStateException("Only an in-progress session can record sets"))
        if (s.exercises.none { it.id == set.sessionExerciseId }) return Result.failure(IllegalArgumentException("Unknown exercise row"))
        if (s.sets.any { it.id == set.id }) return Result.failure(IllegalArgumentException("Duplicate set id"))
        if (s.sets.any { it.sessionExerciseId == set.sessionExerciseId && it.setIndex == set.setIndex })
            return Result.failure(IllegalArgumentException("Set ${set.setIndex + 1} is already logged"))
        set.reps?.let { if (it !in 1..WorkoutRules.MAX_REP) return Result.failure(IllegalArgumentException("Reps must be 1 to ${WorkoutRules.MAX_REP}")) }
        set.loadKg?.let { if (it < 0.0 || it > WorkoutRules.MAX_LOAD_KG || it.isNaN()) return Result.failure(IllegalArgumentException("Load must be 0 to ${WorkoutRules.MAX_LOAD_KG} kg")) }
        set.rpe?.let { if (it < WorkoutRules.MIN_RPE || it > WorkoutRules.MAX_RPE) return Result.failure(IllegalArgumentException("RPE must be 1 to 10")) }
        return Result.success(s.copy(sets = s.sets + set))
    }

    fun finish(s: WorkoutSession, now: Long): Result<WorkoutSession> {
        if (s.status != SessionStatus.IN_PROGRESS) return Result.failure(IllegalStateException("Only an in-progress session can be finished"))
        if (s.sets.none { it.type != SetType.WARMUP }) return Result.failure(IllegalStateException("Log at least one working set, or abandon the session"))
        return Result.success(s.copy(status = SessionStatus.COMPLETED, completedAtMs = now))
    }

    fun abandon(s: WorkoutSession, now: Long): Result<WorkoutSession> {
        if (s.status != SessionStatus.IN_PROGRESS) return Result.failure(IllegalStateException("Only an in-progress session can be abandoned"))
        return Result.success(s.copy(status = SessionStatus.ABANDONED, completedAtMs = now))
    }
}
