package com.hazardhawk.privacy

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * GDPR-compliant GPS consent dialog with clear privacy information
 * Implements Article 7 requirements for consent collection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GPSConsentDialog(
    onConsentGranted: (GPSPrivacyManager.PrecisionLevel, Int) -> Unit,
    onConsentDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPrecisionLevel by remember { mutableStateOf(GPSPrivacyManager.PrecisionLevel.APPROXIMATE) }
    var selectedRetentionDays by remember { mutableIntStateOf(30 * 365) } // 30 years default
    var hasReadPrivacyNotice by remember { mutableStateOf(false) }
    var showDetailedInfo by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onConsentDenied,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
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
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFFF6B35), // Safety Orange
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "Location Data Consent",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GDPR Compliance Required",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Divider()

                // Privacy Notice
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Why we need your location:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrivacyInfoRow(
                            icon = Icons.Default.PhotoCamera,
                            text = "Document exact safety incident locations"
                        )
                        PrivacyInfoRow(
                            icon = Icons.Default.Gavel,
                            text = "Meet OSHA compliance requirements"
                        )
                        PrivacyInfoRow(
                            icon = Icons.Default.Security,
                            text = "Generate legally admissible safety reports"
                        )
                        PrivacyInfoRow(
                            icon = Icons.Default.Map,
                            text = "Create site-specific hazard maps"
                        )
                    }
                }

                // Precision Level Selection
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Choose your privacy level:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    GPSPrivacyManager.PrecisionLevel.values().filterNot { it == GPSPrivacyManager.PrecisionLevel.DISABLED }.forEach { level ->
                        PrecisionLevelOption(
                            level = level,
                            selected = selectedPrecisionLevel == level,
                            onSelect = { selectedPrecisionLevel = level }
                        )
                    }
                }

                // Data Retention Selection
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Data retention period:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val retentionOptions = listOf(
                        Pair(365, "1 Year"),
                        Pair(5 * 365, "5 Years"),
                        Pair(30 * 365, "30 Years (OSHA Compliance)")
                    )

                    retentionOptions.forEach { (days, displayName) ->
                        RetentionOption(
                            days = days,
                            displayName = displayName,
                            selected = selectedRetentionDays == days,
                            onSelect = { selectedRetentionDays = days },
                            isRecommended = days == 30 * 365
                        )
                    }
                }

                // GDPR Rights Information
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Your Privacy Rights (GDPR)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (showDetailedInfo) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                GDPRRightItem("Right to withdraw consent at any time")
                                GDPRRightItem("Right to access your location data")
                                GDPRRightItem("Right to rectify incorrect data")
                                GDPRRightItem("Right to erasure (deletion)")
                                GDPRRightItem("Right to data portability")
                                GDPRRightItem("Right to object to processing")
                            }
                        } else {
                            TextButton(
                                onClick = { showDetailedInfo = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Show detailed privacy rights")
                                Icon(
                                    imageVector = Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Privacy Notice Acknowledgment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = hasReadPrivacyNotice,
                        onCheckedChange = { hasReadPrivacyNotice = it }
                    )
                    Text(
                        text = "I have read and understand how my location data will be used and my rights under GDPR",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.clickable { hasReadPrivacyNotice = !hasReadPrivacyNotice }
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onConsentDenied,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deny")
                    }

                    Button(
                        onClick = {
                            onConsentGranted(selectedPrecisionLevel, selectedRetentionDays)
                        },
                        enabled = hasReadPrivacyNotice,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981) // Safety Green
                        )
                    ) {
                        Text("Grant Consent")
                    }
                }

                // Legal Notice
                Text(
                    text = "This consent can be withdrawn at any time in the app settings. Denying consent will disable location-based features but you can still use other app functions.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PrivacyInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF10B981), // Safety Green
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PrecisionLevelOption(
    level: GPSPrivacyManager.PrecisionLevel,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect
            ),
        border = if (selected) BorderStroke(2.dp, Color(0xFFFF6B35)) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFFF6B35).copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = level.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = level.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (level.accuracyMeters > 0) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "~${level.accuracyMeters.toInt()}m",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RetentionOption(
    days: Int,
    displayName: String,
    selected: Boolean,
    onSelect: () -> Unit,
    isRecommended: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (isRecommended) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFFF6B35).copy(alpha = 0.2f)
            ) {
                Text(
                    text = "RECOMMENDED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun GDPRRightItem(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}