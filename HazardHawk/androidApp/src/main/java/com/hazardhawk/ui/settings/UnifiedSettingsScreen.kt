package com.hazardhawk.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.security.SecureKeyManager
import com.hazardhawk.ui.components.APIKeySetupCard
import com.hazardhawk.ui.components.validateAPIKey
import com.hazardhawk.data.repositories.UISettingsRepository
import com.hazardhawk.camera.MetadataSettingsManager
import com.hazardhawk.data.ProjectManager
import org.koin.compose.koinInject

/**
 * Unified Settings Screen - Following UNIFIED_SETTINGS_RECOMMENDATIONS.md
 *
 * Features:
 * - Profile section at top (temporary until Dashboard implemented)
 * - Camera & Capture settings
 * - AI & Analysis configuration
 * - AR settings with privacy controls
 * - Location & Metadata options
 * - Privacy & Security settings
 * - Storage & Backup management
 * - Display & Accessibility (NO glass effects, emergency mode, or high contrast)
 * - Notifications
 * - About & Help
 *
 * REMOVED: Glass effects, Emergency Mode, High Contrast Mode (deprecated)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPTP: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val secureKeyManager = remember { SecureKeyManager.getInstance(context) }
    val uiSettingsRepository: UISettingsRepository = koinInject()
    val projectManager = remember { ProjectManager(context) }
    val metadataSettingsManager = remember { MetadataSettingsManager(context, projectManager) }
    val coroutineScope = rememberCoroutineScope()

    // Load settings from repository
    val uiSettings by uiSettingsRepository.getSettingsFlow().collectAsStateWithLifecycle()
    val appSettings by metadataSettingsManager.appSettings.collectAsStateWithLifecycle()
    val currentProject by metadataSettingsManager.currentProject.collectAsStateWithLifecycle()
    val userProfile by metadataSettingsManager.userProfile.collectAsStateWithLifecycle()

    // Initialize repository on first composition
    LaunchedEffect(Unit) {
        uiSettingsRepository.loadSettings()
    }

    // API Key state
    var apiKey by remember { mutableStateOf(secureKeyManager.getGeminiApiKey() ?: "") }
    var isApiKeyExpanded by remember { mutableStateOf(false) }
    val isApiKeyValid by remember { derivedStateOf { validateAPIKey(apiKey).isEmpty() } }

    // Selected tab state
    var selectedTab by remember { mutableStateOf(SettingsTab.CAMERA) }

    // Profile dialog state
    var showProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ConstructionColors.Surface,
                        ConstructionColors.SurfaceVariant
                    )
                )
            )
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold,
                    color = ConstructionColors.OnSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = ConstructionColors.OnSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ConstructionColors.SafetyOrange
            )
        )

        // Profile Section (Prominent at top)
        ProfileHeader(
            userName = userProfile.userName.ifBlank { "User Name" },
            userRole = userProfile.role.ifBlank { "Field Worker" },
            currentProject = currentProject.projectName.ifBlank { "No Project Selected" },
            companyName = userProfile.company.ifBlank { "Company Name" },
            onClick = { showProfileDialog = true }
        )

        // Tab Navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = ConstructionColors.Surface,
            contentColor = ConstructionColors.SafetyOrange,
            edgePadding = 8.dp
        ) {
            SettingsTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tab.title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }
        }

        // Settings Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                SettingsTab.CAMERA -> CameraSettings(appSettings, metadataSettingsManager, coroutineScope)
                SettingsTab.AI -> AISettings(apiKey, isApiKeyValid, isApiKeyExpanded,
                    onApiKeyChange = { newKey ->
                        apiKey = newKey
                        if (newKey.isNotEmpty() && isApiKeyValid) {
                            try {
                                secureKeyManager.storeGeminiApiKey(newKey, "user_entered_${System.currentTimeMillis()}")
                            } catch (e: Exception) {
                                android.util.Log.e("UnifiedSettings", "Failed to store API key: ${e.message}", e)
                            }
                        }
                    },
                    onExpandedChange = { isApiKeyExpanded = it }
                )
                SettingsTab.AR -> ARSettings(uiSettings, uiSettingsRepository, coroutineScope)
                SettingsTab.DOCUMENTS -> DocumentsSettings(onNavigateToPTP)
                SettingsTab.LOCATION -> LocationMetadataSettings(appSettings, metadataSettingsManager, coroutineScope)
                SettingsTab.PRIVACY -> PrivacySecuritySettings(appSettings, metadataSettingsManager, coroutineScope)
                SettingsTab.STORAGE -> StorageBackupSettings()
                SettingsTab.DISPLAY -> DisplayAccessibilitySettings(uiSettings, uiSettingsRepository, coroutineScope)
                SettingsTab.NOTIFICATIONS -> NotificationsSettings()
                SettingsTab.ABOUT -> AboutSettings()
            }
        }
    }

    // Profile Dialog
    if (showProfileDialog) {
        ProfileDialogWithEdit(
            userProfile = userProfile,
            currentProject = currentProject,
            projectsList = metadataSettingsManager.projectsList.collectAsStateWithLifecycle().value,
            onDismiss = { showProfileDialog = false },
            onSave = { updatedProfile, updatedProject ->
                coroutineScope.launch {
                    // Update user profile
                    metadataSettingsManager.updateUserProfile(updatedProfile)

                    // Update project if changed
                    if (updatedProject.projectId != currentProject.projectId) {
                        metadataSettingsManager.updateCurrentProject(updatedProject)
                    }

                    showProfileDialog = false
                }
            }
        )
    }
}

/**
 * Settings Tab Enumeration
 */
enum class SettingsTab(val title: String, val icon: ImageVector) {
    CAMERA("Camera", Icons.Default.Camera),
    AI("AI Analysis", Icons.Default.AutoAwesome),
    AR("AR Mode", Icons.Default.ViewInAr),
    DOCUMENTS("Documents", Icons.Default.Description),
    LOCATION("Location", Icons.Default.LocationOn),
    PRIVACY("Privacy", Icons.Default.Security),
    STORAGE("Storage", Icons.Default.Storage),
    DISPLAY("Display", Icons.Default.Settings),
    NOTIFICATIONS("Notifications", Icons.Default.Notifications),
    ABOUT("About", Icons.Default.Info)
}

/**
 * Profile Header - Prominent section at top of settings
 * Will be removed once Dashboard Home Screen is implemented
 */
@Composable
private fun ProfileHeader(
    userName: String,
    userRole: String,
    currentProject: String,
    companyName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.SafetyOrange.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ConstructionColors.SafetyOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(2).uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // User Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ConstructionColors.OnSurface
                )
                Text(
                    text = userRole,
                    fontSize = 14.sp,
                    color = ConstructionColors.OnSurfaceVariant
                )
                Text(
                    text = companyName,
                    fontSize = 12.sp,
                    color = ConstructionColors.OnSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Engineering,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = ConstructionColors.SafetyOrange
                    )
                    Text(
                        text = currentProject,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = ConstructionColors.SafetyOrange
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Edit Profile",
                tint = ConstructionColors.OnSurfaceVariant
            )
        }
    }
}

/**
 * Camera & Capture Settings
 */
@Composable
private fun CameraSettings(
    appSettings: com.hazardhawk.camera.AppSettings,
    metadataSettingsManager: MetadataSettingsManager,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    // Photo Quality Section
    SettingsSection(title = "Photo Quality") {
        SettingsDropdownItem(
            title = "Image Quality",
            subtitle = "Adjust JPEG compression quality (95% recommended)",
            icon = Icons.Default.HighQuality,
            selectedValue = "${appSettings.cameraSettings.compressionLevel}%",
            options = listOf("85%", "90%", "95%", "100%"),
            onValueChange = { quality ->
                val qualityInt = quality.replace("%", "").toIntOrNull() ?: 95
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        cameraSettings = appSettings.cameraSettings.copy(compressionLevel = qualityInt)
                    )
                    metadataSettingsManager.updateAppSettings(updatedSettings)
                }
            }
        )

        SettingsToggleItem(
            title = "HDR Mode",
            subtitle = "Capture multiple exposures for better dynamic range",
            icon = Icons.Default.WbSunny,
            checked = appSettings.cameraSettings.enableHDR,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        cameraSettings = appSettings.cameraSettings.copy(enableHDR = enabled)
                    )
                    metadataSettingsManager.updateAppSettings(updatedSettings)
                }
            }
        )

        SettingsToggleItem(
            title = "Save Original Without Watermark",
            subtitle = "Save both watermarked and clean versions",
            icon = Icons.Default.CopyAll,
            checked = false, // TODO: Add to AppSettings.camera
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )
    }

    // Camera Controls Section
    SettingsSection(title = "Camera Controls") {
        SettingsDropdownItem(
            title = "Flash Mode",
            subtitle = "Control camera flash behavior",
            icon = Icons.Default.FlashOn,
            selectedValue = when (appSettings.cameraSettings.flashMode) {
                "AUTO" -> "Auto"
                "ON" -> "Always On"
                "OFF" -> "Always Off"
                else -> "Auto"
            },
            options = listOf("Auto", "Always On", "Always Off"),
            onValueChange = { mode ->
                val flashMode = when (mode) {
                    "Auto" -> "AUTO"
                    "Always On" -> "ON"
                    "Always Off" -> "OFF"
                    else -> "AUTO"
                }
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        cameraSettings = appSettings.cameraSettings.copy(flashMode = flashMode)
                    )
                    metadataSettingsManager.updateAppSettings(updatedSettings)
                }
            }
        )

        SettingsToggleItem(
            title = "Grid Lines",
            subtitle = "Show rule-of-thirds grid for better composition",
            icon = Icons.Default.Grid3x3,
            checked = appSettings.cameraSettings.enableGridLines,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        cameraSettings = appSettings.cameraSettings.copy(enableGridLines = enabled)
                    )
                    metadataSettingsManager.updateAppSettings(updatedSettings)
                }
            }
        )

        SettingsToggleItem(
            title = "Volume Button Capture",
            subtitle = "Use volume buttons to take photos (glove-friendly)",
            icon = Icons.Default.VolumeUp,
            checked = true, // TODO: Add to AppSettings.camera
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsToggleItem(
            title = "Auto-Focus",
            subtitle = "Automatically focus when tapping to capture",
            icon = Icons.Default.CenterFocusWeak,
            checked = true, // TODO: Add to AppSettings.camera
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsDropdownItem(
            title = "Timer Delay",
            subtitle = "Countdown before photo capture",
            icon = Icons.Default.Timer,
            selectedValue = when (appSettings.cameraSettings.timerDelay) {
                0 -> "Off"
                3 -> "3 seconds"
                5 -> "5 seconds"
                10 -> "10 seconds"
                else -> "Off"
            },
            options = listOf("Off", "3 seconds", "5 seconds", "10 seconds"),
            onValueChange = { delay ->
                val delaySeconds = when (delay) {
                    "Off" -> 0
                    "3 seconds" -> 3
                    "5 seconds" -> 5
                    "10 seconds" -> 10
                    else -> 0
                }
                coroutineScope.launch {
                    metadataSettingsManager.updateTimerDelay(delaySeconds)
                }
            }
        )
    }

    // Aspect Ratio & Zoom Section
    SettingsSection(title = "Aspect Ratio & Zoom") {
        SettingsDropdownItem(
            title = "Default Aspect Ratio",
            subtitle = "Choose photo dimensions for capture",
            icon = Icons.Default.AspectRatio,
            selectedValue = when (appSettings.cameraSettings.aspectRatio) {
                "1:1" -> "Square (1:1)"
                "4:3" -> "4:3 (Standard)"
                "16:9" -> "16:9 (Wide)"
                "3:2" -> "3:2 (DSLR)"
                else -> "4:3 (Standard)"
            },
            options = listOf("Square (1:1)", "4:3 (Standard)", "16:9 (Wide)", "3:2 (DSLR)"),
            onValueChange = { ratio ->
                val aspectRatio = when (ratio) {
                    "Square (1:1)" -> "1:1"
                    "4:3 (Standard)" -> "4:3"
                    "16:9 (Wide)" -> "16:9"
                    "3:2 (DSLR)" -> "3:2"
                    else -> "4:3"
                }
                coroutineScope.launch {
                    metadataSettingsManager.updateAspectRatio(aspectRatio)
                }
            }
        )

        SettingsItem(
            title = "Zoom Settings",
            subtitle = "Configure zoom behavior and limits",
            icon = Icons.Default.ZoomIn,
            onClick = {
                // TODO: Navigate to zoom configuration screen
            }
        )
    }
}

/**
 * AI & Analysis Settings
 */
@Composable
private fun AISettings(
    apiKey: String,
    isValid: Boolean,
    expanded: Boolean,
    onApiKeyChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit
) {
    SettingsSection(title = "AI Configuration") {
        APIKeySetupCard(
            apiKey = apiKey,
            onApiKeyChange = onApiKeyChange,
            isValid = isValid,
            expanded = expanded,
            onExpandedChange = onExpandedChange
        )
    }
}

/**
 * AR Settings with Privacy Controls
 */
@Composable
private fun ARSettings(
    uiSettings: com.hazardhawk.data.repositories.UISettings,
    uiSettingsRepository: UISettingsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    SettingsSection(title = "Augmented Reality") {
        SettingsToggleItem(
            title = "AR Safety Mode",
            subtitle = "Enable augmented reality hazard detection overlays",
            icon = Icons.Default.ViewInAr,
            checked = uiSettings.arEnabled,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    val updatedSettings = uiSettings.copy(arEnabled = enabled)
                    uiSettingsRepository.saveSettings(updatedSettings)
                }
            }
        )

        // AR Privacy Settings (only show if AR is enabled)
        AnimatedVisibility(
            visible = uiSettings.arEnabled,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsToggleItem(
                    title = "Facial Anonymization",
                    subtitle = "Automatically blur worker faces for privacy protection",
                    icon = Icons.Default.Security,
                    checked = uiSettings.facialAnonymizationEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            val updatedSettings = uiSettings.copy(facialAnonymizationEnabled = enabled)
                            uiSettingsRepository.saveSettings(updatedSettings)
                        }
                    }
                )

                SettingsDropdownItem(
                    title = "Privacy Protection Level",
                    subtitle = "Adjust level of worker privacy protection",
                    icon = Icons.Default.PrivacyTip,
                    selectedValue = uiSettings.privacyProtectionLevel,
                    options = listOf("MINIMAL", "STANDARD", "MAXIMUM"),
                    onValueChange = { level ->
                        coroutineScope.launch {
                            val updatedSettings = uiSettings.copy(privacyProtectionLevel = level)
                            uiSettingsRepository.saveSettings(updatedSettings)
                        }
                    }
                )

                SettingsDropdownItem(
                    title = "Data Retention",
                    subtitle = "Automatically delete AR data after specified period",
                    icon = Icons.Default.Schedule,
                    selectedValue = "${uiSettings.arDataRetentionDays} days",
                    options = listOf("7 days", "30 days", "90 days"),
                    onValueChange = { retention ->
                        val days = retention.replace(" days", "").toIntOrNull() ?: 30
                        coroutineScope.launch {
                            val updatedSettings = uiSettings.copy(arDataRetentionDays = days)
                            uiSettingsRepository.saveSettings(updatedSettings)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Location & Metadata Settings
 */
@Composable
private fun LocationMetadataSettings(
    appSettings: com.hazardhawk.camera.AppSettings,
    metadataSettingsManager: MetadataSettingsManager,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    SettingsSection(title = "Location Display") {
        SettingsToggleItem(
            title = "Include Location",
            subtitle = "Add GPS coordinates or address to photos",
            icon = Icons.Default.LocationOn,
            checked = appSettings.dataPrivacy.includeLocation,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        dataPrivacy = appSettings.dataPrivacy.copy(includeLocation = enabled)
                    )
                    metadataSettingsManager.updateAppSettings(updatedSettings)
                }
            }
        )

        AnimatedVisibility(visible = appSettings.dataPrivacy.includeLocation) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsToggleItem(
                    title = "Show GPS Coordinates",
                    subtitle = "Display precise lat/long instead of address",
                    icon = Icons.Default.MyLocation,
                    checked = appSettings.dataPrivacy.showGPSCoordinates,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            val updatedSettings = appSettings.copy(
                                dataPrivacy = appSettings.dataPrivacy.copy(showGPSCoordinates = enabled)
                            )
                            metadataSettingsManager.updateAppSettings(updatedSettings)
                        }
                    }
                )

                SettingsToggleItem(
                    title = "GPS Overlay on Camera",
                    subtitle = "Show live GPS data in camera viewfinder",
                    icon = Icons.Default.GpsFixed,
                    checked = appSettings.cameraSettings.showGPSOverlay,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            val updatedSettings = appSettings.copy(
                                cameraSettings = appSettings.cameraSettings.copy(showGPSOverlay = enabled)
                            )
                            metadataSettingsManager.updateAppSettings(updatedSettings)
                        }
                    }
                )
            }
        }
    }

    SettingsSection(title = "Metadata Overlay") {
        SettingsToggleItem(
            title = "Project Information",
            subtitle = "Show project name and details on photos",
            icon = Icons.Default.Engineering,
            checked = true, // TODO: Add to AppSettings.metadata
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsToggleItem(
            title = "User Information",
            subtitle = "Display photographer name and role",
            icon = Icons.Default.Person,
            checked = true, // TODO: Add to AppSettings.metadata
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsToggleItem(
            title = "Timestamp",
            subtitle = "Add capture date and time to photos",
            icon = Icons.Default.Schedule,
            checked = true, // TODO: Add to AppSettings.metadata
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsDropdownItem(
            title = "Metadata Position",
            subtitle = "Where to display metadata overlay",
            icon = Icons.Default.Place,
            selectedValue = "Bottom Left",
            options = listOf("Top Left", "Top Right", "Bottom Left", "Bottom Right"),
            onValueChange = { position ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsSliderItem(
            title = "Font Size",
            subtitle = "Adjust metadata text size (12-24sp)",
            icon = Icons.Default.FormatSize,
            value = 16f,
            valueRange = 12f..24f,
            onValueChange = { size ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsSliderItem(
            title = "Opacity",
            subtitle = "Control metadata overlay transparency",
            icon = Icons.Default.Opacity,
            value = 0.85f,
            valueRange = 0.5f..1f,
            onValueChange = { opacity ->
                // TODO: Implement in MetadataSettingsManager
            }
        )
    }
}

/**
 * Privacy & Security Settings
 */
@Composable
private fun PrivacySecuritySettings(
    appSettings: com.hazardhawk.camera.AppSettings,
    metadataSettingsManager: MetadataSettingsManager,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    SettingsSection(title = "Data Privacy") {
        SettingsToggleItem(
            title = "Encrypt Local Storage",
            subtitle = "Protect stored photos with device encryption",
            icon = Icons.Default.Lock,
            checked = appSettings.dataPrivacy.encryptLocalStorage,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        dataPrivacy = appSettings.dataPrivacy.copy(encryptLocalStorage = enabled)
                    )
                    metadataSettingsManager.updateAppSettings(updatedSettings)
                }
            }
        )

        SettingsToggleItem(
            title = "Allow Cloud Sync",
            subtitle = "Enable uploading to cloud storage services",
            icon = Icons.Default.CloudUpload,
            checked = true, // TODO: Add to AppSettings.dataPrivacy
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsDropdownItem(
            title = "Auto-Delete After",
            subtitle = "Automatically remove old photos from device",
            icon = Icons.Default.DeleteSweep,
            selectedValue = "Never",
            options = listOf("Never", "7 days", "30 days", "90 days"),
            onValueChange = { retention ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsToggleItem(
            title = "Include Device Information",
            subtitle = "Add device model and OS version to metadata",
            icon = Icons.Default.PhoneAndroid,
            checked = true, // TODO: Add to AppSettings.dataPrivacy
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )
    }

    SettingsSection(title = "Compliance & Security") {
        SettingsToggleItem(
            title = "Digital Signature",
            subtitle = "Cryptographically sign photos for authenticity",
            icon = Icons.Default.VerifiedUser,
            checked = false, // TODO: Add to AppSettings.compliance
            onCheckedChange = { enabled ->
                // TODO: Implement digital signature feature
            }
        )

        SettingsToggleItem(
            title = "Voice Notes",
            subtitle = "Enable audio notes attachment to photos",
            icon = Icons.Default.Mic,
            checked = false, // TODO: Add to AppSettings.compliance
            onCheckedChange = { enabled ->
                // TODO: Implement voice notes feature
            }
        )

        SettingsItem(
            title = "Mandatory Fields",
            subtitle = "Configure required metadata fields",
            icon = Icons.Default.Assignment,
            onClick = {
                // TODO: Navigate to mandatory fields configuration
            }
        )
    }

    SettingsSection(title = "Security Actions") {
        SettingsItem(
            title = "Change Security PIN",
            subtitle = "Update app lock PIN code",
            icon = Icons.Default.Pin,
            onClick = {
                // TODO: Navigate to PIN change screen
            }
        )

        SettingsItem(
            title = "Export Encrypted Backup",
            subtitle = "Create secure backup of all data",
            icon = Icons.Default.BackupTable,
            onClick = {
                // TODO: Trigger encrypted backup export
            }
        )

        SettingsItem(
            title = "Clear Cache & Temporary Files",
            subtitle = "Remove temporary data (photos remain safe)",
            icon = Icons.Default.CleaningServices,
            onClick = {
                // TODO: Clear app cache
            }
        )
    }
}

/**
 * Storage & Backup Settings
 */
@Composable
private fun StorageBackupSettings() {
    val context = LocalContext.current
    var storageStats by remember { mutableStateOf<StorageStats?>(null) }

    LaunchedEffect(Unit) {
        // TODO: Load storage statistics
        // storageStats = getStorageStats(context)
    }

    SettingsSection(title = "Storage Health") {
        // Storage Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ConstructionColors.Surface.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Storage Used",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "2.4 GB / 128 GB", // TODO: Real data
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionColors.SafetyOrange
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = ConstructionColors.SafetyOrange.copy(alpha = 0.5f)
                    )
                }

                LinearProgressIndicator(
                    progress = { 0.02f }, // TODO: Real percentage
                    modifier = Modifier.fillMaxWidth(),
                    color = ConstructionColors.SafetyOrange
                )

                Text(
                    text = "Photo Library: 1,247 photos • 2.1 GB",
                    fontSize = 12.sp,
                    color = ConstructionColors.OnSurfaceVariant
                )
            }
        }

        SettingsItem(
            title = "Manage Storage",
            subtitle = "Review and delete photos to free up space",
            icon = Icons.Default.FolderOpen,
            onClick = {
                // TODO: Navigate to storage management screen
            }
        )
    }

    SettingsSection(title = "Backup Configuration") {
        SettingsToggleItem(
            title = "Auto-Backup",
            subtitle = "Automatically backup photos to cloud storage",
            icon = Icons.Default.CloudUpload,
            checked = false, // TODO: Get from settings
            onCheckedChange = { enabled ->
                // TODO: Implement auto-backup toggle
            }
        )

        SettingsDropdownItem(
            title = "Backup Quality",
            subtitle = "Choose backup compression level",
            icon = Icons.Default.Compress,
            selectedValue = "High Quality (95%)",
            options = listOf(
                "Original (100%)",
                "High Quality (95%)",
                "Medium Quality (85%)",
                "Low Quality (75%)"
            ),
            onValueChange = { quality ->
                // TODO: Implement backup quality setting
            }
        )

        SettingsDropdownItem(
            title = "Backup Frequency",
            subtitle = "When to automatically backup photos",
            icon = Icons.Default.Schedule,
            selectedValue = "Wi-Fi Only",
            options = listOf(
                "Wi-Fi Only",
                "Wi-Fi + Cellular",
                "Manual Only"
            ),
            onValueChange = { frequency ->
                // TODO: Implement backup frequency setting
            }
        )

        SettingsItem(
            title = "Backup Now",
            subtitle = "Manually trigger backup of all photos",
            icon = Icons.Default.Backup,
            onClick = {
                // TODO: Trigger manual backup
            }
        )
    }

    SettingsSection(title = "Storage Provider") {
        SettingsItem(
            title = "Configure Cloud Storage",
            subtitle = "Connect AWS S3, Google Drive, or Dropbox",
            icon = Icons.Default.CloudQueue,
            onClick = {
                // TODO: Navigate to cloud storage configuration
            }
        )

        SettingsItem(
            title = "View Backup History",
            subtitle = "See recent backup operations and status",
            icon = Icons.Default.History,
            onClick = {
                // TODO: Navigate to backup history screen
            }
        )
    }

    SettingsSection(title = "Emergency Actions") {
        SettingsItem(
            title = "Export All Photos",
            subtitle = "Create ZIP archive of all photos",
            icon = Icons.Default.FolderZip,
            onClick = {
                // TODO: Trigger photo export
            }
        )

        SettingsItem(
            title = "Import Photos",
            subtitle = "Restore photos from backup ZIP",
            icon = Icons.Default.Unarchive,
            onClick = {
                // TODO: Trigger photo import
            }
        )
    }
}

/**
 * Display & Accessibility Settings
 * NOTE: Glass effects, Emergency Mode, and High Contrast Mode have been REMOVED
 */
@Composable
private fun DisplayAccessibilitySettings(
    uiSettings: com.hazardhawk.data.repositories.UISettings,
    uiSettingsRepository: UISettingsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    SettingsSection(title = "Display & Accessibility") {
        // Orientation Lock toggle
        SettingsToggleItem(
            title = "Lock Portrait Orientation",
            subtitle = "Prevent camera from rotating to landscape mode",
            icon = Icons.Default.PhoneAndroid,
            checked = uiSettings.orientationLock == "PORTRAIT",
            onCheckedChange = { isLocked ->
                coroutineScope.launch {
                    uiSettingsRepository.updateOrientationLock(
                        if (isLocked) "PORTRAIT" else "AUTO"
                    )
                }
            }
        )

        SettingsToggleItem(
            title = "Haptic Feedback",
            subtitle = "Vibration feedback for button presses and interactions",
            icon = Icons.Default.Vibration,
            checked = uiSettings.hapticFeedbackEnabled,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    uiSettingsRepository.updateHapticFeedback(enabled)
                }
            }
        )

        // Metadata Font Size Slider
        SettingsSliderItem(
            title = "Metadata Font Size",
            subtitle = "Adjust overlay text size (12-24sp)",
            icon = Icons.Default.FormatSize,
            value = uiSettings.metadataFontSize,
            valueRange = 12f..24f,
            onValueChange = { size ->
                coroutineScope.launch {
                    uiSettingsRepository.updateMetadataFontSize(size)
                }
            }
        )

        // Auto-Fade Delay Slider
        SettingsSliderItem(
            title = "Overlay Auto-Fade Delay",
            subtitle = "Time before overlay fades (1-10 seconds)",
            icon = Icons.Default.Timer,
            value = (uiSettings.autoFadeDelay / 1000f),
            valueRange = 1f..10f,
            onValueChange = { seconds ->
                coroutineScope.launch {
                    uiSettingsRepository.updateAutoFadeDelay((seconds * 1000).toLong())
                }
            }
        )
    }
}

/**
 * Notifications Settings
 */
@Composable
private fun NotificationsSettings() {
    SettingsSection(title = "Safety Notifications") {
        SettingsToggleItem(
            title = "Safety Alerts",
            subtitle = "Get notified of critical safety violations detected",
            icon = Icons.Default.Warning,
            checked = true, // TODO: Get from AppSettings.notifications
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsToggleItem(
            title = "Photo Reminders",
            subtitle = "Daily reminders to document safety conditions",
            icon = Icons.Default.PhotoCamera,
            checked = true, // TODO: Get from AppSettings.notifications
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )

        SettingsToggleItem(
            title = "Location Alerts",
            subtitle = "Notify when entering job site geofence",
            icon = Icons.Default.LocationOn,
            checked = false, // TODO: Get from AppSettings.notifications
            onCheckedChange = { enabled ->
                // TODO: Implement geofence notifications
            }
        )

        SettingsToggleItem(
            title = "Analysis Complete",
            subtitle = "Notification when AI analysis finishes",
            icon = Icons.Default.CheckCircle,
            checked = true, // TODO: Get from AppSettings.notifications
            onCheckedChange = { enabled ->
                // TODO: Implement in MetadataSettingsManager
            }
        )
    }

    SettingsSection(title = "Quiet Hours") {
        SettingsToggleItem(
            title = "Enable Quiet Hours",
            subtitle = "Silence non-critical notifications during specified times",
            icon = Icons.Default.DoNotDisturb,
            checked = false, // TODO: Get from AppSettings.notifications
            onCheckedChange = { enabled ->
                // TODO: Implement quiet hours toggle
            }
        )

        SettingsItem(
            title = "Quiet Hours Schedule",
            subtitle = "10:00 PM - 6:00 AM", // TODO: Get from AppSettings.notifications
            icon = Icons.Default.Schedule,
            onClick = {
                // TODO: Open time picker dialog for quiet hours
            }
        )
    }

    SettingsSection(title = "Notification Channels") {
        SettingsItem(
            title = "Manage System Notifications",
            subtitle = "Configure notification settings in system preferences",
            icon = Icons.Default.Settings,
            onClick = {
                // TODO: Open Android notification settings
                // Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            }
        )

        SettingsDropdownItem(
            title = "Notification Sound",
            subtitle = "Choose alert sound for safety notifications",
            icon = Icons.Default.Notifications,
            selectedValue = "Default",
            options = listOf("Default", "Safety Horn", "Construction Bell", "Silent"),
            onValueChange = { sound ->
                // TODO: Implement notification sound selection
            }
        )

        SettingsToggleItem(
            title = "Vibration",
            subtitle = "Vibrate for notifications",
            icon = Icons.Default.Vibration,
            checked = true, // TODO: Get from AppSettings.notifications
            onCheckedChange = { enabled ->
                // TODO: Implement vibration toggle
            }
        )
    }
}

/**
 * About & Help Settings
 */
@Composable
private fun AboutSettings() {
    SettingsSection(title = "About HazardHawk") {
        SettingsItem(
            title = "App Version",
            subtitle = "v3.1.0 Production Ready",
            icon = Icons.Default.Info,
            onClick = { /* Version info */ }
        )

        SettingsItem(
            title = "Legal & Compliance",
            subtitle = "Terms of Service, Privacy Policy, Licenses",
            icon = Icons.Default.Gavel,
            onClick = { /* Legal info */ }
        )

        SettingsItem(
            title = "Reset to Defaults",
            subtitle = "Restore all settings to their default values",
            icon = Icons.Default.RestartAlt,
            onClick = { /* Reset settings */ }
        )
    }
}

/**
 * Profile Dialog with Edit Functionality (Temporary - will be removed when Dashboard implemented)
 */
@Composable
private fun ProfileDialogWithEdit(
    userProfile: com.hazardhawk.camera.UserProfile,
    currentProject: com.hazardhawk.camera.ProjectInfo,
    projectsList: List<com.hazardhawk.camera.ProjectInfo>,
    onDismiss: () -> Unit,
    onSave: (com.hazardhawk.camera.UserProfile, com.hazardhawk.camera.ProjectInfo) -> Unit
) {
    var editedUserName by remember { mutableStateOf(userProfile.userName) }
    var editedRole by remember { mutableStateOf(userProfile.role) }
    var editedCompany by remember { mutableStateOf(userProfile.company) }
    var selectedProject by remember { mutableStateOf(currentProject) }
    var showProjectDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Profile",
                fontWeight = FontWeight.Bold,
                color = ConstructionColors.OnSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // User Name Field
                OutlinedTextField(
                    value = editedUserName,
                    onValueChange = { editedUserName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )

                // Role Field
                OutlinedTextField(
                    value = editedRole,
                    onValueChange = { editedRole = it },
                    label = { Text("Role") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null)
                    }
                )

                // Company Field
                OutlinedTextField(
                    value = editedCompany,
                    onValueChange = { editedCompany = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null)
                    }
                )

                // Project Selection Dropdown
                Text(
                    text = "Current Project",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConstructionColors.OnSurfaceVariant
                )

                Box {
                    OutlinedButton(
                        onClick = { showProjectDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Engineering, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedProject.projectName.ifBlank { "Select Project" },
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showProjectDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    DropdownMenu(
                        expanded = showProjectDropdown,
                        onDismissRequest = { showProjectDropdown = false }
                    ) {
                        projectsList.forEach { project ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = project.projectName,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (project.siteAddress.isNotBlank()) {
                                            Text(
                                                text = project.siteAddress,
                                                fontSize = 12.sp,
                                                color = ConstructionColors.OnSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedProject = project
                                    showProjectDropdown = false
                                },
                                leadingIcon = {
                                    if (project.projectId == selectedProject.projectId) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = ConstructionColors.SafetyOrange
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedProfile = userProfile.copy(
                        userName = editedUserName,
                        role = editedRole,
                        company = editedCompany
                    )
                    onSave(updatedProfile, selectedProject)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConstructionColors.SafetyOrange
                ),
                enabled = editedUserName.isNotBlank() && editedCompany.isNotBlank()
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ConstructionColors.OnSurfaceVariant)
            }
        },
        containerColor = ConstructionColors.Surface
    )
}

// Reuse components from original SettingsScreen.kt
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ConstructionColors.SafetyOrange,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ConstructionColors.Surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) ConstructionColors.SafetyOrange else ConstructionColors.OnSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConstructionColors.OnSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ConstructionColors.OnSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = ConstructionColors.SafetyOrange,
                checkedTrackColor = ConstructionColors.SafetyOrange.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SettingsDropdownItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ConstructionColors.SafetyOrange,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConstructionColors.OnSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ConstructionColors.OnSurfaceVariant,
                lineHeight = 16.sp
            )

            Box {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(selectedValue)
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ConstructionColors.SafetyOrange,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConstructionColors.OnSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ConstructionColors.OnSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = ConstructionColors.OnSurfaceVariant
        )
    }
}

/**
 * Slider Settings Item - Helper composable for slider-based settings
 */
@Composable
private fun SettingsSliderItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ConstructionColors.SafetyOrange,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ConstructionColors.OnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = ConstructionColors.OnSurfaceVariant
                )
            }

            Text(
                text = String.format("%.1f", value),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ConstructionColors.SafetyOrange
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ConstructionColors.SafetyOrange,
                activeTrackColor = ConstructionColors.SafetyOrange
            )
        )
    }
}

/**
 * Documents Settings - Quick access to safety documentation features
 */
@Composable
private fun DocumentsSettings(onNavigateToPTP: (() -> Unit)?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Safety Documentation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Pre-Task Plans
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onNavigateToPTP != null) {
                    onNavigateToPTP?.invoke()
                },
            colors = CardDefaults.cardColors(
                containerColor = if (onNavigateToPTP != null) {
                    ConstructionColors.Surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ConstructionColors.SafetyOrange
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Pre-Task Plans",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create OSHA-compliant safety documentation with AI-assisted hazard identification",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onNavigateToPTP != null) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Coming Soon: Other document types
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Coming Soon",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )

                DocumentFeatureRow(
                    icon = Icons.Default.Article,
                    title = "Incident Reports",
                    description = "Document workplace incidents and near-misses"
                )

                DocumentFeatureRow(
                    icon = Icons.Default.Groups,
                    title = "Toolbox Talks",
                    description = "Weekly safety meeting documentation"
                )

                DocumentFeatureRow(
                    icon = Icons.Default.Assignment,
                    title = "JSAs",
                    description = "Job Safety Analysis documents"
                )
            }
        }
    }
}

@Composable
private fun DocumentFeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Data class for storage statistics
 */
data class StorageStats(
    val totalSpace: Long,
    val usedSpace: Long,
    val photoCount: Int,
    val photoSize: Long
)
