package com.hazardhawk.domain.repositories

import com.hazardhawk.models.crew.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import com.hazardhawk.models.common.*

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

    /**
     * Send expiration reminder notification for a certification
     */
    suspend fun sendExpirationReminder(
        certificationId: String,
        channels: List<NotificationChannel> = listOf(NotificationChannel.EMAIL)
    ): Result<ExpirationReminderResult>

    /**
     * Send bulk expiration reminders
     */
    suspend fun sendBulkExpirationReminders(
        certificationIds: List<String>,
        channels: List<NotificationChannel> = listOf(NotificationChannel.EMAIL)
    ): Result<BulkReminderResult>

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

    // ===== Bulk Operations =====

    /**
     * Import certifications from CSV
     */
    suspend fun importCertificationsFromCSV(
        companyId: String,
        csvData: String,
        validateOnly: Boolean = false
    ): Result<CSVImportResult>

    // ===== Advanced Search =====

    /**
     * Search certifications with advanced filters
     */
    suspend fun searchCertifications(
        companyId: String,
        filters: CertificationSearchFilters
    ): PaginatedResult<WorkerCertification>

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

/**
 * Notification channels for expiration reminders
 */
enum class NotificationChannel {
    EMAIL,
    SMS,
    PUSH,
    IN_APP
}

/**
 * Result of sending an expiration reminder
 */
data class ExpirationReminderResult(
    val certificationId: String,
    val workerName: String?,
    val certificationType: String,
    val expirationDate: LocalDate?,
    val sentChannels: List<NotificationChannel>,
    val failedChannels: List<NotificationChannel>,
    val sentAt: String
)

/**
 * Result of sending bulk expiration reminders
 */
data class BulkReminderResult(
    val totalRequested: Int,
    val successCount: Int,
    val failureCount: Int,
    val results: List<ExpirationReminderResult>
)

/**
 * CSV import result
 */
data class CSVImportResult(
    val totalRows: Int,
    val successCount: Int,
    val errorCount: Int,
    val errors: List<CSVImportError>,
    val createdCertifications: List<WorkerCertification>
)

/**
 * CSV import error
 */
data class CSVImportError(
    val rowNumber: Int,
    val field: String?,
    val value: String?,
    val error: String
)

/**
 * Advanced certification search filters
 */
data class CertificationSearchFilters(
    val query: String? = null,
    val status: CertificationStatus? = null,
    val certificationTypeIds: List<String>? = null,
    val workerIds: List<String>? = null,
    val expirationDateFrom: LocalDate? = null,
    val expirationDateTo: LocalDate? = null,
    val issueDateFrom: LocalDate? = null,
    val issueDateTo: LocalDate? = null,
    val pagination: PaginationRequest = PaginationRequest(),
    val sortBy: CertificationSortField = CertificationSortField.CREATED_AT,
    val sortDirection: SortDirection = SortDirection.DESC
)

/**
 * Sort field for certifications
 */
enum class CertificationSortField {
    CREATED_AT,
    UPDATED_AT,
    EXPIRATION_DATE,
    ISSUE_DATE,
    WORKER_NAME,
    CERTIFICATION_TYPE
}

/**
 * Sort direction
 */
enum class SortDirection {
    ASC,
    DESC
}
