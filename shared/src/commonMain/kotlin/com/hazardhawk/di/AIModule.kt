package com.hazardhawk.di

import com.hazardhawk.ai.SimpleAIPhotoAnalyzer
import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.litert.LiteRTModelEngine
import com.hazardhawk.performance.*
import org.koin.dsl.module

/**
 * Simplified AI services dependency injection module.
 * Replaces complex orchestrator system with single MediaPipe-based analyzer.
 * 
 * Based on research findings: Simplified from 6 orchestrators to 1 service.
 * Uses MediaPipe Vision + TensorFlow Lite for construction safety analysis.
 */
val aiModule = module {
    
    // Core AI Components - Keep for compatibility
    single<LiteRTModelEngine> {
        LiteRTModelEngine()
    }
    
    // Performance Components - Platform-specific DeviceTierDetector is provided in platform modules
    single<PerformanceMonitor> {
        PerformanceMonitor(
            deviceDetector = get() // Provided by platform-specific module
        )
    }
    
    // Simplified AI Photo Analyzer - Replaces all orchestrators
    single<SimpleAIPhotoAnalyzer> {
        SimpleAIPhotoAnalyzer()
    }

    // Main AI Service Interface
    single<AIPhotoAnalyzer> {
        get<SimpleAIPhotoAnalyzer>()
    }
}
