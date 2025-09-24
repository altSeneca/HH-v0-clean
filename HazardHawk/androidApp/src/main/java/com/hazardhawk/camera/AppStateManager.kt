package com.hazardhawk.camera

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * AppStateManager - Lifecycle-aware state persistence system
 *
 * Provides automatic state loading and validation for HazardHawk application startup.
 * Integrates with existing MetadataSettingsManager while adding lifecycle awareness
 * and corruption recovery.
 */
class AppStateManager(
    private val context: Context,
    private val metadataSettingsManager: MetadataSettingsManager
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "AppStateManager"
        private const val PREFS_NAME = "hazardhawk_app_state"
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_STATE_VERSION = "state_version"
        private const val KEY_LAST_LAUNCH_DATE = "last_launch_date"
        private const val CURRENT_STATE_VERSION = 1
    }

    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _appState = MutableStateFlow<AppState>(AppState.Loading)
    val appState: StateFlow<AppState> = _appState.asStateFlow()

    init {
        // Observe app lifecycle to validate state on app start/resume
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App started - validating persisted state")
        validatePersistedState()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        // Only validate if state is loading (first resume) to avoid repeated validation
        if (_appState.value is AppState.Loading) {
            validatePersistedState()
        }
    }

    /**
     * Validates persisted state and determines appropriate app route
     */
    private fun validatePersistedState() {
        try {
            Log.d(TAG, "Validating persisted state...")

            // Check if this is first launch
            val isFirstLaunch = sharedPrefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
            val stateVersion = sharedPrefs.getInt(KEY_STATE_VERSION, 0)

            if (isFirstLaunch || stateVersion != CURRENT_STATE_VERSION) {
                Log.d(TAG, "First launch or version mismatch - routing to setup")
                _appState.value = AppState.RequiresSetup(
                    isFirstLaunch = isFirstLaunch,
                    reason = if (isFirstLaunch) "First launch" else "State version mismatch"
                )
                return
            }

            // Validate state from MetadataSettingsManager
            val userProfile = metadataSettingsManager.userProfile.value
            val currentProject = metadataSettingsManager.currentProject.value

            val isUserProfileValid = userProfile.let {
                it.company.isNotBlank() &&
                it.userName.isNotBlank() &&
                it.email.isNotBlank()
            }

            val isProjectValid = currentProject.let {
                it.projectName.isNotBlank() &&
                it.projectId.isNotBlank()
            }

            when {
                isUserProfileValid && isProjectValid -> {
                    Log.d(TAG, "Valid state found - routing to camera")
                    _appState.value = AppState.Ready(
                        userProfile = userProfile,
                        currentProject = currentProject
                    )
                }

                else -> {
                    Log.d(TAG, "Invalid state - user: $isUserProfileValid, project: $isProjectValid")
                    _appState.value = AppState.RequiresSetup(
                        isFirstLaunch = false,
                        reason = "Incomplete user or project information"
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error validating state", e)
            _appState.value = AppState.RequiresSetup(
                isFirstLaunch = false,
                reason = "State validation error: ${e.message}"
            )
        }
    }

    /**
     * Called after successful setup completion
     */
    fun markSetupComplete() {
        Log.d(TAG, "Marking setup as complete")
        sharedPrefs.edit()
            .putBoolean(KEY_IS_FIRST_LAUNCH, false)
            .putInt(KEY_STATE_VERSION, CURRENT_STATE_VERSION)
            .apply()

        // Re-validate state to transition to Ready
        validatePersistedState()
    }

    /**
     * Check if this is the first launch of the current day
     */
    fun isFirstLaunchToday(): Boolean {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastLaunchDate = sharedPrefs.getString(KEY_LAST_LAUNCH_DATE, "")

        return lastLaunchDate != today
    }

    /**
     * Update the last launch date to today
     */
    fun updateLastLaunchDate() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        sharedPrefs.edit()
            .putString(KEY_LAST_LAUNCH_DATE, today)
            .apply()
        Log.d(TAG, "Updated last launch date to: $today")
    }

    /**
     * Forces state re-validation (useful for manual refresh)
     */
    fun refreshState() {
        Log.d(TAG, "Forcing state refresh")
        validatePersistedState()
    }

    /**
     * Provides state defaults for corrupted data recovery
     */
    fun provideStateDefaults(): Pair<UserProfile, ProjectInfo> {
        return Pair(
            UserProfile(
                userId = "",
                userName = "",
                company = "",
                email = "",
                role = "Field Worker",
                certificationLevel = "Basic",
                phone = ""
            ),
            ProjectInfo(
                projectId = "",
                projectName = "",
                siteAddress = "",
                projectManager = "",
                contractor = "",
                startDate = "",
                expectedEndDate = "",
                safetyOfficer = ""
            )
        )
    }

    /**
     * App state sealed class for reactive UI navigation
     */
    sealed class AppState {
        object Loading : AppState()

        data class RequiresSetup(
            val isFirstLaunch: Boolean,
            val reason: String
        ) : AppState()

        data class Ready(
            val userProfile: UserProfile,
            val currentProject: ProjectInfo
        ) : AppState()
    }
}