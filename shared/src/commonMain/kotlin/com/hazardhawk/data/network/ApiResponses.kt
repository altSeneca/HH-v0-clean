package com.hazardhawk.data.network

import kotlinx.serialization.Serializable

/**
 * Common API response wrappers
 */

/**
 * Paginated list response
 */
@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: PaginationMetadata
)

/**
 * Pagination metadata
 */
@Serializable
data class PaginationMetadata(
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * Single item response wrapper
 */
@Serializable
data class ApiResponse<T>(
    val data: T,
    val message: String? = null
)

/**
 * Error response
 */
@Serializable
data class ApiError(
    val error: String,
    val message: String,
    val code: String? = null,
    val details: Map<String, String>? = null
)

/**
 * Success response without data
 */
@Serializable
data class SuccessResponse(
    val success: Boolean,
    val message: String? = null
)

/**
 * QR Code generation response
 */
@Serializable
data class QRCodeResponse(
    val qrCodeUrl: String,
    val qrCodeData: String,
    val expiresAt: String? = null
)

/**
 * Assignment response
 */
@Serializable
data class AssignmentResponse(
    val assignmentId: String,
    val crewId: String,
    val projectId: String,
    val assignedAt: String,
    val assignedBy: String
)
