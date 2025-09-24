package com.hazardhawk.security

/**
 * Android-specific implementations for security test utilities
 */

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual suspend fun delay(millis: Long) {
    kotlinx.coroutines.delay(millis)
}

actual fun getUsedMemory(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.totalMemory() - runtime.freeMemory()
}