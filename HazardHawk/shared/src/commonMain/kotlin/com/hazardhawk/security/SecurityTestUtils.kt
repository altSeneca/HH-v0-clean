package com.hazardhawk.security

/**
 * Cross-platform test utilities for security components
 * These functions provide platform-specific implementations for testing needs
 */

/**
 * Gets current time in milliseconds since epoch
 */
expect fun currentTimeMillis(): Long

/**
 * Platform-specific delay implementation
 */
expect suspend fun delay(millis: Long)

/**
 * Gets used memory in bytes (approximation)
 */
expect fun getUsedMemory(): Long