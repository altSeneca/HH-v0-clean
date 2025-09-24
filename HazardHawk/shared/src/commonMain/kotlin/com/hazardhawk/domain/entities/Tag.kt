package com.hazardhawk.domain.entities

import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: String,
    val name: String,
    val category: String,
    val description: String = "",
    val oshaCode: String? = null
)