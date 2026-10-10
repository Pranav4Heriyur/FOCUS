package com.focus.app.navigation

object FocusRoute {
    const val Setup = "setup"          // A1
    const val Home = "home"
    const val Focus = "focus"          // A2
    const val Study = "study"
    const val Stats = "stats"
    const val Profile = "profile"
    const val Active = "active"
    const val Complete = "complete"    // A5

    /** Tab order is fixed by the spec: Home, Focus, Study, Stats, Profile. */
    val tabs = listOf(Home, Focus, Study, Stats, Profile)
}
