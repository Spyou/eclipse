package com.spyou.youtubedownload.domain.usecase

import com.spyou.youtubedownload.data.model.VideoInfo
import com.spyou.youtubedownload.data.repository.DownloadRepository

class GetVideoInfoUseCase(
    private val repository: DownloadRepository
) {
    suspend operator fun invoke(url: String): Result<VideoInfo> {
        return repository.getVideoInfo(url)
    }
}
