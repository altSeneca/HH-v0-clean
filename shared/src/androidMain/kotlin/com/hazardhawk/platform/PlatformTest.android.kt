package com.hazardhawk.platform

import java.io.File

/**
 * Android-specific implementation of PlatformTest.
 * Provides platform-specific testing utilities for Android.
 */
actual class PlatformTest {
    
    actual fun getPlatformName(): String {
        return "Android ${android.os.Build.VERSION.SDK_INT}"
    }
    
    actual fun testFileReading(filePath: String): String {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                "File exists: ${file.absolutePath}"
            } else {
                "File not found: ${file.absolutePath}"
            }
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }
}
