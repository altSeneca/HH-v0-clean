package com.hazardhawk.documents.models

import com.hazardhawk.core.models.Hazard
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.core.models.WorkType
import kotlinx.serialization.Serializable

/**
 * Pre-Task Plan (PTP) document model for construction safety planning.
 * Generated from AI photo analysis to ensure comprehensive hazard mitigation.
 */
@Serializable
data class PTPDocument(
    val id: String,
    val title: String,
    val createdAt: Long,
    val projectInfo: ProjectInfo,
    val jobDescription: JobDescription,
    val hazardAnalysis: HazardAnalysisSection,
    val safetyProcedures: List<SafetyProcedure>,
    val requiredPPE: List<PPERequirement>,
    val emergencyInformation: EmergencyInformation,
    val approvals: List<Approval>,
    val attachedPhotos: List<String>, // Photo IDs
    val oshaReferences: List<OSHAReference>,
    val revisionHistory: List<Revision> = emptyList()
)

/**
 * Project information for PTP context.
 */
@Serializable
data class ProjectInfo(
    val projectName: String,
    val projectNumber: String? = null,
    val location: String,
    val contractor: String,
    val subcontractor: String? = null,
    val workDate: String,
    val estimatedDuration: String,
    val weatherConditions: String? = null
)

/**
 * Detailed job description and scope.
 */
@Serializable
data class JobDescription(
    val workType: WorkType,
    val taskDescription: String,
    val workLocation: String,
    val equipmentRequired: List<String>,
    val materialsRequired: List<String>,
    val numberOfWorkers: Int,
    val skillLevelRequired: SkillLevel,
    val workHours: String
)

/**
 * AI-enhanced hazard analysis section.
 */
@Serializable
data class HazardAnalysisSection(
    val identifiedHazards: List<IdentifiedHazard>,
    val riskAssessment: RiskAssessment,
    val controlMeasures: List<ControlMeasure>,
    val residualRisk: RiskLevel,
    val analysisSource: String // "AI Photo Analysis + Expert Review"
)

/**
 * Individual hazard identification with AI confidence.
 */
@Serializable
data class IdentifiedHazard(
    val hazardId: String,
    val hazardType: String,
    val description: String,
    val location: String,
    val severity: String,
    val probability: String,
    val riskRating: String,
    val oshaReference: String? = null,
    val aiConfidence: Float? = null,
    val photoEvidence: List<String> = emptyList()
)

/**
 * Risk assessment matrix and scoring.
 */
@Serializable
data class RiskAssessment(
    val overallRiskLevel: RiskLevel,
    val highRiskTasks: List<String>,
    val criticalControlPoints: List<String>,
    val stopWorkConditions: List<String>
)

/**
 * Hazard control measures with hierarchy.
 */
@Serializable
data class ControlMeasure(
    val hazardId: String,
    val controlType: ControlType,
    val description: String,
    val responsiblePerson: String,
    val implementationDate: String? = null,
    val verificationMethod: String,
    val priority: Priority
)

/**
 * Safety procedure step-by-step instructions.
 */
@Serializable
data class SafetyProcedure(
    val id: String,
    val title: String,
    val steps: List<ProcedureStep>,
    val applicableHazards: List<String>,
    val requiredTraining: List<String> = emptyList(),
    val inspectionPoints: List<String> = emptyList()
)

/**
 * Individual procedure step.
 */
@Serializable
data class ProcedureStep(
    val stepNumber: Int,
    val instruction: String,
    val safetyNote: String? = null,
    val requiredPPE: List<String> = emptyList(),
    val verificationRequired: Boolean = false
)

/**
 * PPE requirements based on identified hazards.
 */
@Serializable
data class PPERequirement(
    val ppeType: PPEType,
    val specification: String,
    val oshaStandard: String? = null,
    val applicableHazards: List<String>,
    val inspectionRequired: Boolean = false
)

/**
 * Emergency contact and response information.
 */
@Serializable
data class EmergencyInformation(
    val emergencyContacts: List<EmergencyContact>,
    val nearestHospital: HospitalInfo,
    val evacuationProcedure: String,
    val emergencyEquipment: List<String>,
    val incidentReportingProcess: String
)

/**
 * Emergency contact details.
 */
@Serializable
data class EmergencyContact(
    val title: String,
    val name: String,
    val phoneNumber: String,
    val alternateNumber: String? = null
)

/**
 * Hospital information for emergencies.
 */
@Serializable
data class HospitalInfo(
    val name: String,
    val address: String,
    val phoneNumber: String,
    val distance: String
)

/**
 * Approval signatures and dates.
 */
@Serializable
data class Approval(
    val role: String,
    val name: String,
    val signature: String? = null, // Base64 encoded signature
    val date: String,
    val comments: String? = null
)

/**
 * OSHA regulation references.
 */
@Serializable
data class OSHAReference(
    val code: String,
    val title: String,
    val description: String,
    val applicableHazards: List<String>,
    val complianceNotes: String? = null
)

/**
 * Document revision tracking.
 */
@Serializable
data class Revision(
    val version: String,
    val date: String,
    val author: String,
    val changes: String,
    val reason: String
)

// Enums for structured data
@Serializable
enum class SkillLevel {
    ENTRY_LEVEL,
    EXPERIENCED,
    SKILLED_CRAFTSPERSON,
    SUPERVISOR,
    SPECIALIST
}

@Serializable
enum class RiskLevel {
    VERY_LOW,
    LOW,
    MODERATE,
    HIGH,
    VERY_HIGH,
    EXTREME
}

@Serializable
enum class ControlType {
    ELIMINATION,
    SUBSTITUTION,
    ENGINEERING_CONTROLS,
    ADMINISTRATIVE_CONTROLS,
    PPE
}

@Serializable
enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class PPEType {
    HEAD_PROTECTION,
    EYE_PROTECTION,
    HEARING_PROTECTION,
    RESPIRATORY_PROTECTION,
    HAND_PROTECTION,
    FOOT_PROTECTION,
    BODY_PROTECTION,
    FALL_PROTECTION
}

/**
 * PTP generation request with analysis input.
 */
@Serializable
data class PTPGenerationRequest(
    val projectInfo: ProjectInfo,
    val jobDescription: JobDescription,
    val safetyAnalyses: List<SafetyAnalysis>,
    val additionalRequirements: List<String> = emptyList(),
    val template: String? = null
)

/**
 * PTP generation response with metadata.
 */
@Serializable
data class PTPGenerationResponse(
    val document: PTPDocument,
    val generationMetadata: GenerationMetadata,
    val qualityScore: Float,
    val recommendations: List<String>
)

/**
 * Generation metadata for tracking and improvement.
 */
@Serializable
data class GenerationMetadata(
    val aiModel: String,
    val processingTimeMs: Long,
    val confidenceScore: Float,
    val hazardsProcessed: Int,
    val templatesUsed: List<String>,
    val reviewRequired: Boolean
)