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

    // Margins (CONDENSED for 3-page layout)
    const val MARGIN_TOP = 30f             // Increased for better balance
    const val MARGIN_BOTTOM = 30f          // Increased for better balance
    const val MARGIN_LEFT = 20f            // Was 36f
    const val MARGIN_RIGHT = 20f           // Was 36f

    // Calculated dimensions
    const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT
    const val CONTENT_HEIGHT = PAGE_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM

    // Typography (CONDENSED - optimized for field readability)
    const val FONT_SIZE_TITLE = 18f        // Was 22f (-4pt)
    const val FONT_SIZE_LARGE = 14f        // Was 16f
    const val FONT_SIZE_HEADING = 14f      // Was 18f (-4pt)
    const val FONT_SIZE_SUBHEADING = 12f   // Was 14f
    const val FONT_SIZE_BODY = 12f         // Increased from 11f for better readability
    const val FONT_SIZE_SMALL = 10f        // Increased from 9f for outdoor visibility

    // Line spacing (CONDENSED - balanced for readability)
    const val LINE_SPACING_TITLE = 22f     // Was 28f
    const val LINE_SPACING_HEADING = 18f   // Was 24f
    const val LINE_SPACING_BODY = 16f      // Increased from 14f for breathing room
    const val LINE_SPACING_SMALL = 13f     // Increased from 12f

    // Section spacing (CONDENSED - improved for visual clarity)
    const val SECTION_SPACING = 16f        // Increased from 12f for better separation
    const val SUBSECTION_SPACING = 10f     // Increased from 8f
    const val PARAGRAPH_SPACING = 6f       // Was 8f

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

    // Hazard box styling (MINIMAL left-edge only design)
    const val HAZARD_LEFT_EDGE_WIDTH = 2f       // Thin colored left edge only
    const val HAZARD_BOX_PADDING = 8f           // Was 16f (condensed)
    const val HAZARD_BOX_PADDING_LEFT = 8f      // Was 16f
    const val HAZARD_BOX_PADDING_TOP = 8f       // Top padding
    const val HAZARD_BOX_PADDING_BOTTOM = 8f    // Bottom padding
    const val HAZARD_BOX_PADDING_RIGHT = 8f     // Right padding
    const val HAZARD_VERTICAL_SPACING = 10f     // Increased from 8f - space between hazards
    const val HAZARD_INDENT = 8f                // Indent from left edge
    const val HAZARD_BOX_MIN_HEIGHT = 40f       // Minimum height for hazard boxes
    const val HAZARD_BOX_BORDER_WIDTH = 2f      // Border width (legacy for old methods)

    // Photo sizing
    const val PHOTOS_PER_PAGE = 2
    const val PHOTO_WIDTH = 300f
    const val PHOTO_HEIGHT = 225f
    const val PHOTO_MAX_WIDTH = 500f
    const val PHOTO_MAX_HEIGHT = 350f
    const val PHOTO_CAPTION_SPACING = 8f
    const val PHOTO_METADATA_WIDTH = CONTENT_WIDTH - PHOTO_WIDTH - 20f

    // Signature styling (CONDENSED for compact table)
    const val SIGNATURE_LINE_WIDTH = 200f
    const val SIGNATURE_LINE_HEIGHT = 32f      // Reduced from 40f for compact table
    const val SIGNATURE_SPACING = 30f
}
