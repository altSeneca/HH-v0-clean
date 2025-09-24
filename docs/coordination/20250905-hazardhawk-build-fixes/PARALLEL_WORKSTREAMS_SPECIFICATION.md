# Parallel Workstreams Specification
## HazardHawk Build Fixes - Tactical Implementation Guide

## Workstream A: Critical Model Creation
**Agent**: simple-architect | **Duration**: 45 minutes | **Critical Path**: YES

### Scope & Objectives
Create missing data classes that are causing compilation failures across the Android app. These models are fundamental to the report generation and dialog systems.

### Detailed Task Breakdown

#### Task A1: ReportTemplate Data Class (15 minutes)
**Location**: `/shared/src/commonMain/kotlin/com/hazardhawk/models/ReportTemplate.kt`

**Implementation Requirements**:
```kotlin
@Serializable
data class ReportTemplate(
    val id: String,
    val name: String,
    val type: ReportType,
    val description: String,
    val sections: List<ReportSection> = emptyList(),
    val requiredFields: List<String> = emptyList(),
    val oshaCompliance: Boolean = true,
    val version: String = "1.0",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
```

**Dependencies**: None - Can start immediately
**Validation**: Serialization test required
**Risk Level**: 🟢 Low

#### Task A2: ReportType Enum (10 minutes)
**Location**: Same file as ReportTemplate

**Implementation Requirements**:
```kotlin
@Serializable
enum class ReportType(
    val displayName: String,
    val oshaRequired: Boolean = false,
    val category: ReportCategory
) {
    DAILY_INSPECTION("Daily Safety Inspection", true, ReportCategory.INSPECTION),
    INCIDENT_REPORT("Incident Report", true, ReportCategory.INCIDENT),
    PRE_TASK_PLAN("Pre-Task Plan (PTP)", true, ReportCategory.PLANNING),
    WEEKLY_SUMMARY("Weekly Safety Summary", false, ReportCategory.SUMMARY),
    HAZARD_IDENTIFICATION("Hazard Identification", true, ReportCategory.SAFETY),
    SAFETY_TRAINING("Safety Training Record", true, ReportCategory.TRAINING),
    TOOLBOX_TALK("Toolbox Talk", true, ReportCategory.TRAINING),
    SAFETY_AUDIT("Safety Audit", true, ReportCategory.INSPECTION),
    NEAR_MISS("Near Miss Report", true, ReportCategory.INCIDENT)
}

@Serializable
enum class ReportCategory {
    INSPECTION, INCIDENT, PLANNING, SUMMARY, SAFETY, TRAINING
}
```

**Dependencies**: None
**Validation**: OSHA compliance validation
**Risk Level**: 🟢 Low

#### Task A3: ReportSection Data Class (10 minutes)
**Location**: Same file as ReportTemplate

**Implementation Requirements**:
```kotlin
@Serializable
data class ReportSection(
    val id: String,
    val title: String,
    val description: String,
    val required: Boolean = false,
    val fieldType: SectionFieldType,
    val options: List<String> = emptyList(),
    val validation: SectionValidation? = null,
    val order: Int = 0
)

@Serializable
enum class SectionFieldType {
    TEXT, TEXTAREA, CHECKBOX, RADIO, DROPDOWN, 
    DATE, TIME, PHOTO, SIGNATURE, LOCATION
}

@Serializable
data class SectionValidation(
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val required: Boolean = false,
    val pattern: String? = null
)
```

**Dependencies**: None
**Validation**: Field type validation
**Risk Level**: 🟢 Low

#### Task A4: ReportGenerationState (10 minutes)
**Location**: `/androidApp/src/main/java/com/hazardhawk/reports/ReportGenerationState.kt`

**Implementation Requirements**:
```kotlin
data class ReportGenerationState(
    val selectedTemplate: ReportTemplate? = null,
    val isGenerating: Boolean = false,
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val errorMessage: String? = null,
    val generatedReportId: String? = null,
    val formData: Map<String, Any> = emptyMap()
)
```

**Dependencies**: Task A1 completion (ReportTemplate)
**Validation**: State transition testing
**Risk Level**: 🟢 Low

### Workstream A Success Criteria
- [ ] All data classes compile without errors
- [ ] Serialization/deserialization tests pass
- [ ] No circular dependencies created
- [ ] Shared module build time <40 seconds
- [ ] OSHA compliance validation functional

### Workstream A Coordination Points
- **30 min checkpoint**: Report completion status to coordination
- **Handoff to Workstream C**: Models available for testing
- **Handoff to Workstream D**: Impact assessment for build config

---

## Workstream B: Type System Fixes
**Agent**: complete-reviewer | **Duration**: 30 minutes | **Critical Path**: NO

### Scope & Objectives
Resolve lambda type inference issues and suspend function context problems that are preventing compilation in Android app components.

### Detailed Task Breakdown

#### Task B1: Property Delegate Type Annotations (10 minutes)
**Files Affected**:
- `/androidApp/src/main/java/com/hazardhawk/reports/ReportGenerationDialogs.kt`
- `/androidApp/src/main/java/com/hazardhawk/gallery/PhotoDeletionDialog.kt`

**Issue Pattern**:
```kotlin
// Before (failing):
var selectedTemplate by remember { mutableStateOf<ReportTemplate?>(null) }
var showConfirmation by remember { mutableStateOf(false) }

// After (working):
var selectedTemplate: ReportTemplate? by remember { mutableStateOf(null) }
var showConfirmation: Boolean by remember { mutableStateOf(false) }
```

**Implementation Strategy**:
1. Identify all property delegate declarations
2. Add explicit type annotations before `by` keyword
3. Verify Compose state management compatibility
4. Test incremental compilation

**Risk Level**: 🟢 Low
**Dependencies**: None

#### Task B2: Lambda Parameter Types (10 minutes)
**Files Affected**:
- Various Compose components with callback lambdas
- Event handler implementations

**Issue Pattern**:
```kotlin
// Before (type inference failing):
onClick = { onTemplateSelected(it) }
onValueChange = { newValue -> handleChange(newValue) }

// After (explicit types):
onClick = { template: ReportTemplate -> onTemplateSelected(template) }
onValueChange = { newValue: String -> handleChange(newValue) }
```

**Implementation Strategy**:
1. Scan for lambda expressions with inference issues
2. Add explicit parameter types
3. Verify callback contract compatibility
4. Test with Compose compiler

**Risk Level**: 🟢 Low
**Dependencies**: None

#### Task B3: Suspend Function Context (10 minutes)
**Files Affected**:
- Dialog components with coroutine usage
- Animation and haptic feedback implementations

**Issue Pattern**:
```kotlin
// Before (context missing):
delay(100)
hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

// After (proper context):
LaunchedEffect(key1) {
    delay(100)
    // Haptic feedback in compose context
}
```

**Implementation Strategy**:
1. Identify suspend function calls outside coroutine scope
2. Wrap in appropriate Compose effects (LaunchedEffect, rememberCoroutineScope)
3. Verify side effect management
4. Test lifecycle compatibility

**Risk Level**: 🟡 Medium
**Dependencies**: None

### Workstream B Success Criteria
- [ ] Zero type inference errors remaining
- [ ] All lambda expressions compile cleanly
- [ ] Suspend functions properly scoped
- [ ] No new Compose lint warnings
- [ ] Incremental compilation functional

### Workstream B Coordination Points
- **15 min checkpoint**: Report progress on type fixes
- **30 min completion**: Ready for Phase 2 integration
- **Handoff to Workstream D**: Type system impact on build config

---

## Workstream C: Testing Infrastructure
**Agent**: test-guardian | **Duration**: 30 minutes | **Dependencies**: Workstream A

### Scope & Objectives
Ensure testing infrastructure can handle new models and maintains compatibility with existing test suite. Focus on preventing regression while adding new validation.

### Detailed Task Breakdown

#### Task C1: Dependency Resolution (10 minutes)
**Location**: `/androidApp/build.gradle.kts`

**Issue**: Missing JUnit and testing dependencies
**Implementation**:
```kotlin
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Android testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

**Risk Level**: 🟡 Medium
**Dependencies**: None

#### Task C2: Model Validation Tests (15 minutes)
**Location**: `/shared/src/commonTest/kotlin/com/hazardhawk/models/`

**Create Tests For**:
- ReportTemplate serialization/deserialization
- ReportType OSHA compliance validation
- ReportSection field validation
- State management correctness

**Implementation Example**:
```kotlin
class ReportTemplateTest {
    @Test
    fun `ReportTemplate should serialize correctly`() = runTest {
        val template = ReportTemplate(
            id = "test-id",
            name = "Daily Inspection", 
            type = ReportType.DAILY_INSPECTION,
            description = "Test template"
        )
        
        val json = Json.encodeToString(template)
        val decoded = Json.decodeFromString<ReportTemplate>(json)
        
        assertEquals(template, decoded)
    }
    
    @Test
    fun `ReportType should validate OSHA compliance`() {
        val oshaRequired = ReportType.INCIDENT_REPORT.oshaRequired
        assertTrue(oshaRequired)
        
        val optional = ReportType.WEEKLY_SUMMARY.oshaRequired
        assertFalse(optional)
    }
}
```

**Risk Level**: 🟢 Low
**Dependencies**: Workstream A completion

#### Task C3: Integration Test Verification (5 minutes)
**Scope**: Ensure existing tests still pass

**Actions**:
1. Run existing test suite
2. Identify any breaking changes
3. Update mocks if necessary
4. Verify test execution time

**Command Sequence**:
```bash
./gradlew :shared:test
./gradlew :androidApp:testDebugUnitTest
```

**Risk Level**: 🟢 Low
**Dependencies**: Workstreams A & B partial completion

### Workstream C Success Criteria
- [ ] All test dependencies resolve correctly
- [ ] New model tests pass
- [ ] Existing tests maintain functionality
- [ ] Test execution time <2 minutes
- [ ] Mock infrastructure compatible

### Workstream C Coordination Points
- **15 min checkpoint**: Dependency resolution status
- **30 min completion**: Full test suite operational
- **Blocker escalation**: If existing tests break

---

## Workstream D: Build Configuration Optimization
**Agent**: simple-architect | **Duration**: 15 minutes | **Dependencies**: A & B

### Scope & Objectives
Optimize build configuration for new components while maintaining excellent performance baseline.

### Detailed Task Breakdown

#### Task D1: Memory Setting Validation (5 minutes)
**Current**: 6GB heap, G1GC
**Action**: Verify adequacy for new models
**Check**: Monitor during build execution

#### Task D2: Incremental Compilation (5 minutes)
**Focus**: Ensure new classes don't break incremental builds
**Validation**: Test incremental vs clean build times

#### Task D3: Cache Optimization (5 minutes)
**Action**: Update cache keys for new model classes
**Verify**: Build cache effectiveness maintained

### Workstream D Success Criteria
- [ ] Build performance maintained (<40s shared)
- [ ] Incremental compilation functional
- [ ] Memory usage within limits
- [ ] Cache hit rate preserved

---

## Workstream E: Documentation Updates
**Agent**: docs-curator | **Duration**: 30 minutes | **Dependencies**: None

### Scope & Objectives
Update implementation documentation and create troubleshooting guides.

### Task Breakdown
1. **API Documentation** (10 min): Document new model classes
2. **Implementation Guide** (10 min): Update coordination decisions
3. **Troubleshooting Guide** (10 min): Common fix patterns

### Success Criteria
- [ ] All new APIs documented
- [ ] Coordination decisions recorded
- [ ] Troubleshooting guide complete

---

## Coordination Synchronization Points

### 15-Minute Status Check
**Required Reports**:
- Workstream A: Model creation progress
- Workstream B: Type fixes completed
- Blockers or delays identified

### 30-Minute Integration Check
**Required Confirmations**:
- Workstream A: Models complete and tested
- Workstream B: Type system clean
- Workstream C: Dependencies resolved
- Ready for Phase 3 validation

### 45-Minute Final Status
**Required Validations**:
- All workstreams complete
- Integration testing passed
- Build verification ready
- Success criteria met

This specification enables precise, coordinated execution of parallel workstreams while maintaining quality and preventing conflicts.