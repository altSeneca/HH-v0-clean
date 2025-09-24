package com.hazardhawk.location

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance benchmarks for GPS location acquisition
 * Measures timing and resource usage of location operations
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class LocationPerformanceBenchmark {
    
    @get:Rule
    val benchmarkRule = BenchmarkRule()
    
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val locationService = LiveLocationService(context)
    
    @Test
    fun benchmarkGetCurrentLiveLocation() {
        benchmarkRule.measureRepeated {
            runBlocking {
                locationService.getCurrentLiveLocation()
            }
        }
    }
    
    @Test
    fun benchmarkGetBestAvailableLocation() {
        benchmarkRule.measureRepeated {
            runBlocking {
                locationService.getBestAvailableLocation()
            }
        }
    }
    
    @Test
    fun benchmarkConcurrentLocationRequests() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val jobs = List(5) {
                    kotlinx.coroutines.async {
                        locationService.getBestAvailableLocation()
                    }
                }
                jobs.forEach { it.await() }
            }
        }
    }
}
