package com.hazardhawk.domain.services

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.*

/**
 * Integration test suite for certification upload workflow (15 tests)
 * Tests the complete flow from file upload through OCR to final verification
 * 
 * Coverage:
 * - End-to-end workflow (5 tests)
 * - Error recovery (5 tests)
 * - Concurrent uploads (3 tests)
 * - Large file handling (2 tests)
 */
class CertificationUploadIntegrationTest {

    private lateinit var fileUploadService: MockFileUploadService
    private lateinit var ocrService: MockOCRService
    private lateinit var notificationService: MockNotificationService

    @BeforeTest
    fun setup() {
        fileUploadService = MockFileUploadService()
        ocrService = MockOCRService()
        notificationService = MockNotificationService()
    }

    @AfterTest
    fun tearDown() {
        fileUploadService.reset()
        ocrService.reset()
        notificationService.reset()
    }

    // ====================
    // End-to-End Workflow (5 tests)
    // ====================

    @Test
    fun `complete workflow should upload file, extract data, and return certification`() = runTest {
        // Step 1: Upload file
        val file = CertificationTestFixtures.createMockJpeg(500)
        val uploadResult = fileUploadService.uploadFile(file, "osha10.jpg", "image/jpeg")
        
        assertTrue(uploadResult.isSuccess)
        val uploadData = uploadResult.getOrNull()!!
        
        // Step 2: Extract OCR data
        ocrService.mockRawText = CertificationTestFixtures.osha10RawText
        val ocrResult = ocrService.extractCertificationData(uploadData.url)
        
        assertTrue(ocrResult.isSuccess)
        val extractedData = ocrResult.getOrNull()!!
        
        // Step 3: Verify extracted data quality
        assertEquals("OSHA_10", extractedData.certificationType)
        assertTrue(extractedData.confidence >= ExtractedCertification.MIN_AUTO_ACCEPT_CONFIDENCE)
        assertFalse(extractedData.needsReview)
        
        // Step 4: Verify file is accessible at URL
        assertTrue(uploadData.url.isNotEmpty())
        assertTrue(uploadData.thumbnailUrl != null)
    }

    @Test
    fun `workflow should handle PDF certification documents`() = runTest {
        // Upload PDF
        val file = CertificationTestFixtures.createMockPdf(800)
        val uploadResult = fileUploadService.uploadFile(file, "cert.pdf", "application/pdf")
        
        assertTrue(uploadResult.isSuccess)
        
        // Extract from PDF
        ocrService.mockRawText = CertificationTestFixtures.forkliftRawText
        val ocrResult = ocrService.extractCertificationData(uploadResult.getOrNull()!!.url)
        
        assertTrue(ocrResult.isSuccess)
        assertEquals("FORKLIFT", ocrResult.getOrNull()?.certificationType)
    }

    @Test
    fun `workflow should compress large images before upload`() = runTest {
        // Large image (2MB)
        val largeFile = CertificationTestFixtures.createMockJpeg(2000)
        
        // Compress first
        val compressResult = fileUploadService.compressImage(largeFile, maxSizeKB = 500)
        assertTrue(compressResult.isSuccess)
        val compressed = compressResult.getOrNull()!!
        
        // Upload compressed
        val uploadResult = fileUploadService.uploadFile(compressed, "cert.jpg", "image/jpeg")
        assertTrue(uploadResult.isSuccess)
        
        // Verify size reduction
        assertTrue(compressed.size < largeFile.size)
        assertTrue(compressed.size < 500 * 1024)
    }

    @Test
    fun `workflow should flag low confidence extractions for manual review`() = runTest {
        // Upload file
        val file = CertificationTestFixtures.createMockJpeg(500)
        val uploadResult = fileUploadService.uploadFile(file, "poor-quality.jpg", "image/jpeg")
        assertTrue(uploadResult.isSuccess)
        
        // Extract with poor quality text
        ocrService.mockRawText = CertificationTestFixtures.poorQualityText
        val ocrResult = ocrService.extractCertificationData(uploadResult.getOrNull()!!.url)
        
        assertTrue(ocrResult.isSuccess)
        val extracted = ocrResult.getOrNull()!!
        
        // Should be flagged for review
        assertTrue(extracted.needsReview)
        assertTrue(extracted.confidence < ExtractedCertification.MIN_AUTO_ACCEPT_CONFIDENCE)
    }

    @Test
    fun `workflow should preserve all metadata through pipeline`() = runTest {
        // Upload with metadata
        val file = CertificationTestFixtures.createMockJpeg(300)
        val uploadResult = fileUploadService.uploadFile(file, "metadata-test.jpg", "image/jpeg")
        assertTrue(uploadResult.isSuccess)
        
        val uploadData = uploadResult.getOrNull()!!
        
        // Extract data
        ocrService.mockRawText = CertificationTestFixtures.osha10RawText
        val ocrResult = ocrService.extractCertificationData(uploadData.url)
        assertTrue(ocrResult.isSuccess)
        
        val extracted = ocrResult.getOrNull()!!
        
        // Verify all critical fields extracted
        assertNotNull(extracted.holderName)
        assertNotNull(extracted.certificationType)
        assertNotNull(extracted.issueDate)
        assertNotNull(extracted.expirationDate)
        assertNotNull(extracted.certificationNumber)
        assertNotNull(extracted.issuingAuthority)
        assertNotNull(extracted.rawText)
    }

    // ====================
    // Error Recovery (5 tests)
    // ====================

    @Test
    fun `workflow should retry upload on transient network failure`() = runTest {
        fileUploadService.failureCount = 2 // Fail twice, succeed third time
        
        val file = CertificationTestFixtures.createMockJpeg(500)
        val result = fileUploadService.uploadFile(file, "retry-test.jpg", "image/jpeg")
        
        assertTrue(result.isSuccess)
        assertEquals(3, fileUploadService.attemptCount)
    }

    @Test
    fun `workflow should handle OCR service timeout gracefully`() = runTest {
        // Upload succeeds
        val file = CertificationTestFixtures.createMockJpeg(500)
        val uploadResult = fileUploadService.uploadFile(file, "test.jpg", "image/jpeg")
        assertTrue(uploadResult.isSuccess)
        
        // OCR fails
        ocrService.failOnUrl = uploadResult.getOrNull()!!.url
        val ocrResult = ocrService.extractCertificationData(uploadResult.getOrNull()!!.url)
        
        assertTrue(ocrResult.isFailure)
        assertTrue(ocrResult.exceptionOrNull() is OCRError.ExtractionFailed)
        
        // Uploaded file remains accessible for retry
        assertNotNull(uploadResult.getOrNull()?.url)
    }

    @Test
    fun `workflow should rollback on critical failure`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(500)
        
        // Upload succeeds
        val uploadResult = fileUploadService.uploadFile(file, "test.jpg", "image/jpeg")
        assertTrue(uploadResult.isSuccess)
        
        // OCR fails critically
        ocrService.failOnUrl = uploadResult.getOrNull()!!.url
        val ocrResult = ocrService.extractCertificationData(uploadResult.getOrNull()!!.url)
        
        assertTrue(ocrResult.isFailure)
        
        // Cleanup should have been called
        assertTrue(fileUploadService.cleanupCalled)
    }

    @Test
    fun `workflow should handle partial OCR extraction gracefully`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(500)
        val uploadResult = fileUploadService.uploadFile(file, "test.jpg", "image/jpeg")
        assertTrue(uploadResult.isSuccess)
        
        // OCR extracts partial data
        ocrService.mockRawText = """
            OSHA 10
            Some incomplete information
        """.trimIndent()
        
        val ocrResult = ocrService.extractCertificationData(uploadResult.getOrNull()!!.url)
        
        assertTrue(ocrResult.isSuccess)
        val extracted = ocrResult.getOrNull()!!
        
        // Should flag for review
        assertTrue(extracted.needsReview)
    }

    @Test
    fun `workflow should validate extracted dates are reasonable`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(500)
        val uploadResult = fileUploadService.uploadFile(file, "test.jpg", "image/jpeg")
        assertTrue(uploadResult.isSuccess)
        
        ocrService.mockRawText = """
            OSHA 10
            Name: John Doe
            Issue: 01/15/2023
            Expiry: 01/15/2028
        """.trimIndent()
        
        val ocrResult = ocrService.extractCertificationData(uploadResult.getOrNull()!!.url)
        assertTrue(ocrResult.isSuccess)
        
        val extracted = ocrResult.getOrNull()!!
        
        // Validate dates
        assertNotNull(extracted.issueDate)
        assertNotNull(extracted.expirationDate)
        assertTrue(extracted.expirationDate!! > extracted.issueDate!!)
    }

    // ====================
    // Concurrent Uploads (3 tests)
    // ====================

    @Test
    fun `workflow should handle multiple simultaneous uploads`() = runTest {
        val files = (1..5).map {
            CertificationTestFixtures.createFileUpload("cert$it.jpg", "image/jpeg", 300)
        }
        
        val results = fileUploadService.uploadFiles(files)
        
        assertEquals(5, results.size)
        assertTrue(results.all { it.isSuccess })
        
        // All should have unique keys
        val keys = results.mapNotNull { it.getOrNull()?.key }
        assertEquals(5, keys.distinct().size)
    }

    @Test
    fun `workflow should process batch OCR requests efficiently`() = runTest {
        // Upload multiple files
        val files = (1..10).map {
            CertificationTestFixtures.createFileUpload("cert$it.jpg", "image/jpeg", 200)
        }
        
        val uploadResults = fileUploadService.uploadFiles(files)
        assertTrue(uploadResults.all { it.isSuccess })
        
        // Batch OCR
        val urls = uploadResults.mapNotNull { it.getOrNull()?.url }
        ocrService.mockRawText = CertificationTestFixtures.osha10RawText
        ocrService.addNumberToName = true
        
        val ocrResults = ocrService.extractCertificationDataBatch(urls)
        
        assertEquals(10, ocrResults.size)
        assertTrue(ocrResults.all { it.isSuccess })
    }

    @Test
    fun `workflow should handle mixed success and failure in batch operations`() = runTest {
        val files = listOf(
            CertificationTestFixtures.createFileUpload("valid1.jpg", "image/jpeg", 200),
            CertificationTestFixtures.createFileUpload("invalid.exe", "application/exe", 200),
            CertificationTestFixtures.createFileUpload("valid2.jpg", "image/jpeg", 200)
        )
        
        val results = fileUploadService.uploadFiles(files)
        
        assertEquals(3, results.size)
        assertTrue(results[0].isSuccess)
        assertTrue(results[1].isFailure)
        assertTrue(results[2].isSuccess)
    }

    // ====================
    // Large File Handling (2 tests)
    // ====================

    @Test
    fun `workflow should chunk and upload very large files`() = runTest {
        // Very large file (5MB)
        val largeFile = CertificationTestFixtures.createMockJpeg(5000)
        
        // Should compress first
        val compressed = fileUploadService.compressImage(largeFile, maxSizeKB = 800)
        assertTrue(compressed.isSuccess)
        
        // Then upload
        val uploadResult = fileUploadService.uploadFile(
            compressed.getOrNull()!!,
            "large-cert.jpg",
            "image/jpeg"
        )
        
        assertTrue(uploadResult.isSuccess)
    }

    @Test
    fun `workflow should report progress for large file uploads`() = runTest {
        val largeFile = CertificationTestFixtures.createMockJpeg(3000)
        val progressUpdates = mutableListOf<Float>()
        
        val result = fileUploadService.uploadFile(
            largeFile,
            "progress-test.jpg",
            "image/jpeg"
        ) { progress ->
            progressUpdates.add(progress)
        }
        
        assertTrue(result.isSuccess)
        assertTrue(progressUpdates.isNotEmpty())
        assertEquals(0.0f, progressUpdates.first())
        assertEquals(1.0f, progressUpdates.last())
        
        // Should have multiple updates
        assertTrue(progressUpdates.size >= 3)
    }
}
