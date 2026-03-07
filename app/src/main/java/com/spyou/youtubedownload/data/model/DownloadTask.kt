package com.spyou.youtubedownload.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class DownloadTask(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val videoInfo: VideoInfo? = null,
    val format: DownloadFormat,
    val outputPath: String,
    val fileName: String? = null,
    val progress: DownloadProgress = DownloadProgress(0f),
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val errorMessage: String? = null
) {
    val displayTitle: String
        get() = videoInfo?.title ?: fileName ?: "Unknown"

    val isActive: Boolean
        get() = progress.status == DownloadProgress.DownloadStatus.DOWNLOADING ||
                progress.status == DownloadProgress.DownloadStatus.PENDING

    val isCompleted: Boolean
        get() = progress.status == DownloadProgress.DownloadStatus.COMPLETED

    val hasFailed: Boolean
        get() = progress.status == DownloadProgress.DownloadStatus.FAILED
}
