package com.focus.app.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focus.app.FocusViewModel
import com.focus.app.design.component.FocusBottomBar
import com.focus.app.design.motion.LocalReduceMotion
import com.focus.app.design.shape.FocusDimens
import com.focus.app.feature.focus.ActiveFocusScreen
import com.focus.app.feature.focus.SessionCompleteScreen
import com.focus.app.feature.focus.StartFocusScreen
import com.focus.app.feature.home.HomeScreen
import com.focus.app.feature.placeholder.PlaceholderScreen
import com.focus.app.feature.setup.SetupLockScreen

@Composable
fun FocusApp(vm: FocusViewModel = viewModel()) {
    val nav = rememberNavController()
    val startRoute = remember { vm.initialRoute() }
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showTabBar = currentRoute in FocusRoute.tabs
    val reduce = LocalReduceMotion.current

    fun clearAndGoHome() = nav.navigate(FocusRoute.Home) {
        popUpTo(nav.graph.id) { inclusive = true }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = startRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(if (reduce) 0 else 220)) },
            exitTransition = { fadeOut(tween(if (reduce) 0 else 120)) },
            popEnterTransition = { fadeIn(tween(if (reduce) 0 else 220)) },
            popExitTransition = { fadeOut(tween(if (reduce) 0 else 120)) },
        ) {
            composable(FocusRoute.Setup) {
                SetupLockScreen(onContinue = {
                    vm.markSetupSeen()
                    nav.navigate(FocusRoute.Home) { popUpTo(FocusRoute.Setup) { inclusive = true } }
                })
            }
            composable(FocusRoute.Home) {
                HomeScreen(stats = vm.stats.collectAsState().value, onRefresh = vm::refreshStats)
            }
            composable(FocusRoute.Focus) {
                StartFocusScreen(onBegin = { name, subject, minutes, strict ->
                    vm.start(name, subject, minutes, strict)
                    nav.navigate(FocusRoute.Active) { launchSingleTop = true }
                })
            }
            composable(FocusRoute.Study) {
                PlaceholderScreen("Study", "Question log, mock tests and the mistake notebook come after the Focus lock.")
            }
            composable(FocusRoute.Stats) {
                PlaceholderScreen("Stats", "Charts arrive once there is data to chart.")
            }
            composable(FocusRoute.Profile) {
                PlaceholderScreen("Profile", "Connections, permissions, security and units.")
            }
            composable(FocusRoute.Active) {
                // A session ends only via completion or the A4 exit flow, never via Back.
                BackHandler(enabled = true) { }
                val session = vm.session.collectAsState().value
                val streak = vm.stats.collectAsState().value.streakDays
                if (session != null) {
                    ActiveFocusScreen(
                        session = session,
                        streakDays = streak,
                        onComplete = {
                            vm.complete()
                            nav.navigate(FocusRoute.Complete) {
                                popUpTo(FocusRoute.Active) { inclusive = true }
                            }
                        },
                        onExit = {
                            vm.abort()
                            clearAndGoHome()
                        },
                    )
                }
            }
            composable(FocusRoute.Complete) {
                val result = vm.result.collectAsState().value
                if (result == null) {
                    LaunchedEffect(Unit) { clearAndGoHome() }
                } else {
                    SessionCompleteScreen(
                        result = result,
                        onLogQuestions = {
                            nav.navigate(FocusRoute.Study) { popUpTo(nav.graph.id) { inclusive = true } }
                        },
                        onDone = { clearAndGoHome() },
                    )
                }
            }
        }

        if (showTabBar) {
            FocusBottomBar(
                tabs = focusTabs,
                selectedRoute = currentRoute,
                onSelect = { route ->
                    if (route != currentRoute) {
                        nav.navigate(route) {
                            popUpTo(FocusRoute.Home) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = FocusDimens.TabBarBottomOffset),
            )
        }
    }
}
