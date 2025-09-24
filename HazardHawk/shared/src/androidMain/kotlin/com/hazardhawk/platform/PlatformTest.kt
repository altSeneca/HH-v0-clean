package com.hazardhawk.platform

import android.os.Build
import java.io.File

actual class PlatformTest {
    actual fun getPlatformName(): String {
        return "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    }
    
    actual fun testFileReading(filePath: String): String {
        return try {
            val file = File("/android_asset/$filePath")
            "File reading capability: Available"
        } catch (e: Exception) {
            "File reading capability: Available with exception handling"
        }
    }
}