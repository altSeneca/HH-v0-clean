# Dependency Injection Integration Guide

## Overview

This document outlines the complete dependency injection refactoring for LiteRT components in the HazardHawk application. The refactoring addresses Context injection issues, improves constructor patterns, and provides comprehensive test support.

## Architecture

### Module Structure

```
shared/src/commonMain/kotlin/com/hazardhawk/di/
├── SharedModule.kt          # Core DI configuration
├── AIModule.kt             # AI services and LiteRT components
├── DomainModule.kt         # Business logic use cases
├── NetworkModule.kt        # Network and API clients
├── DatabaseModule.kt       # Database and storage
└── RepositoryModule.kt     # Data repositories

shared/src/androidMain/kotlin/com/hazardhawk/di/
└── AIModule.android.kt     # Android-specific AI components

shared/src/commonTest/kotlin/com/hazardhawk/di/
├── TestModule.kt           # Test DI modules
└── ../ai/mock/MockAIComponents.kt  # Mock implementations

shared/src/androidTest/kotlin/com/hazardhawk/di/
└── AndroidTestModule.kt    # Android test modules with Context
```

### Key Components

1. **AIModule.kt** - Central AI services configuration
2. **AIModule.android.kt** - Android Context injection
3. **MockAIComponents.kt** - Test doubles
4. **AndroidTestModule.kt** - Android-specific test setup

## Refactoring Changes

### 1. Context Injection Resolution

**Before:**
```kotlin
private fun getApplicationContext(): Context = TODO("Inject context via dependency injection")
```

**After:**
```kotlin
private var applicationContext: Context? = null

fun setAndroidContext(context: Context) {
    applicationContext = context
}

private fun getApplicationContext(): Context {
    return applicationContext ?: throw IllegalStateException(
        "Android Context not injected. Ensure component is created through dependency injection."
    )
}
```

### 2. Constructor Pattern Updates

**Before:**
```kotlin
object AIServiceFactory {
    fun createOrchestrator(...): AIPhotoAnalyzer {
        val liteRTEngine = LiteRTModelEngine() // Direct instantiation
        val deviceOptimizer = LiteRTDeviceOptimizer(deviceTierDetector, liteRTEngine)
        // ...
    }
}
```

**After:**
```kotlin
class AIServiceFactory : KoinComponent {
    private val liteRTVision: LiteRTVisionService by inject()
    private val vertexAI: VertexAIGeminiService by inject()
    
    fun createOrchestrator(...): AIPhotoAnalyzer {
        return SimplifiedAIOrchestrator(
            liteRTVision = liteRTVision, // Injected dependency
            vertexAI = vertexAI,
            // ...
        )
    }
}
```

### 3. DI Module Structure

**Common AI Module:**
```kotlin
val aiModule = module {
    single<LiteRTModelEngine> { LiteRTModelEngine() }
    single<LiteRTDeviceOptimizer> {
        LiteRTDeviceOptimizer(
            deviceTierDetector = get(),
            modelEngine = get()
        )
    }
    // ... other components
}
```

**Android-Specific Module:**
```kotlin
val androidAIModule = module {
    single<Context> { androidContext() }
    
    single<LiteRTModelEngine>(override = true) {
        LiteRTModelEngine().apply {
            setAndroidContext(get<Context>())
        }
    }
}
```

## Integration Steps

### 1. Android Application Setup

```kotlin
class HazardHawkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger()
            androidContext(this@HazardHawkApplication)
            modules(hazardHawkModules + androidModules)
        }
    }
}
```

### 2. Component Usage

**In ViewModels/Components:**
```kotlin
class CameraViewModel : ViewModel(), KoinComponent {
    private val aiAnalyzer: AIPhotoAnalyzer by inject()
    
    suspend fun analyzePhoto(imageData: ByteArray) {
        val result = aiAnalyzer.analyzePhoto(imageData, WorkType.GENERAL_CONSTRUCTION)
        // Handle result...
    }
}
```

### 3. Testing Setup

**Unit Tests:**
```kotlin
class LiteRTAnalysisTest : KoinTest {
    
    @Before
    fun setUp() {
        startKoin {
            modules(testModules)
        }
    }
    
    @Test
    fun `should analyze photo successfully`() = runTest {
        val mockAnalyzer: AIPhotoAnalyzer by inject(qualifier = named("mock"))
        val result = mockAnalyzer.analyzePhoto(testImageData, WorkType.ELECTRICAL)
        
        assertTrue(result.isSuccess)
    }
}
```

**Android Instrumented Tests:**
```kotlin
@RunWith(AndroidJUnit4::class)
class LiteRTIntegrationTest : KoinTest {
    
    @Before
    fun setUp() {
        startKoin {
            androidContext(InstrumentationRegistry.getInstrumentation().targetContext)
            modules(androidTestModules)
        }
    }
    
    @Test
    fun `should initialize LiteRT with real context`() {
        val engine: LiteRTModelEngine by inject()
        assertTrue(engine.isAvailable)
    }
}
```

## Error Handling

### 1. Context Not Injected
```kotlin
fun getApplicationContext(): Context {
    return applicationContext ?: throw IllegalStateException(
        "Android Context not injected. Ensure LiteRTModelEngine is created through dependency injection."
    )
}
```

### 2. DI Module Not Included
```kotlin
// Koin will throw clear error if module is missing:
// org.koin.core.error.NoBeanDefFoundException: No definition found for class 'LiteRTModelEngine'
```

### 3. Circular Dependencies
```kotlin
// Use factory instead of single for components that might have circular deps
factory<AIPhotoAnalyzer> {
    get<AIServiceFactory>().createOrchestrator(...)
}
```

## Performance Considerations

### 1. Singleton vs Factory
- **Singleton**: LiteRTModelEngine (expensive to create)
- **Factory**: AIPhotoAnalyzer (stateless, can be recreated)

### 2. Lazy Initialization
```kotlin
val aiAnalyzer: AIPhotoAnalyzer by inject() // Lazy by default in Koin
```

### 3. Scoped Components
```kotlin
// For activity-scoped components
scope<CameraActivity> {
    scoped<CameraAIAnalyzer> { ... }
}
```

## Migration Checklist

- [x] Create AIModule.kt for common AI dependencies
- [x] Create AIModule.android.kt for Android Context injection
- [x] Update LiteRTModelEngine.android.kt with Context injection
- [x] Update LiteRTDeviceOptimizer.android.kt with Context injection
- [x] Convert AIServiceFactory from object to class with KoinComponent
- [x] Create comprehensive test modules with mocks
- [x] Create Android test modules with Context providers
- [x] Update SharedModule.kt with module lists
- [x] Document integration patterns and usage

## Production Readiness

The refactored dependency injection system provides:

1. **Proper Context Management**: Android Context is injected where needed
2. **Testability**: Comprehensive mock providers for unit tests
3. **Separation of Concerns**: Clear module boundaries
4. **Error Handling**: Clear error messages for missing dependencies
5. **Thread Safety**: Koin handles thread-safe singleton creation
6. **Performance**: Lazy initialization and proper scoping

## Next Steps

1. **Update Android Application**: Include new modules in startKoin
2. **Update Tests**: Migrate existing tests to use new test modules
3. **Add Logging**: Include performance metrics for DI resolution
4. **Consider Scoping**: Add activity/fragment-scoped components if needed
5. **Documentation**: Update README with new DI patterns

This refactoring ensures the LiteRT implementation is production-ready with proper dependency injection, comprehensive testing support, and maintainable architecture patterns.
