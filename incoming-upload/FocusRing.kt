package com.focus.app.design.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors

/** White arc over a #2A2A2D track. [glow] adds the low-alpha wide-stroke fallback bloom. */
@Composable
fun FocusRing(
    progress: Float,
    diameter: Dp,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = strokeWidth.toPx()
            val glowExtra = if (glow) 10.dp.toPx() else 0f
            val inset = (sw + glowExtra) / 2f
            val topLeft = Offset(inset, inset)
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)

            drawArc(
                color = FocusColors.Track,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = sw),
            )
            val sweep = 360f * progress.coerceIn(0f, 1f)
            if (sweep > 0f) {
                if (glow) {
                    drawArc(
                        color = FocusColors.GlowSubtle,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = sw + glowExtra, cap = StrokeCap.Round),
                    )
                }
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}
