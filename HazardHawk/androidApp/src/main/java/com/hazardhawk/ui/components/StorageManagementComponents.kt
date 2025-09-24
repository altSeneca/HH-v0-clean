package com.hazardhawk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.security.storage.*
import com.hazardhawk.ui.theme.ConstructionTheme

/**
 * Manual API Key Entry Dialog
 * Displayed when storage systems fail and manual entry is required
 */
@Composable
fun ManualApiKeyEntryDialog(
    request: ManualEntryRequest,
    onValueEntered: (String) -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var apiKey by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Storage System Unavailable",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "All secure storage systems have failed. Manual entry is required to continue using AI analysis features.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = request.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { 
                        apiKey = it
                        showError = false
                    },
                    label = { Text("API Key") },
                    placeholder = { Text("Enter your Gemini API key") },
                    visualTransformation = if (isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { isPasswordVisible = !isPasswordVisible }
                        ) {
                            Icon(
                                imageVector = if (isPasswordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (isPasswordVisible) {
                                    "Hide API key"
                                } else {
                                    "Show API key"
                                }
                            )
                        }
                    },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text("Please enter a valid API key", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (request.isRequired) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "This value is required for core functionality",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!request.isRequired) {
                    OutlinedButton(
                        onClick = onSkip
                    ) {
                        Text("Skip")
                    }
                }
                
                Button(
                    onClick = {
                        if (apiKey.isBlank()) {
                            showError = true
                        } else {
                            onValueEntered(apiKey)
                        }
                    },
                    enabled = apiKey.isNotBlank()
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        modifier = modifier
    )
}

/**
 * Storage Security Level Indicator
 * Shows the current storage security level with appropriate styling
 */
@Composable
fun StorageSecurityIndicator(
    securityLevel: StorageSecurityLevel,
    modifier: Modifier = Modifier
) {
    val (icon, color, backgroundColor) = when (securityLevel) {
        StorageSecurityLevel.ENCRYPTED_SECURE -> Triple(
            Icons.Default.Security,
            Color(0xFF4CAF50),
            Color(0xFFE8F5E8)
        )
        StorageSecurityLevel.OBFUSCATED_MEDIUM -> Triple(
            Icons.Default.Shield,
            Color(0xFFFF9800),
            Color(0xFFFFF3E0)
        )
        StorageSecurityLevel.MEMORY_LOW -> Triple(
            Icons.Default.Memory,
            Color(0xFFFF5722),
            Color(0xFFFFF1F0)
        )
        StorageSecurityLevel.MANUAL_EMERGENCY -> Triple(
            Icons.Default.Warning,
            Color(0xFFF44336),
            Color(0xFFFFEBEE)
        )
        StorageSecurityLevel.NONE -> Triple(
            Icons.Default.Error,
            Color(0xFF9E9E9E),
            Color(0xFFF5F5F5)
        )
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = securityLevel.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Storage Health Status Card
 * Shows detailed information about storage system health
 */
@Composable
fun StorageHealthCard(
    healthStatus: StorageHealthStatus,
    onRetry: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (healthStatus.isHealthy) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (healthStatus.isHealthy) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = if (healthStatus.isHealthy) {
                            Color(0xFF4CAF50)
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = if (healthStatus.isHealthy) {
                            "Storage System Healthy"
                        } else {
                            "Storage System Issues"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                StorageSecurityIndicator(
                    securityLevel = healthStatus.currentLevel
                )
            }
            
            if (!healthStatus.isHealthy && healthStatus.errorMessage != null) {
                Text(
                    text = healthStatus.errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!healthStatus.isHealthy) {
                    Button(
                        onClick = onRetry,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry")
                    }
                }
                
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Details")
                }
            }
        }
    }
}

/**
 * Storage Provider Status List
 * Shows the status of all available storage providers
 */
@Composable
fun StorageProviderStatusList(
    providers: List<StorageProviderInfo>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(providers) { provider ->
            StorageProviderCard(
                provider = provider,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Individual Storage Provider Status Card
 */
@Composable
fun StorageProviderCard(
    provider: StorageProviderInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (provider.isAvailable && provider.isHealthy) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = when {
                    provider.isAvailable && provider.isHealthy -> Icons.Default.CheckCircle
                    provider.isAvailable && !provider.isHealthy -> Icons.Default.Warning
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when {
                    provider.isAvailable && provider.isHealthy -> Color(0xFF4CAF50)
                    provider.isAvailable && !provider.isHealthy -> Color(0xFFFF9800)
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = provider.level.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = provider.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            StorageSecurityIndicator(
                securityLevel = provider.level
            )
        }
    }
}

@Preview
@Composable
fun ManualApiKeyEntryDialogPreview() {
    ConstructionTheme {
        ManualApiKeyEntryDialog(
            request = ManualEntryRequest(
                id = "test",
                key = "gemini_api_key",
                operation = ManualEntryOperation.GET,
                description = "Google Gemini API Key for photo analysis",
                isRequired = true
            ),
            onValueEntered = {},
            onSkip = {},
            onDismiss = {}
        )
    }
}

@Preview
@Composable
fun StorageSecurityIndicatorPreview() {
    ConstructionTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            StorageSecurityIndicator(StorageSecurityLevel.ENCRYPTED_SECURE)
            StorageSecurityIndicator(StorageSecurityLevel.OBFUSCATED_MEDIUM)
            StorageSecurityIndicator(StorageSecurityLevel.MEMORY_LOW)
            StorageSecurityIndicator(StorageSecurityLevel.MANUAL_EMERGENCY)
        }
    }
}