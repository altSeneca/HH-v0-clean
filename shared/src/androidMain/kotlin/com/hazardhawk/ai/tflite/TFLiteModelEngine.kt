package com.hazardhawk.ai.tflite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import com.hazardhawk.ai.litert.*
import com.hazardhawk.core.models.WorkType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.common.ops.QuantizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp

/**
 * TensorFlow Lite implementation of the model engine for construction safety analysis.
 * Provides hardware-accelerated on-device AI processing using TFLite.
 * 
 * Features:
 * - GPU acceleration with GPU delegate
 * - NNAPI acceleration for supported devices  
 * - CPU fallback for universal compatibility
 * - Construction-focused safety analysis
 * - Memory-efficient processing with thermal protection
 */
class TFLiteModelEngine {
    
    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var nnApiDelegate: NnApiDelegate? = null
    private var selectedBackend: LiteRTBackend = LiteRTBackend.AUTO
    private var modelBuffer: MappedByteBuffer? = null
    private var isInitialized = false
    
    // Image processing
    private lateinit var imageProcessor: ImageProcessor
    private val inputImageWidth = 224
    private val inputImageHeight = 224
    private val pixelSize = 3 // RGB
    
    // Performance tracking
    private var analysisCount = 0L
    private var totalProcessingTime = 0L
    private var successCount = 0L
    private var peakMemoryUsage = 0f
    private var averageMemoryUsage = 0f
    
    val isAvailable: Boolean
        get() = isInitialized && interpreter != null
    
    val supportedBackends: Set<LiteRTBackend>
        get() = detectSupportedBackends()
    
    val currentBackend: LiteRTBackend?
        get() = if (isAvailable) selectedBackend else null
    
    /**
     * Initialize TFLite engine with construction safety model and optimal backend.
     */
    suspend fun initialize(
        context: Context,
        modelPath: String,
        backend: LiteRTBackend = LiteRTBackend.AUTO
    ): Result<Unit> = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "Initializing TFLite engine with backend: $backend")
            
            // Select actual backend (resolve AUTO to specific backend)
            val resolvedBackend = when (backend) {
                LiteRTBackend.AUTO -> selectOptimalBackend()
                else -> backend
            }
            
            // Validate backend is supported
            if (resolvedBackend !in supportedBackends) {
                return@withContext Result.failure(
                    LiteRTException.UnsupportedBackendException(resolvedBackend)
                )
            }
            
            // Load model from assets
            modelBuffer = loadModelFile(context, modelPath)
            
            // Create interpreter options for the selected backend
            val options = Interpreter.Options().apply {
                when (resolvedBackend) {
                    LiteRTBackend.GPU -> {
                        try {
                            gpuDelegate = GpuDelegate()
                            addDelegate(gpuDelegate)
                            Log.d(TAG, "GPU delegate enabled")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to create GPU delegate, falling back to CPU", e)
                        }
                    }
                    LiteRTBackend.NNAPI -> {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                nnApiDelegate = NnApiDelegate()
                                addDelegate(nnApiDelegate)
                                Log.d(TAG, "NNAPI delegate enabled")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to create NNAPI delegate, falling back to CPU", e)
                        }
                    }
                    LiteRTBackend.CPU -> {
                        // Use CPU only
                        setNumThreads(4)
                        Log.d(TAG, "CPU backend selected")
                    }
                    else -> {
                        // AUTO case handled above
                        setNumThreads(4)
                    }
                }
                
                // Performance options
                setUseNNAPI(resolvedBackend == LiteRTBackend.NNAPI)
                setAllowFp16PrecisionForFp32(true)
                setAllowBufferHandleOutput(true)
            }
            
            // Initialize TFLite interpreter
            interpreter = Interpreter(modelBuffer!!, options)
            
            // Initialize image processor
            imageProcessor = ImageProcessor.Builder()
                .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                .add(NormalizeOp(MEAN_RGB, STDDEV_RGB))
                .build()
            
            selectedBackend = resolvedBackend
            isInitialized = true
            
            Log.i(TAG, "TFLite engine initialized successfully with $selectedBackend backend")
            Result.success(Unit)
            
        } catch (e: OutOfMemoryError) {
            val availableMemory = getAvailableMemoryMB()
            Result.failure(
                LiteRTException.OutOfMemoryException(
                    requiredMB = 500f, // Estimate
                    availableMB = availableMemory
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "TFLite initialization failed", e)
            Result.failure(
                LiteRTException.InitializationException(
                    "Failed to initialize with $backend: ${e.message}",
                    e
                )
            )
        }
    }
    
    /**
     * Generate comprehensive construction safety analysis using TFLite model.
     */
    suspend fun generateSafetyAnalysis(
        imageData: ByteArray,
        workType: WorkType,
        includeOSHACodes: Boolean = true,
        confidenceThreshold: Float = 0.6f
    ): Result<LiteRTAnalysisResult> = withContext(Dispatchers.Default) {
        
        val currentInterpreter = interpreter
            ?: return@withContext Result.failure(
                LiteRTException.InferenceException("TFLite interpreter not initialized")
            )
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Check thermal state before processing
            val thermalState = getCurrentThermalState()
            if (thermalState >= THERMAL_STATE_SEVERE) {
                return@withContext Result.failure(
                    LiteRTException.ThermalThrottlingException(
                        getDeviceTemperature()
                    )
                )
            }
            
            // Track memory usage
            val memoryBefore = getCurrentMemoryUsageMB()
            
            // Preprocess image for model input
            val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                ?: return@withContext Result.failure(
                    LiteRTException.InferenceException("Failed to decode image")
                )
            
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processedImage = imageProcessor.process(tensorImage)
            
            // Prepare input and output tensors
            val inputBuffer = processedImage.buffer
            val outputBuffer = Array(1) { FloatArray(getOutputSize()) }
            
            // Run inference
            currentInterpreter.run(inputBuffer, outputBuffer)
            
            val processingTime = System.currentTimeMillis() - startTime
            
            // Post-process results
            val analysisResult = postProcessResults(
                outputBuffer[0],
                workType,
                confidenceThreshold,
                includeOSHACodes,
                processingTime
            )
            
            // Update performance metrics
            updatePerformanceMetrics(processingTime, getCurrentMemoryUsageMB() - memoryBefore)
            
            Log.d(TAG, "Safety analysis completed in ${processingTime}ms using $selectedBackend")
            Result.success(analysisResult)
            
        } catch (e: Exception) {
            Log.e(TAG, "Safety analysis failed", e)
            Result.failure(
                LiteRTException.InferenceException("Analysis failed: ${e.message}", e)
            )
        }
    }
    
    /**
     * Get current performance metrics for monitoring.
     */
    fun getPerformanceMetrics(): LiteRTPerformanceMetrics {
        return LiteRTPerformanceMetrics(
            backendUsed = selectedBackend,
            totalInferences = analysisCount,
            averageInferenceTime = if (analysisCount > 0) totalProcessingTime / analysisCount else 0L,
            successRate = if (analysisCount > 0) (successCount.toFloat() / analysisCount) * 100f else 0f,
            peakMemoryUsage = peakMemoryUsage,
            averageMemoryUsage = averageMemoryUsage,
            deviceTemperature = getDeviceTemperature()
        )
    }
    
    /**
     * Clean up resources and release model memory.
     */
    fun cleanup() {
        try {
            interpreter?.close()
            gpuDelegate?.close()
            nnApiDelegate?.close()
            
            interpreter = null
            gpuDelegate = null
            nnApiDelegate = null
            modelBuffer = null
            isInitialized = false
            
            Log.d(TAG, "TFLite engine cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    // Private helper methods
    
    private fun detectSupportedBackends(): Set<LiteRTBackend> {
        val supported = mutableSetOf(LiteRTBackend.CPU, LiteRTBackend.AUTO)
        
        // Check GPU support
        try {
            val testDelegate = GpuDelegate()
            testDelegate.close()
            supported.add(LiteRTBackend.GPU)
        } catch (e: Exception) {
            Log.d(TAG, "GPU delegate not supported: ${e.message}")
        }
        
        // Check NNAPI support (Android 8.1+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val testDelegate = NnApiDelegate()
                testDelegate.close()
                supported.add(LiteRTBackend.NNAPI)
            } catch (e: Exception) {
                Log.d(TAG, "NNAPI delegate not supported: ${e.message}")
            }
        }
        
        return supported
    }
    
    private fun selectOptimalBackend(): LiteRTBackend {
        val supported = supportedBackends
        
        return when {
            LiteRTBackend.GPU in supported -> LiteRTBackend.GPU
            LiteRTBackend.NNAPI in supported -> LiteRTBackend.NNAPI
            else -> LiteRTBackend.CPU
        }
    }
    
    private suspend fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        return withContext(Dispatchers.IO) {
            try {
                val assetFileDescriptor = context.assets.openFd(modelPath)
                val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
                val fileChannel = inputStream.channel
                val startOffset = assetFileDescriptor.startOffset
                val declaredLength = assetFileDescriptor.declaredLength
                
                fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            } catch (e: IOException) {
                throw LiteRTException.InitializationException("Failed to load model: $modelPath", e)
            }
        }
    }
    
    private fun getOutputSize(): Int {
        // This should match your model's output size
        // For construction safety analysis, this might be:
        // - Number of hazard classes + PPE detection + OSHA codes
        return 100 // Placeholder - adjust based on actual model
    }
    
    private fun postProcessResults(
        outputArray: FloatArray,
        workType: WorkType,
        confidenceThreshold: Float,
        includeOSHACodes: Boolean,
        processingTime: Long
    ): LiteRTAnalysisResult {
        
        // This is a simplified implementation - replace with actual post-processing logic
        val detectedHazards = mutableListOf<LiteRTHazardDetection>()
        val ppeDetections = mutableListOf<LiteRTPPEDetection>()
        val oshaViolations = if (includeOSHACodes) mutableListOf<LiteRTOSHAViolation>() else null
        
        // Process output array and extract detections
        // This is placeholder logic - implement based on your model's output format
        for (i in outputArray.indices) {
            val confidence = sigmoid(outputArray[i])
            if (confidence > confidenceThreshold) {
                // Map index to hazard type, PPE, or OSHA code
                // This mapping should be based on your model's training labels
            }
        }
        
        return LiteRTAnalysisResult(
            processingTimeMs = processingTime,
            backendUsed = selectedBackend,
            confidenceThreshold = confidenceThreshold,
            detectedHazards = detectedHazards,
            ppeDetections = ppeDetections,
            oshaViolations = oshaViolations,
            overallRiskLevel = calculateOverallRisk(detectedHazards),
            recommendations = generateRecommendations(detectedHazards, ppeDetections, workType)
        )
    }
    
    private fun sigmoid(x: Float): Float = 1f / (1f + exp(-x))
    
    private fun calculateOverallRisk(hazards: List<LiteRTHazardDetection>): String {
        return when {
            hazards.any { it.severity == "HIGH" } -> "HIGH"
            hazards.any { it.severity == "MEDIUM" } -> "MEDIUM"
            hazards.isNotEmpty() -> "LOW"
            else -> "MINIMAL"
        }
    }
    
    private fun generateRecommendations(
        hazards: List<LiteRTHazardDetection>,
        ppe: List<LiteRTPPEDetection>,
        workType: WorkType
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Add work-type specific recommendations
        // Add hazard-specific recommendations
        // Add PPE recommendations
        
        return recommendations
    }
    
    private fun updatePerformanceMetrics(processingTime: Long, memoryUsed: Float) {
        analysisCount++
        totalProcessingTime += processingTime
        successCount++
        
        if (memoryUsed > peakMemoryUsage) {
            peakMemoryUsage = memoryUsed
        }
        
        averageMemoryUsage = ((averageMemoryUsage * (analysisCount - 1)) + memoryUsed) / analysisCount
    }
    
    private fun getCurrentThermalState(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Get thermal state from PowerManager
            0 // Placeholder
        } else {
            0
        }
    }
    
    private fun getDeviceTemperature(): Float {
        return 25.0f // Placeholder
    }
    
    private fun getCurrentMemoryUsageMB(): Float {
        val runtime = Runtime.getRuntime()
        val usedMemoryBytes = runtime.totalMemory() - runtime.freeMemory()
        return usedMemoryBytes / (1024f * 1024f)
    }
    
    private fun getAvailableMemoryMB(): Float {
        val runtime = Runtime.getRuntime()
        return runtime.maxMemory() / (1024f * 1024f)
    }
    
    companion object {
        private const val TAG = "TFLiteModelEngine"
        private const val THERMAL_STATE_SEVERE = 4
        
        // Image preprocessing constants
        private val MEAN_RGB = floatArrayOf(127.5f, 127.5f, 127.5f)
        private val STDDEV_RGB = floatArrayOf(127.5f, 127.5f, 127.5f)
    }
}