package com.pranav.focus.data

enum class SetType(val code: String, val label: String) {
    NORMAL("normal", "Working"), WARMUP("warmup", "Warm-up"), DROP("drop", "Drop"), FAILURE("failure", "Failure");
    companion object { fun of(c: String) = values().firstOrNull { it.code == c } ?: NORMAL }
}

/** credits: EffectiveLoad muscle key (see MuscleGroup.fromKey) -> weight. pattern null = isolation. */
data class Exercise(
    val id: String, val name: String, val equipment: String, val pattern: String?,
    val credits: Map<String, Double>, val bwFactor: Double = 0.0, val custom: Boolean = false,
)

/** Reusable session definition. Targets only; never rewritten by logging. */
data class TemplateExercise(
    val id: Long, val templateId: Long, val ord: Int, val exerciseId: String, val name: String,
    val targetSets: Int = 3, val repMin: Int = 8, val repMax: Int = 12, val targetKg: Double? = null,
    val restSeconds: Int? = null, val targetRpe: Double? = null, val notes: String = "",
)
data class WorkoutTemplate(
    val id: Long, val name: String, val notes: String, val createdAt: Long, val updatedAt: Long,
    val archivedAt: Long?, val exercises: List<TemplateExercise>,
)

enum class RoutineStatus(val code: String) {
    DRAFT("draft"), ACTIVE("active"), PAUSED("paused"), ARCHIVED("archived");
    companion object { fun of(c: String) = values().firstOrNull { it.code == c } ?: DRAFT }
}
/** weekday 1=Mon..7=Sun or null for flexible days. */
data class RoutineDay(val id: Long, val routineId: Long, val ord: Int, val label: String, val weekday: Int?, val rest: Boolean, val templateId: Long?)
data class WorkoutRoutine(
    val id: Long, val name: String, val description: String, val weeks: Int?, val startDate: String?,
    val status: RoutineStatus, val createdAt: Long, val updatedAt: Long, val days: List<RoutineDay>,
)

/** Actual execution. Targets are copied at start; later template edits never change history. */
data class SetEntry(
    val id: Long, val workoutExId: Long, val ord: Int, val weightKg: Double, val reps: Int,
    val rpe: Double?, val type: SetType, val done: Boolean, val ts: Long,
)
data class WorkoutExercise(val id: Long, val workoutId: Long, val ord: Int, val exerciseId: String, val name: String, val sets: List<SetEntry>)
data class Workout(
    val id: Long, val name: String, val startTs: Long, val endTs: Long, val active: Boolean,
    val verified: Boolean, val quick: Boolean, val rpe: Double?, val minutes: Int, val lu: Double,
    val templateId: Long?, val routineDayId: Long?, val exercises: List<WorkoutExercise>,
)

data class PrHit(val exerciseName: String, val kind: String, val value: Double)
