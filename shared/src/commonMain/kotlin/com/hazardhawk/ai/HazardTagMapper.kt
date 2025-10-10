package com.hazardhawk.ai

import kotlinx.serialization.Serializable
import com.hazardhawk.core.models.WorkType

/**
 * Simplified hazard-to-tag mapper for build infrastructure
 * Minimal implementation to satisfy compilation requirements
 */
class HazardTagMapper {
    
    fun mapToTags(detections: List<String>, workType: WorkType): TagMappingResult {
        return TagMappingResult(
            recommendations = detections.map { UITagRecommendation.basic(it) },
            autoSelections = emptySet()
        )
    }
}

@Serializable
data class TagMappingResult(
    val recommendations: List<UITagRecommendation>,
    val autoSelections: Set<String>
)
