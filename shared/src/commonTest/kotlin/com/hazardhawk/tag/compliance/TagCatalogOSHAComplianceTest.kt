package com.hazardhawk.tag.compliance

import com.hazardhawk.data.repositories.TagRepositoryImpl
import com.hazardhawk.core.models.Tag
import com.hazardhawk.compliance.OSHAValidator
import com.hazardhawk.compliance.OSHAStandard
import com.hazardhawk.compliance.ComplianceReport
import com.hazardhawk.compliance.ComplianceStatus
import com.hazardhawk.test.TestDataFactory
import com.hazardhawk.test.MockInMemoryDatabase
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive OSHA compliance validation tests for tag catalog operations.
 * Tests regulatory compliance, safety standard validation, and audit trail generation.
 * Ensures all tags meet OSHA requirements and maintain compliance documentation.
 */
class TagCatalogOSHAComplianceTest {
    
    private lateinit var mockDatabase: MockInMemoryDatabase
    private lateinit var repository: TagRepositoryImpl
    private lateinit var mockOSHAValidator: OSHAValidator
    
    @BeforeTest
    fun setup() {
        mockDatabase = MockInMemoryDatabase()
        repository = TagRepositoryImpl(mockDatabase)
        mockOSHAValidator = mockk(relaxed = true)
    }
    
    @AfterTest
    fun teardown() {
        mockDatabase.clear()
        clearAllMocks()
    }
    
    // MARK: - OSHA Code Validation Tests
    
    @Test
    fun `valid OSHA reference codes should be accepted`() = runTest {
        // Given - Tags with valid OSHA reference codes
        val validOSHACodes = listOf(
            "1926.501", // Fall protection in construction
            "1926.95",  // Personal protective equipment
            "1926.652", // Requirements for protective systems (excavation)
            "1910.147", // Control of hazardous energy (lockout/tagout)
            "1926.416", // General requirements for electrical construction
            "1926.850"  // Preparatory operations (demolition)
        )
        
        validOSHACodes.forEach { oshaCode ->
            every { mockOSHAValidator.isValidOSHACode(oshaCode) } returns true
            every { mockOSHAValidator.getOSHAStandard(oshaCode) } returns OSHAStandard(
                code = oshaCode,
                title = "OSHA Standard for $oshaCode",
                category = "Construction Safety",
                isActive = true
            )
            
            // When - Validate OSHA code
            val isValid = mockOSHAValidator.isValidOSHACode(oshaCode)
            val standard = mockOSHAValidator.getOSHAStandard(oshaCode)
            
            // Then - Should be valid and return standard details
            assertTrue(isValid, "OSHA code $oshaCode should be valid")
            assertEquals(oshaCode, standard.code)
            assertTrue(standard.isActive)
        }
    }
    
    @Test
    fun `invalid OSHA reference codes should be rejected`() = runTest {
        // Given - Tags with invalid OSHA reference codes
        val invalidOSHACodes = listOf(
            "9999.999", // Non-existent code
            "1926.ABC", // Invalid format
            "1910.",    // Incomplete code
            "OSHA-123", // Wrong format
            "1926.501.1.2.3", // Too specific
            ""          // Empty code
        )
        
        invalidOSHACodes.forEach { invalidCode ->
            every { mockOSHAValidator.isValidOSHACode(invalidCode) } returns false
            
            // When - Validate invalid OSHA code
            val isValid = mockOSHAValidator.isValidOSHACode(invalidCode)
            
            // Then - Should be rejected
            assertFalse(isValid, "OSHA code '$invalidCode' should be invalid")
        }
    }
    
    @Test
    fun `deprecated OSHA codes should be flagged for update`() = runTest {
        // Given - Tags with deprecated OSHA codes
        val deprecatedCode = "1926.500" // Example deprecated code
        every { mockOSHAValidator.isValidOSHACode(deprecatedCode) } returns true
        every { mockOSHAValidator.getOSHAStandard(deprecatedCode) } returns OSHAStandard(
            code = deprecatedCode,
            title = "Deprecated Safety Standard",
            category = "Construction Safety",
            isActive = false,
            replacedBy = "1926.501"
        )
        
        // When - Check deprecated status
        val standard = mockOSHAValidator.getOSHAStandard(deprecatedCode)
        
        // Then - Should identify as deprecated with replacement
        assertFalse(standard.isActive)
        assertEquals("1926.501", standard.replacedBy)
        
        verify { mockOSHAValidator.getOSHAStandard(deprecatedCode) }
    }
    
    // MARK: - Tag Category Compliance Tests
    
    @Test
    fun `safety tags should reference appropriate OSHA standards`() = runTest {
        // Given - Safety-related tags
        val safetyTags = listOf(
            TestDataFactory.createTestTag(
                id = "safety-1",
                name = "Fall Protection Required",
                category = "Safety",
                oshaReferences = listOf("1926.501", "1926.502")
            ),
            TestDataFactory.createTestTag(
                id = "safety-2", 
                name = "PPE Required",
                category = "Safety",
                oshaReferences = listOf("1926.95")
            )
        )
        
        safetyTags.forEach { tag ->
            every { mockOSHAValidator.validateTagCompliance(tag) } returns ComplianceReport(
                tagId = tag.id,
                status = ComplianceStatus.COMPLIANT,
                oshaStandards = tag.oshaReferences,
                violations = emptyList(),
                recommendations = emptyList()
            )
            
            // When - Validate safety tag compliance
            val complianceReport = mockOSHAValidator.validateTagCompliance(tag)
            
            // Then - Should be compliant with relevant OSHA standards
            assertEquals(ComplianceStatus.COMPLIANT, complianceReport.status)
            assertTrue(complianceReport.oshaStandards.isNotEmpty())
            assertTrue(complianceReport.violations.isEmpty())
        }
    }
    
    @Test
    fun `electrical tags should reference electrical safety standards`() = runTest {
        // Given - Electrical safety tags
        val electricalTag = TestDataFactory.createTestTag(
            id = "electrical-1",
            name = "Electrical Lockout/Tagout",
            category = "Electrical",
            oshaReferences = listOf("1910.147", "1926.416")
        )
        
        every { mockOSHAValidator.validateTagCompliance(electricalTag) } returns ComplianceReport(
            tagId = electricalTag.id,
            status = ComplianceStatus.COMPLIANT,
            oshaStandards = electricalTag.oshaReferences,
            violations = emptyList(),
            recommendations = listOf("Ensure all electrical workers are properly trained")
        )
        
        // When - Validate electrical tag compliance
        val complianceReport = mockOSHAValidator.validateTagCompliance(electricalTag)
        
        // Then - Should reference electrical safety standards
        assertEquals(ComplianceStatus.COMPLIANT, complianceReport.status)
        assertTrue(complianceReport.oshaStandards.contains("1910.147")) // LOTO standard
        assertTrue(complianceReport.oshaStandards.contains("1926.416")) // Electrical construction
        assertTrue(complianceReport.recommendations.isNotEmpty())
    }
    
    @Test
    fun `excavation tags should reference excavation safety standards`() = runTest {
        // Given - Excavation safety tags
        val excavationTag = TestDataFactory.createTestTag(
            id = "excavation-1",
            name = "Trench Shoring Required",
            category = "Excavation",
            oshaReferences = listOf("1926.651", "1926.652")
        )
        
        every { mockOSHAValidator.validateTagCompliance(excavationTag) } returns ComplianceReport(
            tagId = excavationTag.id,
            status = ComplianceStatus.COMPLIANT,
            oshaStandards = excavationTag.oshaReferences,
            violations = emptyList(),
            recommendations = listOf("Daily excavation inspections required")
        )
        
        // When - Validate excavation tag compliance
        val complianceReport = mockOSHAValidator.validateTagCompliance(excavationTag)
        
        // Then - Should reference excavation standards
        assertEquals(ComplianceStatus.COMPLIANT, complianceReport.status)
        assertTrue(complianceReport.oshaStandards.contains("1926.651")) // General excavation requirements
        assertTrue(complianceReport.oshaStandards.contains("1926.652")) // Protective systems
    }
    
    // MARK: - Compliance Violation Detection Tests
    
    @Test
    fun `tags without required OSHA references should be flagged`() = runTest {
        // Given - Safety tag without OSHA references
        val nonCompliantTag = TestDataFactory.createTestTag(
            id = "non-compliant-1",
            name = "Safety Requirement",
            category = "Safety",
            oshaReferences = emptyList() // Missing OSHA references
        )
        
        every { mockOSHAValidator.validateTagCompliance(nonCompliantTag) } returns ComplianceReport(
            tagId = nonCompliantTag.id,
            status = ComplianceStatus.NON_COMPLIANT,
            oshaStandards = emptyList(),
            violations = listOf("Safety tags must reference applicable OSHA standards"),
            recommendations = listOf("Add appropriate OSHA references for safety requirements")
        )
        
        // When - Validate non-compliant tag
        val complianceReport = mockOSHAValidator.validateTagCompliance(nonCompliantTag)
        
        // Then - Should be flagged as non-compliant
        assertEquals(ComplianceStatus.NON_COMPLIANT, complianceReport.status)
        assertTrue(complianceReport.violations.isNotEmpty())
        assertTrue(complianceReport.violations.first().contains("OSHA standards"))
    }
    
    @Test
    fun `tags with incorrect OSHA references should be flagged`() = runTest {
        // Given - Tag with mismatched OSHA references
        val mismatchedTag = TestDataFactory.createTestTag(
            id = "mismatched-1",
            name = "Electrical Safety",
            category = "Electrical",
            oshaReferences = listOf("1926.501") // Fall protection standard for electrical work
        )
        
        every { mockOSHAValidator.validateTagCompliance(mismatchedTag) } returns ComplianceReport(
            tagId = mismatchedTag.id,
            status = ComplianceStatus.NEEDS_REVIEW,
            oshaStandards = mismatchedTag.oshaReferences,
            violations = listOf("OSHA standard 1926.501 is not appropriate for electrical work"),
            recommendations = listOf("Consider using 1910.147 or 1926.416 for electrical safety")
        )
        
        // When - Validate mismatched tag
        val complianceReport = mockOSHAValidator.validateTagCompliance(mismatchedTag)
        
        // Then - Should need review
        assertEquals(ComplianceStatus.NEEDS_REVIEW, complianceReport.status)
        assertTrue(complianceReport.violations.isNotEmpty())
        assertTrue(complianceReport.recommendations.isNotEmpty())
    }
    
    // MARK: - Compliance Report Generation Tests
    
    @Test
    fun `compliance reports should include all required information`() = runTest {
        // Given - Set of tags for compliance reporting
        val tagsForReport = listOf(
            TestDataFactory.createTestTag(
                id = "report-1",
                name = "Compliant Tag",
                category = "Safety",
                oshaReferences = listOf("1926.95")
            ),
            TestDataFactory.createTestTag(
                id = "report-2",
                name = "Non-Compliant Tag",
                category = "Safety",
                oshaReferences = emptyList()
            )
        )
        
        tagsForReport.forEach { mockDatabase.insertTag(it) }
        
        every { mockOSHAValidator.generateComplianceReport(any()) } returns ComplianceReport(
            tagId = "bulk-report",
            status = ComplianceStatus.PARTIAL_COMPLIANCE,
            oshaStandards = listOf("1926.95"),
            violations = listOf("1 tag missing OSHA references"),
            recommendations = listOf("Review and add OSHA references to all safety tags"),
            summary = ComplianceSummary(
                totalTags = 2,
                compliantTags = 1,
                nonCompliantTags = 1,
                needsReviewTags = 0,
                compliancePercentage = 50.0
            )
        )
        
        // When - Generate compliance report
        val allTags = repository.getAllTags().first()
        val complianceReport = mockOSHAValidator.generateComplianceReport(allTags)
        
        // Then - Report should be comprehensive
        assertEquals(ComplianceStatus.PARTIAL_COMPLIANCE, complianceReport.status)
        assertNotNull(complianceReport.summary)
        assertEquals(2, complianceReport.summary!!.totalTags)
        assertEquals(1, complianceReport.summary!!.compliantTags)
        assertEquals(1, complianceReport.summary!!.nonCompliantTags)
        assertEquals(50.0, complianceReport.summary!!.compliancePercentage)
    }
    
    @Test
    fun `compliance reports should track improvement over time`() = runTest {
        // Given - Historical compliance data
        val historicalReports = listOf(
            ComplianceReport(
                tagId = "historical-1",
                status = ComplianceStatus.PARTIAL_COMPLIANCE,
                timestamp = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000), // 30 days ago
                summary = ComplianceSummary(
                    totalTags = 100,
                    compliantTags = 60,
                    compliancePercentage = 60.0
                )
            ),
            ComplianceReport(
                tagId = "historical-2",
                status = ComplianceStatus.PARTIAL_COMPLIANCE,
                timestamp = System.currentTimeMillis(),
                summary = ComplianceSummary(
                    totalTags = 100,
                    compliantTags = 80,
                    compliancePercentage = 80.0
                )
            )
        )
        
        every { mockOSHAValidator.getComplianceTrend(any(), any()) } returns ComplianceTrend(
            previousCompliance = 60.0,
            currentCompliance = 80.0,
            improvement = 20.0,
            trendDirection = TrendDirection.IMPROVING
        )
        
        // When - Analyze compliance trend
        val trend = mockOSHAValidator.getComplianceTrend(
            historicalReports.first(),
            historicalReports.last()
        )
        
        // Then - Should show improvement
        assertEquals(TrendDirection.IMPROVING, trend.trendDirection)
        assertEquals(20.0, trend.improvement)
        assertTrue(trend.currentCompliance > trend.previousCompliance)
    }
    
    // MARK: - Industry Standard Compliance Tests
    
    @Test
    fun `construction tags should comply with construction industry standards`() = runTest {
        // Given - Construction-specific tags
        val constructionTags = listOf(
            TestDataFactory.createTestTag(
                id = "construction-1",
                name = "Scaffolding Safety",
                category = "Construction",
                oshaReferences = listOf("1926.451")
            ),
            TestDataFactory.createTestTag(
                id = "construction-2",
                name = "Crane Operation",
                category = "Construction", 
                oshaReferences = listOf("1926.1400")
            )
        )
        
        constructionTags.forEach { tag ->
            every { mockOSHAValidator.validateIndustryCompliance(tag, "Construction") } returns IndustryComplianceResult(
                industry = "Construction",
                isCompliant = true,
                applicableStandards = tag.oshaReferences,
                industrySpecificRequirements = listOf("Daily safety briefings", "Competent person on site")
            )
            
            // When - Validate industry compliance
            val industryCompliance = mockOSHAValidator.validateIndustryCompliance(tag, "Construction")
            
            // Then - Should meet construction industry requirements
            assertTrue(industryCompliance.isCompliant)
            assertEquals("Construction", industryCompliance.industry)
            assertTrue(industryCompliance.applicableStandards.isNotEmpty())
        }
    }
    
    @Test
    fun `manufacturing tags should comply with general industry standards`() = runTest {
        // Given - Manufacturing-specific tag
        val manufacturingTag = TestDataFactory.createTestTag(
            id = "manufacturing-1",
            name = "Machine Guarding",
            category = "Equipment",
            oshaReferences = listOf("1910.212")
        )
        
        every { mockOSHAValidator.validateIndustryCompliance(manufacturingTag, "Manufacturing") } returns IndustryComplianceResult(
            industry = "Manufacturing",
            isCompliant = true,
            applicableStandards = listOf("1910.212"),
            industrySpecificRequirements = listOf("Machine guarding inspection", "Employee training documentation")
        )
        
        // When - Validate manufacturing compliance
        val industryCompliance = mockOSHAValidator.validateIndustryCompliance(manufacturingTag, "Manufacturing")
        
        // Then - Should meet manufacturing requirements
        assertTrue(industryCompliance.isCompliant)
        assertTrue(industryCompliance.applicableStandards.contains("1910.212"))
    }
    
    // MARK: - Audit Trail and Documentation Tests
    
    @Test
    fun `compliance validations should create audit trail entries`() = runTest {
        // Given - Tag being validated for compliance
        val auditTag = TestDataFactory.createTestTag(
            id = "audit-1",
            name = "Audited Safety Tag"
        )
        
        every { mockOSHAValidator.validateTagCompliance(auditTag) } answers {
            // Simulate audit trail creation
            mockOSHAValidator.createAuditEntry(
                AuditEntry(
                    tagId = auditTag.id,
                    action = "COMPLIANCE_VALIDATION",
                    timestamp = System.currentTimeMillis(),
                    userId = "compliance-officer-1",
                    details = "Automated compliance validation performed"
                )
            )
            ComplianceReport(
                tagId = auditTag.id,
                status = ComplianceStatus.COMPLIANT,
                oshaStandards = emptyList(),
                violations = emptyList(),
                recommendations = emptyList()
            )
        }
        
        every { mockOSHAValidator.createAuditEntry(any()) } returns Unit
        
        // When - Validate compliance
        mockOSHAValidator.validateTagCompliance(auditTag)
        
        // Then - Should create audit entry
        verify { mockOSHAValidator.createAuditEntry(any()) }
    }
    
    @Test
    fun `compliance reports should be exportable for regulatory review`() = runTest {
        // Given - Compliance report ready for export
        val exportReport = ComplianceReport(
            tagId = "export-report",
            status = ComplianceStatus.COMPLIANT,
            oshaStandards = listOf("1926.95", "1926.501"),
            violations = emptyList(),
            recommendations = emptyList(),
            summary = ComplianceSummary(
                totalTags = 150,
                compliantTags = 150,
                compliancePercentage = 100.0
            )
        )
        
        every { mockOSHAValidator.exportComplianceReport(exportReport, "PDF") } returns ExportResult(
            format = "PDF",
            filePath = "/exports/compliance_report_${System.currentTimeMillis()}.pdf",
            success = true,
            fileSize = 1024000 // 1MB
        )
        
        // When - Export compliance report
        val exportResult = mockOSHAValidator.exportComplianceReport(exportReport, "PDF")
        
        // Then - Should create exportable document
        assertTrue(exportResult.success)
        assertEquals("PDF", exportResult.format)
        assertTrue(exportResult.filePath.endsWith(".pdf"))
        assertTrue(exportResult.fileSize > 0)
    }
    
    // MARK: - Automated Compliance Monitoring Tests
    
    @Test
    fun `compliance monitoring should detect changes requiring revalidation`() = runTest {
        // Given - Tag that gets modified
        val monitoredTag = TestDataFactory.createTestTag(
            id = "monitored-1",
            name = "Original Name",
            category = "Safety",
            oshaReferences = listOf("1926.95")
        )
        
        val modifiedTag = monitoredTag.copy(
            name = "Modified Name",
            category = "Electrical", // Category change should trigger revalidation
            oshaReferences = listOf("1910.147") // OSHA references changed
        )
        
        every { mockOSHAValidator.requiresRevalidation(monitoredTag, modifiedTag) } returns true
        every { mockOSHAValidator.scheduleRevalidation(modifiedTag.id) } returns Unit
        
        // When - Monitor tag changes
        val needsRevalidation = mockOSHAValidator.requiresRevalidation(monitoredTag, modifiedTag)
        
        if (needsRevalidation) {
            mockOSHAValidator.scheduleRevalidation(modifiedTag.id)
        }
        
        // Then - Should trigger revalidation
        assertTrue(needsRevalidation)
        verify { mockOSHAValidator.scheduleRevalidation(modifiedTag.id) }
    }
    
    @Test
    fun `periodic compliance reviews should be scheduled automatically`() = runTest {
        // Given - Tags requiring periodic review
        val reviewTags = TestDataFactory.createLargeTagList(50)
        reviewTags.forEach { mockDatabase.insertTag(it) }
        
        every { mockOSHAValidator.schedulePeriodicReview(any()) } returns PeriodicReviewSchedule(
            tagIds = reviewTags.map { it.id },
            reviewFrequency = ReviewFrequency.QUARTERLY,
            nextReviewDate = System.currentTimeMillis() + (90 * 24 * 60 * 60 * 1000) // 90 days from now
        )
        
        // When - Schedule periodic reviews
        val allTags = repository.getAllTags().first()
        val reviewSchedule = mockOSHAValidator.schedulePeriodicReview(allTags)
        
        // Then - Should schedule appropriate reviews
        assertEquals(ReviewFrequency.QUARTERLY, reviewSchedule.reviewFrequency)
        assertEquals(50, reviewSchedule.tagIds.size)
        assertTrue(reviewSchedule.nextReviewDate > System.currentTimeMillis())
    }
}

// MARK: - Supporting Classes and Enums for OSHA Compliance

data class OSHAStandard(
    val code: String,
    val title: String,
    val category: String,
    val isActive: Boolean,
    val replacedBy: String? = null,
    val effectiveDate: Long = System.currentTimeMillis(),
    val description: String = ""
)

data class ComplianceReport(
    val tagId: String,
    val status: ComplianceStatus,
    val oshaStandards: List<String>,
    val violations: List<String>,
    val recommendations: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
    val summary: ComplianceSummary? = null
)

enum class ComplianceStatus {
    COMPLIANT,
    NON_COMPLIANT,
    PARTIAL_COMPLIANCE,
    NEEDS_REVIEW
}

data class ComplianceSummary(
    val totalTags: Int,
    val compliantTags: Int,
    val nonCompliantTags: Int = 0,
    val needsReviewTags: Int = 0,
    val compliancePercentage: Double
)

data class ComplianceTrend(
    val previousCompliance: Double,
    val currentCompliance: Double,
    val improvement: Double,
    val trendDirection: TrendDirection
)

enum class TrendDirection {
    IMPROVING,
    DECLINING,
    STABLE
}

data class IndustryComplianceResult(
    val industry: String,
    val isCompliant: Boolean,
    val applicableStandards: List<String>,
    val industrySpecificRequirements: List<String>
)

data class AuditEntry(
    val tagId: String,
    val action: String,
    val timestamp: Long,
    val userId: String,
    val details: String
)

data class ExportResult(
    val format: String,
    val filePath: String,
    val success: Boolean,
    val fileSize: Long,
    val error: String? = null
)

data class PeriodicReviewSchedule(
    val tagIds: List<String>,
    val reviewFrequency: ReviewFrequency,
    val nextReviewDate: Long
)

enum class ReviewFrequency {
    MONTHLY,
    QUARTERLY,
    SEMI_ANNUALLY,
    ANNUALLY
}

// Mock OSHA Validator interface
abstract class OSHAValidator {
    abstract fun isValidOSHACode(code: String): Boolean
    abstract fun getOSHAStandard(code: String): OSHAStandard
    abstract fun validateTagCompliance(tag: Tag): ComplianceReport
    abstract fun generateComplianceReport(tags: List<Tag>): ComplianceReport
    abstract fun getComplianceTrend(previous: ComplianceReport, current: ComplianceReport): ComplianceTrend
    abstract fun validateIndustryCompliance(tag: Tag, industry: String): IndustryComplianceResult
    abstract fun createAuditEntry(entry: AuditEntry)
    abstract fun exportComplianceReport(report: ComplianceReport, format: String): ExportResult
    abstract fun requiresRevalidation(original: Tag, modified: Tag): Boolean
    abstract fun scheduleRevalidation(tagId: String)
    abstract fun schedulePeriodicReview(tags: List<Tag>): PeriodicReviewSchedule
}