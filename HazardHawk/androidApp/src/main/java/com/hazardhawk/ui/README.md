# Glass Morphism Camera System Documentation

## Overview

The Glass Morphism Camera System provides a modern, construction-optimized camera interface with beautiful glass effects powered by the Haze library. This implementation creates a parallel camera experience that maintains full feature parity with the existing camera functionality while adding adaptive glass morphism effects.

## Architecture

### Core Components

```
ui/glass/
├── state/
│   ├── GlassConfiguration.kt     # Configuration and adaptation logic
│   └── GlassState.kt            # UI state management
├── screens/
│   └── GlassCameraScreen.kt     # Main glass camera screen
├── components/
│   ├── GlassCameraControls.kt   # Glass-enhanced camera controls
│   ├── GlassViewfinder.kt       # Translucent viewfinder overlay
│   ├── GlassMetadataOverlay.kt  # Adaptive metadata display
│   └── GlassUIComponents.kt     # Supporting glass components
└── README.md                    # This documentation
```

## Key Features

### 🔧 Construction-Optimized Design
- **Large Touch Targets**: Minimum 60dp for gloved hands
- **High Contrast Mode**: Emergency visibility enhancement
- **Safety Orange Borders**: High-visibility construction colors
- **Vibration Feedback**: Tactile confirmation for critical actions
- **Outdoor Visibility**: Adaptive transparency based on lighting

### 🎨 Glass Morphism Effects
- **Haze Integration**: Backend blur effects using Haze library
- **Adaptive Transparency**: Responds to lighting conditions
- **Smooth Transitions**: 300ms animated state changes
- **Layered Rendering**: Optimized multi-layer glass effects
- **Performance Scaling**: Graceful degradation for older devices

### 📱 Smart Adaptations
- **Environmental Sensing**: Adapts to bright sunlight, low light, etc.
- **Performance Monitoring**: Real-time FPS and memory tracking
- **Thermal Management**: Automatic effect reduction under thermal stress
- **Battery Optimization**: Reduces effects when battery is low
- **Emergency Override**: Instant high-contrast mode for safety

## Usage

### Basic Implementation

```kotlin
@Composable
fun MyApp() {
    GlassCameraScreen(
        onNavigateBack = { /* handle back navigation */ },
        onNavigateToGallery = { /* navigate to gallery */ },
        onNavigateToSettings = { /* navigate to settings */ }
    )
}
```

### Custom Configuration

```kotlin
val customConfig = GlassConfiguration(
    blurRadius = 18.0f,
    opacity = 0.7f,
    tintColor = ConstructionColors.SafetyOrange.copy(alpha = 0.1f),
    isOutdoorMode = true,
    minTouchTargetSize = 72.0f,
    enableEmergencyOverride = true
)

val glassState = GlassUIState.constructionCamera().withConfiguration(customConfig)
```

## Configuration Options

### Glass Visual Properties

```kotlin
data class GlassConfiguration(
    val blurRadius: Float = 15.0f,           // Background blur intensity
    val opacity: Float = 0.6f,               // Glass transparency
    val tintColor: Color = Color.White,      // Glass tint overlay
    val borderColor: Color = SafetyOrange,   // Border highlight color
    val borderWidth: Float = 2.0f,           // Border thickness
    
    // Animation settings
    val animationsEnabled: Boolean = true,
    val transitionDurationMs: Int = 300,
    
    // Performance settings
    val supportLevel: GlassSupportLevel,     // FULL, REDUCED, DISABLED
    val adaptiveQualityEnabled: Boolean,
    
    // Construction-specific
    val minTouchTargetSize: Float = 60.0f,   // Minimum button size
    val isOutdoorMode: Boolean = true,
    val enableEmergencyOverride: Boolean = true
)
```

### Predefined Configurations

```kotlin
// Standard construction environment
GlassConfiguration.constructionDefault()

// Bright outdoor conditions
GlassConfiguration.outdoorHighVisibility()

// Emergency safety mode
GlassConfiguration.emergencyMode()

// Low-performance devices
GlassConfiguration.lowPerformance()
```

## Environmental Adaptations

### Lighting Conditions

The system automatically adapts to different lighting conditions:

- **Bright Sunlight**: Reduces blur, increases opacity, enables high contrast
- **Overcast/Normal**: Standard glass effects with moderate opacity
- **Low Light**: Increases blur radius, reduces opacity for better visibility
- **Night**: Minimal glass effects, yellow safety borders

### Performance Adaptations

- **High Performance**: Full glass effects with animations
- **Moderate Performance**: Reduced blur radius, disabled animations
- **Low Performance**: Fallback to solid overlays, minimal effects
- **Critical Performance**: Complete disable of glass effects

### Thermal Management

- **Normal**: Full glass effects
- **Light Throttling**: Disable animations
- **Moderate Throttling**: Reduce to simple glass mode
- **Severe/Critical**: Disable all glass effects

## Performance Monitoring

### Automatic Performance Tracking

```kotlin
LaunchedEffect(Unit) {
    while (true) {
        delay(1000)
        val fps = measureFrameRate()
        val memoryUsage = getMemoryUsage()
        
        glassUIState = glassUIState.updatePerformanceMetrics(
            frameRate = fps,
            memoryUsage = memoryUsage,
            renderTime = 16L,
            gpuUtilization = 30.0f,
            batteryImpact = 8.0f
        )
        
        // Auto-adapt if performance degrades
        if (glassUIState.isPerformanceDegraded) {
            val newConfig = glassUIState.getRecommendedConfiguration()
            glassUIState = glassUIState.withConfiguration(newConfig)
        }
    }
}
```

### Performance Thresholds

```kotlin
companion object {
    const val MIN_FRAME_RATE_FPS = 45.0
    const val MAX_MEMORY_USAGE_MB = 50L
    const val MAX_LOAD_TIME_MS = 200L
    const val MAX_BATTERY_IMPACT_PERCENT = 15.0
    const val EMERGENCY_MODE_MAX_LATENCY_MS = 100L
}
```

## Emergency Mode

### Automatic Triggers
- Battery level below 10%
- Device temperature above 45°C
- Heavy vibration detected in low light
- User manually activates emergency mode

### Emergency Mode Changes
- Disables all blur effects (blurRadius = 0.0f)
- Increases opacity to 95% for maximum visibility
- Switches to red safety borders
- Disables all animations
- Increases touch target sizes by 20%
- Enables continuous haptic feedback

### Manual Emergency Toggle

```kotlin
GlassEmergencyToggle(
    isInEmergencyMode = glassUIState.isInEmergencyMode,
    configuration = glassUIState.configuration,
    onToggleEmergencyMode = {
        glassUIState = glassUIState.toggleEmergencyMode()
    }
)
```

## Construction Safety Features

### Safety Zone Indicators
- Visual safety zone grid overlay
- Color-coded safe/caution areas
- Construction zone legend

### PPE Compliance Reminders
- Rotating safety messages
- "Hard Hat Zone", "PPE Required", etc.
- Glass-enhanced safety notifications

### OSHA-Compliant Documentation
- Metadata overlay with project information
- GPS coordinates and timestamp embedding
- Company and project name display
- Location and address information

## Integration with Existing Camera

### Full Feature Parity

The glass camera maintains complete compatibility with the existing camera system:

```kotlin
// All existing camera functions work identically
val cameraViewModel: CameraViewModel = viewModel {
    CameraViewModel(context, locationService)
}

// Glass overlay enhances, doesn't replace
GlassCameraControls(
    uiState = cameraViewModel.uiState.collectAsState(),
    onFlashToggle = cameraViewModel::toggleFlashMode,
    onHDRToggle = cameraViewModel::toggleHDR,
    onCapture = { cameraViewModel.capturePhoto(imageCapture) },
    // ... all existing functionality
)
```

### Camera Features Supported
- ✅ Single shot, burst mode, timer mode, HDR, voice activation
- ✅ Flash control (auto, on, off)
- ✅ Grid overlays (rule of thirds, golden ratio, safety zones)
- ✅ Digital level indicator with glass enhancement
- ✅ Zoom controls with glass effects
- ✅ Tap-to-focus functionality
- ✅ Camera switching (front/back)
- ✅ Metadata embedding and overlay
- ✅ Storage management and photo counting

## Testing and Quality Assurance

### Unit Tests
Located in `src/test/java/com/hazardhawk/ui/glass/`
- Configuration validation tests
- Performance monitoring tests
- Environmental adaptation tests
- Error handling and fallback tests

### Integration Tests
Located in `src/androidTest/java/com/hazardhawk/ui/glass/`
- Camera integration tests
- UI interaction tests
- Performance regression tests
- Construction environment simulation tests

### Performance Benchmarks
- Frame rate monitoring (target: 45+ FPS)
- Memory usage tracking (limit: 50MB)
- Load time measurement (target: <200ms)
- Battery impact assessment (limit: 15%)

## Troubleshooting

### Common Issues

1. **Glass Effects Not Visible**
   - Check if `supportLevel` is set to `DISABLED`
   - Verify Haze library is properly included
   - Ensure `blurRadius > 0` and `opacity > 0`

2. **Poor Performance**
   - Monitor FPS with `GlassPerformanceIndicator`
   - Enable adaptive quality: `adaptiveQualityEnabled = true`
   - Use predefined low-performance config

3. **Touch Targets Too Small**
   - Increase `minTouchTargetSize` to 72dp for heavy gloves
   - Enable `isOutdoorMode` for construction optimization
   - Check accessibility compliance

4. **Emergency Mode Not Activating**
   - Verify `enableEmergencyOverride = true`
   - Check battery level and temperature thresholds
   - Test manual toggle functionality

### Debug Information

```kotlin
// Enable performance monitoring
val glassState = GlassUIState.constructionCamera().copy(
    configuration = configuration.copy(
        enablePerformanceMonitoring = true
    )
)

// View debug overlay
if (BuildConfig.DEBUG) {
    GlassPerformanceIndicator(
        fps = glassUIState.currentFrameRate,
        memoryMB = glassUIState.memoryUsageMB,
        isReduced = glassUIState.isPerformanceReduced,
        configuration = glassUIState.configuration
    )
}
```

## Future Enhancements

### Planned Features
- [ ] Machine learning-based environmental detection
- [ ] Advanced thermal management with device-specific profiles
- [ ] Voice-activated glass effect controls
- [ ] Custom company branding themes
- [ ] Offline performance analytics storage
- [ ] Integration with external construction sensors

### Performance Optimizations
- [ ] GPU-accelerated blur rendering
- [ ] Predictive performance scaling
- [ ] Background effect pre-calculation
- [ ] Memory pooling for glass components
- [ ] Differential rendering optimizations

## Dependencies

```kotlin
// Required dependencies in build.gradle.kts
implementation("dev.chrisbanes.haze:haze:0.9.0-beta02")
implementation("androidx.compose.animation:animation:1.5.4")
implementation("androidx.compose.material3:material3:1.1.2")
```

## License and Credits

This glass morphism implementation uses:
- [Haze](https://github.com/chrisbanes/haze) by Chris Banes for backdrop blur effects
- Material 3 design system for base components
- Construction safety standards and OSHA compliance guidelines

Built for HazardHawk construction safety platform with focus on real-world construction environment usability.