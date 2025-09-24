package com.hazardhawk.ui.glass.components

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeChild
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import com.hazardhawk.ui.glass.GlassConfiguration
import com.hazardhawk.ui.glass.GlassState

/**
 * Android implementation of GlassButton with Haze library integration.
 * Optimized for construction environments with large touch targets and haptic feedback.
 */
@Composable
actual fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier,
    config: GlassConfiguration,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    disabledContainerColor: Color,
    disabledContentColor: Color,
    shape: Shape?,
    elevation: Dp?,
    hapticFeedback: Boolean,
    emergencyMode: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val glassState = remember { GlassState.getInstance(context) }
    val hazeState = remember { HazeState() }
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    
    // Determine effective configuration
    val effectiveConfig = remember(config, glassState.supportLevel, emergencyMode) {
        when {
            emergencyMode -> GlassButtonConfig.emergency
            glassState.supportLevel == com.hazardhawk.ui.glass.GlassSupportLevel.DISABLED ->
                GlassButtonConfig.construction.copy(
                    blurRadius = 0f,
                    emergencyHighContrast = true
                )
            glassState.supportLevel == com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED ->
                GlassButtonConfig.construction.copy(
                    rippleEnabled = false
                )
            else -> GlassButtonConfig.construction
        }
    }
    
    val buttonShape = shape ?: RoundedCornerShape(effectiveConfig.cornerRadius.dp)
    val buttonElevation = elevation ?: 4.dp
    
    // Enhanced click handler with haptic feedback
    val enhancedOnClick = remember(onClick, hapticFeedback, effectiveConfig.hapticFeedbackEnabled) {
        {
            if (enabled) {
                if (hapticFeedback && effectiveConfig.hapticFeedbackEnabled) {
                    haptic.performHapticFeedback(
                        if (emergencyMode) HapticFeedbackType.LongPress
                        else HapticFeedbackType.TextHandleMove
                    )
                    
                    // Additional vibration for construction environments
                    performConstructionHaptic(context, emergencyMode)
                }
                onClick()
            }
        }
    }
    
    Box(
        modifier = modifier
            .defaultMinSize(
                minWidth = effectiveConfig.minTouchTargetSize,
                minHeight = effectiveConfig.minTouchTargetSize
            )
            .semantics {
                contentDescription = if (emergencyMode) "Emergency action button" else "Action button"
            },
        contentAlignment = Alignment.Center
    ) {
        // Background haze effect
        if (glassState.isEnabled && effectiveConfig.blurRadius > 0f && enabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(
                        hazeState,
                        backgroundColor = if (enabled) containerColor else disabledContainerColor,
                        blurRadius = effectiveConfig.blurRadius.dp
                    )
            )
        }
        
        // Button implementation
        Button(
            onClick = enhancedOnClick,
            modifier = Modifier.fillMaxSize(),
            enabled = enabled,
            shape = buttonShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (glassState.isEnabled && !emergencyMode) {
                    containerColor.copy(alpha = effectiveConfig.opacity)
                } else {
                    // Fallback to solid colors
                    if (emergencyMode) Color.Red.copy(alpha = 0.9f)
                    else Color(0xFFFF6B35).copy(alpha = 0.8f) // Safety orange
                },
                contentColor = if (emergencyMode) Color.White else contentColor,
                disabledContainerColor = disabledContainerColor,
                disabledContentColor = disabledContentColor
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = buttonElevation),
            border = BorderStroke(
                width = effectiveConfig.borderWidth.dp,
                color = if (enabled) {
                    if (emergencyMode) Color.White
                    else effectiveConfig.borderColor
                } else {
                    Color.Gray.copy(alpha = 0.3f)
                }
            ),
            interactionSource = interactionSource
        ) {
            // Glass effect child for backdrop blur
            if (glassState.isEnabled && effectiveConfig.blurRadius > 0f && enabled) {
                HazeChild(
                    state = hazeState,
                    shape = buttonShape,
                    modifier = Modifier.fillMaxSize()
                ) {
                    ButtonContent(
                        enabled = enabled,
                        emergencyMode = emergencyMode,
                        config = effectiveConfig,
                        content = content
                    )
                }
            } else {
                // Fallback without glass effect
                ButtonContent(
                    enabled = enabled,
                    emergencyMode = emergencyMode,
                    config = effectiveConfig,
                    content = content
                )
            }
        }
    }
    
    // Record frame for performance monitoring
    LaunchedEffect(Unit) {
        glassState.recordFrame()
    }
}

/**
 * Button content wrapper with construction optimizations
 */
@Composable
private fun ButtonContent(
    enabled: Boolean,
    emergencyMode: Boolean,
    config: GlassButtonConfig,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
    
    // Safety orange accent line for non-emergency buttons
    if (!emergencyMode && config.safetyOrangeAccent && enabled) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(Color(0xFFFF6B35))
                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
        )
    }
}

/**
 * Perform construction-specific haptic feedback
 */
private fun performConstructionHaptic(context: Context, emergency: Boolean) {
    try {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = if (emergency) {
                    // Double pulse for emergency
                    VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1)
                } else {
                    // Single pulse for normal actions
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                }
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                if (emergency) {
                    vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
                } else {
                    vibrator.vibrate(50)
                }
            }
        }
    } catch (e: Exception) {
        // Ignore vibration errors - not critical for functionality
    }
}

/**
 * Construction-optimized glass button with preset configurations
 */
@Composable
fun ConstructionGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonType: ConstructionButtonType = ConstructionButtonType.PRIMARY,
    enabled: Boolean = true,
    emergencyMode: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val config = when (buttonType) {
        ConstructionButtonType.PRIMARY -> GlassButtonConfig.primary
        ConstructionButtonType.SECONDARY -> GlassButtonConfig.secondary
        ConstructionButtonType.EMERGENCY -> GlassButtonConfig.emergency
        ConstructionButtonType.STANDARD -> GlassButtonConfig.construction
    }
    
    val containerColor = when (buttonType) {
        ConstructionButtonType.PRIMARY -> Color(0xFFFF6B35).copy(alpha = 0.8f) // Safety orange
        ConstructionButtonType.SECONDARY -> Color.Black.copy(alpha = 0.6f)
        ConstructionButtonType.EMERGENCY -> Color.Red.copy(alpha = 0.8f)
        ConstructionButtonType.STANDARD -> Color.Black.copy(alpha = 0.7f)
    }
    
    val contentColor = Color.White
    
    GlassButton(
        onClick = onClick,
        modifier = modifier,
        config = GlassConfiguration(
            blurRadius = config.blurRadius,
            opacity = config.opacity,
            animationsEnabled = config.rippleEnabled,
            enabledInEmergencyMode = buttonType == ConstructionButtonType.EMERGENCY,
            supportLevel = if (emergencyMode) {
                com.hazardhawk.ui.glass.GlassSupportLevel.DISABLED
            } else {
                com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED
            }
        ),
        enabled = enabled,
        containerColor = containerColor,
        contentColor = contentColor,
        hapticFeedback = config.hapticFeedbackEnabled,
        emergencyMode = emergencyMode || buttonType == ConstructionButtonType.EMERGENCY,
        content = content
    )
}

/**
 * Button types for construction environments
 */
enum class ConstructionButtonType {
    PRIMARY,    // Main actions with safety orange
    SECONDARY,  // Secondary actions 
    EMERGENCY,  // Emergency/critical actions
    STANDARD    // General purpose actions
}

/**
 * Safety-focused glass button with WCAG compliance
 */
@Composable
fun SafetyGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    safetyLevel: SafetyButtonLevel = SafetyButtonLevel.STANDARD,
    enabled: Boolean = true,
    emergencyMode: Boolean = false,
    accessibilityLabel: String = "",
    content: @Composable RowScope.() -> Unit
) {
    val safetyConfig = remember(safetyLevel) {
        when (safetyLevel) {
            SafetyButtonLevel.STANDARD -> GlassButtonConfig.construction
            SafetyButtonLevel.WARNING -> GlassButtonConfig.construction.copy(
                borderColor = Color(0xFFFFB020), // Warning orange
                borderWidth = 3.0f,
                minTouchTargetSize = 64.dp
            )
            SafetyButtonLevel.CRITICAL -> GlassButtonConfig.emergency.copy(
                minTouchTargetSize = 72.dp
            )
        }
    }
    
    val buttonColor = when (safetyLevel) {
        SafetyButtonLevel.STANDARD -> Color(0xFFFF6B35).copy(alpha = 0.8f)
        SafetyButtonLevel.WARNING -> Color(0xFFFFB020).copy(alpha = 0.8f)
        SafetyButtonLevel.CRITICAL -> Color.Red.copy(alpha = 0.9f)
    }
    
    GlassButton(
        onClick = onClick,
        modifier = modifier
            .semantics {
                if (accessibilityLabel.isNotEmpty()) {
                    contentDescription = accessibilityLabel
                }
            },
        config = GlassConfiguration(
            blurRadius = safetyConfig.blurRadius,
            opacity = safetyConfig.opacity,
            animationsEnabled = safetyConfig.rippleEnabled,
            enabledInEmergencyMode = safetyLevel == SafetyButtonLevel.CRITICAL,
            supportLevel = when (safetyLevel) {
                SafetyButtonLevel.CRITICAL -> com.hazardhawk.ui.glass.GlassSupportLevel.DISABLED
                else -> com.hazardhawk.ui.glass.GlassSupportLevel.REDUCED
            }
        ),
        enabled = enabled,
        containerColor = buttonColor,
        contentColor = Color.White,
        hapticFeedback = safetyConfig.hapticFeedbackEnabled,
        emergencyMode = emergencyMode || safetyLevel == SafetyButtonLevel.CRITICAL,
        content = content
    )
}

/**
 * Safety levels for construction button styling
 */
enum class SafetyButtonLevel {
    STANDARD,  // Normal operation
    WARNING,   // Caution required
    CRITICAL   // High risk/emergency action
}