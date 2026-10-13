package com.pranav.focus.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Focus glass materials for the A1 / A2 / A8 / A9 / A10 screens.
 * Near-black layered surfaces, selective white diffusion BEHIND crisp text/rings/glyphs.
 * No platform blur is required for ordinary cards (gradients only), so it runs fine on an A32.
 */

object FG {
    val Base = Color(0xFF080809)
    val Tile = Color(0xFF0E0E0F)
    val Edge = Color.White
}

private val Ease = FastOutSlowInEasing
const val FAST_MS = 140
const val STD_MS = 220
const val RING_MS = 650

/** Smoked-glass surface: base, faint top-left reflection, diagonal gradient, 1dp edge, top specular. */
fun Modifier.fgSurface(radius: Dp = 22.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape).background(FG.Base).drawBehind {
        val w = size.width; val h = size.height
        drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.055f), Color.Transparent), center = Offset.Zero, radius = 260.dp.toPx()))
        drawRect(Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.045f), Color.White.copy(alpha = 0.012f), Color.White.copy(alpha = 0.022f)),
            start = Offset.Zero, end = Offset(w, h),
        ))
        val r = radius.toPx(); val s = 1.dp.toPx()
        if (w > 2 * r) {
            drawLine(Color.White.copy(alpha = 0.095f), Offset(r, s), Offset(w - r, s), s)
            drawLine(Color.White.copy(alpha = 0.025f), Offset(r, h - s), Offset(w - r, h - s), s)
        }
    }.border(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.17f), Color.White.copy(alpha = 0.07f))), shape)
}

/** Raised icon tile: near-black, restrained reflection, 12% edge. Glyph is drawn by the caller, separately and crisp. */
@Composable
fun FgTile(size: Dp = 52.dp, radius: Dp = 15.dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.size(size).clip(shape).background(FG.Tile).drawBehind {
            drawRect(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent), center = Offset(this.size.width * 0.25f, 0f), radius = this.size.width))
            val s = 1.dp.toPx()
            drawLine(Color.White.copy(alpha = 0.08f), Offset(radius.toPx(), s), Offset(this.size.width - radius.toPx(), s), s)
        }.border(1.dp, Color.White.copy(alpha = 0.12f), shape),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** White halo drawn BEHIND an opaque white shape of the same outline (CTA, selected pill, switch thumb). */
fun Modifier.whiteHalo(corner: Dp, vararg layers: Pair<Dp, Float>): Modifier = drawBehind {
    drawIntoNative { c ->
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        val r = corner.toPx().coerceAtMost(minOf(size.width, size.height) / 2f)
        layers.forEach { (blur, a) ->
            p.setShadowLayer(blur.toPx(), 0f, 0f, android.graphics.Color.argb((a * 255).toInt(), 255, 255, 255))
            c.drawRoundRect(0f, 0f, size.width, size.height, r, r, p)
        }
    }
}

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIntoNative(block: (android.graphics.Canvas) -> Unit) =
    block(drawContext.canvas.nativeCanvas)

/** Primary white CTA: 6dp @25% + 18dp @16% halo. */
@Composable
fun FgPrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(56.dp)
            .then(if (enabled) Modifier.whiteHalo(28.dp, 18.dp to 0.16f, 6.dp to 0.25f) else Modifier)
            .clip(CircleShape).background(if (enabled) C.White else C.Track)
            .clickable(enabled = enabled, role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) C.Black else C.Text3, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

/** Outlined capsule action (Allow, Import from contacts, How it works, Edit). */
@Composable
fun GlassOutlineButton(
    text: String, modifier: Modifier = Modifier, icon: Int? = null, trailingChevron: Boolean = false,
    height: Dp = 44.dp, fontSize: TextUnit = 15.sp, onClick: () -> Unit,
) {
    Row(
        modifier.height(height).clip(CircleShape).background(Color.White.copy(alpha = 0.02f))
            .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape).clickable(role = Role.Button) { onClick() }.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Ico(icon, 20.dp); Spacer(Modifier.size(8.dp)) }
        Text(text, color = C.White, fontSize = fontSize, fontWeight = FontWeight.Medium, maxLines = 1)
        if (trailingChevron) { Spacer(Modifier.size(4.dp)); Ico(com.pranav.focus.R.drawable.focus_chevron_right_white, 16.dp) }
    }
}

/** Selectable capsule (preset times). Selected = white with restrained halo; others = glass. */
@Composable
fun GlassChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg by animateFloatAsState(if (selected) 1f else 0f, tween(STD_MS, easing = Ease), label = "chip")
    Box(
        modifier.height(48.dp)
            .then(if (selected) Modifier.whiteHalo(24.dp, 14.dp to 0.16f, 5.dp to 0.22f) else Modifier)
            .then(if (selected) Modifier.clip(CircleShape).background(C.White) else Modifier.fgSurface(24.dp))
            .clickable(role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Color(1f - bg, 1f - bg, 1f - bg), fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1) }
}

/**
 * Text with diffusion behind a crisp glyph. The same white glyphs are stacked with different shadow radii,
 * so only the shadows differ; the top layer is never blurred. Use for hero values only.
 */
@Composable
fun FgGlowText(
    text: String, modifier: Modifier = Modifier, fontSize: TextUnit, fontWeight: FontWeight = FontWeight.Bold,
    color: Color = C.White, lineHeight: TextUnit = TextUnit.Unspecified, letterSpacing: TextUnit = TextUnit.Unspecified,
    tiers: Int = 3, maxLines: Int = 1,
) {
    val d = LocalDensity.current
    fun style(dp: Float, a: Float) = TextStyle(
        fontSize = fontSize, fontWeight = fontWeight, color = color, lineHeight = lineHeight, letterSpacing = letterSpacing,
        shadow = Shadow(Color.White.copy(alpha = a), Offset.Zero, with(d) { dp.dp.toPx() }),
    )
    Box(modifier) {
        if (tiers >= 3) Text(text, Modifier.clearAndSetSemantics { }, style = style(22f, 0.10f), maxLines = maxLines)
        if (tiers >= 2) Text(text, Modifier.clearAndSetSemantics { }, style = style(9f, 0.22f), maxLines = maxLines)
        Text(text, style = style(3f, 0.30f), maxLines = maxLines)
    }
}

/** Dark track + three-pass white arc (wide faint, mid, crisp core with round caps). */
@Composable
fun FgRing(progress: Float, modifier: Modifier = Modifier, stroke: Dp = 6.dp, animate: Boolean = true) {
    val anim = animate && !reduceMotion()
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(if (anim) RING_MS else 0, easing = Ease), label = "ring")
    Canvas(modifier) {
        val s = stroke.toPx()
        val pad = s * 1.6f
        val sz = Size(size.width - pad * 2, size.height - pad * 2)
        val tl = Offset(pad, pad)
        drawArc(Color.White.copy(alpha = 0.12f), -90f, 360f, false, tl, sz, style = Stroke(s))
        val sweep = 360f * p
        if (sweep > 0.5f) {
            drawArc(Color.White.copy(alpha = 0.075f), -90f, sweep, false, tl, sz, style = Stroke(s * 3.0f, cap = StrokeCap.Round))
            drawArc(Color.White.copy(alpha = 0.19f), -90f, sweep, false, tl, sz, style = Stroke(s * 1.7f, cap = StrokeCap.Round))
            drawArc(Color.White, -90f, sweep, false, tl, sz, style = Stroke(s, cap = StrokeCap.Round))
        }
    }
}

/** Horizontal bar with the same three-pass construction. Height is the crisp core thickness. */
@Composable
fun FgBar(frac: Float, modifier: Modifier = Modifier, thickness: Dp = 8.dp, glow: Boolean = true, animate: Boolean = true) {
    val anim = animate && !reduceMotion()
    val p by animateFloatAsState(frac.coerceIn(0f, 1f), tween(if (anim) RING_MS else 0, easing = Ease), label = "bar")
    Canvas(modifier.fillMaxWidth().height(thickness * (if (glow) 3.4f else 1f))) {
        val t = thickness.toPx()
        val pad = if (glow) t * 1.4f else 0f
        val w = size.width - pad * 2
        val cy = size.height / 2
        drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(pad, cy - t / 2), Size(w, t), CornerRadius(t / 2))
        val aw = w * p
        if (aw > 0f) {
            val core = aw.coerceAtLeast(t)
            if (glow) {
                drawRoundRect(Color.White.copy(alpha = 0.07f), Offset(pad - t * 0.9f, cy - t * 1.4f), Size(core + t * 1.8f, t * 2.8f), CornerRadius(t * 1.4f))
                drawRoundRect(Color.White.copy(alpha = 0.18f), Offset(pad - t * 0.35f, cy - t * 0.85f), Size(core + t * 0.7f, t * 1.7f), CornerRadius(t * 0.85f))
            }
            drawRoundRect(Color.White, Offset(pad, cy - t / 2), Size(core, t), CornerRadius(t / 2))
        }
    }
}

/** Switch: dark track + clean white thumb with a small two-layer halo when ON; neutral grey thumb, no glow, when OFF. */
@Composable
fun GlassSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val x by animateDpAsState(if (checked) 24.dp else 3.dp, tween(FAST_MS, easing = Ease), label = "thumb")
    val track = RoundedCornerShape(16.dp)
    Box(
        modifier.size(width = 54.dp, height = 32.dp).clip(track).background(Color(0xFF0A0A0B))
            .border(1.dp, Color.White.copy(alpha = if (checked) 0.5f else 0.22f), track)
            .then(if (onChange != null) Modifier.toggleable(checked, role = Role.Switch, onValueChange = onChange) else Modifier),
    ) {
        Box(
            Modifier.offset(x, 3.dp).size(24.dp)
                .then(if (checked) Modifier.whiteHalo(12.dp, 9.dp to 0.20f, 3.dp to 0.30f) else Modifier)
                .clip(CircleShape).background(if (checked) C.White else Color(0xFF7C7C80)),
        )
    }
}

/** Soft radial glow behind a hero glyph (never over it, never full-screen). */
fun Modifier.glyphGlow(alpha: Float = 0.12f): Modifier = drawBehind {
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = alpha), Color.Transparent), center = center, radius = size.minDimension * 0.62f), size.minDimension * 0.62f, center)
}

/** Round white badge with black check (Granted / selected). */
@Composable
fun CheckBadge(size: Dp = 34.dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).background(C.White), contentAlignment = Alignment.Center) {
        Ico(com.pranav.focus.R.drawable.focus_checkmark_black, size * 0.6f, Color.Black)
    }
}

@Composable
fun GlassBack(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(44.dp).fgSurface(22.dp).clickable(role = Role.Button) { onBack() },
        contentAlignment = Alignment.Center,
    ) { Ico(com.pranav.focus.R.drawable.focus_back_chevron_white, 22.dp) }
}
