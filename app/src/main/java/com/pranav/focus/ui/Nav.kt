package com.pranav.focus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.R

private data class NavItem(val screen: Screen, val label: String, val icon: Int)

private val NAV_ITEMS = listOf(
    NavItem(Screen.Home, "Home", R.drawable.ic_home),
    NavItem(Screen.Focus, "Focus", R.drawable.ic_adjust),
    NavItem(Screen.Study, "Study", R.drawable.ic_menu_book),
    NavItem(Screen.Workout, "Workout", R.drawable.ic_fitness_center),
    NavItem(Screen.Stats, "Stats", R.drawable.ic_bar_chart),
    NavItem(Screen.TimeBank, "Time Bank", R.drawable.ic_hourglass_top),
    NavItem(Screen.Profile, "Profile", R.drawable.ic_person),
)

/**
 * Shared bottom navigation: seven destinations in a horizontally scrollable pill.
 * Swiping the bar only scrolls it; tapping a tab navigates. The selected tab is
 * scrolled into view when the route changes (only if it is not already fully visible).
 */
@Composable
fun TabBar(current: Screen, onSelect: (Screen) -> Unit) {
    val state = rememberLazyListState()
    val idx = NAV_ITEMS.indexOfFirst { it.screen == current }

    LaunchedEffect(current) {
        if (idx < 0) return@LaunchedEffect
        val info = state.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == idx }
        val fullyVisible = item != null &&
            item.offset >= info.viewportStartOffset &&
            item.offset + item.size <= info.viewportEndOffset
        if (!fullyVisible) {
            val viewport = info.viewportEndOffset - info.viewportStartOffset
            val itemSize = item?.size ?: (viewport / 4)
            state.animateScrollToItem(idx, -((viewport - itemSize) / 2).coerceAtLeast(0))
        }
    }

    LazyRow(
        state = state,
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp, top = 4.dp)
            .fillMaxWidth().height(64.dp)
            .clip(CircleShape).background(C.Card.copy(alpha = 0.94f)).border(1.dp, C.Hairline, CircleShape),
        contentPadding = PaddingValues(5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        itemsIndexed(NAV_ITEMS, key = { _, it -> it.screen.name }) { _, item ->
            val sel = item.screen == current
            Column(
                Modifier.width(78.dp).height(54.dp).clip(CircleShape)
                    .then(if (sel) Modifier.background(C.White) else Modifier)
                    .clickable { onSelect(item.screen) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Ico(item.icon, 24.dp, if (sel) C.Black else C.Text2)
                Text(
                    item.label, color = if (sel) C.Black else C.Text2, fontSize = 10.sp, lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium, maxLines = 1,
                )
            }
        }
    }
}
