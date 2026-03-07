package com.spyou.youtubedownload.domain.usecase

import com.spyou.youtubedownload.data.repository.DownloadRepository

class DeleteDownloadUseCase(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(taskId: String) {
        repository.deleteDownload(taskId)
    }
}
