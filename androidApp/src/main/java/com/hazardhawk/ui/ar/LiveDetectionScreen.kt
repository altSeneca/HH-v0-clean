package com.hazardhawk.ui.ar

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.ai.models.WorkType
import com.hazardhawk.ui.camera.ARCameraPreview
import com.hazardhawk.ui.theme.HazardColors
import com.hazardhawk.ui.theme.ConstructionTypography
import com.hazardhawk.ui.camera.capturePhoto

/**
 * Live hazard detection screen with real-time AR overlay.
 * Combines camera preview with AI-powered safety analysis.
 */
@Composable
fun LiveDetectionScreen(
    workType: WorkType = WorkType.GENERAL_CONSTRUCTION,
    onNavigateToResults: (SafetyAnalysis) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: LiveDetectionViewModel = viewModel { 
        LiveDetectionViewModel.createForTesting() 
    }
    
    // Collect state from ViewModel
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val currentAnalysis by viewModel.currentAnalysis.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    
    // Configure work type
    LaunchedEffect(workType) {
        viewModel.setWorkType(workType)
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        
        // Camera preview layer
        ARCameraPreview(
            onImageCaptured = { imageData ->
                viewModel.analyzePhoto(imageData)
            },
            onImageCaptureReady = { imageCapture ->
                viewModel.updateImageCapture(imageCapture)
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // AR hazard detection overlay
        HazardDetectionOverlay(
            safetyAnalysis = currentAnalysis,
            onHazardClick = { hazard ->
                // Handle hazard selection - could show details or highlight
                viewModel.selectHazard(hazard)
            },
            showBoundingBoxes = true,
            showOSHABadges = true,
            animationEnabled = true,
            compactMode = false,
            modifier = Modifier.fillMaxSize()
        )
        
        // Top controls bar
        TopControlsBar(
            workType = workType,
            analysisState = analysisState,
            onWorkTypeChange = { newWorkType ->
                viewModel.setWorkType(newWorkType)
            },
            onNavigateToSettings = onNavigateToSettings,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // Bottom camera controls
        BottomCameraControls(
            isAnalyzing = isAnalyzing,
            currentAnalysis = currentAnalysis,
            onCapturePhoto = {
                // Capture and analyze photo using CameraX
                viewModel.captureAndAnalyze(context)
            },
            onToggleFlash = {
                viewModel.toggleFlash()
            },
            onToggleContinuousAnalysis = {
                viewModel.toggleContinuousAnalysis()
            },
            onNavigateToGallery = onNavigateToGallery,
            onNavigateToResults = {
                currentAnalysis?.let { analysis ->
                    onNavigateToResults(analysis)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        )
        
        // Analysis status overlay
        when (analysisState) {
            is AnalysisState.Analyzing -> {
                AnalyzingOverlay(
                    progress = analysisState.progress,
                    currentTask = analysisState.currentTask,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is AnalysisState.Error -> {
                ErrorOverlay(
                    error = analysisState.error,
                    onRetry = { viewModel.retryAnalysis() },
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> { /* Show AR overlay normally */ }
        }
        
        // Performance monitor (Debug only)
        if (BuildConfig.DEBUG) {
            PerformanceMonitor(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Top controls bar with work type selector and settings.
 */
@Composable
private fun TopControlsBar(
    workType: WorkType,
    analysisState: AnalysisState,
    onWorkTypeChange: (WorkType) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        
        // Work type selector
        WorkTypeSelector(
            currentWorkType = workType,
            onWorkTypeSelected = onWorkTypeChange,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Settings button
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier
                .background(
                    color = HazardColors.OVERLAY_BACKGROUND,
                    shape = CircleShape
                )
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White
            )
        }
    }
}

/**
 * Work type selection dropdown for contextual analysis.
 */
@Composable
private fun WorkTypeSelector(
    currentWorkType: WorkType,
    onWorkTypeSelected: (WorkType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = HazardColors.OVERLAY_BACKGROUND,
                contentColor = Color.White
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.dp,
                brush = SolidColor(Color.White.copy(alpha = 0.5f))
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = formatWorkType(currentWorkType),
                style = ConstructionTypography.cameraControl
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }
        
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(HazardColors.OVERLAY_BACKGROUND.copy(alpha = 0.95f))
        ) {
            WorkType.values().forEach { workType ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = formatWorkType(workType),
                            style = ConstructionTypography.hazardDescription,
                            color = if (workType == currentWorkType) {
                                HazardColors.SAFE_GREEN
                            } else {
                                Color.White
                            }
                        )
                    },
                    onClick = {
                        onWorkTypeSelected(workType)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Bottom camera controls with capture and analysis options.
 */
@Composable
private fun BottomCameraControls(
    isAnalyzing: Boolean,
    currentAnalysis: SafetyAnalysis?,
    onCapturePhoto: () -> Unit,
    onToggleFlash: () -> Unit,
    onToggleContinuousAnalysis: () -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        
        // Gallery button
        IconButton(
            onClick = onNavigateToGallery,
            modifier = Modifier
                .background(
                    color = HazardColors.OVERLAY_BACKGROUND,
                    shape = CircleShape
                )
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Main capture button
        FloatingActionButton(
            onClick = onCapturePhoto,
            containerColor = if (isAnalyzing) {
                HazardColors.MEDIUM_AMBER
            } else {
                HazardColors.OSHA_BLUE
            },
            modifier = Modifier.size(72.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture & Analyze",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Results/Flash toggle button
        if (currentAnalysis != null) {
            IconButton(
                onClick = onNavigateToResults,
                modifier = Modifier
                    .background(
                        color = HazardColors.SAFE_GREEN,
                        shape = CircleShape
                    )
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = "View Results",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            IconButton(
                onClick = onToggleFlash,
                modifier = Modifier
                    .background(
                        color = HazardColors.OVERLAY_BACKGROUND,
                        shape = CircleShape
                    )
                    .size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn, // TODO: Toggle based on flash state
                    contentDescription = "Toggle Flash",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Analyzing state overlay with progress indicator.
 */
@Composable
private fun AnalyzingOverlay(
    progress: Float,
    currentTask: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = HazardColors.OVERLAY_BACKGROUND.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                progress = progress,
                color = HazardColors.OSHA_BLUE,
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp
            )
            
            Text(
                text = currentTask,
                style = ConstructionTypography.hazardDescription,
                color = Color.White
            )
            
            Text(
                text = "${(progress * 100).toInt()}%",
                style = ConstructionTypography.confidenceText,
                color = HazardColors.TEXT_SECONDARY
            )
        }
    }
}

/**
 * Error state overlay with retry option.
 */
@Composable
private fun ErrorOverlay(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = HazardColors.OVERLAY_BACKGROUND.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = HazardColors.CRITICAL_RED,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "Analysis Failed",
                style = ConstructionTypography.hazardTitle,
                color = HazardColors.CRITICAL_RED
            )
            
            Text(
                text = error,
                style = ConstructionTypography.hazardDescription,
                color = Color.White
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Dismiss")
                }
                
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HazardColors.OSHA_BLUE
                    )
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * Performance monitoring overlay for development.
 */
@Composable
private fun PerformanceMonitor(
    modifier: Modifier = Modifier
) {
    // TODO: Implement performance monitoring display
    Box(
        modifier = modifier
            .background(
                color = HazardColors.OVERLAY_BACKGROUND.copy(alpha = 0.7f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    ) {
        Text(
            text = "FPS: 30 | AI: 2.1s",
            style = ConstructionTypography.secondaryInfo,
            color = HazardColors.TEXT_SECONDARY
        )
    }
}

/**
 * Format work type for display.
 */
private fun formatWorkType(workType: WorkType): String {
    return workType.name
        .lowercase()
        .split('_')
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}

// BuildConfig for debug mode check
object BuildConfig {
    const val DEBUG = true
}

import androidx.compose.ui.graphics.SolidColor