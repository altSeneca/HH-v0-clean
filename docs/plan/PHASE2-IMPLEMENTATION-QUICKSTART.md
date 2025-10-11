# Phase 2 Implementation Quick Start Card

**Current Status:** 660 errors
**Target:** 0 errors
**Time:** 8-12 hours
**Start Date:** October 10, 2025

---

## Quick Action Plan (Copy-Paste Ready)

### Phase 2.1: Foundation (3 hours) - START HERE

#### Step 1: Add Enum Values (30 min)
```bash
# Edit this file:
code shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt

# Add to HazardType enum:
# CRANE_LIFTING, CONFINED_SPACE, STEEL_WORK, ELECTRICAL_SAFETY, OTHER, UNKNOWN

# Verify:
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep -c "^e:"
# Expected: ~540 (120 errors fixed)
```

#### Step 2: Fix Import Duplicates (1.5 hours)
```bash
# Create and run this script:
cat > scripts/fix-duplicate-imports.sh << 'EOF'
#!/bin/bash
echo "Standardizing imports..."
find shared/src/commonMain/kotlin -name "*.kt" -type f -exec sed -i '' \
  -e 's|import com\.hazardhawk\.models\.Severity|import com.hazardhawk.core.models.Severity|g' \
  -e 's|import com\.hazardhawk\.ai\.models\.Severity|import com.hazardhawk.core.models.Severity|g' \
  -e 's|import com\.hazardhawk\.models\.AnalysisType|import com.hazardhawk.core.models.AnalysisType|g' \
  -e 's|import com\.hazardhawk\.models\.BoundingBox|import com.hazardhawk.core.models.BoundingBox|g' \
  -e 's|import com\.hazardhawk\.models\.HazardType|import com.hazardhawk.core.models.HazardType|g' \
  -e 's|import com\.hazardhawk\.models\.RiskLevel|import com.hazardhawk.core.models.RiskLevel|g' \
  -e 's|import com\.hazardhawk\.models\.SafetyAnalysis|import com.hazardhawk.core.models.SafetyAnalysis|g' \
  {} +
echo "Done"
EOF

chmod +x scripts/fix-duplicate-imports.sh
./scripts/fix-duplicate-imports.sh

# Verify:
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep -c "^e:"
# Expected: ~445 (95 more errors fixed)
```

#### Step 3: Fix OSHAViolation (1 hour)
```kotlin
// Files to edit:
// 1. shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/LiveOSHAAnalyzer.kt
// 2. shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/SimpleOSHAAnalyzer.kt

// Replace OLD pattern:
OSHAViolation(
    violationId = "...",
    oshaStandard = "...",
    standardTitle = "...",
    violationType = "...",
    description = "...",
    potentialPenalty = "...",
    recommendations = listOf(...),
    timeframe = "..."
)

// With NEW pattern:
OSHAViolation(
    code = item.code,              // was oshaStandard
    title = item.title,            // was standardTitle
    description = item.description,
    severity = mapSeverity(item.severity),  // was violationType
    category = null,
    recommendations = item.correctiveActions,  // was recommendations
    potentialFine = item.penaltyRange,        // was potentialPenalty
    correctiveActions = item.correctiveActions,
    complianceDeadline = item.complianceTimeframe,  // was timeframe
    boundingBox = null
)
```

**Phase 2.1 Checkpoint:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep -c "^e:"
# Expected: ~360 errors (300 fixed!)
git add -A && git commit -m "Phase 2.1 complete - foundation fixes"
```

---

### Phase 2.2: Type System (4 hours)

#### SafetyAnalysis Fix Pattern (use for 10 files)
```kotlin
// OLD (BROKEN):
SafetyAnalysis(
    id = id,
    analysisType = AnalysisType.YOLO_LOCAL,
    hazards = hazards,
    ppeStatus = ppeStatus,
    oshaCodes = oshaCodes,              // ❌ REMOVE
    oshaViolations = oshaViolations,
    recommendations = recommendations,
    confidence = confidence,
    analyzedAt = Clock.System.now(),    // ❌ WRONG TYPE
    metadata = metadata
)

// NEW (FIXED):
SafetyAnalysis(
    id = id,
    timestamp = Clock.System.now().toEpochMilliseconds(),  // ✅
    analysisType = AnalysisType.YOLO_LOCAL,
    workType = workType,                                   // ✅ ADD
    hazards = hazards,
    ppeStatus = ppeStatus,
    oshaViolations = oshaViolations,
    recommendations = recommendations,
    overallRiskLevel = calculateRiskLevel(hazards),        // ✅ ADD
    confidence = confidence,
    processingTimeMs = processingTimeMs,                   // ✅ ADD
    metadata = metadata
)

// Helper (add once per file):
private fun calculateRiskLevel(hazards: List<Hazard>): RiskLevel {
    if (hazards.isEmpty()) return RiskLevel.LOW
    val maxSeverity = hazards.maxByOrNull { it.severity }?.severity ?: Severity.LOW
    return when (maxSeverity) {
        Severity.CRITICAL -> RiskLevel.SEVERE
        Severity.HIGH -> RiskLevel.HIGH
        Severity.MODERATE -> RiskLevel.MODERATE
        Severity.LOW, Severity.NEGLIGIBLE -> RiskLevel.LOW
    }
}
```

**Files to Fix (in this order):**
1. HybridAIServiceFacade.kt (85 errors) - 45 min
2. ConstructionHazardMapper.kt (72 errors) - 35 min
3. YOLO11SafetyAnalyzer.kt (68 errors) - 30 min
4. GeminiVisionAnalyzer.kt (35 errors) - 20 min
5. SmartAIOrchestrator.kt (28 errors) - 15 min
6. Others (remaining ~47 errors) - 1h 15min

**Phase 2.2 Checkpoint:**
```bash
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep -c "^e:"
# Expected: ~80 errors (280 more fixed!)
git add -A && git commit -m "Phase 2.2 complete - type system alignment"
```

---

### Phase 2.3: Platform (2 hours) - Can parallelize with 2.2

```bash
# Create 3 new files:

# 1. commonMain interface
mkdir -p shared/src/commonMain/kotlin/com/hazardhawk/platform
code shared/src/commonMain/kotlin/com/hazardhawk/platform/IDeviceInfo.kt
# Copy interface definition from synthesis doc

# 2. Android implementation
mkdir -p shared/src/androidMain/kotlin/com/hazardhawk/platform
code shared/src/androidMain/kotlin/com/hazardhawk/platform/AndroidDeviceInfo.kt
# Copy Android implementation from synthesis doc

# 3. iOS implementation
mkdir -p shared/src/iosMain/kotlin/com/hazardhawk/platform
code shared/src/iosMain/kotlin/com/hazardhawk/platform/IOSDeviceInfo.kt
# Copy iOS implementation from synthesis doc

# 4. Update usage
code shared/src/commonMain/kotlin/com/hazardhawk/ai/litert/LiteRTDeviceOptimizer.kt
# Replace PlatformDeviceInfo usage with IDeviceInfo injection
```

**Phase 2.3 Checkpoint:**
```bash
./gradlew :shared:compileKotlinMetadata
# Expected: SUCCESS
git add -A && git commit -m "Phase 2.3 complete - platform implementations"
```

---

### Phase 2.4: Final Cleanup (2-3 hours)

```bash
# Run full build
./gradlew clean
./gradlew :shared:build

# Fix any remaining errors shown in output
# Most will be minor import or parameter issues

# Run tests
./gradlew :shared:test

# Final verification
./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep -c "^e:"
# Expected: 0 ✅

git add -A && git commit -m "Phase 2 complete - ZERO ERRORS! 🚀"
```

---

## Progress Tracker (Update Hourly)

```
[ ] Phase 2.1.1 - Enum values (30m)
[ ] Phase 2.1.2 - OSHAViolation (1h)
[ ] Phase 2.1.3 - Import cleanup (1.5h)
    Checkpoint: ~360 errors

[ ] Phase 2.2.1 - SafetyAnalysis (3h)
[ ] Phase 2.2.2 - Type mismatches (1h)
    Checkpoint: ~80 errors

[ ] Phase 2.3.1 - Platform impl (2h)
    Checkpoint: ~20 errors

[ ] Phase 2.4.1 - Final fixes (1-2h)
[ ] Phase 2.4.2 - Verification (1h)
    Checkpoint: 0 errors ✅
```

---

## Error Count Tracking (Run After Each Step)

```bash
#!/bin/bash
# save as: scripts/track-errors.sh

ERRORS=$(./gradlew :shared:compileCommonMainKotlinMetadata 2>&1 | grep -c "^e:")
TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")
echo "$TIMESTAMP | $ERRORS errors remaining" | tee -a phase2-progress.log
```

---

## Emergency Commands

### Rollback Last Change
```bash
git reset --hard HEAD~1
```

### Rollback to Phase Start
```bash
git reset --hard phase2.1-start
```

### Check What Changed
```bash
git diff HEAD~1
```

### Restore Single File
```bash
git checkout HEAD~1 -- path/to/file.kt
```

---

## Quick References

### Top 10 Error Files
1. HybridAIServiceFacade.kt - 85 errors
2. ConstructionHazardMapper.kt - 72 errors
3. YOLO11SafetyAnalyzer.kt - 68 errors
4. LiteRTDeviceOptimizer.kt - 60 errors
5. LiveOSHAAnalyzer.kt - 48 errors
6. SimpleOSHAAnalyzer.kt - 42 errors
7. GeminiVisionAnalyzer.kt - 35 errors
8. SmartAIOrchestrator.kt - 28 errors
9. YOLO11SafetyAnalyzerExample.kt - 24 errors
10. GeminiSafetyAnalysisAdapter.kt - 18 errors

### Key File Paths
```
Models:
  shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt

Analyzers:
  shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/LiveOSHAAnalyzer.kt
  shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/SimpleOSHAAnalyzer.kt

Services:
  shared/src/commonMain/kotlin/com/hazardhawk/ai/AIServiceFacade.kt
  shared/src/commonMain/kotlin/com/hazardhawk/ai/HybridAIServiceFacade.kt

Platform:
  shared/src/commonMain/kotlin/com/hazardhawk/platform/IDeviceInfo.kt
  shared/src/androidMain/kotlin/com/hazardhawk/platform/AndroidDeviceInfo.kt
  shared/src/iosMain/kotlin/com/hazardhawk/platform/IOSDeviceInfo.kt
```

---

## Success Criteria Checklist

### Build Success ✅
- [ ] `./gradlew :shared:compileKotlinMetadata` - SUCCESS
- [ ] `./gradlew :shared:compileKotlinAndroid` - SUCCESS
- [ ] `./gradlew :shared:compileKotlinIosSimulatorArm64` - SUCCESS
- [ ] `./gradlew :shared:build` - SUCCESS
- [ ] Error count: 0

### Test Success ✅
- [ ] `./gradlew :shared:test` - PASS
- [ ] Pass rate ≥ 98%
- [ ] No test regressions

### Quality ✅
- [ ] No type safety warnings
- [ ] All imports use core.models
- [ ] No backup files in repo
- [ ] Documentation updated

---

## Time Tracking Template

```
Start Time: [____:____]

Phase 2.1.1: [____:____] - [____:____] = ____ min (expected 30m)
Phase 2.1.2: [____:____] - [____:____] = ____ min (expected 60m)
Phase 2.1.3: [____:____] - [____:____] = ____ min (expected 90m)

Phase 2.2.1: [____:____] - [____:____] = ____ min (expected 180m)
Phase 2.2.2: [____:____] - [____:____] = ____ min (expected 60m)

Phase 2.3.1: [____:____] - [____:____] = ____ min (expected 120m)

Phase 2.4: [____:____] - [____:____] = ____ min (expected 120-180m)

Total Time: ____ hours (expected 8-12h)
```

---

## Help & Documentation

**Full Implementation Plan:**
`docs/plan/20251010-UNIFIED-PHASE2-IMPLEMENTATION-SYNTHESIS.md`

**Testing Strategy:**
`docs/testing/20251010-phase2-testing-strategy.md`

**Refactoring Details:**
`docs/plan/20251010-refactoring-strategy-phase2.md`

**Error Analysis:**
`docs/plan/20251010-error-analysis-summary.md`

---

**Ready? Start with Phase 2.1.1 (30 minutes)! You've got this! 🚀**
