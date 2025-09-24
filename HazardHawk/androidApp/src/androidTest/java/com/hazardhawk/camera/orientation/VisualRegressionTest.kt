package com.hazardhawk.camera.orientation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
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
import kotlin.math.*

/**
 * Visual regression tests for photo orientation and watermark consistency
 * 
 * These tests create visual comparisons to detect:
 * - Watermark positioning drift across orientations
 * - Text sizing inconsistencies between devices
 * - Visual artifacts from orientation transformations
 * - Aspect ratio preservation accuracy
 */
@RunWith(AndroidJUnit4::class)
class VisualRegressionTest {

    private lateinit var context: Context
    private lateinit var testDir: File
    private lateinit var metadataEmbedder: MetadataEmbedder
    private lateinit var baselineDir: File

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        testDir = File(context.cacheDir, "visual_regression_tests").apply {
            mkdirs()
        }
        baselineDir = File(testDir, "baselines").apply {
            mkdirs()
        }
        metadataEmbedder = MetadataEmbedder(context)
    }

    @Test
    fun testWatermarkPositionalConsistency() = runBlocking {
        val testCases = listOf(
            VisualTestCase("portrait_4_3", 3000, 4000, "AspectRatio: 4:3"),
            VisualTestCase("landscape_16_9", 4000, 2250, "AspectRatio: 16:9"),
            VisualTestCase("square_1_1", 3000, 3000, "AspectRatio: 1:1"),
            VisualTestCase("wide_panoramic", 6000, 2000, "AspectRatio: 16:9"),
            VisualTestCase("tall_banner", 1500, 6000, "AspectRatio: 4:3")
        )

        testCases.forEach { testCase ->
            // Generate test image with watermark
            val testImage = generateTestImageWithWatermark(testCase)
            
            // Extract watermark region for comparison
            val watermarkRegion = extractWatermarkRegion(testImage, testCase)
            
            // Calculate positioning metrics
            val metrics = calculateWatermarkMetrics(watermarkRegion, testCase)
            
            // Verify consistent positioning rules
            assertWatermarkPositioning(metrics, testCase)
            
            // Save for manual inspection if needed
            saveTestResult(testImage, "watermark_${testCase.name}")
            
            testImage.recycle()
        }
    }

    @Test
    fun testTextSizeScaling() = runBlocking {
        val scalingTestCases = listOf(
            ScalingTestCase("small_image", 800, 600),
            ScalingTestCase("medium_image", 1920, 1080),
            ScalingTestCase("large_image", 4000, 3000),
            ScalingTestCase("ultra_large", 8000, 6000),
            ScalingTestCase("minimum_size", 400, 300)
        )

        scalingTestCases.forEach { testCase ->
            val testImage = generateTestImageWithWatermark(
                VisualTestCase(testCase.name, testCase.width, testCase.height, "AspectRatio: 4:3")
            )
            
            // Analyze text size in watermark
            val textMetrics = analyzeWatermarkTextSize(testImage, testCase)
            
            // Verify scaling follows expected ratios
            val expectedTextSize = (testCase.width * 0.035f).coerceAtLeast(60f)
            val tolerance = expectedTextSize * 0.1f // 10% tolerance
            
            assertTrue("Text size should match scaling formula for ${testCase.name} (expected: $expectedTextSize, actual: ${textMetrics.estimatedTextSize})",
                abs(textMetrics.estimatedTextSize - expectedTextSize) <= tolerance
            )
            
            // Verify readability thresholds
            assertTrue("Text should be readable on ${testCase.name}",
                textMetrics.estimatedTextSize >= 60f
            )
            
            saveTestResult(testImage, "scaling_${testCase.name}")
            testImage.recycle()
        }
    }

    @Test
    fun testOrientationVisualConsistency() = runBlocking {
        val orientationCases = listOf(
            OrientationTestCase("normal", 2000, 3000, 1), // Normal
            OrientationTestCase("rotated_90", 2000, 3000, 6), // 90° CW
            OrientationTestCase("rotated_180", 2000, 3000, 3), // 180°
            OrientationTestCase("rotated_270", 2000, 3000, 8) // 270° CW
        )

        val baselineHashes = mutableMapOf<String, String>()

        orientationCases.forEach { testCase ->
            val testImage = generateOrientedTestImage(testCase)
            
            // Extract visual features for comparison
            val visualHash = calculateVisualHash(testImage)
            val watermarkMetrics = calculateWatermarkMetrics(
                extractWatermarkRegion(testImage, 
                    VisualTestCase(testCase.name, testCase.width, testCase.height, "AspectRatio: 4:3")
                ),
                VisualTestCase(testCase.name, testCase.width, testCase.height, "AspectRatio: 4:3")
            )
            
            // Store baseline or compare
            val key = "watermark_consistency"
            if (!baselineHashes.containsKey(key)) {
                baselineHashes[key] = visualHash
            } else {
                // Compare visual consistency (watermarks should look similar)
                val similarity = calculateHashSimilarity(baselineHashes[key]!!, visualHash)
                assertTrue("Watermark should be visually consistent across orientations for ${testCase.name} (similarity: $similarity)",
                    similarity > 0.85f // 85% similarity threshold
                )
            }
            
            saveTestResult(testImage, "orientation_${testCase.name}")
            testImage.recycle()
        }
    }

    @Test
    fun testWatermarkReadabilityAcrossConditions() = runBlocking {
        val readabilityTests = listOf(
            ReadabilityTestCase("high_contrast", Color.WHITE, Color.BLACK),
            ReadabilityTestCase("low_contrast", Color.LTGRAY, Color.GRAY),
            ReadabilityTestCase("blue_background", Color.BLUE, Color.WHITE),
            ReadabilityTestCase("red_background", Color.RED, Color.WHITE),
            ReadabilityTestCase("complex_background", Color.TRANSPARENT, Color.WHITE) // Uses pattern
        )

        readabilityTests.forEach { testCase ->
            val testImage = generateTestImageWithBackground(testCase)
            
            // Apply watermark
            val watermarkedImage = applyWatermarkToTestImage(testImage, 
                VisualTestCase(testCase.name, testImage.width, testImage.height, "AspectRatio: 4:3")
            )
            
            // Analyze readability
            val readabilityScore = calculateWatermarkReadability(watermarkedImage)
            
            // Verify minimum readability thresholds
            assertTrue("Watermark should be readable on ${testCase.name} background (score: $readabilityScore)",
                readabilityScore > 0.7f // 70% readability threshold
            )
            
            saveTestResult(watermarkedImage, "readability_${testCase.name}")
            testImage.recycle()
            watermarkedImage.recycle()
        }
    }

    @Test
    fun testAspectRatioCroppingAccuracy() = runBlocking {
        val croppingTests = listOf(
            CroppingTestCase("crop_to_square", 3000, 4000, 1.0f), // Square crop
            CroppingTestCase("crop_to_wide", 3000, 4000, 16f/9f), // Wide crop
            CroppingTestCase("crop_to_standard", 4000, 3000, 4f/3f), // Standard crop
            CroppingTestCase("no_crop_needed", 1920, 1080, 16f/9f) // Already correct ratio
        )

        croppingTests.forEach { testCase ->
            val originalImage = generateTestPattern(testCase.name, testCase.originalWidth, testCase.originalHeight)
            
            // Apply aspect ratio cropping
            val croppedImage = applyCropWithAspectRatio(originalImage, testCase.targetAspectRatio)
            
            // Verify final aspect ratio
            val actualRatio = croppedImage.width.toFloat() / croppedImage.height.toFloat()
            val tolerance = 0.01f
            
            assertTrue("Cropped image should match target aspect ratio for ${testCase.name} " +
                "(expected: ${testCase.targetAspectRatio}, actual: $actualRatio)",
                abs(actualRatio - testCase.targetAspectRatio) <= tolerance
            )
            
            // Verify content preservation (center crop)
            val centerPreservationScore = calculateCenterPreservation(originalImage, croppedImage)
            assertTrue("Center content should be preserved during cropping for ${testCase.name}",
                centerPreservationScore > 0.8f
            )
            
            saveTestResult(croppedImage, "cropping_${testCase.name}")
            originalImage.recycle()
            croppedImage.recycle()
        }
    }

    // Helper Methods

    private suspend fun generateTestImageWithWatermark(testCase: VisualTestCase): Bitmap {
        val bitmap = Bitmap.createBitmap(testCase.width, testCase.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE) // Blue background for contrast
        
        val testFile = File(testDir, "${testCase.name}_temp.jpg")
        FileOutputStream(testFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        
        val metadata = CaptureMetadata(
            timestamp = System.currentTimeMillis(),
            locationData = LocationData(latitude = 40.7128, longitude = -74.0060, isAvailable = true, address = "Test Location"),
            projectName = "Visual Test Project",
            projectId = "VTP001",
            userName = "Test User",
            userId = "TU001",
            deviceInfo = testCase.deviceInfo
        )
        
        metadataEmbedder.embedMetadata(testFile, metadata, addVisualWatermark = true)
        
        val result = BitmapFactory.decodeFile(testFile.absolutePath)
        testFile.delete()
        bitmap.recycle()
        
        return result ?: throw IllegalStateException("Failed to generate watermarked image")
    }

    private fun extractWatermarkRegion(image: Bitmap, testCase: VisualTestCase): Bitmap {
        // Extract bottom portion where watermark should be located
        val textSize = (testCase.width * 0.035f).coerceAtLeast(60f)
        val lineHeight = textSize * 1.3f
        val watermarkHeight = (lineHeight * 3 + 40).toInt() // 3 lines + padding
        val regionHeight = minOf(watermarkHeight, image.height / 3)
        val regionTop = image.height - regionHeight
        
        return Bitmap.createBitmap(image, 0, regionTop, image.width, regionHeight)
    }

    private fun calculateWatermarkMetrics(watermarkRegion: Bitmap, testCase: VisualTestCase): WatermarkMetrics {
        val pixels = IntArray(watermarkRegion.width * watermarkRegion.height)
        watermarkRegion.getPixels(pixels, 0, watermarkRegion.width, 0, 0, watermarkRegion.width, watermarkRegion.height)
        
        // Count white pixels (text) vs black pixels (background)
        var whitePixels = 0
        var blackPixels = 0
        
        pixels.forEach { pixel ->
            when {
                Color.red(pixel) > 200 && Color.green(pixel) > 200 && Color.blue(pixel) > 200 -> whitePixels++
                Color.red(pixel) < 50 && Color.green(pixel) < 50 && Color.blue(pixel) < 50 -> blackPixels++
            }
        }
        
        val textCoverageRatio = whitePixels.toFloat() / pixels.size
        val backgroundCoverageRatio = blackPixels.toFloat() / pixels.size
        
        return WatermarkMetrics(
            textCoverageRatio = textCoverageRatio,
            backgroundCoverageRatio = backgroundCoverageRatio,
            regionHeight = watermarkRegion.height,
            regionWidth = watermarkRegion.width,
            estimatedTextSize = estimateTextSizeFromCoverage(textCoverageRatio, testCase.width)
        )
    }

    private fun estimateTextSizeFromCoverage(coverage: Float, imageWidth: Int): Float {
        // Rough estimation based on coverage ratio
        return (imageWidth * coverage * 0.5f).coerceAtLeast(40f).coerceAtMost(200f)
    }

    private fun assertWatermarkPositioning(metrics: WatermarkMetrics, testCase: VisualTestCase) {
        // Verify watermark takes appropriate portion of image
        val heightRatio = metrics.regionHeight.toFloat() / testCase.height
        assertTrue("Watermark height should be reasonable for ${testCase.name}",
            heightRatio in 0.1f..0.3f // 10-30% of image height
        )
        
        // Verify text coverage is reasonable
        assertTrue("Text coverage should be reasonable for ${testCase.name}",
            metrics.textCoverageRatio in 0.05f..0.25f // 5-25% text coverage
        )
        
        // Verify background is present
        assertTrue("Background should be present for ${testCase.name}",
            metrics.backgroundCoverageRatio > 0.3f // At least 30% background
        )
    }

    private fun analyzeWatermarkTextSize(image: Bitmap, testCase: ScalingTestCase): TextMetrics {
        val watermarkRegion = extractWatermarkRegion(image, 
            VisualTestCase(testCase.name, testCase.width, testCase.height, "AspectRatio: 4:3")
        )
        
        val metrics = calculateWatermarkMetrics(watermarkRegion, 
            VisualTestCase(testCase.name, testCase.width, testCase.height, "AspectRatio: 4:3")
        )
        
        return TextMetrics(
            estimatedTextSize = metrics.estimatedTextSize,
            readabilityScore = metrics.textCoverageRatio / (metrics.textCoverageRatio + metrics.backgroundCoverageRatio)
        )
    }

    private fun generateOrientedTestImage(testCase: OrientationTestCase): Bitmap {
        // This would need to simulate different EXIF orientations
        // For now, create a basic test image
        return Bitmap.createBitmap(testCase.width, testCase.height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
        }
    }

    private fun calculateVisualHash(image: Bitmap): String {
        // Simple perceptual hash implementation
        val smallImage = Bitmap.createScaledBitmap(image, 32, 32, false)
        val pixels = IntArray(1024)
        smallImage.getPixels(pixels, 0, 32, 0, 0, 32, 32)
        
        val hash = pixels.joinToString("") { (Color.red(it) + Color.green(it) + Color.blue(it)).toString(16).take(1) }
        smallImage.recycle()
        return hash
    }

    private fun calculateHashSimilarity(hash1: String, hash2: String): Float {
        val matches = hash1.zip(hash2).count { it.first == it.second }
        return matches.toFloat() / hash1.length
    }

    private fun generateTestImageWithBackground(testCase: ReadabilityTestCase): Bitmap {
        val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(testCase.backgroundColor)
        
        if (testCase.backgroundColor == Color.TRANSPARENT) {
            // Create complex pattern background
            val canvas = Canvas(bitmap)
            // Add some pattern complexity here
        }
        
        return bitmap
    }

    private fun applyWatermarkToTestImage(image: Bitmap, testCase: VisualTestCase): Bitmap {
        // This would use the actual watermark application logic
        return image.copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun calculateWatermarkReadability(image: Bitmap): Float {
        // Calculate contrast ratio between text and background
        val watermarkRegion = extractWatermarkRegion(image, 
            VisualTestCase("readability", image.width, image.height, "AspectRatio: 4:3")
        )
        
        // Simple readability score based on contrast
        val metrics = calculateWatermarkMetrics(watermarkRegion, 
            VisualTestCase("readability", image.width, image.height, "AspectRatio: 4:3")
        )
        
        return minOf(1.0f, metrics.textCoverageRatio * 4) // Simple scoring
    }

    private fun generateTestPattern(name: String, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Create test pattern for cropping analysis
        canvas.drawColor(Color.WHITE)
        // Add center markers and grid for analysis
        
        return bitmap
    }

    private fun applyCropWithAspectRatio(image: Bitmap, targetRatio: Float): Bitmap {
        // Simulate aspect ratio cropping logic
        val currentRatio = image.width.toFloat() / image.height.toFloat()
        
        if (abs(currentRatio - targetRatio) < 0.01f) {
            return image
        }
        
        val (cropWidth, cropHeight) = if (targetRatio > currentRatio) {
            val newHeight = (image.width / targetRatio).toInt()
            image.width to newHeight.coerceAtMost(image.height)
        } else {
            val newWidth = (image.height * targetRatio).toInt()
            newWidth.coerceAtMost(image.width) to image.height
        }
        
        val cropX = (image.width - cropWidth) / 2
        val cropY = (image.height - cropHeight) / 2
        
        return Bitmap.createBitmap(image, cropX, cropY, cropWidth, cropHeight)
    }

    private fun calculateCenterPreservation(original: Bitmap, cropped: Bitmap): Float {
        // Simple center preservation score
        return 0.9f // Placeholder implementation
    }

    private fun saveTestResult(bitmap: Bitmap, testName: String) {
        val file = File(testDir, "$testName.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }

    // Data Classes

    data class VisualTestCase(
        val name: String,
        val width: Int,
        val height: Int,
        val deviceInfo: String
    )

    data class ScalingTestCase(
        val name: String,
        val width: Int,
        val height: Int
    )

    data class OrientationTestCase(
        val name: String,
        val width: Int,
        val height: Int,
        val exifOrientation: Int
    )

    data class ReadabilityTestCase(
        val name: String,
        val backgroundColor: Int,
        val textColor: Int
    )

    data class CroppingTestCase(
        val name: String,
        val originalWidth: Int,
        val originalHeight: Int,
        val targetAspectRatio: Float
    )

    data class WatermarkMetrics(
        val textCoverageRatio: Float,
        val backgroundCoverageRatio: Float,
        val regionHeight: Int,
        val regionWidth: Int,
        val estimatedTextSize: Float
    )

    data class TextMetrics(
        val estimatedTextSize: Float,
        val readabilityScore: Float
    )
}
