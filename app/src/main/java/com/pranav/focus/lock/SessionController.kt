package com.pranav.focus.lock

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.app.role.RoleManager
import android.content.pm.PackageManager
import android.Manifest
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.pranav.focus.data.Store

/** Observable UI state shared by the activity and the services. */
object AppState {
    var active by mutableStateOf(false)
    var lastEarned by mutableStateOf<Int?>(null)

    var a11yOn by mutableStateOf(false)
    var overlayOn by mutableStateOf(false)
    var notifOn by mutableStateOf(false)
    var callRoleOn by mutableStateOf(false)
    var batteryOk by mutableStateOf(false)

    val coreReady: Boolean get() = a11yOn && overlayOn

    fun refresh(ctx: Context) {
        val s = Store.get(ctx)
        active = s.sessionActive && s.remainingMs() > 0
        a11yOn = isAccessibilityEnabled(ctx)
        overlayOn = Settings.canDrawOverlays(ctx)
        notifOn = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        callRoleOn = ctx.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
        batteryOk = ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) == true
    }

    private fun isAccessibilityEnabled(ctx: Context): Boolean {
        val me = ComponentName(ctx, FocusAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}

object SessionController {
    fun start(ctx: Context, minutes: Int) {
        val s = Store.get(ctx)
        s.lastMinutes = minutes
        s.startSession(minutes)
        ContextCompat.startForegroundService(ctx, Intent(ctx, FocusService::class.java))
        AppState.refresh(ctx)
    }

    /** Timer ran out: reward and unlock. */
    fun complete(ctx: Context) {
        val s = Store.get(ctx)
        if (!s.sessionActive) return
        val minutes = s.sessionMinutes
        s.endSession()
        val earned = s.recordCompleted(minutes)
        ctx.stopService(Intent(ctx, FocusService::class.java))
        AppState.lastEarned = earned
        AppState.refresh(ctx)
    }

    /** Quit through the friction flow: penalty applies. */
    fun quit(ctx: Context) {
        val s = Store.get(ctx)
        if (!s.sessionActive) return
        s.endSession()
        s.recordQuit()
        ctx.stopService(Intent(ctx, FocusService::class.java))
        AppState.refresh(ctx)
    }
}
