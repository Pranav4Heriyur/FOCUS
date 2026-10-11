package com.pranav.focus.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pranav.focus.R
import com.pranav.focus.data.AgendaItem
import com.pranav.focus.data.Entry
import com.pranav.focus.data.Fmt
import com.pranav.focus.data.Ranks
import com.pranav.focus.data.Rewards
import com.pranav.focus.data.Store
import com.pranav.focus.data.Weekly
import com.pranav.focus.lock.AppState
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun agendaIcon(k: String) = when (k) {
    "Physics" -> R.drawable.ic_atr
    "Chemistry" -> R.drawable.ic_science
    "Biology" -> R.drawable.ic_eco
    "Workout" -> R.drawable.ic_fitness_center
    else -> R.drawable.ic_shield
}

// --------------------------------------------------------------------------------------
// A11 Accountability Mirror
// --------------------------------------------------------------------------------------
@Composable
fun MirrorScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var agenda by remember { mutableStateOf(store.agenda) }
    var done by remember { mutableStateOf(store.reviewDone()) }
    var submitted by remember { mutableStateOf(store.reviewSubmitted()) }
    var avoided by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    var faced by remember { mutableStateOf(store.avoided) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        ScreenHeader("Accountability Mirror", back, "Be honest. Nobody else is watching.")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Today’s agenda", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (edit) "Done" else "Edit", color = C.Text2, fontSize = 14.sp, modifier = Modifier.clickable { edit = !edit }.padding(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        agenda.forEach { a ->
            val isDone = a.id in done
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(agendaIcon(a.icon), active = isDone, size = 44.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(a.title, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text(a.sub, color = C.Text2, fontSize = 13.sp)
                    }
                    if (edit) {
                        Ico(R.drawable.ic_delete, 22.dp, C.Text3, Modifier.clickable {
                            agenda = agenda.filter { it.id != a.id }; store.agenda = agenda
                        }.padding(4.dp))
                    } else {
                        Text(if (isDone) "Done" else "Not done", color = if (isDone) C.Black else C.Text2, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(CircleShape).background(if (isDone) C.White else C.Track)
                                .clickable(enabled = !submitted) {
                                    done = if (isDone) done - a.id else done + a.id
                                    store.putReviewDone(done)
                                }.padding(horizontal = 14.dp, vertical = 8.dp))
                    }
                }
            }
        }
        if (edit) GhostButton("Add item", icon = R.drawable.ic_add) { adding = true }
        Spacer(Modifier.height(16.dp))
        Text("One thing you avoided today", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            avoided, { if (it.length <= 300) avoided = it }, Modifier.fillMaxWidth().height(110.dp),
            placeholder = { Text("What did you dodge?") }, colors = fieldColors(), shape = RoundedCornerShape(16.dp),
        )
        Text("${avoided.length}/300", color = C.Text3, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(8.dp))
        GhostButton("Face it", enabled = avoided.isNotBlank()) {
            faced = listOf(Entry(UUID.randomUUID().toString(), System.currentTimeMillis(), avoided.trim(), "")) + faced
            store.avoided = faced; avoided = ""
        }
        faced.take(3).forEach {
            Text("• ${it.title}", color = C.Text2, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(20.dp))
        val missed = agenda.count { it.id !in done }
        if (submitted) {
            Text("Review submitted for today.", color = C.Text2, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        } else {
            PrimaryButton("Submit review") {
                val tomorrow = LocalDate.now().plusDays(1).toString()
                val d = store.day(tomorrow)
                d.penalty += missed * Rewards.MISSED_ITEM_PENALTY_MIN
                store.saveDay(d, tomorrow)
                store.xp += (agenda.size - missed) * Rewards.REVIEW_ITEM_XP
                store.putReviewSubmitted(); submitted = true
                AppState.dataTick++
            }
            Text(if (missed == 0) "Everything done. +${agenda.size * Rewards.REVIEW_ITEM_XP} XP."
                else "$missed missed = −${missed * Rewards.MISSED_ITEM_PENALTY_MIN} min from tomorrow’s Time Bank.",
                color = C.Text3, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(32.dp))
    }
    if (adding) {
        var t by remember { mutableStateOf("") }
        var sub by remember { mutableStateOf("") }
        FocusSheet({ adding = false }) {
            Text("New item", color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Title") }, colors = fieldColors())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(sub, { sub = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Detail") }, colors = fieldColors())
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Add", enabled = t.isNotBlank()) {
                agenda = agenda + AgendaItem(UUID.randomUUID().toString(), t.trim(), sub.trim(), "Other"); store.agenda = agenda; adding = false
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// A12 Cookie Jar
// --------------------------------------------------------------------------------------
@Composable
fun JarScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var items by remember { mutableStateOf(store.jar) }
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        ScreenHeader("Cookie Jar", back, "Proof you’ve done hard things. Read it when you want to quit.")
        if (items.isEmpty()) Text("Empty. Add your first win.", color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
        items.forEach { e ->
            FocusCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(e.title, color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        if (e.note.isNotBlank()) Text(e.note, color = C.Text2, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                        Text(java.time.Instant.ofEpochMilli(e.dateMs).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(DATE_FMT),
                            color = C.Text3, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                    Ico(R.drawable.ic_delete, 20.dp, C.Text3, Modifier.clickable { items = items.filter { it.id != e.id }; store.jar = items }.padding(4.dp))
                }
            }
        }
        PrimaryButton("Add to the jar") { adding = true }
        Spacer(Modifier.height(100.dp))
    }
    if (adding) {
        var t by remember { mutableStateOf("") }
        var n by remember { mutableStateOf("") }
        FocusSheet({ adding = false }) {
            Text("New win", color = C.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(t, { t = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("What did you do?") }, colors = fieldColors())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(n, { n = it }, Modifier.fillMaxWidth(), label = { Text("Why it mattered") }, colors = fieldColors())
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Save", enabled = t.isNotBlank()) {
                items = listOf(Entry(UUID.randomUUID().toString(), System.currentTimeMillis(), t.trim(), n.trim())) + items
                store.jar = items; adding = false
            }
        }
    }
}

// --------------------------------------------------------------------------------------
// A13 Rank
// --------------------------------------------------------------------------------------
@Composable
fun RankScreen(back: () -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    remember { Weekly.rollover(store) }
    val xp = store.xp
    val cur = Ranks.forXp(xp)
    val nxt = Ranks.next(xp)
    val w = remember { Weekly.stats(store, Weekly.weekStart()) }
    val t = Weekly.target(store.targetScale)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        ScreenHeader("Rank", back)
        FocusCard(Modifier.fillMaxWidth()) {
            Caps("Current rank")
            Text(cur.name, color = C.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text(cur.tagline, color = C.Text2, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Bar(Ranks.progress(xp))
            Spacer(Modifier.height(6.dp))
            Text(if (nxt != null) "$xp / ${nxt.minXp} XP to ${nxt.name}" else "$xp XP · top rank", color = C.Text2, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text("This week", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(if (store.targetScale > 1f) "Targets scaled to ${(store.targetScale * 100).toInt()}%" else "Hit every target to raise the bar 5%.", color = C.Text2, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Target("Focus", "${Fmt.hm(w.focusMin)} / ${Fmt.hm(t.focusMin)}", w.focusMin.toFloat() / t.focusMin)
            Target("Runs", "${w.runs} / ${t.runs}", w.runs.toFloat() / t.runs)
            Target("Workouts", "${w.workouts} / ${t.workouts}", w.workouts.toFloat() / t.workouts)
            Target("Questions", "${w.questions} / ${t.questions}", w.questions.toFloat() / t.questions)
        }
        Spacer(Modifier.height(20.dp))
        Text("Ladder", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Ranks.ALL.forEach { r ->
            val reached = xp >= r.minXp
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(if (reached) C.White else C.Track))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(r.name, color = if (reached) C.White else C.Text3, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Text(r.tagline, color = C.Text3, fontSize = 12.sp)
                }
                Text("${r.minXp} XP", color = C.Text3, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun Bar(frac: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(C.Track)) {
        Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(8.dp).clip(CircleShape).background(C.White))
    }
}

@Composable
private fun Target(name: String, value: String, frac: Float) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Row { Text(name, color = C.White, fontSize = 15.sp, modifier = Modifier.weight(1f)); Text(value, color = C.Text2, fontSize = 13.sp) }
        Spacer(Modifier.height(6.dp))
        Bar(frac)
    }
}

// --------------------------------------------------------------------------------------
// A14 Stats
// --------------------------------------------------------------------------------------
private fun buckets(range: Int, f: (LocalDate) -> Double): Pair<List<Float>, List<String>> {
    val today = LocalDate.now()
    return when (range) {
        0 -> {
            val s = Weekly.weekStart()
            (0..6).map { f(s.plusDays(it.toLong())).toFloat() } to listOf("M", "T", "W", "T", "F", "S", "S")
        }
        1 -> {
            val days = (29 downTo 0).map { today.minusDays(it.toLong()) }
            days.map { f(it).toFloat() } to days.map { if (it.dayOfMonth % 5 == 0) "${it.dayOfMonth}" else "" }
        }
        else -> {
            val months = (11 downTo 0).map { today.withDayOfMonth(1).minusMonths(it.toLong()) }
            months.map { m -> (0 until m.lengthOfMonth()).sumOf { f(m.plusDays(it.toLong())) }.toFloat() } to
                months.map { it.month.name.take(1) }
        }
    }
}

@Composable
private fun BarChart(values: List<Float>, labels: List<String>, height: Int = 110) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(0.0001f)
    Canvas(Modifier.fillMaxWidth().height(height.dp)) {
        val n = values.size
        val gap = 4.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        values.forEachIndexed { i, v ->
            val x = i * (bw + gap)
            drawRoundRect(C.Track, Offset(x, 0f), Size(bw, size.height), CornerRadius(bw / 3))
            val h = (size.height * (v / max)).coerceAtLeast(if (v > 0) 3.dp.toPx() else 0f)
            drawRoundRect(Color.White, Offset(x, size.height - h), Size(bw, h), CornerRadius(bw / 3))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        labels.forEach { Text(it, color = C.Text3, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1) }
    }
}

private val QUOTES = listOf(
    "Comfort is the thief. Show up anyway.",
    "Nobody is coming. Do the work.",
    "You don’t need motivation. You need a start.",
    "Hard today, free tomorrow.",
    "The standard is the standard.",
    "Be the person your excuses are afraid of.",
    "One more rep. One more page. One more minute.",
)

@Composable
fun StatsScreen(go: (com.pranav.focus.ui.Screen) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var range by remember { mutableIntStateOf(0) }
    val tick = AppState.dataTick
    val focus = remember(range, tick) { val m = store.focusDays(); buckets(range) { (m[it.toString()] ?: 0).toDouble() } }
    val acts = remember(tick) { store.activities }
    val dist = remember(range, tick) { buckets(range) { d -> acts.filter { it.startDate == d.toString() && (it.isRun || it.isRide) }.sumOf { it.distM } / 1000.0 } }
    val wk = remember(range, tick) { buckets(range) { d -> acts.count { it.startDate == d.toString() && it.isWorkout } + store.int("workouts_$d").toDouble() } }
    val apps = remember(tick) { if (UsageHelper.hasAccess(ctx)) UsageHelper.today(ctx).take(5) else emptyList() }
    val focusDays = remember(tick) { store.focusDays() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 100.dp)) {
        ScreenHeader("Stats", null)
        SegFull(listOf("Week", "Month", "Year"), range) { range = it }
        Spacer(Modifier.height(16.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row { Text("Focus", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(Fmt.hm(focus.first.sum().toInt()), color = C.Text2, fontSize = 14.sp) }
            Spacer(Modifier.height(10.dp))
            BarChart(focus.first, focus.second)
        }
        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row { Text("Distance", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(Fmt.km(dist.first.sum() * 1000.0, store.imperial) + if (store.imperial) " mi" else " km", color = C.Text2, fontSize = 14.sp) }
            Spacer(Modifier.height(10.dp))
            BarChart(dist.first, dist.second, 90)
            if (acts.isEmpty()) Text("Connect Strava to fill this in.", color = C.Text3, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row { Text("Workouts", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${wk.first.sum().toInt()}", color = C.Text2, fontSize = 14.sp) }
            Spacer(Modifier.height(10.dp))
            BarChart(wk.first, wk.second, 80)
        }
        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Text("Screen time today", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            if (apps.isEmpty()) Text("Allow usage access in Permissions to see this.", color = C.Text3, fontSize = 13.sp)
            else {
                val total = apps.sumOf { it.ms }.toFloat()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(110.dp)) {
                        var start = -90f
                        val w = 16.dp.toPx()
                        apps.forEachIndexed { i, a ->
                            val sweep = 360f * a.ms / total
                            drawArc(Color.White.copy(alpha = 1f - i * 0.17f), start, (sweep - 2f).coerceAtLeast(1f), false,
                                Offset(w / 2, w / 2), Size(size.width - w, size.height - w), style = Stroke(w, cap = StrokeCap.Butt))
                            start += sweep
                        }
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        apps.forEachIndexed { i, a ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 1f - i * 0.17f)))
                                Text("${a.label} ${UsageHelper.format(a.ms)}", color = C.Text2, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row { Text("Focus streak", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${store.streak} days", color = C.Text2, fontSize = 14.sp) }
            Spacer(Modifier.height(10.dp))
            val start = Weekly.weekStart().minusWeeks(11)
            Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                val cell = (size.width - 11 * 4.dp.toPx()) / 12
                for (c in 0 until 12) for (r in 0 until 7) {
                    val d = start.plusDays((c * 7 + r).toLong())
                    if (d.isAfter(LocalDate.now())) continue
                    val m = focusDays[d.toString()] ?: 0
                    val a = when { m <= 0 -> 0.08f; m < 30 -> 0.35f; m < 90 -> 0.65f; else -> 1f }
                    drawRoundRect(Color.White.copy(alpha = a), Offset(c * (cell + 4.dp.toPx()), r * (cell.coerceAtMost(14.dp.toPx()) + 2.dp.toPx())),
                        Size(cell, cell.coerceAtMost(14.dp.toPx())), CornerRadius(3.dp.toPx()))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Text("“${QUOTES[LocalDate.now().dayOfYear % QUOTES.size]}”", color = C.White, fontSize = 18.sp, fontWeight = FontWeight.Light, lineHeight = 26.sp)
        }
        Spacer(Modifier.height(12.dp))
        NavRow(R.drawable.ic_emoji_events, "Rank", Ranks.forXp(store.xp).name) { go(Screen.Rank) }
        NavRow(R.drawable.ic_visibility_off, "Accountability Mirror", "Review today") { go(Screen.Mirror) }
        NavRow(R.drawable.ic_redeem, "Cookie Jar", "${store.jar.size} wins") { go(Screen.Jar) }
    }
}

@Composable
fun NavRow(icon: Int, title: String, sub: String, onClick: () -> Unit) {
    FocusCard(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = onClick) {
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

// --------------------------------------------------------------------------------------
// A15 Profile
// --------------------------------------------------------------------------------------
private fun exportJson(ctx: Context): String {
    val o = JSONObject()
    Store.get(ctx).sp.all.forEach { (k, v) -> o.put(k, if (v is Set<*>) org.json.JSONArray(v.toList()) else v) }
    return o.toString(2)
}

@Composable
fun ProfileScreen2(go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val store = remember { Store.get(ctx) }
    var watch by remember { mutableStateOf(store.watchMe) }
    var tone by remember { mutableIntStateOf(store.tone) }
    var units by remember { mutableIntStateOf(if (store.imperial) 1 else 0) }
    var lockOn by remember { mutableStateOf(store.restrictionsEnabled) }
    var confirmReset by remember { mutableStateOf(false) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) runCatching { ctx.contentResolver.openOutputStream(uri)?.use { it.write(exportJson(ctx).toByteArray()) } }
    }
    val rank = Ranks.forXp(store.xp)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 100.dp)) {
        ScreenHeader("Profile", null)
        FocusCard(Modifier.fillMaxWidth(), onClick = { go(Screen.Rank) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(C.Sheet), contentAlignment = Alignment.Center) {
                    Text("P", color = C.White, fontSize = 26.sp, fontWeight = FontWeight.Light)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("Pranav", color = C.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("${rank.name} · ${store.xp} XP", color = C.Text2, fontSize = 14.sp)
                }
                Ico(R.drawable.ic_chevron_right, 22.dp, C.Text3)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileStat("${store.streak}", "Day streak")
                ProfileStat("${store.bankMinutes}m", "Time bank")
                ProfileStat(Fmt.hm(store.totalFocusMinutes), "Focused")
            }
        }
        Spacer(Modifier.height(16.dp))
        Caps("Connections"); Spacer(Modifier.height(6.dp))
        ProfileRow(R.drawable.ic_link, "Connections", "Strava · Notion · Health") { go(Screen.Connections) }
        Spacer(Modifier.height(10.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(R.drawable.ic_watch)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text("Watch worn by me", color = C.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Turn off when someone else wears it.", color = C.Text2, fontSize = 13.sp)
                }
                FocusSwitch(watch) { watch = it; store.watchMe = it }
            }
        }
        Spacer(Modifier.height(16.dp))
        Caps("Focus"); Spacer(Modifier.height(6.dp))
        ProfileRow(R.drawable.ic_hourglass_top, "Time Bank", "${store.bankMinutes} min available") { go(Screen.TimeBank) }
        Spacer(Modifier.height(10.dp))
        ProfileRow(R.drawable.ic_tune, "App rules", "${store.appRules.size} limits · ${store.unlockRules.size} unlock rules") { go(Screen.Rules) }
        Spacer(Modifier.height(10.dp))
        ProfileRow(R.drawable.ic_grid_view, "Allowed apps", "${store.allowedApps.size} chosen + Phone") { go(Screen.Apps) }
        Spacer(Modifier.height(10.dp))
        ProfileRow(R.drawable.ic_call, "Allowed callers", "${store.callers.size} numbers") { go(Screen.Callers) }
        Spacer(Modifier.height(16.dp))
        Caps("Permissions & security"); Spacer(Modifier.height(6.dp))
        ProfileRow(R.drawable.ic_shield, "Permissions", if (AppState.coreReady) "Lock ready" else "Action needed") { go(Screen.Setup) }
        Spacer(Modifier.height(10.dp))
        ProfileRow(R.drawable.ic_fingerprint, "Security", if (store.biometricLock) "Biometric lock on" else "App lock off") { go(Screen.Security) }
        Spacer(Modifier.height(16.dp))
        Caps("Preferences"); Spacer(Modifier.height(6.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Text("Tone", color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SegFull(listOf("Normal", "Hard", "Goggins"), tone) { tone = it; store.tone = it }
            Spacer(Modifier.height(14.dp))
            Text("Units", color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            SegFull(listOf("Metric", "Imperial"), units) { units = it; store.imperial = it == 1 }
        }
        Spacer(Modifier.height(10.dp))
        ProfileRow(R.drawable.ic_inventory_2, "Export data", "Save everything as JSON") { export.launch("focus-export.json") }
        Spacer(Modifier.height(10.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Text("About", color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text("Focus 1.0 · private build · all data stays on this phone.", color = C.Text2, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
        Caps("Danger zone"); Spacer(Modifier.height(6.dp))
        FocusCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("App lock enabled", color = C.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Off pauses all app rules and blocks.", color = C.Text2, fontSize = 13.sp)
                }
                FocusSwitch(lockOn) { lockOn = it; store.restrictionsEnabled = it }
            }
        }
        Spacer(Modifier.height(10.dp))
        GhostButton("Reset all data", icon = R.drawable.ic_delete) { confirmReset = true }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        containerColor = C.Sheet,
        title = { Text("Reset everything?", color = C.White) },
        text = { Text("XP, streak, rules, jar and history are erased. This can’t be undone.", color = C.Text2) },
        confirmButton = { TextButton({ store.resetAll(); confirmReset = false; AppState.dataTick++; AppState.refresh(ctx) }) { Text("Reset", color = C.White) } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel", color = C.Text2) } },
    )
}
