package com.hazardhawk.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the photo gallery feature.
 * Manages photo selection, filtering, and batch operations.
 */
class GalleryViewModel(
    // Dependencies will be injected once repositories are implemented
    // private val photoRepository: PhotoRepository,
    // private val analysisRepository: AnalysisRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()
    
    private val _selectedPhotos = MutableStateFlow<Set<String>>(emptySet())
    val selectedPhotos: StateFlow<Set<String>> = _selectedPhotos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadPhotos()
    }
    
    /**
     * Load photos from repository
     */
    fun loadPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // TODO: Implement with actual repository
                // val photos = photoRepository.getAllPhotos()
                // _uiState.value = _uiState.value.copy(
                //     photos = photos,
                //     isLoading = false
                // )
                
                // Placeholder for now
                _uiState.value = _uiState.value.copy(
                    photos = emptyList(),
                    isLoading = false
                )
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle photo selection
     */
    fun togglePhotoSelection(photoId: String) {
        val currentSelection = _selectedPhotos.value.toMutableSet()
        if (currentSelection.contains(photoId)) {
            currentSelection.remove(photoId)
        } else {
            currentSelection.add(photoId)
        }
        _selectedPhotos.value = currentSelection
    }
    
    /**
     * Clear all selections
     */
    fun clearSelection() {
        _selectedPhotos.value = emptySet()
    }
    
    /**
     * Select all photos
     */
    fun selectAll() {
        val allPhotoIds = _uiState.value.photos.map { it.id }.toSet()
        _selectedPhotos.value = allPhotoIds
    }
    
    /**
     * Delete selected photos
     */
    fun deleteSelectedPhotos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val selectedIds = _selectedPhotos.value
                // TODO: Implement with actual repository
                // photoRepository.deletePhotos(selectedIds.toList())
                
                clearSelection()
                loadPhotos()
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Filter photos by criteria
     */
    fun filterPhotos(filter: GalleryFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
        loadPhotos()
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Refresh the gallery
     */
    fun refresh() {
        loadPhotos()
    }
}

/**
 * UI state for the gallery screen
 */
data class GalleryUiState(
    val photos: List<GalleryPhoto> = emptyList(),
    val isLoading: Boolean = false,
    val filter: GalleryFilter = GalleryFilter.All,
    val sortOrder: GallerySortOrder = GallerySortOrder.DateDescending
)

/**
 * Photo item for gallery display
 */
data class GalleryPhoto(
    val id: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val timestamp: Long,
    val hasAnalysis: Boolean = false,
    val tagCount: Int = 0,
    val hazardCount: Int = 0
)

/**
 * Filter options for gallery photos
 */
enum class GalleryFilter {
    All,
    Analyzed,
    Unanalyzed,
    HasHazards,
    Tagged,
    Untagged
}

/**
 * Sort order options for gallery
 */
enum class GallerySortOrder {
    DateAscending,
    DateDescending,
    NameAscending,
    NameDescending,
    HazardCount
}
