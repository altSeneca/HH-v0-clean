package com.hazardhawk.di

import android.content.Context
import com.hazardhawk.ai.litert.AndroidDeviceAnalyzer
import com.hazardhawk.ai.litert.LiteRTDeviceOptimizer
import com.hazardhawk.ai.litert.LiteRTModelEngine
import io.mockk.mockk
import org.koin.dsl.module

/**
 * Android-specific test module providing mock Context and Android-specific components.
 * This module is used for Android instrumented tests where real Android Context is needed.
 */
val androidTestModule = module {
    
    // Mock Android Context for testing
    single<Context> {
        mockk<Context>(relaxed = true) {
            // Configure mock context behavior as needed
        }
    }
    
    // Mock Android Device Analyzer
    single<AndroidDeviceAnalyzer> {
        mockk<AndroidDeviceAnalyzer>(relaxed = true) {
            // Configure mock analyzer behavior
        }
    }
    
    // Override LiteRT components with mock Context injection
    single<LiteRTModelEngine>(override = true) {
        mockk<LiteRTModelEngine>(relaxed = true) {
            // Mock engine configured for testing
        }
    }
    
    single<LiteRTDeviceOptimizer>(override = true) {
        mockk<LiteRTDeviceOptimizer>(relaxed = true) {
            // Mock optimizer configured for testing
        }
    }
}

/**
 * Android unit test module with real Context from instrumentation.
 * Use this for tests that need actual Android framework components.
 */
val androidInstrumentedTestModule = module {
    
    // Real Android Context from instrumentation
    single<Context> {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
    }
    
    // Real Android Device Analyzer with test context
    single<AndroidDeviceAnalyzer> {
        AndroidDeviceAnalyzer(
            context = get()
        )
    }
    
    // LiteRT components with real context but controlled for testing
    single<LiteRTModelEngine>(override = true) {
        LiteRTModelEngine().apply {
            setAndroidContext(get<Context>())
        }
    }
    
    single<LiteRTDeviceOptimizer>(override = true) {
        LiteRTDeviceOptimizer(
            deviceTierDetector = get(),
            modelEngine = get()
        ).apply {
            setAndroidContext(get<Context>())
        }
    }
}

/**
 * Fake Android module with simplified implementations for unit tests.
 * Provides lightweight alternatives to full Android implementations.
 */
val fakeAndroidModule = module {
    
    // Fake Context with minimal implementation
    single<Context> {
        FakeContext()
    }
    
    // Fake Android Device Analyzer
    single<AndroidDeviceAnalyzer> {
        FakeAndroidDeviceAnalyzer()
    }
}

/**
 * Minimal fake Context implementation for unit tests.
 */
class FakeContext : Context {
    // Minimal implementation for testing
    // Override only the methods needed for your tests
    
    override fun getApplicationContext(): Context = this
    override fun getPackageName(): String = "com.hazardhawk.test"
    override fun getAssets(): android.content.res.AssetManager = mockk(relaxed = true)
    override fun getSystemService(name: String): Any? = mockk(relaxed = true)
    
    // Delegate other methods to mock or throw UnsupportedOperationException
    // ... (implement as needed for your specific tests)
}

/**
 * Fake Android Device Analyzer for unit tests.
 */
class FakeAndroidDeviceAnalyzer : AndroidDeviceAnalyzer {
    
    constructor() : super(FakeContext())
    
    override fun analyzeDeviceCapabilities(): AndroidDeviceCapabilities {
        return AndroidDeviceCapabilities(
            manufacturer = "TestDevice",
            model = "MockModel",
            androidVersion = 30,
            chipset = ChipsetInfo(
                vendor = ChipsetVendor.UNKNOWN,
                series = "Test",
                hasNPU = false,
                optimizedBackends = setOf(LiteRTBackend.CPU)
            ),
            totalMemoryGB = 4.0f,
            availableMemoryGB = 2.0f,
            cpuCoreCount = 4,
            hasNPU = false,
            gpuInfo = GpuInfo(
                renderer = "Test GPU",
                version = "Test Version",
                vendor = GpuVendor.UNKNOWN,
                supportsOpenCL = false,
                supportsVulkan = false
            ),
            thermalCapabilities = ThermalCapabilities(
                hasThermalAPI = false,
                maxOperatingTemp = 70f,
                throttlingTemp = 60f,
                shutdownTemp = 80f
            ),
            batteryCapacity = 100,
            supportedAbiList = listOf("arm64-v8a", "armeabi-v7a")
        )
    }
}
