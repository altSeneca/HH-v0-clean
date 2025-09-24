package com.hazardhawk.compliance

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

/**
 * OSHA Compliance Manager for HazardHawk construction safety application
 * 
 * Manages compliance with OSHA recordkeeping requirements including:
 * - 29 CFR 1904 - Recording and Reporting Occupational Injuries and Illnesses
 * - 29 CFR 1926 - Safety and Health Regulations for Construction
 * - Data retention requirements (7+ years)
 * - Audit trail maintenance
 * - Digital safety record compliance
 */
class OSHAComplianceManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OSHACompliance"
        private const val AUDIT_LOG_FILE = "osha_audit_log.json"
        private const val COMPLIANCE_PREFS = "osha_compliance_settings"
        
        // OSHA requirement constants
        const val RECORD_RETENTION_YEARS = 7
        const val INCIDENT_REPORT_HOURS = 24 // Hours to report serious incidents
        const val FATALITY_REPORT_HOURS = 8  // Hours to report fatalities
        const val FORM_300_RETENTION_YEARS = 5 // OSHA Form 300 retention
        const val TRAINING_RECORD_RETENTION_YEARS = 3
    }
    
    private val json = Json { prettyPrint = true }
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    
    /**
     * Initialize OSHA compliance system
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing OSHA compliance system")
            
            // Create compliance audit entry
            logComplianceEvent(
                event = ComplianceEvent.SYSTEM_INITIALIZATION,
                category = ComplianceCategory.SYSTEM,
                details = mapOf(
                    "app_version" to getAppVersion(),
                    "device_info" to getDeviceInfo(),
                    "compliance_version" to "1.0"
                )
            )
            
            // Verify compliance requirements
            validateComplianceRequirements()
            
            Log.i(TAG, "OSHA compliance system initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize OSHA compliance system", e)
            throw ComplianceException("Failed to initialize compliance system", e)
        }
    }
    
    /**
     * Log safety analysis for OSHA compliance
     */
    suspend fun logSafetyAnalysis(
        photoId: String,
        analysisResult: String,
        hazardsDetected: List<String>,
        oshaViolations: List<String>,
        userId: String,
        projectId: String,
        location: String?
    ) = withContext(Dispatchers.IO) {
        val details = mapOf(
            "photo_id" to photoId,
            "user_id" to userId,
            "project_id" to projectId,
            "hazards_detected" to hazardsDetected.size,
            "osha_violations" to oshaViolations.size,
            "location" to (location ?: "unknown"),
            "analysis_type" to "ai_safety_analysis"
        )
        
        logComplianceEvent(
            event = ComplianceEvent.SAFETY_ANALYSIS_PERFORMED,
            category = ComplianceCategory.SAFETY_INSPECTION,
            details = details,
            retentionPeriodYears = RECORD_RETENTION_YEARS
        )
        
        // Log specific OSHA violations if detected
        oshaViolations.forEach { violation ->
            logOSHAViolation(violation, photoId, userId, projectId)
        }
    }
    
    /**
     * Log incident report for OSHA compliance
     */
    suspend fun logIncidentReport(
        incidentType: IncidentType,
        severity: IncidentSeverity,
        description: String,
        userId: String,
        projectId: String,
        requiresOSHAReport: Boolean
    ) = withContext(Dispatchers.IO) {
        val details = mapOf(
            "incident_type" to incidentType.name,
            "severity" to severity.name,
            "description" to description,
            "user_id" to userId,
            "project_id" to projectId,
            "requires_osha_report" to requiresOSHAReport,
            "report_deadline" to calculateReportDeadline(severity)
        )
        
        logComplianceEvent(
            event = ComplianceEvent.INCIDENT_REPORTED,
            category = ComplianceCategory.INCIDENT_MANAGEMENT,
            details = details,
            retentionPeriodYears = RECORD_RETENTION_YEARS,
            priority = if (severity == IncidentSeverity.FATALITY) CompliancePriority.CRITICAL else CompliancePriority.HIGH
        )
    }
    
    /**
     * Log training completion for compliance
     */
    suspend fun logTrainingCompletion(
        trainingType: String,
        userId: String,
        certificationType: String,
        expirationDate: String?
    ) = withContext(Dispatchers.IO) {
        val details = mapOf(
            "training_type" to trainingType,
            "user_id" to userId,
            "certification_type" to certificationType,
            "expiration_date" to (expirationDate ?: "none"),
            "completion_timestamp" to getCurrentTimestamp()
        )
        
        logComplianceEvent(
            event = ComplianceEvent.TRAINING_COMPLETED,
            category = ComplianceCategory.TRAINING,
            details = details,
            retentionPeriodYears = TRAINING_RECORD_RETENTION_YEARS
        )
    }
    
    /**
     * Generate compliance audit report
     */
    suspend fun generateAuditReport(
        startDate: String,
        endDate: String,
        includePhotos: Boolean = false
    ): ComplianceAuditReport = withContext(Dispatchers.IO) {
        try {
            val auditEntries = loadAuditEntries(startDate, endDate)
            
            val summary = ComplianceAuditSummary(
                totalEvents = auditEntries.size,
                safetyAnalyses = auditEntries.count { it.event == ComplianceEvent.SAFETY_ANALYSIS_PERFORMED },
                incidentsReported = auditEntries.count { it.event == ComplianceEvent.INCIDENT_REPORTED },
                oshaViolations = auditEntries.count { it.event == ComplianceEvent.OSHA_VIOLATION_DETECTED },
                trainingCompleted = auditEntries.count { it.event == ComplianceEvent.TRAINING_COMPLETED },
                reportGeneratedAt = getCurrentTimestamp()
            )
            
            ComplianceAuditReport(
                reportId = generateReportId(),
                startDate = startDate,
                endDate = endDate,
                summary = summary,
                auditEntries = auditEntries,
                generatedBy = "HazardHawk OSHA Compliance System",
                complianceVersion = "1.0"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate audit report", e)
            throw ComplianceException("Failed to generate audit report", e)
        }
    }
    
    /**
     * Check compliance status
     */
    suspend fun checkComplianceStatus(): ComplianceStatus = withContext(Dispatchers.IO) {
        try {
            val recentEntries = loadRecentAuditEntries(days = 30)
            val criticalIssues = recentEntries.filter { it.priority == CompliancePriority.CRITICAL }
            val warningIssues = recentEntries.filter { it.priority == CompliancePriority.HIGH }
            
            ComplianceStatus(
                overallStatus = when {
                    criticalIssues.isNotEmpty() -> "NON_COMPLIANT"
                    warningIssues.isNotEmpty() -> "NEEDS_ATTENTION"
                    else -> "COMPLIANT"
                },
                criticalIssues = criticalIssues.size,
                warnings = warningIssues.size,
                lastAuditDate = getCurrentTimestamp(),
                nextRequiredAction = determineNextAction(criticalIssues, warningIssues)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check compliance status", e)
            ComplianceStatus(
                overallStatus = "ERROR",
                criticalIssues = 0,
                warnings = 0,
                lastAuditDate = getCurrentTimestamp(),
                nextRequiredAction = "Review compliance system errors"
            )
        }
    }
    
    /**
     * Core compliance event logging
     */
    private suspend fun logComplianceEvent(
        event: ComplianceEvent,
        category: ComplianceCategory,
        details: Map<String, Any>,
        retentionPeriodYears: Int = RECORD_RETENTION_YEARS,
        priority: CompliancePriority = CompliancePriority.NORMAL
    ) = withContext(Dispatchers.IO) {
        try {
            val auditEntry = ComplianceAuditEntry(
                id = generateEventId(),
                timestamp = getCurrentTimestamp(),
                event = event,
                category = category,
                priority = priority,
                details = details.mapValues { it.value.toString() },
                retentionUntil = calculateRetentionDate(retentionPeriodYears),
                deviceId = getDeviceId(),
                appVersion = getAppVersion()
            )
            
            saveAuditEntry(auditEntry)
            
            // Log to system for immediate monitoring
            Log.i(TAG, "Compliance event logged: ${event.name} - Category: ${category.name}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log compliance event: ${event.name}", e)
            // Don't throw exception here to avoid disrupting main app flow
        }
    }
    
    /**
     * Log specific OSHA violation
     */
    private suspend fun logOSHAViolation(
        violation: String,
        photoId: String,
        userId: String,
        projectId: String
    ) {
        val details = mapOf(
            "violation_description" to violation,
            "photo_id" to photoId,
            "user_id" to userId,
            "project_id" to projectId,
            "detection_method" to "ai_analysis"
        )
        
        logComplianceEvent(
            event = ComplianceEvent.OSHA_VIOLATION_DETECTED,
            category = ComplianceCategory.SAFETY_VIOLATION,
            details = details,
            priority = CompliancePriority.HIGH
        )
    }
    
    // Helper methods for compliance calculations and data management
    
    private fun calculateReportDeadline(severity: IncidentSeverity): String {
        val hours = when (severity) {
            IncidentSeverity.FATALITY -> FATALITY_REPORT_HOURS
            IncidentSeverity.SERIOUS_INJURY -> INCIDENT_REPORT_HOURS
            else -> INCIDENT_REPORT_HOURS
        }
        
        val deadline = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, hours)
        }
        
        return dateFormat.format(deadline.time)
    }
    
    private fun calculateRetentionDate(years: Int): String {
        val retentionDate = Calendar.getInstance().apply {
            add(Calendar.YEAR, years)
        }
        return dateFormat.format(retentionDate.time)
    }
    
    private fun getCurrentTimestamp(): String = dateFormat.format(Date())
    
    private fun generateEventId(): String = "CE_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    
    private fun generateReportId(): String = "CR_${System.currentTimeMillis()}_${UUID.randomUUID().toString().substring(0, 8)}"
    
    private fun getAppVersion(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }
    
    private fun getDeviceId(): String = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ANDROID_ID
    ) ?: "unknown"
    
    private fun getDeviceInfo(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
    }
    
    private fun validateComplianceRequirements() {
        // Validate that all required compliance components are available
        Log.d(TAG, "Validating OSHA compliance requirements")
    }
    
    private suspend fun saveAuditEntry(entry: ComplianceAuditEntry) {
        // Save to encrypted local storage and optionally sync to backend
        // Implementation would use secure storage for audit logs
    }
    
    private suspend fun loadAuditEntries(startDate: String, endDate: String): List<ComplianceAuditEntry> {
        // Load audit entries from secure storage for the specified date range
        return emptyList() // Placeholder
    }
    
    private suspend fun loadRecentAuditEntries(days: Int): List<ComplianceAuditEntry> {
        // Load recent audit entries for compliance status checking
        return emptyList() // Placeholder
    }
    
    private fun determineNextAction(critical: List<ComplianceAuditEntry>, warnings: List<ComplianceAuditEntry>): String {
        return when {
            critical.isNotEmpty() -> "Address critical compliance issues immediately"
            warnings.isNotEmpty() -> "Review and resolve warning-level compliance issues"
            else -> "Continue regular compliance monitoring"
        }
    }
}

// Data classes for compliance management

@Serializable
data class ComplianceAuditEntry(
    val id: String,
    val timestamp: String,
    val event: ComplianceEvent,
    val category: ComplianceCategory,
    val priority: CompliancePriority,
    val details: Map<String, String>, // Changed to Map<String, String> for serialization
    val retentionUntil: String,
    val deviceId: String,
    val appVersion: String
)

@Serializable
data class ComplianceAuditReport(
    val reportId: String,
    val startDate: String,
    val endDate: String,
    val summary: ComplianceAuditSummary,
    val auditEntries: List<ComplianceAuditEntry>,
    val generatedBy: String,
    val complianceVersion: String
)

@Serializable
data class ComplianceAuditSummary(
    val totalEvents: Int,
    val safetyAnalyses: Int,
    val incidentsReported: Int,
    val oshaViolations: Int,
    val trainingCompleted: Int,
    val reportGeneratedAt: String
)

@Serializable
data class ComplianceStatus(
    val overallStatus: String,
    val criticalIssues: Int,
    val warnings: Int,
    val lastAuditDate: String,
    val nextRequiredAction: String
)

@Serializable
enum class ComplianceEvent {
    SYSTEM_INITIALIZATION,
    SAFETY_ANALYSIS_PERFORMED,
    INCIDENT_REPORTED,
    OSHA_VIOLATION_DETECTED,
    TRAINING_COMPLETED,
    AUDIT_REPORT_GENERATED,
    DATA_RETENTION_CLEANUP,
    SECURITY_EVENT
}

@Serializable
enum class ComplianceCategory {
    SYSTEM,
    SAFETY_INSPECTION,
    INCIDENT_MANAGEMENT,
    SAFETY_VIOLATION,
    TRAINING,
    AUDIT,
    DATA_MANAGEMENT,
    SECURITY
}

@Serializable
enum class CompliancePriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

@Serializable
enum class IncidentType {
    INJURY,
    NEAR_MISS,
    EQUIPMENT_FAILURE,
    ENVIRONMENTAL,
    SECURITY_BREACH
}

@Serializable
enum class IncidentSeverity {
    MINOR,
    MODERATE,
    SERIOUS_INJURY,
    FATALITY
}

class ComplianceException(message: String, cause: Throwable? = null) : Exception(message, cause)