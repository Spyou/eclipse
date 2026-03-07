package com.spyou.youtubedownload.data.local.database

import androidx.room.TypeConverter
import com.spyou.youtubedownload.data.model.DownloadFormat
import com.spyou.youtubedownload.data.model.DownloadProgress

class Converters {

    @TypeConverter
    fun fromFormatType(value: DownloadFormat.FormatType): String {
        return value.name
    }

    @TypeConverter
    fun toFormatType(value: String): DownloadFormat.FormatType {
        return DownloadFormat.FormatType.valueOf(value)
    }

    @TypeConverter
    fun fromDownloadStatus(value: DownloadProgress.DownloadStatus): String {
        return value.name
    }

    @TypeConverter
    fun toDownloadStatus(value: String): DownloadProgress.DownloadStatus {
        return DownloadProgress.DownloadStatus.valueOf(value)
    }
}
