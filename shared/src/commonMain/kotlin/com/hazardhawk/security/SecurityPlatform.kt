package com.hazardhawk.security

/**
 * Platform-specific security service factory
 * 
 * Uses expect/actual pattern to provide platform-specific implementations
 * of security services while maintaining a common interface.
 */
expected class SecurityPlatform {
    
    /**
     * Create platform-specific secure storage service
     */
    fun createSecureStorageService(): SecureStorageService
    
    /**
     * Create platform-specific photo encryption service
     */
    fun createPhotoEncryptionService(): PhotoEncryptionService
    
    /**
     * Get platform security capabilities
     */
    fun getSecurityCapabilities(): Map<String, Any>
}

/**
 * Common security manager that works across all platforms
 */
class SecurityManager(private val platform: SecurityPlatform) {
    
    val secureStorage: SecureStorageService by lazy {
        platform.createSecureStorageService()
    }
    
    val photoEncryption: PhotoEncryptionService by lazy {
        platform.createPhotoEncryptionService()
    }
    
    /**
     * Initialize security services
     */
    suspend fun initialize() {
        // Validate secure storage
        val storageValid = secureStorage.validateIntegrity()
        if (!storageValid) {
            throw SecurityException("Security storage validation failed")
        }
        
        // Test encryption capability
        val testKey = secureStorage.generateSecureKey()
        if (!photoEncryption.isValidEncryptionKey(testKey)) {
            throw SecurityException("Photo encryption validation failed")
        }
    }
    
    /**
     * Get security status information
     */
    suspend fun getSecurityStatus(): SecurityStatus {
        val capabilities = platform.getSecurityCapabilities()
        val storageIntegrity = secureStorage.validateIntegrity()
        
        return SecurityStatus(
            hardwareBacked = capabilities["hardwareBacked"] as? Boolean ?: false,
            encryptionAvailable = true,
            storageIntegrity = storageIntegrity,
            platformCapabilities = capabilities
        )
    }
}

/**
 * Security status information
 */
data class SecurityStatus(
    val hardwareBacked: Boolean,
    val encryptionAvailable: Boolean,
    val storageIntegrity: Boolean,
    val platformCapabilities: Map<String, Any>
) {
    val isSecure: Boolean
        get() = encryptionAvailable && storageIntegrity
}