package com.hazardhawk.models.crew

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: String,
    val name: String,
    val subdomain: String,
    val tier: String,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val phone: String? = null,
    val logoUrl: String? = null
)
