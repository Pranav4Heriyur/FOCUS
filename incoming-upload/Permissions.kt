package com.focus.app.feature.setup

import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.focus.app.service.FocusAccessibilityService

/** Reads the real OS state. Tapping "Allow" is never treated as proof of a grant. */
object Permissions {
    fun accessibilityEnabled(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val component = ComponentName(ctx, FocusAccessibilityService::class.java)
        val full = component.flattenToString()
        val short = component.flattenToShortString()
        return enabled.split(':').any { it.equals(full, ignoreCase = true) || it.equals(short, ignoreCase = true) }
    }

    fun overlayGranted(ctx: Context): Boolean = Settings.canDrawOverlays(ctx)

    fun usageAccessGranted(ctx: Context): Boolean {
        val ops = ctx.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = ops.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun callScreeningHeld(ctx: Context): Boolean {
        val roles = ctx.getSystemService(RoleManager::class.java) ?: return false
        return roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
            roles.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
    }

    fun notificationsEnabled(ctx: Context): Boolean =
        NotificationManagerCompat.from(ctx).areNotificationsEnabled()
}
