package com.spyou.youtubedownload.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.spyou.youtubedownload.data.local.preferences.ThemeMode
import com.spyou.youtubedownload.ui.screens.downloads.DownloadsScreen
import com.spyou.youtubedownload.ui.screens.home.HomeScreen
import com.spyou.youtubedownload.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                currentThemeMode = currentThemeMode,
                onThemeModeChange = onThemeModeChange
            )
        }
    }
}
