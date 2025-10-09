package com.hazardhawk.domain.services

import kotlinx.datetime.LocalDate

/**
 * Service for extracting certification data from documents using Google Document AI.
 * Provides OCR and intelligent field extraction for safety certifications.
 */
interface OCRService {
    /**
     * Extracts certification data from a document URL.
     * Uses Google Document AI to parse and extract structured certification information.
     *
     * @param documentUrl S3 URL or publicly accessible document URL (PDF, PNG, JPG)
     * @return Result containing extracted certification data or error
     */
    suspend fun extractCertificationData(
        documentUrl: String
    ): Result<ExtractedCertification>

    /**
     * Validates if the document is accessible and in a supported format.
     * Supported formats: PDF, PNG, JPG, JPEG
     *
     * @param documentUrl Document URL to validate
     * @return Result indicating if the document is valid and accessible
     */
    suspend fun validateDocumentFormat(
        documentUrl: String
    ): Result<DocumentValidation>

    /**
     * Batch process multiple certification documents.
     * Processes documents in parallel for efficiency.
     *
     * @param documentUrls List of document URLs to process
     * @return List of results for each document (maintains order)
     */
    suspend fun batchExtractCertifications(
        documentUrls: List<String>
    ): List<Result<ExtractedCertification>>
}

/**
 * Extracted certification data from OCR processing.
 * Contains structured information parsed from certification documents.
 */
data class ExtractedCertification(
    val holderName: String,
    val certificationType: String, // Mapped to standard codes (OSHA_10, OSHA_30, etc.)
    val certificationNumber: String?,
    val issueDate: LocalDate?,
    val expirationDate: LocalDate?,
    val issuingAuthority: String?,
    val confidence: Float, // 0.0 to 1.0
    val needsReview: Boolean, // True if confidence < 0.85
    val rawText: String? = null, // Full extracted text for debugging
    val extractedFields: Map<String, String> = emptyMap() // Raw field extractions
) {
    /**
     * Returns true if all critical fields were extracted successfully.
     */
    val hasCriticalFields: Boolean
        get() = holderName.isNotBlank() &&
                certificationType.isNotBlank() &&
                (issueDate != null || expirationDate != null)

    /**
     * Returns a human-readable summary of extraction quality.
     */
    val qualityDescription: String
        get() = when {
            confidence >= 0.95f -> "Excellent"
            confidence >= 0.85f -> "Good"
            confidence >= 0.70f -> "Fair"
            else -> "Poor - Manual Review Required"
        }
}

/**
 * Document validation result.
 */
data class DocumentValidation(
    val isValid: Boolean,
    val format: String?, // PDF, PNG, JPG, etc.
    val sizeBytes: Long?,
    val errorMessage: String?
)

/**
 * Standard certification type codes used across the system.
 * These should match the 'code' field in CertificationType model.
 */
object CertificationTypeCodes {
    // OSHA Training
    const val OSHA_10 = "OSHA_10"
    const val OSHA_30 = "OSHA_30"
    const val OSHA_500 = "OSHA_500"
    const val OSHA_510 = "OSHA_510"

    // Medical Certifications
    const val CPR = "CPR"
    const val FIRST_AID = "FIRST_AID"
    const val AED = "AED"

    // Equipment Operator
    const val FORKLIFT = "FORKLIFT"
    const val CRANE_OPERATOR = "CRANE_OPERATOR"
    const val AERIAL_LIFT = "AERIAL_LIFT"
    const val BOOM_LIFT = "BOOM_LIFT"
    const val SCISSOR_LIFT = "SCISSOR_LIFT"
    const val BACKHOE = "BACKHOE"
    const val EXCAVATOR = "EXCAVATOR"
    const val BULLDOZER = "BULLDOZER"
    const val LOADER = "LOADER"

    // Specialized Safety
    const val CONFINED_SPACE = "CONFINED_SPACE"
    const val FALL_PROTECTION = "FALL_PROTECTION"
    const val SCAFFOLDING = "SCAFFOLDING"
    const val RIGGING = "RIGGING"
    const val SIGNAL_PERSON = "SIGNAL_PERSON"
    const val HAZWOPER = "HAZWOPER"
    const val LOCKOUT_TAGOUT = "LOCKOUT_TAGOUT"
    const val HOT_WORK = "HOT_WORK"

    // Trade Specific
    const val ELECTRICAL_LICENSE = "ELECTRICAL_LICENSE"
    const val PLUMBING_LICENSE = "PLUMBING_LICENSE"
    const val HVAC_LICENSE = "HVAC_LICENSE"
    const val WELDING_CERT = "WELDING_CERT"
    const val ASBESTOS_HANDLER = "ASBESTOS_HANDLER"
    const val LEAD_PAINT_CERT = "LEAD_PAINT_CERT"

    // General
    const val CDL_CLASS_A = "CDL_CLASS_A"
    const val CDL_CLASS_B = "CDL_CLASS_B"
    const val DRIVERS_LICENSE = "DRIVERS_LICENSE"
    const val OTHER = "OTHER"

    /**
     * Returns a list of all standard certification codes.
     */
    val ALL_CODES = listOf(
        OSHA_10, OSHA_30, OSHA_500, OSHA_510,
        CPR, FIRST_AID, AED,
        FORKLIFT, CRANE_OPERATOR, AERIAL_LIFT, BOOM_LIFT, SCISSOR_LIFT,
        BACKHOE, EXCAVATOR, BULLDOZER, LOADER,
        CONFINED_SPACE, FALL_PROTECTION, SCAFFOLDING, RIGGING, SIGNAL_PERSON,
        HAZWOPER, LOCKOUT_TAGOUT, HOT_WORK,
        ELECTRICAL_LICENSE, PLUMBING_LICENSE, HVAC_LICENSE, WELDING_CERT,
        ASBESTOS_HANDLER, LEAD_PAINT_CERT,
        CDL_CLASS_A, CDL_CLASS_B, DRIVERS_LICENSE,
        OTHER
    )

    /**
     * Maps common certification names and variations to standard codes.
     * Used for intelligent matching during OCR extraction.
     */
    val NAME_TO_CODE_MAPPING = mapOf(
        // OSHA variations
        "osha 10" to OSHA_10,
        "osha 10 hour" to OSHA_10,
        "osha 10-hour" to OSHA_10,
        "10 hour osha" to OSHA_10,
        "osha 30" to OSHA_30,
        "osha 30 hour" to OSHA_30,
        "osha 30-hour" to OSHA_30,
        "30 hour osha" to OSHA_30,
        "osha 500" to OSHA_500,
        "osha 510" to OSHA_510,

        // Medical variations
        "cpr" to CPR,
        "cardiopulmonary resuscitation" to CPR,
        "first aid" to FIRST_AID,
        "firstaid" to FIRST_AID,
        "aed" to AED,
        "automated external defibrillator" to AED,

        // Equipment variations
        "forklift" to FORKLIFT,
        "fork lift" to FORKLIFT,
        "forklift operator" to FORKLIFT,
        "powered industrial truck" to FORKLIFT,
        "crane" to CRANE_OPERATOR,
        "crane operator" to CRANE_OPERATOR,
        "aerial lift" to AERIAL_LIFT,
        "boom lift" to BOOM_LIFT,
        "scissor lift" to SCISSOR_LIFT,
        "backhoe" to BACKHOE,
        "excavator" to EXCAVATOR,
        "bulldozer" to BULLDOZER,
        "dozer" to BULLDOZER,
        "loader" to LOADER,

        // Specialized safety variations
        "confined space" to CONFINED_SPACE,
        "confined space entry" to CONFINED_SPACE,
        "fall protection" to FALL_PROTECTION,
        "fall arrest" to FALL_PROTECTION,
        "scaffolding" to SCAFFOLDING,
        "scaffold" to SCAFFOLDING,
        "scaffold erector" to SCAFFOLDING,
        "rigging" to RIGGING,
        "rigger" to RIGGING,
        "signal person" to SIGNAL_PERSON,
        "signalperson" to SIGNAL_PERSON,
        "hazwoper" to HAZWOPER,
        "hazardous waste operations" to HAZWOPER,
        "lockout tagout" to LOCKOUT_TAGOUT,
        "lockout/tagout" to LOCKOUT_TAGOUT,
        "loto" to LOCKOUT_TAGOUT,
        "hot work" to HOT_WORK,
        "hot work permit" to HOT_WORK,

        // Trade specific variations
        "electrician" to ELECTRICAL_LICENSE,
        "electrical license" to ELECTRICAL_LICENSE,
        "plumber" to PLUMBING_LICENSE,
        "plumbing license" to PLUMBING_LICENSE,
        "hvac" to HVAC_LICENSE,
        "hvac license" to HVAC_LICENSE,
        "welder" to WELDING_CERT,
        "welding" to WELDING_CERT,
        "welding certification" to WELDING_CERT,
        "asbestos" to ASBESTOS_HANDLER,
        "asbestos handler" to ASBESTOS_HANDLER,
        "lead paint" to LEAD_PAINT_CERT,
        "lead-based paint" to LEAD_PAINT_CERT,

        // CDL variations
        "cdl class a" to CDL_CLASS_A,
        "cdl-a" to CDL_CLASS_A,
        "class a cdl" to CDL_CLASS_A,
        "cdl class b" to CDL_CLASS_B,
        "cdl-b" to CDL_CLASS_B,
        "class b cdl" to CDL_CLASS_B,
        "driver's license" to DRIVERS_LICENSE,
        "drivers license" to DRIVERS_LICENSE,
        "driver license" to DRIVERS_LICENSE
    )
}
