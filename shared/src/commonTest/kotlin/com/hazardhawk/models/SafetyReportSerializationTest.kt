package com.hazardhawk.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * JSON serialization/deserialization tests for SafetyReport models
 * Ensures all models can be properly serialized to/from JSON for API communication
 */
class SafetyReportSerializationTest {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val testInstant = Clock.System.now()
    
    @Test
    fun `SafetyReport should serialize and deserialize correctly`() {
        val originalReport = createTestSafetyReport()
        
        val jsonString = json.encodeToString(originalReport)
        val deserializedReport = json.decodeFromString<SafetyReport>(jsonString)
        
        assertEquals(originalReport.id, deserializedReport.id)
        assertEquals(originalReport.templateId, deserializedReport.templateId)
        assertEquals(originalReport.templateName, deserializedReport.templateName)
        assertEquals(originalReport.reportType, deserializedReport.reportType)
        assertEquals(originalReport.siteInformation, deserializedReport.siteInformation)
        assertEquals(originalReport.reporterInformation, deserializedReport.reporterInformation)
        assertEquals(originalReport.complianceStatus, deserializedReport.complianceStatus)
        assertEquals(originalReport.oshaCompliant, deserializedReport.oshaCompliant)
        assertEquals(originalReport.photos.size, deserializedReport.photos.size)
        assertEquals(originalReport.sections.size, deserializedReport.sections.size)
    }
    
    @Test
    fun `ReportTemplate should serialize and deserialize correctly`() {
        val originalTemplate = ReportTemplate(
            id = "template-123",
            name = "Safety Inspection Template",
            type = ReportType.SAFETY_AUDIT,
            description = "Comprehensive safety inspection template",
            sections = listOf(
                ReportSection(
                    id = "section-1",
                    title = "Site Overview",
                    required = true,
                    order = 1
                )
            ),
            requiredFields = listOf("siteName", "reporterName"),
            oshaCompliant = true,
            minimumPhotos = 5,
            requiredSignatures = listOf("Safety Manager", "Site Supervisor"),
            oshaStandards = listOf("29 CFR 1926.95", "29 CFR 1926.100")
        )
        
        val jsonString = json.encodeToString(originalTemplate)
        val deserializedTemplate = json.decodeFromString<ReportTemplate>(jsonString)
        
        assertEquals(originalTemplate.id, deserializedTemplate.id)
        assertEquals(originalTemplate.name, deserializedTemplate.name)
        assertEquals(originalTemplate.type, deserializedTemplate.type)
        assertEquals(originalTemplate.description, deserializedTemplate.description)
        assertEquals(originalTemplate.sections.size, deserializedTemplate.sections.size)
        assertEquals(originalTemplate.requiredFields.size, deserializedTemplate.requiredFields.size)
        assertEquals(originalTemplate.oshaCompliant, deserializedTemplate.oshaCompliant)
        assertEquals(originalTemplate.minimumPhotos, deserializedTemplate.minimumPhotos)
        assertEquals(originalTemplate.requiredSignatures.size, deserializedTemplate.requiredSignatures.size)
        assertEquals(originalTemplate.oshaStandards.size, deserializedTemplate.oshaStandards.size)
    }
    
    @Test
    fun `ReportSection should serialize and deserialize correctly`() {
        val originalSection = ReportSection(
            id = "section-456",
            title = "PPE Compliance Check",
            content = "All workers observed wearing required PPE including hard hats, safety glasses, and high-vis vests.",
            required = true,
            order = 3,
            photos = listOf("photo-101", "photo-102", "photo-103")
        )
        
        val jsonString = json.encodeToString(originalSection)
        val deserializedSection = json.decodeFromString<ReportSection>(jsonString)
        
        assertEquals(originalSection.id, deserializedSection.id)
        assertEquals(originalSection.title, deserializedSection.title)
        assertEquals(originalSection.content, deserializedSection.content)
        assertEquals(originalSection.required, deserializedSection.required)
        assertEquals(originalSection.order, deserializedSection.order)
        assertEquals(originalSection.photos.size, deserializedSection.photos.size)
    }
    
    @Test
    fun `SiteInformation should serialize and deserialize correctly`() {
        val originalSiteInfo = SiteInformation(
            siteName = "Downtown Office Complex",
            siteAddress = "456 Business Ave, Suite 100, City, State 12345",
            projectName = "Phase 2 Renovation",
            contractorName = "Premier Construction Group",
            location = Location(
                latitude = 40.7831,
                longitude = -73.9712,
                address = "Central Park, New York, NY",
                accuracy = 3.2f
            )
        )
        
        val jsonString = json.encodeToString(originalSiteInfo)
        val deserializedSiteInfo = json.decodeFromString<SiteInformation>(jsonString)
        
        assertEquals(originalSiteInfo.siteName, deserializedSiteInfo.siteName)
        assertEquals(originalSiteInfo.siteAddress, deserializedSiteInfo.siteAddress)
        assertEquals(originalSiteInfo.projectName, deserializedSiteInfo.projectName)
        assertEquals(originalSiteInfo.contractorName, deserializedSiteInfo.contractorName)
        assertNotNull(deserializedSiteInfo.location)
        assertEquals(originalSiteInfo.location?.latitude, deserializedSiteInfo.location?.latitude)
        assertEquals(originalSiteInfo.location?.longitude, deserializedSiteInfo.location?.longitude)
    }
    
    @Test
    fun `ReporterInformation should serialize and deserialize correctly`() {
        val originalReporterInfo = ReporterInformation(
            name = "Michael Johnson",
            title = "Site Safety Coordinator",
            company = "Johnson Safety Services",
            email = "m.johnson@safetycorp.com",
            phone = "+1-555-0199",
            certification = "OSHA 30-Hour, First Aid/CPR Certified"
        )
        
        val jsonString = json.encodeToString(originalReporterInfo)
        val deserializedReporterInfo = json.decodeFromString<ReporterInformation>(jsonString)
        
        assertEquals(originalReporterInfo.name, deserializedReporterInfo.name)
        assertEquals(originalReporterInfo.title, deserializedReporterInfo.title)
        assertEquals(originalReporterInfo.company, deserializedReporterInfo.company)
        assertEquals(originalReporterInfo.email, deserializedReporterInfo.email)
        assertEquals(originalReporterInfo.phone, deserializedReporterInfo.phone)
        assertEquals(originalReporterInfo.certification, deserializedReporterInfo.certification)
    }
    
    @Test
    fun `Location should serialize and deserialize correctly`() {
        val originalLocation = Location(
            latitude = 34.0522,
            longitude = -118.2437,
            address = "Los Angeles, CA, USA",
            accuracy = 8.7f
        )
        
        val jsonString = json.encodeToString(originalLocation)
        val deserializedLocation = json.decodeFromString<Location>(jsonString)
        
        assertEquals(originalLocation.latitude, deserializedLocation.latitude)
        assertEquals(originalLocation.longitude, deserializedLocation.longitude)
        assertEquals(originalLocation.address, deserializedLocation.address)
        assertEquals(originalLocation.accuracy, deserializedLocation.accuracy)
    }
    
    @Test
    fun `ReportSignature should serialize and deserialize correctly`() {
        val originalSignature = ReportSignature(
            signerName = "Sarah Chen",
            signerTitle = "Project Manager",
            signatureData = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
            signedAt = testInstant,
            verified = true
        )
        
        val jsonString = json.encodeToString(originalSignature)
        val deserializedSignature = json.decodeFromString<ReportSignature>(jsonString)
        
        assertEquals(originalSignature.signerName, deserializedSignature.signerName)
        assertEquals(originalSignature.signerTitle, deserializedSignature.signerTitle)
        assertEquals(originalSignature.signatureData, deserializedSignature.signatureData)
        assertEquals(originalSignature.signedAt, deserializedSignature.signedAt)
        assertEquals(originalSignature.verified, deserializedSignature.verified)
    }
    
    @Test
    fun `BatchOperation should serialize and deserialize correctly`() {
        val originalOperation = BatchOperation(
            id = "batch-789",
            operationType = BatchOperationType.ExportPhotos,
            status = BatchOperationStatus.Completed,
            totalItems = 25,
            processedItems = 25,
            failedItems = 0,
            progress = 1.0f,
            startedAt = testInstant,
            completedAt = testInstant
        )
        
        val jsonString = json.encodeToString(originalOperation)
        val deserializedOperation = json.decodeFromString<BatchOperation>(jsonString)
        
        assertEquals(originalOperation.id, deserializedOperation.id)
        assertEquals(originalOperation.operationType, deserializedOperation.operationType)
        assertEquals(originalOperation.status, deserializedOperation.status)
        assertEquals(originalOperation.totalItems, deserializedOperation.totalItems)
        assertEquals(originalOperation.processedItems, deserializedOperation.processedItems)
        assertEquals(originalOperation.failedItems, deserializedOperation.failedItems)
        assertEquals(originalOperation.progress, deserializedOperation.progress)
        assertEquals(originalOperation.startedAt, deserializedOperation.startedAt)
        assertEquals(originalOperation.completedAt, deserializedOperation.completedAt)
    }
    
    @Test
    fun `ReportGenerationRequest should serialize and deserialize correctly`() {
        val originalRequest = createTestReportGenerationRequest()
        
        val jsonString = json.encodeToString(originalRequest)
        val deserializedRequest = json.decodeFromString<ReportGenerationRequest>(jsonString)
        
        assertEquals(originalRequest.templateId, deserializedRequest.templateId)
        assertEquals(originalRequest.photoIds.size, deserializedRequest.photoIds.size)
        assertEquals(originalRequest.siteInfo, deserializedRequest.siteInfo)
        assertEquals(originalRequest.reporterInfo, deserializedRequest.reporterInfo)
        assertEquals(originalRequest.exportFormat, deserializedRequest.exportFormat)
        assertEquals(originalRequest.includeAnalysis, deserializedRequest.includeAnalysis)
        assertEquals(originalRequest.includeOSHACodes, deserializedRequest.includeOSHACodes)
    }
    
    @Test
    fun `ReportGenerationProgress should serialize and deserialize correctly`() {
        val originalProgress = ReportGenerationProgress(
            requestId = "progress-123",
            status = GenerationStatus.GeneratingReport,
            progress = 0.85f,
            currentStep = "Compiling final PDF document",
            totalPhotos = 8,
            processedPhotos = 7,
            startedAt = testInstant
        )
        
        val jsonString = json.encodeToString(originalProgress)
        val deserializedProgress = json.decodeFromString<ReportGenerationProgress>(jsonString)
        
        assertEquals(originalProgress.requestId, deserializedProgress.requestId)
        assertEquals(originalProgress.status, deserializedProgress.status)
        assertEquals(originalProgress.progress, deserializedProgress.progress)
        assertEquals(originalProgress.currentStep, deserializedProgress.currentStep)
        assertEquals(originalProgress.totalPhotos, deserializedProgress.totalPhotos)
        assertEquals(originalProgress.processedPhotos, deserializedProgress.processedPhotos)
        assertEquals(originalProgress.startedAt, deserializedProgress.startedAt)
    }
    
    @Test
    fun `Enums should serialize and deserialize correctly`() {
        val reportType = ReportType.HAZARD_IDENTIFICATION
        val exportFormat = ExportFormat.DOCX
        val complianceStatus = ComplianceStatus.COMPLIANT
        val generationStatus = GenerationStatus.Failed
        val batchOpType = BatchOperationType.AnalyzePhotos
        val batchOpStatus = BatchOperationStatus.Cancelled
        
        assertEquals(reportType, json.decodeFromString<ReportType>(json.encodeToString(reportType)))
        assertEquals(exportFormat, json.decodeFromString<ExportFormat>(json.encodeToString(exportFormat)))
        assertEquals(complianceStatus, json.decodeFromString<ComplianceStatus>(json.encodeToString(complianceStatus)))
        assertEquals(generationStatus, json.decodeFromString<GenerationStatus>(json.encodeToString(generationStatus)))
        assertEquals(batchOpType, json.decodeFromString<BatchOperationType>(json.encodeToString(batchOpType)))
        assertEquals(batchOpStatus, json.decodeFromString<BatchOperationStatus>(json.encodeToString(batchOpStatus)))
    }
    
    private fun createTestSafetyReport(): SafetyReport {
        return SafetyReport(
            id = "report-test-123",
            templateId = "template-test-456",
            templateName = "Test Safety Report",
            reportType = ReportType.PRE_TASK_PLAN,
            siteInformation = SiteInformation(
                siteName = "Test Construction Site",
                siteAddress = "123 Test Street, Test City, TS 12345",
                contractorName = "Test Construction Co"
            ),
            reporterInformation = ReporterInformation(
                name = "Test Reporter",
                title = "Test Safety Manager",
                company = "Test Safety Corp"
            ),
            photos = listOf("photo-1", "photo-2", "photo-3"),
            sections = listOf(
                ReportSection(
                    id = "section-1",
                    title = "Test Section",
                    content = "Test content",
                    required = true,
                    order = 1
                )
            ),
            complianceStatus = ComplianceStatus.COMPLIANT,
            createdAt = testInstant,
            updatedAt = testInstant,
            oshaCompliant = true
        )
    }
    
    private fun createTestReportGenerationRequest(): ReportGenerationRequest {
        return ReportGenerationRequest(
            templateId = "template-request-789",
            photoIds = listOf("photo-req-1", "photo-req-2", "photo-req-3", "photo-req-4"),
            siteInfo = SiteInformation(
                siteName = "Request Test Site",
                siteAddress = "789 Request Ave",
                contractorName = "Request Construction LLC"
            ),
            reporterInfo = ReporterInformation(
                name = "Request Reporter",
                title = "Request Safety Officer",
                company = "Request Safety Group"
            ),
            exportFormat = ExportFormat.HTML,
            includeAnalysis = true,
            includeOSHACodes = true
        )
    }
}
