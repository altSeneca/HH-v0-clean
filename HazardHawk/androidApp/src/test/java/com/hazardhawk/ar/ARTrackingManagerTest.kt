package com.hazardhawk.ar

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * Unit tests for AR Tracking Manager focusing on tracking state and pose accuracy.
 * Tests camera pose tracking, coordinate system transformations, and tracking quality metrics.
 */
@RunWith(AndroidJUnit4::class)
class ARTrackingManagerTest {

    private lateinit var trackingManager: ARTrackingManager

    @Before
    fun setUp() {
        trackingManager = ARTrackingManager()
    }

    @Test
    fun arTrackingManager_initializesWithNotTrackingState() = runTest {
        // Given
        val manager = ARTrackingManager()
        
        // When
        val state = manager.trackingState.first()
        
        // Then
        assertEquals(TrackingState.NOT_TRACKING, state.trackingState)
        assertFalse(state.isTrackingStable)
        assertEquals(0f, state.trackingQuality)
        assertNull(state.currentPose)
    }

    @Test
    fun arTrackingManager_startsTrackingSuccessfully() = runTest {
        // Given
        val trackingConfig = ARTrackingConfig(
            enablePlaneDetection = true,
            enableLightEstimation = true,
            trackingMode = TrackingMode.WORLD_TRACKING
        )
        
        // When
        val result = trackingManager.startTracking(trackingConfig)
        
        // Then
        assertTrue(result.isSuccess)
        val state = trackingManager.trackingState.first()
        assertTrue(state.trackingState != TrackingState.NOT_TRACKING)
    }

    @Test
    fun arTrackingManager_handlesTrackingStartFailure() = runTest {
        // Given
        val invalidConfig = ARTrackingConfig(
            trackingMode = TrackingMode.INVALID_MODE
        )
        
        // When
        val result = trackingManager.startTracking(invalidConfig)
        
        // Then
        assertTrue(result.isFailure)
        assertEquals("Invalid tracking mode", result.exceptionOrNull()?.message)
        
        val state = trackingManager.trackingState.first()
        assertEquals(TrackingState.NOT_TRACKING, state.trackingState)
    }

    @Test
    fun arTrackingManager_transitionsToTrackingState() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        
        // When
        trackingManager.updateTrackingState(TrackingState.TRACKING)
        
        // Then
        val state = trackingManager.trackingState.first()
        assertEquals(TrackingState.TRACKING, state.trackingState)
        assertTrue(state.isTrackingStable)
    }

    @Test
    fun arTrackingManager_handlesTrackingLoss() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        trackingManager.updateTrackingState(TrackingState.TRACKING)
        assertTrue(trackingManager.trackingState.first().isTrackingStable)
        
        // When
        trackingManager.updateTrackingState(TrackingState.LIMITED_TRACKING)
        
        // Then
        val state = trackingManager.trackingState.first()
        assertEquals(TrackingState.LIMITED_TRACKING, state.trackingState)
        assertFalse(state.isTrackingStable)
        assertTrue(state.trackingQuality < 0.7f)
    }

    @Test
    fun arTrackingManager_updatesPoseAccurately() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        val testPose = Pose(
            position = Vector3(1.0f, 2.0f, 3.0f),
            rotation = Quaternion(0.0f, 0.0f, 0.0f, 1.0f)
        )
        
        // When
        trackingManager.updatePose(testPose, 0.95f)
        
        // Then
        val state = trackingManager.trackingState.first()
        assertEquals(testPose, state.currentPose)
        assertEquals(0.95f, state.poseAccuracy)
    }

    @Test
    fun arTrackingManager_transformsWorldToScreenCoordinates() {
        // Given
        val worldPosition = Vector3(0.5f, 1.0f, -2.0f)
        val cameraIntrinsics = CameraIntrinsics(
            focalLength = Vector2(800f, 800f),
            principalPoint = Vector2(320f, 240f),
            imageSize = Vector2(640f, 480f)
        )
        val cameraPose = Pose(
            position = Vector3(0f, 0f, 0f),
            rotation = Quaternion(0f, 0f, 0f, 1f)
        )
        
        // When
        val screenPoint = trackingManager.worldToScreen(
            worldPosition, cameraPose, cameraIntrinsics
        )
        
        // Then
        assertNotNull(screenPoint)
        assertTrue(screenPoint.x >= 0f && screenPoint.x <= 640f)
        assertTrue(screenPoint.y >= 0f && screenPoint.y <= 480f)
    }

    @Test
    fun arTrackingManager_handlesWorldToScreenBehindCamera() {
        // Given
        val worldPositionBehind = Vector3(0f, 0f, 1f) // Behind camera (positive Z)
        val cameraIntrinsics = CameraIntrinsics(
            focalLength = Vector2(800f, 800f),
            principalPoint = Vector2(320f, 240f),
            imageSize = Vector2(640f, 480f)
        )
        val cameraPose = Pose(
            position = Vector3(0f, 0f, 0f),
            rotation = Quaternion(0f, 0f, 0f, 1f)
        )
        
        // When
        val screenPoint = trackingManager.worldToScreen(
            worldPositionBehind, cameraPose, cameraIntrinsics
        )
        
        // Then
        assertNull(screenPoint) // Should return null for points behind camera
    }

    @Test
    fun arTrackingManager_calculatesTrackingQuality() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        
        // When - Simulate good tracking conditions
        trackingManager.updateTrackingMetrics(
            featurePoints = 150,
            trackingConfidence = 0.9f,
            lightingQuality = 0.8f,
            motionStability = 0.95f
        )
        
        // Then
        val state = trackingManager.trackingState.first()
        assertTrue(state.trackingQuality > 0.8f)
        assertEquals(TrackingQuality.EXCELLENT, state.qualityLevel)
    }

    @Test
    fun arTrackingManager_detectsPoorTrackingConditions() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        
        // When - Simulate poor tracking conditions
        trackingManager.updateTrackingMetrics(
            featurePoints = 20,      // Low feature count
            trackingConfidence = 0.3f, // Low confidence
            lightingQuality = 0.2f,   // Poor lighting
            motionStability = 0.4f    // Unstable motion
        )
        
        // Then
        val state = trackingManager.trackingState.first()
        assertTrue(state.trackingQuality < 0.5f)
        assertEquals(TrackingQuality.POOR, state.qualityLevel)
        assertFalse(state.isTrackingReliable)
    }

    @Test
    fun arTrackingManager_handlesRapidMotion() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        trackingManager.updateTrackingState(TrackingState.TRACKING)
        
        // When - Simulate rapid camera motion
        val poses = listOf(
            Pose(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1f)),
            Pose(Vector3(2f, 1f, -1f), Quaternion(0.1f, 0.2f, 0.1f, 0.97f)),
            Pose(Vector3(5f, 3f, -3f), Quaternion(0.3f, 0.4f, 0.2f, 0.85f))
        )
        
        poses.forEach { pose ->
            trackingManager.updatePose(pose, 0.8f)
        }
        
        // Then
        val state = trackingManager.trackingState.first()
        assertTrue(state.motionDetected)
        assertEquals(MotionType.RAPID, state.motionType)
    }

    @Test
    fun arTrackingManager_detectsStaticCamera() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        val staticPose = Pose(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1f))
        
        // When - Multiple updates with same pose
        repeat(10) {
            trackingManager.updatePose(staticPose, 0.95f)
        }
        
        // Then
        val state = trackingManager.trackingState.first()
        assertFalse(state.motionDetected)
        assertEquals(MotionType.STATIC, state.motionType)
    }

    @Test
    fun arTrackingManager_recoversFromTrackingLoss() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        trackingManager.updateTrackingState(TrackingState.NOT_TRACKING)
        
        // When - Simulate tracking recovery
        trackingManager.updateTrackingState(TrackingState.TRACKING)
        trackingManager.updatePose(
            Pose(Vector3(0f, 0f, 0f), Quaternion(0f, 0f, 0f, 1f)), 
            0.9f
        )
        
        // Then
        val state = trackingManager.trackingState.first()
        assertEquals(TrackingState.TRACKING, state.trackingState)
        assertTrue(state.isTrackingStable)
        assertTrue(state.trackingRecovered)
    }

    @Test
    fun arTrackingManager_calculatesRelativePose() {
        // Given
        val referencePose = Pose(
            Vector3(1f, 0f, 0f),
            Quaternion(0f, 0f, 0f, 1f)
        )
        val currentPose = Pose(
            Vector3(2f, 1f, -1f),
            Quaternion(0f, 0.1f, 0f, 0.995f)
        )
        
        // When
        val relativePose = trackingManager.calculateRelativePose(currentPose, referencePose)
        
        // Then
        assertNotNull(relativePose)
        
        // Verify position difference
        val expectedPositionDiff = Vector3(1f, 1f, -1f)
        assertEquals(expectedPositionDiff.x, relativePose.position.x, 0.01f)
        assertEquals(expectedPositionDiff.y, relativePose.position.y, 0.01f)
        assertEquals(expectedPositionDiff.z, relativePose.position.z, 0.01f)
    }

    @Test
    fun arTrackingManager_tracksPerformanceMetrics() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        
        // When
        repeat(100) { frame ->
            trackingManager.recordFrameProcessingTime(16.67f) // 60 FPS target
            trackingManager.recordTrackingLatency(8.33f)      // Half frame latency
        }
        
        // Then
        val metrics = trackingManager.getPerformanceMetrics()
        assertEquals(100, metrics.totalFramesProcessed)
        assertEquals(16.67f, metrics.averageFrameTime, 0.1f)
        assertEquals(8.33f, metrics.averageTrackingLatency, 0.1f)
        assertTrue(metrics.isPerformanceGood)
    }

    @Test
    fun arTrackingManager_stopsTrackingCleanly() = runTest {
        // Given
        trackingManager.startTracking(ARTrackingConfig())
        trackingManager.updateTrackingState(TrackingState.TRACKING)
        
        // When
        trackingManager.stopTracking()
        
        // Then
        val state = trackingManager.trackingState.first()
        assertEquals(TrackingState.NOT_TRACKING, state.trackingState)
        assertFalse(state.isTrackingStable)
        assertNull(state.currentPose)
        assertEquals(0f, state.trackingQuality)
    }

    @Test
    fun arTrackingManager_handlesMultipleStartStopCycles() = runTest {
        // Given & When
        repeat(5) {
            val result = trackingManager.startTracking(ARTrackingConfig())
            assertTrue(result.isSuccess)
            
            trackingManager.updateTrackingState(TrackingState.TRACKING)
            assertTrue(trackingManager.trackingState.first().isTrackingStable)
            
            trackingManager.stopTracking()
            assertFalse(trackingManager.trackingState.first().isTrackingStable)
        }
        
        // Then - Should handle multiple cycles without issues
        val finalState = trackingManager.trackingState.first()
        assertEquals(TrackingState.NOT_TRACKING, finalState.trackingState)
    }
}

/**
 * Mock AR Tracking Manager for testing
 */
class ARTrackingManager {
    private val _trackingState = MutableStateFlow(ARTrackingState())
    val trackingState = _trackingState

    private var isTracking = false
    private var currentConfig: ARTrackingConfig? = null
    private var performanceMetrics = TrackingPerformanceMetrics()
    private var lastPose: Pose? = null
    private var poseHistory = mutableListOf<Pose>()

    suspend fun startTracking(config: ARTrackingConfig): Result<Unit> {
        return try {
            if (config.trackingMode == TrackingMode.INVALID_MODE) {
                throw IllegalArgumentException("Invalid tracking mode")
            }
            
            currentConfig = config
            isTracking = true
            
            _trackingState.value = _trackingState.value.copy(
                trackingState = TrackingState.LIMITED_TRACKING,
                isTrackingStable = false,
                trackingQuality = 0.3f
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopTracking() {
        isTracking = false
        currentConfig = null
        lastPose = null
        poseHistory.clear()
        performanceMetrics = TrackingPerformanceMetrics()
        
        _trackingState.value = ARTrackingState()
    }

    fun updateTrackingState(state: TrackingState) {
        val isStable = state == TrackingState.TRACKING
        val quality = when (state) {
            TrackingState.TRACKING -> 0.9f
            TrackingState.LIMITED_TRACKING -> 0.5f
            TrackingState.NOT_TRACKING -> 0.0f
        }
        
        val wasNotTracking = _trackingState.value.trackingState == TrackingState.NOT_TRACKING
        val trackingRecovered = wasNotTracking && state == TrackingState.TRACKING
        
        _trackingState.value = _trackingState.value.copy(
            trackingState = state,
            isTrackingStable = isStable,
            trackingQuality = quality,
            trackingRecovered = trackingRecovered
        )
    }

    fun updatePose(pose: Pose, accuracy: Float) {
        lastPose = pose
        poseHistory.add(pose)
        if (poseHistory.size > 10) {
            poseHistory.removeAt(0)
        }
        
        val motionType = detectMotionType()
        
        _trackingState.value = _trackingState.value.copy(
            currentPose = pose,
            poseAccuracy = accuracy,
            motionDetected = motionType != MotionType.STATIC,
            motionType = motionType
        )
    }

    private fun detectMotionType(): MotionType {
        if (poseHistory.size < 3) return MotionType.STATIC
        
        val recentPoses = poseHistory.takeLast(3)
        var totalDistance = 0f
        
        for (i in 1 until recentPoses.size) {
            val prev = recentPoses[i - 1]
            val curr = recentPoses[i]
            totalDistance += calculateDistance(prev.position, curr.position)
        }
        
        return when {
            totalDistance > 1.0f -> MotionType.RAPID
            totalDistance > 0.1f -> MotionType.MODERATE
            else -> MotionType.STATIC
        }
    }

    private fun calculateDistance(pos1: Vector3, pos2: Vector3): Float {
        val dx = pos1.x - pos2.x
        val dy = pos1.y - pos2.y
        val dz = pos1.z - pos2.z
        return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun worldToScreen(
        worldPosition: Vector3,
        cameraPose: Pose,
        cameraIntrinsics: CameraIntrinsics
    ): Vector2? {
        // Transform world position to camera space
        val cameraSpacePos = transformToCameraSpace(worldPosition, cameraPose)
        
        // Check if point is behind camera
        if (cameraSpacePos.z > 0) return null
        
        // Project to screen coordinates
        val x = (cameraSpacePos.x / -cameraSpacePos.z) * cameraIntrinsics.focalLength.x + cameraIntrinsics.principalPoint.x
        val y = (cameraSpacePos.y / -cameraSpacePos.z) * cameraIntrinsics.focalLength.y + cameraIntrinsics.principalPoint.y
        
        // Check if within screen bounds
        if (x < 0 || x > cameraIntrinsics.imageSize.x || y < 0 || y > cameraIntrinsics.imageSize.y) {
            return null
        }
        
        return Vector2(x, y)
    }

    private fun transformToCameraSpace(worldPosition: Vector3, cameraPose: Pose): Vector3 {
        // Simplified transformation - in reality would use full transformation matrix
        val relative = Vector3(
            worldPosition.x - cameraPose.position.x,
            worldPosition.y - cameraPose.position.y,
            worldPosition.z - cameraPose.position.z
        )
        return relative
    }

    fun updateTrackingMetrics(
        featurePoints: Int,
        trackingConfidence: Float,
        lightingQuality: Float,
        motionStability: Float
    ) {
        val quality = (trackingConfidence + lightingQuality + motionStability) / 3f
        val qualityLevel = when {
            quality > 0.8f -> TrackingQuality.EXCELLENT
            quality > 0.6f -> TrackingQuality.GOOD
            quality > 0.4f -> TrackingQuality.FAIR
            else -> TrackingQuality.POOR
        }
        
        _trackingState.value = _trackingState.value.copy(
            trackingQuality = quality,
            qualityLevel = qualityLevel,
            isTrackingReliable = quality > 0.6f,
            featurePointCount = featurePoints
        )
    }

    fun calculateRelativePose(currentPose: Pose, referencePose: Pose): Pose {
        val relativePosition = Vector3(
            currentPose.position.x - referencePose.position.x,
            currentPose.position.y - referencePose.position.y,
            currentPose.position.z - referencePose.position.z
        )
        
        // Simplified relative rotation calculation
        val relativeRotation = currentPose.rotation
        
        return Pose(relativePosition, relativeRotation)
    }

    fun recordFrameProcessingTime(frameTime: Float) {
        performanceMetrics = performanceMetrics.copy(
            totalFramesProcessed = performanceMetrics.totalFramesProcessed + 1,
            frameTimes = performanceMetrics.frameTimes + frameTime
        )
    }

    fun recordTrackingLatency(latency: Float) {
        performanceMetrics = performanceMetrics.copy(
            trackingLatencies = performanceMetrics.trackingLatencies + latency
        )
    }

    fun getPerformanceMetrics(): TrackingPerformanceMetrics {
        return performanceMetrics.copy(
            averageFrameTime = if (performanceMetrics.frameTimes.isNotEmpty()) {
                performanceMetrics.frameTimes.average().toFloat()
            } else 0f,
            averageTrackingLatency = if (performanceMetrics.trackingLatencies.isNotEmpty()) {
                performanceMetrics.trackingLatencies.average().toFloat()
            } else 0f,
            isPerformanceGood = performanceMetrics.frameTimes.isNotEmpty() && 
                performanceMetrics.frameTimes.average() < 20f // Better than 50 FPS
        )
    }
}

// Data classes for testing
data class ARTrackingState(
    val trackingState: TrackingState = TrackingState.NOT_TRACKING,
    val isTrackingStable: Boolean = false,
    val trackingQuality: Float = 0f,
    val qualityLevel: TrackingQuality = TrackingQuality.POOR,
    val isTrackingReliable: Boolean = false,
    val currentPose: Pose? = null,
    val poseAccuracy: Float = 0f,
    val motionDetected: Boolean = false,
    val motionType: MotionType = MotionType.STATIC,
    val trackingRecovered: Boolean = false,
    val featurePointCount: Int = 0
)

data class ARTrackingConfig(
    val enablePlaneDetection: Boolean = false,
    val enableLightEstimation: Boolean = false,
    val trackingMode: TrackingMode = TrackingMode.WORLD_TRACKING
)

data class Pose(
    val position: Vector3,
    val rotation: Quaternion
)

data class Vector3(val x: Float, val y: Float, val z: Float)
data class Vector2(val x: Float, val y: Float)
data class Quaternion(val x: Float, val y: Float, val z: Float, val w: Float)

data class CameraIntrinsics(
    val focalLength: Vector2,
    val principalPoint: Vector2,
    val imageSize: Vector2
)

data class TrackingPerformanceMetrics(
    val totalFramesProcessed: Int = 0,
    val frameTimes: List<Float> = emptyList(),
    val trackingLatencies: List<Float> = emptyList(),
    val averageFrameTime: Float = 0f,
    val averageTrackingLatency: Float = 0f,
    val isPerformanceGood: Boolean = false
)

enum class TrackingState { NOT_TRACKING, LIMITED_TRACKING, TRACKING }
enum class TrackingMode { WORLD_TRACKING, FACE_TRACKING, INVALID_MODE }
enum class TrackingQuality { POOR, FAIR, GOOD, EXCELLENT }
enum class MotionType { STATIC, MODERATE, RAPID }
