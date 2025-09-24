package com.hazardhawk.security

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Core interface for secure credential storage across all platforms.
 * Provides tamper-resistant storage for API keys, authentication tokens,
 * and sensitive configuration data with OSHA compliance requirements.
 * 
 * Platform implementations:
 * - Android: Android Keystore + EncryptedSharedPreferences
 * - iOS: iOS Keychain Services
 * - Desktop: Platform-specific secure storage (e.g., Windows Credential Manager, macOS Keychain)
 * - Web: Browser secure storage with encryption
 */
interface SecureStorageService {
    
    /**
     * Store an API key or sensitive credential securely
     * @param key Unique identifier for the credential
     * @param value The sensitive data to store
     * @param metadata Optional metadata for audit purposes
     * @return Result indicating success or failure
     */
    suspend fun storeApiKey(
        key: String, 
        value: String, 
        metadata: CredentialMetadata? = null
    ): Result<Unit>
    
    /**
     * Retrieve a stored API key or credential
     * @param key Unique identifier for the credential
     * @return Result containing the credential or null if not found
     */
    suspend fun getApiKey(key: String): Result<String?>
    
    /**
     * Remove a specific credential from secure storage
     * @param key Unique identifier for the credential
     * @return Result indicating success or failure
     */
    suspend fun removeApiKey(key: String): Result<Unit>
    
    /**
     * Clear all stored credentials (for security incidents or user logout)
     * This operation is logged for audit purposes
     * @return Result indicating success or failure
     */
    suspend fun clearAllCredentials(): Result<Unit>
    
    /**
     * List all stored credential keys (without values) for management
     * @return Result containing list of credential keys
     */
    suspend fun listCredentialKeys(): Result<List<String>>
    
    /**
     * Check if secure storage is available and properly configured
     * @return true if secure storage is ready for use
     */
    suspend fun isAvailable(): Boolean
    
    /**
     * Get metadata for a stored credential without accessing the value
     * @param key Unique identifier for the credential
     * @return Result containing metadata or null if not found
     */
    suspend fun getCredentialMetadata(key: String): Result<CredentialMetadata?>
    
    /**
     * Update metadata for an existing credential
     * @param key Unique identifier for the credential
     * @param metadata New metadata to associate with the credential
     * @return Result indicating success or failure
     */
    suspend fun updateCredentialMetadata(key: String, metadata: CredentialMetadata): Result<Unit>
}

/**
 * Metadata for stored credentials to support audit logging and compliance
 */
@Serializable
data class CredentialMetadata(
    val createdAt: Instant,
    val lastAccessedAt: Instant? = null,
    val expiresAt: Instant? = null,
    val purpose: CredentialPurpose,
    val userId: String? = null,
    val description: String? = null,
    val accessCount: Int = 0,
    val complianceLevel: ComplianceLevel = ComplianceLevel.Standard
)

/**
 * Purpose classification for stored credentials
 */
@Serializable
enum class CredentialPurpose {
    /** API keys for AI analysis services */
    AI_SERVICE_API_KEY,
    /** AWS/Cloud storage access keys */
    CLOUD_STORAGE_ACCESS,
    /** Authentication tokens */
    AUTH_TOKEN,
    /** Database connection credentials */
    DATABASE_ACCESS,
    /** Third-party integration keys */
    THIRD_PARTY_API,
    /** Encryption keys for data protection */
    ENCRYPTION_KEY,
    /** Certificate pinning data */
    CERTIFICATE_DATA,
    /** Other sensitive configuration */
    OTHER
}

/**
 * Compliance level for credential storage requirements
 */
@Serializable
enum class ComplianceLevel {
    /** Standard security requirements */
    Standard,
    /** Enhanced security for sensitive data */
    Enhanced,
    /** Maximum security for critical systems */
    Critical,
    /** OSHA-specific compliance requirements */
    OSHA_Compliant
}

/**
 * Standard credential keys used throughout the application
 */
object CredentialKeys {
    const val GEMINI_API_KEY = "gemini_api_key"
    const val AWS_ACCESS_KEY_ID = "aws_access_key_id"
    const val AWS_SECRET_ACCESS_KEY = "aws_secret_access_key"
    const val JWT_TOKEN = "jwt_token"
    const val REFRESH_TOKEN = "refresh_token"
    const val DEVICE_ENCRYPTION_KEY = "device_encryption_key"
    const val PHOTO_ENCRYPTION_KEY = "photo_encryption_key"
    const val CERTIFICATE_PINS = "certificate_pins"
}