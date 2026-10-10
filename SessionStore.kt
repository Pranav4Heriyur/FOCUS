package com.focus.app.data.persistence

import android.content.Context
import com.focus.app.data.model.FocusSession

/**
 * Persists the running session as an absolute end time so it survives recomposition,
 * backgrounding and process death. Holds no secrets.
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("focus_session", Context.MODE_PRIVATE)

    fun load(): FocusSession? {
        val end = prefs.getLong(KEY_END, 0L)
        if (end == 0L) return null
        return FocusSession(
            name = prefs.getString(KEY_NAME, "") ?: "",
            subject = prefs.getString(KEY_SUBJECT, "") ?: "",
            startMs = prefs.getLong(KEY_START, 0L),
            endMs = end,
            strict = prefs.getBoolean(KEY_STRICT, false),
        )
    }

    fun save(session: FocusSession) {
        prefs.edit()
            .putString(KEY_NAME, session.name)
            .putString(KEY_SUBJECT, session.subject)
            .putLong(KEY_START, session.startMs)
            .putLong(KEY_END, session.endMs)
            .putBoolean(KEY_STRICT, session.strict)
            .commit()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val KEY_NAME = "name"
        const val KEY_SUBJECT = "subject"
        const val KEY_START = "start"
        const val KEY_END = "end"
        const val KEY_STRICT = "strict"
    }
}
