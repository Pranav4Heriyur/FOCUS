package com.pranav.focus.workout.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.pranav.focus.workout.domain.Exercise
import com.pranav.focus.workout.domain.LoggedSet
import com.pranav.focus.workout.domain.MuscleGroup
import com.pranav.focus.workout.domain.Prescription
import com.pranav.focus.workout.domain.RoutineDay
import com.pranav.focus.workout.domain.RoutineStatus
import com.pranav.focus.workout.domain.SessionExercise
import com.pranav.focus.workout.domain.SessionStatus
import com.pranav.focus.workout.domain.SetType
import com.pranav.focus.workout.domain.TemplateExercise
import com.pranav.focus.workout.domain.WorkoutCatalogue
import com.pranav.focus.workout.domain.WorkoutRoutine
import com.pranav.focus.workout.domain.WorkoutSession
import com.pranav.focus.workout.domain.WorkoutTemplate
import java.time.LocalDate

/**
 * SQLite implementation of [WorkoutRepository] using the framework API, like StudyDb.
 * Every multi-row write runs in one transaction. Foreign keys are enabled on every connection.
 * Built-in exercises are (re)inserted with INSERT OR IGNORE on open, so they exist without touching custom rows.
 */
class WorkoutDb private constructor(ctx: Context) :
    SQLiteOpenHelper(ctx.applicationContext, WorkoutSchema.DB_NAME, null, WorkoutSchema.VERSION), WorkoutRepository {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        WorkoutSchema.STATEMENTS.forEach { db.execSQL(it) }
        seedBuiltins(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 is the first schema. Future versions add migration steps here, in order, inside a transaction.
        throw IllegalStateException("Unexpected workout.db upgrade $oldVersion -> $newVersion")
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        seedBuiltins(db)
    }

    private fun seedBuiltins(db: SQLiteDatabase) {
        for (e in WorkoutCatalogue.BUILTIN) {
            db.execSQL(
                "INSERT OR IGNORE INTO exercise (id, name, primary_muscles, is_custom) VALUES (?, ?, ?, 0)",
                arrayOf<Any?>(e.id, e.name, muscleCsv(e.primaryMuscles)),
            )
        }
    }

    private fun muscleCsv(m: Set<MuscleGroup>) = m.joinToString(",") { it.name }
    private fun parseMuscles(csv: String): Set<MuscleGroup> =
        if (csv.isBlank()) emptySet() else csv.split(",").map { MuscleGroup.valueOf(it) }.toSet()

    private inline fun <T> tx(block: (SQLiteDatabase) -> T): T {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val out = block(db)
            db.setTransactionSuccessful()
            return out
        } catch (e: WorkoutStorageException) {
            throw e
        } catch (e: Exception) {
            throw WorkoutStorageException("Couldn't save workout data", e)
        } finally {
            db.endTransaction()
        }
    }

    private inline fun <T> read(block: (SQLiteDatabase) -> T): T = try {
        block(readableDatabase)
    } catch (e: Exception) {
        throw WorkoutStorageException("Couldn't read workout data", e)
    }

    // ---- exercises ----

    override fun exercises(): List<Exercise> = read { db ->
        db.rawQuery("SELECT id, name, primary_muscles, is_custom FROM exercise ORDER BY is_custom, name", null).use { c ->
            buildList { while (c.moveToNext()) add(c.toExercise()) }
        }
    }

    override fun exercise(id: String): Exercise? = read { db ->
        db.rawQuery("SELECT id, name, primary_muscles, is_custom FROM exercise WHERE id = ?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) c.toExercise() else null
        }
    }

    override fun saveCustomExercise(e: Exercise) {
        require(e.isCustom && e.id.startsWith("custom:")) { "only custom exercises can be saved" }
        tx { db ->
            db.insertWithOnConflict("exercise", null, ContentValues().apply {
                put("id", e.id); put("name", e.name.trim()); put("primary_muscles", muscleCsv(e.primaryMuscles)); put("is_custom", 1)
            }, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    // ---- templates ----

    override fun templates(includeArchived: Boolean): List<WorkoutTemplate> = read { db ->
        val sql = "SELECT id, name, notes, created_ms, updated_ms, archived_ms FROM workout_template" +
            (if (includeArchived) "" else " WHERE archived_ms IS NULL") + " ORDER BY updated_ms DESC"
        db.rawQuery(sql, null).use { c -> buildList { while (c.moveToNext()) add(c.toTemplate(db)) } }
    }

    override fun template(id: String): WorkoutTemplate? = read { db ->
        db.rawQuery("SELECT id, name, notes, created_ms, updated_ms, archived_ms FROM workout_template WHERE id = ?", arrayOf(id)).use { c ->
            if (c.moveToFirst()) c.toTemplate(db) else null
        }
    }

    override fun saveTemplate(t: WorkoutTemplate) = tx { db ->
        val values = ContentValues().apply {
            put("name", t.name); put("notes", t.notes); put("created_ms", t.createdAtMs)
            put("updated_ms", t.updatedAtMs)
            if (t.archivedAtMs == null) putNull("archived_ms") else put("archived_ms", t.archivedAtMs)
        }
        // Update first, insert if missing. INSERT OR REPLACE would delete the row and cascade-delete its exercises.
        val updated = db.update("workout_template", values, "id = ?", arrayOf(t.id))
        if (updated == 0) {
            values.put("id", t.id)
            db.insertOrThrow("workout_template", null, values)
        }
        db.delete("template_exercise", "template_id = ?", arrayOf(t.id))
        t.exercises.sortedBy { it.orderIndex }.forEach { e ->
            db.insertOrThrow("template_exercise", null, ContentValues().apply {
                put("id", e.id); put("template_id", t.id); put("exercise_id", e.exerciseId); put("order_index", e.orderIndex)
                put("target_sets", e.prescription.targetSets); put("rep_min", e.prescription.repMin); put("rep_max", e.prescription.repMax)
                putNullable("target_load_kg", e.prescription.targetLoadKg); putNullable("rest_seconds", e.prescription.restSeconds)
                putNullable("target_rpe", e.prescription.targetRpe); put("notes", e.prescription.notes)
            })
        }
    }

    override fun deleteTemplate(id: String) = tx { db ->
        val referenced = db.rawQuery("SELECT COUNT(*) FROM routine_day WHERE template_id = ?", arrayOf(id)).use { c -> c.moveToFirst() && c.getInt(0) > 0 }
        if (referenced) throw WorkoutStorageException("This workout is still used by a routine day. Remove it from the routine first.")
        db.delete("workout_template", "id = ?", arrayOf(id))
        Unit
    }

    // ---- routines ----

    override fun routines(): List<WorkoutRoutine> = read { db ->
        db.rawQuery("SELECT id, name, description, duration_weeks, start_date, status, created_ms, updated_ms FROM workout_routine ORDER BY updated_ms DESC", null)
            .use { c -> buildList { while (c.moveToNext()) add(c.toRoutine(db)) } }
    }

    override fun routine(id: String): WorkoutRoutine? = read { db ->
        db.rawQuery("SELECT id, name, description, duration_weeks, start_date, status, created_ms, updated_ms FROM workout_routine WHERE id = ?", arrayOf(id))
            .use { c -> if (c.moveToFirst()) c.toRoutine(db) else null }
    }

    override fun saveRoutine(r: WorkoutRoutine) = tx { db ->
        val values = ContentValues().apply {
            put("name", r.name); put("description", r.description)
            putNullable("duration_weeks", r.durationWeeks); putNullable("start_date", r.startDate?.toString())
            put("status", r.status.name); put("created_ms", r.createdAtMs); put("updated_ms", r.updatedAtMs)
        }
        val updated = db.update("workout_routine", values, "id = ?", arrayOf(r.id))
        if (updated == 0) {
            values.put("id", r.id)
            db.insertOrThrow("workout_routine", null, values)
        }
        db.delete("routine_day", "routine_id = ?", arrayOf(r.id))
        r.days.sortedBy { it.orderIndex }.forEach { d ->
            db.insertOrThrow("routine_day", null, ContentValues().apply {
                put("id", d.id); put("routine_id", r.id); put("order_index", d.orderIndex); put("label", d.label)
                put("is_rest", if (d.isRestDay) 1 else 0); putNullable("template_id", if (d.isRestDay) null else d.templateId)
                putNullable("weekday", d.weekday)
            })
        }
    }

    override fun deleteRoutine(id: String) = tx { db ->
        db.delete("workout_routine", "id = ?", arrayOf(id))
        Unit
    }

    // ---- sessions ----

    override fun sessions(): List<WorkoutSession> = read { db ->
        db.rawQuery(SESSION_SELECT + " ORDER BY session_date DESC, started_ms DESC", null).use { c ->
            buildList { while (c.moveToNext()) add(c.toSession(db)) }
        }
    }

    override fun session(id: String): WorkoutSession? = read { db ->
        db.rawQuery(SESSION_SELECT + " WHERE id = ?", arrayOf(id)).use { c -> if (c.moveToFirst()) c.toSession(db) else null }
    }

    override fun saveSession(s: WorkoutSession) = tx { db ->
        val values = ContentValues().apply {
            put("session_date", s.date.toString())
            putNullable("template_id", s.templateId); put("template_name", s.templateName)
            putNullable("routine_id", s.routineId); putNullable("routine_day_id", s.routineDayId)
            put("status", s.status.name); put("started_ms", s.startedAtMs)
            putNullable("completed_ms", s.completedAtMs)
        }
        val updated = db.update("workout_session", values, "id = ?", arrayOf(s.id))
        if (updated == 0) {
            values.put("id", s.id)
            db.insertOrThrow("workout_session", null, values)
        }
        // Deleting session_exercise rows cascades to logged_set, so sets are replaced with their session.
        db.delete("session_exercise", "session_id = ?", arrayOf(s.id))
        s.exercises.sortedBy { it.orderIndex }.forEach { e ->
            db.insertOrThrow("session_exercise", null, ContentValues().apply {
                put("id", e.id); put("session_id", s.id); put("exercise_id", e.exerciseId); put("exercise_name", e.exerciseName)
                put("order_index", e.orderIndex); put("target_sets", e.prescription.targetSets)
                put("rep_min", e.prescription.repMin); put("rep_max", e.prescription.repMax)
                putNullable("target_load_kg", e.prescription.targetLoadKg); putNullable("rest_seconds", e.prescription.restSeconds)
                putNullable("target_rpe", e.prescription.targetRpe); put("notes", e.prescription.notes)
            })
        }
        s.sets.forEach { set ->
            db.insertOrThrow("logged_set", null, ContentValues().apply {
                put("id", set.id); put("session_id", s.id); put("session_exercise_id", set.sessionExerciseId)
                put("set_index", set.setIndex); put("set_type", set.type.name)
                putNullable("reps", set.reps); putNullable("load_kg", set.loadKg); putNullable("rpe", set.rpe)
                put("logged_ms", set.loggedAtMs)
            })
        }
    }

    // ---- mapping helpers ----

    private fun ContentValues.putNullable(key: String, v: Any?) {
        when (v) {
            null -> putNull(key)
            is String -> put(key, v)
            is Int -> put(key, v)
            is Long -> put(key, v)
            is Double -> put(key, v)
            else -> throw IllegalArgumentException("unsupported type for $key")
        }
    }

    private fun Cursor.nullableInt(i: Int): Int? = if (isNull(i)) null else getInt(i)
    private fun Cursor.nullableLong(i: Int): Long? = if (isNull(i)) null else getLong(i)
    private fun Cursor.nullableDouble(i: Int): Double? = if (isNull(i)) null else getDouble(i)
    private fun Cursor.nullableString(i: Int): String? = if (isNull(i)) null else getString(i)

    private fun Cursor.toExercise() = Exercise(
        id = getString(0), name = getString(1), primaryMuscles = parseMuscles(getString(2)), isCustom = getInt(3) == 1,
    )

    private fun Cursor.toPrescription(offset: Int) = Prescription(
        targetSets = getInt(offset), repMin = getInt(offset + 1), repMax = getInt(offset + 2),
        targetLoadKg = nullableDouble(offset + 3), restSeconds = nullableInt(offset + 4),
        targetRpe = nullableDouble(offset + 5), notes = getString(offset + 6),
    )

    private fun Cursor.toTemplate(db: SQLiteDatabase): WorkoutTemplate {
        val id = getString(0)
        val rows = db.rawQuery(
            "SELECT id, exercise_id, order_index, target_sets, rep_min, rep_max, target_load_kg, rest_seconds, target_rpe, notes " +
                "FROM template_exercise WHERE template_id = ? ORDER BY order_index", arrayOf(id),
        ).use { c -> buildList { while (c.moveToNext()) add(TemplateExercise(c.getString(0), c.getString(1), c.getInt(2), c.toPrescription(3))) } }
        return WorkoutTemplate(id, getString(1), getString(2), rows, getLong(3), getLong(4), nullableLong(5))
    }

    private fun Cursor.toRoutine(db: SQLiteDatabase): WorkoutRoutine {
        val id = getString(0)
        val days = db.rawQuery(
            "SELECT id, order_index, label, is_rest, template_id, weekday FROM routine_day WHERE routine_id = ? ORDER BY order_index", arrayOf(id),
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(RoutineDay(c.getString(0), c.getInt(1), c.getString(2), c.getInt(3) == 1, c.nullableString(4), c.nullableInt(5)))
            }
        }
        return WorkoutRoutine(
            id = id, name = getString(1), description = getString(2), durationWeeks = nullableInt(3),
            startDate = nullableString(4)?.let { LocalDate.parse(it) }, status = RoutineStatus.valueOf(getString(5)),
            days = days, createdAtMs = getLong(6), updatedAtMs = getLong(7),
        )
    }

    private fun Cursor.toSession(db: SQLiteDatabase): WorkoutSession {
        val id = getString(0)
        val exercises = db.rawQuery(
            "SELECT id, exercise_id, exercise_name, order_index, target_sets, rep_min, rep_max, target_load_kg, rest_seconds, target_rpe, notes " +
                "FROM session_exercise WHERE session_id = ? ORDER BY order_index", arrayOf(id),
        ).use { c -> buildList { while (c.moveToNext()) add(SessionExercise(c.getString(0), c.getString(1), c.getString(2), c.getInt(3), c.toPrescription(4))) } }
        val sets = db.rawQuery(
            "SELECT id, session_exercise_id, set_index, set_type, reps, load_kg, rpe, logged_ms FROM logged_set WHERE session_id = ? ORDER BY session_exercise_id, set_index",
            arrayOf(id),
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(LoggedSet(c.getString(0), c.getString(1), c.getInt(2), SetType.valueOf(c.getString(3)),
                    c.nullableInt(4), c.nullableDouble(5), c.nullableDouble(6), c.getLong(7)))
            }
        }
        return WorkoutSession(
            id = id, date = LocalDate.parse(getString(1)), templateId = nullableString(2), templateName = getString(3),
            routineId = nullableString(4), routineDayId = nullableString(5), status = SessionStatus.valueOf(getString(6)),
            startedAtMs = getLong(7), completedAtMs = nullableLong(8), exercises = exercises, sets = sets,
        )
    }

    companion object {
        private const val SESSION_SELECT =
            "SELECT id, session_date, template_id, template_name, routine_id, routine_day_id, status, started_ms, completed_ms FROM workout_session"

        @Volatile private var instance: WorkoutDb? = null

        fun get(ctx: Context): WorkoutDb = instance ?: synchronized(this) {
            instance ?: WorkoutDb(ctx).also { instance = it }
        }
    }
}
