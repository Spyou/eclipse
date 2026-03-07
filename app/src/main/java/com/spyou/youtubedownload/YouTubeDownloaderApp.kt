package com.spyou.youtubedownload

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class YouTubeDownloaderApp : Application() {

    companion object {
        const val TAG = "EclipseApp"
        var isInitialized = false
            private set
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initializeLibraries()
    }

    private fun initializeLibraries() {
        try {
            YoutubeDL.getInstance().init(this)
            Log.i(TAG, "YoutubeDL initialized successfully")

            FFmpeg.getInstance().init(this)
            Log.i(TAG, "FFmpeg initialized successfully")

            isInitialized = true

            // Update yt-dlp in background to fix YouTube extraction issues
            appScope.launch {
                updateYtDlp()
            }
        } catch (e: YoutubeDLException) {
            Log.e(TAG, "Failed to initialize libraries", e)
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during initialization", e)
            isInitialized = false
        }
    }

    private fun updateYtDlp() {
        try {
            val status = YoutubeDL.getInstance().updateYoutubeDL(
                this,
                YoutubeDL.UpdateChannel.NIGHTLY
            )
            Log.i(TAG, "yt-dlp update status: $status")
        } catch (e: Exception) {
            Log.w(TAG, "yt-dlp update failed (will use bundled version): ${e.message}")
        }
    }
}
