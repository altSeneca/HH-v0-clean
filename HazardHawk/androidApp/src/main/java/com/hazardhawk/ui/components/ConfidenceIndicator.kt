package com.hazardhawk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors
import kotlin.math.roundToInt

/**
 * Confidence Indicator Components for HazardHawk AI Analysis
 * 
 * Construction-optimized design with:
 * - Clear visual hierarchy for confidence levels
 * - Color-coded risk assessment (matches construction safety standards)
 * - Large touch targets and readable text
 * - Educational explanations for confidence scores
 */

/**
 * Main Confidence Indicator Component
 */
@Composable
fun ConfidenceIndicator(
    confidence: Float, // 0.0 to 1.0
    hazardType: String,
    oshaReference: String? = null,
    analysisMode: AIAnalysisMode = AIAnalysisMode.LOCAL,
    showDetails: Boolean = false,
    onToggleDetails: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val confidenceLevel = getConfidenceLevel(confidence)
    val animatedConfidence by animateFloatAsState(
        targetValue = confidence,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "confidence_animation"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = confidenceLevel.backgroundColor.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(
            width = 2.dp,
            color = confidenceLevel.primaryColor
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Confidence Score Circle
                ConfidenceScoreCircle(
                    confidence = animatedConfidence,
                    level = confidenceLevel
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Hazard Information
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = hazardType,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = confidenceLevel.primaryColor
                    )
                    
                    oshaReference?.let { ref ->
                        Text(
                            text = "OSHA ${ref}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    ConfidenceLevelBadge(
                        level = confidenceLevel,
                        analysisMode = analysisMode
                    )
                }
                
                // Details Toggle
                IconButton(
                    onClick = onToggleDetails,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showDetails) "Hide details" else "Show details",
                        modifier = Modifier.size(24.dp),
                        tint = confidenceLevel.primaryColor
                    )
                }
            }
            
            // Expandable Details Section
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = confidenceLevel.primaryColor.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    ConfidenceDetailsSection(
                        confidence = confidence,
                        level = confidenceLevel,
                        analysisMode = analysisMode
                    )
                }
            }
        }
    }
}

/**
 * Circular confidence score display
 */
@Composable
fun ConfidenceScoreCircle(
    confidence: Float,
    level: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    val percentage = (confidence * 100).roundToInt()
    
    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated circular progress
        Canvas(modifier = Modifier.size(80.dp)) {
            val strokeWidth = 8.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)
            
            // Background circle
            drawArc(
                color = level.primaryColor.copy(alpha = 0.2f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Progress arc with gradient
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        level.primaryColor.copy(alpha = 0.7f),
                        level.primaryColor
                    ),
                    center = center
                ),
                startAngle = -90f,
                sweepAngle = 360f * confidence,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        // Percentage text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$percentage%",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = level.primaryColor
            )
        }
    }
}

/**
 * Confidence level badge
 */
@Composable
fun ConfidenceLevelBadge(
    level: ConfidenceLevel,
    analysisMode: AIAnalysisMode,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        level.primaryColor.copy(alpha = 0.15f),
                        level.primaryColor.copy(alpha = 0.08f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = level.primaryColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = level.icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = level.primaryColor
        )
        
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = level.primaryColor
        )
        
        // AI Mode indicator
        Icon(
            imageVector = when (analysisMode) {
                AIAnalysisMode.LOCAL -> Icons.Default.PhoneAndroid
                AIAnalysisMode.CLOUD -> Icons.Default.Cloud
            },
            contentDescription = "AI Mode",
            modifier = Modifier.size(12.dp),
            tint = level.primaryColor.copy(alpha = 0.7f)
        )
    }
}

/**
 * Detailed confidence information
 */
@Composable
fun ConfidenceDetailsSection(
    confidence: Float,
    level: ConfidenceLevel,
    analysisMode: AIAnalysisMode,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Confidence Explanation
        ConfidenceExplanationCard(
            confidence = confidence,
            level = level,
            analysisMode = analysisMode
        )
        
        // Recommendation Actions
        ConfidenceActionRecommendations(
            level = level
        )
    }
}

/**
 * Educational explanation of confidence score
 */
@Composable
fun ConfidenceExplanationCard(
    confidence: Float,
    level: ConfidenceLevel,
    analysisMode: AIAnalysisMode,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    Icons.Default.Psychology,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = ConstructionColors.WorkZoneBlue
                )
                Text(
                    text = "Confidence Analysis",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ConstructionColors.WorkZoneBlue
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = level.getExplanation(analysisMode),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Action recommendations based on confidence level
 */
@Composable
fun ConfidenceActionRecommendations(
    level: ConfidenceLevel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = level.backgroundColor.copy(alpha = 0.05f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = level.primaryColor.copy(alpha = 0.2f)
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
                    Icons.Default.Checklist,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = level.primaryColor
                )
                Text(
                    text = "Recommended Actions",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = level.primaryColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            level.recommendations.forEach { recommendation ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = level.primaryColor.copy(alpha = 0.7f)
                    )
                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Compact confidence indicator for lists
 */
@Composable
fun CompactConfidenceIndicator(
    confidence: Float,
    analysisMode: AIAnalysisMode,
    modifier: Modifier = Modifier
) {
    val level = getConfidenceLevel(confidence)
    val percentage = (confidence * 100).roundToInt()
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(level.backgroundColor.copy(alpha = 0.1f))
            .border(
                width = 1.dp,
                color = level.primaryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = level.icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = level.primaryColor
        )
        
        Text(
            text = "$percentage%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = level.primaryColor
        )
        
        Icon(
            imageVector = when (analysisMode) {
                AIAnalysisMode.LOCAL -> Icons.Default.PhoneAndroid
                AIAnalysisMode.CLOUD -> Icons.Default.Cloud
            },
            contentDescription = "AI Mode",
            modifier = Modifier.size(10.dp),
            tint = level.primaryColor.copy(alpha = 0.6f)
        )
    }
}

/**
 * Confidence Level Data Class
 */
data class ConfidenceLevel(
    val label: String,
    val icon: ImageVector,
    val primaryColor: Color,
    val backgroundColor: Color,
    val recommendations: List<String>
) {
    fun getExplanation(mode: AIAnalysisMode): String {
        val baseExplanation = when (label) {
            "High Confidence" -> "The AI is very confident about this hazard identification. Multiple visual indicators clearly match known safety patterns."
            "Medium Confidence" -> "The AI has moderate confidence in this identification. Some visual indicators match safety patterns, but additional verification may be helpful."
            "Low Confidence" -> "The AI has lower confidence in this identification. The visual patterns are less clear and manual verification is recommended."
            else -> "Confidence level explanation not available."
        }
        
        val modeContext = when (mode) {
            AIAnalysisMode.LOCAL -> " This analysis was performed using on-device AI for privacy and speed."
            AIAnalysisMode.CLOUD -> " This analysis uses advanced cloud AI models for enhanced accuracy."
        }
        
        return baseExplanation + modeContext
    }
}

/**
 * Get confidence level from float value
 */
fun getConfidenceLevel(confidence: Float): ConfidenceLevel {
    return when {
        confidence >= 0.8f -> ConfidenceLevel(
            label = "High Confidence",
            icon = Icons.Default.VerifiedUser,
            primaryColor = ConstructionColors.SafetyGreen,
            backgroundColor = ConstructionColors.SafetyGreen,
            recommendations = listOf(
                "Document this hazard immediately",
                "Implement corrective measures",
                "Verify compliance with OSHA standards",
                "Share with safety team"
            )
        )
        confidence >= 0.6f -> ConfidenceLevel(
            label = "Medium Confidence",
            icon = Icons.Default.Warning,
            primaryColor = ConstructionColors.HighVisYellow,
            backgroundColor = ConstructionColors.HighVisYellow,
            recommendations = listOf(
                "Conduct manual verification",
                "Take additional photos if needed",
                "Consult with safety supervisor",
                "Monitor area for changes"
            )
        )
        else -> ConfidenceLevel(
            label = "Low Confidence",
            icon = Icons.Default.ErrorOutline,
            primaryColor = ConstructionColors.CautionRed,
            backgroundColor = ConstructionColors.CautionRed,
            recommendations = listOf(
                "Requires manual inspection",
                "Retake photo with better lighting",
                "Get closer view if safe to do so",
                "Seek expert safety assessment"
            )
        )
    }
}
