package com.pranav.focus.lock

import android.content.Context
import com.pranav.focus.data.Store
import com.pranav.focus.data.TimeBank
import com.pranav.focus.ui.UsageHelper

/** Decides whether an app must be bounced back to Focus right now. */
object Enforcer {
    /** Minutes left today in a rule app (daily limit and unlocked time both apply when locked). */
    fun remainingMin(ctx: Context, store: Store, pkg: String): Int {
        val rule = store.appRules.firstOrNull { it.pkg == pkg } ?: return Int.MAX_VALUE
        val used = UsageHelper.usedMin(ctx, pkg)
        if (!rule.locked) return (rule.limitMin - used).toInt().coerceAtLeast(0)
        return TimeBank.remaining(rule.limitMin, used, store.day().alloc[pkg] ?: 0)
    }

    fun shouldBlock(ctx: Context, store: Store, pkg: String): Boolean {
        if (!store.restrictionsEnabled) return false
        if (Allowed.isCore(ctx, pkg)) return false
        if (pkg in store.blockedAlways) return true
        if (store.sessionActive && pkg in store.blockedDuring) return true
        if (store.appRules.none { it.pkg == pkg }) return false
        return remainingMin(ctx, store, pkg) <= 0
    }

    fun isTimed(store: Store, pkg: String) = store.appRules.any { it.pkg == pkg }
}
