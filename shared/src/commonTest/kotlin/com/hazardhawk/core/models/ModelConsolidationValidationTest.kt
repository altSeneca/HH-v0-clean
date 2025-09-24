package com.hazardhawk.core.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Comprehensive validation test for Phase 2 Model Consolidation.
 * Ensures unified models maintain all functionality and backward compatibility.
 */
class ModelConsolidationValidationTest {
    
    @Test
    fun `SafetyAnalysis unified model contains all required fields`() {
        val analysis = SafetyAnalysis(
            id = "test-analysis-1",
            photoId = "photo-123",
            timestamp = Clock.System.now(),
            analysisType = AnalysisType.COMBINED,
            workType = WorkType.GENERAL_CONSTRUCTION,
            overallRiskLevel = RiskLevel.HIGH,
            severity = Severity.HIGH,
            aiConfidence = 0.85f,
            processingTimeMs = 1500L
        )
        
        // Verify core properties
        assertEquals("test-analysis-1", analysis.id)
        assertEquals("photo-123", analysis.photoId)
        assertEquals(AnalysisType.COMBINED, analysis.analysisType)
        assertEquals(WorkType.GENERAL_CONSTRUCTION, analysis.workType)
        assertEquals(RiskLevel.HIGH, analysis.overallRiskLevel)
        assertEquals(Severity.HIGH, analysis.severity)
        assertEquals(0.85f, analysis.aiConfidence)
        assertEquals(1500L, analysis.processingTimeMs)
        
        // Test backward compatibility properties
        assertEquals(analysis.aiConfidence, analysis.confidence)
        assertEquals(analysis.timestamp, analysis.analyzedAt)
        assertEquals(analysis.overallRiskLevel, analysis.riskLevel)
        assertEquals(analysis.processingTimeMs, analysis.processingTime)
        
        // Test OSHA codes mapping
        assertNotNull(analysis.oshaCodes)
        assertTrue(analysis.oshaCodes.isEmpty()) // Empty by default
    }
    
    @Test
    fun `SafetyAnalysis with OSHA violations maps correctly to oshaCodes`() {
        val oshaViolations = listOf(
            OSHAViolation(
                code = "29 CFR 1926.95",
                title = "Personal Protective Equipment",
                description = "Hard hat required",
                severity = Severity.CRITICAL,
                correctiveAction = "Ensure all workers wear approved hard hats"
            ),
            OSHAViolation(
                code = "29 CFR 1926.501",
                title = "Fall Protection",
                description = "Fall protection required at 6 feet",
                severity = Severity.HIGH,
                correctiveAction = "Install guardrails or provide harnesses"
            )
        )
        
        val analysis = SafetyAnalysis(
            id = "test-analysis-2",
            photoId = "photo-456",
            timestamp = Clock.System.now(),
            analysisType = AnalysisType.CLOUD_GEMINI,
            workType = WorkType.ROOFING,
            overallRiskLevel = RiskLevel.SEVERE,
            severity = Severity.CRITICAL,
            aiConfidence = 0.92f,
            processingTimeMs = 2000L,
            oshaViolations = oshaViolations
        )
        
        // Test OSHA codes backward compatibility
        val oshaCodes = analysis.oshaCodes
        assertEquals(2, oshaCodes.size)
        
        val firstCode = oshaCodes[0]
        assertEquals("29 CFR 1926.95", firstCode.code)
        assertEquals("Personal Protective Equipment", firstCode.title)
        assertEquals("Hard hat required", firstCode.description)
        assertEquals(1.0f, firstCode.applicability)
        
        val secondCode = oshaCodes[1]
        assertEquals("29 CFR 1926.501", secondCode.code)
        assertEquals("Fall Protection", secondCode.title)
        assertEquals("Fall protection required at 6 feet", secondCode.description)
    }
    
    @Test
    fun `Tag unified model contains all required fields and functions`() {
        val tag = Tag(
            id = "tag-1",
            name = "Hard Hat Required",
            category = TagCategory.PPE,
            description = "Ensures head protection in construction zones",
            oshaReferences = listOf("29 CFR 1926.95", "29 CFR 1926.96"),
            complianceStatus = ComplianceStatus.CRITICAL,
            severity = Severity.HIGH,
            workTypes = listOf(WorkType.GENERAL_CONSTRUCTION, WorkType.ROOFING)
        )
        
        // Verify core properties
        assertEquals("tag-1", tag.id)
        assertEquals("Hard Hat Required", tag.name)
        assertEquals(TagCategory.PPE, tag.category)
        assertEquals("Ensures head protection in construction zones", tag.description)
        assertEquals(ComplianceStatus.CRITICAL, tag.complianceStatus)
        assertEquals(Severity.HIGH, tag.severity)
        
        // Test OSHA references
        assertEquals(2, tag.oshaReferences.size)
        assertTrue(tag.oshaReferences.contains("29 CFR 1926.95"))
        assertTrue(tag.oshaReferences.contains("29 CFR 1926.96"))
        
        // Test backward compatibility properties
        assertEquals("29 CFR 1926.95", tag.oshaCode) // First OSHA reference
        assertEquals(WorkType.GENERAL_CONSTRUCTION, tag.workType) // First work type
        assertEquals(false, tag.projectSpecific) // No projectId
        
        // Test computed properties
        assertTrue(tag.hasComplianceImplications) // Has OSHA references
        assertNotNull(tag.displayPriorityScore)
        
        // Test validation
        val validationErrors = tag.validate()
        assertTrue(validationErrors.isEmpty()) // Should be valid
    }
    
    @Test
    fun `Tag usage statistics work correctly`() {
        val initialStats = TagUsageStats(
            totalUsageCount = 5,
            recentUsageCount = 2
        )
        
        val tag = Tag(
            id = "tag-2",
            name = "Safety Vest Required",
            category = TagCategory.PPE,
            usageStats = initialStats
        )
        
        // Test usage calculations
        assertTrue(tag.isFrequentlyUsed) // 5 >= 5 OR 2 >= 2
        assertEquals(5, tag.usageStats.totalUsageCount)
        assertEquals(2, tag.usageStats.recentUsageCount)
        
        // Test frequency score calculation
        assertTrue(tag.usageStats.frequencyScore > 0.0)
        
        // Test usage update
        val updatedTag = tag.withUpdatedUsage(
            incrementUsage = true,
            projectId = "project-123",
            confidenceScore = 0.88
        )
        
        // Verify incremented usage
        assertEquals(6, updatedTag.usageStats.totalUsageCount)
        assertEquals(3, updatedTag.usageStats.recentUsageCount)
        assertTrue(updatedTag.usageStats.projectUsageMap.contains("project-123"))
        assertEquals(1, updatedTag.usageStats.projectUsageMap["project-123"])
    }
    
    @Test
    fun `TagCategory enum provides correct OSHA mappings`() {
        // Test PPE category
        val ppeCategory = TagCategory.PPE
        assertEquals("Personal Protective Equipment", ppeCategory.displayName)
        assertEquals("29 CFR 1926.95-106", ppeCategory.primaryOshaSection)
        assertEquals(1, ppeCategory.displayPriority)
        
        // Test Fall Protection category
        val fallCategory = TagCategory.FALL_PROTECTION
        assertEquals("Fall Protection", fallCategory.displayName)
        assertEquals("29 CFR 1926.500-503", fallCategory.primaryOshaSection)
        assertEquals(2, fallCategory.displayPriority)
        
        // Test Electrical Safety category
        val electricalCategory = TagCategory.ELECTRICAL_SAFETY
        assertEquals("Electrical Safety", electricalCategory.displayName)
        assertEquals("29 CFR 1926.400-449", electricalCategory.primaryOshaSection)
        assertEquals(3, electricalCategory.displayPriority)
    }
    
    @Test
    fun `ComplianceStatus enum provides correct properties`() {
        val compliant = ComplianceStatus.COMPLIANT
        assertEquals("Compliant", compliant.displayName)
        assertEquals("#4CAF50", compliant.statusColor)
        
        val needsImprovement = ComplianceStatus.NEEDS_IMPROVEMENT
        assertEquals("Needs Improvement", needsImprovement.displayName)
        assertEquals("#FF9800", needsImprovement.statusColor)
        
        val critical = ComplianceStatus.CRITICAL
        assertEquals("Critical", critical.displayName)
        assertEquals("#F44336", critical.statusColor)
    }
    
    @Test
    fun `Hazard model provides backward compatibility`() {
        val hazard = Hazard(
            id = "hazard-1",
            type = HazardType.PPE_VIOLATION,
            severity = Severity.HIGH,
            description = "Worker not wearing hard hat",
            confidence = 0.89f,
            oshaCode = "29 CFR 1926.95"
        )
        
        // Test core properties
        assertEquals("hazard-1", hazard.id)
        assertEquals(HazardType.PPE_VIOLATION, hazard.type)
        assertEquals(Severity.HIGH, hazard.severity)
        assertEquals("Worker not wearing hard hat", hazard.description)
        assertEquals(0.89f, hazard.confidence)
        assertEquals("29 CFR 1926.95", hazard.oshaCode)
        
        // Test backward compatibility
        assertEquals(hazard.oshaCode, hazard.oshaReference)
    }
    
    @Test
    fun `BoundingBox provides alternative accessors`() {
        val box = BoundingBox(
            left = 10.0f,
            top = 20.0f,
            width = 100.0f,
            height = 50.0f
        )
        
        // Test core properties
        assertEquals(10.0f, box.left)
        assertEquals(20.0f, box.top)
        assertEquals(100.0f, box.width)
        assertEquals(50.0f, box.height)
        
        // Test alternative accessors
        assertEquals(10.0f, box.x)
        assertEquals(20.0f, box.y)
        assertEquals(110.0f, box.right) // left + width
        assertEquals(70.0f, box.bottom) // top + height
    }
    
    @Test
    fun `Migration validation detects success`() {
        val validationResult = ModelMigrationUtils.validateMigration()
        
        // Should succeed with unified models present
        assertTrue(validationResult.isSuccess)
        assertTrue(validationResult.checks.isNotEmpty())
        
        // Check for key validation messages
        val checksString = validationResult.checks.joinToString("\n")
        assertTrue(checksString.contains("SafetyAnalysis backward compatibility: PASSED"))
        assertTrue(checksString.contains("Tag backward compatibility: PASSED"))
    }
    
    @Test
    fun `Enum consistency validation`() {
        // WorkType enum should have adequate coverage
        val workTypes = WorkType.values()
        assertTrue(workTypes.size >= 12, "WorkType should have at least 12 values")
        assertTrue(workTypes.contains(WorkType.GENERAL_CONSTRUCTION))
        assertTrue(workTypes.contains(WorkType.ELECTRICAL))
        assertTrue(workTypes.contains(WorkType.ROOFING))
        assertTrue(workTypes.contains(WorkType.FALL_PROTECTION))
        
        // HazardType enum should have adequate coverage
        val hazardTypes = HazardType.values()
        assertTrue(hazardTypes.size >= 15, "HazardType should have at least 15 values")
        assertTrue(hazardTypes.contains(HazardType.PPE_VIOLATION))
        assertTrue(hazardTypes.contains(HazardType.FALL_PROTECTION))
        assertTrue(hazardTypes.contains(HazardType.ELECTRICAL_HAZARD))
        
        // Severity enum should have exactly 4 values
        val severities = Severity.values()
        assertEquals(4, severities.size)
        assertTrue(severities.contains(Severity.LOW))
        assertTrue(severities.contains(Severity.MEDIUM))
        assertTrue(severities.contains(Severity.HIGH))
        assertTrue(severities.contains(Severity.CRITICAL))
        
        // RiskLevel enum should have adequate coverage
        val riskLevels = RiskLevel.values()
        assertEquals(5, riskLevels.size)
        assertTrue(riskLevels.contains(RiskLevel.MINIMAL))
        assertTrue(riskLevels.contains(RiskLevel.SEVERE))
    }
}