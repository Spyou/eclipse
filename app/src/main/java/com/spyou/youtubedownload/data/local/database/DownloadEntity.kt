package com.spyou.youtubedownload.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.spyou.youtubedownload.data.model.DownloadFormat
import com.spyou.youtubedownload.data.model.DownloadProgress

@Entity(tableName = "downloads")
@TypeConverters(Converters::class)
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String? = null,
    val author: String? = null,
    val thumbnail: String? = null,
    val duration: Long? = null,
    val formatType: DownloadFormat.FormatType,
    val formatQuality: String,
    val formatCodec: String? = null,
    val outputPath: String,
    val fileName: String? = null,
    val progress: Float = 0f,
    val progressStatus: DownloadProgress.DownloadStatus = DownloadProgress.DownloadStatus.PENDING,
    val etaInSeconds: Long? = null,
    val speed: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val videoId: String? = null
)
