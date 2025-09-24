package com.hazardhawk.ui.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import androidx.compose.runtime.*
import kotlinx.coroutines.*

/**
 * Android implementation of construction environment adaptation.
 * Monitors environmental factors and adapts glass effects accordingly.
 */
class ConstructionEnvironmentAdapter(private val context: Context) : SensorEventListener {
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    
    // Sensors
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    
    // Environment state
    private var _currentEnvironment = mutableStateOf(
        ConstructionEnvironment(
            name = "Unknown",
            ambientLightLux = 1000,
            isWearingGloves = false,
            gloveThicknessMM = 0,
            isVibrationPresent = false,
            batteryLevel = 1.0f,
            thermalState = ThermalState.NOMINAL
        )
    )
    val currentEnvironment: State<ConstructionEnvironment> = _currentEnvironment
    
    // Environmental monitoring
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    private var lastVibrationTime = 0L
    private var vibrationDetected = false
    
    // Light level tracking
    private var currentLightLevel = 1000.0f
    private val lightLevelHistory = mutableListOf<Float>()
    
    // Vibration detection
    private var lastAcceleration = FloatArray(3)
    private var accelerationThreshold = 15.0f // m/s²
    
    /**
     * Start monitoring construction environment factors
     */
    fun startMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        
        // Register sensor listeners
        lightSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        accelerometer?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        gyroscope?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        
        // Start periodic environment updates
        monitoringJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isMonitoring) {
                updateEnvironment()
                delay(2000) // Update every 2 seconds
            }
        }
    }
    
    /**
     * Stop monitoring environment factors
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJob?.cancel()
        sensorManager.unregisterListener(this)
    }
    
    /**
     * Manually set glove wearing status (could be from user input or other detection)
     */
    fun setGloveWearing(isWearing: Boolean, thicknessMM: Int = 3) {
        _currentEnvironment.value = _currentEnvironment.value.copy(
            isWearingGloves = isWearing,
            gloveThicknessMM = thicknessMM
        )
    }
    
    /**
     * Get adaptive glass configuration for current environment
     */
    fun getAdaptiveConfiguration(baseConfig: GlassConfiguration): GlassConfiguration {
        val environment = _currentEnvironment.value
        return adaptConfigurationForEnvironment(baseConfig, environment)
    }
    
    /**
     * Get construction-specific recommendations
     */
    fun getConstructionRecommendations(): List<ConstructionRecommendation> {
        val environment = _currentEnvironment.value
        val recommendations = mutableListOf<ConstructionRecommendation>()
        
        // Light level recommendations
        when {
            environment.ambientLightLux > 50000 -> {
                recommendations.add(
                    ConstructionRecommendation(
                        type = RecommendationType.VISIBILITY,
                        priority = RecommendationPriority.HIGH,
                        message = "Bright sunlight detected. Increasing UI opacity for better visibility.",
                        action = "Use outdoor glass configuration"
                    )
                )
            }
            environment.ambientLightLux < 50 -> {
                recommendations.add(
                    ConstructionRecommendation(
                        type = RecommendationType.VISIBILITY,
                        priority = RecommendationPriority.MEDIUM,
                        message = "Low light conditions. Adjusting glass effects for better readability.",
                        action = "Use enhanced blur configuration"
                    )
                )
            }
        }
        
        // Vibration recommendations
        if (environment.isVibrationPresent) {
            recommendations.add(
                ConstructionRecommendation(
                    type = RecommendationType.USABILITY,
                    priority = RecommendationPriority.HIGH,
                    message = "Vibration detected. Disabling animations to prevent visual distraction.",
                    action = "Disable glass animations"
                )
            )
        }
        
        // Glove recommendations
        if (environment.isWearingGloves && environment.gloveThicknessMM > 5) {
            recommendations.add(
                ConstructionRecommendation(
                    type = RecommendationType.ACCESSIBILITY,
                    priority = RecommendationPriority.CRITICAL,
                    message = "Heavy gloves detected. Consider disabling glass effects for better touch accuracy.",
                    action = "Use emergency mode configuration"
                )
            )
        }
        
        // Battery recommendations
        if (environment.batteryLevel < 0.2f) {
            recommendations.add(
                ConstructionRecommendation(
                    type = RecommendationType.PERFORMANCE,
                    priority = RecommendationPriority.CRITICAL,
                    message = "Low battery detected. Disabling glass effects to preserve power.",
                    action = "Switch to power saving mode"
                )
            )
        }
        
        // Thermal recommendations
        if (environment.thermalState >= ThermalState.MODERATE) {
            recommendations.add(
                ConstructionRecommendation(
                    type = RecommendationType.PERFORMANCE,
                    priority = RecommendationPriority.HIGH,
                    message = "Device heating detected. Reducing glass effects to prevent overheating.",
                    action = "Use reduced glass configuration"
                )
            )
        }
        
        return recommendations
    }
    
    private suspend fun updateEnvironment() {
        val batteryLevel = getBatteryLevel()
        val thermalState = getCurrentThermalState()
        
        _currentEnvironment.value = _currentEnvironment.value.copy(
            ambientLightLux = currentLightLevel.toInt(),
            isVibrationPresent = vibrationDetected,
            batteryLevel = batteryLevel,
            thermalState = thermalState,
            name = determineEnvironmentName()
        )
        
        // Reset vibration detection after update
        if (System.currentTimeMillis() - lastVibrationTime > 5000) {
            vibrationDetected = false
        }
    }
    
    private fun getBatteryLevel(): Float {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            level / 100.0f
        } else {
            1.0f // Assume full battery on older devices
        }
    }
    
    private fun getCurrentThermalState(): ThermalState {
        // Use the same implementation as in GlassCapabilityDetector
        val detector = GlassCapabilityDetector(context)
        return detector.getCurrentThermalState()
    }
    
    private fun determineEnvironmentName(): String {
        val environment = _currentEnvironment.value
        
        return when {
            environment.ambientLightLux > 50000 -> "Bright Outdoor"
            environment.ambientLightLux < 50 -> "Low Light"
            environment.isVibrationPresent && environment.ambientLightLux > 1000 -> "Heavy Machinery Area"
            environment.isWearingGloves && environment.gloveThicknessMM > 5 -> "Heavy Work Environment"
            environment.batteryLevel < 0.2f -> "Power Conservation Mode"
            environment.thermalState >= ThermalState.MODERATE -> "High Temperature Environment"
            environment.ambientLightLux in 500..2000 -> "Indoor Workshop"
            else -> "Standard Construction Site"
        }
    }
    
    private fun adaptConfigurationForEnvironment(
        baseConfig: GlassConfiguration,
        environment: ConstructionEnvironment
    ): GlassConfiguration {
        var adaptedConfig = baseConfig
        
        // Bright sunlight adaptations - increase opacity for visibility
        if (environment.ambientLightLux > 50000) {
            adaptedConfig = adaptedConfig.copy(
                opacity = adaptedConfig.opacity.coerceAtLeast(0.8f),
                animationsEnabled = false,
                supportLevel = GlassSupportLevel.REDUCED
            )
        }
        
        // Low light adaptations - increase blur for better effect visibility
        if (environment.ambientLightLux < 100) {
            adaptedConfig = adaptedConfig.copy(
                blurRadius = adaptedConfig.blurRadius.coerceAtLeast(20.0f),
                opacity = adaptedConfig.opacity.coerceAtMost(0.5f)
            )
        }
        
        // Heavy glove adaptations - disable glass for better touch accuracy
        if (environment.gloveThicknessMM > 5) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.DISABLED
            )
        }
        
        // Vibration present adaptations - disable animations
        if (environment.isVibrationPresent) {
            adaptedConfig = adaptedConfig.copy(
                animationsEnabled = false
            )
        }
        
        // Battery level adaptations
        if (environment.batteryLevel < 0.2f) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.DISABLED,
                animationsEnabled = false
            )
        } else if (environment.batteryLevel < 0.5f) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.REDUCED,
                animationsEnabled = false
            )
        }
        
        // Thermal throttling adaptations
        if (environment.thermalState >= ThermalState.SEVERE) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.DISABLED
            )
        } else if (environment.thermalState >= ThermalState.MODERATE) {
            adaptedConfig = adaptedConfig.copy(
                supportLevel = GlassSupportLevel.REDUCED,
                animationsEnabled = false
            )
        }
        
        return adaptedConfig
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let { sensorEvent ->
            when (sensorEvent.sensor.type) {
                Sensor.TYPE_LIGHT -> {
                    currentLightLevel = sensorEvent.values[0]
                    
                    // Keep history of light levels for more stable detection
                    lightLevelHistory.add(currentLightLevel)
                    if (lightLevelHistory.size > 10) {
                        lightLevelHistory.removeFirst()
                    }
                    
                    // Use average of recent readings for more stable detection
                    if (lightLevelHistory.size >= 5) {
                        currentLightLevel = lightLevelHistory.average().toFloat()
                    }
                }
                
                Sensor.TYPE_ACCELEROMETER -> {
                    val x = sensorEvent.values[0]
                    val y = sensorEvent.values[1]
                    val z = sensorEvent.values[2]
                    
                    // Calculate magnitude of acceleration change
                    val deltaX = x - lastAcceleration[0]
                    val deltaY = y - lastAcceleration[1]
                    val deltaZ = z - lastAcceleration[2]
                    
                    val accelerationMagnitude = kotlin.math.sqrt(
                        (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()
                    ).toFloat()
                    
                    // Detect significant vibration/movement
                    if (accelerationMagnitude > accelerationThreshold) {
                        vibrationDetected = true
                        lastVibrationTime = System.currentTimeMillis()
                    }
                    
                    lastAcceleration[0] = x
                    lastAcceleration[1] = y
                    lastAcceleration[2] = z
                }
                
                Sensor.TYPE_GYROSCOPE -> {
                    // Could be used for rotation detection in the future
                    // For now, we primarily use accelerometer for vibration detection
                }
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle sensor accuracy changes if needed
    }
}

/**
 * Construction environment recommendation data
 */
data class ConstructionRecommendation(
    val type: RecommendationType,
    val priority: RecommendationPriority,
    val message: String,
    val action: String
)

/**
 * Types of construction environment recommendations
 */
enum class RecommendationType {
    VISIBILITY,     // Lighting and visual clarity
    USABILITY,      // Interaction and user experience
    ACCESSIBILITY,  // Gloves, touch targets, etc.
    PERFORMANCE,    // Battery, thermal, etc.
    SAFETY         // Emergency and critical situations
}

/**
 * Priority levels for recommendations
 */
enum class RecommendationPriority {
    LOW,        // Optional improvement
    MEDIUM,     // Recommended change
    HIGH,       // Important change
    CRITICAL    // Required change for safety/functionality
}

/**
 * Composable function for using environment adapter
 */
@Composable
fun rememberConstructionEnvironmentAdapter(context: Context): ConstructionEnvironmentAdapter {
    val adapter = remember { ConstructionEnvironmentAdapter(context) }
    
    DisposableEffect(adapter) {
        adapter.startMonitoring()
        onDispose {
            adapter.stopMonitoring()
        }
    }
    
    return adapter
}

/**
 * Environmental conditions display for debugging/monitoring
 */
@Composable
fun ConstructionEnvironmentDisplay(
    adapter: ConstructionEnvironmentAdapter,
    visible: Boolean = false
) {
    val environment by adapter.currentEnvironment
    val recommendations = adapter.getConstructionRecommendations()
    
    if (visible) {
        androidx.compose.material3.Card(
            modifier = androidx.compose.ui.Modifier
                .androidx.compose.foundation.layout.fillMaxWidth()
                .androidx.compose.foundation.layout.padding(8.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
            )
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier.androidx.compose.foundation.layout.padding(12.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "Construction Environment: ${environment.name}",
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = androidx.compose.ui.graphics.Color.White
                )
                
                androidx.compose.foundation.layout.Spacer(
                    modifier = androidx.compose.ui.Modifier.androidx.compose.foundation.layout.height(8.dp)
                )
                
                androidx.compose.material3.Text(
                    text = "Light: ${environment.ambientLightLux} lux",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
                
                androidx.compose.material3.Text(
                    text = "Battery: ${(environment.batteryLevel * 100).toInt()}%",
                    color = if (environment.batteryLevel > 0.2f) androidx.compose.ui.graphics.Color.Green 
                           else androidx.compose.ui.graphics.Color.Red,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
                
                androidx.compose.material3.Text(
                    text = "Thermal: ${environment.thermalState}",
                    color = when (environment.thermalState) {
                        ThermalState.NOMINAL, ThermalState.LIGHT -> androidx.compose.ui.graphics.Color.Green
                        ThermalState.MODERATE -> androidx.compose.ui.graphics.Color.Yellow
                        ThermalState.SEVERE, ThermalState.CRITICAL -> androidx.compose.ui.graphics.Color.Red
                    },
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
                
                if (environment.isWearingGloves) {
                    androidx.compose.material3.Text(
                        text = "Gloves: ${environment.gloveThicknessMM}mm",
                        color = androidx.compose.ui.graphics.Color.Yellow,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                
                if (environment.isVibrationPresent) {
                    androidx.compose.material3.Text(
                        text = "Vibration: Detected",
                        color = androidx.compose.ui.graphics.Color.Orange,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                    )
                }
                
                if (recommendations.isNotEmpty()) {
                    androidx.compose.foundation.layout.Spacer(
                        modifier = androidx.compose.ui.Modifier.androidx.compose.foundation.layout.height(8.dp)
                    )
                    
                    androidx.compose.material3.Text(
                        text = "Active Recommendations:",
                        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    
                    recommendations.take(3).forEach { recommendation ->
                        androidx.compose.material3.Text(
                            text = "• ${recommendation.message}",
                            color = when (recommendation.priority) {
                                RecommendationPriority.CRITICAL -> androidx.compose.ui.graphics.Color.Red
                                RecommendationPriority.HIGH -> androidx.compose.ui.graphics.Color.Orange
                                RecommendationPriority.MEDIUM -> androidx.compose.ui.graphics.Color.Yellow
                                RecommendationPriority.LOW -> androidx.compose.ui.graphics.Color.Green
                            },
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}