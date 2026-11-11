package com.soundsing.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.soundsing.app.model.AudioOutputDevice
import com.soundsing.app.ui.screens.CompatibilityScreen
import com.soundsing.app.ui.screens.DevicesScreen
import com.soundsing.app.ui.screens.GuideScreen
import com.soundsing.app.ui.screens.HomeScreen
import com.soundsing.app.ui.theme.DarkPrimary
import com.soundsing.app.viewmodel.SoundSingUiState

@Composable
fun SoundSingNavGraph(
    navController: NavHostController,
    uiState: SoundSingUiState,
    onStartRouting: () -> Unit,
    onStopRouting: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onDelayChange: (Int) -> Unit,
    onSelectDevice: (AudioOutputDevice) -> Unit,
    onRefreshDevices: () -> Unit,
    onRefreshAudit: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(imageVector = screen.icon, contentDescription = screen.title)
                        },
                        label = {
                            Text(text = screen.title)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkPrimary,
                            selectedTextColor = DarkPrimary,
                            indicatorColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    uiState = uiState,
                    onStartRouting = onStartRouting,
                    onStopRouting = onStopRouting,
                    onVolumeChange = onVolumeChange,
                    onDelayChange = onDelayChange,
                    onNavigateToDevices = {
                        navController.navigate(Screen.Devices.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onDismissError = onDismissError
                )
            }
            composable(Screen.Devices.route) {
                DevicesScreen(
                    uiState = uiState,
                    onSelectDevice = onSelectDevice,
                    onRefreshDevices = onRefreshDevices
                )
            }
            composable(Screen.Compatibility.route) {
                CompatibilityScreen(
                    report = uiState.compatibilityReport,
                    onRefresh = onRefreshAudit
                )
            }
            composable(Screen.Guide.route) {
                GuideScreen()
            }
        }
    }
}
