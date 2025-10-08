package com.hazardhawk.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.domain.repositories.ProjectRepository
import com.hazardhawk.models.crew.CompanyWorker
import com.hazardhawk.models.crew.Project
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * ViewModel for Project management screens
 *
 * Manages project CRUD operations, form validation, and worker selection.
 * Integrates with ProjectRepository for data operations.
 *
 * Features:
 * - Project list with status filtering
 * - Project create/edit with comprehensive validation
 * - Auto-populate company information
 * - Worker selection for project managers and superintendents
 * - Form state management with validation
 */
class ProjectViewModel(
    private val projectRepository: ProjectRepository,
    private val companyId: String = "default-company-id" // TODO: Get from auth/session
) : ViewModel() {

    // ===== Project List State =====

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>(null)
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    // ===== Project Form State =====

    private val _formState = MutableStateFlow(ProjectFormState())
    val formState: StateFlow<ProjectFormState> = _formState.asStateFlow()

    private val _validationErrors = MutableStateFlow<Map<String, String>>(emptyMap())
    val validationErrors: StateFlow<Map<String, String>> = _validationErrors.asStateFlow()

    private val _availableWorkers = MutableStateFlow<List<CompanyWorker>>(emptyList())
    val availableWorkers: StateFlow<List<CompanyWorker>> = _availableWorkers.asStateFlow()

    // ===== UI State =====

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadProjects()
        loadAvailableWorkers()
    }

    // ===== Project List Operations =====

    /**
     * Load all projects for the company
     */
    private fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val status = _selectedStatus.value
                val result = projectRepository.getProjects(
                    companyId = companyId,
                    status = status?.let { com.hazardhawk.models.crew.ProjectStatus.valueOf(it.uppercase()) }
                )
                _projects.value = result.data
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load projects: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh project list
     */
    fun refreshProjects() {
        loadProjects()
    }

    /**
     * Filter projects by status
     */
    fun filterByStatus(status: String?) {
        _selectedStatus.value = status
        loadProjects()
    }

    // ===== Project Form Operations =====

    /**
     * Initialize form for new project
     */
    fun initializeNewProject() {
        _formState.value = ProjectFormState()
        _validationErrors.value = emptyMap()
    }

    /**
     * Load existing project for editing
     */
    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val project = projectRepository.getProject(
                    projectId = projectId,
                    includeCompany = true,
                    includeManagers = true
                )

                if (project != null) {
                    _formState.value = ProjectFormState(
                        id = project.id,
                        name = project.name,
                        projectNumber = project.projectNumber ?: "",
                        startDate = project.startDate,
                        endDate = project.endDate,
                        clientName = project.clientName ?: "",
                        clientContact = project.clientContact ?: "",
                        clientPhone = project.clientPhone ?: "",
                        clientEmail = project.clientEmail ?: "",
                        streetAddress = project.streetAddress ?: "",
                        city = project.city ?: "",
                        state = project.state ?: "",
                        zip = project.zip ?: "",
                        generalContractor = project.generalContractor ?: "",
                        projectManagerId = project.projectManagerId,
                        superintendentId = project.superintendentId,
                        projectManagerName = project.projectManager?.workerProfile?.fullName,
                        superintendentName = project.superintendent?.workerProfile?.fullName
                    )
                } else {
                    _errorMessage.value = "Project not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update a form field
     */
    fun updateFormField(fieldName: String, value: Any?) {
        _formState.update { currentState ->
            when (fieldName) {
                "name" -> currentState.copy(name = value as String)
                "projectNumber" -> currentState.copy(projectNumber = value as String)
                "startDate" -> currentState.copy(startDate = value as LocalDate?)
                "endDate" -> currentState.copy(endDate = value as LocalDate?)
                "clientName" -> currentState.copy(clientName = value as String)
                "clientContact" -> currentState.copy(clientContact = value as String)
                "clientPhone" -> currentState.copy(clientPhone = value as String)
                "clientEmail" -> currentState.copy(clientEmail = value as String)
                "streetAddress" -> currentState.copy(streetAddress = value as String)
                "city" -> currentState.copy(city = value as String)
                "state" -> currentState.copy(state = value as String)
                "zip" -> currentState.copy(zip = value as String)
                "generalContractor" -> currentState.copy(generalContractor = value as String)
                "projectManagerId" -> {
                    val workerId = value as String?
                    val workerName = _availableWorkers.value
                        .find { it.id == workerId }
                        ?.workerProfile?.fullName
                    currentState.copy(
                        projectManagerId = workerId,
                        projectManagerName = workerName
                    )
                }
                "superintendentId" -> {
                    val workerId = value as String?
                    val workerName = _availableWorkers.value
                        .find { it.id == workerId }
                        ?.workerProfile?.fullName
                    currentState.copy(
                        superintendentId = workerId,
                        superintendentName = workerName
                    )
                }
                else -> currentState
            }
        }

        // Clear validation error for this field
        _validationErrors.update { errors ->
            errors.filterKeys { it != fieldName }
        }
    }

    /**
     * Validate form and return true if valid
     */
    private fun validateForm(): Boolean {
        val errors = mutableMapOf<String, String>()
        val state = _formState.value

        // Required fields
        if (state.name.isBlank()) {
            errors["name"] = "Project name is required"
        }

        if (state.startDate == null) {
            errors["startDate"] = "Start date is required"
        }

        // Date validation
        if (state.startDate != null && state.endDate != null) {
            if (state.endDate < state.startDate) {
                errors["endDate"] = "End date must be after start date"
            }
        }

        // Email validation (if provided)
        if (state.clientEmail.isNotBlank() && !isValidEmail(state.clientEmail)) {
            errors["clientEmail"] = "Invalid email format"
        }

        // ZIP code validation (if provided)
        if (state.zip.isNotBlank() && !isValidZip(state.zip)) {
            errors["zip"] = "Invalid ZIP code format"
        }

        _validationErrors.value = errors
        return errors.isEmpty()
    }

    /**
     * Save project (create or update)
     */
    fun saveProject(onSuccess: () -> Unit) {
        if (!validateForm()) {
            _errorMessage.value = "Please fix validation errors"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val state = _formState.value
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

                if (state.id != null) {
                    // Update existing project
                    val updateRequest = com.hazardhawk.domain.repositories.UpdateProjectRequest(
                        name = state.name,
                        projectNumber = state.projectNumber.takeIf { it.isNotBlank() },
                        startDate = state.startDate,
                        endDate = state.endDate,
                        clientName = state.clientName.takeIf { it.isNotBlank() },
                        clientContact = state.clientContact.takeIf { it.isNotBlank() },
                        clientPhone = state.clientPhone.takeIf { it.isNotBlank() },
                        clientEmail = state.clientEmail.takeIf { it.isNotBlank() },
                        streetAddress = state.streetAddress.takeIf { it.isNotBlank() },
                        city = state.city.takeIf { it.isNotBlank() },
                        state = state.state.takeIf { it.isNotBlank() },
                        zip = state.zip.takeIf { it.isNotBlank() },
                        generalContractor = state.generalContractor.takeIf { it.isNotBlank() },
                        projectManagerId = state.projectManagerId,
                        superintendentId = state.superintendentId
                    )

                    projectRepository.updateProject(state.id, updateRequest)
                        .onSuccess {
                            onSuccess()
                        }
                        .onFailure { e ->
                            _errorMessage.value = "Failed to update project: ${e.message}"
                        }
                } else {
                    // Create new project
                    projectRepository.createProject(
                        companyId = companyId,
                        name = state.name,
                        projectNumber = state.projectNumber.takeIf { it.isNotBlank() },
                        startDate = state.startDate ?: today,
                        endDate = state.endDate,
                        clientName = state.clientName.takeIf { it.isNotBlank() },
                        streetAddress = state.streetAddress.takeIf { it.isNotBlank() },
                        city = state.city.takeIf { it.isNotBlank() },
                        state = state.state.takeIf { it.isNotBlank() },
                        zip = state.zip.takeIf { it.isNotBlank() },
                        generalContractor = state.generalContractor.takeIf { it.isNotBlank() },
                        projectManagerId = state.projectManagerId,
                        superintendentId = state.superintendentId
                    ).onSuccess {
                        onSuccess()
                    }.onFailure { e ->
                        _errorMessage.value = "Failed to create project: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving project: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Load available workers for project manager/superintendent selection
     */
    private fun loadAvailableWorkers() {
        viewModelScope.launch {
            try {
                // TODO: Fetch from WorkerRepository when available
                // For now, using empty list - will be populated when crew management is integrated
                _availableWorkers.value = emptyList()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load workers: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ===== Validation Helpers =====

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidZip(zip: String): Boolean {
        // US ZIP code: 5 digits or 5+4 format
        return zip.matches(Regex("^\\d{5}(-\\d{4})?$"))
    }
}

/**
 * Project Form State
 * Holds all form field values for create/edit operations
 */
data class ProjectFormState(
    val id: String? = null,
    val name: String = "",
    val projectNumber: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val clientName: String = "",
    val clientContact: String = "",
    val clientPhone: String = "",
    val clientEmail: String = "",
    val streetAddress: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val generalContractor: String = "",
    val projectManagerId: String? = null,
    val superintendentId: String? = null,
    val projectManagerName: String? = null,
    val superintendentName: String? = null
)
