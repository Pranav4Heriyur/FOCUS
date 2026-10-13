package com.pranav.focus.workout.musclemap

/**
 * Muscle map domain. Pure Kotlin, no Android dependencies, so it is unit-testable on the JVM.
 *
 * Labels follow the user-supplied anatomy chart at group level. Where the chart does not label a
 * boundary separately, the group is broadened instead of guessed.
 *
 * Unknown vs zero: a null count means UNKNOWN (no workout source). A zero means the source
 * reported no completed sets for that muscle in the window.
 */
enum class MuscleView(val label: String) { FRONT("Front"), BACK("Back") }

enum class MuscleGroup(val label: String, val view: MuscleView, val description: String) {
    PECTORALS("Pectorals", MuscleView.FRONT, "Pectoralis major: horizontal pressing and arm adduction."),
    ANTERIOR_DELTOID("Anterior deltoid", MuscleView.FRONT, "Front of the shoulder: shoulder flexion and pressing."),
    BICEPS("Biceps brachii", MuscleView.FRONT, "Elbow flexion and forearm supination."),
    FOREARMS("Forearms", MuscleView.FRONT, "Forearm flexors and extensors: grip and wrist movement."),
    ABDOMINALS("Abdominals", MuscleView.FRONT, "Rectus abdominis: trunk flexion."),
    OBLIQUES("Obliques", MuscleView.FRONT, "Trunk rotation and lateral flexion."),
    QUADRICEPS("Quadriceps group", MuscleView.FRONT, "Knee extension."),
    ADDUCTORS("Adductor group", MuscleView.FRONT, "Brings the thigh toward the body's midline."),
    TIBIALIS_ANTERIOR("Tibialis anterior", MuscleView.FRONT, "Ankle dorsiflexion."),
    TRAPEZIUS("Trapezius", MuscleView.BACK, "Scapular elevation, retraction and rotation."),
    POSTERIOR_DELTOID("Posterior deltoid", MuscleView.BACK, "Back of the shoulder: horizontal abduction and extension."),
    LATISSIMUS_DORSI("Latissimus dorsi", MuscleView.BACK, "Shoulder extension and adduction."),
    TRICEPS("Triceps brachii", MuscleView.BACK, "Elbow extension."),
    LOWER_BACK("Lower back", MuscleView.BACK, "Spinal erectors: trunk extension."),
    GLUTEAL_GROUP("Gluteal group", MuscleView.BACK, "Hip extension and stabilisation."),
    HAMSTRINGS("Hamstrings", MuscleView.BACK, "Hip extension and knee flexion."),
    CALVES("Calves", MuscleView.BACK, "Ankle plantarflexion.");
}

enum class SetBand(val label: String, val range: String) {
    LOW("Low", "0–9"),
    IN_RANGE("In range", "10–20"),
    HIGH("High", "21+")
}

/** Bands: 0–9 LOW, 10–20 IN_RANGE, 21+ HIGH. null (unknown) returns null, never LOW. Not medical guidance. */
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
 * Source of COMPLETED set counts for the current week. Only completed sets may be counted.
 * Return null when no workout data source is available, which means every muscle is UNKNOWN.
 * Return a map when a source exists; muscles missing from the map are known zeroes.
 */
interface CompletedSetSource {
    suspend fun completedSetsThisWeek(): Map<MuscleGroup, Int>?
}

/** Default until a real workout source exists. It never fabricates counts. */
object UnavailableCompletedSetSource : CompletedSetSource {
    override suspend fun completedSetsThisWeek(): Map<MuscleGroup, Int>? = null
}

/** Expands a source result into one entry per group. null = unknown, 0 = known zero. */
fun countsFor(source: Map<MuscleGroup, Int>?): Map<MuscleGroup, Int?> {
    return MuscleGroup.values().associateWith { group ->
        if (source == null) null
        else (source[group] ?: 0).also { require(it >= 0) { "negative count for $group" } }
    }
}
