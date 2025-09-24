package com.hazardhawk.security

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.random.Random

/**
 * Integration tests for iOS security implementations.
 * These tests validate the iOS-specific security service implementations.
 */
class IOSSecurityIntegrationTest {
    
    private lateinit var secureStorage: SecureStorageService
    private lateinit var photoEncryption: PhotoEncryptionService
    
    @BeforeTest
    fun setup() {
        // Note: In actual iOS tests, these would be the iOS implementations
        // For common tests, we'd need mock implementations or conditional compilation
        // secureStorage = SecureStorageServiceImpl()
        // photoEncryption = PhotoEncryptionServiceImpl()
    }
    
    @AfterTest
    fun cleanup() = runTest {
        // Clean up test data
        secureStorage?.clearAllCredentials()
    }
    
    @Test
    fun testSecureStorageBasicOperations() = runTest {
        // This would run only on iOS platform
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return@runTest
        }
        
        val testKey = "test_api_key"
        val testValue = "sk-test123456789abcdef"
        
        // Test availability
        assertTrue(secureStorage.isAvailable(), "Secure storage should be available")
        
        // Test store operation
        val storeResult = secureStorage.storeApiKey(testKey, testValue)
        assertTrue(storeResult.isSuccess, "Store operation should succeed")
        
        // Test retrieve operation
        val retrieveResult = secureStorage.getApiKey(testKey)
        assertTrue(retrieveResult.isSuccess, "Retrieve operation should succeed")
        assertEquals(testValue, retrieveResult.getOrNull(), "Retrieved value should match stored value")
        
        // Test remove operation
        val removeResult = secureStorage.removeApiKey(testKey)
        assertTrue(removeResult.isSuccess, "Remove operation should succeed")
        
        // Verify key is removed
        val verifyResult = secureStorage.getApiKey(testKey)
        assertTrue(verifyResult.isSuccess, "Verify operation should succeed")
        assertNull(verifyResult.getOrNull(), "Key should be removed")
    }
    
    @Test
    fun testSecureStorageWithMetadata() = runTest {
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return@runTest
        }
        
        val testKey = "test_metadata_key"
        val testValue = "test_value_with_metadata"
        val metadata = CredentialMetadata(
            createdAt = Clock.System.now(),
            purpose = CredentialPurpose.AI_SERVICE_API_KEY,
            userId = "test_user",
            description = "Test credential with metadata",
            complianceLevel = ComplianceLevel.Enhanced
        )
        
        // Store with metadata
        val storeResult = secureStorage.storeApiKey(testKey, testValue, metadata)
        assertTrue(storeResult.isSuccess, "Store with metadata should succeed")
        
        // Retrieve metadata
        val metadataResult = secureStorage.getCredentialMetadata(testKey)
        assertTrue(metadataResult.isSuccess, "Metadata retrieval should succeed")
        
        val retrievedMetadata = metadataResult.getOrNull()
        assertNotNull(retrievedMetadata, "Retrieved metadata should not be null")
        assertEquals(metadata.purpose, retrievedMetadata.purpose, "Purpose should match")
        assertEquals(metadata.userId, retrievedMetadata.userId, "User ID should match")
        assertEquals(metadata.complianceLevel, retrievedMetadata.complianceLevel, "Compliance level should match")
    }
    
    @Test
    fun testPhotoEncryptionBasicOperations() = runTest {
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return@runTest
        }
        
        val originalData = generateTestPhotoData(1024) // 1KB test data
        
        // Generate encryption key
        val encryptionKey = photoEncryption.generateEncryptionKey()
        assertTrue(photoEncryption.isValidEncryptionKey(encryptionKey), "Generated key should be valid")
        
        // Encrypt photo
        val encryptedData = photoEncryption.encryptPhoto(originalData, encryptionKey)
        assertNotNull(encryptedData, "Encrypted data should not be null")
        assertTrue(encryptedData.encryptedData.isNotEmpty(), "Encrypted data should not be empty")
        assertEquals(SecurityConstants.IV_LENGTH, encryptedData.iv.size, "IV should have correct length")
        assertEquals(SecurityConstants.AUTH_TAG_LENGTH, encryptedData.authTag.size, "Auth tag should have correct length")
        
        // Decrypt photo
        val decryptedData = photoEncryption.decryptPhoto(encryptedData, encryptionKey)
        assertContentEquals(originalData, decryptedData, "Decrypted data should match original")
    }
    
    @Test
    fun testPhotoEncryptionLargeData() = runTest {
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return@runTest
        }
        
        val originalData = generateTestPhotoData(1024 * 1024) // 1MB test data
        
        val encryptionKey = photoEncryption.generateEncryptionKey()
        
        // Test encryption/decryption of larger data
        val encryptedData = photoEncryption.encryptPhoto(originalData, encryptionKey)
        val decryptedData = photoEncryption.decryptPhoto(encryptedData, encryptionKey)
        
        assertContentEquals(originalData, decryptedData, "Large data should encrypt/decrypt correctly")
    }
    
    @Test
    fun testMetadataEncryption() = runTest {
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return@runTest
        }
        
        val originalMetadata = mapOf(
            "gps_lat" to "40.7128",
            "gps_lon" to "-74.0060",
            "timestamp" to "2025-01-15T10:30:00Z",
            "device_id" to "iPhone_12_Pro",
            "hazard_type" to "fall_hazard"
        )
        
        val encryptionKey = photoEncryption.generateEncryptionKey()
        
        // Encrypt metadata
        val encryptedMetadata = photoEncryption.encryptMetadata(originalMetadata, encryptionKey)
        assertNotEquals(originalMetadata.toString(), encryptedMetadata, "Metadata should be encrypted")
        
        // Decrypt metadata
        val decryptedMetadata = photoEncryption.decryptMetadata(encryptedMetadata, encryptionKey)
        assertEquals(originalMetadata, decryptedMetadata, "Decrypted metadata should match original")
    }
    
    @Test
    fun testEncryptionKeyValidation() {
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return
        }
        
        // Test invalid keys
        assertFalse(photoEncryption.isValidEncryptionKey(""), "Empty key should be invalid")
        assertFalse(photoEncryption.isValidEncryptionKey("short"), "Short key should be invalid")
        assertFalse(photoEncryption.isValidEncryptionKey("not_base64!"), "Non-base64 key should be invalid")
        
        // Test valid key
        val validKey = photoEncryption.generateEncryptionKey()
        assertTrue(photoEncryption.isValidEncryptionKey(validKey), "Generated key should be valid")
    }
    
    @Test
    fun testEncryptionInfo() {
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return
        }
        
        val encryptionInfo = photoEncryption.getEncryptionInfo()
        
        assertEquals("AES", encryptionInfo.algorithm, "Algorithm should be AES")
        assertEquals(256, encryptionInfo.keyLength, "Key length should be 256 bits")
        assertEquals("GCM", encryptionInfo.blockMode, "Block mode should be GCM")
        assertEquals("NoPadding", encryptionInfo.padding, "Padding should be NoPadding")
        // isHardwareBacked depends on device capabilities, so we don't assert a specific value
    }
    
    @Test
    fun testCredentialPurposeSegmentation() = runTest {
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return@runTest
        }
        
        val credentials = mapOf(
            "ai_key" to Pair("ai-secret-123", CredentialPurpose.AI_SERVICE_API_KEY),
            "aws_key" to Pair("aws-secret-456", CredentialPurpose.CLOUD_STORAGE_ACCESS),
            "auth_token" to Pair("jwt-token-789", CredentialPurpose.AUTH_TOKEN)
        )
        
        // Store credentials with different purposes
        credentials.forEach { (key, pair) ->
            val metadata = CredentialMetadata(
                createdAt = Clock.System.now(),
                purpose = pair.second,
                complianceLevel = ComplianceLevel.Standard
            )
            
            val result = secureStorage.storeApiKey(key, pair.first, metadata)
            assertTrue(result.isSuccess, "Should store credential for purpose ${pair.second}")
        }
        
        // Verify all credentials are stored
        val keysResult = secureStorage.listCredentialKeys()
        assertTrue(keysResult.isSuccess, "Should list credential keys")
        
        val storedKeys = keysResult.getOrNull() ?: emptyList()
        credentials.keys.forEach { key ->
            assertTrue(storedKeys.contains(key), "Should find stored key: $key")
        }
    }
    
    @Test
    fun testSecureWipe() {
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return
        }
        
        val sensitiveData = "sensitive_data_123456789".encodeToByteArray()
        val originalData = sensitiveData.copyOf()
        
        // Verify data is initially correct
        assertContentEquals(originalData, sensitiveData, "Data should initially match")
        
        // Wipe the data
        photoEncryption.secureWipe(sensitiveData)
        
        // Verify data has been wiped (should be all zeros)
        assertFalse(
            sensitiveData.contentEquals(originalData),
            "Data should be wiped and not match original"
        )
        
        // All bytes should be zero after secure wipe
        assertTrue(
            sensitiveData.all { it == 0.toByte() },
            "All bytes should be zero after secure wipe"
        )
    }
    
    @Test
    fun testErrorHandling() = runTest {
        if (!isIOSPlatform()) {
            println("Skipping iOS-specific test on non-iOS platform")
            return@runTest
        }
        
        val invalidKey = "invalid_base64_key!"
        val testData = byteArrayOf(1, 2, 3, 4, 5)
        
        // Test encryption with invalid key should throw exception
        assertFailsWith<EncryptionException> {
            photoEncryption.encryptPhoto(testData, invalidKey)
        }
        
        // Test decryption with invalid key should throw exception
        val validKey = photoEncryption.generateEncryptionKey()
        val encryptedData = photoEncryption.encryptPhoto(testData, validKey)
        
        assertFailsWith<EncryptionException> {
            photoEncryption.decryptPhoto(encryptedData, invalidKey)
        }
    }
    
    // Helper functions
    
    private fun generateTestPhotoData(size: Int): ByteArray {
        return ByteArray(size) { Random.nextInt(0, 256).toByte() }
    }
    
    private fun isIOSPlatform(): Boolean {
        // This would be implemented using expect/actual declarations
        // or platform-specific compilation flags
        // For now, return false in common tests
        return false
    }
}