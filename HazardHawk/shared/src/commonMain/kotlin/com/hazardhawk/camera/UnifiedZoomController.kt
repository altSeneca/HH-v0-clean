package com.hazardhawk.camera

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Unified zoom controller - Single source of truth for all zoom operations
 * Replaces: CameraViewModel zoom logic, CameraStateManager zoom, DualVerticalSelectors zoom
 */
class UnifiedZoomController(
    initialMinZoom: Float = 0.5f,
    initialMaxZoom: Float = 8.0f
) {
    private val _zoomState = MutableStateFlow(
        ZoomControlState(
            currentZoom = 1.0f,
            minZoom = initialMinZoom,
            maxZoom = initialMaxZoom,
            targetZoom = 1.0f,
            isLiveZooming = false
        )
    )
    val zoomState: StateFlow<ZoomControlState> = _zoomState.asStateFlow()
    
    private val _zoomEvents = MutableStateFlow<ZoomEvent?>(null)
    val zoomEvents: StateFlow<ZoomEvent?> = _zoomEvents.asStateFlow()
    
    /**
     * Update zoom from any source (pinch, wheel, buttons)
     */
    fun updateZoom(newZoom: Float, source: ZoomSource, isLive: Boolean = false) {
        val currentState = _zoomState.value
        val clampedZoom = newZoom.coerceIn(currentState.minZoom, currentState.maxZoom)
        
        // Avoid unnecessary updates
        if (abs(clampedZoom - currentState.currentZoom) < 0.01f && !isLive) {
            return
        }
        
        _zoomState.value = currentState.copy(
            currentZoom = clampedZoom,
            targetZoom = if (isLive) currentState.targetZoom else clampedZoom,
            isLiveZooming = isLive
        )
        
        // Emit event for camera application
        _zoomEvents.value = ZoomEvent(
            zoom = clampedZoom,
            source = source,
            isLive = isLive,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Update zoom range when camera capabilities change
     */
    fun updateZoomRange(minZoom: Float, maxZoom: Float) {
        val currentState = _zoomState.value
        _zoomState.value = currentState.copy(
            minZoom = minZoom,
            maxZoom = maxZoom,
            currentZoom = currentState.currentZoom.coerceIn(minZoom, maxZoom)
        )
    }
    
    /**
     * Clear zoom event after consumption
     */
    fun clearZoomEvent() {
        _zoomEvents.value = null
    }
    
    /**
     * Quick zoom presets
     */
    fun setZoomPreset(preset: ZoomPreset) {
        val targetZoom = when (preset) {
            ZoomPreset.WIDE -> _zoomState.value.minZoom
            ZoomPreset.NORMAL -> 1.0f
            ZoomPreset.CLOSE -> 2.0f
            ZoomPreset.FAR -> _zoomState.value.maxZoom
        }
        updateZoom(targetZoom, ZoomSource.PRESET)
    }
}

/**
 * Simplified zoom state
 */
data class ZoomControlState(
    val currentZoom: Float,
    val minZoom: Float,
    val maxZoom: Float,
    val targetZoom: Float,
    val isLiveZooming: Boolean
)

/**
 * Zoom event for camera application
 */
data class ZoomEvent(
    val zoom: Float,
    val source: ZoomSource,
    val isLive: Boolean,
    val timestamp: Long
)

/**
 * Source of zoom change for debugging and behavior differentiation
 */
enum class ZoomSource {
    PINCH,      // Pinch-to-zoom gesture
    WHEEL,      // Zoom wheel UI control
    BUTTON,     // Plus/minus buttons
    PRESET,     // Quick preset buttons
    EXTERNAL    // External API call
}

/**
 * Quick zoom presets
 */
enum class ZoomPreset {
    WIDE,       // Minimum zoom
    NORMAL,     // 1x zoom
    CLOSE,      // 2x zoom
    FAR         // Maximum zoom
}
