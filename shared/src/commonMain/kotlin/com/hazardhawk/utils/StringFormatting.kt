package com.hazardhawk.utils

import kotlin.math.pow
import kotlin.math.round

/**
 * KMP-compatible string formatting utilities.
 * Provides decimal formatting for Float and Double types that works across all platforms.
 */

/**
 * Format a Float to a string with specified decimal places.
 * This is a KMP-compatible alternative to String.format("%.Xf", value)
 * 
 * @param places Number of decimal places (default: 1)
 * @return Formatted string representation
 */
fun Float.formatDecimal(places: Int = 1): String {
    val multiplier = 10.0.pow(places.toDouble())
    val rounded = round(this * multiplier) / multiplier
    
    // Handle the string representation
    val str = rounded.toString()
    val parts = str.split('.')
    
    return if (parts.size == 2) {
        val intPart = parts[0]
        val decPart = parts[1].take(places).padEnd(places, '0')
        "$intPart.$decPart"
    } else {
        // No decimal point, add one with zeros
        val zeros = "0".repeat(places)
        "$str.$zeros"
    }
}

/**
 * Format a Double to a string with specified decimal places.
 * This is a KMP-compatible alternative to String.format("%.Xf", value)
 * 
 * @param places Number of decimal places (default: 1)
 * @return Formatted string representation
 */
fun Double.formatDecimal(places: Int = 1): String {
    val multiplier = 10.0.pow(places.toDouble())
    val rounded = round(this * multiplier) / multiplier
    
    // Handle the string representation
    val str = rounded.toString()
    val parts = str.split('.')
    
    return if (parts.size == 2) {
        val intPart = parts[0]
        val decPart = parts[1].take(places).padEnd(places, '0')
        "$intPart.$decPart"
    } else {
        // No decimal point, add one with zeros
        val zeros = "0".repeat(places)
        "$str.$zeros"
    }
}

/**
 * Format an Int to a string (for consistency in API)
 */
fun Int.formatDecimal(places: Int = 0): String {
    return this.toString()
}

/**
 * Format a Long to a string (for consistency in API)
 */
fun Long.formatDecimal(places: Int = 0): String {
    return this.toString()
}

/**
 * Format percentage with specified decimal places.
 * Multiplies by 100 and adds % symbol.
 */
fun Float.formatPercent(places: Int = 1): String {
    return "${(this * 100).formatDecimal(places)}%"
}

/**
 * Format percentage with specified decimal places.
 * Multiplies by 100 and adds % symbol.
 */
fun Double.formatPercent(places: Int = 1): String {
    return "${(this * 100).formatDecimal(places)}%"
}
