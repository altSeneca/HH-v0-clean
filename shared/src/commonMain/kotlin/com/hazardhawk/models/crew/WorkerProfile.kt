package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class WorkerProfile(
    val id: String,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: LocalDate? = null,
    val email: String? = null,
    val phone: String? = null,
    val photoUrl: String? = null,
    val createdAt: String,
    val updatedAt: String
) {
    val fullName: String get() = "$firstName $lastName"
}
