package com.hazardhawk.ui.gallery

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.camera.MetadataEmbedder
import com.hazardhawk.domain.entities.Photo
import kotlinx.coroutines.launch

/**
 * Dialog for batch reprocessing photos to add timestamp watermarks
 * Useful for fixing photos taken without timestamp overlays
 */
@Composable
fun BatchReprocessDialog(
    photos: List<Photo>,
    onDismiss: () -> Unit,
    metadataEmbedder: MetadataEmbedder,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0) }
    var totalPhotos by remember { mutableStateOf(photos.size) }
    var successCount by remember { mutableStateOf(0) }
    var isComplete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = null,
                    tint = Color(0xFFFF6B35)
                )
                Text(
                    text = if (isComplete) "Reprocessing Complete" else "Add Timestamp to Photos",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isComplete) {
                    Text(
                        text = "Successfully added timestamps to $successCount of $totalPhotos photos.",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                } else if (isProcessing) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Processing photo $currentProgress of $totalPhotos...",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        LinearProgressIndicator(
                            progress = { currentProgress.toFloat() / totalPhotos.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFF6B35),
                        )
                    }
                } else {
                    Text(
                        text = "This will add timestamp watermarks to ${photos.size} photo${if (photos.size == 1) "" else "s"}.\n\n" +
                                "The timestamp will be extracted from each photo's metadata and added to the visual overlay.",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        },
        confirmButton = {
            if (isComplete) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF6B35)
                    )
                ) {
                    Text("Done")
                }
            } else {
                Button(
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            val photoFilePaths = photos.map { it.filePath }
                            val result = metadataEmbedder.batchReprocessPhotosWithTimestamp(
                                photoFilePaths = photoFilePaths,
                                onProgress = { current, total, _ ->
                                    currentProgress = current
                                    totalPhotos = total
                                }
                            )

                            result.onSuccess { count ->
                                successCount = count
                                isComplete = true
                                isProcessing = false
                            }.onFailure {
                                isProcessing = false
                                // You could add error handling here
                            }
                        }
                    },
                    enabled = !isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35)
                    )
                ) {
                    Text("Add Timestamps", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!isComplete) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isProcessing,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    )
                ) {
                    Text("Cancel")
                }
            }
        },
        containerColor = Color(0xFF2A2A2A)
    )
}
