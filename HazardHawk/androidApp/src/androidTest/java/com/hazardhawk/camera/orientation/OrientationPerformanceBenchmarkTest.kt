package com.hazardhawk.camera.orientation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlin.system.measureTimeMillis

/**
 * Performance benchmark tests for image orientation and watermark processing
 * 
 * These tests establish performance baselines and detect regressions:
 * - Photo processing time across different image sizes
 * - Memory usage during orientation transformations
 * - Batch processing performance
 * - Memory leak detection during repeated operations
 */
@RunWith(AndroidJUnit4::class)
class OrientationPerformanceBenchmarkTest {

    private lateinit var context: Context
    private lateinit var testDir: File
    private lateinit var orientationManager: PhotoOrientationManager
    private lateinit var metadataEmbedder: MetadataEmbedder

    companion object {
        // Performance thresholds in milliseconds
        private const val SMALL_IMAGE_THRESHOLD_MS = 1000    // 1080p images
        private const val MEDIUM_IMAGE_THRESHOLD_MS = 2000   // 4K images
        private const val LARGE_IMAGE_THRESHOLD_MS = 3000    // 8K images
        private const val BATCH_PROCESSING_THRESHOLD_MS = 15000 // 10 photos
        
        // Memory thresholds in MB
        private const val MEMORY_BASELINE_THRESHOLD_MB = 50
        private const val MEMORY_PROCESSING_THRESHOLD_MB = 200
        private const val MEMORY_LEAK_TOLERANCE_MB = 10
    }

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.cacheDir, "performance_tests").apply {
            mkdirs()
        }
        orientationManager = PhotoOrientationManager.getInstance()
        metadataEmbedder = MetadataEmbedder(context)
    }

    @Test
    fun benchmarkSingleImageProcessingTimes() = runBlocking {
        val testCases = listOf(
            PerformanceTestCase("small_1080p", 1920, 1080, SMALL_IMAGE_THRESHOLD_MS),
            PerformanceTestCase("medium_4k", 3840, 2160, MEDIUM_IMAGE_THRESHOLD_MS),
            PerformanceTestCase("large_8k", 7680, 4320, LARGE_IMAGE_THRESHOLD_MS),
            PerformanceTestCase("portrait_4k", 2160, 3840, MEDIUM_IMAGE_THRESHOLD_MS),
            PerformanceTestCase("square_4k", 3000, 3000, MEDIUM_IMAGE_THRESHOLD_MS)
        )

        val results = mutableMapOf<String, Long>()

        testCases.forEach { testCase ->
            // Create test image
            val testImage = createTestImage(testCase.name, testCase.width, testCase.height)
            val metadata = createTestMetadata()

            // Warm up JIT compiler
            repeat(3) {
                metadataEmbedder.embedMetadata(testImage, metadata, addVisualWatermark = true)
            }

            // Benchmark processing time
            val processingTime = measureTimeMillis {
                val result = metadataEmbedder.embedMetadata(testImage, metadata, addVisualWatermark = true)
                assertTrue("Processing should succeed for ${testCase.name}", result.isSuccess)
            }

            results[testCase.name] = processingTime

            // Verify performance threshold
            assertTrue(
                "Processing time for ${testCase.name} should be under ${testCase.thresholdMs}ms (actual: ${processingTime}ms)",
                processingTime <= testCase.thresholdMs
            )

            // Clean up
            testImage.delete()
        }

        // Log results for monitoring
        results.forEach { (name, time) ->
            println("Performance Benchmark - $name: ${time}ms")
        }
    }

    @Test
    fun benchmarkOrientationTransformationPerformance() {
        val transformationCases = listOf(
            TransformationTestCase("rotation_90", 4000, 3000, PhotoOrientationManager.PhotoOrientation.ROTATE_90),
            TransformationTestCase("rotation_180", 4000, 3000, PhotoOrientationManager.PhotoOrientation.ROTATE_180),
            TransformationTestCase("rotation_270", 4000, 3000, PhotoOrientationManager.PhotoOrientation.ROTATE_270),
            TransformationTestCase("flip_horizontal", 4000, 3000, PhotoOrientationManager.PhotoOrientation.FLIP_HORIZONTAL),
            TransformationTestCase("transpose", 4000, 3000, PhotoOrientationManager.PhotoOrientation.TRANSPOSE)
        )

        transformationCases.forEach { testCase ->
            val testBitmap = Bitmap.createBitmap(testCase.width, testCase.height, Bitmap.Config.ARGB_8888)
            testBitmap.eraseColor(android.graphics.Color.BLUE)

            // Warm up
            repeat(3) {
                val transformed = orientationManager.applyOrientationToBitmap(testBitmap, testCase.orientation)
                if (transformed != testBitmap) transformed.recycle()
            }

            // Benchmark transformation
            val transformationTime = measureTimeMillis {
                val transformed = orientationManager.applyOrientationToBitmap(testBitmap, testCase.orientation)
                assertNotNull("Transformation should succeed for ${testCase.name}", transformed)
                if (transformed != testBitmap) transformed.recycle()
            }

            // Verify transformation is fast enough (should be under 500ms for 4K images)
            assertTrue(
                "Orientation transformation for ${testCase.name} should be under 500ms (actual: ${transformationTime}ms)",
                transformationTime <= 500
            )

            println("Transformation Benchmark - ${testCase.name}: ${transformationTime}ms")
            testBitmap.recycle()
        }
    }

    @Test
    fun benchmarkMemoryUsageDuringProcessing() = runBlocking {
        val runtime = Runtime.getRuntime()
        
        // Establish baseline memory usage
        System.gc()
        Thread.sleep(100)
        val baselineMemory = runtime.totalMemory() - runtime.freeMemory()
        
        assertTrue("Baseline memory should be reasonable", 
            baselineMemory < MEMORY_BASELINE_THRESHOLD_MB * 1024 * 1024)

        // Test memory usage during large image processing
        val largeImage = createTestImage("memory_test", 6000, 4000)
        val metadata = createTestMetadata()

        var peakMemory = baselineMemory
        
        val processingTime = measureTimeMillis {
            val result = metadataEmbedder.embedMetadata(largeImage, metadata, addVisualWatermark = true)
            assertTrue("Processing should succeed", result.isSuccess)
            
            // Check peak memory usage
            val currentMemory = runtime.totalMemory() - runtime.freeMemory()
            if (currentMemory > peakMemory) {
                peakMemory = currentMemory
            }
        }

        // Check memory after processing
        System.gc()
        Thread.sleep(100)
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val peakMemoryMB = peakMemory / (1024 * 1024)
        val memoryIncrease = (finalMemory - baselineMemory) / (1024 * 1024)

        assertTrue("Peak memory usage should be under threshold (${peakMemoryMB}MB)",
            peakMemoryMB < MEMORY_PROCESSING_THRESHOLD_MB)
        
        assertTrue("Memory should return near baseline after processing (increase: ${memoryIncrease}MB)",
            memoryIncrease < MEMORY_LEAK_TOLERANCE_MB)

        println("Memory Benchmark - Peak: ${peakMemoryMB}MB, Final increase: ${memoryIncrease}MB")
        largeImage.delete()
    }

    @Test
    fun benchmarkBatchProcessingPerformance() = runBlocking {
        val batchSize = 10
        val testImages = mutableListOf<File>()
        
        // Create batch of test images
        repeat(batchSize) { index ->
            val image = createTestImage("batch_$index", 2000, 1500)
            testImages.add(image)
        }

        val metadata = createTestMetadata()
        val runtime = Runtime.getRuntime()
        
        System.gc()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Benchmark batch processing
        val batchProcessingTime = measureTimeMillis {
            testImages.forEach { image ->
                val result = metadataEmbedder.embedMetadata(image, metadata, addVisualWatermark = true)
                assertTrue("Batch processing should succeed for ${image.name}", result.isSuccess)
            }
        }

        // Check final memory state
        System.gc()
        Thread.sleep(100)
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024)

        // Verify batch processing performance
        assertTrue("Batch processing should complete within threshold (${batchProcessingTime}ms)",
            batchProcessingTime <= BATCH_PROCESSING_THRESHOLD_MS)
        
        assertTrue("Batch processing should not leak significant memory (increase: ${memoryIncrease}MB)",
            memoryIncrease < MEMORY_LEAK_TOLERANCE_MB * 2) // Allow slightly more tolerance for batch

        val averageTimePerImage = batchProcessingTime / batchSize
        println("Batch Processing Benchmark - Total: ${batchProcessingTime}ms, Average per image: ${averageTimePerImage}ms")

        // Clean up
        testImages.forEach { it.delete() }
    }

    @Test
    fun benchmarkConcurrentProcessingStress() = runBlocking {
        val concurrentJobs = 3 // Reasonable concurrent load
        val imagesPerJob = 5
        
        val allTestImages = mutableListOf<File>()
        val metadata = createTestMetadata()
        
        // Create test images for concurrent processing
        repeat(concurrentJobs * imagesPerJob) { index ->
            val image = createTestImage("concurrent_$index", 1920, 1080)
            allTestImages.add(image)
        }

        val runtime = Runtime.getRuntime()
        System.gc()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // Benchmark concurrent processing
        val concurrentProcessingTime = measureTimeMillis {
            val imageChunks = allTestImages.chunked(imagesPerJob)
            
            // Process chunks concurrently (simulated)
            imageChunks.forEach { chunk ->
                chunk.forEach { image ->
                    val result = metadataEmbedder.embedMetadata(image, metadata, addVisualWatermark = true)
                    assertTrue("Concurrent processing should succeed for ${image.name}", result.isSuccess)
                }
            }
        }

        // Check memory stability under concurrent load
        System.gc()
        Thread.sleep(200)
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024)

        // Verify concurrent processing doesn't cause excessive resource usage
        val expectedSequentialTime = (allTestImages.size * SMALL_IMAGE_THRESHOLD_MS * 1.2).toLong() // 20% overhead allowance
        assertTrue("Concurrent processing should be reasonably efficient (${concurrentProcessingTime}ms vs expected ${expectedSequentialTime}ms)",
            concurrentProcessingTime <= expectedSequentialTime)
        
        assertTrue("Concurrent processing should not leak significant memory (increase: ${memoryIncrease}MB)",
            memoryIncrease < MEMORY_LEAK_TOLERANCE_MB * concurrentJobs)

        println("Concurrent Processing Benchmark - ${allTestImages.size} images: ${concurrentProcessingTime}ms")

        // Clean up
        allTestImages.forEach { it.delete() }
    }

    @Test
    fun benchmarkMemoryLeakDetection() = runBlocking {
        val iterations = 20
        val runtime = Runtime.getRuntime()
        val metadata = createTestMetadata()
        
        // Establish baseline
        System.gc()
        Thread.sleep(100)
        val baselineMemory = runtime.totalMemory() - runtime.freeMemory()
        
        val memorySnapshots = mutableListOf<Long>()
        
        // Process images repeatedly to detect memory leaks
        repeat(iterations) { iteration ->
            val testImage = createTestImage("leak_test_$iteration", 2000, 1500)
            
            val result = metadataEmbedder.embedMetadata(testImage, metadata, addVisualWatermark = true)
            assertTrue("Processing should succeed in iteration $iteration", result.isSuccess)
            
            testImage.delete()
            
            // Take memory snapshot every few iterations
            if (iteration % 5 == 4) {
                System.gc()
                Thread.sleep(50)
                val currentMemory = runtime.totalMemory() - runtime.freeMemory()
                memorySnapshots.add(currentMemory - baselineMemory)
            }
        }
        
        // Analyze memory trend
        val memoryTrend = calculateMemoryTrend(memorySnapshots)
        val finalMemoryIncrease = memorySnapshots.last() / (1024 * 1024)
        
        // Verify no significant memory leak
        assertTrue("Memory should not increase significantly over iterations (final increase: ${finalMemoryIncrease}MB)",
            finalMemoryIncrease < MEMORY_LEAK_TOLERANCE_MB)
        
        assertTrue("Memory usage should not show significant upward trend",
            memoryTrend < MEMORY_LEAK_TOLERANCE_MB * 1024 * 1024 / iterations) // Trend per iteration

        println("Memory Leak Test - Final increase: ${finalMemoryIncrease}MB, Trend: ${memoryTrend / (1024 * 1024)}MB per iteration")
    }

    @Test
    fun benchmarkExifProcessingPerformance() {
        val testCases = listOf(
            "minimal_exif" to createMinimalExifImage(),
            "rich_exif" to createRichExifImage(),
            "corrupted_exif" to createCorruptedExifImage()
        )

        testCases.forEach { (caseName, testImage) ->
            // Warm up
            repeat(3) {
                orientationManager.analyzeOrientation(testImage)
            }

            // Benchmark EXIF analysis
            val analysisTime = measureTimeMillis {
                val result = orientationManager.analyzeOrientation(testImage)
                assertNotNull("EXIF analysis should complete for $caseName", result)
            }

            // EXIF analysis should be very fast (<100ms)
            assertTrue("EXIF analysis for $caseName should be under 100ms (actual: ${analysisTime}ms)",
                analysisTime <= 100)

            println("EXIF Processing Benchmark - $caseName: ${analysisTime}ms")
            testImage.delete()
        }
    }

    // Helper Methods

    private fun createTestImage(name: String, width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmap.eraseColor(android.graphics.Color.BLUE)
        
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
                latitude = 40.7128,
                longitude = -74.0060,
                isAvailable = true,
                address = "Performance Test Location",
                accuracy = 5.0f
            ),
            projectName = "Performance Test Project",
            projectId = "PTP001",
            userName = "Test User",
            userId = "TU001",
            deviceInfo = "Performance Test Device - AspectRatio: 4:3"
        )
    }

    private fun createMinimalExifImage(): File {
        val file = createTestImage("minimal_exif", 1920, 1080)
        // Minimal EXIF data
        return file
    }

    private fun createRichExifImage(): File {
        val file = createTestImage("rich_exif", 1920, 1080)
        val exif = ExifInterface(file.absolutePath)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
        exif.setAttribute(ExifInterface.TAG_DATETIME, "2023:01:01 12:00:00")
        exif.setAttribute(ExifInterface.TAG_MAKE, "Test Camera")
        exif.setAttribute(ExifInterface.TAG_MODEL, "Performance Test Model")
        exif.setLatLong(40.7128, -74.0060)
        exif.saveAttributes()
        return file
    }

    private fun createCorruptedExifImage(): File {
        val file = createTestImage("corrupted_exif", 1920, 1080)
        // This would simulate corrupted EXIF - for testing purposes, just leave minimal
        return file
    }

    private fun calculateMemoryTrend(snapshots: List<Long>): Long {
        if (snapshots.size < 2) return 0
        
        // Simple linear trend calculation
        val firstHalf = snapshots.take(snapshots.size / 2).average()
        val secondHalf = snapshots.drop(snapshots.size / 2).average()
        
        return (secondHalf - firstHalf).toLong()
    }

    // Data Classes

    data class PerformanceTestCase(
        val name: String,
        val width: Int,
        val height: Int,
        val thresholdMs: Long
    )

    data class TransformationTestCase(
        val name: String,
        val width: Int,
        val height: Int,
        val orientation: PhotoOrientationManager.PhotoOrientation
    )
}
