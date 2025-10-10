package com.hazardhawk.models

import kotlinx.serialization.Serializable
import com.hazardhawk.core.models.WorkType
import com.hazardhawk.core.models.Severity

@Serializable
enum class TagCategory {
    PPE,
    FALL_PROTECTION,
    ELECTRICAL_SAFETY,
    CHEMICAL_SAFETY,
    FIRE_SAFETY,
    EQUIPMENT_SAFETY,
    HOUSEKEEPING,
    HOT_WORK,
    CRANE_LIFTING,
    CONFINED_SPACE,
    ERGONOMICS,
    ENVIRONMENTAL,
    GENERAL_SAFETY,
    EMERGENCY_PROCEDURES,
    TRAINING_COMMUNICATION
}

@Serializable
enum class RecommendationReason {
    AI_DETECTED,
    OSHA_REQUIREMENT,
    BEST_PRACTICE,
    USER_HISTORY,
    SIMILAR_PHOTOS,
    PROJECT_REQUIREMENTS,
    SEASONAL_REMINDER
}

// Severity enum is defined in SafetyAnalysis.kt

// WorkType is already defined in WorkType.kt - removing duplicate

@Serializable
data class Tag(
    val id: String,
    val name: String,
    val category: TagCategory,
    val oshaCode: String? = null,
    val description: String? = null,
    val isRequired: Boolean = false,
    val workTypes: List<WorkType> = emptyList(),
    val severity: Severity = Severity.LOW,
    val usageCount: Int = 0,
    val lastUsed: Long? = null,
    val isCustom: Boolean = false
)

@Serializable
data class TagRecommendation(
    val tag: Tag,
    val confidence: Float,
    val reason: RecommendationReason,
    val explanation: String? = null,
    val isSelected: Boolean = false
)

@Serializable
data class TagSelectionResult(
    val selectedTags: List<Tag>,
    val timestamp: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
    val photoId: String? = null,
    val workType: WorkType? = null
)

// Extension functions for tag management
fun TagCategory.getDisplayName(): String {
    return when (this) {
        TagCategory.PPE -> "Personal Protective Equipment"
        TagCategory.FALL_PROTECTION -> "Fall Protection"
        TagCategory.ELECTRICAL_SAFETY -> "Electrical Safety"
        TagCategory.CHEMICAL_SAFETY -> "Chemical Safety"
        TagCategory.FIRE_SAFETY -> "Fire Safety"
        TagCategory.EQUIPMENT_SAFETY -> "Equipment Safety"
        TagCategory.HOUSEKEEPING -> "Housekeeping"
        TagCategory.HOT_WORK -> "Hot Work"
        TagCategory.CRANE_LIFTING -> "Crane & Lifting"
        TagCategory.CONFINED_SPACE -> "Confined Space"
        TagCategory.ERGONOMICS -> "Ergonomics"
        TagCategory.ENVIRONMENTAL -> "Environmental"
        TagCategory.GENERAL_SAFETY -> "General Safety"
        TagCategory.EMERGENCY_PROCEDURES -> "Emergency Procedures"
        TagCategory.TRAINING_COMMUNICATION -> "Training & Communication"
    }
}

fun WorkType.getDisplayName(): String {
    return name.replace("_", " ").lowercase()
        .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
}

// Common tag operations
fun List<Tag>.filterByCategory(category: TagCategory): List<Tag> {
    return filter { it.category == category }
}

fun List<Tag>.filterByWorkType(workType: WorkType): List<Tag> {
    return filter { it.workTypes.isEmpty() || it.workTypes.contains(workType) }
}

fun List<Tag>.sortByUsage(): List<Tag> {
    return sortedWith(
        compareByDescending<Tag> { it.usageCount }
            .thenByDescending { it.lastUsed ?: 0L }
    )
}

fun List<TagRecommendation>.sortByConfidence(): List<TagRecommendation> {
    return sortedByDescending { it.confidence }
}
