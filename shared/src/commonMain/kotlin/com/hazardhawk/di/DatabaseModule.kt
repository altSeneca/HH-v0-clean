package com.hazardhawk.di

import com.hazardhawk.database.HazardHawkDatabase
import app.cash.sqldelight.db.SqlDriver
import org.koin.dsl.module

/**
 * Database module for data persistence.
 * Contains database driver and configuration.
 */
val databaseModule = module {
    
    // Database instance - driver will be provided by platform-specific modules
    single<HazardHawkDatabase> {
        HazardHawkDatabase(get<SqlDriver>())
    }
}

/**
 * Database configuration module for settings and constants.
 */
val databaseConfigModule = module {
    
    // Database configuration
    // single<DatabaseConfig> {
    //     DatabaseConfig(
    //         name = "hazard_hawk.db",
    //         version = 6,
    //         enableWAL = true,
    //         enableForeignKeys = true
    //     )
    // }
    
    // Query timeout settings
    // single<QueryTimeoutConfig> {
    //     QueryTimeoutConfig(
    //         shortQuery = 5000L,  // 5 seconds
    //         longQuery = 30000L   // 30 seconds
    //     )
    // }
}
