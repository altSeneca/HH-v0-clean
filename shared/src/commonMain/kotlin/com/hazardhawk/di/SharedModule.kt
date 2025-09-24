package com.hazardhawk.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.scope.Scope
import org.koin.dsl.module

/**
 * Shared Koin module containing common dependencies used across all platforms.
 * This module provides core services that don't require platform-specific implementations.
 */
val sharedModule = module {
    
    // Application-wide coroutine scope for background operations
    single<CoroutineScope>(qualifier = org.koin.core.qualifier.named("ApplicationScope")) {
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    }
    
    // IO-optimized coroutine scope for database and network operations
    single<CoroutineScope>(qualifier = org.koin.core.qualifier.named("IOScope")) {
        CoroutineScope(Dispatchers.IO + SupervisorJob())
    }
    
    // Main thread coroutine scope for UI operations
    single<CoroutineScope>(qualifier = org.koin.core.qualifier.named("MainScope")) {
        CoroutineScope(Dispatchers.Main + SupervisorJob())
    }
}

/**
 * Extension function to get named coroutine scopes easily
 */
fun Scope.getApplicationScope(): CoroutineScope = get(qualifier = org.koin.core.qualifier.named("ApplicationScope"))
fun Scope.getIOScope(): CoroutineScope = get(qualifier = org.koin.core.qualifier.named("IOScope"))
fun Scope.getMainScope(): CoroutineScope = get(qualifier = org.koin.core.qualifier.named("MainScope"))

/**
 * Complete module list for HazardHawk application.
 * Include all necessary modules for proper dependency injection.
 * 
 * Usage in Android Application:
 * ```
 * startKoin {
 *     androidContext(this@Application)
 *     modules(hazardHawkModules)
 * }
 * ```
 * 
 * Usage in Tests:
 * ```
 * startKoin {
 *     modules(testModules)
 * }
 * ```
 */
val hazardHawkModules = listOf(
    sharedModule,
    aiModule,
    domainModule,
    networkModule,
    databaseModule,
    repositoryModule
)

/**
 * Test modules for unit testing.
 */
val testModules = listOf(
    testModule,
    mockAIModule,
    mockRepositoryModule,
    fakeImplementationsModule
)

/**
 * Android-specific modules (add to hazardHawkModules in Android app).
 */
val androidModules = listOf(
    androidModule,
    androidAIModule,
    androidServicesModule,
    androidSecurityModule
)

/**
 * Android test modules for instrumented testing.
 */
val androidTestModules = listOf(
    androidTestModule,
    androidInstrumentedTestModule
)
