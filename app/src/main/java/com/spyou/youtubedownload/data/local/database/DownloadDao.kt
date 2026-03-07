package com.spyou.youtubedownload.data.local.database

import androidx.room.*
import com.spyou.youtubedownload.data.model.DownloadProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE progressStatus = 'DOWNLOADING' OR progressStatus = 'PENDING' ORDER BY createdAt DESC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE progressStatus = 'COMPLETED' ORDER BY completedAt DESC")
    fun getCompletedDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE progressStatus = 'COMPLETED' ORDER BY completedAt DESC LIMIT 5")
    fun getRecentCompletedDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)

    @Update
    suspend fun updateDownload(download: DownloadEntity)

    @Query("UPDATE downloads SET progress = :progress, progressStatus = :status, etaInSeconds = :eta, speed = :speed WHERE id = :id")
    suspend fun updateProgress(
        id: String,
        progress: Float,
        status: DownloadProgress.DownloadStatus,
        eta: Long? = null,
        speed: String? = null
    )

    @Query("UPDATE downloads SET progressStatus = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateStatus(
        id: String,
        status: DownloadProgress.DownloadStatus,
        completedAt: Long? = null
    )

    @Query("UPDATE downloads SET errorMessage = :errorMessage, progressStatus = 'FAILED' WHERE id = :id")
    suspend fun updateError(id: String, errorMessage: String)

    @Delete
    suspend fun deleteDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownloadById(id: String)

    @Query("DELETE FROM downloads WHERE progressStatus = 'COMPLETED'")
    suspend fun clearCompletedDownloads()
}
