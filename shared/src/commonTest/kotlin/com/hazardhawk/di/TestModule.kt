package com.hazardhawk.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Test module for dependency injection in unit tests.
 * Provides test doubles and mock implementations.
 */
val testModule = module {
    
    // Test coroutine scopes using TestDispatcher
    single<CoroutineScope>(qualifier = named("ApplicationScope")) {
        CoroutineScope(get<TestDispatcher>() + SupervisorJob())
    }
    
    single<CoroutineScope>(qualifier = named("IOScope")) {
        CoroutineScope(get<TestDispatcher>() + SupervisorJob())
    }
    
    single<CoroutineScope>(qualifier = named("MainScope")) {
        CoroutineScope(get<TestDispatcher>() + SupervisorJob())
    }
    
    // Test dispatcher for coroutine testing
    single<TestDispatcher> {
        UnconfinedTestDispatcher()
    }
}

/**
 * Mock repository module for testing
 */
val mockRepositoryModule = module {
    
    // Mock repositories for testing
    // single<PhotoRepository> {
    //     mockk<PhotoRepository>()
    // }
    
    // single<AnalysisRepository> {
    //     mockk<AnalysisRepository>()
    // }
    
    // single<TagRepository> {
    //     mockk<TagRepository>()
    // }
    
    // single<ReportRepository> {
    //     mockk<ReportRepository>()
    // }
    
    // single<UserRepository> {
    //     mockk<UserRepository>()
    // }
    
    // single<ProjectRepository> {
    //     mockk<ProjectRepository>()
    // }
}

/**
 * Mock AI module for testing LiteRT components with proper Context mocking.
 */
val mockAIModule = module {
    
    // Mock AI Components
    factory<MockLiteRTModelEngine>(qualifier = named("mock")) {
        MockLiteRTModelEngine()
    }
    
    factory<MockLiteRTDeviceOptimizer>(qualifier = named("mock")) {
        MockLiteRTDeviceOptimizer()
    }
    
    factory<MockLiteRTVisionService>(qualifier = named("mock")) {
        MockLiteRTVisionService()
    }
    
    factory<MockAIServiceFactory>(qualifier = named("mock")) {
        MockAIServiceFactory()
    }
    
    // Mock Performance Components
    factory<MockNetworkConnectivityService>(qualifier = named("mock")) {
        MockNetworkConnectivityService()
    }
    
    factory<MockAdaptivePerformanceManager>(qualifier = named("mock")) {
        MockAdaptivePerformanceManager()
    }
    
    factory<MockMemoryManager>(qualifier = named("mock")) {
        MockMemoryManager()
    }
    
    factory<MockPerformanceMonitor>(qualifier = named("mock")) {
        MockPerformanceMonitor()
    }
    
    factory<MockDeviceTierDetector>(qualifier = named("mock")) {
        MockDeviceTierDetector()
    }
}

/**
 * Mock network module for testing
 */
val mockNetworkModule = module {
    
    // Mock HTTP client for testing
    // single<HttpClient> {
    //     mockk<HttpClient>()
    // }
    
    // Mock API services
    // single<GeminiVisionClient> {
    //     mockk<GeminiVisionClient>()
    // }
    
    // single<S3Client> {
    //     mockk<S3Client>()
    // }
    
    // single<HazardHawkApiService> {
    //     mockk<HazardHawkApiService>()
    // }
}

/**
 * Fake implementations module for integration testing
 */
val fakeImplementationsModule = module {
    
    // Fake repositories with in-memory implementations
    // single<PhotoRepository> {
    //     FakePhotoRepository()
    // }
    
    // single<AnalysisRepository> {
    //     FakeAnalysisRepository()
    // }
    
    // single<TagRepository> {
    //     FakeTagRepository()
    // }
    
    // In-memory database for testing
    // single<SqlDriver> {
    //     JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    // }
}

/**
 * Test data factory module
 */
val testDataModule = module {
    
    // Test data factories
    // single<TestPhotoFactory> {
    //     TestPhotoFactory()
    // }
    
    // single<TestAnalysisFactory> {
    //     TestAnalysisFactory()
    // }
    
    // single<TestTagFactory> {
    //     TestTagFactory()
    // }
    
    // single<TestReportFactory> {
    //     TestReportFactory()
    // }
}
