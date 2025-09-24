package com.hazardhawk.security

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*
import kotlin.random.Random

/**
 * iOS-specific security service tests that run on actual iOS devices/simulators.
 * These tests validate iOS Keychain Services and Security framework integration.
 */
class IOSSecurityServiceTest {
    
    private val secureStorage = SecureStorageServiceImpl()
    private val photoEncryption = PhotoEncryptionServiceImpl()
    
    @BeforeTest
    fun setup() = runTest {
        // Clean up any existing test data
        secureStorage.clearAllCredentials()
    }
    
    @AfterTest
    fun cleanup() = runTest {
        // Clean up test data
        secureStorage.clearAllCredentials()
    }
    
    @Test
    fun testKeychainBasicOperations() = runTest {
        val testKey = "ios_test_keychain_key"
        val testValue = "test_keychain_value_123"
        
        // Test keychain availability
        assertTrue(secureStorage.isAvailable(), "iOS Keychain should be available")
        
        // Test store operation
        val storeResult = secureStorage.storeApiKey(testKey, testValue)
        assertTrue(storeResult.isSuccess, "Keychain store should succeed: ${storeResult.exceptionOrNull()}")
        
        // Test retrieve operation
        val retrieveResult = secureStorage.getApiKey(testKey)
        assertTrue(retrieveResult.isSuccess, "Keychain retrieve should succeed: ${retrieveResult.exceptionOrNull()}")
        assertEquals(testValue, retrieveResult.getOrNull(), "Retrieved value should match stored value")
        
        // Test update operation
        val updatedValue = "updated_value_456"
        val updateResult = secureStorage.storeApiKey(testKey, updatedValue)
        assertTrue(updateResult.isSuccess, "Keychain update should succeed")
        
        val retrieveUpdatedResult = secureStorage.getApiKey(testKey)
        assertEquals(updatedValue, retrieveUpdatedResult.getOrNull(), "Updated value should be retrieved")
        
        // Test remove operation
        val removeResult = secureStorage.removeApiKey(testKey)
        assertTrue(removeResult.isSuccess, "Keychain remove should succeed")
        
        // Verify removal
        val verifyResult = secureStorage.getApiKey(testKey)
        assertNull(verifyResult.getOrNull(), "Key should be removed from keychain")
    }
    
    @Test
    fun testKeychainWithSecureEnclave() = runTest {
        // Skip test if Secure Enclave is not available
        if (!IOSSecurityConfig.CapabilityDetector.hasSecureEnclave()) {
            println("Skipping Secure Enclave test - not available on this device")
            return@runTest
        }
        
        val testKey = "secure_enclave_test_key"
        val testValue = "secure_enclave_test_value"
        val metadata = CredentialMetadata(
            createdAt = Clock.System.now(),
            purpose = CredentialPurpose.ENCRYPTION_KEY,
            complianceLevel = ComplianceLevel.Critical
        )
        
        // Store with Secure Enclave protection
        val storeResult = secureStorage.storeApiKey(testKey, testValue, metadata)
        assertTrue(storeResult.isSuccess, "Secure Enclave storage should succeed")
        
        // Retrieve and verify
        val retrieveResult = secureStorage.getApiKey(testKey)
        assertTrue(retrieveResult.isSuccess, "Secure Enclave retrieval should succeed")
        assertEquals(testValue, retrieveResult.getOrNull(), "Secure Enclave value should match")
    }
    
    @Test
    fun testKeychainComplianceLevels() = runTest {
        val baseKey = "compliance_test_key"
        val testValue = "compliance_test_value"
        
        ComplianceLevel.entries.forEach { level ->
            val key = "${baseKey}_${level.name}"
            val metadata = CredentialMetadata(
                createdAt = Clock.System.now(),
                purpose = CredentialPurpose.OTHER,
                complianceLevel = level
            )
            
            val storeResult = secureStorage.storeApiKey(key, testValue, metadata)
            assertTrue(
                storeResult.isSuccess,
                "Should store credential with compliance level ${level.name}: ${storeResult.exceptionOrNull()}"
            )
            
            val metadataResult = secureStorage.getCredentialMetadata(key)
            assertTrue(metadataResult.isSuccess, "Should retrieve metadata for compliance level ${level.name}")
            
            val retrievedMetadata = metadataResult.getOrNull()
            assertEquals(
                level,
                retrievedMetadata?.complianceLevel,
                "Compliance level should match for ${level.name}"
            )
        }
    }
    
    @Test
    fun testAESGCMEncryption() = runTest {
        val testData = generateRandomTestData(4096) // 4KB test data
        val encryptionKey = photoEncryption.generateEncryptionKey()
        
        println("Testing AES-GCM encryption with ${testData.size} bytes")
        
        // Test encryption
        val startEncrypt = Clock.System.now()
        val encryptedData = photoEncryption.encryptPhoto(testData, encryptionKey)
        val encryptTime = Clock.System.now() - startEncrypt
        println("Encryption took: ${encryptTime}")
        
        assertNotNull(encryptedData, "Encrypted data should not be null")
        assertTrue(encryptedData.encryptedData.isNotEmpty(), "Encrypted data should not be empty")
        assertFalse(
            encryptedData.encryptedData.contentEquals(testData),
            "Encrypted data should be different from original"
        )
        
        // Test decryption
        val startDecrypt = Clock.System.now()
        val decryptedData = photoEncryption.decryptPhoto(encryptedData, encryptionKey)
        val decryptTime = Clock.System.now() - startDecrypt
        println("Decryption took: ${decryptTime}")
        
        assertContentEquals(testData, decryptedData, "Decrypted data should match original")
    }
    
    @Test
    fun testLargePhotoEncryption() = runTest {
        // Test with photo-sized data (2MB)
        val largeData = generateRandomTestData(2 * 1024 * 1024)
        val encryptionKey = photoEncryption.generateEncryptionKey()
        
        println("Testing large photo encryption with ${largeData.size} bytes")
        
        val startTime = Clock.System.now()
        
        val encryptedData = photoEncryption.encryptPhoto(largeData, encryptionKey)
        val decryptedData = photoEncryption.decryptPhoto(encryptedData, encryptionKey)
        
        val totalTime = Clock.System.now() - startTime
        println("Large photo encryption/decryption took: ${totalTime}")
        
        assertContentEquals(largeData, decryptedData, "Large photo should encrypt/decrypt correctly")
    }
    
    @Test
    fun testHardwareBackedCrypto() {
        val encryptionInfo = photoEncryption.getEncryptionInfo()
        
        println("Hardware-backed crypto: ${encryptionInfo.isHardwareBacked}")
        println("Algorithm: ${encryptionInfo.algorithm}")
        println("Key length: ${encryptionInfo.keyLength} bits")
        println("Block mode: ${encryptionInfo.blockMode}")
        println("Padding: ${encryptionInfo.padding}")
        
        assertEquals("AES", encryptionInfo.algorithm)
        assertEquals(256, encryptionInfo.keyLength)
        assertEquals("GCM", encryptionInfo.blockMode)
        assertEquals("NoPadding", encryptionInfo.padding)
    }
    
    @Test
    fun testBiometricCapabilities() {
        val hasSecureEnclave = IOSSecurityConfig.CapabilityDetector.hasSecureEnclave()
        val hasTouchID = IOSSecurityConfig.CapabilityDetector.hasTouchID()
        val hasFaceID = IOSSecurityConfig.CapabilityDetector.hasFaceID()
        val hasPasscode = IOSSecurityConfig.CapabilityDetector.hasPasscode()
        val bestMethod = IOSSecurityConfig.CapabilityDetector.getBestBiometricMethod()
        
        println("Device Security Capabilities:")
        println("- Secure Enclave: $hasSecureEnclave")
        println("- Touch ID: $hasTouchID")
        println("- Face ID: $hasFaceID")
        println("- Passcode: $hasPasscode")
        println("- Best biometric method: $bestMethod")
        
        // At least passcode should be available on test devices
        assertTrue(hasPasscode, "Test device should have passcode enabled")
    }
    
    @Test
    fun testSecurityValidation() {
        ComplianceLevel.entries.forEach { level ->
            val validation = IOSSecurityConfig.SecurityValidator.validateDeviceSecurity(level)
            
            println("Security validation for ${level.name}:")
            println("- Valid: ${validation.isValid}")
            if (validation.issues.isNotEmpty()) {
                println("- Issues: ${validation.issues}")
                println("- Recommendations: ${validation.recommendedActions}")
            }
        }
    }
    
    @Test
    fun testSecureKeyGeneration() = runTest {
        val keys = mutableSetOf<String>()
        
        // Generate multiple keys to test uniqueness
        repeat(100) {
            val key = photoEncryption.generateEncryptionKey()
            assertTrue(photoEncryption.isValidEncryptionKey(key), "Generated key should be valid")
            assertFalse(keys.contains(key), "Generated keys should be unique")
            keys.add(key)
        }
        
        assertEquals(100, keys.size, "All generated keys should be unique")
    }
    
    @Test
    fun testSecureWipeEffectiveness() {
        val sensitiveData = "This is very sensitive data that must be wiped securely".repeat(100).encodeToByteArray()
        val originalChecksum = sensitiveData.contentHashCode()
        
        // Verify data is initially intact
        assertEquals(originalChecksum, sensitiveData.contentHashCode())
        
        // Perform secure wipe
        photoEncryption.secureWipe(sensitiveData)
        
        // Verify data has been overwritten
        assertNotEquals(originalChecksum, sensitiveData.contentHashCode())
        assertTrue(sensitiveData.all { it == 0.toByte() }, "All bytes should be zero after wipe")
    }
    
    @Test
    fun testMetadataEncryptionWithComplexData() = runTest {
        val complexMetadata = mapOf(
            "gps_coordinates" to "40.7589,-73.9851",
            "timestamp" to "2025-01-15T10:30:45.123Z",
            "device_info" to "iPhone 15 Pro, iOS 17.2",
            "hazard_classification" to "OSHA_1926.95_fall_protection",
            "confidence_score" to "0.9876",
            "inspector_id" to "inspector_john_doe_12345",
            "project_code" to "PROJ_2025_NYC_CONSTRUCTION_001",
            "weather_conditions" to "Clear, 72°F, 15mph NW wind",
            "safety_equipment" to "Hard hat, safety harness, steel-toed boots",
            "notes" to "Worker observed without fall protection at height >6ft. Immediate correction required per OSHA 1926.501(b)(1)."
        )
        
        val encryptionKey = photoEncryption.generateEncryptionKey()
        
        val encryptedMetadata = photoEncryption.encryptMetadata(complexMetadata, encryptionKey)
        val decryptedMetadata = photoEncryption.decryptMetadata(encryptedMetadata, encryptionKey)
        
        assertEquals(complexMetadata, decryptedMetadata, "Complex metadata should encrypt/decrypt correctly")
    }
    
    @Test
    fun testPerformanceBenchmark() = runTest {
        val testSizes = listOf(1024, 10240, 102400, 1048576) // 1KB, 10KB, 100KB, 1MB
        val encryptionKey = photoEncryption.generateEncryptionKey()
        
        println("Performance Benchmark Results:")
        println("Size\t\tEncryption (ms)\tDecryption (ms)\tTotal (ms)")
        
        testSizes.forEach { size ->
            val testData = generateRandomTestData(size)
            
            val startEncrypt = Clock.System.now()
            val encryptedData = photoEncryption.encryptPhoto(testData, encryptionKey)
            val encryptTime = Clock.System.now() - startEncrypt
            
            val startDecrypt = Clock.System.now()
            val decryptedData = photoEncryption.decryptPhoto(encryptedData, encryptionKey)
            val decryptTime = Clock.System.now() - startDecrypt
            
            val totalTime = encryptTime + decryptTime
            
            println("${formatSize(size)}\t\t${encryptTime}\t${decryptTime}\t${totalTime}")
            
            // Verify data integrity
            assertContentEquals(testData, decryptedData, "Data should remain intact for size $size")
        }
    }
    
    // Helper functions
    
    private fun generateRandomTestData(size: Int): ByteArray {
        return ByteArray(size) { Random.nextInt(0, 256).toByte() }
    }
    
    private fun formatSize(bytes: Int): String {
        return when {
            bytes >= 1048576 -> "${bytes / 1048576}MB"
            bytes >= 1024 -> "${bytes / 1024}KB"
            else -> "${bytes}B"
        }
    }
}