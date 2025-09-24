# HazardHawk Cross-Platform Testing Framework

Comprehensive testing infrastructure for the HazardHawk tag management system across Android, iOS, Desktop, and Web platforms.

## Overview

This testing framework provides:
- **Unit Testing**: >90% code coverage with comprehensive component testing
- **Integration Testing**: Real database operations with in-memory SQLite
- **Performance Testing**: Load testing with 10,000+ tags, <100ms search requirement
- **Accessibility Testing**: TalkBack, high contrast, and construction-friendly UX
- **OSHA Compliance Testing**: Validation of safety tag compliance requirements
- **Cross-Platform Testing**: Automated testing on Android, iOS, Desktop, and Web
- **CI/CD Integration**: GitHub Actions pipeline with performance regression detection

## Test Structure

```
shared/src/commonTest/kotlin/com/hazardhawk/
├── test/
│   ├── TestDataFactory.kt           # Comprehensive test data generation
│   ├── MockRepositories.kt          # Mock implementations with configurable behavior
│   └── TestUtils.kt                 # Utility functions and common assertions
├── domain/engine/
│   └── TagRecommendationEngineTest.kt # Unit tests for recommendation algorithm
├── integration/
│   └── TagManagementIntegrationTest.kt # End-to-end integration tests
├── performance/
│   ├── TagPerformanceTests.kt       # Performance validation tests
│   └── PerformanceRegression.kt     # Automated regression detection
└── compliance/
    └── OSHAComplianceTests.kt       # OSHA safety compliance validation

androidApp/src/androidTest/kotlin/com/hazardhawk/ui/
├── TagSelectionFlowTest.kt          # Jetpack Compose UI tests
└── accessibility/
    └── AccessibilityTests.kt        # Android accessibility testing
```

## Test Categories

### 1. Unit Tests

**Coverage Target**: >90%
**Location**: `shared/src/commonTest/kotlin/`

```kotlin
// Example: Tag recommendation algorithm testing
@Test
fun `getRecommendedTags should apply correct weighted scoring`() = runTest {
    val result = engine.getRecommendedTags("user-id", "project-id")
    assertTrue(result.size <= 8)
    // Validate 40/30/30 weighting algorithm
}
```

**Key Features**:
- Algorithm validation (40% personal, 30% project, 30% industry weights)
- Mock repositories with configurable latency and error simulation
- Data model validation and serialization testing
- Edge case handling (empty datasets, special characters)

### 2. Integration Tests

**Location**: `shared/src/commonTest/kotlin/com/hazardhawk/integration/`

```kotlin
// Example: Complete tag workflow integration
@Test
fun `integration test - complete tag recommendation flow`() = runTest {
    // Real database operations with mock data
    val recommendations = getRecommendedTagsUseCase.execute(userId, projectId)
    // Validate end-to-end functionality
}
```

**Key Features**:
- In-memory SQLite database for realistic testing
- Cross-platform repository integration
- Real use case execution with mock data
- Conflict resolution and sync testing

### 3. Performance Tests

**Requirements**:
- Search performance: <100ms for 10,000+ tags
- Memory usage: <100MB under load
- Recommendation generation: <50ms average

```kotlin
@Test
fun `performance test - search with 10000 tags under 100ms`() = runTest {
    val performanceData = TestDataFactory.createPerformanceTestDataset(10000)
    val duration = measureTime {
        engine.searchTagsWithContext(query, userId, projectId, 10)
    }
    assertTrue(duration.inWholeMilliseconds < 100)
}
```

**Key Features**:
- Load testing with realistic construction datasets
- Memory usage profiling and leak detection
- Concurrent operation performance validation
- Database operation performance benchmarking

### 4. Accessibility Tests

**Location**: `androidApp/src/androidTest/kotlin/com/hazardhawk/ui/accessibility/`

```kotlin
@Test
fun tagSelectionDialog_hasProperContentDescriptions() {
    composeTestRule.onNodeWithContentDescription("Search tags")
        .assertExists("Search field should have content description")
}
```

**Key Features**:
- TalkBack compatibility testing
- High contrast mode validation
- Minimum touch target size (48dp) verification
- Keyboard navigation support
- One-handed operation testing
- Glove mode compatibility

### 5. OSHA Compliance Tests

**Location**: `shared/src/commonTest/kotlin/com/hazardhawk/compliance/`

```kotlin
@Test
fun `compliance test - fall protection scenarios require 1926.501`() = runTest {
    val recommendations = engine.getRecommendedTags("safety-inspector", "high-rise")
    val oshaRefs = recommendations.flatMap { it.oshaReferences }
    assertTrue(oshaRefs.contains("1926.501"))
}
```

**Key Features**:
- Construction scenario compliance validation
- OSHA reference format verification
- Multi-standard requirement testing (PPE, Fall Protection, Electrical, etc.)
- Emergency response compliance
- Hazard communication validation

### 6. Cross-Platform UI Tests

**Android**: Jetpack Compose Testing
**iOS**: XCTest and XCUITest
**Desktop**: Compose Multiplatform Testing
**Web**: Playwright/Karma integration

```kotlin
// Android Compose Test Example
@Test
fun tagSelectionDialog_selectsTag_whenTagChipPressed() {
    composeTestRule.onNodeWithText("Hard Hat Required")
        .performClick()
    verify { mockViewModel.selectTag(testTag) }
}
```

## Test Data Management

### TestDataFactory Features

```kotlin
object TestDataFactory {
    // Basic tag creation
    fun createTestTag(id: String, name: String, ...): Tag
    
    // Performance datasets
    fun createPerformanceTestDataset(count: Int): PerformanceTestData
    
    // Construction scenarios
    fun createConstructionScenarios(): List<ConstructionScenario>
    
    // Accessibility test scenarios
    fun createAccessibilityTestScenarios(): List<AccessibilityScenario>
}
```

**Key Features**:
- Realistic construction safety datasets
- Performance test data (10,000+ tags)
- Hierarchical tag structures
- Multi-language support (English, Spanish, Japanese)
- OSHA compliance scenarios

### Mock Repositories

```kotlin
class MockTagRepository : TagRepository {
    // Configurable behavior
    var shouldSimulateNetworkError = false
    var simulatedLatencyMs = 0L
    
    // Enhanced mock responses
    override suspend fun searchTagsByName(query: String, limit: Int): List<Tag>
}
```

**Key Features**:
- Configurable latency simulation
- Network error simulation
- Large dataset support
- Realistic usage patterns

## CI/CD Pipeline

### GitHub Actions Configuration

**File**: `.github/workflows/test-automation.yml`

**Pipeline Stages**:
1. **Shared Module Tests** (Unit, Integration, Performance)
2. **Android Tests** (Unit, Instrumented, Accessibility)
3. **iOS Tests** (Unit, UI)
4. **Desktop Tests** (Windows, macOS, Linux)
5. **Web Tests** (Chrome, Firefox, E2E)
6. **OSHA Compliance Tests**
7. **Performance Regression Detection**

**Test Matrix**:
- Android API levels: 28, 33
- iOS Xcode versions: 14.3.1, 15.0
- Desktop OS: Ubuntu, Windows, macOS
- Web browsers: Chrome, Firefox

### Performance Regression Detection

```yaml
# Automated performance monitoring
performance-regression:
  runs-on: ubuntu-latest
  if: github.event_name == 'schedule'
  
  steps:
  - name: Run Performance Benchmark
    run: ./gradlew :shared:testDebugUnitTest --tests "*.performance.*"
    
  - name: Store Benchmark Results
    uses: benchmark-action/github-action-benchmark@v1
    with:
      alert-threshold: '150%'
      fail-on-alert: true
```

## Code Coverage

### Kover Configuration

**Target**: >90% line coverage for unit tests, >80% for integration tests

```kotlin
kover {
    reports {
        verify {
            rule {
                bound {
                    minValue = 90
                    metric = MetricType.LINE
                    aggregation = AggregationType.COVERED_PERCENTAGE
                }
            }
        }
    }
}
```

**Exclusions**:
- Test files (`*.test.*`, `*.*Test*`)
- Mock objects (`*.mock.*`)
- Generated code
- Test data factories

## Running Tests

### Local Development

```bash
# Run all unit tests
./gradlew :shared:testDebugUnitTest

# Run specific test categories
./gradlew testUnit              # Unit tests only
./gradlew testIntegration       # Integration tests
./gradlew testPerformance       # Performance tests
./gradlew testCompliance        # OSHA compliance tests

# Run all tests with coverage
./gradlew testAll

# Android instrumented tests
./gradlew :androidApp:connectedDebugAndroidTest

# Generate coverage report
./gradlew koverHtmlReport
```

### Performance Benchmarking

```bash
# Run performance benchmarks
./gradlew benchmarkPerformance

# Validate test data integrity
./gradlew validateTestData
```

### Platform-Specific Tests

```bash
# iOS tests (requires macOS)
cd iosApp && xcodebuild test -workspace HazardHawk.xcworkspace -scheme HazardHawk-iOS

# Desktop tests
./gradlew :desktopApp:test

# Web tests
./gradlew :webApp:jsTest
```

## Quality Metrics

### Current Targets

- **Unit Test Coverage**: >90%
- **Integration Test Coverage**: >80%
- **Performance Requirements**:
  - Tag search: <100ms for 10,000+ tags
  - Recommendation generation: <50ms average
  - Memory usage: <100MB under load
- **Accessibility**: Zero critical violations
- **OSHA Compliance**: 100% validation coverage

### Performance Baselines

| Operation | Target | Threshold |
|-----------|--------|-----------|
| Tag Search (10k tags) | <100ms avg | <200ms max |
| Recommendation Gen | <50ms avg | <100ms max |
| Database Operations | <150ms avg | <300ms max |
| Memory Usage | <50MB | <100MB |
| Concurrent Throughput | >10 ops/sec | >5 ops/sec |

## Test Execution Reports

### Automated Reporting

The CI/CD pipeline generates:
- **Coverage Reports**: HTML and XML formats
- **Performance Metrics**: JSON benchmark data
- **Accessibility Reports**: Compliance validation
- **OSHA Compliance**: Safety requirement validation
- **Cross-Platform Summary**: Multi-platform test results

### Local Reports

```bash
# View coverage report
open shared/build/reports/kover/html/index.html

# View test results
open shared/build/reports/tests/testDebugUnitTest/index.html
```

## Contributing

### Adding New Tests

1. **Unit Tests**: Add to appropriate package in `commonTest`
2. **Integration Tests**: Use `MockInMemoryDatabase` for realistic scenarios
3. **Performance Tests**: Validate against established baselines
4. **UI Tests**: Follow platform-specific testing patterns
5. **Mock Data**: Extend `TestDataFactory` for new scenarios

### Test Best Practices

- Use descriptive test names with backticks
- Follow Arrange-Act-Assert pattern
- Mock external dependencies appropriately
- Validate performance requirements
- Include accessibility considerations
- Test OSHA compliance scenarios
- Use realistic construction industry data

### Performance Considerations

- Always measure performance in tests
- Use `measureTime` for timing validation
- Test with realistic data sizes
- Validate memory usage patterns
- Consider concurrent operation scenarios

## Monitoring and Alerts

### Automated Alerts

- **Performance Regression**: >150% baseline increase
- **Coverage Drop**: Below 90% unit test coverage
- **Test Failures**: Immediate notification on CI/CD failure
- **Accessibility Issues**: Critical violation detection

### Dashboard Integration

Performance metrics and test results are available through:
- GitHub Actions Summary
- Codecov Coverage Reports
- Performance Benchmark Tracking
- Test Result Artifacts

This comprehensive testing framework ensures the HazardHawk tag management system maintains high quality, performance, and compliance across all supported platforms.