package com.hazardhawk

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.ai.*
import com.hazardhawk.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.runner.RunWith
import org.mockito.*
import org.mockito.kotlin.*
import java.io.File

/**
 * End-to-End Workflow Tests for Complete AI Photo Analysis Flow
 * 
 * These tests validate the complete workflow from photo capture through AI analysis
 * to tag selection and final storage. Critical for ensuring the AI workflow fix
 * works in real-world scenarios.
 * 
 * Tests cover:
 * 1. Photo capture → AI analysis → Results storage → Tag dialog display
 * 2. Multiple work types and scenarios
 * 3. Error handling and recovery
 * 4. Performance under field conditions
 */
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class AIWorkflowEndToEndTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<CameraGalleryActivity>()

    private val testDispatcher = TestCoroutineDispatcher()
    private lateinit var testScope: TestCoroutineScope
    
    @Mock
    private lateinit var mockAIServiceFacade: AIServiceFacade

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testScope = TestCoroutineScope(testDispatcher)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cleanupTestCoroutines()
    }

    /**
     * CRITICAL TEST: Complete AI workflow from photo capture to tag selection
     * This is the primary end-to-end test validating the complete fix
     */
    @Test
    fun completeAIWorkflowFromPhotoCaptureToTagSelection() {
        // Given: Successful AI analysis setup
        val expectedAnalysisResult = createConstructionSiteAIAnalysis()
        
        whenever(mockAIServiceFacade.analyzePhotoWithTags(
            any(), any(), any(), any()
        )).thenReturn(expectedAnalysisResult)

        whenever(mockAIServiceFacade.initialize()).thenReturn(Result.success(Unit))
        whenever(mockAIServiceFacade.getModelStatus()).thenReturn(
            ModelStatus(isLoaded = true, modelName = "Gemma-3N-E2B", lastError = null)
        )

        // Step 1: Navigate to camera screen
        composeTestRule.onNodeWithText("Take Photo")
            .assertIsDisplayed()
            .performClick()

        // Wait for camera to initialize
        composeTestRule.waitForIdle()

        // Step 2: Capture photo (simulate camera capture)
        composeTestRule.onNodeWithContentDescription("Camera capture button")
            .assertIsDisplayed()
            .performClick()

        // Wait for photo capture and AI processing
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            // Wait for AI processing to complete and tag dialog to appear
            composeTestRule.onAllNodesWithText("Safety Tagging").fetchSemanticsNodes().isNotEmpty()
        }

        // Step 3: Verify AI analysis results are displayed in tag dialog
        composeTestRule.onNodeWithText("Safety Tagging").assertIsDisplayed()
        composeTestRule.onNodeWithText("Smart Suggestions").assertIsDisplayed()
        
        // Verify specific AI recommendations are shown
        composeTestRule.onNodeWithText("Fall Protection Required").assertIsDisplayed()
        composeTestRule.onNodeWithText("PPE - Hard Hat Required").assertIsDisplayed()
        
        // Verify AI badge is present
        composeTestRule.onNodeWithText("AI").assertIsDisplayed()
        
        // Verify compliance state based on AI analysis
        composeTestRule.onNodeWithText("Needs Work").assertIsDisplayed()

        // Step 4: User interacts with AI recommendations
        composeTestRule.onNodeWithText("Fall Protection Required")
            .assertIsDisplayed()
            .performClick()
        
        composeTestRule.onNodeWithText("PPE - Hard Hat Required")
            .assertIsDisplayed() 
            .performClick()

        // Step 5: Save tags with compliance status
        composeTestRule.onNodeWithText("Save").performClick()

        // Step 6: Verify completion - should return to gallery or main screen
        composeTestRule.waitForIdle()
        
        // Verify we're back to main interface (photo count should be updated)
        composeTestRule.onNodeWithText("Photo saved!").assertIsDisplayed()
    }

    /**
     * CRITICAL TEST: Construction worker scenarios with different work types
     */
    @Test
    fun constructionWorkerScenariosWithDifferentWorkTypes() = testScope.runBlockingTest {
        
        // Test Scenario 1: Electrical Work
        testWorkTypeScenario(
            workType = WorkType.ELECTRICAL_WORK,
            expectedHazards = listOf(
                HazardType.ELECTRICAL_HAZARD,
                HazardType.PERSON_NO_SAFETY_VEST
            ),
            expectedTags = listOf(
                "electrical-safety",
                "lockout-tagout-required",
                "ppe-electrical"
            ),
            expectedOSHAReferences = listOf("1926.416"),
            scenarioName = "Electrical Work Safety Assessment"
        )

        // Test Scenario 2: Roofing Work
        testWorkTypeScenario(
            workType = WorkType.ROOFING,
            expectedHazards = listOf(
                HazardType.FALL_HAZARD,
                HazardType.UNPROTECTED_EDGE,
                HazardType.IMPROPER_LADDER_USE
            ),
            expectedTags = listOf(
                "fall-protection-required",
                "edge-protection",
                "ladder-safety"
            ),
            expectedOSHAReferences = listOf("1926.501", "1926.502"),
            scenarioName = "Roofing Safety Assessment"
        )

        // Test Scenario 3: Excavation Work
        testWorkTypeScenario(
            workType = WorkType.EXCAVATION,
            expectedHazards = listOf(
                HazardType.CAVE_IN_HAZARD,
                HazardType.HEAVY_MACHINERY
            ),
            expectedTags = listOf(
                "excavation-safety",
                "shoring-required",
                "competent-person"
            ),
            expectedOSHAReferences = listOf("1926.651", "1926.652"),
            scenarioName = "Excavation Safety Assessment"
        )
    }

    /**
     * Test AI failure recovery and retry functionality
     */
    @Test
    fun aiFailureRecoveryAndRetryFunctionality() {
        // Given: AI service initially fails then recovers
        whenever(mockAIServiceFacade.analyzePhotoWithTags(any(), any(), any(), any()))
            .thenThrow(RuntimeException("AI service temporarily unavailable"))
            .thenReturn(createFallbackAnalysisResult())

        // Step 1: Attempt photo capture with initial AI failure
        composeTestRule.onNodeWithText("Take Photo").performClick()
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithContentDescription("Camera capture button").performClick()
        
        // Step 2: Should gracefully handle AI failure
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithText("Safety Tagging").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Should show fallback tag suggestions (no AI badge)
        composeTestRule.onNodeWithText("Safety Tagging").assertIsDisplayed()
        composeTestRule.onNodeWithText("Smart Suggestions").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("AI").assertCountEquals(0) // No AI badges for fallback

        // Should show generic construction safety options
        composeTestRule.onNodeWithText("General Safety").assertExists()
        composeTestRule.onNodeWithText("PPE Review").assertExists()
        
        // Step 3: User can still complete workflow with generic suggestions
        composeTestRule.onNodeWithText("General Safety").performClick()
        composeTestRule.onNodeWithText("Save").performClick()
        
        // Should complete successfully despite AI failure
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Photo saved!").assertIsDisplayed()
    }

    /**
     * Test batch photo analysis workflow
     */
    @Test
    fun batchPhotoAnalysisWorkflow() {
        // Given: Multiple photos with AI analysis
        val batchAnalysisResults = listOf(
            createConstructionSiteAIAnalysis(),
            createElectricalWorkAIAnalysis(),
            createRoofingWorkAIAnalysis()
        )
        
        whenever(mockAIServiceFacade.analyzePhotoWithTags(any(), any(), any(), any()))
            .thenReturn(batchAnalysisResults[0])
            .thenReturn(batchAnalysisResults[1])
            .thenReturn(batchAnalysisResults[2])

        // Step 1: Navigate to gallery with existing photos
        composeTestRule.activity.intent.putExtra("DIRECT_TO_GALLERY", true)
        
        // Step 2: Select multiple photos for batch AI analysis
        composeTestRule.waitForIdle()
        
        // Long press to enter multi-select mode
        composeTestRule.onAllNodesWithContentDescription("Safety photo")[0]
            .performTouchInput { longClick() }
        
        // Select additional photos
        composeTestRule.onAllNodesWithContentDescription("Safety photo")[1].performClick()
        composeTestRule.onAllNodesWithContentDescription("Safety photo")[2].performClick()
        
        // Step 3: Initiate batch AI analysis
        composeTestRule.onNodeWithText("AI Analysis").performClick()
        
        // Step 4: Monitor batch processing progress
        composeTestRule.onNodeWithText("Analyzing Images...").assertIsDisplayed()
        
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Analysis Complete").fetchSemanticsNodes().isNotEmpty()
        }
        
        // Step 5: Verify batch results are available
        composeTestRule.onNodeWithText("Analysis Complete").assertIsDisplayed()
        
        // Should show aggregated results
        composeTestRule.onNodeWithText("3 photos analyzed").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 hazards detected").assertIsDisplayed() // Total from all analyses
    }

    /**
     * Test offline/online transition workflow
     */
    @Test
    fun offlineOnlineTransitionWorkflow() {
        // Given: Offline scenario (AI service unavailable)
        whenever(mockAIServiceFacade.analyzePhotoWithTags(any(), any(), any(), any()))
            .thenThrow(java.net.ConnectException("No network connection"))

        // Step 1: Capture photo while offline
        composeTestRule.onNodeWithText("Take Photo").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Camera capture button").performClick()

        // Step 2: Should queue for later processing
        composeTestRule.waitUntil(timeoutMillis = 8000) {
            composeTestRule.onAllNodesWithText("Queued for AI Analysis").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("Queued for AI Analysis").assertIsDisplayed()
        
        // Step 3: User can still add manual tags
        composeTestRule.onNodeWithText("Safety Tagging").assertIsDisplayed()
        composeTestRule.onNodeWithText("General Safety").performClick()
        composeTestRule.onNodeWithText("Save").performClick()

        // Step 4: Simulate coming back online
        whenever(mockAIServiceFacade.analyzePhotoWithTags(any(), any(), any(), any()))
            .thenReturn(createConstructionSiteAIAnalysis())

        // Step 5: Trigger sync when back online
        composeTestRule.onNodeWithText("Sync").performClick()
        
        // Should process queued photos
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("AI analysis complete").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Test complex construction site scenario
     */
    @Test
    fun complexConstructionSiteScenario() {
        // Given: Complex multi-hazard construction site
        val complexAnalysis = PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(HazardType.PERSON_NO_HARD_HAT, 0.92f, BoundingBox(0.2f, 0.1f, 0.4f, 0.4f), 
                              OSHACategory.SUBPART_E_1926_95, HazardSeverity.CRITICAL, "Worker without hard hat"),
                HazardDetection(HazardType.FALL_HAZARD, 0.87f, BoundingBox(0.6f, 0.2f, 0.9f, 0.8f), 
                              OSHACategory.SUBPART_M_1926_501, HazardSeverity.CRITICAL, "Unprotected edge"),
                HazardDetection(HazardType.ELECTRICAL_HAZARD, 0.81f, BoundingBox(0.1f, 0.6f, 0.3f, 0.9f), 
                              OSHACategory.SUBPART_K_1926_416, HazardSeverity.HIGH, "Exposed wiring"),
                HazardDetection(HazardType.HEAVY_MACHINERY, 0.76f, BoundingBox(0.4f, 0.7f, 0.8f, 1.0f), 
                              OSHACategory.SUBPART_O_1926_600, HazardSeverity.MODERATE, "Operating equipment nearby")
            ),
            recommendedTags = listOf(
                UITagRecommendation("ppe-hard-hat-critical", "PPE - Hard Hat CRITICAL", 0.92f, 
                                   "Immediate hard hat required", TagPriority.CRITICAL, "1926.95"),
                UITagRecommendation("fall-protection-critical", "Fall Protection CRITICAL", 0.87f, 
                                   "Immediate fall protection required", TagPriority.CRITICAL, "1926.501"),
                UITagRecommendation("electrical-safety-high", "Electrical Safety HIGH", 0.81f, 
                                   "Electrical hazard requires attention", TagPriority.HIGH, "1926.416"),
                UITagRecommendation("equipment-safety", "Equipment Safety", 0.76f, 
                                   "Heavy equipment safety protocols", TagPriority.MEDIUM, "1926.600")
            ),
            autoSelectTags = setOf("ppe-hard-hat-critical", "fall-protection-critical"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 3,
                oshaViolations = listOf("1926.95", "1926.501", "1926.416")
            ),
            processingTimeMs = 2150L
        )

        whenever(mockAIServiceFacade.analyzePhotoWithTags(any(), any(), any(), any()))
            .thenReturn(complexAnalysis)

        // Execute complete workflow
        composeTestRule.onNodeWithText("Take Photo").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Camera capture button").performClick()

        // Should handle complex analysis results
        composeTestRule.waitUntil(timeoutMillis = 12000) {
            composeTestRule.onAllNodesWithText("Safety Tagging").fetchSemanticsNodes().isNotEmpty()
        }

        // Should prioritize critical issues
        composeTestRule.onNodeWithText("PPE - Hard Hat CRITICAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fall Protection CRITICAL").assertIsDisplayed()
        
        // Should show multiple OSHA violations
        composeTestRule.onNodeWithText("Needs Work").assertIsDisplayed() // Non-compliant state
        
        // Critical tags should be auto-selected
        composeTestRule.onNodeWithText("2 Selected").assertIsDisplayed() // Auto-selected count
        
        // User should be able to save complex analysis
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Photo saved!").assertIsDisplayed()
    }

    // Helper function to test different work type scenarios
    private suspend fun testWorkTypeScenario(
        workType: WorkType,
        expectedHazards: List<HazardType>,
        expectedTags: List<String>,
        expectedOSHAReferences: List<String>,
        scenarioName: String
    ) {
        val analysisResult = createWorkTypeSpecificAnalysis(workType, expectedHazards, expectedTags, expectedOSHAReferences)
        
        whenever(mockAIServiceFacade.analyzePhotoWithTags(
            any(), any(), any(), eq(workType)
        )).thenReturn(analysisResult)

        // Execute workflow for this work type
        composeTestRule.onNodeWithText("Take Photo").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Camera capture button").performClick()

        // Wait for work-type specific analysis
        composeTestRule.waitUntil(timeoutMillis = 10000) {
            composeTestRule.onAllNodesWithText("Safety Tagging").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify work-type specific recommendations
        expectedTags.forEach { tagName ->
            composeTestRule.onNodeWithText(tagName, useUnmergedTree = true).assertExists()
        }

        // Save and complete
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()
    }

    // Helper functions to create test data
    private fun createConstructionSiteAIAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(HazardType.FALL_HAZARD, 0.89f, BoundingBox(0.3f, 0.2f, 0.7f, 0.8f), 
                              OSHACategory.SUBPART_M_1926_501, HazardSeverity.CRITICAL, "Unprotected fall hazard"),
                HazardDetection(HazardType.PERSON_NO_HARD_HAT, 0.83f, BoundingBox(0.1f, 0.1f, 0.3f, 0.5f), 
                              OSHACategory.SUBPART_E_1926_95, HazardSeverity.HIGH, "Worker without hard hat")
            ),
            recommendedTags = listOf(
                UITagRecommendation("fall-protection-required", "Fall Protection Required", 0.89f, 
                                   "Critical fall hazard identified", TagPriority.CRITICAL, "1926.501"),
                UITagRecommendation("ppe-hard-hat-required", "PPE - Hard Hat Required", 0.83f, 
                                   "Worker missing required hard hat", TagPriority.HIGH, "1926.95")
            ),
            autoSelectTags = setOf("fall-protection-required"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 2,
                oshaViolations = listOf("1926.501", "1926.95")
            ),
            processingTimeMs = 1540L
        )
    }

    private fun createElectricalWorkAIAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(HazardType.ELECTRICAL_HAZARD, 0.91f, BoundingBox(0.2f, 0.3f, 0.8f, 0.7f), 
                              OSHACategory.SUBPART_K_1926_416, HazardSeverity.CRITICAL, "Exposed electrical equipment")
            ),
            recommendedTags = listOf(
                UITagRecommendation("electrical-safety", "Electrical Safety Required", 0.91f, 
                                   "Electrical hazard requires immediate attention", TagPriority.CRITICAL, "1926.416"),
                UITagRecommendation("lockout-tagout-required", "Lockout/Tagout Required", 0.87f, 
                                   "LOTO procedures must be implemented", TagPriority.HIGH, "1926.416")
            ),
            autoSelectTags = setOf("electrical-safety"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.NON_COMPLIANT,
                criticalIssues = 1,
                oshaViolations = listOf("1926.416")
            ),
            processingTimeMs = 1320L
        )
    }

    private fun createRoofingWorkAIAnalysis(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = listOf(
                HazardDetection(HazardType.UNPROTECTED_EDGE, 0.88f, BoundingBox(0.4f, 0.1f, 0.9f, 0.6f), 
                              OSHACategory.SUBPART_M_1926_501, HazardSeverity.CRITICAL, "Roof edge without protection"),
                HazardDetection(HazardType.IMPROPER_LADDER_USE, 0.74f, BoundingBox(0.1f, 0.4f, 0.3f, 1.0f), 
                              OSHACategory.SUBPART_X_1926_1053, HazardSeverity.MODERATE, "Ladder positioning issue")
            ),
            recommendedTags = listOf(
                UITagRecommendation("edge-protection", "Edge Protection Required", 0.88f, 
                                   "Roof edge requires immediate protection", TagPriority.CRITICAL, "1926.501"),
                UITagRecommendation("ladder-safety", "Ladder Safety Review", 0.74f, 
                                   "Ladder setup needs safety review", TagPriority.MEDIUM, "1926.1053")
            ),
            autoSelectTags = setOf("edge-protection"),
            complianceOverview = ComplianceOverview(
                overallLevel = ComplianceLevel.PARTIAL_COMPLIANCE,
                criticalIssues = 1,
                oshaViolations = listOf("1926.501")
            ),
            processingTimeMs = 1670L
        )
    }

    private fun createFallbackAnalysisResult(): PhotoAnalysisWithTags {
        return PhotoAnalysisWithTags(
            detections = emptyList(),
            recommendedTags = listOf(
                UITagRecommendation("general-safety-check", "General Safety", 0.5f, 
                                   "Basic safety review recommended"),
                UITagRecommendation("ppe-review", "PPE Review", 0.5f, 
                                   "Check personal protective equipment")
            ),
            autoSelectTags = emptySet(),
            complianceOverview = ComplianceOverview.basic(),
            processingTimeMs = 500L
        )
    }

    private fun createWorkTypeSpecificAnalysis(
        workType: WorkType,
        hazards: List<HazardType>,
        tags: List<String>,
        oshaRefs: List<String>
    ): PhotoAnalysisWithTags {
        val detections = hazards.mapIndexed { index, hazardType ->
            HazardDetection(
                hazardType = hazardType,
                confidence = 0.8f + (index * 0.05f),
                boundingBox = BoundingBox(0.1f + index * 0.2f, 0.1f, 0.3f, 0.4f),
                oshaCategory = OSHACategory.SUBPART_E_1926_95, // Simplified for test
                severity = HazardSeverity.HIGH,
                description = "Work type specific hazard: $hazardType"
            )
        }

        val recommendations = tags.mapIndexed { index, tagName ->
            UITagRecommendation(
                tagId = tagName.lowercase().replace(" ", "-"),
                displayName = tagName,
                confidence = 0.8f + (index * 0.05f),
                reason = "Work type specific recommendation for $workType",
                priority = TagPriority.HIGH,
                oshaReference = oshaRefs.getOrNull(index % oshaRefs.size)
            )
        }

        return PhotoAnalysisWithTags(
            detections = detections,
            recommendedTags = recommendations,
            autoSelectTags = recommendations.take(2).map { it.tagId }.toSet(),
            complianceOverview = ComplianceOverview(
                overallLevel = if (hazards.isNotEmpty()) ComplianceLevel.PARTIAL_COMPLIANCE else ComplianceLevel.COMPLIANT,
                criticalIssues = hazards.size,
                oshaViolations = oshaRefs
            ),
            processingTimeMs = 1200L + (hazards.size * 200L)
        )
    }
}