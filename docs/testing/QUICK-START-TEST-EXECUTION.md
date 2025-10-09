# Quick Start: Phase 2 Test Execution

**Status**: Ready to execute after build fixes  
**Estimated Time**: 6-8 hours  
**Owner**: test-guardian agent

---

## Pre-Requisites

Before running tests, ensure build fixes are applied:

- [ ] ✅ Action 1: FeatureFlags.kt accessible from HazardHawk/shared
- [ ] ✅ Action 2: Model consolidation complete (786 errors fixed)
- [ ] ✅ Gradle build succeeds: `./gradlew :shared:build`

---

## Option 1: Automated Script (Recommended)

```bash
cd /Users/aaron/Apps-Coded/HH-v0-fresh
./scripts/run-phase2-tests.sh
```

**What it does**:
1. Runs smoke tests (validates framework)
2. Runs service tests (~140 tests)
3. Runs repository tests (~240 tests)
4. Runs integration tests (~60 tests)
5. Generates coverage report
6. Provides color-coded summary

**Output**: Pass/Fail for each phase + coverage report location

---

## Option 2: Manual Phase-by-Phase

### Step 1: Smoke Test (2 minutes)
```bash
cd /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.test.BasicFrameworkTest" \
  --no-daemon
```
**Expected**: 4 tests pass ✅

---

### Step 2: Service Tests (30-45 minutes)
```bash
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.*" \
  --continue \
  --no-daemon
```
**Expected**: 90%+ pass rate (~140 tests)

---

### Step 3: Repository Tests (45-60 minutes)
```bash
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.data.repositories.*" \
  --continue \
  --no-daemon
```
**Expected**: 95%+ pass rate (~240 tests)

---

### Step 4: Integration Tests (1-2 hours)
```bash
./gradlew :shared:integrationTest --no-daemon
# OR if custom task not available:
./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.integration.*" \
  --no-daemon
```
**Expected**: 100% pass rate (~60 tests)

---

### Step 5: Coverage Report (5 minutes)
```bash
./gradlew :shared:jacocoTestReport --no-daemon
open /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk/shared/build/reports/jacoco/test/html/index.html
```
**Expected**: 80%+ coverage ✅

---

## Option 3: Full Suite (One Command)

```bash
cd /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk
./gradlew :shared:testDebugUnitTest :shared:integrationTest \
  --continue \
  --no-daemon
```

**Duration**: 2-3 hours  
**Tests**: ~450 tests (excluding legacy root shared)

---

## Success Criteria Checklist

After test execution, verify:

- [ ] ✅ Zero compilation errors
- [ ] ✅ 95%+ pass rate (allow 5% for implementation gaps)
- [ ] ✅ 80%+ code coverage (JaCoCo verification)
- [ ] ✅ All integration tests pass (100%)
- [ ] ✅ Test suite completes in < 30 minutes
- [ ] ✅ No flaky tests (< 1% variance)

---

## What to Do if Tests Fail

### Compilation Errors
**Action**: HALT - Apply remaining build fixes
**Owner**: refactor-master agent

### > 20% Test Failures
**Action**: Review implementation gaps
**Owner**: simple-architect agent

### Integration Test Failures
**Action**: Review service orchestration
**Owner**: All agents (critical issue)

### Coverage < 80%
**Action**: Identify gaps, create backlog
**Owner**: test-guardian agent (non-blocking)

---

## Where to Find Test Results

**HTML Test Report**:
```
/Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk/shared/build/reports/tests/testDebugUnitTest/index.html
```

**Coverage Report**:
```
/Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk/shared/build/reports/jacoco/test/html/index.html
```

**Test Logs**:
```
/Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk/shared/build/test-results/testDebugUnitTest/
```

---

## Quick Commands Reference

```bash
# Smoke test only
./gradlew :shared:testDebugUnitTest --tests "*.test.*" --no-daemon

# Specific service test
./gradlew :shared:testDebugUnitTest --tests "*.DOBVerificationServiceTest" --no-daemon

# All services
./gradlew :shared:testDebugUnitTest --tests "*.services.*" --no-daemon

# All repositories
./gradlew :shared:testDebugUnitTest --tests "*.repositories.*" --no-daemon

# All integration
./gradlew :shared:testDebugUnitTest --tests "*.integration.*" --no-daemon

# With detailed logs
./gradlew :shared:testDebugUnitTest --tests "*Test" --info

# Clean and test
./gradlew clean :shared:testDebugUnitTest --no-daemon

# Coverage
./gradlew :shared:jacocoTestReport --no-daemon
```

---

## Next Steps After Validation

1. **Document Results** - Create test execution summary
2. **Update Gate Status** - Mark integration gate as PASS/FAIL
3. **Triage Failures** - Categorize and prioritize any failures
4. **Write New Tests** - Phase 5: 15+ cross-service integration tests
5. **Performance Tune** - Optimize if suite > 30 minutes

---

## Need Help?

**Detailed Documentation**:
- `/docs/testing/test-execution-strategy-post-build-fix.md` (11 sections)
- `/docs/testing/TEST-RESEARCH-SUMMARY.md` (comprehensive analysis)
- `/docs/testing/test-execution-guide.md` (how-to guide)

**Automation**:
- `/scripts/run-phase2-tests.sh` (automated execution)

**Contacts**:
- test-guardian: Test execution and results
- refactor-master: Build and compilation issues
- simple-architect: Architecture and model issues

---

**Last Updated**: October 9, 2025  
**Version**: 1.0  
**Status**: Ready for execution

---
