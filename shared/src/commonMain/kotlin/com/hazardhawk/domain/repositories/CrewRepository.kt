package com.hazardhawk.domain.repositories

import com.hazardhawk.models.crew.Crew

/**
 * Repository for crew data access
 *
 * Provides core operations for crew management.
 * Implementation may be API-backed or mock-based depending on feature flags.
 */
interface CrewRepository {
    /**
     * Get crew by ID with optional related data
     *
     * @param crewId The crew ID
     * @param includeMembers Include crew members in the result
     * @param includeForeman Include foreman details in the result
     * @param includeProject Include project details in the result
     * @return Crew if found, null otherwise
     */
    suspend fun getCrew(
        crewId: String,
        includeMembers: Boolean = false,
        includeForeman: Boolean = false,
        includeProject: Boolean = false
    ): Crew?
}
