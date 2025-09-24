package com.hazardhawk.camera

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ui.theme.ConstructionColors
import kotlin.math.min

/**
 * Unified Camera Overlay System
 * 
 * Single Canvas component that renders all camera overlay elements with perfect alignment
 * and optimal performance. Consolidates mask, border, grid, and metadata into one 
 * rendering operation to eliminate alignment issues and improve performance.
 * 
 * Key Features:
 * - Single Canvas for all overlay rendering (eliminates multiple Canvas overhead)
 * - Layered rendering system with proper z-ordering
 * - Shared coordinate system for perfect alignment
 * - Burnin prevention with safe area constraints
 * - Performance optimized with batched drawing operations
 */

/**
 * Configuration for overlay appearance and behavior
 */
data class OverlayConfiguration(
    val showMask: Boolean = true,
    val showBorder: Boolean = true,
    val showGrid: Boolean = false,
    val showMetadata: Boolean = true,
    val showCorners: Boolean = true,
    
    // Visual styling
    val maskColor: Color = Color.Black.copy(alpha = 0.8f),
    val borderColor: Color = ConstructionColors.SafetyOrange,
    val gridColor: Color = Color.White.copy(alpha = 0.3f),
    val metadataColor: Color = Color.White,
    
    // Dimensions
    val borderWidth: Float = 3f,
    val cornerLength: Float = 24f,
    val cornerWidth: Float = 4f,
    val gridType: GridType = GridType.RULE_OF_THIRDS,
    
    // Animation
    val animated: Boolean = true,
    val animationDurationMs: Int = 300
)

/**
 * Metadata positioning within the viewfinder
 */
data class MetadataOverlayInfo(
    val companyName: String? = null,
    val projectName: String? = null,
    val location: String? = null,
    val timestamp: String? = null,
    val position: MetadataPosition = MetadataPosition.BOTTOM_LEFT,
    val textSize: Float = 14f  // Increased to better match watermark visibility
)

/**
 * Complete unified camera overlay system
 * Replaces multiple Canvas components with single optimized renderer
 */
@Composable
fun UnifiedCameraOverlay(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    modifier: Modifier = Modifier,
    configuration: OverlayConfiguration = OverlayConfiguration(),
    metadata: MetadataOverlayInfo? = null
) {
    val animatedRatio by animateFloatAsState(
        targetValue = aspectRatio.ratio,
        animationSpec = tween(
            durationMillis = if (configuration.animated) configuration.animationDurationMs else 0
        ),
        label = "aspectRatio"
    )
    
    Canvas(modifier = modifier) {
        // Calculate unified bounds for all overlays
        val bounds = UnifiedViewfinderCalculator.calculateBoundsAnimated(
            canvasSize = size,
            currentRatio = animatedRatio,
            targetAspectRatio = aspectRatio,
            marginFactor = UnifiedViewfinderCalculator.calculateOptimalMarginFactor(size),
            safeAreaMargin = 16f
        )
        
        // Layer 1: Background mask (creates viewfinder window)
        if (configuration.showMask) {
            drawViewfinderMask(bounds, configuration.maskColor)
        }
        
        // Layer 2: Viewfinder border with corner indicators
        if (configuration.showBorder) {
            drawViewfinderBorder(
                bounds = bounds,
                color = configuration.borderColor,
                borderWidth = configuration.borderWidth,
                showCorners = configuration.showCorners,
                cornerLength = configuration.cornerLength,
                cornerWidth = configuration.cornerWidth
            )
        }
        
        // Layer 3: Grid overlay for composition guidance
        if (configuration.showGrid) {
            drawGridOverlay(
                bounds = bounds,
                gridType = configuration.gridType,
                color = configuration.gridColor
            )
        }
        
        // Layer 4: Metadata information (within safe area)
        if (configuration.showMetadata && metadata != null) {
            drawMetadataOverlay(
                bounds = bounds,
                metadata = metadata,
                color = configuration.metadataColor
            )
        }
    }
}

/**
 * Draw the viewfinder mask (black overlay with transparent center)
 */
private fun DrawScope.drawViewfinderMask(
    bounds: UnifiedViewfinderCalculator.ViewfinderBounds,
    maskColor: Color
) {
    // Top mask
    if (bounds.top > 0) {
        drawRect(
            color = maskColor,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, bounds.top)
        )
    }
    
    // Bottom mask
    if (bounds.bottom < size.height) {
        drawRect(
            color = maskColor,
            topLeft = Offset(0f, bounds.bottom),
            size = Size(size.width, size.height - bounds.bottom)
        )
    }
    
    // Left mask
    if (bounds.left > 0) {
        drawRect(
            color = maskColor,
            topLeft = Offset(0f, bounds.top),
            size = Size(bounds.left, bounds.height)
        )
    }
    
    // Right mask
    if (bounds.right < size.width) {
        drawRect(
            color = maskColor,
            topLeft = Offset(bounds.right, bounds.top),
            size = Size(size.width - bounds.right, bounds.height)
        )
    }
}

/**
 * Draw the viewfinder border with optional corner indicators
 */
private fun DrawScope.drawViewfinderBorder(
    bounds: UnifiedViewfinderCalculator.ViewfinderBounds,
    color: Color,
    borderWidth: Float,
    showCorners: Boolean,
    cornerLength: Float,
    cornerWidth: Float
) {
    val strokeWidth = borderWidth.dp.toPx()
    val cornerLengthPx = cornerLength.dp.toPx()
    val cornerWidthPx = cornerWidth.dp.toPx()
    
    // Main viewfinder border
    drawRect(
        color = color,
        topLeft = Offset(bounds.left, bounds.top),
        size = Size(bounds.width, bounds.height),
        style = Stroke(width = strokeWidth)
    )
    
    // Corner indicators for professional appearance
    if (showCorners) {
        // Top-left corner
        drawLine(
            color = color,
            start = Offset(bounds.left, bounds.top + cornerLengthPx),
            end = Offset(bounds.left, bounds.top),
            strokeWidth = cornerWidthPx
        )
        drawLine(
            color = color,
            start = Offset(bounds.left, bounds.top),
            end = Offset(bounds.left + cornerLengthPx, bounds.top),
            strokeWidth = cornerWidthPx
        )
        
        // Top-right corner
        drawLine(
            color = color,
            start = Offset(bounds.right - cornerLengthPx, bounds.top),
            end = Offset(bounds.right, bounds.top),
            strokeWidth = cornerWidthPx
        )
        drawLine(
            color = color,
            start = Offset(bounds.right, bounds.top),
            end = Offset(bounds.right, bounds.top + cornerLengthPx),
            strokeWidth = cornerWidthPx
        )
        
        // Bottom-left corner
        drawLine(
            color = color,
            start = Offset(bounds.left, bounds.bottom - cornerLengthPx),
            end = Offset(bounds.left, bounds.bottom),
            strokeWidth = cornerWidthPx
        )
        drawLine(
            color = color,
            start = Offset(bounds.left, bounds.bottom),
            end = Offset(bounds.left + cornerLengthPx, bounds.bottom),
            strokeWidth = cornerWidthPx
        )
        
        // Bottom-right corner
        drawLine(
            color = color,
            start = Offset(bounds.right - cornerLengthPx, bounds.bottom),
            end = Offset(bounds.right, bounds.bottom),
            strokeWidth = cornerWidthPx
        )
        drawLine(
            color = color,
            start = Offset(bounds.right, bounds.bottom - cornerLengthPx),
            end = Offset(bounds.right, bounds.bottom),
            strokeWidth = cornerWidthPx
        )
    }
}

/**
 * Draw grid overlay for composition guidance
 */
private fun DrawScope.drawGridOverlay(
    bounds: UnifiedViewfinderCalculator.ViewfinderBounds,
    gridType: GridType,
    color: Color
) {
    val strokeWidth = 1.dp.toPx()
    
    when (gridType) {
        GridType.RULE_OF_THIRDS -> {
            // Vertical lines (1/3 and 2/3)
            val verticalLine1 = bounds.left + bounds.width / 3f
            val verticalLine2 = bounds.left + bounds.width * 2f / 3f
            
            drawLine(
                color = color,
                start = Offset(verticalLine1, bounds.top),
                end = Offset(verticalLine1, bounds.bottom),
                strokeWidth = strokeWidth
            )
            
            drawLine(
                color = color,
                start = Offset(verticalLine2, bounds.top),
                end = Offset(verticalLine2, bounds.bottom),
                strokeWidth = strokeWidth
            )
            
            // Horizontal lines (1/3 and 2/3)
            val horizontalLine1 = bounds.top + bounds.height / 3f
            val horizontalLine2 = bounds.top + bounds.height * 2f / 3f
            
            drawLine(
                color = color,
                start = Offset(bounds.left, horizontalLine1),
                end = Offset(bounds.right, horizontalLine1),
                strokeWidth = strokeWidth
            )
            
            drawLine(
                color = color,
                start = Offset(bounds.left, horizontalLine2),
                end = Offset(bounds.right, horizontalLine2),
                strokeWidth = strokeWidth
            )
        }
        
        GridType.CENTER_LINES -> {
            // Vertical center line
            val centerX = bounds.centerX
            drawLine(
                color = color,
                start = Offset(centerX, bounds.top),
                end = Offset(centerX, bounds.bottom),
                strokeWidth = strokeWidth
            )
            
            // Horizontal center line
            val centerY = bounds.centerY
            drawLine(
                color = color,
                start = Offset(bounds.left, centerY),
                end = Offset(bounds.right, centerY),
                strokeWidth = strokeWidth
            )
        }
        
        GridType.DIAGONAL -> {
            // Main diagonals
            drawLine(
                color = color,
                start = Offset(bounds.left, bounds.top),
                end = Offset(bounds.right, bounds.bottom),
                strokeWidth = strokeWidth
            )
            
            drawLine(
                color = color,
                start = Offset(bounds.right, bounds.top),
                end = Offset(bounds.left, bounds.bottom),
                strokeWidth = strokeWidth
            )
        }
        
        GridType.GOLDEN_RATIO -> {
            // Golden ratio lines (approximately 0.618)
            val goldenRatio = 0.618f
            
            // Vertical lines
            val verticalLine1 = bounds.left + bounds.width * goldenRatio
            val verticalLine2 = bounds.left + bounds.width * (1f - goldenRatio)
            
            drawLine(
                color = color,
                start = Offset(verticalLine1, bounds.top),
                end = Offset(verticalLine1, bounds.bottom),
                strokeWidth = strokeWidth
            )
            
            drawLine(
                color = color,
                start = Offset(verticalLine2, bounds.top),
                end = Offset(verticalLine2, bounds.bottom),
                strokeWidth = strokeWidth
            )
            
            // Horizontal lines
            val horizontalLine1 = bounds.top + bounds.height * goldenRatio
            val horizontalLine2 = bounds.top + bounds.height * (1f - goldenRatio)
            
            drawLine(
                color = color,
                start = Offset(bounds.left, horizontalLine1),
                end = Offset(bounds.right, horizontalLine1),
                strokeWidth = strokeWidth
            )
            
            drawLine(
                color = color,
                start = Offset(bounds.left, horizontalLine2),
                end = Offset(bounds.right, horizontalLine2),
                strokeWidth = strokeWidth
            )
        }
        
        GridType.SAFETY_ZONES -> {
            // Safety zone grid for construction sites
            // Create a 2x2 grid with emphasis on safe working areas
            val centerX = bounds.centerX
            val centerY = bounds.centerY
            
            // Vertical center line
            drawLine(
                color = color,
                start = Offset(centerX, bounds.top),
                end = Offset(centerX, bounds.bottom),
                strokeWidth = strokeWidth * 2f
            )
            
            // Horizontal center line  
            drawLine(
                color = color,
                start = Offset(bounds.left, centerY),
                end = Offset(bounds.right, centerY),
                strokeWidth = strokeWidth * 2f
            )
        }
    }
}

/**
 * Draw metadata overlay within safe area (burnin prevention)
 * Styled to match the final watermark appearance
 */
private fun DrawScope.drawMetadataOverlay(
    bounds: UnifiedViewfinderCalculator.ViewfinderBounds,
    metadata: MetadataOverlayInfo,
    color: Color
) {
    val textSizePx = metadata.textSize.sp.toPx()
    val textPaint = android.graphics.Paint().apply {
        this.color = color.toArgb()
        this.textSize = textSizePx
        this.isAntiAlias = true
        this.typeface = android.graphics.Typeface.DEFAULT_BOLD
        // Add shadow for better readability (matching watermark)
        setShadowLayer(6f, 3f, 3f, Color.Black.toArgb())
    }

    // Build metadata text using shared utility to ensure exact match with watermark
    val metadataLines = MetadataEmbedder.createMetadataLines(
        companyName = metadata.companyName,
        projectName = metadata.projectName,
        timestamp = metadata.timestamp,
        location = metadata.location
    )

    if (metadataLines.isEmpty()) return

    val lineHeight = textSizePx * 1.2f
    val totalTextHeight = metadataLines.size * lineHeight
    val padding = 16f  // Match watermark padding (20f -> 16f for preview)
    val topPadding = 16f
    val bottomPadding = 16f
    val overlayHeight = totalTextHeight + topPadding + bottomPadding

    // Position at bottom of viewfinder (matching watermark position)
    val textX = bounds.left + padding
    val overlayTop = bounds.bottom - overlayHeight
    val textStartY = overlayTop + topPadding + textSizePx

    // Draw background matching watermark style (alpha = 120 ≈ 0.47f)
    val backgroundPaint = android.graphics.Paint().apply {
        this.color = Color.Black.copy(alpha = 0.47f).toArgb()
    }

    // Background rect covering full width of viewfinder (flush to bottom)
    val backgroundRect = android.graphics.Rect(
        bounds.left.toInt(),
        overlayTop.toInt(),
        bounds.right.toInt(),
        bounds.bottom.toInt()  // Flush to bottom like watermark
    )

    drawContext.canvas.nativeCanvas.drawRect(backgroundRect, backgroundPaint)

    // Draw text lines exactly matching watermark layout
    metadataLines.forEachIndexed { index, line ->
        drawContext.canvas.nativeCanvas.drawText(
            line,
            textX,
            textStartY + (index * lineHeight),
            textPaint
        )
    }
}

/**
 * Helper function to calculate maximum text width for positioning
 */
private fun getMaxTextWidth(lines: List<String>, paint: android.graphics.Paint): Float {
    return lines.maxOfOrNull { paint.measureText(it) } ?: 0f
}

/**
 * Simplified overlay for basic use cases
 */
@Composable
fun BasicCameraOverlay(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    modifier: Modifier = Modifier,
    showGrid: Boolean = false
) {
    UnifiedCameraOverlay(
        aspectRatio = aspectRatio,
        modifier = modifier,
        configuration = OverlayConfiguration(
            showGrid = showGrid,
            showMetadata = false
        )
    )
}

/**
 * Professional overlay for construction documentation
 */
@Composable
fun ProfessionalCameraOverlay(
    aspectRatio: UnifiedViewfinderCalculator.ViewfinderAspectRatio,
    companyName: String?,
    projectName: String?,
    location: String?,
    timestamp: String?,
    modifier: Modifier = Modifier,
    showGrid: Boolean = true
) {
    UnifiedCameraOverlay(
        aspectRatio = aspectRatio,
        modifier = modifier,
        configuration = OverlayConfiguration(
            showGrid = showGrid,
            showMetadata = true,
            gridType = GridType.RULE_OF_THIRDS,
            borderColor = ConstructionColors.SafetyOrange,
            maskColor = Color.Black.copy(alpha = 0.7f)
        ),
        metadata = MetadataOverlayInfo(
            companyName = companyName,
            projectName = projectName,
            location = location,
            timestamp = timestamp,
            position = MetadataPosition.BOTTOM_LEFT
        )
    )
}