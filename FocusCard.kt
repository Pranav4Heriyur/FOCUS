package com.focus.app.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.shape.FocusShapes

/** Standard card: #0B0B0C, 24 dp radius, 16 dp padding, white 10% hairline. */
@Composable
fun FocusCard(
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = FocusShapes.Card,
    color: Color = FocusColors.Card,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.focusClickable(onClick = onClick) else Modifier)
            .clip(shape)
            .background(color)
            .border(1.dp, FocusColors.Hairline, shape)
            .padding(contentPadding),
        content = content,
    )
}
