package com.hazardhawk.ui.safety.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hazardhawk.domain.models.ptp.SignatureData
import java.io.ByteArrayOutputStream

/**
 * Signature Capture Component
 *
 * Allows users to either:
 * 1. Draw their signature with finger/stylus
 * 2. Type their name for text-based signature
 *
 * Construction-friendly design with large touch targets and high contrast.
 */
@Composable
fun SignatureCaptureComponent(
    onSignatureCaptured: (SignatureData) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var signatureMode by remember { mutableStateOf(SignatureMode.DRAW) }
    var supervisorName by remember { mutableStateOf("") }
    var drawingPaths by remember { mutableStateOf(listOf<DrawPath>()) }
    var currentPath by remember { mutableStateOf<DrawPath?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Supervisor Signature",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, "Cancel")
                }
            }

            // Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = signatureMode == SignatureMode.DRAW,
                    onClick = { signatureMode = SignatureMode.DRAW },
                    label = { Text("Draw") },
                    leadingIcon = {
                        Icon(Icons.Default.Draw, null)
                    },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = signatureMode == SignatureMode.TYPE,
                    onClick = { signatureMode = SignatureMode.TYPE },
                    label = { Text("Type") },
                    leadingIcon = {
                        Icon(Icons.Default.TextFields, null)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            // Supervisor Name Input
            OutlinedTextField(
                value = supervisorName,
                onValueChange = { supervisorName = it },
                label = { Text("Supervisor Name") },
                placeholder = { Text("Enter full name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                leadingIcon = {
                    Icon(Icons.Default.Person, null)
                }
            )

            // Signature Canvas (for DRAW mode)
            if (signatureMode == SignatureMode.DRAW) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Draw your signature below",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SignatureCanvas(
                        paths = drawingPaths,
                        currentPath = currentPath,
                        onPathsChange = { paths ->
                            drawingPaths = paths
                        },
                        onCurrentPathChange = { path ->
                            currentPath = path
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )

                    // Clear Button for Canvas
                    if (drawingPaths.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Clear, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Signature")
                        }
                    }
                }
            }

            // Typed Signature Preview (for TYPE mode)
            if (signatureMode == SignatureMode.TYPE && supervisorName.isNotBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Signature Preview",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = MaterialTheme.shapes.medium
                            )
                            .background(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.shapes.medium
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            supervisorName,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val signatureData = when (signatureMode) {
                            SignatureMode.DRAW -> {
                                // Convert paths to bitmap and then to ByteArray
                                val signatureBlob = convertPathsToByteArray(drawingPaths)
                                SignatureData(
                                    supervisorName = supervisorName,
                                    signatureDate = System.currentTimeMillis(),
                                    signatureBlob = signatureBlob
                                )
                            }
                            SignatureMode.TYPE -> {
                                SignatureData(
                                    supervisorName = supervisorName,
                                    signatureDate = System.currentTimeMillis(),
                                    signatureBlob = null // Text-based signature
                                )
                            }
                        }
                        onSignatureCaptured(signatureData)
                    },
                    enabled = supervisorName.isNotBlank() &&
                             (signatureMode == SignatureMode.TYPE || drawingPaths.isNotEmpty()),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm")
                }
            }
        }
    }

    // Clear Confirmation Dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear Signature?") },
            text = { Text("This will remove your drawn signature. You'll need to draw it again.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        drawingPaths = emptyList()
                        currentPath = null
                        showClearConfirmation = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Canvas for drawing signature
 */
@Composable
private fun SignatureCanvas(
    paths: List<DrawPath>,
    currentPath: DrawPath?,
    onPathsChange: (List<DrawPath>) -> Unit,
    onCurrentPathChange: (DrawPath?) -> Unit,
    modifier: Modifier = Modifier
) {
    val strokeColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        onCurrentPathChange(DrawPath(color = strokeColor))
                    },
                    onDrag = { change, _ ->
                        val newPath = currentPath?.copy(
                            points = currentPath.points + change.position
                        )
                        onCurrentPathChange(newPath)
                    },
                    onDragEnd = {
                        currentPath?.let { path ->
                            onPathsChange(paths + path)
                            onCurrentPathChange(null)
                        }
                    }
                )
            }
    ) {
        // Draw X at signature line (construction convention)
        val lineY = size.height - 40f
        drawLine(
            color = Color.Gray,
            start = Offset(40f, lineY),
            end = Offset(size.width - 40f, lineY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        // Draw all completed paths
        paths.forEach { path ->
            if (path.points.size > 1) {
                val pathToDraw = Path().apply {
                    moveTo(path.points.first().x, path.points.first().y)
                    path.points.forEach { point ->
                        lineTo(point.x, point.y)
                    }
                }
                drawPath(
                    path = pathToDraw,
                    color = path.color,
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // Draw current path being drawn
        currentPath?.let { path ->
            if (path.points.size > 1) {
                val pathToDraw = Path().apply {
                    moveTo(path.points.first().x, path.points.first().y)
                    path.points.forEach { point ->
                        lineTo(point.x, point.y)
                    }
                }
                drawPath(
                    path = pathToDraw,
                    color = path.color,
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/**
 * Represents a drawn path on the signature canvas
 */
data class DrawPath(
    val points: List<Offset> = emptyList(),
    val color: Color
)

/**
 * Signature capture mode
 */
enum class SignatureMode {
    DRAW,
    TYPE
}

/**
 * Convert drawing paths to ByteArray for storage
 * In production, this should create a bitmap and compress to PNG/JPEG
 */
private fun convertPathsToByteArray(paths: List<DrawPath>): ByteArray {
    // Simplified implementation - in production, render to bitmap
    // For now, we'll serialize the path data as a simple format
    val stream = ByteArrayOutputStream()

    // TODO: Implement proper bitmap rendering
    // This is a placeholder that stores basic path data
    paths.forEach { path ->
        path.points.forEach { point ->
            stream.write("${point.x},${point.y};".toByteArray())
        }
        stream.write("\n".toByteArray())
    }

    return stream.toByteArray()
}
