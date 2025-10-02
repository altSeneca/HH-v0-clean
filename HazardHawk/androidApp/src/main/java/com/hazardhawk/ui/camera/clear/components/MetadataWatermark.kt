package com.hazardhawk.ui.camera.clear.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hazardhawk.ui.camera.clear.theme.ClearDesignTokens

/**
 * MetadataWatermark - Live Metadata Overlay (Matches Photo Burn-in Format EXACTLY)
 *
 * Displays live metadata that matches the format burned into photos:
 * Line 1: Company | Timestamp
 * Line 2: Project
 * Line 3: GPS Coordinates or address (optional - controlled by settings)
 * Line 4: "Taken with HazardHawk" (optional - controlled by settings)
 *
 * Design: White bold text on semi-transparent black card
 * Matches MetadataEmbedder watermark format exactly - same spacing, same padding, same layout
 */
@Composable
fun MetadataWatermark(
    companyName: String,
    projectName: String,
    location: String,
    timestamp: String,
    showLocation: Boolean = true,
    showBranding: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Match photo burn-in: semi-transparent black background, same padding
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.47f) // 120/255 ≈ 0.47 - matches MetadataEmbedder
        ),
        shape = RoundedCornerShape(8.dp) // Subtle rounding
    ) {
        Column(
            modifier = Modifier
                .padding(
                    horizontal = 20.dp, // WATERMARK_PADDING from MetadataEmbedder
                    vertical = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(2.dp), // Tighter spacing to match photo
            horizontalAlignment = Alignment.Start
        ) {
            // Line 1: Company name | Timestamp
            Text(
                text = "$companyName | $timestamp",
                color = Color.White,
                fontSize = ClearDesignTokens.Typography.SmallText,
                fontWeight = FontWeight.Bold,
                lineHeight = ClearDesignTokens.Typography.SmallText * 1.3f,
                modifier = Modifier.shadow(
                    elevation = 6.dp,
                    spotColor = Color.Black,
                    ambientColor = Color.Black
                )
            )

            // Line 2: Project name
            Text(
                text = projectName,
                color = Color.White,
                fontSize = ClearDesignTokens.Typography.SmallText,
                fontWeight = FontWeight.Bold,
                lineHeight = ClearDesignTokens.Typography.SmallText * 1.3f,
                modifier = Modifier.shadow(
                    elevation = 6.dp,
                    spotColor = Color.Black,
                    ambientColor = Color.Black
                )
            )

            // Line 3: GPS location (optional - controlled by showLocation)
            if (showLocation && location != "Location unavailable") {
                Text(
                    text = location,
                    color = Color.White,
                    fontSize = ClearDesignTokens.Typography.SmallText,
                    fontWeight = FontWeight.Bold,
                    lineHeight = ClearDesignTokens.Typography.SmallText * 1.3f,
                    modifier = Modifier.shadow(
                        elevation = 6.dp,
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    )
                )
            }

            // Line 4: "Taken with HazardHawk" (optional - controlled by showBranding)
            if (showBranding) {
                Text(
                    text = "Taken with HazardHawk",
                    color = Color.White,
                    fontSize = ClearDesignTokens.Typography.SmallText,
                    fontWeight = FontWeight.Bold,
                    lineHeight = ClearDesignTokens.Typography.SmallText * 1.3f,
                    modifier = Modifier.shadow(
                        elevation = 6.dp,
                        spotColor = Color.Black,
                        ambientColor = Color.Black
                    )
                )
            }
        }
    }
}
