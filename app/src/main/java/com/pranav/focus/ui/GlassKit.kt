package com.pranav.focus.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Shared glass materials for the A18 planner (see docs: A18 visual fidelity addendum).
 *
 * Rules: text and icon geometry stay sharp. Light is drawn BEHIND the geometry as separate, low-alpha passes.
 * No blur filters and no render effects (the target phone is a Galaxy A32): glow is stacked translucent
 * shapes, same approach as Modifier.bloom. Only anchors glow (ring arc, hero numerals, selected chip, primary capsule).
 */

/** Extra tokens that sit on top of the project palette [C]. */
object G {
    val CardBase = Color(0xFF080809)
    val TileBase = Color(0xFF111112)
    val Edge = Color.White.copy(alpha = 0.13f)
    val EdgeSelected = Color.White.copy(alpha = 0.17f)
    val Specular = Color.White.copy(alpha = 0.095f)
    val Rule = Color.White.copy(alpha = 0.07f)

    /** The single documented colour exception in the planner: overlap / destructive warning. */
    val Red = Color(0xFFFF5A4F)
}

/**
 * Glass card layers, in order: separation shadow, dark base, top-left reflection, faint return reflection,
 * inset top/bottom specular lines, 1 dp edge. Everything is drawn behind content.
 */
fun Modifier.glassSurface(
    radius: Dp = 18.dp,
    selected: Boolean = false,
    elevated: Boolean = true,
    base: Color = G.CardBase,
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .then(
            if (elevated) Modifier.shadow(
                10.dp, shape, clip = false,
                ambientColor = Color.Black.copy(alpha = 0.36f), spotColor = Color.Black.copy(alpha = 0.36f),
            ) else Modifier
        )
        .clip(shape)
        .background(base)
        .drawWithCache {
            val w = size.width
            val h = size.height
            val r = minOf(radius.toPx(), h / 2f, w / 2f)
            val diag = Brush.linearGradient(
                colors = listOf(Color.White.copy(alpha = 0.045f), Color.White.copy(alpha = 0.012f), Color.White.copy(alpha = 0.022f)),
                start = Offset.Zero, end = Offset(w, h),
            )
            val reflection = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.055f), Color.Transparent),
                center = Offset(w * 0.08f, 0f), radius = maxOf(w * 1.2f, 1f),
            )
            val back = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.025f), Color.Transparent),
                center = Offset(w, h), radius = maxOf(w * 0.7f, 1f),
            )
            val specTop = Brush.horizontalGradient(
                0f to Color.Transparent, 0.18f to G.Specular, 0.82f to G.Specular, 1f to Color.Transparent,
                startX = r * 0.5f, endX = maxOf(w - r * 0.5f, r * 0.5f + 1f),
            )
            val specBottom = Brush.horizontalGradient(
                0f to Color.Transparent, 0.25f to Color.White.copy(alpha = 0.025f), 0.75f to Color.White.copy(alpha = 0.025f), 1f to Color.Transparent,
                startX = r * 0.5f, endX = maxOf(w - r * 0.5f, r * 0.5f + 1f),
            )
            onDrawBehind {
                val px = 1.dp.toPx()
                drawRect(diag)
                drawRect(reflection)
                drawRect(back)
                drawRect(specTop, topLeft = Offset(r * 0.5f, px), size = Size(maxOf(w - r, 1f), px))
                drawRect(specBottom, topLeft = Offset(r * 0.5f, h - 2f * px), size = Size(maxOf(w - r, 1f), px))
            }
        }
        .border(
            1.dp,
            if (selected) Brush.linearGradient(listOf(G.EdgeSelected, G.EdgeSelected))
            else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.13f))),
            shape,
        )
}

/** Drop-in for FocusCard inside the planner. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null,
    radius: Dp = 18.dp, padding: Dp = 16.dp, selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.glassSurface(radius, selected)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Ambient wash behind the header/hero area only. Peak 4.5% white, gone well before the card edges. */
fun Modifier.ambientWash(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.045f), Color.Transparent),
            center = Offset(size.width * 0.5f, 0f), radius = size.width * 0.9f,
        )
    )
}

/**
 * Hero text: a sharp white final glyph over two faint shadow-only copies (near + far diffusion).
 * Use for the large progress numeral and key values only.
 */
@Composable
fun GlowText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    weight: FontWeight = FontWeight.Bold,
    letterSpacing: TextUnit = (-0.4).sp,
    glow: Boolean = true,
) {
    val density = LocalDensity.current
    val style = TextStyle(fontSize = fontSize, lineHeight = fontSize * 1.14f, fontWeight = weight, letterSpacing = letterSpacing)
    Box(modifier) {
        if (glow) {
            Text(
                text, color = Color.White.copy(alpha = 0.035f),
                style = style.copy(shadow = Shadow(Color.White.copy(alpha = 0.10f), Offset.Zero, with(density) { 22.dp.toPx() })),
            )
            Text(
                text, color = Color.White.copy(alpha = 0.08f),
                style = style.copy(shadow = Shadow(Color.White.copy(alpha = 0.22f), Offset.Zero, with(density) { 9.dp.toPx() })),
            )
        }
        Text(text, color = Color.White, style = style)
    }
}

/**
 * Three-pass progress ring (wide diffusion, mid halo, crisp core) drawn over the same arc geometry.
 * The glow passes overflow the box by design; the number is a sharp [GlowText] on top.
 */
@Composable
fun GlowRing(frac: Float, size: Dp, label: AnnotatedString, labelSize: TextUnit = 20.sp, strokeDp: Dp = 6.dp) {
    val f = frac.coerceIn(0f, 1f)
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val w = strokeDp.toPx()
            val tl = Offset(w * 0.5f, w * 0.5f)
            val sz = Size(this.size.width - w, this.size.height - w)
            drawArc(C.Track, 0f, 360f, false, tl, sz, style = Stroke(w))
            if (f > 0f) {
                val sweep = 360f * f
                drawArc(Color.White.copy(alpha = 0.08f), -90f, sweep, false, tl, sz, style = Stroke(w * 3.0f, cap = StrokeCap.Round))
                drawArc(Color.White.copy(alpha = 0.14f), -90f, sweep, false, tl, sz, style = Stroke(w * 2.2f, cap = StrokeCap.Round))
                drawArc(Color.White.copy(alpha = 0.22f), -90f, sweep, false, tl, sz, style = Stroke(w * 1.6f, cap = StrokeCap.Round))
                drawArc(Color.White.copy(alpha = 0.98f), -90f, sweep, false, tl, sz, style = Stroke(w, cap = StrokeCap.Round))
            }
        }
        GlowText(label, fontSize = labelSize, weight = FontWeight.Bold, letterSpacing = (-0.3).sp)
    }
}

/** Linear progress with the same layering: dark track, soft diffusion behind, crisp core. */
@Composable
fun GlowBar(frac: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val f = frac.coerceIn(0f, 1f)
    Box(
        modifier.fillMaxWidth().height(height).drawBehind {
            val h = size.height
            val w = size.width * f
            drawRoundRect(C.Track, size = size, cornerRadius = CornerRadius(h / 2f))
            if (w > 0f) {
                val e1 = h * 0.9f
                val e2 = h * 0.45f
                drawRoundRect(Color.White.copy(alpha = 0.07f), Offset(-e1, -e1), Size(w + 2 * e1, h + 2 * e1), CornerRadius(h / 2f + e1))
                drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(-e2, -e2), Size(w + 2 * e2, h + 2 * e2), CornerRadius(h / 2f + e2))
                drawRoundRect(Color.White.copy(alpha = 0.98f), size = Size(w, h), cornerRadius = CornerRadius(h / 2f))
            }
        },
    )
}

/** Primary capsule: near-white fill, 72% edge, restrained halo; 0.985 press scale over 120 ms. */
@Composable
fun GlassPrimaryButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f, tween(120), label = "capsulePress")
    Box(
        modifier.fillMaxWidth().height(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (enabled) Modifier.bloom(if (pressed) 9.dp else 14.dp, if (pressed) 0.16f else 0.22f) else Modifier)
            .clip(CircleShape)
            .background(if (enabled) Color(0xFFFAFAFA) else C.Track)
            .border(1.dp, if (enabled) Color.White.copy(alpha = 0.72f) else Color.Transparent, CircleShape)
            .clickable(interactionSource = src, indication = null, enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) Color(0xFF050505) else C.Text3, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Segmented control: dark glass track, white active segment with black label and a small halo. */
@Composable
fun GlassSeg(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().glassSurface(radius = 999.dp, elevated = false).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) Color.White else Color.Transparent, tween(180), label = "segBg")
            val fg by animateColorAsState(if (sel) Color.Black else C.Text2, tween(180), label = "segFg")
            Box(
                Modifier.weight(1f).height(28.dp)
                    .then(if (sel) Modifier.bloom(5.dp, 0.14f) else Modifier)
                    .clip(CircleShape).background(bg).clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) { Text(o, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

/** Icon tile: dark base, soft diagonal sheen, top highlight, 12% edge. The glyph itself is untouched. */
@Composable
fun GlassTile(@DrawableRes icon: Int, size: Dp = 40.dp, radius: Dp = 12.dp, active: Boolean = false) {
    val shape = RoundedCornerShape(radius)
    Box(
        Modifier.size(size).clip(shape).background(G.TileBase)
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.025f), Color.White.copy(alpha = 0.012f)),
                        start = Offset.Zero, end = Offset(this.size.width, this.size.height),
                    )
                )
                drawRect(Color.White.copy(alpha = 0.08f), Offset(radius.toPx() * 0.4f, 1.dp.toPx()), Size(this.size.width - radius.toPx() * 0.8f, 1.dp.toPx()))
            }
            .border(1.dp, Color.White.copy(alpha = if (active) 0.2f else 0.12f), shape),
        contentAlignment = Alignment.Center,
    ) { Ico(icon, (size.value * 0.5f).dp) }
}
