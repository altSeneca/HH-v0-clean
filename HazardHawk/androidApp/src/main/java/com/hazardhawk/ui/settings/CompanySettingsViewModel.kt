package com.hazardhawk.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.domain.repositories.CompanyRepository
import com.hazardhawk.models.crew.Company
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for Company Settings screen.
 * Manages centralized company information that is reused across all safety documents.
 *
 * Features:
 * - Edit company name, address, phone
 * - Upload/delete company logo
 * - Form validation
 * - Real-time state updates
 */
class CompanySettingsViewModel(
    private val companyRepository: CompanyRepository,
    private val currentCompanyId: String = "default-company" // TODO: Get from session/auth
) : ViewModel() {

    // Company State
    private val _company = MutableStateFlow<Company?>(null)
    val company: StateFlow<Company?> = _company.asStateFlow()

    // Form State
    private val _companyName = MutableStateFlow("")
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _city = MutableStateFlow("")
    val city: StateFlow<String> = _city.asStateFlow()

    private val _state = MutableStateFlow("")
    val state: StateFlow<String> = _state.asStateFlow()

    private val _zip = MutableStateFlow("")
    val zip: StateFlow<String> = _zip.asStateFlow()

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _logoUrl = MutableStateFlow<String?>(null)
    val logoUrl: StateFlow<String?> = _logoUrl.asStateFlow()

    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Validation State
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()

    private val _phoneError = MutableStateFlow<String?>(null)
    val phoneError: StateFlow<String?> = _phoneError.asStateFlow()

    private val _zipError = MutableStateFlow<String?>(null)
    val zipError: StateFlow<String?> = _zipError.asStateFlow()

    // Derived State
    val hasChanges: StateFlow<Boolean> = combine(
        _company,
        _companyName,
        _address,
        _city,
        _state,
        _zip,
        _phone
    ) { company, name, address, city, state, zip, phone ->
        company?.let {
            name != it.name ||
            address != (it.address ?: "") ||
            city != (it.city ?: "") ||
            state != (it.state ?: "") ||
            zip != (it.zip ?: "") ||
            phone != (it.phone ?: "")
        } ?: false
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isFormValid: StateFlow<Boolean> = combine(
        _nameError,
        _phoneError,
        _zipError,
        _companyName
    ) { nameError, phoneError, zipError, name ->
        nameError == null && phoneError == null && zipError == null && name.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        loadCompany()
    }

    /**
     * Load company information
     */
    private fun loadCompany() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val company = companyRepository.getCompany(currentCompanyId)
                if (company != null) {
                    _company.value = company
                    populateForm(company)
                } else {
                    _errorMessage.value = "Company not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load company: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Populate form fields from company data
     */
    private fun populateForm(company: Company) {
        _companyName.value = company.name
        _address.value = company.address ?: ""
        _city.value = company.city ?: ""
        _state.value = company.state ?: ""
        _zip.value = company.zip ?: ""
        _phone.value = company.phone ?: ""
        _logoUrl.value = company.logoUrl
    }

    /**
     * Update company name
     */
    fun updateCompanyName(name: String) {
        _companyName.value = name
        validateCompanyName(name)
    }

    /**
     * Update address
     */
    fun updateAddress(address: String) {
        _address.value = address
    }

    /**
     * Update city
     */
    fun updateCity(city: String) {
        _city.value = city
    }

    /**
     * Update state
     */
    fun updateState(state: String) {
        _state.value = state
    }

    /**
     * Update zip code
     */
    fun updateZip(zip: String) {
        _zip.value = zip
        validateZipCode(zip)
    }

    /**
     * Update phone number
     */
    fun updatePhone(phone: String) {
        _phone.value = phone
        validatePhoneNumber(phone)
    }

    /**
     * Save company changes
     */
    fun saveCompany() {
        if (!isFormValid.value) {
            _errorMessage.value = "Please fix validation errors before saving"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val result = companyRepository.updateCompany(
                    companyId = currentCompanyId,
                    name = _companyName.value,
                    address = _address.value.takeIf { it.isNotBlank() },
                    city = _city.value.takeIf { it.isNotBlank() },
                    state = _state.value.takeIf { it.isNotBlank() },
                    zip = _zip.value.takeIf { it.isNotBlank() },
                    phone = _phone.value.takeIf { it.isNotBlank() }
                )

                result.onSuccess { updatedCompany ->
                    _company.value = updatedCompany
                    _successMessage.value = "Company information saved successfully"
                }.onFailure { e ->
                    _errorMessage.value = "Failed to save company: ${e.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error saving company: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Upload company logo
     */
    fun uploadLogo(logoData: ByteArray, fileName: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val result = companyRepository.uploadCompanyLogo(
                    companyId = currentCompanyId,
                    logoData = logoData,
                    fileName = fileName
                )

                result.onSuccess { logoUrl ->
                    _logoUrl.value = logoUrl
                    _successMessage.value = "Logo uploaded successfully"
                    // Reload company to get updated data
                    loadCompany()
                }.onFailure { e ->
                    _errorMessage.value = "Failed to upload logo: ${e.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error uploading logo: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Delete company logo
     */
    fun deleteLogo() {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val result = companyRepository.deleteCompanyLogo(currentCompanyId)

                result.onSuccess {
                    _logoUrl.value = null
                    _successMessage.value = "Logo deleted successfully"
                    // Reload company to get updated data
                    loadCompany()
                }.onFailure { e ->
                    _errorMessage.value = "Failed to delete logo: ${e.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting logo: ${e.message}"
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Reset form to original values
     */
    fun resetForm() {
        _company.value?.let { company ->
            populateForm(company)
            clearErrors()
            _errorMessage.value = null
            _successMessage.value = null
        }
    }

    /**
     * Clear all error messages
     */
    fun clearErrors() {
        _nameError.value = null
        _phoneError.value = null
        _zipError.value = null
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // ===== Validation Methods =====

    /**
     * Validate company name
     */
    private fun validateCompanyName(name: String) {
        _nameError.value = when {
            name.isBlank() -> "Company name is required"
            name.length < 2 -> "Company name must be at least 2 characters"
            name.length > 255 -> "Company name must be less than 255 characters"
            else -> null
        }
    }

    /**
     * Validate phone number
     */
    private fun validatePhoneNumber(phone: String) {
        if (phone.isBlank()) {
            _phoneError.value = null
            return
        }

        // Simple US phone number validation (10 digits)
        val digits = phone.filter { it.isDigit() }
        _phoneError.value = when {
            digits.length != 10 -> "Phone number must be 10 digits"
            else -> null
        }
    }

    /**
     * Validate zip code
     */
    private fun validateZipCode(zip: String) {
        if (zip.isBlank()) {
            _zipError.value = null
            return
        }

        // Simple US zip code validation (5 or 9 digits)
        val digits = zip.filter { it.isDigit() }
        _zipError.value = when {
            digits.length != 5 && digits.length != 9 -> "Zip code must be 5 or 9 digits"
            else -> null
        }
    }

    /**
     * Format phone number for display
     */
    fun formatPhoneNumber(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when (digits.length) {
            10 -> "(${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6)}"
            else -> phone
        }
    }
}
