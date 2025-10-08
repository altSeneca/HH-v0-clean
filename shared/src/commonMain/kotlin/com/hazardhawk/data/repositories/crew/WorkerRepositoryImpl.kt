package com.hazardhawk.data.repositories.crew

import com.hazardhawk.domain.repositories.WorkerRepository
import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

/**
 * Implementation of WorkerRepository with in-memory storage.
 * TODO: Replace with actual API client and database integration
 */
class WorkerRepositoryImpl : WorkerRepository {

    // In-memory storage (replace with actual persistence)
    private val workers = mutableMapOf<String, CompanyWorker>()
    private val profiles = mutableMapOf<String, WorkerProfile>()
    private val workersFlow = MutableStateFlow<List<CompanyWorker>>(emptyList())

    // ===== Core CRUD Operations =====

    override suspend fun createWorker(
        companyId: String,
        request: CreateWorkerRequest
    ): Result<CompanyWorker> {
        return try {
            // Check for duplicate employee number
            val existing = workers.values.find {
                it.companyId == companyId && it.employeeNumber == request.employeeNumber
            }
            if (existing != null) {
                return Result.failure(
                    IllegalArgumentException("Worker with employee number ${request.employeeNumber} already exists")
                )
            }

            val now = Clock.System.now().toString()
            val profileId = generateId()
            val workerId = generateId()

            val profile = WorkerProfile(
                id = profileId,
                firstName = request.firstName,
                lastName = request.lastName,
                dateOfBirth = request.dateOfBirth,
                email = request.email,
                phone = request.phone,
                photoUrl = null,
                createdAt = now,
                updatedAt = now
            )

            val worker = CompanyWorker(
                id = workerId,
                companyId = companyId,
                workerProfileId = profileId,
                employeeNumber = request.employeeNumber,
                role = request.role,
                hireDate = request.hireDate,
                status = WorkerStatus.ACTIVE,
                hourlyRate = request.hourlyRate,
                permissions = emptyList(),
                metadata = request.metadata,
                createdAt = now,
                updatedAt = now,
                workerProfile = profile
            )

            profiles[profileId] = profile
            workers[workerId] = worker
            emitWorkersUpdate()

            Result.success(worker)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWorker(
        workerId: String,
        includeProfile: Boolean,
        includeCertifications: Boolean,
        includeCrews: Boolean
    ): CompanyWorker? {
        return workers[workerId]?.let { worker ->
            var result = worker

            if (includeProfile && result.workerProfile == null) {
                result = result.copy(
                    workerProfile = profiles[worker.workerProfileId]
                )
            }

            // TODO: Load certifications and crews if requested

            result
        }
    }

    override suspend fun updateWorker(
        workerId: String,
        request: UpdateWorkerRequest
    ): Result<CompanyWorker> {
        return try {
            val worker = workers[workerId]
                ?: return Result.failure(IllegalArgumentException("Worker not found: $workerId"))

            val updated = worker.copy(
                employeeNumber = request.employeeNumber ?: worker.employeeNumber,
                role = request.role ?: worker.role,
                status = request.status ?: worker.status,
                hourlyRate = request.hourlyRate ?: worker.hourlyRate,
                permissions = request.permissions ?: worker.permissions,
                metadata = request.metadata ?: worker.metadata,
                updatedAt = Clock.System.now().toString()
            )

            workers[workerId] = updated
            emitWorkersUpdate()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteWorker(workerId: String): Result<Unit> {
        return try {
            val worker = workers[workerId]
                ?: return Result.failure(IllegalArgumentException("Worker not found: $workerId"))

            workers[workerId] = worker.copy(
                status = WorkerStatus.TERMINATED,
                updatedAt = Clock.System.now().toString()
            )
            emitWorkersUpdate()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Search and Filtering =====

    override suspend fun getWorkers(
        companyId: String,
        filters: WorkerFilters,
        pagination: PaginationRequest
    ): PaginatedResult<CompanyWorker> {
        var filtered = workers.values.filter { it.companyId == companyId }

        // Apply filters
        filters.status?.let { status ->
            filtered = filtered.filter { it.status == status }
        }
        filters.role?.let { role ->
            filtered = filtered.filter { it.role == role }
        }
        filters.search?.let { search ->
            filtered = filtered.filter {
                it.employeeNumber.contains(search, ignoreCase = true) ||
                it.workerProfile?.fullName?.contains(search, ignoreCase = true) == true
            }
        }

        // Apply sorting
        val sorted = when (pagination.sortBy) {
            "name" -> filtered.sortedBy { it.workerProfile?.fullName }
            "hire_date" -> filtered.sortedBy { it.hireDate }
            "employee_number" -> filtered.sortedBy { it.employeeNumber }
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

    override suspend fun searchWorkers(
        companyId: String,
        query: String,
        filters: WorkerFilters,
        limit: Int
    ): List<CompanyWorker> {
        return workers.values
            .filter { it.companyId == companyId }
            .filter { worker ->
                worker.employeeNumber.contains(query, ignoreCase = true) ||
                worker.workerProfile?.fullName?.contains(query, ignoreCase = true) == true ||
                worker.workerProfile?.email?.contains(query, ignoreCase = true) == true
            }
            .let { filtered ->
                filters.status?.let { status ->
                    filtered.filter { it.status == status }
                } ?: filtered
            }
            .let { filtered ->
                filters.role?.let { role ->
                    filtered.filter { it.role == role }
                } ?: filtered
            }
            .take(limit)
    }

    override suspend fun getWorkersByRole(
        companyId: String,
        role: WorkerRole,
        status: WorkerStatus
    ): List<CompanyWorker> {
        return workers.values.filter {
            it.companyId == companyId && it.role == role && it.status == status
        }
    }

    override suspend fun getWorkersByStatus(
        companyId: String,
        status: WorkerStatus
    ): List<CompanyWorker> {
        return workers.values.filter {
            it.companyId == companyId && it.status == status
        }
    }

    // ===== Worker Profile Management =====

    override suspend fun getWorkerProfile(profileId: String): WorkerProfile? {
        return profiles[profileId]
    }

    override suspend fun updateWorkerProfile(
        profileId: String,
        firstName: String?,
        lastName: String?,
        email: String?,
        phone: String?,
        dateOfBirth: kotlinx.datetime.LocalDate?
    ): Result<WorkerProfile> {
        return try {
            val profile = profiles[profileId]
                ?: return Result.failure(IllegalArgumentException("Profile not found: $profileId"))

            val updated = profile.copy(
                firstName = firstName ?: profile.firstName,
                lastName = lastName ?: profile.lastName,
                email = email ?: profile.email,
                phone = phone ?: profile.phone,
                dateOfBirth = dateOfBirth ?: profile.dateOfBirth,
                updatedAt = Clock.System.now().toString()
            )

            profiles[profileId] = updated

            // Update all workers with this profile
            workers.forEach { (id, worker) ->
                if (worker.workerProfileId == profileId) {
                    workers[id] = worker.copy(workerProfile = updated)
                }
            }
            emitWorkersUpdate()

            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadWorkerPhoto(
        profileId: String,
        photoData: ByteArray,
        fileName: String
    ): Result<String> {
        // TODO: Implement S3 upload
        return Result.failure(NotImplementedError("Photo upload not yet implemented"))
    }

    // ===== Bulk Operations =====

    override suspend fun createWorkersBatch(
        companyId: String,
        workers: List<CreateWorkerRequest>
    ): Result<List<CompanyWorker>> {
        return try {
            val created = workers.mapNotNull { request ->
                createWorker(companyId, request).getOrNull()
            }
            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateWorkersBatch(
        updates: Map<String, UpdateWorkerRequest>
    ): Result<Int> {
        return try {
            var count = 0
            updates.forEach { (workerId, request) ->
                if (updateWorker(workerId, request).isSuccess) {
                    count++
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ===== Statistics =====

    override suspend fun getWorkerCountByStatus(companyId: String): Map<WorkerStatus, Int> {
        return workers.values
            .filter { it.companyId == companyId }
            .groupingBy { it.status }
            .eachCount()
    }

    override suspend fun getWorkerCountByRole(companyId: String): Map<WorkerRole, Int> {
        return workers.values
            .filter { it.companyId == companyId }
            .groupingBy { it.role }
            .eachCount()
    }

    override suspend fun getActiveWorkerCount(companyId: String): Int {
        return workers.values.count {
            it.companyId == companyId && it.status == WorkerStatus.ACTIVE
        }
    }

    // ===== Reactive Queries =====

    override fun observeWorkers(
        companyId: String,
        filters: WorkerFilters
    ): Flow<List<CompanyWorker>> {
        return workersFlow.map { allWorkers ->
            var filtered = allWorkers.filter { it.companyId == companyId }

            filters.status?.let { status ->
                filtered = filtered.filter { it.status == status }
            }
            filters.role?.let { role ->
                filtered = filtered.filter { it.role == role }
            }
            filters.search?.let { search ->
                filtered = filtered.filter {
                    it.employeeNumber.contains(search, ignoreCase = true) ||
                    it.workerProfile?.fullName?.contains(search, ignoreCase = true) == true
                }
            }

            filtered
        }
    }

    override fun observeWorker(workerId: String): Flow<CompanyWorker?> {
        return workersFlow.map { allWorkers ->
            allWorkers.find { it.id == workerId }
        }
    }

    // ===== Helper Methods =====

    private fun emitWorkersUpdate() {
        workersFlow.value = workers.values.toList()
    }

    private fun generateId(): String {
        return "id_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
}
