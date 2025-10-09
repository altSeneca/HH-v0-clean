package com.hazardhawk.documents

import com.hazardhawk.domain.models.ptp.PreTaskPlan
import kotlinx.datetime.Clock

/**
 * Interface for generating OSHA-compliant PDF documents from Pre-Task Plans.
 *
 * This interface provides a cross-platform contract for PDF generation.
 * Platform-specific implementations will use appropriate PDF libraries:
 * - Android: PdfDocument API
 * - iOS: PDFKit framework
 * - Desktop: Apache PDFBox or iText
 * - Web: Server-side generation or PDF.js
 */
interface PTPPDFGenerator {
    /**
     * Generate a PDF document from a Pre-Task Plan with associated photos.
     *
     * @param ptp The Pre-Task Plan to generate PDF from
     * @param photos List of photos with metadata to include in the document
     * @return Result containing the PDF as a ByteArray, or an error
     */
    suspend fun generatePDF(ptp: PreTaskPlan, photos: List<PhotoData>): Result<ByteArray>

    /**
     * Generate a PDF document with custom metadata (company branding, etc).
     *
     * @param ptp The Pre-Task Plan to generate PDF from
     * @param photos List of photos with metadata to include in the document
     * @param metadata Custom metadata for branding and document information
     * @return Result containing the PDF as a ByteArray, or an error
     */
    suspend fun generatePDFWithMetadata(
        ptp: PreTaskPlan,
        photos: List<PhotoData>,
        metadata: PDFMetadata
    ): Result<ByteArray>
}

/**
 * Metadata for customizing the PDF document appearance and branding.
 * NOTE: Most fields are now auto-populated from PreTaskPlan model. This class is being
 * phased out in favor of using the centralized data directly from the PTP model.
 *
 * @property companyName Name of the company generating the document (deprecated - use PTP.companyName)
 * @property companyLogo Optional company logo as raw image bytes (PNG/JPEG) (deprecated - use PTP.companyLogoUrl)
 * @property projectName Name of the project this PTP belongs to (deprecated - use PTP.projectName)
 * @property projectLocation Physical location of the project (deprecated - use PTP.projectAddress)
 * @property taskDescription Brief description of the task (deprecated - use PTP.workScope)
 * @property competentPerson Name of the competent person overseeing the work (deprecated - use PTP.foremanName)
 * @property generatedBy Application name that generated the PDF (default: "HazardHawk")
 * @property generatedAt Timestamp when the PDF was generated (epoch milliseconds)
 */
data class PDFMetadata(
    val companyName: String,
    val companyLogo: ByteArray? = null,
    val projectName: String,
    val projectLocation: String,
    val taskDescription: String? = null,
    val competentPerson: String? = null,
    val generatedBy: String = "HazardHawk",
    val generatedAt: Long = Clock.System.now().toEpochMilliseconds()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PDFMetadata

        if (companyName != other.companyName) return false
        if (companyLogo != null) {
            if (other.companyLogo == null) return false
            if (!companyLogo.contentEquals(other.companyLogo)) return false
        } else if (other.companyLogo != null) return false
        if (projectName != other.projectName) return false
        if (projectLocation != other.projectLocation) return false
        if (taskDescription != other.taskDescription) return false
        if (competentPerson != other.competentPerson) return false
        if (generatedBy != other.generatedBy) return false
        if (generatedAt != other.generatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = companyName.hashCode()
        result = 31 * result + (companyLogo?.contentHashCode() ?: 0)
        result = 31 * result + projectName.hashCode()
        result = 31 * result + projectLocation.hashCode()
        result = 31 * result + (taskDescription?.hashCode() ?: 0)
        result = 31 * result + (competentPerson?.hashCode() ?: 0)
        result = 31 * result + generatedBy.hashCode()
        result = 31 * result + generatedAt.hashCode()
        return result
    }
}

/**
 * Photo data to be included in the PDF document.
 *
 * @property imageBytes Raw image data (PNG/JPEG format)
 * @property metadata Metadata associated with the photo (location, timestamp, etc)
 */
data class PhotoData(
    val imageBytes: ByteArray,
    val metadata: PhotoMetadata
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PhotoData

        if (!imageBytes.contentEquals(other.imageBytes)) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }
}

/**
 * Metadata about a photo to be displayed alongside the image in the PDF.
 *
 * @property location Human-readable location description (e.g., "Building 3, 2nd Floor")
 * @property gpsCoordinates GPS coordinates in "latitude, longitude" format
 * @property timestamp When the photo was taken (epoch milliseconds)
 * @property aiAnalysisSummary List of hazards detected by AI analysis
 * @property caption Optional user-provided caption for the photo
 */
data class PhotoMetadata(
    val location: String? = null,
    val gpsCoordinates: String? = null,
    val timestamp: Long,
    val aiAnalysisSummary: List<String> = emptyList(),
    val caption: String? = null
)

