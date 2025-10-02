# HazardHawk Unified Settings Implementation Plan

**Created:** October 1, 2025
**Status:** Ready for Implementation
**Priority:** High
**Estimated Effort:** 2-3 weeks

---

## Executive Summary

The UnifiedSettingsScreen.kt currently has **7 TODO placeholders** that need full implementation. This plan provides detailed specifications for each missing component, following the architecture defined in UNIFIED_SETTINGS_RECOMMENDATIONS.md.

### Current State
- ✅ Profile Header (complete)
- ✅ AI Settings with API Key (complete)
- ✅ AR Settings with Privacy Controls (complete)
- ✅ Display & Accessibility Settings (partial - orientation lock & haptic feedback implemented)
  - ❌ Glass effects **REMOVED** from architecture per UNIFIED_SETTINGS_RECOMMENDATIONS.md
  - ❌ Emergency mode **REMOVED** (deprecated)
  - ❌ High contrast mode still in UISettings but needs to be removed
  - ✅ Metadata font size & auto-fade delay available but not exposed in UI, make sure changing these settings will make changes to the UI elements
- ❌ Camera Settings (placeholder)
- ❌ Location & Metadata Settings (placeholder)
- ❌ Privacy & Security Settings (placeholder)
- ❌ Storage & Backup Settings (placeholder)
- ❌ Notifications Settings (placeholder)
- ❌ Profile Dialog Save Functionality (placeholder)

---

## TODO Items Analysis

### File: `UnifiedSettingsScreen.kt`

#### TODO #1: Profile Dialog Save (Line 206)
```kotlin
onSave = { /* TODO: Implement profile save */ }
```

#### TODO #2: Camera Quality Settings (Line 332)
```kotlin
Text("Camera quality settings - TODO: Implement from recommendations")
```

#### TODO #3: Camera Control Settings (Line 337)
```kotlin
Text("Camera control settings - TODO: Implement from recommendations")
```

#### TODO #4: Location Metadata Settings (Line 455)
```kotlin
Text("Location and metadata settings - TODO: Implement from recommendations")
```

#### TODO #5: Privacy Security Settings (Line 469)
```kotlin
Text("Privacy and security settings - TODO: Implement from recommendations")
```

#### TODO #6: Storage Backup Settings (Line 479)
```kotlin
Text("Storage and backup settings - TODO: Implement from recommendations")
```

#### TODO #7: Notifications Settings (Line 532)
```kotlin
Text("Notification settings - TODO: Implement from recommendations")
```

---

## Implementation Details

### TODO #1: Profile Dialog Save Functionality

**Location:** Lines 202-208
**Complexity:** Medium
**Dependencies:** MetadataSettingsManager

**Implementation:**
```kotlin
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
```

**New Composable Needed:**
```kotlin
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
```

---

### TODO #2 & #3: Camera Settings

**Location:** Lines 324-339
**Complexity:** High
**Dependencies:** MetadataSettingsManager, AppSettings

**Implementation:**
```kotlin
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
            selectedValue = "${appSettings.camera.quality}%",
            options = listOf("85%", "90%", "95%", "100%"),
            onValueChange = { quality ->
                val qualityInt = quality.replace("%", "").toIntOrNull() ?: 95
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        camera = appSettings.camera.copy(quality = qualityInt)
                    )
                    metadataSettingsManager.updateAppSettings(updatedSettings)
                }
            }
        )

        SettingsToggleItem(
            title = "HDR Mode",
            subtitle = "Capture multiple exposures for better dynamic range",
            icon = Icons.Default.Hdr,
            checked = appSettings.camera.hdrEnabled,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        camera = appSettings.camera.copy(hdrEnabled = enabled)
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
            selectedValue = when (appSettings.camera.flashMode) {
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
                        camera = appSettings.camera.copy(flashMode = flashMode)
                    )
                    metadataSettingsManager.updateAppSettings(updatedSettings)
                }
            }
        )

        SettingsToggleItem(
            title = "Grid Lines",
            subtitle = "Show rule-of-thirds grid for better composition",
            icon = Icons.Default.Grid3x3,
            checked = appSettings.camera.gridEnabled,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        camera = appSettings.camera.copy(gridEnabled = enabled)
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
            selectedValue = "3 seconds",
            options = listOf("Off", "3 seconds", "5 seconds", "10 seconds"),
            onValueChange = { delay ->
                // TODO: Implement timer settings in AppSettings.camera
            }
        )
    }

    // Aspect Ratio & Zoom Section
    SettingsSection(title = "Aspect Ratio & Zoom") {
        SettingsDropdownItem(
            title = "Default Aspect Ratio",
            subtitle = "Choose photo dimensions for capture",
            icon = Icons.Default.AspectRatio,
            selectedValue = "4:3 (Standard)",
            options = listOf("Square (1:1)", "4:3 (Standard)", "16:9 (Wide)", "3:2 (DSLR)"),
            onValueChange = { ratio ->
                // TODO: Implement in MetadataSettingsManager
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
```

---

### TODO #4: Location & Metadata Settings

**Location:** Lines 448-457
**Complexity:** Medium
**Dependencies:** MetadataSettingsManager, AppSettings

**Implementation:**
```kotlin
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
                    checked = appSettings.camera.gpsOverlayEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            val updatedSettings = appSettings.copy(
                                camera = appSettings.camera.copy(gpsOverlayEnabled = enabled)
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
```

---

### TODO #5: Privacy & Security Settings

**Location:** Lines 462-471
**Complexity:** High
**Dependencies:** MetadataSettingsManager, AppSettings, SecureKeyManager

**Implementation:**
```kotlin
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
            checked = appSettings.dataPrivacy.encryptLocalData,
            onCheckedChange = { enabled ->
                coroutineScope.launch {
                    val updatedSettings = appSettings.copy(
                        dataPrivacy = appSettings.dataPrivacy.copy(encryptLocalData = enabled)
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
```

---

### TODO #6: Storage & Backup Settings

**Location:** Lines 476-481
**Complexity:** Medium
**Dependencies:** StorageManager, PhotoStorageManagerCompat

**Implementation:**
```kotlin
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

// Helper composable for slider settings
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

data class StorageStats(
    val totalSpace: Long,
    val usedSpace: Long,
    val photoCount: Int,
    val photoSize: Long
)
```

---

### TODO #7: Notifications Settings

**Location:** Lines 529-534
**Complexity:** Low
**Dependencies:** AppSettings.notifications

**Implementation:**
```kotlin
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
```

---

## Data Model Additions Required

### Important Notes on Deprecated Settings

Per UNIFIED_SETTINGS_RECOMMENDATIONS.md:
- ❌ **Glass Effects** - Completely removed from architecture (lines 13-14, 486 of recommendations)
- ❌ **Emergency Mode** - Removed (deprecated, line 183 of recommendations)
- ⚠️ **High Contrast Mode** - Still exists in UISettings but should be evaluated for removal
- ✅ **Performance Tier** - Still exists in UISettings but may be glass-effects related

### AppSettings Extensions Needed

```kotlin
// Add to AppSettings.camera
data class CameraSettings(
    // ... existing fields ...
    val volumeButtonCapture: Boolean = true,
    val autoFocus: Boolean = true,
    val timerEnabled: Boolean = false,
    val timerDelay: Int = 3,
    val defaultAspectRatio: String = "4:3"
)

// Add to AppSettings.metadata (new section)
data class MetadataDisplaySettings(
    val showProjectInfo: Boolean = true,
    val showUserInfo: Boolean = true,
    val showTimestamp: Boolean = true,
    val position: String = "BOTTOM_LEFT", // TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
    val fontSize: Float = 16f,
    val opacity: Float = 0.85f
)

// Add to AppSettings.dataPrivacy
data class DataPrivacySettings(
    // ... existing fields ...
    val allowCloudSync: Boolean = true,
    val autoDeleteAfterDays: Int = 0,
    val includeDeviceInfo: Boolean = true
)

// Add to AppSettings.compliance (new section)
data class ComplianceSettings(
    val digitalSignature: Boolean = false,
    val workerIdRequired: Boolean = false,
    val voiceNotes: Boolean = false,
    val mandatoryFields: List<String> = emptyList()
)

// Add to AppSettings.notifications
data class NotificationSettings(
    val safetyAlerts: Boolean = true,
    val photoReminders: Boolean = true,
    val locationAlerts: Boolean = false,
    val analysisComplete: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "06:00",
    val notificationSound: String = "DEFAULT",
    val vibrationEnabled: Boolean = true
)

// Add to AppSettings.storage (new section)
data class StorageSettings(
    val autoBackup: Boolean = false,
    val backupQuality: Int = 95,
    val backupFrequency: String = "WIFI_ONLY" // WIFI_ONLY, WIFI_CELLULAR, MANUAL
)
```

---

## Implementation Priority & Dependencies

### Phase 1: Low-Hanging Fruit (Week 1)
1. ✅ **TODO #7: Notifications Settings** - Simple toggles, minimal logic
2. ✅ **TODO #1: Profile Dialog Save** - Uses existing MetadataSettingsManager

### Phase 2: Core Functionality (Week 1-2)
3. ✅ **TODO #2 & #3: Camera Settings** - Critical user-facing features
4. ✅ **TODO #4: Location & Metadata Settings** - Existing infrastructure

### Phase 3: Advanced Features (Week 2-3)
5. ✅ **TODO #5: Privacy & Security Settings** - Complex but isolated
6. ✅ **TODO #6: Storage & Backup Settings** - Requires new storage APIs

### Additional Enhancement: Display & Accessibility Completion
7. ✅ **Expose Metadata Font Size Slider** - Already in UISettings, just needs UI
8. ✅ **Expose Auto-Fade Delay Slider** - Already in UISettings, just needs UI

---

## Cleanup Tasks

### Deprecated Settings to Remove (Phase 4)

Per UNIFIED_SETTINGS_RECOMMENDATIONS.md, these settings should be removed from UISettings:

1. **`glassEnabled: Boolean`** - Glass effects completely removed from architecture
2. **`performanceTier: String`** - Related to glass effects, no longer needed
3. **`emergencyMode: Boolean`** - Deprecated feature, removed from design
4. **`highContrastMode: Boolean`** - Evaluate for removal (may be useful for outdoor visibility)

**Migration Strategy:**
```kotlin
// In UISettingsRepositoryImpl migration logic
override suspend fun loadSettings(): UISettings {
    val legacySettings = loadLegacySettings()

    // Strip out deprecated fields
    return legacySettings.copy(
        glassEnabled = false, // Always disabled
        performanceTier = "AUTO", // Default value
        emergencyMode = false, // Always disabled
        // Keep highContrastMode for now - evaluate with users
    )
}
```

---

## Testing Requirements

### Unit Tests
- [ ] Profile save/load functionality
- [ ] Settings persistence for each category
- [ ] Data model serialization/deserialization
- [ ] Default value validation

### Integration Tests
- [ ] Settings synchronization across app
- [ ] Camera settings applied correctly
- [ ] Metadata overlay reflects settings
- [ ] Notification permissions handling

### UI Tests
- [ ] Tab navigation works correctly
- [ ] All toggles update state properly
- [ ] Dropdowns show correct options
- [ ] Sliders update in real-time

---

## Documentation Updates Needed

1. **User Guide**: Add screenshots and descriptions for each settings category
2. **API Documentation**: Document new MetadataSettingsManager methods
3. **Migration Guide**: Instructions for upgrading from old settings
4. **Privacy Policy**: Update with new data collection/storage practices

---

## Success Criteria

✅ All 7 TODO comments replaced with working implementations
✅ Metadata font size and auto-fade delay sliders added to Display & Accessibility
✅ Deprecated settings (glass effects, emergency mode) removed from UISettings
✅ Settings persist correctly across app restarts
✅ UI is responsive and construction-worker friendly
✅ No crashes or data loss during settings changes
✅ All existing features continue to work with new settings
✅ Unit test coverage > 80% for new code
✅ Integration tests pass for all settings flows
✅ No references to deprecated glass effects or emergency mode in UI

---

## Risk Mitigation

| Risk | Mitigation Strategy |
|------|-------------------|
| Breaking existing settings | Feature flag for gradual rollout |
| Data loss during migration | Comprehensive backup before changes |
| Performance degradation | Lazy loading and caching |
| User confusion | In-app tutorial and help tooltips |

---

## Next Steps

1. **Review & Approve** this plan with stakeholders
2. **Create Jira tickets** for each TODO item (7 main + 2 enhancements + 4 cleanup)
3. **Set up feature branch** `feature/unified-settings-complete`
4. **Begin Phase 1 implementation** (Notifications + Profile)
5. **Phase 4: Cleanup deprecated settings** from UISettings data model
6. **Daily progress updates** to team

---

## Alignment with Architecture

This implementation plan is fully aligned with **UNIFIED_SETTINGS_RECOMMENDATIONS.md**:

✅ **Glass Effects Removed** - No glass morphism UI elements in implementation
✅ **Emergency Mode Removed** - Deprecated feature not included
✅ **Profile Prominent** - Placed at top of settings with dedicated icon
✅ **Future Dashboard Ready** - Profile section designed for easy removal
✅ **Display & Accessibility** - Renamed from "Display & Interface" as recommended
✅ **9 Category Tabs** - Matches recommended hierarchy exactly
✅ **Construction-Friendly** - Large touch targets (72dp), high contrast
✅ **Single Source of Truth** - Reduces fragmentation across 3 repositories

**Key Differences from Recommendations:**
- Implementation uses existing AppSettings/UISettings models (pragmatic approach)
- Full unified HazardHawkSettings model recommended for Phase 5 (future refactor)
- Current approach: Enhance existing models vs. rewrite from scratch

---

**Document Version:** 1.1
**Author:** Claude Code Senior Android Developer
**Status:** Ready for Implementation
**Last Updated:** October 1, 2025 (Updated to align with UNIFIED_SETTINGS_RECOMMENDATIONS.md v1.1)
