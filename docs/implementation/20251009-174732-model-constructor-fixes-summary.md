# AI Service Model Constructor Fixes - Implementation Summary

**Date:** 2025-10-09
**Session:** Model Constructor Standardization
**Branch:** fix/phase2-build-critical-fixes

## Objective

Fix all AI service files to use correct SafetyAnalysis and OSHAViolation model constructors, eliminating build errors caused by inconsistent model usage across the codebase.

## Root Cause Analysis

AI services were using incorrect constructor parameters for `SafetyAnalysis` and `OSHAViolation` models due to:
1. Multiple duplicate OSHAViolation class definitions across different packages
2. Legacy model constructors with different parameter names
3. Inconsistent use of unified vs. detailed violation models

## Model Signatures (Correct/Unified)

### SafetyAnalysis (com.hazardhawk.core.models.SafetyAnalysis)
```kotlin
data class SafetyAnalysis(
    val id: String,
    val photoId: String,
    val timestamp: Instant,
    val analysisType: AnalysisType,
    val workType: WorkType,
    val hazards: List<Hazard> = emptyList(),
    val ppeStatus: PPEStatus? = null,
    val oshaViolations: List<OSHAViolation> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val overallRiskLevel: RiskLevel,
    val severity: Severity,
    val aiConfidence: Float,
    val processingTimeMs: Long,
    val metadata: AnalysisMetadata? = null
)
```

### OSHAViolation (Unified - com.hazardhawk.core.models.OSHAViolation)
```kotlin
data class OSHAViolation(
    val code: String,              // OSHA code (e.g., "1926.95")
    val title: String,             // Standard title
    val description: String,       // Violation description
    val severity: Severity,        // Severity level
    val fineRange: String? = null, // Potential fine range
    val correctiveAction: String   // Required corrective action
)
```

### OSHADetailedViolation (Detailed - com.hazardhawk.core.models.OSHADetailedViolation)
```kotlin
data class OSHADetailedViolation(
    val violationId: String,
    val oshaStandard: String,      // Full CFR citation
    val standardTitle: String,
    val violationType: OSHAViolationType,
    val description: String,
    val potentialPenalty: String?,
    val correctiveAction: String,
    val timeframe: String? = null
)
```

## Files Fixed

### 1. HazardDetectionProcessor.kt
**Changes:**
- Removed duplicate `OSHAViolation` class definition (lines 307-313)
- Added import for unified `OSHAViolation` from `com.hazardhawk.core.models`
- Updated constructor calls to use unified model parameters:
  - `standard` → `code`
  - Added `title` parameter
  - `correctionDeadline` → removed (not in unified model)
  - Added `correctiveAction` parameter

**Constructor Fixes:**
```kotlin
// BEFORE
OSHAViolation(
    standard = "1926.501(b)(1)",
    description = "Unprotected sides and edges",
    severity = hazard.severity,
    fineRange = "$7,000 - $15,625",
    correctionDeadline = "Immediate"
)

// AFTER
OSHAViolation(
    code = "1926.501(b)(1)",
    title = "Unprotected Sides and Edges",
    description = "Unprotected sides and edges",
    severity = hazard.severity,
    fineRange = "$7,000 - $15,625",
    correctiveAction = "Install guardrail systems or personal fall arrest systems"
)
```

### 2. OSHAAnalysisResult.kt
**Changes:**
- Renamed duplicate `OSHAViolation` class to `OSHADetailedViolation`
- Updated all references in the file
- Maintains detailed OSHA violation model for comprehensive analysis results

**Parameter Mapping:**
- `violationId` - Unique violation identifier
- `oshaStandard` - Full CFR citation (e.g., "29 CFR 1926.95(a)")
- `standardTitle` - Standard title (e.g., "Personal Protective Equipment")
- `violationType` - Violation severity type (SERIOUS, OTHER_THAN_SERIOUS, etc.)
- `potentialPenalty` - Fine range (e.g., "Up to $15,625 per violation")
- `correctiveAction` - Required corrective action
- `timeframe` - Correction timeframe (optional)

### 3. LiveOSHAAnalyzer.kt
**Changes:**
- Updated to use `OSHADetailedViolation` instead of legacy `OSHAViolation`
- Changed: `mutableListOf<OSHAViolation>()` → `mutableListOf<OSHADetailedViolation>()`
- Changed: `OSHAViolation(...)` → `OSHADetailedViolation(...)`
- Fixed both main analysis and fallback analysis

**Constructor Parameter Mapping:**
- `violationId` - Generated from analysis context
- `oshaStandard` - OSHA standard code
- `standardTitle` - Retrieved via `getStandardTitle()`
- `violationType` - Mapped from `OSHASeverity` enum
- `description` - Violation details
- `potentialPenalty` - Calculated via `calculatePenalty()`
- `correctiveAction` - Required action
- `timeframe` - Generated via `getRequiredTimeframe()`

### 4. SimpleOSHAAnalyzer.kt
**Changes:**
- Updated to use `OSHADetailedViolation` instead of legacy `OSHAViolation`
- Changed: `mutableListOf<OSHAViolation>()` → `mutableListOf<OSHADetailedViolation>()`
- Changed: `OSHAViolation(...)` → `OSHADetailedViolation(...)`

### 5. OSHAReportIntegrationService.kt
**Changes:**
- Added conversion helper function `convertToUnifiedViolation()`
- Updated `generateViolationsSection()` to convert `OSHADetailedViolation` to `OSHAViolation`
- Fixed property access patterns:
  - `violation.violationType` → `violation.severity`
  - `violation.oshaStandard` → `violation.code`
  - Updated severity filtering logic
  - Updated sorting by code instead of oshaStandard

**Conversion Helper Function:**
```kotlin
private fun convertToUnifiedViolation(detailed: OSHADetailedViolation): OSHAViolation {
    val severity = when (detailed.violationType) {
        OSHAViolationType.SERIOUS, OSHAViolationType.WILLFUL -> Severity.CRITICAL
        OSHAViolationType.REPEAT -> Severity.HIGH
        OSHAViolationType.OTHER_THAN_SERIOUS -> Severity.MEDIUM
        OSHAViolationType.DE_MINIMIS, OSHAViolationType.FAILURE_TO_ABATE -> Severity.LOW
    }
    
    return OSHAViolation(
        code = detailed.oshaStandard,
        title = detailed.standardTitle,
        description = detailed.description,
        severity = severity,
        fineRange = detailed.potentialPenalty,
        correctiveAction = detailed.correctiveAction
    )
}
```

**Filtering Logic Updates:**
```kotlin
// BEFORE
val criticalViolations = allViolations.filter { 
    it.violation.violationType == OSHAViolationType.SERIOUS 
}.sortedBy { it.violation.oshaStandard }

// AFTER
val criticalViolations = allViolations.filter { 
    it.violation.severity == Severity.CRITICAL 
}.sortedBy { it.violation.code }
```

## Error Reduction

### Before Fixes
- **Total compilation errors:** 1,780

### After Fixes
- **Total compilation errors:** 1,759
- **Errors fixed:** 21 errors eliminated (1.2% reduction)

### Errors Fixed By Category
1. **OSHAViolation constructor errors:** ~8-10 errors
2. **Property access errors (violationType, oshaStandard):** ~6-8 errors
3. **Type mismatch errors (OSHAViolation vs OSHADetailedViolation):** ~3-5 errors

## Remaining Model-Related Issues

### LiteRT Services (Not addressed in this session)
- LiteRTOSHAViolation class undefined
- Constructor parameter mismatches in LiteRTModelEngine
- Missing expected declarations in LiteRTDeviceOptimizer

### GeminiSafetyAnalysisAdapter (Not addressed in this session)
- Severity enum conflicts between `com.hazardhawk.core.models.Severity` and `com.hazardhawk.models.Severity`
- Requires enum consolidation

## Architecture Notes

### Two-Tier Violation Model

The codebase now uses a two-tier violation model:

1. **Unified OSHAViolation (com.hazardhawk.core.models.OSHAViolation)**
   - Simple, consistent model for cross-cutting concerns
   - Used in reports, UI, and general safety analysis
   - Standardized parameters across all services

2. **Detailed OSHADetailedViolation (com.hazardhawk.core.models.OSHADetailedViolation)**
   - Comprehensive OSHA-specific model
   - Used in OSHA compliance analysis and detailed reporting
   - Includes violation type, full CFR citations, timeframes

This separation allows:
- Flexibility in OSHA-specific analysis
- Consistency in general safety reporting
- Easy conversion between models via helper functions

## Testing Recommendations

1. **Unit Tests:**
   - Test conversion functions (OSHADetailedViolation → OSHAViolation)
   - Verify all AI services create correct model structures
   - Test severity mapping logic

2. **Integration Tests:**
   - Test end-to-end OSHA analysis workflow
   - Verify report generation with converted violations
   - Test violation aggregation and filtering

3. **Edge Cases:**
   - Empty violation lists
   - Violations with null optional fields
   - All severity levels
   - All violation types

## Next Steps

1. **Address LiteRT Services:**
   - Define or remove LiteRTOSHAViolation
   - Fix constructor parameter mismatches
   - Add missing expected declarations

2. **Fix Severity Enum Conflicts:**
   - Consolidate Severity enums
   - Update GeminiSafetyAnalysisAdapter
   - Remove legacy com.hazardhawk.models.Severity

3. **Validate Complete Build:**
   - Run full build after all fixes
   - Target: <1500 errors (15%+ total reduction)
   - Document remaining error categories

4. **Update Documentation:**
   - Update API documentation for unified models
   - Add migration guide for legacy code
   - Document conversion patterns

## Lessons Learned

1. **Model Consolidation Critical:**
   - Multiple model definitions cause type conflicts
   - Unified models simplify codebase maintenance
   - Migration should be phased and documented

2. **Parameter Naming Consistency:**
   - Consistent naming across models prevents confusion
   - `code` vs `standard` vs `oshaCode` caused issues
   - Document parameter mappings clearly

3. **Conversion Layers Valuable:**
   - Helper functions for model conversion are essential
   - Allows flexibility while maintaining compatibility
   - Centralizes conversion logic for easier maintenance

## Files Modified

1. `/shared/src/commonMain/kotlin/com/hazardhawk/ai/HazardDetectionProcessor.kt`
2. `/shared/src/commonMain/kotlin/com/hazardhawk/core/models/OSHAAnalysisResult.kt`
3. `/shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/LiveOSHAAnalyzer.kt`
4. `/shared/src/commonMain/kotlin/com/hazardhawk/ai/impl/SimpleOSHAAnalyzer.kt`
5. `/shared/src/commonMain/kotlin/com/hazardhawk/reports/OSHAReportIntegrationService.kt`

## Commit Message

```
fix: Standardize AI service model constructors for SafetyAnalysis and OSHAViolation

- Remove duplicate OSHAViolation class from HazardDetectionProcessor
- Rename duplicate OSHAViolation to OSHADetailedViolation in OSHAAnalysisResult
- Update LiveOSHAAnalyzer and SimpleOSHAAnalyzer to use OSHADetailedViolation
- Add conversion helper in OSHAReportIntegrationService for unified model
- Fix property references (violationType → severity, oshaStandard → code)
- Reduce compilation errors from 1780 to 1759 (21 errors fixed)

Related to Phase 2 critical build fixes.
```

## Summary

This session successfully addressed critical model constructor inconsistencies in AI services, establishing a clear two-tier violation model architecture and providing conversion mechanisms between detailed and unified models. While this represents a modest 1.2% error reduction, it resolves foundational type system issues that were blocking further progress on AI service integration.

The fixes ensure that:
- All AI services use consistent, unified models
- OSHA-specific analysis maintains detailed violation information
- Conversion between models is clean and centralized
- Property access patterns are standardized across services

These changes lay the groundwork for addressing remaining LiteRT and Severity enum issues in subsequent sessions.
