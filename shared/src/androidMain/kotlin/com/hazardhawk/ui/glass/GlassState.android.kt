package com.hazardhawk.ui.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*

/**
 * Android implementation of glass morphism state management.
 * Handles device capability detection and performance monitoring.
 */
actual class GlassState private constructor(private val context: Context) {
    
    private var _configuration by mutableStateOf(GlassConfiguration.construction)
    private var _isEnabled by mutableStateOf(true)
    private var _supportLevel by mutableStateOf(GlassSupportLevel.FULL)
    private var _performanceMetrics by mutableStateOf<GlassPerformanceMetrics?>(null)
    
    private val capabilityDetector = GlassCapabilityDetector(context)
    private val environmentAdapter = ConstructionEnvironmentAdapter()
    
    // Performance monitoring
    private var performanceMonitoringJob: Job? = null
    private var monitoringStartTime: Long = 0L
    private var frameCount = 0
    private var lastFrameTime = 0L
    
    actual val configuration: GlassConfiguration
        get() = _configuration
        
    actual val isEnabled: Boolean
        get() = _isEnabled && _supportLevel != GlassSupportLevel.DISABLED
        
    actual val supportLevel: GlassSupportLevel
        get() = _supportLevel
        
    actual val performanceMetrics: GlassPerformanceMetrics?
        get() = _performanceMetrics
    
    init {
        // Initialize with device capability detection
        _supportLevel = capabilityDetector.detectCapability()
        
        // Adapt configuration based on device capability
        when (_supportLevel) {
            GlassSupportLevel.DISABLED -> {
                _configuration = GlassConfiguration.emergency.copy(
                    supportLevel = GlassSupportLevel.DISABLED
                )
                _isEnabled = false
            }
            GlassSupportLevel.REDUCED -> {
                _configuration = GlassConfiguration.construction.copy(
                    supportLevel = GlassSupportLevel.REDUCED,
                    animationsEnabled = false
                )
            }
            GlassSupportLevel.FULL -> {
                // Keep default construction configuration
            }
        }
    }
    
    actual fun updateConfiguration(config: GlassConfiguration) {
        // Validate configuration against device capabilities
        val violations = config.validate()
        if (violations.isNotEmpty()) {
            // Use fallback configuration
            _configuration = when (_supportLevel) {
                GlassSupportLevel.DISABLED -> GlassConfiguration.emergency
                GlassSupportLevel.REDUCED -> GlassConfiguration.construction
                GlassSupportLevel.FULL -> config
            }
        } else {
            _configuration = config.copy(supportLevel = _supportLevel)
        }
    }
    
    actual fun adaptForEnvironment(environment: ConstructionEnvironment) {
        val adaptedConfig = environmentAdapter.adaptConfiguration(_configuration, environment)
        
        // Update support level based on environment constraints
        _supportLevel = when {
            environment.thermalState >= ThermalState.SEVERE -> GlassSupportLevel.DISABLED
            environment.batteryLevel < 0.2f -> GlassSupportLevel.DISABLED
            environment.gloveThicknessMM > 5 -> GlassSupportLevel.DISABLED
            environment.thermalState >= ThermalState.MODERATE -> GlassSupportLevel.REDUCED
            environment.batteryLevel < 0.5f -> GlassSupportLevel.REDUCED
            environment.ambientLightLux > 50000 -> GlassSupportLevel.REDUCED
            else -> capabilityDetector.detectCapability()
        }
        
        _configuration = adaptedConfig.copy(supportLevel = _supportLevel)
        _isEnabled = _supportLevel != GlassSupportLevel.DISABLED
    }
    
    actual fun setEnabled(enabled: Boolean) {
        _isEnabled = enabled && _supportLevel != GlassSupportLevel.DISABLED
    }
    
    actual fun startPerformanceMonitoring() {
        stopPerformanceMonitoring() // Stop any existing monitoring
        
        monitoringStartTime = SystemClock.elapsedRealtime()
        frameCount = 0
        lastFrameTime = monitoringStartTime
        
        performanceMonitoringJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(1000) // Sample every second
                
                val currentTime = SystemClock.elapsedRealtime()
                val elapsedTime = currentTime - monitoringStartTime
                
                if (elapsedTime > 0) {
                    val frameRate = (frameCount * 1000.0) / elapsedTime
                    val memoryUsage = getMemoryUsageMB()
                    val batteryImpact = estimateBatteryImpact()
                    val loadTime = if (frameCount == 0) elapsedTime else 0L
                    val gpuUtilization = estimateGpuUtilization()
                    val thermalState = capabilityDetector.getCurrentThermalState()
                    
                    _performanceMetrics = GlassPerformanceMetrics(
                        frameRate = frameRate,
                        memoryUsageMB = memoryUsage,
                        batteryImpactPercent = batteryImpact,
                        loadTimeMs = loadTime,
                        gpuUtilizationPercent = gpuUtilization,
                        thermalState = thermalState
                    )
                    
                    // Auto-adapt based on performance
                    val violations = _performanceMetrics!!.validate()
                    if (violations.isNotEmpty() && _supportLevel != GlassSupportLevel.DISABLED) {
                        // Downgrade support level if performance is poor
                        _supportLevel = when (_supportLevel) {
                            GlassSupportLevel.FULL -> GlassSupportLevel.REDUCED
                            GlassSupportLevel.REDUCED -> GlassSupportLevel.DISABLED
                            GlassSupportLevel.DISABLED -> GlassSupportLevel.DISABLED
                        }
                        _configuration = _configuration.copy(
                            supportLevel = _supportLevel,
                            animationsEnabled = _supportLevel == GlassSupportLevel.FULL
                        )
                    }
                }
            }
        }
    }
    
    actual fun stopPerformanceMonitoring(): GlassPerformanceMetrics? {
        performanceMonitoringJob?.cancel()
        performanceMonitoringJob = null
        
        return _performanceMetrics
    }
    
    fun recordFrame() {
        frameCount++
        lastFrameTime = SystemClock.elapsedRealtime()
    }
    
    private fun getMemoryUsageMB(): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val usedMemory = memInfo.totalMem - memInfo.availMem
        return usedMemory / (1024 * 1024) // Convert to MB
    }
    
    private fun estimateBatteryImpact(): Double {
        // Estimate based on glass effects usage
        return when (_supportLevel) {
            GlassSupportLevel.FULL -> if (_configuration.animationsEnabled) 15.0 else 10.0
            GlassSupportLevel.REDUCED -> 8.0
            GlassSupportLevel.DISABLED -> 2.0
        }
    }
    
    private fun estimateGpuUtilization(): Double {
        // Estimate based on blur radius and configuration
        return when (_supportLevel) {
            GlassSupportLevel.FULL -> (_configuration.blurRadius / 25.0) * 80.0
            GlassSupportLevel.REDUCED -> (_configuration.blurRadius / 25.0) * 50.0
            GlassSupportLevel.DISABLED -> 5.0
        }
    }
    
    actual companion object {
        @Volatile
        private var INSTANCE: GlassState? = null
        
        actual fun getInstance(): GlassState {
            throw IllegalStateException("Context required for Android GlassState")
        }
        
        fun getInstance(context: Context): GlassState {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GlassState(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Android implementation of device capability detection
 */
actual class GlassCapabilityDetector(private val context: Context) {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    actual fun detectCapability(): GlassSupportLevel {
        val thermalState = getCurrentThermalState()
        
        // Emergency thermal throttling
        if (thermalState >= ThermalState.SEVERE) {
            return GlassSupportLevel.DISABLED
        }
        
        // API level requirements
        if (Build.VERSION.SDK_INT < 24) {
            return GlassSupportLevel.DISABLED
        }
        
        // Hardware acceleration requirement
        if (!hasHardwareAcceleration()) {
            return GlassSupportLevel.DISABLED
        }
        
        // Power saving mode
        if (isPowerSavingEnabled()) {
            return GlassSupportLevel.REDUCED
        }
        
        // RAM-based capability detection
        val ramMB = getDeviceRamMB()
        return when {
            ramMB >= 6144 && Build.VERSION.SDK_INT >= 30 -> GlassSupportLevel.FULL
            ramMB >= 3072 && Build.VERSION.SDK_INT >= 26 -> GlassSupportLevel.REDUCED
            ramMB >= 2048 -> GlassSupportLevel.DISABLED
            else -> GlassSupportLevel.DISABLED
        }
    }
    
    actual fun getDeviceRamMB(): Int {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / (1024 * 1024)).toInt()
    }
    
    actual fun getApiLevel(): Int {
        return Build.VERSION.SDK_INT
    }
    
    actual fun hasHardwareAcceleration(): Boolean {
        return activityManager.deviceConfigurationInfo.reqGlEsVersion >= 0x20000
    }
    
    actual fun getCurrentThermalState(): ThermalState {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            when (powerManager.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> ThermalState.NOMINAL
                PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.LIGHT
                PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.MODERATE
                PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.SEVERE
                PowerManager.THERMAL_STATUS_CRITICAL,
                PowerManager.THERMAL_STATUS_EMERGENCY,
                PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL
                else -> ThermalState.NOMINAL
            }
        } else {
            ThermalState.NOMINAL
        }
    }
    
    actual fun isPowerSavingEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }
}