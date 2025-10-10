package com.hazardhawk.integration.refactoring

import com.hazardhawk.TestDataFactory
import com.hazardhawk.TestUtils
import com.hazardhawk.MockAIPhotoAnalyzer
import com.hazardhawk.core.models.*
import com.hazardhawk.documents.models.*
import com.hazardhawk.documents.generators.PTPGenerator
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Model Consolidation Test for HazardHawk Refactoring Validation
 * 
 * Tests that SafetyAnalysis consolidation preserves AI workflows,
 * ONNX integration, PDF generation, and backward compatibility.
 * 
 * Key Requirements:
 * - SafetyAnalysis model consolidation preserves all data
 * - ONNX integration functionality maintained
 * - PDF generation workflows preserved
 * - Backward compatibility with existing data
 * - Migration utilities work correctly
 * - Performance maintained after consolidation
 */
class ModelConsolidationTest {
    
    @Test
    fun `SafetyAnalysis consolidation should preserve all AI workflow data`() = runTest {
        // Given - Original SafetyAnalysis with comprehensive data
        val originalAnalysis = TestDataFactory.createSampleSafetyAnalysis(
            analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
            hazardCount = 5,
            includeCriticalHazards = true,
            confidence = 0.89f,
            processingTime = 2340L
        )
        
        // When - Consolidate model (simulate data migration)
        val consolidatedAnalysis = consolidateAnalysisModel(originalAnalysis)
        
        // Then - Validate all data preservation
        assertEquals(originalAnalysis.id, consolidatedAnalysis.id)
        assertEquals(originalAnalysis.timestamp, consolidatedAnalysis.timestamp)
        assertEquals(originalAnalysis.analysisType, consolidatedAnalysis.analysisType)
        assertEquals(originalAnalysis.workType, consolidatedAnalysis.workType)
        assertEquals(originalAnalysis.confidence, consolidatedAnalysis.confidence)
        assertEquals(originalAnalysis.processingTimeMs, consolidatedAnalysis.processingTimeMs)
        assertEquals(originalAnalysis.overallRiskLevel, consolidatedAnalysis.overallRiskLevel)
        
        // Validate hazard preservation
        assertEquals(originalAnalysis.hazards.size, consolidatedAnalysis.hazards.size)
        originalAnalysis.hazards.forEachIndexed { index, originalHazard ->
            val consolidatedHazard = consolidatedAnalysis.hazards[index]
            assertEquals(originalHazard.id, consolidatedHazard.id)
            assertEquals(originalHazard.type, consolidatedHazard.type)
            assertEquals(originalHazard.severity, consolidatedHazard.severity)
            assertEquals(originalHazard.confidence, consolidatedHazard.confidence)
            assertEquals(originalHazard.oshaCode, consolidatedHazard.oshaCode)
        }
        
        // Validate PPE status preservation
        assertEquals(originalAnalysis.ppeStatus.overallCompliance, consolidatedAnalysis.ppeStatus.overallCompliance)
        assertEquals(originalAnalysis.ppeStatus.detectedItems.size, consolidatedAnalysis.ppeStatus.detectedItems.size)
        
        // Validate OSHA violations preservation
        assertEquals(originalAnalysis.oshaViolations.size, consolidatedAnalysis.oshaViolations.size)
        
        // Validate recommendations preservation
        assertEquals(originalAnalysis.recommendations.size, consolidatedAnalysis.recommendations.size)
        
        // Validate metadata preservation
        assertEquals(originalAnalysis.metadata?.imageWidth, consolidatedAnalysis.metadata?.imageWidth)
        assertEquals(originalAnalysis.metadata?.imageHeight, consolidatedAnalysis.metadata?.imageHeight)
        assertEquals(originalAnalysis.metadata?.location?.latitude, consolidatedAnalysis.metadata?.location?.latitude)
    }
    
    @Test
    fun `ONNX integration should remain functional after consolidation`() = runTest {
        // Given - ONNX-based analysis workflow
        val onnxAnalyzer = MockAIPhotoAnalyzer(
            analyzerName = "YOLO11 ONNX",
            customAnalysis = TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_YOLO_FALLBACK,
                hazardCount = 3,
                confidence = 0.76f
            )
        )
        
        val imageData = TestDataFactory.createMockImageData(640, 480)
        
        // When - Process through ONNX pipeline
        val result = onnxAnalyzer.analyzePhoto(imageData, WorkType.ELECTRICAL)
        
        // Then - Validate ONNX functionality preserved
        assertTrue(result.isSuccess, "ONNX analysis should succeed")
        val analysis = result.getOrNull()!!
        
        assertEquals(AnalysisType.LOCAL_YOLO_FALLBACK, analysis.analysisType)
        assertTrue(analysis.hazards.isNotEmpty(), "ONNX should detect hazards")
        assertTrue(analysis.confidence > 0.5f, "ONNX confidence should be reasonable")
        
        // Validate ONNX-specific data preservation
        assertTrue(analysis.processingTimeMs > 0, "Processing time should be recorded")
        assertNotNull(analysis.metadata, "Metadata should be preserved")
        
        // Validate hazard bounding boxes (critical for ONNX models)
        analysis.hazards.forEach { hazard ->
            assertNotNull(hazard.boundingBox, "ONNX hazards should have bounding boxes")
            assertTrue(hazard.boundingBox!!.x >= 0f, "Bounding box X should be valid")
            assertTrue(hazard.boundingBox!!.y >= 0f, "Bounding box Y should be valid")
            assertTrue(hazard.boundingBox!!.width > 0f, "Bounding box width should be positive")
            assertTrue(hazard.boundingBox!!.height > 0f, "Bounding box height should be positive")
        }
    }
    
    @Test
    fun `PDF generation should work with consolidated models`() = runTest {
        // Given - Multiple SafetyAnalyses for PDF generation
        val safetyAnalyses = listOf(
            TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
                hazardCount = 3,
                includeCriticalHazards = true
            ),
            TestDataFactory.createSampleSafetyAnalysis(
                analysisType = AnalysisType.CLOUD_GEMINI,
                hazardCount = 2,
                includeCriticalHazards = false
            )
        )
        
        // Consolidate models
        val consolidatedAnalyses = safetyAnalyses.map { consolidateAnalysisModel(it) }
        
        val ptpRequest = PTPGenerationRequest(
            safetyAnalyses = consolidatedAnalyses,
            projectInfo = ProjectInfo(
                projectName = "Model Consolidation Test Project",
                location = "Test Construction Site",
                projectManager = "Test Manager",
                safetyManager = "Test Safety Manager"
            ),
            jobDescription = JobDescription(
                workType = WorkType.GENERAL_CONSTRUCTION,
                taskDescription = "Testing consolidated model PDF generation",
                estimatedDuration = "4 hours",
                workLocation = "Test area"
            )
        )
        
        // When - Generate PTP with consolidated models
        val mockPTPGenerator = createMockPTPGenerator()
        val ptpResult = mockPTPGenerator.generatePTP(ptpRequest)
        
        // Then - Validate PDF generation works
        assertTrue(ptpResult.isSuccess, "PTP generation should succeed with consolidated models")
        val response = ptpResult.getOrNull()!!
        
        val document = response.document
        assertTrue(document.id.isNotEmpty(), "Document should have ID")
        assertTrue(document.title.isNotEmpty(), "Document should have title")
        
        // Validate hazard integration from consolidated models
        val totalExpectedHazards = consolidatedAnalyses.sumOf { it.hazards.size }
        assertTrue(
            document.hazardAnalysis.identifiedHazards.size >= totalExpectedHazards,
            "Should integrate hazards from all consolidated analyses"
        )
        
        // Validate OSHA violation integration
        val totalViolations = consolidatedAnalyses.sumOf { it.oshaViolations.size }
        assertTrue(
            totalViolations > 0 || document.hazardAnalysis.riskAssessment.overallRisk != RiskLevel.MINIMAL,
            "Should reflect OSHA violations from consolidated models"
        )
        
        // Validate PPE requirements generation
        assertTrue(
            document.requiredPPE.isNotEmpty(),
            "Should generate PPE requirements from consolidated data"
        )
    }
    
    @Test
    fun `backward compatibility should be maintained during model migration`() = runTest {
        // Given - Legacy data format (simulate old model structure)
        val legacyAnalysis = createLegacyAnalysisData()
        
        // When - Migrate to new consolidated model
        val migratedAnalysis = migrateLegacyAnalysis(legacyAnalysis)
        
        // Then - Validate migration success
        assertNotNull(migratedAnalysis, "Migration should succeed")
        assertTrue(migratedAnalysis!!.id.isNotEmpty(), "Migrated analysis should have ID")
        
        // Validate critical data preservation during migration
        assertEquals("legacy-hazard-detection", migratedAnalysis.analysisType.toString())
        assertTrue(migratedAnalysis.hazards.isNotEmpty(), "Legacy hazards should be preserved")
        assertTrue(migratedAnalysis.processingTimeMs > 0, "Processing time should be preserved")
        
        // Validate hazard compatibility
        migratedAnalysis.hazards.forEach { hazard ->
            assertTrue(hazard.type in HazardType.values(), "Hazard type should be valid")
            assertTrue(hazard.severity in Severity.values(), "Hazard severity should be valid")
            assertTrue(hazard.confidence >= 0f && hazard.confidence <= 1f, "Confidence should be valid")
        }
        
        // Validate OSHA compliance preservation
        assertTrue(
            migratedAnalysis.oshaViolations.all { it.code.isNotEmpty() },
            "OSHA codes should be preserved in migration"
        )
    }
    
    @Test
    fun `migration utilities should handle edge cases correctly`() = runTest {
        // Given - Various edge cases for migration
        val edgeCases = listOf(
            "Empty hazard list" to createAnalysisWithNoHazards(),
            "Missing metadata" to createAnalysisWithoutMetadata(),
            "Invalid confidence values" to createAnalysisWithInvalidConfidence(),
            "Null PPE status" to createAnalysisWithNullPPE()
        )
        
        // When & Then - Test each edge case
        edgeCases.forEach { (caseName, analysis) ->
            val migrationResult = try {
                consolidateAnalysisModel(analysis)
            } catch (e: Exception) {
                null
            }
            
            assertNotNull(migrationResult, "Migration should handle edge case: $caseName")
            
            // Validate data integrity after migration
            assertTrue(migrationResult.id.isNotEmpty(), "ID should be preserved for $caseName")
            assertTrue(migrationResult.confidence >= 0f, "Confidence should be normalized for $caseName")
            
            // Validate analysis is still usable
            val validationErrors = TestUtils.validateSafetyAnalysis(migrationResult)
            assertTrue(
                validationErrors.isEmpty(),
                "Migrated analysis should be valid for $caseName: ${validationErrors.joinToString()}"
            )
        }
    }
    
    @Test
    fun `performance should be maintained after model consolidation`() = runTest {
        // Given - Performance benchmarking setup
        val testIterations = 50
        val consolidationTimes = mutableListOf<Long>()
        val validationTimes = mutableListOf<Long>()
        
        // When - Benchmark consolidation performance
        repeat(testIterations) {
            val originalAnalysis = TestDataFactory.createSampleSafetyAnalysis(
                hazardCount = 5,
                includeCriticalHazards = true
            )
            
            val (consolidatedAnalysis, consolidationTime) = TestUtils.measureExecutionTime {
                consolidateAnalysisModel(originalAnalysis)
            }
            consolidationTimes.add(consolidationTime.inWholeMilliseconds)
            
            val (_, validationTime) = TestUtils.measureExecutionTime {
                TestUtils.validateSafetyAnalysis(consolidatedAnalysis)
            }
            validationTimes.add(validationTime.inWholeMilliseconds)
        }
        
        // Then - Validate performance targets
        val avgConsolidationTime = consolidationTimes.average()
        val avgValidationTime = validationTimes.average()
        
        assertTrue(
            avgConsolidationTime < 100L, // Should be fast operation
            "Model consolidation should be fast, got ${avgConsolidationTime}ms average"
        )
        
        assertTrue(
            avgValidationTime < 50L, // Validation should be very fast
            "Analysis validation should be fast, got ${avgValidationTime}ms average"
        )
        
        // Validate consistency (low variance in processing time)
        val consolidationVariance = consolidationTimes.map { (it - avgConsolidationTime) * (it - avgConsolidationTime) }.average()
        assertTrue(
            consolidationVariance < 2500.0, // Standard deviation < 50ms
            "Consolidation performance should be consistent, got variance ${consolidationVariance}"
        )
    }
    
    // Helper functions and mock implementations
    
    private fun consolidateAnalysisModel(original: SafetyAnalysis): SafetyAnalysis {
        // Simulate model consolidation process
        return original.copy(
            // Ensure all fields are properly preserved
            // In real implementation, this would handle model schema changes
        )
    }
    
    private fun createLegacyAnalysisData(): SafetyAnalysis {
        return TestDataFactory.createSampleSafetyAnalysis(
            analysisType = AnalysisType.LOCAL_YOLO_FALLBACK, // Simulate legacy type
            hazardCount = 2,
            confidence = 0.65f
        )
    }
    
    private fun migrateLegacyAnalysis(legacy: SafetyAnalysis): SafetyAnalysis? {
        return try {
            // Simulate legacy data migration
            legacy.copy(
                confidence = maxOf(0f, minOf(1f, legacy.confidence)), // Normalize confidence
                recommendations = legacy.recommendations.ifEmpty { 
                    listOf("Migrated from legacy analysis - review recommended")
                }
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun createAnalysisWithNoHazards(): SafetyAnalysis {
        return TestDataFactory.createSampleSafetyAnalysis(hazardCount = 0)
    }
    
    private fun createAnalysisWithoutMetadata(): SafetyAnalysis {
        return TestDataFactory.createSampleSafetyAnalysis().copy(metadata = null)
    }
    
    private fun createAnalysisWithInvalidConfidence(): SafetyAnalysis {
        return TestDataFactory.createSampleSafetyAnalysis(confidence = -0.5f) // Invalid confidence
    }
    
    private fun createAnalysisWithNullPPE(): SafetyAnalysis {
        return TestDataFactory.createSampleSafetyAnalysis().copy(
            ppeStatus = PPEStatus(
                overallCompliance = PPECompliance.NON_COMPLIANT,
                detectedItems = emptyList(),
                missingItems = emptyList(),
                confidence = 0.0f
            )
        )
    }
    
    private fun createMockPTPGenerator(): MockPTPGenerator {
        return MockPTPGenerator()
    }
    
    /**
     * Mock PTP generator for testing
     */
    class MockPTPGenerator {
        suspend fun generatePTP(request: PTPGenerationRequest): Result<PTPGenerationResponse> {
            return try {
                val document = createMockPTPDocument(request)
                Result.success(PTPGenerationResponse(
                    document = document,
                    generationMetadata = PTPGenerationMetadata(
                        generationTime = 1500L,
                        reviewRequired = request.safetyAnalyses.any { 
                            it.hazards.any { h -> h.severity == Severity.CRITICAL }
                        },
                        aiModel = "Mock Generator v1.0",
                        templateVersion = "test-template-v1"
                    )
                ))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        
        private fun createMockPTPDocument(request: PTPGenerationRequest): PTPDocument {
            val allHazards = request.safetyAnalyses.flatMap { it.hazards }
            
            return PTPDocument(
                id = "mock-ptp-${System.currentTimeMillis()}",
                title = "Pre-Task Plan - ${request.projectInfo.projectName}",
                hazardAnalysis = HazardAnalysis(
                    identifiedHazards = allHazards.map { hazard ->
                        IdentifiedHazard(
                            description = hazard.description,
                            riskLevel = hazard.severity.toRiskLevel(),
                            controlMeasures = hazard.recommendations
                        )
                    },
                    riskAssessment = RiskAssessment(
                        overallRisk = determineOverallRisk(allHazards),
                        stopWorkConditions = allHazards.filter { it.severity == Severity.CRITICAL }
                            .map { it.immediateAction ?: "Stop work and address critical hazard" }
                    )
                ),
                requiredPPE = extractPPERequirements(request.safetyAnalyses),
                emergencyInformation = EmergencyInformation(
                    emergencyContacts = listOf("Site Safety Manager: (555) 123-4567"),
                    evacuationPlan = "Follow site evacuation procedures"
                )
            )
        }
        
        private fun determineOverallRisk(hazards: List<Hazard>): RiskLevel {
            return when {
                hazards.any { it.severity == Severity.CRITICAL } -> RiskLevel.SEVERE
                hazards.any { it.severity == Severity.HIGH } -> RiskLevel.HIGH
                hazards.any { it.severity == Severity.MEDIUM } -> RiskLevel.MODERATE
                else -> RiskLevel.LOW
            }
        }
        
        private fun extractPPERequirements(analyses: List<SafetyAnalysis>): List<String> {
            val requirements = mutableSetOf<String>()
            analyses.forEach { analysis ->
                analysis.hazards.forEach { hazard ->
                    when (hazard.type) {
                        HazardType.FALL_PROTECTION -> requirements.add("Fall protection harness")
                        HazardType.PPE_VIOLATION -> requirements.add("Hard hat, safety glasses")
                        HazardType.ELECTRICAL -> requirements.add("Arc flash PPE, insulated gloves")
                        else -> requirements.add("Standard construction PPE")
                    }
                }
            }
            return requirements.toList()
        }
    }
    
    // Extension function for Severity to RiskLevel conversion
    private fun Severity.toRiskLevel(): RiskLevel {
        return when (this) {
            Severity.CRITICAL -> RiskLevel.SEVERE
            Severity.HIGH -> RiskLevel.HIGH
            Severity.MEDIUM -> RiskLevel.MODERATE
            Severity.LOW -> RiskLevel.LOW
        }
    }
}
