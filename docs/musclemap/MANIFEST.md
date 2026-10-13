# Muscle map: file and asset manifest

## Asset
- app/src/main/res/drawable-nodpi/muscle_map_chart.png
  - Source: user-supplied anatomy chart (user_muscle_anatomy_chart.png, 422x316, RGB).
  - Licence: NOT VERIFIED. The user supplied it as the MVP reference. Redistribution rights are
    the user's responsibility to confirm before public release.
  - Not medically validated. Labels come from this chart. Group-level labels are broadened where
    the chart does not label a boundary separately.
  - Low resolution: it is scaled up to fill the card, so it will look soft.

## Changed
- app/src/main/java/com/pranav/focus/MainActivity.kt: Screen.Workout now renders MuscleMapScreen
  instead of PlaceholderScreen. Single-line change. Other screens untouched.

## Added (Kotlin)
- workout/musclemap/MuscleMapDomain.kt: MuscleView, MuscleGroup (17 groups), SetBand (0-9 / 10-20 / 21+),
  setBandFor(), CompletedSetSource, UnavailableCompletedSetSource (default: unknown), countsFor().
- workout/musclemap/MuscleMapHitTest.kt: MaskPolygon, ArtworkFit (letterbox), hitTest() (smallest region wins).
- workout/musclemap/MuscleMapGeometry.kt: GENERATED polygons, normalised to 0..1.
- workout/musclemap/MuscleMapScreen.kt: Compose screen (front/back chips, chart + polygon overlay,
  tap selection, accessible chip list, detail card with Unknown state, set bands note).

## Added (tools)
- tools/musclemap/regions.py: region polygons in chart pixel space (edit here).
- tools/musclemap/generate_geometry.py: regenerates MuscleMapGeometry.kt from regions.py.
- tools/musclemap/checks/MuscleMapChecks.kt and run_checks.sh: pure-Kotlin checks.

## Resolution of the four adapter-only keys
- Rhomboids: NOT selectable. The chart does not label rhomboids, so no boundary was drawn.
- Rotator cuff (infraspinatus / teres minor): NOT selectable. Not in the user's group list.
- Teres major: NOT selectable. Chart-labelled, but the boundary could not be verified reliably.
- Brachialis: NOT selectable. Chart-labelled; a separate boundary could not be verified. It is not merged into Biceps.
No alias mapping is used.

## Data
- No workout or completed-set model exists in the repository.
- Default source returns null, so every muscle shows "Unknown". No counts are fabricated.
- Integration point: implement CompletedSetSource and pass it to MuscleMapScreen(source = ...).
- Exercise-to-muscle mapping is not implemented because there are no exercises in the repo.
