package com.pranav.focus.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pranav.focus.data.Store
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

/** One planned block on the agenda. [start] and [dur] are minutes (start = minutes since midnight). */
data class PlanTask(
    val id: String,
    val date: String,
    val title: String,
    val category: String,   // Study | Workout | Personal | Other
    val subject: String,    // Physics | Chemistry | Biology | Workout | Personal | Other
    val start: Int,
    val dur: Int,
    val priority: Int = 1,  // 0 Low, 1 Medium, 2 High
    val notes: String = "",
    val done: Boolean = false,
    val checklist: List<Pair<String, Boolean>> = emptyList(),
) {
    val end get() = start + dur
    fun overlaps(o: PlanTask) = id != o.id && date == o.date && start < o.end && o.start < end
}

val PLAN_CATEGORIES = listOf("Study", "Workout", "Personal", "Other")
val PLAN_SUBJECTS = listOf("Physics", "Chemistry", "Biology")
val PLAN_PRIORITIES = listOf("Low", "Medium", "High")
val PLAN_SUBJECT_ORDER = listOf("Physics", "Chemistry", "Biology", "Workout", "Personal", "Other")
const val PLAN_DAY_MINUTES = 14 * 60 // planning window used by the "planned / 14h" meter

fun fmtT(m: Int): String {
    val h = (m / 60) % 24
    val ap = if (h < 12) "AM" else "PM"
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%02d:%02d %s".format(h12, m % 60, ap)
}

fun fmtD(m: Int): String = "${m / 60}h ${m % 60}m"

object PlanState {
    var tasks by mutableStateOf<List<PlanTask>>(emptyList())
    var date by mutableStateOf(LocalDate.now())
    var selectedId by mutableStateOf<String?>(null)
    private var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val raw = Store.get(ctx).str("plan_tasks", "")
        tasks = if (raw.isBlank()) seed() else runCatching { decode(raw) }.getOrDefault(seed())
        if (raw.isBlank()) save(ctx)
    }

    fun save(ctx: Context) = Store.get(ctx).putStr("plan_tasks", encode(tasks))

    fun upsert(ctx: Context, t: PlanTask) {
        tasks = if (tasks.any { it.id == t.id }) tasks.map { if (it.id == t.id) t else it } else tasks + t
        save(ctx)
    }

    fun delete(ctx: Context, id: String) { tasks = tasks.filter { it.id != id }; save(ctx) }

    fun get(id: String?) = tasks.firstOrNull { it.id == id }

    fun forDate(d: LocalDate = date) = tasks.filter { it.date == d.toString() }.sortedBy { it.start }

    fun newId() = UUID.randomUUID().toString()

    /** SAMPLE data from the A18 concept board (kept distinct from real data: it is only seeded on first open). */
    private fun seed(): List<PlanTask> {
        val d = LocalDate.now().toString()
        fun t(title: String, cat: String, subj: String, h: Int, m: Int, dur: Int, pr: Int = 1, done: Boolean = false, notes: String = "", cl: List<Pair<String, Boolean>> = emptyList()) =
            PlanTask(newId(), d, title, cat, subj, h * 60 + m, dur, pr, notes, done, cl)
        return listOf(
            t("Run", "Workout", "Workout", 6, 0, 45, done = true),
            t("Breakfast", "Personal", "Personal", 7, 30, 30, done = true),
            t("Physics Practice", "Study", "Physics", 9, 0, 120, notes = "Chapter 5 - PYQs\nRevise key formulas",
                cl = listOf("Revise key formulas" to true, "Solve 20 PYQs" to true, "Review mistakes" to false, "Summarise notes" to false, "Weak topics" to false)),
            t("Break", "Personal", "Personal", 11, 30, 30),
            t("Chemistry", "Study", "Chemistry", 12, 0, 90),
            t("Lunch", "Personal", "Personal", 13, 0, 60),
            t("Gym (Chest)", "Workout", "Workout", 14, 0, 60, 2, notes = "Chest + Triceps\nProgressive overload"),
            t("Biology", "Study", "Biology", 16, 0, 120),
            t("NEET Questions", "Study", "Physics", 19, 0, 60),
            t("Dinner", "Personal", "Personal", 21, 0, 60),
            t("Revision", "Study", "Other", 22, 0, 30),
        )
    }

    private fun encode(l: List<PlanTask>) = JSONArray(l.map {
        JSONObject().put("id", it.id).put("date", it.date).put("title", it.title).put("cat", it.category).put("subj", it.subject)
            .put("start", it.start).put("dur", it.dur).put("pr", it.priority).put("notes", it.notes).put("done", it.done)
            .put("cl", JSONArray(it.checklist.map { c -> JSONObject().put("t", c.first).put("d", c.second) }))
    }).toString()

    private fun decode(s: String): List<PlanTask> {
        val a = JSONArray(s)
        return (0 until a.length()).map {
            val o = a.getJSONObject(it)
            val cl = o.optJSONArray("cl")
            PlanTask(
                o.getString("id"), o.getString("date"), o.getString("title"), o.getString("cat"), o.getString("subj"),
                o.getInt("start"), o.getInt("dur"), o.optInt("pr", 1), o.optString("notes"), o.optBoolean("done"),
                if (cl == null) emptyList() else (0 until cl.length()).map { i -> cl.getJSONObject(i).let { c -> c.getString("t") to c.optBoolean("d") } },
            )
        }
    }
}

/** Planner preferences (A18_11), persisted in the app Store. */
object PlanPrefs {
    fun showDone(c: Context) = Store.get(c).bool("plan_show_done", true)
    fun showFree(c: Context) = Store.get(c).bool("plan_show_free", true)
    fun quickHome(c: Context) = Store.get(c).bool("plan_quick_home", true)
    fun defStart(c: Context) = Store.get(c).int("plan_def_start", 540)
    fun defDur(c: Context) = Store.get(c).int("plan_def_dur", 60)
    fun remind(c: Context) = Store.get(c).int("plan_remind", 10)
}
