package com.hazardhawk.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Utility class for managing file storage operations in the Android app.
 * Handles saving, retrieving, and deleting PDF files in app-specific storage.
 */
class FileStorageUtil(private val context: Context) {

    companion object {
        private const val PDF_DIRECTORY = "ptps"
        private const val FILE_PROVIDER_AUTHORITY = "com.hazardhawk.fileprovider"
    }

    /**
     * Save a PDF byte array to external files directory.
     * Files saved here are app-specific and will be deleted when the app is uninstalled.
     *
     * @param pdfBytes The PDF content as a byte array
     * @param ptpId The unique ID of the Pre-Task Plan
     * @return The absolute file path where the PDF was saved
     */
    fun savePdfToStorage(pdfBytes: ByteArray, ptpId: String): String {
        val fileName = "PTP_${ptpId}_${System.currentTimeMillis()}.pdf"
        val directory = File(context.getExternalFilesDir(null), PDF_DIRECTORY)

        // Create directory if it doesn't exist
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, fileName)
        file.writeBytes(pdfBytes)

        return file.absolutePath
    }

    /**
     * Get a PDF file by its absolute path.
     *
     * @param filePath Absolute path to the PDF file
     * @return File object if the file exists, null otherwise
     */
    fun getPdfFile(filePath: String): File? {
        val file = File(filePath)
        return if (file.exists() && file.canRead()) file else null
    }

    /**
     * Delete a PDF file from storage.
     *
     * @param filePath Absolute path to the PDF file
     * @return true if the file was successfully deleted, false otherwise
     */
    fun deletePdf(filePath: String): Boolean {
        return try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get a content URI for a PDF file using FileProvider.
     * This is needed for sharing PDFs with other apps.
     *
     * @param filePath Absolute path to the PDF file
     * @return Content URI that can be used with Intent.ACTION_VIEW or Intent.ACTION_SEND
     */
    fun getPdfContentUri(filePath: String): android.net.Uri? {
        val file = getPdfFile(filePath) ?: return null
        return try {
            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Create an intent to view a PDF file in an external app.
     *
     * @param filePath Absolute path to the PDF file
     * @return Intent configured to open the PDF, or null if the file doesn't exist
     */
    fun createViewPdfIntent(filePath: String): Intent? {
        val uri = getPdfContentUri(filePath) ?: return null

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Create an intent to share a PDF file.
     *
     * @param filePath Absolute path to the PDF file
     * @param subject Optional subject for the share dialog
     * @return Intent configured to share the PDF, or null if the file doesn't exist
     */
    fun createSharePdfIntent(filePath: String, subject: String? = null): Intent? {
        val uri = getPdfContentUri(filePath) ?: return null

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }

    /**
     * Get all PDF files for PTPs stored in the app directory.
     *
     * @return List of PDF files, sorted by modification date (newest first)
     */
    fun getAllPtpPdfs(): List<File> {
        val directory = File(context.getExternalFilesDir(null), PDF_DIRECTORY)

        if (!directory.exists()) {
            return emptyList()
        }

        return directory.listFiles()
            ?.filter { it.isFile && it.extension == "pdf" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    /**
     * Get the total size of all stored PDF files in bytes.
     *
     * @return Total size in bytes
     */
    fun getTotalPdfStorageSize(): Long {
        return getAllPtpPdfs().sumOf { it.length() }
    }

    /**
     * Clean up old PDF files to free up storage space.
     *
     * @param maxAgeMillis Maximum age of files to keep (default: 90 days)
     * @return Number of files deleted
     */
    fun cleanupOldPdfs(maxAgeMillis: Long = 90L * 24 * 60 * 60 * 1000): Int {
        val currentTime = System.currentTimeMillis()
        val pdfs = getAllPtpPdfs()

        var deletedCount = 0
        for (file in pdfs) {
            if (currentTime - file.lastModified() > maxAgeMillis) {
                if (file.delete()) {
                    deletedCount++
                }
            }
        }

        return deletedCount
    }
}
