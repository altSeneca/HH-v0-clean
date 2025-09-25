package com.hazardhawk.ui.gallery

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.domain.entities.Photo
import com.hazardhawk.domain.repositories.PhotoRepository
import com.hazardhawk.reports.ReportGenerationManager
import com.hazardhawk.models.*
import com.hazardhawk.models.ReportGenerationProgress
import android.util.Log
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File

/**
 * Simplified Gallery State Management
 * Centralized state for the gallery screen with basic CRUD operations
 */

@Stable
data class GalleryState(
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedPhotos: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val currentPhotoIndex: Int = 0,
    val showPhotoViewer: Boolean = false,
    val recentlyDeleted: List<DeletedPhotoGroup> = emptyList(),
    val showUndoSnackbar: Boolean = false,
    val undoMessage: String? = null,
    val isGeneratingReport: Boolean = false,
    val reportGenerationProgress: Float = 0f,
    val reportGenerationMessage: String? = null
)

data class DeletedPhotoGroup(
    val photos: List<Photo>,
    val deletedAt: Long = System.currentTimeMillis()
)

class GalleryViewModel(
    private val photoRepository: PhotoRepository,
    private val reportGenerationManager: ReportGenerationManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(GalleryState())
    val state: StateFlow<GalleryState> = _state.asStateFlow()
    
    init {
        loadPhotos()
    }
    
    fun loadPhotos() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                Log.d("GalleryViewModel", "🖼️ Loading photos from repository...")
                photoRepository.getPhotos().collect { photos ->
                    Log.d("GalleryViewModel", "🖼️ Repository returned ${photos.size} photos")
                    photos.take(10).forEach { photo ->
                        Log.d("GalleryViewModel", "🖼️ Photo: ${photo.fileName}, timestamp: ${photo.timestamp}, date: ${java.util.Date(photo.timestamp)}")
                    }

                    _state.update {
                        it.copy(
                            photos = photos,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("GalleryViewModel", "🖼️ Error loading photos: ${e.message}")
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load photos: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun selectPhoto(photoId: String) {
        _state.update { currentState ->
            val newSelection = if (currentState.selectedPhotos.contains(photoId)) {
                currentState.selectedPhotos - photoId
            } else {
                currentState.selectedPhotos + photoId
            }
            
            currentState.copy(
                selectedPhotos = newSelection,
                isSelectionMode = newSelection.isNotEmpty()
            )
        }
    }
    
    fun clearSelection() {
        _state.update { it.copy(selectedPhotos = emptySet(), isSelectionMode = false) }
    }
    
    fun selectAll() {
        _state.update { currentState ->
            val allPhotoIds = currentState.photos.map { it.id }.toSet()
            currentState.copy(
                selectedPhotos = allPhotoIds,
                isSelectionMode = true
            )
        }
    }
    
    fun deleteSelectedPhotos() {
        val selectedIds = _state.value.selectedPhotos
        if (selectedIds.isEmpty()) return
        
        val photosToDelete = _state.value.photos.filter { selectedIds.contains(it.id) }
        
        viewModelScope.launch {
            try {
                // Create deleted photo group for undo functionality
                val deletedGroup = DeletedPhotoGroup(photosToDelete)
                
                // Update state immediately (optimistic update)
                _state.update { currentState ->
                    currentState.copy(
                        photos = currentState.photos.filterNot { selectedIds.contains(it.id) },
                        selectedPhotos = emptySet(),
                        isSelectionMode = true, // Keep selection mode active so user can select more photos
                        recentlyDeleted = currentState.recentlyDeleted + deletedGroup,
                        showUndoSnackbar = true,
                        undoMessage = "${photosToDelete.size} photo${if (photosToDelete.size > 1) "s" else ""} deleted"
                    )
                }
                
                // Schedule permanent deletion after UNDO_TIMEOUT
                kotlinx.coroutines.delay(UNDO_TIMEOUT_MS)
                
                // Check if this group hasn't been restored
                val currentState = _state.value
                val stillDeleted = currentState.recentlyDeleted.contains(deletedGroup)
                
                if (stillDeleted) {
                    // Permanently delete from repository
                    photosToDelete.forEach { photo ->
                        photoRepository.deletePhoto(photo.id)
                    }
                    
                    // Remove from undo list
                    _state.update { state ->
                        state.copy(
                            recentlyDeleted = state.recentlyDeleted - deletedGroup,
                            showUndoSnackbar = false,
                            undoMessage = null
                        )
                    }
                }
                
            } catch (e: Exception) {
                // Rollback on error
                _state.update { currentState ->
                    currentState.copy(
                        photos = currentState.photos + photosToDelete,
                        selectedPhotos = selectedIds, // Restore the previous selection
                        isSelectionMode = true, // Keep selection mode active
                        showUndoSnackbar = false,
                        undoMessage = null,
                        error = "Failed to delete photos: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun undoDelete() {
        val recentlyDeleted = _state.value.recentlyDeleted.lastOrNull()
        if (recentlyDeleted != null) {
            _state.update { currentState ->
                currentState.copy(
                    photos = (currentState.photos + recentlyDeleted.photos).sortedByDescending { it.timestamp },
                    recentlyDeleted = currentState.recentlyDeleted - recentlyDeleted,
                    showUndoSnackbar = false,
                    undoMessage = null
                )
            }
        }
    }
    
    fun dismissUndo() {
        _state.update { it.copy(showUndoSnackbar = false, undoMessage = null) }
    }
    
    fun generateReport() {
        Log.d("GalleryViewModel", "generateReport() called")
        val selectedIds = _state.value.selectedPhotos
        Log.d("GalleryViewModel", "Selected IDs: $selectedIds")
        if (selectedIds.isEmpty()) {
            Log.w("GalleryViewModel", "No photos selected, returning")
            return
        }
        
        val selectedPhotos = _state.value.photos.filter { selectedIds.contains(it.id) }
        Log.d("GalleryViewModel", "Selected photos count: ${selectedPhotos.size}")
        
        viewModelScope.launch {
            try {
                _state.update { 
                    it.copy(
                        isGeneratingReport = true,
                        reportGenerationProgress = 0f,
                        reportGenerationMessage = "Starting report generation...",
                        error = null
                    )
                }
                
                // Generate actual PDF report using ReportGenerationManager
                reportGenerationManager.generatePhotoExportPdf(
                    photos = selectedPhotos,
                    exportTitle = "HazardHawk Safety Photo Report",
                    siteInfo = null // TODO: Add site info if available
                ).collect { progress ->
                    _state.update { currentState ->
                        currentState.copy(
                            reportGenerationProgress = progress.progress,
                            reportGenerationMessage = progress.currentStep,
                            isGeneratingReport = progress.status != GenerationStatus.Completed && progress.status != GenerationStatus.Failed,
                            error = progress.errorMessage
                        )
                    }
                    
                    // Handle completion
                    if (progress.status == GenerationStatus.Completed) {
                        _state.update { currentState ->
                            currentState.copy(
                                selectedPhotos = emptySet(),
                                isSelectionMode = false,
                                isGeneratingReport = false,
                                reportGenerationProgress = 0f,
                                reportGenerationMessage = null,
                                error = null
                            )
                        }
                    }
                }
                
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        isGeneratingReport = false,
                        reportGenerationProgress = 0f,
                        reportGenerationMessage = null,
                        error = "Failed to generate report: ${e.message}"
                    )
                }
            }
        }
    }
    
    companion object {
        private const val UNDO_TIMEOUT_MS = 500L // 0.5 seconds for immediate deletion experience
    }
    
    fun showPhotoViewer(photoIndex: Int) {
        _state.update { 
            it.copy(
                currentPhotoIndex = photoIndex,
                showPhotoViewer = true
            )
        }
    }
    
    fun hidePhotoViewer() {
        _state.update { it.copy(showPhotoViewer = false) }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun updatePhotoTags(photoId: String, tags: List<String>) {
        viewModelScope.launch {
            try {
                val result = photoRepository.updatePhotoTags(photoId, tags)
                if (result.isSuccess) {
                    // Reload photos to reflect the tag changes
                    loadPhotos()
                } else {
                    _state.update { 
                        it.copy(error = "Failed to update photo tags: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(error = "Failed to update photo tags: ${e.message}")
                }
            }
        }
    }
}

/**
 * Composable function to remember gallery state
 */
@Composable
fun rememberGalleryState(
    photoRepository: PhotoRepository,
    reportGenerationManager: ReportGenerationManager
): GalleryViewModel {
    return remember {
        GalleryViewModel(photoRepository, reportGenerationManager)
    }
}