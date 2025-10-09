package com.hazardhawk.platform

/**
 * Simple platform test to verify expect/actual pattern and iOS compilation
 */
expect class PlatformTest() {
    fun getPlatformName(): String
    fun testFileReading(filePath: String): String
}

class MultiplatformTester {
    private val platformTest = PlatformTest()
    
    fun runTest(): String {
        val platform = platformTest.getPlatformName()
        val testFile = "test.txt"
        val result = platformTest.testFileReading(testFile)
        return "Platform: $platform, File test: $result"
    }
}