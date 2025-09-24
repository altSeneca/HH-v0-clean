package com.hazardhawk.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Android-specific security integration tests
 * 
 * These tests run on actual Android devices/emulators to validate:
 * - Android Keystore integration
 * - EncryptedSharedPreferences functionality
 * - Hardware-backed security when available
 * - Real AES-256-GCM encryption/decryption
 */
@RunWith(AndroidJUnit4::class)
class AndroidSecurityIntegrationTest {
    
    private lateinit var context: Context
    private lateinit var secureStorage: SecureStorageService
    private lateinit var photoEncryption: PhotoEncryptionService
    private lateinit var securityPlatform: SecurityPlatform
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        securityPlatform = SecurityPlatform(context)
        secureStorage = securityPlatform.createSecureStorageService()
        photoEncryption = securityPlatform.createPhotoEncryptionService()
    }
    
    @Test
    fun testAndroidSecureStorageIntegration() = runTest {
        val testKey = "integration_test_key_${System.currentTimeMillis()}"
        val testValue = "integration_test_value_with_special_chars_!@#$%^&*()"
        val version = "1.0"
        
        try {
            // Test storage
            secureStorage.storeSecurely(testKey, testValue, version)
            assertTrue(secureStorage.hasKey(testKey))
            
            // Test retrieval
            val retrievedValue = secureStorage.retrieveSecurely(testKey)
            assertEquals(testValue, retrievedValue)
            
            // Test version
            val retrievedVersion = secureStorage.getKeyVersion(testKey)
            assertEquals(version, retrievedVersion)
            
        } finally {
            // Cleanup
            secureStorage.removeKey(testKey)
        }
    }
    
    @Test
    fun testAndroidPhotoEncryptionIntegration() = runTest {
        // Create test photo data
        val originalPhoto = createTestPhotoData()
        val encryptionKey = photoEncryption.generateEncryptionKey()
        
        // Validate key
        assertTrue(photoEncryption.isValidEncryptionKey(encryptionKey))
        
        // Encrypt photo
        val encryptedData = photoEncryption.encryptPhoto(originalPhoto, encryptionKey)
        assertNotNull(encryptedData)
        assertEquals(SecurityConstants.ENCRYPTION_ALGORITHM, encryptedData.algorithm)
        assertEquals(SecurityConstants.MIN_KEY_LENGTH, encryptedData.keyLength)
        assertEquals(SecurityConstants.IV_LENGTH, encryptedData.iv.size)
        assertEquals(SecurityConstants.AUTH_TAG_LENGTH, encryptedData.authTag.size)
        
        // Verify data is actually encrypted (not same as original)
        assertFalse(originalPhoto.contentEquals(encryptedData.encryptedData))
        
        // Decrypt photo
        val decryptedPhoto = photoEncryption.decryptPhoto(encryptedData, encryptionKey)
        assertContentEquals(originalPhoto, decryptedPhoto)
    }
    
    @Test
    fun testAndroidKeystoreCapabilities() {
        val capabilities = securityPlatform.getSecurityCapabilities()
        
        // Verify platform information
        assertEquals("Android", capabilities["platform"])
        assertTrue(capabilities.containsKey("apiLevel"))
        assertTrue(capabilities.containsKey("hardwareBacked"))
        assertTrue(capabilities.containsKey("strongBoxAvailable"))
        assertTrue(capabilities.containsKey("deviceSecure"))
        
        // Log capabilities for debugging
        println("Android Security Capabilities:")
        capabilities.forEach { (key, value) ->
            println("  $key: $value")
        }
    }
    
    @Test
    fun testAndroidSecurityManagerIntegration() = runTest {
        val securityManager = SecurityManager(securityPlatform)
        
        // Initialize security manager
        securityManager.initialize()
        
        // Get security status
        val securityStatus = securityManager.getSecurityStatus()
        assertTrue(securityStatus.encryptionAvailable)
        assertTrue(securityStatus.storageIntegrity)
        assertTrue(securityStatus.isSecure)
        
        // Log security status
        println("Android Security Status:")
        println("  Hardware Backed: ${securityStatus.hardwareBacked}")
        println("  Encryption Available: ${securityStatus.encryptionAvailable}")
        println("  Storage Integrity: ${securityStatus.storageIntegrity}")
        println("  Is Secure: ${securityStatus.isSecure}")
    }
    
    @Test
    fun testRealWorldApiCredentialWorkflow() = runTest {
        val apiName = "gemini"
        val fakeApiKey = "AIzaSyC-test_key_for_integration_testing_${System.currentTimeMillis()}"
        val version = "test_v1.0"
        
        try {
            // Store API credentials
            secureStorage.storeApiCredentials(apiName, fakeApiKey, version)
            
            // Retrieve API credentials
            val retrievedKey = secureStorage.getApiCredentials(apiName)
            assertEquals(fakeApiKey, retrievedKey)
            
            // Test credential rotation
            val newFakeApiKey = "AIzaSyC-new_test_key_for_rotation_${System.currentTimeMillis()}"
            val newVersion = "test_v2.0"
            secureStorage.rotateApiCredentials(apiName, newFakeApiKey, newVersion)
            
            // Verify rotation
            val rotatedKey = secureStorage.getApiCredentials(apiName)
            assertEquals(newFakeApiKey, rotatedKey)
            
        } finally {
            // Cleanup test data
            secureStorage.removeKey("api_${apiName}_credentials")
        }
    }
    
    @Test
    fun testLargePhotoEncryption() = runTest {
        // Test with larger photo data (simulate real photo size)
        val largePhotoData = createLargeTestPhotoData(1024 * 1024) // 1MB
        val encryptionKey = photoEncryption.generateEncryptionKey()
        
        // Measure encryption time
        val startTime = System.currentTimeMillis()
        val encryptedData = photoEncryption.encryptPhoto(largePhotoData, encryptionKey)
        val encryptionTime = System.currentTimeMillis() - startTime
        
        println("Large photo encryption time: ${encryptionTime}ms")
        
        // Verify encryption worked
        assertNotNull(encryptedData)
        assertFalse(largePhotoData.contentEquals(encryptedData.encryptedData))
        
        // Measure decryption time
        val decryptStartTime = System.currentTimeMillis()
        val decryptedData = photoEncryption.decryptPhoto(encryptedData, encryptionKey)
        val decryptionTime = System.currentTimeMillis() - decryptStartTime
        
        println("Large photo decryption time: ${decryptionTime}ms")
        
        // Verify decryption worked
        assertContentEquals(largePhotoData, decryptedData)
    }
    
    @Test
    fun testMetadataEncryptionWithRealData() = runTest {
        val realMetadata = mapOf(
            "gps_latitude" to "40.712776",
            "gps_longitude" to "-74.005974",
            "timestamp" to "2024-01-15T10:30:00Z",
            "camera_make" to "Google",
            "camera_model" to "Pixel 7",
            "hazard_types" to "fall_protection,electrical_hazard",
            "osha_codes" to "1926.501,1926.416",
            "worker_id" to "worker_123",
            "project_id" to "project_abc",
            "weather_conditions" to "clear,dry"
        )
        
        val encryptionKey = photoEncryption.generateEncryptionKey()
        
        // Encrypt metadata
        val encryptedMetadata = photoEncryption.encryptMetadata(realMetadata, encryptionKey)
        assertNotEquals(realMetadata.toString(), encryptedMetadata)
        
        // Decrypt metadata
        val decryptedMetadata = photoEncryption.decryptMetadata(encryptedMetadata, encryptionKey)
        assertEquals(realMetadata, decryptedMetadata)
    }
    
    @Test
    fun testStorageIntegrityValidation() = runTest {
        // Test integrity validation
        val isValid = secureStorage.validateIntegrity()
        assertTrue(isValid, "Secure storage integrity validation failed")
    }
    
    @Test
    fun testSecureKeyGeneration() = runTest {
        val key1 = secureStorage.generateSecureKey(32)
        val key2 = secureStorage.generateSecureKey(32)
        val key3 = photoEncryption.generateEncryptionKey()
        
        // All keys should be unique
        assertNotEquals(key1, key2)
        assertNotEquals(key2, key3)
        assertNotEquals(key1, key3)
        
        // All keys should be valid
        assertTrue(key1.isNotBlank())
        assertTrue(key2.isNotBlank())
        assertTrue(photoEncryption.isValidEncryptionKey(key3))
    }
    
    /**
     * Create test photo data that simulates real photo bytes
     */
    private fun createTestPhotoData(): ByteArray {
        // Simulate JPEG header and data
        val jpegHeader = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte() // JPEG SOI + APP0
        )
        val testData = "Test photo content with various data types: 1234567890 !@#$%^&*()_+".toByteArray()
        return jpegHeader + testData
    }
    
    /**
     * Create large test photo data
     */
    private fun createLargeTestPhotoData(size: Int): ByteArray {
        val data = ByteArray(size)
        // Fill with pseudo-random data that simulates real photo content
        for (i in data.indices) {
            data[i] = ((i * 123 + 456) % 256).toByte()
        }
        return data
    }
}