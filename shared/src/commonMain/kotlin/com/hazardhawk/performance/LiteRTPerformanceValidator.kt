package com.hazardhawk.performance

import com.hazardhawk.ai.core.SimplifiedAIOrchestrator
import com.hazardhawk.ai.core.SmartAIOrchestrator
import com.hazardhawk.ai.litert.LiteRTModelEngine
import com.hazardhawk.ai.litert.LiteRTBackend
import com.hazardhawk.core.models.WorkType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/**
 * Comprehensive LiteRT performance validator that ensures the 3-8x performance improvement targets are met.
 * 
 * VALIDATION TARGETS:
 * - CPU Backend: 243 tokens/sec baseline
 * - GPU Backend: 1876 tokens/sec (7.7x improvement)
 * - NPU Backend: 5836 tokens/sec (24x improvement)
 * - SimplifiedAIOrchestrator vs SmartAIOrchestrator: 3-8x improvement
 * - Real AI analysis vs mock generation: Quality improvement with acceptable performance trade-off
 * - Production workload handling: Stress testing under real conditions
 * 
 * This validator provides comprehensive performance analysis and recommendations
 * for optimizing HazardHawk's AI analysis pipeline across different device tiers.
 */
class LiteRTPerformanceValidator(
    private val simplifiedOrchestrator: SimplifiedAIOrchestrator? = null,
    private val smartOrchestrator: SmartAIOrchestrator? = null,
    private val liteRTEngine: LiteRTModelEngine? = null,
    private val deviceDetector: DeviceTierDetector,
    private val performanceMonitor: PerformanceMonitor
) {
    
    private val _validationProgress = MutableStateFlow(0f)
    val validationProgress: StateFlow<Float> = _validationProgress
    
    private val _currentTest = MutableStateFlow("Initializing...")
    val currentTest: StateFlow<String> = _currentTest
    
    /**
     * Execute complete LiteRT performance validation suite.
     * Returns comprehensive analysis of whether performance targets are met.
     */
    suspend fun executeCompleteValidation(): LiteRTValidationReport = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val deviceCapabilities = deviceDetector.detectCapabilities()
        
        _validationProgress.value = 0f
        _currentTest.value = "Starting validation..."
        
        val validationResults = mutableListOf<LiteRTValidationResult>()
        val totalTests = 6
        var completedTests = 0
        
        try {
            // Test 1: Backend Performance Validation
            _currentTest.value = "Testing LiteRT backend performance..."
            val backendResult = validateBackendPerformance()
            validationResults.add(backendResult)
            completedTests++
            _validationProgress.value = completedTests.toFloat() / totalTests
            
            // Test 2: Orchestrator Comparison
            _currentTest.value = "Comparing orchestrator performance..."
            val orchestratorResult = validateOrchestratorComparison()
            validationResults.add(orchestratorResult)
            completedTests++
            _validationProgress.value = completedTests.toFloat() / totalTests
            
            // Test 3: Real vs Mock Analysis
            _currentTest.value = "Testing real AI vs mock performance..."
            val realVsMockResult = validateRealVsMockPerformance()
            validationResults.add(realVsMockResult)
            completedTests++
            _validationProgress.value = completedTests.toFloat() / totalTests
            
            // Test 4: Device Optimization
            _currentTest.value = "Validating device-specific optimizations..."
            val deviceOptResult = validateDeviceOptimization()
            validationResults.add(deviceOptResult)
            completedTests++
            _validationProgress.value = completedTests.toFloat() / totalTests
            
            // Test 5: Production Load Testing
            _currentTest.value = "Testing production workload scenarios..."
            val productionResult = validateProductionLoad()
            validationResults.add(productionResult)
            completedTests++
            _validationProgress.value = completedTests.toFloat() / totalTests
            
            // Test 6: Memory and Thermal Optimization
            _currentTest.value = "Testing memory and thermal optimization..."
            val memoryThermalResult = validateMemoryThermalOptimization()
            validationResults.add(memoryThermalResult)
            completedTests++
            _validationProgress.value = 1f
            
            _currentTest.value = "Generating final report..."
            
            val overallScore = validationResults.map { it.score }.average().toFloat()
            val allTargetsMet = validationResults.all { it.targetsMet }
            val criticalIssues = validationResults.filter { !it.targetsMet }
                .flatMap { it.issues }
            
            LiteRTValidationReport(
                timestamp = System.currentTimeMillis(),
                validationDurationMs = System.currentTimeMillis() - startTime,
                deviceCapabilities = deviceCapabilities,
                validationResults = validationResults,
                overallScore = overallScore,
                allTargetsMet = allTargetsMet,
                performanceGrade = calculatePerformanceGrade(overallScore),
                criticalIssues = criticalIssues,
                recommendations = generateOptimizationRecommendations(validationResults, deviceCapabilities)
            )
            
        } catch (e: Exception) {
            _currentTest.value = "Validation failed: ${e.message}"
            
            LiteRTValidationReport(
                timestamp = System.currentTimeMillis(),
                validationDurationMs = System.currentTimeMillis() - startTime,
                deviceCapabilities = deviceCapabilities,
                validationResults = validationResults,
                overallScore = 0f,
                allTargetsMet = false,
                performanceGrade = "F",
                criticalIssues = listOf("Validation execution failed: ${e.message}"),
                recommendations = listOf("Fix validation setup and retry")
            )
        }
    }
    
    /**
     * Validate LiteRT backend performance against targets.
     */
    private suspend fun validateBackendPerformance(): LiteRTValidationResult {
        val results = mutableMapOf<String, Any>()
        val issues = mutableListOf<String>()
        var overallScore = 0f
        
        val engine = liteRTEngine
        if (engine == null) {
            return LiteRTValidationResult(
                testName = "Backend Performance",
                score = 0f,
                targetsMet = false,
                results = mapOf("error" to "LiteRT engine not available"),
                issues = listOf("LiteRT engine is not initialized or available")
            )
        }
        
        val supportedBackends = engine.supportedBackends
        val backendScores = mutableListOf<Float>()
        
        for (backend in supportedBackends) {
            try {
                val backendStart = System.currentTimeMillis()
                
                // Initialize backend
                val initResult = engine.initialize("performance_test_model", backend)
                if (!initResult.isSuccess) {
                    results["${backend.displayName}_init"] = "Failed: ${initResult.exceptionOrNull()?.message}"
                    continue
                }
                
                // Run performance test
                val testImage = generateTestImage()
                val analysisResult = engine.generateSafetyAnalysis(
                    imageData = testImage,
                    workType = WorkType.GENERAL_CONSTRUCTION
                )
                
                val testDuration = System.currentTimeMillis() - backendStart
                val metrics = engine.getPerformanceMetrics()
                
                val expectedTokens = backend.expectedTokensPerSecond
                val actualTokens = metrics.tokensPerSecond
                val performanceRatio = actualTokens / expectedTokens
                val achievesTarget = performanceRatio >= 0.8f // 80% of target acceptable
                
                val backendScore = (performanceRatio * 100f).coerceAtMost(100f)
                backendScores.add(backendScore)
                
                results["${backend.displayName}_expectedTokens"] = expectedTokens
                results["${backend.displayName}_actualTokens"] = actualTokens
                results["${backend.displayName}_performanceRatio"] = performanceRatio
                results["${backend.displayName}_achievesTarget"] = achievesTarget
                results["${backend.displayName}_score"] = backendScore
                results["${backend.displayName}_memoryMB"] = metrics.averageMemoryUsageMB
                
                if (!achievesTarget) {
                    issues.add("${backend.displayName} achieves only ${String.format("%.1f%%", performanceRatio * 100)} of target performance")
                }
                
                // Clean up
                engine.cleanup()
                
            } catch (e: Exception) {
                results["${backend.displayName}_error"] = e.message
                issues.add("${backend.displayName} test failed: ${e.message}")
            }
        }
        
        overallScore = backendScores.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: 0f
        val targetsMet = backendScores.isNotEmpty() && backendScores.all { it >= 80f }
        
        results["supportedBackends"] = supportedBackends.size
        results["testedBackends"] = backendScores.size
        results["overallScore"] = overallScore
        
        return LiteRTValidationResult(
            testName = "Backend Performance",
            score = overallScore,
            targetsMet = targetsMet,
            results = results,
            issues = issues
        )
    }
    
    /**
     * Validate performance improvement of SimplifiedAIOrchestrator vs SmartAIOrchestrator.
     */
    private suspend fun validateOrchestratorComparison(): LiteRTValidationResult {
        val results = mutableMapOf<String, Any>()
        val issues = mutableListOf<String>()
        
        if (simplifiedOrchestrator == null || smartOrchestrator == null) {
            return LiteRTValidationResult(
                testName = "Orchestrator Comparison",
                score = 0f,
                targetsMet = false,
                results = mapOf("error" to "Orchestrators not available"),
                issues = listOf("Both SimplifiedAIOrchestrator and SmartAIOrchestrator must be provided")
            )
        }
        
        val testImage = generateTestImage()
        val workType = WorkType.GENERAL_CONSTRUCTION
        val numTests = 5
        
        // Test SimplifiedAIOrchestrator (LiteRT-enhanced)
        val simplifiedTimes = mutableListOf<Long>()
        repeat(numTests) {
            val start = System.currentTimeMillis()
            val result = simplifiedOrchestrator.analyzePhoto(testImage, workType)
            val duration = System.currentTimeMillis() - start
            
            if (result.isSuccess) {
                simplifiedTimes.add(duration)
            }
        }
        
        // Test SmartAIOrchestrator (legacy)
        val smartTimes = mutableListOf<Long>()
        repeat(numTests) {
            val start = System.currentTimeMillis()
            val result = smartOrchestrator.analyzePhoto(testImage, workType)
            val duration = System.currentTimeMillis() - start
            
            if (result.isSuccess) {
                smartTimes.add(duration)
            }
        }
        
        val simplifiedAvg = simplifiedTimes.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val smartAvg = smartTimes.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        
        val performanceImprovement = if (simplifiedAvg > 0) smartAvg / simplifiedAvg else 0.0
        val meetsMinTarget = performanceImprovement >= 3.0 // 3x minimum
        val achievesOptimal = performanceImprovement >= 8.0 // 8x optimal
        
        val score = when {
            achievesOptimal -> 100f
            performanceImprovement >= 5.0 -> 85f
            meetsMinTarget -> 70f
            performanceImprovement >= 2.0 -> 50f
            else -> 25f
        }
        
        results["simplifiedAvgMs"] = simplifiedAvg
        results["smartAvgMs"] = smartAvg
        results["performanceImprovement"] = "${String.format("%.1f", performanceImprovement)}x"
        results["meetsMinTarget"] = meetsMinTarget
        results["achievesOptimal"] = achievesOptimal
        results["simplifiedSuccessCount"] = simplifiedTimes.size
        results["smartSuccessCount"] = smartTimes.size
        
        if (!meetsMinTarget) {
            issues.add("Performance improvement of ${String.format("%.1f", performanceImprovement)}x does not meet 3x minimum target")
        }
        
        return LiteRTValidationResult(
            testName = "Orchestrator Comparison", 
            score = score,
            targetsMet = meetsMinTarget,
            results = results,
            issues = issues
        )
    }
    
    /**
     * Validate real AI analysis vs mock JSON generation performance.
     */
    private suspend fun validateRealVsMockPerformance(): LiteRTValidationResult {
        val results = mutableMapOf<String, Any>()
        val issues = mutableListOf<String>()
        
        val testImage = generateTestImage()
        val numTests = 10
        
        // Test real AI analysis
        val realTimes = mutableListOf<Long>()
        val realAccuracy = mutableListOf<Float>()
        
        liteRTEngine?.let { engine ->
            repeat(numTests) {
                val start = System.currentTimeMillis()
                val result = engine.generateSafetyAnalysis(
                    imageData = testImage,
                    workType = WorkType.GENERAL_CONSTRUCTION
                )
                val duration = System.currentTimeMillis() - start
                
                if (result.isSuccess) {
                    realTimes.add(duration)
                    realAccuracy.add(result.getOrNull()?.confidence ?: 0f)
                }
            }
        }
        
        // Test mock generation
        val mockTimes = mutableListOf<Long>()
        repeat(numTests) {
            val start = System.currentTimeMillis()
            generateMockAnalysis() // Fast mock generation
            val duration = System.currentTimeMillis() - start
            mockTimes.add(duration)
        }
        
        val realAvgTime = realTimes.takeIf { it.isNotEmpty() }?.average() ?: 0.0
        val mockAvgTime = mockTimes.average()
        val realAvgAccuracy = realAccuracy.takeIf { it.isNotEmpty() }?.average() ?: 0f
        val mockAccuracy = 0.3f // Mock has low accuracy
        
        val speedRatio = realAvgTime / mockAvgTime
        val accuracyImprovement = realAvgAccuracy / mockAccuracy
        val qualityScore = realAvgAccuracy * 100f
        
        val worthwhileTradeoff = speedRatio <= 10f && realAvgAccuracy >= 0.7f
        val targetsMet = worthwhileTradeoff && qualityScore >= 70f
        
        val score = when {
            targetsMet && qualityScore >= 80f -> 100f
            qualityScore >= 70f -> 85f
            qualityScore >= 60f -> 70f
            qualityScore >= 50f -> 50f
            else -> 25f
        }
        
        results["realAvgTimeMs"] = realAvgTime
        results["mockAvgTimeMs"] = mockAvgTime
        results["speedRatio"] = "${String.format("%.1f", speedRatio)}x"
        results["realAccuracy"] = "${String.format("%.1f", realAvgAccuracy * 100)}%"
        results["mockAccuracy"] = "${String.format("%.1f", mockAccuracy * 100)}%"
        results["accuracyImprovement"] = "${String.format("%.1f", accuracyImprovement)}x"
        results["qualityScore"] = qualityScore
        results["worthwhileTradeoff"] = worthwhileTradeoff
        
        if (!worthwhileTradeoff) {
            if (speedRatio > 10f) {
                issues.add("Real AI analysis is ${String.format("%.1f", speedRatio)}x slower than mock (target: d10x)")
            }
            if (realAvgAccuracy < 0.7f) {
                issues.add("Real AI accuracy of ${String.format("%.1f", realAvgAccuracy * 100)}% is below 70% target")
            }
        }
        
        return LiteRTValidationResult(
            testName = "Real vs Mock Performance",
            score = score,
            targetsMet = targetsMet,
            results = results,
            issues = issues
        )
    }
    
    // Helper method to generate test image
    private fun generateTestImage(): ByteArray {
        return Random.nextBytes(500_000) // 500KB test image
    }
    
    // Helper method to generate mock analysis
    private fun generateMockAnalysis(): String {
        delay(Random.nextLong(5, 20)) // Much faster than real analysis
        return """{"mock": true, "confidence": 0.3}"""
    }
    
    // Placeholder methods for remaining validation tests
    private suspend fun validateDeviceOptimization(): LiteRTValidationResult = TODO("Implement device optimization validation")
    private suspend fun validateProductionLoad(): LiteRTValidationResult = TODO("Implement production load validation") 
    private suspend fun validateMemoryThermalOptimization(): LiteRTValidationResult = TODO("Implement memory thermal validation")
    
    private fun calculatePerformanceGrade(score: Float): String {
        return when {
            score >= 90f -> "A"
            score >= 80f -> "B" 
            score >= 70f -> "C"
            score >= 60f -> "D"
            else -> "F"
        }
    }
    
    private fun generateOptimizationRecommendations(
        validationResults: List<LiteRTValidationResult>,
        capabilities: DeviceCapabilities
    ): List<String> {
        return listOf("Optimization recommendations to be implemented")
    }
}

/**
 * Individual validation test result.
 */
data class LiteRTValidationResult(
    val testName: String,
    val score: Float, // 0-100
    val targetsMet: Boolean,
    val results: Map<String, Any>,
    val issues: List<String>
)

/**
 * Complete LiteRT validation report.
 */
data class LiteRTValidationReport(
    val timestamp: Long,
    val validationDurationMs: Long,
    val deviceCapabilities: DeviceCapabilities,
    val validationResults: List<LiteRTValidationResult>,
    val overallScore: Float,
    val allTargetsMet: Boolean,
    val performanceGrade: String,
    val criticalIssues: List<String>,
    val recommendations: List<String>
) {
    val performanceImprovement: String
        get() {
            val orchestratorResult = validationResults.find { it.testName == "Orchestrator Comparison" }
            return orchestratorResult?.results?.get("performanceImprovement")?.toString() ?: "Unknown"
        }
        
    val meetsMinimumTargets: Boolean
        get() = performanceImprovement.removeSuffix("x").toFloatOrNull()?.let { it >= 3f } ?: false
        
    val achievesOptimalTargets: Boolean
        get() = performanceImprovement.removeSuffix("x").toFloatOrNull()?.let { it >= 8f } ?: false
}