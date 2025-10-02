package com.hazardhawk.ui.gallery

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazardhawk.domain.repositories.PhotoRepository
import com.hazardhawk.domain.entities.Photo
import android.util.Log
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import java.io.File

// Temporary stubs for missing components
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = SafetyOrange)
    }
}

@Composable
private fun EmptyGalleryState(
    onTakePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SafetyOrange
            )
            Text(
                text = "No Photos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onTakePhoto,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SafetyOrange
                )
            ) {
                Text("Take Your First Photo")
            }
        }
    }
}


@Composable
private fun SelectionActionsBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SafetyOrange,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount of $totalCount selected",
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = if (selectedCount == totalCount) onClearSelection else onSelectAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                ) {
                    Text(if (selectedCount == totalCount) "None" else "All")
                }
                IconButton(onClick = onDeleteSelected) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGrid(
    photos: List<Photo>,
    selectedPhotos: Set<String>,
    isSelectionMode: Boolean,
    onPhotoClick: (Photo, Int) -> Unit,
    onPhotoLongPress: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(photos.size) { index ->
            val photo = photos[index]
            PhotoThumbnail(
                photo = photo,
                isSelected = selectedPhotos.contains(photo.id),
                isSelectionMode = isSelectionMode,
                onClick = { onPhotoClick(photo, index) },
                onLongPress = { onPhotoLongPress(photo) }
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(
    photo: Photo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) {
            BorderStroke(4.dp, SafetyOrange)
        } else null
    ) {
        Box {
            // Load actual thumbnail using AsyncImage
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(photo.filePath)
                    .build(),
                contentDescription = "Photo: ${photo.fileName}",
                modifier = Modifier
                    .fillMaxSize()
                    .let { mod ->
                        if (isSelected) {
                            mod.alpha(0.7f) // Dim selected photos slightly
                        } else mod
                    },
                contentScale = ContentScale.Crop
            )

            // Selection overlay for better visibility
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SafetyOrange.copy(alpha = 0.3f))
                )
            }

            // Always show selection indicator in selection mode
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(
                            if (isSelected) SafetyOrange else Color.White.copy(alpha = 0.8f),
                            CircleShape
                        )
                        .border(
                            2.dp,
                            if (isSelected) Color.White else SafetyOrange,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        // Empty circle for unselected items in selection mode
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(Color.Transparent, CircleShape)
                        )
                    }
                }
            }

            // Selection count badge for better multi-select feedback
            if (isSelected && isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(SafetyOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoViewer(
    photos: List<Photo>,
    initialPhotoIndex: Int,
    onDismiss: () -> Unit,
    onShare: (Photo) -> Unit,
    onDelete: (Photo) -> Unit,
    onTagsUpdated: (String, List<String>) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    // Use the refactored PhotoViewer for better performance and maintainability
    PhotoViewerScreenRefactored(
        photos = photos,
        initialPhotoIndex = initialPhotoIndex,
        onNavigateBack = onDismiss,
        onShare = onShare,
        onDelete = onDelete,
        onTagsUpdated = onTagsUpdated,
        modifier = modifier
    )
}

/**
 * Simplified Photo Gallery
 * Main gallery screen that replaces 8+ complex gallery files
 */

private val SafetyOrange = Color(0xFFFF6B35)

/**
 * Main Photo Gallery Screen
 * Simplified and focused gallery implementation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoGallery(
    photoRepository: PhotoRepository,
    reportGenerationManager: com.hazardhawk.reports.ReportGenerationManager,
    onNavigateToCamera: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Determine required permission based on Android version
    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, mediaPermission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val viewModel = rememberGalleryState(photoRepository, reportGenerationManager)

    // Metadata embedder for reprocessing photos
    val metadataEmbedder = remember { com.hazardhawk.camera.MetadataEmbedder(context) }
    var showReprocessDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            Log.d("PhotoGallery", "Media permission granted, refreshing photos...")
            // Trigger photo reload when permission is granted
            viewModel.loadPhotos()
        } else {
            Log.w("PhotoGallery", "Media permission denied")
        }
    }

    // Request permission when gallery opens if not granted
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            Log.d("PhotoGallery", "Requesting media permission: $mediaPermission")
            permissionLauncher.launch(mediaPermission)
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Show permission required message if permission denied
    if (!hasPermission) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Photo Access Required",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "This app needs permission to access photos to display your existing HazardHawk photos.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Log.d("PhotoGallery", "User clicked Grant Permission button")
                    permissionLauncher.launch(mediaPermission)
                }
            ) {
                Text("Grant Permission")
            }
        }
        return
    }

    // Handle error display
    LaunchedEffect(state.error) {
        if (state.error != null) {
            // Show error as toast and clear it
            android.widget.Toast.makeText(
                context,
                state.error,
                android.widget.Toast.LENGTH_LONG
            ).show()
            viewModel.clearError()
        }
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            // Use custom top bar instead of Material TopAppBar to avoid duplication
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCamera,
                modifier = Modifier.size(64.dp),
                containerColor = SafetyOrange,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Take Photo",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = {
            val undoMessage = state.undoMessage
            if (state.showUndoSnackbar && undoMessage != null) {
                ConstructionUndoSnackbar(
                    message = undoMessage,
                    onUndo = viewModel::undoDelete,
                    onDismiss = viewModel::dismissUndo
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    LoadingState()
                }
                
                state.photos.isEmpty() && !state.isLoading -> {
                    EmptyGalleryState(
                        onTakePhoto = onNavigateToCamera
                    )
                }
                
                else -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column {
                            // Custom Gallery Top Bar
                            GalleryTopBar(
                                state = state,
                                onBack = onBack,
                                onSelectAll = viewModel::selectAll,
                                onClearSelection = viewModel::clearSelection,
                                onDeleteSelected = viewModel::deleteSelectedPhotos,
                                onReprocessSelected = { showReprocessDialog = true }
                            )
                            
                            // Photo grid
                            PhotoGrid(
                                photos = state.photos,
                                selectedPhotos = state.selectedPhotos,
                                isSelectionMode = state.isSelectionMode,
                                onPhotoClick = { photo, index ->
                                    if (state.isSelectionMode) {
                                        viewModel.selectPhoto(photo.id)
                                    } else {
                                        viewModel.showPhotoViewer(index)
                                    }
                                },
                                onPhotoLongPress = { photo ->
                                    if (!state.isSelectionMode) {
                                        viewModel.selectPhoto(photo.id)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = if (state.isSelectionMode && state.selectedPhotos.isNotEmpty()) 160.dp else 80.dp) // Space for FABs
                            )
                        }
                        
                        // Generate Report FAB - positioned absolutely when photos are selected
                        if (state.isSelectionMode && state.selectedPhotos.isNotEmpty()) {
                            ExtendedFloatingActionButton(
                                onClick = { 
                                    Log.d("PhotoGallery", "Generate Report FAB clicked")
                                    viewModel.generateReport() 
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .padding(bottom = 80.dp), // Above camera FAB
                                containerColor = SafetyOrange,
                                contentColor = Color.White
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generate Report (${state.selectedPhotos.size})",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Photo Viewer
    if (state.showPhotoViewer) {
        PhotoViewer(
            photos = state.photos,
            initialPhotoIndex = state.currentPhotoIndex,
            onDismiss = viewModel::hidePhotoViewer,
            onShare = { photo ->
                // Create Android share intent for photo
                try {
                    val photoFile = File(photo.filePath)
                    if (photoFile.exists()) {
                        val photoUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            photoFile
                        )

                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(android.content.Intent.EXTRA_STREAM, photoUri)
                            putExtra(android.content.Intent.EXTRA_TEXT, "Construction Safety Photo - ${photo.fileName}")
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        val chooserIntent = android.content.Intent.createChooser(shareIntent, "Share Photo")
                        context.startActivity(chooserIntent)
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Photo file not found",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        "Failed to share photo: ${e.message}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDelete = { photo ->
                viewModel.selectPhoto(photo.id)
                viewModel.deleteSelectedPhotos()
                viewModel.hidePhotoViewer()
            },
            onTagsUpdated = { photoId, tags ->
                viewModel.updatePhotoTags(photoId, tags)
            }
        )
    }
    
    // Report generation progress dialog
    if (state.isGeneratingReport) {
        Log.d("PhotoGallery", "Showing report generation dialog - progress: ${state.reportGenerationProgress}")
        ReportGenerationProgressDialog(
            progress = state.reportGenerationProgress,
            message = state.reportGenerationMessage ?: "Generating report...",
            onDismiss = { /* Cannot dismiss during generation */ }
        )
    }

    // Batch reprocess dialog
    if (showReprocessDialog) {
        val selectedPhotos = state.photos.filter { state.selectedPhotos.contains(it.id) }
        BatchReprocessDialog(
            photos = selectedPhotos,
            onDismiss = {
                showReprocessDialog = false
                viewModel.clearSelection()
            },
            metadataEmbedder = metadataEmbedder
        )
    }
}

/**
 * Gallery Top Bar
 * Handles navigation and bulk actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalleryTopBar(
    state: GalleryState,
    onBack: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onReprocessSelected: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (state.isSelectionMode) SafetyOrange else MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back/Exit Selection Button
            Button(
                onClick = {
                    if (state.isSelectionMode) {
                        onClearSelection()
                    } else {
                        onBack()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isSelectionMode) Color.White else SafetyOrange,
                    contentColor = if (state.isSelectionMode) SafetyOrange else Color.White
                ),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = if (state.isSelectionMode) Icons.Default.Close else Icons.Default.ArrowBack,
                    contentDescription = if (state.isSelectionMode) "Exit selection" else "Back",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isSelectionMode) "Exit" else "Back",
                    fontWeight = FontWeight.Bold
                )
            }

            // Title/Selection Count
            Text(
                text = if (state.isSelectionMode) {
                    "${state.selectedPhotos.size} Selected"
                } else {
                    "Photos (${state.photos.size})"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (state.isSelectionMode) Color.White else SafetyOrange
            )

            // Action Buttons (only in selection mode)
            if (state.isSelectionMode && state.selectedPhotos.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = if (state.selectedPhotos.size == state.photos.size) onClearSelection else onSelectAll,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            if (state.selectedPhotos.size == state.photos.size) "Clear" else "All",
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(
                        onClick = onReprocessSelected
                    ) {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = "Add timestamps",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDeleteSelected
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete selected",
                            tint = Color.White
                        )
                    }
                }
            } else if (!state.isSelectionMode) {
                // Spacer to balance the layout
                Spacer(modifier = Modifier.width(120.dp))
            }
        }
    }
}

/**
 * Construction-Optimized Undo Snackbar
 * High contrast and glove-friendly for construction workers
 */
@Composable
private fun ConstructionUndoSnackbar(
    message: String,
    onUndo: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(5000) // Auto-dismiss after 5 seconds
        onDismiss()
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = Color(0xFF2D2D2D), // Dark background for high contrast
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Large, glove-friendly UNDO button
            TextButton(
                onClick = onUndo,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = SafetyOrange
                ),
                modifier = Modifier
                    .height(48.dp) // Large touch target
                    .widthIn(min = 72.dp) // Minimum width for glove compatibility
            ) {
                Text(
                    text = "UNDO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Construction-Grade Report Generation Progress Dialog
 * Shows PDF generation progress with construction-friendly visual design
 */
@Composable
private fun ReportGenerationProgressDialog(
    progress: Float,
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal during generation */ },
        modifier = modifier,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = SafetyOrange,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Generating Safety Report",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SafetyOrange
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Progress indicator
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = SafetyOrange,
                    trackColor = Color.Gray.copy(alpha = 0.3f)
                )
                
                // Progress percentage and message
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SafetyOrange
                    )
                }
                
                // Construction-friendly instructions
                Text(
                    text = "Please wait while we generate your OSHA-compliant safety report. This process includes photo optimization and compliance validation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            // No confirm button during generation
        },
        containerColor = Color.White,
        tonalElevation = 8.dp
    )
}
