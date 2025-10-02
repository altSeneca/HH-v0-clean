package com.hazardhawk.ui.ar

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.ai.core.SmartAIOrchestrator
import com.hazardhawk.ai.models.Hazard
import com.hazardhawk.ai.models.WorkType
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.ui.camera.capturePhoto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * ViewModel for LiveDetectionScreen managing real-time AI analysis and camera state.
 * Integrates with SmartAIOrchestrator for 2 FPS throttled AI processing.
 */
class LiveDetectionViewModel(
    private val smartAIOrchestrator: SmartAIOrchestrator
) : ViewModel() {
    
    companion object {
        /**
         * Create a LiveDetectionViewModel with mock SmartAIOrchestrator for development.
         * TODO: Replace with proper DI when modules are set up.
         */
        fun createForTesting(): LiveDetectionViewModel {
            // This is a temporary factory for development
            // In production, this will be provided by DI
            val mockOrchestrator = MockSmartAIOrchestrator()
            return LiveDetectionViewModel(mockOrchestrator)
        }
    }

    // Analysis state management
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _currentAnalysis = MutableStateFlow<SafetyAnalysis?>(null)
    val currentAnalysis: StateFlow<SafetyAnalysis?> = _currentAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // Camera state management
    private val _cameraState = MutableStateFlow(CameraState())
    val cameraState: StateFlow<CameraState> = _cameraState.asStateFlow()

    // AI processing throttling (2 FPS = 500ms intervals)
    private var lastAnalysisTime = 0L
    private val analysisInterval = 500L // 2 FPS throttling
    
    // Current work type and configuration
    private var currentWorkType = WorkType.GENERAL_CONSTRUCTION
    private var imageCapture: ImageCapture? = null
    private var selectedHazard: Hazard? = null
    
    // Continuous analysis flag
    private val _continuousAnalysis = MutableStateFlow(false)
    val continuousAnalysis: StateFlow<Boolean> = _continuousAnalysis.asStateFlow()
    
    init {
        // Initialize AI orchestrator with API key from BuildConfig
        viewModelScope.launch {
            try {
                // Get API key from local.properties via BuildConfig
                val apiKey = com.hazardhawk.ai.AIConfig.getGeminiApiKey()

                if (com.hazardhawk.ai.AIConfig.isGeminiConfigured()) {
                    smartAIOrchestrator.configure(apiKey)
                } else {
                    // Log warning if API key not configured
                    println("⚠️ Gemini API key not configured. Add to local.properties")
                    smartAIOrchestrator.configure()
                }
            } catch (e: Exception) {
                println("❌ Error configuring AI: ${e.message}")
                smartAIOrchestrator.configure()
            }
        }
    }

    /**
     * Analyze photo with 2 FPS throttling and error handling.
     */
    fun analyzePhoto(imageData: ByteArray) {
        val currentTime = System.currentTimeMillis()
        
        // Apply 2 FPS throttling
        if (currentTime - lastAnalysisTime < analysisInterval) {
            return // Skip this frame
        }
        
        lastAnalysisTime = currentTime
        
        if (_isAnalyzing.value) {
            return // Already analyzing
        }
        
        viewModelScope.launch {
            try {
                _isAnalyzing.value = true
                _analysisState.value = AnalysisState.Analyzing(0.1f, "Initializing AI analysis...")
                
                delay(100) // Brief delay to show analyzing state
                
                _analysisState.value = AnalysisState.Analyzing(0.3f, "Processing image...")
                
                // Call SmartAIOrchestrator for analysis
                val result = smartAIOrchestrator.analyzePhoto(imageData, currentWorkType)
                
                _analysisState.value = AnalysisState.Analyzing(0.8f, "Finalizing results...")
                
                delay(100) // Brief delay before showing results
                
                result.fold(
                    onSuccess = { analysis ->
                        _currentAnalysis.value = analysis
                        _analysisState.value = AnalysisState.Results(analysis)
                        
                        // Auto-transition back to idle after brief delay if continuous analysis is enabled
                        if (_continuousAnalysis.value) {
                            delay(1000) // Show results for 1 second
                            _analysisState.value = AnalysisState.Idle
                        }
                    },
                    onFailure = { exception ->
                        val errorMessage = exception.message ?: "Unknown analysis error"
                        _analysisState.value = AnalysisState.Error(errorMessage)
                        
                        // Auto-clear error after 3 seconds if continuous analysis is enabled
                        if (_continuousAnalysis.value) {
                            delay(3000)
                            _analysisState.value = AnalysisState.Idle
                        }
                    }
                )
                
            } catch (e: Exception) {
                val errorMessage = "Analysis failed: ${e.message}"
                _analysisState.value = AnalysisState.Error(errorMessage)
                
                // Auto-clear error after 3 seconds if continuous analysis is enabled
                if (_continuousAnalysis.value) {
                    delay(3000)
                    _analysisState.value = AnalysisState.Idle
                }
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Capture photo and perform detailed analysis.
     * Requires context to be provided from the UI layer.
     */
    fun captureAndAnalyze(context: Context? = null) {
        if (_isAnalyzing.value) {
            return // Already analyzing
        }
        
        val captureInstance = imageCapture
        if (captureInstance == null || context == null) {
            // If no ImageCapture available, just trigger analysis state
            _analysisState.value = AnalysisState.Analyzing(0.0f, "Preparing capture...")
            return
        }
        
        viewModelScope.launch {
            try {
                _isAnalyzing.value = true
                _analysisState.value = AnalysisState.Analyzing(0.1f, "Capturing photo...")
                
                // Capture photo using CameraX
                capturePhoto(
                    imageCapture = captureInstance,
                    context = context,
                    onPhotoCaptured = { photoBytes ->
                        // Analyze the captured photo
                        viewModelScope.launch {
                            _analysisState.value = AnalysisState.Analyzing(0.5f, "Processing captured image...")
                            
                            val result = smartAIOrchestrator.analyzePhoto(photoBytes, currentWorkType)
                            
                            result.fold(
                                onSuccess = { analysis ->
                                    _currentAnalysis.value = analysis
                                    _analysisState.value = AnalysisState.Results(analysis)
                                },
                                onFailure = { exception ->
                                    val errorMessage = exception.message ?: "Photo analysis failed"
                                    _analysisState.value = AnalysisState.Error(errorMessage)
                                }
                            )
                            
                            _isAnalyzing.value = false
                        }
                    },
                    onError = { exception ->
                        viewModelScope.launch {
                            val errorMessage = "Photo capture failed: ${exception.message}"
                            _analysisState.value = AnalysisState.Error(errorMessage)
                            _isAnalyzing.value = false
                        }
                    }
                )
                
            } catch (e: Exception) {
                val errorMessage = "Capture failed: ${e.message}"
                _analysisState.value = AnalysisState.Error(errorMessage)
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Update the ImageCapture instance from CameraX.
     */
    fun updateImageCapture(imageCapture: ImageCapture) {
        this.imageCapture = imageCapture
    }

    /**
     * Set the work type for contextual analysis.
     */
    fun setWorkType(workType: WorkType) {
        currentWorkType = workType
        
        // Clear previous analysis when work type changes
        _currentAnalysis.value = null
        _analysisState.value = AnalysisState.Idle
    }

    /**
     * Toggle flash on/off.
     */
    fun toggleFlash() {
        val currentState = _cameraState.value
        _cameraState.value = currentState.copy(
            isFlashOn = !currentState.isFlashOn
        )
        
        // Apply flash setting to ImageCapture if available
        imageCapture?.flashMode = if (_cameraState.value.isFlashOn) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
    }

    /**
     * Toggle continuous analysis mode.
     */
    fun toggleContinuousAnalysis() {
        val newValue = !_continuousAnalysis.value
        _continuousAnalysis.value = newValue
        
        // Update camera state
        val currentState = _cameraState.value
        _cameraState.value = currentState.copy(
            isContinuousAnalysis = newValue
        )
        
        // If disabling continuous analysis, clear current state
        if (!newValue) {
            _analysisState.value = AnalysisState.Idle
            _isAnalyzing.value = false
        }
    }

    /**
     * Select a specific hazard from the current analysis.
     */
    fun selectHazard(hazard: Hazard) {
        selectedHazard = hazard
        // Could trigger additional detailed analysis or UI updates
    }

    /**
     * Retry the last analysis.
     */
    fun retryAnalysis() {
        _analysisState.value = AnalysisState.Idle
        _isAnalyzing.value = false
        
        // The UI should trigger a new analysis by calling analyzePhoto again
    }

    /**
     * Clear error state.
     */
    fun clearError() {
        if (_analysisState.value is AnalysisState.Error) {
            _analysisState.value = AnalysisState.Idle
        }
    }

    /**
     * Get current flash mode for UI display.
     */
    fun getFlashMode(): Int {
        return if (_cameraState.value.isFlashOn) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
    }

    /**
     * Update zoom level.
     */
    fun updateZoomLevel(zoomLevel: Float) {
        val currentState = _cameraState.value
        _cameraState.value = currentState.copy(
            zoomLevel = zoomLevel.coerceIn(1f, 10f)
        )
    }

    /**
     * Get performance statistics from the AI orchestrator.
     */
    fun getPerformanceStats(): Map<String, Any> {
        return mapOf(
            "totalAnalyses" to (lastAnalysisTime > 0),
            "currentWorkType" to currentWorkType.name,
            "continuousMode" to _continuousAnalysis.value,
            "flashEnabled" to _cameraState.value.isFlashOn,
            "aiOrchestratorStats" to smartAIOrchestrator.getStats()
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up any resources if needed
        imageCapture = null
        selectedHazard = null
    }
}

/**
 * Analysis state sealed class for UI state management.
 */
sealed class AnalysisState {
    object Idle : AnalysisState()
    data class Analyzing(val progress: Float, val currentTask: String) : AnalysisState()
    data class Results(val analysis: SafetyAnalysis) : AnalysisState()
    data class Error(val error: String) : AnalysisState()
}

/**
 * Camera state data class.
 */
data class CameraState(
    val isFlashOn: Boolean = false,
    val isContinuousAnalysis: Boolean = false,
    val zoomLevel: Float = 1f
)

/**
 * Mock SmartAIOrchestrator for development and testing.
 * TODO: Remove this when proper DI is configured.
 */
private class MockSmartAIOrchestrator : SmartAIOrchestrator(
    gemma3NE2B = MockGemma3NE2BVisionService(),
    vertexAI = MockVertexAIGeminiService(),
    yolo11 = MockYOLO11LocalService(),
    networkMonitor = MockNetworkConnectivityService(),
    performanceManager = MockAdaptivePerformanceManager(),
    memoryManager = MockMemoryManager(),
    performanceMonitor = MockPerformanceMonitor()
) {
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis> {
        // Simulate analysis time
        delay(1000 + (Math.random() * 2000).toLong())
        
        // Mock successful analysis
        return Result.success(createMockSafetyAnalysis(workType))
    }
    
    override fun getStats(): com.hazardhawk.ai.core.OrchestratorStats {
        return com.hazardhawk.ai.core.OrchestratorStats()
    }
    
    override suspend fun configure(apiKey: String?): Result<Unit> {
        return Result.success(Unit)
    }
}

// Mock implementations for development - these would be removed in production
private class MockGemma3NE2BVisionService : com.hazardhawk.ai.services.Gemma3NE2BVisionService()
private class MockVertexAIGeminiService : com.hazardhawk.ai.services.VertexAIGeminiService()  
private class MockYOLO11LocalService : com.hazardhawk.ai.services.YOLO11LocalService()
private class MockNetworkConnectivityService : com.hazardhawk.ai.core.NetworkConnectivityService {
    override val isConnected = true
    override val connectionQuality = com.hazardhawk.ai.core.ConnectionQuality.GOOD
}
private class MockAdaptivePerformanceManager : com.hazardhawk.performance.AdaptivePerformanceManager()
private class MockMemoryManager : com.hazardhawk.performance.MemoryManager()
private class MockPerformanceMonitor : com.hazardhawk.performance.PerformanceMonitor()

private fun createMockSafetyAnalysis(workType: WorkType): SafetyAnalysis {
    // Create a basic mock safety analysis for demonstration
    return SafetyAnalysis(
        id = "mock-analysis-${System.currentTimeMillis()}",
        workType = workType,
        analysisType = com.hazardhawk.core.models.AnalysisType.LOCAL_GEMMA_MULTIMODAL,
        hazards = emptyList(), // No hazards for mock
        recommendations = listOf("Mock analysis complete"),
        confidence = 0.85f,
        processingTimeMs = 1500L,
        metadata = null
    )
}