package com.hazardhawk.ui.camera.hud

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.ai.AIServiceFacade
import com.hazardhawk.camera.MetadataSettingsManager
import com.hazardhawk.ui.camera.hud.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * State manager for Safety HUD Camera UI
 * Simple, single-responsibility state coordination
 * 
 * Follows the "Simple" principle: clear state transitions, minimal complexity
 */
class HUDStateManager(
    private val context: Context,
    private val metadataSettings: MetadataSettingsManager,
    private val aiService: AIServiceFacade? = null
) : ViewModel() {
    
    companion object {
        private const val TAG = "HUDStateManager"
        private const val EMERGENCY_MODE_TIMEOUT_MS = 500L
        private const val AI_ANALYSIS_TIMEOUT_MS = 30_000L
        private const val METADATA_UPDATE_INTERVAL_MS = 1000L
    }
    
    // Single state source - Simple principle
    private val _state = MutableStateFlow(SafetyHUDState())
    val state: StateFlow<SafetyHUDState> = _state.asStateFlow()
    
    // Individual state flows for UI binding
    val cameraState = state.map { it.cameraState }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CameraState.Ready)
    
    val controlRingVisible = state.map { it.controlRingVisible }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    
    val metadataOverlay = state.map { it.metadataOverlay }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MetadataOverlayState())
    
    val emergencyMode = state.map { it.emergencyMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    
    // Performance tracking
    private var lastRenderTime = 0L
    private var frameCount = 0L
    
    init {
        // Start metadata updates
        startMetadataUpdates()
        Log.d(TAG, "HUD State Manager initialized")
    }
    
    /**
     * Core camera operations - Simple, reliable state transitions
     */
    
    fun capturePhoto() {
        Log.d(TAG, "Starting photo capture")
        viewModelScope.launch {
            try {
                // Transition to capturing state
                updateState { it.copy(
                    cameraState = CameraState.Capturing,
                    controlRingVisible = false
                )}
                
                // Brief delay for UI feedback
                delay(200)
                
                // Photo captured - transition to AI analysis if enabled
                if (aiService != null && metadataSettings.aiAnalysisEnabledState.value) {
                    updateState { it.copy(cameraState = CameraState.AnalyzingAI) }
                    // AI analysis will be handled by the actual capture implementation
                } else {
                    // No AI analysis - return to ready state
                    returnToReady()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Photo capture failed", e)
                handleError(HUDError.CameraCaptureError)
            }
        }
    }
    
    fun showPostCapture(photo: HUDPhotoAnalysis) {
        Log.d(TAG, "Showing post-capture overlay for: ${photo.photoPath}")
        updateState { it.copy(
            cameraState = CameraState.PostCapture,
            capturedPhoto = photo,
            aiAnalysisInProgress = false,
            controlRingVisible = false
        )}
    }
    
    fun confirmCapture() {
        Log.d(TAG, "Confirming photo capture")
        val currentPhoto = _state.value.capturedPhoto
        if (currentPhoto != null) {
            // Save photo metadata and tags here
            Log.d(TAG, "Photo confirmed: ${currentPhoto.photoPath}")
        }
        returnToReady()
    }
    
    fun cancelCapture() {
        Log.d(TAG, "Canceling photo capture")
        returnToReady()
    }
    
    fun returnToReady() {
        updateState { it.copy(
            cameraState = CameraState.Ready,
            capturedPhoto = null,
            aiAnalysisInProgress = false,
            controlRingVisible = true
        )}
    }
    
    /**
     * Flash control for construction environments
     */
    fun toggleFlash() {
        val currentFlashMode = _state.value.flashMode
        val newFlashMode = when (currentFlashMode) {
            FlashMode.Auto -> FlashMode.On
            FlashMode.On -> FlashMode.Off
            FlashMode.Off -> FlashMode.Auto
        }
        
        Log.d(TAG, "Flash mode: $currentFlashMode -> $newFlashMode")
        updateState { it.copy(flashMode = newFlashMode) }
        
        // Update metadata settings for persistence
        metadataSettings.updateFlashMode(newFlashMode.name.lowercase())
    }
    
    /**
     * Aspect ratio control for different documentation needs
     */
    fun updateAspectRatio(ratio: AspectRatio) {
        Log.d(TAG, "Aspect ratio changed to: $ratio")
        updateState { it.copy(aspectRatio = ratio) }
        
        // TODO: Apply aspect ratio to camera controller
        // This will require updating the camera setup in the main screen
    }
    
    /**
     * Zoom control for one-handed operation
     */
    fun zoomIn() {
        val currentZoom = _state.value.zoomRatio
        val newZoom = (currentZoom + 0.5f).coerceAtMost(8.0f) // Max 8x zoom
        
        Log.d(TAG, "Zoom in: $currentZoom -> $newZoom")
        updateState { it.copy(zoomRatio = newZoom) }
        
        // TODO: Apply zoom to camera controller
    }
    
    fun zoomOut() {
        val currentZoom = _state.value.zoomRatio
        val newZoom = (currentZoom - 0.5f).coerceAtLeast(1.0f) // Min 1x zoom
        
        Log.d(TAG, "Zoom out: $currentZoom -> $newZoom")
        updateState { it.copy(zoomRatio = newZoom) }
        
        // TODO: Apply zoom to camera controller
    }
    
    fun resetZoom() {
        Log.d(TAG, "Zoom reset to 1x")
        updateState { it.copy(zoomRatio = 1.0f) }
        
        // TODO: Apply zoom to camera controller
    }
    
    /**
     * Emergency mode for critical situations
     * High-contrast, essential functionality only
     */
    fun enableEmergencyMode() {
        Log.w(TAG, "Emergency mode activated")
        updateState { it.copy(emergencyMode = true) }
        
        viewModelScope.launch {
            // Auto-disable after timeout unless manually disabled
            delay(EMERGENCY_MODE_TIMEOUT_MS * 60) // 30 seconds
            if (_state.value.emergencyMode) {
                Log.d(TAG, "Emergency mode auto-disabled after timeout")
                disableEmergencyMode()
            }
        }
    }
    
    fun disableEmergencyMode() {
        Log.d(TAG, "Emergency mode deactivated")
        updateState { it.copy(emergencyMode = false) }
    }
    
    /**
     * Location and metadata updates
     */
    fun updateLocation(location: Location?) {
        if (location != null) {
            val coordinates = "${location.latitude}, ${location.longitude}"
            val currentOverlay = _state.value.metadataOverlay
            updateState { it.copy(
                metadataOverlay = currentOverlay.copy(gpsCoordinates = coordinates)
            )}
        }
    }
    
    fun updateProjectInfo(companyName: String, projectName: String) {
        Log.d(TAG, "Updating project info: $companyName - $projectName")
        val currentOverlay = _state.value.metadataOverlay
        updateState { it.copy(
            metadataOverlay = currentOverlay.copy(
                companyName = companyName,
                projectName = projectName
            )
        )}
    }
    
    /**
     * Error handling with construction-friendly messaging
     */
    fun handleError(error: HUDError) {
        Log.e(TAG, "HUD Error: $error")
        updateState { it.copy(cameraState = CameraState.Error) }
        
        // Auto-recovery after error display
        viewModelScope.launch {
            delay(3000)
            if (_state.value.cameraState == CameraState.Error) {
                returnToReady()
            }
        }
    }
    
    /**
     * Performance monitoring for construction environment optimization
     */
    fun recordRenderTime(renderTimeMs: Long) {
        lastRenderTime = renderTimeMs
        frameCount++
        
        // Log performance issues for optimization
        if (renderTimeMs > 16) { // >16ms = <60fps
            Log.w(TAG, "HUD render time: ${renderTimeMs}ms (target: <16ms)")
        }
    }
    
    fun getPerformanceMetrics(): HUDPerformanceMetrics {
        val avgRenderTime = if (frameCount > 0) lastRenderTime else 0L
        val fps = if (lastRenderTime > 0) 1000f / lastRenderTime else 0f
        
        return HUDPerformanceMetrics(
            hudRenderTimeMs = avgRenderTime,
            cameraPreviewFps = fps
        )
    }
    
    /**
     * Control Ring tool management
     */
    fun getControlRingTools(): List<ControlRingTool> {
        val currentState = _state.value
        return when (currentState.cameraState) {
            CameraState.Ready -> getReadyStateTools()
            CameraState.PostCapture -> getPostCaptureTools()
            CameraState.Error -> getErrorStateTools()
            else -> emptyList()
        }
    }
    
    private fun getReadyStateTools(): List<ControlRingTool> {
        return listOf(
            ControlRingTool("gallery", "gallery", "Gallery", RingPosition.RIGHT),
            ControlRingTool("flash", "flash_${_state.value.flashMode.name.lowercase()}", "Flash", RingPosition.LEFT),
            ControlRingTool("settings", "settings", "Settings", RingPosition.TOP),
            ControlRingTool("zoom", "zoom", "Zoom", RingPosition.BOTTOM)
        )
    }
    
    private fun getPostCaptureTools(): List<ControlRingTool> {
        return listOf(
            ControlRingTool("confirm", "check", "Confirm", RingPosition.RIGHT),
            ControlRingTool("cancel", "close", "Cancel", RingPosition.LEFT),
            ControlRingTool("retake", "camera", "Retake", RingPosition.BOTTOM)
        )
    }
    
    private fun getErrorStateTools(): List<ControlRingTool> {
        return listOf(
            ControlRingTool("retry", "refresh", "Retry", RingPosition.RIGHT),
            ControlRingTool("emergency", "warning", "Emergency", RingPosition.LEFT)
        )
    }
    
    /**
     * Private helper methods
     */
    
    private fun updateState(update: (SafetyHUDState) -> SafetyHUDState) {
        _state.value = update(_state.value)
    }
    
    private fun startMetadataUpdates() {
        viewModelScope.launch {
            while (true) {
                val currentOverlay = _state.value.metadataOverlay
                updateState { it.copy(
                    metadataOverlay = currentOverlay.copy(
                        timestamp = System.currentTimeMillis()
                    )
                )}
                delay(METADATA_UPDATE_INTERVAL_MS)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "HUD State Manager cleared")
    }
}

/**
 * Factory for creating HUD State Manager with proper dependencies
 */
object HUDStateManagerFactory {
    fun create(
        context: Context,
        metadataSettings: MetadataSettingsManager,
        aiService: AIServiceFacade? = null
    ): HUDStateManager {
        return HUDStateManager(context, metadataSettings, aiService)
    }
}