package com.focus.app.util

import java.util.Locale

/** 45:00, or 1:05:00 for sessions of an hour or more. */
fun formatClock(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) + 999L) / 1000L   // round up so 0:00 only shows at the end
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, s) else String.format(Locale.ROOT, "%02d:%02d", m, s)
}
