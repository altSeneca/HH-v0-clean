package com.hazardhawk.domain.fixtures

import com.hazardhawk.domain.services.*
import com.hazardhawk.core.models.crew.*
import kotlinx.datetime.LocalDate
import kotlin.random.Random

/**
 * Test fixtures for certification-related testing.
 * Provides reusable test data for all Phase 2 tests.
 */
object CertificationTestFixtures {
    
    // ===== Worker Certifications =====
    
    fun createWorkerCertification(
        id: String = "cert-${Random.nextInt(1000)}",
        workerProfileId: String = "worker-123",
        companyId: String = "company-456",
        certificationTypeId: String = "type-789",
        certificationNumber: String? = "CERT-${Random.nextInt(100000)}",
        issueDate: LocalDate = LocalDate(2024, 1, 15),
        expirationDate: LocalDate? = LocalDate(2026, 1, 15),
        issuingAuthority: String? = "OSHA",
        documentUrl: String = "https://s3.amazonaws.com/bucket/certifications/doc-${Random.nextInt(1000)}.pdf",
        thumbnailUrl: String? = "https://cdn.hazardhawk.com/thumbnails/thumb-${Random.nextInt(1000)}.jpg",
        status: CertificationStatus = CertificationStatus.VERIFIED,
        verifiedBy: String? = null,
        verifiedAt: String? = null,
        rejectionReason: String? = null,
        ocrConfidence: Double? = 0.95,
        createdAt: String = "2025-01-15T10:00:00Z",
        updatedAt: String = "2025-01-15T10:00:00Z",
        certificationType: CertificationType? = createCertificationType()
    ): WorkerCertification {
        return WorkerCertification(
            id = id,
            workerProfileId = workerProfileId,
            companyId = companyId,
            certificationTypeId = certificationTypeId,
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
            updatedAt = updatedAt,
            certificationType = certificationType
        )
    }
    
    fun createCertificationType(
        id: String = "type-789",
        code: String = CertificationTypeCodes.OSHA_10,
        name: String = "OSHA 10-Hour Construction",
        category: String = "OSHA Training",
        region: String = "US",
        typicalDurationMonths: Int? = 60,
        renewalRequired: Boolean = true,
        description: String? = "10-hour OSHA construction safety training"
    ): CertificationType {
        return CertificationType(
            id = id,
            code = code,
            name = name,
            category = category,
            region = region,
            typicalDurationMonths = typicalDurationMonths,
            renewalRequired = renewalRequired,
            description = description
        )
    }
    
    // ===== OCR Extracted Data =====
    
    fun createExtractedCertification(
        holderName: String = "John Doe",
        certificationType: String = CertificationTypeCodes.OSHA_10,
        certificationNumber: String? = "OSHA-123456",
        issueDate: LocalDate? = LocalDate(2024, 1, 15),
        expirationDate: LocalDate? = LocalDate(2026, 1, 15),
        issuingAuthority: String? = "OSHA Training Institute",
        confidence: Float = 0.95f,
        needsReview: Boolean = false,
        rawText: String? = "Sample OCR extracted text",
        extractedFields: Map<String, String> = emptyMap()
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
            extractedFields = extractedFields
        )
    }
    
    // ===== Upload Results =====
    
    fun createUploadResult(
        url: String = "https://s3.amazonaws.com/bucket/certifications/doc-123.pdf",
        thumbnailUrl: String? = "https://cdn.hazardhawk.com/thumbnails/thumb-123.jpg",
        sizeBytes: Long = 1024L * 500 // 500KB
    ): UploadResult {
        return UploadResult(
            url = url,
            thumbnailUrl = thumbnailUrl,
            sizeBytes = sizeBytes
        )
    }
    
    // ===== Document Validation =====
    
    fun createDocumentValidation(
        isValid: Boolean = true,
        format: String? = "pdf",
        sizeBytes: Long? = 1024L * 500,
        errorMessage: String? = null
    ): DocumentValidation {
        return DocumentValidation(
            isValid = isValid,
            format = format,
            sizeBytes = sizeBytes,
            errorMessage = errorMessage
        )
    }
    
    // ===== Sample File Data =====
    
    /**
     * Creates a sample PDF file (just a ByteArray with PDF header).
     * Not a real PDF, but sufficient for testing upload logic.
     */
    fun createSamplePdfData(sizeKB: Int = 100): ByteArray {
        val pdfHeader = "%PDF-1.4\n".toByteArray()
        val remainingSize = (sizeKB * 1024) - pdfHeader.size
        val padding = ByteArray(remainingSize.coerceAtLeast(0)) { 0 }
        return pdfHeader + padding
    }
    
    /**
     * Creates a sample image file (just a ByteArray with JPEG header).
     * Not a real image, but sufficient for testing upload logic.
     */
    fun createSampleImageData(sizeKB: Int = 200): ByteArray {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val remainingSize = (sizeKB * 1024) - jpegHeader.size
        val padding = ByteArray(remainingSize.coerceAtLeast(0)) { 0 }
        return jpegHeader + padding
    }
    
    // ===== OCR Response Samples =====
    
    val SAMPLE_OCR_RESPONSE_OSHA_10 = mapOf(
        "holder_name" to "John Doe",
        "certification_type" to "OSHA 10-Hour Construction",
        "certification_number" to "OSHA-123456",
        "issue_date" to "01/15/2024",
        "expiration_date" to "01/15/2026",
        "issuing_authority" to "OSHA Training Institute"
    )
    
    val SAMPLE_OCR_RESPONSE_FORKLIFT = mapOf(
        "holder_name" to "Jane Smith",
        "certification_type" to "Powered Industrial Truck Operator",
        "certification_number" to "FL-789012",
        "issue_date" to "2024-03-20",
        "expiration_date" to "2027-03-20",
        "issuing_authority" to "National Safety Council"
    )
    
    val SAMPLE_OCR_RESPONSE_LOW_CONFIDENCE = mapOf(
        "holder_name" to "J███ D██",  // Simulated poor OCR
        "certification_type" to "OS█A 30",
        "certification_number" to null,
        "issue_date" to null,
        "expiration_date" to "12/██/2025"
    )
    
    // ===== Date Parsing Test Cases =====
    
    val DATE_FORMAT_TEST_CASES = listOf(
        "01/15/2024" to LocalDate(2024, 1, 15),    // MM/DD/YYYY
        "2024-01-15" to LocalDate(2024, 1, 15),    // YYYY-MM-DD
        "15/01/2024" to LocalDate(2024, 1, 15),    // DD/MM/YYYY (will be parsed as MM/DD/YYYY)
        "01-15-2024" to LocalDate(2024, 1, 15),    // MM-DD-YYYY
        "01.15.2024" to LocalDate(2024, 1, 15),    // MM.DD.YYYY
        "01/15/24" to LocalDate(2024, 1, 15),      // MM/DD/YY
        "2024/01/15" to LocalDate(2024, 1, 15),    // YYYY/MM/DD
    )
    
    val INVALID_DATE_STRINGS = listOf(
        "invalid",
        "13/32/2024",   // Invalid month/day
        "00/00/2024",   // Zero month/day
        "2024-13-01",   // Invalid month
        "abc-def-ghij", // Non-numeric
        ""              // Empty string
    )
    
    // ===== Certification Type Mapping Test Cases =====
    
    val CERTIFICATION_TYPE_MAPPING_TESTS = listOf(
        "OSHA 10-Hour Construction Safety" to CertificationTypeCodes.OSHA_10,
        "osha 30" to CertificationTypeCodes.OSHA_30,
        "FORKLIFT OPERATOR" to CertificationTypeCodes.FORKLIFT,
        "Powered Industrial Truck" to CertificationTypeCodes.FORKLIFT,
        "CPR Certification" to CertificationTypeCodes.CPR,
        "Cardiopulmonary Resuscitation" to CertificationTypeCodes.CPR,
        "First Aid Training" to CertificationTypeCodes.FIRST_AID,
        "Crane Operator License" to CertificationTypeCodes.CRANE_OPERATOR,
        "Aerial Lift Platform" to CertificationTypeCodes.AERIAL_LIFT,
        "Confined Space Entry" to CertificationTypeCodes.CONFINED_SPACE,
        "Fall Protection Training" to CertificationTypeCodes.FALL_PROTECTION,
        "Scaffolding Erector" to CertificationTypeCodes.SCAFFOLDING,
        "Rigging and Signaling" to CertificationTypeCodes.RIGGING,
        "HAZWOPER 40-Hour" to CertificationTypeCodes.HAZWOPER,
        "Lockout/Tagout Procedures" to CertificationTypeCodes.LOCKOUT_TAGOUT,
        "Hot Work Permit" to CertificationTypeCodes.HOT_WORK,
        "Electrician License" to CertificationTypeCodes.ELECTRICAL_LICENSE,
        "Welding Certification AWS" to CertificationTypeCodes.WELDING_CERT,
        "CDL Class A" to CertificationTypeCodes.CDL_CLASS_A,
        "Unknown Certification Type" to CertificationTypeCodes.OTHER
    )
}
