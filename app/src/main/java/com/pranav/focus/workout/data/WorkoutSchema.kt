package com.pranav.focus.workout.data

/**
 * SQLite schema v1 for the workout feature (its own file: workout.db).
 * Keep this list the single source of truth. tools/workout/checks/schema_check.py extracts and runs these statements.
 *
 * Relationships:
 *  - template_exercise -> workout_template: CASCADE (deleting a template removes its rows).
 *  - routine_day -> workout_routine: CASCADE.
 *  - routine_day.template_id -> workout_template: NO ACTION, so a template referenced by a routine day can't be
 *    deleted until the reference is removed. This protects routines.
 *  - workout_session.template_id / routine_id / routine_day_id: plain references, NO foreign key. Sessions are
 *    historical records: deleting a template or routine must never delete or rewrite them.
 *  - session_exercise, logged_set -> workout_session: CASCADE.
 */
object WorkoutSchema {
    const val DB_NAME = "workout.db"
    const val VERSION = 1

    val STATEMENTS: List<String> = listOf(
        """CREATE TABLE exercise (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            primary_muscles TEXT NOT NULL,
            is_custom INTEGER NOT NULL CHECK (is_custom IN (0,1))
        )""",
        """CREATE TABLE workout_template (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            notes TEXT NOT NULL DEFAULT '',
            created_ms INTEGER NOT NULL,
            updated_ms INTEGER NOT NULL,
            archived_ms INTEGER
        )""",
        """CREATE TABLE template_exercise (
            id TEXT PRIMARY KEY,
            template_id TEXT NOT NULL REFERENCES workout_template(id) ON DELETE CASCADE,
            exercise_id TEXT NOT NULL REFERENCES exercise(id),
            order_index INTEGER NOT NULL,
            target_sets INTEGER NOT NULL CHECK (target_sets BETWEEN 1 AND 10),
            rep_min INTEGER NOT NULL CHECK (rep_min BETWEEN 1 AND 100),
            rep_max INTEGER NOT NULL CHECK (rep_max BETWEEN 1 AND 100 AND rep_max >= rep_min),
            target_load_kg REAL CHECK (target_load_kg IS NULL OR (target_load_kg > 0 AND target_load_kg <= 1000)),
            rest_seconds INTEGER CHECK (rest_seconds IS NULL OR rest_seconds BETWEEN 0 AND 1800),
            target_rpe REAL CHECK (target_rpe IS NULL OR target_rpe BETWEEN 1 AND 10),
            notes TEXT NOT NULL DEFAULT '',
            UNIQUE (template_id, order_index)
        )""",
        """CREATE TABLE workout_routine (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            description TEXT NOT NULL DEFAULT '',
            duration_weeks INTEGER CHECK (duration_weeks IS NULL OR duration_weeks BETWEEN 1 AND 52),
            start_date TEXT,
            status TEXT NOT NULL CHECK (status IN ('DRAFT','ACTIVE','PAUSED','ARCHIVED')),
            created_ms INTEGER NOT NULL,
            updated_ms INTEGER NOT NULL
        )""",
        """CREATE TABLE routine_day (
            id TEXT PRIMARY KEY,
            routine_id TEXT NOT NULL REFERENCES workout_routine(id) ON DELETE CASCADE,
            order_index INTEGER NOT NULL,
            label TEXT NOT NULL,
            is_rest INTEGER NOT NULL CHECK (is_rest IN (0,1)),
            template_id TEXT REFERENCES workout_template(id),
            weekday INTEGER CHECK (weekday IS NULL OR weekday BETWEEN 1 AND 7),
            CHECK (is_rest = 0 OR template_id IS NULL),
            UNIQUE (routine_id, order_index)
        )""",
        """CREATE TABLE workout_session (
            id TEXT PRIMARY KEY,
            session_date TEXT NOT NULL,
            template_id TEXT,
            template_name TEXT NOT NULL,
            routine_id TEXT,
            routine_day_id TEXT,
            status TEXT NOT NULL CHECK (status IN ('IN_PROGRESS','COMPLETED','ABANDONED','SKIPPED')),
            started_ms INTEGER NOT NULL,
            completed_ms INTEGER
        )""",
        """CREATE TABLE session_exercise (
            id TEXT PRIMARY KEY,
            session_id TEXT NOT NULL REFERENCES workout_session(id) ON DELETE CASCADE,
            exercise_id TEXT NOT NULL,
            exercise_name TEXT NOT NULL,
            order_index INTEGER NOT NULL,
            target_sets INTEGER NOT NULL,
            rep_min INTEGER NOT NULL,
            rep_max INTEGER NOT NULL,
            target_load_kg REAL,
            rest_seconds INTEGER,
            target_rpe REAL,
            notes TEXT NOT NULL DEFAULT '',
            UNIQUE (session_id, order_index)
        )""",
        """CREATE TABLE logged_set (
            id TEXT PRIMARY KEY,
            session_id TEXT NOT NULL REFERENCES workout_session(id) ON DELETE CASCADE,
            session_exercise_id TEXT NOT NULL REFERENCES session_exercise(id) ON DELETE CASCADE,
            set_index INTEGER NOT NULL,
            set_type TEXT NOT NULL CHECK (set_type IN ('NORMAL','WARMUP','FAILURE','DROP')),
            reps INTEGER CHECK (reps IS NULL OR reps BETWEEN 1 AND 100),
            load_kg REAL CHECK (load_kg IS NULL OR (load_kg >= 0 AND load_kg <= 1000)),
            rpe REAL CHECK (rpe IS NULL OR rpe BETWEEN 1 AND 10),
            logged_ms INTEGER NOT NULL,
            UNIQUE (session_exercise_id, set_index)
        )""",
        "CREATE INDEX idx_session_date ON workout_session(session_date)",
        "CREATE INDEX idx_session_routine ON workout_session(routine_id)",
        "CREATE INDEX idx_routine_day_template ON routine_day(template_id)",
    )
}
