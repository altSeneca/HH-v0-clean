package com.hazardhawk.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaActionSound
import android.os.Environment
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.camera.LocationService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * Enhanced camera modes for professional construction documentation
 */
enum class CameraMode {
    SINGLE_SHOT,
    BURST_MODE,
    TIMER_MODE,
    HDR_MODE,
    VOICE_ACTIVATED
}

/**
 * Flash modes for construction environments
 */
enum class FlashMode {
    AUTO,
    ON,
    OFF
}

/**
 * Zoom control configuration
 */
data class ZoomState(
    val currentZoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 8.0f,
    val isZooming: Boolean = false
)

/**
 * Level indicator state for straight shots
 */
data class LevelState(
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val isLevel: Boolean = false,
    val threshold: Float = 2.0f // degrees
)

/**
 * Professional camera UI state
 */
data class CameraUIState(
    val mode: CameraMode = CameraMode.SINGLE_SHOT,
    val flashMode: FlashMode = FlashMode.AUTO,
    val isHDREnabled: Boolean = false,
    val showGrid: Boolean = true,
    val showLevel: Boolean = true,
    val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    val zoomState: ZoomState = ZoomState(),
    val levelState: LevelState = LevelState(),
    val isCapturing: Boolean = false,
    val captureCount: Int = 0,
    val burstCount: Int = 3,
    val timerSeconds: Int = 3,
    val timerCountdown: Int = 0,
    val isVoiceEnabled: Boolean = false,
    val lastPhotoUri: String? = null,
    val availableStorageGB: Float = 0f,
    val message: String = "",
    val isError: Boolean = false
)

/**
 * Professional camera ViewModel with construction worker-friendly features
 */
class CameraViewModel(
    private val context: Context,
    private val locationService: LocationService
) : ViewModel(), SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val soundEffects = MediaActionSound()
    
    private val _uiState = MutableStateFlow(CameraUIState())
    val uiState: StateFlow<CameraUIState> = _uiState.asStateFlow()
    
    private val _focusPoint = MutableStateFlow<Pair<Float, Float>?>(null)
    val focusPoint: StateFlow<Pair<Float, Float>?> = _focusPoint.asStateFlow()
    
    // Sensor data for level calculation
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    
    private var timerJob: Job? = null
    private var burstJob: Job? = null
    
    init {
        // Initialize sound effects
        soundEffects.load(MediaActionSound.SHUTTER_CLICK)
        soundEffects.load(MediaActionSound.START_VIDEO_RECORDING)
        
        // Start level monitoring
        startLevelMonitoring()
        
        // Update storage info
        updateStorageInfo()
        
        // Start location updates if available
        if (locationService.hasLocationPermissions()) {
            locationService.startLocationUpdates()
        }
    }
    
    /**
     * Toggle between front and back camera
     */
    fun switchCamera() {
        val newSelector = if (_uiState.value.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        
        _uiState.value = _uiState.value.copy(
            cameraSelector = newSelector,
            zoomState = ZoomState() // Reset zoom when switching cameras
        )
    }
    
    /**
     * Cycle through flash modes
     */
    fun toggleFlashMode() {
        val newFlashMode = when (_uiState.value.flashMode) {
            FlashMode.AUTO -> FlashMode.ON
            FlashMode.ON -> FlashMode.OFF
            FlashMode.OFF -> FlashMode.AUTO
        }
        
        _uiState.value = _uiState.value.copy(flashMode = newFlashMode)
    }
    
    /**
     * Toggle HDR mode
     */
    fun toggleHDR() {
        _uiState.value = _uiState.value.copy(isHDREnabled = !_uiState.value.isHDREnabled)
    }
    
    /**
     * Toggle grid overlay
     */
    fun toggleGrid() {
        _uiState.value = _uiState.value.copy(showGrid = !_uiState.value.showGrid)
    }
    
    /**
     * Toggle level indicator
     */
    fun toggleLevel() {
        _uiState.value = _uiState.value.copy(showLevel = !_uiState.value.showLevel)
    }
    
    /**
     * Set camera mode
     */
    fun setCameraMode(mode: CameraMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
        
        // Enable voice recognition for voice mode
        if (mode == CameraMode.VOICE_ACTIVATED) {
            _uiState.value = _uiState.value.copy(isVoiceEnabled = true)
        }
    }
    
    /**
     * Handle pinch-to-zoom
     */
    fun onZoomChange(zoomRatio: Float) {
        val currentZoom = _uiState.value.zoomState
        val clampedZoom = zoomRatio.coerceIn(currentZoom.minZoomRatio, currentZoom.maxZoomRatio)
        
        _uiState.value = _uiState.value.copy(
            zoomState = currentZoom.copy(
                currentZoomRatio = clampedZoom,
                isZooming = true
            )
        )
        
        // Stop zooming indicator after a delay
        viewModelScope.launch {
            delay(1000)
            _uiState.value = _uiState.value.copy(
                zoomState = _uiState.value.zoomState.copy(isZooming = false)
            )
        }
    }
    
    /**
     * Digital zoom buttons
     */
    fun zoomIn() {
        val currentZoom = _uiState.value.zoomState
        val newZoom = (currentZoom.currentZoomRatio + 0.5f).coerceAtMost(currentZoom.maxZoomRatio)
        onZoomChange(newZoom)
    }
    
    fun zoomOut() {
        val currentZoom = _uiState.value.zoomState
        val newZoom = (currentZoom.currentZoomRatio - 0.5f).coerceAtLeast(currentZoom.minZoomRatio)
        onZoomChange(newZoom)
    }
    
    /**
     * Handle tap-to-focus
     */
    fun onTapToFocus(x: Float, y: Float) {
        _focusPoint.value = Pair(x, y)
        
        // Clear focus point after animation
        viewModelScope.launch {
            delay(2000)
            _focusPoint.value = null
        }
    }
    
    /**
     * Set burst count
     */
    fun setBurstCount(count: Int) {
        _uiState.value = _uiState.value.copy(burstCount = count.coerceIn(2, 10))
    }
    
    /**
     * Set timer duration
     */
    fun setTimerSeconds(seconds: Int) {
        _uiState.value = _uiState.value.copy(timerSeconds = seconds)
    }
    
    /**
     * Capture photo based on current mode
     */
    fun capturePhoto(imageCapture: ImageCapture) {
        when (_uiState.value.mode) {
            CameraMode.SINGLE_SHOT -> captureSinglePhoto(imageCapture)
            CameraMode.BURST_MODE -> captureBurstPhotos(imageCapture)
            CameraMode.TIMER_MODE -> startTimerCapture(imageCapture)
            CameraMode.HDR_MODE -> captureHDRPhoto(imageCapture)
            CameraMode.VOICE_ACTIVATED -> captureSinglePhoto(imageCapture)
        }
    }
    
    /**
     * Single photo capture
     */
    private fun captureSinglePhoto(imageCapture: ImageCapture) {
        if (_uiState.value.isCapturing) return
        
        _uiState.value = _uiState.value.copy(isCapturing = true, message = "Capturing photo...")
        
        val photoFile = createPhotoFile()
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        // Apply flash mode
        imageCapture.flashMode = when (_uiState.value.flashMode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
        
        imageCapture.takePicture(
            outputFileOptions,
            context.mainExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    soundEffects.play(MediaActionSound.SHUTTER_CLICK)
                    
                    _uiState.value = _uiState.value.copy(
                        isCapturing = false,
                        captureCount = _uiState.value.captureCount + 1,
                        lastPhotoUri = photoFile.absolutePath,
                        message = "Photo saved: ${photoFile.name}",
                        isError = false
                    )
                    
                    updateStorageInfo()
                    clearMessageAfterDelay()
                }
                
                override fun onError(exception: ImageCaptureException) {
                    _uiState.value = _uiState.value.copy(
                        isCapturing = false,
                        message = "Capture failed: ${exception.message}",
                        isError = true
                    )
                    
                    clearMessageAfterDelay()
                }
            }
        )
    }
    
    /**
     * Burst mode capture
     */
    private fun captureBurstPhotos(imageCapture: ImageCapture) {
        if (_uiState.value.isCapturing || burstJob?.isActive == true) return
        
        _uiState.value = _uiState.value.copy(isCapturing = true)
        
        burstJob = viewModelScope.launch {
            val burstCount = _uiState.value.burstCount
            
            repeat(burstCount) { index ->
                _uiState.value = _uiState.value.copy(
                    message = "Burst ${index + 1}/$burstCount"
                )
                
                captureSinglePhoto(imageCapture)
                
                if (index < burstCount - 1) {
                    delay(500) // 500ms between shots
                }
            }
            
            _uiState.value = _uiState.value.copy(
                isCapturing = false,
                message = "Burst capture complete ($burstCount photos)"
            )
            
            clearMessageAfterDelay()
        }
    }
    
    /**
     * Timer mode capture
     */
    private fun startTimerCapture(imageCapture: ImageCapture) {
        if (timerJob?.isActive == true) {
            // Cancel existing timer
            timerJob?.cancel()
            _uiState.value = _uiState.value.copy(
                timerCountdown = 0,
                isCapturing = false,
                message = "Timer cancelled"
            )
            clearMessageAfterDelay()
            return
        }
        
        _uiState.value = _uiState.value.copy(isCapturing = true)
        
        timerJob = viewModelScope.launch {
            val timerSeconds = _uiState.value.timerSeconds
            
            // Countdown
            for (i in timerSeconds downTo 1) {
                _uiState.value = _uiState.value.copy(
                    timerCountdown = i,
                    message = "Taking photo in $i..."
                )
                
                // Beep sound for last 3 seconds
                if (i <= 3) {
                    soundEffects.play(MediaActionSound.START_VIDEO_RECORDING)
                }
                
                delay(1000)
            }
            
            _uiState.value = _uiState.value.copy(timerCountdown = 0)
            captureSinglePhoto(imageCapture)
        }
    }
    
    /**
     * HDR photo capture
     */
    private fun captureHDRPhoto(imageCapture: ImageCapture) {
        // For now, use single shot with HDR flag
        // In a real implementation, you'd capture multiple exposures
        _uiState.value = _uiState.value.copy(message = "HDR mode enabled")
        captureSinglePhoto(imageCapture)
    }
    
    /**
     * Voice command capture
     */
    fun onVoiceCommand(command: String) {
        if (!_uiState.value.isVoiceEnabled) return
        
        val normalizedCommand = command.lowercase().trim()
        if (normalizedCommand.contains("capture") || 
            normalizedCommand.contains("photo") ||
            normalizedCommand.contains("shoot") ||
            normalizedCommand.contains("take")) {
            
            // This will be called by VoiceCapture service
            // We'll implement the actual voice recognition in VoiceCapture.kt
        }
    }
    
    /**
     * Create unique photo file
     */
    private fun createPhotoFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val modePrefix = when (_uiState.value.mode) {
            CameraMode.BURST_MODE -> "BURST"
            CameraMode.HDR_MODE -> "HDR"
            CameraMode.TIMER_MODE -> "TIMER"
            CameraMode.VOICE_ACTIVATED -> "VOICE"
            else -> "HH"
        }
        
        val fileName = "${modePrefix}_${timestamp}.jpg"
        
        // Try external pictures directory first, fallback to cache
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.externalCacheDir
            ?: context.cacheDir
            
        return File(picturesDir, fileName)
    }
    
    /**
     * Update available storage information
     */
    private fun updateStorageInfo() {
        viewModelScope.launch {
            try {
                val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: context.externalCacheDir
                    ?: context.cacheDir
                    
                val freeBytes = picturesDir.freeSpace
                val freeGB = freeBytes / (1024.0 * 1024.0 * 1024.0)
                
                _uiState.value = _uiState.value.copy(availableStorageGB = freeGB.toFloat())
            } catch (e: Exception) {
                // Ignore storage calculation errors
            }
        }
    }
    
    /**
     * Clear message after delay
     */
    private fun clearMessageAfterDelay() {
        viewModelScope.launch {
            delay(3000)
            _uiState.value = _uiState.value.copy(message = "", isError = false)
        }
    }
    
    /**
     * Start level monitoring using device sensors
     */
    private fun startLevelMonitoring() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
    
    /**
     * Stop level monitoring
     */
    private fun stopLevelMonitoring() {
        sensorManager.unregisterListener(this)
    }
    
    /**
     * Sensor event handling for level calculation
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, gravity.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagnetic, 0, geomagnetic.size)
            }
        }
        
        // Calculate orientation
        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotationMatrix, orientation)
            
            // Convert to degrees
            val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            
            val threshold = _uiState.value.levelState.threshold
            val isLevel = abs(pitch) < threshold && abs(roll) < threshold
            
            _uiState.value = _uiState.value.copy(
                levelState = LevelState(
                    pitch = pitch,
                    roll = roll,
                    isLevel = isLevel,
                    threshold = threshold
                )
            )
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
    
    /**
     * Cleanup resources
     */
    override fun onCleared() {
        super.onCleared()
        stopLevelMonitoring()
        soundEffects.release()
        timerJob?.cancel()
        burstJob?.cancel()
        locationService.cleanup()
    }
}