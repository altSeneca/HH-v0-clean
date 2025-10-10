# Phase 2 Build Fixes - Quick Reference Card

**Print this or keep it open while working**

---

## Current Status
```
Errors: 728 → Target: 0
Time: 4-6 hours
Phases: 4
Confidence: 95%
```

---

## The 4 Phases

```
┌─────────────────────────────────────────────────────┐
│ Phase 1: FOUNDATION (2 hours)                       │
│ ✓ Add 6 missing enum values                         │
│ ✓ Fix OSHAViolation constructors                    │
│ ✓ Run import replacement script                     │
│ → Result: 325 errors (55% reduction)                │
└─────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────┐
│ Phase 2: TYPE SYSTEM (1.5 hours)                    │
│ ✓ Update SafetyAnalysis constructors (10 files)     │
│ ✓ Fix type mismatches                               │
│ → Result: 45 errors (94% total reduction)           │
└─────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────┐
│ Phase 3: PLATFORM (1 hour) - PARALLEL OK            │
│ ✓ Create IDeviceInfo interface                      │
│ ✓ Implement AndroidDeviceInfo                       │
│ ✓ Implement IOSDeviceInfo                           │
│ → Result: Platform builds work                      │
└─────────────────────────────────────────────────────┘
            ↓
┌─────────────────────────────────────────────────────┐
│ Phase 4: INTEGRATION (1 hour)                       │
│ ✓ Fix remaining misc errors                         │
│ ✓ Run full build verification                       │
│ → Result: 0 errors ✅                                │
└─────────────────────────────────────────────────────┘
```

---

## Phase 1 Cheat Sheet

### Task 1.1: Add Enum Values (30 min)

**File:** `shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt`

**Add to HazardType enum:**
```kotlin
CRANE_LIFTING,
CONFINED_SPACE,
STEEL_WORK,
ELECTRICAL_SAFETY,
OTHER,
UNKNOWN
```

**Verify:**
```bash
grep -r "Unresolved reference 'CRANE_LIFTING'" shared/src --include="*.kt"
# Should return: no results
```

---

### Task 1.2: Fix OSHAViolation (45 min)

**Files:** `LiveOSHAAnalyzer.kt`, `SimpleOSHAAnalyzer.kt`

**Replace this:**
```kotlin
OSHAViolation(
    violationId = uuid,
    oshaStandard = code,
    standardTitle = title,
    violationType = severity,
    potentialPenalty = penalty,
    timeframe = deadline
)
```

**With this:**
```kotlin
OSHAViolation(
    code = code,
    title = title,
    description = description,
    severity = severity,
    potentialFine = penalty,
    complianceDeadline = deadline
)
```

**Verify:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "OSHAViolation"
# Should show: no parameter errors
```

---

### Task 1.3: Fix Duplicate Imports (45 min)

**Run this script:**
```bash
find shared/src/commonMain/kotlin -name "*.kt" -type f -exec sed -i '' \
  -e 's/import com\.hazardhawk\.models\.Severity/import com.hazardhawk.core.models.Severity/g' \
  -e 's/import com\.hazardhawk\.models\.AnalysisType/import com.hazardhawk.core.models.AnalysisType/g' \
  -e 's/import com\.hazardhawk\.models\.BoundingBox/import com.hazardhawk.core.models.BoundingBox/g' \
  {} +
```

**Verify:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "incompatible enums"
# Should return: no results
```

**Checkpoint:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
# Target: ~325 errors
git commit -m "Phase 1 complete"
```

---

## Phase 2 Cheat Sheet

### Task 2.1: SafetyAnalysis Constructors (1 hour)

**Files to update:** HybridAIServiceFacade.kt, ConstructionHazardMapper.kt, YOLO11SafetyAnalyzer.kt, etc.

**Replace this pattern everywhere:**
```kotlin
SafetyAnalysis(
    analyzedAt = Clock.System.now(),  // ❌
    oshaCodes = codes,                // ❌
)
```

**With this:**
```kotlin
SafetyAnalysis(
    timestamp = Clock.System.now().toEpochMilliseconds(),
    workType = workType,              // Add from function param
    overallRiskLevel = calculateRiskLevel(hazards),
    processingTimeMs = processingTime
)
```

**Find all locations:**
```bash
grep -rn "SafetyAnalysis(" shared/src/commonMain --include="*.kt" \
  | grep -v "data class SafetyAnalysis"
```

**Verify:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "No value passed for parameter"
# Should return: no results
```

---

### Task 2.2: Type Mismatches (30 min)

**SmartAIOrchestrator.kt:**
```kotlin
// Replace Result.getOrDefault() with Result.getOrElse { ... }
```

**AIServiceFactory.kt:**
```kotlin
// Change List to Set
val capabilities = setOf(AnalysisCapability.HAZARD_DETECTION)
```

**Checkpoint:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
# Target: ~45 errors
git commit -m "Phase 2 complete"
```

---

## Phase 3 Cheat Sheet

### Task 3.1: Platform Refactor (1 hour)

**Step 1:** Create interface
```kotlin
// File: shared/src/commonMain/kotlin/com/hazardhawk/platform/IDeviceInfo.kt
interface IDeviceInfo {
    fun detectDeviceTier(): DeviceTier
    fun getCurrentThermalState(): ThermalState
    fun getMemoryInfo(): MemoryInfo
}
expect fun createPlatformDeviceInfo(): IDeviceInfo
```

**Step 2:** Android implementation
```kotlin
// File: shared/src/androidMain/kotlin/com/hazardhawk/platform/AndroidDeviceInfo.kt
class AndroidDeviceInfo : IDeviceInfo { /* ... */ }
actual fun createPlatformDeviceInfo() = AndroidDeviceInfo()
```

**Step 3:** iOS implementation
```kotlin
// File: shared/src/iosMain/kotlin/com/hazardhawk/platform/IOSDeviceInfo.kt
class IOSDeviceInfo : IDeviceInfo { /* ... */ }
actual fun createPlatformDeviceInfo() = IOSDeviceInfo()
```

**Step 4:** Update LiteRTDeviceOptimizer.kt
```kotlin
// Remove expect object, use interface
private val deviceInfo = createPlatformDeviceInfo()
val tier = deviceInfo.detectDeviceTier()
```

**Verify:**
```bash
./gradlew :shared:compileKotlinMetadata
# Should succeed
```

**Checkpoint:**
```bash
git commit -m "Phase 3 complete"
```

---

## Phase 4 Cheat Sheet

### Task 4.1: Final Fixes (30 min)

**Fix underscore conflicts in ModelMigrationUtils.kt:**
```kotlin
// Replace multiple '_' with named variables
val (unused1, unused2, unused3) = oldModel
```

**Fix any remaining errors:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | head -50
# Address each error individually
```

---

### Task 4.2: Verification (30 min)

**Full build:**
```bash
./gradlew clean
./gradlew :shared:build
# Must succeed
```

**Run tests:**
```bash
./gradlew :shared:test
# All must pass
```

**Final checkpoint:**
```bash
git commit -m "Phase 4 complete - zero errors ✅"
git push origin fix/phase2-build-critical-fixes
```

---

## Emergency Commands

### Count Errors
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "^e: file" | wc -l
```

### Top Error Files
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 \
  | grep "^e: file" | cut -d: -f1-3 | sort | uniq -c | sort -rn | head -10
```

### Find Pattern
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep "PATTERN"
```

### Rollback
```bash
git reset --hard HEAD~1  # Undo last commit
git reset --hard phase1-start  # Rollback to phase start
```

### Create Checkpoint
```bash
git tag -a phase1-start -m "Before Phase 1"
```

---

## Success Criteria Checklist

### After Phase 1
- [ ] 6 enum values added
- [ ] OSHAViolation fixed in 2 files
- [ ] Imports updated
- [ ] ~325 errors remaining
- [ ] Git commit created

### After Phase 2
- [ ] SafetyAnalysis fixed in 10 files
- [ ] Type mismatches fixed
- [ ] ~45 errors remaining
- [ ] Git commit created

### After Phase 3
- [ ] Platform interface created
- [ ] Android implementation complete
- [ ] iOS implementation complete
- [ ] `./gradlew :shared:compileKotlinMetadata` succeeds
- [ ] Git commit created

### After Phase 4
- [ ] All errors fixed
- [ ] `./gradlew :shared:build` succeeds
- [ ] `./gradlew :shared:test` passes
- [ ] Git commit created
- [ ] Changes pushed

---

## Time Tracking

| Phase | Estimate | Actual | Notes |
|-------|----------|--------|-------|
| Phase 1 | 2h | ___ | ___ |
| Phase 2 | 1.5h | ___ | ___ |
| Phase 3 | 1h | ___ | ___ |
| Phase 4 | 1h | ___ | ___ |
| Buffer | 0.5h | ___ | ___ |
| **Total** | **6h** | ___ | ___ |

---

## Error Tracking

| Checkpoint | Expected | Actual | Diff |
|------------|----------|--------|------|
| Baseline | 728 | 728 | ✅ |
| Phase 1 | ~325 | ___ | ___ |
| Phase 2 | ~45 | ___ | ___ |
| Phase 3 | ~15 | ___ | ___ |
| Phase 4 | 0 | ___ | ___ |

---

## Key File Paths

**Models:**
- `shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt`

**AI Services:**
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/HybridAIServiceFacade.kt`
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/yolo/ConstructionHazardMapper.kt`
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/yolo/YOLO11SafetyAnalyzer.kt`
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/LiveOSHAAnalyzer.kt`
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/SimpleOSHAAnalyzer.kt`

**Platform:**
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/litert/LiteRTDeviceOptimizer.kt`
- Create: `shared/src/commonMain/kotlin/com/hazardhawk/platform/IDeviceInfo.kt`
- Create: `shared/src/androidMain/kotlin/com/hazardhawk/platform/AndroidDeviceInfo.kt`
- Create: `shared/src/iosMain/kotlin/com/hazardhawk/platform/IOSDeviceInfo.kt`

---

## Full Documentation

- **Quick Reference (this doc):** `docs/plan/PHASE2-QUICK-REFERENCE.md`
- **Executive Summary:** `docs/plan/PHASE2-EXECUTIVE-SUMMARY.md`
- **Full Implementation Plan:** `docs/plan/PHASE2-UNIFIED-IMPLEMENTATION-PLAN.md`

---

**Remember:**
- Git commit after each phase
- Verify error count after each phase
- Don't skip verification steps
- Take breaks between phases

**You've got this!** 🚀
