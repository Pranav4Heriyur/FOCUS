package com.focus.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import com.focus.app.design.component.FocusTab

val focusTabs = listOf(
    FocusTab(FocusRoute.Home, "Home", Icons.Rounded.Home),
    FocusTab(FocusRoute.Focus, "Focus", Icons.Rounded.Timer),
    FocusTab(FocusRoute.Study, "Study", Icons.Rounded.MenuBook),
    FocusTab(FocusRoute.Stats, "Stats", Icons.Rounded.BarChart),
    FocusTab(FocusRoute.Profile, "Profile", Icons.Rounded.Person),
)
