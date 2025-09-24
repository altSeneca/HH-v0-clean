package com.hazardhawk.ui.ar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.hazardhawk.ai.models.Hazard
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.ai.models.Severity

/**
 * Real-time AR-style hazard detection overlay.
 * Displays hazard bounding boxes and OSHA badges over live camera feed.
 */
@Composable
fun HazardDetectionOverlay(
    safetyAnalysis: SafetyAnalysis?,
    modifier: Modifier = Modifier,
    onHazardClick: ((Hazard) -> Unit)? = null,
    showBoundingBoxes: Boolean = true,
    showOSHABadges: Boolean = true,
    animationEnabled: Boolean = true,
    compactMode: Boolean = false
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // Default canvas size based on screen dimensions
    LaunchedEffect(configuration) {
        canvasSize = Size(
            width = with(density) { configuration.screenWidthDp.dp.toPx() },
            height = with(density) { configuration.screenHeightDp.dp.toPx() }
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                canvasSize = size.toSize()
            }
    ) {
        
        // Render hazard overlays if analysis is available
        safetyAnalysis?.let { analysis ->
            
            // Critical hazards - render first with highest priority
            val criticalHazards = analysis.hazards.filter { it.severity == Severity.CRITICAL }
            val otherHazards = analysis.hazards.filter { it.severity != Severity.CRITICAL }
            
            // Bounding boxes layer
            if (showBoundingBoxes && canvasSize != Size.Zero) {
                // Critical hazards with animation
                criticalHazards.forEach { hazard ->
                    HazardBoundingBox(
                        hazard = hazard,
                        canvasSize = canvasSize,
                        showFill = true,
                        animationEnabled = animationEnabled
                    )
                }
                
                // Other hazards without animation
                MultiHazardBoundingBoxes(
                    hazards = otherHazards,
                    canvasSize = canvasSize,
                    showFill = true,
                    animationEnabled = false
                )
            }
            
            // OSHA badges and labels layer
            if (showOSHABadges) {
                analysis.hazards.forEach { hazard ->
                    hazard.boundingBox?.let { boundingBox ->
                        HazardBadgePositioned(
                            hazard = hazard,
                            boundingBox = boundingBox,
                            canvasSize = canvasSize,
                            onClick = onHazardClick?.let { { it(hazard) } },
                            isCompact = compactMode
                        )
                    }
                }
            }
        }
        
        // Real-time analysis status indicator
        AnalysisStatusIndicator(
            analysis = safetyAnalysis,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        
        // Quick stats overlay
        if (safetyAnalysis != null) {
            QuickStatsOverlay(
                analysis = safetyAnalysis,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Positioned OSHA badge component that calculates optimal placement.
 */
@Composable
private fun BoxScope.HazardBadgePositioned(
    hazard: Hazard,
    boundingBox: com.hazardhawk.ai.models.BoundingBox,
    canvasSize: Size,
    onClick: (() -> Unit)?,
    isCompact: Boolean
) {
    // Calculate badge position relative to bounding box
    val badgeOffset = calculateBadgePosition(boundingBox, canvasSize)
    
    Box(
        modifier = Modifier
            .offset(
                x = with(LocalDensity.current) { badgeOffset.x.toDp() },
                y = with(LocalDensity.current) { badgeOffset.y.toDp() }
            )
    ) {
        if (hazard.severity == Severity.CRITICAL) {
            CriticalOSHABadge(
                hazard = hazard,
                modifier = if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
        } else {
            OSHABadgeComponent(
                hazard = hazard,
                showConfidence = !isCompact,
                isCompact = isCompact,
                modifier = if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
        }
    }
}

/**
 * Calculate optimal badge position to avoid overlaps and stay visible.
 */
private fun calculateBadgePosition(
    boundingBox: com.hazardhawk.ai.models.BoundingBox,
    canvasSize: Size
): androidx.compose.ui.geometry.Offset {
    // Convert normalized coordinates to canvas coordinates
    val left = boundingBox.left * canvasSize.width
    val top = boundingBox.top * canvasSize.height
    val width = boundingBox.width * canvasSize.width
    val height = boundingBox.height * canvasSize.height
    
    // Default position: top-left of bounding box with offset
    var badgeX = left - 10f
    var badgeY = top - 40f
    
    // Ensure badge stays within canvas bounds
    if (badgeX < 10f) {
        badgeX = left + 10f // Move to right side if too far left
    }
    if (badgeY < 10f) {
        badgeY = top + height + 10f // Move below bounding box if too high
    }
    
    // Ensure badge doesn't exceed canvas bounds
    badgeX = badgeX.coerceIn(10f, canvasSize.width - 150f)
    badgeY = badgeY.coerceIn(10f, canvasSize.height - 100f)
    
    return androidx.compose.ui.geometry.Offset(badgeX, badgeY)
}

/**
 * Analysis status indicator showing current processing state.
 */
@Composable
private fun AnalysisStatusIndicator(
    analysis: SafetyAnalysis?,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    Box(
        modifier = modifier
            .background(
                color = com.hazardhawk.ui.theme.HazardColors.OVERLAY_BACKGROUND,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        when {
            analysis == null -> {
                Text(
                    text = "🔍 Analyzing...",
                    style = com.hazardhawk.ui.theme.ConstructionTypography.cameraControl,
                    color = com.hazardhawk.ui.theme.HazardColors.TEXT_SECONDARY
                )
            }
            analysis.hazards.isEmpty() -> {
                Text(
                    text = "✅ No Hazards",
                    style = com.hazardhawk.ui.theme.ConstructionTypography.cameraControl,
                    color = com.hazardhawk.ui.theme.HazardColors.SAFE_GREEN
                )
            }
            analysis.hazards.any { it.severity == Severity.CRITICAL } -> {
                Text(
                    text = "🚨 CRITICAL",
                    style = com.hazardhawk.ui.theme.ConstructionTypography.criticalWarning,
                    color = com.hazardhawk.ui.theme.HazardColors.CRITICAL_RED
                )
            }
            else -> {
                Text(
                    text = "⚠️ ${analysis.hazards.size} Issue${if (analysis.hazards.size > 1) "s" else ""}",
                    style = com.hazardhawk.ui.theme.ConstructionTypography.cameraControl,
                    color = com.hazardhawk.ui.theme.HazardColors.getSeverityColor(
                        analysis.hazards.maxByOrNull { it.severity.ordinal }?.severity ?: Severity.LOW
                    )
                )
            }
        }
    }
}

/**
 * Quick stats overlay showing analysis summary.
 */
@Composable
private fun QuickStatsOverlay(
    analysis: SafetyAnalysis,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = com.hazardhawk.ui.theme.HazardColors.OVERLAY_BACKGROUND.copy(alpha = 0.8f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${analysis.analysisType.name.replace("_", " ")}",
            style = com.hazardhawk.ui.theme.ConstructionTypography.secondaryInfo,
            color = com.hazardhawk.ui.theme.HazardColors.TEXT_SECONDARY
        )
        
        Text(
            text = "Confidence: ${(analysis.confidence * 100).toInt()}%",
            style = com.hazardhawk.ui.theme.ConstructionTypography.confidenceText,
            color = com.hazardhawk.ui.theme.HazardColors.getConfidenceColor(analysis.confidence)
        )
        
        if (analysis.processingTimeMs > 0) {
            Text(
                text = "${analysis.processingTimeMs}ms",
                style = com.hazardhawk.ui.theme.ConstructionTypography.secondaryInfo,
                color = com.hazardhawk.ui.theme.HazardColors.TEXT_SECONDARY
            )
        }
    }
}

/**
 * Hazard count indicator for quick reference.
 */
@Composable
fun HazardCountIndicator(
    hazards: List<Hazard>,
    modifier: Modifier = Modifier
) {
    val criticalCount = hazards.count { it.severity == Severity.CRITICAL }
    val highCount = hazards.count { it.severity == Severity.HIGH }
    val totalCount = hazards.size
    
    Row(
        modifier = modifier
            .background(
                color = com.hazardhawk.ui.theme.HazardColors.OVERLAY_BACKGROUND,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (criticalCount > 0) {
            HazardCountBadge(
                count = criticalCount,
                color = com.hazardhawk.ui.theme.HazardColors.CRITICAL_RED,
                label = "CRITICAL"
            )
        }
        
        if (highCount > 0) {
            HazardCountBadge(
                count = highCount,
                color = com.hazardhawk.ui.theme.HazardColors.HIGH_ORANGE,
                label = "HIGH"
            )
        }
        
        Text(
            text = "$totalCount total",
            style = com.hazardhawk.ui.theme.ConstructionTypography.secondaryInfo,
            color = com.hazardhawk.ui.theme.HazardColors.TEXT_SECONDARY
        )
    }
}

@Composable
private fun HazardCountBadge(
    count: Int,
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    androidx.compose.material3.Surface(
        color = color,
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = count.toString(),
                style = com.hazardhawk.ui.theme.ConstructionTypography.confidenceText.copy(
                    fontSize = 10.sp
                ),
                color = androidx.compose.ui.graphics.Color.White
            )
        }
    }
}