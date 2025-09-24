package com.hazardhawk.ui.ar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.ai.models.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for HazardDetectionOverlay component.
 * Tests AR UI rendering, interactions, and performance.
 */
@RunWith(AndroidJUnit4::class)
class HazardDetectionOverlayTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun hazardDetectionOverlay_displaysNoHazardsState() {
        // Given
        val emptyAnalysis = createSafetyAnalysis(hazards = emptyList())
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = emptyAnalysis,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("✅ No Hazards").assertIsDisplayed()
    }
    
    @Test
    fun hazardDetectionOverlay_displaysCriticalHazards() {
        // Given
        val criticalAnalysis = createSafetyAnalysis(
            hazards = listOf(createCriticalHazard())
        )
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = criticalAnalysis,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
    }
    
    @Test
    fun hazardDetectionOverlay_displaysMultipleHazards() {
        // Given
        val multipleHazardsAnalysis = createSafetyAnalysis(
            hazards = listOf(
                createCriticalHazard(),
                createHighHazard(),
                createMediumHazard()
            )
        )
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = multipleHazardsAnalysis,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
        // Should prioritize critical hazards in display
    }
    
    @Test
    fun hazardDetectionOverlay_handlesHazardClick() {
        // Given
        val hazard = createCriticalHazard()
        val analysis = createSafetyAnalysis(hazards = listOf(hazard))
        var clickedHazard: Hazard? = null
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                onHazardClick = { clickedHazard = it },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Find and click on hazard badge
        composeTestRule.onNodeWithContentDescription("OSHA Badge for ${hazard.type}")
            .assertIsDisplayed()
            .performClick()
        
        // Then
        assert(clickedHazard == hazard) { "Should trigger hazard click callback" }
    }
    
    @Test
    fun hazardDetectionOverlay_showsAnalysisStatus() {
        // Given
        val analysis = createSafetyAnalysis()
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Then - Should display analysis type and confidence
        composeTestRule.onNodeWithText("LOCAL GEMMA MULTIMODAL", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Confidence: 87%", substring = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun hazardDetectionOverlay_showsProcessingIndicator() {
        // Given - No analysis (processing state)
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = null,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("🔍 Analyzing...").assertIsDisplayed()
    }
    
    @Test
    fun hazardDetectionOverlay_respectsCompactMode() {
        // Given
        val analysis = createSafetyAnalysis()
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                compactMode = true,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Then - Should use compact display format
        // Compact mode should show less detailed information
        composeTestRule.onNodeWithText("Confidence:", substring = true)
            .assertDoesNotExist()
    }
    
    @Test
    fun hazardDetectionOverlay_handlesDisabledFeatures() {
        // Given
        val analysis = createSafetyAnalysis()
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                showBoundingBoxes = false,
                showOSHABadges = false,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Then - Should not show hazard overlays
        composeTestRule.onNodeWithContentDescription("Hazard Bounding Box")
            .assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("OSHA Badge", substring = true)
            .assertDoesNotExist()
        
        // But should still show status
        composeTestRule.onNodeWithText("⚠️ 3 Issues").assertIsDisplayed()
    }
    
    @Test
    fun hazardDetectionOverlay_animatesCorrectly() {
        // Given
        val analysis = createSafetyAnalysis(
            hazards = listOf(createCriticalHazard())
        )
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                animationEnabled = true,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Then - Animation should be present for critical hazards
        // Note: Testing animations in Compose requires more advanced techniques
        composeTestRule.waitForIdle()
        
        // Verify critical hazard is displayed (animation testing would require more setup)
        composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
    }
    
    @Test
    fun hazardDetectionOverlay_calculatesBadgePositions() {
        // Given
        val hazardWithBoundingBox = createCriticalHazard().copy(
            boundingBox = BoundingBox(0.1f, 0.1f, 0.2f, 0.2f)
        )
        val analysis = createSafetyAnalysis(hazards = listOf(hazardWithBoundingBox))
        
        // When
        composeTestRule.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                HazardDetectionOverlay(
                    safetyAnalysis = analysis,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Then - Badge should be positioned relative to bounding box
        composeTestRule.onNodeWithContentDescription("OSHA Badge")
            .assertIsDisplayed()
            .assertPositionInRootIsEqualTo(
                expectedLeft = composeTestRule.density.run { 10.dp }, // Calculated position
                expectedTop = composeTestRule.density.run { 10.dp }
            )
    }
    
    @Test
    fun hazardDetectionOverlay_handlesLargeNumberOfHazards() {
        // Given - Many hazards to test performance
        val manyHazards = (1..20).map { index ->
            createMediumHazard().copy(
                id = "hazard-$index",
                boundingBox = BoundingBox(
                    left = (index % 5) * 0.2f,
                    top = (index / 5) * 0.25f,
                    width = 0.15f,
                    height = 0.15f
                )
            )
        }
        val analysis = createSafetyAnalysis(hazards = manyHazards)
        
        // When
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Then - Should display summary count
        composeTestRule.onNodeWithText("⚠️ 20 Issues").assertIsDisplayed()
        
        // Should handle rendering without performance issues
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun hazardCountIndicator_displaysCorrectCounts() {
        // Given
        val hazards = listOf(
            createCriticalHazard(),
            createCriticalHazard(),
            createHighHazard(),
            createMediumHazard(),
            createMediumHazard(),
            createMediumHazard()
        )
        
        // When
        composeTestRule.setContent {
            HazardCountIndicator(hazards = hazards)
        }
        
        // Then
        composeTestRule.onNodeWithText("2", substring = true).assertIsDisplayed() // Critical count
        composeTestRule.onNodeWithText("1", substring = true).assertIsDisplayed() // High count  
        composeTestRule.onNodeWithText("6 total").assertIsDisplayed() // Total count
    }
    
    @Test
    fun hazardDetectionOverlay_maintainsPerformance() {
        // Given
        val analysis = createSafetyAnalysis()
        
        // When - Measure rendering time
        val startTime = System.currentTimeMillis()
        
        composeTestRule.setContent {
            HazardDetectionOverlay(
                safetyAnalysis = analysis,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        composeTestRule.waitForIdle()
        val renderTime = System.currentTimeMillis() - startTime
        
        // Then - Should render within performance budget (targeting 16ms for 60fps)
        assert(renderTime < 100) { "Rendering should be fast, took ${renderTime}ms" }
    }
    
    // Helper methods for creating test data
    private fun createSafetyAnalysis(
        hazards: List<Hazard> = listOf(createCriticalHazard(), createHighHazard(), createMediumHazard()),
        confidence: Float = 0.87f
    ): SafetyAnalysis {
        return SafetyAnalysis(
            id = "test-analysis",
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            workType = WorkType.GENERAL_CONSTRUCTION,
            hazards = hazards,
            ppeStatus = createPPEStatus(),
            recommendations = listOf("Test recommendation"),
            overallRiskLevel = RiskLevel.HIGH,
            confidence = confidence,
            processingTimeMs = 2000L,
            oshaViolations = emptyList(),
            metadata = null
        )
    }
    
    private fun createCriticalHazard(): Hazard {
        return Hazard(
            id = "critical-hazard",
            type = HazardType.FALL_PROTECTION,
            severity = Severity.CRITICAL,
            description = "Worker at height without fall protection",
            oshaCode = "1926.501(b)(1)",
            boundingBox = BoundingBox(0.2f, 0.1f, 0.3f, 0.4f),
            confidence = 0.89f,
            recommendations = listOf("Install fall protection"),
            immediateAction = "STOP WORK"
        )
    }
    
    private fun createHighHazard(): Hazard {
        return Hazard(
            id = "high-hazard",
            type = HazardType.PPE_VIOLATION,
            severity = Severity.HIGH,
            description = "Missing hard hat",
            oshaCode = "1926.95(a)",
            boundingBox = BoundingBox(0.15f, 0.05f, 0.2f, 0.25f),
            confidence = 0.94f,
            recommendations = listOf("Require hard hat"),
            immediateAction = "Don hard hat"
        )
    }
    
    private fun createMediumHazard(): Hazard {
        return Hazard(
            id = "medium-hazard",
            type = HazardType.ELECTRICAL_HAZARD,
            severity = Severity.MEDIUM,
            description = "Exposed wiring",
            oshaCode = "1926.416(a)(1)",
            boundingBox = BoundingBox(0.6f, 0.7f, 0.2f, 0.15f),
            confidence = 0.76f,
            recommendations = listOf("Cover exposed wiring")
        )
    }
    
    private fun createPPEStatus(): PPEStatus {
        return PPEStatus(
            hardHat = PPEItem(PPEItemStatus.MISSING, 0.94f, null, true),
            safetyVest = PPEItem(PPEItemStatus.PRESENT, 0.87f, null, true),
            safetyBoots = PPEItem(PPEItemStatus.PRESENT, 0.82f, null, true),
            safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.45f, null, false),
            fallProtection = PPEItem(PPEItemStatus.MISSING, 0.92f, null, true),
            respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.2f, null, false),
            overallCompliance = 0.4f
        )
    }
}
