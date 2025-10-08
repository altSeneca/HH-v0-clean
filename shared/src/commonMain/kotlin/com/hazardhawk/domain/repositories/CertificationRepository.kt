package com.hazardhawk.domain.repositories

import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Repository for certification management with support for upload, verification,
 * OCR extraction, and expiration tracking.
 */
interface CertificationRepository {

    // ===== Core CRUD Operations =====

    /**
     * Create a new certification for a worker
     */
    suspend fun createCertification(
        workerProfileId: String,
        companyId: String?,
        request: CreateCertificationRequest
    ): Result<WorkerCertification>

    /**
     * Get certification by ID
     */
    suspend fun getCertification(
        certificationId: String,
        includeType: Boolean = true
    ): WorkerCertification?

    /**
     * Update certification details
     */
    suspend fun updateCertification(
        certificationId: String,
        issueDate: LocalDate? = null,
        expirationDate: LocalDate? = null,
        issuingAuthority: String? = null,
        certificationNumber: String? = null
    ): Result<WorkerCertification>

    /**
     * Delete certification
     */
    suspend fun deleteCertification(certificationId: String): Result<Unit>

    // ===== Certification Queries =====

    /**
     * Get all certifications for a worker
     */
    suspend fun getWorkerCertifications(
        workerProfileId: String,
        status: CertificationStatus? = null,
        includeExpired: Boolean = false
    ): List<WorkerCertification>

    /**
     * Get certifications by company
     */
    suspend fun getCompanyCertifications(
        companyId: String,
        status: CertificationStatus? = null,
        pagination: PaginationRequest = PaginationRequest()
    ): PaginatedResult<WorkerCertification>

    /**
     * Get certifications by type
     */
    suspend fun getCertificationsByType(
        companyId: String,
        certificationTypeId: String,
        status: CertificationStatus = CertificationStatus.VERIFIED
    ): List<WorkerCertification>

    // ===== Verification =====

    /**
     * Approve a pending certification
     */
    suspend fun approveCertification(
        certificationId: String,
        verifiedBy: String,
        notes: String? = null
    ): Result<WorkerCertification>

    /**
     * Reject a certification
     */
    suspend fun rejectCertification(
        certificationId: String,
        verifiedBy: String,
        reason: String
    ): Result<WorkerCertification>

    /**
     * Get pending certifications for verification
     */
    suspend fun getPendingCertifications(
        companyId: String,
        limit: Int = 50
    ): List<WorkerCertification>

    // ===== Expiration Tracking =====

    /**
     * Get certifications expiring within specified days
     */
    suspend fun getExpiringCertifications(
        companyId: String,
        daysUntilExpiration: Int = 30
    ): List<WorkerCertification>

    /**
     * Get expired certifications
     */
    suspend fun getExpiredCertifications(
        companyId: String,
        limit: Int = 100
    ): List<WorkerCertification>

    /**
     * Mark certifications as expired (batch operation)
     */
    suspend fun markCertificationsExpired(
        certificationIds: List<String>
    ): Result<Int>

    /**
     * Get workers with expiring certifications
     */
    suspend fun getWorkersWithExpiringCerts(
        companyId: String,
        daysUntilExpiration: Int = 30
    ): Map<String, List<WorkerCertification>> // workerId to certifications

    // ===== OCR and Document Processing =====

    /**
     * Upload certification document with OCR extraction
     */
    suspend fun uploadCertificationDocument(
        workerProfileId: String,
        companyId: String?,
        documentData: ByteArray,
        fileName: String,
        mimeType: String
    ): Result<CertificationUploadResult>

    /**
     * Process OCR data and create certification
     */
    suspend fun processCertificationOCR(
        workerProfileId: String,
        companyId: String?,
        documentUrl: String,
        ocrData: OCRExtractedData
    ): Result<WorkerCertification>

    // ===== Certification Types =====

    /**
     * Get all certification types
     */
    suspend fun getCertificationTypes(
        category: String? = null,
        region: String = "US"
    ): List<CertificationType>

    /**
     * Get certification type by code
     */
    suspend fun getCertificationTypeByCode(code: String): CertificationType?

    /**
     * Search certification types
     */
    suspend fun searchCertificationTypes(
        query: String,
        limit: Int = 20
    ): List<CertificationType>

    // ===== Statistics =====

    /**
     * Get certification count by status
     */
    suspend fun getCertificationCountByStatus(companyId: String): Map<CertificationStatus, Int>

    /**
     * Get certification count by type
     */
    suspend fun getCertificationCountByType(companyId: String): Map<String, Int>

    /**
     * Get compliance metrics
     */
    suspend fun getComplianceMetrics(companyId: String): CertificationComplianceMetrics

    // ===== Reactive Queries =====

    /**
     * Observe worker certifications
     */
    fun observeWorkerCertifications(
        workerProfileId: String
    ): Flow<List<WorkerCertification>>

    /**
     * Observe pending certifications
     */
    fun observePendingCertifications(companyId: String): Flow<List<WorkerCertification>>

    /**
     * Observe expiring certifications
     */
    fun observeExpiringCertifications(
        companyId: String,
        daysUntilExpiration: Int = 30
    ): Flow<List<WorkerCertification>>
}

/**
 * Result of certification document upload
 */
data class CertificationUploadResult(
    val documentUrl: String,
    val thumbnailUrl: String?,
    val extractedData: OCRExtractedData?,
    val needsReview: Boolean
)

/**
 * OCR extracted data from certification document
 */
data class OCRExtractedData(
    val rawText: String,
    val holderName: String?,
    val certificationType: String?,
    val certificationNumber: String?,
    val issueDate: LocalDate?,
    val expirationDate: LocalDate?,
    val issuingAuthority: String?,
    val confidence: Double, // 0.0 to 1.0
    val fieldConfidences: Map<String, Double> = emptyMap()
)

/**
 * Certification compliance metrics
 */
data class CertificationComplianceMetrics(
    val totalCertifications: Int,
    val verifiedCertifications: Int,
    val pendingCertifications: Int,
    val expiredCertifications: Int,
    val expiringWithin30Days: Int,
    val complianceRate: Double, // percentage of verified/total
    val workersCertified: Int,
    val workersWithExpiredCerts: Int
)
