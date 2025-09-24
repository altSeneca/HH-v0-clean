package com.hazardhawk.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.CameraGalleryActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertTrue

/**
 * Performance benchmark tests for gallery functionality.
 * Tests loading times, scroll performance, and memory usage with large photo collections.
 */
@RunWith(AndroidJUnit4::class)
class GalleryPerformanceTest {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<CameraGalleryActivity>()
    
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    @Before
    fun setup() {
        // Clean up before performance tests
        cleanupTestPhotos()
    }
    
    @Test
    fun benchmark_galleryLaunch_smallCollection() {
        // Setup: Create 10 photos
        val photos = createTestPhotos(10, "small_collection")
        
        try {
            benchmarkRule.measureRepeated {
                // Launch gallery and measure time to load
                composeTestRule.onNodeWithText("🖼️ View Gallery")
                    .performClick()
                
                composeTestRule.waitForIdle()
                
                // Verify gallery loaded
                composeTestRule.onNodeWithText("Gallery (10 photos)")
                    .assertExists()
                
                // Navigate back for next iteration
                composeTestRule.onNodeWithText("← Back")
                    .performClick()
                
                composeTestRule.waitForIdle()
            }
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun benchmark_galleryLaunch_mediumCollection() {
        // Setup: Create 50 photos
        val photos = createTestPhotos(50, "medium_collection")
        
        try {
            benchmarkRule.measureRepeated {
                composeTestRule.onNodeWithText("🖼️ View Gallery")
                    .performClick()
                
                composeTestRule.waitForIdle()
                
                composeTestRule.onNodeWithText("Gallery (50 photos)")
                    .assertExists()
                
                composeTestRule.onNodeWithText("← Back")
                    .performClick()
                
                composeTestRule.waitForIdle()
            }
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun benchmark_galleryLaunch_largeCollection() {
        // Setup: Create 100 photos  
        val photos = createTestPhotos(100, "large_collection")
        
        try {
            benchmarkRule.measureRepeated {
                composeTestRule.onNodeWithText("🖼️ View Gallery")
                    .performClick()
                
                composeTestRule.waitForIdle()
                
                composeTestRule.onNodeWithText("Gallery (100 photos)")
                    .assertExists()
                
                composeTestRule.onNodeWithText("← Back")
                    .performClick()
                
                composeTestRule.waitForIdle()
            }
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun benchmark_scrollPerformance_largeCollection() {
        val photos = createTestPhotos(200, "scroll_test")
        
        try {
            // Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            benchmarkRule.measureRepeated {
                // Scroll through the entire collection
                val photoGrid = composeTestRule.onNodeWithTag("PhotoGrid")
                
                // Scroll to end
                photoGrid.performScrollToIndex(199)
                composeTestRule.waitForIdle()
                
                // Scroll back to start
                photoGrid.performScrollToIndex(0)
                composeTestRule.waitForIdle()
            }
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun benchmark_selectionMode_performance() {
        val photos = createTestPhotos(50, "selection_perf")
        
        try {
            // Open gallery and enable selection mode
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            composeTestRule.onNodeWithContentDescription("Select photos")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            benchmarkRule.measureRepeated {
                // Select all photos rapidly
                for (i in 0 until 25) { // Select half for performance
                    val photo = photos[i]
                    composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
                        .performClick()
                }
                
                composeTestRule.waitForIdle()
                
                // Clear selections
                composeTestRule.onNodeWithContentDescription("Select photos")
                    .performClick()
                
                composeTestRule.waitForIdle()
                
                composeTestRule.onNodeWithContentDescription("Select photos")
                    .performClick()
                
                composeTestRule.waitForIdle()
            }
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun test_memoryUsage_largeCollection() {
        val photos = createTestPhotos(100, "memory_test")
        
        try {
            val runtime = Runtime.getRuntime()
            runtime.gc()
            val initialMemory = runtime.totalMemory() - runtime.freeMemory()
            
            // Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Scroll through photos to load thumbnails
            val photoGrid = composeTestRule.onNodeWithTag("PhotoGrid")
            for (i in 0 until 100 step 10) {
                photoGrid.performScrollToIndex(i)
                composeTestRule.waitForIdle()
            }
            
            runtime.gc()
            val peakMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryIncrease = (peakMemory - initialMemory) / (1024 * 1024) // MB
            
            // Navigate away to test memory cleanup
            composeTestRule.onNodeWithText("← Back")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            runtime.gc()
            val finalMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryRecovered = (peakMemory - finalMemory) / (1024 * 1024) // MB
            
            // Performance assertions
            assertTrue("Memory increase should be reasonable (< 100MB for 100 photos)", 
                memoryIncrease < 100)
            assertTrue("Should recover some memory when leaving gallery", 
                memoryRecovered > 0)
            
            println("Memory increase: ${memoryIncrease}MB, Memory recovered: ${memoryRecovered}MB")
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun test_thumbnailGeneration_performance() {
        // Create photos with varying sizes
        val smallPhotos = createTestPhotos(10, "small", 512) // 512x512
        val mediumPhotos = createTestPhotos(10, "medium", 1024) // 1024x1024
        val largePhotos = createTestPhotos(10, "large", 2048) // 2048x2048
        
        val allPhotos = smallPhotos + mediumPhotos + largePhotos
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Open gallery - this should trigger thumbnail generation
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Verify all photos loaded
            composeTestRule.onNodeWithText("Gallery (30 photos)")
                .assertExists()
            
            val loadTime = System.currentTimeMillis() - startTime
            
            // Performance assertion
            assertTrue("Thumbnail generation should complete within 5 seconds", 
                loadTime < 5000)
            
            println("Thumbnail generation time for 30 photos: ${loadTime}ms")
            
        } finally {
            allPhotos.forEach { it.delete() }
        }
    }
    
    @Test
    fun benchmark_searchPerformance_largeDataset() {
        val photos = createTestPhotos(100, "search_test")
        
        try {
            // Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            benchmarkRule.measureRepeated {
                // Perform search
                composeTestRule.onNodeWithContentDescription("Search photos")
                    .performTextInput("search_test")
                
                composeTestRule.waitForIdle()
                
                // Clear search
                composeTestRule.onNodeWithContentDescription("Search photos")
                    .performTextClearance()
                
                composeTestRule.waitForIdle()
            }
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun test_concurrentOperations_performance() {
        val photos = createTestPhotos(50, "concurrent_test")
        
        try {
            // Open gallery
            composeTestRule.onNodeWithText("🖼️ View Gallery")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            val startTime = System.currentTimeMillis()
            
            // Simulate concurrent operations
            // Enable selection mode
            composeTestRule.onNodeWithContentDescription("Select photos")
                .performClick()
            
            composeTestRule.waitForIdle()
            
            // Select multiple photos while scrolling
            for (i in 0 until 20) {
                if (i % 5 == 0) {
                    // Scroll occasionally
                    composeTestRule.onNodeWithTag("PhotoGrid")
                        .performScrollToIndex(i + 10)
                }
                
                val photo = photos[i]
                composeTestRule.onNodeWithContentDescription("Select ${photo.nameWithoutExtension}")
                    .performClick()
            }
            
            composeTestRule.waitForIdle()
            
            val operationTime = System.currentTimeMillis() - startTime
            
            // Verify operations completed
            composeTestRule.onNodeWithText("20 selected")
                .assertExists()
            
            // Performance assertion
            assertTrue("Concurrent operations should complete efficiently (< 3s)", 
                operationTime < 3000)
            
            println("Concurrent operations time: ${operationTime}ms")
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    @Test
    fun test_rapidNavigation_performance() {
        val photos = createTestPhotos(30, "nav_test")
        
        try {
            val startTime = System.currentTimeMillis()
            
            // Rapidly navigate in and out of gallery multiple times
            repeat(5) {
                composeTestRule.onNodeWithText("🖼️ View Gallery")
                    .performClick()
                
                composeTestRule.waitForIdle()
                
                composeTestRule.onNodeWithText("← Back")
                    .performClick()
                
                composeTestRule.waitForIdle()
            }
            
            val totalTime = System.currentTimeMillis() - startTime
            
            // Performance assertion
            assertTrue("Rapid navigation should be smooth (< 5s for 5 round trips)", 
                totalTime < 5000)
            
            println("Rapid navigation time: ${totalTime}ms for 5 round trips")
            
        } finally {
            photos.forEach { it.delete() }
        }
    }
    
    // Helper methods
    private fun createTestPhotos(count: Int, prefix: String, size: Int = 800): List<File> {
        val hazardHawkDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "HazardHawk"
        )
        if (!hazardHawkDir.exists()) {
            hazardHawkDir.mkdirs()
        }
        
        return (1..count).map { index ->
            File(hazardHawkDir, "HH_${prefix}_${index}_${System.currentTimeMillis()}.jpg").apply {
                writeBytes(createMockJpegData(size))
            }
        }
    }
    
    private fun createMockJpegData(size: Int = 800): ByteArray {
        // Create larger mock data for different sizes
        val baseData = byteArrayOf(
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
        
        // Pad with data to simulate different file sizes
        val padding = ByteArray((size * size / 10).coerceAtLeast(100)) { 0x00 }
        return baseData + padding
    }
    
    private fun cleanupTestPhotos() {
        val hazardHawkDir = File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
            "HazardHawk"
        )
        
        hazardHawkDir.listFiles()?.filter { 
            it.name.contains("collection") || 
            it.name.contains("scroll_test") ||
            it.name.contains("selection_perf") ||
            it.name.contains("memory_test") ||
            it.name.contains("search_test") ||
            it.name.contains("concurrent_test") ||
            it.name.contains("nav_test")
        }?.forEach { it.delete() }
    }
}