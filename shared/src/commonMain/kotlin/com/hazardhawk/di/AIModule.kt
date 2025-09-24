package com.hazardhawk.di

import com.hazardhawk.ai.SimpleAIPhotoAnalyzer
import com.hazardhawk.ai.core.AIPhotoAnalyzer
import com.hazardhawk.ai.litert.LiteRTModelEngine
import com.hazardhawk.performance.*
import org.koin.core.qualifier.named
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
    
    // Performance Components - Keep essential monitoring
    single<PerformanceMonitor> {
        PerformanceMonitor()
    }
    
    single<DeviceTierDetector> {
        DeviceTierDetector()
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

/**
 * AI testing module with mock implementations for unit tests.
 */
val aiTestModule = module {
    
    // Mock AI services for testing
    factory<LiteRTModelEngine>(qualifier = named("mock")) {
        MockLiteRTModelEngine()
    }
    
    factory<LiteRTDeviceOptimizer>(qualifier = named("mock")) {
        MockLiteRTDeviceOptimizer()
    }
    
    factory<LiteRTVisionService>(qualifier = named("mock")) {
        MockLiteRTVisionService()
    }
    
    factory<AIPhotoAnalyzer>(qualifier = named("mock")) {
        MockAIPhotoAnalyzer()
    }
}
