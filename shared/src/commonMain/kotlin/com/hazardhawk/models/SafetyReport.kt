package com.hazardhawk.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import com.hazardhawk.core.models.ComplianceStatus

/**
 * Core safety report model with OSHA compliance support
 */
@Serializable
data class SafetyReport(
    val id: String,
    val templateId: String,
    val templateName: String,
    val reportType: ReportType,
    val title: String = "", // Add title property
    val siteInformation: SiteInformation,
    val reporterInformation: ReporterInformation,
    val photos: List<String> = emptyList(), // Photo IDs
    val sections: List<ReportSection> = emptyList(),
    val generationRequest: ReportGenerationRequest? = null,
    val complianceStatus: ComplianceStatus = ComplianceStatus.REQUIRES_REVIEW,
    val createdAt: Instant,
    val updatedAt: Instant,
    val oshaCompliant: Boolean = false,
    val signatures: List<ReportSignature> = emptyList(),
    // Add missing properties for ReportGenerationManager using existing models
    val hazardAnalysis: List<HazardAnalysis> = emptyList(),
    val oshaCompliance: OshaComplianceInfo = OshaComplianceInfo(),
    val digitalSignatures: List<DigitalSignature> = emptyList()
)

@Serializable
data class ReportTemplate(
    val id: String,
    val name: String,
    val type: ReportType,
    val description: String,
    val sections: List<ReportSection> = emptyList(),
    val requiredFields: List<String> = emptyList(),
    val oshaCompliant: Boolean = true,
    val minimumPhotos: Int = 1,
    val requiredSignatures: List<String> = emptyList(),
    val oshaStandards: List<String> = emptyList()
)

@Serializable
enum class ReportType {
    DAILY_INSPECTION,
    INCIDENT_REPORT,
    PRE_TASK_PLAN,
    WEEKLY_SUMMARY,
    HAZARD_IDENTIFICATION,
    SAFETY_TRAINING,
    TOOLBOX_TALK,
    SAFETY_AUDIT,
    NEAR_MISS
}

@Serializable
data class ReportSection(
    val id: String,
    val title: String,
    val type: String = "text", // Add type property
    val content: String = "",
    val required: Boolean = false,
    val order: Int = 0,
    val photos: List<String> = emptyList()
)

@Serializable
data class SiteInformation(
    val siteName: String,
    val siteAddress: String,
    val projectName: String? = null,
    val contractorName: String,
    val coordinates: String? = null, // Simplified to avoid cross-package dependencies
    val location: Location? = null, // For backward compatibility with tests
    val weatherConditions: String? = null // Add weatherConditions property
)

@Serializable
data class ReporterInformation(
    val name: String,
    val title: String,
    val company: String,
    val email: String? = null,
    val phone: String? = null,
    val certification: String? = null
)

@Serializable
data class ReportGenerationRequest(
    val templateId: String,
    val photoIds: List<String>,
    val siteInfo: SiteInformation,
    val reporterInfo: ReporterInformation,
    val exportFormat: ExportFormat = ExportFormat.PDF,
    val includeAnalysis: Boolean = true,
    val includeOSHACodes: Boolean = true
)

@Serializable
enum class ExportFormat {
    PDF, DOCX, HTML, CSV
}

@Serializable
data class ReportGenerationProgress(
    val requestId: String,
    val status: GenerationStatus,
    val progress: Float = 0f,
    val currentStep: String = "",
    val totalPhotos: Int = 0,
    val processedPhotos: Int = 0,
    val errorMessage: String? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val lastUpdate: Long = 0L // Add missing lastUpdate property
)

// Use Location model from existing Location.kt file (don't redeclare)


@Serializable
data class ReportSignature(
    val signerName: String,
    val signerTitle: String,
    val signatureData: String, // Base64 encoded signature image
    val signedAt: Instant,
    val verified: Boolean = false
)

@Serializable
data class BatchOperation(
    val id: String,
    val operationType: BatchOperationType,
    val status: BatchOperationStatus,
    val totalItems: Int,
    val processedItems: Int = 0,
    val failedItems: Int = 0,
    val progress: Float = 0f,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val errorMessage: String? = null
)

@Serializable
enum class BatchOperationType {
    DeletePhotos,
    ExportPhotos,
    GenerateReports,
    AnalyzePhotos,
    UpdateTags
}

@Serializable
enum class BatchOperationStatus {
    Pending,
    InProgress,
    Completed,
    Failed,
    Cancelled
}
