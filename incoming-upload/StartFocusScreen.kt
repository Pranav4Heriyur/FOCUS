package com.focus.app.feature.focus

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focus.app.design.color.FocusColors
import com.focus.app.design.component.FocusButton
import com.focus.app.design.component.FocusCapsule
import com.focus.app.design.component.FocusCard
import com.focus.app.design.component.FocusChip
import com.focus.app.design.component.FocusIconButton
import com.focus.app.design.component.FocusText
import com.focus.app.design.component.FocusTextField
import com.focus.app.design.component.FocusToggle
import com.focus.app.design.component.ScreenColumn
import com.focus.app.design.shape.FocusDimens
import com.focus.app.design.typography.FocusType

private val subjects = listOf("Physics", "Chemistry", "Biology", "Workout", "Other")
private val presets = listOf(25, 45, 90)
private const val MIN_MINUTES = 5
private const val MAX_MINUTES = 180
private const val STEP_MINUTES = 5

/** A2: Start Focus Session. */
@Composable
fun StartFocusScreen(onBegin: (name: String, subject: String, minutes: Int, strict: Boolean) -> Unit) {
    var minutes by rememberSaveable { mutableStateOf(45) }
    var name by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("Physics") }
    var strict by rememberSaveable { mutableStateOf(false) }

    ScreenColumn(withTabBar = true) {
        Spacer(Modifier.height(16.dp))
        FocusText("Start Focus", style = FocusType.ScreenTitle)
        Spacer(Modifier.height(FocusDimens.SectionGap))

        // Duration: large light numeral with 48 dp previous / next targets.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusIconButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Decrease duration",
                onClick = { minutes = (minutes - STEP_MINUTES).coerceAtLeast(MIN_MINUTES) },
                enabled = minutes > MIN_MINUTES,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FocusText(minutes.toString(), style = FocusType.LargeHero, textAlign = TextAlign.Center)
                FocusText("MINUTES", style = FocusType.TrackedCaps, color = FocusColors.Secondary)
            }
            FocusIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Increase duration",
                onClick = { minutes = (minutes + STEP_MINUTES).coerceAtMost(MAX_MINUTES) },
                enabled = minutes < MAX_MINUTES,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            presets.forEach { preset ->
                FocusChip(preset.toString(), selected = minutes == preset, onClick = { minutes = preset })
            }
            FocusChip(
                "Custom",
                selected = minutes !in presets,
                onClick = { if (minutes in presets) minutes = 60 },
            )
        }

        Spacer(Modifier.height(FocusDimens.SectionGap))
        FocusTextField(value = name, onValueChange = { name = it }, hint = "Session name")

        Spacer(Modifier.height(FocusDimens.CardGap))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            subjects.forEach { s ->
                FocusChip(s, selected = subject == s, onClick = { subject = s })
            }
        }

        Spacer(Modifier.height(FocusDimens.SectionGap))
        // The Edit screens (A9 Allowed Apps, A8 Allowed Callers) are not built yet, so Edit is disabled.
        SettingRow(title = "Allowed apps", detail = "Phone, Claude, Gemini", actionLabel = "Edit", onAction = null)
        Spacer(Modifier.height(FocusDimens.CardGap))
        SettingRow(title = "Allowed callers", detail = "4 contacts", actionLabel = "Edit", onAction = null)
        Spacer(Modifier.height(FocusDimens.CardGap))
        FocusCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    FocusText("Strict lock", style = FocusType.CardTitle)
                    FocusText("Harder to leave early", style = FocusType.Body, color = FocusColors.Secondary)
                }
                FocusToggle(checked = strict, onCheckedChange = { strict = it })
            }
        }

        Spacer(Modifier.height(FocusDimens.SectionGap))
        FocusButton(text = "Begin Focus", onClick = { onBegin(name.trim(), subject, minutes, strict) })
    }
}

@Composable
private fun SettingRow(title: String, detail: String, actionLabel: String, onAction: (() -> Unit)?) {
    FocusCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                FocusText(title, style = FocusType.CardTitle)
                FocusText(detail, style = FocusType.Body, color = FocusColors.Secondary)
            }
            Spacer(Modifier.width(12.dp))
            FocusCapsule(text = actionLabel, onClick = onAction)
        }
    }
}
