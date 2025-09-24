package com.hazardhawk.security

import android.content.Context
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android-specific security module for dependency injection
 * 
 * Provides platform-specific implementations of security services
 * using Android Keystore and crypto APIs.
 */
val androidSecurityModule: Module = module {
    
    /**
     * Provide Android implementation of SecureStorageService
     */
    single<SecureStorageService> {
        SecureStorageServiceImpl(context = get())
    }
    
    /**
     * Provide Android implementation of PhotoEncryptionService
     */
    single<PhotoEncryptionService> {
        PhotoEncryptionServiceImpl()
    }
    
    /**
     * Provide Android security configuration
     */
    single<AndroidSecurityConfig> {
        AndroidSecurityConfig(
            context = get(),
            secureStorage = get(),
            photoEncryption = get()
        )
    }
}

/**
 * Android-specific security configuration
 */
class AndroidSecurityConfig(
    private val context: Context,
    private val secureStorage: SecureStorageService,
    private val photoEncryption: PhotoEncryptionService
) {
    
    companion object {
        private const val TAG = "AndroidSecurityConfig"
    }
    
    /**
     * Initialize Android security services
     */
    suspend fun initialize() {
        try {
            // Validate secure storage integrity
            val storageValid = secureStorage.validateIntegrity()
            if (!storageValid) {
                throw SecurityException("Secure storage integrity validation failed")
            }
            
            // Initialize encryption service
            val encryptionInfo = photoEncryption.getEncryptionInfo()
            android.util.Log.i(TAG, "Encryption initialized: ${encryptionInfo.algorithm}-${encryptionInfo.keyLength}")
            
            // Check hardware security availability
            val hardwareBacked = secureStorage.isHardwareBackedSecurity()
            android.util.Log.i(TAG, "Hardware-backed security: ${if (hardwareBacked) "Available" else "Not available"}")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Security initialization failed", e)
            throw SecurityException("Failed to initialize Android security services", e)
        }
    }
    
    /**
     * Get security capabilities for this device
     */
    fun getSecurityCapabilities(): AndroidSecurityCapabilities {
        return AndroidSecurityCapabilities(
            hardwareBackedStorage = secureStorage.isHardwareBackedSecurity(),
            encryptionInfo = photoEncryption.getEncryptionInfo(),
            apiLevel = android.os.Build.VERSION.SDK_INT,
            strongBoxAvailable = isStrongBoxAvailable()
        )
    }
    
    /**
     * Check if StrongBox security is available (Android 9+)
     */
    private fun isStrongBoxAvailable(): Boolean {
        return try {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature("android.hardware.strongbox_keystore")
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Android security capabilities information
 */
data class AndroidSecurityCapabilities(
    val hardwareBackedStorage: Boolean,
    val encryptionInfo: EncryptionInfo,
    val apiLevel: Int,
    val strongBoxAvailable: Boolean
) {
    val isHighSecurityDevice: Boolean
        get() = hardwareBackedStorage && strongBoxAvailable && apiLevel >= 28
    
    val securityLevel: String
        get() = when {
            isHighSecurityDevice -> "HIGH"
            hardwareBackedStorage -> "MEDIUM"
            else -> "STANDARD"
        }
}