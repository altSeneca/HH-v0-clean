package com.hazardhawk.camera

import com.hazardhawk.ui.components.WheelItem
import com.hazardhawk.ui.components.AspectRatios
import com.hazardhawk.ui.components.ZoomLevels
import com.hazardhawk.data.models.CameraSettings
import com.hazardhawk.data.repositories.CameraSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Camera state for aspect ratio and zoom management
 */
data class CameraState(
    val aspectRatio: WheelItem = AspectRatios.FULL,
    val zoom: Float = 1.0f,
    val supportedAspectRatios: List<WheelItem> = AspectRatios.getSupported(),
    val supportedZoomLevels: List<WheelItem> = ZoomLevels.generateZoomItems(),
    val minZoom: Float = 0.5f,
    val maxZoom: Float = 8.0f,
    val isZoomLive: Boolean = false,
    val selectedAspectRatioIndex: Int = 0,
    val selectedZoomIndex: Int = getDefaultZoomIndex()
) {
    companion object {
        private fun getDefaultZoomIndex(): Int {
            val defaultZoomLevels = ZoomLevels.generateZoomItems()
            return defaultZoomLevels.indexOfFirst { (it.value as? Float) == 1.0f }.coerceAtLeast(0)
        }
    }
}

/**
 * Camera state manager with debounced updates and persistent settings
 */
class CameraStateManager(
    private val settingsRepository: CameraSettingsRepository,
    private val scope: CoroutineScope
) {
    
    private val _state = MutableStateFlow(CameraState())
    val state: StateFlow<CameraState> = _state.asStateFlow()
    
    private val _aspectRatioChangeEvents = MutableStateFlow<AspectRatioChangeEvent?>(null)
    val aspectRatioChangeEvents: StateFlow<AspectRatioChangeEvent?> = _aspectRatioChangeEvents.asStateFlow()
    
    private val _zoomChangeEvents = MutableStateFlow<ZoomChangeEvent?>(null)
    val zoomChangeEvents: StateFlow<ZoomChangeEvent?> = _zoomChangeEvents.asStateFlow()
    
    init {
        // Load persisted settings on initialization
        scope.launch {
            loadSettings()
        }
        
        // Observe settings changes and update state
        scope.launch {
            settingsRepository.getSettingsFlow().collect { settings ->
                updateStateFromSettings(settings)
            }
        }
    }
    
    /**
     * Load settings from persistent storage
     */
    private suspend fun loadSettings() {
        val settings = settingsRepository.loadSettings()
        updateStateFromSettings(settings)
    }
    
    /**
     * Update camera state from persistent settings
     */
    private fun updateStateFromSettings(settings: CameraSettings) {
        val currentState = _state.value
        
        // Find the aspect ratio item that matches the saved setting
        val aspectRatioItem = currentState.supportedAspectRatios.find { 
            it.id == settings.selectedAspectRatio 
        } ?: AspectRatios.FULL
        
        // Find the zoom level that matches the saved setting
        val zoomIndex = findClosestZoomIndex(settings.selectedZoom)
        
        _state.value = currentState.copy(
            aspectRatio = aspectRatioItem,
            selectedAspectRatioIndex = settings.selectedAspectRatioIndex,
            zoom = settings.selectedZoom,
            selectedZoomIndex = zoomIndex,
            minZoom = settings.minZoom,
            maxZoom = settings.maxZoom
        )
    }
    
    /**
     * Update aspect ratio with debouncing
     */
    fun updateAspectRatio(item: WheelItem, index: Int) {
        val currentState = _state.value
        if (currentState.aspectRatio.id != item.id) {
            _state.value = currentState.copy(
                aspectRatio = item,
                selectedAspectRatioIndex = index
            )
            
            // Persist the change
            scope.launch {
                settingsRepository.updateAspectRatio(item.id, index)
            }
            
            // Emit change event for camera preview update
            _aspectRatioChangeEvents.value = AspectRatioChangeEvent(
                ratio = item.value as? Float ?: 0f,
                ratioItem = item,
                timestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Update zoom with live streaming and snap support
     */
    fun updateZoom(zoom: Float, isLive: Boolean = false, snapToItem: WheelItem? = null) {
        val clampedZoom = zoom.coerceIn(_state.value.minZoom, _state.value.maxZoom)
        val currentState = _state.value
        
        // Find closest zoom index
        val zoomIndex = if (snapToItem != null) {
            currentState.supportedZoomLevels.indexOfFirst { it.id == snapToItem.id }
                .coerceAtLeast(0)
        } else {
            findClosestZoomIndex(clampedZoom)
        }
        
        _state.value = currentState.copy(
            zoom = clampedZoom,
            isZoomLive = isLive,
            selectedZoomIndex = zoomIndex
        )
        
        // Persist zoom changes (but not during live pinch-to-zoom to avoid excessive writes)
        if (!isLive) {
            scope.launch {
                settingsRepository.updateZoom(clampedZoom, zoomIndex)
            }
        }
        
        // Emit zoom change event
        _zoomChangeEvents.value = ZoomChangeEvent(
            zoom = clampedZoom,
            isLive = isLive,
            snapItem = snapToItem,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Update supported zoom range based on current lens
     */
    fun updateZoomRange(minZoom: Float, maxZoom: Float) {
        val currentState = _state.value
        val newZoomLevels = ZoomLevels.generateZoomItems(minZoom, maxZoom)
        
        _state.value = currentState.copy(
            minZoom = minZoom,
            maxZoom = maxZoom,
            supportedZoomLevels = newZoomLevels,
            zoom = currentState.zoom.coerceIn(minZoom, maxZoom),
            selectedZoomIndex = findClosestZoomIndex(currentState.zoom, newZoomLevels)
        )
    }
    
    /**
     * Update supported aspect ratios based on camera capabilities
     */
    fun updateSupportedAspectRatios(ratios: List<WheelItem>) {
        val currentState = _state.value
        val currentRatioStillSupported = ratios.any { it.id == currentState.aspectRatio.id }
        
        val newAspectRatio = if (currentRatioStillSupported) {
            currentState.aspectRatio
        } else {
            ratios.firstOrNull() ?: AspectRatios.FULL
        }
        
        val newIndex = ratios.indexOfFirst { it.id == newAspectRatio.id }.coerceAtLeast(0)
        
        _state.value = currentState.copy(
            supportedAspectRatios = ratios,
            aspectRatio = newAspectRatio,
            selectedAspectRatioIndex = newIndex
        )
    }
    
    /**
     * Clear change events after consumption
     */
    fun clearAspectRatioEvent() {
        _aspectRatioChangeEvents.value = null
    }
    
    fun clearZoomEvent() {
        _zoomChangeEvents.value = null
    }
    
    /**
     * Persist the current zoom level after live zoom gestures end
     */
    fun persistCurrentZoom() {
        val currentState = _state.value
        scope.launch {
            settingsRepository.updateZoom(currentState.zoom, currentState.selectedZoomIndex)
        }
    }
    
    private fun findClosestZoomIndex(zoom: Float, zoomLevels: List<WheelItem> = _state.value.supportedZoomLevels): Int {
        if (zoomLevels.isEmpty()) return 0
        
        var closestIndex = 0
        var smallestDifference = Float.MAX_VALUE
        
        zoomLevels.forEachIndexed { index, item ->
            val itemZoom = item.value as? Float ?: 1f
            val difference = kotlin.math.abs(itemZoom - zoom)
            
            if (difference < smallestDifference) {
                smallestDifference = difference
                closestIndex = index
            }
        }
        
        return closestIndex
    }
}

/**
 * Events for camera state changes
 */
data class AspectRatioChangeEvent(
    val ratio: Float,
    val ratioItem: WheelItem,
    val timestamp: Long
)

data class ZoomChangeEvent(
    val zoom: Float,
    val isLive: Boolean,
    val snapItem: WheelItem?,
    val timestamp: Long
)