package com.hazardhawk.domain.repositories

import com.hazardhawk.models.dashboard.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for dashboard data access
 *
 * Provides access to safety metrics, compliance data, and activity feeds.
 * Implementation may be API-backed or mock-based depending on feature flags.
 */
interface DashboardRepository {
    /**
     * Get safety metrics for a given period
     *
     * @param period Time period for metrics ("last_7_days", "last_30_days", "last_90_days", "ytd")
     * @param startDate Optional custom start date (ISO 8601 format: "2025-01-01")
     * @param endDate Optional custom end date (ISO 8601 format: "2025-12-31")
     * @return Result containing SafetyMetricsResponse or error
     */
    suspend fun getSafetyMetrics(
        period: String = "last_30_days",
        startDate: String? = null,
        endDate: String? = null
    ): Result<SafetyMetricsResponse>

    /**
     * Get compliance summary data
     *
     * @return Result containing ComplianceSummaryResponse or error
     */
    suspend fun getComplianceSummary(): Result<ComplianceSummaryResponse>

    /**
     * Get activity feed with pagination
     *
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @param includeResolved Include resolved hazards and dismissed alerts
     * @return Result containing ActivityFeedResponse or error
     */
    suspend fun getActivityFeed(
        page: Int = 0,
        pageSize: Int = 20,
        includeResolved: Boolean = false
    ): Result<ActivityFeedResponse>

    /**
     * Get activity feed as a Flow for reactive updates
     *
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @param includeResolved Include resolved hazards and dismissed alerts
     * @return Flow of ActivityFeedResponse with real-time updates
     */
    fun getActivityFeedFlow(
        page: Int = 0,
        pageSize: Int = 20,
        includeResolved: Boolean = false
    ): Flow<Result<ActivityFeedResponse>>

    /**
     * Refresh dashboard data from backend
     * Invalidates cache and fetches fresh data
     *
     * @return Result indicating success or failure
     */
    suspend fun refreshDashboardData(): Result<Unit>

    /**
     * Get cached safety metrics if available
     *
     * @param period Time period for metrics
     * @return Cached data or null if not available
     */
    suspend fun getCachedSafetyMetrics(period: String): CachedData<SafetyMetricsResponse>?

    /**
     * Get cached compliance summary if available
     *
     * @return Cached data or null if not available
     */
    suspend fun getCachedComplianceSummary(): CachedData<ComplianceSummaryResponse>?

    /**
     * Check if cached data is available and not expired
     *
     * @return True if valid cached data exists
     */
    suspend fun hasFreshCachedData(): Boolean

    /**
     * Get time series data for chart rendering
     *
     * @param metricType Type of metric ("incidents", "hazards", "certifications", "compliance_score")
     * @param startDate Start date (ISO 8601 format: "2025-01-01")
     * @param endDate End date (ISO 8601 format: "2025-12-31")
     * @param period Granularity ("daily", "weekly", "monthly")
     * @return Result containing TimeSeriesResponse or error
     */
    suspend fun getTimeSeriesData(
        metricType: String,
        startDate: String,
        endDate: String,
        period: String = "daily"
    ): Result<TimeSeriesResponse>

    /**
     * Get comparison metrics for week-over-week or period-over-period analysis
     *
     * @param metricType Type of metric to compare ("incidents", "hazards", "certifications", "compliance")
     * @param currentStartDate Start date of current period (ISO 8601 format)
     * @param currentEndDate End date of current period (ISO 8601 format)
     * @param previousStartDate Start date of previous period (ISO 8601 format, optional)
     * @param previousEndDate End date of previous period (ISO 8601 format, optional)
     * @return Result containing ComparisonMetricsResponse or error
     */
    suspend fun getComparisonMetrics(
        metricType: String,
        currentStartDate: String,
        currentEndDate: String,
        previousStartDate: String? = null,
        previousEndDate: String? = null
    ): Result<ComparisonMetricsResponse>
}
