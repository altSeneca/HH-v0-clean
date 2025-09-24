package com.hazardhawk.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset

/**
 * Typography optimized for construction environments:
 * - High contrast for outdoor visibility
 * - Large touch targets for work gloves
 * - Strong shadows for readability over varied backgrounds
 * - OSHA-compliant color coding
 */
object ConstructionTypography {
    
    /**
     * Primary text style for hazard titles and critical information.
     * Optimized for high visibility in bright outdoor conditions.
     */
    val hazardTitle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )
    
    /**
     * OSHA code badge text - needs to be clearly readable.
     */
    val oshaCode = TextStyle(
        fontSize = 14.sp, 
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        letterSpacing = 0.5.sp,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.7f),
            offset = Offset(1f, 1f),
            blurRadius = 2f
        )
    )
    
    /**
     * Hazard description text for detailed information.
     */
    val hazardDescription = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        lineHeight = 18.sp,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.7f),
            offset = Offset(1f, 1f),
            blurRadius = 3f
        )
    )
    
    /**
     * Confidence and status indicators.
     */
    val confidenceText = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.6f),
            offset = Offset(1f, 1f),
            blurRadius = 2f
        )
    )
    
    /**
     * PPE status labels.
     */
    val ppeStatus = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        letterSpacing = 0.3.sp,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.7f),
            offset = Offset(1f, 1f),
            blurRadius = 2f
        )
    )
    
    /**
     * Camera control labels - need to be visible over various backgrounds.
     */
    val cameraControl = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )
    
    /**
     * Large text for critical warnings that need immediate attention.
     */
    val criticalWarning = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
        color = HazardColors.CRITICAL_RED,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.9f),
            offset = Offset(3f, 3f),
            blurRadius = 6f
        )
    )
    
    /**
     * Analysis results header text.
     */
    val analysisHeader = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )
    
    /**
     * Secondary information text with lower priority.
     */
    val secondaryInfo = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = HazardColors.TEXT_SECONDARY,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.5f),
            offset = Offset(1f, 1f),
            blurRadius = 2f
        )
    )
    
    /**
     * Get text style based on severity for dynamic emphasis.
     */
    fun getSeverityTextStyle(severity: com.hazardhawk.ai.models.Severity): TextStyle {
        return when (severity) {
            com.hazardhawk.ai.models.Severity.CRITICAL -> criticalWarning
            com.hazardhawk.ai.models.Severity.HIGH -> hazardTitle.copy(
                color = HazardColors.HIGH_ORANGE,
                fontSize = 17.sp
            )
            com.hazardhawk.ai.models.Severity.MEDIUM -> hazardTitle.copy(
                color = HazardColors.MEDIUM_AMBER
            )
            com.hazardhawk.ai.models.Severity.LOW -> hazardTitle.copy(
                color = HazardColors.LOW_YELLOW,
                fontSize = 15.sp
            )
        }
    }
    
    /**
     * Get text style with appropriate color for confidence level.
     */
    fun getConfidenceTextStyle(confidence: Float): TextStyle {
        return confidenceText.copy(
            color = HazardColors.getConfidenceColor(confidence)
        )
    }
}