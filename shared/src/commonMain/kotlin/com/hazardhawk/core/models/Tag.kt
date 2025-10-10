package com.hazardhawk.core.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

/**
 * Core tag model - single source of truth for all tag implementations
 * Consolidates domain entities, UI models, and database representations
 * 
 * Migration Notes:
 * - Replaces com.hazardhawk.domain.entities.Tag
 * - Supports both legacy and new database schemas
 * - Maintains full OSHA compliance features
 * - Includes usage analytics for AI recommendations
 */
@Serializable
data class Tag(
    val id: String,
    val name: String,
    val category: TagCategory,
    val description: String? = null,
    
    // OSHA compliance
    val oshaReferences: List<String> = emptyList(),
    val complianceStatus: ComplianceStatus = ComplianceStatus.COMPLIANT,
    val severity: Severity = Severity.LOW,
    
    // Usage analytics
    val usageStats: TagUsageStats = TagUsageStats(),
    
    // Organization
    val projectId: String? = null,
    val workTypes: List<WorkType> = emptyList(),
    
    // Metadata
    val isCustom: Boolean = false,
    val isActive: Boolean = true,
    val isRequired: Boolean = false,
    val priority: Int = 100,
    val color: String? = null,
    
    // Audit fields
    val createdBy: String? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
) {
    
    /**
     * Backward compatibility - single OSHA code
     */
    val oshaCode: String? get() = oshaReferences.firstOrNull()
    
    /**
     * Legacy compatibility for domain entities
     */
    @Deprecated("Use workTypes instead", ReplaceWith("workTypes"))
    val workType: WorkType? get() = workTypes.firstOrNull()
    
    /**
     * Database schema compatibility
     */
    val projectSpecific: Boolean get() = projectId != null
    
    /**
     * Usage-based properties
     */
    val isFrequentlyUsed: Boolean
        get() = usageStats.totalUsageCount >= 5 || usageStats.recentUsageCount >= 2
    
    val hasComplianceImplications: Boolean
        get() = oshaReferences.isNotEmpty() || complianceStatus != ComplianceStatus.COMPLIANT
    
    val displayPriorityScore: Double
        get() {
            val basePriority = 1000.0 / priority
            val usageBoost = usageStats.totalUsageCount * 0.1
            val recentBoost = usageStats.recentUsageCount * 0.5
            return basePriority + usageBoost + recentBoost
        }
    
    /**
     * Creates a copy with updated usage statistics
     */
    fun withUpdatedUsage(
        incrementUsage: Boolean = true,
        lastUsedAt: Instant = Clock.System.now(),
        projectId: String? = null,
        confidenceScore: Double? = null
    ): Tag {
        return copy(
            usageStats = usageStats.withNewUsage(
                projectId = projectId,
                confidenceScore = confidenceScore,
                usedAt = lastUsedAt
            ).let { stats ->
                if (incrementUsage) {
                    stats.copy(
                        totalUsageCount = stats.totalUsageCount + 1,
                        recentUsageCount = stats.recentUsageCount + 1
                    )
                } else stats
            },
            updatedAt = Clock.System.now()
        )
    }
    
    /**
     * Validation
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) errors.add("Tag name cannot be blank")
        if (name.length > 100) errors.add("Tag name cannot exceed 100 characters")
        if (description?.length ?: 0 > 500) errors.add("Tag description cannot exceed 500 characters")
        if (priority !in 1..1000) errors.add("Tag priority must be between 1 and 1000")
        
        // Validate OSHA references
        oshaReferences.forEach { ref ->
            if (!ref.matches(Regex("^\\d+\\s+(CFR|USC)\\s+\\d+(\\.\\d+)*.*"))) {
                errors.add("Invalid OSHA reference format: $ref")
            }
        }
        
        return errors
    }
    
    companion object {
        /**
         * Create a custom tag
         */
        fun createCustomTag(
            name: String,
            category: TagCategory,
            description: String? = null,
            projectId: String? = null,
            createdBy: String
        ): Tag {
            return Tag(
                id = generateTagId(),
                name = name,
                category = category,
                description = description,
                projectId = projectId,
                isCustom = true,
                createdBy = createdBy
            )
        }
        
        private fun generateTagId(): String {
            return "tag-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
        }
    }
}

/**
 * Unified tag usage statistics
 */
@Serializable
data class TagUsageStats(
    val totalUsageCount: Int = 0,
    val recentUsageCount: Int = 0,
    val lastUsedAt: Instant? = null,
    val averageConfidenceScore: Double? = null,
    val projectUsageMap: Map<String, Int> = emptyMap(),
    val hourlyUsagePattern: List<Int> = List(24) { 0 }
) {
    val frequencyScore: Double
        get() {
            val baseScore = totalUsageCount.toDouble()
            val recencyMultiplier = if (recentUsageCount > 0) 2.0 else 1.0
            val timeDecay = lastUsedAt?.let { 
                val daysSinceLastUse = (Clock.System.now().epochSeconds - it.epochSeconds) / 86400.0
                maxOf(0.1, 1.0 - (daysSinceLastUse / 30.0))
            } ?: 0.1
            return baseScore * recencyMultiplier * timeDecay
        }
    
    val peakUsageHour: Int
        get() = hourlyUsagePattern.withIndex().maxByOrNull { it.value }?.index ?: 9
    
    fun withNewUsage(
        projectId: String? = null,
        confidenceScore: Double? = null,
        usedAt: Instant = Clock.System.now()
    ): TagUsageStats {
        val hour = ((usedAt.epochSeconds % 86400) / 3600).toInt()
        val updatedHourlyPattern = hourlyUsagePattern.toMutableList()
        if (hour in 0..23) updatedHourlyPattern[hour]++
        
        val updatedProjectMap = if (projectId != null) {
            projectUsageMap + (projectId to (projectUsageMap[projectId] ?: 0) + 1)
        } else projectUsageMap
        
        val updatedConfidenceScore = if (confidenceScore != null) {
            val count = totalUsageCount + 1
            val currentAvg = averageConfidenceScore ?: confidenceScore
            (currentAvg * totalUsageCount + confidenceScore) / count
        } else averageConfidenceScore
        
        return copy(
            lastUsedAt = usedAt,
            averageConfidenceScore = updatedConfidenceScore,
            projectUsageMap = updatedProjectMap,
            hourlyUsagePattern = updatedHourlyPattern
        )
    }
}

/**
 * Unified tag categories - consolidates all previous enum definitions
 */
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
    TRAINING_COMMUNICATION,
    TRAFFIC_SAFETY,
    EXCAVATION,
    HAZMAT,
    TRADE_SPECIFIC,
    CUSTOM;
    
    val displayName: String
        get() = when (this) {
            PPE -> "Personal Protective Equipment"
            FALL_PROTECTION -> "Fall Protection"
            ELECTRICAL_SAFETY -> "Electrical Safety"
            CHEMICAL_SAFETY -> "Chemical Safety"
            FIRE_SAFETY -> "Fire Safety"
            EQUIPMENT_SAFETY -> "Equipment Safety"
            HOUSEKEEPING -> "Housekeeping"
            HOT_WORK -> "Hot Work"
            CRANE_LIFTING -> "Crane & Lifting"
            CONFINED_SPACE -> "Confined Space"
            ERGONOMICS -> "Ergonomics"
            ENVIRONMENTAL -> "Environmental"
            GENERAL_SAFETY -> "General Safety"
            EMERGENCY_PROCEDURES -> "Emergency Procedures"
            TRAINING_COMMUNICATION -> "Training & Communication"
            TRAFFIC_SAFETY -> "Traffic Safety"
            EXCAVATION -> "Excavation & Trenching"
            HAZMAT -> "Hazardous Materials"
            TRADE_SPECIFIC -> "Trade Specific"
            CUSTOM -> "Custom"
        }
    
    val primaryOshaSection: String?
        get() = when (this) {
            PPE -> "29 CFR 1926.95-106"
            FALL_PROTECTION -> "29 CFR 1926.500-503"
            ELECTRICAL_SAFETY -> "29 CFR 1926.400-449"
            HOUSEKEEPING -> "29 CFR 1926.25"
            EQUIPMENT_SAFETY -> "29 CFR 1926.300-307"
            HOT_WORK -> "29 CFR 1926.350-354"
            CRANE_LIFTING -> "29 CFR 1926.1400-1442"
            ENVIRONMENTAL -> "29 CFR 1926.50-106"
            TRAFFIC_SAFETY -> "29 CFR 1926.200-203"
            EXCAVATION -> "29 CFR 1926.650-652"
            HAZMAT -> "29 CFR 1926.55-62"
            GENERAL_SAFETY -> "29 CFR 1926.1-99"
            else -> null
        }
    
    val displayPriority: Int
        get() = when (this) {
            PPE -> 1
            FALL_PROTECTION -> 2
            ELECTRICAL_SAFETY -> 3
            EQUIPMENT_SAFETY -> 4
            HOT_WORK -> 5
            CRANE_LIFTING -> 6
            EXCAVATION -> 7
            TRAFFIC_SAFETY -> 8
            ENVIRONMENTAL -> 9
            HAZMAT -> 10
            HOUSEKEEPING -> 11
            GENERAL_SAFETY -> 12
            TRADE_SPECIFIC -> 13
            else -> 99
        }
}

/**
 * Compliance status
 */
@Serializable
enum class ComplianceStatus {
    COMPLIANT,
    NON_COMPLIANT,
    MINOR_VIOLATIONS,
    SERIOUS_VIOLATIONS,
    REQUIRES_REVIEW,
    NEEDS_IMPROVEMENT,
    CRITICAL;
    
    val displayName: String
        get() = when (this) {
            COMPLIANT -> "Compliant"
            NON_COMPLIANT -> "Non-Compliant"
            MINOR_VIOLATIONS -> "Minor Violations"
            SERIOUS_VIOLATIONS -> "Serious Violations"
            REQUIRES_REVIEW -> "Requires Review"
            NEEDS_IMPROVEMENT -> "Needs Improvement"
            CRITICAL -> "Critical"
        }
    
    val statusColor: String
        get() = when (this) {
            COMPLIANT -> "#4CAF50"
            NON_COMPLIANT -> "#F44336"
            MINOR_VIOLATIONS -> "#FF9800"
            SERIOUS_VIOLATIONS -> "#F44336"
            REQUIRES_REVIEW -> "#FFC107"
            NEEDS_IMPROVEMENT -> "#FF9800"
            CRITICAL -> "#F44336"
        }
}

/**
 * Tag recommendation data
 */
@Serializable
data class TagRecommendation(
    val tag: Tag,
    val confidence: Float,
    val reason: RecommendationReason,
    val explanation: String? = null,
    val isSelected: Boolean = false
)

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

/**
 * Tag selection result
 */
@Serializable
data class TagSelectionResult(
    val selectedTags: List<Tag>,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val photoId: String? = null,
    val workType: WorkType? = null
)

/**
 * Missing enums for full compatibility
 */
@Serializable
enum class WorkType {
    GENERAL_CONSTRUCTION,
    ELECTRICAL,
    PLUMBING,
    ROOFING,
    SCAFFOLDING,
    EXCAVATION,
    CONCRETE,
    WELDING,
    PAINTING,
    DEMOLITION,
    FALL_PROTECTION,
    CRANE_OPERATIONS,
    ELECTRICAL_WORK,
    STEEL_ERECTION,
    MAINTENANCE,
    LANDSCAPING
}


/**
 * Migration utilities for Tag model consolidation
 */
object TagModelMigration {
    
    /**
     * Migrate from domain entities Tag
     */
    @Deprecated("Use unified model directly")
    fun fromDomainEntity(
        id: String,
        name: String,
        category: String,
        description: String? = null,
        oshaReferences: List<String> = emptyList(),
        complianceStatus: String = "COMPLIANT",
        usageStats: Any? = null,
        projectId: String? = null,
        isCustom: Boolean = false,
        isActive: Boolean = true,
        priority: Int = 100,
        color: String? = null,
        createdBy: String? = null,
        createdAt: kotlinx.datetime.Instant = Clock.System.now(),
        updatedAt: kotlinx.datetime.Instant = Clock.System.now()
    ): Tag {
        return Tag(
            id = id,
            name = name,
            category = TagCategory.valueOf(category.uppercase().replace(" ", "_")),
            description = description,
            oshaReferences = oshaReferences,
            complianceStatus = ComplianceStatus.valueOf(complianceStatus.uppercase()),
            usageStats = TagUsageStats(), // Default stats
            projectId = projectId,
            isCustom = isCustom,
            isActive = isActive,
            priority = priority,
            color = color,
            createdBy = createdBy,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    /**
     * Create tag from simple string-based data
     */
    fun fromSimpleData(
        name: String,
        categoryName: String,
        oshaCode: String? = null
    ): Tag {
        return Tag(
            id = generateTagId(),
            name = name,
            category = mapCategoryName(categoryName),
            oshaReferences = oshaCode?.let { listOf(it) } ?: emptyList()
        )
    }
    
    private fun mapCategoryName(categoryName: String): TagCategory {
        return when (categoryName.uppercase()) {
            "PPE", "PERSONAL_PROTECTIVE_EQUIPMENT" -> TagCategory.PPE
            "FALL_PROTECTION", "FALL" -> TagCategory.FALL_PROTECTION
            "ELECTRICAL", "ELECTRICAL_SAFETY" -> TagCategory.ELECTRICAL_SAFETY
            "CHEMICAL", "CHEMICAL_SAFETY" -> TagCategory.CHEMICAL_SAFETY
            "FIRE", "FIRE_SAFETY" -> TagCategory.FIRE_SAFETY
            "EQUIPMENT", "EQUIPMENT_SAFETY" -> TagCategory.EQUIPMENT_SAFETY
            "HOUSEKEEPING" -> TagCategory.HOUSEKEEPING
            "HOT_WORK" -> TagCategory.HOT_WORK
            "CRANE", "CRANE_LIFTING", "LIFTING" -> TagCategory.CRANE_LIFTING
            "CONFINED_SPACE" -> TagCategory.CONFINED_SPACE
            "ERGONOMICS" -> TagCategory.ERGONOMICS
            "ENVIRONMENTAL" -> TagCategory.ENVIRONMENTAL
            "GENERAL", "GENERAL_SAFETY" -> TagCategory.GENERAL_SAFETY
            "EMERGENCY", "EMERGENCY_PROCEDURES" -> TagCategory.EMERGENCY_PROCEDURES
            "TRAINING", "TRAINING_COMMUNICATION" -> TagCategory.TRAINING_COMMUNICATION
            "TRAFFIC", "TRAFFIC_SAFETY" -> TagCategory.TRAFFIC_SAFETY
            "EXCAVATION" -> TagCategory.EXCAVATION
            "HAZMAT", "HAZARDOUS_MATERIALS" -> TagCategory.HAZMAT
            "TRADE", "TRADE_SPECIFIC" -> TagCategory.TRADE_SPECIFIC
            else -> TagCategory.CUSTOM
        }
    }
    
    private fun generateTagId(): String {
        return "tag-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
    }
}
