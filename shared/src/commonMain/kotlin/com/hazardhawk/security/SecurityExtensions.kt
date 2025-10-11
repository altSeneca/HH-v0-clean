package com.hazardhawk.security

import kotlinx.datetime.Clock

/**
 * Extension methods to provide simplified API for security services.
 * These bridge the gap between the full interface API and simplified usage patterns
 * used throughout the AI and analysis code.
 */

/**
 * Get a string value from secure storage (simplified API)
 */
suspend fun SecureStorageService.getString(key: String): String? {
    return getApiKey(key).getOrNull()
}

/**
 * Set a string value in secure storage (simplified API)
 */
suspend fun SecureStorageService.setString(key: String, value: String): Boolean {
    return storeApiKey(key, value, metadata = null).isSuccess
}

/**
 * Encrypt data using photo encryption service (simplified API)
 */
suspend fun PhotoEncryptionService.encryptData(data: ByteArray): ByteArray {
    // Generate a unique ID for this encryption operation
    val photoId = "generic_${Clock.System.now().toEpochMilliseconds()}"
    return encryptPhoto(data, photoId, compressionLevel = 0).getOrNull()?.encryptedData ?: data
}

/**
 * Decrypt data using photo encryption service (simplified API)
 */
suspend fun PhotoEncryptionService.decryptData(encryptedData: ByteArray): ByteArray {
    // For simplified decrypt, we assume the data is already in EncryptedPhoto format
    // This is a limitation of the simplified API - full API should be used for production
    val now = Clock.System.now()
    val encryptedPhoto = EncryptedPhoto(
        photoId = "generic_decrypt",
        encryptedData = encryptedData,
        initializationVector = ByteArray(12), // GCM uses 96-bit IV
        authenticationTag = ByteArray(16), // GCM uses 128-bit tag
        keyId = "default_key",
        encryptionAlgorithm = "AES-256-GCM",
        compressionUsed = false,
        originalSize = encryptedData.size.toLong(),
        encryptedAt = now,
        integrity = IntegrityMetadata(
            checksum = "",
            algorithm = "SHA-256",
            createdAt = now
        )
    )
    return decryptPhoto(encryptedPhoto).getOrNull() ?: encryptedData
}

/**
 * Extension function to provide a generic logEvent method for backward compatibility.
 * Maps generic event logging calls to appropriate specific audit logging methods.
 * 
 * This function intelligently routes events based on their type to the appropriate
 * specialized logging method (SafetyAction, ComplianceEvent, SecurityEvent, or DataAccessEvent).
 */
suspend fun AuditLogger.logEvent(
    eventType: String,
    details: Map<String, String>,
    userId: String? = null,
    metadata: Map<String, String> = emptyMap()
) {
    val timestamp = Clock.System.now()
    
    when {
        eventType.contains("HAZARD", ignoreCase = true) ||
        eventType.contains("SAFETY", ignoreCase = true) ||
        eventType.contains("ALERT", ignoreCase = true) -> {
            // Map to SafetyAction
            val actionType = when {
                eventType.contains("DETECTION") -> SafetyActionType.HAZARD_IDENTIFICATION
                eventType.contains("INCIDENT") -> SafetyActionType.INCIDENT_REPORT
                eventType.contains("INSPECTION") -> SafetyActionType.EQUIPMENT_INSPECTION
                else -> SafetyActionType.COMPLIANCE_CHECK
            }
            
            logSafetyAction(
                SafetyAction(
                    userId = userId ?: "system",
                    actionType = actionType,
                    description = details.values.joinToString(", "),
                    photoIds = listOfNotNull(details["photoId"]),
                    analysisId = details["analysisId"],
                    location = metadata["location"],
                    projectId = details["projectId"],
                    timestamp = timestamp,
                    metadata = details + metadata
                )
            )
        }
        
        eventType.contains("COMPLIANCE", ignoreCase = true) ||
        eventType.contains("VIOLATION", ignoreCase = true) ||
        eventType.contains("CORRECTION", ignoreCase = true) -> {
            // Map to ComplianceEvent
            val complianceType = when {
                eventType.contains("VIOLATION") -> ComplianceEventType.VIOLATION_IDENTIFIED
                eventType.contains("CORRECTION") -> ComplianceEventType.CORRECTION_REQUIRED
                eventType.contains("TRAINING") -> ComplianceEventType.TRAINING_REQUIRED
                eventType.contains("AUDIT") -> ComplianceEventType.AUDIT_FINDING
                else -> ComplianceEventType.POLICY_VIOLATION
            }
            
            val severity = when (details["severity"]?.uppercase()) {
                "CRITICAL" -> EventSeverity.CRITICAL
                "HIGH" -> EventSeverity.HIGH
                "MEDIUM" -> EventSeverity.MEDIUM
                else -> EventSeverity.LOW
            }
            
            logComplianceEvent(
                ComplianceEvent(
                    eventType = complianceType,
                    severity = severity,
                    description = details.values.joinToString(", "),
                    userId = userId,
                    affectedEntities = listOfNotNull(details["photoId"], details["analysisId"]),
                    correctionRequired = details["correctionRequired"]?.toBoolean() ?: false,
                    regulatoryReference = details["standard"] ?: details["oshaStandard"],
                    timestamp = timestamp,
                    metadata = details + metadata
                )
            )
        }
        
        eventType.contains("SYSTEM", ignoreCase = true) ||
        eventType.contains("HEALTH", ignoreCase = true) ||
        eventType.contains("PERFORMANCE", ignoreCase = true) -> {
            // Map to SecurityEvent (using it for system events)
            val securityType = when {
                eventType.contains("CONFIG") -> SecurityEventType.SYSTEM_CONFIGURATION
                eventType.contains("ENCRYPTION") -> SecurityEventType.ENCRYPTION_EVENT
                else -> SecurityEventType.SYSTEM_CONFIGURATION
            }
            
            val severity = when (details["status"]?.uppercase() ?: details["severity"]?.uppercase()) {
                "CRITICAL", "DOWN", "FAILING" -> EventSeverity.CRITICAL
                "HIGH", "DEGRADED" -> EventSeverity.HIGH
                "MEDIUM" -> EventSeverity.MEDIUM
                else -> EventSeverity.LOW
            }
            
            logSecurityEvent(
                SecurityEvent(
                    eventType = securityType,
                    severity = severity,
                    description = "${details["component"] ?: "System"}: ${details.values.joinToString(", ")}",
                    userId = userId,
                    successful = details["status"] != "FAILING" && details["status"] != "DOWN",
                    timestamp = timestamp,
                    metadata = details + metadata
                )
            )
        }
        
        eventType.contains("ACCESS", ignoreCase = true) ||
        eventType.contains("DATA", ignoreCase = true) -> {
            // Map to DataAccessEvent
            logDataAccessEvent(
                DataAccessEvent(
                    accessType = DataAccessType.READ,
                    dataType = when {
                        details.containsKey("photoId") -> DataType.PHOTO
                        details.containsKey("analysisId") -> DataType.ANALYSIS
                        else -> DataType.METADATA
                    },
                    entityId = details["photoId"] ?: details["analysisId"] ?: details["entityId"] ?: "unknown",
                    userId = userId ?: "system",
                    purpose = eventType,
                    successful = true,
                    timestamp = timestamp,
                    metadata = details + metadata
                )
            )
        }
        
        else -> {
            // Default to SecurityEvent for unknown types
            logSecurityEvent(
                SecurityEvent(
                    eventType = SecurityEventType.SYSTEM_CONFIGURATION,
                    severity = EventSeverity.LOW,
                    description = "$eventType: ${details.values.joinToString(", ")}",
                    userId = userId,
                    successful = true,
                    timestamp = timestamp,
                    metadata = details + metadata
                )
            )
        }
    }
}
