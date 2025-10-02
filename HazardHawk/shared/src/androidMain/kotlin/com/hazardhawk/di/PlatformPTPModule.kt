package com.hazardhawk.di

import com.hazardhawk.documents.AndroidPTPPDFGenerator
import com.hazardhawk.documents.PTPPDFGenerator

/**
 * Android implementation of platform-specific PTP PDF generator.
 * Returns an instance of AndroidPTPPDFGenerator that uses Android's PdfDocument API.
 */
actual fun getPlatformPTPPDFGenerator(): PTPPDFGenerator {
    return AndroidPTPPDFGenerator()
}
