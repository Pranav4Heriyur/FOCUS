package com.pranav.focus.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.pranav.focus.MainActivity
import com.pranav.focus.data.Store

/**
 * Bounces blocked apps back to Focus: the allow-list during a strict session, "blocked" lists,
 * and time-limited apps whose unlocked minutes ran out. Reads only the package name of the front window.
 */
class FocusAccessibilityService : AccessibilityService() {
    private var lastRedirect = 0L
    private var current: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private val recheck = object : Runnable {
        override fun run() {
            val pkg = current ?: return
            val store = Store.get(this@FocusAccessibilityService)
            if (!decide(store, pkg) && Enforcer.isTimed(store, pkg)) handler.postDelayed(this, 15_000)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (Allowed.isCore(this, pkg)) return
        val store = Store.get(this)
        current = pkg
        handler.removeCallbacks(recheck)
        if (store.sessionActive && store.remainingMs() <= 0) {
            SessionController.complete(this)
            return
        }
        if (!decide(store, pkg) && Enforcer.isTimed(store, pkg)) handler.postDelayed(recheck, 15_000)
    }

    /** True when the app was blocked and we redirected. */
    private fun decide(store: Store, pkg: String): Boolean {
        if (!store.restrictionsEnabled) return false
        if (store.sessionActive && store.strictLock) {
            if (Allowed.isAllowed(this, store, pkg)) return false
            redirect(null)
            return true
        }
        if (Enforcer.shouldBlock(this, store, pkg)) {
            redirect(pkg)
            return true
        }
        return false
    }

    private fun redirect(blocked: String?) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirect < 300) return
        lastRedirect = now
        val i = Intent(this, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        if (blocked != null) i.putExtra("blocked_pkg", blocked)
        startActivity(i)
    }

    override fun onInterrupt() { handler.removeCallbacks(recheck) }
}
