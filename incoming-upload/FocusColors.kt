package com.focus.app.design.color

import androidx.compose.ui.graphics.Color

/** Monochrome only: black, white and neutral greys. No accent colours (spec section 1). */
object FocusColors {
    // Surfaces
    val Background = Color(0xFF000000)
    val Card = Color(0xFF0B0B0C)
    val Raised = Color(0xFF141416)
    val Inset = Color(0xFF1C1C1E)
    val Track = Color(0xFF2A2A2D)
    val Pressed = Color(0xFF242427)

    // Text and icons
    val Primary = Color(0xFFFFFFFF)
    val Secondary = Color(0xFFA1A1A6)
    val Tertiary = Color(0xFF636366)
    val High = Color.White.copy(alpha = 0.70f)
    val Dim = Color.White.copy(alpha = 0.40f)
    val Disabled = Color.White.copy(alpha = 0.30f)
    val Muted = Color.White.copy(alpha = 0.20f)
    val Black = Color(0xFF000000)

    // Borders
    val Hairline = Color.White.copy(alpha = 0.10f)
    val Specular = Color.White.copy(alpha = 0.08f)
    val FocusBorder = Color.White.copy(alpha = 0.70f)

    // Glow
    val GlowCore = Color.White.copy(alpha = 0.25f)
    val GlowSubtle = Color.White.copy(alpha = 0.12f)

    // Overlays (non-blur fallbacks, so the UI never depends on live blur)
    val Scrim = Color.Black.copy(alpha = 0.64f)
    val TabBarFallback = Card.copy(alpha = 0.96f)
    val SheetFallback = Inset.copy(alpha = 0.98f)
}
