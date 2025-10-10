package com.hazardhawk.documents

import com.hazardhawk.TestDataFactory
import com.hazardhawk.TestUtils
import com.hazardhawk.documents.generators.PTPGenerator
import com.hazardhawk.documents.models.*
import com.hazardhawk.documents.services.DocumentAIService
import com.hazardhawk.documents.templates.PTPTemplateEngine
import com.hazardhawk.core.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive unit tests for PTP (Pre-Task Plan) Generator.
 * Tests document generation, quality assessment, and OSHA compliance.
 */
class PTPGeneratorTest {
    
    private lateinit var mockDocumentAIService: MockDocumentAIService
    private lateinit var mockTemplateEngine: MockPTPTemplateEngine
    private lateinit var ptpGenerator: PTPGenerator
    
    @BeforeTest
    fun setup() {
        mockDocumentAIService = MockDocumentAIService()
        mockTemplateEngine = MockPTPTemplateEngine()
        ptpGenerator = PTPGenerator(mockDocumentAIService, mockTemplateEngine)
    }
    
    @Test
    fun `should generate complete PTP from safety analysis`() = runTest {
        // Given
        val request = createSamplePTPRequest()
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess, "PTP generation should succeed")
        val response = result.getOrNull()!!
        
        // Validate document structure
        assertNotNull(response.document, "Document should be generated")
        assertTrue(response.document.id.isNotEmpty(), "Document should have ID")
        assertEquals(request.jobDescription.workType, response.document.jobDescription.workType)
        assertEquals(request.projectInfo.projectName, response.document.projectInfo.projectName)
        
        // Validate hazard analysis section
        assertNotNull(response.document.hazardAnalysis, "Hazard analysis should be present")
        assertTrue(
            response.document.hazardAnalysis.identifiedHazards.isNotEmpty(),
            "Should identify hazards from analysis"
        )
        
        // Validate safety procedures
        assertTrue(
            response.document.safetyProcedures.isNotEmpty(),
            "Should generate safety procedures"
        )
        
        // Validate PPE requirements
        assertTrue(
            response.document.requiredPPE.isNotEmpty(),
            "Should specify PPE requirements"
        )
        
        // Validate emergency information
        assertNotNull(response.document.emergencyInformation, "Emergency info should be present")
        assertTrue(
            response.document.emergencyInformation.emergencyContacts.isNotEmpty(),
            "Should include emergency contacts"
        )
    }
    
    @Test
    fun `should aggregate duplicate hazards correctly`() = runTest {
        // Given
        val hazard1 = TestDataFactory.createFallProtectionHazard()
        val hazard2 = TestDataFactory.createFallProtectionHazard() // Duplicate type
        val hazard3 = TestDataFactory.createPPEViolationHazard() // Different type
        
        val analysis1 = TestDataFactory.createSampleSafetyAnalysis().copy(hazards = listOf(hazard1, hazard3))
        val analysis2 = TestDataFactory.createSampleSafetyAnalysis().copy(hazards = listOf(hazard2))
        
        val request = createSamplePTPRequest(safetyAnalyses = listOf(analysis1, analysis2))
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()!!
        
        // Should deduplicate fall protection hazards but keep different types
        val identifiedHazards = response.document.hazardAnalysis.identifiedHazards
        val fallProtectionHazards = identifiedHazards.filter { 
            it.hazardType.contains("FALL_PROTECTION", ignoreCase = true) 
        }
        val ppeHazards = identifiedHazards.filter { 
            it.hazardType.contains("PPE", ignoreCase = true) 
        }
        
        assertTrue(fallProtectionHazards.size <= 1, "Should deduplicate similar hazards")
        assertTrue(ppeHazards.isNotEmpty(), "Should keep different hazard types")
    }
    
    @Test
    fun `should generate appropriate PPE requirements based on hazards`() = runTest {
        // Given
        val fallHazard = TestDataFactory.createFallProtectionHazard()
        val electricalHazard = TestDataFactory.createElectricalHazard()
        val analysis = TestDataFactory.createSampleSafetyAnalysis().copy(
            hazards = listOf(fallHazard, electricalHazard)
        )
        val request = createSamplePTPRequest(safetyAnalyses = listOf(analysis))
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()!!
        val ppeRequirements = response.document.requiredPPE
        
        // Should include fall protection PPE
        assertTrue(
            ppeRequirements.any { it.ppeType == PPEType.FALL_PROTECTION },
            "Should require fall protection PPE"
        )
        
        // Should include electrical safety PPE
        assertTrue(
            ppeRequirements.any { it.ppeType == PPEType.EYE_PROTECTION },
            "Should require eye protection for electrical work"
        )
        
        // Should include basic construction PPE
        assertTrue(
            ppeRequirements.any { it.ppeType == PPEType.HEAD_PROTECTION },
            "Should require head protection"
        )
        
        // Validate OSHA standards are referenced
        ppeRequirements.forEach { ppe ->
            assertTrue(ppe.oshaStandard.isNotEmpty(), "PPE should reference OSHA standard")
            assertTrue(ppe.specification.isNotEmpty(), "PPE should have specifications")
        }
    }
    
    @Test
    fun `should adjust risk level based on control measures`() = runTest {
        // Given
        val criticalHazard = TestDataFactory.createFallProtectionHazard().copy(
            severity = Severity.CRITICAL
        )
        val analysis = TestDataFactory.createSampleSafetyAnalysis().copy(
            hazards = listOf(criticalHazard),
            overallRiskLevel = RiskLevel.SEVERE
        )
        val request = createSamplePTPRequest(safetyAnalyses = listOf(analysis))
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()!!
        val hazardAnalysis = response.document.hazardAnalysis
        
        // Initial risk should be severe
        assertEquals(RiskLevel.EXTREME, hazardAnalysis.riskAssessment.overallRiskLevel)
        
        // Residual risk should be lower after control measures
        assertTrue(
            hazardAnalysis.residualRisk.ordinal < RiskLevel.EXTREME.ordinal,
            "Residual risk should be reduced after control measures"
        )
        
        // Should have control measures for critical hazard
        assertTrue(
            hazardAnalysis.controlMeasures.any { it.hazardId == criticalHazard.id },
            "Should have control measures for critical hazard"
        )
    }
    
    @Test
    fun `should generate work-type specific procedures`() = runTest {
        // Given - Electrical work type
        val electricalRequest = createSamplePTPRequest().copy(
            jobDescription = JobDescription(
                workType = WorkType.ELECTRICAL,
                taskDescription = "Electrical panel installation",
                estimatedDuration = "4 hours",
                workLocation = "Building electrical room"
            )
        )
        
        // When
        val result = ptpGenerator.generatePTP(electricalRequest)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()!!
        val procedures = response.document.safetyProcedures
        
        // Should include electrical-specific procedures
        assertTrue(
            procedures.any { procedure ->
                procedure.steps.any { step ->
                    step.description.contains("lockout", ignoreCase = true) ||
                    step.description.contains("de-energized", ignoreCase = true)
                }
            },
            "Should include lockout/tagout procedures for electrical work"
        )
        
        // Should include general safety procedures
        assertTrue(
            procedures.any { it.title.contains("General") },
            "Should include general safety procedures"
        )
    }
    
    @Test
    fun `should include stop work conditions for critical hazards`() = runTest {
        // Given
        val criticalHazard = TestDataFactory.createFallProtectionHazard().copy(
            severity = Severity.CRITICAL
        )
        val analysis = TestDataFactory.createSampleSafetyAnalysis().copy(
            hazards = listOf(criticalHazard)
        )
        val request = createSamplePTPRequest(safetyAnalyses = listOf(analysis))
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()!!
        val stopWorkConditions = response.document.hazardAnalysis.riskAssessment.stopWorkConditions
        
        assertTrue(stopWorkConditions.isNotEmpty(), "Should define stop work conditions")
        assertTrue(
            stopWorkConditions.any { it.contains("fall protection", ignoreCase = true) },
            "Should include fall protection stop work condition"
        )
    }
    
    @Test
    fun `should generate quality assessment and recommendations`() = runTest {
        // Given
        val request = createSamplePTPRequest()
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()!!
        
        // Quality assessment
        assertTrue(response.qualityScore >= 0.0f && response.qualityScore <= 1.0f,
            "Quality score should be between 0 and 1")
        
        // Recommendations
        assertTrue(response.recommendations.isNotEmpty(),
            "Should provide recommendations")
        
        // Generation metadata
        val metadata = response.generationMetadata
        assertNotNull(metadata, "Should include generation metadata")
        assertTrue(metadata.processingTimeMs > 0, "Should record processing time")
        assertTrue(metadata.hazardsProcessed > 0, "Should record hazards processed")
        assertTrue(metadata.confidenceScore >= 0.0f, "Should have confidence score")
    }
    
    @Test
    fun `should flag documents requiring review`() = runTest {
        // Given - Request with critical hazards
        val criticalHazard = TestDataFactory.createFallProtectionHazard().copy(
            severity = Severity.CRITICAL
        )
        val analysis = TestDataFactory.createSampleSafetyAnalysis().copy(
            hazards = listOf(criticalHazard),
            confidence = 0.6f // Low confidence
        )
        val request = createSamplePTPRequest(safetyAnalyses = listOf(analysis))
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess)
        val response = result.getOrNull()!!
        
        // Should require review for critical hazards or low confidence
        assertTrue(
            response.generationMetadata.reviewRequired,
            "Should flag document for review when critical hazards present"
        )
    }
    
    @Test
    fun `should handle performance requirements`() = runTest {
        // Given
        val request = createSamplePTPRequest()
        
        // When
        val (result, duration) = TestUtils.measureExecutionTime {
            ptpGenerator.generatePTP(request)
        }
        
        // Then
        assertTrue(result.isSuccess)
        
        // Performance validation - should complete within reasonable time
        TestUtils.assertPerformanceWithin(
            actualMs = duration.inWholeMilliseconds,
            expectedMs = 5000L, // 5 seconds max
            tolerancePercent = 50.0,
            scenario = "PTP Generation"
        )
    }
    
    @Test
    fun `should validate generated document completeness`() = runTest {
        // Given
        val request = createSamplePTPRequest()
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess)
        val document = result.getOrNull()!!.document
        
        // Validate all required sections are present
        val validationErrors = validatePTPDocument(document)
        assertTrue(
            validationErrors.isEmpty(),
            "Document should be complete. Errors: $validationErrors"
        )
    }
    
    @Test
    fun `should handle empty safety analysis gracefully`() = runTest {
        // Given - Request with no hazards
        val emptyAnalysis = TestDataFactory.createSampleSafetyAnalysis().copy(
            hazards = emptyList()
        )
        val request = createSamplePTPRequest(safetyAnalyses = listOf(emptyAnalysis))
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess, "Should handle empty analysis gracefully")
        val response = result.getOrNull()!!
        
        // Should still generate basic document structure
        assertTrue(response.document.safetyProcedures.isNotEmpty(),
            "Should include general procedures even without hazards")
        assertTrue(response.document.requiredPPE.isNotEmpty(),
            "Should include basic PPE requirements")
    }
    
    @Test
    fun `should handle generation failures gracefully`() = runTest {
        // Given
        mockDocumentAIService.shouldFail = true
        val request = createSamplePTPRequest()
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isFailure, "Should fail when AI service fails")
        val error = result.exceptionOrNull()!!
        assertTrue(
            error.message!!.contains("PTP generation failed"),
            "Should provide meaningful error message"
        )
    }
    
    // Helper methods
    private fun createSamplePTPRequest(
        safetyAnalyses: List<SafetyAnalysis> = listOf(TestDataFactory.createSampleSafetyAnalysis())
    ): PTPGenerationRequest {
        return PTPGenerationRequest(
            safetyAnalyses = safetyAnalyses,
            projectInfo = ProjectInfo(
                projectName = "Test Construction Project",
                location = "123 Test St, Test City, TS 12345",
                projectManager = "John Doe",
                safetyManager = "Jane Smith"
            ),
            jobDescription = JobDescription(
                workType = WorkType.GENERAL_CONSTRUCTION,
                taskDescription = "General construction tasks",
                estimatedDuration = "8 hours",
                workLocation = "Main building site"
            )
        )
    }
    
    private fun validatePTPDocument(document: PTPDocument): List<String> {
        val errors = mutableListOf<String>()
        
        if (document.id.isEmpty()) errors.add("Document ID missing")
        if (document.title.isEmpty()) errors.add("Document title missing")
        if (document.projectInfo.projectName.isEmpty()) errors.add("Project name missing")
        if (document.jobDescription.taskDescription.isEmpty()) errors.add("Task description missing")
        if (document.safetyProcedures.isEmpty()) errors.add("Safety procedures missing")
        if (document.requiredPPE.isEmpty()) errors.add("PPE requirements missing")
        
        // Validate emergency information
        val emergencyInfo = document.emergencyInformation
        if (emergencyInfo.emergencyContacts.isEmpty()) errors.add("Emergency contacts missing")
        if (emergencyInfo.nearestHospital.name.isEmpty()) errors.add("Hospital information missing")
        
        return errors
    }
}

/**
 * Mock implementation of DocumentAIService for testing.
 */
class MockDocumentAIService : DocumentAIService {
    var shouldFail = false
    
    override suspend fun generateHazardAnalysis(request: Any): Result<String> {
        return if (shouldFail) {
            Result.failure(Exception("Mock AI service failure"))
        } else {
            Result.success("Mock hazard analysis generated")
        }
    }
    
    // Add other required methods based on DocumentAIService interface
}

/**
 * Mock implementation of PTPTemplateEngine for testing.
 */
class MockPTPTemplateEngine : PTPTemplateEngine {
    override suspend fun renderPTPTemplate(data: Map<String, Any>): String {
        return "Mock PTP template rendered"
    }
    
    // Add other required methods based on PTPTemplateEngine interface
}
