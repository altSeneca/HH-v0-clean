package com.hazardhawk.platform

/**
 * Platform-agnostic time utilities for Kotlin Multiplatform
 *
 * This provides a common interface for getting current time across all platforms
 * without relying on platform-specific APIs like System.currentTimeMillis()
 */

/**
 * Get current time in milliseconds since epoch
 * Uses kotlinx.datetime which is available in commonMain
 */
fun currentTimeMillis(): Long {
    return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
}

/**
 * Get current time as Instant
 */
fun currentInstant(): kotlinx.datetime.Instant {
    return kotlinx.datetime.Clock.System.now()
}
