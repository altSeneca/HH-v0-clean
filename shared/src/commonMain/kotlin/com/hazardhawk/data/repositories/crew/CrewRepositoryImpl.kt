package com.hazardhawk.data.repositories.crew

import com.hazardhawk.domain.repositories.*
import com.hazardhawk.models.crew.*
import com.hazardhawk.models.common.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate

/**
 * Implementation of CrewRepository with in-memory storage.
 * TODO: Replace with actual API client and database integration
 */
class CrewRepositoryImpl(
    private val workerRepository: WorkerRepository
) : CrewRepository {

    // In-memory storage
    private val crews = mutableMapOf<String, Crew>()
    private val crewMembers = mutableMapOf<String, MutableList<CrewMember>>()
    private val crewsFlow = MutableStateFlow<List<Crew>>(emptyList())

    // ===== Core CRUD Operations =====

    suspend fun createCrew(
        companyId: String,
        request: CreateCrewRequest
    ): Result<Crew> {
        return try {
            val now = Clock.System.now().toString()
            val crewId = generateId()

            val crew = Crew(
                id = crewId,
                companyId = companyId,
                projectId = request.projectId,
                name = request.name,
                crewType = request.crewType,
                trade = request.trade,
                foremanId = request.foremanId,
                location = request.location,
                status = CrewStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )

            crews[crewId] = crew
            crewMembers[crewId] = mutableListOf()
            emitCrewsUpdate()

            Result.success(crew)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCrew(
        crewId: String,
        includeMembers: Boolean,
        includeForeman: Boolean,
        includeProject: Boolean
    ): Crew? {
        return crews[crewId]?.let { crew ->
            var result = crew

            if (includeMembers) {
                result = result.copy(
                    members = crewMembers[crewId] ?: emptyList()
                )
            }

            if (includeForeman && crew.foremanId != null) {
                result = result.copy(
                    foreman = workerRepository.getWorker(crew.foremanId)
                )
            }

            // TODO: Load project if requested

            result
        }
    }

    suspend fun updateCrew(
        crewId: String,
        request: UpdateCrewRequest
    ): Result<Crew> {
        return try {
            val crew = crews[crewId]
                ?: return Result.failure(IllegalArgumentException("Crew not found: $crewId"))

            val updated = crew.copy(
                name = request.name ?: crew.name,
                foremanId = request.foremanId ?: crew.foremanId,
                location = request.location ?: crew.location,
                status = request.status ?: crew.status,
                updatedAt = Clock.System.now().toString()
            )

            crews[crewId] = updated
            emitCrewsUpdate()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCrew(crewId: String): Result<Unit> {
        return try {
            val crew = crews[crewId]
                ?: return Result.failure(IllegalArgumentException("Crew not found: $crewId"))

            crews[crewId] = crew.copy(
                status = CrewStatus.DISBANDED,
                updatedAt = Clock.System.now().toString()
            )
            emitCrewsUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Crew Queries =====

    suspend fun getCrews(
        companyId: String,
        projectId: String?,
        status: CrewStatus,
        pagination: PaginationRequest
    ): PaginatedResult<Crew> {
        var filtered = crews.values.filter {
            it.companyId == companyId && it.status == status
        }

        projectId?.let { pid ->
            filtered = filtered.filter { it.projectId == pid }
        }

        // Apply sorting
        val sorted = when (pagination.sortBy) {
            "name" -> filtered.sortedBy { it.name }
            "created_at" -> filtered.sortedBy { it.createdAt }
            else -> filtered
        }

        val finalSorted = if (pagination.sortDirection == SortDirection.DESC) {
            sorted.reversed()
        } else {
            sorted
        }

        // Apply pagination
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

    suspend fun getCrewsByProject(
        projectId: String,
        status: CrewStatus
    ): List<Crew> {
        return crews.values.filter {
            it.projectId == projectId && it.status == status
        }
    }

    suspend fun getCrewsByType(
        companyId: String,
        crewType: CrewType,
        status: CrewStatus
    ): List<Crew> {
        return crews.values.filter {
            it.companyId == companyId && it.crewType == crewType && it.status == status
        }
    }

    suspend fun searchCrews(
        companyId: String,
        query: String,
        limit: Int
    ): List<Crew> {
        return crews.values
            .filter { it.companyId == companyId }
            .filter { crew ->
                crew.name.contains(query, ignoreCase = true) ||
                crew.trade?.contains(query, ignoreCase = true) == true ||
                crew.location?.contains(query, ignoreCase = true) == true
            }
            .take(limit)
    }

    // ===== Member Management =====

    suspend fun addCrewMembers(
        crewId: String,
        workerIds: List<String>,
        role: CrewMemberRole,
        startDate: LocalDate
    ): Result<Unit> {
        return try {
            val crew = crews[crewId]
                ?: return Result.failure(IllegalArgumentException("Crew not found: $crewId"))

            val members = crewMembers.getOrPut(crewId) { mutableListOf() }
            val now = Clock.System.now().toString()

            workerIds.forEach { workerId ->
                // Check if already a member
                val existing = members.find {
                    it.companyWorkerId == workerId && it.status == "active"
                }
                if (existing == null) {
                    val member = CrewMember(
                        id = generateId(),
                        crewId = crewId,
                        companyWorkerId = workerId,
                        role = role,
                        startDate = startDate,
                        endDate = null,
                        status = "active",
                        worker = workerRepository.getWorker(workerId)
                    )
                    members.add(member)
                }
            }

            emitCrewsUpdate()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeCrewMembers(
        crewId: String,
        workerIds: List<String>
    ): Result<Unit> {
        return try {
            val members = crewMembers[crewId]
                ?: return Result.failure(IllegalArgumentException("Crew not found: $crewId"))

            members.removeAll { member ->
                workerIds.contains(member.companyWorkerId)
            }

            emitCrewsUpdate()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCrewMemberRole(
        crewId: String,
        workerId: String,
        newRole: CrewMemberRole
    ): Result<Unit> {
        return try {
            val members = crewMembers[crewId]
                ?: return Result.failure(IllegalArgumentException("Crew not found: $crewId"))

            val memberIndex = members.indexOfFirst {
                it.companyWorkerId == workerId && it.status == "active"
            }

            if (memberIndex == -1) {
                return Result.failure(IllegalArgumentException("Member not found in crew"))
            }

            members[memberIndex] = members[memberIndex].copy(role = newRole)
            emitCrewsUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCrewMembers(
        crewId: String,
        includeWorkerDetails: Boolean
    ): List<CrewMember> {
        return crewMembers[crewId]?.let { members ->
            if (includeWorkerDetails) {
                members.map { member ->
                    member.copy(
                        worker = workerRepository.getWorker(member.companyWorkerId)
                    )
                }
            } else {
                members
            }
        } ?: emptyList()
    }

    suspend fun getWorkerCrews(workerId: String): List<CrewMembership> {
        return crewMembers.flatMap { (crewId, members) ->
            members.filter { it.companyWorkerId == workerId && it.status == "active" }
                .mapNotNull { member ->
                    crews[crewId]?.let { crew ->
                        CrewMembership(
                            crewId = crewId,
                            crewName = crew.name,
                            role = member.role,
                            startDate = member.startDate,
                            projectName = null // TODO: Load project name
                        )
                    }
                }
        }
    }

    // ===== Statistics =====

    suspend fun getCrewCountByStatus(companyId: String): Map<CrewStatus, Int> {
        return crews.values
            .filter { it.companyId == companyId }
            .groupingBy { it.status }
            .eachCount()
    }

    suspend fun getCrewCountByType(companyId: String): Map<CrewType, Int> {
        return crews.values
            .filter { it.companyId == companyId }
            .groupingBy { it.crewType }
            .eachCount()
    }

    suspend fun getActiveCrewCount(companyId: String): Int {
        return crews.values.count {
            it.companyId == companyId && it.status == CrewStatus.ACTIVE
        }
    }

    // ===== Reactive Queries =====

    fun observeCrews(
        companyId: String,
        projectId: String?
    ): Flow<List<Crew>> {
        return crewsFlow.map { allCrews ->
            var filtered = allCrews.filter { it.companyId == companyId }
            projectId?.let { pid ->
                filtered = filtered.filter { it.projectId == pid }
            }
            filtered
        }
    }

    fun observeCrew(crewId: String): Flow<Crew?> {
        return crewsFlow.map { allCrews ->
            allCrews.find { it.id == crewId }
        }
    }

    fun observeCrewMembers(crewId: String): Flow<List<CrewMember>> {
        return crewsFlow.map {
            crewMembers[crewId] ?: emptyList()
        }
    }

    // ===== Helper Methods =====

    private fun emitCrewsUpdate() {
        crewsFlow.value = crews.values.toList()
    }

    private fun generateId(): String {
        return "id_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}
