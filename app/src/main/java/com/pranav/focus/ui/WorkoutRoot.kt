package com.pranav.focus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.R
import com.pranav.focus.data.AppClock
import com.pranav.focus.data.EffectiveLoad
import com.pranav.focus.data.RoutineStatus
import com.pranav.focus.data.Store
import com.pranav.focus.data.WorkoutDb
import com.pranav.focus.data.WorkoutLogic

sealed class WRoute {
    object Hub : WRoute()
    object Templates : WRoute()
    data class TemplateEdit(val id: Long) : WRoute()
    object Routines : WRoute()
    data class RoutineEdit(val id: Long) : WRoute()
    object Active : WRoute()
    object Summary : WRoute()
    object History : WRoute()
    data class Past(val id: Long) : WRoute()
    object Quick : WRoute()
    object Balance : WRoute()
    object Progress : WRoute()
    object Library : WRoute()
    object Settings : WRoute()
}

data class SummaryInfo(val workoutId: Long, val name: String, val minutes: Int, val sets: Int, val volumeKg: Double, val prs: List<String>, val verified: Boolean, val xp: Int, val templateId: Long?)

class WNav(val push: (WRoute) -> Unit, val pop: () -> Unit, val replaceTop: (WRoute) -> Unit, val bump: () -> Unit, val toHub: () -> Unit)

/** Workout tab host: internal back stack. An unfinished session is resumed after process death because every set is persisted as it is edited. */
@Composable
fun WorkoutRoot(back: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { WorkoutDb.get(ctx) }
    val stack = remember { mutableStateListOf<WRoute>(WRoute.Hub) }
    var tick by remember { mutableIntStateOf(0) }
    var summary by remember { mutableStateOf<SummaryInfo?>(null) }
    LaunchedEffect(Unit) { if (db.activeWorkout() != null && stack.size == 1) stack.add(WRoute.Active) }
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex); tick++ }
    val nav = remember {
        WNav(
            push = { stack.add(it) },
            pop = { if (stack.size > 1) stack.removeAt(stack.lastIndex) else back(); tick++ },
            replaceTop = { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex); stack.add(it) },
            bump = { tick++ },
            toHub = { while (stack.size > 1) stack.removeAt(stack.lastIndex); tick++ },
        )
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        when (val r = stack.last()) {
            WRoute.Hub -> WorkoutHub(db, nav, tick)
            WRoute.Templates -> TemplateListScreen(db, nav, tick)
            is WRoute.TemplateEdit -> TemplateEditScreen(db, nav, r.id) { id -> startFromTemplate(db, nav, id) }
            WRoute.Routines -> RoutineListScreen(db, nav, tick)
            is WRoute.RoutineEdit -> RoutineEditScreen(db, nav, r.id)
            WRoute.Active -> ActiveWorkoutScreen(db, nav) { s -> summary = s }
            WRoute.Summary -> SummaryScreen(db, nav, summary)
            WRoute.History -> HistoryScreen(db, nav, tick)
            is WRoute.Past -> PastWorkoutScreen(db, nav, r.id)
            WRoute.Quick -> QuickLogScreen(db, nav)
            WRoute.Balance -> BalanceScreen(db, nav)
            WRoute.Progress -> ProgressScreen(db, nav)
            WRoute.Library -> LibraryScreen(db, nav)
            WRoute.Settings -> WorkoutSettingsScreen(nav)
        }
    }
}

fun startFromTemplate(db: WorkoutDb, nav: WNav, templateId: Long?, routineDayId: Long? = null) {
    if (db.activeWorkout() != null) { nav.push(WRoute.Active); return }
    val t = templateId?.let { db.template(it) }
    db.startWorkout(t?.name ?: "Workout", t, routineDayId)
    nav.push(WRoute.Active)
}

@Composable
private fun StatTile(label: String, value: String, sub: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FocusCard(modifier, onClick = onClick, radius = 20.dp, padding = 12.dp) {
        Text(label, color = C.Text2, fontSize = 12.sp, maxLines = 1)
        Text(value, color = C.White, fontSize = 28.sp, fontWeight = FontWeight.Light, maxLines = 1)
        Text(sub, color = C.Text2, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun WeekRing(frac: Float?, center: String, caption: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(92.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val st = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                drawArc(Color(0xFF2A2A2D), 0f, 360f, false, style = st)
                if (frac != null && frac > 0f) drawArc(Color.White, -90f, 360f * frac.coerceIn(0f, 1f), false, style = st)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(center, color = C.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                Text("This week", color = C.Text2, fontSize = 10.sp)
            }
        }
        Text(caption, color = C.Text2, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

/** C0 hub: today hero + ring, three stat tiles, My plans carousel, Recent workouts. Everything is derived from saved sessions. */
@Composable
private fun WorkoutHub(db: WorkoutDb, nav: WNav, tick: Int) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val imperial = remember { store.imperial }
    val active = remember(tick) { db.activeWorkout() }
    val routine = remember(tick) { db.activeRoutine() }
    val routines = remember(tick) { db.routines().filter { it.status != RoutineStatus.ARCHIVED } }
    val templates = remember(tick) { db.templates() }
    val history = remember(tick) { db.history() }
    val weekStart = remember(tick) { AppClock.today().minusDays(6).atStartOfDay(AppClock.zone()).toInstant().toEpochMilli() }
    val week = remember(tick) { history.filter { it.startTs >= weekStart } }
    val weekVolKg = remember(tick) { db.completedSets(sinceTs = weekStart).filter { it.type != com.pranav.focus.data.SetType.WARMUP }.sumOf { it.weightKg * it.reps } }
    val form = remember(tick) {
        val loads = db.dailyLoads()
        if (loads.isEmpty()) null else {
            val start = loads.keys.minOf { java.time.LocalDate.parse(it) }
            val n = java.time.temporal.ChronoUnit.DAYS.between(start, AppClock.today()).toInt() + 1
            EffectiveLoad.fitnessFatigue((0 until n).map { loads[start.plusDays(it.toLong()).toString()] ?: 0.0 }).last().form
        }
    }
    val exMap = remember { db.exercises().associateBy { it.id } }
    val today = AppClock.today()
    val planned = remember(tick, routine) { routine?.let { r -> WorkoutLogic.plannedFor(r.days, today.dayOfWeek.value, db.sessionsForRoutine(r.id)) } }
    val tpl = planned?.templateId?.let { db.template(it) }
    val plannedPerWeek = routine?.days?.count { !it.rest } ?: 0
    val ringFrac = if (routine != null && plannedPerWeek > 0) week.count { !it.quick }.toFloat() / plannedPerWeek else null
    val focus = tpl?.exercises?.flatMap { e -> exMap[e.exerciseId]?.credits?.filter { it.value >= 1.0 }?.keys.orEmpty() }?.mapNotNull { com.pranav.focus.data.MuscleGroup.fromKey(it)?.label }?.distinct()?.take(3)?.joinToString(" • ")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("Workout", null, "Stronger habits. A stronger you.")
        if (active != null) {
            FocusCard(Modifier.fillMaxWidth(), radius = 28.dp, onClick = { nav.push(WRoute.Active) }) {
                Caps("Workout in progress")
                Text(active.name, color = C.White, fontSize = 22.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 4.dp))
                Text("${active.exercises.sumOf { e -> e.sets.count { it.done } }} sets logged · tap to resume", color = C.Text2, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
        }
        FocusCard(Modifier.fillMaxWidth(), radius = 28.dp) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Caps(if (routine != null) "Today · ${routine.name}" else "Today")
                    Text(
                        when { planned == null && routine != null -> "Rest day"; planned != null -> planned.label.ifBlank { tpl?.name ?: "Training day" }; else -> "Free session" },
                        color = C.White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp),
                    )
                    Text(focus ?: if (routine == null) "Build a routine or start empty" else if (planned == null) "Nothing scheduled — start anything you like" else "No workout assigned to this day", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    if (tpl != null) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Ico(R.drawable.p1_dumbbell, 18.dp, C.Text2); Spacer(Modifier.width(6.dp)); Text("${tpl.exercises.size} exercises", color = C.Text2, fontSize = 13.sp) }
                    }
                }
                if (routine != null) WeekRing(ringFrac, ringFrac?.let { "${(it.coerceAtMost(1f) * 100).toInt()}%" } ?: "—", "${week.count { !it.quick }} / $plannedPerWeek sessions")
                else WeekRing(null, "${week.size}", "sessions")
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(52.dp).clip(CircleShape).background(if (active != null || tpl != null) C.White else C.Track)
                        .clickable(enabled = active != null || tpl != null) { if (active != null) nav.push(WRoute.Active) else { startFromTemplate(db, nav, tpl!!.id, planned!!.id); nav.bump() } },
                    contentAlignment = Alignment.Center,
                ) { Text(if (active != null) "Resume" else "Start workout", color = if (active != null || tpl != null) C.Black else C.Text3, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                Box(
                    Modifier.weight(1f).height(52.dp).clip(CircleShape).border(1.dp, C.Text3, CircleShape)
                        .clickable { if (db.activeWorkout() == null) db.startWorkout("Workout", null, null); nav.push(WRoute.Active) },
                    contentAlignment = Alignment.Center,
                ) { Text("Empty workout", color = C.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("This week", "${week.size}", if (week.size == 1) "session" else "sessions", { nav.push(WRoute.History) }, Modifier.weight(1f))
            StatTile("Load", WorkoutLogic.fmt((WorkoutLogic.toDisplay(weekVolKg, imperial) / 1000.0 * 10).roundToInt() / 10.0), if (imperial) "k lb · 7 days" else "tonnes · 7 days", { nav.push(WRoute.Progress) }, Modifier.weight(1f))
            StatTile("Form", form?.let { (if (it >= 0) "+" else "") + it.roundToInt() } ?: "—", form?.let { if (it >= 5) "Fresh" else if (it <= -5) "Fatigued" else "Neutral" } ?: "No data", { nav.push(WRoute.Progress) }, Modifier.weight(1f))
        }
        WSection("My plans", "View all") { nav.push(WRoute.Routines) }
        if (routines.isEmpty() && templates.isEmpty()) WEmpty("No plans yet", "Create a workout or a weekly routine, any split you like.", "Create routine") { nav.push(WRoute.RoutineEdit(0)) }
        else LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(routines, key = { "r${it.id}" }) { r ->
                val sel = r.status == RoutineStatus.ACTIVE
                Column(
                    Modifier.width(150.dp).height(112.dp).clip(RoundedCornerShape(20.dp)).background(C.Card).border(if (sel) 1.5.dp else 1.dp, if (sel) C.White else C.Hairline, RoundedCornerShape(20.dp))
                        .clickable { nav.push(WRoute.RoutineEdit(r.id)) }.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(if (sel) "ACTIVE" else r.status.code.uppercase(), color = C.Text2, fontSize = 10.sp, letterSpacing = 1.2.sp)
                    Column {
                        Text(r.name, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                        Text("${r.days.count { !it.rest }} days" + (r.weeks?.let { " · $it wk" } ?: ""), color = C.Text2, fontSize = 12.sp)
                    }
                }
            }
            items(templates.take(8), key = { "t${it.id}" }) { t ->
                Column(
                    Modifier.width(150.dp).height(112.dp).clip(RoundedCornerShape(20.dp)).background(C.Card).border(1.dp, C.Hairline, RoundedCornerShape(20.dp))
                        .clickable { startFromTemplate(db, nav, t.id); nav.bump() }.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("WORKOUT", color = C.Text2, fontSize = 10.sp, letterSpacing = 1.2.sp)
                    Column {
                        Text(t.name, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                        Text("${t.exercises.size} exercises · tap to start", color = C.Text2, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Create workout", Modifier.weight(1f)) { nav.push(WRoute.TemplateEdit(0)) }
            GhostButton("Create routine", Modifier.weight(1f)) { nav.push(WRoute.RoutineEdit(0)) }
        }
        WSection("Recent workouts", "+ Quick log") { nav.push(WRoute.Quick) }
        if (history.isEmpty()) WEmpty("Nothing logged yet", "Finish a workout and it shows up here with its volume.")
        history.take(3).forEach { w ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = { nav.push(WRoute.Past(w.id)) }, radius = 20.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(w.name, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(wkDate(w.startTs) + " · ${w.minutes} min", color = C.Text2, fontSize = 13.sp)
                    }
                    val vol = w.exercises.sumOf { e -> e.sets.filter { it.done && it.type != com.pranav.focus.data.SetType.WARMUP }.sumOf { it.weightKg * it.reps } }
                    Column(horizontalAlignment = Alignment.End) {
                        if (w.quick) Text("${w.lu.toInt()} LU", color = C.White, fontSize = 14.sp) else Text("${WorkoutLogic.fmt(WorkoutLogic.toDisplay(vol, imperial).roundToInt().toDouble())} ${if (imperial) "lb" else "kg"}", color = C.White, fontSize = 14.sp)
                        Text(if (w.quick) "Quick log" else "Volume", color = C.Text2, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    Ico(R.drawable.ic_chevron_right, 20.dp, C.Text3)
                }
            }
        }
        WSection("More")
        listOf(
            Triple("Muscle balance", "Weekly hard sets per muscle", WRoute.Balance), Triple("Progress & PRs", "Fitness, fatigue and best lifts", WRoute.Progress),
            Triple("Exercise library", "Built-in and custom exercises", WRoute.Library), Triple("History", "Every logged session", WRoute.History),
            Triple("Workout settings", "Units and barbell weight", WRoute.Settings),
        ).forEach { (t, s2, r) ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = { nav.push(r) }, radius = 20.dp, padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(t, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.Medium); Text(s2, color = C.Text2, fontSize = 13.sp) }
                    Ico(R.drawable.ic_chevron_right, 20.dp, C.Text3)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------- history / past
@Composable
private fun HistoryScreen(db: WorkoutDb, nav: WNav, tick: Int) {
    val list = remember(tick) { db.history() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("History", { nav.pop() }, "${list.size} saved")
        if (list.isEmpty()) WEmpty("Nothing logged yet", "Finished workouts and quick logs show up here.")
        list.forEach { w ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = { nav.push(WRoute.Past(w.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(w.name, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        val sets = w.exercises.sumOf { e -> e.sets.count { it.done } }
                        Text(wkDate(w.startTs) + " · " + if (w.quick) "${w.minutes} min · ${w.lu.toInt()} LU" else "$sets sets · ${w.minutes} min", color = C.Text2, fontSize = 13.sp)
                    }
                    if (!w.verified) Pill(if (w.quick) "Quick" else "Unverified")
                }
            }
        }
    }
}

@Composable
private fun PastWorkoutScreen(db: WorkoutDb, nav: WNav, id: Long) {
    val ctx = LocalContext.current
    val imperial = remember { Store.get(ctx).imperial }
    var tick by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    val w = remember(tick) { db.workout(id) }
    if (w == null) { LaunchedEffect(Unit) { nav.pop() }; return }
    if (confirmDelete) WConfirm("Delete this workout?", "It will be removed from history and from your muscle and PR stats.", "Delete", { db.deleteWorkout(id); nav.pop() }, { confirmDelete = false })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader(w.name, { nav.pop() }, wkDate(w.startTs) + " · ${w.minutes} min" + if (w.verified) "" else " · unverified")
        if (w.quick) FocusCard(Modifier.fillMaxWidth()) { Text("${w.lu.toInt()} load units", color = C.White, fontSize = 20.sp); Text("Effort ${w.rpe?.let { WorkoutLogic.fmt(it) } ?: "—"}/10 × ${w.minutes} min. Quick logs earn no Time Bank minutes.", color = C.Text2, fontSize = 13.sp) }
        else {
            Text("Edits are allowed and mark the workout unverified.", color = C.Text2, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            w.exercises.forEach { e ->
                FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(e.name, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                    e.sets.forEachIndexed { i, s ->
                        SetRowEditor(s, i + 1, null, imperial, showCheck = false, star = false, onChange = { kg, reps, rpe, type, done -> db.updateSet(s.id, kg, reps, rpe, type, done); db.markEdited(w.id); tick++ }, onDelete = { db.deleteSet(s.id); db.markEdited(w.id); tick++ })
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        GhostButton("Delete workout", icon = R.drawable.ic_delete) { confirmDelete = true }
    }
}

// ---------------------------------------------------------------------------------- quick log
@Composable
private fun QuickLogScreen(db: WorkoutDb, nav: WNav) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var name by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("") }
    var effort by remember { mutableStateOf(6) }
    val mins = minutes.toIntOrNull()
    val valid = name.isNotBlank() && mins != null && mins in 1..600
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("Quick log", { nav.pop() }, "Runs, sports, classes — anything that isn't a lift")
        Text("Activity", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        WField(name, { name = it }, Modifier.fillMaxWidth(), "e.g. Cricket practice", numeric = false, align = androidx.compose.ui.text.style.TextAlign.Start)
        Text("Duration (minutes)", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp))
        WField(minutes, { minutes = it.filter { c -> c.isDigit() }.take(3) }, Modifier.fillMaxWidth(), "45", decimal = false)
        Text("How hard was it? (1–10)", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) { (1..10).forEach { n -> WChip("$n", effort == n) { effort = n } } }
        if (valid) Text("≈ ${EffectiveLoad.sessionLoad(mins!!.toDouble(), rpe = effort.toDouble()).toInt()} load units", color = C.White, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp))
        Text("Manual logs are unverified: no Time Bank minutes.", color = C.Text3, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        PrimaryButton("Save activity", enabled = valid) {
            db.saveQuick(name.trim(), mins!!, effort.toDouble(), System.currentTimeMillis() - mins * 60_000L)
            store.awardXp("quick:${System.currentTimeMillis()}", "quicklog", 5)
            nav.pop()
        }
    }
}

// ---------------------------------------------------------------------------------- settings
@Composable
private fun WorkoutSettingsScreen(nav: WNav) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var imperial by remember { mutableStateOf(store.imperial) }
    var bar by remember { mutableStateOf(WorkoutLogic.fmt(WorkoutLogic.toDisplay(store.str("wk_bar", "20").toDoubleOrNull() ?: 20.0, imperial))) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("Workout settings", { nav.pop() })
        FocusCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Use pounds", color = C.White, fontSize = 16.sp); Text("Stored values stay in kg", color = C.Text2, fontSize = 13.sp) }
                FocusSwitch(imperial) { imperial = it; store.imperial = it }
            }
        }
        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Text("Barbell weight (${if (imperial) "lb" else "kg"})", color = C.White, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            WField(bar, { v -> bar = v; v.toDoubleOrNull()?.takeIf { it in 0.0..100.0 }?.let { store.putStr("wk_bar", WorkoutLogic.fromDisplay(it, imperial).toString()) } }, Modifier.fillMaxWidth(), "20")
        }
    }
}
