package com.hazardhawk.ui.safety.ptp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.data.repositories.ptp.PTPRepository
import com.hazardhawk.documents.PDFMetadata
import com.hazardhawk.documents.PhotoData
import com.hazardhawk.documents.PTPPDFGenerator
import com.hazardhawk.domain.models.ptp.*
import com.hazardhawk.utils.FileStorageUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for PTP Creation and Editing workflow.
 * Manages questionnaire state, AI generation, document editing, and PDF export.
 */
class PTPViewModel(
    private val ptpRepository: PTPRepository,
    private val pdfGenerator: PTPPDFGenerator,
    private val fileStorageUtil: FileStorageUtil
) : ViewModel() {

    // Questionnaire State
    private val _questionnaireState = MutableStateFlow(QuestionnaireState())
    val questionnaireState: StateFlow<QuestionnaireState> = _questionnaireState.asStateFlow()

    // Generation State
    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    // Validation Errors
    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    // Document State (for editor)
    private val _documentState = MutableStateFlow<DocumentState?>(null)
    val documentState: StateFlow<DocumentState?> = _documentState.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow(PTPUIState())
    val uiState: StateFlow<PTPUIState> = _uiState.asStateFlow()

    init {
        // Observe questionnaire state and validate
        viewModelScope.launch {
            questionnaireState.collect { state ->
                validateQuestionnaire(state)
            }
        }
    }

    // ===== Questionnaire Methods =====

    fun updateWorkType(workType: String) {
        _questionnaireState.update { it.copy(workType = workType) }
    }

    fun updateTaskDescription(description: String) {
        _questionnaireState.update { it.copy(taskDescription = description) }
    }

    fun updateToolsEquipment(tools: String) {
        _questionnaireState.update { it.copy(toolsEquipment = tools) }
    }

    fun updateWorkingAtHeight(working: Boolean) {
        _questionnaireState.update { it.copy(workingAtHeight = working) }
    }

    fun updateMaximumHeight(height: Int?) {
        _questionnaireState.update { it.copy(maximumHeight = height) }
    }

    fun updateCrewSize(size: Int?) {
        _questionnaireState.update { it.copy(crewSize = size) }
    }

    fun updateSpecificTasks(tasks: List<String>) {
        _questionnaireState.update { it.copy(specificTasks = tasks) }
    }

    fun updateMechanicalEquipment(equipment: List<String>) {
        _questionnaireState.update { it.copy(mechanicalEquipment = equipment) }
    }

    fun updateEnvironmentalConditions(conditions: List<String>) {
        _questionnaireState.update { it.copy(environmentalConditions = conditions) }
    }

    fun updateMaterialsInvolved(materials: List<String>) {
        _questionnaireState.update { it.copy(materialsInvolved = materials) }
    }

    fun updateNearPowerLines(near: Boolean) {
        _questionnaireState.update { it.copy(nearPowerLines = near) }
    }

    fun updateConfinedSpace(confined: Boolean) {
        _questionnaireState.update { it.copy(confinedSpace = confined) }
    }

    fun updateHazardousMaterials(hazardous: Boolean) {
        _questionnaireState.update { it.copy(hazardousMaterials = hazardous) }
    }

    fun updateAdditionalNotes(notes: String?) {
        _questionnaireState.update { it.copy(additionalNotes = notes) }
    }

    // ===== Validation =====

    private fun validateQuestionnaire(state: QuestionnaireState) {
        val errors = mutableListOf<String>()

        if (state.workType.isBlank()) {
            errors.add("Work type is required")
        }

        if (state.taskDescription.isBlank()) {
            errors.add("Task description is required")
        }

        if (state.toolsEquipment.isBlank()) {
            errors.add("Tools and equipment information is required")
        }

        if (state.crewSize == null || state.crewSize <= 0) {
            errors.add("Valid crew size is required")
        }

        if (state.workingAtHeight && (state.maximumHeight == null || state.maximumHeight <= 0)) {
            errors.add("Maximum height is required when working at height")
        }

        _validationErrors.value = errors
    }

    // ===== AI Generation =====

    fun generatePTP(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _questionnaireState.value

        // Final validation
        if (_validationErrors.value.isNotEmpty()) {
            onError("Please fix all validation errors before generating")
            return
        }

        viewModelScope.launch {
            _generationState.value = GenerationState.Generating(0.0f)
            _uiState.update { it.copy(isLoading = true) }

            try {
                val ptpId = "ptp_${System.currentTimeMillis()}"

                // Build AI request from questionnaire
                val questionnaire = PtpQuestionnaire(
                    workType = state.workType,
                    specificTasks = listOf(state.taskDescription), // Convert description to task list
                    toolsEquipment = state.toolsEquipment.split(",").map { it.trim() },
                    mechanicalEquipment = state.mechanicalEquipment,
                    environmentalConditions = state.environmentalConditions,
                    materialsInvolved = state.materialsInvolved,
                    crewSize = state.crewSize ?: 1,
                    workingAtHeight = state.workingAtHeight,
                    maximumHeight = state.maximumHeight?.toDouble(),
                    fallProtection = emptyList(), // TODO: Add from questionnaire if available
                    nearPowerLines = state.nearPowerLines,
                    confinedSpace = state.confinedSpace,
                    hazardousMaterials = state.hazardousMaterials,
                    additionalNotes = state.additionalNotes
                )

                val aiRequest = PtpAIRequest(
                    questionnaire = questionnaire,
                    photoAnalysisResults = emptyList(), // TODO: Add photo analysis if available
                    projectHistory = emptyList(), // TODO: Add project history if available
                    includeSpanish = false
                )

                // Call AI service to generate PTP content
                val aiResult = ptpRepository.generatePtpWithAI(aiRequest)

                aiResult
                    .onSuccess { aiResponse ->
                        // Create PTP with AI-generated content
                        val ptp = PreTaskPlan(
                            id = ptpId,
                            projectId = null, // TODO: Get from context
                            createdBy = "current_user", // TODO: Get from auth
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            workType = state.workType,
                            workScope = state.taskDescription,
                            crewSize = state.crewSize,
                            status = PtpStatus.DRAFT,
                            aiGeneratedContent = aiResponse.content,
                            userModifiedContent = null,
                            toolsEquipment = state.toolsEquipment.split(",").map { it.trim() },
                            emergencyContacts = emptyList(), // TODO: Add from questionnaire
                            nearestHospital = null,
                            evacuationRoutes = null,
                            pdfPath = null,
                            cloudStorageUrl = null,
                            signatureSupervisor = null
                        )

                        // Save to repository
                        ptpRepository.createPtp(ptp)
                            .onSuccess {
                                _generationState.value = GenerationState.Success(ptpId)
                                _uiState.update { it.copy(isLoading = false) }
                                onSuccess(ptpId)
                            }
                            .onFailure { error ->
                                _generationState.value = GenerationState.Error(
                                    error.message ?: "Failed to save PTP"
                                )
                                _uiState.update { it.copy(isLoading = false, error = error.message) }
                                onError(error.message ?: "Failed to save PTP")
                            }
                    }
                    .onFailure { error ->
                        _generationState.value = GenerationState.Error(
                            error.message ?: "AI generation failed"
                        )
                        _uiState.update { it.copy(isLoading = false, error = error.message) }
                        onError(error.message ?: "AI generation failed")
                    }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Unknown error occurred"
                _generationState.value = GenerationState.Error(errorMsg)
                _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                onError(errorMsg)
            }
        }
    }

    fun clearGenerationError() {
        _generationState.value = GenerationState.Idle
        _uiState.update { it.copy(error = null) }
    }

    // ===== Document Editor Methods =====

    fun loadPTP(ptpId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                ptpRepository.getPtpById(ptpId)
                    .onSuccess { ptp ->
                        if (ptp != null) {
                            _documentState.value = DocumentState(
                                ptp = ptp,
                                aiGeneratedContent = ptp.aiGeneratedContent,
                                userModifiedContent = ptp.userModifiedContent,
                                hasUnsavedChanges = false,
                                hasSignature = ptp.signatureSupervisor != null,
                                aiConfidence = 0.0, // TODO: Get from AI response
                                aiWarnings = emptyList()
                            )
                            _uiState.update { it.copy(isLoading = false) }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "PTP not found"
                                )
                            }
                        }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load PTP"
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun updateHazard(index: Int, hazard: PtpHazard) {
        _documentState.update { state ->
            state?.let {
                val currentContent = it.userModifiedContent ?: it.aiGeneratedContent
                if (currentContent != null) {
                    val updatedHazards = currentContent.hazards.toMutableList().apply {
                        if (index < size) {
                            set(index, hazard)
                        }
                    }
                    val updatedContent = currentContent.copy(hazards = updatedHazards)
                    it.copy(
                        userModifiedContent = updatedContent,
                        hasUnsavedChanges = true
                    )
                } else {
                    it
                }
            }
        }
    }

    fun updateJobStep(index: Int, jobStep: JobStep) {
        _documentState.update { state ->
            state?.let {
                val currentContent = it.userModifiedContent ?: it.aiGeneratedContent
                if (currentContent != null) {
                    val updatedSteps = currentContent.jobSteps.toMutableList().apply {
                        if (index < size) {
                            set(index, jobStep)
                        }
                    }
                    val updatedContent = currentContent.copy(jobSteps = updatedSteps)
                    it.copy(
                        userModifiedContent = updatedContent,
                        hasUnsavedChanges = true
                    )
                } else {
                    it
                }
            }
        }
    }

    fun saveSignature(signatureData: SignatureData) {
        _documentState.update { state ->
            state?.copy(
                ptp = state.ptp.copy(signatureSupervisor = signatureData),
                hasSignature = true,
                hasUnsavedChanges = true
            )
        }
    }

    fun savePTP() {
        val docState = _documentState.value ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val content = docState.userModifiedContent ?: docState.aiGeneratedContent
                if (content != null) {
                    ptpRepository.updatePtpContent(docState.ptp.id, content)
                        .onSuccess {
                            _documentState.update { it?.copy(hasUnsavedChanges = false) }
                            _uiState.update { it.copy(isSaving = false) }
                        }
                        .onFailure { error ->
                            _uiState.update {
                                it.copy(
                                    isSaving = false,
                                    error = error.message ?: "Failed to save PTP"
                                )
                            }
                        }
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = "No content to save"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }

    fun exportPDF(
        companyName: String = "HazardHawk",
        projectName: String? = null,
        projectLocation: String = "Project Site",
        onComplete: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val docState = _documentState.value ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            try {
                val ptp = docState.ptp

                // Load photos for the PTP
                val photos = loadPhotosForPtp(ptp.id)

                // Create metadata
                val metadata = PDFMetadata(
                    companyName = companyName,
                    projectName = projectName ?: ptp.projectId ?: "Unknown Project",
                    projectLocation = projectLocation
                )

                // Generate PDF
                pdfGenerator.generatePDFWithMetadata(ptp, photos, metadata)
                    .onSuccess { pdfBytes ->
                        // Save to local storage
                        val filePath = withContext(Dispatchers.IO) {
                            fileStorageUtil.savePdfToStorage(pdfBytes, ptp.id)
                        }

                        // Update database with PDF path
                        ptpRepository.updatePtpPdfPaths(ptp.id, filePath, null)
                            .onSuccess {
                                _documentState.update { state ->
                                    state?.copy(
                                        ptp = ptp.copy(pdfPath = filePath)
                                    )
                                }
                                _uiState.update { it.copy(isExporting = false) }
                                onComplete(filePath)
                            }
                            .onFailure { error ->
                                _uiState.update {
                                    it.copy(
                                        isExporting = false,
                                        error = error.message
                                    )
                                }
                                onError(error.message ?: "Failed to update PDF path")
                            }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isExporting = false,
                                error = error.message
                            )
                        }
                        onError(error.message ?: "PDF generation failed")
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        error = e.message ?: "Unknown error occurred"
                    )
                }
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Load photos associated with a PTP.
     * This is a placeholder - implement based on your photo repository.
     */
    private suspend fun loadPhotosForPtp(ptpId: String): List<PhotoData> {
        // TODO: Implement photo loading from repository
        // For now, return empty list
        return ptpRepository.getPhotosForPtp(ptpId)
            .map { ptpPhotos ->
                // Convert PtpPhoto to PhotoData
                // This requires loading actual photo bytes from storage
                emptyList<PhotoData>()
            }
            .getOrDefault(emptyList())
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// ===== State Classes =====

/**
 * Questionnaire state for PTP creation
 */
data class QuestionnaireState(
    val workType: String = "",
    val taskDescription: String = "",
    val toolsEquipment: String = "",
    val workingAtHeight: Boolean = false,
    val maximumHeight: Int? = null,
    val crewSize: Int? = null,
    val specificTasks: List<String> = emptyList(),
    val mechanicalEquipment: List<String> = emptyList(),
    val environmentalConditions: List<String> = emptyList(),
    val materialsInvolved: List<String> = emptyList(),
    val nearPowerLines: Boolean = false,
    val confinedSpace: Boolean = false,
    val hazardousMaterials: Boolean = false,
    val additionalNotes: String? = null
)

/**
 * AI generation state
 */
sealed class GenerationState {
    object Idle : GenerationState()
    data class Generating(val progress: Float) : GenerationState()
    data class Success(val ptpId: String) : GenerationState()
    data class Error(val message: String) : GenerationState()
}

/**
 * Document editing state
 */
data class DocumentState(
    val ptp: PreTaskPlan,
    val aiGeneratedContent: PtpContent?,
    val userModifiedContent: PtpContent?,
    val hasUnsavedChanges: Boolean = false,
    val hasSignature: Boolean = false,
    val aiConfidence: Double = 0.0,
    val aiWarnings: List<String> = emptyList()
)

/**
 * UI state for loading, saving, exporting
 */
data class PTPUIState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val error: String? = null
)
