# Model Consolidation Matrix

**Date**: 2025-10-09
**Refactor Master**: Phase 2 Foundation Layer
**Objective**: Eliminate duplicate models and establish single source of truth

## Summary

- **Total Duplicates Found**: 20 files
- **Identical Files**: 17 (safe to delete)
- **Different Files**: 3 (need reconciliation)
- **Unique Files in HazardHawk**: 11 (evaluate for migration)

## Source of Truth

**Path**: `/shared/src/commonMain/kotlin/com/hazardhawk/models/`

All model files should exist in this location ONLY. The HazardHawk/shared directory is a duplicate that should be eliminated.

## Duplicate Files - Action Plan

### Category 1: Identical Duplicates (Safe to Delete)

These files are byte-for-byte identical. Action: Delete from HazardHawk/shared immediately.

| File Path | Status | Action |
|-----------|--------|--------|
| crew/Crew.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/CrewMembership.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/Project.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/CrewMember.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/CompanyWorker.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/WorkerRole.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/Company.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/CrewMemberRole.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/CertificationType.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/WorkerStatus.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/CrewType.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/WorkerProfile.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/CrewStatus.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/CertificationStatus.kt | IDENTICAL | DELETE from HazardHawk/shared |
| crew/ProjectStatus.kt | IDENTICAL | DELETE from HazardHawk/shared |
| dashboard/SafetyAction.kt | IDENTICAL | DELETE from HazardHawk/shared |
| dashboard/NavigationNotifications.kt | IDENTICAL | DELETE from HazardHawk/shared |

**Total**: 17 files

### Category 2: Different Files (Need Reconciliation)

These files have minor differences. Need to merge changes before deletion.

#### crew/WorkerCertification.kt
**Difference**: HazardHawk version has unused import `kotlinx.datetime.todayIn`
**Resolution**: Keep /shared/ version (cleaner, no unused imports)
**Action**: DELETE HazardHawk version

#### dashboard/SiteConditions.kt
**Difference**: HazardHawk version uses `Clock.System.now().toEpochMilliseconds()` instead of `System.currentTimeMillis()`
**Resolution**: UPDATE /shared/ version with the Clock fix, then DELETE HazardHawk version
**Action**: 
1. Update /shared/src/commonMain/kotlin/com/hazardhawk/models/dashboard/SiteConditions.kt
2. DELETE HazardHawk version

#### dashboard/ActivityFeedItem.kt
**Difference**: /shared/ version has extra imports for PTPDocument
**Resolution**: Keep /shared/ version (imports may be used)
**Action**: DELETE HazardHawk version

**Total**: 3 files

### Category 3: Unique Files (Evaluate for Migration)

These files exist ONLY in HazardHawk/shared. Need to evaluate if they should be moved to /shared/.

| File Path | Evaluation | Action |
|-----------|------------|--------|
| OSHAAnalysisResult.kt | Legacy AI model | MOVE to /shared/models/ai/ or keep in domain |
| AlertType.kt | Core UI model | MOVE to /shared/models/ui/ |
| SafetyReportTemplates.kt | Document templates | MOVE to /shared/models/documents/ |
| Location.kt | Core data model | MOVE to /shared/models/common/ |
| SafetyAnalysis.kt | AI analysis model | MOVE to /shared/models/ai/ |
| TagModels.kt | Photo tagging | MOVE to /shared/models/photos/ |
| ReportModels.kt | Reporting | MOVE to /shared/models/reports/ |
| crew/RepositoryModels.kt | Repository layer DTOs | KEEP in domain layer (not models) |
| crew/CrewRequests.kt | API request models | MOVE to /shared/models/api/ |
| SafetyReport.kt | Report model | MOVE to /shared/models/reports/ |
| OSHARegulationModels.kt | OSHA compliance | MOVE to /shared/models/osha/ |

**Total**: 11 files

## Reconciliation Steps

### Step 1: Fix SiteConditions.kt in /shared/
Update the lastUpdated default value to use Clock API.

```kotlin
// From:
val lastUpdated: Long = System.currentTimeMillis()

// To:
val lastUpdated: Long = Clock.System.now().toEpochMilliseconds()
```

Add import:
```kotlin
import kotlinx.datetime.Clock
```

### Step 2: Delete Identical Duplicates
Delete all 17 identical duplicate files from HazardHawk/shared/

### Step 3: Delete Reconciled Files
After Step 1 is complete, delete the 3 different files from HazardHawk/shared/

### Step 4: Update Import Statements
Search and replace all imports from:
```kotlin
import com.hazardhawk.models.crew.*
import com.hazardhawk.models.dashboard.*
```

Ensure they point to:
```kotlin
// Should remain the same, but verify no HazardHawk/ prefix
import com.hazardhawk.models.crew.*
import com.hazardhawk.models.dashboard.*
```

### Step 5: Evaluate Unique Files
- Review each unique file
- Determine proper location in /shared/
- Move or leave in place based on architectural principles

## Impact Analysis

### Files Affected (Estimated)
- **Kotlin files with imports**: ~150 files
- **Test files**: ~30 files
- **Build files**: 2 files

### Risk Level
**LOW** - All changes are import path updates, no logic changes required.

## Validation

### Compilation Check
```bash
cd /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk
./gradlew :shared:build
./gradlew :androidApp:assembleDebug
```

### Test Check
```bash
cd /Users/aaron/Apps-Coded/HH-v0-fresh/HazardHawk
./gradlew :shared:test
```

## Commit Strategy

Use atomic commits for each category:

1. `refactor: Fix SiteConditions.kt to use Clock API`
2. `refactor: Delete 17 identical duplicate crew models`
3. `refactor: Delete 4 reconciled duplicate dashboard models`
4. `refactor: Update imports after model consolidation`
5. `test: Verify model consolidation with full test suite`

## Success Criteria

- [ ] Zero duplicate model files in HazardHawk/shared/src/commonMain/kotlin/com/hazardhawk/models/
- [ ] All imports resolved correctly
- [ ] ./gradlew :shared:build succeeds
- [ ] All unit tests pass
- [ ] Migration guide document created

## Notes

- The PTPAIService.kt does NOT have compilation errors related to model imports
- The actual compilation errors are in iOS YOLO code (unrelated to this refactor)
- System.currentTimeMillis() usage has been addressed with platform abstraction layer
