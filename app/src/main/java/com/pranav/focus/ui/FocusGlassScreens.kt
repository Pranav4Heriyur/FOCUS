package com.pranav.focus.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.R
import com.pranav.focus.data.Caller
import com.pranav.focus.data.Cond
import com.pranav.focus.data.Store
import com.pranav.focus.data.UnlockRule
import com.pranav.focus.lock.Allowed
import com.pranav.focus.lock.AppState
import com.pranav.focus.lock.Earning
import com.pranav.focus.lock.Enforcer
import com.pranav.focus.lock.SessionController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Glass-material versions of A1 Permissions, A2 Focus Setup, A8 Allowed Callers, A9 Allowed Apps, A10 Time Bank.
 * Signatures are unchanged from the screens they replace, so navigation / state / persistence are untouched.
 */

// ======================================================================================
// A1 — Permissions
// ======================================================================================
private data class PermRow(val icon: Int, val title: String, val desc: String, val ok: Boolean, val action: () -> Unit)

@Composable
fun SetupScreen(back: (() -> Unit)?, requestNotif: () -> Unit, requestRole: () -> Unit, onContinue: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    fun open(i: Intent) { runCatching { ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    val pkgUri = Uri.parse("package:${ctx.packageName}")
    val usageOk = remember(AppState.tick) { UsageHelper.hasAccess(ctx) }

    // Real OS state only. Nothing here fakes a grant.
    val perms = listOf(
        PermRow(R.drawable.focus_accessibility_white, "Accessibility", "Blocks selected apps and enforces focus mode", AppState.a11yOn) {
            open(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        },
        PermRow(R.drawable.focus_overlay_windows_white, "Display over other apps", "Shows timer overlay across all apps", AppState.overlayOn) {
            open(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkgUri))
        },
        PermRow(R.drawable.focus_usage_bars_white, "Usage access", "Tracks screen time and app usage", usageOk) {
            open(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        },
        PermRow(R.drawable.focus_phone_call_white, "Call screening", "Only allows selected contacts during focus", AppState.callRoleOn) { requestRole() },
        PermRow(R.drawable.focus_notification_bell_white, "Notifications", "Shows focus status and important alerts", AppState.notifOn) { requestNotif() },
    )
    val granted = perms.count { it.ok }
    val frac = granted / perms.size.toFloat()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        if (back != null) Box(Modifier.padding(top = 12.dp)) { GlassBack(back) } else Spacer(Modifier.height(12.dp))

        // Hero: concentric hairlines + partial lock-progress ring + lock
        Box(Modifier.fillMaxWidth().height(184.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                drawCircle(Color.White.copy(alpha = 0.05f), 92.dp.toPx(), c, style = Stroke(1.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.035f), 150.dp.toPx(), c, style = Stroke(1.dp.toPx()))
            }
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(104.dp).glyphGlow(0.10f))
                FgRing(frac, Modifier.size(150.dp), stroke = 5.dp)
                Image(painterResource(R.drawable.focus_lock_hero_white), "Lock", Modifier.size(58.dp))
            }
        }
        FgGlowText("Set up your lock", Modifier.align(Alignment.CenterHorizontally), fontSize = 34.sp, fontWeight = FontWeight.SemiBold, tiers = 2)
        Text("5 permissions make the lock work", color = C.Text2, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp))
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$granted of ${perms.size} granted", color = C.White, fontSize = 14.sp)
            Text("${(frac * 100).roundToInt()}%", color = C.Text2, fontSize = 14.sp)
        }
        FgBar(frac, Modifier.padding(top = 2.dp), thickness = 6.dp)
        Spacer(Modifier.height(6.dp))

        perms.forEach { p -> PermissionRow(p); Spacer(Modifier.height(10.dp)) }

        Spacer(Modifier.height(6.dp))
        FgPrimaryButton("Continue") { (onContinue ?: back)?.invoke() }
        Text("You can change these later in Settings", color = C.Text3, fontSize = 13.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp))
        // Samsung needs these two to keep the timer alive; the reference has no row for them, so they stay as a quiet link.
        Text("Battery & restricted settings", color = C.Text2, fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally).clip(CircleShape)
                .clickable { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri)) }.padding(horizontal = 16.dp, vertical = 12.dp))
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PermissionRow(p: PermRow) {
    Row(Modifier.fillMaxWidth().fgSurface(22.dp).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        FgTile(52.dp) { Ico(p.icon, 28.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(p.title, color = C.White, fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Spacer(Modifier.height(2.dp))
            Text(p.desc, color = C.Text2, fontSize = 13.sp, lineHeight = 17.sp)
        }
        Box(Modifier.width(1.dp).height(52.dp).background(Color.White.copy(alpha = 0.10f)))
        Column(Modifier.width(66.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (p.ok) CheckBadge(32.dp)
            else Box(Modifier.size(32.dp).border(1.5.dp, Color.White, CircleShape))
            Text(if (p.ok) "Granted" else "Needed", color = C.Text2, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
        }
        Box(Modifier.width(74.dp), contentAlignment = Alignment.CenterEnd) {
            if (!p.ok) GlassOutlineButton("Allow", height = 40.dp, fontSize = 15.sp, onClick = p.action, modifier = Modifier.width(72.dp))
        }
    }
}

// ======================================================================================
// A2 — Focus Setup
// ======================================================================================
private val DURATION_STOPS = listOf(5, 10, 15, 20, 25, 30, 35, 45, 60, 75, 90, 105, 120, 150, 180, 240)

@Composable
fun FocusStartScreen(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var minutes by remember { mutableIntStateOf(store.lastMinutes) }
    var extra by remember { mutableStateOf(store.lastMinutes.takeIf { it !in DURATION_STOPS }) }
    var name by remember { mutableStateOf(store.lastName) }
    var subject by remember { mutableStateOf(store.lastSubject) }
    var strict by remember { mutableStateOf(store.strictLock) }
    var askCustom by remember { mutableStateOf(false) }
    if (askCustom) NameDialog("Custom time (minutes)", minutes.toString(), emptyList(), "", { askCustom = false }) { n, _ ->
        n.filter { it.isDigit() }.toIntOrNull()?.let { v ->
            minutes = v.coerceIn(1, 600)
            extra = minutes.takeIf { it !in DURATION_STOPS }
        }
        askCustom = false
    }
    val stops = remember(extra) { (DURATION_STOPS + listOfNotNull(extra)).sorted() }

    val pm = ctx.packageManager
    val appNames = remember(store.allowedApps) {
        val labels = store.allowedApps.filter { Allowed.isInstalled(ctx, it) }
            .map { runCatching { pm.getApplicationLabel(pm.getApplicationInfo(it, 0)).toString() }.getOrDefault(it) }
        (listOf("Phone") + labels.take(2)).joinToString(", ") + if (labels.size > 2) " +${labels.size - 2}" else ""
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                GlassBack({ go(Screen.Home) }, Modifier.align(Alignment.CenterStart))
                Column(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    FgGlowText("Start Focus Session", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, tiers = 2)
                    Text("Choose a time, set your focus, and lock in.", color = C.Text2, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
            }
            DurationWheel(minutes, stops) { minutes = it }

            Text("Session name", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth().height(58.dp).fgSurface(20.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Ico(R.drawable.focus_book_open_white, 24.dp)
                Box(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    if (name.isEmpty()) Text("e.g. Physics Practice", color = C.Text3, fontSize = 17.sp)
                    BasicTextField(name, { name = it }, singleLine = true, textStyle = TextStyle(color = C.White, fontSize = 17.sp), cursorBrush = SolidColor(C.White), modifier = Modifier.fillMaxWidth())
                }
                if (name.isNotEmpty()) Box(
                    Modifier.size(26.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.78f)).clickable(role = androidx.compose.ui.semantics.Role.Button) { name = "" },
                    contentAlignment = Alignment.Center,
                ) { Ico(R.drawable.focus_close_white, 14.dp, Color.Black) }
            }
            Spacer(Modifier.height(16.dp))
            Text("Focus on", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Physics", "Chemistry", "Biology", "Workout", "Other").forEach { s ->
                    val sel = s == subject
                    Column(
                        Modifier.weight(1f).height(76.dp)
                            .then(if (sel) Modifier.whiteHalo(16.dp, 14.dp to 0.16f, 5.dp to 0.22f).clip(RoundedCornerShape(16.dp)).background(C.White) else Modifier.fgSurface(16.dp))
                            .clickable(role = androidx.compose.ui.semantics.Role.Button) { subject = s },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                    ) {
                        Ico(subjectGlyph(s), 28.dp, if (sel) C.Black else C.White)
                        Spacer(Modifier.height(5.dp))
                        Text(s, color = if (sel) C.Black else C.Text2, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Preset time", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val presets = listOf(25, 45, 90)
                presets.forEach { p -> GlassChip("$p min", minutes == p, Modifier.weight(1f)) { minutes = p } }
                val custom = minutes !in presets
                GlassChip(if (custom) "$minutes min" else "Custom", custom, Modifier.weight(1f)) { askCustom = true }
            }
            Spacer(Modifier.height(16.dp))
            SetupLinkRow(R.drawable.focus_app_grid_white, "Allowed apps", appNames) { go(Screen.Apps) }
            Spacer(Modifier.height(10.dp))
            SetupLinkRow(R.drawable.focus_phone_call_white, "Allowed callers", "${store.callers.size} contacts") { go(Screen.Callers) }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().fgSurface(20.dp).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                FgTile(48.dp) { Ico(R.drawable.focus_lock_small_white, 26.dp) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Strict lock", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Blocks all other apps and websites", color = C.Text2, fontSize = 14.sp, maxLines = 2)
                }
                GlassSwitch(strict, { strict = it; store.strictLock = it })
            }
            Spacer(Modifier.height(16.dp))
        }
        if (AppState.coreReady) {
            FgPrimaryButton("Begin Focus") { SessionController.start(ctx, minutes, name.trim(), subject) }
        } else {
            FgPrimaryButton("Finish setup to begin") { go(Screen.Setup) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private fun subjectGlyph(s: String) = when (s) {
    "Physics" -> R.drawable.focus_physics_atom_white
    "Chemistry" -> R.drawable.focus_chemistry_flask_white
    "Biology" -> R.drawable.focus_biology_leaf_white
    "Workout" -> R.drawable.focus_workout_dumbbell_white
    else -> R.drawable.focus_ellipsis_white
}

@Composable
private fun SetupLinkRow(icon: Int, title: String, sub: String, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth().fgSurface(20.dp).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        FgTile(48.dp) { Ico(icon, 26.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(title, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = C.Text2, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        GlassOutlineButton("Edit", trailingChevron = true, height = 42.dp, onClick = onEdit)
    }
}

/** Snapping vertical wheel over duration stops. Selected value sits in a glass capsule with diffusion; neighbours fade with no glow. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DurationWheel(minutes: Int, stops: List<Int>, onChange: (Int) -> Unit) {
    val itemH = 54.dp
    val itemPx = with(LocalDensity.current) { itemH.toPx() }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = stops.indexOf(minutes).coerceAtLeast(0))
    val fling = rememberSnapFlingBehavior(state)
    val cb = rememberUpdatedState(onChange)
    val live by remember(stops) {
        derivedStateOf {
            (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0).coerceIn(0, stops.lastIndex)
        }
    }
    LaunchedEffect(state, stops) {
        snapshotFlow { if (state.isScrollInProgress) -1 else state.firstVisibleItemIndex }
            .distinctUntilChanged().collect { if (it >= 0) cb.value(stops[it.coerceIn(0, stops.lastIndex)]) }
    }
    LaunchedEffect(minutes, stops) {
        val t = stops.indexOf(minutes)
        if (t >= 0 && t != state.firstVisibleItemIndex && !state.isScrollInProgress) state.animateScrollToItem(t)
    }
    Box(Modifier.fillMaxWidth().height(itemH * 5 + 24.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Color.White.copy(alpha = 0.06f), 150.dp.toPx(), center, style = Stroke(1.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.04f), 118.dp.toPx(), center, style = Stroke(1.dp.toPx()))
        }
        LazyColumn(Modifier.fillMaxWidth().height(itemH * 5), state = state, flingBehavior = fling) {
            items(2) { Spacer(Modifier.height(itemH)) }
            items(stops.size) { i ->
                val d = kotlin.math.abs(i - live)
                Box(Modifier.fillMaxWidth().height(itemH), contentAlignment = Alignment.Center) {
                    Text("${stops[i]}", color = C.White.copy(alpha = when (d) { 0 -> 0f; 1 -> 0.5f; 2 -> 0.2f; else -> 0.06f }),
                        fontSize = 38.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            items(2) { Spacer(Modifier.height(itemH)) }
        }
        // Selected capsule
        Row(
            Modifier.fillMaxWidth(0.62f).height(78.dp).fgSurface(39.dp),
            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
        ) {
            FgGlowText("${stops[live]}", fontSize = 60.sp, fontWeight = FontWeight.Bold, tiers = 3)
            Text(" min", color = C.Text2, fontSize = 18.sp, modifier = Modifier.padding(start = 10.dp, top = 22.dp))
        }
    }
}

// ======================================================================================
// A8 — Allowed Callers
// ======================================================================================
@Composable
fun CallersScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var list by remember { mutableStateOf(store.callers) }
    var q by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    val week = remember { store.blockedCallsThisWeek() }

    fun save(l: List<Caller>) { list = l; store.callers = l }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri: Uri? = r.data?.data
        if (r.resultCode == Activity.RESULT_OK && uri != null) {
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use {
                if (it.moveToFirst()) save(list + Caller(it.getString(0) ?: "Contact", it.getString(1) ?: ""))
            }
        }
    }

    val shown = list.filter { q.isBlank() || it.name.contains(q, true) || it.number.replace(" ", "").contains(q.replace(" ", "")) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            GlassBack(back, Modifier.align(Alignment.CenterStart))
            Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                FgGlowText("Allowed Callers", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, tiers = 2)
                Text("Only these numbers can reach you during focus.", color = C.Text2, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth().height(54.dp).fgSurface(20.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Ico(R.drawable.focus_search_white, 22.dp, C.Text2)
            Box(Modifier.weight(1f).padding(start = 14.dp)) {
                if (q.isEmpty()) Text("Search name or number", color = C.Text3, fontSize = 17.sp)
                BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(color = C.White, fontSize = 17.sp), cursorBrush = SolidColor(C.White), modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(10.dp))
        if (list.isNotEmpty()) Text("← Swipe a row to delete", color = C.Text3, fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
        shown.forEach { c ->
            SwipeRow(onDelete = { save(list.filter { it !== c }) }) {
                Row(Modifier.fillMaxWidth().fgSurface(20.dp).padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    FgTile(46.dp, 13.dp) { Text(c.name.trim().take(1).uppercase().ifEmpty { "?" }, color = C.White, fontSize = 20.sp, fontWeight = FontWeight.Medium) }
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(c.name, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(c.number, color = C.Text2, fontSize = 14.sp, maxLines = 1)
                    }
                    GlassSwitch(c.enabled, { on -> save(list.map { if (it === c) c.copy(enabled = on) else it }) })
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        if (list.isEmpty()) Text("No numbers yet. Add one or import from your contacts.", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp))
        Spacer(Modifier.height(4.dp))
        GlassOutlineButton("Import from contacts", Modifier.fillMaxWidth(), icon = R.drawable.focus_contacts_white, height = 54.dp, fontSize = 17.sp) {
            pick.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
        }
        Spacer(Modifier.height(10.dp))
        FgPrimaryButton("+ Add number") { sheet = true }
        Spacer(Modifier.height(14.dp))
        BlockedCallsCard(week)
        Spacer(Modifier.height(24.dp))
    }
    if (sheet) AddNumberSheet({ sheet = false }) { n, num -> save(list + Caller(n, num)) }
}

/** Horizontal reveal: drag left to expose Delete; tap Delete to remove. Also exposes a TalkBack "Delete" action. */
@Composable
private fun SwipeRow(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val revealPx = with(LocalDensity.current) { 96.dp.toPx() }
    val off = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val drag = rememberDraggableState { d -> scope.launch { off.snapTo((off.value + d).coerceIn(-revealPx, 0f)) } }
    Box(Modifier.fillMaxWidth().semantics { customActions = listOf(CustomAccessibilityAction("Delete") { onDelete(); true }) }) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = (-off.value / revealPx).coerceIn(0f, 1f) }.clip(RoundedCornerShape(20.dp)), contentAlignment = Alignment.CenterEnd) {
            Column(
                Modifier.width(96.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF3A3A3D), Color(0xFF59595C))))
                    .clickable(role = androidx.compose.ui.semantics.Role.Button) { onDelete() },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                Ico(R.drawable.focus_trash_white, 22.dp)
                Text("Delete", color = C.White, fontSize = 13.sp)
            }
        }
        Box(
            Modifier.offset { IntOffset(off.value.roundToInt(), 0) }.draggable(drag, Orientation.Horizontal, onDragStopped = {
                scope.launch { off.animateTo(if (off.value < -revealPx * 0.4f) -revealPx else 0f, tween(STD_MS)) }
            }),
        ) { content() }
    }
}

@Composable
private fun BlockedCallsCard(week: List<Int>) {
    Row(Modifier.fillMaxWidth().fgSurface(20.dp).padding(14.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FgTile(40.dp, 12.dp) { Ico(R.drawable.focus_call_blocked_white, 22.dp) }
                Text("BLOCKED CALLS THIS WEEK", color = C.Text2, fontSize = 10.sp, letterSpacing = 1.6.sp, modifier = Modifier.padding(start = 12.dp), maxLines = 2)
            }
            FgGlowText("${week.sum()}", Modifier.padding(start = 6.dp, top = 6.dp), fontSize = 52.sp, tiers = 3)
        }
        Column(Modifier.width(132.dp).padding(top = 8.dp)) {
            val mx = (week.maxOrNull() ?: 0).coerceAtLeast(1)
            Canvas(Modifier.fillMaxWidth().height(56.dp)) {
                val n = week.size
                val gap = 8.dp.toPx()
                val bw = (size.width - gap * (n - 1)) / n
                week.forEachIndexed { i, v ->
                    val h = (size.height * v / mx).coerceAtLeast(5.dp.toPx())
                    val hot = v == mx && v > 0
                    val tl = Offset(i * (bw + gap), size.height - h)
                    if (hot) drawRoundRect(Color.White.copy(alpha = 0.16f), Offset(tl.x - 3.dp.toPx(), tl.y - 3.dp.toPx()), Size(bw + 6.dp.toPx(), h + 6.dp.toPx()), CornerRadius(4.dp.toPx()))
                    drawRoundRect(
                        if (hot) SolidColor(Color.White) else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.16f)), startY = tl.y, endY = size.height),
                        tl, Size(bw, h), CornerRadius(2.dp.toPx()),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                "MTWTFSS".forEach { Text("$it", color = C.Text3, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun AddNumberSheet(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    val valid = name.isNotBlank() && number.count { it.isDigit() } >= 5
    FocusSheet(onDismiss) {
        Text("Add number", color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        SheetInput(name, "Name", KeyboardType.Text) { name = it }
        Spacer(Modifier.height(8.dp))
        SheetInput(number, "Phone number", KeyboardType.Phone) { number = it }
        Spacer(Modifier.height(16.dp))
        FgPrimaryButton("Add", enabled = valid) { onAdd(name.trim(), number.trim()); onDismiss() }
    }
}

@Composable
private fun SheetInput(value: String, hint: String, type: KeyboardType, onChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.Raised).border(1.dp, C.Hairline, RoundedCornerShape(16.dp)).padding(16.dp)) {
        if (value.isEmpty()) Text(hint, color = C.Text3, fontSize = 16.sp)
        BasicTextField(value, onChange, singleLine = true, textStyle = TextStyle(color = C.White, fontSize = 16.sp), cursorBrush = SolidColor(C.White),
            keyboardOptions = KeyboardOptions(keyboardType = type), modifier = Modifier.fillMaxWidth())
    }
}

// ======================================================================================
// A9 — Allowed Apps
// ======================================================================================
private data class AppRowData(val pkg: String, val label: String, val category: String)

private fun categoryFor(pkg: String, label: String): String {
    val k = (pkg + " " + label).lowercase()
    return when {
        "anthropic" in k || "bard" in k || "gemini" in k || "claude" in k || "chatgpt" in k -> "AI Assistant"
        "calculator" in k -> "Utilities"
        "instagram" in k || "facebook" in k || "snapchat" in k || "twitter" in k || "reddit" in k -> "Social"
        "youtube" in k || "netflix" in k || "hotstar" in k || "primevideo" in k -> "Entertainment"
        "whatsapp" in k || "telegram" in k || "dialer" in k || "messag" in k || "phone" in k || "sms" in k -> "Communication"
        "spotify" in k || "music" in k || "gaana" in k -> "Music"
        "chrome" in k || "gmail" in k || "notes" in k || "keep" in k || "docs" in k || "drive" in k || "calendar" in k || "outlook" in k -> "Productivity"
        "camera" in k || "gallery" in k || "photos" in k -> "Media"
        "maps" in k || "uber" in k -> "Travel"
        else -> "App"
    }
}

@Composable
fun AppsScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val dialer = remember { Allowed.dialerPackage(ctx) }
    val rows = remember {
        val initial = store.allowedApps
        Allowed.launchableApps(ctx)
            .filter { it.pkg != dialer && it.pkg != "com.android.settings" }
            .map { AppRowData(it.pkg, it.label, categoryFor(it.pkg, it.label)) }
            // Allowed apps first, ordered once, so rows don't jump while toggling.
            .sortedWith(compareBy({ it.pkg !in initial }, { it.label.lowercase() }))
    }
    var selected by remember { mutableStateOf(store.allowedApps) }
    var q by remember { mutableStateOf("") }
    val installedSelected = rows.filter { it.pkg in selected }
    val count = installedSelected.size + 1 // Phone is always allowed
    val filtered = rows.filter { q.isBlank() || it.label.contains(q, true) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    GlassBack(back, Modifier.align(Alignment.CenterStart))
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        FgGlowText("Allowed Apps During Focus", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, tiers = 2)
                        Text("Everything else is locked.", color = C.Text2, fontSize = 15.sp)
                    }
                }
            }
            item {
                LazyRow(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { ShortcutTile("Phone") { Ico(R.drawable.focus_phone_call_white, 36.dp) } }
                    items(installedSelected, key = { it.pkg }) { a ->
                        ShortcutTile(a.label) { MonoGlyph(a) }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(52.dp).fgSurface(18.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Ico(R.drawable.focus_search_white, 22.dp, C.Text2)
                    Box(Modifier.weight(1f).padding(start = 14.dp)) {
                        if (q.isEmpty()) Text("Search apps...", color = C.Text3, fontSize = 17.sp)
                        BasicTextField(q, { q = it }, singleLine = true, textStyle = TextStyle(color = C.White, fontSize = 17.sp), cursorBrush = SolidColor(C.White), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Select apps", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("$count selected", color = C.Text2, fontSize = 14.sp)
                }
            }
            if (q.isBlank() || "phone".contains(q, true)) item {
                AppToggleRow(dialer, "Phone", "Communication", on = true, onToggle = null)
            }
            items(filtered, key = { it.pkg }) { a ->
                val on = a.pkg in selected
                AppToggleRow(a.pkg, a.label, a.category, on) { selected = if (on) selected - a.pkg else selected + a.pkg }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp).fgSurface(18.dp).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FgTile(40.dp, 12.dp) { Ico(R.drawable.ic_settings, 22.dp) }
                    Text("Settings is always blocked during a session.", color = C.White, fontSize = 15.sp, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        FgPrimaryButton("Save allowed apps") { store.allowedApps = selected; back() }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ShortcutTile(label: String, glyph: @Composable () -> Unit) {
    Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp)) {
            Box(Modifier.align(Alignment.BottomStart).size(68.dp).fgSurface(20.dp), contentAlignment = Alignment.Center) { glyph() }
            Box(Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(Color(0xFFF2F2F4)), contentAlignment = Alignment.Center) {
                Ico(R.drawable.focus_checkmark_black, 14.dp, Color.Black)
            }
        }
        Text(label, color = C.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    }
}

/** Monochrome shortcut glyphs; unknown apps fall back to their real icon desaturated. */
@Composable
private fun MonoGlyph(a: AppRowData) {
    val k = (a.pkg + " " + a.label).lowercase()
    when {
        "anthropic" in k || "claude" in k -> Canvas(Modifier.size(38.dp)) {
            val c = center; val r = size.minDimension / 2
            for (i in 0 until 12) {
                val t = Math.toRadians(i * 30.0)
                drawLine(Color.White, Offset(c.x + (r * 0.28f * cos(t)).toFloat(), c.y + (r * 0.28f * sin(t)).toFloat()),
                    Offset(c.x + (r * cos(t)).toFloat(), c.y + (r * sin(t)).toFloat()), 3.dp.toPx(), StrokeCap.Round)
            }
        }
        "bard" in k || "gemini" in k -> Canvas(Modifier.size(38.dp)) {
            val c = center; val r = size.minDimension / 2
            val p = Path().apply {
                moveTo(c.x, c.y - r); quadraticBezierTo(c.x, c.y, c.x + r, c.y); quadraticBezierTo(c.x, c.y, c.x, c.y + r)
                quadraticBezierTo(c.x, c.y, c.x - r, c.y); quadraticBezierTo(c.x, c.y, c.x, c.y - r); close()
            }
            drawPath(p, Color.White)
        }
        "calculator" in k -> Ico(R.drawable.ic_calculate, 36.dp)
        "notes" in k || "keep" in k -> Ico(R.drawable.focus_document_white, 34.dp)
        else -> AppIcon(a.pkg, 40.dp, mono = true)
    }
}

@Composable
private fun AppToggleRow(pkg: String?, label: String, category: String, on: Boolean, onToggle: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().height(66.dp).fgSurface(18.dp)
            .then(if (onToggle != null) Modifier.clickable(role = androidx.compose.ui.semantics.Role.Switch) { onToggle() } else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Real (coloured) launcher icon from PackageManager; clean tile if the package is unavailable.
        if (pkg != null && Allowed.isInstalled(LocalContext.current, pkg)) AppIcon(pkg, 46.dp)
        else FgTile(46.dp, 12.dp) { Ico(R.drawable.focus_phone_call_white, 24.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(label, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(category, color = C.Text2, fontSize = 14.sp, maxLines = 1)
        }
        GlassSwitch(on, null)
    }
}

// ======================================================================================
// A10 — Time Bank
// ======================================================================================
private data class EarnRow(val glyph: Int, val title: String, val sub: String, val prog: Double, val target: Int, val label: String, val reward: Int, val onClick: () -> Unit)

@Composable
fun TimeBankScreen(back: () -> Unit, onRules: () -> Unit, onFocus: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val tick = AppState.dataTick + AppState.tick
    var bump by remember { mutableIntStateOf(0) }
    var help by remember { mutableStateOf(false) }
    val day = remember(tick, bump) { store.day() }
    val bank = remember(tick, bump) { store.bankMinutes }
    val locked = remember(tick, bump) { store.appRules.filter { it.locked } }

    val earn = remember(tick, bump) {
        fun act(id: String) = Earning.ACTIONS.first { it.id == id }
        val run = act("run"); val wo = act("workout"); val st = act("study")
        listOf(
            EarnRow(R.drawable.focus_clock_white, "Focus for 25 min", "Complete a focus session", Earning.progress(store, Cond.FOCUS), 25, "${Earning.progress(store, Cond.FOCUS).toInt()} / 25 min", 5, onFocus),
            EarnRow(R.drawable.focus_run_white, UnlockRule("", run.cond, run.target, "", 0).title(), "Verified via Strava", Earning.progress(store, run.cond), run.target,
                "%.1f / %d km".format(Earning.progress(store, run.cond), run.target), run.minutes, onRules),
            EarnRow(R.drawable.focus_workout_dumbbell_white, UnlockRule("", wo.cond, wo.target, "", 0).title(), "Verified via Health Connect", Earning.progress(store, wo.cond), wo.target,
                "${Earning.progress(store, wo.cond).toInt()} / ${wo.target} workout", wo.minutes, onRules),
            EarnRow(R.drawable.focus_book_open_white, UnlockRule("", st.cond, st.target, "", 0).title(), "With 60%+ accuracy", Earning.progress(store, st.cond), st.target,
                "${Earning.progress(store, st.cond).toInt()} / ${st.target}", st.minutes, onRules),
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                FgGlowText("Time Bank", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, tiers = 2)
                Text("Earn time. Use it wisely.", color = C.Text2, fontSize = 15.sp)
            }
            GlassOutlineButton("How it works", icon = R.drawable.focus_clock_white, height = 42.dp, fontSize = 14.sp) { help = true }
        }

        // Hero
        Column(Modifier.fillMaxWidth().fgSurface(24.dp).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FgTile(40.dp, 12.dp) { Ico(R.drawable.ic_database, 22.dp) }
                Text("YOUR TIME BANK", color = C.Text2, fontSize = 11.sp, letterSpacing = 2.sp, modifier = Modifier.padding(start = 12.dp))
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    FgGlowText("${day.earnedTotal} min", Modifier.padding(top = 6.dp), fontSize = 62.sp, lineHeight = 68.sp, fontWeight = FontWeight.Bold, tiers = 3)
                    Text("earned today", color = C.White.copy(alpha = 0.92f), fontSize = 22.sp)
                }
                TimeStackArt(Modifier.size(width = 112.dp, height = 132.dp))
            }
            Spacer(Modifier.height(4.dp))
            FgBar(if (day.earnedTotal > 0) day.fromPool / day.earnedTotal.toFloat() else 0f, thickness = 9.dp)
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${day.earnedTotal} min earned", color = C.White, fontSize = 14.sp)
                Text("${day.fromPool} min spent", color = C.White, fontSize = 14.sp)
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp, start = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Locked apps", color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Use your time to unlock", color = C.Text2, fontSize = 13.sp)
        }
        if (locked.isEmpty()) Text("Nothing locked yet. Add apps in App Rules.", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(4.dp))
        locked.forEach { r ->
            val alloc = day.alloc[r.pkg] ?: 0
            val avail = minOf(bank, (r.limitMin - alloc).coerceAtLeast(0))
            val rem = Enforcer.remainingMin(ctx, store, r.pkg)
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).fgSurface(18.dp).padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(r.pkg, 46.dp)
                Spacer(Modifier.width(8.dp))
                FgRing(if (r.limitMin > 0) (rem / r.limitMin.toFloat()).coerceIn(0f, 1f) else 0f, Modifier.size(34.dp), stroke = 2.5.dp, animate = false)
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(appLabel(ctx, r.pkg), color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Ico(R.drawable.ic_lock, 13.dp, C.Text2)
                        Text(" " + if (rem > 0) "$rem min left" else "Locked", color = C.Text2, fontSize = 13.sp, maxLines = 1)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 6.dp)) {
                    Text("$avail min", color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("available", color = C.Text2, fontSize = 12.sp)
                }
                val can = bank > 0
                Box(
                    Modifier.height(40.dp).then(if (can) Modifier.whiteHalo(20.dp, 10.dp to 0.16f, 4.dp to 0.22f) else Modifier)
                        .clip(CircleShape).background(if (can) C.White else C.Track)
                        .clickable(enabled = can, role = androidx.compose.ui.semantics.Role.Button) { Earning.unlockFromPool(ctx, r.pkg, 15); bump++; AppState.dataTick++ }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Unlock 15 min", color = if (can) C.Black else C.Text3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp, start = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Earn more time", color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Complete these to add to your Time Bank", color = C.Text2, fontSize = 11.sp, maxLines = 1)
        }
        Column(Modifier.fillMaxWidth().fgSurface(22.dp)) {
            earn.forEachIndexed { i, e ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.08f)))
                Row(Modifier.fillMaxWidth().clickable { e.onClick() }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    FgTile(46.dp, 13.dp) { Ico(e.glyph, 26.dp) }
                    Spacer(Modifier.width(8.dp))
                    FgRing((e.prog / e.target).toFloat(), Modifier.size(38.dp), stroke = 3.dp, animate = false)
                    Column(Modifier.weight(1f).padding(start = 6.dp)) {
                        Text(e.title, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(e.sub, color = C.Text2, fontSize = 12.sp, lineHeight = 15.sp, maxLines = 2)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { FgBar((e.prog / e.target).toFloat(), thickness = 4.dp, glow = false) }
                            Text(e.label, color = C.Text2, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    Box(Modifier.padding(horizontal = 8.dp).width(1.dp).height(44.dp).background(Color.White.copy(alpha = 0.10f)))
                    Text("+${e.reward} min", color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Ico(R.drawable.focus_chevron_right_white, 16.dp, C.Text2)
                }
            }
        }

        // Reset card
        Row(Modifier.fillMaxWidth().padding(top = 14.dp).fgSurface(20.dp).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            FgTile(48.dp, 14.dp) { Ico(R.drawable.focus_calendar_white, 26.dp) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Resets at midnight", color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("Your Time Bank will reset in", color = C.Text2, fontSize = 13.sp)
            }
            MidnightCountdown()
        }
        Spacer(Modifier.height(24.dp))
    }
    if (help) AlertDialog(
        onDismissRequest = { help = false },
        confirmButton = { TextButton({ help = false }) { Text("Got it", color = C.White) } },
        containerColor = C.Sheet,
        title = { Text("How the Time Bank works", color = C.White) },
        text = {
            Text("Locked apps stay shut until you earn time. Verified effort (focus sessions, Strava runs, workouts, study) adds minutes. " +
                "Spend them to unlock an app, up to its daily limit. Everything resets at midnight. Missed Accountability Mirror items cost 10 minutes tomorrow. Manual entries never earn time.",
                color = C.Text2, fontSize = 14.sp)
        },
    )
}

/** Live HH : MM : SS to local midnight. Own composable so the per-second tick doesn't recompose the screen. */
@Composable
private fun MidnightCountdown() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = LocalDateTime.now() } }
    val d = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay())
    val h = d.toHours().toInt(); val m = (d.toMinutes() % 60).toInt(); val s = (d.seconds % 60).toInt()
    Row(verticalAlignment = Alignment.Top) {
        CountUnit("%02d".format(h), "HOURS"); Colon(); CountUnit("%02d".format(m), "MINUTES"); Colon(); CountUnit("%02d".format(s), "SECONDS")
    }
}

@Composable
private fun Colon() = Text(":", color = C.White, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 3.dp))

@Composable
private fun CountUnit(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FgGlowText(v, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, tiers = 2)
        Text(l, color = C.Text2, fontSize = 8.sp, letterSpacing = 0.4.sp, maxLines = 1)
    }
}

/** Cylindrical stack of time discs with an up arrow: elliptic contours, inner diffusion, sparse highlights. Pure Canvas. */
@Composable
private fun TimeStackArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val cx = w / 2
        val rx = w * 0.43f
        val ry = rx * 0.30f
        val layerGap = size.height * 0.15f
        val top = size.height * 0.34f
        val line = 1.3.dp.toPx()
        val ys = List(4) { top + it * layerGap }
        val bottom = ys.last() + layerGap * 0.55f

        // inner diffusion behind the middle discs (squashed radial)
        withTransform({ scale(1f, 0.34f, Offset(cx, (ys[1] + ys[2]) / 2)) }) {
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.22f), Color.Transparent), Offset(cx, (ys[1] + ys[2]) / 2), rx), rx, Offset(cx, (ys[1] + ys[2]) / 2))
        }
        // side walls
        drawLine(Color.White.copy(alpha = 0.7f), Offset(cx - rx, ys[0]), Offset(cx - rx, bottom), line)
        drawLine(Color.White.copy(alpha = 0.7f), Offset(cx + rx, ys[0]), Offset(cx + rx, bottom), line)
        // top disc, full ellipse (soft halo pass then crisp)
        drawOval(Color.White.copy(alpha = 0.14f), Offset(cx - rx, ys[0] - ry - 1.5.dp.toPx()), Size(rx * 2, ry * 2 + 3.dp.toPx()), style = Stroke(line * 3))
        drawOval(Color.White.copy(alpha = 0.95f), Offset(cx - rx, ys[0] - ry), Size(rx * 2, ry * 2), style = Stroke(line))
        drawOval(Color.White.copy(alpha = 0.10f), Offset(cx - rx, ys[0] - ry), Size(rx * 2, ry * 2))
        // lower layer seams: front arcs bright, back arcs faint
        for (i in 1 until 4) {
            val tl = Offset(cx - rx, ys[i] - ry)
            val sz = Size(rx * 2, ry * 2)
            drawArc(Color.White.copy(alpha = 0.22f), 180f, 180f, false, tl, sz, style = Stroke(line))
            drawArc(Color.White.copy(alpha = if (i == 2) 1f else 0.8f), 0f, 180f, false, tl, sz, style = Stroke(line * if (i == 2) 1.6f else 1f))
        }
        // base
        drawArc(Color.White.copy(alpha = 0.85f), 0f, 180f, false, Offset(cx - rx, bottom - ry), Size(rx * 2, ry * 2), style = Stroke(line))
        // up arrow
        val ay0 = ys[0] - ry - 6.dp.toPx()
        val ay1 = ay0 - 22.dp.toPx()
        drawLine(Color.White.copy(alpha = 0.18f), Offset(cx, ay0), Offset(cx, ay1), line * 5, StrokeCap.Round)
        drawLine(Color.White, Offset(cx, ay0), Offset(cx, ay1), 2.2.dp.toPx(), StrokeCap.Round)
        val head = Path().apply { moveTo(cx - 6.dp.toPx(), ay1 + 7.dp.toPx()); lineTo(cx, ay1); lineTo(cx + 6.dp.toPx(), ay1 + 7.dp.toPx()) }
        drawPath(head, Color.White, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // sparse highlights
        listOf(0.08f to 0.10f, 0.92f to 0.16f, 0.14f to 0.36f, 0.88f to 0.30f, 0.72f to 0.05f, 0.30f to 0.06f).forEach { (x, y) ->
            drawCircle(Color.White.copy(alpha = 0.55f), 0.9.dp.toPx(), Offset(w * x, size.height * y))
        }
    }
}
