package com.hazardhawk.domain.repositories

import com.hazardhawk.models.crew.Project

/**
 * Repository for project data access
 */
interface ProjectRepository {
    /**
     * Get project by ID with optional related data
     * 
     * @param projectId The project ID
     * @param includeCompany Include company details in the result
     * @param includeManagers Include project manager and superintendent details
     */
    suspend fun getProject(
        projectId: String,
        includeCompany: Boolean = false,
        includeManagers: Boolean = false
    ): Project?
}
