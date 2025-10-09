package com.hazardhawk.ui.crew

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hazardhawk.ui.theme.ConstructionColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Certification Upload Screen - 9-State Upload Workflow
 *
 * States:
 * 1. Idle - Source selection (Camera/Gallery)
 * 2. Camera - Live camera with document frame overlay
 * 3. UploadProgress - Animated progress (0-100%)
 * 4. Processing - OCR processing spinner
 * 5. OCRReview - Confidence badges and field display
 * 6. ManualEntry - Fallback form
 * 7. Submitting - Final submission
 * 8. Success - Animated checkmark
 * 9. Error - Retry/cancel options
 *
 * Features:
 * - Construction-worker friendly UX (80dp buttons, high contrast)
 * - Haptic feedback on success
 * - Document frame overlay with animated corners
 * - Confidence badges (>85% green, 60-85% amber, <60% red)
 * - Auto-navigation after success
 */

// State Machine
sealed class UploadState {
    object Idle : UploadState()
    object Camera : UploadState()
    data class UploadProgress(val progress: Float) : UploadState()
    object Processing : UploadState()
    data class OCRReview(val extractedData: ExtractedCertData) : UploadState()
    object ManualEntry : UploadState()
    object Submitting : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

// Data Classes
data class ExtractedCertData(
    val holderName: String,
    val certificationType: String,
    val certificationNumber: String?,
    val issueDate: String?,
    val expirationDate: String?,
    val issuingAuthority: String?,
    val confidence: Float,
    val needsReview: Boolean
)

// ViewModel
class CertificationUploadViewModel : ViewModel() {
    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state.asStateFlow()

    private val _formData = MutableStateFlow(CertFormData())
    val formData: StateFlow<CertFormData> = _formData.asStateFlow()

    fun selectCamera() {
        _state.value = UploadState.Camera
        // TODO: Launch CameraX
    }

    fun selectGallery() {
        // TODO: Launch gallery picker
        simulateUpload()
    }

    fun capturePhoto() {
        // TODO: Capture from camera
        simulateUpload()
    }

    private fun simulateUpload() {
        viewModelScope.launch {
            // Simulate upload progress
            for (i in 0..100 step 5) {
                _state.value = UploadState.UploadProgress(i / 100f)
                delay(50)
            }

            // Simulate OCR processing
            _state.value = UploadState.Processing
            delay(2000)

            // Simulate OCR results
            val mockData = ExtractedCertData(
                holderName = "John Doe",
                certificationType = "OSHA 30",
                certificationNumber = "12345678",
                issueDate = "2024-06-15",
                expirationDate = "2029-06-15",
                issuingAuthority = "OSHA",
                confidence = 0.92f,
                needsReview = false
            )
            _state.value = UploadState.OCRReview(mockData)
        }
    }

    fun confirmOCRData(data: ExtractedCertData) {
        _formData.value = CertFormData(
            holderName = data.holderName,
            certificationType = data.certificationType,
            certificationNumber = data.certificationNumber ?: "",
            issueDate = data.issueDate ?: "",
            expirationDate = data.expirationDate ?: "",
            issuingAuthority = data.issuingAuthority ?: ""
        )
        submitCertification()
    }

    fun editManually() {
        _state.value = UploadState.ManualEntry
    }

    fun updateFormData(data: CertFormData) {
        _formData.value = data
    }

    fun submitCertification() {
        _state.value = UploadState.Submitting

        viewModelScope.launch {
            // TODO: Submit to WorkerCertificationRepository
            delay(1500)
            _state.value = UploadState.Success

            // Auto-reset after 2 seconds
            delay(2000)
            reset()
        }
    }

    fun retry() {
        _state.value = UploadState.Idle
    }

    fun reset() {
        _state.value = UploadState.Idle
        _formData.value = CertFormData()
    }

    fun cancel() {
        reset()
    }
}

data class CertFormData(
    val holderName: String = "",
    val certificationType: String = "",
    val certificationNumber: String = "",
    val issueDate: String = "",
    val expirationDate: String = "",
    val issuingAuthority: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun CertificationUploadScreen(
    workerId: String,
    onNavigateBack: () -> Unit,
    onUploadComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CertificationUploadViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val formData by viewModel.formData.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Certification") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state is UploadState.Idle) {
                            onNavigateBack()
                        } else {
                            viewModel.cancel()
                        }
                    }) {
                        Icon(Icons.Default.Close, "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConstructionColors.WorkZoneBlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ConstructionColors.Surface)
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) with
                            fadeOut(animationSpec = tween(300))
                },
                label = "upload_state"
            ) { currentState ->
                when (currentState) {
                    is UploadState.Idle -> IdleScreen(
                        onCameraClick = { viewModel.selectCamera() },
                        onGalleryClick = { viewModel.selectGallery() }
                    )
                    is UploadState.Camera -> CameraScreen(
                        onCapture = { viewModel.capturePhoto() },
                        onCancel = { viewModel.cancel() }
                    )
                    is UploadState.UploadProgress -> UploadProgressScreen(
                        progress = currentState.progress
                    )
                    is UploadState.Processing -> ProcessingScreen()
                    is UploadState.OCRReview -> OCRReviewScreen(
                        data = currentState.extractedData,
                        onConfirm = { viewModel.confirmOCRData(currentState.extractedData) },
                        onEdit = { viewModel.editManually() }
                    )
                    is UploadState.ManualEntry -> ManualEntryScreen(
                        formData = formData,
                        onFormUpdate = { viewModel.updateFormData(it) },
                        onSubmit = { viewModel.submitCertification() },
                        onCancel = { viewModel.cancel() }
                    )
                    is UploadState.Submitting -> SubmittingScreen()
                    is UploadState.Success -> SuccessScreen()
                    is UploadState.Error -> ErrorScreen(
                        message = currentState.message,
                        onRetry = { viewModel.retry() },
                        onCancel = { viewModel.cancel() }
                    )
                }
            }
        }
    }
}

// ========== State Screens ==========

@Composable
private fun IdleScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.AddCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = ConstructionColors.SafetyOrange
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Upload Certification",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Take a photo or select from gallery",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onCameraClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConstructionColors.SafetyOrange
            )
        ) {
            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Take Photo", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Icon(Icons.Default.Photo, null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Choose from Gallery", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CameraScreen(
    onCapture: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // TODO: CameraX preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
        )

        // Document frame overlay
        DocumentFrameOverlay()

        // Capture button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onCapture,
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                // Capture circle
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onCancel) {
                Text("Cancel", color = Color.White)
            }
        }
    }
}

@Composable
private fun DocumentFrameOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "frame")
    val animatedAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corner_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val frameSize = size.minDimension * 0.7f
        val frameLeft = (size.width - frameSize) / 2
        val frameTop = (size.height - frameSize) / 2
        val cornerLength = 40.dp.toPx()

        // Draw animated corners
        val cornerColor = Color.White.copy(alpha = animatedAlpha)

        // Top-left corner
        drawPath(
            path = Path().apply {
                moveTo(frameLeft, frameTop + cornerLength)
                lineTo(frameLeft, frameTop)
                lineTo(frameLeft + cornerLength, frameTop)
            },
            color = cornerColor,
            style = Stroke(width = 4.dp.toPx())
        )

        // Top-right corner
        drawPath(
            path = Path().apply {
                moveTo(frameLeft + frameSize - cornerLength, frameTop)
                lineTo(frameLeft + frameSize, frameTop)
                lineTo(frameLeft + frameSize, frameTop + cornerLength)
            },
            color = cornerColor,
            style = Stroke(width = 4.dp.toPx())
        )

        // Bottom-left corner
        drawPath(
            path = Path().apply {
                moveTo(frameLeft, frameTop + frameSize - cornerLength)
                lineTo(frameLeft, frameTop + frameSize)
                lineTo(frameLeft + cornerLength, frameTop + frameSize)
            },
            color = cornerColor,
            style = Stroke(width = 4.dp.toPx())
        )

        // Bottom-right corner
        drawPath(
            path = Path().apply {
                moveTo(frameLeft + frameSize - cornerLength, frameTop + frameSize)
                lineTo(frameLeft + frameSize, frameTop + frameSize)
                lineTo(frameLeft + frameSize, frameTop + frameSize - cornerLength)
            },
            color = cornerColor,
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

@Composable
private fun UploadProgressScreen(progress: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(120.dp),
            color = ConstructionColors.SafetyOrange,
            strokeWidth = 8.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Uploading...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ProcessingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(120.dp),
            color = ConstructionColors.WorkZoneBlue,
            strokeWidth = 8.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Processing with AI...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Extracting certification data",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun OCRReviewScreen(
    data: ExtractedCertData,
    onConfirm: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        // Confidence badge
        ConfidenceBadge(confidence = data.confidence)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Review Extracted Data",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Extracted fields
        ExtractedField("Name", data.holderName, data.confidence > 0.85f)
        ExtractedField("Type", data.certificationType, data.confidence > 0.85f)
        data.certificationNumber?.let { ExtractedField("Number", it, data.confidence > 0.75f) }
        data.issueDate?.let { ExtractedField("Issue Date", it, data.confidence > 0.80f) }
        data.expirationDate?.let { ExtractedField("Expiration", it, data.confidence > 0.80f) }
        data.issuingAuthority?.let { ExtractedField("Authority", it, data.confidence > 0.85f) }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConstructionColors.SafetyGreen
            )
        ) {
            Icon(Icons.Default.Check, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirm & Submit", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            Icon(Icons.Default.Edit, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Manually", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: Float) {
    val (color, icon, label) = when {
        confidence >= 0.85f -> Triple(
            ConstructionColors.SafetyGreen,
            Icons.Default.CheckCircle,
            "High Confidence"
        )
        confidence >= 0.60f -> Triple(
            Color(0xFFFF9800),
            Icons.Default.Warning,
            "Medium Confidence"
        )
        else -> Triple(
            ConstructionColors.CautionRed,
            Icons.Default.Error,
            "Low Confidence - Review Needed"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(label, fontWeight = FontWeight.Bold, color = color)
                Text("${(confidence * 100).toInt()}% accuracy", color = color.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun ExtractedField(label: String, value: String, highConfidence: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
            if (highConfidence) {
                Icon(Icons.Default.Check, null, tint = ConstructionColors.SafetyGreen)
            } else {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualEntryScreen(
    formData: CertFormData,
    onFormUpdate: (CertFormData) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            "Manual Entry",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = formData.holderName,
            onValueChange = { onFormUpdate(formData.copy(holderName = it)) },
            label = { Text("Holder Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formData.certificationType,
            onValueChange = { onFormUpdate(formData.copy(certificationType = it)) },
            label = { Text("Certification Type") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formData.certificationNumber,
            onValueChange = { onFormUpdate(formData.copy(certificationNumber = it)) },
            label = { Text("Certification Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formData.issueDate,
            onValueChange = { onFormUpdate(formData.copy(issueDate = it)) },
            label = { Text("Issue Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formData.expirationDate,
            onValueChange = { onFormUpdate(formData.copy(expirationDate = it)) },
            label = { Text("Expiration Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = formData.issuingAuthority,
            onValueChange = { onFormUpdate(formData.copy(issuingAuthority = it)) },
            label = { Text("Issuing Authority") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConstructionColors.SafetyOrange
            )
        ) {
            Text("Submit", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}

@Composable
private fun SubmittingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(120.dp),
            color = ConstructionColors.SafetyOrange,
            strokeWidth = 8.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Submitting...",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SuccessScreen() {
    val view = LocalView.current

    LaunchedEffect(Unit) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "success")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "checkmark_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier
                .size((120 * scale).dp),
            tint = ConstructionColors.SafetyGreen
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Success!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ConstructionColors.SafetyGreen
        )

        Text(
            "Certification uploaded successfully",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ErrorScreen(
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = ConstructionColors.CautionRed
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Upload Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ConstructionColors.CautionRed
        )

        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ConstructionColors.SafetyOrange
            )
        ) {
            Icon(Icons.Default.Refresh, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancel")
        }
    }
}
