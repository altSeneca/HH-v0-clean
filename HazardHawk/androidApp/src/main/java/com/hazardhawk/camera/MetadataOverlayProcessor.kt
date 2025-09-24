package com.hazardhawk.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withSave
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * High-performance metadata overlay processor for HazardHawk camera system
 * Handles bitmap composition, Compose-to-bitmap conversion, and image burn-in operations
 */
class MetadataOverlayProcessor(private val context: Context) {
    
    companion object {
        private const val TAG = "MetadataOverlayProcessor"
        private const val OVERLAY_ALPHA = 0.85f
        private const val TEXT_SIZE_DP = 80f // Massive base text size for visibility
        private const val PADDING_DP = 40f // Large padding
        private const val CORNER_RADIUS_DP = 20f // Large corner radius
        private const val MAX_BITMAP_SIZE = 4096 // Prevent OOM on high-res cameras
    }
    
    private val density = context.resources.displayMetrics.density
    
    /**
     * Process captured image with metadata overlay burn-in
     * This is the main entry point for adding metadata directly to captured photos
     */
    suspend fun processImageWithMetadataOverlay(
        originalImageFile: File,
        companyName: String,
        projectName: String,
        location: Location?,
        showGPS: Boolean,
        position: MetadataPosition = MetadataPosition.BOTTOM_LEFT
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            // Load and validate original image
            val originalBitmap = loadAndValidateBitmap(originalImageFile)
                ?: return@withContext Result.failure(IllegalArgumentException("Cannot load image: ${originalImageFile.name}"))
            
            // Create metadata overlay bitmap
            val overlayBitmap = createMetadataOverlayBitmap(
                canvasWidth = originalBitmap.width,
                canvasHeight = originalBitmap.height,
                companyName = companyName,
                projectName = projectName,
                location = location,
                showGPS = showGPS,
                position = position
            )
            
            // Composite the images
            val compositeImage = compositeBitmaps(originalBitmap, overlayBitmap)
            
            // Save the result
            val outputFile = createOutputFile(originalImageFile)
            saveCompositeBitmap(compositeImage, outputFile)
            
            // Cleanup
            originalBitmap.recycle()
            overlayBitmap.recycle()
            compositeImage.recycle()
            
            Log.d(TAG, "Successfully processed image with metadata overlay: ${outputFile.name}")
            Result.success(outputFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image with metadata overlay", e)
            Result.failure(e)
        }
    }
    
    /**
     * Load and validate bitmap with memory optimization
     */
    private fun loadAndValidateBitmap(imageFile: File): Bitmap? {
        return try {
            // First, get image dimensions without loading into memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imageFile.absolutePath, options)
            
            // Calculate sample size to prevent OOM
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, MAX_BITMAP_SIZE)
            
            // Load bitmap with appropriate sampling
            val loadOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inMutable = false
            }
            
            BitmapFactory.decodeFile(imageFile.absolutePath, loadOptions)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap: ${imageFile.name}", e)
            null
        }
    }
    
    /**
     * Calculate optimal sample size for memory management
     */
    private fun calculateSampleSize(width: Int, height: Int, maxSize: Int): Int {
        var sampleSize = 1
        val maxDimension = maxOf(width, height)
        
        while (maxDimension / sampleSize > maxSize) {
            sampleSize *= 2
        }
        
        return sampleSize
    }
    
    /**
     * Create metadata overlay bitmap using native Canvas operations
     * This provides pixel-perfect alignment and high performance
     */
    private fun createMetadataOverlayBitmap(
        canvasWidth: Int,
        canvasHeight: Int,
        companyName: String,
        projectName: String,
        location: Location?,
        showGPS: Boolean,
        position: MetadataPosition
    ): Bitmap {
        // Create transparent overlay bitmap
        val overlayBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(overlayBitmap)
        
        // Calculate metadata text and dimensions
        val timestamp = SimpleDateFormat("MM.dd.yyyy HH:mm a", Locale.getDefault()).format(Date())
        val gpsText = if (showGPS && location != null) {
            "GPS: ${String.format("%.6f, %.6f", location.latitude, location.longitude)}"
        } else if (showGPS) {
            "GPS: Acquiring..."
        } else null
        
        val textLines = listOfNotNull(
            companyName,
            projectName.ifEmpty { "No Project Selected" },
            timestamp,
            gpsText
        )
        
        // Setup paint objects
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            alpha = (255 * OVERLAY_ALPHA).roundToInt()
        }
        
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            // Much simpler and EXTREMELY aggressive text sizing
            val baseTextSize = TEXT_SIZE_DP * density
            val minDimension = min(canvasWidth, canvasHeight)

            // Debug logging to understand actual dimensions
            android.util.Log.d(TAG, "Canvas size: ${canvasWidth}x${canvasHeight}, min: $minDimension, density: $density")

            // EXTREME multipliers - make text absolutely massive
            val sizeMultiplier = when {
                minDimension > 3000 -> 8.0f  // 8x for very high res
                minDimension > 2000 -> 6.0f  // 6x for high res
                minDimension > 1500 -> 5.0f  // 5x for medium-high res
                else -> 4.0f  // 4x minimum
            }

            val finalTextSize = baseTextSize * sizeMultiplier
            android.util.Log.d(TAG, "Text size calculation: base=$baseTextSize, multiplier=$sizeMultiplier, final=$finalTextSize")

            textSize = finalTextSize
            typeface = Typeface.DEFAULT_BOLD
        }
        
        // Calculate text bounds and overlay dimensions
        val textBounds = Rect()
        val maxTextWidth = textLines.maxOfOrNull { line ->
            textPaint.getTextBounds(line, 0, line.length, textBounds)
            textBounds.width()
        } ?: 0
        
        val lineHeight = textPaint.fontMetrics.let { it.bottom - it.top }
        val totalTextHeight = lineHeight * textLines.size

        // Scale padding and corner radius with same EXTREME logic as text
        val minDimension = min(canvasWidth, canvasHeight)
        val sizeMultiplier = when {
            minDimension > 3000 -> 8.0f  // 8x for very high res
            minDimension > 2000 -> 6.0f  // 6x for high res
            minDimension > 1500 -> 5.0f  // 5x for medium-high res
            else -> 4.0f  // 4x minimum
        }
        val padding = PADDING_DP * density * sizeMultiplier
        val cornerRadius = CORNER_RADIUS_DP * density * sizeMultiplier

        android.util.Log.d(TAG, "Padding calculation: base=$PADDING_DP, density=$density, multiplier=$sizeMultiplier, final=$padding")
        
        val overlayWidth = maxTextWidth + (padding * 2)
        val overlayHeight = totalTextHeight + (padding * 2)
        
        // Calculate overlay position based on MetadataPosition
        val overlayRect = calculateOverlayPosition(
            canvasWidth = canvasWidth.toFloat(),
            canvasHeight = canvasHeight.toFloat(),
            overlayWidth = overlayWidth,
            overlayHeight = overlayHeight,
            position = position,
            margin = padding
        )
        
        // Draw background with rounded corners
        canvas.drawRoundRect(
            overlayRect,
            cornerRadius,
            cornerRadius,
            backgroundPaint
        )
        
        // Draw text lines
        val textStartX = overlayRect.left + padding
        var textY = overlayRect.top + padding - textPaint.fontMetrics.top
        
        textLines.forEach { line ->
            canvas.drawText(line, textStartX, textY, textPaint)
            textY += lineHeight
        }
        
        return overlayBitmap
    }
    
    /**
     * Calculate overlay position based on metadata position setting
     */
    private fun calculateOverlayPosition(
        canvasWidth: Float,
        canvasHeight: Float,
        overlayWidth: Float,
        overlayHeight: Float,
        position: MetadataPosition,
        margin: Float
    ): RectF {
        return when (position) {
            MetadataPosition.TOP_LEFT -> RectF(
                margin,
                margin,
                margin + overlayWidth,
                margin + overlayHeight
            )
            MetadataPosition.TOP_RIGHT -> RectF(
                canvasWidth - margin - overlayWidth,
                margin,
                canvasWidth - margin,
                margin + overlayHeight
            )
            MetadataPosition.BOTTOM_LEFT -> RectF(
                margin,
                canvasHeight - margin - overlayHeight,
                margin + overlayWidth,
                canvasHeight - margin
            )
            MetadataPosition.BOTTOM_RIGHT -> RectF(
                canvasWidth - margin - overlayWidth,
                canvasHeight - margin - overlayHeight,
                canvasWidth - margin,
                canvasHeight - margin
            )
        }
    }
    
    /**
     * Composite two bitmaps with proper alpha blending
     */
    private fun compositeBitmaps(baseBitmap: Bitmap, overlayBitmap: Bitmap): Bitmap {
        val result = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // Draw overlay with proper blending
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }
        
        canvas.drawBitmap(overlayBitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Save composite bitmap to file with optimization
     */
    private fun saveCompositeBitmap(bitmap: Bitmap, outputFile: File) {
        FileOutputStream(outputFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.flush()
        }
    }
    
    /**
     * Create output file for processed image
     */
    private fun createOutputFile(originalFile: File): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "HH_processed_${timestamp}_${originalFile.nameWithoutExtension}.jpg"
        return File(originalFile.parent, filename)
    }
    
    /**
     * Convert Compose content to bitmap for testing or preview purposes
     * Note: This is resource-intensive and should be used sparingly
     */
    suspend fun composeContentToBitmap(
        width: Int,
        height: Int,
        content: @Composable () -> Unit
    ): Bitmap? = withContext(Dispatchers.Main) {
        try {
            val composeView = ComposeView(context)
            composeView.setContent {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    content()
                }
            }
            
            // Measure and layout
            composeView.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
            )
            composeView.layout(0, 0, width, height)
            
            // Create bitmap and draw
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            composeView.draw(canvas)
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert Compose content to bitmap", e)
            null
        }
    }
}

/**
 * Extension functions for metadata overlay processing
 */

/**
 * Process image with metadata overlay - Extension function for easy use
 */
suspend fun File.addMetadataOverlay(
    context: Context,
    companyName: String,
    projectName: String,
    location: Location? = null,
    showGPS: Boolean = true,
    position: MetadataPosition = MetadataPosition.BOTTOM_LEFT
): Result<File> {
    val processor = MetadataOverlayProcessor(context)
    return processor.processImageWithMetadataOverlay(
        originalImageFile = this,
        companyName = companyName,
        projectName = projectName,
        location = location,
        showGPS = showGPS,
        position = position
    )
}

/**
 * Calculate optimal overlay dimensions for current screen
 */
@Composable
fun rememberOverlayDimensions(): Pair<Int, Int> {
    val density = LocalDensity.current
    
    return remember {
        with(density) {
            val width = (320.dp.toPx()).roundToInt()
            val height = (120.dp.toPx()).roundToInt()
            Pair(width, height)
        }
    }
}

enum class MetadataPosition {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
}