# Workout persistence

File: `workout.db` (SQLite, framework API), version 1. Defined in `workout/data/WorkoutSchema.kt`. The schema is checked by `tools/workout/checks/schema_check.py`.

Tables: `exercise`, `workout_template`, `template_exercise`, `workout_routine`, `routine_day`, `workout_session`, `session_exercise`, `logged_set`.

Guarantees, each checked by the schema check (SCHEMA) or the in-memory reference tests (JVM):
- Foreign keys are on for every connection (`setForeignKeyConstraintsEnabled(true)`).
- Deleting a template referenced by a routine day is rejected (JVM and SCHEMA).
- Deleting an unreferenced template removes its exercise rows (SCHEMA).
- Deleting a template or routine never removes or rewrites sessions (SCHEMA, JVM).
- Deleting a session removes its exercise rows and logged sets (SCHEMA).
- Check constraints reject out-of-range reps, loads, RPE, rest, and inverted rep ranges, and reject a rest day with a template (SCHEMA).
- `saveTemplate` / `saveRoutine` / `saveSession` update in place and never use INSERT OR REPLACE, so updates don't cascade-delete children (code review; not device-tested).
- Multi-row saves run in one transaction (`beginTransaction` / `endTransaction`).
- Built-in exercises are inserted with INSERT OR IGNORE on create and open. Custom exercises must start with `custom:`.

Migrations: version 1 is the first schema. `onUpgrade` throws until a real migration is written, so an unexpected upgrade fails loudly.

Not verified on a device: opening the database, restart survival, and the Android adapter's cursor mapping. These need an instrumentation test or a manual run.
