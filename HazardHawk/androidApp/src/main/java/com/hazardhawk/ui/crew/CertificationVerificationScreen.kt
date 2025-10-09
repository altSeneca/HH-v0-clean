package com.hazardhawk.ui.crew

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Certification Verification Screen - Admin Review Workflow
 *
 * Features:
 * - Statistics dashboard (pending, flagged, approved today)
 * - Queue overview with thumbnails
 * - Certification detail view (60/40 split: document/data)
 * - Zoomable document viewer (1x-5x scale)
 * - Rejection dialog with predefined reasons
 * - Filter and sort controls
 * - Empty state celebration
 *
 * Admin Workflow:
 * Queue → Filter/Sort → Select Item → Review → Approve/Reject/Skip → Next Item
 */

// Data Models
data class CertificationQueueItem(
    val id: String,
    val workerId: String,
    val workerName: String,
    val certificationType: String,
    val documentUrl: String,
    val thumbnailUrl: String?,
    val uploadedAt: String,
    val ocrConfidence: Float,
    val status: String,
    val isUserEdited: Boolean
)

enum class RejectionReason(val displayName: String) {
    EXPIRED("Certificate Expired"),
    UNREADABLE("Document Unreadable"),
    WRONG_TYPE("Wrong Certificate Type"),
    MISSING_INFO("Missing Information"),
    INVALID("Invalid Certificate"),
    CUSTOM("Other (specify)")
}

enum class VerificationFilter {
    ALL, LOW_CONFIDENCE, USER_EDITED
}

enum class VerificationSort(val displayName: String) {
    RECENT("Most Recent"),
    OLDEST("Oldest First"),
    BY_WORKER("By Worker Name"),
    LOW_CONFIDENCE_FIRST("Low Confidence First")
}

data class VerificationStatistics(
    val pendingCount: Int,
    val flaggedCount: Int,
    val approvedToday: Int
)

// ViewModel
class CertificationVerificationViewModel : ViewModel() {
    private val _queueItems = MutableStateFlow<List<CertificationQueueItem>>(emptyList())
    val queueItems: StateFlow<List<CertificationQueueItem>> = _queueItems.asStateFlow()

    private val _selectedItem = MutableStateFlow<CertificationQueueItem?>(null)
    val selectedItem: StateFlow<CertificationQueueItem?> = _selectedItem.asStateFlow()

    private val _filter = MutableStateFlow(VerificationFilter.ALL)
    val filter: StateFlow<VerificationFilter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(VerificationSort.RECENT)
    val sort: StateFlow<VerificationSort> = _sort.asStateFlow()

    private val _statistics = MutableStateFlow(VerificationStatistics(0, 0, 0))
    val statistics: StateFlow<VerificationStatistics> = _statistics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadQueue()
    }

    fun loadQueue() {
        viewModelScope.launch {
            _isLoading.value = true

            // TODO: Load from CertificationRepository
            delay(500)

            // Mock data
            _queueItems.value = listOf(
                CertificationQueueItem(
                    id = "1",
                    workerId = "worker1",
                    workerName = "John Doe",
                    certificationType = "OSHA 30",
                    documentUrl = "",
                    thumbnailUrl = null,
                    uploadedAt = "2 hours ago",
                    ocrConfidence = 0.92f,
                    status = "pending",
                    isUserEdited = false
                ),
                CertificationQueueItem(
                    id = "2",
                    workerId = "worker2",
                    workerName = "Jane Smith",
                    certificationType = "Forklift",
                    documentUrl = "",
                    thumbnailUrl = null,
                    uploadedAt = "4 hours ago",
                    ocrConfidence = 0.68f,
                    status = "pending",
                    isUserEdited = true
                )
            )

            _statistics.value = VerificationStatistics(
                pendingCount = 23,
                flaggedCount = 2,
                approvedToday = 15
            )

            _isLoading.value = false
        }
    }

    fun selectItem(item: CertificationQueueItem) {
        _selectedItem.value = item
    }

    fun closeDetail() {
        _selectedItem.value = null
    }

    fun setFilter(filter: VerificationFilter) {
        _filter.value = filter
    }

    fun setSort(sort: VerificationSort) {
        _sort.value = sort
    }

    fun approveItem(item: CertificationQueueItem) {
        viewModelScope.launch {
            // TODO: Call CertificationRepository.approve()
            // TODO: Send notification via NotificationService

            delay(500)

            _queueItems.value = _queueItems.value.filter { it.id != item.id }
            _selectedItem.value = null
            _successMessage.value = "Certification approved"
            _statistics.value = _statistics.value.copy(
                pendingCount = _statistics.value.pendingCount - 1,
                approvedToday = _statistics.value.approvedToday + 1
            )

            // Auto-load next item if available
            _queueItems.value.firstOrNull()?.let { selectItem(it) }
        }
    }

    fun rejectItem(item: CertificationQueueItem, reason: RejectionReason, customReason: String?) {
        viewModelScope.launch {
            // TODO: Call CertificationRepository.reject()
            // TODO: Send notification via NotificationService

            delay(500)

            _queueItems.value = _queueItems.value.filter { it.id != item.id }
            _selectedItem.value = null
            _successMessage.value = "Certification rejected"
            _statistics.value = _statistics.value.copy(
                pendingCount = _statistics.value.pendingCount - 1
            )
        }
    }

    fun skipItem() {
        _selectedItem.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertificationVerificationScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CertificationVerificationViewModel = viewModel()
) {
    val queueItems by viewModel.queueItems.collectAsStateWithLifecycle()
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Certifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadQueue() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConstructionColors.WorkZoneBlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ConstructionColors.Surface)
        ) {
            if (selectedItem != null) {
                CertificationDetailView(
                    item = selectedItem!!,
                    onClose = { viewModel.closeDetail() },
                    onApprove = { viewModel.approveItem(selectedItem!!) },
                    onReject = { reason, customReason ->
                        viewModel.rejectItem(selectedItem!!, reason, customReason)
                    },
                    onSkip = { viewModel.skipItem() }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Statistics Dashboard
                    StatisticsDashboard(statistics = statistics)

                    // Filter and Sort Controls
                    FilterSortControls(
                        currentFilter = filter,
                        currentSort = sort,
                        onFilterChange = { viewModel.setFilter(it) },
                        onSortChange = { viewModel.setSort(it) }
                    )

                    // Queue Overview
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (queueItems.isEmpty()) {
                        EmptyQueueState()
                    } else {
                        QueueOverview(
                            items = queueItems,
                            onItemClick = { viewModel.selectItem(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsDashboard(statistics: VerificationStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                icon = Icons.Default.Schedule,
                count = statistics.pendingCount,
                label = "Pending",
                color = Color(0xFFFF9800) // Orange
            )
            StatCard(
                icon = Icons.Default.Warning,
                count = statistics.flaggedCount,
                label = "Flagged",
                color = ConstructionColors.CautionRed
            )
            StatCard(
                icon = Icons.Default.CheckCircle,
                count = statistics.approvedToday,
                label = "Today",
                color = ConstructionColors.SafetyGreen
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSortControls(
    currentFilter: VerificationFilter,
    currentSort: VerificationSort,
    onFilterChange: (VerificationFilter) -> Unit,
    onSortChange: (VerificationSort) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Filter chips
        Text("Filter:", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = currentFilter == VerificationFilter.ALL,
                onClick = { onFilterChange(VerificationFilter.ALL) },
                label = { Text("All") }
            )
            FilterChip(
                selected = currentFilter == VerificationFilter.LOW_CONFIDENCE,
                onClick = { onFilterChange(VerificationFilter.LOW_CONFIDENCE) },
                label = { Text("Low Confidence") }
            )
            FilterChip(
                selected = currentFilter == VerificationFilter.USER_EDITED,
                onClick = { onFilterChange(VerificationFilter.USER_EDITED) },
                label = { Text("User Edited") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sort dropdown
        var sortExpanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = sortExpanded,
            onExpandedChange = { sortExpanded = it }
        ) {
            OutlinedTextField(
                value = currentSort.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Sort by") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            ExposedDropdownMenu(
                expanded = sortExpanded,
                onDismissRequest = { sortExpanded = false }
            ) {
                VerificationSort.entries.forEach { sort ->
                    DropdownMenuItem(
                        text = { Text(sort.displayName) },
                        onClick = {
                            onSortChange(sort)
                            sortExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueOverview(
    items: List<CertificationQueueItem>,
    onItemClick: (CertificationQueueItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            CertificationQueueCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CertificationQueueCard(
    item: CertificationQueueItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.workerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.certificationType,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = item.uploadedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Confidence badge
            val badgeColor = when {
                item.ocrConfidence >= 0.85f -> ConstructionColors.SafetyGreen
                item.ocrConfidence >= 0.60f -> Color(0xFFFF9800)
                else -> ConstructionColors.CautionRed
            }

            Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "${(item.ocrConfidence * 100).toInt()}%",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
private fun EmptyQueueState() {
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
            modifier = Modifier.size(120.dp),
            tint = ConstructionColors.SafetyGreen
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "All Caught Up! 🎉",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "No certifications pending review",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CertificationDetailView(
    item: CertificationQueueItem,
    onClose: () -> Unit,
    onApprove: () -> Unit,
    onReject: (RejectionReason, String?) -> Unit,
    onSkip: () -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Close button bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, "Close")
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onSkip) {
                    Text("Skip")
                }
            }
        }

        Row(modifier = Modifier.weight(1f)) {
            // Document viewer (60%)
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .fillMaxHeight()
                    .background(Color.Black)
            ) {
                ZoomableDocumentViewer(documentUrl = item.documentUrl)
            }

            // Data panel (40%)
            CertificationDataPanel(
                item = item,
                modifier = Modifier.weight(0.4f)
            )
        }

        // Action buttons
        ActionButtonBar(
            onSkip = onSkip,
            onReject = { showRejectDialog = true },
            onApprove = onApprove
        )
    }

    if (showRejectDialog) {
        RejectionDialog(
            onDismiss = { showRejectDialog = false },
            onConfirm = { reason, customReason ->
                onReject(reason, customReason)
                showRejectDialog = false
            }
        )
    }
}

@Composable
private fun ZoomableDocumentViewer(documentUrl: String) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // TODO: Load actual document
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            tint = Color.White.copy(alpha = 0.5f)
        )

        // Zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { scale = (scale + 0.5f).coerceAtMost(5f) },
                modifier = Modifier.size(48.dp),
                containerColor = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.Add, "Zoom in", tint = Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            FloatingActionButton(
                onClick = { scale = (scale - 0.5f).coerceAtLeast(1f) },
                modifier = Modifier.size(48.dp),
                containerColor = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.Remove, "Zoom out", tint = Color.Black)
            }

            Spacer(modifier = Modifier.height(8.dp))

            FloatingActionButton(
                onClick = {
                    scale = 1f
                    offsetX = 0f
                    offsetY = 0f
                },
                modifier = Modifier.size(48.dp),
                containerColor = Color.White.copy(alpha = 0.9f)
            ) {
                Icon(Icons.Default.RestartAlt, "Reset", tint = Color.Black)
            }
        }

        // Zoom level indicator
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.9f)
        ) {
            Text(
                text = "${(scale * 100).toInt()}%",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun CertificationDataPanel(
    item: CertificationQueueItem,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Certification Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (item.ocrConfidence < 0.80f) {
            item {
                Surface(
                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = Color(0xFFFF9800))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Low confidence - verify carefully",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
        }

        item { DataField("Worker", item.workerName) }
        item { DataField("Type", item.certificationType) }
        item { DataField("OCR Confidence", "${(item.ocrConfidence * 100).toInt()}%") }
        item { DataField("Uploaded", item.uploadedAt) }
        item { DataField("Status", item.status) }
        item { DataField("Worker ID", item.workerId) }
    }
}

@Composable
private fun DataField(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionButtonBar(
    onSkip: () -> Unit,
    onReject: () -> Unit,
    onApprove: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
            ) {
                Icon(Icons.Default.SkipNext, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Skip")
            }

            Button(
                onClick = onReject,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConstructionColors.CautionRed
                )
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reject")
            }

            Button(
                onClick = onApprove,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConstructionColors.SafetyGreen
                )
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Approve")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RejectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (RejectionReason, String?) -> Unit
) {
    var selectedReason by remember { mutableStateOf<RejectionReason?>(null) }
    var customReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reject Certification") },
        text = {
            Column {
                Text("Select a reason:")
                Spacer(modifier = Modifier.height(16.dp))

                RejectionReason.entries.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(reason.displayName) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                AnimatedVisibility(visible = selectedReason == RejectionReason.CUSTOM) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customReason,
                            onValueChange = { customReason = it },
                            label = { Text("Specify reason") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedReason?.let { reason ->
                        onConfirm(
                            reason,
                            if (reason == RejectionReason.CUSTOM) customReason else null
                        )
                    }
                },
                enabled = selectedReason != null &&
                        (selectedReason != RejectionReason.CUSTOM || customReason.isNotBlank())
            ) {
                Text("Reject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
