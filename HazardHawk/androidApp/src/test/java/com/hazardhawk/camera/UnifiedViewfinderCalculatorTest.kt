package com.hazardhawk.camera

import androidx.compose.ui.geometry.Size
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

/**
 * Comprehensive Unit Tests for UnifiedViewfinderCalculator
 * 
 * This test suite validates the critical viewport positioning fixes that address
 * aspect ratio inconsistencies across different screen configurations.
 * 
 * KEY TESTING AREAS:
 * - Viewport bounds calculation accuracy
 * - Aspect ratio consistency (1:1, 4:3, 16:9)  
 * - Portrait/landscape orientation handling
 * - Safe area calculations for burnin prevention
 * - Edge case validation and error handling
 * - Performance optimization validation
 */
class UnifiedViewfinderCalculatorTest {
    
    // Test canvas configurations representing common Android device screen sizes
    companion object {
        private val PHONE_PORTRAIT = Size(1080f, 1920f)      // Standard phone portrait
        private val PHONE_LANDSCAPE = Size(1920f, 1080f)     // Standard phone landscape  
        private val TABLET_PORTRAIT = Size(1600f, 2560f)     // Tablet portrait
        private val TABLET_LANDSCAPE = Size(2560f, 1600f)    // Tablet landscape
        private val SMALL_PHONE = Size(720f, 1280f)          // Small phone
        private val LARGE_TABLET = Size(2048f, 2732f)        // Large tablet (iPad Pro size)
        
        private const val FLOAT_TOLERANCE = 0.001f
    }
    
    // VIEWPORT BOUNDS CALCULATION TESTS
    
    @Test
    fun `calculateBounds returns valid dimensions for all aspect ratios on portrait phone`() {
        UnifiedViewfinderCalculator.ViewfinderAspectRatio.values().forEach { aspectRatio ->
            val bounds = UnifiedViewfinderCalculator.calculateBounds(
                canvasSize = PHONE_PORTRAIT,
                aspectRatio = aspectRatio
            )
            
            // Basic dimension validation
            assertTrue("Width must be positive for $aspectRatio", bounds.width > 0f)
            assertTrue("Height must be positive for $aspectRatio", bounds.height > 0f)
            assertTrue("Bounds must be valid for $aspectRatio", 
                UnifiedViewfinderCalculator.validateBounds(bounds))
            
            // Canvas containment validation
            assertTrue("Left boundary must be within canvas for $aspectRatio", 
                bounds.left >= 0f)
            assertTrue("Top boundary must be within canvas for $aspectRatio", 
                bounds.top >= 0f)
            assertTrue("Right boundary must be within canvas for $aspectRatio", 
                bounds.right <= bounds.canvasWidth)
            assertTrue("Bottom boundary must be within canvas for $aspectRatio", 
                bounds.bottom <= bounds.canvasHeight)
        }
    }
    
    @Test
    fun `calculateBounds maintains correct aspect ratios for portrait orientation`() {
        val testCases = listOf(
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE to 1f,
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE to (3f/4f), // Portrait mode
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE to (9f/16f) // Portrait mode
        )
        
        testCases.forEach { (aspectRatio, expectedRatio) ->
            val bounds = UnifiedViewfinderCalculator.calculateBounds(
                canvasSize = PHONE_PORTRAIT,
                aspectRatio = aspectRatio
            )
            
            val calculatedRatio = bounds.width / bounds.height
            assertEquals(
                "Aspect ratio $aspectRatio should match expected portrait ratio",
                expectedRatio, calculatedRatio, FLOAT_TOLERANCE
            )
        }
    }
    
    @Test
    fun `calculateBounds handles landscape canvas correctly`() {
        val bounds43 = UnifiedViewfinderCalculator.calculateBounds(
            canvasSize = PHONE_LANDSCAPE,
            aspectRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE
        )
        
        val bounds169 = UnifiedViewfinderCalculator.calculateBounds(
            canvasSize = PHONE_LANDSCAPE,
            aspectRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE
        )
        
        // In landscape, 16:9 should be wider than 4:3
        assertTrue("16:9 should be wider than 4:3 in landscape", 
            bounds169.width >= bounds43.width)
            
        // Both should maintain valid ratios
        assertTrue("4:3 landscape bounds should be valid", 
            UnifiedViewfinderCalculator.validateBounds(bounds43))
        assertTrue("16:9 landscape bounds should be valid", 
            UnifiedViewfinderCalculator.validateBounds(bounds169))
    }
    
    @Test
    fun `calculateBounds centers viewfinder horizontally`() {
        val aspectRatios = UnifiedViewfinderCalculator.ViewfinderAspectRatio.values()
        
        aspectRatios.forEach { aspectRatio ->
            val bounds = UnifiedViewfinderCalculator.calculateBounds(
                canvasSize = PHONE_PORTRAIT,
                aspectRatio = aspectRatio
            )
            
            val leftMargin = bounds.left
            val rightMargin = bounds.canvasWidth - bounds.right
            
            assertEquals(
                "Viewfinder should be horizontally centered for $aspectRatio",
                leftMargin, rightMargin, FLOAT_TOLERANCE
            )
        }
    }
    
    @Test
    fun `calculateBounds positions viewfinder at top of screen`() {
        val bounds = UnifiedViewfinderCalculator.calculateBounds(
            canvasSize = PHONE_PORTRAIT,
            aspectRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE
        )
        
        assertEquals("Viewfinder should be positioned at top of screen", 
            0f, bounds.top, FLOAT_TOLERANCE)
    }
    
    // ASPECT RATIO CONSISTENCY TESTS
    
    @Test
    fun `aspect ratio enum values are mathematically correct`() {
        assertEquals("Square ratio", 1f, 
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE.ratio, FLOAT_TOLERANCE)
        assertEquals("4:3 ratio", 4f/3f, 
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE.ratio, FLOAT_TOLERANCE)
        assertEquals("16:9 ratio", 16f/9f, 
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE.ratio, FLOAT_TOLERANCE)
        assertEquals("3:2 ratio", 3f/2f, 
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.THREE_TWO.ratio, FLOAT_TOLERANCE)
    }
    
    @Test
    fun `portrait ratio calculations are correct`() {
        assertEquals("Square portrait ratio", 1f,
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE.portraitRatio, FLOAT_TOLERANCE)
        assertEquals("4:3 portrait ratio", 3f/4f,
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE.portraitRatio, FLOAT_TOLERANCE)
        assertEquals("16:9 portrait ratio", 9f/16f,
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE.portraitRatio, FLOAT_TOLERANCE)
    }
    
    @Test
    fun `aspect ratio orientation detection works correctly`() {
        assertTrue("Square should not be landscape", 
            !UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE.isLandscape)
        assertTrue("4:3 should be landscape", 
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE.isLandscape)
        assertTrue("16:9 should be landscape", 
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE.isLandscape)
            
        assertTrue("Square should not be portrait", 
            !UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE.isPortrait)
        assertTrue("4:3 should not be portrait (landscape format)", 
            !UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE.isPortrait)
        assertTrue("16:9 should not be portrait (landscape format)", 
            !UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE.isPortrait)
    }
    
    // SAFE AREA AND BURNIN PREVENTION TESTS
    
    @Test
    fun `safe area calculations provide adequate margins`() {
        val bounds = UnifiedViewfinderCalculator.calculateBounds(
            canvasSize = PHONE_PORTRAIT,
            aspectRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE,
            safeAreaMargin = 20f
        )
        
        // Verify safe area is properly inset
        assertEquals("Safe area left margin", 20f, bounds.safeAreaLeft - bounds.left, FLOAT_TOLERANCE)
        assertEquals("Safe area top margin", 20f, bounds.safeAreaTop - bounds.top, FLOAT_TOLERANCE)
        assertEquals("Safe area right margin", 20f, bounds.right - bounds.safeAreaRight, FLOAT_TOLERANCE)
        assertEquals("Safe area bottom margin", 20f, bounds.bottom - bounds.safeAreaBottom, FLOAT_TOLERANCE)
        
        // Verify safe area dimensions are positive
        assertTrue("Safe area width should be positive", bounds.safeAreaWidth > 0f)
        assertTrue("Safe area height should be positive", bounds.safeAreaHeight > 0f)
    }
    
    @Test
    fun `safe area contains point detection works correctly`() {
        val bounds = UnifiedViewfinderCalculator.calculateBounds(
            canvasSize = PHONE_PORTRAIT,
            aspectRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE,
            safeAreaMargin = 16f
        )
        
        // Point in safe area
        assertTrue("Point in safe area should be detected", 
            bounds.safeAreaContains(bounds.safeAreaLeft + 10f, bounds.safeAreaTop + 10f))
        
        // Point outside safe area but inside viewfinder
        assertFalse("Point outside safe area should be rejected", 
            bounds.safeAreaContains(bounds.left + 5f, bounds.top + 5f))
        
        // Point outside viewfinder entirely
        assertFalse("Point outside viewfinder should be rejected", 
            bounds.safeAreaContains(bounds.left - 10f, bounds.top - 10f))
    }
    
    // DIFFERENT SCREEN SIZE VALIDATION
    
    @Test
    fun `bounds calculation works across different screen sizes`() {
        val screenSizes = listOf(SMALL_PHONE, PHONE_PORTRAIT, TABLET_PORTRAIT, LARGE_TABLET)
        
        screenSizes.forEach { screenSize ->
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.values().forEach { aspectRatio ->
                val bounds = UnifiedViewfinderCalculator.calculateBounds(
                    canvasSize = screenSize,
                    aspectRatio = aspectRatio
                )
                
                assertTrue("Bounds should be valid for screen ${screenSize.width}x${screenSize.height} with $aspectRatio", 
                    UnifiedViewfinderCalculator.validateBounds(bounds))
                
                // Verify reasonable sizing
                assertTrue("Width should be reasonable proportion of screen", 
                    bounds.width >= screenSize.width * 0.5f)
                assertTrue("Height should be reasonable proportion of screen", 
                    bounds.height >= screenSize.height * 0.3f)
            }
        }
    }
    
    @Test
    fun `optimal margin factor calculation scales with screen size`() {
        val smallMargin = UnifiedViewfinderCalculator.calculateOptimalMarginFactor(SMALL_PHONE)
        val phoneMargin = UnifiedViewfinderCalculator.calculateOptimalMarginFactor(PHONE_PORTRAIT)
        val tabletMargin = UnifiedViewfinderCalculator.calculateOptimalMarginFactor(LARGE_TABLET)
        
        // Smaller screens should have more conservative margins
        assertTrue("Small phone should have smaller margin factor than regular phone", 
            smallMargin <= phoneMargin)
        assertTrue("Phone should have smaller margin factor than tablet", 
            phoneMargin <= tabletMargin)
            
        // All margins should be reasonable
        assertTrue("All margin factors should be between 0.8 and 1.0", 
            listOf(smallMargin, phoneMargin, tabletMargin).all { it in 0.8f..1.0f })
    }
    
    // ANIMATED BOUNDS CALCULATION TESTS
    
    @Test
    fun `animated bounds calculation handles smooth transitions`() {
        val startRatio = 1f // Square
        val targetRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE
        
        val animatedBounds = UnifiedViewfinderCalculator.calculateBoundsAnimated(
            canvasSize = PHONE_PORTRAIT,
            currentRatio = startRatio,
            targetAspectRatio = targetRatio
        )
        
        assertTrue("Animated bounds should be valid", 
            UnifiedViewfinderCalculator.validateBounds(animatedBounds))
        
        // Should maintain consistent positioning
        assertEquals("Animated bounds should maintain top positioning", 
            0f, animatedBounds.top, FLOAT_TOLERANCE)
        
        // Should maintain horizontal centering
        val leftMargin = animatedBounds.left
        val rightMargin = animatedBounds.canvasWidth - animatedBounds.right
        assertEquals("Animated bounds should maintain horizontal centering", 
            leftMargin, rightMargin, FLOAT_TOLERANCE)
    }
    
    @Test
    fun `animated bounds handles portrait orientation correction`() {
        val landscapeRatio = 16f / 9f // Landscape ratio
        val targetRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE
        
        val bounds = UnifiedViewfinderCalculator.calculateBoundsAnimated(
            canvasSize = PHONE_PORTRAIT,
            currentRatio = landscapeRatio,
            targetAspectRatio = targetRatio
        )
        
        // Should correct to portrait-appropriate ratio
        val actualRatio = bounds.width / bounds.height
        assertTrue("Should use portrait-corrected ratio", actualRatio < 1f)
        
        assertTrue("Corrected bounds should be valid", 
            UnifiedViewfinderCalculator.validateBounds(bounds))
    }
    
    // LEGACY COMPATIBILITY TESTS
    
    @Test
    fun `legacy aspect ratio conversion handles inverted ratios correctly`() {
        // Test correct ratios
        assertEquals("Correct 4:3 should map to FOUR_THREE",
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE,
            UnifiedViewfinderCalculator.fromLegacyAspectRatio(4f/3f))
        
        // Test inverted legacy ratios (the bug we're fixing)
        assertEquals("Legacy inverted 3:4 should map to FOUR_THREE",
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE,
            UnifiedViewfinderCalculator.fromLegacyAspectRatio(3f/4f))
            
        assertEquals("Legacy inverted 9:16 should map to SIXTEEN_NINE",
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE,
            UnifiedViewfinderCalculator.fromLegacyAspectRatio(9f/16f))
    }
    
    // EDGE CASE AND ERROR HANDLING TESTS
    
    @Test
    fun `bounds validation catches invalid dimensions`() {
        val invalidBounds = UnifiedViewfinderCalculator.ViewfinderBounds(
            left = 0f, top = 0f, right = -100f, bottom = 100f, // Negative width
            width = -100f, height = 100f, centerX = 50f, centerY = 50f,
            canvasWidth = 1000f, canvasHeight = 1000f,
            aspectRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE,
            safeAreaLeft = 10f, safeAreaTop = 10f, 
            safeAreaRight = 90f, safeAreaBottom = 90f,
            safeAreaWidth = 80f, safeAreaHeight = 80f
        )
        
        assertFalse("Should reject bounds with negative width", 
            UnifiedViewfinderCalculator.validateBounds(invalidBounds))
    }
    
    @Test
    fun `bounds calculation handles extreme aspect ratios gracefully`() {
        val extremeRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE
        
        // Very wide canvas
        val wideCanvas = Size(4000f, 800f)
        val wideBounds = UnifiedViewfinderCalculator.calculateBounds(wideCanvas, extremeRatio)
        assertTrue("Should handle wide canvas", 
            UnifiedViewfinderCalculator.validateBounds(wideBounds))
        
        // Very tall canvas  
        val tallCanvas = Size(800f, 4000f)
        val tallBounds = UnifiedViewfinderCalculator.calculateBounds(tallCanvas, extremeRatio)
        assertTrue("Should handle tall canvas", 
            UnifiedViewfinderCalculator.validateBounds(tallBounds))
    }
    
    // UTILITY METHOD TESTS
    
    @Test
    fun `viewfinder bounds utility methods work correctly`() {
        val bounds = UnifiedViewfinderCalculator.calculateBounds(
            canvasSize = PHONE_PORTRAIT,
            aspectRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE
        )
        
        // Test corner calculations
        val (tlX, tlY) = bounds.topLeftCorner()
        assertEquals("Top left X", bounds.left, tlX, FLOAT_TOLERANCE)
        assertEquals("Top left Y", bounds.top, tlY, FLOAT_TOLERANCE)
        
        val (brX, brY) = bounds.bottomRightCorner()
        assertEquals("Bottom right X", bounds.right, brX, FLOAT_TOLERANCE)
        assertEquals("Bottom right Y", bounds.bottom, brY, FLOAT_TOLERANCE)
        
        // Test center calculation
        val (centerX, centerY) = bounds.center()
        assertEquals("Center X", bounds.centerX, centerX, FLOAT_TOLERANCE)
        assertEquals("Center Y", bounds.centerY, centerY, FLOAT_TOLERANCE)
        
        // Test margin calculations
        assertEquals("Left margin", bounds.left, bounds.marginLeft, FLOAT_TOLERANCE)
        assertEquals("Top margin", bounds.top, bounds.marginTop, FLOAT_TOLERANCE)
        assertEquals("Right margin", bounds.canvasWidth - bounds.right, bounds.marginRight, FLOAT_TOLERANCE)
        assertEquals("Bottom margin", bounds.canvasHeight - bounds.bottom, bounds.marginBottom, FLOAT_TOLERANCE)
    }
    
    @Test
    fun `point containment detection works correctly`() {
        val bounds = UnifiedViewfinderCalculator.calculateBounds(
            canvasSize = PHONE_PORTRAIT,
            aspectRatio = UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE
        )
        
        // Point inside viewfinder
        assertTrue("Point inside should be detected", 
            bounds.containsPoint(bounds.centerX, bounds.centerY))
        
        // Point on border (should be included)
        assertTrue("Point on left border should be included", 
            bounds.containsPoint(bounds.left, bounds.centerY))
        assertTrue("Point on right border should be included", 
            bounds.containsPoint(bounds.right, bounds.centerY))
        
        // Point outside viewfinder
        assertFalse("Point outside left should be rejected", 
            bounds.containsPoint(bounds.left - 1f, bounds.centerY))
        assertFalse("Point outside right should be rejected", 
            bounds.containsPoint(bounds.right + 1f, bounds.centerY))
    }
    
    // PERFORMANCE VALIDATION TESTS
    
    @Test
    fun `bounds calculation is performant for multiple calls`() {
        val startTime = System.currentTimeMillis()
        
        // Simulate rapid aspect ratio changes (e.g., during animation)
        repeat(1000) {
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.values().forEach { aspectRatio ->
                UnifiedViewfinderCalculator.calculateBounds(
                    canvasSize = PHONE_PORTRAIT,
                    aspectRatio = aspectRatio
                )
            }
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Should complete 4000 calculations (1000 * 4 aspect ratios) quickly
        assertTrue("Bounds calculations should be performant (completed in ${duration}ms)", 
            duration < 1000) // Less than 1 second for 4000 calculations
    }
    
    @Test
    fun `calculated aspect ratio matches expected values`() {
        val testCases = mapOf(
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SQUARE to 1f,
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.FOUR_THREE to (3f/4f), // Portrait
            UnifiedViewfinderCalculator.ViewfinderAspectRatio.SIXTEEN_NINE to (9f/16f) // Portrait
        )
        
        testCases.forEach { (aspectRatio, expected) ->
            val bounds = UnifiedViewfinderCalculator.calculateBounds(
                canvasSize = PHONE_PORTRAIT,
                aspectRatio = aspectRatio
            )
            
            assertEquals("Calculated aspect ratio for $aspectRatio", 
                expected, bounds.calculatedAspectRatio, FLOAT_TOLERANCE)
        }
    }
}
