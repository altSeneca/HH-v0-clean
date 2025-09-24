package com.hazardhawk.ai.litert

import com.hazardhawk.core.models.*
import kotlinx.coroutines.delay

/**
 * Mock implementation of LiteRT Model Engine for testing.
 * Provides controllable behavior for comprehensive testing scenarios.
 */
class MockLiteRTModelEngine {
    
    private var _isInitialized = false
    private var _currentBackend: LiteRTBackend? = null
    private var _supportedBackends: Set<LiteRTBackend> = setOf(
        LiteRTBackend.CPU,
        LiteRTBackend.GPU_OPENCL,
        LiteRTBackend.GPU_OPENGL,
        LiteRTBackend.NPU_NNAPI
    )
    
    // Device capability simulation
    private var androidVersion = 34
    private var boardInfo = "qcom_msm8998"
    private var totalMemoryGB = 6f
    private var availableMemoryMB = 2000f
    private var deviceTemperature = 35f
    
    // Test control flags
    private var modelFileExists = true
    private var modelCorrupted = false
    private var permissionDenied = false
    
    // Performance tracking
    private var analysisCount = 0L
    private var totalProcessingTime = 0L
    private var successCount = 0L
    private var peakMemoryUsage = 0f
    private var averageMemoryUsage = 0f
    
    val isAvailable: Boolean get() = _isInitialized
    val currentBackend: LiteRTBackend? get() = _currentBackend
    val supportedBackends: Set<LiteRTBackend> get() = _supportedBackends
    
    suspend fun initialize(
        modelPath: String,
        backend: LiteRTBackend
    ): Result<Unit> {
        delay(50) // Simulate initialization time
        
        try {
            // Check test control flags
            if (!modelFileExists) {
                return Result.failure(
                    LiteRTException.ModelLoadException("Model file not found: $modelPath")
                )
            }
            
            if (modelCorrupted) {
                return Result.failure(
                    LiteRTException.ModelLoadException("Model file corrupted: $modelPath")
                )
            }
            
            if (permissionDenied) {
                return Result.failure(
                    LiteRTException.InitializationException("Permission denied")
                )
            }
            
            // Resolve AUTO backend
            val resolvedBackend = if (backend == LiteRTBackend.AUTO) {
                selectOptimalBackend()
            } else {
                backend
            }
            
            // Check if backend is supported
            if (resolvedBackend !in _supportedBackends) {
                return Result.failure(
                    LiteRTException.UnsupportedBackendException(resolvedBackend)
                )
            }
            
            // Check memory requirements
            val requiredMemory = getMemoryRequirement(resolvedBackend)
            if (availableMemoryMB < requiredMemory) {
                return Result.failure(
                    LiteRTException.OutOfMemoryException(requiredMemory, availableMemoryMB)
                )
            }
            
            _currentBackend = resolvedBackend
            _isInitialized = true
            return Result.success(Unit)
            
        } catch (e: Exception) {
            return Result.failure(
                LiteRTException.InitializationException("Unexpected error: ${e.message}", e)
            )
        }
    }
    
    suspend fun generateSafetyAnalysis(
        imageData: ByteArray,
        workType: WorkType,
        includeOSHACodes: Boolean = true,
        confidenceThreshold: Float = 0.6f
    ): Result<LiteRTAnalysisResult> {
        
        if (!_isInitialized) {
            return Result.failure(
                LiteRTException.InferenceException("Engine not initialized")
            )
        }
        
        // Check thermal throttling
        if (deviceTemperature >= 80f) {
            return Result.failure(
                LiteRTException.ThermalThrottlingException(deviceTemperature)
            )
        }
        
        val startTime = System.currentTimeMillis()
        
        try {
            // Validate image data
            if (imageData.isEmpty()) {
                return Result.failure(
                    LiteRTException.InferenceException("Empty image data")
                )
            }
            
            // Check for corrupted image data
            if (imageData.all { it == 0.toByte() }) {
                return Result.failure(
                    LiteRTException.InferenceException("Corrupted image data")
                )
            }
            
            // Check memory during processing
            val memoryBefore = getCurrentMemoryUsage()
            val imageSize = imageData.size
            val estimatedMemoryNeeded = (imageSize * 1.5f) / (1024 * 1024) // MB
            
            if (memoryBefore + estimatedMemoryNeeded > availableMemoryMB) {
                return Result.failure(
                    LiteRTException.OutOfMemoryException(
                        estimatedMemoryNeeded,
                        availableMemoryMB - memoryBefore
                    )
                )
            }
            
            // Simulate processing time based on backend
            val processingTime = getExpectedProcessingTime(_currentBackend!!, imageData.size)
            delay(processingTime)
            
            // Generate mock analysis result
            val result = generateMockAnalysisResult(
                imageData, workType, includeOSHACodes, confidenceThreshold, processingTime
            )
            
            // Update performance metrics
            analysisCount++
            totalProcessingTime += processingTime
            successCount++
            val memoryUsed = estimatedMemoryNeeded
            peakMemoryUsage = maxOf(peakMemoryUsage, memoryUsed)
            averageMemoryUsage = ((averageMemoryUsage * (analysisCount - 1)) + memoryUsed) / analysisCount
            
            return Result.success(result)
            
        } catch (e: Exception) {
            return Result.failure(
                LiteRTException.InferenceException("Analysis failed: ${e.message}", e)
            )
        }
    }
    
    fun getPerformanceMetrics(): LiteRTPerformanceMetrics {
        val avgProcessingTime = if (analysisCount > 0) totalProcessingTime / analysisCount else 0L
        val successRate = if (analysisCount > 0) successCount.toFloat() / analysisCount else 0f
        
        val tokensPerSecond = when (_currentBackend) {
            LiteRTBackend.NPU_NNAPI, LiteRTBackend.NPU_QTI_HTP -> 5836f
            LiteRTBackend.GPU_OPENCL, LiteRTBackend.GPU_OPENGL -> 1876f
            else -> 243f
        }
        
        return LiteRTPerformanceMetrics(
            analysisCount = analysisCount,
            averageProcessingTimeMs = avgProcessingTime,
            tokensPerSecond = tokensPerSecond,
            peakMemoryUsageMB = peakMemoryUsage,
            averageMemoryUsageMB = averageMemoryUsage,
            successRate = successRate,
            preferredBackend = _currentBackend,
            thermalThrottlingDetected = deviceTemperature >= 80f
        )
    }
    
    fun cleanup() {
        _isInitialized = false
        _currentBackend = null
        resetMetrics()
    }
    
    // Test configuration methods
    fun setDeviceCapabilities(
        totalMemoryGB: Float = this.totalMemoryGB,
        supportedBackends: Set<LiteRTBackend> = this._supportedBackends
    ) {
        this.totalMemoryGB = totalMemoryGB
        this._supportedBackends = supportedBackends
    }
    
    fun setAndroidVersion(version: Int) {
        this.androidVersion = version
        updateSupportedBackendsForVersion()
    }
    
    fun setBoardInfo(board: String) {
        this.boardInfo = board
        updateSupportedBackendsForBoard()
    }
    
    fun setAvailableMemory(memoryMB: Float) {
        this.availableMemoryMB = memoryMB
    }
    
    fun setDeviceTemperature(temperature: Float) {
        this.deviceTemperature = temperature
    }
    
    fun setModelFileExists(exists: Boolean) {
        this.modelFileExists = exists
    }
    
    fun setModelCorrupted(corrupted: Boolean) {
        this.modelCorrupted = corrupted
    }
    
    fun setPermissionDenied(denied: Boolean) {
        this.permissionDenied = denied
    }
    
    fun reset() {
        modelFileExists = true
        modelCorrupted = false
        permissionDenied = false
        availableMemoryMB = 2000f
        deviceTemperature = 35f
    }
    
    // Private helper methods
    private fun selectOptimalBackend(): LiteRTBackend {
        return when {
            LiteRTBackend.NPU_QTI_HTP in _supportedBackends && totalMemoryGB >= 8 -> 
                LiteRTBackend.NPU_QTI_HTP
            LiteRTBackend.NPU_NNAPI in _supportedBackends && totalMemoryGB >= 6 -> 
                LiteRTBackend.NPU_NNAPI
            LiteRTBackend.GPU_OPENCL in _supportedBackends && totalMemoryGB >= 4 -> 
                LiteRTBackend.GPU_OPENCL
            LiteRTBackend.GPU_OPENGL in _supportedBackends && totalMemoryGB >= 3 -> 
                LiteRTBackend.GPU_OPENGL
            else -> LiteRTBackend.CPU
        }
    }
    
    private fun getMemoryRequirement(backend: LiteRTBackend): Float {
        return when (backend) {
            LiteRTBackend.CPU -> 300f
            LiteRTBackend.GPU_OPENGL -> 400f
            LiteRTBackend.GPU_OPENCL -> 500f
            LiteRTBackend.NPU_NNAPI -> 400f
            LiteRTBackend.NPU_QTI_HTP -> 350f
            LiteRTBackend.AUTO -> 300f
        }
    }
    
    private fun getExpectedProcessingTime(backend: LiteRTBackend, imageSize: Int): Long {
        val baseTimes = mapOf(
            LiteRTBackend.CPU to 2500L,
            LiteRTBackend.GPU_OPENGL to 1200L,
            LiteRTBackend.GPU_OPENCL to 1000L,
            LiteRTBackend.NPU_NNAPI to 600L,
            LiteRTBackend.NPU_QTI_HTP to 500L
        )
        
        val baseTime = baseTimes[backend] ?: 2500L
        val scaleFactor = imageSize.toDouble() / (1920 * 1080 * 3) // Assuming RGB
        return (baseTime * scaleFactor).toLong()
    }
    
    private fun getCurrentMemoryUsage(): Float = 800f // Mock value
    
    private fun updateSupportedBackendsForVersion() {
        _supportedBackends = buildSet {
            add(LiteRTBackend.CPU) // Always supported
            add(LiteRTBackend.GPU_OPENGL) // OpenGL ES supported from early Android
            
            if (androidVersion >= 21) { // Android 5.0+
                add(LiteRTBackend.GPU_OPENCL)
            }
            
            if (androidVersion >= 27) { // Android 8.1+
                add(LiteRTBackend.NPU_NNAPI)
            }
            
            // Qualcomm HTP support depends on board info
            if (boardInfo.contains("qcom", ignoreCase = true)) {
                add(LiteRTBackend.NPU_QTI_HTP)
            }
        }
    }
    
    private fun updateSupportedBackendsForBoard() {
        val currentBackends = _supportedBackends.toMutableSet()
        
        if (boardInfo.contains("qcom", ignoreCase = true)) {
            currentBackends.add(LiteRTBackend.NPU_QTI_HTP)
        } else {
            currentBackends.remove(LiteRTBackend.NPU_QTI_HTP)
        }
        
        _supportedBackends = currentBackends
    }
    
    private fun resetMetrics() {
        analysisCount = 0L
        totalProcessingTime = 0L
        successCount = 0L
        peakMemoryUsage = 0f
        averageMemoryUsage = 0f
    }
    
    private fun generateMockAnalysisResult(
        imageData: ByteArray,
        workType: WorkType,
        includeOSHACodes: Boolean,
        confidenceThreshold: Float,
        processingTime: Long
    ): LiteRTAnalysisResult {
        
        // Generate hazards based on work type and image characteristics
        val hazards = generateMockHazards(workType, confidenceThreshold, imageData)
        val ppeStatus = generateMockPPEStatus(imageData)
        val oshaViolations = if (includeOSHACodes) generateMockOSHAViolations(workType) else emptyList()
        val riskAssessment = generateMockRiskAssessment(hazards, ppeStatus)
        
        return LiteRTAnalysisResult(
            hazards = hazards,
            ppeStatus = ppeStatus,
            oshaViolations = oshaViolations,
            overallRiskAssessment = riskAssessment,
            confidence = 0.85f,
            processingTimeMs = processingTime,
            backendUsed = _currentBackend!!,
            debugInfo = LiteRTDebugInfo(
                modelVersion = "MockLiteRT-v1.0",
                inputPreprocessingTime = 50L,
                inferenceTime = processingTime - 100L,
                postProcessingTime = 50L,
                memoryPeakMB = peakMemoryUsage,
                deviceTemperature = deviceTemperature
            )
        )
    }
    
    private fun generateMockHazards(
        workType: WorkType,
        confidenceThreshold: Float,
        imageData: ByteArray
    ): List<DetectedHazard> {
        val hazards = mutableListOf<DetectedHazard>()
        
        // Determine hazards based on work type and image characteristics
        when (workType) {
            WorkType.ELECTRICAL -> {
                hazards.add(createMockHazard(HazardType.ELECTRICAL, 0.92f))
                hazards.add(createMockHazard(HazardType.FALL, 0.78f))
            }
            WorkType.EXCAVATION -> {
                hazards.add(createMockHazard(HazardType.CAUGHT_IN, 0.88f))
                hazards.add(createMockHazard(HazardType.STRUCK_BY, 0.82f))
            }
            WorkType.ROOFING -> {
                hazards.add(createMockHazard(HazardType.FALL, 0.95f))
            }
            else -> {
                hazards.add(createMockHazard(HazardType.STRUCK_BY, 0.75f))
            }
        }
        
        // Special image characteristics
        if (isLowConfidenceImage(imageData)) {
            hazards.forEach { hazard ->
                hazards[hazards.indexOf(hazard)] = hazard.copy(confidence = hazard.confidence * 0.6f)
            }
        }
        
        return hazards.filter { it.confidence >= confidenceThreshold }
    }
    
    private fun createMockHazard(type: HazardType, confidence: Float): DetectedHazard {
        return DetectedHazard(
            type = type,
            description = "Mock ${type.name.lowercase()} hazard detected",
            severity = when {
                confidence > 0.9f -> Severity.HIGH
                confidence > 0.7f -> Severity.MEDIUM
                else -> Severity.LOW
            },
            confidence = confidence,
            boundingBox = BoundingBox(
                x = 0.2f + (Math.random() * 0.6).toFloat(),
                y = 0.2f + (Math.random() * 0.6).toFloat(),
                width = 0.1f + (Math.random() * 0.3).toFloat(),
                height = 0.1f + (Math.random() * 0.3).toFloat(),
                confidence = confidence
            ),
            oshaCode = getOSHACodeForHazard(type),
            recommendations = listOf("Follow proper safety procedures", "Use appropriate PPE")
        )
    }
    
    private fun generateMockPPEStatus(imageData: ByteArray): Map<PPEType, PPEDetection> {
        val ppeMap = mutableMapOf<PPEType, PPEDetection>()
        
        // Base detection rates
        val ppeDetectionRates = mapOf(
            PPEType.HARD_HAT to 0.95f,
            PPEType.SAFETY_VEST to 0.89f,
            PPEType.SAFETY_BOOTS to 0.85f,
            PPEType.SAFETY_GLASSES to 0.78f,
            PPEType.GLOVES to 0.82f
        )
        
        ppeDetectionRates.forEach { (ppeType, baseRate) ->
            val detected = if (isMissingPPEImage(imageData, ppeType)) {
                Math.random() < 0.1 // Low chance if missing PPE image
            } else {
                Math.random() < baseRate
            }
            
            ppeMap[ppeType] = PPEDetection(
                detected = detected,
                confidence = 0.8f + (Math.random() * 0.18).toFloat()
            )
        }
        
        return ppeMap
    }
    
    private fun generateMockOSHAViolations(workType: WorkType): List<OSHAViolation> {
        val violations = mutableListOf<OSHAViolation>()
        
        when (workType) {
            WorkType.ELECTRICAL -> {
                violations.add(OSHAViolation(
                    regulationCode = "1926.416",
                    description = "Electrical safety-related work practices violation",
                    severity = Severity.HIGH,
                    recommendation = "Ensure proper lockout/tagout procedures"
                ))
            }
            WorkType.EXCAVATION -> {
                violations.add(OSHAViolation(
                    regulationCode = "1926.651",
                    description = "Excavation requirements violation", 
                    severity = Severity.HIGH,
                    recommendation = "Implement proper excavation protection systems"
                ))
            }
            WorkType.ROOFING -> {
                violations.add(OSHAViolation(
                    regulationCode = "1926.501",
                    description = "Fall protection violation",
                    severity = Severity.HIGH,
                    recommendation = "Install proper guardrail systems"
                ))
            }
            else -> {
                violations.add(OSHAViolation(
                    regulationCode = "1926.95",
                    description = "Personal protective equipment violation",
                    severity = Severity.MEDIUM,
                    recommendation = "Ensure all workers wear required PPE"
                ))
            }
        }
        
        return violations
    }
    
    private fun generateMockRiskAssessment(
        hazards: List<DetectedHazard>,
        ppeStatus: Map<PPEType, PPEDetection>
    ): RiskAssessment {
        val highSeverityCount = hazards.count { it.severity == Severity.HIGH }
        val missingPPECount = ppeStatus.count { !it.value.detected }
        
        val overallRisk = when {
            highSeverityCount > 2 || missingPPECount > 2 -> Severity.HIGH
            highSeverityCount > 0 || missingPPECount > 0 -> Severity.MEDIUM
            else -> Severity.LOW
        }
        
        return RiskAssessment(
            overallRisk = overallRisk,
            riskScore = when (overallRisk) {
                Severity.HIGH -> 80 + (Math.random() * 20).toInt()
                Severity.MEDIUM -> 40 + (Math.random() * 40).toInt()
                Severity.LOW -> (Math.random() * 40).toInt()
                else -> 50
            },
            mitigationPriority = overallRisk,
            recommendations = listOf(
                "Address high-severity hazards immediately",
                "Ensure all PPE is properly worn",
                "Conduct regular safety briefings"
            )
        )
    }
    
    private fun getOSHACodeForHazard(type: HazardType): String? {
        return when (type) {
            HazardType.FALL -> "1926.501"
            HazardType.ELECTRICAL -> "1926.416" 
            HazardType.STRUCK_BY -> "1926.95"
            HazardType.CAUGHT_IN -> "1926.651"
            else -> null
        }
    }
    
    private fun isLowConfidenceImage(imageData: ByteArray): Boolean {
        // Mock logic - check for specific test patterns
        return imageData.size < 1000 || imageData.take(10).all { it == 0.toByte() }
    }
    
    private fun isMissingPPEImage(imageData: ByteArray, ppeType: PPEType): Boolean {
        // Mock logic - simulate missing PPE detection based on image data patterns
        val hash = imageData.take(100).sum()
        return (hash % 100) < 20 // 20% chance of missing PPE
    }
}
