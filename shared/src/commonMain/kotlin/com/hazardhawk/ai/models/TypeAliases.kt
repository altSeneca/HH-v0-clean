package com.hazardhawk.ai.models

import com.hazardhawk.core.models.AnalysisCapability as CoreAnalysisCapability
import com.hazardhawk.core.models.AnalysisMetadata as CoreAnalysisMetadata
import com.hazardhawk.core.models.AnalysisType as CoreAnalysisType
import com.hazardhawk.core.models.BoundingBox as CoreBoundingBox
import com.hazardhawk.core.models.Hazard as CoreHazard
import com.hazardhawk.core.models.HazardType as CoreHazardType
import com.hazardhawk.core.models.Location as CoreLocation
import com.hazardhawk.core.models.OSHAViolation as CoreOSHAViolation
import com.hazardhawk.core.models.PPEItem as CorePPEItem
import com.hazardhawk.core.models.PPEItemStatus as CorePPEItemStatus
import com.hazardhawk.core.models.PPEStatus as CorePPEStatus
import com.hazardhawk.core.models.RiskLevel as CoreRiskLevel
import com.hazardhawk.core.models.SafetyAnalysis as CoreSafetyAnalysis
import com.hazardhawk.core.models.Severity as CoreSeverity
import com.hazardhawk.core.models.WeatherConditions as CoreWeatherConditions
import com.hazardhawk.core.models.WorkType as CoreWorkType

/**
 * Type aliases for backward compatibility during migration from ai.models to core.models.
 *
 * Migration Path:
 * 1. This file provides temporary aliases so existing code continues to work
 * 2. Gradually update imports in all files: com.hazardhawk.ai.models.* → com.hazardhawk.core.models.*
 * 3. Once all files are updated, delete this file
 *
 * DO NOT add new types here - add them directly to core.models
 *
 * @deprecated Use com.hazardhawk.core.models.* directly
 */

/**
 * Core data model type aliases
 */
@Deprecated(
    "Use com.hazardhawk.core.models.SafetyAnalysis instead",
    ReplaceWith("com.hazardhawk.core.models.SafetyAnalysis", "com.hazardhawk.core.models.SafetyAnalysis")
)
typealias SafetyAnalysis = CoreSafetyAnalysis

@Deprecated(
    "Use com.hazardhawk.core.models.Hazard instead",
    ReplaceWith("com.hazardhawk.core.models.Hazard", "com.hazardhawk.core.models.Hazard")
)
typealias Hazard = CoreHazard

@Deprecated(
    "Use com.hazardhawk.core.models.BoundingBox instead",
    ReplaceWith("com.hazardhawk.core.models.BoundingBox", "com.hazardhawk.core.models.BoundingBox")
)
typealias BoundingBox = CoreBoundingBox

@Deprecated(
    "Use com.hazardhawk.core.models.PPEStatus instead",
    ReplaceWith("com.hazardhawk.core.models.PPEStatus", "com.hazardhawk.core.models.PPEStatus")
)
typealias PPEStatus = CorePPEStatus

@Deprecated(
    "Use com.hazardhawk.core.models.PPEItem instead",
    ReplaceWith("com.hazardhawk.core.models.PPEItem", "com.hazardhawk.core.models.PPEItem")
)
typealias PPEItem = CorePPEItem

@Deprecated(
    "Use com.hazardhawk.core.models.OSHAViolation instead",
    ReplaceWith("com.hazardhawk.core.models.OSHAViolation", "com.hazardhawk.core.models.OSHAViolation")
)
typealias OSHAViolation = CoreOSHAViolation

@Deprecated(
    "Use com.hazardhawk.core.models.AnalysisMetadata instead",
    ReplaceWith("com.hazardhawk.core.models.AnalysisMetadata", "com.hazardhawk.core.models.AnalysisMetadata")
)
typealias AnalysisMetadata = CoreAnalysisMetadata

@Deprecated(
    "Use com.hazardhawk.core.models.Location instead",
    ReplaceWith("com.hazardhawk.core.models.Location", "com.hazardhawk.core.models.Location")
)
typealias Location = CoreLocation

@Deprecated(
    "Use com.hazardhawk.core.models.WeatherConditions instead",
    ReplaceWith("com.hazardhawk.core.models.WeatherConditions", "com.hazardhawk.core.models.WeatherConditions")
)
typealias WeatherConditions = CoreWeatherConditions

/**
 * Enum type aliases
 */
@Deprecated(
    "Use com.hazardhawk.core.models.HazardType instead",
    ReplaceWith("com.hazardhawk.core.models.HazardType", "com.hazardhawk.core.models.HazardType")
)
typealias HazardType = CoreHazardType

@Deprecated(
    "Use com.hazardhawk.core.models.Severity instead",
    ReplaceWith("com.hazardhawk.core.models.Severity", "com.hazardhawk.core.models.Severity")
)
typealias Severity = CoreSeverity

@Deprecated(
    "Use com.hazardhawk.core.models.RiskLevel instead",
    ReplaceWith("com.hazardhawk.core.models.RiskLevel", "com.hazardhawk.core.models.RiskLevel")
)
typealias RiskLevel = CoreRiskLevel

@Deprecated(
    "Use com.hazardhawk.core.models.PPEItemStatus instead",
    ReplaceWith("com.hazardhawk.core.models.PPEItemStatus", "com.hazardhawk.core.models.PPEItemStatus")
)
typealias PPEItemStatus = CorePPEItemStatus

@Deprecated(
    "Use com.hazardhawk.core.models.WorkType instead",
    ReplaceWith("com.hazardhawk.core.models.WorkType", "com.hazardhawk.core.models.WorkType")
)
typealias WorkType = CoreWorkType

@Deprecated(
    "Use com.hazardhawk.core.models.AnalysisType instead",
    ReplaceWith("com.hazardhawk.core.models.AnalysisType", "com.hazardhawk.core.models.AnalysisType")
)
typealias AnalysisType = CoreAnalysisType

@Deprecated(
    "Use com.hazardhawk.core.models.AnalysisCapability instead",
    ReplaceWith("com.hazardhawk.core.models.AnalysisCapability", "com.hazardhawk.core.models.AnalysisCapability")
)
typealias AnalysisCapability = CoreAnalysisCapability
