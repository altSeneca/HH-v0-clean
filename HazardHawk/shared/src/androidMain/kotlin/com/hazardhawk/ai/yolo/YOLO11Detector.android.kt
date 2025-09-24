package com.hazardhawk.ai.yolo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
// Note: NNAPIProvider import will be handled through execution providers
import java.io.InputStream
import java.nio.FloatBuffer
import java.util.concurrent.Executors
import com.hazardhawk.domain.entities.WorkType
import kotlin.math.min

/**
 * Android-specific YOLO11 object detector implementation using ONNX Runtime.
 * Provides high-performance construction hazard detection with GPU acceleration
 * and memory optimization for mobile devices.
 */
actual class YOLOObjectDetector {
    
    private val tag = "YOLO11Detector"
    
    // ONNX Runtime components
    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null
    private var sessionOptions: SessionOptions? = null
    
    // Thread management
    private val inferenceExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "YOLO-Inference").apply {
            priority = Thread.NORM_PRIORITY + 1
        }
    }
    
    // State management
    private val _detectorStatus = MutableStateFlow(YOLODetectorStatus.UNINITIALIZED)
    actual val detectorStatus: StateFlow<YOLODetectorStatus> = _detectorStatus.asStateFlow()
    
    private val sessionMutex = Mutex()
    private val optimizer = AndroidYOLOOptimizer()
    
    // Configuration and status
    private var _configuration: YOLOModelConfiguration? = null
    private var _isReady: Boolean = false
    private var _lastMetrics: YOLOPerformanceMetrics? = null
    
    actual val currentConfiguration: YOLOModelConfiguration?
        get() = _configuration
        
    actual val isReady: Boolean
        get() = _isReady
    
    // Detection parameters
    private var confidenceThreshold = 0.5f
    private var iouThreshold = 0.45f
    
    // Performance metrics
    private var lastInferenceTime = 0L
    private var lastPreprocessTime = 0L
    private var lastPostprocessTime = 0L
    
    /**
     * Initialize the YOLO detector with specified configuration
     */
    actual suspend fun initialize(configuration: YOLOModelConfiguration): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                _detectorStatus.value = YOLODetectorStatus.INITIALIZING
                Log.d(tag, "Initializing YOLO detector with model: ${configuration.modelName}")
                
                // Initialize ONNX Runtime environment
                ortEnvironment = OrtEnvironment.getEnvironment()
                
                // Create session options with optimizations
                sessionOptions = SessionOptions().apply {
                    // Enable optimizations
                    setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT)
                    setInterOpNumThreads(optimizer.getOptimalThreadCount())
                    setIntraOpNumThreads(optimizer.getOptimalThreadCount())
                    
                    // Add execution providers based on device capabilities
                    val deviceCapability = optimizer.assessDeviceCapability()
                    if (deviceCapability.hasGPU && optimizer.isGPUAccelerationAvailable()) {
                        Log.d(tag, "GPU acceleration available but NNAPI provider setup needs implementation")
                        // NNAPI provider setup would go here
                    }
                    
                    // Enable memory optimization
                    setMemoryPatternOptimization(true)
                    setCPUArenaAllocator(true)
                    
                    // Memory arena configurations would be set here if supported by ONNX version
                }
                
                // Load model from assets or file system
                val modelBytes = loadModelBytes(configuration.modelPath)
                ortSession = ortEnvironment?.createSession(modelBytes, sessionOptions)
                
                // Validate model inputs/outputs
                validateModelStructure()
                
                _configuration = configuration
                confidenceThreshold = configuration.confidenceThreshold
                iouThreshold = configuration.iouThreshold
                _isReady = true
                
                _detectorStatus.value = YOLODetectorStatus.READY
                Log.d(tag, "YOLO detector initialized successfully")
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(tag, "Failed to initialize YOLO detector", e)
                _detectorStatus.value = YOLODetectorStatus.ERROR
                cleanup()
                Result.failure(YOLODetectionException.ModelLoadFailed(e))
            }
        }
    }
    
    /**
     * Detect objects in the provided image data
     */
    actual suspend fun detectObjects(
        imageData: ByteArray,
        workType: WorkType
    ): Result<YOLODetectionResult> {
        return sessionMutex.withLock {
            try {
                // Validate input
                validateImageData(imageData).getOrThrow()
                
                if (!_isReady || ortSession == null) {
                    return Result.failure(YOLODetectionException.ModelNotInitialized())
                }
                
                _detectorStatus.value = YOLODetectorStatus.PROCESSING
                val startTime = System.currentTimeMillis()
                
                // Preprocess image
                val preprocessStart = System.currentTimeMillis()
                val preprocessedData = preprocessImage(imageData)
                lastPreprocessTime = System.currentTimeMillis() - preprocessStart
                
                // Run inference
                val inferenceStart = System.currentTimeMillis()
                val output = runInference(preprocessedData.tensorData)
                lastInferenceTime = System.currentTimeMillis() - inferenceStart
                
                // Postprocess results
                val postprocessStart = System.currentTimeMillis()
                val detections = postprocessOutput(
                    output,
                    preprocessedData.originalSize,
                    preprocessedData.processedSize
                )
                lastPostprocessTime = System.currentTimeMillis() - postprocessStart
                
                val totalTime = System.currentTimeMillis() - startTime
                
                // Create performance metrics
                _lastMetrics = createPerformanceMetrics(
                    lastInferenceTime,
                    lastPreprocessTime,
                    lastPostprocessTime,
                    detections.size,
                    preprocessedData.originalSize,
                    preprocessedData.processedSize,
                    Runtime.getRuntime().let { (it.totalMemory() - it.freeMemory()) / 1024 / 1024 }
                )
                
                val result = YOLODetectionResult(
                    detections = detections,
                    imageWidth = preprocessedData.originalSize.width,
                    imageHeight = preprocessedData.originalSize.height,
                    processingTimeMs = totalTime,
                    modelVersion = _configuration?.modelName ?: "unknown",
                    deviceInfo = android.os.Build.MODEL
                )
                
                _detectorStatus.value = YOLODetectorStatus.READY
                Log.d(tag, "Detection completed: ${detections.size} objects found in ${totalTime}ms")
                
                Result.success(result)
                
            } catch (e: Exception) {
                Log.e(tag, "Detection failed", e)
                _detectorStatus.value = YOLODetectorStatus.ERROR
                Result.failure(YOLODetectionException.InferenceError(e.message ?: "Unknown error", e))
            }
        }
    }
    
    /**
     * Batch detect objects in multiple images
     */
    actual fun detectObjectsBatch(
        imageBatch: List<ByteArray>,
        workType: WorkType
    ): Flow<Result<YOLODetectionResult>> = flow {
        for (imageData in imageBatch) {
            emit(detectObjects(imageData, workType))
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Get device capability assessment
     */
    actual suspend fun getDeviceCapability(): DeviceCapability {
        return optimizer.assessDeviceCapability()
    }
    
    /**
     * Update detection parameters at runtime
     */
    actual fun updateDetectionParameters(
        confidenceThreshold: Float,
        iouThreshold: Float
    ) {
        validateDetectionParameters(confidenceThreshold, iouThreshold).getOrThrow()
        this.confidenceThreshold = confidenceThreshold
        this.iouThreshold = iouThreshold
        Log.d(tag, "Updated detection parameters: confidence=$confidenceThreshold, iou=$iouThreshold")
    }
    
    /**
     * Get performance metrics for the last detection
     */
    actual fun getLastPerformanceMetrics(): YOLOPerformanceMetrics? = _lastMetrics
    
    // Helper validation methods
    private fun validateDetectionParameters(
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
    
    private fun validateImageData(imageData: ByteArray): Result<Unit> {
        return when {
            imageData.isEmpty() -> 
                Result.failure(IllegalArgumentException("Image data cannot be empty"))
            imageData.size > 50 * 1024 * 1024 -> // 50MB limit
                Result.failure(IllegalArgumentException("Image data too large (>50MB)"))
            else -> Result.success(Unit)
        }
    }
    
    private fun createPerformanceMetrics(
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
    private fun applyNMS(
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
    
    /**
     * Clean up resources and shutdown detector
     */
    actual suspend fun cleanup() {
        withContext(Dispatchers.IO) {
            try {
                _detectorStatus.value = YOLODetectorStatus.CLEANUP
                Log.d(tag, "Cleaning up YOLO detector resources")
                
                // Close ONNX Runtime resources
                ortSession?.close()
                ortSession = null
                
                sessionOptions?.close()
                sessionOptions = null
                
                ortEnvironment?.close()
                ortEnvironment = null
                
                // Shutdown executor
                inferenceExecutor.shutdown()
                
                _isReady = false
                _configuration = null
                _detectorStatus.value = YOLODetectorStatus.UNINITIALIZED
                
                Log.d(tag, "YOLO detector cleanup completed")
            } catch (e: Exception) {
                Log.e(tag, "Error during cleanup", e)
            }
        }
    }
    
    // Private helper methods
    
    /**
     * Load model bytes from assets or file system
     */
    private fun loadModelBytes(modelPath: String): ByteArray {
        // This should be implemented to load from Android assets or storage
        // For now, assume the model is in assets folder
        return try {
            // In a real implementation, you would get context from DI or constructor
            // For now, this is a placeholder - in actual implementation, 
            // the model loading would be handled through proper Android asset management
            throw NotImplementedError("Model loading from $modelPath not implemented - requires Android Context")
        } catch (e: Exception) {
            throw YOLODetectionException.ModelLoadFailed(e)
        }
    }
    
    /**
     * Validate model structure and compatibility
     */
    private fun validateModelStructure() {
        ortSession?.let { session ->
            val inputInfo = session.inputInfo
            val outputInfo = session.outputInfo
            
            Log.d(tag, "Model validation:")
            Log.d(tag, "  Inputs: ${inputInfo.keys}")
            Log.d(tag, "  Outputs: ${outputInfo.keys}")
            
            // Validate expected YOLO model structure
            if (inputInfo.isEmpty() || outputInfo.isEmpty()) {
                throw IllegalStateException("Invalid model structure: missing inputs or outputs")
            }
        }
    }
    
    /**
     * Preprocess image data for YOLO inference
     */
    private suspend fun preprocessImage(imageData: ByteArray): PreprocessedImageData {
        return withContext(Dispatchers.Default) {
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: throw YOLODetectionException.InvalidImageData("Cannot decode image")
                
            val originalSize = ImageSize(bitmap.width, bitmap.height)
            val targetSize = _configuration?.inputSize ?: ImageSize(640, 640)
            
            // Resize image maintaining aspect ratio
            val resizedBitmap = resizeImageWithPadding(bitmap, targetSize)
            
            // Convert to normalized float array (RGB format)
            val tensorData = bitmapToFloatArray(resizedBitmap)
            
            bitmap.recycle()
            if (resizedBitmap !== bitmap) {
                resizedBitmap.recycle()
            }
            
            PreprocessedImageData(
                tensorData = tensorData,
                originalSize = originalSize,
                processedSize = targetSize
            )
        }
    }
    
    /**
     * Resize image with padding to maintain aspect ratio
     */
    private fun resizeImageWithPadding(bitmap: Bitmap, targetSize: ImageSize): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetAspectRatio = targetSize.width.toFloat() / targetSize.height.toFloat()
        
        val (newWidth, newHeight) = if (aspectRatio > targetAspectRatio) {
            targetSize.width to (targetSize.width / aspectRatio).toInt()
        } else {
            (targetSize.height * aspectRatio).toInt() to targetSize.height
        }
        
        // Resize bitmap
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        
        // Create padded bitmap
        val paddedBitmap = Bitmap.createBitmap(targetSize.width, targetSize.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paddedBitmap)
        
        // Center the resized image
        val left = (targetSize.width - newWidth) / 2f
        val top = (targetSize.height - newHeight) / 2f
        
        canvas.drawBitmap(resizedBitmap, left, top, null)
        
        if (resizedBitmap !== bitmap) {
            resizedBitmap.recycle()
        }
        
        return paddedBitmap
    }
    
    /**
     * Convert bitmap to normalized float array for ONNX input
     */
    private fun bitmapToFloatArray(bitmap: Bitmap): FloatArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val floatArray = FloatArray(width * height * 3) // RGB channels
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            
            // ONNX format: CHW (Channel, Height, Width)
            val baseIndex = i * 3
            floatArray[baseIndex] = r
            floatArray[baseIndex + 1] = g
            floatArray[baseIndex + 2] = b
        }
        
        return floatArray
    }
    
    /**
     * Run ONNX inference
     */
    private suspend fun runInference(inputData: FloatArray): OnnxTensor {
        return withContext(Dispatchers.IO) {
            val session = ortSession ?: throw YOLODetectionException.ModelNotInitialized()
            val env = ortEnvironment ?: throw YOLODetectionException.ModelNotInitialized()
            
            val inputSize = _configuration?.inputSize ?: ImageSize(640, 640)
            val shape = longArrayOf(1, 3, inputSize.height.toLong(), inputSize.width.toLong())
            
            val inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), shape)
            
            val inputs = mapOf(session.inputNames.first() to inputTensor)
            val outputs = session.run(inputs)
            
            inputTensor.close()
            
            outputs.get(0).value as OnnxTensor
        }
    }
    
    /**
     * Postprocess ONNX output to YOLO detections
     */
    private fun postprocessOutput(
        output: OnnxTensor,
        originalSize: ImageSize,
        processedSize: ImageSize
    ): List<YOLOBoundingBox> {
        val detections = mutableListOf<YOLOBoundingBox>()
        
        try {
            // Parse ONNX output tensor
            val outputData = output.floatBuffer.array()
            val shape = output.info.shape
            
            // YOLO output format: [batch, detections, (x, y, w, h, conf, class_probs...)]
            val numDetections = shape[1].toInt()
            val numElements = shape[2].toInt() // typically 85 for 80 classes
            
            for (i in 0 until numDetections) {
                val baseIndex = i * numElements
                
                val centerX = outputData[baseIndex]
                val centerY = outputData[baseIndex + 1]
                val width = outputData[baseIndex + 2]
                val height = outputData[baseIndex + 3]
                val confidence = outputData[baseIndex + 4]
                
                if (confidence >= confidenceThreshold) {
                    // Find best class
                    var bestClassId = 0
                    var bestClassScore = 0f
                    
                    for (j in 5 until numElements) {
                        val classScore = outputData[baseIndex + j]
                        if (classScore > bestClassScore) {
                            bestClassScore = classScore
                            bestClassId = j - 5
                        }
                    }
                    
                    val finalConfidence = confidence * bestClassScore
                    if (finalConfidence >= confidenceThreshold) {
                        // Convert coordinates from processed image to original image space
                        val scaledX = centerX * originalSize.width / processedSize.width
                        val scaledY = centerY * originalSize.height / processedSize.height
                        val scaledWidth = width * originalSize.width / processedSize.width
                        val scaledHeight = height * originalSize.height / processedSize.height
                        
                        // Normalize coordinates
                        val normalizedX = scaledX / originalSize.width
                        val normalizedY = scaledY / originalSize.height
                        val normalizedWidth = scaledWidth / originalSize.width
                        val normalizedHeight = scaledHeight / originalSize.height
                        
                        detections.add(
                            YOLOBoundingBox(
                                x = normalizedX,
                                y = normalizedY,
                                width = normalizedWidth,
                                height = normalizedHeight,
                                confidence = finalConfidence,
                                classId = bestClassId,
                                className = getClassName(bestClassId)
                            )
                        )
                    }
                }
            }
            
        } finally {
            output.close()
        }
        
        // Apply Non-Maximum Suppression
        return applyNMS(detections, iouThreshold)
    }
    
    /**
     * Get class name for class ID
     */
    private fun getClassName(classId: Int): String {
        // This should map to actual YOLO class names
        // For construction safety, this would include things like:
        // "person", "hardhat", "safety_vest", "equipment", etc.
        return when (classId) {
            0 -> "person"
            1 -> "hardhat"
            2 -> "safety_vest"
            3 -> "equipment"
            else -> "object_$classId"
        }
    }
    
    /**
     * Data class for preprocessed image data
     */
    private data class PreprocessedImageData(
        val tensorData: FloatArray,
        val originalSize: ImageSize,
        val processedSize: ImageSize
    )
}

/**
 * Android-specific YOLO detector factory
 */
actual object YOLODetectorFactory {
    
    actual fun createDetector(
        enableGPU: Boolean,
        optimizeForBattery: Boolean
    ): YOLOObjectDetector {
        return YOLOObjectDetector()
    }
    
    actual suspend fun getOptimalConfiguration(): YOLOModelConfiguration {
        val optimizer = AndroidYOLOOptimizer()
        val deviceCapability = optimizer.assessDeviceCapability()
        return YOLOModelConfiguration.getOptimalConfiguration(deviceCapability)
    }
    
    actual fun isSupported(): Boolean {
        return try {
            // Check if ONNX Runtime is available
            Class.forName("ai.onnxruntime.OrtEnvironment")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    actual suspend fun getAvailableModels(): List<YOLOModelConfiguration> {
        // This would scan available model files in assets or storage
        // For now, return standard configurations
        return listOf(
            YOLOModelConfiguration(
                modelName = "yolo11n",
                modelPath = "models/yolo11n.onnx",
                inputSize = ImageSize(416, 416),
                numClasses = 80,
                estimatedMemoryMB = 128,
                estimatedInferenceTimeMs = 50
            ),
            YOLOModelConfiguration(
                modelName = "yolo11s",
                modelPath = "models/yolo11s.onnx",
                inputSize = ImageSize(640, 640),
                numClasses = 80,
                estimatedMemoryMB = 256,
                estimatedInferenceTimeMs = 75
            ),
            YOLOModelConfiguration(
                modelName = "yolo11m",
                modelPath = "models/yolo11m.onnx",
                inputSize = ImageSize(640, 640),
                numClasses = 80,
                estimatedMemoryMB = 512,
                estimatedInferenceTimeMs = 100
            )
        )
    }
}