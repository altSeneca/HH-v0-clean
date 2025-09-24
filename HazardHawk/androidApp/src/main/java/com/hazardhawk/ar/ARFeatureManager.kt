package com.hazardhawk.ar

import android.content.Context
import com.hazardhawk.data.repositories.UISettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Manages AR feature flags and capabilities for gradual rollout.
 * Provides centralized control over AR feature availability.
 */
class ARFeatureManager(
    private val context: Context,
    private val uiSettingsRepository: UISettingsRepository
) {

    /**
     * Check if AR features should be enabled based on device capabilities and user settings.
     */
    suspend fun isAREnabled(): Boolean {
        val settings = uiSettingsRepository.loadSettings()
        val capabilities = ARCapabilityChecker.getARCapabilities(context)

        return settings.arEnabled &&
               capabilities.isSupported &&
               !isLowEndDevice() &&
               !isEmergencyMode()
    }

    /**
     * Flow of AR enabled state that reacts to settings changes.
     */
    fun arEnabledFlow(): Flow<Boolean> {
        return uiSettingsRepository.getSettingsFlow().map { settings ->
            if (!settings.arEnabled) return@map false

            try {
                val capabilities = ARCapabilityChecker.getARCapabilities(context)
                capabilities.isSupported && !isLowEndDevice() && !isEmergencyMode()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Enable AR features for the user.
     */
    suspend fun enableAR(): Boolean {
        val capabilities = ARCapabilityChecker.getARCapabilities(context)

        return if (capabilities.isSupported) {
            uiSettingsRepository.updateAREnabled(true)
            true
        } else {
            false
        }
    }

    /**
     * Disable AR features.
     */
    suspend fun disableAR() {
        uiSettingsRepository.updateAREnabled(false)
    }

    /**
     * Check if this is a low-end device that should skip AR.
     */
    private fun isLowEndDevice(): Boolean {
        // Check device capabilities for AR performance
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as android.app.ActivityManager
            activityManager.isLowRamDevice
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if emergency mode is active (AR should be disabled).
     */
    private suspend fun isEmergencyMode(): Boolean {
        val settings = uiSettingsRepository.loadSettings()
        return settings.emergencyMode
    }

    /**
     * Get AR readiness status with user-friendly message.
     */
    suspend fun getARStatus(): ARFeatureStatus {
        val capabilities = ARCapabilityChecker.getARCapabilities(context)
        val settings = uiSettingsRepository.loadSettings()

        return when {
            !capabilities.isSupported -> ARFeatureStatus(
                isReady = false,
                canEnable = false,
                message = "AR is not supported on this device",
                requiresAction = false
            )

            capabilities.requiresInstallation -> ARFeatureStatus(
                isReady = false,
                canEnable = true,
                message = "ARCore needs to be installed",
                requiresAction = true,
                actionType = ARActionType.INSTALL_ARCORE
            )

            capabilities.requiresUpdate -> ARFeatureStatus(
                isReady = false,
                canEnable = true,
                message = "ARCore needs to be updated",
                requiresAction = true,
                actionType = ARActionType.UPDATE_ARCORE
            )

            isLowEndDevice() -> ARFeatureStatus(
                isReady = false,
                canEnable = false,
                message = "AR features disabled on low-end devices for performance",
                requiresAction = false
            )

            isEmergencyMode() -> ARFeatureStatus(
                isReady = false,
                canEnable = false,
                message = "AR disabled in emergency mode",
                requiresAction = false
            )

            !settings.arEnabled -> ARFeatureStatus(
                isReady = false,
                canEnable = true,
                message = "AR features are disabled in settings",
                requiresAction = true,
                actionType = ARActionType.ENABLE_IN_SETTINGS
            )

            else -> ARFeatureStatus(
                isReady = true,
                canEnable = true,
                message = "AR features are ready",
                requiresAction = false
            )
        }
    }

    /**
     * Handle AR action requests (install, update, enable).
     */
    suspend fun handleARAction(actionType: ARActionType, context: Context): Boolean {
        return when (actionType) {
            ARActionType.INSTALL_ARCORE -> {
                val result = ARCapabilityChecker.requestARInstallation(context)
                result == com.hazardhawk.ar.ARInstallResult.ALREADY_INSTALLED
            }

            ARActionType.UPDATE_ARCORE -> {
                val result = ARCapabilityChecker.requestARInstallation(context)
                result == com.hazardhawk.ar.ARInstallResult.ALREADY_INSTALLED
            }

            ARActionType.ENABLE_IN_SETTINGS -> {
                enableAR()
            }
        }
    }
}

/**
 * AR feature status with user-actionable information.
 */
data class ARFeatureStatus(
    val isReady: Boolean,
    val canEnable: Boolean,
    val message: String,
    val requiresAction: Boolean,
    val actionType: ARActionType? = null
)

/**
 * Types of actions users can take to enable AR.
 */
enum class ARActionType {
    INSTALL_ARCORE,
    UPDATE_ARCORE,
    ENABLE_IN_SETTINGS
}

/**
 * Extension to UISettings for AR support.
 */
suspend fun UISettingsRepository.updateAREnabled(enabled: Boolean) {
    val current = loadSettings()
    val updated = current.copy(arEnabled = enabled)
    saveSettings(updated)
}

/**
 * Add AR enabled property to UISettings data class.
 * This should be added to the UISettings data class in UISettingsRepository.kt
 */
// data class UISettings(
//     val glassEnabled: Boolean = false,
//     val performanceTier: String = "AUTO",
//     val emergencyMode: Boolean = false,
//     val highContrastMode: Boolean = false,
//     val metadataFontSize: Float = 16f,
//     val autoFadeDelay: Long = 8000L,
//     val hapticFeedbackEnabled: Boolean = true,
//     val arEnabled: Boolean = false  // Add this line
// )