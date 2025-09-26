package com.hazardhawk.ui.gallery

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.ImageLoader
import coil3.size.Size
import android.util.Log
import com.hazardhawk.domain.entities.Photo
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.ai.GeminiVisionAnalyzer
import com.hazardhawk.ai.PhotoAnalysisWithTags
import com.hazardhawk.ai.yolo.ConstructionHazardDetection
import com.hazardhawk.tags.MobileTagManager
import com.hazardhawk.data.repositories.OSHARegulationRepository
import com.hazardhawk.models.OSHAAnalysisResult
import org.koin.compose.koinInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import com.hazardhawk.camera.MetadataEmbedder
import com.hazardhawk.camera.CaptureMetadata
import com.hazardhawk.camera.LocationData
import com.hazardhawk.camera.MetadataSettingsManager
import android.location.Geocoder
import java.util.Locale
import com.hazardhawk.performance.ConstructionPerformanceMonitor
import com.hazardhawk.performance.PhotoViewerPerformanceTracker
import com.hazardhawk.performance.ConstructionPhotoMemoryManager
import com.hazardhawk.performance.PerformanceTracker
import com.hazardhawk.performance.TouchPerformanceWrapper

/**
 * Construction-Grade Photo Viewer
 * Optimized for outdoor visibility and glove-friendly operation
 *
 * Features:
 * - Dynamic metadata extraction from EXIF data using MetadataEmbedder
 * - GPS coordinate to human-readable address conversion
 * - Real project information from embedded metadata or database
 * - Enhanced metadata display with device info, file size, and dimensions
 * - Secure metadata handling with encryption support
 */

// Construction-safe colors optimized for outdoor visibility
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)
private val ConstructionBlack = Color(0xFF1A1A1A)

/**
 * Stable state classes for optimized Compose recomposition
 */
@Stable
data class ConstructionPhotoViewerState(
    val photo: Photo,
    val metadata: CaptureMetadata?,
    val aiAnalysis: PhotoAnalysisWithTags?,
    val oshaAnalysis: OSHAAnalysisResult?,
    val locationAddress: String?,
    val isLoadingMetadata: Boolean,
    val isAnalyzing: Boolean,
    val analysisError: String?
) {
    // Stable computed properties for performance
    val hasAnalysis: Boolean get() = aiAnalysis != null
    val hasOSHAAnalysis: Boolean get() = oshaAnalysis != null
    val isProcessing: Boolean get() = isLoadingMetadata || isAnalyzing
}

@Stable
data class PhotoNavigationState(
    val currentIndex: Int,
    val totalPhotos: Int,
    val canNavigatePrevious: Boolean,
    val canNavigateNext: Boolean
) {
    // Stable computed properties
    val progressText: String get() = "${currentIndex + 1} of $totalPhotos"
    val isFirstPhoto: Boolean get() = currentIndex == 0
    val isLastPhoto: Boolean get() = currentIndex == totalPhotos - 1
}

/**
 * Debounced state manager for efficient updates
 */
class DebouncedStateManager<T>(
    private val debounceTimeMs: Long = 300L
) {
    private var lastUpdateTime = 0L
    private val pendingUpdates = mutableMapOf<String, T>()

    fun updateWithDebounce(key: String, value: T, onUpdate: (T) -> Unit) {
        val currentTime = System.currentTimeMillis()
        pendingUpdates[key] = value

        if (currentTime - lastUpdateTime > debounceTimeMs) {
            lastUpdateTime = currentTime
            pendingUpdates[key]?.let { onUpdate(it) }
            pendingUpdates.remove(key)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreen(
    photos: List<Photo>,
    initialPhotoIndex: Int,
    onNavigateBack: () -> Unit,
    onShare: (Photo) -> Unit,
    onDelete: (Photo) -> Unit,
    onEditTags: (Photo) -> Unit,
    onTagsUpdated: (String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Performance monitoring integration
    val performanceTracker: PhotoViewerPerformanceTracker = koinInject()
    val memoryManager: ConstructionPhotoMemoryManager = koinInject()
    val imageLoader: ImageLoader = koinInject()

    var currentIndex by remember { mutableIntStateOf(initialPhotoIndex.coerceIn(0, photos.size - 1)) }
    val hapticFeedback = LocalHapticFeedback.current

    // UI visibility state for immersive photo viewing
    var isUiVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-fade UI after 3 seconds of inactivity
    LaunchedEffect(lastInteractionTime, isUiVisible) {
        if (isUiVisible) {
            delay(3000) // 3 second delay
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInteractionTime >= 3000) {
                isUiVisible = false
            }
        }
    }

    // Track PhotoViewer launch performance
    val launchTracker = remember { performanceTracker.trackPhotoLoad("initial_load") }
    LaunchedEffect(Unit) {
        delay(100) // Allow initial composition to complete
        launchTracker.complete()
    }

    // Monitor memory usage periodically for construction site operation
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // Check every 5 seconds
            val memoryStats = memoryManager.getMemoryStats()
            if (memoryStats.memoryPressure) {
                Log.w("PhotoViewer", "Memory pressure detected: ${memoryStats.usedMemoryMB}MB")
                memoryManager.cleanupMemoryOnBackground()
            }
        }
    }

    val currentPhoto = photos.getOrNull(currentIndex)

    // Stable navigation state for optimized recomposition
    val navigationState = remember(currentIndex, photos.size) {
        PhotoNavigationState(
            currentIndex = currentIndex,
            totalPhotos = photos.size,
            canNavigatePrevious = currentIndex > 0,
            canNavigateNext = currentIndex < photos.size - 1
        )
    }

    if (currentPhoto == null) {
        // Fallback for invalid photo index
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = DangerRed
                )
                Text(
                    text = "Photo not found",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ConstructionBlack
                )
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
                ) {
                    Text("Go Back")
                }
            }
        }
        return
    }

    // Wrap entire PhotoViewer with performance monitoring
    TouchPerformanceWrapper(enabled = true) {
        val bottomSheetState = rememberStandardBottomSheetState()
        val scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = bottomSheetState
        )

        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetContent = {
                // Photo info section as bottom sheet content with fixed height
                PhotoInfoSection(
                    photo = currentPhoto,
                    navigationState = navigationState,
                    onTagsUpdated = onTagsUpdated,
                    performanceTracker = performanceTracker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp) // Fixed height for consistent expanded position
                )
            },
            sheetPeekHeight = 120.dp,
            modifier = modifier
        ) {
            // Full-screen photo viewer with controls
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ConstructionBlack)
            ) {
            // Photo viewer with tap-to-toggle UI
            ConstructionPhotoViewer(
                photo = currentPhoto,
                onPhotoTap = {
                    isUiVisible = !isUiVisible
                    lastInteractionTime = System.currentTimeMillis()
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top controls bar with animated visibility
            TopControlsBar(
                navigationState = navigationState,
                currentPhoto = currentPhoto,
                isVisible = isUiVisible,
                onBack = onNavigateBack,
                onShare = {
                    onShare(currentPhoto)
                    lastInteractionTime = System.currentTimeMillis()
                },
                onDelete = {
                    onDelete(currentPhoto)
                    lastInteractionTime = System.currentTimeMillis()
                },
                onPrevious = {
                    if (navigationState.canNavigatePrevious) {
                        val navTracker = performanceTracker.trackTabSwitch("photo_${currentIndex}", "photo_${currentIndex - 1}")
                        currentIndex--
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        lastInteractionTime = System.currentTimeMillis()
                        navTracker.complete()
                    }
                },
                onNext = {
                    if (navigationState.canNavigateNext) {
                        val navTracker = performanceTracker.trackTabSwitch("photo_${currentIndex}", "photo_${currentIndex + 1}")
                        currentIndex++
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        lastInteractionTime = System.currentTimeMillis()
                        navTracker.complete()
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            }
        }
    }
}

@Composable
private fun ConstructionPhotoViewer(
    photo: Photo,
    onPhotoTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader: ImageLoader = koinInject()
    val memoryManager: ConstructionPhotoMemoryManager = koinInject()

    // Performance-optimized image loading for construction photography with tap support
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(photo.filePath)
            .size(Size.ORIGINAL) // Load full resolution for construction documentation
            .memoryCacheKey("construction_photo_${photo.id}") // Stable cache key
            .diskCacheKey("construction_photo_${photo.id}")
            .build(),
        imageLoader = imageLoader,
        contentDescription = "Construction Photo: ${photo.fileName}",
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onPhotoTap()
            },
        contentScale = ContentScale.Fit,
        onSuccess = {
            Log.d("ConstructionPhotoViewer", "Successfully loaded photo: ${photo.fileName}")
        },
        onError = { error ->
            Log.e("ConstructionPhotoViewer", "Failed to load photo: ${photo.fileName}, error: $error")
        }
    )

    // Track memory usage after image load
    LaunchedEffect(photo.id) {
        delay(500) // Allow image to load
        val memoryStats = memoryManager.getMemoryStats()
        Log.d("ConstructionPhotoViewer", "Memory usage after loading ${photo.fileName}: ${memoryStats.usedMemoryMB}MB")
    }
}

@Composable
private fun TopControlsBar(
    navigationState: PhotoNavigationState,
    currentPhoto: Photo,
    isVisible: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Clean floating controls without background clutter with visibility animation
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (isVisible) 1f else 0f
            }
    ) {
        // Status bar spacing
        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button - left aligned
            FloatingIconButton(
                onClick = onBack,
                backgroundColor = SafetyOrange,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to gallery",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Navigation controls - center
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                FloatingIconButton(
                    onClick = onPrevious,
                    enabled = navigationState.canNavigatePrevious,
                    backgroundColor = if (navigationState.canNavigatePrevious) SafetyOrange else Color.Gray.copy(alpha = 0.3f),
                    contentColor = Color.White,
                    size = 48.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateBefore,
                        contentDescription = "Previous photo",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next button
                FloatingIconButton(
                    onClick = onNext,
                    enabled = navigationState.canNavigateNext,
                    backgroundColor = if (navigationState.canNavigateNext) SafetyOrange else Color.Gray.copy(alpha = 0.3f),
                    contentColor = Color.White,
                    size = 48.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.NavigateNext,
                        contentDescription = "Next photo",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Action buttons - right aligned
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FloatingIconButton(
                    onClick = {
                        // PRIVACY SECURITY: Show sharing consent dialog before sharing
                        // In production, this would integrate with PhotoSharingSecurityManager
                        onShare()
                    },
                    backgroundColor = SafetyGreen,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share photo",
                        modifier = Modifier.size(20.dp)
                    )
                }

                FloatingIconButton(
                    onClick = onDelete,
                    backgroundColor = DangerRed,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete photo",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoInfoSection(
    photo: Photo,
    navigationState: PhotoNavigationState,
    onTagsUpdated: (String, List<String>) -> Unit,
    performanceTracker: PhotoViewerPerformanceTracker,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Info", "Tags", "AI Analysis", "OSHA")

    // Stable tab switching with performance tracking
    val tabSwitchHandler = remember {
        { newTab: Int ->
            val tabTracker = performanceTracker.trackTabSwitch(tabs[selectedTab], tabs[newTab])
            selectedTab = newTab
            tabTracker.complete()
        }
    }

    // Lift AI analysis state to this level so it persists across tab navigation
    var aiAnalysisResult by remember(photo.id) { mutableStateOf<PhotoAnalysisWithTags?>(null) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column {
            // Tab bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                contentColor = SafetyOrange,
                edgePadding = 0.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { tabSwitchHandler(index) },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) SafetyOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Tab content with fixed height to prevent bottom sheet movement
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp) // Fixed content height for consistent positioning
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> PhotoMetadataPanel(
                        photo = photo,
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> PhotoTagsPanel(
                        photo = photo,
                        onTagsUpdated = onTagsUpdated,
                        modifier = Modifier.fillMaxSize()
                    )
                    2 -> AIAnalysisPanel(
                        photo = photo,
                        analysisResult = aiAnalysisResult,
                        onAnalysisResult = { result -> aiAnalysisResult = result },
                        onTagsUpdated = onTagsUpdated,
                        performanceTracker = performanceTracker,
                        modifier = Modifier.fillMaxSize()
                    )
                    3 -> OSHACodesPanel(
                        photo = photo,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoMetadataPanel(
    photo: Photo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var extractedMetadata by remember(photo.id) { mutableStateOf<CaptureMetadata?>(null) }
    var locationAddress by remember(photo.id) { mutableStateOf<String?>(null) }
    var isLoadingMetadata by remember(photo.id) { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Get settings for GPS display preference
    val metadataSettingsManager = remember { MetadataSettingsManager(context) }
    val appSettings by metadataSettingsManager.appSettings.collectAsStateWithLifecycle()
    val showGPSCoordinates by remember(appSettings) {
        mutableStateOf(appSettings.dataPrivacy.showGPSCoordinates)
    }

    // Extract metadata when component loads
    LaunchedEffect(photo.id) {
        isLoadingMetadata = true
        try {
            val metadataEmbedder = MetadataEmbedder(context)
            val photoFile = File(photo.filePath)
            if (photoFile.exists()) {
                extractedMetadata = metadataEmbedder.extractMetadataFromPhoto(photoFile)

                // Convert GPS coordinates to human-readable address if available
                extractedMetadata?.locationData?.let { locationData ->
                    if (locationData.isAvailable) {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(
                                locationData.latitude,
                                locationData.longitude,
                                1
                            )
                            locationAddress = addresses?.firstOrNull()?.let { address ->
                                buildString {
                                    address.thoroughfare?.let { append("$it, ") }
                                    address.locality?.let { append("$it, ") }
                                    address.adminArea?.let { append(it) }
                                }.takeIf { it.isNotBlank() } ?: "${String.format("%.6f", locationData.latitude)}, ${String.format("%.6f", locationData.longitude)}"
                            }
                        } catch (e: Exception) {
                            // Fallback to coordinates if geocoding fails
                            locationAddress = "${String.format("%.6f", locationData.latitude)}, ${String.format("%.6f", locationData.longitude)}"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Handle extraction errors gracefully
            extractedMetadata = null
        } finally {
            isLoadingMetadata = false
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Photo Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    MetadataRow(label = "Captured", value = formatTimestamp(photo.timestamp))
                    MetadataRow(label = "Photo ID", value = photo.id)
                    MetadataRow(label = "File Name", value = photo.fileName)

                    // Dynamic project info - prioritize EXIF metadata, then photo.projectId lookup, then current project
                    val projectName = when {
                        // First, try project name from EXIF metadata
                        extractedMetadata?.projectName?.isNotBlank() == true ->
                            extractedMetadata!!.projectName

                        // Second, try to look up project name using projectId from Photo entity
                        photo.projectId?.isNotBlank() == true -> {
                            try {
                                val metadataSettings = MetadataSettingsManager(context)
                                val projectsList = metadataSettings.projectsList.value
                                projectsList.find { it.projectId == photo.projectId }?.projectName
                                    ?.takeIf { it.isNotBlank() }
                                    ?: photo.projectId!! // Fall back to showing projectId if name not found
                            } catch (e: Exception) {
                                photo.projectId!! // Fall back to showing projectId if lookup fails
                            }
                        }

                        // Third, try to use current project from MetadataSettingsManager if no project info found
                        else -> {
                            try {
                                val metadataSettings = MetadataSettingsManager(context)
                                val currentProject = metadataSettings.currentProject.value
                                if (currentProject.projectName.isNotBlank()) {
                                    currentProject.projectName
                                } else {
                                    "No project assigned"
                                }
                            } catch (e: Exception) {
                                "No project assigned"
                            }
                        }
                    }
                    MetadataRow(label = "Project", value = projectName)

                    // Dynamic location from GPS coordinates or address based on user preference
                    val locationValue = when {
                        isLoadingMetadata -> "Loading location..."
                        showGPSCoordinates -> {
                            // User prefers coordinates - show raw GPS data
                            when {
                                photo.location != null -> "${String.format("%.6f", photo.location!!.latitude)}, ${String.format("%.6f", photo.location!!.longitude)}"
                                extractedMetadata?.locationData?.isAvailable == true -> {
                                    val loc = extractedMetadata!!.locationData
                                    "${String.format("%.6f", loc.latitude)}, ${String.format("%.6f", loc.longitude)}"
                                }
                                else -> "Location unavailable"
                            }
                        }
                        else -> {
                            // User prefers address - show geocoded address or fallback to coordinates
                            when {
                                locationAddress != null -> locationAddress!!
                                photo.location != null -> "${String.format("%.6f", photo.location!!.latitude)}, ${String.format("%.6f", photo.location!!.longitude)}"
                                extractedMetadata?.locationData?.isAvailable == true -> {
                                    val loc = extractedMetadata!!.locationData
                                    "${String.format("%.6f", loc.latitude)}, ${String.format("%.6f", loc.longitude)}"
                                }
                                else -> "Location unavailable"
                            }
                        }
                    }
                    MetadataRow(label = "Location", value = locationValue)

                    // Additional metadata if available
                    extractedMetadata?.let { metadata ->
                        if (metadata.userName.isNotBlank()) {
                            MetadataRow(label = "Captured by", value = metadata.userName)
                        }
                        if (metadata.deviceInfo.isNotBlank()) {
                            MetadataRow(label = "Device", value = metadata.deviceInfo)
                        }
                    }

                    // Photo technical details
                    photo.fileSize.let { size ->
                        if (size > 0) {
                            val sizeInMB = size / (1024.0 * 1024.0)
                            MetadataRow(label = "File Size", value = String.format("%.2f MB", sizeInMB))
                        }
                    }

                    photo.width?.let { width ->
                        photo.height?.let { height ->
                            MetadataRow(label = "Dimensions", value = "${width} × ${height}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTagsPanel(
    photo: Photo,
    onTagsUpdated: (String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTagManager by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Current tags
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Safety Tags (${photo.tags.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showTagManager = true },
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add")
                            }

                            if (photo.tags.isNotEmpty()) {
                                Button(
                                    onClick = { showTagManager = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit")
                                }
                            }
                        }
                    }

                    // Tags display with performance optimization
                    if (photo.tags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = photo.tags,
                                key = { tag -> "photo_tag_${photo.id}_$tag" } // Stable key for performance
                            ) { tag ->
                                TagChip(
                                    text = tag,
                                    onRemove = {
                                        val updatedTags = photo.tags.filter { it != tag }
                                        onTagsUpdated(photo.id, updatedTags)
                                    }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "No tags assigned. Tap 'Add' to start tagging this photo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Tag management dialog
    if (showTagManager) {
        MobileTagManager(
            photoId = photo.id,
            existingTags = photo.tags.toSet(),
            onTagsUpdated = { tagSet ->
                onTagsUpdated(photo.id, tagSet.toList())
                showTagManager = false
            },
            onDismiss = { showTagManager = false }
        )
    }
}

@Composable
private fun AIAnalysisPanel(
    photo: Photo,
    analysisResult: PhotoAnalysisWithTags?,
    onAnalysisResult: (PhotoAnalysisWithTags?) -> Unit,
    onTagsUpdated: (String, List<String>) -> Unit,
    performanceTracker: PhotoViewerPerformanceTracker,
    modifier: Modifier = Modifier
) {
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Get AI service from dependency injection
    val aiService: GeminiVisionAnalyzer = koinInject()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = SafetyOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Gemini AI Safety Analysis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Analysis status
                    when {
                        isAnalyzing -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = SafetyOrange
                                )
                                Text(
                                    text = "🤖 Analyzing photo with Gemini Vision...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        analysisError != null -> {
                            Text(
                                text = "Analysis failed: $analysisError",
                                style = MaterialTheme.typography.bodyLarge,
                                color = DangerRed
                            )
                        }

                        analysisResult == null -> {
                            Text(
                                text = "📸 Ready to analyze photo for safety hazards",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        else -> {
                            Text(
                                text = "✅ Analysis completed in ${analysisResult!!.processingTimeMs}ms",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SafetyGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Analyze button with performance tracking
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val analysisTracker = performanceTracker.trackAIAnalysis(photo.id)
                                isAnalyzing = true
                                analysisError = null

                                try {
                                    // Initialize AI service if needed
                                    aiService.initialize()

                                    // Load photo as byte array
                                    val photoFile = File(photo.filePath)
                                    if (photoFile.exists()) {
                                        val photoBytes = withContext(Dispatchers.IO) {
                                            photoFile.readBytes()
                                        }
                                        Log.d("PhotoViewer", "Starting AI analysis for photo: ${photo.fileName}, size: ${photoBytes.size} bytes")
                                        val result = aiService.analyzePhotoWithTags(
                                            data = photoBytes,
                                            width = 1920, // Default values, could be extracted from metadata
                                            height = 1080,
                                            workType = WorkType.GENERAL_CONSTRUCTION
                                        )
                                        Log.i("PhotoViewer", "AI analysis completed for: ${photo.fileName}")
                                        onAnalysisResult(result)
                                        analysisTracker.complete()
                                    } else {
                                        analysisError = "Photo file not found"
                                    }
                                } catch (e: Exception) {
                                    analysisError = e.message ?: "Unknown error occurred"
                                    Log.e("PhotoViewer", "AI analysis failed for: ${photo.fileName}", e)
                                } finally {
                                    isAnalyzing = false
                                }
                            }
                        },
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isAnalyzing) "Analyzing..." else "Analyze with Gemini Vision")
                    }

                    // Display analysis results
                    analysisResult?.let { result ->
                        Divider()

                        Text(
                            text = "Analysis Results:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Show recommended tags with optimized rendering
                        if (result.recommendedTags.isNotEmpty()) {
                            Text(
                                text = "Recommended Safety Tags:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tap a tag to add it to this photo",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Stable keys for performance optimization
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(
                                    items = result.recommendedTags,
                                    key = { tag -> "recommended_tag_$tag" } // Stable key for performance
                                ) { tag ->
                                    RecommendedTagChip(
                                        tag = tag,
                                        isAlreadyAdded = photo.tags.contains(tag),
                                        onTagClick = {
                                            if (!photo.tags.contains(tag)) {
                                                val updatedTags = photo.tags + tag
                                                onTagsUpdated(photo.id, updatedTags)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Show hazard detections
                        if (result.hazardDetections.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Detected Hazards (${result.hazardDetections.size}):",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )

                            result.hazardDetections.forEach { hazard ->
                                HazardDetectionItem(hazard = hazard)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OSHACodesPanel(
    photo: Photo,
    modifier: Modifier = Modifier
) {
    var oshaAnalysis by remember(photo.id) { mutableStateOf<OSHAAnalysisResult?>(null) }
    var isLoadingOSHA by remember { mutableStateOf(false) }
    val oshaRepository: OSHARegulationRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = DangerRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "OSHA Standards",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isLoadingOSHA = true
                                    try {
                                        // Analyze with Simple OSHA Analyzer based on photo tags
                                        val workType = when {
                                            photo.tags.any { it.contains("electrical", ignoreCase = true) } -> WorkType.ELECTRICAL
                                            photo.tags.any { it.contains("steel", ignoreCase = true) || it.contains("metal", ignoreCase = true) } -> WorkType.STEEL_WORK
                                            else -> WorkType.GENERAL_CONSTRUCTION
                                        }

                                        val simpleAnalyzer = com.hazardhawk.ai.impl.SimpleOSHAAnalyzer()
                                        simpleAnalyzer.configure()
                                        val analysisResult = simpleAnalyzer.analyzeForOSHACompliance(ByteArray(0), workType)
                                        oshaAnalysis = analysisResult.getOrNull()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isLoadingOSHA = false
                                    }
                                }
                            },
                            enabled = !isLoadingOSHA,
                            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isLoadingOSHA) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Analyze", fontSize = 12.sp)
                        }
                    }

                    oshaAnalysis?.let { analysis ->
                        // Display OSHA violations found
                        analysis.oshaViolations.forEach { violation ->
                            OSHACodeItem(
                                code = violation.oshaStandard.substringAfter("CFR ").substringBefore("("),
                                title = violation.standardTitle,
                                description = violation.description,
                                violationType = violation.violationType.name,
                                penalty = violation.potentialPenalty
                            )
                        }

                        if (analysis.oshaViolations.isEmpty()) {
                            Text(
                                text = "✅ No OSHA violations detected for this photo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SafetyGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } ?: run {
                        if (!isLoadingOSHA) {
                            // Default OSHA codes when no analysis is available
                            OSHACodeItem(
                                code = "1926.95",
                                title = "Personal Protective Equipment",
                                description = "Requirements for hard hats and protective equipment on construction sites"
                            )

                            OSHACodeItem(
                                code = "1926.951",
                                title = "High-Visibility Safety Apparel",
                                description = "Workers shall wear high-visibility safety apparel when working in areas with vehicular traffic"
                            )

                            Text(
                                text = "Tap 'Analyze' to get relevant OSHA standards for this photo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}

// Construction-optimized UI components
@Composable
private fun ConstructionIconButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Surface(
        onClick = {
            if (enabled) {
                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        modifier = Modifier.size(size),
        enabled = enabled,
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = if (enabled) 8.dp else 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ConstructionButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Button(
        onClick = {
            if (enabled) {
                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp
        ),
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

// Helper Components
@Composable
private fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(2f)
        )
    }
}

@Composable
private fun TagChip(
    text: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = SafetyGreen.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, SafetyGreen)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = SafetyGreen
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier.size(12.dp),
                    tint = SafetyGreen
                )
            }
        }
    }
}

@Composable
private fun HazardDetectionItem(
    hazard: ConstructionHazardDetection,
    modifier: Modifier = Modifier
) {
    val severityColor = when (hazard.severity.name) {
        "CRITICAL" -> Color(0xFFB71C1C)
        "HIGH" -> DangerRed
        "MEDIUM" -> SafetyOrange
        else -> SafetyGreen
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = severityColor.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.dp, severityColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = hazard.severity.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
                Text(
                    text = "${(hazard.boundingBox.confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = hazard.description?.ifEmpty { hazard.hazardType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() } }
                    ?: hazard.hazardType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            if (!hazard.oshaReference.isNullOrEmpty()) {
                Text(
                    text = "OSHA: ${hazard.oshaReference}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bounding box info (for debugging)
            Text(
                text = "Location: (${(hazard.boundingBox.x * 100).toInt()}%, ${(hazard.boundingBox.y * 100).toInt()}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OSHACodeItem(
    code: String,
    title: String,
    description: String,
    violationType: String? = null,
    penalty: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (violationType) {
                "SERIOUS" -> DangerRed.copy(alpha = 0.1f)
                "OTHER_THAN_SERIOUS" -> SafetyOrange.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            when (violationType) {
                "SERIOUS" -> DangerRed
                "OTHER_THAN_SERIOUS" -> SafetyOrange
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OSHA $code",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )

                violationType?.let {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (it) {
                            "SERIOUS" -> DangerRed
                            "OTHER_THAN_SERIOUS" -> SafetyOrange
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Text(
                            text = it.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            penalty?.let {
                Text(
                    text = "💰 Potential Penalty: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = DangerRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Optimized component for recommended tag chips
 * Reduces recomposition by using stable state
 */
@Composable
private fun RecommendedTagChip(
    tag: String,
    isAlreadyAdded: Boolean,
    onTagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isAlreadyAdded) SafetyGreen.copy(alpha = 0.2f) else SafetyOrange.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, if (isAlreadyAdded) SafetyGreen else SafetyOrange),
        modifier = modifier
            .padding(vertical = 4.dp)
            .clickable { if (!isAlreadyAdded) onTagClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAlreadyAdded) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Already added",
                    tint = SafetyGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = if (isAlreadyAdded) SafetyGreen else SafetyOrange
            )
        }
    }
}

// Utility functions
private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}

/**
 * Floating icon button with subtle shadow for visibility over photos
 */
@Composable
private fun FloatingIconButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean = true,
    size: androidx.compose.ui.unit.Dp = 56.dp,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    Surface(
        onClick = {
            if (enabled) {
                hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                // Add subtle shadow for visibility over bright photos
                shadowElevation = 12.0f
                shape = CircleShape
                clip = true
            },
        enabled = enabled,
        shape = CircleShape,
        color = backgroundColor.copy(alpha = if (enabled) 0.9f else 0.5f),
        shadowElevation = if (enabled) 12.dp else 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                content()
            }
        }
    }
}
