# Comprehensive Refactoring Strategy: Phase 2 Build Error Resolution

**Document:** Phase 2 Refactoring Strategy  
**Date:** 2025-10-10  
**Status:** In Progress (284 errors down from 660)  
**Goal:** Systematic resolution of remaining compilation errors while improving code maintainability

---

## Executive Summary

We've successfully fixed 376 errors (57%) through model consolidation and import cleanup. The remaining 284 errors fall into clear patterns that can be addressed through systematic refactoring.

### Progress Overview
- **Fixed:** 376 errors (57%)
- **Remaining:** 284 errors (43%)
- **Error Reduction Rate:** 68 errors per major fix cycle

### Key Achievements
1. Unified `SafetyAnalysis` model in `core.models` package
2. Consolidated `OSHAViolation` types
3. Standardized `Severity`, `HazardType`, `ComplianceStatus` enums
4. Removed duplicate model definitions

---

## Error Classification & Analysis

### Category 1: Platform API Gaps (Priority: CRITICAL)
**Impact:** 70+ errors  
**Files Affected:** 5 files

#### Issues:
1. **LiteRTDeviceOptimizer** - Missing `expect` declarations
   - `getBatteryLevel()`, `isPowerSaveModeEnabled()`, `getCpuCoreCount()`
   - `detectGpuVendor()`, `hasHighPerformanceGpu()`

2. **IDeviceInfo** interface incomplete
   - Missing cross-platform device capability APIs
   - No platform-specific implementations visible

#### Root Cause:
The refactoring moved platform-specific code without creating proper KMP `expect/actual` declarations.

#### Refactoring Strategy:
```kotlin
// Step 1: Create expect interface in commonMain
// File: shared/src/commonMain/kotlin/com/hazardhawk/platform/IDeviceInfo.kt
expect interface IDeviceInfo {
    fun getBatteryLevel(): Int
    fun isPowerSaveModeEnabled(): Boolean
    fun getCpuCoreCount(): Int
    fun detectGpuVendor(): GpuVendor
    fun hasHighPerformanceGpu(): Boolean
}

// Step 2: Implement in androidMain
// File: shared/src/androidMain/kotlin/com/hazardhawk/platform/IDeviceInfo.android.kt
actual class AndroidDeviceInfo : IDeviceInfo {
    actual override fun getBatteryLevel(): Int { /* implementation */ }
    // ... other implementations
}

// Step 3: Implement in iosMain
// File: shared/src/iosMain/kotlin/com/hazardhawk/platform/IDeviceInfo.ios.kt
actual class IOSDeviceInfo : IDeviceInfo {
    actual override fun getBatteryLevel(): Int { /* implementation */ }
    // ... other implementations
}
```

#### Technical Debt to Address:
- Move all platform-specific code to `platform` package
- Create factory functions: `createPlatformDeviceInfo()`
- Add comprehensive KDoc for platform APIs

---

### Category 2: Type Constructor Mismatches (Priority: HIGH)
**Impact:** 120+ errors  
**Files Affected:** LiteRTModelEngine, TFLiteModelEngine, VertexAIClient

#### Issues:
1. **LiteRTPerformanceMetrics** - Wrong parameter names
   ```
   OLD: backendUsed, totalInferences, averageInferenceTime
   NEW: analysisCount, averageProcessingTimeMs, tokensPerSecond
   ```

2. **SafetyAnalysis** - Missing required parameters
   ```
   Missing: photoId, severity, aiConfidence
   Extra: confidence, detectedHazards, ppeDetections
   ```

3. **LiteRTAnalysisResult** - Constructor parameter mismatch
   ```
   OLD: confidenceThreshold, detectedHazards, ppeDetections, overallRiskLevel
   NEW: hazards, ppeStatus, oshaViolations, overallRiskAssessment, confidence
   ```

#### Root Cause:
Model consolidation changed constructor signatures without updating all call sites.

#### Refactoring Strategy:

**Approach A: Batch Constructor Alignment (RECOMMENDED)**
```kotlin
// Create migration utility class
object ModelConstructorMigration {
    fun createSafetyAnalysis(
        id: String,
        timestamp: Instant,
        // ... old parameters
    ): SafetyAnalysis {
        return SafetyAnalysis(
            id = id,
            photoId = id, // Derive from id
            timestamp = timestamp,
            severity = deriveSeverity(overallRiskLevel),
            aiConfidence = confidence,
            // ... map all parameters
        )
    }
    
    fun createPerformanceMetrics(
        // Old parameters
        backendUsed: String,
        totalInferences: Long,
        averageInferenceTime: Long
    ): LiteRTPerformanceMetrics {
        return LiteRTPerformanceMetrics(
            analysisCount = totalInferences,
            averageProcessingTimeMs = averageInferenceTime,
            tokensPerSecond = calculateTokensPerSec(averageInferenceTime),
            // ... map remaining
        )
    }
}
```

**Approach B: Factory Pattern (MAINTAINABLE)**
```kotlin
// Add factory methods to models
data class SafetyAnalysis(...) {
    companion object {
        fun fromLegacyAnalysis(
            id: String,
            confidence: Float,
            // ... legacy params
        ): SafetyAnalysis {
            return SafetyAnalysis(/* mapped parameters */)
        }
    }
}
```

#### Technical Debt to Address:
- Add `@Deprecated` annotations to old constructor patterns
- Create type-safe builder patterns for complex models
- Implement validation in factory methods

---

### Category 3: Unresolved References (Priority: HIGH)
**Impact:** 60+ errors  
**Common Patterns:** 20 distinct unresolved references

#### Top Unresolved References:
1. **`copy` (10 occurrences)** - Data class copy method not found
2. **`SecurityConstants` (9 occurrences)** - Missing security configuration
3. **`LiteRTHazardDetection` (9 occurrences)** - Renamed to `DetectedHazard`
4. **`ambientLightLux` (8 occurrences)** - Removed from models
5. **`uuid4` (6 occurrences)** - Should use `uuid()` from kotlinx-uuid
6. **`GPU/NPU/NNAPI` (15 occurrences)** - Enum reference issues

#### Root Cause Analysis:

**1. Data Class Copy Issues:**
```kotlin
// PROBLEM: Trying to copy with wrong parameter names
existingModel.copy(confidence = newValue)

// SOLUTION: Update parameter names to match new model
existingModel.copy(aiConfidence = newValue)
```

**2. Missing Constants:**
```kotlin
// PROBLEM: SecurityConstants moved/deleted
SecurityConstants.ENCRYPTION_KEY_SIZE

// SOLUTION: Create consolidated constants
object HazardHawkConstants {
    object Security {
        const val ENCRYPTION_KEY_SIZE = 256
        const val IV_LENGTH = 12
    }
}
```

**3. Type Name Changes:**
```kotlin
// PROBLEM: Old type names in LiteRT code
val detection: LiteRTHazardDetection

// SOLUTION: Update to unified types
val detection: DetectedHazard
```

#### Refactoring Strategy:

**Phase 1: Global Find/Replace (Safe)**
```bash
# Script to rename types consistently
find shared/src -name "*.kt" -type f -exec sed -i '' \
  -e 's/LiteRTHazardDetection/DetectedHazard/g' \
  -e 's/LiteRTPPEDetection/PPEDetection/g' \
  -e 's/LiteRTOSHAViolation/OSHAViolation/g' \
  {} +
```

**Phase 2: Import Cleanup (Automated)**
```kotlin
// Remove old imports
import com.hazardhawk.ai.models.SafetyAnalysis  // DELETE
import com.hazardhawk.models.SafetyAnalysis    // DELETE

// Add unified imports
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.core.models.OSHAViolation
import com.hazardhawk.core.models.Severity
```

**Phase 3: Constants Consolidation**
```kotlin
// Create: shared/src/commonMain/kotlin/com/hazardhawk/core/HazardHawkConstants.kt
object HazardHawkConstants {
    object Security {
        const val ENCRYPTION_KEY_SIZE = 256
        const val IV_LENGTH = 12
        const val MAX_RETRY_ATTEMPTS = 3
    }
    
    object Performance {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f
        const val MAX_BATCH_SIZE = 50
    }
    
    object API {
        const val REQUEST_TIMEOUT_MS = 30000L
        const val RETRY_DELAY_MS = 1000L
    }
}
```

---

### Category 4: Import Path Cleanup (Priority: MEDIUM)
**Impact:** 30+ files  
**Pattern:** Mixed import sources for same types

#### Current State:
```kotlin
// File A:
import com.hazardhawk.core.models.Severity

// File B:
import com.hazardhawk.security.EventSeverity

// File C:
import com.hazardhawk.models.Severity  // WRONG - deleted
```

#### Target State:
```kotlin
// All files should use:
import com.hazardhawk.core.models.Severity
import com.hazardhawk.core.models.HazardType
import com.hazardhawk.core.models.OSHAViolation
import com.hazardhawk.core.models.SafetyAnalysis
import com.hazardhawk.core.models.ComplianceStatus
```

#### Refactoring Strategy:

**Automated Import Standardization:**
```bash
#!/bin/bash
# Script: scripts/standardize_imports.sh

# Define import mappings
declare -A import_map=(
    ["com.hazardhawk.models.SafetyAnalysis"]="com.hazardhawk.core.models.SafetyAnalysis"
    ["com.hazardhawk.ai.models.SafetyAnalysis"]="com.hazardhawk.core.models.SafetyAnalysis"
    ["com.hazardhawk.models.OSHAViolation"]="com.hazardhawk.core.models.OSHAViolation"
    ["com.hazardhawk.models.Severity"]="com.hazardhawk.core.models.Severity"
)

# Apply transformations
for file in $(find shared/src/commonMain -name "*.kt"); do
    for old_import in "${!import_map[@]}"; do
        new_import="${import_map[$old_import]}"
        sed -i "s|import $old_import|import $new_import|g" "$file"
    done
done

echo "Import standardization complete"
```

---

### Category 5: Backup File Cleanup (Priority: LOW, Impact: High)
**Impact:** 25+ backup files cluttering codebase  
**Risk:** Confusion, accidental edits to wrong files

#### Files to Remove:
```
*.kt.bak1, *.kt.bak2, *.kt.bak3, *.kt.bak10, *.kt.bak11
*.kt.backup
*.kt.prefixbackup
*.kt.fix
```

#### Cleanup Strategy:
```bash
# Archive backups before deletion
tar -czf "backup_archive_$(date +%Y%m%d_%H%M%S).tar.gz" \
    $(find shared/src -name "*.kt.bak*" -o -name "*.kt.backup")

# Remove backup files
find shared/src -name "*.kt.bak*" -delete
find shared/src -name "*.kt.backup" -delete
find shared/src -name "*.kt.fix" -delete
find shared/src -name "*.kt.prefixbackup" -delete

echo "Backup cleanup complete - $(find shared/src -name "*.kt" | wc -l) clean files remain"
```

---

## Incremental Implementation Plan

### Phase 2.1: Platform API Resolution (CRITICAL PATH)
**Estimated Time:** 2-3 hours  
**Error Reduction:** ~70 errors

**Tasks:**
1. Create `IDeviceInfo` interface in commonMain
2. Implement Android-specific device APIs
3. Implement iOS-specific device APIs  
4. Add platform factory functions
5. Update `LiteRTDeviceOptimizer` to use interface

**Success Criteria:**
- All `has no corresponding expected declaration` errors resolved
- Platform-specific code isolated to platform source sets
- Cross-platform builds succeed

---

### Phase 2.2: Constructor Alignment (HIGH PRIORITY)
**Estimated Time:** 3-4 hours  
**Error Reduction:** ~120 errors

**Tasks:**
1. Create `ModelConstructorMigration` utility class
2. Add factory methods to core models
3. Update all LiteRT model instantiation sites
4. Update TFLite model instantiation sites
5. Update VertexAI client model creation
6. Add deprecation warnings to old patterns

**Success Criteria:**
- No "No parameter with name" errors
- No "No value passed for parameter" errors
- All model constructors use correct signatures

---

### Phase 2.3: Type Reference Cleanup (HIGH PRIORITY)
**Estimated Time:** 2-3 hours  
**Error Reduction:** ~60 errors

**Tasks:**
1. Run global type name replacements
2. Create `HazardHawkConstants` object
3. Update all `uuid4()` to `uuid()` or proper UUID generation
4. Fix enum reference issues (GPU, NPU, NNAPI)
5. Remove `ambientLightLux` references
6. Fix data class copy operations

**Success Criteria:**
- No "Unresolved reference" errors for types
- All constant references point to correct locations
- UUID generation uses proper KMP library

---

### Phase 2.4: Import Standardization (MEDIUM PRIORITY)
**Estimated Time:** 1-2 hours  
**Error Reduction:** ~30 errors

**Tasks:**
1. Create import standardization script
2. Run automated import replacement
3. Verify imports in all affected files
4. Remove unused imports
5. Add import order linting rules

**Success Criteria:**
- All imports use unified model package
- No duplicate or conflicting imports
- Import order follows Kotlin conventions

---

### Phase 2.5: Cleanup & Validation (LOW PRIORITY)
**Estimated Time:** 1 hour  
**Error Reduction:** 0 (improves maintainability)

**Tasks:**
1. Archive all backup files
2. Delete backup files from source tree
3. Run full compilation test
4. Generate error report for remaining issues
5. Update documentation

**Success Criteria:**
- No backup files in source tree
- Clean git status
- Compilation produces < 10 errors

---

## Batch Operation Recommendations

### Recommended Batch Fixes

#### Batch 1: Automated Renaming (SAFE)
```bash
# Type name standardization
find shared/src -name "*.kt" -type f -exec sed -i '' \
  -e 's/LiteRTHazardDetection/DetectedHazard/g' \
  -e 's/LiteRTPPEDetection/PPEDetection/g' \
  -e 's/\.uuid4()/\.uuid()/g' \
  {} +
```

**Risk:** LOW  
**Impact:** ~20 errors fixed  
**Reversible:** Yes (git revert)

#### Batch 2: Import Path Updates (MODERATE RISK)
```bash
# Run import standardization script
./scripts/standardize_imports.sh
```

**Risk:** MODERATE  
**Impact:** ~30 errors fixed  
**Reversible:** Yes (git revert)  
**Validation:** Requires full compilation test

#### Batch 3: Backup File Removal (SAFE)
```bash
# Archive and remove backup files
./scripts/cleanup_backups.sh
```

**Risk:** LOW  
**Impact:** 0 errors fixed, improves maintainability  
**Reversible:** Yes (backup archive created)

### Individual Fix Recommendations

#### Fix Individually:
1. **Platform API gaps** - Requires careful KMP architecture
2. **Constructor mismatches** - Need case-by-case parameter mapping
3. **SecurityConstants references** - May have security implications
4. **Model copy operations** - Data integrity concerns

#### Reasoning:
These changes require understanding business logic and can't be safely automated. Each fix needs:
- Context understanding
- Data validation
- Security review (for crypto/auth code)
- Backward compatibility checks

---

## Code Simplification Opportunities

### 1. Eliminate Duplicate Model Types

**Before:**
```kotlin
// Multiple packages with similar models
com.hazardhawk.ai.models.SafetyAnalysis
com.hazardhawk.models.SafetyAnalysis
com.hazardhawk.core.models.SafetyAnalysis
```

**After:**
```kotlin
// Single source of truth
com.hazardhawk.core.models.SafetyAnalysis

// Legacy support with type aliases
package com.hazardhawk.models
typealias SafetyAnalysis = com.hazardhawk.core.models.SafetyAnalysis
```

### 2. Consolidate Enum Definitions

**Before:**
```kotlin
// Scattered enum definitions
enum class Severity { LOW, MEDIUM, HIGH }  // in models
enum class EventSeverity { LOW, HIGH }     // in security
enum class OSHASeverity { SERIOUS, ... }   // in OSHA
```

**After:**
```kotlin
// Unified enums with domain context
package com.hazardhawk.core.models

enum class Severity { LOW, MEDIUM, HIGH, CRITICAL }
enum class OSHASeverity { IMMINENT_DANGER, SERIOUS, ... }

// Context-specific adapters
fun Severity.toOSHASeverity(): OSHASeverity = when (this) {
    LOW -> OSHASeverity.OTHER_THAN_SERIOUS
    MEDIUM -> OSHASeverity.SERIOUS
    HIGH, CRITICAL -> OSHASeverity.IMMINENT_DANGER
}
```

### 3. Factory Pattern for Complex Models

**Before:**
```kotlin
// Repetitive construction code
val analysis = SafetyAnalysis(
    id = UUID.randomUUID().toString(),
    photoId = photoId,
    timestamp = Clock.System.now(),
    analysisType = AnalysisType.LOCAL_LITERT_VISION,
    workType = workType,
    hazards = hazards,
    ppeStatus = ppeStatus,
    oshaViolations = violations,
    recommendations = recommendations,
    overallRiskLevel = RiskLevel.LOW,
    severity = Severity.MEDIUM,
    aiConfidence = confidence,
    processingTimeMs = processingTime
)
```

**After:**
```kotlin
// Factory pattern with defaults
val analysis = SafetyAnalysis.Builder()
    .forPhoto(photoId)
    .withWorkType(workType)
    .addHazards(hazards)
    .withPPEStatus(ppeStatus)
    .withConfidence(confidence)
    .build()

// Or DSL approach
val analysis = safetyAnalysis {
    photoId(photoId)
    workType(workType)
    hazards(hazards)
    confidence(confidence)
}
```

### 4. Reduce Parameter Coupling

**Before:**
```kotlin
fun createAnalysis(
    id: String,
    photoId: String,
    timestamp: Instant,
    analysisType: AnalysisType,
    workType: WorkType,
    hazards: List<Hazard>,
    ppeStatus: PPEStatus?,
    violations: List<OSHAViolation>,
    recommendations: List<String>,
    overallRisk: RiskLevel,
    severity: Severity,
    confidence: Float,
    processingTime: Long
): SafetyAnalysis
```

**After:**
```kotlin
// Group related parameters
data class AnalysisContext(
    val id: String = UUID.randomUUID().toString(),
    val photoId: String,
    val timestamp: Instant = Clock.System.now(),
    val analysisType: AnalysisType,
    val workType: WorkType
)

data class AnalysisResults(
    val hazards: List<Hazard>,
    val ppeStatus: PPEStatus?,
    val violations: List<OSHAViolation>,
    val recommendations: List<String>
)

fun createAnalysis(
    context: AnalysisContext,
    results: AnalysisResults,
    metrics: AnalysisMetrics
): SafetyAnalysis
```

---

## Type Safety Improvements

### 1. Replace String Types with Type-Safe Alternatives

**Before:**
```kotlin
data class OSHACode(
    val code: String,  // Could be anything
    val url: String?   // Could be invalid URL
)
```

**After:**
```kotlin
@JvmInline
value class OSHACodeId(val value: String) {
    init {
        require(value.matches(Regex("\\d+\\.\\d+"))) {
            "Invalid OSHA code format"
        }
    }
}

data class OSHACode(
    val code: OSHACodeId,
    val url: Url?  // Use kotlinx-io Url type
)
```

### 2. Non-Null Defaults Instead of Nullable + Default

**Before:**
```kotlin
data class Hazard(
    val id: String,
    val recommendations: List<String> = emptyList(),  // Can be null
    val immediateAction: String? = null               // Often set
)
```

**After:**
```kotlin
data class Hazard(
    val id: String,
    val recommendations: List<String> = emptyList(),  // Non-null
    val immediateAction: ImmediateAction = ImmediateAction.None  // Sealed class
)

sealed class ImmediateAction {
    data object None : ImmediateAction()
    data class Required(val action: String) : ImmediateAction()
}
```

### 3. Sealed Classes for State Machines

**Before:**
```kotlin
// Boolean flags that can create invalid states
data class AnalysisState(
    val isProcessing: Boolean,
    val isComplete: Boolean,
    val hasError: Boolean,
    val errorMessage: String?
)
```

**After:**
```kotlin
sealed class AnalysisState {
    data object Idle : AnalysisState()
    data class Processing(val progress: Float) : AnalysisState()
    data class Complete(val result: SafetyAnalysis) : AnalysisState()
    data class Error(val message: String, val cause: Throwable?) : AnalysisState()
}
```

---

## Maintainable Code Structure

### Package Organization

**Current Issues:**
- Models scattered across multiple packages
- Platform code mixed with common code
- No clear separation of concerns

**Proposed Structure:**
```
shared/src/commonMain/kotlin/com/hazardhawk/
├── core/                      # Core domain models and logic
│   ├── models/               # Unified data models
│   │   ├── SafetyAnalysis.kt
│   │   ├── OSHAModels.kt
│   │   └── CommonEnums.kt
│   ├── constants/            # Application constants
│   │   └── HazardHawkConstants.kt
│   └── migration/            # Model migration utilities
│       └── ModelMigration.kt
│
├── domain/                   # Business logic
│   ├── repositories/         # Repository interfaces
│   ├── usecases/            # Use case implementations
│   └── services/            # Domain services
│
├── data/                     # Data layer
│   ├── repositories/        # Repository implementations
│   ├── models/              # Data transfer objects
│   └── cloud/               # Cloud service clients
│
├── ai/                       # AI/ML services
│   ├── core/                # Core AI interfaces
│   ├── services/            # AI service implementations
│   ├── litert/              # LiteRT specific
│   ├── yolo/                # YOLO specific
│   └── models/              # AI-specific models
│
└── platform/                 # Platform abstractions
    ├── IDeviceInfo.kt       # Device capability interface
    └── Factories.kt         # Platform factory functions
```

### Dependency Direction

**Principle:** Dependencies should flow inward
```
Presentation → Domain ← Data
     ↓
  Platform ← AI Services
```

**Rules:**
1. Core models depend on nothing
2. Domain depends only on core
3. Data implements domain interfaces
4. Platform code is isolated to platform source sets

---

## Risk Assessment & Mitigation

### High-Risk Changes

#### 1. Platform API Refactoring
**Risk:** Breaking cross-platform compilation  
**Mitigation:**
- Implement Android first, validate
- Add iOS stubs immediately
- Test both platforms after each change
- Keep expect/actual declarations simple

#### 2. Constructor Signature Changes
**Risk:** Runtime crashes from missed call sites  
**Mitigation:**
- Use factory methods instead of direct changes
- Add @Deprecated to old constructors
- Create comprehensive migration guide
- Add runtime validation in constructors

### Medium-Risk Changes

#### 3. Import Path Updates
**Risk:** Compilation failures across many files  
**Mitigation:**
- Use automated script with validation
- Test compilation after each batch
- Keep git history clean for easy revert
- Run on feature branch first

#### 4. Type Name Changes
**Risk:** Breaking external dependencies  
**Mitigation:**
- Add type aliases for backward compatibility
- Document breaking changes
- Version the shared module
- Provide migration path

### Low-Risk Changes

#### 5. Backup File Removal
**Risk:** Losing useful code  
**Mitigation:**
- Archive before deletion
- Review each backup file
- Keep archive for 30 days
- Document what was removed

---

## Success Metrics

### Quantitative Metrics
- **Error Count:** Target < 10 compilation errors
- **Build Time:** Target < 2 minutes for full build
- **Code Coverage:** Maintain > 70% for modified files
- **Duplicate Code:** Reduce by 50%

### Qualitative Metrics
- **Code Readability:** All models have clear documentation
- **Maintainability:** New team members can understand structure
- **Type Safety:** Compile-time safety for all model operations
- **Consistency:** All similar patterns follow same approach

### Validation Checklist
- [ ] All platforms compile successfully
- [ ] No backup files in source tree
- [ ] All imports use unified package structure
- [ ] All models have consistent constructor patterns
- [ ] Platform-specific code isolated properly
- [ ] Documentation updated for breaking changes
- [ ] Migration guide created for external consumers

---

## Next Steps

### Immediate Actions (Today)
1. Review this strategy with team
2. Create feature branch: `refactor/phase2-systematic-cleanup`
3. Start with Phase 2.1 (Platform APIs)
4. Run validation tests after each phase

### Short-Term (This Week)
1. Complete Phases 2.1 through 2.3
2. Reduce error count to < 50
3. Update project documentation
4. Create migration guide

### Medium-Term (Next Week)
1. Complete Phases 2.4 and 2.5
2. Achieve < 10 compilation errors
3. Conduct code review
4. Merge to main branch

---

## References

### Related Documentation
- `docs/implementation/20251009-173000-phase2-completion-summary.md`
- `docs/plan/20251010-094500-phase2-next-steps-plan.md`
- `CLAUDE.md` - Project architecture guidelines

### Key Files to Reference
- `/Users/aaron/Apps-Coded/HH-v0-fresh/shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt`
- `/Users/aaron/Apps-Coded/HH-v0-fresh/shared/src/commonMain/kotlin/com/hazardhawk/core/models/OSHAAnalysisResult.kt`
- `/Users/aaron/Apps-Coded/HH-v0-fresh/shared/src/commonMain/kotlin/com/hazardhawk/ai/litert/LiteRTDeviceOptimizer.kt`

---

**End of Strategy Document**
