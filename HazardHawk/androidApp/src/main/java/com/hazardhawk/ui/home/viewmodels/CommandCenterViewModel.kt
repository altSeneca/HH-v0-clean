package com.hazardhawk.ui.home.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazardhawk.data.repositories.dashboard.UserProfileRepositoryImpl
import com.hazardhawk.models.dashboard.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the Command Center component.
 * Manages command center buttons, permissions, and notification badges.
 *
 * Injected Dependencies:
 * - UserProfileRepositoryImpl: Provides user tier and permissions
 *
 * StateFlows:
 * - availableActions: List of safety actions available to the current user (filtered by tier)
 * - notificationBadges: Badge counts for each action button
 * - buttonConfigs: Full button configurations with all display properties
 * - errorMessage: Error messages for user feedback
 *
 * Functions:
 * - getButtonConfig(SafetyAction): Get configuration for a specific button
 * - isActionEnabled(SafetyAction): Check if an action is enabled for current user
 */
class CommandCenterViewModel(
    private val userProfileRepository: UserProfileRepositoryImpl
) : ViewModel() {

    // Available Actions State (filtered by user tier)
    private val _availableActions = MutableStateFlow<List<SafetyAction>>(emptyList())
    val availableActions: StateFlow<List<SafetyAction>> = _availableActions.asStateFlow()

    // Notification Badges State
    private val _notificationBadges = MutableStateFlow<Map<SafetyAction, Int>>(emptyMap())
    val notificationBadges: StateFlow<Map<SafetyAction, Int>> = _notificationBadges.asStateFlow()

    // Button Configurations State
    private val _buttonConfigs = MutableStateFlow<List<CommandCenterButton>>(emptyList())
    val buttonConfigs: StateFlow<List<CommandCenterButton>> = _buttonConfigs.asStateFlow()

    // Current User Tier
    private val _userTier = MutableStateFlow(UserTier.FIELD_ACCESS)
    val userTier: StateFlow<UserTier> = _userTier.asStateFlow()

    // Error State
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Observe user profile changes
        viewModelScope.launch {
            userProfileRepository.getCurrentUserFlow().collect { profile ->
                _userTier.value = profile.userTier
                updateAvailableActions(profile.userTier)
            }
        }

        // Initialize with default buttons
        loadCommandCenter()
    }

    /**
     * Load command center configuration
     */
    private fun loadCommandCenter() {
        viewModelScope.launch {
            try {
                // Get current user tier
                val tier = userProfileRepository.getUserTier()
                _userTier.value = tier

                // Update available actions
                updateAvailableActions(tier)

                // Initialize notification badges
                updateNotificationBadges()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load command center: ${e.message}"
            }
        }
    }

    /**
     * Update available actions based on user tier
     */
    private fun updateAvailableActions(userTier: UserTier) {
        try {
            // Get default command center actions
            val defaultActions = getDefaultCommandCenterButtons()

            // Filter by user tier
            val available = defaultActions.filterByTier(userTier)
            _availableActions.value = available

            // Convert to button configurations
            val configs = available.map { action ->
                createButtonConfig(action, userTier)
            }
            _buttonConfigs.value = configs
        } catch (e: Exception) {
            _errorMessage.value = "Failed to update actions: ${e.message}"
        }
    }

    /**
     * Create button configuration for an action
     */
    private fun createButtonConfig(
        action: SafetyAction,
        userTier: UserTier
    ): CommandCenterButton {
        val baseConfig = action.toButtonConfig()
        val badge = _notificationBadges.value[action] ?: 0

        return baseConfig.copy(
            enabled = isActionEnabled(action),
            notificationBadge = badge
        )
    }

    /**
     * Get button configuration for a specific action
     *
     * @param action Safety action
     * @return Button configuration or null if not available
     */
    fun getButtonConfig(action: SafetyAction): CommandCenterButton? {
        return _buttonConfigs.value.find { it.action == action }
    }

    /**
     * Check if an action is enabled for the current user
     *
     * @param action Safety action to check
     * @return True if action is enabled
     */
    fun isActionEnabled(action: SafetyAction): Boolean {
        val tier = _userTier.value

        // Check if user tier has permission
        if (!action.isAvailableForTier(tier)) {
            return false
        }

        // Check if feature is implemented
        if (!action.isImplemented()) {
            return false
        }

        return true
    }

    /**
     * Update notification badges for all actions
     * This would typically be called when:
     * - New PTPs are created
     * - New hazards are detected
     * - New photos need review
     * - New incidents are reported
     */
    fun updateNotificationBadges() {
        viewModelScope.launch {
            try {
                // In a real implementation, these would come from respective repositories
                val badges = mapOf(
                    // Example badge counts (would be fetched from repositories)
                    SafetyAction.VIEW_REPORTS to 3,      // 3 new reports
                    SafetyAction.OPEN_GALLERY to 5,      // 5 photos needing review
                    SafetyAction.CREATE_PTP to 0,
                    SafetyAction.CAPTURE_PHOTO to 0,
                    SafetyAction.LIVE_DETECTION to 0,
                    SafetyAction.CREATE_TOOLBOX_TALK to 0,
                    SafetyAction.ASSIGN_TASKS to 2,      // 2 pending assignments
                    SafetyAction.REPORT_INCIDENT to 0,
                    SafetyAction.START_PRE_SHIFT to 0,
                    SafetyAction.MANAGE_CREW to 0
                )

                _notificationBadges.value = badges

                // Update button configs with new badges
                val tier = _userTier.value
                val configs = _availableActions.value.map { action ->
                    createButtonConfig(action, tier)
                }
                _buttonConfigs.value = configs
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update badges: ${e.message}"
            }
        }
    }

    /**
     * Set badge count for a specific action
     *
     * @param action Safety action
     * @param count Badge count
     */
    fun setBadgeCount(action: SafetyAction, count: Int) {
        viewModelScope.launch {
            _notificationBadges.update { current ->
                current.toMutableMap().apply {
                    put(action, count.coerceAtLeast(0))
                }
            }

            // Update button configs
            val tier = _userTier.value
            val configs = _availableActions.value.map { act ->
                createButtonConfig(act, tier)
            }
            _buttonConfigs.value = configs
        }
    }

    /**
     * Increment badge count for an action
     *
     * @param action Safety action
     */
    fun incrementBadge(action: SafetyAction) {
        val currentCount = _notificationBadges.value[action] ?: 0
        setBadgeCount(action, currentCount + 1)
    }

    /**
     * Decrement badge count for an action
     *
     * @param action Safety action
     */
    fun decrementBadge(action: SafetyAction) {
        val currentCount = _notificationBadges.value[action] ?: 0
        setBadgeCount(action, (currentCount - 1).coerceAtLeast(0))
    }

    /**
     * Clear badge count for an action
     *
     * @param action Safety action
     */
    fun clearBadge(action: SafetyAction) {
        setBadgeCount(action, 0)
    }

    /**
     * Clear all badge counts
     */
    fun clearAllBadges() {
        viewModelScope.launch {
            _notificationBadges.value = emptyMap()

            // Update button configs
            val tier = _userTier.value
            val configs = _availableActions.value.map { action ->
                createButtonConfig(action, tier)
            }
            _buttonConfigs.value = configs
        }
    }

    /**
     * Refresh command center (reload from user profile)
     */
    fun refresh() {
        loadCommandCenter()
    }

    /**
     * Get actions by tier level
     * Useful for displaying what features are available at different tiers
     *
     * @param tier User tier
     * @return List of actions available to that tier
     */
    fun getActionsForTier(tier: UserTier): List<SafetyAction> {
        return getDefaultCommandCenterButtons().filterByTier(tier)
    }

    /**
     * Get permission display information
     * Used for showing user what features they can access
     *
     * @return Map of actions to their permission requirements
     */
    fun getPermissionInfo(): Map<SafetyAction, PermissionInfo> {
        return SafetyAction.values().associateWith { action ->
            PermissionInfo(
                action = action,
                requiredTier = action.getRequiredTier(),
                isImplemented = action.isImplemented(),
                isAvailable = isActionEnabled(action)
            )
        }
    }

    /**
     * Check if user has permission for an action
     *
     * @param action Safety action
     * @return True if user has permission
     */
    suspend fun hasPermission(action: SafetyAction): Boolean {
        return try {
            val tier = userProfileRepository.getUserTier()
            action.isAvailableForTier(tier)
        } catch (e: Exception) {
            _errorMessage.value = "Failed to check permission: ${e.message}"
            false
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Get count of enabled actions
     */
    fun getEnabledActionCount(): Int {
        return _buttonConfigs.value.count { it.enabled }
    }

    /**
     * Get count of coming soon actions
     */
    fun getComingSoonActionCount(): Int {
        return _buttonConfigs.value.count { it.comingSoon }
    }

    /**
     * Get total badge count across all actions
     */
    fun getTotalBadgeCount(): Int {
        return _notificationBadges.value.values.sum()
    }
}

/**
 * Permission information for an action
 */
data class PermissionInfo(
    val action: SafetyAction,
    val requiredTier: UserTier,
    val isImplemented: Boolean,
    val isAvailable: Boolean
)

/**
 * Extension function to get command center layout configuration
 */
fun CommandCenterViewModel.getGridLayout(): CommandCenterLayout {
    return CommandCenterLayout(
        columns = 2,
        rows = 3,
        buttonCount = availableActions.value.size
    )
}

/**
 * Command center layout configuration
 */
data class CommandCenterLayout(
    val columns: Int,
    val rows: Int,
    val buttonCount: Int
)
