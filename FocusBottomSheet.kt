package com.focus.app.design.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.motion.FocusMotion
import com.focus.app.design.motion.LocalReduceMotion
import com.focus.app.design.shape.FocusShapes
import androidx.compose.animation.core.tween

/**
 * Bottom sheet over a 64% scrim. Solid #1C1C1E 98% fallback (live blur is optional).
 * Max height 90% of the available height; scrolls internally and respects the keyboard.
 */
@Composable
fun FocusBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduce = LocalReduceMotion.current
    BackHandler(enabled = visible, onBack = onDismiss)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val maxSheetHeight = maxHeight * 0.9f

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(if (reduce) 0 else FocusMotion.StandardMs)),
            exit = fadeOut(tween(if (reduce) 0 else FocusMotion.StandardMs)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(FocusColors.Scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(if (reduce) 0 else FocusMotion.Sheet, easing = FocusMotion.Emphasized)) { it },
            exit = slideOutVertically(tween(if (reduce) 0 else FocusMotion.Exit, easing = FocusMotion.Standard)) { it },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .clip(FocusShapes.SheetTop)
                    .background(FocusColors.SheetFallback)
                    .border(1.dp, FocusColors.Hairline, FocusShapes.SheetTop)
                    // Swallow taps so they do not reach the scrim.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
            ) {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 16.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(FocusShapes.Capsule)
                        .background(FocusColors.Muted),
                )
                content()
            }
        }
    }
}
