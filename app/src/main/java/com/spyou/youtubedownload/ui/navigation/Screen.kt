package com.spyou.youtubedownload.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Downloads : Screen("downloads")
    object Settings : Screen("settings")
    object Format : Screen("format/{url}") {
        fun createRoute(url: String) = "format/$url"
    }
}
