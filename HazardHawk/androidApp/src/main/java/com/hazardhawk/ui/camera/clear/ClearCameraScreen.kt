package com.hazardhawk.ui.camera.clear

import android.Manifest
import android.content.ContentValues
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.camera.*
import com.hazardhawk.data.ProjectManager
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.ui.camera.clear.components.*
import com.hazardhawk.ui.camera.clear.theme.ClearDesignTokens
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * ClearCameraScreen - Minimalist Camera Interface
 *
 * Jony Ive-inspired design: "Less, but Better"
 * - Full-screen camera preview
 * - Auto-hiding controls
 * - Elegant interactions
 * - Focus on content, technology recedes
 */
@Composable
fun ClearCameraScreen(
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAR: () -> Unit = {},
    onSetVolumeCaptureCallback: ((callback: (() -> Unit)?) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: ClearCameraViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    // Project and settings managers
    val projectManager = remember { ProjectManager(context) }
    val metadataSettingsManager = remember { MetadataSettingsManager(context, projectManager) }

    // Collect state from managers
    val userProfile by metadataSettingsManager.userProfile.collectAsStateWithLifecycle()
    val currentProject by metadataSettingsManager.currentProject.collectAsStateWithLifecycle()
    val projectsList by metadataSettingsManager.projectsList.collectAsStateWithLifecycle()
    val appSettings by metadataSettingsManager.appSettings.collectAsStateWithLifecycle()

    // AI Photo Analyzer
    val aiPhotoAnalyzer: AIPhotoAnalyzer = koinInject()

    // Camera controller
    val cameraController = remember { LifecycleCameraController(context) }

    // Permission states
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Location state
    var currentLocation by remember { mutableStateOf<LocationData?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Collect ViewModel state
    val bottomBarVisible by viewModel.bottomBarVisible.collectAsStateWithLifecycle()
    val zoomPanelVisible by viewModel.zoomPanelVisible.collectAsStateWithLifecycle()
    val currentZoom by viewModel.currentZoom.collectAsStateWithLifecycle()
    val maxZoom by viewModel.maxZoom.collectAsStateWithLifecycle()
    val currentAspectRatio by viewModel.currentAspectRatio.collectAsStateWithLifecycle()
    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val isARMode by viewModel.isARMode.collectAsStateWithLifecycle()
    val isCapturing by viewModel.isCapturing.collectAsStateWithLifecycle()
    val aiAnalysisState by viewModel.aiAnalysisState.collectAsStateWithLifecycle()

    // UI state for dialogs
    var showProjectDialog by remember { mutableStateOf(false) }

    // Permission launchers
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            getCurrentLocation(fusedLocationClient, hasLocationPermission) { location ->
                currentLocation = location
            }
        }
    }

    // Request permissions on launch
    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Bind camera to lifecycle with aspect ratio configuration
    LaunchedEffect(hasCameraPermission, currentAspectRatio) {
        if (hasCameraPermission) {
            // Configure ImageCapture with target size for aspect ratio
            val targetSize = when (currentAspectRatio) {
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE ->
                    CameraController.OutputSize(android.util.Size(2048, 2048))
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE ->
                    CameraController.OutputSize(android.util.Size(2048, 1536))
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE ->
                    CameraController.OutputSize(android.util.Size(1920, 1080))
                UnifiedViewfinderCalculator.ViewfinderAspectRatio.THREE_TWO ->
                    CameraController.OutputSize(android.util.Size(2048, 1365))
            }

            cameraController.imageCaptureTargetSize = targetSize
            cameraController.bindToLifecycle(lifecycleOwner)
            cameraController.isPinchToZoomEnabled = true
            cameraController.isTapToFocusEnabled = true
            cameraController.setEnabledUseCases(
                CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE
            )

            // Get max zoom capability
            try {
                val cameraInfo = cameraController.cameraInfo
                val zoomState = cameraInfo?.zoomState?.value
                zoomState?.let {
                    viewModel.updateMaxZoom(it.maxZoomRatio)
                }
            } catch (e: Exception) {
                android.util.Log.w("ClearCamera", "Could not get camera zoom capabilities", e)
            }
        }
    }

    // Handle zoom changes
    LaunchedEffect(currentZoom) {
        if (hasCameraPermission) {
            cameraController.setZoomRatio(currentZoom)
        }
    }

    // Handle flash mode
    LaunchedEffect(flashMode) {
        cameraController.enableTorch(flashMode == com.hazardhawk.ui.camera.clear.components.FlashMode.ON)
    }

    // Photo capture function
    val capturePhoto = {
        if (!isCapturing) {
            viewModel.startCapture()
            viewModel.onUserInteraction()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

            // Refresh location before capture
            if (hasLocationPermission) {
                getCurrentLocation(fusedLocationClient, hasLocationPermission) { location ->
                    currentLocation = location
                }
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "HH_${timestamp}_${System.nanoTime()}"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HazardHawk")
                }
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            cameraController.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        coroutineScope.launch {
                            try {
                                val photoUri = output.savedUri ?: return@launch
                                android.util.Log.d("ClearCamera", "Photo saved: $photoUri")

                                // Create metadata
                                val metadata = metadataSettingsManager.createCaptureMetadata(
                                    locationData = currentLocation ?: LocationData(),
                                    aspectRatio = currentAspectRatio.label
                                )

                                // Embed metadata with visual watermark
                                val metadataEmbedder = MetadataEmbedder(context)
                                val embedResult = metadataEmbedder.processPhotoInPlace(
                                    photoUri = photoUri,
                                    metadata = metadata,
                                    addVisualWatermark = true
                                )

                                if (embedResult.isSuccess) {
                                    android.util.Log.d("ClearCamera", "Metadata embedded successfully")
                                }

                                // Start AI analysis
                                viewModel.startAnalysis()

                                coroutineScope.launch {
                                    try {
                                        viewModel.updateAnalysisProgress("Reading photo data...")
                                        val photoBytes = context.contentResolver.openInputStream(photoUri)?.use { it.readBytes() }
                                            ?: throw IllegalStateException("Could not read photo bytes")

                                        viewModel.updateAnalysisProgress("Configuring AI analyzer...")
                                        aiPhotoAnalyzer.configure()

                                        viewModel.updateAnalysisProgress("Analyzing for safety hazards...")
                                        val analysisResult = aiPhotoAnalyzer.analyzePhoto(
                                            imageData = photoBytes,
                                            workType = WorkType.GENERAL_CONSTRUCTION
                                        )

                                        analysisResult.onSuccess { analysis ->
                                            val isCritical = analysis.hazards.any { it.severity.name == "CRITICAL" }
                                            val message = if (analysis.hazards.isEmpty()) {
                                                "Analysis Complete: No hazards detected"
                                            } else {
                                                "Analysis Complete: ${analysis.hazards.size} Hazards. " +
                                                        analysis.hazards.firstOrNull()?.let { "Violation: ${it.type.name.replace("_", " ")}" }.orEmpty()
                                            }
                                            viewModel.completeAnalysis(message, isCritical)
                                        }.onFailure { exception ->
                                            viewModel.analysisError("Analysis failed: ${exception.message}")
                                        }
                                    } catch (e: Exception) {
                                        viewModel.analysisError("Analysis error: ${e.message}")
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("ClearCamera", "Error processing photo", e)
                            } finally {
                                viewModel.endCapture()
                            }
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        android.util.Log.e("ClearCamera", "Photo capture failed", exception)
                        viewModel.endCapture()
                    }
                }
            )
        }
    }

    // Register volume capture callback
    LaunchedEffect(Unit) {
        onSetVolumeCaptureCallback?.invoke(capturePhoto)
    }

    // Reset zoom when leaving camera screen to sync with camera controller
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetZoom()
        }
    }

    // Main UI
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClearDesignTokens.Colors.TrueBlack)
    ) {
        if (!hasCameraPermission) {
            PermissionRequiredScreen(
                onRetryPermission = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        } else {
            // Camera Preview (Full Screen)
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        controller = cameraController
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Viewfinder Aspect Ratio Frame (Subtle guide) with content inside
            Box(modifier = Modifier.fillMaxSize()) {
                // Draw the frame
                ViewfinderAspectRatioFrame(
                    aspectRatio = currentAspectRatio
                )

                // Metadata Watermark (Bottom-Left, inside viewfinder)
                MetadataWatermark(
                    companyName = userProfile.company.ifBlank { "HazardHawk" },
                    projectName = currentProject.projectName.ifBlank { "Safety Documentation" },
                    location = currentLocation?.let { location ->
                        if (appSettings.dataPrivacy.showGPSCoordinates) {
                            // Show coordinates when setting is enabled
                            "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                        } else {
                            // Show address when setting is disabled, fallback to coordinates if no address
                            location.address.ifBlank {
                                "${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                            }
                        }
                    } ?: "Location unavailable",
                    timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    showLocation = appSettings.dataPrivacy.includeLocation,
                    showBranding = true, // Always show branding
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 32.dp, bottom = 120.dp) // Inside viewfinder frame with margins
                )
            }

            // AI Analysis Banner (Top, Conditional) - DISABLED FOR NOW
            // AutoDismissAIAnalysisBanner(
            //     analysisState = aiAnalysisState,
            //     modifier = Modifier.align(Alignment.TopCenter)
            // )

            // Minimal Top Bar (Always Visible)
            MinimalTopBar(
                isARMode = isARMode,
                onARToggle = {
                    viewModel.toggleARMode()
                    onNavigateToAR()
                },
                currentProject = currentProject.projectName.ifBlank { "Select Project" },
                onProjectTap = {
                    showProjectDialog = true
                    viewModel.onUserInteraction()
                },
                flashMode = flashMode,
                onFlashToggle = { viewModel.toggleFlash() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Contextual Zoom/Aspect Ratio Panel (Right Edge)
            ContextualZoomPanel(
                visible = zoomPanelVisible,
                currentZoom = currentZoom,
                maxZoom = maxZoom,
                onZoomChange = { zoom ->
                    viewModel.updateZoom(zoom)
                    viewModel.onUserInteraction()
                },
                currentAspectRatio = currentAspectRatio,
                onAspectRatioChange = { ratio ->
                    viewModel.updateAspectRatio(ratio)
                    metadataSettingsManager.updateAspectRatio(ratio.label)
                    viewModel.onUserInteraction()
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
            )

            // Elegant Bottom Bar (Center Bottom)
            ElegantBottomBar(
                visible = bottomBarVisible,
                isCapturing = isCapturing,
                onGalleryClick = {
                    viewModel.onUserInteraction()
                    onNavigateToGallery()
                },
                onCaptureClick = {
                    capturePhoto()
                },
                onSettingsClick = {
                    viewModel.onUserInteraction()
                    onNavigateToSettings()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )

            // Invisible tap detection layer - covers center area to toggle zoom panel
            // Positioned to not interfere with top bar, bottom bar, or right panel
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = 100.dp,    // Below top bar (increased to avoid blocking MinimalTopBar)
                        bottom = 120.dp, // Above bottom bar
                        end = 100.dp     // Left of zoom panel
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // Tap center area to toggle zoom panel
                        viewModel.toggleZoomPanel()
                    }
            )
        }

        // Project Selection Dialog
        if (showProjectDialog) {
            ProjectSelectionDialog(
                currentProject = currentProject,
                projectsList = projectsList,
                onDismiss = { showProjectDialog = false },
                onProjectSelected = { project ->
                    metadataSettingsManager.updateCurrentProject(project)
                    showProjectDialog = false
                },
                onAddProject = { project ->
                    metadataSettingsManager.addProject(project)
                    metadataSettingsManager.updateCurrentProject(project)
                    showProjectDialog = false
                }
            )
        }
    }
}

/**
 * Project Selection Dialog - Minimalist, Full-Featured
 *
 * Allows:
 * - Selecting from existing projects
 * - Creating new projects (editable field)
 * - Switching projects dynamically
 *
 * Syncs with MetadataSettingsManager - same data source as SafetyHUD camera
 */
@Composable
private fun ProjectSelectionDialog(
    currentProject: com.hazardhawk.camera.ProjectInfo,
    projectsList: List<com.hazardhawk.camera.ProjectInfo>,
    onDismiss: () -> Unit,
    onProjectSelected: (com.hazardhawk.camera.ProjectInfo) -> Unit,
    onAddProject: (com.hazardhawk.camera.ProjectInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    var projectText by remember { mutableStateOf(currentProject.projectName) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Project",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Editable project name field
                OutlinedTextField(
                    value = projectText,
                    onValueChange = { projectText = it },
                    label = { Text("Project Name", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("Enter or select project", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ClearDesignTokens.Colors.SafetyOrange,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = ClearDesignTokens.Colors.SafetyOrange,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
                    ),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle project list",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                )

                // Project suggestions dropdown
                if (expanded && projectsList.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            items(projectsList.size) { index ->
                                val project = projectsList[index]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (project.projectName == currentProject.projectName)
                                                ClearDesignTokens.Colors.SafetyOrange.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            projectText = project.projectName
                                            expanded = false
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = project.projectName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (project.siteAddress.isNotBlank()) {
                                            Text(
                                                text = project.siteAddress,
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    if (project.projectName == currentProject.projectName) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = ClearDesignTokens.Colors.SafetyOrange,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (projectText.isNotBlank()) {
                        // Check if this is an existing project
                        val existingProject = projectsList.find { it.projectName == projectText }
                        if (existingProject != null) {
                            // Select existing project
                            onProjectSelected(existingProject)
                        } else {
                            // Create new project
                            val newProject = com.hazardhawk.camera.ProjectInfo(
                                projectId = "CUSTOM_${System.currentTimeMillis()}",
                                projectName = projectText,
                                siteAddress = ""
                            )
                            onAddProject(newProject)
                        }
                    }
                },
                enabled = projectText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClearDesignTokens.Colors.SafetyOrange
                )
            ) {
                Text("Select", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        },
        containerColor = ClearDesignTokens.Colors.DeepCharcoal
    )
}

/**
 * Helper function to get current location
 */
private fun getCurrentLocation(
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    hasPermission: Boolean,
    onLocationReceived: (LocationData) -> Unit
) {
    if (!hasPermission) return

    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onLocationReceived(
                        LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            altitude = location.altitude,
                            timestamp = System.currentTimeMillis(),
                            isAvailable = true,
                            address = "GPS: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                        )
                    )
                }
            }
    } catch (e: SecurityException) {
        android.util.Log.w("ClearCamera", "Location permission not granted", e)
    }
}

/**
 * Permission Required Screen (Minimalist)
 */
@Composable
private fun PermissionRequiredScreen(
    onRetryPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClearDesignTokens.Colors.TrueBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(ClearDesignTokens.Spacing.Large)
        ) {
            androidx.compose.material3.Text(
                text = "Camera Permission Required",
                color = ClearDesignTokens.Colors.TranslucentWhite90,
                fontSize = ClearDesignTokens.Typography.HeaderText
            )

            androidx.compose.material3.Button(
                onClick = onRetryPermission,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ClearDesignTokens.Colors.SafetyOrange
                )
            ) {
                androidx.compose.material3.Text("Grant Permission")
            }
        }
    }
}
