#!/usr/bin/env bash
# Compiles and runs the pure-Kotlin muscle map checks (no Android, no Gradle).
# Needs kotlinc on PATH.
set -euo pipefail
cd "$(dirname "$0")/../.."
P=app/src/main/java/com/pranav/focus/workout/musclemap
kotlinc "$P/MuscleMapDomain.kt" "$P/MuscleMapHitTest.kt" "$P/MuscleMapGeometry.kt" \
        tools/musclemap/checks/MuscleMapChecks.kt -include-runtime -d /tmp/musclemap-checks.jar
java -jar /tmp/musclemap-checks.jar
