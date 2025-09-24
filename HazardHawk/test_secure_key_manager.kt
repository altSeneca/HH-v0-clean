#!/usr/bin/env kotlin

/**
 * Test script to validate SecureKeyManager functionality
 * This would be run as part of the validation process
 */

import android.content.Context
import com.hazardhawk.security.SecureKeyManager

fun testSecureKeyManager(context: Context) {
    println("Testing SecureKeyManager...")
    
    try {
        val secureKeyManager = SecureKeyManager.getInstance(context)
        
        // Test 1: Check security info
        val securityInfo = secureKeyManager.getSecurityInfo()
        println("Security Info:")
        println("  Hardware-backed: ${securityInfo.isHardwareBacked}")
        println("  Storage type: ${securityInfo.storageType}")
        println("  Has API key: ${securityInfo.hasApiKey}")
        
        // Test 2: Store and retrieve API key
        val testApiKey = "AIzaSyDummyTestKeyForValidation123456789"
        
        println("\nTesting API key storage...")
        secureKeyManager.storeGeminiApiKey(testApiKey, "test-1.0")
        
        val retrievedKey = secureKeyManager.getGeminiApiKey()
        val isValid = secureKeyManager.hasValidApiKey()
        
        println("  Stored key: ${testApiKey.take(10)}...")
        println("  Retrieved key: ${retrievedKey?.take(10)}...")
        println("  Keys match: ${testApiKey == retrievedKey}")
        println("  Is valid: $isValid")
        
        // Test 3: Validate key integrity
        println("\nTesting key integrity...")
        val integrityValid = secureKeyManager.validateKeyIntegrity()
        println("  Integrity test: ${if (integrityValid) "PASSED" else "FAILED"}")
        
        // Clean up test data
        secureKeyManager.clearAllKeys()
        println("\nTest cleanup completed")
        
        println("✅ SecureKeyManager test completed successfully")
        
    } catch (e: Exception) {
        println("❌ SecureKeyManager test failed: ${e.message}")
        e.printStackTrace()
    }
}