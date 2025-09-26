package com.hazardhawk.ui.gallery.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.hazardhawk.domain.entities.Photo

// Construction-safe colors
private val SafetyOrange = Color(0xFFFF6B35)
private val SafetyGreen = Color(0xFF10B981)
private val DangerRed = Color(0xFFEF4444)

/**
 * Reusable Safety Tag Selector Component
 * Provides manual hazard tagging functionality
 */
@Composable
fun SafetyTagSelector(
    photo: Photo,
    onTagClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Manual Safety Assessment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Quick hazard buttons for immediate tagging:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Quick hazard buttons
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    getQuickHazardOptions()
                ) { (hazard, color) ->
                    QuickHazardChip(
                        hazard = hazard,
                        color = color,
                        isAlreadyAdded = photo.tags.contains(hazard.lowercase().replace(" ", "-")),
                        onTagClick = {
                            val tagName = hazard.lowercase().replace(" ", "-")
                            if (!photo.tags.contains(tagName)) {
                                onTagClick(tagName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickHazardChip(
    hazard: String,
    color: Color,
    isAlreadyAdded: Boolean,
    onTagClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isAlreadyAdded) color.copy(alpha = 0.2f) else color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color),
        modifier = modifier
            .clickable { if (!isAlreadyAdded) onTagClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAlreadyAdded) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Already added",
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = hazard,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun getQuickHazardOptions(): List<Pair<String, Color>> {
    return listOf(
        "No Hard Hat" to DangerRed,
        "Fall Risk" to Color(0xFFB71C1C),
        "Trip Hazard" to SafetyOrange,
        "Electrical" to DangerRed,
        "No Safety Vest" to SafetyOrange,
        "Unsafe Ladder" to DangerRed,
        "Exposed Wiring" to Color(0xFFB71C1C),
        "No Eye Protection" to SafetyOrange,
        "Heavy Machinery" to Color(0xFFB71C1C),
        "Confined Space" to DangerRed
    )
}