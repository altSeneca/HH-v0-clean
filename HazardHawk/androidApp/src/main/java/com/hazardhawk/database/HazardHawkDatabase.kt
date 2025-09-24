package com.hazardhawk.database

import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Android-specific database driver initialization for SQLDelight.
 * This file provides Android-specific database setup while using the shared SQLDelight schema.
 */
/**
 * Create Android SQLDelight database driver
 */
fun createAndroidDriver(context: android.content.Context): AndroidSqliteDriver {
    return AndroidSqliteDriver(
        schema = com.hazardhawk.database.HazardHawkDatabase.Schema,
        context = context,
        name = "hazard_hawk.db"
    )
}

/**
 * Create HazardHawk database instance with Android driver
 */
fun createHazardHawkDatabase(context: android.content.Context): com.hazardhawk.database.HazardHawkDatabase {
    val driver = createAndroidDriver(context)
    return com.hazardhawk.database.HazardHawkDatabase(driver)
}