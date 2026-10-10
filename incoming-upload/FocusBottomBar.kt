package com.focus.app.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.shape.FocusDimens
import com.focus.app.design.shape.FocusShapes
import com.focus.app.design.typography.FocusType

data class FocusTab(val route: String, val label: String, val icon: ImageVector)

/**
 * Floating capsule tab bar. Uses the solid #0B0B0C 96% fallback rather than live blur, which is
 * optional on Android 11-13. Place it above the navigation inset with [FocusDimens.TabBarBottomOffset].
 */
@Composable
fun FocusBottomBar(
    tabs: List<FocusTab>,
    selectedRoute: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FocusDimens.Gutter)
            .height(FocusDimens.TabBarHeight)
            .clip(FocusShapes.Capsule)
            .background(FocusColors.TabBarFallback)
            .border(1.dp, FocusColors.Hairline, FocusShapes.Capsule)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.route == selectedRoute
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(FocusShapes.Capsule)
                    .background(if (isSelected) Color.White else Color.Transparent)
                    .semantics { this.selected = isSelected }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                        onClick = { onSelect(tab.route) },
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val tint = if (isSelected) Color.Black else FocusColors.Dim
                FocusIcon(tab.icon, tint = tint, size = 24.dp)
                FocusText(tab.label, style = FocusType.TabLabel, color = tint, maxLines = 1)
            }
        }
    }
}
