package com.focus.app.design.typography

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.focus.app.R
import com.focus.app.design.color.FocusColors

@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    resId = R.font.inter_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Inter is the production font (SF Pro is only the visual reference). */
val InterFamily = FontFamily(inter(300), inter(400), inter(500), inter(600))

/** Numeric values are (est.) in the spec. Tabular numerals are on for timers/counters. */
object FocusType {
    private fun style(weight: Int, size: Int, line: Int, tracking: Float, tabular: Boolean = false) =
        TextStyle(
            fontFamily = InterFamily,
            fontWeight = FontWeight(weight),
            fontSize = size.sp,
            lineHeight = line.sp,
            letterSpacing = tracking.sp,
            fontFeatureSettings = if (tabular) "tnum" else null,
            color = FocusColors.Primary,
        )

    val HeroNumeral = style(300, 64, 72, -1.8f, tabular = true)
    val LargeHero = style(300, 56, 64, -1.5f, tabular = true)
    val MetricNumeral = style(300, 36, 42, -0.8f, tabular = true)
    val ScreenTitle = style(500, 30, 36, -0.6f)
    val SectionTitle = style(500, 20, 26, -0.2f)
    val CardTitle = style(500, 17, 22, 0f)
    val Body = style(400, 14, 20, 0f)
    val BodyEmphasis = style(500, 14, 20, 0f)
    val Caption = style(400, 12, 16, 0.1f)
    val TrackedCaps = style(600, 10, 14, 1.4f)
    val Button = style(600, 15, 20, 0f)
    val TabLabel = style(500, 10, 12, 0.1f)
    val TextField = style(400, 16, 24, 0f)
    val FieldHint = style(400, 14, 20, 0f)
    val ChartLabel = style(500, 10, 14, 0.2f)
}
