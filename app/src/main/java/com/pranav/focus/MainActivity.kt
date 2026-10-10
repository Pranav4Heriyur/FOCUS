package com.pranav.focus

import android.Manifest
import android.app.role.RoleManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pranav.focus.data.Store
import com.pranav.focus.lock.Allowed
import com.pranav.focus.lock.AppState
import com.pranav.focus.ui.AppsScreen
import com.pranav.focus.ui.C
import com.pranav.focus.ui.CallersScreen
import com.pranav.focus.ui.FocusTheme
import com.pranav.focus.ui.HomeScreen
import com.pranav.focus.ui.Screen
import com.pranav.focus.ui.SessionScreen
import com.pranav.focus.ui.SetupScreen

class MainActivity : ComponentActivity() {
    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { AppState.refresh(this) }
    private val notifLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { AppState.refresh(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.get(this).ensureDefaults(Allowed.DEFAULT_PACKAGES.filter { Allowed.isInstalled(this, it) }.toSet())
        AppState.refresh(this)
        setContent { FocusTheme { Root() } }
    }

    override fun onResume() {
        super.onResume()
        AppState.refresh(this)
    }

    @Composable
    private fun Root() {
        var screen by remember { mutableStateOf(Screen.Home) }
        val active = AppState.active

        BackHandler(enabled = active) { /* no exit during a session */ }
        BackHandler(enabled = !active && screen != Screen.Home) { screen = Screen.Home }

        DisposableEffect(active) {
            if (active) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { }
        }

        Box(Modifier.fillMaxSize().background(C.Black)) {
            if (active) {
                SessionScreen()
            } else when (screen) {
                Screen.Home -> HomeScreen { screen = it }
                Screen.Apps -> AppsScreen { screen = Screen.Home }
                Screen.Callers -> CallersScreen { screen = Screen.Home }
                Screen.Setup -> SetupScreen(
                    back = { screen = Screen.Home },
                    requestNotif = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    requestRole = {
                        getSystemService(RoleManager::class.java)
                            ?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                            ?.let { roleLauncher.launch(it) }
                    },
                )
            }
        }

        AppState.lastEarned?.let { earned ->
            AlertDialog(
                onDismissRequest = { AppState.lastEarned = null },
                containerColor = C.Sheet,
                title = { Text("Session complete", color = C.White) },
                text = { Text(if (earned > 0) "You earned $earned min in your Time Bank. It expires at midnight." else "Done.", color = C.Text2) },
                confirmButton = { TextButton(onClick = { AppState.lastEarned = null }) { Text("OK", color = C.White) } },
            )
        }
    }
}
