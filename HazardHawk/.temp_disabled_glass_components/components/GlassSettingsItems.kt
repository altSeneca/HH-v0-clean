package com.hazardhawk.ui.glass.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
// Semantics import removed to fix compilation
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
 * Settings-specific glass components optimized for construction environments.
 * 
 * Features:
 * - Glass morphism settings sections with backdrop blur
 * - Construction safety category organization
 * - Performance monitoring displays
 * - Device capability information cards
 * - Emergency settings with high visibility
 * - OSHA compliance configuration panels
 */

/**
 * Glass settings section header with construction styling
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassSettingsSection(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    isEmergencySection: Boolean = false,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    // Emergency section pulse animation
    val emergencyPulse by rememberInfiniteTransition(label = "emergency_pulse").animateFloat(
        initialValue = if (isEmergencySection) 0.7f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emergency_alpha"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        border = if (isEmergencySection) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = ConstructionColors.CautionRed.copy(alpha = emergencyPulse)
            )
        } else if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = configuration.borderColor.copy(alpha = 0.4f)
            )
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = HazeMaterials.regular(
                                backgroundColor = if (isEmergencySection) {
                                    ConstructionColors.CautionRed.copy(alpha = 0.05f)
                                } else configuration.tintColor,
                                blurRadius = configuration.effectiveBlurRadius.dp
                            )
                        )
                    } else Modifier
                )
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Section icon
                icon?.let {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEmergencySection) {
                                ConstructionColors.CautionRed.copy(alpha = 0.2f)
                            } else ConstructionColors.SafetyOrange.copy(alpha = 0.2f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 2.dp,
                            color = if (isEmergencySection) {
                                ConstructionColors.CautionRed.copy(alpha = emergencyPulse)
                            } else ConstructionColors.SafetyOrange
                        ),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = if (isEmergencySection) {
                                    ConstructionColors.CautionRed.copy(alpha = emergencyPulse)
                                } else ConstructionColors.SafetyOrange,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                // Section text
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        color = if (isEmergencySection) {
                            ConstructionColors.CautionRed.copy(alpha = emergencyPulse)
                        } else if (configuration.isHighContrastMode) {
                            configuration.borderColor
                        } else Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    description?.let { desc ->
                        Text(
                            text = desc,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Section content
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Glass settings item with navigation arrow
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassSettingsItem(
    title: String,
    description: String? = null,
    value: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    configuration: GlassConfiguration,
    enabled: Boolean = true,
    isWarning: Boolean = false,
    showBadge: Boolean = false,
    badgeText: String = "NEW",
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = true,
                    color = if (isWarning) ConstructionColors.CautionRed else ConstructionColors.SafetyOrange
                ),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) {
                ConstructionColors.CautionRed.copy(alpha = 0.1f)
            } else Color.White.copy(alpha = 0.05f)
        ),
        border = if (isWarning) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = ConstructionColors.CautionRed.copy(alpha = 0.6f)
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.2f)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = configuration.minTouchTargetSize.dp)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) {
                        if (isWarning) ConstructionColors.CautionRed else Color.White
                    } else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
                
                // Badge
                if (showBadge) {
                    Card(
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = ConstructionColors.CautionRed
                        ),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = if (enabled) {
                        if (isWarning) ConstructionColors.CautionRed else Color.White
                    } else Color.White.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                description?.let { desc ->
                    Text(
                        text = desc,
                        color = Color.White.copy(alpha = if (enabled) 0.7f else 0.4f),
                        fontSize = 12.sp
                    )
                }
                
                value?.let { valueText ->
                    Text(
                        text = valueText,
                        color = if (isWarning) ConstructionColors.CautionRed else ConstructionColors.SafetyOrange,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Navigation arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Glass device info card showing capabilities and status
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassDeviceInfoCard(
    deviceModel: String,
    androidVersion: String,
    glassSupport: String,
    performanceLevel: String,
    memoryUsage: String,
    batteryLevel: String,
    thermalState: String,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.7f)
            } else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = ConstructionColors.WorkZoneBlue.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = HazeMaterials.regular(
                                backgroundColor = ConstructionColors.WorkZoneBlue.copy(alpha = 0.05f),
                                blurRadius = configuration.effectiveBlurRadius.dp
                            )
                        )
                    } else Modifier
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = ConstructionColors.WorkZoneBlue,
                    modifier = Modifier.size(24.dp)
                )
                
                Text(
                    text = "Device Information",
                    color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Device specs grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassDeviceInfoRow("Model", deviceModel, Icons.Default.PhoneAndroid)
                GlassDeviceInfoRow("Android", androidVersion, Icons.Default.Android)
                GlassDeviceInfoRow("Glass Support", glassSupport, Icons.Default.AutoAwesome)
                GlassDeviceInfoRow("Performance", performanceLevel, Icons.Default.Speed)
                GlassDeviceInfoRow("Memory Usage", memoryUsage, Icons.Default.Memory)
                GlassDeviceInfoRow("Battery", batteryLevel, Icons.Default.Battery90)
                GlassDeviceInfoRow("Thermal State", thermalState, Icons.Default.Thermostat)
            }
        }
    }
}

/**
 * Individual device info row
 */
@Composable
private fun GlassDeviceInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
        
        Text(
            text = value,
            color = ConstructionColors.WorkZoneBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End
        )
    }
}

/**
 * Glass performance monitor card with real-time metrics
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassPerformanceMonitorCard(
    frameRate: Double,
    memoryUsage: Long,
    gpuUtilization: Float,
    batteryImpact: Float,
    isPerformanceReduced: Boolean,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    onOptimizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    val performanceColor = when {
        frameRate < 30 || memoryUsage > 40 -> ConstructionColors.CautionRed
        frameRate < 45 || memoryUsage > 30 -> ConstructionColors.SafetyOrange
        else -> ConstructionColors.SafetyGreen
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.7f)
            } else Color.Transparent
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = performanceColor.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = HazeMaterials.regular(
                                backgroundColor = performanceColor.copy(alpha = 0.05f),
                                blurRadius = configuration.effectiveBlurRadius.dp
                            )
                        )
                    } else Modifier
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Monitoring,
                        contentDescription = null,
                        tint = performanceColor,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column {
                        Text(
                            text = "Performance Monitor",
                            color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (isPerformanceReduced) {
                            Text(
                                text = "Reduced performance mode active",
                                color = ConstructionColors.SafetyOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Optimize button
                if (frameRate < 45 || memoryUsage > 30) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOptimizeClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ConstructionColors.SafetyOrange,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "OPTIMIZE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Performance metrics
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassPerformanceMetric(
                    label = "Frame Rate",
                    value = "${frameRate.toInt()} fps",
                    progress = (frameRate / 60.0).toFloat().coerceIn(0f, 1f),
                    color = performanceColor,
                    isOptimal = frameRate >= 45
                )
                
                GlassPerformanceMetric(
                    label = "Memory Usage",
                    value = "${memoryUsage}MB",
                    progress = (memoryUsage / 50.0).toFloat().coerceIn(0f, 1f),
                    color = performanceColor,
                    isOptimal = memoryUsage < 30
                )
                
                GlassPerformanceMetric(
                    label = "GPU Utilization",
                    value = "${gpuUtilization.toInt()}%",
                    progress = gpuUtilization / 100f,
                    color = performanceColor,
                    isOptimal = gpuUtilization < 70f
                )
                
                GlassPerformanceMetric(
                    label = "Battery Impact",
                    value = "${batteryImpact.toInt()}%",
                    progress = batteryImpact / 20f,
                    color = performanceColor,
                    isOptimal = batteryImpact < 10f
                )
            }
        }
    }
}

/**
 * Individual performance metric row with progress indicator
 */
@Composable
private fun GlassPerformanceMetric(
    label: String,
    value: String,
    progress: Float,
    color: Color,
    isOptimal: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Label and value
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOptimal) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isOptimal) ConstructionColors.SafetyGreen else color,
                    modifier = Modifier.size(16.dp)
                )
                
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            
            Text(
                text = value,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Progress bar
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color.White.copy(alpha = 0.2f)
        )
    }
}