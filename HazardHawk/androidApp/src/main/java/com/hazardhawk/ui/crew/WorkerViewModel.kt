package com.hazardhawk.ui.crew

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/**
 * ViewModel for Worker Management
 *
 * Handles:
 * - Worker list display
 * - Worker creation (multi-step form)
 * - Worker editing
 * - Photo upload
 * - Certification upload
 * - Form validation
 */
class WorkerViewModel : ViewModel() {

    // Worker List State
    private val _workers = MutableStateFlow<List<CompanyWorker>>(emptyList())
    val workers: StateFlow<List<CompanyWorker>> = _workers.asStateFlow()

    private val _isLoadingWorkers = MutableStateFlow(false)
    val isLoadingWorkers: StateFlow<Boolean> = _isLoadingWorkers.asStateFlow()

    // Add Worker Form State
    private val _addWorkerState = MutableStateFlow(AddWorkerFormState())
    val addWorkerState: StateFlow<AddWorkerFormState> = _addWorkerState.asStateFlow()

    // Current Worker Details (for view/edit)
    private val _selectedWorker = MutableStateFlow<CompanyWorker?>(null)
    val selectedWorker: StateFlow<CompanyWorker?> = _selectedWorker.asStateFlow()

    // UI State
    private val _uiState = MutableStateFlow(WorkerUIState())
    val uiState: StateFlow<WorkerUIState> = _uiState.asStateFlow()

    // Form Validation Errors
    private val _validationErrors = MutableStateFlow<List<String>>(emptyList())
    val validationErrors: StateFlow<List<String>> = _validationErrors.asStateFlow()

    init {
        // Validate form whenever state changes
        viewModelScope.launch {
            addWorkerState.collect { state ->
                validateAddWorkerForm(state)
            }
        }
    }

    // ===== Worker List Methods =====

    fun loadWorkers(companyId: String) {
        viewModelScope.launch {
            _isLoadingWorkers.value = true
            _uiState.update { it.copy(error = null) }

            try {
                // TODO: Call repository to fetch workers
                // For now, using mock data
                val mockWorkers = listOf(
                    CompanyWorker(
                        id = "w1",
                        companyId = companyId,
                        workerProfileId = "wp1",
                        employeeNumber = "E-001",
                        role = WorkerRole.LABORER,
                        hireDate = LocalDate(2024, 1, 15),
                        status = WorkerStatus.ACTIVE,
                        hourlyRate = 25.0,
                        createdAt = "2024-01-15T08:00:00Z",
                        updatedAt = "2024-01-15T08:00:00Z",
                        workerProfile = WorkerProfile(
                            id = "wp1",
                            firstName = "John",
                            lastName = "Doe",
                            email = "john.doe@example.com",
                            phone = "+1-555-0100",
                            photoUrl = null,
                            createdAt = "2024-01-15T08:00:00Z",
                            updatedAt = "2024-01-15T08:00:00Z"
                        )
                    )
                )
                _workers.value = mockWorkers
                _isLoadingWorkers.value = false
            } catch (e: Exception) {
                _isLoadingWorkers.value = false
                _uiState.update { it.copy(error = e.message ?: "Failed to load workers") }
            }
        }
    }

    fun refreshWorkers() {
        // Reload workers with current company ID
        // TODO: Get company ID from current context
        loadWorkers("current_company_id")
    }

    // ===== Add Worker Form Methods =====

    fun updateFirstName(value: String) {
        _addWorkerState.update { it.copy(firstName = value) }
    }

    fun updateLastName(value: String) {
        _addWorkerState.update { it.copy(lastName = value) }
    }

    fun updateEmail(value: String) {
        _addWorkerState.update { it.copy(email = value) }
    }

    fun updatePhone(value: String) {
        _addWorkerState.update { it.copy(phone = value) }
    }

    fun updateRole(role: WorkerRole) {
        _addWorkerState.update { it.copy(role = role) }
    }

    fun updateHireDate(date: LocalDate) {
        _addWorkerState.update { it.copy(hireDate = date) }
    }

    fun updateHourlyRate(rate: String) {
        _addWorkerState.update { it.copy(hourlyRate = rate) }
    }

    fun updateEmployeeNumber(number: String) {
        _addWorkerState.update { it.copy(employeeNumber = number) }
    }

    fun updatePhotoUri(uri: Uri?) {
        _addWorkerState.update { it.copy(photoUri = uri) }
    }

    fun addCertificationUri(uri: Uri) {
        _addWorkerState.update {
            it.copy(certificationUris = it.certificationUris + uri)
        }
    }

    fun removeCertificationUri(uri: Uri) {
        _addWorkerState.update {
            it.copy(certificationUris = it.certificationUris - uri)
        }
    }

    fun nextStep() {
        val currentStep = _addWorkerState.value.currentStep
        if (currentStep < 2) {
            _addWorkerState.update { it.copy(currentStep = currentStep + 1) }
        }
    }

    fun previousStep() {
        val currentStep = _addWorkerState.value.currentStep
        if (currentStep > 0) {
            _addWorkerState.update { it.copy(currentStep = currentStep - 1) }
        }
    }

    fun resetForm() {
        _addWorkerState.value = AddWorkerFormState()
        _validationErrors.value = emptyList()
    }

    // ===== Form Validation =====

    private fun validateAddWorkerForm(state: AddWorkerFormState) {
        val errors = mutableListOf<String>()

        // Step 0: Basic Info Validation
        if (state.firstName.isBlank()) {
            errors.add("First name is required")
        }

        if (state.lastName.isBlank()) {
            errors.add("Last name is required")
        }

        if (state.phone.isBlank()) {
            errors.add("Phone number is required")
        } else if (!isValidPhone(state.phone)) {
            errors.add("Phone number must be in format +1-XXX-XXXX")
        }

        if (state.email.isNotBlank() && !isValidEmail(state.email)) {
            errors.add("Email address is not valid")
        }

        if (state.employeeNumber.isBlank()) {
            errors.add("Employee number is required")
        }

        _validationErrors.value = errors
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        // Basic validation for phone format
        return phone.matches(Regex("^\\+?[1-9]\\d{1,14}$")) ||
               phone.matches(Regex("^\\+1-\\d{3}-\\d{4}$"))
    }

    fun canProceedToNextStep(): Boolean {
        val state = _addWorkerState.value
        val errors = _validationErrors.value

        return when (state.currentStep) {
            0 -> {
                // Step 0: Basic info must be valid
                state.firstName.isNotBlank() &&
                state.lastName.isNotBlank() &&
                state.phone.isNotBlank() &&
                state.employeeNumber.isNotBlank() &&
                errors.isEmpty()
            }
            1 -> true // Step 1: Photo is optional
            2 -> true // Step 2: Certifications are optional
            else -> false
        }
    }

    // ===== Worker Creation =====

    fun createWorker(
        companyId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _addWorkerState.value

        // Final validation
        if (_validationErrors.value.isNotEmpty()) {
            onError("Please fix all validation errors")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                // Generate IDs
                val workerId = "w_${System.currentTimeMillis()}"
                val workerProfileId = "wp_${System.currentTimeMillis()}"
                val now = Clock.System.now().toString()

                // TODO: Upload photo to S3 if provided
                val photoUrl: String? = null // state.photoUri?.let { uploadPhoto(it) }

                // Create WorkerProfile
                val workerProfile = WorkerProfile(
                    id = workerProfileId,
                    firstName = state.firstName,
                    lastName = state.lastName,
                    email = state.email.ifBlank { null },
                    phone = state.phone,
                    photoUrl = photoUrl,
                    createdAt = now,
                    updatedAt = now
                )

                // Create CompanyWorker
                val companyWorker = CompanyWorker(
                    id = workerId,
                    companyId = companyId,
                    workerProfileId = workerProfileId,
                    employeeNumber = state.employeeNumber,
                    role = state.role,
                    hireDate = state.hireDate,
                    status = WorkerStatus.ACTIVE,
                    hourlyRate = state.hourlyRate.toDoubleOrNull(),
                    createdAt = now,
                    updatedAt = now,
                    workerProfile = workerProfile
                )

                // TODO: Call repository to save worker
                // workerRepository.createWorker(companyWorker)

                // TODO: Upload certifications if provided
                // state.certificationUris.forEach { uri ->
                //     uploadCertification(workerId, uri)
                // }

                // Update local state
                _workers.update { workers ->
                    workers + companyWorker
                }

                _uiState.update { it.copy(isSaving = false) }
                resetForm()
                onSuccess(workerId)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = e.message ?: "Failed to create worker"
                    )
                }
                onError(e.message ?: "Failed to create worker")
            }
        }
    }

    // ===== Worker Details =====

    fun loadWorkerDetails(workerId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // TODO: Call repository to fetch worker details
                val worker = _workers.value.find { it.id == workerId }

                if (worker != null) {
                    _selectedWorker.value = worker
                    _uiState.update { it.copy(isLoading = false) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Worker not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load worker"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// ===== State Classes =====

/**
 * State for Add Worker form (multi-step)
 */
data class AddWorkerFormState(
    // Step 0: Basic Info
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: WorkerRole = WorkerRole.LABORER,
    val hireDate: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val hourlyRate: String = "",
    val employeeNumber: String = "",

    // Step 1: Photo
    val photoUri: Uri? = null,

    // Step 2: Certifications
    val certificationUris: List<Uri> = emptyList(),

    // Multi-step state
    val currentStep: Int = 0
)

/**
 * UI state for loading, saving, etc.
 */
data class WorkerUIState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
)
