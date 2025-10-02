package com.hazardhawk.ui.camera.clear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.camera.UnifiedViewfinderCalculator
import com.hazardhawk.ui.camera.clear.components.AIAnalysisState
import com.hazardhawk.ui.camera.clear.components.FlashMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ClearCameraViewModel - State Management for ClearCameraScreen
 *
 * Manages:
 * - UI visibility (auto-hide timers)
 * - Camera controls (zoom, aspect ratio, flash)
 * - AI analysis state
 * - Capture state
 * - Interaction tracking
 */
class ClearCameraViewModel : ViewModel() {

    // ══════════════════════════════════════════════════════════════════
    // UI Visibility State
    // ══════════════════════════════════════════════════════════════════

    private val _bottomBarVisible = MutableStateFlow(true)
    val bottomBarVisible: StateFlow<Boolean> = _bottomBarVisible.asStateFlow()

    private val _zoomPanelVisible = MutableStateFlow(false)
    val zoomPanelVisible: StateFlow<Boolean> = _zoomPanelVisible.asStateFlow()

    private val _lastInteractionTime = MutableStateFlow(System.currentTimeMillis())
    val lastInteractionTime: StateFlow<Long> = _lastInteractionTime.asStateFlow()

    // Auto-hide jobs
    private var bottomBarAutoHideJob: Job? = null
    private var zoomPanelAutoHideJob: Job? = null

    // ══════════════════════════════════════════════════════════════════
    // Camera Controls State
    // ══════════════════════════════════════════════════════════════════

    private val _currentZoom = MutableStateFlow(1.0f)
    val currentZoom: StateFlow<Float> = _currentZoom.asStateFlow()

    private val _maxZoom = MutableStateFlow(10.0f)
    val maxZoom: StateFlow<Float> = _maxZoom.asStateFlow()

    private val _currentAspectRatio = MutableStateFlow(UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE)
    val currentAspectRatio: StateFlow<UnifiedViewfinderCalculator.ViewfinderAspectRatio> = _currentAspectRatio.asStateFlow()

    private val _flashMode = MutableStateFlow(FlashMode.OFF)
    val flashMode: StateFlow<FlashMode> = _flashMode.asStateFlow()

    private val _isARMode = MutableStateFlow(false)
    val isARMode: StateFlow<Boolean> = _isARMode.asStateFlow()

    // ══════════════════════════════════════════════════════════════════
    // Capture State
    // ══════════════════════════════════════════════════════════════════

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    // ══════════════════════════════════════════════════════════════════
    // AI Analysis State
    // ══════════════════════════════════════════════════════════════════

    private val _aiAnalysisState = MutableStateFlow<AIAnalysisState>(AIAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AIAnalysisState> = _aiAnalysisState.asStateFlow()

    // ══════════════════════════════════════════════════════════════════
    // Initialization
    // ══════════════════════════════════════════════════════════════════

    init {
        // Bottom bar stays visible - no auto-hide for clear camera
        _bottomBarVisible.value = true
        // Show zoom panel initially for 5 seconds so user discovers it
        _zoomPanelVisible.value = true
        startZoomPanelAutoHide()
    }

    // ══════════════════════════════════════════════════════════════════
    // UI Visibility Management
    // ══════════════════════════════════════════════════════════════════

    fun onUserInteraction() {
        _lastInteractionTime.value = System.currentTimeMillis()

        // Bottom bar always visible - no auto-hide
        _bottomBarVisible.value = true
    }

    private fun startBottomBarAutoHide() {
        // Disabled - bottom bar stays visible in clear camera
        bottomBarAutoHideJob?.cancel()
        /* Commented out - bottom bar no longer auto-hides
        bottomBarAutoHideJob = viewModelScope.launch {
            delay(8000L) // 8 seconds
            _bottomBarVisible.value = false
        }
        */
    }

    fun showZoomPanel() {
        _zoomPanelVisible.value = true
        startZoomPanelAutoHide()
        onUserInteraction()
    }

    fun hideZoomPanel() {
        _zoomPanelVisible.value = false
        zoomPanelAutoHideJob?.cancel()
    }

    fun toggleZoomPanel() {
        if (_zoomPanelVisible.value) {
            hideZoomPanel()
        } else {
            showZoomPanel()
        }
    }

    private fun startZoomPanelAutoHide() {
        zoomPanelAutoHideJob?.cancel()
        zoomPanelAutoHideJob = viewModelScope.launch {
            delay(5000L) // 5 seconds - longer for discoverability
            _zoomPanelVisible.value = false
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Camera Controls
    // ══════════════════════════════════════════════════════════════════

    fun updateZoom(zoom: Float) {
        _currentZoom.value = zoom.coerceIn(1.0f, _maxZoom.value)
        showZoomPanel()
    }

    fun updateMaxZoom(maxZoom: Float) {
        _maxZoom.value = maxZoom
    }

    /**
     * Reset zoom to 1.0x (called when camera is rebound or screen is left)
     */
    fun resetZoom() {
        _currentZoom.value = 1.0f
    }

    fun updateAspectRatio(aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio) {
        _currentAspectRatio.value = aspectRatio
        onUserInteraction()
    }

    fun toggleFlash() {
        _flashMode.value = when (_flashMode.value) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        onUserInteraction()
    }

    fun toggleARMode() {
        _isARMode.value = !_isARMode.value
        onUserInteraction()
    }

    // ══════════════════════════════════════════════════════════════════
    // Capture State Management
    // ══════════════════════════════════════════════════════════════════

    fun startCapture() {
        _isCapturing.value = true
    }

    fun endCapture() {
        _isCapturing.value = false
    }

    // ══════════════════════════════════════════════════════════════════
    // AI Analysis State Management
    // ══════════════════════════════════════════════════════════════════

    fun startAnalysis() {
        _aiAnalysisState.value = AIAnalysisState.Analyzing("Starting AI analysis...")
    }

    fun updateAnalysisProgress(progress: String) {
        _aiAnalysisState.value = AIAnalysisState.Analyzing(progress)
    }

    fun completeAnalysis(message: String, isCritical: Boolean = false) {
        _aiAnalysisState.value = AIAnalysisState.Complete(message, isCritical)

        // Auto-reset to idle after banner dismisses
        viewModelScope.launch {
            delay(5000L + 500L) // Banner duration + fade out
            _aiAnalysisState.value = AIAnalysisState.Idle
        }
    }

    fun analysisError(errorMessage: String) {
        _aiAnalysisState.value = AIAnalysisState.Error(errorMessage)

        // Auto-reset to idle after error dismisses
        viewModelScope.launch {
            delay(3000L + 500L)
            _aiAnalysisState.value = AIAnalysisState.Idle
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Cleanup
    // ══════════════════════════════════════════════════════════════════

    override fun onCleared() {
        super.onCleared()
        bottomBarAutoHideJob?.cancel()
        zoomPanelAutoHideJob?.cancel()
    }
}
