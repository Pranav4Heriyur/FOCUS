package com.pranav.focus.workout.data

import com.pranav.focus.workout.domain.Exercise
import com.pranav.focus.workout.domain.WorkoutRoutine
import com.pranav.focus.workout.domain.WorkoutSession
import com.pranav.focus.workout.domain.WorkoutTemplate

/** Thrown when storage fails. The UI shows an error state. It never falls back to fake data. */
class WorkoutStorageException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Persistence contract for the workout feature. Multi-row writes are atomic in every implementation.
 * Sessions store snapshots, so saving a template never rewrites a past session.
 */
interface WorkoutRepository {
    fun exercises(): List<Exercise>
    fun exercise(id: String): Exercise?
    /** Saves a CUSTOM exercise. Built-in exercises are read-only. */
    fun saveCustomExercise(e: Exercise)

    fun templates(includeArchived: Boolean = false): List<WorkoutTemplate>
    fun template(id: String): WorkoutTemplate?
    /** Replaces a template and its ordered exercise rows atomically. Never touches sessions. */
    fun saveTemplate(t: WorkoutTemplate)
    /** Hard-deletes a template. Fails with a storage exception if any routine day still references it. */
    fun deleteTemplate(id: String)

    fun routines(): List<WorkoutRoutine>
    fun routine(id: String): WorkoutRoutine?
    /** Replaces a routine and its days atomically. */
    fun saveRoutine(r: WorkoutRoutine)
    /** Deletes a routine definition. Sessions keep their routine id as a plain reference. */
    fun deleteRoutine(id: String)

    fun sessions(): List<WorkoutSession>
    fun session(id: String): WorkoutSession?
    /** Writes a session with its exercise snapshots and sets atomically. */
    fun saveSession(s: WorkoutSession)
}
