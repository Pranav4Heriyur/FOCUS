package com.pranav.focus.ui

import android.app.Activity
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.R
import com.pranav.focus.data.Caller
import com.pranav.focus.data.Fmt
import com.pranav.focus.data.Polyline
import com.pranav.focus.data.Store
import com.pranav.focus.lock.AppState
import com.pranav.focus.net.NetState
import com.pranav.focus.net.Notion
import com.pranav.focus.net.Secure
import com.pranav.focus.net.Strava
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos

// --------------------------------------------------------------------------------------
// Home: Strava hero
// --------------------------------------------------------------------------------------
@Composable
fun StravaHero(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var mode by remember { mutableIntStateOf(0) }
    val tick = AppState.dataTick
    val connected = remember(tick) { Strava.connected(ctx) }
    val act = remember(mode, tick) { store.activities.firstOrNull { if (mode == 0) it.isRun else it.isRide } }
    val shape = RoundedCornerShape(28.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(C.Card).border(1.dp, C.Hairline, shape)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(if (mode == 0) R.drawable.ic_directions_run else R.drawable.ic_directions_bike, size = 44.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(if (mode == 0) "Latest run" else "Latest ride", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        act != null -> act.name.ifBlank { "Activity" } + " · " + act.startDate
                        connected -> "Nothing recent"
                        else -> "Strava not connected"
                    }, color = C.Text2, fontSize = 13.sp, maxLines = 1,
                )
            }
            Row(Modifier.clip(CircleShape).border(1.dp, C.Hairline, CircleShape).background(C.Raised).padding(3.dp)) {
                listOf("Run", "Ride").forEachIndexed { i, o ->
                    Box(Modifier.clip(CircleShape).then(if (i == mode) Modifier.background(C.White) else Modifier).clickable { mode = i }
                        .padding(horizontal = 14.dp, vertical = 7.dp)) {
                        Text(o, color = if (i == mode) C.Black else C.Text2, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(170.dp).background(Color(0xFF070708)).clickable { if (!connected) go(Screen.Connections) },
            contentAlignment = Alignment.Center) {
            val pts = remember(act?.id) { act?.polyline?.takeIf { it.isNotBlank() }?.let { Polyline.decode(it) } ?: emptyList() }
            if (pts.size > 1) {
                Canvas(Modifier.fillMaxSize().padding(20.dp)) {
                    val minLat = pts.minOf { it.first }; val maxLat = pts.maxOf { it.first }
                    val minLng = pts.minOf { it.second }; val maxLng = pts.maxOf { it.second }
                    val kx = cos(Math.toRadians((minLat + maxLat) / 2))
                    val w = ((maxLng - minLng) * kx).coerceAtLeast(1e-6)
                    val h = (maxLat - minLat).coerceAtLeast(1e-6)
                    val sc = minOf(size.width / w, size.height / h).toFloat()
                    val ox = (size.width - (w * sc).toFloat()) / 2
                    val oy = (size.height - (h * sc).toFloat()) / 2
                    val path = Path()
                    pts.forEachIndexed { i, (la, ln) ->
                        val x = ox + (((ln - minLng) * kx) * sc).toFloat()
                        val y = oy + ((maxLat - la) * sc).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, Color.White.copy(alpha = 0.12f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, Color.White, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            } else {
                Text(if (connected) "No route yet" else "Connect Strava to see your route", color = C.Text3, fontSize = 13.sp)
            }
        }
        val a = act
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            HeroStat(if (a != null) Fmt.km(a.distM, store.imperial) else "—", if (store.imperial) "mi" else "km")
            HeroStat(if (a != null) Fmt.pace(a.distM, a.movingS, store.imperial) else "—", "pace")
            HeroStat(if (a != null) Fmt.clock(a.movingS) else "—", "time")
            HeroStat(if (a != null && a.avgHr > 0) "${a.avgHr.toInt()}" else "—", "bpm")
        }
        Text("Powered by Strava", color = C.Text3, fontSize = 10.sp, modifier = Modifier.padding(start = 16.dp, bottom = 12.dp))
    }
}

@Composable
private fun HeroStat(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Caps(l)
    }
}

// --------------------------------------------------------------------------------------
// A16 Connections
// --------------------------------------------------------------------------------------
@Composable
fun ConnectionsScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val tick = AppState.dataTick
    val status = NetState.strava
    val nstatus = NetState.notion
    var sId by remember { mutableStateOf(Strava.clientId(ctx)) }
    var sSec by remember { mutableStateOf("") }
    var nTok by remember { mutableStateOf("") }
    var nDb by remember { mutableStateOf("") }
    var watch by remember { mutableStateOf(store.watchMe) }
    val connected = remember(tick, status) { Strava.connected(ctx) }
    val nOn = remember(tick, nstatus) { Notion.configured(ctx) }
    val hasKeys = remember(tick, status) { Strava.hasKeys(ctx) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        ScreenHeader("Connections", back, "Your data, only where you send it.")

        FocusCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(R.drawable.ic_directions_run, active = connected)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Strava", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (connected) "Connected" + Strava.athlete(ctx).let { if (it.isNotBlank()) " as $it" else "" } else "Runs and rides, verified", color = C.Text2, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (connected) {
                GhostButton("Sync now", icon = R.drawable.ic_sync) { Strava.syncAsync(ctx) }
                Spacer(Modifier.height(8.dp))
                GhostButton("Disconnect") { Strava.disconnect(ctx); AppState.dataTick++ }
            } else {
                Text("Create a free app at strava.com/settings/api. Set the callback domain to focus.app, then paste the keys.", color = C.Text2, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(sId, { sId = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Client ID") }, colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(sSec, { sSec = it }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(if (hasKeys) "Client Secret (saved)" else "Client Secret") }, colors = fieldColors(),
                    visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Save and connect", enabled = sId.isNotBlank() && (sSec.isNotBlank() || hasKeys)) {
                    Strava.saveKeys(ctx, sId, if (sSec.isNotBlank()) sSec else Secure.get(ctx, "strava_secret"))
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Strava.authUri(ctx)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            if (status.isNotBlank()) Text(status, color = C.Text2, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        }

        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(R.drawable.ic_database, active = nOn)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Notion", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (nOn) "Each finished session is logged" else "Log sessions to a database", color = C.Text2, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (nOn) {
                GhostButton("Send today’s summary") {
                    Notion.pushAsync(ctx, "Focus · ${LocalDate.now()}",
                        "Focused ${Fmt.hm(store.todayFocusMinutes)} today. Streak ${store.streak} days. XP ${store.xp}.")
                }
                Spacer(Modifier.height(8.dp))
                GhostButton("Disconnect") { Notion.disconnect(ctx); AppState.dataTick++ }
            } else {
                Text("Create an internal integration at notion.so/my-integrations, share your database with it, then paste the token and database link.", color = C.Text2, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(nTok, { nTok = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Integration token") },
                    colors = fieldColors(), visualTransformation = PasswordVisualTransformation())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(nDb, { nDb = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Database link or ID") }, colors = fieldColors())
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Save", enabled = nTok.isNotBlank() && nDb.isNotBlank()) {
                    Notion.save(ctx, nTok, nDb); nTok = ""; NetState.notion = "Saved. Finished sessions will be logged."; AppState.dataTick++
                }
            }
            if (nstatus.isNotBlank()) Text(nstatus, color = C.Text2, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
        }

        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(R.drawable.ic_monitor_heart)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Health Connect", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Not in this build. Heart rate and sleep come later.", color = C.Text2, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(R.drawable.ic_watch, active = watch)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Watch worn by me", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Turn off when someone else wears it.", color = C.Text2, fontSize = 13.sp)
                }
                FocusSwitch(watch) { watch = it; store.watchMe = it; AppState.dataTick++ }
            }
            val wins = remember(tick, watch) { store.watchWindows().sortedByDescending { it.first } }
            val zone = ZoneId.systemDefault()
            val dayStart = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
            val todayMs = wins.sumOf { (s, e) -> (e - maxOf(s, dayStart)).coerceAtLeast(0) }
            Spacer(Modifier.height(10.dp))
            Text("On wrist today: ${Fmt.hm((todayMs / 60_000).toInt())}", color = C.Text2, fontSize = 13.sp)
            val f = DateTimeFormatter.ofPattern("d MMM HH:mm")
            wins.take(4).forEach { (s, e) ->
                Text("${Instant.ofEpochMilli(s).atZone(zone).format(f)} → ${Instant.ofEpochMilli(e).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    color = C.Text3, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

// --------------------------------------------------------------------------------------
// A17 Security
// --------------------------------------------------------------------------------------
fun canLockDevice(ctx: android.content.Context): Boolean {
    val bm = ctx.getSystemService(BiometricManager::class.java) ?: return false
    return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL) ==
        BiometricManager.BIOMETRIC_SUCCESS
}

@Composable
fun SecurityScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var bio by remember { mutableStateOf(store.biometricLock) }
    var exit by remember { mutableStateOf(store.lockOnExit) }
    var recents by remember { mutableStateOf(store.hideInRecents) }
    var auto by remember { mutableIntStateOf(when (store.autoLockSeconds) { 0 -> 0; 30 -> 1; 300 -> 2; else -> 3 }) }
    val tick = AppState.dataTick
    val can = remember { canLockDevice(ctx) }
    val sOn = remember(tick) { Strava.connected(ctx) }
    val nOn = remember(tick) { Notion.configured(ctx) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        ScreenHeader("Security", back, "Everything stays on this phone.")
        SwitchCard(R.drawable.ic_fingerprint, "Biometric lock", if (can) "Fingerprint or screen lock to open Focus" else "Set a screen lock in Android settings first", bio, can || bio) {
            bio = it; store.biometricLock = it
        }
        SwitchCard(R.drawable.ic_lock, "Lock on exit", "Ask again every time you leave the app", exit, bio) { exit = it; store.lockOnExit = it }
        SwitchCard(R.drawable.ic_visibility_off, "Hide in recents", "Blank preview and no screenshots", recents, true) { recents = it; store.hideInRecents = it }
        FocusCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Text("Auto-lock after", color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SegFull(listOf("Now", "30 s", "5 min", "Never"), auto) {
                auto = it; store.autoLockSeconds = when (it) { 0 -> 0; 1 -> 30; 2 -> 300; else -> -1 }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Connected tokens", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text("Encrypted with a key that never leaves the phone’s secure hardware.", color = C.Text2, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        TokenRow("Strava", sOn) { Strava.disconnect(ctx); AppState.dataTick++ }
        TokenRow("Notion", nOn) { Notion.disconnect(ctx); AppState.dataTick++ }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun SwitchCard(icon: Int, title: String, sub: String, on: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    FocusCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, active = on)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, color = C.Text2, fontSize = 13.sp)
            }
            FocusSwitch(on) { if (enabled) onChange(it) }
        }
    }
}

@Composable
private fun TokenRow(name: String, on: Boolean, revoke: () -> Unit) {
    FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(if (on) "Token stored" else "Not connected", color = C.Text2, fontSize = 13.sp)
            }
            if (on) Text("Revoke", color = C.Black, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(CircleShape).background(C.White).clickable { revoke() }.padding(horizontal = 14.dp, vertical = 7.dp))
        }
    }
}

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Ico(R.drawable.ic_lock, 48.dp)
        Spacer(Modifier.height(16.dp))
        Text("Focus is locked", color = C.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Unlock") { onUnlock() }
    }
}

// --------------------------------------------------------------------------------------
// A8 Allowed callers
// --------------------------------------------------------------------------------------
@Composable
fun CallersScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var list by remember { mutableStateOf(store.callers) }
    var only by remember { mutableStateOf(store.callersOnly) }
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    val blocked = remember { store.blockedCallsThisWeek().sum() }

    fun save(l: List<Caller>) { list = l; store.callers = l }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri: Uri? = r.data?.data
        if (r.resultCode == Activity.RESULT_OK && uri != null) {
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use {
                if (it.moveToFirst()) save(list + Caller(it.getString(0) ?: "Contact", it.getString(1) ?: ""))
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        ScreenHeader("Allowed callers", back, "During a session, only these numbers can ring you.")
        FocusCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Only these numbers can ring", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Needs call screening in Permissions.", color = C.Text2, fontSize = 14.sp)
                }
                FocusSwitch(only) { only = it; store.callersOnly = it }
            }
            Spacer(Modifier.height(8.dp))
            Text("$blocked blocked this week", color = C.Text3, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (list.size > 4) {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Search") }, colors = fieldColors(), shape = RoundedCornerShape(16.dp))
            Spacer(Modifier.height(8.dp))
        }
        list.filter { q.isBlank() || it.name.contains(q, true) || it.number.contains(q) }.forEach { c ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(R.drawable.ic_person, active = c.enabled, size = 44.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(c.name, color = C.White, fontSize = 17.sp)
                        Text(c.number, color = C.Text2, fontSize = 14.sp)
                    }
                    FocusSwitch(c.enabled) { on -> save(list.map { if (it === c) c.copy(enabled = on) else it }) }
                    Box(Modifier.clickable { save(list.filter { it !== c }) }.padding(8.dp)) { Ico(R.drawable.ic_delete, 20.dp, C.Text2) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        GhostButton("Import from contacts", icon = R.drawable.ic_contacts) {
            pick.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
        }
        Spacer(Modifier.height(16.dp))
        Caps("Add a number")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = fieldColors())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            number, { number = it }, label = { Text("Phone number") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Add", enabled = name.isNotBlank() && number.filter { it.isDigit() }.length >= 5) {
            save(list + Caller(name.trim(), number.trim())); name = ""; number = ""
        }
        Spacer(Modifier.height(32.dp))
    }
}
