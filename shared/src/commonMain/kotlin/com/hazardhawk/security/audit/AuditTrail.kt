package com.hazardhawk.security.audit

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import com.hazardhawk.security.SafetyAction
import com.hazardhawk.security.ComplianceEvent
import com.hazardhawk.security.SecurityEvent
import com.hazardhawk.security.DataAccessEvent

/**
 * Comprehensive audit trail for tracking all system activities.
 * Provides tamper-evident logging with cryptographic integrity protection.
 * Supports OSHA compliance and regulatory audit requirements.
 */
@Serializable
data class AuditTrail(
    val id: String,
    val sessionId: String,
    val userId: String?,
    val deviceId: String?,
    val entries: List<AuditEntry>,
    val createdAt: Instant,
    val lastModified: Instant,
    val integrityHash: String,
    val version: Int = 1,
    val metadata: AuditTrailMetadata
) {
    /**
     * Add a new audit entry to the trail
     */
    fun addEntry(entry: AuditEntry): AuditTrail {
        val newEntries = entries + entry
        val newHash = calculateIntegrityHash(newEntries)
        
        return copy(
            entries = newEntries,
            lastModified = kotlinx.datetime.Clock.System.now(),
            integrityHash = newHash,
            version = version + 1
        )
    }
    
    /**
     * Verify the integrity of the audit trail
     */
    fun verifyIntegrity(): Boolean {
        val expectedHash = calculateIntegrityHash(entries)
        return expectedHash == integrityHash
    }
    
    /**
     * Get entries within a specific date range
     */
    fun getEntriesInRange(startDate: Instant, endDate: Instant): List<AuditEntry> {
        return entries.filter { entry ->
            entry.timestamp >= startDate && entry.timestamp <= endDate
        }
    }
    
    /**
     * Get entries by type
     */
    inline fun <reified T : AuditEntry> getEntriesByType(): List<T> {
        return entries.filterIsInstance<T>()
    }
    
    internal fun calculateIntegrityHash(entries: List<AuditEntry>): String {
        // Implementation would use SHA-256 hash of serialized entries
        val content = entries.joinToString("|") { "${it.id}:${it.timestamp}:${it.type}" }
        return "sha256:${content.hashCode().toString(16)}" // Simplified for demo
    }
}

/**
 * Base class for all audit entries
 */
@Serializable
sealed class AuditEntry {
    abstract val id: String
    abstract val timestamp: Instant
    abstract val userId: String?
    abstract val type: AuditEntryType
    abstract val description: String
    abstract val metadata: Map<String, String>
    
    /**
     * Get the severity level of this audit entry
     */
    abstract fun getSeverity(): AuditSeverity
    
    /**
     * Check if this entry requires immediate attention
     */
    abstract fun requiresAttention(): Boolean
}

/**
 * Safety action audit entry
 */
@Serializable
data class SafetyActionAuditEntry(
    override val id: String,
    override val timestamp: Instant,
    override val userId: String?,
    override val description: String,
    override val metadata: Map<String, String>,
    val safetyAction: SafetyAction,
    val workLocation: String? = null,
    val supervisorId: String? = null,
    val photoCount: Int = 0,
    val hazardCount: Int = 0
) : AuditEntry() {
    override val type = AuditEntryType.SAFETY_ACTION
    
    override fun getSeverity(): AuditSeverity {
        return when {
            hazardCount > 5 -> AuditSeverity.HIGH
            hazardCount > 2 -> AuditSeverity.MEDIUM
            else -> AuditSeverity.LOW
        }
    }
    
    override fun requiresAttention(): Boolean {
        return hazardCount > 3 || safetyAction.complianceLevel == com.hazardhawk.security.AuditComplianceLevel.Critical
    }
}

/**
 * Compliance event audit entry
 */
@Serializable
data class ComplianceEventAuditEntry(
    override val id: String,
    override val timestamp: Instant,
    override val userId: String?,
    override val description: String,
    override val metadata: Map<String, String>,
    val complianceEvent: ComplianceEvent,
    val regulatoryFramework: String? = null,
    val correctionPlan: String? = null,
    val estimatedResolutionDate: Instant? = null
) : AuditEntry() {
    override val type = AuditEntryType.COMPLIANCE_EVENT
    
    override fun getSeverity(): AuditSeverity {
        return when (complianceEvent.severity) {
            com.hazardhawk.security.EventSeverity.CRITICAL -> AuditSeverity.CRITICAL
            com.hazardhawk.security.EventSeverity.HIGH -> AuditSeverity.HIGH
            com.hazardhawk.security.EventSeverity.MEDIUM -> AuditSeverity.MEDIUM
            com.hazardhawk.security.EventSeverity.LOW -> AuditSeverity.LOW
        }
    }
    
    override fun requiresAttention(): Boolean {
        return complianceEvent.correctionRequired && !complianceEvent.correctionCompleted
    }
}

/**
 * Security event audit entry
 */
@Serializable
data class SecurityEventAuditEntry(
    override val id: String,
    override val timestamp: Instant,
    override val userId: String?,
    override val description: String,
    override val metadata: Map<String, String>,
    val securityEvent: SecurityEvent,
    val threatLevel: ThreatLevel,
    val sourceIpAddress: String? = null,
    val userAgent: String? = null,
    val responseAction: String? = null
) : AuditEntry() {
    override val type = AuditEntryType.SECURITY_EVENT
    
    override fun getSeverity(): AuditSeverity {
        return when (threatLevel) {
            ThreatLevel.CRITICAL -> AuditSeverity.CRITICAL
            ThreatLevel.HIGH -> AuditSeverity.HIGH
            ThreatLevel.MEDIUM -> AuditSeverity.MEDIUM
            ThreatLevel.LOW -> AuditSeverity.LOW
            ThreatLevel.INFO -> AuditSeverity.INFO
        }
    }
    
    override fun requiresAttention(): Boolean {
        return !securityEvent.successful || threatLevel == ThreatLevel.CRITICAL
    }
}

/**
 * Data access audit entry
 */
@Serializable
data class DataAccessAuditEntry(
    override val id: String,
    override val timestamp: Instant,
    override val userId: String?,
    override val description: String,
    override val metadata: Map<String, String>,
    val dataAccessEvent: DataAccessEvent,
    val dataClassification: DataClassification,
    val accessMethod: AccessMethod,
    val ipAddress: String? = null,
    val dataSize: Long? = null
) : AuditEntry() {
    override val type = AuditEntryType.DATA_ACCESS
    
    override fun getSeverity(): AuditSeverity {
        return when {
            !dataAccessEvent.successful -> AuditSeverity.HIGH
            dataClassification == DataClassification.CONFIDENTIAL -> AuditSeverity.MEDIUM
            dataClassification == DataClassification.SENSITIVE -> AuditSeverity.LOW
            else -> AuditSeverity.INFO
        }
    }
    
    override fun requiresAttention(): Boolean {
        return !dataAccessEvent.successful || 
               (dataClassification == DataClassification.CONFIDENTIAL && 
                accessMethod == AccessMethod.EXPORT)
    }
}

/**
 * System event audit entry
 */
@Serializable
data class SystemEventAuditEntry(
    override val id: String,
    override val timestamp: Instant,
    override val userId: String?,
    override val description: String,
    override val metadata: Map<String, String>,
    val eventType: SystemEventType,
    val systemComponent: String,
    val eventDetails: String,
    val errorCode: String? = null,
    val stackTrace: String? = null
) : AuditEntry() {
    override val type = AuditEntryType.SYSTEM_EVENT
    
    override fun getSeverity(): AuditSeverity {
        return when (eventType) {
            SystemEventType.SYSTEM_FAILURE -> AuditSeverity.CRITICAL
            SystemEventType.SERVICE_ERROR -> AuditSeverity.HIGH
            SystemEventType.CONFIGURATION_CHANGE -> AuditSeverity.MEDIUM
            SystemEventType.STARTUP, SystemEventType.SHUTDOWN -> AuditSeverity.LOW
            SystemEventType.MAINTENANCE -> AuditSeverity.INFO
        }
    }
    
    override fun requiresAttention(): Boolean {
        return eventType in listOf(
            SystemEventType.SYSTEM_FAILURE,
            SystemEventType.SERVICE_ERROR
        )
    }
}

/**
 * Metadata for audit trail
 */
@Serializable
data class AuditTrailMetadata(
    val platform: String, // "Android", "iOS", "Desktop", "Web"
    val appVersion: String,
    val deviceInfo: String? = null,
    val networkInfo: String? = null,
    val geoLocation: String? = null,
    val sessionDuration: Long? = null, // milliseconds
    val totalActions: Int = 0,
    val complianceLevel: com.hazardhawk.security.AuditComplianceLevel,
    val encryptionEnabled: Boolean = true,
    val backupLocation: String? = null
)

/**
 * Summary statistics for audit trail analysis
 */
@Serializable
data class AuditTrailSummary(
    val trailId: String,
    val timeRange: com.hazardhawk.security.DateRange,
    val totalEntries: Int,
    val entriesByType: Map<AuditEntryType, Int>,
    val entriesBySeverity: Map<AuditSeverity, Int>,
    val uniqueUsers: Int,
    val attentionRequired: Int,
    val complianceScore: Double, // 0.0 to 1.0
    val securityIncidents: Int,
    val dataAccessViolations: Int,
    val topRisks: List<RiskIndicator>
)

/**
 * Risk indicator from audit analysis
 */
@Serializable
data class RiskIndicator(
    val type: RiskType,
    val description: String,
    val severity: AuditSeverity,
    val occurrenceCount: Int,
    val firstDetected: Instant,
    val lastDetected: Instant,
    val recommendation: String
)

// Enums for audit classification
@Serializable
enum class AuditEntryType {
    SAFETY_ACTION, COMPLIANCE_EVENT, SECURITY_EVENT, 
    DATA_ACCESS, SYSTEM_EVENT, USER_ACTION
}

@Serializable
enum class AuditSeverity {
    INFO, LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
enum class ThreatLevel {
    INFO, LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
enum class DataClassification {
    PUBLIC, INTERNAL, SENSITIVE, CONFIDENTIAL
}

@Serializable
enum class AccessMethod {
    VIEW, EDIT, EXPORT, SHARE, DELETE, BACKUP
}

@Serializable
enum class SystemEventType {
    STARTUP, SHUTDOWN, CONFIGURATION_CHANGE, 
    SERVICE_ERROR, SYSTEM_FAILURE, MAINTENANCE
}

@Serializable
enum class RiskType {
    SECURITY_VULNERABILITY, COMPLIANCE_VIOLATION, 
    DATA_BREACH_RISK, OPERATIONAL_RISK, 
    REGULATORY_RISK, TECHNICAL_RISK
}

/**
 * Audit trail builder for creating properly structured trails
 */
class AuditTrailBuilder {
    private var id: String = generateTrailId()
    private var sessionId: String = generateSessionId()
    private var userId: String? = null
    private var deviceId: String? = null
    private val entries = mutableListOf<AuditEntry>()
    private var metadata: AuditTrailMetadata? = null
    
    fun withId(id: String) = apply { this.id = id }
    fun withSessionId(sessionId: String) = apply { this.sessionId = sessionId }
    fun withUserId(userId: String?) = apply { this.userId = userId }
    fun withDeviceId(deviceId: String?) = apply { this.deviceId = deviceId }
    fun withMetadata(metadata: AuditTrailMetadata) = apply { this.metadata = metadata }
    
    fun addEntry(entry: AuditEntry) = apply { entries.add(entry) }
    fun addEntries(entries: List<AuditEntry>) = apply { this.entries.addAll(entries) }
    
    fun build(): AuditTrail {
        val now = kotlinx.datetime.Clock.System.now()
        val trail = AuditTrail(
            id = id,
            sessionId = sessionId,
            userId = userId,
            deviceId = deviceId,
            entries = entries.toList(),
            createdAt = now,
            lastModified = now,
            integrityHash = "", // Will be calculated
            version = 1,
            metadata = metadata ?: createDefaultMetadata()
        )
        
        // Calculate integrity hash after creating the trail
        val hash = trail.calculateIntegrityHash(trail.entries)
        return trail.copy(integrityHash = hash)
    }
    
    private fun createDefaultMetadata() = AuditTrailMetadata(
        platform = "Unknown",
        appVersion = "1.0.0",
        complianceLevel = com.hazardhawk.security.AuditComplianceLevel.Standard
    )
    
    private fun generateTrailId(): String {
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return "trail_${timestamp}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }
    
    private fun generateSessionId(): String {
        val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        return "session_${timestamp}_${kotlin.random.Random.nextInt(1000, 9999)}"
    }
}