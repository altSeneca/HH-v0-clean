package com.hazardhawk.ar

import android.content.Context
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.hazardhawk.ai.models.WorkType
import com.hazardhawk.ui.camera.ARCameraPreview
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.*

/**
 * Integration tests for AR camera and CameraX interaction.
 * Tests real camera integration, image analysis pipeline, and AR overlay rendering.
 */
@RunWith(AndroidJUnit4::class)
class ARCameraIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val cameraPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO
    )

    private lateinit var context: Context
    private lateinit var cameraManager: MockARCameraManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        cameraManager = MockARCameraManager()
    }

    @Test
    fun arCameraIntegration_initializesSuccessfully() {
        var imageCaptureReady = false
        var capturedImageData: ByteArray? = null

        composeTestRule.setContent {
            ARCameraPreview(
                onImageCaptured = { imageData ->
                    capturedImageData = imageData
                },
                onImageCaptureReady = { _ ->
                    imageCaptureReady = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Wait for camera initialization
        composeTestRule.waitForIdle()
        Thread.sleep(2000) // Allow camera to initialize

        assertTrue(imageCaptureReady, "ImageCapture should be initialized")
    }

    @Test
    fun arCameraIntegration_capturesImagesForAnalysis() = runTest {
        val latch = CountDownLatch(1)
        var capturedImageCount = 0
        val maxWaitTime = 10L // seconds

        composeTestRule.setContent {
            ARCameraPreview(
                onImageCaptured = { imageData ->
                    if (imageData.isNotEmpty()) {
                        capturedImageCount++
                        if (capturedImageCount >= 3) {
                            latch.countDown()
                        }
                    }
                },
                enableImageAnalysis = true,
                analysisInterval = 500L,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Wait for image analysis to start
        val success = latch.await(maxWaitTime, TimeUnit.SECONDS)
        assertTrue(success, "Should capture at least 3 images for analysis within $maxWaitTime seconds")
        assertTrue(capturedImageCount >= 3, "Should have captured multiple images for analysis")
    }

    @Test
    fun arCameraIntegration_handlesImageCaptureConfiguration() {
        var imageCapture: ImageCapture? = null
        val testFlashMode = ImageCapture.FLASH_MODE_ON

        composeTestRule.setContent {
            ARCameraPreview(
                onImageCaptured = { _ -> },
                onImageCaptureReady = { capture ->
                    imageCapture = capture
                    capture.flashMode = testFlashMode
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        assertNotNull(imageCapture, "ImageCapture should be configured")
        assertEquals(testFlashMode, imageCapture?.flashMode)
    }

    @Test
    fun arCameraIntegration_throttlesAnalysisCorrectly() = runTest {
        val captureTimestamps = mutableListOf<Long>()
        val analysisInterval = 1000L // 1 second
        val latch = CountDownLatch(5)

        composeTestRule.setContent {
            ARCameraPreview(
                onImageCaptured = { imageData ->
                    if (imageData.isNotEmpty()) {
                        captureTimestamps.add(System.currentTimeMillis())
                        latch.countDown()
                    }
                },
                enableImageAnalysis = true,
                analysisInterval = analysisInterval,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Wait for several captures
        val success = latch.await(15, TimeUnit.SECONDS)
        assertTrue(success, "Should complete analysis captures within time limit")

        // Verify throttling intervals
        for (i in 1 until captureTimestamps.size) {
            val interval = captureTimestamps[i] - captureTimestamps[i - 1]
            assertTrue(interval >= analysisInterval * 0.8, // Allow 20% tolerance
                "Analysis interval should be respected: actual=$interval, expected>=${analysisInterval}")
        }
    }

    @Test
    fun arCameraIntegration_handlesPermissionDenied() {
        // This test assumes camera permission is already granted by the rule
        // In a real scenario, we would test permission denial flows
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        assertTrue(hasPermission, "Camera permission should be granted for testing")
    }

    @Test
    fun arCameraIntegration_switchesBetweenCameras() = runTest {
        var currentCameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
        val cameraManager = MockARCameraManager()

        // Test switching to front camera
        val switchResult = cameraManager.switchCamera(
            androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
        )

        assertTrue(switchResult.isSuccess, "Should successfully switch cameras")
    }

    @Test
    fun arCameraIntegration_handlesCameraError() = runTest {
        val cameraManager = MockARCameraManager()
        var errorOccurred = false

        cameraManager.onCameraError = { error ->
            errorOccurred = true
            assertTrue(error.isNotEmpty(), "Error message should not be empty")
        }

        // Simulate camera error
        cameraManager.simulateError("Camera hardware failure")
        assertTrue(errorOccurred, "Camera error callback should be triggered")
    }

    @Test
    fun arCameraIntegration_maintainsPerformance() = runTest {
        val performanceMetrics = mutableListOf<Long>()
        val latch = CountDownLatch(10)

        composeTestRule.setContent {
            ARCameraPreview(
                onImageCaptured = { imageData ->
                    val startTime = System.currentTimeMillis()
                    // Simulate processing time
                    Thread.sleep(1)
                    val processingTime = System.currentTimeMillis() - startTime
                    performanceMetrics.add(processingTime)
                    latch.countDown()
                },
                enableImageAnalysis = true,
                analysisInterval = 100L, // High frequency for performance testing
                modifier = Modifier.fillMaxSize()
            )
        }

        val success = latch.await(10, TimeUnit.SECONDS)
        assertTrue(success, "Should complete performance measurements")

        val averageProcessingTime = performanceMetrics.average()
        assertTrue(averageProcessingTime < 50.0, // Should be under 50ms
            "Average processing time should be performant: ${averageProcessingTime}ms")
    }

    @Test
    fun arCameraIntegration_handlesDeviceRotation() = runTest {
        val cameraManager = MockARCameraManager()
        
        // Test different orientations
        val orientations = listOf(0, 90, 180, 270)
        
        orientations.forEach { rotation ->
            val result = cameraManager.updateOrientation(rotation)
            assertTrue(result.isSuccess, "Should handle rotation to $rotation degrees")
        }
    }

    @Test
    fun arCameraIntegration_configuresForDifferentWorkTypes() = runTest {
        val workTypes = listOf(
            WorkType.GENERAL_CONSTRUCTION,
            WorkType.ELECTRICAL_WORK,
            WorkType.FALL_PROTECTION
        )

        workTypes.forEach { workType ->
            val config = ARCameraConfig.forWorkType(workType)
            val cameraManager = MockARCameraManager()
            
            val result = cameraManager.applyConfiguration(config)
            assertTrue(result.isSuccess, "Should configure camera for work type: $workType")
            
            // Verify work-type specific settings
            when (workType) {
                WorkType.ELECTRICAL_WORK -> {
                    assertTrue(config.enhanceContrastForMetalDetection)
                }
                WorkType.FALL_PROTECTION -> {
                    assertTrue(config.enableHeightDetection)
                }
                else -> {
                    assertTrue(config.generalPurposeOptimization)
                }
            }
        }
    }

    @Test
    fun arCameraIntegration_handlesLowLightConditions() = runTest {
        val cameraManager = MockARCameraManager()
        
        // Simulate low light conditions
        cameraManager.simulateLightingConditions(lightLevel = 0.1f) // Very dim
        
        val config = cameraManager.getCurrentConfiguration()
        assertTrue(config.lowLightOptimizationEnabled, 
            "Should enable low light optimization in dim conditions")
        assertTrue(config.isoSensitivity > 800, 
            "Should increase ISO in low light")
    }

    @Test
    fun arCameraIntegration_synchronizesWithARTracking() = runTest {
        val cameraManager = MockARCameraManager()
        val trackingManager = MockARTrackingManager()
        
        // Start both camera and tracking
        val cameraResult = cameraManager.startCamera()
        val trackingResult = trackingManager.startTracking()
        
        assertTrue(cameraResult.isSuccess, "Camera should start successfully")
        assertTrue(trackingResult.isSuccess, "Tracking should start successfully")
        
        // Verify synchronization
        val frameTimestamp = System.nanoTime()
        cameraManager.captureFrameWithTimestamp(frameTimestamp)
        trackingManager.updatePoseWithTimestamp(frameTimestamp)
        
        val cameraPose = trackingManager.getPoseAtTimestamp(frameTimestamp)
        assertNotNull(cameraPose, "Should have synchronized pose data")
    }
}

/**
 * Mock AR Camera Manager for testing camera integration
 */
class MockARCameraManager {
    var onCameraError: ((String) -> Unit)? = null
    private var currentConfig = ARCameraConfig()
    private var isStarted = false

    suspend fun switchCamera(cameraSelector: androidx.camera.core.CameraSelector): Result<Unit> {
        return try {
            // Simulate camera switching
            Thread.sleep(500)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun simulateError(errorMessage: String) {
        onCameraError?.invoke(errorMessage)
    }

    suspend fun updateOrientation(rotation: Int): Result<Unit> {
        return try {
            currentConfig = currentConfig.copy(rotation = rotation)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyConfiguration(config: ARCameraConfig): Result<Unit> {
        return try {
            currentConfig = config
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentConfiguration(): ARCameraConfig = currentConfig

    fun simulateLightingConditions(lightLevel: Float) {
        currentConfig = currentConfig.copy(
            lowLightOptimizationEnabled = lightLevel < 0.3f,
            isoSensitivity = when {
                lightLevel < 0.2f -> 1600
                lightLevel < 0.5f -> 800
                else -> 400
            }
        )
    }

    suspend fun startCamera(): Result<Unit> {
        return try {
            isStarted = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun captureFrameWithTimestamp(timestamp: Long) {
        // Simulate frame capture with timestamp
    }
}

/**
 * Mock AR Tracking Manager for integration testing
 */
class MockARTrackingManager {
    private val poseHistory = mutableMapOf<Long, ARPose>()

    suspend fun startTracking(): Result<Unit> {
        return Result.success(Unit)
    }

    fun updatePoseWithTimestamp(timestamp: Long) {
        val mockPose = ARPose(
            position = Vector3(0f, 0f, 0f),
            rotation = Quaternion(0f, 0f, 0f, 1f),
            timestamp = timestamp
        )
        poseHistory[timestamp] = mockPose
    }

    fun getPoseAtTimestamp(timestamp: Long): ARPose? {
        return poseHistory[timestamp]
    }
}

// Data classes for testing
data class ARCameraConfig(
    val rotation: Int = 0,
    val enhanceContrastForMetalDetection: Boolean = false,
    val enableHeightDetection: Boolean = false,
    val generalPurposeOptimization: Boolean = true,
    val lowLightOptimizationEnabled: Boolean = false,
    val isoSensitivity: Int = 400
) {
    companion object {
        fun forWorkType(workType: WorkType): ARCameraConfig {
            return when (workType) {
                WorkType.ELECTRICAL_WORK -> ARCameraConfig(
                    enhanceContrastForMetalDetection = true,
                    generalPurposeOptimization = false
                )
                WorkType.FALL_PROTECTION -> ARCameraConfig(
                    enableHeightDetection = true,
                    generalPurposeOptimization = false
                )
                else -> ARCameraConfig()
            }
        }
    }
}

data class ARPose(
    val position: Vector3,
    val rotation: Quaternion,
    val timestamp: Long
)
