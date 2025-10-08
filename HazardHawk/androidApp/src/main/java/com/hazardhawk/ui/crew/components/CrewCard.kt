package com.hazardhawk.ui.crew.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hazardhawk.models.crew.*
import com.hazardhawk.ui.theme.HazardHawkTheme
import kotlinx.datetime.LocalDate

/**
 * Crew Card Component
 *
 * An expandable card displaying crew information with member list.
 *
 * Features:
 * - Crew name, location, and member count
 * - Foreman identification
 * - Expandable/collapsible member list
 * - Action buttons (Add Member, Start Toolbox Talk)
 * - Material 3 design with smooth animations
 * - Accessibility support
 *
 * Design System:
 * - 4dp elevation
 * - 16dp padding
 * - Smooth expand/collapse animation
 * - High contrast for outdoor use
 * - Large touch targets (60dp minimum)
 *
 * @param crew Crew data with embedded members
 * @param onClick Action when card header is tapped
 * @param onAddMember Action to add a new crew member
 * @param onStartToolboxTalk Action to start a toolbox talk
 * @param modifier Modifier for customization
 */
@Composable
fun CrewCard(
    crew: Crew,
    onClick: () -> Unit = {},
    onAddMember: () -> Unit = {},
    onStartToolboxTalk: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Crew card for ${crew.name}, ${crew.memberCount} members"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header - Always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .heightIn(min = 60.dp), // Minimum touch target
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Crew Name
                    Text(
                        text = crew.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Member Count and Location
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${crew.memberCount} members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (crew.location != null) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = crew.location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Foreman
                    if (crew.foreman != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Foreman: ${crew.foreman.workerProfile?.fullName ?: "Unknown"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Expand/Collapse Button
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse crew details" else "Expand crew details",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Expanded Content - Member List and Actions
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(12.dp))

                    // Member List
                    if (crew.members.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No crew members assigned",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        crew.members.forEach { member ->
                            if (member.worker != null) {
                                WorkerCard(
                                    worker = member.worker,
                                    showCertifications = true,
                                    onClick = { onClick() },
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onAddMember,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Member")
                        }

                        Button(
                            onClick = onStartToolboxTalk,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Announcement,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Toolbox Talk")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compact Crew Card - Non-expandable version for lists
 */
@Composable
fun CrewCardCompact(
    crew: Crew,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(min = 60.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = crew.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${crew.memberCount} members",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (crew.location != null) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = crew.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View crew details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Loading state for CrewCard
 */
@Composable
fun CrewCardLoading(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {}

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall
            ) {}
        }
    }
}

// ============================================================================
// PREVIEW SECTION
// ============================================================================

@Preview(name = "Crew Card - Expanded", showBackground = true)
@Composable
private fun CrewCardPreview() {
    HazardHawkTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CrewCard(
                    crew = Crew(
                        id = "crew1",
                        companyId = "company1",
                        projectId = "project1",
                        name = "Framing Crew A",
                        crewType = CrewType.PROJECT_BASED,
                        trade = "Framing",
                        foremanId = "foreman1",
                        location = "Floor 3 - East Wing",
                        status = CrewStatus.ACTIVE,
                        createdAt = "",
                        updatedAt = "",
                        foreman = CompanyWorker(
                            id = "foreman1",
                            companyId = "company1",
                            workerProfileId = "profile1",
                            employeeNumber = "F-001",
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
                        ),
                        members = listOf(
                            CrewMember(
                                id = "member1",
                                crewId = "crew1",
                                companyWorkerId = "worker1",
                                role = CrewMemberRole.MEMBER,
                                startDate = LocalDate(2023, 1, 1),
                                status = "active",
                                worker = CompanyWorker(
                                    id = "worker1",
                                    companyId = "company1",
                                    workerProfileId = "profile2",
                                    employeeNumber = "W-101",
                                    role = WorkerRole.SKILLED_WORKER,
                                    hireDate = LocalDate(2023, 1, 1),
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
                            )
                        )
                    )
                )

                CrewCardCompact(
                    crew = Crew(
                        id = "crew2",
                        companyId = "company1",
                        projectId = "project1",
                        name = "Electrical Crew B",
                        crewType = CrewType.TRADE_SPECIFIC,
                        trade = "Electrical",
                        location = "Building A",
                        status = CrewStatus.ACTIVE,
                        createdAt = "",
                        updatedAt = "",
                        members = emptyList()
                    )
                )

                CrewCardLoading()
            }
        }
    }
}
