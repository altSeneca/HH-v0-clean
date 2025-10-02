package com.hazardhawk

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.get

// Import navigation components
import com.hazardhawk.ui.gallery.PhotoGallery
// Import CameraScreen - removing old import
// import com.hazardhawk.CameraScreen
// import com.hazardhawk.ui.camera.ElegantCameraScreen  // Disabled - has dependency issues
// Import new Safety HUD Camera interface
import com.hazardhawk.ui.camera.hud.SafetyHUDCameraScreen
// Import ClearCamera interface (minimalist design)
import com.hazardhawk.ui.camera.clear.ClearCameraScreen
// Import AR Camera functionality
import com.hazardhawk.ui.camera.ARCameraPreview
import com.hazardhawk.ui.settings.SettingsScreen
import com.hazardhawk.ui.home.CompanyProjectEntryScreen
// Import PTP navigation
import com.hazardhawk.navigation.ptpNavGraph

// Import working gallery components from enhancement implementation
import java.io.File
import com.hazardhawk.data.PhotoStorageManager
import com.hazardhawk.data.PhotoStorageManagerCompat
import com.hazardhawk.domain.repositories.PhotoRepository
import com.hazardhawk.data.PhotoRepositoryCompat
import com.hazardhawk.database.HazardHawkDatabase
import com.hazardhawk.background.OSHASyncManager
import com.hazardhawk.camera.AppStateManager
import com.hazardhawk.camera.MetadataSettingsManager
import com.hazardhawk.data.ProjectManager

class MainActivity : ComponentActivity() {
    // Volume button capture trigger state
    private var volumeCaptureCallback: (() -> Unit)? = null

    // OSHA sync manager
    // TODO: Enable when Ktor HttpClient is properly configured
    // private val oshaSyncManager: OSHASyncManager by inject()
    
    // Inject dependencies using Koin
    private val photoStorageManager: PhotoStorageManager by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Initialize OSHA sync on app startup
        // TODO: Enable when Ktor HttpClient is properly configured
        // oshaSyncManager.initializeSync()

        Log.d("HazardHawk", "MainActivity onCreate - DI dependencies available")
        Log.d("HazardHawk", "PhotoStorageManager injected: ${photoStorageManager::class.simpleName}")
        Log.d("HazardHawk", "OSHA sync initialized")

        setContent {
            HazardHawkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HazardHawkNavigation(
                        photoStorageManager = photoStorageManager
                    ) { callback ->
                        volumeCaptureCallback = callback
                    }
                }
            }
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP -> {
                // Trigger camera capture when volume buttons are pressed
                if (event?.repeatCount == 0) { // Only on initial press, not repeated
                    volumeCaptureCallback?.invoke()
                    Log.d("HazardHawk", "Volume button camera capture triggered")
                }
                true // Consume the event to prevent volume change
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}

@Composable
fun HazardHawkNavigation(
    photoStorageManager: PhotoStorageManager,
    onSetVolumeCaptureCallback: ((callback: (() -> Unit)?) -> Unit)
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Setup state managers
    val projectManager = remember { ProjectManager(context) }
    val metadataSettingsManager = remember { MetadataSettingsManager(context, projectManager) }
    val appStateManager = remember { AppStateManager(context, metadataSettingsManager) }

    // Collect app settings
    val appSettings by metadataSettingsManager.appSettings.collectAsStateWithLifecycle()
    val userProfile by metadataSettingsManager.userProfile.collectAsStateWithLifecycle()
    val currentProject by metadataSettingsManager.currentProject.collectAsStateWithLifecycle()

    // Calculate dynamic start destination
    // ClearCamera is now the default camera interface (minimalist design)
    // Users can switch to SafetyHUD camera via Settings if they prefer the HUD interface
    val startDestination = remember(appSettings, userProfile, currentProject) {
        val hasData = userProfile.company.isNotBlank() && currentProject.projectName.isNotBlank()
        val showOnLaunch = appSettings.startup.showCompanyProjectOnLaunch
        val firstToday = appStateManager.isFirstLaunchToday()

        // ClearCamera is the default - provides minimalist, distraction-free capture
        when {
            !hasData -> "company_project_entry"  // First time setup
            !showOnLaunch -> "clear_camera"      // Setting disabled - use ClearCamera (default)
            firstToday -> "company_project_entry" // First launch today
            else -> "clear_camera"               // Subsequent launches - ClearCamera default
        }.also {
            Log.d("HazardHawk", "Navigation decision: hasData=$hasData, showOnLaunch=$showOnLaunch, firstToday=$firstToday -> $it")
        }
    }

    // TODO: Replace with proper dependency injection (Koin)
    val mockDatabase: HazardHawkDatabase? = null // This will be properly injected
    val photoRepository: PhotoRepository = PhotoRepositoryCompat(context)

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Company/Project entry screen - new starting point
        composable("company_project_entry") {
            CompanyProjectEntryScreen(
                onNavigateToCamera = { company, project ->
                    Log.d("HazardHawk", "Navigating to camera with Company: $company, Project: $project")
                    // Update last launch date to prevent showing this screen again today
                    appStateManager.updateLastLaunchDate()
                    navController.navigate("camera") {
                        popUpTo("company_project_entry") { inclusive = true }
                    }
                }
            )
        }
        
        // Home screen disabled - app launches directly to camera
        // Keeping route for potential future use
        composable("home") {
            // Redirect to camera if somehow accessed
            LaunchedEffect(Unit) {
                navController.navigate("camera") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }
        
        // Main camera route - uses ClearCamera (minimalist design)
        composable("camera") {
            Log.d("HazardHawk", "Showing Clear camera screen (minimalist design)")
            ClearCameraScreen(
                onNavigateToGallery = {
                    navController.navigate("gallery")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAR = {
                    navController.navigate("ar_camera")
                },
                onSetVolumeCaptureCallback = onSetVolumeCaptureCallback
            )
        }

        // ClearCamera interface - Minimalist design (Jony Ive-inspired)
        composable("clear_camera") {
            Log.d("HazardHawk", "Showing Clear camera screen (minimalist design)")
            ClearCameraScreen(
                onNavigateToGallery = {
                    navController.navigate("gallery")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onNavigateToAR = {
                    navController.navigate("ar_camera")
                },
                onSetVolumeCaptureCallback = onSetVolumeCaptureCallback
            )
        }

        // AR Camera interface - Real-time hazard detection with AR overlays
        composable("ar_camera") {
            Log.d("HazardHawk", "Showing AR camera screen")
            ARCameraPreview(
                onFrameAnalyzed = { frameData ->
                    Log.d("HazardHawk", "AR frame analyzed: ${frameData.size} bytes")
                },
                onError = { error ->
                    Log.e("HazardHawk", "AR error: $error")
                },
                onNavigateBack = {
                    Log.d("HazardHawk", "Navigating back from AR camera to main camera")
                    navController.navigate("camera") {
                        popUpTo("ar_camera") { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        
        // Standard gallery interface
        composable("gallery") {
            val reportGenerationManager = remember { com.hazardhawk.reports.ReportGenerationManager(context) }
            PhotoGallery(
                photoRepository = photoRepository,
                reportGenerationManager = reportGenerationManager,
                onNavigateToCamera = {
                    // Navigate to ClearCamera (now the default)
                    navController.navigate("clear_camera")
                },
                onBack = {
                    // Navigate back to ClearCamera instead of SafetyHUD
                    navController.navigate("clear_camera") {
                        popUpTo("clear_camera") { inclusive = false }
                    }
                }
            )
        }
        
        
        // Settings Screen - Unified Settings (No Glass Effects, Emergency Mode, or High Contrast)
        composable("settings") {
            com.hazardhawk.ui.settings.UnifiedSettingsScreen(
                onNavigateBack = {
                    // Navigate back to Clear Camera (default camera screen)
                    navController.navigate("clear_camera") {
                        popUpTo("clear_camera") { inclusive = false }
                    }
                },
                onNavigateToPTP = {
                    // Navigate to PTP list screen
                    navController.navigate("ptp/list")
                }
            )
        }

        // PTP Feature Navigation Graph
        ptpNavGraph(navController)
    }
}

// Mock database creation - this will be replaced with proper database initialization
private fun createMockDatabase(): HazardHawkDatabase? {
    // TODO: Create actual database instance with SQLDelight driver
    // For now, return null to use the safe null handling in PhotoRepositoryImpl
    return null
}

@Composable
fun HazardHawkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = MD_THEME_LIGHT_PRIMARY,
            onPrimary = MD_THEME_LIGHT_ON_PRIMARY,
            secondary = MD_THEME_LIGHT_SECONDARY,
            onSecondary = MD_THEME_LIGHT_ON_SECONDARY,
            background = MD_THEME_LIGHT_BACKGROUND,
            onBackground = MD_THEME_LIGHT_ON_BACKGROUND,
            surface = MD_THEME_LIGHT_SURFACE,
            onSurface = MD_THEME_LIGHT_ON_SURFACE
        ),
        content = content
    )
}

// Safety-focused color scheme
private val MD_THEME_LIGHT_PRIMARY = androidx.compose.ui.graphics.Color(0xFFFF6B35) // Safety Orange
private val MD_THEME_LIGHT_ON_PRIMARY = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val MD_THEME_LIGHT_SECONDARY = androidx.compose.ui.graphics.Color(0xFFFFA500) // Orange
private val MD_THEME_LIGHT_ON_SECONDARY = androidx.compose.ui.graphics.Color(0xFF000000)
private val MD_THEME_LIGHT_BACKGROUND = androidx.compose.ui.graphics.Color(0xFFF5F5F5)
private val MD_THEME_LIGHT_ON_BACKGROUND = androidx.compose.ui.graphics.Color(0xFF1C1B1F)
private val MD_THEME_LIGHT_SURFACE = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val MD_THEME_LIGHT_ON_SURFACE = androidx.compose.ui.graphics.Color(0xFF1C1B1F)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    HazardHawkTheme {
        // Preview disabled since home screen is removed
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "HazardHawk Camera App",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
