package com.hazardhawk.e2e

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.hazardhawk.MainActivity
import com.hazardhawk.test.AITestDataFactory
import com.hazardhawk.test.TestScenarioType
import com.hazardhawk.domain.entities.*
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * End-to-End tests for Camera → AI → UI workflow
 * Tests the complete user journey from photo capture to AI analysis display
 */
@RunWith(AndroidJUnit4::class)
class CameraToAIWorkflowE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @Test
    fun testCompleteCameraToAIWorkflowPPEViolation() {
        // Given: App is launched and permissions are granted
        composeTestRule.waitForIdle()
        
        // Navigate to camera screen if not already there
        if (composeTestRule.onNodeWithText("Capture").isDisplayed()) {
            // Already on camera screen
        } else {
            // Navigate to camera from main screen
            composeTestRule.onNodeWithContentDescription("Camera")
                .assertIsDisplayed()
                .performClick()
        }
        
        // Wait for camera to initialize
        composeTestRule.waitForIdle()
        
        // When: Capture photo using camera button
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        
        // Wait for photo capture to complete
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Analysis").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Analyzing").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Then: Should show AI analysis progress
        composeTestRule.onNode(
            hasText("Analyzing", ignoreCase = true) or 
            hasText("Processing", ignoreCase = true) or
            hasContentDescription("AI analysis in progress")
        ).assertIsDisplayed()
        
        // Wait for AI analysis to complete
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Analysis Complete").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Safety Analysis").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithContentDescription("Analysis results").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Then: Should display AI analysis results
        composeTestRule.onNode(
            hasText("Analysis Complete", ignoreCase = true) or
            hasText("Safety Analysis", ignoreCase = true) or
            hasContentDescription("Analysis results")
        ).assertIsDisplayed()
        
        // Verify AI analysis components are visible
        composeTestRule.onNodeWithText("Hazards Detected", ignoreCase = true)
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Recommendations", ignoreCase = true)
            .assertIsDisplayed()
        
        // Check for PPE-related analysis results (if PPE violation detected)
        val ppeNodes = composeTestRule.onAllNodesWithText(
            "PPE", 
            substring = true, 
            ignoreCase = true
        )
        if (ppeNodes.fetchSemanticsNodes().isNotEmpty()) {
            ppeNodes[0].assertIsDisplayed()
        }
        
        // Verify safety recommendations are present
        composeTestRule.onNode(
            hasText("safety", substring = true, ignoreCase = true) and
            (hasText("recommend", substring = true, ignoreCase = true) or
             hasText("action", substring = true, ignoreCase = true))
        ).assertIsDisplayed()
        
        // Test interaction with analysis results
        composeTestRule.onNodeWithText("View Details", ignoreCase = true)
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify detailed analysis view
        composeTestRule.onNodeWithText("Detailed Analysis", ignoreCase = true)
            .assertIsDisplayed()
        
        // Test navigation back to camera for another capture
        composeTestRule.onNodeWithContentDescription("Back") 
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Should return to camera view
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .assertIsDisplayed()
    }

    @Test
    fun testCameraToAIWorkflowWithBatchCapture() {
        // Given: Camera screen is active
        navigateToCameraScreen()
        
        val captureCount = 3
        
        // When: Capture multiple photos in sequence
        repeat(captureCount) { index ->
            composeTestRule.onNodeWithContentDescription("Capture photo")
                .assertIsDisplayed()
                .performClick()
            
            // Wait briefly between captures
            composeTestRule.waitForIdle()
            Thread.sleep(1000)
        }
        
        // Then: Should handle batch processing
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("$captureCount photos captured").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Batch Analysis").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Navigate to photo gallery/analysis view
        composeTestRule.onNodeWithContentDescription("Gallery") 
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify batch analysis results
        composeTestRule.onNodeWithText("Batch Analysis Results", ignoreCase = true)
            .assertIsDisplayed()
        
        // Should show multiple photos with their analysis status
        composeTestRule.onAllNodesWithContentDescription("Photo analysis result")
            .assertCountEquals(captureCount)
        
        // Test individual photo analysis viewing
        composeTestRule.onAllNodesWithContentDescription("Photo analysis result")[0]
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Should show individual analysis details
        composeTestRule.onNodeWithText("Analysis Details", ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun testCameraToAIWorkflowErrorRecovery() {
        // Given: Camera screen is active
        navigateToCameraScreen()
        
        // Simulate network connectivity issues (if applicable)
        // This would require test hooks in the actual app
        
        // When: Capture photo under error conditions
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .performClick()
        
        // Wait for either success or error handling
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithText("Analysis Complete").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Analysis Failed").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Manual Review Required").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Then: Should handle errors gracefully
        val hasError = composeTestRule.onAllNodesWithText("Analysis Failed").fetchSemanticsNodes().isNotEmpty() ||
                      composeTestRule.onAllNodesWithText("Manual Review Required").fetchSemanticsNodes().isNotEmpty()
        
        if (hasError) {
            // Verify error handling UI
            composeTestRule.onNodeWithText("Retry Analysis", ignoreCase = true)
                .assertIsDisplayed()
            
            composeTestRule.onNodeWithText("Manual Review", ignoreCase = true)
                .assertIsDisplayed()
            
            // Test retry functionality
            composeTestRule.onNodeWithText("Retry Analysis")
                .performClick()
            
            composeTestRule.waitUntil(timeoutMillis = 15000) {
                composeTestRule.onAllNodesWithText("Analysis Complete").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Still processing").fetchSemanticsNodes().isNotEmpty()
            }
        }
        
        // Should eventually provide some form of result or fallback
        composeTestRule.onNode(
            hasText("Analysis", substring = true, ignoreCase = true) or
            hasText("Review", substring = true, ignoreCase = true) or
            hasText("Safety", substring = true, ignoreCase = true)
        ).assertIsDisplayed()
    }

    @Test
    fun testCameraToAIWorkflowWithDifferentWorkTypes() {
        // Test different work type scenarios
        val workTypes = listOf(
            "General Construction" to TestScenarioType.PPE_VIOLATION,
            "Electrical Work" to TestScenarioType.ELECTRICAL_HAZARD,
            "Roofing" to TestScenarioType.FALL_HAZARD
        )
        
        workTypes.forEach { (workType, _) ->
            // Given: Navigate to camera and set work type
            navigateToCameraScreen()
            
            // Open work type selector
            composeTestRule.onNodeWithContentDescription("Work type selector")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Select specific work type
            composeTestRule.onNodeWithText(workType)
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // When: Capture photo
            composeTestRule.onNodeWithContentDescription("Capture photo")
                .performClick()
            
            // Wait for AI analysis
            composeTestRule.waitUntil(timeoutMillis = 15000) {
                composeTestRule.onAllNodesWithText("Analysis Complete").fetchSemanticsNodes().isNotEmpty()
            }
            
            // Then: Should show work-type-specific analysis
            when (workType) {
                "General Construction" -> {
                    // Look for general construction safety elements
                    composeTestRule.onNode(
                        hasText("PPE", substring = true, ignoreCase = true) or
                        hasText("Hard Hat", substring = true, ignoreCase = true) or
                        hasText("Safety Vest", substring = true, ignoreCase = true)
                    ).assertIsDisplayed()
                }
                "Electrical Work" -> {
                    // Look for electrical safety elements
                    composeTestRule.onNode(
                        hasText("Electrical", substring = true, ignoreCase = true) or
                        hasText("LOTO", substring = true, ignoreCase = true) or
                        hasText("Lockout", substring = true, ignoreCase = true)
                    ).assertIsDisplayed()
                }
                "Roofing" -> {
                    // Look for fall protection elements
                    composeTestRule.onNode(
                        hasText("Fall", substring = true, ignoreCase = true) or
                        hasText("Height", substring = true, ignoreCase = true) or
                        hasText("Harness", substring = true, ignoreCase = true)
                    ).assertIsDisplayed()
                }
            }
        }
    }

    @Test
    fun testCameraToAIWorkflowPerformance() {
        // Given: Camera screen is active
        navigateToCameraScreen()
        
        val startTime = System.currentTimeMillis()
        
        // When: Capture photo and measure end-to-end time
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .performClick()
        
        // Wait for analysis to complete
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Analysis Complete").fetchSemanticsNodes().isNotEmpty()
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Then: Should complete within reasonable time
        assertTrue(
            totalTime < 15000, // Less than 15 seconds end-to-end
            "Complete camera to AI workflow should complete within 15 seconds, took ${totalTime}ms"
        )
        
        // Verify analysis results are displayed
        composeTestRule.onNodeWithText("Analysis Complete", ignoreCase = true)
            .assertIsDisplayed()
        
        // Check for performance indicators in the UI
        val processingTimeNodes = composeTestRule.onAllNodesWithText(
            "Processing time", 
            substring = true, 
            ignoreCase = true
        )
        if (processingTimeNodes.fetchSemanticsNodes().isNotEmpty()) {
            // If processing time is displayed, verify it's reasonable
            processingTimeNodes[0].assertIsDisplayed()
        }
    }

    @Test
    fun testCameraToAIWorkflowAccessibility() {
        // Given: Camera screen with accessibility enabled
        navigateToCameraScreen()
        
        // Verify accessibility labels
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule.onNodeWithContentDescription("Gallery")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule.onNodeWithContentDescription("Settings")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // When: Capture photo
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .performClick()
        
        // Wait for analysis
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithContentDescription("Analysis results").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Then: Analysis results should be accessible
        composeTestRule.onNodeWithContentDescription("Analysis results")
            .assertIsDisplayed()
        
        // Verify hazard information is accessible
        composeTestRule.onNode(
            hasContentDescription("Hazard detected", substring = true) or
            hasContentDescription("Safety recommendation", substring = true)
        ).assertIsDisplayed()
        
        // Test TalkBack navigation through results
        composeTestRule.onAllNodesWithContentDescription("Safety recommendation")[0]
            .assertHasClickAction()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Should navigate to detailed view with proper accessibility
        composeTestRule.onNodeWithContentDescription("Detailed safety recommendation")
            .assertIsDisplayed()
    }

    @Test
    fun testCameraToAIWorkflowOfflineMode() {
        // Given: Camera screen (simulating offline mode)
        navigateToCameraScreen()
        
        // Note: Actual offline simulation would require test doubles or network mocking
        // For now, test the UI flow assuming offline capabilities exist
        
        // When: Capture photo in offline mode
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .performClick()
        
        // Should either:
        // 1. Process with on-device AI immediately
        // 2. Queue for later processing
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithText("Analysis Complete").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Queued for Analysis").fetchSemanticsNodes().isNotEmpty() ||
            composeTestRule.onAllNodesWithText("Offline Analysis").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Then: Should handle offline mode appropriately
        val isQueued = composeTestRule.onAllNodesWithText("Queued", substring = true).fetchSemanticsNodes().isNotEmpty()
        val isOfflineAnalysis = composeTestRule.onAllNodesWithText("Offline", substring = true).fetchSemanticsNodes().isNotEmpty()
        val isComplete = composeTestRule.onAllNodesWithText("Analysis Complete").fetchSemanticsNodes().isNotEmpty()
        
        assertTrue(
            isQueued || isOfflineAnalysis || isComplete,
            "Should handle offline mode with queuing, offline analysis, or immediate processing"
        )
        
        if (isQueued) {
            // Test queue management UI
            composeTestRule.onNodeWithText("View Queue", ignoreCase = true)
                .performClick()
            
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithText("Analysis Queue", ignoreCase = true)
                .assertIsDisplayed()
        }
    }

    @Test
    fun testCameraToAIWorkflowWithRotation() {
        // Given: Camera screen in portrait mode
        navigateToCameraScreen()
        
        // When: Capture photo in portrait
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .performClick()
        
        // Rotate device to landscape during analysis
        composeTestRule.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeTestRule.waitForIdle()
        
        // Should handle rotation gracefully during analysis
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Analysis Complete").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Then: Analysis results should be displayed in landscape mode
        composeTestRule.onNodeWithText("Analysis Complete", ignoreCase = true)
            .assertIsDisplayed()
        
        // Rotate back to portrait
        composeTestRule.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        composeTestRule.waitForIdle()
        
        // Should maintain analysis results after rotation
        composeTestRule.onNodeWithText("Analysis Complete", ignoreCase = true)
            .assertIsDisplayed()
    }

    // Helper methods
    private fun navigateToCameraScreen() {
        // Check if already on camera screen
        val captureButton = composeTestRule.onNodeWithContentDescription("Capture photo")
        
        try {
            captureButton.assertIsDisplayed()
            // Already on camera screen
            return
        } catch (e: AssertionError) {
            // Navigate to camera screen
            composeTestRule.onNodeWithContentDescription("Camera")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Verify navigation succeeded
            composeTestRule.onNodeWithContentDescription("Capture photo")
                .assertIsDisplayed()
        }
    }

    private fun SemanticsNodeInteraction.isDisplayed(): Boolean {
        return try {
            assertIsDisplayed()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}

/**
 * UI component tests for AI analysis results display
 */
@RunWith(AndroidJUnit4::class)
class AIAnalysisUIComponentsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAIAnalysisLoadingStates() {
        // Test various loading states of AI analysis
        
        // Given: Mock AI analysis in progress
        composeTestRule.setContent {
            // Mock loading state components
            AIAnalysisLoadingComponent(
                analysisProgress = 0.6f,
                currentStep = "Analyzing safety conditions...",
                estimatedTimeRemaining = 3000L
            )
        }
        
        // Then: Should display loading indicators
        composeTestRule.onNodeWithText("Analyzing safety conditions...", ignoreCase = true)
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithContentDescription("Analysis progress")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("3 seconds remaining", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun testAIAnalysisResultsDisplay() {
        // Test display of AI analysis results
        
        val mockAnalysisResult = createMockAnalysisResult()
        
        composeTestRule.setContent {
            AIAnalysisResultsComponent(
                analysisResult = mockAnalysisResult,
                onViewDetails = { },
                onRetryAnalysis = { },
                onAcceptRecommendations = { }
            )
        }
        
        // Then: Should display all result components
        composeTestRule.onNodeWithText("Safety Analysis Complete", ignoreCase = true)
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Hazards Detected", ignoreCase = true)
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Safety Recommendations", ignoreCase = true)
            .assertIsDisplayed()
        
        // Test interaction with recommendations
        composeTestRule.onNodeWithText("View Details")
            .performClick()
        
        composeTestRule.onNodeWithText("Accept Recommendations")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun testAIAnalysisErrorStates() {
        // Test error state display
        
        composeTestRule.setContent {
            AIAnalysisErrorComponent(
                errorType = AIAnalysisError.TIMEOUT,
                errorMessage = "Analysis timed out after 10 seconds",
                onRetry = { },
                onManualReview = { }
            )
        }
        
        // Then: Should display error information
        composeTestRule.onNodeWithText("Analysis Error", ignoreCase = true)
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Analysis timed out", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Retry Analysis")
            .assertIsDisplayed()
            .assertIsEnabled()
        
        composeTestRule.onNodeWithText("Manual Review")
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    @Test
    fun testAISuggestedTagsComponent() {
        // Test AI-suggested tags display and interaction
        
        val mockSuggestedTags = createMockSuggestedTags()
        
        composeTestRule.setContent {
            AISuggestedTagsComponent(
                suggestedTags = mockSuggestedTags,
                selectedTags = setOf("ppe-hard-hat-required"),
                onTagSelected = { },
                onTagDeselected = { },
                onAcceptAllSuggestions = { }
            )
        }
        
        // Then: Should display suggested tags
        composeTestRule.onNodeWithText("AI Suggestions", ignoreCase = true)
            .assertIsDisplayed()
        
        // Test tag interaction
        mockSuggestedTags.forEach { tag ->
            composeTestRule.onNodeWithText(tag.displayName)
                .assertIsDisplayed()
                .assertHasClickAction()
        }
        
        // Test bulk acceptance
        composeTestRule.onNodeWithText("Accept All Suggestions")
            .assertIsDisplayed()
            .performClick()
    }

    private fun createMockAnalysisResult(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            id = "test-analysis-1",
            photoId = "test-photo-1",
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.PERSON_NO_HARD_HAT,
                    confidence = 0.92f,
                    boundingBox = BoundingBox(0.3f, 0.2f, 0.2f, 0.4f),
                    oshaCategory = OSHACategory.SUBPART_E_1926_95,
                    severity = HazardSeverity.CRITICAL,
                    description = "Worker without required hard hat"
                )
            ),
            enhancedResults = emptyList(),
            recommendedTags = listOf("ppe-hard-hat-required", "safety-violation"),
            autoSelectTags = setOf("ppe-hard-hat-required"),
            complianceOverview = ComplianceOverview(
                overallStatus = ComplianceStatus.NEEDS_IMPROVEMENT,
                criticalViolations = 1,
                warnings = 0,
                compliantItems = 0,
                recommendationsSummary = listOf("Provide required hard hat"),
                priorityActions = listOf("Stop work until PPE is provided")
            ),
            processingTimeMs = 2500L,
            analysisSource = AnalysisSource.ON_DEVICE
        )
    }
    
    private fun createMockSuggestedTags(): List<UITagRecommendation> {
        return listOf(
            UITagRecommendation(
                tagId = "ppe-hard-hat-required",
                displayName = "Hard Hat Required",
                confidence = 0.92f,
                reason = "Worker detected without hard hat",
                priority = TagPriority.CRITICAL
            ),
            UITagRecommendation(
                tagId = "safety-violation",
                displayName = "Safety Violation",
                confidence = 0.88f,
                reason = "OSHA compliance violation identified",
                priority = TagPriority.HIGH
            )
        )
    }
}

// Mock data classes for testing
enum class AIAnalysisError {
    TIMEOUT, NETWORK_ERROR, MODEL_ERROR, INVALID_IMAGE
}

enum class TagPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}
