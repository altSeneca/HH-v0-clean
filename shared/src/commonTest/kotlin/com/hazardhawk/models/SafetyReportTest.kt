package com.hazardhawk.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Unit tests for SafetyReport model classes
 * Tests basic construction, validation, and serialization requirements
 */
class SafetyReportTest {
    
    private val testInstant = Clock.System.now()
    
    @Test
    fun `SafetyReport should be created with required fields`() {
        val siteInfo = SiteInformation(
            siteName = "Test Construction Site",
            siteAddress = "123 Main St",
            contractorName = "ABC Construction"
        )
        
        val reporterInfo = ReporterInformation(
            name = "John Doe",
            title = "Safety Manager",
            company = "ABC Construction"
        )
        
        val report = SafetyReport(
            id = "report-123",
            templateId = "template-456",
            templateName = "Daily Inspection",
            reportType = ReportType.DAILY_INSPECTION,
            siteInformation = siteInfo,
            reporterInformation = reporterInfo,
            createdAt = testInstant,
            updatedAt = testInstant
        )
        
        assertEquals("report-123", report.id)
        assertEquals("template-456", report.templateId)
        assertEquals("Daily Inspection", report.templateName)
        assertEquals(ReportType.DAILY_INSPECTION, report.reportType)
        assertEquals(siteInfo, report.siteInformation)
        assertEquals(reporterInfo, report.reporterInformation)
        assertEquals(ComplianceStatus.PENDING, report.complianceStatus)
        assertFalse(report.oshaCompliant)
    }
    
    @Test
    fun `ReportTemplate should have correct default values`() {
        val template = ReportTemplate(
            id = "template-123",
            name = "Incident Report Template",
            type = ReportType.INCIDENT_REPORT,
            description = "Standard incident report template"
        )
        
        assertEquals("template-123", template.id)
        assertEquals("Incident Report Template", template.name)
        assertEquals(ReportType.INCIDENT_REPORT, template.type)
        assertTrue(template.sections.isEmpty())
        assertTrue(template.requiredFields.isEmpty())
        assertTrue(template.oshaCompliant)
        assertEquals(1, template.minimumPhotos)
        assertTrue(template.requiredSignatures.isEmpty())
        assertTrue(template.oshaStandards.isEmpty())
    }
    
    @Test
    fun `ReportSection should be created with required fields`() {
        val section = ReportSection(
            id = "section-123",
            title = "Hazards Identified",
            content = "No hazards found during inspection",
            required = true,
            order = 1,
            photos = listOf("photo-1", "photo-2")
        )
        
        assertEquals("section-123", section.id)
        assertEquals("Hazards Identified", section.title)
        assertEquals("No hazards found during inspection", section.content)
        assertTrue(section.required)
        assertEquals(1, section.order)
        assertEquals(2, section.photos.size)
    }
    
    @Test
    fun `SiteInformation should be created with required fields`() {
        val location = Location(
            latitude = 40.7128,
            longitude = -74.0060,
            address = "New York, NY",
            accuracy = 10.0f
        )
        
        val siteInfo = SiteInformation(
            siteName = "Manhattan Construction Site",
            siteAddress = "123 Broadway, New York, NY 10001",
            projectName = "Empire State Building Renovation",
            contractorName = "Elite Construction LLC",
            location = location
        )
        
        assertEquals("Manhattan Construction Site", siteInfo.siteName)
        assertEquals("123 Broadway, New York, NY 10001", siteInfo.siteAddress)
        assertEquals("Empire State Building Renovation", siteInfo.projectName)
        assertEquals("Elite Construction LLC", siteInfo.contractorName)
        assertNotNull(siteInfo.location)
        assertEquals(40.7128, siteInfo.location?.latitude)
    }
    
    @Test
    fun `ReporterInformation should be created with required fields`() {
        val reporterInfo = ReporterInformation(
            name = "Jane Smith",
            title = "Site Safety Supervisor",
            company = "Safety First Corp",
            email = "jane.smith@safetyfirst.com",
            phone = "+1-555-0123",
            certification = "OSHA 30-Hour Construction"
        )
        
        assertEquals("Jane Smith", reporterInfo.name)
        assertEquals("Site Safety Supervisor", reporterInfo.title)
        assertEquals("Safety First Corp", reporterInfo.company)
        assertEquals("jane.smith@safetyfirst.com", reporterInfo.email)
        assertEquals("+1-555-0123", reporterInfo.phone)
        assertEquals("OSHA 30-Hour Construction", reporterInfo.certification)
    }
    
    @Test
    fun `Location should store coordinates correctly`() {
        val location = Location(
            latitude = 37.7749,
            longitude = -122.4194,
            address = "San Francisco, CA",
            accuracy = 5.5f
        )
        
        assertEquals(37.7749, location.latitude)
        assertEquals(-122.4194, location.longitude)
        assertEquals("San Francisco, CA", location.address)
        assertEquals(5.5f, location.accuracy)
    }
    
    @Test
    fun `ReportSignature should be created with required fields`() {
        val signature = ReportSignature(
            signerName = "Safety Manager",
            signerTitle = "Construction Safety Lead",
            signatureData = "base64encodeddata==",
            signedAt = testInstant,
            verified = true
        )
        
        assertEquals("Safety Manager", signature.signerName)
        assertEquals("Construction Safety Lead", signature.signerTitle)
        assertEquals("base64encodeddata==", signature.signatureData)
        assertEquals(testInstant, signature.signedAt)
        assertTrue(signature.verified)
    }
    
    @Test
    fun `BatchOperation should track progress correctly`() {
        val operation = BatchOperation(
            id = "batch-123",
            operationType = BatchOperationType.GenerateReports,
            status = BatchOperationStatus.InProgress,
            totalItems = 10,
            processedItems = 5,
            failedItems = 1,
            progress = 0.5f,
            startedAt = testInstant
        )
        
        assertEquals("batch-123", operation.id)
        assertEquals(BatchOperationType.GenerateReports, operation.operationType)
        assertEquals(BatchOperationStatus.InProgress, operation.status)
        assertEquals(10, operation.totalItems)
        assertEquals(5, operation.processedItems)
        assertEquals(1, operation.failedItems)
        assertEquals(0.5f, operation.progress)
        assertEquals(testInstant, operation.startedAt)
    }
    
    @Test
    fun `ReportGenerationRequest should have correct default values`() {
        val siteInfo = SiteInformation(
            siteName = "Test Site",
            siteAddress = "Test Address",
            contractorName = "Test Contractor"
        )
        
        val reporterInfo = ReporterInformation(
            name = "Test Reporter",
            title = "Test Title",
            company = "Test Company"
        )
        
        val request = ReportGenerationRequest(
            templateId = "template-123",
            photoIds = listOf("photo-1", "photo-2"),
            siteInfo = siteInfo,
            reporterInfo = reporterInfo
        )
        
        assertEquals("template-123", request.templateId)
        assertEquals(2, request.photoIds.size)
        assertEquals(ExportFormat.PDF, request.exportFormat)
        assertTrue(request.includeAnalysis)
        assertTrue(request.includeOSHACodes)
    }
    
    @Test
    fun `ReportGenerationProgress should track generation status`() {
        val progress = ReportGenerationProgress(
            requestId = "request-123",
            status = GenerationStatus.ProcessingPhotos,
            progress = 0.75f,
            currentStep = "Processing photo 3 of 4",
            totalPhotos = 4,
            processedPhotos = 3,
            startedAt = testInstant
        )
        
        assertEquals("request-123", progress.requestId)
        assertEquals(GenerationStatus.ProcessingPhotos, progress.status)
        assertEquals(0.75f, progress.progress)
        assertEquals("Processing photo 3 of 4", progress.currentStep)
        assertEquals(4, progress.totalPhotos)
        assertEquals(3, progress.processedPhotos)
        assertEquals(testInstant, progress.startedAt)
    }
}
