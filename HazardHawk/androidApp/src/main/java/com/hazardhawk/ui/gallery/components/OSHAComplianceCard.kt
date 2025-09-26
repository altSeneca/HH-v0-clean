package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hazardhawk.models.OSHAAnalysisResult

// Construction-safe colors
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)

/**
 * Reusable OSHA Compliance Card Component
 * Simplified and focused on OSHA compliance display
 */
@Composable
fun OSHAComplianceCard(
    oshaAnalysis: OSHAAnalysisResult?,
    isLoadingOSHA: Boolean,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with analyze button
            OSHAComplianceHeader(
                isLoadingOSHA = isLoadingOSHA,
                onAnalyze = onAnalyze
            )
            
            // Analysis results or default codes
            OSHAComplianceContent(
                oshaAnalysis = oshaAnalysis,
                isLoadingOSHA = isLoadingOSHA
            )
        }
    }
}

@Composable
private fun OSHAComplianceHeader(
    isLoadingOSHA: Boolean,
    onAnalyze: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Gavel,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "OSHA Standards",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Button(
            onClick = onAnalyze,
            enabled = !isLoadingOSHA,
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
            modifier = Modifier.height(36.dp)
        ) {
            if (isLoadingOSHA) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Analyze", fontSize = 12.sp)
        }
    }
}

@Composable
private fun OSHAComplianceContent(
    oshaAnalysis: OSHAAnalysisResult?,
    isLoadingOSHA: Boolean
) {
    when {
        oshaAnalysis != null -> {
            OSHAViolationsList(oshaAnalysis = oshaAnalysis)
        }
        
        !isLoadingOSHA -> {
            DefaultOSHACodes()
        }
    }
}

@Composable
private fun OSHAViolationsList(oshaAnalysis: OSHAAnalysisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (oshaAnalysis.oshaViolations.isEmpty()) {
            Text(
                text = "No OSHA violations detected for this photo",
                style = MaterialTheme.typography.bodyMedium,
                color = SafetyGreen,
                fontWeight = FontWeight.Medium
            )
        } else {
            oshaAnalysis.oshaViolations.forEach { violation ->
                OSHACodeItem(
                    code = violation.oshaStandard.substringAfter("CFR ").substringBefore("("),
                    title = violation.standardTitle,
                    description = violation.description,
                    violationType = violation.violationType.name,
                    penalty = violation.potentialPenalty
                )
            }
        }
    }
}

@Composable
private fun DefaultOSHACodes() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OSHACodeItem(
            code = "1926.95",
            title = "Personal Protective Equipment",
            description = "Requirements for hard hats and protective equipment on construction sites"
        )
        
        OSHACodeItem(
            code = "1926.951",
            title = "High-Visibility Safety Apparel",
            description = "Workers shall wear high-visibility safety apparel when working in areas with vehicular traffic"
        )
        
        Text(
            text = "Tap 'Analyze' to get relevant OSHA standards for this photo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )
    }
}

@Composable
fun OSHACodeItem(
    code: String,
    title: String,
    description: String,
    violationType: String? = null,
    penalty: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (violationType) {
                "SERIOUS" -> DangerRed.copy(alpha = 0.1f)
                "OTHER_THAN_SERIOUS" -> SafetyOrange.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            when (violationType) {
                "SERIOUS" -> DangerRed
                "OTHER_THAN_SERIOUS" -> SafetyOrange
                else -> MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OSHA $code",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )
                
                violationType?.let {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (it) {
                            "SERIOUS" -> DangerRed
                            "OTHER_THAN_SERIOUS" -> SafetyOrange
                            else -> MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Text(
                            text = it.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            penalty?.let {
                Text(
                    text = "Potential Penalty: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = DangerRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}