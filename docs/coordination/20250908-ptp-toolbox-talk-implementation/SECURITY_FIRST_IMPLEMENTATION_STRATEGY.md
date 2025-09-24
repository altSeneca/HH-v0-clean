# Security-First Implementation Strategy

## Executive Summary

Based on the research findings that identified critical security gaps in HazardHawk's current implementation, this security-first strategy ensures that certificate pinning, cross-platform secure storage, and document authenticity measures are implemented before any production features are deployed.

**Critical Finding**: Current implementation has unencrypted local storage and missing certificate pinning, creating high-risk vulnerabilities that must be addressed immediately.

## Security Priority Matrix

### Phase 1 Critical Security (MUST COMPLETE FIRST)
**Timeline**: Days 1-5 of implementation
**Blocking**: All API integration and production deployment
**Risk Level**: Critical - Production blocker

### Phase 2 Compliance Security (HIGH PRIORITY) 
**Timeline**: Days 15-25 of implementation
**Blocking**: Digital signatures and legal compliance
**Risk Level**: High - Regulatory compliance blocker

### Phase 3 Advanced Security (NICE TO HAVE)
**Timeline**: Days 25-30 of implementation  
**Blocking**: Advanced monitoring and AI security
**Risk Level**: Medium - Operational excellence

## Phase 1 Critical Security Implementation

### Certificate Pinning Implementation (Days 1-2)

#### Current Vulnerability
```kotlin
// CURRENT STATE - Missing Certificate Pinning
// File: /shared/src/commonMain/kotlin/com/hazardhawk/data/api/HttpClientConfig.kt
val client = HttpClient(CIO) {
    // No certificate pinning configured
    // Vulnerable to MITM attacks
}
```

#### Required Implementation
```kotlin
// SECURE IMPLEMENTATION - Certificate Pinning
class SecureHttpClientFactory {
    companion object {
        private val GOOGLE_API_PINS = setOf(
            "sha256/FEzVOUp4dF3gI0ZVPRJhFbSD608T8kFJqsPlW4xMHRU=", // Google
            "sha256/Ir6OQrZfOe2tOnzddJKgc8KTT8J2kKXf4v1sTDdNmCU="  // Google Backup
        )
        
        private val HAZARDHAWK_API_PINS = setOf(
            "sha256/YourAPIServerCertificateHash=",
            "sha256/YourBackupCertificateHash="
        )
    }
    
    fun createSecureClient(): HttpClient = HttpClient(CIO) {
        engine {
            // Certificate pinning configuration
            https {
                addCertificatePinner(
                    hostPattern = "*.googleapis.com",
                    pins = GOOGLE_API_PINS
                )
                addCertificatePinner(
                    hostPattern = "api.hazardhawk.com", 
                    pins = HAZARDHAWK_API_PINS
                )
            }
        }
        
        // Security headers
        defaultRequest {
            header("X-API-Version", "2025-09-08")
            header("X-Client-Platform", getPlatformIdentifier())
        }
    }
}
```

#### Cross-Platform Certificate Validation
```kotlin
// Android-specific certificate validation
// /shared/src/androidMain/kotlin/com/hazardhawk/security/AndroidCertificateValidator.kt
actual class CertificateValidator {
    actual fun validateCertificate(hostname: String, certificate: X509Certificate): Boolean {
        // Android-specific certificate validation using TrustManagerFactory
        return AndroidNetworkSecurityPolicy.getInstance()
            .isCertificateTransparencyVerificationRequired(hostname)
    }
}

// iOS-specific certificate validation  
// /shared/src/iosMain/kotlin/com/hazardhawk/security/IOSCertificateValidator.kt
actual class CertificateValidator {
    actual fun validateCertificate(hostname: String, certificate: X509Certificate): Boolean {
        // iOS-specific validation using Security framework
        return SecTrustEvaluateWithError(trust, nil)
    }
}
```

### Secure Storage Implementation (Days 3-4)

#### Current Vulnerability Fix
```kotlin
// CURRENT VULNERABLE STATE (Must be fixed immediately)
// File: /shared/src/commonMain/kotlin/com/hazardhawk/camera/MetadataSettings.kt
data class MetadataSettings(
    val encryptLocalStorage: Boolean = false // ❌ CRITICAL VULNERABILITY
)

// SECURE IMPLEMENTATION
data class MetadataSettings(
    val encryptLocalStorage: Boolean = true // ✅ Always encrypted
)
```

#### Cross-Platform Secure Storage Architecture
```kotlin
// Common interface for secure storage
// /shared/src/commonMain/kotlin/com/hazardhawk/security/SecureStorageService.kt
expect class SecureStorageService {
    suspend fun storeApiKey(key: String, value: String): Result<Unit>
    suspend fun retrieveApiKey(key: String): Result<String?>
    suspend fun deleteApiKey(key: String): Result<Unit>
    suspend fun storeDocument(documentId: String, encryptedData: ByteArray): Result<Unit>
    suspend fun retrieveDocument(documentId: String): Result<ByteArray?>
}
```

#### Android Secure Implementation
```kotlin
// /shared/src/androidMain/kotlin/com/hazardhawk/security/AndroidSecureStorage.kt
actual class SecureStorageService(private val context: Context) {
    private val keyAlias = "hazardhawk_document_key"
    
    private fun getOrCreateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setUserAuthenticationRequired(false) // For background document generation
        .setRandomizedEncryptionRequired(true)
        .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    actual suspend fun storeApiKey(key: String, value: String): Result<Unit> {
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val encryptedValue = cipher.doFinal(value.toByteArray())
            val iv = cipher.iv
            
            // Store encrypted value + IV
            val prefs = EncryptedSharedPreferences.create(
                "hazardhawk_secure_prefs",
                keyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            prefs.edit()
                .putString("${key}_value", Base64.encodeToString(encryptedValue, Base64.DEFAULT))
                .putString("${key}_iv", Base64.encodeToString(iv, Base64.DEFAULT))
                .apply()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(SecurityException("Failed to store API key securely", e))
        }
    }
}
```

#### iOS Secure Implementation
```kotlin
// /shared/src/iosMain/kotlin/com/hazardhawk/security/IOSSecureStorage.kt
actual class SecureStorageService {
    actual suspend fun storeApiKey(key: String, value: String): Result<Unit> {
        return try {
            val query = mapOf(
                kSecClass to kSecClassGenericPassword,
                kSecAttrService to "HazardHawk",
                kSecAttrAccount to key,
                kSecValueData to value.encodeToByteArray(),
                kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
            )
            
            val status = SecItemAdd(query as CFDictionary, null)
            if (status == errSecSuccess || status == errSecDuplicateItem) {
                if (status == errSecDuplicateItem) {
                    // Update existing item
                    val updateQuery = mapOf(kSecClass to kSecClassGenericPassword, kSecAttrAccount to key)
                    val updateAttributes = mapOf(kSecValueData to value.encodeToByteArray())
                    SecItemUpdate(updateQuery as CFDictionary, updateAttributes as CFDictionary)
                }
                Result.success(Unit)
            } else {
                Result.failure(SecurityException("Keychain storage failed with status: $status"))
            }
        } catch (e: Exception) {
            Result.failure(SecurityException("Failed to store API key in Keychain", e))
        }
    }
}
```

### Input Validation & Sanitization (Day 5)

#### Prompt Injection Prevention
```kotlin
// CURRENT VULNERABLE CODE (Must be fixed)
// File: /shared/src/commonMain/kotlin/com/hazardhawk/ai/GemmaVisionAnalyzer.kt
private fun buildConstructionSafetyPrompt(basePrompt: String): String {
    return "Focus on: $basePrompt"  // ❌ Direct injection vulnerability
}

// SECURE IMPLEMENTATION
class SecurePromptBuilder {
    companion object {
        private val ALLOWED_WORK_TYPES = setOf(
            "excavation", "electrical", "roofing", "scaffolding", 
            "concrete", "demolition", "painting", "welding"
        )
        
        private val DANGEROUS_PATTERNS = listOf(
            "ignore previous instructions",
            "system:",
            "assistant:",
            "<script>",
            "javascript:",
            "eval(",
            "exec("
        )
    }
    
    fun buildSecurePTPPrompt(
        workDescription: String,
        siteInfo: SiteInformation,
        photos: List<String>
    ): String {
        // Sanitize inputs
        val sanitizedDescription = sanitizeWorkDescription(workDescription)
        val validatedSiteInfo = validateSiteInformation(siteInfo)
        
        // Build secure prompt with validation
        return """
            |Generate a Pre-Task Plan for construction work with the following parameters:
            |Work Type: ${sanitizedDescription}
            |Location: ${validatedSiteInfo.sanitizedLocation}
            |Weather: ${validatedSiteInfo.weather}
            |Crew Size: ${validatedSiteInfo.crewSize}
            |
            |Requirements:
            |1. Include OSHA-compliant safety measures
            |2. Identify potential hazards for this work type
            |3. Specify required PPE
            |4. Include environmental considerations
            |5. Format as structured safety document
            |
            |Do not include any instructions outside of safety planning.
        """.trimMargin()
    }
    
    private fun sanitizeWorkDescription(input: String): String {
        // Remove dangerous patterns
        var sanitized = input
        DANGEROUS_PATTERNS.forEach { pattern ->
            sanitized = sanitized.replace(pattern, "", ignoreCase = true)
        }
        
        // Validate against allowed work types
        val words = sanitized.lowercase().split(Regex("\\W+"))
        val validWorkTypes = words.filter { it in ALLOWED_WORK_TYPES }
        
        return if (validWorkTypes.isNotEmpty()) {
            validWorkTypes.joinToString(" ")
        } else {
            "general construction work" // Safe default
        }
    }
}
```

## Phase 2 Compliance Security Implementation

### Digital Signature Infrastructure (Days 15-20)

```kotlin
// /shared/src/commonMain/kotlin/com/hazardhawk/security/DocumentSignatureService.kt
class DocumentSignatureService(
    private val secureStorage: SecureStorageService,
    private val certificateProvider: CertificateProvider
) {
    suspend fun signDocument(
        document: SafetyReport,
        signerId: String,
        signerRole: String
    ): Result<SignedDocument> {
        return try {
            // Generate document hash
            val documentHash = generateSHA256Hash(document.toJSON())
            
            // Create signature with timestamp
            val signature = createDigitalSignature(
                documentHash = documentHash,
                signerId = signerId,
                timestamp = Clock.System.now(),
                signerRole = signerRole
            )
            
            // Create signed document with chain of custody
            val signedDocument = SignedDocument(
                originalDocument = document,
                signature = signature,
                chainOfCustody = generateChainOfCustody(document, signerId),
                complianceMetadata = generateComplianceMetadata(document)
            )
            
            Result.success(signedDocument)
        } catch (e: Exception) {
            Result.failure(SignatureException("Failed to sign document", e))
        }
    }
    
    suspend fun verifySignature(signedDocument: SignedDocument): Result<SignatureVerification> {
        // Verify cryptographic signature
        // Check certificate validity
        // Validate chain of custody
        // Return verification result
    }
}
```

### Audit Trail Implementation (Days 18-22)

```kotlin
// /shared/src/commonMain/kotlin/com/hazardhawk/security/AuditTrailManager.kt
class AuditTrailManager {
    suspend fun logDocumentCreation(
        documentId: String,
        createdBy: String,
        aiAssisted: Boolean,
        confidence: Float
    ) {
        val auditEntry = AuditEntry(
            id = generateUUID(),
            timestamp = Clock.System.now(),
            action = AuditAction.DOCUMENT_CREATED,
            documentId = documentId,
            userId = createdBy,
            metadata = mapOf(
                "ai_assisted" to aiAssisted.toString(),
                "confidence_score" to confidence.toString(),
                "client_version" to BuildConfig.VERSION_NAME,
                "platform" to getPlatformIdentifier()
            )
        )
        
        securelyStoreAuditEntry(auditEntry)
    }
    
    suspend fun generateComplianceReport(
        startDate: Instant,
        endDate: Instant
    ): Result<ComplianceReport> {
        // Generate audit report for OSHA compliance
        // Include document creation statistics
        // Verify signature integrity
        // Export in regulatory-compliant format
    }
}
```

## Phase 3 Advanced Security Implementation

### AI Security Hardening (Days 25-28)

```kotlin
// /shared/src/commonMain/kotlin/com/hazardhawk/security/AISecurityManager.kt
class AISecurityManager {
    private val anomalyDetector = ResponseAnomalyDetector()
    
    suspend fun validateAIResponse(
        prompt: String,
        response: String,
        expectedType: DocumentType
    ): Result<ValidatedResponse> {
        // Check for prompt injection attempts in response
        if (detectPromptInjectionInResponse(response)) {
            return Result.failure(SecurityException("AI response contains injection attempt"))
        }
        
        // Validate response matches expected document structure
        if (!validateResponseStructure(response, expectedType)) {
            return Result.failure(ValidationException("Response doesn't match expected structure"))
        }
        
        // Check for anomalous content patterns
        val anomalyScore = anomalyDetector.scoreResponse(response)
        if (anomalyScore > ANOMALY_THRESHOLD) {
            logSecurityAlert("High anomaly score in AI response", anomalyScore)
        }
        
        return Result.success(ValidatedResponse(response, anomalyScore))
    }
}
```

## Security Testing Strategy

### Penetration Testing Checklist
- [ ] Certificate pinning bypass attempts
- [ ] API key extraction testing
- [ ] Network traffic interception
- [ ] Local storage encryption verification
- [ ] Input injection testing
- [ ] Cross-platform security consistency
- [ ] Document signature tampering
- [ ] Audit trail integrity verification

### Automated Security Testing
```kotlin
// /shared/src/commonTest/kotlin/com/hazardhawk/security/SecurityTestSuite.kt
class SecurityTestSuite {
    @Test
    fun testCertificatePinningRejectsInvalidCerts() {
        // Test certificate pinning implementation
    }
    
    @Test
    fun testSecureStorageEncryption() {
        // Verify encryption of stored data
    }
    
    @Test
    fun testPromptInjectionPrevention() {
        // Test input sanitization effectiveness
    }
    
    @Test
    fun testDigitalSignatureIntegrity() {
        // Verify signature creation and validation
    }
}
```

## Security Metrics & Monitoring

### Real-time Security Monitoring
```kotlin
// Security event monitoring
class SecurityMonitor {
    fun trackSecurityEvent(event: SecurityEvent) {
        when (event.severity) {
            SecuritySeverity.CRITICAL -> {
                // Immediate alert to security team
                notifySecurityTeam(event)
                // Consider automatic service lockdown
            }
            SecuritySeverity.HIGH -> {
                // Log and alert within 1 hour
                scheduleSecurityAlert(event, Duration.ofHours(1))
            }
            SecuritySeverity.MEDIUM -> {
                // Daily security report
                addToDailySecurityReport(event)
            }
        }
    }
}
```

### Security Success Criteria
- **Certificate Pinning**: 100% validation on all API calls
- **Secure Storage**: 100% encryption of sensitive data
- **Input Validation**: 0% successful injection attempts in testing
- **Digital Signatures**: 100% signature verification success
- **Audit Trail**: 100% event capture with tamper detection
- **Penetration Testing**: 0% critical vulnerabilities found
- **Response Time**: Security alerts within 15 minutes of detection

This security-first implementation strategy ensures that HazardHawk's PTP & Toolbox Talk features are built with production-grade security from day one, addressing all critical vulnerabilities identified in the research phase while enabling safe deployment to construction industry users.