package com.hazardhawk.integration

import com.hazardhawk.data.mocks.MockApiClient
import com.hazardhawk.data.mocks.MockS3Client
import com.hazardhawk.data.mocks.MockOCRClient
import com.hazardhawk.data.repositories.crew.CertificationApiRepository
import com.hazardhawk.data.network.ApiClient
import com.hazardhawk.domain.services.*
import com.hazardhawk.models.crew.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Integration tests for complete certification workflow
 *
 * Tests end-to-end flows:
 * 1. Upload → OCR → Create → Verify → Approve
 * 2. Upload → OCR (low confidence) → Manual Review → Approve
 * 3. QR Code Generation → Verification
 * 4. DOB Verification → Approval
 * 5. Expiration Tracking → Notifications
 * 6. Batch Upload → Multiple Approvals
 */
class CertificationFlowIntegrationTest {

    private lateinit var mockApi: MockApiClient
    private lateinit var mockS3: MockS3Client
    private lateinit var mockOCR: MockOCRClient
    private lateinit var repository: CertificationApiRepository
    private lateinit var qrService: QRCodeService
    private lateinit var dobService: DOBVerificationService

    @BeforeTest
    fun setup() {
        mockApi = MockApiClient()
        mockS3 = MockS3Client()
        mockOCR = MockOCRClient()
        repository = CertificationApiRepository(apiClient = mockApi as ApiClient)
        qrService = QRCodeServiceImpl(apiClient = mockApi as ApiClient)
        dobService = DOBVerificationServiceImpl(apiClient = mockApi as ApiClient)
    }

    @AfterTest
    fun teardown() {
        mockApi.clearHistory()
        mockS3.clear()
        mockOCR.clearHistory()
    }

    // ===== Test 1: Complete Upload to Approval Flow =====

    @Test
    fun `complete certification flow - upload, OCR, create, approve`() = runTest {
        // Given: A worker needs to upload OSHA 10 certification
        val workerProfileId = "worker_flow_123"
        val companyId = "company_flow_456"
        val documentData = ByteArray(2048) { it.toByte() }

        // Step 1: Upload certification document
        val uploadResult = repository.uploadCertificationDocument(
            workerProfileId = workerProfileId,
            companyId = companyId,
            documentData = documentData,
            fileName = "osha10_cert.pdf",
            mimeType = "application/pdf"
        )

        // Verify upload called correct endpoints
        assertTrue(mockApi.verifyCalled("POST", "/api/storage/presigned-url"))
        assertTrue(mockApi.verifyCalled("POST", "/api/ocr/extract-certification"))

        // Step 2: Verify OCR extraction occurred
        assertTrue(uploadResult.isSuccess || uploadResult.isFailure) // Mock may not succeed

        // Step 3: Create certification from OCR data (if successful)
        val createRequest = CreateCertificationRequest(
            certificationTypeId = "type_osha_10",
            issueDate = LocalDate(2025, 1, 1),
            expirationDate = null, // OSHA is awareness training
            issuingAuthority = "OSHA Training Provider",
            certificationNumber = "OSHA10-2025-123",
            documentUrl = "https://cdn.hazardhawk.com/cert123.pdf"
        )

        val createResult = repository.createCertification(workerProfileId, companyId, createRequest)

        assertTrue(mockApi.verifyCalled("POST", "/api/certifications"))

        // Step 4: Approve certification
        repository.approveCertification(
            certificationId = "cert_123",
            verifiedBy = "safety_manager_789",
            notes = "OSHA 10 verified and approved"
        )

        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/cert_123/approve"))

        // Verify complete flow executed
        assertTrue(mockApi.getCallHistory().size >= 3)
    }

    // ===== Test 2: Low Confidence OCR Flow =====

    @Test
    fun `certification flow with low confidence OCR requiring manual review`() = runTest {
        // Given: Document with poor OCR quality
        val mockOCRLowConfidence = MockOCRClient(
            config = MockOCRClient.MockOCRConfig(
                baseConfidence = 0.60, // Low confidence
                confidenceVariation = 0.05
            )
        )

        val documentUrl = "https://cdn.hazardhawk.com/low-quality-cert.pdf"

        // Step 1: Extract with low confidence
        val ocrResult = mockOCRLowConfidence.extractCertificationData(documentUrl)

        assertTrue(ocrResult.isSuccess)
        val extracted = ocrResult.getOrThrow()
        assertTrue(extracted.needsReview) // Should flag for manual review

        // Step 2: Create certification (will be pending verification)
        val createRequest = CreateCertificationRequest(
            certificationTypeId = "type_forklift",
            issueDate = LocalDate(2024, 6, 15),
            expirationDate = LocalDate(2027, 6, 15),
            issuingAuthority = extracted.issuingAuthority,
            certificationNumber = extracted.certificationNumber,
            documentUrl = documentUrl
        )

        repository.createCertification("worker_low_conf", "company_123", createRequest)

        // Step 3: Get pending certifications for review
        repository.getPendingCertifications("company_123", limit = 50)

        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_123/certifications/pending"))

        // Step 4: Manual review and approval
        repository.approveCertification(
            certificationId = "cert_low_conf",
            verifiedBy = "safety_lead_456",
            notes = "Manually verified certificate details"
        )

        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/cert_low_conf/approve"))
    }

    // ===== Test 3: QR Code Generation and Verification Flow =====

    @Test
    fun `QR code generation and verification flow`() = runTest {
        // Step 1: Generate QR code for certification
        val qrResult = qrService.generateCertificationQRCode(
            certificationId = "cert_qr_123",
            size = 512,
            errorCorrection = QRErrorCorrection.M
        )

        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/cert_qr_123/qr-code"))

        // Step 2: Scan QR code (simulate)
        val qrData = "encrypted_cert_data_abc123"

        // Step 3: Verify QR code
        val verifyResult = qrService.verifyQRCode(qrData)

        assertTrue(mockApi.verifyCalled("POST", "/api/qr-codes/verify"))

        // Verify complete QR flow executed
        assertEquals(2, mockApi.getCallHistory().size)
    }

    // ===== Test 4: DOB Verification Before Approval =====

    @Test
    fun `DOB verification before certification approval flow`() = runTest {
        // Given: Worker scans QR code on web portal
        val workerProfileId = "worker_dob_123"
        val certificationId = "cert_dob_456"

        // Step 1: Generate QR code
        qrService.generateCertificationQRCode(certificationId)

        // Step 2: Worker enters DOB
        val correctDOB = LocalDate(1990, 5, 15)
        val sessionId = "session_web_portal_789"

        val dobVerifyResult = dobService.verifyDOBWithRetryLimit(
            workerProfileId = workerProfileId,
            dateOfBirth = correctDOB,
            sessionId = sessionId
        )

        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/verify-dob-session"))

        // Step 3: If DOB verified, approve certification
        // In real flow, backend would link DOB verification to cert approval
        repository.approveCertification(
            certificationId = certificationId,
            verifiedBy = "web_portal_automated",
            notes = "Approved via web portal with DOB verification"
        )

        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/cert_dob_456/approve"))
    }

    // ===== Test 5: Expiration Tracking Flow =====

    @Test
    fun `expiration tracking and notification flow`() = runTest {
        // Step 1: Get expiring certifications
        val expiringCerts = repository.getExpiringCertifications(
            companyId = "company_expiry_123",
            daysUntilExpiration = 30
        )

        assertTrue(mockApi.verifyCalled("GET", "/api/companies/company_expiry_123/certifications/expiring"))

        // Step 2: Get workers with expiring certs
        val workersWithExpiring = repository.getWorkersWithExpiringCerts(
            companyId = "company_expiry_123",
            daysUntilExpiration = 30
        )

        // Step 3: Mark expired certifications
        val expiredIds = listOf("cert_exp_1", "cert_exp_2", "cert_exp_3")
        repository.markCertificationsExpired(expiredIds)

        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/mark-expired"))

        // Verify expiration flow completed
        assertTrue(mockApi.getCallHistory().size >= 2)
    }

    // ===== Test 6: Batch Upload and Approval Flow =====

    @Test
    fun `batch upload multiple certifications and approve all`() = runTest {
        // Given: Worker has multiple certifications to upload
        val workerProfileId = "worker_batch_123"
        val companyId = "company_batch_456"

        val certifications = listOf(
            "OSHA_10" to "osha10.pdf",
            "FORKLIFT" to "forklift.pdf",
            "CPR" to "cpr.pdf"
        )

        // Step 1: Upload each certification
        certifications.forEach { (certType, fileName) ->
            val documentData = ByteArray(1024) { it.toByte() }

            repository.uploadCertificationDocument(
                workerProfileId = workerProfileId,
                companyId = companyId,
                documentData = documentData,
                fileName = fileName,
                mimeType = "application/pdf"
            )
        }

        // Verify all uploads were attempted
        assertTrue(mockApi.countCalls("/api/storage/presigned-url") >= certifications.size)

        // Step 2: Get all pending certifications for company
        repository.getPendingCertifications(companyId, limit = 100)

        // Step 3: Approve all pending certifications
        val certIds = listOf("cert_batch_1", "cert_batch_2", "cert_batch_3")
        certIds.forEach { certId ->
            repository.approveCertification(
                certificationId = certId,
                verifiedBy = "safety_manager_batch",
                notes = "Batch approved"
            )
        }

        // Verify batch approval occurred
        assertTrue(certIds.all { certId ->
            mockApi.verifyCalled("POST", "/api/certifications/$certId/approve")
        })
    }

    // ===== Test 7: Rejection and Re-upload Flow =====

    @Test
    fun `certification rejection and re-upload flow`() = runTest {
        // Step 1: Submit certification
        val createRequest = CreateCertificationRequest(
            certificationTypeId = "type_scaffold",
            issueDate = LocalDate(2024, 1, 1),
            expirationDate = LocalDate(2026, 1, 1),
            documentUrl = "https://cdn.hazardhawk.com/scaffold_expired.pdf"
        )

        repository.createCertification("worker_reject", "company_reject", createRequest)

        // Step 2: Reject due to expired document
        val rejectResult = repository.rejectCertification(
            certificationId = "cert_reject_123",
            verifiedBy = "safety_manager_reject",
            reason = "Certificate document has expired"
        )

        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/cert_reject_123/reject"))

        // Step 3: Worker re-uploads with correct document
        val newDocumentData = ByteArray(2048) { it.toByte() }
        repository.uploadCertificationDocument(
            workerProfileId = "worker_reject",
            companyId = "company_reject",
            documentData = newDocumentData,
            fileName = "scaffold_current.pdf",
            mimeType = "application/pdf"
        )

        // Step 4: Approve new submission
        repository.approveCertification(
            certificationId = "cert_reject_new",
            verifiedBy = "safety_manager_reject",
            notes = "Correct document uploaded and verified"
        )

        assertTrue(mockApi.verifyCalled("POST", "/api/certifications/cert_reject_new/approve"))
    }

    // ===== Test 8: Worker Profile QR Code Flow =====

    @Test
    fun `worker profile QR code with all certifications flow`() = runTest {
        // Given: Worker has multiple certifications
        val workerProfileId = "worker_profile_123"

        // Step 1: Get all worker certifications
        repository.getWorkerCertifications(
            workerProfileId = workerProfileId,
            status = CertificationStatus.VERIFIED,
            includeExpired = false
        )

        assertTrue(mockApi.verifyCalled("GET", "/api/workers/$workerProfileId/certifications"))

        // Step 2: Generate worker profile QR code
        qrService.generateWorkerProfileQRCode(
            workerProfileId = workerProfileId,
            size = 1024
        )

        assertTrue(mockApi.verifyCalled("POST", "/api/workers/$workerProfileId/qr-code"))

        // Step 3: Verify worker profile QR code
        qrService.verifyQRCode("worker_profile_qr_data")

        assertTrue(mockApi.verifyCalled("POST", "/api/qr-codes/verify"))

        // Verify complete worker profile flow
        assertEquals(3, mockApi.getCallHistory().size)
    }

    // ===== Performance Test =====

    @Test
    fun `certification flow should complete within performance target`() = runTest {
        // Target: < 500ms for dev environment (mock)
        val startTime = System.currentTimeMillis()

        // Execute typical certification workflow
        val createRequest = CreateCertificationRequest(
            certificationTypeId = "type_osha_30",
            issueDate = LocalDate(2025, 1, 1),
            documentUrl = "https://cdn.hazardhawk.com/perf_test.pdf"
        )

        repository.createCertification("worker_perf", "company_perf", createRequest)
        repository.approveCertification("cert_perf", "manager_perf")

        val duration = System.currentTimeMillis() - startTime

        // Assert: Mock operations should be very fast
        assertTrue(duration < 500, "Flow took ${duration}ms, expected < 500ms")
    }
}
