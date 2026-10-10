package com.focus.app.design.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** True when the system animator scale is 0 (reduced motion). */
val LocalReduceMotion = compositionLocalOf { false }

object FocusMotion {
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Standard = FastOutSlowInEasing

    const val Fast = 120
    const val StandardMs = 220
    const val Enter = 350
    const val Exit = 250
    const val Sheet = 300
    const val RingFill = 600
    const val Celebration = 650

    /** Hold-to-leave duration. The spec flags 60 s as long; usability-test it. */
    const val HoldToConfirmMs = 60_000L
}

/** A tween that collapses to 0 ms when the user asked for reduced motion. */
@Composable
fun <T> focusTween(
    durationMs: Int,
    easing: androidx.compose.animation.core.Easing = FocusMotion.Standard,
): TweenSpec<T> = tween(if (LocalReduceMotion.current) 0 else durationMs, easing = easing)
