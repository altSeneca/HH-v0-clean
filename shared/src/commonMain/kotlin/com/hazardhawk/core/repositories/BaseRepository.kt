package com.hazardhawk.core.repositories

import kotlinx.coroutines.flow.Flow

/**
 * Base repository interface defining common patterns
 */
interface BaseRepository<T, ID> {
    suspend fun save(entity: T): Result<T>
    suspend fun findById(id: ID): T?
    suspend fun findAll(): Flow<List<T>>
    suspend fun delete(id: ID): Result<Unit>
    suspend fun update(entity: T): Result<T>
}

/**
 * Common error handling patterns
 */
sealed class RepositoryError : Exception() {
    data class NotFound(val id: String) : RepositoryError()
    data class ValidationError(val errors: List<String>) : RepositoryError()
    data class NetworkError(override val cause: Throwable) : RepositoryError()
    data class DatabaseError(override val cause: Throwable) : RepositoryError()
    data class UnauthorizedError(override val message: String) : RepositoryError()
}

/**
 * Standardized pagination
 */
data class PageRequest(
    val page: Int,
    val size: Int,
    val sortBy: String? = null,
    val sortDirection: SortDirection = SortDirection.ASC
)

data class PageResult<T>(
    val content: List<T>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

enum class SortDirection { ASC, DESC }

/**
 * Standardized sync patterns
 */
data class SyncResult(
    val syncedItems: Int,
    val conflictsResolved: Int,
    val errors: List<String>,
    val syncTimestamp: Long
)

interface SyncableRepository<T> {
    suspend fun sync(): Result<SyncResult>
    suspend fun getPendingSyncItems(): List<T>
    suspend fun markAsSynced(ids: List<String>): Result<Unit>
}

/**
 * Cache management patterns
 */
enum class CacheLevel { L1_MEMORY, L2_DATABASE, L3_NETWORK }

data class CacheStatistics(
    val l1MemoryCacheSize: Int,
    val l1HitRate: Double,
    val l1MissRate: Double,
    val l2DatabaseQueryCount: Long,
    val l2HitRate: Double,
    val l3NetworkRequestCount: Long,
    val l3HitRate: Double,
    val averageResponseTime: Long,
    val cacheEvictionCount: Long
)

interface CacheableRepository {
    suspend fun clearCache(level: CacheLevel)
    suspend fun clearAllCaches()
    suspend fun getCacheStats(): CacheStatistics
    suspend fun preloadCache()
}
