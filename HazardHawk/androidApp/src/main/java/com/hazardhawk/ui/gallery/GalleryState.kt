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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
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
        if (selectedIds.isEmpty()) {
            Log.w("GalleryViewModel", "⚠️ No photos selected for deletion")
            return
        }

        val photosToDelete = _state.value.photos.filter { selectedIds.contains(it.id) }
        Log.d("GalleryViewModel", "🗑️ Starting deletion of ${photosToDelete.size} photos")

        viewModelScope.launch {
            try {
                // Create deleted photo group for undo functionality
                val deletedGroup = DeletedPhotoGroup(photosToDelete)

                // Update state immediately (optimistic update)
                _state.update { currentState ->
                    currentState.copy(
                        photos = currentState.photos.filterNot { selectedIds.contains(it.id) },
                        selectedPhotos = emptySet(),
                        isSelectionMode = false, // Exit selection mode after deletion
                        recentlyDeleted = currentState.recentlyDeleted + deletedGroup,
                        showUndoSnackbar = true,
                        undoMessage = "${photosToDelete.size} photo${if (photosToDelete.size > 1) "s" else ""} deleted",
                        error = null // Clear any previous errors
                    )
                }

                Log.d("GalleryViewModel", "✅ Optimistic UI update completed")

                // Perform actual deletion in parallel for better performance
                val deleteJobs = photosToDelete.map { photo ->
                    async(Dispatchers.IO) {
                        val result = photoRepository.deletePhoto(photo.id)
                        if (result.isFailure) {
                            Log.e("GalleryViewModel", "❌ Failed to delete photo: ${photo.fileName} - ${result.exceptionOrNull()?.message}")
                        } else {
                            Log.d("GalleryViewModel", "✅ Successfully deleted photo: ${photo.fileName}")
                        }
                        result
                    }
                }

                // Wait for all deletions to complete
                val results = deleteJobs.awaitAll()
                val failedDeletions = results.count { it.isFailure }
                val successfulDeletions = results.count { it.isSuccess }

                Log.d("GalleryViewModel", "🗑️ Deletion results: $successfulDeletions successful, $failedDeletions failed")

                if (failedDeletions > 0) {
                    // Some deletions failed - show error and potentially rollback
                    val errorMessage = "Failed to delete $failedDeletions photo${if (failedDeletions > 1) "s" else ""}"

                    _state.update { currentState ->
                        currentState.copy(
                            error = errorMessage,
                            undoMessage = "$successfulDeletions photo${if (successfulDeletions > 1) "s" else ""} deleted",
                            selectedPhotos = emptySet(), // Ensure selection is cleared even on partial failure
                            isSelectionMode = false // Ensure selection mode is exited even on partial failure
                        )
                    }
                } else {
                    // All deletions successful
                    Log.d("GalleryViewModel", "🎉 All photos deleted successfully")
                }

                // Schedule cleanup of undo data after timeout
                kotlinx.coroutines.delay(UNDO_TIMEOUT_MS)

                // Check if this group hasn't been restored
                val currentState = _state.value
                val stillDeleted = currentState.recentlyDeleted.contains(deletedGroup)

                if (stillDeleted) {
                    // Remove from undo list (actual deletion already completed above)
                    _state.update { state ->
                        state.copy(
                            recentlyDeleted = state.recentlyDeleted - deletedGroup,
                            showUndoSnackbar = false,
                            undoMessage = null
                        )
                    }
                    Log.d("GalleryViewModel", "🧹 Cleanup completed - removed from undo list")
                }

            } catch (e: Exception) {
                Log.e("GalleryViewModel", "❌ Exception during photo deletion", e)

                // Rollback on error
                _state.update { currentState ->
                    currentState.copy(
                        photos = (currentState.photos + photosToDelete).sortedByDescending { it.timestamp },
                        selectedPhotos = emptySet(), // Don't restore selection to avoid confusion
                        isSelectionMode = false,
                        recentlyDeleted = currentState.recentlyDeleted.filterNot { it.photos == photosToDelete },
                        showUndoSnackbar = false,
                        undoMessage = null,
                        error = "Deletion failed: ${e.message}"
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