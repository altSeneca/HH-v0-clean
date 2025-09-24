package com.hazardhawk.ui.ar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hazardhawk.ai.models.*
import com.hazardhawk.ui.theme.HazardColors
import com.hazardhawk.ui.theme.ConstructionTypography

/**
 * OSHA code badge component for AR overlay display.
 * Shows OSHA regulation codes with appropriate severity styling.
 */
@Composable
fun OSHABadgeComponent(
    hazard: Hazard,
    modifier: Modifier = Modifier,
    showConfidence: Boolean = true,
    isCompact: Boolean = false
) {
    val severityColor = HazardColors.getSeverityColor(hazard.severity)
    val confidenceColor = HazardColors.getConfidenceColor(hazard.confidence)
    
    Column(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .background(
                color = HazardColors.OVERLAY_BACKGROUND,
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.5.dp,
                color = severityColor,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = if (isCompact) 6.dp else 8.dp,
                vertical = if (isCompact) 4.dp else 6.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        
        // OSHA Code Badge
        hazard.oshaCode?.let { code ->
            Box(
                modifier = Modifier
                    .background(
                        color = HazardColors.OSHA_BLUE,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(
                        horizontal = if (isCompact) 6.dp else 8.dp,
                        vertical = if (isCompact) 2.dp else 4.dp
                    )
            ) {
                Text(
                    text = code,
                    style = if (isCompact) {
                        ConstructionTypography.oshaCode.copy(fontSize = 12.sp)
                    } else {
                        ConstructionTypography.oshaCode
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Severity Indicator with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SeverityIcon(
                severity = hazard.severity,
                size = if (isCompact) 12.dp else 16.dp
            )
            
            Text(
                text = hazard.severity.name,
                style = if (isCompact) {
                    ConstructionTypography.ppeStatus.copy(fontSize = 11.sp)
                } else {
                    ConstructionTypography.ppeStatus
                },
                color = severityColor
            )
        }
        
        // Hazard Type Label
        if (!isCompact) {
            Text(
                text = formatHazardType(hazard.type),
                style = ConstructionTypography.hazardDescription.copy(fontSize = 12.sp),
                color = HazardColors.TEXT_SECONDARY,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        
        // Confidence Indicator
        if (showConfidence && hazard.confidence > 0.6f) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = if (hazard.confidence > 0.85f) Icons.Default.Verified else Icons.Default.Warning,
                    contentDescription = "Confidence Level",
                    tint = confidenceColor,
                    modifier = Modifier.size(if (isCompact) 10.dp else 12.dp)
                )
                
                Text(
                    text = "${(hazard.confidence * 100).toInt()}%",
                    style = if (isCompact) {
                        ConstructionTypography.confidenceText.copy(fontSize = 10.sp)
                    } else {
                        ConstructionTypography.confidenceText
                    },
                    color = confidenceColor
                )
            }
        }
    }
}

/**
 * Severity icon component for visual hazard identification.
 */
@Composable
private fun SeverityIcon(
    severity: Severity,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val (icon, color) = when (severity) {
        Severity.CRITICAL -> Icons.Default.Warning to HazardColors.CRITICAL_RED
        Severity.HIGH -> Icons.Default.Warning to HazardColors.HIGH_ORANGE
        Severity.MEDIUM -> Icons.Default.Warning to HazardColors.MEDIUM_AMBER
        Severity.LOW -> Icons.Default.Warning to HazardColors.LOW_YELLOW
    }
    
    Icon(
        imageVector = icon,
        contentDescription = "${severity.name} severity",
        tint = color,
        modifier = modifier.size(size)
    )
}

/**
 * Format hazard type for display.
 */
private fun formatHazardType(type: HazardType): String {
    return when (type) {
        HazardType.FALL_PROTECTION -> "Fall Risk"
        HazardType.PPE_VIOLATION -> "PPE Issue"
        HazardType.ELECTRICAL_HAZARD -> "Electrical"
        HazardType.MECHANICAL_HAZARD -> "Mechanical"
        HazardType.CHEMICAL_HAZARD -> "Chemical"
        HazardType.FIRE_HAZARD -> "Fire Risk"
        HazardType.STRUCK_BY_OBJECT -> "Struck By"
        HazardType.CAUGHT_IN_EQUIPMENT -> "Caught In"
        HazardType.ERGONOMIC_HAZARD -> "Ergonomic"
        HazardType.ENVIRONMENTAL_HAZARD -> "Environmental"
        HazardType.HOUSEKEEPING -> "Housekeeping"
        HazardType.LOCKOUT_TAGOUT -> "LOTO"
        HazardType.CONFINED_SPACE -> "Confined Space"
        HazardType.SCAFFOLDING_UNSAFE -> "Scaffolding"
        HazardType.EQUIPMENT_DEFECT -> "Equipment"
    }
}

/**
 * Compact OSHA badge for dense AR overlay scenarios.
 */
@Composable
fun CompactOSHABadge(
    oshaCode: String,
    severity: Severity,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = HazardColors.OSHA_BLUE,
                shape = RoundedCornerShape(4.dp)
            )
            .border(
                width = 1.dp,
                color = HazardColors.getSeverityColor(severity),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = oshaCode,
            style = ConstructionTypography.oshaCode.copy(fontSize = 11.sp),
            color = Color.White
        )
    }
}

/**
 * Animated OSHA badge for critical hazards that need attention.
 */
@Composable
fun CriticalOSHABadge(
    hazard: Hazard,
    modifier: Modifier = Modifier
) {
    // TODO: Add animation for critical hazards
    OSHABadgeComponent(
        hazard = hazard,
        modifier = modifier,
        showConfidence = true,
        isCompact = false
    )
}