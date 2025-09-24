package com.hazardhawk.ui.camera

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazardhawk.ar.ARCameraController
import com.hazardhawk.ar.ARCapabilityChecker
import com.hazardhawk.ar.ARSessionState
import com.hazardhawk.ar.ARTrackingState
import com.hazardhawk.ar.privacy.ARPrivacyManager
import com.hazardhawk.ar.privacy.ARPrivacyConsentDialog
import com.hazardhawk.ar.privacy.PrivacyProtectionLevel
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.data.repositories.UISettingsRepository
import org.koin.compose.koinInject
import kotlinx.coroutines.launch


/**
 * AR-enabled camera preview that integrates ARCore with CameraX.
 * Provides real-time hazard detection with AR overlays.
 */
@Composable
fun ARCameraPreview(
    workType: WorkType = WorkType.GENERAL_CONSTRUCTION,
    onFrameAnalyzed: (ByteArray) -> Unit = {},
    onError: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Dependency injection
    val uiSettingsRepository: UISettingsRepository = koinInject()

    // AR controller and state
    val arController = remember { ARCameraController(context) }
    val sessionState by arController.sessionState.collectAsStateWithLifecycle()
    val trackingState by arController.trackingState.collectAsStateWithLifecycle()

    // Privacy manager
    val privacyManager = remember { ARPrivacyManager(context, uiSettingsRepository) }
    val privacySettings by privacyManager.privacyProtectionFlow().collectAsStateWithLifecycle(
        initialValue = com.hazardhawk.ar.privacy.ARPrivacySettings()
    )

    // Camera controller
    val cameraController = remember { LifecycleCameraController(context) }


    // AR capabilities and consent state
    var arCapabilities by remember { mutableStateOf<com.hazardhawk.ar.ARCapabilities?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var hasConsent by remember { mutableStateOf(false) }

    // Check consent first, then initialize AR capabilities
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            // Check if user has given consent for AR
            val consentGiven = privacyManager.hasARConsent()
            hasConsent = consentGiven

            if (!consentGiven) {
                showConsentDialog = true
                return@launch
            }

            try {
                val capabilities = ARCapabilityChecker.getARCapabilities(context)
                arCapabilities = capabilities

                if (capabilities.isSupported) {
                    // Initialize AR session with privacy protection
                    val success = arController.initializeSession(context)
                    if (success) {
                        // Bind AR controller to camera
                        arController.bindToCamera(cameraController)
                        cameraController.bindToLifecycle(lifecycleOwner)
                        isInitialized = true
                    } else {
                        onError("Failed to initialize AR session")
                    }
                } else {
                    onError("AR not supported: ${capabilities.statusDescription}")
                }
            } catch (e: Exception) {
                onError("AR initialization error: ${e.message}")
            }
        }
    }

    // Start AR session when ready
    LaunchedEffect(sessionState, isInitialized) {
        if (isInitialized && sessionState == ARSessionState.Ready) {
            arController.startSession()
        }
    }

    // Lifecycle management
    LaunchedEffect(lifecycleOwner) {
        // Handle lifecycle events
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (arCapabilities?.isSupported == true) {
            // AR Camera Preview
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        this.controller = cameraController
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                        // TODO: Add real-time frame analysis when AI integration is ready
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // TODO: Add HazardDetectionOverlay when AI integration is ready

            // Back Navigation Button
            FloatingActionButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
                containerColor = Color.Black.copy(alpha = 0.7f),
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Camera"
                )
            }

            // AR Status Overlay (moved to top-right to avoid back button)
            ARStatusOverlay(
                sessionState = sessionState,
                trackingState = trackingState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            )

            // AR Controls (simplified and moved to bottom-left)
            ARControlsOverlay(
                onStartSession = { arController.startSession() },
                onPauseSession = { arController.pauseSession() },
                onStopSession = { arController.stopSession() },
                isSessionRunning = sessionState == ARSessionState.Running,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            )

        } else {
            // Fallback UI when AR is not supported
            ARNotSupportedScreen(
                capabilities = arCapabilities,
                onRetry = {
                    coroutineScope.launch {
                        val capabilities = ARCapabilityChecker.getARCapabilities(context)
                        arCapabilities = capabilities
                    }
                },
                onNavigateBack = onNavigateBack,
                modifier = Modifier.fillMaxSize()
            )
        }

        // AR Privacy Consent Dialog
        ARPrivacyConsentDialog(
            isVisible = showConsentDialog,
            onConsentGiven = {
                hasConsent = true
                showConsentDialog = false
                // Re-trigger AR initialization after consent
                coroutineScope.launch {
                    try {
                        val capabilities = ARCapabilityChecker.getARCapabilities(context)
                        arCapabilities = capabilities

                        if (capabilities.isSupported) {
                            val success = arController.initializeSession(context)
                            if (success) {
                                arController.bindToCamera(cameraController)
                                cameraController.bindToLifecycle(lifecycleOwner)
                                isInitialized = true
                            } else {
                                onError("Failed to initialize AR session")
                            }
                        } else {
                            onError("AR not supported: ${capabilities.statusDescription}")
                        }
                    } catch (e: Exception) {
                        onError("AR initialization error: ${e.message}")
                    }
                }
            },
            onConsentDenied = {
                hasConsent = false
                showConsentDialog = false
                onError("AR features require consent for privacy protection")
            },
            onDismiss = {
                // Don't allow dismissing without making a choice
            }
        )
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            arController.stopSession()
        }
    }
}

/**
 * AR status overlay showing session and tracking state.
 */
@Composable
private fun ARStatusOverlay(
    sessionState: ARSessionState,
    trackingState: ARTrackingState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "AR: ${sessionState.javaClass.simpleName}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Tracking: ${trackingState.javaClass.simpleName}",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * AR controls overlay for session management.
 */
@Composable
private fun ARControlsOverlay(
    onStartSession: () -> Unit,
    onPauseSession: () -> Unit,
    onStopSession: () -> Unit,
    isSessionRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isSessionRunning) {
            Button(
                onClick = onPauseSession,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFA500)
                )
            ) {
                Text("Pause AR")
            }
        } else {
            Button(
                onClick = onStartSession,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Green
                )
            ) {
                Text("Start AR")
            }
        }

        Button(
            onClick = onStopSession,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red
            )
        ) {
            Text("Stop AR")
        }
    }
}

/**
 * Screen shown when AR is not supported.
 */
@Composable
private fun ARNotSupportedScreen(
    capabilities: com.hazardhawk.ar.ARCapabilities?,
    onRetry: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Back Navigation Button
        FloatingActionButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            containerColor = Color.Black.copy(alpha = 0.7f),
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back to Camera"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AR Not Available",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = capabilities?.statusDescription ?: "Checking AR capabilities...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue
                )
            ) {
                Text("Retry")
            }
        }
    }
}