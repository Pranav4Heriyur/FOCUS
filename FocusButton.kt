package com.focus.app.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.shape.FocusShapes
import com.focus.app.design.typography.FocusType

enum class FocusButtonStyle { Primary, Outlined }

/** Primary: white fill, black text, 56 dp. Outlined: transparent, white 70% border, 52 dp. */
@Composable
fun FocusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: FocusButtonStyle = FocusButtonStyle.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val primary = style == FocusButtonStyle.Primary

    val fill = when {
        primary && !enabled -> Color.White.copy(alpha = 0.20f)
        primary && pressed -> Color.White.copy(alpha = 0.85f)
        primary -> Color.White
        enabled && pressed -> Color.White.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val textColor = when {
        primary && enabled -> Color.Black
        primary -> FocusColors.Dim
        enabled -> Color.White
        else -> FocusColors.Disabled
    }
    val borderColor = if (enabled) FocusColors.FocusBorder else FocusColors.Muted

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(source)
            // CTA bloom fallback: two translucent rounded shapes behind the capsule.
            .then(
                if (primary && enabled) Modifier.drawBehind {
                    val outer = 12.dp.toPx()
                    val inner = 6.dp.toPx()
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.03f),
                        topLeft = Offset(-outer, -outer),
                        size = Size(size.width + outer * 2, size.height + outer * 2),
                        cornerRadius = CornerRadius((size.height + outer * 2) / 2),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.06f),
                        topLeft = Offset(-inner, -inner),
                        size = Size(size.width + inner * 2, size.height + inner * 2),
                        cornerRadius = CornerRadius((size.height + inner * 2) / 2),
                    )
                } else Modifier,
            )
            .height(if (primary) 56.dp else 52.dp)
            .clip(FocusShapes.Capsule)
            .background(fill)
            .then(if (primary) Modifier else Modifier.border(1.dp, borderColor, FocusShapes.Capsule))
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            FocusIcon(icon, tint = textColor, size = 20.dp)
            Spacer(Modifier.width(8.dp))
        }
        FocusText(text, style = FocusType.Button, color = textColor, maxLines = 1)
    }
}

/** 36 dp capsule inside a 48 dp touch target. Used for Allow / Edit / Unlock. */
@Composable
fun FocusCapsule(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val enabled = onClick != null
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val fill = when {
        selected -> Color.White
        pressed -> FocusColors.Pressed
        else -> FocusColors.Inset
    }
    val textColor = when {
        !enabled -> FocusColors.Disabled
        selected -> Color.Black
        else -> Color.White
    }
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .pressScale(source)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = { onClick?.invoke() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .height(36.dp)
                .clip(FocusShapes.Capsule)
                .background(fill)
                .border(1.dp, FocusColors.Hairline, FocusShapes.Capsule)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            FocusText(text, style = FocusType.Button, color = textColor, maxLines = 1)
        }
    }
}

/** 48 x 48 icon button. */
@Composable
fun FocusIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val fill = when {
        selected -> Color.White
        pressed -> FocusColors.Pressed
        else -> FocusColors.Inset
    }
    val tint = when {
        !enabled -> FocusColors.Disabled
        selected -> Color.Black
        else -> Color.White
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .pressScale(source)
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .border(1.dp, FocusColors.Hairline, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        FocusIcon(icon, tint = tint, contentDescription = contentDescription)
    }
}
