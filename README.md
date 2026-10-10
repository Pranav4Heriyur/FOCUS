# Focus

Focus lock + fitness + study + earned screen time, for Android (Kotlin + Jetpack Compose).
Design: monochrome (black, white, neutral greys). Source of truth: `Focus_Android_Unified_Design_Spec.md`.

## Build

Pushing to `main` runs GitHub Actions (`.github/workflows/build.yml`): unit tests, then a debug APK.
Download it from the run's **Artifacts** (`focus-debug-apk`) and sideload it.

Locally (Android Studio, or Gradle 8.9 + JDK 17 + Android SDK 34):

```
gradle testDebugUnitTest assembleDebug
```

No Gradle wrapper is committed yet. Run `gradle wrapper --gradle-version 8.9` once and commit the
generated `gradlew`, `gradlew.bat` and `gradle/wrapper/` files if you want one.

## What exists (step 1)

| Area | Status |
|---|---|
| Design system (`design/`) | Colour, Inter type, shapes, spacing, motion tokens; button, capsule, icon button, card, chip, segmented control, toggle, ring, text field, floating tab bar, bottom sheet, hold-to-confirm |
| Navigation | Five tabs in spec order (Home, Focus, Study, Stats, Profile); tab bar hidden on A1, active session and A5 |
| A1 Setup lock | Real OS permission checks, re-read on every resume |
| A2 Start Focus | Duration stepper, presets, name, subject chips, strict toggle |
| Active session | Persisted end time; timer recomputed from the clock; Back disabled |
| A4 Leave early | 60 s hold + typed phrase; exit costs 50 XP and resets the streak |
| A5 Session complete | Real streak / XP / earned minutes |
| Home / Study / Stats / Profile | Placeholders (Home shows real Time Bank, streak, XP) |

## Not built yet (on purpose)

- **Actual app blocking.** The Accessibility service and call-screening service are stubs so A1 can
  detect them. Nothing is blocked during a session yet. This is the next step.
- Allowed apps / callers (A8, A9): the Edit buttons on A2 are disabled until those screens exist.
- A3 and A16 are undefined in the spec and are intentionally not implemented.

## Open decisions (placeholders in code, see `data/ProgressMath.kt`)

- **Earn rate:** 1 earned minute per 5 focus minutes (fits the spec's 45 min -> +9 min example).
- **XP per focus minute:** 1. The spec example shows +40 for 45 min, so this is a guess.
- **Strict lock:** stored on the session, but the spec does not say what it changes.
- **60 s hold:** the spec flags it as long. Change `FocusMotion.HoldToConfirmMs`.

## Known limits

- The timer uses wall-clock time, so changing the phone's clock can shorten a session.
  Reboot and safe mode also break a lock (already listed as known limits in the handoff).
- `app/debug.keystore` is committed so CI builds are signed identically and install as updates.
  It is a throwaway debug key. Use a GitHub Secret for a real release key.
- The hold-to-confirm needs a finger held down for the full 60 s. Add an accessible alternative later.
