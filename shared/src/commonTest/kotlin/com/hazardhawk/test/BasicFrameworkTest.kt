package com.hazardhawk.test

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Basic test to verify the test framework is working correctly.
 * This test validates that the Kotlin Multiplatform test setup can execute properly.
 */
class BasicFrameworkTest {

    @Test
    fun testBasicAssertion() {
        assertEquals(4, 2 + 2, "Basic math should work")
    }

    @Test
    fun testStringOperations() {
        val testString = "HazardHawk"
        assertTrue(testString.contains("Hawk"), "String should contain 'Hawk'")
        assertEquals("HAZARDHAWK", testString.uppercase(), "Uppercase should work")
    }

    @Test
    fun testCollectionOperations() {
        val testList = listOf("safety", "construction", "osha", "compliance")
        assertEquals(4, testList.size, "List should have 4 elements")
        assertTrue(testList.contains("safety"), "List should contain 'safety'")
    }

    @Test
    fun testBasicMath() {
        val result = (10 * 5) / 2
        assertEquals(25, result, "Mathematical operations should work")
    }
}
