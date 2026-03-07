package com.spyou.youtubedownload.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
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
import com.spyou.youtubedownload.data.model.VideoInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadBottomSheet(
    videoInfo: VideoInfo?,
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onDownload: (DownloadFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFormat by remember { mutableStateOf<DownloadFormat?>(null) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = "Download",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            when {
                isLoading -> {
                    LoadingContent()
                }
                error != null -> {
                    ErrorContent(error = error)
                }
                videoInfo != null -> {
                    VideoInfoContent(
                        videoInfo = videoInfo,
                        selectedFormat = selectedFormat,
                        onFormatSelected = { selectedFormat = it }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Download button
                    Button(
                        onClick = { 
                            selectedFormat?.let { onDownload(it) }
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = selectedFormat != null,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Download",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            
            // Bottom spacing for navigation bar
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
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
private fun ErrorContent(error: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun VideoInfoContent(
    videoInfo: VideoInfo,
    selectedFormat: DownloadFormat?,
    onFormatSelected: (DownloadFormat) -> Unit
) {
    // Video thumbnail and title
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Thumbnail
        AsyncImage(
            model = videoInfo.thumbnail,
            contentDescription = null,
            modifier = Modifier
                .size(120.dp, 68.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        
        // Title and duration
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = videoInfo.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            videoInfo.duration?.let { duration ->
                val minutes = duration / 60
                val seconds = duration % 60
                Text(
                    text = "${minutes}:${seconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            videoInfo.author?.let { author ->
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(20.dp))
    
    HorizontalDivider()
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Format selector
    Text(
        text = "Select Format",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    
    FormatSelector(
        videoInfo = videoInfo,
        selectedFormat = selectedFormat,
        onFormatSelected = onFormatSelected,
        modifier = Modifier.heightIn(max = 300.dp)
    )
}
