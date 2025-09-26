package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazardhawk.camera.CaptureMetadata
import com.hazardhawk.camera.MetadataEmbedder
import com.hazardhawk.camera.MetadataSettingsManager
import com.hazardhawk.domain.entities.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import android.location.Geocoder
import java.util.Locale

/**
 * Reusable Photo Metadata Section Component
 * Displays photo information and metadata in a clean, organized way
 */
@Composable
fun PhotoMetadataSection(
    photo: Photo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var extractedMetadata by remember(photo.id) { mutableStateOf<CaptureMetadata?>(null) }
    var locationAddress by remember(photo.id) { mutableStateOf<String?>(null) }
    var isLoadingMetadata by remember(photo.id) { mutableStateOf(false) }
    
    // Get settings for GPS display preference
    val metadataSettingsManager = remember { MetadataSettingsManager(context) }
    val appSettings by metadataSettingsManager.appSettings.collectAsStateWithLifecycle()
    val showGPSCoordinates by remember(appSettings) {
        mutableStateOf(appSettings.dataPrivacy.showGPSCoordinates)
    }
    
    // Extract metadata when component loads
    LaunchedEffect(photo.id) {
        loadPhotoMetadata(
            photo = photo,
            context = context,
            onMetadataLoaded = { metadata -> extractedMetadata = metadata },
            onLocationLoaded = { address -> locationAddress = address },
            onLoadingChanged = { loading -> isLoadingMetadata = loading }
        )
    }
    
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PhotoInfoCard(
                photo = photo,
                extractedMetadata = extractedMetadata,
                locationAddress = locationAddress,
                showGPSCoordinates = showGPSCoordinates,
                isLoadingMetadata = isLoadingMetadata
            )
        }
    }
}

@Composable
private fun PhotoInfoCard(
    photo: Photo,
    extractedMetadata: CaptureMetadata?,
    locationAddress: String?,
    showGPSCoordinates: Boolean,
    isLoadingMetadata: Boolean
) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Photo Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Basic photo info
            MetadataRow(label = "Captured", value = formatTimestamp(photo.timestamp))
            MetadataRow(label = "Photo ID", value = photo.id)
            MetadataRow(label = "File Name", value = photo.fileName)
            
            // Dynamic project info
            val projectName = getProjectName(photo, extractedMetadata, context)
            MetadataRow(label = "Project", value = projectName)
            
            // Location information
            val locationValue = getLocationValue(
                photo = photo,
                extractedMetadata = extractedMetadata,
                locationAddress = locationAddress,
                showGPSCoordinates = showGPSCoordinates,
                isLoadingMetadata = isLoadingMetadata
            )
            MetadataRow(label = "Location", value = locationValue)
            
            // Additional metadata
            extractedMetadata?.let { metadata ->
                if (metadata.userName.isNotBlank()) {
                    MetadataRow(label = "Captured by", value = metadata.userName)
                }
                if (metadata.deviceInfo.isNotBlank()) {
                    MetadataRow(label = "Device", value = metadata.deviceInfo)
                }
            }
            
            // Technical details
            if (photo.fileSize > 0) {
                val sizeInMB = photo.fileSize / (1024.0 * 1024.0)
                MetadataRow(label = "File Size", value = String.format("%.2f MB", sizeInMB))
            }
            
            photo.width?.let { width ->
                photo.height?.let { height ->
                    MetadataRow(label = "Dimensions", value = "${width} × ${height}")
                }
            }
        }
    }
}

@Composable
fun MetadataRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(2f)
        )
    }
}

private suspend fun loadPhotoMetadata(
    photo: Photo,
    context: android.content.Context,
    onMetadataLoaded: (CaptureMetadata?) -> Unit,
    onLocationLoaded: (String?) -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    onLoadingChanged(true)
    
    try {
        withContext(Dispatchers.IO) {
            val metadataEmbedder = MetadataEmbedder(context)
            val photoFile = File(photo.filePath)
            
            if (photoFile.exists()) {
                val metadata = metadataEmbedder.extractMetadataFromPhoto(photoFile)
                onMetadataLoaded(metadata)
                
                // Convert GPS coordinates to human-readable address if available
                metadata?.locationData?.let { locationData ->
                    if (locationData.isAvailable) {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            val addresses = geocoder.getFromLocation(
                                locationData.latitude,
                                locationData.longitude,
                                1
                            )
                            val address = addresses?.firstOrNull()?.let { address ->
                                buildString {
                                    address.thoroughfare?.let { append("$it, ") }
                                    address.locality?.let { append("$it, ") }
                                    address.adminArea?.let { append(it) }
                                }.takeIf { it.isNotBlank() } ?: "${String.format("%.6f", locationData.latitude)}, ${String.format("%.6f", locationData.longitude)}"
                            }
                            onLocationLoaded(address)
                        } catch (e: Exception) {
                            // Fallback to coordinates if geocoding fails
                            onLocationLoaded("${String.format("%.6f", locationData.latitude)}, ${String.format("%.6f", locationData.longitude)}")
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Handle extraction errors gracefully
        onMetadataLoaded(null)
    } finally {
        onLoadingChanged(false)
    }
}

private fun getProjectName(
    photo: Photo,
    extractedMetadata: CaptureMetadata?,
    context: android.content.Context
): String {
    return when {
        // First, try project name from EXIF metadata
        extractedMetadata?.projectName?.isNotBlank() == true ->
            extractedMetadata.projectName
        
        // Second, try to look up project name using projectId from Photo entity
        photo.projectId?.isNotBlank() == true -> {
            try {
                val metadataSettings = MetadataSettingsManager(context)
                val projectsList = metadataSettings.projectsList.value
                projectsList.find { it.projectId == photo.projectId }?.projectName
                    ?.takeIf { it.isNotBlank() }
                    ?: photo.projectId!! // Fall back to showing projectId if name not found
            } catch (e: Exception) {
                photo.projectId!! // Fall back to showing projectId if lookup fails
            }
        }
        
        // Third, try to use current project from MetadataSettingsManager if no project info found
        else -> {
            try {
                val metadataSettings = MetadataSettingsManager(context)
                val currentProject = metadataSettings.currentProject.value
                if (currentProject.projectName.isNotBlank()) {
                    currentProject.projectName
                } else {
                    "No project assigned"
                }
            } catch (e: Exception) {
                "No project assigned"
            }
        }
    }
}

private fun getLocationValue(
    photo: Photo,
    extractedMetadata: CaptureMetadata?,
    locationAddress: String?,
    showGPSCoordinates: Boolean,
    isLoadingMetadata: Boolean
): String {
    return when {
        isLoadingMetadata -> "Loading location..."
        showGPSCoordinates -> {
            // User prefers coordinates - show raw GPS data
            when {
                photo.location != null -> "${String.format("%.6f", photo.location!!.latitude)}, ${String.format("%.6f", photo.location!!.longitude)}"
                extractedMetadata?.locationData?.isAvailable == true -> {
                    val loc = extractedMetadata.locationData
                    "${String.format("%.6f", loc.latitude)}, ${String.format("%.6f", loc.longitude)}"
                }
                else -> "Location unavailable"
            }
        }
        else -> {
            // User prefers address - show geocoded address or fallback to coordinates
            when {
                locationAddress != null -> locationAddress
                photo.location != null -> "${String.format("%.6f", photo.location!!.latitude)}, ${String.format("%.6f", photo.location!!.longitude)}"
                extractedMetadata?.locationData?.isAvailable == true -> {
                    val loc = extractedMetadata.locationData
                    "${String.format("%.6f", loc.latitude)}, ${String.format("%.6f", loc.longitude)}"
                }
                else -> "Location unavailable"
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}