package com.hazardhawk.ar

import android.content.Context
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.LifecycleOwner
import com.google.ar.core.*
import com.google.ar.core.exceptions.*
import com.hazardhawk.domain.entities.WorkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AR camera controller that integrates ARCore with CameraX for safety monitoring.
 * Provides session management, tracking, and hazard detection capabilities.
 */
class ARCameraController(
    private val context: Context? = null
) {

    // AR session management
    private var arSession: Session? = null
    private var isSessionRunning = false

    // Camera integration
    private var cameraController: LifecycleCameraController? = null

    // Configuration
    private var currentConfig = ARCameraConfiguration()

    // State flows
    private val _sessionState = MutableStateFlow<ARSessionState>(ARSessionState.Stopped)
    val sessionState: StateFlow<ARSessionState> = _sessionState.asStateFlow()

    private val _trackingState = MutableStateFlow<ARTrackingState>(ARTrackingState.NotTracking)
    val trackingState: StateFlow<ARTrackingState> = _trackingState.asStateFlow()

    /**
     * Initialize AR session with given configuration.
     */
    suspend fun initializeSession(context: Context, config: ARCameraConfiguration = ARCameraConfiguration()): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("ARCameraController", "Starting AR session initialization...")
                _sessionState.value = ARSessionState.Initializing

                // Check AR capabilities first
                android.util.Log.d("ARCameraController", "Checking AR capabilities...")
                val capabilities = ARCapabilityChecker.getARCapabilities(context)
                android.util.Log.d("ARCameraController", "AR capabilities: ${capabilities.statusDescription}")

                if (!capabilities.isSupported) {
                    val errorMsg = "AR not supported on this device: ${capabilities.statusDescription}"
                    android.util.Log.e("ARCameraController", errorMsg)
                    _sessionState.value = ARSessionState.Error(errorMsg)
                    return@withContext false
                }

                // Special handling for emulator
                val isEmulator = android.os.Build.FINGERPRINT.contains("generic") ||
                                android.os.Build.MODEL.contains("Emulator") ||
                                android.os.Build.MODEL.contains("Android SDK")

                if (isEmulator) {
                    android.util.Log.w("ARCameraController", "Running on emulator - using fallback AR mode")
                    _sessionState.value = ARSessionState.Ready
                    currentConfig = config
                    return@withContext true
                }

                // Create AR session
                android.util.Log.d("ARCameraController", "Creating AR session...")
                val session = Session(context)
                val arConfig = Config(session).apply {
                    focusMode = Config.FocusMode.AUTO
                    planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                }

                android.util.Log.d("ARCameraController", "Configuring AR session...")
                session.configure(arConfig)
                arSession = session
                currentConfig = config

                _sessionState.value = ARSessionState.Ready
                android.util.Log.d("ARCameraController", "AR session initialized successfully")
                true

            } catch (e: UnavailableException) {
                val errorMsg = when (e) {
                    is UnavailableArcoreNotInstalledException -> "ARCore not installed"
                    is UnavailableApkTooOldException -> "ARCore APK too old"
                    is UnavailableDeviceNotCompatibleException -> "Device not compatible with ARCore"
                    is UnavailableSdkTooOldException -> "SDK too old for ARCore"
                    is UnavailableUserDeclinedInstallationException -> "User declined ARCore installation"
                    else -> "ARCore unavailable: ${e.message}"
                }
                android.util.Log.e("ARCameraController", errorMsg, e)
                _sessionState.value = ARSessionState.Error(errorMsg)
                false
            } catch (e: SecurityException) {
                val errorMsg = "Camera permission denied for AR"
                android.util.Log.e("ARCameraController", errorMsg, e)
                _sessionState.value = ARSessionState.Error(errorMsg)
                false
            } catch (e: Exception) {
                val errorMsg = "Failed to initialize AR: ${e.message}"
                android.util.Log.e("ARCameraController", errorMsg, e)
                _sessionState.value = ARSessionState.Error(errorMsg)
                false
            }
        }
    }

    /**
     * Start AR session and tracking.
     */
    fun startSession() {
        try {
            val session = arSession ?: throw IllegalStateException("AR session not initialized")
            session.resume()
            isSessionRunning = true
            _sessionState.value = ARSessionState.Running
            _trackingState.value = ARTrackingState.Initializing
            android.util.Log.d("ARCameraController", "AR session started")
        } catch (e: Exception) {
            android.util.Log.e("ARCameraController", "Failed to start AR session", e)
            _sessionState.value = ARSessionState.Error("Failed to start AR session: ${e.message}")
        }
    }

    /**
     * Pause AR session.
     */
    fun pauseSession() {
        try {
            arSession?.pause()
            isSessionRunning = false
            _sessionState.value = ARSessionState.Paused
            _trackingState.value = ARTrackingState.NotTracking
            android.util.Log.d("ARCameraController", "AR session paused")
        } catch (e: Exception) {
            android.util.Log.e("ARCameraController", "Failed to pause AR session", e)
        }
    }

    /**
     * Stop and cleanup AR session.
     */
    fun stopSession() {
        try {
            arSession?.close()
            arSession = null
            isSessionRunning = false
            _sessionState.value = ARSessionState.Stopped
            _trackingState.value = ARTrackingState.NotTracking
            android.util.Log.d("ARCameraController", "AR session stopped")
        } catch (e: Exception) {
            android.util.Log.e("ARCameraController", "Failed to stop AR session", e)
        }
    }

    /**
     * Update AR frame and tracking state.
     */
    fun updateFrame(): Frame? {
        if (!isSessionRunning) return null

        return try {
            val session = arSession ?: return null
            val frame = session.update()

            // Update tracking state
            val camera = frame.camera
            _trackingState.value = when (camera.trackingState) {
                TrackingState.TRACKING -> ARTrackingState.Tracking
                TrackingState.PAUSED -> ARTrackingState.Paused("Tracking paused")
                TrackingState.STOPPED -> ARTrackingState.Stopped("Tracking stopped")
                else -> ARTrackingState.NotTracking
            }

            frame
        } catch (e: Exception) {
            android.util.Log.w("ARCameraController", "Error updating AR frame", e)
            null
        }
    }

    /**
     * Get current AR configuration.
     */
    fun getCurrentConfiguration(): ARCameraConfiguration = currentConfig

    /**
     * Integrate with CameraX controller.
     */
    fun bindToCamera(cameraController: LifecycleCameraController) {
        this.cameraController = cameraController
        // Configure CameraX for AR compatibility
        // Note: Camera configuration is handled through the CameraX lifecycle
        android.util.Log.d("ARCameraController", "Bound to CameraX controller")
    }

    /**
     * Analyze image frame for hazards (integrates with existing AI pipeline).
     */
    fun analyzeFrame(imageProxy: ImageProxy, workType: WorkType) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convert ImageProxy to byte array for AI analysis
                val buffer = imageProxy.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                // Here we would integrate with the existing SmartAIOrchestrator
                // For now, just log the analysis request
                android.util.Log.d("ARCameraController", "Analyzing frame for work type: $workType")

            } catch (e: Exception) {
                android.util.Log.e("ARCameraController", "Error analyzing frame", e)
            } finally {
                imageProxy.close()
            }
        }
    }
}

/**
 * AR camera configuration options.
 */
data class ARCameraConfiguration(
    val analysisInterval: Long = 500L, // 2 FPS for AR analysis
    val targetAspectRatio: Int = AspectRatio.RATIO_16_9,
    val enableStabilization: Boolean = true,
    val enablePlaneDetection: Boolean = true,
    val enableLightEstimation: Boolean = true
)

/**
 * AR session state.
 */
sealed class ARSessionState {
    object Stopped : ARSessionState()
    object Initializing : ARSessionState()
    object Ready : ARSessionState()
    object Running : ARSessionState()
    object Paused : ARSessionState()
    data class Error(val message: String) : ARSessionState()
}

/**
 * AR tracking state.
 */
sealed class ARTrackingState {
    object NotTracking : ARTrackingState()
    object Initializing : ARTrackingState()
    object Tracking : ARTrackingState()
    data class Paused(val reason: String) : ARTrackingState()
    data class Stopped(val reason: String) : ARTrackingState()
}