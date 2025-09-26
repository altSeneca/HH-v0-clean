package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazardhawk.ai.PhotoAnalysisWithTags
import com.hazardhawk.ai.yolo.ConstructionHazardDetection
import com.hazardhawk.ai.yolo.ConstructionHazardType
import com.hazardhawk.ai.yolo.YOLOBoundingBox
import com.hazardhawk.domain.entities.SafetyAnalysis
import com.hazardhawk.models.Severity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Construction-safe colors
private val SafetyOrange = Color(0xFFFF6B35)
private val DangerRed = Color(0xFFEF4444)
private val ConstructionBlack = Color(0xFF1A1A1A)

/**
 * Error Fallback Component
 * Shown when photo is not found or invalid
 */
@Composable
fun ErrorFallback(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = DangerRed
            )
            Text(
                text = "Photo not found",
                style = MaterialTheme.typography.headlineSmall,
                color = ConstructionBlack
            )
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange)
            ) {
                Text("Go Back")
            }
        }
    }
}


/**
 * Convert PhotoAnalysisWithTags to SafetyAnalysis for database storage
 */
fun convertPhotoAnalysisToSafetyAnalysis(
    photoAnalysis: PhotoAnalysisWithTags,
    photoId: String
): SafetyAnalysis {
    return SafetyAnalysis(
        id = photoAnalysis.id,
        photoId = photoId,
        severity = determineSeverityFromHazards(photoAnalysis.hazardDetections),
        aiConfidence = photoAnalysis.hazardDetections.maxOfOrNull { it.boundingBox.confidence } ?: 0.0f,
        analyzedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        analysisSource = "gemini_vision",
        hazards = photoAnalysis.hazardDetections.map { it.hazardType.name },
        oshaCodes = photoAnalysis.hazardDetections.mapNotNull { it.oshaReference },
        recommendations = photoAnalysis.recommendedTags
    )
}

/**
 * Convert SafetyAnalysis from database back to PhotoAnalysisWithTags
 */
fun convertSafetyAnalysisToPhotoAnalysis(
    safetyAnalysis: SafetyAnalysis
): PhotoAnalysisWithTags {
    return PhotoAnalysisWithTags(
        id = safetyAnalysis.id,
        photoId = safetyAnalysis.photoId,
        processingTimeMs = 0L, // Not stored in SafetyAnalysis
        hazardDetections = safetyAnalysis.hazards.mapIndexed { index, hazard ->
            ConstructionHazardDetection(
                hazardType = try {
                    ConstructionHazardType.valueOf(hazard.uppercase().replace(" ", "_"))
                } catch (e: Exception) {
                    ConstructionHazardType.UNKNOWN_HAZARD
                },
                boundingBox = YOLOBoundingBox(
                    x = 0.5f, y = 0.5f, width = 0.1f, height = 0.1f,
                    confidence = safetyAnalysis.aiConfidence,
                    classId = index,
                    className = hazard
                ),
                severity = try {
                    Severity.valueOf(safetyAnalysis.severity.uppercase())
                } catch (e: Exception) {
                    Severity.LOW
                },
                oshaReference = safetyAnalysis.oshaCodes.getOrNull(index),
                description = "Detected $hazard"
            )
        },
        recommendedTags = safetyAnalysis.recommendations
    )
}

/**
 * Determine overall severity from hazard detections
 */
fun determineSeverityFromHazards(hazards: List<ConstructionHazardDetection>): String {
    return when {
        hazards.any { it.severity == Severity.CRITICAL } -> "critical"
        hazards.any { it.severity == Severity.HIGH } -> "high"
        hazards.any { it.severity == Severity.MEDIUM } -> "medium"
        else -> "low"
    }
}