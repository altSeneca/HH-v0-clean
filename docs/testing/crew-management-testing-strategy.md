# Crew Management Testing Strategy
**Version**: 1.0  
**Created**: October 8, 2025  
**Coverage Targets**: Unit 90%+ | Integration 80%+ | Critical Paths 100%  
**Performance Target**: All APIs <200ms

---

## Table of Contents

1. [Testing Philosophy](#testing-philosophy)
2. [Test Coverage Matrix](#test-coverage-matrix)
3. [Phase 2: Certification Management Tests](#phase-2-certification-management-tests)
4. [Phase 3: Worker Onboarding Tests](#phase-3-worker-onboarding-tests)
5. [Phase 4: Crew Assignment Tests](#phase-4-crew-assignment-tests)
6. [Performance Test Specifications](#performance-test-specifications)
7. [Security Test Cases](#security-test-cases)
8. [Test Data Generation](#test-data-generation)
9. [Mock Services](#mock-services)
10. [CI/CD Integration](#cicd-integration)
11. [Acceptance Criteria](#acceptance-criteria)

---

## Testing Philosophy

### Simple, Loveable, Complete Framework

**SIMPLE**
- Tests should be easy to write, read, and maintain
- One assertion per test when possible
- Clear, descriptive test names that explain behavior
- Minimal setup/teardown complexity

**LOVEABLE**
- Tests provide confidence, not just coverage
- Fast feedback loops (<5s for unit tests, <30s for integration)
- Helpful failure messages that guide debugging
- Tests serve as living documentation

**COMPLETE**
- All critical paths have 100% coverage
- Edge cases and error scenarios tested
- Performance regression detection
- Security vulnerabilities caught early

### Test Pyramid Distribution

```
                    /\
                   /  \     E2E Tests (5%)
                  /____\    - Critical user flows
                 /      \   - Multi-phase workflows
                /________\  
               /          \ Integration Tests (25%)
              /____________\- API contract tests
             /              \- Database interactions
            /________________\- External service mocks
           /                  \
          /____________________\ Unit Tests (70%)
                                - Business logic
                                - Repository methods
                                - ViewModels
                                - Data transformations
```

---

## Test Coverage Matrix

| Phase | Feature | Unit | Integration | UI | Performance | Security |
|-------|---------|------|-------------|-----|-------------|----------|
| **Phase 2** | OCR Extraction | ✅ 95% | ✅ 85% | ✅ 80% | ✅ Benchmarked | ✅ File upload |
| | Cert Verification | ✅ 90% | ✅ 80% | ✅ 75% | ✅ <100ms | ✅ Auth flow |
| | Expiration Alerts | ✅ 100% | ✅ 90% | N/A | ✅ BG jobs | N/A |
| **Phase 3** | Magic Links | ✅ 100% | ✅ 100% | ✅ 90% | ✅ <2s gen | ✅ Token security |
| | Multi-step Forms | ✅ 90% | ✅ 85% | ✅ 95% | ✅ <200ms/step | ✅ Validation |
| | E-Signatures | ✅ 85% | ✅ 80% | ✅ 90% | ✅ <1s capture | ✅ Non-repudiation |
| **Phase 4** | Drag-and-Drop | ✅ 80% | ✅ 75% | ✅ 100% | ✅ <50ms | N/A |
| | Roster Gen | ✅ 95% | ✅ 90% | ✅ 85% | ✅ <2s PDF | ✅ Data privacy |
| | Real-time Updates | ✅ 90% | ✅ 90% | ✅ 85% | ✅ <500ms WS | ✅ Channel auth |

---

## Phase 2: Certification Management Tests

See separate document: `phase-2-certification-tests.md`

**Unit Test Files**:
- `CertificationOCRProcessorTest.kt` - OCR extraction logic (30+ tests)
- `CertificationRepositoryTest.kt` - CRUD operations (50+ tests)
- `ExpirationTrackerTest.kt` - Alert scheduling logic (25+ tests)

**Integration Test Files**:
- `CertificationUploadE2ETest.kt` - Upload to verification flow (10+ tests)
- `CertificationExpirationFlowTest.kt` - Alert delivery and automation (8+ tests)

---

## Phase 3: Worker Onboarding Tests

See separate document: `phase-3-onboarding-tests.md`

**Unit Test Files**:
- `MagicLinkServiceTest.kt` - Token generation and verification (20+ tests)
- `OnboardingSessionManagerTest.kt` - Multi-step form state (15+ tests)
- `ESignatureServiceTest.kt` - Signature capture and storage (12+ tests)

**Integration Test Files**:
- `WorkerOnboardingE2ETest.kt` - Complete onboarding workflow (8+ tests)
- `MagicLinkDeliveryTest.kt` - Email/SMS delivery (6+ tests)

---

## Phase 4: Crew Assignment Tests

See separate document: `phase-4-crew-assignment-tests.md`

**Unit Test Files**:
- `CrewRepositoryTest.kt` - CRUD operations (40+ tests)
- `RosterGeneratorTest.kt` - PDF generation logic (18+ tests)
- `CrewWebSocketManagerTest.kt` - Real-time updates (15+ tests)

**Integration Test Files**:
- `CrewAssignmentE2ETest.kt` - Drag-and-drop workflow (10+ tests)
- `RosterGenerationE2ETest.kt` - PDF generation flow (8+ tests)

---

## Performance Test Specifications

### Benchmark Test Suite

**File**: `androidApp/src/androidTest/kotlin/com/hazardhawk/benchmark/CrewManagementBenchmark.kt`

```kotlin
package com.hazardhawk.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit4.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrewManagementBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmarkListWorkers_10Workers() {
        benchmarkRule.measureRepeated {
            // List 10 workers from repository
            runBlocking {
                repository.getWorkers(
                    companyId = "test_company",
                    pagination = PaginationRequest(pageSize = 10)
                )
            }
        }
        // Target: p95 < 50ms
    }

    @Test
    fun benchmarkListWorkers_100Workers() {
        benchmarkRule.measureRepeated {
            runBlocking {
                repository.getWorkers(
                    companyId = "test_company",
                    pagination = PaginationRequest(pageSize = 100)
                )
            }
        }
        // Target: p95 < 100ms
    }

    @Test
    fun benchmarkCertificationUploadWithOCR() {
        val testDocument = loadTestDocument("osha10_cert.pdf")
        
        benchmarkRule.measureRepeated {
            runBlocking {
                repository.uploadCertificationDocument(
                    workerProfileId = "worker_001",
                    companyId = "test_company",
                    documentData = testDocument,
                    fileName = "osha10_cert.pdf",
                    mimeType = "application/pdf"
                )
            }
        }
        // Target: p95 < 5000ms (5s)
    }

    @Test
    fun benchmarkGenerateCrewRosterPDF_50Members() {
        benchmarkRule.measureRepeated {
            runBlocking {
                rosterGenerator.generatePDF(
                    crew = createTestCrew(memberCount = 50),
                    includeCertifications = true,
                    includePhotos = false
                )
            }
        }
        // Target: p95 < 2000ms (2s)
    }

    @Test
    fun benchmarkMagicLinkGeneration() {
        benchmarkRule.measureRepeated {
            runBlocking {
                magicLinkService.generateMagicLink(
                    email = "worker@example.com",
                    companyId = "test_company"
                )
            }
        }
        // Target: p95 < 2000ms (2s)
    }
}
```

### Load Testing Scenarios

**File**: `load-tests/crew-management.k6.js`

```javascript
import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const workerListDuration = new Trend('worker_list_duration');
const certUploadDuration = new Trend('cert_upload_duration');

export let options = {
  stages: [
    { duration: '2m', target: 50 },   // Ramp up to 50 users
    { duration: '5m', target: 100 },  // Hold at 100 users
    { duration: '3m', target: 200 },  // Spike to 200 users
    { duration: '2m', target: 0 },    // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'],  // 95% of requests under 500ms
    'errors': ['rate<0.01'],              // Error rate below 1%
    'worker_list_duration': ['p(95)<200'], // Worker list API target
    'cert_upload_duration': ['p(95)<5000'], // Cert upload target
  },
};

const BASE_URL = __ENV.API_BASE_URL || 'https://api.hazardhawk.com/v1';
const API_TOKEN = __ENV.API_TOKEN;

export default function () {
  const headers = {
    'Authorization': `Bearer ${API_TOKEN}`,
    'Content-Type': 'application/json',
  };

  // Scenario 1: List workers
  group('List Workers', () => {
    const res = http.get(`${BASE_URL}/workers?page_size=20`, { headers });
    
    workerListDuration.add(res.timings.duration);
    
    check(res, {
      'workers list status 200': (r) => r.status === 200,
      'workers list response time < 200ms': (r) => r.timings.duration < 200,
      'workers list has data': (r) => JSON.parse(r.body).data.length > 0,
    }) || errorRate.add(1);
  });

  sleep(1);

  // Scenario 2: Get worker detail
  group('Get Worker Detail', () => {
    const res = http.get(`${BASE_URL}/workers/worker_001`, { headers });
    
    check(res, {
      'worker detail status 200': (r) => r.status === 200,
      'worker detail response time < 100ms': (r) => r.timings.duration < 100,
      'worker detail includes certifications': (r) => 
        JSON.parse(r.body).certifications !== undefined,
    }) || errorRate.add(1);
  });

  sleep(2);

  // Scenario 3: Search workers
  group('Search Workers', () => {
    const res = http.get(
      `${BASE_URL}/search/workers?q=john&filters[status]=active`,
      { headers }
    );
    
    check(res, {
      'search status 200': (r) => r.status === 200,
      'search response time < 300ms': (r) => r.timings.duration < 300,
    }) || errorRate.add(1);
  });

  sleep(3);

  // Scenario 4: List pending certifications
  group('Pending Certifications', () => {
    const res = http.get(`${BASE_URL}/certifications/pending`, { headers });
    
    check(res, {
      'pending certs status 200': (r) => r.status === 200,
      'pending certs response time < 100ms': (r) => r.timings.duration < 100,
    }) || errorRate.add(1);
  });

  sleep(5);
}

// Spike test scenario
export function spikeTest() {
  const headers = {
    'Authorization': `Bearer ${API_TOKEN}`,
    'Content-Type': 'application/json',
  };

  // Rapid-fire requests to test system under stress
  for (let i = 0; i < 10; i++) {
    http.get(`${BASE_URL}/workers?page_size=100`, { headers });
  }
}
```

---

## Security Test Cases

### Authentication & Authorization Tests

**File**: `shared/src/commonTest/kotlin/com/hazardhawk/security/CrewManagementSecurityTest.kt`

```kotlin
package com.hazardhawk.security

import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Security-focused tests for crew management features
 * 
 * Focus areas:
 * - Authentication bypass attempts
 * - Authorization privilege escalation
 * - Data access controls
 * - File upload vulnerabilities
 * - Token security
 */
class CrewManagementSecurityTest {

    private lateinit var securityTestHelper: SecurityTestHelper

    @BeforeTest
    fun setup() {
        securityTestHelper = SecurityTestHelper()
    }

    // ===== Magic Link Security =====

    @Test
    fun `magic link tokens are cryptographically secure`() = runTest {
        // Generate 1000 tokens
        val tokens = (1..1000).map {
            magicLinkService.generateMagicLink(
                email = "worker@example.com",
                companyId = "test_company"
            ).getOrNull()?.token
        }

        // Verify uniqueness (no collisions)
        assertEquals(1000, tokens.toSet().size)

        // Verify entropy (minimum 256 bits)
        tokens.forEach { token ->
            assertNotNull(token)
            assertTrue(token.length >= 43) // Base64(256 bits) = 43 chars
            
            // Verify contains sufficient character variety
            val hasUppercase = token.any { it.isUpperCase() }
            val hasLowercase = token.any { it.isLowerCase() }
            val hasDigits = token.any { it.isDigit() }
            
            assertTrue(hasUppercase && hasLowercase && hasDigits)
        }
    }

    @Test
    fun `magic link tokens stored as secure hashes`() = runTest {
        // Given: Generate magic link
        val magicLink = magicLinkService.generateMagicLink(
            email = "worker@example.com",
            companyId = "test_company"
        ).getOrNull()!!

        // When: Retrieve token from database
        val storedToken = database.getTokenRecord(magicLink.token)

        // Then: Token should be hashed, not plaintext
        assertNotNull(storedToken)
        assertNotEquals(magicLink.token, storedToken.tokenHash)
        
        // Verify bcrypt hash format
        assertTrue(storedToken.tokenHash.startsWith("\$2a\$") || 
                   storedToken.tokenHash.startsWith("\$2b\$"))
    }

    @Test
    fun `magic link rate limiting prevents brute force`() = runTest {
        val email = "worker@example.com"
        val companyId = "test_company"

        // Attempt to generate many tokens rapidly
        val results = (1..20).map {
            magicLinkService.generateMagicLink(email, companyId)
        }

        // First 5 should succeed
        assertTrue(results.take(5).all { it.isSuccess })

        // Remaining should be rate limited
        val rateLimitedResults = results.drop(5)
        assertTrue(rateLimitedResults.all { it.isFailure })
        
        val error = rateLimitedResults.first().exceptionOrNull()
        assertTrue(error is RateLimitExceededException)
    }

    // ===== File Upload Security =====

    @Test
    fun `certification upload validates MIME types`() = runTest {
        val maliciousFiles = listOf(
            "malicious.exe" to "application/x-msdownload",
            "script.php" to "application/x-php",
            "payload.sh" to "application/x-sh"
        )

        maliciousFiles.forEach { (fileName, mimeType) ->
            val result = repository.uploadCertificationDocument(
                workerProfileId = "worker_001",
                companyId = "test_company",
                documentData = "fake_data".toByteArray(),
                fileName = fileName,
                mimeType = mimeType
            )

            // Should reject non-document MIME types
            assertTrue(result.isFailure)
            assertTrue(
                result.exceptionOrNull() is InvalidMimeTypeException,
                "Failed to reject $mimeType"
            )
        }
    }

    @Test
    fun `certification upload enforces file size limit`() = runTest {
        // Create oversized file (> 10MB)
        val oversizedFile = ByteArray(11 * 1024 * 1024) // 11MB

        val result = repository.uploadCertificationDocument(
            workerProfileId = "worker_001",
            companyId = "test_company",
            documentData = oversizedFile,
            fileName = "large_file.pdf",
            mimeType = "application/pdf"
        )

        // Should reject file over 10MB limit
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is FileSizeExceededException)
    }

    @Test
    fun `uploaded files scanned for malware`() = runTest {
        // Simulate EICAR test virus string
        val eicarTestString = "X5O!P%@AP[4\\PZX54(P^)7CC)7}\$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!\$H+H*"
        val maliciousFile = eicarTestString.toByteArray()

        val result = repository.uploadCertificationDocument(
            workerProfileId = "worker_001",
            companyId = "test_company",
            documentData = maliciousFile,
            fileName = "test.pdf",
            mimeType = "application/pdf"
        )

        // Should detect and reject malicious file
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is MalwareDetectedException)
    }

    // ===== Authorization Tests =====

    @Test
    fun `field access user cannot approve certifications`() = runTest {
        // Given: User with FIELD_ACCESS tier
        securityTestHelper.authenticateAs(
            userId = "field_user_001",
            tier = UserTier.FIELD_ACCESS
        )

        // When: Attempting to approve certification
        val result = repository.approveCertification(
            certificationId = "cert_001",
            verifiedBy = "field_user_001"
        )

        // Then: Should fail with permission error
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is InsufficientPermissionsException)
    }

    @Test
    fun `users cannot access other companies' data`() = runTest {
        // Given: User from Company A
        securityTestHelper.authenticateAs(
            userId = "user_company_a",
            companyId = "company_a"
        )

        // When: Attempting to access Company B's workers
        val result = repository.getWorkers(
            companyId = "company_b", // Different company!
            filters = WorkerFilters()
        )

        // Then: Should return empty list (RLS prevents access)
        assertTrue(result.data.isEmpty())
        
        // Alternative: Could throw exception
        // assertTrue(result.isFailure)
        // assertTrue(result.exceptionOrNull() is UnauthorizedAccessException)
    }

    @Test
    fun `PDF rosters do not leak PII`() = runTest {
        // Given: Crew with sensitive worker data
        val crew = createTestCrewWithPII()

        // When: Generating PDF roster
        val pdfBytes = rosterGenerator.generatePDF(
            crew = crew,
            includeCertifications = true,
            includePhotos = false
        )

        val pdfText = extractTextFromPDF(pdfBytes)

        // Then: Should NOT contain sensitive PII
        assertFalse(pdfText.contains("SSN"))
        assertFalse(pdfText.contains("Date of Birth"))
        assertFalse(pdfText.contains("Emergency Contact"))
        
        // Should only contain work-related info
        assertTrue(pdfText.contains("Employee Number"))
        assertTrue(pdfText.contains("Role"))
        assertTrue(pdfText.contains("Certifications"))
    }

    // ===== E-Signature Security =====

    @Test
    fun `e-signatures stored with non-repudiation metadata`() = runTest {
        // Given: Worker signs onboarding form
        val signatureData = "signature_svg_data".toByteArray()
        
        val result = onboardingService.captureSignature(
            sessionId = "onboarding_session_001",
            signatureData = signatureData
        )

        // When: Retrieving signature record
        val signatureRecord = database.getSignature(result.getOrNull()!!.signatureId)

        // Then: Should include non-repudiation metadata
        assertNotNull(signatureRecord)
        assertNotNull(signatureRecord.timestamp) // When signed
        assertNotNull(signatureRecord.ipAddress) // From where
        assertNotNull(signatureRecord.userAgent) // Which device
        assertNotNull(signatureRecord.sessionId) // Which session
        
        // Signature should be tamper-proof (hashed)
        assertNotNull(signatureRecord.signatureHash)
        assertTrue(signatureRecord.signatureHash.length >= 64) // SHA-256 minimum
    }

    @Test
    fun `WebSocket connections require authentication`() = runTest {
        // Given: Unauthenticated WebSocket connection attempt
        val wsClient = WebSocketClient()

        // When: Attempting to connect without token
        val result = wsClient.connect(
            url = "wss://api.hazardhawk.com/crews/realtime",
            authToken = null
        )

        // Then: Should reject connection
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is WebSocketAuthenticationException)
    }

    @Test
    fun `WebSocket channels isolated by company`() = runTest {
        // Given: Two users from different companies
        val user1 = authenticateUser("user_company_a", "company_a")
        val user2 = authenticateUser("user_company_b", "company_b")

        // When: Both subscribe to crew updates
        val ws1 = connectWebSocket(user1)
        val ws2 = connectWebSocket(user2)

        ws1.subscribe("crew_updates")
        ws2.subscribe("crew_updates")

        // And: Company A creates a new crew
        val crewA = repository.createCrew(
            companyId = "company_a",
            request = CreateCrewRequest(name = "Test Crew A")
        )

        // Then: Only user1 should receive the update
        val update1 = ws1.receiveNextMessage(timeout = 2.seconds)
        val update2 = ws2.receiveNextMessage(timeout = 2.seconds)

        assertNotNull(update1)
        assertTrue(update1.contains("Test Crew A"))
        
        assertNull(update2) // User2 should NOT receive it
    }
}

// Security exception classes
class InvalidMimeTypeException(message: String) : Exception(message)
class FileSizeExceededException(message: String) : Exception(message)
class MalwareDetectedException(message: String) : Exception(message)
class InsufficientPermissionsException(message: String) : Exception(message)
class UnauthorizedAccessException(message: String) : Exception(message)
class WebSocketAuthenticationException(message: String) : Exception(message)
```


---

## Test Data Generation

### Test Data Factory Pattern

**File**: `shared/src/commonTest/kotlin/com/hazardhawk/test/TestDataFactory.kt`

```kotlin
package com.hazardhawk.test

import com.hazardhawk.models.crew.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

/**
 * Centralized test data generation for crew management tests
 * 
 * Benefits:
 * - Consistent test data across all tests
 * - Easy to create realistic scenarios
 * - Reduces test setup boilerplate
 * - Supports data relationships
 */
object TestDataFactory {

    // ===== Company Data =====

    fun createTestCompany(
        id: String = "company_${randomId()}",
        name: String = "Test Construction Co.",
        tier: String = "professional",
        maxWorkers: Int = 100
    ): Company {
        return Company(
            id = id,
            name = name,
            subdomain = name.lowercase().replace(" ", "-"),
            tier = tier,
            address = "123 Main St",
            city = "Test City",
            state = "TX",
            zip = "12345",
            phone = "+1-555-${Random.nextInt(1000, 9999)}",
            logoUrl = "https://storage.example.com/logos/$id.png"
        )
    }

    // ===== Project Data =====

    fun createTestProject(
        id: String = "project_${randomId()}",
        companyId: String,
        name: String = "Test Project ${Random.nextInt(1, 100)}",
        status: String = "active"
    ): Project {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        return Project(
            id = id,
            companyId = companyId,
            name = name,
            projectNumber = "PRJ-${Random.nextInt(1000, 9999)}",
            location = "Construction Site",
            startDate = today.minus(30.days),
            endDate = today.plus(180.days),
            status = status,
            projectManagerId = null,
            superintendentId = null,
            clientName = "Test Client",
            clientContact = "John Client",
            clientPhone = "+1-555-0100",
            clientEmail = "client@example.com",
            streetAddress = "456 Construction Ave",
            city = "Build City",
            state = "TX",
            zip = "54321",
            generalContractor = "Test GC Company",
            company = null,
            projectManager = null,
            superintendent = null
        )
    }

    // ===== Worker Data =====

    fun createTestWorkerProfile(
        id: String = "worker_profile_${randomId()}",
        firstName: String = randomFirstName(),
        lastName: String = randomLastName(),
        email: String? = "${firstName.lowercase()}.${lastName.lowercase()}@example.com",
        phone: String? = "+1-555-${Random.nextInt(1000, 9999)}"
    ): WorkerProfile {
        val now = Clock.System.now().toString()
        return WorkerProfile(
            id = id,
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = randomDateOfBirth(),
            email = email,
            phone = phone,
            photoUrl = "https://storage.example.com/workers/$id.jpg",
            createdAt = now,
            updatedAt = now
        )
    }

    fun createTestCompanyWorker(
        id: String = "company_worker_${randomId()}",
        companyId: String,
        workerProfile: WorkerProfile = createTestWorkerProfile(),
        role: WorkerRole = WorkerRole.LABORER,
        status: WorkerStatus = WorkerStatus.ACTIVE,
        certifications: List<WorkerCertification> = emptyList()
    ): CompanyWorker {
        val now = Clock.System.now().toString()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        return CompanyWorker(
            id = id,
            companyId = companyId,
            workerProfileId = workerProfile.id,
            employeeNumber = "E-${Random.nextInt(1000, 9999)}",
            role = role,
            hireDate = today.minus(Random.nextInt(30, 365).days),
            status = status,
            hourlyRate = when (role) {
                WorkerRole.LABORER -> 18.0 + Random.nextDouble(0.0, 5.0)
                WorkerRole.SKILLED_WORKER -> 25.0 + Random.nextDouble(0.0, 10.0)
                WorkerRole.FOREMAN -> 35.0 + Random.nextDouble(0.0, 15.0)
                else -> 30.0
            },
            permissions = emptyList(),
            createdAt = now,
            updatedAt = now,
            workerProfile = workerProfile,
            certifications = certifications,
            crews = emptyList()
        )
    }

    // ===== Certification Data =====

    fun createTestCertification(
        id: String = "cert_${randomId()}",
        workerProfileId: String,
        companyId: String? = null,
        certificationType: CertificationType = createTestCertificationType("OSHA_10"),
        status: CertificationStatus = CertificationStatus.VERIFIED,
        expirationDate: LocalDate? = Clock.System.todayIn(TimeZone.currentSystemDefault()).plus(365.days)
    ): WorkerCertification {
        val now = Clock.System.now().toString()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        
        return WorkerCertification(
            id = id,
            workerProfileId = workerProfileId,
            companyId = companyId,
            certificationTypeId = certificationType.id,
            certificationNumber = generateCertNumber(certificationType.code),
            issueDate = today.minus(Random.nextInt(10, 100).days),
            expirationDate = expirationDate,
            issuingAuthority = when (certificationType.code) {
                "OSHA_10", "OSHA_30" -> "OSHA Training Institute"
                "CPR", "FIRST_AID" -> "American Red Cross"
                "FORKLIFT" -> "National Safety Council"
                else -> "Certifying Authority"
            },
            documentUrl = "s3://certs/$workerProfileId/${certificationType.code}.pdf",
            thumbnailUrl = "s3://certs/$workerProfileId/${certificationType.code}_thumb.jpg",
            status = status,
            verifiedBy = if (status == CertificationStatus.VERIFIED) "admin_001" else null,
            verifiedAt = if (status == CertificationStatus.VERIFIED) now else null,
            rejectionReason = null,
            ocrConfidence = Random.nextDouble(0.85, 0.99),
            createdAt = now,
            updatedAt = now,
            certificationType = certificationType
        )
    }

    fun createTestCertificationType(
        code: String,
        name: String = code.replace("_", " ").split(" ")
            .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } },
        category: String = "safety_training",
        typicalDurationMonths: Int? = when (code) {
            "OSHA_10", "OSHA_30" -> 60
            "CPR", "FIRST_AID" -> 24
            "FORKLIFT" -> 36
            else -> 36
        }
    ): CertificationType {
        return CertificationType(
            id = "cert_type_${code.lowercase()}",
            code = code,
            name = name,
            category = category,
            region = "US",
            typicalDurationMonths = typicalDurationMonths,
            renewalRequired = typicalDurationMonths != null,
            description = "Standard $name certification"
        )
    }

    // ===== Crew Data =====

    fun createTestCrew(
        id: String = "crew_${randomId()}",
        companyId: String,
        projectId: String? = null,
        name: String = "Test Crew ${Random.nextInt(1, 100)}",
        crewType: CrewType = CrewType.PROJECT_BASED,
        memberCount: Int = 5,
        foremanId: String? = null
    ): Crew {
        val now = Clock.System.now().toString()
        
        // Create members
        val members = (1..memberCount).map { index ->
            val worker = createTestCompanyWorker(
                companyId = companyId,
                role = if (index == 1) WorkerRole.FOREMAN else WorkerRole.LABORER
            )
            
            CrewMember(
                id = "crew_member_${randomId()}",
                crewId = id,
                companyWorkerId = worker.id,
                role = if (index == 1) CrewMemberRole.FOREMAN else CrewMemberRole.MEMBER,
                startDate = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(Random.nextInt(1, 30).days),
                endDate = null,
                status = "active",
                worker = worker
            )
        }

        val foreman = members.firstOrNull { it.role == CrewMemberRole.FOREMAN }?.worker

        return Crew(
            id = id,
            companyId = companyId,
            projectId = projectId,
            name = name,
            crewType = crewType,
            trade = randomTrade(),
            foremanId = foremanId ?: foreman?.id,
            location = randomLocation(),
            status = CrewStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
            members = members,
            foreman = foreman
        )
    }

    // ===== Helper Functions =====

    private fun randomId(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    private val firstNames = listOf(
        "John", "Jane", "Michael", "Sarah", "David", "Emily",
        "James", "Maria", "Robert", "Linda", "William", "Patricia"
    )

    private val lastNames = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia",
        "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez"
    )

    private fun randomFirstName() = firstNames.random()
    private fun randomLastName() = lastNames.random()

    private fun randomDateOfBirth(): LocalDate {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val age = Random.nextInt(22, 65) // Workers aged 22-65
        return today.minus((age * 365).days)
    }

    private fun generateCertNumber(certType: String): String {
        return when (certType) {
            "OSHA_10", "OSHA_30" -> "OSHA-${Random.nextInt(10000000, 99999999)}"
            "CPR" -> "CPR-${Random.nextInt(100000, 999999)}"
            "FORKLIFT" -> "FL-${Random.nextInt(1000, 9999)}"
            else -> "${certType}-${Random.nextInt(10000, 99999)}"
        }
    }

    private val trades = listOf(
        "Framing", "Electrical", "Plumbing", "HVAC", "Concrete",
        "Masonry", "Roofing", "Drywall", "Painting", "Flooring"
    )

    private fun randomTrade() = trades.random()

    private val locations = listOf(
        "Floor 1", "Floor 2", "Floor 3", "Basement",
        "Building A", "Building B", "East Wing", "West Wing"
    )

    private fun randomLocation() = locations.random()

    // ===== Batch Creation Helpers =====

    /**
     * Create a complete test company with workers, crews, and certifications
     */
    fun createTestCompanyEnvironment(
        workerCount: Int = 50,
        crewCount: Int = 5,
        certificationRatio: Double = 0.8 // 80% of workers have certifications
    ): TestCompanyEnvironment {
        val company = createTestCompany()
        val project = createTestProject(companyId = company.id)

        // Create workers
        val workers = (1..workerCount).map {
            val profile = createTestWorkerProfile()
            val hasCerts = Random.nextDouble() < certificationRatio
            
            val certifications = if (hasCerts) {
                listOf(
                    createTestCertification(
                        workerProfileId = profile.id,
                        companyId = company.id,
                        certificationType = createTestCertificationType("OSHA_10")
                    )
                )
            } else {
                emptyList()
            }

            createTestCompanyWorker(
                companyId = company.id,
                workerProfile = profile,
                certifications = certifications
            )
        }

        // Create crews
        val crews = (1..crewCount).map {
            createTestCrew(
                companyId = company.id,
                projectId = project.id,
                memberCount = workerCount / crewCount
            )
        }

        return TestCompanyEnvironment(
            company = company,
            project = project,
            workers = workers,
            crews = crews
        )
    }
}

/**
 * Complete test environment for integration tests
 */
data class TestCompanyEnvironment(
    val company: Company,
    val project: Project,
    val workers: List<CompanyWorker>,
    val crews: List<Crew>
)
```

---

## Mock Services

### Mock Service Implementations

**File**: `shared/src/commonTest/kotlin/com/hazardhawk/test/MockServices.kt`

```kotlin
package com.hazardhawk.test

import com.hazardhawk.domain.repositories.OCRExtractedData
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Mock S3 client for file upload testing
 */
class MockS3Client {
    private val uploadedFiles = mutableMapOf<String, ByteArray>()
    private val uploadLatency: Duration = 500.milliseconds

    suspend fun uploadFile(
        data: ByteArray,
        path: String,
        mimeType: String
    ): String {
        delay(uploadLatency) // Simulate network latency
        
        uploadedFiles[path] = data
        return "s3://test-bucket/$path"
    }

    suspend fun generateThumbnail(s3Url: String): String {
        delay(100.milliseconds)
        return "${s3Url.removeSuffix(".pdf")}_thumb.jpg"
    }

    fun getUploadedFile(path: String): ByteArray? {
        return uploadedFiles[path]
    }

    fun clear() {
        uploadedFiles.clear()
    }
}

/**
 * Mock OCR processor for certification document testing
 */
class MockOCRProcessor {
    private val processingLatency: Duration = 2.seconds

    suspend fun processDocument(
        s3Url: String,
        mimeType: String
    ): OCRExtractedData {
        delay(processingLatency) // Simulate OCR processing time

        // Return mock extracted data based on filename
        return when {
            s3Url.contains("osha10") -> osha10ExtractedData()
            s3Url.contains("osha30") -> osha30ExtractedData()
            s3Url.contains("cpr") -> cprExtractedData()
            s3Url.contains("forklift") -> forkliftExtractedData()
            s3Url.contains("corrupt") -> throw Exception("OCR processing failed")
            else -> defaultExtractedData()
        }
    }

    private fun osha10ExtractedData() = OCRExtractedData(
        rawText = "OSHA 10-Hour Construction Safety and Health\nJohn Doe\n12345678\n05/15/2024",
        holderName = "John Doe",
        certificationType = "OSHA_10",
        certificationNumber = "12345678",
        issueDate = LocalDate(2024, 5, 15),
        expirationDate = LocalDate(2029, 5, 15),
        issuingAuthority = "OSHA Training Institute",
        confidence = 0.92,
        fieldConfidences = mapOf(
            "holder_name" to 0.95,
            "cert_type" to 0.98,
            "cert_number" to 0.88,
            "issue_date" to 0.90,
            "expiration_date" to 0.92
        )
    )

    private fun osha30ExtractedData() = OCRExtractedData(
        rawText = "OSHA 30-Hour Construction Safety and Health\nJane Smith\n87654321\n01/10/2024",
        holderName = "Jane Smith",
        certificationType = "OSHA_30",
        certificationNumber = "87654321",
        issueDate = LocalDate(2024, 1, 10),
        expirationDate = LocalDate(2029, 1, 10),
        issuingAuthority = "OSHA Training Institute",
        confidence = 0.94,
        fieldConfidences = mapOf(
            "holder_name" to 0.96,
            "cert_type" to 0.99,
            "cert_number" to 0.91
        )
    )

    private fun cprExtractedData() = OCRExtractedData(
        rawText = "CPR/AED Certification\nMike Johnson\nCPR-456789\n03/20/2024",
        holderName = "Mike Johnson",
        certificationType = "CPR",
        certificationNumber = "CPR-456789",
        issueDate = LocalDate(2024, 3, 20),
        expirationDate = LocalDate(2026, 3, 20), // 2-year validity
        issuingAuthority = "American Red Cross",
        confidence = 0.88,
        fieldConfidences = mapOf(
            "holder_name" to 0.90,
            "cert_type" to 0.95,
            "cert_number" to 0.82
        )
    )

    private fun forkliftExtractedData() = OCRExtractedData(
        rawText = "Forklift Operator Certification\nSarah Davis\nFL-5678\n06/01/2024",
        holderName = "Sarah Davis",
        certificationType = "FORKLIFT",
        certificationNumber = "FL-5678",
        issueDate = LocalDate(2024, 6, 1),
        expirationDate = LocalDate(2027, 6, 1), // 3-year validity
        issuingAuthority = "National Safety Council",
        confidence = 0.90,
        fieldConfidences = mapOf(
            "holder_name" to 0.93,
            "cert_type" to 0.96,
            "cert_number" to 0.85
        )
    )

    private fun defaultExtractedData() = OCRExtractedData(
        rawText = "Generic Certification",
        holderName = "Test Worker",
        certificationType = "UNKNOWN",
        certificationNumber = null,
        issueDate = null,
        expirationDate = null,
        issuingAuthority = null,
        confidence = 0.50,
        fieldConfidences = emptyMap()
    )
}

/**
 * Mock email service for notification testing
 */
class MockEmailService {
    private val sentEmails = mutableListOf<SentEmail>()

    suspend fun sendEmail(
        to: String,
        subject: String,
        body: String,
        htmlBody: String? = null
    ): Boolean {
        delay(100.milliseconds) // Simulate network latency
        
        sentEmails.add(SentEmail(
            to = to,
            subject = subject,
            body = body,
            htmlBody = htmlBody,
            sentAt = Clock.System.now().toString()
        ))
        
        return true
    }

    fun wasSent(email: String): Boolean {
        return sentEmails.any { it.to == email }
    }

    fun getLatestEmail(email: String): String? {
        return sentEmails.lastOrNull { it.to == email }?.body
    }

    fun getSentEmails(email: String): List<SentEmail> {
        return sentEmails.filter { it.to == email }
    }

    fun clear() {
        sentEmails.clear()
    }

    data class SentEmail(
        val to: String,
        val subject: String,
        val body: String,
        val htmlBody: String?,
        val sentAt: String
    )
}

/**
 * Mock SMS service for notification testing
 */
class MockSMSService {
    private val sentSMS = mutableListOf<SentSMS>()

    suspend fun sendSMS(
        to: String,
        message: String
    ): Boolean {
        delay(150.milliseconds) // Simulate network latency
        
        sentSMS.add(SentSMS(
            to = to,
            message = message,
            sentAt = Clock.System.now().toString()
        ))
        
        return true
    }

    fun wasSent(phone: String): Boolean {
        return sentSMS.any { it.to == phone }
    }

    fun getLatestSMS(phone: String): String? {
        return sentSMS.lastOrNull { it.to == phone }?.message
    }

    fun getSentSMS(phone: String): List<SentSMS> {
        return sentSMS.filter { it.to == phone }
    }

    fun clear() {
        sentSMS.clear()
    }

    data class SentSMS(
        val to: String,
        val message: String,
        val sentAt: String
    )
}

/**
 * Mock WebSocket client for real-time update testing
 */
class MockWebSocketClient {
    private val messages = mutableListOf<WebSocketMessage>()
    private var connected = false

    fun connect(url: String, authToken: String?): Boolean {
        return if (authToken != null && authToken.isNotBlank()) {
            connected = true
            true
        } else {
            false
        }
    }

    fun subscribe(channel: String) {
        if (!connected) throw IllegalStateException("Not connected")
        // Subscription logic
    }

    fun sendMessage(message: String) {
        if (!connected) throw IllegalStateException("Not connected")
        
        messages.add(WebSocketMessage(
            message = message,
            direction = MessageDirection.OUTGOING,
            timestamp = Clock.System.now().toString()
        ))
    }

    suspend fun receiveNextMessage(timeout: Duration): String? {
        if (!connected) return null
        
        // Simulate receiving message (in real tests, would wait for actual message)
        delay(timeout)
        return messages.lastOrNull { it.direction == MessageDirection.INCOMING }?.message
    }

    fun simulateIncomingMessage(message: String) {
        messages.add(WebSocketMessage(
            message = message,
            direction = MessageDirection.INCOMING,
            timestamp = Clock.System.now().toString()
        ))
    }

    fun disconnect() {
        connected = false
    }

    data class WebSocketMessage(
        val message: String,
        val direction: MessageDirection,
        val timestamp: String
    )

    enum class MessageDirection {
        INCOMING, OUTGOING
    }
}
```


---

## Acceptance Criteria

### Phase 2: Certification Management

#### Unit Test Acceptance Criteria
- ✅ OCR extraction logic tested with >30 scenarios (happy path, edge cases, errors)
- ✅ >85% accuracy on 100-sample test dataset
- ✅ All certification CRUD operations tested (create, read, update, delete)
- ✅ Verification workflow fully tested (approve/reject paths)
- ✅ Expiration tracking logic 100% covered
- ✅ Batch operations handle edge cases (empty lists, duplicates, errors)
- ✅ 95%+ code coverage on CertificationRepository

#### Integration Test Acceptance Criteria
- ✅ Upload → OCR → Pre-fill flow completes in <5s
- ✅ Admin verification workflow tested end-to-end (pending → approved/rejected)
- ✅ Expiration alerts sent within 1 hour of scheduled time
- ✅ Worker notification delivery confirmed (email + SMS)
- ✅ Multi-certification upload tested (bulk scenario)

#### Performance Criteria
- ✅ Certification upload API <5s (p95, including OCR processing)
- ✅ Get pending certifications API <100ms (p95)
- ✅ List worker certifications <50ms (p95)
- ✅ Expiration check background job completes in <30s for 1000 certs
- ✅ No memory leaks during 1000-cert processing

#### Security Criteria
- ✅ File upload validates MIME types (reject .exe, .php, .sh, etc.)
- ✅ File upload validates size limit (<10MB enforced)
- ✅ Uploaded files scanned for malware (EICAR test virus detected)
- ✅ Document URLs use signed S3 URLs with 1-hour expiration
- ✅ Only admins can approve/reject certifications (permission tested)
- ✅ Row-level security prevents cross-company data access

---

### Phase 3: Worker Onboarding

#### Unit Test Acceptance Criteria
- ✅ Magic link tokens are cryptographically secure (256-bit entropy minimum)
- ✅ Token uniqueness verified (no collisions in 1000 token test)
- ✅ Token expiration after 24 hours enforced
- ✅ One-time use of tokens verified (second use fails)
- ✅ Multi-step form data persistence tested (session state)
- ✅ Step validation logic tested (cannot skip steps)
- ✅ E-signature capture and storage tested

#### Integration Test Acceptance Criteria
- ✅ Complete onboarding flow tested (all 5 steps: basic info, photo ID, selfie, certs, signature)
- ✅ Magic link email/SMS delivery confirmed
- ✅ Photo ID OCR extraction tested (name, DOB extraction)
- ✅ E-signature capture and non-repudiation metadata verified
- ✅ Admin approval workflow tested (pending → approved/rejected)
- ✅ Worker notified upon approval/rejection

#### Performance Criteria
- ✅ Magic link generation <2s (p95)
- ✅ Each onboarding step saves in <200ms (p95)
- ✅ Complete onboarding flow takes <5 minutes (UX target)
- ✅ Email delivery <5s (p95)
- ✅ SMS delivery <10s (p95)

#### Security Criteria
- ✅ Magic link tokens stored as hashes (bcrypt with cost factor 10+)
- ✅ Rate limiting prevents token generation abuse (max 5 per email per hour)
- ✅ E-signatures stored with non-repudiation metadata (timestamp, IP, user-agent)
- ✅ Session data encrypted in database
- ✅ Magic link verification logs failed attempts for security monitoring
- ✅ SQL injection prevention tested (parameterized queries)

---

### Phase 4: Crew Assignment

#### Unit Test Acceptance Criteria
- ✅ Crew CRUD operations 90%+ coverage (create, read, update, delete)
- ✅ Member assignment/removal logic tested (add, remove, bulk operations)
- ✅ Roster generation logic tested with various crew sizes (1, 10, 50, 100 members)
- ✅ Foreman selection logic tested (default vs. custom foreman)
- ✅ Crew filtering and search tested
- ✅ Historical crew assignment tracking tested

#### Integration Test Acceptance Criteria
- ✅ Drag-and-drop crew assignment tested (simulated touch/mouse events)
- ✅ Real-time crew updates via WebSocket verified (multi-client)
- ✅ PDF roster generation tested end-to-end (create → download)
- ✅ Roster includes correct data (names, roles, certifications)
- ✅ Crew history audit trail tested (member adds/removes tracked)

#### Performance Criteria
- ✅ Drag-and-drop UI latency <50ms (p95)
- ✅ List crews API <100ms for 50 crews (p95)
- ✅ Get crew members API <50ms for 50-member crew (p95)
- ✅ PDF roster generation <2s for 50-member crew (p95)
- ✅ WebSocket updates propagate to all clients <500ms (p95)

#### Security Criteria
- ✅ Only authorized users can modify crew assignments (permission tested)
- ✅ PDF rosters do not leak PII (SSN, DOB excluded)
- ✅ WebSocket channels authenticated per user (token required)
- ✅ WebSocket channels isolated by company (cross-tenant access prevented)
- ✅ Crew member data access restricted by company (RLS tested)

---

## Test Execution Summary

### Daily Development Workflow

1. **Pre-commit** (Local)
   - Run unit tests for changed files
   - Run ktlint formatter
   - Run detekt static analysis
   - Estimated time: 30-60 seconds

2. **Pull Request** (CI/CD)
   - Run all unit tests (shared module)
   - Run Android instrumentation tests
   - Run performance benchmarks
   - Run security tests
   - Check code coverage (fail if <90%)
   - Estimated time: 15-25 minutes

3. **Merge to Main** (CI/CD)
   - Full test suite (unit + integration + E2E)
   - Load testing (k6)
   - Security scan
   - Generate test reports
   - Publish coverage to Codecov
   - Estimated time: 30-45 minutes

### Test Metrics Dashboard

Track these metrics in CI/CD:

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| Unit Test Coverage | 90%+ | <85% fails build |
| Integration Test Coverage | 80%+ | <75% fails build |
| Test Execution Time (Unit) | <5s | >10s warning |
| Test Execution Time (Integration) | <30s | >60s warning |
| Flaky Test Rate | <1% | >5% alert |
| Bug Escape Rate | <2% | >5% alert |
| Mean Time To Detect (MTTD) | <24h | >48h alert |
| API Response Time (p95) | <200ms | >500ms alert |

---

## Quick Reference: Test File Locations

### Unit Tests (Shared Module)

```
shared/src/commonTest/kotlin/com/hazardhawk/
├── domain/
│   ├── repositories/
│   │   ├── WorkerRepositoryTest.kt          # Worker CRUD tests (50+ tests)
│   │   ├── CrewRepositoryTest.kt            # Crew CRUD tests (40+ tests)
│   │   ├── CertificationRepositoryTest.kt   # Certification tests (60+ tests)
│   │   ├── CompanyRepositoryTest.kt         # Company management tests
│   │   └── ProjectRepositoryTest.kt         # Project management tests
│   ├── ocr/
│   │   └── CertificationOCRProcessorTest.kt # OCR extraction tests (30+ tests)
│   └── auth/
│       ├── MagicLinkServiceTest.kt          # Magic link tests (20+ tests)
│       └── OnboardingSessionManagerTest.kt  # Onboarding flow tests (15+ tests)
├── security/
│   └── CrewManagementSecurityTest.kt        # Security tests (25+ tests)
└── test/
    ├── TestDataFactory.kt                    # Test data generation helpers
    └── MockServices.kt                       # Mock S3, OCR, Email, SMS, WebSocket
```

### Integration Tests (Android)

```
androidApp/src/androidTest/kotlin/com/hazardhawk/
├── certification/
│   ├── CertificationUploadE2ETest.kt        # Upload → verification flow (10+ tests)
│   └── CertificationExpirationFlowTest.kt   # Expiration alerts flow (8+ tests)
├── onboarding/
│   ├── WorkerOnboardingE2ETest.kt           # Complete onboarding workflow (8+ tests)
│   └── MagicLinkDeliveryTest.kt             # Email/SMS delivery tests (6+ tests)
├── crew/
│   ├── CrewAssignmentE2ETest.kt             # Drag-and-drop workflow (10+ tests)
│   └── RosterGenerationE2ETest.kt           # PDF generation flow (8+ tests)
└── benchmark/
    └── CrewManagementBenchmark.kt            # Performance benchmarks (10+ tests)
```

---

## Next Steps

### Immediate Actions (This Sprint)

1. **Create test template files** based on this strategy
   - Copy unit test templates to `shared/src/commonTest`
   - Copy integration test templates to `androidApp/src/androidTest`
   - Create `TestDataFactory.kt` and `MockServices.kt`

2. **Set up CI/CD pipeline** with GitHub Actions workflow
   - Configure test execution on PR
   - Set up code coverage reporting (Codecov)
   - Configure performance regression detection

3. **Establish baseline metrics**
   - Run initial test suite to get baseline coverage
   - Run performance benchmarks to establish targets
   - Document current state for comparison

### Phase 2 Implementation (Weeks 1-2)

- [ ] Implement OCR extraction tests (30+ tests)
- [ ] Implement certification repository tests (60+ tests)
- [ ] Create integration tests for upload → verification flow
- [ ] Set up mock OCR service
- [ ] Add performance benchmarks for certification upload

### Phase 3 Implementation (Week 2)

- [ ] Implement magic link service tests (20+ tests)
- [ ] Implement onboarding session manager tests (15+ tests)
- [ ] Create E2E onboarding workflow tests
- [ ] Set up mock email/SMS services
- [ ] Add security tests for token generation

### Phase 4 Implementation (Week 3)

- [ ] Implement crew repository tests (40+ tests)
- [ ] Implement roster generator tests (18+ tests)
- [ ] Create drag-and-drop integration tests
- [ ] Set up mock WebSocket service
- [ ] Add performance benchmarks for real-time updates

### Continuous Improvement

- **Weekly**: Review test metrics and address flaky tests
- **Bi-weekly**: Review code coverage and add tests for gaps
- **Monthly**: Update test data factory with new scenarios
- **Quarterly**: Review and update testing strategy based on lessons learned

---

## Resources and References

### Testing Libraries

- **kotlin.test**: Core testing framework (multiplatform)
- **kotlinx.coroutines.test**: Coroutine testing utilities
- **MockK**: Mocking framework for Kotlin
- **Compose Test**: UI testing for Jetpack Compose
- **AndroidX Benchmark**: Performance benchmarking

### Documentation

- [Kotlin Multiplatform Testing](https://kotlinlang.org/docs/multiplatform-run-tests.html)
- [Jetpack Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [AndroidX Benchmark Guide](https://developer.android.com/studio/profile/benchmark)
- [MockK Documentation](https://mockk.io/)

### Performance Tools

- **k6**: Load testing tool for APIs
- **Android Profiler**: CPU, memory, network profiling
- **LeakCanary**: Memory leak detection

### Security Tools

- **OWASP ZAP**: Security vulnerability scanning
- **SonarQube**: Code quality and security analysis
- **Dependabot**: Dependency vulnerability scanning

---

**Document Version**: 1.0  
**Last Updated**: October 8, 2025  
**Next Review**: End of Phase 2 (Week 2)  
**Maintained By**: Quality Assurance Team

---

## Appendix: Sample Test Output

### Successful Test Run Example

```
> Task :shared:test
com.hazardhawk.domain.repositories.WorkerRepositoryTest
  ✓ createWorker should insert worker and return ID (45ms)
  ✓ createWorker with duplicate employee number should fail (32ms)
  ✓ getWorkers should filter by status (58ms)
  ✓ searchWorkers should return matching results (41ms)
  ... (50 tests passed)

com.hazardhawk.domain.repositories.CertificationRepositoryTest
  ✓ createCertification successfully creates new certification (38ms)
  ✓ approveCertification updates status to VERIFIED (29ms)
  ✓ getExpiringCertifications returns certs expiring within specified days (52ms)
  ... (60 tests passed)

com.hazardhawk.security.CrewManagementSecurityTest
  ✓ magic link tokens are cryptographically secure (125ms)
  ✓ certification upload validates MIME types (18ms)
  ✓ WebSocket connections require authentication (21ms)
  ... (25 tests passed)

Test Summary:
  Total: 265 tests
  Passed: 265
  Failed: 0
  Skipped: 0
  Duration: 8.3s

Coverage Report:
  Line Coverage: 94.2%
  Branch Coverage: 91.8%
  Function Coverage: 96.1%
```

### Failed Test Example (What to Fix)

```
com.hazardhawk.domain.repositories.CertificationRepositoryTest
  ✗ approveCertification updates status to VERIFIED (42ms)
    
    Expected: CertificationStatus.VERIFIED
    Actual: CertificationStatus.PENDING_VERIFICATION
    
    at CertificationRepositoryTest.kt:89
    
    Context:
      - Certification ID: cert_001
      - Verified By: admin_001
      - Database transaction may have rolled back
    
    Recommended Fix:
      Check that database commit() is called after status update.
      Verify that transaction isolation level allows status changes.
```

---

**END OF TESTING STRATEGY DOCUMENT**

Total Lines: ~1400
Estimated Reading Time: 45 minutes
Estimated Implementation Time: 3-4 weeks (Phases 2-4)
