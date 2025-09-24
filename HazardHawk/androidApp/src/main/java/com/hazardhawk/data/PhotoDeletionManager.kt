package com.hazardhawk.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import java.io.IOException

/**
 * Secure Photo Deletion Manager for HazardHawk Construction Safety Platform
 * 
 * Features:
 * - Secure file deletion with proper error handling
 * - MediaStore integration for Android compliance
 * - Audit trail logging for safety-critical operations
 * - Permission validation with security controls
 * - Undo functionality with temporary file recovery
 * - OSHA compliance documentation support
 */
class PhotoDeletionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PhotoDeletionManager"
        private const val UNDO_TIMEOUT_MS = 30000L // 30 seconds undo window
        private const val TRASH_DIR_NAME = ".hazardhawk_deleted"
        
        // Security audit event types
        private const val AUDIT_DELETE_REQUESTED = "DELETE_REQUESTED"
        private const val AUDIT_DELETE_COMPLETED = "DELETE_COMPLETED"
        private const val AUDIT_DELETE_FAILED = "DELETE_FAILED"
        private const val AUDIT_PERMISSION_DENIED = "PERMISSION_DENIED"
        private const val AUDIT_UNDO_REQUESTED = "UNDO_REQUESTED"
        private const val AUDIT_UNDO_COMPLETED = "UNDO_COMPLETED"
    }
    
    /**
     * Photo deletion result with detailed information
     */
    data class DeletionResult(
        val success: Boolean,
        val deletedFiles: List<String> = emptyList(),
        val failedFiles: List<String> = emptyList(),
        val errorMessage: String? = null,
        val permissionDenied: Boolean = false,
        val undoToken: String? = null,
        val auditId: String = UUID.randomUUID().toString()
    )
    
    /**
     * Undo information for file recovery
     */
    private data class UndoInfo(
        val originalFiles: List<File>,
        val tempFiles: List<File>,
        val timestamp: Long,
        val auditId: String
    )
    
    private val undoCache = mutableMapOf<String, UndoInfo>()
    private val trashDir by lazy {
        File(context.filesDir, TRASH_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }
    
    /**
     * Securely delete a single photo with comprehensive security controls
     */
    suspend fun deletePhoto(photoFile: File): DeletionResult = withContext(Dispatchers.IO) {
        val auditId = UUID.randomUUID().toString()
        
        try {
            logSecurityEvent(AUDIT_DELETE_REQUESTED, 
                mapOf(
                    "file" to sanitizeFilePath(photoFile.absolutePath),
                    "size" to photoFile.length().toString(),
                    "audit_id" to auditId
                )
            )
            
            // Validate permissions
            if (!hasRequiredPermissions()) {
                logSecurityEvent(AUDIT_PERMISSION_DENIED, mapOf(
                    "audit_id" to auditId,
                    "requested_file" to sanitizeFilePath(photoFile.absolutePath)
                ))
                return@withContext DeletionResult(
                    success = false,
                    failedFiles = listOf(photoFile.absolutePath),
                    errorMessage = "Storage permission required",
                    permissionDenied = true,
                    auditId = auditId
                )
            }
            
            // Validate file exists and is accessible
            if (!photoFile.exists()) {
                return@withContext DeletionResult(
                    success = false,
                    failedFiles = listOf(photoFile.absolutePath),
                    errorMessage = "File does not exist",
                    auditId = auditId
                )
            }
            
            if (!photoFile.canWrite()) {
                return@withContext DeletionResult(
                    success = false,
                    failedFiles = listOf(photoFile.absolutePath),
                    errorMessage = "No write permission for file",
                    permissionDenied = true,
                    auditId = auditId
                )
            }
            
            // Create backup for undo functionality
            val undoToken = UUID.randomUUID().toString()
            val tempFile = createTempBackup(photoFile, undoToken)
            
            if (tempFile != null) {
                // Store undo information
                undoCache[undoToken] = UndoInfo(
                    originalFiles = listOf(photoFile),
                    tempFiles = listOf(tempFile),
                    timestamp = System.currentTimeMillis(),
                    auditId = auditId
                )
            }
            
            // Remove from MediaStore if present
            val mediaStoreUri = findMediaStoreUri(photoFile)
            var mediaStoreDeleted = false
            
            mediaStoreUri?.let { uri ->
                try {
                    val deletedRows = context.contentResolver.delete(uri, null, null)
                    mediaStoreDeleted = deletedRows > 0
                    Log.d(TAG, "MediaStore deletion: $deletedRows rows deleted")
                } catch (e: SecurityException) {
                    Log.w(TAG, "MediaStore deletion failed: ${e.message}")
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error during MediaStore deletion", e)
                }
            }
            
            // Delete actual file
            val fileDeleted = photoFile.delete()
            
            if (fileDeleted) {
                logSecurityEvent(AUDIT_DELETE_COMPLETED, mapOf(
                    "file" to sanitizeFilePath(photoFile.absolutePath),
                    "media_store_deleted" to mediaStoreDeleted.toString(),
                    "undo_available" to (tempFile != null).toString(),
                    "audit_id" to auditId
                ))
                
                return@withContext DeletionResult(
                    success = true,
                    deletedFiles = listOf(photoFile.absolutePath),
                    undoToken = if (tempFile != null) undoToken else null,
                    auditId = auditId
                )
            } else {
                // Delete failed, clean up temp file
                tempFile?.delete()
                undoCache.remove(undoToken)
                
                logSecurityEvent(AUDIT_DELETE_FAILED, mapOf(
                    "file" to sanitizeFilePath(photoFile.absolutePath),
                    "reason" to "File.delete() returned false",
                    "audit_id" to auditId
                ))
                
                return@withContext DeletionResult(
                    success = false,
                    failedFiles = listOf(photoFile.absolutePath),
                    errorMessage = "Failed to delete file",
                    auditId = auditId
                )
            }
            
        } catch (e: SecurityException) {
            logSecurityEvent(AUDIT_DELETE_FAILED, mapOf(
                "file" to sanitizeFilePath(photoFile.absolutePath),
                "reason" to "SecurityException: ${e.message}",
                "audit_id" to auditId
            ))
            
            return@withContext DeletionResult(
                success = false,
                failedFiles = listOf(photoFile.absolutePath),
                errorMessage = "Security error: ${e.message}",
                permissionDenied = true,
                auditId = auditId
            )
        } catch (e: IOException) {
            logSecurityEvent(AUDIT_DELETE_FAILED, mapOf(
                "file" to sanitizeFilePath(photoFile.absolutePath),
                "reason" to "IOException: ${e.message}",
                "audit_id" to auditId
            ))
            
            return@withContext DeletionResult(
                success = false,
                failedFiles = listOf(photoFile.absolutePath),
                errorMessage = "IO error: ${e.message}",
                auditId = auditId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during photo deletion", e)
            logSecurityEvent(AUDIT_DELETE_FAILED, mapOf(
                "file" to sanitizeFilePath(photoFile.absolutePath),
                "reason" to "Exception: ${e.javaClass.simpleName}: ${e.message}",
                "audit_id" to auditId
            ))
            
            return@withContext DeletionResult(
                success = false,
                failedFiles = listOf(photoFile.absolutePath),
                errorMessage = "Unexpected error: ${e.message}",
                auditId = auditId
            )
        }
    }
    
    /**
     * Batch delete multiple photos with progress tracking
     */
    suspend fun deletePhotos(
        photoFiles: List<File>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): DeletionResult = withContext(Dispatchers.IO) {
        
        val batchAuditId = UUID.randomUUID().toString()
        val deletedFiles = mutableListOf<String>()
        val failedFiles = mutableListOf<String>()
        var hasPermissionError = false
        val errors = mutableListOf<String>()
        val undoTokens = mutableListOf<String>()
        
        logSecurityEvent("BATCH_DELETE_REQUESTED", mapOf(
            "file_count" to photoFiles.size.toString(),
            "batch_audit_id" to batchAuditId
        ))
        
        photoFiles.forEachIndexed { index, file ->
            onProgress?.invoke(index, photoFiles.size)
            
            val result = deletePhoto(file)
            
            if (result.success) {
                deletedFiles.addAll(result.deletedFiles)
                result.undoToken?.let { undoTokens.add(it) }
            } else {
                failedFiles.addAll(result.failedFiles)
                result.errorMessage?.let { errors.add("${file.name}: $it") }
                if (result.permissionDenied) {
                    hasPermissionError = true
                }
            }
        }
        
        onProgress?.invoke(photoFiles.size, photoFiles.size)
        
        val combinedUndoToken = if (undoTokens.isNotEmpty()) {
            val batchUndoToken = UUID.randomUUID().toString()
            // Combine all individual undo tokens into batch undo
            val allOriginalFiles = mutableListOf<File>()
            val allTempFiles = mutableListOf<File>()
            
            undoTokens.forEach { token ->
                undoCache[token]?.let { undoInfo ->
                    allOriginalFiles.addAll(undoInfo.originalFiles)
                    allTempFiles.addAll(undoInfo.tempFiles)
                    undoCache.remove(token) // Remove individual tokens
                }
            }
            
            undoCache[batchUndoToken] = UndoInfo(
                originalFiles = allOriginalFiles,
                tempFiles = allTempFiles,
                timestamp = System.currentTimeMillis(),
                auditId = batchAuditId
            )
            
            batchUndoToken
        } else null
        
        logSecurityEvent("BATCH_DELETE_COMPLETED", mapOf(
            "batch_audit_id" to batchAuditId,
            "deleted_count" to deletedFiles.size.toString(),
            "failed_count" to failedFiles.size.toString(),
            "undo_available" to (combinedUndoToken != null).toString()
        ))
        
        return@withContext DeletionResult(
            success = failedFiles.isEmpty(),
            deletedFiles = deletedFiles,
            failedFiles = failedFiles,
            errorMessage = if (errors.isNotEmpty()) errors.joinToString("; ") else null,
            permissionDenied = hasPermissionError,
            undoToken = combinedUndoToken,
            auditId = batchAuditId
        )
    }
    
    /**
     * Undo photo deletion if within time window
     */
    suspend fun undoDeletion(undoToken: String): DeletionResult = withContext(Dispatchers.IO) {
        val undoAuditId = UUID.randomUUID().toString()
        
        try {
            val undoInfo = undoCache[undoToken] ?: return@withContext DeletionResult(
                success = false,
                errorMessage = "Undo token not found or expired",
                auditId = undoAuditId
            )
            
            // Check if undo window has expired
            val timeSinceDeletion = System.currentTimeMillis() - undoInfo.timestamp
            if (timeSinceDeletion > UNDO_TIMEOUT_MS) {
                // Clean up expired undo data
                undoInfo.tempFiles.forEach { it.delete() }
                undoCache.remove(undoToken)
                
                return@withContext DeletionResult(
                    success = false,
                    errorMessage = "Undo window expired (${UNDO_TIMEOUT_MS / 1000}s limit)",
                    auditId = undoAuditId
                )
            }
            
            logSecurityEvent(AUDIT_UNDO_REQUESTED, mapOf(
                "undo_token" to undoToken,
                "original_audit_id" to undoInfo.auditId,
                "undo_audit_id" to undoAuditId,
                "file_count" to undoInfo.originalFiles.size.toString()
            ))
            
            val restoredFiles = mutableListOf<String>()
            val failedFiles = mutableListOf<String>()
            
            // Restore each file from temporary backup
            undoInfo.originalFiles.zip(undoInfo.tempFiles).forEach { (originalFile, tempFile) ->
                try {
                    if (tempFile.exists()) {
                        // Copy temp file back to original location
                        val restored = tempFile.copyTo(originalFile, overwrite = true)
                        if (restored.exists()) {
                            restoredFiles.add(originalFile.absolutePath)
                            
                            // Re-add to MediaStore if needed
                            addToMediaStore(originalFile)
                        } else {
                            failedFiles.add(originalFile.absolutePath)
                        }
                    } else {
                        failedFiles.add(originalFile.absolutePath)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore file: ${originalFile.absolutePath}", e)
                    failedFiles.add(originalFile.absolutePath)
                }
            }
            
            // Clean up temporary files
            undoInfo.tempFiles.forEach { it.delete() }
            undoCache.remove(undoToken)
            
            logSecurityEvent(AUDIT_UNDO_COMPLETED, mapOf(
                "undo_token" to undoToken,
                "original_audit_id" to undoInfo.auditId,
                "undo_audit_id" to undoAuditId,
                "restored_count" to restoredFiles.size.toString(),
                "failed_count" to failedFiles.size.toString()
            ))
            
            return@withContext DeletionResult(
                success = failedFiles.isEmpty(),
                deletedFiles = restoredFiles, // These are actually restored files
                failedFiles = failedFiles,
                errorMessage = if (failedFiles.isNotEmpty()) "Some files could not be restored" else null,
                auditId = undoAuditId
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during undo operation", e)
            return@withContext DeletionResult(
                success = false,
                errorMessage = "Undo failed: ${e.message}",
                auditId = undoAuditId
            )
        }
    }
    
    /**
     * Check if undo is available for a token
     */
    fun isUndoAvailable(undoToken: String): Boolean {
        val undoInfo = undoCache[undoToken] ?: return false
        val timeSinceDeletion = System.currentTimeMillis() - undoInfo.timestamp
        return timeSinceDeletion <= UNDO_TIMEOUT_MS
    }
    
    /**
     * Get remaining undo time in milliseconds
     */
    fun getUndoTimeRemaining(undoToken: String): Long {
        val undoInfo = undoCache[undoToken] ?: return 0L
        val timeSinceDeletion = System.currentTimeMillis() - undoInfo.timestamp
        return maxOf(0L, UNDO_TIMEOUT_MS - timeSinceDeletion)
    }
    
    /**
     * Clean up expired undo data
     */
    suspend fun cleanupExpiredUndoData() = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()
        val expiredTokens = undoCache.filter { (_, undoInfo) ->
            currentTime - undoInfo.timestamp > UNDO_TIMEOUT_MS
        }
        
        expiredTokens.forEach { (token, undoInfo) ->
            // Delete temporary files
            undoInfo.tempFiles.forEach { tempFile ->
                try {
                    tempFile.delete()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete expired temp file: ${tempFile.absolutePath}", e)
                }
            }
            undoCache.remove(token)
        }
        
        Log.d(TAG, "Cleaned up ${expiredTokens.size} expired undo entries")
    }
    
    /**
     * Find MediaStore URI for a file
     */
    private fun findMediaStoreUri(file: File): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val selection = "${MediaStore.Images.Media.DATA}=?"
        val selectionArgs = arrayOf(file.absolutePath)
        
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            
            if (cursor?.moveToFirst() == true) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val id = cursor.getLong(idColumn)
                Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to find MediaStore URI for ${file.absolutePath}", e)
            null
        } finally {
            cursor?.close()
        }
    }
    
    /**
     * Add file to MediaStore (for undo operations)
     */
    private fun addToMediaStore(file: File) {
        try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.TITLE, file.nameWithoutExtension)
                put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.SIZE, file.length())
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.DATE_MODIFIED, file.lastModified() / 1000)
            }
            
            context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            Log.d(TAG, "Re-added file to MediaStore: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to re-add file to MediaStore: ${file.absolutePath}", e)
        }
    }
    
    /**
     * Create temporary backup for undo functionality
     */
    private fun createTempBackup(originalFile: File, undoToken: String): File? {
        return try {
            val tempFileName = "${undoToken}_${originalFile.name}"
            val tempFile = File(trashDir, tempFileName)
            
            originalFile.copyTo(tempFile, overwrite = true)
            
            if (tempFile.exists()) {
                Log.d(TAG, "Created temp backup: ${tempFile.absolutePath}")
                tempFile
            } else {
                Log.w(TAG, "Failed to create temp backup for: ${originalFile.absolutePath}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp backup for: ${originalFile.absolutePath}", e)
            null
        }
    }
    
    /**
     * Check required permissions for photo deletion
     */
    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses scoped storage
            true // App can delete its own files
        } else {
            // Android 9 and below
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Log security event for audit trail (OSHA compliance)
     */
    private fun logSecurityEvent(eventType: String, details: Map<String, String>) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
            .format(Date())
        
        val logMessage = buildString {
            append("[SECURITY_AUDIT] ")
            append("event=$eventType ")
            append("timestamp=$timestamp ")
            details.forEach { (key, value) ->
                append("$key=$value ")
            }
        }
        
        Log.i("$TAG.Security", logMessage)
        
        // In production, this would also write to:
        // - Encrypted local audit log file
        // - Remote audit service (if available)
        // - OSHA compliance documentation system
    }
    
    /**
     * Sanitize file paths for logging (remove sensitive information)
     */
    private fun sanitizeFilePath(filePath: String): String {
        // Remove potential sensitive directory names, keep filename only
        return File(filePath).name
    }
}