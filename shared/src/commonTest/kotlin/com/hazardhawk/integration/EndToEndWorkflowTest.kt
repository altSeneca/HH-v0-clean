package com.hazardhawk.integration

import com.hazardhawk.TestDataFactory
import com.hazardhawk.TestUtils
import com.hazardhawk.MockAIPhotoAnalyzer
import com.hazardhawk.core.models.*
import com.hazardhawk.documents.generators.PTPGenerator
import com.hazardhawk.documents.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * End-to-end integration tests that validate complete workflows
 * from photo capture through document generation.
 */
class EndToEndWorkflowTest {
    
    @Test
    fun `complete safety analysis workflow should work end-to-end`() = runTest {
        // Given - Simulated construction site photo with hazards
        val imageData = TestDataFactory.createMockImageData(1920, 1080)
        val workType = WorkType.FALL_PROTECTION
        
        val mockAnalyzer = MockAIPhotoAnalyzer(
            customAnalysis = TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                hazardCount = 3,
                includeCriticalHazards = true
            )
        )
        
        // When - Complete workflow: Photo -> Analysis -> Validation
        val analysisResult = mockAnalyzer.analyzePhoto(imageData, workType)
        
        // Then - Validate complete analysis pipeline
        assertTrue(analysisResult.isSuccess, "Analysis should succeed")
        val analysis = analysisResult.getOrNull()!!
        
        // Validate analysis completeness
        val validationErrors = TestUtils.validateSafetyAnalysis(analysis)
        assertTrue(
            validationErrors.isEmpty(),
            "Analysis should be valid: ${validationErrors.joinToString()}"
        )
        
        // Validate hazard detection
        assertTrue(analysis.hazards.isNotEmpty(), "Should detect hazards")
        assertTrue(
            analysis.hazards.any { it.severity == Severity.CRITICAL },
            "Should detect critical hazards"
        )
        
        // Validate OSHA compliance
        assertTrue(analysis.oshaViolations.isNotEmpty(), "Should identify OSHA violations")
        analysis.oshaViolations.forEach { violation ->
            assertTrue(violation.code.isNotEmpty(), "OSHA codes should be present")
            assertTrue(violation.correctiveAction.isNotEmpty(), "Corrective actions should be specified")
        }
        
        // Validate metadata
        assertNotNull(analysis.metadata, "Analysis metadata should be present")
        assertTrue(analysis.processingTimeMs > 0, "Processing time should be recorded")
    }
    
    @Test
    fun `photo analysis to PTP generation workflow should work`() = runTest {
        // Given - Safety analysis results
        val safetyAnalyses = listOf(
            TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                hazardCount = 2,
                includeCriticalHazards = true
            ),
            TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                hazardCount = 1,
                includeCriticalHazards = false
            )
        )
        
        val ptpRequest = PTPGenerationRequest(
            safetyAnalyses = safetyAnalyses,
            projectInfo = ProjectInfo(
                projectName = "Integration Test Project",
                location = "Test Construction Site",
                projectManager = "Test Manager",
                safetyManager = "Test Safety Manager"
            ),
            jobDescription = JobDescription(
                workType = WorkType.GENERAL_CONSTRUCTION,
                taskDescription = "Integration testing construction tasks",
                estimatedDuration = "4 hours",
                workLocation = "Test area"
            )
        )
        
        // Mock PTP generator (would use real implementation in actual integration test)
        val mockDocumentAI = MockDocumentAIService()
        val mockTemplateEngine = MockPTPTemplateEngine()
        val ptpGenerator = PTPGenerator(mockDocumentAI, mockTemplateEngine)
        
        // When - Generate PTP from analysis results
        val ptpResult = ptpGenerator.generatePTP(ptpRequest)
        
        // Then - Validate PTP generation
        assertTrue(ptpResult.isSuccess, "PTP generation should succeed")
        val response = ptpResult.getOrNull()!!
        
        // Validate document structure
        val document = response.document
        assertTrue(document.id.isNotEmpty(), "Document should have ID")
        assertTrue(document.title.isNotEmpty(), "Document should have title")
        
        // Validate hazard integration
        assertTrue(
            document.hazardAnalysis.identifiedHazards.isNotEmpty(),
            "Should integrate hazards from analysis"
        )
        
        // Validate that critical hazards are properly handled
        val hasCriticalHazards = safetyAnalyses.any { it.hazards.any { h -> h.severity == Severity.CRITICAL } }
        if (hasCriticalHazards) {
            assertTrue(
                response.generationMetadata.reviewRequired,
                "Critical hazards should trigger review requirement"
            )
            assertTrue(
                document.hazardAnalysis.riskAssessment.stopWorkConditions.isNotEmpty(),
                "Critical hazards should generate stop work conditions"
            )
        }
        
        // Validate PPE requirements are generated
        assertTrue(
            document.requiredPPE.isNotEmpty(),
            "Should generate PPE requirements"
        )
        
        // Validate emergency information
        assertTrue(
            document.emergencyInformation.emergencyContacts.isNotEmpty(),
            "Should include emergency contacts"
        )
    }
    
    @Test
    fun `multi-analyzer fallback workflow should work correctly`() = runTest {
        // Given - Multiple analyzers with different availability
        val primaryAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Primary Gemma",
            shouldSucceed = false, // Simulate failure
            isAvailable = false
        )
        
        val fallbackAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Fallback Vertex AI",
            customAnalysis = TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.CLOUD_GEMINI
            ),
            shouldSucceed = true
        )
        
        val finalFallback = MockAIPhotoAnalyzer(
            analyzerName = "YOLO Fallback",
            customAnalysis = TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
                confidence = 0.6f // Lower confidence for fallback
            ),
            shouldSucceed = true
        )
        
        val imageData = TestDataFactory.createMockImageData()
        
        // When - Simulate orchestrator fallback logic
        var analysisResult: Result<SafetyAnalysis>
        
        // Try primary
        analysisResult = primaryAnalyzer.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
        
        // Fallback to secondary if primary fails
        if (analysisResult.isFailure) {
            analysisResult = fallbackAnalyzer.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
        }
        
        // Final fallback if needed
        if (analysisResult.isFailure) {
            analysisResult = finalFallback.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
        }
        
        // Then - Should succeed with fallback
        assertTrue(analysisResult.isSuccess, "Fallback should provide analysis")
        val analysis = analysisResult.getOrNull()!!
        
        // Validate fallback behavior
        assertEquals(AnalysisType.CLOUD_GEMINI, analysis.analysisType)
        assertTrue(analysis.confidence > 0.5f, "Should have reasonable confidence")
        
        // Should indicate fallback in recommendations
        assertTrue(
            analysis.recommendations.any { it.contains("cloud service", ignoreCase = true) },
            "Should indicate cloud fallback usage"
        )
    }
    
    @Test
    fun `offline to online transition workflow should work`() = runTest {
        // Given - Simulated offline/online transition scenario
        val offlineAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Offline YOLO",
            customAnalysis = TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
                confidence = 0.65f
            )
        )
        
        val onlineAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Online Gemini",
            customAnalysis = TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.CLOUD_GEMINI,
                confidence = 0.92f
            )
        )
        
        val imageData = TestDataFactory.createMockImageData()
        val workType = WorkType.ELECTRICAL
        
        // When - Process image offline first
        val offlineResult = offlineAnalyzer.analyzePhoto(imageData, workType)
        assertTrue(offlineResult.isSuccess, "Offline analysis should succeed")
        val offlineAnalysis = offlineResult.getOrNull()!!
        
        // Then process same image online (when network available)
        val onlineResult = onlineAnalyzer.analyzePhoto(imageData, workType)
        assertTrue(onlineResult.isSuccess, "Online analysis should succeed")
        val onlineAnalysis = onlineResult.getOrNull()!!
        
        // Then - Validate transition behavior
        assertTrue(
            onlineAnalysis.confidence > offlineAnalysis.confidence,
            "Online analysis should have higher confidence"
        )
        
        assertEquals(workType, offlineAnalysis.workType)
        assertEquals(workType, onlineAnalysis.workType)
        
        // Online analysis should provide more comprehensive results
        assertTrue(
            onlineAnalysis.recommendations.size >= offlineAnalysis.recommendations.size,
            "Online analysis should provide comprehensive recommendations"
        )
    }
    
    @Test
    fun `batch processing workflow should handle mixed results`() = runTest {
        // Given - Mixed batch with successful and failed analyses
        val successAnalyzer = MockAIPhotoAnalyzer(shouldSucceed = true)
        val failingAnalyzer = MockAIPhotoAnalyzer(shouldSucceed = false)
        
        val imagesBatch = listOf(
            TestDataFactory.createMockImageData() to WorkType.GENERAL_CONSTRUCTION,
            TestDataFactory.createMockImageData() to WorkType.ELECTRICAL,
            TestDataFactory.createMockImageData() to WorkType.FALL_PROTECTION,
            TestDataFactory.createMockImageData() to WorkType.EXCAVATION
        )
        
        // When - Process batch with mixed success/failure
        val results = mutableListOf<Result<SafetyAnalysis>>()
        
        imagesBatch.forEachIndexed { index, (imageData, workType) ->
            val analyzer = if (index % 2 == 0) successAnalyzer else failingAnalyzer
            val result = analyzer.analyzePhoto(imageData, workType)
            results.add(result)
        }
        
        // Then - Should handle mixed results gracefully
        val successfulResults = results.filter { it.isSuccess }
        val failedResults = results.filter { it.isFailure }
        
        assertEquals(2, successfulResults.size, "Should have successful analyses")
        assertEquals(2, failedResults.size, "Should have failed analyses")
        
        // Successful analyses should be valid
        successfulResults.forEach { result ->
            val analysis = result.getOrNull()!!
            val validationErrors = TestUtils.validateSafetyAnalysis(analysis)
            assertTrue(
                validationErrors.isEmpty(),
                "Successful analyses should be valid"
            )
        }
        
        // Failed analyses should provide meaningful error messages
        failedResults.forEach { result ->
            val error = result.exceptionOrNull()!!
            assertTrue(
                error.message?.isNotEmpty() == true,
                "Failed analyses should provide error details"
            )
        }
    }
    
    @Test
    fun `performance monitoring workflow should track metrics`() = runTest {
        // Given - Analyzers with different performance characteristics
        val fastAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Fast Analyzer",
            responseDelay = 500L
        )
        
        val slowAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "Slow Analyzer", 
            responseDelay = 3000L
        )
        
        val imageData = TestDataFactory.createMockImageData()
        
        // When - Measure performance across multiple analyses
        val performanceResults = mutableListOf<Long>()
        
        listOf(fastAnalyzer, slowAnalyzer, fastAnalyzer).forEach { analyzer ->
            val (result, duration) = TestUtils.measureExecutionTime {
                analyzer.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
            }
            
            assertTrue(result.isSuccess, "Analysis should succeed for ${analyzer.analyzerName}")
            performanceResults.add(duration.inWholeMilliseconds)
        }
        
        // Then - Performance should be tracked and reasonable
        assertTrue(performanceResults[0] < 1000L, "Fast analyzer should be quick")
        assertTrue(performanceResults[1] > 2500L, "Slow analyzer should be measurably slower")
        assertTrue(performanceResults[2] < 1000L, "Fast analyzer should consistently perform")
        
        // Calculate performance statistics
        val averageTime = performanceResults.average()
        val maxTime = performanceResults.maxOrNull() ?: 0L
        val minTime = performanceResults.minOrNull() ?: 0L
        
        assertTrue(maxTime / minTime < 10, "Performance variance should be reasonable")
    }
    
    // Mock implementations for integration testing
    private class MockDocumentAIService {
        suspend fun generateContent(prompt: String): String {
            return "Mock AI generated content for: ${prompt.take(50)}..."
        }
    }
    
    private class MockPTPTemplateEngine {
        suspend fun renderTemplate(templateName: String, data: Map<String, Any>): String {
            return "Mock rendered template: $templateName with ${data.size} data points"
        }
    }
}
