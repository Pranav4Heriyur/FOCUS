package com.focus.app.feature.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focus.app.data.ProgressMath
import com.focus.app.data.model.FocusSession
import com.focus.app.design.color.FocusColors
import com.focus.app.design.component.FocusBottomSheet
import com.focus.app.design.component.FocusButton
import com.focus.app.design.component.FocusButtonStyle
import com.focus.app.design.component.FocusIcon
import com.focus.app.design.component.FocusRing
import com.focus.app.design.component.FocusText
import com.focus.app.design.component.FocusTextField
import com.focus.app.design.component.HoldToConfirm
import com.focus.app.design.motion.FocusMotion
import com.focus.app.design.shape.FocusDimens
import com.focus.app.design.typography.FocusType
import com.focus.app.util.formatClock
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

const val EXIT_PHRASE = "I choose distraction over my goals"

/**
 * Active focus session. The visual composition is not defined in the spec, so this is a
 * minimal, token-based proposal: remaining time, session name/subject, leave-early action.
 * Time is recomputed from the persisted end time on every tick.
 */
@Composable
fun ActiveFocusScreen(
    session: FocusSession,
    streakDays: Int,
    onComplete: () -> Unit,
    onExit: () -> Unit,
) {
    val now by produceState(initialValue = System.currentTimeMillis(), session.endMs) {
        while (true) {
            value = System.currentTimeMillis()
            delay(250)
        }
    }
    val remaining = session.remainingMs(now)
    val progress = session.progress(now)
    var showLeave by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(remaining <= 0L) {
        if (remaining <= 0L) onComplete()
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = FocusDimens.Gutter)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(32.dp))
            FocusText(session.subject.uppercase(), style = FocusType.TrackedCaps, color = FocusColors.Secondary)
            Spacer(Modifier.height(4.dp))
            FocusText(session.name, style = FocusType.SectionTitle, textAlign = TextAlign.Center, maxLines = 2)

            Spacer(Modifier.weight(1f))
            FocusRing(progress = progress, diameter = 280.dp, strokeWidth = 8.dp, glow = true) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FocusText(formatClock(remaining), style = FocusType.LargeHero)
                    FocusText("REMAINING", style = FocusType.TrackedCaps, color = FocusColors.Secondary)
                }
            }
            Spacer(Modifier.height(24.dp))
            FocusText(
                "Ends at " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(session.endMs)),
                style = FocusType.Body,
                color = FocusColors.Secondary,
            )
            Spacer(Modifier.weight(1f))

            FocusButton(
                text = "Leave early",
                onClick = { showLeave = true },
                style = FocusButtonStyle.Outlined,
            )
        }

        LeaveEarlySheet(
            visible = showLeave,
            percentDone = (progress * 100).toInt(),
            streakDays = streakDays,
            onStay = { showLeave = false },
            onExit = onExit,
        )
    }
}

/** A4: Leave Early bottom sheet. Exit stays disabled until the hold and the phrase are both done. */
@Composable
private fun LeaveEarlySheet(
    visible: Boolean,
    percentDone: Int,
    streakDays: Int,
    onStay: () -> Unit,
    onExit: () -> Unit,
) {
    var held by rememberSaveable { mutableStateOf(false) }
    var typed by rememberSaveable { mutableStateOf("") }
    val phraseOk = typed.trim().equals(EXIT_PHRASE, ignoreCase = true)

    // Reset the friction each time the sheet is dismissed.
    LaunchedEffect(visible) {
        if (!visible) {
            held = false
            typed = ""
        }
    }

    val penalty = buildString {
        append("Leaving costs ${ProgressMath.EARLY_EXIT_XP_PENALTY} XP")
        if (streakDays > 0) append(" and resets your $streakDays-day streak")
    }

    FocusBottomSheet(visible = visible, onDismiss = onStay) {
        FocusText("You’re at $percentDone%.", style = FocusType.ScreenTitle)
        Spacer(Modifier.height(4.dp))
        FocusText("You can do more than you think.", style = FocusType.Body, color = FocusColors.Secondary)

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FocusIcon(Icons.Rounded.Warning, tint = FocusColors.High, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            FocusText(penalty, style = FocusType.BodyEmphasis, color = FocusColors.High)
        }

        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            HoldToConfirm(
                durationMs = FocusMotion.HoldToConfirmMs,
                completed = held,
                onCompleted = { held = true },
            )
            Spacer(Modifier.height(4.dp))
            FocusText(
                if (held) "Hold complete" else "Hold for ${FocusMotion.HoldToConfirmMs / 1000} seconds",
                style = FocusType.Caption,
                color = FocusColors.Secondary,
            )
        }

        Spacer(Modifier.height(16.dp))
        FocusTextField(value = typed, onValueChange = { typed = it }, hint = "Type: $EXIT_PHRASE")

        Spacer(Modifier.height(20.dp))
        FocusButton(
            text = "Exit session",
            onClick = onExit,
            style = FocusButtonStyle.Outlined,
            enabled = held && phraseOk,
        )
        Spacer(Modifier.height(12.dp))
        FocusButton(text = "Stay focused", onClick = onStay)
    }
}
