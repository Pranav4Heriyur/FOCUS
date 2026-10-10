package com.focus.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.focus.app.data.model.FocusSession
import com.focus.app.data.model.ProgressStats
import com.focus.app.data.model.SessionResult
import com.focus.app.data.persistence.AppPrefs
import com.focus.app.data.persistence.ProgressStore
import com.focus.app.data.persistence.SessionStore
import com.focus.app.navigation.FocusRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/**
 * Single source of truth for the running session. The session is persisted as an absolute end
 * time, so the timer is always recomputed from the clock and never depends on composable state.
 */
class FocusViewModel(app: Application) : AndroidViewModel(app) {
    private val sessionStore = SessionStore(app)
    private val progressStore = ProgressStore(app)
    private val appPrefs = AppPrefs(app)

    private val _session = MutableStateFlow(sessionStore.load())
    val session: StateFlow<FocusSession?> = _session.asStateFlow()

    private val _stats = MutableStateFlow(progressStore.stats(today()))
    val stats: StateFlow<ProgressStats> = _stats.asStateFlow()

    private val _result = MutableStateFlow<SessionResult?>(null)
    val result: StateFlow<SessionResult?> = _result.asStateFlow()

    fun initialRoute(): String = when {
        sessionStore.load() != null -> FocusRoute.Active
        !appPrefs.setupSeen -> FocusRoute.Setup
        else -> FocusRoute.Home
    }

    fun markSetupSeen() {
        appPrefs.setupSeen = true
    }

    fun refreshStats() {
        _stats.value = progressStore.stats(today())
    }

    fun start(name: String, subject: String, minutes: Int, strict: Boolean) {
        val now = System.currentTimeMillis()
        val session = FocusSession(
            name = name.ifBlank { subject },
            subject = subject,
            startMs = now,
            endMs = now + minutes * 60_000L,
            strict = strict,
        )
        sessionStore.save(session)
        _result.value = null
        _session.value = session
    }

    /** Idempotent: safe if two triggers fire for the same completion. */
    fun complete() {
        val s = _session.value ?: return
        _result.value = progressStore.recordCompletedSession(s.subject, s.durationMinutes, today())
        sessionStore.clear()
        _session.value = null
        refreshStats()
    }

    /** Confirmed early exit (A4). Costs 50 XP and resets the streak. */
    fun abort() {
        if (_session.value == null) return
        progressStore.applyEarlyExit()
        sessionStore.clear()
        _session.value = null
        refreshStats()
    }

    private fun today(): Long = LocalDate.now().toEpochDay()
}
