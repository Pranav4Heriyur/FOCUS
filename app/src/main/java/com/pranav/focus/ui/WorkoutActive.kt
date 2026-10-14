package com.pranav.focus.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.R
import com.pranav.focus.data.EffectiveLoad
import com.pranav.focus.data.SetEntry
import com.pranav.focus.data.SetType
import com.pranav.focus.data.Store
import com.pranav.focus.data.WorkoutDb
import com.pranav.focus.data.WorkoutLogic
import com.pranav.focus.lock.AppState
import com.pranav.focus.lock.Earning
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private fun disp(kg: Double, imperial: Boolean) = WorkoutLogic.fmt((WorkoutLogic.toDisplay(kg, imperial) * 10).roundToInt() / 10.0)

private val DashedShape = RoundedCornerShape(16.dp)

/**
 * One set row (C8 table: SET | PREVIOUS | KG | REPS | RPE | check). Weight is stored in kg; the text is only converted back when the user
 * actually edits it (no unit round-trip drift). Warm-up shows a grey "W" badge, a record-beating set shows a star.
 */
@Composable
fun SetRowEditor(
    s: SetEntry, number: Int, prev: Pair<Double, Int>?, imperial: Boolean, showCheck: Boolean, star: Boolean,
    onChange: (Double, Int, Double?, SetType, Boolean) -> Unit, onDelete: () -> Unit,
) {
    val initial = remember(s.id) { disp(s.weightKg, imperial) }
    var kg by remember(s.id) { mutableStateOf(initial) }
    var reps by remember(s.id) { mutableStateOf(s.reps.toString()) }
    var rpe by remember(s.id) { mutableStateOf(s.rpe?.let { WorkoutLogic.fmt(it) } ?: "") }
    var type by remember(s.id) { mutableStateOf(s.type) }
    var done by remember(s.id) { mutableStateOf(s.done || !showCheck) }
    fun push(d: Boolean = done) {
        val w = if (kg == initial) s.weightKg else WorkoutLogic.fromDisplay(kg.toDoubleOrNull() ?: 0.0, imperial)
        onChange(w, reps.toIntOrNull() ?: 0, rpe.toDoubleOrNull()?.takeIf { it in 1.0..10.0 }, type, d)
    }
    val tag = when (type) { SetType.NORMAL -> "$number"; SetType.WARMUP -> "W"; SetType.DROP -> "D"; SetType.FAILURE -> "F" }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.width(40.dp).height(48.dp).clickable {
                type = SetType.values()[(type.ordinal + 1) % SetType.values().size]; push()
            }.semantics { contentDescription = "Set $number type ${type.label}. Tap to change." },
            contentAlignment = Alignment.Center,
        ) {
            if (type == SetType.NORMAL) Text(tag, color = C.White, fontSize = 14.sp)
            else Box(Modifier.clip(RoundedCornerShape(8.dp)).background(C.Track).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(tag, color = C.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            if (star && showCheck) Text("★", color = C.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.TopStart))
        }
        Text(prev?.let { "${disp(it.first, imperial)} x ${it.second}" } ?: "—", color = C.Text3, fontSize = 13.sp, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1.1f))
        WField(kg, { kg = it.filter { c -> c.isDigit() || c == '.' }.take(6); push() }, Modifier.weight(1.1f), if (imperial) "lb" else "kg", desc = "Weight for set $number")
        WField(reps, { reps = it.filter { c -> c.isDigit() }.take(3); push() }, Modifier.weight(0.9f), "reps", decimal = false, desc = "Reps for set $number")
        WField(rpe, { rpe = it.filter { c -> c.isDigit() || c == '.' }.take(3); push() }, Modifier.weight(0.9f), "RPE", desc = "RPE for set $number")
        if (showCheck) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (done) C.White else Color.Transparent).border(1.5.dp, if (done) C.White else C.Text3, CircleShape)
                    .clickable { done = !done; push(done) }.semantics { contentDescription = if (done) "Set $number done. Tap to undo." else "Mark set $number done" },
                contentAlignment = Alignment.Center,
            ) { if (done) Ico(R.drawable.ic_check, 20.dp, C.Black) }
        } else WIconBtn(R.drawable.ic_delete, "Delete set $number", C.Text2, 40.dp) { onDelete() }
    }
}

@Composable
private fun DashedAdd(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(DashedShape).dashedBorder().clickable { onClick() },
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) { Ico(R.drawable.ic_add, 20.dp); Spacer(Modifier.width(8.dp)); Text(label, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
}

private fun Modifier.dashedBorder(): Modifier = drawBehind {
    drawRoundRect(
        Color.White.copy(alpha = 0.35f), cornerRadius = CornerRadius(16.dp.toPx()),
        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))),
    )
}

@Composable
fun ActiveWorkoutScreen(db: WorkoutDb, nav: WNav, onSummary: (SummaryInfo) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    val imperial = remember { store.imperial }
    val view = LocalView.current
    DisposableEffect(Unit) { view.keepScreenOn = true; onDispose { view.keepScreenOn = false } }
    var tick by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    var picker by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var plateFor by remember { mutableStateOf<Double?>(null) }
    var finishing by remember { mutableStateOf(false) }
    val w = remember(tick) { db.activeWorkout() }
    val exMap = remember { db.exercises().associateBy { it.id } }
    if (w == null) { LaunchedEffect(Unit) { nav.pop() }; return }
    if (picker) { ExercisePicker(db, onPick = { ex -> db.addExerciseToWorkout(w.id, ex).also { id -> val last = db.lastSet(ex.id); db.addSet(id, last?.first ?: 0.0, last?.second ?: 8) }; picker = false; tick++ }, onClose = { picker = false }); return }
    val priorBest = remember(w.id) { WorkoutLogic.bestE1rm(db.completedSets(excludeWorkoutId = w.id)) }
    val prevCache = remember(w.id, w.exercises.size) { w.exercises.associate { it.exerciseId to db.previousSets(it.exerciseId, w.id) } }

    val restEnd = store.str("wk_rest_end", "0").toLongOrNull() ?: 0L
    val restLeft = (restEnd - now) / 1000
    fun startRest(sec: Int) { store.putStr("wk_rest_end", (System.currentTimeMillis() + sec * 1000L).toString()); now = System.currentTimeMillis() }
    val doneSets = w.exercises.sumOf { e -> e.sets.count { it.done } }
    val totalSets = w.exercises.sumOf { it.sets.size }
    val exercisesStarted = w.exercises.count { e -> e.sets.any { it.done } }
    val minutes = ((now - w.startTs) / 60_000).toInt()
    val volKg = w.exercises.sumOf { e -> e.sets.filter { it.done && it.type != SetType.WARMUP }.sumOf { it.weightKg * it.reps } }

    fun finish() {
        if (finishing) return
        finishing = true
        val names = w.exercises.associate { it.exerciseId to it.name }
        val done = w.exercises.flatMap { e -> e.sets.filter { it.done }.map { WorkoutLogic.SetLike(e.exerciseId, it.weightKg, it.reps, it.rpe, it.type, it.ts) } }
        val working = done.count { it.type != SetType.WARMUP }
        val mins = ((System.currentTimeMillis() - w.startTs) / 60_000).toInt()
        val verified = WorkoutLogic.isVerified(working, mins)
        val prs = WorkoutLogic.detectPrs(names, done, db.completedSets(excludeWorkoutId = w.id))
        val volume = done.filter { it.type != SetType.WARMUP }.sumOf { it.weightKg * it.reps }
        if (!db.finishWorkout(w.id, verified, mins, null)) { store.putStr("wk_rest_end", "0"); nav.pop(); return }
        var xp = 5
        if (verified) {
            val d = store.today(); store.putInt("workouts_$d", store.int("workouts_$d") + 1)
            Earning.evaluate(ctx); AppState.dataTick++
            xp = WorkoutLogic.xpFor(prs.size)
        }
        store.awardXp("workout:${w.id}", "workout", xp)
        store.putStr("wk_rest_end", "0")
        onSummary(SummaryInfo(w.id, w.name, mins, working, volume, prs.map { "${it.exerciseName} · ${it.kind} ${WorkoutLogic.fmt((WorkoutLogic.toDisplay(it.value, imperial) * 10).roundToInt() / 10.0)}" }, verified, xp, w.templateId))
        nav.replaceTop(WRoute.Summary)
    }

    if (confirmFinish) {
        val working = w.exercises.sumOf { e -> e.sets.count { it.done && it.type != SetType.WARMUP } }
        WConfirm(
            "Finish workout?",
            "$doneSets sets logged in $minutes min." + if (!WorkoutLogic.isVerified(working, minutes)) " This won't count as a verified workout (needs 5 working sets and 20 minutes), so it earns no Time Bank minutes. Unticked sets are discarded." else " Unticked sets are discarded.",
            "Finish", { confirmFinish = false; finish() }, { confirmFinish = false },
        )
    }
    if (confirmDiscard) WConfirm("Discard workout?", "Everything logged in this session will be deleted.", "Discard", { db.discardWorkout(w.id); store.putStr("wk_rest_end", "0"); nav.pop() }, { confirmDiscard = false })
    plateFor?.let { PlateDialog(it, imperial, store) { plateFor = null } }

    val focusMuscles = w.exercises.flatMap { e -> exMap[e.exerciseId]?.credits?.filter { it.value >= 1.0 }?.keys.orEmpty() }.mapNotNull { com.pranav.focus.data.MuscleGroup.fromKey(it)?.label }.distinct().take(3).joinToString(" • ")

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = if (restLeft > 0) 190.dp else 120.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                WIconBtn(R.drawable.ic_arrow_back, "Back") { nav.pop() }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.height(44.dp).clip(CircleShape).border(1.dp, C.Hairline, CircleShape).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text("Load ${WorkoutLogic.fmt((WorkoutLogic.toDisplay(volKg, imperial) / 1000.0 * 100).roundToInt() / 100.0)} ${if (imperial) "k lb" else "t"}", color = C.White, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.height(44.dp).clip(CircleShape).background(if (doneSets > 0 && !finishing) C.White else C.Track).clickable(enabled = doneSets > 0 && !finishing) { confirmFinish = true }.padding(horizontal = 22.dp)
                        .semantics { contentDescription = "Finish workout" },
                    contentAlignment = Alignment.Center,
                ) { Text("Finish", color = if (doneSets > 0) C.Black else C.Text3, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
            Text(w.name, color = C.White, fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            if (focusMuscles.isNotEmpty()) Text(focusMuscles, color = C.Text2, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            FocusCard(Modifier.fillMaxWidth(), radius = 20.dp, padding = 12.dp) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    StatCell(mmss((now - w.startTs) / 1000), "Elapsed")
                    StatCell("$exercisesStarted / ${w.exercises.size}", "Exercises")
                    StatCell("$doneSets / $totalSets", "Total sets")
                }
            }
            Spacer(Modifier.height(12.dp))
            if (w.exercises.isEmpty()) WEmpty("No exercises yet", "Add an exercise to start logging sets.")
            w.exercises.forEachIndexed { idx, e ->
                val ex = exMap[e.exerciseId]
                val prevs = prevCache[e.exerciseId].orEmpty()
                FocusCard(Modifier.fillMaxWidth().padding(bottom = 12.dp), padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${idx + 1}. ${e.name}", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            val tags = ex?.credits?.entries?.sortedByDescending { it.value }?.mapNotNull { com.pranav.focus.data.MuscleGroup.fromKey(it.key)?.label }?.distinct()?.take(3).orEmpty()
                            if (tags.isNotEmpty()) Text(tags.joinToString(" • "), color = C.Text2, fontSize = 12.sp)
                        }
                        if (ex?.equipment == "Barbell") WChip("Plates", false) { plateFor = e.sets.lastOrNull()?.weightKg ?: 0.0 }
                        WIconBtn(R.drawable.ic_close, "Remove ${e.name}", C.Text2) { db.removeExerciseFromWorkout(e.id); tick++ }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(10.dp)).background(C.Raised).padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Caps("Set", Modifier.width(40.dp)); Caps("Previous", Modifier.weight(1.1f)); Caps(if (imperial) "lb" else "kg", Modifier.weight(1.1f)); Caps("Reps", Modifier.weight(0.9f)); Caps("RPE", Modifier.weight(0.9f)); Spacer(Modifier.width(40.dp))
                    }
                    var workingNo = 0
                    e.sets.forEachIndexed { i, s ->
                        if (s.type != SetType.WARMUP) workingNo++
                        val own = EffectiveLoad.e1rm(s.weightKg, s.reps, EffectiveLoad.rirFrom(s.rpe, s.type.code))
                        val pb = priorBest[e.exerciseId]
                        val star = s.done && s.type != SetType.WARMUP && own != null && pb != null && own > pb + 0.01
                        SetRowEditor(s, if (s.type == SetType.WARMUP) i + 1 else workingNo, prevs.getOrNull(i), imperial, showCheck = true, star = star,
                            onChange = { kg, reps, rpe, type, done ->
                                db.updateSet(s.id, kg, reps, rpe, type, done)
                                if (done != s.done) { if (done) startRest(WorkoutLogic.restSuggestSeconds(reps, type)); tick++ }
                            },
                            onDelete = { db.deleteSet(s.id); tick++ })
                    }
                    Spacer(Modifier.height(10.dp))
                    DashedAdd("Add set") {
                        val last = e.sets.lastOrNull(); db.addSet(e.id, last?.weightKg ?: db.lastSet(e.exerciseId)?.first ?: 0.0, last?.reps ?: 8); tick++
                    }
                }
            }
            DashedAdd("Add exercise") { picker = true }
            Spacer(Modifier.height(12.dp))
            GhostButton("Discard workout", icon = R.drawable.ic_delete) { confirmDiscard = true }
        }
        if (restLeft > 0) {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp).fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(C.Raised.copy(alpha = 0.96f))
                    .border(1.dp, C.Hairline, RoundedCornerShape(36.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WChip("−15s", false) { store.putStr("wk_rest_end", (restEnd - 15_000).toString()); now = System.currentTimeMillis() }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rest timer", color = C.Text2, fontSize = 12.sp)
                    Text(mmss(restLeft), color = C.White, fontSize = 32.sp, fontWeight = FontWeight.Light, modifier = Modifier.semantics { contentDescription = "Rest ${mmss(restLeft)} remaining" })
                }
                WChip("+15s", false) { store.putStr("wk_rest_end", (restEnd + 15_000).toString()); now = System.currentTimeMillis() }
                Spacer(Modifier.width(6.dp))
                WChip("Skip", true) { store.putStr("wk_rest_end", "0"); now = System.currentTimeMillis() }
            }
        }
    }
}

@Composable
private fun PlateDialog(startKg: Double, imperial: Boolean, store: Store, onClose: () -> Unit) {
    val bar0 = WorkoutLogic.toDisplay(store.str("wk_bar", "20").toDoubleOrNull() ?: 20.0, imperial)
    var target by remember { mutableStateOf(WorkoutLogic.fmt((WorkoutLogic.toDisplay(startKg, imperial) * 10).roundToInt() / 10.0)) }
    val inv = if (imperial) listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5) else listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val t = target.toDoubleOrNull() ?: 0.0
    val (plates, rem) = WorkoutLogic.plates(t, bar0, inv)
    val unit = if (imperial) "lb" else "kg"
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onClose, containerColor = C.Sheet, titleContentColor = C.White, textContentColor = C.Text2,
        title = { Text("Plate calculator", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text("Target weight ($unit) · bar ${WorkoutLogic.fmt(bar0)} $unit", color = C.Text2, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                WField(target, { target = it.filter { c -> c.isDigit() || c == '.' }.take(6) }, Modifier.fillMaxWidth(), unit)
                Spacer(Modifier.height(12.dp))
                if (t <= bar0) Text("Just the bar.", color = C.White)
                else {
                    val side = plates.sum()
                    Text("Per side: " + plates.joinToString(" + ") { WorkoutLogic.fmt(it) } + " = ${WorkoutLogic.fmt(side)} $unit", color = C.White, fontSize = 16.sp)
                    Text("Total loaded: ${WorkoutLogic.fmt(bar0 + side * 2)} $unit", color = C.Text2, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    if (rem > 0) Text("Can't make exact weight: ${WorkoutLogic.fmt(rem * 2)} $unit short with these plates.", color = C.Text2, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        },
        confirmButton = { Text("Close", color = C.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onClose() }.padding(12.dp)) },
    )
}

// ---------------------------------------------------------------------------------- summary / PR
@Composable
fun SummaryScreen(db: WorkoutDb, nav: WNav, info: SummaryInfo?) {
    val ctx = LocalContext.current
    val imperial = remember { Store.get(ctx).imperial }
    if (info == null) { LaunchedEffect(Unit) { nav.toHub() }; return }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("Workout complete", null, info.name)
        if (info.prs.isNotEmpty()) {
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).bloom(12.dp, 0.25f).clip(CircleShape).border(2.dp, C.White, CircleShape), contentAlignment = Alignment.Center) { Text("PR", color = C.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
                    Spacer(Modifier.width(16.dp))
                    Column { Text("New personal record${if (info.prs.size > 1) "s" else ""}", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Medium); info.prs.forEach { Text(it, color = C.Text2, fontSize = 14.sp) } }
                }
            }
        }
        FocusCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell("${info.minutes}", "Minutes"); StatCell("${info.sets}", "Working sets")
                StatCell(WorkoutLogic.fmt((WorkoutLogic.toDisplay(info.volumeKg, imperial)).roundToInt().toDouble()), "Volume ${if (imperial) "lb" else "kg"}")
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (info.verified) "Verified workout · +${info.xp} XP · Time Bank updated" else "Saved as unverified (needs 5 working sets and 20 min) · +${info.xp} XP, no Time Bank minutes",
            color = C.Text2, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (info.templateId != null && db.template(info.templateId) != null) {
            PrimaryButton("Repeat workout") { nav.toHub(); startFromTemplate(db, nav, info.templateId) }
            Spacer(Modifier.height(8.dp))
            GhostButton("Edit workout template") { nav.toHub(); nav.push(WRoute.TemplateEdit(info.templateId)) }
            Spacer(Modifier.height(8.dp))
            GhostButton("Back to Workout hub") { nav.toHub() }
        } else PrimaryButton("Back to Workout hub") { nav.toHub() }
    }
}
