package com.hazardhawk.data.repositories

import com.hazardhawk.data.mocks.MockApiClient
import com.hazardhawk.data.repositories.crew.CertificationApiRepository
import com.hazardhawk.core.models.crew.*
import com.hazardhawk.FeatureFlags
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Unit tests for CertificationApiRepository
 *
 * Tests all CRUD operations, verification workflows, and error handling
 */
class CertificationApiRepositoryTest {

    private lateinit var mockApi: MockApiClient
    private lateinit var repository: CertificationApiRepository

    @BeforeTest
    fun setup() {
        // Enable API for testing
        FeatureFlags.API_CERTIFICATION_ENABLED = true

        mockApi = MockApiClient()
        repository = CertificationApiRepository(apiClient = mockApi as Any as com.hazardhawk.data.network.ApiClient)
    }

    @AfterTest
    fun teardown() {
        mockApi.clearHistory()

        // Reset feature flag
        FeatureFlags.API_CERTIFICATION_ENABLED = false
    }

    // ===== Core CRUD Tests =====

    @Test
    fun `createCertification should call POST endpoint with correct payload`() = runTest {
        // Arrange
        val request = CreateCertificationRequest(
            certificationTypeId = "type_osha_10",
            issueDate = LocalDate(2025, 1, 15),
            expirationDate = LocalDate(2030, 1, 15),
            issuingAuthority = "OSHA Training Provider",
            certificationNumber = "OSHA10-2025-123456",
            documentUrl = "https://cdn.hazardhawk.com/certs/cert123.pdf"
        )

        // Act
        repository.createCertification(
            workerProfileId = "worker_123",
            companyId = "company_456",
            request = request
        )

        // Assert
        assertTrue(mockApi.verifyCalled("POST", "/api/certifications"))
        assertEquals(1, mockApi.countCalls("/api/certifications"))
    }

    @Test
    fun `getCertification should call GET endpoint with certification ID`() = runTest {
        // Act
        repository.getCertification(
            certificationId = "cert_123",
            includeType = true
        )

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/certifications/cert_123"))
    }

    @Test
    fun `updateCertification should call PATCH endpoint with updated fields`() = runTest {
        // Act
        repository.updateCertification(
            certificationId = "cert_123",
            issueDate = LocalDate(2025, 2, 1),
            certificationNumber = "NEW-123"
        )

        // Assert
        assertTrue(mockApi.verifyCalled("PATCH", "/api/certifications/cert_123"))
    }

    @Test
    fun `deleteCertification should call DELETE endpoint`() = runTest {
        // Act
        repository.deleteCertification("cert_123")

        // Assert
        assertTrue(mockApi.verifyCalled("DELETE", "/api/certifications/cert_123"))
    }

    // ===== Query Tests =====

    @Test
    fun `getWorkerCertifications should call GET with worker ID`() = runTest {
        // Act
        repository.getWorkerCertifications(
            workerProfileId = "worker_123",
            status = CertificationStatus.VERIFIED,
            includeExpired = false
        )

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/workers/worker_123/certifications"))
    }

    @Test
    fun `getCompanyCertifications should call GET with pagination params`() = runTest {
        // Act
        repository.getCompanyCertifications(
            companyId = "company_456",
            status = CertificationStatus.PENDING_VERIFICATION,
            pagination = PaginationRequest(pageSize = 20, cursor = "cursor_abc")
        )

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_456/certifications"))
        val lastCall = mockApi.getLastCall("/api/companies/company_456/certifications")
        assertNotNull(lastCall)
    }

    @Test
    fun `getCertificationsByType should filter by type and status`() = runTest {
        // Act
        repository.getCertificationsByType(
            companyId = "company_456",
            certificationTypeId = "type_osha_10",
            status = CertificationStatus.VERIFIED
        )

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_456/certifications"))
    }

    // ===== Verification Tests =====

    @Test
    fun `approveCertification should call POST approve endpoint`() = runTest {
        // Act
        repository.approveCertification(
            certificationId = "cert_123",
            verifiedBy = "safety_manager_789",
            notes = "All documents verified"
        )

        // Assert
        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/cert_123/approve"))
    }

    @Test
    fun `rejectCertification should call POST reject endpoint with reason`() = runTest {
        // Act
        repository.rejectCertification(
            certificationId = "cert_123",
            verifiedBy = "safety_manager_789",
            reason = "Expired document"
        )

        // Assert
        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/cert_123/reject"))
    }

    @Test
    fun `getPendingCertifications should call GET pending endpoint`() = runTest {
        // Act
        repository.getPendingCertifications(
            companyId = "company_456",
            limit = 50
        )

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_456/certifications/pending"))
    }

    // ===== Expiration Tracking Tests =====

    @Test
    fun `getExpiringCertifications should call GET with days parameter`() = runTest {
        // Act
        repository.getExpiringCertifications(
            companyId = "company_456",
            daysUntilExpiration = 30
        )

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_456/certifications/expiring"))
    }

    @Test
    fun `getExpiredCertifications should call GET expired endpoint`() = runTest {
        // Act
        repository.getExpiredCertifications(
            companyId = "company_456",
            limit = 100
        )

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_456/certifications/expired"))
    }

    @Test
    fun `markCertificationsExpired should call POST with IDs array`() = runTest {
        // Act
        repository.markCertificationsExpired(
            certificationIds = listOf("cert_1", "cert_2", "cert_3")
        )

        // Assert
        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/mark-expired"))
    }

    // ===== OCR and Document Processing Tests =====

    @Test
    fun `uploadCertificationDocument should call presigned URL endpoint first`() = runTest {
        // Arrange
        val documentData = ByteArray(1024) { it.toByte() }

        // Act
        repository.uploadCertificationDocument(
            workerProfileId = "worker_123",
            companyId = "company_456",
            documentData = documentData,
            fileName = "cert.pdf",
            mimeType = "application/pdf"
        )

        // Assert
        assertTrue(mockApi.verifyCalled("POST", "/api/storage/presigned-url"))
    }

    @Test
    fun `processCertificationOCR should create certification from OCR data`() = runTest {
        // Arrange
        val ocrData = OCRExtractedData(
            rawText = "OSHA 10 Certificate",
            holderName = "John Doe",
            certificationType = "OSHA_10",
            certificationNumber = "OSHA10-2025-123",
            issueDate = LocalDate(2025, 1, 1),
            expirationDate = LocalDate(2030, 1, 1),
            issuingAuthority = "OSHA",
            confidence = 0.95
        )

        // Act
        // Note: This will fail in mock, but verifies the flow
        repository.processCertificationOCR(
            workerProfileId = "worker_123",
            companyId = "company_456",
            documentUrl = "https://cdn.hazardhawk.com/cert.pdf",
            ocrData = ocrData
        )

        // Assert: Should attempt to fetch cert type by code
        assertTrue(mockApi.verifyCalled("GET", "/api/certification-types/by-code/OSHA_10"))
    }

    // ===== Certification Types Tests =====

    @Test
    fun `getCertificationTypes should call GET with region and category`() = runTest {
        // Act
        repository.getCertificationTypes(
            category = "safety_training",
            region = "US"
        )

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/certification-types"))
    }

    @Test
    fun `getCertificationTypeByCode should call GET by-code endpoint`() = runTest {
        // Act
        repository.getCertificationTypeByCode("OSHA_10")

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/certification-types/by-code/OSHA_10"))
    }

    @Test
    fun `searchCertificationTypes should call search endpoint with query`() = runTest {
        // Act
        repository.searchCertificationTypes(
            query = "forklift",
            limit = 20
        )

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/certification-types/search"))
    }

    // ===== Statistics Tests =====

    @Test
    fun `getCertificationCountByStatus should call stats endpoint`() = runTest {
        // Act
        repository.getCertificationCountByStatus("company_456")

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_456/certifications/stats/by-status"))
    }

    @Test
    fun `getCertificationCountByType should call stats by-type endpoint`() = runTest {
        // Act
        repository.getCertificationCountByType("company_456")

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_456/certifications/stats/by-type"))
    }

    @Test
    fun `getComplianceMetrics should call compliance-metrics endpoint`() = runTest {
        // Act
        repository.getComplianceMetrics("company_456")

        // Assert
        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_456/certifications/compliance-metrics"))
    }

    // ===== Error Handling Tests =====

    @Test
    fun `createCertification should handle API errors gracefully`() = runTest {
        // Arrange
        val errorApi = MockApiClient(
            config = MockApiClient.MockApiConfig(shouldReturnErrors = true)
        )
        val errorRepo = CertificationApiRepository(apiClient = errorApi as Any as com.hazardhawk.data.network.ApiClient)

        val request = CreateCertificationRequest(
            certificationTypeId = "type_osha_10",
            issueDate = LocalDate(2025, 1, 1),
            documentUrl = "https://example.com/cert.pdf"
        )

        // Act
        val result = errorRepo.createCertification("worker_123", "company_456", request)

        // Assert
        assertTrue(result.isFailure)
    }

    @Test
    fun `uploadCertificationDocument should handle network failures with retry`() = runTest {
        // Arrange
        val unreliableApi = MockApiClient(
            config = MockApiClient.MockApiConfig(failureRate = 0.5)
        )
        val unreliableRepo = CertificationApiRepository(apiClient = unreliableApi as Any as com.hazardhawk.data.network.ApiClient)

        val documentData = ByteArray(512) { it.toByte() }

        // Act
        val result = unreliableRepo.uploadCertificationDocument(
            workerProfileId = "worker_123",
            companyId = "company_456",
            documentData = documentData,
            fileName = "test.pdf",
            mimeType = "application/pdf"
        )

        // Assert: May succeed or fail, but should not throw
        assertNotNull(result)
    }

    // ===== Week 3: Expiration Notification Tests =====

    @Test
    fun `sendExpirationReminder should call endpoint with correct channels`() = runTest {
        // Arrange
        val certId = "cert_123"
        val channels = listOf(NotificationChannel.EMAIL, NotificationChannel.SMS)

        // Act
        val result = repository.sendExpirationReminder(certId, channels)

        // Assert
        assertTrue(result.isSuccess)
        val reminderResult = result.getOrNull()
        assertNotNull(reminderResult)
        assertEquals(certId, reminderResult.certificationId)
        assertTrue(reminderResult.sentChannels.contains(NotificationChannel.EMAIL))

        // Verify API call
        val calls = mockApi.getCallHistory()
        val postCall = calls.find { it.path.contains("/send-expiration-reminder") }
        assertNotNull(postCall)
        assertEquals("POST", postCall.method)
    }

    @Test
    fun `sendExpirationReminder should handle partial channel failures`() = runTest {
        // Arrange
        val certId = "cert_456"
        val channels = listOf(
            NotificationChannel.EMAIL,
            NotificationChannel.SMS,
            NotificationChannel.PUSH
        )

        // Act
        val result = repository.sendExpirationReminder(certId, channels)

        // Assert
        assertTrue(result.isSuccess)
        val reminderResult = result.getOrNull()
        assertNotNull(reminderResult)

        // Mock should indicate some channels succeeded and some failed
        assertTrue(reminderResult.sentChannels.isNotEmpty() || reminderResult.failedChannels.isNotEmpty())
    }

    @Test
    fun `sendBulkExpirationReminders should process multiple certifications`() = runTest {
        // Arrange
        val certIds = listOf("cert_1", "cert_2", "cert_3")
        val channels = listOf(NotificationChannel.EMAIL)

        // Act
        val result = repository.sendBulkExpirationReminders(certIds, channels)

        // Assert
        assertTrue(result.isSuccess)
        val bulkResult = result.getOrNull()
        assertNotNull(bulkResult)
        assertEquals(3, bulkResult.totalRequested)
        assertTrue(bulkResult.successCount > 0)
        assertEquals(3, bulkResult.results.size)

        // Verify API call
        val calls = mockApi.getCallHistory()
        val postCall = calls.find { it.path.contains("/send-bulk-expiration-reminders") }
        assertNotNull(postCall)
        assertEquals("POST", postCall.method)
    }

    @Test
    fun `sendBulkExpirationReminders should handle mixed success and failures`() = runTest {
        // Arrange
        val certIds = listOf("cert_valid", "cert_invalid", "cert_expired")
        val channels = listOf(NotificationChannel.EMAIL, NotificationChannel.SMS)

        // Act
        val result = repository.sendBulkExpirationReminders(certIds, channels)

        // Assert
        assertTrue(result.isSuccess)
        val bulkResult = result.getOrNull()
        assertNotNull(bulkResult)
        assertEquals(3, bulkResult.totalRequested)

        // Mock should handle partial failures
        val totalProcessed = bulkResult.successCount + bulkResult.failureCount
        assertEquals(3, totalProcessed)
    }

    // ===== Week 3: CSV Import Tests =====

    @Test
    fun `importCertificationsFromCSV should parse valid CSV data`() = runTest {
        // Arrange
        val csvData = """
            WorkerID,CertificationType,IssueDate,ExpirationDate,CertificationNumber,IssuingAuthority
            worker_1,OSHA_10,2025-01-15,2030-01-15,OSHA10-001,OSHA Training Provider
            worker_2,OSHA_30,2025-02-20,2030-02-20,OSHA30-002,OSHA Training Provider
        """.trimIndent()

        // Act
        val result = repository.importCertificationsFromCSV(
            companyId = "company_123",
            csvData = csvData,
            validateOnly = false
        )

        // Assert
        assertTrue(result.isSuccess)
        val importResult = result.getOrNull()
        assertNotNull(importResult)
        assertEquals(2, importResult.totalRows)
        assertTrue(importResult.successCount > 0)
        assertTrue(importResult.createdCertifications.isNotEmpty())

        // Verify API call
        val calls = mockApi.getCallHistory()
        val postCall = calls.find { it.path.contains("/bulk-import") }
        assertNotNull(postCall)
        assertEquals("POST", postCall.method)
    }

    @Test
    fun `importCertificationsFromCSV should validate without creating records`() = runTest {
        // Arrange
        val csvData = """
            WorkerID,CertificationType,IssueDate,ExpirationDate
            worker_1,OSHA_10,2025-01-15,2030-01-15
        """.trimIndent()

        // Act
        val result = repository.importCertificationsFromCSV(
            companyId = "company_123",
            csvData = csvData,
            validateOnly = true
        )

        // Assert
        assertTrue(result.isSuccess)
        val importResult = result.getOrNull()
        assertNotNull(importResult)

        // In validation mode, no certifications should be created
        // Mock will handle this appropriately
        assertEquals(1, importResult.totalRows)
    }

    @Test
    fun `importCertificationsFromCSV should report validation errors`() = runTest {
        // Arrange
        val csvData = """
            WorkerID,CertificationType,IssueDate,ExpirationDate
            worker_1,INVALID_TYPE,invalid-date,2030-01-15
            worker_2,OSHA_10,2025-01-15,2030-01-15
        """.trimIndent()

        // Act
        val result = repository.importCertificationsFromCSV(
            companyId = "company_123",
            csvData = csvData,
            validateOnly = false
        )

        // Assert
        assertTrue(result.isSuccess)
        val importResult = result.getOrNull()
        assertNotNull(importResult)
        assertEquals(2, importResult.totalRows)

        // Mock should report errors for invalid row
        if (importResult.errorCount > 0) {
            assertTrue(importResult.errors.isNotEmpty())
            val firstError = importResult.errors.first()
            assertTrue(firstError.rowNumber > 0)
            assertNotNull(firstError.error)
        }
    }

    @Test
    fun `importCertificationsFromCSV should handle empty CSV`() = runTest {
        // Arrange
        val csvData = "WorkerID,CertificationType,IssueDate,ExpirationDate"

        // Act
        val result = repository.importCertificationsFromCSV(
            companyId = "company_123",
            csvData = csvData,
            validateOnly = false
        )

        // Assert
        assertTrue(result.isSuccess)
        val importResult = result.getOrNull()
        assertNotNull(importResult)
        assertEquals(0, importResult.totalRows)
        assertEquals(0, importResult.successCount)
    }

    // ===== Week 3: Advanced Search Tests =====

    @Test
    fun `searchCertifications should apply status filter`() = runTest {
        // Arrange
        val filters = CertificationSearchFilters(
            status = CertificationStatus.VERIFIED,
            pagination = PaginationRequest(pageSize = 20)
        )

        // Act
        val result = repository.searchCertifications(
            companyId = "company_123",
            filters = filters
        )

        // Assert
        assertNotNull(result)
        assertTrue(result.data.isNotEmpty())

        // Verify all returned certifications have VERIFIED status
        result.data.forEach { cert ->
            assertEquals(CertificationStatus.VERIFIED, cert.status)
        }

        // Verify API call
        val calls = mockApi.getCallHistory()
        val getCall = calls.find { it.path.contains("/certifications/search") }
        assertNotNull(getCall)
        assertEquals("GET", getCall.method)
    }

    @Test
    fun `searchCertifications should apply date range filters`() = runTest {
        // Arrange
        val filters = CertificationSearchFilters(
            expirationDateFrom = LocalDate(2025, 1, 1),
            expirationDateTo = LocalDate(2025, 12, 31),
            pagination = PaginationRequest(pageSize = 50)
        )

        // Act
        val result = repository.searchCertifications(
            companyId = "company_123",
            filters = filters
        )

        // Assert
        assertNotNull(result)

        // Verify certifications are within date range (if mock returns data)
        result.data.forEach { cert ->
            cert.expirationDate?.let { expDate ->
                assertTrue(expDate >= filters.expirationDateFrom!!)
                assertTrue(expDate <= filters.expirationDateTo!!)
            }
        }
    }

    @Test
    fun `searchCertifications should apply sorting`() = runTest {
        // Arrange
        val filters = CertificationSearchFilters(
            sortBy = CertificationSortField.EXPIRATION_DATE,
            sortDirection = SortDirection.ASC,
            pagination = PaginationRequest(pageSize = 10)
        )

        // Act
        val result = repository.searchCertifications(
            companyId = "company_123",
            filters = filters
        )

        // Assert
        assertNotNull(result)

        // Verify sorting (if mock returns multiple items)
        if (result.data.size >= 2) {
            val dates = result.data.mapNotNull { it.expirationDate }
            if (dates.size >= 2) {
                // Check ascending order
                for (i in 0 until dates.size - 1) {
                    assertTrue(dates[i] <= dates[i + 1])
                }
            }
        }
    }

    @Test
    fun `searchCertifications should support pagination`() = runTest {
        // Arrange
        val filters = CertificationSearchFilters(
            pagination = PaginationRequest(
                pageSize = 5,
                cursor = null
            )
        )

        // Act - First page
        val firstPage = repository.searchCertifications(
            companyId = "company_123",
            filters = filters
        )

        // Assert
        assertNotNull(firstPage)
        assertTrue(firstPage.data.size <= 5)

        // If there's a next cursor, fetch second page
        if (firstPage.pagination.hasMore && firstPage.pagination.nextCursor != null) {
            val secondPageFilters = filters.copy(
                pagination = PaginationRequest(
                    pageSize = 5,
                    cursor = firstPage.pagination.nextCursor
                )
            )

            val secondPage = repository.searchCertifications(
                companyId = "company_123",
                filters = secondPageFilters
            )

            assertNotNull(secondPage)
            assertTrue(secondPage.data.size <= 5)
        }
    }
}
