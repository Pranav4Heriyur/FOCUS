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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pranav.focus.data.Store
import com.pranav.focus.lock.Allowed
import com.pranav.focus.lock.AppState
import com.pranav.focus.ui.AppsScreen
import com.pranav.focus.ui.C
import com.pranav.focus.ui.CallersScreen
import com.pranav.focus.ui.CompleteScreen
import com.pranav.focus.ui.FocusStartScreen
import com.pranav.focus.ui.FocusTheme
import com.pranav.focus.ui.HomeScreen
import com.pranav.focus.ui.PlaceholderScreen
import com.pranav.focus.ui.ProfileScreen
import com.pranav.focus.ui.Screen
import com.pranav.focus.ui.SessionScreen
import com.pranav.focus.ui.SetupScreen
import com.pranav.focus.ui.TAB_SCREENS
import com.pranav.focus.ui.TabBar

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
        val summary = AppState.summary

        BackHandler(enabled = active) { /* no exit during a session */ }
        BackHandler(enabled = !active && summary == null && screen != Screen.Home) {
            screen = when (screen) {
                Screen.Apps, Screen.Callers -> Screen.Focus
                else -> Screen.Home
            }
        }

        DisposableEffect(active) {
            if (active) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            onDispose { }
        }

        Box(Modifier.fillMaxSize().background(C.Black)) {
            when {
                active -> SessionScreen()
                summary != null -> CompleteScreen(
                    summary,
                    onDone = { AppState.summary = null; screen = Screen.Home },
                    onLog = { AppState.summary = null; screen = Screen.Study },
                )
                else -> {
                    when (screen) {
                        Screen.Home -> HomeScreen { screen = it }
                        Screen.Focus -> FocusStartScreen { screen = it }
                        Screen.Study -> PlaceholderScreen("Study", "NEET question tracker: Physics, Chemistry, Biology, mocks and the mistake notebook.")
                        Screen.Stats -> PlaceholderScreen("Stats", "Charts for focus, screen time, workouts and study.")
                        Screen.Profile -> ProfileScreen { screen = it }
                        Screen.Apps -> AppsScreen { screen = Screen.Focus }
                        Screen.Callers -> CallersScreen { screen = Screen.Focus }
                        Screen.Setup -> SetupScreen(
                            back = { screen = Screen.Profile },
                            requestNotif = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            requestRole = {
                                getSystemService(RoleManager::class.java)
                                    ?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                    ?.let { roleLauncher.launch(it) }
                            },
                        )
                    }
                    if (screen in TAB_SCREENS) {
                        Box(Modifier.align(Alignment.BottomCenter)) { TabBar(screen) { screen = it } }
                    }
                }
            }
        }
    }
}
