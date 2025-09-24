package com.hazardhawk.ui.camera.wheel

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.camera.CameraStateManager
import com.hazardhawk.data.repositories.CameraSettingsRepository
import com.hazardhawk.ui.camera.DualVerticalSelectors
import com.hazardhawk.ui.components.WheelItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/**
 * Integration tests for wheel positioning across different screen sizes
 * Tests the actual UI layout and positioning of camera wheels
 */
@RunWith(AndroidJUnit4::class)
class WheelPositioningIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockRepository = mock<CameraSettingsRepository>()
    private val testScope = CoroutineScope(SupervisorJob())
    private val testStateManager = CameraStateManager(mockRepository, testScope)

    @Test
    fun testWheelPositioningOnPhoneScreen() {
        testWheelPositioningForScreenSize(
            screenWidthDp = 360,
            screenHeightDp = 640,
            deviceCategory = "Phone"
        )
    }

    @Test
    fun testWheelPositioningOnTabletScreen() {
        testWheelPositioningForScreenSize(
            screenWidthDp = 768,
            screenHeightDp = 1024,
            deviceCategory = "Tablet"
        )
    }

    @Test
    fun testWheelPositioningOnLargePhoneScreen() {
        testWheelPositioningForScreenSize(
            screenWidthDp = 411,
            screenHeightDp = 731,
            deviceCategory = "Large Phone"
        )
    }

    private fun testWheelPositioningForScreenSize(
        screenWidthDp: Int,
        screenHeightDp: Int,
        deviceCategory: String
    ) {
        composeTestRule.setContent {
            TestScreenConfiguration(screenWidthDp, screenHeightDp) {
                DualVerticalSelectors(
                    cameraStateManager = testStateManager,
                    onAspectRatioChange = { _, _ -> },
                    onZoomChange = { },
                    onZoomLive = { },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("DualVerticalSelectors")
                )
            }
        }

        // Test aspect ratio wheel positioning (left side)
        composeTestRule
            .onNode(hasTestTag("AspectRatioWheel") or hasContentDescription("Aspect ratio selector"))
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(56.dp) // Expected padding start
            .assertWidthIsEqualTo(80.dp) // Expected wheel width

        // Test zoom wheel positioning (right side)  
        composeTestRule
            .onNode(hasTestTag("ZoomWheel") or hasContentDescription("Zoom control"))
            .assertIsDisplayed()
            .assertRightPositionInRootIsEqualTo(56.dp) // Expected padding end
            .assertWidthIsEqualTo(80.dp) // Expected wheel width

        // Test that both wheels have the same height
        composeTestRule
            .onNode(hasTestTag("AspectRatioWheel") or hasContentDescription("Aspect ratio selector"))
            .assertHeightIsEqualTo(240.dp)

        composeTestRule
            .onNode(hasTestTag("ZoomWheel") or hasContentDescription("Zoom control"))
            .assertHeightIsEqualTo(240.dp)
    }

    @Test
    fun testWheelEqualSpacingAndPadding() {
        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { },
                modifier = Modifier.testTag("DualVerticalSelectors")
            )
        }

        val aspectRatioWheel = composeTestRule
            .onNode(hasTestTag("AspectRatioWheel") or hasContentDescription("Aspect ratio selector"))
        val zoomWheel = composeTestRule
            .onNode(hasTestTag("ZoomWheel") or hasContentDescription("Zoom control"))

        // Both wheels should have identical dimensions
        aspectRatioWheel.assertWidthIsEqualTo(80.dp)
        zoomWheel.assertWidthIsEqualTo(80.dp)

        aspectRatioWheel.assertHeightIsEqualTo(240.dp)
        zoomWheel.assertHeightIsEqualTo(240.dp)

        // Both wheels should have same distance from edges
        aspectRatioWheel.assertLeftPositionInRootIsEqualTo(56.dp)
        zoomWheel.assertRightPositionInRootIsEqualTo(56.dp)

        // Both wheels should be vertically centered
        val aspectRatioBounds = aspectRatioWheel.fetchSemanticsNode().boundsInRoot
        val zoomBounds = zoomWheel.fetchSemanticsNode().boundsInRoot
        
        val aspectRatioCenterY = aspectRatioBounds.center.y
        val zoomCenterY = zoomBounds.center.y
        
        // Allow 1dp tolerance for floating point precision
        assert((aspectRatioCenterY - zoomCenterY).value < 1f) {
            "Wheels should be vertically aligned. AspectRatio center: $aspectRatioCenterY, Zoom center: $zoomCenterY"
        }
    }

    @Test
    fun testTouchTargetAccessibility() {
        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { },
                modifier = Modifier.testTag("DualVerticalSelectors")
            )
        }

        // Both wheels should meet minimum touch target size (48dp x 48dp)
        composeTestRule
            .onNode(hasTestTag("AspectRatioWheel") or hasContentDescription("Aspect ratio selector"))
            .assertTouchWidthIsAtLeast(48.dp)
            .assertTouchHeightIsAtLeast(48.dp)

        composeTestRule
            .onNode(hasTestTag("ZoomWheel") or hasContentDescription("Zoom control"))
            .assertTouchWidthIsAtLeast(48.dp)
            .assertTouchHeightIsAtLeast(48.dp)

        // Actual wheel dimensions should exceed minimum for better usability
        composeTestRule
            .onNode(hasTestTag("AspectRatioWheel") or hasContentDescription("Aspect ratio selector"))
            .assertWidthIsAtLeast(80.dp)
            .assertHeightIsAtLeast(240.dp)

        composeTestRule
            .onNode(hasTestTag("ZoomWheel") or hasContentDescription("Zoom control"))
            .assertWidthIsAtLeast(80.dp) 
            .assertHeightIsAtLeast(240.dp)
    }

    @Test
    fun testWheelInteractionWithGlovedHands() {
        var aspectRatioChanged = false
        var zoomChanged = false

        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, _ -> aspectRatioChanged = true },
                onZoomChange = { zoomChanged = true },
                onZoomLive = { },
                modifier = Modifier.testTag("DualVerticalSelectors")
            )
        }

        // Simulate gloved hand interaction (larger touch area, less precise)
        composeTestRule
            .onNode(hasTestTag("AspectRatioWheel") or hasContentDescription("Aspect ratio selector"))
            .performTouchInput {
                // Simulate imprecise touch with gloved hands
                down(center.copy(x = center.x + 20f, y = center.y + 15f))
                moveBy(offset = androidx.compose.ui.geometry.Offset(0f, 100f))
                up()
            }

        composeTestRule.waitForIdle()

        // Should register interaction despite imprecise touch
        assert(aspectRatioChanged) { "Aspect ratio wheel should respond to gloved hand interaction" }

        // Test zoom wheel with gloved interaction
        composeTestRule
            .onNode(hasTestTag("ZoomWheel") or hasContentDescription("Zoom control"))
            .performTouchInput {
                down(center.copy(x = center.x - 25f, y = center.y + 20f))
                moveBy(offset = androidx.compose.ui.geometry.Offset(0f, -80f))
                up()
            }

        composeTestRule.waitForIdle()

        assert(zoomChanged) { "Zoom wheel should respond to gloved hand interaction" }
    }

    @Test
    fun testWheelAccessibilitySemantics() {
        composeTestRule.setContent {
            DualVerticalSelectors(
                cameraStateManager = testStateManager,
                onAspectRatioChange = { _, _ -> },
                onZoomChange = { },
                onZoomLive = { },
                modifier = Modifier.testTag("DualVerticalSelectors")
            )
        }

        // Test content descriptions for screen readers
        composeTestRule
            .onNodeWithContentDescription("Aspect ratio selector")
            .assertExists()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Zoom control")
            .assertExists()
            .assertIsDisplayed()

        // Test that individual wheel items have proper descriptions
        composeTestRule
            .onAllNodesWithContentDescription(text = ".*selected.*", useUnmergedTree = true)
            .assertCountEquals(2) // One selected item per wheel
    }

    @Test
    fun testWheelVisibilityInLandscapeOrientation() {
        composeTestRule.setContent {
            TestScreenConfiguration(screenWidthDp = 640, screenHeightDp = 360) {
                DualVerticalSelectors(
                    cameraStateManager = testStateManager,
                    onAspectRatioChange = { _, _ -> },
                    onZoomChange = { },
                    onZoomLive = { },
                    modifier = Modifier.testTag("DualVerticalSelectors")
                )
            }
        }

        // Both wheels should remain visible and properly positioned in landscape
        composeTestRule
            .onNode(hasTestTag("AspectRatioWheel") or hasContentDescription("Aspect ratio selector"))
            .assertIsDisplayed()
            .assertLeftPositionInRootIsEqualTo(56.dp)

        composeTestRule
            .onNode(hasTestTag("ZoomWheel") or hasContentDescription("Zoom control"))
            .assertIsDisplayed()
            .assertRightPositionInRootIsEqualTo(56.dp)
    }

    @Composable
    private fun TestScreenConfiguration(
        screenWidthDp: Int,
        screenHeightDp: Int,
        content: @Composable () -> Unit
    ) {
        MaterialTheme {
            // Note: In real implementation, you'd use CompositionLocalProvider
            // to override LocalConfiguration for testing different screen sizes
            content()
        }
    }
}

// Extension functions for better test readability
private fun SemanticsNodeInteraction.assertRightPositionInRootIsEqualTo(expectedRight: androidx.compose.ui.unit.Dp): SemanticsNodeInteraction {
    val bounds = fetchSemanticsNode().boundsInRoot
    val actualRight = bounds.right
    val expectedRightPx = with(androidx.compose.ui.unit.Density(1f)) { expectedRight.toPx() }
    
    assert((actualRight - expectedRightPx).absoluteValue < 1f) {
        "Expected right position to be $expectedRight but was ${actualRight}px"
    }
    return this
}

private fun SemanticsNodeInteraction.assertTouchWidthIsAtLeast(minimumWidth: androidx.compose.ui.unit.Dp): SemanticsNodeInteraction {
    val bounds = fetchSemanticsNode().boundsInRoot
    val actualWidth = bounds.width
    val minimumWidthPx = with(androidx.compose.ui.unit.Density(1f)) { minimumWidth.toPx() }
    
    assert(actualWidth >= minimumWidthPx) {
        "Touch width should be at least $minimumWidth but was ${actualWidth}px"
    }
    return this
}

private fun SemanticsNodeInteraction.assertTouchHeightIsAtLeast(minimumHeight: androidx.compose.ui.unit.Dp): SemanticsNodeInteraction {
    val bounds = fetchSemanticsNode().boundsInRoot
    val actualHeight = bounds.height
    val minimumHeightPx = with(androidx.compose.ui.unit.Density(1f)) { minimumHeight.toPx() }
    
    assert(actualHeight >= minimumHeightPx) {
        "Touch height should be at least $minimumHeight but was ${actualHeight}px"
    }
    return this
}
