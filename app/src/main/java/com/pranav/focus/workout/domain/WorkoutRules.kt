package com.pranav.focus.workout.domain

import java.time.LocalDate

/** Validation and pure editing operations for templates and routines. Every edit returns a new value. */
object WorkoutRules {

    const val NAME_MAX = 60
    const val NOTES_MAX = 500
    const val LABEL_MAX = 40
    const val MAX_EXERCISES_PER_TEMPLATE = 40
    const val MAX_DAYS_PER_ROUTINE = 28
    const val MAX_SETS = 10
    const val MAX_REP = 100
    const val MAX_LOAD_KG = 1000.0
    const val MAX_REST_SECONDS = 1800
    const val MIN_RPE = 1.0
    const val MAX_RPE = 10.0
    const val MAX_DURATION_WEEKS = 52

    sealed class Problem(val message: String) {
        class Blank(field: String) : Problem("$field can't be empty")
        class TooLong(field: String, max: Int) : Problem("$field must be $max characters or fewer")
        class OutOfRange(field: String, min: String, max: String) : Problem("$field must be between $min and $max")
        class RepRangeInverted : Problem("Minimum reps can't be more than maximum reps")
        class TooMany(what: String, max: Int) : Problem("At most $max $what")
        class NoExercises : Problem("Add at least one exercise before starting")
        class NoTrainingDays : Problem("Add at least one training day")
        class UnassignedDay(label: String) : Problem("Training day \"$label\" has no workout assigned")
        class MissingTemplate(label: String) : Problem("Training day \"$label\" uses a workout that no longer exists")
        class ArchivedTemplate(label: String) : Problem("Training day \"$label\" uses an archived workout")
        class DuplicateWeekday(day: String) : Problem("$day is used by more than one training day")
        class BadStatus(from: RoutineStatus, to: RoutineStatus) : Problem("A routine can't go from ${from.name.lowercase()} to ${to.name.lowercase()}")
    }

    // ---- field validation ----

    fun validateName(field: String, value: String, max: Int = NAME_MAX): List<Problem> = when {
        value.isBlank() -> listOf(Problem.Blank(field))
        value.trim().length > max -> listOf(Problem.TooLong(field, max))
        else -> emptyList()
    }

    fun validateNotes(value: String): List<Problem> =
        if (value.length > NOTES_MAX) listOf(Problem.TooLong("Notes", NOTES_MAX)) else emptyList()

    fun validatePrescription(p: Prescription): List<Problem> {
        val out = mutableListOf<Problem>()
        if (p.targetSets !in 1..MAX_SETS) out.add(Problem.OutOfRange("Sets", "1", "$MAX_SETS"))
        if (p.repMin !in 1..MAX_REP) out.add(Problem.OutOfRange("Minimum reps", "1", "$MAX_REP"))
        if (p.repMax !in 1..MAX_REP) out.add(Problem.OutOfRange("Maximum reps", "1", "$MAX_REP"))
        if (p.repMin in 1..MAX_REP && p.repMax in 1..MAX_REP && p.repMin > p.repMax) out.add(Problem.RepRangeInverted())
        p.targetLoadKg?.let { if (it <= 0.0 || it > MAX_LOAD_KG || it.isNaN()) out.add(Problem.OutOfRange("Target load (kg)", "0.5", "$MAX_LOAD_KG")) }
        p.restSeconds?.let { if (it !in 0..MAX_REST_SECONDS) out.add(Problem.OutOfRange("Rest (seconds)", "0", "$MAX_REST_SECONDS")) }
        p.targetRpe?.let { if (it < MIN_RPE || it > MAX_RPE) out.add(Problem.OutOfRange("Target RPE", "$MIN_RPE", "$MAX_RPE")) }
        out.addAll(validateNotes(p.notes))
        return out
    }

    /** Template validation. Saving a draft with zero exercises is allowed; starting it is not (see canStart). */
    fun validateTemplate(t: WorkoutTemplate): List<Problem> {
        val out = mutableListOf<Problem>()
        out += validateName("Workout name", t.name)
        out += validateNotes(t.notes)
        if (t.exercises.size > MAX_EXERCISES_PER_TEMPLATE) out.add(Problem.TooMany("exercises", MAX_EXERCISES_PER_TEMPLATE))
        t.exercises.forEach { out += validatePrescription(it.prescription) }
        return out
    }

    fun canStart(t: WorkoutTemplate): List<Problem> =
        buildList {
            if (t.archivedAtMs != null) add(Problem.Blank("This workout is archived; restore it to start"))
            if (t.exercises.isEmpty()) add(Problem.NoExercises())
        }

    // ---- template editing (pure) ----

    private fun reindex(list: List<TemplateExercise>) = list.mapIndexed { i, e -> e.copy(orderIndex = i) }

    fun newTemplate(id: String, name: String, now: Long): WorkoutTemplate =
        WorkoutTemplate(id, name.trim(), "", emptyList(), now, now)

    fun addExercise(t: WorkoutTemplate, rowId: String, exerciseId: String, now: Long,
                    prescription: Prescription = Prescription()): WorkoutTemplate =
        t.copy(exercises = reindex(t.exercises + TemplateExercise(rowId, exerciseId, t.exercises.size, prescription)), updatedAtMs = now)

    fun removeExercise(t: WorkoutTemplate, rowId: String, now: Long): WorkoutTemplate =
        t.copy(exercises = reindex(t.exercises.filterNot { it.id == rowId }), updatedAtMs = now)

    /** Moves a row to [toIndex] (clamped). Order indexes are always re-numbered 0..n-1. */
    fun moveExercise(t: WorkoutTemplate, rowId: String, toIndex: Int, now: Long): WorkoutTemplate {
        val from = t.exercises.indexOfFirst { it.id == rowId }
        if (from < 0) return t
        val mutable = t.exercises.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(toIndex.coerceIn(0, mutable.size), item)
        return t.copy(exercises = reindex(mutable), updatedAtMs = now)
    }

    /** Duplicates a row directly after the original, with a NEW row id. */
    fun duplicateExercise(t: WorkoutTemplate, rowId: String, newRowId: String, now: Long): WorkoutTemplate {
        val idx = t.exercises.indexOfFirst { it.id == rowId }
        if (idx < 0) return t
        val src = t.exercises[idx]
        val list = t.exercises.toMutableList()
        list.add(idx + 1, src.copy(id = newRowId))
        return t.copy(exercises = reindex(list), updatedAtMs = now)
    }

    fun replaceExercise(t: WorkoutTemplate, rowId: String, newExerciseId: String, now: Long): WorkoutTemplate =
        t.copy(exercises = t.exercises.map { if (it.id == rowId) it.copy(exerciseId = newExerciseId) else it }, updatedAtMs = now)

    fun updatePrescription(t: WorkoutTemplate, rowId: String, p: Prescription, now: Long): WorkoutTemplate =
        t.copy(exercises = t.exercises.map { if (it.id == rowId) it.copy(prescription = p) else it }, updatedAtMs = now)

    /**
     * Duplicates a whole template with new ids. [newRowIds] must supply one id per exercise row, in order.
     * The duplicate is not archived and is not linked to any routine.
     */
    fun duplicateTemplate(t: WorkoutTemplate, newId: String, newRowIds: List<String>, newName: String, now: Long): WorkoutTemplate {
        require(newRowIds.size == t.exercises.size) { "need one row id per exercise" }
        val rows = t.exercises.mapIndexed { i, e -> e.copy(id = newRowIds[i]) }
        return WorkoutTemplate(newId, newName.trim(), t.notes, rows, now, now, null)
    }

    fun archiveTemplate(t: WorkoutTemplate, now: Long): WorkoutTemplate = t.copy(archivedAtMs = now, updatedAtMs = now)
    fun restoreTemplate(t: WorkoutTemplate, now: Long): WorkoutTemplate = t.copy(archivedAtMs = null, updatedAtMs = now)

    // ---- routines ----

    fun validateRoutine(r: WorkoutRoutine, templates: Map<String, WorkoutTemplate>): List<Problem> {
        val out = mutableListOf<Problem>()
        out += validateName("Routine name", r.name)
        if (r.description.length > NOTES_MAX) out.add(Problem.TooLong("Description", NOTES_MAX))
        r.durationWeeks?.let { if (it !in 1..MAX_DURATION_WEEKS) out.add(Problem.OutOfRange("Duration (weeks)", "1", "$MAX_DURATION_WEEKS")) }
        if (r.days.size > MAX_DAYS_PER_ROUTINE) out.add(Problem.TooMany("days", MAX_DAYS_PER_ROUTINE))
        r.days.forEach { d ->
            out += validateName("Day name", d.label, LABEL_MAX)
            d.weekday?.let { if (it !in 1..7) out.add(Problem.OutOfRange("Weekday", "1", "7")) }
        }
        val weekdays = r.days.filter { !it.isRestDay }.mapNotNull { it.weekday }
        weekdays.groupBy { it }.filter { it.value.size > 1 }.keys.forEach { wd ->
            out.add(Problem.DuplicateWeekday(java.time.DayOfWeek.of(wd).name.lowercase().replaceFirstChar { it.uppercase() }))
        }
        return out
    }

    /** Problems that block ACTIVATION (a draft may be saved with problems). */
    fun validateForActivation(r: WorkoutRoutine, templates: Map<String, WorkoutTemplate>): List<Problem> {
        val out = validateRoutine(r, templates).toMutableList()
        val training = r.days.filter { !it.isRestDay }
        if (training.isEmpty()) out.add(Problem.NoTrainingDays())
        training.forEach { d ->
            val tid = d.templateId
            when {
                tid == null -> out.add(Problem.UnassignedDay(d.label))
                templates[tid] == null -> out.add(Problem.MissingTemplate(d.label))
                templates[tid]!!.archivedAtMs != null -> out.add(Problem.ArchivedTemplate(d.label))
            }
        }
        return out
    }

    private fun reorderDays(days: List<RoutineDay>) = days.mapIndexed { i, d -> d.copy(orderIndex = i) }

    fun newRoutine(id: String, name: String, now: Long): WorkoutRoutine =
        WorkoutRoutine(id, name.trim(), "", null, null, RoutineStatus.DRAFT, emptyList(), now, now)

    fun addDay(r: WorkoutRoutine, dayId: String, label: String, isRest: Boolean, templateId: String?, weekday: Int?, now: Long): WorkoutRoutine =
        r.copy(days = reorderDays(r.days + RoutineDay(dayId, r.days.size, label.trim(), isRest, if (isRest) null else templateId, weekday)), updatedAtMs = now)

    fun removeDay(r: WorkoutRoutine, dayId: String, now: Long): WorkoutRoutine =
        r.copy(days = reorderDays(r.days.filterNot { it.id == dayId }), updatedAtMs = now)

    fun moveDay(r: WorkoutRoutine, dayId: String, toIndex: Int, now: Long): WorkoutRoutine {
        val from = r.days.indexOfFirst { it.id == dayId }
        if (from < 0) return r
        val list = r.days.toMutableList()
        val item = list.removeAt(from)
        list.add(toIndex.coerceIn(0, list.size), item)
        return r.copy(days = reorderDays(list), updatedAtMs = now)
    }

    fun duplicateDay(r: WorkoutRoutine, dayId: String, newDayId: String, now: Long): WorkoutRoutine {
        val idx = r.days.indexOfFirst { it.id == dayId }
        if (idx < 0) return r
        val list = r.days.toMutableList()
        // A copy must not create a duplicate fixed weekday, so the copy is made flexible.
        list.add(idx + 1, r.days[idx].copy(id = newDayId, weekday = null))
        return r.copy(days = reorderDays(list), updatedAtMs = now)
    }

    fun updateDay(r: WorkoutRoutine, dayId: String, label: String, isRest: Boolean, templateId: String?, weekday: Int?, now: Long): WorkoutRoutine =
        r.copy(days = r.days.map {
            if (it.id == dayId) it.copy(label = label.trim(), isRestDay = isRest, templateId = if (isRest) null else templateId, weekday = weekday) else it
        }, updatedAtMs = now)

    /** Copies a routine with new routine/day ids. Template references are preserved (a template may appear on several days). */
    fun duplicateRoutine(r: WorkoutRoutine, newId: String, newDayIds: List<String>, newName: String, now: Long): WorkoutRoutine {
        require(newDayIds.size == r.days.size) { "need one day id per day" }
        val days = r.days.mapIndexed { i, d -> d.copy(id = newDayIds[i]) }
        return r.copy(id = newId, name = newName.trim(), days = days, status = RoutineStatus.DRAFT, createdAtMs = now, updatedAtMs = now)
    }

    /**
     * Status transitions:
     *   DRAFT -> ACTIVE (validated), ARCHIVED
     *   ACTIVE -> PAUSED, ARCHIVED
     *   PAUSED -> ACTIVE (validated), ARCHIVED
     *   ARCHIVED -> DRAFT (restore)
     */
    fun changeStatus(r: WorkoutRoutine, to: RoutineStatus, templates: Map<String, WorkoutTemplate>, now: Long): Result<WorkoutRoutine> {
        val allowed = when (r.status) {
            RoutineStatus.DRAFT -> setOf(RoutineStatus.ACTIVE, RoutineStatus.ARCHIVED)
            RoutineStatus.ACTIVE -> setOf(RoutineStatus.PAUSED, RoutineStatus.ARCHIVED)
            RoutineStatus.PAUSED -> setOf(RoutineStatus.ACTIVE, RoutineStatus.ARCHIVED)
            RoutineStatus.ARCHIVED -> setOf(RoutineStatus.DRAFT)
        }
        if (to !in allowed) return Result.failure(IllegalStateException(Problem.BadStatus(r.status, to).message))
        if (to == RoutineStatus.ACTIVE) {
            val problems = validateForActivation(r, templates)
            if (problems.isNotEmpty()) return Result.failure(IllegalStateException(problems.first().message))
        }
        return Result.success(r.copy(status = to, updatedAtMs = now))
    }

    /** Activating one routine pauses any other ACTIVE routine so only one routine drives "today". */
    fun activateExclusively(target: WorkoutRoutine, others: List<WorkoutRoutine>, now: Long): List<WorkoutRoutine> =
        others.filter { it.status == RoutineStatus.ACTIVE && it.id != target.id }.map { it.copy(status = RoutineStatus.PAUSED, updatedAtMs = now) }

    fun routinesReferencing(templateId: String, routines: List<WorkoutRoutine>): List<WorkoutRoutine> =
        routines.filter { r -> r.days.any { it.templateId == templateId } }

    enum class TemplateRemoval { ARCHIVE, REMOVE_REFERENCES, CANCEL }

    /** Removes template references from routine days. Days become unassigned, so the routine can't activate until fixed. */
    fun unassignTemplate(templateId: String, routines: List<WorkoutRoutine>, now: Long): List<WorkoutRoutine> =
        routines.map { r ->
            if (r.days.none { it.templateId == templateId }) r
            else r.copy(days = r.days.map { if (it.templateId == templateId) it.copy(templateId = null) else it }, updatedAtMs = now)
        }

    /** Rest days are not training days. Only weekdays are validated for conflicts. */
    fun trainingDayCount(r: WorkoutRoutine): Int = r.days.count { !it.isRestDay }

    fun isFixedSchedule(r: WorkoutRoutine): Boolean = r.days.any { !it.isRestDay && it.weekday != null }

    /** Convenience for the UI: the weekday name for a weekday number. */
    fun weekdayName(weekday: Int): String = java.time.DayOfWeek.of(weekday).name.lowercase().replaceFirstChar { it.uppercase() }

}
