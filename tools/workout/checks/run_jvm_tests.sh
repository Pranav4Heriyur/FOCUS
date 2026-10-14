#!/usr/bin/env bash
# Compiles the pure workout domain + in-memory repository + tests with kotlinc and runs them.
# Needs kotlinc on PATH. Does NOT need Android, Gradle or the network.
set -euo pipefail
cd "$(dirname "$0")/../../.."
W=app/src/main/java/com/pranav/focus/workout
kotlinc $W/domain/*.kt $W/data/WorkoutRepository.kt $W/data/InMemoryWorkoutRepository.kt \
        app/src/test/java/com/pranav/focus/workout/WorkoutDomainTest.kt \
        tools/workout/shim/org/junit/Test.kt tools/workout/shim/org/junit/Assert.kt \
        tools/workout/checks/RunJUnitClasses.kt \
        -include-runtime -d /tmp/workout-jvm-tests.jar
java -cp /tmp/workout-jvm-tests.jar:. RunJUnitClassesKt com.pranav.focus.workout.WorkoutDomainTest
