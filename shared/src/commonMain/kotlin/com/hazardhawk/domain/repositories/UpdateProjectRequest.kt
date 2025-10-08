package com.hazardhawk.domain.repositories

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Request to update an existing project
 * All fields are optional - only provided fields will be updated
 */
@Serializable
data class UpdateProjectRequest(
    val name: String? = null,
    val projectNumber: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String? = null,
    val clientName: String? = null,
    val clientContact: String? = null,
    val clientPhone: String? = null,
    val clientEmail: String? = null,
    val streetAddress: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val generalContractor: String? = null,
    val projectManagerId: String? = null,
    val superintendentId: String? = null
)
