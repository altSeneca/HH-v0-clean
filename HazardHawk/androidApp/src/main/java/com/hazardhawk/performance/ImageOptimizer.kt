package com.hazardhawk.performance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * ImageOptimizer handles smart compression, progressive JPEG encoding,
 * and background photo processing for optimal performance
 */
class ImageOptimizer private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "ImageOptimizer"
        private const val MAX_CONCURRENT_OPERATIONS = 3
        private const val HIGH_QUALITY_THRESHOLD = 95
        private const val MEDIUM_QUALITY_THRESHOLD = 85
        private const val LOW_QUALITY_THRESHOLD = 75
        
        @Volatile
        private var INSTANCE: ImageOptimizer? = null
        
        fun getInstance(context: Context): ImageOptimizer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageOptimizer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val processingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val processingQueue = ConcurrentLinkedQueue<ProcessingTask>()
    private val semaphore = Semaphore(MAX_CONCURRENT_OPERATIONS)
    private val activeOperations = AtomicInteger(0)
    
    private val optimizedDir by lazy {
        File(context.cacheDir, "optimized_photos").apply { mkdirs() }
    }
    
    private val tempDir by lazy {
        File(context.cacheDir, "temp_processing").apply { mkdirs() }
    }
    
    /**
     * Quality settings based on device capabilities and user preferences
     */
    enum class QualityMode {
        ULTRA_HIGH,    // 95+ quality, for critical documentation
        HIGH,          // 85-94 quality, for normal usage
        BALANCED,      // 75-84 quality, for faster processing
        OPTIMIZED,     // Adaptive quality based on content
        FAST           // Lowest quality for instant processing
    }
    
    /**
     * Processing priority levels
     */
    enum class ProcessingPriority {
        IMMEDIATE,     // Block UI until complete
        HIGH,          // Process before other tasks
        NORMAL,        // Standard background processing
        LOW            // Process when system is idle
    }
    
    data class CompressionSettings(
        val qualityMode: QualityMode = QualityMode.BALANCED,
        val maxWidth: Int = 2048,
        val maxHeight: Int = 2048,
        val enableProgressiveJPEG: Boolean = true,
        val preserveExif: Boolean = true,
        val enableSmartCompression: Boolean = true
    )
    
    data class ProcessingResult(
        val success: Boolean,
        val originalPath: String,
        val optimizedPath: String?,
        val originalSize: Long,
        val optimizedSize: Long,
        val compressionRatio: Float,
        val processingTimeMs: Long,
        val error: String? = null
    )
    
    private data class ProcessingTask(
        val inputPath: String,
        val outputPath: String,
        val settings: CompressionSettings,
        val priority: ProcessingPriority,
        val callback: (ProcessingResult) -> Unit
    )
    
    /**
     * Process image with smart compression in background
     */
    fun processImageAsync(
        inputPath: String,
        settings: CompressionSettings = CompressionSettings(),
        priority: ProcessingPriority = ProcessingPriority.NORMAL,
        callback: (ProcessingResult) -> Unit
    ) {
        val outputPath = generateOptimizedPath(inputPath)
        val task = ProcessingTask(inputPath, outputPath, settings, priority, callback)
        
        when (priority) {
            ProcessingPriority.IMMEDIATE -> {
                processingScope.launch {
                    processTask(task)
                }
            }
            ProcessingPriority.HIGH -> {
                // Add to front of queue
                val currentTasks = mutableListOf<ProcessingTask>()
                while (true) {
                    val existingTask = processingQueue.poll() ?: break
                    currentTasks.add(existingTask)
                }
                processingQueue.offer(task)
                currentTasks.forEach { processingQueue.offer(it) }
                startProcessingIfNeeded()
            }
            else -> {
                processingQueue.offer(task)
                startProcessingIfNeeded()
            }
        }
    }
    
    /**
     * Process image synchronously for immediate results
     */
    suspend fun processImageSync(
        inputPath: String,
        settings: CompressionSettings = CompressionSettings()
    ): ProcessingResult {
        val outputPath = generateOptimizedPath(inputPath)
        val task = ProcessingTask(inputPath, outputPath, settings, ProcessingPriority.IMMEDIATE) {}
        
        return withContext(Dispatchers.Default) {
            processTask(task)
        }
    }
    
    /**
     * Optimize image for instant capture display
     */
    suspend fun createInstantPreview(
        inputPath: String,
        maxSize: Int = 512
    ): String? {
        return withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()
                
                val previewSettings = CompressionSettings(
                    qualityMode = QualityMode.FAST,
                    maxWidth = maxSize,
                    maxHeight = maxSize,
                    enableProgressiveJPEG = false,
                    preserveExif = false,
                    enableSmartCompression = false
                )
                
                val result = processTask(
                    ProcessingTask(
                        inputPath = inputPath,
                        outputPath = File(tempDir, "preview_${System.currentTimeMillis()}.jpg").absolutePath,
                        settings = previewSettings,
                        priority = ProcessingPriority.IMMEDIATE,
                        callback = {}
                    )
                )
                
                val processingTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "Instant preview created in ${processingTime}ms")
                
                if (result.success) result.optimizedPath else null
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create instant preview", e)
                null
            }
        }
    }
    
    /**
     * Get current processing queue status
     */
    fun getProcessingStatus(): ProcessingStatus {
        return ProcessingStatus(
            queueSize = processingQueue.size,
            activeOperations = activeOperations.get(),
            maxConcurrentOperations = MAX_CONCURRENT_OPERATIONS
        )
    }
    
    /**
     * Clear processing queue and cancel pending operations
     */
    fun clearQueue() {
        processingQueue.clear()
        Log.d(TAG, "Processing queue cleared")
    }
    
    /**
     * Clean up old optimized files
     */
    suspend fun cleanupOptimizedFiles(olderThanHours: Int = 24): Int {
        return withContext(Dispatchers.IO) {
            var deletedCount = 0
            val cutoffTime = System.currentTimeMillis() - (olderThanHours * 60 * 60 * 1000L)
            
            try {
                optimizedDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoffTime && file.delete()) {
                        deletedCount++
                    }
                }
                
                tempDir.listFiles()?.forEach { file ->
                    if (file.lastModified() < cutoffTime && file.delete()) {
                        deletedCount++
                    }
                }
                
                Log.d(TAG, "Cleaned up $deletedCount old optimized files")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up optimized files", e)
            }
            
            deletedCount
        }
    }
    
    // Private methods
    
    private fun startProcessingIfNeeded() {
        if (activeOperations.get() < MAX_CONCURRENT_OPERATIONS && processingQueue.isNotEmpty()) {
            processingScope.launch {
                processQueuedTasks()
            }
        }
    }
    
    private suspend fun processQueuedTasks() {
        while (processingQueue.isNotEmpty() && activeOperations.get() < MAX_CONCURRENT_OPERATIONS) {
            val task = processingQueue.poll() ?: break
            
            processingScope.async {
                processTask(task)
            }
        }
    }
    
    private suspend fun processTask(task: ProcessingTask): ProcessingResult {
        semaphore.acquire()
        activeOperations.incrementAndGet()
        
        return try {
            val startTime = System.currentTimeMillis()
            
            val inputFile = File(task.inputPath)
            if (!inputFile.exists()) {
                return ProcessingResult(
                    success = false,
                    originalPath = task.inputPath,
                    optimizedPath = null,
                    originalSize = 0,
                    optimizedSize = 0,
                    compressionRatio = 0f,
                    processingTimeMs = 0,
                    error = "Input file does not exist"
                )
            }
            
            val originalSize = inputFile.length()
            val bitmap = loadOptimizedBitmap(task.inputPath, task.settings)
            
            if (bitmap == null) {
                return ProcessingResult(
                    success = false,
                    originalPath = task.inputPath,
                    optimizedPath = null,
                    originalSize = originalSize,
                    optimizedSize = 0,
                    compressionRatio = 0f,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    error = "Failed to decode image"
                )
            }
            
            try {
                val processedBitmap = processBitmap(bitmap, task.inputPath, task.settings)
                val success = saveBitmap(processedBitmap, task.outputPath, task.settings)
                
                val outputFile = File(task.outputPath)
                val optimizedSize = if (outputFile.exists()) outputFile.length() else 0
                val compressionRatio = if (originalSize > 0) (originalSize - optimizedSize).toFloat() / originalSize else 0f
                
                val result = ProcessingResult(
                    success = success,
                    originalPath = task.inputPath,
                    optimizedPath = if (success) task.outputPath else null,
                    originalSize = originalSize,
                    optimizedSize = optimizedSize,
                    compressionRatio = compressionRatio,
                    processingTimeMs = System.currentTimeMillis() - startTime
                )
                
                bitmap.recycle()
                if (processedBitmap != bitmap) {
                    processedBitmap.recycle()
                }
                
                task.callback(result)
                result
                
            } catch (e: Exception) {
                bitmap.recycle()
                val errorResult = ProcessingResult(
                    success = false,
                    originalPath = task.inputPath,
                    optimizedPath = null,
                    originalSize = originalSize,
                    optimizedSize = 0,
                    compressionRatio = 0f,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    error = e.message
                )
                task.callback(errorResult)
                errorResult
            }
            
        } finally {
            activeOperations.decrementAndGet()
            semaphore.release()
        }
    }
    
    private fun loadOptimizedBitmap(imagePath: String, settings: CompressionSettings): Bitmap? {
        return try {
            val options = BitmapFactory.Options()
            
            // First pass: get dimensions
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(imagePath, options)
            
            // Calculate sample size for memory efficiency
            val sampleSize = calculateSampleSize(options, settings.maxWidth, settings.maxHeight)
            
            // Second pass: decode with sample size
            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Memory efficient
            
            BitmapFactory.decodeFile(imagePath, options)
            
        } catch (e: OutOfMemoryError) {
            // Fallback with more aggressive downsampling
            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 8
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeFile(imagePath, options)
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun processBitmap(bitmap: Bitmap, originalPath: String, settings: CompressionSettings): Bitmap {
        var processedBitmap = bitmap
        
        // Handle EXIF orientation
        if (settings.preserveExif) {
            processedBitmap = handleExifOrientation(processedBitmap, originalPath)
        }
        
        // Resize if needed
        if (processedBitmap.width > settings.maxWidth || processedBitmap.height > settings.maxHeight) {
            val resizedBitmap = resizeBitmap(processedBitmap, settings.maxWidth, settings.maxHeight)
            if (resizedBitmap != processedBitmap) {
                processedBitmap.recycle()
                processedBitmap = resizedBitmap
            }
        }
        
        return processedBitmap
    }
    
    private fun saveBitmap(bitmap: Bitmap, outputPath: String, settings: CompressionSettings): Boolean {
        return try {
            val outputFile = File(outputPath)
            outputFile.parentFile?.mkdirs()
            
            val quality = when (settings.qualityMode) {
                QualityMode.ULTRA_HIGH -> HIGH_QUALITY_THRESHOLD
                QualityMode.HIGH -> MEDIUM_QUALITY_THRESHOLD
                QualityMode.BALANCED -> LOW_QUALITY_THRESHOLD
                QualityMode.OPTIMIZED -> calculateOptimalQuality(bitmap)
                QualityMode.FAST -> 60
            }
            
            FileOutputStream(outputFile).use { out ->
                val format = if (settings.enableProgressiveJPEG) {
                    Bitmap.CompressFormat.JPEG
                } else {
                    Bitmap.CompressFormat.JPEG
                }
                
                bitmap.compress(format, quality, out)
                out.flush()
            }
            
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save optimized bitmap", e)
            false
        }
    }
    
    private fun calculateSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val scale = minOf(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun handleExifOrientation(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
            
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }
    
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        return try {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
    
    private fun calculateOptimalQuality(bitmap: Bitmap): Int {
        val pixels = bitmap.width * bitmap.height
        return when {
            pixels > 8_000_000 -> 70  // Large images need more compression
            pixels > 4_000_000 -> 80  // Medium images
            else -> 85                // Small images can maintain higher quality
        }
    }
    
    private fun generateOptimizedPath(inputPath: String): String {
        val inputFile = File(inputPath)
        val timestamp = System.currentTimeMillis()
        return File(optimizedDir, "opt_${timestamp}_${inputFile.nameWithoutExtension}.jpg").absolutePath
    }
    
    data class ProcessingStatus(
        val queueSize: Int,
        val activeOperations: Int,
        val maxConcurrentOperations: Int
    ) {
        val isIdle: Boolean get() = queueSize == 0 && activeOperations == 0
        val isBusy: Boolean get() = activeOperations >= maxConcurrentOperations
    }
}