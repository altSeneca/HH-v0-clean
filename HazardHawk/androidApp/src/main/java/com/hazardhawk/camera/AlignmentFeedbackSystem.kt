package com.hazardhawk.camera

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.coroutines.delay

/**
 * Real-Time Alignment Feedback System
 * 
 * Provides immediate visual feedback to construction workers about overlay alignment
 * accuracy and system status. Designed for professional construction environments
 * with clear, actionable feedback.
 * 
 * Key Features:
 * - Real-time alignment accuracy indicators
 * - Professional status badges with OSHA compliance context
 * - Construction-friendly color coding and typography
 * - Haptic feedback for alignment confirmations
 * - Auto-hide behavior to prevent UI clutter
 * - Voice feedback integration for hands-free operation
 */

/**
 * Alignment accuracy levels with professional thresholds
 */
enum class AlignmentAccuracy(
    val threshold: Float,
    val label: String,
    val description: String,
    val color: Color,
    val icon: ImageVector
) {
    PERFECT(
        threshold = 0.95f,
        label = "PERFECT",
        description = "Ready for professional documentation",
        color = ConstructionColors.SafetyGreen,
        icon = Icons.Default.CheckCircle
    ),
    GOOD(
        threshold = 0.8f,
        label = "GOOD",
        description = "Acceptable for safety documentation",
        color = ConstructionColors.HighVisYellow,
        icon = Icons.Default.Warning
    ),
    NEEDS_ADJUSTMENT(
        threshold = 0f,
        label = "ADJUST",
        description = "Alignment needs improvement",
        color = ConstructionColors.CautionRed,
        icon = Icons.Default.Error
    );
    
    companion object {
        fun fromAccuracy(accuracy: Float): AlignmentAccuracy {
            return when {
                accuracy >= PERFECT.threshold -> PERFECT
                accuracy >= GOOD.threshold -> GOOD
                else -> NEEDS_ADJUSTMENT
            }
        }
    }
}

/**
 * System status for comprehensive feedback
 */
enum class SystemStatus(
    val label: String,
    val color: Color,
    val description: String
) {
    CALIBRATING(
        label = "CALIBRATING",
        color = ConstructionColors.HighVisYellow,
        description = "System is calibrating overlay alignment"
    ),
    READY(
        label = "READY",
        color = ConstructionColors.SafetyGreen,
        description = "System ready for photo capture"
    ),
    PROCESSING(
        label = "PROCESSING",
        color = ConstructionColors.WorkZoneBlue,
        description = "Processing photo with safety metadata"
    ),
    ERROR(
        label = "ERROR",
        color = ConstructionColors.CautionRed,
        description = "System error - check configuration"
    )
}

/**
 * Main alignment feedback component
 */
@Composable
fun AlignmentFeedbackDisplay(
    alignmentAccuracy: Float,
    systemStatus: SystemStatus = SystemStatus.READY,
    isVisible: Boolean = true,
    showPercentage: Boolean = true,
    enableHapticFeedback: Boolean = true,
    autoHideDelayMs: Long = 5000L,
    modifier: Modifier = Modifier
) {
    val accuracy = AlignmentAccuracy.fromAccuracy(alignmentAccuracy)
    val hapticFeedback = LocalHapticFeedback.current
    
    // Auto-hide after delay
    var shouldShow by remember(alignmentAccuracy, systemStatus) { mutableStateOf(true) }
    
    LaunchedEffect(alignmentAccuracy, systemStatus) {
        shouldShow = true
        if (autoHideDelayMs > 0 && accuracy == AlignmentAccuracy.PERFECT) {
            delay(autoHideDelayMs)
            shouldShow = false
        }
    }
    
    // Trigger haptic feedback for status changes
    LaunchedEffect(accuracy) {
        if (enableHapticFeedback) {
            when (accuracy) {
                AlignmentAccuracy.PERFECT -> {
                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
                AlignmentAccuracy.GOOD -> {
                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                }
                else -> {
                    // Light feedback for adjustment needed
                }
            }
        }
    }
    
    AnimatedVisibility(
        visible = isVisible && shouldShow,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(500)) + scaleOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        AlignmentStatusCard(
            accuracy = accuracy,
            alignmentPercentage = alignmentAccuracy,
            systemStatus = systemStatus,
            showPercentage = showPercentage
        )
    }
}

/**
 * Professional status card component
 */
@Composable
private fun AlignmentStatusCard(
    accuracy: AlignmentAccuracy,
    alignmentPercentage: Float,
    systemStatus: SystemStatus,
    showPercentage: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            2.dp, 
            accuracy.color
        ),
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main alignment status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = accuracy.icon,
                    contentDescription = accuracy.label,
                    tint = accuracy.color,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = accuracy.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = accuracy.color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                )
                
                if (showPercentage) {
                    Text(
                        text = "${(alignmentPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Description text
            Text(
                text = accuracy.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            )
            
            // System status if different from ready
            if (systemStatus != SystemStatus.READY) {
                Spacer(modifier = Modifier.height(8.dp))
                
                SystemStatusIndicator(
                    status = systemStatus,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * System status indicator component
 */
@Composable
private fun SystemStatusIndicator(
    status: SystemStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                status.color.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                status.color.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status == SystemStatus.PROCESSING || status == SystemStatus.CALIBRATING) {
            CircularProgressIndicator(
                color = status.color,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

/**
 * Floating alignment accuracy indicator for minimal UI
 */
@Composable
fun FloatingAlignmentIndicator(
    alignmentAccuracy: Float,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val accuracy = AlignmentAccuracy.fromAccuracy(alignmentAccuracy)
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(accuracy.color)
                .border(3.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = accuracy.icon,
                    contentDescription = accuracy.label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = "${(alignmentAccuracy * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}

/**
 * Success animation for completed documentation
 */
@Composable
fun DocumentationSuccessAnimation(
    isVisible: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = ConstructionColors.SafetyGreen
            ),
            shape = CircleShape,
            modifier = Modifier.size(120.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "DOCUMENTED",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(2000)
            onAnimationComplete()
        }
    }
}

/**
 * Professional calibration progress indicator
 */
@Composable
fun CalibrationProgressDisplay(
    progress: Float,
    isVisible: Boolean = true,
    calibrationStep: String = "Aligning overlay...",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                2.dp,
                ConstructionColors.WorkZoneBlue
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CALIBRATING SYSTEM",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = ConstructionColors.WorkZoneBlue,
                        fontWeight = FontWeight.Bold
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = ConstructionColors.WorkZoneBlue,
                    trackColor = Color.Gray.copy(alpha = 0.3f),
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = calibrationStep,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f)
                    )
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${(progress * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}