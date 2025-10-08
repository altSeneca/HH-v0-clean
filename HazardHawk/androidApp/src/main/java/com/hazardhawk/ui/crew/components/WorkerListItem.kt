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
import com.hazardhawk.models.crew.CompanyWorker
import com.hazardhawk.models.crew.WorkerProfile
import com.hazardhawk.models.crew.WorkerRole
import com.hazardhawk.models.crew.WorkerStatus
import com.hazardhawk.ui.theme.HazardHawkTheme
import kotlinx.datetime.LocalDate

/**
 * Worker List Item Component
 *
 * A compact list item for displaying workers in lists, dropdowns, and selectors.
 * More compact than WorkerCard for high-density displays.
 *
 * Features:
 * - 40dp circular photo with placeholder
 * - Worker name and role
 * - Status indicator dot
 * - Trailing action icon (chevron or custom)
 * - Material 3 design
 * - Accessibility support
 *
 * Design System:
 * - 56dp minimum height (touch target)
 * - 16dp horizontal padding
 * - No card elevation (flat list item)
 * - Divider support between items
 * - High contrast colors
 *
 * @param worker CompanyWorker data
 * @param onClick Action when item is tapped
 * @param showTrailingIcon Whether to show chevron icon
 * @param trailingContent Custom trailing content (overrides showTrailingIcon)
 * @param modifier Modifier for customization
 */
@Composable
fun WorkerListItem(
    worker: CompanyWorker,
    onClick: () -> Unit = {},
    showTrailingIcon: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val workerName = worker.workerProfile?.fullName ?: "Unknown Worker"
    val statusColor = when (worker.status) {
        WorkerStatus.ACTIVE -> Color(0xFF4CAF50)
        WorkerStatus.INACTIVE -> Color(0xFFFFA500)
        WorkerStatus.TERMINATED -> Color(0xFFF44336)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription = "Worker item for $workerName, ${worker.role.displayName}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Worker Photo
        Box {
            AsyncImage(
                model = worker.workerProfile?.photoUrl,
                contentDescription = "Photo of $workerName",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                error = {
                    Surface(
                        modifier = Modifier.size(40.dp),
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
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                placeholder = {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            )

            // Status Indicator Dot
            Surface(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = statusColor,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                )
            ) {}
        }

        // Worker Info
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = workerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = worker.role.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Trailing Content
        if (trailingContent != null) {
            trailingContent()
        } else if (showTrailingIcon) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Worker List Item with checkbox for selection
 */
@Composable
fun WorkerListItemSelectable(
    worker: CompanyWorker,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WorkerListItem(
        worker = worker,
        onClick = {
            onSelectedChange(!selected)
            onClick()
        },
        trailingContent = {
            Checkbox(
                checked = selected,
                onCheckedChange = onSelectedChange
            )
        },
        modifier = modifier
    )
}

/**
 * Worker List Item with role badge
 */
@Composable
fun WorkerListItemWithRole(
    worker: CompanyWorker,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WorkerListItem(
        worker = worker,
        onClick = onClick,
        showTrailingIcon = false,
        trailingContent = {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = worker.role.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        modifier = modifier
    )
}

/**
 * Worker List Item with certification count
 */
@Composable
fun WorkerListItemWithCertCount(
    worker: CompanyWorker,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val certCount = worker.certifications.size
    val validCerts = worker.certifications.count { it.isValid }

    WorkerListItem(
        worker = worker,
        onClick = onClick,
        showTrailingIcon = false,
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = if (validCerts > 0) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (validCerts > 0) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                        Text(
                            text = "$validCerts/$certCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (validCerts > 0) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        modifier = modifier
    )
}

/**
 * Loading skeleton for WorkerListItem
 */
@Composable
fun WorkerListItemLoading(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Photo skeleton
        Surface(
            modifier = Modifier.size(40.dp),
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
                    .fillMaxWidth(0.5f)
                    .height(16.dp),
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

@Preview(name = "Worker List Items", showBackground = true)
@Composable
private fun WorkerListItemPreview() {
    HazardHawkTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Standard List Items",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(16.dp)
                )

                WorkerListItem(
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
                        )
                    )
                )

                HorizontalDivider()

                WorkerListItemWithRole(
                    worker = CompanyWorker(
                        id = "2",
                        companyId = "company1",
                        workerProfileId = "profile2",
                        employeeNumber = "W-5678",
                        role = WorkerRole.SKILLED_WORKER,
                        hireDate = LocalDate(2023, 3, 1),
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

                HorizontalDivider()

                WorkerListItemSelectable(
                    worker = CompanyWorker(
                        id = "3",
                        companyId = "company1",
                        workerProfileId = "profile3",
                        employeeNumber = "W-9012",
                        role = WorkerRole.LABORER,
                        hireDate = LocalDate(2023, 6, 1),
                        status = WorkerStatus.INACTIVE,
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
                    ),
                    selected = true,
                    onSelectedChange = {}
                )

                HorizontalDivider()

                WorkerListItemLoading()
            }
        }
    }
}
