package com.hazardhawk.ar

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hazardhawk.ai.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Unit tests for Hazard Overlay Manager focusing on lifecycle and visibility logic.
 * Tests overlay state management, filtering, and display optimization.
 */
@RunWith(AndroidJUnit4::class)
class HazardOverlayManagerTest {

    private lateinit var overlayManager: HazardOverlayManager
    private lateinit var testHazards: List<Hazard>

    @Before
    fun setUp() {
        overlayManager = HazardOverlayManager()
        testHazards = createTestHazards()
    }

    @Test
    fun hazardOverlayManager_initializesWithEmptyState() = runTest {
        // Given
        val manager = HazardOverlayManager()
        
        // When
        val state = manager.overlayState.first()
        
        // Then
        assertTrue(state.visibleHazards.isEmpty())
        assertFalse(state.isVisible)
        assertEquals(OverlayMode.FULL, state.mode)
        assertTrue(state.animationsEnabled)
    }

    @Test
    fun hazardOverlayManager_showsHazardsCorrectly() = runTest {
        // Given
        val hazards = listOf(
            createCriticalHazard(),
            createHighHazard()
        )
        
        // When
        overlayManager.showHazards(hazards)
        
        // Then
        val state = overlayManager.overlayState.first()
        assertTrue(state.isVisible)
        assertEquals(2, state.visibleHazards.size)
        assertEquals(hazards, state.visibleHazards)
    }

    @Test
    fun hazardOverlayManager_hidesOverlayCorrectly() = runTest {
        // Given
        overlayManager.showHazards(testHazards)
        assertTrue(overlayManager.overlayState.first().isVisible)
        
        // When
        overlayManager.hideOverlay()
        
        // Then
        val state = overlayManager.overlayState.first()
        assertFalse(state.isVisible)
        assertTrue(state.visibleHazards.isEmpty())
    }

    @Test
    fun hazardOverlayManager_filtersHazardsBySeverity() = runTest {
        // Given
        val allSeverityHazards = listOf(
            createCriticalHazard(),
            createHighHazard(),
            createMediumHazard(),
            createLowHazard()
        )
        
        // When
        overlayManager.showHazards(allSeverityHazards)
        overlayManager.setMinimumSeverity(Severity.HIGH)
        
        // Then
        val state = overlayManager.overlayState.first()
        assertEquals(2, state.visibleHazards.size) // Only critical and high
        assertTrue(state.visibleHazards.all { it.severity >= Severity.HIGH })
    }

    @Test
    fun hazardOverlayManager_limitsVisibleHazardsForPerformance() = runTest {
        // Given
        val manyHazards = (1..30).map { index ->
            createMediumHazard().copy(id = "hazard-$index")
        }
        
        // When
        overlayManager.showHazards(manyHazards)
        overlayManager.setMaxVisibleHazards(15)
        
        // Then
        val state = overlayManager.overlayState.first()
        assertEquals(15, state.visibleHazards.size)
    }

    @Test
    fun hazardOverlayManager_prioritizesCriticalHazards() = runTest {
        // Given
        val mixedHazards = listOf(
            createMediumHazard().copy(id = "medium-1"),
            createCriticalHazard().copy(id = "critical-1"),
            createLowHazard().copy(id = "low-1"),
            createHighHazard().copy(id = "high-1"),
            createCriticalHazard().copy(id = "critical-2")
        )
        
        // When
        overlayManager.showHazards(mixedHazards)
        overlayManager.setMaxVisibleHazards(3)
        
        // Then
        val state = overlayManager.overlayState.first()
        assertEquals(3, state.visibleHazards.size)
        
        // Should include both critical hazards
        val criticalCount = state.visibleHazards.count { it.severity == Severity.CRITICAL }
        assertEquals(2, criticalCount)
    }

    @Test
    fun hazardOverlayManager_togglesCompactMode() = runTest {
        // Given
        overlayManager.showHazards(testHazards)
        
        // When
        overlayManager.setOverlayMode(OverlayMode.COMPACT)
        
        // Then
        val state = overlayManager.overlayState.first()
        assertEquals(OverlayMode.COMPACT, state.mode)
    }

    @Test
    fun hazardOverlayManager_filtersHazardsByWorkType() = runTest {
        // Given
        val workTypeHazards = listOf(
            createElectricalHazard(),
            createFallProtectionHazard(),
            createPPEHazard()
        )
        
        // When
        overlayManager.showHazards(workTypeHazards)
        overlayManager.setWorkTypeFilter(WorkType.ELECTRICAL_WORK)
        
        // Then
        val state = overlayManager.overlayState.first()
        assertTrue(state.visibleHazards.all { 
            it.type == HazardType.ELECTRICAL_HAZARD 
        })
    }

    @Test
    fun hazardOverlayManager_handlesHazardSelection() = runTest {
        // Given
        val hazard = createCriticalHazard()
        overlayManager.showHazards(listOf(hazard))
        
        // When
        overlayManager.selectHazard(hazard.id)
        
        // Then
        val state = overlayManager.overlayState.first()
        assertEquals(hazard.id, state.selectedHazardId)
        assertTrue(state.isHazardSelected)
    }

    @Test
    fun hazardOverlayManager_clearsHazardSelection() = runTest {
        // Given
        val hazard = createCriticalHazard()
        overlayManager.showHazards(listOf(hazard))
        overlayManager.selectHazard(hazard.id)
        assertTrue(overlayManager.overlayState.first().isHazardSelected)
        
        // When
        overlayManager.clearSelection()
        
        // Then
        val state = overlayManager.overlayState.first()
        assertNull(state.selectedHazardId)
        assertFalse(state.isHazardSelected)
    }

    @Test
    fun hazardOverlayManager_updatesHazardConfidence() = runTest {
        // Given
        val hazard = createCriticalHazard().copy(confidence = 0.7f)
        overlayManager.showHazards(listOf(hazard))
        overlayManager.setMinimumConfidence(0.8f)
        
        // When - Initial state should filter out low confidence hazard
        var state = overlayManager.overlayState.first()
        assertTrue(state.visibleHazards.isEmpty())
        
        // Update hazard confidence
        val updatedHazard = hazard.copy(confidence = 0.9f)
        overlayManager.updateHazard(updatedHazard)
        
        // Then
        state = overlayManager.overlayState.first()
        assertEquals(1, state.visibleHazards.size)
        assertEquals(0.9f, state.visibleHazards.first().confidence)
    }

    @Test
    fun hazardOverlayManager_handlesAnimationToggle() = runTest {
        // Given
        overlayManager.showHazards(testHazards)
        assertTrue(overlayManager.overlayState.first().animationsEnabled)
        
        // When
        overlayManager.setAnimationsEnabled(false)
        
        // Then
        val state = overlayManager.overlayState.first()
        assertFalse(state.animationsEnabled)
    }

    @Test
    fun hazardOverlayManager_tracksOverlayLifecycle() = runTest {
        // Given
        var lifecycleEvents = mutableListOf<OverlayLifecycleEvent>()
        overlayManager.onLifecycleEvent = { event -> lifecycleEvents.add(event) }
        
        // When
        overlayManager.showHazards(testHazards)
        overlayManager.hideOverlay()
        
        // Then
        assertTrue(lifecycleEvents.contains(OverlayLifecycleEvent.SHOWN))
        assertTrue(lifecycleEvents.contains(OverlayLifecycleEvent.HIDDEN))
    }

    @Test
    fun hazardOverlayManager_handlesRapidUpdates() = runTest {
        // Given
        val initialHazards = listOf(createCriticalHazard())
        
        // When - Rapid successive updates
        repeat(10) { index ->
            val hazards = (0..index).map { 
                createMediumHazard().copy(id = "rapid-hazard-$it") 
            }
            overlayManager.showHazards(hazards)
        }
        
        // Then - Should handle without errors and show latest state
        val state = overlayManager.overlayState.first()
        assertEquals(10, state.visibleHazards.size)
        assertTrue(state.isVisible)
    }

    @Test
    fun hazardOverlayManager_preservesSelectionDuringUpdates() = runTest {
        // Given
        val hazard = createCriticalHazard()
        overlayManager.showHazards(listOf(hazard))
        overlayManager.selectHazard(hazard.id)
        
        // When - Update with same hazard plus additional ones
        val updatedHazards = listOf(hazard, createHighHazard())
        overlayManager.showHazards(updatedHazards)
        
        // Then - Selection should be preserved
        val state = overlayManager.overlayState.first()
        assertEquals(hazard.id, state.selectedHazardId)
        assertTrue(state.isHazardSelected)
    }

    @Test
    fun hazardOverlayManager_clearsSelectionWhenHazardRemoved() = runTest {
        // Given
        val hazard = createCriticalHazard()
        overlayManager.showHazards(listOf(hazard))
        overlayManager.selectHazard(hazard.id)
        
        // When - Update without the selected hazard
        overlayManager.showHazards(listOf(createHighHazard()))
        
        // Then - Selection should be cleared
        val state = overlayManager.overlayState.first()
        assertNull(state.selectedHazardId)
        assertFalse(state.isHazardSelected)
    }

    @Test
    fun hazardOverlayManager_calculatesOverlayStatistics() = runTest {
        // Given
        val mixedHazards = listOf(
            createCriticalHazard(),
            createCriticalHazard(),
            createHighHazard(),
            createMediumHazard(),
            createMediumHazard(),
            createMediumHazard()
        )
        
        // When
        overlayManager.showHazards(mixedHazards)
        val stats = overlayManager.getOverlayStatistics()
        
        // Then
        assertEquals(6, stats.totalHazards)
        assertEquals(2, stats.criticalCount)
        assertEquals(1, stats.highCount)
        assertEquals(3, stats.mediumCount)
        assertEquals(0, stats.lowCount)
        assertEquals(Severity.CRITICAL, stats.highestSeverity)
    }

    // Helper methods for creating test data
    private fun createTestHazards(): List<Hazard> {
        return listOf(
            createCriticalHazard(),
            createHighHazard(),
            createMediumHazard()
        )
    }

    private fun createCriticalHazard(): Hazard {
        return Hazard(
            id = "critical-hazard",
            type = HazardType.FALL_PROTECTION,
            severity = Severity.CRITICAL,
            description = "Worker at height without fall protection",
            oshaCode = "1926.501(b)(1)",
            boundingBox = BoundingBox(0.2f, 0.1f, 0.3f, 0.4f),
            confidence = 0.89f,
            recommendations = listOf("Install fall protection"),
            immediateAction = "STOP WORK"
        )
    }

    private fun createHighHazard(): Hazard {
        return Hazard(
            id = "high-hazard",
            type = HazardType.PPE_VIOLATION,
            severity = Severity.HIGH,
            description = "Missing hard hat",
            oshaCode = "1926.95(a)",
            boundingBox = BoundingBox(0.15f, 0.05f, 0.2f, 0.25f),
            confidence = 0.94f,
            recommendations = listOf("Require hard hat"),
            immediateAction = "Don hard hat"
        )
    }

    private fun createMediumHazard(): Hazard {
        return Hazard(
            id = "medium-hazard",
            type = HazardType.ELECTRICAL_HAZARD,
            severity = Severity.MEDIUM,
            description = "Exposed wiring",
            oshaCode = "1926.416(a)(1)",
            boundingBox = BoundingBox(0.6f, 0.7f, 0.2f, 0.15f),
            confidence = 0.76f,
            recommendations = listOf("Cover exposed wiring")
        )
    }

    private fun createLowHazard(): Hazard {
        return Hazard(
            id = "low-hazard",
            type = HazardType.HOUSEKEEPING,
            severity = Severity.LOW,
            description = "Debris on walkway",
            oshaCode = "1926.25(a)",
            boundingBox = BoundingBox(0.3f, 0.8f, 0.4f, 0.1f),
            confidence = 0.65f,
            recommendations = listOf("Clear debris")
        )
    }

    private fun createElectricalHazard(): Hazard {
        return createMediumHazard().copy(
            id = "electrical-hazard",
            type = HazardType.ELECTRICAL_HAZARD
        )
    }

    private fun createFallProtectionHazard(): Hazard {
        return createCriticalHazard().copy(
            id = "fall-protection-hazard",
            type = HazardType.FALL_PROTECTION
        )
    }

    private fun createPPEHazard(): Hazard {
        return createHighHazard().copy(
            id = "ppe-hazard",
            type = HazardType.PPE_VIOLATION
        )
    }
}

/**
 * Mock Hazard Overlay Manager for testing
 */
class HazardOverlayManager {
    private val _overlayState = MutableStateFlow(OverlayState())
    val overlayState = _overlayState

    private var allHazards = emptyList<Hazard>()
    private var minimumSeverity = Severity.LOW
    private var minimumConfidence = 0.5f
    private var maxVisibleHazards = 50
    private var workTypeFilter: WorkType? = null

    var onLifecycleEvent: ((OverlayLifecycleEvent) -> Unit)? = null

    fun showHazards(hazards: List<Hazard>) {
        allHazards = hazards
        updateVisibleHazards()
        onLifecycleEvent?.invoke(OverlayLifecycleEvent.SHOWN)
    }

    fun hideOverlay() {
        _overlayState.value = _overlayState.value.copy(
            isVisible = false,
            visibleHazards = emptyList()
        )
        onLifecycleEvent?.invoke(OverlayLifecycleEvent.HIDDEN)
    }

    fun setMinimumSeverity(severity: Severity) {
        minimumSeverity = severity
        updateVisibleHazards()
    }

    fun setMinimumConfidence(confidence: Float) {
        minimumConfidence = confidence
        updateVisibleHazards()
    }

    fun setMaxVisibleHazards(max: Int) {
        maxVisibleHazards = max
        updateVisibleHazards()
    }

    fun setWorkTypeFilter(workType: WorkType?) {
        workTypeFilter = workType
        updateVisibleHazards()
    }

    fun setOverlayMode(mode: OverlayMode) {
        _overlayState.value = _overlayState.value.copy(mode = mode)
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        _overlayState.value = _overlayState.value.copy(animationsEnabled = enabled)
    }

    fun selectHazard(hazardId: String) {
        _overlayState.value = _overlayState.value.copy(
            selectedHazardId = hazardId,
            isHazardSelected = true
        )
    }

    fun clearSelection() {
        _overlayState.value = _overlayState.value.copy(
            selectedHazardId = null,
            isHazardSelected = false
        )
    }

    fun updateHazard(updatedHazard: Hazard) {
        allHazards = allHazards.map { hazard ->
            if (hazard.id == updatedHazard.id) updatedHazard else hazard
        }
        updateVisibleHazards()
    }

    fun getOverlayStatistics(): OverlayStatistics {
        return OverlayStatistics(
            totalHazards = allHazards.size,
            criticalCount = allHazards.count { it.severity == Severity.CRITICAL },
            highCount = allHazards.count { it.severity == Severity.HIGH },
            mediumCount = allHazards.count { it.severity == Severity.MEDIUM },
            lowCount = allHazards.count { it.severity == Severity.LOW },
            highestSeverity = allHazards.maxByOrNull { it.severity.ordinal }?.severity ?: Severity.LOW
        )
    }

    private fun updateVisibleHazards() {
        val currentSelection = _overlayState.value.selectedHazardId
        
        var filteredHazards = allHazards
            .filter { it.severity >= minimumSeverity }
            .filter { it.confidence >= minimumConfidence }

        workTypeFilter?.let { filter ->
            filteredHazards = filteredHazards.filter { hazard ->
                when (filter) {
                    WorkType.ELECTRICAL_WORK -> hazard.type == HazardType.ELECTRICAL_HAZARD
                    WorkType.FALL_PROTECTION -> hazard.type == HazardType.FALL_PROTECTION
                    else -> true
                }
            }
        }

        // Sort by severity (critical first) then by confidence
        val sortedHazards = filteredHazards.sortedWith(
            compareByDescending<Hazard> { it.severity.ordinal }
                .thenByDescending { it.confidence }
        )

        val visibleHazards = sortedHazards.take(maxVisibleHazards)

        // Clear selection if selected hazard is no longer visible
        val newSelectedHazardId = if (currentSelection != null && 
            visibleHazards.any { it.id == currentSelection }) {
            currentSelection
        } else {
            null
        }

        _overlayState.value = _overlayState.value.copy(
            isVisible = visibleHazards.isNotEmpty(),
            visibleHazards = visibleHazards,
            selectedHazardId = newSelectedHazardId,
            isHazardSelected = newSelectedHazardId != null
        )
    }
}

// Data classes for testing
data class OverlayState(
    val isVisible: Boolean = false,
    val visibleHazards: List<Hazard> = emptyList(),
    val selectedHazardId: String? = null,
    val isHazardSelected: Boolean = false,
    val mode: OverlayMode = OverlayMode.FULL,
    val animationsEnabled: Boolean = true
)

enum class OverlayMode { FULL, COMPACT, MINIMAL }

enum class OverlayLifecycleEvent { SHOWN, HIDDEN, UPDATED }

data class OverlayStatistics(
    val totalHazards: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val highestSeverity: Severity
)
