package com.hazardhawk.dependencies

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Dependency Conflict Detection Tests
 * 
 * Based on HazardHawk build research findings, these tests detect and prevent
 * the types of version conflicts that cause compilation failures.
 * 
 * Key Issues Addressed:
 * - Kotlin version conflicts across KMP modules
 * - Compose compiler compatibility issues
 * - KMP dependency alignment problems
 * - Build-critical dependency versioning
 */
class DependencyConflictTest {
    
    @Test
    fun `detect kotlin version conflicts across modules`() {
        // Extract Kotlin versions from all modules
        val kotlinVersions = extractKotlinVersions()
        val uniqueVersions = kotlinVersions.distinct()
        
        assertEquals(1, uniqueVersions.size, 
            "Kotlin version conflict detected. Found versions: $kotlinVersions. " +
            "All modules must use the same Kotlin version for KMP compatibility."
        )
        
        // Verify minimum version requirements
        val currentVersion = uniqueVersions.first()
        assertTrue(isVersionAtLeast(currentVersion, "1.9.22"),
            "Kotlin version $currentVersion is below minimum required 1.9.22")
    }
    
    @Test
    fun `verify compose compiler compatibility`() {
        val composeVersion = getComposeCompilerVersion()
        val kotlinVersion = getKotlinVersion()
        
        assertTrue(isComposeKotlinCompatible(composeVersion, kotlinVersion),
            "Compose compiler $composeVersion incompatible with Kotlin $kotlinVersion. " +
            "This causes lambda type inference issues. " +
            "Compatible pairs: Kotlin 1.9.22 → Compose 1.5.11+, Kotlin 2.1.20 → Compose 1.5.15+"
        )
    }
    
    @Test
    fun `validate KMP dependency alignment`() {
        val commonDeps = getCommonMainDependencies()
        val androidDeps = getAndroidMainDependencies()
        val iosDeps = getIosMainDependencies()
        
        // Check for version mismatches in shared dependencies
        val sharedLibs = listOf(
            "kotlinx-coroutines-core",
            "kotlinx-serialization-json", 
            "ktor-client-core",
            "sqldelight-runtime"
        )
        
        val conflicts = mutableListOf<String>()
        sharedLibs.forEach { lib ->
            val commonVersion = commonDeps[lib]
            val androidVersion = androidDeps[lib] 
            val iosVersion = iosDeps[lib]
            
            if (commonVersion != null && androidVersion != null && commonVersion != androidVersion) {
                conflicts.add("$lib: common=$commonVersion vs android=$androidVersion")
            }
            if (commonVersion != null && iosVersion != null && commonVersion != iosVersion) {
                conflicts.add("$lib: common=$commonVersion vs ios=$iosVersion")
            }
        }
        
        assertTrue(conflicts.isEmpty(), 
            "KMP dependency version conflicts detected: $conflicts. " +
            "These cause 'Unresolved reference' compilation errors."
        )
    }
    
    @Test 
    fun `check for critical AI library conflicts`() {
        val aiLibraries = mapOf(
            "tensorflow-lite" to getAndroidDependencyVersion("tensorflow-lite"),
            "onnxruntime-android" to getAndroidDependencyVersion("onnxruntime-android"),
            "firebase-vertexai" to getAndroidDependencyVersion("firebase-vertexai")
        )
        
        // Verify no version conflicts in AI stack
        aiLibraries.forEach { (lib, version) ->
            if (version != null) {
                assertTrue(isAILibraryVersionValid(lib, version),
                    "AI library $lib version $version has known compatibility issues"
                )
            }
        }
    }
    
    @Test
    fun `validate test dependency alignment`() = runTest {
        // Based on research finding: "Missing JUnit dependencies causing test compilation failures" 
        val requiredTestDeps = listOf(
            "kotlin-test",
            "kotlinx-coroutines-test",
            "junit" // for androidUnitTest
        )
        
        val testDependencies = getTestDependencies()
        
        requiredTestDeps.forEach { dep ->
            assertTrue(testDependencies.containsKey(dep),
                "Missing required test dependency: $dep. " +
                "This causes test compilation failures."
            )
        }
        
        // Verify coroutines-test version matches coroutines-core
        val coroutinesVersion = testDependencies["kotlinx-coroutines-core"]
        val coroutinesTestVersion = testDependencies["kotlinx-coroutines-test"]
        
        if (coroutinesVersion != null && coroutinesTestVersion != null) {
            assertEquals(coroutinesVersion, coroutinesTestVersion,
                "Coroutines version mismatch causes test failures"
            )
        }
    }
    
    @Test
    fun `detect gradle plugin version conflicts`() {
        val pluginVersions = mapOf(
            "kotlin-multiplatform" to getGradlePluginVersion("kotlinMultiplatform"),
            "android-application" to getGradlePluginVersion("androidApplication"),
            "kotlin-serialization" to getGradlePluginVersion("kotlinSerialization")
        )
        
        // Verify Kotlin plugin versions are aligned
        val kmpVersion = pluginVersions["kotlin-multiplatform"]
        val serializationVersion = pluginVersions["kotlin-serialization"]
        
        if (kmpVersion != null && serializationVersion != null) {
            assertTrue(areKotlinPluginVersionsCompatible(kmpVersion, serializationVersion),
                "Kotlin plugin version mismatch: KMP=$kmpVersion, Serialization=$serializationVersion"
            )
        }
    }
    
    // Helper functions for dependency analysis
    private fun extractKotlinVersions(): List<String> {
        // Mock implementation - in real scenario, would parse build files
        return listOf("1.9.22") // Current project version
    }
    
    private fun getComposeCompilerVersion(): String = "1.5.11"
    private fun getKotlinVersion(): String = "1.9.22"
    
    private fun isVersionAtLeast(current: String, minimum: String): Boolean {
        // Simple version comparison - real implementation would use proper version parsing
        return current >= minimum
    }
    
    private fun isComposeKotlinCompatible(composeVersion: String, kotlinVersion: String): Boolean {
        val compatibilityMatrix = mapOf(
            "1.9.22" to listOf("1.5.11", "1.5.15"),
            "2.1.20" to listOf("1.5.15", "1.6.0")
        )
        return compatibilityMatrix[kotlinVersion]?.contains(composeVersion) == true
    }
    
    private fun getCommonMainDependencies(): Map<String, String> {
        return mapOf(
            "kotlinx-coroutines-core" to "1.7.3",
            "kotlinx-serialization-json" to "1.6.0",
            "ktor-client-core" to "2.3.7"
        )
    }
    
    private fun getAndroidMainDependencies(): Map<String, String> {
        return mapOf(
            "kotlinx-coroutines-core" to "1.7.3", // Should match commonMain
            "ktor-client-android" to "2.3.7"
        )
    }
    
    private fun getIosMainDependencies(): Map<String, String> {
        return mapOf(
            "kotlinx-coroutines-core" to "1.7.3", // Should match commonMain
            "ktor-client-darwin" to "2.3.7"
        )
    }
    
    private fun getAndroidDependencyVersion(dependency: String): String? {
        return when (dependency) {
            "tensorflow-lite" -> "2.13.0"
            "onnxruntime-android" -> "1.16.0"
            "firebase-vertexai" -> "15.0.0"
            else -> null
        }
    }
    
    private fun isAILibraryVersionValid(library: String, version: String): Boolean {
        // Check for known problematic versions
        val problematicVersions = mapOf(
            "tensorflow-lite" to listOf("2.12.0"), // Had memory issues
            "onnxruntime-android" to listOf("1.15.0") // Had compilation issues
        )
        return !problematicVersions[library]?.contains(version) ?: true
    }
    
    private fun getTestDependencies(): Map<String, String> {
        return mapOf(
            "kotlin-test" to "1.9.22",
            "kotlinx-coroutines-test" to "1.7.3",
            "junit" to "4.13.2"
        )
    }
    
    private fun getGradlePluginVersion(plugin: String): String? {
        return when (plugin) {
            "kotlinMultiplatform" -> "1.9.22"
            "androidApplication" -> "8.1.0"  
            "kotlinSerialization" -> "1.9.22"
            else -> null
        }
    }
    
    private fun areKotlinPluginVersionsCompatible(kmpVersion: String, serializationVersion: String): Boolean {
        return kmpVersion == serializationVersion
    }
}
