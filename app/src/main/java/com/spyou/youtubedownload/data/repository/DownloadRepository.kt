package com.spyou.youtubedownload.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.spyou.youtubedownload.data.local.database.DownloadDao
import com.spyou.youtubedownload.data.local.database.DownloadEntity
import com.spyou.youtubedownload.data.model.*
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.File

class DownloadRepository(
    private val context: Context,
    private val downloadDao: DownloadDao
) {
    companion object {
        const val TAG = "DownloadRepository"
    }

    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val activeDownloads: Flow<List<DownloadEntity>> = downloadDao.getActiveDownloads()
    val completedDownloads: Flow<List<DownloadEntity>> = downloadDao.getCompletedDownloads()
    val recentDownloads: Flow<List<DownloadEntity>> = downloadDao.getRecentCompletedDownloads()

    suspend fun getVideoInfo(url: String): Result<VideoInfo> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== getVideoInfo START ===")
            Log.d(TAG, "URL: $url")
            
            if (!url.contains("youtube") && !url.contains("youtu.be")) {
                Log.e(TAG, "Invalid URL - not a YouTube link")
                return@withContext Result.failure(IllegalArgumentException("Invalid YouTube URL"))
            }
            
            val request = YoutubeDLRequest(url)
            // Add cookie support for info extraction too
            addCookieOptions(request)
            
            val streamInfo = YoutubeDL.getInstance().getInfo(request)
            
            Log.d(TAG, "Video ID: ${streamInfo.id}")
            Log.d(TAG, "Video Title: ${streamInfo.title}")
            Log.d(TAG, "Formats count: ${streamInfo.formats?.size ?: 0}")
            
            val formats = streamInfo.formats?.mapNotNull { format ->
                try {
                    val isAudioOnly = format.vcodec == "none" || format.formatNote == "audio only"
                    
                    // Extract height from formatNote (contains resolution like "1080p")
                    val height = extractHeightFromResolution(format.formatNote)
                    val resolutionLabel = if (height != null) "${height}p" else (format.formatNote ?: "Unknown")
                    
                    // Log each format for debugging
                    if (height != null && height >= 360) {
                        Log.d(TAG, "Format available: id=${format.formatId}, ${height}p, ext=${format.ext}, vcodec=${format.vcodec}, acodec=${format.acodec}, note=${format.formatNote}")
                    }
                    
                    VideoFormat(
                        formatId = format.formatId ?: "",
                        ext = format.ext ?: "",
                        quality = resolutionLabel,
                        resolution = resolutionLabel,
                        fps = null,
                        filesize = null,
                        filesizeApprox = null,
                        videoCodec = format.vcodec,
                        audioCodec = format.acodec,
                        abr = format.abr?.toFloat(),
                        vbr = null,
                        asr = format.asr,
                        formatNote = format.formatNote,
                        hasVideo = format.vcodec != "none" && !isAudioOnly,
                        hasAudio = format.acodec != "none"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error parsing format: ${e.message}")
                    null
                }
            } ?: emptyList()

            val videoInfo = VideoInfo(
                id = streamInfo.id ?: "",
                title = streamInfo.title ?: "Unknown",
                description = streamInfo.description,
                thumbnail = streamInfo.thumbnail,
                author = streamInfo.uploader,
                duration = streamInfo.duration?.toLong(),
                viewCount = streamInfo.viewCount?.toLong(),
                uploadDate = streamInfo.uploadDate,
                formats = formats
            )
            
            Log.d(TAG, "=== getVideoInfo SUCCESS ===")
            Result.success(videoInfo)
        } catch (e: Exception) {
            Log.e(TAG, "=== getVideoInfo FAILED ===", e)
            Result.failure(e)
        }
    }

    suspend fun createDownloadTask(
        url: String,
        videoInfo: VideoInfo,
        format: DownloadFormat
    ): DownloadTask = withContext(Dispatchers.IO) {
        Log.d(TAG, "=== createDownloadTask START ===")
        
        val outputDir = getOutputDirectory()
        Log.d(TAG, "Output directory: ${outputDir.absolutePath}")
        Log.d(TAG, "Directory exists: ${outputDir.exists()}")
        Log.d(TAG, "Directory canWrite: ${outputDir.canWrite()}")
        
        val safeTitle = videoInfo.title.replace(Regex("[^a-zA-Z0-9\\s.-]"), "_").take(50)
        val task = DownloadTask(
            url = url,
            videoInfo = videoInfo,
            format = format,
            outputPath = outputDir.absolutePath,
            fileName = "$safeTitle.${getFileExtension(format)}"
        )
        
        Log.d(TAG, "Task ID: ${task.id}")
        Log.d(TAG, "File name: ${task.fileName}")
        Log.d(TAG, "Format type: ${format.type}")
        Log.d(TAG, "Format quality: ${format.quality}")
        
        val entity = DownloadEntity(
            id = task.id,
            url = url,
            title = videoInfo.title,
            author = videoInfo.author,
            thumbnail = videoInfo.thumbnail,
            duration = videoInfo.duration,
            formatType = format.type,
            formatQuality = format.quality,
            formatCodec = format.codec,
            outputPath = outputDir.absolutePath,
            fileName = task.fileName,
            videoId = videoInfo.id
        )
        
        downloadDao.insertDownload(entity)
        Log.d(TAG, "=== createDownloadTask SUCCESS ===")
        task
    }

    suspend fun startDownload(
        task: DownloadTask,
        onProgress: suspend (DownloadProgress) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== startDownload START ===")
            Log.d(TAG, "Task ID: ${task.id}")
            Log.d(TAG, "URL: ${task.url}")
            Log.d(TAG, "Output path: ${task.outputPath}")
            
            // Verify output directory is writable
            val outputDir = File(task.outputPath)
            if (!outputDir.exists()) {
                Log.e(TAG, "Output directory does not exist: ${task.outputPath}")
                val created = outputDir.mkdirs()
                Log.d(TAG, "Created directory: $created")
                if (!created) {
                    throw Exception("Cannot create output directory")
                }
            }
            if (!outputDir.canWrite()) {
                Log.e(TAG, "Output directory is not writable: ${task.outputPath}")
                throw Exception("Output directory is not writable")
            }
            
            Log.d(TAG, "Building YoutubeDL request...")
            val request = buildYoutubeDLRequest(task)
            
            Log.d(TAG, "Updating status to DOWNLOADING...")
            downloadDao.updateStatus(
                task.id,
                DownloadProgress.DownloadStatus.DOWNLOADING
            )

            val processId = task.id
            var lastProgress = 0f
            var hasProgress = false
            
            Log.d(TAG, "Calling YoutubeDL.execute() with processId: $processId")
            
            val response = YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, text ->
                hasProgress = true
                if (progress > lastProgress) {
                    lastProgress = progress
                    Log.d(TAG, "Progress: ${progress.toInt()}%, ETA: ${etaInSeconds}s")
                    
                    val downloadProgress = DownloadProgress(
                        progress = progress / 100f,
                        etaInSeconds = etaInSeconds,
                        status = DownloadProgress.DownloadStatus.DOWNLOADING
                    )
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            downloadDao.updateProgress(
                                task.id,
                                progress / 100f,
                                DownloadProgress.DownloadStatus.DOWNLOADING,
                                etaInSeconds
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating progress in DB", e)
                        }
                    }
                    
                    launch {
                        try {
                            onProgress(downloadProgress)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in progress callback", e)
                        }
                    }
                }
            }
            
            if (!hasProgress) {
                Log.w(TAG, "Download completed but no progress callbacks received!")
            }
            
            // Log the output to see what format was selected
            val output = response.out
            // Extract format info from verbose output
            val formatMatch = Regex("\\[download\\] Destination:.*").find(output)
            Log.d(TAG, "Download output snippet: ${formatMatch?.value}")
            Log.d(TAG, "Full output length: ${output.length} chars")
            
            // Try to find what format was selected
            if (output.contains("format=")) {
                val formatLine = output.lines().find { it.contains("format=") }
                Log.d(TAG, "Selected format info: $formatLine")
            }

            Log.d(TAG, "Download finished, updating status to COMPLETED...")
            downloadDao.updateStatus(
                task.id,
                DownloadProgress.DownloadStatus.COMPLETED,
                System.currentTimeMillis()
            )
            
            // Scan only the newly downloaded file to gallery
            val dir = File(task.outputPath)
            if (dir.exists() && dir.canRead()) {
                // Find the file that was just downloaded — it's the most recently modified one
                // that didn't exist before (or was modified during this download)
                val newFile = dir.listFiles()
                    ?.filter { it.isFile && !it.name.startsWith(".") }
                    ?.maxByOrNull { it.lastModified() }

                if (newFile != null) {
                    Log.d(TAG, "Scanning newly downloaded file to gallery: ${newFile.name}")
                    scanFileToGallery(newFile.absolutePath)
                } else {
                    Log.w(TAG, "No file found after download in: ${dir.absolutePath}")
                }
            } else {
                Log.e(TAG, "Download directory not accessible: ${dir.absolutePath}")
            }
            
            Log.d(TAG, "=== startDownload SUCCESS ===")
            Result.success(task.outputPath)
        } catch (e: Exception) {
            Log.e(TAG, "=== startDownload FAILED ===", e)
            
            // Better error detection — extract the actual ERROR line from yt-dlp output
            val fullMsg = e.message ?: ""
            // yt-dlp prefixes real errors with "ERROR:" — use that line for classification
            val errorLine = fullMsg.lines()
                .firstOrNull { it.trimStart().startsWith("ERROR:") }
                ?: fullMsg
            val errorMessage = when {
                errorLine.contains("permission", ignoreCase = true) ||
                errorLine.contains("Operation not permitted", ignoreCase = true) ->
                    "Storage permission denied. The app cannot write to the download folder."
                errorLine.contains("403", ignoreCase = true) ->
                    "YouTube blocked this download. Try a different video or use cookies."
                errorLine.contains("Sign in", ignoreCase = true) ||
                errorLine.contains("LOGIN_REQUIRED", ignoreCase = true) ->
                    "This video requires sign in. Try a different video."
                errorLine.contains("copyright", ignoreCase = true) ->
                    "This video is not available due to copyright restrictions."
                errorLine.contains("unavailable", ignoreCase = true) ->
                    "Video unavailable. It may be private or deleted."
                errorLine.contains("network", ignoreCase = true) ||
                errorLine.contains("URLError", ignoreCase = true) ||
                errorLine.contains("timed out", ignoreCase = true) ->
                    "Network error. Check your internet connection."
                errorLine.contains("unable to open for writing", ignoreCase = true) ->
                    "Cannot write to download folder. Try restarting the app."
                errorLine.contains("ffmpeg returned error", ignoreCase = true) ||
                errorLine.contains("ffmpeg exited", ignoreCase = true) ->
                    "FFmpeg error during merging. Try audio-only format."
                else -> "Download failed: ${errorLine.removePrefix("ERROR:").trim().ifEmpty { "Unknown error" }}"
            }
            
            Log.e(TAG, "Error message: $errorMessage")
            downloadDao.updateError(task.id, errorMessage)
            Result.failure(Exception(errorMessage, e))
        }
    }

    suspend fun cancelDownload(taskId: String) = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().destroyProcessById(taskId)
            Log.d(TAG, "Download cancelled: $taskId")
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling download", e)
        }
        downloadDao.updateStatus(
            taskId,
            DownloadProgress.DownloadStatus.CANCELLED
        )
    }

    suspend fun deleteDownload(taskId: String) = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().destroyProcessById(taskId)
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying process", e)
        }
        downloadDao.deleteDownloadById(taskId)
    }

    suspend fun clearCompletedDownloads() {
        downloadDao.clearCompletedDownloads()
    }

    suspend fun getPlaylistInfo(url: String): Result<PlaylistInfo> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== getPlaylistInfo START ===")
            Log.d(TAG, "URL: $url")

            val request = YoutubeDLRequest(url)
            request.addOption("--flat-playlist")
            request.addOption("--dump-single-json")
            request.addOption("--no-warnings")
            addCookieOptions(request)

            val response = YoutubeDL.getInstance().execute(request)
            val json = JSONObject(response.out)

            val title = json.optString("title", "Unknown Playlist")
            val uploader = json.optString("uploader", "").ifEmpty { null }
            val playlistId = json.optString("id", "")

            val entriesArray = json.optJSONArray("entries") ?: run {
                return@withContext Result.failure(Exception("No videos found in playlist"))
            }

            val entries = mutableListOf<PlaylistEntry>()
            for (i in 0 until entriesArray.length()) {
                val entry = entriesArray.getJSONObject(i)
                val videoId = entry.optString("id", "")
                if (videoId.isNotEmpty()) {
                    entries.add(
                        PlaylistEntry(
                            id = videoId,
                            title = entry.optString("title", "Video ${i + 1}"),
                            thumbnail = entry.optString("thumbnail", "")
                                .ifEmpty { null }?.takeIf { it != "null" },
                            duration = entry.optLong("duration", 0L).takeIf { it > 0 },
                            url = "https://www.youtube.com/watch?v=$videoId"
                        )
                    )
                }
            }

            Log.d(TAG, "Playlist: $title, ${entries.size} videos")

            val playlistInfo = PlaylistInfo(
                id = playlistId,
                title = title,
                author = uploader,
                videoCount = entries.size,
                entries = entries
            )

            Result.success(playlistInfo)
        } catch (e: Exception) {
            Log.e(TAG, "=== getPlaylistInfo FAILED ===", e)
            Result.failure(e)
        }
    }

    private fun addCookieOptions(request: YoutubeDLRequest) {
        // Check for cookie file
        val cookieFile = File(context.filesDir, "cookies.txt")
        if (cookieFile.exists()) {
            Log.d(TAG, "Using cookies from: ${cookieFile.absolutePath}")
            request.addOption("--cookies", cookieFile.absolutePath)
        } else {
            Log.d(TAG, "No cookie file found")
        }
    }

    private fun buildYoutubeDLRequest(task: DownloadTask): YoutubeDLRequest {
        Log.d(TAG, "=== buildYoutubeDLRequest ===")
        val request = YoutubeDLRequest(task.url)
        
        // Use output template that overwrites existing files
        val outputTemplate = "${task.outputPath}/%(title)s.%(ext)s"
        Log.d(TAG, "Output template: $outputTemplate")
        request.addOption("-o", outputTemplate)
        
        // Force overwrite if file exists (allows re-downloading same video)
        request.addOption("--force-overwrites")
        
        when (task.format.type) {
            DownloadFormat.FormatType.VIDEO -> {
                val qualityHeight = task.format.quality.filter { it.isDigit() }
                Log.d(TAG, "Video format, quality height: $qualityHeight")
                
                if (qualityHeight.isNotEmpty() && qualityHeight != "0") {
                    // Extract just the first 3-4 digits (the height), ignore FPS
                    val targetHeight = qualityHeight.take(4).filter { it.isDigit() }.toIntOrNull() ?: 1080
                    Log.d(TAG, "Requested quality raw: $qualityHeight, parsed height: ${targetHeight}p")
                    
                    // FIXED: Use explicit format IDs like Seal app
                    // YouTube format IDs: 137=1080p, 136=720p, 135=480p, 134=360p, 140=audio
                    // Format 18 is 360p combined - we want to avoid it for higher qualities
                    
                    val formatString = when (targetHeight) {
                        // Try specific format IDs first, then fall back to bv+ba, NEVER use format 18 for HD
                        4320 -> "401+140/313+140/bv[height<=4320]+ba/b[height<=4320]"
                        2160 -> "401+140/313+140/bv[height<=2160]+ba/b[height<=2160]"
                        1440 -> "400+140/271+140/bv[height<=1440]+ba/b[height<=1440]"
                        1080 -> "137+140/399+140/bv[height=1080][ext=mp4]+ba/bv[height=1080]+ba/bv[height<=1080]+ba/b[height<=1080]"
                        720 -> "136+140/398+140/bv[height=720][ext=mp4]+ba/bv[height=720]+ba/bv[height<=720]+ba/b[height<=720]"
                        480 -> "135+140/397+140/bv[height=480][ext=mp4]+ba/bv[height=480]+ba/bv[height<=480]+ba/b[height<=480]"
                        360 -> "134+140/396+140/bv[height=360][ext=mp4]+ba/bv[height=360]+ba/18/b[height<=360]"
                        else -> "bv[height<=$targetHeight][ext=mp4]+ba/bv[height<=$targetHeight]+ba/b[height<=$targetHeight]"
                    }
                    Log.d(TAG, "Format string: $formatString")
                    request.addOption("-f", formatString)

                    // Merge to mp4 (CRITICAL for separate video+audio)
                    request.addOption("--merge-output-format", "mp4")
                } else {
                    Log.d(TAG, "Using default best format")
                    request.addOption("-f", "bv+ba/b")
                    request.addOption("--merge-output-format", "mp4")
                }
            }
            DownloadFormat.FormatType.AUDIO -> {
                val codec = task.format.codec ?: "mp3"
                Log.d(TAG, "Audio format, codec: $codec")
                request.addOption("-x")
                request.addOption("--audio-format", codec)
                request.addOption("--audio-quality", "0")
                // WAV doesn't support embedded thumbnails
                if (codec != "wav") {
                    request.addOption("--embed-thumbnail")
                }
            }
        }
        
        // Sanitize filenames — prevent crashes from Unicode/special chars in titles
        request.addOption("--restrict-filenames")
        // Add options to bypass restrictions
        request.addOption("--add-metadata")
        request.addOption("--no-playlist")
        request.addOption("--no-warnings")
        request.addOption("--newline")
        request.addOption("--no-cache-dir")
        
        // Try to bypass age restrictions and geo-blocking
        request.addOption("--age-limit", "99")
        
        // Add cookies if available
        addCookieOptions(request)
        
        // Use android_vr client: serves HD DASH formats WITHOUT requiring PO tokens.
        // - android client: only 360p muxed, needs PO token for some videos
        // - web client: needs GVS PO token, gets 403 without it
        // - tv client: gets LOGIN_REQUIRED for some videos
        // - android_vr: NO PO token needed, full HD DASH formats available
        // DO NOT set custom --user-agent/--referer/--add-header (conflicts with yt-dlp per-client headers)
        request.addOption("--extractor-args", "youtube:player_client=android_vr")
        
        Log.d(TAG, "=== Request built successfully ===")
        return request
    }

    private fun getOutputDirectory(): File {
        Log.d(TAG, "=== getOutputDirectory ===")
        return try {
            // Use app-private external storage for downloads
            // This doesn't require WRITE_EXTERNAL_STORAGE permission on Android 10+
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                ?: context.getExternalFilesDir(null)
                ?: context.filesDir
            
            val appDir = File(baseDir, "Eclipse")
            Log.d(TAG, "App directory path: ${appDir.absolutePath}")
            
            if (!appDir.exists()) {
                val created = appDir.mkdirs()
                Log.d(TAG, "Directory created: $created")
                if (!created) {
                    Log.w(TAG, "Failed to create Eclipse directory, using fallback")
                    return baseDir
                }
            } else {
                Log.d(TAG, "Directory already exists")
            }
            
            Log.d(TAG, "Final directory: ${appDir.absolutePath}")
            Log.d(TAG, "Can write: ${appDir.canWrite()}")
            appDir
        } catch (e: Exception) {
            Log.e(TAG, "Error creating output directory", e)
            context.filesDir
        }
    }

    fun scanFileToGallery(filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "File doesn't exist, can't scan: $filePath")
                return
            }
            
            Log.d(TAG, "Scanning file to gallery: $filePath")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Insert into MediaStore
                addFileToMediaStore(file)
            } else {
                // Older Android - Use MediaScanner
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(filePath),
                    arrayOf(getMimeType(filePath)),
                    null
                )
                Log.d(TAG, "Scanned file via MediaScanner: $filePath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan file to gallery", e)
        }
    }
    
    private fun addFileToMediaStore(file: File) {
        try {
            val isVideo = file.name.endsWith(".mp4") || file.name.endsWith(".webm") ||
                         file.name.endsWith(".mkv") || file.name.endsWith(".mov")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

                // Check if this file already exists in MediaStore to avoid duplicates
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
                val selectionArgs = arrayOf(file.name, "$relativePath/")
                val cursor = context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null)
                val existingUri = cursor?.use {
                    if (it.moveToFirst()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        android.content.ContentUris.withAppendedId(collection, id)
                    } else null
                }

                if (existingUri != null) {
                    // File already in MediaStore — update it in place instead of creating a duplicate
                    context.contentResolver.openOutputStream(existingUri, "wt")?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    val updateValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                        put(MediaStore.MediaColumns.SIZE, file.length())
                    }
                    context.contentResolver.update(existingUri, updateValues, null, null)
                    Log.d(TAG, "Updated existing MediaStore entry: $existingUri")
                    return
                }

                // New file — insert into MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(file.absolutePath))
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                    put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
                    put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                    put(MediaStore.MediaColumns.SIZE, file.length())
                }

                val uri = context.contentResolver.insert(collection, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)

                    Log.d(TAG, "Added new file to MediaStore: $uri")
                } else {
                    Log.e(TAG, "Failed to insert into MediaStore")
                }
            } else {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf(getMimeType(file.absolutePath)),
                    null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to MediaStore", e)
        }
    }

    private fun getMimeType(filePath: String): String {
        return when {
            filePath.endsWith(".mp4") -> "video/mp4"
            filePath.endsWith(".mp3") -> "audio/mpeg"
            filePath.endsWith(".m4a") -> "audio/mp4"
            filePath.endsWith(".opus") -> "audio/opus"
            filePath.endsWith(".wav") -> "audio/wav"
            filePath.endsWith(".flac") -> "audio/flac"
            filePath.endsWith(".webm") -> "video/webm"
            filePath.endsWith(".mkv") -> "video/x-matroska"
            filePath.endsWith(".mov") -> "video/quicktime"
            else -> "*/*"
        }
    }

    private fun extractHeightFromResolution(resolution: String?): Int? {
        if (resolution == null) return null
        
        return when {
            resolution.contains("p") -> {
                // Format like "1080p" or "1080p60" - extract just the height before 'p'
                val beforeP = resolution.substringBefore("p")
                beforeP.filter { it.isDigit() }.toIntOrNull()
            }
            resolution.contains("x") -> {
                // Format like "1920x1080" - get height after x
                resolution.split("x").getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()
            }
            else -> resolution.filter { it.isDigit() }.toIntOrNull()
        }
    }

    private fun getFileExtension(format: DownloadFormat): String {
        return when (format.type) {
            DownloadFormat.FormatType.VIDEO -> "mp4"
            DownloadFormat.FormatType.AUDIO -> format.codec ?: "mp3"
        }
    }
}
