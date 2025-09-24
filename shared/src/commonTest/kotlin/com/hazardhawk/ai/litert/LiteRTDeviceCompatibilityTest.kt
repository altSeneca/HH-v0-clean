package com.hazardhawk.ai.litert

import com.hazardhawk.TestUtils
import com.hazardhawk.core.models.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive device compatibility testing matrix for LiteRT integration.
 * Tests across Android versions API 24-34 and various device configurations.
 */
class LiteRTDeviceCompatibilityTest {
    
    private lateinit var testDataFactory: LiteRTTestDataFactory
    private val deviceCompatibilityMatrix = DeviceCompatibilityMatrix()
    
    @BeforeTest
    fun setup() {
        testDataFactory = LiteRTTestDataFactory()
    }
    
    // =====================================================
    // ANDROID VERSION COMPATIBILITY TESTS
    // =====================================================
    
    @Test
    fun `test Android API 24-26 compatibility (Nougat to Oreo)`() = runTest {
        val legacyDevices = listOf(
            DeviceConfiguration(
                name = "Samsung Galaxy S7",
                apiLevel = 24,
                totalMemoryMB = 4096,
                boardInfo = "exynos8890",
                expectedBackends = setOf(LiteRTBackend.CPU, LiteRTBackend.GPU_OPENGL),
                expectedOptimalBackend = LiteRTBackend.GPU_OPENGL,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 4000L,
                    minSuccessRate = 0.85f,
                    maxMemoryUsageMB = 800f
                )
            ),
            DeviceConfiguration(
                name = "Google Pixel (2016)",
                apiLevel = 25,
                totalMemoryMB = 4096,
                boardInfo = "qcom_msm8996",
                expectedBackends = setOf(LiteRTBackend.CPU, LiteRTBackend.GPU_OPENGL),
                expectedOptimalBackend = LiteRTBackend.GPU_OPENGL,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 3500L,
                    minSuccessRate = 0.88f,
                    maxMemoryUsageMB = 750f
                )
            ),
            DeviceConfiguration(
                name = "OnePlus 5",
                apiLevel = 26,
                totalMemoryMB = 6144,
                boardInfo = "qcom_msm8998",
                expectedBackends = setOf(LiteRTBackend.CPU, LiteRTBackend.GPU_OPENGL, LiteRTBackend.GPU_OPENCL),
                expectedOptimalBackend = LiteRTBackend.GPU_OPENCL,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 3000L,
                    minSuccessRate = 0.90f,
                    maxMemoryUsageMB = 700f
                )
            )
        )
        
        testDeviceConfigurations(legacyDevices, "Legacy Android (API 24-26)")
    }
    
    @Test
    fun `test Android API 27-29 compatibility (Oreo MR1 to Android 10)`() = runTest {
        val midGenerationDevices = listOf(
            DeviceConfiguration(
                name = "Google Pixel 2",
                apiLevel = 27,
                totalMemoryMB = 4096,
                boardInfo = "qcom_msm8998",
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                ),
                expectedOptimalBackend = LiteRTBackend.NPU_NNAPI,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 2000L,
                    minSuccessRate = 0.92f,
                    maxMemoryUsageMB = 600f
                )
            ),
            DeviceConfiguration(
                name = "Samsung Galaxy S9",
                apiLevel = 28,
                totalMemoryMB = 4096,
                boardInfo = "exynos9810",
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI
                    // No QTI HTP support for Exynos
                ),
                expectedOptimalBackend = LiteRTBackend.NPU_NNAPI,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 2200L,
                    minSuccessRate = 0.90f,
                    maxMemoryUsageMB = 650f
                )
            ),
            DeviceConfiguration(
                name = "OnePlus 7 Pro",
                apiLevel = 29,
                totalMemoryMB = 8192,
                boardInfo = "qcom_sm8150",
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                ),
                expectedOptimalBackend = LiteRTBackend.NPU_QTI_HTP,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 1200L,
                    minSuccessRate = 0.95f,
                    maxMemoryUsageMB = 500f
                )
            )
        )
        
        testDeviceConfigurations(midGenerationDevices, "Mid-generation Android (API 27-29)")
    }
    
    @Test
    fun `test Android API 30-32 compatibility (Android 11-12)`() = runTest {
        val modernDevices = listOf(
            DeviceConfiguration(
                name = "Google Pixel 5",
                apiLevel = 30,
                totalMemoryMB = 8192,
                boardInfo = "qcom_sm7250",
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                ),
                expectedOptimalBackend = LiteRTBackend.NPU_QTI_HTP,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 1000L,
                    minSuccessRate = 0.96f,
                    maxMemoryUsageMB = 450f
                )
            ),
            DeviceConfiguration(
                name = "Samsung Galaxy S21",
                apiLevel = 31,
                totalMemoryMB = 8192,
                boardInfo = "exynos2100",
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI
                ),
                expectedOptimalBackend = LiteRTBackend.NPU_NNAPI,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 1100L,
                    minSuccessRate = 0.94f,
                    maxMemoryUsageMB = 500f
                )
            ),
            DeviceConfiguration(
                name = "OnePlus 10 Pro",
                apiLevel = 32,
                totalMemoryMB = 12288,
                boardInfo = "qcom_sm8450",
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                ),
                expectedOptimalBackend = LiteRTBackend.NPU_QTI_HTP,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 800L,
                    minSuccessRate = 0.97f,
                    maxMemoryUsageMB = 400f
                )
            )
        )
        
        testDeviceConfigurations(modernDevices, "Modern Android (API 30-32)")
    }
    
    @Test
    fun `test Android API 33-34 compatibility (Android 13-14)`() = runTest {
        val latestDevices = listOf(
            DeviceConfiguration(
                name = "Google Pixel 7 Pro",
                apiLevel = 33,
                totalMemoryMB = 12288,
                boardInfo = "google_tensor_g2",
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI
                    // Google Tensor has its own NPU architecture
                ),
                expectedOptimalBackend = LiteRTBackend.NPU_NNAPI,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 700L,
                    minSuccessRate = 0.98f,
                    maxMemoryUsageMB = 350f
                )
            ),
            DeviceConfiguration(
                name = "Samsung Galaxy S23 Ultra",
                apiLevel = 34,
                totalMemoryMB = 12288,
                boardInfo = "qcom_sm8550",
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                ),
                expectedOptimalBackend = LiteRTBackend.NPU_QTI_HTP,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 600L,
                    minSuccessRate = 0.98f,
                    maxMemoryUsageMB = 300f
                )
            ),
            DeviceConfiguration(
                name = "Nothing Phone (2)",
                apiLevel = 34,
                totalMemoryMB = 8192,
                boardInfo = "qcom_sm8475",
                expectedBackends = setOf(
                    LiteRTBackend.CPU,
                    LiteRTBackend.GPU_OPENGL,
                    LiteRTBackend.GPU_OPENCL,
                    LiteRTBackend.NPU_NNAPI,
                    LiteRTBackend.NPU_QTI_HTP
                ),
                expectedOptimalBackend = LiteRTBackend.NPU_QTI_HTP,
                performanceExpectations = PerformanceExpectations(
                    maxProcessingTimeMs = 800L,
                    minSuccessRate = 0.96f,
                    maxMemoryUsageMB = 400f
                )
            )
        )
        
        testDeviceConfigurations(latestDevices, "Latest Android (API 33-34)")
    }
    
    // =====================================================
    // CHIPSET-SPECIFIC COMPATIBILITY TESTS
    // =====================================================
    
    @Test
    fun `test Qualcomm chipset compatibility`() = runTest {
        val qualcommDevices = listOf(
            ChipsetTestCase(
                chipsetName = "Snapdragon 8 Gen 3",
                boardInfo = "qcom_sm8550",
                expectedHTPreSupport = true,
                expectedNNAPISupport = true,
                expectedPerformanceLevel = PerformanceLevel.FLAGSHIP
            ),
            ChipsetTestCase(
                chipsetName = "Snapdragon 7+ Gen 2",
                boardInfo = "qcom_sm7475",
                expectedHTPreSupport = true,
                expectedNNAPISupport = true,
                expectedPerformanceLevel = PerformanceLevel.HIGH_END
            ),
            ChipsetTestCase(
                chipsetName = "Snapdragon 6 Gen 1",
                boardInfo = "qcom_sm6450",
                expectedHTPreSupport = true,
                expectedNNAPISupport = true,
                expectedPerformanceLevel = PerformanceLevel.MID_RANGE
            ),
            ChipsetTestCase(
                chipsetName = "Snapdragon 4 Gen 1",
                boardInfo = "qcom_sm4375",
                expectedHTPreSupport = false, // Lower-end chipsets may not support HTP
                expectedNNAPISupport = true,
                expectedPerformanceLevel = PerformanceLevel.BUDGET
            )
        )
        
        testChipsetCompatibility(qualcommDevices)
    }
    
    @Test
    fun `test Samsung Exynos chipset compatibility`() = runTest {
        val exynosDevices = listOf(
            ChipsetTestCase(
                chipsetName = "Exynos 2400",
                boardInfo = "exynos2400",
                expectedHTPreSupport = false, // No Qualcomm HTP
                expectedNNAPISupport = true,
                expectedPerformanceLevel = PerformanceLevel.FLAGSHIP
            ),
            ChipsetTestCase(
                chipsetName = "Exynos 1480",
                boardInfo = "exynos1480",
                expectedHTPreSupport = false,
                expectedNNAPISupport = true,
                expectedPerformanceLevel = PerformanceLevel.MID_RANGE
            )
        )
        
        testChipsetCompatibility(exynosDevices)
    }
    
    @Test
    fun `test Google Tensor chipset compatibility`() = runTest {
        val tensorDevices = listOf(
            ChipsetTestCase(
                chipsetName = "Google Tensor G3",
                boardInfo = "google_tensor_g3",
                expectedHTPreSupport = false, // Custom NPU architecture
                expectedNNAPISupport = true,
                expectedPerformanceLevel = PerformanceLevel.FLAGSHIP
            ),
            ChipsetTestCase(
                chipsetName = "Google Tensor G2",
                boardInfo = "google_tensor_g2",
                expectedHTPreSupport = false,
                expectedNNAPISupport = true,
                expectedPerformanceLevel = PerformanceLevel.HIGH_END
            )
        )
        
        testChipsetCompatibility(tensorDevices)
    }
    
    // =====================================================
    // MEMORY TIER COMPATIBILITY TESTS
    // =====================================================
    
    @Test
    fun `test memory tier compatibility`() = runTest {
        val memoryTiers = listOf(
            MemoryTierTestCase(
                tierName = "Ultra High Memory (16GB+)",
                memoryMB = 16384,
                expectedOptimalBackend = LiteRTBackend.NPU_QTI_HTP,
                expectedConcurrentAnalyses = 4,
                expectedMaxImageResolution = Pair(7680, 4320) // 8K
            ),
            MemoryTierTestCase(
                tierName = "High Memory (12GB)",
                memoryMB = 12288,
                expectedOptimalBackend = LiteRTBackend.NPU_QTI_HTP,
                expectedConcurrentAnalyses = 3,
                expectedMaxImageResolution = Pair(3840, 2160) // 4K
            ),
            MemoryTierTestCase(
                tierName = "Standard Memory (8GB)",
                memoryMB = 8192,
                expectedOptimalBackend = LiteRTBackend.NPU_NNAPI,
                expectedConcurrentAnalyses = 2,
                expectedMaxImageResolution = Pair(1920, 1080) // FHD
            ),
            MemoryTierTestCase(
                tierName = "Mid-Range Memory (6GB)",
                memoryMB = 6144,
                expectedOptimalBackend = LiteRTBackend.GPU_OPENCL,
                expectedConcurrentAnalyses = 2,
                expectedMaxImageResolution = Pair(1920, 1080) // FHD
            ),
            MemoryTierTestCase(
                tierName = "Low Memory (4GB)",
                memoryMB = 4096,
                expectedOptimalBackend = LiteRTBackend.GPU_OPENGL,
                expectedConcurrentAnalyses = 1,
                expectedMaxImageResolution = Pair(1280, 720) // HD
            ),
            MemoryTierTestCase(
                tierName = "Very Low Memory (2GB)",
                memoryMB = 2048,
                expectedOptimalBackend = LiteRTBackend.CPU,
                expectedConcurrentAnalyses = 1,
                expectedMaxImageResolution = Pair(640, 480) // VGA
            )
        )
        
        testMemoryTierCompatibility(memoryTiers)
    }
    
    // =====================================================
    // STRESS TESTING SCENARIOS
    // =====================================================
    
    @Test
    fun `test device stress scenarios`() = runTest {
        val stressScenarios = listOf(
            StressTestScenario(
                name = "High Temperature Conditions",
                setup = { mockEngine -> 
                    mockEngine.setDeviceTemperature(75f) // Near thermal throttling
                },
                expectedBehavior = StressExpectedBehavior.PERFORMANCE_DEGRADATION
            ),
            StressTestScenario(
                name = "Low Battery Conditions",
                setup = { mockEngine ->
                    mockEngine.setBatteryLevel(15) // Low battery
                    mockEngine.setPowerSavingMode(true)
                },
                expectedBehavior = StressExpectedBehavior.POWER_SAVING_MODE
            ),
            StressTestScenario(
                name = "Memory Pressure Conditions",
                setup = { mockEngine ->
                    mockEngine.setAvailableMemory(300f) // Very low available memory
                },
                expectedBehavior = StressExpectedBehavior.MEMORY_ERROR_OR_FALLBACK
            ),
            StressTestScenario(
                name = "Concurrent App Competition",
                setup = { mockEngine ->
                    mockEngine.setConcurrentAppLoad(true) // Other apps using AI resources
                },
                expectedBehavior = StressExpectedBehavior.RESOURCE_CONTENTION
            )
        )
        
        testStressScenarios(stressScenarios)
    }
    
    // =====================================================
    // HELPER TEST METHODS
    // =====================================================
    
    private suspend fun testDeviceConfigurations(devices: List<DeviceConfiguration>, categoryName: String) {
        val results = mutableListOf<DeviceTestResult>()
        
        devices.forEach { device ->
            val mockEngine = MockLiteRTModelEngine()
            
            try {
                // Configure mock engine with device specs
                mockEngine.setDeviceCapabilities(
                    totalMemoryGB = device.totalMemoryMB / 1024f,
                    supportedBackends = device.expectedBackends
                )
                mockEngine.setAndroidVersion(device.apiLevel)
                mockEngine.setBoardInfo(device.boardInfo)
                
                // Test initialization
                val initResult = mockEngine.initialize("test_model.litertmlm", LiteRTBackend.AUTO)
                val initSuccess = initResult.isSuccess
                val actualOptimalBackend = mockEngine.currentBackend
                
                // Test analysis performance
                val testImage = testDataFactory.createStandardTestImage()
                val (analysisResult, duration) = TestUtils.measureExecutionTime {
                    mockEngine.generateSafetyAnalysis(
                        imageData = testImage,
                        workType = WorkType.GENERAL_CONSTRUCTION
                    )
                }
                
                val analysisSuccess = analysisResult.isSuccess
                val processingTime = duration.inWholeMilliseconds
                val metrics = mockEngine.getPerformanceMetrics()
                
                val testResult = DeviceTestResult(
                    deviceName = device.name,
                    apiLevel = device.apiLevel,
                    initializationSuccess = initSuccess,
                    backendSelectionCorrect = actualOptimalBackend == device.expectedOptimalBackend,
                    analysisSuccess = analysisSuccess,
                    processingTimeMs = processingTime,
                    memoryUsageMB = metrics.averageMemoryUsageMB,
                    meetsPerformanceExpectations = meetsPerformanceExpectations(
                        processingTime, metrics.successRate, metrics.averageMemoryUsageMB,
                        device.performanceExpectations
                    )
                )
                
                results.add(testResult)
                
                // Assertions for this device
                assertTrue(initSuccess, "${device.name}: Should initialize successfully")
                assertEquals(
                    device.expectedOptimalBackend,
                    actualOptimalBackend,
                    "${device.name}: Should select optimal backend"
                )
                assertTrue(analysisSuccess, "${device.name}: Analysis should succeed")
                
                // Performance assertions
                assertTrue(
                    processingTime <= device.performanceExpectations.maxProcessingTimeMs,
                    "${device.name}: Processing time ${processingTime}ms should be <= ${device.performanceExpectations.maxProcessingTimeMs}ms"
                )
                
                assertTrue(
                    metrics.successRate >= device.performanceExpectations.minSuccessRate,
                    "${device.name}: Success rate ${metrics.successRate} should be >= ${device.performanceExpectations.minSuccessRate}"
                )
                
                assertTrue(
                    metrics.averageMemoryUsageMB <= device.performanceExpectations.maxMemoryUsageMB,
                    "${device.name}: Memory usage ${metrics.averageMemoryUsageMB}MB should be <= ${device.performanceExpectations.maxMemoryUsageMB}MB"
                )
                
            } finally {
                mockEngine.cleanup()
            }
        }
        
        // Generate compatibility report
        generateCompatibilityReport(categoryName, results)
    }
    
    private suspend fun testChipsetCompatibility(chipsets: List<ChipsetTestCase>) {
        chipsets.forEach { chipset ->
            val mockEngine = MockLiteRTModelEngine()
            
            try {
                mockEngine.setBoardInfo(chipset.boardInfo)
                mockEngine.setAndroidVersion(33) // Use modern Android version
                
                val supportedBackends = mockEngine.supportedBackends
                
                // Test HTP support
                val actualHTPreSupport = supportedBackends.contains(LiteRTBackend.NPU_QTI_HTP)
                assertEquals(
                    chipset.expectedHTPreSupport,
                    actualHTPreSupport,
                    "${chipset.chipsetName}: HTP support should be ${chipset.expectedHTPreSupport}"
                )
                
                // Test NNAPI support
                val actualNNAPISupport = supportedBackends.contains(LiteRTBackend.NPU_NNAPI)
                assertEquals(
                    chipset.expectedNNAPISupport,
                    actualNNAPISupport,
                    "${chipset.chipsetName}: NNAPI support should be ${chipset.expectedNNAPISupport}"
                )
                
                // Test performance characteristics
                val memoryForTier = getMemoryForPerformanceLevel(chipset.expectedPerformanceLevel)
                mockEngine.setDeviceCapabilities(totalMemoryGB = memoryForTier)
                
                val initResult = mockEngine.initialize("test_model.litertmlm", LiteRTBackend.AUTO)
                assertTrue(initResult.isSuccess, "${chipset.chipsetName}: Should initialize successfully")
                
                // Test analysis with performance expectations
                val testImage = testDataFactory.createStandardTestImage()
                val analysisResult = mockEngine.generateSafetyAnalysis(
                    imageData = testImage,
                    workType = WorkType.GENERAL_CONSTRUCTION
                )
                
                assertTrue(analysisResult.isSuccess, "${chipset.chipsetName}: Analysis should succeed")
                
                val expectedMaxTime = getMaxProcessingTimeForLevel(chipset.expectedPerformanceLevel)
                val actualTime = analysisResult.getOrNull()!!.processingTimeMs
                
                assertTrue(
                    actualTime <= expectedMaxTime,
                    "${chipset.chipsetName}: Processing time ${actualTime}ms should be <= ${expectedMaxTime}ms for ${chipset.expectedPerformanceLevel} tier"
                )
                
            } finally {
                mockEngine.cleanup()
            }
        }
    }
    
    private suspend fun testMemoryTierCompatibility(tiers: List<MemoryTierTestCase>) {
        tiers.forEach { tier ->
            val mockEngine = MockLiteRTModelEngine()
            
            try {
                mockEngine.setDeviceCapabilities(
                    totalMemoryGB = tier.memoryMB / 1024f,
                    supportedBackends = setOf(
                        LiteRTBackend.CPU,
                        LiteRTBackend.GPU_OPENGL,
                        LiteRTBackend.GPU_OPENCL,
                        LiteRTBackend.NPU_NNAPI,
                        LiteRTBackend.NPU_QTI_HTP
                    )
                )
                
                val initResult = mockEngine.initialize("test_model.litertmlm", LiteRTBackend.AUTO)
                assertTrue(initResult.isSuccess, "${tier.tierName}: Should initialize successfully")
                
                val actualOptimalBackend = mockEngine.currentBackend
                assertEquals(
                    tier.expectedOptimalBackend,
                    actualOptimalBackend,
                    "${tier.tierName}: Should select optimal backend"
                )
                
                // Test maximum image resolution handling
                val largeImage = testDataFactory.createTestImage(
                    tier.expectedMaxImageResolution.first,
                    tier.expectedMaxImageResolution.second
                )
                
                val analysisResult = mockEngine.generateSafetyAnalysis(
                    imageData = largeImage,
                    workType = WorkType.GENERAL_CONSTRUCTION
                )
                
                assertTrue(
                    analysisResult.isSuccess,
                    "${tier.tierName}: Should handle maximum resolution ${tier.expectedMaxImageResolution.first}x${tier.expectedMaxImageResolution.second}"
                )
                
                // Test concurrent analysis capability
                if (tier.expectedConcurrentAnalyses > 1) {
                    val concurrentResults = mutableListOf<Result<LiteRTAnalysisResult>>()
                    
                    repeat(tier.expectedConcurrentAnalyses) {
                        val result = mockEngine.generateSafetyAnalysis(
                            imageData = testDataFactory.createStandardTestImage(),
                            workType = WorkType.GENERAL_CONSTRUCTION
                        )
                        concurrentResults.add(result)
                    }
                    
                    val successfulAnalyses = concurrentResults.count { it.isSuccess }
                    assertTrue(
                        successfulAnalyses >= tier.expectedConcurrentAnalyses - 1, // Allow for 1 failure
                        "${tier.tierName}: Should handle ${tier.expectedConcurrentAnalyses} concurrent analyses"
                    )
                }
                
            } finally {
                mockEngine.cleanup()
            }
        }
    }
    
    private suspend fun testStressScenarios(scenarios: List<StressTestScenario>) {
        scenarios.forEach { scenario ->
            val mockEngine = MockLiteRTModelEngine()
            
            try {
                // Apply stress scenario setup
                scenario.setup(mockEngine)
                
                val initResult = mockEngine.initialize("test_model.litertmlm", LiteRTBackend.AUTO)
                val testImage = testDataFactory.createStandardTestImage()
                
                val analysisResult = mockEngine.generateSafetyAnalysis(
                    imageData = testImage,
                    workType = WorkType.GENERAL_CONSTRUCTION
                )
                
                when (scenario.expectedBehavior) {
                    StressExpectedBehavior.PERFORMANCE_DEGRADATION -> {
                        assertTrue(initResult.isSuccess, "${scenario.name}: Should still initialize")
                        if (analysisResult.isSuccess) {
                            val processingTime = analysisResult.getOrNull()!!.processingTimeMs
                            assertTrue(
                                processingTime > 2000L,
                                "${scenario.name}: Should show performance degradation (slower processing)"
                            )
                        }
                    }
                    
                    StressExpectedBehavior.POWER_SAVING_MODE -> {
                        assertTrue(initResult.isSuccess, "${scenario.name}: Should initialize with power saving")
                        if (analysisResult.isSuccess) {
                            // Should use CPU backend in power saving mode
                            val backend = analysisResult.getOrNull()!!.backendUsed
                            assertEquals(
                                LiteRTBackend.CPU,
                                backend,
                                "${scenario.name}: Should use CPU backend for power saving"
                            )
                        }
                    }
                    
                    StressExpectedBehavior.MEMORY_ERROR_OR_FALLBACK -> {
                        // Either initialization fails or analysis fails with memory error
                        if (initResult.isFailure) {
                            assertTrue(
                                initResult.exceptionOrNull() is LiteRTException.OutOfMemoryException,
                                "${scenario.name}: Should fail with OutOfMemoryException"
                            )
                        } else if (analysisResult.isFailure) {
                            assertTrue(
                                analysisResult.exceptionOrNull() is LiteRTException.OutOfMemoryException,
                                "${scenario.name}: Should fail analysis with OutOfMemoryException"
                            )
                        }
                    }
                    
                    StressExpectedBehavior.RESOURCE_CONTENTION -> {
                        // Should still work but with degraded performance
                        assertTrue(initResult.isSuccess, "${scenario.name}: Should handle resource contention")
                        if (analysisResult.isSuccess) {
                            val processingTime = analysisResult.getOrNull()!!.processingTimeMs
                            assertTrue(
                                processingTime > 1500L,
                                "${scenario.name}: Should show slower processing due to resource contention"
                            )
                        }
                    }
                }
                
            } finally {
                mockEngine.cleanup()
            }
        }
    }
    
    private fun meetsPerformanceExpectations(
        processingTime: Long,
        successRate: Float,
        memoryUsage: Float,
        expectations: PerformanceExpectations
    ): Boolean {
        return processingTime <= expectations.maxProcessingTimeMs &&
               successRate >= expectations.minSuccessRate &&
               memoryUsage <= expectations.maxMemoryUsageMB
    }
    
    private fun getMemoryForPerformanceLevel(level: PerformanceLevel): Float {
        return when (level) {
            PerformanceLevel.FLAGSHIP -> 12f
            PerformanceLevel.HIGH_END -> 8f
            PerformanceLevel.MID_RANGE -> 6f
            PerformanceLevel.BUDGET -> 4f
        }
    }
    
    private fun getMaxProcessingTimeForLevel(level: PerformanceLevel): Long {
        return when (level) {
            PerformanceLevel.FLAGSHIP -> 800L
            PerformanceLevel.HIGH_END -> 1200L
            PerformanceLevel.MID_RANGE -> 2000L
            PerformanceLevel.BUDGET -> 3500L
        }
    }
    
    private fun generateCompatibilityReport(categoryName: String, results: List<DeviceTestResult>) {
        println("\n=== $categoryName Compatibility Report ===")
        
        val totalDevices = results.size
        val successfulInits = results.count { it.initializationSuccess }
        val successfulAnalyses = results.count { it.analysisSuccess }
        val correctBackendSelections = results.count { it.backendSelectionCorrect }
        val meetsPerformance = results.count { it.meetsPerformanceExpectations }
        
        println("Total devices tested: $totalDevices")
        println("Successful initializations: $successfulInits/$totalDevices (${(successfulInits * 100 / totalDevices)}%)")
        println("Successful analyses: $successfulAnalyses/$totalDevices (${(successfulAnalyses * 100 / totalDevices)}%)")
        println("Correct backend selections: $correctBackendSelections/$totalDevices (${(correctBackendSelections * 100 / totalDevices)}%)")
        println("Meet performance expectations: $meetsPerformance/$totalDevices (${(meetsPerformance * 100 / totalDevices)}%)")
        
        println("\nDevice-by-device results:")
        results.forEach { result ->
            val status = if (result.analysisSuccess && result.meetsPerformanceExpectations) "✅ PASS" else "❌ FAIL"
            println("  ${result.deviceName} (API ${result.apiLevel}): $status - ${result.processingTimeMs}ms, ${result.memoryUsageMB}MB")
        }
    }
}

// =====================================================
// DATA CLASSES FOR DEVICE TESTING
// =====================================================

data class DeviceConfiguration(
    val name: String,
    val apiLevel: Int,
    val totalMemoryMB: Int,
    val boardInfo: String,
    val expectedBackends: Set<LiteRTBackend>,
    val expectedOptimalBackend: LiteRTBackend,
    val performanceExpectations: PerformanceExpectations
)

data class PerformanceExpectations(
    val maxProcessingTimeMs: Long,
    val minSuccessRate: Float,
    val maxMemoryUsageMB: Float
)

data class DeviceTestResult(
    val deviceName: String,
    val apiLevel: Int,
    val initializationSuccess: Boolean,
    val backendSelectionCorrect: Boolean,
    val analysisSuccess: Boolean,
    val processingTimeMs: Long,
    val memoryUsageMB: Float,
    val meetsPerformanceExpectations: Boolean
)

data class ChipsetTestCase(
    val chipsetName: String,
    val boardInfo: String,
    val expectedHTPreSupport: Boolean,
    val expectedNNAPISupport: Boolean,
    val expectedPerformanceLevel: PerformanceLevel
)

enum class PerformanceLevel {
    FLAGSHIP, HIGH_END, MID_RANGE, BUDGET
}

data class MemoryTierTestCase(
    val tierName: String,
    val memoryMB: Int,
    val expectedOptimalBackend: LiteRTBackend,
    val expectedConcurrentAnalyses: Int,
    val expectedMaxImageResolution: Pair<Int, Int>
)

data class StressTestScenario(
    val name: String,
    val setup: (MockLiteRTModelEngine) -> Unit,
    val expectedBehavior: StressExpectedBehavior
)

enum class StressExpectedBehavior {
    PERFORMANCE_DEGRADATION,
    POWER_SAVING_MODE,
    MEMORY_ERROR_OR_FALLBACK,
    RESOURCE_CONTENTION
}

class DeviceCompatibilityMatrix {
    // Central registry for device compatibility data and test results
    private val testResults = mutableMapOf<String, DeviceTestResult>()
    
    fun recordResult(deviceName: String, result: DeviceTestResult) {
        testResults[deviceName] = result
    }
    
    fun getCompatibilityScore(): Float {
        if (testResults.isEmpty()) return 0f
        
        val totalScore = testResults.values.sumOf { result ->
            var score = 0
            if (result.initializationSuccess) score += 25
            if (result.backendSelectionCorrect) score += 25
            if (result.analysisSuccess) score += 25
            if (result.meetsPerformanceExpectations) score += 25
            score
        }
        
        return totalScore.toFloat() / (testResults.size * 100)
    }
    
    fun generateMatrix(): Map<String, Map<String, Boolean>> {
        return testResults.mapValues { (_, result) ->
            mapOf(
                "Initialization" to result.initializationSuccess,
                "Backend Selection" to result.backendSelectionCorrect,
                "Analysis" to result.analysisSuccess,
                "Performance" to result.meetsPerformanceExpectations
            )
        }
    }
}

// Extension methods for MockLiteRTModelEngine to support stress testing
fun MockLiteRTModelEngine.setBatteryLevel(level: Int) {
    // Mock implementation - would set battery level flag
}

fun MockLiteRTModelEngine.setPowerSavingMode(enabled: Boolean) {
    // Mock implementation - would enable power saving mode
}

fun MockLiteRTModelEngine.setConcurrentAppLoad(enabled: Boolean) {
    // Mock implementation - would simulate other apps using resources
}
