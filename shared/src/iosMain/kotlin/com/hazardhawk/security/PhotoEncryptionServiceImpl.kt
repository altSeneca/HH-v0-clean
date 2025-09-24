package com.hazardhawk.security

import kotlinx.cinterop.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.CommonCrypto.*
import kotlin.experimental.xor
import kotlin.random.Random

/**
 * iOS implementation of PhotoEncryptionService using iOS Security framework and CommonCrypto.
 * Provides AES-256-GCM encryption with hardware-backed security when available.
 */
class PhotoEncryptionServiceImpl : PhotoEncryptionService {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // Check if hardware-backed crypto is available
    private val isHardwareBacked: Boolean by lazy {
        // Check for Secure Enclave availability
        val query = NSMutableDictionary().apply {
            this[kSecAttrTokenID] = kSecAttrTokenIDSecureEnclave
            this[kSecClass] = kSecClassKey
        }
        
        val status = SecItemCopyMatching(query, null)
        status != errSecUnimplemented
    }
    
    override suspend fun encryptPhoto(
        photoData: ByteArray, 
        encryptionKey: String
    ): EncryptedPhotoData {
        try {
            if (!isValidEncryptionKey(encryptionKey)) {
                throw EncryptionException("Invalid encryption key format or length")
            }
            
            val keyData = NSData.create(base64EncodedString = encryptionKey, options = 0u)
                ?: throw EncryptionException("Failed to decode encryption key")
            
            // Generate random IV (12 bytes for GCM)
            val iv = generateRandomBytes(SecurityConstants.IV_LENGTH)
            val authTag = ByteArray(SecurityConstants.AUTH_TAG_LENGTH)
            
            return memScoped {
                val keyBytes = ByteArray(keyData.length.toInt())
                keyData.getBytes(keyBytes.refTo(0).getPointer(this), keyData.length)
                
                val encryptedData = encryptAESGCM(
                    plaintext = photoData,
                    key = keyBytes,
                    iv = iv,
                    authTag = authTag
                )
                
                // Securely wipe key from memory
                secureWipe(keyBytes)
                
                EncryptedPhotoData(
                    encryptedData = encryptedData,
                    iv = iv,
                    authTag = authTag,
                    algorithm = SecurityConstants.ENCRYPTION_ALGORITHM,
                    keyLength = SecurityConstants.MIN_KEY_LENGTH
                )
            }
        } catch (e: Exception) {
            throw EncryptionException("Photo encryption failed: ${e.message}", e)
        }
    }
    
    override suspend fun decryptPhoto(
        encryptedData: EncryptedPhotoData, 
        encryptionKey: String
    ): ByteArray {
        try {
            if (!isValidEncryptionKey(encryptionKey)) {
                throw EncryptionException("Invalid encryption key format or length")
            }
            
            val keyData = NSData.create(base64EncodedString = encryptionKey, options = 0u)
                ?: throw EncryptionException("Failed to decode encryption key")
            
            return memScoped {
                val keyBytes = ByteArray(keyData.length.toInt())
                keyData.getBytes(keyBytes.refTo(0).getPointer(this), keyData.length)
                
                val decryptedData = decryptAESGCM(
                    ciphertext = encryptedData.encryptedData,
                    key = keyBytes,
                    iv = encryptedData.iv,
                    authTag = encryptedData.authTag
                )
                
                // Securely wipe key from memory
                secureWipe(keyBytes)
                
                decryptedData
            }
        } catch (e: Exception) {
            throw EncryptionException("Photo decryption failed: ${e.message}", e)
        }
    }
    
    override suspend fun generateEncryptionKey(): String {
        val keyBytes = generateRandomBytes(SecurityConstants.AES_KEY_LENGTH)
        val nsData = keyBytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = keyBytes.size.toULong())
        }
        
        val base64Key = nsData.base64EncodedStringWithOptions(0u)
        
        // Securely wipe key from memory
        secureWipe(keyBytes)
        
        return base64Key
    }
    
    override suspend fun encryptMetadata(
        metadata: Map<String, String>, 
        encryptionKey: String
    ): String {
        try {
            val metadataJson = json.encodeToString(metadata)
            val metadataBytes = metadataJson.encodeToByteArray()
            
            val encryptedMetadata = encryptPhoto(metadataBytes, encryptionKey)
            
            // Serialize encrypted metadata to JSON string
            return json.encodeToString(encryptedMetadata)
        } catch (e: Exception) {
            throw EncryptionException("Metadata encryption failed: ${e.message}", e)
        }
    }
    
    override suspend fun decryptMetadata(
        encryptedMetadata: String, 
        encryptionKey: String
    ): Map<String, String> {
        try {
            val encryptedData = json.decodeFromString<EncryptedPhotoData>(encryptedMetadata)
            val decryptedBytes = decryptPhoto(encryptedData, encryptionKey)
            val metadataJson = decryptedBytes.decodeToString()
            
            return json.decodeFromString<Map<String, String>>(metadataJson)
        } catch (e: Exception) {
            throw EncryptionException("Metadata decryption failed: ${e.message}", e)
        }
    }
    
    override fun isValidEncryptionKey(key: String): Boolean {
        return try {
            val keyData = NSData.create(base64EncodedString = key, options = 0u)
            keyData?.length?.toInt() == SecurityConstants.AES_KEY_LENGTH
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getEncryptionInfo(): EncryptionInfo {
        return EncryptionInfo(
            algorithm = "AES",
            keyLength = SecurityConstants.MIN_KEY_LENGTH,
            blockMode = "GCM",
            padding = "NoPadding",
            isHardwareBacked = isHardwareBacked
        )
    }
    
    override fun secureWipe(sensitiveData: ByteArray) {
        // Overwrite with random data multiple times for secure deletion
        repeat(3) {
            for (i in sensitiveData.indices) {
                sensitiveData[i] = Random.nextInt(0, 256).toByte()
            }
        }
        
        // Final pass with zeros
        sensitiveData.fill(0)
    }
    
    /**
     * Generate cryptographically secure random bytes using iOS SecRandomCopyBytes
     */
    private fun generateRandomBytes(length: Int): ByteArray {
        val randomBytes = ByteArray(length)
        randomBytes.usePinned { pinned ->
            val status = SecRandomCopyBytes(kSecRandomDefault, length.toULong(), pinned.addressOf(0))
            if (status != errSecSuccess) {
                throw EncryptionException("Failed to generate secure random bytes: status $status")
            }
        }
        return randomBytes
    }
    
    /**
     * Encrypt data using AES-GCM with CommonCrypto
     */
    private fun encryptAESGCM(
        plaintext: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        authTag: ByteArray
    ): ByteArray {
        return memScoped {
            val cryptorRef = alloc<CCCryptorRefVar>()
            
            // Create GCM cryptor
            val createStatus = CCCryptorCreateWithMode(
                kCCEncrypt,
                kCCModeGCM,
                kCCAlgorithmAES,
                ccNoPadding,
                iv.refTo(0).getPointer(this),
                key.refTo(0).getPointer(this),
                key.size.toULong(),
                null,
                0u,
                0u,
                0u,
                cryptorRef.ptr
            )
            
            if (createStatus != kCCSuccess) {
                throw EncryptionException("Failed to create AES-GCM encryptor: status $createStatus")
            }
            
            try {
                val ciphertext = ByteArray(plaintext.size)
                val ciphertextLength = alloc<size_tVar>()
                
                // Encrypt the data
                val updateStatus = CCCryptorUpdate(
                    cryptorRef.value,
                    plaintext.refTo(0).getPointer(this),
                    plaintext.size.toULong(),
                    ciphertext.refTo(0).getPointer(this),
                    ciphertext.size.toULong(),
                    ciphertextLength.ptr
                )
                
                if (updateStatus != kCCSuccess) {
                    throw EncryptionException("AES-GCM encryption failed: status $updateStatus")
                }
                
                // Finalize and get auth tag
                val finalLength = alloc<size_tVar>()
                val finalStatus = CCCryptorFinal(
                    cryptorRef.value,
                    null,
                    0u,
                    finalLength.ptr
                )
                
                if (finalStatus != kCCSuccess) {
                    throw EncryptionException("AES-GCM finalization failed: status $finalStatus")
                }
                
                // Get the authentication tag
                val tagLength = alloc<size_tVar>()
                tagLength.value = authTag.size.toULong()
                
                val tagStatus = CCCryptorGCMGetTag(
                    cryptorRef.value,
                    authTag.refTo(0).getPointer(this),
                    tagLength.ptr
                )
                
                if (tagStatus != kCCSuccess) {
                    throw EncryptionException("Failed to get GCM authentication tag: status $tagStatus")
                }
                
                ciphertext
            } finally {
                CCCryptorRelease(cryptorRef.value)
            }
        }
    }
    
    /**
     * Decrypt data using AES-GCM with CommonCrypto
     */
    private fun decryptAESGCM(
        ciphertext: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        authTag: ByteArray
    ): ByteArray {
        return memScoped {
            val cryptorRef = alloc<CCCryptorRefVar>()
            
            // Create GCM decryptor
            val createStatus = CCCryptorCreateWithMode(
                kCCDecrypt,
                kCCModeGCM,
                kCCAlgorithmAES,
                ccNoPadding,
                iv.refTo(0).getPointer(this),
                key.refTo(0).getPointer(this),
                key.size.toULong(),
                null,
                0u,
                0u,
                0u,
                cryptorRef.ptr
            )
            
            if (createStatus != kCCSuccess) {
                throw EncryptionException("Failed to create AES-GCM decryptor: status $createStatus")
            }
            
            try {
                // Set the authentication tag
                val tagStatus = CCCryptorGCMSetTag(
                    cryptorRef.value,
                    authTag.refTo(0).getPointer(this),
                    authTag.size.toULong()
                )
                
                if (tagStatus != kCCSuccess) {
                    throw EncryptionException("Failed to set GCM authentication tag: status $tagStatus")
                }
                
                val plaintext = ByteArray(ciphertext.size)
                val plaintextLength = alloc<size_tVar>()
                
                // Decrypt the data
                val updateStatus = CCCryptorUpdate(
                    cryptorRef.value,
                    ciphertext.refTo(0).getPointer(this),
                    ciphertext.size.toULong(),
                    plaintext.refTo(0).getPointer(this),
                    plaintext.size.toULong(),
                    plaintextLength.ptr
                )
                
                if (updateStatus != kCCSuccess) {
                    throw EncryptionException("AES-GCM decryption failed: status $updateStatus")
                }
                
                // Finalize decryption
                val finalLength = alloc<size_tVar>()
                val finalStatus = CCCryptorFinal(
                    cryptorRef.value,
                    null,
                    0u,
                    finalLength.ptr
                )
                
                if (finalStatus != kCCSuccess) {
                    throw EncryptionException("AES-GCM decryption finalization failed: status $finalStatus")
                }
                
                // Return only the actual decrypted data
                plaintext.copyOf(plaintextLength.value.toInt())
            } finally {
                CCCryptorRelease(cryptorRef.value)
            }
        }
    }
}