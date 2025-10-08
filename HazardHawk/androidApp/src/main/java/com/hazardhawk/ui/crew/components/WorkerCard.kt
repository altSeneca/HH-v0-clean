package com.hazardhawk.ui.crew.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.hazardhawk.models.crew.CompanyWorker
import com.hazardhawk.models.crew.WorkerCertification
import com.hazardhawk.models.crew.WorkerProfile
import com.hazardhawk.models.crew.WorkerRole
import com.hazardhawk.models.crew.WorkerStatus
import com.hazardhawk.ui.theme.HazardHawkTheme
import kotlinx.datetime.LocalDate

/**
 * Worker Card Component
 *
 * A Material 3 card displaying worker information with photo, name, role, and certifications.
 *
 * Features:
 * - 48dp circular profile photo with placeholder
 * - Worker name, employee number, and role
 * - Up to 3 certification badges with "+X" overflow indicator
 * - Status indicator (active/inactive/terminated)
 * - Long-press gesture support
 * - Accessibility annotations
 * - Loading/error states
 *
 * Design System:
 * - 16dp padding
 * - 2dp elevation
 * - Material 3 surface colors
 * - High contrast for construction environments
 * - 60dp minimum touch target (entire card)
 *
 * @param worker CompanyWorker data with embedded profile and certifications
 * @param showCertifications Whether to display certification badges
 * @param onClick Action when card is tapped
 * @param onLongClick Action when card is long-pressed
 * @param modifier Modifier for customization
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkerCard(
    worker: CompanyWorker,
    showCertifications: Boolean = true,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val workerName = worker.workerProfile?.fullName ?: "Unknown Worker"
    val statusColor = when (worker.status) {
        WorkerStatus.ACTIVE -> Color(0xFF4CAF50)
        WorkerStatus.INACTIVE -> Color(0xFFFFA500)
        WorkerStatus.TERMINATED -> Color(0xFFF44336)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp) // Minimum touch target
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .semantics {
                contentDescription = "Worker card for $workerName, ${worker.role.displayName}, " +
                        "${worker.status.name.lowercase()} status"
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Worker Photo
            AsyncImage(
                model = worker.workerProfile?.photoUrl,
                contentDescription = "Photo of $workerName",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = if (worker.workerProfile?.photoUrl != null) {
                            "Profile photo of $workerName"
                        } else {
                            "Default profile photo placeholder"
                        }
                    },
                contentScale = ContentScale.Crop,
                error = {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                placeholder = {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            )

            // Worker Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Name
                Text(
                    text = workerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Employee Number and Role
                Text(
                    text = "${worker.employeeNumber} | ${worker.role.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Certifications
                if (showCertifications && worker.certifications.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        worker.certifications.take(3).forEach { cert ->
                            CertificationBadge(
                                certification = cert,
                                compact = true
                            )
                        }

                        if (worker.certifications.size > 3) {
                            Text(
                                text = "+${worker.certifications.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Status Indicator
            Icon(
                imageVector = when (worker.status) {
                    WorkerStatus.ACTIVE -> Icons.Default.CheckCircle
                    WorkerStatus.INACTIVE -> Icons.Default.Warning
                    WorkerStatus.TERMINATED -> Icons.Default.Cancel
                },
                contentDescription = "${worker.status.name.lowercase()} status",
                tint = statusColor,
                modifier = Modifier
                    .size(24.dp)
                    .semantics {
                        contentDescription = "Worker status: ${worker.status.name.lowercase()}"
                    }
            )
        }
    }
}

/**
 * Loading state for WorkerCard
 */
@Composable
fun WorkerCardLoading(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo skeleton
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {}

            // Text skeleton
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {}

                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.extraSmall
                ) {}
            }
        }
    }
}

/**
 * Empty state for worker lists
 */
@Composable
fun WorkerCardEmpty(
    message: String = "No workers found",
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// PREVIEW SECTION
// ============================================================================

@Preview(name = "Worker Card - Active", showBackground = true)
@Composable
private fun WorkerCardPreview() {
    HazardHawkTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WorkerCard(
                    worker = CompanyWorker(
                        id = "1",
                        companyId = "company1",
                        workerProfileId = "profile1",
                        employeeNumber = "W-1234",
                        role = WorkerRole.FOREMAN,
                        hireDate = LocalDate(2023, 1, 15),
                        status = WorkerStatus.ACTIVE,
                        createdAt = "",
                        updatedAt = "",
                        workerProfile = WorkerProfile(
                            id = "profile1",
                            firstName = "John",
                            lastName = "Smith",
                            email = "john.smith@example.com",
                            phone = "555-1234",
                            photoUrl = null,
                            createdAt = "",
                            updatedAt = ""
                        ),
                        certifications = emptyList()
                    )
                )

                WorkerCardLoading()

                WorkerCardEmpty()
            }
        }
    }
}
