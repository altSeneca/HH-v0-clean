package com.hazardhawk.ui.camera.hud

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors
import kotlin.math.*

/**
 * Contextual Control Ring for Safety HUD Camera
 * 
 * Radial menu around central capture button optimized for construction gloves
 * One-thumb operation with 56dp minimum touch targets
 * Adaptive tools based on camera state
 */
@Composable
fun ContextualControlRing(
    tools: List<ControlRingTool>,
    visible: Boolean,
    emergencyMode: Boolean = false,
    onCapturePhoto: () -> Unit,
    onToolSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    var pressedTool by remember { mutableStateOf<String?>(null) }
    
    // Animation states
    val ringScale by animateFloatAsState(
        targetValue = if (visible && isExpanded) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )
    
    val captureButtonScale by animateFloatAsState(
        targetValue = if (pressedTool == "capture") 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh)
    )
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Control Ring Tools
        AnimatedVisibility(
            visible = visible && isExpanded,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            ControlRingLayout(
                tools = tools,
                ringScale = ringScale,
                emergencyMode = emergencyMode,
                onToolSelected = { toolId ->
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    pressedTool = toolId
                    onToolSelected(toolId)
                    isExpanded = false
                }
            )
        }
        
        // Central Capture Button
        CentralCaptureButton(
            scale = captureButtonScale,
            emergencyMode = emergencyMode,
            onCapture = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                pressedTool = "capture"
                onCapturePhoto()
            },
            onLongPress = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                isExpanded = !isExpanded
            }
        )
        
        // Ring expansion hint
        if (visible && !isExpanded) {
            RingExpansionHint(emergencyMode = emergencyMode)
        }
    }
    
    // Auto-collapse ring after inactivity
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            kotlinx.coroutines.delay(5000) // 5 seconds
            isExpanded = false
        }
    }
}

/**
 * Central capture button with construction-optimized design
 * Large touch target with clear visual feedback
 */
@Composable
private fun CentralCaptureButton(
    scale: Float,
    emergencyMode: Boolean,
    onCapture: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Surface(
        modifier = modifier
            .size((80 * scale).dp)
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onCapture() },
                    onLongPress = { onLongPress() }
                )
            },
        color = if (emergencyMode) {
            ConstructionColors.CautionRed
        } else {
            ConstructionColors.SafetyOrange
        },
        shadowElevation = 8.dp,
        shape = CircleShape
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Outer ring
            Canvas(modifier = Modifier.fillMaxSize()) { 
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2
                
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = radius - 4.dp.toPx(),
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
            }
            
            // Capture icon
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Capture Photo",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

/**
 * Control Ring Layout with 8 radial tool positions
 * Tools positioned around capture button with optimal thumb reach
 */
@Composable
private fun ControlRingLayout(
    tools: List<ControlRingTool>,
    ringScale: Float,
    emergencyMode: Boolean,
    onToolSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val ringRadius = with(density) { 120.dp.toPx() }
    
    Box(
        modifier = modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        tools.forEach { tool ->
            if (tool.visible) {
                val angle = getAngleForPosition(tool.position)
                val x = cos(angle) * ringRadius * ringScale
                val y = sin(angle) * ringRadius * ringScale
                
                ControlRingTool(
                    tool = tool,
                    emergencyMode = emergencyMode,
                    onSelected = { onToolSelected(tool.id) },
                    modifier = Modifier.offset(
                        x = with(density) { x.toDp() },
                        y = with(density) { y.toDp() }
                    )
                )
            }
        }
    }
}

/**
 * Individual Control Ring Tool Button
 * 56dp minimum touch target for gloved hands
 */
@Composable
private fun ControlRingTool(
    tool: ControlRingTool,
    emergencyMode: Boolean,
    onSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }
    
    val toolScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh)
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tool button
        Surface(
            modifier = Modifier
                .size((56 * toolScale).dp) // Construction glove-friendly size
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = tool.enabled
                ) {
                    isPressed = true
                    onSelected()
                },
            color = if (emergencyMode) {
                Color.White.copy(alpha = 0.9f)
            } else {
                ConstructionColors.SteelBlue.copy(alpha = 0.9f)
            },
            shadowElevation = 4.dp,
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForTool(tool.icon),
                    contentDescription = tool.label,
                    tint = if (emergencyMode) {
                        ConstructionColors.AsphaltBlack
                    } else {
                        Color.White
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Tool label
        Text(
            text = tool.label,
            color = if (emergencyMode) {
                Color.White
            } else {
                Color.White.copy(alpha = 0.9f)
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
    
    // Reset pressed state after animation
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * Ring Expansion Hint
 * Subtle animation indicating long-press to expand
 */
@Composable
private fun RingExpansionHint(
    emergencyMode: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Canvas(
        modifier = modifier.size(140.dp)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        
        drawCircle(
            color = if (emergencyMode) {
                Color.White.copy(alpha = pulseAlpha)
            } else {
                ConstructionColors.SafetyOrange.copy(alpha = pulseAlpha)
            },
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

/**
 * Helper functions for ring positioning and icons
 */

private fun getAngleForPosition(position: RingPosition): Float {
    return when (position) {
        RingPosition.TOP -> -PI.toFloat() / 2
        RingPosition.TOP_RIGHT -> -PI.toFloat() / 4
        RingPosition.RIGHT -> 0f
        RingPosition.BOTTOM_RIGHT -> PI.toFloat() / 4
        RingPosition.BOTTOM -> PI.toFloat() / 2
        RingPosition.BOTTOM_LEFT -> 3 * PI.toFloat() / 4
        RingPosition.LEFT -> PI.toFloat()
        RingPosition.TOP_LEFT -> -3 * PI.toFloat() / 4
    }
}

private fun getIconForTool(iconId: String): ImageVector {
    return when (iconId) {
        "gallery" -> Icons.Default.PhotoLibrary
        "flash_auto" -> Icons.Default.FlashAuto
        "flash_on" -> Icons.Default.FlashOn
        "flash_off" -> Icons.Default.FlashOff
        "settings" -> Icons.Default.Settings
        "zoom" -> Icons.Default.ZoomIn
        "check" -> Icons.Default.Check
        "close" -> Icons.Default.Close
        "camera" -> Icons.Default.CameraAlt
        "refresh" -> Icons.Default.Refresh
        "warning" -> Icons.Default.Warning
        else -> Icons.Default.Help
    }
}

/**
 * Construction-optimized haptic feedback patterns
 */
object ConstructionHaptics {
    fun capturePhoto(hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback) {
        // Strong feedback for photo capture confirmation
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    
    fun toolSelection(hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback) {
        // Medium feedback for tool selection
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    
    fun ringExpansion(hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback) {
        // Subtle feedback for ring expansion
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
    
    fun error(hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback) {
        // Alert pattern for errors (multiple short pulses)
        repeat(3) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
}