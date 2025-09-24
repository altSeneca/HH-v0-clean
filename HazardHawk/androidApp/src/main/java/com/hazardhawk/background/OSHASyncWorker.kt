package com.hazardhawk.background

import android.content.Context
import androidx.work.*
import androidx.lifecycle.LiveData
import com.hazardhawk.data.repositories.OSHARegulationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import android.util.Log

/**
 * Background worker for syncing OSHA regulations from ecfr.gov
 * Runs monthly to keep regulations up to date
 */
class OSHASyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val oshaRepository: OSHARegulationRepository by inject()

    override suspend fun doWork(): Result {
        return try {
            Log.d("OSHASyncWorker", "Starting OSHA regulation sync")

            // Check if sync is needed
            if (!oshaRepository.isUpdateNeeded()) {
                Log.d("OSHASyncWorker", "OSHA sync not needed, skipping")
                return Result.success()
            }

            // Perform sync
            val syncResult = oshaRepository.syncOSHARegulations(forceUpdate = false)

            if (syncResult.isSuccess) {
                val status = syncResult.getOrThrow()
                Log.d("OSHASyncWorker", "OSHA sync completed successfully. Total regulations: ${status.totalRegulations}")

                // Schedule next sync
                scheduleNextSync(applicationContext)

                Result.success()
            } else {
                val error = syncResult.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("OSHASyncWorker", "OSHA sync failed: $error")

                // Retry on failure
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("OSHASyncWorker", "OSHA sync worker failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "osha_regulation_sync"
        private const val TAG = "OSHASync"

        /**
         * Schedule monthly OSHA regulation sync
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // WiFi only by default
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(false)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<OSHASyncWorker>(
                30, TimeUnit.DAYS // Monthly sync
            )
                .setConstraints(constraints)
                .addTag(TAG)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )

            Log.d("OSHASyncWorker", "Scheduled periodic OSHA sync")
        }

        /**
         * Schedule next sync after successful completion
         */
        private fun scheduleNextSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val nextSyncRequest = OneTimeWorkRequestBuilder<OSHASyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(30, TimeUnit.DAYS)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "${WORK_NAME}_next",
                    ExistingWorkPolicy.REPLACE,
                    nextSyncRequest
                )
        }

        /**
         * Trigger immediate sync (for testing or manual refresh)
         */
        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateSyncRequest = OneTimeWorkRequestBuilder<OSHASyncWorker>()
                .setConstraints(constraints)
                .addTag("${TAG}_immediate")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "${WORK_NAME}_immediate",
                    ExistingWorkPolicy.REPLACE,
                    immediateSyncRequest
                )

            Log.d("OSHASyncWorker", "Triggered immediate OSHA sync")
        }

        /**
         * Cancel all OSHA sync work
         */
        fun cancelSync(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            Log.d("OSHASyncWorker", "Cancelled OSHA sync work")
        }

        /**
         * Get sync work status
         */
        fun getSyncStatus(context: Context): LiveData<List<WorkInfo>> {
            return WorkManager.getInstance(context).getWorkInfosByTagLiveData(TAG)
        }
    }
}

/**
 * Manager for OSHA background sync operations
 */
class OSHASyncManager(private val context: Context) : KoinComponent {

    private val oshaRepository: OSHARegulationRepository by inject()

    /**
     * Initialize OSHA sync on app startup
     */
    fun initializeSync() {
        // Schedule periodic sync
        OSHASyncWorker.schedulePeriodicSync(context)

        // Check if immediate sync is needed (first run or long time since last sync)
        CoroutineScope(Dispatchers.IO).launch {
            if (oshaRepository.isUpdateNeeded()) {
                OSHASyncWorker.triggerImmediateSync(context)
            }
        }
    }

    /**
     * Update sync configuration
     */
    suspend fun updateSyncConfig(
        enabled: Boolean = true,
        wifiOnly: Boolean = true,
        backgroundSync: Boolean = true
    ) {
        val currentConfig = oshaRepository.getSyncConfig().getOrElse {
            com.hazardhawk.models.OSHASyncConfig()
        }

        val newConfig = currentConfig.copy(
            enabled = enabled,
            wifiOnly = wifiOnly,
            backgroundSync = backgroundSync
        )

        oshaRepository.updateSyncConfig(newConfig)

        if (enabled && backgroundSync) {
            OSHASyncWorker.schedulePeriodicSync(context)
        } else {
            OSHASyncWorker.cancelSync(context)
        }
    }

    /**
     * Manually trigger sync
     */
    fun manualSync() {
        OSHASyncWorker.triggerImmediateSync(context)
    }

    /**
     * Get current sync status
     */
    fun observeSyncStatus() = OSHASyncWorker.getSyncStatus(context)
}