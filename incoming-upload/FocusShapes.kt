package com.focus.app.design.shape

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object FocusShapes {
    val Card = RoundedCornerShape(24.dp)
    val Hero = RoundedCornerShape(28.dp)
    val Compact = RoundedCornerShape(20.dp)   // (est.)
    val Field = RoundedCornerShape(16.dp)
    val AppTile = RoundedCornerShape(14.dp)   // (est.)
    val SmallTile = RoundedCornerShape(12.dp)
    val Capsule = RoundedCornerShape(percent = 50)
    val SheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}

object FocusDimens {
    val Gutter = 16.dp
    val CardGap = 12.dp
    val SectionGap = 24.dp
    val CardPadding = 16.dp
    val ButtonHeight = 56.dp
    val MinTouch = 48.dp
    val TabBarHeight = 64.dp        // (est.)
    val TabBarBottomOffset = 8.dp   // (est.)
}
