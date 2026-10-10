package com.focus.app.design.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.shape.FocusShapes
import com.focus.app.design.motion.focusTween

/** 52 x 32 dp track inside a 48 dp-high touch target. On = white track + black thumb. */
@Composable
fun FocusToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val thumbX by animateDpAsState(if (checked) 22.dp else 2.dp, focusTween(180), label = "thumbX")
    val track by animateColorAsState(if (checked) Color.White else FocusColors.Track, focusTween(180), label = "track")
    val thumb by animateColorAsState(if (checked) Color.Black else Color.White, focusTween(180), label = "thumb")

    Box(
        modifier = modifier
            .size(width = 52.dp, height = 48.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(FocusShapes.Capsule)
                .background(track),
        ) {
            Box(
                Modifier
                    .offset(x = thumbX, y = 2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(thumb),
            )
        }
    }
}
