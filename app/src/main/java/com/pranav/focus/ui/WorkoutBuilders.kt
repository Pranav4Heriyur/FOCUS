@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pranav.focus.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.R
import com.pranav.focus.data.AppClock
import com.pranav.focus.data.Exercise
import com.pranav.focus.data.MuscleGroup
import com.pranav.focus.data.RoutineDay
import com.pranav.focus.data.RoutineStatus
import com.pranav.focus.data.Store
import com.pranav.focus.data.TemplateExercise
import com.pranav.focus.data.WorkoutDb
import com.pranav.focus.data.WorkoutLogic
import com.pranav.focus.data.WorkoutRoutine
import com.pranav.focus.data.WorkoutTemplate
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val MUSCLE_KEYS = listOf("chest", "front_delts", "side_delts", "rear_delts", "biceps", "triceps", "forearms", "traps", "upper_back", "lats", "lower_back", "core", "obliques", "quads", "hamstrings", "glutes", "calves", "adductors", "hip_flexors")
private fun muscleLabel(k: String) = MuscleGroup.fromKey(k)?.label ?: k

private fun exerciseMuscles(e: Exercise) = e.credits.entries.sortedByDescending { it.value }.joinToString(", ") { muscleLabel(it.key) }

// ---------------------------------------------------------------------------------- exercise picker / library
@Composable
fun ExercisePicker(db: WorkoutDb, onPick: (Exercise) -> Unit, onClose: () -> Unit, title: String = "Add exercise", browseOnly: Boolean = false) {
    var q by remember { mutableStateOf("") }
    var tick by remember { mutableIntStateOf(0) }
    var creating by remember { mutableStateOf(false) }
    val all = remember(tick) { db.exercises() }
    val list = remember(q, all) { all.filter { q.isBlank() || it.name.contains(q.trim(), ignoreCase = true) || it.equipment.contains(q.trim(), ignoreCase = true) } }
    BackHandler { if (creating) creating = false else onClose() }
    if (creating) {
        CustomExerciseForm(db, onCancel = { creating = false }, onSaved = { ex -> creating = false; tick++; if (!browseOnly) onPick(ex) })
        return
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title, onClose, "${all.size} exercises · ${all.count { it.custom }} custom")
        WField(q, { q = it }, Modifier.fillMaxWidth(), "Search exercises or equipment", numeric = false, align = TextAlign.Start, desc = "Search exercises")
        Spacer(Modifier.height(8.dp))
        GhostButton("Create custom exercise", icon = R.drawable.ic_add) { creating = true }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
            if (list.isEmpty()) WEmpty("No match", "Try another name, or create it as a custom exercise.", "Create \"${q.trim()}\"".takeIf { q.isNotBlank() }) { creating = true }
            list.forEach { e ->
                FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), onClick = if (browseOnly) null else ({ onPick(e) }), padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("${e.equipment} · ${exerciseMuscles(e)}", color = C.Text2, fontSize = 12.sp)
                        }
                        if (e.custom) Pill("Custom")
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomExerciseForm(db: WorkoutDb, onCancel: () -> Unit, onSaved: (Exercise) -> Unit) {
    var name by remember { mutableStateOf("") }
    var equip by remember { mutableStateOf("Barbell") }
    var primary by remember { mutableStateOf<String?>(null) }
    val secondary = remember { mutableStateListOf<String>() }
    val valid = name.isNotBlank() && primary != null
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("Custom exercise", onCancel, "Pick the main muscle. Secondary muscles get half credit in weekly sets.")
        WField(name, { name = it.take(60) }, Modifier.fillMaxWidth(), "Exercise name", numeric = false, align = TextAlign.Start)
        Text("Equipment", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Other").forEach { WChip(it, equip == it) { equip = it } }
        }
        Text("Primary muscle", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MUSCLE_KEYS.forEach { k -> WChip(muscleLabel(k), primary == k) { primary = k; secondary.remove(k) } }
        }
        Text("Secondary muscles (optional)", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MUSCLE_KEYS.filter { it != primary }.forEach { k -> WChip(muscleLabel(k), k in secondary) { if (k in secondary) secondary.remove(k) else secondary.add(k) } }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Save exercise", enabled = valid) { onSaved(db.addCustomExercise(name, equip, primary!!, secondary.toList())) }
    }
}

@Composable
fun LibraryScreen(db: WorkoutDb, nav: WNav) = ExercisePicker(db, onPick = {}, onClose = { nav.pop() }, title = "Exercise library", browseOnly = true)

// ---------------------------------------------------------------------------------- templates
@Composable
fun TemplateListScreen(db: WorkoutDb, nav: WNav, tick: Int) {
    var t2 by remember { mutableIntStateOf(0) }
    var del by remember { mutableStateOf<WorkoutTemplate?>(null) }
    val list = remember(tick, t2) { db.templates() }
    del?.let { t ->
        val used = db.routinesUsingTemplate(t.id)
        if (used.isEmpty()) WConfirm("Delete \"${t.name}\"?", "Past sessions keep their own history.", "Delete", { db.deleteTemplate(t.id); del = null; t2++ }, { del = null })
        else WConfirm(
            "Used by ${used.joinToString(", ")}", "Deleting removes it from those routine days (they'll show as unassigned). Archive keeps routines intact and just hides it.",
            "Archive", { db.archiveTemplate(t.id); del = null; t2++ }, { del = null }, extraLabel = "Delete anyway", onExtra = { db.deleteTemplate(t.id); del = null; t2++ },
        )
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("Saved workouts", { nav.pop() }, "${list.size} templates")
        PrimaryButton("Create workout") { nav.push(WRoute.TemplateEdit(0)) }
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) WEmpty("No saved workouts", "Create one to reuse it in routines or start it any time.")
        list.forEach { t ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(t.name, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text("${t.exercises.size} exercises · ${t.exercises.sumOf { it.targetSets }} sets", color = C.Text2, fontSize = 13.sp)
                if (t.exercises.isNotEmpty()) Text(t.exercises.take(4).joinToString(" · ") { it.name }, color = C.Text3, fontSize = 12.sp, maxLines = 2, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WChip("Start", true) { startFromTemplate(db, nav, t.id) }
                    WChip("Edit", false) { nav.push(WRoute.TemplateEdit(t.id)) }
                    WChip("Duplicate", false) { db.duplicateTemplate(t.id); t2++ }
                    WChip("Delete", false) { del = t }
                }
            }
        }
    }
}

private var tempId = -1L
private fun newTempId(): Long { tempId -= 1; return tempId }

@Composable
fun TemplateEditScreen(db: WorkoutDb, nav: WNav, id: Long, onStart: (Long) -> Unit) {
    val ctx = LocalContext.current
    val imperial = remember { Store.get(ctx).imperial }
    val loaded = remember(id) { if (id == 0L) null else db.template(id) }
    var name by remember(id) { mutableStateOf(loaded?.name ?: "") }
    var notes by remember(id) { mutableStateOf(loaded?.notes ?: "") }
    val items = remember(id) { mutableStateListOf<TemplateExercise>().apply { addAll(loaded?.exercises.orEmpty().map { it.copy(id = newTempId()) }) } }
    var dirty by remember(id) { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var replaceIdx by remember { mutableStateOf<Int?>(null) }
    var leave by remember { mutableStateOf(false) }
    fun tryLeave() { if (dirty) leave = true else nav.pop() }
    BackHandler(enabled = !picker) { tryLeave() }
    if (leave) WConfirm("Discard changes?", "You have unsaved changes to this workout.", "Discard", { leave = false; nav.pop() }, { leave = false })
    if (picker) {
        ExercisePicker(db, onPick = { ex ->
            val r = replaceIdx
            if (r != null) items[r] = items[r].copy(exerciseId = ex.id, name = ex.name)
            else items.add(TemplateExercise(newTempId(), 0, items.size, ex.id, ex.name))
            replaceIdx = null; picker = false; dirty = true
        }, onClose = { picker = false; replaceIdx = null })
        return
    }
    val err = items.firstNotNullOfOrNull { WorkoutLogic.validatePrescription(it.targetSets, it.repMin, it.repMax, it.targetKg, it.restSeconds, it.targetRpe)?.let { m -> "${it.name}: $m" } }
    val valid = name.isNotBlank() && items.isNotEmpty() && err == null
    fun save(): Long = db.saveTemplate(WorkoutTemplate(id, name, notes, 0, 0, null, items.mapIndexed { i, e -> e.copy(ord = i) })).also { dirty = false }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader(if (id == 0L) "Create workout" else "Edit workout", { tryLeave() })
        WField(name, { name = it.take(60); dirty = true }, Modifier.fillMaxWidth(), "Workout name (e.g. Push A)", numeric = false, align = TextAlign.Start)
        Spacer(Modifier.height(8.dp))
        WField(notes, { notes = it.take(200); dirty = true }, Modifier.fillMaxWidth(), "Notes (optional)", numeric = false, align = TextAlign.Start)
        WSection("Exercises")
        if (items.isEmpty()) WEmpty("No exercises yet", "Add at least one exercise from the library.")
        items.forEachIndexed { i, e ->
            key(e.id) {
                PrescriptionCard(
                    e, imperial, i > 0, i < items.lastIndex,
                    onChange = { items[i] = it; dirty = true },
                    onUp = { val t = items[i]; items[i] = items[i - 1]; items[i - 1] = t; dirty = true },
                    onDown = { val t = items[i]; items[i] = items[i + 1]; items[i + 1] = t; dirty = true },
                    onDup = { items.add(i + 1, e.copy(id = newTempId())); dirty = true },
                    onReplace = { replaceIdx = i; picker = true },
                    onRemove = { items.removeAt(i); dirty = true },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        GhostButton("Add exercise", icon = R.drawable.ic_add) { picker = true }
        if (err != null) Text(err, color = C.White, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        else if (name.isBlank() || items.isEmpty()) Text("Name the workout and add at least one exercise to save.", color = C.Text3, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Save workout", enabled = valid) { save(); nav.pop() }
        Spacer(Modifier.height(8.dp))
        GhostButton("Save & start now", enabled = valid) { val tid = save(); nav.pop(); onStart(tid) }
    }
}

@Composable
private fun PrescriptionCard(
    e: TemplateExercise, imperial: Boolean, canUp: Boolean, canDown: Boolean, onChange: (TemplateExercise) -> Unit,
    onUp: () -> Unit, onDown: () -> Unit, onDup: () -> Unit, onReplace: () -> Unit, onRemove: () -> Unit,
) {
    val kg0 = remember(e.id) { e.targetKg?.let { WorkoutLogic.fmt((WorkoutLogic.toDisplay(it, imperial) * 10).toLong() / 10.0) } ?: "" }
    var sets by remember(e.id) { mutableStateOf(e.targetSets.toString()) }
    var rmin by remember(e.id) { mutableStateOf(e.repMin.toString()) }
    var rmax by remember(e.id) { mutableStateOf(e.repMax.toString()) }
    var kg by remember(e.id) { mutableStateOf(kg0) }
    var rest by remember(e.id) { mutableStateOf(e.restSeconds?.toString() ?: "") }
    var rpe by remember(e.id) { mutableStateOf(e.targetRpe?.let { WorkoutLogic.fmt(it) } ?: "") }
    var note by remember(e.id) { mutableStateOf(e.notes) }
    fun emit() = onChange(
        e.copy(
            targetSets = sets.toIntOrNull() ?: 0, repMin = rmin.toIntOrNull() ?: 0, repMax = rmax.toIntOrNull() ?: 0,
            targetKg = if (kg.isBlank()) null else if (kg == kg0) e.targetKg else WorkoutLogic.fromDisplay(kg.toDoubleOrNull() ?: -1.0, imperial),
            restSeconds = rest.toIntOrNull(), targetRpe = rpe.toDoubleOrNull(), notes = note,
        )
    )
    FocusCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Text(e.name, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (canUp) WChip("↑", false) { onUp() }
            if (canDown) WChip("↓", false) { onDown() }
            WChip("Copy", false) { onDup() }; WChip("Swap", false) { onReplace() }; WChip("Remove", false) { onRemove() }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) { Caps("Sets"); WField(sets, { sets = it.filter(Char::isDigit).take(2); emit() }, Modifier.fillMaxWidth(), "3", decimal = false, desc = "Target sets") }
            Column(Modifier.weight(1f)) { Caps("Reps min"); WField(rmin, { rmin = it.filter(Char::isDigit).take(3); emit() }, Modifier.fillMaxWidth(), "8", decimal = false, desc = "Minimum reps") }
            Column(Modifier.weight(1f)) { Caps("Reps max"); WField(rmax, { rmax = it.filter(Char::isDigit).take(3); emit() }, Modifier.fillMaxWidth(), "12", decimal = false, desc = "Maximum reps") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) { Caps("Load ${if (imperial) "lb" else "kg"}"); WField(kg, { kg = it.filter { c -> c.isDigit() || c == '.' }.take(6); emit() }, Modifier.fillMaxWidth(), "—", desc = "Target load") }
            Column(Modifier.weight(1f)) { Caps("Rest sec"); WField(rest, { rest = it.filter(Char::isDigit).take(4); emit() }, Modifier.fillMaxWidth(), "—", decimal = false, desc = "Rest seconds") }
            Column(Modifier.weight(1f)) { Caps("RPE"); WField(rpe, { rpe = it.filter { c -> c.isDigit() || c == '.' }.take(3); emit() }, Modifier.fillMaxWidth(), "—", desc = "Target RPE") }
        }
        Spacer(Modifier.height(8.dp))
        WField(note, { note = it.take(120); emit() }, Modifier.fillMaxWidth(), "Notes", numeric = false, align = TextAlign.Start)
    }
}

// ---------------------------------------------------------------------------------- routines
private fun routineProgress(db: WorkoutDb, r: WorkoutRoutine): String {
    val start = r.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val since = start?.atStartOfDay(AppClock.zone())?.toInstant()?.toEpochMilli() ?: 0L
    val done = db.sessionsForRoutine(r.id, since)
    val fixed = r.days.filter { it.weekday != null && !it.rest }
    if (start == null || fixed.isEmpty()) return "$done session${if (done == 1) "" else "s"} completed"
    val today = AppClock.today()
    val end = r.weeks?.let { w -> start.plusWeeks(w.toLong()).minusDays(1).let { last -> if (last.isBefore(today)) last else today } } ?: today
    if (end.isBefore(start)) return "Starts ${start}"
    val weekdays = fixed.map { it.weekday!! }.toSet()
    val planned = (0..ChronoUnit.DAYS.between(start, end)).count { start.plusDays(it).dayOfWeek.value in weekdays }
    val a = WorkoutLogic.adherence(planned, done) ?: return "$done completed"
    return "$done of $planned planned sessions · ${(a * 100).toInt()}% adherence"
}

@Composable
fun RoutineListScreen(db: WorkoutDb, nav: WNav, tick: Int) {
    var t2 by remember { mutableIntStateOf(0) }
    var del by remember { mutableStateOf<WorkoutRoutine?>(null) }
    val list = remember(tick, t2) { db.routines() }
    del?.let { r -> WConfirm("Delete \"${r.name}\"?", "Logged sessions stay in your history; they just lose their routine link.", "Delete", { db.deleteRoutine(r.id); del = null; t2++ }, { del = null }) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("Routines", { nav.pop() }, "Your training splits")
        PrimaryButton("Create routine") { nav.push(WRoute.RoutineEdit(0)) }
        Spacer(Modifier.height(12.dp))
        if (list.isEmpty()) WEmpty("No routines yet", "Build any split: push/pull/legs, upper/lower, 5-day bro split, or your own.")
        list.forEach { r ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.name, color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Pill(r.status.code.replaceFirstChar { it.uppercase() }, filled = r.status == RoutineStatus.ACTIVE)
                }
                Text("${r.days.count { !it.rest }} training · ${r.days.count { it.rest }} rest days" + (r.weeks?.let { " · $it weeks" } ?: ""), color = C.Text2, fontSize = 13.sp)
                if (r.status == RoutineStatus.ACTIVE || r.status == RoutineStatus.PAUSED) Text(routineProgress(db, r), color = C.Text2, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    WChip("Edit", false) { nav.push(WRoute.RoutineEdit(r.id)) }
                    when (r.status) {
                        RoutineStatus.ACTIVE -> WChip("Pause", false) { db.setRoutineStatus(r.id, RoutineStatus.PAUSED); t2++ }
                        RoutineStatus.ARCHIVED -> WChip("Restore", false) { db.setRoutineStatus(r.id, RoutineStatus.DRAFT); t2++ }
                        else -> WChip("Activate", true) { db.setRoutineStatus(r.id, RoutineStatus.ACTIVE); t2++ }
                    }
                    WChip("Duplicate", false) { db.duplicateRoutine(r.id); t2++ }
                    if (r.status != RoutineStatus.ARCHIVED) WChip("Archive", false) { db.setRoutineStatus(r.id, RoutineStatus.ARCHIVED); t2++ }
                    WChip("Delete", false) { del = r }
                }
            }
        }
    }
}

@Composable
fun RoutineEditScreen(db: WorkoutDb, nav: WNav, id: Long) {
    val loaded = remember(id) { if (id == 0L) null else db.routine(id) }
    val templates = remember { db.templates() }
    var name by remember(id) { mutableStateOf(loaded?.name ?: "") }
    var desc by remember(id) { mutableStateOf(loaded?.description ?: "") }
    var weeks by remember(id) { mutableStateOf(loaded?.weeks?.toString() ?: "") }
    var startToday by remember(id) { mutableStateOf(loaded?.startDate != null) }
    val days = remember(id) { mutableStateListOf<RoutineDay>().apply { addAll(loaded?.days.orEmpty()) } }
    var dirty by remember(id) { mutableStateOf(false) }
    var leave by remember { mutableStateOf(false) }
    var pickFor by remember { mutableStateOf<Int?>(null) }
    fun tryLeave() { if (dirty) leave = true else nav.pop() }
    BackHandler { tryLeave() }
    if (leave) WConfirm("Discard changes?", "You have unsaved changes to this routine.", "Discard", { leave = false; nav.pop() }, { leave = false })
    pickFor?.let { idx ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pickFor = null }, containerColor = C.Sheet, titleContentColor = C.White, textContentColor = C.Text2,
            title = { Text("Assign workout", fontWeight = FontWeight.SemiBold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (templates.isEmpty()) Text("No saved workouts yet. Create one first from the Workout hub.")
                    templates.forEach { t -> Text(t.name, color = C.White, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clickable { days[idx] = days[idx].copy(templateId = t.id, label = days[idx].label.ifBlank { t.name }); dirty = true; pickFor = null }.padding(vertical = 12.dp)) }
                }
            },
            confirmButton = { Text("Close", color = C.White, modifier = Modifier.clickable { pickFor = null }.padding(12.dp)) },
            dismissButton = { Text("Unassign", color = C.Text2, modifier = Modifier.clickable { days[idx] = days[idx].copy(templateId = null); dirty = true; pickFor = null }.padding(12.dp)) },
        )
    }
    val plans = days.map { WorkoutLogic.DayPlan(it.label, it.weekday, it.rest, it.templateId) }
    val issues = WorkoutLogic.routineIssues(plans)
    val wk = weeks.toIntOrNull()
    val weeksBad = weeks.isNotBlank() && (wk == null || wk !in 1..52)
    val canSave = name.isNotBlank() && !weeksBad
    fun build(status: RoutineStatus) = WorkoutRoutine(id, name, desc, wk?.takeIf { !weeksBad }, if (startToday) (loaded?.startDate ?: AppClock.today().toString()) else null, status, 0, 0, days.mapIndexed { i, d -> d.copy(ord = i) })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader(if (id == 0L) "Create routine" else "Edit routine", { tryLeave() })
        WField(name, { name = it.take(60); dirty = true }, Modifier.fillMaxWidth(), "Routine name (e.g. Upper/Lower)", numeric = false, align = TextAlign.Start)
        Spacer(Modifier.height(8.dp))
        WField(desc, { desc = it.take(200); dirty = true }, Modifier.fillMaxWidth(), "Description (optional)", numeric = false, align = TextAlign.Start)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            WField(weeks, { weeks = it.filter(Char::isDigit).take(2); dirty = true }, Modifier.weight(1f), "Weeks (optional)", decimal = false)
            WChip(if (startToday) "Starts today ✓" else "Set start: today", startToday) { startToday = !startToday; dirty = true }
        }
        if (weeksBad) Text("Weeks must be 1–52", color = C.White, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        WSection("Days")
        if (days.isEmpty()) WEmpty("No days yet", "Add training days and rest days in any order. Weekdays are optional — leave them off for a flexible rotation.")
        days.forEachIndexed { i, d ->
            key(d.id) {
                FocusCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    var label by remember(d.id) { mutableStateOf(d.label) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        WField(label, { label = it.take(40); days[i] = days[i].copy(label = label); dirty = true }, Modifier.weight(1f), if (d.rest) "Rest day" else "Day name (e.g. Push A)", numeric = false, align = TextAlign.Start)
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        WChip("Any day", d.weekday == null) { days[i] = days[i].copy(weekday = null); dirty = true }
                        (1..7).forEach { w -> WChip(WorkoutLogic.dayName(w), d.weekday == w) { days[i] = days[i].copy(weekday = w); dirty = true } }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (!d.rest) WChip(d.templateId?.let { tid -> templates.firstOrNull { it.id == tid }?.name ?: "Workout (archived)" } ?: "Assign workout…", d.templateId != null) { pickFor = i }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        WChip(if (d.rest) "Rest day ✓" else "Make rest day", d.rest) { days[i] = days[i].copy(rest = !d.rest, templateId = null); dirty = true }
                        if (i > 0) WChip("↑", false) { val t = days[i]; days[i] = days[i - 1]; days[i - 1] = t; dirty = true }
                        if (i < days.lastIndex) WChip("↓", false) { val t = days[i]; days[i] = days[i + 1]; days[i + 1] = t; dirty = true }
                        WChip("Copy", false) { days.add(i + 1, d.copy(id = newTempId())); dirty = true }
                        WChip("Remove", false) { days.removeAt(i); dirty = true }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Add training day", Modifier.weight(1f)) { days.add(RoutineDay(newTempId(), id, days.size, "", null, false, null)); dirty = true }
            GhostButton("Add rest day", Modifier.weight(1f)) { days.add(RoutineDay(newTempId(), id, days.size, "Rest", null, true, null)); dirty = true }
        }
        WSection("Week preview")
        FocusCard(Modifier.fillMaxWidth()) {
            (1..7).forEach { w ->
                val here = days.filter { it.weekday == w }
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text(WorkoutLogic.dayName(w), color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(end = 16.dp))
                    Text(if (here.isEmpty()) "—" else here.joinToString(", ") { it.label.ifBlank { if (it.rest) "Rest" else "Unnamed" } }, color = if (here.isEmpty()) C.Text3 else C.White, fontSize = 14.sp)
                }
            }
            val flex = days.filter { it.weekday == null }
            if (flex.isNotEmpty()) Text("Flexible rotation: " + flex.joinToString(" → ") { it.label.ifBlank { if (it.rest) "Rest" else "Unnamed" } }, color = C.Text2, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        }
        issues.forEach { Text("• $it", color = C.White, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Activate routine", enabled = canSave && issues.isEmpty()) {
            val rid = db.saveRoutine(build(loaded?.status ?: RoutineStatus.DRAFT)); db.setRoutineStatus(rid, RoutineStatus.ACTIVE); dirty = false; nav.pop()
        }
        Spacer(Modifier.height(8.dp))
        GhostButton(if (loaded == null || loaded.status == RoutineStatus.DRAFT) "Save as draft" else "Save changes", enabled = canSave) { db.saveRoutine(build(loaded?.status ?: RoutineStatus.DRAFT)); dirty = false; nav.pop() }
    }
}
