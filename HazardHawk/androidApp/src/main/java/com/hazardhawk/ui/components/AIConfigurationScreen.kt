package com.hazardhawk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * AI Configuration Screen for HazardHawk
 * 
 * Construction-optimized design with:
 * - 72dp touch targets for safety compliance
 * - High contrast colors for outdoor visibility
 * - Educational progress indicators
 * - Simple 2-tap workflow
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfigurationScreen(
    currentMode: AIAnalysisMode = AIAnalysisMode.LOCAL,
    onModeChange: (AIAnalysisMode) -> Unit,
    apiKey: String = "",
    onApiKeyChange: (String) -> Unit,
    isApiKeyValid: Boolean = false,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiKeySetup by remember { mutableStateOf(currentMode == AIAnalysisMode.CLOUD && apiKey.isEmpty()) }
    var showEducationalInfo by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Configuration",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    FlikkerBackButton(onClick = onBack)
                },
                actions = {
                    IconButton(
                        onClick = { showEducationalInfo = !showEducationalInfo },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Help",
                            modifier = Modifier.size(28.dp),
                            tint = ConstructionColors.SafetyOrange
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = ConstructionColors.SafetyOrange
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Educational Info Card (collapsible)
            if (showEducationalInfo) {
                EducationalInfoCard(
                    title = "AI Analysis Explained",
                    content = "HazardHawk uses AI to identify construction safety hazards in your photos. Choose between:\n\n• Local AI: Runs on your device, works offline, instant results\n• Cloud AI: Uses Google Gemini Vision Pro 2.5, requires internet, most accurate",
                    onDismiss = { showEducationalInfo = false }
                )
            }
            
            // AI Mode Selection
            AIAnalysisModeSelector(
                currentMode = currentMode,
                onModeChange = { mode ->
                    onModeChange(mode)
                    if (mode == AIAnalysisMode.CLOUD && apiKey.isEmpty()) {
                        showApiKeySetup = true
                    }
                }
            )
            
            // API Key Setup (for cloud mode)
            if (currentMode == AIAnalysisMode.CLOUD) {
                APIKeySetupCard(
                    apiKey = apiKey,
                    onApiKeyChange = onApiKeyChange,
                    isValid = isApiKeyValid,
                    expanded = showApiKeySetup,
                    onExpandedChange = { showApiKeySetup = it }
                )
            }
            
            // Performance Comparison
            AIPerformanceComparison()
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Configuration Button
            FlikkerPrimaryButton(
                onClick = onSave,
                text = "Save Configuration",
                icon = Icons.Default.Save,
                modifier = Modifier.fillMaxWidth(),
                enabled = if (currentMode == AIAnalysisMode.CLOUD) isApiKeyValid else true
            )
        }
    }
}

/**
 * AI Analysis Mode Selector with construction-friendly design
 */
@Composable
fun AIAnalysisModeSelector(
    currentMode: AIAnalysisMode,
    onModeChange: (AIAnalysisMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Analysis Method",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = ConstructionColors.SafetyOrange
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Local AI Option
            AIOptionCard(
                title = "Local AI (Recommended)",
                subtitle = "Runs on your device",
                description = "• Works offline\n• Instant results\n• Privacy focused\n• Good accuracy",
                icon = Icons.Default.PhoneAndroid,
                isSelected = currentMode == AIAnalysisMode.LOCAL,
                onClick = { onModeChange(AIAnalysisMode.LOCAL) },
                statusColor = ConstructionColors.SafetyGreen
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Cloud AI Option
            AIOptionCard(
                title = "Cloud AI (Advanced)",
                subtitle = "Google Gemini Vision Pro 2.5",
                description = "• Requires internet\n• Most accurate results\n• Detailed explanations\n• API key required",
                icon = Icons.Default.Cloud,
                isSelected = currentMode == AIAnalysisMode.CLOUD,
                onClick = { onModeChange(AIAnalysisMode.CLOUD) },
                statusColor = ConstructionColors.WorkZoneBlue
            )
        }
    }
}

/**
 * Individual AI Option Card with construction-optimized touch targets
 */
@Composable
fun AIOptionCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) statusColor else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                statusColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .heightIn(min = 72.dp), // Construction-friendly touch target
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with status indicator
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = statusColor
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        modifier = Modifier
                            .size(20.dp)
                            .offset(x = 22.dp, y = (-8).dp),
                        tint = ConstructionColors.SafetyGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Educational Information Card
 */
@Composable
fun EducationalInfoCard(
    title: String,
    content: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.HighVisYellow.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = ConstructionColors.HighVisYellow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ConstructionColors.SafetyOrange
                        )
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * AI Performance Comparison Card
 */
@Composable
fun AIPerformanceComparison(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Performance Comparison",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = ConstructionColors.SafetyOrange
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Comparison rows
            ComparisonRow(
                feature = "Speed",
                localValue = "Instant",
                cloudValue = "2-3 seconds",
                localBetter = true
            )
            ComparisonRow(
                feature = "Offline Use",
                localValue = "Yes",
                cloudValue = "No",
                localBetter = true
            )
            ComparisonRow(
                feature = "Accuracy",
                localValue = "Good (85%)",
                cloudValue = "Excellent (95%)",
                localBetter = false
            )
            ComparisonRow(
                feature = "Detail Level",
                localValue = "Basic",
                cloudValue = "Comprehensive",
                localBetter = false
            )
            ComparisonRow(
                feature = "Privacy",
                localValue = "Full",
                cloudValue = "Google Terms",
                localBetter = true
            )
        }
    }
}

/**
 * Individual comparison row
 */
@Composable
fun ComparisonRow(
    feature: String,
    localValue: String,
    cloudValue: String,
    localBetter: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = localValue,
            style = MaterialTheme.typography.bodyMedium,
            color = if (localBetter) ConstructionColors.SafetyGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        
        Text(
            text = cloudValue,
            style = MaterialTheme.typography.bodyMedium,
            color = if (!localBetter) ConstructionColors.WorkZoneBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

/**
 * AI Analysis Mode Enum
 */
enum class AIAnalysisMode {
    LOCAL,
    CLOUD
}
