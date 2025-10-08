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
 *
 * @property companyName Name of the company generating the document
 * @property companyLogo Optional company logo as raw image bytes (PNG/JPEG)
 * @property projectName Name of the project this PTP belongs to
 * @property projectLocation Physical location of the project
 * @property generatedBy Application name that generated the PDF (default: "HazardHawk")
 * @property generatedAt Timestamp when the PDF was generated (epoch milliseconds)
 */
data class PDFMetadata(
    val companyName: String,
    val companyLogo: ByteArray? = null,
    val projectName: String,
    val projectLocation: String,
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
        if (generatedBy != other.generatedBy) return false
        if (generatedAt != other.generatedAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = companyName.hashCode()
        result = 31 * result + (companyLogo?.contentHashCode() ?: 0)
        result = 31 * result + projectName.hashCode()
        result = 31 * result + projectLocation.hashCode()
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

/**
 * PDF layout configuration constants for OSHA-compliant document generation.
 */
object PDFLayoutConfig {
    // Page dimensions (US Letter: 8.5" x 11" at 72 DPI)
    const val PAGE_WIDTH = 612f // 8.5 * 72
    const val PAGE_HEIGHT = 792f // 11 * 72

    // Margins (0.5" all sides)
    const val MARGIN_LEFT = 36f // 0.5 * 72
    const val MARGIN_RIGHT = 36f
    const val MARGIN_TOP = 36f
    const val MARGIN_BOTTOM = 36f

    // Content area
    const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT // 540
    const val CONTENT_HEIGHT = PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM // 720

    // Font sizes - optimized for field readability and 6th grade level
    const val FONT_SIZE_TITLE = 20f          // Larger for prominence
    const val FONT_SIZE_HEADING = 16f        // Larger for section scanning
    const val FONT_SIZE_SUBHEADING = 14f
    const val FONT_SIZE_BODY = 12f           // Increased from 10pt for field use
    const val FONT_SIZE_SMALL = 10f          // Increased from 8pt
    const val FONT_SIZE_LARGE = 14f          // For critical warnings

    // Line spacing
    const val LINE_SPACING_TITLE = 24f
    const val LINE_SPACING_HEADING = 20f
    const val LINE_SPACING_BODY = 16f
    const val LINE_SPACING_SMALL = 13f

    // Hazard box styling - INCREASED PADDING
    const val HAZARD_BOX_PADDING_TOP = 20f      // Increased from 15f
    const val HAZARD_BOX_PADDING_BOTTOM = 20f   // Increased from 15f
    const val HAZARD_BOX_PADDING_LEFT = 15f     // Increased from 10f
    const val HAZARD_BOX_PADDING_RIGHT = 15f    // Increased from 10f
    const val HAZARD_BOX_BORDER_RADIUS = 8f
    const val HAZARD_BOX_BORDER_WIDTH = 3f
    const val HAZARD_BOX_MIN_HEIGHT = 100f      // Increased from 80f

    // Signature section
    const val SIGNATURE_LINE_WIDTH = 200f
    const val SIGNATURE_LINE_HEIGHT = 60f
    const val SIGNATURE_SPACING = 40f           // Space between signature lines

    // Photo dimensions (3" x 4" at 72 DPI)
    const val PHOTO_WIDTH = 216f // 3 * 72
    const val PHOTO_HEIGHT = 288f // 4 * 72
    const val PHOTO_METADATA_WIDTH = CONTENT_WIDTH - PHOTO_WIDTH - 20f // Space for metadata
    const val PHOTOS_PER_PAGE = 2

    // Colors (ARGB format for Android compatibility)
    const val COLOR_CRITICAL = 0xFFD32F2F.toInt() // Red
    const val COLOR_MAJOR = 0xFFFF8F00.toInt() // Orange
    const val COLOR_MINOR = 0xFFFDD835.toInt() // Yellow
    const val COLOR_HEADER = 0xFF1976D2.toInt() // Dark blue
    const val COLOR_BLACK = 0xFF000000.toInt()
    const val COLOR_DARK_GRAY = 0xFF424242.toInt()
    const val COLOR_LIGHT_GRAY = 0xFFE0E0E0.toInt()

    // Section spacing
    const val SECTION_SPACING = 16f
    const val SUBSECTION_SPACING = 8f
    const val PARAGRAPH_SPACING = 4f
}
