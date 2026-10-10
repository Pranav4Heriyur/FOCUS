package com.focus.app.data.persistence

import android.content.Context

class AppPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("focus_app", Context.MODE_PRIVATE)

    var setupSeen: Boolean
        get() = prefs.getBoolean("setup_seen", false)
        set(value) { prefs.edit().putBoolean("setup_seen", value).apply() }
}
