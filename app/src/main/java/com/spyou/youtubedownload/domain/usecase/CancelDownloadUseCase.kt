package com.spyou.youtubedownload.domain.usecase

import com.spyou.youtubedownload.data.repository.DownloadRepository

class CancelDownloadUseCase(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(taskId: String) {
        repository.cancelDownload(taskId)
    }
}
