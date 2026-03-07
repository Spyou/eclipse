package com.spyou.youtubedownload.data.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoInfo(
    val id: String,
    val title: String,
    val description: String? = null,
    val thumbnail: String? = null,
    val author: String? = null,
    val duration: Long? = null,
    val viewCount: Long? = null,
    val uploadDate: String? = null,
    val formats: List<VideoFormat> = emptyList(),
    val subtitles: Map<String, List<SubtitleInfo>> = emptyMap()
)

@Serializable
data class VideoFormat(
    val formatId: String,
    val ext: String,
    val quality: String? = null,
    val resolution: String? = null,
    val fps: Float? = null,
    val filesize: Long? = null,
    val filesizeApprox: Long? = null,
    val videoCodec: String? = null,
    val audioCodec: String? = null,
    val abr: Float? = null, // Audio bitrate
    val vbr: Float? = null, // Video bitrate
    val asr: Int? = null,   // Audio sample rate
    val formatNote: String? = null,
    val hasVideo: Boolean = true,
    val hasAudio: Boolean = true
) {
    val displayQuality: String
        get() = when {
            resolution != null && resolution != "audio only" -> resolution
            quality != null && quality != "audio only" -> quality
            abr != null -> "${abr.toInt()}kbps"
            else -> "Unknown"
        }

    val isAudioOnly: Boolean
        get() = resolution == null || resolution == "audio only"

    val formattedFileSize: String
        get() = when {
            filesize != null && filesize > 0 -> formatBytes(filesize)
            filesizeApprox != null && filesizeApprox > 0 -> "~${formatBytes(filesizeApprox)}"
            else -> "Unknown size"
        }

    private fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }
}

@Serializable
data class SubtitleInfo(
    val url: String,
    val name: String? = null,
    val ext: String? = null
)

@Serializable
data class DownloadFormat(
    val type: FormatType,
    val quality: String,
    val codec: String? = null
) {
    enum class FormatType {
        VIDEO,
        AUDIO
    }

    companion object {
        fun video(quality: String) = DownloadFormat(FormatType.VIDEO, quality)
        fun audio(codec: String) = DownloadFormat(FormatType.AUDIO, codec, codec)
    }
}

@Serializable
data class DownloadProgress(
    val progress: Float,
    val etaInSeconds: Long? = null,
    val speed: String? = null,
    val downloadedBytes: Long? = null,
    val totalBytes: Long? = null,
    val status: DownloadStatus = DownloadStatus.DOWNLOADING
) {
    enum class DownloadStatus {
        PENDING,
        DOWNLOADING,
        PAUSED,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    val progressPercent: Int
        get() = (progress * 100).toInt()
}
