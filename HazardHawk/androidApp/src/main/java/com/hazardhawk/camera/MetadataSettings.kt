package com.hazardhawk.camera

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import com.hazardhawk.data.ProjectManager

data class UserProfile(
    val userId: String = "",
    val userName: String = "",
    val role: String = "Field Worker", // Field Worker, Safety Lead, Project Admin
    val company: String = "",
    val certificationLevel: String = "Basic", // Basic, Advanced, Expert
    val email: String = "",
    val phone: String = ""
)

data class ProjectInfo(
    val projectId: String = "",
    val projectName: String = "",
    val siteAddress: String = "",
    val projectManager: String = "",
    val contractor: String = "",
    val startDate: String = "",
    val expectedEndDate: String = "",
    val safetyOfficer: String = ""
)

data class AppSettings(
    val metadataDisplay: MetadataDisplaySettings = MetadataDisplaySettings(),
    val cameraSettings: CameraSettings = CameraSettings(),
    val dataPrivacy: DataPrivacySettings = DataPrivacySettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val startup: StartupSettings = StartupSettings()
)

data class CameraSettings(
    val autoFocus: Boolean = true,
    val flashMode: String = "auto", // auto, on, off
    val photoQuality: String = "high", // low, medium, high, maximum
    val enableHDR: Boolean = false,
    val enableGridLines: Boolean = true,
    val enableLevelIndicator: Boolean = true,
    val saveOriginalWithoutWatermark: Boolean = false,
    val compressionLevel: Int = 95, // JPEG compression 1-100
    val aspectRatio: String = "16:9", // Camera aspect ratio
    val timerDelay: Int = 0, // Timer delay in seconds (0 = off, 3, 5, 10)
    val showGPSOverlay: Boolean = true, // Show GPS overlay on viewfinder
    val aiAnalysisEnabled: Boolean = false // Enable AI safety analysis for captured photos - OFF by default
)

data class DataPrivacySettings(
    val includeLocation: Boolean = true,
    val includePreciseCoordinates: Boolean = false,
    val includeDeviceInfo: Boolean = true,
    val allowCloudSync: Boolean = true,
    val encryptLocalStorage: Boolean = true,
    val autoDeleteAfterDays: Int = 0, // 0 = never delete
    val showGPSCoordinates: Boolean = false // true = show coordinates, false = show address
)

data class NotificationSettings(
    val enablePhotoReminders: Boolean = true,
    val enableSafetyAlerts: Boolean = true,
    val enableLocationAlerts: Boolean = true,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "06:00"
)

data class StartupSettings(
    val showCompanyProjectOnLaunch: Boolean = true // Show company/project selection on first daily launch
)

class MetadataSettingsManager(
    context: Context,
    private val projectManager: ProjectManager? = null
) {

    /**
     * Enhanced MetadataSettingsManager with direct camera settings StateFlows
     * for seamless integration with CameraScreen UI components.
     * Now synchronized with ProjectManager for consistent project management.
     */

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "hazardhawk_metadata_settings",
        Context.MODE_PRIVATE
    )
    
    // StateFlows for reactive UI updates
    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()
    
    private val _currentProject = MutableStateFlow(loadCurrentProject())
    val currentProject: StateFlow<ProjectInfo> = _currentProject.asStateFlow()
    
    private val _appSettings = MutableStateFlow(loadAppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _projectsList = MutableStateFlow(loadProjectsList())
    val projectsList: StateFlow<List<ProjectInfo>> = _projectsList.asStateFlow()

    init {
        // Synchronize with ProjectManager on initialization
        syncWithProjectManager()
    }

    // Individual camera setting StateFlows for direct UI binding
    private val cameraScope = CoroutineScope(Dispatchers.Main)
    
    val flashModeState: StateFlow<String> = appSettings.map { it.cameraSettings.flashMode }
        .stateIn(cameraScope, SharingStarted.Eagerly, loadAppSettings().cameraSettings.flashMode)
    
    val aiAnalysisEnabledState: StateFlow<Boolean> = appSettings.map { it.cameraSettings.aiAnalysisEnabled }
        .stateIn(cameraScope, SharingStarted.Eagerly, loadAppSettings().cameraSettings.aiAnalysisEnabled)
                
    val aspectRatioState: StateFlow<String> = appSettings.map { it.cameraSettings.aspectRatio }
        .stateIn(cameraScope, SharingStarted.Eagerly, loadAppSettings().cameraSettings.aspectRatio)
                
    val showGPSOverlayState: StateFlow<Boolean> = appSettings.map { it.cameraSettings.showGPSOverlay }
        .stateIn(cameraScope, SharingStarted.Eagerly, loadAppSettings().cameraSettings.showGPSOverlay)
    
    companion object {
        // Preference keys
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_COMPANY = "user_company"
        private const val KEY_USER_CERTIFICATION = "user_certification"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        
        private const val KEY_PROJECT_ID = "project_id"
        private const val KEY_PROJECT_NAME = "project_name"
        private const val KEY_PROJECT_SITE_ADDRESS = "project_site_address"
        private const val KEY_PROJECT_MANAGER = "project_manager"
        private const val KEY_PROJECT_CONTRACTOR = "project_contractor"
        private const val KEY_PROJECT_START_DATE = "project_start_date"
        private const val KEY_PROJECT_END_DATE = "project_end_date"
        private const val KEY_PROJECT_SAFETY_OFFICER = "project_safety_officer"
        
        private const val KEY_SHOW_TIMESTAMP = "show_timestamp"
        private const val KEY_SHOW_LOCATION = "show_location"
        private const val KEY_SHOW_PROJECT_INFO = "show_project_info"
        private const val KEY_SHOW_USER_INFO = "show_user_info"
        private const val KEY_SHOW_COORDINATES = "show_coordinates"
        private const val KEY_OVERLAY_OPACITY = "overlay_opacity"
        private const val KEY_OVERLAY_POSITION = "overlay_position"
        
        private const val KEY_AUTO_FOCUS = "auto_focus"
        private const val KEY_FLASH_MODE = "flash_mode"
        private const val KEY_PHOTO_QUALITY = "photo_quality"
        private const val KEY_ENABLE_HDR = "enable_hdr"
        private const val KEY_ENABLE_GRID_LINES = "enable_grid_lines"
        private const val KEY_ENABLE_LEVEL_INDICATOR = "enable_level_indicator"
        private const val KEY_SAVE_ORIGINAL_WITHOUT_WATERMARK = "save_original_without_watermark"
        private const val KEY_COMPRESSION_LEVEL = "compression_level"
        private const val KEY_ASPECT_RATIO = "camera_aspect_ratio"
        private const val KEY_TIMER_DELAY = "camera_timer_delay"
        private const val KEY_SHOW_GPS_OVERLAY = "show_gps_overlay"
        private const val KEY_AI_ANALYSIS_ENABLED = "ai_analysis_enabled"
        
        private const val KEY_INCLUDE_LOCATION = "include_location"
        private const val KEY_INCLUDE_PRECISE_COORDINATES = "include_precise_coordinates"
        private const val KEY_INCLUDE_DEVICE_INFO = "include_device_info"
        private const val KEY_ALLOW_CLOUD_SYNC = "allow_cloud_sync"
        private const val KEY_ENCRYPT_LOCAL_STORAGE = "encrypt_local_storage"
        private const val KEY_AUTO_DELETE_AFTER_DAYS = "auto_delete_after_days"
        private const val KEY_SHOW_GPS_COORDINATES = "show_gps_coordinates"
        
        private const val KEY_ENABLE_PHOTO_REMINDERS = "enable_photo_reminders"
        private const val KEY_ENABLE_SAFETY_ALERTS = "enable_safety_alerts"
        private const val KEY_ENABLE_LOCATION_ALERTS = "enable_location_alerts"
        private const val KEY_QUIET_HOURS_START = "quiet_hours_start"
        private const val KEY_QUIET_HOURS_END = "quiet_hours_end"

        private const val KEY_SHOW_COMPANY_PROJECT_ON_LAUNCH = "show_company_project_on_launch"

        private const val KEY_PROJECTS_LIST = "projects_list"
    }
    
    // User Profile Management
    fun updateUserProfile(profile: UserProfile) {
        with(sharedPrefs.edit()) {
            putString(KEY_USER_ID, profile.userId)
            putString(KEY_USER_NAME, profile.userName)
            putString(KEY_USER_ROLE, profile.role)
            putString(KEY_USER_COMPANY, profile.company)
            putString(KEY_USER_CERTIFICATION, profile.certificationLevel)
            putString(KEY_USER_EMAIL, profile.email)
            putString(KEY_USER_PHONE, profile.phone)
            apply()
        }
        _userProfile.value = profile
    }
    
    private fun loadUserProfile(): UserProfile {
        return UserProfile(
            userId = sharedPrefs.getString(KEY_USER_ID, "") ?: "",
            userName = sharedPrefs.getString(KEY_USER_NAME, "") ?: "",
            role = sharedPrefs.getString(KEY_USER_ROLE, "Field Worker") ?: "Field Worker",
            company = sharedPrefs.getString(KEY_USER_COMPANY, "") ?: "",
            certificationLevel = sharedPrefs.getString(KEY_USER_CERTIFICATION, "Basic") ?: "Basic",
            email = sharedPrefs.getString(KEY_USER_EMAIL, "") ?: "",
            phone = sharedPrefs.getString(KEY_USER_PHONE, "") ?: ""
        )
    }
    
    // Project Management
    fun updateCurrentProject(project: ProjectInfo) {
        with(sharedPrefs.edit()) {
            putString(KEY_PROJECT_ID, project.projectId)
            putString(KEY_PROJECT_NAME, project.projectName)
            putString(KEY_PROJECT_SITE_ADDRESS, project.siteAddress)
            putString(KEY_PROJECT_MANAGER, project.projectManager)
            putString(KEY_PROJECT_CONTRACTOR, project.contractor)
            putString(KEY_PROJECT_START_DATE, project.startDate)
            putString(KEY_PROJECT_END_DATE, project.expectedEndDate)
            putString(KEY_PROJECT_SAFETY_OFFICER, project.safetyOfficer)
            apply()
        }
        _currentProject.value = project

        // Synchronize with ProjectManager
        if (project.projectName.isNotEmpty()) {
            projectManager?.let { pm ->
                if (!pm.projectExists(project.projectName)) {
                    pm.addProject(project.projectName)
                }
                pm.setCurrentProject(project.projectName)
            }
        }
    }
    
    private fun loadCurrentProject(): ProjectInfo {
        return ProjectInfo(
            projectId = sharedPrefs.getString(KEY_PROJECT_ID, "") ?: "",
            projectName = sharedPrefs.getString(KEY_PROJECT_NAME, "") ?: "",
            siteAddress = sharedPrefs.getString(KEY_PROJECT_SITE_ADDRESS, "") ?: "",
            projectManager = sharedPrefs.getString(KEY_PROJECT_MANAGER, "") ?: "",
            contractor = sharedPrefs.getString(KEY_PROJECT_CONTRACTOR, "") ?: "",
            startDate = sharedPrefs.getString(KEY_PROJECT_START_DATE, "") ?: "",
            expectedEndDate = sharedPrefs.getString(KEY_PROJECT_END_DATE, "") ?: "",
            safetyOfficer = sharedPrefs.getString(KEY_PROJECT_SAFETY_OFFICER, "") ?: ""
        )
    }
    
    // App Settings Management
    fun updateAppSettings(settings: AppSettings) {
        with(sharedPrefs.edit()) {
            // Metadata display settings
            putBoolean(KEY_SHOW_TIMESTAMP, settings.metadataDisplay.showTimestamp)
            putBoolean(KEY_SHOW_LOCATION, settings.metadataDisplay.showLocation)
            putBoolean(KEY_SHOW_PROJECT_INFO, settings.metadataDisplay.showProjectInfo)
            putBoolean(KEY_SHOW_USER_INFO, settings.metadataDisplay.showUserInfo)
            // Keep GPS coordinates setting synced with dataPrivacy setting
            putBoolean(KEY_SHOW_COORDINATES, settings.dataPrivacy.showGPSCoordinates)
            putFloat(KEY_OVERLAY_OPACITY, settings.metadataDisplay.overlayOpacity)
            putString(KEY_OVERLAY_POSITION, settings.metadataDisplay.position.name)
            
            // Camera settings
            putBoolean(KEY_AUTO_FOCUS, settings.cameraSettings.autoFocus)
            putString(KEY_FLASH_MODE, settings.cameraSettings.flashMode)
            putString(KEY_PHOTO_QUALITY, settings.cameraSettings.photoQuality)
            putBoolean(KEY_ENABLE_HDR, settings.cameraSettings.enableHDR)
            putBoolean(KEY_ENABLE_GRID_LINES, settings.cameraSettings.enableGridLines)
            putBoolean(KEY_ENABLE_LEVEL_INDICATOR, settings.cameraSettings.enableLevelIndicator)
            putBoolean(KEY_SAVE_ORIGINAL_WITHOUT_WATERMARK, settings.cameraSettings.saveOriginalWithoutWatermark)
            putInt(KEY_COMPRESSION_LEVEL, settings.cameraSettings.compressionLevel)
            putString(KEY_ASPECT_RATIO, settings.cameraSettings.aspectRatio)
            putInt(KEY_TIMER_DELAY, settings.cameraSettings.timerDelay)
            putBoolean(KEY_SHOW_GPS_OVERLAY, settings.cameraSettings.showGPSOverlay)
            putBoolean(KEY_AI_ANALYSIS_ENABLED, settings.cameraSettings.aiAnalysisEnabled)
            
            // Data privacy settings
            putBoolean(KEY_INCLUDE_LOCATION, settings.dataPrivacy.includeLocation)
            putBoolean(KEY_INCLUDE_PRECISE_COORDINATES, settings.dataPrivacy.includePreciseCoordinates)
            putBoolean(KEY_INCLUDE_DEVICE_INFO, settings.dataPrivacy.includeDeviceInfo)
            putBoolean(KEY_ALLOW_CLOUD_SYNC, settings.dataPrivacy.allowCloudSync)
            putBoolean(KEY_ENCRYPT_LOCAL_STORAGE, settings.dataPrivacy.encryptLocalStorage)
            putInt(KEY_AUTO_DELETE_AFTER_DAYS, settings.dataPrivacy.autoDeleteAfterDays)
            putBoolean(KEY_SHOW_GPS_COORDINATES, settings.dataPrivacy.showGPSCoordinates)
            
            // Notification settings
            putBoolean(KEY_ENABLE_PHOTO_REMINDERS, settings.notifications.enablePhotoReminders)
            putBoolean(KEY_ENABLE_SAFETY_ALERTS, settings.notifications.enableSafetyAlerts)
            putBoolean(KEY_ENABLE_LOCATION_ALERTS, settings.notifications.enableLocationAlerts)
            putString(KEY_QUIET_HOURS_START, settings.notifications.quietHoursStart)
            putString(KEY_QUIET_HOURS_END, settings.notifications.quietHoursEnd)

            // Startup settings
            putBoolean(KEY_SHOW_COMPANY_PROJECT_ON_LAUNCH, settings.startup.showCompanyProjectOnLaunch)

            apply()
        }
        _appSettings.value = settings
    }
    
    private fun loadAppSettings(): AppSettings {
        val metadataDisplay = MetadataDisplaySettings(
            showTimestamp = sharedPrefs.getBoolean(KEY_SHOW_TIMESTAMP, true),
            showLocation = sharedPrefs.getBoolean(KEY_SHOW_LOCATION, true),
            showProjectInfo = sharedPrefs.getBoolean(KEY_SHOW_PROJECT_INFO, true),
            showUserInfo = sharedPrefs.getBoolean(KEY_SHOW_USER_INFO, true),
            // Sync with GPS coordinates preference
            showCoordinates = sharedPrefs.getBoolean(KEY_SHOW_GPS_COORDINATES, false),
            overlayOpacity = sharedPrefs.getFloat(KEY_OVERLAY_OPACITY, 0.85f),
            position = try {
                OverlayPosition.valueOf(sharedPrefs.getString(KEY_OVERLAY_POSITION, "TOP_LEFT") ?: "TOP_LEFT")
            } catch (e: IllegalArgumentException) {
                OverlayPosition.TOP_LEFT
            }
        )
        
        val cameraSettings = CameraSettings(
            autoFocus = sharedPrefs.getBoolean(KEY_AUTO_FOCUS, true),
            flashMode = sharedPrefs.getString(KEY_FLASH_MODE, "auto") ?: "auto",
            photoQuality = sharedPrefs.getString(KEY_PHOTO_QUALITY, "high") ?: "high",
            enableHDR = sharedPrefs.getBoolean(KEY_ENABLE_HDR, false),
            enableGridLines = sharedPrefs.getBoolean(KEY_ENABLE_GRID_LINES, true),
            enableLevelIndicator = sharedPrefs.getBoolean(KEY_ENABLE_LEVEL_INDICATOR, true),
            saveOriginalWithoutWatermark = sharedPrefs.getBoolean(KEY_SAVE_ORIGINAL_WITHOUT_WATERMARK, false),
            compressionLevel = sharedPrefs.getInt(KEY_COMPRESSION_LEVEL, 95),
            aspectRatio = sharedPrefs.getString(KEY_ASPECT_RATIO, "16:9") ?: "16:9",
            timerDelay = sharedPrefs.getInt(KEY_TIMER_DELAY, 0),
            showGPSOverlay = sharedPrefs.getBoolean(KEY_SHOW_GPS_OVERLAY, true),
            aiAnalysisEnabled = sharedPrefs.getBoolean(KEY_AI_ANALYSIS_ENABLED, false)
        )
        
        val dataPrivacy = DataPrivacySettings(
            includeLocation = sharedPrefs.getBoolean(KEY_INCLUDE_LOCATION, true),
            includePreciseCoordinates = sharedPrefs.getBoolean(KEY_INCLUDE_PRECISE_COORDINATES, false),
            includeDeviceInfo = sharedPrefs.getBoolean(KEY_INCLUDE_DEVICE_INFO, true),
            allowCloudSync = sharedPrefs.getBoolean(KEY_ALLOW_CLOUD_SYNC, true),
            encryptLocalStorage = sharedPrefs.getBoolean(KEY_ENCRYPT_LOCAL_STORAGE, true),
            autoDeleteAfterDays = sharedPrefs.getInt(KEY_AUTO_DELETE_AFTER_DAYS, 0),
            showGPSCoordinates = sharedPrefs.getBoolean(KEY_SHOW_GPS_COORDINATES, false)
        )
        
        val notifications = NotificationSettings(
            enablePhotoReminders = sharedPrefs.getBoolean(KEY_ENABLE_PHOTO_REMINDERS, true),
            enableSafetyAlerts = sharedPrefs.getBoolean(KEY_ENABLE_SAFETY_ALERTS, true),
            enableLocationAlerts = sharedPrefs.getBoolean(KEY_ENABLE_LOCATION_ALERTS, true),
            quietHoursStart = sharedPrefs.getString(KEY_QUIET_HOURS_START, "22:00") ?: "22:00",
            quietHoursEnd = sharedPrefs.getString(KEY_QUIET_HOURS_END, "06:00") ?: "06:00"
        )

        val startup = StartupSettings(
            showCompanyProjectOnLaunch = sharedPrefs.getBoolean(KEY_SHOW_COMPANY_PROJECT_ON_LAUNCH, true)
        )

        return AppSettings(
            metadataDisplay = metadataDisplay,
            cameraSettings = cameraSettings,
            dataPrivacy = dataPrivacy,
            notifications = notifications,
            startup = startup
        )
    }
    
    /**
     * Get device information for metadata
     */
    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    }
    
    /**
     * Create capture metadata from current settings
     */
    fun createCaptureMetadata(
        locationData: LocationData = LocationData(),
        aspectRatio: String? = null
    ): CaptureMetadata {
        val userProfile = _userProfile.value
        val project = _currentProject.value
        val settings = _appSettings.value

        // DEBUG: Log project information when creating metadata
        android.util.Log.d("MetadataSettings", "🔍 createCaptureMetadata - Project debug:")
        android.util.Log.d("MetadataSettings", "  ├─ project.projectName: '${project.projectName}' (length: ${project.projectName.length})")
        android.util.Log.d("MetadataSettings", "  ├─ project.projectId: '${project.projectId}' (length: ${project.projectId.length})")
        android.util.Log.d("MetadataSettings", "  ├─ userProfile.userName: '${userProfile.userName}' (length: ${userProfile.userName.length})")
        android.util.Log.d("MetadataSettings", "  ├─ userProfile.userId: '${userProfile.userId}' (length: ${userProfile.userId.length})")
        android.util.Log.d("MetadataSettings", "  └─ Reading directly from SharedPrefs:")
        android.util.Log.d("MetadataSettings", "    ├─ KEY_PROJECT_NAME: '${sharedPrefs.getString(KEY_PROJECT_NAME, "") ?: ""}'")
        android.util.Log.d("MetadataSettings", "    └─ KEY_PROJECT_ID: '${sharedPrefs.getString(KEY_PROJECT_ID, "") ?: ""}'")

        // Include aspect ratio in device info for cropping reference
        val currentAspectRatio = aspectRatio ?: settings.cameraSettings.aspectRatio
        val deviceInfoWithAspectRatio = "${getDeviceInfo()} | AspectRatio: $currentAspectRatio"

        val metadata = CaptureMetadata(
            timestamp = System.currentTimeMillis(),
            locationData = locationData,
            projectName = project.projectName,
            projectId = project.projectId,
            userName = userProfile.userName,
            userId = userProfile.userId,
            deviceInfo = deviceInfoWithAspectRatio
        )

        android.util.Log.d("MetadataSettings", "📦 Final CaptureMetadata created:")
        android.util.Log.d("MetadataSettings", "  ├─ projectName: '${metadata.projectName}'")
        android.util.Log.d("MetadataSettings", "  ├─ projectId: '${metadata.projectId}'")
        android.util.Log.d("MetadataSettings", "  ├─ userName: '${metadata.userName}'")
        android.util.Log.d("MetadataSettings", "  └─ userId: '${metadata.userId}'")

        return metadata
    }

    // Projects List Management
    fun addProject(project: ProjectInfo) {
        val currentProjects = _projectsList.value.toMutableList()
        val existingIndex = currentProjects.indexOfFirst { it.projectId == project.projectId }

        if (existingIndex >= 0) {
            // Update existing project
            currentProjects[existingIndex] = project
        } else {
            // Add new project
            currentProjects.add(project)
        }

        saveProjectsList(currentProjects)
        _projectsList.value = currentProjects

        // Synchronize with ProjectManager
        projectManager?.let { pm ->
            if (!pm.projectExists(project.projectName)) {
                pm.addProject(project.projectName)
            }
        }
    }

    fun removeProject(projectId: String) {
        val currentProjects = _projectsList.value.toMutableList()
        val projectToRemove = currentProjects.find { it.projectId == projectId }
        currentProjects.removeAll { it.projectId == projectId }

        saveProjectsList(currentProjects)
        _projectsList.value = currentProjects

        // Synchronize with ProjectManager
        projectToRemove?.let { project ->
            projectManager?.removeProject(project.projectName)
        }

        // If the removed project was the current one, clear current project
        if (_currentProject.value.projectId == projectId) {
            updateCurrentProject(ProjectInfo())
        }
    }

    private fun saveProjectsList(projects: List<ProjectInfo>) {
        try {
            val projectsJson = projects.joinToString("|||") { project ->
                "${project.projectId}::${project.projectName}::${project.siteAddress}::${project.projectManager}::${project.contractor}::${project.startDate}::${project.expectedEndDate}::${project.safetyOfficer}"
            }
            sharedPrefs.edit().putString(KEY_PROJECTS_LIST, projectsJson).apply()
        } catch (e: Exception) {
            android.util.Log.e("MetadataSettings", "Error saving projects list", e)
        }
    }

    private fun loadProjectsList(): List<ProjectInfo> {
        return try {
            val projectsJson = sharedPrefs.getString(KEY_PROJECTS_LIST, "") ?: ""
            if (projectsJson.isBlank()) {
                // Return default projects if none exist
                getDefaultProjects()
            } else {
                projectsJson.split("|||").mapNotNull { projectString ->
                    val parts = projectString.split("::")
                    if (parts.size >= 8) {
                        ProjectInfo(
                            projectId = parts[0],
                            projectName = parts[1],
                            siteAddress = parts[2],
                            projectManager = parts[3],
                            contractor = parts[4],
                            startDate = parts[5],
                            expectedEndDate = parts[6],
                            safetyOfficer = parts[7]
                        )
                    } else null
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MetadataSettings", "Error loading projects list", e)
            getDefaultProjects()
        }
    }

    private fun getDefaultProjects(): List<ProjectInfo> {
        return listOf(
            ProjectInfo(
                projectId = "DEMO",
                projectName = "Demo Project",
                siteAddress = "Sample Site"
            )
        )
    }

    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        sharedPrefs.edit().clear().apply()
        _userProfile.value = UserProfile()
        _currentProject.value = ProjectInfo()
        _appSettings.value = AppSettings()
        _projectsList.value = getDefaultProjects()
    }
    
    /**
     * Check if user profile is complete enough for metadata
     */
    fun isUserProfileComplete(): Boolean {
        val profile = _userProfile.value
        return profile.userName.isNotBlank() && profile.userId.isNotBlank()
    }
    
    /**
     * Check if project info is complete enough for metadata
     */
    fun isProjectInfoComplete(): Boolean {
        val project = _currentProject.value
        return project.projectName.isNotBlank() && project.projectId.isNotBlank()
    }
    
    // Convenient update methods for individual camera settings
    fun updateFlashMode(flashMode: String) {
        val currentSettings = _appSettings.value
        val updatedSettings = currentSettings.copy(
            cameraSettings = currentSettings.cameraSettings.copy(flashMode = flashMode)
        )
        updateAppSettings(updatedSettings)
    }
    
    fun updateAIAnalysisEnabled(enabled: Boolean) {
        val currentSettings = _appSettings.value
        val updatedSettings = currentSettings.copy(
            cameraSettings = currentSettings.cameraSettings.copy(aiAnalysisEnabled = enabled)
        )
        updateAppSettings(updatedSettings)
    }
    
    fun updateAspectRatio(aspectRatio: String) {
        val currentSettings = _appSettings.value
        val updatedSettings = currentSettings.copy(
            cameraSettings = currentSettings.cameraSettings.copy(aspectRatio = aspectRatio)
        )
        updateAppSettings(updatedSettings)
    }

    fun updateTimerDelay(delaySeconds: Int) {
        val currentSettings = _appSettings.value
        val updatedSettings = currentSettings.copy(
            cameraSettings = currentSettings.cameraSettings.copy(timerDelay = delaySeconds)
        )
        updateAppSettings(updatedSettings)
    }

    fun updateShowGPSOverlay(show: Boolean) {
        val currentSettings = _appSettings.value
        val updatedSettings = currentSettings.copy(
            cameraSettings = currentSettings.cameraSettings.copy(showGPSOverlay = show)
        )
        updateAppSettings(updatedSettings)
    }

    fun updateShowGPSCoordinates(showCoordinates: Boolean) {
        val currentSettings = _appSettings.value
        val updatedSettings = currentSettings.copy(
            dataPrivacy = currentSettings.dataPrivacy.copy(showGPSCoordinates = showCoordinates),
            metadataDisplay = currentSettings.metadataDisplay.copy(showCoordinates = showCoordinates)
        )
        updateAppSettings(updatedSettings)
    }

    /**
     * Synchronize projects between ProjectManager and MetadataSettingsManager
     */
    private fun syncWithProjectManager() {
        projectManager?.let { pm ->
            val projectManagerProjects = pm.allProjects.value
            val currentProjects = _projectsList.value.toMutableList()

            // Add any projects from ProjectManager that don't exist in MetadataSettingsManager
            projectManagerProjects.forEach { projectName ->
                if (!currentProjects.any { it.projectName == projectName }) {
                    val newProject = ProjectInfo(
                        projectId = "SYNC_${System.currentTimeMillis()}_${projectName.hashCode()}",
                        projectName = projectName
                    )
                    currentProjects.add(newProject)
                }
            }

            // Add any projects from MetadataSettingsManager that don't exist in ProjectManager
            currentProjects.forEach { project ->
                if (!pm.projectExists(project.projectName)) {
                    pm.addProject(project.projectName)
                }
            }

            // Update the projects list if changes were made
            if (currentProjects.size != _projectsList.value.size) {
                saveProjectsList(currentProjects)
                _projectsList.value = currentProjects
            }
        }
    }
}