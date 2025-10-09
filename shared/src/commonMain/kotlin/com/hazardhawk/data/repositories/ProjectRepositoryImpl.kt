package com.hazardhawk.data.repositories

import com.hazardhawk.domain.repositories.*
import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.UUID

/**
 * Default implementation of ProjectRepository.
 * This is a basic in-memory implementation for demonstration.
 *
 * TODO: Replace with actual database implementation (SQLDelight, Room, etc.)
 */
class ProjectRepositoryImpl : ProjectRepository {

    // In-memory storage for demo purposes - replace with actual database
    private val projects = mutableMapOf<String, Project>()
    
    // ===== Core CRUD Operations =====

    override suspend fun createProject(
        companyId: String,
        name: String,
        projectNumber: String?,
        startDate: kotlinx.datetime.LocalDate,
        endDate: kotlinx.datetime.LocalDate?,
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
    
    override suspend fun getProject(projectId: String): Project? {
        return projects[projectId]
    }
    
    override suspend fun updateProject(project: Project): Result<Project> {
        return if (projects.containsKey(project.id)) {
            try {
                val updatedProject = project.copy(updatedAt = Clock.System.now())
                projects[project.id] = updatedProject
                
                recordProjectActivity(
                    project.id,
                    "Project updated",
                    "system",
                    mapOf("projectName" to project.name)
                )
                
                Result.success(updatedProject)
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(IllegalArgumentException("Project not found: ${project.id}"))
        }
    }
    
    override suspend fun deleteProject(projectId: String): Result<ProjectDeletionSummary> {
        return try {
            val project = projects[projectId] ?: return Result.failure(
                IllegalArgumentException("Project not found: $projectId")
            )
            
            // Count items to be deleted
            val membersCount = projectMembers[projectId]?.size ?: 0
            val activitiesCount = activities.count { it.projectId == projectId }
            
            // Remove all project data
            projects.remove(projectId)
            projectMembers.remove(projectId)
            safetyConfigs.remove(projectId)
            analyticsSettings.remove(projectId)
            activities.removeAll { it.projectId == projectId }
            
            // TODO: Also clean up associated photos and analyses
            
            val summary = ProjectDeletionSummary(
                projectId = projectId,
                photosDeleted = 0, // TODO: Count actual photos
                analysesDeleted = 0, // TODO: Count actual analyses
                activitiesDeleted = activitiesCount,
                memberRemoved = membersCount
            )
            
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun archiveProject(projectId: String, reason: String): Result<Unit> {
        return try {
            val project = projects[projectId] ?: return Result.failure(
                IllegalArgumentException("Project not found: $projectId")
            )
            
            projects[projectId] = project.copy(
                isArchived = true,
                archivedAt = Clock.System.now(),
                archivalReason = reason,
                updatedAt = Clock.System.now()
            )
            
            recordProjectActivity(projectId, "Project archived: $reason", "system")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun restoreProject(projectId: String): Result<Unit> {
        return try {
            val project = projects[projectId] ?: return Result.failure(
                IllegalArgumentException("Project not found: $projectId")
            )
            
            projects[projectId] = project.copy(
                isArchived = false,
                archivedAt = null,
                archivalReason = null,
                updatedAt = Clock.System.now()
            )
            
            recordProjectActivity(projectId, "Project restored from archive", "system")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getProjectsByOrganization(
        organizationId: String,
        includeArchived: Boolean
    ): Flow<List<Project>> {
        val filtered = projects.values.filter { project ->
            project.organizationId == organizationId &&
            (includeArchived || !project.isArchived)
        }
        return flowOf(filtered)
    }
    
    override suspend fun getProjectsByUser(
        userId: String,
        includeArchived: Boolean
    ): Flow<List<Project>> {
        // Find projects where user is a member
        val userProjectIds = projectMembers.entries
            .filter { (_, members) -> members.any { it.userId == userId } }
            .map { it.key }
            .toSet()
        
        val filtered = projects.values.filter { project ->
            project.id in userProjectIds &&
            (includeArchived || !project.isArchived)
        }
        return flowOf(filtered)
    }
    
    override suspend fun getProjectsByWorkType(workType: WorkType): Flow<List<Project>> {
        val filtered = projects.values.filter { it.workType == workType && !it.isArchived }
        return flowOf(filtered)
    }
    
    override suspend fun getProjectsInRegion(
        centerLatitude: Double,
        centerLongitude: Double,
        radiusKm: Double
    ): Flow<List<Project>> {
        val filtered = projects.values.filter { project ->
            !project.isArchived &&
            project.location.latitude != null &&
            project.location.longitude != null &&
            calculateDistance(
                centerLatitude, centerLongitude,
                project.location.latitude!!, project.location.longitude!!
            ) <= radiusKm
        }
        return flowOf(filtered)
    }
    
    override suspend fun searchProjects(query: String, organizationId: String?): Flow<List<Project>> {
        val filtered = projects.values.filter { project ->
            !project.isArchived &&
            (organizationId == null || project.organizationId == organizationId) &&
            (project.name.contains(query, ignoreCase = true) ||
             project.description?.contains(query, ignoreCase = true) == true ||
             project.location.address.contains(query, ignoreCase = true))
        }
        return flowOf(filtered)
    }
    
    override suspend fun getActiveProjects(organizationId: String?): Flow<List<Project>> {
        val filtered = projects.values.filter { project ->
            !project.isArchived &&
            project.status != ProjectStatus.CANCELLED &&
            project.status != ProjectStatus.COMPLETED &&
            (organizationId == null || project.organizationId == organizationId)
        }
        return flowOf(filtered)
    }
    
    override suspend fun getProjectsByStatus(
        status: ProjectStatus,
        organizationId: String?
    ): Flow<List<Project>> {
        val filtered = projects.values.filter { project ->
            project.status == status &&
            (organizationId == null || project.organizationId == organizationId)
        }
        return flowOf(filtered)
    }
    
    override suspend fun getProjectTeam(projectId: String): List<ProjectMember> {
        return projectMembers[projectId]?.toList() ?: emptyList()
    }
    
    override suspend fun addProjectMember(
        projectId: String,
        userId: String,
        role: ProjectRole
    ): Result<Unit> {
        return try {
            val members = projectMembers.getOrPut(projectId) { mutableListOf() }
            
            // Check if user is already a member
            if (members.any { it.userId == userId }) {
                return Result.failure(IllegalArgumentException("User is already a project member"))
            }
            
            val member = ProjectMember(
                userId = userId,
                userEmail = "user@example.com", // TODO: Get actual user email
                userFullName = "User $userId", // TODO: Get actual user name
                role = role,
                addedAt = Clock.System.now(),
                addedBy = "system" // TODO: Get actual user who added
            )
            
            members.add(member)
            recordProjectActivity(
                projectId,
                "Team member added: $userId as $role",
                "system"
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateProjectMemberRole(
        projectId: String,
        userId: String,
        newRole: ProjectRole
    ): Result<Unit> {
        return try {
            val members = projectMembers[projectId] ?: return Result.failure(
                IllegalArgumentException("Project not found: $projectId")
            )
            
            val memberIndex = members.indexOfFirst { it.userId == userId }
            if (memberIndex == -1) {
                return Result.failure(IllegalArgumentException("User not found in project: $userId"))
            }
            
            val oldRole = members[memberIndex].role
            members[memberIndex] = members[memberIndex].copy(role = newRole)
            
            recordProjectActivity(
                projectId,
                "Member role updated: $userId from $oldRole to $newRole",
                "system"
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeProjectMember(projectId: String, userId: String): Result<Unit> {
        return try {
            val members = projectMembers[projectId] ?: return Result.failure(
                IllegalArgumentException("Project not found: $projectId")
            )
            
            val removed = members.removeAll { it.userId == userId }
            if (!removed) {
                return Result.failure(IllegalArgumentException("User not found in project: $userId"))
            }
            
            recordProjectActivity(
                projectId,
                "Team member removed: $userId",
                "system"
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getProjectsByUserRole(
        userId: String,
        role: ProjectRole
    ): Flow<List<Project>> {
        val projectIds = projectMembers.entries
            .filter { (_, members) -> 
                members.any { it.userId == userId && it.role == role }
            }
            .map { it.key }
            .toSet()
        
        val filtered = projects.values.filter { project ->
            project.id in projectIds && !project.isArchived
        }
        return flowOf(filtered)
    }
    
    override suspend fun getProjectSafetyConfig(projectId: String): ProjectSafetyConfig? {
        return safetyConfigs[projectId]
    }
    
    override suspend fun updateProjectSafetyConfig(
        projectId: String,
        config: ProjectSafetyConfig
    ): Result<Unit> {
        return try {
            safetyConfigs[projectId] = config.copy(
                projectId = projectId,
                updatedAt = Clock.System.now()
            )
            recordProjectActivity(projectId, "Safety configuration updated", "system")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getProjectAnalyticsSettings(projectId: String): ProjectAnalyticsSettings {
        return analyticsSettings[projectId] ?: ProjectAnalyticsSettings(
            projectId = projectId,
            updatedAt = Clock.System.now()
        )
    }
    
    override suspend fun updateProjectAnalyticsSettings(
        projectId: String,
        settings: ProjectAnalyticsSettings
    ): Result<Unit> {
        return try {
            analyticsSettings[projectId] = settings.copy(
                projectId = projectId,
                updatedAt = Clock.System.now()
            )
            recordProjectActivity(projectId, "Analytics settings updated", "system")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getProjectStats(
        projectId: String,
        dateRange: DateRange?
    ): ProjectStatistics {
        val project = projects[projectId]
        val teamSize = projectMembers[projectId]?.size ?: 0
        val lastActivity = activities
            .filter { it.projectId == projectId }
            .maxByOrNull { it.timestamp }?.timestamp
        
        // TODO: Get actual photo and analysis counts from respective repositories
        return ProjectStatistics(
            projectId = projectId,
            totalPhotos = 0,
            analyzedPhotos = 0,
            hazardCount = 0,
            safetyViolations = 0,
            averageConfidence = 0f,
            mostCommonHazards = emptyList(),
            teamMemberCount = teamSize,
            lastActivity = lastActivity
        )
    }
    
    override suspend fun getProjectSafetyMetrics(
        projectId: String,
        dateRange: DateRange
    ): ProjectSafetyMetrics {
        // TODO: Calculate actual safety metrics from analysis data
        return ProjectSafetyMetrics(
            projectId = projectId,
            dateRange = dateRange,
            incidentRate = 0f,
            nearMissCount = 0,
            complianceScore = 0f,
            improvementTrends = emptyMap(),
            riskAssessment = "No assessment available"
        )
    }
    
    override suspend fun getProjectActivityTimeline(projectId: String, limit: Int): List<ProjectActivity> {
        return activities
            .filter { it.projectId == projectId }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    override suspend fun recordProjectActivity(
        projectId: String,
        activity: String,
        userId: String,
        metadata: Map<String, String>?
    ): Result<Unit> {
        return try {
            val activityRecord = ProjectActivity(
                id = "activity_${Clock.System.now().toEpochMilliseconds()}",
                projectId = projectId,
                activity = activity,
                userId = userId,
                userFullName = if (userId == "system") "System" else "User $userId",
                metadata = metadata ?: emptyMap(),
                timestamp = Clock.System.now()
            )
            activities.add(activityRecord)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun createProjectsBatch(projects: List<Project>): Result<Int> {
        return try {
            var created = 0
            projects.forEach { project ->
                createProject(project).onSuccess { created++ }
            }
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateProjectStatusesBatch(
        updates: Map<String, ProjectStatus>
    ): Result<Unit> {
        return try {
            updates.forEach { (projectId, status) ->
                val project = projects[projectId]
                if (project != null) {
                    projects[projectId] = project.copy(
                        status = status,
                        updatedAt = Clock.System.now()
                    )
                    recordProjectActivity(
                        projectId,
                        "Status updated to $status",
                        "system"
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getProjectStatsBatch(
        projectIds: List<String>,
        dateRange: DateRange?
    ): Map<String, ProjectStatistics> {
        return projectIds.associateWith { projectId ->
            getProjectStats(projectId, dateRange)
        }
    }
    
    override suspend fun cleanupOldProjectData(retentionDays: Int): Result<DataCleanupSummary> {
        return try {
            val cutoffDate = Clock.System.now().minus(
                retentionDays.toLong() * 24 * 60 * 60 * 1000,
                kotlinx.datetime.DateTimeUnit.MILLISECOND
            )
            
            val oldActivities = activities.filter { it.timestamp < cutoffDate }
            activities.removeAll { it.timestamp < cutoffDate }
            
            // TODO: Clean up old photos and analyses
            
            val summary = DataCleanupSummary(
                projectsProcessed = projects.size,
                photosDeleted = 0, // TODO: Count actual photos
                analysesDeleted = 0, // TODO: Count actual analyses
                activitiesDeleted = oldActivities.size,
                spaceSavedBytes = oldActivities.size * 1024L // Estimated
            )
            
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun optimizeStorage(): Result<Unit> {
        // Placeholder for storage optimization
        return Result.success(Unit)
    }
    
    override suspend fun getStorageStats(organizationId: String?): ProjectStorageStats {
        val filteredProjects = if (organizationId != null) {
            projects.values.filter { it.organizationId == organizationId }
        } else {
            projects.values
        }
        
        val activeCount = filteredProjects.count { !it.isArchived }
        val archivedCount = filteredProjects.count { it.isArchived }
        val oldestProject = filteredProjects.minByOrNull { it.createdAt }?.createdAt
        val newestProject = filteredProjects.maxByOrNull { it.createdAt }?.createdAt
        
        return ProjectStorageStats(
            totalProjects = filteredProjects.size,
            activeProjects = activeCount,
            archivedProjects = archivedCount,
            totalStorageBytes = filteredProjects.size * 10240L, // Estimated
            averageProjectSize = if (filteredProjects.isNotEmpty()) 10240L else 0L,
            largestProject = filteredProjects.maxByOrNull { it.name.length }?.name,
            oldestProject = oldestProject,
            newestProject = newestProject
        )
    }
    
    override suspend fun exportProjectData(
        projectId: String,
        includePhotos: Boolean
    ): Result<ProjectExportData> {
        return try {
            val project = projects[projectId] ?: return Result.failure(
                IllegalArgumentException("Project not found: $projectId")
            )
            
            val exportData = ProjectExportData(
                project = project,
                team = getProjectTeam(projectId),
                safetyConfig = getProjectSafetyConfig(projectId) ?: ProjectSafetyConfig(
                    projectId = projectId,
                    requiredPpe = emptyList(),
                    hazardTypes = emptyList(),
                    oshaStandards = emptyList(),
                    customSafetyRules = emptyList(),
                    emergencyProcedures = null,
                    incidentReportingProcess = null,
                    updatedAt = Clock.System.now()
                ),
                analyticsSettings = getProjectAnalyticsSettings(projectId),
                activities = getProjectActivityTimeline(projectId, 1000),
                statistics = getProjectStats(projectId),
                exportedAt = Clock.System.now()
            )
            
            Result.success(exportData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun importProjectData(
        exportData: ProjectExportData,
        organizationId: String
    ): Result<Project> {
        return try {
            val project = exportData.project.copy(
                organizationId = organizationId,
                createdAt = Clock.System.now(),
                updatedAt = Clock.System.now()
            )
            
            // Create the project and associated data
            createProject(project).getOrThrow()
            updateProjectSafetyConfig(project.id, exportData.safetyConfig)
            updateProjectAnalyticsSettings(project.id, exportData.analyticsSettings)
            
            // Add team members
            exportData.team.forEach { member ->
                addProjectMember(project.id, member.userId, member.role)
            }
            
            Result.success(project)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helper function to calculate distance between two points
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        // Haversine formula for distance calculation
        val earthRadius = 6371.0 // Earth radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return earthRadius * c
    }
}