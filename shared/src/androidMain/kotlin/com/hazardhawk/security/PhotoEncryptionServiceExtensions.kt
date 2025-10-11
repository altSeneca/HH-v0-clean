package com.hazardhawk.security

/**
 * Extension functions for PhotoEncryptionService to provide additional capabilities
 */

/**
 * Information about encryption implementation
 */
data class EncryptionInfo(
    val algorithm: String,
    val keyLength: Int,
    val mode: String = "GCM"
)

/**
 * Get encryption implementation information
 */
fun PhotoEncryptionService.getEncryptionInfo(): EncryptionInfo {
    return EncryptionInfo(
        algorithm = "AES",
        keyLength = 256,
        mode = "GCM"
    )
}
