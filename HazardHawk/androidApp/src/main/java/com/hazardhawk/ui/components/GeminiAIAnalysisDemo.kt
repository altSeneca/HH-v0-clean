package com.hazardhawk.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazardhawk.ai.GeminiVisionAnalyzer
import com.hazardhawk.ai.GeminiSafetyAnalysisAdapter
import com.hazardhawk.ai.PhotoAnalysisWithTags
import com.hazardhawk.domain.entities.WorkType
import com.hazardhawk.security.PhotoEncryptionService
import com.hazardhawk.security.SecureStorageService
import kotlinx.coroutines.launch

/**
 * Demonstration component showing the new Gemini AI analysis capabilities
 * with real-time bounding box overlays and OSHA compliance detection.
 */
@Composable
fun GeminiAIAnalysisDemo(
    secureStorage: SecureStorageService,
    encryptionService: PhotoEncryptionService,
    modifier: Modifier = Modifier
) {
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<PhotoAnalysisWithTags?>(null) }
    val adapter = remember { GeminiSafetyAnalysisAdapter() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val geminiAnalyzer = remember {
        GeminiVisionAnalyzer(secureStorage, encryptionService)
    }
    
    LaunchedEffect(Unit) {
        geminiAnalyzer.initialize()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🤖 Enhanced Gemini AI Analysis",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real AI responses with bounding box overlays for OSHA compliance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
        
        // API Key Status
        Card {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔑 API Connection Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val isAvailable = geminiAnalyzer.isServiceAvailable
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isAvailable) {
                            androidx.compose.material.icons.Icons.Default.CheckCircle
                        } else {
                            androidx.compose.material.icons.Icons.Default.Error
                        },
                        contentDescription = null,
                        tint = if (isAvailable) Color.Green else Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAvailable) "✅ Gemini API Connected" else "❌ API Key Required",
                        color = if (isAvailable) Color.Green else Color.Red
                    )
                }
                
                if (!isAvailable) {
                    Text(
                        text = "Add your Gemini API key to Settings to see real AI analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
        
        // Demo Analysis Button
        Button(
            onClick = {
                scope.launch {
                    isAnalyzing = true
                    errorMessage = null
                    try {
                        // Demo with sample construction image data
                        val sampleImageData = ByteArray(1024) { 0 } // Placeholder
                        
                        analysisResult = geminiAnalyzer.analyzePhotoWithTags(
                            data = sampleImageData,
                            width = 1920,
                            height = 1080,
                            workType = WorkType.GENERAL_CONSTRUCTION
                        )
                        
                    } catch (e: Exception) {
                        errorMessage = "Analysis failed: ${e.message}"
                    } finally {
                        isAnalyzing = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAnalyzing
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyzing with Gemini AI...")
            } else {
                Text("🔍 Demo AI Safety Analysis")
            }
        }
        
        // Error Display
        errorMessage?.let { error ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        // Analysis Results
        analysisResult?.let { analysis ->
            val hazardSummary = adapter.generateHazardSummary(analysis.hazardDetections)
            val recommendations = adapter.extractRecommendations(analysis.recommendedTags)
            
            Card {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "📊 Enhanced AI Analysis Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Key Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatChip(
                            label = "Hazards",
                            value = (hazardSummary["total_hazards"] as Int).toString(),
                            color = if ((hazardSummary["total_hazards"] as Int) > 0) MaterialTheme.colorScheme.error else Color.Green
                        )
                        StatChip(
                            label = "Confidence",
                            value = "${((hazardSummary["overall_confidence"] as Float) * 100).toInt()}%",
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatChip(
                            label = "Processing",
                            value = "${analysis.processingTimeMs}ms",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Hazards List
                    if (analysis.hazardDetections.isNotEmpty()) {
                        Text(
                            text = "⚠️ Detected Hazards with Coordinates:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.heightIn(max = 200.dp)
                        ) {
                            items(analysis.hazardDetections) { hazard ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = hazard.hazardType.name.replace("_", " "),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Surface(
                                                color = getSeverityColor(hazard.severity),
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = hazard.severity.name,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                        
                                        Text(
                                            text = adapter.getHazardDescription(hazard.hazardType),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        
                                        adapter.getOSHAReference(hazard.hazardType)?.let { oshaCode ->
                                            Text(
                                                text = "OSHA: $oshaCode",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        Text(
                                            text = "📍 Location: (${(hazard.boundingBox.x * 100).toInt()}%, ${(hazard.boundingBox.y * 100).toInt()}%) - Confidence: ${(hazard.boundingBox.confidence * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "✅ No hazards detected in this analysis",
                            color = Color.Green
                        )
                    }
                    
                    // Recommendations  
                    if (recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "💡 AI Recommendations:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        recommendations.take(5).forEach { recommendation ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = "• $recommendation",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    
                    // Tags Summary
                    if (analysis.recommendedTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "🏷️ Analysis Tags (${analysis.recommendedTags.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = analysis.recommendedTags.joinToString(", "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

private fun getSeverityColor(severity: com.hazardhawk.models.Severity): Color {
    return when (severity) {
        com.hazardhawk.models.Severity.CRITICAL -> Color(0xFFD32F2F)
        com.hazardhawk.models.Severity.HIGH -> Color(0xFFF57C00)
        com.hazardhawk.models.Severity.MEDIUM -> Color(0xFFFBC02D)
        com.hazardhawk.models.Severity.LOW -> Color(0xFF388E3C)
    }
}