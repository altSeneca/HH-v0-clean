package com.hazardhawk.ui.camera.clear.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazardhawk.ui.camera.clear.theme.ClearDesignTokens
import kotlinx.coroutines.delay

/**
 * AIAnalysisBanner - Drop-Down Status Banner
 *
 * Appears after photo capture to show AI analysis status:
 * - "Starting AI analysis..."
 * - "Analyzing for safety hazards..."
 * - "AI Analysis Complete: 3 Hazards. Violation: Fall Protection."
 *
 * Pulses orange if critical hazard detected.
 * Auto-dismisses after 5 seconds.
 */
@Composable
fun AIAnalysisBanner(
    visible: Boolean,
    message: String,
    isCritical: Boolean = false,
    isComplete: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Pulse animation for critical hazards
    val infiniteTransition = rememberInfiniteTransition(label = "critical_pulse")
    val criticalPulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isCritical) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "critical_alpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(ClearDesignTokens.Animation.Normal)) +
                slideInVertically(
                    animationSpec = tween(ClearDesignTokens.Animation.Normal),
                    initialOffsetY = { -it }
                ),
        exit = fadeOut(animationSpec = tween(ClearDesignTokens.Animation.Smooth)) +
               slideOutVertically(
                   animationSpec = tween(ClearDesignTokens.Animation.Smooth),
                   targetOffsetY = { -it }
               ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ClearDesignTokens.Spacing.Large)
                .padding(top = ClearDesignTokens.Spacing.Large),
            color = ClearDesignTokens.Colors.TranslucentDark85,
            shape = RoundedCornerShape(ClearDesignTokens.CornerRadius.Medium)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ClearDesignTokens.Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ClearDesignTokens.Spacing.Medium)
            ) {
                // Left border for critical hazards
                if (isCritical) {
                    Box(
                        modifier = Modifier
                            .width(ClearDesignTokens.Border.Thick)
                            .height(48.dp)
                            .background(
                                ClearDesignTokens.Colors.SafetyOrange.copy(alpha = criticalPulse),
                                RoundedCornerShape(ClearDesignTokens.CornerRadius.Small)
                            )
                    )
                }

                // Status icon
                if (isComplete) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Analysis Complete",
                        tint = if (isCritical)
                            ClearDesignTokens.Colors.SafetyOrange
                        else
                            ClearDesignTokens.Colors.SuccessGreen,
                        modifier = Modifier.size(ClearDesignTokens.Sizing.SmallIcon)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ClearDesignTokens.Sizing.SmallIcon),
                        color = ClearDesignTokens.Colors.SafetyOrange,
                        strokeWidth = 2.dp
                    )
                }

                // Message text
                Text(
                    text = message,
                    color = ClearDesignTokens.Colors.TranslucentWhite90,
                    fontSize = ClearDesignTokens.Typography.BodyText,
                    fontWeight = if (isCritical) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * AI Analysis State
 */
sealed class AIAnalysisState {
    object Idle : AIAnalysisState()
    data class Analyzing(val progress: String) : AIAnalysisState()
    data class Complete(val message: String, val isCritical: Boolean) : AIAnalysisState()
    data class Error(val errorMessage: String) : AIAnalysisState()
}

/**
 * Auto-dismiss AI Analysis Banner
 * Shows banner for 5 seconds after completion, then auto-hides
 */
@Composable
fun AutoDismissAIAnalysisBanner(
    analysisState: AIAnalysisState,
    modifier: Modifier = Modifier
) {
    var showBanner by remember { mutableStateOf(false) }
    var currentState by remember { mutableStateOf<AIAnalysisState>(AIAnalysisState.Idle) }

    // Monitor state changes
    LaunchedEffect(analysisState) {
        currentState = analysisState

        when (analysisState) {
            is AIAnalysisState.Idle -> {
                showBanner = false
            }
            is AIAnalysisState.Analyzing -> {
                showBanner = true
            }
            is AIAnalysisState.Complete -> {
                showBanner = true
                // Auto-dismiss after 5 seconds
                delay(ClearDesignTokens.Timing.AIBannerDismissMs)
                showBanner = false
            }
            is AIAnalysisState.Error -> {
                showBanner = true
                // Auto-dismiss errors after 3 seconds
                delay(3000L)
                showBanner = false
            }
        }
    }

    // Render banner based on state
    when (val state = currentState) {
        is AIAnalysisState.Idle -> {
            // No banner
        }
        is AIAnalysisState.Analyzing -> {
            AIAnalysisBanner(
                visible = showBanner,
                message = state.progress,
                isCritical = false,
                isComplete = false,
                modifier = modifier
            )
        }
        is AIAnalysisState.Complete -> {
            AIAnalysisBanner(
                visible = showBanner,
                message = state.message,
                isCritical = state.isCritical,
                isComplete = true,
                modifier = modifier
            )
        }
        is AIAnalysisState.Error -> {
            AIAnalysisBanner(
                visible = showBanner,
                message = state.errorMessage,
                isCritical = true,
                isComplete = false,
                modifier = modifier
            )
        }
    }
}
