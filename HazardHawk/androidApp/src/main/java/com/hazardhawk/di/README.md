# HazardHawk Dependency Injection Setup

This directory contains the comprehensive Koin dependency injection configuration for the HazardHawk Android application.

## Architecture Overview

The DI setup follows a modular architecture with clear separation of concerns:

```
shared/src/commonMain/kotlin/com/hazardhawk/di/    # Cross-platform modules
androidApp/src/main/java/com/hazardhawk/di/        # Android-specific modules
shared/src/commonTest/kotlin/com/hazardhawk/di/    # Test modules
```

## Module Structure

### Shared Modules (Cross-platform)

1. **SharedModule.kt** - Core coroutine scopes and utilities
   - Application, IO, and Main coroutine scopes
   - Extension functions for easy scope access

2. **DatabaseModule.kt** - Database configuration
   - SQLDelight database setup
   - Database constants and configuration

3. **RepositoryModule.kt** - Data layer repositories
   - Photo, Analysis, Tag, Report, User, Project repositories
   - Local data sources and sync management

4. **DomainModule.kt** - Business logic use cases
   - Photo management, AI analysis, tag management use cases
   - Safety report and OSHA compliance use cases
   - Domain services and recommendation engines

5. **NetworkModule.kt** - HTTP client and API configuration
   - Ktor HTTP client with authentication, retry, and timeout
   - API service clients for Gemini AI and AWS S3

### Android-specific Modules

1. **AndroidModule.kt** - Android system services
   - Context, SharedPreferences, LocationManager
   - CameraX, WorkManager, PhotoStorageManager
   - HTTP client engine for Android

2. **ViewModelModule.kt** - Compose UI ViewModels
   - Screen-specific ViewModels (Gallery, Camera, Analysis)
   - Shared ViewModels for cross-screen state

3. **ModuleRegistry.kt** - Module organization and registration
   - Centralized module management
   - Easy access to all module collections

### Test Modules

1. **TestModule.kt** - Test-specific dependencies
   - Test coroutine dispatchers
   - Mock repositories and network clients
   - Fake implementations for integration testing

## Usage Examples

### Application Initialization

```kotlin
// HazardHawkApplication.kt
class HazardHawkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@HazardHawkApplication)
            modules(ModuleRegistry.allAndroidModules)
        }
    }
}
```

### Activity Injection

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    private val photoStorageManager: PhotoStorageManager by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Dependencies are automatically injected
    }
}
```

### ViewModel Injection in Compose

```kotlin
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel = koinViewModel()
) {
    // ViewModel is automatically created with injected dependencies
}
```

### Repository Injection

```kotlin
class PhotoRepositoryImpl(
    private val database: HazardHawkDatabase,
    private val s3Client: S3Client,
    private val ioScope: CoroutineScope
) : PhotoRepository
```

### Use Case Injection

```kotlin
class CapturePhotoUseCase(
    private val photoRepository: PhotoRepository,
    private val locationService: LocationService
) {
    suspend fun execute(imageData: ByteArray): Result<Photo>
}
```

## Testing Setup

### Unit Tests

```kotlin
class PhotoRepositoryTest {
    @Before
    fun setup() {
        startKoin {
            modules(testModule, mockRepositoryModule)
        }
    }
    
    private val repository: PhotoRepository by inject()
}
```

### Integration Tests

```kotlin
class IntegrationTest {
    @Before
    fun setup() {
        startKoin {
            modules(testModule, fakeImplementationsModule)
        }
    }
}
```

## Scoping Strategy

- **Single**: Long-lived instances (repositories, database, services)
- **Factory**: Short-lived instances (use cases, temporary objects)
- **ViewModel**: Compose lifecycle-scoped ViewModels
- **Scope**: Custom scopes for specific contexts

## Benefits

1. **Testability**: Easy mocking and test double injection
2. **Modularity**: Clear separation of concerns across layers
3. **Platform Consistency**: Shared modules work across Android/iOS/Desktop
4. **Memory Management**: Proper scoping prevents memory leaks
5. **Type Safety**: Compile-time dependency validation
6. **Lazy Loading**: Dependencies created only when needed

## Migration from Direct Instantiation

The previous codebase used direct instantiation:

```kotlin
// Before
val tagStorage = TagStorage()
val dialog = LoveableTagDialog(tagStorage)

// After
val tagStorage: TagStorage by inject()
```

## Future Extensions

1. **iOS Module**: Mirror Android modules for KMP iOS support
2. **Desktop Module**: Desktop-specific implementations
3. **Web Module**: Browser-based implementations
4. **Production/Debug Modules**: Environment-specific configurations
5. **Feature Modules**: Modular features with their own DI

## Troubleshooting

### Common Issues

1. **Module Not Found**: Ensure module is added to ModuleRegistry
2. **Circular Dependencies**: Check dependency graph for cycles
3. **Injection Failures**: Verify all dependencies are provided
4. **Scope Issues**: Check lifetime and scope compatibility

### Debug Tips

1. Enable Koin logging: `androidLogger(Level.DEBUG)`
2. Use `koinApplication.checkModules()` to validate configuration
3. Check logcat for "HazardHawk" DI initialization messages

## Contributing

When adding new dependencies:

1. Choose appropriate module (shared vs Android-specific)
2. Use correct scoping (single vs factory)
3. Add to appropriate module registry
4. Create test doubles in test modules
5. Update this documentation
