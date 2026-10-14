package com.pranav.focus.workout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.ui.C
import com.pranav.focus.ui.Caps
import com.pranav.focus.ui.FocusCard
import com.pranav.focus.ui.FocusSheet
import com.pranav.focus.ui.GhostButton
import com.pranav.focus.ui.PrimaryButton
import com.pranav.focus.ui.ScreenHeader
import com.pranav.focus.ui.fieldColors
import com.pranav.focus.workout.data.WorkoutDb
import com.pranav.focus.workout.data.WorkoutRepository
import com.pranav.focus.workout.domain.RoutineDay
import com.pranav.focus.workout.domain.RoutineStatus
import com.pranav.focus.workout.domain.WorkoutRoutine
import com.pranav.focus.workout.domain.WorkoutRules
import com.pranav.focus.workout.domain.WorkoutTemplate
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Custom routine builder. A routine is a plan of training days, each with a custom name, optional fixed weekday
 * and an assigned workout template. Rest days have no template. Activation is validated by the domain rules.
 */
@Composable
fun RoutineBuilderScreen(routineId: String?, onBack: () -> Unit) {
    val repo: WorkoutRepository = remember { WorkoutDb.get(LocalContext.current) }
    val loaded = remember(routineId) {
        runCatching {
            if (routineId == null) WorkoutRules.newRoutine(newId("rtn"), "", System.currentTimeMillis())
            else repo.routine(routineId) ?: throw IllegalStateException("This routine no longer exists")
        }
    }
    val initial = loaded.getOrNull()
    if (initial == null) {
        Column(Modifier.fillMaxSize().background(C.Black).padding(horizontal = 20.dp, vertical = 8.dp)) {
            ScreenHeader("Routine", onBack = onBack)
            FocusCard { Text(loaded.exceptionOrNull()?.message ?: "Couldn't open this routine", color = C.Text2, fontSize = 13.sp) }
        }
        return
    }
    val isNew = routineId == null

    var draft by remember(routineId) { mutableStateOf(initial) }
    var saved by remember(routineId) { mutableStateOf(initial) }
    val templates: Map<String, WorkoutTemplate> = remember { loadTemplates(repo) }
    var problems by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var editingDay by remember { mutableStateOf<String?>(null) }
    var addingDay by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var startText by remember(routineId) { mutableStateOf(initial.startDate?.toString() ?: "") }
    var weeksText by remember(routineId) { mutableStateOf(initial.durationWeeks?.toString() ?: "") }

    val dirty = draft != saved
    BackHandler(enabled = dirty) { confirmDiscard = true }

    /** Applies optional field text to the draft. Blank = unset. Invalid text becomes a visible problem. */
    fun withFields(r: WorkoutRoutine): Result<WorkoutRoutine> {
        val start: LocalDate? = if (startText.isBlank()) null else try {
            LocalDate.parse(startText.trim())
        } catch (e: DateTimeParseException) {
            return Result.failure(IllegalArgumentException("Start date must look like 2026-10-14"))
        }
        val weeks: Int? = if (weeksText.isBlank()) null else (weeksText.trim().toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Duration must be a whole number of weeks")))
        return Result.success(r.copy(startDate = start, durationWeeks = weeks))
    }

    fun saveDraft(): Boolean {
        val ready = withFields(draft).getOrElse { problems = listOf(it.message ?: "Check the routine details"); return false }
        val p = WorkoutRules.validateRoutine(ready, templates).map { it.message }
        if (p.isNotEmpty()) { problems = p; message = null; return false }
        return runCatching { repo.saveRoutine(ready) }.fold(
            onSuccess = { draft = ready; saved = ready; problems = emptyList(); message = "Saved"; true },
            onFailure = { problems = listOf(it.message ?: "Couldn't save this routine"); message = null; false },
        )
    }

    /** Activates after validation. Any other active routine is paused so only one drives "today". */
    fun setStatus(to: RoutineStatus) {
        val base = withFields(draft).getOrElse { problems = listOf(it.message ?: "Check the routine details"); return }
        val now = System.currentTimeMillis()
        val next = WorkoutRules.changeStatus(base, to, templates, now).getOrElse {
            problems = listOf(it.message ?: "Can't change status"); return
        }
        runCatching {
            if (to == RoutineStatus.ACTIVE) {
                WorkoutRules.activateExclusively(next, repo.routines(), now).forEach { repo.saveRoutine(it) }
            }
            repo.saveRoutine(next)
        }.fold(
            onSuccess = { draft = next; saved = next; problems = emptyList(); message = "Status: ${statusLabel(next.status)}" },
            onFailure = { problems = listOf(it.message ?: "Couldn't update status") },
        )
    }

    Column(
        Modifier.fillMaxSize().background(C.Black).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            if (isNew) "New routine" else "Edit routine",
            onBack = { if (dirty) confirmDiscard = true else onBack() },
            subtitle = "Status: ${statusLabel(draft.status)}",
        )

        OutlinedTextField(
            value = draft.name, onValueChange = { draft = draft.copy(name = it) }, label = { Text("Routine name") },
            singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
        )
        OutlinedTextField(
            value = draft.description, onValueChange = { draft = draft.copy(description = it) }, label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weeksText, onValueChange = { weeksText = it }, label = { Text("Weeks (optional)") },
                singleLine = true, modifier = Modifier.weight(1f), colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = startText, onValueChange = { startText = it }, label = { Text("Start (yyyy-mm-dd)") },
                singleLine = true, modifier = Modifier.weight(1f), colors = fieldColors(),
            )
        }

        Caps("Training days")
        if (draft.days.isEmpty()) {
            FocusCard {
                Text("No days yet", color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Add a day for each session. Name them however you like, such as Push A, Legs or Conditioning. Add rest days where you want recovery.", color = C.Text2, fontSize = 13.sp)
            }
        }
        val last = draft.days.lastIndex
        draft.days.sortedBy { it.orderIndex }.forEachIndexed { i, d ->
            FocusCard {
                Text(d.label, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(dayLine(d, templates), color = C.Text2, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GhostButton("Up", enabled = i > 0) { draft = WorkoutRules.moveDay(draft, d.id, i - 1, System.currentTimeMillis()) }
                    GhostButton("Down", enabled = i < last) { draft = WorkoutRules.moveDay(draft, d.id, i + 1, System.currentTimeMillis()) }
                    GhostButton("Copy") { draft = WorkoutRules.duplicateDay(draft, d.id, newId("day"), System.currentTimeMillis()) }
                    GhostButton("Edit") { editingDay = d.id }
                    GhostButton("Remove") { draft = WorkoutRules.removeDay(draft, d.id, System.currentTimeMillis()) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GhostButton("Add training day", modifier = Modifier.weight(1f)) { addingDay = true }
            GhostButton("Add rest day", modifier = Modifier.weight(1f)) {
                draft = WorkoutRules.addDay(draft, newId("day"), "Rest", true, null, null, System.currentTimeMillis())
            }
        }

        Caps("Week preview")
        FocusCard {
            val sorted = draft.days.sortedBy { it.orderIndex }
            if (sorted.isEmpty()) Text("Nothing scheduled yet.", color = C.Text2, fontSize = 13.sp)
            sorted.forEach { d ->
                val when_ = d.weekday?.let { WorkoutRules.weekdayName(it) } ?: "Flexible"
                Text("$when_: ${d.label}${if (d.isRestDay) " (rest)" else ""}", color = C.Text2, fontSize = 13.sp)
            }
        }

        if (problems.isNotEmpty()) FocusCard { problems.forEach { Text(it, color = C.White, fontSize = 13.sp) } }
        message?.let { Text(it, color = C.Text2, fontSize = 13.sp) }

        PrimaryButton(if (dirty) "Save draft" else "Saved", enabled = dirty) { saveDraft() }

        if (!isNew) {
            when (draft.status) {
                RoutineStatus.DRAFT, RoutineStatus.PAUSED -> PrimaryButton("Activate", enabled = !dirty) { setStatus(RoutineStatus.ACTIVE) }
                RoutineStatus.ACTIVE -> GhostButton("Pause", modifier = Modifier.fillMaxWidth(), enabled = !dirty) { setStatus(RoutineStatus.PAUSED) }
                RoutineStatus.ARCHIVED -> GhostButton("Restore as draft", modifier = Modifier.fillMaxWidth(), enabled = !dirty) { setStatus(RoutineStatus.DRAFT) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Duplicate", modifier = Modifier.weight(1f), enabled = !dirty) {
                    val copy = WorkoutRules.duplicateRoutine(saved, newId("rtn"), saved.days.map { newId("day") }, saved.name + " copy", System.currentTimeMillis())
                    runCatching { repo.saveRoutine(copy) }
                        .onSuccess { message = "Saved a copy as \"${copy.name}\"" }
                        .onFailure { problems = listOf(it.message ?: "Couldn't copy this routine") }
                }
                GhostButton(
                    if (saved.status == RoutineStatus.ARCHIVED) "Restore" else "Archive",
                    modifier = Modifier.weight(1f), enabled = !dirty && saved.status != RoutineStatus.ACTIVE,
                ) {
                    setStatus(if (saved.status == RoutineStatus.ARCHIVED) RoutineStatus.DRAFT else RoutineStatus.ARCHIVED)
                }
            }
            GhostButton("Delete routine", modifier = Modifier.fillMaxWidth()) { confirmDelete = true }
        }
    }

    if (addingDay) {
        DaySheet(
            title = "Add day", templates = templates.values.filter { it.archivedAtMs == null }.sortedBy { it.name },
            initial = null,
            onDismiss = { addingDay = false },
            onSave = { label, rest, tid, wd ->
                draft = WorkoutRules.addDay(draft, newId("day"), label, rest, tid, wd, System.currentTimeMillis())
                addingDay = false
            },
        )
    }

    val dayToEdit = editingDay
    if (dayToEdit != null) {
        val d = draft.days.firstOrNull { it.id == dayToEdit }
        if (d == null) {
            editingDay = null
        } else {
            DaySheet(
                title = "Edit day",
                templates = templates.values.filter { it.archivedAtMs == null || it.id == d.templateId }.sortedBy { it.name },
                initial = d,
                onDismiss = { editingDay = null },
                onSave = { label, rest, tid, wd ->
                    draft = WorkoutRules.updateDay(draft, dayToEdit, label, rest, tid, wd, System.currentTimeMillis())
                    editingDay = null
                },
            )
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false }, containerColor = C.Raised,
            title = { Text("Discard changes?", color = C.White) },
            text = { Text("Your unsaved edits to this routine will be lost.", color = C.Text2) },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Discard", color = C.White) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing", color = C.Text2) } },
        )
    }

    if (confirmDelete && routineId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false }, containerColor = C.Raised,
            title = { Text("Delete this routine?", color = C.White) },
            text = { Text("The routine plan is removed. Sessions you completed from it stay in your history.", color = C.Text2) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    runCatching { repo.deleteRoutine(routineId) }.fold(onSuccess = { onBack() }, onFailure = { problems = listOf(it.message ?: "Couldn't delete") })
                }) { Text("Delete", color = C.White) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = C.Text2) } },
        )
    }
}

private fun loadTemplates(repo: WorkoutRepository): Map<String, WorkoutTemplate> =
    runCatching { repo.templates(includeArchived = true).associateBy { it.id } }.getOrDefault(emptyMap())

private fun dayLine(d: RoutineDay, templates: Map<String, WorkoutTemplate>): String {
    val when_ = d.weekday?.let { WorkoutRules.weekdayName(it) } ?: "Flexible day"
    if (d.isRestDay) return "$when_ · Rest / recovery"
    val t = d.templateId?.let { templates[it] }
    val workout = when {
        d.templateId == null -> "No workout assigned"
        t == null -> "Workout no longer exists"
        t.archivedAtMs != null -> "${t.name} (archived)"
        else -> t.name
    }
    return "$when_ · $workout"
}

@Composable
private fun DaySheet(
    title: String,
    templates: List<WorkoutTemplate>,
    initial: RoutineDay?,
    onDismiss: () -> Unit,
    onSave: (label: String, rest: Boolean, templateId: String?, weekday: Int?) -> Unit,
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var rest by remember { mutableStateOf(initial?.isRestDay ?: false) }
    var templateId by remember { mutableStateOf(initial?.templateId) }
    var weekday by remember { mutableStateOf(initial?.weekday) }
    var error by remember { mutableStateOf<String?>(null) }

    FocusSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = label, onValueChange = { label = it; error = null }, label = { Text("Day name (for example Push A)") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Choice("Training", !rest, Modifier.weight(1f)) { rest = false }
                Choice("Rest day", rest, Modifier.weight(1f)) { rest = true; templateId = null }
            }
            if (!rest) {
                Caps("Workout for this day")
                if (templates.isEmpty()) {
                    Text("No saved workouts yet. Create one first, then assign it here.", color = C.Text2, fontSize = 13.sp)
                }
                templates.forEach { t ->
                    Choice(t.name, templateId == t.id, Modifier.fillMaxWidth()) { templateId = t.id }
                }
            }
            Caps("Fixed weekday (optional)")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Choice("Any", weekday == null, Modifier.weight(1f)) { weekday = null }
            }
            (1..7).toList().chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { wd ->
                        Choice(WorkoutRules.weekdayName(wd).take(3), weekday == wd, Modifier.weight(1f)) { weekday = wd }
                    }
                }
            }
            error?.let { Text(it, color = C.White, fontSize = 13.sp) }
            PrimaryButton("Done") {
                when {
                    label.isBlank() -> error = "Give this day a name"
                    label.trim().length > WorkoutRules.LABEL_MAX -> error = "Day names must be ${WorkoutRules.LABEL_MAX} characters or fewer"
                    !rest && templateId == null -> error = "Choose a workout for this day, or make it a rest day"
                    else -> onSave(label.trim(), rest, if (rest) null else templateId, weekday)
                }
            }
        }
    }
}

@Composable
private fun Choice(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) C.Black else C.White,
        fontSize = 13.sp,
        modifier = modifier
            .background(if (selected) C.White else C.Raised, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    )
}
