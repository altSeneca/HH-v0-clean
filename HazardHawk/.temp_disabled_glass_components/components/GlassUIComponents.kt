package com.hazardhawk.ui.glass.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
// Semantics import removed to fix compilation
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
// GlassConfiguration is now defined in GlassExtensions.kt
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Additional glass UI components for the camera interface.
 * 
 * This file contains supporting glass components like emergency toggle,
 * performance indicators, and error overlays with construction-optimized designs.
 */

/**
 * Emergency mode toggle with prominent glass effect and safety coloring
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassEmergencyToggle(
    isInEmergencyMode: Boolean,
    configuration: GlassConfiguration,
    onToggleEmergencyMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = if (isInEmergencyMode) 0.7f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = false,
                    radius = 30.dp,
                    color = if (isInEmergencyMode) ConstructionColors.CautionRed else ConstructionColors.SafetyOrange
                ),
                onClick = onToggleEmergencyMode
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                if (isInEmergencyMode) ConstructionColors.CautionRed else Color.Black.copy(alpha = 0.7f)
            } else Color.Transparent
        ),
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isInEmergencyMode) 3.dp else 2.dp,
            color = if (isInEmergencyMode) {
                ConstructionColors.CautionRed.copy(alpha = pulseAnimation)
            } else {
                ConstructionColors.SafetyOrange.copy(alpha = 0.8f)
            }
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(configuration.minTouchTargetSize.dp)
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = if (isInEmergencyMode) {
                                    listOf(
                                        ConstructionColors.CautionRed.copy(alpha = 0.3f * pulseAnimation),
                                        ConstructionColors.CautionRed.copy(alpha = 0.1f * pulseAnimation),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        ConstructionColors.SafetyOrange.copy(alpha = 0.2f),
                                        ConstructionColors.SafetyOrange.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                }
                            ),
                            shape = CircleShape
                        )
                    } else Modifier
                )
        ) {
            Icon(
                imageVector = if (isInEmergencyMode) Icons.Default.Warning else Icons.Default.Security,
                contentDescription = if (isInEmergencyMode) "Exit Emergency Mode" else "Emergency Mode",
                tint = if (isInEmergencyMode) {
                    Color.White.copy(alpha = pulseAnimation)
                } else {
                    Color.White
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Performance indicator showing FPS, memory usage, and performance status
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassPerformanceIndicator(
    fps: Double,
    memoryMB: Long,
    isReduced: Boolean,
    configuration: GlassConfiguration,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        fps < 30 -> ConstructionColors.CautionRed
        fps < 45 -> ConstructionColors.SafetyOrange
        else -> ConstructionColors.SafetyGreen
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = statusColor.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isReduced) Icons.Default.Speed else Icons.Default.Monitoring,
                    contentDescription = "Performance",
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "PERFORMANCE",
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // FPS indicator
            Row(
                justifyContent = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "FPS:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
                Text(
                    text = "${fps.toInt()}",
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Memory indicator
            Row(
                justifyContent = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Memory:",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
                Text(
                    text = "${memoryMB}MB",
                    color = if (memoryMB > 40) ConstructionColors.SafetyOrange else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Performance status
            if (isReduced) {
                Text(
                    text = "REDUCED MODE",
                    color = ConstructionColors.SafetyOrange,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Error overlay with glass effect for displaying error messages
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassErrorOverlay(
    error: String,
    configuration: GlassConfiguration,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pulseAnimation by rememberInfiniteTransition().animateFloat(
        initialValue = 0.8f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Card(
        modifier = modifier
            .padding(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = true,
                    color = ConstructionColors.CautionRed
                ),
                onClick = onDismiss
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                ConstructionColors.CautionRed.copy(alpha = 0.9f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = ConstructionColors.CautionRed.copy(alpha = pulseAnimation)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error icon with pulsing animation
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = ConstructionColors.CautionRed.copy(alpha = pulseAnimation),
                modifier = Modifier.size(48.dp)
            )
            
            // Error title
            Text(
                text = "SYSTEM ERROR",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            // Error message
            Text(
                text = error,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            
            // Dismiss button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConstructionColors.SafetyOrange,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(configuration.minTouchTargetSize.dp)
            ) {
                Text(
                    text = "DISMISS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Glass loading indicator with construction-themed animations
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassLoadingOverlay(
    message: String,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .hazeChild(
                state = hazeState,
                style = HazeMaterials.regular(
                    backgroundColor = configuration.tintColor,
                    blurRadius = configuration.effectiveBlurRadius.dp
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = ConstructionColors.SafetyOrange.copy(alpha = 0.6f)
            )
        } else null
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Construction-themed progress indicator
            CircularProgressIndicator(
                color = ConstructionColors.SafetyOrange,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            
            // Loading message
            Text(
                text = message,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Glass confirmation dialog with construction safety styling
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "CONFIRM",
    cancelText: String = "CANCEL",
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(24.dp)
            .hazeChild(
                state = hazeState,
                style = HazeMaterials.regular(
                    backgroundColor = configuration.tintColor,
                    blurRadius = configuration.effectiveBlurRadius.dp
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.9f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = configuration.borderColor.copy(alpha = 0.8f)
            )
        } else null
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            Text(
                text = title,
                color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Message
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel button
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(configuration.minTouchTargetSize.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = cancelText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Confirm button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(configuration.minTouchTargetSize.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ConstructionColors.SafetyOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = confirmText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Glass notification banner with construction alerts
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassNotificationBanner(
    icon: ImageVector,
    title: String,
    message: String,
    type: NotificationType = NotificationType.INFO,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val typeColor = when (type) {
        NotificationType.SUCCESS -> ConstructionColors.SafetyGreen
        NotificationType.WARNING -> ConstructionColors.SafetyOrange
        NotificationType.ERROR -> ConstructionColors.CautionRed
        NotificationType.INFO -> ConstructionColors.WorkZoneBlue
    }
    
    Card(
        modifier = modifier
            .hazeChild(
                state = hazeState,
                style = HazeMaterials.regular(
                    backgroundColor = typeColor.copy(alpha = 0.2f),
                    blurRadius = configuration.effectiveBlurRadius.dp * 0.8f
                )
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                typeColor.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = typeColor
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = typeColor,
                modifier = Modifier.size(24.dp)
            )
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
            
            // Dismiss button
            onDismiss?.let { dismiss ->
                IconButton(
                    onClick = dismiss,
                    modifier = Modifier.size(configuration.minTouchTargetSize.dp * 0.6f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

enum class NotificationType {
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}