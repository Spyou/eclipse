package com.spyou.youtubedownload.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spyou.youtubedownload.data.model.DownloadFormat
import com.spyou.youtubedownload.data.model.VideoFormat
import com.spyou.youtubedownload.data.model.VideoInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatSelector(
    videoInfo: VideoInfo,
    selectedFormat: DownloadFormat?,
    onFormatSelected: (DownloadFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxWidth()) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
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
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
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

        when (selectedTab) {
            0 -> VideoFormatsList(
                videoInfo = videoInfo,
                selectedFormat = selectedFormat,
                onFormatSelected = onFormatSelected
            )
            1 -> AudioFormatsList(
                videoInfo = videoInfo,
                selectedFormat = selectedFormat,
                onFormatSelected = onFormatSelected
            )
        }
    }
}

@Composable
private fun VideoFormatsList(
    videoInfo: VideoInfo,
    selectedFormat: DownloadFormat?,
    onFormatSelected: (DownloadFormat) -> Unit
) {
    val formatsByHeight = videoInfo.formats
        .filter { it.hasVideo && !it.isAudioOnly }
        .groupBy { extractHeight(it.resolution) }
        .mapValues { (_, formats) ->
            formats.maxWithOrNull(compareBy(
                { it.hasAudio },
                { it.filesize ?: it.filesizeApprox ?: 0L }
            ))
        }
        .filter { it.key != null && it.value != null }
        .toSortedMap(compareByDescending { it })

    if (formatsByHeight.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No video formats available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(formatsByHeight.toList()) { (height, format) ->
            if (height == null || format == null) return@items

            val isSelected = selectedFormat?.type == DownloadFormat.FormatType.VIDEO &&
                    selectedFormat.quality == height.toString()

            val (title, description) = when (height) {
                4320 -> "8K Ultra HD" to "MP4 • Maximum quality"
                2160 -> "4K Ultra HD" to "MP4 • Crystal clear"
                1440 -> "2K Quad HD" to "MP4 • Very sharp"
                1080 -> "Full HD" to "MP4 • Recommended"
                720 -> "HD" to "MP4 • Good quality, smaller file"
                480 -> "SD" to "MP4 • Standard quality"
                360 -> "Low Quality" to "MP4 • Small file, faster download"
                240 -> "Very Low Quality" to "MP4 • Fastest download"
                else -> "${height}p" to "MP4"
            }

            val fileSize = format.formattedFileSize

            FormatCard(
                badgeText = "${height}p",
                title = title,
                description = description,
                details = if (fileSize != "Unknown size") fileSize else "",
                isSelected = isSelected,
                onClick = { onFormatSelected(DownloadFormat.video(height.toString())) }
            )
        }
    }
}

@Composable
private fun AudioFormatsList(
    videoInfo: VideoInfo,
    selectedFormat: DownloadFormat?,
    onFormatSelected: (DownloadFormat) -> Unit
) {
    val audioFormats = listOf(
        Triple("MP3", "320kbps", "Most compatible"),
        Triple("M4A", "256kbps", "Apple devices"),
        Triple("FLAC", "Lossless", "Lossless quality"),
        Triple("OPUS", "160kbps", "Best compression"),
        Triple("WAV", "Lossless", "Uncompressed")
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(audioFormats) { (codec, bitrate, desc) ->
            val isSelected = selectedFormat?.type == DownloadFormat.FormatType.AUDIO &&
                    selectedFormat.codec?.equals(codec, ignoreCase = true) == true

            FormatCard(
                badgeText = codec,
                title = "$codec Audio",
                description = bitrate,
                details = desc,
                isSelected = isSelected,
                onClick = { onFormatSelected(DownloadFormat.audio(codec.lowercase())) }
            )
        }
    }
}

@Composable
private fun FormatCard(
    badgeText: String,
    title: String,
    description: String,
    details: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed-width badge text
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(56.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Text content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (description.isNotEmpty() || details.isNotEmpty()) {
                    Text(
                        text = listOf(description, details).filter { it.isNotEmpty() }.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // RadioButton
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        }
    }
}

private fun extractHeight(resolution: String?): Int? {
    if (resolution == null) return null

    return when {
        resolution.contains("p") -> resolution.filter { it.isDigit() }.toIntOrNull()
        resolution.contains("x") -> {
            resolution.split("x").getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()
        }
        else -> resolution.filter { it.isDigit() }.toIntOrNull()
    }
}
