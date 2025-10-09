package com.hazardhawk.ui.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.data.repositories.dashboard.ActivityRepositoryImpl
import com.hazardhawk.models.dashboard.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Activity Feed component.
 * Manages activity feed state, pull-to-refresh, and feed item actions.
 *
 * Injected Dependencies:
 * - ActivityRepositoryImpl: Provides activity feed data and actions
 *
 * StateFlows:
 * - activities: List of activity feed items (PTPs, hazards, photos, alerts)
 * - isRefreshing: Loading state for pull-to-refresh
 * - unreadCount: Number of items requiring user attention
 * - filterState: Current filter settings (show resolved, activity types, etc.)
 * - errorMessage: Error messages for user feedback
 */
class ActivityFeedViewModel(
    private val activityRepository: ActivityRepositoryImpl
) : ViewModel() {

    // Activity Feed State
    private val _activities = MutableStateFlow<List<ActivityFeedItem>>(emptyList())
    val activities: StateFlow<List<ActivityFeedItem>> = _activities.asStateFlow()

    // UI State
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Unread/Pending Count
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // Filter State
    private val _filterState = MutableStateFlow(ActivityFilterState())
    val filterState: StateFlow<ActivityFilterState> = _filterState.asStateFlow()

    // Error State
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Loading State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Load initial activity feed
        loadActivityFeed()

        // Observe filter changes and reload
        viewModelScope.launch {
            filterState.collect { filter ->
                loadActivityFeed()
            }
        }
    }

    /**
     * Load activity feed with current filter settings
     */
    private fun loadActivityFeed() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val filter = _filterState.value
                activityRepository.getActivityFeed(
                    limit = filter.limit,
                    includeResolved = filter.includeResolved
                ).collect { items ->
                    // Apply additional filters
                    val filteredItems = applyFilters(items, filter)
                    _activities.value = filteredItems

                    // Update unread count
                    updateUnreadCount()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load activity feed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refresh activity feed
     * Called by pull-to-refresh gesture
     */
    fun refreshFeed() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            try {
                // Trigger repository refresh
                val result = activityRepository.refreshActivities()
                result.onSuccess {
                    // Reload feed after refresh
                    loadActivityFeed()
                }.onFailure { e ->
                    _errorMessage.value = "Failed to refresh: ${e.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error refreshing feed: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Handle action click on an activity feed item
     *
     * @param item The activity feed item
     * @param actionType The type of action to perform
     */
    fun onActionClick(item: ActivityFeedItem, actionType: FeedActionType) {
        viewModelScope.launch {
            _errorMessage.value = null

            try {
                when (actionType) {
                    FeedActionType.VIEW -> handleViewAction(item)
                    FeedActionType.EDIT -> handleEditAction(item)
                    FeedActionType.SHARE -> handleShareAction(item)
                    FeedActionType.EXPORT -> handleExportAction(item)
                    FeedActionType.ANALYZE -> handleAnalyzeAction(item)
                    FeedActionType.ASSIGN -> handleAssignAction(item)
                    FeedActionType.RESOLVE -> handleResolveAction(item)
                    FeedActionType.DELETE -> handleDeleteAction(item)
                    FeedActionType.DISMISS -> handleDismissAction(item)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to perform action: ${e.message}"
            }
        }
    }

    /**
     * Update filter settings
     *
     * @param filter New filter state
     */
    fun updateFilter(filter: ActivityFilterState) {
        _filterState.value = filter
        // Filter observer in init will automatically reload feed
    }

    /**
     * Toggle show resolved items
     */
    fun toggleShowResolved() {
        _filterState.update { current ->
            current.copy(includeResolved = !current.includeResolved)
        }
    }

    /**
     * Filter by activity type
     *
     * @param types Set of activity types to show (empty = show all)
     */
    fun filterByType(types: Set<ActivityType>) {
        _filterState.update { current ->
            current.copy(activityTypes = types)
        }
    }

    /**
     * Set item limit
     *
     * @param limit Maximum number of items to show
     */
    fun setLimit(limit: Int) {
        _filterState.update { current ->
            current.copy(limit = limit)
        }
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _filterState.value = ActivityFilterState()
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * Apply filter settings to activity list
     */
    private fun applyFilters(
        items: List<ActivityFeedItem>,
        filter: ActivityFilterState
    ): List<ActivityFeedItem> {
        var filtered = items

        // Filter by activity type if specified
        if (filter.activityTypes.isNotEmpty()) {
            filtered = filtered.filter { item ->
                when (item) {
                    is ActivityFeedItem.PTPActivity -> ActivityType.PTP in filter.activityTypes
                    is ActivityFeedItem.HazardActivity -> ActivityType.HAZARD in filter.activityTypes
                    is ActivityFeedItem.PhotoActivity -> ActivityType.PHOTO in filter.activityTypes
                    is ActivityFeedItem.SystemAlert -> ActivityType.ALERT in filter.activityTypes
                    is ActivityFeedItem.ToolboxTalkActivity -> ActivityType.TOOLBOX_TALK in filter.activityTypes
                }
            }
        }

        return filtered
    }

    /**
     * Update unread count
     */
    private suspend fun updateUnreadCount() {
        try {
            val count = activityRepository.getUnreadCount()
            _unreadCount.value = count
        } catch (e: Exception) {
            // Don't show error for count update failure
        }
    }

    /**
     * Handle VIEW action
     */
    private fun handleViewAction(item: ActivityFeedItem) {
        // Navigation will be handled in UI layer
        // ViewModel just validates the action here
    }

    /**
     * Handle EDIT action
     */
    private fun handleEditAction(item: ActivityFeedItem) {
        // Navigation will be handled in UI layer
    }

    /**
     * Handle SHARE action
     */
    private fun handleShareAction(item: ActivityFeedItem) {
        // Share functionality will be handled in UI layer
    }

    /**
     * Handle EXPORT action
     */
    private fun handleExportAction(item: ActivityFeedItem) {
        // Export functionality will be handled in UI layer
    }

    /**
     * Handle ANALYZE action
     */
    private suspend fun handleAnalyzeAction(item: ActivityFeedItem) {
        if (item is ActivityFeedItem.PhotoActivity && !item.analyzed) {
            // Trigger AI analysis
            // This would call an analysis repository
            _errorMessage.value = "AI analysis feature coming soon"
        }
    }

    /**
     * Handle ASSIGN action
     */
    private fun handleAssignAction(item: ActivityFeedItem) {
        // Assignment functionality will be handled in UI layer
    }

    /**
     * Handle RESOLVE action
     */
    private suspend fun handleResolveAction(item: ActivityFeedItem) {
        when (item) {
            is ActivityFeedItem.HazardActivity -> {
                val result = activityRepository.markHazardResolved(item.hazardId)
                result.onSuccess {
                    // Update local state
                    _activities.update { current ->
                        current.map { activity ->
                            if (activity is ActivityFeedItem.HazardActivity &&
                                activity.hazardId == item.hazardId
                            ) {
                                activity.copy(resolved = true)
                            } else {
                                activity
                            }
                        }
                    }
                    updateUnreadCount()
                }.onFailure { e ->
                    _errorMessage.value = "Failed to resolve hazard: ${e.message}"
                }
            }
            else -> {
                _errorMessage.value = "This item cannot be resolved"
            }
        }
    }

    /**
     * Handle DELETE action
     */
    private fun handleDeleteAction(item: ActivityFeedItem) {
        // Delete functionality will be handled in UI layer with confirmation dialog
    }

    /**
     * Handle DISMISS action
     */
    private suspend fun handleDismissAction(item: ActivityFeedItem) {
        if (item is ActivityFeedItem.SystemAlert) {
            val result = activityRepository.dismissAlert(item.id)
            result.onSuccess {
                // Update local state
                _activities.update { current ->
                    current.map { activity ->
                        if (activity is ActivityFeedItem.SystemAlert && activity.id == item.id) {
                            activity.copy(dismissed = true)
                        } else {
                            activity
                        }
                    }
                }
                updateUnreadCount()
            }.onFailure { e ->
                _errorMessage.value = "Failed to dismiss alert: ${e.message}"
            }
        }
    }
}

/**
 * Filter state for activity feed
 */
data class ActivityFilterState(
    val includeResolved: Boolean = false,
    val activityTypes: Set<ActivityType> = emptySet(), // Empty = show all
    val limit: Int = 20
)

/**
 * Activity type enum for filtering
 */
enum class ActivityType {
    PTP,
    HAZARD,
    PHOTO,
    ALERT,
    TOOLBOX_TALK
}
