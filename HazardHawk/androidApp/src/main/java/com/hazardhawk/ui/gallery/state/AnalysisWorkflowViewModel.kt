package com.hazardhawk.ui.gallery.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.ai.GeminiVisionAnalyzer
import com.hazardhawk.ai.PhotoAnalysisWithTags
import com.hazardhawk.data.repositories.OSHARegulationRepository
import com.hazardhawk.domain.entities.Photo
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.domain.repositories.AnalysisRepository
import com.hazardhawk.models.OSHAAnalysisResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import android.util.Log
import java.io.File

/**
 * ViewModel for managing the two-state analysis workflow in PhotoViewer
 * 
 * Handles:
 * - PRE_ANALYSIS: Manual hazard tagging by user
 * - POST_ANALYSIS: AI analysis results, OSHA compliance, validation
 * - State transitions between phases
 * - Integration with analysis backends
 * - Progress tracking and error handling
 */
class AnalysisWorkflowViewModel(
    private val aiService: GeminiVisionAnalyzer,
    private val oshaRepository: OSHARegulationRepository,
    private val analysisRepository: AnalysisRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SafetyAnalysisState())
    val state: StateFlow<SafetyAnalysisState> = _state.asStateFlow()

    private val _analysisSession = MutableStateFlow<AnalysisSession?>(null)
    val analysisSession: StateFlow<AnalysisSession?> = _analysisSession.asStateFlow()

    private val _analysisProgress = MutableStateFlow<AnalysisProgress?>(null)
    val analysisProgress: StateFlow<AnalysisProgress?> = _analysisProgress.asStateFlow()

    private val _configuration = MutableStateFlow(AnalysisConfiguration())
    val configuration: StateFlow<AnalysisConfiguration> = _configuration.asStateFlow()

    // Current photo being analyzed
    private var currentPhoto: Photo? = null

    /**
     * Initialize analysis for a photo
     */
    fun initializeAnalysis(photo: Photo) {
        currentPhoto = photo
        val session = AnalysisSession(
            id = "session-${Clock.System.now().toEpochMilliseconds()}",
            photoId = photo.id
        )
        _analysisSession.value = session
        dispatch(SafetyAnalysisAction.Reset)
        
        // Load any existing analysis
        viewModelScope.launch {
            loadExistingAnalysis(photo.id)
        }
    }

    /**
     * Load existing analysis for a photo
     */
    private suspend fun loadExistingAnalysis(photoId: String) {
        try {
            analysisRepository.getAnalysis(photoId)?.let { savedAnalysis ->
                // Convert saved analysis back to PhotoAnalysisWithTags
                val analysisResult = convertSafetyAnalysisToPhotoAnalysis(savedAnalysis)
                dispatch(SafetyAnalysisAction.SetAIResult(analysisResult))
                
                // If we have AI results, transition to POST_ANALYSIS
                if (analysisResult != null) {
                    dispatch(SafetyAnalysisAction.TransitionToPostAnalysis)
                }
            }
        } catch (e: Exception) {
            Log.e("AnalysisWorkflowViewModel", "Failed to load existing analysis", e)
        }
    }

    /**
     * Dispatch actions to update state
     */
    fun dispatch(action: SafetyAnalysisAction) {
        val currentState = _state.value
        val newState = safetyAnalysisReducer(currentState, action)
        _state.value = newState
        
        // Handle side effects
        handleSideEffects(action, currentState, newState)
    }

    /**
     * Handle side effects from state changes
     */
    private fun handleSideEffects(
        action: SafetyAnalysisAction,
        oldState: SafetyAnalysisState,
        newState: SafetyAnalysisState
    ) {
        when (action) {
            is SafetyAnalysisAction.AddManualTag -> {
                updateAnalysisSession { session ->
                    session.copy(
                        manualTagsData = session.manualTagsData.copy(
                            tags = newState.manualTags,
                            tagInputHistory = session.manualTagsData.tagInputHistory + TagInputEvent(
                                action = TagInputAction.ADDED,
                                tagName = action.tag.name,
                                category = action.tag.category
                            )
                        )
                    )
                }
            }
            
            SafetyAnalysisAction.StartAIAnalysis -> {
                currentPhoto?.let { photo ->
                    viewModelScope.launch {
                        performAIAnalysis(photo)
                    }
                }
            }
            
            SafetyAnalysisAction.StartOSHAAnalysis -> {
                currentPhoto?.let { photo ->
                    viewModelScope.launch {
                        performOSHAAnalysis(photo)
                    }
                }
            }
            
            is SafetyAnalysisAction.SetAIResult -> {
                if (action.result != null && _configuration.value.enableOSHAAutoFetch) {
                    // Auto-fetch OSHA data when AI analysis completes
                    dispatch(SafetyAnalysisAction.StartOSHAAnalysis)
                }
                
                updateAnalysisSession { session ->
                    session.copy(
                        aiAnalysisData = session.aiAnalysisData.copy(
                            analysis = action.result,
                            completedAt = Clock.System.now()
                        )
                    )
                }
            }
            
            SafetyAnalysisAction.TransitionToPostAnalysis -> {
                updateAnalysisSession { session ->
                    session.copy(currentPhase = AnalysisPhase.POST_ANALYSIS)
                }
            }
            
            else -> { /* No side effects */ }
        }
    }

    /**
     * Add a manual hazard tag
     */
    fun addManualTag(name: String, category: HazardTagCategory, userNote: String? = null) {
        val tag = createManualHazardTag(name, category, userNote)
        dispatch(SafetyAnalysisAction.AddManualTag(tag))
    }

    /**
     * Start AI analysis process
     */
    fun startAIAnalysis() {
        if (_state.value.canProceedToAIAnalysis) {
            dispatch(SafetyAnalysisAction.StartAIAnalysis)
        }
    }

    /**
     * Validate AI analysis results
     */
    fun validateAIResults(validatedBy: String? = null) {
        dispatch(SafetyAnalysisAction.ValidateAIResults(validatedBy))
    }

    /**
     * Override a specific AI finding
     */
    fun overrideAIFinding(findingId: String, override: Boolean) {
        dispatch(SafetyAnalysisAction.OverrideAIFinding(findingId, override))
    }

    /**
     * Transition between analysis phases
     */
    fun transitionToPhase(phase: AnalysisPhase) {
        val currentState = _state.value
        if (canTransitionToPhase(currentState, phase)) {
            when (phase) {
                AnalysisPhase.PRE_ANALYSIS -> dispatch(SafetyAnalysisAction.TransitionToPreAnalysis)
                AnalysisPhase.POST_ANALYSIS -> dispatch(SafetyAnalysisAction.TransitionToPostAnalysis)
            }
        } else {
            Log.w("AnalysisWorkflowViewModel", "Cannot transition to phase $phase from current state")
        }
    }

    /**
     * Get analysis comparison between manual and AI results
     */
    fun getAnalysisComparison(): AnalysisComparison? {
        val currentState = _state.value
        val aiFindings = currentState.aiAnalysis?.hazardDetections ?: return null
        
        return AnalysisComparison(
            manualTags = currentState.manualTags,
            aiFindings = currentState.aiAnalysis?.recommendedTags ?: emptyList(),
            agreements = emptyList(), // TODO: Implement agreement detection
            disagreements = emptyList(), // TODO: Implement disagreement detection
            manualOnly = currentState.manualTags, // Simplified for now
            aiOnly = currentState.aiAnalysis?.recommendedTags ?: emptyList() // Simplified for now
        )
    }

    /**
     * Update analysis configuration
     */
    fun updateConfiguration(newConfiguration: AnalysisConfiguration) {
        _configuration.value = newConfiguration
    }

    /**
     * Get workflow result for export
     */
    fun getWorkflowResult(): WorkflowResult? {
        val session = _analysisSession.value ?: return null
        val currentState = _state.value
        
        // Create combined analysis result
        val hazards = mutableListOf<IdentifiedHazard>()
        
        // Add manual tags as hazards
        hazards.addAll(currentState.manualTags.map { tag ->
            IdentifiedHazard(
                id = tag.id,
                name = tag.name,
                category = tag.category,
                severity = mapCategoryToSeverity(tag.category),
                confidence = tag.confidence,
                source = AnalysisSource.MANUAL_USER_INPUT
            )
        })
        
        // Add AI detections as hazards (simplified - fix when actual hazard detection models are available)
        currentState.aiAnalysis?.recommendedTags?.forEach { tag ->
            hazards.add(
                IdentifiedHazard(
                    id = "ai-${tag.hashCode()}",
                    name = tag,
                    category = HazardTagCategory.GENERAL_SAFETY, // Default category
                    severity = HazardSeverity.MEDIUM, // Default severity
                    confidence = 0.8f, // Default confidence
                    source = AnalysisSource.AI_DETECTION
                )
            )
        }
        
        val combinedResult = CombinedAnalysisResult(
            hazards = hazards,
            riskLevel = calculateRiskLevel(hazards),
            confidence = calculateOverallConfidence(hazards),
            completeness = calculateAnalysisCompleteness(currentState),
            sourceBreakdown = hazards.groupBy { it.source }.mapValues { it.value.size }
        )
        
        return WorkflowResult(
            session = session,
            finalAnalysis = combinedResult,
            userValidation = UserValidationResult(
                isValidated = currentState.aiValidation.isValidated,
                validatedAt = currentState.aiValidation.validatedAt,
                validatedBy = currentState.aiValidation.validatedBy,
                overrides = emptyList(), // TODO: Implement validation overrides
                additionalFindings = currentState.aiValidation.additionalFindings,
                comments = null
            ),
            oshaCompliance = OSHAComplianceResult(
                overallCompliance = ComplianceLevel.COMPLIANT, // TODO: Calculate from OSHA analysis
                violations = emptyList(),
                applicableStandards = emptyList(),
                recommendations = emptyList()
            ),
            recommendations = generateRecommendations(combinedResult),
            exportData = createExportData(session, combinedResult)
        )
    }

    /**
     * Perform AI analysis on the photo
     */
    private suspend fun performAIAnalysis(photo: Photo) {
        updateAnalysisProgress(
            AnalysisProgress(
                currentStep = AnalysisStep.PREPARING_IMAGE,
                totalSteps = 4,
                completedSteps = 0,
                message = "Preparing image for analysis..."
            )
        )
        
        try {
            updateAnalysisSession { session ->
                session.copy(
                    aiAnalysisData = session.aiAnalysisData.copy(
                        requestedAt = Clock.System.now()
                    )
                )
            }
            
            aiService.initialize()
            
            updateAnalysisProgress(
                AnalysisProgress(
                    currentStep = AnalysisStep.RUNNING_AI_ANALYSIS,
                    totalSteps = 4,
                    completedSteps = 1,
                    message = "Running AI analysis..."
                )
            )
            
            val photoFile = File(photo.filePath)
            if (!photoFile.exists()) {
                dispatch(SafetyAnalysisAction.SetAIError("Photo file not found"))
                return
            }
            
            val photoBytes = photoFile.readBytes()
            val result = aiService.analyzePhotoWithTags(
                data = photoBytes,
                width = 1920,
                height = 1080,
                workType = determineWorkType(photo)
            )
            
            updateAnalysisProgress(
                AnalysisProgress(
                    currentStep = AnalysisStep.PROCESSING_RESULTS,
                    totalSteps = 4,
                    completedSteps = 2,
                    message = "Processing analysis results..."
                )
            )
            
            // Save analysis to database
            try {
                val safetyAnalysis = convertPhotoAnalysisToSafetyAnalysis(result, photo.id)
                analysisRepository.saveAnalysis(safetyAnalysis)
            } catch (e: Exception) {
                Log.e("AnalysisWorkflowViewModel", "Failed to save analysis", e)
            }
            
            updateAnalysisProgress(
                AnalysisProgress(
                    currentStep = AnalysisStep.FINALIZING_RESULTS,
                    totalSteps = 4,
                    completedSteps = 3,
                    currentStepProgress = 1f,
                    message = "Analysis complete!"
                )
            )
            
            dispatch(SafetyAnalysisAction.SetAIResult(result))
            
            // Clear progress after a short delay
            delay(1000)
            _analysisProgress.value = null
            
        } catch (e: Exception) {
            Log.e("AnalysisWorkflowViewModel", "AI analysis failed", e)
            dispatch(SafetyAnalysisAction.SetAIError(e.message ?: "Analysis failed"))
            _analysisProgress.value = null
        }
    }

    /**
     * Perform OSHA compliance analysis
     */
    private suspend fun performOSHAAnalysis(photo: Photo) {
        try {
            updateAnalysisSession { session ->
                session.copy(
                    oshaAnalysisData = session.oshaAnalysisData.copy(
                        requestedAt = Clock.System.now()
                    )
                )
            }
            
            // Determine work type from photo tags or manual tags
            val workType = determineWorkType(photo)
            
            val simpleAnalyzer = com.hazardhawk.ai.impl.SimpleOSHAAnalyzer()
            simpleAnalyzer.configure()
            val result = simpleAnalyzer.analyzeForOSHACompliance(ByteArray(0), workType)
            
            updateAnalysisSession { session ->
                session.copy(
                    oshaAnalysisData = session.oshaAnalysisData.copy(
                        analysis = result.getOrNull(),
                        completedAt = Clock.System.now()
                    )
                )
            }
            
            dispatch(SafetyAnalysisAction.SetOSHAResult(result.getOrNull()))
            
        } catch (e: Exception) {
            Log.e("AnalysisWorkflowViewModel", "OSHA analysis failed", e)
            dispatch(SafetyAnalysisAction.SetOSHAError(e.message ?: "OSHA analysis failed"))
        }
    }

    /**
     * Update analysis session
     */
    private fun updateAnalysisSession(update: (AnalysisSession) -> AnalysisSession) {
        _analysisSession.value?.let { session ->
            _analysisSession.value = update(session)
        }
    }

    /**
     * Update analysis progress
     */
    private fun updateAnalysisProgress(progress: AnalysisProgress) {
        _analysisProgress.value = progress
    }

    /**
     * Determine work type from photo
     */
    private fun determineWorkType(photo: Photo): WorkType {
        val allTags = photo.tags + _state.value.manualTags.map { it.name }
        
        return when {
            allTags.any { it.contains("electrical", ignoreCase = true) } -> WorkType.ELECTRICAL
            allTags.any { it.contains("steel", ignoreCase = true) } -> WorkType.STEEL_WORK
            allTags.any { it.contains("excavation", ignoreCase = true) } -> WorkType.EXCAVATION
            else -> WorkType.GENERAL_CONSTRUCTION
        }
    }

    // Helper functions for mapping and calculations
    private fun mapCategoryToSeverity(category: HazardTagCategory): HazardSeverity {
        return when (category) {
            HazardTagCategory.FALL_HAZARD,
            HazardTagCategory.ELECTRICAL,
            HazardTagCategory.STRUCTURAL -> HazardSeverity.HIGH
            HazardTagCategory.MACHINERY,
            HazardTagCategory.CHEMICAL -> HazardSeverity.MEDIUM
            else -> HazardSeverity.LOW
        }
    }

    // Simplified for now - can be enhanced when actual detection models are available
    private fun mapTagToCategory(tag: String): HazardTagCategory {
        val lowercaseTag = tag.lowercase()
        return when {
            lowercaseTag.contains("fall") || lowercaseTag.contains("height") -> HazardTagCategory.FALL_HAZARD
            lowercaseTag.contains("electrical") || lowercaseTag.contains("wire") -> HazardTagCategory.ELECTRICAL
            lowercaseTag.contains("ppe") || lowercaseTag.contains("helmet") || lowercaseTag.contains("vest") -> HazardTagCategory.PPE_VIOLATION
            lowercaseTag.contains("machinery") || lowercaseTag.contains("equipment") -> HazardTagCategory.MACHINERY
            else -> HazardTagCategory.GENERAL_SAFETY
        }
    }

    private fun mapConfidenceToSeverity(confidence: Float): HazardSeverity {
        return when {
            confidence >= 0.9f -> HazardSeverity.HIGH
            confidence >= 0.7f -> HazardSeverity.MEDIUM
            else -> HazardSeverity.LOW
        }
    }

    private fun calculateRiskLevel(hazards: List<IdentifiedHazard>): RiskLevel {
        val criticalCount = hazards.count { it.severity == HazardSeverity.CRITICAL }
        val highCount = hazards.count { it.severity == HazardSeverity.HIGH }
        
        return when {
            criticalCount > 0 -> RiskLevel.SEVERE
            highCount >= 3 -> RiskLevel.HIGH
            highCount > 0 -> RiskLevel.MODERATE
            hazards.isNotEmpty() -> RiskLevel.LOW
            else -> RiskLevel.MINIMAL
        }
    }

    private fun calculateOverallConfidence(hazards: List<IdentifiedHazard>): Float {
        return if (hazards.isNotEmpty()) {
            hazards.map { it.confidence }.average().toFloat()
        } else 0f
    }

    private fun generateRecommendations(analysis: CombinedAnalysisResult): List<SafetyRecommendation> {
        // Generate basic recommendations based on identified hazards
        return analysis.hazards.mapNotNull { hazard ->
            when (hazard.category) {
                HazardTagCategory.PPE_VIOLATION -> SafetyRecommendation(
                    id = "rec-ppe-${hazard.id}",
                    title = "PPE Compliance",
                    description = "Ensure all workers wear required personal protective equipment",
                    priority = RecommendationPriority.HIGH,
                    category = hazard.category,
                    applicableHazards = listOf(hazard.id),
                    actionItems = listOf(
                        "Provide required PPE to all workers",
                        "Conduct PPE training",
                        "Implement daily PPE checks"
                    ),
                    resources = listOf("OSHA PPE Standards", "Safety Training Materials")
                )
                else -> null // TODO: Add more recommendation types
            }
        }
    }

    private fun createExportData(session: AnalysisSession, analysis: CombinedAnalysisResult): AnalysisExportData {
        val photo = currentPhoto
        
        return AnalysisExportData(
            summary = AnalysisSummary(
                totalHazards = analysis.hazards.size,
                criticalHazards = analysis.hazards.count { it.severity == HazardSeverity.CRITICAL },
                riskLevel = analysis.riskLevel,
                complianceLevel = ComplianceLevel.COMPLIANT, // TODO: Calculate from OSHA analysis
                analysisDate = session.startedAt,
                analyzer = "HazardHawk AI",
                photoInfo = PhotoSummary(
                    fileName = photo?.fileName ?: "Unknown",
                    captureDate = photo?.let { kotlinx.datetime.Instant.fromEpochMilliseconds(it.timestamp) } ?: Clock.System.now(),
                    location = photo?.location?.toString(),
                    workType = null, // TODO: Add work type to photo
                    dimensions = "1920x1080" // TODO: Get actual dimensions
                )
            ),
            detailedFindings = analysis.hazards,
            recommendations = generateRecommendations(analysis),
            oshaCompliance = OSHAComplianceResult(
                overallCompliance = ComplianceLevel.COMPLIANT,
                violations = emptyList(),
                applicableStandards = emptyList(),
                recommendations = emptyList()
            ),
            metadata = ExportMetadata(
                exportedBy = null, // TODO: Add user info
                format = ExportFormat.PDF,
                version = "1.0",
                includeImages = true
            ),
            attachments = emptyList() // TODO: Add photo attachments
        )
    }

    /**
     * Convert PhotoAnalysisWithTags to SafetyAnalysis for database storage
     */
    private fun convertPhotoAnalysisToSafetyAnalysis(
        analysis: PhotoAnalysisWithTags,
        photoId: String
    ): com.hazardhawk.domain.entities.SafetyAnalysis {
        return com.hazardhawk.domain.entities.SafetyAnalysis(
            id = analysis.id,
            photoId = photoId,
            severity = "medium", // TODO: Calculate severity
            aiConfidence = 0.8f, // TODO: Calculate confidence
            analyzedAt = kotlinx.datetime.LocalDateTime(2025, 9, 26, 12, 0, 0, 0), // Placeholder for now
            analysisSource = "AI",
            hazards = analysis.recommendedTags,
            oshaCodes = emptyList(), // TODO: Extract OSHA codes
            recommendations = analysis.recommendedTags
        )
    }

    /**
     * Convert SafetyAnalysis back to PhotoAnalysisWithTags
     */
    private fun convertSafetyAnalysisToPhotoAnalysis(
        analysis: com.hazardhawk.domain.entities.SafetyAnalysis
    ): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            id = analysis.id,
            photoId = analysis.photoId,
            recommendedTags = analysis.recommendations,
            processingTimeMs = 1000L, // Default value
            hazardDetections = emptyList() // TODO: Convert hazards back to detections
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up resources
        viewModelScope.launch {
            try {
                aiService.release()
            } catch (e: Exception) {
                Log.e("AnalysisWorkflowViewModel", "Error releasing AI service", e)
            }
        }
    }
}