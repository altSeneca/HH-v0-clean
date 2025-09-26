package com.hazardhawk.di

import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import androidx.camera.core.ImageCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.work.WorkManager
import com.hazardhawk.data.PhotoStorageManager
import com.hazardhawk.data.PhotoStorageManagerCompat
import com.hazardhawk.ai.AIServiceFacade
import com.hazardhawk.ai.GeminiVisionAnalyzer
import com.hazardhawk.ai.SimpleAIPhotoAnalyzer
import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.core.OSHAPhotoAnalyzer
import com.hazardhawk.ai.impl.SimpleOSHAAnalyzer
import com.hazardhawk.ai.impl.LiveOSHAAnalyzer
import com.hazardhawk.security.SecureStorageService
import com.hazardhawk.security.PhotoEncryptionService
import com.hazardhawk.security.AndroidSecureStorageService
import com.hazardhawk.security.AndroidPhotoEncryptionService
import com.hazardhawk.data.repositories.CameraSettingsRepository
import com.hazardhawk.data.repositories.CameraSettingsRepositoryImpl
import com.hazardhawk.data.repositories.UISettingsRepository
import com.hazardhawk.data.repositories.UISettingsRepositoryImpl
import com.hazardhawk.data.repositories.SecureStorage
import com.hazardhawk.data.repositories.OSHARegulationRepository
import com.hazardhawk.data.repositories.OSHARegulationRepositoryImpl
import com.hazardhawk.data.repositories.OSHAAnalysisRepository
import com.hazardhawk.data.repositories.OSHAAnalysisRepositoryImpl
import com.hazardhawk.data.storage.OSHAAnalysisStorage
import com.hazardhawk.data.storage.OSHAAnalysisStorageImpl
import com.hazardhawk.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import com.hazardhawk.data.storage.AndroidSecureStorage
import com.hazardhawk.camera.CameraStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import coil3.ImageLoader
import coil3.memory.MemoryCache
import coil3.disk.DiskCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.hazardhawk.performance.ConstructionImageLoader
import com.hazardhawk.performance.ConstructionPerformanceMonitor
import com.hazardhawk.performance.PhotoViewerPerformanceTracker
import com.hazardhawk.performance.ConstructionPhotoMemoryManager
import com.hazardhawk.camera.MetadataEmbedder

/**
 * Android-specific dependency injection module.
 * Provides platform-specific implementations for Android dependencies.
 */
val androidModule = module {
    
    // Android Context (provided by Koin Android)
    // single<Context> { androidContext() } // This is automatically provided
    
    // SharedPreferences for app settings
    single<SharedPreferences> {
        androidContext().getSharedPreferences("hazard_hawk_prefs", Context.MODE_PRIVATE)
    }
    
    // Location Manager for GPS functionality
    single<LocationManager> {
        androidContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    
    // Work Manager for background tasks
    single<WorkManager> {
        WorkManager.getInstance(androidContext())
    }
    
    // Photo Storage Manager - provide the legacy object singleton
    single { PhotoStorageManager }
    
    // Security services
    single<SecureStorageService> { AndroidSecureStorageService(androidContext()) }
    single<PhotoEncryptionService> { AndroidPhotoEncryptionService() }

    // SecureKeyManager - provide singleton instance for compatibility
    single<com.hazardhawk.security.SecureKeyManager> {
        com.hazardhawk.security.SecureKeyManager.getInstance(androidContext())
    }
    
    // Settings storage and repositories
    single<SecureStorage> { AndroidSecureStorage(androidContext()) }
    single<CameraSettingsRepository> { CameraSettingsRepositoryImpl(get()) }
    single<UISettingsRepository> { UISettingsRepositoryImpl(get()) }
    
    // Application scope for camera state management
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    
    // Camera state manager
    single<CameraStateManager> { CameraStateManager(get(), get()) }

    // Performance monitoring components
    single<ConstructionPerformanceMonitor> { ConstructionPerformanceMonitor() }
    single<PhotoViewerPerformanceTracker> { PhotoViewerPerformanceTracker(get()) }
    single<ConstructionPhotoMemoryManager> { ConstructionPhotoMemoryManager() }

    // Metadata and camera components
    single<MetadataEmbedder> { MetadataEmbedder(androidContext()) }

    // Optimized image loading for construction photography
    single<ConstructionImageLoader> { ConstructionImageLoader(androidContext()) }
    single<ImageLoader> { get<ConstructionImageLoader>().imageLoader }
    
    // AI Service Facade - using GeminiVisionAnalyzer
    single<AIServiceFacade> {
        GeminiVisionAnalyzer(
            secureStorage = get(),
            encryptionService = get()
        )
    }

    // AI Photo Analyzer - Simple implementation for testing
    single<SimpleAIPhotoAnalyzer> {
        SimpleAIPhotoAnalyzer()
    }

    // Main AI Photo Analyzer interface - using real Gemini Vision via AIServiceFacade
    single<AIPhotoAnalyzer> {
        get<SimpleAIPhotoAnalyzer>()
    }

    // Gemini Vision Analyzer for PhotoViewer integration
    single<GeminiVisionAnalyzer> {
        GeminiVisionAnalyzer(
            secureStorage = get(),
            encryptionService = get()
        )
    }

    // OSHA Analysis Storage for persistence
    single<OSHAAnalysisStorage> {
        OSHAAnalysisStorageImpl(get())
    }

    // OSHA Analysis Repository for managing analysis data
    single<OSHAAnalysisRepository> {
        OSHAAnalysisRepositoryImpl(get())
    }

    // Live OSHA Analyzer with persistence and AI integration
    single<LiveOSHAAnalyzer> {
        LiveOSHAAnalyzer(
            geminiAnalyzer = get(),
            oshaAnalysisRepository = get()
        )
    }

    // OSHA Photo Analyzer for detailed compliance analysis - using Live implementation
    single<OSHAPhotoAnalyzer> {
        get<LiveOSHAAnalyzer>()
    }

    // Simple OSHA Analyzer for fallback/testing
    single<SimpleOSHAAnalyzer> {
        SimpleOSHAAnalyzer()
    }

    // OSHA Report Integration Service for including analysis in reports
    single<com.hazardhawk.reports.OSHAReportIntegrationService> {
        com.hazardhawk.reports.OSHAReportIntegrationServiceImpl(get())
    }

    // Mock OSHA Regulation Repository for demo purposes (avoiding Ktor dependencies)
    single<OSHARegulationRepository> {
        MockOSHARegulationRepository()
    }
    
    // CameraX dependencies
    single<ProcessCameraProvider> {
        ProcessCameraProvider.getInstance(androidContext()).get()
    }
    
    factory<ImageCapture> {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }
    
    // HTTP Client Engine for Android - commented out until Ktor dependencies are configured
    // single<HttpClientEngine> {
    //     Android.create()
    // }
    
    // Database Room instance (when we migrate from SQLDelight)
    // single<HazardHawkDatabase> {
    //     Room.databaseBuilder(
    //         androidContext(),
    //         HazardHawkDatabase::class.java,
    //         "hazard_hawk_database"
    //     ).build()
    // }
    
    // SQLDelight Android Driver
    single<app.cash.sqldelight.db.SqlDriver> {
        app.cash.sqldelight.driver.android.AndroidSqliteDriver(
            com.hazardhawk.database.HazardHawkDatabase.Schema,
            androidContext(),
            "hazard_hawk.db"
        )
    }

    // HazardHawk Database instance for AnalysisRepository
    single<com.hazardhawk.database.HazardHawkDatabase> {
        com.hazardhawk.database.createHazardHawkDatabase(androidContext())
    }
}

/**
 * Android services module for system-level integrations
 */
val androidServicesModule = module {
    
    // Permission manager
    // single<PermissionManager> {
    //     PermissionManagerImpl(androidContext())
    // }
    
    // File manager for Android-specific file operations
    // single<AndroidFileManager> {
    //     AndroidFileManager(
    //         context = androidContext(),
    //         storageManager = get()
    //     )
    // }
    
    // Notification manager for background operations
    // single<NotificationManager> {
    //     androidContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // }
    
    // Biometric authentication (if needed)
    // single<BiometricManager> {
    //     BiometricManager.from(androidContext())
    // }
}

/**
 * Mock implementation of OSHARegulationRepository for demo purposes
 */
class MockOSHARegulationRepository : OSHARegulationRepository {

    private val _syncStatus = MutableStateFlow(
        OSHAUpdateStatus(
            lastCheckDate = Clock.System.now().toEpochMilliseconds(),
            lastUpdateDate = Clock.System.now().toEpochMilliseconds(),
            apiVersion = "v1",
            totalRegulations = 25,
            updateInProgress = false,
            nextScheduledUpdate = Clock.System.now().toEpochMilliseconds() + 30 * 24 * 60 * 60 * 1000L // 30 days
        )
    )
    override val syncStatus: StateFlow<OSHAUpdateStatus> = _syncStatus.asStateFlow()

    override suspend fun syncOSHARegulations(forceUpdate: Boolean): Result<OSHAUpdateStatus> {
        return Result.success(_syncStatus.value)
    }

    override suspend fun searchRegulations(
        query: String,
        searchType: OSHASearchType
    ): Result<OSHARegulationLookup> {
        val mockRegulations = listOf(
            OSHARegulationEntity(
                id = "1926.95",
                identifier = "1926.95",
                label = "Personal Protective Equipment",
                type = OSHARegulationType.SECTION,
                level = 1,
                parentId = null,
                fullText = "Employees working in areas where there is a possible danger of head injury from impact, or from falling or flying objects, or from electrical shock and burns, shall be protected by protective helmets.",
                keywords = "ppe,helmet,protection,safety",
                lastUpdated = Clock.System.now().toEpochMilliseconds(),
                isActive = true,
                size = 1024
            ),
            OSHARegulationEntity(
                id = "1926.501",
                identifier = "1926.501",
                label = "Fall Protection",
                type = OSHARegulationType.SECTION,
                level = 1,
                parentId = null,
                fullText = "Each employee on a walking/working surface with an unprotected side or edge which is 6 feet or more above a lower level shall be protected from falling.",
                keywords = "fall,protection,height,safety",
                lastUpdated = Clock.System.now().toEpochMilliseconds(),
                isActive = true,
                size = 2048
            )
        )

        val matches = mockRegulations.map { regulation ->
            OSHARegulationMatch(
                regulation = regulation,
                relevanceScore = 0.9f,
                matchedKeywords = listOf(query),
                contextSnippet = regulation.label,
                exactMatch = query == regulation.identifier
            )
        }

        return Result.success(
            OSHARegulationLookup(
                query = query,
                matches = matches,
                totalResults = matches.size,
                searchType = searchType
            )
        )
    }

    override suspend fun getRegulationById(regulationId: String): Result<OSHARegulationEntity?> {
        return searchRegulations(regulationId, OSHASearchType.REGULATION_ID)
            .map { it.matches.firstOrNull()?.regulation }
    }

    override suspend fun getRegulationsByPart(partNumber: String): Result<List<OSHARegulationEntity>> {
        return searchRegulations(partNumber, OSHASearchType.REGULATION_ID)
            .map { it.matches.map { match -> match.regulation } }
    }

    override suspend fun getRegulationContent(regulationId: String): Result<OSHARegulationContent?> {
        val regulation = getRegulationById(regulationId).getOrNull()
        return if (regulation != null) {
            Result.success(
                OSHARegulationContent(
                    sectionId = regulation.identifier,
                    fullText = regulation.fullText,
                    requirements = emptyList(),
                    penalties = emptyList(),
                    lastModified = Clock.System.now().toString(),
                    authority = "29 CFR ${regulation.identifier}",
                    source = "Mock Data"
                )
            )
        } else {
            Result.success(null)
        }
    }

    override suspend fun updateSyncConfig(config: OSHASyncConfig): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getSyncConfig(): Result<OSHASyncConfig> {
        return Result.success(OSHASyncConfig())
    }

    override suspend fun isUpdateNeeded(): Boolean {
        return false
    }
}

/**
 * Android security module for secure storage and authentication
 */
val androidSecurityModule = module {
    
    // Encrypted SharedPreferences for sensitive data
    // single<SharedPreferences>(qualifier = named("secure")) {
    //     EncryptedSharedPreferences.create(
    //         "secure_prefs",
    //         MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
    //         androidContext(),
    //         EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    //         EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    //     )
    // }
    
    // Secure token storage
    // single<SecureTokenStorage> {
    //     SecureTokenStorageImpl(
    //         encryptedPrefs = get(qualifier = named("secure"))
    //     )
    // }
}
