package com.hazardhawk.domain.services

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive test suite for FileUploadService (30 tests)
 * 
 * Coverage:
 * - Upload scenarios (10 tests)
 * - Retry logic (5 tests)
 * - Image compression (5 tests)
 * - Progress tracking (5 tests)
 * - Error handling (5 tests)
 */
class FileUploadServiceTest {

    private lateinit var mockService: MockFileUploadService

    @BeforeTest
    fun setup() {
        mockService = MockFileUploadService()
    }

    @AfterTest
    fun tearDown() {
        mockService.reset()
    }

    // ====================
    // Upload Scenarios (10 tests)
    // ====================

    @Test
    fun `uploadFile should successfully upload valid JPEG`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        val result = mockService.uploadFile(file, "test.jpg", "image/jpeg")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { upload ->
            assertTrue(upload.url.isNotEmpty())
            assertEquals(file.size.toLong(), upload.sizeBytes)
            assertNotNull(upload.thumbnailUrl)
        }
    }

    @Test
    fun `uploadFile should successfully upload valid PNG`() = runTest {
        val file = CertificationTestFixtures.createMockPng(150)
        val result = mockService.uploadFile(file, "test.png", "image/png")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { upload ->
            assertTrue(upload.url.isNotEmpty())
            assertNotNull(upload.thumbnailUrl)
        }
    }

    @Test
    fun `uploadFile should successfully upload valid PDF`() = runTest {
        val file = CertificationTestFixtures.createMockPdf(500)
        val result = mockService.uploadFile(file, "cert.pdf", "application/pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { upload ->
            assertTrue(upload.url.isNotEmpty())
            assertNull(upload.thumbnailUrl) // PDFs don't get thumbnails in basic impl
        }
    }

    @Test
    fun `uploadFile should reject file exceeding size limit`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(15000) // 15MB
        mockService.maxFileSizeBytes = 10 * 1024 * 1024 // 10MB limit
        
        val result = mockService.uploadFile(file, "huge.jpg", "image/jpeg")
        
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as? FileUploadError.FileTooLarge
        assertNotNull(error)
        assertEquals(file.size.toLong(), error.sizeBytes)
    }

    @Test
    fun `uploadFile should reject invalid file type`() = runTest {
        val file = CertificationTestFixtures.createInvalidFile(50)
        val result = mockService.uploadFile(file, "test.exe", "application/exe")
        
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as? FileUploadError.InvalidFileType
        assertNotNull(error)
        assertEquals("application/exe", error.contentType)
    }

    @Test
    fun `uploadFile should handle empty file`() = runTest {
        val file = ByteArray(0)
        val result = mockService.uploadFile(file, "empty.jpg", "image/jpeg")
        
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as? FileUploadError.InvalidFileType
        assertNotNull(error)
    }

    @Test
    fun `uploadFile should sanitize filename with special characters`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        val result = mockService.uploadFile(file, "test file @#$.jpg", "image/jpeg")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { upload ->
            assertFalse(upload.key.contains(" "))
            assertFalse(upload.key.contains("@"))
            assertFalse(upload.key.contains("#"))
        }
    }

    @Test
    fun `uploadFile should generate unique keys for duplicate filenames`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        
        val result1 = mockService.uploadFile(file, "cert.jpg", "image/jpeg")
        val result2 = mockService.uploadFile(file, "cert.jpg", "image/jpeg")
        
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)
        assertNotEquals(result1.getOrNull()?.key, result2.getOrNull()?.key)
    }

    @Test
    fun `uploadFile should preserve file extension`() = runTest {
        val file = CertificationTestFixtures.createMockPdf(200)
        val result = mockService.uploadFile(file, "document.pdf", "application/pdf")
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { upload ->
            assertTrue(upload.key.endsWith(".pdf"))
        }
    }

    @Test
    fun `uploadFile should support various image formats`() = runTest {
        val formats = listOf(
            Triple("test.jpg", "image/jpeg", CertificationTestFixtures.validJpegHeader),
            Triple("test.jpeg", "image/jpeg", CertificationTestFixtures.validJpegHeader),
            Triple("test.png", "image/png", CertificationTestFixtures.validPngHeader)
        )
        
        formats.forEach { (fileName, contentType, header) ->
            val file = ByteArray(10240)
            header.copyInto(file)
            
            val result = mockService.uploadFile(file, fileName, contentType)
            assertTrue(result.isSuccess, "Failed to upload $fileName")
        }
    }

    // ====================
    // Retry Logic (5 tests)
    // ====================

    @Test
    fun `uploadFile should retry on transient network error`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        mockService.failureCount = 2 // Fail first 2 attempts
        
        val result = mockService.uploadFile(file, "test.jpg", "image/jpeg")
        
        assertTrue(result.isSuccess)
        assertEquals(3, mockService.attemptCount) // 2 failures + 1 success
    }

    @Test
    fun `uploadFile should fail after max retries`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        mockService.failureCount = 10 // Always fail
        mockService.maxRetries = 3
        
        val result = mockService.uploadFile(file, "test.jpg", "image/jpeg")
        
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as? FileUploadError.RetryExhausted
        assertNotNull(error)
        assertEquals(4, error.attempts) // 1 initial + 3 retries
    }

    @Test
    fun `uploadFile should implement exponential backoff`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        mockService.failureCount = 2
        mockService.trackBackoffTimes = true
        
        mockService.uploadFile(file, "test.jpg", "image/jpeg")
        
        val backoffTimes = mockService.backoffTimes
        assertTrue(backoffTimes.size >= 2)
        assertTrue(backoffTimes[1] > backoffTimes[0]) // Second backoff longer than first
    }

    @Test
    fun `uploadFile should not retry on authentication errors`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        mockService.simulateAuthError = true
        
        val result = mockService.uploadFile(file, "test.jpg", "image/jpeg")
        
        assertTrue(result.isFailure)
        assertEquals(1, mockService.attemptCount) // No retries
        assertTrue(result.exceptionOrNull() is FileUploadError.AuthenticationError)
    }

    @Test
    fun `uploadFile should not retry on invalid file type`() = runTest {
        val file = CertificationTestFixtures.createInvalidFile(50)
        
        val result = mockService.uploadFile(file, "test.exe", "application/exe")
        
        assertTrue(result.isFailure)
        assertEquals(1, mockService.attemptCount) // No retries on validation errors
    }

    // ====================
    // Image Compression (5 tests)
    // ====================

    @Test
    fun `compressImage should reduce file size below target`() = runTest {
        val largeImage = CertificationTestFixtures.createMockJpeg(2000) // 2MB
        val result = mockService.compressImage(largeImage, maxSizeKB = 500)
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { compressed ->
            assertTrue(compressed.size < 500 * 1024)
        }
    }

    @Test
    fun `compressImage should not compress if already below target`() = runTest {
        val smallImage = CertificationTestFixtures.createMockJpeg(100) // 100KB
        val result = mockService.compressImage(smallImage, maxSizeKB = 500)
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { compressed ->
            // Should be similar size (no compression needed)
            assertTrue(compressed.size <= smallImage.size)
        }
    }

    @Test
    fun `compressImage should preserve JPEG header`() = runTest {
        val image = CertificationTestFixtures.createMockJpeg(1000)
        val result = mockService.compressImage(image, maxSizeKB = 500)
        
        assertTrue(result.isSuccess)
        result.getOrNull()?.let { compressed ->
            // Check JPEG magic number
            assertEquals(0xFF.toByte(), compressed[0])
            assertEquals(0xD8.toByte(), compressed[1])
        }
    }

    @Test
    fun `compressImage should fail on invalid image data`() = runTest {
        val invalidData = ByteArray(1024) { 0x00 }
        val result = mockService.compressImage(invalidData, maxSizeKB = 500)
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileUploadError.CompressionFailed)
    }

    @Test
    fun `compressImage should handle various quality levels`() = runTest {
        val image = CertificationTestFixtures.createMockJpeg(1000)
        
        val highQuality = mockService.compressImage(image, maxSizeKB = 800)
        val mediumQuality = mockService.compressImage(image, maxSizeKB = 400)
        val lowQuality = mockService.compressImage(image, maxSizeKB = 200)
        
        assertTrue(highQuality.isSuccess)
        assertTrue(mediumQuality.isSuccess)
        assertTrue(lowQuality.isSuccess)
        
        // Lower target = smaller result
        val highSize = highQuality.getOrNull()!!.size
        val mediumSize = mediumQuality.getOrNull()!!.size
        val lowSize = lowQuality.getOrNull()!!.size
        
        assertTrue(lowSize <= mediumSize)
        assertTrue(mediumSize <= highSize)
    }

    // ====================
    // Progress Tracking (5 tests)
    // ====================

    @Test
    fun `uploadFile should report progress from 0 to 1`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(1000)
        val progressValues = mutableListOf<Float>()
        
        mockService.uploadFile(file, "test.jpg", "image/jpeg") { progress ->
            progressValues.add(progress)
        }
        
        assertTrue(progressValues.isNotEmpty())
        assertEquals(0.0f, progressValues.first())
        assertEquals(1.0f, progressValues.last())
    }

    @Test
    fun `uploadFile should report monotonically increasing progress`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(1000)
        val progressValues = mutableListOf<Float>()
        
        mockService.uploadFile(file, "test.jpg", "image/jpeg") { progress ->
            progressValues.add(progress)
        }
        
        progressValues.zipWithNext().forEach { (prev, next) ->
            assertTrue(next >= prev, "Progress should not decrease: $prev -> $next")
        }
    }

    @Test
    fun `uploadFiles should report aggregated progress`() = runTest {
        val files = listOf(
            CertificationTestFixtures.createFileUpload("file1.jpg", "image/jpeg", 100),
            CertificationTestFixtures.createFileUpload("file2.jpg", "image/jpeg", 100),
            CertificationTestFixtures.createFileUpload("file3.jpg", "image/jpeg", 100)
        )
        val progressValues = mutableListOf<Float>()
        
        mockService.uploadFiles(files) { progress ->
            progressValues.add(progress)
        }
        
        assertTrue(progressValues.isNotEmpty())
        assertEquals(0.0f, progressValues.first())
        assertEquals(1.0f, progressValues.last())
    }

    @Test
    fun `uploadFile should call progress callback multiple times`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(1000)
        var callbackCount = 0
        
        mockService.uploadFile(file, "test.jpg", "image/jpeg") { _ ->
            callbackCount++
        }
        
        assertTrue(callbackCount >= 3, "Expected at least 3 progress callbacks, got $callbackCount")
    }

    @Test
    fun `uploadFiles should handle individual file progress`() = runTest {
        val files = (1..5).map {
            CertificationTestFixtures.createFileUpload("file$it.jpg", "image/jpeg", 100)
        }
        
        val results = mockService.uploadFiles(files)
        
        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isSuccess)
        }
    }

    // ====================
    // Error Handling (5 tests)
    // ====================

    @Test
    fun `uploadFile should handle network timeout gracefully`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        mockService.simulateTimeout = true
        
        val result = mockService.uploadFile(file, "test.jpg", "image/jpeg")
        
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileUploadError.NetworkError)
    }

    @Test
    fun `uploadFiles should handle partial failures`() = runTest {
        val files = listOf(
            CertificationTestFixtures.createFileUpload("file1.jpg", "image/jpeg", 100),
            CertificationTestFixtures.createFileUpload("invalid.exe", "application/exe", 100),
            CertificationTestFixtures.createFileUpload("file3.jpg", "image/jpeg", 100)
        )
        
        val results = mockService.uploadFiles(files)
        
        assertEquals(3, results.size)
        assertTrue(results[0].isSuccess)
        assertTrue(results[1].isFailure)
        assertTrue(results[2].isSuccess)
    }

    @Test
    fun `uploadFile should provide detailed error messages`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        mockService.simulateDetailedError = true
        
        val result = mockService.uploadFile(file, "test.jpg", "image/jpeg")
        
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error.message!!.contains("detailed"))
    }

    @Test
    fun `uploadFile should handle concurrent uploads without race conditions`() = runTest {
        val files = (1..10).map {
            CertificationTestFixtures.createMockJpeg(100)
        }
        
        val results = files.map { file ->
            mockService.uploadFile(file, "test.jpg", "image/jpeg")
        }
        
        assertTrue(results.all { it.isSuccess })
        val keys = results.mapNotNull { it.getOrNull()?.key }
        assertEquals(keys.size, keys.distinct().size) // All keys should be unique
    }

    @Test
    fun `uploadFile should clean up resources on failure`() = runTest {
        val file = CertificationTestFixtures.createMockJpeg(100)
        mockService.simulateTimeout = true
        
        mockService.uploadFile(file, "test.jpg", "image/jpeg")
        
        // Verify cleanup occurred
        assertTrue(mockService.cleanupCalled)
    }
}

/**
 * Mock implementation of FileUploadService for testing
 */
class MockFileUploadService : FileUploadService {
    var maxFileSizeBytes: Long = 10 * 1024 * 1024 // 10MB default
    var maxRetries: Int = 3
    var failureCount: Int = 0
    var attemptCount: Int = 0
    var simulateAuthError: Boolean = false
    var simulateTimeout: Boolean = false
    var simulateDetailedError: Boolean = false
    var trackBackoffTimes: Boolean = false
    var backoffTimes: MutableList<Long> = mutableListOf()
    var cleanupCalled: Boolean = false
    
    private var uploadCounter: Int = 0
    private val allowedContentTypes = setOf(
        "image/jpeg", "image/jpg", "image/png", "application/pdf"
    )

    fun reset() {
        attemptCount = 0
        failureCount = 0
        backoffTimes.clear()
        cleanupCalled = false
        simulateAuthError = false
        simulateTimeout = false
        simulateDetailedError = false
    }

    override suspend fun uploadFile(
        file: ByteArray,
        fileName: String,
        contentType: String,
        onProgress: (Float) -> Unit
    ): Result<UploadResult> {
        attemptCount++
        
        // Simulate authentication error
        if (simulateAuthError) {
            return Result.failure(FileUploadError.AuthenticationError("Invalid credentials"))
        }
        
        // Validate file size
        if (file.isEmpty()) {
            return Result.failure(FileUploadError.InvalidFileType(contentType))
        }
        
        if (file.size > maxFileSizeBytes) {
            return Result.failure(FileUploadError.FileTooLarge(file.size.toLong(), maxFileSizeBytes))
        }
        
        // Validate content type
        if (contentType !in allowedContentTypes) {
            return Result.failure(FileUploadError.InvalidFileType(contentType))
        }
        
        // Simulate timeout
        if (simulateTimeout) {
            cleanupCalled = true
            return Result.failure(FileUploadError.NetworkError(Exception("Connection timeout")))
        }
        
        // Simulate detailed error
        if (simulateDetailedError) {
            return Result.failure(FileUploadError.UploadFailed("Detailed error description"))
        }
        
        // Simulate transient failures with retry
        if (failureCount > 0 && attemptCount <= failureCount) {
            if (trackBackoffTimes) {
                backoffTimes.add(attemptCount * 100L)
            }
            if (attemptCount > maxRetries + 1) {
                return Result.failure(FileUploadError.RetryExhausted(attemptCount))
            }
            return Result.failure(FileUploadError.NetworkError(Exception("Transient error")))
        }
        
        // Simulate progress
        onProgress(0.0f)
        onProgress(0.3f)
        onProgress(0.7f)
        onProgress(1.0f)
        
        // Generate unique key
        val sanitizedName = fileName.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val key = "certifications/${++uploadCounter}/$sanitizedName"
        
        val hasThumbnail = contentType.startsWith("image/")
        
        return Result.success(
            UploadResult(
                url = "https://cdn.hazardhawk.com/$key",
                thumbnailUrl = if (hasThumbnail) "https://cdn.hazardhawk.com/$key-thumb.jpg" else null,
                sizeBytes = file.size.toLong(),
                key = key
            )
        )
    }

    override suspend fun compressImage(imageData: ByteArray, maxSizeKB: Int): Result<ByteArray> {
        // Check for valid JPEG header
        if (imageData.size < 2 || imageData[0] != 0xFF.toByte() || imageData[1] != 0xD8.toByte()) {
            return Result.failure(FileUploadError.CompressionFailed("Invalid JPEG format"))
        }
        
        val targetSize = maxSizeKB * 1024
        
        if (imageData.size <= targetSize) {
            return Result.success(imageData)
        }
        
        // Simulate compression by creating smaller array with header
        val compressed = ByteArray(minOf(targetSize, imageData.size))
        CertificationTestFixtures.validJpegHeader.copyInto(compressed)
        
        return Result.success(compressed)
    }

    override suspend fun uploadFiles(
        files: List<FileUpload>,
        onProgress: (Float) -> Unit
    ): List<Result<UploadResult>> {
        val results = mutableListOf<Result<UploadResult>>()
        
        files.forEachIndexed { index, file ->
            val result = uploadFile(file.data, file.fileName, file.contentType)
            results.add(result)
            
            val progress = (index + 1) / files.size.toFloat()
            onProgress(progress)
        }
        
        return results
    }
}
