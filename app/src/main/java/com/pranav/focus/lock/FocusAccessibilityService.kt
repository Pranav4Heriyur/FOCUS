package com.pranav.focus.lock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.pranav.focus.MainActivity
import com.pranav.focus.data.Store

/**
 * During a session, whenever a window that is not on the allowed list comes to the
 * front, bring the Focus timer screen back. Reads only the package name of the window.
 */
class FocusAccessibilityService : AccessibilityService() {
    private var lastRedirect = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val store = Store.get(this)
        if (!store.sessionActive) return
        if (store.remainingMs() <= 0) {
            SessionController.complete(this)
            return
        }
        if (Allowed.isAllowed(this, store, pkg)) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirect < 300) return
        lastRedirect = now
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        )
    }

    override fun onInterrupt() {}
}
