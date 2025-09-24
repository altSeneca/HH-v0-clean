# HazardHawk AR-Style UI Specifications

**Based on Reference Image Analysis**  
**Created**: September 9, 2025  
**UI Type**: Real-time AR Overlay + Post-Analysis View  

## UI Design Analysis from Reference Image

### Core Visual Elements Identified

1. **Live Camera Background**: Full-screen construction site view
2. **Hazard Detection Overlays**: Semi-transparent colored rectangles
3. **OSHA Code Badges**: Professional badges with regulation numbers
4. **Hazard Type Labels**: Clear, readable hazard classification
5. **Bounding Boxes**: Precise detection boundaries
6. **Color Coding**: Red for violations, yellow/amber for warnings

### Design Specifications

#### Color Palette (Construction Safety)
```kotlin
object HazardColors {
    val CRITICAL_RED = Color(0xFFE53E3E)      // Fall protection, immediate danger
    val HIGH_ORANGE = Color(0xFFFF8C00)       // PPE violations, high risk
    val MEDIUM_AMBER = Color(0xFFFFA500)      // Equipment hazards, medium risk  
    val LOW_YELLOW = Color(0xFFFFD700)        // Housekeeping, low risk
    val OSHA_BLUE = Color(0xFF2B6CB0)         // OSHA code backgrounds
    val SAFE_GREEN = Color(0xFF38A169)        // Compliant areas
}
```

#### Typography (Construction-Friendly)
```kotlin
object HazardTypography {
    val hazardTitle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        shadow = Shadow(
            color = Color.Black.copy(alpha = 0.8f),
            offset = Offset(2f, 2f),
            blurRadius = 4f
        )
    )
    
    val oshaCode = TextStyle(
        fontSize = 14.sp, 
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
        letterSpacing = 0.5.sp
    )
}
```

## Real-Time AR Overlay Components

### 1. Camera Overlay Container
```kotlin
@Composable
fun HazardDetectionOverlay(
    detections: List<HazardDetection>,
    cameraViewSize: Size,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Live camera view (handled by CameraX)
        AndroidView(
            factory = { context -> PreviewView(context) }
        )
        
        // Hazard detection overlays
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            detections.forEach { detection ->
                drawHazardOverlay(
                    detection = detection,
                    canvasSize = size
                )
            }
        }
        
        // OSHA badges and labels
        detections.forEach { detection ->
            HazardBadge(
                detection = detection,
                modifier = Modifier.offset(
                    x = detection.bounds.left.dp,
                    y = detection.bounds.top.dp
                )
            )
        }
    }
}
```

### 2. Hazard Detection Badge
```kotlin
@Composable
fun HazardBadge(
    detection: HazardDetection,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        // OSHA Code Badge
        Box(
            modifier = Modifier
                .background(
                    color = HazardColors.OSHA_BLUE,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = detection.oshaCode,
                style = HazardTypography.oshaCode
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Hazard Type Label
        Text(
            text = detection.hazardType,
            style = HazardTypography.hazardTitle,
            color = getHazardColor(detection.severity)
        )
        
        // Confidence indicator (optional)
        if (detection.confidence > 0.8f) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "High Confidence",
                    tint = HazardColors.SAFE_GREEN,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${(detection.confidence * 100).toInt()}%",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = Color.White
                    )
                )
            }
        }
    }
}
```

### 3. Bounding Box Drawing
```kotlin
fun DrawScope.drawHazardOverlay(
    detection: HazardDetection,
    canvasSize: Size
) {
    val bounds = detection.bounds
    val hazardColor = getHazardColor(detection.severity)
    
    // Draw bounding box
    drawRect(
        color = hazardColor,
        topLeft = Offset(bounds.left, bounds.top),
        size = Size(bounds.width, bounds.height),
        style = Stroke(
            width = 3.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(10f, 5f), 0f
            )
        )
    )
    
    // Draw semi-transparent fill
    drawRect(
        color = hazardColor.copy(alpha = 0.2f),
        topLeft = Offset(bounds.left, bounds.top),
        size = Size(bounds.width, bounds.height),
        style = Fill
    )
    
    // Draw corner markers (for AR effect)
    val cornerSize = 20.dp.toPx()
    drawCornerMarkers(
        bounds = bounds,
        color = hazardColor,
        cornerSize = cornerSize
    )
}

fun DrawScope.drawCornerMarkers(
    bounds: Rect,
    color: Color,
    cornerSize: Float
) {
    val strokeWidth = 4.dp.toPx()
    
    // Top-left corner
    drawLine(
        color = color,
        start = Offset(bounds.left, bounds.top),
        end = Offset(bounds.left + cornerSize, bounds.top),
        strokeWidth = strokeWidth
    )
    drawLine(
        color = color,
        start = Offset(bounds.left, bounds.top),
        end = Offset(bounds.left, bounds.top + cornerSize),
        strokeWidth = strokeWidth
    )
    
    // Repeat for other corners...
}
```

## Post-Analysis View Components

### 1. Analysis Results Screen
```kotlin
@Composable
fun AnalysisResultsScreen(
    analysisResult: SafetyAnalysis,
    originalImage: ImageBitmap,
    onRetakePhoto: () -> Unit,
    onSaveReport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Header with analysis summary
        AnalysisHeader(
            totalHazards = analysisResult.hazards.size,
            criticalCount = analysisResult.hazards.count { it.severity == Severity.CRITICAL },
            analysisTime = analysisResult.processingTimeMs
        )
        
        // Image with overlays (static version)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Image(
                bitmap = originalImage,
                contentDescription = "Analyzed construction photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Static hazard overlays
            analysisResult.hazards.forEach { hazard ->
                HazardOverlayStatic(
                    hazard = hazard,
                    modifier = Modifier
                        .offset(
                            x = hazard.boundingBox.left.dp,
                            y = hazard.boundingBox.top.dp
                        )
                        .size(
                            width = hazard.boundingBox.width.dp,
                            height = hazard.boundingBox.height.dp
                        )
                )
            }
        }
        
        // Hazards list with expansion
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(analysisResult.hazards.sortedByDescending { it.severity.ordinal }) { hazard ->
                HazardDetailCard(
                    hazard = hazard,
                    onClick = { /* Highlight on image */ }
                )
            }
        }
        
        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onRetakePhoto,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Gray
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Retake")
            }
            
            Button(
                onClick = onSaveReport,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = HazardColors.OSHA_BLUE
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save Report")
            }
        }
    }
}
```

### 2. Hazard Detail Card
```kotlin
@Composable
fun HazardDetailCard(
    hazard: Hazard,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundColor = Color.Gray.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Severity indicator
                SeverityIndicator(severity = hazard.severity)
                
                // OSHA code badge  
                OSHACodeBadge(code = hazard.oshaCode)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Hazard description
            Text(
                text = hazard.description,
                style = MaterialTheme.typography.body1,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            
            // Expandable recommendations
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Recommended Actions:",
                        style = MaterialTheme.typography.caption,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    hazard.recommendations.forEach { recommendation ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "• ",
                                color = HazardColors.OSHA_BLUE,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = recommendation,
                                style = MaterialTheme.typography.body2,
                                color = Color.White
                            )
                        }
                    }
                }
            }
            
            // Expand/collapse button
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = if (expanded) "Show Less" else "View Details",
                    color = HazardColors.OSHA_BLUE
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = HazardColors.OSHA_BLUE
                )
            }
        }
    }
}
```

## Real-Time Detection Integration

### 1. AI Model Integration with UI
```kotlin
@Composable
fun LiveHazardDetectionScreen(
    viewModel: LiveDetectionViewModel
) {
    val detections by viewModel.liveDetections.collectAsState()
    val analysisState by viewModel.analysisState.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        CameraPreview(
            onImageCaptured = { imageData ->
                viewModel.analyzeFrame(imageData)
            }
        )
        
        // Real-time hazard overlays
        when (analysisState) {
            is AnalysisState.Analyzing -> {
                AnalyzingOverlay(
                    progress = analysisState.progress,
                    currentTask = analysisState.currentTask
                )
            }
            is AnalysisState.Results -> {
                HazardDetectionOverlay(
                    detections = detections,
                    cameraViewSize = LocalConfiguration.current.run { 
                        Size(screenWidthDp.toFloat(), screenHeightDp.toFloat()) 
                    }
                )
            }
            is AnalysisState.Error -> {
                ErrorOverlay(
                    error = analysisState.error,
                    onRetry = viewModel::retryAnalysis
                )
            }
        }
        
        // Camera controls
        CameraControlsOverlay(
            onCapturePhoto = viewModel::captureAndAnalyze,
            onToggleFlash = viewModel::toggleFlash,
            onSwitchCamera = viewModel::switchCamera,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Settings access
        IconButton(
            onClick = { /* Navigate to settings */ },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .padding(8.dp)
            )
        }
    }
}
```

## Performance Optimizations for AR UI

### 1. Efficient Overlay Rendering
```kotlin
class HazardOverlayRenderer {
    private val overlayCache = mutableMapOf<String, ImageBitmap>()
    
    fun renderOptimized(
        detections: List<HazardDetection>,
        canvasSize: Size
    ): ImageBitmap {
        // Cache static elements
        val cacheKey = detections.joinToString { "${it.id}-${it.bounds}" }
        
        return overlayCache.getOrPut(cacheKey) {
            createOverlayBitmap(detections, canvasSize)
        }
    }
    
    private fun createOverlayBitmap(
        detections: List<HazardDetection>,
        canvasSize: Size
    ): ImageBitmap {
        return ImageBitmap(
            width = canvasSize.width.toInt(),
            height = canvasSize.height.toInt()
        ).apply {
            Canvas(this).apply {
                detections.forEach { detection ->
                    drawHazardOverlay(detection, canvasSize)
                }
            }
        }
    }
}
```

### 2. Frame Rate Optimization
```kotlin
class LiveDetectionViewModel : ViewModel() {
    private var lastAnalysisTime = 0L
    private val analysisInterval = 500L // 2 FPS for AI analysis
    private val uiUpdateInterval = 33L   // 30 FPS for UI updates
    
    fun analyzeFrame(imageData: ByteArray) {
        val currentTime = System.currentTimeMillis()
        
        // Throttle AI analysis
        if (currentTime - lastAnalysisTime >= analysisInterval) {
            lastAnalysisTime = currentTime
            
            viewModelScope.launch {
                val result = aiOrchestrator.analyzePhoto(imageData, currentWorkType)
                result.onSuccess { analysis ->
                    updateDetections(analysis.hazards)
                }
            }
        }
    }
    
    private fun updateDetections(hazards: List<Hazard>) {
        val detections = hazards.map { hazard ->
            HazardDetection(
                id = hazard.id,
                hazardType = hazard.type.displayName,
                oshaCode = hazard.oshaCode ?: "N/A",
                severity = hazard.severity,
                bounds = hazard.boundingBox,
                confidence = hazard.confidence
            )
        }
        
        _liveDetections.value = detections
    }
}
```

This AR-style UI specification matches your reference image perfectly and integrates with the Gemma 3N E2B multimodal analysis system for real-time construction safety detection with professional OSHA compliance overlays.