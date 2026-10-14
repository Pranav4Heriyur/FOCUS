package com.pranav.focus.workout.ui

import com.pranav.focus.workout.domain.Prescription

/** Shared formatting and parsing for the builders. Numbers are shown without a trailing ".0". */
internal fun fmtNum(v: Double): String = if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.1f", v)

internal fun prescriptionSummary(p: Prescription): String = buildString {
    append("${p.targetSets} × ${p.repMin}–${p.repMax} reps")
    p.targetLoadKg?.let { append(" · ${fmtNum(it)} kg") }
    p.restSeconds?.let { append(" · ${it}s rest") }
    p.targetRpe?.let { append(" · RPE ${fmtNum(it)}") }
}

/** Parses prescription form fields. Blank optional fields mean "not set". Non-blank invalid input is an error. */
internal fun parsePrescription(
    sets: String, repMin: String, repMax: String, load: String, rest: String, rpe: String, notes: String,
): Result<Prescription> {
    val s = sets.trim().toIntOrNull() ?: return Result.failure(IllegalArgumentException("Sets must be a whole number"))
    val min = repMin.trim().toIntOrNull() ?: return Result.failure(IllegalArgumentException("Minimum reps must be a whole number"))
    val max = repMax.trim().toIntOrNull() ?: return Result.failure(IllegalArgumentException("Maximum reps must be a whole number"))
    val kg = if (load.isBlank()) null else (load.trim().toDoubleOrNull() ?: return Result.failure(IllegalArgumentException("Load must be a number")))
    val restS = if (rest.isBlank()) null else (rest.trim().toIntOrNull() ?: return Result.failure(IllegalArgumentException("Rest must be whole seconds")))
    val rp = if (rpe.isBlank()) null else (rpe.trim().toDoubleOrNull() ?: return Result.failure(IllegalArgumentException("RPE must be a number")))
    return Result.success(Prescription(s, min, max, kg, restS, rp, notes.trim()))
}
