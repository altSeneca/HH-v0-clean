# HazardHawk Testing Framework

Comprehensive testing framework for the HazardHawk AI-powered construction safety platform, built with Kotlin Multiplatform for Android, iOS, Desktop, and Web deployment.

## Overview

This testing framework ensures reliability and performance for:
- **AI-powered hazard detection** (Gemma 3N E2B, Vertex AI, YOLO11)  
- **Real-time AR overlays** (30 FPS UI, 2 FPS AI analysis)
- **Document generation** (PTP, Toolbox Talks, Incident Reports)
- **Cross-platform compatibility** (Android, iOS, Desktop, Web)

## Framework Architecture

### Test Categories

#### 1. Unit Tests (`shared/src/commonTest/kotlin/`)
- **AI Orchestrator**: Fallback logic, performance, configuration
- **Document Generation**: PTP creation, quality assessment, OSHA compliance
- **Business Logic**: Safety analysis validation, data models
- **Cross-platform**: Works across Android, iOS, Desktop, Web

#### 2. Android Instrumented Tests (`androidApp/src/androidTest/`)
- **AR UI Components**: Hazard detection overlays, bounding boxes
- **Live Camera Integration**: Real-time analysis display, user interactions
- **Performance**: 30 FPS UI responsiveness during AI processing
- **Device Testing**: Camera permissions, network connectivity

#### 3. Performance Benchmarks (`shared/src/commonTest/kotlin/com/hazardhawk/performance/`)
- **AI Analysis Speed**: 2 FPS real-time target (500ms budget)
- **UI Responsiveness**: 30 FPS during analysis (33ms frame budget)  
- **Batch Processing**: 10+ images per minute throughput
- **Memory Efficiency**: <2GB peak usage, graceful degradation

#### 4. Integration Tests (`shared/src/commonTest/kotlin/com/hazardhawk/integration/`)
- **End-to-End Workflows**: Photo → Analysis → Document generation
- **Analyzer Fallback**: Gemma → Vertex AI → YOLO11 cascading
- **Offline/Online Transitions**: Local analysis with cloud enhancement
- **Batch Processing**: Mixed success/failure handling

## Quick Start

### Run All Tests
```bash
./run_hazardhawk_tests.sh
```

### Run Specific Test Suites
```bash
# Unit tests only (shared module)
./gradlew :shared:testDebugUnitTest

# Android UI tests (requires device/emulator)
./gradlew :androidApp:connectedDebugAndroidTest

# Performance benchmarks
./gradlew :shared:test --tests "*PerformanceBenchmarkTest"

# Integration tests
./gradlew :shared:test --tests "*EndToEndWorkflowTest"
```

### Individual Component Tests
```bash
# AI Orchestrator
./gradlew :shared:test --tests "*SmartAIOrchestratorTest"

# Document Generation  
./gradlew :shared:test --tests "*PTPGeneratorTest"

# AR UI Components
./gradlew :androidApp:test --tests "*HazardDetectionOverlayTest"
```

## Performance Requirements

### Construction Industry Targets
- **Real-time AI Analysis**: ≤500ms (2 FPS minimum)
- **UI Responsiveness**: ≤33ms frame time (30 FPS target)
- **Batch Processing**: ≥10 images per minute
- **Memory Usage**: <2GB peak, graceful degradation
- **Network Tolerance**: Offline capability with online enhancement

### Validation Criteria
- **Critical Hazard Detection**: <200ms response time
- **OSHA Compliance**: 100% accuracy for standard violations
- **Document Generation**: <5 seconds for complete PTP
- **AR Overlay Rendering**: No dropped frames during analysis

## Test Data & Scenarios

### Mock Safety Scenarios
- **Fall Protection**: Critical hazards, OSHA 1926.501 violations
- **PPE Violations**: Missing hard hats, safety equipment  
- **Electrical Hazards**: Exposed wiring, lockout/tagout issues
- **Housekeeping**: Tools in walkways, material storage
- **Mechanical**: Unguarded machinery, equipment defects

### Performance Test Scenarios
1. **Light Load**: 640x480, 1 hazard, 500ms target
2. **Medium Load**: 1280x720, 3 hazards, 1.5s target  
3. **Heavy Load**: 1920x1080, 5 hazards, 3s target
4. **Stress Test**: 4K resolution, 10+ hazards, 8s target

## Mock Services & Test Utilities

### TestDataFactory
```kotlin
// Create realistic construction safety scenarios
val analysis = TestDataFactory.createSampleSafetyAnalysis(
    analysisType = AnalysisType.LOCAL_GEMMA_MULTIMODAL,
    hazardCount = 3,
    includeCriticalHazards = true
)

// Generate performance test scenarios
val scenarios = TestDataFactory.createPerformanceScenarios()
```

### MockAIPhotoAnalyzer
```kotlin
// Simulate AI service behavior
val mockAnalyzer = MockAIPhotoAnalyzer(
    responseDelay = 2000L,
    shouldSucceed = true,
    customAnalysis = customSafetyAnalysis
)
```

### Performance Testing
```kotlin
// Measure execution time and validate performance
val (result, duration) = TestUtils.measureExecutionTime {
    analyzer.analyzePhoto(imageData, workType)
}

TestUtils.assertPerformanceWithin(
    actualMs = duration.inWholeMilliseconds,
    expectedMs = 500L,
    tolerancePercent = 20.0
)
```

## CI/CD Integration

### GitHub Actions / Jenkins
```yaml
- name: Run HazardHawk Tests
  run: ./run_hazardhawk_tests.sh
  env:
    CI: true
    ANDROID_API_LEVEL: 30
```

### Test Reporting
- **JUnit XML**: Compatible with CI/CD systems
- **Coverage Reports**: Jacoco HTML reports
- **Performance Metrics**: JSON output for monitoring
- **Screenshot Testing**: UI regression detection

## Platform-Specific Considerations

### Android
- **API Level**: Minimum 26, Target 34
- **Permissions**: Camera, location, storage
- **Hardware**: ARM64, x86_64 support
- **Testing**: Requires device/emulator for AR UI tests

### iOS (via KMP)
- **Deployment Target**: iOS 14.0+
- **Testing**: XCTest integration with shared KMP logic
- **Simulator**: Full testing support without physical device

### Desktop (JVM)
- **Runtime**: Java 11+ required
- **Testing**: Full headless testing support
- **Platform**: Windows, macOS, Linux compatible

### Web (JS/WASM)
- **Browsers**: Chrome, Firefox, Safari, Edge
- **Testing**: Karma/Jest integration planned
- **Performance**: WebAssembly optimization for AI processing

## Troubleshooting

### Common Issues

#### Android Tests Failing
```bash
# Check device connection
adb devices

# Install debug APK manually
./gradlew :androidApp:installDebug

# Check logcat for errors
adb logcat | grep HazardHawk
```

#### Performance Tests Slow
```bash
# Check system resources
./gradlew --stop  # Stop gradle daemon
./gradlew --status  # Check gradle processes

# Run with performance profiling
./gradlew :shared:test --profile
```

#### Mock Services Not Working
```bash
# Verify test data files
ls -la shared/src/commonTest/resources/test-data/

# Check mock implementations
./gradlew :shared:test --tests "*MockAIPhotoAnalyzer*" --info
```

## Extending the Framework

### Adding New Test Scenarios
1. Create test data in `TestDataFactory`
2. Add mock implementations as needed
3. Write test cases following existing patterns
4. Update automation script if needed

### Custom Performance Benchmarks
```kotlin
@Test
fun `custom performance scenario`() = runTest {
    val scenario = TestDataFactory.PerformanceTestScenario(
        name = "Custom Scenario",
        imageSize = 1920 to 1080,
        hazardCount = 5,
        expectedProcessingTime = 3000L,
        targetFPS = 1
    )
    
    val result = performanceRunner.runPerformanceTest(analyzer, scenario)
    // Add custom assertions
}
```

### Platform-Specific Tests
- **Android**: Add to `androidApp/src/androidTest/`
- **iOS**: Will integrate with KMP test framework
- **Desktop**: Add to desktop module test sources
- **Web**: Add to web module test sources

## Success Criteria

✅ **90%+ Test Coverage** for critical AI and safety components
✅ **Performance Targets Met** for all platform scenarios  
✅ **Zero Critical Failures** in hazard detection accuracy
✅ **Cross-Platform Compatibility** verified on all targets
✅ **CI/CD Integration** with automated deployment gates

This comprehensive testing framework ensures the HazardHawk AI safety platform meets construction industry reliability and performance requirements across all deployment platforms.
