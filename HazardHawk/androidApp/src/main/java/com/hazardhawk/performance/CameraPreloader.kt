package com.hazardhawk.performance

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * CameraPreloader handles preloading camera components for instant startup
 * Ensures < 2 second camera initialization and < 500ms capture time
 */
class CameraPreloader private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "CameraPreloader"
        
        @Volatile
        private var INSTANCE: CameraPreloader? = null
        
        fun getInstance(context: Context): CameraPreloader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CameraPreloader(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val preloadScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val cameraProviderRef = AtomicReference<ProcessCameraProvider?>()
    private val isPreloading = AtomicBoolean(false)
    private val isPreloaded = AtomicBoolean(false)
    
    // Pre-configured camera components
    private var preConfiguredImageCapture: ImageCapture? = null
    private var preConfiguredPreview: Preview? = null
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    
    // Performance metrics
    private var preloadStartTime = 0L
    private var preloadEndTime = 0L
    
    /**
     * Start preloading camera components immediately
     * Should be called from Application.onCreate()
     */
    fun startPreloading() {
        if (isPreloading.get() || isPreloaded.get()) {
            Log.d(TAG, "Camera already preloading or preloaded")
            return
        }
        
        if (!isPreloading.compareAndSet(false, true)) {
            return
        }
        
        preloadStartTime = System.currentTimeMillis()
        Log.d(TAG, "Starting camera preloading...")
        
        preloadScope.launch {
            try {
                preloadCameraProvider()
                preloadCameraComponents()
                
                preloadEndTime = System.currentTimeMillis()
                val preloadTime = preloadEndTime - preloadStartTime
                Log.d(TAG, "Camera preloading completed in ${preloadTime}ms")
                
                isPreloaded.set(true)
                isPreloading.set(false)
                
            } catch (e: Exception) {
                Log.e(TAG, "Camera preloading failed", e)
                isPreloading.set(false)
                
                // Retry after delay
                kotlinx.coroutines.delay(2000)
                if (!isPreloaded.get()) {
                    startPreloading()
                }
            }
        }
    }
    
    /**
     * Get preloaded camera provider for instant binding
     */
    suspend fun getPreloadedCameraProvider(): ProcessCameraProvider? {
        return withContext(Dispatchers.Main) {
            if (isPreloaded.get()) {
                cameraProviderRef.get()
            } else {
                // Fallback to regular loading
                Log.w(TAG, "Camera not preloaded, falling back to regular initialization")
                ProcessCameraProvider.getInstance(context).get()
            }
        }
    }
    
    /**
     * Get pre-configured ImageCapture for instant photo taking
     */
    fun getPreConfiguredImageCapture(): ImageCapture? {
        return if (isPreloaded.get()) {
            preConfiguredImageCapture
        } else {
            // Create new ImageCapture with optimized settings
            ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build()
        }
    }
    
    /**
     * Get pre-configured Preview for instant display
     */
    fun getPreConfiguredPreview(): Preview? {
        return if (isPreloaded.get()) {
            preConfiguredPreview
        } else {
            Preview.Builder().build()
        }
    }
    
    /**
     * Bind preloaded camera to lifecycle for instant startup
     */
    suspend fun bindPreloadedCamera(
        lifecycleOwner: LifecycleOwner,
        onSuccess: (ProcessCameraProvider, Preview, ImageCapture) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val bindingStartTime = System.currentTimeMillis()
            
            val cameraProvider = getPreloadedCameraProvider() ?: run {
                onError(IllegalStateException("Camera provider not available"))
                return
            }
            
            val preview = getPreConfiguredPreview() ?: run {
                onError(IllegalStateException("Preview not available"))
                return
            }
            
            val imageCapture = getPreConfiguredImageCapture() ?: run {
                onError(IllegalStateException("ImageCapture not available"))
                return
            }
            
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                
                val bindingTime = System.currentTimeMillis() - bindingStartTime
                Log.d(TAG, "Camera binding completed in ${bindingTime}ms")
                
                onSuccess(cameraProvider, preview, imageCapture)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            onError(e)
        }
    }
    
    /**
     * Check if camera is ready for instant use
     */
    fun isCameraReady(): Boolean {
        return isPreloaded.get() && 
               cameraProviderRef.get() != null &&
               preConfiguredImageCapture != null &&
               preConfiguredPreview != null
    }
    
    /**
     * Get preloading performance metrics
     */
    fun getPreloadMetrics(): PreloadMetrics {
        return PreloadMetrics(
            isPreloaded = isPreloaded.get(),
            isPreloading = isPreloading.get(),
            preloadTimeMs = if (preloadEndTime > preloadStartTime) preloadEndTime - preloadStartTime else -1,
            cameraProviderAvailable = cameraProviderRef.get() != null,
            componentsConfigured = preConfiguredImageCapture != null && preConfiguredPreview != null
        )
    }
    
    /**
     * Release preloaded resources
     */
    fun release() {
        Log.d(TAG, "Releasing camera preloader resources")
        
        preloadScope.launch {
            try {
                cameraProviderRef.get()?.unbindAll()
                cameraProviderRef.set(null)
                preConfiguredImageCapture = null
                preConfiguredPreview = null
                
                isPreloaded.set(false)
                isPreloading.set(false)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing camera resources", e)
            }
        }
    }
    
    // Private methods
    
    private suspend fun preloadCameraProvider() {
        val cameraProvider = withContext(Dispatchers.Main) {
            ProcessCameraProvider.getInstance(context).get()
        }
        
        cameraProviderRef.set(cameraProvider)
        Log.d(TAG, "CameraProvider preloaded successfully")
    }
    
    private fun preloadCameraComponents() {
        // Pre-configure ImageCapture with optimal settings for buffer management
        preConfiguredImageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
            .setJpegQuality(85) // Balanced quality to reduce buffer allocation pressure
            .build()
            
        // Pre-configure Preview with buffer optimization
        preConfiguredPreview = Preview.Builder()
            .setTargetRotation(android.view.Surface.ROTATION_0)
            .build()
            
        // Trigger initial memory cleanup to prepare buffers
        System.gc()
        
        Log.d(TAG, "Camera components pre-configured successfully with buffer optimization")
    }
    
    /**
     * Data class for preloading performance metrics
     */
    data class PreloadMetrics(
        val isPreloaded: Boolean,
        val isPreloading: Boolean,
        val preloadTimeMs: Long,
        val cameraProviderAvailable: Boolean,
        val componentsConfigured: Boolean
    ) {
        fun isOptimal(): Boolean {
            return isPreloaded && preloadTimeMs in 0..2000 // Target < 2 seconds
        }
    }
}

/**
 * Extension function to easily initialize camera preloader in Application class
 */
fun Context.initializeCameraPreloader() {
    CameraPreloader.getInstance(this).startPreloading()
}