package com.hazardhawk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.coroutines.delay

/**
 * Flikker UI Component Library for HazardHawk
 * 
 * ENFORCEMENT COMPLIANCE:
 * ✅ All components follow Flikker design standards
 * ✅ Construction-optimized touch targets (≥56dp)
 * ✅ High contrast colors for outdoor visibility
 * ✅ Consistent haptic feedback patterns
 * ✅ Safety-themed color palette
 * ✅ Accessibility-first design
 * ✅ Material Design 3 compliance
 */

// CORE COMPONENTS

/**
 * FlikkerDialog - Replaces AlertDialog with construction-optimized design
 * ✅ MANDATORY REPLACEMENT for AlertDialog
 */
@Composable
fun FlikkerDialog(
    onDismissRequest: () -> Unit,
    title: String? = null,
    content: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Title
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = ConstructionColors.SafetyOrange
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Content
                Box(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    dismissButton?.let {
                        Box(modifier = Modifier.weight(1f)) {
                            it()
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        confirmButton()
                    }
                }
            }
        }
    }
}

/**
 * FlikkerTextField - Replaces OutlinedTextField with construction optimization
 * ✅ MANDATORY REPLACEMENT for OutlinedTextField
 */
@Composable
fun FlikkerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp), // Construction-friendly height
            label = label?.let { { Text(it, fontSize = 16.sp) } },
            placeholder = placeholder?.let { { Text(it, fontSize = 16.sp) } },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            enabled = enabled,
            readOnly = readOnly,
            singleLine = singleLine,
            maxLines = maxLines,
            minLines = minLines,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ConstructionColors.SafetyOrange,
                focusedLabelColor = ConstructionColors.SafetyOrange,
                cursorColor = ConstructionColors.SafetyOrange,
                errorBorderColor = ConstructionColors.CautionRed,
                errorLabelColor = ConstructionColors.CautionRed
            ),
            textStyle = LocalTextStyle.current.copy(
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        )
        
        // Error message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = ConstructionColors.CautionRed,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/**
 * FlikkerLoadingIndicator - Replaces CircularProgressIndicator
 * ✅ MANDATORY REPLACEMENT for CircularProgressIndicator
 */
@Composable
fun FlikkerLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = ConstructionColors.SafetyOrange
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            )
        ),
        label = "rotation"
    )
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(size),
            strokeWidth = strokeWidth,
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

// INTERACTIVE COMPONENTS

/**
 * FlikkerPrimaryButton - Primary action button with construction optimization
 * ✅ MANDATORY REPLACEMENT for Button
 */
@Composable
fun FlikkerPrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = ConstructionColors.SafetyOrange,
    contentColor: Color = Color.White
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )
    
    Button(
        onClick = {
            if (!isLoading) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        modifier = modifier
            .heightIn(min = 56.dp)
            .scale(scale),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 12.dp,
            disabledElevation = 2.dp
        ),
        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 16.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                FlikkerLoadingIndicator(
                    size = 20.dp,
                    strokeWidth = 2.dp,
                    color = contentColor
                )
            } else {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = if (isLoading) "Loading..." else text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * FlikkerSecondaryButton - Secondary action button
 * ✅ MANDATORY REPLACEMENT for OutlinedButton/TextButton
 */
@Composable
fun FlikkerSecondaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val hapticFeedback = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (enabled && !isLoading) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )
    
    OutlinedButton(
        onClick = {
            if (!isLoading) {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
        },
        modifier = modifier
            .heightIn(min = 56.dp)
            .scale(scale),
        enabled = enabled && !isLoading,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ConstructionColors.SafetyOrange
        ),
        border = BorderStroke(2.dp, ConstructionColors.SafetyOrange),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 16.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                FlikkerLoadingIndicator(
                    size = 20.dp,
                    strokeWidth = 2.dp,
                    color = ConstructionColors.SafetyOrange
                )
            } else {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = if (isLoading) "Loading..." else text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * FlikkerDestructiveButton - For delete/remove actions
 * ✅ SPECIALIZED COMPONENT for destructive actions
 */
@Composable
fun FlikkerDestructiveButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Delete,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    FlikkerPrimaryButton(
        onClick = onClick,
        text = text,
        modifier = modifier,
        icon = icon,
        enabled = enabled,
        isLoading = isLoading,
        containerColor = ConstructionColors.CautionRed,
        contentColor = Color.White
    )
}

// NAVIGATION COMPONENTS

/**
 * FlikkerBackButton - Standardized back navigation
 * ✅ MANDATORY REPLACEMENT for IconButton with ArrowBack
 */
@Composable
fun FlikkerBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current
    
    IconButton(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.size(56.dp), // Large touch target
        enabled = enabled
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            modifier = Modifier.size(28.dp),
            tint = ConstructionColors.SafetyOrange
        )
    }
}

// SPECIALIZED COMPONENTS

/**
 * FlikkerEmailField - Specialized for email input
 * ✅ SPECIALIZED COMPONENT for email validation
 */
@Composable
fun FlikkerEmailField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Email Address",
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    FlikkerTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = "Enter your email address",
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = "Email",
                tint = ConstructionColors.SafetyOrange
            )
        },
        isError = isError,
        errorMessage = errorMessage,
        enabled = enabled,
        singleLine = true
    )
}

/**
 * FlikkerPasswordField - Specialized for password input with toggle
 * ✅ SPECIALIZED COMPONENT for password with visibility toggle
 */
@Composable
fun FlikkerPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Password",
    showPassword: Boolean = false,
    onTogglePasswordVisibility: () -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    FlikkerTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = "Enter your password",
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Password",
                tint = ConstructionColors.SafetyOrange
            )
        },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Hide password" else "Show password",
                    tint = ConstructionColors.SafetyOrange
                )
            }
        },
        isError = isError,
        errorMessage = errorMessage,
        enabled = enabled,
        singleLine = true
    )
}

/**
 * FlikkerSearchField - Specialized for search with clear button
 * ✅ SPECIALIZED COMPONENT for search functionality
 */
@Composable
fun FlikkerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    enabled: Boolean = true
) {
    FlikkerTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = ConstructionColors.SafetyOrange
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = ConstructionColors.SafetyOrange
                    )
                }
            }
        } else null,
        enabled = enabled,
        singleLine = true
    )
}

// AI ANALYSIS PROGRESS COMPONENTS

/**
 * AIAnalysisProgressDialog - Main progress dialog with construction-optimized stages
 * Displays educational safety content during AI processing delays
 */
@Composable
fun AIAnalysisProgressDialog(
    currentStep: AnalysisStep,
    progress: Float = 0f,
    onCancel: (() -> Unit)? = null,
    analysisMode: AnalysisMode = AnalysisMode.LOCAL,
    modifier: Modifier = Modifier
) {
    var currentTipIndex by remember { mutableIntStateOf(0) }
    
    // Rotate safety tips every 5 seconds
    LaunchedEffect(currentStep) {
        while (true) {
            delay(5000)
            currentTipIndex = (currentTipIndex + 1) % OSHA_SAFETY_TIPS.size
        }
    }
    
    FlikkerDialog(
        onDismissRequest = onCancel ?: {},
        title = "AI Safety Analysis",
        dismissOnBackPress = onCancel != null,
        dismissOnClickOutside = false,
        confirmButton = {
            if (onCancel != null) {
                FlikkerSecondaryButton(
                    onClick = onCancel,
                    text = "Cancel",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Analysis Mode Indicator
                AnalysisModeIndicator(
                    mode = analysisMode,
                    isActive = currentStep != AnalysisStep.COMPLETED
                )
                
                // Main Progress Circle with Stage Icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    ConstructionProgressBar(
                        progress = progress,
                        strokeWidth = 8.dp,
                        color = currentStep.color,
                        modifier = Modifier.size(140.dp)
                    )
                    
                    Icon(
                        imageVector = currentStep.icon,
                        contentDescription = currentStep.title,
                        modifier = Modifier.size(48.dp),
                        tint = currentStep.color
                    )
                }
                
                // Stage Progress Indicator
                AnalysisStageIndicator(
                    currentStep = currentStep,
                    progress = progress
                )
                
                // Educational Safety Content
                SafetyTipCard(
                    tip = OSHA_SAFETY_TIPS[currentTipIndex],
                    currentStep = currentStep
                )
            }
        },
        modifier = modifier
    )
}

/**
 * AnalysisStageIndicator - Visual progress through analysis stages
 */
@Composable
fun AnalysisStageIndicator(
    currentStep: AnalysisStep,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val steps = AnalysisStep.entries.toList().filter { it != AnalysisStep.COMPLETED }
    val currentIndex = steps.indexOf(currentStep)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            StageIndicatorItem(
                step = step,
                isCompleted = index < currentIndex,
                isCurrent = index == currentIndex,
                progress = if (index == currentIndex) progress else if (index < currentIndex) 1f else 0f
            )
            
            // Connector line between stages
            if (index < steps.size - 1) {
                StageConnector(
                    isCompleted = index < currentIndex,
                    progress = if (index == currentIndex - 1) progress else if (index < currentIndex - 1) 1f else 0f
                )
            }
        }
    }
}

/**
 * Individual stage indicator item
 */
@Composable
private fun StageIndicatorItem(
    step: AnalysisStep,
    isCompleted: Boolean,
    isCurrent: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        isCompleted -> ConstructionColors.SafetyGreen
        isCurrent -> step.color
        else -> MaterialTheme.colorScheme.outline
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent && !isCompleted) {
                ConstructionProgressBar(
                    progress = progress,
                    strokeWidth = 3.dp,
                    color = color,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Card(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = color
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isCurrent) 6.dp else 2.dp
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Completed",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    } else {
                        Icon(
                            step.icon,
                            contentDescription = step.title,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = step.shortTitle,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            ),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Connector line between stage indicators
 */
@Composable
private fun StageConnector(
    isCompleted: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val progressColor = if (isCompleted) {
        ConstructionColors.SafetyGreen
    } else {
        ConstructionColors.SafetyOrange
    }
    val backgroundColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    
    Canvas(
        modifier = modifier
            .width(32.dp)
            .height(3.dp)
    ) {
        val strokeWidth = 6.dp.toPx()
        val centerY = size.height / 2
        
        // Background line
        drawLine(
            color = backgroundColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Progress line
        if (progress > 0f || isCompleted) {
            drawLine(
                color = progressColor,
                start = Offset(0f, centerY),
                end = Offset(size.width * (if (isCompleted) 1f else progress), centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * SafetyTipCard - Educational content during processing delays
 */
@Composable
fun SafetyTipCard(
    tip: OSHASafetyTip,
    currentStep: AnalysisStep,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.HighVisYellow.copy(alpha = 0.1f)
        ),
        border = BorderStroke(2.dp, ConstructionColors.HighVisYellow.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Step information
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = currentStep.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = currentStep.color
                )
                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = currentStep.color
                )
            }
            
            Text(
                text = currentStep.description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Safety tip section
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "Safety Tip",
                    modifier = Modifier.size(24.dp),
                    tint = ConstructionColors.HighVisYellow
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Safety Tip",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = ConstructionColors.HighVisYellow
                    )
                    Text(
                        text = tip.tip,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (tip.oshaReference != null) {
                        Text(
                            text = "OSHA Reference: ${tip.oshaReference}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * ConstructionProgressBar - High-contrast progress visualization
 */
@Composable
fun ConstructionProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ConstructionColors.SafetyOrange,
    strokeWidth: Dp = 6.dp,
    backgroundColor: Color = color.copy(alpha = 0.2f)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(
            durationMillis = 800,
            easing = EaseInOutCubic
        ),
        label = "progress_animation"
    )
    
    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round
        )
        
        val radius = (size.minDimension - stroke.width) / 2
        val center = Offset(size.width / 2, size.height / 2)
        val topLeft = Offset(
            center.x - radius,
            center.y - radius
        )
        val arcSize = Size(radius * 2, radius * 2)
        
        // Background circle
        drawArc(
            color = backgroundColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
        
        // Progress arc
        if (animatedProgress > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }
    }
}

/**
 * AnalysisModeIndicator - Shows local vs cloud processing
 */
@Composable
fun AnalysisModeIndicator(
    mode: AnalysisMode,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mode_pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                when (mode) {
                    AnalysisMode.LOCAL -> ConstructionColors.SafetyGreen.copy(
                        alpha = if (isActive) alpha * 0.15f else 0.1f
                    )
                    AnalysisMode.CLOUD -> ConstructionColors.WorkZoneBlue.copy(
                        alpha = if (isActive) alpha * 0.15f else 0.1f
                    )
                }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (mode) {
                AnalysisMode.LOCAL -> Icons.Default.PhoneAndroid
                AnalysisMode.CLOUD -> Icons.Default.CloudQueue
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = when (mode) {
                AnalysisMode.LOCAL -> ConstructionColors.SafetyGreen
                AnalysisMode.CLOUD -> ConstructionColors.WorkZoneBlue
            }
        )
        
        Text(
            text = when (mode) {
                AnalysisMode.LOCAL -> "On-Device AI Processing"
                AnalysisMode.CLOUD -> "Cloud AI Processing"
            },
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = when (mode) {
                AnalysisMode.LOCAL -> ConstructionColors.SafetyGreen
                AnalysisMode.CLOUD -> ConstructionColors.WorkZoneBlue
            }
        )
    }
}

// DATA MODELS FOR AI ANALYSIS

/**
 * Analysis processing steps with construction-themed progression
 */
enum class AnalysisStep(
    val title: String,
    val shortTitle: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
) {
    PHOTO_PROCESSING(
        title = "Processing Photo",
        shortTitle = "Process",
        description = "Optimizing image quality and compression for AI analysis",
        icon = Icons.Default.PhotoCamera,
        color = ConstructionColors.HighVisYellow
    ),
    AI_ANALYSIS(
        title = "AI Analysis",
        shortTitle = "Analyze", 
        description = "Scanning for construction hazards and safety violations using advanced AI",
        icon = Icons.Default.Psychology,
        color = ConstructionColors.SafetyOrange
    ),
    HAZARD_DETECTION(
        title = "Hazard Detection",
        shortTitle = "Hazards",
        description = "Identifying fall risks, electrical dangers, PPE violations, and environmental hazards",
        icon = Icons.Default.Warning,
        color = ConstructionColors.CautionRed
    ),
    COMPLIANCE_CHECKING(
        title = "OSHA Compliance",
        shortTitle = "OSHA",
        description = "Cross-referencing findings with OSHA 1926 construction safety standards",
        icon = Icons.Default.Gavel,
        color = ConstructionColors.WorkZoneBlue
    ),
    COMPLETED(
        title = "Analysis Complete",
        shortTitle = "Complete",
        description = "Safety analysis ready with actionable recommendations",
        icon = Icons.Default.CheckCircle,
        color = ConstructionColors.SafetyGreen
    )
}

/**
 * AI processing modes
 */
enum class AnalysisMode {
    LOCAL,   // On-device processing
    CLOUD    // Cloud-based processing
}

/**
 * Educational OSHA safety tips
 */
data class OSHASafetyTip(
    val tip: String,
    val category: String,
    val oshaReference: String? = null
)

/**
 * Database of OSHA safety tips for educational content
 */
private val OSHA_SAFETY_TIPS = listOf(
    OSHASafetyTip(
        tip = "Always maintain three points of contact when climbing ladders - two hands and one foot, or two feet and one hand.",
        category = "Fall Protection",
        oshaReference = "1926.1053(a)(3)(ii)"
    ),
    OSHASafetyTip(
        tip = "Hard hats must be worn in areas where there is potential for head injury from falling or flying objects.",
        category = "PPE",
        oshaReference = "1926.95(a)"
    ),
    OSHASafetyTip(
        tip = "Scaffolding must be inspected by a competent person before each work shift and after any occurrence that could affect structural integrity.",
        category = "Scaffolding",
        oshaReference = "1926.451(f)(3)"
    ),
    OSHASafetyTip(
        tip = "Electrical equipment must be de-energized and locked out before maintenance work begins.",
        category = "Electrical Safety",
        oshaReference = "1926.417"
    ),
    OSHASafetyTip(
        tip = "Eye and face protection must be provided when machines or operations present potential eye or face injury.",
        category = "PPE",
        oshaReference = "1926.102(a)(1)"
    ),
    OSHASafetyTip(
        tip = "Excavations 5 feet or deeper require a protective system such as sloping, benching, or shoring.",
        category = "Excavation",
        oshaReference = "1926.652(a)(1)"
    ),
    OSHASafetyTip(
        tip = "Safety nets must be installed as close as practicable under the walking/working surface, but never more than 30 feet below.",
        category = "Fall Protection", 
        oshaReference = "1926.502(c)(1)"
    ),
    OSHASafetyTip(
        tip = "Compressed gas cylinders must be secured in an upright position and protected from physical damage.",
        category = "Hazardous Materials",
        oshaReference = "1926.350(a)(9)"
    ),
    OSHASafetyTip(
        tip = "Respirators must be provided when workers are exposed to harmful dusts, fogs, fumes, mists, gases, smokes, sprays, or vapors.",
        category = "PPE",
        oshaReference = "1926.103(a)(1)"
    ),
    OSHASafetyTip(
        tip = "Power tools must be fitted with guards and safety switches to prevent accidental operation.",
        category = "Tool Safety",
        oshaReference = "1926.300(d)(1)"
    ),
    OSHASafetyTip(
        tip = "Workers exposed to noise levels of 85 dBA or higher must be enrolled in a hearing conservation program.",
        category = "Noise Control",
        oshaReference = "1926.101(b)"
    ),
    OSHASafetyTip(
        tip = "Guardrails must be installed on open sides of scaffolds more than 10 feet above the ground.",
        category = "Fall Protection",
        oshaReference = "1926.451(g)(1)"
    )
)

// USAGE EXAMPLE AND INTEGRATION

// Note: AIAnalysisState and related components moved to AIIntegrationExample.kt to avoid conflicts
