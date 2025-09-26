package com.hazardhawk.ui.gallery.state

import androidx.compose.runtime.Stable
import com.hazardhawk.ai.PhotoAnalysisWithTags
import com.hazardhawk.models.OSHAAnalysisResult
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Analysis workflow phases for PhotoViewer
 */
enum class AnalysisPhase {
    PRE_ANALYSIS,   // User can add manual tags, no AI analysis yet
    POST_ANALYSIS   // AI analysis complete, results displayed with OSHA data
}

/**
 * Manual hazard tag with metadata
 */
@Stable
data class ManualHazardTag(
    val id: String,
    val name: String,
    val category: HazardTagCategory,
    val addedAt: Instant = Clock.System.now(),
    val confidence: Float = 1.0f, // Manual tags have full confidence
    val userNote: String? = null
)

/**
 * Categories for hazard tags to organize them logically
 */
enum class HazardTagCategory {
    PPE_VIOLATION,
    FALL_HAZARD,
    ELECTRICAL,
    MACHINERY,
    CHEMICAL,
    STRUCTURAL,
    ENVIRONMENTAL,
    GENERAL_SAFETY,
    CUSTOM
}

/**
 * AI analysis result validation state
 */
@Stable
data class AIValidationState(
    val isValidated: Boolean = false,
    val overriddenFindings: Set<String> = emptySet(), // IDs of AI findings user disagreed with
    val additionalFindings: List<ManualHazardTag> = emptyList(), // User-added findings
    val validatedAt: Instant? = null,
    val validatedBy: String? = null
)

/**
 * OSHA standards visibility and display state
 */
@Stable
data class OSHADisplayState(
    val isVisible: Boolean = false,
    val expandedStandards: Set<String> = emptySet(), // OSHA standard IDs that are expanded
    val selectedStandard: String? = null // Currently selected standard for detailed view
)

/**
 * Comprehensive Safety Analysis State Management
 * Supports two-phase analysis workflow: PRE_ANALYSIS and POST_ANALYSIS
 */
@Stable
data class SafetyAnalysisState(
    // Core analysis phase
    val currentPhase: AnalysisPhase = AnalysisPhase.PRE_ANALYSIS,

    // Pre-analysis state (manual tagging)
    val manualTags: List<ManualHazardTag> = emptyList(),
    val availableTagCategories: List<HazardTagCategory> = HazardTagCategory.values().toList(),
    val isAddingManualTag: Boolean = false,

    // AI analysis state
    val aiAnalysis: PhotoAnalysisWithTags? = null,
    val isAnalyzingAI: Boolean = false,
    val aiError: String? = null,
    val aiValidation: AIValidationState = AIValidationState(),

    // OSHA analysis state
    val oshaAnalysis: OSHAAnalysisResult? = null,
    val isAnalyzingOSHA: Boolean = false,
    val oshaError: String? = null,
    val oshaDisplay: OSHADisplayState = OSHADisplayState(),

    // UI display settings
    val showBoundingBoxes: Boolean = false,
    val showAnalysisComparison: Boolean = false // Shows manual vs AI tags side-by-side
) {
    // Computed properties
    val isAnalyzing: Boolean get() = isAnalyzingAI || isAnalyzingOSHA
    val hasAnyAnalysis: Boolean get() = aiAnalysis != null || oshaAnalysis != null || manualTags.isNotEmpty()
    val hasErrors: Boolean get() = aiError != null || oshaError != null
    val canProceedToAIAnalysis: Boolean get() = currentPhase == AnalysisPhase.PRE_ANALYSIS && !isAnalyzing
    val isPostAnalysisPhase: Boolean get() = currentPhase == AnalysisPhase.POST_ANALYSIS
    val hasValidatedAIResults: Boolean get() = aiValidation.isValidated

    // Tag-related computed properties
    val totalHazardCount: Int get() = manualTags.size + (aiAnalysis?.hazardDetections?.size ?: 0)
    val manualTagsByCategory: Map<HazardTagCategory, List<ManualHazardTag>> get() =
        manualTags.groupBy { it.category }
    val criticalHazardCount: Int get() = manualTags.count { it.category in listOf(
        HazardTagCategory.FALL_HAZARD,
        HazardTagCategory.ELECTRICAL,
        HazardTagCategory.STRUCTURAL
    )}
}

/**
 * Actions for Safety Analysis State - Extended for Two-Phase Workflow
 */
sealed class SafetyAnalysisAction {
    // Phase management
    data class SetAnalysisPhase(val phase: AnalysisPhase) : SafetyAnalysisAction()
    object TransitionToPostAnalysis : SafetyAnalysisAction()
    object TransitionToPreAnalysis : SafetyAnalysisAction()

    // Manual tagging actions (PRE_ANALYSIS phase)
    data class AddManualTag(val tag: ManualHazardTag) : SafetyAnalysisAction()
    data class RemoveManualTag(val tagId: String) : SafetyAnalysisAction()
    data class UpdateManualTag(val tagId: String, val updatedTag: ManualHazardTag) : SafetyAnalysisAction()
    data class SetAddingManualTag(val isAdding: Boolean) : SafetyAnalysisAction()
    object ClearManualTags : SafetyAnalysisAction()

    // AI analysis actions
    object StartAIAnalysis : SafetyAnalysisAction()
    data class SetAIResult(val result: PhotoAnalysisWithTags?) : SafetyAnalysisAction()
    data class SetAIError(val error: String?) : SafetyAnalysisAction()

    // AI validation actions (POST_ANALYSIS phase)
    data class ValidateAIResults(val validatedBy: String?) : SafetyAnalysisAction()
    data class OverrideAIFinding(val findingId: String, val override: Boolean) : SafetyAnalysisAction()
    data class AddValidationFinding(val finding: ManualHazardTag) : SafetyAnalysisAction()
    object ClearAIValidation : SafetyAnalysisAction()

    // OSHA analysis actions
    object StartOSHAAnalysis : SafetyAnalysisAction()
    data class SetOSHAResult(val result: OSHAAnalysisResult?) : SafetyAnalysisAction()
    data class SetOSHAError(val error: String?) : SafetyAnalysisAction()

    // OSHA display actions
    data class SetOSHAVisible(val visible: Boolean) : SafetyAnalysisAction()
    data class ExpandOSHAStandard(val standardId: String, val expanded: Boolean) : SafetyAnalysisAction()
    data class SelectOSHAStandard(val standardId: String?) : SafetyAnalysisAction()

    // UI display actions
    data class SetBoundingBoxesVisible(val visible: Boolean) : SafetyAnalysisAction()
    data class SetAnalysisComparisonVisible(val visible: Boolean) : SafetyAnalysisAction()

    // General actions
    object ClearErrors : SafetyAnalysisAction()
    object Reset : SafetyAnalysisAction()
}

/**
 * Reducer function for Safety Analysis State - Extended for Two-Phase Workflow
 */
fun safetyAnalysisReducer(
    state: SafetyAnalysisState,
    action: SafetyAnalysisAction
): SafetyAnalysisState {
    return when (action) {
        // Phase management
        is SafetyAnalysisAction.SetAnalysisPhase -> state.copy(
            currentPhase = action.phase
        )

        SafetyAnalysisAction.TransitionToPostAnalysis -> state.copy(
            currentPhase = AnalysisPhase.POST_ANALYSIS
        )

        SafetyAnalysisAction.TransitionToPreAnalysis -> state.copy(
            currentPhase = AnalysisPhase.PRE_ANALYSIS,
            aiValidation = AIValidationState() // Reset validation when going back
        )

        // Manual tagging actions
        is SafetyAnalysisAction.AddManualTag -> state.copy(
            manualTags = state.manualTags + action.tag,
            isAddingManualTag = false
        )

        is SafetyAnalysisAction.RemoveManualTag -> state.copy(
            manualTags = state.manualTags.filterNot { it.id == action.tagId }
        )

        is SafetyAnalysisAction.UpdateManualTag -> state.copy(
            manualTags = state.manualTags.map { tag ->
                if (tag.id == action.tagId) action.updatedTag else tag
            }
        )

        is SafetyAnalysisAction.SetAddingManualTag -> state.copy(
            isAddingManualTag = action.isAdding
        )

        SafetyAnalysisAction.ClearManualTags -> state.copy(
            manualTags = emptyList()
        )

        // AI analysis actions
        SafetyAnalysisAction.StartAIAnalysis -> state.copy(
            isAnalyzingAI = true,
            aiError = null
        )

        is SafetyAnalysisAction.SetAIResult -> state.copy(
            aiAnalysis = action.result,
            isAnalyzingAI = false,
            aiError = null,
            currentPhase = if (action.result != null) AnalysisPhase.POST_ANALYSIS else state.currentPhase
        )

        is SafetyAnalysisAction.SetAIError -> state.copy(
            isAnalyzingAI = false,
            aiError = action.error
        )

        // AI validation actions
        is SafetyAnalysisAction.ValidateAIResults -> state.copy(
            aiValidation = state.aiValidation.copy(
                isValidated = true,
                validatedAt = Clock.System.now(),
                validatedBy = action.validatedBy
            )
        )

        is SafetyAnalysisAction.OverrideAIFinding -> {
            val overriddenFindings = if (action.override) {
                state.aiValidation.overriddenFindings + action.findingId
            } else {
                state.aiValidation.overriddenFindings - action.findingId
            }
            state.copy(
                aiValidation = state.aiValidation.copy(
                    overriddenFindings = overriddenFindings
                )
            )
        }

        is SafetyAnalysisAction.AddValidationFinding -> state.copy(
            aiValidation = state.aiValidation.copy(
                additionalFindings = state.aiValidation.additionalFindings + action.finding
            )
        )

        SafetyAnalysisAction.ClearAIValidation -> state.copy(
            aiValidation = AIValidationState()
        )

        // OSHA analysis actions
        SafetyAnalysisAction.StartOSHAAnalysis -> state.copy(
            isAnalyzingOSHA = true,
            oshaError = null
        )

        is SafetyAnalysisAction.SetOSHAResult -> state.copy(
            oshaAnalysis = action.result,
            isAnalyzingOSHA = false,
            oshaError = null
        )

        is SafetyAnalysisAction.SetOSHAError -> state.copy(
            isAnalyzingOSHA = false,
            oshaError = action.error
        )

        // OSHA display actions
        is SafetyAnalysisAction.SetOSHAVisible -> state.copy(
            oshaDisplay = state.oshaDisplay.copy(
                isVisible = action.visible
            )
        )

        is SafetyAnalysisAction.ExpandOSHAStandard -> {
            val expandedStandards = if (action.expanded) {
                state.oshaDisplay.expandedStandards + action.standardId
            } else {
                state.oshaDisplay.expandedStandards - action.standardId
            }
            state.copy(
                oshaDisplay = state.oshaDisplay.copy(
                    expandedStandards = expandedStandards
                )
            )
        }

        is SafetyAnalysisAction.SelectOSHAStandard -> state.copy(
            oshaDisplay = state.oshaDisplay.copy(
                selectedStandard = action.standardId
            )
        )

        // UI display actions
        is SafetyAnalysisAction.SetBoundingBoxesVisible -> state.copy(
            showBoundingBoxes = action.visible
        )

        is SafetyAnalysisAction.SetAnalysisComparisonVisible -> state.copy(
            showAnalysisComparison = action.visible
        )

        // General actions
        SafetyAnalysisAction.ClearErrors -> state.copy(
            aiError = null,
            oshaError = null
        )

        SafetyAnalysisAction.Reset -> SafetyAnalysisState()
    }
}

/**
 * Helper functions for state management
 */

/**
 * Create a manual hazard tag with sensible defaults
 */
fun createManualHazardTag(
    name: String,
    category: HazardTagCategory,
    userNote: String? = null
): ManualHazardTag {
    return ManualHazardTag(
        id = "manual-${Clock.System.now().toEpochMilliseconds()}-${name.hashCode()}",
        name = name,
        category = category,
        userNote = userNote
    )
}

/**
 * Get predefined hazard tags for a category
 */
fun getPredefinedTagsForCategory(category: HazardTagCategory): List<String> {
    return when (category) {
        HazardTagCategory.PPE_VIOLATION -> listOf(
            "Missing Hard Hat",
            "No Safety Vest",
            "Missing Safety Glasses",
            "No Steel-Toed Boots",
            "Missing Gloves",
            "No Hearing Protection"
        )
        HazardTagCategory.FALL_HAZARD -> listOf(
            "Unguarded Edge",
            "Missing Guardrails",
            "Improper Ladder Use",
            "No Fall Protection",
            "Unstable Surface",
            "Open Excavation"
        )
        HazardTagCategory.ELECTRICAL -> listOf(
            "Exposed Wiring",
            "Damaged Equipment",
            "Missing Ground Fault",
            "Improper Extension Cord",
            "Overhead Power Lines",
            "Wet Conditions"
        )
        HazardTagCategory.MACHINERY -> listOf(
            "Missing Guards",
            "Improper Lockout",
            "Equipment Malfunction",
            "Moving Parts Exposed",
            "Maintenance Required",
            "Operator Error"
        )
        HazardTagCategory.CHEMICAL -> listOf(
            "Improper Storage",
            "Missing Labels",
            "No Ventilation",
            "Spill Hazard",
            "Incompatible Materials",
            "Missing SDS"
        )
        HazardTagCategory.STRUCTURAL -> listOf(
            "Structural Damage",
            "Load Exceeds Capacity",
            "Missing Supports",
            "Deteriorated Materials",
            "Foundation Issues",
            "Unstable Structure"
        )
        HazardTagCategory.ENVIRONMENTAL -> listOf(
            "Extreme Weather",
            "Poor Lighting",
            "Noise Exposure",
            "Air Quality",
            "Temperature Extremes",
            "Limited Visibility"
        )
        HazardTagCategory.GENERAL_SAFETY -> listOf(
            "Housekeeping",
            "Emergency Access",
            "Safety Signage",
            "Traffic Control",
            "Material Storage",
            "Workspace Organization"
        )
        HazardTagCategory.CUSTOM -> listOf(
            "Custom Hazard",
            "Site-Specific Risk",
            "Special Conditions"
        )
    }
}

/**
 * Validate analysis state transitions
 */
fun canTransitionToPhase(currentState: SafetyAnalysisState, targetPhase: AnalysisPhase): Boolean {
    return when (targetPhase) {
        AnalysisPhase.PRE_ANALYSIS -> true // Can always go back to pre-analysis
        AnalysisPhase.POST_ANALYSIS -> {
            // Can only transition to post-analysis if we have AI analysis results
            currentState.aiAnalysis != null && !currentState.isAnalyzing
        }
    }
}

/**
 * Calculate analysis completeness score (0.0 to 1.0)
 */
fun calculateAnalysisCompleteness(state: SafetyAnalysisState): Float {
    var score = 0f
    var totalWeight = 0f

    // Manual tags contribute 30%
    totalWeight += 0.3f
    if (state.manualTags.isNotEmpty()) {
        score += 0.3f
    }

    // AI analysis contributes 40%
    totalWeight += 0.4f
    if (state.aiAnalysis != null) {
        score += 0.4f
    }

    // AI validation contributes 20%
    totalWeight += 0.2f
    if (state.aiValidation.isValidated) {
        score += 0.2f
    }

    // OSHA analysis contributes 10%
    totalWeight += 0.1f
    if (state.oshaAnalysis != null) {
        score += 0.1f
    }

    return if (totalWeight > 0) score / totalWeight else 0f
}