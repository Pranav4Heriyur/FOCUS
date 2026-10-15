package com.pranav.focus.data

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Faithful port of docs/effective_load.py (pure Kotlin, no Android imports). Constants marked HEURISTIC are tunable. */
object EffectiveLoad {
    const val EFFECTIVE_WINDOW = 5
    const val DEFAULT_RIR = 2
    const val MIN_INTENSITY = 0.30
    const val MAX_RTF_FOR_1RM = 15
    const val ASSUMED_I_HIGH_REP = 0.45
    const val TRIMP_TO_LU = 3.5

    data class Hse(val hse: Double, val intensity: Double, val confidence: String)

    fun rirFrom(rpe: Double? = null, setType: String = "normal"): Int {
        if (setType == "failure") return 0
        if (rpe != null) return max(0, pyRound(10 - rpe))
        return DEFAULT_RIR
    }

    /** Python round() = banker's rounding; keeps parity with the reference. */
    private fun pyRound(x: Double): Int = Math.rint(x).toInt()

    fun effectiveLoadKg(externalKg: Double, bodyweightKg: Double = 0.0, bwFactor: Double = 0.0, assistKg: Double = 0.0): Double =
        max(0.0, externalKg + bodyweightKg * bwFactor - assistKg)

    fun e1rm(load: Double, reps: Int, rir: Int): Double? {
        val rtf = reps + rir
        if (rtf > MAX_RTF_FOR_1RM || load <= 0) return null
        if (rtf == 1) return load
        val epley = load * (1 + rtf / 30.0)
        val brzycki = load * 36 / (37 - rtf)
        return (epley + brzycki) / 2
    }

    fun hardSetEquivalent(reps: Int, rir: Int, load: Double, e1rmRef: Double? = null, ownE1rm: Double? = null): Hse {
        val ref = e1rmRef ?: ownE1rm
        val intensity: Double
        val confidence: String
        if (ref != null && ref > 0) {
            intensity = min(1.0, load / ref)
            confidence = if (e1rmRef != null) "high" else "medium"
        } else {
            intensity = ASSUMED_I_HIGH_REP; confidence = "low"
        }
        val er = max(0, min(reps, EFFECTIVE_WINDOW - rir))
        val g = min(1.0, intensity / MIN_INTENSITY)
        return Hse(er.toDouble() / EFFECTIVE_WINDOW * g, intensity, confidence)
    }

    val PATTERNS: Map<String, Map<String, Double>> = mapOf(
        "horizontal_push" to mapOf("chest" to 1.0, "triceps" to 0.5, "front_delts" to 0.5),
        "vertical_push" to mapOf("front_delts" to 1.0, "triceps" to 0.5, "chest" to 0.5),
        "vertical_pull" to mapOf("lats" to 1.0, "biceps" to 0.5, "upper_back" to 0.5),
        "horizontal_pull" to mapOf("upper_back" to 1.0, "lats" to 0.5, "biceps" to 0.5, "rear_delts" to 0.5),
        "squat" to mapOf("quads" to 1.0, "glutes" to 0.5),
        "hinge" to mapOf("glutes" to 1.0, "hamstrings" to 1.0, "lower_back" to 0.5),
        "lunge" to mapOf("quads" to 1.0, "glutes" to 0.5),
        "core" to mapOf("core" to 1.0),
        "carry" to mapOf("core" to 1.0, "forearms" to 0.5, "traps" to 0.5),
    )

    fun muscleCredit(primary: List<String>, pattern: String? = null): Map<String, Double> =
        PATTERNS[pattern]?.toMap() ?: primary.associateWith { 1.0 }

    fun srpeLoad(rpe: Double, minutes: Double) = rpe * minutes

    fun banisterTrimp(minutes: Double, hrAvg: Double, hrRest: Double, hrMax: Double, male: Boolean = true): Double {
        val hrr = ((hrAvg - hrRest) / (hrMax - hrRest)).coerceIn(0.0, 1.0)
        val a = if (male) 0.64 else 0.86
        val b = if (male) 1.92 else 1.67
        return minutes * hrr * a * exp(b * hrr)
    }

    fun metFromKcal(activeKcal: Double, weightKg: Double, minutes: Double) = activeKcal / (weightKg * minutes / 60) + 1
    fun rpeFromMet(met: Double) = (met / 1.4).coerceIn(1.0, 10.0)

    fun sessionLoad(minutes: Double, rpe: Double? = null, trimp: Double? = null, met: Double? = null, k: Double = TRIMP_TO_LU): Double {
        val parts = ArrayList<Double>()
        if (rpe != null) parts.add(srpeLoad(rpe, minutes))
        if (trimp != null) parts.add(trimp * k)
        if (parts.isNotEmpty()) return parts.sum() / parts.size
        if (met != null) return srpeLoad(rpeFromMet(met), minutes)
        throw IllegalArgumentException("need duration + one of rpe / hr / calories")
    }

    data class Ffr(val fitness: Double, val fatigue: Double, val form: Double)

    fun fitnessFatigue(dailyLoads: List<Double>, tauFit: Int = 42, tauFat: Int = 7): List<Ffr> {
        var ctl: Double? = null; var atl: Double? = null
        val out = ArrayList<Ffr>()
        for (load in dailyLoads) {
            if (ctl == null || atl == null) { ctl = load; atl = load }
            ctl += (load - ctl) * (1 - exp(-1.0 / tauFit))
            atl += (load - atl) * (1 - exp(-1.0 / tauFat))
            out.add(Ffr(round1(ctl), round1(atl), round1(ctl - atl)))
        }
        return out
    }

    private fun round1(x: Double) = (x * 10).roundToInt() / 10.0
}
