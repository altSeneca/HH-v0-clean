# HazardHawk AR Implementation Plan

## Overview
Transform HazardHawk's camera interface to display real-time AR overlays for hazard detection, OSHA compliance violations, and safety zones before photo capture.

## Phase 1: Foundation Setup (Week 1-2)

### Dependencies & Configuration
- Add ARCore dependency to `androidApp/build.gradle.kts`
- Configure AR permissions in AndroidManifest.xml
- Set up ARCore session lifecycle management

### Core Architecture
- Create `ARHazardDetectionService` in shared module
- Implement `AROverlayRenderer` for Jetpack Compose
- Establish `OSHAComplianceDatabase` with regulation mappings

## Phase 2: AR Camera Integration (Week 2-3)

### Camera Service Upgrade
- Modify `SafetyHUDCameraScreen.kt` to support ARCore session
- Implement shared camera access between CameraX and ARCore
- Create `ARCameraController` extending existing camera functionality

### AR Session Management
- Initialize ARCore session with construction site tracking
- Configure environmental understanding for plane detection
- Set up motion tracking for stable overlay positioning

## Phase 3: Hazard Detection System (Week 3-4)

### AI Integration
- Connect existing Gemini Vision AI to AR pipeline
- Implement real-time frame analysis for hazard detection
- Create confidence scoring system for overlay reliability

### Overlay Rendering
- Develop `HazardOverlayComponent` for visual violations
- Implement OSHA regulation lookup and display
- Create safety zone boundary visualization

## Phase 4: UI/UX Enhancement (Week 4-5)

### Interactive Overlays
- Add tap-to-focus on hazard overlays
- Implement overlay persistence during camera movement
- Create overlay filtering by hazard type/severity

### Performance Optimization
- Optimize AR frame processing for construction environments
- Implement adaptive quality based on device capabilities
- Add battery usage monitoring for AR features

## Key Components

### 1. ARHazardOverlay System
```kotlin
// Real-time overlay rendering with:
- Fall protection violation boxes (red)
- PPE compliance zones (yellow) 
- OSHA regulation references
- Distance-based scaling
```

### 2. OSHA Integration
```kotlin
// Database structure:
- Regulation codes (1926.95, etc.)
- Violation descriptions
- Severity levels
- Required corrective actions
```

### 3. Construction-Specific Tracking
```kotlin
// Optimized for:
- Outdoor lighting conditions
- Reflective surfaces (steel, glass)
- Moving equipment detection
- Multi-level construction sites
```

## Technical Implementation

### AR Session Setup
- Use ARCore with `TEMPLATE_RECORD` for camera sharing
- Configure environmental understanding for construction sites
- Implement SLAM for stable tracking in industrial environments

### Overlay Rendering Pipeline
1. Frame capture from ARCore session
2. AI analysis via Gemini Vision API
3. Hazard detection and classification
4. OSHA compliance checking
5. 3D overlay positioning and rendering
6. UI overlay composition in Jetpack Compose

### Integration Points
- Extend existing `UnifiedCameraOverlay` component
- Leverage current metadata system for AR context
- Maintain compatibility with existing photo capture workflow

## Success Metrics
- Real-time overlay rendering at 30fps
- <200ms latency for hazard detection
- Accurate tracking in 90% of construction environments
- Zero interference with existing camera functionality

This plan transforms HazardHawk into a proactive safety monitoring system, displaying live compliance information before incidents occur.

## Detailed Implementation Steps

### Phase 1: Foundation Setup

#### 1.1 Gradle Dependencies
```kotlin
// androidApp/build.gradle.kts
implementation("com.google.ar:core:1.41.0")
implementation("com.google.ar.sceneform:core:1.17.1")
```

#### 1.2 Permissions Setup
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera.ar" android:required="true" />
<meta-data android:name="com.google.ar.core" android:value="required" />
```

#### 1.3 Core AR Architecture
- `shared/src/commonMain/kotlin/com/hazardhawk/ar/ARHazardDetectionService.kt`
- `shared/src/commonMain/kotlin/com/hazardhawk/ar/OSHAComplianceDatabase.kt`
- `androidApp/src/main/java/com/hazardhawk/ar/AROverlayRenderer.kt`

### Phase 2: Camera Integration

#### 2.1 Enhanced Camera Screen
Modify `SafetyHUDCameraScreen.kt`:
- Add ARCore session initialization
- Implement shared camera access
- Create AR overlay composition layer

#### 2.2 AR Session Management
```kotlin
class ARCameraController {
    private lateinit var arSession: Session
    private lateinit var sharedCamera: SharedCamera
    
    fun initializeAR(context: Context) {
        arSession = Session(context)
        sharedCamera = arSession.sharedCamera
        // Configure for construction environments
    }
}
```

### Phase 3: Hazard Detection

#### 3.1 AI Pipeline Integration
- Connect Gemini Vision to ARCore frames
- Implement hazard classification system
- Create confidence scoring for overlays

#### 3.2 Overlay Components
```kotlin
@Composable
fun HazardOverlayComponent(
    hazard: DetectedHazard,
    confidence: Float,
    oshaReference: String
) {
    // Render violation boxes and safety zones
}
```

### Phase 4: Performance & UX

#### 4.1 Optimization Strategies
- Frame rate monitoring and adaptive quality
- Battery usage optimization
- Memory management for AR sessions

#### 4.2 User Interaction
- Tap-to-inspect hazard details
- Filter overlays by severity
- Toggle AR mode on/off

## File Structure

```
HazardHawk/
├── shared/src/commonMain/kotlin/com/hazardhawk/
│   ├── ar/
│   │   ├── ARHazardDetectionService.kt
│   │   ├── OSHAComplianceDatabase.kt
│   │   ├── HazardClassifier.kt
│   │   └── ARTrackingManager.kt
│   └── models/
│       ├── DetectedHazard.kt
│       └── OSHAViolation.kt
└── androidApp/src/main/java/com/hazardhawk/
    ├── ar/
    │   ├── ARCameraController.kt
    │   ├── AROverlayRenderer.kt
    │   └── HazardOverlayComponent.kt
    └── ui/camera/
        └── EnhancedARCameraScreen.kt
```

## Testing Strategy

### Unit Tests
- AR tracking accuracy
- Hazard detection confidence
- OSHA regulation matching

### Integration Tests
- Camera + AR session coordination
- Overlay rendering performance
- AI pipeline reliability

### Field Tests
- Construction site validation
- Various lighting conditions
- Device compatibility testing

## Risk Mitigation

### Technical Risks
- **AR tracking instability**: Implement fallback to 2D overlays
- **Performance degradation**: Adaptive quality system
- **Camera compatibility**: Extensive device testing

### User Experience Risks
- **Overlay occlusion**: Smart positioning algorithms
- **Information overload**: Configurable overlay density
- **Battery drain**: Efficient AR session management

## Success Criteria

### Technical Performance
- [ ] 30fps AR overlay rendering
- [ ] <200ms hazard detection latency
- [ ] 90% tracking accuracy in construction environments
- [ ] <10% battery drain increase

### User Experience
- [ ] Intuitive overlay interaction
- [ ] Accurate OSHA compliance reporting
- [ ] Zero interference with photo capture
- [ ] Smooth AR mode transitions

### Business Impact
- [ ] Proactive hazard identification
- [ ] Reduced safety incidents
- [ ] Enhanced OSHA compliance
- [ ] Improved documentation quality

---

*This plan provides a roadmap for transforming HazardHawk into an industry-leading AR-powered construction safety platform.*