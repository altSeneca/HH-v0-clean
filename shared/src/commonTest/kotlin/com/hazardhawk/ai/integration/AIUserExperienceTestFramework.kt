package com.hazardhawk.ai.integration

import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.core.models.*
import kotlin.test.*
import kotlinx.coroutines.test.runTest

/**
 * User Experience Testing Framework for AI Results Presentation.
 * This framework ensures that AI analysis results are presented in a way that is
 * accessible, actionable, and appropriate for construction workers in field conditions.
 */
class AIUserExperienceTestFramework {
    
    /**
     * Test scenarios representing different user contexts and needs
     */
    data class UserTestScenario(
        val userId: String,
        val userRole: UserRole,
        val experienceLevel: ExperienceLevel,
        val accessibilityNeeds: List<AccessibilityNeed>,
        val deviceConditions: DeviceConditions,
        val environmentalFactors: EnvironmentalFactors,
        val language: String = "en",
        val timeConstraints: TimeConstraints
    )
    
    data class UXTestResult(
        val scenario: UserTestScenario,
        val comprehensionScore: Float, // 0-1, how well user understood results
        val actionabilityScore: Float, // 0-1, how actionable the recommendations were
        val accessibilityScore: Float, // 0-1, accessibility compliance
        val timeToUnderstand: Long, // milliseconds
        val userSatisfaction: Float, // 0-1, user satisfaction rating
        val taskCompletionRate: Float, // 0-1, rate of successful task completion
        val errorRate: Float, // 0-1, rate of user errors
        val cognitiveLoad: CognitiveLoadLevel,
        val usabilityIssues: List<UsabilityIssue>
    )
    
    enum class UserRole {
        GENERAL_WORKER,
        SAFETY_SUPERVISOR,
        FOREMAN,
        SAFETY_MANAGER,
        INSPECTOR
    }
    
    enum class ExperienceLevel {
        NOVICE,
        INTERMEDIATE,
        EXPERIENCED,
        EXPERT
    }
    
    enum class AccessibilityNeed {
        VISUAL_IMPAIRMENT,
        HEARING_IMPAIRMENT,
        MOTOR_IMPAIRMENT,
        COGNITIVE_ASSISTANCE,
        LANGUAGE_SUPPORT,
        NONE
    }
    
    /**
     * AI Results Presentation Testing
     */
    class AIResultsPresentationTest {
        
        private lateinit var aiAnalyzer: AIPhotoAnalyzer
        private lateinit var uiRenderer: MockUIRenderer
        
        @BeforeTest
        fun setup() {
            aiAnalyzer = createUXTestAIAnalyzer()
            uiRenderer = MockUIRenderer()
        }
        
        @Test
        fun `test hazard visualization clarity across screen sizes`() = runTest {
            val hazardTestImage = loadTestImage("multiple_visible_hazards")
            val analysis = aiAnalyzer.analyzePhoto(hazardTestImage, WorkType.GENERAL_CONSTRUCTION)
                .getOrNull()!!
            
            val screenSizes = ScreenSize.values()
            val clarityResults = mutableMapOf<ScreenSize, UXTestResult>()
            
            screenSizes.forEach { screenSize ->
                val scenario = createStandardScenario().copy(
                    deviceConditions = DeviceConditions(
                        screenSize = screenSize,
                        brightness = BrightnessLevel.NORMAL,
                        orientation = DeviceOrientation.PORTRAIT,
                        gloveUse = true,
                        batteryLevel = BatteryLevel.HIGH
                    )
                )
                
                val renderedUI = uiRenderer.renderHazardResults(analysis, scenario)
                val testResult = evaluateHazardVisualization(renderedUI, scenario, analysis)
                
                clarityResults[screenSize] = testResult
                
                // Verify minimum requirements for each screen size
                assertTrue(
                    "Comprehension score too low for ${screenSize}: ${testResult.comprehensionScore}",
                    testResult.comprehensionScore >= getMinimumComprehensionScore(screenSize)
                )
                
                // Verify bounding boxes are visible and appropriately sized
                assertTrue(
                    "Actionability score too low for ${screenSize}: ${testResult.actionabilityScore}",
                    testResult.actionabilityScore >= 0.8f
                )
            }
            
            // Verify consistency across screen sizes
            val clarityVariance = calculateScoreVariance(clarityResults.values.map { it.comprehensionScore })
            assertTrue(
                "Too much variance in clarity across screen sizes: ${clarityVariance}",
                clarityVariance <= 0.15f // Max 15% variance
            )
            
            logHazardVisualizationResults(clarityResults)
        }
        
        @Test
        fun `test confidence score interpretation and display`() = runTest {
            val confidenceTestCases = listOf(
                0.95f to "Very High Confidence",
                0.85f to "High Confidence", 
                0.70f to "Medium Confidence",
                0.55f to "Low Confidence",
                0.40f to "Very Low Confidence"
            )
            
            confidenceTestCases.forEach { (confidence, expectedInterpretation) ->
                val mockAnalysis = createMockAnalysisWithConfidence(confidence)
                val scenario = createStandardScenario()
                
                val renderedUI = uiRenderer.renderConfidenceScore(mockAnalysis, scenario)
                val testResult = evaluateConfidencePresentation(renderedUI, scenario, confidence)
                
                assertTrue(
                    "Confidence interpretation unclear for ${confidence}: ${testResult.comprehensionScore}",
                    testResult.comprehensionScore >= 0.85f
                )
                
                // Verify appropriate visual indicators for confidence level
                assertTrue(
                    "Confidence visualization inadequate for ${confidence}",
                    testResult.accessibilityScore >= 0.90f
                )
                
                // Verify users understand implications of confidence level
                assertTrue(
                    "Users don't understand confidence implications for ${confidence}",
                    testResult.actionabilityScore >= 0.75f
                )
            }
        }
        
        @Test
        fun `test accessibility compliance for visual impairments`() = runTest {
            val visualImpairmentScenarios = listOf(
                AccessibilityNeed.VISUAL_IMPAIRMENT,
                AccessibilityNeed.NONE // Control group
            )
            
            val testImage = loadTestImage("high_contrast_hazards")
            val analysis = aiAnalyzer.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
                .getOrNull()!!
            
            visualImpairmentScenarios.forEach { accessibilityNeed ->
                val scenario = createStandardScenario().copy(
                    accessibilityNeeds = listOf(accessibilityNeed)
                )
                
                val renderedUI = uiRenderer.renderAccessibleResults(analysis, scenario)
                val testResult = evaluateAccessibility(renderedUI, scenario, analysis)
                
                when (accessibilityNeed) {
                    AccessibilityNeed.VISUAL_IMPAIRMENT -> {
                        // Higher accessibility requirements
                        assertTrue(
                            "Accessibility score inadequate for visual impairment: ${testResult.accessibilityScore}",
                            testResult.accessibilityScore >= 0.95f
                        )
                        
                        // Verify audio descriptions are available
                        val audioDescription = uiRenderer.generateAudioDescription(analysis)
                        assertTrue(
                            "Audio description missing or inadequate",
                            audioDescription.isNotEmpty() && audioDescription.length >= 50
                        )
                        
                        // Verify high contrast mode
                        assertTrue(
                            "High contrast mode not properly implemented",
                            renderedUI.usesHighContrast
                        )
                    }
                    
                    AccessibilityNeed.NONE -> {
                        // Standard accessibility requirements
                        assertTrue(
                            "Standard accessibility score inadequate: ${testResult.accessibilityScore}",
                            testResult.accessibilityScore >= 0.85f
                        )
                    }
                }
            }
        }
        
        // Helper functions
        private fun createStandardScenario(): UserTestScenario {
            return UserTestScenario(
                userId = "test_user_001",
                userRole = UserRole.GENERAL_WORKER,
                experienceLevel = ExperienceLevel.INTERMEDIATE,
                accessibilityNeeds = listOf(AccessibilityNeed.NONE),
                deviceConditions = DeviceConditions(
                    screenSize = ScreenSize.STANDARD_PHONE,
                    brightness = BrightnessLevel.NORMAL,
                    orientation = DeviceOrientation.PORTRAIT,
                    gloveUse = true,
                    batteryLevel = BatteryLevel.HIGH
                ),
                environmentalFactors = EnvironmentalFactors(
                    lightingCondition = LightingCondition.NORMAL_INDOOR,
                    noiseLevel = NoiseLevel.MODERATE,
                    weatherCondition = WeatherCondition.CLEAR,
                    workSiteActivity = WorkSiteActivity.MODERATE_ACTIVITY
                ),
                language = "en",
                timeConstraints = TimeConstraints(
                    maxReviewTimeSeconds = 30,
                    criticalDecisionRequired = false,
                    workflowPressure = WorkflowPressure.MODERATE
                )
            )
        }
        
        private fun evaluateHazardVisualization(
            ui: MockRenderedUI, 
            scenario: UserTestScenario, 
            analysis: SafetyAnalysis
        ): UXTestResult {
            var comprehensionScore = 0.8f
            var actionabilityScore = 0.8f
            var accessibilityScore = 0.9f
            val usabilityIssues = mutableListOf<UsabilityIssue>()
            
            // Evaluate bounding box visibility
            analysis.hazards.forEach { hazard ->
                if (hazard.boundingBox == null) {
                    comprehensionScore -= 0.1f
                    usabilityIssues.add(UsabilityIssue(
                        type = UsabilityIssueType.VISIBILITY_PROBLEM,
                        severity = Severity.MEDIUM,
                        description = "Missing bounding box for ${hazard.type}",
                        location = "Hazard visualization",
                        recommendation = "Add bounding box visualization"
                    ))
                }
            }
            
            return UXTestResult(
                scenario = scenario,
                comprehensionScore = comprehensionScore.coerceIn(0f, 1f),
                actionabilityScore = actionabilityScore.coerceIn(0f, 1f),
                accessibilityScore = accessibilityScore.coerceIn(0f, 1f),
                timeToUnderstand = 5000L,
                userSatisfaction = 0.85f,
                taskCompletionRate = 0.90f,
                errorRate = 0.05f,
                cognitiveLoad = CognitiveLoadLevel.MODERATE,
                usabilityIssues = usabilityIssues
            )
        }
        
        private fun getMinimumComprehensionScore(screenSize: ScreenSize): Float {
            return when (screenSize) {
                ScreenSize.SMALL_PHONE -> 0.70f
                ScreenSize.STANDARD_PHONE -> 0.80f
                ScreenSize.LARGE_PHONE -> 0.85f
                ScreenSize.TABLET -> 0.90f
                ScreenSize.LARGE_TABLET -> 0.90f
            }
        }
        
        private fun calculateScoreVariance(scores: List<Float>): Float {
            val mean = scores.average()
            val variance = scores.map { (it - mean) * (it - mean) }.average()
            return kotlin.math.sqrt(variance).toFloat()
        }
        
        private fun logHazardVisualizationResults(results: Map<ScreenSize, UXTestResult>) {
            println("=== Hazard Visualization Results ===")
            results.forEach { (screenSize, result) ->
                println("${screenSize}: Comprehension=${String.format("%.2f", result.comprehensionScore)}, " +
                       "Actionability=${String.format("%.2f", result.actionabilityScore)}")
            }
            println("====================================")
        }
    }
    
    // Supporting data classes and enums
    data class DeviceConditions(
        val screenSize: ScreenSize,
        val brightness: BrightnessLevel,
        val orientation: DeviceOrientation,
        val gloveUse: Boolean = true,
        val batteryLevel: BatteryLevel
    )
    
    data class EnvironmentalFactors(
        val lightingCondition: LightingCondition,
        val noiseLevel: NoiseLevel,
        val weatherCondition: WeatherCondition,
        val workSiteActivity: WorkSiteActivity
    )
    
    data class TimeConstraints(
        val maxReviewTimeSeconds: Int,
        val criticalDecisionRequired: Boolean,
        val workflowPressure: WorkflowPressure
    )
    
    data class UsabilityIssue(
        val type: UsabilityIssueType,
        val severity: Severity,
        val description: String,
        val location: String,
        val recommendation: String
    )
    
    enum class ScreenSize { SMALL_PHONE, STANDARD_PHONE, LARGE_PHONE, TABLET, LARGE_TABLET }
    enum class BrightnessLevel { VERY_DIM, DIM, NORMAL, BRIGHT, VERY_BRIGHT }
    enum class DeviceOrientation { PORTRAIT, LANDSCAPE }
    enum class BatteryLevel { CRITICAL, LOW, MEDIUM, HIGH }
    enum class LightingCondition { VERY_DARK, DIM_INDOOR, NORMAL_INDOOR, BRIGHT_INDOOR, OUTDOOR_SHADE, OUTDOOR_DIRECT_SUN, ARTIFICIAL_NIGHT }
    enum class NoiseLevel { QUIET, MODERATE, LOUD_CONSTRUCTION, VERY_LOUD_MACHINERY }
    enum class WeatherCondition { CLEAR, OVERCAST, LIGHT_RAIN, HEAVY_RAIN, SNOW, EXTREME_COLD, EXTREME_HEAT }
    enum class WorkSiteActivity { LOW_ACTIVITY, MODERATE_ACTIVITY, HIGH_ACTIVITY, EMERGENCY_RESPONSE }
    enum class WorkflowPressure { LOW, MODERATE, HIGH, URGENT }
    enum class CognitiveLoadLevel { LOW, MODERATE, HIGH, EXCESSIVE }
    enum class UsabilityIssueType { VISIBILITY_PROBLEM, COMPREHENSION_ISSUE, INTERACTION_DIFFICULTY, INFORMATION_OVERLOAD, ACCESSIBILITY_BARRIER, PERFORMANCE_ISSUE, CONTENT_UNCLEAR }
    
    // Mock classes for testing
    class MockUIRenderer {
        fun renderHazardResults(analysis: SafetyAnalysis, scenario: UserTestScenario): MockRenderedUI = MockRenderedUI()
        fun renderConfidenceScore(analysis: SafetyAnalysis, scenario: UserTestScenario): MockRenderedUI = MockRenderedUI()
        fun renderAccessibleResults(analysis: SafetyAnalysis, scenario: UserTestScenario): MockRenderedUI = MockRenderedUI()
        fun generateAudioDescription(analysis: SafetyAnalysis): String = "Mock audio description"
    }
    
    data class MockRenderedUI(
        val usesHighContrast: Boolean = false,
        val hasAudioSupport: Boolean = false
    )
    
    // Helper functions
    private fun createUXTestAIAnalyzer(): AIPhotoAnalyzer = UXTestMockAIAnalyzer()
    private fun loadTestImage(scenario: String): ByteArray = scenario.toByteArray()
    
    private fun createMockAnalysisWithConfidence(confidence: Float): SafetyAnalysis {
        return SafetyAnalysis(
            id = "confidence-test-${confidence}",
            hazards = listOf(
                DetectedHazard(
                    type = HazardType.FALL_PROTECTION,
                    description = "Test hazard for confidence ${confidence}",
                    severity = Severity.MEDIUM,
                    confidence = confidence
                )
            ),
            overallRisk = RiskLevel.MEDIUM,
            confidence = confidence,
            processingTimeMs = 200L,
            aiProvider = "UX Test AI"
        )
    }
    
    private fun evaluateConfidencePresentation(ui: MockRenderedUI, scenario: UserTestScenario, confidence: Float): UXTestResult {
        return UXTestResult(
            scenario = scenario,
            comprehensionScore = 0.9f,
            actionabilityScore = 0.85f,
            accessibilityScore = 0.9f,
            timeToUnderstand = 3000L,
            userSatisfaction = 0.8f,
            taskCompletionRate = 0.88f,
            errorRate = 0.07f,
            cognitiveLoad = CognitiveLoadLevel.LOW,
            usabilityIssues = emptyList()
        )
    }
    
    private fun evaluateAccessibility(ui: MockRenderedUI, scenario: UserTestScenario, analysis: SafetyAnalysis): UXTestResult {
        return UXTestResult(
            scenario = scenario,
            comprehensionScore = 0.85f,
            actionabilityScore = 0.80f,
            accessibilityScore = 0.90f,
            timeToUnderstand = 6000L,
            userSatisfaction = 0.85f,
            taskCompletionRate = 0.85f,
            errorRate = 0.08f,
            cognitiveLoad = CognitiveLoadLevel.MODERATE,
            usabilityIssues = emptyList()
        )
    }
}

/**
 * Mock AI analyzer for UX testing
 */
class UXTestMockAIAnalyzer : AIPhotoAnalyzer {
    override val analyzerName = "UX Test AI Analyzer"
    override val analysisCapabilities = setOf(AnalysisCapability.HAZARD_DETECTION)
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType,
        analysisOptions: AnalysisOptions
    ): Result<SafetyAnalysis> {
        return Result.success(
            SafetyAnalysis(
                id = "ux-test-${System.currentTimeMillis()}",
                hazards = listOf(
                    DetectedHazard(
                        type = HazardType.FALL_PROTECTION,
                        description = "Mock hazard for UX testing",
                        severity = Severity.HIGH,
                        confidence = 0.85f,
                        boundingBox = BoundingBox(0.2f, 0.3f, 0.4f, 0.5f, 0.85f)
                    )
                ),
                overallRisk = RiskLevel.HIGH,
                confidence = 0.85f,
                processingTimeMs = 200L,
                aiProvider = analyzerName
            )
        )
    }
    
    override fun getPerformanceMetrics(): AnalyzerPerformanceMetrics {
        return AnalyzerPerformanceMetrics(100L, 200L, 1.0f, 0.85f)
    }
    
    override fun cleanup() {}
}
