package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable
import kotlinx.datetime.LocalDate

@Serializable
data class Project(
    val id: String,
    val companyId: String,
    val name: String,
    val projectNumber: String? = null,
    val location: String? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val status: String,
    val projectManagerId: String? = null,
    val superintendentId: String? = null,

    // Centralized project info
    val clientName: String? = null,
    val clientContact: String? = null,
    val clientPhone: String? = null,
    val clientEmail: String? = null,
    val streetAddress: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val generalContractor: String? = null,

    // Embedded for convenience
    val company: Company? = null,
    val projectManager: CompanyWorker? = null,
    val superintendent: CompanyWorker? = null
)
