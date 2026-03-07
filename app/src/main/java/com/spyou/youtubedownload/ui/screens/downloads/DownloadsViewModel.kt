package com.spyou.youtubedownload.ui.screens.downloads

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.spyou.youtubedownload.data.local.database.DownloadDatabase
import com.spyou.youtubedownload.data.local.database.DownloadEntity
import com.spyou.youtubedownload.data.repository.DownloadRepository
import com.spyou.youtubedownload.util.FileOpener
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DownloadDatabase.getDatabase(application)
    private val repository = DownloadRepository(application, database.downloadDao())

    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    val allDownloads: StateFlow<List<DownloadEntity>> = repository.allDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val activeDownloads: StateFlow<List<DownloadEntity>> = repository.activeDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val completedDownloads: StateFlow<List<DownloadEntity>> = repository.completedDownloads
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun cancelDownload(taskId: String) {
        viewModelScope.launch {
            repository.cancelDownload(taskId)
        }
    }

    fun deleteDownload(taskId: String) {
        viewModelScope.launch {
            repository.deleteDownload(taskId)
        }
    }

    fun clearCompletedDownloads() {
        viewModelScope.launch {
            repository.clearCompletedDownloads()
        }
    }

    fun openDownloadedFile(context: Context, download: DownloadEntity) {
        FileOpener.openDownload(context, download)
    }
}

data class DownloadsUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
