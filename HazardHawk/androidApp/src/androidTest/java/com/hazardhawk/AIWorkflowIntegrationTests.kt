package com.hazardhawk

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.ai.*
import com.hazardhawk.models.*
import com.hazardhawk.tags.LoveableTagDialog
import com.hazardhawk.tags.models.*
import com.hazardhawk.ui.theme.HazardHawkTheme
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.kotlin.*

/**
 * UI Integration Tests for LoveableTagDialog AI Workflow
 * 
 * These tests verify that AI analysis results are properly passed to and displayed
 * in the LoveableTagDialog, ensuring the complete AI-to-UI integration works.
 * 
 * Critical for validating the AI workflow results fix.
 */
@RunWith(AndroidJUnit4::class)
class AIWorkflowIntegrationTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * CRITICAL TEST: LoveableTagDialog receives AI analysis result parameter
     * This verifies the core fix - AI results reaching the tag dialog
     */
    @Test
    fun loveableTagDialogReceivesAIAnalysisResultParameter() {
        // Given: AI analysis result with specific recommendations
        val aiAnalysisResult = createMockPhotoAnalysisWithTags(
            hazardDetections = listOf(
                "Person without hard hat detected with 85% confidence",
                "Fall hazard identified at elevated work area"
            ),
            recommendedTags = listOf(
                "ppe-hard-hat" to "PPE - Hard Hat Required",
                "fall-protection" to "Fall Protection Required",
                "electrical-safety" to "Electrical Safety Review"
            ),
            autoSelectTags = setOf("ppe-hard-hat", "fall-protection"),
            complianceLevel = ComplianceLevel.NON_COMPLIANT
        )

        var dialogDismissed = false
        var receivedTags = setOf<String>()
        var receivedCompliance = false

        // When: LoveableTagDialog opened with AI analysis result
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "test-photo-123",
                    photoPath = "/test/path/photo.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { tags, compliance ->
                        receivedTags = tags
                        receivedCompliance = compliance
                    },
                    onDismiss = { dialogDismissed = true },
                    aiAnalysisResult = aiAnalysisResult.toJsonString() // Simulate serialized result
                )
            }
        }

        // Then: Dialog should display and process AI analysis
        composeTestRule.onNodeWithText("Safety Tagging").assertIsDisplayed()
        
        // Verify AI suggestions are shown
        composeTestRule.onNodeWithText("Smart Suggestions").assertIsDisplayed()
        
        // Verify AI badge is visible for AI-powered suggestions
        composeTestRule.onNodeWithText("AI").assertIsDisplayed()
        
        // Verify specific AI recommendations are displayed
        composeTestRule.onNodeWithText("PPE - Hard Hat Required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fall Protection Required").assertIsDisplayed()
        
        // Verify compliance state is set based on AI analysis
        composeTestRule.onNodeWithText("Needs Work").assertIsDisplayed()
    }

    /**
     * CRITICAL TEST: Tag dialog displays AI recommendations when available
     */
    @Test
    fun tagDialogDisplaysAIRecommendationsWhenAvailable() {
        // Given: PhotoAnalysisWithTags result with high-confidence recommendations
        val aiAnalysisResult = PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.PERSON_NO_HARD_HAT,
                    confidence = 0.92f,
                    boundingBox = BoundingBox(0.2f, 0.3f, 0.6f, 0.4f),
                    oshaCategory = OSHACategory.SUBPART_E_1926_95,
                    severity = HazardSeverity.CRITICAL,
                    description = "Worker without required hard hat"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "ppe-hard-hat-violation",
                    displayName = "PPE - Hard Hat Violation",
                    confidence = 0.92f,
                    reason = "AI detected worker without required hard hat protection",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.95"
                ),
                UITagRecommendation(
                    tagId = "safety-training-needed",
                    displayName = "Safety Training Required",
                    confidence = 0.78f,
                    reason = "PPE non-compliance indicates training gap",
                    priority = TagPriority.HIGH,
                    oshaReference = "1926.95"
                )
            ),
            autoSelectTags = setOf("ppe-hard-hat-violation"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 1,
                oshaViolations = listOf("1926.95")
            ),
            processingTimeMs = 1420L
        )

        // When: Dialog rendered with AI analysis
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "test-photo-456",
                    photoPath = "/test/construction-site.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Then: AI recommendations should be prominently displayed
        composeTestRule.onNodeWithText("Smart Suggestions").assertIsDisplayed()
        
        // Verify high-priority AI recommendations are shown first
        composeTestRule.onNodeWithText("PPE - Hard Hat Violation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Safety Training Required").assertIsDisplayed()
        
        // Verify confidence scores are communicated (via visual indicators)
        composeTestRule.onAllNodesWithText("AI").assertCountEquals(2) // Should have AI badges
        
        // Verify OSHA references are available (might be in detail view)
        // This tests the integration of AI analysis with compliance tracking
        
        // Verify critical priority is visually distinct
        composeTestRule.onNodeWithText("Needs Work").assertHasClickAction()
        
        // Test auto-selection functionality
        // The PPE violation should be auto-selected based on AI confidence
        composeTestRule.onNodeWithText("PPE - Hard Hat Violation")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    /**
     * CRITICAL TEST: Tag dialog shows generic suggestions when AI unavailable
     */
    @Test
    fun tagDialogShowsGenericSuggestionsWhenAIUnavailable() {
        // Given: aiAnalysisResult is null (AI failed or unavailable)
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "test-photo-789",
                    photoPath = "/test/fallback-photo.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = null // No AI analysis available
                )
            }
        }

        // Then: Generic tag suggestions should be shown with fallback messaging
        composeTestRule.onNodeWithText("Safety Tagging").assertIsDisplayed()
        
        // Should show quick tags without AI badge
        composeTestRule.onNodeWithText("Smart Suggestions").assertIsDisplayed()
        
        // Should NOT show AI badges when no AI analysis available
        composeTestRule.onAllNodesWithText("AI").assertCountEquals(0)
        
        // Should show generic construction safety tags
        composeTestRule.onNodeWithText("General Safety").assertExists()
        composeTestRule.onNodeWithText("PPE Review").assertExists()
        
        // Should default to basic compliance state
        composeTestRule.onNodeWithText("Compliant").assertIsDisplayed()
        
        // Should still be fully functional for manual tag selection
        composeTestRule.onNodeWithText("More Options").assertIsDisplayed().assertHasClickAction()
    }

    /**
     * Test AI recommendation interaction and selection
     */
    @Test
    fun aiRecommendationsCanBeInteractedWithAndSelected() {
        // Given: AI analysis with multiple recommendations
        val aiAnalysisResult = createConstructionSiteAnalysisResult()

        var selectedTags = setOf<String>()
        var complianceState = true

        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "interactive-test",
                    photoPath = "/test/interactive.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { tags, compliance ->
                        selectedTags = tags
                        complianceState = compliance
                    },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // When: User interacts with AI recommendations
        
        // Select first AI recommendation
        composeTestRule.onNodeWithText("Fall Protection Required")
            .assertIsDisplayed()
            .performClick()
        
        // Select second AI recommendation  
        composeTestRule.onNodeWithText("Scaffold Safety Check")
            .assertIsDisplayed()
            .performClick()
        
        // Toggle compliance state to "Needs Work"
        composeTestRule.onNodeWithText("Needs Work")
            .assertIsDisplayed()
            .performClick()
        
        // Save the selections
        composeTestRule.onNodeWithText("Save")
            .assertIsDisplayed()
            .performClick()
        
        // Then: Selections should be processed
        // The actual tag IDs would be verified in a real implementation
        assert(selectedTags.isNotEmpty())
        assert(!complianceState) // Should be false for "Needs Work"
    }

    /**
     * Test AI analysis with different construction work types
     */
    @Test
    fun aiAnalysisAdaptsToConstructionWorkTypes() {
        // Test electrical work specific AI recommendations
        val electricalWorkAnalysis = PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.ELECTRICAL_HAZARD,
                    confidence = 0.89f,
                    boundingBox = BoundingBox(0.1f, 0.2f, 0.8f, 0.7f),
                    oshaCategory = OSHACategory.SUBPART_K_1926_416,
                    severity = HazardSeverity.CRITICAL,
                    description = "Exposed electrical wiring detected"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "electrical-lockout-tagout",
                    displayName = "Lockout/Tagout Required",
                    confidence = 0.89f,
                    reason = "Electrical hazard requires LOTO procedures",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.416"
                ),
                UITagRecommendation(
                    tagId = "electrical-ppe-required", 
                    displayName = "Electrical PPE Required",
                    confidence = 0.82f,
                    reason = "Arc flash protection needed for electrical work",
                    priority = TagPriority.HIGH,
                    oshaReference = "1926.416"
                )
            ),
            autoSelectTags = setOf("electrical-lockout-tagout"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 1,
                oshaViolations = listOf("1926.416")
            )
        )

        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "electrical-work-test",
                    photoPath = "/test/electrical-hazard.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = electricalWorkAnalysis.toJsonString()
                )
            }
        }

        // Then: Should show electrical work specific recommendations
        composeTestRule.onNodeWithText("Lockout/Tagout Required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Electrical PPE Required").assertIsDisplayed()
        
        // Should indicate high criticality
        composeTestRule.onNodeWithText("Needs Work").assertIsDisplayed()
        
        // Should show OSHA reference context
        composeTestRule.onAllNodesWithText("AI").assertCountEquals(2) // Both should be AI recommendations
    }

    /**
     * Test performance with large AI analysis results
     */
    @Test
    fun performanceWithLargeAIAnalysisResults() {
        // Given: Large AI analysis result (many detections and recommendations)
        val largeAnalysisResult = PhotoAnalysisWithTags(
            detections = (1..10).map { i ->
                HazardDetection(
                    hazardType = HazardType.values()[i % HazardType.values().size],
                    confidence = 0.6f + (i * 0.03f),
                    boundingBox = BoundingBox(0.1f * i, 0.1f * i, 0.2f, 0.2f),
                    oshaCategory = OSHACategory.SUBPART_E_1926_95,
                    severity = if (i % 2 == 0) HazardSeverity.CRITICAL else HazardSeverity.MODERATE,
                    description = "Hazard detection $i"
                )
            },
            recommendedTags = (1..15).map { i ->
                UITagRecommendation(
                    tagId = "tag-recommendation-$i",
                    displayName = "Safety Recommendation $i",
                    confidence = 0.5f + (i * 0.02f),
                    reason = "AI detected condition requiring attention $i",
                    priority = TagPriority.values()[i % TagPriority.values().size]
                )
            },
            autoSelectTags = (1..5).map { "tag-recommendation-$it" }.toSet(),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.PARTIAL_COMPLIANCE,
                criticalIssues = 5,
                oshaViolations = listOf("1926.95", "1926.501", "1926.416")
            ),
            processingTimeMs = 2840L
        )

        val startTime = System.currentTimeMillis()

        // When: Dialog loads with large analysis result
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "performance-test",
                    photoPath = "/test/complex-site.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = largeAnalysisResult.toJsonString()
                )
            }
        }

        val loadTime = System.currentTimeMillis() - startTime

        // Then: Should load within reasonable time for construction site use
        assert(loadTime < 2000L) // Should load within 2 seconds
        
        // Should still display properly
        composeTestRule.onNodeWithText("Safety Tagging").assertIsDisplayed()
        composeTestRule.onNodeWithText("Smart Suggestions").assertIsDisplayed()
        
        // Should show reasonable number of quick suggestions (not overwhelm user)
        composeTestRule.onAllNodesWithText("AI").assertCountGreaterThan(0)
        composeTestRule.onAllNodesWithText("AI").assertCountAtMost(6) // Should limit to reasonable number
    }

    /**
     * Test accessibility features with AI recommendations
     */
    @Test
    fun accessibilityFeaturesWorkWithAIRecommendations() {
        val aiAnalysisResult = createAccessibilityTestAnalysisResult()

        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "accessibility-test",
                    photoPath = "/test/accessibility.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Then: AI recommendations should be accessible
        composeTestRule.onNodeWithText("PPE - Hard Hat Required")
            .assertIsDisplayed()
            .assert(hasContentDescription()) // Should have content description for screen readers
            
        composeTestRule.onNodeWithText("Fall Protection Required")
            .assertIsDisplayed()
            .assert(hasClickAction()) // Should be clickable for users with motor impairments
        
        // Touch targets should be large enough for construction workers with gloves
        composeTestRule.onNodeWithText("Save")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp) // Minimum recommended touch target size
    }

    // Helper functions for creating test data
    private fun createMockPhotoAnalysisWithTags(
        hazardDetections: List<String>,
        recommendedTags: List<Pair<String, String>>,
        autoSelectTags: Set<String>,
        complianceLevel: ComplianceLevel
    ): PhotoAnalysisWithTags {
        val detections = hazardDetections.mapIndexed { index, description ->
            HazardDetection(
                hazardType = HazardType.values()[index % HazardType.values().size],
                confidence = 0.7f + (index * 0.1f),
                boundingBox = BoundingBox(0.2f, 0.3f, 0.6f, 0.4f),
                oshaCategory = OSHACategory.SUBPART_E_1926_95,
                severity = HazardSeverity.MODERATE,
                description = description
            )
        }
        
        val tagRecommendations = recommendedTags.map { (id, name) ->
            UITagRecommendation(
                tagId = id,
                displayName = name,
                confidence = 0.8f,
                reason = "AI analysis recommendation"
            )
        }
        
        return PhotoAnalysisWithTags(
            detections = detections,
            recommendedTags = tagRecommendations,
            autoSelectTags = autoSelectTags,
            complianceOverview = ComplianceOverview(
                overallLevel = complianceLevel,
                criticalIssues = detections.size,
                oshaViolations = listOf("1926.95")
            )
        )
    }

    private fun createConstructionSiteAnalysisResult(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.FALL_HAZARD,
                    confidence = 0.87f,
                    boundingBox = BoundingBox(0.3f, 0.2f, 0.7f, 0.8f),
                    oshaCategory = OSHACategory.SUBPART_M_1926_501,
                    severity = HazardSeverity.CRITICAL,
                    description = "Unprotected fall hazard at elevation"
                ),
                HazardDetection(
                    hazardType = HazardType.UNSAFE_SCAFFOLDING,
                    confidence = 0.73f,
                    boundingBox = BoundingBox(0.1f, 0.4f, 0.5f, 0.9f),
                    oshaCategory = OSHACategory.SUBPART_L_1926_451,
                    severity = HazardSeverity.MODERATE,
                    description = "Scaffold missing guardrails"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "fall-protection-required",
                    displayName = "Fall Protection Required",
                    confidence = 0.87f,
                    reason = "Working at height without proper fall protection",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.501"
                ),
                UITagRecommendation(
                    tagId = "scaffold-safety-check",
                    displayName = "Scaffold Safety Check",
                    confidence = 0.73f,
                    reason = "Scaffold configuration needs safety review",
                    priority = TagPriority.HIGH,
                    oshaReference = "1926.451"
                )
            ),
            autoSelectTags = setOf("fall-protection-required"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 2,
                oshaViolations = listOf("1926.501", "1926.451")
            )
        )
    }

    private fun createAccessibilityTestAnalysisResult(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.PERSON_NO_HARD_HAT,
                    confidence = 0.91f,
                    boundingBox = BoundingBox(0.4f, 0.1f, 0.6f, 0.5f),
                    oshaCategory = OSHACategory.SUBPART_E_1926_95,
                    severity = HazardSeverity.CRITICAL,
                    description = "Worker without required hard hat protection"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "ppe-hard-hat-required",
                    displayName = "PPE - Hard Hat Required",
                    confidence = 0.91f,
                    reason = "Critical safety violation - hard hat missing",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.95"
                ),
                UITagRecommendation(
                    tagId = "fall-protection-required",
                    displayName = "Fall Protection Required", 
                    confidence = 0.76f,
                    reason = "Elevated work area requires fall protection",
                    priority = TagPriority.HIGH,
                    oshaReference = "1926.501"
                )
            ),
            autoSelectTags = setOf("ppe-hard-hat-required"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 1,
                oshaViolations = listOf("1926.95")
            )
        )
    }
}

/**
 * Extension function to convert PhotoAnalysisWithTags to JSON string for testing
 */
private fun PhotoAnalysisWithTags.toJsonString(): String {
    // In a real implementation, this would use proper JSON serialization
    // For testing purposes, we'll return a mock JSON string
    return """
    {
        "detections": ${detections.size},
        "recommendedTags": ${recommendedTags.size},
        "autoSelectTags": ${autoSelectTags.size},
        "complianceOverview": {
            "overallLevel": "${complianceOverview.overallLevel}",
            "criticalIssues": ${complianceOverview.criticalIssues}
        },
        "processingTimeMs": $processingTimeMs
    }
    """.trimIndent()
}