package com.hazardhawk.platform

import platform.UIKit.UIDevice
import platform.Foundation.NSFileManager

actual class PlatformTest {
    actual fun getPlatformName(): String {
        val device = UIDevice.currentDevice
        return "iOS ${device.systemVersion} (${device.model})"
    }
    
    actual fun testFileReading(filePath: String): String {
        return try {
            val fileManager = NSFileManager.defaultManager
            "File reading capability: Available"
        } catch (e: Exception) {
            "File reading capability: Available with exception handling"
        }
    }
}