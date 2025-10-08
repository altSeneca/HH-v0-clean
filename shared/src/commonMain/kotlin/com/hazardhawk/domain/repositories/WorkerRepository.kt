package com.hazardhawk.domain.repositories

import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for worker management with support for CRUD, search, filtering, and pagination.
 * Follows the crew management implementation plan.
 */
interface WorkerRepository {

    // ===== Core CRUD Operations =====

    /**
     * Create a new worker for a company
     */
    suspend fun createWorker(
        companyId: String,
        request: CreateWorkerRequest
    ): Result<CompanyWorker>

    /**
     * Get worker by ID with optional related data
     */
    suspend fun getWorker(
        workerId: String,
        includeProfile: Boolean = true,
        includeCertifications: Boolean = false,
        includeCrews: Boolean = false
    ): CompanyWorker?

    /**
     * Update worker information
     */
    suspend fun updateWorker(
        workerId: String,
        request: UpdateWorkerRequest
    ): Result<CompanyWorker>

    /**
     * Soft delete worker (set status to TERMINATED)
     */
    suspend fun deleteWorker(workerId: String): Result<Unit>

    // ===== Search and Filtering =====

    /**
     * Get all workers for a company with filtering and pagination
     */
    suspend fun getWorkers(
        companyId: String,
        filters: WorkerFilters = WorkerFilters(),
        pagination: PaginationRequest = PaginationRequest()
    ): PaginatedResult<CompanyWorker>

    /**
     * Search workers by name or employee number
     */
    suspend fun searchWorkers(
        companyId: String,
        query: String,
        filters: WorkerFilters = WorkerFilters(),
        limit: Int = 20
    ): List<CompanyWorker>

    /**
     * Get workers by role
     */
    suspend fun getWorkersByRole(
        companyId: String,
        role: WorkerRole,
        status: WorkerStatus = WorkerStatus.ACTIVE
    ): List<CompanyWorker>

    /**
     * Get workers by status
     */
    suspend fun getWorkersByStatus(
        companyId: String,
        status: WorkerStatus
    ): List<CompanyWorker>

    // ===== Worker Profile Management =====

    /**
     * Get worker profile
     */
    suspend fun getWorkerProfile(profileId: String): WorkerProfile?

    /**
     * Update worker profile information
     */
    suspend fun updateWorkerProfile(
        profileId: String,
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        phone: String? = null,
        dateOfBirth: kotlinx.datetime.LocalDate? = null
    ): Result<WorkerProfile>

    /**
     * Upload worker photo
     */
    suspend fun uploadWorkerPhoto(
        profileId: String,
        photoData: ByteArray,
        fileName: String
    ): Result<String> // Returns photo URL

    // ===== Bulk Operations =====

    /**
     * Create multiple workers in a batch
     */
    suspend fun createWorkersBatch(
        companyId: String,
        workers: List<CreateWorkerRequest>
    ): Result<List<CompanyWorker>>

    /**
     * Update multiple workers
     */
    suspend fun updateWorkersBatch(
        updates: Map<String, UpdateWorkerRequest>
    ): Result<Int> // Returns number updated

    // ===== Statistics =====

    /**
     * Get worker count by status
     */
    suspend fun getWorkerCountByStatus(companyId: String): Map<WorkerStatus, Int>

    /**
     * Get worker count by role
     */
    suspend fun getWorkerCountByRole(companyId: String): Map<WorkerRole, Int>

    /**
     * Get total active workers
     */
    suspend fun getActiveWorkerCount(companyId: String): Int

    // ===== Reactive Queries =====

    /**
     * Observe workers as a Flow for reactive updates
     */
    fun observeWorkers(
        companyId: String,
        filters: WorkerFilters = WorkerFilters()
    ): Flow<List<CompanyWorker>>

    /**
     * Observe a single worker
     */
    fun observeWorker(workerId: String): Flow<CompanyWorker?>
}
