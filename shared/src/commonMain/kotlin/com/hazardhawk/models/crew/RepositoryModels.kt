package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

/**
 * Pagination request parameters
 */
@Serializable
data class PaginationRequest(
    val page: Int = 1,
    val pageSize: Int = 20,
    val sortBy: String? = null,
    val sortDirection: SortDirection = SortDirection.DESC
)

/**
 * Sort direction
 */
@Serializable
enum class SortDirection {
    ASC,
    DESC
}

/**
 * Paginated result wrapper
 */
@Serializable
data class PaginatedResult<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun <T> empty(): PaginatedResult<T> {
            return PaginatedResult(
                data = emptyList(),
                page = 1,
                pageSize = 20,
                totalCount = 0,
                totalPages = 0,
                hasNext = false,
                hasPrevious = false
            )
        }
    }
}
