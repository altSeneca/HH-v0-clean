package com.hazardhawk.models.dashboard
import kotlinx.datetime.Clock

import kotlinx.serialization.Serializable

/**
 * API response models for Dashboard Service
 * These models map directly to backend API responses
 */

/**
 * Safety metrics data for a given period
 * Endpoint: GET /api/dashboard/safety-metrics
 */
@Serializable
data class SafetyMetricsResponse(
    val period: String, // "last_7_days", "last_30_days", "last_90_days", "ytd"
    val incidentCount: Int,
    val incidentRate: Double, // incidents per 100 workers
    val nearMissCount: Int,
    val safetyObservations: Int,
    val complianceScore: Double, // 0-100 percentage
    val activeCertifications: Int,
    val expiringCertifications: Int, // expiring in next 30 days
    val expiredCertifications: Int,
    val totalWorkers: Int,
    val activeProjects: Int,
    val daysWithoutIncident: Int = 0,
    val timestamp: Long // Unix timestamp when data was generated
)

/**
 * Compliance summary data
 * Endpoint: GET /api/dashboard/compliance-summary
 */
@Serializable
data class ComplianceSummaryResponse(
    val totalWorkers: Int,
    val workersWithAllCerts: Int,
    val workersWithExpiringSoon: Int, // expiring in next 30 days
    val workersWithExpired: Int,
    val compliancePercentage: Double, // 0-100 percentage
    val requiredCertifications: List<String>, // e.g., ["OSHA 10", "Fall Protection", "First Aid/CPR"]
    val mostCommonGap: String?, // certification type most commonly missing
    val timestamp: Long // Unix timestamp when data was generated
)

/**
 * Activity feed response with pagination
 * Endpoint: GET /api/dashboard/activity-feed
 */
@Serializable
data class ActivityFeedResponse(
    val activities: List<ActivityItemDto>,
    val pagination: PaginationInfo,
    val timestamp: Long // Unix timestamp when data was generated
)

/**
 * Activity item DTO from API
 */
@Serializable
data class ActivityItemDto(
    val id: String,
    val type: String, // "ptp", "hazard", "toolbox_talk", "photo", "system_alert"
    val timestamp: Long,
    val data: ActivityDataDto
)

/**
 * Activity data DTO (polymorphic based on type)
 */
@Serializable
data class ActivityDataDto(
    // PTP fields
    val ptpId: String? = null,
    val ptpTitle: String? = null,
    val status: String? = null,
    val projectName: String? = null,
    val createdBy: String? = null,

    // Hazard fields
    val hazardId: String? = null,
    val hazardType: String? = null,
    val hazardDescription: String? = null,
    val severity: String? = null, // "critical", "high", "medium", "low"
    val location: String? = null,
    val oshaCode: String? = null,
    val photoId: String? = null,
    val resolved: Boolean? = null,

    // Toolbox talk fields
    val talkId: String? = null,
    val talkTitle: String? = null,
    val topic: String? = null,
    val attendeeCount: Int? = null,
    val conductedBy: String? = null,

    // Photo fields
    val photoPath: String? = null,
    val needsReview: Boolean? = null,
    val analyzed: Boolean? = null,
    val hazardCount: Int? = null,

    // System alert fields
    val alertType: String? = null, // "osha_update", "safety_reminder", "system_update", etc.
    val message: String? = null,
    val priority: String? = null, // "urgent", "high", "medium", "low"
    val actionRequired: Boolean? = null,
    val dismissed: Boolean? = null
)

/**
 * Pagination information
 */
@Serializable
data class PaginationInfo(
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * Date range filter for metrics queries
 */
data class DateRangeFilter(
    val startDate: String, // ISO 8601 format: "2025-01-01"
    val endDate: String    // ISO 8601 format: "2025-12-31"
)

/**
 * Cached data wrapper for graceful degradation
 */
data class CachedData<T>(
    val data: T,
    val cachedAt: Long, // Unix timestamp
    val isStale: Boolean = false
) {
    fun age(): Long = Clock.System.now().toEpochMilliseconds() - cachedAt

    fun ageMinutes(): Long = age() / 60000

    fun isExpired(ttlMs: Long): Boolean = age() > ttlMs
}

/**
 * Time series data point for chart rendering
 * Used for visualizing trends over time
 */
@Serializable
data class TimeSeriesDataPoint(
    val timestamp: Long, // Unix timestamp
    val value: Double,    // Metric value at this point
    val label: String     // Human-readable label (e.g., "Jan 15", "Week 3")
)

/**
 * Time series response for chart data
 * Endpoint: GET /api/dashboard/time-series
 */
@Serializable
data class TimeSeriesResponse(
    val metricType: String, // "incidents", "hazards", "certifications", "compliance_score"
    val dataPoints: List<TimeSeriesDataPoint>,
    val startDate: String,  // ISO 8601 format
    val endDate: String,    // ISO 8601 format
    val period: String,     // "daily", "weekly", "monthly"
    val timestamp: Long     // Unix timestamp when data was generated
)

/**
 * Comparison metrics for week-over-week or period-over-period analysis
 * Endpoint: GET /api/dashboard/comparison
 */
@Serializable
data class ComparisonMetricsResponse(
    val metricType: String,           // "incidents", "hazards", "certifications", "compliance"
    val currentPeriod: PeriodMetrics,
    val previousPeriod: PeriodMetrics,
    val percentageChange: Double,     // Positive = increase, negative = decrease
    val trend: String,                // "improving", "declining", "stable"
    val significance: String,         // "significant", "moderate", "minimal"
    val timestamp: Long               // Unix timestamp when data was generated
)

/**
 * Metrics for a specific time period (used in comparisons)
 */
@Serializable
data class PeriodMetrics(
    val startDate: String,  // ISO 8601 format
    val endDate: String,    // ISO 8601 format
    val value: Double,      // Metric value for this period
    val label: String       // Human-readable label (e.g., "Last Week", "Previous Week")
)
