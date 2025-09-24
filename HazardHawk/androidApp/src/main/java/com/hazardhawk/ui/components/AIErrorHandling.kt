package com.hazardhawk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Educational Error Handling Components for HazardHawk AI
 * 
 * Construction-optimized design with:
 * - Clear, actionable error messages
 * - Educational guidance for problem resolution
 * - Large touch targets for easy recovery actions
 * - Construction-friendly visual design
 */

/**
 * Main AI Error Display Component
 */
@Composable
fun AIErrorDisplay(
    error: AIError,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    onSwitchToLocal: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = error.severity.backgroundColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(
            width = 2.dp,
            color = error.severity.primaryColor
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Error header
            AIErrorHeader(
                error = error,
                showDetails = showDetails,
                onToggleDetails = { showDetails = !showDetails }
            )
            
            // Error message and guidance
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = error.userFriendlyMessage,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Expandable details
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    AIErrorDetailsSection(
                        error = error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Recovery actions
            AIErrorActions(
                error = error,
                onRetry = onRetry,
                onDismiss = onDismiss,
                onSwitchToLocal = onSwitchToLocal
            )
        }
    }
}

/**
 * Error header with icon and severity
 */
@Composable
fun AIErrorHeader(
    error: AIError,
    showDetails: Boolean,
    onToggleDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated error icon
            val infiniteTransition = rememberInfiniteTransition(label = "error_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "icon_scale"
            )
            
            Icon(
                imageVector = error.severity.icon,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .scale(if (error.severity == AIErrorSeverity.CRITICAL) scale else 1f),
                tint = error.severity.primaryColor
            )
            
            Column {
                Text(
                    text = error.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = error.severity.primaryColor
                )
                
                // Severity badge
                AIErrorSeverityBadge(severity = error.severity)
            }
        }
        
        // Details toggle
        IconButton(
            onClick = onToggleDetails,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (showDetails) "Hide details" else "Show details",
                modifier = Modifier.size(24.dp),
                tint = error.severity.primaryColor
            )
        }
    }
}

/**
 * Severity badge component
 */
@Composable
fun AIErrorSeverityBadge(
    severity: AIErrorSeverity,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(severity.primaryColor.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = severity.primaryColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = severity.icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = severity.primaryColor
        )
        
        Text(
            text = severity.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = severity.primaryColor
        )
    }
}

/**
 * Detailed error information section
 */
@Composable
fun AIErrorDetailsSection(
    error: AIError,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Divider(color = error.severity.primaryColor.copy(alpha = 0.3f))
        
        // Technical details (if available)
        if (error.technicalDetails.isNotEmpty()) {
            AIErrorTechnicalCard(
                title = "Technical Details",
                content = error.technicalDetails,
                color = ConstructionColors.WorkZoneBlue
            )
        }
        
        // Troubleshooting steps
        if (error.troubleshootingSteps.isNotEmpty()) {
            AIErrorTroubleshootingCard(
                steps = error.troubleshootingSteps,
                color = error.severity.primaryColor
            )
        }
        
        // Educational content
        if (error.educationalContent.isNotEmpty()) {
            AIErrorEducationalCard(
                content = error.educationalContent,
                color = ConstructionColors.SafetyGreen
            )
        }
    }
}

/**
 * Technical details card
 */
@Composable
fun AIErrorTechnicalCard(
    title: String,
    content: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Troubleshooting steps card
 */
@Composable
fun AIErrorTroubleshootingCard(
    steps: List<String>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
                
                Text(
                    text = "Troubleshooting Steps",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = color
                    )
                    
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Educational content card
 */
@Composable
fun AIErrorEducationalCard(
    content: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
                
                Text(
                    text = "Learn More",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Error recovery actions
 */
@Composable
fun AIErrorActions(
    error: AIError,
    onRetry: (() -> Unit)?,
    onDismiss: (() -> Unit)?,
    onSwitchToLocal: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Primary action (usually retry)
        onRetry?.let {
            FlikkerPrimaryButton(
                onClick = it,
                text = error.primaryActionText,
                icon = error.primaryActionIcon,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Secondary actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Switch to local AI (for cloud errors)
            if (error.type in listOf(AIErrorType.NETWORK, AIErrorType.API_KEY, AIErrorType.QUOTA_EXCEEDED) && onSwitchToLocal != null) {
                FlikkerSecondaryButton(
                    onClick = onSwitchToLocal,
                    text = "Use Local AI",
                    icon = Icons.Default.PhoneAndroid,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Dismiss button
            onDismiss?.let {
                FlikkerSecondaryButton(
                    onClick = it,
                    text = "Dismiss",
                    icon = Icons.Default.Close,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Compact error indicator for status bars
 */
@Composable
fun CompactAIErrorIndicator(
    error: AIError,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(error.severity.primaryColor.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = error.severity.primaryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = error.severity.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = error.severity.primaryColor
        )
        
        Text(
            text = "AI Error",
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = error.severity.primaryColor
        )
        
        Icon(
            Icons.Default.ExpandMore,
            contentDescription = "Show details",
            modifier = Modifier.size(14.dp),
            tint = error.severity.primaryColor.copy(alpha = 0.7f)
        )
    }
}

/**
 * AI Error Data Classes
 */
data class AIError(
    val type: AIErrorType,
    val severity: AIErrorSeverity,
    val title: String,
    val userFriendlyMessage: String,
    val technicalDetails: String = "",
    val troubleshootingSteps: List<String> = emptyList(),
    val educationalContent: String = "",
    val primaryActionText: String = "Try Again",
    val primaryActionIcon: ImageVector = Icons.Default.Refresh
)

enum class AIErrorType {
    NETWORK,
    API_KEY,
    QUOTA_EXCEEDED,
    PROCESSING_FAILED,
    UNSUPPORTED_FORMAT,
    LOCAL_AI_UNAVAILABLE,
    UNKNOWN
}

enum class AIErrorSeverity(
    val label: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val backgroundColor: Color
) {
    INFO(
        label = "Info",
        icon = Icons.Default.Info,
        primaryColor = ConstructionColors.WorkZoneBlue,
        backgroundColor = ConstructionColors.WorkZoneBlue
    ),
    WARNING(
        label = "Warning",
        icon = Icons.Default.Warning,
        primaryColor = ConstructionColors.HighVisYellow,
        backgroundColor = ConstructionColors.HighVisYellow
    ),
    ERROR(
        label = "Error",
        icon = Icons.Default.Error,
        primaryColor = ConstructionColors.SafetyOrange,
        backgroundColor = ConstructionColors.SafetyOrange
    ),
    CRITICAL(
        label = "Critical",
        icon = Icons.Default.ErrorOutline,
        primaryColor = ConstructionColors.CautionRed,
        backgroundColor = ConstructionColors.CautionRed
    )
}

/**
 * Common AI Error Scenarios
 */
object CommonAIErrors {
    fun networkError() = AIError(
        type = AIErrorType.NETWORK,
        severity = AIErrorSeverity.ERROR,
        title = "Network Connection Error",
        userFriendlyMessage = "Unable to connect to the AI service. Check your internet connection and try again.",
        troubleshootingSteps = listOf(
            "Check your WiFi or cellular connection",
            "Try switching between WiFi and cellular data",
            "Wait a moment and try again",
            "Consider using Local AI mode for offline analysis"
        ),
        educationalContent = "Cloud AI requires an internet connection to process your photos. Local AI works offline but with slightly reduced accuracy."
    )
    
    fun invalidAPIKey() = AIError(
        type = AIErrorType.API_KEY,
        severity = AIErrorSeverity.ERROR,
        title = "Invalid API Key",
        userFriendlyMessage = "Your Gemini API key appears to be invalid or has expired. Please check your API key configuration.",
        troubleshootingSteps = listOf(
            "Verify your API key in Settings",
            "Ensure the key starts with 'AIzaSy'",
            "Check if the key has been disabled in Google Cloud Console",
            "Generate a new API key if needed"
        ),
        educationalContent = "API keys can expire or be disabled for security reasons. You can generate a new key in the Google Cloud Console.",
        primaryActionText = "Fix API Key",
        primaryActionIcon = Icons.Default.Key
    )
    
    fun quotaExceeded() = AIError(
        type = AIErrorType.QUOTA_EXCEEDED,
        severity = AIErrorSeverity.WARNING,
        title = "API Quota Exceeded",
        userFriendlyMessage = "You've reached your daily limit for cloud AI analysis. You can continue using Local AI or wait until tomorrow.",
        troubleshootingSteps = listOf(
            "Use Local AI for immediate analysis",
            "Wait until tomorrow for quota reset",
            "Increase your quota limits in Google Cloud Console",
            "Consider upgrading to a paid plan for higher limits"
        ),
        educationalContent = "Google Gemini API has usage limits to prevent abuse. Free tier includes generous limits for most users.",
        primaryActionText = "Switch to Local AI",
        primaryActionIcon = Icons.Default.PhoneAndroid
    )
    
    fun processingFailed() = AIError(
        type = AIErrorType.PROCESSING_FAILED,
        severity = AIErrorSeverity.ERROR,
        title = "Analysis Processing Failed",
        userFriendlyMessage = "The AI couldn't analyze this photo. This might be due to image quality, format, or content issues.",
        troubleshootingSteps = listOf(
            "Ensure the photo shows a clear construction scene",
            "Try taking a new photo with better lighting",
            "Check that the image isn't corrupted",
            "Try a different photo format if possible"
        ),
        educationalContent = "AI works best with clear, well-lit photos of construction sites. Blurry, dark, or corrupted images may fail to process."
    )
    
    fun localAIUnavailable() = AIError(
        type = AIErrorType.LOCAL_AI_UNAVAILABLE,
        severity = AIErrorSeverity.ERROR,
        title = "Local AI Unavailable",
        userFriendlyMessage = "Local AI processing is not available on this device. You can use Cloud AI with an internet connection.",
        troubleshootingSteps = listOf(
            "Ensure your device meets minimum requirements",
            "Update the app to the latest version",
            "Restart the app and try again",
            "Use Cloud AI as an alternative"
        ),
        educationalContent = "Local AI requires specific hardware capabilities and may not be available on all devices. Cloud AI provides the same functionality with internet access.",
        primaryActionText = "Setup Cloud AI",
        primaryActionIcon = Icons.Default.Cloud
    )
}
