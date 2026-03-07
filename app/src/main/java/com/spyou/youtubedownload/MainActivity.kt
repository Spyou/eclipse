package com.spyou.youtubedownload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spyou.youtubedownload.data.local.preferences.ThemeMode
import com.spyou.youtubedownload.data.local.preferences.ThemePreferences
import com.spyou.youtubedownload.data.model.DownloadFormat
import com.spyou.youtubedownload.data.model.VideoInfo
import com.spyou.youtubedownload.data.repository.DownloadRepository
import com.spyou.youtubedownload.ui.components.SealBottomSheet
import com.spyou.youtubedownload.ui.navigation.NavGraph
import com.spyou.youtubedownload.ui.navigation.Screen
import com.spyou.youtubedownload.ui.theme.EclipseTheme
import com.spyou.youtubedownload.worker.DownloadQueueManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf<String?>(null)
    private lateinit var downloadRepository: DownloadRepository
    private lateinit var downloadQueueManager: DownloadQueueManager
    private lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        requestNotificationPermission()

        // Initialize repositories
        val database = com.spyou.youtubedownload.data.local.database.DownloadDatabase.getDatabase(this)
        downloadRepository = DownloadRepository(this, database.downloadDao())
        downloadQueueManager = DownloadQueueManager.getInstance(this)
        themePreferences = ThemePreferences(this)

        // Handle shared URL from other apps
        handleSharedIntent(intent)

        setContent {
            val currentThemeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (currentThemeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            EclipseTheme(darkTheme = darkTheme) {
                var showBottomSheet by remember { mutableStateOf(false) }
                var videoInfo by remember { mutableStateOf<VideoInfo?>(null) }
                var isLoading by remember { mutableStateOf(false) }
                var error by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                // Check for shared URL
                sharedUrl?.let { url ->
                    showBottomSheet = true
                    sharedUrl = null // Clear after handling

                    // Load video info
                    scope.launch {
                        isLoading = true
                        error = null
                        videoInfo = null

                        downloadRepository.getVideoInfo(url)
                            .onSuccess { info ->
                                videoInfo = info
                                isLoading = false
                            }
                            .onFailure { e ->
                                error = e.message ?: "Failed to load video"
                                isLoading = false
                            }
                    }
                }

                MainApp(
                    currentThemeMode = currentThemeMode,
                    onThemeModeChange = { mode ->
                        themePreferences.setThemeMode(mode)
                    }
                )

                // Show Seal-style download bottom sheet
                if (showBottomSheet) {
                    SealBottomSheet(
                        videoInfo = videoInfo,
                        isLoading = isLoading,
                        error = error,
                        onDismiss = {
                            showBottomSheet = false
                            videoInfo = null
                            error = null
                        },
                        onDownload = { format ->
                            videoInfo?.let { info ->
                                scope.launch {
                                    // Create download task
                                    val task = downloadRepository.createDownloadTask(
                                        url = info.id.let { "https://youtube.com/watch?v=$it" },
                                        videoInfo = info,
                                        format = format
                                    )

                                    // Enqueue with WorkManager
                                    downloadQueueManager.enqueueDownload(
                                        taskId = task.id,
                                        url = task.url,
                                        videoInfo = info,
                                        format = format,
                                        outputPath = task.outputPath,
                                        fileName = task.fileName
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedIntent(intent)
    }

    private fun handleSharedIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            sharedText?.let { text ->
                // Extract URL from shared text
                val url = extractUrl(text)
                if (url != null && (url.contains("youtube") || url.contains("youtu.be"))) {
                    sharedUrl = url
                }
            }
        }
    }

    private fun extractUrl(text: String): String? {
        // Simple URL extraction - find first URL in text
        val urlRegex = "(https?://[^\\s]+)".toRegex()
        return urlRegex.find(text)?.value
    }
}

@Composable
fun MainApp(
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem(
            route = Screen.Home.route,
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
            label = "Home"
        ),
        BottomNavItem(
            route = Screen.Downloads.route,
            selectedIcon = Icons.AutoMirrored.Filled.List,
            unselectedIcon = Icons.AutoMirrored.Outlined.List,
            label = "Downloads"
        ),
        BottomNavItem(
            route = Screen.Settings.route,
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            label = "Settings"
        )
    )

    Scaffold(
        bottomBar = {
            val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                }
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                items.forEach { item ->
                    val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            currentThemeMode = currentThemeMode,
            onThemeModeChange = onThemeModeChange,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
)
