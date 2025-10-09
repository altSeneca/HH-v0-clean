package com.hazardhawk.domain.services

import com.hazardhawk.data.storage.S3Client
import com.hazardhawk.domain.fixtures.CertificationTestFixtures
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit tests for FileUploadService.
 * Tests upload success scenarios, retry logic, image compression, 
 * thumbnail generation, progress tracking, and error handling.
 *
 * Total: 30 tests
 */
class FileUploadServiceTest {
    
    private lateinit var mockS3Client: MockS3Client
    private lateinit var service: FileUploadServiceImpl
    
    @BeforeTest
    fun setup() {
        mockS3Client = MockS3Client()
        service = FileUploadServiceImpl(
            s3Client = mockS3Client,
            bucket = "test-bucket",
            cdnBaseUrl = "https://cdn.test.com"
        )
    }
    
    // ===== Upload Success Scenarios (10 tests) =====
    
    @Test
    fun `uploadFile should successfully upload PDF`() = runTest {
        // Given
        val pdfData = CertificationTestFixtures.createSamplePdfData(100)
        val fileName = "cert.pdf"
        
        // When
        val result = service.uploadFile(
            file = pdfData,
            fileName = fileName,
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        val uploadResult = result.getOrThrow()
        assertTrue(uploadResult.url.contains("cdn.test.com"))
        assertEquals(pdfData.size.toLong(), uploadResult.sizeBytes)
        assertNull(uploadResult.thumbnailUrl) // PDFs don't get thumbnails
    }
    
    @Test
    fun `uploadFile should successfully upload image with thumbnail`() = runTest {
        // Given
        val imageData = CertificationTestFixtures.createSampleImageData(200)
        val fileName = "cert.jpg"
        
        // When
        val result = service.uploadFile(
            file = imageData,
            fileName = fileName,
            contentType = "image/jpeg"
        )
        
        // Then
        assertTrue(result.isSuccess)
        val uploadResult = result.getOrThrow()
        assertTrue(uploadResult.url.contains("cdn.test.com"))
        assertNotNull(uploadResult.thumbnailUrl)
        assertTrue(uploadResult.thumbnailUrl!!.contains("thumbnails"))
    }
    
    @Test
    fun `uploadFile should sanitize filename with special characters`() = runTest {
        // Given
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        val fileName = "My Cert (2024) #1.pdf"
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = fileName,
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        val uploadedKey = mockS3Client.lastUploadedKey!!
        assertFalse(uploadedKey.contains("("))
        assertFalse(uploadedKey.contains(")"))
        assertFalse(uploadedKey.contains("#"))
        assertFalse(uploadedKey.contains(" "))
    }
    
    @Test
    fun `uploadFile should include timestamp in S3 key`() = runTest {
        // Given
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        val uploadedKey = mockS3Client.lastUploadedKey!!
        assertTrue(uploadedKey.startsWith("certifications/"))
        assertTrue(uploadedKey.contains("-")) // Timestamp separator
    }
    
    @Test
    fun `uploadFile should upload to correct S3 prefix`() = runTest {
        // Given
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        val uploadedKey = mockS3Client.lastUploadedKey!!
        assertTrue(uploadedKey.startsWith("certifications/"))
    }
    
    @Test
    fun `uploadFile should handle PNG images`() = runTest {
        // Given
        val pngData = ByteArray(100 * 1024) { 0 }
        
        // When
        val result = service.uploadFile(
            file = pngData,
            fileName = "cert.png",
            contentType = "image/png"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrThrow().thumbnailUrl)
    }
    
    @Test
    fun `uploadFile should handle JPEG images`() = runTest {
        // Given
        val jpegData = CertificationTestFixtures.createSampleImageData(150)
        
        // When
        val result = service.uploadFile(
            file = jpegData,
            fileName = "cert.jpeg",
            contentType = "image/jpeg"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrThrow().thumbnailUrl)
    }
    
    @Test
    fun `uploadFile should convert S3 URL to CDN URL`() = runTest {
        // Given
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        mockS3Client.mockUrl = "https://s3.amazonaws.com/test-bucket/certifications/file.pdf"
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        val url = result.getOrThrow().url
        assertTrue(url.startsWith("https://cdn.test.com"))
        assertFalse(url.contains("s3.amazonaws.com"))
    }
    
    @Test
    fun `uploadFile should use S3 URL when CDN not configured`() = runTest {
        // Given
        val serviceNoCDN = FileUploadServiceImpl(
            s3Client = mockS3Client,
            bucket = "test-bucket",
            cdnBaseUrl = null
        )
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = serviceNoCDN.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        val url = result.getOrThrow().url
        assertTrue(url.contains("s3.amazonaws.com"))
    }
    
    @Test
    fun `uploadFile should return correct file size`() = runTest {
        // Given
        val fileData = CertificationTestFixtures.createSamplePdfData(250)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(fileData.size.toLong(), result.getOrThrow().sizeBytes)
    }
    
    // ===== Retry Logic (5 tests) =====
    
    @Test
    fun `uploadFile should retry on first failure`() = runTest {
        // Given
        mockS3Client.failureCount = 1
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(2, mockS3Client.uploadAttempts) // Initial + 1 retry
    }
    
    @Test
    fun `uploadFile should retry on two failures`() = runTest {
        // Given
        mockS3Client.failureCount = 2
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(3, mockS3Client.uploadAttempts) // Initial + 2 retries
    }
    
    @Test
    fun `uploadFile should fail after 3 attempts`() = runTest {
        // Given
        mockS3Client.failureCount = 3
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isFailure)
        assertEquals(3, mockS3Client.uploadAttempts)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("3 attempts"))
    }
    
    @Test
    fun `uploadFile should handle network timeout on retry`() = runTest {
        // Given
        mockS3Client.shouldTimeout = true
        mockS3Client.failureCount = 1
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess) // Succeeds on second attempt
        assertEquals(2, mockS3Client.uploadAttempts)
    }
    
    @Test
    fun `uploadFile should use exponential backoff between retries`() = runTest {
        // Given
        mockS3Client.failureCount = 2
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        val startTime = kotlinx.datetime.Clock.System.now()
        
        // When
        service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        val endTime = kotlinx.datetime.Clock.System.now()
        val durationMs = (endTime - startTime).inWholeMilliseconds
        
        // Then
        // Should have delays: 1000ms + 2000ms = 3000ms minimum
        assertTrue(durationMs >= 3000, "Expected at least 3000ms, got ${durationMs}ms")
    }
    
    // ===== Image Compression (5 tests) =====
    
    @Test
    fun `compressImage should return original if already small enough`() = runTest {
        // Given
        val smallImage = ByteArray(100 * 1024) // 100KB
        
        // When
        val result = service.compressImage(smallImage, maxSizeKB = 500)
        
        // Then
        assertTrue(result.isSuccess)
        assertContentEquals(smallImage, result.getOrThrow())
    }
    
    @Test
    fun `compressImage should fail for unsupported platform compression`() = runTest {
        // Given
        val largeImage = ByteArray(600 * 1024) // 600KB
        
        // When
        val result = service.compressImage(largeImage, maxSizeKB = 500)
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is UnsupportedOperationException)
    }
    
    @Test
    fun `compressImage should report current and target sizes in error`() = runTest {
        // Given
        val largeImage = ByteArray(750 * 1024) // 750KB
        
        // When
        val result = service.compressImage(largeImage, maxSizeKB = 500)
        
        // Then
        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()!!.message!!
        assertTrue(message.contains("750KB"))
        assertTrue(message.contains("500KB"))
    }
    
    @Test
    fun `compressImage should handle custom max size`() = runTest {
        // Given
        val image = ByteArray(150 * 1024) // 150KB
        
        // When
        val result = service.compressImage(image, maxSizeKB = 200)
        
        // Then
        assertTrue(result.isSuccess)
        assertContentEquals(image, result.getOrThrow())
    }
    
    @Test
    fun `compressImage should handle very small images`() = runTest {
        // Given
        val tinyImage = ByteArray(10 * 1024) // 10KB
        
        // When
        val result = service.compressImage(tinyImage, maxSizeKB = 500)
        
        // Then
        assertTrue(result.isSuccess)
        assertContentEquals(tinyImage, result.getOrThrow())
    }
    
    // ===== Progress Tracking (5 tests) =====
    
    @Test
    fun `uploadFile should report progress for main file`() = runTest {
        // Given
        val fileData = CertificationTestFixtures.createSamplePdfData(100)
        val progressValues = mutableListOf<Float>()
        
        // When
        service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf",
            onProgress = { progressValues.add(it) }
        )
        
        // Then
        assertTrue(progressValues.isNotEmpty())
        assertTrue(progressValues.last() == 1.0f)
        assertTrue(progressValues.all { it in 0.0f..1.0f })
    }
    
    @Test
    fun `uploadFile should allocate 80 percent progress to main file`() = runTest {
        // Given
        val imageData = CertificationTestFixtures.createSampleImageData(100)
        val progressValues = mutableListOf<Float>()
        
        // When
        service.uploadFile(
            file = imageData,
            fileName = "cert.jpg",
            contentType = "image/jpeg",
            onProgress = { progressValues.add(it) }
        )
        
        // Then
        // Main file progress should reach 0.8
        assertTrue(progressValues.any { it >= 0.79f && it <= 0.81f })
    }
    
    @Test
    fun `uploadFile should report 100 percent when complete`() = runTest {
        // Given
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        var finalProgress = 0.0f
        
        // When
        service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf",
            onProgress = { finalProgress = it }
        )
        
        // Then
        assertEquals(1.0f, finalProgress)
    }
    
    @Test
    fun `uploadFile should report progress incrementally`() = runTest {
        // Given
        val fileData = CertificationTestFixtures.createSamplePdfData(200)
        val progressValues = mutableListOf<Float>()
        
        // When
        service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf",
            onProgress = { progressValues.add(it) }
        )
        
        // Then
        assertTrue(progressValues.size > 1)
        // Verify progress is monotonically increasing
        for (i in 1 until progressValues.size) {
            assertTrue(progressValues[i] >= progressValues[i - 1])
        }
    }
    
    @Test
    fun `uploadFile should handle progress callback exceptions gracefully`() = runTest {
        // Given
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf",
            onProgress = { throw RuntimeException("Progress callback error") }
        )
        
        // Then
        // Upload should still succeed even if progress callback throws
        assertTrue(result.isSuccess)
    }
    
    // ===== Error Handling (5 tests) =====
    
    @Test
    fun `uploadFile should handle S3 client exceptions`() = runTest {
        // Given
        mockS3Client.shouldThrowException = true
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileUploadException)
    }
    
    @Test
    fun `uploadFile should handle empty file`() = runTest {
        // Given
        val emptyFile = ByteArray(0)
        
        // When
        val result = service.uploadFile(
            file = emptyFile,
            fileName = "empty.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(0L, result.getOrThrow().sizeBytes)
    }
    
    @Test
    fun `uploadFile should handle very large files`() = runTest {
        // Given
        val largeFile = ByteArray(10 * 1024 * 1024) // 10MB
        
        // When
        val result = service.uploadFile(
            file = largeFile,
            fileName = "large.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(largeFile.size.toLong(), result.getOrThrow().sizeBytes)
    }
    
    @Test
    fun `uploadFile should wrap exceptions in FileUploadException`() = runTest {
        // Given
        mockS3Client.shouldThrowException = true
        val fileData = CertificationTestFixtures.createSamplePdfData(50)
        
        // When
        val result = service.uploadFile(
            file = fileData,
            fileName = "cert.pdf",
            contentType = "application/pdf"
        )
        
        // Then
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is FileUploadException)
        assertTrue(exception.message!!.contains("Failed to upload file"))
    }
    
    @Test
    fun `uploadFile should handle thumbnail generation failure gracefully`() = runTest {
        // Given
        mockS3Client.shouldFailThumbnailUpload = true
        val imageData = CertificationTestFixtures.createSampleImageData(100)
        
        // When
        val result = service.uploadFile(
            file = imageData,
            fileName = "cert.jpg",
            contentType = "image/jpeg"
        )
        
        // Then
        // Main upload should still succeed
        assertTrue(result.isSuccess)
        // But thumbnail URL might be empty or null
        val uploadResult = result.getOrThrow()
        assertTrue(uploadResult.thumbnailUrl == null || uploadResult.thumbnailUrl.isEmpty())
    }
}

/**
 * Mock S3 client for testing file upload logic.
 */
class MockS3Client : S3Client {
    var mockUrl = "https://s3.amazonaws.com/test-bucket/certifications/file.pdf"
    var failureCount = 0
    var uploadAttempts = 0
    var shouldTimeout = false
    var shouldThrowException = false
    var shouldFailThumbnailUpload = false
    var lastUploadedKey: String? = null
    
    private var currentAttempt = 0
    
    override suspend fun uploadFile(
        bucket: String,
        key: String,
        data: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<String> {
        uploadAttempts++
        currentAttempt++
        lastUploadedKey = key
        
        // Simulate progress
        try {
            onProgress(0.0f)
            onProgress(0.5f)
            onProgress(1.0f)
        } catch (e: Exception) {
            // Ignore progress callback exceptions
        }
        
        if (shouldThrowException) {
            return Result.failure(Exception("S3 upload failed"))
        }
        
        if (shouldTimeout && currentAttempt == 1) {
            return Result.failure(Exception("Network timeout"))
        }
        
        if (shouldFailThumbnailUpload && key.contains("thumbnails/")) {
            return Result.failure(Exception("Thumbnail upload failed"))
        }
        
        if (currentAttempt <= failureCount) {
            return Result.failure(Exception("S3 upload failed (attempt $currentAttempt)"))
        }
        
        return Result.success("$mockUrl")
    }
    
    override suspend fun generatePresignedUploadUrl(
        bucket: String,
        key: String,
        contentType: String,
        expirationSeconds: Int
    ): Result<String> {
        return Result.success("https://s3.amazonaws.com/presigned-url")
    }
    
    override suspend fun deleteFile(bucket: String, key: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun fileExists(bucket: String, key: String): Result<Boolean> {
        return Result.success(true)
    }
}
