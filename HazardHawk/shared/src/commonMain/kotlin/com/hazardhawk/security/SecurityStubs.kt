package com.hazardhawk.security

/**
 * Stub interfaces for Phase 2 implementation
 * These will be replaced with actual implementations when security services are integrated
 */

interface AuditLogger {
    suspend fun logEvent(
        eventType: String,
        details: Map<String, String>,
        userId: String?,
        metadata: Map<String, String>
    )
}

interface SecureStorageService {
    suspend fun getString(key: String): String?
    suspend fun setString(key: String, value: String): Boolean
}

interface PhotoEncryptionService {
    suspend fun encryptData(data: ByteArray): ByteArray
    suspend fun decryptData(encryptedData: ByteArray): ByteArray
}

// Stub implementations
class StubAuditLogger : AuditLogger {
    override suspend fun logEvent(
        eventType: String,
        details: Map<String, String>,
        userId: String?,
        metadata: Map<String, String>
    ) {
        // Stub implementation - logs would be stored in actual implementation
    }
}

class StubSecureStorageService : SecureStorageService {
    private val storage = mutableMapOf<String, String>()
    
    override suspend fun getString(key: String): String? {
        return storage[key]
    }
    
    override suspend fun setString(key: String, value: String): Boolean {
        storage[key] = value
        return true
    }
}

class StubPhotoEncryptionService : PhotoEncryptionService {
    override suspend fun encryptData(data: ByteArray): ByteArray {
        // Stub implementation - actual encryption would be applied
        return data
    }
    
    override suspend fun decryptData(encryptedData: ByteArray): ByteArray {
        // Stub implementation - actual decryption would be applied
        return encryptedData
    }
}