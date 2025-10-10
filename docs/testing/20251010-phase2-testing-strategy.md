# Phase 2 Build Error Fixes - Comprehensive Testing Strategy

**Document ID:** TEST-STRATEGY-PHASE2-20251010
**Created:** 2025-10-10
**Status:** Ready for Implementation
**Priority:** P0 - Critical

---

## Executive Summary

This document provides a comprehensive testing strategy for Phase 2 build error fixes affecting ~282 compilation errors across three critical areas:

1. **Performance Monitoring Models** (~180 errors, 64%)
2. **PTP Generator** (68 errors, 24%)
3. **Photo Repository** (34 errors, 12%)

### Key Testing Objectives

- **Zero Regression:** Ensure existing functionality remains intact
- **Type Safety:** Validate all model changes preserve type contracts
- **Performance:** Verify no performance degradation
- **Integration:** Test cross-module compatibility
- **Documentation:** Generate comprehensive test reports

### Success Criteria

| Metric | Target | Measurement |
|--------|--------|-------------|
| Compilation Success | 100% | All modules compile without errors |
| Test Pass Rate | ≥98% | All existing tests pass |
| New Test Coverage | ≥85% | Coverage for fixed code |
| Performance | ±10% | No significant degradation |
| Build Time | ≤5min | Full project build completes |

---

## Table of Contents

1. [Test Strategy Overview](#test-strategy-overview)
2. [Area 1: Performance Monitoring Models](#area-1-performance-monitoring-models)
3. [Area 2: PTP Generator](#area-2-ptp-generator)
4. [Area 3: Photo Repository](#area-3-photo-repository)
5. [Integration Testing](#integration-testing)
6. [Performance Testing](#performance-testing)
7. [Test Execution Plan](#test-execution-plan)
8. [Acceptance Criteria](#acceptance-criteria)
9. [Risk Mitigation](#risk-mitigation)

---

## Test Strategy Overview

### Testing Pyramid for Phase 2

```
         /\
        /E2E\      10% - End-to-end workflow tests (3 tests)
       /------\
      /  INT   \    30% - Integration tests (9 tests)
     /----------\
    /   UNIT     \  60% - Unit tests (18 tests)
   /--------------\
```

**Total Tests Required:** 30 tests
**Estimated Test Development Time:** 8-12 hours
**Test Execution Time:** 15-20 minutes (full suite)

### Test Categories

1. **Compilation Tests** (3 tests)
   - Metadata compilation
   - iOS platform compilation  
   - Android platform compilation

2. **Unit Tests** (18 tests)
   - Model constructor tests
   - Default parameter validation
   - Serialization/deserialization
   - Repository method tests

3. **Integration Tests** (9 tests)
   - Cross-module interactions
   - Data flow validation
   - Error propagation

4. **Regression Tests** (Subset of existing)
   - Existing test suite execution
   - Behavioral equivalence checks

5. **Performance Tests** (3 tests)
   - Throughput benchmarks
   - Memory usage validation
   - Response time checks

---

## Area 1: Performance Monitoring Models

### Problem Analysis

**Error Count:** ~180 errors (64% of total)
**Root Cause:** Missing default parameters in complex data models
**Files Affected:**
- `/Users/aaron/Apps-Coded/HH-v0-fresh/shared/src/commonMain/kotlin/com/hazardhawk/monitoring/PerformanceDashboard.kt` (695 lines)
- `/Users/aaron/Apps-Coded/HH-v0-fresh/shared/src/commonMain/kotlin/com/hazardhawk/monitoring/ProductionMonitoringSystem.kt` (1384 lines)

### Critical Models Requiring Fixes

```kotlin
// Models with default parameter issues
@Serializable
data class DashboardSnapshot(
    val timestamp: Long,
    val safetyMetrics: SafetyDashboardMetrics,
    val performanceMetrics: PerformanceDashboardMetrics,
    val systemHealthMetrics: SystemHealthDashboardMetrics,
    val photoProcessingMetrics: PhotoProcessingDashboardMetrics,
    val alertSummary: AlertDashboardSummary,
    val chartData: DashboardChartData
) // Missing default parameters for optional fields

@Serializable
data class SafetyDashboardMetrics(
    val totalActiveAlerts: Int,
    val criticalAlerts: Int,
    val alertsLast24Hours: Int,
    val complianceEventsLast24Hours: Int,
    val complianceRate: Double,
    val alertsByType: Map<String, Int>,
    val severityDistribution: Map<String, Int>
) // Missing defaults for maps

@Serializable
data class SystemHealthReport(
    val timestamp: Long,
    val overallStatus: SystemHealthStatus,
    val componentHealth: Map<String, ComponentHealthResult>,
    val systemUptime: Long,
    val criticalIssues: List<CriticalIssue>,
    val performanceSummary: PerformanceSummary?,
    val resourceUtilization: ResourceUtilizationReport?,
    val activeAlerts: List<SystemAlert>,
    val recommendations: List<String>
) // Nullable fields need explicit defaults
```

### Test Plan: Performance Monitoring Models

#### 1.1 Compilation Tests

**Test ID:** PERF-COMP-001
**Objective:** Verify metadata compilation succeeds after fixes
```bash
./gradlew :shared:compileKotlinMetadata
```
**Expected:** BUILD SUCCESSFUL in < 2 minutes
**Priority:** P0

---

**Test ID:** PERF-COMP-002  
**Objective:** Verify Android platform compilation
```bash
./gradlew :shared:compileDebugKotlinAndroid
```
**Expected:** BUILD SUCCESSFUL with no errors
**Priority:** P0

---

**Test ID:** PERF-COMP-003
**Objective:** Verify iOS platform compilation
```bash
./gradlew :shared:compileKotlinIosSimulatorArm64
```
**Expected:** BUILD SUCCESSFUL with no errors
**Priority:** P0

#### 1.2 Unit Tests

**Test ID:** PERF-UNIT-001
**Test Name:** `should construct DashboardSnapshot with all parameters`
**File:** `PerformanceDashboardTest.kt`

```kotlin
@Test
fun `should construct DashboardSnapshot with all parameters`() {
    // Given
    val timestamp = Clock.System.now().toEpochMilliseconds()
    val safetyMetrics = SafetyDashboardMetrics(
        totalActiveAlerts = 5,
        criticalAlerts = 2,
        alertsLast24Hours = 12,
        complianceEventsLast24Hours = 45,
        complianceRate = 95.5,
        alertsByType = mapOf("FALL_PROTECTION" to 3, "PPE_VIOLATION" to 2),
        severityDistribution = mapOf("HIGH" to 2, "MEDIUM" to 3)
    )
    // ... create other metrics
    
    // When
    val snapshot = DashboardSnapshot(
        timestamp = timestamp,
        safetyMetrics = safetyMetrics,
        performanceMetrics = performanceMetrics,
        systemHealthMetrics = systemHealthMetrics,
        photoProcessingMetrics = photoMetrics,
        alertSummary = alertSummary,
        chartData = chartData
    )
    
    // Then
    assertEquals(timestamp, snapshot.timestamp)
    assertEquals(5, snapshot.safetyMetrics.totalActiveAlerts)
    assertNotNull(snapshot.chartData)
}
```
**Priority:** P0

---

**Test ID:** PERF-UNIT-002
**Test Name:** `should handle default parameter values for optional fields`

```kotlin
@Test
fun `should handle default parameter values for optional fields`() {
    // Given
    val snapshot = DashboardSnapshot(
        timestamp = Clock.System.now().toEpochMilliseconds(),
        safetyMetrics = createMinimalSafetyMetrics(),
        performanceMetrics = createMinimalPerformanceMetrics(),
        systemHealthMetrics = createMinimalHealthMetrics(),
        photoProcessingMetrics = createMinimalPhotoMetrics(),
        alertSummary = createMinimalAlertSummary()
        // chartData should use default if optional
    )
    
    // Then
    assertNotNull(snapshot)
    // Validate defaults are applied correctly
}
```
**Priority:** P1

---

**Test ID:** PERF-UNIT-003
**Test Name:** `should serialize and deserialize DashboardSnapshot correctly`

```kotlin
@Test
fun `should serialize and deserialize DashboardSnapshot correctly`() = runTest {
    // Given
    val original = createSampleDashboardSnapshot()
    
    // When
    val json = Json.encodeToString(DashboardSnapshot.serializer(), original)
    val deserialized = Json.decodeFromString(DashboardSnapshot.serializer(), json)
    
    // Then
    assertEquals(original.timestamp, deserialized.timestamp)
    assertEquals(original.safetyMetrics.totalActiveAlerts, 
                 deserialized.safetyMetrics.totalActiveAlerts)
    assertEquals(original.performanceMetrics.averageResponseTimeMs,
                 deserialized.performanceMetrics.averageResponseTimeMs)
}
```
**Priority:** P0

---

**Test ID:** PERF-UNIT-004
**Test Name:** `should calculate performance metrics correctly`

```kotlin
@Test
fun `should calculate performance metrics correctly`() = runTest {
    // Given
    val dashboard = PerformanceDashboard(mockMonitoring, mockAIOptimizer)
    
    // When
    dashboard.updateMetrics(
        responseTime = 1500L,
        workType = WorkType.GENERAL_CONSTRUCTION,
        success = true,
        errorMessage = null
    )
    
    val snapshot = dashboard.generateDashboardSnapshot()
    
    // Then
    assertTrue(snapshot.performanceMetrics.averageResponseTimeMs > 0)
    assertTrue(snapshot.performanceMetrics.errorRate >= 0.0)
    assertTrue(snapshot.performanceMetrics.successRate <= 100.0)
}
```
**Priority:** P1

---

**Test ID:** PERF-UNIT-005
**Test Name:** `should trigger alerts when thresholds exceeded`

```kotlin
@Test
fun `should trigger alerts when thresholds exceeded`() = runTest {
    // Given
    val dashboard = PerformanceDashboard(mockMonitoring, mockAIOptimizer)
    val alerts = mutableListOf<PerformanceAlert>()
    
    // Collect alerts
    val job = launch {
        dashboard.performanceAlerts.collect { alert ->
            alerts.add(alert)
        }
    }
    
    // When - Trigger high response time
    dashboard.updateMetrics(
        responseTime = 5000L, // Exceeds threshold
        workType = WorkType.ELECTRICAL,
        success = true
    )
    
    delay(100) // Allow alert propagation
    
    // Then
    assertTrue(alerts.isNotEmpty(), "Should trigger performance alert")
    assertTrue(alerts.any { it.type == AlertType.PERFORMANCE_DEGRADATION })
    
    job.cancel()
}
```
**Priority:** P0

---

**Test ID:** PERF-UNIT-006
**Test Name:** `should maintain metrics history within size limits`

```kotlin
@Test
fun `should maintain metrics history within size limits`() = runTest {
    // Given
    val dashboard = PerformanceDashboard(mockMonitoring, mockAIOptimizer)
    val maxHistorySize = 288 // 24 hours at 5-minute intervals
    
    // When - Generate more metrics than history size
    repeat(maxHistorySize + 50) { index ->
        dashboard.updateMetrics(
            responseTime = 1000L,
            workType = WorkType.GENERAL_CONSTRUCTION,
            success = true
        )
    }
    
    // Then - Verify history doesn't exceed limit
    // (Would need to expose history size or verify through behavior)
    // Metrics should still be accurate
    val snapshot = dashboard.generateDashboardSnapshot()
    assertTrue(snapshot.performanceMetrics.throughputPerMinute > 0)
}
```
**Priority:** P2

#### 1.3 Integration Tests

**Test ID:** PERF-INT-001
**Test Name:** `should integrate with RealTimeMonitoringSystem`

```kotlin
@Test
fun `should integrate with RealTimeMonitoringSystem`() = runTest {
    // Given
    val monitoring = RealTimeMonitoringSystem(mockAuditLogger)
    val aiOptimizer = AIPerformanceOptimizer()
    val dashboard = PerformanceDashboard(monitoring, aiOptimizer)
    
    // When
    val initResult = dashboard.initializeDashboard()
    
    // Then
    assertTrue(initResult.success)
    assertNotNull(initResult.initialSnapshot)
    assertTrue(initResult.enabledFeatures.contains("Real-time safety alerts"))
}
```
**Priority:** P0

---

**Test ID:** PERF-INT-002
**Test Name:** `should propagate alerts through monitoring system`

```kotlin
@Test
fun `should propagate alerts through monitoring system`() = runTest {
    // Given
    val productionMonitoring = ProductionMonitoringSystem(
        realtimeMonitoring, dashboard, aiModelManager, 
        batchProcessor, auditLogger
    )
    
    // When
    productionMonitoring.triggerAlert(
        alertType = AlertType.PERFORMANCE_DEGRADATION,
        severity = Severity.HIGH,
        component = "AI_MODELS",
        message = "High inference time detected"
    )
    
    // Then
    val healthReport = productionMonitoring.performHealthCheck()
    assertTrue(healthReport.activeAlerts.isNotEmpty())
}
```
**Priority:** P1

---

**Test ID:** PERF-INT-003
**Test Name:** `should generate SLA compliance reports`

```kotlin
@Test
fun `should generate SLA compliance reports`() = runTest {
    // Given
    val monitoring = ProductionMonitoringSystem(
        realtimeMonitoring, dashboard, aiModelManager,
        batchProcessor, auditLogger
    )
    
    // Simulate system activity
    monitoring.initializeMonitoring()
    delay(100)
    
    // When
    val slaReport = monitoring.generateSLAReport(periodHours = 24)
    
    // Then
    assertNotNull(slaReport)
    assertTrue(slaReport.actualMetrics.uptime >= 0.0)
    assertTrue(slaReport.actualMetrics.availability >= 0.0)
    assertNotNull(slaReport.compliance)
}
```
**Priority:** P1

#### 1.4 Mock Data Requirements

**Factory Functions Needed:**

```kotlin
object PerformanceTestDataFactory {
    
    fun createMinimalSafetyMetrics() = SafetyDashboardMetrics(
        totalActiveAlerts = 0,
        criticalAlerts = 0,
        alertsLast24Hours = 0,
        complianceEventsLast24Hours = 0,
        complianceRate = 100.0,
        alertsByType = emptyMap(),
        severityDistribution = emptyMap()
    )
    
    fun createSampleDashboardSnapshot() = DashboardSnapshot(
        timestamp = Clock.System.now().toEpochMilliseconds(),
        safetyMetrics = createSafetyMetrics(),
        performanceMetrics = createPerformanceMetrics(),
        systemHealthMetrics = createSystemHealthMetrics(),
        photoProcessingMetrics = createPhotoProcessingMetrics(),
        alertSummary = createAlertSummary(),
        chartData = createChartData()
    )
    
    fun createSafetyMetrics() = SafetyDashboardMetrics(
        totalActiveAlerts = 5,
        criticalAlerts = 2,
        alertsLast24Hours = 12,
        complianceEventsLast24Hours = 45,
        complianceRate = 95.5,
        alertsByType = mapOf(
            "FALL_PROTECTION" to 3,
            "PPE_VIOLATION" to 2,
            "ELECTRICAL_HAZARD" to 1
        ),
        severityDistribution = mapOf(
            "CRITICAL" to 2,
            "HIGH" to 3,
            "MEDIUM" to 5,
            "LOW" to 2
        )
    )
    
    // Additional factory methods...
}
```

#### 1.5 Acceptance Criteria

✅ **Compilation Success**
- [ ] `./gradlew :shared:compileKotlinMetadata` succeeds
- [ ] `./gradlew :shared:compileDebugKotlinAndroid` succeeds
- [ ] `./gradlew :shared:compileKotlinIosSimulatorArm64` succeeds

✅ **Unit Test Coverage**
- [ ] All data model constructors tested
- [ ] Serialization/deserialization validated
- [ ] Default parameters tested
- [ ] Edge cases covered (empty lists, null values)

✅ **Integration Validation**
- [ ] Dashboard initialization works
- [ ] Metrics collection and aggregation correct
- [ ] Alert triggering and propagation functional
- [ ] SLA reporting generates valid data

✅ **Performance Benchmarks**
- [ ] Dashboard snapshot generation < 100ms
- [ ] Metrics update throughput ≥ 100 updates/second
- [ ] Memory footprint ≤ 50MB for 24h history

---

## Area 2: PTP Generator

### Problem Analysis

**Error Count:** 68 errors (24% of total)
**Root Cause:** Complex document generation logic with incomplete model definitions
**Files Affected:**
- `/Users/aaron/Apps-Coded/HH-v0-fresh/shared/src/commonMain/kotlin/com/hazardhawk/documents/generators/PTPGenerator.kt` (486 lines)

### Critical Issues

1. **Missing Document Model Properties**
   - `PTPDocument` missing required fields
   - `HazardAnalysisSection` incomplete
   - `SafetyProcedure` validation missing

2. **Incomplete Type Definitions**
   - `DocumentAIService` interface not fully defined
   - `PTPTemplateEngine` missing methods
   - Model dependencies unclear

3. **Test Coverage Gaps**
   - Existing tests at `/Users/aaron/Apps-Coded/HH-v0-fresh/shared/src/commonTest/kotlin/com/hazardhawk/documents/PTPGeneratorTest.kt` need updates
   - Mock implementations incomplete

### Test Plan: PTP Generator

#### 2.1 Compilation Tests

**Test ID:** PTP-COMP-001
**Objective:** Verify PTPGenerator compiles successfully
```bash
./gradlew :shared:compileKotlinMetadata --console=plain | grep "PTPGenerator"
```
**Expected:** No compilation errors in PTPGenerator.kt
**Priority:** P0

#### 2.2 Unit Tests

**Test ID:** PTP-UNIT-001
**Test Name:** `should generate complete PTP document from hazard analysis`
**Status:** EXISTS - Update required
**Location:** Line 31 in PTPGeneratorTest.kt

**Updates Needed:**
```kotlin
@Test
fun `should generate complete PTP document from hazard analysis`() = runTest {
    // Given
    val request = createSamplePTPRequest()
    
    // When
    val result = ptpGenerator.generatePTP(request)
    
    // Then
    assertTrue(result.isSuccess, "PTP generation should succeed")
    val response = result.getOrNull()!!
    
    // NEW: Validate all required sections are present
    assertNotNull(response.document.hazardAnalysis)
    assertNotNull(response.document.safetyProcedures)
    assertNotNull(response.document.requiredPPE)
    assertNotNull(response.document.emergencyInformation)
    
    // NEW: Validate metadata
    assertTrue(response.generationMetadata.processingTimeMs > 0)
    assertTrue(response.generationMetadata.hazardsProcessed > 0)
    assertTrue(response.qualityScore > 0.0f)
}
```
**Priority:** P0

---

**Test ID:** PTP-UNIT-002
**Test Name:** `should handle missing optional fields gracefully`

```kotlin
@Test
fun `should handle missing optional fields gracefully`() = runTest {
    // Given
    val minimalRequest = PTPGenerationRequest(
        safetyAnalyses = listOf(createMinimalSafetyAnalysis()),
        projectInfo = ProjectInfo(
            projectName = "Test Project",
            location = "Test Location",
            projectManager = "", // Optional empty
            safetyManager = ""   // Optional empty
        ),
        jobDescription = JobDescription(
            workType = WorkType.GENERAL_CONSTRUCTION,
            taskDescription = "Basic task",
            estimatedDuration = "1 hour",
            workLocation = "Site"
        )
    )
    
    // When
    val result = ptpGenerator.generatePTP(minimalRequest)
    
    // Then
    assertTrue(result.isSuccess)
    val document = result.getOrNull()!!.document
    
    // Should use defaults for missing optional fields
    assertNotNull(document.projectInfo)
    assertNotNull(document.jobDescription)
}
```
**Priority:** P1

---

**Test ID:** PTP-UNIT-003
**Test Name:** `should aggregate hazards and remove duplicates correctly`
**Status:** EXISTS - Update required  
**Location:** Line 76 in PTPGeneratorTest.kt

**Updates Needed:**
```kotlin
@Test
fun `should aggregate hazards and remove duplicates correctly`() = runTest {
    // Given
    val hazard1 = Hazard(
        id = "h1",
        type = HazardType.FALL_PROTECTION,
        description = "Fall from height",
        severity = Severity.HIGH,
        confidence = 0.9f,
        oshaCode = "1926.501",
        recommendations = listOf("Install guardrails")
    )
    val hazard2 = hazard1.copy(id = "h2", confidence = 0.85f) // Similar hazard
    val hazard3 = Hazard(
        id = "h3",
        type = HazardType.PPE_VIOLATION,
        description = "Missing hard hat",
        severity = Severity.MEDIUM,
        confidence = 0.8f,
        oshaCode = "1926.100",
        recommendations = listOf("Require hard hats")
    )
    
    val analysis1 = createSafetyAnalysis(hazards = listOf(hazard1, hazard3))
    val analysis2 = createSafetyAnalysis(hazards = listOf(hazard2))
    
    val request = createPTPRequest(safetyAnalyses = listOf(analysis1, analysis2))
    
    // When
    val result = ptpGenerator.generatePTP(request)
    
    // Then
    assertTrue(result.isSuccess)
    val document = result.getOrNull()!!.document
    val identifiedHazards = document.hazardAnalysis.identifiedHazards
    
    // Should deduplicate fall protection (keep higher confidence)
    val fallHazards = identifiedHazards.filter { 
        it.hazardType.contains("FALL", ignoreCase = true) 
    }
    assertEquals(1, fallHazards.size, "Should deduplicate similar hazards")
    
    // Should keep PPE violation hazard
    val ppeHazards = identifiedHazards.filter {
        it.hazardType.contains("PPE", ignoreCase = true)
    }
    assertEquals(1, ppeHazards.size, "Should keep different hazard types")
}
```
**Priority:** P0

---

**Test ID:** PTP-UNIT-004
**Test Name:** `should generate work-type specific safety procedures`
**Status:** EXISTS - Update required
**Location:** Line 187 in PTPGeneratorTest.kt

**Updates Needed:**
```kotlin
@Test
fun `should generate work-type specific safety procedures`() = runTest {
    // Test multiple work types
    val workTypes = listOf(
        WorkType.ELECTRICAL,
        WorkType.FALL_PROTECTION,
        WorkType.EXCAVATION,
        WorkType.GENERAL_CONSTRUCTION
    )
    
    workTypes.forEach { workType ->
        // Given
        val request = createPTPRequest(
            jobDescription = JobDescription(
                workType = workType,
                taskDescription = "Test task for $workType",
                estimatedDuration = "4 hours",
                workLocation = "Test site"
            )
        )
        
        // When
        val result = ptpGenerator.generatePTP(request)
        
        // Then
        assertTrue(result.isSuccess, "Should succeed for $workType")
        val procedures = result.getOrNull()!!.document.safetyProcedures
        
        // Validate work-type specific content
        when (workType) {
            WorkType.ELECTRICAL -> {
                assertTrue(procedures.any { 
                    it.steps.any { step -> 
                        step.description.contains("lockout", ignoreCase = true)
                    }
                }, "Electrical work should include LOTO procedures")
            }
            WorkType.FALL_PROTECTION -> {
                assertTrue(procedures.any {
                    it.steps.any { step ->
                        step.description.contains("fall protection", ignoreCase = true)
                    }
                }, "Fall protection work should include fall protection procedures")
            }
            // Add other work type validations
        }
    }
}
```
**Priority:** P1

---

**Test ID:** PTP-UNIT-005
**Test Name:** `should calculate quality scores accurately`

```kotlin
@Test
fun `should calculate quality scores accurately`() = runTest {
    // Test different quality scenarios
    
    // Scenario 1: High quality (complete data, high confidence)
    val highQualityRequest = createPTPRequest(
        safetyAnalyses = listOf(
            createSafetyAnalysis(confidence = 0.95f, hazardCount = 5)
        )
    )
    val highQualityResult = ptpGenerator.generatePTP(highQualityRequest)
    assertTrue(highQualityResult.isSuccess)
    assertTrue(highQualityResult.getOrNull()!!.qualityScore >= 0.85f)
    
    // Scenario 2: Low quality (low confidence, few hazards)
    val lowQualityRequest = createPTPRequest(
        safetyAnalyses = listOf(
            createSafetyAnalysis(confidence = 0.5f, hazardCount = 1)
        )
    )
    val lowQualityResult = ptpGenerator.generatePTP(lowQualityRequest)
    assertTrue(lowQualityResult.isSuccess)
    assertTrue(lowQualityResult.getOrNull()!!.qualityScore < 0.85f)
    assertTrue(lowQualityResult.getOrNull()!!.generationMetadata.reviewRequired)
}
```
**Priority:** P1

---

**Test ID:** PTP-UNIT-006
**Test Name:** `should generate appropriate PPE requirements for hazard combinations`

```kotlin
@Test
fun `should generate appropriate PPE requirements for hazard combinations`() = runTest {
    // Given - Multiple hazard types
    val hazards = listOf(
        createHazard(HazardType.FALL_PROTECTION, Severity.CRITICAL),
        createHazard(HazardType.ELECTRICAL_HAZARD, Severity.HIGH),
        createHazard(HazardType.CHEMICAL_HAZARD, Severity.MEDIUM)
    )
    
    val request = createPTPRequest(
        safetyAnalyses = listOf(createSafetyAnalysis(hazards = hazards))
    )
    
    // When
    val result = ptpGenerator.generatePTP(request)
    
    // Then
    assertTrue(result.isSuccess)
    val ppeRequirements = result.getOrNull()!!.document.requiredPPE
    
    // Validate comprehensive PPE coverage
    val requiredPPETypes = setOf(
        PPEType.FALL_PROTECTION,
        PPEType.HEAD_PROTECTION,
        PPEType.EYE_PROTECTION,
        PPEType.HAND_PROTECTION,
        PPEType.RESPIRATORY_PROTECTION
    )
    
    val actualPPETypes = ppeRequirements.map { it.ppeType }.toSet()
    
    assertTrue(
        actualPPETypes.containsAll(requiredPPETypes),
        "Should include all required PPE types. Missing: ${requiredPPETypes - actualPPETypes}"
    )
    
    // Validate each PPE has proper specification
    ppeRequirements.forEach { ppe ->
        assertFalse(ppe.specification.isEmpty(), "PPE should have specification")
        assertFalse(ppe.oshaStandard.isEmpty(), "PPE should reference OSHA standard")
        assertTrue(ppe.applicableHazards.isNotEmpty(), "PPE should list applicable hazards")
    }
}
```
**Priority:** P0

#### 2.3 Integration Tests

**Test ID:** PTP-INT-001
**Test Name:** `should integrate with AI service for hazard analysis`

```kotlin
@Test
fun `should integrate with AI service for hazard analysis`() = runTest {
    // Given - Real AI service (or realistic mock)
    val aiService = MockDocumentAIService(responseDelay = 100L)
    val templateEngine = MockPTPTemplateEngine()
    val generator = PTPGenerator(aiService, templateEngine)
    
    val request = createPTPRequest()
    
    // When
    val result = generator.generatePTP(request)
    
    // Then
    assertTrue(result.isSuccess)
    assertTrue(aiService.callCount > 0, "Should call AI service")
    
    val document = result.getOrNull()!!.document
    assertNotNull(document.hazardAnalysis)
    assertTrue(document.hazardAnalysis.identifiedHazards.isNotEmpty())
}
```
**Priority:** P1

---

**Test ID:** PTP-INT-002
**Test Name:** `should generate documents that pass validation`

```kotlin
@Test
fun `should generate documents that pass validation`() = runTest {
    // Given
    val request = createPTPRequest()
    
    // When
    val result = ptpGenerator.generatePTP(request)
    
    // Then
    assertTrue(result.isSuccess)
    val document = result.getOrNull()!!.document
    
    // Validate document structure
    val validationErrors = validatePTPDocument(document)
    assertTrue(
        validationErrors.isEmpty(),
        "Document should pass validation. Errors: $validationErrors"
    )
}

private fun validatePTPDocument(document: PTPDocument): List<String> {
    val errors = mutableListOf<String>()
    
    // Required fields
    if (document.id.isEmpty()) errors.add("Missing document ID")
    if (document.title.isEmpty()) errors.add("Missing title")
    
    // Required sections
    if (document.hazardAnalysis.identifiedHazards.isEmpty()) {
        errors.add("No hazards identified")
    }
    if (document.safetyProcedures.isEmpty()) {
        errors.add("No safety procedures")
    }
    if (document.requiredPPE.isEmpty()) {
        errors.add("No PPE requirements")
    }
    
    // Emergency information
    val emergency = document.emergencyInformation
    if (emergency.emergencyContacts.isEmpty()) {
        errors.add("No emergency contacts")
    }
    if (emergency.nearestHospital.name.isEmpty()) {
        errors.add("No hospital information")
    }
    
    return errors
}
```
**Priority:** P0

#### 2.4 Performance Tests

**Test ID:** PTP-PERF-001
**Test Name:** `should generate PTP within performance budget`
**Status:** EXISTS - Update required
**Location:** Line 305 in PTPGeneratorTest.kt

```kotlin
@Test
fun `should generate PTP within performance budget`() = runTest {
    // Given
    val request = createPTPRequest()
    val targetTimeMs = 5000L // 5 seconds max
    
    // When
    val startTime = System.currentTimeMillis()
    val result = ptpGenerator.generatePTP(request)
    val duration = System.currentTimeMillis() - startTime
    
    // Then
    assertTrue(result.isSuccess)
    assertTrue(
        duration < targetTimeMs,
        "PTP generation should complete within ${targetTimeMs}ms, took ${duration}ms"
    )
    
    // Validate metadata matches actual duration
    val metadata = result.getOrNull()!!.generationMetadata
    assertTrue(
        metadata.processingTimeMs > 0,
        "Metadata should record processing time"
    )
}
```
**Priority:** P1

#### 2.5 Acceptance Criteria

✅ **Compilation Success**
- [ ] PTPGenerator.kt compiles without errors
- [ ] All dependent models compile successfully
- [ ] No cascading errors from PTP module

✅ **Functional Completeness**
- [ ] Generates complete PTP documents
- [ ] Handles all work types correctly
- [ ] Aggregates hazards accurately
- [ ] Generates appropriate PPE requirements
- [ ] Includes emergency information

✅ **Quality Validation**
- [ ] Quality scoring works correctly
- [ ] Review flagging logic accurate
- [ ] Recommendations generated appropriately

✅ **Performance**
- [ ] PTP generation < 5 seconds
- [ ] Memory usage < 100MB per generation
- [ ] Handles batch generation (10 PTPs/minute)

---

## Area 3: Photo Repository

### Problem Analysis

**Error Count:** 34 errors (12% of total)
**Root Cause:** Repository interface mismatches and missing implementations
**Files Affected:**
- `/Users/aaron/Apps-Coded/HH-v0-fresh/shared/src/commonMain/kotlin/com/hazardhawk/data/repositories/PhotoRepositoryImpl.kt` (122 lines)
- `/Users/aaron/Apps-Coded/HH-v0-fresh/shared/src/commonMain/kotlin/com/hazardhawk/domain/repositories/PhotoRepository.kt` (interface)

### Critical Issues

1. **Interface Mismatch**
   - Return type inconsistencies (Flow vs List)
   - Suspend function requirements
   - Result type wrapping

2. **Missing Database Integration**
   - TODO comments for database operations
   - SQLDelight query implementations pending
   - Transaction handling incomplete

3. **File Management**
   - Platform-specific file operations stubbed
   - FileManager interface incomplete

### Test Plan: Photo Repository

#### 3.1 Compilation Tests

**Test ID:** PHOTO-COMP-001
**Objective:** Verify PhotoRepository implementations compile
```bash
./gradlew :shared:compileKotlinMetadata --console=plain | grep -i "photo"
```
**Expected:** No compilation errors in photo repository files
**Priority:** P0

#### 3.2 Unit Tests

**Test ID:** PHOTO-UNIT-001
**Test Name:** `should save photo successfully`

```kotlin
@Test
fun `should save photo successfully`() = runTest {
    // Given
    val photo = Photo(
        id = "photo-123",
        fileName = "test.jpg",
        filePath = "/path/to/test.jpg",
        capturedAt = Clock.System.now(),
        location = GeoLocation(37.7749, -122.4194),
        tags = listOf("Fall Protection", "PPE")
    )
    
    val repository = PhotoRepositoryImpl(mockDatabase, mockFileManager)
    
    // When
    val result = repository.savePhoto(photo)
    
    // Then
    assertTrue(result.isSuccess)
    assertEquals(photo.id, result.getOrNull()?.id)
}
```
**Priority:** P0

---

**Test ID:** PHOTO-UNIT-002
**Test Name:** `should retrieve photo by ID`

```kotlin
@Test
fun `should retrieve photo by ID`() = runTest {
    // Given
    val photoId = "photo-123"
    val expectedPhoto = createSamplePhoto(id = photoId)
    
    // Mock database to return photo
    whenever(mockDatabase.photoDao().getPhotoById(photoId))
        .thenReturn(expectedPhoto.toEntity())
    
    val repository = PhotoRepositoryImpl(mockDatabase, mockFileManager)
    
    // When
    val photo = repository.getPhoto(photoId)
    
    // Then
    assertNotNull(photo)
    assertEquals(photoId, photo.id)
}
```
**Priority:** P0

---

**Test ID:** PHOTO-UNIT-003
**Test Name:** `should return null for non-existent photo`

```kotlin
@Test
fun `should return null for non-existent photo`() = runTest {
    // Given
    val nonExistentId = "non-existent-123"
    
    whenever(mockDatabase.photoDao().getPhotoById(nonExistentId))
        .thenReturn(null)
    
    val repository = PhotoRepositoryImpl(mockDatabase, mockFileManager)
    
    // When
    val photo = repository.getPhoto(nonExistentId)
    
    // Then
    assertNull(photo)
}
```
**Priority:** P1

---

**Test ID:** PHOTO-UNIT-004
**Test Name:** `should delete photo and file successfully`

```kotlin
@Test
fun `should delete photo and file successfully`() = runTest {
    // Given
    val photoId = "photo-123"
    val photo = createSamplePhoto(id = photoId, filePath = "/path/to/photo.jpg")
    
    whenever(mockDatabase.photoDao().getPhotoById(photoId))
        .thenReturn(photo.toEntity())
    
    val repository = PhotoRepositoryImpl(mockDatabase, mockFileManager)
    
    // When
    val result = repository.deletePhoto(photoId)
    
    // Then
    assertTrue(result.isSuccess)
    
    // Verify database deletion called
    verify(mockDatabase.photoDao()).deletePhoto(photoId)
    
    // Verify file deletion called
    verify(mockFileManager).deleteFile(photo.filePath)
}
```
**Priority:** P0

---

**Test ID:** PHOTO-UNIT-005
**Test Name:** `should handle file deletion failure gracefully`

```kotlin
@Test
fun `should handle file deletion failure gracefully`() = runTest {
    // Given
    val photoId = "photo-123"
    val photo = createSamplePhoto(id = photoId)
    
    whenever(mockDatabase.photoDao().getPhotoById(photoId))
        .thenReturn(photo.toEntity())
    
    // Mock file manager to fail
    whenever(mockFileManager.deleteFile(any()))
        .thenReturn(Result.failure(IOException("File deletion failed")))
    
    val repository = PhotoRepositoryImpl(mockDatabase, mockFileManager)
    
    // When
    val result = repository.deletePhoto(photoId)
    
    // Then
    // Should still succeed (database entry removed)
    // or fail gracefully with proper error
    assertTrue(result.isFailure || result.isSuccess)
}
```
**Priority:** P1

---

**Test ID:** PHOTO-UNIT-006
**Test Name:** `should update photo tags successfully`

```kotlin
@Test
fun `should update photo tags successfully`() = runTest {
    // Given
    val photoId = "photo-123"
    val originalPhoto = createSamplePhoto(id = photoId, tags = listOf("Original"))
    val newTags = listOf("Fall Protection", "PPE", "Updated")
    
    whenever(mockDatabase.photoDao().getPhotoById(photoId))
        .thenReturn(originalPhoto.toEntity())
    
    val repository = PhotoRepositoryImpl(mockDatabase, mockFileManager)
    
    // When
    val result = repository.updatePhotoTags(photoId, newTags)
    
    // Then
    assertTrue(result.isSuccess)
    val updatedPhoto = result.getOrNull()!!
    assertEquals(newTags, updatedPhoto.tags)
    
    // Verify update was persisted
    verify(mockDatabase.photoDao()).updatePhoto(any())
}
```
**Priority:** P0

---

**Test ID:** PHOTO-UNIT-007
**Test Name:** `should return Flow of all photos`

```kotlin
@Test
fun `should return Flow of all photos`() = runTest {
    // Given
    val photos = listOf(
        createSamplePhoto(id = "1"),
        createSamplePhoto(id = "2"),
        createSamplePhoto(id = "3")
    )
    
    whenever(mockDatabase.photoDao().getAllPhotos())
        .thenReturn(photos.map { it.toEntity() })
    
    val repository = PhotoRepositoryImpl(mockDatabase, mockFileManager)
    
    // When
    val photoFlow = repository.getPhotos()
    val collectedPhotos = mutableListOf<List<Photo>>()
    
    photoFlow.collect { photoList ->
        collectedPhotos.add(photoList)
    }
    
    // Then
    assertEquals(1, collectedPhotos.size)
    assertEquals(3, collectedPhotos.first().size)
}
```
**Priority:** P0

#### 3.3 Integration Tests

**Test ID:** PHOTO-INT-001
**Test Name:** `should integrate with SQLDelight database`

```kotlin
@Test
fun `should integrate with SQLDelight database`() = runTest {
    // Given - Real SQLDelight in-memory database
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    HazardHawkDatabase.Schema.create(driver)
    val database = HazardHawkDatabase(driver)
    
    val repository = PhotoRepositoryImpl(database, mockFileManager)
    
    val photo = createSamplePhoto()
    
    // When - Perform CRUD operations
    val saveResult = repository.savePhoto(photo)
    assertTrue(saveResult.isSuccess)
    
    val retrievedPhoto = repository.getPhoto(photo.id)
    assertNotNull(retrievedPhoto)
    
    val updateResult = repository.updatePhotoTags(photo.id, listOf("New Tag"))
    assertTrue(updateResult.isSuccess)
    
    val deleteResult = repository.deletePhoto(photo.id)
    assertTrue(deleteResult.isSuccess)
    
    val deletedPhoto = repository.getPhoto(photo.id)
    assertNull(deletedPhoto)
}
```
**Priority:** P0

---

**Test ID:** PHOTO-INT-002
**Test Name:** `should handle concurrent photo operations`

```kotlin
@Test
fun `should handle concurrent photo operations`() = runTest {
    // Given
    val repository = PhotoRepositoryImpl(mockDatabase, mockFileManager)
    val photoCount = 10
    
    // When - Perform concurrent saves
    val jobs = (1..photoCount).map { index ->
        async {
            val photo = createSamplePhoto(id = "photo-$index")
            repository.savePhoto(photo)
        }
    }
    
    val results = jobs.awaitAll()
    
    // Then
    assertEquals(photoCount, results.size)
    assertTrue(results.all { it.isSuccess })
}
```
**Priority:** P1

#### 3.4 Mock Data Requirements

```kotlin
object PhotoTestDataFactory {
    
    fun createSamplePhoto(
        id: String = "photo-${UUID.randomUUID()}",
        fileName: String = "sample.jpg",
        filePath: String = "/path/to/sample.jpg",
        tags: List<String> = emptyList()
    ) = Photo(
        id = id,
        fileName = fileName,
        filePath = filePath,
        capturedAt = Clock.System.now(),
        location = GeoLocation(37.7749, -122.4194),
        tags = tags
    )
    
    fun createPhotoEntity(photo: Photo): PhotoEntity {
        // Convert domain Photo to database entity
        return PhotoEntity(
            id = photo.id,
            fileName = photo.fileName,
            filePath = photo.filePath,
            capturedAt = photo.capturedAt.toEpochMilliseconds(),
            latitude = photo.location?.latitude,
            longitude = photo.location?.longitude,
            tags = photo.tags.joinToString(",")
        )
    }
}

class MockFileManager : FileManager {
    override suspend fun deleteFile(filePath: String): Result<Unit> {
        return Result.success(Unit)
    }
    
    override suspend fun getPhotoPath(photoId: String): String {
        return "/mock/path/$photoId.jpg"
    }
}
```

#### 3.5 Acceptance Criteria

✅ **Compilation Success**
- [ ] PhotoRepository interface fully implemented
- [ ] No type mismatch errors
- [ ] All method signatures correct

✅ **Functional Completeness**
- [ ] CRUD operations work correctly
- [ ] Flow emissions function properly
- [ ] Error handling robust

✅ **Database Integration**
- [ ] SQLDelight queries execute successfully
- [ ] Transactions work correctly
- [ ] Data persistence validated

✅ **Performance**
- [ ] Photo save < 100ms
- [ ] Photo retrieval < 50ms
- [ ] Batch operations efficient (100 photos < 5s)

---

## Integration Testing

### Cross-Module Integration Tests

**Test ID:** INT-001
**Test Name:** `end-to-end photo analysis to PTP workflow`

```kotlin
@Test
fun `end-to-end photo analysis to PTP workflow`() = runTest {
    // Given - Complete system setup
    val photoRepository = PhotoRepositoryImpl(database, fileManager)
    val aiAnalyzer = MockAIPhotoAnalyzer()
    val ptpGenerator = PTPGenerator(aiService, templateEngine)
    
    // Step 1: Save photos
    val photos = (1..3).map { createSamplePhoto(id = "photo-$it") }
    photos.forEach { photo ->
        val result = photoRepository.savePhoto(photo)
        assertTrue(result.isSuccess)
    }
    
    // Step 2: Analyze photos
    val analyses = photos.map { photo ->
        aiAnalyzer.analyzePhoto(photo.filePath).getOrThrow()
    }
    
    // Step 3: Generate PTP
    val ptpRequest = PTPGenerationRequest(
        safetyAnalyses = analyses,
        projectInfo = createProjectInfo(),
        jobDescription = createJobDescription()
    )
    
    val ptpResult = ptpGenerator.generatePTP(ptpRequest)
    
    // Then
    assertTrue(ptpResult.isSuccess)
    val document = ptpResult.getOrNull()!!.document
    
    // Validate end-to-end data flow
    assertEquals(photos.size, ptpRequest.safetyAnalyses.size)
    assertTrue(document.hazardAnalysis.identifiedHazards.isNotEmpty())
    assertTrue(document.safetyProcedures.isNotEmpty())
}
```
**Priority:** P0

---

**Test ID:** INT-002
**Test Name:** `performance monitoring captures PTP generation metrics`

```kotlin
@Test
fun `performance monitoring captures PTP generation metrics`() = runTest {
    // Given
    val monitoring = ProductionMonitoringSystem(
        realtimeMonitoring, performanceDashboard,
        aiModelManager, batchProcessor, auditLogger
    )
    val ptpGenerator = PTPGenerator(aiService, templateEngine)
    
    monitoring.initializeMonitoring()
    
    // When - Generate PTP with monitoring
    val request = createPTPRequest()
    val startTime = System.currentTimeMillis()
    val result = ptpGenerator.generatePTP(request)
    val duration = System.currentTimeMillis() - startTime
    
    // Report to monitoring
    performanceDashboard.updateMetrics(
        responseTime = duration,
        workType = request.jobDescription.workType,
        success = result.isSuccess
    )
    
    delay(100) // Allow metrics to propagate
    
    // Then - Verify monitoring captured metrics
    val snapshot = performanceDashboard.generateDashboardSnapshot()
    assertTrue(snapshot.performanceMetrics.averageResponseTimeMs > 0)
}
```
**Priority:** P1

---

**Test ID:** INT-003
**Test Name:** `photo repository integrates with analysis repository`

```kotlin
@Test
fun `photo repository integrates with analysis repository`() = runTest {
    // Given
    val photoRepo = PhotoRepositoryImpl(database, fileManager)
    val analysisRepo = AnalysisRepositoryImpl(database)
    
    val photo = createSamplePhoto()
    
    // When
    photoRepo.savePhoto(photo)
    
    val analysis = createSafetyAnalysis(photoId = photo.id)
    val saveResult = analysisRepo.saveAnalysis(analysis)
    
    // Then
    assertTrue(saveResult.isSuccess)
    
    // Verify linkage
    val retrievedAnalysis = analysisRepo.getAnalysisForPhoto(photo.id)
    assertNotNull(retrievedAnalysis)
    assertEquals(photo.id, retrievedAnalysis.photoId)
}
```
**Priority:** P1

---

## Performance Testing

### Performance Benchmarks

**Test ID:** PERF-BENCH-001
**Test Name:** `dashboard snapshot generation performance`

```kotlin
@Test
fun `dashboard snapshot generation should be fast`() = runTest {
    // Given
    val dashboard = PerformanceDashboard(monitoring, aiOptimizer)
    dashboard.initializeDashboard()
    
    // Warm up
    repeat(5) {
        dashboard.generateDashboardSnapshot()
    }
    
    // When - Benchmark snapshot generation
    val iterations = 100
    val durations = mutableListOf<Long>()
    
    repeat(iterations) {
        val startTime = System.nanoTime()
        dashboard.generateDashboardSnapshot()
        val duration = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms
        durations.add(duration)
    }
    
    // Then
    val avgDuration = durations.average()
    val p95Duration = durations.sorted()[((iterations * 0.95).toInt())]
    
    assertTrue(avgDuration < 100.0, "Average generation time should be <100ms, was ${avgDuration}ms")
    assertTrue(p95Duration < 150, "P95 generation time should be <150ms, was ${p95Duration}ms")
}
```
**Target:** < 100ms average, < 150ms p95
**Priority:** P1

---

**Test ID:** PERF-BENCH-002
**Test Name:** `PTP generation throughput`

```kotlin
@Test
fun `PTP generation should meet throughput requirements`() = runTest {
    // Given
    val generator = PTPGenerator(aiService, templateEngine)
    val targetThroughput = 10 // 10 PTPs per minute
    
    // When - Generate PTPs concurrently
    val startTime = System.currentTimeMillis()
    val requests = (1..targetThroughput).map { createPTPRequest() }
    
    val results = requests.map { request ->
        async {
            generator.generatePTP(request)
        }
    }.awaitAll()
    
    val totalTime = System.currentTimeMillis() - startTime
    
    // Then
    assertTrue(results.all { it.isSuccess })
    assertTrue(totalTime < 60_000, "Should generate $targetThroughput PTPs in <60s, took ${totalTime}ms")
    
    val actualThroughput = (results.size * 60_000.0) / totalTime
    assertTrue(
        actualThroughput >= targetThroughput,
        "Throughput should be ≥$targetThroughput PTPs/min, was $actualThroughput"
    )
}
```
**Target:** ≥10 PTPs/minute
**Priority:** P1

---

**Test ID:** PERF-BENCH-003
**Test Name:** `photo repository batch operations`

```kotlin
@Test
fun `photo repository should handle batch operations efficiently`() = runTest {
    // Given
    val repository = PhotoRepositoryImpl(database, fileManager)
    val batchSize = 100
    val photos = (1..batchSize).map { createSamplePhoto(id = "photo-$it") }
    
    // When - Batch save
    val startTime = System.currentTimeMillis()
    val results = photos.map { photo ->
        async {
            repository.savePhoto(photo)
        }
    }.awaitAll()
    val saveTime = System.currentTimeMillis() - startTime
    
    // Then
    assertTrue(results.all { it.isSuccess })
    assertTrue(saveTime < 5000, "Batch save of $batchSize photos should take <5s, took ${saveTime}ms")
    
    val throughput = (batchSize * 1000.0) / saveTime
    assertTrue(throughput >= 20, "Should achieve ≥20 photos/second, got $throughput")
}
```
**Target:** ≥20 photos/second
**Priority:** P2

---

## Test Execution Plan

### Execution Order and Dependencies

```
Phase 1: Compilation Validation (5 minutes)
├── PERF-COMP-001 (metadata compilation)
├── PERF-COMP-002 (Android compilation)
├── PERF-COMP-003 (iOS compilation)
├── PTP-COMP-001 (PTP compilation)
└── PHOTO-COMP-001 (Photo repository compilation)

Phase 2: Unit Tests (10 minutes)
├── Performance Monitoring (6 tests, parallel)
│   ├── PERF-UNIT-001
│   ├── PERF-UNIT-002
│   ├── PERF-UNIT-003
│   ├── PERF-UNIT-004
│   ├── PERF-UNIT-005
│   └── PERF-UNIT-006
├── PTP Generator (6 tests, parallel)
│   ├── PTP-UNIT-001
│   ├── PTP-UNIT-002
│   ├── PTP-UNIT-003
│   ├── PTP-UNIT-004
│   ├── PTP-UNIT-005
│   └── PTP-UNIT-006
└── Photo Repository (7 tests, parallel)
    ├── PHOTO-UNIT-001
    ├── PHOTO-UNIT-002
    ├── PHOTO-UNIT-003
    ├── PHOTO-UNIT-004
    ├── PHOTO-UNIT-005
    ├── PHOTO-UNIT-006
    └── PHOTO-UNIT-007

Phase 3: Integration Tests (8 minutes)
├── PERF-INT-001
├── PERF-INT-002
├── PERF-INT-003
├── PTP-INT-001
├── PTP-INT-002
├── PHOTO-INT-001
├── PHOTO-INT-002
├── INT-001
├── INT-002
└── INT-003

Phase 4: Performance Tests (5 minutes)
├── PTP-PERF-001
├── PERF-BENCH-001
├── PERF-BENCH-002
└── PERF-BENCH-003

Phase 5: Regression Tests (5 minutes)
└── Execute existing test suite
```

**Total Estimated Time:** 33 minutes

### Automated Execution Script

```bash
#!/bin/bash
# test-phase2-fixes.sh

set -e

echo "========================================="
echo "Phase 2 Build Fixes - Test Execution"
echo "========================================="

# Phase 1: Compilation Tests
echo ""
echo "Phase 1: Compilation Validation"
echo "---------------------------------"

echo "Testing metadata compilation..."
./gradlew :shared:compileKotlinMetadata || exit 1

echo "Testing Android compilation..."
./gradlew :shared:compileDebugKotlinAndroid || exit 1

echo "Testing iOS compilation..."
./gradlew :shared:compileKotlinIosSimulatorArm64 || exit 1

echo "✅ Phase 1: All compilation tests passed"

# Phase 2: Unit Tests
echo ""
echo "Phase 2: Unit Tests"
echo "-------------------"

echo "Running performance monitoring unit tests..."
./gradlew :shared:testDebugUnitTest --tests "*PerformanceDashboardTest*" || exit 1
./gradlew :shared:testDebugUnitTest --tests "*ProductionMonitoringSystemTest*" || exit 1

echo "Running PTP generator unit tests..."
./gradlew :shared:testDebugUnitTest --tests "*PTPGeneratorTest*" || exit 1

echo "Running photo repository unit tests..."
./gradlew :shared:testDebugUnitTest --tests "*PhotoRepositoryTest*" || exit 1

echo "✅ Phase 2: All unit tests passed"

# Phase 3: Integration Tests
echo ""
echo "Phase 3: Integration Tests"
echo "--------------------------"

./gradlew :shared:testDebugUnitTest --tests "*IntegrationTest*" || exit 1

echo "✅ Phase 3: All integration tests passed"

# Phase 4: Performance Tests
echo ""
echo "Phase 4: Performance Benchmarks"
echo "--------------------------------"

./gradlew :shared:testDebugUnitTest --tests "*PerformanceBenchmarkTest*" || exit 1

echo "✅ Phase 4: All performance tests passed"

# Phase 5: Regression Tests
echo ""
echo "Phase 5: Regression Validation"
echo "-------------------------------"

./gradlew :shared:test || exit 1

echo "✅ Phase 5: Regression tests passed"

# Generate test report
echo ""
echo "Generating test report..."
./gradlew :shared:jacocoTestReport

echo ""
echo "========================================="
echo "✅ ALL TESTS PASSED"
echo "========================================="
echo ""
echo "Test Report: shared/build/reports/tests/test/index.html"
echo "Coverage Report: shared/build/reports/jacoco/test/html/index.html"
```

---

## Acceptance Criteria

### Overall Success Criteria

#### Critical (Must-Pass)

- [ ] **Zero Compilation Errors**
  - All modules compile successfully
  - No cascading errors
  - All platforms (Android, iOS, metadata) build

- [ ] **Test Pass Rate ≥98%**
  - All new tests pass
  - All existing tests pass
  - No test regressions

- [ ] **Type Safety Maintained**
  - No type casting errors
  - Serialization works correctly
  - Model contracts preserved

#### Important (Should-Pass)

- [ ] **Performance Maintained**
  - Dashboard snapshot < 100ms
  - PTP generation < 5s
  - Photo operations < 100ms

- [ ] **Test Coverage ≥85%**
  - Line coverage for fixed code
  - Branch coverage for logic
  - All error paths tested

- [ ] **Documentation Complete**
  - Test reports generated
  - Coverage reports available
  - Known issues documented

#### Nice-to-Have

- [ ] **Performance Improvements**
  - Better than baseline metrics
  - Optimizations identified

- [ ] **Additional Test Coverage**
  - Edge cases covered
  - Stress tests added

### Detailed Acceptance Checklist

#### Area 1: Performance Monitoring Models

**Compilation:**
- [ ] `PerformanceDashboard.kt` compiles without errors
- [ ] `ProductionMonitoringSystem.kt` compiles without errors
- [ ] All data models have proper default parameters
- [ ] Serialization annotations correct

**Functionality:**
- [ ] Dashboard initialization works
- [ ] Metrics collection functional
- [ ] Alert triggering accurate
- [ ] SLA reporting generates valid data
- [ ] Metrics history management works

**Tests:**
- [ ] 6/6 unit tests pass
- [ ] 3/3 integration tests pass
- [ ] Performance benchmarks meet targets

#### Area 2: PTP Generator

**Compilation:**
- [ ] `PTPGenerator.kt` compiles without errors
- [ ] All document models complete
- [ ] Dependencies resolved

**Functionality:**
- [ ] PTP generation produces complete documents
- [ ] Hazard aggregation works correctly
- [ ] PPE requirements accurate
- [ ] Work-type specific procedures generated
- [ ] Quality scoring functional

**Tests:**
- [ ] 6/6 unit tests pass
- [ ] 2/2 integration tests pass
- [ ] 1/1 performance test passes

#### Area 3: Photo Repository

**Compilation:**
- [ ] `PhotoRepositoryImpl.kt` compiles without errors
- [ ] Interface fully implemented
- [ ] No type mismatches

**Functionality:**
- [ ] CRUD operations work
- [ ] Flow emissions correct
- [ ] Error handling robust
- [ ] File management functional

**Tests:**
- [ ] 7/7 unit tests pass
- [ ] 2/2 integration tests pass

### Verification Commands

```bash
# Verify all acceptance criteria
./scripts/verify-phase2-acceptance.sh

# Check compilation
./gradlew :shared:compileKotlinMetadata --console=plain

# Check test pass rate
./gradlew :shared:test --console=plain | grep "tests completed"

# Check test coverage
./gradlew :shared:jacocoTestReport
open shared/build/reports/jacoco/test/html/index.html

# Verify performance benchmarks
./gradlew :shared:testDebugUnitTest --tests "*PerformanceBenchmarkTest*"
```

---

## Risk Mitigation

### Identified Risks and Mitigation Strategies

#### Risk 1: Test Failures Due to Mock Incompleteness

**Probability:** Medium
**Impact:** High
**Mitigation:**
- Complete all mock implementations before test execution
- Use realistic test data from `TestDataFactory`
- Validate mock behavior in isolation first

**Contingency:**
- If mocks fail, use in-memory implementations
- Implement minimal real services for critical paths
- Document mock limitations

#### Risk 2: Performance Regression

**Probability:** Low
**Impact:** Medium
**Mitigation:**
- Establish baseline metrics before fixes
- Run performance tests before and after
- Profile critical paths if regression detected

**Contingency:**
- If performance degrades >10%, investigate immediately
- Use profiling tools to identify bottlenecks
- Optimize or roll back if necessary

#### Risk 3: Integration Test Dependencies

**Probability:** Medium
**Impact:** Medium
**Mitigation:**
- Use in-memory databases for tests
- Mock external dependencies
- Isolate test environments

**Contingency:**
- If integration fails, test components individually
- Create hermetic test environments
- Use test containers if needed

#### Risk 4: Cascading Test Failures

**Probability:** Low
**Impact:** High
**Mitigation:**
- Fix compilation errors first
- Run tests in dependency order
- Fail fast and report early

**Contingency:**
- If cascading failures occur, stop and fix root cause
- Don't proceed to next phase until current phase passes
- Document dependencies clearly

#### Risk 5: Time Overrun

**Probability:** Medium
**Impact:** Low
**Mitigation:**
- Start with highest priority tests (P0)
- Run tests in parallel where possible
- Use incremental testing approach

**Contingency:**
- If time runs out, ensure P0 tests complete
- Document incomplete tests for future work
- Provide interim status reports

### Test Failure Response Plan

```
Test Failure Detected
      |
      v
Identify Failure Category
      |
      +-- Compilation Error
      |     |
      |     v
      |   Fix immediately (P0)
      |   Re-run compilation tests
      |
      +-- Unit Test Failure
      |     |
      |     v
      |   Investigate root cause
      |   Fix and verify in isolation
      |   Re-run related tests
      |
      +-- Integration Test Failure
      |     |
      |     v
      |   Check component dependencies
      |   Verify mocks and data
      |   Fix and re-run integration suite
      |
      +-- Performance Test Failure
            |
            v
          Profile and optimize
          Re-benchmark
          Document if acceptable variance
```

---

## Appendix

### Test Data Factory Reference

```kotlin
// TestDataFactory.kt - Complete reference

object TestDataFactory {
    
    // Performance Monitoring
    fun createSampleDashboardSnapshot() = DashboardSnapshot(...)
    fun createSafetyMetrics() = SafetyDashboardMetrics(...)
    fun createPerformanceMetrics() = PerformanceDashboardMetrics(...)
    fun createSystemHealthMetrics() = SystemHealthDashboardMetrics(...)
    
    // PTP Generator
    fun createSamplePTPRequest() = PTPGenerationRequest(...)
    fun createSampleSafetyAnalysis() = SafetyAnalysis(...)
    fun createFallProtectionHazard() = Hazard(...)
    fun createElectricalHazard() = Hazard(...)
    fun createPPEViolationHazard() = Hazard(...)
    
    // Photo Repository
    fun createSamplePhoto() = Photo(...)
    fun createPhotoEntity() = PhotoEntity(...)
    
    // Performance Testing
    fun createPerformanceScenarios() = listOf(...)
}
```

### Test Utilities Reference

```kotlin
// TestUtils.kt - Performance measurement utilities

object TestUtils {
    
    suspend fun <T> measureExecutionTime(block: suspend () -> T): Pair<T, Duration> {
        val startTime = TimeSource.Monotonic.markNow()
        val result = block()
        val duration = startTime.elapsedNow()
        return result to duration
    }
    
    fun assertPerformanceWithin(
        actualMs: Long,
        expectedMs: Long,
        tolerancePercent: Double,
        scenario: String
    ) {
        val tolerance = expectedMs * (tolerancePercent / 100.0)
        val maxAllowed = expectedMs + tolerance
        
        assertTrue(
            actualMs <= maxAllowed,
            "$scenario: Expected ≤${maxAllowed}ms, got ${actualMs}ms"
        )
    }
}
```

### Mock Implementation Templates

```kotlin
// Mock implementations for testing

class MockAIPhotoAnalyzer(
    val analyzerName: String = "Mock Analyzer",
    val responseDelay: Long = 100L,
    val shouldSucceed: Boolean = true
) : AIPhotoAnalyzer {
    
    override suspend fun analyzePhoto(photoPath: String): Result<SafetyAnalysis> {
        delay(responseDelay)
        
        return if (shouldSucceed) {
            Result.success(TestDataFactory.createSampleSafetyAnalysis())
        } else {
            Result.failure(Exception("Mock analyzer failure"))
        }
    }
}

class MockDocumentAIService(
    val responseDelay: Long = 50L
) : DocumentAIService {
    var callCount = 0
    var shouldFail = false
    
    override suspend fun generateHazardAnalysis(request: Any): Result<String> {
        callCount++
        delay(responseDelay)
        
        return if (shouldFail) {
            Result.failure(Exception("Mock AI service failure"))
        } else {
            Result.success("Mock hazard analysis")
        }
    }
}
```

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-10-10 | Test Guardian (Claude) | Initial comprehensive testing strategy |

---

**Next Steps:**

1. Review and approve this testing strategy
2. Implement mock factories and test utilities
3. Begin Phase 1: Compilation Tests
4. Proceed through test execution plan
5. Generate final test report

**Questions or Concerns:** Contact the development team or refer to Phase 2 implementation logs.
