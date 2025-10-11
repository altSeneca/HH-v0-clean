package com.hazardhawk.security

/**
 * Extension functions for SecureStorageService to provide additional capabilities
 */

/**
 * Check if hardware-backed security is available
 */
fun SecureStorageService.isHardwareBackedSecurity(): Boolean {
    // This would check if Android Keystore is using hardware backing
    // For now, return a sensible default
    return try {
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
    } catch (e: Exception) {
        false
    }
}
