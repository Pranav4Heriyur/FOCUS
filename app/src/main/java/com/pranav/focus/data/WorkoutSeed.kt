package com.pranav.focus.data

/** Built-in exercise library (stable ids). Users add custom exercises; those are flagged custom=true. */
object WorkoutSeed {
    private fun p(id: String, n: String, eq: String, pat: String, bw: Double = 0.0) =
        Exercise(id, n, eq, pat, EffectiveLoad.muscleCredit(emptyList(), pat), bw)
    private fun i(id: String, n: String, eq: String, m: String, bw: Double = 0.0) =
        Exercise(id, n, eq, null, EffectiveLoad.muscleCredit(listOf(m)), bw)
    private fun c(id: String, n: String, eq: String, credits: Map<String, Double>) = Exercise(id, n, eq, null, credits)

    val ALL: List<Exercise> = listOf(
        p("bench_press", "Barbell Bench Press", "Barbell", "horizontal_push"),
        p("incline_bench", "Incline Barbell Bench Press", "Barbell", "horizontal_push"),
        p("db_bench", "Dumbbell Bench Press", "Dumbbell", "horizontal_push"),
        p("incline_db", "Incline Dumbbell Press", "Dumbbell", "horizontal_push"),
        p("pushup", "Push-up", "Bodyweight", "horizontal_push", 0.65),
        p("dip", "Dip", "Bodyweight", "horizontal_push", 1.0),
        i("cable_fly", "Cable Fly", "Cable", "chest"),
        i("pec_deck", "Pec Deck", "Machine", "chest"),
        p("ohp", "Overhead Press", "Barbell", "vertical_push"),
        p("db_shoulder_press", "Dumbbell Shoulder Press", "Dumbbell", "vertical_push"),
        i("lateral_raise", "Lateral Raise", "Dumbbell", "side_delts"),
        i("rear_delt_fly", "Rear Delt Fly", "Dumbbell", "rear_delts"),
        i("face_pull", "Face Pull", "Cable", "rear_delts"),
        i("triceps_pushdown", "Triceps Pushdown", "Cable", "triceps"),
        i("skullcrusher", "Skull Crusher", "Barbell", "triceps"),
        i("overhead_triceps", "Overhead Triceps Extension", "Dumbbell", "triceps"),
        p("pullup", "Pull-up", "Bodyweight", "vertical_pull", 1.0),
        p("chinup", "Chin-up", "Bodyweight", "vertical_pull", 1.0),
        p("lat_pulldown", "Lat Pulldown", "Cable", "vertical_pull"),
        p("barbell_row", "Barbell Row", "Barbell", "horizontal_pull"),
        p("db_row", "One-arm Dumbbell Row", "Dumbbell", "horizontal_pull"),
        p("cable_row", "Seated Cable Row", "Cable", "horizontal_pull"),
        i("biceps_curl", "Barbell Curl", "Barbell", "biceps"),
        i("db_curl", "Dumbbell Curl", "Dumbbell", "biceps"),
        c("hammer_curl", "Hammer Curl", "Dumbbell", mapOf("biceps" to 1.0, "forearms" to 0.5)),
        i("wrist_curl", "Wrist Curl", "Dumbbell", "forearms"),
        i("shrug", "Barbell Shrug", "Barbell", "traps"),
        p("squat", "Barbell Back Squat", "Barbell", "squat"),
        p("front_squat", "Front Squat", "Barbell", "squat"),
        p("leg_press", "Leg Press", "Machine", "squat"),
        p("lunge", "Walking Lunge", "Dumbbell", "lunge"),
        p("split_squat", "Bulgarian Split Squat", "Dumbbell", "lunge"),
        p("deadlift", "Conventional Deadlift", "Barbell", "hinge"),
        p("rdl", "Romanian Deadlift", "Barbell", "hinge"),
        p("hip_thrust", "Hip Thrust", "Barbell", "hinge"),
        i("leg_extension", "Leg Extension", "Machine", "quads"),
        i("leg_curl", "Lying Leg Curl", "Machine", "hamstrings"),
        i("calf_raise", "Standing Calf Raise", "Machine", "calves"),
        i("hip_adduction", "Hip Adduction", "Machine", "adductors"),
        i("crunch", "Cable Crunch", "Cable", "core"),
        i("plank", "Plank", "Bodyweight", "core"),
        i("hanging_leg_raise", "Hanging Leg Raise", "Bodyweight", "core"),
        i("back_extension", "Back Extension", "Bodyweight", "lower_back"),
        p("farmer_carry", "Farmer's Carry", "Dumbbell", "carry"),
    )
}
