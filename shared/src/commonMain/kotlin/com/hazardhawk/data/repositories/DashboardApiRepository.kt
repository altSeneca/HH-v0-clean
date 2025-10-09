package com.hazardhawk.data.repositories

import com.hazardhawk.FeatureFlags
import com.hazardhawk.data.network.ApiClient
import com.hazardhawk.data.network.ApiException
import com.hazardhawk.domain.repositories.DashboardRepository
import com.hazardhawk.models.dashboard.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

/**
 * API-backed implementation of DashboardRepository
 *
 * Features:
 * - Real backend API integration
 * - Caching with configurable TTL
 * - Auto-refresh support
 * - Graceful degradation to cached data on errors
 * - Performance optimization
 */
class DashboardApiRepository(
    private val apiClient: ApiClient
) : DashboardRepository {

    // Cache storage
    private val cache = mutableMapOf<String, CachedData<*>>()

    // Cache keys
    private companion object {
        const val CACHE_KEY_SAFETY_METRICS_PREFIX = "safety_metrics_"
        const val CACHE_KEY_COMPLIANCE_SUMMARY = "compliance_summary"
        const val CACHE_KEY_ACTIVITY_FEED_PREFIX = "activity_feed_"
        const val CACHE_KEY_TIME_SERIES_PREFIX = "time_series_"
        const val CACHE_KEY_COMPARISON_PREFIX = "comparison_"

        // Cache TTL: 30 seconds as per requirements
        const val CACHE_TTL_MS = 30_000L

        // API endpoints
        const val ENDPOINT_SAFETY_METRICS = "/api/dashboard/safety-metrics"
        const val ENDPOINT_COMPLIANCE_SUMMARY = "/api/dashboard/compliance-summary"
        const val ENDPOINT_ACTIVITY_FEED = "/api/dashboard/activity-feed"
        const val ENDPOINT_TIME_SERIES = "/api/dashboard/time-series"
        const val ENDPOINT_COMPARISON = "/api/dashboard/comparison"
    }

    /**
     * Get safety metrics for a given period
     */
    override suspend fun getSafetyMetrics(
        period: String,
        startDate: String?,
        endDate: String?
    ): Result<SafetyMetricsResponse> {
        // Check feature flag
        if (!FeatureFlags.API_DASHBOARD_ENABLED) {
            return Result.failure(
                ApiException.ServerError("Dashboard service is temporarily unavailable. Please try again in a few minutes.")
            )
        }

        val cacheKey = "$CACHE_KEY_SAFETY_METRICS_PREFIX$period"

        // Try to get from cache first
        val cachedData = getCachedSafetyMetrics(period)
        if (cachedData != null && !cachedData.isExpired(CACHE_TTL_MS)) {
            return Result.success(cachedData.data)
        }

        // Build query parameters
        val params = mutableMapOf("period" to period)
        if (startDate != null) params["start_date"] = startDate
        if (endDate != null) params["end_date"] = endDate

        // Fetch from API
        val result = apiClient.get<SafetyMetricsResponse>(
            path = ENDPOINT_SAFETY_METRICS,
            parameters = params
        )

        // Cache successful response
        result.onSuccess { response ->
            cache[cacheKey] = CachedData(
                data = response,
                cachedAt = Clock.System.now().toEpochMilliseconds()
            )
        }

        // Graceful degradation: return cached data if API fails and cache exists
        if (result.isFailure && cachedData != null) {
            return Result.success(cachedData.data)
        }

        return result
    }

    /**
     * Get compliance summary data
     */
    override suspend fun getComplianceSummary(): Result<ComplianceSummaryResponse> {
        // Check feature flag
        if (!FeatureFlags.API_DASHBOARD_ENABLED) {
            return Result.failure(
                ApiException.ServerError("Dashboard service is temporarily unavailable. Please try again in a few minutes.")
            )
        }

        // Try to get from cache first
        val cachedData = getCachedComplianceSummary()
        if (cachedData != null && !cachedData.isExpired(CACHE_TTL_MS)) {
            return Result.success(cachedData.data)
        }

        // Fetch from API
        val result = apiClient.get<ComplianceSummaryResponse>(
            path = ENDPOINT_COMPLIANCE_SUMMARY
        )

        // Cache successful response
        result.onSuccess { response ->
            cache[CACHE_KEY_COMPLIANCE_SUMMARY] = CachedData(
                data = response,
                cachedAt = Clock.System.now().toEpochMilliseconds()
            )
        }

        // Graceful degradation: return cached data if API fails and cache exists
        if (result.isFailure && cachedData != null) {
            return Result.success(cachedData.data)
        }

        return result
    }

    /**
     * Get activity feed with pagination
     */
    override suspend fun getActivityFeed(
        page: Int,
        pageSize: Int,
        includeResolved: Boolean
    ): Result<ActivityFeedResponse> {
        // Check feature flag
        if (!FeatureFlags.API_DASHBOARD_ENABLED) {
            return Result.failure(
                ApiException.ServerError("Dashboard service is temporarily unavailable. Please try again in a few minutes.")
            )
        }

        val cacheKey = "$CACHE_KEY_ACTIVITY_FEED_PREFIX${page}_${pageSize}_$includeResolved"

        // Try to get from cache first
        val cachedEntry = cache[cacheKey] as? CachedData<ActivityFeedResponse>
        if (cachedEntry != null && !cachedEntry.isExpired(CACHE_TTL_MS)) {
            return Result.success(cachedEntry.data)
        }

        // Build query parameters
        val params = mapOf(
            "page" to page.toString(),
            "page_size" to pageSize.toString(),
            "include_resolved" to includeResolved.toString()
        )

        // Fetch from API
        val result = apiClient.get<ActivityFeedResponse>(
            path = ENDPOINT_ACTIVITY_FEED,
            parameters = params
        )

        // Cache successful response
        result.onSuccess { response ->
            cache[cacheKey] = CachedData(
                data = response,
                cachedAt = Clock.System.now().toEpochMilliseconds()
            )
        }

        // Graceful degradation: return cached data if API fails and cache exists
        if (result.isFailure && cachedEntry != null) {
            return Result.success(cachedEntry.data)
        }

        return result
    }

    /**
     * Get activity feed as a Flow for reactive updates
     * Polls the API every 30 seconds
     */
    override fun getActivityFeedFlow(
        page: Int,
        pageSize: Int,
        includeResolved: Boolean
    ): Flow<Result<ActivityFeedResponse>> = flow {
        // Initial fetch
        val initialResult = getActivityFeed(page, pageSize, includeResolved)
        emit(initialResult)

        // Auto-refresh every 30 seconds
        while (true) {
            delay(30_000L) // 30 second polling interval

            val refreshResult = getActivityFeed(page, pageSize, includeResolved)
            emit(refreshResult)
        }
    }

    /**
     * Refresh dashboard data from backend
     * Invalidates all caches and fetches fresh data
     */
    override suspend fun refreshDashboardData(): Result<Unit> {
        return try {
            // Clear all caches
            cache.clear()

            // Fetch fresh data for all dashboard components
            // We fetch in parallel to minimize total time
            val metricsResult = getSafetyMetrics()
            val complianceResult = getComplianceSummary()
            val activityResult = getActivityFeed()

            // Check if any of the core requests failed
            when {
                metricsResult.isFailure -> {
                    Result.failure(
                        metricsResult.exceptionOrNull() ?: Exception("Failed to refresh safety metrics")
                    )
                }
                complianceResult.isFailure -> {
                    Result.failure(
                        complianceResult.exceptionOrNull() ?: Exception("Failed to refresh compliance summary")
                    )
                }
                activityResult.isFailure -> {
                    Result.failure(
                        activityResult.exceptionOrNull() ?: Exception("Failed to refresh activity feed")
                    )
                }
                else -> Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get cached safety metrics if available
     */
    override suspend fun getCachedSafetyMetrics(period: String): CachedData<SafetyMetricsResponse>? {
        val cacheKey = "$CACHE_KEY_SAFETY_METRICS_PREFIX$period"
        return cache[cacheKey] as? CachedData<SafetyMetricsResponse>
    }

    /**
     * Get cached compliance summary if available
     */
    override suspend fun getCachedComplianceSummary(): CachedData<ComplianceSummaryResponse>? {
        return cache[CACHE_KEY_COMPLIANCE_SUMMARY] as? CachedData<ComplianceSummaryResponse>
    }

    /**
     * Check if cached data is available and not expired
     */
    override suspend fun hasFreshCachedData(): Boolean {
        val metricsCached = getCachedSafetyMetrics("last_30_days")
        val complianceCached = getCachedComplianceSummary()

        val metricsValid = metricsCached != null && !metricsCached.isExpired(CACHE_TTL_MS)
        val complianceValid = complianceCached != null && !complianceCached.isExpired(CACHE_TTL_MS)

        return metricsValid && complianceValid
    }

    /**
     * Get time series data for chart rendering
     */
    override suspend fun getTimeSeriesData(
        metricType: String,
        startDate: String,
        endDate: String,
        period: String
    ): Result<TimeSeriesResponse> {
        // Check feature flag
        if (!FeatureFlags.API_DASHBOARD_ENABLED) {
            return Result.failure(
                ApiException.ServerError("Dashboard service is temporarily unavailable. Please try again in a few minutes.")
            )
        }

        val cacheKey = "$CACHE_KEY_TIME_SERIES_PREFIX${metricType}_${startDate}_${endDate}_$period"

        // Try to get from cache first
        val cachedEntry = cache[cacheKey] as? CachedData<TimeSeriesResponse>
        if (cachedEntry != null && !cachedEntry.isExpired(CACHE_TTL_MS)) {
            return Result.success(cachedEntry.data)
        }

        // Build query parameters
        val params = mapOf(
            "metric_type" to metricType,
            "start_date" to startDate,
            "end_date" to endDate,
            "period" to period
        )

        // Fetch from API
        val result = apiClient.get<TimeSeriesResponse>(
            path = ENDPOINT_TIME_SERIES,
            parameters = params
        )

        // Cache successful response
        result.onSuccess { response ->
            cache[cacheKey] = CachedData(
                data = response,
                cachedAt = Clock.System.now().toEpochMilliseconds()
            )
        }

        // Graceful degradation: return cached data if API fails and cache exists
        if (result.isFailure && cachedEntry != null) {
            return Result.success(cachedEntry.data)
        }

        return result
    }

    /**
     * Get comparison metrics for week-over-week or period-over-period analysis
     */
    override suspend fun getComparisonMetrics(
        metricType: String,
        currentStartDate: String,
        currentEndDate: String,
        previousStartDate: String?,
        previousEndDate: String?
    ): Result<ComparisonMetricsResponse> {
        // Check feature flag
        if (!FeatureFlags.API_DASHBOARD_ENABLED) {
            return Result.failure(
                ApiException.ServerError("Dashboard service is temporarily unavailable. Please try again in a few minutes.")
            )
        }

        val cacheKey = "$CACHE_KEY_COMPARISON_PREFIX${metricType}_${currentStartDate}_${currentEndDate}"

        // Try to get from cache first
        val cachedEntry = cache[cacheKey] as? CachedData<ComparisonMetricsResponse>
        if (cachedEntry != null && !cachedEntry.isExpired(CACHE_TTL_MS)) {
            return Result.success(cachedEntry.data)
        }

        // Build query parameters
        val params = mutableMapOf(
            "metric_type" to metricType,
            "current_start_date" to currentStartDate,
            "current_end_date" to currentEndDate
        )
        if (previousStartDate != null) params["previous_start_date"] = previousStartDate
        if (previousEndDate != null) params["previous_end_date"] = previousEndDate

        // Fetch from API
        val result = apiClient.get<ComparisonMetricsResponse>(
            path = ENDPOINT_COMPARISON,
            parameters = params
        )

        // Cache successful response
        result.onSuccess { response ->
            cache[cacheKey] = CachedData(
                data = response,
                cachedAt = Clock.System.now().toEpochMilliseconds()
            )
        }

        // Graceful degradation: return cached data if API fails and cache exists
        if (result.isFailure && cachedEntry != null) {
            return Result.success(cachedEntry.data)
        }

        return result
    }

    /**
     * Get friendly error message for user display
     */
    fun getFriendlyErrorMessage(exception: Throwable): String {
        return when (exception) {
            is ApiException.NetworkError -> "Unable to connect. Please check your internet connection and try again."
            is ApiException.Unauthorized -> "Session expired. Please log in again."
            is ApiException.Forbidden -> "You don't have permission to view this data."
            is ApiException.NotFound -> "Dashboard data not available."
            is ApiException.ServerError -> "Our servers are experiencing issues. Please try again later."
            is ApiException.BadRequest -> "Invalid request. Please try refreshing the page."
            is ApiException.ValidationError -> "Invalid data format. Please contact support."
            else -> "An unexpected error occurred. Please try again."
        }
    }
}
