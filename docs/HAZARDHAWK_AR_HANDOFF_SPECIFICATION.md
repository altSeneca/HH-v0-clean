# HazardHawk AR Implementation - Handoff Specification

## 1. Executive Summary

This document provides a complete handoff specification for implementing augmented reality (AR) hazard detection in HazardHawk. The AR system will display real-time safety violation overlays, OSHA compliance warnings, and safety zones directly in the camera viewfinder before photo capture.

**Target Outcome:** Transform HazardHawk from reactive documentation to proactive safety monitoring with live AR overlays showing fall protection violations, PPE compliance zones, and OSHA regulation references.

---

## 2. Branching, Code Style, and CI

### Branch Model
- **Trunk-based development** with short-lived feature branches
- Feature branches: `feature/ar-{component-name}` (max 3 days)
- Integration branch: `integration/ar-system` for AR-specific testing
- Release branches: `release/v{major}.{minor}.{patch}`

### Code Style & Static Analysis
```yaml
# .github/workflows/android.yml
name: Android CI
on:
  push:
    branches: [ main, integration/ar-system ]
  pull_request:
    branches: [ main ]

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - name: Setup JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Lint & Static Analysis
      run: |
        ./gradlew ktlintCheck detekt
        ./gradlew :shared:apiCheck
    
    - name: Unit Tests
      run: ./gradlew testDebugUnitTest -PminCoverage=0.85
    
    - name: AR Integration Tests
      run: ./gradlew :androidApp:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.hazardhawk.ar
    
    - name: Build AR-enabled APK
      run: ./gradlew :androidApp:assembleDebug -PenableAR=true
```

### Pre-commit Hooks
```bash
# .git/hooks/pre-commit
#!/bin/sh
./gradlew ktlintFormat detekt
if [ $? -ne 0 ]; then
  echo "❌ Linting failed. Fix issues before committing."
  exit 1
fi
```

### Test Coverage Gates
- **Minimum coverage:** 85% for AR-related modules
- **Critical paths:** 95% coverage for hazard detection and overlay rendering
- **Integration tests:** Required for all AR camera interactions

---

## 3. Module Map & File Layout

### Architecture Overview
```
HazardHawk/
├── shared/                                    # Cross-platform business logic
│   ├── src/commonMain/kotlin/com/hazardhawk/
│   │   ├── ar/                               # AR domain logic
│   │   │   ├── HazardDetector.kt             # Core detection interface
│   │   │   ├── OSHAComplianceEngine.kt       # Regulation matching
│   │   │   ├── ARTrackingManager.kt          # Tracking state management
│   │   │   └── models/                       # AR data models
│   │   ├── ai/                               # AI integration
│   │   │   ├── GeminiVisionAnalyzer.kt       # Enhanced for AR
│   │   │   └── AIServiceFacade.kt            # Updated interface
│   │   └── data/                             # Data layer
│   │       ├── OSHADatabase.kt               # OSHA regulations DB
│   │       └── repositories/
├── androidApp/                               # Android-specific implementation
│   ├── src/main/java/com/hazardhawk/
│   │   ├── ar/                              # AR controllers & renderers
│   │   │   ├── ARCameraController.kt         # ARCore integration
│   │   │   ├── AROverlayRenderer.kt          # Compose overlay system
│   │   │   ├── HazardOverlayManager.kt       # Overlay lifecycle
│   │   │   ├── TrackingStateManager.kt       # AR tracking state
│   │   │   └── coordinatesystem/             # Transform utilities
│   │   ├── ui/camera/                       # Enhanced camera UI
│   │   │   ├── EnhancedARCameraScreen.kt     # AR-enabled camera
│   │   │   ├── overlays/                     # Overlay components
│   │   │   └── fallback/                     # 2D fallback mode
│   │   └── di/                              # Dependency injection
│   │       └── ARModule.kt                   # AR dependencies
└── docs/                                     # Documentation
    ├── ar/                                   # AR-specific docs
    ├── handoff/                             # Implementation guides
    └── testing/                             # Test specifications
```

### Dependency Rules
- ❌ **Forbidden:** `androidApp` → `shared` (circular)
- ✅ **Allowed:** `shared` → platform-specific interfaces
- ❌ **Forbidden:** `ar/` → `ui/` direct imports
- ✅ **Required:** All AR dependencies via DI

---

## 4. Interfaces & Data Contracts

### Core AR Interfaces

```kotlin
// shared/src/commonMain/kotlin/com/hazardhawk/ar/HazardDetector.kt
interface HazardDetector {
    suspend fun analyzeFrame(
        frame: ARFrame,
        context: DetectionContext
    ): Result<HazardDetectionResult>
    
    suspend fun analyzeStatic(
        imageData: ByteArray,
        metadata: ImageMetadata
    ): Result<HazardDetectionResult>
}

// shared/src/commonMain/kotlin/com/hazardhawk/ar/models/DetectedHazard.kt
data class DetectedHazard(
    val id: String,
    val type: HazardType,
    val boundingBox: BoundingBox3D,
    val confidence: Float,
    val oshaViolations: List<OSHAViolation>,
    val severity: HazardSeverity,
    val recommendedActions: List<String>,
    val trackingAnchor: String? = null
)

sealed class HazardType {
    object FallProtection : HazardType()
    object PPEViolation : HazardType()
    object ElectricalHazard : HazardType()
    object MovingEquipment : HazardType()
    data class Custom(val name: String) : HazardType()
}

enum class HazardSeverity(val displayName: String, val color: Long) {
    CRITICAL("CRITICAL", 0xFFD32F2F),
    WARNING("WARNING", 0xFFF57C00),
    INFO("INFO", 0xFF1976D2)
}

data class BoundingBox3D(
    val center: Vector3,
    val size: Vector3,
    val rotation: Quaternion
)

data class OSHAViolation(
    val code: String,                          // "1926.501"
    val title: String,                         // "Fall Protection"
    val section: String,                       // "1926.501(a)(1)"
    val description: String,
    val correctiveActions: List<String>,
    val penalty: PenaltyRange?
)

// Result types
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable, val retryable: Boolean = false) : Result<Nothing>()
}

data class HazardDetectionResult(
    val hazards: List<DetectedHazard>,
    val processingTimeMs: Long,
    val frameQuality: FrameQuality,
    val trackingState: ARTrackingState
)
```

### AR Tracking Interfaces

```kotlin
// shared/src/commonMain/kotlin/com/hazardhawk/ar/ARTrackingManager.kt
interface ARTrackingManager {
    val trackingState: StateFlow<ARTrackingState>
    val lastKnownPose: StateFlow<CameraPose?>
    
    suspend fun initializeTracking(config: ARConfig): Result<Unit>
    suspend fun updateTracking(frame: ARFrame): Result<TrackingUpdate>
    fun createAnchor(pose: Pose): Result<TrackingAnchor>
    fun removeAnchor(anchorId: String): Result<Unit>
}

sealed interface ARTrackingState {
    object Initializing : ARTrackingState
    object Tracking : ARTrackingState
    data class Limited(val reason: LimitedReason) : ARTrackingState
    object Paused : ARTrackingState
    object Stopped : ARTrackingState
}

enum class LimitedReason {
    INSUFFICIENT_LIGHT,
    EXCESSIVE_MOTION,
    INSUFFICIENT_FEATURES,
    RELOCALIZING
}

data class CameraPose(
    val position: Vector3,
    val rotation: Quaternion,
    val timestamp: Long
)
```

### Overlay Rendering Interface

```kotlin
// androidApp/src/main/java/com/hazardhawk/ar/AROverlayRenderer.kt
interface AROverlayRenderer {
    fun renderHazardOverlay(
        hazard: DetectedHazard,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ): OverlayRenderData
    
    fun renderSafetyZone(
        zone: SafetyZone,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ): OverlayRenderData
    
    fun renderOSHAReference(
        violation: OSHAViolation,
        anchorPose: Pose
    ): OverlayRenderData
}

data class OverlayRenderData(
    val screenCoordinates: RectF,
    val depth: Float,
    val visible: Boolean,
    val renderInstructions: RenderInstructions
)
```

---

## 5. AI Integration Specifications

### Gemini Vision Integration

```kotlin
// shared/src/commonMain/kotlin/com/hazardhawk/ai/ARGeminiAnalyzer.kt
class ARGeminiAnalyzer : HazardDetector {
    private val config = GeminiConfig(
        model = "gemini-pro-vision",
        maxTokens = 4096,
        temperature = 0.1f,
        timeoutMs = 5000L,
        retryPolicy = ExponentialBackoff(
            initialDelayMs = 100L,
            maxDelayMs = 2000L,
            maxRetries = 3
        )
    )
    
    override suspend fun analyzeFrame(
        frame: ARFrame,
        context: DetectionContext
    ): Result<HazardDetectionResult> {
        // Frame preprocessing with privacy protection
        val processedFrame = preprocessFrame(frame)
        
        val prompt = buildPrompt(context, frame.metadata)
        
        return try {
            val response = geminiClient.analyzeImage(
                image = processedFrame.imageData,
                prompt = prompt,
                config = config
            )
            
            parseGeminiResponse(response, frame)
        } catch (e: Exception) {
            Result.Error(e, retryable = e is NetworkException)
        }
    }
}
```

### Frame Sampling Strategy

```kotlin
// Frame sampling configuration
data class FrameSamplingConfig(
    val baseIntervalMs: Long = 500L,              // Analyze every 500ms baseline
    val adaptiveMode: Boolean = true,              // Adjust based on tracking state
    val maxResolution: Size = Size(1280, 720),    // Downscale for performance
    val qualityThreshold: Float = 0.7f,           // Minimum quality for analysis
    val batteryOptimized: Boolean = true          // Reduce frequency on low battery
)

// Adaptive sampling logic
class AdaptiveFrameSampler(private val config: FrameSamplingConfig) {
    fun shouldAnalyzeFrame(
        trackingState: ARTrackingState,
        batteryLevel: Float,
        lastAnalysisMs: Long
    ): Boolean {
        val interval = when {
            trackingState is ARTrackingState.Limited -> config.baseIntervalMs * 2
            batteryLevel < 0.2f && config.batteryOptimized -> config.baseIntervalMs * 3
            else -> config.baseIntervalMs
        }
        
        return System.currentTimeMillis() - lastAnalysisMs >= interval
    }
}
```

### Privacy Protection Pipeline

```kotlin
// Privacy protection before API calls
class FramePrivacyProcessor {
    suspend fun processFrame(frame: ARFrame): ProcessedFrame {
        return ProcessedFrame(
            imageData = blurPersonalInfo(frame.imageData),
            metadata = sanitizeMetadata(frame.metadata),
            hash = generateFrameHash(frame)
        )
    }
    
    private suspend fun blurPersonalInfo(imageData: ByteArray): ByteArray {
        // On-device face detection and blurring
        val faces = faceDetector.detectFaces(imageData)
        return blurRegions(imageData, faces.map { it.boundingBox })
    }
    
    private fun sanitizeMetadata(metadata: FrameMetadata): FrameMetadata {
        return metadata.copy(
            gpsCoordinates = null,  // Remove precise location
            deviceId = metadata.deviceId.take(8) + "****"  // Partial hash only
        )
    }
}
```

### Prompt Templates

```markdown
# /docs/ai/prompts/construction_hazard_detection.md

## System Prompt
You are a construction safety expert analyzing job site images for OSHA compliance violations and safety hazards. Return JSON only.

## User Prompt Template
```
CONTEXT:
- Site Type: {site_type}
- Lighting: {lighting_condition}  
- Time: {time_of_day}
- Weather: {weather}
- Previous Detections: {recent_hazards}

FRAME DATA:
- Resolution: {width}x{height}
- AR Tracking: {tracking_state}
- Detected Planes: {plane_count}
- Camera Pose: position({x},{y},{z}) rotation({rx},{ry},{rz})

ANALYZE FOR:
1. Fall protection violations (guardrails, harnesses, nets)
2. PPE compliance (hard hats, safety vests, steel-toed boots)
3. Electrical hazards (exposed wiring, wet conditions near power)
4. Moving equipment (cranes, forklifts, vehicles)
5. Scaffolding safety (stability, access, fall protection)

RETURN JSON:
{
  "hazards": [
    {
      "type": "fall_protection",
      "description": "Worker at elevated platform without safety harness",
      "confidence": 0.92,
      "bounding_box": {
        "center": {"x": 0.3, "y": -0.2, "z": 5.0},
        "size": {"x": 1.5, "y": 2.0, "z": 0.5}
      },
      "osha_codes": ["1926.501"],
      "severity": "critical",
      "corrective_actions": [
        "Provide and require use of personal fall arrest system",
        "Install guardrail system with top rail at 42 inches"
      ]
    }
  ],
  "processing_time_ms": 1240,
  "frame_quality": "good"
}
```

## Example Response
```json
{
  "hazards": [
    {
      "type": "fall_protection",
      "description": "Worker at elevated platform without safety harness", 
      "confidence": 0.92,
      "bounding_box": {
        "center": {"x": 0.3, "y": -0.2, "z": 5.0},
        "size": {"x": 1.5, "y": 2.0, "z": 0.5}
      },
      "osha_codes": ["1926.501"],
      "severity": "critical",
      "corrective_actions": [
        "Provide and require use of personal fall arrest system",
        "Install guardrail system with top rail at 42 inches"
      ]
    }
  ],
  "processing_time_ms": 1240,
  "frame_quality": "good"
}
```
```

### Rate Limiting & Offline Mode

```kotlin
// AI service configuration
data class AIServiceConfig(
    val rateLimitPerMinute: Int = 30,
    val burstAllowance: Int = 5,
    val offlineMode: Boolean = false,
    val fallbackToCache: Boolean = true,
    val cacheExpirationHours: Int = 24
)

// Offline fallback using on-device models
class OfflineHazardDetector : HazardDetector {
    override suspend fun analyzeFrame(
        frame: ARFrame,
        context: DetectionContext
    ): Result<HazardDetectionResult> {
        // Use TensorFlow Lite models for basic detection
        // Lower accuracy but maintains functionality offline
        return runOnDeviceInference(frame)
    }
}
```

---

## 6. OSHA Database Schema & Seed Data

### Database Schema

```sql
-- /shared/src/commonMain/sqldelight/com/hazardhawk/database/osha.sq

CREATE TABLE osha_regulations (
    code TEXT PRIMARY KEY,                    -- "1926.501"
    title TEXT NOT NULL,                     -- "Fall Protection"
    section TEXT NOT NULL,                   -- "1926.501(a)(1)"
    description TEXT NOT NULL,
    category TEXT NOT NULL,                  -- "fall_protection", "ppe", "electrical"
    severity INTEGER NOT NULL DEFAULT 1,    -- 1=Info, 2=Warning, 3=Critical
    effective_date TEXT NOT NULL,           -- "2023-01-01"
    last_updated TEXT NOT NULL,
    tags TEXT NOT NULL DEFAULT '',          -- JSON array of searchable tags
    penalty_min INTEGER,                    -- Minimum penalty in USD
    penalty_max INTEGER,                    -- Maximum penalty in USD
    industry_specific TEXT DEFAULT '',      -- Construction, maritime, etc.
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now')),
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now'))
);

CREATE TABLE osha_corrective_actions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    regulation_code TEXT NOT NULL,
    action_text TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 1,    -- 1=Immediate, 2=24hrs, 3=30days
    estimated_cost TEXT,                     -- "low", "medium", "high"
    FOREIGN KEY (regulation_code) REFERENCES osha_regulations(code)
);

CREATE TABLE hazard_mappings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    hazard_type TEXT NOT NULL,              -- "fall_protection", "ppe_violation"
    regulation_code TEXT NOT NULL,
    confidence_threshold REAL NOT NULL DEFAULT 0.8,
    auto_trigger INTEGER NOT NULL DEFAULT 1, -- Boolean for automatic detection
    FOREIGN KEY (regulation_code) REFERENCES osha_regulations(code)
);

-- Indices for performance
CREATE INDEX idx_osha_category ON osha_regulations(category);
CREATE INDEX idx_osha_severity ON osha_regulations(severity);
CREATE INDEX idx_osha_tags ON osha_regulations(tags);
CREATE INDEX idx_hazard_type ON hazard_mappings(hazard_type);
```

### Seed Data

```sql
-- /shared/src/commonMain/sqldelight/com/hazardhawk/database/osha_seed.sq

-- Fall Protection Regulations
INSERT INTO osha_regulations (code, title, section, description, category, severity, effective_date, last_updated, tags, penalty_min, penalty_max, industry_specific) VALUES
('1926.501', 'Fall Protection', '1926.501(a)(1)', 'Employers must provide fall protection for employees working at heights of 6 feet or more above lower levels', 'fall_protection', 3, '2023-01-01', '2024-01-01', '["fall", "height", "protection", "safety", "harness"]', 7000, 70000, 'construction'),
('1926.502', 'Fall Protection Systems', '1926.502(a)', 'Requirements for guardrail systems, safety net systems, and personal fall arrest systems', 'fall_protection', 3, '2023-01-01', '2024-01-01', '["guardrail", "safety_net", "harness", "fall_arrest"]', 7000, 70000, 'construction'),
('1926.503', 'Training Requirements', '1926.503(a)', 'Training requirements for fall protection systems', 'fall_protection', 2, '2023-01-01', '2024-01-01', '["training", "education", "fall_protection"]', 3000, 30000, 'construction');

-- PPE Regulations
INSERT INTO osha_regulations (code, title, section, description, category, severity, effective_date, last_updated, tags, penalty_min, penalty_max, industry_specific) VALUES
('1926.95', 'Personal Protective Equipment', '1926.95(a)', 'Requirements for head protection in construction', 'ppe', 2, '2023-01-01', '2024-01-01', '["hard_hat", "head_protection", "ppe"]', 1500, 15000, 'construction'),
('1926.96', 'Occupational Foot Protection', '1926.96(a)', 'Requirements for protective footwear', 'ppe', 2, '2023-01-01', '2024-01-01', '["safety_boots", "foot_protection", "ppe"]', 1500, 15000, 'construction'),
('1926.102', 'Eye and Face Protection', '1926.102(a)', 'Requirements for eye and face protection', 'ppe', 2, '2023-01-01', '2024-01-01', '["safety_glasses", "face_shield", "eye_protection"]', 1500, 15000, 'construction');

-- Corrective Actions
INSERT INTO osha_corrective_actions (regulation_code, action_text, priority, estimated_cost) VALUES
('1926.501', 'Install guardrail system with top rail at 42 inches ± 3 inches', 1, 'medium'),
('1926.501', 'Provide personal fall arrest system with full-body harness', 1, 'low'),
('1926.501', 'Install safety net system below work area', 2, 'high'),
('1926.95', 'Provide Class E hard hats for all workers', 1, 'low'),
('1926.95', 'Ensure hard hats are worn at all times in designated areas', 1, 'low'),
('1926.96', 'Provide steel-toed safety boots meeting ASTM F2413 standards', 2, 'low');

-- Hazard Mappings
INSERT INTO hazard_mappings (hazard_type, regulation_code, confidence_threshold, auto_trigger) VALUES
('fall_protection', '1926.501', 0.85, 1),
('fall_protection', '1926.502', 0.90, 1),
('ppe_violation', '1926.95', 0.80, 1),
('ppe_violation', '1926.96', 0.75, 1),
('ppe_violation', '1926.102', 0.80, 1);
```

### Migration Strategy

```kotlin
// Database versioning for OSHA updates
object OSHADatabaseMigrations {
    val MIGRATION_1_2 = Migration(1, 2) { database ->
        database.execSQL("""
            ALTER TABLE osha_regulations 
            ADD COLUMN industry_specific TEXT DEFAULT ''
        """)
    }
    
    val MIGRATION_2_3 = Migration(2, 3) { database ->
        database.execSQL("""
            CREATE TABLE osha_updates_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                regulation_code TEXT NOT NULL,
                change_type TEXT NOT NULL,
                old_value TEXT,
                new_value TEXT,
                updated_by TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
        """)
    }
}
```

---

## 7. AR & Camera Contracts

### ARCore Session Configuration

```kotlin
// androidApp/src/main/java/com/hazardhawk/ar/ARSessionConfig.kt
data class ARSessionConfig(
    val environmentalMode: EnvironmentalMode = EnvironmentalMode.OUTDOOR_OPTIMIZED,
    val planeFindingMode: PlaneFindingMode = PlaneFindingMode.HORIZONTAL_AND_VERTICAL,
    val lightEstimationMode: LightEstimationMode = LightEstimationMode.ENVIRONMENTAL_HDR,
    val depthMode: DepthMode = DepthMode.AUTOMATIC,
    val instantPlacementMode: InstantPlacementMode = InstantPlacementMode.LOCAL_Y_UP,
    val focusMode: FocusMode = FocusMode.AUTO,
    val cameraSharing: Boolean = true
)

enum class EnvironmentalMode {
    INDOOR,
    OUTDOOR_OPTIMIZED,  // Better for construction sites
    ADAPTIVE
}

// AR Session wrapper for construction environments
class ConstructionARSession(
    private val context: Context,
    private val config: ARSessionConfig
) {
    private lateinit var arSession: Session
    private lateinit var sharedCamera: SharedCamera
    
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Main) {
        try {
            arSession = Session(context, EnumSet.of(Session.Feature.SHARED_CAMERA))
            
            val arConfig = Config(arSession).apply {
                // Optimize for construction environments
                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                focusMode = Config.FocusMode.AUTO
                
                // Enable depth for better occlusion
                if (arSession.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                    depthMode = Config.DepthMode.AUTOMATIC
                }
            }
            
            arSession.configure(arConfig)
            sharedCamera = arSession.sharedCamera
            
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
```

### Coordinate System Conventions

```kotlin
// androidApp/src/main/java/com/hazardhawk/ar/coordinatesystem/ARCoordinateSystem.kt

/**
 * Coordinate System Conventions for HazardHawk AR:
 * 
 * World Space (ARCore):
 * - Origin: First tracked position when AR session starts
 * - X: Right (east) 
 * - Y: Up (vertical)
 * - Z: Forward (north)
 * - Units: Meters
 * 
 * Screen Space:
 * - Origin: Top-left corner
 * - X: Right
 * - Y: Down  
 * - Units: Pixels
 * 
 * Camera Space:
 * - Origin: Camera center
 * - X: Right
 * - Y: Up
 * - Z: Forward (into scene)
 * - Units: Meters
 */

object CoordinateTransforms {
    /**
     * Transform world space position to screen coordinates
     */
    fun worldToScreen(
        worldPos: Vector3,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        screenWidth: Int,
        screenHeight: Int
    ): PointF? {
        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        
        val worldVec = floatArrayOf(worldPos.x, worldPos.y, worldPos.z, 1.0f)
        val clipVec = FloatArray(4)
        
        Matrix.multiplyMV(clipVec, 0, mvpMatrix, 0, worldVec, 0)
        
        if (clipVec[3] == 0f) return null
        
        val ndcX = clipVec[0] / clipVec[3]
        val ndcY = clipVec[1] / clipVec[3]
        
        // Check if behind camera
        if (clipVec[3] < 0) return null
        
        val screenX = (ndcX + 1.0f) * screenWidth * 0.5f
        val screenY = (1.0f - ndcY) * screenHeight * 0.5f
        
        return PointF(screenX, screenY)
    }
    
    /**
     * Calculate distance-based scale factor for overlays
     */
    fun calculateOverlayScale(distance: Float): Float {
        return when {
            distance < 2f -> 1.0f
            distance < 5f -> 0.8f
            distance < 10f -> 0.6f
            distance < 20f -> 0.4f
            else -> 0.2f
        }.coerceIn(0.1f, 1.0f)
    }
}
```

### Tracking State Management

```kotlin
// androidApp/src/main/java/com/hazardhawk/ar/TrackingStateManager.kt
class TrackingStateManager {
    private val _trackingState = MutableStateFlow<ARTrackingState>(ARTrackingState.Initializing)
    val trackingState: StateFlow<ARTrackingState> = _trackingState.asStateFlow()
    
    private val _fallbackMode = MutableStateFlow(false)
    val fallbackMode: StateFlow<Boolean> = _fallbackMode.asStateFlow()
    
    fun updateTrackingState(arFrame: Frame) {
        val camera = arFrame.camera
        val newState = when (camera.trackingState) {
            TrackingState.TRACKING -> ARTrackingState.Tracking
            TrackingState.PAUSED -> {
                val reason = when (camera.trackingFailureReason) {
                    TrackingFailureReason.INSUFFICIENT_LIGHT -> LimitedReason.INSUFFICIENT_LIGHT
                    TrackingFailureReason.EXCESSIVE_MOTION -> LimitedReason.EXCESSIVE_MOTION
                    TrackingFailureReason.INSUFFICIENT_FEATURES -> LimitedReason.INSUFFICIENT_FEATURES
                    else -> LimitedReason.RELOCALIZING
                }
                ARTrackingState.Limited(reason)
            }
            TrackingState.STOPPED -> ARTrackingState.Stopped
        }
        
        _trackingState.value = newState
        
        // Auto-fallback to 2D overlays if tracking is poor
        val shouldFallback = when (newState) {
            is ARTrackingState.Limited -> {
                newState.reason == LimitedReason.INSUFFICIENT_FEATURES ||
                newState.reason == LimitedReason.EXCESSIVE_MOTION
            }
            ARTrackingState.Stopped -> true
            else -> false
        }
        
        if (shouldFallback != _fallbackMode.value) {
            _fallbackMode.value = shouldFallback
        }
    }
}

// Fallback to 2D overlays when AR tracking fails
class Fallback2DOverlayRenderer {
    fun renderFallbackOverlay(
        hazard: DetectedHazard,
        screenBounds: RectF
    ): OverlayRenderData {
        // Use simple 2D bounding boxes when 3D tracking unavailable
        return OverlayRenderData(
            screenCoordinates = screenBounds,
            depth = 0f,  // Ignore depth in 2D mode
            visible = true,
            renderInstructions = RenderInstructions.Simple2D(
                strokeColor = hazard.severity.color,
                strokeWidth = 3.dp,
                label = "${hazard.type} - ${hazard.oshaViolations.firstOrNull()?.code}"
            )
        )
    }
}
```

---

## 8. Overlay UX Specification

### Visual Design System

```kotlin
// androidApp/src/main/java/com/hazardhawk/ui/ar/OverlayDesignSystem.kt
object HazardOverlayDesign {
    // Severity-based visual styling
    data class SeverityStyle(
        val strokeWidth: Dp,
        val fillAlpha: Float,
        val labelStyle: TextStyle,
        val color: Color
    )
    
    val severityStyles = mapOf(
        HazardSeverity.CRITICAL to SeverityStyle(
            strokeWidth = 4.dp,
            fillAlpha = 0.2f,
            labelStyle = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            color = Color(0xFFD32F2F)
        ),
        HazardSeverity.WARNING to SeverityStyle(
            strokeWidth = 3.dp,
            fillAlpha = 0.15f,
            labelStyle = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            ),
            color = Color(0xFFF57C00)
        ),
        HazardSeverity.INFO to SeverityStyle(
            strokeWidth = 2.dp,
            fillAlpha = 0.1f,
            labelStyle = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White
            ),
            color = Color(0xFF1976D2)
        )
    )
    
    // Minimum touch target sizes for accessibility
    const val MIN_TOUCH_TARGET_DP = 44
    const val MIN_LABEL_HEIGHT_DP = 32
    
    // Animation configuration
    val overlayFadeIn = fadeIn(animationSpec = tween(300))
    val overlayFadeOut = fadeOut(animationSpec = tween(200))
    val overlaySlideIn = slideInVertically(
        animationSpec = tween(300),
        initialOffsetY = { it / 4 }
    )
}
```

### Interaction Specifications

```kotlin
// Overlay interaction behaviors
@Composable
fun HazardOverlay(
    hazard: DetectedHazard,
    screenCoords: RectF,
    onTap: (DetectedHazard) -> Unit,
    onLongPress: (DetectedHazard) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = HazardOverlayDesign.severityStyles[hazard.severity]!!
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = modifier
            .offset(screenCoords.left.dp, screenCoords.top.dp)
            .size(screenCoords.width().dp, screenCoords.height().dp)
            .background(
                color = style.color.copy(alpha = style.fillAlpha),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = style.strokeWidth,
                color = style.color,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(color = style.color)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onTap(hazard)
            }
            .pointerInput(hazard.id) {
                detectTapGestures(
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress(hazard)
                    }
                )
            }
    ) {
        // Label with OSHA code
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(
                    color = style.color,
                    shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .heightIn(min = HazardOverlayDesign.MIN_LABEL_HEIGHT_DP.dp)
        ) {
            Text(
                text = "${hazard.severity.displayName} — ${hazard.oshaViolations.firstOrNull()?.code ?: ""}",
                style = style.labelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Confidence indicator
        if (hazard.confidence < 0.9f) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            ) {
                Text(
                    text = "?",
                    color = style.color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
```

### Overlay Filtering & Management

```kotlin
// Overlay filtering system
data class OverlayFilterConfig(
    val enabledSeverities: Set<HazardSeverity> = HazardSeverity.values().toSet(),
    val enabledTypes: Set<HazardType> = emptySet(),  // Empty = all types
    val minimumConfidence: Float = 0.7f,
    val maxOverlaysVisible: Int = 10,
    val prioritizeByDistance: Boolean = true
)

class OverlayManager {
    private val _filterConfig = MutableStateFlow(OverlayFilterConfig())
    val filterConfig: StateFlow<OverlayFilterConfig> = _filterConfig.asStateFlow()
    
    fun filterOverlays(
        allHazards: List<DetectedHazard>,
        cameraPosition: Vector3
    ): List<DetectedHazard> {
        val config = _filterConfig.value
        
        return allHazards
            .filter { hazard ->
                // Filter by severity
                hazard.severity in config.enabledSeverities &&
                // Filter by type (empty = all types allowed)
                (config.enabledTypes.isEmpty() || hazard.type in config.enabledTypes) &&
                // Filter by confidence
                hazard.confidence >= config.minimumConfidence
            }
            .let { filtered ->
                if (config.prioritizeByDistance) {
                    // Sort by distance and take closest
                    filtered.sortedBy { hazard ->
                        hazard.boundingBox.center.distanceTo(cameraPosition)
                    }
                } else {
                    // Sort by severity and confidence
                    filtered.sortedWith(
                        compareByDescending<DetectedHazard> { it.severity.ordinal }
                            .thenByDescending { it.confidence }
                    )
                }
            }
            .take(config.maxOverlaysVisible)
    }
}

// Quick filter presets
object OverlayPresets {
    val CRITICAL_ONLY = OverlayFilterConfig(
        enabledSeverities = setOf(HazardSeverity.CRITICAL),
        minimumConfidence = 0.85f,
        maxOverlaysVisible = 5
    )
    
    val FALL_PROTECTION_FOCUS = OverlayFilterConfig(
        enabledTypes = setOf(HazardType.FallProtection),
        minimumConfidence = 0.75f,
        maxOverlaysVisible = 8
    )
    
    val ALL_VIOLATIONS = OverlayFilterConfig(
        minimumConfidence = 0.6f,
        maxOverlaysVisible = 15
    )
}
```

### Accessibility Features

```kotlin
// Accessibility support for AR overlays
@Composable
fun AccessibleHazardOverlay(
    hazard: DetectedHazard,
    screenCoords: RectF,
    onTap: (DetectedHazard) -> Unit
) {
    val accessibilityDescription = buildString {
        append("${hazard.severity.displayName} safety hazard detected. ")
        append("Type: ${hazard.type}. ")
        hazard.oshaViolations.firstOrNull()?.let { violation ->
            append("OSHA violation: ${violation.code} - ${violation.title}. ")
        }
        append("Confidence: ${(hazard.confidence * 100).roundToInt()}%. ")
        append("Double tap for details.")
    }
    
    Box(
        modifier = Modifier
            .semantics {
                contentDescription = accessibilityDescription
                role = Role.Button
                stateDescription = "Safety hazard overlay"
            }
            .clickable { onTap(hazard) }
            // ... other modifiers
    ) {
        // Overlay content
    }
}

// Voice announcements for critical hazards
class AccessibilityAnnouncementManager(
    private val ttsEngine: TextToSpeech
) {
    suspend fun announceCriticalHazard(hazard: DetectedHazard) {
        if (hazard.severity == HazardSeverity.CRITICAL) {
            val announcement = "Critical safety hazard detected: ${hazard.type}. " +
                    "OSHA violation ${hazard.oshaViolations.firstOrNull()?.code}. " +
                    "Take immediate action."
            
            ttsEngine.speak(
                announcement,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "hazard_${hazard.id}"
            )
        }
    }
}
```

---

## 9. Performance Budgets & Telemetry

### Performance Targets

```kotlin
// Performance monitoring configuration
data class PerformanceBudgets(
    val targetFps: Int = 30,
    val maxFrameTimeMs: Long = 33L,  // 30 FPS = 33ms per frame
    val maxHazardDetectionLatencyMs: Long = 200L,
    val maxOverlayRenderTimeMs: Long = 16L,  // 60 FPS overlay rendering
    val maxBatteryDrainPercentPerHour: Float = 10f,
    val maxMemoryUsageMB: Int = 150,
    val minTrackingAccuracy: Float = 0.9f
)

class PerformanceMonitor {
    private val frameTimes = mutableListOf<Long>()
    private val detectionLatencies = mutableListOf<Long>()
    private val batteryStartLevel = getBatteryLevel()
    private val sessionStartTime = System.currentTimeMillis()
    
    fun recordFrameTime(timeMs: Long) {
        frameTimes.add(timeMs)
        if (frameTimes.size > 100) {
            frameTimes.removeFirst()
        }
        
        // Alert if consistently over budget
        if (frameTimes.takeLast(10).all { it > PerformanceBudgets().maxFrameTimeMs }) {
            triggerPerformanceAdjustment(PerformanceIssue.FRAME_RATE)
        }
    }
    
    fun recordDetectionLatency(latencyMs: Long) {
        detectionLatencies.add(latencyMs)
        if (latencyMs > PerformanceBudgets().maxHazardDetectionLatencyMs) {
            triggerPerformanceAdjustment(PerformanceIssue.DETECTION_LATENCY)
        }
    }
    
    private fun triggerPerformanceAdjustment(issue: PerformanceIssue) {
        when (issue) {
            PerformanceIssue.FRAME_RATE -> {
                // Reduce AR quality
                adjustARQuality(ARQuality.REDUCED)
                // Reduce detection frequency
                adjustDetectionFrequency(0.5f)
            }
            PerformanceIssue.DETECTION_LATENCY -> {
                // Switch to smaller image resolution
                adjustImageResolution(ImageResolution.LOW)
            }
            PerformanceIssue.BATTERY_DRAIN -> {
                // Enable battery saver mode
                enableBatterySaverMode()
            }
        }
    }
}

enum class PerformanceIssue {
    FRAME_RATE,
    DETECTION_LATENCY,
    BATTERY_DRAIN,
    MEMORY_USAGE
}
```

### Adaptive Quality System

```kotlin
// Dynamic quality adjustment
class AdaptiveQualityManager {
    private var currentQuality = ARQuality.HIGH
    private var currentImageResolution = ImageResolution.HIGH
    private var detectionFrequencyMultiplier = 1.0f
    
    fun adjustForPerformance(metrics: PerformanceMetrics) {
        when {
            metrics.averageFrameTime > 50L -> {
                // Frame rate too low, reduce quality
                if (currentQuality > ARQuality.LOW) {
                    currentQuality = ARQuality.values()[currentQuality.ordinal - 1]
                    applyQualitySettings()
                }
            }
            
            metrics.detectionLatency > 300L -> {
                // Detection too slow, reduce image resolution
                if (currentImageResolution > ImageResolution.LOW) {
                    currentImageResolution = ImageResolution.values()[currentImageResolution.ordinal - 1]
                    applyImageResolution()
                }
            }
            
            metrics.batteryDrainRate > 15f -> {
                // Battery draining too fast, reduce detection frequency
                detectionFrequencyMultiplier = (detectionFrequencyMultiplier * 0.8f).coerceAtLeast(0.3f)
                applyDetectionFrequency()
            }
            
            // Recovery conditions - gradually increase quality if performance allows
            metrics.averageFrameTime < 25L && currentQuality < ARQuality.HIGH -> {
                currentQuality = ARQuality.values()[currentQuality.ordinal + 1]
                applyQualitySettings()
            }
        }
    }
    
    private fun applyQualitySettings() {
        when (currentQuality) {
            ARQuality.HIGH -> {
                // Full AR features, all planes, high tracking frequency
                enableFullARFeatures()
            }
            ARQuality.MEDIUM -> {
                // Reduced plane detection, medium tracking frequency
                enableReducedARFeatures()
            }
            ARQuality.LOW -> {
                // Minimal AR, basic tracking only
                enableMinimalARFeatures()
            }
        }
    }
}

enum class ARQuality { LOW, MEDIUM, HIGH }
enum class ImageResolution(val width: Int, val height: Int) {
    LOW(640, 480),
    MEDIUM(1280, 720),
    HIGH(1920, 1080)
}
```

### Telemetry & Analytics

```kotlin
// Anonymous telemetry collection
data class ARTelemetryEvent(
    val eventType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sessionId: String,
    val data: Map<String, Any>
)

class ARTelemetryCollector {
    private val sessionId = UUID.randomUUID().toString()
    
    fun recordHazardDetection(
        hazard: DetectedHazard,
        latencyMs: Long,
        trackingState: ARTrackingState
    ) {
        val event = ARTelemetryEvent(
            eventType = "ar_hazard_detected",
            sessionId = sessionId,
            data = mapOf(
                "hazard_type" to hazard.type.toString(),
                "confidence" to hazard.confidence,
                "latency_ms" to latencyMs,
                "tracking_state" to trackingState.toString(),
                "device_model" to Build.MODEL,
                "android_version" to Build.VERSION.SDK_INT,
                "osha_codes" to hazard.oshaViolations.map { it.code }
            )
        )
        
        sendTelemetry(event)
    }
    
    fun recordPerformanceMetrics(
        avgFrameTime: Float,
        memoryUsageMB: Int,
        batteryLevel: Float
    ) {
        val event = ARTelemetryEvent(
            eventType = "ar_performance_snapshot",
            sessionId = sessionId,
            data = mapOf(
                "avg_frame_time_ms" to avgFrameTime,
                "memory_usage_mb" to memoryUsageMB,
                "battery_level" to batteryLevel,
                "ar_quality" to currentARQuality.toString(),
                "detection_frequency" to currentDetectionFrequency
            )
        )
        
        sendTelemetry(event)
    }
    
    fun recordTrackingStateChange(
        oldState: ARTrackingState,
        newState: ARTrackingState,
        durationMs: Long
    ) {
        val event = ARTelemetryEvent(
            eventType = "ar_tracking_state_change",
            sessionId = sessionId,
            data = mapOf(
                "old_state" to oldState.toString(),
                "new_state" to newState.toString(),
                "duration_ms" to durationMs,
                "lighting_estimate" to getCurrentLightingEstimate(),
                "plane_count" to getCurrentPlaneCount()
            )
        )
        
        sendTelemetry(event)
    }
}

// Telemetry event schemas
object TelemetryEventSchemas {
    val HAZARD_DETECTED = mapOf(
        "event" to "ar_hazard_detected",
        "required_fields" to listOf(
            "hazard_type",     // String: fall_protection, ppe_violation, etc.
            "confidence",      // Float: 0.0-1.0
            "latency_ms",      // Long: detection time in milliseconds
            "tracking_state"   // String: tracking, limited, stopped
        ),
        "optional_fields" to listOf(
            "device_model",    // String: anonymized device model
            "osha_codes",      // Array[String]: applicable OSHA regulation codes
            "distance_m"       // Float: estimated distance to hazard
        )
    )
    
    val PERFORMANCE_SNAPSHOT = mapOf(
        "event" to "ar_performance_snapshot",
        "required_fields" to listOf(
            "avg_frame_time_ms",  // Float: average frame rendering time
            "memory_usage_mb",    // Int: current memory usage
            "battery_level"       // Float: 0.0-1.0 battery percentage
        )
    )
}
```

---

## 10. Error Handling & User Messaging

### Unified Error Model

```kotlin
// Comprehensive error handling system
sealed class ARError(
    val message: String,
    val retryable: Boolean = false,
    val fallbackAvailable: Boolean = false,
    val userAction: UserAction? = null
) : Exception(message) {
    
    // Permission-related errors
    data class CameraPermissionDenied(
        val permanently: Boolean = false
    ) : ARError(
        message = "Camera permission is required for AR features",
        retryable = !permanently,
        fallbackAvailable = false,
        userAction = if (permanently) UserAction.OPEN_SETTINGS else UserAction.REQUEST_PERMISSION
    )
    
    // AR capability errors
    data class ARNotSupported(
        val reason: ARUnsupportedReason
    ) : ARError(
        message = when (reason) {
            ARUnsupportedReason.DEVICE_NOT_SUPPORTED -> "This device doesn't support ARCore"
            ARUnsupportedReason.ARCORE_NOT_INSTALLED -> "ARCore needs to be installed"
            ARUnsupportedReason.ARCORE_OUTDATED -> "ARCore needs to be updated"
        },
        retryable = reason != ARUnsupportedReason.DEVICE_NOT_SUPPORTED,
        fallbackAvailable = true,
        userAction = when (reason) {
            ARUnsupportedReason.ARCORE_NOT_INSTALLED -> UserAction.INSTALL_ARCORE
            ARUnsupportedReason.ARCORE_OUTDATED -> UserAction.UPDATE_ARCORE
            else -> UserAction.USE_FALLBACK_MODE
        }
    )
    
    // Network/AI errors
    data class HazardDetectionFailed(
        val cause: Throwable,
        val canRetry: Boolean = true
    ) : ARError(
        message = "Hazard detection temporarily unavailable",
        retryable = canRetry,
        fallbackAvailable = true,
        userAction = if (canRetry) UserAction.RETRY else UserAction.USE_OFFLINE_MODE
    )
    
    // Tracking errors
    data class TrackingLost(
        val reason: LimitedReason,
        val duration: Long
    ) : ARError(
        message = when (reason) {
            LimitedReason.INSUFFICIENT_LIGHT -> "Need better lighting for AR tracking"
            LimitedReason.EXCESSIVE_MOTION -> "Move device more slowly"
            LimitedReason.INSUFFICIENT_FEATURES -> "Point camera at textured surfaces"
            LimitedReason.RELOCALIZING -> "AR is reestablishing tracking"
        },
        retryable = true,
        fallbackAvailable = true,
        userAction = UserAction.IMPROVE_CONDITIONS
    )
    
    // Performance errors
    data class PerformanceIssue(
        val type: PerformanceIssueType,
        val metric: Float
    ) : ARError(
        message = when (type) {
            PerformanceIssueType.LOW_FRAME_RATE -> "AR performance is reduced"
            PerformanceIssueType.HIGH_BATTERY_DRAIN -> "AR is draining battery quickly"
            PerformanceIssueType.MEMORY_PRESSURE -> "Device memory is low"
        },
        retryable = false,
        fallbackAvailable = true,
        userAction = UserAction.REDUCE_QUALITY
    )
}

enum class ARUnsupportedReason {
    DEVICE_NOT_SUPPORTED,
    ARCORE_NOT_INSTALLED,
    ARCORE_OUTDATED
}

enum class PerformanceIssueType {
    LOW_FRAME_RATE,
    HIGH_BATTERY_DRAIN,
    MEMORY_PRESSURE
}

enum class UserAction {
    REQUEST_PERMISSION,
    OPEN_SETTINGS,
    INSTALL_ARCORE,
    UPDATE_ARCORE,
    USE_FALLBACK_MODE,
    RETRY,
    USE_OFFLINE_MODE,
    IMPROVE_CONDITIONS,
    REDUCE_QUALITY
}
```

### User Messaging System

```kotlin
// User-facing error messages and recovery suggestions
object ARUserMessages {
    fun getErrorMessage(error: ARError): UserMessage {
        return when (error) {
            is ARError.CameraPermissionDenied -> UserMessage(
                title = "Camera Access Needed",
                message = "HazardHawk needs camera access to detect safety hazards in real-time.",
                action = if (error.permanently) "Open Settings" else "Grant Permission",
                severity = MessageSeverity.BLOCKING
            )
            
            is ARError.ARNotSupported -> UserMessage(
                title = when (error.reason) {
                    ARUnsupportedReason.DEVICE_NOT_SUPPORTED -> "AR Not Available"
                    ARUnsupportedReason.ARCORE_NOT_INSTALLED -> "ARCore Required"
                    ARUnsupportedReason.ARCORE_OUTDATED -> "ARCore Update Needed"
                },
                message = error.message + if (error.fallbackAvailable) "\n\nYou can still use basic camera features." else "",
                action = when (error.reason) {
                    ARUnsupportedReason.ARCORE_NOT_INSTALLED -> "Install ARCore"
                    ARUnsupportedReason.ARCORE_OUTDATED -> "Update ARCore"
                    else -> "Continue without AR"
                },
                severity = if (error.fallbackAvailable) MessageSeverity.WARNING else MessageSeverity.BLOCKING
            )
            
            is ARError.TrackingLost -> UserMessage(
                title = "AR Tracking Lost",
                message = error.message + "\n\nAR overlays may be inaccurate until tracking is restored.",
                action = "Continue",
                severity = MessageSeverity.INFO,
                autoHide = true,
                autoHideDelayMs = 5000L
            )
            
            is ARError.HazardDetectionFailed -> UserMessage(
                title = "Detection Unavailable",
                message = "AI hazard detection is temporarily offline. Camera will continue working normally.",
                action = if (error.retryable) "Retry" else "Continue",
                severity = MessageSeverity.WARNING,
                autoHide = !error.retryable,
                autoHideDelayMs = 3000L
            )
            
            is ARError.PerformanceIssue -> UserMessage(
                title = "Performance Notice",
                message = when (error.type) {
                    PerformanceIssueType.LOW_FRAME_RATE -> "AR quality has been reduced to maintain smooth operation."
                    PerformanceIssueType.HIGH_BATTERY_DRAIN -> "AR features are using significant battery. Consider reducing usage."
                    PerformanceIssueType.MEMORY_PRESSURE -> "Device memory is low. Some AR features may be limited."
                },
                action = "Understood",
                severity = MessageSeverity.INFO,
                autoHide = true,
                autoHideDelayMs = 4000L
            )
        }
    }
}

data class UserMessage(
    val title: String,
    val message: String,
    val action: String,
    val severity: MessageSeverity,
    val autoHide: Boolean = false,
    val autoHideDelayMs: Long = 0L
)

enum class MessageSeverity {
    INFO,       // Blue - informational, non-blocking
    WARNING,    // Orange - important but not blocking
    BLOCKING    // Red - prevents core functionality
}
```

### Recovery Actions Implementation

```kotlin
// User action handlers
class ARErrorRecoveryManager(
    private val context: Context,
    private val permissionManager: PermissionManager,
    private val arController: ARCameraController
) {
    suspend fun handleUserAction(action: UserAction): Result<Unit> {
        return when (action) {
            UserAction.REQUEST_PERMISSION -> {
                permissionManager.requestCameraPermission()
            }
            
            UserAction.OPEN_SETTINGS -> {
                openAppSettings()
                Result.Success(Unit)
            }
            
            UserAction.INSTALL_ARCORE -> {
                openPlayStore("com.google.ar.core")
                Result.Success(Unit)
            }
            
            UserAction.UPDATE_ARCORE -> {
                openPlayStore("com.google.ar.core")
                Result.Success(Unit)
            }
            
            UserAction.USE_FALLBACK_MODE -> {
                arController.enableFallbackMode()
                Result.Success(Unit)
            }
            
            UserAction.RETRY -> {
                arController.retryInitialization()
            }
            
            UserAction.USE_OFFLINE_MODE -> {
                arController.enableOfflineMode()
                Result.Success(Unit)
            }
            
            UserAction.IMPROVE_CONDITIONS -> {
                showImprovementGuidance()
                Result.Success(Unit)
            }
            
            UserAction.REDUCE_QUALITY -> {
                arController.enableBatterySaverMode()
                Result.Success(Unit)
            }
        }
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
    
    private fun openPlayStore(packageName: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        context.startActivity(intent)
    }
    
    private fun showImprovementGuidance() {
        // Show overlay with tips for better AR tracking
        // E.g., "Point camera at textured surfaces", "Ensure adequate lighting"
    }
}

// Silent logging vs user notifications
class ARLogManager {
    fun logError(error: ARError, context: String) {
        when (error.severity) {
            MessageSeverity.BLOCKING -> {
                // Always show to user + log
                Logger.error("AR_BLOCKING_ERROR", error, mapOf("context" to context))
                showUserMessage(error)
            }
            
            MessageSeverity.WARNING -> {
                // Show to user + log
                Logger.warn("AR_WARNING", error, mapOf("context" to context))
                showUserMessage(error)
            }
            
            MessageSeverity.INFO -> {
                // Log only, optionally show brief notification
                Logger.info("AR_INFO", error, mapOf("context" to context))
                if (shouldShowInfoMessage(error)) {
                    showBriefNotification(error)
                }
            }
        }
    }
    
    private fun shouldShowInfoMessage(error: ARError): Boolean {
        return when (error) {
            is ARError.TrackingLost -> error.duration > 3000L  // Only if tracking lost > 3s
            is ARError.PerformanceIssue -> true  // Always inform about performance changes
            else -> false
        }
    }
}
```

---

## 11. Test Plan

### Unit Testing Strategy

```kotlin
// Test structure for AR components
class HazardDetectorTest {
    @Test
    fun `analyzeFrame returns correct hazards for fall protection scenario`() = runTest {
        // Given
        val mockFrame = createMockARFrame(
            imageData = loadTestImage("fall_protection_violation.jpg"),
            pose = CameraPose(Vector3(0f, 1.5f, 0f), Quaternion.IDENTITY, 1234567890L),
            trackingState = ARTrackingState.Tracking
        )
        
        val expectedHazards = listOf(
            DetectedHazard(
                id = "hazard_1",
                type = HazardType.FallProtection,
                boundingBox = BoundingBox3D(
                    center = Vector3(2.0f, 3.0f, 5.0f),
                    size = Vector3(1.0f, 2.0f, 0.5f),
                    rotation = Quaternion.IDENTITY
                ),
                confidence = 0.92f,
                oshaViolations = listOf(
                    OSHAViolation(
                        code = "1926.501",
                        title = "Fall Protection",
                        section = "1926.501(a)(1)",
                        description = "Fall protection required at 6 feet or more",
                        correctiveActions = listOf("Install guardrail system", "Provide safety harness"),
                        penalty = PenaltyRange(7000, 70000)
                    )
                ),
                severity = HazardSeverity.CRITICAL
            )
        )
        
        // When
        val result = hazardDetector.analyzeFrame(mockFrame, DetectionContext.Construction)
        
        // Then
        assertThat(result).isInstanceOf<Result.Success<HazardDetectionResult>>()
        val detectionResult = (result as Result.Success).data
        
        assertThat(detectionResult.hazards).hasSize(1)
        assertThat(detectionResult.hazards[0].type).isEqualTo(HazardType.FallProtection)
        assertThat(detectionResult.hazards[0].confidence).isAtLeast(0.9f)
        assertThat(detectionResult.hazards[0].oshaViolations).isNotEmpty()
        assertThat(detectionResult.processingTimeMs).isLessThan(200L)
    }
    
    @Test
    fun `analyzeFrame handles network timeout gracefully`() = runTest {
        // Given
        val mockFrame = createMockARFrame()
        coEvery { geminiClient.analyzeImage(any(), any(), any()) } throws SocketTimeoutException()
        
        // When
        val result = hazardDetector.analyzeFrame(mockFrame, DetectionContext.Construction)
        
        // Then
        assertThat(result).isInstanceOf<Result.Error>()
        val error = (result as Result.Error)
        assertThat(error.retryable).isTrue()
    }
    
    @Test
    fun `coordinate transforms are mathematically correct`() {
        // Given
        val worldPos = Vector3(1.0f, 2.0f, 5.0f)
        val viewMatrix = createViewMatrix(
            eye = Vector3(0f, 0f, 0f),
            target = Vector3(0f, 0f, 1f),
            up = Vector3(0f, 1f, 0f)
        )
        val projectionMatrix = createPerspectiveMatrix(
            fovy = 60f,
            aspect = 16f/9f,
            near = 0.1f,
            far = 100f
        )
        
        // When
        val screenPos = CoordinateTransforms.worldToScreen(
            worldPos, viewMatrix, projectionMatrix, 1920, 1080
        )
        
        // Then
        assertThat(screenPos).isNotNull()
        assertThat(screenPos!!.x).isBetween(0f, 1920f)
        assertThat(screenPos.y).isBetween(0f, 1080f)
        
        // Verify back-transformation
        val backTransformed = CoordinateTransforms.screenToWorld(
            screenPos, viewMatrix, projectionMatrix, worldPos.z
        )
        assertThat(backTransformed.x).isWithin(0.01f).of(worldPos.x)
        assertThat(backTransformed.y).isWithin(0.01f).of(worldPos.y)
    }
}

// Mock data factories for testing
object ARTestDataFactory {
    fun createMockARFrame(
        imageData: ByteArray = ByteArray(0),
        pose: CameraPose = CameraPose(Vector3.ZERO, Quaternion.IDENTITY, 0L),
        trackingState: ARTrackingState = ARTrackingState.Tracking,
        lightEstimate: Float = 0.5f
    ): ARFrame = mockk {
        every { this@mockk.imageData } returns imageData
        every { this@mockk.cameraPose } returns pose
        every { this@mockk.trackingState } returns trackingState
        every { this@mockk.lightEstimate } returns lightEstimate
    }
    
    fun loadTestImage(filename: String): ByteArray {
        return javaClass.classLoader!!.getResourceAsStream("test_images/$filename").readBytes()
    }
}
```

### Integration Testing

```kotlin
// Integration tests for camera + AR coordination
@RunWith(AndroidJUnit4::class)
@LargeTest
class ARCameraIntegrationTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @get:Rule
    val permissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)
    
    @Test
    fun `AR session initializes and displays overlays correctly`() {
        // Given
        val testScenario = ARTestScenario.FallProtectionViolation
        
        composeTestRule.setContent {
            HazardHawkTheme {
                EnhancedARCameraScreen(
                    testMode = true,
                    testScenario = testScenario
                )
            }
        }
        
        // When - Wait for AR initialization
        composeTestRule.waitForIdle()
        Thread.sleep(2000) // Allow AR session to start
        
        // Then - Verify overlay appears
        composeTestRule
            .onNodeWithContentDescription("Critical safety hazard detected")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("CRITICAL — 1926.501")
            .assertIsDisplayed()
    }
    
    @Test
    fun `overlay interaction triggers detail view`() {
        // Given
        val testScenario = ARTestScenario.PPEViolation
        var detailsShown = false
        
        composeTestRule.setContent {
            EnhancedARCameraScreen(
                testMode = true,
                testScenario = testScenario,
                onHazardDetails = { detailsShown = true }
            )
        }
        
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        
        // When - Tap overlay
        composeTestRule
            .onNodeWithContentDescription("Warning safety hazard detected")
            .performClick()
        
        // Then
        assertThat(detailsShown).isTrue()
    }
    
    @Test
    fun `fallback mode activates when tracking fails`() {
        // Given
        val testScenario = ARTestScenario.TrackingLost
        
        composeTestRule.setContent {
            EnhancedARCameraScreen(
                testMode = true,
                testScenario = testScenario
            )
        }
        
        composeTestRule.waitForIdle()
        
        // When - Simulate tracking loss
        // (Test scenario will inject tracking failure)
        
        // Then - Verify fallback UI appears
        composeTestRule
            .onNodeWithText("AR tracking lost")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Using 2D overlay mode")
            .assertIsDisplayed()
    }
}

enum class ARTestScenario {
    FallProtectionViolation,
    PPEViolation,
    MultipleHazards,
    TrackingLost,
    NetworkError,
    PerformanceIssue
}
```

### Field Testing Matrix

```kotlin
// Field test specifications
object FieldTestMatrix {
    data class TestEnvironment(
        val lighting: LightingCondition,
        val surface: SurfaceType,
        val motion: MotionLevel,
        val equipment: List<ConstructionEquipment>,
        val expectedChallenges: List<TrackingChallenge>
    )
    
    val testEnvironments = listOf(
        TestEnvironment(
            lighting = LightingCondition.BRIGHT_SUN,
            surface = SurfaceType.CONCRETE,
            motion = MotionLevel.STATIONARY,
            equipment = listOf(ConstructionEquipment.SCAFFOLDING),
            expectedChallenges = listOf(TrackingChallenge.HARSH_SHADOWS)
        ),
        
        TestEnvironment(
            lighting = LightingCondition.OVERCAST,
            surface = SurfaceType.STEEL_FRAMEWORK,
            motion = MotionLevel.SLOW_WALKING,
            equipment = listOf(ConstructionEquipment.CRANE, ConstructionEquipment.FORKLIFT),
            expectedChallenges = listOf(TrackingChallenge.REFLECTIVE_SURFACES, TrackingChallenge.MOVING_OBJECTS)
        ),
        
        TestEnvironment(
            lighting = LightingCondition.LOW_LIGHT,
            surface = SurfaceType.MUD_GRAVEL,
            motion = MotionLevel.NORMAL_WALKING,
            equipment = listOf(ConstructionEquipment.EXCAVATOR),
            expectedChallenges = listOf(TrackingChallenge.INSUFFICIENT_LIGHT, TrackingChallenge.LOW_TEXTURE)
        ),
        
        TestEnvironment(
            lighting = LightingCondition.INDOOR_FLUORESCENT,
            surface = SurfaceType.DRYWALL,
            motion = MotionLevel.RAPID_MOVEMENT,
            equipment = listOf(),
            expectedChallenges = listOf(TrackingChallenge.EXCESSIVE_MOTION, TrackingChallenge.UNIFORM_TEXTURE)
        )
    )
    
    val deviceTestMatrix = listOf(
        DeviceSpec("Google Pixel 8 Pro", minApiLevel = 33, arCoreVersion = "1.41.0"),
        DeviceSpec("Google Pixel 7", minApiLevel = 33, arCoreVersion = "1.40.0"),
        DeviceSpec("Samsung Galaxy S24", minApiLevel = 34, arCoreVersion = "1.41.0"),
        DeviceSpec("Samsung Galaxy S22", minApiLevel = 31, arCoreVersion = "1.39.0"),
        DeviceSpec("OnePlus 11", minApiLevel = 33, arCoreVersion = "1.41.0")
    )
}

enum class LightingCondition {
    BRIGHT_SUN,
    OVERCAST,
    LOW_LIGHT,
    INDOOR_FLUORESCENT,
    MIXED_LIGHTING
}

enum class SurfaceType {
    CONCRETE,
    STEEL_FRAMEWORK,
    MUD_GRAVEL,
    DRYWALL,
    BRICK,
    WOOD_FRAMING
}

enum class MotionLevel {
    STATIONARY,
    SLOW_WALKING,
    NORMAL_WALKING,
    RAPID_MOVEMENT
}

enum class ConstructionEquipment {
    SCAFFOLDING,
    CRANE,
    FORKLIFT,
    EXCAVATOR,
    CONCRETE_MIXER,
    WELDING_STATION
}

enum class TrackingChallenge {
    HARSH_SHADOWS,
    REFLECTIVE_SURFACES,
    MOVING_OBJECTS,
    INSUFFICIENT_LIGHT,
    LOW_TEXTURE,
    EXCESSIVE_MOTION,
    UNIFORM_TEXTURE
}
```

### Test Media Pack

```
# Test media structure
/shared/src/commonTest/resources/
├── test_images/
│   ├── fall_protection/
│   │   ├── violation_harness_missing.jpg
│   │   ├── violation_guardrail_inadequate.jpg
│   │   ├── compliant_safety_net.jpg
│   │   └── expected_results.json
│   ├── ppe_violations/
│   │   ├── missing_hard_hat.jpg
│   │   ├── improper_footwear.jpg
│   │   ├── no_safety_vest.jpg
│   │   └── expected_results.json
│   ├── environmental/
│   │   ├── bright_sunlight.jpg
│   │   ├── low_light_conditions.jpg
│   │   ├── reflective_steel.jpg
│   │   └── expected_results.json
│   └── edge_cases/
│       ├── multiple_hazards.jpg
│       ├── false_positives.jpg
│       ├── partially_occluded.jpg
│       └── expected_results.json
└── test_videos/
    ├── tracking_scenarios/
    │   ├── stable_tracking.mp4
    │   ├── tracking_recovery.mp4
    │   ├── excessive_motion.mp4
    │   └── low_feature_environment.mp4
    └── detection_scenarios/
        ├── hazard_detection_sequence.mp4
        ├── multi_hazard_tracking.mp4
        └── overlay_persistence.mp4
```

Example expected results format:
```json
{
  "image": "violation_harness_missing.jpg",
  "expected_hazards": [
    {
      "type": "fall_protection",
      "confidence_range": [0.85, 1.0],
      "bounding_box": {
        "center": [0.3, -0.2, 5.0],
        "size": [1.5, 2.0, 0.5],
        "tolerance": 0.1
      },
      "osha_codes": ["1926.501"],
      "severity": "critical"
    }
  ],
  "max_detection_time_ms": 200,
  "lighting_conditions": "outdoor_bright",
  "tracking_requirements": "stable"
}
```

---

## 12. Content & Assets

### Icon Library

```
# Icon specifications for hazards and UI
/androidApp/src/main/res/drawable/
├── hazard_icons/
│   ├── ic_fall_protection_critical.xml      # 24dp, red #D32F2F
│   ├── ic_fall_protection_warning.xml       # 24dp, orange #F57C00
│   ├── ic_ppe_violation_critical.xml        # 24dp, red #D32F2F
│   ├── ic_ppe_violation_warning.xml         # 24dp, orange #F57C00
│   ├── ic_electrical_hazard_critical.xml    # 24dp, red #D32F2F
│   ├── ic_moving_equipment_warning.xml      # 24dp, orange #F57C00
│   ├── ic_scaffolding_issue_critical.xml    # 24dp, red #D32F2F
│   └── ic_generic_hazard_info.xml          # 24dp, blue #1976D2
├── ar_ui_icons/
│   ├── ic_ar_enabled.xml                   # 24dp, white
│   ├── ic_ar_disabled.xml                  # 24dp, gray
│   ├── ic_tracking_lost.xml                # 24dp, orange
│   ├── ic_overlay_filter.xml               # 24dp, white
│   ├── ic_fallback_mode.xml               # 24dp, yellow
│   └── ic_performance_warning.xml          # 24dp, orange
└── osha_compliance/
    ├── ic_osha_compliant.xml              # 24dp, green #4CAF50
    ├── ic_osha_violation.xml              # 24dp, red #D32F2F
    └── ic_osha_warning.xml                # 24dp, orange #F57C00
```

### Example UI Screenshots

```
# Expected overlay appearance documentation
/docs/ui_examples/
├── ar_overlays/
│   ├── fall_protection_overlay.png         # Red box with "CRITICAL — 1926.501"
│   ├── ppe_violation_overlay.png           # Orange box with "WARNING — 1926.95"
│   ├── safety_zone_overlay.png             # Yellow transparent zone
│   ├── multiple_hazards_view.png           # Multiple overlays simultaneously
│   └── overlay_interaction_states.png      # Tapped, long-pressed states
├── fallback_mode/
│   ├── 2d_overlay_mode.png                 # 2D boxes when AR tracking lost
│   ├── tracking_lost_message.png           # User messaging for tracking issues
│   └── ar_unavailable_mode.png            # Interface when AR not supported
├── error_states/
│   ├── permission_required_dialog.png      # Camera permission request
│   ├── arcore_installation_prompt.png      # ARCore install/update prompt
│   ├── performance_warning_snackbar.png    # Performance degradation notice
│   └── network_error_toast.png            # AI service unavailable message
└── settings_screens/
    ├── ar_quality_settings.png             # AR quality adjustment options
    ├── overlay_filter_settings.png         # Hazard type filtering options
    └── osha_database_update.png           # OSHA regulation update interface
```

### Localization Keys

```yaml
# /androidApp/src/main/res/values/strings_ar.xml
ar_features:
  # Core AR features
  ar_mode_enabled: "AR Mode Enabled"
  ar_mode_disabled: "AR Mode Disabled"
  tracking_stable: "AR Tracking Stable"
  tracking_limited: "AR Tracking Limited"
  tracking_lost: "AR Tracking Lost"
  fallback_mode_active: "Using 2D Overlay Mode"
  
  # Hazard types
  hazard_fall_protection: "Fall Protection"
  hazard_ppe_violation: "PPE Violation"
  hazard_electrical: "Electrical Hazard"
  hazard_moving_equipment: "Moving Equipment"
  hazard_scaffolding: "Scaffolding Issue"
  
  # Severity levels
  severity_critical: "CRITICAL"
  severity_warning: "WARNING"
  severity_info: "INFO"
  
  # OSHA regulations (commonly referenced)
  osha_1926_501: "Fall Protection - 1926.501"
  osha_1926_502: "Fall Protection Systems - 1926.502"
  osha_1926_95: "Head Protection - 1926.95"
  osha_1926_96: "Foot Protection - 1926.96"
  osha_1926_102: "Eye Protection - 1926.102"
  
  # Corrective actions
  action_install_guardrail: "Install guardrail system"
  action_provide_harness: "Provide safety harness"
  action_ensure_hard_hat: "Ensure hard hat usage"
  action_safety_footwear: "Provide safety footwear"
  action_immediate_attention: "Requires immediate attention"
  
  # Error messages
  error_camera_permission: "Camera permission is required for AR safety features"
  error_arcore_missing: "ARCore is required for augmented reality features"
  error_arcore_outdated: "Please update ARCore for the latest safety features"
  error_device_unsupported: "This device doesn't support AR safety features"
  error_tracking_insufficient_light: "Need better lighting for AR tracking"
  error_tracking_excessive_motion: "Move device more slowly for better tracking"
  error_tracking_low_features: "Point camera at textured surfaces for better tracking"
  error_detection_offline: "Hazard detection is temporarily offline"
  error_performance_reduced: "AR quality reduced for better performance"
  
  # User guidance
  guidance_improve_lighting: "Move to better lighting for AR tracking"
  guidance_reduce_motion: "Move device more slowly"
  guidance_find_features: "Point camera at textured surfaces"
  guidance_ar_initialization: "Initializing AR safety detection..."
  guidance_first_use: "AR will highlight safety hazards and OSHA violations in real-time"
  
  # Settings and preferences
  setting_ar_quality: "AR Quality"
  setting_overlay_filters: "Overlay Filters"
  setting_osha_database: "OSHA Database"
  setting_performance_mode: "Performance Mode"
  setting_battery_saver: "Battery Saver"
  
  quality_high: "High Quality"
  quality_medium: "Medium Quality"
  quality_low: "Low Quality (Battery Saver)"
  
  filter_critical_only: "Critical Violations Only"
  filter_all_hazards: "All Hazards"
  filter_fall_protection: "Fall Protection Focus"
  filter_ppe_only: "PPE Violations Only"
  
  # Notifications and toasts
  toast_ar_enabled: "AR safety detection enabled"
  toast_ar_disabled: "AR safety detection disabled"
  toast_fallback_active: "Using 2D overlay mode"
  toast_tracking_restored: "AR tracking restored"
  toast_detection_restored: "Hazard detection restored"
  
  # Accessibility descriptions
  accessibility_hazard_overlay: "%1$s safety hazard detected. Type: %2$s. OSHA violation: %3$s. Confidence: %4$d%%. Double tap for details."
  accessibility_ar_camera: "AR-enabled safety camera. Shows real-time hazard overlays."
  accessibility_tracking_status: "AR tracking status: %1$s"
  accessibility_overlay_filter: "Filter overlays by hazard type and severity"

# Spanish translations example
# /androidApp/src/main/res/values-es/strings_ar.xml
ar_features:
  hazard_fall_protection: "Protección contra Caídas"
  hazard_ppe_violation: "Violación de EPP"
  severity_critical: "CRÍTICO"
  severity_warning: "ADVERTENCIA"
  error_camera_permission: "Se requiere permiso de cámara para funciones de seguridad AR"
  # ... more translations
```

### Content Assets Organization

```
# Asset organization for AR content
/shared/src/commonMain/resources/
├── osha_content/
│   ├── regulations/
│   │   ├── 1926_501_fall_protection.json
│   │   ├── 1926_502_fall_systems.json
│   │   ├── 1926_95_head_protection.json
│   │   └── regulation_index.json
│   ├── corrective_actions/
│   │   ├── fall_protection_actions.json
│   │   ├── ppe_compliance_actions.json
│   │   └── electrical_safety_actions.json
│   └── penalties/
│       ├── violation_penalties_2024.json
│       └── penalty_calculation_rules.json
├── ai_prompts/
│   ├── construction_hazard_detection.txt
│   ├── osha_compliance_check.txt
│   └── safety_zone_identification.txt
└── example_outputs/
    ├── sample_hazard_detection_response.json
    ├── sample_osha_violation_mapping.json
    └── sample_confidence_thresholds.json
```

Example regulation content:
```json
{
  "code": "1926.501",
  "title": "Fall Protection",
  "sections": [
    {
      "section": "1926.501(a)(1)",
      "text": "This section sets forth requirements for employers to provide fall protection systems. All employees working at heights of 6 feet (1.8 m) or more above lower levels are covered.",
      "key_points": [
        "6 feet height threshold",
        "Employer responsibility",
        "All employees covered"
      ],
      "exceptions": ["Scaffolds covered under subpart L"],
      "related_sections": ["1926.502", "1926.503"]
    }
  ],
  "penalties": {
    "serious": {"min": 7000, "max": 70000},
    "willful": {"min": 70000, "max": 700000}
  },
  "corrective_actions": [
    {
      "priority": 1,
      "action": "Install guardrail system with top rail at 42 inches ± 3 inches",
      "cost_estimate": "medium",
      "timeframe": "immediate"
    }
  ],
  "detection_keywords": [
    "fall protection",
    "guardrail",
    "safety harness",
    "elevated work",
    "roof work",
    "scaffolding"
  ]
}
```

---

## 13. Security & Compliance

### Privacy Protection Framework

```kotlin
// Privacy-first data handling
class ARPrivacyManager {
    private val faceDetector = FaceDetector()
    private val textRecognizer = TextRecognizer()
    
    /**
     * Process frame for privacy protection before any network transmission
     */
    suspend fun sanitizeFrame(frame: ARFrame): SanitizedFrame {
        return withContext(Dispatchers.Default) {
            val imageData = frame.imageData
            
            // 1. Detect and blur faces
            val faces = faceDetector.detectFaces(imageData)
            val faceBlurredData = blurFaces(imageData, faces)
            
            // 2. Detect and redact text (badges, signs, licenses)
            val textBlocks = textRecognizer.recognizeText(faceBlurredData)
            val personalTextRedacted = redactPersonalText(faceBlurredData, textBlocks)
            
            // 3. Remove EXIF GPS data
            val sanitizedData = removeLocationData(personalTextRedacted)
            
            // 4. Generate privacy-safe hash for debugging
            val frameHash = generateSafeHash(sanitizedData)
            
            SanitizedFrame(
                imageData = sanitizedData,
                originalHash = frameHash,
                privacyFlags = listOf(
                    if (faces.isNotEmpty()) PrivacyFlag.FACES_BLURRED else null,
                    if (textBlocks.isNotEmpty()) PrivacyFlag.TEXT_REDACTED else null
                ).filterNotNull(),
                metadata = frame.metadata.copy(
                    gpsCoordinates = null,  // Always remove precise location
                    deviceId = hashDeviceId(frame.metadata.deviceId)
                )
            )
        }
    }
    
    private fun blurFaces(imageData: ByteArray, faces: List<Face>): ByteArray {
        // Apply Gaussian blur to detected face regions
        return FaceBlurProcessor.blurRegions(imageData, faces.map { it.boundingBox })
    }
    
    private fun redactPersonalText(imageData: ByteArray, textBlocks: List<TextBlock>): ByteArray {
        val personalPatterns = listOf(
            Regex("\\b\\d{3}-\\d{2}-\\d{4}\\b"),      // SSN pattern
            Regex("\\b[A-Z]\\d{7}\\b"),              // License plate pattern
            Regex("\\bID\\s*:?\\s*\\d+\\b"),         // Badge ID pattern
            Regex("\\b\\d{4}\\s*\\d{4}\\s*\\d{4}\\s*\\d{4}\\b")  // Credit card pattern
        )
        
        val regionsToRedact = textBlocks.filter { block ->
            personalPatterns.any { pattern -> pattern.containsMatchIn(block.text) }
        }
        
        return TextRedactionProcessor.redactRegions(imageData, regionsToRedact.map { it.boundingBox })
    }
    
    private fun hashDeviceId(deviceId: String): String {
        // Use first 8 characters of SHA-256 hash for debugging purposes
        return MessageDigest.getInstance("SHA-256")
            .digest(deviceId.toByteArray())
            .take(4)
            .joinToString("") { String.format("%02x", it) }
    }
}

data class SanitizedFrame(
    val imageData: ByteArray,
    val originalHash: String,
    val privacyFlags: List<PrivacyFlag>,
    val metadata: FrameMetadata
)

enum class PrivacyFlag {
    FACES_BLURRED,
    TEXT_REDACTED,
    LOCATION_REMOVED,
    DEVICE_ID_HASHED
}
```

### Data Retention & Storage Policy

```kotlin
// Secure data lifecycle management
class ARDataRetentionManager {
    private val config = DataRetentionConfig(
        maxFrameBufferSizeBytes = 50 * 1024 * 1024,  // 50MB max in memory
        frameBufferTtlSeconds = 30,                   // Clear frames after 30s
        analysisResultTtlHours = 24,                  // Cache results for 24h
        telemetryRetentionDays = 7,                   // Keep anonymous telemetry 7 days
        personalDataRetentionHours = 0                // Never persist personal data
    )
    
    /**
     * Frame buffers are ephemeral - never written to disk
     */
    private val frameBuffer = LRUCache<String, SanitizedFrame>(
        maxSize = config.maxFrameBufferSizeBytes,
        sizeOf = { _, frame -> frame.imageData.size }
    )
    
    /**
     * Analysis results cached temporarily for performance
     */
    private val analysisCache = ExpiringCache<String, HazardDetectionResult>(
        ttl = config.analysisResultTtlHours.hours,
        maxSize = 1000
    )
    
    fun storeFrame(frameId: String, frame: SanitizedFrame): Boolean {
        // Frames are only stored in memory, never persisted
        frameBuffer[frameId] = frame
        
        // Schedule automatic cleanup
        cleanupScheduler.schedule({
            frameBuffer.remove(frameId)
        }, config.frameBufferTtlSeconds, TimeUnit.SECONDS)
        
        return true
    }
    
    fun clearAllFrames() {
        frameBuffer.evictAll()
        Logger.info("All frame buffers cleared for privacy protection")
    }
    
    /**
     * Only non-personal metadata is persisted for analysis results
     */
    fun cacheAnalysisResult(frameHash: String, result: HazardDetectionResult) {
        val anonymizedResult = result.copy(
            // Remove any potentially identifying information
            metadata = result.metadata.copy(
                deviceInfo = result.metadata.deviceInfo.copy(
                    deviceId = "****",
                    userId = null
                )
            )
        )
        
        analysisCache[frameHash] = anonymizedResult
    }
}

// Configuration for compliance with data protection laws
data class DataRetentionConfig(
    val maxFrameBufferSizeBytes: Int,
    val frameBufferTtlSeconds: Long,
    val analysisResultTtlHours: Long,
    val telemetryRetentionDays: Int,
    val personalDataRetentionHours: Int = 0  // GDPR compliance - no personal data retention
)
```

### Permissions & Access Control

```kotlin
// Comprehensive permission management
class ARPermissionManager(private val context: Context) {
    
    suspend fun requestRequiredPermissions(): PermissionResult {
        val requiredPermissions = listOf(
            Manifest.permission.CAMERA,
            // Note: AR features don't require location permission
            // Location is only used if explicitly granted for metadata
        )
        
        val optionalPermissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,  // For enhanced metadata only
            Manifest.permission.RECORD_AUDIO           // For voice annotations (future)
        )
        
        val deniedRequired = requiredPermissions.filter { 
            !hasPermission(it) 
        }
        
        if (deniedRequired.isNotEmpty()) {
            return requestPermissions(deniedRequired)
        }
        
        return PermissionResult.AllGranted(
            optional = optionalPermissions.filter { hasPermission(it) }
        )
    }
    
    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == 
               PackageManager.PERMISSION_GRANTED
    }
    
    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> 
                "Camera access is required to detect safety hazards and OSHA violations in real-time. " +
                "Images are processed on-device and only anonymized data is sent for analysis."
                
            Manifest.permission.ACCESS_FINE_LOCATION ->
                "Location access allows HazardHawk to add site coordinates to safety documentation. " +
                "This is optional and can be disabled in settings."
                
            else -> "This permission enhances HazardHawk's safety monitoring capabilities."
        }
    }
}

sealed class PermissionResult {
    data class AllGranted(val optional: List<String>) : PermissionResult()
    data class PartiallyGranted(val granted: List<String>, val denied: List<String>) : PermissionResult()
    data class AllDenied(val permanentlyDenied: List<String>) : PermissionResult()
}
```

### Audit Logging

```kotlin
// Security audit trail for compliance
class ARSecurityAuditLogger {
    private val auditLog = SecureLogger("AR_SECURITY_AUDIT")
    
    fun logFrameProcessing(
        sessionId: String,
        frameHash: String,
        privacyFlags: List<PrivacyFlag>,
        processingTimeMs: Long
    ) {
        auditLog.info(
            event = "frame_processed",
            details = mapOf(
                "session_id" to sessionId.take(8), // Truncated session ID
                "frame_hash" to frameHash.take(8),  // Truncated frame hash
                "privacy_protections" to privacyFlags.map { it.name },
                "processing_time_ms" to processingTimeMs,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
    
    fun logDataTransmission(
        destination: String,
        dataType: String,
        dataSize: Int,
        encrypted: Boolean,
        privacyCompliant: Boolean
    ) {
        auditLog.info(
            event = "data_transmission",
            details = mapOf(
                "destination" to destination,
                "data_type" to dataType,
                "data_size_bytes" to dataSize,
                "encrypted" to encrypted,
                "privacy_compliant" to privacyCompliant,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
    
    fun logPermissionChange(
        permission: String,
        granted: Boolean,
        requestContext: String
    ) {
        auditLog.warn(
            event = "permission_change",
            details = mapOf(
                "permission" to permission,
                "granted" to granted,
                "context" to requestContext,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
    
    fun logSecurityViolation(
        violationType: SecurityViolationType,
        details: String,
        severity: SecuritySeverity
    ) {
        auditLog.error(
            event = "security_violation",
            details = mapOf(
                "violation_type" to violationType.name,
                "details" to details,
                "severity" to severity.name,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
}

enum class SecurityViolationType {
    UNAUTHORIZED_DATA_ACCESS,
    PRIVACY_PROTECTION_BYPASS,
    EXCESSIVE_DATA_RETENTION,
    UNENCRYPTED_TRANSMISSION
}

enum class SecuritySeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}
```

### Configuration Flags for Compliance

```kotlin
// Runtime configuration for different compliance requirements
object ARComplianceConfig {
    // GDPR compliance settings
    val gdprMode = BuildConfig.GDPR_COMPLIANCE_ENABLED
    
    // Local-only processing mode
    val forceLocalProcessing = BuildConfig.FORCE_LOCAL_PROCESSING || gdprMode
    
    // Data retention limits
    val maxDataRetentionDays = if (gdprMode) 0 else 7
    
    // Encryption requirements
    val requireEncryption = BuildConfig.REQUIRE_ENCRYPTION || gdprMode
    
    // User consent tracking
    val trackConsent = gdprMode
    
    fun getComplianceSettings(): ComplianceSettings {
        return ComplianceSettings(
            enablePrivacyProtection = true,  // Always enabled
            forceLocalProcessing = forceLocalProcessing,
            maxDataRetentionHours = maxDataRetentionDays * 24,
            requireUserConsent = trackConsent,
            enableAuditLogging = true,
            anonymizeTelemetry = true,
            encryptDataInTransit = requireEncryption
        )
    }
}

data class ComplianceSettings(
    val enablePrivacyProtection: Boolean,
    val forceLocalProcessing: Boolean,
    val maxDataRetentionHours: Int,
    val requireUserConsent: Boolean,
    val enableAuditLogging: Boolean,
    val anonymizeTelemetry: Boolean,
    val encryptDataInTransit: Boolean
)

// User consent management
class ARConsentManager {
    suspend fun getConsentStatus(): ConsentStatus {
        val prefs = getEncryptedPreferences()
        
        return ConsentStatus(
            dataProcessing = prefs.getBoolean("consent_data_processing", false),
            aiAnalysis = prefs.getBoolean("consent_ai_analysis", false),
            telemetry = prefs.getBoolean("consent_telemetry", false),
            consentTimestamp = prefs.getLong("consent_timestamp", 0L)
        )
    }
    
    suspend fun recordConsent(
        dataProcessing: Boolean,
        aiAnalysis: Boolean,
        telemetry: Boolean
    ) {
        val prefs = getEncryptedPreferences()
        prefs.edit {
            putBoolean("consent_data_processing", dataProcessing)
            putBoolean("consent_ai_analysis", aiAnalysis)
            putBoolean("consent_telemetry", telemetry)
            putLong("consent_timestamp", System.currentTimeMillis())
        }
        
        // Log consent change for audit trail
        ARSecurityAuditLogger().logConsentChange(
            dataProcessing, aiAnalysis, telemetry
        )
    }
}

data class ConsentStatus(
    val dataProcessing: Boolean,
    val aiAnalysis: Boolean,
    val telemetry: Boolean,
    val consentTimestamp: Long
)
```

---

## 14. Release & Rollout Strategy

### Feature Flag Implementation

```kotlin
// AR feature flag system
class ARFeatureFlags(
    private val remoteConfig: RemoteConfig,
    private val localStorage: SharedPreferences
) {
    // Primary AR feature flag
    val arModeEnabled: Boolean
        get() = remoteConfig.getBoolean("ar_mode_enabled", false) && 
                localStorage.getBoolean("ar_mode_user_enabled", true)
    
    // Staged rollout percentage
    val arRolloutPercentage: Int
        get() = remoteConfig.getLong("ar_rollout_percentage", 0L).toInt()
    
    // AI detection feature flags
    val aiHazardDetectionEnabled: Boolean
        get() = remoteConfig.getBoolean("ai_hazard_detection_enabled", true) &&
                arModeEnabled
    
    val aiConfidenceThreshold: Float
        get() = remoteConfig.getDouble("ai_confidence_threshold", 0.8).toFloat()
    
    // Performance feature flags
    val adaptiveQualityEnabled: Boolean
        get() = remoteConfig.getBoolean("adaptive_quality_enabled", true)
    
    val batteryOptimizationEnabled: Boolean
        get() = remoteConfig.getBoolean("battery_optimization_enabled", true)
    
    // Debug and testing flags
    val arDebugOverlaysEnabled: Boolean
        get() = BuildConfig.DEBUG && 
                remoteConfig.getBoolean("ar_debug_overlays", false)
    
    val mockARDataEnabled: Boolean
        get() = BuildConfig.DEBUG && 
                localStorage.getBoolean("mock_ar_data", false)
    
    /**
     * Check if user is included in AR rollout
     */
    fun isUserInARRollout(userId: String): Boolean {
        if (!arModeEnabled) return false
        
        val userHash = userId.hashCode().absoluteValue
        val userPercentile = userHash % 100
        
        return userPercentile < arRolloutPercentage
    }
    
    /**
     * Enable AR for specific user (override rollout)
     */
    fun enableARForUser(userId: String, enabled: Boolean) {
        localStorage.edit {
            putBoolean("ar_override_$userId", enabled)
        }
    }
    
    /**
     * Emergency disable switch
     */
    fun emergencyDisableAR(): Boolean {
        return remoteConfig.getBoolean("ar_emergency_disable", false)
    }
}

// Feature flag UI integration
@Composable
fun ARFeatureGate(
    featureFlags: ARFeatureFlags,
    fallback: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    if (featureFlags.arModeEnabled && !featureFlags.emergencyDisableAR()) {
        content()
    } else {
        fallback()
    }
}
```

### Staged Rollout Plan

```kotlin
// Rollout phases configuration
object ARRolloutPhases {
    data class RolloutPhase(
        val name: String,
        val percentage: Int,
        val criteria: RolloutCriteria,
        val duration: Duration,
        val successMetrics: List<SuccessMetric>
    )
    
    val phases = listOf(
        RolloutPhase(
            name = "Internal Testing",
            percentage = 0,  // Manual activation only
            criteria = RolloutCriteria(
                deviceModels = listOf("Pixel 8 Pro", "Pixel 7"),
                minAndroidVersion = 33,
                betaUsersOnly = true
            ),
            duration = 1.weeks,
            successMetrics = listOf(
                SuccessMetric("crash_rate", maxValue = 0.01),
                SuccessMetric("ar_initialization_success", minValue = 0.95),
                SuccessMetric("hazard_detection_accuracy", minValue = 0.85)
            )
        ),
        
        RolloutPhase(
            name = "Limited Beta",
            percentage = 5,
            criteria = RolloutCriteria(
                deviceModels = listOf("Pixel 8 Pro", "Pixel 7", "Galaxy S24", "Galaxy S22"),
                minAndroidVersion = 31,
                betaUsersOnly = true
            ),
            duration = 2.weeks,
            successMetrics = listOf(
                SuccessMetric("crash_rate", maxValue = 0.005),
                SuccessMetric("user_retention", minValue = 0.9),
                SuccessMetric("performance_satisfaction", minValue = 0.8)
            )
        ),
        
        RolloutPhase(
            name = "Production Pilot",
            percentage = 10,
            criteria = RolloutCriteria(
                deviceModels = emptyList(),  // All ARCore-compatible devices
                minAndroidVersion = 24,
                betaUsersOnly = false
            ),
            duration = 3.weeks,
            successMetrics = listOf(
                SuccessMetric("crash_rate", maxValue = 0.002),
                SuccessMetric("ar_feature_adoption", minValue = 0.3),
                SuccessMetric("safety_violations_detected", minValue = 100)
            )
        ),
        
        RolloutPhase(
            name = "Full Release",
            percentage = 100,
            criteria = RolloutCriteria(
                deviceModels = emptyList(),
                minAndroidVersion = 24,
                betaUsersOnly = false
            ),
            duration = Duration.INFINITE,
            successMetrics = listOf(
                SuccessMetric("crash_rate", maxValue = 0.001),
                SuccessMetric("overall_user_satisfaction", minValue = 0.85)
            )
        )
    )
}

data class RolloutCriteria(
    val deviceModels: List<String>,
    val minAndroidVersion: Int,
    val betaUsersOnly: Boolean
)

data class SuccessMetric(
    val name: String,
    val minValue: Double? = null,
    val maxValue: Double? = null
)
```

### Crash Rollback System

```kotlin
// Automated rollback for critical issues
class ARCrashMonitor {
    private val crashThreshold = 0.005  // 0.5% crash rate threshold
    private val timeWindow = 1.hours
    
    private val _crashEvents = mutableListOf<CrashEvent>()
    private val crashEvents: List<CrashEvent> = _crashEvents
    
    fun reportCrash(
        exception: Throwable,
        arFeatureActive: Boolean,
        deviceInfo: DeviceInfo
    ) {
        val crashEvent = CrashEvent(
            timestamp = System.currentTimeMillis(),
            exception = exception.javaClass.simpleName,
            stackTrace = exception.stackTrace.take(5).joinToString("\n"),
            arFeatureActive = arFeatureActive,
            deviceModel = deviceInfo.model,
            androidVersion = deviceInfo.androidVersion
        )
        
        _crashEvents.add(crashEvent)
        
        // Check if rollback is needed
        if (shouldTriggerRollback()) {
            triggerEmergencyRollback(
                reason = "Crash rate exceeded threshold: ${getCurrentCrashRate()}"
            )
        }
    }
    
    private fun shouldTriggerRollback(): Boolean {
        val recentCrashes = getRecentARCrashes()
        val totalARSessions = getTotalARSessions(timeWindow)
        
        if (totalARSessions < 100) return false  // Need minimum sample size
        
        val crashRate = recentCrashes.size.toDouble() / totalARSessions
        return crashRate > crashThreshold
    }
    
    private fun getRecentARCrashes(): List<CrashEvent> {
        val cutoff = System.currentTimeMillis() - timeWindow.inWholeMilliseconds
        return crashEvents.filter { 
            it.timestamp >= cutoff && it.arFeatureActive 
        }
    }
    
    private fun triggerEmergencyRollback(reason: String) {
        // Immediately disable AR for all users
        remoteConfig.setParameter("ar_emergency_disable", true)
        
        // Log rollback event
        Logger.critical("AR_EMERGENCY_ROLLBACK", mapOf(
            "reason" to reason,
            "crash_rate" to getCurrentCrashRate(),
            "affected_users" to getCurrentARUsers()
        ))
        
        // Notify development team
        alertingService.sendCriticalAlert(
            title = "HazardHawk AR Emergency Rollback",
            message = "AR features have been automatically disabled due to: $reason",
            severity = AlertSeverity.CRITICAL
        )
    }
    
    private fun getCurrentCrashRate(): Double {
        val recentCrashes = getRecentARCrashes()
        val totalSessions = getTotalARSessions(timeWindow)
        return if (totalSessions > 0) recentCrashes.size.toDouble() / totalSessions else 0.0
    }
}

data class CrashEvent(
    val timestamp: Long,
    val exception: String,
    val stackTrace: String,
    val arFeatureActive: Boolean,
    val deviceModel: String,
    val androidVersion: Int
)
```

### Play Store Integration

```kotlin
// Play Store listing updates for AR features
object PlayStoreMetadata {
    val arRequirementsNote = """
        NEW: Augmented Reality Safety Detection
        
        HazardHawk now includes cutting-edge AR technology to detect safety hazards 
        and OSHA violations in real-time before incidents occur.
        
        AR REQUIREMENTS:
        • Android 7.0 (API level 24) or higher
        • ARCore-supported device (automatic detection)
        • Camera permission (required for safety detection)
        
        If your device doesn't support AR, HazardHawk will continue to work with 
        all existing camera and documentation features.
        
        PRIVACY PROTECTION:
        • All image processing includes automatic face blurring
        • Personal information is automatically redacted
        • No raw video data is stored or transmitted
        • Full compliance with GDPR and privacy regulations
    """.trimIndent()
    
    val updatedPermissions = listOf(
        Permission(
            name = "Camera",
            reason = "Required for AR safety detection and photo documentation",
            required = true
        ),
        Permission(
            name = "AR Features",
            reason = "Enables real-time hazard detection overlay (ARCore)",
            required = false
        ),
        Permission(
            name = "Location",
            reason = "Optional - adds site coordinates to safety reports",
            required = false
        )
    )
    
    val featureGraphics = listOf(
        "ar_hazard_detection_hero.png",        // Main feature graphic
        "real_time_overlay_demo.gif",          // Animated demo
        "osha_compliance_preview.png",         // OSHA violation detection
        "before_after_comparison.png",         // Traditional vs AR workflow
        "device_compatibility_chart.png"      // Supported devices
    )
}

data class Permission(
    val name: String,
    val reason: String,
    val required: Boolean
)
```

### Support & Diagnostics

```kotlin
// Comprehensive diagnostics for AR support
class ARDiagnosticsCollector {
    suspend fun collectDiagnostics(): ARDiagnosticReport {
        return ARDiagnosticReport(
            deviceInfo = collectDeviceInfo(),
            arSupport = checkARSupport(),
            cameraInfo = collectCameraInfo(),
            performanceMetrics = collectPerformanceMetrics(),
            errorHistory = collectRecentErrors(),
            featureFlags = collectFeatureFlags(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private suspend fun checkARSupport(): ARSupportInfo {
        return ARSupportInfo(
            arCoreInstalled = isARCoreInstalled(),
            arCoreVersion = getARCoreVersion(),
            deviceSupported = isDeviceSupported(),
            cameraCompatible = isCameraCompatible(),
            openGLVersion = getOpenGLVersion(),
            availableFeatures = getAvailableARFeatures()
        )
    }
    
    private suspend fun collectPerformanceMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            averageFrameTime = getAverageFrameTime(),
            memoryUsage = getCurrentMemoryUsage(),
            batteryDrainRate = getBatteryDrainRate(),
            thermalState = getThermalState(),
            trackingAccuracy = getTrackingAccuracy()
        )
    }
    
    private suspend fun collectRecentErrors(): List<ErrorSummary> {
        return ErrorLogger.getRecentErrors(24.hours)
            .filter { it.component == "AR" }
            .map { error ->
                ErrorSummary(
                    type = error.type,
                    message = error.message,
                    frequency = error.count,
                    lastOccurrence = error.lastSeen
                )
            }
    }
    
    fun generateSupportString(report: ARDiagnosticReport): String {
        return buildString {
            appendLine("=== HazardHawk AR Diagnostics ===")
            appendLine("Device: ${report.deviceInfo.model} (Android ${report.deviceInfo.androidVersion})")
            appendLine("ARCore: ${report.arSupport.arCoreVersion} (Supported: ${report.arSupport.deviceSupported})")
            appendLine("Performance: ${report.performanceMetrics.averageFrameTime}ms avg frame time")
            appendLine("Memory: ${report.performanceMetrics.memoryUsage}MB")
            appendLine("Recent Errors: ${report.errorHistory.size}")
            appendLine("Report ID: ${report.timestamp}")
            appendLine("===")
        }
    }
}

data class ARDiagnosticReport(
    val deviceInfo: DeviceInfo,
    val arSupport: ARSupportInfo,
    val cameraInfo: CameraInfo,
    val performanceMetrics: PerformanceMetrics,
    val errorHistory: List<ErrorSummary>,
    val featureFlags: Map<String, Boolean>,
    val timestamp: Long
)

// Customer support integration
class ARSupportManager {
    fun createSupportTicket(
        userDescription: String,
        diagnostics: ARDiagnosticReport,
        attachScreenshot: Boolean = true
    ): SupportTicket {
        return SupportTicket(
            id = generateTicketId(),
            title = "AR Feature Issue - ${diagnostics.deviceInfo.model}",
            description = userDescription,
            diagnostics = diagnostics,
            priority = determinePriority(diagnostics),
            category = SupportCategory.AR_FEATURES,
            attachments = if (attachScreenshot) listOf("screenshot.png") else emptyList()
        )
    }
    
    private fun determinePriority(diagnostics: ARDiagnosticReport): SupportPriority {
        return when {
            !diagnostics.arSupport.deviceSupported -> SupportPriority.LOW
            diagnostics.errorHistory.any { it.type == "CRASH" } -> SupportPriority.HIGH
            diagnostics.performanceMetrics.averageFrameTime > 50 -> SupportPriority.MEDIUM
            else -> SupportPriority.MEDIUM
        }
    }
}

enum class SupportPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class SupportCategory {
    AR_FEATURES,
    CAMERA_ISSUES,
    PERFORMANCE,
    PRIVACY_CONCERNS,
    GENERAL
}
```

---

## 15. Implementation Timeline & Next Steps

### Development Phases

| Phase | Duration | Key Deliverables | Success Criteria |
|-------|----------|------------------|------------------|
| **Phase 1: Foundation** | 2 weeks | ARCore integration, basic overlay rendering, OSHA database setup | AR session initializes, simple overlays display |
| **Phase 2: AI Integration** | 2 weeks | Gemini Vision connection, hazard detection pipeline, privacy protection | <200ms detection latency, >85% accuracy |
| **Phase 3: UX Polish** | 1 week | Interaction design, error handling, performance optimization | 30fps overlay rendering, accessible design |
| **Phase 4: Testing** | 1 week | Field testing, device compatibility, integration testing | Pass all test scenarios, device certification |
| **Phase 5: Release** | 1 week | Feature flags, staged rollout, monitoring setup | Successful 5% rollout, <0.5% crash rate |

### Critical Dependencies

1. **ARCore Compatibility**: Ensure target devices support ARCore
2. **Gemini Vision API**: Confirm API access and rate limits
3. **Performance Baseline**: Establish current app performance metrics
4. **Privacy Compliance**: Legal review of data handling practices
5. **OSHA Database**: Validate regulation accuracy and completeness

### Risk Mitigation

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|-------------------|
| AR tracking instability | Medium | High | Implement 2D fallback mode |
| AI detection accuracy | Low | High | Extensive field testing, confidence thresholds |
| Performance degradation | Medium | Medium | Adaptive quality system, battery optimization |
| Device compatibility | Low | Medium | Comprehensive device testing matrix |
| Privacy compliance | Low | High | Built-in privacy protection, legal review |

### Success Metrics

**Technical Performance:**
- 30fps AR overlay rendering
- <200ms hazard detection latency  
- >90% AR tracking accuracy
- <10% battery drain increase

**User Experience:**
- >80% feature adoption rate
- <5% user reports of AR issues
- >4.0 app store rating maintenance
- Zero privacy complaints

**Business Impact:**
- 25% increase in hazard detection efficiency
- 15% reduction in safety incident documentation time
- Positive feedback from construction industry users
- Competitive differentiation in safety app market

---

This comprehensive handoff specification provides everything needed to implement AR features in HazardHawk successfully. The plan balances technical innovation with practical construction industry needs, ensuring a robust, privacy-compliant, and user-friendly augmented reality safety detection system.