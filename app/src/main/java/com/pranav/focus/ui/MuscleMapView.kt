package com.pranav.focus.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pranav.focus.data.BodyFace
import com.pranav.focus.data.MuscleAtlas
import com.pranav.focus.data.MuscleGroup
import com.pranav.focus.data.MuscleRegion
import com.pranav.focus.data.MuscleStatus
import com.pranav.focus.data.WeeklyMuscleLoad

private const val MAP_W = 100f
private const val MAP_H = 205f

/**
 * Front/back muscle map. The SAME polygons (MuscleAtlas) drive drawing and tap hit-testing, so shapes and targets always agree
 * after scaling. Monochrome: unknown = outline only, under = dim fill, on target = bright white, over = bright + two-layer halo behind a crisp outline.
 */
@Composable
fun MuscleMapView(
    face: BodyFace, loads: Map<MuscleGroup, WeeklyMuscleLoad>, selected: MuscleGroup?, onSelect: (MuscleGroup?) -> Unit, modifier: Modifier = Modifier,
) {
    val reduce = reduceMotion()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        0.6f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse), label = "p",
    )
    val haloAlpha = if (reduce) 0.8f else pulse
    val regions = MuscleAtlas.regionsOn(face)
    Canvas(
        modifier.fillMaxWidth().aspectRatio(MAP_W / MAP_H)
            .semantics { contentDescription = if (face == BodyFace.FRONT) "Front muscle map. Tap a muscle." else "Back muscle map. Tap a muscle." }
            .pointerInput(face) {
                detectTapGestures { o ->
                    val s = minOf(size.width / MAP_W, size.height / MAP_H)
                    val ox = (size.width - MAP_W * s) / 2f; val oy = (size.height - MAP_H * s) / 2f
                    val hit = MuscleAtlas.hit(face, (o.x - ox) / s, (o.y - oy) / s)
                    onSelect(hit?.group)
                }
            },
    ) {
        val s = minOf(size.width / MAP_W, size.height / MAP_H)
        val ox = (size.width - MAP_W * s) / 2f; val oy = (size.height - MAP_H * s) / 2f
        fun map(p: Pair<Float, Float>) = Offset(ox + p.first * s, oy + p.second * s)
        drawBodyDecor(s, ox, oy)
        // fills first, then halos behind outlines for OVER, then outlines on top
        for (r in regions) {
            val st = loads[r.group]?.status ?: MuscleStatus.UNKNOWN
            val fill = when (st) {
                MuscleStatus.UNKNOWN -> Color.Transparent
                MuscleStatus.UNDER_TARGET -> Color.White.copy(alpha = 0.22f)
                MuscleStatus.ON_TARGET, MuscleStatus.OVER_TARGET -> Color.White.copy(alpha = 0.88f)
            }
            for (poly in r.polygons()) {
                val path = polyPath(poly, ::map)
                if (st == MuscleStatus.OVER_TARGET) {
                    drawPath(path, Color.White.copy(alpha = 0.10f * haloAlpha), style = Stroke(width = 9f * s / 2f, join = StrokeJoin.Round))
                    drawPath(path, Color.White.copy(alpha = 0.16f * haloAlpha), style = Stroke(width = 4.5f * s / 2f, join = StrokeJoin.Round))
                }
                if (fill != Color.Transparent) drawPath(path, fill, style = Fill)
            }
        }
        for (r in regions) {
            val isSel = r.group == selected
            val st = loads[r.group]?.status ?: MuscleStatus.UNKNOWN
            val line = if (isSel) Color.White else if (st == MuscleStatus.ON_TARGET || st == MuscleStatus.OVER_TARGET) Color.Black.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.30f)
            val w = if (isSel) 2.4f else 0.9f
            for (poly in r.polygons()) drawPath(polyPath(poly, ::map), line, style = Stroke(width = w * s / 2f, join = StrokeJoin.Round, cap = StrokeCap.Round))
        }
    }
}

private fun polyPath(poly: List<Pair<Float, Float>>, map: (Pair<Float, Float>) -> Offset): Path = Path().apply {
    poly.forEachIndexed { i, p -> val o = map(p); if (i == 0) moveTo(o.x, o.y) else lineTo(o.x, o.y) }
    close()
}

/** Non-selectable anatomy context: head, hands, feet, knees. */
private fun DrawScope.drawBodyDecor(s: Float, ox: Float, oy: Float) {
    val c = Color.White.copy(alpha = 0.14f)
    val st = Stroke(width = 1.2f * s / 2f)
    fun oval(cx: Float, cy: Float, rx: Float, ry: Float) =
        drawOval(c, Offset(ox + (cx - rx) * s, oy + (cy - ry) * s), Size(rx * 2 * s, ry * 2 * s), style = st)
    oval(50f, 11f, 8.2f, 11f)                                   // head
    for (sx in listOf(1f, -1f)) {                               // hands, feet, knees (mirrored)
        oval(50f + sx * 39f, 116f, 3.6f, 7f)
        oval(50f + sx * 13f, 198f, 5f, 4f)
        oval(50f + sx * 11.5f, 149f, 5.5f, 3.2f)
    }
}
