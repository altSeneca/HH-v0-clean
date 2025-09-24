package com.hazardhawk.security

import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.Serializable

/**
 * Security configuration constants and policies for HazardHawk application.
 * Centralized security settings for compliance, encryption, and access control.
 * 
 * These configurations support OSHA compliance requirements and follow
 * industry best practices for construction safety applications.
 */
object SecurityConfig {
    
    // OSHA Compliance Settings
    object OSHACompliance {
        /** OSHA-required data retention period in years */
        const val DATA_RETENTION_YEARS = 5
        
        /** Maximum days before safety correction is considered overdue */
        const val CORRECTION_DEADLINE_DAYS = 30
        
        /** Minimum frequency for safety inspections (days) */
        const val INSPECTION_FREQUENCY_DAYS = 30
        
        /** Required training documentation retention period */
        const val TRAINING_RECORD_RETENTION_YEARS = 3
        
        /** Incident report completion deadline (hours) */
        const val INCIDENT_REPORT_DEADLINE_HOURS = 24
        
        /** Minimum photo resolution for compliance documentation */
        const val MIN_PHOTO_RESOLUTION_PIXELS = 1024 * 768
        
        /** Required metadata fields for compliance photos */
        val REQUIRED_PHOTO_METADATA = setOf(
            "timestamp", "location", "work_type", "inspector_id"
        )
    }
    
    // Encryption Configuration
    object Encryption {
        /** Primary encryption algorithm for data at rest */
        const val PRIMARY_ALGORITHM = "AES-256-GCM"
        
        /** Alternative encryption algorithm for compatibility */
        const val FALLBACK_ALGORITHM = "AES-256-CBC"
        
        /** Key derivation algorithm */
        const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
        
        /** Key derivation iterations for enhanced security */
        const val KEY_DERIVATION_ITERATIONS = 100_000
        
        /** Salt length for key derivation */
        const val SALT_LENGTH_BYTES = 32
        
        /** Initialization vector length */
        const val IV_LENGTH_BYTES = 16
        
        /** Authentication tag length for GCM mode */
        const val AUTH_TAG_LENGTH_BYTES = 16
        
        /** Key rotation interval in days */
        const val KEY_ROTATION_INTERVAL_DAYS = 90
        
        /** Maximum key usage count before rotation */
        const val MAX_KEY_USAGE_COUNT = 1_000_000
    }
    
    // Certificate Pinning Configuration
    object CertificatePinning {
        /** Google Gemini API certificate pins (SHA-256) */
        val GEMINI_API_PINS = setOf(
            "sha256/5kJvNEMw0KjrCAu7eXY5HgpHoQUhPbWjEyxKmJPEYBo=", // Primary
            "sha256/r/mIkG3eEpVdm+u/ko/cwdkwMPQHYoEYExoSHkDo6cA=", // Backup
            "sha256/Y0XF2o4j3O3+1f6Y+7PVrKqLfW6x4yVgHjJcQOq+Duk="  // Root CA
        )
        
        /** AWS S3 certificate pins (SHA-256) */
        val S3_API_PINS = setOf(
            "sha256/JSMzqOOrtyOT1kmau6zKhgT676hGgczD5VMdRMyJZFA=", // Primary
            "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=", // Backup
            "sha256/gI1os/q0iEpflxrOfRBVDXqVoWN3Tz7Sk2VmLvOAaxU="  // Root CA
        )
        
        /** Backend API certificate pins (SHA-256) */
        val BACKEND_API_PINS = setOf(
            "sha256/REPLACE_WITH_ACTUAL_BACKEND_PINS_IN_PRODUCTION" // Placeholder
        )
        
        /** Certificate pin validation timeout in milliseconds */
        const val PIN_VALIDATION_TIMEOUT_MS = 10_000L
        
        /** Allow pinning bypass in debug builds */
        const val ALLOW_DEBUG_BYPASS = true
        
        /** Pinning failure retry attempts */
        const val PIN_FAILURE_RETRY_COUNT = 3
    }
    
    // Authentication & Access Control
    object Authentication {
        /** JWT token expiration time in minutes */
        const val JWT_EXPIRATION_MINUTES = 60
        
        /** Refresh token expiration time in days */
        const val REFRESH_TOKEN_EXPIRATION_DAYS = 30
        
        /** Maximum failed login attempts before lockout */
        const val MAX_FAILED_LOGIN_ATTEMPTS = 5
        
        /** Account lockout duration in minutes */
        const val LOCKOUT_DURATION_MINUTES = 15
        
        /** Password minimum length */
        const val MIN_PASSWORD_LENGTH = 8
        
        /** Require special characters in password */
        const val REQUIRE_SPECIAL_CHARS = true
        
        /** Session timeout in minutes for inactive users */
        const val SESSION_TIMEOUT_MINUTES = 30
        
        /** Two-factor authentication requirement */
        const val REQUIRE_2FA = false // Can be enabled per deployment
    }
    
    // Audit Logging Configuration
    object AuditLogging {
        /** Maximum log entry size in bytes */
        const val MAX_LOG_ENTRY_SIZE_BYTES = 64 * 1024 // 64KB
        
        /** Log rotation size threshold in bytes */
        const val LOG_ROTATION_SIZE_BYTES = 100 * 1024 * 1024 // 100MB
        
        /** Maximum number of archived log files */
        const val MAX_ARCHIVED_LOG_FILES = 10
        
        /** Log integrity check interval in hours */
        const val INTEGRITY_CHECK_INTERVAL_HOURS = 24
        
        /** Compress archived logs */
        const val COMPRESS_ARCHIVED_LOGS = true
        
        /** Encrypt log files */
        const val ENCRYPT_LOG_FILES = true
        
        /** Remote backup of critical logs */
        const val ENABLE_REMOTE_LOG_BACKUP = true
        
        /** Real-time log monitoring for security events */
        const val ENABLE_REALTIME_MONITORING = true
    }
    
    // Data Protection Configuration
    object DataProtection {
        /** Enable data minimization practices */
        const val ENABLE_DATA_MINIMIZATION = true
        
        /** Automatic PII detection and masking */
        const val AUTO_PII_MASKING = true
        
        /** Data export encryption requirement */
        const val REQUIRE_EXPORT_ENCRYPTION = true
        
        /** User consent expiration time in days */
        const val CONSENT_EXPIRATION_DAYS = 365
        
        /** Data retention policy enforcement */
        const val ENFORCE_RETENTION_POLICY = true
        
        /** Automatic data deletion for expired data */
        const val AUTO_DELETE_EXPIRED_DATA = true
        
        /** Cross-border data transfer restrictions */
        const val RESTRICT_CROSS_BORDER_TRANSFER = false
    }
    
    // Network Security Configuration
    object NetworkSecurity {
        /** Enforce HTTPS for all communications */
        const val ENFORCE_HTTPS = true
        
        /** Minimum TLS version */
        const val MIN_TLS_VERSION = "1.2"
        
        /** Request timeout in seconds */
        const val REQUEST_TIMEOUT_SECONDS = 30L
        
        /** Connection timeout in seconds */
        const val CONNECTION_TIMEOUT_SECONDS = 15L
        
        /** Maximum retry attempts for failed requests */
        const val MAX_RETRY_ATTEMPTS = 3
        
        /** Retry delay in milliseconds */
        const val RETRY_DELAY_MS = 1000L
        
        /** User agent string for API requests */
        const val USER_AGENT = "HazardHawk/1.0 (Construction Safety Platform)"
        
        /** Rate limiting configuration */
        const val RATE_LIMIT_REQUESTS_PER_MINUTE = 100
    }
    
    // File Security Configuration
    object FileSecurity {
        /** Maximum file size for photo uploads in bytes */
        const val MAX_PHOTO_SIZE_BYTES = 50 * 1024 * 1024 // 50MB
        
        /** Allowed photo file extensions */
        val ALLOWED_PHOTO_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
        
        /** Allowed export file formats */
        val ALLOWED_EXPORT_FORMATS = setOf("pdf", "json", "csv", "xml")
        
        /** Scan uploaded files for malware */
        const val ENABLE_MALWARE_SCANNING = true
        
        /** Quarantine suspicious files */
        const val QUARANTINE_SUSPICIOUS_FILES = true
        
        /** Maximum concurrent file uploads */
        const val MAX_CONCURRENT_UPLOADS = 3
        
        /** Enable file integrity verification */
        const val VERIFY_FILE_INTEGRITY = true
    }
    
    // Platform-Specific Configurations
    object Platform {
        /** Android-specific settings */
        object Android {
            /** Use Android Keystore for key storage */
            const val USE_KEYSTORE = true
            
            /** Require device lock for app access */
            const val REQUIRE_DEVICE_LOCK = false
            
            /** Enable biometric authentication */
            const val ENABLE_BIOMETRIC_AUTH = true
            
            /** Root detection sensitivity */
            const val ROOT_DETECTION_LEVEL = 2 // 0=off, 1=basic, 2=strict
        }
        
        /** iOS-specific settings */
        object IOS {
            /** Use iOS Keychain for secure storage */
            const val USE_KEYCHAIN = true
            
            /** Require Face ID/Touch ID */
            const val REQUIRE_BIOMETRIC_AUTH = false
            
            /** Jailbreak detection sensitivity */
            const val JAILBREAK_DETECTION_LEVEL = 2
            
            /** App Transport Security level */
            const val ATS_LEVEL = 2
        }
        
        /** Desktop-specific settings */
        object Desktop {
            /** Use system credential store */
            const val USE_SYSTEM_CREDENTIAL_STORE = true
            
            /** Require screen lock */
            const val REQUIRE_SCREEN_LOCK = false
            
            /** Enable remote desktop detection */
            const val DETECT_REMOTE_DESKTOP = true
        }
        
        /** Web-specific settings */
        object Web {
            /** Use secure contexts only */
            const val REQUIRE_SECURE_CONTEXT = true
            
            /** Enable Content Security Policy */
            const val ENABLE_CSP = true
            
            /** Strict transport security max age */
            const val HSTS_MAX_AGE_SECONDS = 31536000L // 1 year
        }
    }
    
    // Emergency & Incident Response
    object IncidentResponse {
        /** Emergency contact for security incidents */
        const val EMERGENCY_CONTACT = "security@hazardhawk.com"
        
        /** Incident escalation threshold (severity level) */
        const val ESCALATION_THRESHOLD = "HIGH"
        
        /** Automatic incident reporting */
        const val AUTO_INCIDENT_REPORTING = true
        
        /** Maximum response time for critical incidents (minutes) */
        const val CRITICAL_INCIDENT_RESPONSE_TIME_MINUTES = 60
        
        /** Enable emergency data wipe capability */
        const val ENABLE_EMERGENCY_WIPE = true
        
        /** Breach notification timeline (hours) */
        const val BREACH_NOTIFICATION_DEADLINE_HOURS = 72
    }
}

/**
 * Security policy enforcement levels
 */
@Serializable
enum class SecurityPolicyLevel {
    /** Basic security measures */
    BASIC,
    /** Standard corporate security */
    STANDARD,
    /** Enhanced security for sensitive environments */
    ENHANCED,
    /** Maximum security for high-risk environments */
    MAXIMUM,
    /** Custom security configuration */
    CUSTOM
}

/**
 * Security configuration profile for different deployment environments
 */
@Serializable
data class SecurityProfile(
    val name: String,
    val description: String,
    val policyLevel: SecurityPolicyLevel,
    val oshaCompliant: Boolean = true,
    val encryptionRequired: Boolean = true,
    val auditLoggingEnabled: Boolean = true,
    val certificatePinningEnabled: Boolean = true,
    val customSettings: Map<String, String> = emptyMap()
) {
    companion object {
        /** Development environment profile */
        fun development() = SecurityProfile(
            name = "Development",
            description = "Relaxed security for development and testing",
            policyLevel = SecurityPolicyLevel.BASIC,
            certificatePinningEnabled = false
        )
        
        /** Production environment profile */
        fun production() = SecurityProfile(
            name = "Production",
            description = "Full security for production deployment",
            policyLevel = SecurityPolicyLevel.ENHANCED
        )
        
        /** High-security environment profile */
        fun highSecurity() = SecurityProfile(
            name = "High Security",
            description = "Maximum security for sensitive environments",
            policyLevel = SecurityPolicyLevel.MAXIMUM,
            customSettings = mapOf(
                "require_2fa" to "true",
                "max_session_duration" to "15",
                "force_key_rotation" to "true"
            )
        )
    }
}

/**
 * Runtime security configuration that can be updated
 */
@Serializable
data class RuntimeSecurityConfig(
    val profile: SecurityProfile,
    val overrides: Map<String, String> = emptyMap(),
    val lastUpdated: kotlinx.datetime.Instant,
    val updatedBy: String
)