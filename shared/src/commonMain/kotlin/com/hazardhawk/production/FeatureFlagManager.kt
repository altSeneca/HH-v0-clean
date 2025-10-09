package com.hazardhawk.production

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random

/**
 * Production-ready feature flag management system for HazardHawk
 * Handles LiteRT rollout, A/B testing, and emergency controls
 */
class FeatureFlagManager(
    private val configProvider: ProductionConfigProvider,
    private val analyticsLogger: ProductionAnalyticsLogger,
    private val userHashProvider: UserHashProvider
) {
    private val _flags = MutableStateFlow<Map<String, FeatureFlag>>(mapOf())
    val flags: StateFlow<Map<String, FeatureFlag>> = _flags.asStateFlow()

    private val _emergencyOverrides = MutableStateFlow<Map<String, Boolean>>(mapOf())
    val emergencyOverrides: StateFlow<Map<String, Boolean>> = _emergencyOverrides.asStateFlow()

    companion object {
        // LiteRT Feature Flags
        const val LITERT_ENABLED = "litert_enabled"
        const val LITERT_GPU_BACKEND = "litert_gpu_backend"
        const val LITERT_NPU_BACKEND = "litert_npu_backend"
        const val LITERT_ADAPTIVE_SWITCHING = "litert_adaptive_switching"
        const val LITERT_PERFORMANCE_MONITORING = "litert_performance_monitoring"
        
        // AI Processing Flags
        const val AI_FALLBACK_TO_CLOUD = "ai_fallback_to_cloud"
        const val AI_HYBRID_PROCESSING = "ai_hybrid_processing"
        const val AI_CACHE_OPTIMIZATION = "ai_cache_optimization"
        
        // Performance & Monitoring Flags
        const val PERFORMANCE_PROFILING = "performance_profiling"
        const val MEMORY_MONITORING = "memory_monitoring"
        const val CRASH_REPORTING_ENHANCED = "crash_reporting_enhanced"
        const val USER_ANALYTICS_ENHANCED = "user_analytics_enhanced"
        
        // Security & Privacy Flags
        const val SECURE_MODEL_VALIDATION = "secure_model_validation"
        const val PRIVACY_MODE_ENHANCED = "privacy_mode_enhanced"
        const val AUDIT_LOGGING_DETAILED = "audit_logging_detailed"
        
        // Operational Flags
        const val DEBUG_UI_OVERLAY = "debug_ui_overlay"
        const val MAINTENANCE_MODE = "maintenance_mode"
        const val BETA_FEATURES_ENABLED = "beta_features_enabled"
    }

    init {
        initializeDefaultFlags()
    }

    private fun initializeDefaultFlags() {
        val defaultFlags = mapOf(
            // LiteRT flags - start with minimal rollout
            LITERT_ENABLED to FeatureFlag(
                key = LITERT_ENABLED,
                name = "LiteRT-LM Processing",
                description = "Enable LiteRT-LM for on-device AI processing",
                rolloutPercentage = 0f, // Start with 0% - manual enable only
                isEnabled = false,
                rolloutStrategy = RolloutStrategy.GRADUAL,
                targetGroups = listOf("internal_testers"),
                dependencies = listOf(),
                emergencyKillSwitch = true,
                metadata = mapOf(
                    "performance_target" to "3x-8x improvement",
                    "memory_target" to "< 2GB",
                    "fallback_required" to "true"
                )
            ),
            
            LITERT_GPU_BACKEND to FeatureFlag(
                key = LITERT_GPU_BACKEND,
                name = "LiteRT GPU Backend",
                description = "Enable GPU acceleration for LiteRT processing",
                rolloutPercentage = 0f,
                isEnabled = false,
                rolloutStrategy = RolloutStrategy.CAPABILITY_BASED,
                targetGroups = listOf("gpu_capable_devices"),
                dependencies = listOf(LITERT_ENABLED),
                emergencyKillSwitch = true,
                metadata = mapOf(
                    "performance_target" to "1876 tokens/sec",
                    "gpu_memory_required" to "512MB"
                )
            ),
            
            LITERT_NPU_BACKEND to FeatureFlag(
                key = LITERT_NPU_BACKEND,
                name = "LiteRT NPU Backend",
                description = "Enable NPU acceleration for LiteRT processing",
                rolloutPercentage = 0f,
                isEnabled = false,
                rolloutStrategy = RolloutStrategy.CAPABILITY_BASED,
                targetGroups = listOf("npu_capable_devices"),
                dependencies = listOf(LITERT_ENABLED),
                emergencyKillSwitch = true,
                metadata = mapOf(
                    "performance_target" to "5836 tokens/sec",
                    "npu_driver_required" to "v2.0+"
                )
            ),
            
            LITERT_ADAPTIVE_SWITCHING to FeatureFlag(
                key = LITERT_ADAPTIVE_SWITCHING,
                name = "LiteRT Adaptive Backend Switching",
                description = "Automatically switch between CPU/GPU/NPU based on performance",
                rolloutPercentage = 0f,
                isEnabled = false,
                rolloutStrategy = RolloutStrategy.PERFORMANCE_BASED,
                targetGroups = listOf("performance_testers"),
                dependencies = listOf(LITERT_ENABLED),
                emergencyKillSwitch = true,
                metadata = mapOf(
                    "switch_threshold_ms" to "500",
                    "memory_pressure_threshold" to "80%"
                )
            ),
            
            LITERT_PERFORMANCE_MONITORING to FeatureFlag(
                key = LITERT_PERFORMANCE_MONITORING,
                name = "LiteRT Performance Monitoring",
                description = "Enable detailed performance tracking for LiteRT operations",
                rolloutPercentage = 100f, // Always enabled for monitoring
                isEnabled = true,
                rolloutStrategy = RolloutStrategy.IMMEDIATE,
                targetGroups = listOf("all_users"),
                dependencies = listOf(),
                emergencyKillSwitch = false,
                metadata = mapOf(
                    "metrics_collection_interval" to "1000ms",
                    "performance_history_days" to "7"
                )
            ),
            
            // Monitoring & Analytics flags
            PERFORMANCE_PROFILING to FeatureFlag(
                key = PERFORMANCE_PROFILING,
                name = "Performance Profiling",
                description = "Enable detailed performance profiling and tracing",
                rolloutPercentage = 10f,
                isEnabled = false,
                rolloutStrategy = RolloutStrategy.HASH_BASED,
                targetGroups = listOf("beta_testers"),
                dependencies = listOf(),
                emergencyKillSwitch = true
            ),
            
            CRASH_REPORTING_ENHANCED to FeatureFlag(
                key = CRASH_REPORTING_ENHANCED,
                name = "Enhanced Crash Reporting",
                description = "Detailed crash reporting with LiteRT context",
                rolloutPercentage = 100f,
                isEnabled = true,
                rolloutStrategy = RolloutStrategy.IMMEDIATE,
                targetGroups = listOf("all_users"),
                dependencies = listOf(),
                emergencyKillSwitch = false
            ),
            
            // Security flags
            SECURE_MODEL_VALIDATION to FeatureFlag(
                key = SECURE_MODEL_VALIDATION,
                name = "Secure Model Validation",
                description = "Validate model file integrity and signatures",
                rolloutPercentage = 100f,
                isEnabled = true,
                rolloutStrategy = RolloutStrategy.IMMEDIATE,
                targetGroups = listOf("all_users"),
                dependencies = listOf(),
                emergencyKillSwitch = false
            ),
            
            AUDIT_LOGGING_DETAILED to FeatureFlag(
                key = AUDIT_LOGGING_DETAILED,
                name = "Detailed Audit Logging",
                description = "Enhanced audit logging for security compliance",
                rolloutPercentage = 100f,
                isEnabled = true,
                rolloutStrategy = RolloutStrategy.IMMEDIATE,
                targetGroups = listOf("all_users"),
                dependencies = listOf(),
                emergencyKillSwitch = false
            ),
            
            // Operational flags
            MAINTENANCE_MODE to FeatureFlag(
                key = MAINTENANCE_MODE,
                name = "Maintenance Mode",
                description = "Enable maintenance mode with reduced functionality",
                rolloutPercentage = 0f,
                isEnabled = false,
                rolloutStrategy = RolloutStrategy.MANUAL,
                targetGroups = listOf(),
                dependencies = listOf(),
                emergencyKillSwitch = false
            )
        )
        
        _flags.value = defaultFlags
    }

    /**
     * Check if a feature is enabled for the current user
     */
    suspend fun isFeatureEnabled(key: String, userId: String? = null): Boolean {
        // Check emergency overrides first
        _emergencyOverrides.value[key]?.let { return it }
        
        val flag = _flags.value[key] ?: return false
        
        // Check dependencies first
        for (dependency in flag.dependencies) {
            if (!isFeatureEnabled(dependency, userId)) {
                analyticsLogger.logFeatureFlagDependencyBlocked(key, dependency)
                return false
            }
        }
        
        // Check if flag is globally enabled
        if (!flag.isEnabled) {
            return false
        }
        
        // Apply rollout strategy
        val isEnabledForUser = when (flag.rolloutStrategy) {
            RolloutStrategy.IMMEDIATE -> true
            RolloutStrategy.MANUAL -> flag.targetGroups.contains("manual_enable")
            RolloutStrategy.HASH_BASED -> {
                userId?.let { 
                    val userHash = userHashProvider.getUserHash(it)
                    (userHash % 100) < flag.rolloutPercentage.toInt()
                } ?: false
            }
            RolloutStrategy.GRADUAL -> {
                // Simple time-based gradual rollout
                val daysSinceEpoch = (Clock.System.now().epochSeconds / 86400).toInt()
                val rolloutHash = (daysSinceEpoch + flag.key.hashCode()) % 100
                rolloutHash < flag.rolloutPercentage.toInt()
            }
            RolloutStrategy.CAPABILITY_BASED -> {
                // Check device capabilities (implementation depends on device detector)
                checkDeviceCapabilities(flag.key)
            }
            RolloutStrategy.PERFORMANCE_BASED -> {
                // Enable based on device performance metrics
                checkPerformanceRequirements(flag.key)
            }
        }
        
        // Log feature flag evaluation
        analyticsLogger.logFeatureFlagEvaluation(
            flagKey = key,
            enabled = isEnabledForUser,
            strategy = flag.rolloutStrategy.name,
            rolloutPercentage = flag.rolloutPercentage,
            userId = userId
        )
        
        return isEnabledForUser
    }

    /**
     * Enable emergency override for a feature flag
     * Used for immediate rollbacks or emergency enables
     */
    suspend fun setEmergencyOverride(key: String, enabled: Boolean, reason: String) {
        val currentOverrides = _emergencyOverrides.value.toMutableMap()
        currentOverrides[key] = enabled
        _emergencyOverrides.value = currentOverrides
        
        analyticsLogger.logEmergencyOverride(
            flagKey = key,
            enabled = enabled,
            reason = reason,
            timestamp = Clock.System.now()
        )
    }

    /**
     * Update feature flag configuration from remote config
     */
    suspend fun updateFromRemoteConfig() {
        try {
            val remoteConfig = configProvider.fetchFeatureFlags()
            val updatedFlags = _flags.value.toMutableMap()
            
            remoteConfig.forEach { (key, remoteFlag) ->
                updatedFlags[key] = remoteFlag
            }
            
            _flags.value = updatedFlags
            analyticsLogger.logRemoteConfigUpdate(remoteConfig.keys.toList())
            
        } catch (e: Exception) {
            analyticsLogger.logRemoteConfigError(e.message ?: "Unknown error")
        }
    }

    /**
     * Get A/B test assignment for user
     */
    suspend fun getABTestAssignment(testKey: String, userId: String): String {
        val userHash = userHashProvider.getUserHash(userId)
        val assignment = when (userHash % 100) {
            in 0..49 -> "control"
            in 50..99 -> "treatment"
            else -> "control"
        }
        
        analyticsLogger.logABTestAssignment(testKey, assignment, userId)
        return assignment
    }

    /**
     * Batch check multiple feature flags
     */
    suspend fun checkFeatures(keys: List<String>, userId: String? = null): Map<String, Boolean> {
        return keys.associateWith { isFeatureEnabled(it, userId) }
    }

    private fun checkDeviceCapabilities(flagKey: String): Boolean {
        // Implementation would check actual device capabilities
        // For now, return a default based on flag requirements
        return when (flagKey) {
            LITERT_GPU_BACKEND -> {
                // Check for GPU availability and memory
                true // Placeholder - implement actual GPU detection
            }
            LITERT_NPU_BACKEND -> {
                // Check for NPU/NNAPI availability
                false // Placeholder - NPU less common currently
            }
            else -> true
        }
    }

    private fun checkPerformanceRequirements(flagKey: String): Boolean {
        // Implementation would check device performance metrics
        // For now, return based on basic device characteristics
        return when (flagKey) {
            LITERT_ADAPTIVE_SWITCHING -> {
                // Only enable on mid-range and above devices
                true // Placeholder - implement actual performance detection
            }
            else -> true
        }
    }
}

/**
 * Feature flag configuration data class
 */
data class FeatureFlag(
    val key: String,
    val name: String,
    val description: String,
    val rolloutPercentage: Float,
    val isEnabled: Boolean,
    val rolloutStrategy: RolloutStrategy,
    val targetGroups: List<String>,
    val dependencies: List<String>,
    val emergencyKillSwitch: Boolean,
    val metadata: Map<String, String> = mapOf(),
    val lastUpdated: Instant = Clock.System.now()
)

/**
 * Feature flag rollout strategies
 */
enum class RolloutStrategy {
    IMMEDIATE,           // Enable immediately for all users
    MANUAL,              // Manual enable only
    HASH_BASED,          // Based on user hash
    GRADUAL,             // Gradual rollout over time
    CAPABILITY_BASED,    // Based on device capabilities
    PERFORMANCE_BASED    // Based on device performance
}

/**
 * Provider for remote feature flag configuration
 */
interface ProductionConfigProvider {
    suspend fun fetchFeatureFlags(): Map<String, FeatureFlag>
    suspend fun reportMetrics(metrics: Map<String, Any>)
}

/**
 * Analytics logger for feature flag events
 */
interface ProductionAnalyticsLogger {
    fun logFeatureFlagEvaluation(
        flagKey: String,
        enabled: Boolean,
        strategy: String,
        rolloutPercentage: Float,
        userId: String?
    )
    
    fun logFeatureFlagDependencyBlocked(flagKey: String, blockedDependency: String)
    
    fun logEmergencyOverride(
        flagKey: String,
        enabled: Boolean,
        reason: String,
        timestamp: Instant
    )
    
    fun logRemoteConfigUpdate(updatedKeys: List<String>)
    
    fun logRemoteConfigError(error: String)
    
    fun logABTestAssignment(testKey: String, assignment: String, userId: String)
}

/**
 * Provider for consistent user hashing
 */
interface UserHashProvider {
    fun getUserHash(userId: String): Int
}