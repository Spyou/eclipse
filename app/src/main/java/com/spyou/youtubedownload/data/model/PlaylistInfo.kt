package com.spyou.youtubedownload.data.model

data class PlaylistInfo(
    val id: String,
    val title: String,
    val author: String? = null,
    val videoCount: Int,
    val entries: List<PlaylistEntry>
)

data class PlaylistEntry(
    val id: String,
    val title: String,
    val thumbnail: String? = null,
    val duration: Long? = null,
    val url: String
)
