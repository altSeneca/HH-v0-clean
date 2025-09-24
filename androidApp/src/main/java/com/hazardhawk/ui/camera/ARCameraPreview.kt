package com.hazardhawk.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * AR-optimized camera preview component for real-time hazard detection.
 * Integrates CameraX with Compose for seamless AR overlay rendering.
 */
@Composable
fun ARCameraPreview(
    onImageCaptured: (ByteArray) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit = {},
    modifier: Modifier = Modifier,
    enableImageAnalysis: Boolean = true,
    analysisInterval: Long = 500L // 2 FPS for AI analysis
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewView = this
            }
        },
        modifier = modifier.fillMaxSize(),
        update = { view ->
            startCamera(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = view,
                onImageCaptured = onImageCaptured,
                onImageCaptureReady = { capture ->
                    imageCapture = capture
                    onImageCaptureReady(capture)
                },
                enableImageAnalysis = enableImageAnalysis,
                analysisInterval = analysisInterval
            )
        }
    )
}

/**
 * Initialize and start CameraX with AR-optimized configuration.
 */
private fun startCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onImageCaptured: (ByteArray) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    enableImageAnalysis: Boolean,
    analysisInterval: Long
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val cameraExecutor = Executors.newSingleThreadExecutor()
    
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        
        // Preview configuration
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        
        // Image capture configuration
        val imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_OPTIMIZE_FOR_QUALITY)
            .build()
        
        // Image analysis configuration for real-time AI processing
        val imageAnalysis = if (enableImageAnalysis) {
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(cameraExecutor, ARImageAnalyzer(
                        onImageAnalyzed = onImageCaptured,
                        analysisInterval = analysisInterval
                    ))
                }
        } else null
        
        // Camera selector - prefer back camera for construction work
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        
        try {
            // Unbind any existing use cases
            cameraProvider.unbindAll()
            
            // Bind use cases to camera
            val useCases = listOfNotNull(preview, imageCapture, imageAnalysis)
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )
            
            // Connect preview to PreviewView
            preview.setSurfaceProvider(previewView.surfaceProvider)
            
            // Notify that image capture is ready
            onImageCaptureReady(imageCapture)
            
            // Enable tap-to-focus
            previewView.setOnTouchListener { _, event ->
                val meteringPointFactory = previewView.meteringPointFactory
                val meteringPoint = meteringPointFactory.createPoint(event.x, event.y)
                
                val focusAction = FocusMeteringAction.Builder(meteringPoint)
                    .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                camera.cameraControl.startFocusAndMetering(focusAction)
                true
            }
            
        } catch (e: Exception) {
            // Handle camera binding errors
            e.printStackTrace()
        }
        
    }, ContextCompat.getMainExecutor(context))
}

/**
 * Custom image analyzer for real-time hazard detection processing.
 * Converts ImageProxy to compressed JPEG ByteArray for AI analysis.
 */
private class ARImageAnalyzer(
    private val onImageAnalyzed: (ByteArray) -> Unit,
    private val analysisInterval: Long
) : ImageAnalysis.Analyzer {
    
    private var lastAnalysisTime = 0L
    
    override fun analyze(image: ImageProxy) {
        val currentTime = System.currentTimeMillis()
        
        // Throttle analysis to prevent overwhelming AI services (2 FPS)
        if (currentTime - lastAnalysisTime >= analysisInterval) {
            lastAnalysisTime = currentTime
            
            try {
                // Convert ImageProxy to compressed JPEG ByteArray
                val jpegBytes = imageProxyToJpegByteArray(image)
                if (jpegBytes != null && jpegBytes.isNotEmpty()) {
                    onImageAnalyzed(jpegBytes)
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Always close the image
        image.close()
    }
    
    /**
     * Convert ImageProxy to compressed JPEG ByteArray.
     * Handles both YUV_420_888 and JPEG formats.
     */
    private fun imageProxyToJpegByteArray(image: ImageProxy): ByteArray? {
        return try {
            when (image.format) {
                ImageFormat.JPEG -> {
                    // Image is already JPEG, extract bytes directly
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    bytes
                }
                ImageFormat.YUV_420_888 -> {
                    // Convert YUV to JPEG
                    yuvToJpeg(image)
                }
                else -> {
                    // Fallback: try to extract from first plane
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    
                    // If it's not already compressed, compress as JPEG
                    if (bytes.size > 1024 * 1024) { // If larger than 1MB, compress
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        bitmap?.let { bitmapToJpeg(it, 85) }
                    } else {
                        bytes
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Convert YUV_420_888 ImageProxy to JPEG ByteArray.
     */
    private fun yuvToJpeg(image: ImageProxy): ByteArray? {
        return try {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            // Copy Y plane
            yBuffer.get(nv21, 0, ySize)
            
            // Interleave U and V planes for NV21 format
            val uvPixelStride = image.planes[1].pixelStride
            if (uvPixelStride == 1) {
                uBuffer.get(nv21, ySize, uSize)
                vBuffer.get(nv21, ySize + uSize, vSize)
            } else {
                // Handle pixel stride > 1
                val uv = ByteArray(uSize + vSize)
                uBuffer.get(uv, 0, uSize)
                vBuffer.get(uv, uSize, vSize)
                
                var uvIndex = 0
                for (i in 0 until uSize) {
                    nv21[ySize + uvIndex] = uv[i]
                    nv21[ySize + uvIndex + 1] = uv[uSize + i]
                    uvIndex += 2
                }
            }
            
            // Convert to YuvImage and compress to JPEG
            val yuvImage = YuvImage(
                nv21, 
                ImageFormat.NV21, 
                image.width, 
                image.height, 
                null
            )
            
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(
                Rect(0, 0, image.width, image.height), 
                85, // JPEG quality (85%)
                out
            )
            
            out.toByteArray()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Convert Bitmap to JPEG ByteArray with specified quality.
     */
    private fun bitmapToJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }
}

/**
 * Capture a single photo for detailed analysis.
 * Context must be provided from the calling Composable.
 */
fun capturePhoto(
    imageCapture: ImageCapture,
    context: Context,
    onPhotoCaptured: (ByteArray) -> Unit,
    onError: (Exception) -> Unit
) {
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
        createTempImageFile(context)
    ).build()
    
    imageCapture.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    val bytes = output.savedUri?.let { uri ->
                        // Read file and convert to ByteArray
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.readBytes()
                        }
                    } ?: ByteArray(0)
                    
                    onPhotoCaptured(bytes)
                } catch (e: Exception) {
                    onError(e)
                }
            }
            
            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

/**
 * Create temporary file for photo capture.
 */
private fun createTempImageFile(context: Context): java.io.File {
    val cacheDir = context.cacheDir
    return java.io.File.createTempFile(
        "hazard_photo_${System.currentTimeMillis()}", 
        ".jpg",
        cacheDir
    )
}

/**
 * Camera configuration for optimal AR performance.
 */
data class ARCameraConfig(
    val targetAspectRatio: Int = AspectRatio.RATIO_16_9,
    val analysisInterval: Long = 500L,
    val enableHDR: Boolean = true,
    val enableStabilization: Boolean = true,
    val flashMode: Int = ImageCapture.FLASH_MODE_AUTO
)

/**
 * Camera controls for AR interface.
 */
@Composable
fun ARCameraControls(
    imageCapture: ImageCapture?,
    onPhotoCaptured: (ByteArray) -> Unit,
    onToggleFlash: () -> Unit,
    flashMode: Int,
    modifier: Modifier = Modifier
) {
    // Camera control UI elements can be added here
    // This is handled by LiveDetectionScreen's bottom controls
}