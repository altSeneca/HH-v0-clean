package com.hazardhawk.documents

/**
 * PDF layout configuration constants for professional PTP document generation.
 *
 * This configuration ensures:
 * - Consistent styling across all PDFs
 * - High contrast for outdoor/field visibility
 * - Optimal readability at 300 DPI print quality
 * - OSHA compliance through clear hazard communication
 */
object PDFLayoutConfig {
    // Page dimensions (US Letter - 8.5" x 11" at 72 DPI)
    const val PAGE_WIDTH = 612f
    const val PAGE_HEIGHT = 792f

    // Margins (increased for field readability)
    const val MARGIN_TOP = 48f
    const val MARGIN_BOTTOM = 48f
    const val MARGIN_LEFT = 36f
    const val MARGIN_RIGHT = 36f

    // Calculated dimensions
    const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT
    const val CONTENT_HEIGHT = PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM

    // Typography (increased for outdoor visibility)
    const val FONT_SIZE_TITLE = 22f        // Was 20f
    const val FONT_SIZE_LARGE = 16f        // For prominent text
    const val FONT_SIZE_HEADING = 18f      // Was 16f
    const val FONT_SIZE_SUBHEADING = 14f   // Was 12f
    const val FONT_SIZE_BODY = 13f         // Was 12f
    const val FONT_SIZE_SMALL = 11f        // Was 10f

    // Line spacing (improved readability)
    const val LINE_SPACING_TITLE = 28f
    const val LINE_SPACING_HEADING = 24f
    const val LINE_SPACING_BODY = 18f      // Was 16f
    const val LINE_SPACING_SMALL = 15f     // Was 13f

    // Section spacing
    const val SECTION_SPACING = 24f
    const val SUBSECTION_SPACING = 16f
    const val PARAGRAPH_SPACING = 8f

    // Colors (high contrast for field conditions)
    const val COLOR_BLACK = 0xFF000000.toInt()
    const val COLOR_DARK_GRAY = 0xFF212121.toInt()    // Was 0xFF424242
    const val COLOR_MEDIUM_GRAY = 0xFF757575.toInt()
    const val COLOR_LIGHT_GRAY = 0xFFBDBDBD.toInt()   // Was 0xFFE0E0E0
    const val COLOR_HEADER = 0xFF1565C0.toInt()       // Professional blue

    // Severity colors (extra saturated for visibility)
    const val COLOR_CRITICAL = 0xFFD32F2F.toInt()     // Deep red
    const val COLOR_MAJOR = 0xFFFF8F00.toInt()        // Safety orange
    const val COLOR_MINOR = 0xFFFBC02D.toInt()        // Warning yellow

    // Background colors (very light, optimized for printing)
    const val COLOR_BG_CRITICAL = 0xFFFFEBEE.toInt()
    const val COLOR_BG_MAJOR = 0xFFFFF3E0.toInt()
    const val COLOR_BG_MINOR = 0xFFFFFDE7.toInt()

    // Branding
    const val COLOR_PRIMARY = 0xFF1976D2.toInt()      // HazardHawk blue
    const val COLOR_ACCENT = 0xFFFF8F00.toInt()       // Safety orange

    // Hazard box styling
    const val HAZARD_BOX_PADDING = 16f
    const val HAZARD_BOX_PADDING_TOP = 12f
    const val HAZARD_BOX_PADDING_BOTTOM = 12f
    const val HAZARD_BOX_PADDING_LEFT = 16f
    const val HAZARD_BOX_PADDING_RIGHT = 16f
    const val HAZARD_BOX_BORDER_WIDTH = 4f            // Was 3f
    const val HAZARD_BOX_BORDER_RADIUS = 4f
    const val HAZARD_BOX_CORNER_RADIUS = 4f
    const val HAZARD_BOX_MIN_HEIGHT = 80f

    // Photo sizing
    const val PHOTOS_PER_PAGE = 2
    const val PHOTO_WIDTH = 300f
    const val PHOTO_HEIGHT = 225f
    const val PHOTO_MAX_WIDTH = 500f
    const val PHOTO_MAX_HEIGHT = 350f
    const val PHOTO_CAPTION_SPACING = 8f
    const val PHOTO_METADATA_WIDTH = CONTENT_WIDTH - PHOTO_WIDTH - 20f

    // Signature styling
    const val SIGNATURE_LINE_WIDTH = 200f
    const val SIGNATURE_LINE_HEIGHT = 40f
    const val SIGNATURE_SPACING = 30f
}
