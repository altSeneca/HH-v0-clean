package com.hazardhawk.ai.yolo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.models.SafetyAnalysis

/**
 * YOLO11 Safety Analyzer Usage Examples
 * 
 * This file demonstrates how to integrate and use the YOLO11SafetyAnalyzer
 * in various scenarios within the HazardHawk application.
 * 
 * Examples include:
 * - Basic single image analysis
 * - Batch processing for multiple images  
 * - Real-time analysis with performance monitoring
 * - Integration with existing safety workflows
 * - Error handling and recovery patterns
 */
class YOLO11SafetyAnalyzerExample {
    
    private val analyzer = YOLO11SafetyAnalyzer()
    
    /**
     * Example 1: Basic Safety Analysis
     * 
     * Demonstrates the simplest usage pattern for analyzing a single construction photo.
     */
    suspend fun basicSafetyAnalysisExample(): SafetyAnalysis? {
        return try {
            // Initialize the analyzer
            analyzer.initialize(
                enableGPU = true,
                optimizeForBattery = false
            ).getOrElse {
                println("Failed to initialize analyzer: ${it.message}")
                return null
            }
            
            // Mock image data (in practice, this would come from camera or file system)
            val imageData = generateMockImageData()
            
            // Perform safety analysis
            val result = analyzer.analyzeSafety(
                imageData = imageData,
                workType = WorkType.GENERAL_CONSTRUCTION,
                photoId = "photo-12345",
                quality = AnalysisQuality.BALANCED
            )
            
            result.getOrElse {
                println("Analysis failed: ${it.message}")
                return null
            }
            
        } catch (e: Exception) {
            println("Unexpected error: ${e.message}")
            null
        } finally {
            // Always cleanup resources
            analyzer.cleanup()
        }
    }
    
    /**
     * Example 2: Batch Processing with Progress Monitoring
     * 
     * Shows how to process multiple images efficiently with real-time progress updates.
     */
    suspend fun batchAnalysisWithProgressExample(): List<SafetyAnalysis> {
        val results = mutableListOf<SafetyAnalysis>()
        
        try {
            // Initialize analyzer
            analyzer.initialize().getOrElse {
                println("Initialization failed: ${it.message}")
                return emptyList()
            }
            
            // Prepare batch data
            val imageBatch = (1..5).map { index ->
                ImageData(
                    id = "batch-photo-$index",
                    data = generateMockImageData()
                )
            }
            
            // Process batch with progress monitoring
            analyzer.analyzeSafetyBatch(
                imageBatch = imageBatch,
                workType = WorkType.ROOFING,
                quality = AnalysisQuality.FAST
            ).collect { result ->
                result.onSuccess { analysis ->
                    results.add(analysis)
                    println("Processed ${results.size}/${imageBatch.size}: ${analysis.hazards.size} hazards found")
                }.onFailure { error ->
                    println("Batch item failed: ${error.message}")
                }
            }
            
            println("Batch analysis completed: ${results.size} successful analyses")
            
        } catch (e: Exception) {
            println("Batch processing error: ${e.message}")
        } finally {
            analyzer.cleanup()
        }
        
        return results
    }
    
    /**
     * Example 3: Real-time Analysis with Performance Monitoring
     * 
     * Demonstrates how to implement continuous analysis with performance optimization.
     */
    suspend fun realTimeAnalysisExample() {
        try {
            // Initialize with battery optimization for mobile devices
            analyzer.initialize(
                enableGPU = false,
                optimizeForBattery = true
            ).getOrElse {
                println("Failed to initialize: ${it.message}")
                return
            }
            
            // Monitor performance updates
            val performanceJob = CoroutineScope(Dispatchers.Default).launch {
                analyzer.performanceUpdates.collect { update ->
                    println("Performance Update:")
                    println("  - Score: ${update.analysis.performanceScore}")
                    println("  - Frame Rate: ${update.analysis.frameRate} FPS")
                    println("  - Memory Usage: ${update.analysis.averageMemoryUsage} MB")
                    
                    if (update.recommendations.isNotEmpty()) {
                        println("  - Recommendations: ${update.recommendations.joinToString(", ")}")
                    }
                }
            }
            
            // Monitor analysis results
            val resultsJob = CoroutineScope(Dispatchers.Default).launch {
                analyzer.analysisResults.collect { result ->
                    println("Analysis Complete:")
                    println("  - ID: ${result.analysisId}")
                    println("  - Processing Time: ${result.processingTimeMs}ms")
                    println("  - Detections: ${result.detectionCount}")
                    println("  - Hazards: ${result.safetyAnalysis.hazards.size}")
                    println("  - Overall Severity: ${result.safetyAnalysis.severity}")
                }
            }
            
            // Simulate continuous image analysis (e.g., from camera feed)
            repeat(10) { index ->
                val imageData = generateMockImageData()
                
                analyzer.analyzeSafety(
                    imageData = imageData,
                    workType = WorkType.ELECTRICAL,
                    photoId = "realtime-$index",
                    quality = AnalysisQuality.FAST
                ).onSuccess { analysis ->
                    // Handle successful analysis
                    if (analysis.severity.ordinal >= 2) { // HIGH or CRITICAL
                        println("⚠  HIGH SEVERITY HAZARD DETECTED IN FRAME $index ⚠️")
                        displayCriticalHazardAlert(analysis)
                    }
                }.onFailure { error ->
                    println("Frame $index analysis failed: ${error.message}")
                }
                
                // Small delay between frames
                delay(2000)
            }
            
            // Cancel monitoring jobs
            performanceJob.cancel()
            resultsJob.cancel()
            
        } catch (e: Exception) {
            println("Real-time analysis error: ${e.message}")
        } finally {
            analyzer.cleanup()
        }
    }
    
    /**
     * Example 4: Advanced Configuration and Benchmarking
     * 
     * Shows advanced usage including custom parameters and performance benchmarking.
     */
    suspend fun advancedConfigurationExample() {
        try {
            // Initialize with high-performance settings
            analyzer.initialize(
                enableGPU = true,
                optimizeForBattery = false
            ).getOrElse {
                println("Initialization failed")
                return
            }
            
            // Check initial status
            val status = analyzer.getAnalyzerStatus()
            println("Analyzer Status:")
            println("  - Initialized: ${status.isInitialized}")
            println("  - Detector Status: ${status.detectorStatus}")
            println("  - Model: ${status.currentConfiguration?.modelVariant}")
            println("  - Device Tier: ${status.deviceCapability?.performanceLevel}")
            
            // Run analysis with different quality settings
            val testImage = generateMockImageData()
            
            println("\nTesting different quality levels...")
            
            for (quality in AnalysisQuality.values()) {
                val startTime = Clock.System.now().toEpochMilliseconds()
                
                val result = analyzer.analyzeSafety(
                    imageData = testImage,
                    workType = WorkType.FALL_PROTECTION,
                    photoId = "quality-test-${quality.name}",
                    quality = quality
                )
                
                val endTime = Clock.System.now().toEpochMilliseconds()
                
                result.onSuccess { analysis ->
                    println("${quality.name}:")
                    println("  - Time: ${endTime - startTime}ms")
                    println("  - Hazards: ${analysis.hazards.size}")
                    println("  - Confidence: ${analysis.aiConfidence}")
                }.onFailure {
                    println("${quality.name}: FAILED - ${it.message}")
                }
            }
            
            // Update parameters dynamically
            println("\nUpdating analysis parameters...")
            analyzer.updateAnalysisParameters(
                newQuality = AnalysisQuality.ACCURATE,
                confidenceOverride = 0.8f
            ).onSuccess {
                println("Parameters updated successfully")
            }.onFailure {
                println("Parameter update failed: ${it.message}")
            }
            
            // Generate comprehensive benchmark report
            println("\nGenerating benchmark report...")
            analyzer.generateBenchmarkReport().onSuccess { report ->
                println("Benchmark Report Generated:")
                println("  - Report ID: ${report.reportId}")
                println("  - Total Inferences: ${report.performanceMetrics.totalInferences}")
                println("  - Average Time: ${report.performanceMetrics.averageProcessingTime}ms")
                println("  - Average Memory: ${report.performanceMetrics.averageMemoryUsage}MB")
                println("  - Recommendations: ${report.recommendations.size}")
                
                report.recommendations.forEach { recommendation ->
                    println("    • $recommendation")
                }
            }.onFailure {
                println("Benchmark report failed: ${it.message}")
            }
            
        } catch (e: Exception) {
            println("Advanced configuration error: ${e.message}")
        } finally {
            analyzer.cleanup()
        }
    }
    
    /**
     * Example 5: Integration with Existing HazardHawk Workflows
     * 
     * Shows how to integrate YOLO11 analysis into existing safety workflows.
     */
    suspend fun integrationWithHazardHawkWorkflowExample() {
        try {
            analyzer.initialize().getOrElse {
                println("Integration example: Initialization failed")
                return
            }
            
            // Simulate typical HazardHawk workflow
            val constructionSite = "Site-Alpha-Building-7"
            val inspector = "Inspector-Jane-Smith"
            val workTypes = listOf(
                WorkType.GENERAL_CONSTRUCTION,
                WorkType.ELECTRICAL,
                WorkType.FALL_PROTECTION
            )
            
            println("Starting safety inspection for $constructionSite...")
            
            workTypes.forEachIndexed { index, workType ->
                println("\nInspecting ${workType.name} area...")
                
                // Capture photo (simulated)
                val photoData = generateMockImageData()
                val photoId = "$constructionSite-${workType.name}-${Clock.System.now().toEpochMilliseconds()}"
                
                // Perform YOLO11 analysis
                val analysisResult = analyzer.analyzeSafety(
                    imageData = photoData,
                    workType = workType,
                    photoId = photoId,
                    quality = AnalysisQuality.BALANCED
                )
                
                analysisResult.onSuccess { analysis ->
                    println("✅ Analysis completed for ${workType.name}")
                    
                    // Generate safety report
                    generateSafetyInspectionReport(
                        siteId = constructionSite,
                        inspectorId = inspector,
                        workType = workType,
                        analysis = analysis
                    )
                    
                    // Check for immediate actions required
                    if (analysis.severity == com.hazardhawk.models.Severity.CRITICAL) {
                        triggerImmediateSafetyResponse(analysis)
                    }
                    
                }.onFailure { error ->
                    println("❌ Analysis failed for ${workType.name}: ${error.message}")
                    logFailureForRetry(photoId, workType, error)
                }
            }
            
            println("\nSafety inspection completed for $constructionSite")
            
        } catch (e: Exception) {
            println("Workflow integration error: ${e.message}")
        } finally {
            analyzer.cleanup()
        }
    }
    
    // Helper methods
    
    private fun generateMockImageData(): ByteArray {
        // In practice, this would be real image data from camera or file
        return ByteArray(1024 * 100) { (it % 256).toByte() } // 100KB mock image
    }
    
    private fun displayCriticalHazardAlert(analysis: SafetyAnalysis) {
        println("🚨 CRITICAL SAFETY ALERT 🚨")
        println("Photo ID: ${analysis.photoId}")
        println("Hazards Detected:")
        
        analysis.hazards.forEach { hazard ->
            if (hazard.severity == com.hazardhawk.models.Severity.CRITICAL) {
                println("  ⛔ ${hazard.type}: ${hazard.description}")
                println("     Confidence: ${(hazard.confidence * 100).toInt()}%")
                hazard.oshaReference?.let { osha ->
                    println("     OSHA: $osha")
                }
            }
        }
        
        println("\nRecommendations:")
        analysis.recommendations.forEach { recommendation ->
            println("  • $recommendation")
        }
    }
    
    private fun generateSafetyInspectionReport(
        siteId: String,
        inspectorId: String, 
        workType: WorkType,
        analysis: SafetyAnalysis
    ) {
        println("📋 Generating Safety Inspection Report:")
        println("  Site: $siteId")
        println("  Inspector: $inspectorId") 
        println("  Work Type: ${workType.name}")
        println("  Analysis Date: ${analysis.analyzedAt}")
        println("  Overall Severity: ${analysis.severity}")
        println("  AI Confidence: ${(analysis.aiConfidence * 100).toInt()}%")
        println("  Hazards Found: ${analysis.hazards.size}")
        println("  OSHA Codes: ${analysis.oshaCodes.size}")
        
        // In practice, this would integrate with existing reporting systems
        // saveToDatabase(analysis)
        // generatePDFReport(analysis)
        // notifyManagement(analysis)
    }
    
    private fun triggerImmediateSafetyResponse(analysis: SafetyAnalysis) {
        println("🚨 TRIGGERING IMMEDIATE SAFETY RESPONSE")
        println("  - Notifying site supervisor")
        println("  - Logging critical incident")
        println("  - Initiating safety protocol")
        
        // In practice, this would trigger real safety protocols
        // notifySiteSupervisor(analysis)
        // logCriticalIncident(analysis)
        // initiateSafetyProtocol(analysis)
    }
    
    private fun logFailureForRetry(
        photoId: String,
        workType: WorkType, 
        error: Throwable
    ) {
        println("📝 Logging failure for retry:")
        println("  Photo ID: $photoId")
        println("  Work Type: ${workType.name}")
        println("  Error: ${error.message}")
        
        // In practice, this would queue for retry or manual review
        // queueForRetry(photoId, workType)
        // notifyForManualReview(photoId, error)
    }
}

/**
 * Usage demonstration function
 */
suspend fun demonstrateYOLO11SafetyAnalyzer() {
    val example = YOLO11SafetyAnalyzerExample()
    
    println("=== YOLO11 Safety Analyzer Examples ===\n")
    
    // Run basic example
    println("1. Basic Safety Analysis:")
    val basicResult = example.basicSafetyAnalysisExample()
    basicResult?.let {
        println("✅ Basic analysis completed with ${it.hazards.size} hazards found\n")
    } ?: println("❌ Basic analysis failed\n")
    
    // Run batch processing example  
    println("2. Batch Processing:")
    val batchResults = example.batchAnalysisWithProgressExample()
    println("✅ Batch processing completed: ${batchResults.size} analyses\n")
    
    // Run real-time example (commented out to avoid long runtime)
    // println("3. Real-time Analysis:")
    // example.realTimeAnalysisExample()
    
    // Run advanced configuration
    println("3. Advanced Configuration:")
    example.advancedConfigurationExample()
    println()
    
    // Run integration example
    println("4. HazardHawk Workflow Integration:")
    example.integrationWithHazardHawkWorkflowExample()
    
    println("\n=== All Examples Completed ===")
}
