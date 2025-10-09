package com.hazardhawk.security

import kotlinx.datetime.Clock

/**
 * Extension methods to provide simplified API for security services.
 * These bridge the gap between the full interface API and simplified usage patterns
 * used throughout the AI and analysis code.
 */

/**
 * Get a string value from secure storage (simplified API)
 */
suspend fun SecureStorageService.getString(key: String): String? {
    return getApiKey(key).getOrNull()
}

/**
 * Set a string value in secure storage (simplified API)
 */
suspend fun SecureStorageService.setString(key: String, value: String): Boolean {
    return storeApiKey(key, value, metadata = null).isSuccess
}

/**
 * Encrypt data using photo encryption service (simplified API)
 */
suspend fun PhotoEncryptionService.encryptData(data: ByteArray): ByteArray {
    // Generate a unique ID for this encryption operation
    val photoId = "generic_${Clock.System.now().toEpochMilliseconds()}"
    return encryptPhoto(data, photoId, compressionLevel = 0).getOrNull()?.encryptedData ?: data
}

/**
 * Decrypt data using photo encryption service (simplified API)
 */
suspend fun PhotoEncryptionService.decryptData(encryptedData: ByteArray): ByteArray {
    // For simplified decrypt, we assume the data is already in EncryptedPhoto format
    // This is a limitation of the simplified API - full API should be used for production
    val now = Clock.System.now()
    val encryptedPhoto = EncryptedPhoto(
        photoId = "generic_decrypt",
        encryptedData = encryptedData,
        initializationVector = ByteArray(12), // GCM uses 96-bit IV
        authenticationTag = ByteArray(16), // GCM uses 128-bit tag
        keyId = "default_key",
        encryptionAlgorithm = "AES-256-GCM",
        compressionUsed = false,
        originalSize = encryptedData.size.toLong(),
        encryptedAt = now,
        integrity = IntegrityMetadata(
            checksum = "",
            algorithm = "SHA-256",
            createdAt = now
        )
    )
    return decryptPhoto(encryptedPhoto).getOrNull() ?: encryptedData
}

/**
 * Log an event using audit logger (simplified API)
 */
suspend fun AuditLogger.logEvent(
    eventType: String,
    details: Map<String, String>,
    userId: String? = null,
    metadata: Map<String, String> = emptyMap()
) {
    // Convert to proper SecurityEvent
    val now = Clock.System.now()
    val event = SecurityEvent(
        eventType = SecurityEventType.OTHER,
        severity = EventSeverity.INFO,
        description = eventType,
        userId = userId,
        ipAddress = metadata["ipAddress"],
        deviceId = metadata["deviceId"],
        userAgent = metadata["userAgent"],
        successful = true,
        failureReason = null,
        timestamp = now,
        metadata = details + metadata
    )
    logSecurityEvent(event)
}
