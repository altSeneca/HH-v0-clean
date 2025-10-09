package com.hazardhawk.ui.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.data.repositories.dashboard.ActivityRepositoryImpl
import com.hazardhawk.data.repositories.dashboard.UserProfileRepositoryImpl
import com.hazardhawk.data.repositories.dashboard.UserProfile
import com.hazardhawk.data.repositories.dashboard.WeatherRepositoryImpl
import com.hazardhawk.models.dashboard.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the HazardHawk home dashboard.
 * Manages overall dashboard state including user profile, site conditions, and notifications.
 *
 * Injected Dependencies:
 * - ActivityRepositoryImpl: Provides activity feed data
 * - UserProfileRepositoryImpl: Manages user profile and permissions
 * - WeatherRepositoryImpl: Provides weather and site conditions
 *
 * StateFlows:
 * - userProfile: Current user profile information
 * - siteConditions: Real-time site conditions (weather, crew, shift)
 * - notifications: System notifications and alerts
 * - commandCenterButtons: Available safety actions based on user tier
 * - isRefreshing: Loading state for pull-to-refresh
 * - errorMessage: Error messages for user feedback
 */
class DashboardViewModel(
    private val activityRepository: ActivityRepositoryImpl,
    private val userProfileRepository: UserProfileRepositoryImpl,
    private val weatherRepository: WeatherRepositoryImpl
) : ViewModel() {

    // User Profile State
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Site Conditions State
    private val _siteConditions = MutableStateFlow<SiteConditions?>(null)
    val siteConditions: StateFlow<SiteConditions?> = _siteConditions.asStateFlow()

    // Notifications State
    private val _notifications = MutableStateFlow<List<ActivityFeedItem.SystemAlert>>(emptyList())
    val notifications: StateFlow<List<ActivityFeedItem.SystemAlert>> = _notifications.asStateFlow()

    // Command Center Buttons State
    private val _commandCenterButtons = MutableStateFlow<List<CommandCenterButton>>(emptyList())
    val commandCenterButtons: StateFlow<List<CommandCenterButton>> = _commandCenterButtons.asStateFlow()

    // UI State
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Unread notification count
    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    init {
        // Load initial data
        loadDashboardData()

        // Observe user profile changes
        viewModelScope.launch {
            userProfileRepository.getCurrentUserFlow().collect { profile ->
                _userProfile.value = profile
                updateCommandCenterButtons(profile.userTier)
            }
        }

        // Observe weather changes
        viewModelScope.launch {
            weatherRepository.currentWeather.collect { weather ->
                _siteConditions.update { current ->
                    current?.copy(weather = weather) ?: SiteConditions(weather = weather)
                }
            }
        }
    }

    /**
     * Load all dashboard data
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                // Load user profile
                val profile = userProfileRepository.getCurrentUser()
                _userProfile.value = profile

                // Load site conditions
                loadSiteConditions()

                // Load notifications
                loadNotifications()

                // Update command center buttons based on user tier
                updateCommandCenterButtons(profile.userTier)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load dashboard: ${e.message}"
            }
        }
    }

    /**
     * Refresh all dashboard data
     * Called by pull-to-refresh gesture
     */
    fun refreshData() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null

            try {
                // Refresh activities
                activityRepository.refreshActivities()

                // Refresh user profile
                val profile = userProfileRepository.getCurrentUser()
                _userProfile.value = profile

                // Refresh site conditions
                loadSiteConditions()

                // Refresh notifications
                loadNotifications()

                // Update command center buttons
                updateCommandCenterButtons(profile.userTier)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh dashboard: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Load site conditions (weather, crew, shift)
     */
    private suspend fun loadSiteConditions() {
        try {
            // In a real implementation, get location from LocationService
            val latitude = 34.0522  // Mock location (Los Angeles)
            val longitude = -118.2437

            val result = weatherRepository.getSiteConditions(latitude, longitude)
            result.onSuccess { conditions ->
                _siteConditions.value = conditions
            }.onFailure { e ->
                _errorMessage.value = "Failed to load site conditions: ${e.message}"
            }
        } catch (e: Exception) {
            _errorMessage.value = "Error loading site conditions: ${e.message}"
        }
    }

    /**
     * Load system notifications and alerts
     */
    private suspend fun loadNotifications() {
        try {
            activityRepository.getActivityFeed(limit = 20, includeResolved = false)
                .collect { activities ->
                    // Filter for system alerts only
                    val alerts = activities.filterIsInstance<ActivityFeedItem.SystemAlert>()
                    _notifications.value = alerts

                    // Update unread count
                    _unreadNotificationCount.value = alerts.count { !it.dismissed }
                }
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load notifications: ${e.message}"
        }
    }

    /**
     * Update command center buttons based on user tier
     */
    private fun updateCommandCenterButtons(userTier: UserTier) {
        viewModelScope.launch {
            try {
                // Get default buttons and filter by tier
                val defaultActions = getDefaultCommandCenterButtons()
                val availableActions = defaultActions.filterByTier(userTier)

                // Convert to button configurations
                val buttons = availableActions.map { action ->
                    action.toButtonConfig()
                }

                _commandCenterButtons.value = buttons
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update command center: ${e.message}"
            }
        }
    }

    /**
     * Handle safety action click from command center
     *
     * @param action The safety action to execute
     */
    fun onActionClick(action: SafetyAction) {
        viewModelScope.launch {
            try {
                // Verify user has permission for this action
                val currentUserTier = _userProfile.value?.userTier ?: UserTier.FIELD_ACCESS

                if (!action.isAvailableForTier(currentUserTier)) {
                    _errorMessage.value = "You don't have permission to perform this action"
                    return@launch
                }

                // Check if feature is implemented
                if (!action.isImplemented()) {
                    _errorMessage.value = "This feature is coming soon"
                    return@launch
                }

                // Action will be handled by navigation in the UI layer
                // ViewModel just validates permissions here
            } catch (e: Exception) {
                _errorMessage.value = "Failed to execute action: ${e.message}"
            }
        }
    }

    /**
     * Dismiss a notification
     *
     * @param alertId Alert identifier
     */
    fun dismissNotification(alertId: String) {
        viewModelScope.launch {
            try {
                val result = activityRepository.dismissAlert(alertId)
                result.onSuccess {
                    // Update local notifications list
                    _notifications.update { current ->
                        current.map { alert ->
                            if (alert.id == alertId) {
                                alert.copy(dismissed = true)
                            } else {
                                alert
                            }
                        }
                    }
                    // Update unread count
                    _unreadNotificationCount.update { count -> (count - 1).coerceAtLeast(0) }
                }.onFailure { e ->
                    _errorMessage.value = "Failed to dismiss notification: ${e.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error dismissing notification: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Get weather safety recommendations
     */
    fun getWeatherRecommendations() {
        viewModelScope.launch {
            try {
                val weather = _siteConditions.value?.weather
                if (weather != null) {
                    val recommendations = weatherRepository.getSafetyRecommendations(weather)
                    // Recommendations can be exposed as another StateFlow if needed
                    // For now, they can be accessed through the repository when needed
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load safety recommendations: ${e.message}"
            }
        }
    }

    /**
     * Update current project
     *
     * @param projectId Project identifier
     */
    fun setCurrentProject(projectId: String) {
        viewModelScope.launch {
            try {
                val result = userProfileRepository.setCurrentProject(projectId)
                result.onSuccess {
                    // Refresh dashboard data for new project
                    refreshData()
                }.onFailure { e ->
                    _errorMessage.value = "Failed to switch project: ${e.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error switching project: ${e.message}"
            }
        }
    }

    /**
     * Check if weather is safe for work
     */
    fun isWeatherSafeForWork(): Boolean {
        val weather = _siteConditions.value?.weather
        return weather?.isSafeForWork() ?: true
    }
}
