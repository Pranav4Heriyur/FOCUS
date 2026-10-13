package com.pranav.focus.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pranav.focus.R
import com.pranav.focus.data.AppClock
import com.pranav.focus.data.Countdown
import com.pranav.focus.data.CountdownLogic
import com.pranav.focus.data.StudyDb
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Single source of truth for the NEET countdown. Holds the configured exam date and "today" as Compose state,
 * persisted through [StudyDb.neetDate]. Home, Study, the Study header pill and the date dialog all read and write
 * here, so a change anywhere is reflected everywhere immediately. All maths lives in [CountdownLogic].
 */
object NeetCountdown {
    var examDate by mutableStateOf<LocalDate?>(null)
        private set
    var trackStart by mutableStateOf<LocalDate?>(null)
        private set
    var today by mutableStateOf(AppClock.today())
        private set
    private var loaded = false

    val state: Countdown get() = CountdownLogic.compute(examDate, today)
    val ringPct: Int get() = CountdownLogic.ringPct(state, trackStart)

    /** Re-read the persisted date and the device-local day (call on resume). */
    fun refresh(ctx: Context) {
        val db = StudyDb.get(ctx)
        examDate = db.neetDate; trackStart = db.neetStart; today = AppClock.today(); loaded = true
    }

    fun ensureLoaded(ctx: Context) { if (!loaded) refresh(ctx) }

    /** Cheap day-rollover check (midnight, timezone or clock change). */
    fun refreshToday() { val t = AppClock.today(); if (t != today) today = t }

    /** The only writer. Keeps the original tracking start unless the new date does not come after it. */
    fun setExamDate(ctx: Context, d: LocalDate?) {
        val db = StudyDb.get(ctx)
        val now = AppClock.today()
        val keep = trackStart
        val start = when {
            d == null -> null
            keep == null || !keep.isBefore(d) -> now
            else -> keep
        }
        db.neetDate = d; db.neetStart = start
        examDate = d; trackStart = start; today = now
    }
}

private fun msUntilMidnight(): Long {
    val z = AppClock.zone()
    val now = ZonedDateTime.now(z)
    return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(z)).toMillis()
}

/** The countdown as both cards see it. Also keeps "today" fresh: date/zone/clock broadcasts plus a once-a-minute check that catches midnight. */
@Composable
fun rememberNeetCountdown(): Countdown {
    val ctx = LocalContext.current
    remember { NeetCountdown.ensureLoaded(ctx) }
    DisposableEffect(ctx) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { NeetCountdown.refreshToday() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED); addAction(Intent.ACTION_TIMEZONE_CHANGED); addAction(Intent.ACTION_TIME_CHANGED)
        }
        ContextCompat.registerReceiver(ctx, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { runCatching { ctx.unregisterReceiver(receiver) } }
    }
    LaunchedEffect(Unit) {
        while (true) {
            NeetCountdown.refreshToday()
            delay(minOf(msUntilMidnight() + 500L, 60_000L))
        }
    }
    return NeetCountdown.state
}

/** Exam-date dialog shared by every place that can change the date. */
@Composable
fun ExamDateDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(NeetCountdown.examDate?.toString() ?: "") }
    var err by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Sheet,
        title = { Text("Exam date", color = C.White) },
        text = {
            Column {
                OutlinedTextField(text, { text = it; err = false }, singleLine = true, placeholder = { Text("YYYY-MM-DD") })
                if (err) Text("Use the format YYYY-MM-DD.", color = C.Text2, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton({
                val v = runCatching { LocalDate.parse(text.trim()) }.getOrNull()
                if (v == null) err = true else { NeetCountdown.setExamDate(ctx, v); onDismiss() }
            }) { Text("Save", color = C.White) }
        },
        dismissButton = {
            if (NeetCountdown.examDate != null) TextButton({ NeetCountdown.setExamDate(ctx, null); onDismiss() }) { Text("Clear", color = C.Text2) }
        },
    )
}

/** Home: compact card, shown directly after the Strava hero. Tap opens Study (or the date dialog when unset); long-press edits the date. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeetCountdownCompact(onOpenStudy: () -> Unit) {
    val c = rememberNeetCountdown()
    var edit by remember { mutableStateOf(false) }
    if (edit) ExamDateDialog { edit = false }
    Row(
        Modifier.fillMaxWidth().glass(24.dp)
            .combinedClickable(onClick = { if (c is Countdown.Unset) edit = true else onOpenStudy() }, onLongClick = { edit = true })
            .heightIn(min = 80.dp).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccRing(NeetCountdown.ringPct, 52.dp, 5.dp, label = false)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(CountdownLogic.title(c), color = C.Text2, fontSize = 12.sp, lineHeight = 15.sp)
            Text(CountdownLogic.headline(c), color = C.White, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(CountdownLogic.dateLine(c), color = C.Text2, fontSize = 12.sp, lineHeight = 15.sp)
        }
        Ico(R.drawable.p1_chevron_right, 16.dp, C.Text2)
    }
}

/** Study: the larger countdown card, shown above the existing Study content. Tap edits the date. */
@Composable
fun NeetCountdownCard(onEdit: () -> Unit) {
    val c = rememberNeetCountdown()
    SectionCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${CountdownLogic.title(c)} Countdown", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Ico(R.drawable.p1_chevron_right, 16.dp, C.Text2)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(136.dp), contentAlignment = Alignment.Center) {
                AccRing(NeetCountdown.ringPct, 136.dp, 9.dp, label = false)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (c is Countdown.Unset) "—" else "${CountdownLogic.daysLeft(c)}", color = C.White, fontSize = 34.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when (c) { Countdown.Unset -> "Not set"; is Countdown.Upcoming -> "Days Left"; is Countdown.ExamDay -> "Exam day"; is Countdown.Passed -> "Passed" },
                        color = C.Text2, fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Caps("Exam date")
                Text(
                    if (c is Countdown.Unset) CountdownLogic.headline(c) else CountdownLogic.dateLine(c),
                    color = C.White, fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium,
                )
                if (c is Countdown.Passed) Text(CountdownLogic.headline(c), color = C.Text2, fontSize = 13.sp)
                if (c is Countdown.Unset) Text(CountdownLogic.dateLine(c), color = C.Text2, fontSize = 13.sp)
            }
        }
    }
}
