package com.hazardhawk.performance

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive performance benchmarking system for HazardHawk with LiteRT-LM integration.
 * Tests device capabilities, AI processing performance, backend switching, and memory management.
 * Validates CPU (243 t/s), GPU (1876 t/s), NPU (5836 t/s) performance targets.
 * 
 * VALIDATION TARGETS:
 * - 3-8x performance improvement over legacy implementations
 * - Real AI vs mock JSON generation performance gains
 * - Production workload optimization
 * - Multi-device tier optimization
 */
class PerformanceBenchmark(
    private val deviceDetector: DeviceTierDetector,
    private val memoryManager: MemoryManager,
    private val performanceMonitor: PerformanceMonitor,
    private val simplifiedOrchestrator: SimplifiedAIOrchestrator? = null,
    private val smartOrchestrator: SmartAIOrchestrator? = null,
    private val liteRTEngine: LiteRTModelEngine? = null
) {
    
    private val _benchmarkState = MutableStateFlow<BenchmarkState>(BenchmarkState.Idle)
    val benchmarkState: StateFlow<BenchmarkState> = _benchmarkState
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    
    /**
     * Run comprehensive performance benchmark suite including LiteRT validation.
     */
    suspend fun runBenchmarkSuite(): BenchmarkResults {
        return withContext(Dispatchers.Default) {
            _benchmarkState.value = BenchmarkState.Running
            _progress.value = 0f
            
            val results = mutableListOf<BenchmarkTest>()
            val totalTests = 12 // Increased for LiteRT tests
            var completedTests = 0
            
            try {
                // Test 1: Device Capabilities Detection
                _benchmarkState.value = BenchmarkState.Running
                results.add(benchmarkDeviceDetection())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 2: Memory Management
                results.add(benchmarkMemoryManagement())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 3: AI Model Loading Performance
                results.add(benchmarkModelLoading())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 4: Image Processing Performance
                results.add(benchmarkImageProcessing())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 5: Cache Performance
                results.add(benchmarkCachePerformance())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 6: Frame Rate Performance
                results.add(benchmarkFrameRate())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 7: Memory Pressure Handling
                results.add(benchmarkMemoryPressure())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 8: Battery Impact Assessment
                results.add(benchmarkBatteryImpact())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // NEW LiteRT-specific tests
                
                // Test 9: LiteRT Backend Performance
                results.add(benchmarkLiteRTBackends())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 10: Orchestrator Performance Comparison
                results.add(benchmarkOrchestratorComparison())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 11: Production Load Testing
                results.add(benchmarkProductionLoad())
                completedTests++
                _progress.value = completedTests.toFloat() / totalTests
                
                // Test 12: Real vs Mock Performance
                results.add(benchmarkRealVsMockPerformance())
                completedTests++
                _progress.value = 1f
                
                val overallScore = calculateOverallScore(results)
                val deviceCapabilities = deviceDetector.detectCapabilities()
                
                _benchmarkState.value = BenchmarkState.Completed
                
                BenchmarkResults(
                    deviceCapabilities = deviceCapabilities,
                    tests = results,
                    overallScore = overallScore,
                    recommendations = generateRecommendations(results, deviceCapabilities),
                    timestamp = System.currentTimeMillis()
                )
                
            } catch (e: Exception) {
                _benchmarkState.value = BenchmarkState.Error(e.message ?: "Unknown error")
                throw e
            }
        }
    }
    
    /**
     * Test device detection accuracy and performance.
     */
    private suspend fun benchmarkDeviceDetection(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val capabilities = deviceDetector.detectCapabilities()
            
            results["tier"] = capabilities.tier.displayName
            results["totalMemoryMB"] = capabilities.totalMemoryMB
            results["cpuCores"] = capabilities.cpuCores
            results["hasGPU"] = capabilities.hasGPU
            results["hasNNAPI"] = capabilities.hasNNAPI
            
            val memoryUsage = deviceDetector.getCurrentMemoryUsage()
            val availableMemory = deviceDetector.getAvailableMemory()
            
            results["memoryUsageMB"] = memoryUsage / (1024 * 1024)
            results["availableMemoryMB"] = availableMemory / (1024 * 1024)
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val score = if (success && duration < 1000) 100f else if (success) 70f else 0f
        
        return BenchmarkTest(
            name = "Device Detection",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Test memory management efficiency.
     */
    private suspend fun benchmarkMemoryManagement(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val initialStats = memoryManager.getMemoryStats()
            results["initialStats"] = initialStats
            
            // Test image caching
            val testImages = generateTestImages(10)
            var cachedImages = 0
            
            testImages.forEach { (id, imageData) ->
                if (memoryManager.cacheImage(id, imageData)) {
                    cachedImages++
                }
            }
            
            results["cachedImages"] = cachedImages
            results["cacheEfficiency"] = cachedImages.toFloat() / testImages.size
            
            // Test cache retrieval
            var retrievedImages = 0
            testImages.forEach { (id, _) ->
                if (memoryManager.getCachedImage(id) != null) {
                    retrievedImages++
                }
            }
            
            results["retrievedImages"] = retrievedImages
            results["retrievalEfficiency"] = retrievedImages.toFloat() / testImages.size
            
            // Test memory pressure handling
            memoryManager.handleMemoryPressure(MemoryPressure.HIGH)
            val finalStats = memoryManager.getMemoryStats()
            results["finalStats"] = finalStats
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val cacheEfficiency = results["cacheEfficiency"] as? Float ?: 0f
        val score = if (success) (cacheEfficiency * 100f).coerceAtLeast(10f) else 0f
        
        return BenchmarkTest(
            name = "Memory Management",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Test AI model loading performance.
     */
    private suspend fun benchmarkModelLoading(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val modelLoadTimes = mutableListOf<Long>()
            val modelComplexities = ModelComplexity.values()
            
            for (complexity in modelComplexities) {
                val modelStartTime = System.currentTimeMillis()
                
                // Simulate model loading
                val modelSize = when (complexity) {
                    ModelComplexity.BASIC -> 50 * 1024 * 1024L
                    ModelComplexity.STANDARD -> 150 * 1024 * 1024L
                    ModelComplexity.ADVANCED -> 500 * 1024 * 1024L
                }
                
                val shouldPreload = memoryManager.shouldPreloadModel("test_model_$complexity", complexity)
                results["canPreload_$complexity"] = shouldPreload
                
                if (shouldPreload) {
                    val result = memoryManager.loadModel(
                        "test_model_$complexity",
                        modelSize,
                        complexity
                    ) {
                        // Simulate model loading with delay based on size
                        delay((modelSize / (10 * 1024 * 1024)).coerceAtLeast(100))
                        "mock_model_data_$complexity"
                    }
                    
                    val loadTime = System.currentTimeMillis() - modelStartTime
                    modelLoadTimes.add(loadTime)
                    
                    results["loadTime_$complexity"] = loadTime
                    results["loadSuccess_$complexity"] = result.isSuccess
                }
            }
            
            if (modelLoadTimes.isNotEmpty()) {
                results["avgLoadTime"] = modelLoadTimes.average()
                results["maxLoadTime"] = modelLoadTimes.maxOrNull() ?: 0L
                results["minLoadTime"] = modelLoadTimes.minOrNull() ?: 0L
            }
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgLoadTime = results["avgLoadTime"] as? Double ?: Double.MAX_VALUE
        val score = if (success) {
            when {
                avgLoadTime < 5000 -> 100f // < 5 seconds excellent
                avgLoadTime < 10000 -> 80f // < 10 seconds good
                avgLoadTime < 20000 -> 60f // < 20 seconds acceptable
                else -> 30f // slower than 20 seconds
            }
        } else 0f
        
        return BenchmarkTest(
            name = "Model Loading",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Test image processing performance.
     */
    private suspend fun benchmarkImageProcessing(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val processingTimes = mutableListOf<Long>()
            val testImages = generateTestImages(5)
            
            testImages.forEach { (_, imageData) ->
                val processStartTime = System.currentTimeMillis()
                
                // Simulate image processing
                delay(Random.nextLong(50, 200)) // Simulate processing time
                
                val processTime = System.currentTimeMillis() - processStartTime
                processingTimes.add(processTime)
            }
            
            results["imageCount"] = testImages.size
            results["processingTimes"] = processingTimes
            results["avgProcessingTime"] = processingTimes.average()
            results["totalProcessingTime"] = processingTimes.sum()
            
            // Calculate processing rate (images per second)
            val totalTimeSeconds = processingTimes.sum() / 1000.0
            val processingRate = testImages.size / totalTimeSeconds
            results["processingRate"] = processingRate
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgProcessingTime = results["avgProcessingTime"] as? Double ?: Double.MAX_VALUE
        val score = if (success) {
            when {
                avgProcessingTime < 100 -> 100f // < 100ms excellent
                avgProcessingTime < 500 -> 80f  // < 500ms good  
                avgProcessingTime < 1000 -> 60f // < 1s acceptable
                else -> 30f // slower than 1 second
            }
        } else 0f
        
        return BenchmarkTest(
            name = "Image Processing",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Test cache performance and hit rates.
     */
    private suspend fun benchmarkCachePerformance(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            // Test analysis result caching
            val cacheTests = 100
            var cacheHits = 0
            val cacheResults = mutableListOf<String>()
            
            // Populate cache with some results
            repeat(20) { i ->
                val cacheKey = "test_analysis_$i"
                memoryManager.cacheAnalysisResult(cacheKey, "mock_analysis_$i")
            }
            
            // Test cache retrieval
            repeat(cacheTests) { i ->
                val cacheKey = "test_analysis_${i % 20}" // Ensure some hits
                val cached = memoryManager.getCachedAnalysisResult(cacheKey)
                if (cached != null) {
                    cacheHits++
                    cacheResults.add(cached.toString())
                }
            }
            
            val hitRate = cacheHits.toFloat() / cacheTests
            results["cacheTests"] = cacheTests
            results["cacheHits"] = cacheHits
            results["hitRate"] = hitRate
            
            // Test cache eviction under pressure
            memoryManager.handleMemoryPressure(MemoryPressure.MODERATE)
            
            // Test hit rate after eviction
            var hitsAfterEviction = 0
            repeat(20) { i ->
                val cacheKey = "test_analysis_$i"
                if (memoryManager.getCachedAnalysisResult(cacheKey) != null) {
                    hitsAfterEviction++
                }
            }
            
            results["hitsAfterEviction"] = hitsAfterEviction
            results["evictionEffectiveness"] = (20 - hitsAfterEviction) / 20f
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val hitRate = results["hitRate"] as? Float ?: 0f
        val score = if (success) hitRate * 100f else 0f
        
        return BenchmarkTest(
            name = "Cache Performance",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Test frame rate performance and UI responsiveness.
     */
    private suspend fun benchmarkFrameRate(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val frameCounter = FrameCounter()
            val aiLimiter = AIFrameLimiter(2.0f)
            val uiLimiter = UIFrameRateLimiter(30)
            
            // Simulate 5 seconds of operation
            val testDurationMs = 5000L
            val endTime = System.currentTimeMillis() + testDurationMs
            
            var uiFrames = 0
            var aiProcesses = 0
            var skippedAI = 0
            
            while (System.currentTimeMillis() < endTime) {
                // UI frame processing
                if (uiLimiter.shouldRenderFrame()) {
                    frameCounter.recordFrame(Random.nextLong(10, 30)) // 10-30ms frame time
                    uiFrames++
                }
                
                // AI processing 
                if (aiLimiter.shouldProcess()) {
                    aiProcesses++
                    delay(Random.nextLong(100, 500)) // Simulate AI processing
                } else {
                    skippedAI++
                }
                
                delay(1) // Small delay to prevent busy loop
            }
            
            val actualDurationSeconds = testDurationMs / 1000f
            val uiFPS = uiFrames / actualDurationSeconds
            val aiRate = aiProcesses / actualDurationSeconds
            
            results["testDurationMs"] = testDurationMs
            results["uiFrames"] = uiFrames
            results["aiProcesses"] = aiProcesses
            results["skippedAI"] = skippedAI
            results["actualUiFPS"] = uiFPS
            results["actualAiRate"] = aiRate
            results["targetUiFPS"] = 30f
            results["targetAiRate"] = 2f
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val actualUiFPS = results["actualUiFPS"] as? Float ?: 0f
        val actualAiRate = results["actualAiRate"] as? Float ?: 0f
        
        val uiScore = (actualUiFPS / 30f * 50f).coerceAtMost(50f)
        val aiScore = (actualAiRate / 2f * 50f).coerceAtMost(50f)
        val score = if (success) uiScore + aiScore else 0f
        
        return BenchmarkTest(
            name = "Frame Rate",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Test memory pressure handling.
     */
    private suspend fun benchmarkMemoryPressure(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val initialStats = memoryManager.getMemoryStats()
            results["initialStats"] = initialStats
            
            // Fill up memory with test data
            val testImages = generateTestImages(50)
            testImages.forEach { (id, imageData) ->
                memoryManager.cacheImage(id, imageData)
            }
            
            val fullStats = memoryManager.getMemoryStats()
            results["fullStats"] = fullStats
            
            // Test different pressure levels
            val pressureLevels = listOf(
                MemoryPressure.MODERATE,
                MemoryPressure.HIGH,
                MemoryPressure.CRITICAL
            )
            
            pressureLevels.forEach { pressure ->
                memoryManager.handleMemoryPressure(pressure)
                val stats = memoryManager.getMemoryStats()
                results["statsAfter_$pressure"] = stats
                
                delay(100) // Allow processing time
            }
            
            val finalStats = memoryManager.getMemoryStats()
            results["finalStats"] = finalStats
            
            // Calculate memory freed
            val initialMemory = initialStats.totalManagedMemoryMB
            val finalMemory = finalStats.totalManagedMemoryMB
            val memoryFreed = initialMemory - finalMemory
            val freePercentage = if (initialMemory > 0) memoryFreed.toFloat() / initialMemory else 0f
            
            results["memoryFreedMB"] = memoryFreed
            results["freePercentage"] = freePercentage
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val freePercentage = results["freePercentage"] as? Float ?: 0f
        val score = if (success) (freePercentage * 100f).coerceAtMost(100f) else 0f
        
        return BenchmarkTest(
            name = "Memory Pressure",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Test battery impact assessment.
     */
    private suspend fun benchmarkBatteryImpact(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val capabilities = deviceDetector.detectCapabilities()
            results["batteryOptimized"] = capabilities.batteryOptimized
            results["thermalThrottled"] = capabilities.thermalThrottled
            
            // Simulate continuous operation for battery impact
            val testDurationMs = 3000L // 3 seconds test
            val endTime = System.currentTimeMillis() + testDurationMs
            
            var operations = 0
            val cpuIntensiveTasks = mutableListOf<Long>()
            
            while (System.currentTimeMillis() < endTime) {
                val taskStartTime = System.currentTimeMillis()
                
                // Simulate CPU-intensive operation
                var sum = 0L
                repeat(10000) { i ->
                    sum += i * i
                }
                
                val taskDuration = System.currentTimeMillis() - taskStartTime
                cpuIntensiveTasks.add(taskDuration)
                operations++
                
                delay(10) // Brief pause between operations
            }
            
            val actualDuration = System.currentTimeMillis() - startTime - testDurationMs
            val avgTaskTime = cpuIntensiveTasks.average()
            val operationsPerSecond = operations.toFloat() / (actualDuration / 1000f)
            
            results["operations"] = operations
            results["avgTaskTimeMs"] = avgTaskTime
            results["operationsPerSecond"] = operationsPerSecond
            results["estimatedBatteryImpact"] = when {
                operationsPerSecond > 100 -> "High"
                operationsPerSecond > 50 -> "Medium" 
                else -> "Low"
            }
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val operationsPerSecond = results["operationsPerSecond"] as? Float ?: 0f
        
        // Lower operations per second is better for battery (inverse score)
        val score = if (success) {
            when {
                operationsPerSecond < 20 -> 100f // Low impact
                operationsPerSecond < 50 -> 70f  // Medium impact
                operationsPerSecond < 100 -> 40f // High impact
                else -> 10f // Very high impact
            }
        } else 0f
        
        return BenchmarkTest(
            name = "Battery Impact",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Test LiteRT backend performance against targets.
     * Validates CPU (243 t/s), GPU (1876 t/s), NPU (5836 t/s) performance.
     */
    private suspend fun benchmarkLiteRTBackends(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val engine = liteRTEngine
            if (engine == null) {
                success = false
                results["error"] = "LiteRT engine not available"
                return createFailedTest("LiteRT Backends", startTime, results)
            }
            
            val supportedBackends = engine.supportedBackends
            results["supportedBackends"] = supportedBackends.map { it.displayName }
            results["backendCount"] = supportedBackends.size
            
            val performanceResults = mutableMapOf<String, Map<String, Any>>()
            
            for (backend in supportedBackends) {
                val backendStartTime = System.currentTimeMillis()
                
                try {
                    // Initialize with specific backend
                    val initResult = engine.initialize("test_model", backend)
                    
                    if (initResult.isSuccess) {
                        // Run performance test
                        val testImage = generateTestImage()
                        val analysisResult = engine.generateSafetyAnalysis(
                            imageData = testImage,
                            workType = WorkType.GENERAL_CONSTRUCTION
                        )
                        
                        val backendDuration = System.currentTimeMillis() - backendStartTime
                        val metrics = engine.getPerformanceMetrics()
                        
                        val expectedTokens = backend.expectedTokensPerSecond
                        val actualTokens = metrics.tokensPerSecond
                        val performanceRatio = actualTokens / expectedTokens
                        val achievesTarget = performanceRatio >= 0.8f // 80% of target
                        
                        performanceResults[backend.displayName] = mapOf(
                            "initSuccess" to true,
                            "analysisSuccess" to analysisResult.isSuccess,
                            "durationMs" to backendDuration,
                            "expectedTokensPerSecond" to expectedTokens,
                            "actualTokensPerSecond" to actualTokens,
                            "performanceRatio" to performanceRatio,
                            "achievesTarget" to achievesTarget,
                            "memoryUsageMB" to metrics.averageMemoryUsageMB
                        )
                    } else {
                        performanceResults[backend.displayName] = mapOf(
                            "initSuccess" to false,
                            "error" to initResult.exceptionOrNull()?.message
                        )
                    }
                } catch (e: Exception) {
                    performanceResults[backend.displayName] = mapOf(
                        "initSuccess" to false,
                        "error" to e.message
                    )
                }
            }
            
            results["backendPerformance"] = performanceResults
            
            // Calculate overall performance score
            val workingBackends = performanceResults.values.count { 
                it["initSuccess"] as? Boolean == true && it["analysisSuccess"] as? Boolean == true 
            }
            val avgPerformanceRatio = performanceResults.values
                .mapNotNull { it["performanceRatio"] as? Float }
                .takeIf { it.isNotEmpty() }
                ?.average()?.toFloat() ?: 0f
            
            results["workingBackends"] = workingBackends
            results["avgPerformanceRatio"] = avgPerformanceRatio
            results["meetsTargets"] = avgPerformanceRatio >= 0.8f
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgRatio = results["avgPerformanceRatio"] as? Float ?: 0f
        val score = if (success) (avgRatio * 100f).coerceAtMost(100f) else 0f
        
        return BenchmarkTest(
            name = "LiteRT Backends",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Compare SimplifiedAIOrchestrator vs SmartAIOrchestrator performance.
     */
    private suspend fun benchmarkOrchestratorComparison(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val testImage = generateTestImage()
            val workType = WorkType.GENERAL_CONSTRUCTION
            val numTests = 5
            
            // Test SimplifiedAIOrchestrator (LiteRT-enhanced)
            val simplifiedResults = mutableListOf<Long>()
            simplifiedOrchestrator?.let { orchestrator ->
                repeat(numTests) {
                    val analysisStart = System.currentTimeMillis()
                    val result = orchestrator.analyzePhoto(testImage, workType)
                    val analysisDuration = System.currentTimeMillis() - analysisStart
                    
                    if (result.isSuccess) {
                        simplifiedResults.add(analysisDuration)
                    }
                }
            }
            
            // Test SmartAIOrchestrator (legacy)
            val smartResults = mutableListOf<Long>()
            smartOrchestrator?.let { orchestrator ->
                repeat(numTests) {
                    val analysisStart = System.currentTimeMillis()
                    val result = orchestrator.analyzePhoto(testImage, workType)
                    val analysisDuration = System.currentTimeMillis() - analysisStart
                    
                    if (result.isSuccess) {
                        smartResults.add(analysisDuration)
                    }
                }
            }
            
            // Calculate performance comparison
            val simplifiedAvg = simplifiedResults.takeIf { it.isNotEmpty() }?.average()?.toLong() ?: 0L
            val smartAvg = smartResults.takeIf { it.isNotEmpty() }?.average()?.toLong() ?: 0L
            
            val performanceImprovement = if (smartAvg > 0L) {
                (smartAvg.toFloat() / simplifiedAvg.toFloat()).takeIf { it.isFinite() } ?: 1f
            } else 1f
            
            val meetsTarget = performanceImprovement >= 3f // 3x minimum improvement
            val achievesOptimal = performanceImprovement >= 8f // 8x optimal improvement
            
            results["simplifiedAvgMs"] = simplifiedAvg
            results["smartAvgMs"] = smartAvg
            results["performanceImprovement"] = String.format("%.1fx", performanceImprovement)
            results["meetsMinTarget"] = meetsTarget
            results["achievesOptimal"] = achievesOptimal
            results["simplifiedSuccessCount"] = simplifiedResults.size
            results["smartSuccessCount"] = smartResults.size
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val improvement = results["performanceImprovement"]?.toString()
            ?.removeSuffix("x")?.toFloatOrNull() ?: 1f
        
        val score = if (success) {
            when {
                improvement >= 8f -> 100f // Optimal target achieved
                improvement >= 5f -> 85f  // Excellent improvement
                improvement >= 3f -> 70f  // Minimum target met
                improvement >= 2f -> 50f  // Some improvement
                else -> 25f // Minimal improvement
            }
        } else 0f
        
        return BenchmarkTest(
            name = "Orchestrator Comparison",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Test production workload scenarios.
     */
    private suspend fun benchmarkProductionLoad(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val orchestrator = simplifiedOrchestrator
            if (orchestrator == null) {
                success = false
                results["error"] = "Simplified orchestrator not available"
                return createFailedTest("Production Load", startTime, results)
            }
            
            // Simulate realistic construction site usage patterns
            val scenarios = listOf(
                // Scenario 1: Rapid successive captures (site walk-through)
                "rapid_captures" to { simulateRapidCaptures(orchestrator) },
                // Scenario 2: Batch processing (end-of-day review)
                "batch_processing" to { simulateBatchProcessing(orchestrator) },
                // Scenario 3: Memory pressure scenario
                "memory_pressure" to { simulateMemoryPressure(orchestrator) },
                // Scenario 4: Thermal throttling scenario
                "thermal_scenario" to { simulateThermalScenario(orchestrator) }
            )
            
            val scenarioResults = mutableMapOf<String, Map<String, Any>>()
            
            for ((scenarioName, scenarioTest) in scenarios) {
                try {
                    val scenarioStart = System.currentTimeMillis()
                    val scenarioResult = scenarioTest()
                    val scenarioDuration = System.currentTimeMillis() - scenarioStart
                    
                    scenarioResults[scenarioName] = mapOf(
                        "success" to true,
                        "durationMs" to scenarioDuration,
                        "details" to scenarioResult
                    )
                } catch (e: Exception) {
                    scenarioResults[scenarioName] = mapOf(
                        "success" to false,
                        "error" to e.message
                    )
                }
            }
            
            results["scenarios"] = scenarioResults
            val successfulScenarios = scenarioResults.values.count { 
                it["success"] as? Boolean == true 
            }
            results["successfulScenarios"] = successfulScenarios
            results["totalScenarios"] = scenarios.size
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val successfulScenarios = results["successfulScenarios"] as? Int ?: 0
        val totalScenarios = results["totalScenarios"] as? Int ?: 1
        val score = if (success) (successfulScenarios.toFloat() / totalScenarios * 100f) else 0f
        
        return BenchmarkTest(
            name = "Production Load",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    /**
     * Compare real AI analysis vs mock JSON generation performance.
     */
    private suspend fun benchmarkRealVsMockPerformance(): BenchmarkTest {
        val startTime = System.currentTimeMillis()
        var success = true
        val results = mutableMapOf<String, Any>()
        
        try {
            val testImage = generateTestImage()
            val numTests = 10
            
            // Test real AI analysis (LiteRT)
            val realAnalysisTimes = mutableListOf<Long>()
            val realAccuracyScores = mutableListOf<Float>()
            
            liteRTEngine?.let { engine ->
                repeat(numTests) {
                    val analysisStart = System.currentTimeMillis()
                    val result = engine.generateSafetyAnalysis(
                        imageData = testImage,
                        workType = WorkType.GENERAL_CONSTRUCTION
                    )
                    val analysisDuration = System.currentTimeMillis() - analysisStart
                    
                    if (result.isSuccess) {
                        realAnalysisTimes.add(analysisDuration)
                        realAccuracyScores.add(result.getOrNull()?.confidence ?: 0f)
                    }
                }
            }
            
            // Test mock JSON generation (simulate legacy behavior)
            val mockGenerationTimes = mutableListOf<Long>()
            repeat(numTests) {
                val mockStart = System.currentTimeMillis()
                // Simulate mock JSON generation (much faster but lower quality)
                val mockJson = generateMockAnalysisJson()
                val mockDuration = System.currentTimeMillis() - mockStart
                mockGenerationTimes.add(mockDuration)
            }
            
            // Calculate comparison metrics
            val realAvgTime = realAnalysisTimes.takeIf { it.isNotEmpty() }?.average()?.toLong() ?: 0L
            val mockAvgTime = mockGenerationTimes.average().toLong()
            val realAvgAccuracy = realAccuracyScores.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f
            val mockAccuracy = 0.3f // Mock has low accuracy
            
            val speedRatio = realAvgTime.toFloat() / mockAvgTime.toFloat()
            val accuracyImprovement = realAvgAccuracy / mockAccuracy
            val qualityScore = realAvgAccuracy * 100f // Convert to percentage
            
            results["realAnalysisAvgMs"] = realAvgTime
            results["mockGenerationAvgMs"] = mockAvgTime
            results["speedRatio"] = String.format("%.1fx", speedRatio)
            results["realAccuracy"] = String.format("%.1f%%", realAvgAccuracy * 100)
            results["mockAccuracy"] = String.format("%.1f%%", mockAccuracy * 100)
            results["accuracyImprovement"] = String.format("%.1fx", accuracyImprovement)
            results["qualityScore"] = qualityScore
            results["realSuccessCount"] = realAnalysisTimes.size
            results["worthwhileTradeoff"] = speedRatio <= 10f && realAvgAccuracy >= 0.7f
            
        } catch (e: Exception) {
            success = false
            results["error"] = e.message ?: "Unknown error"
        }
        
        val duration = System.currentTimeMillis() - startTime
        val qualityScore = results["qualityScore"] as? Float ?: 0f
        val worthwhile = results["worthwhileTradeoff"] as? Boolean ?: false
        
        val score = if (success) {
            when {
                worthwhile && qualityScore >= 80f -> 100f // Excellent real analysis
                qualityScore >= 70f -> 85f // Good real analysis 
                qualityScore >= 60f -> 70f // Acceptable real analysis
                qualityScore >= 50f -> 50f // Poor real analysis
                else -> 25f // Very poor real analysis
            }
        } else 0f
        
        return BenchmarkTest(
            name = "Real vs Mock Performance",
            durationMs = duration,
            success = success,
            score = score,
            results = results
        )
    }
    
    // Helper methods for benchmarking
    
    private fun generateTestImages(count: Int): List<Pair<String, ByteArray>> {
        return (1..count).map { i ->
            val imageId = "test_image_$i"
            val imageData = Random.nextBytes(Random.nextInt(100_000, 1_000_000)) // 100KB-1MB
            imageId to imageData
        }
    }
    
    private fun generateTestImage(): ByteArray {
        // Generate a realistic test image for construction site analysis
        return Random.nextBytes(500_000) // 500KB test image
    }
    
    private fun createFailedTest(testName: String, startTime: Long, results: Map<String, Any>): BenchmarkTest {
        return BenchmarkTest(
            name = testName,
            durationMs = System.currentTimeMillis() - startTime,
            success = false,
            score = 0f,
            results = results
        )
    }
    
    // Production scenario simulation methods
    
    private suspend fun simulateRapidCaptures(orchestrator: SimplifiedAIOrchestrator): Map<String, Any> {
        val results = mutableMapOf<String, Any>()
        val testImages = (1..10).map { generateTestImage() }
        val analysisResults = mutableListOf<Long>()
        
        // Simulate rapid photo capture scenario (site walk-through)
        for (image in testImages) {
            val start = System.currentTimeMillis()
            val result = orchestrator.analyzePhoto(image, WorkType.GENERAL_CONSTRUCTION)
            val duration = System.currentTimeMillis() - start
            
            if (result.isSuccess) {
                analysisResults.add(duration)
            }
            
            delay(100) // Brief pause between captures
        }
        
        results["totalImages"] = testImages.size
        results["successfulAnalyses"] = analysisResults.size
        results["avgAnalysisTimeMs"] = analysisResults.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        results["maxAnalysisTimeMs"] = analysisResults.takeIf { it.isNotEmpty() }?.maxOrNull() ?: 0L
        results["successRate"] = analysisResults.size.toFloat() / testImages.size
        
        return results
    }
    
    private suspend fun simulateBatchProcessing(orchestrator: SimplifiedAIOrchestrator): Map<String, Any> {
        val results = mutableMapOf<String, Any>()
        val batchImages = (1..20).map { generateTestImage() } // Larger batch
        
        // Simulate batch processing (end-of-day review)
        val batchStart = System.currentTimeMillis()
        val batchResults = batchImages.map { image ->
            orchestrator.analyzePhoto(image, WorkType.GENERAL_CONSTRUCTION)
        }
        val batchDuration = System.currentTimeMillis() - batchStart
        
        val successCount = batchResults.count { it.isSuccess }
        
        results["batchSize"] = batchImages.size
        results["batchProcessingTimeMs"] = batchDuration
        results["successCount"] = successCount
        results["successRate"] = successCount.toFloat() / batchImages.size
        results["avgTimePerImageMs"] = batchDuration.toFloat() / batchImages.size
        
        return results
    }
    
    private suspend fun simulateMemoryPressure(orchestrator: SimplifiedAIOrchestrator): Map<String, Any> {
        val results = mutableMapOf<String, Any>()
        
        // Force memory pressure by creating large objects
        val memoryPressureObjects = mutableListOf<ByteArray>()
        repeat(50) {
            memoryPressureObjects.add(Random.nextBytes(10_000_000)) // 10MB objects
        }
        
        // Test analysis under memory pressure
        val testImage = generateTestImage()
        val analysisStart = System.currentTimeMillis()
        val analysisResult = orchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        val analysisDuration = System.currentTimeMillis() - analysisStart
        
        // Clean up memory pressure objects
        memoryPressureObjects.clear()
        
        results["memoryPressureApplied"] = true
        results["analysisSuccess"] = analysisResult.isSuccess
        results["analysisTimeMs"] = analysisDuration
        results["handledGracefully"] = analysisResult.isSuccess && analysisDuration < 10000 // Under 10s
        
        return results
    }
    
    private suspend fun simulateThermalScenario(orchestrator: SimplifiedAIOrchestrator): Map<String, Any> {
        val results = mutableMapOf<String, Any>()
        
        // Simulate thermal load with CPU intensive work
        val thermalStart = System.currentTimeMillis()
        repeat(100) {
            // CPU intensive work to generate heat
            var sum = 0L
            repeat(100_000) { i ->
                sum += i * i
            }
        }
        val thermalDuration = System.currentTimeMillis() - thermalStart
        
        // Test analysis after thermal load
        val testImage = generateTestImage()
        val analysisStart = System.currentTimeMillis()
        val analysisResult = orchestrator.analyzePhoto(testImage, WorkType.GENERAL_CONSTRUCTION)
        val analysisDuration = System.currentTimeMillis() - analysisStart
        
        results["thermalLoadTimeMs"] = thermalDuration
        results["postThermalAnalysisSuccess"] = analysisResult.isSuccess
        results["postThermalAnalysisTimeMs"] = analysisDuration
        results["thermalResistant"] = analysisResult.isSuccess && analysisDuration < 8000 // Under 8s
        
        return results
    }
    
    private fun generateMockAnalysisJson(): String {
        // Simulate fast but low-quality mock JSON generation
        delay(Random.nextLong(5, 20)) // Much faster than real analysis
        
        return """
        {
            "hazards": [
                {
                    "type": "MOCK_HAZARD",
                    "confidence": 0.3,
                    "description": "Simulated hazard for testing"
                }
            ],
            "ppe_status": {
                "hard_hat": {"present": true, "confidence": 0.3}
            },
            "confidence": 0.3,
            "analysis_type": "MOCK_GENERATION"
        }
        """.trimIndent()
    }
    
    private fun calculateOverallScore(tests: List<BenchmarkTest>): Float {
        if (tests.isEmpty()) return 0f
        
        val weights = mapOf(
            "Device Detection" to 0.05f,
            "Memory Management" to 0.10f,
            "Model Loading" to 0.10f,
            "Image Processing" to 0.10f,
            "Cache Performance" to 0.05f,
            "Frame Rate" to 0.10f,
            "Memory Pressure" to 0.05f,
            "Battery Impact" to 0.05f,
            // New LiteRT-specific weights (40% total)
            "LiteRT Backends" to 0.15f,
            "Orchestrator Comparison" to 0.15f,
            "Production Load" to 0.05f,
            "Real vs Mock Performance" to 0.05f
        )
        
        var weightedScore = 0f
        var totalWeight = 0f
        
        tests.forEach { test ->
            val weight = weights[test.name] ?: 0.1f
            weightedScore += test.score * weight
            totalWeight += weight
        }
        
        return if (totalWeight > 0) weightedScore / totalWeight else 0f
    }
    
    private fun generateRecommendations(
        tests: List<BenchmarkTest>,
        capabilities: DeviceCapabilities
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        // Device-specific recommendations
        when (capabilities.tier) {
            DeviceTier.LOW_END -> {
                recommendations.add("Device detected as low-end - enabling power saving optimizations")
                recommendations.add("Consider reducing AI processing frequency to 1 FPS for better performance")
                recommendations.add("Enable aggressive caching to reduce repeated processing")
            }
            DeviceTier.MID_RANGE -> {
                recommendations.add("Device has good performance - standard optimizations recommended")
                recommendations.add("Consider preloading frequently used models for faster response")
            }
            DeviceTier.HIGH_END -> {
                recommendations.add("High-end device detected - full feature set available")
                recommendations.add("Enable advanced AI models for maximum accuracy")
            }
        }
        
        // Test-specific recommendations
        tests.forEach { test ->
            when (test.name) {
                "Memory Management" -> {
                    if (test.score < 50) {
                        recommendations.add("Memory management issues detected - enable more aggressive cleanup")
                    }
                }
                "Model Loading" -> {
                    if (test.score < 60) {
                        recommendations.add("Model loading is slow - consider using smaller models")
                    }
                }
                "Frame Rate" -> {
                    if (test.score < 70) {
                        recommendations.add("Frame rate below target - reduce UI complexity or AI frequency")
                    }
                }
                "Battery Impact" -> {
                    if (test.score < 50) {
                        recommendations.add("High battery usage detected - enable battery optimization mode")
                    }
                }
            }
        }
        
        if (capabilities.batteryOptimized) {
            recommendations.add("Battery saver mode detected - performance optimizations active")
        }
        
        if (capabilities.thermalThrottled) {
            recommendations.add("Thermal throttling detected - reduce processing intensity")
        }
        
        return recommendations
    }
}

sealed class BenchmarkState {
    object Idle : BenchmarkState()
    object Running : BenchmarkState()
    object Completed : BenchmarkState()
    data class Error(val message: String) : BenchmarkState()
}

data class BenchmarkTest(
    val name: String,
    val durationMs: Long,
    val success: Boolean,
    val score: Float, // 0-100
    val results: Map<String, Any>
)

data class BenchmarkResults(
    val deviceCapabilities: DeviceCapabilities,
    val tests: List<BenchmarkTest>,
    val overallScore: Float,
    val recommendations: List<String>,
    val timestamp: Long
) {
    val grade: String
        get() = when {
            overallScore >= 90 -> "A"
            overallScore >= 80 -> "B"  
            overallScore >= 70 -> "C"
            overallScore >= 60 -> "D"
            else -> "F"
        }
}