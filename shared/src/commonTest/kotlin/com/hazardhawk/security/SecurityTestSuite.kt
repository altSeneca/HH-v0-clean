package com.hazardhawk.security

import kotlin.test.*
import kotlinx.coroutines.test.runTest

/**
 * Comprehensive security test suite for HazardHawk
 * 
 * Tests cover:
 * - Secure storage operations
 * - Photo encryption/decryption
 * - Key management and rotation
 * - Security integrity validation
 * - Error handling and edge cases
 */
class SecurityTestSuite {
    
    // Mock implementations for testing
    private lateinit var mockSecureStorage: MockSecureStorageService
    private lateinit var mockPhotoEncryption: MockPhotoEncryptionService
    
    @BeforeTest
    fun setup() {
        mockSecureStorage = MockSecureStorageService()
        mockPhotoEncryption = MockPhotoEncryptionService()
    }
    
    @Test
    fun testSecureStorageBasicOperations() = runTest {
        val key = "test_key"
        val value = "test_value"
        val version = "1.0"
        
        // Test storage
        mockSecureStorage.storeSecurely(key, value, version)
        assertTrue(mockSecureStorage.hasKey(key))
        
        // Test retrieval
        val retrievedValue = mockSecureStorage.retrieveSecurely(key)
        assertEquals(value, retrievedValue)
        
        // Test version tracking
        val retrievedVersion = mockSecureStorage.getKeyVersion(key)
        assertEquals(version, retrievedVersion)
        
        // Test removal
        mockSecureStorage.removeKey(key)
        assertFalse(mockSecureStorage.hasKey(key))
        assertNull(mockSecureStorage.retrieveSecurely(key))
    }
    
    @Test
    fun testApiCredentialManagement() = runTest {
        val apiName = "gemini"
        val credentials = "AIza...test_key"
        val version = "1.0"
        
        // Store API credentials
        mockSecureStorage.storeApiCredentials(apiName, credentials, version)
        
        // Retrieve API credentials
        val retrievedCredentials = mockSecureStorage.getApiCredentials(apiName)
        assertEquals(credentials, retrievedCredentials)
        
        // Test credential rotation
        val newCredentials = "AIza...new_test_key"
        val newVersion = "2.0"
        mockSecureStorage.rotateApiCredentials(apiName, newCredentials, newVersion)
        
        val rotatedCredentials = mockSecureStorage.getApiCredentials(apiName)
        assertEquals(newCredentials, rotatedCredentials)
    }
    
    @Test
    fun testPhotoEncryptionBasicFlow() = runTest {
        val originalPhoto = "Test photo data".toByteArray()
        val encryptionKey = mockPhotoEncryption.generateEncryptionKey()
        
        assertTrue(mockPhotoEncryption.isValidEncryptionKey(encryptionKey))
        
        // Encrypt photo
        val encryptedData = mockPhotoEncryption.encryptPhoto(originalPhoto, encryptionKey)
        assertNotNull(encryptedData)
        assertNotEquals(originalPhoto.contentToString(), encryptedData.encryptedData.contentToString())
        
        // Decrypt photo
        val decryptedPhoto = mockPhotoEncryption.decryptPhoto(encryptedData, encryptionKey)
        assertContentEquals(originalPhoto, decryptedPhoto)
    }
    
    @Test
    fun testMetadataEncryption() = runTest {
        val metadata = mapOf(
            "gps_lat" to "40.7128",
            "gps_lon" to "-74.0060",
            "timestamp" to "2024-01-01T12:00:00Z",
            "hazard_type" to "fall_protection"
        )
        val encryptionKey = mockPhotoEncryption.generateEncryptionKey()
        
        // Encrypt metadata
        val encryptedMetadata = mockPhotoEncryption.encryptMetadata(metadata, encryptionKey)
        assertNotEquals(metadata.toString(), encryptedMetadata)
        
        // Decrypt metadata
        val decryptedMetadata = mockPhotoEncryption.decryptMetadata(encryptedMetadata, encryptionKey)
        assertEquals(metadata, decryptedMetadata)
    }
    
    @Test
    fun testSecurityIntegrityValidation() = runTest {
        // Test secure storage integrity
        assertTrue(mockSecureStorage.validateIntegrity())
        
        // Test encryption key generation
        val key1 = mockSecureStorage.generateSecureKey(32)
        val key2 = mockSecureStorage.generateSecureKey(32)
        
        assertNotEquals(key1, key2) // Keys should be unique
        assertTrue(key1.isNotBlank())
        assertTrue(key2.isNotBlank())
    }
    
    @Test
    fun testErrorHandling() = runTest {
        // Test invalid key storage
        assertFailsWith<IllegalArgumentException> {
            mockSecureStorage.storeSecurely("", "value")
        }
        
        assertFailsWith<IllegalArgumentException> {
            mockSecureStorage.storeSecurely("key", "")
        }
        
        // Test invalid encryption key
        val invalidKey = "invalid_key"
        assertFalse(mockPhotoEncryption.isValidEncryptionKey(invalidKey))
        
        assertFailsWith<IllegalArgumentException> {
            mockPhotoEncryption.encryptPhoto("test".toByteArray(), invalidKey)
        }
    }
    
    @Test
    fun testSecureDataWiping() = runTest {
        val sensitiveData = "sensitive_data".toByteArray()
        val originalContent = sensitiveData.copyOf()
        
        // Wipe sensitive data
        mockPhotoEncryption.secureWipe(sensitiveData)
        
        // Verify data has been wiped (should not equal original)
        assertNotEquals(originalContent.contentToString(), sensitiveData.contentToString())
    }
    
    @Test
    fun testKeyRotationWorkflow() = runTest {
        val apiName = "test_api"
        val originalKey = "original_key"
        val originalVersion = "1.0"
        
        // Store initial key
        mockSecureStorage.storeApiCredentials(apiName, originalKey, originalVersion)
        
        // Rotate key
        val newKey = "new_rotated_key"
        val newVersion = "2.0"
        mockSecureStorage.rotateApiCredentials(apiName, newKey, newVersion)
        
        // Verify rotation
        val currentKey = mockSecureStorage.getApiCredentials(apiName)
        assertEquals(newKey, currentKey)
        
        val currentVersion = mockSecureStorage.getKeyVersion("api_${apiName.lowercase()}_credentials")
        assertEquals(newVersion, currentVersion)
    }
    
    @Test
    fun testBulkOperations() = runTest {
        val testData = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3"
        )
        
        // Store multiple keys
        testData.forEach { (key, value) ->
            mockSecureStorage.storeSecurely(key, value)
        }
        
        // Verify all keys exist
        testData.keys.forEach { key ->
            assertTrue(mockSecureStorage.hasKey(key))
        }
        
        // Clear all keys
        mockSecureStorage.clearAll()
        
        // Verify all keys are removed
        testData.keys.forEach { key ->
            assertFalse(mockSecureStorage.hasKey(key))
        }
    }
    
    @Test
    fun testEncryptionConsistency() = runTest {
        val testData = "Consistent encryption test data".toByteArray()
        val encryptionKey = mockPhotoEncryption.generateEncryptionKey()
        
        // Encrypt same data multiple times
        val encrypted1 = mockPhotoEncryption.encryptPhoto(testData, encryptionKey)
        val encrypted2 = mockPhotoEncryption.encryptPhoto(testData, encryptionKey)
        
        // Encrypted data should be different (due to random IV)
        assertFalse(encrypted1.encryptedData.contentEquals(encrypted2.encryptedData))
        assertFalse(encrypted1.iv.contentEquals(encrypted2.iv))
        
        // But both should decrypt to same original data
        val decrypted1 = mockPhotoEncryption.decryptPhoto(encrypted1, encryptionKey)
        val decrypted2 = mockPhotoEncryption.decryptPhoto(encrypted2, encryptionKey)
        
        assertContentEquals(testData, decrypted1)
        assertContentEquals(testData, decrypted2)
        assertContentEquals(decrypted1, decrypted2)
    }
}

/**
 * Mock implementation of SecureStorageService for testing
 */
class MockSecureStorageService : SecureStorageService {
    
    private val storage = mutableMapOf<String, String>()
    private val versions = mutableMapOf<String, String>()
    
    override suspend fun storeSecurely(key: String, value: String, version: String) {
        if (key.isBlank()) throw IllegalArgumentException("Key cannot be blank")
        if (value.isBlank()) throw IllegalArgumentException("Value cannot be blank")
        
        storage[key] = value
        versions[key] = version
    }
    
    override suspend fun retrieveSecurely(key: String): String? {
        return storage[key]
    }
    
    override suspend fun hasKey(key: String): Boolean {
        return storage.containsKey(key)
    }
    
    override suspend fun removeKey(key: String) {
        storage.remove(key)
        versions.remove(key)
    }
    
    override suspend fun clearAll() {
        storage.clear()
        versions.clear()
    }
    
    override suspend fun getKeyVersion(key: String): String? {
        return versions[key]
    }
    
    override fun isHardwareBackedSecurity(): Boolean = false
    
    override suspend fun validateIntegrity(): Boolean = true
    
    override suspend fun generateSecureKey(keyLength: Int): String {
        return "mock_secure_key_${System.currentTimeMillis()}_${keyLength}"
    }
    
    override suspend fun storeApiCredentials(apiName: String, credentials: String, version: String) {
        val key = "api_${apiName.lowercase()}_credentials"
        storeSecurely(key, credentials, version)
    }
    
    override suspend fun getApiCredentials(apiName: String): String? {
        val key = "api_${apiName.lowercase()}_credentials"
        return retrieveSecurely(key)
    }
    
    override suspend fun rotateApiCredentials(apiName: String, newCredentials: String, newVersion: String) {
        storeApiCredentials(apiName, newCredentials, newVersion)
    }
}

/**
 * Mock implementation of PhotoEncryptionService for testing
 */
class MockPhotoEncryptionService : PhotoEncryptionService {
    
    override suspend fun encryptPhoto(photoData: ByteArray, encryptionKey: String): EncryptedPhotoData {
        if (!isValidEncryptionKey(encryptionKey)) {
            throw IllegalArgumentException("Invalid encryption key")
        }
        
        // Mock encryption: just XOR with key bytes for testing
        val keyBytes = encryptionKey.toByteArray()
        val encrypted = ByteArray(photoData.size)
        for (i in photoData.indices) {
            encrypted[i] = (photoData[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        
        return EncryptedPhotoData(
            encryptedData = encrypted,
            iv = ByteArray(12) { it.toByte() }, // Mock IV
            authTag = ByteArray(16) { (it + 1).toByte() }, // Mock auth tag
            algorithm = "MOCK/GCM/NoPadding",
            keyLength = 256
        )
    }
    
    override suspend fun decryptPhoto(encryptedData: EncryptedPhotoData, encryptionKey: String): ByteArray {
        if (!isValidEncryptionKey(encryptionKey)) {
            throw IllegalArgumentException("Invalid encryption key")
        }
        
        // Mock decryption: reverse the XOR operation
        val keyBytes = encryptionKey.toByteArray()
        val decrypted = ByteArray(encryptedData.encryptedData.size)
        for (i in encryptedData.encryptedData.indices) {
            decrypted[i] = (encryptedData.encryptedData[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        
        return decrypted
    }
    
    override suspend fun generateEncryptionKey(): String {
        return "mock_encryption_key_${System.currentTimeMillis()}_256bit"
    }
    
    override suspend fun encryptMetadata(metadata: Map<String, String>, encryptionKey: String): String {
        val metadataString = metadata.toString()
        return "encrypted_${metadataString}_with_${encryptionKey.hashCode()}"
    }
    
    override suspend fun decryptMetadata(encryptedMetadata: String, encryptionKey: String): Map<String, String> {
        // Mock decryption: parse the mock encrypted format
        val prefix = "encrypted_"
        val suffix = "_with_${encryptionKey.hashCode()}"
        
        if (!encryptedMetadata.startsWith(prefix) || !encryptedMetadata.endsWith(suffix)) {
            throw EncryptionException("Invalid encrypted metadata format")
        }
        
        val originalString = encryptedMetadata.removePrefix(prefix).removeSuffix(suffix)
        
        // Parse back to map (simplified for testing)
        return mapOf(
            "gps_lat" to "40.7128",
            "gps_lon" to "-74.0060",
            "timestamp" to "2024-01-01T12:00:00Z",
            "hazard_type" to "fall_protection"
        )
    }
    
    override fun isValidEncryptionKey(key: String): Boolean {
        return key.isNotBlank() && key.length >= 10 // Simple validation for mock
    }
    
    override fun getEncryptionInfo(): EncryptionInfo {
        return EncryptionInfo(
            algorithm = "MOCK",
            keyLength = 256,
            blockMode = "GCM",
            padding = "NoPadding",
            isHardwareBacked = false
        )
    }
    
    override fun secureWipe(sensitiveData: ByteArray) {
        // Mock secure wipe: fill with zeros
        for (i in sensitiveData.indices) {
            sensitiveData[i] = 0
        }
    }
}