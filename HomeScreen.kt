package com.focus.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.focus.app.data.model.ProgressStats
import com.focus.app.design.color.FocusColors
import com.focus.app.design.component.FocusCard
import com.focus.app.design.component.FocusText
import com.focus.app.design.component.ScreenColumn
import com.focus.app.design.shape.FocusDimens
import com.focus.app.design.shape.FocusShapes
import com.focus.app.design.typography.FocusType

/**
 * Temporary Home. The real Home 1A/1B (Strava hero, balance radar, agenda...) comes later.
 * It already shows real stored values so the Focus loop can be verified end to end.
 */
@Composable
fun HomeScreen(stats: ProgressStats, onRefresh: () -> Unit) {
    LaunchedEffect(Unit) { onRefresh() }

    ScreenColumn(withTabBar = true) {
        Spacer(Modifier.height(16.dp))
        FocusText("Home", style = FocusType.ScreenTitle)
        Spacer(Modifier.height(FocusDimens.SectionGap))

        FocusCard(shape = FocusShapes.Hero) {
            FocusText("TIME BANK", style = FocusType.TrackedCaps, color = FocusColors.Secondary)
            FocusText("${stats.earnedTodayMin} min", style = FocusType.HeroNumeral)
            FocusText("earned today", style = FocusType.Body, color = FocusColors.Secondary)
        }

        Spacer(Modifier.height(FocusDimens.CardGap))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FocusDimens.CardGap)) {
            FocusCard(modifier = Modifier.weight(1f), shape = FocusShapes.Compact) {
                FocusText("STREAK", style = FocusType.TrackedCaps, color = FocusColors.Secondary)
                FocusText("${stats.streakDays} days", style = FocusType.MetricNumeral, maxLines = 1)
            }
            FocusCard(modifier = Modifier.weight(1f), shape = FocusShapes.Compact) {
                FocusText("XP", style = FocusType.TrackedCaps, color = FocusColors.Secondary)
                FocusText("${stats.xp}", style = FocusType.MetricNumeral, maxLines = 1)
            }
        }

        Spacer(Modifier.height(FocusDimens.SectionGap))
        FocusText(
            "Full Home arrives after the Focus lock is verified on the device.",
            style = FocusType.Body,
            color = FocusColors.Tertiary,
        )
    }
}
