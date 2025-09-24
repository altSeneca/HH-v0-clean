# HazardHawk Smart Camera Testing Infrastructure

Comprehensive testing suite for the HazardHawk Smart Camera implementation, covering all components from photo capture through AI analysis and tag management.

## Test Suite Overview

### Unit Tests (80+ tests)

#### Core Algorithm Tests
- **TagRecommendationEngineTest** (12 tests)
  - Verifies 40/30/30 weighted algorithm (personal/project/industry)
  - Tests recency boost (7-day window)
  - Validates promotion threshold (5 uses)
  - Ensures maximum 8 recommendations

- **PhotoMetadataEmbedderTest** (13 tests)
  - EXIF data embedding (GPS, timestamps, custom metadata)
  - GPS reference direction validation (N/S/E/W)
  - HazardHawk photo identification
  - Corrupted file handling

- **AIAnalysisParserTest** (9 tests)
  - Gemini Vision API JSON parsing
  - Hazard detection and OSHA code extraction
  - Risk level validation (LOW/MEDIUM/HIGH)
  - Malformed JSON handling

- **SyncConflictResolverTest** (10 tests)
  - Timestamp-based conflict resolution
  - Metadata merging strategies
  - Tag usage count aggregation
  - Batch conflict resolution

- **OfflineQueueManagerTest** (12 tests)
  - Priority-based photo queuing
  - Exponential backoff retry logic
  - Queue pause/resume functionality
  - Upload progress tracking

### Integration Tests (6 tests)

#### End-to-End Pipeline Tests
- **CameraCapturePipelineTest** (6 tests)
  - Complete capture-to-analysis flow
  - Offline mode handling
  - Retry mechanism validation
  - Concurrent capture handling
  - Metadata preservation

### UI Tests (25+ tests)

#### Compose Testing Suite
- **CameraScreenTest** (13 tests)
  - Camera permission handling
  - Photo capture UI flow
  - Metadata overlay display
  - Error state handling
  - Navigation testing

- **TagSelectionFlowTest** (12 tests)
  - Tag selection/deselection
  - Search functionality
  - Custom tag creation
  - Compliance mode filtering
  - Category filtering

### Performance Tests (10 tests)

#### Timing Requirements Validation
- **Camera Launch Time**: < 2 seconds ✓
- **Photo Capture Speed**: < 500ms ✓
- **Tag Recommendation Speed**: < 100ms ✓
- **Local Database Saves**: < 50ms ✓
- **Large Dataset Performance**: < 300ms ✓
- **Concurrent Operations**: < 150ms average ✓
- **Memory Stability**: < 50MB increase ✓

## Test Data Management

### TestDataFactory
```kotlin
TestDataFactory.createTestTag()         // Standard safety tags
TestDataFactory.createPersonalTopTags() // User-specific tags
TestDataFactory.createProjectTopTags()  // Project-specific tags
TestDataFactory.createIndustryStandardTags() // OSHA compliance tags
TestDataFactory.createGeminiApiResponseJson() // Mock AI responses
```

### MockRepositories
- **MockTagRepository**: Controlled tag data for testing
- **MockPhotoRepository**: File system simulation
- **MockAnalysisRepository**: AI response simulation

## CI/CD Pipeline

### GitHub Actions Workflow
1. **Unit Tests**: Common, Android, Desktop
2. **Android Tests**: Unit + Instrumented (API 30 emulator)
3. **iOS Tests**: KMP shared module + XCTest
4. **Performance Tests**: Timing validation
5. **Code Coverage**: 80% minimum requirement

### Test Execution Commands
```bash
# Run all tests
./gradlew test

# Platform-specific tests
./gradlew :shared:commonTest
./gradlew :shared:androidUnitTest
./gradlew :androidApp:testDebugUnitTest
./gradlew :androidApp:connectedDebugAndroidTest

# Performance validation
./gradlew :shared:testPerformance

# Code coverage
./gradlew :shared:koverXmlReport
```

## Test Architecture

### Directory Structure
```
shared/src/
├── commonTest/kotlin/com/hazardhawk/
│   ├── test/
│   │   ├── TestDataFactory.kt
│   │   └── MockRepositories.kt
│   ├── domain/engine/
│   │   └── TagRecommendationEngineTest.kt
│   ├── ai/
│   │   └── AIAnalysisParserTest.kt
│   ├── data/
│   │   ├── SyncConflictResolverTest.kt
│   │   └── OfflineQueueManagerTest.kt
│   ├── integration/
│   │   └── CameraCapturePipelineTest.kt
│   └── performance/
│       └── PerformanceTests.kt
├── androidUnitTest/kotlin/com/hazardhawk/
│   └── service/
│       └── PhotoMetadataEmbedderTest.kt

androidApp/src/androidTest/kotlin/com/hazardhawk/
└── ui/
    ├── CameraScreenTest.kt
    └── TagSelectionFlowTest.kt
```

## Test Coverage Goals

### Critical Path Coverage (>90%)
- Camera capture pipeline
- AI analysis processing
- Sync and conflict resolution
- Tag recommendation algorithm

### Overall Coverage (>80%)
- All business logic
- Repository implementations
- Use cases and domain services
- UI components and ViewModels

## Testing Best Practices

### Test Naming Convention
```kotlin
fun `methodName should expectedBehavior when condition`()
fun `getRecommendedTags should return maximum 8 tags`()
fun `embedMetadata should embed GPS coordinates correctly`()
```

### Mock Usage Guidelines
- Use MockK for Kotlin-friendly mocking
- Create test doubles for external dependencies
- Verify interactions for critical operations
- Use relaxed mocks for non-critical dependencies

### Performance Test Standards
- All timing requirements must pass consistently
- Memory usage must remain stable during extended operation
- Concurrent operations should not degrade performance
- Large datasets should not significantly impact response times

## Quality Gates

### Pre-Merge Requirements
- All unit tests pass
- Android instrumented tests pass
- Performance benchmarks meet requirements
- Code coverage >= 80%
- No critical security vulnerabilities

### Release Requirements
- Full test suite passes on all platforms
- Performance tests validate < 2s camera launch
- UI tests confirm complete user flows
- Integration tests verify end-to-end functionality

## Test Maintenance

### Regular Updates
- Update test data as business rules evolve
- Maintain performance benchmarks with hardware changes
- Expand UI tests for new features
- Review mock implementations for accuracy

### Monitoring
- Track test execution times
- Monitor flaky test patterns
- Analyze code coverage trends
- Validate performance regression alerts

## Running Tests Locally

### Prerequisites
- JDK 17
- Android SDK (API 30+)
- Xcode 15+ (for iOS tests on macOS)

### Quick Start
```bash
# Clone and setup
git clone <repository>
cd HazardHawk

# Run unit tests
./gradlew :shared:commonTest

# Run Android tests (requires emulator)
./gradlew :androidApp:connectedDebugAndroidTest

# Run performance validation
./gradlew :shared:testPerformance
```

---

**Testing Infrastructure Status**: ✅ Complete  
**Coverage Goal**: 80%+ achieved  
**Performance Requirements**: All benchmarks passing  
**CI/CD Integration**: Fully automated  

This comprehensive testing infrastructure ensures HazardHawk Smart Camera delivers reliable, performant safety analysis for construction teams.