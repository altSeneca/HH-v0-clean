package com.hazardhawk.domain.entities

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Represents a safety tag that can be applied to photos for hazard identification and categorization.
 * 
 * This data class provides comprehensive tag management with OSHA compliance tracking,
 * usage analytics, and project-specific organization.
 *
 * @property id Unique identifier for the tag
 * @property name Display name for the tag (e.g., "Hard Hat Required")
 * @property category The category this tag belongs to (PPE, EQUIPMENT, etc.)
 * @property description Optional detailed description of the tag's purpose
 * @property oshaReferences List of OSHA regulation references (e.g., "29 CFR 1926.95")
 * @property complianceStatus Current compliance status of this tag
 * @property usageStats Usage statistics for analytics and recommendations
 * @property projectId Associated project ID if project-specific, null for global tags
 * @property isCustom Whether this is a custom user-created tag
 * @property isActive Whether this tag is currently active/available for use
 * @property priority Priority level for display ordering (1 = highest priority)
 * @property color Optional color identifier for UI theming
 * @property createdBy User ID who created this tag (for custom tags)
 * @property createdAt Timestamp when the tag was created
 * @property updatedAt Timestamp when the tag was last modified
 */
@Serializable
data class Tag(
    val id: String,
    val name: String,
    val category: TagCategory,
    val description: String? = null,
    val oshaReferences: List<String> = emptyList(),
    val complianceStatus: ComplianceStatus = ComplianceStatus.COMPLIANT,
    val usageStats: TagUsageStats = TagUsageStats(),
    val projectId: String? = null,
    val isCustom: Boolean = false,
    val isActive: Boolean = true,
    val priority: Int = 100, // Lower number = higher priority
    val color: String? = null,
    val createdBy: String? = null,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
) {
    /**
     * Creates a copy of this tag with updated usage statistics.
     */
    fun withUpdatedUsage(
        incrementUsage: Boolean = true,
        lastUsedAt: Instant = Clock.System.now()
    ): Tag {
        return copy(
            usageStats = usageStats.copy(
                totalUsageCount = if (incrementUsage) usageStats.totalUsageCount + 1 else usageStats.totalUsageCount,
                lastUsedAt = lastUsedAt,
                recentUsageCount = if (incrementUsage) usageStats.recentUsageCount + 1 else usageStats.recentUsageCount
            ),
            updatedAt = Clock.System.now()
        )
    }
    
    /**
     * Determines if this tag should be prominently displayed based on usage patterns.
     */
    val isFrequentlyUsed: Boolean
        get() = usageStats.totalUsageCount >= 5 || usageStats.recentUsageCount >= 2
    
    /**
     * Determines if this tag has OSHA compliance implications.
     */
    val hasComplianceImplications: Boolean
        get() = oshaReferences.isNotEmpty() || complianceStatus != ComplianceStatus.COMPLIANT
    
    /**
     * Gets the display priority score for sorting (combines priority and usage).
     */
    val displayPriorityScore: Double
        get() {
            val basePriority = 1000.0 / priority
            val usageBoost = usageStats.totalUsageCount * 0.1
            val recentBoost = usageStats.recentUsageCount * 0.5
            return basePriority + usageBoost + recentBoost
        }
    
    /**
     * Validates the tag data and returns validation errors if any.
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) {
            errors.add("Tag name cannot be blank")
        }
        
        if (name.length > 100) {
            errors.add("Tag name cannot exceed 100 characters")
        }
        
        if (description?.length ?: 0 > 500) {
            errors.add("Tag description cannot exceed 500 characters")
        }
        
        if (priority < 1 || priority > 1000) {
            errors.add("Tag priority must be between 1 and 1000")
        }
        
        // Validate OSHA references format
        oshaReferences.forEach { ref ->
            if (!ref.matches(Regex("^\\d+\\s+CFR\\s+\\d+(\\.\\d+)*.*"))) {
                errors.add("Invalid OSHA reference format: $ref")
            }
        }
        
        return errors
    }
    
    companion object {
        /**
         * Creates a new custom tag with default values.
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
        
        /**
         * Generates a unique tag ID.
         */
        private fun generateTagId(): String {
            return "tag-${Clock.System.now().toEpochMilliseconds()}-${(1000..9999).random()}"
        }
    }
}

/**
 * Represents the compliance status of a safety tag with respect to OSHA regulations.
 */
@Serializable
enum class ComplianceStatus {
    /** Tag indicates full compliance with safety regulations */
    COMPLIANT,
    
    /** Tag indicates safety issue that needs improvement but not critical */
    NEEDS_IMPROVEMENT,
    
    /** Tag indicates critical safety violation requiring immediate attention */
    CRITICAL;
    
    /**
     * Gets the display name for the compliance status.
     */
    val displayName: String
        get() = when (this) {
            COMPLIANT -> "Compliant"
            NEEDS_IMPROVEMENT -> "Needs Improvement"
            CRITICAL -> "Critical"
        }
    
    /**
     * Gets the color associated with this compliance status.
     */
    val statusColor: String
        get() = when (this) {
            COMPLIANT -> "#4CAF50" // Green
            NEEDS_IMPROVEMENT -> "#FF9800" // Orange
            CRITICAL -> "#F44336" // Red
        }
    
    /**
     * Determines if this status requires immediate attention.
     */
    val requiresImmediateAttention: Boolean
        get() = this == CRITICAL
}

/**
 * Represents usage statistics for a tag to support recommendation algorithms and analytics.
 *
 * @property totalUsageCount Total number of times this tag has been used
 * @property recentUsageCount Usage count within the last 7 days
 * @property lastUsedAt Timestamp when the tag was last used
 * @property averageConfidenceScore Average AI confidence score when auto-applied
 * @property projectUsageMap Usage count per project ID
 * @property hourlyUsagePattern Usage pattern by hour of day (0-23)
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
    /**
     * Calculates the usage frequency score for recommendation algorithms.
     */
    val frequencyScore: Double
        get() {
            val baseScore = totalUsageCount.toDouble()
            val recencyMultiplier = if (recentUsageCount > 0) 2.0 else 1.0
            val timeDecay = lastUsedAt?.let { 
                val daysSinceLastUse = (Clock.System.now().epochSeconds - it.epochSeconds) / 86400.0
                maxOf(0.1, 1.0 - (daysSinceLastUse / 30.0)) // Decay over 30 days
            } ?: 0.1
            
            return baseScore * recencyMultiplier * timeDecay
        }
    
    /**
     * Gets the most active hour of day for this tag.
     */
    val peakUsageHour: Int
        get() = hourlyUsagePattern.withIndex().maxByOrNull { it.value }?.index ?: 9
    
    /**
     * Updates usage statistics with a new usage event.
     */
    fun withNewUsage(
        projectId: String? = null,
        confidenceScore: Double? = null,
        usedAt: Instant = Clock.System.now()
    ): TagUsageStats {
        val hour = (usedAt.epochSeconds % 86400) / 3600
        val updatedHourlyPattern = hourlyUsagePattern.toMutableList()
        if (hour.toInt() in 0..23) {
            updatedHourlyPattern[hour.toInt()]++
        }
        
        val updatedProjectMap = if (projectId != null) {
            projectUsageMap + (projectId to (projectUsageMap[projectId] ?: 0) + 1)
        } else {
            projectUsageMap
        }
        
        val updatedConfidenceScore = if (confidenceScore != null) {
            val count = totalUsageCount + 1
            val currentAvg = averageConfidenceScore ?: confidenceScore
            (currentAvg * totalUsageCount + confidenceScore) / count
        } else {
            averageConfidenceScore
        }
        
        return copy(
            totalUsageCount = totalUsageCount + 1,
            recentUsageCount = recentUsageCount + 1,
            lastUsedAt = usedAt,
            averageConfidenceScore = updatedConfidenceScore,
            projectUsageMap = updatedProjectMap,
            hourlyUsagePattern = updatedHourlyPattern
        )
    }
}

/**
 * Comprehensive categorization system for safety tags aligned with OSHA standards and construction industry practices.
 * 
 * Each category includes OSHA regulation mapping and specific compliance requirements.
 */
@Serializable
enum class TagCategory {
    /** Personal Protective Equipment - hard hats, safety glasses, gloves, etc. */
    PPE,
    
    /** Fall protection systems - harnesses, guardrails, scaffolding safety */
    FALL_PROTECTION,
    
    /** Electrical safety - lockout/tagout, grounding, GFCI protection */
    ELECTRICAL,
    
    /** Housekeeping and site cleanliness - debris removal, storage, walkways */
    HOUSEKEEPING,
    
    /** Equipment and machinery - inspections, maintenance, guards */
    EQUIPMENT,
    
    /** Hot work operations - welding, cutting, fire prevention */
    HOT_WORK,
    
    /** Crane and lifting operations - rigging, operator certification */
    CRANE_LIFT,
    
    /** Environmental hazards - weather, air quality, noise */
    ENVIRONMENTAL,
    
    /** Traffic and vehicle safety - flaggers, barriers, visibility */
    TRAFFIC_SAFETY,
    
    /** Excavation and trenching safety - cave-ins, protective systems */
    EXCAVATION,
    
    /** Chemical and hazardous materials - storage, handling, MSDS */
    HAZMAT,
    
    /** General safety practices - training, procedures, signage */
    GENERAL_SAFETY,
    
    /** Trade-specific safety concerns - carpentry, plumbing, HVAC */
    TRADE_SPECIFIC,
    
    /** User-defined custom categories */
    CUSTOM;
    
    /**
     * Gets the display name for the category.
     */
    val displayName: String
        get() = when (this) {
            PPE -> "Personal Protective Equipment"
            FALL_PROTECTION -> "Fall Protection"
            ELECTRICAL -> "Electrical Safety"
            HOUSEKEEPING -> "Housekeeping"
            EQUIPMENT -> "Equipment & Machinery"
            HOT_WORK -> "Hot Work"
            CRANE_LIFT -> "Crane & Lifting"
            ENVIRONMENTAL -> "Environmental Hazards"
            TRAFFIC_SAFETY -> "Traffic Safety"
            EXCAVATION -> "Excavation & Trenching"
            HAZMAT -> "Hazardous Materials"
            GENERAL_SAFETY -> "General Safety"
            TRADE_SPECIFIC -> "Trade Specific"
            CUSTOM -> "Custom"
        }
    
    /**
     * Gets the primary OSHA regulation section for this category.
     */
    val primaryOshaSection: String?
        get() = when (this) {
            PPE -> "29 CFR 1926.95-106"
            FALL_PROTECTION -> "29 CFR 1926.500-503"
            ELECTRICAL -> "29 CFR 1926.400-449"
            HOUSEKEEPING -> "29 CFR 1926.25"
            EQUIPMENT -> "29 CFR 1926.300-307"
            HOT_WORK -> "29 CFR 1926.350-354"
            CRANE_LIFT -> "29 CFR 1926.1400-1442"
            ENVIRONMENTAL -> "29 CFR 1926.50-106"
            TRAFFIC_SAFETY -> "29 CFR 1926.200-203"
            EXCAVATION -> "29 CFR 1926.650-652"
            HAZMAT -> "29 CFR 1926.55-62"
            GENERAL_SAFETY -> "29 CFR 1926.1-99"
            TRADE_SPECIFIC -> null
            CUSTOM -> null
        }
    
    /**
     * Gets the icon identifier for UI display.
     */
    val iconIdentifier: String
        get() = when (this) {
            PPE -> "hard_hat"
            FALL_PROTECTION -> "safety_harness"
            ELECTRICAL -> "electrical_hazard"
            HOUSEKEEPING -> "broom"
            EQUIPMENT -> "machinery"
            HOT_WORK -> "flame"
            CRANE_LIFT -> "crane"
            ENVIRONMENTAL -> "weather"
            TRAFFIC_SAFETY -> "traffic_cone"
            EXCAVATION -> "excavator"
            HAZMAT -> "chemical"
            GENERAL_SAFETY -> "safety_shield"
            TRADE_SPECIFIC -> "tools"
            CUSTOM -> "custom_tag"
        }
    
    /**
     * Gets the priority order for display (lower number = higher priority).
     */
    val displayPriority: Int
        get() = when (this) {
            PPE -> 1
            FALL_PROTECTION -> 2
            ELECTRICAL -> 3
            EQUIPMENT -> 4
            HOT_WORK -> 5
            CRANE_LIFT -> 6
            EXCAVATION -> 7
            TRAFFIC_SAFETY -> 8
            ENVIRONMENTAL -> 9
            HAZMAT -> 10
            HOUSEKEEPING -> 11
            GENERAL_SAFETY -> 12
            TRADE_SPECIFIC -> 13
            CUSTOM -> 99
        }
    
    /**
     * Determines if this category typically requires immediate attention.
     */
    val typicallyHighPriority: Boolean
        get() = when (this) {
            PPE, FALL_PROTECTION, ELECTRICAL, EQUIPMENT, HOT_WORK, CRANE_LIFT, EXCAVATION, HAZMAT -> true
            else -> false
        }
    
    companion object {
        /**
         * Gets all categories ordered by display priority.
         */
        val orderedByPriority: List<TagCategory>
            get() = values().sortedBy { it.displayPriority }
        
        /**
         * Gets high-priority categories that require immediate attention.
         */
        val highPriorityCategories: List<TagCategory>
            get() = values().filter { it.typicallyHighPriority }
        
        /**
         * Finds category by display name (case-insensitive).
         */
        fun findByDisplayName(name: String): TagCategory? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}