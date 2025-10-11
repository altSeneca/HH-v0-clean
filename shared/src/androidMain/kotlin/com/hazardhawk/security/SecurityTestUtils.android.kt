package com.hazardhawk.security

import kotlinx.coroutines.delay as coroutineDelay

/**
 * Android-specific implementation of security test utilities.
 */

/**
 * Gets current time in milliseconds since epoch using Android/JVM System class.
 */
actual fun currentTimeMillis(): Long {
    return System.currentTimeMillis()
}

/**
 * Platform-specific delay implementation using Kotlin coroutines.
 */
actual suspend fun delay(millis: Long) {
    coroutineDelay(millis)
}

/**
 * Gets used memory in bytes using Android/JVM Runtime.
 */
actual fun getUsedMemory(): Long {
    val runtime = Runtime.getRuntime()
    return runtime.totalMemory() - runtime.freeMemory()
}
