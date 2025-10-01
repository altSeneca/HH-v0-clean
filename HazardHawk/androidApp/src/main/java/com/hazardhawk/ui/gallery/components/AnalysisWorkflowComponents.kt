package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazardhawk.ui.gallery.state.*
import com.hazardhawk.models.OSHAAnalysisResult

// Construction-safe colors
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)

/**
 * Enhanced OSHA Compliance Card with display state support
 */
@Composable
fun OSHAComplianceCard(
    oshaAnalysis: OSHAAnalysisResult?,
    isLoadingOSHA: Boolean,
    displayState: OSHADisplayState,
    onAnalyze: () -> Unit,
    onToggleVisibility: (Boolean) -> Unit,
    onExpandStandard: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OSHA Compliance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(
                        onClick = { onToggleVisibility(!displayState.isVisible) }
                    ) {
                        Icon(
                            imageVector = if (displayState.isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (displayState.isVisible) "Hide details" else "Show details"
                        )
                    }

                    if (oshaAnalysis == null) {
                        Button(
                            onClick = onAnalyze,
                            enabled = !isLoadingOSHA
                        ) {
                            if (isLoadingOSHA) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Analyze")
                            }
                        }
                    }
                }
            }

            if (displayState.isVisible) {
                when {
                    isLoadingOSHA -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Analyzing OSHA compliance...")
                        }
                    }

                    oshaAnalysis != null -> {
                        OSHAAnalysisResults(
                            analysis = oshaAnalysis,
                            displayState = displayState,
                            onExpandStandard = onExpandStandard
                        )
                    }

                    else -> {
                        Text(
                            text = "Run OSHA compliance analysis to check for regulatory violations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OSHAAnalysisResults(
    analysis: OSHAAnalysisResult,
    displayState: OSHADisplayState,
    onExpandStandard: (String, Boolean) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compliance overview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Compliance Status:",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = analysis.overallCompliance.name.replace("_", " "),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = when (analysis.overallCompliance) {
                    com.hazardhawk.models.ComplianceStatus.COMPLIANT -> SafetyGreen
                    com.hazardhawk.models.ComplianceStatus.MINOR_VIOLATIONS -> SafetyOrange
                    else -> DangerRed
                }
            )
        }

        // Compliance score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Compliance Score:",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${analysis.complianceScore.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Violations
        if (analysis.oshaViolations.isNotEmpty()) {
            Text(
                text = "Violations Found: ${analysis.oshaViolations.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = DangerRed,
                fontWeight = FontWeight.Bold
            )

            analysis.oshaViolations.forEach { violation ->
                ViolationCard(
                    violation = violation,
                    isExpanded = displayState.expandedStandards.contains(violation.oshaStandard),
                    onToggleExpanded = { expanded ->
                        onExpandStandard(violation.oshaStandard, expanded)
                    }
                )
            }
        }

        // Safety hazards
        if (analysis.safetyHazards.isNotEmpty()) {
            Text(
                text = "Safety Hazards: ${analysis.safetyHazards.size}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(analysis.safetyHazards) { hazard ->
                    FilterChip(
                        onClick = {
                            val isExpanded = displayState.expandedStandards.contains(hazard.oshaStandard)
                            onExpandStandard(hazard.oshaStandard, !isExpanded)
                        },
                        label = { Text(hazard.hazardType.name.replace("_", " "), style = MaterialTheme.typography.bodySmall) },
                        selected = displayState.expandedStandards.contains(hazard.oshaStandard)
                    )
                }
            }
        }

        // Recommendations
        if (analysis.recommendations.isNotEmpty()) {
            Text(
                text = "Recommendations: ${analysis.recommendations.size}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ViolationCard(
    violation: com.hazardhawk.models.OSHAViolation,
    isExpanded: Boolean,
    onToggleExpanded: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = violation.oshaStandard,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = violation.violationType.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed
                    )
                }

                IconButton(
                    onClick = { onToggleExpanded(!isExpanded) }
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = violation.description,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Corrective Action:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• ${violation.correctiveAction}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )

                violation.potentialPenalty?.let { penalty ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Potential Penalty: $penalty",
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Progress indicator showing the current step and overall progress
 */
@Composable
fun AnalysisProgressIndicator(
    progress: AnalysisProgress,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analysis Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${progress.completedSteps}/${progress.totalSteps}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            LinearProgressIndicator(
                progress = progress.overallProgress,
                modifier = Modifier.fillMaxWidth()
            )

            progress.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (progress.estimatedTimeRemainingMs > 0) {
                Text(
                    text = "Estimated time remaining: ${progress.estimatedTimeRemainingMs / 1000}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Manual tag category selector with predefined options
 */
@Composable
fun HazardCategorySelector(
    categories: List<HazardTagCategory>,
    selectedCategory: HazardTagCategory,
    onCategorySelected: (HazardTagCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            FilterChip(
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                selected = category == selectedCategory,
                leadingIcon = {
                    Icon(
                        imageVector = getCategoryIcon(category),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            )
        }
    }
}

/**
 * Get icon for hazard category
 */
fun getCategoryIcon(category: HazardTagCategory) = when (category) {
    HazardTagCategory.PPE_VIOLATION -> Icons.Default.Security
    HazardTagCategory.FALL_HAZARD -> Icons.Default.Height
    HazardTagCategory.ELECTRICAL -> Icons.Default.Bolt
    HazardTagCategory.MACHINERY -> Icons.Default.Settings
    HazardTagCategory.CHEMICAL -> Icons.Default.Science
    HazardTagCategory.STRUCTURAL -> Icons.Default.Foundation
    HazardTagCategory.ENVIRONMENTAL -> Icons.Default.Cloud
    HazardTagCategory.GENERAL_SAFETY -> Icons.Default.Shield
    HazardTagCategory.CUSTOM -> Icons.Default.Add
}

/**
 * Analysis phase transition controls
 */
@Composable
fun PhaseTransitionControls(
    currentPhase: AnalysisPhase,
    canTransitionToAI: Boolean,
    hasAIResults: Boolean,
    onTransitionToPreAnalysis: () -> Unit,
    onTransitionToPostAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (currentPhase) {
            AnalysisPhase.PRE_ANALYSIS -> {
                Button(
                    onClick = onTransitionToPostAnalysis,
                    enabled = canTransitionToAI,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start AI Analysis")
                }
            }

            AnalysisPhase.POST_ANALYSIS -> {
                OutlinedButton(
                    onClick = onTransitionToPreAnalysis,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back to Manual")
                }

                if (hasAIResults) {
                    Button(
                        onClick = onTransitionToPostAnalysis,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Re-analyze")
                    }
                }
            }
        }
    }
}

/**
 * Workflow summary card showing key metrics
 */
@Composable
fun WorkflowSummaryCard(
    manualTagCount: Int,
    aiHazardCount: Int,
    criticalHazardCount: Int,
    completeness: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Analysis Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryMetric(
                    value = manualTagCount.toString(),
                    label = "Manual Tags",
                    icon = Icons.Default.Edit
                )

                SummaryMetric(
                    value = aiHazardCount.toString(),
                    label = "AI Detected",
                    icon = Icons.Default.Psychology
                )

                SummaryMetric(
                    value = criticalHazardCount.toString(),
                    label = "Critical",
                    icon = Icons.Default.Warning,
                    isHighlight = criticalHazardCount > 0
                )
            }

            // Completeness bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completeness",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${(completeness * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = completeness,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SummaryMetric(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isHighlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isHighlight) DangerRed else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) DangerRed else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}