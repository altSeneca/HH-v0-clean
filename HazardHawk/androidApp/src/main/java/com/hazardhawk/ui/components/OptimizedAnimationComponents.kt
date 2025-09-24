package com.hazardhawk.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * Optimized version of ElegantCaptureButton that replaces heavy infinite animations 
 * with simple, efficient press states to prevent InputDispatcher warnings
 */
@Composable
fun OptimizedCaptureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCapturing: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    
    // Simple press state instead of continuous animation
    var isPressed by remember { mutableStateOf(false) }
    val pressScale = if (isPressed) 0.95f else 1f
    
    // Only animate color, not scale, for better performance
    val containerColor by animateColorAsState(
        targetValue = if (isCapturing) ConstructionColors.SafetyOrange else Color.White,
        animationSpec = tween(durationMillis = 150),
        label = "containerColor"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .size(80.dp)
            .scale(pressScale),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor
        ),
        shape = CircleShape,
        border = BorderStroke(4.dp, Color.White),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 8.dp,
            pressedElevation = 2.dp
        ),
        interactionSource = interactionSource
    ) {
        // Simple static content instead of animated
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(Color.Black, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCapturing) {
                // Static capture indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                )
            }
        }
    }
    
    // Track press state for simple scaling
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.PressInteraction.Press -> isPressed = true
                is androidx.compose.foundation.interaction.PressInteraction.Release,
                is androidx.compose.foundation.interaction.PressInteraction.Cancel -> isPressed = false
            }
        }
    }
}

/**
 * Optimized project selector with simplified animations
 */
@Composable
fun OptimizedProjectSelector(
    selectedProject: String,
    availableProjects: List<String>,
    onProjectSelected: (String) -> Unit,
    onNewProject: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier) {
        // Main selector card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = !expanded
                },
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedProject.ifEmpty { "Select project..." },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Simple dropdown without complex animations
        if (expanded) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    // Available projects
                    availableProjects.forEach { project ->
                        ProjectOptionItem(
                            text = project,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onProjectSelected(project)
                                expanded = false
                            }
                        )
                    }
                    
                    // Add new project option
                    if (availableProjects.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            color = Color.Black.copy(alpha = 0.1f)
                        )
                    }
                    
                    ProjectOptionItem(
                        text = "+ Add New Project",
                        textColor = ConstructionColors.SafetyOrange,
                        fontWeight = FontWeight.SemiBold,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectOptionItem(
    text: String,
    textColor: Color = Color.Black.copy(alpha = 0.8f),
    fontWeight: FontWeight = FontWeight.Normal,
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = textColor,
        fontSize = 14.sp,
        fontWeight = fontWeight,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

/**
 * Optimized focus ring with minimal Canvas operations
 */
@Composable
fun OptimizedFocusRing(
    position: Offset?,
    modifier: Modifier = Modifier
) {
    // Simple visibility toggle instead of complex animations
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(position) {
        if (position != null) {
            isVisible = true
            kotlinx.coroutines.delay(1000) // Show for 1 second
            isVisible = false
        }
    }

    if (position != null && isVisible) {
        Canvas(modifier = modifier) {
            // Fixed size circle, no animation calculations
            drawCircle(
                color = Color.White,
                radius = 32.dp.toPx(),
                center = position,
                style = Stroke(width = 2.dp.toPx()),
                alpha = 0.8f
            )
            
            // Inner dot
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = position,
                alpha = 0.8f
            )
        }
    }
}

/**
 * Debounced button wrapper to prevent rapid-fire clicks
 */
@Composable
fun DebouncedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    debounceTimeMs: Long = 500L,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    shape: Shape = ButtonDefaults.shape,
    border: BorderStroke? = null,
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    
    Button(
        onClick = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > debounceTimeMs) {
                lastClickTime = currentTime
                onClick()
            }
        },
        modifier = modifier,
        colors = colors,
        shape = shape,
        border = border,
        elevation = elevation,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Optimized glass morphism button with simplified press states
 */
@Composable
fun OptimizedGlassButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val containerAlpha = if (isPressed) 0.2f else 0.1f
    val borderAlpha = if (isPressed) 0.3f else 0.2f

    DebouncedButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .size(40.dp)
            .indication(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // Remove ripple for better performance
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = containerAlpha)
        ),
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = borderAlpha)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Performance monitoring component to track frame drops
 */
@Composable
fun PerformanceMonitor(
    enabled: Boolean = true,
    onFrameDrop: (Long) -> Unit = {}
) {
    if (!enabled) return
    
    var lastFrameTime by remember { mutableLongStateOf(System.nanoTime()) }
    
    LaunchedEffect(Unit) {
        while (enabled) {
            val currentTime = System.nanoTime()
            val frameDuration = (currentTime - lastFrameTime) / 1_000_000L // Convert to ms
            
            if (frameDuration > 32L) { // More than 2 frames at 60fps
                onFrameDrop(frameDuration)
            }
            
            lastFrameTime = currentTime
            kotlinx.coroutines.delay(16L) // Check every frame
        }
    }
}

/**
 * Simple button press state tracker for debugging
 */
@Composable
fun TouchEventDebugger() {
    var touchCount by remember { mutableIntStateOf(0) }
    var lastTouchTime by remember { mutableLongStateOf(0L) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { _ ->
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastTouchTime < 100) { // Rapid touches
                        touchCount++
                        println("TouchDebugger: Rapid touch detected #$touchCount")
                    } else {
                        touchCount = 0
                    }
                    lastTouchTime = currentTime
                }
            }
    )
}