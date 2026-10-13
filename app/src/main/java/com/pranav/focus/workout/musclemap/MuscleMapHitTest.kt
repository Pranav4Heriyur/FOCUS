package com.pranav.focus.workout.musclemap

import kotlin.math.abs

/** A closed polygon in normalised artwork space (0..1). xy is flattened x0,y0,x1,y1,... */
class MaskPolygon(val group: MuscleGroup, val xy: FloatArray) {
    init {
        require(xy.size >= 6 && xy.size % 2 == 0) { "polygon for $group needs >= 3 points" }
        require(xy.all { it in 0f..1f }) { "polygon for $group has a coordinate outside 0..1" }
    }

    val area: Float by lazy {
        var a = 0f
        val n = xy.size / 2
        for (i in 0 until n) {
            val j = (i + 1) % n
            a += xy[2 * i] * xy[2 * j + 1] - xy[2 * j] * xy[2 * i + 1]
        }
        abs(a) / 2f
    }

    /** Ray-casting point-in-polygon. */
    fun contains(x: Float, y: Float): Boolean {
        val n = xy.size / 2
        var inside = false
        var j = n - 1
        for (i in 0 until n) {
            val xi = xy[2 * i]; val yi = xy[2 * i + 1]
            val xj = xy[2 * j]; val yj = xy[2 * j + 1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
            j = i
        }
        return inside
    }
}

/** Where artwork sits in its container after ContentScale.Fit (letterboxed, centred). */
data class ArtworkFit(val left: Float, val top: Float, val width: Float, val height: Float) {
    /** Container point to normalised artwork space, or null if it lands in letterbox padding. */
    fun toArtwork(px: Float, py: Float): Pair<Float, Float>? {
        val nx = (px - left) / width
        val ny = (py - top) / height
        return if (nx in 0f..1f && ny in 0f..1f) nx to ny else null
    }

    companion object {
        fun fit(containerW: Float, containerH: Float, artW: Float, artH: Float): ArtworkFit {
            require(containerW > 0f && containerH > 0f && artW > 0f && artH > 0f) { "sizes must be positive" }
            val scale = minOf(containerW / artW, containerH / artH)
            val w = artW * scale
            val h = artH * scale
            return ArtworkFit((containerW - w) / 2f, (containerH - h) / 2f, w, h)
        }
    }
}

/** Tap hit-test. Overlapping polygons resolve to the SMALLEST containing polygon. */
fun hitTest(polygons: List<MaskPolygon>, fit: ArtworkFit, px: Float, py: Float): MuscleGroup? {
    val (x, y) = fit.toArtwork(px, py) ?: return null
    return polygons.filter { it.contains(x, y) }.minByOrNull { it.area }?.group
}
