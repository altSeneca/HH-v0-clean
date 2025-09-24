package com.hazardhawk.ar

import android.content.Context
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility for checking AR capabilities and device compatibility.
 * Provides safe AR feature detection with fallback handling.
 */
object ARCapabilityChecker {

    /**
     * Check if ARCore is supported and available on this device.
     */
    suspend fun isARSupported(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            when (ArCoreApk.getInstance().checkAvailability(context)) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                    // ARCore is supported but needs update/installation
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            android.util.Log.w("ARCapabilityChecker", "Error checking AR support", e)
            false
        }
    }

    /**
     * Check if ARCore can be installed or updated if needed.
     */
    suspend fun requestARInstallation(context: Context): ARInstallResult = withContext(Dispatchers.IO) {
        try {
            when (ArCoreApk.getInstance().requestInstall(context as android.app.Activity, true)) {
                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> ARInstallResult.INSTALL_REQUESTED
                ArCoreApk.InstallStatus.INSTALLED -> ARInstallResult.ALREADY_INSTALLED
                else -> ARInstallResult.FAILED
            }
        } catch (e: Exception) {
            android.util.Log.e("ARCapabilityChecker", "Error requesting AR installation", e)
            ARInstallResult.FAILED
        }
    }

    /**
     * Test basic AR session creation to verify functionality.
     */
    suspend fun testARSession(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = Session(context)
            val config = Config(session)
            config.focusMode = Config.FocusMode.AUTO
            session.configure(config)
            session.close()
            true
        } catch (e: Exception) {
            android.util.Log.w("ARCapabilityChecker", "AR session test failed", e)
            false
        }
    }

    /**
     * Get detailed AR capabilities for this device.
     */
    suspend fun getARCapabilities(context: Context): ARCapabilities = withContext(Dispatchers.IO) {
        try {
            val availability = ArCoreApk.getInstance().checkAvailability(context)
            val isSupported = availability != ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE

            ARCapabilities(
                isSupported = isSupported,
                availability = availability,
                canCreateSession = if (isSupported) testARSession(context) else false,
                requiresInstallation = availability == ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED,
                requiresUpdate = availability == ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD
            )
        } catch (e: Exception) {
            android.util.Log.e("ARCapabilityChecker", "Error getting AR capabilities", e)
            ARCapabilities(
                isSupported = false,
                availability = ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE,
                canCreateSession = false,
                requiresInstallation = false,
                requiresUpdate = false
            )
        }
    }
}

/**
 * Result of AR installation request.
 */
enum class ARInstallResult {
    ALREADY_INSTALLED,
    INSTALL_REQUESTED,
    FAILED
}

/**
 * Comprehensive AR capabilities for a device.
 */
data class ARCapabilities(
    val isSupported: Boolean,
    val availability: ArCoreApk.Availability,
    val canCreateSession: Boolean,
    val requiresInstallation: Boolean,
    val requiresUpdate: Boolean
) {
    /**
     * True if AR is ready to use without any additional setup.
     */
    val isReadyToUse: Boolean
        get() = isSupported && canCreateSession && !requiresInstallation && !requiresUpdate

    /**
     * User-friendly description of AR status.
     */
    val statusDescription: String
        get() = when {
            !isSupported -> "AR is not supported on this device"
            requiresInstallation -> "ARCore needs to be installed"
            requiresUpdate -> "ARCore needs to be updated"
            !canCreateSession -> "AR session cannot be created"
            isReadyToUse -> "AR is ready to use"
            else -> "AR status unknown"
        }
}