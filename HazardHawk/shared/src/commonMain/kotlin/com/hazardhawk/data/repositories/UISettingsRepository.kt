package com.hazardhawk.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Data class for UI Settings
 */
@Serializable
data class UISettings(
    val glassEnabled: Boolean = false,
    val performanceTier: String = "AUTO",
    val emergencyMode: Boolean = false,
    val highContrastMode: Boolean = false,
    val metadataFontSize: Float = 16f,
    val autoFadeDelay: Long = 8000L,
    val hapticFeedbackEnabled: Boolean = true,
    val arEnabled: Boolean = false,
    // Camera Settings
    val orientationLock: String = "AUTO", // AUTO, PORTRAIT, LANDSCAPE
    // AR Privacy Protection Settings
    val facialAnonymizationEnabled: Boolean = true,
    val arConsentGiven: Boolean = false,
    val consentTimestamp: Long = 0L,
    val privacyProtectionLevel: String = "STANDARD",
    val arDataRetentionDays: Int = 30
)

/**
 * Repository interface for UI settings persistence
 */
interface UISettingsRepository {
    suspend fun loadSettings(): UISettings
    suspend fun saveSettings(settings: UISettings)
    suspend fun updateGlassEnabled(enabled: Boolean)
    suspend fun updatePerformanceTier(tier: String)
    suspend fun updateEmergencyMode(enabled: Boolean)
    suspend fun updateHighContrastMode(enabled: Boolean)
    suspend fun updateMetadataFontSize(size: Float)
    suspend fun updateAutoFadeDelay(delay: Long)
    suspend fun updateHapticFeedback(enabled: Boolean)
    suspend fun updateOrientationLock(orientation: String)
    suspend fun resetToDefaults()
    fun getSettingsFlow(): StateFlow<UISettings>
}

/**
 * Implementation of UI settings repository
 */
class UISettingsRepositoryImpl(
    private val storage: SecureStorage
) : UISettingsRepository {

    companion object {
        private const val UI_SETTINGS_KEY = "ui_settings_v1"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _settingsFlow = MutableStateFlow(UISettings())
    private var hasLoaded = false

    override fun getSettingsFlow(): StateFlow<UISettings> = _settingsFlow.asStateFlow()

    override suspend fun loadSettings(): UISettings {
        return try {
            val settingsJson = storage.getString(UI_SETTINGS_KEY)
            val settings = if (settingsJson != null) {
                json.decodeFromString<UISettings>(settingsJson)
            } else {
                UISettings() // Return defaults
            }

            _settingsFlow.value = settings
            hasLoaded = true
            settings
        } catch (e: Exception) {
            // If there's an error loading settings, return defaults
            val defaultSettings = UISettings()
            _settingsFlow.value = defaultSettings
            hasLoaded = true
            defaultSettings
        }
    }

    override suspend fun saveSettings(settings: UISettings) {
        try {
            val settingsJson = json.encodeToString(settings)
            storage.putString(UI_SETTINGS_KEY, settingsJson)
            _settingsFlow.value = settings
        } catch (e: Exception) {
            // Log error but don't throw
            println("UISettingsRepository: Failed to save settings: ${e.message}")
        }
    }

    override suspend fun updateGlassEnabled(enabled: Boolean) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(glassEnabled = enabled)
        saveSettings(updated)
    }

    override suspend fun updatePerformanceTier(tier: String) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(performanceTier = tier)
        saveSettings(updated)
    }

    override suspend fun updateEmergencyMode(enabled: Boolean) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(emergencyMode = enabled)
        saveSettings(updated)
    }

    override suspend fun updateHighContrastMode(enabled: Boolean) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(highContrastMode = enabled)
        saveSettings(updated)
    }

    override suspend fun updateMetadataFontSize(size: Float) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(metadataFontSize = size.coerceIn(12f, 24f))
        saveSettings(updated)
    }

    override suspend fun updateAutoFadeDelay(delay: Long) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(autoFadeDelay = delay.coerceIn(1000L, 10000L))
        saveSettings(updated)
    }

    override suspend fun updateHapticFeedback(enabled: Boolean) {
        ensureLoaded()
        val updated = _settingsFlow.value.copy(hapticFeedbackEnabled = enabled)
        saveSettings(updated)
    }

    override suspend fun updateOrientationLock(orientation: String) {
        ensureLoaded()
        val validOrientations = listOf("AUTO", "PORTRAIT", "LANDSCAPE")
        val validOrientation = if (orientation in validOrientations) orientation else "AUTO"
        val updated = _settingsFlow.value.copy(orientationLock = validOrientation)
        saveSettings(updated)
    }

    override suspend fun resetToDefaults() {
        val defaultSettings = UISettings()
        saveSettings(defaultSettings)
    }

    private suspend fun ensureLoaded() {
        if (!hasLoaded) {
            loadSettings()
        }
    }
}