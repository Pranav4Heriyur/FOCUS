package com.pranav.focus.workout.musclemap

// Plain assertion harness. JUnit and Gradle are unreachable from the build sandbox, so this runs with kotlinc.
private var passed = 0
private fun check(name: String, cond: Boolean) {
    if (!cond) throw AssertionError("FAILED: $name")
    passed++
}
private fun expectIllegal(name: String, block: () -> Unit) {
    try { block() } catch (e: IllegalArgumentException) { passed++; return }
    throw AssertionError("FAILED (no exception): $name")
}

private fun centroid(p: MaskPolygon): Pair<Float, Float> {
    var sx = 0f; var sy = 0f
    val n = p.xy.size / 2
    for (i in 0 until n) { sx += p.xy[2 * i]; sy += p.xy[2 * i + 1] }
    return sx / n to sy / n
}

fun main() {
    // --- set bands
    check("0 LOW", setBandFor(0) == SetBand.LOW)
    check("9 LOW", setBandFor(9) == SetBand.LOW)
    check("10 IN_RANGE", setBandFor(10) == SetBand.IN_RANGE)
    check("20 IN_RANGE", setBandFor(20) == SetBand.IN_RANGE)
    check("21 HIGH", setBandFor(21) == SetBand.HIGH)
    check("null is unknown, not LOW", setBandFor(null) == null)
    expectIllegal("negative rejected") { setBandFor(-1) }

    // --- unknown vs zero via source
    check("no source => every group unknown", countsFor(null).values.all { it == null })
    val empty = countsFor(emptyMap())
    check("source with no sets => known zero", empty.values.all { it == 0 })
    val some = countsFor(mapOf(MuscleGroup.QUADRICEPS to 12))
    check("quads = 12", some[MuscleGroup.QUADRICEPS] == 12)
    check("unlisted group is known zero when source exists", some[MuscleGroup.CALVES] == 0)
    expectIllegal("negative source count rejected") { countsFor(mapOf(MuscleGroup.CALVES to -2)) }

    // --- every group has at least one polygon in its own view
    for (g in MuscleGroup.values()) {
        val list = if (g.view == MuscleView.FRONT) FRONT_POLYGONS else BACK_POLYGONS
        check("$g has polygons in ${g.view}", list.any { it.group == g })
    }
    // no polygon belongs to the wrong view
    check("front list has only front groups", FRONT_POLYGONS.all { it.group.view == MuscleView.FRONT })
    check("back list has only back groups", BACK_POLYGONS.all { it.group.view == MuscleView.BACK })

    // --- every polygon is inside the artwork
    for (p in FRONT_POLYGONS + BACK_POLYGONS) {
        check("polygon ${p.group} >= 3 points", p.xy.size >= 6)
    }

    // --- label/region identity: each polygon's own centroid hits its own group (no misleading overlap)
    for ((name, list) in listOf("front" to FRONT_POLYGONS, "back" to BACK_POLYGONS)) {
        val fit = ArtworkFit.fit(CHART_WIDTH, CHART_HEIGHT, CHART_WIDTH, CHART_HEIGHT) // identity mapping
        for (p in list) {
            val (cx, cy) = centroid(p)
            val hit = hitTest(list, fit, cx * CHART_WIDTH, cy * CHART_HEIGHT)
            check("$name centroid of ${p.group} hits ${p.group}, got $hit", hit == p.group)
        }
    }

    // --- letterbox fit and mapping
    val fit = ArtworkFit.fit(400f, 800f, 200f, 200f)
    check("fit 400x400", fit.width == 400f && fit.height == 400f)
    check("letterbox top 200", fit.top == 200f && fit.left == 0f)
    check("tap in letterbox => null", fit.toArtwork(100f, 50f) == null)
    check("centre maps to 0.5", fit.toArtwork(200f, 400f) == (0.5f to 0.5f))
    val wide = ArtworkFit.fit(800f, 300f, 400f, 300f)
    check("wide container pillarboxes horizontally", wide.left == 200f && wide.top == 0f && wide.width == 400f)

    // --- hit precedence: smallest polygon wins
    val big = MaskPolygon(MuscleGroup.PECTORALS, floatArrayOf(0.1f, 0.1f, 0.9f, 0.1f, 0.9f, 0.9f, 0.1f, 0.9f))
    val small = MaskPolygon(MuscleGroup.ABDOMINALS, floatArrayOf(0.4f, 0.4f, 0.6f, 0.4f, 0.6f, 0.6f, 0.4f, 0.6f))
    val square = ArtworkFit(0f, 0f, 1f, 1f)
    check("inside big only => pectorals", hitTest(listOf(big, small), square, 0.2f, 0.2f) == MuscleGroup.PECTORALS)
    check("overlap => smallest (abdominals)", hitTest(listOf(big, small), square, 0.5f, 0.5f) == MuscleGroup.ABDOMINALS)
    check("outside => null", hitTest(listOf(big, small), square, 0.99f, 0.01f) == null)

    expectIllegal("polygon coord outside 0..1 rejected") { MaskPolygon(MuscleGroup.CALVES, floatArrayOf(0f, 0f, 1.5f, 0f, 0f, 1f)) }

    println("MUSCLE MAP CHECKS PASSED: $passed")
}
