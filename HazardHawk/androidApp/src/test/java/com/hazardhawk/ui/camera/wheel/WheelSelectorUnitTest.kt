package com.hazardhawk.ui.camera.wheel

import com.hazardhawk.ui.components.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import kotlin.math.abs

/**
 * Unit tests for WheelSelector logic and state management
 * Tests the core wheel functionality without UI dependencies
 */
class WheelSelectorUnitTest {

    private lateinit var wheelLogic: WheelSelectorLogic
    private lateinit var testItems: List<WheelItem>

    @Before
    fun setup() {
        wheelLogic = WheelSelectorLogic()
        testItems = listOf(
            WheelItem("item1", "1×", value = 1f),
            WheelItem("item2", "2×", value = 2f),
            WheelItem("item3", "5×", value = 5f),
            WheelItem("item4", "10×", value = 10f)
        )
    }

    @Test
    fun `test zoom wheel visibility with various zoom ranges`() {
        // Test normal zoom range
        val normalZoom = ZoomLevels.generateZoomItems(0.5f, 20f)
        assertFalse("Normal zoom range should not be empty", normalZoom.isEmpty())
        assertTrue("Should contain 1x zoom", normalZoom.any { (it.value as Float) == 1f })

        // Test limited zoom range
        val limitedZoom = ZoomLevels.generateZoomItems(0.8f, 1.2f)
        assertFalse("Limited zoom range should not be empty", limitedZoom.isEmpty())

        // Test extreme zoom range  
        val extremeZoom = ZoomLevels.generateZoomItems(0.1f, 100f)
        assertFalse("Extreme zoom range should not be empty", extremeZoom.isEmpty())
        assertTrue("Should handle extreme values", extremeZoom.size > 3)
    }

    @Test
    fun `test zoom jumping prevention through state calculation`() {
        val config = WheelSelectorConfig()
        
        // Test center item (should be fully selected)
        val centerStates = wheelLogic.calculateItemStates(testItems, 2, 0f, config)
        val centerItem = centerStates[2]
        
        assertTrue("Center item should be selected", centerItem.isSelected)
        assertEquals("Center item should have full scale", 1.1f, centerItem.scale, 0.01f)
        assertEquals("Center item should have full alpha", 1f, centerItem.alpha, 0.01f)

        // Test slight offset (should still be selected)
        val offsetStates = wheelLogic.calculateItemStates(testItems, 2, 0.3f, config)
        val offsetItem = offsetStates[2]
        
        assertTrue("Slightly offset item should still be selected", offsetItem.isSelected)
    }

    @Test
    fun `test continuous value calculation for live zoom`() {
        // Test exact item position
        val exactValue = wheelLogic.calculateContinuousValue(testItems, 1, 0f)
        assertEquals("Exact position should return item value", 2f, exactValue, 0.01f)

        // Test interpolation between items
        val interpolatedValue = wheelLogic.calculateContinuousValue(testItems, 1, 0.5f)
        assertTrue("Interpolated value should be between 2 and 5", 
            interpolatedValue > 2f && interpolatedValue < 5f)
        assertEquals("Should interpolate halfway between 2 and 5", 3.5f, interpolatedValue, 0.1f)

        // Test edge cases
        val edgeValue = wheelLogic.calculateContinuousValue(testItems, 0, -0.5f)
        assertEquals("Edge case should clamp to first item", 1f, edgeValue, 0.01f)
    }

    @Test
    fun `test snap target calculation prevents jumping`() {
        // Test low velocity (should stay at current index)
        val lowVelocityTarget = wheelLogic.calculateSnapTarget(2, 10f, testItems.size)
        assertEquals("Low velocity should maintain current index", 2, lowVelocityTarget)

        // Test high positive velocity
        val positiveVelocityTarget = wheelLogic.calculateSnapTarget(1, 100f, testItems.size)
        assertEquals("High positive velocity should move to next item", 2, positiveVelocityTarget)

        // Test high negative velocity
        val negativeVelocityTarget = wheelLogic.calculateSnapTarget(2, -100f, testItems.size)
        assertEquals("High negative velocity should move to previous item", 1, negativeVelocityTarget)

        // Test boundary conditions
        val boundaryTarget = wheelLogic.calculateSnapTarget(3, 100f, testItems.size)
        assertEquals("Should not exceed max index", 3, boundaryTarget)
    }

    @Test
    fun `test aspect ratio definitions are mathematically correct`() {
        assertEquals("Full aspect ratio should be 0", 0f, AspectRatios.FULL.value as Float)
        assertEquals("16:9 ratio should be correct", 16f/9f, AspectRatios.RATIO_16_9.value as Float, 0.001f)
        assertEquals("4:3 ratio should be correct", 4f/3f, AspectRatios.RATIO_4_3.value as Float, 0.001f)
        assertEquals("1:1 ratio should be correct", 1f, AspectRatios.RATIO_1_1.value as Float)

        // Test all supported ratios are unique
        val supportedRatios = AspectRatios.getSupported()
        val uniqueValues = supportedRatios.map { it.value }.toSet()
        assertEquals("All aspect ratios should be unique", supportedRatios.size, uniqueValues.size)
    }

    @Test
    fun `test zoom level generation consistency`() {
        val minZoom = 0.5f
        val maxZoom = 10f
        val zoomLevels = ZoomLevels.generateZoomItems(minZoom, maxZoom)

        // Verify all items are within range
        zoomLevels.forEach { item ->
            val zoom = item.value as Float
            assertTrue("Zoom ${zoom} should be >= ${minZoom}", zoom >= minZoom)
            assertTrue("Zoom ${zoom} should be <= ${maxZoom}", zoom <= maxZoom)
        }

        // Verify sorting
        val zoomValues = zoomLevels.map { it.value as Float }
        val sortedValues = zoomValues.sorted()
        assertEquals("Zoom levels should be sorted", sortedValues, zoomValues)

        // Verify labels match values
        zoomLevels.forEach { item ->
            val zoom = item.value as Float
            val expectedLabel = when {
                zoom < 1f -> "${(zoom * 10).toInt() / 10f}×"
                zoom == zoom.toInt().toFloat() -> "${zoom.toInt()}×"
                else -> "${String.format("%.1f", zoom)}×"
            }
            assertEquals("Label should match value for ${zoom}", expectedLabel, item.label)
        }
    }

    @Test
    fun `test wheel item state scaling animation values`() {
        val config = WheelSelectorConfig()
        
        // Test different distances from center
        val testDistances = listOf(0f, 0.5f, 1f, 1.5f, 2f)
        
        testDistances.forEach { distance ->
            val states = wheelLogic.calculateItemStates(testItems, 1, distance, config)
            val itemState = states[1]
            
            when {
                abs(distance) < 0.5f -> {
                    assertEquals("Center items should have 110% scale", 
                        1.1f, itemState.scale, 0.01f)
                    assertEquals("Center items should have full alpha", 
                        1f, itemState.alpha, 0.01f)
                }
                abs(distance) < 1.5f -> {
                    assertTrue("Neighbor items should have scale between 85% and 110%",
                        itemState.scale >= 0.85f && itemState.scale <= 1.1f)
                    assertTrue("Neighbor items should have alpha between 70% and 100%",
                        itemState.alpha >= 0.7f && itemState.alpha <= 1f)
                }
                else -> {
                    assertEquals("Far items should have 70% scale",
                        0.7f, itemState.scale, 0.01f)
                    assertEquals("Far items should have 50% alpha",
                        0.5f, itemState.alpha, 0.01f)
                }
            }
        }
    }

    @Test
    fun `test wheel configuration parameters`() {
        val defaultConfig = WheelSelectorConfig()
        
        assertEquals("Default item height should be 52dp", 52f, defaultConfig.itemHeight)
        assertEquals("Default visible count should be 5", 5, defaultConfig.visibleCount)
        assertTrue("Snap should be enabled by default", defaultConfig.snapEnabled)
        assertTrue("Haptic should be enabled by default", defaultConfig.hapticEnabled)
        assertEquals("Default animation duration should be 200ms", 
            200, defaultConfig.animationDurationMs)

        // Test custom configuration
        val customConfig = WheelSelectorConfig(
            itemHeight = 60f,
            visibleCount = 7,
            snapEnabled = false,
            hapticEnabled = false,
            side = WheelSide.RIGHT,
            animationDurationMs = 300
        )
        
        assertEquals("Custom item height should be applied", 60f, customConfig.itemHeight)
        assertEquals("Custom visible count should be applied", 7, customConfig.visibleCount)
        assertFalse("Custom snap setting should be applied", customConfig.snapEnabled)
        assertFalse("Custom haptic setting should be applied", customConfig.hapticEnabled)
        assertEquals("Custom side should be applied", WheelSide.RIGHT, customConfig.side)
        assertEquals("Custom animation duration should be applied", 
            300, customConfig.animationDurationMs)
    }

    @Test
    fun `test wheel item equality and hashing`() {
        val item1 = WheelItem("zoom_2", "2×", value = 2f)
        val item2 = WheelItem("zoom_2", "2×", value = 2f)
        val item3 = WheelItem("zoom_3", "3×", value = 3f)
        
        assertEquals("Items with same id should be equal", item1, item2)
        assertNotEquals("Items with different id should not be equal", item1, item3)
        
        assertEquals("Equal items should have same hash code", item1.hashCode(), item2.hashCode())
    }

    @Test
    fun `test empty and null wheel item lists`() {
        val emptyList = emptyList<WheelItem>()
        
        // Should not crash with empty list
        val emptyStates = wheelLogic.calculateItemStates(emptyList, 0, 0f, WheelSelectorConfig())
        assertTrue("Empty list should return empty states", emptyStates.isEmpty())
        
        val emptyValue = wheelLogic.calculateContinuousValue(emptyList, 0, 0f)
        assertEquals("Empty list should return 0 value", 0f, emptyValue)
        
        val emptyTarget = wheelLogic.calculateSnapTarget(0, 0f, 0)
        assertEquals("Empty list should return 0 target", 0, emptyTarget)
    }
}
