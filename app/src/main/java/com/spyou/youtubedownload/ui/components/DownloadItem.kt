package com.spyou.youtubedownload.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spyou.youtubedownload.data.local.database.DownloadEntity
import com.spyou.youtubedownload.data.model.DownloadProgress

@Composable
fun DownloadItem(
    download: DownloadEntity,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onCancelClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onOpenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                AsyncImage(
                    model = download.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp, 54.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = download.title ?: "Unknown",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${download.author ?: ""} • ${download.formatQuality}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = getStatusText(download),
                        style = MaterialTheme.typography.labelSmall,
                        color = getStatusColor(download.progressStatus)
                    )
                }

                // Action buttons
                Row {
                    when (download.progressStatus) {
                        DownloadProgress.DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPauseClick) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        }
                        DownloadProgress.DownloadStatus.PENDING -> {
                            IconButton(onClick = onCancelClick) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        }
                        DownloadProgress.DownloadStatus.COMPLETED -> {
                            IconButton(onClick = onOpenClick) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Open")
                            }
                            IconButton(onClick = onDeleteClick) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                        else -> {
                            IconButton(onClick = onDeleteClick) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }

            // Progress bar for active downloads
            if (download.progressStatus == DownloadProgress.DownloadStatus.DOWNLOADING ||
                download.progressStatus == DownloadProgress.DownloadStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { download.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(download.progress * 100).toInt()}%${download.etaInSeconds?.let { " • ETA: ${it}s" } ?: ""}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun getStatusText(download: DownloadEntity): String {
    return when (download.progressStatus) {
        DownloadProgress.DownloadStatus.PENDING -> "Pending"
        DownloadProgress.DownloadStatus.DOWNLOADING -> "Downloading"
        DownloadProgress.DownloadStatus.PAUSED -> "Paused"
        DownloadProgress.DownloadStatus.COMPLETED -> "Completed"
        DownloadProgress.DownloadStatus.FAILED -> "Failed: ${download.errorMessage ?: ""}"
        DownloadProgress.DownloadStatus.CANCELLED -> "Cancelled"
    }
}

@Composable
private fun getStatusColor(status: DownloadProgress.DownloadStatus) = when (status) {
    DownloadProgress.DownloadStatus.PENDING -> MaterialTheme.colorScheme.outline
    DownloadProgress.DownloadStatus.DOWNLOADING -> MaterialTheme.colorScheme.primary
    DownloadProgress.DownloadStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
    DownloadProgress.DownloadStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    DownloadProgress.DownloadStatus.FAILED -> MaterialTheme.colorScheme.error
    DownloadProgress.DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline
}
