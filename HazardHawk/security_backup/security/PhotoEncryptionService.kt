package com.hazardhawk.security

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Interface for photo encryption and decryption across all platforms.
 * Implements AES-256 encryption for photo data with secure key management.
 * Supports both at-rest encryption (local storage) and in-transit encryption.
 * 
 * Platform implementations handle hardware-backed encryption where available:
 * - Android: Android Keystore with AES-256-GCM
 * - iOS: Secure Enclave with AES-256-GCM  
 * - Desktop: Platform crypto libraries with secure key storage
 * - Web: WebCrypto API with secure key derivation
 */
interface PhotoEncryptionService {
    
    /**
     * Encrypt photo data using AES-256-GCM encryption
     * @param photo Raw photo bytes to encrypt
     * @param photoId Unique identifier for the photo (used in key derivation)
     * @param compressionLevel Optional compression before encryption (0-9)
     * @return Result containing encrypted photo data with metadata
     */
    suspend fun encryptPhoto(
        photo: ByteArray,
        photoId: String,
        compressionLevel: Int = 0
    ): Result<EncryptedPhoto>
    
    /**
     * Decrypt previously encrypted photo data
     * @param encrypted Encrypted photo data with metadata
     * @return Result containing decrypted photo bytes
     */
    suspend fun decryptPhoto(encrypted: EncryptedPhoto): Result<ByteArray>
    
    /**
     * Generate a new encryption key for photo data
     * Uses platform-specific secure random number generation
     * @param keyPurpose Purpose of the key for audit logging
     * @return 256-bit encryption key
     */
    fun generateEncryptionKey(keyPurpose: KeyPurpose = KeyPurpose.PHOTO_ENCRYPTION): ByteArray
    
    /**
     * Encrypt photo thumbnail separately for efficient loading
     * @param thumbnail Thumbnail image bytes
     * @param photoId Associated photo identifier
     * @return Result containing encrypted thumbnail
     */
    suspend fun encryptThumbnail(
        thumbnail: ByteArray,
        photoId: String
    ): Result<EncryptedThumbnail>
    
    /**
     * Decrypt photo thumbnail
     * @param encrypted Encrypted thumbnail data
     * @return Result containing decrypted thumbnail bytes
     */
    suspend fun decryptThumbnail(encrypted: EncryptedThumbnail): Result<ByteArray>
    
    /**
     * Bulk encrypt multiple photos efficiently
     * @param photos List of photo data and identifiers
     * @param progress Optional progress callback for UI updates
     * @return Result containing list of encrypted photos
     */
    suspend fun encryptPhotoBatch(
        photos: List<PhotoToEncrypt>,
        progress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<List<EncryptedPhoto>>
    
    /**
     * Bulk decrypt multiple photos efficiently
     * @param encryptedPhotos List of encrypted photo data
     * @param progress Optional progress callback for UI updates
     * @return Result containing list of decrypted photos
     */
    suspend fun decryptPhotoBatch(
        encryptedPhotos: List<EncryptedPhoto>,
        progress: ((current: Int, total: Int) -> Unit)? = null
    ): Result<List<ByteArray>>
    
    /**
     * Verify the integrity of encrypted photo data
     * @param encrypted Encrypted photo to verify
     * @return true if the photo data is intact and valid
     */
    suspend fun verifyPhotoIntegrity(encrypted: EncryptedPhoto): Boolean
    
    /**
     * Get encryption metrics for monitoring and compliance
     * @return Current encryption service statistics
     */
    suspend fun getEncryptionMetrics(): EncryptionMetrics
    
    /**
     * Rotate encryption keys for enhanced security
     * @param oldKeyId Previous key identifier
     * @return Result containing new key information
     */
    suspend fun rotateEncryptionKey(oldKeyId: String): Result<KeyRotationResult>
}

/**
 * Encrypted photo data with integrity protection
 */
@Serializable
data class EncryptedPhoto(
    val photoId: String,
    val encryptedData: ByteArray,
    val initializationVector: ByteArray,
    val authenticationTag: ByteArray,
    val keyId: String,
    val encryptionAlgorithm: String = "AES-256-GCM",
    val compressionUsed: Boolean = false,
    val originalSize: Long,
    val encryptedAt: Instant,
    val integrity: IntegrityMetadata
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        
        other as EncryptedPhoto
        
        return photoId == other.photoId &&
               encryptedData.contentEquals(other.encryptedData) &&
               initializationVector.contentEquals(other.initializationVector) &&
               authenticationTag.contentEquals(other.authenticationTag) &&
               keyId == other.keyId
    }
    
    override fun hashCode(): Int {
        var result = photoId.hashCode()
        result = 31 * result + encryptedData.contentHashCode()
        result = 31 * result + initializationVector.contentHashCode()
        result = 31 * result + authenticationTag.contentHashCode()
        result = 31 * result + keyId.hashCode()
        return result
    }
}

/**
 * Encrypted thumbnail data for efficient preview loading
 */
@Serializable
data class EncryptedThumbnail(
    val photoId: String,
    val encryptedData: ByteArray,
    val initializationVector: ByteArray,
    val authenticationTag: ByteArray,
    val keyId: String,
    val originalSize: Long,
    val encryptedAt: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        
        other as EncryptedThumbnail
        
        return photoId == other.photoId &&
               encryptedData.contentEquals(other.encryptedData) &&
               initializationVector.contentEquals(other.initializationVector) &&
               authenticationTag.contentEquals(other.authenticationTag)
    }
    
    override fun hashCode(): Int {
        var result = photoId.hashCode()
        result = 31 * result + encryptedData.contentHashCode()
        result = 31 * result + initializationVector.contentHashCode()
        result = 31 * result + authenticationTag.contentHashCode()
        return result
    }
}

/**
 * Photo data to be encrypted
 */
@Serializable
data class PhotoToEncrypt(
    val photoId: String,
    val data: ByteArray,
    val compressionLevel: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        
        other as PhotoToEncrypt
        
        return photoId == other.photoId && data.contentEquals(other.data)
    }
    
    override fun hashCode(): Int {
        var result = photoId.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * Integrity metadata for tamper detection
 */
@Serializable
data class IntegrityMetadata(
    val checksum: String,
    val algorithm: String = "SHA-256",
    val createdAt: Instant
)

/**
 * Encryption service performance and security metrics
 */
@Serializable
data class EncryptionMetrics(
    val totalPhotosEncrypted: Long,
    val totalPhotosDecrypted: Long,
    val averageEncryptionTime: Long, // milliseconds
    val averageDecryptionTime: Long, // milliseconds
    val totalDataEncrypted: Long, // bytes
    val hardwareBackedEncryption: Boolean,
    val encryptionFailures: Long,
    val integrityFailures: Long,
    val lastKeyRotation: Instant?,
    val activeKeyCount: Int
)

/**
 * Result of key rotation operation
 */
@Serializable
data class KeyRotationResult(
    val newKeyId: String,
    val oldKeyId: String,
    val rotatedAt: Instant,
    val photosToReencrypt: Int
)

/**
 * Purpose classification for encryption keys
 */
@Serializable
enum class KeyPurpose {
    /** Primary photo data encryption */
    PHOTO_ENCRYPTION,
    /** Thumbnail encryption */
    THUMBNAIL_ENCRYPTION,
    /** Bulk operation encryption */
    BATCH_ENCRYPTION,
    /** Backup and export encryption */
    BACKUP_ENCRYPTION,
    /** Temporary encryption for processing */
    TEMPORARY_ENCRYPTION
}

/**
 * Encryption algorithm configurations
 */
object EncryptionConfig {
    const val ALGORITHM_AES_GCM = "AES-256-GCM"
    const val KEY_SIZE_BYTES = 32 // 256 bits
    const val IV_SIZE_BYTES = 12 // 96 bits for GCM
    const val TAG_SIZE_BYTES = 16 // 128 bits
    const val MAX_PHOTO_SIZE_BYTES = 50 * 1024 * 1024 // 50MB
    const val COMPRESSION_THRESHOLD_BYTES = 1024 * 1024 // 1MB
}