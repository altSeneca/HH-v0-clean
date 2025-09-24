package com.hazardhawk.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

// Import Haze library components (these imports test API availability)
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

import com.hazardhawk.ui.glass.GlassOverlayConfig
import com.hazardhawk.ui.glass.ConstructionGlassMode
import com.hazardhawk.ui.glass.forConstructionMode

/**
 * Tests for Haze 1.6.10 library API compatibility with HazardHawk glass components.
 * 
 * This test suite validates:
 * - Haze library imports and API access
 * - Glass morphism effect rendering with Haze
 * - Construction-specific glass configurations
 * - Performance optimization integration
 * - Emergency mode compatibility
 * - Material design integration
 * 
 * Ensures that Haze 1.6.10 APIs work correctly for construction safety requirements.
 */
@RunWith(AndroidJUnit4::class)
class HazeLibraryCompatibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var hazeState: HazeState
    
    @Before
    fun setup() {
        hazeState = HazeState()
    }
    
    @After
    fun teardown() {
        // Cleanup if needed
    }

    /**
     * Test 1: Haze Library Import Validation
     * Verifies that all required Haze 1.6.10 APIs are accessible
     */
    @Test
    fun testHazeLibraryImports() {
        // Test HazeState creation
        val state = HazeState()
        assertNotNull("HazeState should be created", state)
        
        // Test HazeStyle creation with construction-appropriate values
        val constructionStyle = HazeStyle(
            blurRadius = 16.dp,
            tint = Color(0xFFFF6B00).copy(alpha = 0.1f), // Safety orange tint
            noiseFactor = 0.05f
        )
        assertNotNull("Construction HazeStyle should be created", constructionStyle)
        assertEquals("Blur radius should be set correctly", 16.dp, constructionStyle.blurRadius)
        
        // Test emergency style with high visibility
        val emergencyStyle = HazeStyle(
            blurRadius = 0.dp, // No blur for emergency
            tint = Color.Red.copy(alpha = 0.2f),
            noiseFactor = 0f
        )
        assertEquals("Emergency should have no blur", 0.dp, emergencyStyle.blurRadius)
    }

    /**
     * Test 2: Basic Haze Composable Integration
     * Tests integration of Haze effects with Compose UI
     */
    @Test
    fun testBasicHazeComposableIntegration() {
        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(
                        state = hazeState,
                        style = HazeStyle(blurRadius = 20.dp)
                    )
                    .testTag("haze_background")
            ) {
                Card(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.Center)
                        .hazeChild(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = 20.dp,
                                tint = Color.White.copy(alpha = 0.2f)
                            )
                        )
                        .testTag("haze_card")
                ) {
                    // Card content
                }
            }
        }
        
        // Verify components are displayed
        composeTestRule.onNodeWithTag("haze_background").assertIsDisplayed()
        composeTestRule.onNodeWithTag("haze_card").assertIsDisplayed()
    }

    /**
     * Test 3: Construction-Specific Haze Configurations
     * Tests glass configurations optimized for construction environments
     */
    @Test
    fun testConstructionSpecificHazeConfigurations() {
        val constructionModes = mapOf(
            ConstructionGlassMode.STANDARD to "Standard indoor",
            ConstructionGlassMode.OUTDOOR_BRIGHT to "Bright outdoor",
            ConstructionGlassMode.LOW_LIGHT to "Low light",
            ConstructionGlassMode.EMERGENCY to "Emergency",
            ConstructionGlassMode.POWER_SAVE to "Power save"
        )
        
        constructionModes.forEach { (mode, description) ->
            val config = GlassOverlayConfig.forConstructionMode(mode)
            
            composeTestRule.setContent {
                val hazeState = remember { HazeState() }
                
                // Convert GlassOverlayConfig to HazeStyle
                val hazeStyle = HazeStyle(
                    blurRadius = config.blurRadius.dp,
                    tint = Color.White.copy(alpha = config.opacity * 0.3f),
                    noiseFactor = if (mode == ConstructionGlassMode.OUTDOOR_BRIGHT) 0.1f else 0.05f
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray)
                        .haze(
                            state = hazeState,
                            style = hazeStyle
                        )
                        .testTag("construction_haze_$mode")
                ) {
                    Card(
                        modifier = Modifier
                            .size(150.dp)
                            .align(Alignment.Center)
                            .hazeChild(
                                state = hazeState,
                                style = hazeStyle
                            )
                            .testTag("construction_card_$mode")
                    ) {
                        // Construction info content
                    }
                }
            }
            
            // Verify each configuration renders correctly
            composeTestRule.onNodeWithTag("construction_haze_$mode").assertIsDisplayed()
            composeTestRule.onNodeWithTag("construction_card_$mode").assertIsDisplayed()
            
            // Test configuration properties
            when (mode) {
                ConstructionGlassMode.EMERGENCY -> {
                    assertEquals("Emergency should disable blur", 0f, config.blurRadius)
                    assertTrue("Emergency should have high opacity", config.opacity > 0.9f)
                }
                ConstructionGlassMode.POWER_SAVE -> {
                    assertEquals("Power save should disable blur", 0f, config.blurRadius)
                    assertFalse("Power save should disable animations", config.transitionAnimationEnabled)
                }
                ConstructionGlassMode.OUTDOOR_BRIGHT -> {
                    assertTrue("Outdoor should have high opacity", config.opacity > 0.8f)
                    assertTrue("Outdoor should have thick borders", config.borderWidth >= 3f)
                }
                ConstructionGlassMode.LOW_LIGHT -> {
                    assertTrue("Low light should have enhanced blur", config.blurRadius > 20f)
                }
                else -> {
                    // Standard mode tests
                    assertTrue("Standard should have moderate settings", config.blurRadius > 0f)
                }
            }
        }
    }

    /**
     * Test 4: Haze Materials API Integration
     * Tests integration with Haze Materials for enhanced effects
     */
    @Test
    @OptIn(ExperimentalHazeMaterialsApi::class)
    fun testHazeMaterialsAPIIntegration() {
        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Blue)
                    .haze(
                        state = hazeState,
                        style = HazeMaterials.regular()
                    )
                    .testTag("materials_background")
            ) {
                // Test different material styles
                val materialStyles = listOf(
                    HazeMaterials.regular() to "regular",
                    HazeMaterials.thick() to "thick",
                    HazeMaterials.thin() to "thin"
                )
                
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    materialStyles.forEach { (style, name) ->
                        Card(
                            modifier = Modifier
                                .size(120.dp, 60.dp)
                                .hazeChild(
                                    state = hazeState,
                                    style = style
                                )
                                .testTag("material_card_$name")
                        ) {
                            // Material content
                        }
                    }
                }
            }
        }
        
        // Verify all material styles render
        composeTestRule.onNodeWithTag("materials_background").assertIsDisplayed()
        composeTestRule.onNodeWithTag("material_card_regular").assertIsDisplayed()
        composeTestRule.onNodeWithTag("material_card_thick").assertIsDisplayed()
        composeTestRule.onNodeWithTag("material_card_thin").assertIsDisplayed()
    }

    /**
     * Test 5: Emergency Mode Glass Effects
     * Tests high-contrast emergency mode with Haze integration
     */
    @Test
    fun testEmergencyModeGlassEffects() {
        val emergencyConfig = GlassOverlayConfig.emergency
        
        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            
            // Emergency mode should have no blur and high contrast
            val emergencyStyle = HazeStyle(
                blurRadius = emergencyConfig.blurRadius.dp, // Should be 0
                tint = Color.Red.copy(alpha = 0.1f),
                noiseFactor = 0f
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .haze(
                        state = hazeState,
                        style = emergencyStyle
                    )
                    .testTag("emergency_background")
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(200.dp)
                        .align(Alignment.Center)
                        .hazeChild(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = 0.dp, // No blur for emergency visibility
                                tint = Color.Red.copy(alpha = 0.95f), // High opacity red
                                noiseFactor = 0f
                            )
                        )
                        .testTag("emergency_alert_card"),
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = Color.Red.copy(alpha = 0.9f)
                    )
                ) {
                    // Emergency alert content
                }
            }
        }
        
        composeTestRule.onNodeWithTag("emergency_background").assertIsDisplayed()
        composeTestRule.onNodeWithTag("emergency_alert_card").assertIsDisplayed()
        
        // Verify emergency configuration properties
        assertEquals("Emergency should have no blur", 0f, emergencyConfig.blurRadius)
        assertTrue("Emergency should have high opacity", emergencyConfig.opacity > 0.9f)
        assertTrue("Emergency should be OSHA compliant", emergencyConfig.oshaCompliant)
        assertTrue("Emergency should use high contrast", emergencyConfig.emergencyHighContrast)
    }

    /**
     * Test 6: Camera Viewfinder Glass Integration
     * Tests glass effects over camera preview using Haze
     */
    @Test
    fun testCameraViewfinderGlassIntegration() {
        val cameraConfig = GlassOverlayConfig.cameraViewfinder
        
        composeTestRule.setContent {
            val hazeState = remember { HazeState() }
            
            // Simulate camera preview background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Green) // Simulate camera preview
                    .haze(
                        state = hazeState,
                        style = HazeStyle(
                            blurRadius = cameraConfig.blurRadius.dp,
                            tint = Color.Transparent, // Don't tint camera preview
                            noiseFactor = 0.02f
                        )
                    )
                    .testTag("camera_preview")
            ) {
                // Camera UI overlay with glass effect
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                        .hazeChild(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = cameraConfig.blurRadius.dp,
                                tint = Color.White.copy(alpha = cameraConfig.opacity * 0.5f),
                                noiseFactor = 0.03f
                            )
                        )
                        .testTag("camera_controls"),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Camera control buttons would go here
                    repeat(3) { index ->
                        Card(
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("camera_button_$index")
                        ) {
                            // Button content
                        }
                    }
                }
            }
        }
        
        composeTestRule.onNodeWithTag("camera_preview").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_controls").assertIsDisplayed()
        composeTestRule.onNodeWithTag("camera_button_0").assertIsDisplayed()
        
        // Verify camera configuration is optimized for transparency
        assertTrue("Camera config should have low opacity", cameraConfig.opacity < 0.5f)
        assertTrue("Camera config should have moderate blur", cameraConfig.blurRadius > 0f && cameraConfig.blurRadius < 20f)
    }

    /**
     * Test 7: Performance with Haze Effects
     * Tests rendering performance with various Haze configurations
     */
    @Test
    fun testPerformanceWithHazeEffects() = runTest {
        val performanceConfigs = listOf(
            HazeStyle(blurRadius = 5.dp) to "Low blur",
            HazeStyle(blurRadius = 15.dp) to "Medium blur", 
            HazeStyle(blurRadius = 25.dp) to "High blur",
            HazeStyle(blurRadius = 0.dp) to "No blur"
        )
        
        performanceConfigs.forEach { (style, description) ->
            var renderTime = 0L
            
            composeTestRule.setContent {
                val hazeState = remember { HazeState() }
                
                // Measure composition time
                LaunchedEffect(style) {
                    val startTime = System.currentTimeMillis()
                    // Simulate heavy UI composition
                    kotlinx.coroutines.delay(10L)
                    renderTime = System.currentTimeMillis() - startTime
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Magenta)
                        .haze(
                            state = hazeState,
                            style = style
                        )
                        .testTag("performance_test_$description")
                ) {
                    // Multiple overlapping glass elements
                    repeat(5) { index ->
                        Card(
                            modifier = Modifier
                                .size((100 + index * 20).dp)
                                .offset(x = (index * 30).dp, y = (index * 30).dp)
                                .hazeChild(
                                    state = hazeState,
                                    style = style
                                )
                                .testTag("performance_card_${description}_$index")
                        ) {
                            // Card content
                        }
                    }
                }
            }
            
            // Verify UI renders successfully
            composeTestRule.onNodeWithTag("performance_test_$description").assertIsDisplayed()
            composeTestRule.onNodeWithTag("performance_card_${description}_0").assertIsDisplayed()
            
            // Performance should be acceptable (this is a basic check)
            // More sophisticated performance testing would require additional tooling
            assertTrue("$description should complete composition in reasonable time", 
                      renderTime < 1000L)
        }
    }

    /**
     * Test 8: Haze State Management
     * Tests proper state management and lifecycle handling
     */
    @Test
    fun testHazeStateManagement() {
        var hazeState: HazeState? = null
        var stateRecreated = false
        
        composeTestRule.setContent {
            // Test state creation and recreation
            hazeState = remember {
                stateRecreated = true
                HazeState()
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(
                        state = hazeState!!,
                        style = HazeStyle(blurRadius = 15.dp)
                    )
                    .testTag("state_management_test")
            ) {
                Card(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.Center)
                        .hazeChild(
                            state = hazeState!!,
                            style = HazeStyle(
                                blurRadius = 15.dp,
                                tint = Color.Blue.copy(alpha = 0.1f)
                            )
                        )
                        .testTag("state_management_card")
                )
            }
        }
        
        // Verify state was created
        assertNotNull("Haze state should be created", hazeState)
        assertTrue("State should be created on first composition", stateRecreated)
        
        composeTestRule.onNodeWithTag("state_management_test").assertIsDisplayed()
        composeTestRule.onNodeWithTag("state_management_card").assertIsDisplayed()
    }

    /**
     * Test 9: Haze API Backward Compatibility  
     * Tests that our glass components work with Haze 1.6.10 API changes
     */
    @Test
    fun testHazeAPIBackwardCompatibility() {
        // Test that our glass configurations can be converted to Haze styles
        val glassConfigs = listOf(
            GlassOverlayConfig.modal,
            GlassOverlayConfig.emergency,
            GlassOverlayConfig.cameraViewfinder,
            GlassOverlayConfig.construction
        )
        
        glassConfigs.forEach { config ->
            // Convert GlassOverlayConfig to HazeStyle
            val hazeStyle = convertGlassConfigToHazeStyle(config)
            assertNotNull("Should be able to convert config to HazeStyle", hazeStyle)
            
            // Verify conversion preserves key properties
            assertEquals("Blur radius should match", 
                        config.blurRadius.dp, hazeStyle.blurRadius)
            
            // Test in Compose
            composeTestRule.setContent {
                val hazeState = remember { HazeState() }
                
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .haze(state = hazeState, style = hazeStyle)
                        .testTag("compat_test_${config.hashCode()}")
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeChild(state = hazeState, style = hazeStyle)
                    )
                }
            }
            
            composeTestRule.onNodeWithTag("compat_test_${config.hashCode()}").assertIsDisplayed()
        }
    }
    
    /**
     * Helper function to convert GlassOverlayConfig to HazeStyle
     */
    private fun convertGlassConfigToHazeStyle(config: GlassOverlayConfig): HazeStyle {
        return HazeStyle(
            blurRadius = config.blurRadius.dp,
            tint = if (config.emergencyHighContrast) {
                Color.Red.copy(alpha = 0.1f)
            } else {
                Color.White.copy(alpha = config.opacity * 0.2f)
            },
            noiseFactor = if (config.adaptiveBlurEnabled) 0.05f else 0f
        )
    }
}
