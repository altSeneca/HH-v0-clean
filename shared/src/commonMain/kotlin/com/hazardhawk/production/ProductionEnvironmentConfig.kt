package com.hazardhawk.production

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Production environment configuration for HazardHawk
 * Manages staging, production, and environment-specific settings
 */
@Serializable
data class ProductionEnvironmentConfig(
    val environment: Environment,
    val version: String,
    val buildNumber: String,
    val deploymentTimestamp: String,
    
    // API Configuration
    val apiConfiguration: ApiConfiguration,
    
    // LiteRT Configuration
    val liteRTConfiguration: LiteRTConfiguration,
    
    // Monitoring Configuration
    val monitoringConfiguration: MonitoringConfiguration,
    
    // Security Configuration
    val securityConfiguration: SecurityConfiguration,
    
    // Feature Flags
    val featureFlags: Map<String, Boolean>,
    
    // Performance Configuration
    val performanceConfiguration: PerformanceConfiguration,
    
    // Logging Configuration
    val loggingConfiguration: LoggingConfiguration
) {
    companion object {
        /**
         * Development environment configuration
         */
        fun developmentConfig() = ProductionEnvironmentConfig(
            environment = Environment.DEVELOPMENT,
            version = "1.0.0-dev",
            buildNumber = "dev-build",
            deploymentTimestamp = Clock.System.now().toString(),
            
            apiConfiguration = ApiConfiguration(
                baseUrl = "https://dev-api.hazardhawk.com",
                timeout = 30_000L,
                retryAttempts = 3,
                enableLogging = true,
                rateLimitEnabled = false
            ),
            
            liteRTConfiguration = LiteRTConfiguration(
                enabled = false, // Disabled in dev by default
                modelCacheSize = 100L, // 100MB
                maxConcurrentInferences = 2,
                fallbackToCloudEnabled = true,
                performanceMonitoringEnabled = true,
                adaptiveSwitchingEnabled = false,
                backends = mapOf(
                    "CPU" to LiteRTBackendConfig(enabled = true, priority = 3),
                    "GPU" to LiteRTBackendConfig(enabled = false, priority = 2),
                    "NPU" to LiteRTBackendConfig(enabled = false, priority = 1)
                )
            ),
            
            monitoringConfiguration = MonitoringConfiguration(
                crashReportingEnabled = true,
                performanceMonitoringEnabled = true,
                analyticsEnabled = false,
                loggingLevel = LogLevel.DEBUG,
                metricsCollectionInterval = 60_000L, // 1 minute
                alertingEnabled = true,
                remoteLoggingEnabled = false
            ),
            
            securityConfiguration = SecurityConfiguration(
                modelValidationEnabled = true,
                encryptionEnabled = false, // Disabled for dev convenience
                auditLoggingEnabled = true,
                securityScanningEnabled = false,
                complianceCheckingEnabled = false,
                emergencyLockdownEnabled = true
            ),
            
            featureFlags = mapOf(
                "litert_enabled" to false,
                "performance_profiling" to true,
                "debug_ui_overlay" to true,
                "mock_ai_responses" to true
            ),
            
            performanceConfiguration = PerformanceConfiguration(
                memoryThresholdMB = 512f, // Lower threshold for dev devices
                cacheSize = 50L, // 50MB
                preloadModels = false,
                backgroundProcessingEnabled = false,
                optimizationsEnabled = false
            ),
            
            loggingConfiguration = LoggingConfiguration(
                level = LogLevel.DEBUG,
                enableFileLogging = true,
                maxLogFiles = 5,
                maxLogSizeMB = 10L,
                sensitiveDataRedactionEnabled = false
            )
        )
        
        /**
         * Staging environment configuration
         */
        fun stagingConfig() = ProductionEnvironmentConfig(
            environment = Environment.STAGING,
            version = "1.0.0-staging",
            buildNumber = "staging-${Clock.System.now().epochSeconds}",
            deploymentTimestamp = Clock.System.now().toString(),
            
            apiConfiguration = ApiConfiguration(
                baseUrl = "https://staging-api.hazardhawk.com",
                timeout = 20_000L,
                retryAttempts = 2,
                enableLogging = true,
                rateLimitEnabled = true
            ),
            
            liteRTConfiguration = LiteRTConfiguration(
                enabled = true, // Enable for staging testing
                modelCacheSize = 200L, // 200MB
                maxConcurrentInferences = 3,
                fallbackToCloudEnabled = true,
                performanceMonitoringEnabled = true,
                adaptiveSwitchingEnabled = true, // Test adaptive switching
                backends = mapOf(
                    "CPU" to LiteRTBackendConfig(enabled = true, priority = 3),
                    "GPU" to LiteRTBackendConfig(enabled = true, priority = 2),
                    "NPU" to LiteRTBackendConfig(enabled = false, priority = 1) // NPU disabled until proven stable
                )
            ),
            
            monitoringConfiguration = MonitoringConfiguration(
                crashReportingEnabled = true,
                performanceMonitoringEnabled = true,
                analyticsEnabled = true,
                loggingLevel = LogLevel.INFO,
                metricsCollectionInterval = 30_000L, // 30 seconds
                alertingEnabled = true,
                remoteLoggingEnabled = true
            ),
            
            securityConfiguration = SecurityConfiguration(
                modelValidationEnabled = true,
                encryptionEnabled = true,
                auditLoggingEnabled = true,
                securityScanningEnabled = true,
                complianceCheckingEnabled = true,
                emergencyLockdownEnabled = true
            ),
            
            featureFlags = mapOf(
                "litert_enabled" to true,
                "litert_gpu_backend" to true,
                "litert_adaptive_switching" to true,
                "performance_profiling" to true,
                "debug_ui_overlay" to false
            ),
            
            performanceConfiguration = PerformanceConfiguration(
                memoryThresholdMB = 1500f,
                cacheSize = 200L, // 200MB
                preloadModels = true,
                backgroundProcessingEnabled = true,
                optimizationsEnabled = true
            ),
            
            loggingConfiguration = LoggingConfiguration(
                level = LogLevel.INFO,
                enableFileLogging = true,
                maxLogFiles = 7,
                maxLogSizeMB = 20L,
                sensitiveDataRedactionEnabled = true
            )
        )
        
        /**
         * Production environment configuration
         */
        fun productionConfig() = ProductionEnvironmentConfig(
            environment = Environment.PRODUCTION,
            version = "1.0.0",
            buildNumber = "prod-${Clock.System.now().epochSeconds}",
            deploymentTimestamp = Clock.System.now().toString(),
            
            apiConfiguration = ApiConfiguration(
                baseUrl = "https://api.hazardhawk.com",
                timeout = 15_000L,
                retryAttempts = 3,
                enableLogging = false, // Disable verbose logging in prod
                rateLimitEnabled = true
            ),
            
            liteRTConfiguration = LiteRTConfiguration(
                enabled = false, // Start with LiteRT disabled in production
                modelCacheSize = 500L, // 500MB for production devices
                maxConcurrentInferences = 4,
                fallbackToCloudEnabled = true,
                performanceMonitoringEnabled = true,
                adaptiveSwitchingEnabled = false, // Disable until proven stable
                backends = mapOf(
                    "CPU" to LiteRTBackendConfig(enabled = true, priority = 3),
                    "GPU" to LiteRTBackendConfig(enabled = false, priority = 2), // Gradual rollout
                    "NPU" to LiteRTBackendConfig(enabled = false, priority = 1)  // Future rollout
                )
            ),
            
            monitoringConfiguration = MonitoringConfiguration(
                crashReportingEnabled = true,
                performanceMonitoringEnabled = true,
                analyticsEnabled = true,
                loggingLevel = LogLevel.WARNING, // Only warnings and errors in prod
                metricsCollectionInterval = 120_000L, // 2 minutes
                alertingEnabled = true,
                remoteLoggingEnabled = true
            ),
            
            securityConfiguration = SecurityConfiguration(
                modelValidationEnabled = true,
                encryptionEnabled = true,
                auditLoggingEnabled = true,
                securityScanningEnabled = true,
                complianceCheckingEnabled = true,
                emergencyLockdownEnabled = true
            ),
            
            featureFlags = mapOf(
                "litert_enabled" to false, // Manual rollout control
                "litert_gpu_backend" to false,
                "litert_npu_backend" to false,
                "litert_adaptive_switching" to false,
                "performance_profiling" to false,
                "debug_ui_overlay" to false,
                "crash_reporting_enhanced" to true,
                "secure_model_validation" to true,
                "audit_logging_detailed" to true
            ),
            
            performanceConfiguration = PerformanceConfiguration(
                memoryThresholdMB = 1800f, // Close to 2GB limit
                cacheSize = 500L, // 500MB
                preloadModels = true,
                backgroundProcessingEnabled = true,
                optimizationsEnabled = true
            ),
            
            loggingConfiguration = LoggingConfiguration(
                level = LogLevel.WARNING,
                enableFileLogging = true,
                maxLogFiles = 10,
                maxLogSizeMB = 50L,
                sensitiveDataRedactionEnabled = true
            )
        )
    }
    
    fun isProduction() = environment == Environment.PRODUCTION
    fun isStaging() = environment == Environment.STAGING
    fun isDevelopment() = environment == Environment.DEVELOPMENT
    
    fun getLiteRTBackendConfig(backendName: String): LiteRTBackendConfig? {
        return liteRTConfiguration.backends[backendName]
    }
    
    fun isFeatureEnabled(featureName: String): Boolean {
        return featureFlags[featureName] ?: false
    }
}

@Serializable
data class ApiConfiguration(
    val baseUrl: String,
    val timeout: Long,
    val retryAttempts: Int,
    val enableLogging: Boolean,
    val rateLimitEnabled: Boolean,
    val apiKeys: Map<String, String> = mapOf()
)

@Serializable
data class LiteRTConfiguration(
    val enabled: Boolean,
    val modelCacheSize: Long, // Size in MB
    val maxConcurrentInferences: Int,
    val fallbackToCloudEnabled: Boolean,
    val performanceMonitoringEnabled: Boolean,
    val adaptiveSwitchingEnabled: Boolean,
    val backends: Map<String, LiteRTBackendConfig>,
    val modelUrls: Map<String, String> = mapOf(
        "safety_detection" to "/models/safety-detection-v1.tflite",
        "hazard_classification" to "/models/hazard-classification-v1.tflite"
    )
)

@Serializable
data class LiteRTBackendConfig(
    val enabled: Boolean,
    val priority: Int, // 1 = highest priority
    val memoryLimitMB: Float = 0f, // 0 = no limit
    val timeoutMs: Long = 10_000L,
    val fallbackOnError: Boolean = true
)

@Serializable
data class MonitoringConfiguration(
    val crashReportingEnabled: Boolean,
    val performanceMonitoringEnabled: Boolean,
    val analyticsEnabled: Boolean,
    val loggingLevel: LogLevel,
    val metricsCollectionInterval: Long,
    val alertingEnabled: Boolean,
    val remoteLoggingEnabled: Boolean,
    val alertEndpoints: Map<String, String> = mapOf()
)

@Serializable
data class SecurityConfiguration(
    val modelValidationEnabled: Boolean,
    val encryptionEnabled: Boolean,
    val auditLoggingEnabled: Boolean,
    val securityScanningEnabled: Boolean,
    val complianceCheckingEnabled: Boolean,
    val emergencyLockdownEnabled: Boolean,
    val trustedModelSources: List<String> = listOf(
        "https://models.hazardhawk.com",
        "https://cdn.hazardhawk.com"
    )
)

@Serializable
data class PerformanceConfiguration(
    val memoryThresholdMB: Float,
    val cacheSize: Long,
    val preloadModels: Boolean,
    val backgroundProcessingEnabled: Boolean,
    val optimizationsEnabled: Boolean,
    val performanceTargets: Map<String, Float> = mapOf(
        "cpu_tokens_per_second" to 243f,
        "gpu_tokens_per_second" to 1876f,
        "npu_tokens_per_second" to 5836f,
        "max_memory_mb" to 2000f,
        "max_inference_latency_ms" to 5000f
    )
)

@Serializable
data class LoggingConfiguration(
    val level: LogLevel,
    val enableFileLogging: Boolean,
    val maxLogFiles: Int,
    val maxLogSizeMB: Long,
    val sensitiveDataRedactionEnabled: Boolean,
    val logCategories: Map<String, LogLevel> = mapOf(
        "AI" to LogLevel.INFO,
        "Performance" to LogLevel.DEBUG,
        "Security" to LogLevel.WARNING,
        "Network" to LogLevel.ERROR
    )
)

enum class Environment {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

enum class LogLevel(val value: Int) {
    DEBUG(0),
    INFO(1),
    WARNING(2),
    ERROR(3),
    CRITICAL(4)
}

/**
 * Environment configuration manager
 * Handles loading and switching between environment configurations
 */
class EnvironmentConfigManager {
    private var currentConfig: ProductionEnvironmentConfig = ProductionEnvironmentConfig.developmentConfig()
    
    fun loadConfiguration(environment: Environment): ProductionEnvironmentConfig {
        currentConfig = when (environment) {
            Environment.DEVELOPMENT -> ProductionEnvironmentConfig.developmentConfig()
            Environment.STAGING -> ProductionEnvironmentConfig.stagingConfig()
            Environment.PRODUCTION -> ProductionEnvironmentConfig.productionConfig()
        }
        return currentConfig
    }
    
    fun getCurrentConfig(): ProductionEnvironmentConfig = currentConfig
    
    fun switchEnvironment(newEnvironment: Environment): ProductionEnvironmentConfig {
        if (currentConfig.environment == Environment.PRODUCTION && newEnvironment != Environment.PRODUCTION) {
            // Log environment switch from production
            logEnvironmentSwitch(currentConfig.environment, newEnvironment)
        }
        return loadConfiguration(newEnvironment)
    }
    
    fun updateFeatureFlag(flagName: String, enabled: Boolean): ProductionEnvironmentConfig {
        val updatedFlags = currentConfig.featureFlags.toMutableMap()
        updatedFlags[flagName] = enabled
        
        currentConfig = currentConfig.copy(featureFlags = updatedFlags)
        logFeatureFlagUpdate(flagName, enabled)
        
        return currentConfig
    }
    
    fun validateConfiguration(): ConfigurationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Validate API configuration
        if (currentConfig.apiConfiguration.baseUrl.isEmpty()) {
            errors.add("API base URL is required")
        }
        
        if (!currentConfig.apiConfiguration.baseUrl.startsWith("https://")) {
            if (currentConfig.environment == Environment.PRODUCTION) {
                errors.add("Production API must use HTTPS")
            } else {
                warnings.add("Non-HTTPS API URL detected")
            }
        }
        
        // Validate LiteRT configuration
        if (currentConfig.liteRTConfiguration.enabled) {
            if (currentConfig.liteRTConfiguration.modelCacheSize < 100) {
                warnings.add("Model cache size is very small")
            }
            
            if (currentConfig.liteRTConfiguration.maxConcurrentInferences > 5) {
                warnings.add("High concurrent inference limit may cause memory issues")
            }
        }
        
        // Validate security configuration
        if (currentConfig.environment == Environment.PRODUCTION) {
            if (!currentConfig.securityConfiguration.encryptionEnabled) {
                errors.add("Encryption must be enabled in production")
            }
            
            if (!currentConfig.securityConfiguration.auditLoggingEnabled) {
                errors.add("Audit logging must be enabled in production")
            }
            
            if (currentConfig.loggingConfiguration.level == LogLevel.DEBUG) {
                warnings.add("Debug logging should not be used in production")
            }
        }
        
        // Validate performance configuration
        if (currentConfig.performanceConfiguration.memoryThresholdMB > 2000) {
            errors.add("Memory threshold exceeds recommended 2GB limit")
        }
        
        return ConfigurationValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun logEnvironmentSwitch(from: Environment, to: Environment) {
        // Implementation would log to appropriate logging system
        println("Environment switched from $from to $to at ${Clock.System.now()}")
    }
    
    private fun logFeatureFlagUpdate(flagName: String, enabled: Boolean) {
        // Implementation would log to appropriate logging system
        println("Feature flag '$flagName' set to $enabled at ${Clock.System.now()}")
    }
}

data class ConfigurationValidationResult(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Configuration constants for different deployment scenarios
 */
object ConfigurationConstants {
    // Model URLs for different environments
    val PRODUCTION_MODEL_URLS = mapOf(
        "safety_detection" to "https://models.hazardhawk.com/v1/safety-detection.tflite",
        "hazard_classification" to "https://models.hazardhawk.com/v1/hazard-classification.tflite"
    )
    
    val STAGING_MODEL_URLS = mapOf(
        "safety_detection" to "https://staging-models.hazardhawk.com/v1/safety-detection.tflite",
        "hazard_classification" to "https://staging-models.hazardhawk.com/v1/hazard-classification.tflite"
    )
    
    // Performance targets by device tier
    val HIGH_END_DEVICE_TARGETS = mapOf(
        "cpu_tokens_per_second" to 350f,
        "gpu_tokens_per_second" to 2500f,
        "npu_tokens_per_second" to 8000f,
        "max_memory_mb" to 3000f
    )
    
    val MID_RANGE_DEVICE_TARGETS = mapOf(
        "cpu_tokens_per_second" to 243f,
        "gpu_tokens_per_second" to 1876f,
        "npu_tokens_per_second" to 5836f,
        "max_memory_mb" to 2000f
    )
    
    val LOW_END_DEVICE_TARGETS = mapOf(
        "cpu_tokens_per_second" to 150f,
        "gpu_tokens_per_second" to 800f,
        "npu_tokens_per_second" to 2000f,
        "max_memory_mb" to 1000f
    )
    
    // Alert endpoints for different severity levels
    val PRODUCTION_ALERT_ENDPOINTS = mapOf(
        "critical" to "https://alerts.hazardhawk.com/critical",
        "warning" to "https://alerts.hazardhawk.com/warning",
        "info" to "https://alerts.hazardhawk.com/info"
    )
    
    // Security settings
    val PRODUCTION_SECURITY_SETTINGS = mapOf(
        "max_failed_validations_per_hour" to 10,
        "model_signature_required" to true,
        "audit_retention_days" to 90,
        "encryption_key_rotation_days" to 30
    )
}