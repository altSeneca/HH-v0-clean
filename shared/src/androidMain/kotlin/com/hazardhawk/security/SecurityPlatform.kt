package com.hazardhawk.security

import android.content.Context

/**
 * Android implementation of SecurityPlatform
 */
actual class SecurityPlatform(private val context: Context) {
    
    actual fun createSecureStorageService(): SecureStorageService {
        return SecureStorageServiceImpl(context)
    }
    
    actual fun createPhotoEncryptionService(): PhotoEncryptionService {
        return PhotoEncryptionServiceImpl()
    }
    
    actual fun getSecurityCapabilities(): Map<String, Any> {
        val secureStorage = createSecureStorageService()
        val photoEncryption = createPhotoEncryptionService()
        
        return mapOf(
            "platform" to "Android",
            "apiLevel" to android.os.Build.VERSION.SDK_INT,
            "hardwareBacked" to secureStorage.isHardwareBackedSecurity(),
            "encryptionInfo" to mapOf(
                "algorithm" to photoEncryption.getEncryptionInfo().algorithm,
                "keyLength" to photoEncryption.getEncryptionInfo().keyLength,
                "mode" to photoEncryption.getEncryptionInfo().mode
            ),
            "strongBoxAvailable" to isStrongBoxAvailable(),
            "deviceSecure" to isDeviceSecure()
        )
    }
    
    /**
     * Check if StrongBox hardware security is available
     */
    private fun isStrongBoxAvailable(): Boolean {
        return try {
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature("android.hardware.strongbox_keystore")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if device has secure lock screen
     */
    private fun isDeviceSecure(): Boolean {
        return try {
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.isDeviceSecure
        } catch (e: Exception) {
            false
        }
    }
}
