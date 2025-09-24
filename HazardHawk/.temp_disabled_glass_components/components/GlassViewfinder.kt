package com.hazardhawk.ui.glass.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
// Semantics import removed to fix compilation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import com.hazardhawk.camera.*
// GlassConfiguration is now defined in GlassExtensions.kt
import com.hazardhawk.ui.theme.ConstructionColors
import kotlin.math.min

/**
 * Glass viewfinder overlay with translucent masking and blur effects.
 * 
 * This component creates a glass morphism viewfinder that adapts to construction
 * environments while maintaining professional photo composition guidelines.
 * 
 * Features:
 * - Translucent viewfinder masking with Haze blur
 * - Adaptive opacity based on lighting conditions
 * - Construction-optimized grid overlays
 * - Safety orange glowing borders
 * - Performance-optimized rendering
 */
@Composable
fun GlassViewfinder(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    showGrid: Boolean = true,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Main glass viewfinder mask
        GlassViewfinderMask(
            aspectRatio = aspectRatio,
            configuration = configuration,
            hazeState = hazeState,
            modifier = Modifier.fillMaxSize()
        )
        
        // Glass grid overlay for composition guidance
        if (showGrid) {
            GlassGridOverlay(
                aspectRatio = aspectRatio,
                configuration = configuration,
                hazeState = hazeState,
                gridType = GridType.RULE_OF_THIRDS,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Aspect ratio indicator with glass effect
        GlassAspectRatioLabel(
            aspectRatio = aspectRatio,
            configuration = configuration,
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        )
        
        // Corner focus indicators with glass glow
        GlassViewfinderCorners(
            aspectRatio = aspectRatio,
            configuration = configuration,
            modifier = Modifier.fillMaxSize()
        )
        
        // Safety zone indicators for construction work
        if (configuration.isOutdoorMode) {
            GlassSafetyZoneIndicators(
                aspectRatio = aspectRatio,
                configuration = configuration,
                hazeState = hazeState,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Creates the glass morphism viewfinder mask with translucent overlay
 */
@Composable
private fun GlassViewfinderMask(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Animated aspect ratio transitions
    val animatedRatio by animateFloatAsState(
        targetValue = aspectRatio.ratio,
        animationSpec = if (configuration.animationsEnabled) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        } else {
            snap()
        }
    )
    
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val targetRatio = animatedRatio
        
        // Calculate viewfinder window dimensions
        val (windowWidth, windowHeight) = calculateViewfinderDimensions(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            targetRatio = targetRatio,
            widthMarginFactor = 0.95f,
            heightMarginFactor = 0.8f
        )
        
        // Center the viewfinder window
        val windowLeft = (canvasWidth - windowWidth) / 2f
        val windowTop = (canvasHeight - windowHeight) / 2f
        val windowRight = windowLeft + windowWidth
        val windowBottom = windowTop + windowHeight
        
        // Draw glass morphism overlay masks
        drawGlassOverlayMasks(
            canvasSize = size,
            windowBounds = Rect(windowLeft, windowTop, windowRight, windowBottom),
            configuration = configuration
        )
        
        // Draw the main viewfinder border with glow effect
        drawGlassViewfinderBorder(
            windowBounds = Rect(windowLeft, windowTop, windowRight, windowBottom),
            configuration = configuration
        )
    }
}

/**
 * Draw glass overlay masks around the viewfinder
 */
private fun DrawScope.drawGlassOverlayMasks(
    canvasSize: Size,
    windowBounds: Rect,
    configuration: GlassConfiguration
) {
    val maskColor = Color.Black.copy(alpha = configuration.effectiveOpacity * 0.8f)
    val glassEffect = if (configuration.supportLevel.name != "DISABLED") {
        // Create gradient for glass effect
        Brush.radialGradient(
            colors = listOf(
                maskColor,
                maskColor.copy(alpha = maskColor.alpha * 0.7f),
                maskColor.copy(alpha = maskColor.alpha * 0.9f)
            ),
            center = Offset(canvasSize.width / 2f, canvasSize.height / 2f),
            radius = minOf(canvasSize.width, canvasSize.height) / 2f
        )
    } else {
        SolidColor(maskColor)
    }
    
    // Top mask
    if (windowBounds.top > 0) {
        drawRect(
            brush = glassEffect,
            topLeft = Offset(0f, 0f),
            size = Size(canvasSize.width, windowBounds.top)
        )
    }
    
    // Bottom mask
    if (windowBounds.bottom < canvasSize.height) {
        drawRect(
            brush = glassEffect,
            topLeft = Offset(0f, windowBounds.bottom),
            size = Size(canvasSize.width, canvasSize.height - windowBounds.bottom)
        )
    }
    
    // Left mask
    if (windowBounds.left > 0) {
        drawRect(
            brush = glassEffect,
            topLeft = Offset(0f, windowBounds.top),
            size = Size(windowBounds.left, windowBounds.height)
        )
    }
    
    // Right mask
    if (windowBounds.right < canvasSize.width) {
        drawRect(
            brush = glassEffect,
            topLeft = Offset(windowBounds.right, windowBounds.top),
            size = Size(canvasSize.width - windowBounds.right, windowBounds.height)
        )
    }
}

/**
 * Draw the viewfinder border with glass glow effect
 */
private fun DrawScope.drawGlassViewfinderBorder(
    windowBounds: Rect,
    configuration: GlassConfiguration
) {
    val borderColor = if (configuration.isHighContrastMode) {
        configuration.borderColor
    } else {
        configuration.borderColor.copy(alpha = 0.9f)
    }
    
    val strokeWidth = configuration.borderWidth.dp.toPx()
    
    if (configuration.safetyBorderGlow) {
        // Draw outer glow effect
        val glowColors = listOf(
            borderColor.copy(alpha = 0.1f),
            borderColor.copy(alpha = 0.3f),
            borderColor
        )
        
        for (i in glowColors.indices) {
            val glowWidth = strokeWidth + (glowColors.size - i) * 2.dp.toPx()
            drawRect(
                color = glowColors[i],
                topLeft = Offset(windowBounds.left, windowBounds.top),
                size = Size(windowBounds.width, windowBounds.height),
                style = Stroke(width = glowWidth)
            )
        }
    }
    
    // Main border
    drawRect(
        color = borderColor,
        topLeft = Offset(windowBounds.left, windowBounds.top),
        size = Size(windowBounds.width, windowBounds.height),
        style = Stroke(width = strokeWidth)
    )
}

/**
 * Glass grid overlay for composition guidance
 */
@Composable
private fun GlassGridOverlay(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    gridType: GridType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val targetRatio = aspectRatio.ratio
        
        // Calculate viewfinder window bounds
        val (windowWidth, windowHeight) = calculateViewfinderDimensions(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            targetRatio = targetRatio
        )
        
        val windowLeft = (canvasWidth - windowWidth) / 2f
        val windowTop = (canvasHeight - windowHeight) / 2f
        
        // Draw glass-enhanced grid lines
        drawGlassGrid(
            windowBounds = Rect(windowLeft, windowTop, windowLeft + windowWidth, windowTop + windowHeight),
            gridType = gridType,
            configuration = configuration
        )
    }
}

/**
 * Draw grid lines with glass effect
 */
private fun DrawScope.drawGlassGrid(
    windowBounds: Rect,
    gridType: GridType,
    configuration: GlassConfiguration
) {
    val lineColor = if (configuration.isHighContrastMode) {
        Color.White
    } else {
        Color.White.copy(alpha = 0.6f)
    }
    
    val strokeWidth = if (configuration.isHighContrastMode) 2.dp.toPx() else 1.5.dp.toPx()
    
    when (gridType) {
        GridType.RULE_OF_THIRDS -> drawRuleOfThirdsGridGlass(windowBounds, lineColor, strokeWidth, configuration)
        GridType.GOLDEN_RATIO -> drawGoldenRatioGridGlass(windowBounds, lineColor, strokeWidth, configuration)
        GridType.CENTER_LINES -> drawCenterLinesGridGlass(windowBounds, lineColor, strokeWidth, configuration)
        GridType.DIAGONAL -> drawDiagonalGridGlass(windowBounds, lineColor, strokeWidth, configuration)
        GridType.SAFETY_ZONES -> drawSafetyZonesGridGlass(windowBounds, lineColor, strokeWidth, configuration)
    }
}

private fun DrawScope.drawRuleOfThirdsGridGlass(
    bounds: Rect,
    color: Color,
    strokeWidth: Float,
    configuration: GlassConfiguration
) {
    // Vertical lines with glass effect
    val verticalLine1 = bounds.left + bounds.width / 3f
    val verticalLine2 = bounds.left + bounds.width * 2f / 3f
    
    listOf(verticalLine1, verticalLine2).forEach { x ->
        if (configuration.safetyBorderGlow) {
            // Draw subtle glow
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(x, bounds.top),
                end = Offset(x, bounds.bottom),
                strokeWidth = strokeWidth * 2f
            )
        }
        
        drawLine(
            color = color,
            start = Offset(x, bounds.top),
            end = Offset(x, bounds.bottom),
            strokeWidth = strokeWidth,
            pathEffect = if (configuration.isOutdoorMode) null else PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        )
    }
    
    // Horizontal lines with glass effect
    val horizontalLine1 = bounds.top + bounds.height / 3f
    val horizontalLine2 = bounds.top + bounds.height * 2f / 3f
    
    listOf(horizontalLine1, horizontalLine2).forEach { y ->
        if (configuration.safetyBorderGlow) {
            // Draw subtle glow
            drawLine(
                color = color.copy(alpha = 0.3f),
                start = Offset(bounds.left, y),
                end = Offset(bounds.right, y),
                strokeWidth = strokeWidth * 2f
            )
        }
        
        drawLine(
            color = color,
            start = Offset(bounds.left, y),
            end = Offset(bounds.right, y),
            strokeWidth = strokeWidth,
            pathEffect = if (configuration.isOutdoorMode) null else PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
        )
    }
    
    // Draw intersection points with safety orange glow
    val intersections = listOf(
        Offset(verticalLine1, horizontalLine1),
        Offset(verticalLine2, horizontalLine1),
        Offset(verticalLine1, horizontalLine2),
        Offset(verticalLine2, horizontalLine2)
    )
    
    intersections.forEach { point ->
        if (configuration.safetyBorderGlow) {
            drawCircle(
                color = ConstructionColors.SafetyOrange.copy(alpha = 0.4f),
                radius = 12.dp.toPx(),
                center = point
            )
        }
        drawCircle(
            color = ConstructionColors.SafetyOrange,
            radius = 8.dp.toPx(),
            center = point,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// Simplified implementations for other grid types
private fun DrawScope.drawGoldenRatioGridGlass(bounds: Rect, color: Color, strokeWidth: Float, configuration: GlassConfiguration) {
    val goldenRatio = 1.618f
    val verticalLine1 = bounds.left + bounds.width / goldenRatio
    val verticalLine2 = bounds.left + bounds.width - (bounds.width / goldenRatio)
    val horizontalLine1 = bounds.top + bounds.height / goldenRatio
    val horizontalLine2 = bounds.top + bounds.height - (bounds.height / goldenRatio)
    
    // Draw lines with glass effect (similar to rule of thirds)
    listOf(verticalLine1, verticalLine2).forEach { x ->
        drawLine(color, Offset(x, bounds.top), Offset(x, bounds.bottom), strokeWidth)
    }
    listOf(horizontalLine1, horizontalLine2).forEach { y ->
        drawLine(color, Offset(bounds.left, y), Offset(bounds.right, y), strokeWidth)
    }
}

private fun DrawScope.drawCenterLinesGridGlass(bounds: Rect, color: Color, strokeWidth: Float, configuration: GlassConfiguration) {
    val centerX = bounds.centerX
    val centerY = bounds.centerY
    
    drawLine(color, Offset(centerX, bounds.top), Offset(centerX, bounds.bottom), strokeWidth)
    drawLine(color, Offset(bounds.left, centerY), Offset(bounds.right, centerY), strokeWidth)
    
    // Center point with glow
    if (configuration.safetyBorderGlow) {
        drawCircle(ConstructionColors.SafetyOrange.copy(alpha = 0.6f), 8.dp.toPx(), Offset(centerX, centerY))
    }
    drawCircle(ConstructionColors.SafetyOrange, 6.dp.toPx(), Offset(centerX, centerY), style = Stroke(width = 2.dp.toPx()))
}

private fun DrawScope.drawDiagonalGridGlass(bounds: Rect, color: Color, strokeWidth: Float, configuration: GlassConfiguration) {
    drawLine(color, Offset(bounds.left, bounds.top), Offset(bounds.right, bounds.bottom), strokeWidth)
    drawLine(color, Offset(bounds.right, bounds.top), Offset(bounds.left, bounds.bottom), strokeWidth)
}

private fun DrawScope.drawSafetyZonesGridGlass(bounds: Rect, color: Color, strokeWidth: Float, configuration: GlassConfiguration) {
    val zoneWidth = bounds.width / 4f
    val zoneHeight = bounds.height / 4f
    
    for (i in 1..3) {
        drawLine(color, Offset(bounds.left + zoneWidth * i, bounds.top), Offset(bounds.left + zoneWidth * i, bounds.bottom), strokeWidth)
        drawLine(color, Offset(bounds.left, bounds.top + zoneHeight * i), Offset(bounds.right, bounds.top + zoneHeight * i), strokeWidth)
    }
    
    // Safety indicators
    val safetyColor = ConstructionColors.HighVisYellow.copy(alpha = 0.8f)
    val dangerColor = ConstructionColors.CautionRed.copy(alpha = 0.8f)
    
    // Safe zone (center)
    drawCircle(
        color = safetyColor,
        radius = min(zoneWidth, zoneHeight) * 0.4f,
        center = bounds.center,
        style = Stroke(width = 4.dp.toPx())
    )
}

/**
 * Glass aspect ratio label
 */
@Composable
private fun GlassAspectRatioLabel(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .hazeEffect(state = hazeState),
        colors = CardDefaults.cardColors(
            containerColor = if (configuration.supportLevel.name == "DISABLED") {
                Color.Black.copy(alpha = 0.8f)
            } else Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (configuration.safetyBorderGlow) {
            androidx.compose.foundation.BorderStroke(1.dp, configuration.borderColor)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = aspectRatio.label,
                color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = aspectRatio.description.take(20) + if (aspectRatio.description.length > 20) "..." else "",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

/**
 * Glass viewfinder corners with safety orange glow
 */
@Composable
private fun GlassViewfinderCorners(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    configuration: GlassConfiguration,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val targetRatio = aspectRatio.ratio
        
        val (windowWidth, windowHeight) = calculateViewfinderDimensions(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            targetRatio = targetRatio
        )
        
        val windowLeft = (canvasWidth - windowWidth) / 2f
        val windowTop = (canvasHeight - windowHeight) / 2f
        val windowRight = windowLeft + windowWidth
        val windowBottom = windowTop + windowHeight
        
        val cornerSize = 24.dp.toPx()
        val cornerThickness = if (configuration.isHighContrastMode) 6.dp.toPx() else 4.dp.toPx()
        val cornerColor = if (configuration.isHighContrastMode) {
            configuration.borderColor
        } else {
            configuration.borderColor.copy(alpha = 0.9f)
        }
        
        // Draw corners with optional glow effect
        val corners = listOf(
            // Top-left
            listOf(
                Offset(windowLeft, windowTop + cornerSize) to Offset(windowLeft, windowTop),
                Offset(windowLeft, windowTop) to Offset(windowLeft + cornerSize, windowTop)
            ),
            // Top-right
            listOf(
                Offset(windowRight - cornerSize, windowTop) to Offset(windowRight, windowTop),
                Offset(windowRight, windowTop) to Offset(windowRight, windowTop + cornerSize)
            ),
            // Bottom-left
            listOf(
                Offset(windowLeft, windowBottom - cornerSize) to Offset(windowLeft, windowBottom),
                Offset(windowLeft, windowBottom) to Offset(windowLeft + cornerSize, windowBottom)
            ),
            // Bottom-right
            listOf(
                Offset(windowRight - cornerSize, windowBottom) to Offset(windowRight, windowBottom),
                Offset(windowRight, windowBottom - cornerSize) to Offset(windowRight, windowBottom)
            )
        )
        
        corners.forEach { cornerLines ->
            cornerLines.forEach { (start, end) ->
                if (configuration.safetyBorderGlow) {
                    // Draw glow effect
                    drawLine(
                        color = cornerColor.copy(alpha = 0.4f),
                        start = start,
                        end = end,
                        strokeWidth = cornerThickness * 2f,
                        cap = StrokeCap.Round
                    )
                }
                
                // Draw main corner line
                drawLine(
                    color = cornerColor,
                    start = start,
                    end = end,
                    strokeWidth = cornerThickness,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Glass safety zone indicators for construction work
 */
@Composable
private fun GlassSafetyZoneIndicators(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    configuration: GlassConfiguration,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    // Only show in outdoor/construction mode
    if (!configuration.isOutdoorMode) return
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .hazeEffect(state = hazeState),
            colors = CardDefaults.cardColors(
                containerColor = if (configuration.supportLevel.name == "DISABLED") {
                    Color.Black.copy(alpha = 0.8f)
                } else Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "CONSTRUCTION ZONES",
                    color = if (configuration.isHighContrastMode) configuration.borderColor else Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(ConstructionColors.HighVisYellow)
                    )
                    Text(
                        text = "Safe Zone",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(ConstructionColors.CautionRed)
                    )
                    Text(
                        text = "Caution Area",
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Reuse the existing viewfinder calculation logic
 */
private fun calculateViewfinderDimensions(
    canvasWidth: Float,
    canvasHeight: Float,
    targetRatio: Float,
    widthMarginFactor: Float = 0.95f,
    heightMarginFactor: Float = 0.8f
): Pair<Float, Float> {
    val canvasRatio = canvasWidth / canvasHeight
    val isPortraitScreen = canvasRatio < 1.0f
    val isLandscapeScreen = canvasRatio > 1.0f
    
    return when {
        isPortraitScreen -> {
            when {
                targetRatio <= 1.0f -> {
                    val width = canvasWidth * widthMarginFactor
                    val height = width / targetRatio
                    if (height <= canvasHeight * heightMarginFactor) {
                        Pair(width, height)
                    } else {
                        val constrainedHeight = canvasHeight * heightMarginFactor
                        val constrainedWidth = constrainedHeight * targetRatio
                        Pair(constrainedWidth, constrainedHeight)
                    }
                }
                else -> {
                    val width = canvasWidth * widthMarginFactor
                    val height = width / targetRatio
                    Pair(width, height)
                }
            }
        }
        isLandscapeScreen -> {
            when {
                targetRatio >= 1.0f -> {
                    val height = canvasHeight * heightMarginFactor
                    val width = height * targetRatio
                    if (width <= canvasWidth * widthMarginFactor) {
                        Pair(width, height)
                    } else {
                        val constrainedWidth = canvasWidth * widthMarginFactor
                        val constrainedHeight = constrainedWidth / targetRatio
                        Pair(constrainedWidth, constrainedHeight)
                    }
                }
                else -> {
                    val height = canvasHeight * heightMarginFactor
                    val width = height * targetRatio
                    Pair(width, height)
                }
            }
        }
        else -> {
            if (canvasRatio > targetRatio) {
                val width = canvasHeight * targetRatio * heightMarginFactor
                Pair(width, canvasHeight * heightMarginFactor)
            } else {
                val height = canvasWidth / targetRatio * heightMarginFactor
                Pair(canvasWidth * widthMarginFactor, height)
            }
        }
    }
}