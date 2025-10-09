package com.hazardhawk.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized dimension system to replace all hardcoded .dp values
 * Based on construction-friendly design principles
 */
object ConstructionDimensions {
    // Touch targets (construction glove-friendly)
    val touchTargetMinimum: Dp = 48.dp
    val touchTargetStandard: Dp = 56.dp
    val touchTargetGloves: Dp = 64.dp
    val touchTargetEmergency: Dp = 80.dp
    
    // Camera-specific dimensions
    val cameraShutterSize: Dp = 80.dp
    val cameraControlSize: Dp = 48.dp
    val cameraMetadataHeight: Dp = 60.dp
    val zoomWheelWidth: Dp = 80.dp
    val zoomWheelHeight: Dp = 240.dp
    val wheelPositionOffset: Dp = 56.dp  // Thumb-friendly offset from edge
    
    // Standard spacing system
    val spacingNone: Dp = 0.dp
    val spacingXS: Dp = 4.dp
    val spacingS: Dp = 8.dp
    val spacingM: Dp = 16.dp
    val spacingL: Dp = 24.dp
    val spacingXL: Dp = 32.dp
    val spacingXXL: Dp = 48.dp
    
    // Component-specific spacing
    val screenPadding: Dp = 16.dp
    val cardPadding: Dp = 20.dp
    val buttonSpacing: Dp = 12.dp
    val listItemSpacing: Dp = 4.dp
    
    // Corner radii
    val cornerRadiusS: Dp = 4.dp
    val cornerRadiusM: Dp = 8.dp
    val cornerRadiusL: Dp = 16.dp
    val cornerRadiusXL: Dp = 24.dp
    
    // Elevation/shadow
    val elevationS: Dp = 2.dp
    val elevationM: Dp = 4.dp
    val elevationL: Dp = 8.dp
    
    // Icon sizes
    val iconS: Dp = 16.dp
    val iconM: Dp = 24.dp
    val iconL: Dp = 32.dp
    val iconXL: Dp = 48.dp
}

/**
 * Construction field conditions that affect UI dimensions
 */
data class FieldConditions(
    val isWearingGloves: Boolean = false,
    val isEmergencyMode: Boolean = false,
    val brightnessLevel: BrightnessLevel = BrightnessLevel.NORMAL
)

enum class BrightnessLevel {
    VERY_DARK, DARK, NORMAL, BRIGHT, VERY_BRIGHT
}

/**
 * Adaptive dimensions based on field conditions
 */
class AdaptiveDimensionProvider(private val conditions: FieldConditions) {
    val touchTarget: Dp = when {
        conditions.isEmergencyMode -> ConstructionDimensions.touchTargetEmergency
        conditions.isWearingGloves -> ConstructionDimensions.touchTargetGloves
        else -> ConstructionDimensions.touchTargetStandard
    }
    
    val spacing: Dp = when {
        conditions.isEmergencyMode -> ConstructionDimensions.spacingL
        conditions.isWearingGloves -> ConstructionDimensions.spacingM
        else -> ConstructionDimensions.spacingM
    }
}

/**
 * Composition local for accessing dimensions throughout the app
 */
val LocalConstructionDimensions = staticCompositionLocalOf { ConstructionDimensions }
val LocalFieldConditions = staticCompositionLocalOf { FieldConditions() }

@Composable
fun ProvideConstructionDimensions(
    conditions: FieldConditions = FieldConditions(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalFieldConditions provides conditions
    ) {
        content()
    }
}
