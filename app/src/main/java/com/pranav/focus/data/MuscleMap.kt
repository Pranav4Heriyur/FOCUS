package com.pranav.focus.data

/** Stable muscle-group keys (analytics level). Detailed visual sub-regions map onto these via MuscleRegion.group. */
enum class BodyFace { FRONT, BACK }

enum class MuscleGroup(val label: String) {
    NECK("Neck"), CHEST("Chest"), FRONT_DELTOIDS("Front delts"), SIDE_DELTOIDS("Side delts"), REAR_DELTOIDS("Rear delts"),
    BICEPS("Biceps"), TRICEPS("Triceps"), FOREARMS("Forearms"), TRAPEZIUS("Trapezius"), LATS("Lats"),
    LOWER_BACK("Lower back"), SERRATUS("Serratus"), ABS("Abs"), OBLIQUES("Obliques"), HIP_FLEXORS("Hip flexors"),
    QUADRICEPS("Quadriceps"), ADDUCTORS("Adductors"), HAMSTRINGS("Hamstrings"), GLUTES("Glutes"),
    CALVES("Calves"), TIBIALIS("Shins");

    companion object {
        /** Adapter from EffectiveLoad's muscle keys (effective_load.py) to map groups. */
        fun fromKey(k: String): MuscleGroup? = when (k) {
            "chest" -> CHEST; "triceps" -> TRICEPS; "biceps" -> BICEPS; "front_delts" -> FRONT_DELTOIDS
            "side_delts" -> SIDE_DELTOIDS; "rear_delts" -> REAR_DELTOIDS; "lats" -> LATS
            "upper_back", "traps" -> TRAPEZIUS; "lower_back" -> LOWER_BACK; "core" -> ABS; "obliques" -> OBLIQUES
            "quads" -> QUADRICEPS; "hamstrings" -> HAMSTRINGS; "glutes" -> GLUTES; "calves" -> CALVES
            "forearms" -> FOREARMS; "adductors" -> ADDUCTORS; "hip_flexors" -> HIP_FLEXORS; "neck" -> NECK
            "tibialis" -> TIBIALIS; "serratus" -> SERRATUS
            else -> null
        }
    }
}

enum class MuscleStatus { UNKNOWN, UNDER_TARGET, ON_TARGET, OVER_TARGET }

/** null hardSets = unknown (no data), NOT zero. <10 under, 10..20 on target, >20 over. */
data class WeeklyMuscleLoad(val hardSets: Int?, val targetMin: Int = 10, val targetMax: Int = 20) {
    val status: MuscleStatus
        get() = when {
            hardSets == null -> MuscleStatus.UNKNOWN
            hardSets < targetMin -> MuscleStatus.UNDER_TARGET
            hardSets <= targetMax -> MuscleStatus.ON_TARGET
            else -> MuscleStatus.OVER_TARGET
        }
    val label: String
        get() = when (status) {
            MuscleStatus.UNKNOWN -> "No data"; MuscleStatus.UNDER_TARGET -> "Under target"
            MuscleStatus.ON_TARGET -> "On target"; MuscleStatus.OVER_TARGET -> "Over target"
        }
}

/** A selectable surface region. Polygons are in a 100 x 205 box, RIGHT half only (x >= 50); x is mirrored for the left half. */
class MuscleRegion(val key: String, val label: String, val face: BodyFace, val group: MuscleGroup, val poly: List<Pair<Float, Float>>, val mirror: Boolean = true) {
    fun polygons(): List<List<Pair<Float, Float>>> =
        if (mirror) listOf(poly, poly.map { (100f - it.first) to it.second }) else listOf(poly)

    fun contains(x: Float, y: Float): Boolean = polygons().any { pointInPoly(it, x, y) }
}

/** Muscles that exist only beneath the surface; shown as selectable chips, never drawn on a wrong superficial muscle. */
class DeepMuscle(val key: String, val label: String, val face: BodyFace, val group: MuscleGroup)

fun pointInPoly(p: List<Pair<Float, Float>>, x: Float, y: Float): Boolean {
    var inside = false
    var j = p.size - 1
    for (i in p.indices) {
        val (xi, yi) = p[i]; val (xj, yj) = p[j]
        if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
        j = i
    }
    return inside
}

private typealias Pt = Pair<Float, Float>

private fun lerp(a: Pt, b: Pt, t: Float): Pt = (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)

/** Quad on a limb segment between inner edge (a1->a2) and outer edge (b1->b2); u across (0 inner..1 outer), v along (0..1). */
private fun band(a1: Pt, a2: Pt, b1: Pt, b2: Pt, u0: Float, u1: Float, v0: Float, v1: Float): List<Pt> {
    fun at(u: Float, v: Float) = lerp(lerp(a1, a2, v), lerp(b1, b2, v), u)
    return listOf(at(u0, v0), at(u1, v0), at(u1, v1), at(u0, v1))
}

object MuscleAtlas {

    // front limb edges
    private val fForeIn1 = 75f to 74f; private val fForeIn2 = 83.5f to 106f
    private val fForeOut1 = 83f to 74f; private val fForeOut2 = 88.5f to 106f
    private val fThighIn1 = 53f to 104f; private val fThighIn2 = 55f to 146f
    private val fThighOut1 = 67.5f to 100f; private val fThighOut2 = 66.5f to 146f
    private val fShinIn1 = 55.5f to 152f; private val fShinIn2 = 58f to 192f
    private val fShinOut1 = 66.5f to 152f; private val fShinOut2 = 63f to 192f

    private fun fore(u0: Float, u1: Float, v0: Float, v1: Float) = band(fForeIn1, fForeIn2, fForeOut1, fForeOut2, u0, u1, v0, v1)
    private fun thigh(u0: Float, u1: Float, v0: Float, v1: Float) = band(fThighIn1, fThighIn2, fThighOut1, fThighOut2, u0, u1, v0, v1)
    private fun shin(u0: Float, u1: Float, v0: Float, v1: Float) = band(fShinIn1, fShinIn2, fShinOut1, fShinOut2, u0, u1, v0, v1)

    // back limb edges
    private fun tri(u0: Float, u1: Float, v0: Float, v1: Float) = band(70.5f to 52f, 74.5f to 74f, 78.5f to 50f, 80f to 74f, u0, u1, v0, v1)
    private fun bfore(u0: Float, u1: Float, v0: Float, v1: Float) = band(fForeIn1, fForeIn2, fForeOut1, fForeOut2, u0, u1, v0, v1)
    private fun ham(u0: Float, u1: Float, v0: Float, v1: Float) = band(52f to 122f, 55f to 146f, 68f to 118f, 66.5f to 146f, u0, u1, v0, v1)
    private fun calf(u0: Float, u1: Float, v0: Float, v1: Float) = band(55.5f to 152f, 58f to 190f, 66.5f to 152f, 63.5f to 190f, u0, u1, v0, v1)

    private fun r(key: String, label: String, face: BodyFace, g: MuscleGroup, poly: List<Pt>) = MuscleRegion(key, label, face, g, poly)

    private val F = BodyFace.FRONT
    private val B = BodyFace.BACK

    /** Drawing order = list order; hit-testing runs in reverse so later (top) regions win. */
    val regions: List<MuscleRegion> = listOf(
        // ---------------- FRONT ----------------
        r("neck_front", "Neck / upper trapezius", F, MuscleGroup.NECK, listOf(50f to 22f, 55f to 22f, 58f to 29f, 66f to 33f, 62f to 35.5f, 54f to 32f, 50f to 31f)),
        r("delt_anterior", "Anterior deltoid", F, MuscleGroup.FRONT_DELTOIDS, listOf(65f to 35f, 70f to 33.5f, 73f to 36f, 74f to 46f, 72f to 52f, 68f to 48f, 66f to 42f)),
        r("delt_lateral", "Lateral deltoid", F, MuscleGroup.SIDE_DELTOIDS, listOf(73f to 36f, 77f to 36.5f, 79.5f to 43f, 78f to 52f, 76f to 56f, 72f to 52f, 74f to 46f)),
        r("pec_upper", "Pectoralis major (upper)", F, MuscleGroup.CHEST, listOf(50f to 35f, 61f to 35f, 66f to 38f, 66.5f to 46f, 50f to 46f)),
        r("pec_lower", "Pectoralis major (lower)", F, MuscleGroup.CHEST, listOf(50f to 46f, 66.5f to 46f, 65f to 52f, 60f to 57f, 50f to 58f)),
        r("serratus", "Serratus anterior", F, MuscleGroup.SERRATUS, listOf(65f to 52f, 67.5f to 53f, 67f to 64f, 64f to 68f, 61.5f to 62f, 62f to 57f)),
        r("abs", "Rectus abdominis", F, MuscleGroup.ABS, listOf(50f to 58f, 56f to 58.5f, 57f to 74f, 56f to 90f, 53f to 96f, 50f to 98f)),
        r("obliques", "Obliques", F, MuscleGroup.OBLIQUES, listOf(57f to 62f, 61.5f to 62f, 64f to 68f, 65f to 82f, 65.5f to 92f, 59f to 94f, 56.5f to 90f, 57.5f to 74f)),
        r("hip_flexors", "Hip flexors (iliopsoas)", F, MuscleGroup.HIP_FLEXORS, listOf(50f to 98f, 53f to 96f, 59f to 94f, 62f to 100f, 60f to 108f, 55f to 112f, 52f to 106f)),
        r("tfl", "Tensor fasciae latae", F, MuscleGroup.HIP_FLEXORS, listOf(62.5f to 98f, 67.5f to 96f, 68.5f to 106f, 66f to 113f, 62f to 109f)),
        r("biceps", "Biceps brachii", F, MuscleGroup.BICEPS, listOf(71.5f to 52f, 75f to 52f, 78.5f to 60f, 78.5f to 70f, 75f to 73f, 72.5f to 68f)),
        r("brachialis", "Brachialis", F, MuscleGroup.BICEPS, listOf(78.5f to 62f, 80f to 66f, 80.5f to 72f, 78.5f to 72f)),
        r("brachioradialis", "Brachioradialis", F, MuscleGroup.FOREARMS, fore(0.55f, 1f, 0f, 0.35f)),
        r("wrist_flexors_f", "Wrist flexors", F, MuscleGroup.FOREARMS, fore(0f, 0.55f, 0f, 0.8f)),
        r("wrist_extensors_f", "Wrist extensors", F, MuscleGroup.FOREARMS, fore(0.55f, 1f, 0.35f, 0.8f)),
        r("adductors", "Adductors (longus / gracilis / pectineus)", F, MuscleGroup.ADDUCTORS, thigh(0f, 0.28f, 0f, 0.62f)),
        r("vastus_medialis", "Vastus medialis", F, MuscleGroup.QUADRICEPS, thigh(0f, 0.3f, 0.62f, 1f)),
        r("rectus_femoris", "Rectus femoris", F, MuscleGroup.QUADRICEPS, thigh(0.3f, 0.62f, 0f, 0.88f)),
        r("vastus_lateralis", "Vastus lateralis", F, MuscleGroup.QUADRICEPS, thigh(0.62f, 1f, 0.1f, 1f)),
        r("sartorius", "Sartorius", F, MuscleGroup.ADDUCTORS, listOf(66f to 100f, 68f to 101f, 56f to 142f, 54f to 140f)),
        r("tibialis", "Tibialis anterior", F, MuscleGroup.TIBIALIS, shin(0.1f, 0.55f, 0f, 0.85f)),
        r("edl", "Extensor digitorum longus", F, MuscleGroup.TIBIALIS, shin(0.55f, 0.9f, 0.1f, 0.85f)),
        // ---------------- BACK ----------------
        r("trap_upper", "Upper trapezius", B, MuscleGroup.TRAPEZIUS, listOf(50f to 22f, 55f to 23f, 58f to 29f, 67f to 33f, 63f to 39f, 56f to 38f, 50f to 34f)),
        r("trap_mid", "Middle trapezius", B, MuscleGroup.TRAPEZIUS, listOf(50f to 34f, 56f to 38f, 60f to 41f, 58f to 50f, 50f to 56f)),
        r("trap_lower", "Lower trapezius", B, MuscleGroup.TRAPEZIUS, listOf(50f to 56f, 58f to 50f, 57f to 60f, 53f to 72f, 50f to 76f)),
        r("delt_posterior", "Posterior deltoid", B, MuscleGroup.REAR_DELTOIDS, listOf(65f to 35f, 70f to 33.5f, 73f to 36f, 74f to 46f, 72f to 52f, 68f to 48f, 66f to 42f)),
        r("delt_lateral_b", "Lateral deltoid", B, MuscleGroup.SIDE_DELTOIDS, listOf(73f to 36f, 77f to 36.5f, 79.5f to 43f, 78f to 52f, 76f to 56f, 72f to 52f, 74f to 46f)),
        r("infraspinatus", "Infraspinatus", B, MuscleGroup.REAR_DELTOIDS, listOf(60f to 41f, 66f to 42f, 67f to 50f, 62f to 55f, 58f to 50f)),
        r("teres_minor", "Teres minor", B, MuscleGroup.REAR_DELTOIDS, listOf(66f to 44f, 70f to 46f, 70f to 51f, 67f to 51f)),
        r("teres_major", "Teres major", B, MuscleGroup.LATS, listOf(62f to 55f, 69f to 52f, 70f to 58f, 64f to 60f)),
        r("lats", "Latissimus dorsi", B, MuscleGroup.LATS, listOf(58f to 58f, 64f to 60f, 66f to 68f, 65f to 80f, 58f to 88f, 54f to 80f, 55f to 68f)),
        r("lower_back", "Erector spinae / lower back", B, MuscleGroup.LOWER_BACK, listOf(50f to 74f, 54f to 78f, 58f to 90f, 57f to 98f, 50f to 100f)),
        r("triceps_long", "Triceps (long head)", B, MuscleGroup.TRICEPS, tri(0f, 0.45f, 0f, 0.9f)),
        r("triceps_lateral", "Triceps (lateral head)", B, MuscleGroup.TRICEPS, tri(0.45f, 1f, 0.1f, 0.85f)),
        r("wrist_extensors_b", "Wrist extensors", B, MuscleGroup.FOREARMS, bfore(0.45f, 1f, 0f, 0.8f)),
        r("wrist_flexors_b", "Wrist flexors", B, MuscleGroup.FOREARMS, bfore(0f, 0.45f, 0f, 0.8f)),
        r("glute_max", "Gluteus maximus", B, MuscleGroup.GLUTES, listOf(50f to 100f, 58f to 98f, 66f to 100f, 68f to 108f, 66f to 118f, 58f to 122f, 50f to 118f)),
        r("glute_med", "Gluteus medius", B, MuscleGroup.GLUTES, listOf(58f to 92f, 65f to 92f, 68f to 100f, 66f to 103f, 60f to 100f)),
        r("adductor_magnus", "Adductor magnus / gracilis", B, MuscleGroup.ADDUCTORS, ham(0f, 0.2f, 0f, 0.9f)),
        r("semimembranosus", "Semimembranosus", B, MuscleGroup.HAMSTRINGS, ham(0.2f, 0.45f, 0.08f, 1f)),
        r("semitendinosus", "Semitendinosus", B, MuscleGroup.HAMSTRINGS, ham(0.45f, 0.62f, 0.08f, 0.85f)),
        r("biceps_femoris", "Biceps femoris", B, MuscleGroup.HAMSTRINGS, ham(0.62f, 1f, 0.08f, 1f)),
        r("gastro_medial", "Gastrocnemius (medial head)", B, MuscleGroup.CALVES, calf(0f, 0.5f, 0f, 0.55f)),
        r("gastro_lateral", "Gastrocnemius (lateral head)", B, MuscleGroup.CALVES, calf(0.5f, 1f, 0f, 0.55f)),
        r("soleus", "Soleus", B, MuscleGroup.CALVES, calf(0.1f, 0.9f, 0.55f, 0.95f)),
    )

    val deep: List<DeepMuscle> = listOf(
        DeepMuscle("pec_minor", "Pectoralis minor (deep)", F, MuscleGroup.CHEST),
        DeepMuscle("vastus_intermedius", "Vastus intermedius (deep)", F, MuscleGroup.QUADRICEPS),
        DeepMuscle("adductor_brevis", "Adductor brevis (deep)", F, MuscleGroup.ADDUCTORS),
        DeepMuscle("supraspinatus", "Supraspinatus (deep)", B, MuscleGroup.REAR_DELTOIDS),
        DeepMuscle("rhomboids", "Rhomboids (deep)", B, MuscleGroup.TRAPEZIUS),
        DeepMuscle("levator", "Levator scapulae (deep)", B, MuscleGroup.TRAPEZIUS),
        DeepMuscle("glute_min", "Gluteus minimus (deep)", B, MuscleGroup.GLUTES),
        DeepMuscle("plantaris", "Plantaris (deep)", B, MuscleGroup.CALVES),
        DeepMuscle("popliteus", "Popliteus (deep)", B, MuscleGroup.CALVES),
        DeepMuscle("triceps_medial", "Triceps (medial head, deep)", B, MuscleGroup.TRICEPS),
    )

    fun regionsOn(face: BodyFace) = regions.filter { it.face == face }

    /** Hit-test in map coordinates (100 x 205). Topmost (last drawn) wins. */
    fun hit(face: BodyFace, x: Float, y: Float): MuscleRegion? = regionsOn(face).lastOrNull { it.contains(x, y) }
}
