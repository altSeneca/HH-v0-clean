package com.hazardhawk.ui.ar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hazardhawk.ai.models.BoundingBox
import com.hazardhawk.ai.models.Hazard
import com.hazardhawk.ai.models.Severity
import com.hazardhawk.ui.theme.HazardColors
import kotlin.math.*

/**
 * AR-style hazard bounding box with dynamic corner markers and overlay effects.
 * Provides clear visual indication of detected hazards in real-time camera feed.
 */
@Composable
fun HazardBoundingBox(
    hazard: Hazard,
    canvasSize: Size,
    modifier: Modifier = Modifier,
    showFill: Boolean = true,
    animationEnabled: Boolean = true
) {
    val density = LocalDensity.current
    val boundingBox = hazard.boundingBox ?: return
    
    // Animation state for pulsing critical hazards
    var animationPhase by remember { mutableFloatStateOf(0f) }
    
    if (animationEnabled && hazard.severity == Severity.CRITICAL) {
        LaunchedEffect(hazard.id) {
            while (true) {
                animationPhase = (animationPhase + 0.05f) % (2f * PI.toFloat())
                kotlinx.coroutines.delay(50)
            }
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        drawHazardBoundingBox(
            hazard = hazard,
            boundingBox = boundingBox,
            canvasSize = canvasSize,
            showFill = showFill,
            animationPhase = animationPhase,
            density = density
        )
    }
}

/**
 * Draw hazard bounding box with AR-style effects.
 */
private fun DrawScope.drawHazardBoundingBox(
    hazard: Hazard,
    boundingBox: BoundingBox,
    canvasSize: Size,
    showFill: Boolean,
    animationPhase: Float,
    density: androidx.compose.ui.unit.Density
) {
    val hazardColor = HazardColors.getSeverityColor(hazard.severity)
    val overlayAlpha = HazardColors.getOverlayAlpha(hazard.severity)
    
    // Convert normalized coordinates to canvas coordinates
    val left = boundingBox.left * canvasSize.width
    val top = boundingBox.top * canvasSize.height
    val width = boundingBox.width * canvasSize.width
    val height = boundingBox.height * canvasSize.height
    
    // Animation modifiers for critical hazards
    val strokeWidth = with(density) {
        val baseWidth = when (hazard.severity) {
            Severity.CRITICAL -> 4.dp
            Severity.HIGH -> 3.dp
            Severity.MEDIUM -> 2.5.dp
            Severity.LOW -> 2.dp
        }
        
        if (hazard.severity == Severity.CRITICAL) {
            // Pulsing effect for critical hazards
            val pulse = 1f + 0.3f * sin(animationPhase * 2f)
            (baseWidth * pulse).toPx()
        } else {
            baseWidth.toPx()
        }
    }
    
    // Semi-transparent fill for hazard area
    if (showFill) {
        val fillAlpha = if (hazard.severity == Severity.CRITICAL) {
            overlayAlpha + 0.1f * sin(animationPhase)
        } else {
            overlayAlpha
        }
        
        drawRect(
            color = hazardColor.copy(alpha = fillAlpha.coerceIn(0.1f, 0.4f)),
            topLeft = Offset(left, top),
            size = Size(width, height)
        )
    }
    
    // Main bounding box outline
    drawBoundingBoxOutline(
        color = hazardColor,
        topLeft = Offset(left, top),
        size = Size(width, height),
        strokeWidth = strokeWidth,
        severity = hazard.severity,
        animationPhase = animationPhase
    )
    
    // AR-style corner markers
    drawCornerMarkers(
        color = hazardColor,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerSize = with(density) { 20.dp.toPx() },
        strokeWidth = strokeWidth
    )
    
    // Confidence indicator line
    if (hazard.confidence > 0.7f) {
        drawConfidenceIndicator(
            confidence = hazard.confidence,
            topLeft = Offset(left, top),
            size = Size(width, height),
            color = HazardColors.getConfidenceColor(hazard.confidence)
        )
    }
}

/**
 * Draw the main bounding box outline with style based on severity.
 */
private fun DrawScope.drawBoundingBoxOutline(
    color: Color,
    topLeft: Offset,
    size: Size,
    strokeWidth: Float,
    severity: Severity,
    animationPhase: Float
) {
    val style = when (severity) {
        Severity.CRITICAL -> {
            // Solid line for critical hazards
            Stroke(width = strokeWidth)
        }
        Severity.HIGH -> {
            // Dashed line for high severity
            Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f), 0f)
            )
        }
        Severity.MEDIUM -> {
            // Dotted line for medium severity
            Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )
        }
        Severity.LOW -> {
            // Light dashed line for low severity
            Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }
    }
    
    drawRect(
        color = color,
        topLeft = topLeft,
        size = size,
        style = style
    )
}

/**
 * Draw AR-style corner markers for enhanced visibility.
 */
private fun DrawScope.drawCornerMarkers(
    color: Color,
    topLeft: Offset,
    size: Size,
    cornerSize: Float,
    strokeWidth: Float
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    
    // Top-left corner
    drawLine(
        color = color,
        start = topLeft,
        end = Offset(topLeft.x + cornerSize, topLeft.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = topLeft,
        end = Offset(topLeft.x, topLeft.y + cornerSize),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    
    // Top-right corner
    val topRight = Offset(topLeft.x + size.width, topLeft.y)
    drawLine(
        color = color,
        start = topRight,
        end = Offset(topRight.x - cornerSize, topRight.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = topRight,
        end = Offset(topRight.x, topRight.y + cornerSize),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    
    // Bottom-left corner
    val bottomLeft = Offset(topLeft.x, topLeft.y + size.height)
    drawLine(
        color = color,
        start = bottomLeft,
        end = Offset(bottomLeft.x + cornerSize, bottomLeft.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = bottomLeft,
        end = Offset(bottomLeft.x, bottomLeft.y - cornerSize),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    
    // Bottom-right corner
    val bottomRight = Offset(topLeft.x + size.width, topLeft.y + size.height)
    drawLine(
        color = color,
        start = bottomRight,
        end = Offset(bottomRight.x - cornerSize, bottomRight.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = bottomRight,
        end = Offset(bottomRight.x, bottomRight.y - cornerSize),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

/**
 * Draw confidence indicator as a colored line at the bottom of the bounding box.
 */
private fun DrawScope.drawConfidenceIndicator(
    confidence: Float,
    topLeft: Offset,
    size: Size,
    color: Color
) {
    val indicatorHeight = 4f
    val indicatorWidth = size.width * confidence
    val indicatorTop = topLeft.y + size.height + 2f
    
    drawRect(
        color = color.copy(alpha = 0.8f),
        topLeft = Offset(topLeft.x, indicatorTop),
        size = Size(indicatorWidth, indicatorHeight)
    )
}

/**
 * Multiple hazard bounding boxes overlay component.
 */
@Composable
fun MultiHazardBoundingBoxes(
    hazards: List<Hazard>,
    canvasSize: Size,
    modifier: Modifier = Modifier,
    showFill: Boolean = true,
    animationEnabled: Boolean = true
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        hazards.forEach { hazard ->
            hazard.boundingBox?.let { boundingBox ->
                drawHazardBoundingBox(
                    hazard = hazard,
                    boundingBox = boundingBox,
                    canvasSize = canvasSize,
                    showFill = showFill,
                    animationPhase = 0f, // Individual animation handled in single box component
                    density = LocalDensity.current
                )
            }
        }
    }
}

/**
 * Preview/static hazard bounding box for analysis results.
 */
@Composable
fun StaticHazardBoundingBox(
    hazard: Hazard,
    canvasSize: Size,
    modifier: Modifier = Modifier
) {
    HazardBoundingBox(
        hazard = hazard,
        canvasSize = canvasSize,
        modifier = modifier,
        showFill = true,
        animationEnabled = false
    )
}