package com.pranav.focus.workout.ui

/**
 * Carries the id of the template or routine to open between screens. The app's navigation stack holds only
 * Screen values, so the id is set just before navigating. A null id means "create new".
 */
object WorkoutEditing {
    var templateId: String? = null
    var routineId: String? = null
}

internal fun newId(prefix: String): String = prefix + ":" + java.util.UUID.randomUUID().toString()
