package com.hazardhawk.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.hazardhawk.database.HazardHawkDatabase
import org.koin.dsl.module

val iosDatabaseModule = module {
    single<SqlDriver> {
        NativeSqliteDriver(
            schema = HazardHawkDatabase.Schema,
            name = "hazard_hawk.db"
        )
    }
}