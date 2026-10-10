package com.focus.app.design.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Foundation-only icon (the app does not depend on Material components). */
@Composable
fun FocusIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    contentDescription: String? = null,
) {
    Image(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
