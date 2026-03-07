package com.spyou.youtubedownload.util

import android.content.Context
import android.os.Environment
import java.io.File

object StorageHelper {
    
    fun getDownloadsDirectory(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDir = File(downloadsDir, "YouTubeDownloader")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        return appDir
    }
    
    fun getAvailableStorage(): Long {
        val stat = android.os.StatFs(Environment.getExternalStorageDirectory().path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        return availableBlocks * blockSize
    }
    
    fun formatAvailableStorage(): String {
        return getAvailableStorage().formatAsFileSize()
    }
}
