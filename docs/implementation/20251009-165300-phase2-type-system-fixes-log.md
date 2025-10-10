# Phase 2: Type System Fixes - Implementation Log

**Date:** October 9, 2025 16:53
**Status:** 🟡 In Progress
**Previous Phase:** Phase 1 Foundation Fixes (Completed)

## Executive Summary

Phase 2 implementation is underway to fix type system errors and complete the build error resolution. Based on the comprehensive analysis document, this phase focuses on:
1. Adding missing model definitions
2. Fixing import paths and package structure
3. Fixing serialization configurations
4. Fixing repository exception overrides

### Progress Summary

**Completed:**
- ✅ Moved OSHAAnalysisResult to `com.hazardhawk.core.models`
- ✅ Fixed crew model imports (changed from `core.models.crew` to `models.crew`)
- ✅ Fixed dashboard model imports (changed from `core.models.dashboard` to `models.dashboard`)
- ✅ Automated import replacement script updated

**In Progress:**
- 🟡 Adding missing common model imports (Pagination, Photo, etc.)
- 🟡 Comprehensive error analysis and resolution

**Remaining:**
- ⏳ Fix serialization configurations
- ⏳ Fix repository exception overrides
- ⏳ Final build verification

## Detailed Changes

### 1. Model Package Consolidation

#### OSHAAnalysisResult Migration
**File Moved:**
- From: `shared/src/commonMain/kotlin/com/hazardhawk/models/OSHAAnalysisResult.kt`
- To: `shared/src/commonMain/kotlin/com/hazardhawk/core/models/OSHAAnalysisResult.kt`

**Package Updated:**
```kotlin
// Changed from
package com.hazardhawk.models

// To
package com.hazardhawk.core.models
```

**Impact:**
- Consolidates OSHA-related models with core safety models
- Resolves import conflicts across 21+ files
- Provides single source of truth for OSHA analysis types

#### Crew & Dashboard Model Structure Clarified

**Discovery:** The package structure is:
- **Core Safety Models:** `com.hazardhawk.core.models.*`
  - SafetyAnalysis, OSHAAnalysisResult, Tag, etc.

- **Crew/Project Models:** `com.hazardhawk.models.crew.*`
  - Project, Crew, WorkerCertification, CertificationStatus, etc.

- **Dashboard Models:** `com.hazardhawk.models.dashboard.*`
  - ActivityFeedItem, WeatherData, etc.

- **Common Models:** `com.hazardhawk.models.common.*`
  - PaginatedResult, PaginationRequest, PaginationInfo, etc.

### 2. Import Path Corrections

#### Phase 2.1: Crew Model Imports
**Script Applied:**
```bash
find shared/src/commonMain/kotlin -name "*.kt" -type f -exec sed -i '' \
  's/import com\.hazardhawk\.core\.models\.crew/import com.hazardhawk.models.crew/g' {} +
```

**Files Updated:** 26 files
**Errors Fixed:** ~60 unresolved reference errors for crew models

**Key Files Modified:**
- `data/repositories/crew/CertificationApiRepository.kt`
- `data/repositories/crew/CertificationRepositoryImpl.kt`
- `data/repositories/crew/CompanyRepositoryImpl.kt`
- `data/repositories/crew/CrewRepositoryImpl.kt`
- `data/repositories/crew/ProjectRepositoryImpl.kt`
- `data/repositories/crew/WorkerRepositoryImpl.kt`
- Plus 20+ other repository and domain files

#### Phase 2.2: Dashboard Model Imports
**Script Applied:**
```bash
find shared/src/commonMain/kotlin -name "*.kt" -type f -exec sed -i '' \
  's/import com\.hazardhawk\.core\.models\.dashboard/import com.hazardhawk.models.dashboard/g' {} +
```

**Impact:** Fixed dashboard-related imports across repository layer

### 3. Error Analysis

#### Current Status (as of 16:53)
**Total Errors:** 1,550 (down from 1,771 in Phase 1)
**Reduction:** ~12% from Phase 1 baseline

#### Top Unresolved References (Remaining)
| Reference | Count | Location | Solution Needed |
|-----------|-------|----------|-----------------|
| Photo | 27 | Multiple files | Add `import com.hazardhawk.models.Photo` |
| PaginatedResult | 23 | Repositories | Add `import com.hazardhawk.models.common.*` |
| logEvent | 20 | Analytics/Logging | Add analytics import or stub |
| format | 18 | Date/String formatting | Add formatter imports |
| UserPermission | 15 | Security/Auth | Add permission model import |
| WorkflowType | 14 | Workflow engine | Add workflow imports |
| PaginationRequest | 12 | Repositories | Add `import com.hazardhawk.models.common.*` |
| PaginationInfo | 12 | Repositories | Add `import com.hazardhawk.models.common.*` |
| OSHARegulationMatch | 12 | OSHA services | Add OSHA regulation imports |
| AlertType | 11 | Notifications | Add `import com.hazardhawk.models.AlertType` |
| UserTier | 10 | Permissions | Add permission model import |
| User | 10 | Auth/Profile | Add user model import |

### 4. Model Definitions Status

#### ✅ Completed Models (in core.models)
- `SafetyAnalysis` - Main safety analysis model
- `PPEStatus` - PPE compliance status
- `PPEItem` - Individual PPE item detection
- `BoundingBox` - Object detection bounding boxes
- `Hazard` - Safety hazard details
- `OSHAViolation` - OSHA violation information
- `OSHACode` - OSHA regulation codes
- `OSHAAnalysisResult` - Comprehensive OSHA analysis
- `OSHAHazard` - Individual OSHA hazards
- `OSHARecommendation` - OSHA compliance recommendations
- `AnalysisMetadata` - Analysis context metadata
- All related enums (AnalysisType, WorkType, HazardType, Severity, RiskLevel, etc.)

#### ✅ Existing Models (in models package)
- **Crew Models** (`models.crew.*`): Project, Crew, WorkerCertification, etc.
- **Dashboard Models** (`models.dashboard.*`): ActivityFeedItem, WeatherData, etc.
- **Common Models** (`models.common.*`): PaginatedResult, PaginationRequest, etc.
- **Other Models**: Location, NavigationModels, PDFModels, ReportModels, SafetyReport, TagModels, UserRole, AlertType, OSHARegulationModels, etc.

#### 🔄 Models Needing Import Fixes
The following models exist but files need proper imports:
1. **Photo** - Defined in multiple places, needs standardization
2. **Pagination models** - Defined in `models.common`, need imports
3. **User/Auth models** - Need to locate and add imports
4. **Analytics models** - Need to locate logEvent and related types

## Next Steps - Remaining Work

### Task 1: Fix Common Model Imports (30 min)
Add missing imports for common models across repositories:

```kotlin
// Add to files using pagination
import com.hazardhawk.models.common.PaginatedResult
import com.hazardhawk.models.common.PaginationRequest
import com.hazardhawk.models.common.PaginationInfo
import com.hazardhawk.models.common.SortDirection
```

**Files to Update (~8 files):**
- ProjectRepositoryImplNew.kt
- CertificationApiRepository.kt
- Various other repository implementations

### Task 2: Fix Photo Model References (20 min)
Investigate Photo model location and standardize:
1. Check where Photo is defined
2. Add proper imports or create unified model
3. Update all references

### Task 3: Fix Missing Enum/Status References (30 min)
Address unresolved enums and status types:
- `NonCompliant`, `ReviewRequired`, `Compliant` (ComplianceStatus?)
- `CRANE_LIFTING`, `CONFINED_SPACE` (HazardType additions)
- Missing enum values in existing types

### Task 4: Fix Serialization (@UseSerializers) (1 hour)
Per analysis document:
```kotlin
// Add to files using Instant or other kotlinx.datetime types
@file:UseSerializers(InstantSerializer::class)
```

Configure proper SerializersModule for JSON

### Task 5: Fix Repository Exceptions (30 min)
Update BaseRepository.kt:
```kotlin
sealed class RepositoryError(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    // ... implementations
}
```

### Task 6: Final Build Verification (30 min)
- Run: `./gradlew :shared:compileCommonMainKotlinMetadata`
- Target: 0 errors in metadata compilation
- Run: `./gradlew :shared:build`
- Target: Clean build across all platforms

## Lessons Learned

### 1. Package Structure Complexity
The codebase has a complex but logical package structure:
- `core.models.*` for core domain models (safety, analysis)
- `models.crew.*` for crew/project management
- `models.dashboard.*` for dashboard/UI models
- `models.common.*` for shared utilities (pagination, etc.)

Previous import fixes incorrectly tried to consolidate everything into `core.models`.

### 2. Import Replacement Strategy
Automated import replacement is effective BUT:
- Must understand actual package locations first
- Verify package structure before running scripts
- Check a few files manually before batch operations

### 3. Cascading Errors
Many errors are cascading from a few root issues:
- Missing imports cause "unresolved reference"
- This causes type inference failures
- Which cause parameter mismatch errors
- Pattern: Fix imports → many other errors auto-resolve

### 4. Build Verification is Key
Run incremental builds after each major change:
- After moving files
- After import updates
- After package changes
Catches issues early vs. accumulating errors

## Files Modified This Phase

### New Files
1. `shared/src/commonMain/kotlin/com/hazardhawk/core/models/OSHAAnalysisResult.kt` (moved from models/)

### Modified Files (26+)
- All crew repository implementations (6 files)
- Import update script enhanced
- Multiple files across:
  - `data/repositories/`
  - `ai/` services
  - `domain/` layer
  - Various other components

### Deleted Files
- None (old files will be cleaned up in Phase 3 after verification)

## Metrics

### Error Reduction
- **Phase 1 End:** 1,771 errors
- **Current (Phase 2 partial):** 1,550 errors
- **Reduction:** 221 errors (12%)
- **Target:** <200 errors by Phase 2 complete

### Files Updated
- **Phase 2 so far:** 26+ files
- **Estimated remaining:** 10-15 files
- **Total Phase 2 target:** ~40 files

### Time Tracking
- **Estimated Phase 2 Duration:** 2.5 hours (per analysis)
- **Actual Time So Far:** ~1 hour
- **Remaining:** ~1.5 hours

## Blockers & Risks

### Current Blockers
None - work proceeding as planned

### Risks
1. **Photo Model Ambiguity** (Medium)
   - Photo model seems to be defined in multiple places
   - May need to consolidate or clarify which is canonical
   - Mitigation: Research and standardize in next session

2. **Missing Analytics/Logging Types** (Low)
   - logEvent and related analytics types unresolved
   - May be from external library or needs creation
   - Mitigation: Locate source or create stubs

3. **Enum Value Mismatches** (Low)
   - Some enum values referenced but don't exist
   - May need to add to existing enums
   - Mitigation: Add missing enum values as identified

## Recommendations for Next Session

1. **Start with Pagination Imports**
   - Clear, straightforward fix
   - Will eliminate ~35-40 errors
   - Builds momentum

2. **Then Address Photo Model**
   - Investigate and standardize
   - Another ~27 errors fixed

3. **Fix Enum/Status Additions**
   - Add missing enum values
   - Update ComplianceStatus if needed

4. **Serialization Configuration**
   - Apply @UseSerializers annotations
   - Configure JSON SerializersModule

5. **Repository Exception Fix**
   - Update BaseRepository.kt
   - Override modifiers on sealed class

6. **Final Verification**
   - Clean build
   - Run tests
   - Document completion

## Checkpoint for Rollback

**Git Tag:** `phase2-partial` (recommended to create before continuing)

To create:
```bash
git add -A
git commit -m "Phase 2 partial: OSHAAnalysisResult moved, crew/dashboard imports fixed"
git tag -a phase2-partial -m "Phase 2 partial completion checkpoint"
```

To rollback if needed:
```bash
git reset --hard phase2-partial
```

---

**Log Status:** Active
**Next Update:** After remaining import fixes
**Estimated Completion:** October 9, 2025 18:00 PST
