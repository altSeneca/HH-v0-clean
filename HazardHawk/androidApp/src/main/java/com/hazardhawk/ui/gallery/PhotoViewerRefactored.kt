package com.hazardhawk.ui.gallery

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.ImageLoader
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.util.Log
import java.io.File

// Refactored components
import com.hazardhawk.ui.gallery.components.*
import com.hazardhawk.ui.gallery.state.*
import com.hazardhawk.ui.gallery.PhotoNavigationState
import org.koin.androidx.compose.koinViewModel

// Domain and business logic
import com.hazardhawk.domain.entities.Photo
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.domain.entities.SafetyAnalysis
import com.hazardhawk.domain.repositories.AnalysisRepository
import com.hazardhawk.ai.GeminiVisionAnalyzer
import com.hazardhawk.ai.PhotoAnalysisWithTags
import com.hazardhawk.models.OSHAAnalysisResult
import com.hazardhawk.data.repositories.OSHARegulationRepository
import com.hazardhawk.performance.PhotoViewerPerformanceTracker
import com.hazardhawk.performance.ConstructionPhotoMemoryManager
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// Construction-safe colors optimized for outdoor visibility
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)
private val ConstructionBlack = Color(0xFF1A1A1A)

/**
 * Refactored PhotoViewer with Clean Architecture
 * 
 * Key improvements:
 * - Extracted reusable components
 * - Simplified state management with reducer pattern
 * - Clear separation of concerns
 * - Performance optimizations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoViewerScreenRefactored(
    photos: List<Photo>,
    initialPhotoIndex: Int,
    onNavigateBack: () -> Unit,
    onShare: (Photo) -> Unit,
    onDelete: (Photo) -> Unit,
    onTagsUpdated: (String, List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dependencies
    val performanceTracker: PhotoViewerPerformanceTracker = koinInject()
    val memoryManager: ConstructionPhotoMemoryManager = koinInject()
    val imageLoader: ImageLoader = koinInject()
    val analysisRepository: AnalysisRepository = koinInject()

    // Analysis workflow ViewModel
    val analysisViewModel: AnalysisWorkflowViewModel = koinViewModel()
    val safetyAnalysisState by analysisViewModel.state.collectAsStateWithLifecycle()
    val analysisProgress by analysisViewModel.analysisProgress.collectAsStateWithLifecycle()

    // UI state
    var currentIndex by remember { mutableIntStateOf(initialPhotoIndex.coerceIn(0, photos.size - 1)) }
    var isUiVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val dispatch: (SafetyAnalysisAction) -> Unit = { action ->
        analysisViewModel.dispatch(action)
    }
    
    val currentPhoto = photos.getOrNull(currentIndex)
    val hapticFeedback = LocalHapticFeedback.current
    
    // Auto-fade UI after 3 seconds of inactivity
    LaunchedEffect(lastInteractionTime, isUiVisible) {
        if (isUiVisible) {
            delay(3000)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastInteractionTime >= 3000) {
                isUiVisible = false
            }
        }
    }
    
    // Initialize analysis when photo changes
    LaunchedEffect(currentPhoto?.id) {
        currentPhoto?.let { photo ->
            analysisViewModel.initializeAnalysis(photo)
        }
    }
    
    // Performance monitoring
    val launchTracker = remember { performanceTracker.trackPhotoLoad("refactored_load") }
    LaunchedEffect(Unit) {
        delay(100)
        launchTracker.complete()
    }
    
    if (currentPhoto == null) {
        ErrorFallback(onNavigateBack = onNavigateBack)
        return
    }
    
    val bottomSheetState = rememberStandardBottomSheetState()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            PhotoInfoBottomSheet(
                photo = currentPhoto,
                navigationState = remember(currentIndex, photos.size) {
                    PhotoNavigationState(
                        currentIndex = currentIndex,
                        totalPhotos = photos.size,
                        canNavigatePrevious = currentIndex > 0,
                        canNavigateNext = currentIndex < photos.size - 1
                    )
                },
                safetyAnalysisState = safetyAnalysisState,
                onAnalysisAction = dispatch,
                onTagsUpdated = onTagsUpdated,
                analysisProgress = analysisProgress,
                performanceTracker = performanceTracker,
                modifier = Modifier.fillMaxSize()
            )
        },
        sheetPeekHeight = 120.dp,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ConstructionBlack)
        ) {
            // Main photo viewer
            PhotoViewerContent(
                photo = currentPhoto,
                analysisResult = safetyAnalysisState.aiAnalysis,
                showBoundingBoxes = safetyAnalysisState.showBoundingBoxes,
                onPhotoTap = {
                    isUiVisible = !isUiVisible
                    lastInteractionTime = System.currentTimeMillis()
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Top controls with navigation
            PhotoViewerTopBar(
                currentPhoto = currentPhoto,
                navigationState = remember(currentIndex, photos.size) {
                    PhotoNavigationState(
                        currentIndex = currentIndex,
                        totalPhotos = photos.size,
                        canNavigatePrevious = currentIndex > 0,
                        canNavigateNext = currentIndex < photos.size - 1
                    )
                },
                isVisible = isUiVisible,
                onBack = onNavigateBack,
                onShare = {
                    onShare(currentPhoto)
                    lastInteractionTime = System.currentTimeMillis()
                },
                onDelete = {
                    onDelete(currentPhoto)
                    lastInteractionTime = System.currentTimeMillis()
                },
                onPrevious = {
                    if (currentIndex > 0) {
                        val navTracker = performanceTracker.trackTabSwitch("photo_${currentIndex}", "photo_${currentIndex - 1}")
                        currentIndex--
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        lastInteractionTime = System.currentTimeMillis()
                        navTracker.complete()
                    }
                },
                onNext = {
                    if (currentIndex < photos.size - 1) {
                        val navTracker = performanceTracker.trackTabSwitch("photo_${currentIndex}", "photo_${currentIndex + 1}")
                        currentIndex++
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        lastInteractionTime = System.currentTimeMillis()
                        navTracker.complete()
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
private fun PhotoViewerContent(
    photo: Photo,
    analysisResult: PhotoAnalysisWithTags?,
    showBoundingBoxes: Boolean,
    onPhotoTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader: ImageLoader = koinInject()
    
    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(photo.filePath)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "Construction Photo: ${photo.fileName}",
            modifier = Modifier
                .fillMaxSize()
                .clickable { onPhotoTap() },
            contentScale = ContentScale.Fit
        )
        
        // Hazard overlays
        if (showBoundingBoxes) {
            analysisResult?.hazardDetections?.let { hazards ->
                HazardOverlay(
                    hazards = hazards,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun PhotoInfoBottomSheet(
    photo: Photo,
    navigationState: PhotoNavigationState,
    safetyAnalysisState: SafetyAnalysisState,
    onAnalysisAction: (SafetyAnalysisAction) -> Unit,
    onTagsUpdated: (String, List<String>) -> Unit,
    analysisProgress: AnalysisProgress?,
    performanceTracker: PhotoViewerPerformanceTracker,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Safety Analysis", "Photo Info")
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column {
            // Tab bar with equal spacing
            TabRow(
                selectedTabIndex = selectedTab,
                contentColor = SafetyOrange,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { 
                            val tabTracker = performanceTracker.trackTabSwitch(tabs[selectedTab], tabs[index])
                            selectedTab = index
                            tabTracker.complete()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) SafetyOrange else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Tab content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> SafetyAnalysisTab(
                        photo = photo,
                        safetyAnalysisState = safetyAnalysisState,
                        onAnalysisAction = onAnalysisAction,
                        onTagsUpdated = onTagsUpdated,
                        analysisProgress = analysisProgress,
                        modifier = Modifier.fillMaxSize()
                    )
                    1 -> PhotoMetadataSection(
                        photo = photo,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyAnalysisTab(
    photo: Photo,
    safetyAnalysisState: SafetyAnalysisState,
    onAnalysisAction: (SafetyAnalysisAction) -> Unit,
    onTagsUpdated: (String, List<String>) -> Unit,
    analysisProgress: AnalysisProgress? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Show analysis progress if active
        analysisProgress?.let { progress ->
            AnalysisProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Conditional rendering based on analysis phase
        when (safetyAnalysisState.currentPhase) {
            AnalysisPhase.PRE_ANALYSIS -> {
                PreAnalysisView(
                    photo = photo,
                    manualTags = safetyAnalysisState.manualTags,
                    availableCategories = safetyAnalysisState.availableTagCategories,
                    isAddingTag = safetyAnalysisState.isAddingManualTag,
                    onAddTag = { name, category, note ->
                        val tag = createManualHazardTag(name, category, note)
                        onAnalysisAction(SafetyAnalysisAction.AddManualTag(tag))
                    },
                    onRemoveTag = { tagId ->
                        onAnalysisAction(SafetyAnalysisAction.RemoveManualTag(tagId))
                    },
                    onStartAnalysis = {
                        onAnalysisAction(SafetyAnalysisAction.StartAIAnalysis)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            AnalysisPhase.POST_ANALYSIS -> {
                PostAnalysisView(
                    photo = photo,
                    safetyAnalysisState = safetyAnalysisState,
                    onAnalysisAction = onAnalysisAction,
                    onTagsUpdated = onTagsUpdated,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Phase transition controls
        PhaseTransitionControls(
            currentPhase = safetyAnalysisState.currentPhase,
            canTransitionToAI = safetyAnalysisState.canProceedToAIAnalysis,
            hasAIResults = safetyAnalysisState.aiAnalysis != null,
            onTransitionToPreAnalysis = {
                onAnalysisAction(SafetyAnalysisAction.TransitionToPreAnalysis)
            },
            onTransitionToPostAnalysis = {
                onAnalysisAction(SafetyAnalysisAction.StartAIAnalysis)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}


