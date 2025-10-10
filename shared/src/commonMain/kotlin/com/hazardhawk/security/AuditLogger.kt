package com.hazardhawk.security

import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.serialization.Serializable
import com.hazardhawk.core.models.HazardType
import com.hazardhawk.core.models.WorkType

/**
 * Interface for audit logging and compliance monitoring.
 * Provides tamper-evident logging for OSHA compliance and regulatory requirements.
 * Implements 5-year data retention policy with secure log storage.
 * 
 * Platform implementations:
 * - Android: SQLite with encryption + secure backup to cloud
 * - iOS: Core Data with encryption + iCloud secure storage
 * - Desktop: SQLite with platform-specific security measures
 * - Web: IndexedDB with encryption + server-side backup
 */
interface AuditLogger {
    
    /**
     * Log a safety-related action for OSHA compliance
     * @param action Safety action performed by user
     * @return Result indicating success or failure
     */
    suspend fun logSafetyAction(action: SafetyAction): Result<Unit>
    
    /**
     * Log a compliance event (violations, corrections, etc.)
     * @param event Compliance-related event
     * @return Result indicating success or failure
     */
    suspend fun logComplianceEvent(event: ComplianceEvent): Result<Unit>
    
    /**
     * Log system security events (authentication, access control, etc.)
     * @param event Security event details
     * @return Result indicating success or failure
     */
    suspend fun logSecurityEvent(event: SecurityEvent): Result<Unit>
    
    /**
     * Log data access events for privacy compliance
     * @param event Data access event details
     * @return Result indicating success or failure
     */
    suspend fun logDataAccessEvent(event: DataAccessEvent): Result<Unit>
    
    /**
     * Generate comprehensive audit report for specified date range
     * @param dateRange Time period for the report
     * @param reportType Type of audit report to generate
     * @return Result containing generated audit report
     */
    suspend fun generateAuditReport(
        dateRange: DateRange,
        reportType: AuditReportType = AuditReportType.COMPREHENSIVE
    ): Result<AuditReport>
    
    /**
     * Retrieve audit logs for specific criteria
     * @param criteria Search and filter criteria
     * @param limit Maximum number of records to return
     * @param offset Number of records to skip for pagination
     * @return Result containing matching audit log entries
     */
    suspend fun queryAuditLogs(
        criteria: AuditQueryCriteria,
        limit: Int = 100,
        offset: Int = 0
    ): Result<AuditLogQueryResult>
    
    /**
     * Verify the integrity of audit logs (tamper detection)
     * @param dateRange Optional date range to verify (null for all logs)
     * @return Result containing integrity verification results
     */
    suspend fun verifyLogIntegrity(dateRange: DateRange? = null): Result<LogIntegrityResult>
    
    /**
     * Archive old audit logs according to retention policy
     * @param cutoffDate Archive logs older than this date
     * @return Result containing archival statistics
     */
    suspend fun archiveOldLogs(cutoffDate: Instant): Result<ArchivalResult>
    
    /**
     * Export audit logs for external compliance systems
     * @param dateRange Time period to export
     * @param format Export format (JSON, CSV, XML)
     * @return Result containing exported data
     */
    suspend fun exportAuditLogs(
        dateRange: DateRange,
        format: ExportFormat = ExportFormat.JSON
    ): Result<ExportedAuditData>
    
    /**
     * Get audit logging statistics for monitoring
     * @return Current audit logging metrics
     */
    suspend fun getAuditStatistics(): AuditStatistics
}

/**
 * Safety action performed by user for OSHA compliance tracking
 */
@Serializable
data class SafetyAction(
    val id: String = generateId(),
    val userId: String,
    val actionType: SafetyActionType,
    val description: String,
    val workType: WorkType? = null,
    val hazardTypes: List<HazardType> = emptyList(),
    val photoIds: List<String> = emptyList(),
    val analysisId: String? = null,
    val location: String? = null,
    val projectId: String? = null,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap(),
    val oshaReference: String? = null,
    val complianceLevel: ComplianceLevel = ComplianceLevel.Standard
)

/**
 * Compliance event for regulatory tracking
 */
@Serializable
data class ComplianceEvent(
    val id: String = generateId(),
    val eventType: ComplianceEventType,
    val severity: EventSeverity,
    val description: String,
    val userId: String? = null,
    val affectedEntities: List<String> = emptyList(), // photo IDs, analysis IDs, etc.
    val correctionRequired: Boolean = false,
    val correctionDeadline: Instant? = null,
    val correctionCompleted: Boolean = false,
    val regulatoryReference: String? = null,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Security event for system security monitoring
 */
@Serializable
data class SecurityEvent(
    val id: String = generateId(),
    val eventType: SecurityEventType,
    val severity: EventSeverity,
    val description: String,
    val userId: String? = null,
    val ipAddress: String? = null,
    val deviceId: String? = null,
    val userAgent: String? = null,
    val successful: Boolean,
    val failureReason: String? = null,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Data access event for privacy compliance
 */
@Serializable
data class DataAccessEvent(
    val id: String = generateId(),
    val accessType: DataAccessType,
    val dataType: DataType,
    val entityId: String, // ID of the accessed data
    val userId: String,
    val purpose: String,
    val legalBasis: String? = null,
    val consentId: String? = null,
    val successful: Boolean,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Date range for audit queries and reports
 */
@Serializable
data class DateRange(
    val startDate: Instant,
    val endDate: Instant
) {
    init {
        require(startDate <= endDate) { "Start date must be before or equal to end date" }
    }
    
    /**
     * Check if a timestamp falls within this range
     */
    fun contains(timestamp: Instant): Boolean {
        return timestamp >= startDate && timestamp <= endDate
    }
    
    /**
     * Get the duration of this range in days
     */
    fun getDurationInDays(): Long {
        return startDate.until(endDate, DateTimeUnit.DAY)
    }
    
    companion object {
        /**
         * Create a date range for the last N days
         */
        fun lastDays(days: Int): DateRange {
            val now = kotlinx.datetime.Clock.System.now()
            val start = now.minus(days, DateTimeUnit.DAY)
            return DateRange(start, now)
        }
        
        /**
         * Create a date range for a specific month
         */
        fun forMonth(year: Int, month: Int): DateRange {
            // Implementation would use platform-specific date handling
            val now = kotlinx.datetime.Clock.System.now()
            return DateRange(now, now) // Placeholder
        }
    }
}

/**
 * Comprehensive audit report containing all compliance data
 */
@Serializable
data class AuditReport(
    val id: String = generateId(),
    val reportType: AuditReportType,
    val dateRange: DateRange,
    val generatedAt: Instant,
    val generatedBy: String,
    val safetyActionSummary: SafetyActionSummary,
    val complianceEventSummary: ComplianceEventSummary,
    val securityEventSummary: SecurityEventSummary,
    val dataAccessSummary: DataAccessSummary,
    val recommendations: List<String> = emptyList(),
    val nonComplianceIssues: List<NonComplianceIssue> = emptyList(),
    val integrityVerification: LogIntegrityResult,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Query criteria for searching audit logs
 */
@Serializable
data class AuditQueryCriteria(
    val dateRange: DateRange? = null,
    val userIds: List<String> = emptyList(),
    val actionTypes: List<SafetyActionType> = emptyList(),
    val eventTypes: List<ComplianceEventType> = emptyList(),
    val severityLevels: List<EventSeverity> = emptyList(),
    val projectIds: List<String> = emptyList(),
    val searchText: String? = null,
    val includeMetadata: Boolean = false
)

/**
 * Result of audit log query
 */
@Serializable
data class AuditLogQueryResult(
    val safetyActions: List<SafetyAction>,
    val complianceEvents: List<ComplianceEvent>,
    val securityEvents: List<SecurityEvent>,
    val dataAccessEvents: List<DataAccessEvent>,
    val totalCount: Int,
    val hasMore: Boolean
)

/**
 * Result of log integrity verification
 */
@Serializable
data class LogIntegrityResult(
    val isIntact: Boolean,
    val totalLogsChecked: Int,
    val corruptedLogs: Int,
    val missingLogs: Int,
    val lastVerifiedTimestamp: Instant,
    val verificationMethod: String,
    val issues: List<IntegrityIssue> = emptyList()
)

/**
 * Result of log archival operation
 */
@Serializable
data class ArchivalResult(
    val archivedLogCount: Int,
    val archivedDataSize: Long, // bytes
    val oldestArchivedLog: Instant?,
    val newestArchivedLog: Instant?,
    val archiveLocation: String,
    val compressionUsed: Boolean
)

/**
 * Exported audit data
 */
@Serializable
data class ExportedAuditData(
    val format: ExportFormat,
    val data: String, // Serialized data in requested format
    val recordCount: Int,
    val exportedAt: Instant,
    val checksum: String,
    val compressedSize: Long,
    val originalSize: Long
)

/**
 * Audit logging statistics
 */
@Serializable
data class AuditStatistics(
    val totalSafetyActions: Long,
    val totalComplianceEvents: Long,
    val totalSecurityEvents: Long,
    val totalDataAccessEvents: Long,
    val logsPerDay: Double,
    val storageUsed: Long, // bytes
    val oldestLogDate: Instant?,
    val newestLogDate: Instant?,
    val integrityLastChecked: Instant?,
    val complianceScore: Double // 0.0 to 1.0
)

// Summary classes for audit reports
@Serializable
data class SafetyActionSummary(
    val totalActions: Int,
    val actionsByType: Map<SafetyActionType, Int>,
    val actionsByWorkType: Map<WorkType, Int>,
    val hazardsIdentified: Int,
    val photosAnalyzed: Int
)

@Serializable
data class ComplianceEventSummary(
    val totalEvents: Int,
    val eventsBySeverity: Map<EventSeverity, Int>,
    val openCorrections: Int,
    val overduCorrections: Int,
    val complianceRate: Double
)

@Serializable
data class SecurityEventSummary(
    val totalEvents: Int,
    val successfulEvents: Int,
    val failedEvents: Int,
    val eventsByType: Map<SecurityEventType, Int>,
    val uniqueUsers: Int,
    val suspiciousActivity: Int
)

@Serializable
data class DataAccessSummary(
    val totalAccesses: Int,
    val accessesByType: Map<DataAccessType, Int>,
    val accessesByDataType: Map<DataType, Int>,
    val uniqueUsers: Int,
    val consentBasedAccesses: Int
)

@Serializable
data class NonComplianceIssue(
    val type: ComplianceIssueType,
    val description: String,
    val severity: EventSeverity,
    val firstOccurrence: Instant,
    val occurrenceCount: Int,
    val recommendation: String
)

@Serializable
data class IntegrityIssue(
    val type: IntegrityIssueType,
    val description: String,
    val affectedLogId: String,
    val detectedAt: Instant
)

// Enums for classification
@Serializable
enum class SafetyActionType {
    PHOTO_CAPTURE, HAZARD_IDENTIFICATION, ANALYSIS_REVIEW, 
    INCIDENT_REPORT, SAFETY_BRIEFING, TRAINING_COMPLETION,
    EQUIPMENT_INSPECTION, COMPLIANCE_CHECK, CORRECTION_ACTION
}

@Serializable
enum class ComplianceEventType {
    VIOLATION_IDENTIFIED, CORRECTION_REQUIRED, CORRECTION_COMPLETED,
    TRAINING_REQUIRED, EQUIPMENT_FAILURE, POLICY_VIOLATION,
    REGULATORY_CHANGE, AUDIT_FINDING
}

@Serializable
enum class SecurityEventType {
    LOGIN_ATTEMPT, LOGOUT, PASSWORD_CHANGE, CREDENTIAL_ACCESS,
    DATA_EXPORT, UNAUTHORIZED_ACCESS, ENCRYPTION_EVENT,
    KEY_ROTATION, SYSTEM_CONFIGURATION
}

@Serializable
enum class DataAccessType {
    READ, WRITE, UPDATE, DELETE, EXPORT, SHARE, BACKUP
}

@Serializable
enum class DataType {
    PHOTO, ANALYSIS, USER_DATA, AUDIT_LOG, CREDENTIAL,
    COMPLIANCE_RECORD, SYSTEM_CONFIG, METADATA
}

@Serializable
enum class EventSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
enum class AuditReportType {
    COMPREHENSIVE, SAFETY_ONLY, COMPLIANCE_ONLY, SECURITY_ONLY,
    DATA_PROTECTION, CUSTOM
}

@Serializable
enum class ExportFormat {
    JSON, CSV, XML, PDF
}

@Serializable
enum class ComplianceIssueType {
    MISSING_DOCUMENTATION, OVERDUE_CORRECTION, RECURRING_VIOLATION,
    INSUFFICIENT_TRAINING, EQUIPMENT_NON_COMPLIANCE,
    PROCESS_DEVIATION, DATA_PROTECTION_VIOLATION
}

@Serializable
enum class IntegrityIssueType {
    CHECKSUM_MISMATCH, MISSING_LOG, TIMESTAMP_ANOMALY,
    UNAUTHORIZED_MODIFICATION, ENCRYPTION_FAILURE
}

/**
 * Utility function to generate unique IDs for audit records
 */
private fun generateId(): String {
    val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    val random = kotlin.random.Random.nextInt(1000, 9999)
    return "audit_${timestamp}_$random"
}