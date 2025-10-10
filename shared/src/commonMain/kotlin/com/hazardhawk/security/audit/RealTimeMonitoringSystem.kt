package com.hazardhawk.security.audit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import com.hazardhawk.security.AuditLogger
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.core.models.Severity

/**
 * Real-time monitoring system for audit trail and compliance events
 * Provides live monitoring of safety events and OSHA compliance issues
 */
class RealTimeMonitoringSystem(
    private val auditLogger: AuditLogger
) {
    
    companion object {
        private const val MAX_ALERT_HISTORY = 1000
        private const val CRITICAL_ALERT_RETENTION_HOURS = 72
        private const val COMPLIANCE_CHECK_INTERVAL_MINUTES = 15
    }

    // Real-time event streams
    private val _safetyAlerts = MutableSharedFlow<SafetyAlert>(replay = 10)
    val safetyAlerts: Flow<SafetyAlert> = _safetyAlerts.asSharedFlow()
    
    private val _complianceEvents = MutableSharedFlow<ComplianceEvent>(replay = 10)
    val complianceEvents: Flow<ComplianceEvent> = _complianceEvents.asSharedFlow()
    
    private val _systemEvents = MutableSharedFlow<SystemEvent>(replay = 5)
    val systemEvents: Flow<SystemEvent> = _systemEvents.asSharedFlow()
    
    // Alert storage for historical tracking
    private val alertHistory = mutableListOf<SafetyAlert>()
    private val complianceHistory = mutableListOf<ComplianceEvent>()
    
    /**
     * Process hazard detection and trigger alerts
     */
    suspend fun processHazardDetection(
        photoId: String,
        workType: WorkType,
        hazards: List<String>,
        severity: Severity,
        location: String? = null
    ) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        // Create safety alert
        val alert = SafetyAlert(
            id = "alert-$timestamp",
            photoId = photoId,
            alertType = AlertType.HAZARD_DETECTED,
            severity = severity,
            workType = workType,
            description = "Safety hazards detected: ${hazards.joinToString(", ")}",
            location = location,
            timestamp = timestamp,
            requiresImmediateAction = severity in listOf(Severity.HIGH, Severity.CRITICAL),
            oshaReportable = severity == Severity.CRITICAL,
            acknowledgedBy = null,
            acknowledgedAt = null
        )
        
        // Store in history and emit
        addToAlertHistory(alert)
        _safetyAlerts.emit(alert)
        
        // Log audit event
        auditLogger.logEvent(
            eventType = "HAZARD_DETECTION",
            details = mapOf(
                "photoId" to photoId,
                "severity" to severity.name,
                "hazardCount" to hazards.size.toString(),
                "workType" to workType.name
            ),
            userId = null,
            metadata = mapOf("location" to (location ?: "unknown"))
        )
        
        // Trigger additional actions for critical alerts
        if (severity == Severity.CRITICAL) {
            handleCriticalAlert(alert)
        }
    }

    /**
     * Process OSHA compliance events
     */
    suspend fun processComplianceEvent(
        eventType: ComplianceEventType,
        photoId: String?,
        violationStandard: String?,
        severity: Severity,
        description: String,
        correctionRequired: Boolean = true
    ) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        val complianceEvent = ComplianceEvent(
            id = "compliance-$timestamp",
            photoId = photoId,
            eventType = eventType,
            oshaStandard = violationStandard,
            severity = severity,
            description = description,
            timestamp = timestamp,
            correctionRequired = correctionRequired,
            correctionDeadline = calculateCorrectionDeadline(severity),
            documentationRequired = true,
            retentionPeriodYears = 5,
            reportedToOSHA = false,
            correctionCompleted = false
        )
        
        // Store and emit
        addToComplianceHistory(complianceEvent)
        _complianceEvents.emit(complianceEvent)
        
        // Log compliance audit event
        auditLogger.logEvent(
            eventType = "COMPLIANCE_EVENT",
            details = mapOf(
                "eventType" to eventType.name,
                "severity" to severity.name,
                "standard" to (violationStandard ?: "unknown"),
                "correctionRequired" to correctionRequired.toString()
            ),
            userId = null,
            metadata = mapOf("photoId" to (photoId ?: "none"))
        )
    }

    /**
     * Monitor system performance and health
     */
    suspend fun monitorSystemHealth(
        component: String,
        status: SystemHealthStatus,
        metrics: Map<String, String> = emptyMap()
    ) {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        
        val systemEvent = SystemEvent(
            id = "system-$timestamp",
            component = component,
            status = status,
            timestamp = timestamp,
            metrics = metrics,
            alertLevel = when (status) {
                SystemHealthStatus.HEALTHY -> SystemAlertLevel.INFO
                SystemHealthStatus.DEGRADED -> SystemAlertLevel.WARNING
                SystemHealthStatus.FAILING -> SystemAlertLevel.ERROR
                SystemHealthStatus.DOWN -> SystemAlertLevel.CRITICAL
            }
        )
        
        _systemEvents.emit(systemEvent)
        
        // Log system health event
        auditLogger.logEvent(
            eventType = "SYSTEM_HEALTH",
            details = mapOf(
                "component" to component,
                "status" to status.name
            ) + metrics,
            userId = null,
            metadata = emptyMap()
        )
    }

    /**
     * Get active alerts that require attention
     */
    fun getActiveAlerts(): List<SafetyAlert> {
        return alertHistory.filter { alert ->
            !alert.isAcknowledged() && 
            alert.requiresImmediateAction &&
            isWithinRetentionPeriod(alert.timestamp)
        }.sortedByDescending { it.timestamp }
    }

    /**
     * Get unresolved compliance events
     */
    fun getUnresolvedComplianceEvents(): List<ComplianceEvent> {
        return complianceHistory.filter { event ->
            !event.correctionCompleted &&
            event.correctionRequired
        }.sortedByDescending { it.timestamp }
    }

    /**
     * Generate compliance dashboard data
     */
    suspend fun generateDashboardData(): MonitoringDashboardData {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val last24Hours = currentTime - (24 * 60 * 60 * 1000)
        val last7Days = currentTime - (7 * 24 * 60 * 60 * 1000)
        
        val recentAlerts = alertHistory.filter { it.timestamp > last24Hours }
        val recentCompliance = complianceHistory.filter { it.timestamp > last24Hours }
        
        return MonitoringDashboardData(
            totalActiveAlerts = getActiveAlerts().size,
            criticalAlerts = getActiveAlerts().count { it.severity == Severity.CRITICAL },
            unresolvedCompliance = getUnresolvedComplianceEvents().size,
            alertsLast24Hours = recentAlerts.size,
            complianceEventsLast24Hours = recentCompliance.size,
            alertsByTypeLastWeek = getAlertsByType(last7Days),
            severityDistribution = getSeverityDistribution(last7Days),
            complianceRate = calculateComplianceRate()
        )
    }

    /**
     * Acknowledge an alert
     */
    suspend fun acknowledgeAlert(alertId: String, userId: String, notes: String? = null): Boolean {
        val alert = alertHistory.find { it.id == alertId }
        return if (alert != null) {
            val updatedAlert = alert.copy(
                acknowledgedBy = userId,
                acknowledgedAt = Clock.System.now().toEpochMilliseconds()
            )
            
            // Update in history
            val index = alertHistory.indexOfFirst { it.id == alertId }
            if (index >= 0) {
                alertHistory[index] = updatedAlert
            }
            
            // Log acknowledgment
            auditLogger.logEvent(
                eventType = "ALERT_ACKNOWLEDGED",
                details = mapOf(
                    "alertId" to alertId,
                    "acknowledgedBy" to userId,
                    "notes" to (notes ?: "")
                ),
                userId = userId,
                metadata = emptyMap()
            )
            
            true
        } else {
            false
        }
    }

    private suspend fun handleCriticalAlert(alert: SafetyAlert) {
        // Log critical alert
        auditLogger.logEvent(
            eventType = "CRITICAL_ALERT",
            details = mapOf(
                "alertId" to alert.id,
                "photoId" to alert.photoId,
                "workType" to alert.workType.name,
                "description" to alert.description
            ),
            userId = null,
            metadata = mapOf(
                "requiresOSHAReport" to alert.oshaReportable.toString(),
                "location" to (alert.location ?: "unknown")
            )
        )
        
        // Emit system event for critical alert
        _systemEvents.emit(SystemEvent(
            id = "critical-system-${Clock.System.now().toEpochMilliseconds()}",
            component = "SAFETY_MONITORING",
            status = SystemHealthStatus.DEGRADED,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            metrics = mapOf("criticalAlertId" to alert.id),
            alertLevel = SystemAlertLevel.CRITICAL
        ))
    }

    private fun addToAlertHistory(alert: SafetyAlert) {
        alertHistory.add(alert)
        
        // Maintain history size
        if (alertHistory.size > MAX_ALERT_HISTORY) {
            alertHistory.removeFirst()
        }
    }

    private fun addToComplianceHistory(event: ComplianceEvent) {
        complianceHistory.add(event)
        
        // Maintain history size
        if (complianceHistory.size > MAX_ALERT_HISTORY) {
            complianceHistory.removeFirst()
        }
    }

    private fun calculateCorrectionDeadline(severity: Severity): String {
        return when (severity) {
            Severity.CRITICAL -> "Immediate"
            Severity.HIGH -> "24 hours"
            Severity.MEDIUM -> "7 days"
            Severity.LOW -> "30 days"
        }
    }

    private fun isWithinRetentionPeriod(timestamp: Long): Boolean {
        val retentionPeriod = CRITICAL_ALERT_RETENTION_HOURS * 60 * 60 * 1000
        return Clock.System.now().toEpochMilliseconds() - timestamp < retentionPeriod
    }

    private fun getAlertsByType(sinceTimestamp: Long): Map<String, Int> {
        return alertHistory
            .filter { it.timestamp > sinceTimestamp }
            .groupBy { it.alertType.name }
            .mapValues { it.value.size }
    }

    private fun getSeverityDistribution(sinceTimestamp: Long): Map<String, Int> {
        return alertHistory
            .filter { it.timestamp > sinceTimestamp }
            .groupBy { it.severity.name }
            .mapValues { it.value.size }
    }

    private fun calculateComplianceRate(): Double {
        val totalEvents = complianceHistory.size
        if (totalEvents == 0) return 100.0
        
        val resolvedEvents = complianceHistory.count { it.correctionCompleted }
        return (resolvedEvents.toDouble() / totalEvents) * 100.0
    }
}

/**
 * Safety alert data class
 */
@Serializable
data class SafetyAlert(
    val id: String,
    val photoId: String,
    val alertType: AlertType,
    val severity: Severity,
    val workType: WorkType,
    val description: String,
    val location: String?,
    val timestamp: Long,
    val requiresImmediateAction: Boolean,
    val oshaReportable: Boolean,
    val acknowledgedBy: String?,
    val acknowledgedAt: Long?
) {
    fun isAcknowledged(): Boolean = acknowledgedBy != null
}

/**
 * Compliance event data class
 */
@Serializable
data class ComplianceEvent(
    val id: String,
    val photoId: String?,
    val eventType: ComplianceEventType,
    val oshaStandard: String?,
    val severity: Severity,
    val description: String,
    val timestamp: Long,
    val correctionRequired: Boolean,
    val correctionDeadline: String,
    val documentationRequired: Boolean,
    val retentionPeriodYears: Int,
    val reportedToOSHA: Boolean,
    val correctionCompleted: Boolean
)

/**
 * System event for monitoring
 */
@Serializable
data class SystemEvent(
    val id: String,
    val component: String,
    val status: SystemHealthStatus,
    val timestamp: Long,
    val metrics: Map<String, String>,
    val alertLevel: SystemAlertLevel
)

/**
 * Dashboard data for monitoring
 */
@Serializable
data class MonitoringDashboardData(
    val totalActiveAlerts: Int,
    val criticalAlerts: Int,
    val unresolvedCompliance: Int,
    val alertsLast24Hours: Int,
    val complianceEventsLast24Hours: Int,
    val alertsByTypeLastWeek: Map<String, Int>,
    val severityDistribution: Map<String, Int>,
    val complianceRate: Double
)

/**
 * Alert types
 */
@Serializable
enum class AlertType {
    HAZARD_DETECTED,
    COMPLIANCE_VIOLATION,
    PPE_VIOLATION,
    CRITICAL_INCIDENT,
    SYSTEM_FAILURE,
    DATA_INTEGRITY_ISSUE
}

/**
 * Compliance event types
 */
@Serializable
enum class ComplianceEventType {
    OSHA_VIOLATION,
    SAFETY_INCIDENT,
    NEAR_MISS,
    AUDIT_FINDING,
    TRAINING_DEFICIENCY,
    EQUIPMENT_FAILURE,
    PROCEDURE_VIOLATION
}

/**
 * System health status
 */
@Serializable
enum class SystemHealthStatus {
    HEALTHY,
    DEGRADED,
    FAILING,
    DOWN
}

/**
 * System alert levels
 */
@Serializable
enum class SystemAlertLevel {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}