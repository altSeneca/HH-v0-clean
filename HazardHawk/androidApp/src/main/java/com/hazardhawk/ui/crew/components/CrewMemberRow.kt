package com.hazardhawk.ui.crew.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.hazardhawk.models.crew.*
import com.hazardhawk.ui.theme.HazardHawkTheme
import kotlinx.datetime.LocalDate

/**
 * Crew Member Row Component
 *
 * A specialized row for displaying crew members within crew details or PTP documents.
 * Includes role badge and actions (remove, edit, etc.).
 *
 * Features:
 * - 36dp circular photo
 * - Worker name and employee number
 * - Crew role badge (foreman, crew lead, member)
 * - Action icons (edit, remove)
 * - Material 3 design
 * - Accessibility support
 *
 * Design System:
 * - 64dp minimum height
 * - 12dp horizontal padding
 * - No elevation (list item style)
 * - Role-specific colors
 * - High contrast
 *
 * @param crewMember CrewMember data with embedded worker
 * @param onClick Action when row is tapped
 * @param onRemove Action to remove member from crew (null to hide button)
 * @param modifier Modifier for customization
 */
@Composable
fun CrewMemberRow(
    crewMember: CrewMember,
    onClick: () -> Unit = {},
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val worker = crewMember.worker
    val workerName = worker?.workerProfile?.fullName ?: "Unknown Worker"
    val employeeNumber = worker?.employeeNumber ?: "N/A"

    // Role badge colors
    val (roleBackgroundColor, roleTextColor) = when (crewMember.role) {
        CrewMemberRole.FOREMAN -> Pair(
            Color(0xFF1976D2),
            Color.White
        )
        CrewMemberRole.CREW_LEAD -> Pair(
            Color(0xFFFF8F00),
            Color.White
        )
        CrewMemberRole.MEMBER -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics {
                contentDescription = "Crew member: $workerName, ${crewMember.role.name.lowercase()}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Worker Photo
        AsyncImage(
            model = worker?.workerProfile?.photoUrl,
            contentDescription = "Photo of $workerName",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = {
                Surface(
                    modifier = Modifier.size(36.dp),
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
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            },
            placeholder = {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        )

        // Worker Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = workerName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = employeeNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                // Role Badge
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = roleBackgroundColor
                ) {
                    Text(
                        text = crewMember.role.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = roleTextColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Remove Button
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove $workerName from crew",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Crew Member Row with certifications displayed
 */
@Composable
fun CrewMemberRowWithCerts(
    crewMember: CrewMember,
    onClick: () -> Unit = {},
    onRemove: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val worker = crewMember.worker
    val workerName = worker?.workerProfile?.fullName ?: "Unknown Worker"
    val employeeNumber = worker?.employeeNumber ?: "N/A"
    val certifications = worker?.certifications ?: emptyList()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Worker Photo
        AsyncImage(
            model = worker?.workerProfile?.photoUrl,
            contentDescription = "Photo of $workerName",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = {
                Surface(
                    modifier = Modifier.size(36.dp),
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
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        )

        // Worker Info with Certifications
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = employeeNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Certifications
            if (certifications.isNotEmpty()) {
                CertificationBadgeList(
                    certifications = certifications,
                    maxVisible = 3,
                    compact = true
                )
            } else {
                Text(
                    text = "No certifications",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Remove Button
        if (onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove from crew",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Compact Crew Member Row for dense lists
 */
@Composable
fun CrewMemberRowCompact(
    crewMember: CrewMember,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val worker = crewMember.worker
    val workerName = worker?.workerProfile?.fullName ?: "Unknown Worker"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Small Photo
        AsyncImage(
            model = worker?.workerProfile?.photoUrl,
            contentDescription = "Photo of $workerName",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            error = {
                Surface(
                    modifier = Modifier.size(32.dp),
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
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        )

        // Name only
        Text(
            text = workerName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Role indicator
        if (crewMember.role != CrewMemberRole.MEMBER) {
            Icon(
                imageVector = when (crewMember.role) {
                    CrewMemberRole.FOREMAN -> Icons.Default.Engineering
                    CrewMemberRole.CREW_LEAD -> Icons.Default.Star
                    CrewMemberRole.MEMBER -> Icons.Default.Person
                },
                contentDescription = crewMember.role.name,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Loading skeleton for CrewMemberRow
 */
@Composable
fun CrewMemberRowLoading(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Photo skeleton
        Surface(
            modifier = Modifier.size(36.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {}

        // Text skeleton
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall
            ) {}

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall
            ) {}
        }
    }
}

// ============================================================================
// PREVIEW SECTION
// ============================================================================

@Preview(name = "Crew Member Rows", showBackground = true)
@Composable
private fun CrewMemberRowPreview() {
    HazardHawkTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Crew Member Rows",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(16.dp)
                )

                CrewMemberRow(
                    crewMember = CrewMember(
                        id = "member1",
                        crewId = "crew1",
                        companyWorkerId = "worker1",
                        role = CrewMemberRole.FOREMAN,
                        startDate = LocalDate(2023, 1, 1),
                        status = "active",
                        worker = CompanyWorker(
                            id = "worker1",
                            companyId = "company1",
                            workerProfileId = "profile1",
                            employeeNumber = "W-001",
                            role = WorkerRole.FOREMAN,
                            hireDate = LocalDate(2023, 1, 1),
                            status = WorkerStatus.ACTIVE,
                            createdAt = "",
                            updatedAt = "",
                            workerProfile = WorkerProfile(
                                id = "profile1",
                                firstName = "John",
                                lastName = "Smith",
                                email = "john@example.com",
                                phone = "555-1234",
                                photoUrl = null,
                                createdAt = "",
                                updatedAt = ""
                            )
                        )
                    ),
                    onRemove = {}
                )

                HorizontalDivider()

                CrewMemberRow(
                    crewMember = CrewMember(
                        id = "member2",
                        crewId = "crew1",
                        companyWorkerId = "worker2",
                        role = CrewMemberRole.CREW_LEAD,
                        startDate = LocalDate(2023, 2, 1),
                        status = "active",
                        worker = CompanyWorker(
                            id = "worker2",
                            companyId = "company1",
                            workerProfileId = "profile2",
                            employeeNumber = "W-002",
                            role = WorkerRole.CREW_LEAD,
                            hireDate = LocalDate(2023, 2, 1),
                            status = WorkerStatus.ACTIVE,
                            createdAt = "",
                            updatedAt = "",
                            workerProfile = WorkerProfile(
                                id = "profile2",
                                firstName = "Mike",
                                lastName = "Johnson",
                                email = "mike@example.com",
                                phone = "555-5678",
                                photoUrl = null,
                                createdAt = "",
                                updatedAt = ""
                            )
                        )
                    ),
                    onRemove = null
                )

                HorizontalDivider()

                CrewMemberRowCompact(
                    crewMember = CrewMember(
                        id = "member3",
                        crewId = "crew1",
                        companyWorkerId = "worker3",
                        role = CrewMemberRole.MEMBER,
                        startDate = LocalDate(2023, 3, 1),
                        status = "active",
                        worker = CompanyWorker(
                            id = "worker3",
                            companyId = "company1",
                            workerProfileId = "profile3",
                            employeeNumber = "W-003",
                            role = WorkerRole.LABORER,
                            hireDate = LocalDate(2023, 3, 1),
                            status = WorkerStatus.ACTIVE,
                            createdAt = "",
                            updatedAt = "",
                            workerProfile = WorkerProfile(
                                id = "profile3",
                                firstName = "Sarah",
                                lastName = "Williams",
                                email = "sarah@example.com",
                                phone = "555-9012",
                                photoUrl = null,
                                createdAt = "",
                                updatedAt = ""
                            )
                        )
                    )
                )

                HorizontalDivider()

                CrewMemberRowLoading()
            }
        }
    }
}
