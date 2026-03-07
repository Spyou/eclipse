package com.spyou.youtubedownload.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.spyou.youtubedownload.data.local.database.DownloadEntity
import java.io.File

object FileOpener {

    private const val TAG = "FileOpener"

    fun openDownload(context: Context, download: DownloadEntity) {
        val file = findDownloadedFile(download)
        if (file == null) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = getMimeType(file.name)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Try without explicit MIME type as fallback
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Open with"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file", e)
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findDownloadedFile(download: DownloadEntity): File? {
        val dir = File(download.outputPath)
        if (!dir.exists()) return null

        // Try exact filename first
        download.fileName?.let { fileName ->
            val exact = File(dir, fileName)
            if (exact.exists()) return exact
        }

        // Fallback: find most recently modified file in the output directory
        return dir.listFiles()
            ?.filter { it.isFile && !it.name.startsWith(".") }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".mp4") -> "video/mp4"
            fileName.endsWith(".webm") -> "video/webm"
            fileName.endsWith(".mkv") -> "video/x-matroska"
            fileName.endsWith(".mov") -> "video/quicktime"
            fileName.endsWith(".mp3") -> "audio/mpeg"
            fileName.endsWith(".m4a") -> "audio/mp4"
            fileName.endsWith(".opus") -> "audio/opus"
            fileName.endsWith(".wav") -> "audio/wav"
            fileName.endsWith(".flac") -> "audio/flac"
            else -> "*/*"
        }
    }
}
