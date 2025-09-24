package com.hazardhawk.ui.camera.wheel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.camera.CameraStateManager
import com.hazardhawk.data.repositories.CameraSettingsRepository
import com.hazardhawk.ui.camera.DualVerticalSelectors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import kotlin.system.measureTimeMillis

/**
 * Stress tests for camera wheel performance, edge cases, and error scenarios
 * Tests system behavior under extreme conditions and concurrent interactions
 */
@RunWith(AndroidJUnit4::class)
class CameraWheelStressTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockRepository = mock<CameraSettingsRepository>()
    private val testScope = CoroutineScope(SupervisorJob())
    private val testStateManager = CameraStateManager(mockRepository, testScope)

    @Test
    fun testRapidWheelScrollingAndSelectionChanges() {
        var zoomChangeCount = 0
        var aspectRatioChangeCount = 0
        val capturedZoomValues = mutableListOf<Float>()

        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, _ -> aspectRatioChangeCount++ },
                onZoomChange = { zoom -> 
                    zoomChangeCount++
                    capturedZoomValues.add(zoom)
                },
                onZoomLive = { },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Rapid zoom wheel scrolling
        val zoomWheel = composeTestRule.onNode(hasContentDescription("Zoom control"))
        
        repeat(20) { index ->
            zoomWheel.performTouchInput {
                down(center)
                moveBy(Offset(0f, if (index % 2 == 0) 50f else -50f))
                up()
            }
            // Small delay to allow processing
            composeTestRule.waitForIdle()
        }

        // Rapid aspect ratio wheel scrolling
        val aspectRatioWheel = composeTestRule.onNode(hasContentDescription("Aspect ratio selector"))
        
        repeat(15) { index ->
            aspectRatioWheel.performTouchInput {
                down(center)
                moveBy(Offset(0f, if (index % 2 == 0) 60f else -60f))
                up()
            }
            composeTestRule.waitForIdle()
        }

        // Verify system handled rapid inputs gracefully
        assert(zoomChangeCount > 0) { "Should have registered zoom changes" }
        assert(aspectRatioChangeCount > 0) { "Should have registered aspect ratio changes" }
        assert(capturedZoomValues.isNotEmpty()) { "Should have captured zoom values" }

        // Final state should be consistent
        val finalZoom = capturedZoomValues.lastOrNull()
        assert(finalZoom != null && finalZoom > 0f) { "Final zoom should be valid" }
    }

    @Test
    fun testConcurrentWheelInteractions() {
        var simultaneousInteractionDetected = false
        val interactionTimestamps = mutableListOf<Long>()

        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, _ -> 
                    interactionTimestamps.add(System.currentTimeMillis())
                },
                onZoomChange = { 
                    interactionTimestamps.add(System.currentTimeMillis())
                },
                onZoomLive = { 
                    interactionTimestamps.add(System.currentTimeMillis())
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        val aspectRatioWheel = composeTestRule.onNode(hasContentDescription("Aspect ratio selector"))
        val zoomWheel = composeTestRule.onNode(hasContentDescription("Zoom control"))

        // Simulate simultaneous interactions on both wheels
        aspectRatioWheel.performTouchInput {
            down(center)
        }

        zoomWheel.performTouchInput {
            down(center)
        }

        // Move both simultaneously
        aspectRatioWheel.performTouchInput {
            moveBy(Offset(0f, 100f))
        }

        zoomWheel.performTouchInput {
            moveBy(Offset(0f, -100f))
        }

        // Release both
        aspectRatioWheel.performTouchInput { up() }
        zoomWheel.performTouchInput { up() }

        composeTestRule.waitForIdle()

        // Check for simultaneous interactions (within 100ms of each other)
        if (interactionTimestamps.size >= 2) {
            val sortedTimestamps = interactionTimestamps.sorted()
            for (i in 0 until sortedTimestamps.size - 1) {
                if (sortedTimestamps[i + 1] - sortedTimestamps[i] < 100) {
                    simultaneousInteractionDetected = true
                    break
                }
            }
        }

        // Verify system handled concurrent interactions without conflicts
        assert(interactionTimestamps.isNotEmpty()) { "Should have registered interactions" }
    }

    @Test
    fun testMemoryPressurePerformance() {
        // Simulate memory pressure by allocating large amounts of memory
        val memoryConsumer = mutableListOf<ByteArray>()
        
        try {
            // Allocate memory to simulate pressure (100MB)
            repeat(100) {
                memoryConsumer.add(ByteArray(1024 * 1024)) // 1MB chunks
            }

            var performanceAcceptable = true
            val frameDrops = mutableListOf<Long>()

            composeTestRule.setContent {
                DualVerticalSelectors(
                    cameraStateManager = testStateManager,
                    onAspectRatioChange = { _, _ -> },
                    onZoomChange = { },
                    onZoomLive = { },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Perform wheel operations under memory pressure
            val zoomWheel = composeTestRule.onNode(hasContentDescription("Zoom control"))
            
            repeat(50) { index ->
                val operationTime = measureTimeMillis {
                    zoomWheel.performTouchInput {
                        down(center)
                        moveBy(Offset(0f, (index % 10) * 10f))
                        up()
                    }
                    composeTestRule.waitForIdle()
                }

                // Track operations that take too long (indicating frame drops)
                if (operationTime > 32) { // Longer than ~30fps frame time
                    frameDrops.add(operationTime)
                }

                // If too many frame drops, performance is not acceptable
                if (frameDrops.size > 15) { // Allow some drops but not excessive
                    performanceAcceptable = false
                    break
                }
            }

            assert(performanceAcceptable) { 
                "Performance degraded under memory pressure. Frame drops: ${frameDrops.size}" 
            }

        } finally {
            // Clean up memory
            memoryConsumer.clear()
            System.gc()
        }
    }

    @Test
    fun testDeviceRotationStatePreservation() {
        var currentZoom = 1f
        var currentAspectRatioIndex = 0

        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, index -> currentAspectRatioIndex = index },
                onZoomChange = { zoom -> currentZoom = zoom },
                onZoomLive = { },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Set specific zoom and aspect ratio
        val zoomWheel = composeTestRule.onNode(hasContentDescription("Zoom control"))
        zoomWheel.performScrollToIndex(2) // Scroll to some zoom level
        
        val aspectRatioWheel = composeTestRule.onNode(hasContentDescription("Aspect ratio selector"))
        aspectRatioWheel.performScrollToIndex(1) // Change aspect ratio

        composeTestRule.waitForIdle()
        
        val preRotationZoom = currentZoom
        val preRotationAspectRatio = currentAspectRatioIndex

        // Simulate configuration change (rotation) by recreating the composable
        composeTestRule.setContent {
            // Simulate landscape orientation with different dimensions
            DualVerticalSelectors(
                cameraStateManager = testStateManager, // Same state manager
                onAspectRatioChange = { _, index -> currentAspectRatioIndex = index },
                onZoomChange = { zoom -> currentZoom = zoom },
                onZoomLive = { },
                modifier = Modifier.fillMaxSize()
            )
        }

        composeTestRule.waitForIdle()

        // Verify state is preserved after rotation
        assert(currentZoom == preRotationZoom) { 
            "Zoom should be preserved after rotation. Expected: $preRotationZoom, Got: $currentZoom" 
        }
        
        assert(currentAspectRatioIndex == preRotationAspectRatio) { 
            "Aspect ratio should be preserved after rotation. Expected: $preRotationAspectRatio, Got: $currentAspectRatioIndex" 
        }

        // Verify wheels are still functional after rotation
        zoomWheel.assertExists()
        aspectRatioWheel.assertExists()
    }

    @Test
    fun testErrorRecoveryFromInvalidStates() {
        var errorRecoverySuccessful = true
        val errorMessages = mutableListOf<String>()

        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { zoom ->
                    // Simulate error conditions
                    if (zoom < 0f || zoom.isNaN() || zoom.isInfinite()) {
                        errorMessages.add("Invalid zoom value: $zoom")
                        errorRecoverySuccessful = false
                    }
                },
                onZoomLive = { zoom ->
                    if (zoom < 0f || zoom.isNaN() || zoom.isInfinite()) {
                        errorMessages.add("Invalid live zoom value: $zoom")
                        errorRecoverySuccessful = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        val zoomWheel = composeTestRule.onNode(hasContentDescription("Zoom control"))

        // Try to trigger edge cases that might cause invalid states
        repeat(10) {
            try {
                zoomWheel.performTouchInput {
                    down(center)
                    // Extreme movements that might cause calculation errors
                    moveBy(Offset(0f, 10000f))
                    moveBy(Offset(0f, -20000f))
                    moveBy(Offset(Float.MAX_VALUE, Float.MAX_VALUE))
                    up()
                }
                composeTestRule.waitForIdle()
            } catch (e: Exception) {
                errorMessages.add("Exception during extreme interaction: ${e.message}")
                // Should not crash, continue testing
            }
        }

        assert(errorRecoverySuccessful) { 
            "System should handle invalid states gracefully. Errors: $errorMessages" 
        }

        // Verify wheels are still functional after error conditions
        zoomWheel.assertExists()
        zoomWheel.assertIsDisplayed()
    }

    @Test
    fun testPerformanceWithRapidGestureChanges() {
        val performanceMetrics = mutableListOf<Long>()
        
        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { },
                modifier = Modifier.fillMaxSize()
            )
        }

        val zoomWheel = composeTestRule.onNode(hasContentDescription("Zoom control"))

        // Test rapid gesture state changes (down -> move -> up repeatedly)
        repeat(25) { index ->
            val gestureTime = measureTimeMillis {
                zoomWheel.performTouchInput {
                    down(center)
                    repeat(5) { moveIndex ->
                        moveBy(Offset(0f, moveIndex * 20f))
                    }
                    up()
                }
                composeTestRule.waitForIdle()
            }
            
            performanceMetrics.add(gestureTime)
        }

        // Analyze performance metrics
        val averageGestureTime = performanceMetrics.average()
        val maxGestureTime = performanceMetrics.maxOrNull() ?: 0L
        val gesturesOverThreshold = performanceMetrics.count { it > 50 } // 50ms threshold

        assert(averageGestureTime < 30.0) { 
            "Average gesture time should be under 30ms, got: ${averageGestureTime}ms" 
        }
        
        assert(maxGestureTime < 100L) { 
            "Max gesture time should be under 100ms, got: ${maxGestureTime}ms" 
        }
        
        assert(gesturesOverThreshold < 5) { 
            "Too many slow gestures: $gesturesOverThreshold out of ${performanceMetrics.size}" 
        }
    }

    @Test
    fun testAccessibilityUnderStressConditions() {
        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Perform stress operations
        val zoomWheel = composeTestRule.onNode(hasContentDescription("Zoom control"))
        val aspectRatioWheel = composeTestRule.onNode(hasContentDescription("Aspect ratio selector"))

        // Rapid interactions
        repeat(15) {
            zoomWheel.performTouchInput {
                down(center)
                moveBy(Offset(0f, 50f))
                up()
            }
            
            aspectRatioWheel.performTouchInput {
                down(center)
                moveBy(Offset(0f, -30f))
                up()
            }
        }

        composeTestRule.waitForIdle()

        // Verify accessibility features still work after stress
        composeTestRule.onNodeWithContentDescription("Zoom control")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Aspect ratio selector")
            .assertExists()
            .assertIsDisplayed()

        // Check for selected item descriptions
        composeTestRule.onAllNodesWithContentDescription(text = ".*selected.*", useUnmergedTree = true)
            .assertCountEquals(2) // Should have one selected item per wheel

        // Verify touch targets are still adequate after stress
        zoomWheel.assertTouchHeightIsAtLeast(48.dp)
        aspectRatioWheel.assertTouchHeightIsAtLeast(48.dp)
    }
}

// Extension function for touch size assertions (if not already defined)
private fun SemanticsNodeInteraction.assertTouchHeightIsAtLeast(
    minimumHeight: androidx.compose.ui.unit.Dp
): SemanticsNodeInteraction {
    val bounds = fetchSemanticsNode().boundsInRoot
    val actualHeight = bounds.height
    val minimumHeightPx = with(androidx.compose.ui.unit.Density(1f)) { minimumHeight.toPx() }
    
    assert(actualHeight >= minimumHeightPx) {
        "Touch height should be at least $minimumHeight but was ${actualHeight}px"
    }
    return this
}
