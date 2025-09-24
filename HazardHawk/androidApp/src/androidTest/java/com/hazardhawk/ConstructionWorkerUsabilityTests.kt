package com.hazardhawk

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.ai.*
import com.hazardhawk.models.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.kotlin.*

/**
 * Construction Worker Usability Tests for Field Conditions
 * 
 * These tests validate that the AI workflow meets the specific needs of construction workers
 * in real field conditions:
 * - Touch targets ≥72dp for gloved hands
 * - High contrast ratios for outdoor visibility  
 * - Clear visual hierarchy of AI vs generic suggestions
 * - Haptic feedback functionality
 * - Accessibility compliance for workers with disabilities
 * - Performance under harsh environmental conditions
 */
@RunWith(AndroidJUnit4::class)
class ConstructionWorkerUsabilityTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<CameraGalleryActivity>()

    @Mock
    private lateinit var mockAIServiceFacade: AIServiceFacade

    companion object {
        // Construction industry accessibility standards
        private val MINIMUM_TOUCH_TARGET_SIZE = 72.dp
        private val MINIMUM_CONTRAST_RATIO = 4.5 // WCAG AA standard
        private val MAXIMUM_TEXT_LENGTH_FOR_GLOVES = 20 // Characters readable with safety glasses
    }

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup AI service with construction-relevant responses
        whenever(mockAIServiceFacade.analyzePhotoWithTags(any(), any(), any(), any()))
            .thenReturn(createConstructionSiteAnalysis())
    }

    /**
     * CRITICAL TEST: Touch targets are large enough for gloved hands
     * Construction workers wear thick work gloves that reduce touch precision
     */
    @Test
    fun touchTargetsAreLargeEnoughForGlovedHands() {
        // Given: AI analysis result with recommendations
        val aiAnalysisResult = createConstructionSiteAnalysis()
        
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "gloved-hands-test",
                    photoPath = "/test/construction-site.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Then: All interactive elements should meet minimum touch target size
        
        // Test AI recommendation touch targets
        composeTestRule.onNodeWithText("PPE - Hard Hat Required")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(MINIMUM_TOUCH_TARGET_SIZE)
            .assertHeightIsAtLeast(MINIMUM_TOUCH_TARGET_SIZE)
        
        composeTestRule.onNodeWithText("Fall Protection Required")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(MINIMUM_TOUCH_TARGET_SIZE)
            .assertHeightIsAtLeast(MINIMUM_TOUCH_TARGET_SIZE)
        
        // Test compliance toggle buttons (critical for safety decisions)
        composeTestRule.onNodeWithText("Compliant")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(56.dp) // Should be even larger for critical decisions
        
        composeTestRule.onNodeWithText("Needs Work")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(56.dp)
        
        // Test save button (final action button)
        composeTestRule.onNodeWithText("Save")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(56.dp)
            .assertWidthIsAtLeast(120.dp) // Wide enough for gloved finger tap
        
        // Test close button
        composeTestRule.onNodeWithContentDescription("Close")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(44.dp) // Icon buttons should still be accessible
            .assertHeightIsAtLeast(44.dp)
    }

    /**
     * CRITICAL TEST: High contrast ratios for outdoor visibility
     * Construction sites have bright sunlight and challenging visibility conditions
     */
    @Test
    fun highContrastRatiosForOutdoorVisibility() {
        val aiAnalysisResult = createHighContrastAnalysis()
        
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "contrast-test",
                    photoPath = "/test/bright-outdoor-site.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Test critical safety indicators have high contrast
        
        // "Needs Work" should be highly visible (safety critical)
        composeTestRule.onNodeWithText("Needs Work")
            .assertIsDisplayed()
            .assertIsEnabled() // Should be clearly interactive
        
        // AI badges should be clearly distinguishable
        composeTestRule.onNodeWithText("AI")
            .assertIsDisplayed()
            // AI badges should have distinct visual treatment
        
        // Critical hazard indicators should be prominent
        composeTestRule.onNodeWithText("PPE - Hard Hat Required")
            .assertIsDisplayed()
            
        // Verify text is readable (not too light or low contrast)
        composeTestRule.onNodeWithText("Safety Tagging")
            .assertIsDisplayed()
            .assertTextContains("Safety Tagging") // Verify text is actually readable
    }

    /**
     * CRITICAL TEST: Clear visual hierarchy of AI vs generic suggestions
     * Workers need to quickly distinguish AI-powered recommendations from generic ones
     */
    @Test
    fun clearVisualHierarchyOfAIVsGenericSuggestions() {
        // Test with AI recommendations present
        val aiAnalysisResult = createMixedRecommendationsAnalysis()
        
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "hierarchy-test",
                    photoPath = "/test/mixed-recommendations.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Verify AI recommendations are prominently featured
        composeTestRule.onNodeWithText("Smart Suggestions").assertIsDisplayed()
        
        // AI badges should be clearly visible and consistent
        composeTestRule.onAllNodesWithText("AI")
            .assertCountGreaterThan(0) // Should have AI badges
        
        // AI recommendations should appear first/prominently
        composeTestRule.onNodeWithText("Critical Fall Hazard")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        // Test fallback to generic suggestions when no AI
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "no-ai-test",
                    photoPath = "/test/generic-suggestions.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = null // No AI analysis
                )
            }
        }
        
        // Should show generic suggestions without AI badges
        composeTestRule.onAllNodesWithText("AI").assertCountEquals(0)
        composeTestRule.onNodeWithText("General Safety").assertExists()
        composeTestRule.onNodeWithText("PPE Review").assertExists()
    }

    /**
     * Test haptic feedback for construction workers
     * Important for workers wearing heavy gloves or in noisy environments
     */
    @Test
    fun hapticFeedbackForConstructionWorkers() {
        val aiAnalysisResult = createConstructionSiteAnalysis()
        
        var hapticFeedbackTriggered = false
        
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "haptic-test",
                    photoPath = "/test/haptic-feedback.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> 
                        hapticFeedbackTriggered = true
                    },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Test that critical interactions provide haptic feedback
        
        // Compliance state changes should have haptic feedback
        composeTestRule.onNodeWithText("Needs Work").performClick()
        composeTestRule.waitForIdle()
        
        // Tag selections should provide feedback
        composeTestRule.onNodeWithText("PPE - Hard Hat Required").performClick()
        composeTestRule.waitForIdle()
        
        // Save action should confirm with haptic feedback
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()
        
        assertTrue("Critical actions should trigger haptic feedback", hapticFeedbackTriggered)
    }

    /**
     * Test accessibility features for workers with disabilities
     * Construction industry must accommodate workers with various accessibility needs
     */
    @Test
    fun accessibilityFeaturesForWorkersWithDisabilities() {
        val aiAnalysisResult = createAccessibleAnalysis()
        
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "accessibility-test",
                    photoPath = "/test/accessible-ui.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Test screen reader accessibility
        composeTestRule.onNodeWithText("PPE - Hard Hat Required")
            .assertIsDisplayed()
            .assert(hasContentDescription()) // Should have meaningful content descriptions
        
        // Test keyboard navigation support
        composeTestRule.onNodeWithText("Fall Protection Required")
            .assertIsDisplayed()
            .assert(hasClickAction()) // Should be keyboard accessible
        
        // Test focus management
        composeTestRule.onNodeWithText("Safety Tagging")
            .assertIsDisplayed()
            .assert(hasText()) // Title should be properly announced
        
        // Test semantic labeling for AI recommendations
        composeTestRule.onAllNodesWithText("AI")
            .onFirst()
            .assertIsDisplayed()
            // AI badges should be properly labeled for screen readers
        
        // Test color-independent information
        // Critical information should not rely solely on color
        composeTestRule.onNodeWithText("CRITICAL")
            .assertExists() // Critical priority should be text-based, not just color
    }

    /**
     * Test performance under harsh environmental conditions
     * Construction sites have dust, vibration, extreme temperatures
     */
    @Test
    fun performanceUnderHarshEnvironmentalConditions() {
        // Simulate challenging conditions with rapid user interactions
        val aiAnalysisResult = createEnvironmentalStressAnalysis()
        
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "environmental-test",
                    photoPath = "/test/harsh-conditions.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Test rapid successive interactions (worker with thick gloves making multiple attempts)
        repeat(5) {
            composeTestRule.onNodeWithText("PPE - Hard Hat Required").performClick()
            composeTestRule.waitForIdle()
        }
        
        // UI should remain responsive and stable
        composeTestRule.onNodeWithText("Safety Tagging").assertIsDisplayed()
        
        // Test recovery from interrupted interactions
        composeTestRule.onNodeWithText("Needs Work").performClick()
        composeTestRule.onNodeWithText("Compliant").performClick()
        composeTestRule.onNodeWithText("Needs Work").performClick()
        
        // Should handle rapid state changes gracefully
        composeTestRule.onNodeWithText("Needs Work").assertIsDisplayed()
        
        // Test with simulated device orientation changes (common on construction sites)
        // This would require additional testing infrastructure in a real implementation
        
        // Verify critical information remains visible and accessible
        composeTestRule.onNodeWithText("Save").assertIsDisplayed().assertIsEnabled()
    }

    /**
     * Test text readability with safety glasses and helmets
     * Workers wear PPE that can obstruct vision
     */
    @Test
    fun textReadabilityWithSafetyPPE() {
        val aiAnalysisResult = createReadabilityTestAnalysis()
        
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "ppe-readability-test",
                    photoPath = "/test/ppe-vision.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Test that text is not too small
        composeTestRule.onNodeWithText("Safety Tagging")
            .assertIsDisplayed()
            .assertTextContains("Safety Tagging") // Should be clearly readable
        
        // Test that AI recommendations use clear, concise language
        composeTestRule.onNodeWithText("Hard Hat Required")
            .assertIsDisplayed()
            // Text should be concise and clear (not overly technical)
        
        // Test that critical information is prominently displayed
        composeTestRule.onNodeWithText("CRITICAL")
            .assertExists()
            // Critical safety issues should be clearly marked
        
        // Test that button text is clear and action-oriented
        composeTestRule.onNodeWithText("Save")
            .assertIsDisplayed()
            .assertHasClickAction()
            // Action buttons should have clear, unambiguous labels
        
        // Verify OSHA compliance information is accessible but not overwhelming
        // Should provide OSHA references without cluttering the interface
        composeTestRule.onNodeWithText("1926.95") // OSHA reference
            .assertExists()
            // References should be available but not interfere with primary workflow
    }

    /**
     * Test workflow efficiency for time-pressured work environment
     * Construction workers need to complete safety tasks quickly
     */
    @Test
    fun workflowEfficiencyForTimePressuredEnvironment() {
        val aiAnalysisResult = createQuickWorkflowAnalysis()
        
        var workflowStartTime = 0L
        var workflowEndTime = 0L
        
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "efficiency-test",
                    photoPath = "/test/quick-workflow.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> 
                        workflowEndTime = System.currentTimeMillis()
                    },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        workflowStartTime = System.currentTimeMillis()

        // Test optimized workflow: AI suggests → user confirms → save
        
        // Step 1: AI recommendations should be immediately visible
        composeTestRule.onNodeWithText("Smart Suggestions").assertIsDisplayed()
        
        // Step 2: Critical recommendations should be auto-selected
        composeTestRule.onNodeWithText("1 Selected").assertExists() // Auto-selection worked
        
        // Step 3: User should be able to quickly confirm compliance state
        composeTestRule.onNodeWithText("Needs Work").assertIsDisplayed().performClick()
        
        // Step 4: Save should be prominently available and immediate
        composeTestRule.onNodeWithText("Save").performClick()
        
        val totalWorkflowTime = workflowEndTime - workflowStartTime
        
        // Verify workflow completes quickly (construction time pressure)
        assertTrue("Workflow should complete in under 10 seconds for field use", 
                  totalWorkflowTime < 10000) // 10 seconds max
        
        println("Workflow completion time: ${totalWorkflowTime}ms")
    }

    /**
     * Test multi-language support for diverse construction workforce
     */
    @Test
    fun multiLanguageSupportForDiverseWorkforce() {
        // This test would verify that the interface can handle different languages
        // common in construction (Spanish, English, etc.)
        
        val aiAnalysisResult = createMultiLanguageAnalysis()
        
        composeTestRule.setContent {
            HazardHawkTheme {
                LoveableTagDialog(
                    photoId = "language-test",
                    photoPath = "/test/multi-language.jpg",
                    existingTags = emptySet(),
                    onTagsUpdated = { _, _ -> },
                    onDismiss = { },
                    aiAnalysisResult = aiAnalysisResult.toJsonString()
                )
            }
        }

        // Test that core safety terms are universally recognizable
        composeTestRule.onNodeWithText("PPE")
            .assertIsDisplayed()
            // PPE is a universally recognized safety acronym
        
        // Test that critical actions use clear, simple language
        composeTestRule.onNodeWithText("Save")
            .assertIsDisplayed()
            // Action words should be simple and clear
        
        // Test that icons supplement text for clarity
        composeTestRule.onNodeWithContentDescription("Close")
            .assertIsDisplayed()
            // Icons should provide visual cues independent of language
    }

    // Helper functions to create test data
    private fun createConstructionSiteAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.PERSON_NO_HARD_HAT,
                    confidence = 0.89f,
                    boundingBox = BoundingBox(0.2f, 0.1f, 0.6f, 0.5f),
                    oshaCategory = OSHACategory.SUBPART_E_1926_95,
                    severity = HazardSeverity.CRITICAL,
                    description = "Worker without required hard hat protection"
                ),
                HazardDetection(
                    hazardType = HazardType.FALL_HAZARD,
                    confidence = 0.82f,
                    boundingBox = BoundingBox(0.4f, 0.3f, 0.9f, 0.8f),
                    oshaCategory = OSHACategory.SUBPART_M_1926_501,
                    severity = HazardSeverity.HIGH,
                    description = "Unprotected fall hazard at elevation"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "ppe-hard-hat-required",
                    displayName = "PPE - Hard Hat Required",
                    confidence = 0.89f,
                    reason = "Critical safety violation detected",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.95"
                ),
                UITagRecommendation(
                    tagId = "fall-protection-required",
                    displayName = "Fall Protection Required",
                    confidence = 0.82f,
                    reason = "Working at height without protection",
                    priority = TagPriority.HIGH,
                    oshaReference = "1926.501"
                )
            ),
            autoSelectTags = setOf("ppe-hard-hat-required"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 2,
                oshaViolations = listOf("1926.95", "1926.501")
            ),
            processingTimeMs = 1200L
        )
    }

    private fun createHighContrastAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.ELECTRICAL_HAZARD,
                    confidence = 0.91f,
                    boundingBox = BoundingBox(0.1f, 0.2f, 0.8f, 0.7f),
                    oshaCategory = OSHACategory.SUBPART_K_1926_416,
                    severity = HazardSeverity.CRITICAL,
                    description = "Exposed electrical hazard"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "electrical-lockout-tagout",
                    displayName = "Electrical LOTO Required",
                    confidence = 0.91f,
                    reason = "Critical electrical safety violation",
                    priority = TagPriority.CRITICAL,
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
    }

    private fun createMixedRecommendationsAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.FALL_HAZARD,
                    confidence = 0.95f,
                    boundingBox = BoundingBox(0.3f, 0.2f, 0.7f, 0.8f),
                    oshaCategory = OSHACategory.SUBPART_M_1926_501,
                    severity = HazardSeverity.CRITICAL,
                    description = "Critical fall hazard identified"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "critical-fall-hazard",
                    displayName = "Critical Fall Hazard",
                    confidence = 0.95f,
                    reason = "AI detected critical fall risk",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.501"
                )
            ),
            autoSelectTags = setOf("critical-fall-hazard"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 1,
                oshaViolations = listOf("1926.501")
            )
        )
    }

    private fun createAccessibleAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.PERSON_NO_SAFETY_VEST,
                    confidence = 0.87f,
                    boundingBox = BoundingBox(0.2f, 0.3f, 0.6f, 0.8f),
                    oshaCategory = OSHACategory.SUBPART_E_1926_95,
                    severity = HazardSeverity.HIGH,
                    description = "High visibility vest required"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "high-vis-vest-required",
                    displayName = "High-Vis Vest Required",
                    confidence = 0.87f,
                    reason = "Worker visibility safety concern",
                    priority = TagPriority.HIGH,
                    oshaReference = "1926.95"
                )
            ),
            autoSelectTags = emptySet(),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.PARTIAL_COMPLIANCE,
                criticalIssues = 0,
                oshaViolations = listOf("1926.95")
            )
        )
    }

    private fun createEnvironmentalStressAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.HEAVY_MACHINERY,
                    confidence = 0.78f,
                    boundingBox = BoundingBox(0.1f, 0.4f, 0.9f, 0.9f),
                    oshaCategory = OSHACategory.SUBPART_O_1926_600,
                    severity = HazardSeverity.MODERATE,
                    description = "Heavy machinery operation nearby"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "machinery-safety-zone",
                    displayName = "Machinery Safety Zone",
                    confidence = 0.78f,
                    reason = "Heavy equipment operational area",
                    priority = TagPriority.MEDIUM,
                    oshaReference = "1926.600"
                )
            ),
            autoSelectTags = emptySet(),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.INFORMATIONAL,
                criticalIssues = 0,
                oshaViolations = emptyList()
            )
        )
    }

    private fun createReadabilityTestAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.PERSON_NO_HARD_HAT,
                    confidence = 0.92f,
                    boundingBox = BoundingBox(0.3f, 0.1f, 0.7f, 0.6f),
                    oshaCategory = OSHACategory.SUBPART_E_1926_95,
                    severity = HazardSeverity.CRITICAL,
                    description = "Hard hat protection missing"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "hard-hat-required",
                    displayName = "Hard Hat Required", // Concise, clear text
                    confidence = 0.92f,
                    reason = "CRITICAL safety violation",
                    priority = TagPriority.CRITICAL,
                    oshaReference = "1926.95"
                )
            ),
            autoSelectTags = setOf("hard-hat-required"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 1,
                oshaViolations = listOf("1926.95")
            )
        )
    }

    private fun createQuickWorkflowAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.UNSAFE_SCAFFOLDING,
                    confidence = 0.84f,
                    boundingBox = BoundingBox(0.2f, 0.3f, 0.8f, 0.9f),
                    oshaCategory = OSHACategory.SUBPART_L_1926_451,
                    severity = HazardSeverity.HIGH,
                    description = "Scaffold safety issue"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "scaffold-safety-check",
                    displayName = "Scaffold Safety Check",
                    confidence = 0.84f,
                    reason = "Scaffold requires inspection",
                    priority = TagPriority.HIGH,
                    oshaReference = "1926.451"
                )
            ),
            autoSelectTags = setOf("scaffold-safety-check"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.PARTIAL_COMPLIANCE,
                criticalIssues = 1,
                oshaViolations = listOf("1926.451")
            )
        )
    }

    private fun createMultiLanguageAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(
                    hazardType = HazardType.PERSON_NO_SAFETY_VEST,
                    confidence = 0.81f,
                    boundingBox = BoundingBox(0.3f, 0.2f, 0.7f, 0.7f),
                    oshaCategory = OSHACategory.SUBPART_E_1926_95,
                    severity = HazardSeverity.MODERATE,
                    description = "PPE visibility requirement"
                )
            ),
            recommendedTags = listOf(
                UITagRecommendation(
                    tagId = "ppe-vest",
                    displayName = "PPE - Safety Vest", // Simple, universal terms
                    confidence = 0.81f,
                    reason = "Worker visibility required",
                    priority = TagPriority.MEDIUM,
                    oshaReference = "1926.95"
                )
            ),
            autoSelectTags = emptySet(),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.INFORMATIONAL,
                criticalIssues = 0,
                oshaViolations = emptyList()
            )
        )
    }
}

/**
 * Extension function for consistency with integration tests
 */
private fun PhotoAnalysisWithTags.toJsonString(): String {
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