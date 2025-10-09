package com.hazardhawk.ui.home.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.models.dashboard.*
import com.hazardhawk.ui.theme.ConstructionColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Hero Status Bar - Personalized greeting banner with time-of-day gradients
 *
 * Features:
 * - Time-based greeting (Good Morning/Afternoon/Evening)
 * - Dynamic gradient backgrounds based on time of day
 * - Weather alert integration
 * - Smooth animations and transitions
 * - Construction-optimized typography
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HeroStatusBar(
    userName: String,
    userTier: UserTier,
    siteConditions: SiteConditions? = null,
    modifier: Modifier = Modifier
) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)

    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    val gradientColors = getTimeOfDayGradient(hour)
    val hasWeatherAlert = siteConditions?.weather?.alerts?.isNotEmpty() == true

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors
                    )
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Greeting and user name
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = greeting,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        Text(
                            text = userName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // User tier badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = when (userTier) {
                                    UserTier.PROJECT_ADMIN -> Icons.Default.AdminPanelSettings
                                    UserTier.SAFETY_LEAD -> Icons.Default.Shield
                                    UserTier.FIELD_ACCESS -> Icons.Default.Person
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = when (userTier) {
                                    UserTier.PROJECT_ADMIN -> "Admin"
                                    UserTier.SAFETY_LEAD -> "Lead"
                                    UserTier.FIELD_ACCESS -> "Field"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Current date and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
                            .format(Date()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )

                    Text(
                        text = SimpleDateFormat("h:mm a", Locale.getDefault())
                            .format(Date()),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Weather alert banner (if applicable)
                AnimatedVisibility(
                    visible = hasWeatherAlert,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    WeatherAlertBanner(
                        alerts = siteConditions?.weather?.alerts ?: emptyList()
                    )
                }
            }
        }
    }
}

/**
 * Weather alert banner for critical safety notifications
 */
@Composable
private fun WeatherAlertBanner(
    alerts: List<WeatherAlert>
) {
    if (alerts.isEmpty()) return

    val highestAlert = alerts.maxByOrNull { it.severity.ordinal } ?: return

    val alertColor = when (highestAlert.severity) {
        AlertSeverity.EXTREME -> Color(0xFFDC2626)
        AlertSeverity.SEVERE -> Color(0xFFEA580C)
        AlertSeverity.MODERATE -> Color(0xFFF59E0B)
        AlertSeverity.MINOR -> Color(0xFF3B82F6)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(10.dp),
        color = alertColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${highestAlert.severity.name} ALERT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = highestAlert.message,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }

            if (alerts.size > 1) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "+${alerts.size - 1}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

/**
 * Get gradient colors based on time of day
 */
private fun getTimeOfDayGradient(hour: Int): List<Color> {
    return when (hour) {
        in 0..5 -> listOf(
            Color(0xFF1E293B), // Night - deep blue
            Color(0xFF334155)
        )
        in 6..11 -> listOf(
            Color(0xFF0EA5E9), // Morning - bright blue
            Color(0xFF06B6D4)  // Cyan
        )
        in 12..16 -> listOf(
            Color(0xFFF59E0B), // Afternoon - amber
            Color(0xFFFB923C)  // Orange
        )
        in 17..19 -> listOf(
            Color(0xFFF97316), // Evening - orange
            Color(0xFFDC2626)  // Red
        )
        else -> listOf(
            Color(0xFF6366F1), // Night - purple
            Color(0xFF8B5CF6)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HeroStatusBarPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConstructionColors.Surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Without alerts
            HeroStatusBar(
                userName = "John Smith",
                userTier = UserTier.SAFETY_LEAD,
                siteConditions = SiteConditions()
            )

            // With weather alert
            HeroStatusBar(
                userName = "Sarah Johnson",
                userTier = UserTier.PROJECT_ADMIN,
                siteConditions = SiteConditions(
                    weather = WeatherConditions(
                        temperature = 95,
                        condition = WeatherType.EXTREME_HEAT,
                        description = "Extreme heat",
                        alerts = listOf(
                            WeatherAlert(
                                type = WeatherAlertType.HEAT_ADVISORY,
                                severity = AlertSeverity.SEVERE,
                                message = "Heat index above 100°F. Stay hydrated and take frequent breaks.",
                                startTime = System.currentTimeMillis()
                            )
                        )
                    )
                )
            )

            // Field access user
            HeroStatusBar(
                userName = "Mike Davis",
                userTier = UserTier.FIELD_ACCESS
            )
        }
    }
}
