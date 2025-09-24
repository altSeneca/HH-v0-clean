package com.hazardhawk.ui.settings

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.ui.theme.HazardColors
import com.hazardhawk.security.SecureKeyManager
import com.hazardhawk.ui.components.APIKeySetupCard
import com.hazardhawk.ui.components.validateAPIKey
import com.hazardhawk.data.repositories.UISettingsRepository
import org.koin.compose.koinInject

/**
 * Settings screen with glass morphism toggle for preview functionality.
 * 
 * Features:
 * - Glass effects toggle with live preview
 * - Construction-optimized settings
 * - Performance tier selection
 * - OSHA compliance settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    glassEnabled: Boolean = false,
    onGlassToggle: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val secureKeyManager = remember { SecureKeyManager.getInstance(context) }
    val uiSettingsRepository: UISettingsRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()

    // Load settings from repository
    val uiSettings by uiSettingsRepository.getSettingsFlow().collectAsStateWithLifecycle()

    // Initialize repository on first composition
    LaunchedEffect(Unit) {
        uiSettingsRepository.loadSettings()
    }
    
    // API Key state
    var apiKey by remember { mutableStateOf(secureKeyManager.getGeminiApiKey() ?: "") }
    var isApiKeyExpanded by remember { mutableStateOf(false) }
    val isApiKeyValid by remember { derivedStateOf { validateAPIKey(apiKey).isEmpty() } }
    
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
        
        // Settings Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Configuration Section
            SettingsSection(title = "AI Configuration") {
                APIKeySetupCard(
                    apiKey = apiKey,
                    onApiKeyChange = { newKey ->
                        apiKey = newKey
                        if (newKey.isNotEmpty() && isApiKeyValid) {
                            try {
                                secureKeyManager.storeGeminiApiKey(newKey, "user_entered_${System.currentTimeMillis()}")
                                android.util.Log.i("SettingsScreen", "API key stored successfully")
                            } catch (e: Exception) {
                                // Handle storage error - could show a snackbar or toast
                                android.util.Log.e("SettingsScreen", "Failed to store API key: ${e.message}", e)
                            }
                        }
                    },
                    isValid = isApiKeyValid,
                    expanded = isApiKeyExpanded,
                    onExpandedChange = { isApiKeyExpanded = it }
                )
            }

            // Camera Settings Section
            SettingsSection(title = "Camera Settings") {
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
            }

            // Glass Interface Settings
            SettingsSection(title = "Glass Interface") {
                // Glass Toggle
                SettingsToggleItem(
                    title = "Enable Glass Effects",
                    subtitle = "Turn on glass morphism UI components for a premium visual experience",
                    icon = Icons.Default.AutoAwesome,
                    checked = uiSettings.glassEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            uiSettingsRepository.updateGlassEnabled(enabled)
                        }
                        onGlassToggle(enabled)
                    }
                )
                
                // Performance Tier Selection
                AnimatedVisibility(
                    visible = uiSettings.glassEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    SettingsDropdownItem(
                        title = "Performance Tier",
                        subtitle = "Adjust glass effects based on device capability",
                        icon = Icons.Default.Speed,
                        selectedValue = uiSettings.performanceTier,
                        options = listOf("AUTO", "HIGH", "MEDIUM", "LOW", "EMERGENCY"),
                        onValueChange = { tier ->
                            coroutineScope.launch {
                                uiSettingsRepository.updatePerformanceTier(tier)
                            }
                        }
                    )
                }
            }
            
            // Safety & Accessibility Settings
            SettingsSection(title = "Safety & Accessibility") {
                SettingsToggleItem(
                    title = "Emergency Mode",
                    subtitle = "High contrast, solid backgrounds for safety alerts",
                    icon = Icons.Default.Warning,
                    checked = uiSettings.emergencyMode,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            uiSettingsRepository.updateEmergencyMode(enabled)
                        }
                    }
                )

                SettingsToggleItem(
                    title = "High Contrast Mode",
                    subtitle = "Increased contrast for outdoor visibility",
                    icon = Icons.Default.Contrast,
                    checked = uiSettings.highContrastMode,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            uiSettingsRepository.updateHighContrastMode(enabled)
                        }
                    }
                )

                SettingsToggleItem(
                    title = "AR Safety Mode",
                    subtitle = "Enable augmented reality hazard detection overlays",
                    icon = Icons.Default.ViewInAr,
                    checked = uiSettings.arEnabled,
                    onCheckedChange = { enabled ->
                        coroutineScope.launch {
                            val currentSettings = uiSettingsRepository.loadSettings()
                            val updatedSettings = currentSettings.copy(arEnabled = enabled)
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
                                    val currentSettings = uiSettingsRepository.loadSettings()
                                    val updatedSettings = currentSettings.copy(facialAnonymizationEnabled = enabled)
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
                                    val currentSettings = uiSettingsRepository.loadSettings()
                                    val updatedSettings = currentSettings.copy(privacyProtectionLevel = level)
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
                                    val currentSettings = uiSettingsRepository.loadSettings()
                                    val updatedSettings = currentSettings.copy(arDataRetentionDays = days)
                                    uiSettingsRepository.saveSettings(updatedSettings)
                                }
                            }
                        )
                    }
                }
            }
            
            // Glass Preview Section
            AnimatedVisibility(
                visible = uiSettings.glassEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsSection(title = "Glass Preview") {
                    GlassPreviewCard(
                        performanceTier = uiSettings.performanceTier,
                        emergencyMode = uiSettings.emergencyMode,
                        highContrastMode = uiSettings.highContrastMode
                    )
                }
            }
            
            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    title = "HazardHawk Version",
                    subtitle = "v3.1.0 Production Ready - Glass UI Enabled",
                    icon = Icons.Default.Info,
                    onClick = { /* Version info */ }
                )
            }
        }
    }
}

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
            tint = ConstructionColors.OnSurfaceVariant,
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
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = { },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ConstructionColors.SafetyOrange,
                    unfocusedBorderColor = ConstructionColors.OnSurfaceVariant
                ),
                modifier = Modifier
                    .menuAnchor()
                    .width(100.dp)
            )
            
            ExposedDropdownMenu(
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

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ConstructionColors.OnSurfaceVariant,
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
    }
}

@Composable
private fun GlassPreviewCard(
    performanceTier: String,
    emergencyMode: Boolean,
    highContrastMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (emergencyMode) {
                ConstructionColors.SafetyOrange
            } else {
                Color.White.copy(alpha = 0.8f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (!emergencyMode) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                ConstructionColors.SafetyOrange,
                                ConstructionColors.SafetyOrange
                            )
                        )
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Glass Effect Preview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (emergencyMode) Color.White else ConstructionColors.OnSurface
                )
                
                Text(
                    text = "Performance: $performanceTier",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (emergencyMode) Color.White.copy(alpha = 0.9f) else ConstructionColors.OnSurfaceVariant
                )
                
                if (emergencyMode) {
                    Text(
                        text = "⚠️ EMERGENCY MODE ACTIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                if (highContrastMode) {
                    Text(
                        text = "🔆 High Contrast Enabled",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (emergencyMode) Color.White.copy(alpha = 0.9f) else ConstructionColors.OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    MaterialTheme {
        SettingsScreen(
            onNavigateBack = {},
            glassEnabled = true,
            onGlassToggle = {}
        )
    }
}