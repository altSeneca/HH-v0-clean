package com.hazardhawk.security.audit

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import com.hazardhawk.domain.entities.HazardType
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.security.EventSeverity

/**
 * Detailed compliance event models for OSHA and regulatory compliance tracking.
 * Provides structured data for audit reporting and regulatory submissions.
 */

/**
 * OSHA-specific compliance event with regulatory requirements
 */
@Serializable
data class OSHAComplianceEvent(
    val id: String = generateComplianceId(),
    val eventType: OSHAEventType,
    val oshaStandard: OSHAStandard,
    val violationType: OSHAViolationType,
    val severity: EventSeverity,
    val description: String,
    val location: String,
    val workType: WorkType?,
    val hazardTypes: List<HazardType>,
    val employeesAffected: Int,
    val injuriesReported: Int = 0,
    val nearMissCount: Int = 0,
    val photoEvidence: List<String> = emptyList(), // Photo IDs
    val witnessStatements: List<WitnessStatement> = emptyList(),
    val correctiveActions: List<CorrectiveAction> = emptyList(),
    val inspectorId: String?,
    val contractorId: String?,
    val projectId: String?,
    val reportedAt: Instant,
    val occurredAt: Instant,
    val discoveredBy: String,
    val reportedBy: String,
    val reviewedBy: String? = null,
    val reviewedAt: Instant? = null,
    val status: ComplianceEventStatus,
    val priority: CompliancePriority,
    val estimatedCost: Double? = null,
    val actualCost: Double? = null,
    val regulatoryNotification: RegulatoryNotification? = null,
    val followUpRequired: Boolean = true,
    val followUpDate: Instant? = null,
    val closedAt: Instant? = null,
    val closedBy: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Check if this event requires immediate regulatory notification
     */
    fun requiresImmediateNotification(): Boolean {
        return severity == EventSeverity.CRITICAL || 
               injuriesReported > 0 || 
               violationType == OSHAViolationType.WILLFUL
    }
    
    /**
     * Calculate compliance score impact (0.0 to -1.0, negative values reduce score)
     */
    fun getComplianceScoreImpact(): Double {
        return when (severity) {
            EventSeverity.CRITICAL -> -0.3
            EventSeverity.HIGH -> -0.2
            EventSeverity.MEDIUM -> -0.1
            EventSeverity.LOW -> -0.05
        }
    }
    
    /**
     * Check if the event is overdue for resolution
     */
    fun isOverdue(): Boolean {
        if (status == ComplianceEventStatus.CLOSED) return false
        
        val deadline = when (severity) {
            EventSeverity.CRITICAL -> occurredAt.plus(1, kotlinx.datetime.DateTimeUnit.DAY)
            EventSeverity.HIGH -> occurredAt.plus(7, kotlinx.datetime.DateTimeUnit.DAY)
            EventSeverity.MEDIUM -> occurredAt.plus(30, kotlinx.datetime.DateTimeUnit.DAY)
            EventSeverity.LOW -> occurredAt.plus(90, kotlinx.datetime.DateTimeUnit.DAY)
        }
        
        return kotlinx.datetime.Clock.System.now() > deadline
    }
}

/**
 * Witness statement for compliance events
 */
@Serializable
data class WitnessStatement(
    val witnessId: String,
    val witnessName: String,
    val witnessRole: String,
    val contactInformation: String,
    val statement: String,
    val providedAt: Instant,
    val verifiedBy: String? = null,
    val verifiedAt: Instant? = null
)

/**
 * Corrective action for compliance events
 */
@Serializable
data class CorrectiveAction(
    val id: String = generateActionId(),
    val description: String,
    val actionType: CorrectiveActionType,
    val assignedTo: String,
    val assignedBy: String,
    val dueDate: Instant,
    val priority: ActionPriority,
    val status: ActionStatus,
    val estimatedCost: Double? = null,
    val actualCost: Double? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val verifiedBy: String? = null,
    val verifiedAt: Instant? = null,
    val evidencePhotos: List<String> = emptyList(), // Photo IDs
    val notes: String? = null,
    val followUpActions: List<String> = emptyList() // Follow-up action IDs
) {
    /**
     * Check if this action is overdue
     */
    fun isOverdue(): Boolean {
        return status != ActionStatus.COMPLETED && 
               kotlinx.datetime.Clock.System.now() > dueDate
    }
    
    /**
     * Get the number of days overdue (negative if not overdue)
     */
    fun getDaysOverdue(): Long {
        val now = kotlinx.datetime.Clock.System.now()
        return if (now > dueDate) {
            dueDate.until(now, kotlinx.datetime.DateTimeUnit.DAY)
        } else {
            -1L
        }
    }
}

/**
 * Regulatory notification details
 */
@Serializable
data class RegulatoryNotification(
    val notificationType: NotificationType,
    val regulatoryBody: RegulatoryBody,
    val notificationRequired: Boolean,
    val notificationDeadline: Instant?,
    val notificationSent: Boolean = false,
    val notificationSentAt: Instant? = null,
    val notificationMethod: String? = null,
    val referenceNumber: String? = null,
    val acknowledgmentReceived: Boolean = false,
    val acknowledgmentReceivedAt: Instant? = null,
    val followUpRequired: Boolean = false
)

/**
 * Training requirement event
 */
@Serializable
data class TrainingComplianceEvent(
    val id: String = generateTrainingId(),
    val trainingType: TrainingType,
    val requiredFor: List<String>, // Employee IDs
    val completedBy: List<String> = emptyList(), // Employee IDs who completed
    val trainingProvider: String,
    val certificationRequired: Boolean,
    val certificationValidityPeriod: kotlinx.datetime.DatePeriod? = null,
    val dueDate: Instant,
    val completionRate: Double = 0.0,
    val status: TrainingStatus,
    val cost: Double? = null,
    val trainingMaterials: List<String> = emptyList(), // Document IDs
    val assessmentRequired: Boolean = false,
    val assessmentPassRate: Double = 0.8,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Equipment compliance event
 */
@Serializable
data class EquipmentComplianceEvent(
    val id: String = generateEquipmentId(),
    val equipmentId: String,
    val equipmentType: String,
    val manufacturer: String,
    val model: String,
    val serialNumber: String,
    val inspectionType: InspectionType,
    val inspectionDue: Instant,
    val lastInspection: Instant? = null,
    val inspectionResult: InspectionResult? = null,
    val deficienciesFound: List<EquipmentDeficiency> = emptyList(),
    val maintenanceRequired: Boolean = false,
    val outOfService: Boolean = false,
    val outOfServiceReason: String? = null,
    val certificationRequired: Boolean = false,
    val certificationExpiry: Instant? = null,
    val operatorCertificationRequired: Boolean = false,
    val status: EquipmentStatus,
    val location: String,
    val responsiblePerson: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Equipment deficiency details
 */
@Serializable
data class EquipmentDeficiency(
    val id: String = generateDeficiencyId(),
    val description: String,
    val severity: DeficiencySeverity,
    val component: String,
    val photoEvidence: List<String> = emptyList(), // Photo IDs
    val correctiveAction: String,
    val dueDate: Instant,
    val assignedTo: String,
    val status: DeficiencyStatus,
    val cost: Double? = null,
    val resolvedAt: Instant? = null,
    val resolvedBy: String? = null,
    val verifiedBy: String? = null
)

// Enums for OSHA compliance
@Serializable
enum class OSHAEventType {
    VIOLATION_IDENTIFIED,
    INCIDENT_REPORTED,
    INSPECTION_FINDING,
    NEAR_MISS_REPORTED,
    TRAINING_DEFICIENCY,
    EQUIPMENT_FAILURE,
    PPE_VIOLATION,
    HOUSEKEEPING_ISSUE,
    ENVIRONMENTAL_HAZARD,
    REGULATORY_CHANGE
}

@Serializable
enum class OSHAStandard {
    OSHA_1926_CONSTRUCTION,
    OSHA_1910_GENERAL_INDUSTRY,
    OSHA_1915_SHIPYARD,
    OSHA_1917_MARINE_TERMINALS,
    OSHA_1918_LONGSHORING,
    OSHA_1928_AGRICULTURE,
    STATE_OSHA_PLAN,
    OTHER
}

@Serializable
enum class OSHAViolationType {
    DE_MINIMIS,      // Minimal safety concern
    OTHER_THAN_SERIOUS, // Unlikely to cause death or serious harm
    SERIOUS,         // Substantial probability of death or serious harm
    WILLFUL,         // Intentional disregard or indifference
    REPEAT,          // Substantially similar violation previously cited
    FAILURE_TO_ABATE // Did not correct previously cited violation
}

@Serializable
enum class ComplianceEventStatus {
    REPORTED,
    UNDER_INVESTIGATION,
    CORRECTIVE_ACTION_REQUIRED,
    CORRECTIVE_ACTION_IN_PROGRESS,
    PENDING_VERIFICATION,
    CLOSED,
    APPEALED,
    REOPENED
}

@Serializable
enum class CompliancePriority {
    LOW, MEDIUM, HIGH, CRITICAL, EMERGENCY
}

@Serializable
enum class CorrectiveActionType {
    IMMEDIATE_ACTION,
    ENGINEERING_CONTROL,
    ADMINISTRATIVE_CONTROL,
    PPE_REQUIREMENT,
    TRAINING_REQUIRED,
    POLICY_CHANGE,
    EQUIPMENT_REPLACEMENT,
    PROCEDURE_UPDATE,
    CONTRACTOR_ACTION
}

@Serializable
enum class ActionPriority {
    LOW, MEDIUM, HIGH, URGENT
}

@Serializable
enum class ActionStatus {
    ASSIGNED, IN_PROGRESS, PENDING_REVIEW, COMPLETED, OVERDUE, CANCELLED
}

@Serializable
enum class NotificationType {
    INCIDENT_REPORT,
    INJURY_ILLNESS_REPORT,
    FATALITY_REPORT,
    INSPECTION_NOTICE,
    VIOLATION_NOTICE,
    COMPLIANCE_UPDATE
}

@Serializable
enum class RegulatoryBody {
    FEDERAL_OSHA,
    STATE_OSHA,
    EPA,
    DOT,
    MSHA,
    LOCAL_AUTHORITY,
    OTHER
}

@Serializable
enum class TrainingType {
    GENERAL_SAFETY_ORIENTATION,
    FALL_PROTECTION,
    SCAFFOLDING_SAFETY,
    ELECTRICAL_SAFETY,
    CONFINED_SPACE,
    HAZCOM_TRAINING,
    CRANE_OPERATOR,
    FORKLIFT_OPERATOR,
    FIRST_AID_CPR,
    TOOLBOX_TALK
}

@Serializable
enum class TrainingStatus {
    SCHEDULED, IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED
}

@Serializable
enum class InspectionType {
    DAILY_INSPECTION,
    WEEKLY_INSPECTION,
    MONTHLY_INSPECTION,
    ANNUAL_INSPECTION,
    PERIODIC_INSPECTION,
    POST_INCIDENT_INSPECTION,
    REGULATORY_INSPECTION
}

@Serializable
enum class InspectionResult {
    PASSED, PASSED_WITH_CONDITIONS, FAILED, INCOMPLETE
}

@Serializable
enum class EquipmentStatus {
    IN_SERVICE, OUT_OF_SERVICE, MAINTENANCE, INSPECTION_DUE, DECOMMISSIONED
}

@Serializable
enum class DeficiencySeverity {
    MINOR, MODERATE, MAJOR, CRITICAL, SAFETY_HAZARD
}

@Serializable
enum class DeficiencyStatus {
    IDENTIFIED, ASSIGNED, IN_PROGRESS, RESOLVED, VERIFIED, DEFERRED
}

// ID generators
private fun generateComplianceId(): String {
    val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    return "comp_${timestamp}_${kotlin.random.Random.nextInt(1000, 9999)}"
}

private fun generateActionId(): String {
    val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    return "action_${timestamp}_${kotlin.random.Random.nextInt(1000, 9999)}"
}

private fun generateTrainingId(): String {
    val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    return "training_${timestamp}_${kotlin.random.Random.nextInt(1000, 9999)}"
}

private fun generateEquipmentId(): String {
    val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    return "equipment_${timestamp}_${kotlin.random.Random.nextInt(1000, 9999)}"
}

private fun generateDeficiencyId(): String {
    val timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    return "deficiency_${timestamp}_${kotlin.random.Random.nextInt(1000, 9999)}"
}