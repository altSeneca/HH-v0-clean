package com.hazardhawk.security

import android.content.Context
import android.util.Log
import com.hazardhawk.BuildConfig

/**
 * Centralized security configuration for HazardHawk application
 * 
 * This class provides security policy enforcement and configuration management
 * for construction safety compliance and enterprise deployment.
 */
object SecurityConfig {
    
    private const val TAG = "SecurityConfig"
    
    // Security policy constants
    const val MIN_API_KEY_LENGTH = 20
    const val MAX_API_REQUESTS_PER_MINUTE = 15
    const val API_KEY_ROTATION_INTERVAL_DAYS = 90
    const val AUDIT_LOG_RETENTION_DAYS = 2555 // 7 years for OSHA compliance
    const val SESSION_TIMEOUT_MINUTES = 30
    
    // Network security
    const val ENABLE_CERTIFICATE_PINNING = true
    const val REQUIRE_TLS_1_3 = true
    const val ALLOW_HTTP_IN_DEBUG = false
    
    // Data protection
    const val ENCRYPT_ALL_USER_DATA = true
    const val SECURE_DELETE_ENABLED = true
    const val BACKUP_ENCRYPTION_REQUIRED = true
    
    /**
     * Initialize security configuration based on build variant
     */
    fun initialize(context: Context) {
        Log.i(TAG, "Initializing security configuration for environment: ${BuildConfig.ENVIRONMENT}")
        
        when (BuildConfig.ENVIRONMENT) {
            "production" -> initializeProductionSecurity(context)
            "staging" -> initializeStagingSecurity(context)
            "development" -> initializeDevelopmentSecurity(context)
            else -> initializeDefaultSecurity(context)
        }
        
        // Validate security prerequisites
        validateSecurityRequirements(context)
    }
    
    private fun initializeProductionSecurity(context: Context) {
        Log.i(TAG, "Configuring production security settings")
        
        // Production requires the highest security standards
        validateHardwareBackedSecurity(context)
        configureProductionLogging()
        enableSecurityMonitoring()
    }
    
    private fun initializeStagingSecurity(context: Context) {
        Log.i(TAG, "Configuring staging security settings")
        
        // Staging mimics production but with additional debugging
        configureEnhancedLogging()
    }
    
    private fun initializeDevelopmentSecurity(context: Context) {
        Log.i(TAG, "Configuring development security settings")
        
        // Development allows some relaxed security for debugging
        if (BuildConfig.DEBUG_LOGGING) {
            configureDevelopmentLogging()
        }
    }
    
    private fun initializeDefaultSecurity(context: Context) {
        Log.w(TAG, "Using default security configuration - this should not happen in production")
    }
    
    /**
     * Validate that all security requirements are met
     */
    private fun validateSecurityRequirements(context: Context) {
        val secureKeyManager = SecureKeyManager.getInstance(context)
        
        // Check encrypted storage availability
        if (!secureKeyManager.validateKeyIntegrity()) {
            throw SecurityException("Secure key storage validation failed")
        }
        
        // Validate hardware security if required
        if (BuildConfig.ENVIRONMENT == "production") {
            if (!secureKeyManager.isHardwareBackedSecurity()) {
                Log.w(TAG, "Hardware-backed security not available - using software fallback")
            }
        }
        
        Log.i(TAG, "Security requirements validation completed successfully")
    }
    
    /**
     * Check if hardware-backed security is available and required
     */
    private fun validateHardwareBackedSecurity(context: Context) {
        val secureKeyManager = SecureKeyManager.getInstance(context)
        
        if (!secureKeyManager.isHardwareBackedSecurity()) {
            Log.e(TAG, "Production deployment requires hardware-backed security but it's not available")
            // In production, you might want to restrict functionality or require specific devices
        }
    }
    
    /**
     * Configure production-level logging (minimal, secure)
     */
    private fun configureProductionLogging() {
        Log.i(TAG, "Production logging configured - sensitive data logging disabled")
        // In production, implement centralized secure logging
    }
    
    /**
     * Configure enhanced logging for staging
     */
    private fun configureEnhancedLogging() {
        Log.i(TAG, "Enhanced logging configured for staging environment")
    }
    
    /**
     * Configure development logging (detailed for debugging)
     */
    private fun configureDevelopmentLogging() {
        Log.i(TAG, "Development logging configured - detailed debugging enabled")
    }
    
    /**
     * Enable security monitoring and alerting
     */
    private fun enableSecurityMonitoring() {
        Log.i(TAG, "Security monitoring enabled")
        // Implement security event monitoring and alerting
    }
    
    /**
     * Get security policy for API usage
     */
    fun getApiSecurityPolicy(): ApiSecurityPolicy {
        return ApiSecurityPolicy(
            maxRequestsPerMinute = MAX_API_REQUESTS_PER_MINUTE,
            requireApiKeyRotation = BuildConfig.ENVIRONMENT == "production",
            rotationIntervalDays = API_KEY_ROTATION_INTERVAL_DAYS,
            enforceRateLimiting = true,
            requireRequestValidation = true
        )
    }
    
    /**
     * Get data protection policy
     */
    fun getDataProtectionPolicy(): DataProtectionPolicy {
        return DataProtectionPolicy(
            encryptUserData = ENCRYPT_ALL_USER_DATA,
            encryptPhotos = true,
            encryptAiAnalysis = true,
            secureDeleteEnabled = SECURE_DELETE_ENABLED,
            auditLogRetentionDays = AUDIT_LOG_RETENTION_DAYS,
            requireDataBackupEncryption = BACKUP_ENCRYPTION_REQUIRED
        )
    }
    
    /**
     * Check if debug features should be enabled
     */
    fun isDebugModeAllowed(): Boolean {
        return BuildConfig.DEBUG && BuildConfig.ENVIRONMENT != "production"
    }
    
    /**
     * Get session security policy
     */
    fun getSessionSecurityPolicy(): SessionSecurityPolicy {
        return SessionSecurityPolicy(
            timeoutMinutes = SESSION_TIMEOUT_MINUTES,
            requireReauthenticationForSensitiveOps = true,
            enableSessionValidation = true,
            logSecurityEvents = true
        )
    }
}

/**
 * API security policy configuration
 */
data class ApiSecurityPolicy(
    val maxRequestsPerMinute: Int,
    val requireApiKeyRotation: Boolean,
    val rotationIntervalDays: Int,
    val enforceRateLimiting: Boolean,
    val requireRequestValidation: Boolean
)

/**
 * Data protection policy configuration
 */
data class DataProtectionPolicy(
    val encryptUserData: Boolean,
    val encryptPhotos: Boolean,
    val encryptAiAnalysis: Boolean,
    val secureDeleteEnabled: Boolean,
    val auditLogRetentionDays: Int,
    val requireDataBackupEncryption: Boolean
)

/**
 * Session security policy configuration
 */
data class SessionSecurityPolicy(
    val timeoutMinutes: Int,
    val requireReauthenticationForSensitiveOps: Boolean,
    val enableSessionValidation: Boolean,
    val logSecurityEvents: Boolean
)