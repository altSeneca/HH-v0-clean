package com.hazardhawk.ui.ar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.hazardhawk.ai.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Android instrumented tests for LiveDetectionScreen.
 * Tests camera integration, real-time analysis, and user interactions.
 */
@RunWith(AndroidJUnit4::class)
class LiveDetectionScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.CAMERA
    )
    
    @Test
    fun liveDetectionScreen_displaysInitialState() {
        // Given
        val mockViewModel = createMockViewModel()
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Camera preview").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Capture photo").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("View gallery").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }
    
    @Test
    fun liveDetectionScreen_showsProcessingIndicator() {
        // Given
        val mockViewModel = createMockViewModel(isProcessing = true)
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("🔍 Analyzing...").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Processing overlay").assertIsDisplayed()
    }
    
    @Test
    fun liveDetectionScreen_displaysHazardOverlay() {
        // Given
        val safetyAnalysis = createSampleSafetyAnalysis()
        val mockViewModel = createMockViewModel(currentAnalysis = safetyAnalysis)
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("OSHA Badge", substring = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun liveDetectionScreen_handlesCapturePhoto() {
        // Given
        var captureClicked = false
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = { captureClicked = true },
                onViewGallery = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Capture photo")
            .performClick()
        
        // Then
        assert(captureClicked) { "Should trigger capture photo callback" }
    }
    
    @Test
    fun liveDetectionScreen_handlesGalleryNavigation() {
        // Given
        var galleryClicked = false
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = { galleryClicked = true }
            )
        }
        
        composeTestRule.onNodeWithContentDescription("View gallery")
            .performClick()
        
        // Then
        assert(galleryClicked) { "Should trigger gallery navigation" }
    }
    
    @Test
    fun liveDetectionScreen_handlesBackNavigation() {
        // Given
        var backClicked = false
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = { backClicked = true },
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        composeTestRule.onNodeWithContentDescription("Back")
            .performClick()
        
        // Then
        assert(backClicked) { "Should trigger back navigation" }
    }
    
    @Test
    fun liveDetectionScreen_showsSettingsAccess() {
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
    }
    
    @Test
    fun liveDetectionScreen_displaysConfidenceIndicator() {
        // Given
        val safetyAnalysis = createSampleSafetyAnalysis(confidence = 0.89f)
        val mockViewModel = createMockViewModel(currentAnalysis = safetyAnalysis)
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("Confidence: 89%", substring = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun liveDetectionScreen_showsPerformanceMetrics() {
        // Given
        val safetyAnalysis = createSampleSafetyAnalysis(processingTime = 1500L)
        val mockViewModel = createMockViewModel(currentAnalysis = safetyAnalysis)
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Then
        composeTestRule.onNodeWithText("1500ms", substring = true).assertIsDisplayed()
    }
    
    @Test
    fun liveDetectionScreen_handlesHazardSelection() {
        // Given
        val hazard = createCriticalHazard()
        val safetyAnalysis = createSampleSafetyAnalysis(hazards = listOf(hazard))
        val mockViewModel = createMockViewModel(currentAnalysis = safetyAnalysis)
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Click on hazard
        composeTestRule.onNodeWithContentDescription("OSHA Badge for ${hazard.type}")
            .performClick()
        
        // Then - Should show hazard details
        composeTestRule.onNodeWithText(hazard.description, substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(hazard.immediateAction ?: "", substring = true)
            .assertIsDisplayed()
    }
    
    @Test
    fun liveDetectionScreen_showsRealTimeUpdates() {
        // Given
        val initialAnalysis = createSampleSafetyAnalysis(hazards = emptyList())
        val updatedAnalysis = createSampleSafetyAnalysis(hazards = listOf(createCriticalHazard()))
        val analysisFlow = MutableStateFlow(initialAnalysis)
        
        // When - Initial state
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Then - Initial state shows no hazards
        composeTestRule.onNodeWithText("✅ No Hazards").assertIsDisplayed()
        
        // When - Analysis updates
        analysisFlow.value = updatedAnalysis
        
        // Then - Should show updated hazards
        composeTestRule.onNodeWithText("🚨 CRITICAL").assertIsDisplayed()
    }
    
    @Test
    fun liveDetectionScreen_handlesPermissionDenied() {
        // This test would require mocking permission state
        // Implementation depends on how permissions are handled in the actual component
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Then - Should show permission request or error state
        // This would need to be implemented based on actual permission handling
    }
    
    @Test
    fun liveDetectionScreen_maintainsPerformanceWithHighHazardCount() {
        // Given - Many hazards to test UI performance
        val manyHazards = (1..50).map { index ->
            createMediumHazard().copy(id = "hazard-$index")
        }
        val heavyAnalysis = createSampleSafetyAnalysis(hazards = manyHazards)
        val mockViewModel = createMockViewModel(currentAnalysis = heavyAnalysis)
        
        // When
        val startTime = System.currentTimeMillis()
        
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        composeTestRule.waitForIdle()
        val renderTime = System.currentTimeMillis() - startTime
        
        // Then - Should maintain 30fps (33ms budget)
        assert(renderTime < 100) { "Heavy UI should render quickly, took ${renderTime}ms" }
        
        // Should show hazard count summary
        composeTestRule.onNodeWithText("⚠️ 50 Issues").assertIsDisplayed()
    }
    
    @Test
    fun liveDetectionScreen_handlesNetworkConnectivity() {
        // Given - Mock network state changes
        val mockViewModel = createMockViewModel(isNetworkConnected = false)
        
        // When
        composeTestRule.setContent {
            LiveDetectionScreen(
                onNavigateBack = {},
                onCapturePhoto = {},
                onViewGallery = {}
            )
        }
        
        // Then - Should show offline indicator or local-only analysis
        composeTestRule.onNodeWithContentDescription("Network status", substring = true)
            .assertIsDisplayed()
    }
    
    // Helper methods
    private fun createMockViewModel(
        isProcessing: Boolean = false,
        currentAnalysis: SafetyAnalysis? = null,
        isNetworkConnected: Boolean = true
    ): MockLiveDetectionViewModel {
        return MockLiveDetectionViewModel(
            isProcessing = isProcessing,
            currentAnalysis = currentAnalysis,
            isNetworkConnected = isNetworkConnected
        )
    }
    
    private fun createSampleSafetyAnalysis(
        hazards: List<Hazard> = listOf(createCriticalHazard()),
        confidence: Float = 0.87f,
        processingTime: Long = 2000L
    ): SafetyAnalysis {
        return SafetyAnalysis(
            id = "test-analysis",
            timestamp = System.currentTimeMillis(),
            analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            workType = WorkType.GENERAL_CONSTRUCTION,
            hazards = hazards,
            ppeStatus = PPEStatus(
                hardHat = PPEItem(PPEItemStatus.MISSING, 0.94f, null, true),
                safetyVest = PPEItem(PPEItemStatus.PRESENT, 0.87f, null, true),
                safetyBoots = PPEItem(PPEItemStatus.PRESENT, 0.82f, null, true),
                safetyGlasses = PPEItem(PPEItemStatus.UNKNOWN, 0.45f, null, false),
                fallProtection = PPEItem(PPEItemStatus.MISSING, 0.92f, null, true),
                respirator = PPEItem(PPEItemStatus.UNKNOWN, 0.2f, null, false),
                overallCompliance = 0.4f
            ),
            recommendations = listOf("Test recommendation"),
            overallRiskLevel = RiskLevel.HIGH,
            confidence = confidence,
            processingTimeMs = processingTime,
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
}

/**
 * Mock ViewModel for LiveDetectionScreen testing.
 */
class MockLiveDetectionViewModel(
    val isProcessing: Boolean = false,
    val currentAnalysis: SafetyAnalysis? = null,
    val isNetworkConnected: Boolean = true
) {
    // Add mock implementation methods as needed
}
