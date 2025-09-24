/*
 * Copyright (c) 2025 HazardHawk Safety Platform
 *
 * AI Analysis Error Handling - Minimal stub for compilation
 */
package com.hazardhawk.ui.gallery

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * AI Analysis Error Types - Simplified stub
 */
enum class AIAnalysisError {
    UNKNOWN_ERROR,
    MODEL_LOAD_FAILED,
    PROCESSING_ERROR,
    NETWORK_ERROR,
    PERMISSION_DENIED,
    INSUFFICIENT_MEMORY,
    TIMEOUT_ERROR
}

enum class ErrorSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Simple AI Analysis Error Dialog
 */
@Composable
fun AIAnalysisErrorDialog(
    error: AIAnalysisError,
    failedPhotos: List<File> = emptyList(),
    onRetry: () -> Unit,
    onFallbackToManual: () -> Unit,
    onDismiss: () -> Unit,
    onReduceBatchSize: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "AI Analysis Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = when (error) {
                    AIAnalysisError.MODEL_LOAD_FAILED -> "Failed to load AI model"
                    AIAnalysisError.PROCESSING_ERROR -> "Error processing image"
                    AIAnalysisError.NETWORK_ERROR -> "Network connection issue"
                    AIAnalysisError.PERMISSION_DENIED -> "Permission denied"
                    AIAnalysisError.INSUFFICIENT_MEMORY -> "Insufficient memory"
                    AIAnalysisError.TIMEOUT_ERROR -> "Analysis timed out"
                    AIAnalysisError.UNKNOWN_ERROR -> "Unknown error occurred"
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF6B35)
                )
            ) {
                Text("Retry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Additional stubs for camera integration
data class AIAnalysisResults(
    val hazardsFound: Int = 0,
    val processingTimeMs: Long = 0L,
    val topRecommendation: String = "",
    val confidence: Float = 0.0f,
    val recommendedTags: List<String> = emptyList(),
    val oshaReferences: List<String> = emptyList(),
    val results: List<AnalysisResult> = emptyList()
)

data class AnalysisResult(
    val id: String,
    val type: String,
    val description: String,
    val confidence: Float,
    val severity: String
)

data class AIProgressInfo(
    val status: AIAnalysisStatus = AIAnalysisStatus.IDLE,
    val progress: Float = 0.0f,
    val currentStep: String = "",
    val estimatedTimeMs: Long = 0L,
    val detectedHazards: Int = 0,
    val confidenceScore: Float = 0.0f,
    val processingTimeMs: Long = 0L
)

enum class AIAnalysisStatus {
    IDLE, INITIALIZING, PROCESSING, ANALYZING, COMPLETED, ERROR
}

@Composable
fun CompactAIIndicator(
    status: AIAnalysisStatus,
    progress: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(32.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        when (status) {
            AIAnalysisStatus.IDLE -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Ready",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(24.dp)
                )
            }
            AIAnalysisStatus.INITIALIZING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFFFF6B35)
                )
            }
            AIAnalysisStatus.PROCESSING -> {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFFFF6B35)
                )
            }
            AIAnalysisStatus.ANALYZING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF059669)
                )
            }
            AIAnalysisStatus.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Complete",
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(24.dp)
                )
            }
            AIAnalysisStatus.ERROR -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AIErrorRecoveryCard(
    error: AIAnalysisError,
    onRetryAI: () -> Unit,
    onProceedManually: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFDC2626).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Error",
                    tint = Color(0xFFDC2626)
                )
                Text(
                    text = "AI Analysis Failed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = when (error) {
                    AIAnalysisError.MODEL_LOAD_FAILED -> "Failed to load AI model"
                    AIAnalysisError.PROCESSING_ERROR -> "Error processing image"
                    AIAnalysisError.NETWORK_ERROR -> "Network connection issue"
                    AIAnalysisError.PERMISSION_DENIED -> "Permission denied"
                    AIAnalysisError.INSUFFICIENT_MEMORY -> "Insufficient memory"
                    AIAnalysisError.TIMEOUT_ERROR -> "Analysis timed out"
                    AIAnalysisError.UNKNOWN_ERROR -> "Unknown error occurred"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onRetryAI,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retry AI")
                }
                
                Button(
                    onClick = onProceedManually,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Manual")
                }
                
                IconButton(
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss"
                    )
                }
            }
        }
    }
}

@Composable
fun AIConfidenceBadge(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFFF6B35).copy(alpha = 0.2f)
    ) {
        Text(
            text = "${(confidence * 100).toInt()}%",
            modifier = Modifier.padding(4.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun AIResultsHeroCard(
    hazardsFound: Int,
    processingTime: String,
    topRecommendation: String,
    confidence: Float,
    results: List<AnalysisResult> = emptyList(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF6B35).copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with AI badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Analysis Results",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                AIConfidenceBadge(confidence = confidence)
            }
            
            // Hazards found and processing time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$hazardsFound",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hazardsFound > 0) Color(0xFFDC2626) else Color(0xFF059669)
                    )
                    Text(
                        text = "Hazards Found",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = processingTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Processing Time",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Top recommendation
            if (topRecommendation.isNotEmpty()) {
                Column {
                    Text(
                        text = "Top Recommendation",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = topRecommendation,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun AIRecommendationToggle(
    aiRecommendationsCount: Int,
    genericCount: Int,
    showingAI: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // AI Recommendations button
        Button(
            onClick = { onToggle(true) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showingAI) Color(0xFFFF6B35) else Color.Transparent,
                contentColor = if (showingAI) Color.White else Color(0xFF6B7280)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "AI ($aiRecommendationsCount)",
                style = MaterialTheme.typography.labelMedium
            )
        }
        
        // Generic tags button
        Button(
            onClick = { onToggle(false) },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!showingAI) Color(0xFFFF6B35) else Color.Transparent,
                contentColor = if (!showingAI) Color.White else Color(0xFF6B7280)
            ),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "All ($genericCount)",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}


/**
 * Enhanced Local AI Progress Indicator with detailed status information
 */
@Composable
fun LocalAIProgressIndicator(
    isAnalyzing: Boolean,
    progress: Float = 0f,
    statusMessage: String = "",
    backendInfo: String? = null,
    elapsedTime: Long = 0L,
    estimatedRemaining: Long? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isAnalyzing,
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.8f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header with icon and title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (progress > 0f) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF6B35)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFFFF6B35)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Local AI Analysis",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        if (backendInfo != null) {
                            Text(
                                text = "Using \$backendInfo backend",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                    
                    if (progress > 0f) {
                        Text(
                            text = "\${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B35)
                            )
                        )
                    }
                }
                
                // Progress bar
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFFFF6B35),
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
                
                // Status message
                if (statusMessage.isNotEmpty()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }
                
                // Timing information
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (elapsedTime > 0) {
                        Text(
                            text = "Elapsed: \${elapsedTime / 1000.0}s",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                    
                    if (estimatedRemaining != null && estimatedRemaining > 0) {
                        Text(
                            text = "~\${estimatedRemaining / 1000.0}s remaining",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
    }
}

