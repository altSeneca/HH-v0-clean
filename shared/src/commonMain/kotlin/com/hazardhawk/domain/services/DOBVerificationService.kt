package com.hazardhawk.domain.services

import com.hazardhawk.data.network.ApiClient
import com.hazardhawk.FeatureFlags
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Service for manual Date of Birth verification
 *
 * Used in the web certification portal to verify worker identity
 * through DOB matching before approving certifications.
 *
 * Verification Flow:
 * 1. Worker scans QR code on web portal
 * 2. Portal prompts for Date of Birth
 * 3. DOB is verified against worker profile
 * 4. On success, certification can be approved
 * 5. Failed attempts are logged for security
 */
interface DOBVerificationService {

    /**
     * Verify worker's date of birth
     *
     * @param workerProfileId Worker profile ID
     * @param dateOfBirth Date of birth to verify
     * @return Result containing verification result
     */
    suspend fun verifyDOB(
        workerProfileId: String,
        dateOfBirth: LocalDate
    ): Result<DOBVerificationResult>

    /**
     * Verify DOB with retry limit checking
     *
     * @param workerProfileId Worker profile ID
     * @param dateOfBirth Date of birth to verify
     * @param sessionId Verification session ID (from web portal)
     * @return Result containing verification result with retry info
     */
    suspend fun verifyDOBWithRetryLimit(
        workerProfileId: String,
        dateOfBirth: LocalDate,
        sessionId: String
    ): Result<DOBVerificationResult>

    /**
     * Get remaining verification attempts for a session
     *
     * @param sessionId Verification session ID
     * @return Number of remaining attempts (max 3)
     */
    suspend fun getRemainingAttempts(sessionId: String): Int

    /**
     * Lock verification for worker (after too many failed attempts)
     *
     * @param workerProfileId Worker profile ID
     * @param duration Lock duration in minutes
     * @return Result indicating success/failure
     */
    suspend fun lockVerification(
        workerProfileId: String,
        duration: Int = 30
    ): Result<Unit>

    /**
     * Check if worker verification is locked
     *
     * @param workerProfileId Worker profile ID
     * @return True if locked, false otherwise
     */
    suspend fun isVerificationLocked(workerProfileId: String): Boolean
}

/**
 * DOB verification result
 */
data class DOBVerificationResult(
    val isValid: Boolean,
    val workerName: String? = null,
    val remainingAttempts: Int? = null,
    val isLocked: Boolean = false,
    val lockExpiresAt: String? = null,
    val verifiedAt: String? = null,
    val sessionId: String? = null
)

/**
 * Implementation of DOBVerificationService with backend integration
 */
class DOBVerificationServiceImpl(
    private val apiClient: ApiClient
) : DOBVerificationService {

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val LOCK_DURATION_MINUTES = 30
    }

    override suspend fun verifyDOB(
        workerProfileId: String,
        dateOfBirth: LocalDate
    ): Result<DOBVerificationResult> {
        if (!FeatureFlags.API_CERTIFICATION_ENABLED) {
            return Result.failure(IllegalStateException("DOB verification API is disabled"))
        }

        return try {
            val response = apiClient.post<ApiDOBVerificationResponse>(
                path = "/api/certifications/verify-dob",
                body = DOBVerificationRequest(
                    workerProfileId = workerProfileId,
                    dateOfBirth = dateOfBirth.toString()
                )
            )

            response.mapCatching { apiResponse ->
                DOBVerificationResult(
                    isValid = apiResponse.isValid,
                    workerName = apiResponse.workerName,
                    verifiedAt = apiResponse.verifiedAt,
                    isLocked = apiResponse.isLocked ?: false,
                    lockExpiresAt = apiResponse.lockExpiresAt
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("DOB verification failed: ${e.message}", e))
        }
    }

    override suspend fun verifyDOBWithRetryLimit(
        workerProfileId: String,
        dateOfBirth: LocalDate,
        sessionId: String
    ): Result<DOBVerificationResult> {
        if (!FeatureFlags.API_CERTIFICATION_ENABLED) {
            return Result.failure(IllegalStateException("DOB verification API is disabled"))
        }

        return try {
            val response = apiClient.post<ApiDOBVerificationResponse>(
                path = "/api/certifications/verify-dob-session",
                body = DOBVerificationWithSessionRequest(
                    workerProfileId = workerProfileId,
                    dateOfBirth = dateOfBirth.toString(),
                    sessionId = sessionId
                )
            )

            response.mapCatching { apiResponse ->
                DOBVerificationResult(
                    isValid = apiResponse.isValid,
                    workerName = apiResponse.workerName,
                    remainingAttempts = apiResponse.remainingAttempts,
                    isLocked = apiResponse.isLocked ?: false,
                    lockExpiresAt = apiResponse.lockExpiresAt,
                    verifiedAt = apiResponse.verifiedAt,
                    sessionId = sessionId
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("DOB verification failed: ${e.message}", e))
        }
    }

    override suspend fun getRemainingAttempts(sessionId: String): Int {
        if (!FeatureFlags.API_CERTIFICATION_ENABLED) {
            return MAX_ATTEMPTS
        }

        return try {
            val response = apiClient.get<ApiRemainingAttemptsResponse>(
                path = "/api/certifications/verification-session/$sessionId/attempts"
            )

            response.getOrNull()?.remainingAttempts ?: MAX_ATTEMPTS
        } catch (e: Exception) {
            MAX_ATTEMPTS
        }
    }

    override suspend fun lockVerification(
        workerProfileId: String,
        duration: Int
    ): Result<Unit> {
        if (!FeatureFlags.API_CERTIFICATION_ENABLED) {
            return Result.failure(IllegalStateException("Lock verification API is disabled"))
        }

        return try {
            apiClient.post<Unit>(
                path = "/api/certifications/lock-verification",
                body = LockVerificationRequest(
                    workerProfileId = workerProfileId,
                    durationMinutes = duration
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Lock verification failed: ${e.message}", e))
        }
    }

    override suspend fun isVerificationLocked(workerProfileId: String): Boolean {
        if (!FeatureFlags.API_CERTIFICATION_ENABLED) {
            return false
        }

        return try {
            val response = apiClient.get<ApiVerificationLockStatus>(
                path = "/api/certifications/verification-lock-status/$workerProfileId"
            )

            response.getOrNull()?.isLocked ?: false
        } catch (e: Exception) {
            false
        }
    }
}

// ===== API DTOs =====

@Serializable
private data class DOBVerificationRequest(
    val workerProfileId: String,
    val dateOfBirth: String
)

@Serializable
private data class DOBVerificationWithSessionRequest(
    val workerProfileId: String,
    val dateOfBirth: String,
    val sessionId: String
)

@Serializable
private data class LockVerificationRequest(
    val workerProfileId: String,
    val durationMinutes: Int
)

@Serializable
private data class ApiDOBVerificationResponse(
    val isValid: Boolean,
    val workerName: String?,
    val remainingAttempts: Int?,
    val isLocked: Boolean?,
    val lockExpiresAt: String?,
    val verifiedAt: String?
)

@Serializable
private data class ApiRemainingAttemptsResponse(
    val remainingAttempts: Int
)

@Serializable
private data class ApiVerificationLockStatus(
    val isLocked: Boolean,
    val lockExpiresAt: String?
)
