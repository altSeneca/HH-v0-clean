package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.ai.yolo.ConstructionHazardDetection

// Construction-safe colors
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)

/**
 * Hazard Overlay Component
 * Displays bounding boxes for detected hazards
 */
@Composable
fun HazardOverlay(
    hazards: List<ConstructionHazardDetection>,
    onHazardTap: (ConstructionHazardDetection) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (hazards.isEmpty()) return
    
    Canvas(modifier = modifier.fillMaxSize()) {
        hazards.forEach { hazard ->
            // Determine color based on severity
            val severityColor = when (hazard.severity.name) {
                "CRITICAL" -> Color(0xFFB71C1C).copy(alpha = 0.4f)
                "HIGH" -> DangerRed.copy(alpha = 0.4f)
                "MEDIUM" -> SafetyOrange.copy(alpha = 0.4f)
                else -> SafetyGreen.copy(alpha = 0.4f)
            }
            
            // Convert normalized coordinates to actual pixels
            val boundingBox = hazard.boundingBox
            val rectOffset = Offset(
                x = (boundingBox.x - boundingBox.width / 2) * size.width,
                y = (boundingBox.y - boundingBox.height / 2) * size.height
            )
            val rectSize = ComposeSize(
                width = boundingBox.width * size.width,
                height = boundingBox.height * size.height
            )
            
            // Draw filled rectangle
            drawRect(
                color = severityColor,
                topLeft = rectOffset,
                size = rectSize
            )
            
            // Draw border
            drawRect(
                color = severityColor.copy(alpha = 0.8f),
                topLeft = rectOffset,
                size = rectSize,
                style = Stroke(width = 3.dp.toPx())
            )
            
            // Draw confidence label
            val confidenceText = "${(boundingBox.confidence * 100).toInt()}%"
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 12.sp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
            }
            
            drawContext.canvas.nativeCanvas.drawText(
                confidenceText,
                rectOffset.x + rectSize.width / 2,
                rectOffset.y + 12.sp.toPx(),
                textPaint
            )
        }
    }
}