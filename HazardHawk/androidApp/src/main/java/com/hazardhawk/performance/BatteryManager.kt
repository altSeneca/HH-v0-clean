package com.hazardhawk.performance

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * BatteryManager handles power optimization strategies including adaptive location updates,
 * smart flash usage, background task batching, and wake lock management
 */
class HazardHawkBatteryManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "BatteryManager"
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val CRITICAL_BATTERY_THRESHOLD = 10
        private const val LOCATION_UPDATE_INTERVAL_NORMAL = 30_000L // 30 seconds
        private const val LOCATION_UPDATE_INTERVAL_BATTERY_SAVER = 120_000L // 2 minutes
        private const val BACKGROUND_SYNC_BATCH_SIZE = 10
        private const val WAKE_LOCK_TIMEOUT = 15_000L // 15 seconds max (battery optimized)
        
        @Volatile
        private var INSTANCE: HazardHawkBatteryManager? = null
        
        fun getInstance(context: Context): HazardHawkBatteryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HazardHawkBatteryManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val batteryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    // Battery state tracking
    private val _batteryState = MutableStateFlow(BatteryInfo())
    val batteryState: StateFlow<BatteryInfo> = _batteryState.asStateFlow()
    
    private val _powerMode = MutableStateFlow(PowerMode.NORMAL)
    val powerMode: StateFlow<PowerMode> = _powerMode.asStateFlow()
    
    // Wake lock management
    private var cameraWakeLock: PowerManager.WakeLock? = null
    private var processingWakeLock: PowerManager.WakeLock? = null
    private val wakeLockCount = AtomicLong(0)
    
    // Background task batching
    private val pendingTasks = mutableListOf<BackgroundTask>()
    private val isBatchProcessing = AtomicBoolean(false)
    private var lastBatchTime = 0L
    
    // Location optimization
    private var currentLocationInterval = LOCATION_UPDATE_INTERVAL_NORMAL
    private var isLocationOptimized = false
    
    init {
        startBatteryMonitoring()
        optimizeForDevicePowerProfile()
    }
    
    enum class BatteryState {
        CHARGING, DISCHARGING, FULL, UNKNOWN
    }

    enum class PowerMode {
        HIGH_PERFORMANCE, // Maximum performance mode
        BALANCED,         // Balanced performance and battery life
        POWER_SAVER,      // Battery saving mode
        NORMAL,           // Full performance
        BATTERY_SAVER,    // Reduced performance to save battery
        ULTRA_SAVER,      // Minimal functionality
        CHARGING          // Can use full performance while charging
    }
    
    enum class TaskPriority {
        CRITICAL,     // Must execute immediately
        HIGH,         // Execute soon
        NORMAL,       // Execute in next batch
        LOW           // Execute when device is idle/charging
    }
    
    data class BatteryInfo(
        val level: Int = 100,
        val isCharging: Boolean = false,
        val chargingType: ChargingType = ChargingType.NONE,
        val temperature: Float = 25f,
        val health: String = "Good",
        val powerMode: PowerMode = PowerMode.NORMAL,
        val state: BatteryState = BatteryState.UNKNOWN
    )
    
    enum class ChargingType {
        NONE,
        AC,
        USB,
        WIRELESS
    }
    
    data class BackgroundTask(
        val id: String,
        val action: suspend () -> Unit,
        val priority: TaskPriority,
        val estimatedDurationMs: Long,
        val requiresNetwork: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Acquire wake lock for camera operations
     */
    fun acquireCameraWakeLock() {
        try {
            if (cameraWakeLock?.isHeld != true) {
                cameraWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "HazardHawk:CameraWakeLock"
                ).apply {
                    acquire(WAKE_LOCK_TIMEOUT)
                }
                
                wakeLockCount.incrementAndGet()
                Log.d(TAG, "Camera wake lock acquired")
                
                // Auto-release after timeout for safety
                batteryScope.launch {
                    delay(WAKE_LOCK_TIMEOUT)
                    releaseCameraWakeLock()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire camera wake lock", e)
        }
    }
    
    /**
     * Release camera wake lock
     */
    fun releaseCameraWakeLock() {
        try {
            cameraWakeLock?.let { wakeLock ->
                if (wakeLock.isHeld) {
                    wakeLock.release()
                    Log.d(TAG, "Camera wake lock released")
                }
            }
            cameraWakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing camera wake lock", e)
        }
    }
    
    /**
     * Acquire wake lock for background processing
     */
    fun acquireProcessingWakeLock(durationMs: Long = 30_000L) {
        try {
            if (processingWakeLock?.isHeld != true) {
                val actualDuration = if (_batteryState.value.level < LOW_BATTERY_THRESHOLD) {
                    minOf(durationMs, 15_000L) // Reduce duration on low battery
                } else {
                    durationMs
                }
                
                processingWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "HazardHawk:ProcessingWakeLock"
                ).apply {
                    acquire(actualDuration)
                }
                
                wakeLockCount.incrementAndGet()
                Log.d(TAG, "Processing wake lock acquired for ${actualDuration}ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire processing wake lock", e)
        }
    }
    
    /**
     * Release processing wake lock
     */
    fun releaseProcessingWakeLock() {
        try {
            processingWakeLock?.let { wakeLock ->
                if (wakeLock.isHeld) {
                    wakeLock.release()
                    Log.d(TAG, "Processing wake lock released")
                }
            }
            processingWakeLock = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing processing wake lock", e)
        }
    }
    
    /**
     * Schedule background task with intelligent batching
     */
    fun scheduleBackgroundTask(task: BackgroundTask) {
        synchronized(pendingTasks) {
            // Remove duplicate tasks
            pendingTasks.removeAll { it.id == task.id }
            pendingTasks.add(task)
            
            // Sort by priority
            pendingTasks.sortByDescending { it.priority.ordinal }
        }
        
        when (task.priority) {
            TaskPriority.CRITICAL -> {
                // Execute immediately
                batteryScope.launch {
                    executeSingleTask(task)
                }
            }
            TaskPriority.HIGH -> {
                // Execute soon, possibly batched with other high-priority tasks
                scheduleHighPriorityExecution()
            }
            else -> {
                // Batch with other tasks
                scheduleBatchExecution()
            }
        }
    }
    
    /**
     * Get optimal location update interval based on battery state
     */
    fun getOptimalLocationInterval(): Long {
        return when (_powerMode.value) {
            PowerMode.NORMAL, PowerMode.CHARGING -> LOCATION_UPDATE_INTERVAL_NORMAL
            PowerMode.BATTERY_SAVER -> LOCATION_UPDATE_INTERVAL_BATTERY_SAVER
            PowerMode.ULTRA_SAVER -> LOCATION_UPDATE_INTERVAL_BATTERY_SAVER * 2
            PowerMode.HIGH_PERFORMANCE -> LOCATION_UPDATE_INTERVAL_NORMAL / 2 // More frequent
            PowerMode.BALANCED -> LOCATION_UPDATE_INTERVAL_NORMAL
            PowerMode.POWER_SAVER -> LOCATION_UPDATE_INTERVAL_BATTERY_SAVER
        }
    }
    
    /**
     * Check if flash should be used based on battery level
     */
    fun shouldUseFlash(): Boolean {
        val batteryLevel = _batteryState.value.level
        return when {
            batteryLevel < CRITICAL_BATTERY_THRESHOLD -> false
            batteryLevel < LOW_BATTERY_THRESHOLD -> false // Conservative flash usage
            _batteryState.value.isCharging -> true
            else -> true
        }
    }
    
    /**
     * Check if intensive features should be enabled based on battery level
     */
    fun shouldEnableIntensiveFeatures(): Boolean {
        val batteryLevel = _batteryState.value.level
        val isCharging = _batteryState.value.isCharging
        val isPowerSaveMode = _powerMode.value == PowerMode.BATTERY_SAVER
        
        return when {
            isPowerSaveMode -> false
            batteryLevel < CRITICAL_BATTERY_THRESHOLD -> false
            batteryLevel < LOW_BATTERY_THRESHOLD && !isCharging -> false
            else -> true
        }
    }
    
    /**
     * Get location update interval based on battery state
     */
    fun getLocationUpdateInterval(): Long {
        val batteryLevel = _batteryState.value.level
        val isCharging = _batteryState.value.isCharging
        
        return when {
            isCharging -> LOCATION_UPDATE_INTERVAL_NORMAL
            batteryLevel < LOW_BATTERY_THRESHOLD -> LOCATION_UPDATE_INTERVAL_BATTERY_SAVER * 2 // 4 minutes
            batteryLevel < 50 -> LOCATION_UPDATE_INTERVAL_BATTERY_SAVER // 2 minutes  
            else -> LOCATION_UPDATE_INTERVAL_NORMAL // 30 seconds
        }
    }
    
    /**
     * Get recommended image quality based on power mode
     */
    fun getRecommendedImageQuality(): Int {
        return when (_powerMode.value) {
            PowerMode.HIGH_PERFORMANCE -> 95
            PowerMode.NORMAL, PowerMode.CHARGING, PowerMode.BALANCED -> 90
            PowerMode.BATTERY_SAVER, PowerMode.POWER_SAVER -> 80
            PowerMode.ULTRA_SAVER -> 70
        }
    }
    
    /**
     * Check if device is in power saving mode
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun isPowerSaveMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }
    
    /**
     * Get battery optimization status for the app
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun isBatteryOptimizationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else {
            false
        }
    }
    
    /**
     * Request battery optimization exemption
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun requestBatteryOptimizationExemption(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isBatteryOptimizationEnabled()) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = android.net.Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }
    
    /**
     * Clean up all resources
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up battery manager")
        
        releaseCameraWakeLock()
        releaseProcessingWakeLock()
        
        synchronized(pendingTasks) {
            pendingTasks.clear()
        }
    }
    
    // Private methods
    
    private fun startBatteryMonitoring() {
        batteryScope.launch {
            while (isActive) {
                updateBatteryState()
                updatePowerMode()
                delay(60_000) // Update every 60 seconds (reduced frequency)
            }
        }
    }
    
    private fun updateBatteryState() {
        try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryIntent?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryLevel = if (level >= 0 && scale > 0) {
                    (level.toFloat() / scale.toFloat() * 100).toInt()
                } else {
                    100
                }
                
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
                
                val plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                val chargingType = when (plugType) {
                    BatteryManager.BATTERY_PLUGGED_AC -> ChargingType.AC
                    BatteryManager.BATTERY_PLUGGED_USB -> ChargingType.USB
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingType.WIRELESS
                    else -> ChargingType.NONE
                }
                
                val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) / 10f
                val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
                val healthString = when (health) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failed"
                    BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                    else -> "Unknown"
                }
                
                _batteryState.value = BatteryInfo(
                    level = batteryLevel,
                    isCharging = isCharging,
                    chargingType = chargingType,
                    temperature = temperature,
                    health = healthString,
                    powerMode = _powerMode.value
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating battery state", e)
        }
    }
    
    private fun updatePowerMode() {
        val currentState = _batteryState.value
        val newMode = when {
            currentState.isCharging -> PowerMode.CHARGING
            currentState.level <= CRITICAL_BATTERY_THRESHOLD -> PowerMode.ULTRA_SAVER
            currentState.level <= LOW_BATTERY_THRESHOLD -> PowerMode.BATTERY_SAVER
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isPowerSaveMode() -> PowerMode.BATTERY_SAVER
            else -> PowerMode.NORMAL
        }
        
        if (newMode != _powerMode.value) {
            _powerMode.value = newMode
            onPowerModeChanged(newMode)
            Log.d(TAG, "Power mode changed to: $newMode")
        }
    }
    
    private fun onPowerModeChanged(newMode: PowerMode) {
        when (newMode) {
            PowerMode.ULTRA_SAVER -> {
                // Cancel all non-critical tasks
                synchronized(pendingTasks) {
                    pendingTasks.removeAll { it.priority != TaskPriority.CRITICAL }
                }
            }
            PowerMode.BATTERY_SAVER, PowerMode.POWER_SAVER -> {
                // Reduce background processing
                currentLocationInterval = LOCATION_UPDATE_INTERVAL_BATTERY_SAVER
            }
            PowerMode.CHARGING -> {
                // Can be more aggressive with processing
                scheduleBatchExecution()
            }
            PowerMode.NORMAL, PowerMode.BALANCED -> {
                currentLocationInterval = LOCATION_UPDATE_INTERVAL_NORMAL
            }
            PowerMode.HIGH_PERFORMANCE -> {
                currentLocationInterval = LOCATION_UPDATE_INTERVAL_NORMAL / 2
                scheduleBatchExecution() // Process tasks immediately
            }
        }
    }
    
    private fun scheduleHighPriorityExecution() {
        batteryScope.launch {
            delay(1000) // Small delay to batch high-priority tasks
            executeHighPriorityTasks()
        }
    }
    
    private suspend fun executeHighPriorityTasks() {
        val highPriorityTasks = synchronized(pendingTasks) {
            pendingTasks.filter { it.priority == TaskPriority.HIGH }.toList()
        }
        
        if (highPriorityTasks.isNotEmpty()) {
            acquireProcessingWakeLock(15_000L)
            try {
                highPriorityTasks.forEach { task ->
                    executeSingleTask(task)
                    synchronized(pendingTasks) {
                        pendingTasks.remove(task)
                    }
                }
            } finally {
                releaseProcessingWakeLock()
            }
        }
    }
    
    private fun scheduleBatchExecution() {
        if (isBatchProcessing.compareAndSet(false, true)) {
            batteryScope.launch {
                val delayTime = when (_powerMode.value) {
                    PowerMode.HIGH_PERFORMANCE -> 1_000L // Very quick batching
                    PowerMode.CHARGING -> 5_000L // Quick batching when charging
                    PowerMode.NORMAL, PowerMode.BALANCED -> 10_000L
                    PowerMode.BATTERY_SAVER, PowerMode.POWER_SAVER -> 30_000L
                    PowerMode.ULTRA_SAVER -> 60_000L
                }
                
                delay(delayTime)
                executeBatchedTasks()
                isBatchProcessing.set(false)
            }
        }
    }
    
    private suspend fun executeBatchedTasks() {
        val tasksToExecute = synchronized(pendingTasks) {
            val batchSize = when (_powerMode.value) {
                PowerMode.HIGH_PERFORMANCE -> BACKGROUND_SYNC_BATCH_SIZE * 3
                PowerMode.CHARGING -> BACKGROUND_SYNC_BATCH_SIZE * 2
                PowerMode.NORMAL, PowerMode.BALANCED -> BACKGROUND_SYNC_BATCH_SIZE
                PowerMode.BATTERY_SAVER, PowerMode.POWER_SAVER -> BACKGROUND_SYNC_BATCH_SIZE / 2
                PowerMode.ULTRA_SAVER -> 1
            }
            
            pendingTasks.take(batchSize).toList().also { tasks ->
                pendingTasks.removeAll(tasks)
            }
        }
        
        if (tasksToExecute.isNotEmpty()) {
            val totalDuration = tasksToExecute.sumOf { it.estimatedDurationMs }
            acquireProcessingWakeLock(totalDuration + 5000L) // Add buffer
            
            try {
                tasksToExecute.forEach { task ->
                    executeSingleTask(task)
                }
                lastBatchTime = System.currentTimeMillis()
                Log.d(TAG, "Executed batch of ${tasksToExecute.size} background tasks")
            } finally {
                releaseProcessingWakeLock()
            }
        }
    }
    
    private suspend fun executeSingleTask(task: BackgroundTask) {
        try {
            withContext(Dispatchers.Default) {
                val startTime = System.currentTimeMillis()
                task.action()
                val executionTime = System.currentTimeMillis() - startTime
                Log.d(TAG, "Task ${task.id} executed in ${executionTime}ms")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing background task ${task.id}", e)
        }
    }
    
    private fun optimizeForDevicePowerProfile() {
        // Adjust settings based on device capabilities
        val maxMemory = Runtime.getRuntime().maxMemory()
        val isLowMemoryDevice = maxMemory < 512 * 1024 * 1024 // Less than 512MB
        
        if (isLowMemoryDevice) {
            Log.d(TAG, "Detected low memory device, enabling aggressive power saving")
            // Could trigger more aggressive power saving measures
        }
    }
}
