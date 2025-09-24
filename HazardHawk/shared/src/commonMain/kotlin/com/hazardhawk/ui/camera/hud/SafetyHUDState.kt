package com.hazardhawk.ui.camera.hud

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State definitions for Safety HUD Camera UI
 * Simple, minimal state management focused on construction worker workflow
 */

/**
 * Core Safety HUD state container
 * Single source of truth for all HUD functionality
 */
data class SafetyHUDState(
    val cameraState: CameraState = CameraState.Ready,
    val controlRingVisible: Boolean = true,
    val metadataOverlay: MetadataOverlayState = MetadataOverlayState(),
    val capturedPhoto: HUDPhotoAnalysis? = null,
    val aiAnalysisInProgress: Boolean = false,
    val flashMode: FlashMode = FlashMode.Auto,
    val aspectRatio: AspectRatio = AspectRatio.RATIO_16_9,
    val zoomRatio: Float = 1.0f,
    val lastGalleryPhoto: String? = null, // Thumbnail path for gallery access
    val emergencyMode: Boolean = false // High-contrast, critical functionality only
)

/**
 * Camera operational states
 * Clean state transitions for reliable operation
 */
sealed class CameraState {
    object Ready : CameraState()
    object Capturing : CameraState()
    object AnalyzingAI : CameraState()
    object PostCapture : CameraState()
    object Error : CameraState()
}

/**
 * Flash mode control for construction environments
 * Optimized for outdoor visibility and safety documentation
 */
enum class FlashMode {
    Auto,   // Automatic flash detection
    On,     // Always on for shadowed areas
    Off     // Off for bright daylight
}

/**
 * Aspect ratio options for construction documentation
 * Optimized for safety documentation and compliance photos
 */
enum class AspectRatio {
    RATIO_16_9,  // Standard widescreen for landscape documentation
    RATIO_4_3,   // Traditional camera format for detailed shots
    RATIO_1_1    // Square format for social media and quick documentation
}

/**
 * Metadata overlay state for construction compliance
 * Required information permanently burned into photos
 */
data class MetadataOverlayState(
    val companyName: String = "",
    val projectName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val gpsCoordinates: String = "",
    val address: String = "",
    val visible: Boolean = true
)

/**
 * Post-capture photo analysis results with AI tags
 * Integrates with existing GeminiVisionAnalyzer system
 */
data class HUDPhotoAnalysis(
    val photoPath: String,
    val aiTags: List<AITag> = emptyList(),
    val confidenceScore: Float = 0f,
    val hazardDetected: Boolean = false,
    val analysisTimestamp: Long = System.currentTimeMillis()
)

/**
 * AI-generated safety tags for construction hazards
 * Compatible with existing UITagRecommendation system
 */
data class AITag(
    val id: String,
    val text: String,
    val confidence: Float,
    val category: TagCategory,
    val isHazard: Boolean = false,
    val oshaCode: String? = null
)

/**
 * Construction safety tag categories
 * OSHA-compliant classification system
 */
enum class TagCategory {
    PPE,            // Personal Protective Equipment
    HAZARD,         // Safety hazards
    EQUIPMENT,      // Construction equipment
    STRUCTURAL,     // Building/structural elements
    ENVIRONMENTAL,  // Weather/site conditions
    PERSONNEL,      // People in photos
    COMPLIANCE      // Regulatory compliance items
}

/**
 * Control Ring tool configuration
 * Adaptive tools based on camera state and user context
 */
data class ControlRingTool(
    val id: String,
    val icon: String, // Icon identifier
    val label: String,
    val position: RingPosition,
    val enabled: Boolean = true,
    val visible: Boolean = true
)

/**
 * Radial positions around capture button
 * 8 total positions for contextual tools
 */
enum class RingPosition {
    TOP,
    TOP_RIGHT,
    RIGHT,
    BOTTOM_RIGHT,
    BOTTOM,
    BOTTOM_LEFT,
    LEFT,
    TOP_LEFT
}

/**
 * Error states with recovery actions
 * Construction-friendly error messaging
 */
sealed class HUDError {
    object CameraPermissionDenied : HUDError()
    object LocationPermissionDenied : HUDError()
    object CameraCaptureError : HUDError()
    object AIServiceUnavailable : HUDError()
    object StorageError : HUDError()
    data class NetworkError(val message: String) : HUDError()
}

/**
 * Performance metrics for construction environment optimization
 * Monitors HUD render performance and camera responsiveness
 */
data class HUDPerformanceMetrics(
    val hudRenderTimeMs: Long = 0,
    val cameraPreviewFps: Float = 0f,
    val aiAnalysisTimeMs: Long = 0,
    val memoryUsageMB: Float = 0f,
    val batteryTemperature: Float = 0f
)