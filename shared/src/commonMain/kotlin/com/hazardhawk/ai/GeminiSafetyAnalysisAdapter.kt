package com.hazardhawk.ai

import com.hazardhawk.ai.yolo.ConstructionHazardDetection
import com.hazardhawk.ai.yolo.ConstructionHazardType
import kotlinx.datetime.Clock

/**
 * Simplified adapter to demonstrate real AI analysis integration
 * Uses the existing AI models structure from SafetyAnalysis.kt
 */
class GeminiSafetyAnalysisAdapter {
    
    /**
     * Extract recommendations from tags for display
     */
    fun extractRecommendations(tags: List<String>): List<String> {
        return tags.filter { tag ->
            tag.startsWith("recommendation-") || tag.contains("action-") || tag.contains("osha-")
        }.map { tag ->
            // Convert tag to readable recommendation
            tag.removePrefix("recommendation-")
                .removePrefix("action-")
                .replace("-", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }.distinct().ifEmpty {
            listOf("Follow OSHA safety guidelines", "Ensure proper PPE usage", "Maintain safe work practices")
        }
    }
    
    /**
     * Generate hazard summary from detections
     */
    fun generateHazardSummary(hazardDetections: List<ConstructionHazardDetection>): Map<String, Any> {
        val criticalHazards = hazardDetections.count { 
            it.severity == com.hazardhawk.models.Severity.CRITICAL 
        }
        val highHazards = hazardDetections.count { 
            it.severity == com.hazardhawk.models.Severity.HIGH 
        }
        
        return mapOf(
            "total_hazards" to hazardDetections.size,
            "critical_hazards" to criticalHazards,
            "high_hazards" to highHazards,
            "has_critical" to (criticalHazards > 0),
            "overall_confidence" to if (hazardDetections.isNotEmpty()) {
                hazardDetections.map { it.boundingBox.confidence }.average().toFloat()
            } else 0.8f
        )
    }
    
    /**
     * Map construction hazard to display description
     */
    fun getHazardDescription(hazardType: ConstructionHazardType): String {
        return when (hazardType) {
            ConstructionHazardType.MISSING_HARD_HAT -> "Worker without required hard hat"
            ConstructionHazardType.MISSING_SAFETY_VEST -> "Worker without high-visibility safety vest"
            ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION -> "Fall protection required at elevated work"
            ConstructionHazardType.UNGUARDED_EDGE -> "Unprotected edge presents fall hazard"
            ConstructionHazardType.ELECTRICAL_HAZARD -> "Electrical safety hazard detected"
            ConstructionHazardType.TRIP_HAZARDS -> "Housekeeping issue creating trip hazard"
            ConstructionHazardType.FIRE_HAZARD -> "Fire safety hazard identified"
            else -> hazardType.name.replace("_", " ").lowercase()
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }
    
    /**
     * Get OSHA reference for hazard type
     */
    fun getOSHAReference(hazardType: ConstructionHazardType): String? {
        return when (hazardType) {
            ConstructionHazardType.MISSING_HARD_HAT -> "29 CFR 1926.95"
            ConstructionHazardType.MISSING_SAFETY_VEST -> "29 CFR 1926.95"
            ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION -> "29 CFR 1926.501"
            ConstructionHazardType.UNGUARDED_EDGE -> "29 CFR 1926.501"
            ConstructionHazardType.ELECTRICAL_HAZARD -> "29 CFR 1926.95"
            ConstructionHazardType.FIRE_HAZARD -> "29 CFR 1926.150"
            else -> null
        }
    }
}