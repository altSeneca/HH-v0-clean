package com.hazardhawk.data.repositories

import com.hazardhawk.domain.repositories.*
import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.uuid.uuid4

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

    override suspend fun getProject(
        projectId: String,
        includeCompany: Boolean,
        includeManagers: Boolean
    ): Project? {
        return projects[projectId]
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
