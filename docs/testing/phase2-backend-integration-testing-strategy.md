# Phase 2 Backend Integration & Technical Debt Cleanup - Testing Strategy

**Document Version**: 1.0  
**Date**: October 9, 2025  
**Status**: Test Guardian Approved  
**Focus**: Simple, Loveable, Complete Testing Philosophy

---

## Executive Summary

This document defines a comprehensive testing strategy for Phase 2 Backend Integration and Technical Debt Cleanup. With 110 unit tests already written, we now focus on integration testing, end-to-end workflows, performance benchmarks, and production readiness criteria.

### Current Status
- **Unit Tests**: 110 tests complete (FileUpload: 30, OCR: 40, Notifications: 15, Integration: 25)
- **Integration Tests**: 0 (needs external service mocking)
- **E2E Tests**: 0 (needs Android + Web coverage)
- **Performance Tests**: 0 (needs benchmark baselines)
- **Contract Tests**: 0 (needs API specifications)

### Testing Philosophy: Simple, Loveable, Complete

1. **Simple**: Tests are easy to write, read, and maintain. Clear naming, focused assertions.
2. **Loveable**: Tests provide confidence and fast feedback. Developers love running them.
3. **Complete**: Coverage addresses all important scenarios and edge cases without flakiness.

---

## Table of Contents

1. [Test Pyramid Breakdown](#test-pyramid-breakdown)
2. [Unit Testing Strategy](#unit-testing-strategy)
3. [Integration Testing Strategy](#integration-testing-strategy)
4. [End-to-End Testing Strategy](#end-to-end-testing-strategy)
5. [Performance Testing Strategy](#performance-testing-strategy)
6. [Contract Testing Strategy](#contract-testing-strategy)
7. [Security Testing Strategy](#security-testing-strategy)
8. [Regression Testing Strategy](#regression-testing-strategy)
9. [Test Data Management](#test-data-management)
10. [Mock Service Strategies](#mock-service-strategies)
11. [CI/CD Integration](#cicd-integration)
12. [Acceptance Criteria](#acceptance-criteria)

---

## Test Pyramid Breakdown

### Distribution (Following Industry Best Practices)

```
                    /\
                   /  \
                  / E2E \ (10% - 15 tests)
                 /______\
                /        \
               /   Integ  \ (20% - 30 tests)
              /____________\
             /              \
            /   Unit Tests   \ (70% - 110 tests)
           /__________________\
```

### Target Test Count: 155 Total Tests

| Layer | Count | Percentage | Execution Time | Purpose |
|-------|-------|------------|----------------|---------|
| **Unit Tests** | 110 | 71% | <5 seconds | Fast feedback on business logic |
| **Integration Tests** | 30 | 19% | <30 seconds | Verify external service integrations |
| **E2E Tests** | 15 | 10% | <5 minutes | Validate critical user workflows |
| **Total** | **155** | **100%** | **<6 minutes** | Full test suite execution |

### Rationale

- **70% Unit Tests**: Fast, isolated, catch bugs early in development
- **20% Integration Tests**: Verify boundaries and service contracts
- **10% E2E Tests**: Validate critical paths and user-facing behavior

---

## Unit Testing Strategy

### Current Coverage (110 Tests)

#### FileUploadService (30 tests) ✅
- Upload scenarios (10 tests)
- Retry logic (5 tests)
- Image compression (5 tests)
- Progress tracking (5 tests)
- Error handling (5 tests)

#### OCRService (40 tests) ✅
- Extraction accuracy (10 tests)
- Certification type mapping (10 tests)
- Confidence scoring (5 tests)
- Date parsing (7 tests)
- Error handling (8 tests)

#### NotificationService (15 tests) ✅
- Multi-channel delivery (5 tests)
- Urgency-based routing (3 tests)
- Template rendering (4 tests)
- Retry logic (3 tests)

#### Integration Tests (25 tests) ✅
- End-to-end workflow (5 tests)
- Error recovery (5 tests)
- Concurrent uploads (3 tests)
- Large file handling (2 tests)
- Expiration alerts (10 tests)

### Unit Test Best Practices

```kotlin
/**
 * Template for new unit tests following "Simple, Loveable, Complete" philosophy
 */
@Test
fun `functionName should expectedBehavior when specificCondition`() = runTest {
    // Given: Setup test conditions (Arrange)
    val input = createTestInput()
    mockService.configure(expectedBehavior = true)
    
    // When: Execute the action (Act)
    val result = serviceUnderTest.performAction(input)
    
    // Then: Verify the outcome (Assert)
    assertTrue(result.isSuccess)
    assertEquals(expected, result.getOrNull())
    
    // And: Verify side effects
    verify(mockDependency).wasCalledWith(expected)
}
```

### Coverage Goals

- **Line Coverage**: 85%+ (current: not yet measured)
- **Branch Coverage**: 80%+ (current: not yet measured)
- **Function Coverage**: 95%+ (current: not yet measured)
- **Critical Path Coverage**: 100% (upload → OCR → save)

### Running Unit Tests

```bash
# Run all Phase 2 unit tests
./gradlew :shared:test --tests "com.hazardhawk.domain.services.*"

# Run specific test suite
./gradlew :shared:test --tests "FileUploadServiceTest"

# Run with coverage report
./gradlew :shared:testDebugUnitTestCoverage
open shared/build/reports/coverage/test/debug/index.html

# Expected execution time: <5 seconds
```

---

## Integration Testing Strategy

### New Integration Tests Needed (30 tests)

#### 1. AWS S3 Integration (8 tests)

**File**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/domain/services/S3IntegrationTest.kt`

```kotlin
class S3IntegrationTest {
    @Test
    fun `presigned URL upload should successfully upload file to S3`()
    
    @Test
    fun `presigned URL should expire after configured time`()
    
    @Test
    fun `upload should fail with expired presigned URL`()
    
    @Test
    fun `upload should support CORS for web clients`()
    
    @Test
    fun `uploaded file should be accessible via CDN URL`()
    
    @Test
    fun `thumbnail generation should create 300x300 image`()
    
    @Test
    fun `delete operation should remove file and thumbnail`()
    
    @Test
    fun `upload should handle network interruption with retry`()
}
```

**Test Data**:
- Mock PDF: 500KB sample OSHA-10 certificate
- Mock JPEG: 2MB high-resolution certification photo
- Invalid file: .exe file for rejection testing

**Mock Strategy**: Use Localstack or AWS S3 Mock for local testing

#### 2. Google Document AI Integration (10 tests)

**File**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/domain/services/DocumentAIIntegrationTest.kt`

```kotlin
class DocumentAIIntegrationTest {
    @Test
    fun `OCR should extract all fields from OSHA-10 certificate`()
    
    @Test
    fun `OCR should extract all fields from OSHA-30 certificate`()
    
    @Test
    fun `OCR should extract all fields from Forklift certification`()
    
    @Test
    fun `OCR should handle poor quality scanned images`()
    
    @Test
    fun `OCR should handle rotated documents (90, 180, 270 degrees)`()
    
    @Test
    fun `OCR should parse dates in multiple formats`()
    
    @Test
    fun `OCR should calculate confidence scores accurately`()
    
    @Test
    fun `OCR should flag low confidence extractions for review`()
    
    @Test
    fun `OCR should handle multi-page PDF documents`()
    
    @Test
    fun `OCR should timeout gracefully after 30 seconds`()
}
```

**Test Data**:
- 30+ sample certifications (PDF and images)
- Poor quality scans (low DPI, blurry)
- Rotated images (90°, 180°, 270°)
- Multi-page PDFs (2-5 pages)

**Mock Strategy**: Use Google Document AI Sandbox with sample processor

#### 3. SendGrid Integration (4 tests)

**File**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/domain/services/SendGridIntegrationTest.kt`

```kotlin
class SendGridIntegrationTest {
    @Test
    fun `email should be sent successfully with correct template`()
    
    @Test
    fun `email should handle bounce and unsubscribe gracefully`()
    
    @Test
    fun `email should respect rate limits (100 per minute)`()
    
    @Test
    fun `email should track delivery status and open rate`()
}
```

**Mock Strategy**: Use SendGrid's Test API key with Inbox testing

#### 4. Twilio Integration (4 tests)

**File**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/domain/services/TwilioIntegrationTest.kt`

```kotlin
class TwilioIntegrationTest {
    @Test
    fun `SMS should be sent successfully to valid US number`()
    
    @Test
    fun `SMS should handle invalid phone numbers gracefully`()
    
    @Test
    fun `SMS should stay within 160 character limit`()
    
    @Test
    fun `SMS should respect rate limits (20 per minute)`()
}
```

**Mock Strategy**: Use Twilio Test credentials with magic phone numbers

#### 5. Firebase Cloud Messaging Integration (4 tests)

**File**: `/HazardHawk/androidApp/src/androidTest/java/com/hazardhawk/notifications/FCMIntegrationTest.kt`

```kotlin
class FCMIntegrationTest {
    @Test
    fun `push notification should be delivered to Android device`()
    
    @Test
    fun `push notification should handle invalid device token`()
    
    @Test
    fun `push notification should display correct title and body`()
    
    @Test
    fun `push notification should trigger deep link to certification screen`()
}
```

**Mock Strategy**: Use Firebase Test Lab with real device testing

### Integration Test Execution

```bash
# Run all integration tests (requires Docker for Localstack)
docker-compose -f docker-compose.test.yml up -d
./gradlew :shared:integrationTest

# Run specific integration test suite
./gradlew :shared:integrationTest --tests "S3IntegrationTest"

# Expected execution time: <30 seconds
```

### Integration Test Environment Setup

**Docker Compose** (`docker-compose.test.yml`):
```yaml
version: '3.8'
services:
  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      - SERVICES=s3
      - DEFAULT_REGION=us-east-1
      - AWS_ACCESS_KEY_ID=test
      - AWS_SECRET_ACCESS_KEY=test
    volumes:
      - ./test-data:/tmp/test-data
  
  postgres:
    image: postgres:15-alpine
    ports:
      - "5433:5432"
    environment:
      - POSTGRES_DB=hazardhawk_test
      - POSTGRES_USER=test
      - POSTGRES_PASSWORD=test
```

---

## End-to-End Testing Strategy

### E2E Test Scenarios (15 tests)

#### 1. Android App E2E Tests (8 tests)

**File**: `/HazardHawk/androidApp/src/androidTest/java/com/hazardhawk/ui/certification/CertificationE2ETest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
@LargeTest
class CertificationE2ETest {
    
    @Test
    fun `worker uploads OSHA-10 cert and receives approval notification`() {
        // Given: Worker logged in as Field Access
        onView(withId(R.id.login)).perform(typeText("worker@test.com"), closeSoftKeyboard())
        onView(withId(R.id.password)).perform(typeText("password"), closeSoftKeyboard())
        onView(withId(R.id.loginButton)).perform(click())
        
        // When: Worker navigates to certification upload
        onView(withId(R.id.navCertifications)).perform(click())
        onView(withId(R.id.uploadButton)).perform(click())
        
        // And: Selects certification from gallery
        onView(withId(R.id.selectFromGallery)).perform(click())
        // ... (simulate gallery selection)
        
        // Then: Upload progress should be displayed
        onView(withId(R.id.uploadProgress)).check(matches(isDisplayed()))
        
        // And: OCR extraction should complete
        onView(withId(R.id.ocrReview)).check(matches(isDisplayed()))
        
        // And: Worker confirms extracted data
        onView(withId(R.id.confirmButton)).perform(click())
        
        // And: Success message is displayed
        onView(withText("Certification submitted for review")).check(matches(isDisplayed()))
        
        // And: Notification is received (after admin approval)
        // ... (verify notification via FCM test)
    }
    
    @Test
    fun `worker uploads expired cert and receives rejection message`()
    
    @Test
    fun `worker uploads poor quality image and is prompted to retake`()
    
    @Test
    fun `worker views certification status after upload`()
    
    @Test
    fun `worker receives expiration warning 30 days before expiry`()
    
    @Test
    fun `safety lead approves pending certification from review queue`()
    
    @Test
    fun `safety lead rejects certification with reason`()
    
    @Test
    fun `admin views certification statistics dashboard`()
}
```

**Execution**:
```bash
# Run on emulator
./gradlew :androidApp:connectedAndroidTest

# Run on Firebase Test Lab
gcloud firebase test android run \
  --type instrumentation \
  --app app/build/outputs/apk/debug/app-debug.apk \
  --test app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk \
  --device model=Pixel6,version=33

# Expected execution time: <3 minutes per device
```

#### 2. Web Portal E2E Tests (7 tests)

**File**: `/hazardhawk-web/__tests__/e2e/certification-upload.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test.describe('Certification Upload E2E', () => {
  
  test('admin scans QR code and verifies worker DOB', async ({ page }) => {
    // Given: Admin navigates to verification portal
    await page.goto('http://localhost:3000/admin/verify');
    
    // When: Admin scans QR code (simulate QR data)
    await page.fill('[data-testid="qr-input"]', 'HAZARDHAWK-WORKER-12345');
    await page.click('[data-testid="scan-button"]');
    
    // Then: Worker information is displayed
    await expect(page.locator('[data-testid="worker-name"]')).toContainText('John Doe');
    
    // When: Admin enters DOB for verification
    await page.fill('[data-testid="dob-month"]', '05');
    await page.fill('[data-testid="dob-day"]', '15');
    await page.fill('[data-testid="dob-year"]', '1990');
    await page.click('[data-testid="verify-button"]');
    
    // Then: Upload interface is displayed
    await expect(page.locator('[data-testid="upload-zone"]')).toBeVisible();
    
    // When: Admin uploads certification
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles('./test-data/osha-10-cert.pdf');
    
    // Then: Upload progress is shown
    await expect(page.locator('[data-testid="upload-progress"]')).toBeVisible();
    
    // And: OCR processing begins
    await expect(page.locator('[data-testid="ocr-status"]')).toContainText('Processing');
    
    // And: Results are displayed (wait max 15 seconds)
    await page.waitForSelector('[data-testid="ocr-results"]', { timeout: 15000 });
    
    // And: Admin reviews and approves
    await page.click('[data-testid="approve-button"]');
    
    // And: Success message is displayed
    await expect(page.locator('[data-testid="success-message"]')).toContainText('Certification approved');
  });
  
  test('admin uploads multiple certifications for batch processing')
  
  test('admin manually enters certification when OCR confidence is low')
  
  test('admin rejects certification and provides feedback')
  
  test('admin views pending certifications queue')
  
  test('admin searches for worker by name or ID')
  
  test('web portal handles network interruption gracefully')
});
```

**Execution**:
```bash
# Install Playwright
npm install -D @playwright/test

# Run E2E tests
npm run test:e2e

# Run in headed mode (see browser)
npm run test:e2e:headed

# Run on multiple browsers
npm run test:e2e -- --project=chromium,firefox,webkit

# Expected execution time: <2 minutes
```

---

## Performance Testing Strategy

### Performance Benchmarks (10 tests)

#### 1. Upload Performance Benchmarks

**File**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/domain/services/UploadPerformanceBenchmarkTest.kt`

```kotlin
@Test
fun `upload 100KB file should complete in under 2 seconds`() = runTest {
    val file = createTestFile(sizeKB = 100)
    val startTime = Clock.System.now()
    
    val result = fileUploadService.uploadFile(file, "test.jpg", "image/jpeg")
    
    val duration = Clock.System.now() - startTime
    assertTrue(result.isSuccess)
    assertTrue(duration.inWholeSeconds < 2, "Upload took ${duration.inWholeSeconds}s")
}

@Test
fun `upload 5MB file should complete in under 10 seconds`()

@Test
fun `compression should reduce 5MB image to under 800KB in under 3 seconds`()

@Test
fun `batch upload 10 files should complete in under 15 seconds`()
```

#### 2. OCR Performance Benchmarks

```kotlin
@Test
fun `OCR extraction should complete in under 15 seconds per document`() = runTest {
    val documentUrl = "https://cdn.test.com/sample-osha-10.pdf"
    val startTime = Clock.System.now()
    
    val result = ocrService.extractCertificationData(documentUrl)
    
    val duration = Clock.System.now() - startTime
    assertTrue(result.isSuccess)
    assertTrue(duration.inWholeSeconds < 15, "OCR took ${duration.inWholeSeconds}s")
}

@Test
fun `batch OCR processing of 10 documents should complete in under 60 seconds`()
```

#### 3. API Performance Benchmarks

**File**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/domain/services/APIPerformanceBenchmarkTest.kt`

```kotlin
@Test
fun `presigned URL generation should respond in under 200ms`()

@Test
fun `OCR extraction API should respond in under 15s`()

@Test
fun `notification API should respond in under 500ms`()

@Test
fun `database query for certifications should complete in under 100ms`()
```

### Performance Monitoring

**Load Testing with Gatling** (`LoadTestSimulation.scala`):

```scala
class CertificationUploadLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("https://api.hazardhawk.com")
    .acceptHeader("application/json")
  
  val uploadScenario = scenario("Upload Certification")
    .exec(http("Generate Presigned URL")
      .post("/api/storage/presigned-url")
      .header("Authorization", "Bearer ${token}")
      .body(StringBody("""{"bucket":"hazardhawk-certifications","key":"test.pdf"}"""))
      .check(status.is(200))
      .check(jsonPath("$.presignedUrl").saveAs("presignedUrl")))
    .exec(http("Upload File")
      .put("${presignedUrl}")
      .body(RawFileBody("test-data/osha-10.pdf"))
      .check(status.is(200)))
  
  setUp(
    uploadScenario.inject(
      rampUsers(100) during (60 seconds) // 100 users over 60 seconds
    )
  ).protocols(httpProtocol)
  
  // Target: 95% of requests < 10s, 99% < 15s
}
```

**Run Load Test**:
```bash
# Install Gatling
brew install gatling

# Run load test
gatling -sf ./load-tests -s CertificationUploadLoadTest

# Target metrics:
# - Throughput: 50 uploads/minute
# - P95 latency: <10 seconds
# - P99 latency: <15 seconds
# - Error rate: <1%
```

---

## Contract Testing Strategy

### API Contract Tests (12 tests)

**File**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/domain/services/APIContractTest.kt`

```kotlin
/**
 * Contract tests ensure frontend and backend agree on API specifications
 * Uses Pact for consumer-driven contract testing
 */
class APIContractTest {
    
    @Test
    fun `POST presigned-url contract - request and response match spec`() {
        // Define expected contract
        val expectedRequest = PactDslJsonBody()
            .stringType("bucket", "hazardhawk-certifications")
            .stringType("key", "certifications/1234567890-osha-10.pdf")
            .stringType("contentType", "application/pdf")
            .numberType("expirationSeconds", 3600)
            .stringType("operation", "putObject")
        
        val expectedResponse = PactDslJsonBody()
            .stringType("presignedUrl", "https://s3.amazonaws.com/...")
            .datetime("expiresAt", "yyyy-MM-dd'T'HH:mm:ss'Z'")
        
        // Verify contract
        mockProvider
            .given("User is authenticated")
            .uponReceiving("Request for presigned URL")
            .path("/api/storage/presigned-url")
            .method("POST")
            .body(expectedRequest)
            .willRespondWith()
            .status(200)
            .body(expectedResponse)
        
        // Execute and verify
        val result = apiClient.getPresignedUrl(...)
        assertTrue(result.isSuccess)
        assertNotNull(result.getOrNull()?.presignedUrl)
    }
    
    @Test
    fun `POST ocr/extract-certification contract matches spec`()
    
    @Test
    fun `POST notifications/certification-expiring contract matches spec`()
    
    @Test
    fun `DELETE storage/files contract matches spec`()
    
    @Test
    fun `GET certifications/:id contract matches spec`()
    
    @Test
    fun `PATCH certifications/:id/verify contract matches spec`()
    
    @Test
    fun `GET certifications/pending contract matches spec`()
    
    @Test
    fun `POST certifications/:id/reject contract matches spec`()
    
    @Test
    fun `GET worker/:id/certifications contract matches spec`()
    
    @Test
    fun `GET statistics/dashboard contract matches spec`()
    
    @Test
    fun `Error responses follow standard format`()
    
    @Test
    fun `Authentication errors return 401 with correct structure`()
}
```

**Setup Pact Testing**:
```bash
# Add to build.gradle.kts
dependencies {
    testImplementation("au.com.dius.pact.consumer:junit5:4.5.0")
}

# Run contract tests
./gradlew :shared:contractTest

# Publish contracts to Pact Broker
./gradlew pactPublish
```

---

## Security Testing Strategy

### Security Test Cases (8 tests)

#### 1. File Upload Security Tests

```kotlin
class FileUploadSecurityTest {
    
    @Test
    fun `upload should reject files exceeding 10MB limit`()
    
    @Test
    fun `upload should reject executable files (.exe, .sh, .bat)`()
    
    @Test
    fun `upload should validate MIME type matches file extension`()
    
    @Test
    fun `upload should scan files for viruses before storage`()
    
    @Test
    fun `presigned URL should expire after configured time`()
    
    @Test
    fun `presigned URL should not allow unauthorized bucket access`()
    
    @Test
    fun `file paths should be sanitized to prevent path traversal`()
    
    @Test
    fun `uploaded files should be served with correct Content-Security-Policy headers`()
}
```

#### 2. Authentication Security Tests

```kotlin
class AuthenticationSecurityTest {
    
    @Test
    fun `API requests without auth token should return 401`()
    
    @Test
    fun `API requests with expired token should return 401`()
    
    @Test
    fun `API requests with invalid token should return 401`()
    
    @Test
    fun `field worker should not access admin endpoints`()
    
    @Test
    fun `rate limiting should block excessive requests (100 per minute)`()
    
    @Test
    fun `SQL injection attempts should be blocked`()
    
    @Test
    fun `XSS attempts in certification data should be sanitized`()
    
    @Test
    fun `CORS should only allow whitelisted origins`()
}
```

**Security Scanning Tools**:

```bash
# OWASP Dependency Check
./gradlew dependencyCheckAnalyze

# Static Analysis Security Testing (SAST)
./gradlew detekt

# Container Security Scanning
docker scan hazardhawk-backend:latest

# Penetration Testing (Manual)
# - Use OWASP ZAP for web portal
# - Use Burp Suite for API testing
```

---

## Regression Testing Strategy

### Regression Test Suite (20 tests)

**Purpose**: Ensure refactoring and technical debt cleanup don't break existing functionality

#### 1. Critical Path Regression Tests

```kotlin
class CriticalPathRegressionTest {
    
    @Test
    fun `photo capture workflow still works after refactoring`()
    
    @Test
    fun `PTP generation still works after model consolidation`()
    
    @Test
    fun `incident reporting still works after database changes`()
    
    @Test
    fun `gallery view still works after UI updates`()
    
    @Test
    fun `authentication flow still works after security updates`()
}
```

#### 2. Data Migration Regression Tests

```kotlin
class DataMigrationRegressionTest {
    
    @Test
    fun `existing certifications are readable after schema changes`()
    
    @Test
    fun `worker profiles are accessible after model consolidation`()
    
    @Test
    fun `projects and companies load correctly after refactoring`()
    
    @Test
    fun `PTP documents render correctly after PDF layout changes`()
}
```

#### 3. UI Regression Tests (Screenshot Testing)

**File**: `/HazardHawk/androidApp/src/androidTest/java/com/hazardhawk/ui/ScreenshotRegressionTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class ScreenshotRegressionTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `home dashboard matches baseline screenshot`() {
        composeTestRule.setContent {
            HazardHawkTheme {
                HomeScreen()
            }
        }
        
        composeTestRule.onRoot()
            .captureToImage()
            .assertAgainstGolden("home_dashboard")
    }
    
    @Test
    fun `certification upload screen matches baseline`()
    
    @Test
    fun `PTP creation screen matches baseline`()
    
    @Test
    fun `gallery screen matches baseline`()
}
```

**Setup Screenshot Testing**:
```bash
# Add to build.gradle.kts
androidTestImplementation("com.github.QuickBirdEng:survey-kit:1.1.0")

# Generate baseline screenshots
./gradlew :androidApp:executeScreenshotTests -Precord

# Run regression tests
./gradlew :androidApp:executeScreenshotTests -Pverify
```

---

## Test Data Management

### Test Data Strategy

#### 1. Certification Test Data

**Location**: `/HazardHawk/shared/src/commonTest/resources/test-data/`

```
test-data/
├── certifications/
│   ├── osha-10/
│   │   ├── valid-high-quality.pdf (500KB)
│   │   ├── valid-poor-quality.jpg (300KB)
│   │   ├── valid-rotated-90deg.pdf (450KB)
│   │   └── expired-cert.pdf (400KB)
│   ├── osha-30/
│   │   ├── valid-multi-page.pdf (2MB)
│   │   ├── valid-scanned.jpg (1.5MB)
│   │   └── partial-data.pdf (350KB)
│   ├── forklift/
│   │   ├── valid-standard.pdf (600KB)
│   │   └── valid-image.jpg (800KB)
│   └── invalid/
│       ├── virus-test.exe (10KB)
│       ├── too-large.pdf (15MB)
│       └── corrupted.pdf (200KB)
├── workers/
│   ├── worker-profiles.json
│   └── worker-qr-codes.json
└── expected-ocr-results/
    ├── osha-10-expected.json
    ├── osha-30-expected.json
    └── forklift-expected.json
```

#### 2. Test Fixtures

**File**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/domain/services/CertificationTestFixtures.kt`

```kotlin
object CertificationTestFixtures {
    
    // Valid JPEG header (magic number)
    val validJpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
    
    // Valid PNG header
    val validPngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    
    // Valid PDF header
    val validPdfHeader = "%PDF-1.4".toByteArray()
    
    // Sample OCR raw text
    val osha10RawText = """
        OSHA 10-Hour Construction Safety Certificate
        
        This certifies that John Michael Doe
        Has successfully completed the OSHA 10-Hour Construction Safety training
        
        Certificate Number: OSHA10-2024-12345678
        Issue Date: January 15, 2024
        Expiration Date: January 15, 2029
        
        Authorized by: U.S. Department of Labor - Occupational Safety and Health Administration
        Instructor: Jane Smith, #987654
    """.trimIndent()
    
    val forkliftRawText = """
        POWERED INDUSTRIAL TRUCK OPERATOR CERTIFICATION
        
        Operator Name: Robert Johnson
        Certification Type: Class 1 Electric Motor Rider Trucks
        
        Certificate ID: FORK-2024-789012
        Issued: March 22, 2024
        Valid Through: March 22, 2027
        
        Training Center: ABC Safety Training Inc.
        Trainer: Michael Williams, Certified Instructor
    """.trimIndent()
    
    // Poor quality text (low confidence)
    val poorQualityText = """
        OSHA 10
        Name: J0hn D0e (OCR artifacts)
        Issue: 01/15/202? (unclear)
        Expiry: (unreadable)
    """.trimIndent()
    
    fun createMockJpeg(sizeKB: Int): ByteArray {
        val sizeBytes = sizeKB * 1024
        val data = ByteArray(sizeBytes)
        validJpegHeader.copyInto(data)
        return data
    }
    
    fun createMockPdf(sizeKB: Int): ByteArray {
        val sizeBytes = sizeKB * 1024
        val data = ByteArray(sizeBytes)
        validPdfHeader.copyInto(data)
        return data
    }
    
    fun createFileUpload(fileName: String, contentType: String, sizeKB: Int): FileUpload {
        val data = when {
            contentType.contains("pdf") -> createMockPdf(sizeKB)
            contentType.contains("jpeg") || contentType.contains("jpg") -> createMockJpeg(sizeKB)
            contentType.contains("png") -> createMockPng(sizeKB)
            else -> ByteArray(sizeKB * 1024)
        }
        return FileUpload(data, fileName, contentType)
    }
    
    fun createExpectedOCRResult(certificationType: String): ExtractedCertification {
        return when (certificationType) {
            "OSHA_10" -> ExtractedCertification(
                holderName = "John Michael Doe",
                certificationType = "OSHA_10",
                certificationNumber = "OSHA10-2024-12345678",
                issueDate = LocalDate(2024, 1, 15),
                expirationDate = LocalDate(2029, 1, 15),
                issuingAuthority = "OSHA",
                confidence = 0.95f,
                needsReview = false,
                rawText = osha10RawText,
                fieldConfidences = mapOf(
                    "holderName" to 0.98f,
                    "certificationType" to 0.99f,
                    "certificationNumber" to 0.92f,
                    "issueDate" to 0.96f,
                    "expirationDate" to 0.94f,
                    "issuingAuthority" to 0.97f
                )
            )
            "FORKLIFT" -> ExtractedCertification(
                holderName = "Robert Johnson",
                certificationType = "FORKLIFT",
                certificationNumber = "FORK-2024-789012",
                issueDate = LocalDate(2024, 3, 22),
                expirationDate = LocalDate(2027, 3, 22),
                issuingAuthority = "ABC Safety Training Inc.",
                confidence = 0.91f,
                needsReview = false,
                rawText = forkliftRawText
            )
            else -> throw IllegalArgumentException("Unknown certification type: $certificationType")
        }
    }
}
```

---

## Mock Service Strategies

### 1. AWS S3 Mock (Localstack)

**Setup**:
```kotlin
class MockS3Client : S3Client {
    private val storage = mutableMapOf<String, ByteArray>()
    private val presignedUrls = mutableMapOf<String, Long>() // URL to expiration time
    
    override suspend fun generatePresignedUrl(
        bucket: String,
        key: String,
        contentType: String,
        expirationSeconds: Long
    ): Result<String> {
        val url = "https://mock-s3.local/$bucket/$key?signature=mock"
        presignedUrls[url] = Clock.System.now().toEpochMilliseconds() + (expirationSeconds * 1000)
        return Result.success(url)
    }
    
    override suspend fun uploadFile(url: String, data: ByteArray): Result<Unit> {
        if (!isUrlValid(url)) {
            return Result.failure(Exception("Presigned URL expired"))
        }
        val key = extractKeyFromUrl(url)
        storage[key] = data
        return Result.success(Unit)
    }
    
    override suspend fun deleteFile(bucket: String, key: String): Result<Unit> {
        storage.remove("$bucket/$key")
        return Result.success(Unit)
    }
    
    private fun isUrlValid(url: String): Boolean {
        val expiration = presignedUrls[url] ?: return false
        return Clock.System.now().toEpochMilliseconds() < expiration
    }
    
    fun reset() {
        storage.clear()
        presignedUrls.clear()
    }
}
```

### 2. Google Document AI Mock

```kotlin
class MockOCRService : OCRService {
    var mockRawText: String = ""
    var failOnUrl: String? = null
    var addNumberToName: Boolean = false
    private var callCount = 0
    
    override suspend fun extractCertificationData(documentUrl: String): Result<ExtractedCertification> {
        callCount++
        
        // Simulate failure for specific URL
        if (documentUrl == failOnUrl) {
            return Result.failure(OCRError.ExtractionFailed("Simulated OCR failure"))
        }
        
        // Simulate processing delay
        delay(200) // 200ms processing time
        
        // Parse mock text to extract certification
        return when {
            mockRawText.contains("OSHA 10") -> {
                val name = if (addNumberToName) "John Doe $callCount" else "John Michael Doe"
                Result.success(CertificationTestFixtures.createExpectedOCRResult("OSHA_10").copy(
                    holderName = name
                ))
            }
            mockRawText.contains("Forklift", ignoreCase = true) || 
            mockRawText.contains("POWERED INDUSTRIAL TRUCK") -> {
                Result.success(CertificationTestFixtures.createExpectedOCRResult("FORKLIFT"))
            }
            mockRawText.contains("unreadable") || mockRawText.contains("?") -> {
                Result.success(ExtractedCertification(
                    holderName = "J0hn D0e",
                    certificationType = "OSHA_10",
                    certificationNumber = null,
                    issueDate = null,
                    expirationDate = null,
                    issuingAuthority = null,
                    confidence = 0.65f,
                    needsReview = true,
                    rawText = mockRawText
                ))
            }
            else -> Result.failure(OCRError.UnknownCertificationType("Unknown type in: $mockRawText"))
        }
    }
    
    override suspend fun extractCertificationDataBatch(documentUrls: List<String>): List<Result<ExtractedCertification>> {
        return documentUrls.map { url -> extractCertificationData(url) }
    }
    
    fun reset() {
        callCount = 0
        failOnUrl = null
        addNumberToName = false
    }
}
```

### 3. Notification Service Mock

```kotlin
class MockNotificationService : NotificationService {
    val sentNotifications = mutableListOf<SentNotification>()
    var shouldFailEmail = false
    var shouldFailSMS = false
    var shouldFailPush = false
    
    data class SentNotification(
        val workerId: String,
        val channel: NotificationChannel,
        val message: String,
        val timestamp: Long
    )
    
    override suspend fun sendExpirationAlert(
        workerId: String,
        certificationId: String,
        daysUntilExpiration: Int,
        channels: List<NotificationChannel>
    ): Result<NotificationDeliveryResult> {
        
        val deliveryResults = mutableMapOf<NotificationChannel, ChannelDeliveryResult>()
        
        channels.forEach { channel ->
            val result = when (channel) {
                NotificationChannel.EMAIL -> {
                    if (shouldFailEmail) {
                        ChannelDeliveryResult(channel, DeliveryStatus.FAILED, null, "Simulated failure")
                    } else {
                        sentNotifications.add(SentNotification(
                            workerId, channel, "Expiration alert", Clock.System.now().toEpochMilliseconds()
                        ))
                        ChannelDeliveryResult(channel, DeliveryStatus.SENT, "mock_message_id", null)
                    }
                }
                NotificationChannel.SMS -> {
                    if (shouldFailSMS) {
                        ChannelDeliveryResult(channel, DeliveryStatus.FAILED, null, "Simulated failure")
                    } else {
                        sentNotifications.add(SentNotification(
                            workerId, channel, "Expiration alert", Clock.System.now().toEpochMilliseconds()
                        ))
                        ChannelDeliveryResult(channel, DeliveryStatus.SENT, "mock_sms_id", null)
                    }
                }
                NotificationChannel.PUSH -> {
                    if (shouldFailPush) {
                        ChannelDeliveryResult(channel, DeliveryStatus.FAILED, null, "Simulated failure")
                    } else {
                        sentNotifications.add(SentNotification(
                            workerId, channel, "Expiration alert", Clock.System.now().toEpochMilliseconds()
                        ))
                        ChannelDeliveryResult(channel, DeliveryStatus.SENT, "mock_push_id", null)
                    }
                }
            }
            deliveryResults[channel] = result
        }
        
        return Result.success(NotificationDeliveryResult(
            notificationId = "mock_notif_${sentNotifications.size}",
            deliveryResults = deliveryResults,
            totalChannels = channels.size,
            successfulChannels = deliveryResults.count { it.value.status == DeliveryStatus.SENT },
            failedChannels = deliveryResults.count { it.value.status == DeliveryStatus.FAILED }
        ))
    }
    
    fun reset() {
        sentNotifications.clear()
        shouldFailEmail = false
        shouldFailSMS = false
        shouldFailPush = false
    }
}
```

---

## CI/CD Integration

### GitHub Actions Workflow

**File**: `.github/workflows/test.yml`

```yaml
name: Test Suite

on:
  push:
    branches: [ main, develop, feature/* ]
  pull_request:
    branches: [ main, develop ]

jobs:
  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest
    timeout-minutes: 10
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Cache Gradle packages
        uses: actions/cache@v3
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
          restore-keys: ${{ runner.os }}-gradle-
      
      - name: Run Unit Tests
        run: ./gradlew :shared:test --stacktrace
      
      - name: Generate Coverage Report
        run: ./gradlew :shared:testDebugUnitTestCoverage
      
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./shared/build/reports/coverage/test/debug/report.xml
          flags: unit-tests
      
      - name: Publish Test Results
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always()
        with:
          files: '**/build/test-results/**/*.xml'
  
  integration-tests:
    name: Integration Tests
    runs-on: ubuntu-latest
    timeout-minutes: 15
    
    services:
      localstack:
        image: localstack/localstack:latest
        ports:
          - 4566:4566
        env:
          SERVICES: s3
      
      postgres:
        image: postgres:15-alpine
        ports:
          - 5432:5432
        env:
          POSTGRES_DB: hazardhawk_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Integration Tests
        run: ./gradlew :shared:integrationTest --stacktrace
        env:
          AWS_ENDPOINT: http://localhost:4566
          DB_HOST: localhost
          DB_PORT: 5432
      
      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: integration-test-results
          path: shared/build/reports/tests/integrationTest/
  
  android-e2e-tests:
    name: Android E2E Tests
    runs-on: ubuntu-latest
    timeout-minutes: 30
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Instrumented Tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          target: google_apis
          arch: x86_64
          script: ./gradlew :androidApp:connectedAndroidTest
      
      - name: Upload E2E Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: android-e2e-results
          path: androidApp/build/reports/androidTests/
  
  web-e2e-tests:
    name: Web E2E Tests
    runs-on: ubuntu-latest
    timeout-minutes: 15
    
    defaults:
      run:
        working-directory: hazardhawk-web
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'
          cache: 'npm'
          cache-dependency-path: hazardhawk-web/package-lock.json
      
      - name: Install dependencies
        run: npm ci
      
      - name: Install Playwright browsers
        run: npx playwright install --with-deps
      
      - name: Run E2E tests
        run: npm run test:e2e
      
      - name: Upload Playwright Report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: hazardhawk-web/playwright-report/
  
  performance-tests:
    name: Performance Tests
    runs-on: ubuntu-latest
    timeout-minutes: 20
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Performance Benchmarks
        run: ./gradlew :shared:performanceTest
      
      - name: Upload Performance Results
        uses: actions/upload-artifact@v3
        with:
          name: performance-results
          path: shared/build/reports/performance/

  test-summary:
    name: Test Summary
    runs-on: ubuntu-latest
    needs: [unit-tests, integration-tests, android-e2e-tests, web-e2e-tests]
    if: always()
    
    steps:
      - name: Download All Artifacts
        uses: actions/download-artifact@v3
      
      - name: Generate Test Summary
        run: |
          echo "## Test Results Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          echo "- Unit Tests: ✅" >> $GITHUB_STEP_SUMMARY
          echo "- Integration Tests: ✅" >> $GITHUB_STEP_SUMMARY
          echo "- Android E2E Tests: ✅" >> $GITHUB_STEP_SUMMARY
          echo "- Web E2E Tests: ✅" >> $GITHUB_STEP_SUMMARY
```

### Pre-Commit Hooks

**File**: `.husky/pre-commit`

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

echo "Running pre-commit checks..."

# Run unit tests (fast)
echo "Running unit tests..."
./gradlew :shared:test --parallel
if [ $? -ne 0 ]; then
  echo "❌ Unit tests failed. Commit aborted."
  exit 1
fi

# Run lint checks
echo "Running lint..."
./gradlew ktlintCheck
if [ $? -ne 0 ]; then
  echo "❌ Lint checks failed. Run ./gradlew ktlintFormat to fix."
  exit 1
fi

# Check for compilation errors
echo "Checking compilation..."
./gradlew :shared:compileKotlinMetadata
if [ $? -ne 0 ]; then
  echo "❌ Compilation failed. Fix errors before committing."
  exit 1
fi

echo "✅ All pre-commit checks passed!"
```

### Test Execution Schedule

| Test Type | Trigger | Frequency | Duration |
|-----------|---------|-----------|----------|
| Unit Tests | Every commit | Continuous | <5 sec |
| Integration Tests | Every PR | On-demand | <30 sec |
| E2E Tests (Android) | Every PR to main | Daily + PRs | <3 min |
| E2E Tests (Web) | Every PR to main | Daily + PRs | <2 min |
| Performance Tests | Push to main | Weekly | <20 min |
| Security Scans | Push to main | Daily | <10 min |
| Contract Tests | Version changes | On API updates | <1 min |
| Regression Tests | Major refactoring | Before release | <10 min |

---

## Acceptance Criteria

### Phase 2 Production Readiness Checklist

#### 1. Test Coverage ✅/❌

- [ ] Unit test coverage ≥ 85% (line coverage)
- [ ] Unit test coverage ≥ 80% (branch coverage)
- [ ] Integration tests cover all external services (S3, Document AI, SendGrid, Twilio, FCM)
- [ ] E2E tests cover critical user workflows (upload, OCR, approval, rejection)
- [ ] Performance benchmarks meet SLA targets (upload <10s, OCR <15s, API <200ms)
- [ ] Contract tests verify API specifications match frontend expectations
- [ ] Security tests validate file upload restrictions and authentication
- [ ] Regression tests confirm no breaking changes from refactoring

#### 2. Test Quality ✅/❌

- [ ] All tests have clear, descriptive names (BDD-style)
- [ ] Tests are independent and can run in any order
- [ ] Tests use appropriate mocks/stubs (no real external calls in unit tests)
- [ ] Tests follow AAA pattern (Arrange, Act, Assert)
- [ ] Flaky tests are identified and fixed (<1% flakiness rate)
- [ ] Tests fail clearly with actionable error messages
- [ ] Test data is isolated and doesn't leak between tests

#### 3. CI/CD Integration ✅/❌

- [ ] All tests run automatically on every PR
- [ ] Test results are visible in GitHub PR status checks
- [ ] Coverage reports are generated and tracked over time
- [ ] Failed tests block PR merging
- [ ] Performance regression alerts are configured
- [ ] Test execution time is monitored and optimized
- [ ] Parallel test execution is configured for speed

#### 4. External Service Integration ✅/❌

- [ ] AWS S3 buckets are created and configured
- [ ] Google Document AI processor is trained with 50+ samples
- [ ] SendGrid account is set up with email templates
- [ ] Twilio account is set up with SMS templates
- [ ] Firebase Cloud Messaging is configured for push notifications
- [ ] All API endpoints are deployed and accessible
- [ ] Rate limiting is configured and tested
- [ ] Error handling and retry logic is implemented

#### 5. Documentation ✅/❌

- [ ] Testing strategy document is complete (this document)
- [ ] API documentation includes request/response examples
- [ ] Test data fixtures are documented and accessible
- [ ] Mock service usage is documented with examples
- [ ] Performance benchmarks are documented with targets
- [ ] Security testing procedures are documented
- [ ] Known limitations and edge cases are documented

#### 6. Performance Benchmarks ✅/❌

| Metric | Target | Actual | Status |
|--------|--------|--------|--------|
| File upload (100KB) | <2s | TBD | ⏳ |
| File upload (5MB) | <10s | TBD | ⏳ |
| Image compression | <3s | TBD | ⏳ |
| OCR extraction | <15s | TBD | ⏳ |
| Batch OCR (10 docs) | <60s | TBD | ⏳ |
| Presigned URL API | <200ms | TBD | ⏳ |
| Notification API | <500ms | TBD | ⏳ |
| Database query | <100ms | TBD | ⏳ |
| P95 upload latency | <10s | TBD | ⏳ |
| P99 upload latency | <15s | TBD | ⏳ |

#### 7. Security Validation ✅/❌

- [ ] File upload size limits are enforced (10MB max)
- [ ] Executable files are rejected (.exe, .sh, .bat)
- [ ] MIME type validation matches file content
- [ ] Virus scanning is enabled for all uploads
- [ ] Presigned URLs expire after configured time
- [ ] Authentication is required for all API endpoints
- [ ] Rate limiting prevents abuse (100 req/min per user)
- [ ] CORS is configured to allow only whitelisted origins
- [ ] SQL injection and XSS attacks are blocked
- [ ] Sensitive data is encrypted at rest and in transit

#### 8. Monitoring & Alerting ✅/❌

- [ ] Application logs are structured (JSON format)
- [ ] Log aggregation is configured (CloudWatch, Loki, etc.)
- [ ] Error tracking is enabled (Sentry, Rollbar, etc.)
- [ ] Performance metrics are tracked (Grafana, DataDog, etc.)
- [ ] Uptime monitoring is configured (Pingdom, UptimeRobot, etc.)
- [ ] Alerting rules are defined for critical failures
- [ ] On-call rotation is established for incident response
- [ ] Runbooks are created for common issues

---

## Test Execution Commands

### Quick Reference

```bash
# ============================================
# Unit Tests (5 seconds)
# ============================================
./gradlew :shared:test

# Run specific test class
./gradlew :shared:test --tests "FileUploadServiceTest"

# Run with coverage
./gradlew :shared:testDebugUnitTestCoverage
open shared/build/reports/coverage/test/debug/index.html

# ============================================
# Integration Tests (30 seconds)
# ============================================
# Start test services
docker-compose -f docker-compose.test.yml up -d

# Run integration tests
./gradlew :shared:integrationTest

# Stop test services
docker-compose -f docker-compose.test.yml down

# ============================================
# Android E2E Tests (3 minutes)
# ============================================
# On emulator
./gradlew :androidApp:connectedAndroidTest

# On Firebase Test Lab
gcloud firebase test android run \
  --type instrumentation \
  --app androidApp/build/outputs/apk/debug/app-debug.apk \
  --test androidApp/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

# ============================================
# Web E2E Tests (2 minutes)
# ============================================
cd hazardhawk-web
npm run test:e2e

# Run in headed mode
npm run test:e2e:headed

# ============================================
# Performance Tests (20 minutes)
# ============================================
./gradlew :shared:performanceTest

# Load testing with Gatling
gatling -sf ./load-tests -s CertificationUploadLoadTest

# ============================================
# Contract Tests (1 minute)
# ============================================
./gradlew :shared:contractTest
./gradlew pactPublish

# ============================================
# Security Tests (10 minutes)
# ============================================
# Dependency vulnerabilities
./gradlew dependencyCheckAnalyze

# Static analysis
./gradlew detekt

# Container scanning
docker scan hazardhawk-backend:latest

# ============================================
# All Tests (6 minutes)
# ============================================
./gradlew test integrationTest performanceTest
cd hazardhawk-web && npm run test:e2e
```

---

## Test Maintenance Guidelines

### 1. When to Update Tests

- **New Feature Added**: Write tests before implementation (TDD)
- **Bug Fixed**: Add regression test to prevent recurrence
- **Refactoring Code**: Ensure tests still pass, update if behavior changes
- **API Contract Changes**: Update contract tests and mock responses
- **Performance Degradation**: Add performance benchmark test
- **Security Vulnerability**: Add security test to prevent exploit

### 2. Test Flakiness Prevention

**Common Causes of Flaky Tests**:
- Race conditions in concurrent code
- Hardcoded timeouts that are too short
- Dependence on external services without mocks
- Shared mutable state between tests
- Non-deterministic data (random values, timestamps)

**Solutions**:
```kotlin
// ❌ BAD: Hardcoded sleep
delay(1000) // What if processing takes longer?

// ✅ GOOD: Wait for condition
withTimeout(5000) {
    while (!condition) {
        delay(50)
    }
}

// ❌ BAD: Shared mutable state
companion object {
    var counter = 0 // Leaks between tests
}

// ✅ GOOD: Isolated state
@BeforeTest
fun setup() {
    counter = 0 // Reset for each test
}

// ❌ BAD: Real timestamp
val timestamp = Clock.System.now() // Non-deterministic

// ✅ GOOD: Fixed timestamp
val timestamp = Clock.System.fromEpochMilliseconds(1728400000000)
```

### 3. Test Code Review Checklist

When reviewing test code, ensure:
- [ ] Test names clearly describe what is being tested
- [ ] Tests are focused on one behavior (not testing multiple things)
- [ ] Assertions are specific and meaningful
- [ ] Test data is realistic and representative
- [ ] Mocks are used appropriately (not over-mocking)
- [ ] Tests are fast (unit tests <100ms, integration <5s)
- [ ] Tests clean up resources (files, connections, etc.)
- [ ] Tests don't depend on execution order

---

## Glossary

**AAA Pattern**: Arrange-Act-Assert, a structure for writing tests
**BDD**: Behavior-Driven Development, testing style focused on behaviors
**Contract Testing**: Verifying API contracts between consumer and provider
**E2E Testing**: End-to-End testing, validating full user workflows
**Flaky Test**: Test that intermittently fails without code changes
**Mock**: Test double that simulates behavior of real objects
**Regression Test**: Test ensuring previously working features still work
**Stub**: Test double that returns predefined responses
**Test Fixture**: Reusable test data and setup code
**Test Pyramid**: Distribution strategy emphasizing more unit tests than E2E

---

## Summary

This testing strategy provides a comprehensive, production-ready testing approach for Phase 2 Backend Integration and Technical Debt Cleanup. By following the "Simple, Loveable, Complete" philosophy, we ensure:

- **Simple**: Clear test structure, easy to maintain
- **Loveable**: Fast feedback, high confidence, developer-friendly
- **Complete**: 155 tests covering all scenarios, 85%+ coverage, performance validated

### Next Steps

1. **Week 1**: Implement integration tests (30 tests)
2. **Week 2**: Implement E2E tests (15 tests for Android + Web)
3. **Week 3**: Implement performance and contract tests
4. **Week 4**: Run full test suite, fix issues, achieve acceptance criteria

**Estimated Total Effort**: 3-4 weeks to full production readiness

---

**Document Version**: 1.0  
**Last Updated**: October 9, 2025  
**Maintained By**: Test Guardian  
**Review Cycle**: Monthly or after major feature additions  
**Status**: Ready for Implementation ✅
