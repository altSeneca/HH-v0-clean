@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hazardhawk.security

import kotlinx.cinterop.*
import platform.Security.*
import platform.LocalAuthentication.*
import platform.Foundation.NSError

/**
 * iOS-specific security configuration constants and utilities
 * Provides platform-specific security settings and feature detection
 */
object IOSSecurityConfig {
    
    // Keychain accessibility levels based on compliance requirements
    val ACCESSIBILITY_LEVELS = mapOf(
        ComplianceLevel.Standard to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
        ComplianceLevel.Enhanced to kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
        ComplianceLevel.Critical to kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly,
        ComplianceLevel.OSHA_Compliant to kSecAttrAccessibleWhenPasscodeSetThisDeviceOnly
    )
    
    // Biometric authentication policies
    val BIOMETRIC_POLICIES = mapOf(
        ComplianceLevel.Standard to LAPolicyDeviceOwnerAuthentication,
        ComplianceLevel.Enhanced to LAPolicyDeviceOwnerAuthenticationWithBiometrics,
        ComplianceLevel.Critical to LAPolicyDeviceOwnerAuthenticationWithBiometrics,
        ComplianceLevel.OSHA_Compliant to LAPolicyDeviceOwnerAuthenticationWithBiometrics
    )
    
    // Access control flags for different compliance levels
    val ACCESS_CONTROL_FLAGS = mapOf(
        ComplianceLevel.Standard to 0u, // No special flags
        ComplianceLevel.Enhanced to kSecAccessControlTouchIDAny,
        ComplianceLevel.Critical to (kSecAccessControlTouchIDAny or kSecAccessControlPrivateKeyUsage),
        ComplianceLevel.OSHA_Compliant to (kSecAccessControlTouchIDAny or kSecAccessControlPrivateKeyUsage or kSecAccessControlApplicationPassword)
    )
    
    // Keychain service names for different credential types
    val SERVICE_NAMES = mapOf(
        CredentialPurpose.AI_SERVICE_API_KEY to "com.hazardhawk.ai-service",
        CredentialPurpose.CLOUD_STORAGE_ACCESS to "com.hazardhawk.cloud-storage",
        CredentialPurpose.AUTH_TOKEN to "com.hazardhawk.auth",
        CredentialPurpose.DATABASE_ACCESS to "com.hazardhawk.database",
        CredentialPurpose.THIRD_PARTY_API to "com.hazardhawk.third-party",
        CredentialPurpose.ENCRYPTION_KEY to "com.hazardhawk.encryption",
        CredentialPurpose.CERTIFICATE_DATA to "com.hazardhawk.certificates",
        CredentialPurpose.OTHER to "com.hazardhawk.other"
    )
    
    // iOS Security Framework specific constants
    object IOSConstants {
        // CommonCrypto algorithm identifiers
        const val AES_BLOCK_SIZE = 16
        const val GCM_IV_SIZE = 12
        const val GCM_TAG_SIZE = 16
        
        // Secure Enclave specific
        const val SE_PRIVATE_KEY_SIZE = 256 // bits
        
        // Key derivation parameters
        const val PBKDF2_ITERATIONS = 100_000
        const val SCRYPT_N = 16384
        const val SCRYPT_R = 8
        const val SCRYPT_P = 1
        
        // Biometric prompt strings
        const val TOUCH_ID_PROMPT = "Use Touch ID to access secure credentials"
        const val FACE_ID_PROMPT = "Use Face ID to access secure credentials"
        const val PASSCODE_PROMPT = "Enter your device passcode to access secure credentials"
        
        // Error handling timeouts
        const val KEYCHAIN_TIMEOUT_SECONDS = 30
        const val BIOMETRIC_TIMEOUT_SECONDS = 60
    }
    
    /**
     * Device capability detection utilities
     */
    object CapabilityDetector {
        
        /**
         * Check if device has Secure Enclave
         */
        fun hasSecureEnclave(): Boolean {
            return try {
                val context = LAContext()
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    val result = context.canEvaluatePolicy(
                        LAPolicyDeviceOwnerAuthenticationWithBiometrics, 
                        error = errorPtr.ptr
                    )
                    result && errorPtr.value == null
                }
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Check if Touch ID is available
         */
        fun hasTouchID(): Boolean {
            return try {
                val context = LAContext()
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    context.canEvaluatePolicy(
                        LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                        error = errorPtr.ptr
                    ) && context.biometryType == LABiometryTypeTouchID
                }
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Check if Face ID is available
         */
        fun hasFaceID(): Boolean {
            return try {
                val context = LAContext()
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    context.canEvaluatePolicy(
                        LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                        error = errorPtr.ptr
                    ) && context.biometryType == LABiometryTypeFaceID
                }
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Check if device has passcode set
         */
        fun hasPasscode(): Boolean {
            return try {
                val context = LAContext()
                memScoped {
                    val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                    context.canEvaluatePolicy(
                        LAPolicyDeviceOwnerAuthentication,
                        error = errorPtr.ptr
                    )
                }
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Get the best available biometric authentication method
         */
        fun getBestBiometricMethod(): String {
            return when {
                hasFaceID() -> "Face ID"
                hasTouchID() -> "Touch ID"
                hasPasscode() -> "Passcode"
                else -> "None"
            }
        }
    }
    
    /**
     * Security validation utilities
     */
    object SecurityValidator {
        
        /**
         * Validate that the device meets minimum security requirements
         */
        fun validateDeviceSecurity(requiredLevel: ComplianceLevel): ValidationResult {
            val issues = mutableListOf<String>()
            
            // Check if device has passcode for enhanced security levels
            if (requiredLevel != ComplianceLevel.Standard && !CapabilityDetector.hasPasscode()) {
                issues.add("Device passcode is required for ${requiredLevel.name} compliance")
            }
            
            // Check biometric availability for critical and OSHA levels
            if (requiredLevel in listOf(ComplianceLevel.Critical, ComplianceLevel.OSHA_Compliant)) {
                if (!CapabilityDetector.hasTouchID() && !CapabilityDetector.hasFaceID()) {
                    issues.add("Biometric authentication is required for ${requiredLevel.name} compliance")
                }
            }
            
            // Check Secure Enclave for critical operations
            if (requiredLevel == ComplianceLevel.OSHA_Compliant && !CapabilityDetector.hasSecureEnclave()) {
                issues.add("Secure Enclave is recommended for OSHA compliance")
            }
            
            return ValidationResult(
                isValid = issues.isEmpty(),
                issues = issues,
                recommendedActions = generateRecommendations(issues)
            )
        }
        
        private fun generateRecommendations(issues: List<String>): List<String> {
            val recommendations = mutableListOf<String>()
            
            issues.forEach { issue ->
                when {
                    "passcode" in issue.lowercase() -> {
                        recommendations.add("Enable device passcode in Settings > Face ID & Passcode")
                    }
                    "biometric" in issue.lowercase() -> {
                        recommendations.add("Set up Face ID or Touch ID in device Settings")
                    }
                    "secure enclave" in issue.lowercase() -> {
                        recommendations.add("Consider using a newer device with Secure Enclave support")
                    }
                }
            }
            
            return recommendations
        }
    }
}

/**
 * Security validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val issues: List<String>,
    val recommendedActions: List<String>
)
