# Phase 2 Build Error Fixes - Unified Implementation Plan

**Document Date:** October 10, 2025
**Current Status:** 728 errors (59% reduction from 1,771 baseline)
**Target:** Zero compilation errors
**Estimated Time to Completion:** 4-6 hours

---

## Executive Summary

### Problem Statement
Phase 2 build has 728 compilation errors stemming from:
1. **Model Architecture Conflicts** - Multiple competing model definitions
2. **Type System Issues** - Import mismatches and missing definitions
3. **Platform Implementation Gaps** - Missing expect/actual implementations
4. **Parameter Signature Mismatches** - SafetyAnalysis and OSHAViolation construction errors

### Solution Overview
A **4-phase systematic fix** targeting root causes in dependency order:
- **Phase 1:** Model consolidation and enum fixes (2 hours) → ~400 errors
- **Phase 2:** Type system alignment (1.5 hours) → ~200 errors
- **Phase 3:** Platform implementations (1 hour) → ~50 errors
- **Phase 4:** Final integration fixes (1 hour) → 0 errors

### Success Metrics
- **Build Success:** `./gradlew :shared:build` completes without errors
- **Test Success:** `./gradlew :shared:test` passes all tests
- **Type Safety:** No type erasure warnings or unsafe casts
- **KMP Compliance:** All commonMain code is platform-independent

### Realistic Timeline Reconciliation

**Agent Estimates Analysis:**
- **simple-architect:** 8 minutes (imports only - incomplete scope)
- **refactor-master:** 15-20 hours (comprehensive but conservative)
- **test-guardian:** 33 minutes (testing only, not implementation)
- **loveable-ux:** Embedded in implementation phases

**Unified Estimate:** **4-6 hours** (practical middle ground)
- Based on current 728 errors (down from 1,771)
- Accounts for cascading error resolution
- Includes verification and testing time
- Assumes single developer, sequential execution

---

## Current State Analysis

### Error Distribution (728 total)

| Category | Count | % | Priority | Time Est |
|----------|-------|---|----------|----------|
| Model parameter mismatches | ~280 | 38% | P0 | 2h |
| Missing enum values | ~120 | 16% | P0 | 30min |
| Import/package conflicts | ~95 | 13% | P1 | 45min |
| Type incompatibilities | ~85 | 12% | P1 | 1h |
| Platform expect/actual | ~60 | 8% | P1 | 1h |
| OSHAViolation signature issues | ~45 | 6% | P1 | 30min |
| Miscellaneous | ~43 | 6% | P2 | 45min |

### Critical Files (Top Error Sources)

```
1. HybridAIServiceFacade.kt          - 85 errors (SafetyAnalysis construction)
2. ConstructionHazardMapper.kt       - 72 errors (model mismatches)
3. YOLO11SafetyAnalyzer.kt          - 68 errors (parameter issues)
4. LiteRTDeviceOptimizer.kt         - 60 errors (platform functions)
5. LiveOSHAAnalyzer.kt              - 48 errors (OSHAViolation params)
6. SimpleOSHAAnalyzer.kt            - 42 errors (OSHAViolation params)
7. GeminiVisionAnalyzer.kt          - 35 errors (enum values)
8. SmartAIOrchestrator.kt           - 28 errors (type mismatches)
9. YOLO11SafetyAnalyzerExample.kt   - 24 errors (Severity enum conflicts)
10. GeminiSafetyAnalysisAdapter.kt  - 18 errors (incompatible enums)
```

### Root Causes (Prioritized)

**RC1: SafetyAnalysis Parameter Changes (280 errors - 38%)**
- Missing required parameters: `timestamp`, `workType`, `overallRiskLevel`, `processingTimeMs`
- Removed parameters: `analyzedAt`, `oshaCodes`
- Impact: Every file constructing SafetyAnalysis

**RC2: Missing HazardType Enum Values (120 errors - 16%)**
- `CRANE_LIFTING`, `CONFINED_SPACE`, `STEEL_WORK`, `ELECTRICAL_SAFETY`, `OTHER`
- Impact: AI analyzers and hazard mappers

**RC3: Duplicate Model Definitions (95 errors - 13%)**
- `com.hazardhawk.core.models.Severity` vs `com.hazardhawk.models.Severity`
- `com.hazardhawk.core.models.AnalysisType` vs `com.hazardhawk.models.AnalysisType`
- `com.hazardhawk.core.models.BoundingBox` vs `com.hazardhawk.models.BoundingBox`

**RC4: OSHAViolation Parameter Changes (85 errors - 12%)**
- Missing required: `code`, `title`, `severity`
- Removed: `violationId`, `oshaStandard`, `standardTitle`, `violationType`, `potentialPenalty`, `timeframe`
- Wrong type: Expects `OSHAViolation` but gets `OSHADetailedViolation`

**RC5: Platform Implementation Gaps (60 errors - 8%)**
- `PlatformDeviceInfo` expect/actual mismatch
- Functions incorrectly defined as member functions

---

## Implementation Plan

### Phase 1: Foundation - Model Consolidation (2 hours)

**Goal:** Fix root causes that block other fixes (RC2, RC3, RC4)
**Impact:** Resolve ~400 errors (55% of remaining)

#### Milestone 1.1: Add Missing Enum Values (30 minutes)

**File:** `shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt`

**Task 1.1.1:** Add missing HazardType values
```kotlin
enum class HazardType {
    // Existing values
    FALL_HAZARD,
    ELECTRICAL_HAZARD,
    STRUCK_BY,
    CAUGHT_BETWEEN,
    SCAFFOLDING_ISSUE,
    LADDER_SAFETY,
    EXCAVATION,
    PPE_VIOLATION,
    HOUSEKEEPING,
    FIRE_HAZARD,
    CHEMICAL_EXPOSURE,
    NOISE_EXPOSURE,
    RESPIRATORY,
    MACHINE_GUARDING,
    LOCKOUT_TAGOUT,

    // NEW - Add these
    CRANE_LIFTING,
    CONFINED_SPACE,
    STEEL_WORK,
    ELECTRICAL_SAFETY,
    OTHER,
    UNKNOWN
}
```

**Files Affected:** 12 files
**Errors Fixed:** ~120

**Verification:**
```bash
grep -r "CRANE_LIFTING\|CONFINED_SPACE\|STEEL_WORK\|ELECTRICAL_SAFETY" shared/src/commonMain --include="*.kt" | grep "Unresolved"
# Expected: No results
```

---

#### Milestone 1.2: Fix OSHAViolation Constructor (45 minutes)

**Problem:** Two different OSHAViolation signatures in use

**Investigation Required:**
1. Check current OSHAViolation definition in SafetyAnalysis.kt
2. Identify all construction sites
3. Determine correct parameter set

**File:** `shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt`

**Current Definition (assumed):**
```kotlin
@Serializable
data class OSHAViolation(
    val code: String,              // OSHA regulation code (e.g., "1926.501")
    val title: String,             // Human-readable title
    val description: String,       // Detailed description
    val severity: ViolationSeverity, // SERIOUS, WILLFUL, REPEAT, etc.
    val category: String?,         // Optional category
    val recommendations: List<String> = emptyList(),
    val potentialFine: String? = null,
    val correctiveActions: List<String> = emptyList(),
    val complianceDeadline: String? = null,
    val boundingBox: BoundingBox? = null
)
```

**Task 1.2.1:** Update LiveOSHAAnalyzer.kt (lines 232-239)
```kotlin
// BEFORE (causing errors)
OSHAViolation(
    violationId = UUID.randomUUID().toString(),
    oshaStandard = item.code,
    standardTitle = item.title,
    violationType = mapSeverity(item.severity),
    description = item.description,
    potentialPenalty = item.penaltyRange,
    recommendations = item.correctiveActions,
    timeframe = item.complianceTimeframe
)

// AFTER (matches current signature)
OSHAViolation(
    code = item.code,
    title = item.title,
    description = item.description,
    severity = mapSeverity(item.severity),
    category = null,
    recommendations = item.correctiveActions,
    potentialFine = item.penaltyRange,
    correctiveActions = item.correctiveActions,
    complianceDeadline = item.complianceTimeframe,
    boundingBox = null
)
```

**Task 1.2.2:** Update SimpleOSHAAnalyzer.kt (lines 119-134)
**Task 1.2.3:** Check for OSHADetailedViolation vs OSHAViolation type mismatches

**Files to Update:**
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/LiveOSHAAnalyzer.kt`
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/SimpleOSHAAnalyzer.kt`

**Errors Fixed:** ~85

**Verification:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "OSHAViolation"
# Expected: No parameter errors
```

---

#### Milestone 1.3: Eliminate Duplicate Model Imports (45 minutes)

**Problem:** Files import from both `core.models` and `models` packages

**Task 1.3.1:** Identify all duplicate model files
```bash
# Find duplicate Severity definitions
find shared/src/commonMain/kotlin -name "*.kt" -exec grep -l "enum class Severity" {} \;

# Find duplicate AnalysisType definitions
find shared/src/commonMain/kotlin -name "*.kt" -exec grep -l "enum class AnalysisType" {} \;

# Find duplicate BoundingBox definitions
find shared/src/commonMain/kotlin -name "*.kt" -exec grep -l "data class BoundingBox" {} \;
```

**Task 1.3.2:** Delete old model definitions
```bash
# Only keep versions in com.hazardhawk.core.models
# Delete duplicates in com.hazardhawk.models (if they exist)
```

**Task 1.3.3:** Update all imports using automated script
```bash
#!/bin/bash
# Fix duplicate model imports

find shared/src/commonMain/kotlin -name "*.kt" -type f -exec sed -i '' \
  -e 's/import com\.hazardhawk\.models\.Severity/import com.hazardhawk.core.models.Severity/g' \
  -e 's/import com\.hazardhawk\.models\.AnalysisType/import com.hazardhawk.core.models.AnalysisType/g' \
  -e 's/import com\.hazardhawk\.models\.BoundingBox/import com.hazardhawk.core.models.BoundingBox/g' \
  -e 's/import com\.hazardhawk\.models\.HazardType/import com.hazardhawk.core.models.HazardType/g' \
  -e 's/import com\.hazardhawk\.models\.RiskLevel/import com.hazardhawk.core.models.RiskLevel/g' \
  {} +

echo "✅ Duplicate model imports fixed"
```

**Files Affected:** ~25 files
**Errors Fixed:** ~95

**Verification:**
```bash
# Check for incompatible enum comparisons
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "incompatible enums"
# Expected: No results
```

---

**Phase 1 Checkpoint:**
```bash
# Expected errors after Phase 1: ~325 (down from 728)
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
git add -A
git commit -m "Phase 1: Model consolidation - add missing enums, fix OSHAViolation, eliminate duplicates"
```

---

### Phase 2: Type System Alignment (1.5 hours)

**Goal:** Fix SafetyAnalysis constructor calls across all AI services
**Impact:** Resolve ~280 errors (85% of remaining after Phase 1)

#### Milestone 2.1: SafetyAnalysis Parameter Updates (1 hour)

**Current SafetyAnalysis Constructor (confirmed):**
```kotlin
@Serializable
data class SafetyAnalysis(
    val id: String,
    val timestamp: Long,                    // REQUIRED
    val analysisType: AnalysisType,
    val workType: WorkType,                 // REQUIRED
    val hazards: List<Hazard> = emptyList(),
    val ppeStatus: PPEStatus? = null,
    val oshaViolations: List<OSHAViolation> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val overallRiskLevel: RiskLevel,        // REQUIRED
    val confidence: Float? = null,
    val processingTimeMs: Long,             // REQUIRED
    val metadata: AnalysisMetadata? = null
)
```

**Removed Parameters (causing errors):**
- `analyzedAt: Instant` → replaced by `timestamp: Long`
- `oshaCodes: List<OSHACode>` → removed (use oshaViolations instead)

**Task 2.1.1:** Fix HybridAIServiceFacade.kt (lines 529-534, 572-577)

**Before (BROKEN):**
```kotlin
SafetyAnalysis(
    id = id,
    photoId = photoId,
    analysisType = AnalysisType.YOLO_LOCAL,
    hazards = hazards,
    ppeStatus = ppeStatus,
    oshaCodes = oshaCodes,            // ❌ REMOVED
    oshaViolations = oshaViolations,
    recommendations = recommendations,
    confidence = confidence,
    analyzedAt = Clock.System.now(),  // ❌ REMOVED
    // ❌ MISSING: timestamp, workType, overallRiskLevel, processingTimeMs
    metadata = metadata
)
```

**After (FIXED):**
```kotlin
SafetyAnalysis(
    id = id,
    timestamp = Clock.System.now().toEpochMilliseconds(),  // ✅ ADDED
    analysisType = AnalysisType.YOLO_LOCAL,
    workType = workType,                                   // ✅ ADDED (from function param)
    hazards = hazards,
    ppeStatus = ppeStatus,
    oshaViolations = oshaViolations,
    recommendations = recommendations,
    overallRiskLevel = calculateRiskLevel(hazards),        // ✅ ADDED (compute from hazards)
    confidence = confidence,
    processingTimeMs = processingTimeMs,                   // ✅ ADDED (from function param)
    metadata = metadata
)

// Add helper function if not exists
private fun calculateRiskLevel(hazards: List<Hazard>): RiskLevel {
    if (hazards.isEmpty()) return RiskLevel.LOW
    val maxSeverity = hazards.maxOf { it.severity }
    return when (maxSeverity) {
        Severity.CRITICAL -> RiskLevel.SEVERE
        Severity.HIGH -> RiskLevel.HIGH
        Severity.MODERATE -> RiskLevel.MODERATE
        Severity.LOW -> RiskLevel.LOW
        Severity.NEGLIGIBLE -> RiskLevel.LOW
    }
}
```

**Files to Update (10 files):**
1. `HybridAIServiceFacade.kt` (2 locations)
2. `ConstructionHazardMapper.kt` (1 location)
3. `YOLO11SafetyAnalyzer.kt` (1 location)
4. `YOLO11SafetyAnalyzerExample.kt` (if used in tests)
5. Any other files with SafetyAnalysis construction errors

**Errors Fixed:** ~280

**Script for Automated Fix (if parameters are consistent):**
```bash
#!/bin/bash
# Fix SafetyAnalysis constructor calls

# This is complex - may require manual fixes per file
# Script can help identify locations:

echo "Files with SafetyAnalysis construction:"
grep -rn "SafetyAnalysis(" shared/src/commonMain/kotlin --include="*.kt" \
  | grep -v "data class SafetyAnalysis" \
  | grep -v "import"

echo ""
echo "Check each file for missing parameters:"
echo "  - timestamp: Long"
echo "  - workType: WorkType"
echo "  - overallRiskLevel: RiskLevel"
echo "  - processingTimeMs: Long"
```

**Verification:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "No value passed for parameter"
# Expected: No results
```

---

#### Milestone 2.2: Fix Type Mismatches (30 minutes)

**Task 2.2.1:** Fix SmartAIOrchestrator.kt getOrDefault() calls (lines 332, 336)

**Problem:** Incorrect usage of Result.getOrDefault()

**Before:**
```kotlin
val analysisType = result.getOrDefault(AnalysisType.SIMPLE_ANALYSIS, "Failed to analyze")
```

**After:**
```kotlin
val analysis = result.getOrElse {
    // Return a default SafetyAnalysis on error
    SafetyAnalysis(
        id = UUID.randomUUID().toString(),
        timestamp = Clock.System.now().toEpochMilliseconds(),
        analysisType = AnalysisType.SIMPLE_ANALYSIS,
        workType = workType,
        hazards = emptyList(),
        overallRiskLevel = RiskLevel.LOW,
        processingTimeMs = 0L,
        metadata = AnalysisMetadata(
            modelName = "fallback",
            errorMessage = it.message
        )
    )
}
```

**Task 2.2.2:** Fix AIServiceFactory.kt capability list/set mismatch (line 189)

**Before:**
```kotlin
val capabilities = listOf(AnalysisCapability.HAZARD_DETECTION)
// Then used where Set<AnalysisCapability> expected
```

**After:**
```kotlin
val capabilities = setOf(AnalysisCapability.HAZARD_DETECTION)
```

**Errors Fixed:** ~28

---

**Phase 2 Checkpoint:**
```bash
# Expected errors after Phase 2: ~45 (down from 325)
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
git add -A
git commit -m "Phase 2: Type system alignment - fix SafetyAnalysis constructors and type mismatches"
```

---

### Phase 3: Platform Implementations (1 hour)

**Goal:** Fix expect/actual mismatches for PlatformDeviceInfo
**Impact:** Resolve ~60 errors

#### Milestone 3.1: Refactor PlatformDeviceInfo (1 hour)

**Problem:** `expect object PlatformDeviceInfo` with member functions doesn't work in KMP

**Current (BROKEN):**
```kotlin
// LiteRTDeviceOptimizer.kt:319
expect object PlatformDeviceInfo {
    fun detectDeviceTier(): DeviceTier          // ❌ Member functions not allowed
    fun getCurrentThermalState(): ThermalState  // ❌ in expect object
    fun getMemoryInfo(): MemoryInfo             // ❌
}

// Usage (line 63-65):
val deviceTier = PlatformDeviceInfo.detectDeviceTier()           // ❌ Unresolved
val thermalState = PlatformDeviceInfo.getCurrentThermalState()   // ❌ Unresolved
val memoryInfo = PlatformDeviceInfo.getMemoryInfo()              // ❌ Unresolved
```

**Solution:** Interface + Factory Pattern

**Task 3.1.1:** Create platform interface in commonMain

**File:** `shared/src/commonMain/kotlin/com/hazardhawk/platform/IDeviceInfo.kt`
```kotlin
package com.hazardhawk.platform

interface IDeviceInfo {
    fun detectDeviceTier(): DeviceTier
    fun getCurrentThermalState(): ThermalState
    fun getMemoryInfo(): MemoryInfo
    fun getBatteryLevel(): Int
    fun isPowerSaveModeEnabled(): Boolean
}

// Factory function
expect fun createPlatformDeviceInfo(): IDeviceInfo

// Helper enums (may already exist elsewhere - check before adding)
enum class DeviceTier {
    LOW_END, MID_RANGE, HIGH_END
}

enum class ThermalState {
    NOMINAL, FAIR, SERIOUS, CRITICAL
}

data class MemoryInfo(
    val totalMb: Int,
    val freeMb: Int,
    val usedMb: Int
)
```

**Task 3.1.2:** Implement for Android

**File:** `shared/src/androidMain/kotlin/com/hazardhawk/platform/AndroidDeviceInfo.kt`
```kotlin
package com.hazardhawk.platform

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import kotlin.math.roundToInt

class AndroidDeviceInfo(private val context: Context) : IDeviceInfo {

    override fun detectDeviceTier(): DeviceTier {
        val cores = Runtime.getRuntime().availableProcessors()
        val memoryMb = (Runtime.getRuntime().maxMemory() / 1024 / 1024).toInt()

        return when {
            cores >= 8 && memoryMb >= 6144 -> DeviceTier.HIGH_END
            cores >= 4 && memoryMb >= 3072 -> DeviceTier.MID_RANGE
            else -> DeviceTier.LOW_END
        }
    }

    override fun getCurrentThermalState(): ThermalState {
        // Android 10+ thermal API
        // Simplified implementation - expand as needed
        return ThermalState.NOMINAL
    }

    override fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        return MemoryInfo(
            totalMb = (runtime.maxMemory() / 1024 / 1024).toInt(),
            freeMb = (runtime.freeMemory() / 1024 / 1024).toInt(),
            usedMb = ((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024).toInt()
        )
    }

    override fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
    }

    override fun isPowerSaveModeEnabled(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isPowerSaveMode ?: false
    }
}

// Factory implementation
actual fun createPlatformDeviceInfo(): IDeviceInfo {
    // Note: Context needs to be provided via dependency injection
    // This is a placeholder - actual implementation needs proper DI setup
    throw NotImplementedError("AndroidDeviceInfo requires Context - use DI to provide")
}
```

**Task 3.1.3:** Implement for iOS

**File:** `shared/src/iosMain/kotlin/com/hazardhawk/platform/IOSDeviceInfo.kt`
```kotlin
package com.hazardhawk.platform

import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice

class IOSDeviceInfo : IDeviceInfo {

    override fun detectDeviceTier(): DeviceTier {
        // Simplified - in production, check device model string
        val processorCount = NSProcessInfo.processInfo.processorCount.toInt()
        return when {
            processorCount >= 6 -> DeviceTier.HIGH_END
            processorCount >= 4 -> DeviceTier.MID_RANGE
            else -> DeviceTier.LOW_END
        }
    }

    override fun getCurrentThermalState(): ThermalState {
        return when (NSProcessInfo.processInfo.thermalState) {
            0L -> ThermalState.NOMINAL
            1L -> ThermalState.FAIR
            2L -> ThermalState.SERIOUS
            3L -> ThermalState.CRITICAL
            else -> ThermalState.NOMINAL
        }
    }

    override fun getMemoryInfo(): MemoryInfo {
        // iOS doesn't expose detailed memory APIs
        // Return approximate values
        val physicalMemory = NSProcessInfo.processInfo.physicalMemory
        val totalMb = (physicalMemory / 1024 / 1024).toInt()
        return MemoryInfo(
            totalMb = totalMb,
            freeMb = totalMb / 2,  // Approximation
            usedMb = totalMb / 2   // Approximation
        )
    }

    override fun getBatteryLevel(): Int {
        UIDevice.currentDevice.batteryMonitoringEnabled = true
        return (UIDevice.currentDevice.batteryLevel * 100).toInt()
    }

    override fun isPowerSaveModeEnabled(): Boolean {
        return NSProcessInfo.processInfo.lowPowerModeEnabled
    }
}

actual fun createPlatformDeviceInfo(): IDeviceInfo = IOSDeviceInfo()
```

**Task 3.1.4:** Update LiteRTDeviceOptimizer.kt usage

**File:** `shared/src/commonMain/kotlin/com/hazardhawk/ai/litert/LiteRTDeviceOptimizer.kt`

**Before:**
```kotlin
// Remove this broken expect object declaration
expect object PlatformDeviceInfo {
    fun detectDeviceTier(): DeviceTier
    fun getCurrentThermalState(): ThermalState
    fun getMemoryInfo(): MemoryInfo
}

// Usage (lines 63-65)
val deviceTier = PlatformDeviceInfo.detectDeviceTier()
val thermalState = PlatformDeviceInfo.getCurrentThermalState()
val memoryInfo = PlatformDeviceInfo.getMemoryInfo()
```

**After:**
```kotlin
import com.hazardhawk.platform.createPlatformDeviceInfo

class LiteRTDeviceOptimizer(
    private val deviceInfo: IDeviceInfo = createPlatformDeviceInfo()
) {
    // Usage
    val deviceTier = deviceInfo.detectDeviceTier()
    val thermalState = deviceInfo.getCurrentThermalState()
    val memoryInfo = deviceInfo.getMemoryInfo()
}
```

**Task 3.1.5:** Fix suspend function call (line 225)

**Before:**
```kotlin
val capabilities = analyzeDeviceCapabilities()  // ❌ Suspend function called from non-suspend
```

**After:**
```kotlin
// Option 1: Make caller suspend
suspend fun optimizeForDevice() {
    val capabilities = analyzeDeviceCapabilities()
}

// Option 2: Use coroutine scope
fun optimizeForDevice() {
    CoroutineScope(Dispatchers.Default).launch {
        val capabilities = analyzeDeviceCapabilities()
    }
}
```

**Errors Fixed:** ~60

**Verification:**
```bash
./gradlew :shared:compileKotlinMetadata
# Expected: Success - no expect/actual errors
```

---

**Phase 3 Checkpoint:**
```bash
# Expected errors after Phase 3: ~15-20
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
git add -A
git commit -m "Phase 3: Platform implementations - refactor PlatformDeviceInfo to interface pattern"
```

---

### Phase 4: Final Integration (1 hour)

**Goal:** Fix remaining miscellaneous errors
**Impact:** Achieve zero compilation errors

#### Milestone 4.1: Fix Remaining Type Issues (30 minutes)

**Task 4.1.1:** Fix ModelMigrationUtils.kt underscore conflicts (lines 85-87, 102+)

**Problem:** Kotlin doesn't allow multiple `_` unused variable declarations

**Before:**
```kotlin
val (_, _, _) = oldModel  // ❌ Conflicting declarations
```

**After:**
```kotlin
val (unused1, unused2, unused3) = oldModel  // ✅ Or use destructuring properly
// Or just access properties directly without destructuring
```

**Task 4.1.2:** Fix YOLO11SafetyAnalyzer.kt missing parameters (line 467)

Already covered in Phase 2 SafetyAnalysis fixes.

**Task 4.1.3:** Fix any remaining enum issues

Check for unresolved enum values and add to appropriate enums.

---

#### Milestone 4.2: Clean Build Verification (30 minutes)

**Task 4.2.1:** Run full compilation
```bash
./gradlew clean
./gradlew :shared:compileKotlinMetadata
./gradlew :shared:compileKotlinAndroid
./gradlew :shared:compileKotlinIosSimulatorArm64
```

**Task 4.2.2:** Run tests
```bash
./gradlew :shared:testDebugUnitTest
./gradlew :shared:testReleaseUnitTest
```

**Task 4.2.3:** Fix any remaining individual errors
- Use compiler output to guide final fixes
- Most should be simple import or parameter issues

---

**Phase 4 Checkpoint:**
```bash
# Expected errors: 0
./gradlew :shared:build
# Expected: BUILD SUCCESSFUL

git add -A
git commit -m "Phase 4: Final integration - achieve zero compilation errors ✅"
```

---

## Task Breakdown with Dependencies

### Dependency Graph

```
Phase 1 (Foundation) - NO DEPENDENCIES
├─ 1.1: Add missing enum values (30 min)
│   └─ Blocks: GeminiVisionAnalyzer, HybridAIServiceFacade, LiveOSHAAnalyzer, etc.
│
├─ 1.2: Fix OSHAViolation constructor (45 min)
│   └─ Blocks: LiveOSHAAnalyzer, SimpleOSHAAnalyzer
│
└─ 1.3: Eliminate duplicate imports (45 min)
    └─ Blocks: All files with enum comparison errors

Phase 2 (Type System) - DEPENDS ON PHASE 1
├─ 2.1: SafetyAnalysis parameter updates (1 hour)
│   └─ REQUIRES: 1.1 (enums), 1.2 (OSHAViolation), 1.3 (imports)
│   └─ Blocks: All AI service files
│
└─ 2.2: Fix type mismatches (30 min)
    └─ REQUIRES: 2.1 (SafetyAnalysis fixed)

Phase 3 (Platform) - INDEPENDENT (can run parallel to Phase 2)
└─ 3.1: Refactor PlatformDeviceInfo (1 hour)
    └─ Blocks: LiteRTDeviceOptimizer only

Phase 4 (Integration) - DEPENDS ON PHASES 1-3
├─ 4.1: Fix remaining issues (30 min)
│   └─ REQUIRES: All previous phases
│
└─ 4.2: Verification (30 min)
    └─ REQUIRES: 4.1
```

### Parallel Execution Opportunities

**If 2 developers:**
- Dev 1: Phases 1 → 2 (sequential, 3.5 hours)
- Dev 2: Phase 3 (parallel, 1 hour) → assist with Phase 4

**Result:** 3.5 hours total (vs 6 hours sequential)

**If 3 developers:**
- Dev 1: Phase 1.1 + 1.3 (enum + imports, 1.25 hours)
- Dev 2: Phase 1.2 (OSHAViolation, 45 min) → Phase 2.1 (2 hours)
- Dev 3: Phase 3 (platform, 1 hour) → Phase 2.2 (30 min)
- All: Phase 4 together (1 hour)

**Result:** 3 hours total

---

## Resource Allocation

### Agent Contribution Mapping

| Phase | simple-architect | refactor-master | test-guardian | loveable-ux |
|-------|-----------------|----------------|---------------|-------------|
| Phase 1 | ✅ Model definitions | ✅ Cleanup strategy | Error patterns | Error messages |
| Phase 2 | ✅ Type system design | ✅ Refactoring plan | Test impacts | UX implications |
| Phase 3 | ✅ Platform patterns | Interface design | Platform testing | N/A |
| Phase 4 | Integration review | Code cleanup | ✅ Test strategy | ✅ Final UX review |

### Developer Skill Requirements

**Phase 1:**
- Strong Kotlin knowledge
- Understanding of enum design
- Pattern matching skills

**Phase 2:**
- Deep understanding of data classes
- Constructor parameter management
- Type system expertise

**Phase 3:**
- KMP expect/actual experience
- Platform-specific API knowledge (Android SDK, iOS Foundation)
- Interface design patterns

**Phase 4:**
- General debugging skills
- Build system knowledge
- Testing experience

### Tool Requirements

**IDE:**
- IntelliJ IDEA or Android Studio with Kotlin plugin
- KMP plugin for multiplatform support

**Command Line:**
- Bash for scripts
- Gradle 8.5+
- JDK 17+

**Optional:**
- Xcode (for iOS builds on macOS)
- Android SDK (for Android builds)

---

## Risk Management

### Consolidated Risks

| Risk | Probability | Impact | Agent Source | Mitigation |
|------|------------|--------|--------------|------------|
| Breaking existing tests | Medium | Medium | refactor-master | Run tests after each phase |
| New cascading errors | Low | Medium | simple-architect | Fix foundation first (Phase 1) |
| iOS build failures | Medium | High | refactor-master | Test iOS early in Phase 3 |
| Deleting needed code | Low | High | refactor-master | Git commits per phase |
| Import replacement errors | Low | Low | refactor-master | Use tested scripts |
| Time estimate overrun | Low | Medium | All agents | Budget 20% extra time |
| Platform API unavailable | Low | Medium | simple-architect | Graceful fallbacks |

### Mitigation Strategies

**For Breaking Tests:**
```bash
# Run after each phase
./gradlew :shared:testDebugUnitTest
# If failures, analyze before continuing
```

**For Cascading Errors:**
- Follow dependency order strictly
- Commit after each milestone
- Re-count errors after each phase

**For iOS Build Failures:**
```bash
# Test iOS compilation early
./gradlew :shared:compileKotlinIosSimulatorArm64
```

**For Deleting Code:**
```bash
# Create checkpoint before each phase
git tag -a phase1-start -m "Before Phase 1"
# Can rollback if needed
git reset --hard phase1-start
```

### Rollback Plan

**Phase-Level Rollback:**
```bash
# See what changed
git diff phase1-start HEAD

# Rollback if needed
git reset --hard phase1-start

# Restore deleted files from backup
tar -xzf phase_backup_*.tar.gz
```

**Milestone-Level Rollback:**
```bash
# Undo last commit
git reset --soft HEAD~1

# Or undo and discard changes
git reset --hard HEAD~1
```

---

## Success Criteria

### Build Success Metrics

**Primary:**
- ✅ `./gradlew :shared:compileCommonMainKotlinMetadata` - Success
- ✅ `./gradlew :shared:compileKotlinAndroid` - Success
- ✅ `./gradlew :shared:compileKotlinIosSimulatorArm64` - Success
- ✅ `./gradlew :shared:build` - Success (all platforms)

**Secondary:**
- ✅ Zero compilation errors
- ✅ Zero type safety warnings
- ✅ No deprecated API usage
- ✅ No @Suppress annotations added

### Test Coverage Targets (from test-guardian)

**Unit Tests:**
- ✅ Existing tests pass: 100%
- ✅ New model tests: 80% coverage
- ✅ Platform implementation tests: Basic smoke tests

**Integration Tests:**
- ✅ AI service factory tests
- ✅ Mock analyzer tests
- ⚠️ Live analyzer tests (may be flaky)

**Test Execution:**
```bash
# Quick smoke test (2 min)
./gradlew :shared:testDebugUnitTest

# Full test suite (5 min)
./gradlew :shared:test

# With coverage report (10 min)
./gradlew :shared:testDebugUnitTest jacocoTestReport
```

### Performance Benchmarks (from simple-architect)

**Build Time:**
- Baseline: Current build time with 728 errors (fails)
- Target: Clean build < 2 minutes
- Incremental build: < 30 seconds

**Code Quality:**
- Lines added: < 500 (mostly parameter fixes)
- Lines removed: ~200 (duplicate models, obsolete code)
- Files modified: ~50
- Files added: 2 (platform implementations)

### UX Acceptance Criteria (from loveable-ux)

**Developer Experience:**
- ✅ No confusing error messages
- ✅ Clear error messages for type mismatches
- ✅ No internal implementation details exposed
- ✅ Consistent API across all platforms

**Error Message Quality:**
```kotlin
// Before: "No parameter with name 'analyzedAt' found"
// After: Clean compilation (no error)

// Before: "Comparison of incompatible enums..."
// After: Clean compilation (no error)

// Before: "Unresolved reference 'CRANE_LIFTING'"
// After: Enum value exists and resolves
```

---

## Verification Checklist

### Phase 1 Verification
- [ ] All HazardType enum values exist
- [ ] No "Unresolved reference" errors for CRANE_LIFTING, CONFINED_SPACE, etc.
- [ ] OSHAViolation constructor matches all call sites
- [ ] No "No parameter with name" errors for OSHAViolation
- [ ] No duplicate Severity/AnalysisType/BoundingBox definitions
- [ ] No "incompatible enums" comparison errors
- [ ] Error count reduced to ~325

### Phase 2 Verification
- [ ] SafetyAnalysis constructor has all required parameters
- [ ] No "No value passed for parameter" errors
- [ ] All `analyzedAt` references removed
- [ ] All `oshaCodes` references removed
- [ ] `timestamp`, `workType`, `overallRiskLevel`, `processingTimeMs` provided everywhere
- [ ] SmartAIOrchestrator Result handling correct
- [ ] AIServiceFactory capabilities are Sets not Lists
- [ ] Error count reduced to ~45

### Phase 3 Verification
- [ ] IDeviceInfo interface defined in commonMain
- [ ] AndroidDeviceInfo implemented in androidMain
- [ ] IOSDeviceInfo implemented in iosMain
- [ ] createPlatformDeviceInfo() factory exists
- [ ] No "Unresolved reference" errors for device functions
- [ ] No "Suspend function should be called" errors
- [ ] `./gradlew :shared:compileKotlinMetadata` succeeds
- [ ] Error count reduced to ~15-20

### Phase 4 Verification
- [ ] ModelMigrationUtils.kt no underscore conflicts
- [ ] All remaining errors addressed
- [ ] `./gradlew :shared:build` succeeds
- [ ] `./gradlew :shared:test` passes
- [ ] No type safety warnings
- [ ] Error count: 0

### Final Acceptance
- [ ] Clean build on all platforms
- [ ] All tests passing
- [ ] Git commits for each phase
- [ ] Documentation updated
- [ ] Code reviewed (if team environment)
- [ ] Performance benchmarks met

---

## Quick Start Guide

### For Single Developer (6 hours)

**Setup (5 minutes):**
```bash
cd /Users/aaron/Apps-Coded/HH-v0-fresh
git status  # Confirm on fix/phase2-build-critical-fixes branch
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
# Should show 728 errors
```

**Phase 1 (2 hours):**
```bash
# Milestone 1.1: Add enum values
# Edit: shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt
# Add: CRANE_LIFTING, CONFINED_SPACE, STEEL_WORK, ELECTRICAL_SAFETY, OTHER, UNKNOWN

# Milestone 1.2: Fix OSHAViolation
# Edit: LiveOSHAAnalyzer.kt, SimpleOSHAAnalyzer.kt
# Update all OSHAViolation() constructor calls

# Milestone 1.3: Fix duplicate imports
# Run script to update imports

# Verify
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
# Target: ~325 errors

git add -A && git commit -m "Phase 1 complete"
```

**Phase 2 (1.5 hours):**
```bash
# Milestone 2.1: Fix SafetyAnalysis constructors
# Edit: HybridAIServiceFacade.kt, ConstructionHazardMapper.kt, etc.
# Add missing parameters, remove old parameters

# Milestone 2.2: Fix type mismatches
# Edit: SmartAIOrchestrator.kt, AIServiceFactory.kt

# Verify
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
# Target: ~45 errors

git add -A && git commit -m "Phase 2 complete"
```

**Phase 3 (1 hour):**
```bash
# Create platform files
mkdir -p shared/src/commonMain/kotlin/com/hazardhawk/platform
mkdir -p shared/src/androidMain/kotlin/com/hazardhawk/platform
mkdir -p shared/src/iosMain/kotlin/com/hazardhawk/platform

# Create IDeviceInfo.kt, AndroidDeviceInfo.kt, IOSDeviceInfo.kt
# Edit LiteRTDeviceOptimizer.kt to use new interface

# Verify
./gradlew :shared:compileKotlinMetadata
# Should succeed

git add -A && git commit -m "Phase 3 complete"
```

**Phase 4 (1 hour):**
```bash
# Fix remaining issues
# Edit: ModelMigrationUtils.kt, any other files with errors

# Full verification
./gradlew clean
./gradlew :shared:build
./gradlew :shared:test

# Success!
git add -A && git commit -m "Phase 4 complete - zero compilation errors ✅"
```

---

## Appendix

### Error Count Tracking

```bash
#!/bin/bash
# Track error reduction progress

echo "Phase 0 (Baseline): $(date)"
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
# Expected: 728

echo "Phase 1 Complete: $(date)"
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
# Target: ~325 (55% reduction)

echo "Phase 2 Complete: $(date)"
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
# Target: ~45 (94% reduction)

echo "Phase 3 Complete: $(date)"
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
# Target: ~15-20 (97% reduction)

echo "Phase 4 Complete: $(date)"
./gradlew :shared:build
# Target: 0 (100% reduction)
```

### Useful Commands Reference

```bash
# Count errors by file
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 \
  | grep "^e: file" \
  | cut -d: -f1-3 \
  | sort | uniq -c | sort -rn | head -20

# Find specific error pattern
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 \
  | grep "No value passed for parameter"

# Check for duplicate enums
find shared/src/commonMain/kotlin -name "*.kt" \
  -exec grep -l "enum class Severity" {} \;

# Verify import updates
grep -r "import com.hazardhawk.models.Severity" \
  shared/src/commonMain --include="*.kt"

# Test specific module
./gradlew :shared:testDebugUnitTest --tests "*SafetyAnalysis*"
```

### Related Documentation

- **Original Analysis:** `docs/research/20251009-150600-phase2-build-errors-comprehensive-analysis.html`
- **Session Handoff:** `docs/handoff/20251009-150000-phase2-build-fixes-session-complete.md`
- **Phase 2 Summary:** `docs/implementation/20251009-165300-phase2-type-system-fixes-log.md`
- **This Plan:** `docs/plan/PHASE2-UNIFIED-IMPLEMENTATION-PLAN.md`

---

**Document Version:** 1.0
**Last Updated:** October 10, 2025
**Status:** Ready for Implementation
**Confidence Level:** 95%

---

*This unified plan synthesizes findings from simple-architect, refactor-master, test-guardian, and loveable-ux into a single, actionable roadmap for completing Phase 2 build error resolution.*
