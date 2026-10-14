package com.pranav.focus.data

import kotlin.math.floor
import kotlin.math.roundToInt

/** Pure workout rules (no Android imports). */
object WorkoutLogic {
    const val LB_PER_KG = 2.20462262

    fun toDisplay(kg: Double, imperial: Boolean) = if (imperial) kg * LB_PER_KG else kg
    fun fromDisplay(v: Double, imperial: Boolean) = if (imperial) v / LB_PER_KG else v
    fun fmt(v: Double): String = if (v == floor(v)) v.toLong().toString() else "%.1f".format(v)

    /** Verified = >=5 completed working sets (not warm-ups) and >=20 min, finished via Finish. Quick logs/edits never verify. */
    fun isVerified(workingSets: Int, minutes: Int, viaFinish: Boolean = true) = viaFinish && workingSets >= 5 && minutes >= 20
    fun xpFor(prCount: Int) = 40 + 5 * prCount.coerceIn(0, 4)

    /** Plates per side, greedy. Returns (plates, remainderPerSideKg). */
    fun plates(targetKg: Double, barKg: Double, inventory: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)): Pair<List<Double>, Double> {
        var side = (targetKg - barKg) / 2
        if (side <= 0) return emptyList<Double>() to 0.0
        val out = ArrayList<Double>()
        for (p in inventory.sortedDescending()) while (side + 1e-9 >= p) { out.add(p); side -= p }
        return out to (side * 100).roundToInt() / 100.0
    }

    data class SetLike(val exerciseId: String, val weightKg: Double, val reps: Int, val rpe: Double?, val type: SetType, val ts: Long)

    fun bestE1rm(sets: List<SetLike>): Map<String, Double> {
        val m = HashMap<String, Double>()
        for (s in sets) {
            if (s.type == SetType.WARMUP) continue
            val e = EffectiveLoad.e1rm(s.weightKg, s.reps, EffectiveLoad.rirFrom(s.rpe, s.type.code)) ?: continue
            if (e > (m[s.exerciseId] ?: 0.0)) m[s.exerciseId] = e
        }
        return m
    }

    private fun maxWeight(sets: List<SetLike>): Map<String, Double> {
        val m = HashMap<String, Double>()
        for (s in sets) if (s.type != SetType.WARMUP && s.weightKg > (m[s.exerciseId] ?: 0.0)) m[s.exerciseId] = s.weightKg
        return m
    }

    /** PRs vs prior history. First time an exercise is logged is a baseline, not a PR. */
    fun detectPrs(names: Map<String, String>, current: List<SetLike>, history: List<SetLike>): List<PrHit> {
        val priorE = bestE1rm(history); val priorW = maxWeight(history)
        val out = ArrayList<PrHit>()
        for ((id, e) in bestE1rm(current)) { val b = priorE[id]; if (b != null && e > b + 0.01) out.add(PrHit(names[id] ?: id, "e1RM", e)) }
        for ((id, w) in maxWeight(current)) { val b = priorW[id]; if (b != null && w > b) out.add(PrHit(names[id] ?: id, "Weight", w)) }
        return out
    }

    /** Weekly Hard-Set Equivalents per group. Primary credit 1.0 / secondary 0.5 come from each exercise's credits. */
    fun weeklyHardSets(sets: List<SetLike>, credits: Map<String, Map<String, Double>>, refE1rm: Map<String, Double>): Map<MuscleGroup, Double> {
        val out = HashMap<MuscleGroup, Double>()
        for (s in sets) {
            if (s.type == SetType.WARMUP) continue
            val rir = EffectiveLoad.rirFrom(s.rpe, s.type.code)
            val own = EffectiveLoad.e1rm(s.weightKg, s.reps, rir)
            val hse = EffectiveLoad.hardSetEquivalent(s.reps, rir, s.weightKg, refE1rm[s.exerciseId], own).hse
            for ((k, w) in credits[s.exerciseId].orEmpty()) {
                val g = MuscleGroup.fromKey(k) ?: continue
                out[g] = (out[g] ?: 0.0) + hse * w
            }
        }
        return out
    }

    /** No data at all -> every group unknown (null); data but none for a group -> 0. */
    fun toLoads(hse: Map<MuscleGroup, Double>, anyData: Boolean): Map<MuscleGroup, WeeklyMuscleLoad> =
        MuscleGroup.values().associateWith { g -> if (!anyData) WeeklyMuscleLoad(null) else WeeklyMuscleLoad((hse[g] ?: 0.0).roundToInt()) }

    fun restSuggestSeconds(reps: Int, type: SetType) = when {
        type == SetType.WARMUP -> 45; reps <= 5 -> 180; reps <= 12 -> 120; else -> 75
    }

    // ---- validation (templates) -------------------------------------------------
    fun validatePrescription(sets: Int, repMin: Int, repMax: Int, kg: Double?, rest: Int?, rpe: Double?): String? = when {
        sets !in 1..20 -> "Sets must be 1–20"
        repMin !in 1..100 || repMax !in 1..100 -> "Reps must be 1–100"
        repMax < repMin -> "Max reps can't be below min reps"
        kg != null && (kg < 0 || kg > 1000) -> "Load must be 0–1000 kg"
        rest != null && rest !in 0..1800 -> "Rest must be 0–30 min"
        rpe != null && (rpe < 1 || rpe > 10) -> "RPE must be 1–10"
        else -> null
    }

    // ---- routines ---------------------------------------------------------------
    data class DayPlan(val label: String, val weekday: Int?, val rest: Boolean, val templateId: Long?)

    /** Issues to show in the preview before activation. */
    fun routineIssues(days: List<DayPlan>): List<String> {
        val out = ArrayList<String>()
        if (days.none { !it.rest }) out.add("Add at least one training day")
        days.forEach { if (!it.rest && it.templateId == null) out.add("${it.label.ifBlank { "Unnamed day" }} has no workout assigned") }
        days.filter { it.weekday != null }.groupBy { it.weekday }.filter { it.value.size > 1 }.forEach { (w, _) -> out.add("More than one day is scheduled on ${dayName(w!!)}") }
        return out
    }

    fun dayName(w: Int) = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").getOrElse(w - 1) { "?" }

    /** Today's planned day: weekday-fixed routines match by weekday; flexible ones return the next training day after the last completed index. */
    fun plannedFor(days: List<RoutineDay>, todayWeekday: Int, completedCount: Int): RoutineDay? {
        val fixed = days.filter { it.weekday != null }
        if (fixed.isNotEmpty()) return fixed.firstOrNull { it.weekday == todayWeekday && !it.rest }
        val training = days.filter { !it.rest }
        if (training.isEmpty()) return null
        return training[completedCount % training.size]
    }

    /** Adherence from real data only: planned = weekday-fixed training days elapsed, completed = distinct sessions linked to routine days. null if nothing was planned. */
    fun adherence(planned: Int, completed: Int): Double? = if (planned <= 0) null else (completed.toDouble() / planned).coerceAtMost(1.0)
}
