package com.hazardhawk.ui.camera

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.data.repositories.CameraSettingsRepository
import com.hazardhawk.data.models.*
import com.hazardhawk.security.SecureKeyManager
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Professional camera settings dialog for construction site documentation.
 * Inspired by Timemark but tailored for construction safety and compliance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraSettingsDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            CameraSettingsContent(
                onDismiss = onDismiss,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CameraSettingsContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    // Inject repository and secure key manager
    val cameraSettingsRepository: CameraSettingsRepository = koinInject()
    val context = androidx.compose.ui.platform.LocalContext.current
    val secureKeyManager = remember { SecureKeyManager.getInstance(context) }

    // Load settings from repository
    val cameraSettings by cameraSettingsRepository.getSettingsFlow().collectAsStateWithLifecycle()

    // Initialize repository on first composition
    LaunchedEffect(Unit) {
        cameraSettingsRepository.loadSettings()
    }

    // Repository-backed state management - reactive to changes
    val aiMode by remember(cameraSettings.aiMode) { mutableStateOf(AIMode.fromString(cameraSettings.aiMode)) }
    val selectedModel by remember(cameraSettings.selectedModel) { mutableStateOf(AIModelQuality.fromString(cameraSettings.selectedModel)) }
    val aiConfidenceThreshold by remember(cameraSettings.confidenceThreshold) { mutableStateOf(cameraSettings.confidenceThreshold) }
    val autoAnalyzePhotos by remember(cameraSettings.autoAnalyzePhotos) { mutableStateOf(cameraSettings.autoAnalyzePhotos) }
    val arEnabled by remember(cameraSettings.arEnabled) { mutableStateOf(cameraSettings.arEnabled) }
    val arPrivacyMode by remember(cameraSettings.arPrivacyMode) { mutableStateOf(ARPrivacyMode.fromString(cameraSettings.arPrivacyMode)) }
    val arPerformanceMode by remember(cameraSettings.arPerformanceMode) { mutableStateOf(ARPerformanceMode.fromString(cameraSettings.arPerformanceMode)) }
    val gridLines by remember(cameraSettings.showGrid) { mutableStateOf(cameraSettings.showGrid) }
    val autoTagging by remember(cameraSettings.autoTaggingEnabled) { mutableStateOf(cameraSettings.autoTaggingEnabled) }
    val flashMode by remember(cameraSettings.flashMode) { mutableStateOf(FlashMode.fromString(cameraSettings.flashMode).value) }
    val photoQuality by remember(cameraSettings.imageQuality) { mutableStateOf(when(cameraSettings.imageQuality) {
        in 90..100 -> "High"
        in 70..89 -> "Medium"
        else -> "Low"
    }) }

    // API Key state (not persisted in CameraSettings)
    var geminiApiKey by remember { mutableStateOf(secureKeyManager.getGeminiApiKey() ?: "") }
    var showApiKey by remember { mutableStateOf(false) }

    // Legacy settings - these will be kept as local state for now until added to CameraSettings model
    var workType by remember { mutableStateOf(WorkType.GENERAL_CONSTRUCTION) }
    var autoDetectOSHA by remember { mutableStateOf(true) }
    var hazardOverlay by remember { mutableStateOf(true) }

    var gpsEnabled by remember { mutableStateOf(cameraSettings.requireGPSForPhotos) }

    // Add MetadataSettingsManager for GPS display preference
    val metadataSettingsManager = remember { com.hazardhawk.camera.MetadataSettingsManager(context) }
    val appSettings by metadataSettingsManager.appSettings.collectAsStateWithLifecycle()
    var showGPSCoordinates by remember(appSettings.dataPrivacy.showGPSCoordinates) { mutableStateOf(appSettings.dataPrivacy.showGPSCoordinates) }
    var projectOverlay by remember { mutableStateOf(cameraSettings.showMetadata) }
    var workerIdRequired by remember { mutableStateOf(false) }
    var autoTimestamp by remember { mutableStateOf(true) }
    var environmentalData by remember { mutableStateOf(false) }

    var autoBackup by remember { mutableStateOf(false) }

    var digitalSignature by remember { mutableStateOf(false) }
    var mandatoryFields by remember { mutableStateOf(false) }
    var voiceNotes by remember { mutableStateOf(false) }

    var timerEnabled by remember { mutableStateOf(false) }
    var timerDelay by remember { mutableStateOf(3) }

    // Loading and error states
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Helper functions to update repository
    suspend fun updateAIMode(newMode: AIMode) {
        try {
            isLoading = true
            errorMessage = null
            val updated = cameraSettings.copy(aiMode = newMode.value)
            cameraSettingsRepository.saveSettings(updated)
        } catch (e: Exception) {
            errorMessage = "Failed to save AI mode: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun updateConfidenceThreshold(threshold: Float) {
        try {
            isLoading = true
            errorMessage = null
            val updated = cameraSettings.copy(confidenceThreshold = threshold)
            cameraSettingsRepository.saveSettings(updated)
        } catch (e: Exception) {
            errorMessage = "Failed to save confidence threshold: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun updateAutoAnalyze(enabled: Boolean) {
        try {
            isLoading = true
            errorMessage = null
            val updated = cameraSettings.copy(autoAnalyzePhotos = enabled)
            cameraSettingsRepository.saveSettings(updated)
        } catch (e: Exception) {
            errorMessage = "Failed to save auto-analyze setting: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun updateARSettings(enabled: Boolean, privacy: ARPrivacyMode? = null, performance: ARPerformanceMode? = null) {
        try {
            isLoading = true
            errorMessage = null
            val updated = cameraSettings.copy(
                arEnabled = enabled,
                arPrivacyMode = privacy?.value ?: cameraSettings.arPrivacyMode,
                arPerformanceMode = performance?.value ?: cameraSettings.arPerformanceMode
            )
            cameraSettingsRepository.saveSettings(updated)
        } catch (e: Exception) {
            errorMessage = "Failed to save AR settings: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun updateGridSettings(show: Boolean) {
        try {
            isLoading = true
            errorMessage = null
            cameraSettingsRepository.updateGridSettings(show, GridType.fromString(cameraSettings.gridType))
        } catch (e: Exception) {
            errorMessage = "Failed to save grid settings: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun updateFlashMode(mode: FlashMode) {
        try {
            isLoading = true
            errorMessage = null
            cameraSettingsRepository.updateFlashMode(mode)
        } catch (e: Exception) {
            errorMessage = "Failed to save flash mode: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    suspend fun updateImageQuality(quality: String) {
        try {
            isLoading = true
            errorMessage = null
            val qualityValue = when(quality) {
                "High" -> 95
                "Medium" -> 75
                "Low" -> 60
                else -> 95
            }
            cameraSettingsRepository.updateImageQuality(qualityValue)
        } catch (e: Exception) {
            errorMessage = "Failed to save image quality: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Camera Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ConstructionColors.SafetyOrange,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Settings Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                
                // AI & Analysis Section
                SettingsSection(
                    title = "AI Analysis & Safety",
                    icon = Icons.Default.Psychology
                ) {
                    DropdownSetting(
                        title = "AI Mode",
                        subtitle = "Choose AI analysis method",
                        selectedValue = aiMode.displayName,
                        options = AIMode.values().map { it.displayName },
                        onSelectionChange = { displayName ->
                            val selectedMode = AIMode.values().first { it.displayName == displayName }
                            coroutineScope.launch {
                                updateAIMode(selectedMode)
                            }
                        }
                    )

                    // API Key Input for Cloud/Smart modes
                    if (aiMode == AIMode.CLOUD || aiMode == AIMode.SMART) {
                        ApiKeyInputSetting(
                            title = "Gemini API Key",
                            subtitle = "Required for cloud-based AI analysis",
                            value = geminiApiKey,
                            showValue = showApiKey,
                            onValueChange = { key ->
                                geminiApiKey = key
                                coroutineScope.launch {
                                    try {
                                        secureKeyManager.storeGeminiApiKey(key)
                                        val updated = cameraSettings.copy(geminiApiKeyConfigured = key.isNotBlank())
                                        cameraSettingsRepository.saveSettings(updated)
                                    } catch (e: Exception) {
                                        errorMessage = "Failed to save API key: ${e.message}"
                                    }
                                }
                            },
                            onToggleVisibility = { showApiKey = !showApiKey }
                        )
                    }

                    // Model Selection for On-Device/Smart modes
                    if (aiMode == AIMode.ON_DEVICE || aiMode == AIMode.SMART) {
                        DropdownSetting(
                            title = "AI Model Quality",
                            subtitle = "${selectedModel.displayName} (${selectedModel.sizeMB}MB)",
                            selectedValue = selectedModel.displayName,
                            options = AIModelQuality.values().map { it.displayName },
                            onSelectionChange = { displayName ->
                                val selected = AIModelQuality.values().first { it.displayName == displayName }
                                coroutineScope.launch {
                                    val updated = cameraSettings.copy(selectedModel = selected.value)
                                    cameraSettingsRepository.saveSettings(updated)
                                }
                            }
                        )
                    }

                    if (aiMode != AIMode.OFF) {
                        SliderSetting(
                            title = "AI Confidence Threshold",
                            subtitle = "${(aiConfidenceThreshold * 100).toInt()}% - Minimum detection confidence",
                            value = aiConfidenceThreshold,
                            onValueChange = { value ->
                                coroutineScope.launch {
                                    updateConfidenceThreshold(value)
                                }
                            },
                            valueRange = 0.5f..0.95f
                        )

                        SwitchSetting(
                            title = "Auto-analyze Photos",
                            subtitle = "Automatically analyze photos after capture",
                            checked = autoAnalyzePhotos,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch {
                                    updateAutoAnalyze(enabled)
                                }
                            }
                        )

                        DropdownSetting(
                            title = "Work Type",
                            subtitle = "Select primary construction activity",
                            selectedValue = workType.name,
                            options = WorkType.values().map { it.name },
                            onSelectionChange = { selectedName ->
                                workType = WorkType.values().first { it.name == selectedName }
                            }
                        )

                        SwitchSetting(
                            title = "Auto-detect OSHA Violations",
                            subtitle = "Highlight compliance issues automatically",
                            checked = autoDetectOSHA,
                            onCheckedChange = { autoDetectOSHA = it }
                        )

                        SwitchSetting(
                            title = "Hazard Detection Overlay",
                            subtitle = "Show hazard boxes in live preview",
                            checked = hazardOverlay,
                            onCheckedChange = { hazardOverlay = it }
                        )
                    }
                }

                // AR Mode Configuration Section
                SettingsSection(
                    title = "Augmented Reality",
                    icon = Icons.Default.ViewInAr
                ) {
                    SwitchSetting(
                        title = "AR Mode",
                        subtitle = "Enable augmented reality overlays",
                        checked = arEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                updateARSettings(enabled)
                            }
                        }
                    )

                    if (arEnabled) {
                        DropdownSetting(
                            title = "AR Privacy Mode",
                            subtitle = "Control data collection and processing",
                            selectedValue = arPrivacyMode.value,
                            options = ARPrivacyMode.values().map { it.value },
                            onSelectionChange = { value ->
                                val selectedPrivacy = ARPrivacyMode.fromString(value)
                                coroutineScope.launch {
                                    updateARSettings(arEnabled, privacy = selectedPrivacy)
                                }
                            }
                        )

                        DropdownSetting(
                            title = "AR Performance Mode",
                            subtitle = "Balance performance and battery life",
                            selectedValue = arPerformanceMode.value,
                            options = ARPerformanceMode.values().map { it.value },
                            onSelectionChange = { value ->
                                val selectedPerformance = ARPerformanceMode.fromString(value)
                                coroutineScope.launch {
                                    updateARSettings(arEnabled, performance = selectedPerformance)
                                }
                            }
                        )
                    }
                }
                
                // Metadata & Documentation Section
                SettingsSection(
                    title = "Metadata & Documentation",
                    icon = Icons.Default.LocationOn
                ) {
                    // GPS Display Setting (simplified from two separate settings)
                    val gpsDisplayOptions = listOf("Off", "Address", "Coordinates")
                    val currentGpsOption = when {
                        !gpsEnabled -> "Off"
                        showGPSCoordinates -> "Coordinates"
                        else -> "Address"
                    }

                    DropdownSetting(
                        title = "GPS Display",
                        subtitle = "How location appears in photos and viewfinder",
                        selectedValue = currentGpsOption,
                        options = gpsDisplayOptions,
                        onSelectionChange = { newOption ->
                            when (newOption) {
                                "Off" -> {
                                    gpsEnabled = false
                                    showGPSCoordinates = false
                                    coroutineScope.launch {
                                        val updated = cameraSettings.copy(requireGPSForPhotos = false)
                                        cameraSettingsRepository.saveSettings(updated)
                                        metadataSettingsManager.updateShowGPSCoordinates(false)
                                    }
                                }
                                "Address" -> {
                                    gpsEnabled = true
                                    showGPSCoordinates = false
                                    coroutineScope.launch {
                                        val updated = cameraSettings.copy(requireGPSForPhotos = true)
                                        cameraSettingsRepository.saveSettings(updated)
                                        metadataSettingsManager.updateShowGPSCoordinates(false)
                                    }
                                }
                                "Coordinates" -> {
                                    gpsEnabled = true
                                    showGPSCoordinates = true
                                    coroutineScope.launch {
                                        val updated = cameraSettings.copy(requireGPSForPhotos = true)
                                        cameraSettingsRepository.saveSettings(updated)
                                        metadataSettingsManager.updateShowGPSCoordinates(true)
                                    }
                                }
                            }
                        }
                    )
                    
                    SwitchSetting(
                        title = "Project Information Overlay",
                        subtitle = "Show project name and site details",
                        checked = projectOverlay,
                        onCheckedChange = { enabled ->
                            projectOverlay = enabled
                            coroutineScope.launch {
                                cameraSettingsRepository.updateMetadataSettings(
                                    enabled,
                                    MetadataPosition.fromString(cameraSettings.metadataPosition)
                                )
                            }
                        }
                    )
                    
                    SwitchSetting(
                        title = "Worker ID Required",
                        subtitle = "Require worker identification for photos",
                        checked = workerIdRequired,
                        onCheckedChange = { workerIdRequired = it }
                    )
                    
                    SwitchSetting(
                        title = "Auto-timestamp",
                        subtitle = "Automatically add date/time to photos",
                        checked = autoTimestamp,
                        onCheckedChange = { autoTimestamp = it }
                    )
                    
                    SwitchSetting(
                        title = "Environmental Data",
                        subtitle = "Include weather and temperature",
                        checked = environmentalData,
                        onCheckedChange = { environmentalData = it }
                    )
                }
                
                // Photo Quality & Storage Section
                SettingsSection(
                    title = "Photo Quality & Storage",
                    icon = Icons.Default.PhotoCamera
                ) {
                    DropdownSetting(
                        title = "Photo Quality",
                        subtitle = "Balance quality and file size",
                        selectedValue = photoQuality,
                        options = listOf("High", "Medium", "Low"),
                        onSelectionChange = { quality ->
                            coroutineScope.launch {
                                updateImageQuality(quality)
                            }
                        }
                    )
                    
                    SwitchSetting(
                        title = "Auto-backup",
                        subtitle = "Automatically sync to cloud storage",
                        checked = autoBackup,
                        onCheckedChange = { autoBackup = it }
                    )
                    
                    SwitchSetting(
                        title = "Grid Lines",
                        subtitle = "Show grid overlay for better composition",
                        checked = gridLines,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                updateGridSettings(enabled)
                            }
                        }
                    )
                    
                    SwitchSetting(
                        title = "Auto-tagging",
                        subtitle = "Automatically tag photos by location/project",
                        checked = autoTagging,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                val updated = cameraSettings.copy(autoTaggingEnabled = enabled)
                                cameraSettingsRepository.saveSettings(updated)
                            }
                        }
                    )
                }
                
                // Compliance & Security Section
                SettingsSection(
                    title = "Compliance & Security",
                    icon = Icons.Default.Security
                ) {
                    SwitchSetting(
                        title = "Digital Signature",
                        subtitle = "Add cryptographic proof of authenticity",
                        checked = digitalSignature,
                        onCheckedChange = { digitalSignature = it }
                    )
                    
                    SwitchSetting(
                        title = "Mandatory Fields",
                        subtitle = "Require metadata completion before capture",
                        checked = mandatoryFields,
                        onCheckedChange = { mandatoryFields = it }
                    )
                    
                    SwitchSetting(
                        title = "Voice Notes",
                        subtitle = "Record audio annotations with photos",
                        checked = voiceNotes,
                        onCheckedChange = { voiceNotes = it }
                    )
                }
                
                // Camera Controls Section
                SettingsSection(
                    title = "Camera Controls",
                    icon = Icons.Default.CameraAlt
                ) {
                    DropdownSetting(
                        title = "Flash Mode",
                        subtitle = "Control camera flash behavior",
                        selectedValue = flashMode,
                        options = listOf("Auto", "On", "Off", "Red-eye Reduction"),
                        onSelectionChange = { mode ->
                            coroutineScope.launch {
                                val flashModeEnum = when(mode) {
                                    "Auto" -> FlashMode.AUTO
                                    "On" -> FlashMode.ON
                                    "Off" -> FlashMode.OFF
                                    "Red-eye Reduction" -> FlashMode.AUTO // Map to auto as fallback
                                    else -> FlashMode.AUTO
                                }
                                updateFlashMode(flashModeEnum)
                            }
                        }
                    )
                    
                    SwitchSetting(
                        title = "Timer",
                        subtitle = "Delayed capture for hands-free operation",
                        checked = timerEnabled,
                        onCheckedChange = { timerEnabled = it }
                    )
                    
                    if (timerEnabled) {
                        DropdownSetting(
                            title = "Timer Delay",
                            subtitle = "Seconds before capture",
                            selectedValue = "${timerDelay}s",
                            options = listOf("3s", "5s", "10s"),
                            onSelectionChange = { 
                                timerDelay = it.removeSuffix("s").toInt()
                            }
                        )
                    }
                }
                
                // Error message display
                errorMessage?.let { message ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Loading indicator
                if (isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = ConstructionColors.SafetyOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Saving settings...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ConstructionColors.SafetyOrange,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ConstructionColors.SafetyOrange
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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
private fun SliderSetting(
    title: String,
    subtitle: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = ConstructionColors.SafetyOrange,
                activeTrackColor = ConstructionColors.SafetyOrange,
                inactiveTrackColor = ConstructionColors.SafetyOrange.copy(alpha = 0.3f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSetting(
    title: String,
    subtitle: String,
    selectedValue: String,
    options: List<String>,
    onSelectionChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = { },
                readOnly = true,
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ConstructionColors.SafetyOrange,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectionChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyInputSetting(
    title: String,
    subtitle: String,
    value: String,
    showValue: Boolean,
    onValueChange: (String) -> Unit,
    onToggleVisibility: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your Gemini API key") },
            visualTransformation = if (showValue) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleVisibility()
                }) {
                    Icon(
                        imageVector = if (showValue) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showValue) "Hide API key" else "Show API key"
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ConstructionColors.SafetyOrange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )
    }
}