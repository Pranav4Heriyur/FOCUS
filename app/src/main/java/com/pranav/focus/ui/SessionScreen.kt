package com.pranav.focus.ui

import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.pranav.focus.data.ExitRules
import com.pranav.focus.data.Store
import com.pranav.focus.lock.Allowed
import com.pranav.focus.lock.SessionController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun formatClock(ms: Long): String {
    val total = (ms + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
fun SessionScreen() {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val totalMs = remember { (store.sessionMinutes * 60_000L).coerceAtLeast(1) }
    var remaining by remember { mutableLongStateOf(store.remainingMs()) }
    var exiting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            remaining = store.remainingMs()
            if (remaining <= 0) {
                SessionController.complete(ctx)
                break
            }
            delay(250)
        }
    }

    val progress = (remaining.toFloat() / totalMs).coerceIn(0f, 1f)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Caps("Locked in")
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(290.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 10.dp.toPx()
                val topLeft = Offset(stroke, stroke)
                val arcSize = Size(size.width - stroke * 2, size.height - stroke * 2)
                drawArc(C.Track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                // cheap bloom: a wider, faint arc behind the bright one
                drawArc(C.White.copy(alpha = 0.12f), -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
                drawArc(C.White, -90f, 360f * progress, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text(formatClock(remaining), color = C.White, fontSize = 56.sp, lineHeight = 64.sp, fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.height(16.dp))
        Caps("${store.sessionMinutes} min session")

        Spacer(Modifier.height(32.dp))
        Caps("Allowed")
        Spacer(Modifier.height(12.dp))
        AllowedTiles()

        Spacer(Modifier.weight(1f))
        if (exiting) {
            ExitFlow(onCancel = { exiting = false })
        } else {
            GhostButton("End early") { exiting = true }
        }
    }
}

@Composable
private fun AllowedTiles() {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val pm = ctx.packageManager
    val dialer = remember { Allowed.dialerPackage(ctx) }
    val pkgs = remember {
        buildList {
            if (dialer != null) add(dialer)
            store.allowedApps.filter { Allowed.isInstalled(ctx, it) && it != dialer }.forEach { add(it) }
        }
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        pkgs.forEach { pkg ->
            val label = remember(pkg) { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg) }
            val icon = remember(pkg) { runCatching { pm.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull() }
            Column(
                Modifier.clickable {
                    val launch = if (pkg == dialer) Intent(Intent.ACTION_DIAL) else pm.getLaunchIntentForPackage(pkg)
                    launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { ctx.startActivity(it) }
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon != null) {
                    Image(icon, label, Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)))
                } else {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(C.Sheet), contentAlignment = Alignment.Center) {
                        Text(label.take(1), color = C.White)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(if (pkg == dialer) "Phone" else label, color = C.Text2, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 2.dp))
            }
        }
    }
}

@Composable
private fun ExitFlow(onCancel: () -> Unit) {
    val ctx = LocalContext.current
    var holdDone by remember { mutableStateOf(false) }
    var phrase by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!holdDone) {
            Text(
                "Quitting costs XP and resets your streak. Hold for ${ExitRules.HOLD_SECONDS} seconds to continue.",
                color = C.Text2, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            HoldButton(ExitRules.HOLD_SECONDS) { holdDone = true }
        } else {
            Text("Type exactly: ${ExitRules.PHRASE}", color = C.Text2, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                value = phrase, onValueChange = { phrase = it }, singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default,
                colors = fieldColors(),
            )
            PrimaryButton("Quit session", enabled = ExitRules.phraseOk(phrase)) { SessionController.quit(ctx) }
        }
        GhostButton("Keep going") { onCancel() }
    }
}

@Composable
fun HoldButton(seconds: Int, onDone: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxWidth().height(56.dp)
            .clip(CircleShape)
            .background(C.Sheet)
            .border(1.dp, C.Hairline, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val start = SystemClock.elapsedRealtime()
                    val job = scope.launch {
                        while (true) {
                            val p = (SystemClock.elapsedRealtime() - start) / (seconds * 1000f)
                            progress = p.coerceAtMost(1f)
                            if (p >= 1f) {
                                onDone()
                                break
                            }
                            delay(50)
                        }
                    }
                    tryAwaitRelease()
                    job.cancel()
                    progress = 0f
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(progress).background(C.White.copy(alpha = 0.25f)))
        val left = kotlin.math.ceil((1f - progress) * seconds).toInt()
        Text(if (progress == 0f) "Press and hold" else "Keep holding… ${left}s", color = C.White, fontSize = 15.sp)
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = C.White, unfocusedTextColor = C.White,
    focusedBorderColor = C.White, unfocusedBorderColor = C.Track,
    cursorColor = C.White,
    focusedLabelColor = C.Text2, unfocusedLabelColor = C.Text3,
)
