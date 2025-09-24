package com.hazardhawk.performance

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.CameraGalleryActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertTrue

/**
 * Specialized tests for large photo collection performance.
 * Tests scalability limits and performance degradation patterns.
 */
@RunWith(AndroidJUnit4::class)
class LargePhotoCollectionTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<CameraGalleryActivity>()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val performanceThresholds = PerformanceThresholds()
    
    @Before
    fun setup() {
        cleanupAllTestPhotos()
        System.gc() // Clear memory before tests
    }
    
    @After
    fun tearDown() {
        cleanupAllTestPhotos()
        System.gc()
    }
    
    @Test
    fun test_extremeLoad_1000Photos() {
        val photos = createLargePhotoCollection(1000, "extreme_load")
        
        try {
            val startTime = System.currentTimeMillis()
            val initialMemory = getMemoryUsage()
            
            // Open gallery with 1000 photos
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            val loadTime = System.currentTimeMillis() - startTime
            val memoryAfterLoad = getMemoryUsage()
            val memoryIncrease = memoryAfterLoad - initialMemory
            
            // Verify gallery loaded
            composeTestRule.onNodeWithText("Gallery (1000 photos)")
                .assertExists()
            
            // Performance assertions
            assertTrue("1000 photos should load within 10 seconds", 
                loadTime < 10000)
            assertTrue("Memory increase should be manageable (< 200MB)", 
                memoryIncrease < 200 * 1024 * 1024)
            
            // Test scrolling performance with large collection
            testScrollingPerformance(100) // Scroll to 100th item
            
            println("Extreme load test - Load time: ${loadTime}ms, Memory: ${memoryIncrease / (1024 * 1024)}MB")
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun test_scalabilityLimits_500Photos() {
        val photos = createLargePhotoCollection(500, "scalability")
        
        try {
            val measurements = PerformanceMeasurements()
            
            // Test initial load
            measurements.startMeasurement("initial_load")
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            composeTestRule.waitForIdle()
            measurements.endMeasurement("initial_load")
            
            // Test scroll to middle
            measurements.startMeasurement("scroll_middle")
            composeTestRule.onNodeWithTag("PhotoGrid")
                .performScrollToIndex(250)
            composeTestRule.waitForIdle()
            measurements.endMeasurement("scroll_middle")
            
            // Test scroll to end
            measurements.startMeasurement("scroll_end")
            composeTestRule.onNodeWithTag("PhotoGrid")
                .performScrollToIndex(499)
            composeTestRule.waitForIdle()
            measurements.endMeasurement("scroll_end")
            
            // Test selection mode with large collection
            measurements.startMeasurement("enable_selection")
            composeTestRule.onNodeWithContentDescription("Select photos")
                .performClick()
            composeTestRule.waitForIdle()
            measurements.endMeasurement("enable_selection")
            
            // Test selecting multiple photos
            measurements.startMeasurement("multi_select")
            for (i in 0 until 50 step 5) {
                val photo = photos[i]
                composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
                    .performClick()
            }
            composeTestRule.waitForIdle()
            measurements.endMeasurement("multi_select")
            
            // Verify selections
            composeTestRule.onNodeWithText("10 selected")
                .assertExists()
            
            // Performance assertions
            assertTrue("Initial load should be reasonable", 
                measurements.getMeasurement("initial_load") < performanceThresholds.largeCollectionLoad)
            assertTrue("Scrolling should remain smooth", 
                measurements.getMeasurement("scroll_middle") < performanceThresholds.smoothScroll)
            assertTrue("Selection mode should enable quickly", 
                measurements.getMeasurement("enable_selection") < performanceThresholds.selectionModeToggle)
            
            measurements.printSummary("Scalability Test - 500 Photos")
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun test_memoryPressure_largePhotos() {
        // Create fewer but larger photos to test memory pressure
        val photos = createLargePhotoCollection(100, "memory_pressure", largeFileSize = true)
        
        try {
            val runtime = Runtime.getRuntime()
            val initialMemory = getMemoryUsage()
            
            // Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            composeTestRule.waitForIdle()
            
            val afterLoadMemory = getMemoryUsage()
            
            // Scroll through to force thumbnail loading
            for (i in 0 until 100 step 10) {
                composeTestRule.onNodeWithTag("PhotoGrid")
                    .performScrollToIndex(i)
                composeTestRule.waitForIdle()
            }
            
            val peakMemory = getMemoryUsage()
            
            // Navigate away to test memory cleanup
            composeTestRule.onNodeWithText("← Back")
                .performClick()
            composeTestRule.waitForIdle()
            
            runtime.gc()
            Thread.sleep(1000) // Allow GC to complete
            val finalMemory = getMemoryUsage()
            
            val loadMemoryIncrease = (afterLoadMemory - initialMemory) / (1024 * 1024)
            val peakMemoryIncrease = (peakMemory - initialMemory) / (1024 * 1024)
            val memoryRecovered = (peakMemory - finalMemory) / (1024 * 1024)
            
            // Memory pressure assertions
            assertTrue("Initial load memory should be reasonable (< 150MB)", 
                loadMemoryIncrease < 150)
            assertTrue("Peak memory should not exceed limits (< 300MB)", 
                peakMemoryIncrease < 300)
            assertTrue("Should recover significant memory when leaving (> 50MB)", 
                memoryRecovered > 50)
            
            println("Memory pressure test - Load: ${loadMemoryIncrease}MB, Peak: ${peakMemoryIncrease}MB, Recovered: ${memoryRecovered}MB")
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun test_continuousUsage_stressTest() {
        val photos = createLargePhotoCollection(200, "stress_test")
        
        try {
            val startTime = System.currentTimeMillis()
            val initialMemory = getMemoryUsage()
            
            // Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            composeTestRule.waitForIdle()
            
            // Simulate continuous usage patterns
            repeat(10) { iteration ->
                // Random scrolling
                val randomIndex = (0..199).random()
                composeTestRule.onNodeWithTag("PhotoGrid")
                    .performScrollToIndex(randomIndex)
                composeTestRule.waitForIdle()
                
                // Toggle selection mode
                composeTestRule.onNodeWithContentDescription("Select photos")
                    .performClick()
                composeTestRule.waitForIdle()
                
                // Select random photos
                repeat(5) {
                    val photo = photos.random()
                    try {
                        composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
                            .performClick()
                    } catch (e: Exception) {
                        // Photo might not be visible, continue
                    }
                }
                
                composeTestRule.waitForIdle()
                
                // Toggle selection mode off
                composeTestRule.onNodeWithContentDescription("Select photos")
                    .performClick()
                composeTestRule.waitForIdle()
                
                // Check memory usage periodically
                if (iteration % 3 == 0) {
                    val currentMemory = getMemoryUsage()
                    val memoryIncrease = (currentMemory - initialMemory) / (1024 * 1024)
                    
                    assertTrue("Memory should not continuously grow (iteration $iteration): ${memoryIncrease}MB", 
                        memoryIncrease < 400)
                }
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            val finalMemory = getMemoryUsage()
            val totalMemoryIncrease = (finalMemory - initialMemory) / (1024 * 1024)
            
            // Stress test assertions
            assertTrue("Continuous usage should complete in reasonable time (< 30s)", 
                totalTime < 30000)
            assertTrue("Memory usage should remain stable (< 500MB total)", 
                totalMemoryIncrease < 500)
            
            println("Stress test - Total time: ${totalTime}ms, Memory increase: ${totalMemoryIncrease}MB")
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun test_backgroundPressure_multitasking() {
        val photos = createLargePhotoCollection(300, "background_test")
        
        try {
            // Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            composeTestRule.waitForIdle()
            
            // Simulate background pressure by triggering GC and memory allocation
            val backgroundTasks = mutableListOf<ByteArray>()
            
            val startTime = System.currentTimeMillis()
            
            // Perform gallery operations while creating memory pressure
            repeat(5) { iteration ->
                // Add background memory pressure
                backgroundTasks.add(ByteArray(10 * 1024 * 1024)) // 10MB allocation
                
                // Perform gallery operations
                composeTestRule.onNodeWithTag("PhotoGrid")
                    .performScrollToIndex(iteration * 50)
                composeTestRule.waitForIdle()
                
                // Enable selection mode
                composeTestRule.onNodeWithContentDescription("Select photos")
                    .performClick()
                composeTestRule.waitForIdle()
                
                // Select some photos
                for (i in 0 until 5) {
                    val photo = photos[iteration * 10 + i]
                    try {
                        composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
                            .performClick()
                    } catch (e: Exception) {
                        // Photo might not be visible
                    }
                }
                
                composeTestRule.waitForIdle()
                
                // Clear selections
                composeTestRule.onNodeWithContentDescription("Select photos")
                    .performClick()
                composeTestRule.waitForIdle()
                
                // Force some memory pressure
                System.gc()
            }
            
            val operationTime = System.currentTimeMillis() - startTime
            
            // Clean up background allocations
            backgroundTasks.clear()
            System.gc()
            
            // Performance under pressure assertions
            assertTrue("Operations should complete despite memory pressure (< 15s)", 
                operationTime < 15000)
            
            // Verify gallery is still functional
            composeTestRule.onNodeWithText("Gallery (300 photos)")
                .assertExists()
            
            println("Background pressure test - Operation time: ${operationTime}ms")
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun test_concurrentOperations_largeScale() {
        val photos = createLargePhotoCollection(400, "concurrent_large")
        
        try {
            // Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            composeTestRule.waitForIdle()
            
            val startTime = System.currentTimeMillis()
            
            // Enable selection mode
            composeTestRule.onNodeWithContentDescription("Select photos")
                .performClick()
            composeTestRule.waitForIdle()
            
            // Perform concurrent-like operations (scroll + select + search simulation)
            for (i in 0 until 50) {
                // Scroll to different positions
                if (i % 5 == 0) {
                    composeTestRule.onNodeWithTag("PhotoGrid")
                        .performScrollToIndex(i * 8)
                    composeTestRule.waitForIdle()
                }
                
                // Select photos
                if (i < photos.size) {
                    val photo = photos[i]
                    try {
                        composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
                            .performClick()
                    } catch (e: Exception) {
                        // Photo might not be visible, continue
                    }
                }
                
                // Simulate search operation every 10 iterations
                if (i % 10 == 0) {
                    // This would test search if implemented
                    composeTestRule.waitForIdle()
                }
            }
            
            val operationTime = System.currentTimeMillis() - startTime
            
            // Verify some selections were made
            // Note: Exact count may vary based on visibility
            composeTestRule.onNodeWithText(Regex("\\d+ selected"))
                .assertExists()
            
            // Performance assertions
            assertTrue("Concurrent operations should complete efficiently (< 10s)", 
                operationTime < 10000)
            
            println("Concurrent operations test - Time: ${operationTime}ms")
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    // Helper methods
    private fun createLargePhotoCollection(count: Int, prefix: String, largeFileSize: Boolean = false): List<File> {
        val hazardHawkDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "HazardHawk"
        )
        if (!hazardHawkDir.exists()) {
            hazardHawkDir.mkdirs()
        }
        
        return (1..count).map { index ->
            File(hazardHawkDir, "HH_${prefix}_${index}.jpg").apply {
                val fileSize = if (largeFileSize) 2048 else 800
                writeBytes(createMockJpegData(fileSize))
            }
        }
    }
    
    private fun createMockJpegData(size: Int): ByteArray {
        val header = byteArrayOf(
            0xFF.toByte(), 0xD8.toByte(),
            0xFF.toByte(), 0xE0.toByte(),
            0x00.toByte(), 0x10.toByte(),
            0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte(), 0x00.toByte(),
            0x01.toByte(), 0x01.toByte(),
            0x01.toByte(),
            0x00.toByte(), 0x48.toByte(),
            0x00.toByte(), 0x48.toByte(),
            0x00.toByte(), 0x00.toByte(),
            0xFF.toByte(), 0xD9.toByte()
        )
        
        val paddingSize = (size * size / 8).coerceAtLeast(1000)
        val padding = ByteArray(paddingSize) { (it % 256).toByte() }
        
        return header + padding
    }
    
    private fun testScrollingPerformance(targetIndex: Int) {
        val startTime = System.currentTimeMillis()
        
        composeTestRule.onNodeWithTag("PhotoGrid")
            .performScrollToIndex(targetIndex)
        
        composeTestRule.waitForIdle()
        
        val scrollTime = System.currentTimeMillis() - startTime
        
        assertTrue("Scrolling to index $targetIndex should be smooth (< 1s)", 
            scrollTime < 1000)
    }
    
    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    private fun cleanupAllTestPhotos() {
        val hazardHawkDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "HazardHawk"
        )
        
        hazardHawkDir.listFiles()?.filter { file ->
            listOf("extreme_load", "scalability", "memory_pressure", "stress_test", 
                   "background_test", "concurrent_large").any { pattern ->
                file.name.contains(pattern)
            }
        }?.forEach { it.delete() }
    }
}

// Performance measurement utilities
class PerformanceMeasurements {
    private val measurements = mutableMapOf<String, Long>()
    private val startTimes = mutableMapOf<String, Long>()
    
    fun startMeasurement(key: String) {
        startTimes[key] = System.currentTimeMillis()
    }
    
    fun endMeasurement(key: String) {
        val startTime = startTimes[key] ?: return
        measurements[key] = System.currentTimeMillis() - startTime
    }
    
    fun getMeasurement(key: String): Long {
        return measurements[key] ?: 0L
    }
    
    fun printSummary(testName: String) {
        println("Performance Summary - $testName:")
        measurements.forEach { (key, value) ->
            println("  $key: ${value}ms")
        }
    }
}

class PerformanceThresholds {
    val largeCollectionLoad = 8000L // 8 seconds
    val smoothScroll = 500L // 500ms
    val selectionModeToggle = 300L // 300ms
    val thumbnailGeneration = 2000L // 2 seconds
    val searchOperation = 1000L // 1 second
}