package com.pranav.focus.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.pranav.focus.data.Caller
import com.pranav.focus.data.Store
import com.pranav.focus.lock.Allowed
import com.pranav.focus.lock.AppState
import com.pranav.focus.lock.SessionController

enum class Screen { Home, Apps, Callers, Setup }

@Composable
fun HomeScreen(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var minutes by remember { mutableIntStateOf(store.lastMinutes) }
    val bank = remember(AppState.active, AppState.lastEarned) { store.bankMinutes }
    val streak = remember(AppState.active) { store.streak }
    val xp = remember(AppState.active) { store.xp }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Caps("Focus")
        ScreenHeader("Lock in.", null)

        FocusCard(Modifier.fillMaxWidth()) {
            Caps("Session length")
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Stepper("−") { minutes = (minutes - 5).coerceAtLeast(5) }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$minutes", color = C.White, fontSize = 64.sp, lineHeight = 72.sp, fontWeight = FontWeight.Light)
                    Text(" min", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(bottom = 14.dp))
                }
                Stepper("+") { minutes = (minutes + 5).coerceAtMost(240) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 45, 60, 90).forEach { m ->
                    val sel = m == minutes
                    Box(
                        Modifier.clip(CircleShape).background(if (sel) C.White else C.Sheet)
                            .clickable { minutes = m }.padding(horizontal = 14.dp, vertical = 8.dp),
                    ) { Text("$m", color = if (sel) C.Black else C.White, fontSize = 14.sp) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (!AppState.coreReady) {
            FocusCard(Modifier.fillMaxWidth(), onClick = { go(Screen.Setup) }) {
                Text("Finish setup to enable the lock", color = C.White, fontSize = 17.sp)
                Text("Accessibility and overlay permissions are required. Tap to set up.", color = C.Text2, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        PrimaryButton("Start focus session", enabled = AppState.coreReady) { SessionController.start(ctx, minutes) }
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Time bank", "$bank", "min today", Modifier.weight(1f))
            Stat("Streak", "$streak", "days", Modifier.weight(1f))
            Stat("XP", "$xp", "", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        NavRow("Allowed apps", "${store.allowedApps.size} chosen + Phone") { go(Screen.Apps) }
        Spacer(Modifier.height(12.dp))
        NavRow("Allowed callers", "${store.callers.size} numbers") { go(Screen.Callers) }
        Spacer(Modifier.height(12.dp))
        NavRow("Setup and permissions", if (AppState.coreReady) "Lock ready" else "Action needed") { go(Screen.Setup) }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Stepper(label: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(C.Sheet).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(label, color = C.White, fontSize = 22.sp)
    }
}

@Composable
private fun Stat(title: String, value: String, unit: String, modifier: Modifier) {
    FocusCard(modifier) {
        Caps(title)
        Spacer(Modifier.height(6.dp))
        Text(value, color = C.White, fontSize = 28.sp, fontWeight = FontWeight.Light)
        Text(unit, color = C.Text3, fontSize = 12.sp)
    }
}

@Composable
private fun NavRow(title: String, sub: String, onClick: () -> Unit) {
    FocusCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(title, color = C.White, fontSize = 17.sp)
                Text(sub, color = C.Text2, fontSize = 14.sp)
            }
            Text("›", color = C.Text3, fontSize = 22.sp)
        }
    }
}

@Composable
fun AppsScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val apps = remember { Allowed.launchableApps(ctx) }
    var selected by remember { mutableStateOf(store.allowedApps) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenHeader("Allowed apps", back)
        Text("Phone is always allowed. Everything else is locked during a session unless ticked here.", color = C.Text2, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(apps, key = { it.pkg }) { app ->
                val icon = remember(app.pkg) { runCatching { ctx.packageManager.getApplicationIcon(app.pkg).toBitmap(80, 80).asImageBitmap() }.getOrNull() }
                val on = app.pkg in selected
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable {
                        selected = if (on) selected - app.pkg else selected + app.pkg
                        store.allowedApps = selected
                    }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (icon != null) Image(icon, null, Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)))
                    else Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(C.Sheet))
                    Spacer(Modifier.width(12.dp))
                    Text(app.label, color = C.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = on, onCheckedChange = null,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = C.Black, checkedTrackColor = C.White,
                            uncheckedThumbColor = C.Text2, uncheckedTrackColor = C.Track, uncheckedBorderColor = C.Track,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun CallersScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var list by remember { mutableStateOf(store.callers) }
    var only by remember { mutableStateOf(store.callersOnly) }
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        ScreenHeader("Allowed callers", back)
        FocusCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Only these numbers can ring", color = C.White, fontSize = 17.sp)
                    Text("Applies during a focus session. Needs the call-screening role (Setup).", color = C.Text2, fontSize = 14.sp)
                }
                Switch(
                    checked = only, onCheckedChange = { only = it; store.callersOnly = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = C.Black, checkedTrackColor = C.White, uncheckedThumbColor = C.Text2, uncheckedTrackColor = C.Track, uncheckedBorderColor = C.Track),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        list.forEach { c ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, color = C.White, fontSize = 17.sp)
                        Text(c.number, color = C.Text2, fontSize = 14.sp)
                    }
                    Text("Remove", color = C.Text2, fontSize = 14.sp, modifier = Modifier.clickable {
                        list = list - c
                        store.callers = list
                    }.padding(8.dp))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Caps("Add a number")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            number, { number = it }, label = { Text("Phone number") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Add", enabled = name.isNotBlank() && number.filter { it.isDigit() }.length >= 5) {
            list = list + Caller(name.trim(), number.trim())
            store.callers = list
            name = ""; number = ""
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun SetupScreen(back: () -> Unit, requestNotif: () -> Unit, requestRole: () -> Unit) {
    val ctx = LocalContext.current
    fun open(i: Intent) = ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    val pkgUri = Uri.parse("package:${ctx.packageName}")

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        ScreenHeader("Setup", back)
        SetupRow("1. Accessibility service", "Required. Lets Focus see which app is on screen and send you back. Find \"Focus lock\" and turn it on.", AppState.a11yOn) {
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        SetupRow("2. Display over other apps", "Required. Lets the timer screen come to the front.", AppState.overlayOn) {
            open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri))
        }
        SetupRow("3. Notifications", "Shows the ongoing session notification and keeps the timer alive.", AppState.notifOn) { requestNotif() }
        SetupRow("4. Call screening", "Needed for the allowed-callers list. Choose Focus when asked.", AppState.callRoleOn) { requestRole() }
        SetupRow("5. Battery: unrestricted", "Stops Samsung from killing the timer. Allow it here, then also set Focus to Unrestricted in Settings > Apps > Focus > Battery.", AppState.batteryOk) {
            open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri))
        }
        FocusCard(Modifier.fillMaxWidth()) {
            Text("Accessibility greyed out?", color = C.White, fontSize = 17.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Android 13 blocks sideloaded apps from this. Open Settings > Apps > Focus, tap the three dots at the top right, choose \"Allow restricted settings\", then go back to step 1. If the three dots are missing, open the accessibility toggle once first so Android shows the option.",
                color = C.Text2, fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            GhostButton("Open Focus app info") { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)) }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SetupRow(title: String, desc: String, done: Boolean, action: () -> Unit) {
    FocusCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), onClick = if (done) null else action) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = C.White, fontSize = 17.sp)
                Spacer(Modifier.height(2.dp))
                Text(desc, color = C.Text2, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(if (done) "On" else "Set", color = if (done) C.White else C.Black, fontSize = 13.sp,
                modifier = Modifier.clip(CircleShape).background(if (done) C.Track else C.White).padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}
