package com.spyou.youtubedownload.ui.screens.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spyou.youtubedownload.YouTubeDownloaderApp
import com.spyou.youtubedownload.data.local.database.DownloadDatabase
import com.spyou.youtubedownload.data.local.database.DownloadEntity
import com.spyou.youtubedownload.data.model.DownloadFormat
import com.spyou.youtubedownload.data.model.PlaylistInfo
import com.spyou.youtubedownload.data.model.VideoInfo
import com.spyou.youtubedownload.data.repository.DownloadRepository
import com.spyou.youtubedownload.domain.usecase.GetVideoInfoUseCase
import com.spyou.youtubedownload.worker.DownloadQueueManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val TAG = "HomeViewModel"
    }

    private val database = DownloadDatabase.getDatabase(application)
    private val repository = DownloadRepository(application, database.downloadDao())
    private val getVideoInfoUseCase = GetVideoInfoUseCase(repository)
    private val downloadQueueManager = DownloadQueueManager.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _videoInfo = MutableStateFlow<VideoInfo?>(null)
    val videoInfo: StateFlow<VideoInfo?> = _videoInfo.asStateFlow()

    private val _showFormatDialog = MutableStateFlow(false)
    val showFormatDialog: StateFlow<Boolean> = _showFormatDialog.asStateFlow()

    val recentDownloads: StateFlow<List<DownloadEntity>> = repository.recentDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _playlistInfo = MutableStateFlow<PlaylistInfo?>(null)
    val playlistInfo: StateFlow<PlaylistInfo?> = _playlistInfo.asStateFlow()

    private val _showPlaylistDialog = MutableStateFlow(false)
    val showPlaylistDialog: StateFlow<Boolean> = _showPlaylistDialog.asStateFlow()

    private var currentUrl: String = ""

    init {
        Log.d(TAG, "=== HomeViewModel initialized ===")
        Log.d(TAG, "Libraries initialized: ${YouTubeDownloaderApp.isInitialized}")
        
        if (!YouTubeDownloaderApp.isInitialized) {
            _uiState.update { it.copy(error = "Failed to initialize download library. Please restart the app.") }
        }
    }

    fun onUrlChange(url: String) {
        _uiState.update { it.copy(url = url, error = null) }
    }

    fun fetchVideoInfo() {
        Log.d(TAG, "=== fetchVideoInfo called ===")
        
        if (!YouTubeDownloaderApp.isInitialized) {
            Log.e(TAG, "Libraries not initialized!")
            _uiState.update { it.copy(error = "Download library not initialized. Please restart the app.") }
            return
        }

        val url = _uiState.value.url.trim()
        Log.d(TAG, "URL entered: $url")
        
        if (url.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a valid URL") }
            return
        }

        if (!isValidYouTubeUrl(url)) {
            Log.e(TAG, "Invalid URL format: $url")
            _uiState.update { it.copy(error = "Invalid YouTube URL. Use youtube.com or youtu.be links.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            Log.d(TAG, "Fetching video info...")

            try {
                // Detect playlist URLs
                if (url.contains("list=")) {
                    Log.d(TAG, "Detected playlist URL")
                    repository.getPlaylistInfo(url)
                        .onSuccess { playlist ->
                            Log.d(TAG, "Playlist info fetched: ${playlist.title}, ${playlist.videoCount} videos")
                            _playlistInfo.value = playlist
                            _showPlaylistDialog.value = true
                            _uiState.update { it.copy(isLoading = false) }
                        }
                        .onFailure { error ->
                            val errorMsg = error.message ?: "Failed to fetch playlist info"
                            Log.e(TAG, "Failed to fetch playlist: $errorMsg")
                            _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                        }
                } else {
                    getVideoInfoUseCase(url)
                        .onSuccess { info ->
                            Log.d(TAG, "Video info fetched successfully: ${info.title}")
                            Log.d(TAG, "Available formats: ${info.formats.size}")
                            _videoInfo.value = info
                            currentUrl = url
                            _showFormatDialog.value = true
                            _uiState.update { it.copy(isLoading = false) }
                        }
                        .onFailure { error ->
                            val errorMsg = error.message ?: "Failed to fetch video info"
                            Log.e(TAG, "Failed to fetch video: $errorMsg")
                            Log.e(TAG, "Exception:", error)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = errorMsg
                                )
                            }
                        }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in fetchVideoInfo", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }

    fun startDownload(format: DownloadFormat) {
        Log.d(TAG, "=== startDownload called ===")
        Log.d(TAG, "Format: ${format.type}, Quality: ${format.quality}, Codec: ${format.codec}")

        if (!YouTubeDownloaderApp.isInitialized) {
            Log.e(TAG, "Libraries not initialized!")
            _uiState.update { it.copy(error = "Download library not initialized") }
            return
        }

        val info = _videoInfo.value ?: run {
            Log.e(TAG, "No video info available")
            _uiState.update { it.copy(error = "No video info available") }
            return
        }

        viewModelScope.launch {
            _showFormatDialog.value = false
            _uiState.update { it.copy(isDownloading = true, error = null) }

            try {
                Log.d(TAG, "Creating download task...")
                val task = repository.createDownloadTask(currentUrl, info, format)
                Log.d(TAG, "Task created with ID: ${task.id}")

                // Enqueue via WorkManager — survives app going to background, auto-retries
                downloadQueueManager.enqueueDownload(
                    taskId = task.id,
                    url = currentUrl,
                    videoInfo = info,
                    format = format,
                    outputPath = task.outputPath,
                    fileName = task.fileName
                )
                Log.d(TAG, "Download enqueued via WorkManager")

                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        successMessage = "Download started! Check Downloads tab for progress.",
                        url = ""
                    )
                }
                _videoInfo.value = null

                // Clear success message after delay
                delay(3000)
                _uiState.update { it.copy(successMessage = null) }

            } catch (e: Exception) {
                Log.e(TAG, "Exception in startDownload", e)
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        error = e.message ?: "Failed to start download"
                    )
                }
            }
        }
    }

    fun dismissFormatDialog() {
        _showFormatDialog.value = false
        _videoInfo.value = null
    }

    fun dismissPlaylistDialog() {
        _showPlaylistDialog.value = false
        _playlistInfo.value = null
    }

    fun startPlaylistDownload(format: DownloadFormat) {
        val playlist = _playlistInfo.value ?: return
        Log.d(TAG, "=== startPlaylistDownload: ${playlist.entries.size} videos, format: ${format.type}/${format.quality} ===")

        _showPlaylistDialog.value = false

        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, error = null) }

            try {
                for (entry in playlist.entries) {
                    Log.d(TAG, "Enqueuing: ${entry.title}")
                    val videoInfo = VideoInfo(
                        id = entry.id,
                        title = entry.title,
                        thumbnail = entry.thumbnail,
                        duration = entry.duration
                    )
                    val task = repository.createDownloadTask(
                        url = entry.url,
                        videoInfo = videoInfo,
                        format = format
                    )
                    downloadQueueManager.enqueueDownload(
                        taskId = task.id,
                        url = entry.url,
                        videoInfo = videoInfo,
                        format = format,
                        outputPath = task.outputPath,
                        fileName = task.fileName
                    )
                }

                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        successMessage = "Downloading ${playlist.entries.size} videos from playlist!",
                        url = ""
                    )
                }
                _playlistInfo.value = null

                delay(3000)
                _uiState.update { it.copy(successMessage = null) }
            } catch (e: Exception) {
                Log.e(TAG, "Exception in startPlaylistDownload", e)
                _uiState.update {
                    it.copy(
                        isDownloading = false,
                        error = e.message ?: "Failed to start playlist download"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun isValidYouTubeUrl(url: String): Boolean {
        val result = url.contains("youtube.com") || url.contains("youtu.be")
        Log.d(TAG, "URL validation: $url -> $result")
        return result
    }
}

data class HomeUiState(
    val url: String = "",
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
