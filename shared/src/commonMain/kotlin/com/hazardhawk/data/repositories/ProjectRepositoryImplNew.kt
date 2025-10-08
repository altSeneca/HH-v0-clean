package com.hazardhawk.data.repositories

import com.hazardhawk.domain.repositories.*
import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import java.util.UUID

/**
 * In-memory implementation of ProjectRepository
 * This is a simplified implementation for demonstration purposes.
 *
 * TODO: Replace with actual database implementation (SQLDelight, Room, etc.)
 */
class ProjectRepositoryImplNew : ProjectRepository {

    // In-memory storage
    private val projects = mutableMapOf<String, Project>()

    // ===== Core CRUD Operations =====

    override suspend fun createProject(
        companyId: String,
        name: String,
        projectNumber: String?,
        startDate: LocalDate,
        endDate: LocalDate?,
        clientName: String?,
        streetAddress: String?,
        city: String?,
        state: String?,
        zip: String?,
        generalContractor: String?,
        projectManagerId: String?,
        superintendentId: String?
    ): Result<Project> {
        return try {
            val projectId = UUID.randomUUID().toString()
            val newProject = Project(
                id = projectId,
                companyId = companyId,
                name = name,
                projectNumber = projectNumber,
                location = buildLocationString(streetAddress, city, state, zip),
                startDate = startDate,
                endDate = endDate,
                status = ProjectStatus.ACTIVE.name.lowercase(),
                projectManagerId = projectManagerId,
                superintendentId = superintendentId,
                clientName = clientName,
                clientContact = null,
                clientPhone = null,
                clientEmail = null,
                streetAddress = streetAddress,
                city = city,
                state = state,
                zip = zip,
                generalContractor = generalContractor
            )

            projects[projectId] = newProject
            Result.success(newProject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProject(
        projectId: String,
        includeCompany: Boolean,
        includeManagers: Boolean
    ): Project? {
        return projects[projectId]
    }

    override suspend fun updateProject(
        projectId: String,
        request: UpdateProjectRequest
    ): Result<Project> {
        return try {
            val existingProject = projects[projectId]
                ?: return Result.failure(IllegalArgumentException("Project not found"))

            val updatedProject = existingProject.copy(
                name = request.name ?: existingProject.name,
                projectNumber = request.projectNumber ?: existingProject.projectNumber,
                startDate = request.startDate ?: existingProject.startDate,
                endDate = request.endDate ?: existingProject.endDate,
                status = request.status ?: existingProject.status,
                clientName = request.clientName ?: existingProject.clientName,
                clientContact = request.clientContact ?: existingProject.clientContact,
                clientPhone = request.clientPhone ?: existingProject.clientPhone,
                clientEmail = request.clientEmail ?: existingProject.clientEmail,
                streetAddress = request.streetAddress ?: existingProject.streetAddress,
                city = request.city ?: existingProject.city,
                state = request.state ?: existingProject.state,
                zip = request.zip ?: existingProject.zip,
                generalContractor = request.generalContractor ?: existingProject.generalContractor,
                projectManagerId = request.projectManagerId ?: existingProject.projectManagerId,
                superintendentId = request.superintendentId ?: existingProject.superintendentId,
                location = buildLocationString(
                    request.streetAddress ?: existingProject.streetAddress,
                    request.city ?: existingProject.city,
                    request.state ?: existingProject.state,
                    request.zip ?: existingProject.zip
                )
            )

            projects[projectId] = updatedProject
            Result.success(updatedProject)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            projects.remove(projectId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Project Queries =====

    override suspend fun getProjects(
        companyId: String,
        status: ProjectStatus?,
        pagination: PaginationRequest
    ): PaginatedResult<Project> {
        val filteredProjects = projects.values
            .filter { it.companyId == companyId }
            .filter { status == null || it.status == status.name.lowercase() }
            .toList()

        return PaginatedResult(
            data = filteredProjects,
            page = pagination.page,
            pageSize = pagination.pageSize,
            totalCount = filteredProjects.size,
            totalPages = (filteredProjects.size + pagination.pageSize - 1) / pagination.pageSize,
            hasNext = false,
            hasPrevious = false
        )
    }

    override suspend fun getActiveProjects(companyId: String): List<Project> {
        return projects.values
            .filter { it.companyId == companyId }
            .filter { it.status == ProjectStatus.ACTIVE.name.lowercase() }
            .toList()
    }

    override suspend fun getProjectsByStatus(
        companyId: String,
        status: ProjectStatus
    ): List<Project> {
        return projects.values
            .filter { it.companyId == companyId }
            .filter { it.status == status.name.lowercase() }
            .toList()
    }

    override suspend fun searchProjects(
        companyId: String,
        query: String,
        limit: Int
    ): List<Project> {
        return projects.values
            .filter { it.companyId == companyId }
            .filter {
                it.name.contains(query, ignoreCase = true) ||
                it.projectNumber?.contains(query, ignoreCase = true) == true
            }
            .take(limit)
    }

    // ===== Project Team Management =====

    override suspend fun assignProjectManager(
        projectId: String,
        projectManagerId: String
    ): Result<Unit> {
        return try {
            val project = projects[projectId]
                ?: return Result.failure(IllegalArgumentException("Project not found"))

            projects[projectId] = project.copy(projectManagerId = projectManagerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun assignSuperintendent(
        projectId: String,
        superintendentId: String
    ): Result<Unit> {
        return try {
            val project = projects[projectId]
                ?: return Result.failure(IllegalArgumentException("Project not found"))

            projects[projectId] = project.copy(superintendentId = superintendentId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProjectsByManager(
        projectManagerId: String,
        status: ProjectStatus
    ): List<Project> {
        return projects.values
            .filter { it.projectManagerId == projectManagerId }
            .filter { it.status == status.name.lowercase() }
            .toList()
    }

    // ===== Project Information =====

    override suspend fun getProjectClientInfo(projectId: String): ProjectClientInfo? {
        val project = projects[projectId] ?: return null
        return ProjectClientInfo(
            clientName = project.clientName,
            clientContact = project.clientContact,
            clientPhone = project.clientPhone,
            clientEmail = project.clientEmail
        )
    }

    override suspend fun updateProjectClientInfo(
        projectId: String,
        clientName: String?,
        clientContact: String?,
        clientPhone: String?,
        clientEmail: String?
    ): Result<Unit> {
        return try {
            val project = projects[projectId]
                ?: return Result.failure(IllegalArgumentException("Project not found"))

            projects[projectId] = project.copy(
                clientName = clientName ?: project.clientName,
                clientContact = clientContact ?: project.clientContact,
                clientPhone = clientPhone ?: project.clientPhone,
                clientEmail = clientEmail ?: project.clientEmail
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProjectLocationInfo(projectId: String): ProjectLocationInfo? {
        val project = projects[projectId] ?: return null
        return ProjectLocationInfo(
            streetAddress = project.streetAddress,
            city = project.city,
            state = project.state,
            zip = project.zip,
            fullAddress = buildLocationString(
                project.streetAddress,
                project.city,
                project.state,
                project.zip
            ) ?: ""
        )
    }

    override suspend fun updateProjectLocationInfo(
        projectId: String,
        streetAddress: String?,
        city: String?,
        state: String?,
        zip: String?
    ): Result<Unit> {
        return try {
            val project = projects[projectId]
                ?: return Result.failure(IllegalArgumentException("Project not found"))

            projects[projectId] = project.copy(
                streetAddress = streetAddress ?: project.streetAddress,
                city = city ?: project.city,
                state = state ?: project.state,
                zip = zip ?: project.zip,
                location = buildLocationString(
                    streetAddress ?: project.streetAddress,
                    city ?: project.city,
                    state ?: project.state,
                    zip ?: project.zip
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Statistics =====

    override suspend fun getProjectCountByStatus(companyId: String): Map<ProjectStatus, Int> {
        val projectsByStatus = projects.values
            .filter { it.companyId == companyId }
            .groupBy { ProjectStatus.fromString(it.status) }

        return ProjectStatus.values().associateWith { status ->
            projectsByStatus[status]?.size ?: 0
        }
    }

    override suspend fun getActiveProjectCount(companyId: String): Int {
        return projects.values
            .count { it.companyId == companyId && it.status == ProjectStatus.ACTIVE.name.lowercase() }
    }

    override suspend fun getProjectStats(projectId: String): ProjectStatistics {
        // TODO: Implement actual statistics gathering from related entities
        return ProjectStatistics(
            totalCrews = 0,
            totalWorkers = 0,
            activePTPs = 0,
            completedPTPs = 0,
            totalIncidents = 0,
            daysActive = 0,
            estimatedDaysRemaining = null
        )
    }

    // ===== Reactive Queries =====

    override fun observeProjects(
        companyId: String,
        status: ProjectStatus?
    ): Flow<List<Project>> {
        val filtered = projects.values
            .filter { it.companyId == companyId }
            .filter { status == null || it.status == status.name.lowercase() }
            .toList()
        return flowOf(filtered)
    }

    override fun observeProject(projectId: String): Flow<Project?> {
        return flowOf(projects[projectId])
    }

    // ===== Helper Functions =====

    private fun buildLocationString(
        streetAddress: String?,
        city: String?,
        state: String?,
        zip: String?
    ): String? {
        val parts = listOfNotNull(
            streetAddress?.takeIf { it.isNotBlank() },
            city?.takeIf { it.isNotBlank() },
            state?.takeIf { it.isNotBlank() },
            zip?.takeIf { it.isNotBlank() }
        )
        return if (parts.isEmpty()) null else parts.joinToString(", ")
    }
}
