package com.hazardhawk.models

import kotlinx.serialization.Serializable

/**
 * OSHA regulation data models for ecfr.gov API integration
 * Based on Title 29 CFR structure from https://www.ecfr.gov/api/versioner/v1/structure/
 */

/**
 * Root level OSHA regulation structure
 */
@Serializable
data class OSHARegulationData(
    val identifier: String, // e.g., "title-29"
    val label: String, // e.g., "Title 29—Labor"
    val description: String = "",
    val lastUpdated: String, // ISO date format
    val children: List<OSHARegulationNode> = emptyList(),
    val apiVersion: String = "v1",
    val sourceUrl: String = ""
)

/**
 * Individual regulation node in the hierarchy
 * Can represent subtitle, part, subpart, or section
 */
@Serializable
data class OSHARegulationNode(
    val identifier: String, // e.g., "1926.95", "subtitle-B"
    val label: String, // e.g., "Personal protective equipment"
    val description: String = "",
    val type: OSHARegulationType,
    val level: Int, // Hierarchy depth level
    val size: Long = 0, // Content size in bytes
    val reserved: Boolean = false, // Placeholder section
    val volumes: List<String> = emptyList(), // Associated volumes
    val parentId: String? = null,
    val children: List<OSHARegulationNode> = emptyList(),
    val content: OSHARegulationContent? = null
)

/**
 * Full content of an OSHA regulation section
 */
@Serializable
data class OSHARegulationContent(
    val sectionId: String,
    val fullText: String,
    val requirements: List<OSHARequirement> = emptyList(),
    val penalties: List<OSHAPenalty> = emptyList(),
    val lastModified: String,
    val authority: String = "", // Legal authority
    val source: String = "" // Federal Register source
)

/**
 * Individual OSHA requirement within a regulation
 */
@Serializable
data class OSHARequirement(
    val id: String,
    val subsection: String, // e.g., "(a)(1)", "(b)(2)(i)"
    val requirementText: String,
    val applicableIndustries: List<String> = emptyList(),
    val keywords: List<String> = emptyList(), // For search/matching
    val severity: OSHASeverity = OSHASeverity.OTHER_THAN_SERIOUS,
    val complianceDeadline: String? = null
)

/**
 * OSHA penalty information for violations
 */
@Serializable
data class OSHAPenalty(
    val violationType: OSHAViolationType,
    val minimumPenalty: Double,
    val maximumPenalty: Double,
    val currency: String = "USD",
    val effectiveDate: String,
    val description: String = ""
)

/**
 * Types of OSHA regulation levels
 */
@Serializable
enum class OSHARegulationType {
    TITLE,      // Title 29
    SUBTITLE,   // Subtitle B
    CHAPTER,    // Chapter XVII
    PART,       // Part 1926
    SUBPART,    // Subpart E
    SECTION,    // Section 1926.95
    SUBSECTION, // (a), (b), etc.
    PARAGRAPH   // (1), (2), etc.
}

/**
 * Local database storage model for OSHA regulations
 */
@Serializable
data class OSHARegulationEntity(
    val id: String,
    val identifier: String,
    val label: String,
    val type: OSHARegulationType,
    val level: Int,
    val parentId: String? = null,
    val fullText: String = "",
    val keywords: String = "", // Comma-separated for search
    val lastUpdated: Long, // Timestamp
    val isActive: Boolean = true,
    val size: Long = 0
)

/**
 * OSHA regulation update tracking
 */
@Serializable
data class OSHAUpdateStatus(
    val lastCheckDate: Long,
    val lastUpdateDate: Long,
    val apiVersion: String,
    val totalRegulations: Int,
    val updateInProgress: Boolean = false,
    val lastError: String? = null,
    val nextScheduledUpdate: Long
)

/**
 * Search/lookup result for OSHA regulations
 */
@Serializable
data class OSHARegulationLookup(
    val query: String,
    val matches: List<OSHARegulationMatch>,
    val totalResults: Int,
    val searchType: OSHASearchType
)

/**
 * Individual search match result
 */
@Serializable
data class OSHARegulationMatch(
    val regulation: OSHARegulationEntity,
    val relevanceScore: Float, // 0.0 to 1.0
    val matchedKeywords: List<String>,
    val contextSnippet: String = "", // Relevant text excerpt
    val exactMatch: Boolean = false
)

/**
 * Types of OSHA regulation searches
 */
@Serializable
enum class OSHASearchType {
    KEYWORD,        // General keyword search
    REGULATION_ID,  // Specific regulation lookup (e.g., "1926.95")
    HAZARD_TYPE,    // Search by hazard category
    INDUSTRY_CODE,  // Search by industry application
    PENALTY_RANGE   // Search by penalty severity
}

/**
 * OSHA regulation API response wrapper
 */
@Serializable
data class OSHAApiResponse<T>(
    val data: T,
    val success: Boolean,
    val timestamp: String,
    val apiVersion: String,
    val nextUpdate: String? = null,
    val errors: List<String> = emptyList()
)

/**
 * Monthly sync configuration
 */
@Serializable
data class OSHASyncConfig(
    val enabled: Boolean = true,
    val syncInterval: Long = 30 * 24 * 60 * 60 * 1000L, // 30 days in ms
    val autoRetry: Boolean = true,
    val maxRetries: Int = 3,
    val wifiOnly: Boolean = true,
    val backgroundSync: Boolean = true,
    val lastSyncAttempt: Long = 0,
    val nextSyncDue: Long = 0
)
/**
 * OSHA violation severity levels
 */
@Serializable
enum class OSHASeverity {
    IMMINENT_DANGER,    // Immediate threat to life or health
    SERIOUS,            // Could cause death or serious harm
    OTHER_THAN_SERIOUS, // Less severe violations
    WILLFUL,            // Intentional disregard for safety
    REPEATED,           // Previously cited violations
    DE_MINIMIS          // No direct safety impact
}

/**
 * Types of OSHA violations
 */
@Serializable
enum class OSHAViolationType {
    SERIOUS,            // Serious violation
    WILLFUL,            // Willful violation
    REPEATED,           // Repeated violation
    OTHER,              // Other than serious
    DE_MINIMIS,         // De minimis violation
    FAILURE_TO_ABATE    // Failure to correct previous violation
}
