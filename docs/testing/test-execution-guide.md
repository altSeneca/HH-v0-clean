# Phase 2 Test Execution Guide

**Version**: 1.0  
**Last Updated**: October 9, 2025  
**Owner**: test-guardian agent

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Test Categories](#test-categories)
3. [Running Tests Locally](#running-tests-locally)
4. [Running Tests in CI/CD](#running-tests-in-cicd)
5. [Test Coverage](#test-coverage)
6. [Debugging Tests](#debugging-tests)
7. [Performance Testing](#performance-testing)
8. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites

- JDK 17 or later
- Android SDK (for Android tests)
- Docker and Docker Compose (for integration tests)
- Node.js 18+ (for web E2E tests)

### Run All Unit Tests

```bash
cd HazardHawk
./gradlew :shared:test
```

### Run All Integration Tests

```bash
# Start Localstack
docker-compose -f docker-compose.test.yml up -d localstack

# Run integration tests
./gradlew :shared:integrationTest
```

### Run All E2E Tests (Android)

```bash
./gradlew :androidApp:connectedDebugAndroidTest
```

---

## Test Categories

### Unit Tests (110 tests target)

**Purpose**: Test individual components in isolation  
**Speed**: Fast (< 10 minutes total)  
**Coverage Target**: 85%+

**Location**:
- `/HazardHawk/shared/src/commonTest/kotlin/**/*Test.kt`
- `/HazardHawk/androidApp/src/test/java/**/*Test.kt`

**Examples**:
- `FileUploadServiceTest.kt` - Tests file upload logic
- `OCRServiceTest.kt` - Tests document AI extraction
- `NotificationServiceTest.kt` - Tests notification delivery
- `CertificationRepositoryTest.kt` - Tests repository operations

**Run Command**:
```bash
# All unit tests
./gradlew :shared:test

# Specific test class
./gradlew :shared:test --tests "com.hazardhawk.domain.services.FileUploadServiceTest"

# Specific test method
./gradlew :shared:test --tests "*.FileUploadServiceTest.uploadFile compresses image if over 500KB"
```

---

### Integration Tests (30 tests target)

**Purpose**: Test interactions with external services  
**Speed**: Medium (< 15 minutes total)  
**Dependencies**: Localstack, test database

**Location**:
- `/HazardHawk/shared/src/commonTest/kotlin/**/*IntegrationTest.kt`
- `/HazardHawk/shared/src/commonTest/kotlin/**/*IT.kt`

**Examples**:
- `S3ClientIntegrationTest.kt` - Tests actual S3 uploads to Localstack
- `OCRClientIntegrationTest.kt` - Tests Document AI API
- `NotificationClientIntegrationTest.kt` - Tests SendGrid/Twilio integration

**Setup**:
```bash
# Start Localstack and dependencies
docker-compose -f docker-compose.test.yml up -d

# Verify services are ready
curl http://localhost:4566/_localstack/health
curl http://localhost:5433  # PostgreSQL
```

**Run Command**:
```bash
# All integration tests
./gradlew :shared:integrationTest

# With environment variables
AWS_ENDPOINT=http://localhost:4566 \
AWS_ACCESS_KEY_ID=test \
AWS_SECRET_ACCESS_KEY=test \
./gradlew :shared:integrationTest
```

**Cleanup**:
```bash
# Stop services
docker-compose -f docker-compose.test.yml down

# Clean up volumes
docker-compose -f docker-compose.test.yml down -v
```

---

### E2E Tests (15 tests target)

**Purpose**: Test complete user workflows  
**Speed**: Slow (< 20 minutes total)  
**Dependencies**: Emulator or physical device

**Android E2E Tests**:

**Location**:
- `/HazardHawk/androidApp/src/androidTest/java/**/*E2ETest.kt`

**Setup**:
```bash
# Start Android emulator
emulator -avd Pixel_6_API_33 -no-snapshot-load

# Or connect physical device
adb devices
```

**Run Command**:
```bash
# All E2E tests
./gradlew :androidApp:connectedDebugAndroidTest

# Specific test class
./gradlew :androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.hazardhawk.CertificationUploadE2ETest
```

**Web E2E Tests**:

**Location**:
- `/hazardhawk-web/tests/e2e/**/*.spec.ts`

**Setup**:
```bash
cd hazardhawk-web
npm install
npx playwright install
```

**Run Command**:
```bash
# All web E2E tests
npm run test:e2e

# Headed mode (watch browser)
npm run test:e2e:headed

# Specific test file
npx playwright test tests/e2e/certification-upload.spec.ts
```

---

## Running Tests Locally

### Development Workflow

**1. Before committing code**:
```bash
# Run relevant unit tests
./gradlew :shared:test --tests "*FileUpload*"

# Quick smoke test
./gradlew :shared:test --tests "*Smoke*"
```

**2. Before pushing to remote**:
```bash
# Run all unit tests
./gradlew :shared:test

# Run quick integration tests
docker-compose -f docker-compose.test.yml up -d localstack
./gradlew :shared:integrationTest --tests "*S3*"
docker-compose -f docker-compose.test.yml down
```

**3. Before creating PR**:
```bash
# Full test suite
./gradlew clean test integrationTest
```

### Watch Mode (TDD)

```bash
# Auto-rerun tests on file changes
./gradlew :shared:test --continuous

# With specific tests
./gradlew :shared:test --continuous --tests "*FileUpload*"
```

### Parallel Execution

```bash
# Run tests in parallel (faster)
./gradlew :shared:test --parallel --max-workers=4
```

---

## Running Tests in CI/CD

### GitHub Actions

Tests run automatically on:
- Every push to `main`, `develop`, `feature/**`
- Every pull request

**View Results**:
1. Go to GitHub repository
2. Click "Actions" tab
3. Select workflow run
4. View job logs and test reports

**Workflow Jobs**:
- `unit-tests` - Shared module unit tests (10 min)
- `android-unit-tests` - Android app unit tests (15 min)
- `integration-tests` - Integration tests with Localstack (15 min)
- `e2e-android-tests` - Android E2E tests (20 min, PRs only)
- `performance-tests` - Load tests (15 min, main branch only)

**Download Test Reports**:
1. Go to completed workflow run
2. Scroll to "Artifacts" section
3. Download:
   - `unit-test-reports`
   - `integration-test-reports`
   - `e2e-android-test-reports-api-XX`
   - `performance-test-results`

### Skipping CI Tests

```bash
# Skip CI on commit (use sparingly!)
git commit -m "docs: Update README [skip ci]"
```

---

## Test Coverage

### Generate Coverage Reports

```bash
# Generate JaCoCo report
./gradlew :shared:test :shared:jacocoTestReport

# Open HTML report
open HazardHawk/shared/build/reports/jacoco/test/html/index.html
```

### Coverage Targets

| Module | Target | Current |
|--------|--------|---------|
| Shared Module | 85% | TBD |
| Domain Layer | 90% | TBD |
| Data Layer | 80% | TBD |
| Android App | 75% | TBD |

### Coverage Verification

```bash
# Fail build if coverage below 80%
./gradlew :shared:jacocoTestCoverageVerification
```

### View Coverage in IDE

**IntelliJ IDEA / Android Studio**:
1. Run tests with coverage: `Run > Run with Coverage`
2. View inline coverage indicators
3. Generate report: `Tools > Generate Coverage Report`

---

## Debugging Tests

### Print Debug Logs

```kotlin
@Test
fun `test with debug logs`() = runTest {
    println("DEBUG: Starting test...")
    val result = service.doSomething()
    println("DEBUG: Result = $result")
    assertTrue(result.isSuccess)
}
```

### Run Single Test with Logs

```bash
./gradlew :shared:test \
  --tests "FileUploadServiceTest.uploadFile compresses image" \
  --info
```

### Debug in IDE

**IntelliJ IDEA / Android Studio**:
1. Set breakpoint in test
2. Right-click test method
3. Select "Debug 'testMethodName'"
4. Step through code

### View Test Output

```bash
# Show standard output/error
./gradlew :shared:test --info

# Show all logs
./gradlew :shared:test --debug
```

### Common Issues

**Issue**: `kotlinx.coroutines.test.UncompletedCoroutinesError`

**Solution**: Use `runTest` instead of `runBlocking`:
```kotlin
@Test
fun `async test`() = runTest {  // Use runTest
    val result = suspendFunction()
    assertEquals(expected, result)
}
```

**Issue**: `java.lang.OutOfMemoryError: Java heap space`

**Solution**: Increase test heap size in `build.gradle.kts`:
```kotlin
tasks.withType<Test> {
    maxHeapSize = "2g"
}
```

**Issue**: Tests pass locally but fail in CI

**Solution**: Check for:
- Hardcoded file paths (use relative paths)
- Time zone dependencies (use UTC)
- Random data without seeds
- Flaky network mocks

---

## Performance Testing

### Load Testing with k6

**Prerequisites**:
```bash
# Install k6
brew install k6  # macOS
# Or: sudo apt install k6  # Linux
```

**Run Load Tests**:
```bash
# Phase 2 certification upload test
k6 run load-tests/phase2-certification-upload.js

# With custom parameters
k6 run load-tests/phase2-certification-upload.js \
  --vus 50 \
  --duration 5m
```

**Interpret Results**:
```
✓ presigned URL status is 200
✓ presigned URL response time < 200ms
✓ OCR status is 200
✓ OCR response time < 15s

http_req_duration..............: avg=145ms  p95=195ms  p99=450ms
http_req_failed................: 0.25%
```

**Performance Targets** (from plan):
- Presigned URL p50: < 100ms
- Presigned URL p95: < 200ms
- OCR Processing p50: < 10s
- OCR Processing p95: < 15s
- API Error Rate: < 0.5%

### Profile Memory Usage

```bash
# Run tests with profiler
./gradlew :shared:test -Pprofile=true

# Analyze heap dump
jhat HazardHawk/shared/build/test-heapdump.hprof
```

---

## Troubleshooting

### Tests Failing After Dependency Update

```bash
# Clean and rebuild
./gradlew clean
./gradlew :shared:build --refresh-dependencies
```

### Gradle Daemon Issues

```bash
# Stop daemon
./gradlew --stop

# Run without daemon
./gradlew :shared:test --no-daemon
```

### Localstack Not Starting

```bash
# Check logs
docker-compose -f docker-compose.test.yml logs localstack

# Restart
docker-compose -f docker-compose.test.yml restart localstack

# Reset completely
docker-compose -f docker-compose.test.yml down -v
docker-compose -f docker-compose.test.yml up -d
```

### Test Hangs Indefinitely

Possible causes:
1. **Deadlock in coroutines**: Use `runTest` with timeout
2. **Mock not configured**: Check mock setup
3. **Network timeout**: Reduce timeout in test config

```kotlin
@Test(timeout = 5000) // 5 second timeout
fun `test that might hang`() = runTest {
    withTimeout(3000) {  // Additional timeout
        val result = service.operation()
        assertNotNull(result)
    }
}
```

### Android Emulator Tests Fail

```bash
# Check emulator status
adb devices

# Restart ADB server
adb kill-server
adb start-server

# Clear app data
adb shell pm clear com.hazardhawk.debug
```

---

## Best Practices

1. **Run tests frequently** - Catch bugs early
2. **Write tests first** - TDD approach
3. **Keep tests fast** - Mock external dependencies
4. **Make tests deterministic** - No random failures
5. **Clean up resources** - Close files, stop services
6. **Use descriptive names** - Test names explain what they test
7. **One assertion per test** - Or use soft assertions
8. **Avoid test interdependencies** - Tests should run independently

---

## Additional Resources

- [Test Writing Guidelines](/docs/testing/test-writing-guidelines.md)
- [Mock API Guide](/docs/testing/mock-api-guide.md)
- [Phase 2 Implementation Plan](/docs/plan/20251009-103400-phase2-backend-integration-plan.md)
- [GitHub Actions Workflow](/.github/workflows/phase2-tests.yml)

---

**Questions?** Contact test-guardian agent or check project documentation.
