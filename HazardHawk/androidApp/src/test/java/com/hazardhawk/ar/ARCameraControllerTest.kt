package com.hazardhawk.ar

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.ai.models.WorkType
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for AR camera controller focusing on session management and error handling.
 * Tests camera lifecycle, configuration, and integration with CameraX APIs.
 */
@RunWith(AndroidJUnit4::class)
class ARCameraControllerTest {

    @Mock
    private lateinit var mockImageCapture: ImageCapture
    
    @Mock
    private lateinit var mockImageProxy: ImageProxy
    
    private lateinit var cameraController: ARCameraController

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        cameraController = ARCameraController()
    }

    @Test
    fun arCameraController_initializesWithDefaultConfiguration() {
        // Given
        val controller = ARCameraController()
        
        // When
        val config = controller.getCurrentConfiguration()
        
        // Then
        assertEquals(500L, config.analysisInterval)
        assertEquals(AspectRatio.RATIO_16_9, config.targetAspectRatio)
        assertTrue(config.enableStabilization)
        assertEquals(ImageCapture.FLASH_MODE_AUTO, config.flashMode)
    }

    @Test
    fun arCameraController_startsSessionSuccessfully() = runTest {
        // Given
        val sessionConfig = ARSessionConfig(
            workType = WorkType.GENERAL_CONSTRUCTION,
            enableRealTimeAnalysis = true,
            analysisInterval = 500L
        )
        
        // When
        val result = cameraController.startSession(sessionConfig)
        
        // Then
        assertTrue(result.isSuccess)
        assertTrue(cameraController.isSessionActive())
        assertEquals(WorkType.GENERAL_CONSTRUCTION, cameraController.getCurrentWorkType())
    }

    @Test
    fun arCameraController_handlesSessionStartFailure() = runTest {
        // Given
        val invalidConfig = ARSessionConfig(
            workType = WorkType.GENERAL_CONSTRUCTION,
            enableRealTimeAnalysis = true,
            analysisInterval = -1L // Invalid interval
        )
        
        // When
        val result = cameraController.startSession(invalidConfig)
        
        // Then
        assertTrue(result.isFailure)
        assertFalse(cameraController.isSessionActive())
        assertEquals("Invalid analysis interval: -1", result.exceptionOrNull()?.message)
    }

    @Test
    fun arCameraController_stopsSessionProperly() = runTest {
        // Given
        val sessionConfig = ARSessionConfig(
            workType = WorkType.ELECTRICAL_WORK,
            enableRealTimeAnalysis = false
        )
        cameraController.startSession(sessionConfig)
        assertTrue(cameraController.isSessionActive())
        
        // When
        cameraController.stopSession()
        
        // Then
        assertFalse(cameraController.isSessionActive())
        assertEquals(WorkType.GENERAL_CONSTRUCTION, cameraController.getCurrentWorkType()) // Reset to default
    }

    @Test
    fun arCameraController_handlesMultipleStartCallsGracefully() = runTest {
        // Given
        val config1 = ARSessionConfig(workType = WorkType.GENERAL_CONSTRUCTION)
        val config2 = ARSessionConfig(workType = WorkType.ELECTRICAL_WORK)
        
        // When
        val result1 = cameraController.startSession(config1)
        val result2 = cameraController.startSession(config2)
        
        // Then
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertEquals(WorkType.ELECTRICAL_WORK, cameraController.getCurrentWorkType()) // Latest config
        assertTrue(cameraController.isSessionActive())
    }

    @Test
    fun arCameraController_configuresImageCapture() = runTest {
        // Given
        val config = ARSessionConfig(
            targetAspectRatio = AspectRatio.RATIO_4_3,
            flashMode = ImageCapture.FLASH_MODE_ON
        )
        
        // When
        cameraController.startSession(config)
        cameraController.configureImageCapture(mockImageCapture)
        
        // Then
        verify(mockImageCapture).flashMode = ImageCapture.FLASH_MODE_ON
        assertEquals(mockImageCapture, cameraController.getImageCapture())
    }

    @Test
    fun arCameraController_handlesImageCaptureFailure() = runTest {
        // Given
        whenever(mockImageCapture.flashMode = any()).thenThrow(RuntimeException("Camera hardware error"))
        
        // When
        val result = cameraController.configureImageCapture(mockImageCapture)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Camera hardware error", result.exceptionOrNull()?.message)
    }

    @Test
    fun arCameraController_updatesAnalysisInterval() = runTest {
        // Given
        cameraController.startSession(ARSessionConfig())
        
        // When
        val result = cameraController.updateAnalysisInterval(1000L)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(1000L, cameraController.getCurrentConfiguration().analysisInterval)
    }

    @Test
    fun arCameraController_rejectsInvalidAnalysisInterval() = runTest {
        // Given
        cameraController.startSession(ARSessionConfig())
        
        // When & Then
        val resultNegative = cameraController.updateAnalysisInterval(-100L)
        assertTrue(resultNegative.isFailure)
        
        val resultTooSmall = cameraController.updateAnalysisInterval(50L)
        assertTrue(resultTooSmall.isFailure)
        
        val resultTooLarge = cameraController.updateAnalysisInterval(10000L)
        assertTrue(resultTooLarge.isFailure)
    }

    @Test
    fun arCameraController_switchesWorkTypeCorrectly() = runTest {
        // Given
        cameraController.startSession(ARSessionConfig(workType = WorkType.GENERAL_CONSTRUCTION))
        
        // When
        val result = cameraController.switchWorkType(WorkType.FALL_PROTECTION)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(WorkType.FALL_PROTECTION, cameraController.getCurrentWorkType())
    }

    @Test
    fun arCameraController_handlesWorkTypeSwitchWhileAnalyzing() = runTest {
        // Given
        cameraController.startSession(ARSessionConfig())
        cameraController.setAnalysisInProgress(true)
        
        // When
        val result = cameraController.switchWorkType(WorkType.ELECTRICAL_WORK)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Cannot switch work type while analysis is in progress", result.exceptionOrNull()?.message)
        assertEquals(WorkType.GENERAL_CONSTRUCTION, cameraController.getCurrentWorkType()) // Unchanged
    }

    @Test
    fun arCameraController_tracksPerformanceMetrics() = runTest {
        // Given
        cameraController.startSession(ARSessionConfig())
        
        // When
        repeat(5) {
            cameraController.recordFrameProcessed(16.67f) // 60 FPS target
        }
        cameraController.recordAnalysisCompleted(1500L)
        cameraController.recordAnalysisCompleted(2000L)
        
        // Then
        val metrics = cameraController.getPerformanceMetrics()
        assertEquals(5, metrics.totalFramesProcessed)
        assertEquals(2, metrics.totalAnalysisCompleted)
        assertEquals(16.67f, metrics.averageFrameTime, 0.1f)
        assertEquals(1750.0, metrics.averageAnalysisTime, 0.1)
    }

    @Test
    fun arCameraController_detectsMemoryPressure() = runTest {
        // Given
        cameraController.startSession(ARSessionConfig())
        
        // When
        cameraController.reportMemoryUsage(800_000_000L) // 800MB - high pressure
        
        // Then
        val metrics = cameraController.getPerformanceMetrics()
        assertTrue(metrics.memoryPressureDetected)
        assertEquals(MemoryPressureLevel.HIGH, metrics.memoryPressureLevel)
    }

    @Test
    fun arCameraController_adjustsQualityBasedOnPerformance() = runTest {
        // Given
        cameraController.startSession(ARSessionConfig())
        
        // When - Simulate poor performance
        repeat(10) {
            cameraController.recordFrameProcessed(33.33f) // 30 FPS - below target
        }
        
        // Then
        val config = cameraController.getCurrentConfiguration()
        assertTrue(config.adaptiveQualityEnabled)
        assertEquals(QualityMode.PERFORMANCE, config.currentQualityMode)
    }

    @Test
    fun arCameraController_cleansUpResourcesOnStop() = runTest {
        // Given
        cameraController.startSession(ARSessionConfig())
        cameraController.configureImageCapture(mockImageCapture)
        
        // When
        cameraController.stopSession()
        
        // Then
        assertFalse(cameraController.isSessionActive())
        assertEquals(null, cameraController.getImageCapture())
        
        val metrics = cameraController.getPerformanceMetrics()
        assertEquals(0, metrics.totalFramesProcessed)
        assertEquals(0, metrics.totalAnalysisCompleted)
    }
}

/**
 * Mock AR Camera Controller for testing
 */
class ARCameraController {
    private var sessionActive = false
    private var currentWorkType = WorkType.GENERAL_CONSTRUCTION
    private var currentConfig = ARCameraConfig()
    private var imageCapture: ImageCapture? = null
    private var analysisInProgress = false
    private var performanceMetrics = ARPerformanceMetrics()

    suspend fun startSession(config: ARSessionConfig): Result<Unit> {
        return try {
            if (config.analysisInterval < 100L) {
                throw IllegalArgumentException("Invalid analysis interval: ${config.analysisInterval}")
            }
            
            sessionActive = true
            currentWorkType = config.workType
            currentConfig = currentConfig.copy(
                analysisInterval = config.analysisInterval,
                targetAspectRatio = config.targetAspectRatio,
                flashMode = config.flashMode
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopSession() {
        sessionActive = false
        currentWorkType = WorkType.GENERAL_CONSTRUCTION
        imageCapture = null
        analysisInProgress = false
        performanceMetrics = ARPerformanceMetrics()
    }

    fun isSessionActive(): Boolean = sessionActive

    fun getCurrentWorkType(): WorkType = currentWorkType

    fun getCurrentConfiguration(): ARCameraConfig = currentConfig

    fun configureImageCapture(capture: ImageCapture): Result<Unit> {
        return try {
            capture.flashMode = currentConfig.flashMode
            imageCapture = capture
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getImageCapture(): ImageCapture? = imageCapture

    suspend fun updateAnalysisInterval(interval: Long): Result<Unit> {
        return if (interval in 100L..5000L) {
            currentConfig = currentConfig.copy(analysisInterval = interval)
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Analysis interval must be between 100-5000ms"))
        }
    }

    suspend fun switchWorkType(workType: WorkType): Result<Unit> {
        return if (analysisInProgress) {
            Result.failure(IllegalStateException("Cannot switch work type while analysis is in progress"))
        } else {
            currentWorkType = workType
            Result.success(Unit)
        }
    }

    fun setAnalysisInProgress(inProgress: Boolean) {
        analysisInProgress = inProgress
    }

    fun recordFrameProcessed(frameTime: Float) {
        performanceMetrics = performanceMetrics.copy(
            totalFramesProcessed = performanceMetrics.totalFramesProcessed + 1,
            frameTimes = performanceMetrics.frameTimes + frameTime
        )
        
        // Auto-adjust quality based on performance
        val avgFrameTime = performanceMetrics.frameTimes.average().toFloat()
        if (avgFrameTime > 20f) { // Slower than 50 FPS
            currentConfig = currentConfig.copy(
                adaptiveQualityEnabled = true,
                currentQualityMode = QualityMode.PERFORMANCE
            )
        }
    }

    fun recordAnalysisCompleted(analysisTime: Long) {
        performanceMetrics = performanceMetrics.copy(
            totalAnalysisCompleted = performanceMetrics.totalAnalysisCompleted + 1,
            analysisTimes = performanceMetrics.analysisTimes + analysisTime
        )
    }

    fun reportMemoryUsage(memoryBytes: Long) {
        val pressureLevel = when {
            memoryBytes > 700_000_000L -> MemoryPressureLevel.HIGH
            memoryBytes > 500_000_000L -> MemoryPressureLevel.MEDIUM
            else -> MemoryPressureLevel.LOW
        }
        
        performanceMetrics = performanceMetrics.copy(
            memoryPressureDetected = pressureLevel != MemoryPressureLevel.LOW,
            memoryPressureLevel = pressureLevel
        )
    }

    fun getPerformanceMetrics(): ARPerformanceMetrics = performanceMetrics.copy(
        averageFrameTime = if (performanceMetrics.frameTimes.isNotEmpty()) {
            performanceMetrics.frameTimes.average().toFloat()
        } else 0f,
        averageAnalysisTime = if (performanceMetrics.analysisTimes.isNotEmpty()) {
            performanceMetrics.analysisTimes.average()
        } else 0.0
    )
}

// Data classes for testing
data class ARSessionConfig(
    val workType: WorkType = WorkType.GENERAL_CONSTRUCTION,
    val enableRealTimeAnalysis: Boolean = true,
    val analysisInterval: Long = 500L,
    val targetAspectRatio: Int = AspectRatio.RATIO_16_9,
    val flashMode: Int = ImageCapture.FLASH_MODE_AUTO
)

data class ARCameraConfig(
    val analysisInterval: Long = 500L,
    val targetAspectRatio: Int = AspectRatio.RATIO_16_9,
    val enableStabilization: Boolean = true,
    val flashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    val adaptiveQualityEnabled: Boolean = false,
    val currentQualityMode: QualityMode = QualityMode.QUALITY
)

data class ARPerformanceMetrics(
    val totalFramesProcessed: Int = 0,
    val totalAnalysisCompleted: Int = 0,
    val frameTimes: List<Float> = emptyList(),
    val analysisTimes: List<Long> = emptyList(),
    val averageFrameTime: Float = 0f,
    val averageAnalysisTime: Double = 0.0,
    val memoryPressureDetected: Boolean = false,
    val memoryPressureLevel: MemoryPressureLevel = MemoryPressureLevel.LOW
)

enum class QualityMode { QUALITY, BALANCED, PERFORMANCE }
enum class MemoryPressureLevel { LOW, MEDIUM, HIGH }

// Mock AspectRatio constants
object AspectRatio {
    const val RATIO_16_9 = 1
    const val RATIO_4_3 = 2
}
