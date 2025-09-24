package com.hazardhawk.documents.services

import com.hazardhawk.core.models.Hazard
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.core.models.WorkType

/**
 * AI service interface for generating safety document content.
 * Leverages the same AI models used for photo analysis to create coherent documentation.
 */
interface DocumentAIService {
    
    /**
     * Generate detailed safety procedures for specific hazards.
     */
    suspend fun generateSafetyProcedures(
        hazards: List<Hazard>,
        workType: WorkType,
        context: String = ""
    ): Result<List<SafetyProcedureContent>>
    
    /**
     * Generate toolbox talk content based on recent hazards.
     */
    suspend fun generateToolboxTalkContent(
        topic: String,
        recentHazards: List<Hazard>,
        targetAudience: String = "Construction Workers"
    ): Result<ToolboxTalkContent>
    
    /**
     * Generate incident report narrative from analysis.
     */
    suspend fun generateIncidentReportNarrative(
        analysisResults: List<SafetyAnalysis>,
        incidentType: String,
        context: String
    ): Result<IncidentReportContent>
    
    /**
     * Generate risk assessment content.
     */
    suspend fun generateRiskAssessment(
        hazards: List<Hazard>,
        workType: WorkType,
        riskMatrix: String = "5x5"
    ): Result<RiskAssessmentContent>
    
    /**
     * Generate OSHA-compliant corrective actions.
     */
    suspend fun generateCorrectiveActions(
        hazards: List<Hazard>,
        priorityLevel: String = "HIGH"
    ): Result<List<CorrectiveAction>>
    
    /**
     * Enhance existing content with AI improvements.
     */
    suspend fun enhanceContent(
        originalContent: String,
        contentType: DocumentType,
        improvementGoals: List<String>
    ): Result<EnhancedContent>
}

/**
 * Safety procedure content with AI-generated steps.
 */
data class SafetyProcedureContent(
    val title: String,
    val purpose: String,
    val scope: String,
    val steps: List<ProcedureStep>,
    val warnings: List<String>,
    val references: List<String>
)

/**
 * Individual procedure step with safety considerations.
 */
data class ProcedureStep(
    val stepNumber: Int,
    val action: String,
    val safetyNote: String?,
    val requiredPPE: List<String>,
    val verification: String?
)

/**
 * Toolbox talk content generated from hazard analysis.
 */
data class ToolboxTalkContent(
    val title: String,
    val duration: String, // e.g., "15 minutes"
    val objectives: List<String>,
    val keyPoints: List<String>,
    val discussionQuestions: List<String>,
    val visualAids: List<String>, // References to photos/diagrams
    val takeaways: List<String>,
    val signatures: SignatureSection
)

/**
 * Incident report content with AI analysis.
 */
data class IncidentReportContent(
    val narrative: String,
    val rootCauses: List<String>,
    val contributingFactors: List<String>,
    val immediateActions: List<String>,
    val preventiveActions: List<String>,
    val lessonsLearned: List<String>
)

/**
 * Risk assessment content with scoring.
 */
data class RiskAssessmentContent(
    val hazardDescription: String,
    val riskScenarios: List<RiskScenario>,
    val riskMatrix: RiskMatrix,
    val controlMeasures: List<ControlMeasure>,
    val residualRisk: String
)

/**
 * Risk scenario with probability and impact.
 */
data class RiskScenario(
    val description: String,
    val probability: Int, // 1-5 scale
    val impact: Int, // 1-5 scale
    val riskScore: Int, // probability x impact
    val riskLevel: String // LOW, MODERATE, HIGH, EXTREME
)

/**
 * Risk matrix representation.
 */
data class RiskMatrix(
    val type: String, // "5x5", "3x3", etc.
    val probabilities: List<String>,
    val impacts: List<String>,
    val riskLevels: Map<String, String> // score to level mapping
)

/**
 * Control measure with effectiveness rating.
 */
data class ControlMeasure(
    val description: String,
    val type: String, // Elimination, Substitution, etc.
    val effectiveness: String,
    val implementation: String,
    val responsible: String
)

/**
 * Corrective action with timeline.
 */
data class CorrectiveAction(
    val action: String,
    val priority: String,
    val responsible: String,
    val dueDate: String,
    val verification: String,
    val oshaReference: String?
)

/**
 * Enhanced content with improvement tracking.
 */
data class EnhancedContent(
    val originalContent: String,
    val enhancedContent: String,
    val improvements: List<Improvement>,
    val qualityScore: Float,
    val readabilityScore: Float
)

/**
 * Content improvement tracking.
 */
data class Improvement(
    val type: String, // "Clarity", "Completeness", "Accuracy", etc.
    val description: String,
    val impact: String // "High", "Medium", "Low"
)

/**
 * Signature section for documents.
 */
data class SignatureSection(
    val attendees: List<AttendeeSignature>,
    val instructor: InstructorSignature
)

/**
 * Individual attendee signature.
 */
data class AttendeeSignature(
    val name: String,
    val signature: String?, // Base64 encoded
    val date: String,
    val present: Boolean = true
)

/**
 * Instructor signature.
 */
data class InstructorSignature(
    val name: String,
    val title: String,
    val signature: String?, // Base64 encoded
    val date: String,
    val certifications: List<String> = emptyList()
)

/**
 * Document types for content generation.
 */
enum class DocumentType {
    PRE_TASK_PLAN,
    TOOLBOX_TALK,
    INCIDENT_REPORT,
    RISK_ASSESSMENT,
    SAFETY_PROCEDURE,
    TRAINING_MATERIAL,
    AUDIT_CHECKLIST
}