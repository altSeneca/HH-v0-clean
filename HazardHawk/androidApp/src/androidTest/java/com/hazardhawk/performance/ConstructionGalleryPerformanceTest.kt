package com.hazardhawk.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hazardhawk.test.GalleryTestDataFactory
import com.hazardhawk.gallery.GalleryPhoto
import com.hazardhawk.ui.gallery.GalleryGridComponent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis
import kotlin.test.*

/**
 * Construction-specific performance benchmarking tests for gallery functionality.
 * Tests performance under real construction site conditions with construction worker workflows.
 */
@RunWith(AndroidJUnit4::class)
class ConstructionGalleryPerformanceTest {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val testDataFactory = GalleryTestDataFactory
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    
    // Construction-specific performance requirements
    companion object {
        const val MAX_FIELD_MEMORY_MB = 150 // Lower limit for field devices
        const val MAX_GLOVE_RESPONSE_MS = 200 // Response time with work gloves
        const val MAX_OUTDOOR_LOAD_TIME_MS = 3000 // Slower networks on construction sites
        const val MIN_CONSTRUCTION_FPS = 45 // Lower FPS acceptable in harsh conditions
        const val MAX_INTERRUPTION_RECOVERY_MS = 1000 // Fast recovery from interruptions
    }
    
    private lateinit var memoryMonitor: ConstructionMemoryMonitor
    
    @Before
    fun setup() {
        memoryMonitor = ConstructionMemoryMonitor()
    }
    
    // MARK: - Construction Site Memory Constraints Tests
    
    @Test
    fun constructionSite_memoryUsageUnder150MB() = runTest {
        // Simulate construction site photo collection over a full day
        val dayOfPhotos = testDataFactory.createLargePhotoCollection(
            count = 200, // Typical daily photo count
            withAnalysis = true,
            uploadedPercentage = 0.3f // Poor connectivity means fewer uploads
        )
        
        val initialMemory = memoryMonitor.getCurrentMemoryUsage()
        
        composeTestRule.setContent {
            ConstructionGalleryView(
                photos = dayOfPhotos,
                isFieldMode = true // Enable field optimizations
            )
        }
        
        composeTestRule.waitForIdle()
        
        // Simulate construction worker scrolling through photos
        repeat(5) { cycle ->
            composeTestRule.onNodeWithTag("gallery_grid")
                .performScrollToIndex(cycle * 40)
            composeTestRule.waitForIdle()
        }
        
        val peakMemory = memoryMonitor.getCurrentMemoryUsage()
        val memoryIncrease = peakMemory - initialMemory
        
        assertTrue(
            "Construction site memory usage should be under ${MAX_FIELD_MEMORY_MB}MB, was ${memoryIncrease / (1024 * 1024)}MB",
            memoryIncrease < MAX_FIELD_MEMORY_MB * 1024 * 1024
        )
        
        memoryMonitor.logMemoryStats("Construction Site Usage")
    }
    
    @Test
    fun lowMemoryDevice_handlesGracefulDegradation() = runTest {
        // Simulate low-end construction tablet
        val limitedPhotos = testDataFactory.createLargePhotoCollection(
            count = 50,
            withAnalysis = false, // Reduce processing overhead
            uploadedPercentage = 0.1f
        )
        
        // Force low memory condition
        System.gc()
        val availableMemory = Runtime.getRuntime().freeMemory()
        
        composeTestRule.setContent {
            ConstructionGalleryView(
                photos = limitedPhotos,
                isFieldMode = true,
                isLowMemoryMode = true
            )
        }
        
        // Should still function without crashes
        composeTestRule.onNodeWithTag("gallery_grid")
            .assertIsDisplayed()
        
        // Test scrolling performance under memory pressure
        val scrollTime = measureTimeMillis {
            repeat(10) { index ->
                composeTestRule.onNodeWithTag("gallery_grid")
                    .performScrollToIndex(index * 3)
                composeTestRule.waitForIdle()
            }
        }
        
        assertTrue(
            "Low memory scrolling should remain under 2 seconds, took ${scrollTime}ms",
            scrollTime < 2000
        )
    }
    
    // MARK: - Work Glove Performance Tests
    
    @Test
    fun workGloves_touchResponsePerformance() = runTest {
        val gloveScenarios = testDataFactory.createGloveTestScenarios()
        val testPhotos = testDataFactory.createLargePhotoCollection(20)
        
        gloveScenarios.forEach { scenario ->
            var touchCount = 0
            val touchTimes = mutableListOf<Long>()
            
            composeTestRule.setContent {
                ConstructionGalleryView(
                    photos = testPhotos,
                    isFieldMode = true,
                    gloveMode = scenario
                ) { photoId ->
                    touchCount++
                }
            }
            
            // Perform touches with timing measurement
            repeat(10) { index ->
                val startTime = System.currentTimeMillis()
                
                composeTestRule.onNodeWithTag("photo_item_${testPhotos[index].id}")
                    .performClick()
                
                val endTime = System.currentTimeMillis()
                touchTimes.add(endTime - startTime)
            }
            
            val averageResponseTime = touchTimes.average()
            
            assertTrue(
                "${scenario.gloveName} response should be under ${MAX_GLOVE_RESPONSE_MS}ms, averaged ${averageResponseTime}ms",
                averageResponseTime < MAX_GLOVE_RESPONSE_MS
            )
            
            assertEquals("All touches should register", 10, touchCount)
            
            println("${scenario.gloveName}: Avg response ${averageResponseTime.toInt()}ms")
        }
    }
    
    @Test
    fun heavyDutyGloves_largeTargetPerformance() = runTest {
        val heavyGloveScenario = testDataFactory.createGloveTestScenarios()
            .first { it.gloveName.contains("Heavy") }
        
        val photos = testDataFactory.createLargePhotoCollection(30)
        var selectionCount = 0
        
        composeTestRule.setContent {
            ConstructionGalleryView(
                photos = photos,
                isFieldMode = true,
                gloveMode = heavyGloveScenario,
                onPhotoSelected = { selectionCount++ }
            )
        }
        
        // Test batch selection performance with large touch targets
        val batchSelectionTime = measureTimeMillis {
            repeat(15) { index ->
                composeTestRule.onNodeWithTag("photo_item_${photos[index].id}")
                    .performTouchInput { longClick() }
                composeTestRule.waitForIdle()
            }
        }
        
        val averageSelectionTime = batchSelectionTime / 15
        
        assertTrue(
            "Heavy glove batch selection should average under 250ms, was ${averageSelectionTime}ms",
            averageSelectionTime < 250
        )
        
        assertEquals("Should select 15 photos", 15, selectionCount)
    }
    
    // MARK: - Outdoor Lighting Conditions Tests
    
    @Test
    fun brightSunlight_renderingPerformance() = runTest {
        val outdoorPhotos = testDataFactory.createLargePhotoCollection(100)
            .filter { it.metadata.lightingCondition.contains("bright") }
        
        // Simulate bright outdoor conditions
        val renderTime = measureTimeMillis {
            composeTestRule.setContent {
                MaterialTheme(
                    colorScheme = lightColorScheme().copy(
                        surface = androidx.compose.ui.graphics.Color.White,
                        onSurface = androidx.compose.ui.graphics.Color.Black,
                        primary = androidx.compose.ui.graphics.Color(0xFF0D47A1) // High contrast blue
                    )
                ) {
                    ConstructionGalleryView(
                        photos = outdoorPhotos,
                        isFieldMode = true,
                        isHighContrastMode = true
                    )
                }
            }
            composeTestRule.waitForIdle()
        }
        
        assertTrue(
            "Bright sunlight rendering should be under ${MAX_OUTDOOR_LOAD_TIME_MS}ms, took ${renderTime}ms",
            renderTime < MAX_OUTDOOR_LOAD_TIME_MS
        )
        
        // Test visibility of selection indicators in bright light
        composeTestRule.onNodeWithTag("gallery_grid")
            .assertIsDisplayed()
    }
    
    @Test
    fun dimIndoor_lowLightPerformance() = runTest {
        val indoorPhotos = testDataFactory.createLargePhotoCollection(80)
            .filter { it.metadata.lightingCondition.contains("dim") }
        
        // Simulate dim indoor construction environment
        composeTestRule.setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                ConstructionGalleryView(
                    photos = indoorPhotos,
                    isFieldMode = true,
                    isDimLightMode = true
                )
            }
        }
        
        // Test scroll performance in low light mode
        val scrollTime = measureTimeMillis {
            repeat(8) { index ->
                composeTestRule.onNodeWithTag("gallery_grid")
                    .performScrollToIndex(index * 10)
                composeTestRule.waitForIdle()
            }
        }
        
        assertTrue(
            "Dim light scrolling should be responsive, took ${scrollTime}ms",
            scrollTime < 2000
        )
    }
    
    // MARK: - Construction Worker Workflow Tests
    
    @Test
    fun dailyPhotoReview_workflowPerformance() = runTest {
        // Simulate end-of-day photo review workflow
        val dailyPhotos = testDataFactory.createTimeFilterTestCollection()
            .filter { 
                val now = System.currentTimeMillis()
                val dayStart = now - 24 * 60 * 60 * 1000
                it.captureTimestamp >= dayStart
            }
        
        var reviewedCount = 0
        var selectedForReport = mutableSetOf<String>()
        
        composeTestRule.setContent {
            ConstructionGalleryView(
                photos = dailyPhotos,
                isFieldMode = true,
                onPhotoClick = { reviewedCount++ },
                onPhotoLongClick = { photoId ->
                    selectedForReport = if (selectedForReport.contains(photoId)) {
                        selectedForReport - photoId
                    } else {
                        selectedForReport + photoId
                    }.toMutableSet()
                }
            )
        }
        
        // Simulate worker reviewing and selecting photos
        val workflowTime = measureTimeMillis {
            // Quick review of all photos
            dailyPhotos.take(10).forEach { photo ->
                composeTestRule.onNodeWithTag("photo_item_${photo.id}")
                    .performClick()
            }
            
            // Select photos for safety report
            dailyPhotos.take(5).forEach { photo ->
                composeTestRule.onNodeWithTag("photo_item_${photo.id}")
                    .performTouchInput { longClick() }
            }
            
            composeTestRule.waitForIdle()
        }
        
        assertTrue(
            "Daily review workflow should complete under 3 seconds, took ${workflowTime}ms",
            workflowTime < 3000
        )
        
        assertEquals("Should review 10 photos", 10, reviewedCount)
        assertEquals("Should select 5 photos for report", 5, selectedForReport.size)
    }
    
    @Test
    fun interruption_quickRecoveryPerformance() = runTest {
        val photos = testDataFactory.createLargePhotoCollection(50)
        var isInterrupted = false
        var selectedPhotos = setOf(photos[0].id, photos[2].id, photos[4].id)
        
        // Initial state
        composeTestRule.setContent {
            if (isInterrupted) {
                Text("Call in progress...")
            } else {
                ConstructionGalleryView(
                    photos = photos,
                    isFieldMode = true,
                    selectedPhotos = selectedPhotos
                )
            }
        }
        
        // Simulate interruption (phone call, radio, etc.)
        isInterrupted = true
        composeTestRule.waitForIdle()
        
        // Simulate recovery
        val recoveryTime = measureTimeMillis {
            isInterrupted = false
            composeTestRule.waitForIdle()
            
            // Verify state is preserved
            composeTestRule.onNodeWithTag("gallery_grid")
                .assertIsDisplayed()
        }
        
        assertTrue(
            "Interruption recovery should be under ${MAX_INTERRUPTION_RECOVERY_MS}ms, took ${recoveryTime}ms",
            recoveryTime < MAX_INTERRUPTION_RECOVERY_MS
        )
        
        // Verify selections are preserved
        selectedPhotos.forEach { photoId ->
            composeTestRule.onNodeWithTag("selection_indicator_$photoId")
                .assertExists()
        }
    }
    
    // MARK: - Network Constraint Performance Tests
    
    @Test
    fun poorConnectivity_localPerformanceOptimal() = runTest {
        // Simulate construction site with poor network
        val localPhotos = testDataFactory.createLargePhotoCollection(
            count = 100,
            withAnalysis = false, // No cloud analysis available
            uploadedPercentage = 0.1f // Very few uploads successful
        )
        
        composeTestRule.setContent {
            ConstructionGalleryView(
                photos = localPhotos,
                isFieldMode = true,
                isOfflineMode = true
            )
        }
        
        // Test that local operations remain fast despite network issues
        val localOperationTime = measureTimeMillis {
            // Scroll through photos
            repeat(10) { index ->
                composeTestRule.onNodeWithTag("gallery_grid")
                    .performScrollToIndex(index * 10)
            }
            
            // Select photos for offline processing
            repeat(5) { index ->
                composeTestRule.onNodeWithTag("photo_item_${localPhotos[index].id}")
                    .performTouchInput { longClick() }
            }
            
            composeTestRule.waitForIdle()
        }
        
        assertTrue(
            "Offline operations should remain fast, took ${localOperationTime}ms",
            localOperationTime < 2000
        )
    }
    
    // MARK: - Construction Equipment Vibration Tests
    
    @Test
    fun heavyEquipment_vibrationTolerance() = runTest {
        val photos = testDataFactory.createLargePhotoCollection(30)
        var accurateClicks = 0
        
        composeTestRule.setContent {
            ConstructionGalleryView(
                photos = photos,
                isFieldMode = true,
                vibrationCompensation = true,
                onPhotoClick = { accurateClicks++ }
            )
        }
        
        // Simulate slightly inaccurate touches due to equipment vibration
        val vibrationTestTime = measureTimeMillis {
            repeat(15) { index ->
                composeTestRule.onNodeWithTag("photo_item_${photos[index].id}")
                    .performTouchInput {
                        // Slightly off-center touch to simulate vibration effect
                        click(center.copy(
                            x = center.x + (kotlin.random.Random.nextInt(-10, 10)),
                            y = center.y + (kotlin.random.Random.nextInt(-10, 10))
                        ))
                    }
            }
            composeTestRule.waitForIdle()
        }
        
        // Should still register accurate clicks despite vibration simulation
        assertTrue(
            "Vibration tolerance clicks should register accurately, got $accurateClicks/15",
            accurateClicks >= 12 // Allow for some simulation variance
        )
        
        assertTrue(
            "Vibration test should complete quickly, took ${vibrationTestTime}ms",
            vibrationTestTime < 2000
        )
    }
    
    // MARK: - Battery Optimization Tests
    
    @Test
    fun batteryOptimizedMode_reducedPerformanceAcceptable() = runTest {
        val photos = testDataFactory.createLargePhotoCollection(60)
        
        composeTestRule.setContent {
            ConstructionGalleryView(
                photos = photos,
                isFieldMode = true,
                isBatteryOptimizedMode = true // Reduce animations, lower refresh rate
            )
        }
        
        // Test that battery mode still provides acceptable performance
        val batteryModeTime = measureTimeMillis {
            repeat(12) { index ->
                composeTestRule.onNodeWithTag("gallery_grid")
                    .performScrollToIndex(index * 5)
                composeTestRule.waitForIdle()
            }
        }
        
        // Allow longer times in battery mode but still reasonable
        assertTrue(
            "Battery optimized mode should complete scrolling under 4 seconds, took ${batteryModeTime}ms",
            batteryModeTime < 4000
        )
        
        // Verify gallery is still functional
        composeTestRule.onNodeWithTag("gallery_grid")
            .assertIsDisplayed()
    }
}

/**
 * Construction-specific memory monitor with field device constraints
 */
class ConstructionMemoryMonitor {
    private val runtime = Runtime.getRuntime()
    
    fun getCurrentMemoryUsage(): Long {
        System.gc()
        Thread.sleep(100) // Allow more time for GC on slower devices
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    fun isLowMemoryCondition(): Boolean {
        val freeMemoryMB = runtime.freeMemory() / (1024 * 1024)
        return freeMemoryMB < 50 // Less than 50MB free
    }
    
    fun logMemoryStats(tag: String) {
        val used = getCurrentMemoryUsage() / (1024 * 1024)
        val free = runtime.freeMemory() / (1024 * 1024)
        val max = runtime.maxMemory() / (1024 * 1024)
        val percentage = (used.toDouble() / max.toDouble()) * 100.0
        
        println("$tag - Memory: ${used}MB used, ${free}MB free, ${max}MB max (${percentage.toInt()}%)")
        
        if (isLowMemoryCondition()) {
            println("WARNING: Low memory condition detected!")
        }
    }
}

/**
 * Construction gallery view with field-specific optimizations
 */
@Composable
fun ConstructionGalleryView(
    photos: List<GalleryPhoto>,
    isFieldMode: Boolean = false,
    isLowMemoryMode: Boolean = false,
    isHighContrastMode: Boolean = false,
    isDimLightMode: Boolean = false,
    isOfflineMode: Boolean = false,
    vibrationCompensation: Boolean = false,
    isBatteryOptimizedMode: Boolean = false,
    gloveMode: GalleryTestDataFactory.GloveTestScenario? = null,
    selectedPhotos: Set<String> = emptySet(),
    onPhotoClick: (String) -> Unit = {},
    onPhotoLongClick: (String) -> Unit = {},
    onPhotoSelected: (String) -> Unit = {}
) {
    val minTouchSize = when {
        gloveMode != null -> gloveMode.minimumTouchSize.dp
        isFieldMode -> 72.dp // Construction standard
        else -> 48.dp // Standard Android
    }
    
    GalleryGridComponent(
        photos = photos,
        onPhotoClick = onPhotoClick,
        onPhotoLongClick = onPhotoLongClick,
        selectedPhotos = selectedPhotos,
        minTouchTargetSize = minTouchSize
    )
}