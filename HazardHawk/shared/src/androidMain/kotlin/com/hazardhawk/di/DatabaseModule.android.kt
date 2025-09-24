package com.hazardhawk.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.hazardhawk.database.HazardHawkDatabase
import org.koin.dsl.module

val androidDatabaseModule = module {
    single<SqlDriver> {
        AndroidSqliteDriver(
            schema = HazardHawkDatabase.Schema,
            context = get<Context>(),
            name = "hazard_hawk.db"
        )
    }
}