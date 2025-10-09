package com.hazardhawk.domain.services

import kotlinx.datetime.LocalDate

/**
 * Service for extracting structured data from certification documents using OCR.
 *
 * Supports document types: PDF, PNG, JPG, JPEG
 * Uses Google Document AI for extraction (to be integrated)
 */
interface OCRService {
    /**
     * Extracts certification data from a document URL.
     *
     * @param documentUrl The URL of the document to process (typically S3 URL)
     * @return Result containing extracted certification data or error
     */
    suspend fun extractCertificationData(
        documentUrl: String
    ): Result<ExtractedCertification>

    /**
     * Batch process multiple certification documents in parallel.
     *
     * @param documentUrls List of document URLs to process
     * @return List of results for each document (maintains order)
     */
    suspend fun extractCertificationDataBatch(
        documentUrls: List<String>
    ): List<Result<ExtractedCertification>>
}

/**
 * Extracted certification data from OCR processing.
 *
 * @property holderName Full name of the certification holder
 * @property certificationType Standard certification type code (e.g., "OSHA_10")
 * @property certificationNumber Certificate number/ID if available
 * @property issueDate Date the certification was issued
 * @property expirationDate Date the certification expires
 * @property issuingAuthority Organization that issued the certification
 * @property confidence Overall confidence score (0.0 to 1.0)
 * @property needsReview True if confidence < 0.85 or critical fields missing
 * @property rawText Original text extracted from document
 * @property fieldConfidences Individual confidence scores for each field
 */
data class ExtractedCertification(
    val holderName: String,
    val certificationType: String,
    val certificationNumber: String?,
    val issueDate: LocalDate?,
    val expirationDate: LocalDate?,
    val issuingAuthority: String?,
    val confidence: Float,
    val needsReview: Boolean,
    val rawText: String? = null,
    val fieldConfidences: Map<String, Float> = emptyMap()
) {
    companion object {
        const val MIN_AUTO_ACCEPT_CONFIDENCE = 0.85f
        const val MIN_FIELD_CONFIDENCE = 0.70f
    }
}

/**
 * Errors that can occur during OCR processing.
 */
sealed class OCRError : Exception() {
    data class InvalidDocumentFormat(val format: String) : OCRError() {
        override val message: String = "Unsupported document format: $format. Supported: PDF, PNG, JPG, JPEG"
    }

    data class DocumentTooLarge(val sizeBytes: Long, val maxSizeBytes: Long) : OCRError() {
        override val message: String = "Document size $sizeBytes bytes exceeds maximum $maxSizeBytes bytes"
    }

    data class ExtractionFailed(val reason: String) : OCRError() {
        override val message: String = "OCR extraction failed: $reason"
    }

    data class NetworkError(override val cause: Throwable) : OCRError() {
        override val message: String = "Network error during OCR processing: ${cause.message}"
    }

    data class UnknownCertificationType(val extractedType: String) : OCRError() {
        override val message: String = "Could not map extracted type '$extractedType' to known certification"
    }
}
