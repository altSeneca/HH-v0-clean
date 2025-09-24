package com.hazardhawk.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * OSHA-compliant color scheme for construction safety hazard visualization.
 * Based on construction industry standards and AR interface best practices.
 */
object HazardColors {
    
    // Primary hazard severity colors
    val CRITICAL_RED = Color(0xFFE53E3E)      // Fall protection, immediate danger
    val HIGH_ORANGE = Color(0xFFFF8C00)       // PPE violations, high risk
    val MEDIUM_AMBER = Color(0xFFFFA500)      // Equipment hazards, medium risk  
    val LOW_YELLOW = Color(0xFFFFD700)        // Housekeeping, low risk
    
    // OSHA and compliance colors
    val OSHA_BLUE = Color(0xFF2B6CB0)         // OSHA code backgrounds
    val SAFE_GREEN = Color(0xFF38A169)        // Compliant areas, passed inspections
    val WARNING_ORANGE = Color(0xFFED8936)    // Caution areas, attention needed
    
    // PPE status colors
    val PPE_PRESENT = Color(0xFF48BB78)       // PPE correctly worn
    val PPE_MISSING = Color(0xFFE53E3E)       // PPE not present
    val PPE_INCORRECT = Color(0xFFED8936)     // PPE worn incorrectly
    val PPE_UNKNOWN = Color(0xFF718096)       // Cannot determine PPE status
    
    // AR overlay specific colors
    val OVERLAY_BACKGROUND = Color(0xCC000000) // Semi-transparent black for badges
    val OVERLAY_BORDER = Color(0xFFFFFFFF)     // White borders for visibility
    val TEXT_PRIMARY = Color(0xFFFFFFFF)       // Primary text on overlays
    val TEXT_SECONDARY = Color(0xFFE2E8F0)     // Secondary text, lower contrast
    
    // Confidence indicators
    val HIGH_CONFIDENCE = Color(0xFF48BB78)    // >90% confidence
    val MEDIUM_CONFIDENCE = Color(0xFFED8936)  // 70-90% confidence  
    val LOW_CONFIDENCE = Color(0xFFE53E3E)     // <70% confidence
    
    // Construction work type colors (for category coding)
    val ELECTRICAL_WORK = Color(0xFF4299E1)    // Electrical hazards
    val FALL_WORK = Color(0xFFE53E3E)          // Fall protection work
    val MECHANICAL_WORK = Color(0xFF9F7AEA)    // Mechanical/equipment work
    val CHEMICAL_WORK = Color(0xFF38A169)      // Chemical/environmental work
    
    /**
     * Get severity color based on risk level.
     */
    fun getSeverityColor(severity: com.hazardhawk.ai.models.Severity): Color {
        return when (severity) {
            com.hazardhawk.ai.models.Severity.CRITICAL -> CRITICAL_RED
            com.hazardhawk.ai.models.Severity.HIGH -> HIGH_ORANGE
            com.hazardhawk.ai.models.Severity.MEDIUM -> MEDIUM_AMBER
            com.hazardhawk.ai.models.Severity.LOW -> LOW_YELLOW
        }
    }
    
    /**
     * Get PPE status color based on compliance.
     */
    fun getPPEStatusColor(status: com.hazardhawk.ai.models.PPEItemStatus): Color {
        return when (status) {
            com.hazardhawk.ai.models.PPEItemStatus.PRESENT -> PPE_PRESENT
            com.hazardhawk.ai.models.PPEItemStatus.MISSING -> PPE_MISSING
            com.hazardhawk.ai.models.PPEItemStatus.INCORRECT -> PPE_INCORRECT
            com.hazardhawk.ai.models.PPEItemStatus.UNKNOWN -> PPE_UNKNOWN
        }
    }
    
    /**
     * Get confidence level color based on AI analysis confidence.
     */
    fun getConfidenceColor(confidence: Float): Color {
        return when {
            confidence >= 0.9f -> HIGH_CONFIDENCE
            confidence >= 0.7f -> MEDIUM_CONFIDENCE
            else -> LOW_CONFIDENCE
        }
    }
    
    /**
     * Get work type color for hazard categorization.
     */
    fun getWorkTypeColor(hazardType: com.hazardhawk.ai.models.HazardType): Color {
        return when (hazardType) {
            com.hazardhawk.ai.models.HazardType.ELECTRICAL_HAZARD -> ELECTRICAL_WORK
            com.hazardhawk.ai.models.HazardType.FALL_PROTECTION -> FALL_WORK
            com.hazardhawk.ai.models.HazardType.MECHANICAL_HAZARD -> MECHANICAL_WORK
            com.hazardhawk.ai.models.HazardType.CHEMICAL_HAZARD -> CHEMICAL_WORK
            else -> MEDIUM_AMBER
        }
    }
    
    /**
     * Get alpha value for overlay backgrounds based on hazard severity.
     */
    fun getOverlayAlpha(severity: com.hazardhawk.ai.models.Severity): Float {
        return when (severity) {
            com.hazardhawk.ai.models.Severity.CRITICAL -> 0.3f   // More visible for critical
            com.hazardhawk.ai.models.Severity.HIGH -> 0.25f      // Highly visible
            com.hazardhawk.ai.models.Severity.MEDIUM -> 0.2f     // Moderately visible
            com.hazardhawk.ai.models.Severity.LOW -> 0.15f       // Subtle for low risk
        }
    }
}