package com.hazardhawk.ai.litert

import com.hazardhawk.core.models.WorkType

/**
 * Android stub implementation of LiteRTModelEngine.
 * This is a placeholder until full LiteRT integration is implemented.
 */
actual class LiteRTModelEngine {
    
    actual val isAvailable: Boolean = false
    
    actual val supportedBackends: Set<LiteRTBackend> = emptySet()
    
    actual val currentBackend: LiteRTBackend? = null
    
    actual suspend fun initialize(
        modelPath: String,
        backend: LiteRTBackend
    ): Result<Unit> {
        return Result.failure(LiteRTException.InitializationException("LiteRT not yet implemented for Android"))
    }
    
    actual suspend fun generateSafetyAnalysis(
        imageData: ByteArray,
        workType: WorkType,
        includeOSHACodes: Boolean,
        confidenceThreshold: Float,
        progressCallback: ((LiteRTProgressUpdate) -> Unit)?
    ): Result<LiteRTAnalysisResult> {
        return Result.failure(LiteRTException.InferenceException("LiteRT not yet implemented for Android"))
    }
    
    actual fun getPerformanceMetrics(): LiteRTPerformanceMetrics {
        return LiteRTPerformanceMetrics(
            analysisCount = 0,
            averageProcessingTimeMs = 0,
            tokensPerSecond = 0f,
            peakMemoryUsageMB = 0f,
            averageMemoryUsageMB = 0f,
            successRate = 0f,
            preferredBackend = null,
            thermalThrottlingDetected = false
        )
    }
    
    actual fun cleanup() {
        // No-op for stub implementation
    }
}
