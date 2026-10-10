package com.focus.app.design.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import com.focus.app.design.motion.FocusMotion
import com.focus.app.design.motion.LocalReduceMotion

/** Button press: scale to 0.98 in 100 ms, return in 120 ms (spec section 9). */
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.98f): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduce) pressedScale else 1f,
        animationSpec = tween(if (pressed) 100 else 120, easing = FocusMotion.Standard),
        label = "pressScale",
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Clickable with no ripple (matte look), press-scale feedback and a button role. */
fun Modifier.focusClickable(
    enabled: Boolean = true,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this.pressScale(source).clickable(
        interactionSource = source,
        indication = null,
        enabled = enabled,
        role = role,
        onClick = onClick,
    )
}
