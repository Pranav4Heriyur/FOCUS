package com.pranav.focus.workout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import com.pranav.focus.workout.domain.Exercise
import com.pranav.focus.workout.domain.MuscleGroup
import com.pranav.focus.workout.domain.Prescription
import com.pranav.focus.workout.domain.TemplateExercise
import com.pranav.focus.workout.domain.WorkoutRules
import com.pranav.focus.workout.domain.WorkoutTemplate

/**
 * Custom workout builder (template editor). Edits a reusable template. Past sessions are never changed:
 * sessions hold their own copy of the prescriptions.
 */
@Composable
fun TemplateBuilderScreen(templateId: String?, onBack: () -> Unit) {
    val repo: WorkoutRepository = remember { WorkoutDb.get(LocalContext.current) }
    val loaded = remember(templateId) {
        runCatching {
            if (templateId == null) WorkoutRules.newTemplate(newId("tpl"), "", System.currentTimeMillis())
            else repo.template(templateId) ?: throw IllegalStateException("This workout no longer exists")
        }
    }
    val initial = loaded.getOrNull()
    if (initial == null) {
        LoadFailure(loaded.exceptionOrNull()?.message ?: "Couldn't open this workout", onBack)
        return
    }
    val isNew = templateId == null

    var draft by remember(templateId) { mutableStateOf(initial) }
    var saved by remember(templateId) { mutableStateOf(initial) }
    var catalogue by remember { mutableStateOf(loadCatalogue(repo)) }
    var problems by remember { mutableStateOf<List<String>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val dirty = draft != saved
    BackHandler(enabled = dirty) { confirmDiscard = true }

    fun save(): Boolean {
        val p = WorkoutRules.validateTemplate(draft).map { it.message }
        if (p.isNotEmpty()) { problems = p; message = null; return false }
        return runCatching { repo.saveTemplate(draft) }.fold(
            onSuccess = { saved = draft; problems = emptyList(); message = "Saved"; true },
            onFailure = { problems = listOf(it.message ?: "Couldn't save this workout"); message = null; false },
        )
    }

    Column(
        Modifier.fillMaxSize().background(C.Black).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenHeader(
            if (isNew) "New workout" else "Edit workout",
            onBack = { if (dirty) confirmDiscard = true else onBack() },
            subtitle = "A reusable session template",
        )

        OutlinedTextField(
            value = draft.name, onValueChange = { draft = draft.copy(name = it) },
            label = { Text("Workout name") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
        )
        OutlinedTextField(
            value = draft.notes, onValueChange = { draft = draft.copy(notes = it) },
            label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
        )

        Caps("Exercises")
        if (draft.exercises.isEmpty()) {
            FocusCard {
                Text("No exercises yet", color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Add exercises from the library or create your own. You set the sets, reps and load for each.", color = C.Text2, fontSize = 13.sp)
            }
        }
        val last = draft.exercises.lastIndex
        draft.exercises.sortedBy { it.orderIndex }.forEachIndexed { i, row ->
            val ex = catalogue[row.exerciseId]
            FocusCard {
                Text("${i + 1}. ${ex?.name ?: "Missing exercise"}", color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(prescriptionSummary(row.prescription), color = C.Text2, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GhostButton("Up", enabled = i > 0) { draft = WorkoutRules.moveExercise(draft, row.id, i - 1, System.currentTimeMillis()) }
                    GhostButton("Down", enabled = i < last) { draft = WorkoutRules.moveExercise(draft, row.id, i + 1, System.currentTimeMillis()) }
                    GhostButton("Copy") { draft = WorkoutRules.duplicateExercise(draft, row.id, newId("row"), System.currentTimeMillis()) }
                    GhostButton("Edit") { editingRow = row.id }
                    GhostButton("Remove") { draft = WorkoutRules.removeExercise(draft, row.id, System.currentTimeMillis()) }
                }
            }
        }
        GhostButton("Add exercise") { showAdd = true }

        if (problems.isNotEmpty()) FocusCard { problems.forEach { Text(it, color = C.White, fontSize = 13.sp) } }
        message?.let { Text(it, color = C.Text2, fontSize = 13.sp) }

        PrimaryButton(if (dirty) "Save workout" else "Saved", enabled = dirty) { save() }

        if (!isNew) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("Duplicate", modifier = Modifier.weight(1f), enabled = !dirty) {
                    val copy = WorkoutRules.duplicateTemplate(
                        saved, newId("tpl"), saved.exercises.map { newId("row") },
                        saved.name + " copy", System.currentTimeMillis(),
                    )
                    runCatching { repo.saveTemplate(copy) }
                        .onSuccess { message = "Saved a copy as \"${copy.name}\"" }
                        .onFailure { problems = listOf(it.message ?: "Couldn't copy this workout") }
                }
                GhostButton(
                    if (saved.archivedAtMs == null) "Archive" else "Restore",
                    modifier = Modifier.weight(1f), enabled = !dirty,
                ) {
                    val now = System.currentTimeMillis()
                    val next = if (saved.archivedAtMs == null) WorkoutRules.archiveTemplate(saved, now) else WorkoutRules.restoreTemplate(saved, now)
                    runCatching { repo.saveTemplate(next) }.onSuccess { saved = next; draft = next; message = if (next.archivedAtMs == null) "Restored" else "Archived" }
                        .onFailure { problems = listOf(it.message ?: "Couldn't update this workout") }
                }
            }
            GhostButton("Delete workout", modifier = Modifier.fillMaxWidth()) { confirmDelete = true }
        }
    }

    if (showAdd) {
        FocusSheet(onDismiss = { showAdd = false }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Exercise library", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                catalogue.values.sortedWith(compareBy<Exercise>({ it.isCustom }, { it.name })).forEach { ex ->
                    FocusCard(onClick = {
                        draft = WorkoutRules.addExercise(draft, newId("row"), ex.id, System.currentTimeMillis())
                        showAdd = false
                    }) {
                        Text(ex.name, color = C.White, fontSize = 15.sp)
                        Text(ex.primaryMuscles.joinToString(", ") { it.label }.ifBlank { "No muscles set" }, color = C.Text2, fontSize = 12.sp)
                    }
                }
                GhostButton("Create custom exercise", modifier = Modifier.fillMaxWidth()) { showAdd = false; showCustom = true }
            }
        }
    }

    if (showCustom) {
        CustomExerciseSheet(
            onDismiss = { showCustom = false },
            onCreate = { name, muscles ->
                val ex = Exercise(newId("custom"), name.trim(), muscles, isCustom = true)
                runCatching { repo.saveCustomExercise(ex) }.fold(
                    onSuccess = {
                        catalogue = catalogue + (ex.id to ex)
                        draft = WorkoutRules.addExercise(draft, newId("row"), ex.id, System.currentTimeMillis())
                        showCustom = false
                    },
                    onFailure = { problems = listOf(it.message ?: "Couldn't save this exercise") },
                )
            },
        )
    }

    val rowToEdit = editingRow
    if (rowToEdit != null) {
        val row: TemplateExercise? = draft.exercises.firstOrNull { it.id == rowToEdit }
        if (row == null) {
            editingRow = null
        } else {
            PrescriptionSheet(
                exerciseName = catalogue[row.exerciseId]?.name ?: "Exercise",
                initial = row.prescription,
                onDismiss = { editingRow = null },
                onApply = { p ->
                    val probs = WorkoutRules.validatePrescription(p).map { it.message }
                    if (probs.isEmpty()) {
                        draft = WorkoutRules.updatePrescription(draft, rowToEdit, p, System.currentTimeMillis())
                        editingRow = null
                        null
                    } else probs
                },
            )
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = C.Raised,
            title = { Text("Discard changes?", color = C.White) },
            text = { Text("Your unsaved edits to this workout will be lost.", color = C.Text2) },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Discard", color = C.White) } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing", color = C.Text2) } },
        )
    }

    if (confirmDelete && templateId != null) {
        DeleteTemplateDialog(
            repo = repo,
            template = saved,
            onDismiss = { confirmDelete = false },
            onDeleted = { confirmDelete = false; onBack() },
            onArchived = { next -> saved = next; draft = next; confirmDelete = false; message = "Archived instead" },
            onError = { problems = listOf(it); confirmDelete = false },
        )
    }
}

@Composable
private fun LoadFailure(message: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(C.Black).padding(horizontal = 20.dp, vertical = 8.dp)) {
        ScreenHeader("Workout", onBack = onBack)
        FocusCard { Text(message, color = C.Text2, fontSize = 13.sp) }
    }
}

private fun loadCatalogue(repo: WorkoutRepository): Map<String, Exercise> =
    runCatching { repo.exercises().associateBy { it.id } }.getOrDefault(emptyMap())

@Composable
private fun DeleteTemplateDialog(
    repo: WorkoutRepository,
    template: WorkoutTemplate,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    onArchived: (WorkoutTemplate) -> Unit,
    onError: (String) -> Unit,
) {
    val usedBy = runCatching { repo.routines().filter { r -> r.days.any { it.templateId == template.id } } }.getOrDefault(emptyList())
    if (usedBy.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = C.Raised,
            title = { Text("This workout is used in a routine", color = C.White) },
            text = {
                Text("Used by: ${usedBy.joinToString { it.name }}. Archive it to keep those routines intact, or remove it from those routines first. Past sessions are never changed.", color = C.Text2)
            },
            confirmButton = {
                TextButton(onClick = {
                    val next = WorkoutRules.archiveTemplate(template, System.currentTimeMillis())
                    runCatching { repo.saveTemplate(next) }.fold(onSuccess = { onArchived(next) }, onFailure = { onError(it.message ?: "Couldn't archive") })
                }) { Text("Archive instead", color = C.White) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = C.Text2) } },
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = C.Raised,
            title = { Text("Delete this workout?", color = C.White) },
            text = { Text("This removes the template. Sessions you already completed keep their records.", color = C.Text2) },
            confirmButton = {
                TextButton(onClick = {
                    runCatching { repo.deleteTemplate(template.id) }.fold(onSuccess = { onDeleted() }, onFailure = { onError(it.message ?: "Couldn't delete") })
                }) { Text("Delete", color = C.White) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = C.Text2) } },
        )
    }
}

@Composable
private fun PrescriptionSheet(
    exerciseName: String,
    initial: Prescription,
    onDismiss: () -> Unit,
    onApply: (Prescription) -> List<String>?,
) {
    var sets by remember { mutableStateOf(initial.targetSets.toString()) }
    var repMin by remember { mutableStateOf(initial.repMin.toString()) }
    var repMax by remember { mutableStateOf(initial.repMax.toString()) }
    var load by remember { mutableStateOf(initial.targetLoadKg?.let { fmtNum(it) } ?: "") }
    var rest by remember { mutableStateOf(initial.restSeconds?.toString() ?: "") }
    var rpe by remember { mutableStateOf(initial.targetRpe?.let { fmtNum(it) } ?: "") }
    var notes by remember { mutableStateOf(initial.notes) }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }

    FocusSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(exerciseName, color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Sets", sets, Modifier.weight(1f), KeyboardType.Number) { sets = it }
                NumField("Min reps", repMin, Modifier.weight(1f), KeyboardType.Number) { repMin = it }
                NumField("Max reps", repMax, Modifier.weight(1f), KeyboardType.Number) { repMax = it }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField("Load (kg, optional)", load, Modifier.weight(1f), KeyboardType.Decimal) { load = it }
                NumField("Rest (s, optional)", rest, Modifier.weight(1f), KeyboardType.Number) { rest = it }
                NumField("RPE (optional)", rpe, Modifier.weight(1f), KeyboardType.Decimal) { rpe = it }
            }
            OutlinedTextField(
                value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
            )
            if (errors.isNotEmpty()) FocusCard { errors.forEach { Text(it, color = C.White, fontSize = 13.sp) } }
            PrimaryButton("Apply") {
                val parsed = parsePrescription(sets, repMin, repMax, load, rest, rpe, notes)
                parsed.fold(
                    onSuccess = { p -> errors = onApply(p) ?: emptyList() },
                    onFailure = { errors = listOf(it.message ?: "Check the values") },
                )
            }
        }
    }
}

@Composable
private fun NumField(label: String, value: String, modifier: Modifier, type: KeyboardType, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label, fontSize = 11.sp) },
        singleLine = true, modifier = modifier, colors = fieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = type),
    )
}

@Composable
private fun CustomExerciseSheet(onDismiss: () -> Unit, onCreate: (String, Set<MuscleGroup>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(setOf<MuscleGroup>()) }
    var error by remember { mutableStateOf<String?>(null) }
    FocusSheet(onDismiss = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Custom exercise", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = name, onValueChange = { name = it; error = null }, label = { Text("Exercise name") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors(),
            )
            Caps("Primary muscles (tap to choose)")
            MuscleGroup.values().toList().chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { m ->
                        val on = m in picked
                        Text(
                            m.label,
                            color = if (on) C.Black else C.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                                .background(if (on) C.White else C.Raised, RoundedCornerShape(12.dp))
                                .border(1.dp, C.Hairline, RoundedCornerShape(12.dp))
                                .clickable { picked = if (on) picked - m else picked + m }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                        )
                    }
                }
            }
            error?.let { Text(it, color = C.White, fontSize = 13.sp) }
            PrimaryButton("Create and add") {
                val problems = WorkoutRules.validateName("Exercise name", name)
                if (problems.isNotEmpty()) error = problems.first().message else onCreate(name, picked)
            }
        }
    }
}
