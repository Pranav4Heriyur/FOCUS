package com.pranav.focus.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Workout persistence (own SQLite file, framework API like StudyDb). Templates (targets) and sessions (actuals) are separate tables:
 * logging a session never mutates a template and editing a template never rewrites history. Weights are stored in kg.
 */
class WorkoutDb private constructor(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "workout.db", null, 1) {
    companion object {
        @Volatile private var inst: WorkoutDb? = null
        fun get(ctx: Context): WorkoutDb = inst ?: synchronized(this) { inst ?: WorkoutDb(ctx).also { inst = it } }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE exercise(id TEXT PRIMARY KEY, name TEXT, equipment TEXT, pattern TEXT, credits TEXT, bw REAL DEFAULT 0, custom INT DEFAULT 1)")
        db.execSQL("CREATE TABLE template(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, notes TEXT, created INT, updated INT, archived INT)")
        db.execSQL("CREATE TABLE template_ex(id INTEGER PRIMARY KEY AUTOINCREMENT, template_id INT, ord INT, exercise_id TEXT, name TEXT, sets INT, rep_min INT, rep_max INT, kg REAL, rest INT, rpe REAL, notes TEXT)")
        db.execSQL("CREATE TABLE routine(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, descr TEXT, weeks INT, start TEXT, status TEXT, created INT, updated INT)")
        db.execSQL("CREATE TABLE routine_day(id INTEGER PRIMARY KEY AUTOINCREMENT, routine_id INT, ord INT, label TEXT, weekday INT, rest INT, template_id INT)")
        db.execSQL("CREATE TABLE workout(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, start_ts INT, end_ts INT, active INT, verified INT, quick INT, rpe REAL, minutes INT, lu REAL, template_id INT, routine_day_id INT)")
        db.execSQL("CREATE TABLE workout_ex(id INTEGER PRIMARY KEY AUTOINCREMENT, workout_id INT, ord INT, exercise_id TEXT, name TEXT)")
        db.execSQL("CREATE TABLE set_entry(id INTEGER PRIMARY KEY AUTOINCREMENT, wex_id INT, ord INT, kg REAL, reps INT, rpe REAL, type TEXT, done INT, ts INT)")
        db.execSQL("CREATE INDEX idx_set_wex ON set_entry(wex_id)")
        db.execSQL("CREATE INDEX idx_wex_w ON workout_ex(workout_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {}

    // ---- helpers -----------------------------------------------------------------
    private fun Cursor.d(i: Int): Double? = if (isNull(i)) null else getDouble(i)
    private fun Cursor.l(i: Int): Long? = if (isNull(i)) null else getLong(i)
    private fun Cursor.n(i: Int): Int? = if (isNull(i)) null else getInt(i)
    private fun encode(m: Map<String, Double>) = m.entries.joinToString(",") { "${it.key}:${it.value}" }
    private fun decode(s: String?): Map<String, Double> =
        s.orEmpty().split(',').mapNotNull { p -> p.split(':').takeIf { it.size == 2 }?.let { it[0] to (it[1].toDoubleOrNull() ?: return@mapNotNull null) } }.toMap()
    private fun <T> tx(block: (SQLiteDatabase) -> T): T {
        val db = writableDatabase; db.beginTransaction()
        try { val r = block(db); db.setTransactionSuccessful(); return r } finally { db.endTransaction() }
    }

    // ---- exercises ---------------------------------------------------------------
    fun customExercises(): List<Exercise> {
        val out = ArrayList<Exercise>()
        readableDatabase.rawQuery("SELECT id,name,equipment,pattern,credits,bw FROM exercise ORDER BY name", null).use {
            while (it.moveToNext()) out.add(Exercise(it.getString(0), it.getString(1), it.getString(2) ?: "", it.getString(3)?.takeIf { p -> p.isNotEmpty() }, decode(it.getString(4)), it.getDouble(5), true))
        }
        return out
    }
    fun exercises(): List<Exercise> = (WorkoutSeed.ALL + customExercises()).sortedBy { it.name.lowercase() }

    /** muscleKeys: 1 primary (+ optional secondaries at 0.5) using EffectiveLoad keys. */
    fun addCustomExercise(name: String, equipment: String, primary: String, secondary: List<String>): Exercise {
        val credits = LinkedHashMap<String, Double>().apply { put(primary, 1.0); secondary.filter { it != primary }.forEach { put(it, 0.5) } }
        val id = "custom_" + System.currentTimeMillis()
        val ex = Exercise(id, name.trim(), equipment.trim(), null, credits, 0.0, true)
        writableDatabase.insert("exercise", null, ContentValues().apply {
            put("id", id); put("name", ex.name); put("equipment", ex.equipment); put("pattern", ""); put("credits", encode(credits)); put("bw", 0.0); put("custom", 1)
        })
        return ex
    }

    // ---- templates ---------------------------------------------------------------
    private fun readTemplateExercises(db: SQLiteDatabase, tid: Long): List<TemplateExercise> {
        val out = ArrayList<TemplateExercise>()
        db.rawQuery("SELECT id,ord,exercise_id,name,sets,rep_min,rep_max,kg,rest,rpe,notes FROM template_ex WHERE template_id=? ORDER BY ord", arrayOf(tid.toString())).use {
            while (it.moveToNext()) out.add(TemplateExercise(it.getLong(0), tid, it.getInt(1), it.getString(2), it.getString(3), it.getInt(4), it.getInt(5), it.getInt(6), it.d(7), it.n(8), it.d(9), it.getString(10) ?: ""))
        }
        return out
    }
    fun templates(includeArchived: Boolean = false): List<WorkoutTemplate> {
        val db = readableDatabase; val out = ArrayList<WorkoutTemplate>()
        db.rawQuery("SELECT id,name,notes,created,updated,archived FROM template ${if (includeArchived) "" else "WHERE archived IS NULL"} ORDER BY updated DESC", null).use {
            while (it.moveToNext()) out.add(WorkoutTemplate(it.getLong(0), it.getString(1), it.getString(2) ?: "", it.getLong(3), it.getLong(4), it.l(5), emptyList()))
        }
        return out.map { it.copy(exercises = readTemplateExercises(db, it.id)) }
    }
    fun template(id: Long): WorkoutTemplate? = templates(true).firstOrNull { it.id == id }

    /** Insert (id==0) or update; exercises are replaced as one ordered list. Returns the template id. */
    fun saveTemplate(t: WorkoutTemplate): Long = tx { db ->
        val now = System.currentTimeMillis()
        val cv = ContentValues().apply { put("name", t.name.trim()); put("notes", t.notes); put("updated", now) }
        val id: Long
        if (t.id == 0L) { cv.put("created", now); id = db.insert("template", null, cv) }
        else { db.update("template", cv, "id=?", arrayOf(t.id.toString())); id = t.id; db.delete("template_ex", "template_id=?", arrayOf(id.toString())) }
        t.exercises.forEachIndexed { i, e ->
            db.insert("template_ex", null, ContentValues().apply {
                put("template_id", id); put("ord", i); put("exercise_id", e.exerciseId); put("name", e.name); put("sets", e.targetSets)
                put("rep_min", e.repMin); put("rep_max", e.repMax); put("kg", e.targetKg); put("rest", e.restSeconds); put("rpe", e.targetRpe); put("notes", e.notes)
            })
        }
        id
    }
    fun duplicateTemplate(id: Long): Long? { val t = template(id) ?: return null; return saveTemplate(t.copy(id = 0, name = t.name + " copy", archivedAt = null, exercises = t.exercises.map { it.copy(id = 0) })) }
    fun archiveTemplate(id: Long) { writableDatabase.update("template", ContentValues().apply { put("archived", System.currentTimeMillis()) }, "id=?", arrayOf(id.toString())) }
    fun routinesUsingTemplate(id: Long): List<String> {
        val out = ArrayList<String>()
        readableDatabase.rawQuery("SELECT DISTINCT r.name FROM routine r JOIN routine_day d ON d.routine_id=r.id WHERE d.template_id=? AND r.status!='archived'", arrayOf(id.toString())).use { while (it.moveToNext()) out.add(it.getString(0)) }
        return out
    }
    /** Safe delete: clears routine references first (caller already offered archive/cancel). Past sessions keep their own exercise snapshots. */
    fun deleteTemplate(id: Long) = tx { db ->
        db.execSQL("UPDATE routine_day SET template_id=NULL WHERE template_id=?", arrayOf(id))
        db.execSQL("UPDATE workout SET template_id=NULL WHERE template_id=?", arrayOf(id))
        db.delete("template_ex", "template_id=?", arrayOf(id.toString())); db.delete("template", "id=?", arrayOf(id.toString()))
    }

    // ---- routines ----------------------------------------------------------------
    fun routines(): List<WorkoutRoutine> {
        val db = readableDatabase; val out = ArrayList<WorkoutRoutine>()
        db.rawQuery("SELECT id,name,descr,weeks,start,status,created,updated FROM routine ORDER BY updated DESC", null).use {
            while (it.moveToNext()) out.add(WorkoutRoutine(it.getLong(0), it.getString(1), it.getString(2) ?: "", it.n(3), it.getString(4), RoutineStatus.of(it.getString(5)), it.getLong(6), it.getLong(7), emptyList()))
        }
        return out.map { r ->
            val days = ArrayList<RoutineDay>()
            db.rawQuery("SELECT id,ord,label,weekday,rest,template_id FROM routine_day WHERE routine_id=? ORDER BY ord", arrayOf(r.id.toString())).use {
                while (it.moveToNext()) days.add(RoutineDay(it.getLong(0), r.id, it.getInt(1), it.getString(2) ?: "", it.n(3), it.getInt(4) == 1, it.l(5)))
            }
            r.copy(days = days)
        }
    }
    fun routine(id: Long) = routines().firstOrNull { it.id == id }
    fun activeRoutine() = routines().firstOrNull { it.status == RoutineStatus.ACTIVE }

    fun saveRoutine(r: WorkoutRoutine): Long = tx { db ->
        val now = System.currentTimeMillis()
        val cv = ContentValues().apply { put("name", r.name.trim()); put("descr", r.description); put("weeks", r.weeks); put("start", r.startDate); put("status", r.status.code); put("updated", now) }
        val id: Long
        if (r.id == 0L) { cv.put("created", now); id = db.insert("routine", null, cv) }
        else { db.update("routine", cv, "id=?", arrayOf(r.id.toString())); id = r.id }
        // Keep existing day ids where possible so linked sessions stay linked.
        val keep = r.days.map { it.id }.filter { it != 0L }
        val existing = ArrayList<Long>()
        db.rawQuery("SELECT id FROM routine_day WHERE routine_id=?", arrayOf(id.toString())).use { while (it.moveToNext()) existing.add(it.getLong(0)) }
        existing.filter { it !in keep }.forEach { db.delete("routine_day", "id=?", arrayOf(it.toString())) }
        r.days.forEachIndexed { i, d ->
            val dv = ContentValues().apply { put("routine_id", id); put("ord", i); put("label", d.label.trim()); put("weekday", d.weekday); put("rest", if (d.rest) 1 else 0); put("template_id", d.templateId) }
            if (d.id == 0L || d.id !in existing) db.insert("routine_day", null, dv) else db.update("routine_day", dv, "id=?", arrayOf(d.id.toString()))
        }
        id
    }
    /** Only one routine can be active; activating pauses the others. */
    fun setRoutineStatus(id: Long, s: RoutineStatus) = tx { db ->
        if (s == RoutineStatus.ACTIVE) db.execSQL("UPDATE routine SET status='paused' WHERE status='active' AND id!=?", arrayOf(id))
        db.update("routine", ContentValues().apply { put("status", s.code); put("updated", System.currentTimeMillis()) }, "id=?", arrayOf(id.toString()))
    }
    fun duplicateRoutine(id: Long): Long? {
        val r = routine(id) ?: return null
        return saveRoutine(r.copy(id = 0, name = r.name + " copy", status = RoutineStatus.DRAFT, days = r.days.map { it.copy(id = 0, routineId = 0) }))
    }
    fun deleteRoutine(id: Long) = tx { db ->
        db.execSQL("UPDATE workout SET routine_day_id=NULL WHERE routine_day_id IN (SELECT id FROM routine_day WHERE routine_id=?)", arrayOf(id))
        db.delete("routine_day", "routine_id=?", arrayOf(id.toString())); db.delete("routine", "id=?", arrayOf(id.toString()))
    }

    // ---- sessions ----------------------------------------------------------------
    private fun readSets(db: SQLiteDatabase, wexId: Long): List<SetEntry> {
        val out = ArrayList<SetEntry>()
        db.rawQuery("SELECT id,ord,kg,reps,rpe,type,done,ts FROM set_entry WHERE wex_id=? ORDER BY ord,id", arrayOf(wexId.toString())).use {
            while (it.moveToNext()) out.add(SetEntry(it.getLong(0), wexId, it.getInt(1), it.getDouble(2), it.getInt(3), it.d(4), SetType.of(it.getString(5) ?: ""), it.getInt(6) == 1, it.getLong(7)))
        }
        return out
    }
    private fun readWorkouts(where: String, args: Array<String>, order: String, limit: String? = null): List<Workout> {
        val db = readableDatabase; val out = ArrayList<Workout>()
        db.rawQuery("SELECT id,name,start_ts,end_ts,active,verified,quick,rpe,minutes,lu,template_id,routine_day_id FROM workout WHERE $where ORDER BY $order ${limit?.let { "LIMIT $it" } ?: ""}", args).use {
            while (it.moveToNext()) out.add(Workout(it.getLong(0), it.getString(1) ?: "Workout", it.getLong(2), it.getLong(3), it.getInt(4) == 1, it.getInt(5) == 1, it.getInt(6) == 1, it.d(7), it.getInt(8), it.getDouble(9), it.l(10), it.l(11), emptyList()))
        }
        return out.map { w ->
            val exs = ArrayList<WorkoutExercise>()
            db.rawQuery("SELECT id,ord,exercise_id,name FROM workout_ex WHERE workout_id=? ORDER BY ord,id", arrayOf(w.id.toString())).use {
                while (it.moveToNext()) exs.add(WorkoutExercise(it.getLong(0), w.id, it.getInt(1), it.getString(2), it.getString(3), readSets(db, it.getLong(0))))
            }
            w.copy(exercises = exs)
        }
    }
    fun activeWorkout(): Workout? = readWorkouts("active=1", emptyArray(), "id DESC", "1").firstOrNull()
    fun workout(id: Long): Workout? = readWorkouts("id=?", arrayOf(id.toString()), "id").firstOrNull()
    fun history(): List<Workout> = readWorkouts("active=0", emptyArray(), "start_ts DESC")

    /** Start a session. Template targets are COPIED as empty set rows (planned reps pre-filled) so later template edits never touch it. */
    fun startWorkout(name: String, template: WorkoutTemplate?, routineDayId: Long?): Long = tx { db ->
        val now = System.currentTimeMillis()
        val wid = db.insert("workout", null, ContentValues().apply {
            put("name", name); put("start_ts", now); put("end_ts", 0); put("active", 1); put("verified", 0); put("quick", 0); put("minutes", 0); put("lu", 0.0)
            put("template_id", template?.id); put("routine_day_id", routineDayId)
        })
        template?.exercises?.forEachIndexed { i, e ->
            val wex = db.insert("workout_ex", null, ContentValues().apply { put("workout_id", wid); put("ord", i); put("exercise_id", e.exerciseId); put("name", e.name) })
            repeat(e.targetSets) { s ->
                db.insert("set_entry", null, ContentValues().apply {
                    put("wex_id", wex); put("ord", s); put("kg", e.targetKg ?: 0.0); put("reps", e.repMax); put("rpe", e.targetRpe); put("type", "normal"); put("done", 0); put("ts", 0)
                })
            }
        }
        wid
    }
    fun addExerciseToWorkout(wid: Long, ex: Exercise): Long {
        val db = writableDatabase
        var ord = 0
        db.rawQuery("SELECT COALESCE(MAX(ord)+1,0) FROM workout_ex WHERE workout_id=?", arrayOf(wid.toString())).use { if (it.moveToFirst()) ord = it.getInt(0) }
        return db.insert("workout_ex", null, ContentValues().apply { put("workout_id", wid); put("ord", ord); put("exercise_id", ex.id); put("name", ex.name) })
    }
    fun removeExerciseFromWorkout(wexId: Long) = tx { db ->
        db.delete("set_entry", "wex_id=?", arrayOf(wexId.toString())); db.delete("workout_ex", "id=?", arrayOf(wexId.toString()))
    }
    fun addSet(wexId: Long, kg: Double, reps: Int, type: SetType = SetType.NORMAL): Long {
        val db = writableDatabase
        var ord = 0
        db.rawQuery("SELECT COALESCE(MAX(ord)+1,0) FROM set_entry WHERE wex_id=?", arrayOf(wexId.toString())).use { if (it.moveToFirst()) ord = it.getInt(0) }
        return db.insert("set_entry", null, ContentValues().apply { put("wex_id", wexId); put("ord", ord); put("kg", kg); put("reps", reps); put("type", type.code); put("done", 0); put("ts", 0) })
    }
    /** Persist every edit immediately so an in-progress workout survives process death. */
    fun updateSet(id: Long, kg: Double, reps: Int, rpe: Double?, type: SetType, done: Boolean) {
        writableDatabase.update("set_entry", ContentValues().apply {
            put("kg", kg.coerceIn(0.0, 1000.0)); put("reps", reps.coerceIn(0, 100)); put("rpe", rpe); put("type", type.code); put("done", if (done) 1 else 0); put("ts", if (done) System.currentTimeMillis() else 0L)
        }, "id=?", arrayOf(id.toString()))
    }
    fun deleteSet(id: Long) { writableDatabase.delete("set_entry", "id=?", arrayOf(id.toString())) }

    /** Finish: drops unticked sets and empty exercises, stamps verification. Returns false when nothing was logged. */
    fun finishWorkout(id: Long, verified: Boolean, minutes: Int, rpe: Double?): Boolean = tx { db ->
        db.execSQL("DELETE FROM set_entry WHERE done=0 AND wex_id IN (SELECT id FROM workout_ex WHERE workout_id=?)", arrayOf(id))
        db.execSQL("DELETE FROM workout_ex WHERE workout_id=? AND id NOT IN (SELECT DISTINCT wex_id FROM set_entry)", arrayOf(id))
        var n = 0
        db.rawQuery("SELECT COUNT(*) FROM workout_ex WHERE workout_id=?", arrayOf(id.toString())).use { if (it.moveToFirst()) n = it.getInt(0) }
        if (n == 0) { db.delete("workout", "id=?", arrayOf(id.toString())); false }
        else {
            db.update("workout", ContentValues().apply { put("end_ts", System.currentTimeMillis()); put("active", 0); put("verified", if (verified) 1 else 0); put("minutes", minutes); put("rpe", rpe) }, "id=?", arrayOf(id.toString())); true
        }
    }
    fun discardWorkout(id: Long) = deleteWorkout(id)
    /** Editing a finished workout always downgrades it to unverified (no Time Bank, no extra XP). */
    fun markEdited(id: Long) { writableDatabase.execSQL("UPDATE workout SET verified=0 WHERE id=?", arrayOf(id)) }
    fun deleteWorkout(id: Long) = tx { db ->
        db.execSQL("DELETE FROM set_entry WHERE wex_id IN (SELECT id FROM workout_ex WHERE workout_id=?)", arrayOf(id))
        db.delete("workout_ex", "workout_id=?", arrayOf(id.toString())); db.delete("workout", "id=?", arrayOf(id.toString()))
    }
    /** Non-lifting activity: duration + effort -> Load Units. Always unverified. */
    fun saveQuick(name: String, minutes: Int, rpe: Double, startTs: Long): Long = writableDatabase.insert("workout", null, ContentValues().apply {
        put("name", name); put("start_ts", startTs); put("end_ts", startTs + minutes * 60_000L); put("active", 0); put("verified", 0); put("quick", 1)
        put("rpe", rpe); put("minutes", minutes); put("lu", EffectiveLoad.sessionLoad(minutes.toDouble(), rpe = rpe))
    })

    // ---- analytics inputs (all derived from saved sessions) -------------------------
    fun completedSets(sinceTs: Long = 0L, excludeWorkoutId: Long = -1L, beforeTs: Long = Long.MAX_VALUE): List<WorkoutLogic.SetLike> {
        val out = ArrayList<WorkoutLogic.SetLike>()
        readableDatabase.rawQuery(
            "SELECT e.exercise_id,s.kg,s.reps,s.rpe,s.type,w.start_ts FROM set_entry s JOIN workout_ex e ON e.id=s.wex_id JOIN workout w ON w.id=e.workout_id " +
                "WHERE s.done=1 AND w.quick=0 AND w.id!=? AND w.start_ts>=? AND w.start_ts<?", arrayOf(excludeWorkoutId.toString(), sinceTs.toString(), beforeTs.toString())
        ).use { while (it.moveToNext()) out.add(WorkoutLogic.SetLike(it.getString(0), it.getDouble(1), it.getInt(2), it.d(3), SetType.of(it.getString(4) ?: ""), it.getLong(5))) }
        return out
    }
    fun hasAnyWorkout(): Boolean = readableDatabase.rawQuery("SELECT 1 FROM workout WHERE active=0 AND quick=0 LIMIT 1", null).use { it.moveToFirst() }
    /** Last logged working set of an exercise, for prefill. */
    fun lastSet(exerciseId: String): Pair<Double, Int>? =
        readableDatabase.rawQuery(
            "SELECT s.kg,s.reps FROM set_entry s JOIN workout_ex e ON e.id=s.wex_id WHERE e.exercise_id=? AND s.done=1 AND s.type!='warmup' ORDER BY s.ts DESC LIMIT 1", arrayOf(exerciseId)
        ).use { if (it.moveToFirst()) it.getDouble(0) to it.getInt(1) else null }
    /** Sets (kg, reps) of this exercise from its most recent finished session, in set order, for the PREVIOUS column. */
    fun previousSets(exerciseId: String, excludeWorkoutId: Long): List<Pair<Double, Int>> {
        val db = readableDatabase
        var wex = -1L
        db.rawQuery(
            "SELECT e.id FROM workout_ex e JOIN workout w ON w.id=e.workout_id WHERE e.exercise_id=? AND w.active=0 AND w.quick=0 AND w.id!=? " +
                "AND EXISTS(SELECT 1 FROM set_entry s WHERE s.wex_id=e.id AND s.done=1) ORDER BY w.start_ts DESC LIMIT 1", arrayOf(exerciseId, excludeWorkoutId.toString())
        ).use { if (it.moveToFirst()) wex = it.getLong(0) }
        if (wex < 0) return emptyList()
        return readSets(db, wex).filter { it.done }.map { it.weightKg to it.reps }
    }
    fun sessionsForRoutine(routineId: Long, sinceTs: Long = 0L): Int =
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM workout WHERE active=0 AND start_ts>=? AND routine_day_id IN (SELECT id FROM routine_day WHERE routine_id=?)", arrayOf(sinceTs.toString(), routineId.toString())
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    fun dailyLoads(): Map<String, Double> {
        val out = HashMap<String, Double>()
        for (w in history()) { val d = java.time.Instant.ofEpochMilli(w.startTs).atZone(AppClock.zone()).toLocalDate().toString(); out[d] = (out[d] ?: 0.0) + sessionLoad(w) }
        return out
    }
    /** LU for a lifting session: sRPE (logged or 6 assumed-unknown) x minutes. Quick logs store their own LU. */
    fun sessionLoad(w: Workout): Double = if (w.quick) w.lu else EffectiveLoad.srpeLoad(w.rpe ?: 6.0, w.minutes.toDouble())
}
