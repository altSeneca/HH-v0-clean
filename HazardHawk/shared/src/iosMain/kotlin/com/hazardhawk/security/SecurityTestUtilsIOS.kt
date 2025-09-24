package com.hazardhawk.security

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import kotlinx.coroutines.delay as coroutineDelay

/**
 * iOS-specific implementations for security test utilities
 */

actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}

actual suspend fun delay(millis: Long) {
    coroutineDelay(millis)
}

actual fun getUsedMemory(): Long {
    // iOS memory usage approximation
    // Using a simple approximation since exact memory APIs require additional iOS dependencies
    return 150_000_000L // ~150MB approximation for testing
}