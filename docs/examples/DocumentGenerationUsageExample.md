# Document Generation Service Usage Examples

## Overview

This document provides practical examples of using the new DocumentGenerationService for creating AI-powered PTPs and Toolbox Talks in HazardHawk.

## Core Components

### 1. DocumentGenerationService
Main interface for document generation with Flow-based progress tracking:
- `generatePTP(context: PTPGenerationContext): Flow<DocumentGenerationProgress>`
- `generateToolboxTalk(context: ToolboxTalkContext): Flow<DocumentGenerationProgress>`

### 2. DocumentGenerationModels
Comprehensive data models including:
- `PTPGenerationContext` - Context for Pre-Task Plan creation
- `ToolboxTalkContext` - Context for Toolbox Talk creation
- `DocumentGenerationProgress` - Real-time progress tracking
- `AIGeneratedContent` - Structured AI output

### 3. Enhanced SafetyReportTemplates
AI integration extensions:
- `createAIEnhancedPTPTemplate()` - Enhanced PTP template
- `createAIEnhancedToolboxTalkTemplate()` - Enhanced Toolbox Talk template
- `SafetyReport.enhanceWithAIContent()` - Extension function for AI enhancement

## Usage Examples

### Example 1: Generate PTP for Roofing Work

```kotlin
// Create generation context
val ptpContext = PTPGenerationContext(
    workType = WorkType.ROOFING,
    location = LocationInfo(
        latitude = 37.7749,
        longitude = -122.4194,
        siteName = "Downtown Construction Project",
        address = "123 Main St, San Francisco, CA"
    ),
    crewSize = 4,
    weatherConditions = WeatherConditions(
        temperature = 22.0,
        humidity = 65.0,
        windSpeed = 15.0,
        precipitation = PrecipitationType.NONE,
        visibility = VisibilityLevel.GOOD
    ),
    voiceDescription = "Installing asphalt shingles on steep roof, 3-story building",
    equipmentList = listOf("Safety harnesses", "Ladders", "Nail guns", "Scaffolding")
)

// Generate PTP with progress tracking
documentService.generatePTP(ptpContext).collect { progress ->
    when (progress) {
        is DocumentGenerationProgress.Starting -> {
            showProgressDialog("Initializing PTP generation...")
        }
        is DocumentGenerationProgress.Processing -> {
            updateProgress(progress.stage, progress.progress)
        }
        is DocumentGenerationProgress.Completed -> {
            val enhancedPTP = progress.document
            displayPTP(enhancedPTP)
            hideProgressDialog()
        }
        is DocumentGenerationProgress.Failed -> {
            showError("PTP generation failed: ${progress.error.message}")
            hideProgressDialog()
        }
    }
}
```

### Example 2: Generate Toolbox Talk for Fall Protection

```kotlin
// Create toolbox talk context
val toolboxContext = ToolboxTalkContext(
    topic = SafetyTopic.FALL_PROTECTION,
    attendees = listOf("John Smith", "Maria Garcia", "David Kim", "Sarah Johnson"),
    duration = 15.minutes,
    customContent = "Focus on harness inspection procedures and ladder safety",
    recentIncidents = listOf("Near miss: Worker slipped on wet ladder last week"),
    seasonalTopics = listOf("Wet weather safety", "Reduced daylight hours"),
    interactiveElements = true
)

// Generate Toolbox Talk
documentService.generateToolboxTalk(toolboxContext).collect { progress ->
    when (progress) {
        is DocumentGenerationProgress.Completed -> {
            val toolboxTalk = progress.document
            
            // Enhanced content includes:
            // - AI-generated discussion points
            // - Interactive learning elements
            // - Real-world construction examples
            // - OSHA compliance references
            
            presentToolboxTalk(toolboxTalk)
        }
        is DocumentGenerationProgress.Failed -> {
            // Fallback to template-based generation
            generateBasicToolboxTalk(toolboxContext)
        }
        else -> updateProgressUI(progress)
    }
}
```

### Example 3: AI Content Enhancement

```kotlin
// Create base safety report
val baseReport = SafetyReportTemplates.getTemplateById("pre_task_plan")
    ?.let { template ->
        SafetyReport(
            id = generateReportId(),
            templateId = template.id,
            templateName = template.name,
            reportType = template.type,
            title = "Electrical Panel Installation PTP",
            // ... other required fields
        )
    }

// Create AI-generated content
val aiContent = AIGeneratedContent(
    hazardSuggestions = listOf(
        HazardIdentification(
            type = "Electrical Shock",
            severity = HazardSeverity.CRITICAL,
            description = "Risk of arc flash during panel energization",
            likelihood = RiskLikelihood.MEDIUM,
            oshaStandard = "1926.416",
            mitigation = "Use appropriate arc-rated PPE and follow LOTO procedures"
        )
    ),
    safetyMeasures = listOf(
        SafetyMeasure(
            category = SafetyCategory.PPE,
            description = "Arc-rated face shield and flame-resistant clothing",
            priority = Priority.CRITICAL,
            responsibleParty = "Electrical technicians"
        )
    ),
    oshaReferences = listOf(
        OSHAReference(
            standard = "1926.416",
            title = "General Requirements for Electrical Equipment",
            requirement = "Employees must be protected from electrical hazards",
            applicability = "All electrical work on construction sites"
        )
    ),
    contextualRecommendations = listOf(
        "Weather conditions require additional precautions for outdoor electrical work",
        "Ensure backup power source is available during panel installation"
    )
)

// Enhance report with AI content
val enhancedReport = baseReport?.enhanceWithAIContent(aiContent)

// Add generation metadata for audit trail
val metadata = DocumentGenerationMetadata(
    generatedAt = Clock.System.now(),
    aiModel = "gemini-1.5-pro",
    promptVersion = "1.2",
    processingTimeMs = 2800L
)

val finalReport = enhancedReport?.addDocumentGenerationMetadata(metadata)
```

### Example 4: Error Handling and Validation

```kotlin
// Validate context before generation
val context = PTPGenerationContext(
    workType = WorkType.ELECTRICAL,
    location = LocationInfo(0.0, 0.0, siteName = null), // Invalid
    crewSize = 0, // Invalid
    weatherConditions = WeatherConditions()
)

val validationErrors = context.validate()
if (validationErrors.isNotEmpty()) {
    showValidationErrors(validationErrors)
    return
}

// Handle generation errors gracefully
documentService.generatePTP(context).catch { exception ->
    val error = DocumentGenerationError.from(exception)
    when (error) {
        is DocumentGenerationError.NetworkError -> {
            if (error.retryAfter != null) {
                scheduleRetry(error.retryAfter!!)
            }
            showNetworkError()
        }
        is DocumentGenerationError.AIGenerationFailed -> {
            if (error.retryable) {
                promptForRetry()
            } else {
                fallbackToTemplateGeneration()
            }
        }
        is DocumentGenerationError.ValidationError -> {
            showFieldErrors(error.fieldErrors)
        }
        is DocumentGenerationError.SystemError -> {
            logSystemError(error)
            showGenericError()
        }
    }
}.collect { progress ->
    // Handle successful progress
}
```

### Example 5: Weather-Aware Safety Recommendations

```kotlin
// High wind conditions
val windyWeather = WeatherConditions(
    temperature = 18.0,
    windSpeed = 45.0, // High winds
    precipitation = PrecipitationType.NONE,
    visibility = VisibilityLevel.MODERATE,
    alerts = listOf(
        WeatherAlert(
            type = "High Wind Warning",
            severity = AlertSeverity.HIGH,
            description = "Sustained winds 40-50 km/h with gusts to 65 km/h"
        )
    )
)

// Check if work is safe
if (!windyWeather.isSafeForWork()) {
    // AI will automatically include wind-specific hazards:
    // - Increased fall risk
    // - Material handling precautions  
    // - Crane operation restrictions
    // - Additional tie-off requirements
}

// Context with weather considerations
val windyContext = PTPGenerationContext(
    workType = WorkType.CRANE_LIFTING,
    location = constructionSite,
    crewSize = 6,
    weatherConditions = windyWeather,
    voiceDescription = "Lifting steel beams for third floor framing"
)

// Generated PTP will include weather-specific safety measures
documentService.generatePTP(windyContext).collect { progress ->
    if (progress is DocumentGenerationProgress.Completed) {
        val ptp = progress.document
        // AI-enhanced content will include:
        // - Wind speed monitoring requirements
        // - Modified lifting procedures
        // - Additional communication protocols
        // - Work stoppage thresholds
    }
}
```

## Integration with Existing HazardHawk Features

### 1. Voice Integration
```kotlin
// Use existing voice service
val voiceDescription = voiceToTextService.transcribe(audioData)

val context = PTPGenerationContext(
    // ... other fields
    voiceDescription = voiceDescription
)
```

### 2. Photo Integration
```kotlin
// Include recent site photos for context
val recentPhotos = photoGalleryService.getRecentPhotos(locationId, limit = 5)

val context = PTPGenerationContext(
    // ... other fields
    photos = recentPhotos.map { it.id }
)
```

### 3. User Profile Integration
```kotlin
// Get user-specific information
val userProfile = userService.getCurrentUser()

val reporterInfo = ReporterInformation(
    name = userProfile.fullName,
    title = userProfile.jobTitle,
    company = userProfile.company,
    email = userProfile.email,
    certification = userProfile.safetyCredentials.joinToString(", ")
)
```

## Performance Considerations

### Target Performance Metrics
- PTP Generation: < 5 seconds
- Toolbox Talk Generation: < 3 seconds  
- Memory Usage: < 1GB peak
- Battery Impact: < 0.3% per document

### Optimization Strategies
```kotlin
// Use coroutines for non-blocking operations
val deferredGeneration = async {
    documentService.generatePTP(context)
}

// Cache common AI responses
val cacheKey = "${workType}_${weatherConditions.hashCode()}"
val cachedContent = aiContentCache.get(cacheKey)

// Implement fallback for offline scenarios
if (!networkService.isOnline()) {
    return generateOfflinePTP(context)
}
```

## Testing and Validation

### Unit Test Coverage
- DocumentGenerationService: ✅ Comprehensive test suite
- DocumentGenerationModels: ✅ Validation and serialization tests
- SafetyReportTemplates: ✅ AI integration and enhancement tests

### Integration Testing
```kotlin
@Test
fun `end-to-end PTP generation should complete successfully`() = runTest {
    val context = createValidPTPContext()
    val results = documentService.generatePTP(context).toList()
    
    assertIs<DocumentGenerationProgress.Completed>(results.last())
    val report = (results.last() as DocumentGenerationProgress.Completed).document
    assertTrue(report.oshaCompliant)
    assertTrue(report.sections.isNotEmpty())
}
```

## Security Considerations

### Data Protection
- All API communications use certificate pinning
- User data is anonymized before sending to AI service
- Generated content includes audit trail metadata
- Secure storage for API keys using platform keychain

### Privacy Compliance
- GDPR/CCPA compliant data processing
- User consent required for AI processing
- Data retention policies enforced
- User rights (deletion, portability) supported

This comprehensive implementation provides HazardHawk with powerful AI-enhanced document generation capabilities while maintaining security, performance, and user experience standards.