package com.hazardhawk.di

import android.content.Context
import com.hazardhawk.ai.tflite.TFLiteModelEngine
import com.hazardhawk.ai.services.TFLiteVisionService
import com.hazardhawk.ai.litert.AndroidDeviceAnalyzer
import com.hazardhawk.ai.litert.LiteRTDeviceOptimizer
import com.hazardhawk.ai.litert.LiteRTModelEngine
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific AI module providing Context injection and platform-specific implementations.
 * This module extends the common AI module with Android-specific dependencies.
 */
val androidAIModule = module {
    
    // Android Context provider for LiteRT components
    single<Context> {
        androidContext()
    }
    
    // Android-specific device analyzer
    single<AndroidDeviceAnalyzer> {
        AndroidDeviceAnalyzer(
            context = get()
        )
    }
    
    // Override LiteRT components to inject Android Context
    single<LiteRTModelEngine>(override = true) {
        LiteRTModelEngine().apply {
            // Initialize with Android context for asset loading
            setAndroidContext(get<Context>())
        }
    }
    
    single<LiteRTDeviceOptimizer>(override = true) {
        LiteRTDeviceOptimizer(
            deviceTierDetector = get(),
            modelEngine = get()
        ).apply {
            // Initialize with Android context for device analysis
            setAndroidContext(get<Context>())
        }
    }
}

/**
 * Android-specific test AI module with mock Context.
 */
val androidAITestModule = module {
    
    // Mock Context for testing
    single<Context> {
        mockk<Context>()
    }
    
    // Mock Android device analyzer
    single<AndroidDeviceAnalyzer> {
        mockk<AndroidDeviceAnalyzer>()
    }
}
