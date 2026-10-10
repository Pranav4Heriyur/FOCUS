package com.focus.app.feature.placeholder

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.component.FocusCard
import com.focus.app.design.component.FocusText
import com.focus.app.design.component.ScreenColumn
import com.focus.app.design.typography.FocusType

@Composable
fun PlaceholderScreen(title: String, note: String) {
    ScreenColumn(withTabBar = true) {
        Spacer(Modifier.height(16.dp))
        FocusText(title, style = FocusType.ScreenTitle)
        Spacer(Modifier.height(24.dp))
        FocusCard {
            FocusText("COMING NEXT", style = FocusType.TrackedCaps, color = FocusColors.Secondary)
            Spacer(Modifier.height(4.dp))
            FocusText(note, style = FocusType.Body, color = FocusColors.High)
        }
    }
}
