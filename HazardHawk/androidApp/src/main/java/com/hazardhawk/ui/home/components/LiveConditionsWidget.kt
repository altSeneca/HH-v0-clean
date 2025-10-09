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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.models.dashboard.*
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.coroutines.delay

/**
 * Live Conditions Widget - Real-time weather and crew information
 *
 * Features:
 * - Live weather conditions with temperature and conditions
 * - Crew count and status
 * - Shift information
 * - Animated updates
 * - Safety status indicators
 * - High contrast for outdoor visibility
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LiveConditionsWidget(
    siteConditions: SiteConditions?,
    modifier: Modifier = Modifier
) {
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(siteConditions?.lastUpdated) {
        isRefreshing = true
        delay(500)
        isRefreshing = false
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and refresh indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = ConstructionColors.SafetyOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Live Conditions",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ConstructionColors.OnSurface
                    )
                }

                // Refresh indicator
                AnimatedVisibility(
                    visible = isRefreshing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = ConstructionColors.SafetyOrange
                    )
                }
            }

            Divider(color = ConstructionColors.SurfaceVariant)

            // Conditions grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Weather condition
                WeatherConditionCard(
                    weather = siteConditions?.weather,
                    modifier = Modifier.weight(1f)
                )

                // Crew information
                CrewInfoCard(
                    crewInfo = siteConditions?.crewInfo,
                    modifier = Modifier.weight(1f)
                )
            }

            // Shift information
            siteConditions?.shiftInfo?.let { shift ->
                ShiftStatusBar(shiftInfo = shift)
            }
        }
    }
}

/**
 * Weather condition card with temperature and icon
 */
@Composable
private fun WeatherConditionCard(
    weather: WeatherConditions?,
    modifier: Modifier = Modifier
) {
    val weatherIcon = when (weather?.condition) {
        WeatherType.CLEAR -> Icons.Default.WbSunny
        WeatherType.PARTLY_CLOUDY -> Icons.Default.Cloud
        WeatherType.CLOUDY -> Icons.Default.CloudQueue
        WeatherType.RAIN, WeatherType.HEAVY_RAIN -> Icons.Default.Opacity
        WeatherType.SNOW -> Icons.Default.AcUnit
        WeatherType.THUNDERSTORM -> Icons.Default.Thunderstorm
        WeatherType.FOG -> Icons.Default.BlurOn
        WeatherType.WIND -> Icons.Default.Air
        WeatherType.EXTREME_HEAT -> Icons.Default.Whatshot
        WeatherType.EXTREME_COLD -> Icons.Default.AcUnit
        else -> Icons.Default.WbSunny
    }

    val isSafe = weather?.isSafeForWork() ?: true
    val statusColor = if (isSafe) ConstructionColors.SafetyGreen else ConstructionColors.CautionRed

    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(12.dp),
        color = ConstructionColors.SurfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Safety status indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor)
                    .align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Weather",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ConstructionColors.OnSurfaceVariant
                        )
                        Text(
                            text = if (weather != null) {
                                "${weather.temperature}${weather.temperatureUnit}"
                            } else {
                                "--°F"
                            },
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionColors.OnSurface
                        )
                    }

                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = weather?.description ?: "Loading...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ConstructionColors.OnSurfaceVariant
                )
            }
        }
    }
}

/**
 * Crew information card
 */
@Composable
private fun CrewInfoCard(
    crewInfo: CrewInfo?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(12.dp),
        color = ConstructionColors.SurfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(ConstructionColors.WorkZoneBlue)
                    .align(Alignment.CenterStart)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "Crew",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = ConstructionColors.OnSurfaceVariant
                        )
                        Text(
                            text = "${crewInfo?.presentCount ?: 0}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = ConstructionColors.OnSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = ConstructionColors.WorkZoneBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Total: ${crewInfo?.totalCrew ?: 0} • Supervisors: ${crewInfo?.supervisors ?: 0}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ConstructionColors.OnSurfaceVariant
                    )
                    if (!crewInfo?.specialtyTrades.isNullOrEmpty()) {
                        Text(
                            text = crewInfo?.specialtyTrades?.take(2)?.joinToString(", ") ?: "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = ConstructionColors.OnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shift status bar
 */
@Composable
private fun ShiftStatusBar(
    shiftInfo: ShiftInfo
) {
    val shiftColor = when (shiftInfo.currentStatus) {
        ShiftStatus.ACTIVE, ShiftStatus.OVERTIME_ACTIVE -> ConstructionColors.SafetyGreen
        ShiftStatus.BREAK -> ConstructionColors.SafetyYellow
        ShiftStatus.PRE_SHIFT -> ConstructionColors.WorkZoneBlue
        ShiftStatus.POST_SHIFT -> ConstructionColors.ConcreteGray
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = shiftColor.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = shiftColor,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = when (shiftInfo.currentStatus) {
                            ShiftStatus.ACTIVE -> "Shift Active"
                            ShiftStatus.BREAK -> "On Break"
                            ShiftStatus.PRE_SHIFT -> "Pre-Shift"
                            ShiftStatus.POST_SHIFT -> "Shift Ended"
                            ShiftStatus.OVERTIME_ACTIVE -> "Overtime Active"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = shiftColor
                    )
                    Text(
                        text = "${shiftInfo.startTime} - ${shiftInfo.endTime}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = ConstructionColors.OnSurfaceVariant
                    )
                }
            }

            // Animated status indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(shiftColor)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LiveConditionsWidgetPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConstructionColors.Surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // With full data
            LiveConditionsWidget(
                siteConditions = SiteConditions(
                    weather = WeatherConditions(
                        temperature = 72,
                        condition = WeatherType.PARTLY_CLOUDY,
                        description = "Partly cloudy, comfortable",
                        humidity = 55,
                        windSpeed = 8
                    ),
                    crewInfo = CrewInfo(
                        totalCrew = 24,
                        presentCount = 22,
                        supervisors = 3,
                        specialtyTrades = listOf("Electrical", "Plumbing", "Carpentry")
                    ),
                    shiftInfo = ShiftInfo(
                        shiftType = ShiftType.DAY_SHIFT,
                        startTime = "07:00",
                        endTime = "15:30",
                        currentStatus = ShiftStatus.ACTIVE
                    )
                )
            )

            // Without data
            LiveConditionsWidget(
                siteConditions = null
            )

            // Extreme weather
            LiveConditionsWidget(
                siteConditions = SiteConditions(
                    weather = WeatherConditions(
                        temperature = 98,
                        condition = WeatherType.EXTREME_HEAT,
                        description = "Extreme heat warning",
                        alerts = listOf(
                            WeatherAlert(
                                type = WeatherAlertType.HEAT_ADVISORY,
                                severity = AlertSeverity.SEVERE,
                                message = "Heat warning",
                                startTime = System.currentTimeMillis()
                            )
                        )
                    ),
                    crewInfo = CrewInfo(
                        totalCrew = 15,
                        presentCount = 12,
                        supervisors = 2
                    ),
                    shiftInfo = ShiftInfo(
                        shiftType = ShiftType.DAY_SHIFT,
                        startTime = "06:00",
                        endTime = "14:00",
                        currentStatus = ShiftStatus.BREAK
                    )
                )
            )
        }
    }
}
