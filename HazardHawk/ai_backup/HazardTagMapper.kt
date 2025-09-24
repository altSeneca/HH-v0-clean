package com.hazardhawk.ai

import kotlinx.serialization.Serializable

// Import required types
enum class HazardSeverity(val level: Int) {
    LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4)
}

/**
 * Simplified hazard-to-tag mapper with direct mapping strategy
 * Eliminates complex contextual rules and seasonal considerations for maintainability
 */
class HazardTagMapper {
    
    // Direct mapping from hazard types to tag IDs
    private val hazardToTagMap = mapOf(
        HazardType.PERSON_NO_HARD_HAT to listOf("ppe-hard-hat-required", "general-ppe-violation"),
        HazardType.PERSON_NO_SAFETY_VEST to listOf("ppe-high-visibility-vest", "general-ppe-violation"),
        HazardType.FALL_HAZARD to listOf("fall-protection", "general-safety-hazard"),
        HazardType.ELECTRICAL_HAZARD to listOf("electrical-safety", "lockout-tagout"),
        HazardType.UNPROTECTED_EDGE to listOf("fall-protection", "guardrail-system"),
        HazardType.UNSAFE_LADDER to listOf("ladder-safety", "equipment-safety"),
        HazardType.HEAVY_MACHINERY to listOf("equipment-safety", "proximity-awareness"),
        HazardType.EXCAVATOR to listOf("excavation-safety", "equipment-safety"),
        HazardType.CRANE to listOf("crane-safety", "lifting-operations"),
        HazardType.TRUCK to listOf("vehicle-safety", "traffic-control"),
        HazardType.UNSAFE_SCAFFOLDING to listOf("scaffolding-safety", "fall-protection"),
        HazardType.SAFETY_BARRIER_MISSING to listOf("perimeter-protection", "general-safety-hazard")
    )
    
    // OSHA code mapping for compliance
    private val oshaCodeMap = mapOf(
        HazardType.PERSON_NO_HARD_HAT to "1926.95",
        HazardType.PERSON_NO_SAFETY_VEST to "1926.95",
        HazardType.FALL_HAZARD to "1926.501",
        HazardType.ELECTRICAL_HAZARD to "1926.416",
        HazardType.UNPROTECTED_EDGE to "1926.501",
        HazardType.CRANE to "1926.1400",
        HazardType.EXCAVATOR to "1926.651",
        HazardType.UNSAFE_SCAFFOLDING to "1926.451"
    )
    
    fun mapToTags(detections: List<HazardDetection>, workType: WorkType): TagMappingResult {
        val recommendations = mutableListOf<UITagRecommendation>()
        val autoSelections = mutableSetOf<String>()
        var criticalIssues = 0
        val oshaViolations = mutableListOf<String>()
        
        detections.forEach { detection ->
            val tagIds = hazardToTagMap[detection.hazardType] ?: emptyList()
            val oshaCode = oshaCodeMap[detection.hazardType]
            
            tagIds.forEach { tagId ->
                recommendations.add(
                    UITagRecommendation(
                        tagId = tagId,
                        displayName = formatDisplayName(tagId),
                        confidence = detection.confidence,
                        reason = "Detected ${detection.hazardType.displayName}",
                        priority = mapSeverityToPriority(detection.severity),
                        oshaReference = oshaCode
                    )
                )
                
                // Auto-select if confidence > threshold
                if (detection.confidence > 0.8f) {
                    autoSelections.add(tagId)
                }
            }
            
            // Track compliance issues
            if (detection.severity == HazardSeverity.CRITICAL) {
                criticalIssues++
            }
            
            oshaCode?.let { oshaViolations.add(it) }
        }
        
        // Add work-type specific tags
        addWorkTypeSpecificTags(workType, recommendations, autoSelections)
        
        val complianceLevel = when {
            criticalIssues > 0 -> ComplianceLevel.NON_COMPLIANT
            oshaViolations.isNotEmpty() -> ComplianceLevel.PARTIAL_COMPLIANCE
            recommendations.isEmpty() -> ComplianceLevel.COMPLIANT
            else -> ComplianceLevel.INFORMATIONAL
        }
        
        return TagMappingResult(
            recommendations = recommendations,
            autoSelections = autoSelections,
            complianceOverview = ComplianceOverview(
                overallLevel = complianceLevel,
                criticalIssues = criticalIssues,
                oshaViolations = oshaViolations.distinct()
            )
        )
    }
    
    private fun formatDisplayName(tagId: String): String {
        return tagId.replace("-", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
    
    private fun mapSeverityToPriority(severity: HazardSeverity): TagPriority {
        return when (severity) {
            HazardSeverity.CRITICAL -> TagPriority.CRITICAL
            HazardSeverity.HIGH -> TagPriority.HIGH
            HazardSeverity.MEDIUM -> TagPriority.MEDIUM
            HazardSeverity.LOW -> TagPriority.LOW
        }
    }
    
    private fun addWorkTypeSpecificTags(
        workType: WorkType,
        recommendations: MutableList<UITagRecommendation>,
        autoSelections: MutableSet<String>
    ) {
        val workSpecificTags = when (workType) {
            WorkType.ELECTRICAL_WORK -> listOf("electrical-qualified-person", "gfci-protection")
            WorkType.ROOFING -> listOf("roof-safety-plan", "weather-assessment")
            WorkType.EXCAVATION -> listOf("competent-person", "atmospheric-testing")
            WorkType.WELDING -> listOf("hot-work-permit", "fire-watch")
            else -> emptyList()
        }
        
        workSpecificTags.forEach { tagId ->
            recommendations.add(
                UITagRecommendation(
                    tagId = tagId,
                    displayName = formatDisplayName(tagId),
                    confidence = 0.7f,
                    reason = "Work type: ${workType.name.lowercase().replace("_", " ")}",
                    priority = TagPriority.MEDIUM
                )
            )
        }
    }
}

/**
 * Result container for tag mapping operation
 */
@Serializable
data class TagMappingResult(
    val recommendations: List<UITagRecommendation>,
    val autoSelections: Set<String>,
    val complianceOverview: ComplianceOverview
)