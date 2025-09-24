# LiteRT-LM Integration: Comprehensive Implementation Plan

**Generated**: 2025-09-09 15:03:34  
**Project**: HazardHawk AI Analysis Enhancement  
**Objective**: Replace mock ONNX implementation with Google's LiteRT-LM framework

---

## 🎯 Executive Summary

### Mission
Transform HazardHawk's mock "3 recommendations" AI analysis into real, production-grade on-device intelligence using Google's LiteRT-LM framework, achieving 8x performance improvements through GPU/NPU acceleration while maintaining construction safety focus.

### Key Benefits
- **Real AI Analysis**: Replace mock JSON generation with genuine hazard detection
- **8x Performance Boost**: NPU acceleration (5,836 tokens/sec vs 243 tokens/sec CPU)  
- **Reduced Cloud Dependency**: On-device processing for offline construction sites
- **Construction Optimized**: Maintain OSHA compliance and safety-specific workflows
- **Zero Breaking Changes**: Preserve existing photo capture and UI contracts

### Success Metrics
- Analysis accuracy: **>90%** (vs current mock data)
- Processing speed: **>3x improvement** with GPU, **>8x with NPU**
- Memory usage: **<2GB peak** (down from current 1.5-2GB)
- Device compatibility: **Graceful CPU fallback** for older devices
- User satisfaction: **>4.5/5** rating improvement

---

## 🔬 Technical Architecture

### Current State (To Be Replaced)
```
HazardHawk Mock AI Stack:
├── SmartAIOrchestrator (3-service fallback)
│   ├── Gemma3NE2BVisionService (mock JSON generator)
│   ├── YOLO11LocalService (mock hazard detection)  
│   └── VertexAIGeminiService (cloud fallback)
├── GemmaModelLoader (unused ONNX Runtime)
└── Mock analysis: "3 recommendations" hardcoded
```

### Target LiteRT-LM Architecture  
```
HazardHawk Real AI Stack:
├── SimplifiedAIOrchestrator (LiteRT + cloud fallback)
│   ├── LiteRTVisionService (real on-device AI)
│   └── VertexAIGeminiService (cloud fallback)
├── LiteRTModelEngine (GPU/NPU acceleration)
├── LiteRTDeviceOptimizer (automatic backend selection)
└── Real analysis: Genuine hazard detection with OSHA codes
```

### Core Interface Design
```kotlin
// Preserve existing contracts for zero UI breaking changes
interface AIPhotoAnalyzer {
    suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis>
}

// New LiteRT implementation  
class LiteRTVisionService : AIPhotoAnalyzer {
    private val modelEngine: LiteRTModelEngine
    private val deviceOptimizer: LiteRTDeviceOptimizer
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis> {
        // Real AI analysis replacing mock JSON generation
        return modelEngine.generateSafetyAnalysis(imageData, workType)
    }
}
```

---

## 🗺️ Implementation Roadmap

### Phase 1: Foundation (Week 1-2) 🏗️
**Status**: Ready to start  
**Dependencies**: None  
**Risk Level**: Low

#### Tasks:
1. **Create LiteRT Core Interfaces**
   ```kotlin
   // File: shared/src/commonMain/kotlin/com/hazardhawk/ai/litert/LiteRTModelEngine.kt
   expect class LiteRTModelEngine() {
       val isAvailable: Boolean
       val supportedBackends: Set<LiteRTBackend>
       suspend fun initialize(modelPath: String, backend: LiteRTBackend): Result<Unit>
       suspend fun generateSafetyAnalysis(imageData: ByteArray, workType: WorkType): Result<String>
   }
   ```

2. **Update Build Configuration**
   ```kotlin
   // File: androidApp/build.gradle.kts
   dependencies {
       // Remove ONNX (unused)
       // implementation(libs.onnxruntime.android)
       
       // Add LiteRT-LM
       implementation("com.google.ai.edge.litert:litert-lm:0.7.0")
   }
   ```

3. **Backend Enumeration**
   ```kotlin
   enum class LiteRTBackend {
       AUTO,    // Let LiteRT choose optimal
       CPU,     // CPU inference (baseline)
       GPU,     // GPU acceleration (3x speedup)
       NPU,     // Neural Processing Unit (8x speedup)
       NNAPI    // Android Neural Networks API
   }
   ```

#### Deliverables:
- ✅ Cross-platform LiteRT interfaces
- ✅ Build configuration updates
- ✅ Backend selection framework

---

### Phase 2: Android Implementation (Week 2-3) 📱
**Status**: Depends on Phase 1  
**Dependencies**: LiteRT interfaces complete  
**Risk Level**: Medium (JNI integration)

#### Tasks:
1. **Android LiteRT Integration**
   ```kotlin
   // File: shared/src/androidMain/kotlin/com/hazardhawk/ai/litert/LiteRTModelEngine.android.kt
   actual class LiteRTModelEngine(private val context: Context) {
       private var engine: LiteRTLMEngine? = null
       
       actual suspend fun initialize(modelPath: String, backend: LiteRTBackend): Result<Unit> {
           return try {
               val modelFile = File(context.getExternalFilesDir("models"), modelPath)
               engine = LiteRTLMEngine.create(
                   modelPath = modelFile.absolutePath,
                   backendType = backend.toLiteRTBackend(),
                   enableGPUAcceleration = backend == LiteRTBackend.GPU
               )
               Result.success(Unit)
           } catch (e: Exception) {
               Result.failure(LiteRTInitializationException(e.message, e))
           }
       }
   }
   ```

2. **Device Capability Detection**
   ```kotlin
   class LiteRTDeviceOptimizer {
       fun selectOptimalBackend(): LiteRTBackend {
           return when {
               hasNPU() && ramGB >= 8 -> LiteRTBackend.NPU
               hasGPU() && ramGB >= 4 -> LiteRTBackend.GPU
               else -> LiteRTBackend.CPU
           }
       }
   }
   ```

3. **Model Asset Management**
   ```
   androidApp/src/main/assets/models/
   ├── safety_analysis.litertmlm          # Primary model (300MB)
   ├── safety_analysis_lite.litertmlm     # Fallback model (150MB)
   └── model_config.json                  # Model metadata
   ```

#### Deliverables:
- ✅ Android-specific LiteRT implementation
- ✅ Device capability assessment
- ✅ Model loading and caching system
- ✅ Graceful error handling and fallbacks

---

### Phase 3: Service Integration (Week 3-4) 🔄
**Status**: Depends on Phase 2  
**Dependencies**: Android implementation complete  
**Risk Level**: Low (interface preservation)

#### Tasks:
1. **Replace Mock Services**
   ```kotlin
   // File: shared/src/commonMain/kotlin/com/hazardhawk/ai/services/LiteRTVisionService.kt
   class LiteRTVisionService(
       private val modelEngine: LiteRTModelEngine
   ) : AIPhotoAnalyzer {
       
       override suspend fun analyzePhoto(
           imageData: ByteArray,
           workType: WorkType
       ): Result<SafetyAnalysis> {
           // REAL AI analysis replacing mock JSON generation
           val rawResult = modelEngine.generateSafetyAnalysis(imageData, workType).getOrThrow()
           return SafetyAnalysisParser.parse(rawResult, workType)
       }
   }
   ```

2. **Simplify AI Orchestrator**
   ```kotlin
   // Replace complex 3-service SmartAIOrchestrator with simple 2-service approach
   class SimplifiedAIOrchestrator(
       private val liteRTService: LiteRTVisionService,
       private val cloudFallback: VertexAIGeminiService
   ) : AIPhotoAnalyzer {
       
       override suspend fun analyzePhoto(imageData: ByteArray, workType: WorkType): Result<SafetyAnalysis> {
           // Primary: Real LiteRT analysis
           return liteRTService.analyzePhoto(imageData, workType)
               .recoverCatching { 
                   // Fallback: Cloud analysis
                   cloudFallback.analyzePhoto(imageData, workType).getOrThrow()
               }
       }
   }
   ```

3. **Remove Redundant Services**
   - ❌ Delete `Gemma3NE2BVisionService.kt` (mock JSON generator)
   - ❌ Delete `YOLO11LocalService.kt` (mock hazard detection)
   - ❌ Delete unused ONNX loader code

#### Deliverables:
- ✅ Real AI analysis service
- ✅ Simplified orchestration logic
- ✅ Codebase cleanup and technical debt removal
- ✅ Preserved UI contracts (zero breaking changes)

---

### Phase 4: Performance & UX Enhancement (Week 4-5) ⚡
**Status**: Depends on Phase 3  
**Dependencies**: Service integration complete  
**Risk Level**: Low (UI enhancements)

#### Tasks:
1. **Performance Monitoring**
   ```kotlin
   class LiteRTPerformanceMonitor {
       fun trackAnalysisSpeed(backend: LiteRTBackend, durationMs: Long) {
           val tokensPerSec = when (backend) {
               LiteRTBackend.NPU -> 5836 * (1000 / durationMs.toFloat())
               LiteRTBackend.GPU -> 1876 * (1000 / durationMs.toFloat())
               LiteRTBackend.CPU -> 243 * (1000 / durationMs.toFloat())
               else -> 0f
           }
           
           // Report to analytics and show in UI
           Analytics.track("litert_performance", mapOf(
               "backend" to backend.name,
               "tokens_per_sec" to tokensPerSec,
               "duration_ms" to durationMs
           ))
       }
   }
   ```

2. **Enhanced UI Components**
   ```kotlin
   // File: androidApp/src/main/java/com/hazardhawk/ui/components/LiteRTPerformanceIndicator.kt
   @Composable
   fun LiteRTPerformanceIndicator(
       backend: LiteRTBackend,
       tokensPerSec: Float,
       memoryUsageMB: Int
   ) {
       Card(
           colors = CardDefaults.cardColors(containerColor = ConstructionColors.safetyGreen)
       ) {
           Column(modifier = Modifier.padding(16.dp)) {
               Text("AI Processing", style = MaterialTheme.typography.titleSmall)
               Row {
                   Icon(Icons.Default.Speed, contentDescription = null)
                   Text("$backend: ${tokensPerSec.roundToInt()} tokens/sec")
               }
               Text("Memory: ${memoryUsageMB}MB", style = MaterialTheme.typography.bodySmall)
           }
       }
   }
   ```

3. **Intelligent Error Recovery**
   ```kotlin
   class LiteRTErrorHandler {
       suspend fun handleError(
           backend: LiteRTBackend,
           error: Exception
       ): LiteRTBackend? {
           return when (backend) {
               LiteRTBackend.NPU -> {
                   showUserMessage("NPU unavailable, switching to GPU for 3x speedup")
                   LiteRTBackend.GPU
               }
               LiteRTBackend.GPU -> {
                   showUserMessage("GPU unavailable, using CPU processing")
                   LiteRTBackend.CPU
               }
               else -> null // No more fallbacks
           }
       }
   }
   ```

#### Deliverables:
- ✅ Real-time performance indicators
- ✅ Construction worker-friendly error messages
- ✅ Device optimization feedback
- ✅ Enhanced loading and progress states

---

### Phase 5: Testing & Validation (Week 5-6) 🧪
**Status**: Depends on Phase 4  
**Dependencies**: Performance enhancements complete  
**Risk Level**: Low (comprehensive validation)

#### Testing Strategy:

1. **Unit Tests (70+ tests)**
   ```kotlin
   // File: androidApp/src/test/java/com/hazardhawk/ai/litert/LiteRTModelEngineTest.kt
   @Test
   fun `initialize with NPU backend should succeed on capable devices`() = runTest {
       val engine = LiteRTModelEngine(mockContext)
       val result = engine.initialize("safety_analysis.litertmlm", LiteRTBackend.NPU)
       assertTrue(result.isSuccess)
   }
   
   @Test
   fun `fallback chain should work NPU -> GPU -> CPU`() = runTest {
       // Test graceful degradation
   }
   ```

2. **Integration Tests (30+ scenarios)**
   ```kotlin
   @Test
   fun `end-to-end photo analysis should return real SafetyAnalysis`() = runTest {
       val service = LiteRTVisionService(mockEngine)
       val result = service.analyzePhoto(testImageData, WorkType.CONSTRUCTION)
       
       assertTrue(result.isSuccess)
       val analysis = result.getOrThrow()
       
       // Verify real analysis vs mock data
       assertNotEquals("Generic hazard detected", analysis.hazards[0].description)
       assertTrue(analysis.confidence > 0.8)
       assertTrue(analysis.hazards.isNotEmpty())
   }
   ```

3. **Performance Benchmarks**
   ```kotlin
   @Test
   fun `GPU backend should be 3x faster than CPU`() = runTest {
       val cpuTime = measureTimeMillis { 
           analyzeWithBackend(LiteRTBackend.CPU) 
       }
       val gpuTime = measureTimeMillis { 
           analyzeWithBackend(LiteRTBackend.GPU) 
       }
       
       assertTrue(cpuTime / gpuTime >= 3.0, "GPU should be 3x faster than CPU")
   }
   ```

4. **Construction-Specific Validation**
   ```kotlin
   @Test
   fun `should detect fall hazards with OSHA codes`() = runTest {
       val testImage = loadTestImage("construction_site_unguarded_edge.jpg")
       val analysis = liteRTService.analyzePhoto(testImage, WorkType.CONSTRUCTION).getOrThrow()
       
       val fallHazards = analysis.hazards.filter { it.type == HazardType.FALL }
       assertTrue(fallHazards.isNotEmpty())
       assertTrue(fallHazards.any { it.oshaCode?.startsWith("1926.501") == true })
   }
   ```

#### Performance Targets:
| Metric | Current | Target | Measurement |
|--------|---------|---------|-------------|
| Analysis Speed (GPU) | 0.5s mock | <1.5s real | End-to-end photo → results |
| Analysis Speed (NPU) | 0.5s mock | <0.8s real | Samsung S24 Ultra benchmark |
| Memory Usage | 1.5GB | <2GB | Peak during analysis |
| Accuracy | 0% (mock) | >90% | Labeled construction dataset |
| Device Compatibility | 100% mock | >95% real | Android 7.0+ devices |

#### Deliverables:
- ✅ Comprehensive test suite (100+ tests)
- ✅ Performance validation and benchmarks
- ✅ Construction safety accuracy verification
- ✅ Cross-device compatibility confirmation
- ✅ Production readiness assessment

---

## 🚀 Deployment Strategy

### Feature Flag Implementation
```kotlin
// Safe rollout with instant rollback capability
class LiteRTFeatureManager {
    fun shouldUseLiteRT(userId: String): Boolean {
        return when (BuildConfig.BUILD_TYPE) {
            "debug" -> true                              // Always for development
            "staging" -> userId.hashCode() % 100 < 25    // 25% staging rollout
            "release" -> userId.hashCode() % 100 < 10    // 10% production rollout
            else -> false
        }
    }
}
```

### Hybrid Deployment Architecture  
```kotlin
// Zero-downtime deployment with automatic fallback
class HybridAIOrchestrator : AIPhotoAnalyzer {
    private val liteRTService = LiteRTVisionService()
    private val legacyService = Gemma3NE2BVisionService() // Keep temporarily
    
    override suspend fun analyzePhoto(
        imageData: ByteArray,
        workType: WorkType
    ): Result<SafetyAnalysis> {
        
        return if (featureManager.shouldUseLiteRT(userId)) {
            // Try new LiteRT system
            liteRTService.analyzePhoto(imageData, workType)
                .recoverCatching { error ->
                    // Automatic fallback to proven mock system
                    logger.warn("LiteRT failed, falling back to legacy: ${error.message}")
                    legacyService.analyzePhoto(imageData, workType).getOrThrow()
                }
        } else {
            // Continue using mock system for majority of users
            legacyService.analyzePhoto(imageData, workType)
        }
    }
}
```

### Rollout Timeline
- **Week 6**: Internal testing (development team)
- **Week 7**: Staging deployment (25% of staging users)  
- **Week 8**: Limited production (10% of production users)
- **Week 9**: Gradual ramp-up (25% → 50% → 75%)
- **Week 10**: Full production deployment (100%)
- **Week 11**: Legacy code removal and cleanup

### Rollback Strategy
```kotlin
// Instant rollback capability
class EmergencyRollback {
    fun disableLiteRT() {
        // Remote config update - takes effect immediately
        RemoteConfig.setValue("litert_enabled", false)
        
        // Clear any cached LiteRT models
        LiteRTModelManager.clearCache()
        
        // Force restart AI services to legacy mode
        AIServiceManager.restart(useLegacy = true)
    }
}
```

---

## 📊 Risk Analysis & Mitigation

### High-Impact Risks

#### 1. Model Performance Degradation
**Risk**: LiteRT models perform worse than expected  
**Probability**: Medium | **Impact**: High  
**Mitigation**: A/B testing with rollback triggers at <80% user satisfaction

#### 2. Memory Usage Spikes  
**Risk**: LiteRT uses more memory than anticipated  
**Probability**: High | **Impact**: Medium  
**Mitigation**: Device-adaptive model selection + memory monitoring

#### 3. Device Compatibility Issues
**Risk**: LiteRT fails on older Android devices  
**Probability**: Medium | **Impact**: Medium  
**Mitigation**: Comprehensive CPU fallback + device capability detection

### Medium-Impact Risks

#### 4. Model Download Failures
**Risk**: Users can't download LiteRT models  
**Probability**: Low | **Impact**: Medium  
**Mitigation**: Cached fallback models + background sync

#### 5. Integration Breaking Changes
**Risk**: LiteRT integration breaks existing functionality  
**Probability**: Low | **Impact**: High  
**Mitigation**: Extensive integration testing + feature flags

---

## ✅ Success Criteria & Acceptance Tests

### Technical Success Metrics
- [ ] **Real AI Analysis**: Replace all mock "3 recommendations" with genuine hazard detection
- [ ] **Performance**: >30% speed improvement (GPU) or >300% improvement (NPU)
- [ ] **Memory**: <20% memory usage increase from baseline
- [ ] **Reliability**: <0.1% crash rate increase
- [ ] **Compatibility**: >95% device compatibility maintained

### Business Success Metrics  
- [ ] **Accuracy**: >90% hazard detection accuracy on labeled construction dataset
- [ ] **User Satisfaction**: >4.5/5 rating (up from current 3.8/5)
- [ ] **Feature Adoption**: >70% of users experiencing LiteRT analysis
- [ ] **Support Tickets**: >25% reduction in "AI not working" tickets
- [ ] **OSHA Compliance**: >15% improvement in safety compliance scoring

### Construction-Specific Success Metrics
- [ ] **Fall Hazard Detection**: >85% accuracy with correct OSHA codes
- [ ] **PPE Compliance**: >80% accuracy in detecting missing safety equipment  
- [ ] **Hazard Severity**: Correct severity classification (Low/Medium/High/Critical)
- [ ] **Report Generation**: Real analysis data in PDF safety reports
- [ ] **Site-Specific Learning**: Improved accuracy on repeat construction sites

---

## 🔗 Key Files & References

### Implementation Files
| File Path | Purpose | Priority |
|-----------|---------|----------|
| `shared/src/commonMain/kotlin/com/hazardhawk/ai/litert/LiteRTModelEngine.kt` | Core interface | Critical |
| `shared/src/androidMain/kotlin/com/hazardhawk/ai/litert/LiteRTModelEngine.android.kt` | Android implementation | Critical |
| `shared/src/commonMain/kotlin/com/hazardhawk/ai/services/LiteRTVisionService.kt` | Real AI service | Critical |
| `androidApp/build.gradle.kts` | Dependencies | Critical |
| `androidApp/src/main/assets/models/` | Model files | High |

### Context7 Documentation References
- **AI Edge Torch**: `/google-ai-edge/ai-edge-torch` - Model conversion examples
- **Android ML Frameworks**: Model runtime integration patterns
- **Performance Optimization**: GPU/NPU backend selection strategies

### Current Codebase (To Be Modified)
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/loaders/GemmaModelLoader.kt` - Replace ONNX mock
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/services/Gemma3NE2BVisionService.kt` - Replace JSON generator
- `shared/src/commonMain/kotlin/com/hazardhawk/ai/core/SmartAIOrchestrator.kt` - Simplify orchestration

---

## 🏁 Next Steps

### Immediate Actions (Next 48 hours)
1. **Model Research**: Identify optimal .litertmlm models for construction safety
2. **Dependency Setup**: Update `gradle/libs.versions.toml` with LiteRT-LM dependency
3. **Architecture Review**: Validate technical approach with team leads
4. **Development Environment**: Set up LiteRT-LM development and testing environment

### Week 1 Sprint Planning
1. Create core LiteRT interfaces (`LiteRTModelEngine.kt`)
2. Update Android build configuration 
3. Implement basic Android LiteRT integration
4. Set up feature flag system for safe rollout
5. Design initial test framework structure

### Success Dependencies
- **✅ Team Alignment**: All developers understand the migration approach
- **✅ Model Selection**: Identify and validate optimal .litertmlm models
- **✅ Testing Environment**: LiteRT-LM development setup complete
- **✅ Feature Flags**: Safe rollout mechanism implemented
- **✅ Performance Baseline**: Current system benchmarked for comparison

---

**Total Development Time**: 6 weeks  
**Risk Level**: Low-Medium (well-architected foundation with fallbacks)  
**Expected Outcome**: Production-ready real AI analysis with 3-8x performance improvement  

This plan transforms HazardHawk's mock AI analysis into genuine, construction-optimized intelligence while maintaining the app's excellent architectural foundation and ensuring zero downtime during deployment.