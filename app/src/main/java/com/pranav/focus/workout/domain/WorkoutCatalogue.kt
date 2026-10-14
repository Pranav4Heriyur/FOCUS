package com.pranav.focus.workout.domain

/**
 * Built-in exercise definitions. These are generic reference entries (exercise names and primary muscles),
 * not personal workouts and not statistics. Ids are stable and prefixed "builtin:".
 * Muscle mapping is conservative: an exercise is mapped only to the muscles it clearly trains as the primary mover.
 */
object WorkoutCatalogue {

    val BUILTIN: List<Exercise> = listOf(
        Exercise("builtin:bench_press", "Bench press", setOf(MuscleGroup.PECTORALS), false),
        Exercise("builtin:incline_dumbbell_press", "Incline dumbbell press", setOf(MuscleGroup.PECTORALS), false),
        Exercise("builtin:overhead_press", "Overhead press", setOf(MuscleGroup.ANTERIOR_DELTOID), false),
        Exercise("builtin:face_pull", "Face pull", setOf(MuscleGroup.POSTERIOR_DELTOID, MuscleGroup.TRAPEZIUS), false),
        Exercise("builtin:pull_up", "Pull-up", setOf(MuscleGroup.LATISSIMUS_DORSI), false),
        Exercise("builtin:lat_pulldown", "Lat pulldown", setOf(MuscleGroup.LATISSIMUS_DORSI), false),
        Exercise("builtin:barbell_row", "Barbell row", setOf(MuscleGroup.LATISSIMUS_DORSI), false),
        Exercise("builtin:seated_cable_row", "Seated cable row", setOf(MuscleGroup.LATISSIMUS_DORSI), false),
        Exercise("builtin:barbell_shrug", "Barbell shrug", setOf(MuscleGroup.TRAPEZIUS), false),
        Exercise("builtin:barbell_curl", "Barbell curl", setOf(MuscleGroup.BICEPS), false),
        Exercise("builtin:triceps_pushdown", "Triceps pushdown", setOf(MuscleGroup.TRICEPS), false),
        Exercise("builtin:skull_crusher", "Skull crusher", setOf(MuscleGroup.TRICEPS), false),
        Exercise("builtin:wrist_curl", "Wrist curl", setOf(MuscleGroup.FOREARMS), false),
        Exercise("builtin:back_squat", "Back squat", setOf(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTEAL_GROUP), false),
        Exercise("builtin:leg_press", "Leg press", setOf(MuscleGroup.QUADRICEPS), false),
        Exercise("builtin:romanian_deadlift", "Romanian deadlift", setOf(MuscleGroup.HAMSTRINGS), false),
        Exercise("builtin:leg_curl", "Leg curl", setOf(MuscleGroup.HAMSTRINGS), false),
        Exercise("builtin:hip_thrust", "Hip thrust", setOf(MuscleGroup.GLUTEAL_GROUP), false),
        Exercise("builtin:standing_calf_raise", "Standing calf raise", setOf(MuscleGroup.CALVES), false),
        Exercise("builtin:cable_crunch", "Cable crunch", setOf(MuscleGroup.ABDOMINALS), false),
    )

    val BY_ID: Map<String, Exercise> = BUILTIN.associateBy { it.id }
}
