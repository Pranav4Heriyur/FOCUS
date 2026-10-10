package com.focus.app.feature.setup

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.focus.app.design.color.FocusColors
import com.focus.app.design.component.FocusButton
import com.focus.app.design.component.FocusCapsule
import com.focus.app.design.component.FocusCard
import com.focus.app.design.component.FocusIcon
import com.focus.app.design.component.FocusText
import com.focus.app.design.component.ScreenColumn
import com.focus.app.design.shape.FocusDimens
import com.focus.app.design.shape.FocusShapes
import com.focus.app.design.typography.FocusType

private data class PermissionRow(
    val title: String,
    val detail: String,
    val icon: ImageVector,
    val granted: Boolean,
    val onAllow: () -> Unit,
)

/** A1: Setup lock. Five permissions, read from the real OS state on every resume. */
@Composable
fun SetupLockScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    // Bumped on resume and after each result so the rows re-read the OS state.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { refresh++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    // Keyed on `refresh`, so the OS state is re-read after every resume or result.
    val rows = remember(refresh) { listOf(
        PermissionRow(
            "Accessibility", "Blocks apps", Icons.Rounded.Accessibility,
            Permissions.accessibilityEnabled(context),
        ) { context.startSettings(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        PermissionRow(
            "Display over other apps", "Timer overlay", Icons.Rounded.Layers,
            Permissions.overlayGranted(context),
        ) {
            context.startSettings(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
            )
        },
        PermissionRow(
            "Usage access", "Screen time", Icons.Rounded.QueryStats,
            Permissions.usageAccessGranted(context),
        ) { context.startSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        PermissionRow(
            "Call screening", "Allowed callers", Icons.Rounded.Call,
            Permissions.callScreeningHeld(context),
        ) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                roleLauncher.launch(roles.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        },
        PermissionRow(
            "Notifications", "Session and reminder alerts", Icons.Rounded.Notifications,
            Permissions.notificationsEnabled(context),
        ) {
            if (Build.VERSION.SDK_INT >= 33) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startSettings(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            }
        },
    ) }
    val grantedCount = rows.count { it.granted }

    ScreenColumn {
        Spacer(Modifier.height(32.dp))
        FocusText("Set up your lock", style = FocusType.ScreenTitle)
        Spacer(Modifier.height(4.dp))
        FocusText("5 permissions make the lock work.", style = FocusType.Body, color = FocusColors.Secondary)

        Spacer(Modifier.height(FocusDimens.SectionGap))
        FocusText("$grantedCount of 5 granted", style = FocusType.BodyEmphasis, color = FocusColors.High)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(FocusShapes.Capsule)
                .background(FocusColors.Track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(grantedCount / 5f)
                    .height(4.dp)
                    .clip(FocusShapes.Capsule)
                    .background(Color.White),
            )
        }

        Spacer(Modifier.height(FocusDimens.CardGap))
        FocusCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            rows.forEachIndexed { index, row ->
                PermissionItem(row)
                if (index != rows.lastIndex) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(FocusColors.Hairline))
                }
            }
        }

        Spacer(Modifier.height(FocusDimens.SectionGap))
        FocusButton("Continue", onClick = onContinue)
        Spacer(Modifier.height(FocusDimens.CardGap))
        FocusText(
            "You can change these later in Settings.",
            style = FocusType.Caption,
            color = FocusColors.Tertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun PermissionItem(row: PermissionRow) {
    Row(
        Modifier.fillMaxWidth().height(72.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(FocusShapes.AppTile)
                .background(FocusColors.Inset)
                .border(1.dp, FocusColors.Hairline, FocusShapes.AppTile),
            contentAlignment = Alignment.Center,
        ) {
            FocusIcon(row.icon, size = 24.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            FocusText(row.title, style = FocusType.CardTitle, maxLines = 1)
            FocusText(row.detail, style = FocusType.Body, color = FocusColors.Secondary, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        if (row.granted) {
            // Granted state is a white check in an outlined circle, not a coloured badge.
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                FocusIcon(Icons.Rounded.Check, tint = Color.Black, size = 18.dp, contentDescription = "Granted")
            }
        } else {
            FocusCapsule(text = "Allow", onClick = row.onAllow)
        }
    }
}

private fun Context.startSettings(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        // The settings screen is unavailable on this device; leave the row as "Allow".
    }
}
