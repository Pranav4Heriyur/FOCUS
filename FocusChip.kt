package com.focus.app.design.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.motion.FocusMotion
import com.focus.app.design.motion.focusTween
import com.focus.app.design.shape.FocusShapes
import com.focus.app.design.typography.FocusType

/** Filter / subject chip: 40 dp high, selected = white fill + black text. */
@Composable
fun FocusChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val fill = when {
        selected -> Color.White
        pressed -> FocusColors.Pressed
        else -> FocusColors.Inset
    }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressScale(source)
            .semantics { this.selected = selected }
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .height(40.dp)
                .clip(FocusShapes.Capsule)
                .background(fill)
                .border(1.dp, FocusColors.Hairline, FocusShapes.Capsule)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            FocusText(
                text,
                style = FocusType.BodyEmphasis,
                color = if (selected) Color.Black else FocusColors.Secondary,
                maxLines = 1,
            )
        }
    }
}

/** Capsule segmented control: #1C1C1E shell, selected segment white with black text. */
@Composable
fun FocusSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(FocusShapes.Capsule)
            .background(FocusColors.Inset)
            .border(1.dp, FocusColors.Hairline, FocusShapes.Capsule)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            val fill by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color.Transparent,
                animationSpec = focusTween(FocusMotion.StandardMs),
                label = "segmentFill",
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(FocusShapes.Capsule)
                    .background(fill)
                    .semantics { this.selected = isSelected }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                FocusText(
                    label,
                    style = FocusType.BodyEmphasis.copy(fontSize = 12.sp),
                    color = if (isSelected) Color.Black else FocusColors.High,
                    maxLines = 1,
                )
            }
        }
    }
}
