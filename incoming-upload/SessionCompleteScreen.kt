package com.focus.app.feature.focus

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focus.app.data.model.SessionResult
import com.focus.app.design.color.FocusColors
import com.focus.app.design.component.FocusButton
import com.focus.app.design.component.FocusButtonStyle
import com.focus.app.design.component.FocusCard
import com.focus.app.design.component.FocusIcon
import com.focus.app.design.component.FocusRing
import com.focus.app.design.component.FocusText
import com.focus.app.design.component.ScreenColumn
import com.focus.app.design.motion.FocusMotion
import com.focus.app.design.motion.LocalReduceMotion
import com.focus.app.design.shape.FocusDimens
import com.focus.app.design.typography.FocusType
import com.focus.app.util.formatClock
import kotlin.math.cos
import kotlin.math.sin

/** A5: Session complete. Ring fills, then a static radial burst with fine sparks (no blur needed). */
@Composable
fun SessionCompleteScreen(
    result: SessionResult,
    onLogQuestions: () -> Unit,
    onDone: () -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val ring = remember { Animatable(if (reduce) 1f else 0f) }
    val burst = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        ring.animateTo(1f, tween(if (reduce) 0 else FocusMotion.RingFill, easing = FocusMotion.Standard))
        burst.animateTo(1f, tween(if (reduce) 0 else FocusMotion.Celebration, easing = FocusMotion.Emphasized))
    }

    ScreenColumn {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f * burst.value),
                                Color.White.copy(alpha = 0.04f * burst.value),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                    )
                    val sparks = 24
                    for (i in 0 until sparks) {
                        val angle = Math.toRadians(i * 360.0 / sparks)
                        val inner = size.minDimension * 0.36f
                        val length = (if (i % 2 == 0) 18.dp else 10.dp).toPx() * burst.value
                        val dx = cos(angle).toFloat()
                        val dy = sin(angle).toFloat()
                        drawLine(
                            color = Color.White.copy(alpha = (if (i % 2 == 0) 0.25f else 0.15f) * burst.value),
                            start = center + Offset(dx * inner, dy * inner),
                            end = center + Offset(dx * (inner + length), dy * (inner + length)),
                            strokeWidth = 1.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
                FocusRing(progress = ring.value, diameter = 160.dp, strokeWidth = 8.dp, glow = true) {
                    FocusIcon(Icons.Rounded.Check, size = 64.dp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        FocusText(
            "${formatClock(result.minutes * 60_000L)} locked in",
            style = FocusType.MetricNumeral,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        FocusText(
            result.subject.uppercase(),
            style = FocusType.TrackedCaps,
            color = FocusColors.Secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(FocusDimens.SectionGap))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FocusDimens.CardGap)) {
            StatTile("FOCUS TIME", "${result.minutes} min", Modifier.weight(1f))
            StatTile("STREAK", "${result.streakDays} days", Modifier.weight(1f))
            StatTile("XP", "+${result.xpGained}", Modifier.weight(1f))
        }

        Spacer(Modifier.height(FocusDimens.CardGap))
        FocusCard {
            FocusText("+${result.earnedMin} min earned", style = FocusType.SectionTitle)
            FocusText("added to your Time Bank", style = FocusType.Body, color = FocusColors.Secondary)
        }

        Spacer(Modifier.height(FocusDimens.SectionGap))
        FocusButton("Log questions", onClick = onLogQuestions, style = FocusButtonStyle.Outlined)
        Spacer(Modifier.height(FocusDimens.CardGap))
        FocusButton("Done", onClick = onDone)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    FocusCard(
        modifier = modifier,
        shape = com.focus.app.design.shape.FocusShapes.Compact,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        FocusText(label, style = FocusType.TrackedCaps, color = FocusColors.Secondary, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        FocusText(value, style = FocusType.CardTitle, maxLines = 1)
    }
}
