# HazardHawk Phase 2 Testing Documentation

**Version**: 1.0  
**Date**: October 9, 2025  
**Owner**: test-guardian agent

---

## Overview

This directory contains comprehensive testing documentation for HazardHawk Phase 2 Backend Integration.

### Test Target Summary

| Category | Target | Priority | Timeline |
|----------|--------|----------|----------|
| Unit Tests | 110 | High | Weeks 1-3 |
| Integration Tests | 30 | High | Week 4 Day 1 |
| E2E Tests | 15 | Medium | Week 4 Day 2 |
| Performance Tests | 5 scenarios | Medium | Week 4 Day 3 |
| **Coverage** | **85%+** | **High** | **Week 4** |

---

## Documentation Structure

### 1. [Test Execution Guide](./test-execution-guide.md)
**Purpose**: How to run tests locally and in CI/CD

**Contents**:
- Quick start commands
- Test categories (unit, integration, E2E)
- Running tests locally
- CI/CD integration
- Coverage reporting
- Debugging failing tests
- Troubleshooting

**When to use**: Daily development, before commits, debugging test failures

---

### 2. Test Writing Guidelines (TODO)
**Purpose**: Standards for writing high-quality tests

**Contents**:
- Naming conventions
- Test structure (Arrange-Act-Assert)
- Mock usage best practices
- Test data management
- Coverage targets
- Code examples

**When to use**: Writing new tests, code reviews

---

### 3. Mock API Guide (TODO)
**Purpose**: Using mock infrastructure for testing

**Contents**:
- MockApiClient usage
- MockS3Client for file uploads
- MockOCRClient for document AI
- MockNotificationClient for notifications
- Test fixtures
- Custom response configuration

**When to use**: Writing unit/integration tests

---

## Quick Reference

### Run All Tests
```bash
cd HazardHawk
./gradlew :shared:test :shared:integrationTest
```

### Run with Coverage
```bash
./gradlew :shared:test :shared:jacocoTestReport
open HazardHawk/shared/build/reports/jacoco/test/html/index.html
```

### Start Test Services
```bash
docker-compose -f docker-compose.test.yml up -d
```

---

## Test Infrastructure

### CI/CD Pipeline
- **Workflow**: `.github/workflows/phase2-tests.yml`
- **Triggers**: Push to main/develop/feature, Pull requests
- **Jobs**: Unit tests, Integration tests, E2E tests, Performance tests
- **Reports**: Uploaded as artifacts, coverage to Codecov

### Localstack (AWS Emulation)
- **Purpose**: S3, SES, SNS, SQS testing without AWS costs
- **Endpoint**: http://localhost:4566
- **Setup**: `docker-compose -f docker-compose.test.yml up -d localstack`
- **Init Script**: `scripts/localstack-init.sh`

### Mock Infrastructure
- **Location**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/data/mocks/`
- **Components**:
  - `MockApiClient.kt` - HTTP client mocking
  - `MockS3Client.kt` - S3 upload/download mocking
  - `MockOCRClient.kt` - Document AI mocking
  - `MockNotificationClient.kt` - Email/SMS/Push mocking
  - `MockApiResponses.kt` - Sample JSON responses

### Test Fixtures
- **Location**: `/HazardHawk/shared/src/commonTest/kotlin/com/hazardhawk/fixtures/`
- **Contents**: TestFixtures.kt with sample data for:
  - Certifications (OSHA 10, OSHA 30, CPR, First Aid)
  - Crew/Workers (laborers, supervisors, foremen)
  - Projects (construction, roadwork)
  - Dashboard metrics
  - Test images (base64 encoded)

---

## Test Coverage Targets

### By Module
- **Shared Module**: 85%+ (all services, repositories)
- **Domain Layer**: 90%+ (business logic)
- **Data Layer**: 80%+ (API clients, caching)
- **Android App**: 75%+ (UI ViewModels)

### By Component
- **FileUploadService**: 30 tests (compression, retry logic, progress)
- **OCRService**: 40 tests (extraction accuracy, confidence handling)
- **NotificationService**: 15 tests (email, SMS, push)
- **Repositories**: 25 tests (CRUD, caching, error handling)

---

## Quality Gates

Phase 2 testing includes 7 quality gates:

1. **Week 1**: Foundation layer approval (20+ tests)
2. **Week 2**: Service integration phase 1 (90+ tests)
3. **Week 3**: Service integration phase 2 (150+ tests)
4. **Week 4 Day 1**: Integration testing (15 tests, 100% pass)
5. **Week 4 Day 2**: E2E testing (10 tests, 100% pass)
6. **Week 4 Day 3**: Performance testing (targets met)
7. **Week 4 Day 4**: Canary rollout (<1% error rate)

**Failure Protocol**: If any gate fails, halt progression and fix issues.

---

## Key Files

### Configuration
- `HazardHawk/shared/build.gradle.kts` - Test tasks, JaCoCo, coverage
- `docker-compose.test.yml` - Localstack, PostgreSQL, MockServer
- `.github/workflows/phase2-tests.yml` - CI/CD pipeline

### Scripts
- `scripts/localstack-init.sh` - Initialize AWS services in Localstack
- `scripts/setup-test-s3.sh` - Create S3 buckets and upload test files

### Documentation
- `docs/testing/test-execution-guide.md` - How to run tests
- `docs/testing/test-writing-guidelines.md` - How to write tests (TODO)
- `docs/testing/mock-api-guide.md` - How to use mocks (TODO)

---

## Common Commands

```bash
# Unit tests
./gradlew :shared:test

# Integration tests (requires Localstack)
docker-compose -f docker-compose.test.yml up -d
./gradlew :shared:integrationTest

# E2E tests (Android)
./gradlew :androidApp:connectedDebugAndroidTest

# Coverage report
./gradlew :shared:jacocoTestReport

# Code quality
./gradlew ktlintCheck detekt

# Clean build
./gradlew clean build
```

---

## Success Criteria

At the end of Phase 2, testing infrastructure will support:

- ✅ 155 total tests (110 unit + 30 integration + 15 E2E)
- ✅ 85%+ code coverage
- ✅ CI/CD pipeline running on every commit
- ✅ Comprehensive mock infrastructure
- ✅ Localstack for integration testing
- ✅ Performance testing with k6
- ✅ Quality gates enforced
- ✅ Clear documentation for all testing processes

---

## Contact

- **test-guardian agent**: Primary testing contact
- **simple-architect**: Architecture and API design questions
- **loveable-ux**: UX testing and user experience
- **refactor-master**: Code quality and refactoring

---

**Last Updated**: October 9, 2025 by test-guardian agent

---

## CRITICAL ALERT: Week 4 Day 1 Gate FAILED

**Date**: October 9, 2025  
**Status**: 🔴 **CRITICAL - BUILD FAILURE**  
**Impact**: All Phase 2 Week 4 work BLOCKED

### Issue Summary
Integration testing gate **FAILED** with 786 compilation errors preventing all test execution. Zero tests could run to validate Week 3 claims of 138 tests.

### Root Cause
Dual shared module architecture:
- `/shared` (root) - Contains FeatureFlags.kt
- `/HazardHawk/shared` - Phase 2 code cannot import from root

### Immediate Actions Required
1. **STOP** all Week 4 development
2. Fix module architecture (P0 - refactor-master)
3. Fix model imports (P0 - simple-architect)
4. Re-run integration gate (P1 - test-guardian)

### Full Report
See: [Integration Testing Gate FAIL Report](../implementation/phase2/gates/gate-integration-testing-FAIL.md)

### Next Steps
- Pause Phase 2 Week 4 activities
- Execute fixes per Action 1-2 in gate report
- Re-validate all 138 claimed tests after compilation fixes

**DO NOT PROCEED** with Week 4 until gate re-run shows PASS.

