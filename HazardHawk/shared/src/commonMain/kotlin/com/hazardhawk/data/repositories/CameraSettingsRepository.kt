package com.hazardhawk.data.repositories

import com.hazardhawk.data.models.CameraSettings
import com.hazardhawk.data.models.GridType
import com.hazardhawk.data.models.MetadataPosition
import com.hazardhawk.data.models.FlashMode
import com.hazardhawk.data.models.ImageFormat
import com.hazardhawk.data.models.VideoQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Repository interface for camera settings persistence
 * Provides cross-platform storage for camera configuration
 */
interface CameraSettingsRepository {
    suspend fun loadSettings(): CameraSettings
    suspend fun saveSettings(settings: CameraSettings)
    suspend fun updateAspectRatio(aspectRatio: String, index: Int)
    suspend fun updateZoom(zoom: Float, index: Int)
    suspend fun updateGridSettings(showGrid: Boolean, gridType: GridType)
    suspend fun updateMetadataSettings(showMetadata: Boolean, position: MetadataPosition)
    suspend fun updateFlashMode(mode: FlashMode)
    suspend fun updateImageQuality(quality: Int)
    suspend fun updateHapticFeedback(enabled: Boolean)
    suspend fun resetToDefaults()
    fun getSettingsFlow(): StateFlow<CameraSettings>
}

/**
 * Default implementation of camera settings repository
 * Uses platform-specific storage through SecureStorage interface
 */
class CameraSettingsRepositoryImpl(
    private val storage: SecureStorage
) : CameraSettingsRepository {
    
    companion object {
        private const val CAMERA_SETTINGS_KEY = "camera_settings_v1"
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val _settingsFlow = MutableStateFlow(CameraSettings())
    private var hasLoaded = false
    
    override fun getSettingsFlow(): StateFlow<CameraSettings> = _settingsFlow.asStateFlow()
    
    override suspend fun loadSettings(): CameraSettings {
        return try {
            val settingsJson = storage.getString(CAMERA_SETTINGS_KEY)
            val settings = if (settingsJson != null) {
                json.decodeFromString<CameraSettings>(settingsJson)
            } else {
                CameraSettings() // Return defaults
            }
            
            _settingsFlow.value = settings
            hasLoaded = true
            settings
        } catch (e: Exception) {
            // If there's an error loading settings, return defaults
            val defaultSettings = CameraSettings()
            _settingsFlow.value = defaultSettings
            hasLoaded = true
            defaultSettings
        }
    }
    
    override suspend fun saveSettings(settings: CameraSettings) {
        try {
            val settingsJson = json.encodeToString(settings)
            storage.putString(CAMERA_SETTINGS_KEY, settingsJson)
            _settingsFlow.value = settings
        } catch (e: Exception) {
            // Log error but don't throw - app should continue to work without settings persistence
            // In a real app, you might want to use a logging framework here
            println("CameraSettingsRepository: Failed to save settings: ${e.message}")
        }
    }
    
    override suspend fun updateAspectRatio(aspectRatio: String, index: Int) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(
            selectedAspectRatio = aspectRatio,
            selectedAspectRatioIndex = index
        )
        saveSettings(updated)
    }
    
    override suspend fun updateZoom(zoom: Float, index: Int) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(
            selectedZoom = zoom,
            selectedZoomIndex = index
        )
        saveSettings(updated)
    }
    
    override suspend fun updateGridSettings(showGrid: Boolean, gridType: GridType) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(
            showGrid = showGrid,
            gridType = gridType.value
        )
        saveSettings(updated)
    }
    
    override suspend fun updateMetadataSettings(showMetadata: Boolean, position: MetadataPosition) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(
            showMetadata = showMetadata,
            metadataPosition = position.value
        )
        saveSettings(updated)
    }
    
    override suspend fun updateFlashMode(mode: FlashMode) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(
            flashMode = mode.value
        )
        saveSettings(updated)
    }
    
    override suspend fun updateImageQuality(quality: Int) {
        ensureLoaded()
        val qualityClamped = quality.coerceIn(1, 100)
        val updated = _settingsFlow.value.copy(
            imageQuality = qualityClamped
        )
        saveSettings(updated)
    }
    
    override suspend fun updateHapticFeedback(enabled: Boolean) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(
            hapticFeedbackEnabled = enabled
        )
        saveSettings(updated)
    }
    
    override suspend fun resetToDefaults() {
        val defaultSettings = CameraSettings()
        saveSettings(defaultSettings)
    }
    
    private suspend fun ensureLoaded() {
        if (!hasLoaded) {
            loadSettings()
        }
    }
}

/**
 * Cross-platform storage interface
 * Platform-specific implementations will handle the actual storage
 */
interface SecureStorage {
    suspend fun putString(key: String, value: String)
    suspend fun getString(key: String): String?
    suspend fun remove(key: String)
    suspend fun clear()
}