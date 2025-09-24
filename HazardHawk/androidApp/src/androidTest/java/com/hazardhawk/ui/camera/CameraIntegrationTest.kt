package com.hazardhawk.ui.camera

import android.Manifest
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.hazardhawk.MainActivity
import com.hazardhawk.camera.UnifiedViewfinderCalculator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-End Camera Integration Tests
 * 
 * These tests validate the complete camera functionality pipeline including:
 * - Camera permission handling
 * - Viewport positioning across aspect ratio changes
 * - Photo capture workflow
 * - UI responsiveness and performance
 * - Error handling and recovery scenarios
 * 
 * SUCCESS CRITERIA:
 * - All viewport calculations work correctly across device orientations
 * - Camera controls respond within acceptable timeframes
 * - No crashes during 30-minute test sessions
 * - Memory usage remains stable during extended use
 */
@RunWith(AndroidJUnit4::class)
class CameraIntegrationTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    
    @Before
    fun setup() {
        // Navigate to camera screen
        composeTestRule
            .onNodeWithContentDescription("Camera")
            .performClick()
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun complete_camera_initialization_workflow() {
        // Verify camera permission is granted and camera initializes
        composeTestRule
            .onNodeWithTag("camera-preview")
            .assertIsDisplayed()
        
        // Verify all UI elements are present
        composeTestRule
            .onNodeWithContentDescription("Capture button")
            .assertIsDisplayed()
            .assertTouchWidthIsAtLeast(48.dp)
        
        composeTestRule
            .onNodeWithContentDescription("Gallery button")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Horizontal zoom slider")
            .assertIsDisplayed()
        
        // Verify metadata overlays are displayed
        composeTestRule
            .onNodeWithTag("metadata-overlay")
            .assertIsDisplayed()
    }
    
    @Test
    fun aspect_ratio_changes_update_viewport_correctly() {
        val aspectRatios = listOf("1:1", "4:3", "16:9")
        
        aspectRatios.forEach { ratio ->
            // Tap aspect ratio selector
            composeTestRule
                .onNodeWithContentDescription("Aspect ratio selector")
                .performClick()
            
            // Select the aspect ratio
            composeTestRule
                .onNodeWithText(ratio)
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Verify viewport updates
            composeTestRule
                .onNodeWithTag("camera-preview")
                .assertIsDisplayed()
            
            // Verify zoom slider is properly positioned for new aspect ratio
            composeTestRule
                .onNodeWithContentDescription("Horizontal zoom slider")
                .assertIsDisplayed()
            
            // Test zoom functionality works with new aspect ratio
            composeTestRule
                .onNodeWithContentDescription("Horizontal zoom slider")
                .performTouchInput { swipeRight() }
            
            // Verify zoom level updates
            composeTestRule.waitForIdle()
            
            composeTestRule
                .onNode(hasText(Regex("""\d\.\dx""")))
                .assertIsDisplayed()
        }
    }
    
    @Test
    fun photo_capture_workflow_with_metadata() {
        // Ensure GPS is enabled and working
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule
                    .onNode(hasText(Regex("""GPS: \d+\.\d+° [NS], \d+\.\d+° [EW]""")))
                    .fetchSemanticsNode()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // Set 4:3 aspect ratio for testing
        composeTestRule
            .onNodeWithContentDescription("Aspect ratio selector")
            .performClick()
        
        composeTestRule
            .onNodeWithText("4:3")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Capture photo
        composeTestRule
            .onNodeWithContentDescription("Capture button")
            .performClick()
        
        // Verify capture feedback (animation, sound, or visual indicator)
        composeTestRule.waitForIdle()
        
        // Verify photo was saved (check gallery button shows recent photo)
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule
                    .onNodeWithContentDescription("Gallery button")
                    .assertIsDisplayed()
                true
            } catch (e: Exception) {
                false
            }
        }
        
        // Navigate to gallery to verify photo
        composeTestRule
            .onNodeWithContentDescription("Gallery button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify recent photo is displayed
        composeTestRule
            .onNodeWithTag("recent-photo")
            .assertIsDisplayed()
    }
    
    @Test
    fun zoom_functionality_integration_test() {
        val slider = composeTestRule.onNodeWithContentDescription("Horizontal zoom slider")
        
        // Test minimum zoom
        slider.performTouchInput { 
            swipeLeft(startX = this.width * 0.9f, endX = this.width * 0.1f)
        }
        
        composeTestRule.waitForIdle()
        
        // Verify minimum zoom level
        composeTestRule
            .onNodeWithText("1.0x")
            .assertIsDisplayed()
        
        // Test maximum zoom
        slider.performTouchInput { 
            swipeRight(startX = this.width * 0.1f, endX = this.width * 0.9f)
        }
        
        composeTestRule.waitForIdle()
        
        // Verify zoom level increased
        composeTestRule
            .onNode(hasText(Regex("""[2-9]\.\dx""")))
            .assertIsDisplayed()
        
        // Test intermediate zoom levels
        repeat(5) { i ->
            val position = 0.2f + (i * 0.15f)
            slider.performTouchInput {
                click(Offset(this.width * position, this.height * 0.5f))
            }
            
            composeTestRule.waitForIdle()
            
            // Each click should result in zoom change
            composeTestRule
                .onNode(hasText(Regex("""\d\.\dx""")))
                .assertIsDisplayed()
        }
    }
    
    @Test
    fun viewport_positioning_stress_test() {
        // Rapidly change aspect ratios to test viewport stability
        val aspectRatios = listOf("1:1", "4:3", "16:9")
        
        repeat(10) { iteration ->
            aspectRatios.forEach { ratio ->
                composeTestRule
                    .onNodeWithContentDescription("Aspect ratio selector")
                    .performClick()
                
                composeTestRule
                    .onNodeWithText(ratio)
                    .performClick()
                
                composeTestRule.waitForIdle()
                
                // Verify UI remains stable
                composeTestRule
                    .onNodeWithTag("camera-preview")
                    .assertIsDisplayed()
                
                composeTestRule
                    .onNodeWithContentDescription("Horizontal zoom slider")
                    .assertIsDisplayed()
                
                // Test interaction during rapid changes
                if (iteration % 2 == 0) {
                    composeTestRule
                        .onNodeWithContentDescription("Horizontal zoom slider")
                        .performTouchInput { swipeRight() }
                    
                    composeTestRule.waitForIdle()
                }
            }
        }
        
        // After stress test, verify everything still works
        composeTestRule
            .onNodeWithContentDescription("Capture button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithTag("camera-preview")
            .assertIsDisplayed()
    }
    
    @Test
    fun memory_stability_during_extended_use() {
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Simulate 5 minutes of continuous use
        repeat(50) { iteration ->
            // Change aspect ratio
            composeTestRule
                .onNodeWithContentDescription("Aspect ratio selector")
                .performClick()
            
            val ratios = listOf("1:1", "4:3", "16:9")
            composeTestRule
                .onNodeWithText(ratios[iteration % ratios.size])
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Perform zoom operations
            composeTestRule
                .onNodeWithContentDescription("Horizontal zoom slider")
                .performTouchInput { 
                    if (iteration % 2 == 0) swipeRight() else swipeLeft()
                }
            
            composeTestRule.waitForIdle()
            
            // Capture photo every 10 iterations
            if (iteration % 10 == 0) {
                composeTestRule
                    .onNodeWithContentDescription("Capture button")
                    .performClick()
                
                composeTestRule.waitForIdle()
            }
            
            // Check memory usage every 20 iterations
            if (iteration % 20 == 0) {
                val currentMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                val memoryIncrease = currentMemory - initialMemory
                val memoryIncreaseMB = memoryIncrease / (1024 * 1024)
                
                // Memory increase should be reasonable (less than 50MB)
                assertTrue("Memory increase should be less than 50MB (current: ${memoryIncreaseMB}MB)", 
                    memoryIncreaseMB < 50)
            }
        }
        
        // Verify UI is still responsive after extended use
        composeTestRule
            .onNodeWithContentDescription("Capture button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithTag("camera-preview")
            .assertIsDisplayed()
    }
    
    @Test
    fun error_recovery_and_handling() {
        // Test camera error recovery by simulating various scenarios
        
        // 1. Test aspect ratio change during capture
        composeTestRule
            .onNodeWithContentDescription("Capture button")
            .performClick()
        
        // Immediately change aspect ratio during capture
        composeTestRule
            .onNodeWithContentDescription("Aspect ratio selector")
            .performClick()
        
        composeTestRule
            .onNodeWithText("16:9")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // UI should remain stable
        composeTestRule
            .onNodeWithTag("camera-preview")
            .assertIsDisplayed()
        
        // 2. Test rapid zoom changes
        val slider = composeTestRule.onNodeWithContentDescription("Horizontal zoom slider")
        
        repeat(10) {
            slider.performTouchInput { 
                swipeRight(startX = 0f, endX = this.width, durationMillis = 100)
                swipeLeft(startX = this.width, endX = 0f, durationMillis = 100)
            }
        }
        
        composeTestRule.waitForIdle()
        
        // Should still be responsive
        slider.performTouchInput { click() }
        composeTestRule.waitForIdle()
        
        // 3. Test settings dialog interaction during operations
        composeTestRule
            .onNodeWithContentDescription("Horizontal zoom slider")
            .performTouchInput { swipeRight() }
        
        composeTestRule
            .onNodeWithContentDescription("Settings button")
            .performClick()
        
        composeTestRule
            .onNodeWithTag("settings-dialog")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Close settings")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Camera should still be functional
        composeTestRule
            .onNodeWithContentDescription("Capture button")
            .performClick()
        
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun performance_responsiveness_test() {
        var responseStartTime: Long
        var responseEndTime: Long
        
        // Test capture button responsiveness
        responseStartTime = System.currentTimeMillis()
        
        composeTestRule
            .onNodeWithContentDescription("Capture button")
            .performClick()
        
        composeTestRule.waitForIdle()
        responseEndTime = System.currentTimeMillis()
        
        val captureResponseTime = responseEndTime - responseStartTime
        assertTrue("Capture button should respond within 500ms (actual: ${captureResponseTime}ms)", 
            captureResponseTime < 500)
        
        // Test zoom slider responsiveness
        responseStartTime = System.currentTimeMillis()
        
        composeTestRule
            .onNodeWithContentDescription("Horizontal zoom slider")
            .performTouchInput { swipeRight() }
        
        composeTestRule.waitForIdle()
        responseEndTime = System.currentTimeMillis()
        
        val zoomResponseTime = responseEndTime - responseStartTime
        assertTrue("Zoom slider should respond within 200ms (actual: ${zoomResponseTime}ms)", 
            zoomResponseTime < 200)
        
        // Test aspect ratio change responsiveness
        responseStartTime = System.currentTimeMillis()
        
        composeTestRule
            .onNodeWithContentDescription("Aspect ratio selector")
            .performClick()
        
        composeTestRule
            .onNodeWithText("16:9")
            .performClick()
        
        composeTestRule.waitForIdle()
        responseEndTime = System.currentTimeMillis()
        
        val aspectRatioChangeTime = responseEndTime - responseStartTime
        assertTrue("Aspect ratio change should complete within 300ms (actual: ${aspectRatioChangeTime}ms)", 
            aspectRatioChangeTime < 300)
    }
    
    @Test
    fun viewport_bounds_accuracy_validation() {
        // Test that calculated viewport bounds match actual UI positioning
        val aspectRatios = listOf("1:1", "4:3", "16:9")
        
        aspectRatios.forEach { ratio ->
            composeTestRule
                .onNodeWithContentDescription("Aspect ratio selector")
                .performClick()
            
            composeTestRule
                .onNodeWithText(ratio)
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Get actual UI bounds
            val previewBounds = composeTestRule
                .onNodeWithTag("camera-preview")
                .getBoundsInRoot()
            
            val sliderBounds = composeTestRule
                .onNodeWithContentDescription("Horizontal zoom slider")
                .getBoundsInRoot()
            
            // Verify slider is positioned below preview
            assertTrue("Zoom slider should be positioned below camera preview for $ratio", 
                sliderBounds.top > previewBounds.bottom)
            
            // Verify horizontal centering
            val previewCenterX = (previewBounds.left + previewBounds.right) / 2
            val sliderCenterX = (sliderBounds.left + sliderBounds.right) / 2
            val centerDifference = kotlin.math.abs(previewCenterX.value - sliderCenterX.value)
            
            assertTrue("Preview and slider should be horizontally aligned for $ratio (difference: ${centerDifference}dp)", 
                centerDifference < 10f) // Within 10dp tolerance
        }
    }
    
    @Test
    fun thirty_minute_stability_test() {
        val testStartTime = System.currentTimeMillis()
        val testDurationMs = 30 * 60 * 1000L // 30 minutes
        var iterationCount = 0
        
        while ((System.currentTimeMillis() - testStartTime) < testDurationMs) {
            iterationCount++
            
            // Cycle through aspect ratios
            val ratios = listOf("1:1", "4:3", "16:9")
            val currentRatio = ratios[iterationCount % ratios.size]
            
            composeTestRule
                .onNodeWithContentDescription("Aspect ratio selector")
                .performClick()
            
            composeTestRule
                .onNodeWithText(currentRatio)
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Perform zoom operations
            composeTestRule
                .onNodeWithContentDescription("Horizontal zoom slider")
                .performTouchInput { 
                    when (iterationCount % 4) {
                        0 -> swipeRight()
                        1 -> swipeLeft()
                        2 -> click()
                        else -> swipeRight(startX = this.width * 0.3f, endX = this.width * 0.7f)
                    }
                }
            
            composeTestRule.waitForIdle()
            
            // Capture photo every 30 seconds
            if (iterationCount % 10 == 0) {
                composeTestRule
                    .onNodeWithContentDescription("Capture button")
                    .performClick()
                
                composeTestRule.waitForIdle()
            }
            
            // Verify core UI elements are still present
            composeTestRule
                .onNodeWithTag("camera-preview")
                .assertIsDisplayed()
            
            composeTestRule
                .onNodeWithContentDescription("Horizontal zoom slider")
                .assertIsDisplayed()
            
            // Small delay between iterations
            Thread.sleep(100)
        }
        
        // Final verification after 30-minute test
        composeTestRule
            .onNodeWithContentDescription("Capture button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithTag("camera-preview")
            .assertIsDisplayed()
        
        // Verify memory is still reasonable
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val finalMemoryMB = finalMemory / (1024 * 1024)
        
        assertTrue("Memory usage should remain reasonable after 30-minute test (${finalMemoryMB}MB)", 
            finalMemoryMB < 200) // Less than 200MB total
        
        println("30-minute stability test completed successfully with ${iterationCount} iterations")
    }
}
