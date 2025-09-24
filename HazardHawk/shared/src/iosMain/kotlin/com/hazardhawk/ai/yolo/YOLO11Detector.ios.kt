@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.hazardhawk.ai.yolo

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
import platform.CoreML.*
import platform.Vision.*
import platform.Foundation.*
import platform.UIKit.*
import com.hazardhawk.domain.entities.WorkType

/**
 * iOS-specific YOLO11 object detector implementation using Core ML.
 * Provides high-performance construction hazard detection with Neural Engine acceleration
 * and optimized memory management for iOS devices.
 */
actual class YOLOObjectDetector {
    
    companion object {
        private const val TAG = "YOLO11DetectorIOS"
    }
    
    // Core ML components
    private var coreMLModel: MLModel? = null
    private var vnCoreMLModel: VNCoreMLModel? = null
    private var imageRequestHandler: VNImageRequestHandler? = null
    
    // State management
    private val _detectorStatus = MutableStateFlow(YOLODetectorStatus.UNINITIALIZED)
    actual val detectorStatus: StateFlow<YOLODetectorStatus> = _detectorStatus.asStateFlow()
    
    private val sessionMutex = Mutex()
    private val optimizer = IOSYOLOOptimizer()
    
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
    
    // Core ML optimization settings
    private var computeUnits: MLComputeUnits = MLComputeUnitsAll
    private var modelConfiguration: MLModelConfiguration? = null
    
    /**
     * Initialize the YOLO detector with specified configuration
     */
    actual suspend fun initialize(configuration: YOLOModelConfiguration): Result<Unit> {
        return withContext(Dispatchers.Main) {
            try {
                _detectorStatus.value = YOLODetectorStatus.INITIALIZING
                NSLog("$TAG: Initializing YOLO detector with model: ${configuration.modelName}")
                
                // Assess device capability for optimal configuration
                val deviceCapability = optimizer.assessDeviceCapability()
                
                // Create model configuration with optimal compute units
                modelConfiguration = MLModelConfiguration().apply {
                    computeUnits = selectOptimalComputeUnits(deviceCapability)
                    setAllowLowPrecisionAccumulationOnGPU(true)
                }
                
                NSLog("$TAG: Using compute units: ${computeUnitsToString(computeUnits)}")
                
                // Load Core ML model
                val modelURL = getModelURL(configuration.modelPath)
                coreMLModel = modelConfiguration?.let { config ->
                    MLModel.modelWithContentsOfURL(modelURL, configuration = config, error = null)
                } ?: MLModel.modelWithContentsOfURL(modelURL, error = null)
                
                if (coreMLModel == null) {
                    throw YOLODetectionException.ModelLoadFailed(
                        RuntimeException("Failed to load Core ML model from ${configuration.modelPath}")
                    )
                }
                
                // Create VNCoreMLModel for Vision framework integration
                vnCoreMLModel = VNCoreMLModel.modelForMLModel(coreMLModel!!, error = null)
                if (vnCoreMLModel == null) {
                    throw YOLODetectionException.ModelLoadFailed(
                        RuntimeException("Failed to create VNCoreMLModel")
                    )
                }
                
                // Validate model structure
                validateModelStructure()
                
                _configuration = configuration
                confidenceThreshold = configuration.confidenceThreshold
                iouThreshold = configuration.iouThreshold
                _isReady = true
                
                _detectorStatus.value = YOLODetectorStatus.READY
                NSLog("$TAG: YOLO detector initialized successfully")
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                NSLog("$TAG: Failed to initialize YOLO detector: ${e.message}")
                _detectorStatus.value = YOLODetectorStatus.ERROR
                cleanup()
                Result.failure(when (e) {
                    is YOLODetectionException -> e
                    else -> YOLODetectionException.ModelLoadFailed(e)
                })
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
                
                if (!_isReady || vnCoreMLModel == null) {
                    return Result.failure(YOLODetectionException.ModelNotInitialized())
                }
                
                _detectorStatus.value = YOLODetectorStatus.PROCESSING
                val startTime = NSDate().timeIntervalSince1970 * 1000
                
                // Preprocess image
                val preprocessStart = NSDate().timeIntervalSince1970 * 1000
                val preprocessedData = preprocessImage(imageData)
                lastPreprocessTime = ((NSDate().timeIntervalSince1970 * 1000) - preprocessStart).toLong()
                
                // Run inference using Vision framework
                val inferenceStart = NSDate().timeIntervalSince1970 * 1000
                val detections = runCoreMLInference(preprocessedData)
                lastInferenceTime = ((NSDate().timeIntervalSince1970 * 1000) - inferenceStart).toLong()
                
                // Postprocess results
                val postprocessStart = NSDate().timeIntervalSince1970 * 1000
                val processedDetections = postprocessDetections(
                    detections,
                    preprocessedData.originalSize,
                    preprocessedData.processedSize
                )
                lastPostprocessTime = ((NSDate().timeIntervalSince1970 * 1000) - postprocessStart).toLong()
                
                val totalTime = ((NSDate().timeIntervalSince1970 * 1000) - startTime).toLong()
                
                // Create performance metrics
                _lastMetrics = createPerformanceMetrics(
                    lastInferenceTime,
                    lastPreprocessTime,
                    lastPostprocessTime,
                    processedDetections.size,
                    preprocessedData.originalSize,
                    preprocessedData.processedSize,
                    getMemoryUsageMB()
                )
                
                val result = YOLODetectionResult(
                    detections = processedDetections,
                    imageWidth = preprocessedData.originalSize.width,
                    imageHeight = preprocessedData.originalSize.height,
                    processingTimeMs = totalTime,
                    modelVersion = _configuration?.modelName ?: "unknown",
                    deviceInfo = UIDevice.currentDevice.model
                )
                
                _detectorStatus.value = YOLODetectorStatus.READY
                NSLog("$TAG: Detection completed: ${processedDetections.size} objects found in ${totalTime}ms")
                
                Result.success(result)
                
            } catch (e: Exception) {
                NSLog("$TAG: Detection failed: ${e.message}")
                _detectorStatus.value = YOLODetectorStatus.ERROR
                Result.failure(when (e) {
                    is YOLODetectionException -> e
                    else -> YOLODetectionException.InferenceError(e.message ?: "Unknown error", e)
                })
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
    }.flowOn(Dispatchers.Default)
    
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
        NSLog("$TAG: Updated detection parameters: confidence=$confidenceThreshold, iou=$iouThreshold")
    }
    
    /**
     * Get performance metrics for the last detection
     */
    actual fun getLastPerformanceMetrics(): YOLOPerformanceMetrics? = _lastMetrics
    
    /**
     * Clean up resources and shutdown detector
     */
    actual suspend fun cleanup() {
        withContext(Dispatchers.Main) {
            try {
                _detectorStatus.value = YOLODetectorStatus.CLEANUP
                NSLog("$TAG: Cleaning up YOLO detector resources")
                
                // Release Core ML resources
                coreMLModel = null
                vnCoreMLModel = null
                imageRequestHandler = null
                modelConfiguration = null
                
                _isReady = false
                _configuration = null
                _detectorStatus.value = YOLODetectorStatus.UNINITIALIZED
                
                NSLog("$TAG: YOLO detector cleanup completed")
            } catch (e: Exception) {
                NSLog("$TAG: Error during cleanup: ${e.message}")
            }
        }
    }
    
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
    
    // Private iOS-specific helper methods
    
    private fun selectOptimalComputeUnits(deviceCapability: DeviceCapability): MLComputeUnits {
        return when {
            // Use Neural Engine for high-end devices (A12+)
            optimizer.hasNeuralEngine() -> {
                NSLog("$TAG: Using Neural Engine for optimal performance")
                MLComputeUnits.MLComputeUnitsNeuralEngine
            }
            // Use GPU for medium performance devices
            deviceCapability.hasGPU && deviceCapability.performanceLevel != PerformanceLevel.LOW -> {
                NSLog("$TAG: Using GPU for accelerated inference")
                MLComputeUnits.MLComputeUnitsGPU
            }
            // Fallback to CPU for low-end devices
            else -> {
                NSLog("$TAG: Using CPU for inference")
                MLComputeUnits.MLComputeUnitsCPUOnly
            }
        }
    }
    
    private fun computeUnitsToString(units: MLComputeUnits): String {
        return when (units) {
            MLComputeUnits.MLComputeUnitsCPUOnly -> "CPU Only"
            MLComputeUnits.MLComputeUnitsGPU -> "GPU" 
            MLComputeUnits.MLComputeUnitsNeuralEngine -> "Neural Engine"
            MLComputeUnits.MLComputeUnitsAll -> "All Available"
            else -> "Unknown"
        }
    }
    
    private fun getModelURL(modelPath: String): NSURL {
        // Try to load from main bundle first
        val bundle = NSBundle.mainBundle
        val modelName = modelPath.substringAfterLast("/").substringBeforeLast(".")
        val modelExtension = modelPath.substringAfterLast(".")
        
        val bundleURL = bundle.URLForResource(modelName, modelExtension)
        if (bundleURL != null) {
            return bundleURL
        }
        
        // Fallback to documents directory
        val documentsPath = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory,
            NSUserDomainMask,
            true
        ).first() as String
        
        val fullPath = "$documentsPath/$modelPath"
        return NSURL.fileURLWithPath(fullPath)
    }
    
    private fun validateModelStructure() {
        coreMLModel?.let { model ->
            val modelDescription = model.modelDescription
            val inputDescriptions = modelDescription.inputDescriptionsByName
            val outputDescriptions = modelDescription.outputDescriptionsByName
            
            NSLog("$TAG: Model validation:")
            NSLog("$TAG:   Inputs: ${inputDescriptions.keys}")
            NSLog("$TAG:   Outputs: ${outputDescriptions.keys}")
            
            if (inputDescriptions.isEmpty() || outputDescriptions.isEmpty()) {
                throw IllegalStateException("Invalid model structure: missing inputs or outputs")
            }
        }
    }
    
    private suspend fun preprocessImage(imageData: ByteArray): PreprocessedImageData {
        return withContext(Dispatchers.Default) {
            val nsData = NSData.create(imageData.toUByteArray().toCValues(), imageData.size.toULong())
            val uiImage = UIImage.imageWithData(nsData)
                ?: throw YOLODetectionException.InvalidImageData("Cannot decode image")
            
            val originalSize = ImageSize(
                width = uiImage.size.useContents { width.toInt() },
                height = uiImage.size.useContents { height.toInt() }
            )
            
            val targetSize = _configuration?.inputSize ?: ImageSize(640, 640)
            
            // Resize image maintaining aspect ratio with padding
            val resizedImage = resizeImageWithPadding(uiImage, targetSize)
            
            PreprocessedImageData(
                image = resizedImage,
                originalSize = originalSize,
                processedSize = targetSize
            )
        }
    }
    
    private fun resizeImageWithPadding(image: UIImage, targetSize: ImageSize): UIImage {
        val imageSize = image.size.useContents { 
            CGSizeMake(width, height)
        }
        
        val aspectRatio = imageSize.useContents { width / height }
        val targetAspectRatio = targetSize.width.toDouble() / targetSize.height.toDouble()
        
        val (newWidth, newHeight) = if (aspectRatio > targetAspectRatio) {
            targetSize.width.toDouble() to (targetSize.width.toDouble() / aspectRatio)
        } else {
            (targetSize.height.toDouble() * aspectRatio) to targetSize.height.toDouble()
        }
        
        // Create graphics context for resizing
        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(targetSize.width.toDouble(), targetSize.height.toDouble()),
            false,
            0.0
        )
        
        // Center the image
        val x = (targetSize.width.toDouble() - newWidth) / 2.0
        val y = (targetSize.height.toDouble() - newHeight) / 2.0
        
        image.drawInRect(CGRectMake(x, y, newWidth, newHeight))
        
        val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return resizedImage ?: image
    }
    
    private suspend fun runCoreMLInference(preprocessedData: PreprocessedImageData): List<YOLOBoundingBox> {
        return withContext(Dispatchers.Default) {
            val detections = mutableListOf<YOLOBoundingBox>()
            
            // Create Vision request
            val request = VNCoreMLRequest(vnCoreMLModel!!) { request, error ->
                if (error != null) {
                    NSLog("$TAG: Vision request error: ${error.localizedDescription}")
                    return@VNCoreMLRequest
                }
                
                // Process results
                request.results?.forEach { result ->
                    when (result) {
                        is VNCoreMLFeatureValueObservation -> {
                            processFeatureValueObservation(
                                result, 
                                preprocessedData,
                                detections
                            )
                        }
                        is VNRecognizedObjectObservation -> {
                            processRecognizedObjectObservation(
                                result,
                                preprocessedData,
                                detections
                            )
                        }
                    }
                }
            }
            
            // Configure request
            request.imageCropAndScaleOption = VNImageCropAndScaleOptionScaleFill
            
            // Create image request handler
            val cgImage = preprocessedData.image.CGImage
            if (cgImage != null) {
                imageRequestHandler = VNImageRequestHandler(cgImage, options = emptyMap<Any?, Any?>())
                
                // Perform request
                val requestError: NSError? = null
                imageRequestHandler?.performRequests(listOf(request), requestError)
                
                if (requestError != null) {
                    throw YOLODetectionException.InferenceError(
                        "Vision request failed: ${requestError.localizedDescription}"
                    )
                }
            } else {
                throw YOLODetectionException.InvalidImageData("Cannot get CGImage from UIImage")
            }
            
            detections
        }
    }
    
    private fun processFeatureValueObservation(
        observation: VNCoreMLFeatureValueObservation,
        preprocessedData: PreprocessedImageData,
        detections: MutableList<YOLOBoundingBox>
    ) {
        val featureValue = observation.featureValue
        
        // Handle MLMultiArray output (typical for YOLO models)
        featureValue.multiArrayValue?.let { multiArray ->
            processMultiArrayOutput(multiArray, preprocessedData, detections)
        }
    }
    
    private fun processRecognizedObjectObservation(
        observation: VNRecognizedObjectObservation,
        preprocessedData: PreprocessedImageData,
        detections: MutableList<YOLOBoundingBox>
    ) {
        // Convert Vision framework bounding box to YOLO format
        val boundingBox = observation.boundingBox.useContents {
            YOLOBoundingBox(
                x = (origin.x + size.width / 2).toFloat(),
                y = 1.0f - (origin.y + size.height / 2).toFloat(), // Flip Y coordinate
                width = size.width.toFloat(),
                height = size.height.toFloat(),
                confidence = observation.confidence,
                classId = 0, // Would need proper class mapping
                className = observation.labels.firstOrNull()?.identifier ?: "unknown"
            )
        }
        
        if (boundingBox.confidence >= confidenceThreshold) {
            detections.add(boundingBox)
        }
    }
    
    private fun processMultiArrayOutput(
        multiArray: MLMultiArray,
        preprocessedData: PreprocessedImageData,
        detections: MutableList<YOLOBoundingBox>
    ) {
        val shape = multiArray.shape
        val dataPointer = multiArray.dataPointer
        
        // YOLO output format: [batch, detections, (x, y, w, h, conf, class_probs...)]
        val numDetections = shape[1]?.intValue ?: 0
        val numElements = shape[2]?.intValue ?: 85 // typically 85 for 80 classes
        
        for (i in 0 until numDetections) {
            val baseIndex = i * numElements
            
            // Extract detection data
            val centerX = multiArray.objectAtIndex(baseIndex)?.floatValue ?: 0f
            val centerY = multiArray.objectAtIndex(baseIndex + 1)?.floatValue ?: 0f
            val width = multiArray.objectAtIndex(baseIndex + 2)?.floatValue ?: 0f
            val height = multiArray.objectAtIndex(baseIndex + 3)?.floatValue ?: 0f
            val confidence = multiArray.objectAtIndex(baseIndex + 4)?.floatValue ?: 0f
            
            if (confidence >= confidenceThreshold) {
                // Find best class
                var bestClassId = 0
                var bestClassScore = 0f
                
                for (j in 5 until numElements) {
                    val classScore = multiArray.objectAtIndex(baseIndex + j)?.floatValue ?: 0f
                    if (classScore > bestClassScore) {
                        bestClassScore = classScore
                        bestClassId = j - 5
                    }
                }
                
                val finalConfidence = confidence * bestClassScore
                if (finalConfidence >= confidenceThreshold) {
                    detections.add(
                        YOLOBoundingBox(
                            x = centerX / preprocessedData.processedSize.width,
                            y = centerY / preprocessedData.processedSize.height,
                            width = width / preprocessedData.processedSize.width,
                            height = height / preprocessedData.processedSize.height,
                            confidence = finalConfidence,
                            classId = bestClassId,
                            className = getClassName(bestClassId)
                        )
                    )
                }
            }
        }
    }
    
    private fun postprocessDetections(
        detections: List<YOLOBoundingBox>,
        originalSize: ImageSize,
        processedSize: ImageSize
    ): List<YOLOBoundingBox> {
        // Scale detections back to original image coordinates
        val scaledDetections = detections.map { detection ->
            detection.copy(
                x = detection.x * originalSize.width / processedSize.width,
                y = detection.y * originalSize.height / processedSize.height,
                width = detection.width * originalSize.width / processedSize.width,
                height = detection.height * originalSize.height / processedSize.height
            )
        }
        
        // Apply Non-Maximum Suppression
        return applyNMS(scaledDetections, iouThreshold)
    }
    
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
    
    private fun getClassName(classId: Int): String {
        // This should map to actual YOLO class names for construction safety
        return when (classId) {
            0 -> "person"
            1 -> "hardhat"
            2 -> "safety_vest"
            3 -> "equipment"
            else -> "object_$classId"
        }
    }
    
    private fun getMemoryUsageMB(): Long {
        // Get current memory usage using iOS APIs
        return autoreleasepool {
            val info = mach_task_basic_info_data_t()
            var count = MACH_TASK_BASIC_INFO_COUNT
            val result = task_info(
                mach_task_self(),
                MACH_TASK_BASIC_INFO.toInt(),
                info.ptr,
                count.ptr
            )
            
            if (result == KERN_SUCCESS) {
                info.resident_size.toLong() / 1024 / 1024
            } else {
                0L
            }
        }
    }
    
    /**
     * Data class for preprocessed image data
     */
    private data class PreprocessedImageData(
        val image: UIImage,
        val originalSize: ImageSize,
        val processedSize: ImageSize
    )
}

/**
 * iOS-specific YOLO detector factory
 */
actual object YOLODetectorFactory {
    
    actual fun createDetector(
        enableGPU: Boolean,
        optimizeForBattery: Boolean
    ): YOLOObjectDetector {
        return YOLOObjectDetector()
    }
    
    actual suspend fun getOptimalConfiguration(): YOLOModelConfiguration {
        val optimizer = IOSYOLOOptimizer()
        val deviceCapability = optimizer.assessDeviceCapability()
        return YOLOModelConfiguration.getOptimalConfiguration(deviceCapability)
    }
    
    actual fun isSupported(): Boolean {
        // Core ML is available on iOS 11+
        return NSProcessInfo.processInfo.operatingSystemVersion.majorVersion >= 11
    }
    
    actual suspend fun getAvailableModels(): List<YOLOModelConfiguration> {
        // This would scan available model files in app bundle or documents
        return listOf(
            YOLOModelConfiguration(
                modelName = "yolo11n",
                modelPath = "models/yolo11n.mlmodel",
                inputSize = ImageSize(416, 416),
                numClasses = 80,
                estimatedMemoryMB = 128,
                estimatedInferenceTimeMs = 30 // Neural Engine optimized
            ),
            YOLOModelConfiguration(
                modelName = "yolo11s",
                modelPath = "models/yolo11s.mlmodel",
                inputSize = ImageSize(640, 640),
                numClasses = 80,
                estimatedMemoryMB = 256,
                estimatedInferenceTimeMs = 50
            ),
            YOLOModelConfiguration(
                modelName = "yolo11m",
                modelPath = "models/yolo11m.mlmodel",
                inputSize = ImageSize(640, 640),
                numClasses = 80,
                estimatedMemoryMB = 512,
                estimatedInferenceTimeMs = 80
            )
        )
    }
}