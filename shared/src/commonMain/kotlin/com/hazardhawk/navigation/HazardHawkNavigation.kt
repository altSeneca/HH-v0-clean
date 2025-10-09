package com.hazardhawk.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simplified navigation state matching the specified UX journey:
 * 1. Company/Project entry screen
 * 2. Camera screen with minimal, construction-friendly design
 * 3. Post-capture tagging and AI analysis screen
 * 4. Gallery with multi-select and report generation
 * 5. PDF report sharing
 */
enum class HazardHawkDestination {
    PROJECT_ENTRY,      // Company/Project setup
    CAMERA,             // Main camera screen
    POST_CAPTURE,       // Tagging and AI analysis
    GALLERY,            // Photo gallery with multi-select
    REPORT_SHARING      // PDF report generation and sharing
}

/**
 * Simplified navigation manager
 * Replaces complex navigation logic with clear, purpose-driven flow
 */
class HazardHawkNavigationManager {
    private val _currentDestination = MutableStateFlow(HazardHawkDestination.PROJECT_ENTRY)
    val currentDestination: StateFlow<HazardHawkDestination> = _currentDestination.asStateFlow()
    
    private val _navigationHistory = mutableListOf<HazardHawkDestination>()
    
    /**
     * Navigate to a specific destination
     */
    fun navigateTo(destination: HazardHawkDestination) {
        _navigationHistory.add(_currentDestination.value)
        _currentDestination.value = destination
    }
    
    /**
     * Navigate back in history
     */
    fun navigateBack(): Boolean {
        return if (_navigationHistory.isNotEmpty()) {
            _currentDestination.value = _navigationHistory.removeLastOrNull() 
                ?: HazardHawkDestination.PROJECT_ENTRY
            true
        } else {
            false
        }
    }
    
    /**
     * Reset to start of user journey
     */
    fun resetToProjectEntry() {
        _navigationHistory.clear()
        _currentDestination.value = HazardHawkDestination.PROJECT_ENTRY
    }
    
    /**
     * Get the expected flow destination after current screen
     */
    fun getNextInFlow(): HazardHawkDestination? {
        return when (_currentDestination.value) {
            HazardHawkDestination.PROJECT_ENTRY -> HazardHawkDestination.CAMERA
            HazardHawkDestination.CAMERA -> HazardHawkDestination.POST_CAPTURE
            HazardHawkDestination.POST_CAPTURE -> HazardHawkDestination.GALLERY
            HazardHawkDestination.GALLERY -> HazardHawkDestination.REPORT_SHARING
            HazardHawkDestination.REPORT_SHARING -> null // End of flow
        }
    }
    
    /**
     * Check if we can navigate back
     */
    fun canNavigateBack(): Boolean = _navigationHistory.isNotEmpty()
}

/**
 * Navigation actions for the simplified flow
 */
sealed class NavigationAction {
    object ProjectToCamera : NavigationAction()
    data class CameraToPostCapture(val photoPath: String) : NavigationAction()
    object PostCaptureToGallery : NavigationAction()
    data class GalleryToReportSharing(val selectedPhotos: List<String>) : NavigationAction()
    object BackPressed : NavigationAction()
    object ResetFlow : NavigationAction()
}
