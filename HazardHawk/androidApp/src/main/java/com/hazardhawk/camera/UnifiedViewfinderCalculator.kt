package com.hazardhawk.camera

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.min
import kotlin.math.max

/**
 * Unified Viewfinder Calculator
 * 
 * Single source of truth for all viewfinder dimension calculations across HazardHawk.
 * Eliminates alignment inconsistencies between overlay components and provides 
 * comprehensive burnin prevention mechanisms.
 * 
 * Key Features:
 * - Corrected aspect ratio definitions (fixes inverted ratios)
 * - Safe area constraints for burnin prevention
 * - Consistent calculation logic used by all overlay components
 * - Performance-optimized with minimal object allocation
 */
object UnifiedViewfinderCalculator {
    
    /**
     * Standard viewfinder aspect ratios with corrected definitions
     * CRITICAL FIX: Previous implementation had inverted ratios (3f/4f instead of 4f/3f)
     */
    @Stable
    enum class ViewfinderAspectRatio(
        val ratio: Float, 
        val label: String,
        val description: String,
        val isStandard: Boolean = true
    ) {
        SQUARE(
            ratio = 1f, 
            label = "1:1", 
            description = "Square format for social media",
            isStandard = true
        ),
        FOUR_THREE(
            ratio = 4f / 3f,  // FIXED: Was incorrectly 3f/4f in original CameraScreen.kt
            label = "4:3", 
            description = "Standard photo format",
            isStandard = true
        ),
        SIXTEEN_NINE(
            ratio = 16f / 9f,  // FIXED: Was incorrectly 9f/16f in original CameraScreen.kt
            label = "16:9", 
            description = "Widescreen format",
            isStandard = true
        ),
        THREE_TWO(
            ratio = 3f / 2f,
            label = "3:2", 
            description = "Classic 35mm format",
            isStandard = false
        );
        
        /**
         * Get the inverse ratio for portrait orientation calculations
         */
        val portraitRatio: Float
            get() = 1f / ratio
            
        /**
         * Check if this ratio is wider than it is tall
         */
        val isLandscape: Boolean
            get() = ratio > 1f
            
        /**
         * Check if this ratio is taller than it is wide
         */
        val isPortrait: Boolean
            get() = ratio < 1f
    }
    
    /**
     * Comprehensive viewfinder bounds with burnin prevention
     * Contains all positioning information needed by overlay components
     */
    @Stable
    data class ViewfinderBounds(
        // Core dimensions
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
        val width: Float,
        val height: Float,
        val centerX: Float,
        val centerY: Float,
        
        // Canvas context
        val canvasWidth: Float,
        val canvasHeight: Float,
        val aspectRatio: ViewfinderAspectRatio,
        
        // Burnin prevention
        val safeAreaLeft: Float,
        val safeAreaTop: Float,
        val safeAreaRight: Float,
        val safeAreaBottom: Float,
        val safeAreaWidth: Float,
        val safeAreaHeight: Float
    ) {
        
        /**
         * Get viewfinder bounds as Compose Rect for Canvas operations
         */
        fun toRect(): Rect = Rect(left, top, right, bottom)
        
        /**
         * Get safe area bounds as Compose Rect (for burnin prevention)
         */
        fun safeAreaRect(): Rect = Rect(safeAreaLeft, safeAreaTop, safeAreaRight, safeAreaBottom)
        
        /**
         * Get viewfinder bounds as Android Graphics RectF
         */
        fun toAndroidRectF(): android.graphics.RectF = android.graphics.RectF(left, top, right, bottom)
        
        /**
         * Check if a point is within the viewfinder area
         */
        fun containsPoint(x: Float, y: Float): Boolean {
            return x >= left && x <= right && y >= top && y <= bottom
        }
        
        /**
         * Check if a point is within the safe area (burnin prevention)
         */
        fun safeAreaContains(x: Float, y: Float): Boolean {
            return x >= safeAreaLeft && x <= safeAreaRight && y >= safeAreaTop && y <= safeAreaBottom
        }
        
        /**
         * Get the actual calculated aspect ratio
         */
        val calculatedAspectRatio: Float
            get() = width / height
            
        /**
         * Calculate margin from canvas edges
         */
        val marginLeft: Float get() = left
        val marginTop: Float get() = top
        val marginRight: Float get() = canvasWidth - right
        val marginBottom: Float get() = canvasHeight - bottom
        
        /**
         * Positioning utilities for metadata and controls
         */
        fun topLeftCorner() = Pair(left, top)
        fun topRightCorner() = Pair(right, top)
        fun bottomLeftCorner() = Pair(left, bottom)
        fun bottomRightCorner() = Pair(right, bottom)
        fun center() = Pair(centerX, centerY)
        
        /**
         * Safe positioning utilities (burnin prevention)
         */
        fun safeTopLeft() = Pair(safeAreaLeft, safeAreaTop)
        fun safeTopRight() = Pair(safeAreaRight, safeAreaTop)
        fun safeBottomLeft() = Pair(safeAreaLeft, safeAreaBottom)
        fun safeBottomRight() = Pair(safeAreaRight, safeAreaBottom)
    }
    
    /**
     * Calculate unified viewfinder bounds for consistent overlay alignment
     * 
     * This is the core calculation function used by all overlay components to ensure
     * pixel-perfect alignment and eliminate the aspect ratio issues present in the 
     * original implementation.
     * 
     * @param canvasSize The size of the Canvas drawing area
     * @param aspectRatio The target viewfinder aspect ratio
     * @param marginFactor How much of the canvas to use (0.9 = 90% of canvas)
     * @param safeAreaMargin Additional margin for burnin prevention (in pixels)
     * @return ViewfinderBounds with all positioning information
     */
    fun calculateBounds(
        canvasSize: Size,
        aspectRatio: ViewfinderAspectRatio,
        marginFactor: Float = 0.9f,
        safeAreaMargin: Float = 16f
    ): ViewfinderBounds {
        
        val canvasWidth = canvasSize.width
        val canvasHeight = canvasSize.height
        
        // Detect device orientation and use correct aspect ratio
        val isPortraitCanvas = canvasHeight > canvasWidth
        val targetRatio = if (isPortraitCanvas) {
            // In portrait mode, use the inverse ratio for portrait display
            aspectRatio.portraitRatio
        } else {
            // In landscape mode, use the standard landscape ratio
            aspectRatio.ratio
        }
        
        // Calculate maximum available space with margins
        // Use full width in portrait mode for better composition
        val (availableWidth, availableHeight) = if (isPortraitCanvas) {
            // Full width in portrait mode for viewfinder
            val widthMarginFactor = 1.0f  // Use full width
            val heightMarginFactor = marginFactor  // Keep height margin
            Pair(canvasWidth * widthMarginFactor, canvasHeight * heightMarginFactor)
        } else {
            // Standard margins for landscape mode
            Pair(canvasWidth * marginFactor, canvasHeight * marginFactor)
        }
        
        // Determine viewfinder dimensions while maintaining aspect ratio
        val (viewfinderWidth, viewfinderHeight) = if (availableWidth / availableHeight > targetRatio) {
            // Height is the limiting factor
            val height = availableHeight
            val width = height * targetRatio
            Pair(width, height)
        } else {
            // Width is the limiting factor
            val width = availableWidth
            val height = width / targetRatio
            Pair(width, height)
        }
        
        // Position the viewfinder with reserved space for controls
        // Reserve more space at bottom for camera controls and zoom buttons
        val controlsReservedHeight = when(aspectRatio) {
            ViewfinderAspectRatio.SQUARE -> 280f // More space for 1:1 + zoom buttons
            ViewfinderAspectRatio.FOUR_THREE -> 260f // Medium space for 4:3 + zoom buttons
            ViewfinderAspectRatio.SIXTEEN_NINE -> 240f // Less space needed for 16:9 + zoom buttons
            ViewfinderAspectRatio.THREE_TWO -> 250f
        }
        
        val availableHeightForViewfinder = canvasHeight - controlsReservedHeight
        val adjustedTop = if (viewfinderHeight > availableHeightForViewfinder) {
            // If viewfinder is too tall, reduce it and center in remaining space
            val maxViewfinderHeight = availableHeightForViewfinder * 0.9f
            val adjustedViewfinderHeight = maxViewfinderHeight.coerceAtMost(viewfinderHeight)
            val adjustedViewfinderWidth = adjustedViewfinderHeight * targetRatio
            
            // Recalculate with adjusted dimensions
            val newLeft = (canvasWidth - adjustedViewfinderWidth) / 2f
            val newTop = (availableHeightForViewfinder - adjustedViewfinderHeight) / 3f // Bias toward top
            val newRight = newLeft + adjustedViewfinderWidth
            val newBottom = newTop + adjustedViewfinderHeight
            
            // Return early with adjusted dimensions
            val safeAreaLeft = newLeft + safeAreaMargin
            val safeAreaTop = newTop + safeAreaMargin
            val safeAreaRight = newRight - safeAreaMargin
            val safeAreaBottom = newBottom - safeAreaMargin
            val safeAreaWidth = safeAreaRight - safeAreaLeft
            val safeAreaHeight = safeAreaBottom - safeAreaTop
            
            return ViewfinderBounds(
                left = newLeft,
                top = newTop,
                right = newRight,
                bottom = newBottom,
                width = adjustedViewfinderWidth,
                height = adjustedViewfinderHeight,
                centerX = canvasWidth / 2f,
                centerY = canvasHeight / 2f,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                aspectRatio = aspectRatio,
                safeAreaLeft = safeAreaLeft,
                safeAreaTop = safeAreaTop,
                safeAreaRight = safeAreaRight,
                safeAreaBottom = safeAreaBottom,
                safeAreaWidth = safeAreaWidth,
                safeAreaHeight = safeAreaHeight
            )
        } else {
            // Standard positioning with reserved space at bottom
            (availableHeightForViewfinder - viewfinderHeight) / 3f // Bias toward top
        }
        
        val left = (canvasWidth - viewfinderWidth) / 2f
        val top = adjustedTop.coerceAtLeast(0f)
        val right = left + viewfinderWidth
        val bottom = top + viewfinderHeight
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        
        // Calculate safe area for burnin prevention
        val safeAreaLeft = left + safeAreaMargin
        val safeAreaTop = top + safeAreaMargin
        val safeAreaRight = right - safeAreaMargin
        val safeAreaBottom = bottom - safeAreaMargin
        val safeAreaWidth = safeAreaRight - safeAreaLeft
        val safeAreaHeight = safeAreaBottom - safeAreaTop
        
        return ViewfinderBounds(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            width = viewfinderWidth,
            height = viewfinderHeight,
            centerX = centerX,
            centerY = centerY,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            aspectRatio = aspectRatio,
            safeAreaLeft = safeAreaLeft,
            safeAreaTop = safeAreaTop,
            safeAreaRight = safeAreaRight,
            safeAreaBottom = safeAreaBottom,
            safeAreaWidth = safeAreaWidth,
            safeAreaHeight = safeAreaHeight
        )
    }
    
    /**
     * Calculate bounds for animated aspect ratio transitions
     * Used during aspect ratio changes to provide smooth transitions
     */
    fun calculateBoundsAnimated(
        canvasSize: Size,
        currentRatio: Float,
        targetAspectRatio: ViewfinderAspectRatio,
        marginFactor: Float = 0.9f,
        safeAreaMargin: Float = 16f
    ): ViewfinderBounds {
        
        val canvasWidth = canvasSize.width
        val canvasHeight = canvasSize.height
        
        // Always use portrait-appropriate ratio since app is portrait-only
        val isPortraitCanvas = canvasHeight > canvasWidth
        val correctedCurrentRatio = if (isPortraitCanvas) {
            // In portrait mode, ensure ratio is < 1 (tall rectangle)
            if (currentRatio > 1f) 1f / currentRatio else currentRatio
        } else {
            // In landscape mode, ensure ratio is > 1 (wide rectangle)  
            if (currentRatio < 1f) 1f / currentRatio else currentRatio
        }
        
        val availableWidth = canvasWidth * marginFactor
        val availableHeight = canvasHeight * marginFactor
        
        // Use corrected animated ratio for smooth transitions
        val (viewfinderWidth, viewfinderHeight) = if (availableWidth / availableHeight > correctedCurrentRatio) {
            val height = availableHeight
            val width = height * correctedCurrentRatio
            Pair(width, height)
        } else {
            val width = availableWidth
            val height = width / correctedCurrentRatio
            Pair(width, height)
        }
        
        val left = (canvasWidth - viewfinderWidth) / 2f
        val top = (canvasHeight - viewfinderHeight) / 2f
        val right = left + viewfinderWidth
        val bottom = top + viewfinderHeight
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        
        val safeAreaLeft = left + safeAreaMargin
        val safeAreaTop = top + safeAreaMargin
        val safeAreaRight = right - safeAreaMargin
        val safeAreaBottom = bottom - safeAreaMargin
        val safeAreaWidth = safeAreaRight - safeAreaLeft
        val safeAreaHeight = safeAreaBottom - safeAreaTop
        
        return ViewfinderBounds(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            width = viewfinderWidth,
            height = viewfinderHeight,
            centerX = centerX,
            centerY = centerY,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            aspectRatio = targetAspectRatio,
            safeAreaLeft = safeAreaLeft,
            safeAreaTop = safeAreaTop,
            safeAreaRight = safeAreaRight,
            safeAreaBottom = safeAreaBottom,
            safeAreaWidth = safeAreaWidth,
            safeAreaHeight = safeAreaHeight
        )
    }
    
    /**
     * Validate that calculated bounds are within acceptable parameters
     * Used for testing and debugging
     */
    fun validateBounds(bounds: ViewfinderBounds): Boolean {
        return bounds.width > 0 && 
               bounds.height > 0 &&
               bounds.left >= 0 &&
               bounds.top >= 0 &&
               bounds.right <= bounds.canvasWidth &&
               bounds.bottom <= bounds.canvasHeight &&
               bounds.safeAreaWidth > 0 &&
               bounds.safeAreaHeight > 0
    }
    
    /**
     * Calculate optimal margin factor for different screen sizes
     * Ensures consistent appearance across devices
     */
    fun calculateOptimalMarginFactor(canvasSize: Size): Float {
        val diagonal = kotlin.math.sqrt(canvasSize.width * canvasSize.width + canvasSize.height * canvasSize.height)
        return when {
            diagonal < 800f -> 0.85f    // Small phones - more margin
            diagonal < 1200f -> 0.9f    // Standard phones
            diagonal < 1800f -> 0.92f   // Large phones/small tablets
            else -> 0.95f               // Tablets - less margin
        }
    }
    
    /**
     * Convert legacy AspectRatio enum to new ViewfinderAspectRatio
     * Used during migration from old system
     */
    fun fromLegacyAspectRatio(legacyRatio: Float): ViewfinderAspectRatio {
        return when {
            kotlin.math.abs(legacyRatio - 1f) < 0.01f -> ViewfinderAspectRatio.SQUARE
            kotlin.math.abs(legacyRatio - (4f/3f)) < 0.01f -> ViewfinderAspectRatio.FOUR_THREE
            kotlin.math.abs(legacyRatio - (3f/4f)) < 0.01f -> ViewfinderAspectRatio.FOUR_THREE // Fix legacy inverted
            kotlin.math.abs(legacyRatio - (16f/9f)) < 0.01f -> ViewfinderAspectRatio.SIXTEEN_NINE
            kotlin.math.abs(legacyRatio - (9f/16f)) < 0.01f -> ViewfinderAspectRatio.SIXTEEN_NINE // Fix legacy inverted
            kotlin.math.abs(legacyRatio - (3f/2f)) < 0.01f -> ViewfinderAspectRatio.THREE_TWO
            else -> ViewfinderAspectRatio.FOUR_THREE // Default fallback
        }
    }
}