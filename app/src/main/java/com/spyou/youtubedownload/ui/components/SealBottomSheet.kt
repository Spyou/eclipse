package com.spyou.youtubedownload.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.spyou.youtubedownload.data.model.DownloadFormat
import com.spyou.youtubedownload.data.model.VideoFormat
import com.spyou.youtubedownload.data.model.VideoInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SealBottomSheet(
    videoInfo: VideoInfo?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onDownload: (DownloadFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf<DownloadFormat?>(null) }
    var isAudio by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            when {
                isLoading -> LoadingState()
                error != null -> ErrorState(error = error)
                videoInfo != null -> {
                    VideoPreviewSection(videoInfo = videoInfo)

                    Spacer(modifier = Modifier.height(20.dp))

                    // SegmentedButtonRow instead of custom TypeSelector
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = !isAudio,
                            onClick = { isAudio = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        ) {
                            Text("Video")
                        }
                        SegmentedButton(
                            selected = isAudio,
                            onClick = { isAudio = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        ) {
                            Text("Audio")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Format Grid
                    if (isAudio) {
                        AudioFormatGrid(
                            selectedFormat = selectedFormat,
                            onFormatSelected = { selectedFormat = it }
                        )
                    } else {
                        VideoFormatGrid(
                            videoInfo = videoInfo,
                            selectedFormat = selectedFormat,
                            onFormatSelected = { selectedFormat = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Download Button — no Add icon, 48dp height
                    Button(
                        onClick = {
                            selectedFormat?.let { onDownload(it) }
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = selectedFormat != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = selectedFormat?.let {
                                "Download ${it.quality}"
                            } ?: "Select Format",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPreviewSection(videoInfo: VideoInfo) {
    Column {
        // Large Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = videoInfo.thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Duration badge
            videoInfo.duration?.let { duration ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = videoInfo.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Author
        videoInfo.author?.let { author ->
            Text(
                text = author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VideoFormatGrid(
    videoInfo: VideoInfo,
    selectedFormat: DownloadFormat?,
    onFormatSelected: (DownloadFormat) -> Unit
) {
    val heights = videoInfo.formats
        .filter { it.hasVideo && !it.isAudioOnly }
        .mapNotNull { format ->
            format.resolution?.filter { it.isDigit() }?.toIntOrNull()
        }
        .distinct()
        .sortedDescending()

    val formats = heights.map { height ->
        val label = when (height) {
            4320 -> "8K"
            2160 -> "4K"
            1440 -> "2K"
            1080 -> "1080p"
            720 -> "720p"
            480 -> "480p"
            360 -> "360p"
            240 -> "240p"
            else -> "${height}p"
        }
        val desc = when (height) {
            4320 -> "4320p"
            2160 -> "2160p"
            1440 -> "1440p"
            1080 -> "Full HD"
            720 -> "HD"
            480 -> "SD"
            360 -> "Low"
            else -> "${height}p"
        }
        Triple(height, label, desc)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 200.dp)
    ) {
        items(formats) { (height, label, desc) ->
            val isSelected = selectedFormat?.type == DownloadFormat.FormatType.VIDEO &&
                    selectedFormat.quality == height.toString()

            FormatChip(
                label = label,
                description = desc,
                isSelected = isSelected,
                onClick = { onFormatSelected(DownloadFormat.video(height.toString())) }
            )
        }
    }
}

@Composable
private fun AudioFormatGrid(
    selectedFormat: DownloadFormat?,
    onFormatSelected: (DownloadFormat) -> Unit
) {
    val formats = listOf(
        Triple("MP3", "320kbps", "Best quality"),
        Triple("M4A", "256kbps", "Apple devices"),
        Triple("OPUS", "160kbps", "Smallest")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 120.dp)
    ) {
        items(formats) { (codec, bitrate, desc) ->
            val isSelected = selectedFormat?.type == DownloadFormat.FormatType.AUDIO &&
                    selectedFormat.codec?.equals(codec, ignoreCase = true) == true

            FormatChip(
                label = codec,
                description = bitrate,
                isSelected = isSelected,
                onClick = { onFormatSelected(DownloadFormat.audio(codec.lowercase())) }
            )
        }
    }
}

@Composable
private fun FormatChip(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 4.dp
        )
        Text(
            text = "Loading video info...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(error: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
