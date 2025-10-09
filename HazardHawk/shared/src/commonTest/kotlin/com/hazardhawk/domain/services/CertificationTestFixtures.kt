package com.hazardhawk.domain.services

import com.hazardhawk.domain.services.*
import com.hazardhawk.models.crew.*
import kotlinx.datetime.LocalDate
import kotlin.random.Random

/**
 * Comprehensive test fixtures for certification-related testing.
 * Provides reusable test data generators, sample files, and edge case scenarios.
 */
object CertificationTestFixtures {

    // ====================
    // Sample File Data
    // ====================

    /**
     * Valid JPEG file header (simplified for testing)
     */
    val validJpegHeader = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(),
        0x00, 0x10, 0x4A, 0x46, 0x49, 0x46
    )

    /**
     * Valid PNG file header
     */
    val validPngHeader = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    /**
     * Valid PDF file header
     */
    val validPdfHeader = byteArrayOf(
        0x25, 0x50, 0x44, 0x46, 0x2D // %PDF-
    )

    /**
     * Creates a mock JPEG file with specified size
     */
    fun createMockJpeg(sizeKB: Int = 100): ByteArray {
        val bytes = ByteArray(sizeKB * 1024)
        validJpegHeader.copyInto(bytes, 0)
        // Fill rest with random data
        Random.Default.nextBytes(bytes, validJpegHeader.size, bytes.size)
        return bytes
    }

    /**
     * Creates a mock PNG file with specified size
     */
    fun createMockPng(sizeKB: Int = 100): ByteArray {
        val bytes = ByteArray(sizeKB * 1024)
        validPngHeader.copyInto(bytes, 0)
        Random.Default.nextBytes(bytes, validPngHeader.size, bytes.size)
        return bytes
    }

    /**
     * Creates a mock PDF file with specified size
     */
    fun createMockPdf(sizeKB: Int = 500): ByteArray {
        val bytes = ByteArray(sizeKB * 1024)
        validPdfHeader.copyInto(bytes, 0)
        Random.Default.nextBytes(bytes, validPdfHeader.size, bytes.size)
        return bytes
    }

    /**
     * Creates a file with invalid format
     */
    fun createInvalidFile(sizeKB: Int = 50): ByteArray {
        return ByteArray(sizeKB * 1024) { it.toByte() }
    }

    // ====================
    // Certification Types
    // ====================

    val osha10Type = CertificationType(
        id = "cert-type-1",
        code = "OSHA_10",
        name = "OSHA 10-Hour Construction",
        category = "Safety Training",
        region = "US",
        typicalDurationMonths = 60,
        renewalRequired = true,
        description = "Basic safety and health hazards in construction"
    )

    val osha30Type = CertificationType(
        id = "cert-type-2",
        code = "OSHA_30",
        name = "OSHA 30-Hour Construction",
        category = "Safety Training",
        region = "US",
        typicalDurationMonths = 60,
        renewalRequired = true,
        description = "Advanced safety for construction supervisors"
    )

    val forkliftType = CertificationType(
        id = "cert-type-3",
        code = "FORKLIFT",
        name = "Forklift Operator Certification",
        category = "Equipment Operation",
        region = "US",
        typicalDurationMonths = 36,
        renewalRequired = true
    )

    val firstAidType = CertificationType(
        id = "cert-type-4",
        code = "FIRST_AID",
        name = "First Aid/CPR",
        category = "Medical",
        region = "US",
        typicalDurationMonths = 24,
        renewalRequired = true
    )

    val scaffoldType = CertificationType(
        id = "cert-type-5",
        code = "SCAFFOLD",
        name = "Scaffold Competent Person",
        category = "Safety Training",
        region = "US",
        typicalDurationMonths = 60,
        renewalRequired = true
    )

    val allCertificationTypes = listOf(
        osha10Type, osha30Type, forkliftType, firstAidType, scaffoldType
    )

    // ====================
    // Worker Certifications
    // ====================

    fun createWorkerCertification(
        id: String = "cert-${Random.nextInt(1000, 9999)}",
        workerProfileId: String = "worker-123",
        companyId: String? = "company-456",
        certificationTypeId: String = osha10Type.id,
        certificationType: CertificationType? = osha10Type,
        certificationNumber: String? = "OSHA-${Random.nextInt(100000, 999999)}",
        issueDate: LocalDate = LocalDate(2023, 1, 15),
        expirationDate: LocalDate? = LocalDate(2028, 1, 15),
        issuingAuthority: String? = "OSHA Training Institute",
        documentUrl: String = "https://s3.amazonaws.com/hazardhawk-certs/${id}.pdf",
        thumbnailUrl: String? = "https://s3.amazonaws.com/hazardhawk-certs/${id}-thumb.jpg",
        status: CertificationStatus = CertificationStatus.VERIFIED,
        verifiedBy: String? = "admin-789",
        verifiedAt: String? = "2023-01-20T10:30:00Z",
        rejectionReason: String? = null,
        ocrConfidence: Double? = 0.92,
        createdAt: String = "2023-01-15T08:00:00Z",
        updatedAt: String = "2023-01-20T10:30:00Z"
    ): WorkerCertification {
        return WorkerCertification(
            id = id,
            workerProfileId = workerProfileId,
            companyId = companyId,
            certificationTypeId = certificationTypeId,
            certificationType = certificationType,
            certificationNumber = certificationNumber,
            issueDate = issueDate,
            expirationDate = expirationDate,
            issuingAuthority = issuingAuthority,
            documentUrl = documentUrl,
            thumbnailUrl = thumbnailUrl,
            status = status,
            verifiedBy = verifiedBy,
            verifiedAt = verifiedAt,
            rejectionReason = rejectionReason,
            ocrConfidence = ocrConfidence,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // ====================
    // OCR Test Data
    // ====================

    /**
     * Sample raw OCR text for OSHA 10 certification
     */
    val osha10RawText = """
        OSHA 10-Hour Construction Safety Card
        
        This card certifies that
        John Smith
        
        Has successfully completed the 10-Hour
        OSHA Outreach Training Program
        
        Certificate Number: OSHA-234567
        Issue Date: 01/15/2023
        Expiration Date: 01/15/2028
        
        Issuing Authority: OSHA Training Institute
        Authorized by: U.S. Department of Labor
    """.trimIndent()

    /**
     * Sample raw OCR text for Forklift certification
     */
    val forkliftRawText = """
        FORKLIFT OPERATOR CERTIFICATION
        
        Certificate Holder: Jane Doe
        License Number: FL-987654
        
        Issued: 03/20/2023
        Valid Until: 03/20/2026
        
        Certified by: National Safety Council
        Trainer: Mike Johnson, CSP
    """.trimIndent()

    /**
     * Sample OCR text with ambiguous dates (edge case)
     */
    val ambiguousDateText = """
        Safety Training Certificate
        
        Worker: Bob Williams
        Course: First Aid/CPR
        
        Completed: 12/06/2023
        Valid through: 12/06/2025
        
        Note: Date format unclear - could be MM/DD/YYYY or DD/MM/YYYY
    """.trimIndent()

    /**
     * Sample OCR text with poor quality (low confidence scenario)
     */
    val poorQualityText = """
        C3rt1f1cat3 0f C0mpl3t10n
        
        Nam3: J0hn D03
        Typ3: 0SHA 30
        
        1ssu3d: 0?/15/2023
        3xp1r3s: 0?/15/2028
        
        (Scanned with poor quality, many OCR errors)
    """.trimIndent()

    fun createExtractedCertification(
        holderName: String = "John Smith",
        certificationType: String = "OSHA_10",
        certificationNumber: String? = "OSHA-234567",
        issueDate: LocalDate? = LocalDate(2023, 1, 15),
        expirationDate: LocalDate? = LocalDate(2028, 1, 15),
        issuingAuthority: String? = "OSHA Training Institute",
        confidence: Float = 0.92f,
        needsReview: Boolean = false,
        rawText: String? = osha10RawText,
        fieldConfidences: Map<String, Float> = mapOf(
            "holderName" to 0.95f,
            "certificationType" to 0.90f,
            "certificationNumber" to 0.88f,
            "issueDate" to 0.92f,
            "expirationDate" to 0.91f,
            "issuingAuthority" to 0.89f
        )
    ): ExtractedCertification {
        return ExtractedCertification(
            holderName = holderName,
            certificationType = certificationType,
            certificationNumber = certificationNumber,
            issueDate = issueDate,
            expirationDate = expirationDate,
            issuingAuthority = issuingAuthority,
            confidence = confidence,
            needsReview = needsReview,
            rawText = rawText,
            fieldConfidences = fieldConfidences
        )
    }

    // ====================
    // Date Test Cases
    // ====================

    /**
     * Common date formats found in certification documents
     */
    val dateFormatTestCases = listOf(
        "01/15/2023" to LocalDate(2023, 1, 15),    // MM/DD/YYYY
        "15/01/2023" to LocalDate(2023, 1, 15),    // DD/MM/YYYY
        "2023-01-15" to LocalDate(2023, 1, 15),    // ISO format
        "Jan 15, 2023" to LocalDate(2023, 1, 15),  // Written format
        "15 Jan 2023" to LocalDate(2023, 1, 15),   // European format
        "January 15, 2023" to LocalDate(2023, 1, 15)
    )

    /**
     * Edge case dates (expired, far future, etc.)
     */
    val edgeCaseDates = mapOf(
        "expired_yesterday" to LocalDate(2023, 1, 1),
        "expires_today" to LocalDate(2025, 10, 8),
        "expires_tomorrow" to LocalDate(2025, 10, 9),
        "expires_in_3_days" to LocalDate(2025, 10, 11),
        "expires_in_7_days" to LocalDate(2025, 10, 15),
        "expires_in_14_days" to LocalDate(2025, 10, 22),
        "expires_in_30_days" to LocalDate(2025, 11, 7),
        "expires_in_60_days" to LocalDate(2025, 12, 7),
        "expires_in_90_days" to LocalDate(2026, 1, 6),
        "far_future" to LocalDate(2035, 1, 1)
    )

    // ====================
    // Upload Test Data
    // ====================

    fun createFileUpload(
        fileName: String = "osha10-cert.jpg",
        contentType: String = "image/jpeg",
        sizeKB: Int = 100
    ): FileUpload {
        val data = when {
            contentType.startsWith("image/jpeg") -> createMockJpeg(sizeKB)
            contentType.startsWith("image/png") -> createMockPng(sizeKB)
            contentType.startsWith("application/pdf") -> createMockPdf(sizeKB)
            else -> createInvalidFile(sizeKB)
        }
        return FileUpload(data, fileName, contentType)
    }

    fun createUploadResult(
        fileName: String = "cert-123.jpg",
        sizeBytes: Long = 102400L,
        withThumbnail: Boolean = true
    ): UploadResult {
        val key = "certifications/${Random.nextInt(1000, 9999)}/$fileName"
        return UploadResult(
            url = "https://cdn.hazardhawk.com/$key",
            thumbnailUrl = if (withThumbnail) "https://cdn.hazardhawk.com/$key-thumb.jpg" else null,
            sizeBytes = sizeBytes,
            key = key
        )
    }

    // ====================
    // Certification Type Mapping Test Cases
    // ====================

    /**
     * Common OCR extraction variations for certification types
     */
    val certificationTypeMappings = mapOf(
        // OSHA 10 variations
        "OSHA 10" to "OSHA_10",
        "OSHA 10-Hour" to "OSHA_10",
        "OSHA 10 Hour Construction" to "OSHA_10",
        "10-Hour OSHA" to "OSHA_10",
        
        // OSHA 30 variations
        "OSHA 30" to "OSHA_30",
        "OSHA 30-Hour" to "OSHA_30",
        "OSHA 30 Hour Construction" to "OSHA_30",
        "30-Hour OSHA" to "OSHA_30",
        
        // Forklift variations
        "Forklift" to "FORKLIFT",
        "Forklift Operator" to "FORKLIFT",
        "Powered Industrial Truck" to "FORKLIFT",
        "PIT Certification" to "FORKLIFT",
        
        // First Aid variations
        "First Aid" to "FIRST_AID",
        "CPR/First Aid" to "FIRST_AID",
        "First Aid and CPR" to "FIRST_AID",
        "Emergency First Aid" to "FIRST_AID",
        
        // Scaffold variations
        "Scaffold" to "SCAFFOLD",
        "Scaffold Competent Person" to "SCAFFOLD",
        "Scaffolding Certification" to "SCAFFOLD"
    )

    // ====================
    // Progress Tracking
    // ====================

    /**
     * Creates a sequence of progress values for testing progress callbacks
     */
    fun createProgressSequence(steps: Int = 10): List<Float> {
        return (0..steps).map { it / steps.toFloat() }
    }
}
