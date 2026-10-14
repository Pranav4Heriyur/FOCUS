@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.pranav.focus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.pranav.focus.data.AppClock
import com.pranav.focus.data.BodyFace
import com.pranav.focus.data.EffectiveLoad
import com.pranav.focus.data.MuscleAtlas
import com.pranav.focus.data.MuscleGroup
import com.pranav.focus.data.MuscleStatus
import com.pranav.focus.data.Store
import com.pranav.focus.data.WeeklyMuscleLoad
import com.pranav.focus.data.WorkoutDb
import com.pranav.focus.data.WorkoutLogic
import kotlin.math.roundToInt

private fun dayStartMs(daysBack: Long) = AppClock.today().minusDays(daysBack).atStartOfDay(AppClock.zone()).toInstant().toEpochMilli()

/** C22 Muscle balance: weekly hard-set equivalents (last 7 days) from saved sessions only. Unknown (no sessions at all) is not zero. */
@Composable
fun BalanceScreen(db: WorkoutDb, nav: WNav) {
    var selected by remember { mutableStateOf<MuscleGroup?>(null) }
    val data = remember {
        val credits = db.exercises().associate { it.id to it.credits }
        val ref = WorkoutLogic.bestE1rm(db.completedSets(sinceTs = dayStartMs(56)))
        val week = db.completedSets(sinceTs = dayStartMs(6))
        val hse = WorkoutLogic.weeklyHardSets(week, credits, ref)
        WorkoutLogic.toLoads(hse, db.hasAnyWorkout())
    }
    val loads = data
    val known = loads.values.any { it.hardSets != null }
    val under = MuscleGroup.values().filter { loads[it]?.status == MuscleStatus.UNDER_TARGET }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("Muscle Balance", { nav.pop() }, "See which muscle groups you're training and where to focus.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1.15f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { MuscleMapView(BodyFace.FRONT, loads, selected, { selected = it }, Modifier.fillMaxWidth()); Text("Front", color = C.Text2, fontSize = 12.sp) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { MuscleMapView(BodyFace.BACK, loads, selected, { selected = it }, Modifier.fillMaxWidth()); Text("Back", color = C.Text2, fontSize = 12.sp) }
                }
            }
            Column(Modifier.weight(0.85f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FocusCard(Modifier.fillMaxWidth(), radius = 18.dp, padding = 10.dp) {
                    Legend("Under target", "Less than 10 sets", 0.22f, false)
                    Legend("On target", "10 – 20 sets", 0.88f, false)
                    Legend("Over target", "More than 20 sets", 0.0f, true)
                }
                FocusCard(Modifier.fillMaxWidth(), radius = 18.dp, padding = 10.dp) {
                    val g = selected
                    if (g == null) Text(if (known) "Tap a muscle for details." else "No sessions yet: every muscle is unknown, not zero.", color = C.Text2, fontSize = 12.sp)
                    else {
                        val l = loads.getValue(g)
                        Text(g.label, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(l.hardSets?.let { "$it sets" } ?: "No data", color = C.White, fontSize = 14.sp)
                        Text(l.label, color = C.Text2, fontSize = 12.sp)
                        Text("Aim for 10 – 20 sets per week.", color = C.Text3, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
        val deep = MuscleAtlas.deep
        Text("DEEP MUSCLES (not visible on the surface, tap to select)", color = C.Text3, fontSize = 10.sp, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            deep.forEach { d -> WChip(d.label.removeSuffix(" (deep)"), selected == d.group) { selected = d.group } }
        }
        if (under.isNotEmpty() && known) {
            Spacer(Modifier.height(12.dp))
            FocusCard(Modifier.fillMaxWidth()) {
                Text("Under target this week", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(under.joinToString(", ") { it.label }, color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth(), radius = 20.dp, padding = 12.dp) {
            Row(Modifier.padding(bottom = 6.dp)) {
                Caps("Muscle group", Modifier.weight(1.3f)); Caps("Weekly hard sets", Modifier.weight(2f)); Caps("Target", Modifier.weight(0.7f), C.Text2)
            }
            MuscleGroup.values().forEach { g ->
                val l = loads.getValue(g)
                Row(
                    Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp)).background(if (selected == g) C.Raised else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selected = g }.padding(vertical = 10.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(g.label, color = C.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1.3f), maxLines = 1)
                    Text(l.hardSets?.toString() ?: "—", color = C.White, fontSize = 15.sp, modifier = Modifier.width(30.dp))
                    SetsBar(l.hardSets, Modifier.weight(1.7f).padding(end = 10.dp))
                    Text("10 – 20", color = C.Text2, fontSize = 12.sp, modifier = Modifier.weight(0.7f), maxLines = 1)
                }
            }
        }
        Text("Counts are hard-set equivalents (near-failure working sets) credited 1.0 to primary and 0.5 to secondary muscles, from saved sessions only.", color = C.Text3, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
    }
}

@Composable
private fun Legend(title: String, sub: String, fillAlpha: Float, ring: Boolean) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(20.dp).clip(CircleShape).background(androidx.compose.ui.graphics.Color.White.copy(alpha = fillAlpha))
                .then(if (ring) Modifier.border(1.5.dp, C.White, CircleShape) else Modifier.border(1.dp, C.Hairline, CircleShape)),
        )
        Spacer(Modifier.width(8.dp))
        Column { Text(title, color = C.White, fontSize = 12.sp, fontWeight = FontWeight.Medium); Text(sub, color = C.Text2, fontSize = 11.sp) }
    }
}

/** Bar on a 0–25 scale with dashed markers at 10 and 20 (the target band). null = unknown, drawn empty. */
@Composable
private fun SetsBar(sets: Int?, modifier: Modifier) {
    androidx.compose.foundation.Canvas(modifier.height(12.dp)) {
        val h = size.height; val r = androidx.compose.ui.geometry.CornerRadius(h / 2)
        drawRoundRect(androidx.compose.ui.graphics.Color(0xFF2A2A2D), size = size, cornerRadius = r)
        val f = ((sets ?: 0) / 25f).coerceIn(0f, 1f)
        if (f > 0f) drawRoundRect(androidx.compose.ui.graphics.Color.White, size = androidx.compose.ui.geometry.Size(size.width * f, h), cornerRadius = r)
        for (m in listOf(10f, 20f)) {
            val x = size.width * m / 25f
            drawLine(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(x, -2f), androidx.compose.ui.geometry.Offset(x, h + 2f), strokeWidth = 1.5f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
        }
    }
}

/** C21/C23/C24: best lifts per exercise (PR board) + fitness/fatigue/form from real session loads. */
@Composable
fun ProgressScreen(db: WorkoutDb, nav: WNav) {
    val ctx = LocalContext.current
    val imperial = remember { Store.get(ctx).imperial }
    val all = remember { db.completedSets() }
    val names = remember { db.exercises().associate { it.id to it.name } }
    val best = remember { WorkoutLogic.bestE1rm(all).entries.sortedByDescending { it.value } }
    val ffr = remember {
        val loads = db.dailyLoads()
        if (loads.isEmpty()) null else {
            val start = loads.keys.minOf { java.time.LocalDate.parse(it) }
            val days = java.time.temporal.ChronoUnit.DAYS.between(start, AppClock.today()).toInt() + 1
            EffectiveLoad.fitnessFatigue((0 until days).map { loads[start.plusDays(it.toLong()).toString()] ?: 0.0 }).last()
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 100.dp)) {
        ScreenHeader("Progress & PRs", { nav.pop() }, "Everything here comes from your saved sessions")
        if (ffr == null) WEmpty("No data yet", "Finish a workout or quick log to see fitness, fatigue and personal bests.")
        else FocusCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCell("${ffr.fitness.roundToInt()}", "Fitness"); StatCell("${ffr.fatigue.roundToInt()}", "Fatigue"); StatCell("${ffr.form.roundToInt()}", "Form")
            }
            Text("Load units (sRPE × minutes, 42/7-day averages). Sessions with no logged effort assume RPE 6.", color = C.Text3, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        WSection("Best estimated 1RM")
        if (best.isEmpty()) WEmpty("No personal bests yet", "Log working sets with weight and reps. Warm-ups and sets over 15 reps don't count.")
        best.forEach { (id, e) ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 8.dp), padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(names[id] ?: id, color = C.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(WorkoutLogic.fmt((WorkoutLogic.toDisplay(e, imperial) * 10).roundToInt() / 10.0) + if (imperial) " lb" else " kg", color = C.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
