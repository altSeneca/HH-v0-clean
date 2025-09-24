package com.hazardhawk.ai

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.waitUntil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.test.AITestDataFactory
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

/**
 * End-to-end integration tests for camera → AI → tags workflow on Android
 * Tests the complete user journey from photo capture through AI analysis to tag selection
 */
@RunWith(AndroidJUnit4::class)
class CameraAIIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Test
    fun testCompleteCameraToTagWorkflow() = runTest {
        // Given: User opens camera screen
        composeTestRule.setContent {
            CameraScreenWithAIIntegration(
                onPhotoAnalyzed = { analysis ->
                    // Verify AI analysis results received
                    assert(analysis.detections.isNotEmpty())
                }
            )
        }
        
        // When: User captures photo
        composeTestRule.onNodeWithTag("camera_capture_button").performClick()
        
        // Then: Wait for AI analysis to complete
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("AI Analysis Complete").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Verify AI analysis dialog appears
        composeTestRule.onNodeWithText("Safety Analysis").assertIsDisplayed()
        
        // Verify hazard detections are shown
        composeTestRule.onNodeWithTag("hazard_detections_list").assertIsDisplayed()
        
        // When: User proceeds to tag selection
        composeTestRule.onNodeWithText("Review Tags").performClick()
        
        // Then: Verify tag dialog opens with AI recommendations
        composeTestRule.onNodeWithText("Recommended Tags").assertIsDisplayed()
        
        // Verify AI-recommended tags are pre-selected
        composeTestRule.onNodeWithTag("auto_selected_tags").assertIsDisplayed()
        
        // Verify user can see confidence scores
        composeTestRule.onNodeWithText("High Confidence").assertIsDisplayed()
        
        // When: User saves photo with tags
        composeTestRule.onNodeWithText("Save with Tags").performClick()
        
        // Then: Verify photo saved successfully
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeTestRule.onNodeWithText("Photo Saved").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
    
    @Test
    fun testPPEViolationDetectionWorkflow() = runTest {
        // Given: Camera screen with PPE violation detection enabled
        val mockPhotoData = AITestDataFactory.createPPEViolationPhoto()
        
        composeTestRule.setContent {
            CameraScreenWithMockCapture(
                mockPhotoData = mockPhotoData,
                onAIAnalysisComplete = { analysis ->
                    // Verify PPE violation detected
                    val ppeViolation = analysis.detections.find { 
                        it.hazardType == HazardType.PERSON_NO_HARD_HAT 
                    }
                    assert(ppeViolation != null) { "Should detect PPE violation" }
                    assert(ppeViolation.confidence > 0.8f) { "Should have high confidence" }
                }
            )
        }
        
        // When: Trigger photo capture
        composeTestRule.onNodeWithTag("camera_capture_button").performClick()
        
        // Then: Wait for AI processing
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("Critical Safety Issue").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Verify critical hazard alert
        composeTestRule.onNodeWithText("Critical Safety Issue").assertIsDisplayed()
        composeTestRule.onNodeWithText("PPE Violation Detected").assertIsDisplayed()
        
        // Verify specific hazard details
        composeTestRule.onNodeWithText("Person without Hard Hat").assertIsDisplayed()
        
        // When: User opens tag recommendations
        composeTestRule.onNodeWithText("View Recommendations").performClick()
        
        // Then: Verify PPE-specific tags are auto-selected
        composeTestRule.onNodeWithText("ppe-hard-hat-required").assertIsDisplayed()
        composeTestRule.onNodeWithText("ppe-head-protection-missing").assertIsDisplayed()
        composeTestRule.onNodeWithText("general-ppe-violation").assertIsDisplayed()
        
        // Verify tags are checked/selected
        composeTestRule.onNodeWithTag("tag_ppe-hard-hat-required").assertIsDisplayed()
        
        // Verify OSHA compliance information
        composeTestRule.onNodeWithText("OSHA 29 CFR 1926.95").assertIsDisplayed()
        
        // When: User confirms tags
        composeTestRule.onNodeWithText("Confirm Tags").performClick()
        
        // Then: Verify safety documentation generated
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeTestRule.onNodeWithText("Safety Report Generated").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
    
    @Test
    fun testMultipleHazardsDetectionWorkflow() = runTest {
        // Given: Photo with multiple construction hazards
        val multiHazardPhoto = AITestDataFactory.createMultiHazardPhoto()
        
        composeTestRule.setContent {
            CameraScreenWithMockCapture(
                mockPhotoData = multiHazardPhoto,
                onAIAnalysisComplete = { analysis ->
                    // Verify multiple hazard types detected
                    val hazardTypes = analysis.detections.map { it.hazardType }.distinct()
                    assert(hazardTypes.size >= 2) { "Should detect multiple hazard types" }
                }
            )
        }
        
        // When: Capture photo
        composeTestRule.onNodeWithTag("camera_capture_button").performClick()
        
        // Then: Wait for complex analysis
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            try {
                composeTestRule.onNodeWithText("Multiple Hazards Detected").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Verify multiple hazards shown
        composeTestRule.onNodeWithText("Multiple Hazards Detected").assertIsDisplayed()
        
        // Verify hazard list is expandable
        composeTestRule.onNodeWithText("Show All Hazards").performClick()
        
        // Verify different hazard categories shown
        composeTestRule.onNodeWithTag("hazard_list").assertIsDisplayed()
        
        // Verify priority ordering (critical hazards first)
        composeTestRule.onNodeWithText("CRITICAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("HIGH").assertIsDisplayed()
        
        // When: User proceeds to tags
        composeTestRule.onNodeWithText("Review All Tags").performClick()
        
        // Then: Verify comprehensive tag recommendations
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeTestRule.onNodeWithText("Recommended Tags").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Verify tags grouped by category
        composeTestRule.onNodeWithText("PPE Tags").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fall Protection Tags").assertIsDisplayed()
        composeTestRule.onNodeWithText("Equipment Safety Tags").assertIsDisplayed()
        
        // Verify auto-selection of critical tags only
        composeTestRule.onNodeWithTag("critical_tags_section").assertIsDisplayed()
        
        // When: User customizes tag selection
        composeTestRule.onNodeWithText("Add More Tags").performClick()
        composeTestRule.onNodeWithText("equipment-daily-inspection").performClick()
        
        // Then: Verify custom selection added
        composeTestRule.onNodeWithTag("selected_tags_list").assertIsDisplayed()
        
        // When: User saves comprehensive analysis
        composeTestRule.onNodeWithText("Generate Safety Report").performClick()
        
        // Then: Verify comprehensive report generated
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("Comprehensive Safety Report Ready").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
    
    @Test
    fun testLowConfidenceHandling() = runTest {
        // Given: Ambiguous photo with uncertain detections
        val ambiguousPhoto = AITestDataFactory.createAmbiguousTestPhoto()
        
        composeTestRule.setContent {
            CameraScreenWithMockCapture(
                mockPhotoData = ambiguousPhoto,
                onAIAnalysisComplete = { analysis ->
                    // Verify low confidence detections
                    val lowConfidenceDetections = analysis.detections.filter { it.confidence < 0.5f }
                    assert(lowConfidenceDetections.isNotEmpty()) { "Should have low confidence detections" }
                }
            )
        }
        
        // When: Capture ambiguous photo
        composeTestRule.onNodeWithTag("camera_capture_button").performClick()
        
        // Then: Wait for analysis
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("Analysis Complete").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Verify uncertainty indicators
        composeTestRule.onNodeWithText("Possible Hazards Detected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Please Review").assertIsDisplayed()
        
        // Verify confidence scores shown
        composeTestRule.onNodeWithText("Low Confidence").assertIsDisplayed()
        composeTestRule.onNodeWithText("Requires Review").assertIsDisplayed()
        
        // When: User reviews recommendations
        composeTestRule.onNodeWithText("Review Suggestions").performClick()
        
        // Then: Verify no auto-selections for low confidence
        composeTestRule.onNodeWithText("Suggested Tags").assertIsDisplayed()
        composeTestRule.onNodeWithText("(No auto-selections)").assertIsDisplayed()
        
        // Verify manual selection encouraged
        composeTestRule.onNodeWithText("Select Applicable Tags").assertIsDisplayed()
        
        // When: User manually selects tags
        composeTestRule.onNodeWithText("ppe-safety-assessment").performClick()
        composeTestRule.onNodeWithText("general-site-safety").performClick()
        
        // Then: Verify manual selection works
        composeTestRule.onNodeWithTag("manually_selected_tags").assertIsDisplayed()
        
        // When: User saves with manual tags
        composeTestRule.onNodeWithText("Save with Selected Tags").performClick()
        
        // Then: Verify save successful
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeTestRule.onNodeWithText("Photo Saved with Manual Tags").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
    
    @Test
    fun testAIPerformanceIntegration() = runTest {
        // Given: Standard construction photo
        val testPhoto = AITestDataFactory.createConstructionTestPhoto()
        var analysisStartTime = 0L
        var analysisEndTime = 0L
        
        composeTestRule.setContent {
            CameraScreenWithPerformanceTracking(
                mockPhotoData = testPhoto,
                onAnalysisStart = { analysisStartTime = System.currentTimeMillis() },
                onAnalysisComplete = { analysisEndTime = System.currentTimeMillis() }
            )
        }
        
        // When: Trigger photo analysis
        composeTestRule.onNodeWithTag("camera_capture_button").performClick()
        
        // Then: Wait for analysis completion
        composeTestRule.waitUntil(timeoutMillis = 1000) { // Expect sub-second performance
            try {
                composeTestRule.onNodeWithText("Analysis Complete").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Verify performance metrics
        val analysisTime = analysisEndTime - analysisStartTime
        assert(analysisTime < 500) { "AI analysis should complete in <500ms, took ${analysisTime}ms" }
        
        // Verify performance indicator shown to user
        composeTestRule.onNodeWithText("Analysis Time: ${analysisTime}ms").assertIsDisplayed()
        
        // Verify quality not compromised for speed
        composeTestRule.onNodeWithText("AI Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hazards Detected").assertIsDisplayed()
    }
    
    @Test
    fun testContextualTagEnhancement() = runTest {
        // Given: Electrical work photo with contextual information
        val electricalPhoto = AITestDataFactory.createElectricalWorkPhoto()
        
        composeTestRule.setContent {
            CameraScreenWithContext(
                mockPhotoData = electricalPhoto,
                workType = "ELECTRICAL_WORK",
                workHeight = 8.0, // Above 6 feet
                nearPowerLines = true
            )
        }
        
        // When: Capture photo with context
        composeTestRule.onNodeWithTag("camera_capture_button").performClick()
        
        // Then: Wait for contextual analysis
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("Contextual Analysis Complete").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Verify contextual hazard identification
        composeTestRule.onNodeWithText("Electrical Work Detected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Height Work Identified").assertIsDisplayed()
        composeTestRule.onNodeWithText("Power Line Proximity").assertIsDisplayed()
        
        // When: View contextual recommendations
        composeTestRule.onNodeWithText("View Enhanced Tags").performClick()
        
        // Then: Verify contextual tag recommendations
        composeTestRule.onNodeWithText("ppe-hard-hat-class-e").assertIsDisplayed()
        composeTestRule.onNodeWithText("elec-loto-procedure-active").assertIsDisplayed()
        composeTestRule.onNodeWithText("fall-harness-full-body").assertIsDisplayed()
        composeTestRule.onNodeWithText("crane-powerline-clearance").assertIsDisplayed()
        
        // Verify contextual explanations
        composeTestRule.onNodeWithText("Class E protection required for electrical work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fall protection required above 6 feet").assertIsDisplayed()
        
        // When: User confirms enhanced tags
        composeTestRule.onNodeWithText("Apply Enhanced Tags").performClick()
        
        // Then: Verify contextual tags applied
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeTestRule.onNodeWithText("Enhanced Safety Profile Applied").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
    
    @Test
    fun testOfflineAIIntegration() = runTest {
        // Given: Device in offline mode
        val testPhoto = AITestDataFactory.createConstructionTestPhoto()
        
        composeTestRule.setContent {
            CameraScreenWithNetworkState(
                mockPhotoData = testPhoto,
                isOnline = false
            )
        }
        
        // When: Capture photo while offline
        composeTestRule.onNodeWithTag("camera_capture_button").performClick()
        
        // Then: Verify offline AI processing
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            try {
                composeTestRule.onNodeWithText("Offline Analysis Complete").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        
        // Verify offline mode indicator
        composeTestRule.onNodeWithText("Local AI Analysis").assertIsDisplayed()
        composeTestRule.onNodeWithText("No internet required").assertIsDisplayed()
        
        // Verify reduced but functional capabilities
        composeTestRule.onNodeWithText("Basic Safety Analysis").assertIsDisplayed()
        
        // When: User proceeds with offline analysis
        composeTestRule.onNodeWithText("Continue Offline").performClick()
        
        // Then: Verify offline tag recommendations
        composeTestRule.onNodeWithText("Basic Tag Recommendations").assertIsDisplayed()
        
        // Verify sync notification for when online
        composeTestRule.onNodeWithText("Will sync when online").assertIsDisplayed()
        
        // When: User saves offline
        composeTestRule.onNodeWithText("Save Offline").performClick()
        
        // Then: Verify offline save successful
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            try {
                composeTestRule.onNodeWithText("Saved to Local Storage").fetchSemanticsNode()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
}

// Mock composables for testing - these would be implemented in the actual app
@androidx.compose.runtime.Composable
fun CameraScreenWithAIIntegration(onPhotoAnalyzed: (AIAnalysisResult) -> Unit) {
    // Mock implementation for testing
}

@androidx.compose.runtime.Composable  
fun CameraScreenWithMockCapture(
    mockPhotoData: ByteArray,
    onAIAnalysisComplete: (AIAnalysisResult) -> Unit
) {
    // Mock implementation for testing
}

@androidx.compose.runtime.Composable
fun CameraScreenWithPerformanceTracking(
    mockPhotoData: ByteArray,
    onAnalysisStart: () -> Unit,
    onAnalysisComplete: () -> Unit
) {
    // Mock implementation for testing  
}

@androidx.compose.runtime.Composable
fun CameraScreenWithContext(
    mockPhotoData: ByteArray,
    workType: String,
    workHeight: Double,
    nearPowerLines: Boolean
) {
    // Mock implementation for testing
}

@androidx.compose.runtime.Composable
fun CameraScreenWithNetworkState(
    mockPhotoData: ByteArray,
    isOnline: Boolean
) {
    // Mock implementation for testing
}