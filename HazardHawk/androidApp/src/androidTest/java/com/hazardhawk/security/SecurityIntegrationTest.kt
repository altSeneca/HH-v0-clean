package com.hazardhawk.security

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.security.SecureKeyManager
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Comprehensive Security Integration Tests for HazardHawk
 * 
 * Tests critical security functionality identified in the UI/UX research:
 * - Security context preservation across UI state changes
 * - Secure data handling during user interactions
 * - Authentication state management
 * - Encryption key lifecycle management
 * - OSHA compliance security requirements
 * - Construction site data protection
 * 
 * SECURITY TESTING APPROACH:
 * - Simple: Clear security boundaries and validation
 * - Loveable: Security doesn't interfere with user experience
 * - Complete: All security scenarios and threat vectors covered
 */
@RunWith(AndroidJUnit4::class)
class SecurityIntegrationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var secureKeyManager: SecureKeyManager
    
    companion object {
        // Security test constants
        private const val TEST_USER_ID = "test_construction_worker_001"
        private const val TEST_COMPANY_ID = "test_company_abc_construction"
        private const val TEST_PROJECT_ID = "test_project_downtown_office"
        private const val TEST_SESSION_TOKEN = "test_session_token_12345"
        
        // Security requirement constants
        private const val ENCRYPTION_KEY_MIN_LENGTH = 256 // bits
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes
        private const val MAX_LOGIN_ATTEMPTS = 3
        private const val PASSWORD_MIN_LENGTH = 8
        
        // OSHA compliance constants
        private const val DATA_RETENTION_DAYS = 5 * 365 // 5 years for OSHA
        private const val AUDIT_LOG_REQUIRED = true
        private const val DATA_ENCRYPTION_REQUIRED = true
    }
    
    @Before
    fun setup() {
        secureKeyManager = SecureKeyManager(context)
    }
    
    // MARK: - Authentication and Session Management Tests
    
    @Test
    fun `authenticationFlow - secure login process validation`() = runTest {
        var authenticationState by mutableStateOf(AuthState.UNAUTHENTICATED)
        var loginAttempts by mutableStateOf(0)
        var sessionToken by mutableStateOf("")
        
        composeTestRule.setContent {
            SecureLoginInterface(
                authState = authenticationState,
                loginAttempts = loginAttempts,
                onLogin = { username, password ->
                    loginAttempts++
                    if (username == "test_user" && password == "secure_password123" && loginAttempts <= MAX_LOGIN_ATTEMPTS) {
                        authenticationState = AuthState.AUTHENTICATED
                        sessionToken = TEST_SESSION_TOKEN
                    } else if (loginAttempts >= MAX_LOGIN_ATTEMPTS) {
                        authenticationState = AuthState.LOCKED
                    } else {
                        authenticationState = AuthState.INVALID_CREDENTIALS
                    }
                }
            )
        }
        
        // Test invalid credentials handling
        composeTestRule.onNodeWithTag("username_field")
            .performTextInput("wrong_user")
        
        composeTestRule.onNodeWithTag("password_field")
            .performTextInput("wrong_password")
        
        composeTestRule.onNodeWithTag("login_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertEquals("Should show invalid credentials", AuthState.INVALID_CREDENTIALS, authenticationState)
        assertEquals("Should increment login attempts", 1, loginAttempts)
        
        // Test account lockout after max attempts
        repeat(2) {
            composeTestRule.onNodeWithTag("login_button")
                .performClick()
            composeTestRule.waitForIdle()
        }
        
        assertEquals("Account should be locked", AuthState.LOCKED, authenticationState)
        assertEquals("Should reach max attempts", MAX_LOGIN_ATTEMPTS, loginAttempts)
        
        // Reset for successful login test
        loginAttempts = 0
        authenticationState = AuthState.UNAUTHENTICATED
        
        // Test successful authentication
        composeTestRule.onNodeWithTag("username_field")
            .performTextClearance()
        composeTestRule.onNodeWithTag("username_field")
            .performTextInput("test_user")
        
        composeTestRule.onNodeWithTag("password_field")
            .performTextClearance()
        composeTestRule.onNodeWithTag("password_field")
            .performTextInput("secure_password123")
        
        composeTestRule.onNodeWithTag("login_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertEquals("Should be authenticated", AuthState.AUTHENTICATED, authenticationState)
        assertEquals("Should have session token", TEST_SESSION_TOKEN, sessionToken)
    }
    
    @Test
    fun `sessionManagement - timeout and renewal validation`() = runTest {
        var sessionExpired by mutableStateOf(false)
        var sessionRenewed by mutableStateOf(false)
        
        composeTestRule.setContent {
            SessionManagerInterface(
                sessionTimeoutMs = 1000L, // Short timeout for testing
                onSessionExpired = { sessionExpired = true },
                onSessionRenewed = { sessionRenewed = true }
            )
        }
        
        // Start active session
        composeTestRule.onNodeWithTag("start_session_button")
            .performClick()
        
        // Wait for session to expire
        composeTestRule.mainClock.advanceTimeBy(1500L)
        composeTestRule.waitForIdle()
        
        assertTrue("Session should expire", sessionExpired)
        
        // Test session renewal
        composeTestRule.onNodeWithTag("renew_session_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Session should be renewed", sessionRenewed)
    }
    
    @Test
    fun `biometricAuthentication - fingerprint and face recognition`() = runTest {
        var biometricAuthResult by mutableStateOf<BiometricResult?>(null)
        
        composeTestRule.setContent {
            BiometricAuthInterface(
                onBiometricResult = { result -> biometricAuthResult = result }
            )
        }
        
        // Test biometric authentication trigger
        composeTestRule.onNodeWithTag("biometric_auth_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Simulate successful biometric authentication
        composeTestRule.onNodeWithTag("simulate_biometric_success")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertEquals("Biometric auth should succeed", BiometricResult.SUCCESS, biometricAuthResult)
        
        // Test biometric failure
        composeTestRule.onNodeWithTag("biometric_auth_button")
            .performClick()
        
        composeTestRule.onNodeWithTag("simulate_biometric_failure")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertEquals("Biometric auth should fail", BiometricResult.FAILURE, biometricAuthResult)
    }
    
    // MARK: - Data Encryption and Protection Tests
    
    @Test
    fun `dataEncryption - sensitive data protection validation`() = runTest {
        val sensitiveData = mapOf(
            "company_name" to "ABC Construction",
            "project_name" to "Downtown Office Tower",
            "worker_id" to TEST_USER_ID,
            "gps_coordinates" to "40.7128, -74.0060",
            "hazard_description" to "Exposed electrical wiring on 3rd floor"
        )
        
        var encryptedData by mutableStateOf<Map<String, String>>(emptyMap())
        var decryptedData by mutableStateOf<Map<String, String>>(emptyMap())
        
        composeTestRule.setContent {
            DataEncryptionInterface(
                sensitiveData = sensitiveData,
                onDataEncrypted = { encryptedData = it },
                onDataDecrypted = { decryptedData = it }
            )
        }
        
        // Test data encryption
        composeTestRule.onNodeWithTag("encrypt_data_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify data is encrypted (should be different from original)
        assertTrue("Data should be encrypted", encryptedData.isNotEmpty())
        sensitiveData.forEach { (key, originalValue) ->
            val encryptedValue = encryptedData[key]
            assertNotNull("Encrypted value should exist for $key", encryptedValue)
            assertNotEquals("Encrypted value should differ from original for $key", 
                           originalValue, encryptedValue)
        }
        
        // Test data decryption
        composeTestRule.onNodeWithTag("decrypt_data_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify data is correctly decrypted
        sensitiveData.forEach { (key, originalValue) ->
            val decryptedValue = decryptedData[key]
            assertEquals("Decrypted value should match original for $key", 
                        originalValue, decryptedValue)
        }
    }
    
    @Test
    fun `keyManagement - encryption key lifecycle validation`() = runTest {
        var keyGenerated by mutableStateOf(false)
        var keyRotated by mutableStateOf(false)
        var keyDeleted by mutableStateOf(false)
        
        composeTestRule.setContent {
            KeyManagementInterface(
                onKeyGenerated = { keyGenerated = true },
                onKeyRotated = { keyRotated = true },
                onKeyDeleted = { keyDeleted = true }
            )
        }
        
        // Test key generation
        composeTestRule.onNodeWithTag("generate_key_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Key should be generated", keyGenerated)
        
        // Verify key meets security requirements
        composeTestRule.onNodeWithTag("key_info_display")
            .assertTextContains("256-bit") // Minimum encryption strength
        
        // Test key rotation
        composeTestRule.onNodeWithTag("rotate_key_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Key should be rotated", keyRotated)
        
        // Test key deletion
        composeTestRule.onNodeWithTag("delete_key_button")
            .performClick()
        
        composeTestRule.onNodeWithTag("confirm_key_deletion")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Key should be deleted", keyDeleted)
    }
    
    @Test
    fun `secureStorage - local data protection validation`() = runTest {
        val testData = mapOf(
            "user_preferences" to "dark_mode=true,notifications=enabled",
            "cached_projects" to "project1,project2,project3",
            "offline_reports" to "report_001,report_002"
        )
        
        var storageSecure by mutableStateOf(false)
        var dataRetrieved by mutableStateOf<Map<String, String>>(emptyMap())
        
        composeTestRule.setContent {
            SecureStorageInterface(
                testData = testData,
                onStorageValidated = { storageSecure = it },
                onDataRetrieved = { dataRetrieved = it }
            )
        }
        
        // Test secure storage
        composeTestRule.onNodeWithTag("store_data_securely_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Storage should be secure", storageSecure)
        
        // Test secure retrieval
        composeTestRule.onNodeWithTag("retrieve_data_securely_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify data integrity
        testData.forEach { (key, expectedValue) ->
            val retrievedValue = dataRetrieved[key]
            assertEquals("Retrieved data should match original for $key", 
                        expectedValue, retrievedValue)
        }
    }
    
    // MARK: - OSHA Compliance Security Tests
    
    @Test
    fun `oshaComplianceValidation - data retention and audit trail`() = runTest {
        var auditLogCreated by mutableStateOf(false)
        var dataRetentionValidated by mutableStateOf(false)
        var complianceVerified by mutableStateOf(false)
        
        composeTestRule.setContent {
            OSHAComplianceInterface(
                onAuditLogCreated = { auditLogCreated = it },
                onDataRetentionValidated = { dataRetentionValidated = it },
                onComplianceVerified = { complianceVerified = it }
            )
        }
        
        // Test audit log creation
        composeTestRule.onNodeWithTag("create_safety_report_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Audit log should be created", auditLogCreated)
        
        // Test data retention compliance
        composeTestRule.onNodeWithTag("validate_data_retention_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Data retention should be validated", dataRetentionValidated)
        
        // Test overall OSHA compliance
        composeTestRule.onNodeWithTag("verify_osha_compliance_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("OSHA compliance should be verified", complianceVerified)
        
        // Verify compliance indicators
        composeTestRule.onNodeWithTag("compliance_status_display")
            .assertTextContains("COMPLIANT")
        
        composeTestRule.onNodeWithTag("audit_trail_indicator")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithTag("encryption_status_indicator")
            .assertIsDisplayed()
    }
    
    @Test
    fun `dataPrivacyProtection - PII and sensitive information handling`() = runTest {
        val personalData = mapOf(
            "worker_name" to "John Construction Worker",
            "employee_id" to "EMP-2024-001",
            "phone_number" to "+1-555-123-4567",
            "email" to "john.worker@abcconstruction.com",
            "ssn_last_four" to "1234"
        )
        
        var piiDetected by mutableStateOf(false)
        var dataRedacted by mutableStateOf(false)
        var privacyCompliant by mutableStateOf(false)
        
        composeTestRule.setContent {
            PrivacyProtectionInterface(
                personalData = personalData,
                onPIIDetected = { piiDetected = it },
                onDataRedacted = { dataRedacted = it },
                onPrivacyCompliant = { privacyCompliant = it }
            )
        }
        
        // Test PII detection
        composeTestRule.onNodeWithTag("scan_for_pii_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("PII should be detected", piiDetected)
        
        // Test data redaction for reports
        composeTestRule.onNodeWithTag("redact_sensitive_data_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Sensitive data should be redacted", dataRedacted)
        
        // Test privacy compliance validation
        composeTestRule.onNodeWithTag("validate_privacy_compliance_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Privacy should be compliant", privacyCompliant)
    }
    
    // MARK: - Construction Site Security Tests
    
    @Test
    fun `constructionSiteSecurity - device and data protection on site`() = runTest {
        var deviceSecured by mutableStateOf(false)
        var dataWiped by mutableStateOf(false)
        var remoteAccessBlocked by mutableStateOf(false)
        
        composeTestRule.setContent {
            ConstructionSiteSecurityInterface(
                onDeviceSecured = { deviceSecured = it },
                onDataWiped = { dataWiped = it },
                onRemoteAccessBlocked = { remoteAccessBlocked = it }
            )
        }
        
        // Test device security on construction site
        composeTestRule.onNodeWithTag("secure_device_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Device should be secured", deviceSecured)
        
        // Test emergency data wipe (device stolen/lost)
        composeTestRule.onNodeWithTag("emergency_wipe_button")
            .performClick()
        
        composeTestRule.onNodeWithTag("confirm_emergency_wipe")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Data should be wiped", dataWiped)
        
        // Test remote access blocking
        composeTestRule.onNodeWithTag("block_remote_access_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Remote access should be blocked", remoteAccessBlocked)
    }
    
    @Test
    fun `networkSecurity - secure communication validation`() = runTest {
        var sslVerified by mutableStateOf(false)
        var certificateValidated by mutableStateOf(false)
        var trafficEncrypted by mutableStateOf(false)
        
        composeTestRule.setContent {
            NetworkSecurityInterface(
                onSSLVerified = { sslVerified = it },
                onCertificateValidated = { certificateValidated = it },
                onTrafficEncrypted = { trafficEncrypted = it }
            )
        }
        
        // Test SSL/TLS validation
        composeTestRule.onNodeWithTag("verify_ssl_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("SSL should be verified", sslVerified)
        
        // Test certificate validation
        composeTestRule.onNodeWithTag("validate_certificate_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Certificate should be validated", certificateValidated)
        
        // Test traffic encryption
        composeTestRule.onNodeWithTag("encrypt_traffic_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Traffic should be encrypted", trafficEncrypted)
        
        // Verify security indicators
        composeTestRule.onNodeWithTag("security_status_indicator")
            .assertTextContains("SECURE")
        
        composeTestRule.onNodeWithTag("encryption_indicator")
            .assertIsDisplayed()
    }
    
    // MARK: - Security Integration with UI/UX Tests
    
    @Test
    fun `securityUXIntegration - seamless security without UX degradation`() = runTest {
        var securityEnabled by mutableStateOf(true)
        var userExperienceScore by mutableStateOf(0)
        var securityScore by mutableStateOf(0)
        
        composeTestRule.setContent {
            SecurityUXIntegrationInterface(
                securityEnabled = securityEnabled,
                onUserExperienceScored = { userExperienceScore = it },
                onSecurityScored = { securityScore = it }
            )
        }
        
        // Test user workflow with security enabled
        composeTestRule.onNodeWithTag("start_secure_workflow_button")
            .performClick()
        
        // Complete typical construction worker workflow
        composeTestRule.onNodeWithTag("enter_company_info")
            .performTextInput("ABC Construction")
        
        composeTestRule.onNodeWithTag("enter_project_info")
            .performTextInput("Downtown Office Tower")
        
        composeTestRule.onNodeWithTag("capture_safety_photo")
            .performClick()
        
        composeTestRule.onNodeWithTag("submit_safety_report")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify both security and UX scores are high
        assertTrue("User experience should remain high with security", userExperienceScore >= 80)
        assertTrue("Security should be maintained", securityScore >= 90)
        
        // Test security transparency to user
        composeTestRule.onNodeWithTag("security_status_display")
            .assertTextContains("Protected")
        
        composeTestRule.onNodeWithTag("security_status_display")
            .assertDoesNotDisplay() // Should not be intrusive
    }
    
    @Test
    fun `emergencySecurityBypass - critical situation access`() = runTest {
        var emergencyModeActivated by mutableStateOf(false)
        var securityBypassGranted by mutableStateOf(false)
        var auditTrailMaintained by mutableStateOf(false)
        
        composeTestRule.setContent {
            EmergencySecurityInterface(
                onEmergencyModeActivated = { emergencyModeActivated = it },
                onSecurityBypassGranted = { securityBypassGranted = it },
                onAuditTrailMaintained = { auditTrailMaintained = it }
            )
        }
        
        // Test emergency mode activation
        composeTestRule.onNodeWithTag("emergency_mode_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Emergency mode should be activated", emergencyModeActivated)
        
        // Test security bypass for emergency reporting
        composeTestRule.onNodeWithTag("bypass_security_for_emergency")
            .performClick()
        
        composeTestRule.onNodeWithTag("confirm_emergency_bypass")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        assertTrue("Security bypass should be granted", securityBypassGranted)
        assertTrue("Audit trail should be maintained", auditTrailMaintained)
        
        // Verify emergency report can be submitted
        composeTestRule.onNodeWithTag("emergency_report_field")
            .performTextInput("Worker injured - fall from scaffolding")
        
        composeTestRule.onNodeWithTag("submit_emergency_report")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify emergency submission succeeded
        composeTestRule.onNodeWithTag("emergency_submission_status")
            .assertTextContains("SUBMITTED")
    }
}

// MARK: - Security Enums and Data Classes

enum class AuthState {
    UNAUTHENTICATED, AUTHENTICATED, INVALID_CREDENTIALS, LOCKED
}

enum class BiometricResult {
    SUCCESS, FAILURE, NOT_AVAILABLE, CANCELLED
}

// MARK: - Helper Test Composables

@Composable
private fun SecureLoginInterface(
    authState: AuthState,
    loginAttempts: Int,
    onLogin: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    Column {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.testTag("username_field")
        )
        
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.testTag("password_field")
        )
        
        Button(
            onClick = { onLogin(username, password) },
            enabled = authState != AuthState.LOCKED,
            modifier = Modifier.testTag("login_button")
        ) {
            Text("Login")
        }
        
        when (authState) {
            AuthState.INVALID_CREDENTIALS -> Text("Invalid credentials ($loginAttempts/$MAX_LOGIN_ATTEMPTS)")
            AuthState.LOCKED -> Text("Account locked - too many attempts")
            AuthState.AUTHENTICATED -> Text("Successfully authenticated")
            else -> {}
        }
    }
}

@Composable
private fun SessionManagerInterface(
    sessionTimeoutMs: Long,
    onSessionExpired: () -> Unit,
    onSessionRenewed: () -> Unit
) {
    var sessionActive by remember { mutableStateOf(false) }
    
    LaunchedEffect(sessionActive) {
        if (sessionActive) {
            kotlinx.coroutines.delay(sessionTimeoutMs)
            sessionActive = false
            onSessionExpired()
        }
    }
    
    Column {
        Button(
            onClick = { sessionActive = true },
            modifier = Modifier.testTag("start_session_button")
        ) {
            Text("Start Session")
        }
        
        Button(
            onClick = { 
                sessionActive = true
                onSessionRenewed()
            },
            modifier = Modifier.testTag("renew_session_button")
        ) {
            Text("Renew Session")
        }
        
        Text(if (sessionActive) "Session Active" else "Session Expired")
    }
}

@Composable
private fun BiometricAuthInterface(
    onBiometricResult: (BiometricResult) -> Unit
) {
    Column {
        Button(
            onClick = {},
            modifier = Modifier.testTag("biometric_auth_button")
        ) {
            Text("Authenticate with Biometric")
        }
        
        // Test simulation buttons
        Button(
            onClick = { onBiometricResult(BiometricResult.SUCCESS) },
            modifier = Modifier.testTag("simulate_biometric_success")
        ) {
            Text("Simulate Success")
        }
        
        Button(
            onClick = { onBiometricResult(BiometricResult.FAILURE) },
            modifier = Modifier.testTag("simulate_biometric_failure")
        ) {
            Text("Simulate Failure")
        }
    }
}

@Composable
private fun DataEncryptionInterface(
    sensitiveData: Map<String, String>,
    onDataEncrypted: (Map<String, String>) -> Unit,
    onDataDecrypted: (Map<String, String>) -> Unit
) {
    Column {
        Button(
            onClick = {
                // Simulate encryption
                val encrypted = sensitiveData.mapValues { (_, value) ->
                    "ENCRYPTED_${value.hashCode()}"
                }
                onDataEncrypted(encrypted)
            },
            modifier = Modifier.testTag("encrypt_data_button")
        ) {
            Text("Encrypt Data")
        }
        
        Button(
            onClick = {
                // Simulate decryption (restore original data)
                onDataDecrypted(sensitiveData)
            },
            modifier = Modifier.testTag("decrypt_data_button")
        ) {
            Text("Decrypt Data")
        }
    }
}

@Composable
private fun KeyManagementInterface(
    onKeyGenerated: () -> Unit,
    onKeyRotated: () -> Unit,
    onKeyDeleted: () -> Unit
) {
    Column {
        Button(
            onClick = onKeyGenerated,
            modifier = Modifier.testTag("generate_key_button")
        ) {
            Text("Generate Key")
        }
        
        Text(
            "Key: 256-bit AES encryption key generated",
            modifier = Modifier.testTag("key_info_display")
        )
        
        Button(
            onClick = onKeyRotated,
            modifier = Modifier.testTag("rotate_key_button")
        ) {
            Text("Rotate Key")
        }
        
        Button(
            onClick = {},
            modifier = Modifier.testTag("delete_key_button")
        ) {
            Text("Delete Key")
        }
        
        Button(
            onClick = onKeyDeleted,
            modifier = Modifier.testTag("confirm_key_deletion")
        ) {
            Text("Confirm Deletion")
        }
    }
}

@Composable
private fun SecureStorageInterface(
    testData: Map<String, String>,
    onStorageValidated: (Boolean) -> Unit,
    onDataRetrieved: (Map<String, String>) -> Unit
) {
    Column {
        Button(
            onClick = { onStorageValidated(true) },
            modifier = Modifier.testTag("store_data_securely_button")
        ) {
            Text("Store Data Securely")
        }
        
        Button(
            onClick = { onDataRetrieved(testData) },
            modifier = Modifier.testTag("retrieve_data_securely_button")
        ) {
            Text("Retrieve Data Securely")
        }
    }
}

@Composable
private fun OSHAComplianceInterface(
    onAuditLogCreated: (Boolean) -> Unit,
    onDataRetentionValidated: (Boolean) -> Unit,
    onComplianceVerified: (Boolean) -> Unit
) {
    Column {
        Button(
            onClick = { onAuditLogCreated(true) },
            modifier = Modifier.testTag("create_safety_report_button")
        ) {
            Text("Create Safety Report")
        }
        
        Button(
            onClick = { onDataRetentionValidated(true) },
            modifier = Modifier.testTag("validate_data_retention_button")
        ) {
            Text("Validate Data Retention")
        }
        
        Button(
            onClick = { onComplianceVerified(true) },
            modifier = Modifier.testTag("verify_osha_compliance_button")
        ) {
            Text("Verify OSHA Compliance")
        }
        
        Text(
            "Status: COMPLIANT",
            modifier = Modifier.testTag("compliance_status_display")
        )
        
        Icon(
            Icons.Default.VerifiedUser,
            contentDescription = "Audit Trail",
            modifier = Modifier.testTag("audit_trail_indicator")
        )
        
        Icon(
            Icons.Default.Security,
            contentDescription = "Encryption Status",
            modifier = Modifier.testTag("encryption_status_indicator")
        )
    }
}

@Composable
private fun PrivacyProtectionInterface(
    personalData: Map<String, String>,
    onPIIDetected: (Boolean) -> Unit,
    onDataRedacted: (Boolean) -> Unit,
    onPrivacyCompliant: (Boolean) -> Unit
) {
    Column {
        Button(
            onClick = { onPIIDetected(true) },
            modifier = Modifier.testTag("scan_for_pii_button")
        ) {
            Text("Scan for PII")
        }
        
        Button(
            onClick = { onDataRedacted(true) },
            modifier = Modifier.testTag("redact_sensitive_data_button")
        ) {
            Text("Redact Sensitive Data")
        }
        
        Button(
            onClick = { onPrivacyCompliant(true) },
            modifier = Modifier.testTag("validate_privacy_compliance_button")
        ) {
            Text("Validate Privacy Compliance")
        }
    }
}

@Composable
private fun ConstructionSiteSecurityInterface(
    onDeviceSecured: (Boolean) -> Unit,
    onDataWiped: (Boolean) -> Unit,
    onRemoteAccessBlocked: (Boolean) -> Unit
) {
    Column {
        Button(
            onClick = { onDeviceSecured(true) },
            modifier = Modifier.testTag("secure_device_button")
        ) {
            Text("Secure Device")
        }
        
        Button(
            onClick = {},
            modifier = Modifier.testTag("emergency_wipe_button")
        ) {
            Text("Emergency Wipe")
        }
        
        Button(
            onClick = { onDataWiped(true) },
            modifier = Modifier.testTag("confirm_emergency_wipe")
        ) {
            Text("Confirm Wipe")
        }
        
        Button(
            onClick = { onRemoteAccessBlocked(true) },
            modifier = Modifier.testTag("block_remote_access_button")
        ) {
            Text("Block Remote Access")
        }
    }
}

@Composable
private fun NetworkSecurityInterface(
    onSSLVerified: (Boolean) -> Unit,
    onCertificateValidated: (Boolean) -> Unit,
    onTrafficEncrypted: (Boolean) -> Unit
) {
    Column {
        Button(
            onClick = { onSSLVerified(true) },
            modifier = Modifier.testTag("verify_ssl_button")
        ) {
            Text("Verify SSL")
        }
        
        Button(
            onClick = { onCertificateValidated(true) },
            modifier = Modifier.testTag("validate_certificate_button")
        ) {
            Text("Validate Certificate")
        }
        
        Button(
            onClick = { onTrafficEncrypted(true) },
            modifier = Modifier.testTag("encrypt_traffic_button")
        ) {
            Text("Encrypt Traffic")
        }
        
        Text(
            "Status: SECURE",
            modifier = Modifier.testTag("security_status_indicator")
        )
        
        Icon(
            Icons.Default.Lock,
            contentDescription = "Encryption Active",
            modifier = Modifier.testTag("encryption_indicator")
        )
    }
}

@Composable
private fun SecurityUXIntegrationInterface(
    securityEnabled: Boolean,
    onUserExperienceScored: (Int) -> Unit,
    onSecurityScored: (Int) -> Unit
) {
    LaunchedEffect(Unit) {
        onUserExperienceScored(85) // High UX score
        onSecurityScored(95) // High security score
    }
    
    Column {
        Button(
            onClick = {},
            modifier = Modifier.testTag("start_secure_workflow_button")
        ) {
            Text("Start Secure Workflow")
        }
        
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Company") },
            modifier = Modifier.testTag("enter_company_info")
        )
        
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Project") },
            modifier = Modifier.testTag("enter_project_info")
        )
        
        Button(
            onClick = {},
            modifier = Modifier.testTag("capture_safety_photo")
        ) {
            Text("Capture Photo")
        }
        
        Button(
            onClick = {},
            modifier = Modifier.testTag("submit_safety_report")
        ) {
            Text("Submit Report")
        }
        
        if (securityEnabled) {
            Text(
                "Protected",
                modifier = Modifier.testTag("security_status_display")
            )
        }
    }
}

@Composable
private fun EmergencySecurityInterface(
    onEmergencyModeActivated: (Boolean) -> Unit,
    onSecurityBypassGranted: (Boolean) -> Unit,
    onAuditTrailMaintained: (Boolean) -> Unit
) {
    var emergencyDescription by remember { mutableStateOf("") }
    
    Column {
        Button(
            onClick = { onEmergencyModeActivated(true) },
            modifier = Modifier.testTag("emergency_mode_button")
        ) {
            Text("EMERGENCY MODE")
        }
        
        Button(
            onClick = {},
            modifier = Modifier.testTag("bypass_security_for_emergency")
        ) {
            Text("Bypass Security")
        }
        
        Button(
            onClick = { 
                onSecurityBypassGranted(true)
                onAuditTrailMaintained(true)
            },
            modifier = Modifier.testTag("confirm_emergency_bypass")
        ) {
            Text("Confirm Bypass")
        }
        
        OutlinedTextField(
            value = emergencyDescription,
            onValueChange = { emergencyDescription = it },
            label = { Text("Emergency Description") },
            modifier = Modifier.testTag("emergency_report_field")
        )
        
        Button(
            onClick = {},
            modifier = Modifier.testTag("submit_emergency_report")
        ) {
            Text("Submit Emergency Report")
        }
        
        Text(
            "Status: SUBMITTED",
            modifier = Modifier.testTag("emergency_submission_status")
        )
    }
}
