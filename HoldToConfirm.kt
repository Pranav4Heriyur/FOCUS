package com.focus.app.design.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.typography.FocusType
import kotlinx.coroutines.launch
import kotlin.math.ceil

/**
 * 72 dp progress ring inside an 80 dp touch target. Progress fills linearly while the finger is
 * down and resets if released early. Calls [onCompleted] once when the hold finishes.
 */
@Composable
fun HoldToConfirm(
    durationMs: Long,
    completed: Boolean,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(if (completed) 1f else 0f) }
    val scope = rememberCoroutineScope()
    val totalSeconds = (durationMs / 1000f)

    Box(
        modifier = modifier
            .size(80.dp)
            .semantics { contentDescription = "Hold to confirm leaving the session" }
            .pointerInput(completed) {
                detectTapGestures(
                    onPress = {
                        if (!completed) {
                            val job = scope.launch {
                                val remaining = (durationMs * (1f - progress.value)).toLong().coerceAtLeast(1L)
                                progress.animateTo(1f, tween(remaining.toInt(), easing = LinearEasing))
                                onCompleted()
                            }
                            tryAwaitRelease()
                            if (job.isActive) {
                                job.cancel()
                                scope.launch { progress.animateTo(0f, tween(200)) }
                            }
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        FocusRing(progress = progress.value, diameter = 72.dp, strokeWidth = 5.dp) {
            if (completed) {
                FocusIcon(Icons.Rounded.Check, size = 28.dp)
            } else {
                val left = ceil(totalSeconds * (1f - progress.value)).toInt()
                FocusText(
                    text = if (progress.value == 0f) "Hold" else left.toString(),
                    style = FocusType.BodyEmphasis,
                    color = FocusColors.High,
                )
            }
        }
    }
}
