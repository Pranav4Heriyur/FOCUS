package com.pranav.focus.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.pranav.focus.R
import com.pranav.focus.data.Caller
import com.pranav.focus.data.Store
import com.pranav.focus.lock.Allowed
import com.pranav.focus.lock.AppState
import com.pranav.focus.lock.SessionController
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

enum class Screen { Home, Focus, Study, Stats, Profile, Apps, Callers, Setup }

val TAB_SCREENS = listOf(Screen.Home, Screen.Study, Screen.Stats, Screen.Profile)

// --------------------------------------------------------------------------------------
// Tab bar
// --------------------------------------------------------------------------------------
@Composable
fun TabBar(current: Screen, onSelect: (Screen) -> Unit) {
    val tabs = listOf(
        Triple(Screen.Home, "Home", R.drawable.ic_home),
        Triple(Screen.Focus, "Focus", R.drawable.ic_adjust),
        Triple(Screen.Study, "Study", R.drawable.ic_menu_book),
        Triple(Screen.Stats, "Stats", R.drawable.ic_bar_chart),
        Triple(Screen.Profile, "Profile", R.drawable.ic_person),
    )
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp).fillMaxWidth().height(64.dp)
            .clip(CircleShape).background(C.Card.copy(alpha = 0.94f)).border(1.dp, C.Hairline, CircleShape).padding(5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        tabs.forEach { (screen, label, icon) ->
            val sel = screen == current
            Column(
                Modifier.weight(1f).fillMaxSize().clip(CircleShape).then(if (sel) Modifier.background(C.White) else Modifier)
                    .clickable { onSelect(screen) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Ico(icon, 24.dp, if (sel) C.Black else C.Text2)
                Text(label, color = if (sel) C.Black else C.Text2, fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// Home
// --------------------------------------------------------------------------------------
@Composable
fun HomeScreen(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var watchMe by remember { mutableStateOf(store.watchMe) }
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        else -> "Good evening,"
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(C.Sheet), contentAlignment = Alignment.Center) {
                Text("P", color = C.White, fontSize = 24.sp, fontWeight = FontWeight.Light)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(greeting, color = C.White, fontSize = 15.sp)
                Text("Pranav", color = C.White, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")), color = C.Text2, fontSize = 13.sp)
            }
            Row(
                Modifier.clip(CircleShape).border(1.dp, C.Hairline, CircleShape).background(C.Card)
                    .clickable { watchMe = !watchMe; store.watchMe = watchMe }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (watchMe) C.White else C.Text3))
                Spacer(Modifier.width(8.dp))
                Text(if (watchMe) "Watch: Me" else "Watch: Dad", color = C.White, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        StravaCard()
        Spacer(Modifier.height(12.dp))
        BalanceCard(store)
        Spacer(Modifier.height(12.dp))
        ScreenTimeCard(onSetup = { go(Screen.Setup) })
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Start focus session") { go(Screen.Focus) }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.clip(CircleShape).border(1.dp, C.Hairline, CircleShape).background(C.Raised).padding(3.dp)) {
        options.forEachIndexed { i, o ->
            val sel = i == selected
            Box(
                Modifier.clip(CircleShape).then(if (sel) Modifier.background(C.White) else Modifier)
                    .clickable { onSelect(i) }.padding(horizontal = 20.dp, vertical = 8.dp),
            ) { Text(o, color = if (sel) C.Black else C.Text2, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

/** Strava hero. Real runs arrive with the Strava connection; until then it is an honest empty state. */
@Composable
private fun StravaCard() {
    var mode by remember { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(28.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(C.Card).border(1.dp, C.Hairline, shape)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(if (mode == 0) R.drawable.ic_directions_run else R.drawable.ic_directions_bike, size = 44.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(if (mode == 0) "Latest run" else "Latest ride", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text("Strava not connected", color = C.Text2, fontSize = 13.sp)
            }
            Segmented(listOf("Run", "Ride"), mode) { mode = it }
        }
        Box(Modifier.fillMaxWidth().height(170.dp).background(Color(0xFF070708)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 28.dp.toPx()
                var x = 0f
                while (x < size.width) { drawLine(Color.White.copy(alpha = 0.04f), Offset(x, 0f), Offset(x + 40f, size.height), 1f); x += step }
                var y = 0f
                while (y < size.height) { drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, y), Offset(size.width, y + 14f), 1f); y += step }
                val path = Path().apply {
                    moveTo(size.width * 0.1f, size.height * 0.7f)
                    cubicTo(size.width * 0.3f, size.height * 0.9f, size.width * 0.35f, size.height * 0.3f, size.width * 0.55f, size.height * 0.4f)
                    cubicTo(size.width * 0.7f, size.height * 0.5f, size.width * 0.75f, size.height * 0.2f, size.width * 0.9f, size.height * 0.35f)
                }
                drawPath(path, Color.White.copy(alpha = 0.10f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                drawPath(path, Color.White.copy(alpha = 0.28f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            Text("Connect Strava to see your route here", color = C.Text2, fontSize = 14.sp)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            BigStat("—", "km"); BigStat("—", "/km"); BigStat("—", "time")
        }
    }
}

@Composable
private fun BigStat(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, color = C.White, fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.SemiBold)
        Text(l, color = C.Text2, fontSize = 14.sp)
    }
}

/** Radar: Focus and Discipline come from real data today; the rest fill in as integrations land. */
@Composable
private fun BalanceCard(store: Store) {
    val tick = AppState.tick
    val focus = remember(tick) { (store.todayFocusMinutes / 120f).coerceIn(0f, 1f) }
    val discipline = remember(tick) { (store.streak / 30f).coerceIn(0f, 1f) }
    val labels = listOf("Focus", "Workout", "Running", "Study", "Sleep", "Discipline")
    val values = listOf(focus, 0f, 0f, 0f, 0f, discipline)
    val weakest = if (values.all { it == 0f }) "—" else labels[values.indexOf(values.min())]

    FocusCard(Modifier.fillMaxWidth()) {
        Text("Today’s Balance", color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.fillMaxWidth().height(250.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = 78.dp.toPx()
                fun pt(i: Int, f: Float): Offset {
                    val a = Math.toRadians(-90.0 + i * 60.0)
                    return Offset(c.x + (r * f * cos(a)).toFloat(), c.y + (r * f * sin(a)).toFloat())
                }
                for (lvl in 1..4) {
                    val p = Path()
                    for (i in 0 until 6) { val o = pt(i, lvl / 4f); if (i == 0) p.moveTo(o.x, o.y) else p.lineTo(o.x, o.y) }
                    p.close()
                    drawPath(p, C.Track, style = Stroke(1.dp.toPx()))
                }
                for (i in 0 until 6) drawLine(C.Track, c, pt(i, 1f), 1.dp.toPx())
                val poly = Path()
                for (i in 0 until 6) { val o = pt(i, values[i].coerceAtLeast(0.04f)); if (i == 0) poly.moveTo(o.x, o.y) else poly.lineTo(o.x, o.y) }
                poly.close()
                drawPath(poly, Color.White.copy(alpha = 0.14f))
                drawPath(poly, Color.White, style = Stroke(2.dp.toPx()))
                for (i in 0 until 6) drawCircle(Color.White, 3.5.dp.toPx(), pt(i, values[i].coerceAtLeast(0.04f)))
            }
            Text("Focus", color = C.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.TopCenter))
            Text("Workout", color = C.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(bottom = 90.dp))
            Text("Running", color = C.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(top = 90.dp))
            Text("Study", color = C.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomCenter))
            Text("Sleep", color = C.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterStart).padding(top = 90.dp))
            Text("Discipline", color = C.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterStart).padding(bottom = 90.dp))
        }
        Text("Weakest: $weakest", color = C.Text2, fontSize = 14.sp)
        Text("Workout, Running, Study and Sleep fill in as you connect them.", color = C.Text3, fontSize = 12.sp)
    }
}

@Composable
private fun ScreenTimeCard(onSetup: () -> Unit) {
    val ctx = LocalContext.current
    val tick = AppState.tick
    val ok = remember(tick) { UsageHelper.hasAccess(ctx) }
    val apps = remember(tick, ok) { if (ok) UsageHelper.today(ctx) else emptyList() }

    FocusCard(Modifier.fillMaxWidth()) {
        Text("Phone Screen Time", color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        if (!ok) {
            Spacer(Modifier.height(4.dp))
            Text("Allow usage access to see today’s screen time and your top apps.", color = C.Text2, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            GhostButton("Allow usage access") {
                ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } else {
            val total = apps.sumOf { it.ms }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(UsageHelper.format(total), color = C.White, fontSize = 40.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold)
                    Text("today", color = C.Text2, fontSize = 13.sp)
                }
                Spacer(Modifier.width(16.dp))
                Canvas(Modifier.weight(1f).height(12.dp)) {
                    if (total > 0) {
                        val shades = listOf(1f, 0.6f, 0.32f, 0.14f)
                        var x = 0f
                        val top = apps.take(3).map { it.ms } + listOf((total - apps.take(3).sumOf { it.ms }).coerceAtLeast(0))
                        top.forEachIndexed { i, ms ->
                            val w = size.width * ms / total
                            if (w > 0) {
                                drawRoundRect(Color.White.copy(alpha = shades[i]), Offset(x, 0f), androidx.compose.ui.geometry.Size((w - 3.dp.toPx()).coerceAtLeast(2f), size.height), androidx.compose.ui.geometry.CornerRadius(size.height / 2))
                            }
                            x += w
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (apps.isEmpty()) Text("No app over 1 minute yet today.", color = C.Text2, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                apps.take(3).forEach { a ->
                    val icon = remember(a.pkg) { runCatching { ctx.packageManager.getApplicationIcon(a.pkg).toBitmap(80, 80).asImageBitmap() }.getOrNull() }
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (icon != null) Image(icon, null, Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)))
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(UsageHelper.format(a.ms), color = C.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(a.label, color = C.Text2, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// A2: Start focus session
// --------------------------------------------------------------------------------------
@Composable
fun FocusStartScreen(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var minutes by remember { mutableIntStateOf(store.lastMinutes) }
    var name by remember { mutableStateOf(store.lastName) }
    var subject by remember { mutableStateOf(store.lastSubject) }
    var strict by remember { mutableStateOf(store.strictLock) }

    val pm = ctx.packageManager
    val appNames = remember(store.allowedApps) {
        val labels = store.allowedApps.filter { Allowed.isInstalled(ctx, it) }
            .map { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString() }.getOrDefault(it) }
        (listOf("Phone") + labels.take(2)).joinToString(", ") + if (labels.size > 2) " +${labels.size - 2}" else ""
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ScreenHeader("Start Focus Session", { go(Screen.Home) }, "Choose a time, set your focus, and lock in.")
            MinutePicker(minutes) { d -> minutes = (minutes + d).coerceIn(5, 240) }

            Text("Session name", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            OutlinedTextField(
                value = name, onValueChange = { name = it }, singleLine = true,
                placeholder = { Text("e.g. Physics Practice") },
                leadingIcon = { Ico(R.drawable.ic_menu_book, 22.dp) },
                trailingIcon = { if (name.isNotEmpty()) Box(Modifier.clickable { name = "" }.padding(8.dp)) { Ico(R.drawable.ic_close, 18.dp, C.Text2) } },
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
            )
            Spacer(Modifier.height(16.dp))
            Text("Focus on", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Physics", "Chemistry", "Biology", "Workout", "Other").forEach { s ->
                    val sel = s == subject
                    Column(
                        Modifier.weight(1f).height(76.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (sel) C.White else C.Card).border(1.dp, if (sel) C.White else C.Hairline, RoundedCornerShape(16.dp))
                            .clickable { subject = s },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Ico(subjectIcon(s), 26.dp, if (sel) C.Black else C.White)
                        Spacer(Modifier.height(4.dp))
                        Text(s, color = if (sel) C.Black else C.Text2, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Preset time", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(25, 45, 90)
                presets.forEach { p ->
                    val sel = minutes == p
                    Box(
                        Modifier.weight(1f).height(48.dp).clip(CircleShape).background(if (sel) C.White else C.Card)
                            .border(1.dp, if (sel) C.White else C.Hairline, CircleShape).clickable { minutes = p },
                        contentAlignment = Alignment.Center,
                    ) { Text("$p min", color = if (sel) C.Black else C.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
                }
                val custom = minutes !in presets
                Box(
                    Modifier.weight(1f).height(48.dp).clip(CircleShape).background(if (custom) C.White else C.Card)
                        .border(1.dp, if (custom) C.White else C.Hairline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(if (custom) "$minutes min" else "Custom", color = if (custom) C.Black else C.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
            }
            Spacer(Modifier.height(16.dp))
            SettingRow(R.drawable.ic_grid_view, "Allowed apps", appNames, onEdit = { go(Screen.Apps) })
            Spacer(Modifier.height(10.dp))
            SettingRow(R.drawable.ic_call, "Allowed callers", "${store.callers.size} contacts", onEdit = { go(Screen.Callers) })
            Spacer(Modifier.height(10.dp))
            FocusCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(R.drawable.ic_lock)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Strict lock", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Text("Blocks all other apps", color = C.Text2, fontSize = 14.sp)
                    }
                    FocusSwitch(strict) { strict = it; store.strictLock = it }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (AppState.coreReady) {
            PrimaryButton("Begin Focus") { SessionController.start(ctx, minutes, name.trim(), subject) }
        } else {
            PrimaryButton("Finish setup to begin") { go(Screen.Setup) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SettingRow(icon: Int, title: String, sub: String, onEdit: () -> Unit) {
    FocusCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, color = C.Text2, fontSize = 14.sp, maxLines = 1)
            }
            Row(
                Modifier.clip(CircleShape).border(1.dp, C.Text3, CircleShape).clickable { onEdit() }.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Edit", color = C.White, fontSize = 14.sp)
                Ico(R.drawable.ic_chevron_right, 18.dp)
            }
        }
    }
}

/** Drag up or down to change minutes in steps of 5. */
@Composable
private fun MinutePicker(value: Int, onDelta: (Int) -> Unit) {
    val cb = rememberUpdatedState(onDelta)
    val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { 34.dp.toPx() }
    var acc by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxWidth().height(290.dp).pointerInput(Unit) {
            detectVerticalDragGestures { _, dy ->
                acc += dy
                while (acc <= -stepPx) { cb.value(5); acc += stepPx }
                while (acc >= stepPx) { cb.value(-5); acc -= stepPx }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(Color.White.copy(alpha = 0.06f), 140.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.04f), 110.dp.toPx(), c, style = Stroke(1.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            listOf(-10, -5).forEach { d ->
                val v = value + d
                Text(if (v >= 5) "$v" else " ", color = C.White.copy(alpha = if (d == -10) 0.18f else 0.38f), fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(
                Modifier.padding(vertical = 4.dp).clip(CircleShape).border(1.dp, C.Hairline, CircleShape).background(C.Raised)
                    .padding(horizontal = 36.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text("$value", color = C.White, fontSize = 72.sp, lineHeight = 80.sp, fontWeight = FontWeight.Bold)
                Text(" min", color = C.Text2, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
            }
            listOf(5, 10).forEach { d ->
                val v = value + d
                Text(if (v <= 240) "$v" else " ", color = C.White.copy(alpha = if (d == 5) 0.38f else 0.18f), fontSize = 34.sp, lineHeight = 44.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// Profile + placeholders
// --------------------------------------------------------------------------------------
@Composable
fun ProfileScreen(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 100.dp)) {
        ScreenHeader("Profile", null)
        FocusCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileStat("${store.xp}", "XP")
                ProfileStat("${store.streak}", "Day streak")
                ProfileStat("${store.totalFocusMinutes / 60}h ${store.totalFocusMinutes % 60}m", "Focused")
            }
        }
        Spacer(Modifier.height(12.dp))
        ProfileRow(R.drawable.ic_grid_view, "Allowed apps", "${store.allowedApps.size} chosen + Phone") { go(Screen.Apps) }
        Spacer(Modifier.height(10.dp))
        ProfileRow(R.drawable.ic_call, "Allowed callers", "${store.callers.size} numbers") { go(Screen.Callers) }
        Spacer(Modifier.height(10.dp))
        ProfileRow(R.drawable.ic_shield, "Setup and permissions", if (AppState.coreReady) "Lock ready" else "Action needed") { go(Screen.Setup) }
    }
}

@Composable
private fun ProfileStat(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, color = C.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Caps(l)
    }
}

@Composable
private fun ProfileRow(icon: Int, title: String, sub: String, onClick: () -> Unit) {
    FocusCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, color = C.Text2, fontSize = 14.sp)
            }
            Ico(R.drawable.ic_chevron_right, 22.dp, C.Text3)
        }
    }
}

@Composable
fun PlaceholderScreen(title: String, note: String) {
    Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 100.dp)) {
        ScreenHeader(title, null)
        FocusCard(Modifier.fillMaxWidth()) {
            Text("Coming next", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(note, color = C.Text2, fontSize = 14.sp)
        }
    }
}

// --------------------------------------------------------------------------------------
// A9 / A8 / A1
// --------------------------------------------------------------------------------------
@Composable
fun AppsScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val apps = remember { Allowed.launchableApps(ctx) }
    var selected by remember { mutableStateOf(store.allowedApps) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ScreenHeader("Allowed apps", back, "Phone is always allowed. Everything else is locked during a session unless switched on here.")
        Spacer(Modifier.height(8.dp))
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
                    FocusSwitch(on) { v ->
                        selected = if (v) selected + app.pkg else selected - app.pkg
                        store.allowedApps = selected
                    }
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
        ScreenHeader("Allowed callers", back, "During a session, only these numbers can ring you.")
        FocusCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Only these numbers can ring", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Needs the call-screening role (Setup).", color = C.Text2, fontSize = 14.sp)
                }
                FocusSwitch(only) { only = it; store.callersOnly = it }
            }
        }
        Spacer(Modifier.height(12.dp))
        list.forEach { c ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconTile(R.drawable.ic_person, size = 44.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(c.name, color = C.White, fontSize = 17.sp)
                        Text(c.number, color = C.Text2, fontSize = 14.sp)
                    }
                    Box(Modifier.clickable { list = list - c; store.callers = list }.padding(8.dp)) { Ico(R.drawable.ic_close, 20.dp, C.Text2) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
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
    val usageOk = remember(AppState.tick) { UsageHelper.hasAccess(ctx) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        ScreenHeader("Permissions", back, "Focus needs these to lock your phone properly.")
        SetupRow(R.drawable.ic_lock, "1. Accessibility service", "Required. Lets Focus see which app is on screen and send you back. Find “Focus lock” and turn it on.", AppState.a11yOn) {
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        SetupRow(R.drawable.ic_layers, "2. Display over other apps", "Required. Lets the timer screen come to the front.", AppState.overlayOn) {
            open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri))
        }
        SetupRow(R.drawable.ic_notifications, "3. Notifications", "Shows the ongoing session notification and keeps the timer alive.", AppState.notifOn) { requestNotif() }
        SetupRow(R.drawable.ic_call, "4. Call screening", "Needed for the allowed-callers list. Choose Focus when asked.", AppState.callRoleOn) { requestRole() }
        SetupRow(R.drawable.ic_bolt, "5. Battery: unrestricted", "Stops Samsung from killing the timer. Allow it here, then also set Focus to Unrestricted in Settings > Apps > Focus > Battery.", AppState.batteryOk) {
            open(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri))
        }
        SetupRow(R.drawable.ic_bar_chart, "6. Usage access", "Optional. Shows your screen time and top apps on Home.", usageOk) {
            open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        FocusCard(Modifier.fillMaxWidth()) {
            Text("Accessibility greyed out?", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Android 13 blocks sideloaded apps from this. Open Settings > Apps > Focus, tap the three dots at the top right, choose “Allow restricted settings”, then go back to step 1. If the three dots are missing, open the accessibility toggle once first so Android shows the option.",
                color = C.Text2, fontSize = 14.sp,
            )
            Spacer(Modifier.height(12.dp))
            GhostButton("Open Focus app info") { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)) }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SetupRow(icon: Int, title: String, desc: String, done: Boolean, action: () -> Unit) {
    FocusCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), onClick = if (done) null else action) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, active = done)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(desc, color = C.Text2, fontSize = 13.sp, lineHeight = 18.sp)
            }
            Text(if (done) "On" else "Set", color = if (done) C.White else C.Black, fontSize = 13.sp,
                modifier = Modifier.clip(CircleShape).background(if (done) C.Track else C.White).padding(horizontal = 12.dp, vertical = 6.dp))
        }
    }
}
