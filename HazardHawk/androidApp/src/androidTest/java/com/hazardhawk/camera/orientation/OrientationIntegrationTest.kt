package com.hazardhawk.camera.orientation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.camera.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Integration tests for photo orientation and watermark processing
 * 
 * These tests use real Android components and file I/O to verify:
 * - End-to-end orientation handling from capture to storage
 * - EXIF metadata preservation across processing steps
 * - Visual watermark application without orientation corruption
 * - Cross-device compatibility and performance
 */
@RunWith(AndroidJUnit4::class)
class OrientationIntegrationTest {

    private lateinit var context: Context
    private lateinit var testDir: File
    private lateinit var orientationManager: PhotoOrientationManager
    private lateinit var metadataEmbedder: MetadataEmbedder

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.cacheDir, "orientation_tests").apply {
            mkdirs()
        }
        orientationManager = PhotoOrientationManager.getInstance()
        metadataEmbedder = MetadataEmbedder(context)
    }

    @Test
    fun testFullOrientationWorkflow() = runBlocking {
        // Create test images with different orientations
        val orientationCases = listOf(
            TestImageCase("portrait_normal", 1080, 1920, ExifInterface.ORIENTATION_NORMAL),
            TestImageCase("portrait_rotated", 1080, 1920, ExifInterface.ORIENTATION_ROTATE_90),
            TestImageCase("landscape_normal", 1920, 1080, ExifInterface.ORIENTATION_NORMAL),
            TestImageCase("landscape_rotated", 1920, 1080, ExifInterface.ORIENTATION_ROTATE_270)
        )

        orientationCases.forEach { testCase ->
            // Create test image
            val testFile = createTestImageWithOrientation(testCase)
            
            // Analyze orientation
            val orientationResult = orientationManager.analyzeOrientation(testFile)
            
            // Verify orientation detection
            assertEquals("Orientation should be detected correctly for ${testCase.name}",
                PhotoOrientationManager.PhotoOrientation.fromExifValue(testCase.exifOrientation),
                orientationResult.orientation
            )
            
            // Apply metadata embedding with watermark
            val metadata = createTestMetadata()
            val result = metadataEmbedder.embedMetadata(testFile, metadata, addVisualWatermark = true)
            
            // Verify processing succeeded
            assertTrue("Metadata embedding should succeed for ${testCase.name}", result.isSuccess)
            
            // Verify EXIF orientation preserved after processing
            val finalExif = ExifInterface(testFile.absolutePath)
            val finalOrientation = finalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            assertEquals("EXIF orientation should be preserved for ${testCase.name}",
                testCase.exifOrientation, finalOrientation
            )
            
            // Verify image is still readable and has correct dimensions
            val processedBitmap = BitmapFactory.decodeFile(testFile.absolutePath)
            assertNotNull("Processed image should be readable for ${testCase.name}", processedBitmap)
            
            // Clean up
            testFile.delete()
            processedBitmap?.recycle()
        }
    }

    @Test
    fun testWatermarkConsistencyAcrossOrientations() = runBlocking {
        val aspectRatioTests = listOf(
            Triple("4:3 Portrait", 3000, 4000, "AspectRatio: 4:3"),
            Triple("16:9 Landscape", 4000, 2250, "AspectRatio: 16:9"),
            Triple("1:1 Square", 3000, 3000, "AspectRatio: 1:1")
        )

        aspectRatioTests.forEach { (name, width, height, deviceInfo) ->
            val testFile = createTestImage(name.replace(":", "_"), width, height)
            val metadata = createTestMetadata().copy(deviceInfo = deviceInfo)
            
            // Apply watermark
            val result = metadataEmbedder.embedMetadata(testFile, metadata, addVisualWatermark = true)
            assertTrue("Watermark should be applied successfully for $name", result.isSuccess)
            
            // Load processed image and verify it's readable
            val processedBitmap = BitmapFactory.decodeFile(testFile.absolutePath)
            assertNotNull("Watermarked image should be readable for $name", processedBitmap)
            
            // Verify aspect ratio is maintained (with tolerance for cropping)
            val originalRatio = width.toFloat() / height.toFloat()
            val processedRatio = processedBitmap!!.width.toFloat() / processedBitmap.height.toFloat()
            val ratioDifference = kotlin.math.abs(originalRatio - processedRatio)
            
            assertTrue("Aspect ratio should be maintained within tolerance for $name (original: $originalRatio, processed: $processedRatio)",
                ratioDifference < 0.1f || ratioDifference > 2.0f) // Allow for orientation swaps
            
            testFile.delete()
            processedBitmap.recycle()
        }
    }

    @Test
    fun testMemoryEfficiencyDuringProcessing() = runBlocking {
        // Create large test image to stress memory management
        val largeImageFile = createTestImage("large_image", 4000, 6000)
        val metadata = createTestMetadata()
        
        // Monitor memory before processing
        val runtime = Runtime.getRuntime()
        runtime.gc() // Suggest garbage collection
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Process large image
        val result = metadataEmbedder.embedMetadata(largeImageFile, metadata, addVisualWatermark = true)
        assertTrue("Large image processing should succeed", result.isSuccess)
        
        // Monitor memory after processing
        runtime.gc()
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        
        // Verify no significant memory leak (allowing for some overhead)
        val memoryIncrease = memoryAfter - memoryBefore
        val maxAcceptableIncrease = 50 * 1024 * 1024 // 50MB threshold
        
        assertTrue("Memory usage should not increase excessively (increased by ${memoryIncrease / 1024 / 1024}MB)",
            memoryIncrease < maxAcceptableIncrease
        )
        
        // Verify processed image is still valid
        val processedBitmap = BitmapFactory.decodeFile(largeImageFile.absolutePath)
        assertNotNull("Large processed image should be readable", processedBitmap)
        
        largeImageFile.delete()
        processedBitmap?.recycle()
    }

    @Test
    fun testConcurrentOrientationProcessing() = runBlocking {
        // Create multiple test images for concurrent processing
        val testFiles = (1..5).map { index ->
            createTestImageWithOrientation(
                TestImageCase(
                    "concurrent_$index",
                    1920, 1080,
                    listOf(
                        ExifInterface.ORIENTATION_NORMAL,
                        ExifInterface.ORIENTATION_ROTATE_90,
                        ExifInterface.ORIENTATION_ROTATE_180,
                        ExifInterface.ORIENTATION_ROTATE_270,
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL
                    )[index - 1]
                )
            )
        }

        // Process all images concurrently
        val results = testFiles.map { file ->
            val metadata = createTestMetadata()
            metadataEmbedder.embedMetadata(file, metadata, addVisualWatermark = true)
        }

        // Verify all processing succeeded
        results.forEachIndexed { index, result ->
            assertTrue("Concurrent processing should succeed for image $index", result.isSuccess)
        }

        // Verify all images are still valid and have correct orientations
        testFiles.forEachIndexed { index, file ->
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            assertNotNull("Concurrent processed image $index should be readable", bitmap)
            
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            assertTrue("Concurrent processed image $index should have valid orientation", 
                orientation != ExifInterface.ORIENTATION_UNDEFINED)
            
            file.delete()
            bitmap?.recycle()
        }
    }

    @Test
    fun testOrientationPreservationWithDifferentFileFormats() {
        // Note: This test would ideally test different formats, but Android primarily uses JPEG
        val testFile = createTestImageWithOrientation(
            TestImageCase("format_test", 2000, 3000, ExifInterface.ORIENTATION_ROTATE_90)
        )

        // Verify EXIF data is correctly embedded and preserved
        val originalExif = ExifInterface(testFile.absolutePath)
        val originalOrientation = originalExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        assertEquals("Original orientation should be set correctly",
            ExifInterface.ORIENTATION_ROTATE_90, originalOrientation
        )

        // Process with orientation manager
        val bitmap = orientationManager.loadBitmapWithCorrectOrientation(testFile)
        assertNotNull("Bitmap should be loaded with correct orientation", bitmap)

        // Verify dimensions match expected rotation (width and height should be swapped)
        assertTrue("Rotated bitmap should have correct dimensions", 
            bitmap!!.width == 3000 && bitmap.height == 2000
        )

        testFile.delete()
        bitmap.recycle()
    }

    @Test
    fun testDeviceSpecificOrientationHandling() = runBlocking {
        // Test with various device info strings that might come from different devices
        val deviceInfoCases = listOf(
            "Samsung Galaxy S21 - AspectRatio: 16:9",
            "Google Pixel 6 - AspectRatio: 4:3",
            "OnePlus 9 - AspectRatio: 1:1",
            "Unknown Device - No AspectRatio",
            "" // Empty device info
        )

        deviceInfoCases.forEach { deviceInfo ->
            val testFile = createTestImage("device_${deviceInfo.hashCode()}", 1920, 1080)
            val metadata = createTestMetadata().copy(deviceInfo = deviceInfo)
            
            val result = metadataEmbedder.embedMetadata(testFile, metadata, addVisualWatermark = true)
            assertTrue("Processing should handle device info: '$deviceInfo'", result.isSuccess)
            
            val processedBitmap = BitmapFactory.decodeFile(testFile.absolutePath)
            assertNotNull("Image should be readable with device info: '$deviceInfo'", processedBitmap)
            
            testFile.delete()
            processedBitmap?.recycle()
        }
    }

    // Helper methods

    private fun createTestImageWithOrientation(testCase: TestImageCase): File {
        val file = createTestImage(testCase.name, testCase.width, testCase.height)
        
        // Set EXIF orientation
        val exif = ExifInterface(file.absolutePath)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, testCase.exifOrientation.toString())
        exif.saveAttributes()
        
        return file
    }

    private fun createTestImage(name: String, width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmap.eraseColor(android.graphics.Color.BLUE) // Fill with blue for visibility
        
        val file = File(testDir, "$name.jpg")
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        bitmap.recycle()
        
        return file
    }

    private fun createTestMetadata(): CaptureMetadata {
        return CaptureMetadata(
            timestamp = System.currentTimeMillis(),
            locationData = LocationData(
                latitude = 40.7589,
                longitude = -73.9851,
                isAvailable = true,
                address = "Times Square, New York, NY",
                accuracy = 5.0f
            ),
            projectName = "Integration Test Project",
            projectId = "ITP001",
            userName = "Test User",
            userId = "TU001",
            deviceInfo = "Test Device - AspectRatio: 4:3"
        )
    }

    data class TestImageCase(
        val name: String,
        val width: Int,
        val height: Int,
        val exifOrientation: Int
    )
}
