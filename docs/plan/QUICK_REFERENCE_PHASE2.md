# Phase 2 Quick Reference Guide

**Last Updated:** 2025-10-10  
**Status:** 284 errors remaining (down from 660)

## Quick Stats

| Metric | Value |
|--------|-------|
| Total Errors | 284 |
| Errors Fixed | 376 (57%) |
| Top Error Type | Constructor Mismatches (120) |
| Most Affected File | ConstructionEnvironmentAdapter.android.kt (62 errors) |
| Estimated Time to < 50 errors | 8-10 hours |
| Estimated Time to < 10 errors | 12-15 hours |

## Error Categories at a Glance

```
┌─────────────────────────────────────┐
│ CRITICAL: Platform API Gaps         │
│ 70 errors | 5 files                 │
│ Fix Time: 2-3 hours                 │
│ Priority: 1 (DO THIS FIRST)         │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ HIGH: Constructor Mismatches        │
│ 120 errors | 6 files                │
│ Fix Time: 3-4 hours                 │
│ Priority: 2                         │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ HIGH: Unresolved References         │
│ 60 errors | 10+ files               │
│ Fix Time: 2-3 hours                 │
│ Priority: 3                         │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ MEDIUM: Import Path Issues          │
│ 30 errors | 30+ files               │
│ Fix Time: 1-2 hours (automated)     │
│ Priority: 4                         │
└─────────────────────────────────────┘
┌─────────────────────────────────────┐
│ LOW: Backup File Cleanup            │
│ 0 errors | 25 files                 │
│ Fix Time: 1 hour                    │
│ Priority: 5 (maintainability)       │
└─────────────────────────────────────┘
```

## Cheat Sheet: Common Error Patterns

### Pattern 1: "has no corresponding expected declaration"
```kotlin
// ERROR: actual function without expect declaration
actual fun LiteRTDeviceOptimizer.getBatteryLevel(): Int

// FIX: Add expect declaration in commonMain
expect class IDeviceInfo {
    fun getBatteryLevel(): Int
}
```

### Pattern 2: "No parameter with name 'X'"
```kotlin
// ERROR: Old parameter name
LiteRTPerformanceMetrics(
    backendUsed = backend,
    totalInferences = count
)

// FIX: Use new parameter names
LiteRTPerformanceMetrics(
    preferredBackend = backend,
    analysisCount = count,
    averageProcessingTimeMs = avgTime,
    tokensPerSecond = tokens,
    // ... add other required params
)
```

### Pattern 3: "Unresolved reference 'LiteRTHazardDetection'"
```kotlin
// ERROR: Old type name
val detection: LiteRTHazardDetection

// FIX: Use unified type
val detection: DetectedHazard
```

### Pattern 4: Wrong import path
```kotlin
// ERROR: Old import
import com.hazardhawk.models.SafetyAnalysis

// FIX: Use unified import
import com.hazardhawk.core.models.SafetyAnalysis
```

### Pattern 5: "No value passed for parameter 'X'"
```kotlin
// ERROR: Missing required parameter
SafetyAnalysis(
    id = id,
    confidence = 0.8f  // Wrong parameter name
)

// FIX: Provide all required parameters
SafetyAnalysis(
    id = id,
    photoId = photoId,          // Required
    timestamp = Clock.System.now(),
    analysisType = AnalysisType.LOCAL_LITERT_VISION,
    workType = WorkType.GENERAL_CONSTRUCTION,
    overallRiskLevel = RiskLevel.LOW,
    severity = Severity.MEDIUM, // Required
    aiConfidence = 0.8f,        // Required (was 'confidence')
    processingTimeMs = 1000L
)
```

## Quick Command Reference

### See current error count
```bash
./gradlew :shared:compileDebugKotlinAndroid 2>&1 | grep "^e: " | wc -l
```

### Get top error files
```bash
./gradlew :shared:compileDebugKotlinAndroid 2>&1 | \
  grep "^e: " | sed 's/^e: file:\/\/.*\///' | \
  sed 's/:[0-9]*:.*//' | sort | uniq -c | sort -rn | head -10
```

### Find unresolved references
```bash
./gradlew :shared:compileDebugKotlinAndroid 2>&1 | \
  grep "Unresolved reference" | \
  grep -o "'[^']*'" | sort | uniq -c | sort -rn
```

### Count backup files
```bash
find shared/src -name "*.kt.bak*" -o -name "*.kt.backup" | wc -l
```

### Run import standardization (when script exists)
```bash
./scripts/standardize_imports.sh
```

## Top 5 Files to Fix First

1. **ConstructionEnvironmentAdapter.android.kt** (62 errors)
   - Type: Type mismatches
   - Strategy: Update to unified models

2. **LiteRTModelEngine.android.kt** (55 errors)
   - Type: Constructor mismatches
   - Strategy: Use factory methods

3. **PhotoEncryptionServiceImpl.kt** (49 errors)
   - Type: Missing constants
   - Strategy: Create HazardHawkConstants

4. **TFLiteModelEngine.kt** (41 errors)
   - Type: Constructor mismatches
   - Strategy: Use factory methods

5. **SecureStorageServiceImpl.kt** (29 errors)
   - Type: Missing constants
   - Strategy: Use HazardHawkConstants

## Unified Model Locations (Cheat Sheet)

| Old Location | New Location | Import |
|-------------|--------------|--------|
| `ai.models.SafetyAnalysis` | `core.models.SafetyAnalysis` | `import com.hazardhawk.core.models.SafetyAnalysis` |
| `models.SafetyAnalysis` | `core.models.SafetyAnalysis` | `import com.hazardhawk.core.models.SafetyAnalysis` |
| `models.OSHAViolation` | `core.models.OSHAViolation` | `import com.hazardhawk.core.models.OSHAViolation` |
| `models.Severity` | `core.models.Severity` | `import com.hazardhawk.core.models.Severity` |
| `ai.models.HazardType` | `core.models.HazardType` | `import com.hazardhawk.core.models.HazardType` |
| `LiteRTHazardDetection` | `DetectedHazard` | Already in LiteRTModelEngine.kt |
| `LiteRTPPEDetection` | `PPEDetection` | `import com.hazardhawk.core.models.PPEDetection` |

## Constructor Parameter Mappings

### SafetyAnalysis
| Old Name | New Name |
|----------|----------|
| `confidence` | `aiConfidence` |
| `processingTime` | `processingTimeMs` |
| N/A (new required) | `photoId` |
| N/A (new required) | `severity` |

### LiteRTPerformanceMetrics
| Old Name | New Name |
|----------|----------|
| `backendUsed` | `preferredBackend` |
| `totalInferences` | `analysisCount` |
| `averageInferenceTime` | `averageProcessingTimeMs` |
| N/A (new required) | `tokensPerSecond` |
| N/A (new required) | `peakMemoryUsageMB` |
| N/A (new required) | `averageMemoryUsageMB` |
| N/A (new required) | `thermalThrottlingDetected` |

### LiteRTAnalysisResult
| Old Name | New Name |
|----------|----------|
| `detectedHazards` | `hazards` |
| `ppeDetections` | `ppeStatus` |
| `overallRiskLevel` | `overallRiskAssessment` |
| `confidenceThreshold` | N/A (removed) |
| N/A (new) | `confidence` |

## Batch Fix Commands (USE WITH CAUTION)

### Rename LiteRT types (SAFE - reversible)
```bash
# Backup first
git add -A && git commit -m "Checkpoint before batch rename"

# Run batch rename
find shared/src -name "*.kt" -type f -exec sed -i '' \
  -e 's/LiteRTHazardDetection/DetectedHazard/g' \
  -e 's/LiteRTPPEDetection/PPEDetection/g' \
  -e 's/LiteRTOSHAViolation/OSHAViolation/g' \
  {} +
  
# Verify
./gradlew :shared:compileDebugKotlinAndroid
```

### Fix uuid4 references (SAFE)
```bash
find shared/src -name "*.kt" -type f -exec sed -i '' \
  -e 's/\.uuid4()/.uuid()/g' \
  {} +
```

### Archive and remove backups (SAFE - creates archive)
```bash
# Create archive
tar -czf "backup_archive_$(date +%Y%m%d_%H%M%S).tar.gz" \
    $(find shared/src -name "*.kt.bak*" -o -name "*.kt.backup" -o -name "*.kt.fix")

# Remove backups
find shared/src -name "*.kt.bak*" -delete
find shared/src -name "*.kt.backup" -delete
find shared/src -name "*.kt.fix" -delete
```

## When Things Go Wrong

### If compilation breaks completely
```bash
# 1. Check git status
git status

# 2. See what changed
git diff

# 3. Revert if needed
git checkout -- shared/src/

# 4. Go back to last working commit
git reset --hard HEAD
```

### If you're stuck on a specific error
```bash
# Get full error details
./gradlew :shared:compileDebugKotlinAndroid 2>&1 | \
  grep -A 5 "YourFileName.kt"
```

### If you need to find where a type is defined
```bash
# Find type definition
grep -r "class YourTypeName" shared/src/commonMain

# Find all usages
grep -r "YourTypeName" shared/src
```

## Phase Implementation Checklist

### Phase 2.1: Platform APIs (2-3 hours)
- [ ] Create IDeviceInfo interface in commonMain
- [ ] Implement AndroidDeviceInfo in androidMain
- [ ] Implement IOSDeviceInfo stub in iosMain
- [ ] Update LiteRTDeviceOptimizer to use interface
- [ ] Test Android compilation
- [ ] Verify error count drops by ~70

### Phase 2.2: Constructor Alignment (3-4 hours)
- [ ] Create ModelConstructorMigration utility
- [ ] Add factory methods to SafetyAnalysis
- [ ] Add factory methods to LiteRTPerformanceMetrics
- [ ] Update LiteRTModelEngine instantiations
- [ ] Update TFLiteModelEngine instantiations
- [ ] Update VertexAIClient instantiations
- [ ] Verify error count drops by ~120

### Phase 2.3: Type References (2-3 hours)
- [ ] Run LiteRT type batch rename
- [ ] Create HazardHawkConstants object
- [ ] Fix all SecurityConstants references
- [ ] Fix uuid4 references
- [ ] Fix GPU/NPU/NNAPI enum references
- [ ] Fix data class copy operations
- [ ] Verify error count drops by ~60

### Phase 2.4: Import Standardization (1-2 hours)
- [ ] Create import standardization script
- [ ] Run on all commonMain files
- [ ] Verify imports manually
- [ ] Remove unused imports
- [ ] Test compilation
- [ ] Verify error count drops by ~30

### Phase 2.5: Cleanup (1 hour)
- [ ] Archive backup files
- [ ] Remove backups from source
- [ ] Run final compilation test
- [ ] Update documentation
- [ ] Create PR with changes

## Success Criteria

| Phase | Target Errors | Actual | Status |
|-------|--------------|--------|--------|
| Start | 660 | 284 | ✅ 57% reduction |
| After 2.1 | ~210 | TBD | ⏳ Pending |
| After 2.2 | ~90 | TBD | ⏳ Pending |
| After 2.3 | ~30 | TBD | ⏳ Pending |
| After 2.4 | <10 | TBD | ⏳ Pending |
| After 2.5 | 0 | TBD | ⏳ Pending |

## Resources

- **Full Strategy:** `docs/plan/20251010-refactoring-strategy-phase2.md`
- **Error Analysis:** `docs/plan/20251010-error-analysis-summary.md`
- **Implementation Plan:** `docs/plan/20251010-094500-phase2-next-steps-plan.md`
- **Model Reference:** `shared/src/commonMain/kotlin/com/hazardhawk/core/models/SafetyAnalysis.kt`

---

**Quick Tip:** Start with Phase 2.1 (Platform APIs). It has the highest impact and unblocks other fixes.
