package com.spyou.youtubedownload.ui.screens.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.spyou.youtubedownload.data.local.preferences.ThemeMode
import com.spyou.youtubedownload.ui.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCookieDialog by remember { mutableStateOf(false) }
    var cookieStatus by remember { mutableStateOf(checkCookieStatus(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // APPEARANCE section label — iOS-style uppercase
            SectionLabel("APPEARANCE")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = currentThemeMode == mode,
                                onClick = { onThemeModeChange(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ThemeMode.entries.size
                                )
                            ) {
                                Text(
                                    text = when (mode) {
                                        ThemeMode.SYSTEM -> "System"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // NOTIFICATIONS section
            SectionLabel("NOTIFICATIONS")

            NotificationCard(context = context)

            Spacer(modifier = Modifier.height(24.dp))

            // YOUTUBE ACCESS section label
            SectionLabel("YOUTUBE ACCESS")

            CookieCard(
                hasCookies = cookieStatus,
                onLoginClick = { showCookieDialog = true },
                onClearCookies = {
                    scope.launch {
                        clearCookies(context)
                        cookieStatus = false
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (cookieStatus)
                    "YouTube cookies active - age-restricted videos should work"
                else
                    "No cookies - some videos may be blocked (403 error)",
                style = MaterialTheme.typography.bodySmall,
                color = if (cookieStatus)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ABOUT section label
            SectionLabel("ABOUT")

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Eclipse",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = "Developed by Krishna Vishwakarma",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "github.com/spyou",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/spyou"))
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Cookie Login Dialog
    if (showCookieDialog) {
        CookieLoginDialog(
            onDismiss = {
                showCookieDialog = false
                cookieStatus = checkCookieStatus(context)
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun NotificationCard(context: Context) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    // Re-check every time screen resumes (user may toggle in system settings)
    val notificationsEnabled = remember(lifecycleState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (notificationsEnabled) SuccessGreen
                        else MaterialTheme.colorScheme.error
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (notificationsEnabled) "Notifications Enabled" else "Notifications Disabled",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
                Text(
                    text = if (notificationsEnabled)
                        "You'll see download progress and completion"
                    else
                        "Enable to see download progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!notificationsEnabled) {
                FilledTonalButton(onClick = {
                    val intent = Intent().apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        } else {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    }
                    context.startActivity(intent)
                }) {
                    Text("Enable")
                }
            }
        }
    }
}

@Composable
private fun CookieCard(
    hasCookies: Boolean,
    onLoginClick: () -> Unit,
    onClearCookies: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small status dot instead of large circle icon
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasCookies) SuccessGreen
                        else MaterialTheme.colorScheme.error
                    )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (hasCookies) "YouTube Cookies Active" else "YouTube Login Required",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )

                Text(
                    text = if (hasCookies)
                        "You can download age-restricted videos"
                    else
                        "Login to download restricted videos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (hasCookies) {
                TextButton(onClick = onClearCookies) {
                    Text("Clear")
                }
            } else {
                FilledTonalButton(onClick = onLoginClick) {
                    Text("Login")
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CookieLoginDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Login to YouTube",
                        style = MaterialTheme.typography.titleLarge
                    )

                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                }

                Text(
                    text = "Login to your YouTube account to enable downloading age-restricted videos. Tap 'Done' after logging in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // WebView
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        if (url?.contains("youtube.com") == true) {
                                            extractAndSaveCookies(ctx, url)
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        return false
                                    }
                                }

                                loadUrl("https://www.youtube.com/signin")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}

private fun checkCookieStatus(context: Context): Boolean {
    val cookieFile = File(context.filesDir, "cookies.txt")
    return cookieFile.exists() && cookieFile.length() > 100
}

private fun extractAndSaveCookies(context: Context, url: String) {
    try {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie(url)

        if (!cookies.isNullOrEmpty()) {
            val netscapeCookies = convertToNetscapeFormat(cookies, url)

            val cookieFile = File(context.filesDir, "cookies.txt")
            cookieFile.writeText(netscapeCookies)

            Log.d("CookieManager", "Cookies saved to: ${cookieFile.absolutePath}")
            Log.d("CookieManager", "Cookie size: ${cookieFile.length()} bytes")
        }
    } catch (e: Exception) {
        Log.e("CookieManager", "Failed to save cookies", e)
    }
}

private fun convertToNetscapeFormat(cookies: String, url: String): String {
    val domain = ".youtube.com"
    val lines = mutableListOf(
        "# Netscape HTTP Cookie File",
        "# This file was generated by Eclipse",
        "# Edit at your own risk",
        ""
    )

    cookies.split("; ").forEach { cookie ->
        val parts = cookie.split("=", limit = 2)
        if (parts.size == 2) {
            val name = parts[0].trim()
            val value = parts[1].trim()

            if (name in listOf("LOGIN_INFO", "SAPISID", "APISID", "SSID", "HSID", "SID", "SIDCC", "__Secure-1PSID", "__Secure-3PSID", "__Secure-1PAPISID", "__Secure-3PAPISID")) {
                lines.add("$domain\tTRUE\t/\tTRUE\t0\t$name\t$value")
            }
        }
    }

    return lines.joinToString("\n")
}

private fun clearCookies(context: Context) {
    try {
        val cookieFile = File(context.filesDir, "cookies.txt")
        if (cookieFile.exists()) {
            cookieFile.delete()
        }

        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        Log.d("CookieManager", "Cookies cleared")
    } catch (e: Exception) {
        Log.e("CookieManager", "Failed to clear cookies", e)
    }
}
