# Phase 2 Tests - Quick Reference Guide

## Running Tests

### Run All Phase 2 Tests
```bash
./gradlew :shared:test --tests "com.hazardhawk.domain.services.*"
```

### Run Individual Test Files

```bash
# FileUploadService (30 tests)
./gradlew :shared:test --tests "FileUploadServiceTest"

# OCRService (40 tests)
./gradlew :shared:test --tests "OCRServiceTest"

# NotificationService (15 tests)
./gradlew :shared:test --tests "NotificationServiceTest"

# Certification Upload Integration (15 tests)
./gradlew :shared:test --tests "CertificationUploadIntegrationTest"

# Expiration Alert Integration (10 tests)
./gradlew :shared:test --tests "ExpirationAlertIntegrationTest"
```

### Run Specific Test
```bash
./gradlew :shared:test --tests "FileUploadServiceTest.uploadFile should successfully upload PDF"
```

### Run with Coverage
```bash
./gradlew :shared:testDebugUnitTestCoverage

# View report:
open shared/build/reports/coverage/test/debug/index.html
```

## Test Files Overview

| File | Location | Tests | Lines |
|------|----------|-------|-------|
| Test Fixtures | `shared/src/commonTest/kotlin/com/hazardhawk/domain/fixtures/CertificationTestFixtures.kt` | - | 200+ |
| FileUploadServiceTest | `shared/src/commonTest/kotlin/com/hazardhawk/domain/services/FileUploadServiceTest.kt` | 30 | 658 |
| OCRServiceTest | `shared/src/commonTest/kotlin/com/hazardhawk/domain/services/OCRServiceTest.kt` | 40 | 489 |
| NotificationServiceTest | `shared/src/commonTest/kotlin/com/hazardhawk/domain/services/NotificationServiceTest.kt` | 15 | 382 |
| CertificationUploadIntegrationTest | `shared/src/commonTest/kotlin/com/hazardhawk/domain/services/CertificationUploadIntegrationTest.kt` | 15 | 385 |
| ExpirationAlertIntegrationTest | `shared/src/commonTest/kotlin/com/hazardhawk/domain/services/ExpirationAlertIntegrationTest.kt` | 10 | 318 |
| **TOTAL** | | **110** | **2,432** |

## Test Categories

### Unit Tests (75 tests)
- Upload success scenarios (10)
- Retry logic (5)
- Image compression (5)
- Progress tracking (5)
- Error handling (5)
- Certification type mapping (15)
- Date parsing (10)
- Confidence calculation (5)
- Field extraction (5)
- Batch processing (5)
- Template generation (5)
- Multi-channel delivery (5)
- Notification retry logic (5)

### Integration Tests (25 tests)
- End-to-end upload workflow (5)
- Error recovery scenarios (5)
- Concurrent uploads (3)
- Large file handling (2)
- Alert delivery at each threshold (7)
- Multi-worker scenarios (3)

### Document Validation (5 bonus tests)
- Accept PDF, PNG, JPG, JPEG
- Reject unsupported formats

## Expected Results

### Performance
- Unit tests: <2 seconds each
- Integration tests: <5 seconds each
- Full suite: <5 minutes total

### Coverage
- Unit test line coverage: 90%+
- Integration test path coverage: 80%+
- Critical path coverage: 100%

## Common Issues & Solutions

### Issue: Tests fail to compile
**Solution**: Ensure all dependencies are in `shared/build.gradle.kts`:
```kotlin
val commonTest by getting {
    dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.kotlinx.serialization.json)
    }
}
```

### Issue: MockK not found
**Solution**: MockK is only available in Android unit tests. Common tests use manual mocks (MockS3Client, etc.)

### Issue: Date tests fail
**Solution**: Ensure kotlinx-datetime is in dependencies:
```kotlin
implementation(libs.kotlinx.datetime)
```

### Issue: HTTP client tests fail
**Solution**: Add ktor-client-mock to test dependencies:
```kotlin
implementation("io.ktor:ktor-client-mock:2.3.7")
```

## Test Data

### Sample Certifications
```kotlin
// OSHA-10
val cert = CertificationTestFixtures.createWorkerCertification(
    certificationType = CertificationTestFixtures.createCertificationType(
        code = CertificationTypeCodes.OSHA_10
    )
)

// Expired certification
val expiredCert = CertificationTestFixtures.createWorkerCertification(
    expirationDate = LocalDate(2024, 1, 1),
    status = CertificationStatus.EXPIRED
)
```

### Sample Files
```kotlin
// PDF (100KB)
val pdfData = CertificationTestFixtures.createSamplePdfData(100)

// JPEG (200KB)
val imageData = CertificationTestFixtures.createSampleImageData(200)
```

### OCR Responses
```kotlin
// High confidence extraction
val extracted = CertificationTestFixtures.createExtractedCertification(
    holderName = "John Doe",
    certificationType = CertificationTypeCodes.OSHA_10,
    confidence = 0.95f
)

// Low confidence (needs review)
val lowConfidence = CertificationTestFixtures.createExtractedCertification(
    holderName = "J███ D██",
    confidence = 0.65f,
    needsReview = true
)
```

## Next Steps

1. Run full test suite: `./gradlew :shared:test`
2. Generate coverage report: `./gradlew :shared:testDebugUnitTestCoverage`
3. Review coverage at target 90%+
4. Integrate into CI/CD pipeline
5. Add platform-specific tests (Android/iOS) in Phase 3

---

**Quick Links**:
- [Full Test Summary](/docs/testing/phase2-certification-management-test-summary.md)
- [Implementation Plan](/docs/plan/20251008-150900-crew-management-next-steps.md)
