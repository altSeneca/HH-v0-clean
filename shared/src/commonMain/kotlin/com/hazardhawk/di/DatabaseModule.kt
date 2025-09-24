package com.hazardhawk.di

import org.koin.dsl.module

/**
 * Database module for SQLDelight database configuration.
 * Platform-specific implementations will provide the actual database driver.
 * 
 * Usage in platform modules:
 * - Android: Provide SqlDriver with AndroidSqliteDriver
 * - iOS: Provide SqlDriver with NativeSqliteDriver
 * - Desktop: Provide SqlDriver with JdbcSqliteDriver
 * - Web: Provide SqlDriver with WebWorkerDriver or IndexedDbDriver
 */
val databaseModule = module {
    
    // Database instance will be created once the platform-specific driver is provided
    // single<HazardHawkDatabase> {
    //     HazardHawkDatabase(
    //         driver = get<SqlDriver>() // Platform-specific driver
    //     )
    // }
    
    // Database-related utilities that don't depend on platform
    // These can be uncommented once we have the actual database schema
    
    // Note: The actual database and driver setup will be done in platform-specific modules
    // This keeps the shared module free from platform dependencies while allowing
    // repositories to be defined here that depend on the database
}

/**
 * Module for database-related constants and configurations
 */
val databaseConfigModule = module {
    
    // Database configuration constants
    single(qualifier = org.koin.core.qualifier.named("DatabaseName")) { "hazard_hawk.db" }
    single(qualifier = org.koin.core.qualifier.named("DatabaseVersion")) { 6 }
    
    // Database migration settings
    single(qualifier = org.koin.core.qualifier.named("EnableMigrations")) { true }
    single(qualifier = org.koin.core.qualifier.named("EnableWAL")) { true }
}
