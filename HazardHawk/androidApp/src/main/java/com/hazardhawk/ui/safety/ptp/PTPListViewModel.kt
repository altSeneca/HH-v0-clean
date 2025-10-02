package com.hazardhawk.ui.safety.ptp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.data.repositories.ptp.PTPRepository
import com.hazardhawk.domain.models.ptp.PreTaskPlan
import com.hazardhawk.domain.models.ptp.PtpStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for PTP List Screen.
 * Manages loading, filtering, and deletion of Pre-Task Plans.
 */
class PTPListViewModel(
    private val ptpRepository: PTPRepository
) : ViewModel() {

    private val _ptps = MutableStateFlow<List<PreTaskPlan>>(emptyList())
    val ptps: StateFlow<List<PreTaskPlan>> = _ptps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedFilter = MutableStateFlow<PtpStatus?>(null)
    val selectedFilter: StateFlow<PtpStatus?> = _selectedFilter.asStateFlow()

    init {
        loadPTPs()
    }

    /**
     * Load all PTPs from repository
     */
    fun loadPTPs() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Get all PTPs
                val allPtpsResult = ptpRepository.getAllPtps()

                allPtpsResult
                    .onSuccess { ptpList ->
                        _ptps.value = ptpList.sortedByDescending { it.createdAt }
                        _isLoading.value = false
                    }
                    .onFailure { throwable ->
                        _error.value = throwable.message ?: "Failed to load PTPs"
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                _isLoading.value = false
            }
        }
    }

    /**
     * Filter PTPs by status
     */
    fun filterByStatus(status: PtpStatus?) {
        viewModelScope.launch {
            _selectedFilter.value = status
            _isLoading.value = true
            _error.value = null

            try {
                val result = if (status == null) {
                    ptpRepository.getAllPtps()
                } else {
                    ptpRepository.getPtpsByStatus(status)
                }

                result
                    .onSuccess { ptpList ->
                        _ptps.value = ptpList.sortedByDescending { it.createdAt }
                        _isLoading.value = false
                    }
                    .onFailure { throwable ->
                        _error.value = throwable.message ?: "Failed to filter PTPs"
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a PTP by ID
     */
    fun deletePTP(ptpId: String) {
        viewModelScope.launch {
            try {
                ptpRepository.deletePtp(ptpId)
                    .onSuccess {
                        // Remove from local list
                        _ptps.value = _ptps.value.filter { it.id != ptpId }
                    }
                    .onFailure { throwable ->
                        _error.value = throwable.message ?: "Failed to delete PTP"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            }
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
}

/**
 * Extension function to get all PTPs from repository.
 * This is a convenience method that wraps the repository's getPtpsByProject
 * or similar method to get all PTPs.
 */
private suspend fun PTPRepository.getAllPtps(): Result<List<PreTaskPlan>> {
    // For now, we'll get PTPs by status and combine them
    // In a real implementation, you might want to add a getAllPtps() method to the repository
    return try {
        val drafts = getPtpsByStatus(PtpStatus.DRAFT).getOrElse { emptyList() }
        val submitted = getPtpsByStatus(PtpStatus.SUBMITTED).getOrElse { emptyList() }
        val approved = getPtpsByStatus(PtpStatus.APPROVED).getOrElse { emptyList() }
        val archived = getPtpsByStatus(PtpStatus.ARCHIVED).getOrElse { emptyList() }

        val allPtps = drafts + submitted + approved + archived
        Result.success(allPtps)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
