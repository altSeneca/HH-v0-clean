package com.hazardhawk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.coroutines.delay

/**
 * Intelligent AI Progress Component for HazardHawk
 * 
 * Educational progress indicators that teach safety concepts while analyzing photos.
 * Construction-optimized design with:
 * - Large, readable text for safety glasses
 * - High contrast colors for outdoor visibility
 * - Educational messaging that builds safety knowledge
 * - Smooth animations to indicate active processing
 */

/**
 * Main AI Progress component with educational steps
 */
@Composable
fun IntelligentAIProgress(
    currentStep: AIAnalysisStep,
    progress: Float = 0f,
    analysisMode: AIAnalysisMode = AIAnalysisMode.LOCAL,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var animatedProgress by remember { mutableFloatStateOf(0f) }
    
    // Animate progress changes
    LaunchedEffect(progress) {
        val animationSpec = tween<Float>(
            durationMillis = 800,
            easing = EaseInOutCubic
        )
        animate(
            initialValue = animatedProgress,
            targetValue = progress,
            animationSpec = animationSpec
        ) { value, _ ->
            animatedProgress = value
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // AI Mode Indicator
            AIProcessingModeIndicator(
                mode = analysisMode,
                isActive = currentStep != AIAnalysisStep.COMPLETED
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Circular Progress with Safety Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                ConstructionCircularProgress(
                    progress = animatedProgress,
                    strokeWidth = 6.dp,
                    color = when (currentStep) {
                        AIAnalysisStep.PREPARING -> ConstructionColors.HighVisYellow
                        AIAnalysisStep.ANALYZING_HAZARDS -> ConstructionColors.SafetyOrange
                        AIAnalysisStep.MAPPING_OSHA -> ConstructionColors.WorkZoneBlue
                        AIAnalysisStep.GENERATING_REPORT -> ConstructionColors.SafetyGreen
                        AIAnalysisStep.COMPLETED -> ConstructionColors.SafetyGreen
                    }
                )
                
                Icon(
                    imageVector = currentStep.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = when (currentStep) {
                        AIAnalysisStep.PREPARING -> ConstructionColors.HighVisYellow
                        AIAnalysisStep.ANALYZING_HAZARDS -> ConstructionColors.SafetyOrange
                        AIAnalysisStep.MAPPING_OSHA -> ConstructionColors.WorkZoneBlue
                        AIAnalysisStep.GENERATING_REPORT -> ConstructionColors.SafetyGreen
                        AIAnalysisStep.COMPLETED -> ConstructionColors.SafetyGreen
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Current Step Information
            AIStepEducationalCard(
                step = currentStep,
                analysisMode = analysisMode
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Progress Steps Indicator
            AIProgressStepsIndicator(
                currentStep = currentStep,
                progress = animatedProgress
            )
            
            // Cancel Button (if provided)
            onCancel?.let {
                Spacer(modifier = Modifier.height(24.dp))
                FlikkerSecondaryButton(
                    onClick = it,
                    text = "Cancel Analysis",
                    icon = Icons.Default.Close,
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            }
        }
    }
}

/**
 * AI Processing Mode Indicator
 */
@Composable
fun AIProcessingModeIndicator(
    mode: AIAnalysisMode,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mode_indicator")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_pulse"
    )
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                when (mode) {
                    AIAnalysisMode.LOCAL -> ConstructionColors.SafetyGreen.copy(
                        alpha = if (isActive) alpha * 0.2f else 0.1f
                    )
                    AIAnalysisMode.CLOUD -> ConstructionColors.WorkZoneBlue.copy(
                        alpha = if (isActive) alpha * 0.2f else 0.1f
                    )
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (mode) {
                AIAnalysisMode.LOCAL -> Icons.Default.PhoneAndroid
                AIAnalysisMode.CLOUD -> Icons.Default.Cloud
            },
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = when (mode) {
                AIAnalysisMode.LOCAL -> ConstructionColors.SafetyGreen
                AIAnalysisMode.CLOUD -> ConstructionColors.WorkZoneBlue
            }
        )
        
        Text(
            text = when (mode) {
                AIAnalysisMode.LOCAL -> "Local AI Processing"
                AIAnalysisMode.CLOUD -> "Cloud AI Processing"
            },
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = when (mode) {
                AIAnalysisMode.LOCAL -> ConstructionColors.SafetyGreen
                AIAnalysisMode.CLOUD -> ConstructionColors.WorkZoneBlue
            }
        )
    }
}

/**
 * Educational card for current AI step
 */
@Composable
fun AIStepEducationalCard(
    step: AIAnalysisStep,
    analysisMode: AIAnalysisMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Step Title
        Text(
            text = step.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = ConstructionColors.SafetyOrange,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Educational Description
        Text(
            text = step.getEducationalDescription(analysisMode),
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Safety Tip (if available)
        step.safetyTip?.let { tip ->
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ConstructionColors.HighVisYellow.copy(alpha = 0.1f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = "Safety Tip",
                    modifier = Modifier.size(20.dp),
                    tint = ConstructionColors.HighVisYellow
                )
                
                Text(
                    text = tip,
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

/**
 * Progress steps indicator with construction-friendly design
 */
@Composable
fun AIProgressStepsIndicator(
    currentStep: AIAnalysisStep,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val steps = AIAnalysisStep.entries.toList()
    val currentIndex = steps.indexOf(currentStep)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            StepIndicator(
                step = step,
                isCompleted = index < currentIndex,
                isCurrent = index == currentIndex,
                progress = if (index == currentIndex) progress else if (index < currentIndex) 1f else 0f
            )
            
            // Add connector line (except for last item)
            if (index < steps.size - 1) {
                StepConnector(
                    isCompleted = index < currentIndex,
                    progress = if (index == currentIndex - 1) 1f else if (index < currentIndex - 1) 1f else 0f
                )
            }
        }
    }
}

/**
 * Individual step indicator
 */
@Composable
fun StepIndicator(
    step: AIAnalysisStep,
    isCompleted: Boolean,
    isCurrent: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val color = when {
        isCompleted -> ConstructionColors.SafetyGreen
        isCurrent -> ConstructionColors.SafetyOrange
        else -> MaterialTheme.colorScheme.outline
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isCurrent) {
                ConstructionCircularProgress(
                    progress = progress,
                    strokeWidth = 3.dp,
                    color = color,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Card(
                modifier = Modifier.size(24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = color
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isCurrent) 4.dp else 2.dp
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    } else {
                        Icon(
                            step.icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = step.shortTitle,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            ),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Connector line between steps
 */
@Composable
fun StepConnector(
    isCompleted: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val progressColor = if (isCompleted) {
        ConstructionColors.SafetyGreen
    } else {
        MaterialTheme.colorScheme.outline
    }
    val backgroundColor = MaterialTheme.colorScheme.outline
    
    Canvas(
        modifier = modifier
            .width(24.dp)
            .height(2.dp)
    ) {
        val strokeWidth = 4.dp.toPx()
        
        // Background line
        drawLine(
            color = backgroundColor,
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Progress line
        if (progress > 0f || isCompleted) {
            drawLine(
                color = progressColor,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width * (if (isCompleted) 1f else progress), size.height / 2),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Construction-optimized circular progress indicator
 */
@Composable
fun ConstructionCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = ConstructionColors.SafetyOrange,
    strokeWidth: Dp = 4.dp,
    backgroundColor: Color = color.copy(alpha = 0.2f)
) {
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
        val size = Size(radius * 2, radius * 2)
        
        // Background circle
        drawArc(
            color = backgroundColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = stroke
        )
        
        // Progress arc
        if (progress > 0f) {
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = stroke
            )
        }
    }
}

/**
 * AI Analysis Steps with educational content
 */
enum class AIAnalysisStep(
    val title: String,
    val shortTitle: String,
    val icon: ImageVector,
    val safetyTip: String? = null
) {
    PREPARING(
        title = "Preparing Analysis",
        shortTitle = "Prepare",
        icon = Icons.Default.Settings,
        safetyTip = "Always take photos from a safe distance from potential hazards"
    ),
    ANALYZING_HAZARDS(
        title = "Detecting Hazards",
        shortTitle = "Analyze",
        icon = Icons.Default.Search,
        safetyTip = "Our AI looks for fall hazards, electrical risks, PPE compliance, and environmental dangers"
    ),
    MAPPING_OSHA(
        title = "Mapping OSHA Standards",
        shortTitle = "OSHA",
        icon = Icons.Default.Gavel,
        safetyTip = "Each hazard is mapped to specific OSHA 1926 construction safety standards"
    ),
    GENERATING_REPORT(
        title = "Creating Safety Report",
        shortTitle = "Report",
        icon = Icons.Default.Description,
        safetyTip = "Reports include corrective actions and can be shared with your safety team"
    ),
    COMPLETED(
        title = "Analysis Complete",
        shortTitle = "Done",
        icon = Icons.Default.CheckCircle,
        safetyTip = "Review all identified hazards and take immediate action on high-priority items"
    );
    
    fun getEducationalDescription(mode: AIAnalysisMode): String {
        val baseDescription = when (this) {
            PREPARING -> "Optimizing your photo for hazard detection and safety analysis"
            ANALYZING_HAZARDS -> "Scanning for construction hazards including falls, electrical risks, and PPE violations"
            MAPPING_OSHA -> "Cross-referencing findings with OSHA 1926 construction safety standards"
            GENERATING_REPORT -> "Compiling detailed safety report with corrective actions and priorities"
            COMPLETED -> "Your safety analysis is ready with actionable recommendations"
        }
        
        val modeInfo = when (mode) {
            AIAnalysisMode.LOCAL -> " (processing on your device)"
            AIAnalysisMode.CLOUD -> " (using advanced cloud AI)"
        }
        
        return baseDescription + modeInfo
    }
}
