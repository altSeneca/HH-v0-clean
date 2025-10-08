package com.hazardhawk.domain.repositories

import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository for crew management with support for crew creation, member assignment,
 * and roster generation.
 */
interface CrewRepository {

    // ===== Core CRUD Operations =====

    /**
     * Create a new crew
     */
    suspend fun createCrew(
        companyId: String,
        request: CreateCrewRequest
    ): Result<Crew>

    /**
     * Get crew by ID with optional related data
     */
    suspend fun getCrew(
        crewId: String,
        includeMembers: Boolean = true,
        includeForeman: Boolean = true,
        includeProject: Boolean = false
    ): Crew?

    /**
     * Update crew information
     */
    suspend fun updateCrew(
        crewId: String,
        request: UpdateCrewRequest
    ): Result<Crew>

    /**
     * Delete crew (set status to DISBANDED)
     */
    suspend fun deleteCrew(crewId: String): Result<Unit>

    // ===== Crew Queries =====

    /**
     * Get all crews for a company
     */
    suspend fun getCrews(
        companyId: String,
        projectId: String? = null,
        status: CrewStatus = CrewStatus.ACTIVE,
        pagination: PaginationRequest = PaginationRequest()
    ): PaginatedResult<Crew>

    /**
     * Get crews by project
     */
    suspend fun getCrewsByProject(
        projectId: String,
        status: CrewStatus = CrewStatus.ACTIVE
    ): List<Crew>

    /**
     * Get crews by type
     */
    suspend fun getCrewsByType(
        companyId: String,
        crewType: CrewType,
        status: CrewStatus = CrewStatus.ACTIVE
    ): List<Crew>

    /**
     * Search crews by name
     */
    suspend fun searchCrews(
        companyId: String,
        query: String,
        limit: Int = 20
    ): List<Crew>

    // ===== Member Management =====

    /**
     * Add workers to a crew
     */
    suspend fun addCrewMembers(
        crewId: String,
        workerIds: List<String>,
        role: CrewMemberRole = CrewMemberRole.MEMBER,
        startDate: kotlinx.datetime.LocalDate
    ): Result<Unit>

    /**
     * Remove workers from a crew
     */
    suspend fun removeCrewMembers(
        crewId: String,
        workerIds: List<String>
    ): Result<Unit>

    /**
     * Update crew member role
     */
    suspend fun updateCrewMemberRole(
        crewId: String,
        workerId: String,
        newRole: CrewMemberRole
    ): Result<Unit>

    /**
     * Get crew members
     */
    suspend fun getCrewMembers(
        crewId: String,
        includeWorkerDetails: Boolean = true
    ): List<CrewMember>

    /**
     * Get worker's crew assignments
     */
    suspend fun getWorkerCrews(workerId: String): List<CrewMembership>

    // ===== Roster Operations =====

    /**
     * Get formatted crew roster for sign-in sheets
     */
    suspend fun getCrewRoster(
        crewId: String,
        date: kotlinx.datetime.LocalDate,
        includeCertifications: Boolean = true
    ): CrewRoster

    /**
     * Get crew members eligible to be foreman
     */
    suspend fun getPotentialForemen(crewId: String): List<CompanyWorker>

    // ===== Statistics =====

    /**
     * Get crew count by status
     */
    suspend fun getCrewCountByStatus(companyId: String): Map<CrewStatus, Int>

    /**
     * Get crew count by type
     */
    suspend fun getCrewCountByType(companyId: String): Map<CrewType, Int>

    /**
     * Get total active crews
     */
    suspend fun getActiveCrewCount(companyId: String): Int

    // ===== Reactive Queries =====

    /**
     * Observe crews as a Flow for reactive updates
     */
    fun observeCrews(
        companyId: String,
        projectId: String? = null
    ): Flow<List<Crew>>

    /**
     * Observe a single crew
     */
    fun observeCrew(crewId: String): Flow<Crew?>

    /**
     * Observe crew members
     */
    fun observeCrewMembers(crewId: String): Flow<List<CrewMember>>
}

/**
 * Crew roster for sign-in sheets and documentation
 */
data class CrewRoster(
    val crewId: String,
    val crewName: String,
    val date: kotlinx.datetime.LocalDate,
    val foremanName: String?,
    val location: String?,
    val members: List<CrewRosterMember>
)

data class CrewRosterMember(
    val employeeNumber: String,
    val name: String,
    val role: String,
    val certifications: List<String> = emptyList(),
    val photoUrl: String? = null
)
