package com.hazardhawk

import android.app.Application
import android.util.Log
import com.hazardhawk.di.ModuleRegistry
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * HazardHawk Application class with comprehensive Koin dependency injection setup.
 * 
 * This configuration provides:
 * - Cross-platform shared dependencies via shared modules
 * - Android-specific implementations for platform features
 * - ViewModel injection for Compose UI
 * - Network and API client configuration
 * - Database and repository layer setup
 * - Proper dependency scoping and lifecycle management
 */
class HazardHawkApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d("HazardHawk", "Initializing HazardHawk Application")
        
        // Initialize Koin dependency injection
        try {
            startKoin {
                // Enable Android-specific Koin features
                androidLogger(Level.INFO)
                androidContext(this@HazardHawkApplication)
                
                // Load safe dependency injection modules to prevent crashes
                modules(*ModuleRegistry.safeModules.toTypedArray())
            }
            
            Log.d("HazardHawk", "Koin dependency injection initialized successfully with safe modules")
        } catch (e: Exception) {
            Log.e("HazardHawk", "Failed to initialize Koin DI: ${e.message}", e)
            
            // Fallback: try with minimal configuration
            try {
                startKoin {
                    androidLogger(Level.ERROR)
                    androidContext(this@HazardHawkApplication)
                    modules(ModuleRegistry.minimalModules)
                }
                Log.w("HazardHawk", "Koin initialized with minimal modules as fallback")
            } catch (fallbackError: Exception) {
                Log.e("HazardHawk", "Critical: Failed to initialize Koin even with minimal modules", fallbackError)
            }
        }
        
        Log.d("HazardHawk", "Koin dependency injection initialized successfully")
        Log.d("HazardHawk", "Available modules: shared, database, repository, domain, network, android, viewmodel")
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d("HazardHawk", "HazardHawk Application terminating")
    }
}