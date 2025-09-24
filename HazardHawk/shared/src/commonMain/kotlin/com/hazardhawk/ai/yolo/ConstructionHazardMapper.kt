package com.hazardhawk.ai.yolo

import com.hazardhawk.domain.entities.HazardType
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.models.Severity
import com.hazardhawk.models.SafetyAnalysis
import com.hazardhawk.models.Hazard
import com.hazardhawk.models.OSHACode
import kotlinx.datetime.Clock

/**
 * Construction Hazard Mapper for YOLO11 Detection Integration
 * 
 * This class provides comprehensive mapping between YOLO11 object detection results
 * and construction safety hazards, with OSHA compliance integration and 
 * context-aware risk assessment based on work type and environmental factors.
 */
class ConstructionHazardMapper {
    
    companion object {
        // Standard YOLO11 class names to construction hazard mappings
        private val YOLO_CLASS_TO_HAZARD_MAP = mapOf(
            // People and PPE detection
            0 to "person", // Used for PPE compliance checking
            
            // Construction equipment and vehicles
            2 to "car", 
            5 to "bus",
            7 to "truck",
            
            // Tools and equipment (custom construction classes would be added here)
            // These would be from a construction-specific YOLO model
            80 to "ladder",
            81 to "scaffold",
            82 to "crane",
            83 to "excavator",
            84 to "concrete_mixer",
            85 to "hard_hat",
            86 to "safety_vest",
            87 to "safety_glasses",
            88 to "gloves",
            89 to "guardrail",
            90 to "safety_barrier",
            91 to "warning_sign",
            92 to "fire_extinguisher",
            93 to "electrical_panel",
            94 to "exposed_wiring",
            95 to "debris",
            96 to "tool",
            97 to "machinery",
            98 to "lifting_equipment",
            99 to "hazardous_material"
        )
        
        // Confidence thresholds by hazard type (more critical hazards need higher confidence)
        private val CONFIDENCE_THRESHOLDS = mapOf(
            ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION to 0.8f,
            ConstructionHazardType.ELECTRICAL_HAZARD to 0.8f,
            ConstructionHazardType.FIRE_HAZARD to 0.8f,
            ConstructionHazardType.MISSING_HARD_HAT to 0.7f,
            ConstructionHazardType.UNGUARDED_EDGE to 0.7f,
            ConstructionHazardType.UNSAFE_EQUIPMENT_OPERATION to 0.7f,
            ConstructionHazardType.MISSING_SAFETY_VEST to 0.6f,
            ConstructionHazardType.TRIP_HAZARDS to 0.6f,
            ConstructionHazardType.CLUTTERED_WORKSPACE to 0.5f
        )
        
        // OSHA reference codes for construction hazards
        private val OSHA_REFERENCES = mapOf(
            ConstructionHazardType.MISSING_HARD_HAT to OSHACode(
                code = "29 CFR 1926.95(a)",
                title = "Personal Protective Equipment - Head Protection",
                description = "Employees working in areas where there is a possible danger of head injury from impact, or from falling or flying objects, or from electrical shock and burns, shall be protected by protective helmets.",
                url = "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.95"
            ),
            ConstructionHazardType.MISSING_SAFETY_VEST to OSHACode(
                code = "29 CFR 1926.95(d)",
                title = "Personal Protective Equipment - High-Visibility Safety Apparel",
                description = "High-visibility safety apparel is required for employees exposed to vehicular traffic or construction equipment within highway and street right-of-ways.",
                url = "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.95"
            ),
            ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION to OSHACode(
                code = "29 CFR 1926.501(b)(1)",
                title = "Fall Protection - General Requirements",
                description = "Each employee on a walking/working surface with an unprotected side or edge which is 6 feet or more above a lower level shall be protected from falling.",
                url = "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.501"
            ),
            ConstructionHazardType.UNGUARDED_EDGE to OSHACode(
                code = "29 CFR 1926.501(b)(1)",
                title = "Fall Protection - Unprotected Sides and Edges",
                description = "Each employee on a walking/working surface with an unprotected side or edge which is 6 feet or more above a lower level shall be protected from falling by the use of guardrail systems, safety net systems, or personal fall arrest systems.",
                url = "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.501"
            ),
            ConstructionHazardType.ELECTRICAL_HAZARD to OSHACode(
                code = "29 CFR 1926.95(c)",
                title = "Personal Protective Equipment - Electrical Protection",
                description = "Employees working in areas where there is a possible danger of electrical shock or burns shall be provided with electrically insulated protective equipment.",
                url = "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.95"
            ),
            ConstructionHazardType.FIRE_HAZARD to OSHACode(
                code = "29 CFR 1926.150",
                title = "Fire Protection",
                description = "An employer shall provide firefighting equipment and ensure fire protection measures are in place.",
                url = "https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.150"
            )
        )
    }
    
    /**
     * Convert YOLO detection results to construction safety analysis
     */
    fun mapToSafetyAnalysis(
        yoloResult: YOLODetectionResult,
        workType: WorkType,
        photoId: String
    ): SafetyAnalysis {
        val constructionHazards = identifyConstructionHazards(yoloResult, workType)
        val hazards = constructionHazards.map { it.toHazard() }
        val oshaCodes = constructionHazards.mapNotNull { OSHA_REFERENCES[it.hazardType] }
        val overallSeverity = determineOverallSeverity(constructionHazards)
        val recommendations = generateRecommendations(constructionHazards, workType)
        
        return SafetyAnalysis(
            id = "yolo-analysis-${Clock.System.now().toEpochMilliseconds()}",
            photoId = photoId,
            hazards = hazards,
            oshaCodes = oshaCodes,
            severity = overallSeverity,
            recommendations = recommendations,
            aiConfidence = calculateOverallConfidence(yoloResult.detections),
            analyzedAt = Clock.System.now(),
            analysisType = com.hazardhawk.models.AnalysisType.ON_DEVICE
        )
    }
    
    /**
     * Identify construction hazards from YOLO detections
     */
    private fun identifyConstructionHazards(
        yoloResult: YOLODetectionResult,
        workType: WorkType
    ): List<ConstructionHazardDetection> {
        val hazards = mutableListOf<ConstructionHazardDetection>()
        
        // Apply work type specific analysis
        val contextualDetections = applyWorkTypeContext(yoloResult.detections, workType)
        
        for (detection in contextualDetections) {
            val className = YOLO_CLASS_TO_HAZARD_MAP[detection.classId] ?: detection.className
            val hazardType = mapClassNameToHazard(className, detection, workType)
            
            hazardType?.let { type ->
                val confidenceThreshold = CONFIDENCE_THRESHOLDS[type] ?: 0.5f
                if (detection.confidence >= confidenceThreshold) {
                    hazards.add(
                        ConstructionHazardDetection(
                            hazardType = type,
                            boundingBox = detection,
                            severity = YOLOClassMapper.getSeverity(type),
                            oshaReference = OSHA_REFERENCES[type]?.code
                        )
                    )
                }
            }
        }
        
        // Perform contextual analysis (e.g., person without hard hat)
        hazards.addAll(performContextualAnalysis(yoloResult.detections, workType))
        
        return hazards
    }
    
    /**
     * Map YOLO class name to construction hazard type
     */
    private fun mapClassNameToHazard(
        className: String,
        detection: YOLOBoundingBox,
        workType: WorkType
    ): ConstructionHazardType? {
        return when (className.lowercase()) {
            "ladder" -> if (isUnsafePosition(detection)) ConstructionHazardType.LADDER_UNSAFE_POSITION else null
            "scaffold" -> if (isMissingGuardrails(detection)) ConstructionHazardType.SCAFFOLD_VIOLATION else null
            "debris" -> ConstructionHazardType.DEBRIS_ACCUMULATION
            "exposed_wiring" -> ConstructionHazardType.EXPOSED_WIRING
            "electrical_panel" -> if (isElectricalHazard(detection)) ConstructionHazardType.ELECTRICAL_HAZARD else null
            "machinery" -> if (isUnsafeOperation(detection)) ConstructionHazardType.UNSAFE_EQUIPMENT_OPERATION else null
            "tool" -> if (isDamaged(detection)) ConstructionHazardType.DAMAGED_TOOLS else null
            "fire_extinguisher" -> null // Presence of fire extinguisher is good
            "warning_sign" -> null // Presence of warning signs is good
            "guardrail" -> null // Presence of guardrails is good
            "safety_barrier" -> null // Presence of barriers is good
            else -> null
        }
    }
    
    /**
     * Apply work type specific context to detections
     */
    private fun applyWorkTypeContext(
        detections: List<YOLOBoundingBox>,
        workType: WorkType
    ): List<YOLOBoundingBox> {
        return when (workType) {
            WorkType.ELECTRICAL, WorkType.ELECTRICAL_SAFETY -> {
                // Prioritize electrical hazards
                detections.filter { detection ->
                    val className = YOLO_CLASS_TO_HAZARD_MAP[detection.classId] ?: detection.className
                    className.contains("electrical", ignoreCase = true) || 
                    className.contains("wiring", ignoreCase = true)
                } + detections
            }
            WorkType.ROOFING, WorkType.FALL_PROTECTION -> {
                // Focus on fall protection
                detections.filter { detection ->
                    val className = YOLO_CLASS_TO_HAZARD_MAP[detection.classId] ?: detection.className
                    className.contains("ladder", ignoreCase = true) ||
                    className.contains("scaffold", ignoreCase = true) ||
                    className.contains("guardrail", ignoreCase = true)
                } + detections
            }
            else -> detections
        }
    }
    
    /**
     * Perform contextual analysis to identify complex hazards
     */
    private fun performContextualAnalysis(
        detections: List<YOLOBoundingBox>,
        workType: WorkType
    ): List<ConstructionHazardDetection> {
        val contextualHazards = mutableListOf<ConstructionHazardDetection>()
        
        // Find people in the image
        val people = detections.filter { it.classId == 0 } // Class 0 is typically "person" in YOLO
        val hardHats = detections.filter { YOLO_CLASS_TO_HAZARD_MAP[it.classId] == "hard_hat" }
        val safetyVests = detections.filter { YOLO_CLASS_TO_HAZARD_MAP[it.classId] == "safety_vest" }
        
        // Check for PPE violations
        for (person in people) {
            // Check for missing hard hat
            if (!hasNearbyPPE(person, hardHats)) {
                contextualHazards.add(
                    ConstructionHazardDetection(
                        hazardType = ConstructionHazardType.MISSING_HARD_HAT,
                        boundingBox = person,
                        severity = Severity.HIGH,
                        oshaReference = OSHA_REFERENCES[ConstructionHazardType.MISSING_HARD_HAT]?.code
                    )
                )
            }
            
            // Check for missing safety vest
            if (requiresSafetyVest(workType) && !hasNearbyPPE(person, safetyVests)) {
                contextualHazards.add(
                    ConstructionHazardDetection(
                        hazardType = ConstructionHazardType.MISSING_SAFETY_VEST,
                        boundingBox = person,
                        severity = Severity.MEDIUM,
                        oshaReference = OSHA_REFERENCES[ConstructionHazardType.MISSING_SAFETY_VEST]?.code
                    )
                )
            }
            
            // Check for working at height without protection
            if (isWorkingAtHeight(person, detections) && !hasProperFallProtection(person, detections)) {
                contextualHazards.add(
                    ConstructionHazardDetection(
                        hazardType = ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION,
                        boundingBox = person,
                        severity = Severity.CRITICAL,
                        oshaReference = OSHA_REFERENCES[ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION]?.code
                    )
                )
            }
        }
        
        return contextualHazards
    }
    
    // Helper methods for contextual analysis
    
    private fun hasNearbyPPE(
        person: YOLOBoundingBox, 
        ppeItems: List<YOLOBoundingBox>
    ): Boolean {
        val proximityThreshold = 0.3f // 30% of image width/height
        return ppeItems.any { ppe ->
            val distance = calculateDistance(person, ppe)
            distance < proximityThreshold
        }
    }
    
    private fun calculateDistance(
        box1: YOLOBoundingBox, 
        box2: YOLOBoundingBox
    ): Float {
        val dx = box1.x - box2.x
        val dy = box1.y - box2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
    
    private fun requiresSafetyVest(workType: WorkType): Boolean {
        return when (workType) {
            WorkType.GENERAL_CONSTRUCTION,
            WorkType.CONCRETE,
            WorkType.STEEL_WORK,
            WorkType.DEMOLITION -> true
            else -> false
        }
    }
    
    private fun isWorkingAtHeight(
        person: YOLOBoundingBox, 
        allDetections: List<YOLOBoundingBox>
    ): Boolean {
        // Check if person is near elevated equipment (scaffold, ladder, etc.)
        val elevatedEquipment = allDetections.filter { detection ->
            val className = YOLO_CLASS_TO_HAZARD_MAP[detection.classId] ?: detection.className
            className.lowercase() in listOf("scaffold", "ladder", "crane")
        }
        return elevatedEquipment.any { equipment ->
            calculateDistance(person, equipment) < 0.2f
        }
    }
    
    private fun hasProperFallProtection(
        person: YOLOBoundingBox, 
        allDetections: List<YOLOBoundingBox>
    ): Boolean {
        val fallProtectionEquipment = allDetections.filter { detection ->
            val className = YOLO_CLASS_TO_HAZARD_MAP[detection.classId] ?: detection.className
            className.lowercase() in listOf("guardrail", "safety_barrier", "harness")
        }
        return fallProtectionEquipment.any { equipment ->
            calculateDistance(person, equipment) < 0.3f
        }
    }
    
    // Additional helper methods for hazard classification
    
    private fun isUnsafePosition(detection: YOLOBoundingBox): Boolean {
        // Logic to determine if ladder is in unsafe position
        // This would analyze angle, proximity to walls, etc.
        return detection.confidence > 0.6f // Simplified logic
    }
    
    private fun isMissingGuardrails(detection: YOLOBoundingBox): Boolean {
        // Logic to determine if scaffold is missing guardrails
        return detection.confidence > 0.7f // Simplified logic
    }
    
    private fun isElectricalHazard(detection: YOLOBoundingBox): Boolean {
        // Logic to determine if electrical panel poses hazard
        return detection.confidence > 0.8f // Simplified logic
    }
    
    private fun isUnsafeOperation(detection: YOLOBoundingBox): Boolean {
        // Logic to determine unsafe equipment operation
        return detection.confidence > 0.7f // Simplified logic
    }
    
    private fun isDamaged(detection: YOLOBoundingBox): Boolean {
        // Logic to determine if tool appears damaged
        return detection.confidence > 0.8f // Simplified logic
    }
    
    private fun determineOverallSeverity(
        hazards: List<ConstructionHazardDetection>
    ): Severity {
        return when {
            hazards.any { it.severity == Severity.CRITICAL } -> Severity.CRITICAL
            hazards.any { it.severity == Severity.HIGH } -> Severity.HIGH
            hazards.any { it.severity == Severity.MEDIUM } -> Severity.MEDIUM
            else -> Severity.LOW
        }
    }
    
    private fun generateRecommendations(
        hazards: List<ConstructionHazardDetection>,
        workType: WorkType
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        hazards.forEach { hazard ->
            when (hazard.hazardType) {
                ConstructionHazardType.MISSING_HARD_HAT -> 
                    recommendations.add("Ensure all workers wear appropriate head protection (hard hats) in construction areas")
                ConstructionHazardType.MISSING_SAFETY_VEST -> 
                    recommendations.add("Workers must wear high-visibility safety vests when working near vehicular traffic or heavy equipment")
                ConstructionHazardType.WORKING_AT_HEIGHT_WITHOUT_PROTECTION -> 
                    recommendations.add("Implement fall protection systems (guardrails, safety nets, or personal fall arrest systems) for work at heights above 6 feet")
                ConstructionHazardType.ELECTRICAL_HAZARD -> 
                    recommendations.add("Ensure electrical hazards are properly identified, de-energized, and protected before work begins")
                ConstructionHazardType.CLUTTERED_WORKSPACE -> 
                    recommendations.add("Maintain clean and organized work areas to prevent trips, falls, and other accidents")
                else -> recommendations.add("Address identified safety hazard according to company safety protocols")
            }
        }
        
        return recommendations.distinct()
    }
    
    private fun calculateOverallConfidence(detections: List<YOLOBoundingBox>): Float {
        return if (detections.isNotEmpty()) {
            detections.map { it.confidence }.average().toFloat()
        } else {
            0.0f
        }
    }
}

/**
 * Extension function to convert ConstructionHazardDetection to Hazard
 */
fun ConstructionHazardDetection.toHazard(): Hazard {
    return Hazard(
        id = "hazard-${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}",
        type = hazardType.toHazardType(),
        description = description,
        severity = severity,
        confidence = boundingBox.confidence,
        boundingBox = com.hazardhawk.models.BoundingBox(
            x = boundingBox.x,
            y = boundingBox.y,
            width = boundingBox.width,
            height = boundingBox.height
        ),
        oshaReference = oshaReference
    )
}
