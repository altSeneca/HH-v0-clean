# Phase 2 Test Execution Strategy (Post-Build Fix)

**Version**: 1.0  
**Date**: October 9, 2025  
**Owner**: test-guardian agent  
**Status**: READY FOR EXECUTION

---

## Executive Summary

This document provides a comprehensive, phased test execution strategy to validate all Phase 2 Week 2-3 code after build fixes are applied. The strategy addresses the critical gap: **138 tests claimed, zero tests executed** due to 786 compilation errors.

### Key Metrics
- **Total Test Files**: 124 files
- **Total Test Methods**: ~892 (317 in HazardHawk/shared + 575 in root shared)
- **Claimed Tests**: 138 (92 Week 2 + 46 Week 3)
- **Current Pass Rate**: 0% (build failure prevents execution)
- **Target Pass Rate**: 95%+ after fixes
- **Estimated Execution Time**: 6-8 hours total

---

## 1. Test Infrastructure Analysis

### 1.1 Test Framework Configuration

**Framework**: Kotlin Test + JUnit Platform  
**Coroutine Testing**: kotlinx-coroutines-test with `runTest`  
**Mocking**: Custom mock infrastructure (not MockK due to KMP compatibility)

**Location**: `/HazardHawk/shared/build.gradle.kts` (lines 122-129, 172-237)

```kotlin
commonTest {
    dependencies {
        implementation(libs.kotlin.test)
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.kotlinx.serialization.json)
    }
}
```

### 1.2 Test Organization Structure

```
HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/
├── test/                    # Framework smoke tests (1 file, 4 tests)
├── models/                  # Model tests (2 files)
├── ar/                      # AR and OSHA tests (2 files)
├── domain/
│   ├── models/             # Domain model tests (1 file)
│   └── services/           # Service tests (7 files, ~100+ tests)
├── data/
│   ├── mocks/              # Mock infrastructure (5 files)
│   │   ├── MockApiClient.kt
│   │   ├── MockS3Client.kt
│   │   ├── MockOCRClient.kt
│   │   ├── MockNotificationClient.kt
│   │   └── MockApiResponses.kt
│   └── repositories/       # Repository tests (7 files, ~200+ tests)
├── fixtures/               # Test data fixtures (1 file)
└── integration/            # Integration tests (3 files, ~50+ tests)

shared/src/commonTest/ (root module, legacy tests - 575 test methods)
```

### 1.3 Mock Infrastructure

**Custom Mock System** (not MockK):
- **MockApiClient**: HTTP client mocking with call history, configurable failures
- **MockS3Client**: S3 upload/download simulation
- **MockOCRClient**: Document AI extraction with confidence levels
- **MockNotificationClient**: Multi-channel notification mocking
- **MockApiResponses**: Sample JSON responses

**Configuration Options**:
```kotlin
MockApiClient.MockApiConfig(
    shouldReturnErrors = false,
    failureRate = 0.0,           // 0.0 to 1.0
    simulateTimeout = false,
    responseDelay = 0L           // milliseconds
)
```

### 1.4 Gradle Test Tasks

**Available Tasks**:
```bash
./gradlew :shared:allTests              # All platforms (iOS fails without macOS)
./gradlew :shared:test                  # Not available in KMP
./gradlew :shared:testDebugUnitTest     # Android unit tests
./gradlew :shared:testReleaseUnitTest   # Android release tests
./gradlew :shared:integrationTest       # Integration tests (custom task)
./gradlew :shared:e2eTest               # E2E tests (custom task)
./gradlew check                         # All verification tasks
```

**Configuration** (build.gradle.kts lines 172-237):
- JUnit Platform with detailed logging
- Parallel execution: `maxParallelForks = CPU cores / 2`
- Heap size: 2GB for tests
- Test reports: HTML + JUnit XML
- JaCoCo coverage with 80% minimum threshold

---

## 2. Phased Test Execution Plan

### Phase 1: Smoke Tests (5-10 minutes)

**Goal**: Verify test framework works and basic compilation succeeds

**Tasks**:
1. Run framework smoke test
2. Run 1 simple repository test
3. Run 1 simple service test

**Commands**:
```bash
cd /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk

# Step 1: Framework test (should always pass)
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.test.BasicFrameworkTest" \
  --no-daemon

# Step 2: Simple service test
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.DOBVerificationServiceTest.testBasicAssertion*" \
  --no-daemon

# Step 3: Simple repository test
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.data.repositories.CertificationApiRepositoryTest.testBasicAssertion*" \
  --no-daemon
```

**Success Criteria**:
- All 3 test commands execute without compilation errors
- At least 1 test passes in each category
- Test framework initializes mock infrastructure correctly

**Failure Protocol**:
- If compilation errors persist → HALT, review build fixes
- If mocks fail to initialize → Fix mock infrastructure first
- If basic assertions fail → Investigation needed

---

### Phase 2: Unit Tests - Services (30-45 minutes)

**Goal**: Validate all service layer tests (Week 2 focus)

**Test Categories**:
1. **DOBVerificationService** (~20 tests)
2. **QRCodeService** (~15 tests)
3. **FileUploadService** (~30 tests)
4. **OCRService** (~40 tests)
5. **NotificationService** (~15 tests)
6. **ExpirationAlertIntegration** (~10 tests)
7. **CertificationUploadIntegration** (~10 tests)

**Commands**:
```bash
# All service tests
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.*" \
  --continue \
  --no-daemon

# Individual service tests (if failures occur)
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.DOBVerificationServiceTest" \
  --no-daemon

./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.QRCodeServiceTest" \
  --no-daemon

./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.FileUploadServiceTest" \
  --no-daemon

./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.OCRServiceTest" \
  --no-daemon

./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.NotificationServiceTest" \
  --no-daemon
```

**Success Criteria**:
- 90%+ pass rate (allow up to 10% failures due to implementation gaps)
- No compilation errors
- Test execution completes in < 45 minutes
- Mock API calls verified correctly

**Failure Triage**:
- **Compilation Errors**: P0 - Fix immediately
- **Assertion Failures**: P1 - Document for implementation team
- **Timeout Errors**: P2 - Investigate mock delays
- **Flaky Tests**: P3 - Mark and investigate separately

---

### Phase 3: Unit Tests - Repositories (45-60 minutes)

**Goal**: Validate all repository layer tests (Week 2-3 focus)

**Test Categories**:
1. **CertificationApiRepository** (~100 tests) - Comprehensive CRUD + verification
2. **CrewApiRepository** (~50 tests)
3. **DashboardApiRepository** (~50 tests)
4. **CrewIntegration** (~20 tests)
5. **DashboardIntegration** (~20 tests)

**Commands**:
```bash
# All repository tests
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.data.repositories.*" \
  --continue \
  --no-daemon

# Individual repository tests (if failures occur)
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.data.repositories.CertificationApiRepositoryTest" \
  --no-daemon

./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.data.repositories.CrewApiRepositoryTest" \
  --no-daemon

./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.data.repositories.DashboardApiRepositoryTest" \
  --no-daemon
```

**Success Criteria**:
- 95%+ pass rate for repository tests
- All CRUD operations execute without errors
- Mock API call history verification passes
- Pagination and filtering logic works

**Failure Triage**:
- **Feature Flag Errors**: Check FeatureFlags.kt imports
- **Model Import Errors**: Verify model consolidation fixes
- **API Endpoint Mismatch**: Update mock responses
- **Serialization Errors**: Check kotlinx.serialization annotations

---

### Phase 4: Integration Tests (1-2 hours)

**Goal**: Validate cross-service workflows and end-to-end flows

**Test Categories**:
1. **CertificationFlowIntegrationTest** (~10 complete workflows)
2. **Module Integration Tests** (cross-service interactions)
3. **End-to-End Workflow Tests** (full system flows)

**Commands**:
```bash
# Integration tests (custom task)
./gradlew :shared:integrationTest \
  --no-daemon

# Specific integration test
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.integration.CertificationFlowIntegrationTest" \
  --no-daemon

# Module integration
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.integration.*" \
  --no-daemon
```

**Integration Test Workflows**:
1. Upload → OCR → Create → Approve
2. Low confidence OCR → Manual Review → Approve
3. QR Code Generation → Verification
4. DOB Verification → Approval
5. Expiration Tracking → Notifications
6. Batch Upload → Multiple Approvals
7. Rejection → Re-upload → Approval
8. Worker Profile QR with all certifications

**Success Criteria**:
- 100% pass rate (integration tests should be rock-solid)
- Complete workflows execute without errors
- Mock services interact correctly
- Performance targets met (< 500ms per workflow in mock)

**Failure Protocol**:
- Integration test failures are **blocking** - must fix before proceeding
- If > 1 workflow fails → Review service orchestration
- If mock interactions fail → Fix mock infrastructure
- If performance degrades → Profile test execution

---

### Phase 5: New Cross-Service Integration Tests (2-3 hours)

**Goal**: Write and execute 15+ new integration tests for cross-service workflows

**New Tests to Write**:

#### Certification + Crew Integration (5 tests)
```kotlin
// File: CertificationCrewIntegrationTest.kt
1. Worker upload certification → Auto-add to crew if verified
2. Crew member assignment → Check certification requirements
3. Bulk certification approval → Update crew availability
4. Expired certification → Remove from active crew
5. Certification rejection → Notify crew manager
```

#### Dashboard + Certification Integration (5 tests)
```kotlin
// File: DashboardCertificationIntegrationTest.kt
1. Certification upload → Update dashboard metrics
2. Expiration detected → Dashboard alert created
3. Bulk approval → Dashboard compliance score updates
4. Certification stats → Dashboard API responses
5. Real-time certification status → Dashboard refresh
```

#### Crew + Dashboard Integration (5 tests)
```kotlin
// File: CrewDashboardIntegrationTest.kt
1. Crew assignment → Dashboard crew metrics update
2. Crew timesheet → Dashboard labor hours
3. Crew safety events → Dashboard incident counts
4. Crew availability → Dashboard resource planning
5. Multi-project crew → Dashboard project summaries
```

**Commands**:
```bash
# After writing new tests
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.integration.CertificationCrewIntegrationTest" \
  --no-daemon

./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.integration.DashboardCertificationIntegrationTest" \
  --no-daemon

./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.integration.CrewDashboardIntegrationTest" \
  --no-daemon
```

**Success Criteria**:
- All 15+ new tests written and passing
- Cross-service data flows validated
- Mock interactions demonstrate correct API sequencing
- Integration test count: 40 → 55+

---

### Phase 6: Coverage Analysis & Gap Identification (30-45 minutes)

**Goal**: Generate coverage reports and identify untested code paths

**Commands**:
```bash
# Generate JaCoCo coverage report
./gradlew :shared:testDebugUnitTest :shared:jacocoTestReport \
  --no-daemon

# Open HTML report
open /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk/shared/build/reports/jacoco/test/html/index.html

# Verify coverage thresholds
./gradlew :shared:jacocoTestCoverageVerification \
  --no-daemon
```

**Coverage Targets**:
- **Overall**: 80%+ (minimum threshold configured)
- **Services**: 85%+
- **Repositories**: 85%+
- **Domain Models**: 75%+

**Gap Analysis Process**:
1. Review uncovered lines in coverage report
2. Identify critical paths without tests
3. Prioritize gaps by risk (P0: authentication, P1: data flow, P2: UI)
4. Create backlog of missing tests

**Output**: Coverage gap report with prioritized test backlog

---

### Phase 7: Performance Validation (30 minutes)

**Goal**: Ensure tests execute within performance budgets

**Metrics to Track**:
- Total test suite execution time
- Average test execution time
- Slowest 10 tests
- Memory usage during test runs

**Commands**:
```bash
# Run all tests with timing
./gradlew :shared:testDebugUnitTest \
  --profile \
  --no-daemon

# View profile report
open /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk/build/reports/profile/profile-*.html
```

**Performance Targets**:
- **Smoke Tests**: < 10 seconds
- **Unit Tests (Services)**: < 5 minutes
- **Unit Tests (Repositories)**: < 8 minutes
- **Integration Tests**: < 15 minutes
- **Total Suite**: < 30 minutes

**Optimization Strategies**:
- Parallelize independent tests
- Reduce mock response delays
- Use test fixtures efficiently
- Avoid unnecessary database/network setup

---

## 3. Test Validation Criteria

### 3.1 Passing Test Suite Definition

A "passing" test suite means:
- **Compilation**: Zero compilation errors
- **Pass Rate**: 95%+ tests pass (allow 5% for known gaps)
- **Performance**: Test suite completes in < 30 minutes
- **Coverage**: 80%+ line coverage (JaCoCo verification)
- **Stability**: No flaky tests (< 1% failure variance across 3 runs)

### 3.2 Acceptable Failure Thresholds

| Failure Type | Threshold | Action |
|--------------|-----------|--------|
| Compilation Errors | 0 | HALT - Must fix immediately |
| Test Assertion Failures | < 5% | Document and triage |
| Timeout/Hang | < 1% | Investigate mock delays |
| Flaky Tests | < 1% | Mark and investigate separately |
| Coverage Below Target | N/A | Create backlog, do not block |

### 3.3 Test Quality Metrics

**Good Test Characteristics**:
- Clear test names describing scenario
- Arrange-Act-Assert structure
- Single responsibility per test
- Proper mock setup and teardown
- Meaningful assertions (not just "not null")

**Red Flags**:
- Tests with no assertions
- Tests that only check mock was called
- Tests with hardcoded waits/sleeps
- Tests that depend on execution order
- Tests that test implementation details

---

## 4. Test Execution Commands Reference

### 4.1 Full Test Suite Execution

```bash
cd /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk

# Option 1: All tests (may include iOS which requires macOS with Xcode)
./gradlew check --no-daemon

# Option 2: Android unit tests only (recommended)
./gradlew :shared:testDebugUnitTest \
  :shared:testReleaseUnitTest \
  --continue \
  --no-daemon

# Option 3: Custom integration + unit tests
./gradlew :shared:testDebugUnitTest :shared:integrationTest \
  --continue \
  --no-daemon
```

### 4.2 Category-Specific Execution

```bash
# Services only
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.*" \
  --no-daemon

# Repositories only
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.data.repositories.*" \
  --no-daemon

# Integration tests only
./gradlew :shared:integrationTest --no-daemon

# Models only
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.models.*" \
  --no-daemon
```

### 4.3 Individual Test Execution

```bash
# Single test class
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.DOBVerificationServiceTest" \
  --no-daemon

# Single test method
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.DOBVerificationServiceTest.verifyDOB*" \
  --no-daemon

# Pattern matching
./gradlew :shared:testDebugUnitTest \
  --tests "*Certification*" \
  --no-daemon
```

### 4.4 Test Execution with Options

```bash
# With detailed logs
./gradlew :shared:testDebugUnitTest --info

# With debug logs (very verbose)
./gradlew :shared:testDebugUnitTest --debug

# Continue on failure (don't stop at first failure)
./gradlew :shared:testDebugUnitTest --continue

# Parallel execution (faster)
./gradlew :shared:testDebugUnitTest --parallel --max-workers=4

# With profiling
./gradlew :shared:testDebugUnitTest --profile

# Clean and test (fresh build)
./gradlew clean :shared:testDebugUnitTest
```

### 4.5 Coverage Report Generation

```bash
# Generate HTML coverage report
./gradlew :shared:testDebugUnitTest :shared:jacocoTestReport

# Open coverage report (macOS)
open /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk/shared/build/reports/jacoco/test/html/index.html

# Verify coverage thresholds (fails if < 80%)
./gradlew :shared:jacocoTestCoverageVerification
```

---

## 5. Failure Triage Process

### 5.1 Compilation Failures

**Symptoms**: Tests don't compile, Gradle fails before test execution

**Common Causes**:
1. Missing imports after model consolidation
2. FeatureFlags.kt not accessible from HazardHawk/shared
3. Incorrect package names
4. Missing dependencies

**Triage Steps**:
1. Note exact error message and file path
2. Check if error is in test file or source code
3. If FeatureFlags error → Apply build fix Action 1
4. If model import error → Apply build fix Action 2
5. Document in `/docs/implementation/phase2/gates/build-failure-triage.md`

**Priority**: P0 - HALT until fixed

---

### 5.2 Test Assertion Failures

**Symptoms**: Test compiles and runs but assertion fails

**Common Causes**:
1. Mock not configured for specific API call
2. Feature not fully implemented
3. Business logic bug
4. Test expects wrong behavior

**Triage Steps**:
1. Run failing test individually with `--info` flag
2. Check mock call history: `mockApi.getCallHistory()`
3. Verify expected vs actual values
4. Categorize:
   - **Implementation Gap**: Document for dev team (P1)
   - **Mock Issue**: Fix mock response (P2)
   - **Test Bug**: Fix test assertion (P2)
   - **Real Bug**: File bug report (P0 if critical)

**Priority**: P1 - Document and continue (unless > 20% failure rate)

---

### 5.3 Timeout/Hang Failures

**Symptoms**: Test starts but never completes, times out

**Common Causes**:
1. Coroutine not properly handled in test
2. Mock configured with excessive delay
3. Infinite loop in code
4. Missing mock response causes real network call

**Triage Steps**:
1. Check test uses `runTest { }` for coroutines
2. Review mock configuration for `responseDelay`
3. Add timeout to test: `@Test(timeout = 5000)`
4. Check for blocking calls in suspend functions

**Priority**: P1 - Must fix (blocks CI/CD)

---

### 5.4 Flaky Test Failures

**Symptoms**: Test passes sometimes, fails other times

**Common Causes**:
1. Race condition in async code
2. Random data without seed
3. Time-dependent logic (dates, timestamps)
4. Shared mutable state between tests

**Triage Steps**:
1. Run test 10 times: `./gradlew test --tests "FlakyTest" --rerun`
2. Calculate failure rate
3. If > 5% failure rate → Mark as flaky and disable
4. Investigate root cause:
   - Use `runTest` with virtual time
   - Seed random generators
   - Use fixed dates in tests
   - Ensure `@AfterTest` cleanup runs

**Priority**: P2 - Mark and investigate (don't block)

---

## 6. Time Estimates & Milestones

### 6.1 Phased Timeline

| Phase | Duration | Cumulative | Milestone |
|-------|----------|------------|-----------|
| Phase 1: Smoke Tests | 10 min | 10 min | Test framework validated |
| Phase 2: Service Tests | 45 min | 55 min | 140 service tests pass |
| Phase 3: Repository Tests | 60 min | 1h 55m | 240 repository tests pass |
| Phase 4: Integration Tests | 90 min | 3h 25m | 10 workflows validated |
| Phase 5: New Integration Tests | 2-3 hours | 5-6h | 15+ new tests written |
| Phase 6: Coverage Analysis | 30 min | 6h | Coverage report generated |
| Phase 7: Performance Validation | 30 min | 6.5h | Performance targets met |
| **Total** | **6-7 hours** | | **Full validation complete** |

### 6.2 Success Milestones

1. **Milestone 1** (Phase 1 complete): Test framework works ✅
2. **Milestone 2** (Phase 2 complete): Service layer validated ✅
3. **Milestone 3** (Phase 3 complete): Repository layer validated ✅
4. **Milestone 4** (Phase 4 complete): Integration workflows validated ✅
5. **Milestone 5** (Phase 5 complete): Cross-service integration complete ✅
6. **Milestone 6** (Phase 6 complete): Coverage analysis complete ✅
7. **Milestone 7** (Phase 7 complete): Performance validated ✅

**Final Gate**: 95%+ pass rate, 80%+ coverage, < 30 min execution

---

## 7. Automation Potential

### 7.1 Scriptable Test Execution

Create test execution script:

```bash
# File: /Users/aaron/Apps-Coded/HH-v0-fresh/scripts/run-phase2-tests.sh
#!/bin/bash
set -e

cd "$(dirname "$0")/../HazardHawk"

echo "=============================="
echo "Phase 2 Test Execution Script"
echo "=============================="

# Phase 1: Smoke Tests
echo "[Phase 1] Running smoke tests..."
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.test.BasicFrameworkTest" \
  --no-daemon || exit 1

# Phase 2: Service Tests
echo "[Phase 2] Running service tests..."
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.*" \
  --continue \
  --no-daemon

# Phase 3: Repository Tests
echo "[Phase 3] Running repository tests..."
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.data.repositories.*" \
  --continue \
  --no-daemon

# Phase 4: Integration Tests
echo "[Phase 4] Running integration tests..."
./gradlew :shared:integrationTest --no-daemon

# Coverage Report
echo "[Coverage] Generating coverage report..."
./gradlew :shared:jacocoTestReport --no-daemon

echo "=============================="
echo "Test execution complete!"
echo "Coverage report: HazardHawk/shared/build/reports/jacoco/test/html/index.html"
echo "=============================="
```

Make executable:
```bash
chmod +x /Users/aaron/Apps-Coded/HH-v0-fresh/scripts/run-phase2-tests.sh
```

### 7.2 CI/CD Integration

Existing workflow: `.github/workflows/phase2-tests.yml`

Update to use phased execution:
```yaml
jobs:
  phase1-smoke:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run smoke tests
        run: ./gradlew :shared:testDebugUnitTest --tests "*.test.*"
  
  phase2-services:
    needs: phase1-smoke
    runs-on: ubuntu-latest
    steps:
      - name: Run service tests
        run: ./gradlew :shared:testDebugUnitTest --tests "*.services.*"
  
  phase3-repositories:
    needs: phase2-services
    runs-on: ubuntu-latest
    steps:
      - name: Run repository tests
        run: ./gradlew :shared:testDebugUnitTest --tests "*.repositories.*"
```

### 7.3 Pre-Commit Hook

Create Git pre-commit hook to run smoke tests:

```bash
# File: .git/hooks/pre-commit
#!/bin/bash
echo "Running Phase 2 smoke tests before commit..."
cd HazardHawk
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.test.BasicFrameworkTest" \
  --no-daemon --quiet

if [ $? -ne 0 ]; then
  echo "❌ Smoke tests failed. Commit aborted."
  exit 1
fi

echo "✅ Smoke tests passed!"
exit 0
```

---

## 8. Post-Test Validation Actions

### 8.1 Test Results Documentation

After test execution, document:

1. **Test Execution Summary**
   - Total tests: X
   - Passed: Y (Z%)
   - Failed: A (B%)
   - Skipped: C
   - Duration: D minutes

2. **Failure Analysis**
   - Compilation errors: 0 (must be zero)
   - Assertion failures: List with file:line
   - Timeout failures: List with test name
   - Flaky tests: List with failure rate

3. **Coverage Report**
   - Overall coverage: X%
   - By module: Services X%, Repositories Y%, Models Z%
   - Gaps identified: List critical uncovered code

4. **Performance Analysis**
   - Total execution time: X minutes
   - Slowest tests: Top 10 list
   - Memory usage: Peak X GB

### 8.2 Gate Re-Validation

Update gate status document:

```markdown
# Integration Testing Gate RE-RUN

**Date**: October 9, 2025 (Post-Build Fix)
**Status**: [PASS/FAIL]

## Test Execution Results
- Total Tests: 892
- Pass Rate: X%
- Coverage: Y%
- Duration: Z minutes

## Verdict
[✅ PASS / ❌ FAIL]

## Next Steps
- If PASS: Proceed to Week 4 activities
- If FAIL: Apply additional fixes and re-run
```

### 8.3 Week 4 Readiness Checklist

Before proceeding to Week 4:

- [ ] Build compiles with zero errors
- [ ] 95%+ test pass rate achieved
- [ ] 80%+ code coverage achieved
- [ ] Integration tests all passing
- [ ] 15+ new cross-service tests written
- [ ] Performance targets met (< 30 min suite)
- [ ] Test execution documented
- [ ] Gate status updated to PASS

---

## 9. Known Issues & Workarounds

### 9.1 FeatureFlags Import Issue

**Problem**: Tests in `HazardHawk/shared` cannot import `FeatureFlags` from root `shared` module

**Workaround**: Build fix Action 1 will create symlink or copy file

**Test Impact**: CertificationApiRepositoryTest currently has workaround (lines 6, 24, 35)

---

### 9.2 Model Import Issues

**Problem**: 786 compilation errors due to model import paths

**Workaround**: Build fix Action 2 will consolidate models

**Test Impact**: All repository tests may need import updates after fix

---

### 9.3 iOS Tests Require macOS

**Problem**: `iosSimulatorArm64Test` and `iosX64Test` require macOS with Xcode

**Workaround**: Run Android tests only on Linux/Windows CI runners

**Command**: Use `:shared:testDebugUnitTest` instead of `:shared:allTests`

---

## 10. Success Criteria Summary

### Minimum Requirements for PASS

1. ✅ **Zero compilation errors** across all test files
2. ✅ **95%+ pass rate** (allow 5% for implementation gaps)
3. ✅ **80%+ code coverage** (JaCoCo verification)
4. ✅ **All integration tests pass** (100% pass rate)
5. ✅ **15+ new cross-service tests** written and passing
6. ✅ **< 30 minute execution time** for full suite
7. ✅ **No flaky tests** (< 1% variance)
8. ✅ **Test infrastructure stable** (mocks work reliably)

### Recommended Stretch Goals

- 98%+ pass rate (only 2% failures)
- 85%+ code coverage (exceeds minimum)
- 20+ new cross-service tests (exceeds 15 minimum)
- < 20 minute execution time (50% faster)
- 100% test stability (zero flaky tests)

---

## 11. Contact & Escalation

### Primary Contacts

- **test-guardian agent**: Test strategy, execution, results
- **refactor-master agent**: Build fixes, compilation errors
- **simple-architect agent**: Model consolidation, architecture issues

### Escalation Path

1. **Compilation Errors** → refactor-master (P0)
2. **Test Failures > 20%** → simple-architect (P0)
3. **Integration Test Failures** → All agents review (P0)
4. **Coverage Below 70%** → Identify gaps, create backlog (P1)
5. **Performance Issues** → Profile and optimize (P2)

---

## Appendix A: Quick Reference Commands

```bash
# Smoke test (5 min)
./gradlew :shared:testDebugUnitTest --tests "*.test.*" --no-daemon

# All service tests (30 min)
./gradlew :shared:testDebugUnitTest --tests "*.services.*" --no-daemon

# All repository tests (45 min)
./gradlew :shared:testDebugUnitTest --tests "*.repositories.*" --no-daemon

# Integration tests (1 hour)
./gradlew :shared:integrationTest --no-daemon

# Full suite (< 30 min target)
./gradlew :shared:testDebugUnitTest :shared:integrationTest --no-daemon

# Coverage report
./gradlew :shared:jacocoTestReport --no-daemon

# Open coverage
open HazardHawk/shared/build/reports/jacoco/test/html/index.html
```

---

**Document Owner**: test-guardian agent  
**Last Updated**: October 9, 2025  
**Version**: 1.0  
**Status**: Ready for execution post-build fix

---
