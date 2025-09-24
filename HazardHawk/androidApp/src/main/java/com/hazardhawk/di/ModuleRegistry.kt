package com.hazardhawk.di

/**
 * Simplified Module registry that provides easy access to DI modules.
 * This version only includes modules that are actually defined to prevent crashes.
 */
object ModuleRegistry {

    /**
     * Minimal modules for basic functionality
     * Use this during development to avoid dependency issues
     */
    val minimalModules = listOf(
        androidModule,              // Essential Android services (includes AI dependencies)
        viewModelModule            // Basic ViewModels
    )
    
    /**
     * Available shared modules (when they exist and compile properly)
     */
    val availableSharedModules = try {
        listOf<org.koin.core.module.Module>(
            // Only include modules that actually exist and compile
            // com.hazardhawk.di.aiModule,  // AI module with GeminiVisionAnalyzer - temporarily disabled, handled by androidModule instead
            // com.hazardhawk.di.sharedModule,
            // com.hazardhawk.di.databaseModule,
            // com.hazardhawk.di.repositoryModule,
            // com.hazardhawk.di.domainModule,
            // com.hazardhawk.di.networkModule
        )
    } catch (e: Exception) {
        emptyList<org.koin.core.module.Module>()
    }
    
    /**
     * Android-specific modules
     */
    val androidModules = listOf(
        androidModule,              // Android system services and components
        androidServicesModule,      // Android-specific services
        androidSecurityModule       // Secure storage and authentication
    )
    
    /**
     * UI layer modules for Android
     */
    val uiModules = listOf(
        viewModelModule,            // Compose ViewModels
        sharedViewModelModule       // Cross-screen shared ViewModels
    )
    
    /**
     * All modules for Android application
     * Uses only confirmed working modules to prevent initialization crashes
     */
    val allAndroidModules = minimalModules + listOf(
        androidServicesModule,
        androidSecurityModule,
        sharedViewModelModule
    )
    
    /**
     * Safe modules list for production - only includes modules that are guaranteed to work
     */
    val safeModules = listOf(
        androidModule,              // Includes AI dependencies
        viewModelModule
    ) + availableSharedModules
    
    /**
     * Test modules for unit testing
     */
    val testModules = listOf<org.koin.core.module.Module>(
        // These will be imported when needed for testing
    )
}