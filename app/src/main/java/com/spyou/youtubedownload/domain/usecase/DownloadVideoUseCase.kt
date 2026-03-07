package com.spyou.youtubedownload.domain.usecase

import com.spyou.youtubedownload.data.model.DownloadFormat
import com.spyou.youtubedownload.data.model.DownloadProgress
import com.spyou.youtubedownload.data.model.DownloadTask
import com.spyou.youtubedownload.data.model.VideoInfo
import com.spyou.youtubedownload.data.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DownloadVideoUseCase(
    private val repository: DownloadRepository
) {
    operator fun invoke(
        url: String,
        videoInfo: VideoInfo,
        format: DownloadFormat
    ): Flow<DownloadState> = flow {
        emit(DownloadState.Loading)
        
        try {
            // Create download task
            val task = repository.createDownloadTask(url, videoInfo, format)
            emit(DownloadState.TaskCreated(task))
            
            // Start download
            val result = repository.startDownload(task) { progress ->
                // Progress updates will be handled via database observation
            }
            
            result.fold(
                onSuccess = { path ->
                    emit(DownloadState.Success(path))
                },
                onFailure = { error ->
                    emit(DownloadState.Error(error.message ?: "Download failed"))
                }
            )
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Unknown error"))
        }
    }
    
    sealed class DownloadState {
        object Loading : DownloadState()
        data class TaskCreated(val task: DownloadTask) : DownloadState()
        data class Success(val outputPath: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }
}
