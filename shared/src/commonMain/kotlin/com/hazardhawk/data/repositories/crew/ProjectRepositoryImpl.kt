package com.hazardhawk.data.repositories.crew

import com.hazardhawk.domain.repositories.*
import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

/**
 * Implementation of ProjectRepository with in-memory storage.
 * TODO: Replace with actual API client and database integration
 */
class ProjectRepositoryImpl(
    private val companyRepository: CompanyRepository,
    private val workerRepository: WorkerRepository
) : ProjectRepository {

    // In-memory storage
    private val projects = mutableMapOf<String, Project>()
    private val projectsFlow = MutableStateFlow<List<Project>>(emptyList())

    // ===== Core CRUD Operations =====

    suspend fun createProject(
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
            val projectId = generateId()

            val project = Project(
                id = projectId,
                companyId = companyId,
                name = name,
                projectNumber = projectNumber,
                startDate = startDate,
                endDate = endDate,
                status = ProjectStatus.ACTIVE.name,
                projectManagerId = projectManagerId,
                superintendentId = superintendentId,
                clientName = clientName,
                streetAddress = streetAddress,
                city = city,
                state = state,
                zip = zip,
                generalContractor = generalContractor
            )

            projects[projectId] = project
            emitProjectsUpdate()

            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProject(
        projectId: String,
        includeCompany: Boolean,
        includeManagers: Boolean
    ): Project? {
        return projects[projectId]?.let { project ->
            var result = project

            if (includeCompany) {
                result = result.copy(
                    company = companyRepository.getCompany(project.companyId)
                )
            }

            if (includeManagers) {
                result = result.copy(
                    projectManager = project.projectManagerId?.let { workerRepository.getWorker(it) },
                    superintendent = project.superintendentId?.let { workerRepository.getWorker(it) }
                )
            }

            result
        }
    }

    suspend fun updateProject(
        projectId: String,
        request: UpdateProjectRequest
    ): Result<Project> {
        return try {
            val project = projects[projectId]
                ?: return Result.failure(IllegalArgumentException("Project not found: $projectId"))

            val updated = project.copy(
                name = request.name ?: project.name,
                projectNumber = request.projectNumber ?: project.projectNumber,
                status = request.status?.name ?: project.status,
                clientName = request.clientName ?: project.clientName,
                streetAddress = request.streetAddress ?: project.streetAddress,
                city = request.city ?: project.city,
                state = request.state ?: project.state,
                zip = request.zip ?: project.zip,
                generalContractor = request.generalContractor ?: project.generalContractor
            )

            projects[projectId] = updated
            emitProjectsUpdate()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProject(projectId: String): Result<Unit> {
        return try {
            val project = projects[projectId]
                ?: return Result.failure(IllegalArgumentException("Project not found"))

            projects[projectId] = project.copy(
                status = ProjectStatus.COMPLETED.name
            )
            emitProjectsUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Project Queries =====

    suspend fun getProjects(
        companyId: String,
        status: ProjectStatus?,
        pagination: PaginationRequest
    ): PaginatedResult<Project> {
        var filtered = projects.values.filter { it.companyId == companyId }

        status?.let { s ->
            filtered = filtered.filter { it.status == s.name }
        }

        val sorted = when (pagination.sortBy) {
            "name" -> filtered.sortedBy { it.name }
            "start_date" -> filtered.sortedBy { it.startDate }
            else -> filtered
        }

        val finalSorted = if (pagination.sortDirection == SortDirection.DESC) {
            sorted.reversed()
        } else {
            sorted
        }

        val pageSize = pagination.pageSize.coerceIn(1, 100)
        val startIndex = pagination.cursor?.toIntOrNull() ?: 0
        val endIndex = (startIndex + pageSize).coerceAtMost(finalSorted.size)
        val page = finalSorted.subList(startIndex, endIndex)

        return PaginatedResult(
            data = page,
            pagination = PaginationInfo(
                nextCursor = if (endIndex < finalSorted.size) endIndex.toString() else null,
                hasMore = endIndex < finalSorted.size,
                totalCount = filtered.size
            )
        )
    }

    suspend fun getActiveProjects(companyId: String): List<Project> {
        return projects.values.filter {
            it.companyId == companyId && it.status == ProjectStatus.ACTIVE.name
        }
    }

    suspend fun getProjectsByStatus(
        companyId: String,
        status: ProjectStatus
    ): List<Project> {
        return projects.values.filter {
            it.companyId == companyId && it.status == status.name
        }
    }

    suspend fun searchProjects(
        companyId: String,
        query: String,
        limit: Int
    ): List<Project> {
        return projects.values
            .filter { it.companyId == companyId }
            .filter { project ->
                project.name.contains(query, ignoreCase = true) ||
                project.projectNumber?.contains(query, ignoreCase = true) == true ||
                project.clientName?.contains(query, ignoreCase = true) == true
            }
            .take(limit)
    }

    // ===== Project Team Management =====

    suspend fun assignProjectManager(
        projectId: String,
        projectManagerId: String
    ): Result<Unit> {
        return try {
            val project = projects[projectId]
                ?: return Result.failure(IllegalArgumentException("Project not found"))

            projects[projectId] = project.copy(
                projectManagerId = projectManagerId
            )
            emitProjectsUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun assignSuperintendent(
        projectId: String,
        superintendentId: String
    ): Result<Unit> {
        return try {
            val project = projects[projectId]
                ?: return Result.failure(IllegalArgumentException("Project not found"))

            projects[projectId] = project.copy(
                superintendentId = superintendentId
            )
            emitProjectsUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectsByManager(
        projectManagerId: String,
        status: ProjectStatus
    ): List<Project> {
        return projects.values.filter {
            it.projectManagerId == projectManagerId && it.status == status.name
        }
    }

    // ===== Project Information =====

    suspend fun getProjectClientInfo(projectId: String): ProjectClientInfo? {
        return projects[projectId]?.let { project ->
            ProjectClientInfo(
                clientName = project.clientName,
                clientContact = project.clientContact,
                clientPhone = project.clientPhone,
                clientEmail = project.clientEmail
            )
        }
    }

    suspend fun updateProjectClientInfo(
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
            emitProjectsUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProjectLocationInfo(projectId: String): ProjectLocationInfo? {
        return projects[projectId]?.let { project ->
            ProjectLocationInfo(
                streetAddress = project.streetAddress,
                city = project.city,
                state = project.state,
                zip = project.zip,
                fullAddress = buildString {
                    project.streetAddress?.let { append(it) }
                    project.city?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                    project.state?.let {
                        if (isNotEmpty()) append(", ")
                        append(it)
                    }
                    project.zip?.let {
                        if (isNotEmpty()) append(" ")
                        append(it)
                    }
                }
            )
        }
    }

    suspend fun updateProjectLocationInfo(
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
                zip = zip ?: project.zip
            )
            emitProjectsUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Statistics =====

    suspend fun getProjectCountByStatus(companyId: String): Map<ProjectStatus, Int> {
        return projects.values
            .filter { it.companyId == companyId }
            .groupingBy { ProjectStatus.fromString(it.status) }
            .eachCount()
    }

    suspend fun getActiveProjectCount(companyId: String): Int {
        return projects.values.count {
            it.companyId == companyId && it.status == ProjectStatus.ACTIVE.name
        }
    }

    suspend fun getProjectStats(projectId: String): ProjectStatistics {
        // TODO: Aggregate stats from crews, PTPs, incidents
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

    fun observeProjects(
        companyId: String,
        status: ProjectStatus?
    ): Flow<List<Project>> {
        return projectsFlow.map { allProjects ->
            var filtered = allProjects.filter { it.companyId == companyId }
            status?.let { s ->
                filtered = filtered.filter { it.status == s.name }
            }
            filtered
        }
    }

    fun observeProject(projectId: String): Flow<Project?> {
        return projectsFlow.map { allProjects ->
            allProjects.find { it.id == projectId }
        }
    }

    // ===== Helper Methods =====

    private fun emitProjectsUpdate() {
        projectsFlow.value = projects.values.toList()
    }

    private fun generateId(): String {
        return "proj_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}
