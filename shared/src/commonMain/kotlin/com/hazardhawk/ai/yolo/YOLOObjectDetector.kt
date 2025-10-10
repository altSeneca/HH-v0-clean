package com.hazardhawk.ai.yolo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import com.hazardhawk.core.models.WorkType

/**
 * Cross-platform YOLO object detection interface using expect/actual pattern.
 * This interface provides the core abstraction for YOLO11 model integration
 * across Android, iOS, Desktop, and Web platforms.
 */
expect class YOLOObjectDetector {
    /**
     * Initialize the YOLO detector with specified configuration
     * @param configuration Model configuration for device-adaptive selection
     * @return Result indicating success or failure with error details
     */
    suspend fun initialize(configuration: YOLOModelConfiguration): Result<Unit>
    
    /**
     * Detect objects in the provided image data
     * @param imageData Raw image bytes (JPEG, PNG, etc.)
     * @param workType Context for construction-specific detection
     * @return Detection results or error
     */
    suspend fun detectObjects(
        imageData: ByteArray,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION
    ): Result<YOLODetectionResult>
    
    /**
     * Batch detect objects in multiple images
     * @param imageBatch List of image data arrays
     * @param workType Context for construction-specific detection
     * @return Flow of detection results
     */
    fun detectObjectsBatch(
        imageBatch: List<ByteArray>,
        workType: WorkType = WorkType.GENERAL_CONSTRUCTION
    ): Flow<Result<YOLODetectionResult>>
    
    /**
     * Get current detector status
     */
    val detectorStatus: StateFlow<YOLODetectorStatus>
    
    /**
     * Check if detector is ready for inference
     */
    val isReady: Boolean
    
    /**
     * Get current model configuration
     */
    val currentConfiguration: YOLOModelConfiguration?
    
    /**
     * Clean up resources and shutdown detector
     */
    suspend fun cleanup()
    
    /**
     * Get device capability assessment
     * @return Device capability information for model selection
     */
    suspend fun getDeviceCapability(): DeviceCapability
    
    /**
     * Update detection parameters at runtime
     * @param confidenceThreshold Minimum confidence for detections
     * @param iouThreshold IoU threshold for non-maximum suppression
     */
    fun updateDetectionParameters(
        confidenceThreshold: Float,
        iouThreshold: Float
    )
    
    /**
     * Get performance metrics for the last detection
     */
    fun getLastPerformanceMetrics(): YOLOPerformanceMetrics?
}

/**
 * YOLO detector status enumeration
 */
enum class YOLODetectorStatus {
    UNINITIALIZED,
    INITIALIZING,
    READY,
    PROCESSING,
    ERROR,
    CLEANUP
}

/**
 * Performance metrics for YOLO detection
 */
data class YOLOPerformanceMetrics(
    val inferenceTimeMs: Long,
    val preprocessTimeMs: Long,
    val postprocessTimeMs: Long,
    val totalTimeMs: Long,
    val memoryUsageMB: Long,
    val detectionsCount: Int,
    val inputImageSize: ImageSize,
    val modelInputSize: ImageSize
) {
    val fps: Float get() = if (totalTimeMs > 0) 1000f / totalTimeMs else 0f
    val throughput: Float get() = detectionsCount.toFloat() / (totalTimeMs / 1000f)
}

/**
 * YOLO detection factory for creating platform-specific instances
 */
expect object YOLODetectorFactory {
    /**
     * Create a new YOLO detector instance optimized for current platform
     * @param enableGPU Whether to enable GPU acceleration if available
     * @param optimizeForBattery Whether to prioritize battery life over performance
     * @return Platform-specific detector instance
     */
    fun createDetector(
        enableGPU: Boolean = true,
        optimizeForBattery: Boolean = false
    ): YOLOObjectDetector
    
    /**
     * Get optimal model configuration for current device
     * @return Recommended configuration based on device capabilities
     */
    suspend fun getOptimalConfiguration(): YOLOModelConfiguration
    
    /**
     * Check if YOLO detection is supported on current platform
     * @return True if platform supports YOLO detection
     */
    fun isSupported(): Boolean
    
    /**
     * Get list of available model files on current platform
     * @return List of model configurations that are available
     */
    suspend fun getAvailableModels(): List<YOLOModelConfiguration>
}

/**
 * Common YOLO detector implementation providing shared logic
 */
abstract class BaseYOLOObjectDetector {
    protected var _configuration: YOLOModelConfiguration? = null
    protected var _isReady: Boolean = false
    protected var _lastMetrics: YOLOPerformanceMetrics? = null
    
    val currentConfiguration: YOLOModelConfiguration?
        get() = _configuration
        
    val isReady: Boolean
        get() = _isReady
        
    fun getLastPerformanceMetrics(): YOLOPerformanceMetrics? = _lastMetrics
    
    /**
     * Validate detection parameters
     */
    protected fun validateDetectionParameters(
        confidenceThreshold: Float,
        iouThreshold: Float
    ): Result<Unit> {
        return when {
            confidenceThreshold !in 0.0f..1.0f -> 
                Result.failure(IllegalArgumentException("Confidence threshold must be between 0.0 and 1.0"))
            iouThreshold !in 0.0f..1.0f -> 
                Result.failure(IllegalArgumentException("IoU threshold must be between 0.0 and 1.0"))
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Validate image data
     */
    protected fun validateImageData(imageData: ByteArray): Result<Unit> {
        return when {
            imageData.isEmpty() -> 
                Result.failure(IllegalArgumentException("Image data cannot be empty"))
            imageData.size > 50 * 1024 * 1024 -> // 50MB limit
                Result.failure(IllegalArgumentException("Image data too large (>50MB)"))
            else -> Result.success(Unit)
        }
    }
    
    /**
     * Create performance metrics from timing data
     */
    protected fun createPerformanceMetrics(
        inferenceTime: Long,
        preprocessTime: Long,
        postprocessTime: Long,
        detectionsCount: Int,
        inputSize: ImageSize,
        modelSize: ImageSize,
        memoryUsage: Long
    ): YOLOPerformanceMetrics {
        val totalTime = inferenceTime + preprocessTime + postprocessTime
        return YOLOPerformanceMetrics(
            inferenceTimeMs = inferenceTime,
            preprocessTimeMs = preprocessTime,
            postprocessTimeMs = postprocessTime,
            totalTimeMs = totalTime,
            memoryUsageMB = memoryUsage,
            detectionsCount = detectionsCount,
            inputImageSize = inputSize,
            modelInputSize = modelSize
        )
    }
    
    /**
     * Apply Non-Maximum Suppression to remove duplicate detections
     */
    protected fun applyNMS(
        detections: List<YOLOBoundingBox>,
        iouThreshold: Float
    ): List<YOLOBoundingBox> {
        if (detections.isEmpty()) return emptyList()
        
        // Sort by confidence (descending)
        val sorted = detections.sortedByDescending { it.confidence }
        val result = mutableListOf<YOLOBoundingBox>()
        val suppressed = mutableSetOf<Int>()
        
        for (i in sorted.indices) {
            if (i in suppressed) continue
            
            val current = sorted[i]
            result.add(current)
            
            // Suppress overlapping boxes
            for (j in (i + 1) until sorted.size) {
                if (j in suppressed) continue
                
                val candidate = sorted[j]
                if (current.classId == candidate.classId && 
                    calculateIoU(current, candidate) > iouThreshold) {
                    suppressed.add(j)
                }
            }
        }
        
        return result
    }
    
    /**
     * Calculate Intersection over Union (IoU) between two bounding boxes
     */
    private fun calculateIoU(box1: YOLOBoundingBox, box2: YOLOBoundingBox): Float {
        // Convert center coordinates to corners
        val x1_1 = box1.x - box1.width / 2
        val y1_1 = box1.y - box1.height / 2
        val x2_1 = box1.x + box1.width / 2
        val y2_1 = box1.y + box1.height / 2
        
        val x1_2 = box2.x - box2.width / 2
        val y1_2 = box2.y - box2.height / 2
        val x2_2 = box2.x + box2.width / 2
        val y2_2 = box2.y + box2.height / 2
        
        // Calculate intersection
        val intersectionX1 = maxOf(x1_1, x1_2)
        val intersectionY1 = maxOf(y1_1, y1_2)
        val intersectionX2 = minOf(x2_1, x2_2)
        val intersectionY2 = minOf(y2_1, y2_2)
        
        val intersectionWidth = maxOf(0f, intersectionX2 - intersectionX1)
        val intersectionHeight = maxOf(0f, intersectionY2 - intersectionY1)
        val intersectionArea = intersectionWidth * intersectionHeight
        
        // Calculate union
        val area1 = box1.width * box1.height
        val area2 = box2.width * box2.height
        val unionArea = area1 + area2 - intersectionArea
        
        return if (unionArea > 0f) intersectionArea / unionArea else 0f
    }
}

/**
 * Exception types for YOLO detection errors
 */
sealed class YOLODetectionException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ModelNotInitialized : YOLODetectionException("YOLO model not initialized")
    class ModelLoadFailed(cause: Throwable) : YOLODetectionException("Failed to load YOLO model", cause)
    class InferenceError(message: String, cause: Throwable? = null) : YOLODetectionException("Inference error: $message", cause)
    class InvalidImageData(message: String) : YOLODetectionException("Invalid image data: $message")
    class UnsupportedPlatform : YOLODetectionException("YOLO detection not supported on this platform")
    class OutOfMemory : YOLODetectionException("Insufficient memory for YOLO inference")
}
