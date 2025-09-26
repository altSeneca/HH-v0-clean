package com.hazardhawk.ui.gallery.state

import androidx.compose.runtime.Stable
import com.hazardhawk.ai.PhotoAnalysisWithTags
import com.hazardhawk.models.OSHAAnalysisResult
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Data models for the two-state analysis workflow
 * These models support the PhotoViewer's PRE_ANALYSIS and POST_ANALYSIS phases
 */

/**
 * Represents a complete analysis session from start to finish
 */
@Stable
data class AnalysisSession(
    val id: String,
    val photoId: String,
    val startedAt: Instant = Clock.System.now(),
    val completedAt: Instant? = null,
    val currentPhase: AnalysisPhase = AnalysisPhase.PRE_ANALYSIS,
    val manualTagsData: ManualTagsData = ManualTagsData(),
    val aiAnalysisData: AIAnalysisData = AIAnalysisData(),
    val oshaAnalysisData: OSHAAnalysisData = OSHAAnalysisData(),
    val metadata: AnalysisMetadata = AnalysisMetadata()
) {
    val isComplete: Boolean get() = completedAt != null
    val durationMs: Long get() = (completedAt ?: Clock.System.now()).toEpochMilliseconds() - startedAt.toEpochMilliseconds()
    val totalFindingsCount: Int get() = manualTagsData.tags.size + (aiAnalysisData.analysis?.hazardDetections?.size ?: 0)
}

/**
 * Container for manual tagging data and metrics
 */
@Stable
data class ManualTagsData(
    val tags: List<ManualHazardTag> = emptyList(),
    val tagInputHistory: List<TagInputEvent> = emptyList(),
    val timespentTaggingMs: Long = 0L,
    val averageTaggingTimeMs: Long = 0L
) {
    val tagsByCategory: Map<HazardTagCategory, List<ManualHazardTag>> get() = tags.groupBy { it.category }
    val mostCommonCategory: HazardTagCategory? get() = tagsByCategory.maxByOrNull { it.value.size }?.key
    val hasAnyTags: Boolean get() = tags.isNotEmpty()
}

/**
 * Container for AI analysis data and validation
 */
@Stable
data class AIAnalysisData(
    val analysis: PhotoAnalysisWithTags? = null,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val validation: AIValidationState = AIValidationState(),
    val requestedAt: Instant? = null,
    val completedAt: Instant? = null,
    val retryCount: Int = 0
) {
    val processingTimeMs: Long get() = {
        if (requestedAt != null && completedAt != null) {
            completedAt.toEpochMilliseconds() - requestedAt.toEpochMilliseconds()
        } else 0L
    }()
    val hasResults: Boolean get() = analysis != null
    val isValidated: Boolean get() = validation.isValidated
}

/**
 * Container for OSHA analysis data and display state
 */
@Stable
data class OSHAAnalysisData(
    val analysis: OSHAAnalysisResult? = null,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val displayState: OSHADisplayState = OSHADisplayState(),
    val requestedAt: Instant? = null,
    val completedAt: Instant? = null
) {
    val hasResults: Boolean get() = analysis != null
    val isVisible: Boolean get() = displayState.isVisible
}

/**
 * Metadata about the analysis session
 */
@Stable
data class AnalysisMetadata(
    val userId: String? = null,
    val deviceInfo: String? = null,
    val appVersion: String? = null,
    val workType: String? = null,
    val siteId: String? = null,
    val additionalContext: Map<String, String> = emptyMap()
)

/**
 * Tracks user interactions during manual tagging
 */
@Stable
data class TagInputEvent(
    val timestamp: Instant = Clock.System.now(),
    val action: TagInputAction,
    val tagName: String,
    val category: HazardTagCategory,
    val timeTakenMs: Long = 0L
)

/**
 * Types of tag input actions for analytics
 */
enum class TagInputAction {
    ADDED,
    REMOVED,
    MODIFIED,
    SELECTED_FROM_PREDEFINED,
    TYPED_CUSTOM
}

/**
 * Comparison between manual and AI analysis results
 */
@Stable
data class AnalysisComparison(
    val manualTags: List<ManualHazardTag>,
    val aiFindings: List<String>, // Simplified to tags for now
    val agreements: List<TagAgreement>,
    val disagreements: List<TagDisagreement>,
    val manualOnly: List<ManualHazardTag>,
    val aiOnly: List<String> // Simplified to tags for now
) {
    val agreementRate: Float get() = if (totalComparisons > 0) agreements.size.toFloat() / totalComparisons else 0f
    val totalComparisons: Int get() = agreements.size + disagreements.size
    val hasSignificantDisagreement: Boolean get() = disagreements.size > agreements.size
}

/**
 * Represents agreement between manual and AI analysis
 */
@Stable
data class TagAgreement(
    val manualTag: ManualHazardTag,
    val aiHazard: String, // Simplified to tag for now
    val confidenceScore: Float,
    val semanticSimilarity: Float
)

/**
 * Represents disagreement between manual and AI analysis
 */
@Stable
data class TagDisagreement(
    val manualTag: ManualHazardTag?,
    val aiHazard: String?, // Simplified to tag for now
    val reason: DisagreementReason,
    val explanation: String
)

/**
 * Reasons for disagreement between manual and AI analysis
 */
enum class DisagreementReason {
    AI_MISSED_HAZARD,
    AI_FALSE_POSITIVE,
    CATEGORY_MISMATCH,
    SEVERITY_MISMATCH,
    DIFFERENT_INTERPRETATION,
    INSUFFICIENT_AI_CONFIDENCE
}

/**
 * Progress tracking for analysis operations
 */
@Stable
data class AnalysisProgress(
    val currentStep: AnalysisStep,
    val totalSteps: Int,
    val completedSteps: Int,
    val currentStepProgress: Float = 0f,
    val estimatedTimeRemainingMs: Long = 0L,
    val message: String? = null
) {
    val overallProgress: Float get() = (completedSteps + currentStepProgress) / totalSteps.coerceAtLeast(1)
    val isComplete: Boolean get() = completedSteps >= totalSteps
}

/**
 * Steps in the analysis process
 */
enum class AnalysisStep {
    PREPARING_IMAGE,
    UPLOADING_TO_AI,
    RUNNING_AI_ANALYSIS,
    PROCESSING_RESULTS,
    FETCHING_OSHA_DATA,
    GENERATING_RECOMMENDATIONS,
    FINALIZING_RESULTS
}

/**
 * Configuration for analysis behavior
 */
@Stable
data class AnalysisConfiguration(
    val enableAutoTransition: Boolean = true, // Auto-transition to POST_ANALYSIS after AI completes
    val requireManualValidation: Boolean = false, // Require user to validate AI results
    val enableOSHAAutoFetch: Boolean = true, // Automatically fetch OSHA data after AI analysis
    val maxRetryAttempts: Int = 3,
    val timeoutMs: Long = 30000L,
    val enableAnalysisComparison: Boolean = true,
    val saveIntermediateResults: Boolean = true
)

/**
 * Result of the complete analysis workflow
 */
@Stable
data class WorkflowResult(
    val session: AnalysisSession,
    val finalAnalysis: CombinedAnalysisResult,
    val userValidation: UserValidationResult,
    val oshaCompliance: OSHAComplianceResult,
    val recommendations: List<SafetyRecommendation>,
    val exportData: AnalysisExportData
)

/**
 * Combined results from all analysis phases
 */
@Stable
data class CombinedAnalysisResult(
    val hazards: List<IdentifiedHazard>,
    val riskLevel: RiskLevel,
    val confidence: Float,
    val completeness: Float,
    val sourceBreakdown: Map<AnalysisSource, Int>
)

/**
 * A hazard identified by any analysis method
 */
@Stable
data class IdentifiedHazard(
    val id: String,
    val name: String,
    val category: HazardTagCategory,
    val severity: HazardSeverity,
    val confidence: Float,
    val source: AnalysisSource,
    val location: HazardLocation? = null,
    val oshaReference: String? = null,
    val recommendations: List<String> = emptyList()
)

/**
 * Source of hazard identification
 */
enum class AnalysisSource {
    MANUAL_USER_INPUT,
    AI_DETECTION,
    OSHA_COMPLIANCE_CHECK,
    VALIDATION_OVERRIDE
}

/**
 * Hazard severity levels
 */
enum class HazardSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Risk level for the overall analysis
 */
enum class RiskLevel {
    MINIMAL,
    LOW,
    MODERATE,
    HIGH,
    SEVERE
}

/**
 * Location of a hazard in the image
 */
@Stable
data class HazardLocation(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val boundingBoxId: String? = null
)

/**
 * User validation of analysis results
 */
@Stable
data class UserValidationResult(
    val isValidated: Boolean,
    val validatedAt: Instant?,
    val validatedBy: String?,
    val overrides: List<ValidationOverride>,
    val additionalFindings: List<ManualHazardTag>,
    val comments: String?
)

/**
 * Override of AI analysis by user
 */
@Stable
data class ValidationOverride(
    val originalFindingId: String,
    val action: OverrideAction,
    val reason: String,
    val replacementFinding: IdentifiedHazard?
)

/**
 * Types of validation overrides
 */
enum class OverrideAction {
    ACCEPT,
    REJECT,
    MODIFY,
    REPLACE
}

/**
 * OSHA compliance analysis result
 */
@Stable
data class OSHAComplianceResult(
    val overallCompliance: ComplianceLevel,
    val violations: List<OSHAViolation>,
    val applicableStandards: List<OSHAStandard>,
    val recommendations: List<ComplianceRecommendation>
)

/**
 * OSHA compliance levels
 */
enum class ComplianceLevel {
    COMPLIANT,
    MINOR_VIOLATIONS,
    MAJOR_VIOLATIONS,
    CRITICAL_VIOLATIONS
}

/**
 * OSHA violation details
 */
@Stable
data class OSHAViolation(
    val standardId: String,
    val description: String,
    val severity: ViolationSeverity,
    val hazardIds: List<String>,
    val correctiveActions: List<String>
)

/**
 * OSHA standard information
 */
@Stable
data class OSHAStandard(
    val id: String,
    val title: String,
    val description: String,
    val applicableHazards: List<HazardTagCategory>,
    val url: String?
)

/**
 * Compliance recommendation
 */
@Stable
data class ComplianceRecommendation(
    val priority: RecommendationPriority,
    val action: String,
    val standardReference: String,
    val estimatedCost: String?,
    val timeline: String?
)

/**
 * Severity of OSHA violations
 */
enum class ViolationSeverity {
    OTHER,
    SERIOUS,
    WILLFUL,
    REPEAT
}

/**
 * Priority of recommendations
 */
enum class RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH,
    IMMEDIATE
}

/**
 * Safety recommendation
 */
@Stable
data class SafetyRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val category: HazardTagCategory,
    val applicableHazards: List<String>,
    val actionItems: List<String>,
    val resources: List<String>
)

/**
 * Data prepared for export (PDF, reports, etc.)
 */
@Stable
data class AnalysisExportData(
    val summary: AnalysisSummary,
    val detailedFindings: List<IdentifiedHazard>,
    val recommendations: List<SafetyRecommendation>,
    val oshaCompliance: OSHAComplianceResult,
    val metadata: ExportMetadata,
    val attachments: List<ExportAttachment>
)

/**
 * Summary of analysis for export
 */
@Stable
data class AnalysisSummary(
    val totalHazards: Int,
    val criticalHazards: Int,
    val riskLevel: RiskLevel,
    val complianceLevel: ComplianceLevel,
    val analysisDate: Instant,
    val analyzer: String,
    val photoInfo: PhotoSummary
)

/**
 * Photo information for export
 */
@Stable
data class PhotoSummary(
    val fileName: String,
    val captureDate: Instant,
    val location: String?,
    val workType: String?,
    val dimensions: String
)

/**
 * Export metadata
 */
@Stable
data class ExportMetadata(
    val exportedAt: Instant = Clock.System.now(),
    val exportedBy: String?,
    val format: ExportFormat,
    val version: String,
    val includeImages: Boolean
)

/**
 * Export formats
 */
enum class ExportFormat {
    PDF,
    JSON,
    CSV,
    XML
}

/**
 * Export attachments
 */
@Stable
data class ExportAttachment(
    val id: String,
    val name: String,
    val type: AttachmentType,
    val data: ByteArray?,
    val url: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExportAttachment

        if (id != other.id) return false
        if (name != other.name) return false
        if (type != other.type) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (url != other.url) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        return result
    }
}

/**
 * Types of export attachments
 */
enum class AttachmentType {
    ORIGINAL_PHOTO,
    ANNOTATED_PHOTO,
    ANALYSIS_CHART,
    COMPLIANCE_DOCUMENT,
    RECOMMENDATION_CHECKLIST
}