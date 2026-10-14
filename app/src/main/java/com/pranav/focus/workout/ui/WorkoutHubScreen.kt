package com.pranav.focus.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.data.AppClock
import com.pranav.focus.ui.C
import com.pranav.focus.ui.Caps
import com.pranav.focus.ui.FocusCard
import com.pranav.focus.ui.GhostButton
import com.pranav.focus.ui.PrimaryButton
import com.pranav.focus.ui.ProgBar
import com.pranav.focus.ui.ScreenHeader
import com.pranav.focus.workout.data.WorkoutDb
import com.pranav.focus.workout.data.WorkoutRepository
import com.pranav.focus.workout.domain.RoutineDay
import com.pranav.focus.workout.domain.RoutineStatus
import com.pranav.focus.workout.domain.SessionStatus
import com.pranav.focus.workout.domain.WorkoutMath
import com.pranav.focus.workout.domain.WorkoutRoutine
import com.pranav.focus.workout.domain.WorkoutRules
import com.pranav.focus.workout.domain.WorkoutSession
import com.pranav.focus.workout.domain.WorkoutTemplate
import java.time.LocalDate

/**
 * C0 Workout Hub. Every number on this screen comes from saved data:
 *  - "This week" counts COMPLETED sessions in the Monday-based week.
 *  - "Load" is tonnage (sum of load × reps over working sets with both values), shown in tonnes.
 *  - The week ring is completed scheduled slots ÷ scheduled training slots, for fixed-weekday routines only.
 *  - "Form" has no defined formula in the source spec, so it is shown as not available rather than invented.
 * Start-workout, quick-log and PR markers are not implemented in this checkpoint and are not shown.
 */
@Composable
fun WorkoutHubScreen(
    onOpenTemplate: (String?) -> Unit,
    onOpenRoutine: (String?) -> Unit,
) {
    val repo: WorkoutRepository = remember { WorkoutDb.get(LocalContext.current) }
    // Loaded on entry. The screen leaves composition while a builder is open, so returning reloads the data.
    val hub = remember { loadHub(repo) }

    Column(
        Modifier.fillMaxSize().background(C.Black).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenHeader("Workout", onBack = null, subtitle = "Week ${hub.weekLabel}")

        hub.error?.let { FocusCard { Text(it, color = C.Text2, fontSize = 13.sp) } }

        HeroCard(hub)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("This week", "${hub.weekSessions}", "sessions", Modifier.weight(1f))
            StatTile("Load", tonnesText(hub.weekTonnageKg), "tonnes", Modifier.weight(1f))
            StatTile("Form", "—", "not available", Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton("New workout", modifier = Modifier.weight(1f)) { onOpenTemplate(null) }
            GhostButton("New routine", modifier = Modifier.weight(1f)) { onOpenRoutine(null) }
        }

        Caps("Routines")
        if (hub.routines.isEmpty()) {
            FocusCard { Text("No routines yet. A routine schedules workouts across the days you choose.", color = C.Text2, fontSize = 13.sp) }
        } else {
            hub.routines.forEach { r ->
                FocusCard(onClick = { onOpenRoutine(r.id) }) {
                    Text(r.name, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("${statusLabel(r.status)} · ${WorkoutRules.trainingDayCount(r)} training day(s)", color = C.Text2, fontSize = 12.sp)
                }
            }
        }

        Caps("Workouts")
        if (hub.templates.isEmpty()) {
            FocusCard { Text("No saved workouts yet. Create one, then reuse it in any routine.", color = C.Text2, fontSize = 13.sp) }
        } else {
            hub.templates.forEach { t ->
                FocusCard(onClick = { onOpenTemplate(t.id) }) {
                    Text(t.name, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(exerciseCountLine(t), color = C.Text2, fontSize = 12.sp)
                }
            }
        }

        Caps("Recent")
        if (hub.recent.isEmpty()) {
            FocusCard { Text("Completed workouts will appear here.", color = C.Text2, fontSize = 13.sp) }
        } else {
            hub.recent.forEach { s ->
                FocusCard {
                    Text(s.templateName, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("${s.date} · ${durationText(s)} · ${tonnesText(WorkoutMath.tonnageKg(s))} t", color = C.Text2, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroCard(hub: HubState) {
    val active = hub.active
    FocusCard(modifier = Modifier.fillMaxWidth()) {
        Caps("Today")
        if (active == null) {
            Text("No active routine", color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text("Activate a routine to see today's training here.", color = C.Text2, fontSize = 13.sp)
            return@FocusCard
        }
        Text(hub.todayTitle ?: active.name, color = C.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(hub.todayDetail ?: active.name, color = C.Text2, fontSize = 13.sp)
        val frac = hub.weekProgress
        if (frac == null) {
            Text("Week progress needs a fixed weekday schedule.", color = C.Text3, fontSize = 12.sp)
        } else {
            Text("${(frac * 100).toInt()}% of this week's planned sessions", color = C.Text2, fontSize = 12.sp)
            ProgBar(frac)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, unit: String, modifier: Modifier) {
    FocusCard(modifier = modifier) {
        Caps(label)
        Text(value, color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(unit, color = C.Text2, fontSize = 11.sp)
    }
}

internal data class HubState(
    val templates: List<WorkoutTemplate>,
    val routines: List<WorkoutRoutine>,
    val active: WorkoutRoutine?,
    val todayTitle: String?,
    val todayDetail: String?,
    /** Null when the routine has no fixed-weekday schedule. */
    val weekProgress: Float?,
    val weekSessions: Int,
    val weekTonnageKg: Double,
    val weekLabel: String,
    val recent: List<WorkoutSession>,
    val error: String?,
)

internal fun loadHub(repo: WorkoutRepository): HubState = try {
    val today = AppClock.today()
    val weekStart = WorkoutMath.weekStart(today)
    val weekEnd = weekStart.plusDays(6)
    val sessions = repo.sessions()
    val routines = repo.routines()
    val templates = repo.templates(includeArchived = false)
    val active = routines.firstOrNull { it.status == RoutineStatus.ACTIVE }
    val week = WorkoutMath.completedIn(sessions, weekStart, weekEnd)

    val templateById = templates.associateBy { it.id }
    val (title, detail, progress) = if (active == null) Triple(null, null, null) else heroFor(active, sessions, today, weekStart, weekEnd, templateById)

    HubState(
        templates = templates,
        routines = routines,
        active = active,
        todayTitle = title,
        todayDetail = detail,
        weekProgress = progress,
        weekSessions = week.size,
        weekTonnageKg = week.sumOf { WorkoutMath.tonnageKg(it) },
        weekLabel = "${weekStart.format(DATE_FMT)} – ${weekEnd.format(DATE_FMT)}",
        recent = sessions.filter { it.status == SessionStatus.COMPLETED }
            .sortedWith(compareByDescending<WorkoutSession> { it.date }.thenByDescending { it.completedAtMs ?: 0L })
            .take(3),
        error = null,
    )
} catch (e: Exception) {
    HubState(emptyList(), emptyList(), null, null, null, null, 0, 0.0, "", emptyList(),
        "Couldn't load your workouts. ${e.message ?: "Try again."}")
}

/** Title, detail and week progress for the active routine. Progress is null for flexible routines. */
private fun heroFor(r: WorkoutRoutine, sessions: List<WorkoutSession>, today: LocalDate, weekStart: LocalDate, weekEnd: LocalDate, templateById: Map<String, WorkoutTemplate>):
    Triple<String?, String?, Float?> {
    if (!WorkoutRules.isFixedSchedule(r)) {
        val next = WorkoutMath.nextFlexibleDay(r, sessions)
        return Triple(next?.label ?: "No training days yet", "${r.name} · next in rotation", null)
    }
    val day: RoutineDay? = WorkoutMath.dayForDate(r, today)
    val title = when {
        day == null -> "Nothing scheduled today"
        day.isRestDay -> "Rest day"
        else -> day.label
    }
    val detail = when {
        day == null || day.isRestDay -> r.name
        else -> "${r.name} · ${exerciseCountFor(day, templateById)}"
    }
    val adh = WorkoutMath.adherence(r, sessions, weekStart, weekEnd, today) as? WorkoutMath.Adherence.Scheduled
    val planned = adh?.slots?.size ?: 0
    val progress = if (adh == null || planned == 0) null else adh.completed.toFloat() / planned
    return Triple(title, detail, progress)
}

private fun exerciseCountFor(day: RoutineDay, templateById: Map<String, WorkoutTemplate>): String {
    val id = day.templateId ?: return "no workout assigned"
    val t = templateById[id] ?: return "workout no longer exists"
    return "${t.exercises.size} exercise(s)"
}

private val DATE_FMT = java.time.format.DateTimeFormatter.ofPattern("d MMM")

internal fun tonnesText(kg: Double): String = String.format("%.1f", kg / 1000.0)

private fun durationText(s: WorkoutSession): String {
    val end = s.completedAtMs ?: return "—"
    val minutes = ((end - s.startedAtMs) / 60_000L).coerceAtLeast(0)
    return "$minutes min"
}

internal fun statusLabel(s: RoutineStatus) = when (s) {
    RoutineStatus.DRAFT -> "Draft"
    RoutineStatus.ACTIVE -> "Active"
    RoutineStatus.PAUSED -> "Paused"
    RoutineStatus.ARCHIVED -> "Archived"
}

internal fun exerciseCountLine(t: WorkoutTemplate): String =
    if (t.exercises.isEmpty()) "No exercises yet" else "${t.exercises.size} exercise(s)"
