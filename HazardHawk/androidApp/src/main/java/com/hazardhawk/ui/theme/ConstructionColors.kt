package com.hazardhawk.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Construction Safety Color Palette
 * 
 * Designed for high visibility and OSHA compliance in construction environments.
 * Colors optimized for outdoor visibility and accessibility.
 */
object ConstructionColors {
    // Primary Safety Colors
    val SafetyOrange = Color(0xFFFF6B35)        // ANSI Z535.1 Safety Orange
    val SafetyYellow = Color(0xFFFFD700)        // ANSI Z535.1 Safety Yellow  
    val HighVisYellow = Color(0xFFFFEB3B)       // High visibility yellow
    val CautionRed = Color(0xFFE53E3E)          // OSHA Danger Red
    val SafetyGreen = Color(0xFF38A169)         // Safety/Go indication
    val WorkZoneBlue = Color(0xFF3182CE)        // Work zone blue
    
    // Supporting Colors
    val Warning = Color(0xFFD69E2E)             // Warning amber
    val Error = Color(0xFFE53E3E)               // Error state
    val Success = Color(0xFF38A169)             // Success state
    val Info = Color(0xFF3182CE)                // Information
    
    // Neutral Construction Colors
    val ConcreteGray = Color(0xFF718096)        // Concrete/steel gray
    val SteelBlue = Color(0xFF4A5568)           // Steel structural color
    val DustBrown = Color(0xFF8B7355)           // Construction dust/earth
    val AsphaltBlack = Color(0xFF2D3748)        // Deep construction black
    
    // Background Colors
    val Surface = Color(0xFFF7FAFC)             // Light background
    val SurfaceVariant = Color(0xFFE2E8F0)      // Variant background
    val OnSurface = Color(0xFF2D3748)           // Text on surface
    val OnSurfaceVariant = Color(0xFF4A5568)    // Secondary text
    
    // Accessibility Colors (WCAG AA compliant)
    val HighContrast = Color(0xFF000000)        // Maximum contrast
    val MediumContrast = Color(0xFF2D3748)      // Medium contrast
    val LowContrast = Color(0xFF718096)         // Low contrast
    
    // Status Indicator Colors
    val OnlineGreen = Color(0xFF38A169)         // Connected/online
    val OfflineRed = Color(0xFFE53E3E)          // Disconnected/offline
    val PendingOrange = Color(0xFFFF6B35)       // Pending/loading
    val SyncBlue = Color(0xFF3182CE)            // Syncing status
}

/**
 * HazardColors - Alias for ConstructionColors for compatibility
 * 
 * This object provides the same colors as ConstructionColors
 * but with a different name for backward compatibility.
 */
object HazardColors {
    // Primary Safety Colors
    val SafetyOrange = ConstructionColors.SafetyOrange
    val SafetyYellow = ConstructionColors.SafetyYellow
    val HighVisYellow = ConstructionColors.HighVisYellow
    val CautionRed = ConstructionColors.CautionRed
    val SafetyGreen = ConstructionColors.SafetyGreen
    val WorkZoneBlue = ConstructionColors.WorkZoneBlue
    
    // Supporting Colors
    val Warning = ConstructionColors.Warning
    val Error = ConstructionColors.Error
    val Success = ConstructionColors.Success
    val Info = ConstructionColors.Info
    
    // Neutral Construction Colors
    val ConcreteGray = ConstructionColors.ConcreteGray
    val SteelBlue = ConstructionColors.SteelBlue
    val DustBrown = ConstructionColors.DustBrown
    val AsphaltBlack = ConstructionColors.AsphaltBlack
    
    // Background Colors
    val Surface = ConstructionColors.Surface
    val SurfaceVariant = ConstructionColors.SurfaceVariant
    val OnSurface = ConstructionColors.OnSurface
    val OnSurfaceVariant = ConstructionColors.OnSurfaceVariant
    
    // Accessibility Colors (WCAG AA compliant)
    val HighContrast = ConstructionColors.HighContrast
    val MediumContrast = ConstructionColors.MediumContrast
    val LowContrast = ConstructionColors.LowContrast
    
    // Status Indicator Colors
    val OnlineGreen = ConstructionColors.OnlineGreen
    val OfflineRed = ConstructionColors.OfflineRed
    val PendingOrange = ConstructionColors.PendingOrange
    val SyncBlue = ConstructionColors.SyncBlue
    
    // Additional color aliases for backward compatibility
    val OSHA_BLUE = WorkZoneBlue
    val HIGH_ORANGE = SafetyOrange
}