package com.spyou.youtubedownload.domain.usecase

import com.spyou.youtubedownload.data.local.database.DownloadEntity
import com.spyou.youtubedownload.data.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

class GetDownloadsUseCase(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<List<DownloadEntity>> {
        return repository.allDownloads
    }
}

class GetActiveDownloadsUseCase(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<List<DownloadEntity>> {
        return repository.activeDownloads
    }
}

class GetCompletedDownloadsUseCase(
    private val repository: DownloadRepository
) {
    operator fun invoke(): Flow<List<DownloadEntity>> {
        return repository.completedDownloads
    }
}
