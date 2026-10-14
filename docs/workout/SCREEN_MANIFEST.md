# Workout screen manifest (canonical C0–C25)

Status key:
- IMPLEMENTED: a working Compose screen wired to persistence and navigation. Visual match to the canonical design is noted separately.
- PARTIAL: some of the canonical content is implemented; the rest is listed as missing.
- NOT IMPLEMENTED: no code. A placeholder does not count.

Verification key: JVM = run here with kotlinc (domain + repository logic). SCHEMA = run here against sqlite3. UNVERIFIED = compiled only by syntax/library-free checks, not built or run on Android.

| ID | Screen | Status | Route | Persistence | Verification |
|---|---|---|---|---|---|
| C0 | Workout Hub | PARTIAL | Workout tab → `Screen.Workout` (`WorkoutHubScreen`) | reads templates, routines, sessions | UNVERIFIED (Compose); data logic JVM |
| — | Custom Workout Builder (template editor) | IMPLEMENTED, not canonical | `Screen.WorkoutTemplateEditor` (`TemplateBuilderScreen`) | `saveTemplate`, `deleteTemplate`, `saveCustomExercise` | UNVERIFIED (Compose); rules JVM |
| — | Custom Routine Builder | IMPLEMENTED, not canonical | `Screen.WorkoutRoutineEditor` (`RoutineBuilderScreen`) | `saveRoutine`, `deleteRoutine`, `templates` | UNVERIFIED (Compose); rules JVM |
| C1 | Plans | NOT IMPLEMENTED | — | — | — |
| C2 | Plan Detail | NOT IMPLEMENTED | — | — | — |
| C3 | Create Plan | NOT IMPLEMENTED (covered in function by the routine builder, but not this design) | — | — | — |
| C4 | Day Builder | NOT IMPLEMENTED (covered in function by the routine builder's day sheet) | — | — | — |
| C5 | Add Exercise | NOT IMPLEMENTED (library picker is a sheet inside the template builder) | — | — | — |
| C6 | Custom Exercise | NOT IMPLEMENTED (sheet inside the template builder) | — | `saveCustomExercise` | JVM |
| C7 | Schedule | NOT IMPLEMENTED | — | — | — |
| C8 | Active Workout | NOT IMPLEMENTED (domain ready: `WorkoutSessions.startFromTemplate`, `recordSet`, `finish`, `abandon`) | — | `saveSession` | JVM (domain) |
| C9 | Set Entry | NOT IMPLEMENTED | — | — | — |
| C10 | Rest Timer | NOT IMPLEMENTED | — | — | — |
| C11 | Exercise Sheet | NOT IMPLEMENTED | — | — | — |
| C12 | Plate Calculator | NOT IMPLEMENTED | — | — | — |
| C13 | Workout Complete | NOT IMPLEMENTED | — | — | — |
| C14 | PR Celebration | NOT IMPLEMENTED (`bestE1rm` exists; no PR detection UI) | — | — | JVM (e1RM) |
| C15 | Quick Log | NOT IMPLEMENTED | — | — | — |
| C16 | History | NOT IMPLEMENTED | — | `sessions` | — |
| C17 | Past Workout | NOT IMPLEMENTED | — | `session` | — |
| C18 | Edit Workout | PARTIAL: the template editor edits saved templates; it is not the C18 design | `Screen.WorkoutTemplateEditor` | `saveTemplate` | JVM (rules) |
| C19 | Exercise Library | NOT IMPLEMENTED as a screen (catalogue exists; picker is a sheet) | — | `exercises` | JVM |
| C20 | Exercise Detail | NOT IMPLEMENTED | — | — | — |
| C21 | Progress Overview | NOT IMPLEMENTED | — | — | — |
| C22 | Muscle Balance | NOT IMPLEMENTED. The anatomy artwork and region masks from the earlier MVP branch are not part of this change. Hard-set counting (`hardSetsByMuscle`) exists and is tested. | — | `sessions` | JVM (counting) |
| C23 | Strength Trends | NOT IMPLEMENTED (`bestE1rm` exists) | — | — | JVM |
| C24 | PR Board | NOT IMPLEMENTED | — | — | — |
| C25 | Workout Settings | NOT IMPLEMENTED | — | — | — |

## C0 detail (what is and is not on screen)
Shown, each from saved data:
- Today card: active routine's day for today (fixed-weekday routines), or next day in rotation (flexible), or "No active routine".
- Week progress: completed scheduled slots ÷ scheduled training slots this week. Hidden for flexible routines.
- This week: completed sessions Monday–Sunday.
- Load: tonnage in tonnes (sum of load × reps over working sets with both values). Warm-ups are excluded; unknown values are skipped, not zero-filled.
- Form: shown as "not available". The source spec gives no formula, so none is invented.
- Routines, Workouts, Recent (last 3 completed sessions with date, duration, tonnage).

Not shown (not implemented):
- "Start workout" and "Empty workout" (needs C8 active session screen).
- Quick log link (C15).
- PR marker on recent sessions (needs PR detection).
- "My plans" horizontal carousel. Routines are a list instead.
- Estimated time and muscle focus on the Today card.
- Pixel-level match to `screens/C0_workout_hub.jpg`. Layout follows the spec's order, not the mockup's exact geometry.

## Why the builders are not canonical screens
The canonical designs for C1–C7 and C18–C19 were not implemented in this checkpoint. The builders provide the required behaviour: they edit templates and routines, validate them, and persist them. When the canonical designs are implemented, these screens should be restyled to match. The behaviour and navigation stay the same.
