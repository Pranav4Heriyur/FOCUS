# Existing behaviour audit (before this change)

Baseline: origin/main 4d21b00. Each item lists the current behaviour and whether this change touches it.

## Workout data already read by other features
| Location | What it does today | Changed by this feature? |
|---|---|---|
| `ui/Home.kt` ~L87 | Home ring: `workoutMin` = minutes of Health-synced workout activities today | No |
| `ui/Home.kt` ~L101 | Home ring: `day.actions.size + store.int("workouts_<today>")`, capped at 1 | No (see decision D1) |
| `ui/Home.kt` ~L121–122 | Home "Today's Workout" card opens `Screen.Workout` | No. The route now opens the real hub. |
| `ui/Stage2.kt` ~L164 | Stats weekly workouts = Health workouts + `workouts_<day>` | No |
| `data/Weekly.kt` ~L26 | Weekly target `workouts` = Health workouts + `workouts_<day>` | No |
| `lock/Earning.kt` ~L14, ~L23 | Time Bank action "Workout done": counts Health workouts + `workouts_<today>` | No (see decision D1) |
| `ui/PlanScreens.kt` ~L673 | Plan "Workout" category minutes | No |
| `data/Models.kt` L64 | `isWorkout` set for Health activity types | No |
| `net/HealthSync.kt` | Reads Health Connect workouts | No |

Nothing in `main` writes `workouts_<date>`. It is read only.

## Decisions
- **D1: the feature does not write `workouts_<date>`.** Writing it would change the Home ring, Stats, the weekly target, and Time Bank earnings. Each would start counting in-app workouts without any product decision to do so. The new persistence is separate (`workout.db`). Connecting it to these features is a follow-up that needs a product decision.
- **D2: Workout persistence is a separate SQLite file (`workout.db`)**, like `study.db`. It does not touch `Store` (SharedPreferences) and does not change `Store` schemas.
- **D3: Screen enum gains two values** (`WorkoutTemplateEditor`, `WorkoutRoutineEditor`). Existing values and order are unchanged. `MainActivity`'s exhaustive `when` gains two branches.
- **D4: The Workout placeholder is replaced** by `WorkoutHubScreen`. No other screen is touched.
- **D5: Only completed sessions count** in statistics. Abandoned, skipped, and in-progress sessions never count.
- **D6: Unknown is not zero** for set counts: exercises without a muscle mapping are reported as `unmappedSets`, not allocated to a muscle. A known zero means no completed working sets in the window.
- **D7: Sessions snapshot template prescriptions and exercise names.** Editing a template never rewrites a past session.
- **D8: Only one routine is ACTIVE at a time.** Activating a routine pauses the others. This is a product decision made here because "today" needs a single source. Flag for review.
- **D9: e1RM uses Epley** on NORMAL and FAILURE sets with 1–15 reps. The earlier Part C patch referenced an Epley/Brzycki average with RIR; that foundation is not on `main`, so it is not reused.
- **D10: Set bands** 0–9 / 10–20 / 21+ come from the canonical C22 rule in the handoff notes.
