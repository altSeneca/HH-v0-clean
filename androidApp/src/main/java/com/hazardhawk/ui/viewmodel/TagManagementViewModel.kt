/*
 * Copyright (c) 2025 HazardHawk Safety Platform
 *
 * ViewModel for comprehensive tag management with state management.
 * Handles tag selection, filtering, search, and bulk operations.
 */
package com.hazardhawk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hazardhawk.domain.entities.*
import com.hazardhawk.domain.repositories.*
import com.hazardhawk.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import javax.inject.Inject

data class TagManagementUiState(
    val availableTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedCategory: TagCategory? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val maxSelections: Int = Int.MAX_VALUE,
    val showOSHAIndicators: Boolean = true,
    val fieldConditions: FieldConditions = FieldConditions(
        brightnessLevel = BrightnessLevel.NORMAL,
        isWearingGloves = false,
        isEmergencyMode = false,
        noiseLevel = 0.3f,
        batteryLevel = 0.8f
    ),
    val recommendedTags: List<Tag> = emptyList(),
    val recentTags: List<Tag> = emptyList(),
    val frequentTags: List<Tag> = emptyList(),
    val oshaComplianceTags: List<Tag> = emptyList()
)

sealed class TagManagementEvent {
    object LoadTags : TagManagementEvent()
    data class UpdateSearchQuery(val query: String) : TagManagementEvent()
    data class UpdateSelectedCategory(val category: TagCategory?) : TagManagementEvent()
    data class ToggleTagSelection(val tagId: String) : TagManagementEvent()
    data class SetTagSelection(val tagIds: Set<String>) : TagManagementEvent()
    data class CreateCustomTag(val name: String, val category: TagCategory, val description: String? = null) : TagManagementEvent()
    data class ApplyTagsToPhoto(val photoId: String) : TagManagementEvent()
    data class BulkApplyTags(val photoIds: List<String>) : TagManagementEvent()
    data class UpdateFieldConditions(val conditions: FieldConditions) : TagManagementEvent()
    object ClearSelection : TagManagementEvent()
    object LoadRecommendations : TagManagementEvent()
    data class TrackTagUsage(val tagId: String, val context: String) : TagManagementEvent()
}

@HiltViewModel
class TagManagementViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val getRecommendedTagsUseCase: GetRecommendedTagsUseCase,
    private val applyTagsUseCase: ApplyTagsUseCase,
    private val trackTagUsageUseCase: TrackTagUsageUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TagManagementUiState())
    val uiState: StateFlow<TagManagementUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private var currentUserId: String = ""
    private var currentProjectId: String? = null
    
    init {
        handleEvent(TagManagementEvent.LoadTags)
    }
    
    fun handleEvent(event: TagManagementEvent) {
        when (event) {
            is TagManagementEvent.LoadTags -> loadTags()
            is TagManagementEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is TagManagementEvent.UpdateSelectedCategory -> updateSelectedCategory(event.category)
            is TagManagementEvent.ToggleTagSelection -> toggleTagSelection(event.tagId)
            is TagManagementEvent.SetTagSelection -> setTagSelection(event.tagIds)
            is TagManagementEvent.CreateCustomTag -> createCustomTag(event.name, event.category, event.description)
            is TagManagementEvent.ApplyTagsToPhoto -> applyTagsToPhoto(event.photoId)
            is TagManagementEvent.BulkApplyTags -> bulkApplyTags(event.photoIds)
            is TagManagementEvent.UpdateFieldConditions -> updateFieldConditions(event.conditions)
            is TagManagementEvent.ClearSelection -> clearSelection()
            is TagManagementEvent.LoadRecommendations -> loadRecommendations()
            is TagManagementEvent.TrackTagUsage -> trackTagUsage(event.tagId, event.context)
        }
    }
    
    private fun loadTags() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                tagRepository.getAllTags().collect { tags ->
                    _uiState.value = _uiState.value.copy(
                        availableTags = tags,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load tags: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
    
    private fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        // Debounce search to avoid excessive API calls
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Wait for user to stop typing
            performSearch(query)
        }
    }
    
    private suspend fun performSearch(query: String) {
        if (query.isEmpty()) return
        
        try {
            val searchResults = tagRepository.searchTags(
                query = query,
                limit = 50,
                includeCustom = true,
                categoryFilter = _uiState.value.selectedCategory
            )
            
            _uiState.value = _uiState.value.copy(
                availableTags = searchResults
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                error = "Search failed: ${e.message}"
            )
        }
    }
    
    fun setUserContext(userId: String, projectId: String?) {
        currentUserId = userId
        currentProjectId = projectId
        
        // Reload recommendations with new context
        handleEvent(TagManagementEvent.LoadRecommendations)
    }
    
    fun setMaxSelections(max: Int) {
        _uiState.value = _uiState.value.copy(maxSelections = max)
        
        // Trim current selection if it exceeds new max
        val currentSelection = _uiState.value.selectedTagIds
        if (currentSelection.size > max) {
            _uiState.value = _uiState.value.copy(
                selectedTagIds = currentSelection.take(max).toSet()
            )
        }
    }
    
    private fun toggleTagSelection(tagId: String) {
        val currentSelection = _uiState.value.selectedTagIds
        val maxSelections = _uiState.value.maxSelections
        
        val newSelection = if (currentSelection.contains(tagId)) {
            currentSelection - tagId
        } else if (currentSelection.size < maxSelections) {
            currentSelection + tagId
        } else {
            currentSelection // Don't add if at max
        }
        
        _uiState.value = _uiState.value.copy(selectedTagIds = newSelection)
        
        // Track usage if tag was selected
        if (!currentSelection.contains(tagId) && newSelection.contains(tagId)) {
            trackTagUsage(tagId, "manual_selection")
        }
    }
    
    private fun trackTagUsage(tagId: String, context: String) {
        viewModelScope.launch {
            try {
                trackTagUsageUseCase(
                    tagId = tagId,
                    userId = currentUserId,
                    projectId = currentProjectId,
                    context = context,
                    timestamp = Clock.System.now()
                )
            } catch (e: Exception) {
                // Log usage tracking failures silently - don't interrupt user flow
                println("Tag usage tracking failed for $tagId: ${e.message}")
            }
        }
    }
}