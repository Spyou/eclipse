package com.spyou.youtubedownload.worker

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.work.*
import com.spyou.youtubedownload.data.model.DownloadFormat
import com.spyou.youtubedownload.data.model.VideoInfo
import java.util.concurrent.TimeUnit

class DownloadQueueManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun enqueueDownload(
        taskId: String,
        url: String,
        videoInfo: VideoInfo,
        format: DownloadFormat,
        outputPath: String,
        fileName: String?
    ): Operation {
        val inputData = workDataOf(
            DownloadWorker.KEY_TASK_ID to taskId,
            DownloadWorker.KEY_URL to url,
            DownloadWorker.KEY_VIDEO_TITLE to videoInfo.title,
            DownloadWorker.KEY_FORMAT_TYPE to format.type.name,
            DownloadWorker.KEY_FORMAT_QUALITY to format.quality,
            DownloadWorker.KEY_FORMAT_CODEC to (format.codec ?: ""),
            DownloadWorker.KEY_OUTPUT_PATH to outputPath,
            DownloadWorker.KEY_FILE_NAME to (fileName ?: ""),
            DownloadWorker.KEY_VIDEO_ID to videoInfo.id
        )

        // Create constraints - require network
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Build the work request
        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10000L, // 10 seconds minimum backoff
                TimeUnit.MILLISECONDS
            )
            .addTag("download")
            .addTag("download_$taskId")
            .build()

        // Enqueue unique work (prevents duplicate downloads)
        return workManager.enqueueUniqueWork(
            "download_$taskId",
            ExistingWorkPolicy.KEEP,
            downloadWorkRequest
        )
    }

    fun cancelDownload(taskId: String) {
        workManager.cancelUniqueWork("download_$taskId")
    }

    fun cancelAllDownloads() {
        workManager.cancelAllWorkByTag("download")
    }

    fun observeDownloadWork(taskId: String): LiveData<WorkInfo?> {
        return workManager.getWorkInfosForUniqueWorkLiveData("download_$taskId")
            .map { workInfos -> workInfos.firstOrNull() }
    }

    fun observeAllDownloads(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData("download")
    }

    companion object {
        @Volatile
        private var instance: DownloadQueueManager? = null

        fun getInstance(context: Context): DownloadQueueManager {
            return instance ?: synchronized(this) {
                instance ?: DownloadQueueManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
