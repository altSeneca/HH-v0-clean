package com.hazardhawk.ui.glass

import kotlin.test.*

/**
 * Shared test suite for glass morphism functionality across all platforms.
 * Tests device capability detection, configuration adaptation, and performance validation.
 */
class GlassMorphismSharedTest {
    
    private val testLogic = GlassMorphismTestLogic()
    
    // MARK: - Device Capability Detection Tests
    
    @Test
    fun testDeviceCapabilityDetection_HighEndDevice() {
        val capability = testLogic.detectDeviceCapability(
            ramMB = 8192,
            apiLevel = 31,
            hasHardwareAcceleration = true
        )
        
        assertEquals(
            GlassMorphismTestLogic.GlassSupportLevel.FULL,
            capability,
            "High-end device should support full glass effects"
        )
    }
    
    @Test
    fun testDeviceCapabilityDetection_MidRangeDevice() {
        val capability = testLogic.detectDeviceCapability(
            ramMB = 4096,
            apiLevel = 28,
            hasHardwareAcceleration = true
        )
        
        assertEquals(
            GlassMorphismTestLogic.GlassSupportLevel.REDUCED,
            capability,
            "Mid-range device should support reduced glass effects"
        )
    }
    
    @Test
    fun testDeviceCapabilityDetection_LowEndDevice() {
        val capability = testLogic.detectDeviceCapability(
            ramMB = 2048,
            apiLevel = 24,
            hasHardwareAcceleration = false
        )
        
        assertEquals(
            GlassMorphismTestLogic.GlassSupportLevel.DISABLED,
            capability,
            "Low-end device without hardware acceleration should disable glass effects"
        )
    }
    
    @Test
    fun testDeviceCapabilityDetection_ThermalThrottling() {
        val capability = testLogic.detectDeviceCapability(
            ramMB = 8192,
            apiLevel = 31,
            hasHardwareAcceleration = true,
            thermalState = GlassMorphismTestLogic.ThermalState.SEVERE
        )
        
        assertEquals(
            GlassMorphismTestLogic.GlassSupportLevel.DISABLED,
            capability,
            "Severe thermal state should disable glass effects regardless of device capability"
        )
    }
    
    @Test
    fun testDeviceCapabilityDetection_OldApiLevel() {
        val capability = testLogic.detectDeviceCapability(
            ramMB = 8192,
            apiLevel = 23,
            hasHardwareAcceleration = true
        )
        
        assertEquals(
            GlassMorphismTestLogic.GlassSupportLevel.DISABLED,
            capability,
            "API level below 24 should disable glass effects"
        )
    }
    
    // MARK: - Performance Validation Tests
    
    @Test
    fun testPerformanceValidation_OptimalPerformance() {
        val metrics = GlassMorphismTestLogic.GlassPerformanceMetrics(
            frameRate = 60.0,
            memoryUsageMB = 30L,
            batteryImpactPercent = 10.0,
            loadTimeMs = 300L,
            gpuUtilizationPercent = 50.0
        )
        
        val violations = testLogic.validatePerformance(metrics)
        assertTrue(violations.isEmpty(), "Optimal performance should have no violations")
    }
    
    @Test
    fun testPerformanceValidation_LowFrameRate() {
        val metrics = GlassMorphismTestLogic.GlassPerformanceMetrics(
            frameRate = 30.0,
            memoryUsageMB = 30L,
            batteryImpactPercent = 10.0,
            loadTimeMs = 300L,
            gpuUtilizationPercent = 50.0
        )
        
        val violations = testLogic.validatePerformance(metrics)
        assertTrue(
            violations.any { it.contains("Frame rate") },
            "Low frame rate should be flagged as violation"
        )
    }
    
    @Test
    fun testPerformanceValidation_HighMemoryUsage() {
        val metrics = GlassMorphismTestLogic.GlassPerformanceMetrics(
            frameRate = 60.0,
            memoryUsageMB = 100L,
            batteryImpactPercent = 10.0,
            loadTimeMs = 300L,
            gpuUtilizationPercent = 50.0
        )
        
        val violations = testLogic.validatePerformance(metrics)
        assertTrue(
            violations.any { it.contains("Memory usage") },
            "High memory usage should be flagged as violation"
        )
    }
    
    @Test
    fun testPerformanceValidation_SlowLoadTime() {
        val metrics = GlassMorphismTestLogic.GlassPerformanceMetrics(
            frameRate = 60.0,
            memoryUsageMB = 30L,
            batteryImpactPercent = 10.0,
            loadTimeMs = 800L,
            gpuUtilizationPercent = 50.0
        )
        
        val violations = testLogic.validatePerformance(metrics)
        assertTrue(
            violations.any { it.contains("Load time") },
            "Slow load time should be flagged as violation"
        )
    }
    
    // MARK: - Configuration Validation Tests
    
    @Test
    fun testGlassConfigurationValidation_ValidConfiguration() {
        val config = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.6f,
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val violations = testLogic.validateGlassConfiguration(config)
        assertTrue(violations.isEmpty(), "Valid configuration should have no violations")
    }
    
    @Test
    fun testGlassConfigurationValidation_InvalidBlurRadius() {
        val config = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 5.0f, // Too low
            opacity = 0.6f,
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val violations = testLogic.validateGlassConfiguration(config)
        assertTrue(
            violations.any { it.contains("Blur radius") },
            "Invalid blur radius should be flagged"
        )
    }
    
    @Test
    fun testGlassConfigurationValidation_InvalidOpacity() {
        val config = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.1f, // Too low
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val violations = testLogic.validateGlassConfiguration(config)
        assertTrue(
            violations.any { it.contains("Opacity") },
            "Invalid opacity should be flagged"
        )
    }
    
    @Test
    fun testGlassConfigurationValidation_EmergencyModeViolations() {
        val config = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 20.0f,
            opacity = 0.6f,
            animationsEnabled = true,
            enabledInEmergencyMode = true,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val violations = testLogic.validateGlassConfiguration(config)
        assertTrue(
            violations.any { it.contains("emergency mode") },
            "Emergency mode with full effects should be flagged"
        )
    }
    
    // MARK: - Environment Adaptation Tests
    
    @Test
    fun testEnvironmentAdaptation_BrightSunlight() {
        val baseConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.5f,
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val sunlightEnvironment = GlassMorphismTestLogic.ConstructionEnvironment(
            name = "Bright sunlight",
            ambientLightLux = 100000,
            isWearingGloves = false,
            gloveThicknessMM = 0,
            isVibrationPresent = false,
            expectedConfiguration = GlassMorphismTestLogic.GlassConfiguration(
                blurRadius = 15.0f,
                opacity = 0.8f,
                animationsEnabled = false,
                enabledInEmergencyMode = false,
                supportLevel = GlassMorphismTestLogic.GlassSupportLevel.REDUCED
            )
        )
        
        val adaptedConfig = testLogic.adaptConfigurationForEnvironment(baseConfig, sunlightEnvironment)
        
        assertTrue(
            adaptedConfig.opacity >= 0.8f,
            "Bright sunlight should increase opacity for visibility"
        )
        assertFalse(
            adaptedConfig.animationsEnabled,
            "Bright sunlight should disable animations for performance"
        )
    }
    
    @Test
    fun testEnvironmentAdaptation_LowLight() {
        val baseConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.8f,
            animationsEnabled = false,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val lowLightEnvironment = GlassMorphismTestLogic.ConstructionEnvironment(
            name = "Low light",
            ambientLightLux = 50,
            isWearingGloves = false,
            gloveThicknessMM = 0,
            isVibrationPresent = false,
            expectedConfiguration = GlassMorphismTestLogic.GlassConfiguration(
                blurRadius = 20.0f,
                opacity = 0.4f,
                animationsEnabled = true,
                enabledInEmergencyMode = true,
                supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
            )
        )
        
        val adaptedConfig = testLogic.adaptConfigurationForEnvironment(baseConfig, lowLightEnvironment)
        
        assertTrue(
            adaptedConfig.blurRadius >= 20.0f,
            "Low light should increase blur radius for better effect visibility"
        )
        assertTrue(
            adaptedConfig.opacity <= 0.5f,
            "Low light should decrease opacity to reduce obstruction"
        )
    }
    
    @Test
    fun testEnvironmentAdaptation_HeavyGloves() {
        val baseConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.6f,
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val heavyGloveEnvironment = GlassMorphismTestLogic.ConstructionEnvironment(
            name = "Heavy gloves",
            ambientLightLux = 1000,
            isWearingGloves = true,
            gloveThicknessMM = 8,
            isVibrationPresent = false,
            expectedConfiguration = GlassMorphismTestLogic.GlassConfiguration(
                blurRadius = 15.0f,
                opacity = 0.6f,
                animationsEnabled = true,
                enabledInEmergencyMode = false,
                supportLevel = GlassMorphismTestLogic.GlassSupportLevel.DISABLED
            )
        )
        
        val adaptedConfig = testLogic.adaptConfigurationForEnvironment(baseConfig, heavyGloveEnvironment)
        
        assertEquals(
            GlassMorphismTestLogic.GlassSupportLevel.DISABLED,
            adaptedConfig.supportLevel,
            "Heavy gloves should disable glass effects for better touch accuracy"
        )
    }
    
    @Test
    fun testEnvironmentAdaptation_VibrationPresent() {
        val baseConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.6f,
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val vibrationEnvironment = GlassMorphismTestLogic.ConstructionEnvironment(
            name = "Vibration present",
            ambientLightLux = 1000,
            isWearingGloves = false,
            gloveThicknessMM = 0,
            isVibrationPresent = true,
            expectedConfiguration = GlassMorphismTestLogic.GlassConfiguration(
                blurRadius = 15.0f,
                opacity = 0.6f,
                animationsEnabled = false,
                enabledInEmergencyMode = false,
                supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
            )
        )
        
        val adaptedConfig = testLogic.adaptConfigurationForEnvironment(baseConfig, vibrationEnvironment)
        
        assertFalse(
            adaptedConfig.animationsEnabled,
            "Vibration should disable animations to prevent visual distraction"
        )
    }
    
    // MARK: - Accessibility Compliance Tests
    
    @Test
    fun testAccessibilityCompliance_OutdoorVisibility() {
        val lowOpacityConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.4f, // Low opacity
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val outdoorEnvironment = GlassMorphismTestLogic.ConstructionEnvironment(
            name = "Outdoor bright",
            ambientLightLux = 50000,
            isWearingGloves = false,
            gloveThicknessMM = 0,
            isVibrationPresent = false,
            expectedConfiguration = GlassMorphismTestLogic.GlassConfiguration(
                blurRadius = 15.0f,
                opacity = 0.8f,
                animationsEnabled = false,
                enabledInEmergencyMode = false,
                supportLevel = GlassMorphismTestLogic.GlassSupportLevel.REDUCED
            )
        )
        
        val violations = testLogic.validateAccessibilityCompliance(lowOpacityConfig, outdoorEnvironment)
        
        assertTrue(
            violations.any { it.contains("opacity too low") },
            "Low opacity in bright light should be flagged as accessibility violation"
        )
    }
    
    @Test
    fun testAccessibilityCompliance_GloveCompatibility() {
        val config = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.6f,
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val gloveEnvironment = GlassMorphismTestLogic.ConstructionEnvironment(
            name = "Glove wearing",
            ambientLightLux = 1000,
            isWearingGloves = true,
            gloveThicknessMM = 3,
            isVibrationPresent = false,
            expectedConfiguration = config
        )
        
        val violations = testLogic.validateAccessibilityCompliance(config, gloveEnvironment)
        
        assertTrue(
            violations.any { it.contains("Touch targets") },
            "Glove environment should flag touch target size requirements"
        )
    }
    
    @Test
    fun testAccessibilityCompliance_EmergencyModeRequirement() {
        val nonEmergencyConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 20.0f,
            opacity = 0.4f,
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val normalEnvironment = GlassMorphismTestLogic.ConstructionEnvironment(
            name = "Normal",
            ambientLightLux = 1000,
            isWearingGloves = false,
            gloveThicknessMM = 0,
            isVibrationPresent = false,
            expectedConfiguration = nonEmergencyConfig
        )
        
        val violations = testLogic.validateAccessibilityCompliance(nonEmergencyConfig, normalEnvironment)
        
        assertTrue(
            violations.any { it.contains("emergency mode") },
            "Full glass effects without emergency mode support should be flagged"
        )
    }
    
    // MARK: - Fallback System Tests
    
    @Test
    fun testFallbackBehavior_ValidFallback() {
        val primaryConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 20.0f,
            opacity = 0.6f,
            animationsEnabled = true,
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val fallbackConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.8f, // Higher opacity for better visibility
            animationsEnabled = false,
            enabledInEmergencyMode = true,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.REDUCED
        )
        
        val violations = testLogic.validateFallbackBehavior(primaryConfig, fallbackConfig)
        assertTrue(violations.isEmpty(), "Valid fallback should have no violations")
    }
    
    @Test
    fun testFallbackBehavior_InvalidFallback() {
        val primaryConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 15.0f,
            opacity = 0.8f,
            animationsEnabled = false,
            enabledInEmergencyMode = true,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.REDUCED
        )
        
        val invalidFallbackConfig = GlassMorphismTestLogic.GlassConfiguration(
            blurRadius = 25.0f, // Higher than primary
            opacity = 0.4f, // Lower than primary
            animationsEnabled = true, // Enabled when primary disabled
            enabledInEmergencyMode = false,
            supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
        )
        
        val violations = testLogic.validateFallbackBehavior(primaryConfig, invalidFallbackConfig)
        
        assertTrue(violations.size >= 2, "Invalid fallback should have multiple violations")
        assertTrue(
            violations.any { it.contains("blur radius") },
            "Higher fallback blur should be flagged"
        )
        assertTrue(
            violations.any { it.contains("visibility") },
            "Lower fallback opacity should be flagged"
        )
    }
    
    // MARK: - Integration Test Scenarios
    
    @Test
    fun testAllDeviceCapabilities() {
        val deviceCapabilities = testLogic.getTestDeviceCapabilities()
        
        assertTrue(deviceCapabilities.isNotEmpty(), "Should have test device configurations")
        
        deviceCapabilities.forEach { device ->
            val detectedCapability = testLogic.detectDeviceCapability(
                ramMB = device.ramMB,
                apiLevel = device.apiLevel,
                hasHardwareAcceleration = device.hasHardwareAcceleration
            )
            
            assertEquals(
                device.expectedGlassSupport,
                detectedCapability,
                "Device ${device.name} capability detection mismatch"
            )
        }
    }
    
    @Test
    fun testAllConstructionEnvironments() {
        val environments = testLogic.getConstructionEnvironments()
        
        assertTrue(environments.isNotEmpty(), "Should have construction environment scenarios")
        
        environments.forEach { environment ->
            val baseConfig = GlassMorphismTestLogic.GlassConfiguration(
                blurRadius = 15.0f,
                opacity = 0.6f,
                animationsEnabled = true,
                enabledInEmergencyMode = false,
                supportLevel = GlassMorphismTestLogic.GlassSupportLevel.FULL
            )
            
            val adaptedConfig = testLogic.adaptConfigurationForEnvironment(baseConfig, environment)
            val violations = testLogic.validateGlassConfiguration(adaptedConfig)
            
            assertTrue(
                violations.isEmpty(),
                "Environment ${environment.name} should produce valid adapted configuration. Violations: $violations"
            )
            
            // Verify environment-specific adaptations
            when {
                environment.ambientLightLux > 50000 -> {
                    assertTrue(
                        adaptedConfig.opacity >= 0.7f,
                        "Bright environment should have high opacity"
                    )
                }
                environment.gloveThicknessMM > 5 -> {
                    assertEquals(
                        GlassMorphismTestLogic.GlassSupportLevel.DISABLED,
                        adaptedConfig.supportLevel,
                        "Thick gloves should disable glass effects"
                    )
                }
                environment.isVibrationPresent -> {
                    assertFalse(
                        adaptedConfig.animationsEnabled,
                        "Vibration should disable animations"
                    )
                }
            }
        }
    }
}
