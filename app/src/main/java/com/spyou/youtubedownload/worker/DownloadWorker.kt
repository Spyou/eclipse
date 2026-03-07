package com.spyou.youtubedownload.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.spyou.youtubedownload.MainActivity
import com.spyou.youtubedownload.R
import com.spyou.youtubedownload.data.local.database.DownloadDatabase
import com.spyou.youtubedownload.data.model.DownloadProgress
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.absoluteValue

class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "DownloadWorker"
        const val CHANNEL_ID = "eclipse_downloads"

        const val KEY_TASK_ID = "task_id"
        const val KEY_URL = "url"
        const val KEY_VIDEO_TITLE = "video_title"
        const val KEY_FORMAT_TYPE = "format_type"
        const val KEY_FORMAT_QUALITY = "format_quality"
        const val KEY_FORMAT_CODEC = "format_codec"
        const val KEY_OUTPUT_PATH = "output_path"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_VIDEO_ID = "video_id"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val database = DownloadDatabase.getDatabase(context)
    private val downloadDao = database.downloadDao()

    // Unique notification ID per task so multiple downloads don't collide
    private val notificationId by lazy {
        (inputData.getString(KEY_TASK_ID)?.hashCode()?.absoluteValue ?: 1001)
    }

    override suspend fun doWork(): Result {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val videoTitle = inputData.getString(KEY_VIDEO_TITLE) ?: "Unknown"
        val formatType = inputData.getString(KEY_FORMAT_TYPE) ?: "VIDEO"
        val formatQuality = inputData.getString(KEY_FORMAT_QUALITY) ?: "720"
        val formatCodec = inputData.getString(KEY_FORMAT_CODEC)
        val outputPath = inputData.getString(KEY_OUTPUT_PATH) ?: return Result.failure()

        Log.d(TAG, "Starting download for: $videoTitle (task=$taskId)")

        // Create channel FIRST, before any notification
        createNotificationChannel()

        // Show foreground notification immediately
        try {
            setForeground(createForegroundInfo("Downloading: ${videoTitle.take(40)}", 0))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set foreground", e)
            // Continue anyway — download will work, just no persistent notification
        }

        return try {
            downloadDao.updateStatus(taskId, DownloadProgress.DownloadStatus.DOWNLOADING)

            val request = buildDownloadRequest(
                url = url,
                outputPath = outputPath,
                formatType = formatType,
                formatQuality = formatQuality,
                formatCodec = formatCodec
            )

            var lastNotifiedProgress = -1
            val progressScope = CoroutineScope(Dispatchers.IO)

            // Execute download with progress callback
            YoutubeDL.getInstance().execute(request, taskId) { progress, etaInSeconds, text ->
                val pct = progress.toInt().coerceIn(0, 100)

                // Update notification every 2%
                if (pct != lastNotifiedProgress && (pct - lastNotifiedProgress >= 2 || pct >= 99)) {
                    lastNotifiedProgress = pct
                    val notification = buildProgressNotification(
                        title = "Downloading: ${videoTitle.take(40)}",
                        progress = pct
                    )
                    notificationManager.notify(notificationId, notification)
                }

                // Update DB so the UI list shows live progress
                progressScope.launch {
                    try {
                        downloadDao.updateProgress(
                            id = taskId,
                            progress = progress / 100f,
                            status = DownloadProgress.DownloadStatus.DOWNLOADING,
                            eta = etaInSeconds
                        )
                    } catch (_: Exception) {}
                }
            }

            // --- Download completed ---
            downloadDao.updateStatus(taskId, DownloadProgress.DownloadStatus.COMPLETED, System.currentTimeMillis())
            downloadDao.updateProgress(taskId, 1f, DownloadProgress.DownloadStatus.COMPLETED)

            scanFileToGallery(outputPath)

            // Cancel progress notification, show "complete" notification
            notificationManager.cancel(notificationId)
            notificationManager.notify(
                notificationId + 100_000,
                buildResultNotification("Download complete", videoTitle.take(50), android.R.drawable.stat_sys_download_done)
            )

            Log.d(TAG, "Download completed: $videoTitle")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $videoTitle", e)
            // Extract actual ERROR line from yt-dlp output
            val fullMsg = e.message ?: ""
            val errorLine = fullMsg.lines()
                .firstOrNull { it.trimStart().startsWith("ERROR:") }
                ?: fullMsg
            val errorMessage = when {
                errorLine.contains("permission", ignoreCase = true) ||
                errorLine.contains("Operation not permitted", ignoreCase = true) ||
                errorLine.contains("unable to open for writing", ignoreCase = true) ->
                    "Storage permission denied. Cannot write to download folder."
                errorLine.contains("403", ignoreCase = true) ->
                    "YouTube blocked this download. Try using cookies."
                errorLine.contains("Sign in", ignoreCase = true) ||
                errorLine.contains("LOGIN_REQUIRED", ignoreCase = true) ->
                    "This video requires sign in."
                errorLine.contains("network", ignoreCase = true) ||
                errorLine.contains("timed out", ignoreCase = true) ->
                    "Network error. Check your connection."
                else -> "Download failed: ${errorLine.removePrefix("ERROR:").trim().ifEmpty { "Unknown error" }}"
            }
            downloadDao.updateError(taskId, errorMessage)

            notificationManager.cancel(notificationId)
            notificationManager.notify(
                notificationId + 100_000,
                buildResultNotification("Download failed", videoTitle.take(50), android.R.drawable.stat_notify_error)
            )

            Result.failure(workDataOf("error" to errorMessage))
        }
    }

    // ── Notification helpers ──

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Eclipse Downloads",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Shows download progress"
            setSound(null, null)
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundInfo(title: String, progress: Int): ForegroundInfo {
        val notification = buildProgressNotification(title, progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun buildProgressNotification(title: String, progress: Int): android.app.Notification {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("${progress}%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createContentIntent())
            .build()
    }

    private fun buildResultNotification(title: String, text: String, icon: Int): android.app.Notification {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(createContentIntent())
            .build()
    }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            applicationContext, notificationId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // ── Download request ──

    private fun buildDownloadRequest(
        url: String,
        outputPath: String,
        formatType: String,
        formatQuality: String,
        formatCodec: String?
    ): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)
        val outputTemplate = "$outputPath/%(title)s.%(ext)s"
        request.addOption("-o", outputTemplate)
        request.addOption("--force-overwrites")

        when (formatType) {
            "VIDEO" -> {
                val targetHeight = formatQuality.filter { it.isDigit() }.take(4).toIntOrNull() ?: 720
                val formatString = when (targetHeight) {
                    4320 -> "401+140/313+140/bv[height<=4320]+ba/b[height<=4320]"
                    2160 -> "401+140/313+140/bv[height<=2160]+ba/b[height<=2160]"
                    1440 -> "400+140/271+140/bv[height<=1440]+ba/b[height<=1440]"
                    1080 -> "137+140/399+140/bv[height=1080][ext=mp4]+ba/bv[height=1080]+ba/bv[height<=1080]+ba/b[height<=1080]"
                    720 -> "136+140/398+140/bv[height=720][ext=mp4]+ba/bv[height=720]+ba/bv[height<=720]+ba/b[height<=720]"
                    480 -> "135+140/397+140/bv[height=480][ext=mp4]+ba/bv[height=480]+ba/bv[height<=480]+ba/b[height<=480]"
                    360 -> "134+140/396+140/bv[height=360][ext=mp4]+ba/bv[height=360]+ba/18/b[height<=360]"
                    else -> "bv[height<=$targetHeight][ext=mp4]+ba/bv[height<=$targetHeight]+ba/b[height<=$targetHeight]"
                }
                request.addOption("-f", formatString)
                request.addOption("--merge-output-format", "mp4")
            }
            "AUDIO" -> {
                val codec = formatCodec ?: "mp3"
                request.addOption("-x")
                request.addOption("--audio-format", codec)
                request.addOption("--audio-quality", "0")
                if (codec != "wav") {
                    request.addOption("--embed-thumbnail")
                }
            }
        }

        request.addOption("--restrict-filenames")
        request.addOption("--add-metadata")
        request.addOption("--no-playlist")
        request.addOption("--no-warnings")
        request.addOption("--newline")
        request.addOption("--no-cache-dir")
        request.addOption("--age-limit", "99")
        request.addOption("--extractor-args", "youtube:player_client=android_vr")

        val cookieFile = File(applicationContext.filesDir, "cookies.txt")
        if (cookieFile.exists()) {
            request.addOption("--cookies", cookieFile.absolutePath)
        }

        return request
    }

    // ── MediaStore ──

    private fun scanFileToGallery(outputPath: String) {
        val dir = File(outputPath)
        if (dir.exists() && dir.canRead()) {
            dir.listFiles()
                ?.filter { it.isFile && !it.name.startsWith(".") }
                ?.sortedByDescending { it.lastModified() }
                ?.take(3)
                ?.forEach { file ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        addFileToMediaStore(file)
                    } else {
                        android.media.MediaScannerConnection.scanFile(
                            applicationContext, arrayOf(file.absolutePath), null, null
                        )
                    }
                }
        }
    }

    private fun addFileToMediaStore(file: File) {
        try {
            val isVideo = file.name.endsWith(".mp4") || file.name.endsWith(".webm") ||
                         file.name.endsWith(".mkv") || file.name.endsWith(".mov")

            val collection = if (isVideo) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }

            val relativePath = if (isVideo) {
                Environment.DIRECTORY_MOVIES + "/Eclipse"
            } else {
                Environment.DIRECTORY_MUSIC + "/Eclipse"
            }

            val mimeType = when {
                file.name.endsWith(".mp4") -> "video/mp4"
                file.name.endsWith(".mp3") -> "audio/mpeg"
                file.name.endsWith(".m4a") -> "audio/mp4"
                file.name.endsWith(".opus") -> "audio/opus"
                file.name.endsWith(".flac") -> "audio/flac"
                file.name.endsWith(".wav") -> "audio/wav"
                else -> "*/*"
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                put(MediaStore.MediaColumns.SIZE, file.length())
            }

            val uri = applicationContext.contentResolver.insert(collection, values)
            if (uri != null) {
                applicationContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                applicationContext.contentResolver.update(uri, values, null, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to MediaStore", e)
        }
    }
}
