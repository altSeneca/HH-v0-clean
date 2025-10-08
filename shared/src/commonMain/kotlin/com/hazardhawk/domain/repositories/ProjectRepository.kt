package com.hazardhawk.domain.repositories

import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository for project management - centralized source of truth for project information.
 * Project info is reused across all safety documents (PTPs, reports, etc.).
 */
interface ProjectRepository {

    // ===== Core CRUD Operations =====

    /**
     * Create a new project
     */
    suspend fun createProject(
        companyId: String,
        name: String,
        projectNumber: String? = null,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        clientName: String? = null,
        streetAddress: String? = null,
        city: String? = null,
        state: String? = null,
        zip: String? = null,
        generalContractor: String? = null,
        projectManagerId: String? = null,
        superintendentId: String? = null
    ): Result<Project>

    /**
     * Get project by ID
     */
    suspend fun getProject(
        projectId: String,
        includeCompany: Boolean = false,
        includeManagers: Boolean = false
    ): Project?

    /**
     * Update project information
     */
    suspend fun updateProject(
        projectId: String,
        request: UpdateProjectRequest
    ): Result<Project>

    /**
     * Delete project (soft delete - set status to completed)
     */
    suspend fun deleteProject(projectId: String): Result<Unit>

    // ===== Project Queries =====

    /**
     * Get all projects for a company
     */
    suspend fun getProjects(
        companyId: String,
        status: ProjectStatus? = null,
        pagination: PaginationRequest = PaginationRequest()
    ): PaginatedResult<Project>

    /**
     * Get active projects
     */
    suspend fun getActiveProjects(companyId: String): List<Project>

    /**
     * Get projects by status
     */
    suspend fun getProjectsByStatus(
        companyId: String,
        status: ProjectStatus
    ): List<Project>

    /**
     * Search projects by name or number
     */
    suspend fun searchProjects(
        companyId: String,
        query: String,
        limit: Int = 20
    ): List<Project>

    // ===== Project Team Management =====

    /**
     * Assign project manager
     */
    suspend fun assignProjectManager(
        projectId: String,
        projectManagerId: String
    ): Result<Unit>

    /**
     * Assign superintendent
     */
    suspend fun assignSuperintendent(
        projectId: String,
        superintendentId: String
    ): Result<Unit>

    /**
     * Get projects managed by user
     */
    suspend fun getProjectsByManager(
        projectManagerId: String,
        status: ProjectStatus = ProjectStatus.ACTIVE
    ): List<Project>

    // ===== Project Information =====

    /**
     * Get project client information
     */
    suspend fun getProjectClientInfo(projectId: String): ProjectClientInfo?

    /**
     * Update project client information
     */
    suspend fun updateProjectClientInfo(
        projectId: String,
        clientName: String? = null,
        clientContact: String? = null,
        clientPhone: String? = null,
        clientEmail: String? = null
    ): Result<Unit>

    /**
     * Get project location information
     */
    suspend fun getProjectLocationInfo(projectId: String): ProjectLocationInfo?

    /**
     * Update project location information
     */
    suspend fun updateProjectLocationInfo(
        projectId: String,
        streetAddress: String? = null,
        city: String? = null,
        state: String? = null,
        zip: String? = null
    ): Result<Unit>

    // ===== Statistics =====

    /**
     * Get project count by status
     */
    suspend fun getProjectCountByStatus(companyId: String): Map<ProjectStatus, Int>

    /**
     * Get total active projects
     */
    suspend fun getActiveProjectCount(companyId: String): Int

    /**
     * Get project statistics
     */
    suspend fun getProjectStats(projectId: String): ProjectStatistics

    // ===== Reactive Queries =====

    /**
     * Observe projects for a company
     */
    fun observeProjects(
        companyId: String,
        status: ProjectStatus? = null
    ): Flow<List<Project>>

    /**
     * Observe a single project
     */
    fun observeProject(projectId: String): Flow<Project?>
}

/**
 * Project client information
 */
data class ProjectClientInfo(
    val clientName: String?,
    val clientContact: String?,
    val clientPhone: String?,
    val clientEmail: String?
)

/**
 * Project location information
 */
data class ProjectLocationInfo(
    val streetAddress: String?,
    val city: String?,
    val state: String?,
    val zip: String?,
    val fullAddress: String
)

/**
 * Project statistics
 */
data class ProjectStatistics(
    val totalCrews: Int,
    val totalWorkers: Int,
    val activePTPs: Int,
    val completedPTPs: Int,
    val totalIncidents: Int,
    val daysActive: Int,
    val estimatedDaysRemaining: Int?
)
