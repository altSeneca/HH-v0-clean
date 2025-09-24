package com.hazardhawk.ui.glass.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

/**
 * Construction-optimized glass form controls with glass morphism effects.
 * 
 * Features:
 * - Glass morphism sliders and toggles with backdrop blur
 * - Large touch targets for gloved hands (60dp+)
 * - Construction safety color schemes
 * - Emergency mode support with high contrast
 * - Haptic feedback for construction environments
 * - WCAG accessibility compliance
 */

/**
 * Glass toggle switch with construction safety styling
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    label: String,
    description: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isEmergencyControl: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    // Toggle animation
    val toggleOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "toggle_offset"
    )
    
    // Emergency pulse animation
    val emergencyPulse by rememberInfiniteTransition(label = "emergency_pulse").animateFloat(
        initialValue = if (isEmergencyControl && checked) 0.7f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emergency_alpha"
    )
    
    Card(
        modifier = modifier
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(
                    bounded = true,
                    color = if (isEmergencyControl) ConstructionColors.CautionRed else ConstructionColors.SafetyOrange
                ),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(!checked)
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.6f)
            } else Color.Transparent
        ),
        border = if (isEmergencyControl && checked) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = ConstructionColors.CautionRed.copy(alpha = emergencyPulse)
            )
        } else if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = configuration.borderColor.copy(alpha = 0.4f)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = configuration.minTouchTargetSize.dp)
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = HazeMaterials.regular(
                                backgroundColor = if (isEmergencyControl && checked) {
                                    ConstructionColors.CautionRed.copy(alpha = 0.1f)
                                } else configuration.tintColor,
                                blurRadius = configuration.effectiveBlurRadius.dp
                            )
                        )
                    } else Modifier
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Label and description
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = if (enabled) {
                            if (isEmergencyControl && checked) {
                                ConstructionColors.CautionRed.copy(alpha = emergencyPulse)
                            } else Color.White
                        } else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Text content
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = label,
                        color = if (enabled) {
                            if (configuration.isHighContrastMode) configuration.borderColor else Color.White
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
                }
            }
            
            // Toggle switch control
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(32.dp)
            ) {
                // Track
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (checked) {
                            if (isEmergencyControl) {
                                ConstructionColors.CautionRed.copy(alpha = 0.8f)
                            } else ConstructionColors.SafetyOrange.copy(alpha = 0.8f)
                        } else Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize())
                }
                
                // Thumb
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    ),
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterStart)
                        .graphicsLayer {
                            translationX = toggleOffset * (56.dp - 28.dp).toPx()
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (checked && isEmergencyControl) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ConstructionColors.CautionRed,
                                modifier = Modifier.size(16.dp)
                            )
                        } else if (checked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ConstructionColors.SafetyGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Glass slider with construction safety styling and large touch targets
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    label: String,
    description: String? = null,
    icon: ImageVector? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    valueFormatter: (Float) -> String = { "${(it * 100).roundToInt()}%" },
    unit: String? = null,
    enabled: Boolean = true,
    isPerformanceCritical: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    // Color based on value and performance criticality
    val sliderColor = when {
        isPerformanceCritical && value < 0.3f -> ConstructionColors.CautionRed
        isPerformanceCritical && value < 0.6f -> ConstructionColors.SafetyOrange
        else -> ConstructionColors.SafetyGreen
    }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.6f)
            } else Color.Transparent
        ),
        border = if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = configuration.borderColor.copy(alpha = 0.4f)
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (configuration.supportLevel.name != "DISABLED") {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = HazeMaterials.regular(
                                backgroundColor = configuration.tintColor,
                                blurRadius = configuration.effectiveBlurRadius.dp
                            )
                        )
                    } else Modifier
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with label and current value
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label and icon
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (enabled) sliderColor else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (enabled) {
                                if (configuration.isHighContrastMode) configuration.borderColor else Color.White
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
                    }
                }
                
                // Current value display
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = sliderColor.copy(alpha = 0.2f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = sliderColor.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = valueFormatter(value) + (unit?.let { " $it" } ?: ""),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            // Custom slider track with large touch targets
            GlassCustomSliderTrack(
                value = value,
                onValueChange = { newValue ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(newValue)
                },
                valueRange = valueRange,
                steps = steps,
                color = sliderColor,
                configuration = configuration,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Custom slider track with enhanced touch targets for construction use
 */
@Composable
private fun GlassCustomSliderTrack(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    color: Color,
    configuration: GlassConfiguration,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val normalizedValue = (value - valueRange.start) / (valueRange.endInclusive - valueRange.start)
    
    var isDragging by remember { mutableStateOf(false) }
    val trackHeight = 8.dp
    val thumbSize = configuration.minTouchTargetSize.dp.coerceAtLeast(48.dp)
    
    Box(
        modifier = modifier.height(thumbSize),
        contentAlignment = Alignment.CenterStart
    ) {
        // Track background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(Color.White.copy(alpha = 0.2f))
        )
        
        // Active track
        Box(
            modifier = Modifier
                .fillMaxWidth(normalizedValue)
                .height(trackHeight)
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0.8f),
                            color
                        )
                    )
                )
        )
        
        // Thumb
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 3.dp,
                color = color
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDragging) 8.dp else 4.dp
            ),
            modifier = Modifier
                .size(thumbSize)
                .graphicsLayer {
                    translationX = normalizedValue * (size.width - thumbSize.toPx())
                }
                .pointerInput(value) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false }
                    ) { _, dragAmount ->
                        val trackWidth = size.width - thumbSize.toPx()
                        val deltaValue = (dragAmount.x / trackWidth) * (valueRange.endInclusive - valueRange.start)
                        val newValue = (value + deltaValue).coerceIn(valueRange)
                        onValueChange(newValue)
                    }
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Thumb indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        
        // Step indicators (if steps > 0)
        if (steps > 0) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(steps + 2) { index ->
                    val stepValue = index.toFloat() / (steps + 1)
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (stepValue <= normalizedValue) color
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

/**
 * Glass dropdown/selection menu with construction safety styling
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun <T> GlassDropdownMenu(
    selectedValue: T,
    options: List<T>,
    onValueChange: (T) -> Unit,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    label: String,
    description: String? = null,
    icon: ImageVector? = null,
    optionFormatter: (T) -> String = { it.toString() },
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.6f)
            } else Color.Transparent
        ),
        border = if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = configuration.borderColor.copy(alpha = 0.4f)
            )
        } else null
    ) {
        Column {
            // Main selection area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = configuration.minTouchTargetSize.dp)
                    .then(
                        if (configuration.supportLevel.name != "DISABLED") {
                            Modifier.hazeChild(
                                state = hazeState,
                                style = HazeMaterials.regular(
                                    backgroundColor = configuration.tintColor,
                                    blurRadius = configuration.effectiveBlurRadius.dp
                                )
                            )
                        } else Modifier
                    )
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(
                            bounded = true,
                            color = ConstructionColors.SafetyOrange
                        ),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            expanded = !expanded
                        }
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Label and selected value
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Text content
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (enabled) {
                                if (configuration.isHighContrastMode) configuration.borderColor else Color.White
                            } else Color.White.copy(alpha = 0.5f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Text(
                            text = optionFormatter(selectedValue),
                            color = ConstructionColors.SafetyOrange,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        description?.let { desc ->
                            Text(
                                text = desc,
                                color = Color.White.copy(alpha = if (enabled) 0.7f else 0.4f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                // Dropdown arrow
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Dropdown options
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color.Black.copy(alpha = 0.2f)
                        )
                ) {
                    options.forEach { option ->
                        val isSelected = option == selectedValue
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = rememberRipple(
                                        bounded = true,
                                        color = ConstructionColors.SafetyOrange
                                    ),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onValueChange(option)
                                        expanded = false
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = optionFormatter(option),
                                color = if (isSelected) ConstructionColors.SafetyOrange else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = ConstructionColors.SafetyOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}