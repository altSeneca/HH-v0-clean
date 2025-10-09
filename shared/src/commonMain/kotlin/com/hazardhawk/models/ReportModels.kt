package com.hazardhawk.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

// Simplified report models to resolve build errors
@Serializable
enum class GenerationStatus {
    Processing,
    PhotoAnalysis,
    DocumentGeneration,
    GeneratingReport,
    ProcessingPhotos,
    Finalizing,
    Completed,
    Cancelled,
    Failed
}

@Serializable
data class ReportGenerationResult(
    val success: Boolean,
    val filePath: String? = null,
    val error: String? = null
)

@Serializable
data class DigitalSignature(
    val signerName: String,
    val signerTitle: String,
    val signatureData: String, // Base64 encoded signature image
    val timestamp: Long,
    val verified: Boolean = false
)

@Serializable
data class HazardAnalysis(
    val id: String,
    val description: String,
    val severity: String,
    val riskLevel: String,
    val oshaStandard: String? = null,
    val recommendations: List<String> = emptyList()
)

@Serializable
data class OshaComplianceInfo(
    val complianceStatus: String = "PENDING",
    val violationsFound: List<String> = emptyList(),
    val correctiveActions: List<String> = emptyList(),
    val complianceNotes: String? = null
)

@Serializable
enum class ValidationResult {
    Valid,
    Invalid
}

// All report-related models are now defined in SafetyReport.kt
// This file only contains simple enums and data classes that don't conflict

