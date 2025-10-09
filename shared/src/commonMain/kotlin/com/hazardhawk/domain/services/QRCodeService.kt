package com.hazardhawk.domain.services

import com.hazardhawk.data.network.ApiClient
import com.hazardhawk.FeatureFlags
import kotlinx.serialization.Serializable

/**
 * Service for QR code generation and verification
 *
 * Features:
 * - Generate QR codes for worker certifications
 * - Verify QR code validity
 * - Support for both certification and worker profile QR codes
 * - Customizable QR code parameters (size, error correction)
 *
 * QR Code Flow:
 * 1. Request QR code generation from backend
 * 2. Backend generates QR code image and returns URL
 * 3. QR code contains encrypted worker/certification data
 * 4. Scanning QR code initiates verification flow
 */
interface QRCodeService {

    /**
     * Generate QR code for a worker certification
     *
     * @param certificationId Certification ID to encode
     * @param size QR code size in pixels (default: 512)
     * @param errorCorrection Error correction level (L, M, Q, H)
     * @return Result containing QR code image URL
     */
    suspend fun generateCertificationQRCode(
        certificationId: String,
        size: Int = 512,
        errorCorrection: QRErrorCorrection = QRErrorCorrection.M
    ): Result<QRCodeResult>

    /**
     * Generate QR code for worker profile with all certifications
     *
     * @param workerProfileId Worker profile ID
     * @param size QR code size in pixels
     * @param errorCorrection Error correction level
     * @return Result containing QR code image URL
     */
    suspend fun generateWorkerProfileQRCode(
        workerProfileId: String,
        size: Int = 512,
        errorCorrection: QRErrorCorrection = QRErrorCorrection.M
    ): Result<QRCodeResult>

    /**
     * Verify QR code data
     *
     * @param qrData Scanned QR code data (URL or encrypted string)
     * @return Result containing verification details
     */
    suspend fun verifyQRCode(qrData: String): Result<QRCodeVerification>

    /**
     * Get QR code for certification (cached if available)
     *
     * @param certificationId Certification ID
     * @return Cached QR code URL if available
     */
    suspend fun getCachedQRCode(certificationId: String): String?
}

/**
 * QR code error correction levels
 * - L: Low (7% recovery)
 * - M: Medium (15% recovery) - Default
 * - Q: Quartile (25% recovery)
 * - H: High (30% recovery)
 */
enum class QRErrorCorrection {
    L, M, Q, H
}

/**
 * Result of QR code generation
 */
data class QRCodeResult(
    val qrCodeUrl: String,
    val qrData: String,
    val expiresAt: String? = null,
    val size: Int,
    val format: String = "PNG"
)

/**
 * QR code verification result
 */
data class QRCodeVerification(
    val isValid: Boolean,
    val type: QRCodeType,
    val certificationId: String? = null,
    val workerProfileId: String? = null,
    val expirationDate: String? = null,
    val holderName: String? = null,
    val certificationType: String? = null,
    val verifiedAt: String
)

/**
 * Type of QR code
 */
enum class QRCodeType {
    CERTIFICATION,
    WORKER_PROFILE,
    UNKNOWN
}

/**
 * Implementation of QRCodeService with backend integration
 */
class QRCodeServiceImpl(
    private val apiClient: ApiClient
) : QRCodeService {

    // Simple in-memory cache for QR codes
    private val qrCodeCache = mutableMapOf<String, String>()

    override suspend fun generateCertificationQRCode(
        certificationId: String,
        size: Int,
        errorCorrection: QRErrorCorrection
    ): Result<QRCodeResult> {
        if (!FeatureFlags.API_CERTIFICATION_ENABLED) {
            return Result.failure(IllegalStateException("QR code generation API is disabled"))
        }

        // Check cache first
        val cachedUrl = qrCodeCache[certificationId]
        if (cachedUrl != null) {
            return Result.success(
                QRCodeResult(
                    qrCodeUrl = cachedUrl,
                    qrData = certificationId,
                    size = size
                )
            )
        }

        return try {
            val response = apiClient.post<ApiQRCodeResponse>(
                path = "/api/certifications/$certificationId/qr-code",
                body = QRCodeRequest(
                    size = size,
                    errorCorrection = errorCorrection.name
                )
            )

            response.mapCatching { apiResponse ->
                // Cache the result
                qrCodeCache[certificationId] = apiResponse.qrCodeUrl

                QRCodeResult(
                    qrCodeUrl = apiResponse.qrCodeUrl,
                    qrData = apiResponse.qrData,
                    expiresAt = apiResponse.expiresAt,
                    size = size,
                    format = apiResponse.format ?: "PNG"
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("QR code generation failed: ${e.message}", e))
        }
    }

    override suspend fun generateWorkerProfileQRCode(
        workerProfileId: String,
        size: Int,
        errorCorrection: QRErrorCorrection
    ): Result<QRCodeResult> {
        if (!FeatureFlags.API_CERTIFICATION_ENABLED) {
            return Result.failure(IllegalStateException("QR code generation API is disabled"))
        }

        return try {
            val response = apiClient.post<ApiQRCodeResponse>(
                path = "/api/workers/$workerProfileId/qr-code",
                body = QRCodeRequest(
                    size = size,
                    errorCorrection = errorCorrection.name
                )
            )

            response.mapCatching { apiResponse ->
                QRCodeResult(
                    qrCodeUrl = apiResponse.qrCodeUrl,
                    qrData = apiResponse.qrData,
                    expiresAt = apiResponse.expiresAt,
                    size = size,
                    format = apiResponse.format ?: "PNG"
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("QR code generation failed: ${e.message}", e))
        }
    }

    override suspend fun verifyQRCode(qrData: String): Result<QRCodeVerification> {
        if (!FeatureFlags.API_CERTIFICATION_ENABLED) {
            return Result.failure(IllegalStateException("QR code verification API is disabled"))
        }

        return try {
            val response = apiClient.post<ApiQRCodeVerificationResponse>(
                path = "/api/qr-codes/verify",
                body = mapOf("qrData" to qrData)
            )

            response.mapCatching { apiResponse ->
                QRCodeVerification(
                    isValid = apiResponse.isValid,
                    type = when (apiResponse.type) {
                        "certification" -> QRCodeType.CERTIFICATION
                        "worker_profile" -> QRCodeType.WORKER_PROFILE
                        else -> QRCodeType.UNKNOWN
                    },
                    certificationId = apiResponse.certificationId,
                    workerProfileId = apiResponse.workerProfileId,
                    expirationDate = apiResponse.expirationDate,
                    holderName = apiResponse.holderName,
                    certificationType = apiResponse.certificationType,
                    verifiedAt = apiResponse.verifiedAt
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("QR code verification failed: ${e.message}", e))
        }
    }

    override suspend fun getCachedQRCode(certificationId: String): String? {
        return qrCodeCache[certificationId]
    }
}

// ===== API DTOs =====

@Serializable
private data class QRCodeRequest(
    val size: Int,
    val errorCorrection: String
)

@Serializable
private data class ApiQRCodeResponse(
    val qrCodeUrl: String,
    val qrData: String,
    val expiresAt: String?,
    val format: String?
)

@Serializable
private data class ApiQRCodeVerificationResponse(
    val isValid: Boolean,
    val type: String,
    val certificationId: String?,
    val workerProfileId: String?,
    val expirationDate: String?,
    val holderName: String?,
    val certificationType: String?,
    val verifiedAt: String
)
