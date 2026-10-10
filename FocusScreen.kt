package com.focus.app.design.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focus.app.design.shape.FocusDimens

/**
 * Standard scrolling screen: 16 dp gutters, system insets applied once per edge, and clearance for
 * the floating tab bar when [withTabBar] is true. Content scrolls instead of being forced into 914 dp.
 */
@Composable
fun ScreenColumn(
    modifier: Modifier = Modifier,
    withTabBar: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FocusDimens.Gutter),
    ) {
        content()
        val clearance = if (withTabBar) {
            FocusDimens.TabBarHeight + FocusDimens.TabBarBottomOffset + 16.dp
        } else {
            16.dp
        }
        Spacer(Modifier.navigationBarsPadding().height(clearance))
    }
}
