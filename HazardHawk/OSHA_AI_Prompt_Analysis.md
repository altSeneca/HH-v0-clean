# OSHA AI Analysis Prompt Documentation

## Overview

This document details the prompt structure used by HazardHawk's AI system for OSHA compliance analysis of construction site photos. The prompt is sent to Google's Gemini Vision Pro API to analyze images and identify safety hazards.

## File Location

**Source File:** `shared/src/commonMain/kotlin/com/hazardhawk/ai/GeminiVisionAnalyzer.kt`
**Function:** `buildConstructionSafetyPrompt()` (lines 250-310)
**Called From:** PhotoViewer.kt OSHA analysis workflow

## Complete AI Prompt

```
Analyze this construction site photo for safety hazards and OSHA compliance issues.
Work Type Context: ${workType.name}

Return ONLY a JSON response with this EXACT structure:
{
    "hazards": [
        {
            "type": "hazard_type",
            "severity": "LOW|MEDIUM|HIGH|CRITICAL",
            "description": "brief description",
            "oshaCode": "OSHA standard (e.g., 1926.501)",
            "boundingBox": {
                "x": 0.5,
                "y": 0.5,
                "width": 0.2,
                "height": 0.3,
                "confidence": 0.85
            },
            "tags": ["tag1", "tag2"]
        }
    ],
    "ppe_compliance": {
        "status": "COMPLIANT|NON_COMPLIANT|UNKNOWN",
        "missing_ppe": ["hard_hat", "safety_vest", "gloves"],
        "detections": [
            {
                "item": "hard_hat",
                "present": false,
                "boundingBox": {
                    "x": 0.3,
                    "y": 0.1,
                    "width": 0.1,
                    "height": 0.15,
                    "confidence": 0.92
                }
            }
        ],
        "tags": ["ppe-violation", "hard-hat-missing"]
    },
    "recommendations": [
        {
            "priority": "HIGH|MEDIUM|LOW",
            "action": "specific recommended action",
            "tags": ["recommendation-tag"]
        }
    ]
}

CRITICAL REQUIREMENTS:
- Bounding box coordinates must be normalized (0.0-1.0) where 0,0 is top-left
- x,y represent CENTER of detected area
- width,height are relative to image dimensions
- Include confidence score (0.0-1.0) for each detection
- Focus on OSHA 1926.501 (fall protection), 1926.95-96 (PPE), 1926.416-417 (electrical)
- Identify specific hazard locations with accurate coordinates

Return ONLY valid JSON with no additional text or formatting.
```

## Prompt Structure Analysis

### 1. Context Setting
- **Work Type Integration**: Dynamically inserts work type context (GENERAL_CONSTRUCTION, ELECTRICAL, etc.)
- **Scope Definition**: Specifically targets OSHA compliance issues and safety hazards

### 2. Response Format Requirements
- **JSON-Only Output**: Requires structured JSON response with no additional text
- **Schema Enforcement**: Provides exact JSON structure template
- **Field Validation**: Specifies allowed values for enums (severity, status, priority)

### 3. Detection Categories

#### Hazards Array
- **Type Classification**: Open-ended hazard type identification
- **Severity Levels**: LOW, MEDIUM, HIGH, CRITICAL classification
- **OSHA Mapping**: Direct OSHA standard code association
- **Spatial Location**: Normalized bounding box coordinates
- **Tag Generation**: Automatic safety tag creation

#### PPE Compliance Section
- **Compliance Status**: COMPLIANT, NON_COMPLIANT, or UNKNOWN
- **Missing Equipment**: Array of missing PPE items
- **Detection Details**: Individual PPE item detection with coordinates
- **Violation Tagging**: Automatic PPE violation tag generation

#### Recommendations Array
- **Priority Levels**: HIGH, MEDIUM, LOW prioritization
- **Actionable Steps**: Specific corrective actions
- **Tag Association**: Recommendation-specific tags

### 4. Technical Requirements

#### Coordinate System
- **Normalization**: All coordinates in 0.0-1.0 range
- **Origin Point**: Top-left corner (0,0)
- **Center Reference**: x,y represent center of detected area
- **Relative Dimensions**: width/height relative to image size

#### Confidence Scoring
- **Range**: 0.0-1.0 confidence values
- **Detection Quality**: Higher values indicate more certain detections
- **Threshold Logic**: Used for filtering low-confidence detections

#### OSHA Standards Focus
- **Fall Protection**: 1926.501 (primary focus)
- **Personal Protective Equipment**: 1926.95-96
- **Electrical Safety**: 1926.416-417
- **Extensible**: Can identify other relevant standards

## Processing Workflow

### 1. Request Creation
```kotlin
// GeminiVisionAnalyzer.kt:220-248
private fun createAnalysisRequest(encryptedData: ByteArray, workType: WorkType): GeminiVisionRequest
```

### 2. API Communication
```kotlin
// GeminiVisionAnalyzer.kt:312-336
private suspend fun sendToGeminiAPI(request: GeminiVisionRequest): GeminiVisionResponse
```

### 3. Response Processing
```kotlin
// GeminiVisionAnalyzer.kt:382-544
private fun processGeminiResponse(response: GeminiVisionResponse, workType: WorkType): GeminiAnalysisResult
```

### 4. OSHA Conversion
```kotlin
// PhotoViewer.kt:1104-1424
private fun convertAIAnalysisToOSHA(aiAnalysis: PhotoAnalysisWithTags, workType: WorkType): OSHAAnalysisResult
```

## Response Mapping

### Hazard Type Mapping
| AI Response | ConstructionHazardType | OSHA Standard |
|-------------|------------------------|---------------|
| fall_protection | WORKING_AT_HEIGHT_WITHOUT_PROTECTION | 1926.501 |
| missing_hard_hat | MISSING_HARD_HAT | 1926.95 |
| missing_safety_vest | MISSING_SAFETY_VEST | 1926.95 |
| electrical_hazard | ELECTRICAL_HAZARD | 1926.416 |
| unguarded_edge | UNGUARDED_EDGE | 1926.501 |

### Severity Mapping
| AI Response | Internal Severity | OSHA Priority |
|-------------|------------------|---------------|
| CRITICAL | Severity.CRITICAL | Immediate Action |
| HIGH | Severity.HIGH | High Priority |
| MEDIUM | Severity.MEDIUM | Medium Priority |
| LOW | Severity.LOW | Low Priority |

## Error Handling and Fallbacks

### API Failure Response
```kotlin
// GeminiVisionAnalyzer.kt:354-380
private fun createMockResponse(): String
```

### Default Tags by Work Type
```kotlin
// GeminiVisionAnalyzer.kt:546-556
private fun getDefaultTagsForWorkType(workType: WorkType): List<String>
```

## Security Considerations

### Data Protection
- **Photo Encryption**: Images are encrypted before API transmission
- **API Key Security**: Secure storage using SecureKeyManager
- **Response Sanitization**: JSON parsing with unknown key ignoring

### Privacy Compliance
- **No Personal Data**: Prompt focuses only on safety elements
- **Audit Logging**: All analyses logged for compliance tracking
- **Data Retention**: Configurable retention periods for OSHA compliance

## Model Configuration

### Gemini Vision Pro Settings
```kotlin
generationConfig = GeminiGenerationConfig(
    temperature = 0.4,        // Low creativity for consistent safety analysis
    topK = 32,               // Limited vocabulary for precision
    topP = 1.0,              // Full probability distribution
    maxOutputTokens = 2048   // Sufficient for detailed JSON response
)
```

### Request Timeout
- **API Timeout**: 60 seconds for analysis completion
- **Connection Timeout**: 30 seconds for initial connection
- **Socket Timeout**: 60 seconds for data transfer

## Usage in Application

### Trigger Points
1. **Manual Analysis**: User taps "Analyze" button in OSHA tab
2. **Workflow Integration**: Part of photo review process
3. **Batch Processing**: Multiple photos analysis capability

### Result Integration
- **UI Display**: Formatted results in PhotoViewer OSHA panel
- **PDF Export**: Analysis results included in safety reports
- **Tag Generation**: Automatic photo tagging based on findings
- **Compliance Scoring**: Overall safety score calculation

## Future Enhancements

### Prompt Evolution
- **Dynamic Prompting**: Adjust prompt based on detected work environment
- **Seasonal Considerations**: Weather-related safety focus
- **Site-Specific Rules**: Custom safety standards integration

### Response Enhancement
- **Multi-Language Support**: Localized safety terminology
- **Detailed Recommendations**: Step-by-step corrective actions
- **Cost Estimation**: Financial impact of safety improvements

---

*This document was generated on 2025-09-24 for HazardHawk v0 AI analysis system review.*