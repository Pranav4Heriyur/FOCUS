package com.pranav.focus.workout.data

import com.pranav.focus.workout.domain.Exercise
import com.pranav.focus.workout.domain.WorkoutCatalogue
import com.pranav.focus.workout.domain.WorkoutRoutine
import com.pranav.focus.workout.domain.WorkoutSession
import com.pranav.focus.workout.domain.WorkoutTemplate

/** Reference implementation of the contract. Used by unit checks; mirrors the SQLite rules (including references). */
class InMemoryWorkoutRepository : WorkoutRepository {
    private val customMap = LinkedHashMap<String, Exercise>()
    private val templateMap = LinkedHashMap<String, WorkoutTemplate>()
    private val routineMap = LinkedHashMap<String, WorkoutRoutine>()
    private val sessionMap = LinkedHashMap<String, WorkoutSession>()

    override fun exercises(): List<Exercise> = WorkoutCatalogue.BUILTIN + customMap.values

    override fun exercise(id: String): Exercise? = WorkoutCatalogue.BY_ID[id] ?: customMap[id]

    override fun saveCustomExercise(e: Exercise) {
        require(e.isCustom && e.id.startsWith("custom:")) { "only custom exercises can be saved" }
        customMap[e.id] = e
    }

    override fun templates(includeArchived: Boolean): List<WorkoutTemplate> =
        templateMap.values.filter { includeArchived || it.archivedAtMs == null }

    override fun template(id: String): WorkoutTemplate? = templateMap[id]

    override fun saveTemplate(t: WorkoutTemplate) {
        templateMap[t.id] = t
    }

    override fun deleteTemplate(id: String) {
        if (routineMap.values.any { r -> r.days.any { it.templateId == id } })
            throw WorkoutStorageException("Template $id is still used by a routine day")
        templateMap.remove(id)
    }

    override fun routines(): List<WorkoutRoutine> = routineMap.values.toList()
    override fun routine(id: String): WorkoutRoutine? = routineMap[id]
    override fun saveRoutine(r: WorkoutRoutine) {
        routineMap[r.id] = r
    }
    override fun deleteRoutine(id: String) {
        routineMap.remove(id)
    }

    override fun sessions(): List<WorkoutSession> = sessionMap.values.toList()
    override fun session(id: String): WorkoutSession? = sessionMap[id]
    override fun saveSession(s: WorkoutSession) {
        sessionMap[s.id] = s
    }
}
