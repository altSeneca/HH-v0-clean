package com.hazardhawk.ui.camera

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.hazardhawk.ui.components.*
import com.hazardhawk.camera.CameraState
import com.hazardhawk.camera.CameraStateManager
import kotlinx.coroutines.flow.collectLatest
import android.util.Log

/**
 * Dual Vertical Selectors for Camera UI
 * Left side: Aspect Ratio selector
 * Right side: Zoom control
 */
@Composable
fun DualVerticalSelectors(
    cameraStateManager: CameraStateManager,
    onAspectRatioChange: (ratio: Float, ratioItem: WheelItem) -> Unit,
    onZoomChange: (zoom: Float) -> Unit,
    onZoomLive: (zoom: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    // Observe camera state
    val cameraState by cameraStateManager.state.collectAsState()
    
    // Handle aspect ratio change events
    LaunchedEffect(cameraStateManager) {
        cameraStateManager.aspectRatioChangeEvents.collectLatest { event ->
            event?.let {
                Log.d("DualVerticalSelectors", "Aspect ratio changed: ${it.ratioItem.label}")
                onAspectRatioChange(it.ratio, it.ratioItem)
                cameraStateManager.clearAspectRatioEvent()
            }
        }
    }
    
    // Handle zoom change events
    LaunchedEffect(cameraStateManager) {
        cameraStateManager.zoomChangeEvents.collectLatest { event ->
            event?.let {
                Log.d("DualVerticalSelectors", "Zoom changed: ${it.zoom}, live: ${it.isLive}")
                if (it.isLive) {
                    onZoomLive(it.zoom)
                } else {
                    onZoomChange(it.zoom)
                }
                cameraStateManager.clearZoomEvent()
            }
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Left Side - Aspect Ratio Selector
        AspectRatioSelector(
            items = cameraState.supportedAspectRatios,
            selectedIndex = cameraState.selectedAspectRatioIndex,
            onChange = { item, index ->
                Log.d("DualVerticalSelectors", "AspectRatio selected: ${item.label} at index $index")
                cameraStateManager.updateAspectRatio(item, index)
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp) // Offset inward for thumb comfort
                .width(80.dp)
                .height(240.dp)
                .zIndex(10f)
        )
        
        // Right Side - Zoom Control Selector
        ZoomControlSelector(
            items = cameraState.supportedZoomLevels,
            selectedIndex = cameraState.selectedZoomIndex,
            onChange = { item, index ->
                val zoomValue = item.value as? Float ?: 1f
                Log.d("DualVerticalSelectors", "Zoom selected: ${item.label} ($zoomValue) at index $index")
                cameraStateManager.updateZoom(zoomValue, isLive = false, snapToItem = item)
            },
            onChangeLive = { progressValue ->
                // Continuous zoom updates during drag
                Log.d("DualVerticalSelectors", "Zoom live update: $progressValue")
                cameraStateManager.updateZoom(progressValue, isLive = true)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp) // Offset inward for thumb comfort
                .width(80.dp)
                .height(240.dp)
                .zIndex(10f)
        )
    }
}

/**
 * Update zoom range when camera capabilities change
 */
@Composable
fun DualVerticalSelectorsWithCameraController(
    cameraStateManager: CameraStateManager,
    cameraController: androidx.camera.view.LifecycleCameraController,
    onAspectRatioChange: (ratio: Float, ratioItem: WheelItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Monitor camera zoom capabilities
    LaunchedEffect(cameraController) {
        try {
            cameraController.cameraInfo?.let { cameraInfo ->
                val zoomState = cameraInfo.zoomState.value
                if (zoomState != null) {
                    val minZoom = zoomState.minZoomRatio
                    val maxZoom = zoomState.maxZoomRatio
                    
                    Log.d("DualVerticalSelectors", "Camera zoom range: $minZoom - $maxZoom")
                    cameraStateManager.updateZoomRange(minZoom, maxZoom)
                }
            }
        } catch (e: Exception) {
            Log.e("DualVerticalSelectors", "Failed to get camera zoom info: ${e.message}")
        }
    }
    
    DualVerticalSelectors(
        cameraStateManager = cameraStateManager,
        onAspectRatioChange = onAspectRatioChange,
        onZoomChange = { zoom ->
            // Apply zoom to camera controller
            try {
                cameraController.setZoomRatio(zoom)
                Log.d("DualVerticalSelectors", "Applied zoom to camera: $zoom")
            } catch (e: Exception) {
                Log.e("DualVerticalSelectors", "Failed to apply zoom: ${e.message}")
            }
        },
        onZoomLive = { zoom ->
            // Live zoom updates - throttled application to camera
            try {
                cameraController.setZoomRatio(zoom)
            } catch (e: Exception) {
                Log.e("DualVerticalSelectors", "Failed to apply live zoom: ${e.message}")
            }
        },
        modifier = modifier
    )
}