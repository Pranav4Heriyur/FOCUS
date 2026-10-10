package com.focus.app.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Stub. Exists so the Setup screen (A1) can detect whether the user enabled it.
 * App blocking during a session is implemented in the next step.
 */
class FocusAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}
