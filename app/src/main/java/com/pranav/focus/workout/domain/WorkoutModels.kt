package com.pranav.focus.workout.domain

import java.time.LocalDate

/**
 * Workout domain models. Pure Kotlin: no Android imports, so the rules are unit-testable on the JVM.
 *
 * Canonical units: load in kilograms (Double). The UI converts for display only and never stores converted values.
 * Historical sessions store COPIES of template prescriptions (see SessionExercise), so editing a template
 * never rewrites past sessions.
 */

/** Muscle groups aligned to the user's anatomy chart. Some boundaries are broadened where the chart is unlabelled. */
enum class MuscleGroup(val label: String) {
    PECTORALS("Pectorals"),
    ANTERIOR_DELTOID("Anterior deltoid"),
    POSTERIOR_DELTOID("Posterior deltoid"),
    BICEPS("Biceps brachii"),
    TRICEPS("Triceps brachii"),
    FOREARMS("Forearms"),
    TRAPEZIUS("Trapezius"),
    LATISSIMUS_DORSI("Latissimus dorsi"),
    LOWER_BACK("Lower back"),
    ABDOMINALS("Abdominals"),
    OBLIQUES("Obliques"),
    SERRATUS_ANTERIOR("Serratus anterior"),
    QUADRICEPS("Quadriceps group"),
    ADDUCTORS("Adductor group"),
    HAMSTRINGS("Hamstrings"),
    GLUTEAL_GROUP("Gluteal group"),
    CALVES("Calves"),
    TIBIALIS_ANTERIOR("Tibialis anterior");
}

/**
 * An exercise in the catalogue. Built-in exercises have stable ids starting with "builtin:".
 * Custom exercises have ids starting with "custom:" and are always distinguishable.
 */
data class Exercise(
    val id: String,
    val name: String,
    val primaryMuscles: Set<MuscleGroup>,
    val isCustom: Boolean,
)

enum class SetType { NORMAL, WARMUP, FAILURE, DROP }

/** Prescription for one exercise inside a template. */
data class Prescription(
    val targetSets: Int = 3,
    val repMin: Int = 8,
    val repMax: Int = 12,
    val targetLoadKg: Double? = null,
    val restSeconds: Int? = 90,
    val targetRpe: Double? = null,
    val notes: String = "",
)

/** An exercise row inside a template: stable id, order, and prescription. */
data class TemplateExercise(
    val id: String,
    val exerciseId: String,
    val orderIndex: Int,
    val prescription: Prescription,
)

/** Reusable session definition. Archived templates stay readable but cannot be started. */
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val notes: String,
    val exercises: List<TemplateExercise>,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val archivedAtMs: Long? = null,
)

enum class RoutineStatus { DRAFT, ACTIVE, PAUSED, ARCHIVED }

/**
 * One day in a routine. A training day references a template. A rest day has no template.
 * weekday uses java.time.DayOfWeek.getValue() (1 = Monday .. 7 = Sunday). Null = flexible (no fixed weekday).
 */
data class RoutineDay(
    val id: String,
    val orderIndex: Int,
    val label: String,
    val isRestDay: Boolean,
    val templateId: String?,
    val weekday: Int?,
)

data class WorkoutRoutine(
    val id: String,
    val name: String,
    val description: String,
    val durationWeeks: Int?,
    val startDate: LocalDate?,
    val status: RoutineStatus,
    val days: List<RoutineDay>,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

enum class SessionStatus { IN_PROGRESS, COMPLETED, ABANDONED, SKIPPED }

/** Prescription snapshot + name snapshot, frozen when the session starts. */
data class SessionExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val orderIndex: Int,
    val prescription: Prescription,
)

/** One logged set. reps and loadKg are nullable: unknown is not zero. */
data class LoggedSet(
    val id: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val type: SetType,
    val reps: Int?,
    val loadKg: Double?,
    val rpe: Double?,
    val loggedAtMs: Long,
)

/** One real execution. Only COMPLETED sessions feed history and muscle statistics. */
data class WorkoutSession(
    val id: String,
    val date: LocalDate,
    val templateId: String?,
    val templateName: String,
    val routineId: String?,
    val routineDayId: String?,
    val status: SessionStatus,
    val startedAtMs: Long,
    val completedAtMs: Long?,
    val exercises: List<SessionExercise>,
    val sets: List<LoggedSet>,
)
