package com.hazardhawk.di

import org.koin.dsl.module

/**
 * Core shared module for cross-platform dependencies.
 * Contains coroutine scopes and basic utilities.
 */
val sharedModule = module {
    
    // Application scope for long-running operations
    // single<CoroutineScope> {
    //     CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // }
    
    // Dispatcher providers for cross-platform coroutine usage
    // single<CoroutineDispatcherProvider> {
    //     CoroutineDispatcherProvider(
    //         main = Dispatchers.Main,
    //         io = Dispatchers.IO,
    //         default = Dispatchers.Default,
    //         unconfined = Dispatchers.Unconfined
    //     )
    // }
    
    // JSON serializer for API communication
    // single<Json> {
    //     Json {
    //         ignoreUnknownKeys = true
    //         isLenient = true
    //         encodeDefaults = false
    //         prettyPrint = false
    //         coerceInputValues = true
    //     }
    // }
    
    // Logger for cross-platform logging
    // single<Logger> {
    //     Logger.withTag("HazardHawk")
    // }
    
    // Date/time utilities
    // single<DateTimeProvider> {
    //     DateTimeProviderImpl()
    // }
    
    // UUID generator for cross-platform ID generation
    // single<UUIDGenerator> {
    //     UUIDGeneratorImpl()
    // }
}
