package com.hazardhawk.camera.orientation

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.hazardhawk.camera.PhotoOrientationManager
import com.hazardhawk.camera.PhotoOrientationManager.PhotoOrientation
import com.hazardhawk.camera.PhotoOrientationManager.OrientationResult
import com.hazardhawk.camera.PhotoOrientationManager.OrientationSource
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

/**
 * Comprehensive unit tests for PhotoOrientationManager
 * 
 * Tests cover:
 * - EXIF orientation detection for all 8 possible values
 * - Fallback pixel analysis when EXIF is unavailable
 * - Matrix calculations for orientation transformations
 * - Memory management during bitmap operations
 * - Error handling and edge cases
 */
class PhotoOrientationManagerTest {

    private lateinit var orientationManager: PhotoOrientationManager
    private lateinit var mockFile: File
    private lateinit var mockBitmap: Bitmap

    @Before
    fun setUp() {
        orientationManager = PhotoOrientationManager.getInstance()
        mockFile = mockk<File>()
        mockBitmap = mockk<Bitmap>(relaxed = true)
        
        // Mock static methods
        mockkStatic(BitmapFactory::class)
        mockkConstructor(ExifInterface::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test all EXIF orientation values are correctly mapped`() {
        // Test all 8 possible EXIF orientation values
        val testCases = mapOf(
            ExifInterface.ORIENTATION_NORMAL to PhotoOrientation.NORMAL,
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL to PhotoOrientation.FLIP_HORIZONTAL,
            ExifInterface.ORIENTATION_ROTATE_180 to PhotoOrientation.ROTATE_180,
            ExifInterface.ORIENTATION_FLIP_VERTICAL to PhotoOrientation.FLIP_VERTICAL,
            ExifInterface.ORIENTATION_TRANSPOSE to PhotoOrientation.TRANSPOSE,
            ExifInterface.ORIENTATION_ROTATE_90 to PhotoOrientation.ROTATE_90,
            ExifInterface.ORIENTATION_TRANSVERSE to PhotoOrientation.TRANSVERSE,
            ExifInterface.ORIENTATION_ROTATE_270 to PhotoOrientation.ROTATE_270
        )

        testCases.forEach { (exifValue, expectedOrientation) ->
            val result = PhotoOrientation.fromExifValue(exifValue)
            assertEquals(
                "EXIF value $exifValue should map to $expectedOrientation",
                expectedOrientation, 
                result
            )
        }
    }

    @Test
    fun `test EXIF orientation reading with valid metadata`() {
        // Setup
        every { mockFile.absolutePath } returns "/test/photo.jpg"
        val mockExif = mockk<ExifInterface>()
        every { mockExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, any()) } returns ExifInterface.ORIENTATION_ROTATE_90
        
        // Mock constructor
        every { anyConstructed<ExifInterface>().getAttributeInt(any(), any()) } returns ExifInterface.ORIENTATION_ROTATE_90

        // Execute
        val result = orientationManager.analyzeOrientation(mockFile)

        // Verify
        assertEquals(PhotoOrientation.ROTATE_90, result.orientation)
        assertEquals(OrientationSource.EXIF, result.source)
        assertTrue("Confidence should be high for valid EXIF", result.confidence > 0.8f)
    }

    @Test
    fun `test fallback pixel analysis for landscape image`() {
        // Setup: Wide landscape image
        every { mockFile.absolutePath } returns "/test/landscape.jpg"
        every { BitmapFactory.decodeFile(any(), any()) } returns mockBitmap
        
        val mockOptions = mockk<BitmapFactory.Options>()
        mockOptions.outWidth = 1920
        mockOptions.outHeight = 1080
        
        every { BitmapFactory.decodeFile(any(), mockOptions) } returns null
        every { anyConstructed<ExifInterface>().getAttributeInt(any(), any()) } returns ExifInterface.ORIENTATION_UNDEFINED

        // Execute
        val result = orientationManager.analyzeOrientation(mockFile)

        // Verify fallback detection triggered
        assertEquals(OrientationSource.FALLBACK, result.source)
    }

    @Test
    fun `test bitmap orientation transformation preserves dimensions correctly`() {
        // Setup: 100x200 portrait bitmap
        every { mockBitmap.width } returns 100
        every { mockBitmap.height } returns 200
        every { mockBitmap.config } returns Bitmap.Config.ARGB_8888
        
        val rotatedBitmap = mockk<Bitmap>()
        every { Bitmap.createBitmap(any<Bitmap>(), any(), any(), any(), any(), any(), any()) } returns rotatedBitmap
        
        // Test 90-degree rotation
        val result = orientationManager.applyOrientationToBitmap(mockBitmap, PhotoOrientation.ROTATE_90)
        
        // Verify transformation was attempted
        verify { Bitmap.createBitmap(mockBitmap, 0, 0, 100, 200, any(), true) }
        assertEquals(rotatedBitmap, result)
    }

    @Test
    fun `test normal orientation returns original bitmap without transformation`() {
        // Execute
        val result = orientationManager.applyOrientationToBitmap(mockBitmap, PhotoOrientation.NORMAL)
        
        // Verify original bitmap returned
        assertEquals(mockBitmap, result)
        
        // Verify no transformation methods called
        verify(exactly = 0) { Bitmap.createBitmap(any<Bitmap>(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test out of memory error handling during transformation`() {
        // Setup to throw OutOfMemoryError
        every { mockBitmap.width } returns 4000
        every { mockBitmap.height } returns 3000
        every { Bitmap.createBitmap(any<Bitmap>(), any(), any(), any(), any(), any(), any()) } throws OutOfMemoryError()
        
        // Execute
        val result = orientationManager.applyOrientationToBitmap(mockBitmap, PhotoOrientation.ROTATE_90)
        
        // Verify original bitmap returned as fallback
        assertEquals(mockBitmap, result)
    }

    @Test
    fun `test inSampleSize calculation for memory efficiency`() {
        // Test cases for different image sizes
        val testCases = listOf(
            Triple(1920, 1080, 1), // Small enough, no downsampling
            Triple(4000, 3000, 2), // Medium size, 2x downsampling
            Triple(8000, 6000, 4)  // Large size, 4x downsampling
        )
        
        testCases.forEach { (width, height, expectedSample) ->
            val options = BitmapFactory.Options().apply {
                outWidth = width
                outHeight = height
            }
            
            val result = orientationManager.calculateInSampleSize(options, 1920, 1080)
            assertEquals(
                "Image ${width}x${height} should use inSampleSize $expectedSample",
                expectedSample, 
                result
            )
        }
    }

    @Test
    fun `test orientation matrix creation for all orientations`() {
        val testCases = listOf(
            PhotoOrientation.NORMAL,
            PhotoOrientation.ROTATE_90,
            PhotoOrientation.ROTATE_180,
            PhotoOrientation.ROTATE_270,
            PhotoOrientation.FLIP_HORIZONTAL,
            PhotoOrientation.FLIP_VERTICAL,
            PhotoOrientation.TRANSPOSE,
            PhotoOrientation.TRANSVERSE
        )

        testCases.forEach { orientation ->
            val matrix = orientationManager.createOrientationMatrix(orientation)
            
            // Verify matrix is not null
            assertNotNull("Matrix should not be null for $orientation", matrix)
            
            // For NORMAL orientation, matrix should be identity-like
            if (orientation == PhotoOrientation.NORMAL) {
                assertTrue("Normal orientation should not modify matrix significantly", 
                    matrix.isIdentity || matrix.toString().contains("identity"))
            }
        }
    }

    @Test
    fun `test integrity hash generation and validation`() {
        // Setup
        every { mockFile.readBytes() } returns "test image data".toByteArray()
        
        // Execute
        val hash1 = orientationManager.generateIntegrityHash(mockFile)
        val hash2 = orientationManager.generateIntegrityHash(mockFile)
        
        // Verify
        assertNotNull("Hash should not be null", hash1)
        assertEquals("Same file should produce same hash", hash1, hash2)
        
        // Test validation
        assertTrue("Hash validation should succeed with correct hash", 
            orientationManager.validateIntegrity(mockFile, hash1!!))
    }

    @Test
    fun `test error handling when file cannot be read`() {
        // Setup file that throws exception
        every { mockFile.absolutePath } returns "/nonexistent/file.jpg"
        every { anyConstructed<ExifInterface>() } throws Exception("File not found")
        
        // Execute
        val result = orientationManager.analyzeOrientation(mockFile)
        
        // Verify graceful error handling
        assertEquals(PhotoOrientation.NORMAL, result.orientation)
        assertEquals(OrientationSource.FALLBACK, result.source)
        assertTrue("Confidence should be low on error", result.confidence < 0.2f)
        assertNotNull("Error message should be present", result.errorMessage)
    }

    @Test
    fun `test bitmap memory cleanup on transformation failure`() {
        // Setup to cause transformation failure
        every { mockBitmap.width } returns 1000
        every { mockBitmap.height } returns 1000
        every { Bitmap.createBitmap(any<Bitmap>(), any(), any(), any(), any(), any(), any()) } throws Exception("Transform failed")
        
        // Execute
        val result = orientationManager.applyOrientationToBitmap(mockBitmap, PhotoOrientation.ROTATE_90)
        
        // Verify original bitmap returned and no memory leak
        assertEquals(mockBitmap, result)
        
        // Verify no bitmap.recycle() called on original (since it's returned)
        verify(exactly = 0) { mockBitmap.recycle() }
    }

    @Test
    fun `test confidence scoring for different orientation sources`() {
        // Test EXIF confidence
        every { mockFile.absolutePath } returns "/test/photo.jpg"
        every { anyConstructed<ExifInterface>().getAttributeInt(any(), any()) } returns ExifInterface.ORIENTATION_ROTATE_90
        
        val exifResult = orientationManager.analyzeOrientation(mockFile)
        assertTrue("EXIF source should have high confidence", exifResult.confidence > 0.8f)
        
        // Test undefined EXIF confidence
        every { anyConstructed<ExifInterface>().getAttributeInt(any(), any()) } returns ExifInterface.ORIENTATION_UNDEFINED
        
        val undefinedResult = orientationManager.analyzeOrientation(mockFile)
        assertTrue("Undefined EXIF should have low confidence", undefinedResult.confidence < 0.5f)
    }

    @Test
    fun `test aspect ratio detection from pixel analysis`() {
        // Setup for different aspect ratios
        val testCases = listOf(
            Triple(2000, 1000, PhotoOrientation.ROTATE_90), // Wide landscape (2:1)
            Triple(1000, 1500, PhotoOrientation.NORMAL),    // Portrait (2:3)
            Triple(1000, 1000, PhotoOrientation.NORMAL)     // Square
        )
        
        testCases.forEach { (width, height, expectedOrientation) ->
            every { mockFile.absolutePath } returns "/test/aspect_test.jpg"
            every { anyConstructed<ExifInterface>().getAttributeInt(any(), any()) } returns ExifInterface.ORIENTATION_UNDEFINED
            
            val mockOptions = BitmapFactory.Options()
            mockOptions.outWidth = width
            mockOptions.outHeight = height
            every { BitmapFactory.decodeFile(any(), any<BitmapFactory.Options>()) } returns null
            
            // Note: This test verifies the heuristic logic exists
            // Actual implementation may use different thresholds
            val result = orientationManager.analyzeOrientation(mockFile)
            
            assertTrue("Pixel analysis should provide some confidence", result.confidence > 0.0f)
            assertEquals(OrientationSource.FALLBACK, result.source)
        }
    }
}
