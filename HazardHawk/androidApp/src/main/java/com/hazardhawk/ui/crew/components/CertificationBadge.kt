package com.hazardhawk.ui.crew.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hazardhawk.models.crew.CertificationStatus
import com.hazardhawk.models.crew.CertificationType
import com.hazardhawk.models.crew.WorkerCertification
import com.hazardhawk.ui.theme.HazardHawkTheme
import kotlinx.datetime.LocalDate

/**
 * Certification Badge Component
 *
 * A color-coded badge displaying certification status with icon and code.
 *
 * Color Coding:
 * - Green: Verified and valid
 * - Amber/Orange: Expiring soon (within 30 days)
 * - Red: Expired or not verified
 * - Gray: Pending verification
 *
 * Icons:
 * - CheckCircle: Valid certification
 * - Warning: Expiring soon
 * - Error: Expired
 * - HourglassEmpty: Pending verification
 *
 * Modes:
 * - Compact: Icon only (12dp)
 * - Full: Icon + certification code
 *
 * Design System:
 * - 4dp corner radius
 * - 6dp horizontal padding, 4dp vertical padding
 * - Material 3 label typography
 * - High contrast for outdoor visibility
 * - Accessible color combinations
 *
 * @param certification WorkerCertification data
 * @param compact Whether to show icon only (true) or icon + code (false)
 * @param modifier Modifier for customization
 */
@Composable
fun CertificationBadge(
    certification: WorkerCertification,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val certCode = certification.certificationType?.code ?: "CERT"
    val certName = certification.certificationType?.name ?: "Certification"

    // Determine badge colors based on status and expiration
    val (backgroundColor, textColor, icon, statusText) = when {
        certification.status != CertificationStatus.VERIFIED -> {
            Tuple4(
                Color(0xFFEEEEEE), // Gray background
                Color(0xFF757575), // Gray text
                Icons.Default.HourglassEmpty,
                "Pending verification"
            )
        }
        certification.isExpired -> {
            Tuple4(
                Color(0xFFFFEBEE), // Light red
                Color(0xFFC62828), // Dark red
                Icons.Default.Error,
                "Expired"
            )
        }
        certification.isExpiringSoon -> {
            Tuple4(
                Color(0xFFFFF3E0), // Light amber
                Color(0xFFE65100), // Dark orange
                Icons.Default.Warning,
                "Expiring soon"
            )
        }
        else -> {
            Tuple4(
                Color(0xFFE8F5E9), // Light green
                Color(0xFF2E7D32), // Dark green
                Icons.Default.CheckCircle,
                "Valid"
            )
        }
    }

    val contentDescriptionText = "$certName certification: $statusText" +
            if (!compact) " ($certCode)" else ""

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
        modifier = modifier.semantics {
            contentDescription = contentDescriptionText
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = textColor
            )

            // Certification Code (only in full mode)
            if (!compact) {
                Text(
                    text = certCode,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Certification Badge with expiration date
 */
@Composable
fun CertificationBadgeWithDate(
    certification: WorkerCertification,
    modifier: Modifier = Modifier
) {
    val certCode = certification.certificationType?.code ?: "CERT"
    val expirationText = certification.expirationDate?.toString() ?: "No expiration"

    val (backgroundColor, textColor, icon) = when {
        certification.status != CertificationStatus.VERIFIED -> {
            Triple(Color(0xFFEEEEEE), Color(0xFF757575), Icons.Default.HourglassEmpty)
        }
        certification.isExpired -> {
            Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.Error)
        }
        certification.isExpiringSoon -> {
            Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.Warning)
        }
        else -> {
            Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.CheckCircle)
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = textColor
            )

            Column {
                Text(
                    text = certCode,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Expires: $expirationText",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor
                )
            }
        }
    }
}

/**
 * Certification Badge List - displays multiple certifications in a row
 */
@Composable
fun CertificationBadgeList(
    certifications: List<WorkerCertification>,
    maxVisible: Int = 3,
    compact: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (certifications.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        certifications.take(maxVisible).forEach { cert ->
            CertificationBadge(
                certification = cert,
                compact = compact
            )
        }

        if (certifications.size > maxVisible) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "+${certifications.size - maxVisible}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// ============================================================================
// HELPER DATA CLASS
// ============================================================================

private data class Tuple4<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

// ============================================================================
// PREVIEW SECTION
// ============================================================================

@Preview(name = "Certification Badge - All States", showBackground = true)
@Composable
private fun CertificationBadgePreview() {
    HazardHawkTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Compact Badges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Valid certification
                    CertificationBadge(
                        certification = WorkerCertification(
                            id = "1",
                            workerProfileId = "w1",
                            certificationTypeId = "type1",
                            issueDate = LocalDate(2023, 1, 1),
                            expirationDate = LocalDate(2025, 12, 31),
                            documentUrl = "",
                            status = CertificationStatus.VERIFIED,
                            createdAt = "",
                            updatedAt = "",
                            certificationType = CertificationType(
                                id = "type1",
                                code = "OSHA-10",
                                name = "OSHA 10-Hour",
                                category = "safety_training",
                                region = "US"
                            )
                        ),
                        compact = true
                    )

                    // Expiring soon
                    CertificationBadge(
                        certification = WorkerCertification(
                            id = "2",
                            workerProfileId = "w1",
                            certificationTypeId = "type2",
                            issueDate = LocalDate(2023, 1, 1),
                            expirationDate = LocalDate(2025, 11, 15),
                            documentUrl = "",
                            status = CertificationStatus.VERIFIED,
                            createdAt = "",
                            updatedAt = "",
                            certificationType = CertificationType(
                                id = "type2",
                                code = "CPR",
                                name = "CPR/First Aid",
                                category = "emergency_response",
                                region = "US"
                            )
                        ),
                        compact = true
                    )

                    // Expired
                    CertificationBadge(
                        certification = WorkerCertification(
                            id = "3",
                            workerProfileId = "w1",
                            certificationTypeId = "type3",
                            issueDate = LocalDate(2022, 1, 1),
                            expirationDate = LocalDate(2024, 1, 1),
                            documentUrl = "",
                            status = CertificationStatus.VERIFIED,
                            createdAt = "",
                            updatedAt = "",
                            certificationType = CertificationType(
                                id = "type3",
                                code = "FORK",
                                name = "Forklift Operator",
                                category = "equipment_operation",
                                region = "US"
                            )
                        ),
                        compact = true
                    )

                    // Pending
                    CertificationBadge(
                        certification = WorkerCertification(
                            id = "4",
                            workerProfileId = "w1",
                            certificationTypeId = "type4",
                            issueDate = LocalDate(2025, 1, 1),
                            expirationDate = LocalDate(2027, 1, 1),
                            documentUrl = "",
                            status = CertificationStatus.PENDING_VERIFICATION,
                            createdAt = "",
                            updatedAt = "",
                            certificationType = CertificationType(
                                id = "type4",
                                code = "SCAFFOLD",
                                name = "Scaffold Erector",
                                category = "safety_training",
                                region = "US"
                            )
                        ),
                        compact = true
                    )
                }

                Divider()

                Text(
                    text = "Full Badges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CertificationBadge(
                        certification = WorkerCertification(
                            id = "1",
                            workerProfileId = "w1",
                            certificationTypeId = "type1",
                            issueDate = LocalDate(2023, 1, 1),
                            expirationDate = LocalDate(2025, 12, 31),
                            documentUrl = "",
                            status = CertificationStatus.VERIFIED,
                            createdAt = "",
                            updatedAt = "",
                            certificationType = CertificationType(
                                id = "type1",
                                code = "OSHA-30",
                                name = "OSHA 30-Hour",
                                category = "safety_training",
                                region = "US"
                            )
                        ),
                        compact = false
                    )

                    CertificationBadgeWithDate(
                        certification = WorkerCertification(
                            id = "2",
                            workerProfileId = "w1",
                            certificationTypeId = "type2",
                            issueDate = LocalDate(2023, 1, 1),
                            expirationDate = LocalDate(2025, 11, 15),
                            documentUrl = "",
                            status = CertificationStatus.VERIFIED,
                            createdAt = "",
                            updatedAt = "",
                            certificationType = CertificationType(
                                id = "type2",
                                code = "CPR",
                                name = "CPR/First Aid",
                                category = "emergency_response",
                                region = "US"
                            )
                        )
                    )
                }
            }
        }
    }
}
