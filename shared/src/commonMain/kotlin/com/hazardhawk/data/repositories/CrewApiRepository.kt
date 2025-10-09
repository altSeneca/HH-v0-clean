package com.hazardhawk.data.repositories
import kotlinx.datetime.Clock

import com.hazardhawk.FeatureFlags
import com.hazardhawk.data.network.ApiClient
import com.hazardhawk.data.network.ApiResponse
import com.hazardhawk.data.network.AssignmentResponse
import com.hazardhawk.data.network.ErrorMapper
import com.hazardhawk.data.network.PaginatedResponse
import com.hazardhawk.data.network.QRCodeResponse
import com.hazardhawk.data.network.SuccessResponse
import com.hazardhawk.domain.repositories.CrewRepository
import com.hazardhawk.models.crew.Crew
import com.hazardhawk.models.crew.CrewMember
import com.hazardhawk.models.crew.CreateCrewRequest
import com.hazardhawk.models.crew.UpdateCrewRequest
import com.hazardhawk.models.crew.TrackAttendanceRequest
import com.hazardhawk.models.crew.AttendanceRecord
import com.hazardhawk.models.crew.CrewAnalytics
import com.hazardhawk.models.crew.CrewPerformanceMetrics
import com.hazardhawk.models.crew.AssignMultipleProjectsRequest
import com.hazardhawk.models.crew.CrewAvailability
import com.hazardhawk.models.crew.ScheduleConflict

/**
 * API-backed implementation of CrewRepository
 *
 * Implements full CRUD operations, crew member management,
 * QR code generation, and project assignments via backend API.
 *
 * Features:
 * - Feature flag controlled (FeatureFlags.API_CREW_ENABLED)
 * - Full error handling with friendly messages
 * - Result<T> return types for safe error handling
 * - Pagination support
 * - Request caching
 */
class CrewApiRepository(
    private val apiClient: ApiClient
) : CrewRepository {

    private val cache = mutableMapOf<String, CacheEntry<Crew>>()
    private val cacheEnabled = FeatureFlags.API_CACHE_ENABLED
    private val cacheTtlMs = FeatureFlags.API_CACHE_TTL_SECONDS * 1000

    /**
     * Get crew by ID with optional related data
     */
    override suspend fun getCrew(
        crewId: String,
        includeMembers: Boolean,
        includeForeman: Boolean,
        includeProject: Boolean
    ): Crew? {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return null
        }

        // Check cache first
        if (cacheEnabled) {
            val cached = cache[crewId]
            if (cached != null && !cached.isExpired()) {
                return cached.value
            }
        }

        val params = mutableMapOf<String, String>()
        if (includeMembers) params["include_members"] = "true"
        if (includeForeman) params["include_foreman"] = "true"
        if (includeProject) params["include_project"] = "true"

        val result = apiClient.get<ApiResponse<Crew>>(
            path = "/api/crews/$crewId",
            parameters = params
        )

        return result.getOrNull()?.data?.also { crew ->
            if (cacheEnabled) {
                cache[crewId] = CacheEntry(crew)
            }
        }
    }

    /**
     * Get all crews with pagination
     */
    suspend fun getCrews(
        page: Int = 1,
        pageSize: Int = 20,
        includeMembers: Boolean = false,
        includeForeman: Boolean = false
    ): Result<PaginatedResponse<Crew>> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        val params = mutableMapOf(
            "page" to page.toString(),
            "page_size" to pageSize.toString()
        )
        if (includeMembers) params["include_members"] = "true"
        if (includeForeman) params["include_foreman"] = "true"

        return apiClient.get(
            path = "/api/crews",
            parameters = params
        )
    }

    /**
     * Create a new crew
     */
    suspend fun createCrew(request: CreateCrewRequest): Result<Crew> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        // Validate request
        val validationError = validateCrewRequest(request)
        if (validationError != null) {
            return Result.failure(Exception(validationError))
        }

        val result = apiClient.post<ApiResponse<Crew>>(
            path = "/api/crews",
            body = request
        )

        return result.map { response ->
            response.data.also { crew ->
                if (cacheEnabled) {
                    cache[crew.id] = CacheEntry(crew)
                }
            }
        }
    }

    /**
     * Update an existing crew
     */
    suspend fun updateCrew(crewId: String, request: UpdateCrewRequest): Result<Crew> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        val result = apiClient.patch<ApiResponse<Crew>>(
            path = "/api/crews/$crewId",
            body = request
        )

        return result.map { response ->
            response.data.also { crew ->
                // Invalidate cache
                cache.remove(crewId)
                // Update cache with new data
                if (cacheEnabled) {
                    cache[crewId] = CacheEntry(crew)
                }
            }
        }
    }

    /**
     * Delete a crew
     */
    suspend fun deleteCrew(crewId: String): Result<Boolean> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        val result = apiClient.delete<SuccessResponse>(
            path = "/api/crews/$crewId"
        )

        return result.map { response ->
            // Invalidate cache
            cache.remove(crewId)
            response.success
        }
    }

    /**
     * Add a member to a crew
     */
    suspend fun addCrewMember(
        crewId: String,
        companyWorkerId: String,
        role: String
    ): Result<CrewMember> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        val request = mapOf(
            "company_worker_id" to companyWorkerId,
            "role" to role
        )

        val result = apiClient.post<ApiResponse<CrewMember>>(
            path = "/api/crews/$crewId/members",
            body = request
        )

        return result.map { response ->
            // Invalidate crew cache to force refresh
            cache.remove(crewId)
            response.data
        }
    }

    /**
     * Remove a member from a crew
     */
    suspend fun removeCrewMember(crewId: String, memberId: String): Result<Boolean> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        val result = apiClient.delete<SuccessResponse>(
            path = "/api/crews/$crewId/members/$memberId"
        )

        return result.map { response ->
            // Invalidate crew cache to force refresh
            cache.remove(crewId)
            response.success
        }
    }

    /**
     * Update crew member role
     */
    suspend fun updateCrewMemberRole(
        crewId: String,
        memberId: String,
        newRole: String
    ): Result<CrewMember> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        val request = mapOf("role" to newRole)

        val result = apiClient.patch<ApiResponse<CrewMember>>(
            path = "/api/crews/$crewId/members/$memberId",
            body = request
        )

        return result.map { response ->
            // Invalidate crew cache
            cache.remove(crewId)
            response.data
        }
    }

    /**
     * Generate QR code for crew
     */
    suspend fun generateCrewQRCode(crewId: String): Result<QRCodeResponse> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        return apiClient.post(
            path = "/api/crews/$crewId/qr-code",
            body = emptyMap<String, String>()
        )
    }

    /**
     * Assign crew to project
     */
    suspend fun assignCrewToProject(
        projectId: String,
        crewId: String
    ): Result<AssignmentResponse> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        val request = mapOf("crew_id" to crewId)

        return apiClient.post(
            path = "/api/projects/$projectId/assign-crew",
            body = request
        )
    }

    /**
     * Unassign crew from project
     */
    suspend fun unassignCrewFromProject(
        projectId: String,
        crewId: String
    ): Result<Boolean> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        val result = apiClient.delete<SuccessResponse>(
            path = "/api/projects/$projectId/crews/$crewId"
        )

        return result.map { it.success }
    }

    /**
     * Get crew assignments (all projects assigned to a crew)
     */
    suspend fun getCrewAssignments(crewId: String): Result<List<AssignmentResponse>> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        return apiClient.get<ApiResponse<List<AssignmentResponse>>>(
            path = "/api/crews/$crewId/assignments"
        ).map { it.data }
    }

    /**
     * Sync crew roles and permissions from backend
     */
    suspend fun syncCrewRoles(crewId: String): Result<Boolean> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        val result = apiClient.post<SuccessResponse>(
            path = "/api/crews/$crewId/sync-roles",
            body = emptyMap<String, String>()
        )

        return result.map { response ->
            // Invalidate cache to force refresh with new permissions
            cache.remove(crewId)
            response.success
        }
    }

    /**
     * Clear cache (useful for testing or manual refresh)
     */
    fun clearCache() {
        cache.clear()
    }

    // ========== Week 3: Advanced Features ==========

    /**
     * Track crew attendance (check-in/check-out) with GPS coordinates
     *
     * @param crewId The crew ID
     * @param request Attendance tracking request with type, timestamp, and GPS
     * @return Result with attendance record or error
     */
    suspend fun trackAttendance(
        crewId: String,
        request: TrackAttendanceRequest
    ): Result<AttendanceRecord> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        // Validate crew ID
        if (crewId.isBlank()) {
            return Result.failure(Exception("Crew ID cannot be empty"))
        }

        return apiClient.post(
            path = "/api/crews/$crewId/attendance",
            body = request
        )
    }

    /**
     * Get crew analytics (active/inactive counts, utilization metrics)
     *
     * @return Result with crew analytics or error
     */
    suspend fun getCrewAnalytics(): Result<CrewAnalytics> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        return apiClient.get<ApiResponse<CrewAnalytics>>(
            path = "/api/crews/analytics"
        ).map { it.data }
    }

    /**
     * Get crew performance metrics for a specific crew
     *
     * @param crewId The crew ID
     * @param periodStart Optional start date for metrics period (ISO format)
     * @param periodEnd Optional end date for metrics period (ISO format)
     * @return Result with performance metrics or error
     */
    suspend fun getCrewPerformanceMetrics(
        crewId: String,
        periodStart: String? = null,
        periodEnd: String? = null
    ): Result<CrewPerformanceMetrics> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        if (crewId.isBlank()) {
            return Result.failure(Exception("Crew ID cannot be empty"))
        }

        val params = mutableMapOf<String, String>()
        if (periodStart != null) params["period_start"] = periodStart
        if (periodEnd != null) params["period_end"] = periodEnd

        return apiClient.get<ApiResponse<CrewPerformanceMetrics>>(
            path = "/api/crews/$crewId/performance",
            parameters = params
        ).map { it.data }
    }

    /**
     * Assign crew to multiple projects at once
     *
     * @param crewId The crew ID
     * @param projectIds List of project IDs to assign
     * @return Result with list of assignment responses or error
     */
    suspend fun assignCrewToMultipleProjects(
        crewId: String,
        projectIds: List<String>
    ): Result<List<AssignmentResponse>> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        if (crewId.isBlank()) {
            return Result.failure(Exception("Crew ID cannot be empty"))
        }

        if (projectIds.isEmpty()) {
            return Result.failure(Exception("At least one project ID is required"))
        }

        val request = AssignMultipleProjectsRequest(
            crewId = crewId,
            projectIds = projectIds
        )

        return apiClient.post<ApiResponse<List<AssignmentResponse>>>(
            path = "/api/crews/$crewId/projects/assign-multiple",
            body = request
        ).map { it.data }
    }

    /**
     * Get crew availability for a date range
     *
     * @param crewId The crew ID
     * @param startDate Start date for availability check (ISO format)
     * @param endDate End date for availability check (ISO format)
     * @return Result with crew availability or error
     */
    suspend fun getCrewAvailability(
        crewId: String,
        startDate: String,
        endDate: String
    ): Result<CrewAvailability> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        if (crewId.isBlank()) {
            return Result.failure(Exception("Crew ID cannot be empty"))
        }

        if (startDate.isBlank() || endDate.isBlank()) {
            return Result.failure(Exception("Start date and end date are required"))
        }

        val params = mapOf(
            "start_date" to startDate,
            "end_date" to endDate
        )

        return apiClient.get<ApiResponse<CrewAvailability>>(
            path = "/api/crews/$crewId/availability",
            parameters = params
        ).map { it.data }
    }

    /**
     * Detect schedule conflicts for crews (double-booking detection)
     *
     * @param startDate Start date for conflict check (ISO format)
     * @param endDate End date for conflict check (ISO format)
     * @param crewIds Optional list of crew IDs to check (null for all crews)
     * @return Result with list of schedule conflicts or error
     */
    suspend fun detectScheduleConflicts(
        startDate: String,
        endDate: String,
        crewIds: List<String>? = null
    ): Result<List<ScheduleConflict>> {
        if (!FeatureFlags.API_CREW_ENABLED) {
            return Result.failure(Exception("Crew API is not enabled"))
        }

        if (startDate.isBlank() || endDate.isBlank()) {
            return Result.failure(Exception("Start date and end date are required"))
        }

        val request = mutableMapOf(
            "start_date" to startDate,
            "end_date" to endDate
        )
        if (crewIds != null && crewIds.isNotEmpty()) {
            request["crew_ids"] = crewIds.joinToString(",")
        }

        return apiClient.post<ApiResponse<List<ScheduleConflict>>>(
            path = "/api/crews/detect-conflicts",
            body = request
        ).map { it.data }
    }

    /**
     * Validate crew creation request
     */
    private fun validateCrewRequest(request: CreateCrewRequest): String? {
        if (request.name.isBlank()) {
            return com.hazardhawk.data.network.ErrorMapper.CrewErrors.CREW_NAME_EMPTY
        }

        if (request.name.length < 3) {
            return com.hazardhawk.data.network.ErrorMapper.CrewErrors.CREW_NAME_TOO_SHORT
        }

        if (request.name.length > 100) {
            return com.hazardhawk.data.network.ErrorMapper.CrewErrors.CREW_NAME_TOO_LONG
        }

        return null
    }

    /**
     * Cache entry with expiration
     */
    private data class CacheEntry<T>(
        val value: T,
        val timestamp: Long = Clock.System.now().toEpochMilliseconds()
    ) {
        fun isExpired(): Boolean {
            return Clock.System.now().toEpochMilliseconds() - timestamp > cacheTtlMs
        }

        companion object {
            private val cacheTtlMs = FeatureFlags.API_CACHE_TTL_SECONDS * 1000
        }
    }
}
