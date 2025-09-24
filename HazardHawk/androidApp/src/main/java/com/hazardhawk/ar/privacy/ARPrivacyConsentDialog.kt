package com.hazardhawk.ar.privacy

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hazardhawk.ui.theme.ConstructionColors
import com.hazardhawk.data.repositories.UISettingsRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * AR Privacy Consent Dialog for GDPR compliance.
 * Provides clear information about AR data processing and user rights.
 */
@Composable
fun ARPrivacyConsentDialog(
    isVisible: Boolean,
    onConsentGiven: () -> Unit,
    onConsentDenied: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiSettingsRepository: UISettingsRepository = koinInject()
    val coroutineScope = rememberCoroutineScope()

    var selectedProtectionLevel by remember { mutableStateOf(PrivacyProtectionLevel.STANDARD) }
    var dataRetentionDays by remember { mutableStateOf(30) }
    var hasReadTerms by remember { mutableStateOf(false) }

    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ConstructionColors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ConstructionColors.SafetyOrange,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "AR Privacy Protection",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionColors.OnSurface
                        )
                    }

                    HorizontalDivider(
                        color = ConstructionColors.OnSurfaceVariant.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Privacy Notice
                    Text(
                        text = "HazardHawk AR Privacy Notice",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConstructionColors.OnSurface
                    )

                    Text(
                        text = "To enable AR safety features, HazardHawk processes camera data in real-time. Your privacy is our priority:",
                        fontSize = 14.sp,
                        color = ConstructionColors.OnSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    // Data Processing Information
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ConstructionColors.SurfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PrivacyInfoItem(
                                icon = Icons.Default.Visibility,
                                title = "Real-time Processing",
                                description = "Camera data is processed locally on your device for hazard detection"
                            )

                            PrivacyInfoItem(
                                icon = Icons.Default.Security,
                                title = "Facial Protection",
                                description = "Worker faces are automatically blurred or masked to protect identity"
                            )

                            PrivacyInfoItem(
                                icon = Icons.Default.Storage,
                                title = "Data Retention",
                                description = "AR analysis data is automatically deleted after the specified retention period"
                            )

                            PrivacyInfoItem(
                                icon = Icons.Default.Block,
                                title = "No Cloud Upload",
                                description = "Camera frames with facial data are never uploaded to external servers"
                            )
                        }
                    }

                    // Privacy Protection Level Selection
                    Text(
                        text = "Select Privacy Protection Level",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConstructionColors.OnSurface
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrivacyProtectionLevel.values().forEach { level ->
                            ProtectionLevelCard(
                                level = level,
                                isSelected = selectedProtectionLevel == level,
                                onSelect = { selectedProtectionLevel = level }
                            )
                        }
                    }

                    // Data Retention Setting
                    Text(
                        text = "Data Retention Period",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ConstructionColors.OnSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Delete data after:",
                            fontSize = 14.sp,
                            color = ConstructionColors.OnSurfaceVariant
                        )

                        FilterChip(
                            onClick = { dataRetentionDays = 7 },
                            label = { Text("7 days") },
                            selected = dataRetentionDays == 7
                        )

                        FilterChip(
                            onClick = { dataRetentionDays = 30 },
                            label = { Text("30 days") },
                            selected = dataRetentionDays == 30
                        )

                        FilterChip(
                            onClick = { dataRetentionDays = 90 },
                            label = { Text("90 days") },
                            selected = dataRetentionDays == 90
                        )
                    }

                    // Terms Acknowledgment
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = hasReadTerms,
                            onCheckedChange = { hasReadTerms = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ConstructionColors.SafetyOrange
                            )
                        )

                        Text(
                            text = "I have read and understand the privacy terms and my rights under GDPR",
                            fontSize = 12.sp,
                            color = ConstructionColors.OnSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }

                    // User Rights Information
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Blue.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Your Rights (GDPR)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ConstructionColors.OnSurface
                            )

                            Text(
                                text = "• Right to withdraw consent at any time\n• Right to data portability and deletion\n• Right to object to data processing\n• Right to access your data",
                                fontSize = 10.sp,
                                color = ConstructionColors.OnSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    HorizontalDivider(
                        color = ConstructionColors.OnSurfaceVariant.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onConsentDenied,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = ConstructionColors.OnSurface
                            )
                        ) {
                            Text("Decline")
                        }

                        Button(
                            onClick = {
                                if (hasReadTerms) {
                                    coroutineScope.launch {
                                        // Save privacy settings
                                        val currentSettings = uiSettingsRepository.loadSettings()
                                        val updatedSettings = currentSettings.copy(
                                            arConsentGiven = true,
                                            consentTimestamp = System.currentTimeMillis(),
                                            privacyProtectionLevel = selectedProtectionLevel.name,
                                            arDataRetentionDays = dataRetentionDays,
                                            facialAnonymizationEnabled = true
                                        )
                                        uiSettingsRepository.saveSettings(updatedSettings)
                                        onConsentGiven()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = hasReadTerms,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ConstructionColors.SafetyOrange
                            )
                        ) {
                            Text("Accept")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Privacy information item component.
 */
@Composable
private fun PrivacyInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ConstructionColors.SafetyOrange,
            modifier = Modifier.size(20.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = ConstructionColors.OnSurface
            )

            Text(
                text = description,
                fontSize = 11.sp,
                color = ConstructionColors.OnSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * Protection level selection card.
 */
@Composable
private fun ProtectionLevelCard(
    level: PrivacyProtectionLevel,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                ConstructionColors.SafetyOrange.copy(alpha = 0.1f)
            } else {
                ConstructionColors.SurfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                ConstructionColors.SafetyOrange
            )
        } else null,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = ConstructionColors.SafetyOrange
                )
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = level.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConstructionColors.OnSurface
                )

                Text(
                    text = level.description,
                    fontSize = 11.sp,
                    color = ConstructionColors.OnSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}