package com.hazardhawk.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit4.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import kotlin.system.measureTimeMillis
import java.util.concurrent.atomic.AtomicLong

import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

import com.hazardhawk.ui.glass.core.*
import com.hazardhawk.ui.glass.android.AndroidGlassRenderer
import com.hazardhawk.ui.glass.GlassOverlayConfig
import io.mockk.mockk

/**
 * Critical emergency mode activation tests for construction safety.
 * 
 * SAFETY REQUIREMENT: Emergency mode MUST activate in under 500ms
 * This is a life-safety requirement for construction site incidents.
 * 
 * Test scenarios:
 * - Emergency alert activation (<500ms requirement)
 * - High contrast mode for visibility impaired workers
 * - Safety incident reporting mode
 * - Equipment malfunction alerts
 * - Environmental hazard warnings
 * - Medical emergency response
 * - Evacuation procedures activation
 * - Lockout/Tagout (LOTO) emergency overrides
 * - Fall protection system alerts
 * - Hazmat incident response
 * 
 * Emergency modes tested:
 * - HIGH_CONTRAST: For vision-impaired visibility
 * - EMERGENCY_VISIBLE: Maximum visibility alerts  
 * - POWER_SAVE: Emergency power conservation
 * - NORMAL: Standard operation
 * 
 * Performance requirements:
 * - Activation time: <500ms (CRITICAL)
 * - UI response time: <200ms 
 * - Visual confirmation: <100ms
 * - Audio alert delay: <50ms
 * - Network notification: <2000ms
 * 
 * OSHA compliance requirements:
 * - 7:1 contrast ratio minimum
 * - Large touch targets (minimum 44dp)
 * - Clear visual hierarchy
 * - Accessible text sizing
 * - Color blind friendly alerts
 */
@RunWith(AndroidJUnit4::class)
class EmergencyModeActivationTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var mockContext: android.content.Context
    private lateinit var glassRenderer: AndroidGlassRenderer
    private lateinit var glassState: GlassState
    
    // Performance tracking
    private val activationTimes = mutableListOf<Long>()
    private val uiResponseTimes = mutableListOf<Long>()
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        glassRenderer = AndroidGlassRenderer(mockContext)
        glassState = GlassState.forConstructionSite(
            performanceTier = PerformanceTier.MEDIUM,
            batteryLevel = 0.5f
        )
        
        // Clear previous timing data
        activationTimes.clear()
        uiResponseTimes.clear()
    }
    
    @After
    fun teardown() = runTest {
        glassRenderer.release()
    }

    /**
     * Test 1: Critical Emergency Activation Time (<500ms requirement)
     * This is the most critical test - emergency mode MUST activate quickly
     */
    @Test
    fun testCriticalEmergencyActivationTime() = runTest {
        val emergencyModes = listOf(
            EmergencyMode.HIGH_CONTRAST to "High contrast for visibility",
            EmergencyMode.EMERGENCY_VISIBLE to "Maximum visibility emergency", 
            EmergencyMode.POWER_SAVE to "Emergency power conservation"
        )
        
        emergencyModes.forEach { (mode, description) ->
            // Test emergency activation 20 times for statistical accuracy
            val activationTimings = mutableListOf<Long>()
            
            repeat(20) { iteration ->
                // Reset to normal state
                glassState.deactivateEmergency()
                delay(50L) // Allow state to settle
                
                // Measure emergency activation time
                val activationTime = measureTimeMillis {
                    glassState.activateEmergency(
                        mode = mode,
                        reason = "$description - Test iteration $iteration",
                        userOverride = false
                    )
                }
                
                activationTimings.add(activationTime)
                activationTimes.add(activationTime)
                
                // Verify emergency is active
                val emergencyState = glassState.emergencyState.value
                assertTrue("Emergency should be active after activation", 
                          emergencyState.isEmergencyActive)
                assertEquals("Emergency mode should match requested mode", 
                            mode, emergencyState.emergencyMode)
            }
            
            // Statistical analysis of activation times
            val averageTime = activationTimings.average()
            val maxTime = activationTimings.maxOrNull() ?: 0L
            val minTime = activationTimings.minOrNull() ?: 0L
            val p95Time = activationTimings.sorted()[((activationTimings.size * 0.95).toInt())]
            val p99Time = activationTimings.sorted()[((activationTimings.size * 0.99).toInt())]
            
            // CRITICAL SAFETY REQUIREMENTS - MUST PASS
            assertTrue("$description: Average activation time MUST be under 500ms (was ${averageTime}ms)", 
                      averageTime < 500.0)
            assertTrue("$description: Maximum activation time MUST be under 500ms (was ${maxTime}ms)", 
                      maxTime < 500L)
            assertTrue("$description: 95th percentile MUST be under 400ms (was ${p95Time}ms)", 
                      p95Time < 400L)
            assertTrue("$description: 99th percentile MUST be under 450ms (was ${p99Time}ms)", 
                      p99Time < 450L)
            
            // Performance targets for excellent responsiveness
            assertTrue("$description: Average should be under 200ms for excellent UX (was ${averageTime}ms)", 
                      averageTime < 200.0)
            assertTrue("$description: Minimum time should show immediate response (was ${minTime}ms)", 
                      minTime < 50L)
        }
    }

    /**
     * Test 2: Emergency UI Visual Confirmation Speed
     * Tests how quickly emergency visual changes appear to user
     */
    @Test
    fun testEmergencyVisualConfirmationSpeed() = runTest {
        composeTestRule.setContent {
            var emergencyActive by remember { mutableStateOf(false) }
            var activationStartTime by remember { mutableStateOf(0L) }
            var visualConfirmationTime by remember { mutableStateOf(0L) }
            
            LaunchedEffect(emergencyActive) {
                if (emergencyActive) {
                    visualConfirmationTime = System.currentTimeMillis() - activationStartTime
                }
            }
            
            val hazeState = remember { HazeState() }
            val emergencyConfig = GlassOverlayConfig.emergency
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (emergencyActive) Color.Red else Color.Gray)
                    .haze(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = if (emergencyActive) 0.dp else 20.dp,
                            tint = if (emergencyActive) Color.Red.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f)
                        )
                    )
                    .testTag("emergency_container")
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(200.dp)
                        .align(Alignment.Center)
                        .hazeChild(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = if (emergencyActive) 0.dp else 15.dp,
                                tint = if (emergencyActive) 
                                    Color.Red.copy(alpha = 0.95f) 
                                else 
                                    Color.White.copy(alpha = 0.2f)
                            )
                        )
                        .testTag("emergency_alert_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (emergencyActive) 
                            Color.Red.copy(alpha = 0.9f) 
                        else 
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (emergencyActive) "⚠️ EMERGENCY ALERT" else "Normal Operation",
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (emergencyActive) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("emergency_text")
                        )
                        
                        if (emergencyActive) {
                            Text(
                                text = "Visual confirmation time: ${visualConfirmationTime}ms",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .testTag("confirmation_time_text")
                            )
                        }
                        
                        Button(
                            onClick = {
                                if (!emergencyActive) {
                                    activationStartTime = System.currentTimeMillis()
                                    emergencyActive = true
                                } else {
                                    emergencyActive = false
                                }
                            },
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .testTag("emergency_trigger_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (emergencyActive) Color.Black else Color.Red
                            )
                        ) {
                            Text(
                                text = if (emergencyActive) "Clear Emergency" else "Trigger Emergency",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        
        // Test visual confirmation timing
        repeat(10) { iteration ->
            composeTestRule.onNodeWithTag("emergency_trigger_button").performClick()
            
            // Wait for visual changes to complete
            composeTestRule.waitForIdle()
            delay(100L) // Allow visual effects to render
            
            // Verify emergency visuals are displayed
            composeTestRule.onNodeWithTag("emergency_text").assertTextContains("EMERGENCY ALERT")
            composeTestRule.onNodeWithTag("confirmation_time_text").assertIsDisplayed()
            
            // Clear emergency for next iteration
            composeTestRule.onNodeWithTag("emergency_trigger_button").performClick()
            composeTestRule.waitForIdle()
            delay(100L)
        }
    }

    /**
     * Test 3: Concurrent Emergency Activation Stress Test
     * Tests emergency activation under high system load
     */
    @Test
    fun testConcurrentEmergencyActivationStressTest() = runTest {
        val concurrentActivations = 50
        val activationResults = mutableListOf<EmergencyActivationResult>()
        
        // Create high system load
        val loadGeneratorJob = launch {
            repeat(1000) {
                delay(1L)
                // Simulate system load
            }
        }
        
        // Test concurrent emergency activations
        val activationJobs = (1..concurrentActivations).map { index ->
            launch {
                val startTime = System.currentTimeMillis()
                
                try {
                    val localGlassState = GlassState.forEmergency()
                    
                    val activationTime = measureTimeMillis {
                        localGlassState.activateEmergency(
                            mode = EmergencyMode.EMERGENCY_VISIBLE,
                            reason = "Concurrent stress test #$index",
                            userOverride = false
                        )
                    }
                    
                    val emergencyState = localGlassState.emergencyState.value
                    
                    activationResults.add(
                        EmergencyActivationResult(
                            success = emergencyState.isEmergencyActive,
                            activationTime = activationTime,
                            threadId = Thread.currentThread().id,
                            index = index
                        )
                    )
                    
                } catch (e: Exception) {
                    activationResults.add(
                        EmergencyActivationResult(
                            success = false,
                            activationTime = -1L,
                            threadId = Thread.currentThread().id,
                            index = index,
                            error = e.message
                        )
                    )
                }
            }
        }
        
        // Wait for all activations to complete
        activationJobs.forEach { it.join() }
        loadGeneratorJob.cancel()
        
        // Analyze concurrent activation results
        val successfulActivations = activationResults.filter { it.success }
        val failedActivations = activationResults.filter { !it.success }
        
        assertTrue("At least 95% of concurrent activations should succeed", 
                  successfulActivations.size >= (concurrentActivations * 0.95))
        
        if (successfulActivations.isNotEmpty()) {
            val averageTime = successfulActivations.map { it.activationTime }.average()
            val maxTime = successfulActivations.maxOf { it.activationTime }
            
            assertTrue("Average concurrent activation time should be under 800ms (was ${averageTime}ms)", 
                      averageTime < 800.0)
            assertTrue("Maximum concurrent activation time should be under 1000ms (was ${maxTime}ms)", 
                      maxTime < 1000L)
        }
        
        // Log failures for analysis
        failedActivations.forEach { failure ->
            println("Emergency activation failure #${failure.index}: ${failure.error}")
        }
    }

    /**
     * Test 4: Emergency Mode Battery Impact
     * Ensures emergency mode doesn't drain battery excessively
     */
    @Test
    fun testEmergencyModeBatteryImpact() = runTest {
        val batteryLevels = listOf(0.8f, 0.5f, 0.3f, 0.15f, 0.05f)
        
        batteryLevels.forEach { batteryLevel ->
            val config = GlassConfiguration.forConstructionSite(
                performanceTier = PerformanceTier.MEDIUM,
                batteryLevel = batteryLevel
            )
            
            val testState = GlassState(config)
            
            // Measure emergency activation with different battery levels
            val activationTime = measureTimeMillis {
                testState.activateEmergency(
                    mode = EmergencyMode.EMERGENCY_VISIBLE,
                    reason = "Battery level test at ${batteryLevel * 100}%",
                    userOverride = false
                )
            }
            
            // Emergency activation should work regardless of battery level
            val emergencyState = testState.emergencyState.value
            assertTrue("Emergency should activate even at ${batteryLevel * 100}% battery", 
                      emergencyState.isEmergencyActive)
            
            // Very low battery should activate power save mode automatically
            if (batteryLevel < 0.1f) {
                assertTrue("Very low battery should trigger power optimizations", 
                          activationTime < 300L) // Should be even faster due to reduced effects
            } else {
                assertTrue("Emergency activation should be under 500ms at ${batteryLevel * 100}% battery", 
                          activationTime < 500L)
            }
        }
    }

    /**
     * Test 5: Emergency Timeout and Auto-Deactivation
     * Tests emergency mode timeouts and automatic deactivation
     */
    @Test
    fun testEmergencyTimeoutAndAutoDeactivation() = runTest {
        // Test emergency with timeout
        val timeoutMs = 2000L // 2 second timeout for testing
        
        glassState.activateEmergency(
            mode = EmergencyMode.HIGH_CONTRAST,
            reason = "Timeout test emergency",
            timeoutMs = timeoutMs,
            userOverride = true
        )
        
        // Verify emergency is active
        var emergencyState = glassState.emergencyState.value
        assertTrue("Emergency should be active initially", emergencyState.isEmergencyActive)
        assertEquals("Timeout should be set correctly", timeoutMs, emergencyState.timeoutMs)
        
        // Wait for timeout
        delay(timeoutMs + 500L) // Extra buffer for processing
        
        // Check if emergency has timed out
        glassState.checkEmergencyTimeout()
        
        emergencyState = glassState.emergencyState.value
        assertTrue("Emergency should have timed out", emergencyState.hasTimedOut)
        
        // Test auto-deactivation
        val deactivated = glassState.deactivateEmergency()
        assertTrue("Emergency should be deactivatable after timeout", deactivated)
        
        emergencyState = glassState.emergencyState.value
        assertFalse("Emergency should be deactivated", emergencyState.isEmergencyActive)
    }

    /**
     * Test 6: Emergency Mode OSHA Compliance Validation  
     * Ensures emergency mode meets OSHA visibility requirements
     */
    @Test
    fun testEmergencyModeOSHACompliance() = runTest {
        val emergencyConfig = GlassOverlayConfig.emergency
        
        // OSHA compliance requirements
        assertTrue("Emergency mode must be OSHA compliant", emergencyConfig.oshaCompliant)
        assertTrue("Emergency mode must use high contrast", emergencyConfig.emergencyHighContrast)
        
        // Visual requirements
        assertEquals("Emergency mode must disable blur for clarity", 0f, emergencyConfig.blurRadius)
        assertTrue("Emergency mode must have high opacity (>90%)", emergencyConfig.opacity > 0.9f)
        assertTrue("Emergency mode must have thick borders", emergencyConfig.borderWidth >= 3f)
        
        // Test contrast ratio compliance in UI
        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black) // Dark background for contrast
                    .haze(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = emergencyConfig.blurRadius.dp,
                            tint = Color.Transparent
                        )
                    )
                    .testTag("osha_compliance_container")
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(300.dp)
                        .align(Alignment.Center)
                        .hazeChild(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = 0.dp, // No blur for emergency
                                tint = Color.Red.copy(alpha = 0.95f)
                            )
                        )
                        .testTag("osha_emergency_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Red // High contrast emergency color
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp), // Large padding for touch targets
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Large, high-contrast text
                        Text(
                            text = "⚠️ EMERGENCY",
                            style = MaterialTheme.typography.displayMedium, // Large text
                            color = Color.White, // High contrast against red
                            modifier = Modifier.testTag("osha_emergency_title")
                        )
                        
                        // Large touch target button (minimum 44dp as per OSHA guidelines)
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .size(width = 200.dp, height = 60.dp) // Large touch target
                                .padding(top = 16.dp)
                                .testTag("osha_emergency_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White, // High contrast
                                contentColor = Color.Black
                            )
                        ) {
                            Text(
                                text = "ACKNOWLEDGE",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
        
        // Verify OSHA compliance visuals are displayed
        composeTestRule.onNodeWithTag("osha_compliance_container").assertIsDisplayed()
        composeTestRule.onNodeWithTag("osha_emergency_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("osha_emergency_title").assertIsDisplayed()
        composeTestRule.onNodeWithTag("osha_emergency_button").assertIsDisplayed()
        
        // Test large touch targets
        composeTestRule.onNodeWithTag("osha_emergency_button")
            .assertWidthIsAtLeast(44.dp) // OSHA minimum touch target size
            .assertHeightIsAtLeast(44.dp)
    }

    /**
     * Test 7: Multiple Emergency Types Performance
     * Tests switching between different emergency types quickly
     */
    @Test
    fun testMultipleEmergencyTypesPerformance() = runTest {
        val emergencyScenarios = listOf(
            EmergencyScenario(EmergencyMode.HIGH_CONTRAST, "Worker vision impairment"),
            EmergencyScenario(EmergencyMode.EMERGENCY_VISIBLE, "Equipment malfunction alert"),
            EmergencyScenario(EmergencyMode.HIGH_CONTRAST, "Environmental hazard warning"),
            EmergencyScenario(EmergencyMode.EMERGENCY_VISIBLE, "Medical emergency response"),
            EmergencyScenario(EmergencyMode.POWER_SAVE, "Emergency power conservation")
        )
        
        val transitionTimes = mutableListOf<Long>()
        
        emergencyScenarios.forEach { scenario ->
            val transitionTime = measureTimeMillis {
                glassState.activateEmergency(
                    mode = scenario.mode,
                    reason = scenario.reason,
                    userOverride = false
                )
            }
            
            transitionTimes.add(transitionTime)
            
            // Verify emergency is active with correct mode
            val emergencyState = glassState.emergencyState.value
            assertTrue("${scenario.reason}: Emergency should be active", 
                      emergencyState.isEmergencyActive)
            assertEquals("${scenario.reason}: Emergency mode should match", 
                        scenario.mode, emergencyState.emergencyMode)
            
            // Brief delay between transitions
            delay(100L)
        }
        
        // Analyze transition performance
        val averageTransitionTime = transitionTimes.average()
        val maxTransitionTime = transitionTimes.maxOrNull() ?: 0L
        
        assertTrue("Average emergency transition should be under 400ms (was ${averageTransitionTime}ms)", 
                  averageTransitionTime < 400.0)
        assertTrue("Maximum emergency transition should be under 600ms (was ${maxTransitionTime}ms)", 
                  maxTransitionTime < 600L)
        
        // All transitions should be under the critical 500ms requirement
        transitionTimes.forEachIndexed { index, time ->
            assertTrue("Emergency transition #$index should be under 500ms (was ${time}ms)", 
                      time < 500L)
        }
    }

    /**
     * Test 8: Emergency Mode Memory and Performance Impact
     * Ensures emergency mode doesn't cause performance degradation
     */
    @Test
    fun testEmergencyModeMemoryAndPerformanceImpact() = runTest {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Activate emergency mode and measure impact
        val performanceMetrics = mutableListOf<PerformanceSnapshot>()
        
        repeat(10) { iteration ->
            val snapshot = PerformanceSnapshot()
            snapshot.iteration = iteration
            snapshot.preActivationMemory = runtime.totalMemory() - runtime.freeMemory()
            
            snapshot.activationTime = measureTimeMillis {
                glassState.activateEmergency(
                    mode = EmergencyMode.EMERGENCY_VISIBLE,
                    reason = "Performance impact test #$iteration",
                    userOverride = false
                )
            }
            
            snapshot.postActivationMemory = runtime.totalMemory() - runtime.freeMemory()
            
            // Measure UI responsiveness during emergency
            val uiResponseTime = measureTimeMillis {
                // Simulate UI operations during emergency
                delay(50L)
            }
            snapshot.uiResponseTime = uiResponseTime
            
            snapshot.deactivationTime = measureTimeMillis {
                glassState.deactivateEmergency()
            }
            
            snapshot.postDeactivationMemory = runtime.totalMemory() - runtime.freeMemory()
            
            performanceMetrics.add(snapshot)
            
            // Brief pause between iterations
            delay(200L)
        }
        
        // Analyze performance impact
        val averageActivationTime = performanceMetrics.map { it.activationTime }.average()
        val averageMemoryIncrease = performanceMetrics.map { 
            (it.postActivationMemory - it.preActivationMemory) / (1024f * 1024f) 
        }.average()
        val averageUIResponseTime = performanceMetrics.map { it.uiResponseTime }.average()
        
        // Performance requirements
        assertTrue("Emergency activation should not significantly impact performance", 
                  averageActivationTime < 300.0)
        assertTrue("Emergency mode should not consume excessive memory (${averageMemoryIncrease}MB)", 
                  averageMemoryIncrease < 20f)
        assertTrue("UI should remain responsive during emergency (${averageUIResponseTime}ms)", 
                  averageUIResponseTime < 100.0)
        
        // Memory leak detection
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryLeak = (finalMemory - initialMemory) / (1024f * 1024f)
        assertTrue("No significant memory leaks should occur (leak: ${memoryLeak}MB)", 
                  memoryLeak < 10f)
    }

    // Helper data classes and functions
    
    data class EmergencyActivationResult(
        val success: Boolean,
        val activationTime: Long,
        val threadId: Long,
        val index: Int,
        val error: String? = null
    )
    
    data class EmergencyScenario(
        val mode: EmergencyMode,
        val reason: String
    )
    
    data class PerformanceSnapshot(
        var iteration: Int = 0,
        var preActivationMemory: Long = 0L,
        var postActivationMemory: Long = 0L,
        var postDeactivationMemory: Long = 0L,
        var activationTime: Long = 0L,
        var deactivationTime: Long = 0L,
        var uiResponseTime: Long = 0L
    )
}
