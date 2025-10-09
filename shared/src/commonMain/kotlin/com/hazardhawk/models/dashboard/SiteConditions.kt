package com.hazardhawk.models.dashboard

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

/**
 * Real-time site conditions data for the hero status bar.
 * Includes weather, crew information, and shift details.
 */
@Serializable
data class SiteConditions(
    val weather: WeatherConditions? = null,
    val crewInfo: CrewInfo? = null,
    val shiftInfo: ShiftInfo? = null,
    val lastUpdated: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Weather conditions at the jobsite
 */
@Serializable
data class WeatherConditions(
    val temperature: Int,              // Temperature in Fahrenheit
    val temperatureUnit: String = "°F",
    val condition: WeatherType,        // Current weather type
    val description: String,           // Human-readable description
    val humidity: Int? = null,         // Humidity percentage
    val windSpeed: Int? = null,        // Wind speed in mph
    val windDirection: String? = null, // Wind direction (N, S, E, W, etc.)
    val precipitation: Int? = null,    // Precipitation probability (0-100)
    val uvIndex: Int? = null,          // UV index (0-11+)
    val visibility: Double? = null,    // Visibility in miles
    val alerts: List<WeatherAlert> = emptyList() // Active weather alerts
)

/**
 * Weather type classification
 */
enum class WeatherType {
    CLEAR,          // Clear skies
    PARTLY_CLOUDY,  // Partly cloudy
    CLOUDY,         // Overcast
    RAIN,           // Rain
    HEAVY_RAIN,     // Heavy rain
    SNOW,           // Snow
    SLEET,          // Sleet
    FOG,            // Fog
    WIND,           // High winds
    THUNDERSTORM,   // Thunderstorm
    EXTREME_HEAT,   // Extreme heat
    EXTREME_COLD    // Extreme cold
}

/**
 * Weather alerts affecting safety
 */
@Serializable
data class WeatherAlert(
    val type: WeatherAlertType,
    val severity: AlertSeverity,
    val message: String,
    val startTime: Long,
    val endTime: Long? = null
)

enum class WeatherAlertType {
    HEAT_ADVISORY,
    COLD_WEATHER,
    HIGH_WIND,
    SEVERE_STORM,
    FLASH_FLOOD,
    LIGHTNING,
    AIR_QUALITY
}

enum class AlertSeverity {
    EXTREME,    // Life-threatening
    SEVERE,     // Significant danger
    MODERATE,   // Potential danger
    MINOR       // Be aware
}

/**
 * Crew information for the current shift
 */
@Serializable
data class CrewInfo(
    val totalCrew: Int,             // Total crew members on site
    val presentCount: Int,          // Currently present
    val supervisors: Int,           // Number of supervisors
    val specialtyTrades: List<String> = emptyList() // Active trades on site
)

/**
 * Shift schedule information
 */
@Serializable
data class ShiftInfo(
    val shiftType: ShiftType,
    val startTime: String,          // Format: "HH:mm"
    val endTime: String,            // Format: "HH:mm"
    val breakTimes: List<String> = emptyList(), // Scheduled break times
    val currentStatus: ShiftStatus
)

enum class ShiftType {
    DAY_SHIFT,      // Standard day shift
    NIGHT_SHIFT,    // Night shift
    SWING_SHIFT,    // Afternoon/evening shift
    WEEKEND,        // Weekend work
    OVERTIME        // Overtime hours
}

enum class ShiftStatus {
    PRE_SHIFT,      // Before shift start
    ACTIVE,         // Shift in progress
    BREAK,          // On break
    POST_SHIFT,     // After shift end
    OVERTIME_ACTIVE // Working overtime
}

/**
 * Extension functions for weather conditions
 */
fun WeatherConditions.isSafeForWork(): Boolean {
    return when {
        alerts.any { it.severity == AlertSeverity.EXTREME } -> false
        temperature > 105 || temperature < 0 -> false // Extreme temps
        windSpeed != null && windSpeed > 40 -> false // High winds
        condition == WeatherType.THUNDERSTORM -> false
        condition == WeatherType.HEAVY_RAIN -> false
        else -> true
    }
}

fun WeatherConditions.getConditionColor(): String {
    return when {
        !isSafeForWork() -> "#FF0000" // Red for unsafe
        alerts.isNotEmpty() -> "#FFA500" // Orange for alerts
        else -> "#4CAF50" // Green for safe
    }
}

fun WeatherType.toIconName(): String {
    return when (this) {
        WeatherType.CLEAR -> "sunny"
        WeatherType.PARTLY_CLOUDY -> "partly_cloudy"
        WeatherType.CLOUDY -> "cloudy"
        WeatherType.RAIN, WeatherType.HEAVY_RAIN -> "rainy"
        WeatherType.SNOW -> "snowy"
        WeatherType.SLEET -> "sleet"
        WeatherType.FOG -> "foggy"
        WeatherType.WIND -> "windy"
        WeatherType.THUNDERSTORM -> "thunderstorm"
        WeatherType.EXTREME_HEAT -> "extreme_heat"
        WeatherType.EXTREME_COLD -> "extreme_cold"
    }
}
