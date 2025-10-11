package com.hazardhawk.di

import android.content.Context
import com.hazardhawk.ai.litert.LiteRTModelEngine
import com.hazardhawk.ai.litert.LiteRTDeviceOptimizer
import com.hazardhawk.performance.DeviceTierDetector
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Android-specific AI module providing Context injection and platform-specific implementations.
 * This module extends the common AI module with Android-specific dependencies.
 */
val androidAIModule = module {
    
    // Android Context provider - gets from Koin Android extension
    // Must be initialized with startKoin { androidContext(this@Application) }
    
    // Android-specific DeviceTierDetector (actual implementation)
    single<DeviceTierDetector> {
        DeviceTierDetector(get<Context>()) // Provide Android context
    }
    
    // LiteRT components with Android Context if needed
    factory<LiteRTModelEngine> {
        LiteRTModelEngine().apply {
            // Initialize with Android context for asset loading if needed
        }
    }
    
    factory<LiteRTDeviceOptimizer> {
        LiteRTDeviceOptimizer(
            deviceTierDetector = get(),
            modelEngine = get()
        )
    }
}
