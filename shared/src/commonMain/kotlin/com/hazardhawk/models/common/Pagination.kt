package com.hazardhawk.models.common

import kotlinx.serialization.Serializable

/**
 * Cursor-based pagination request parameters.
 * Supports efficient pagination for large datasets.
 */
@Serializable
data class PaginationRequest(
    val cursor: String? = null,
    val pageSize: Int = 20,
    val sortBy: String? = null,
    val sortDirection: SortDirection = SortDirection.ASC
)

/**
 * Sort direction for pagination queries.
 */
@Serializable
enum class SortDirection {
    ASC,
    DESC
}

/**
 * Paginated result wrapper with cursor-based pagination.
 */
@Serializable
data class PaginatedResult<T>(
    val data: List<T>,
    val pagination: PaginationInfo
) {
    companion object {
        fun <T> empty(): PaginatedResult<T> {
            return PaginatedResult(
                data = emptyList(),
                pagination = PaginationInfo(
                    nextCursor = null,
                    hasMore = false,
                    totalCount = 0
                )
            )
        }
    }
}

/**
 * Pagination metadata for cursor-based pagination.
 */
@Serializable
data class PaginationInfo(
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val totalCount: Int = 0
)
