package com.hazardhawk.ui.camera.hud

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.*
import android.content.res.Configuration
import android.view.Surface
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.ui.camera.CameraSettingsDialog
import com.hazardhawk.camera.UnifiedViewfinderCalculator
import com.hazardhawk.camera.UnifiedCameraOverlay
import com.hazardhawk.camera.MetadataOverlayInfo
import com.hazardhawk.camera.MetadataSettingsManager
import com.hazardhawk.camera.MetadataEmbedder
import com.hazardhawk.camera.CaptureMetadata
import com.hazardhawk.camera.LocationData
import com.hazardhawk.camera.ProjectInfo
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.domain.entities.WorkType
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

/**
 * Enhanced Safety HUD Camera Screen
 * Includes viewport positioning fixes, horizontal zoom slider, and aspect ratio controls
 */
@Composable
fun SafetyHUDCameraScreen(
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAR: () -> Unit = {},
    onSetVolumeCaptureCallback: ((callback: (() -> Unit)?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    // Project manager for dropdown synchronization
    val projectManager = remember { com.hazardhawk.data.ProjectManager(context) }

    // Metadata settings manager for project selection
    val metadataSettingsManager = remember { MetadataSettingsManager(context, projectManager) }
    val userProfile by metadataSettingsManager.userProfile.collectAsStateWithLifecycle()
    val currentProject by metadataSettingsManager.currentProject.collectAsStateWithLifecycle()
    val projectsList by metadataSettingsManager.projectsList.collectAsStateWithLifecycle()
    val appSettings by metadataSettingsManager.appSettings.collectAsStateWithLifecycle()

    // AI Photo Analyzer for processing captured photos
    val aiPhotoAnalyzer: AIPhotoAnalyzer = koinInject()

    // Camera controller
    val cameraController = remember { LifecycleCameraController(context) }
    
    // Permission states
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Location state
    var currentLocation by remember { mutableStateOf<LocationData?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Device orientation helper
    val getDeviceOrientation = {
        when (context.resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> 0
            Configuration.ORIENTATION_LANDSCAPE -> {
                // Determine if it's 90 or 270 degrees based on display rotation
                val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
                when (windowManager.defaultDisplay.rotation) {
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_270 -> 270
                    else -> 0
                }
            }
            else -> 0
        }
    }
    
    // UI state
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showProjectDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<com.hazardhawk.camera.ProjectInfo?>(null) }
    var isCapturingPhoto by remember { mutableStateOf(false) }
    var flashEnabled by remember { mutableStateOf(false) }

    // AI Analysis state
    var isAnalyzingPhoto by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableStateOf("") }
    var lastAnalysisResult by remember { mutableStateOf<String?>(null) }

    // Volume button capture function
    var capturePhotoFunction: (() -> Unit)? by remember { mutableStateOf(null) }

    // Enhanced capture guard mechanism - prevents duplicate photos
    var captureStartTime by remember { mutableStateOf(0L) }
    val captureTimeoutMs = 5000L // 5 second timeout for capture operations
    
    // Camera controls state - load from persistent settings
    val aspectRatioState by metadataSettingsManager.aspectRatioState.collectAsStateWithLifecycle()
    var currentAspectRatio by remember { mutableStateOf(
        UnifiedViewfinderCalculator.ViewfinderAspectRatio.values().find { it.label == aspectRatioState }
        ?: UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE
    ) }

    // Update current aspect ratio when settings change
    LaunchedEffect(aspectRatioState) {
        val settingsRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.values().find { it.label == aspectRatioState }
        if (settingsRatio != null && settingsRatio != currentAspectRatio) {
            currentAspectRatio = settingsRatio
        }
    }

    // CameraX automatically handles orientation - no manual rotation needed
    var currentZoom by remember { mutableStateOf(1.0f) }
    val minZoom = 1.0f
    var maxZoom by remember { mutableStateOf(10.0f) }

    // Get actual camera zoom capabilities
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            try {
                val cameraInfo = cameraController.cameraInfo
                if (cameraInfo != null) {
                    val zoomState = cameraInfo.zoomState.value
                    if (zoomState != null) {
                        maxZoom = zoomState.maxZoomRatio
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SafetyHUD", "Could not get camera zoom capabilities", e)
            }
        }
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Function to get current location
    val getCurrentLocation = {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            currentLocation = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                altitude = location.altitude,
                                timestamp = System.currentTimeMillis(),
                                isAvailable = true,
                                address = "GPS: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                            )
                            android.util.Log.d("SafetyHUD", "Location obtained: ${location.latitude}, ${location.longitude}")
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.w("SafetyHUD", "Failed to get location", e)
                        currentLocation = LocationData()
                    }
            } catch (e: SecurityException) {
                android.util.Log.w("SafetyHUD", "Location permission not granted", e)
                currentLocation = LocationData()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            getCurrentLocation()
        }
    }
    
    // Request permissions on launch
    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    // Bind camera to lifecycle and set up zoom with aspect ratio configuration
    LaunchedEffect(hasCameraPermission, currentAspectRatio) {
        if (hasCameraPermission) {
            // ASPECT RATIO FIX: Configure ImageCapture with specific target size instead of aspect ratio
            // CameraX LifecycleCameraController doesn't expose ImageCapture configuration directly,
            // so we use target size to enforce the desired aspect ratio

            android.util.Log.d("SafetyHUD", "ASPECT RATIO FIX: Configuring CameraX for UI aspect ratio ${currentAspectRatio.label}")

            // Calculate target resolution based on aspect ratio
            // Use common resolutions that match our aspect ratios
            val targetSize = when (currentAspectRatio) {
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE -> {
                    // For square, use a 1:1 resolution - we'll crop from 4:3
                    CameraController.OutputSize(android.util.Size(2048, 2048)) // 1:1 square
                }
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE -> {
                    CameraController.OutputSize(android.util.Size(2048, 1536)) // 4:3 standard
                }
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE -> {
                    CameraController.OutputSize(android.util.Size(1920, 1080)) // 16:9 widescreen
                }
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.THREE_TWO -> {
                    CameraController.OutputSize(android.util.Size(2048, 1365)) // 3:2 classic
                }
            }

            // Set the target size for image capture
            cameraController.imageCaptureTargetSize = targetSize
            android.util.Log.d("SafetyHUD", "ASPECT RATIO FIX: Set ImageCapture target size for aspect ratio ${currentAspectRatio.label}")

            cameraController.bindToLifecycle(lifecycleOwner)
            // Enable pinch to zoom
            cameraController.isPinchToZoomEnabled = true
            // Enable tap to focus
            cameraController.isTapToFocusEnabled = true
            // Enable image capture with proper orientation handling
            cameraController.setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE
            )

            // LifecycleCameraController handles orientation automatically
            // The captured image will have correct EXIF orientation data

            android.util.Log.d("SafetyHUD", "CameraX configured with automatic orientation handling and aspect ratio ${currentAspectRatio.label}")
        }
    }
    
    // Handle zoom changes
    LaunchedEffect(currentZoom) {
        if (hasCameraPermission) {
            cameraController.setZoomRatio(currentZoom)
        }
    }



    // Enhanced photo capture function with comprehensive guard mechanism
    val capturePhoto = {
        val currentTime = System.currentTimeMillis()

        // Comprehensive capture guard - prevent duplicates from any source
        if (!isCapturingPhoto || (currentTime - captureStartTime) > captureTimeoutMs) {
            // Reset if timeout exceeded (recover from stuck state)
            if ((currentTime - captureStartTime) > captureTimeoutMs && isCapturingPhoto) {
                android.util.Log.w("SafetyHUD", "Capture timeout exceeded, resetting capture state")
                isCapturingPhoto = false
            }

            if (!isCapturingPhoto) {
                isCapturingPhoto = true
                captureStartTime = currentTime
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                android.util.Log.d("SafetyHUD", "Photo capture initiated at timestamp: $currentTime")

                // Refresh location before capture if permission is available
                if (hasLocationPermission) {
                    getCurrentLocation()
                }

            // Create MediaStore entry for direct saving to public Pictures/HazardHawk folder
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "HH_${timestamp}_${System.nanoTime()}"

            // CameraX handles orientation automatically - keeping device rotation for logging only
            val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
            val deviceRotation = windowManager.defaultDisplay.rotation

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                // Let CameraX handle orientation automatically through EXIF data
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HazardHawk")
                }
            }

            android.util.Log.d("SafetyHUD", "Capturing with CameraX automatic orientation handling - device rotation: $deviceRotation")

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            // CameraX automatically handles orientation via EXIF - no manual rotation needed
            android.util.Log.d("SafetyHUD", "ORIENTATION FIX: CameraX handling orientation automatically via EXIF")

            cameraController.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        coroutineScope.launch {
                            try {
                                // Get the URI of the saved photo from CameraX
                                val photoUri = output.savedUri
                                if (photoUri == null) {
                                    android.util.Log.e("SafetyHUD", "Failed to get saved photo URI")
                                    isCapturingPhoto = false
                                    return@launch
                                }

                                android.util.Log.d("SafetyHUD", "ORIENTATION DEBUG: Photo saved by CameraX at URI: $photoUri")

                                android.util.Log.d("SafetyHUD", "📸 Photo saved to MediaStore: $photoUri")

                                // Convert URI to File object for metadata processing
                                val photoFile = getFileFromMediaStoreUri(context, photoUri)
                                if (photoFile == null || !photoFile.exists()) {
                                    android.util.Log.e("SafetyHUD", "Could not access saved photo file")
                                    isCapturingPhoto = false
                                    return@launch
                                }

                                // ORIENTATION DEBUG: Log original photo details before processing
                                try {
                                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                    val exif = ExifInterface(photoFile.absolutePath)
                                    val exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

                                    android.util.Log.d("SafetyHUD", "ORIENTATION DEBUG - BEFORE PROCESSING:")
                                    android.util.Log.d("SafetyHUD", "  File: ${photoFile.absolutePath}")
                                    android.util.Log.d("SafetyHUD", "  Bitmap dimensions: ${bitmap?.width}x${bitmap?.height}")
                                    android.util.Log.d("SafetyHUD", "  EXIF Orientation: $exifOrientation")
                                    android.util.Log.d("SafetyHUD", "  Device rotation at capture: $deviceRotation")

                                    bitmap?.recycle()
                                } catch (e: Exception) {
                                    android.util.Log.e("SafetyHUD", "Failed to read original photo data", e)
                                }

                                // Create metadata for the photo with aspect ratio information and actual location
                                val metadata = metadataSettingsManager.createCaptureMetadata(
                                    locationData = currentLocation ?: LocationData(
                                        latitude = 0.0,
                                        longitude = 0.0,
                                        accuracy = 0.0f,
                                        altitude = 0.0,
                                        address = "Location unavailable",
                                        timestamp = System.currentTimeMillis(),
                                        isAvailable = false
                                    ),
                                    aspectRatio = currentAspectRatio.label  // Pass the selected aspect ratio
                                )

                                // ASPECT RATIO FIX COMPLETE: Re-enable visual watermark now that aspect ratio is fixed
                                val metadataEmbedder = MetadataEmbedder(context)
                                val embedResult = metadataEmbedder.processPhotoInPlace(
                                    photoUri = photoUri,
                                    metadata = metadata,
                                    addVisualWatermark = true  // RE-ENABLED - Aspect ratio fix should resolve orientation issues
                                )

                                if (embedResult.isSuccess) {
                                    android.util.Log.d("SafetyHUD", "📸 Photo processed with metadata overlay successfully")

                                    // ORIENTATION DEBUG: Log photo details after processing
                                    try {
                                        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                        val exif = ExifInterface(photoFile.absolutePath)
                                        val exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

                                        android.util.Log.d("SafetyHUD", "ORIENTATION DEBUG - AFTER PROCESSING:")
                                        android.util.Log.d("SafetyHUD", "  Bitmap dimensions: ${bitmap?.width}x${bitmap?.height}")
                                        android.util.Log.d("SafetyHUD", "  EXIF Orientation: $exifOrientation")

                                        bitmap?.recycle()
                                    } catch (e: Exception) {
                                        android.util.Log.e("SafetyHUD", "Failed to read processed photo data", e)
                                    }
                                } else {
                                    android.util.Log.e("SafetyHUD", "Failed to embed metadata: ${embedResult.exceptionOrNull()?.message}")
                                }

                                // Start AI analysis of the captured photo
                                isAnalyzingPhoto = true
                                analysisProgress = "Starting AI analysis..."
                                android.util.Log.d("SafetyHUD", "Starting AI analysis for captured photo")

                                // Launch AI analysis in background
                                coroutineScope.launch {
                                    try {
                                        analysisProgress = "Reading photo data..."
                                        val photoBytes = context.contentResolver.openInputStream(photoUri)?.use { it.readBytes() }
                                            ?: throw IllegalStateException("Could not read photo bytes from URI")

                                        analysisProgress = "Configuring AI analyzer..."
                                        // Configure the AI analyzer (in case it needs initialization)
                                        val configResult = aiPhotoAnalyzer.configure()
                                        configResult.onFailure { exception ->
                                            android.util.Log.w("SafetyHUD", "AI analyzer configuration failed: ${exception.message}")
                                        }

                                        analysisProgress = "Analyzing for safety hazards..."
                                        // Perform AI analysis - using general construction as default work type
                                        val analysisResult = aiPhotoAnalyzer.analyzePhoto(
                                            imageData = photoBytes,
                                            workType = WorkType.GENERAL_CONSTRUCTION
                                        )

                                        analysisResult.onSuccess { analysis ->
                                            lastAnalysisResult = "Analysis complete: Found ${analysis.hazards.size} potential hazards"
                                            android.util.Log.d("SafetyHUD", "AI analysis completed successfully: ${analysis.hazards.size} hazards detected")
                                        }.onFailure { exception ->
                                            lastAnalysisResult = "Analysis failed: ${exception.message}"
                                            android.util.Log.e("SafetyHUD", "AI analysis failed", exception)
                                        }
                                    } catch (e: Exception) {
                                        lastAnalysisResult = "Analysis error: ${e.message}"
                                        android.util.Log.e("SafetyHUD", "Error during AI analysis", e)
                                    } finally {
                                        isAnalyzingPhoto = false
                                        analysisProgress = ""
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("SafetyHUD", "Error during photo processing", e)
                            } finally {
                                isCapturingPhoto = false
                                captureStartTime = 0L
                                android.util.Log.d("SafetyHUD", "Photo capture completed, state reset")
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        android.util.Log.e("SafetyHUD", "Photo capture failed", exception)
                        isCapturingPhoto = false
                        captureStartTime = 0L
                        android.util.Log.d("SafetyHUD", "Photo capture error, state reset")
                    }
                }
            )
            } else {
                android.util.Log.d("SafetyHUD", "Photo capture blocked - already in progress (elapsed: ${currentTime - captureStartTime}ms)")
                // Provide haptic feedback to indicate the capture was blocked
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        } else {
            android.util.Log.d("SafetyHUD", "Photo capture blocked - timeout not exceeded (elapsed: ${currentTime - captureStartTime}ms)")
            // Provide haptic feedback to indicate the capture was blocked
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    // Set the capture photo function for volume buttons
    LaunchedEffect(Unit) {
        capturePhotoFunction = capturePhoto
    }

    // Register volume capture callback
    LaunchedEffect(capturePhotoFunction) {
        onSetVolumeCaptureCallback?.invoke(capturePhotoFunction)
    }

    // Listen for pinch-to-zoom changes to update currentZoom state
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            val cameraInfo = cameraController.cameraInfo
            if (cameraInfo != null) {
                cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                    if (zoomState != null) {
                        // Only update if the change came from pinch-to-zoom (not our buttons)
                        val newZoom = zoomState.zoomRatio
                        if (kotlin.math.abs(currentZoom - newZoom) > 0.05f) {
                            currentZoom = newZoom
                        }
                    }
                }
            }
        }
    }
    
    // State for auto-fade functionality - only affects HUD info, not essential controls
    var hudInfoVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var arModeEnabled by remember { mutableStateOf(false) }

    // Auto-fade effect for HUD info only (not essential controls)
    LaunchedEffect(lastInteractionTime) {
        while (true) {
            delay(100) // Check every 100ms
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInteractionTime > 8000L) { // 8 second timeout for info elements only
                hudInfoVisible = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                // Screen tap restores HUD info and resets timer
                lastInteractionTime = System.currentTimeMillis()
                hudInfoVisible = true
            }
    ) {
        when {
            !hasCameraPermission -> {
                PermissionRequiredScreen(
                    onRetryPermission = { 
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
            
            else -> {
                // Camera Preview
                AndroidView(
                    factory = { context ->
                        PreviewView(context).apply {
                            controller = cameraController
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                
                // Unified Camera Overlay with Aspect Ratio Integration (no border)
                UnifiedCameraOverlay(
                    aspectRatio = currentAspectRatio,
                    modifier = Modifier.fillMaxSize(),
                    configuration = com.hazardhawk.camera.OverlayConfiguration(
                        showBorder = false,  // Remove the orange border
                        showMask = true,
                        showMetadata = true,
                        showCorners = false
                    ),
                    metadata = MetadataOverlayInfo(
                        companyName = userProfile.company.ifBlank { "HazardHawk" },
                        projectName = currentProject.projectName.ifBlank { "Safety Documentation" },
                        location = currentLocation?.let { location ->
                            if (appSettings.dataPrivacy.showGPSCoordinates) {
                                "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                            } else {
                                location.address.ifBlank {
                                    "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                                }
                            }
                        } ?: "Location unavailable",
                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                )
                
                // Top Controls with AR toggle in top-left and flash in top-right
                // These controls are always visible for usability
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // AR Toggle (Top-Left)
                        FloatingActionButton(
                            onClick = {
                                onNavigateToAR()
                                lastInteractionTime = System.currentTimeMillis()
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = ConstructionColors.SafetyOrange
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewInAr,
                                contentDescription = "AR Mode",
                                tint = Color.White
                            )
                        }

                        // Flash Toggle (Top-Right)
                        FloatingActionButton(
                            onClick = {
                                flashEnabled = !flashEnabled
                                cameraController.enableTorch(flashEnabled)
                                lastInteractionTime = System.currentTimeMillis()
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = if (flashEnabled) ConstructionColors.SafetyOrange else ConstructionColors.ConcreteGray
                        ) {
                            Icon(
                                imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = Color.White
                            )
                        }
                    }

                // Compact Project Selector (Center-Top) - always visible for easy access
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp) // Inline with the top controls
                ) {
                    CompactProjectSelector(
                        currentProject = currentProject,
                        projectsList = projectsList,
                        onProjectSelected = { project ->
                            metadataSettingsManager.updateCurrentProject(project)
                            lastInteractionTime = System.currentTimeMillis()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onAddProject = {
                            editingProject = null
                            showProjectDialog = true
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onEditProject = { project ->
                            editingProject = project
                            showProjectDialog = true
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Enhanced Bottom Controls - always visible for core functionality
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    CameraControlsLayout(
                        currentZoom = currentZoom,
                        minZoom = minZoom,
                        maxZoom = maxZoom,
                        onZoomChange = { newZoom ->
                            currentZoom = newZoom.coerceIn(minZoom, maxZoom)
                            lastInteractionTime = System.currentTimeMillis()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        currentAspectRatio = currentAspectRatio,
                        onAspectRatioChange = { newRatio ->
                            currentAspectRatio = newRatio
                            // Save to persistent settings
                            metadataSettingsManager.updateAspectRatio(newRatio.label)
                            lastInteractionTime = System.currentTimeMillis()
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        isCapturingPhoto = isCapturingPhoto,
                        onCapturePhoto = {
                            lastInteractionTime = System.currentTimeMillis()
                            capturePhoto()
                        },
                        onNavigateToGallery = {
                            lastInteractionTime = System.currentTimeMillis()
                            onNavigateToGallery()
                        },
                        onNavigateToSettings = {
                            lastInteractionTime = System.currentTimeMillis()
                            showSettingsDialog = true
                        }
                    )
                }
            }
        }
        
        // Settings Dialog
        CameraSettingsDialog(
            isVisible = showSettingsDialog,
            onDismiss = { showSettingsDialog = false }
        )

        // Project Management Dialog
        if (showProjectDialog) {
            ProjectManagementDialog(
                project = editingProject,
                onDismiss = {
                    showProjectDialog = false
                    editingProject = null
                },
                onSaveProject = { project ->
                    metadataSettingsManager.addProject(project)
                    if (editingProject == null) {
                        // If this is a new project, set it as current
                        metadataSettingsManager.updateCurrentProject(project)
                    }
                    showProjectDialog = false
                    editingProject = null
                }
            )
        }
    }
}

/**
 * Permission Required Screen
 */
@Composable
private fun PermissionRequiredScreen(
    onRetryPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ConstructionColors.AsphaltBlack),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = ConstructionColors.SteelBlue
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Permission Required",
                    tint = ConstructionColors.SafetyOrange,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "HazardHawk needs camera access to document safety hazards and maintain OSHA compliance.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                
                Button(
                    onClick = onRetryPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConstructionColors.SafetyOrange
                    )
                ) {
                    Text(
                        text = "Grant Permission",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Enhanced Camera Controls Layout
 * Implements horizontal zoom slider and aspect ratio chips as described in research report
 */
@Composable
private fun CameraControlsLayout(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    currentAspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    onAspectRatioChange: (UnifiedViewfinderCalculator.ViewfinderAspectRatio) -> Unit,
    isCapturingPhoto: Boolean,
    onCapturePhoto: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Compact Zoom Buttons (1x, 2x, 5x, 10x + Max)
        CompactZoomControls(
            currentZoom = currentZoom,
            maxZoom = maxZoom,
            onZoomChange = onZoomChange,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        
        // Aspect Ratio Chips (NEW - addresses research report issue)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            items(UnifiedViewfinderCalculator.ViewfinderAspectRatio.values().filter { it.isStandard }) { aspectRatio ->
                FilterChip(
                    onClick = { onAspectRatioChange(aspectRatio) },
                    label = { Text(aspectRatio.label) },
                    selected = currentAspectRatio == aspectRatio,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ConstructionColors.SafetyOrange,
                        selectedLabelColor = Color.White,
                        containerColor = Color.Black.copy(alpha = 0.3f),
                        labelColor = Color.White
                    )
                )
            }
        }
        
        // Main Control Bar
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Button
            FloatingActionButton(
                onClick = onNavigateToGallery,
                modifier = Modifier.size(56.dp),
                containerColor = ConstructionColors.SteelBlue
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Capture Button
            FloatingActionButton(
                onClick = onCapturePhoto,
                modifier = Modifier.size(80.dp),
                containerColor = Color.White
            ) {
                if (isCapturingPhoto) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = ConstructionColors.SafetyOrange,
                        strokeWidth = 3.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(ConstructionColors.SafetyOrange, CircleShape)
                    )
                }
            }
            
            // Settings Button
            FloatingActionButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(56.dp),
                containerColor = ConstructionColors.ConcreteGray
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Compact Project Selector for Camera UI
 * Provides quick project switching without leaving camera interface
 */
@Composable
private fun CompactProjectSelector(
    currentProject: com.hazardhawk.camera.ProjectInfo,
    projectsList: List<com.hazardhawk.camera.ProjectInfo>,
    onProjectSelected: (com.hazardhawk.camera.ProjectInfo) -> Unit,
    onAddProject: () -> Unit,
    onEditProject: (com.hazardhawk.camera.ProjectInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        // Compact project display card (always visible)
        Card(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .widthIn(max = 240.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ConstructionColors.SafetyOrange.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Engineering,
                    contentDescription = null,
                    tint = ConstructionColors.SafetyOrange,
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = currentProject.projectName.ifBlank { "Select Project" },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Dropdown suggestions (only when expanded)
        if (expanded) {
            Card(
                modifier = Modifier
                    .padding(top = 48.dp)
                    .widthIn(max = 260.dp)
                    .heightIn(max = 160.dp), // Compact for camera UI
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ConstructionColors.SafetyOrange.copy(alpha = 0.3f))
            ) {
                LazyColumn(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Add Project option
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onAddProject()
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Project",
                                tint = ConstructionColors.SafetyOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add New Project",
                                color = ConstructionColors.SafetyOrange,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Divider
                    if (projectsList.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp)
                            )
                        }
                    }

                    // Existing projects
                    items(projectsList.size) { index ->
                        val project = projectsList[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (project.projectName == currentProject.projectName)
                                        ConstructionColors.SafetyOrange.copy(alpha = 0.2f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onProjectSelected(project)
                                    expanded = false
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.projectName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (project.siteAddress.isNotBlank()) {
                                    Text(
                                        text = project.siteAddress,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row {
                                if (project.projectName == currentProject.projectName) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = ConstructionColors.SafetyOrange,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                // Edit button for each project
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Project",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable {
                                            onEditProject(project)
                                            expanded = false
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Zoom Controls with discrete zoom buttons (1x, 2x, 5x, 10x + Max)
 * Provides smaller screen footprint than slider while maintaining functionality
 */
@Composable
private fun CompactZoomControls(
    currentZoom: Float,
    maxZoom: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Define standard zoom levels plus dynamic max
    val zoomLevels = buildList {
        add(1.0f)
        add(2.0f)
        add(5.0f)
        add(10.0f)
        // Add max zoom if it's greater than 10x
        if (maxZoom > 10.0f) {
            add(maxZoom)
        }
    }

    // Find closest zoom level for highlighting
    val closestZoomIndex = remember(currentZoom, zoomLevels) {
        zoomLevels.indexOfFirst { kotlin.math.abs(it - currentZoom) < 0.1f }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            zoomLevels.forEachIndexed { index, zoomLevel ->
                val isCurrentZoom = kotlin.math.abs(currentZoom - zoomLevel) < 0.1f

                FilterChip(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onZoomChange(zoomLevel)
                    },
                    label = {
                        Text(
                            text = if (zoomLevel == maxZoom && zoomLevel > 10.0f) {
                                "${zoomLevel.toInt()}x"
                            } else {
                                "${zoomLevel.toInt()}x"
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isCurrentZoom) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = isCurrentZoom,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ConstructionColors.SafetyOrange,
                        selectedLabelColor = Color.White,
                        containerColor = Color.Black.copy(alpha = 0.2f),
                        labelColor = Color.White.copy(alpha = 0.8f)
                    ),
                    border = if (isCurrentZoom) {
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = true,
                            borderColor = ConstructionColors.SafetyOrange,
                            selectedBorderColor = ConstructionColors.SafetyOrange,
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    } else {
                        FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = Color.White.copy(alpha = 0.3f),
                            selectedBorderColor = Color.White.copy(alpha = 0.3f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    },
                    modifier = Modifier.height(36.dp)
                )
            }

            // Current zoom indicator if between discrete levels
            if (closestZoomIndex == -1 && currentZoom > 1.0f) {
                Text(
                    text = "${String.format("%.1f", currentZoom)}x",
                    color = ConstructionColors.SafetyOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(
                            ConstructionColors.SafetyOrange.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Project Management Dialog for adding and editing projects
 */
@Composable
private fun ProjectManagementDialog(
    project: com.hazardhawk.camera.ProjectInfo?,
    onDismiss: () -> Unit,
    onSaveProject: (com.hazardhawk.camera.ProjectInfo) -> Unit
) {
    var projectName by remember { mutableStateOf(project?.projectName ?: "") }
    var siteAddress by remember { mutableStateOf(project?.siteAddress ?: "") }
    var projectManager by remember { mutableStateOf(project?.projectManager ?: "") }
    var contractor by remember { mutableStateOf(project?.contractor ?: "") }
    var startDate by remember { mutableStateOf(project?.startDate ?: "") }
    var expectedEndDate by remember { mutableStateOf(project?.expectedEndDate ?: "") }
    var safetyOfficer by remember { mutableStateOf(project?.safetyOfficer ?: "") }

    val isEditing = project != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Project" else "Add New Project",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name *", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ConstructionColors.SafetyOrange,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = siteAddress,
                        onValueChange = { siteAddress = it },
                        label = { Text("Site Address", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ConstructionColors.SafetyOrange,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = projectManager,
                        onValueChange = { projectManager = it },
                        label = { Text("Project Manager", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ConstructionColors.SafetyOrange,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = contractor,
                        onValueChange = { contractor = it },
                        label = { Text("Contractor", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ConstructionColors.SafetyOrange,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = safetyOfficer,
                        onValueChange = { safetyOfficer = it },
                        label = { Text("Safety Officer", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ConstructionColors.SafetyOrange,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectName.isNotBlank()) {
                        val projectId = project?.projectId ?: "${System.currentTimeMillis()}"
                        val newProject = com.hazardhawk.camera.ProjectInfo(
                            projectId = projectId,
                            projectName = projectName,
                            siteAddress = siteAddress,
                            projectManager = projectManager,
                            contractor = contractor,
                            startDate = startDate,
                            expectedEndDate = expectedEndDate,
                            safetyOfficer = safetyOfficer
                        )
                        onSaveProject(newProject)
                    }
                },
                enabled = projectName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConstructionColors.SafetyOrange
                )
            ) {
                Text(
                    text = if (isEditing) "Update" else "Add Project",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        },
        containerColor = ConstructionColors.SteelBlue
    )
}

/**
 * Helper function to get File object from MediaStore URI
 */
private fun getFileFromMediaStoreUri(context: android.content.Context, uri: Uri): File? {
    return try {
        context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DATA), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                val filePath = cursor.getString(columnIndex)
                if (filePath != null) File(filePath) else null
            } else null
        }
    } catch (e: Exception) {
        android.util.Log.w("SafetyHUD", "Could not get file from MediaStore URI: ${e.message}")
        // Fallback: create a File object with estimated path
        val fileName = context.contentResolver.query(uri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
            } else null
        }
        fileName?.let { File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "HazardHawk/$it") }
    }
}
