package com.hazardhawk.domain.di

import com.hazardhawk.domain.services.NotificationService
import com.hazardhawk.domain.services.NotificationServiceImpl
import org.koin.dsl.module

val domainModule = module {
    // Services
    single<NotificationService> { NotificationServiceImpl() }

    // Use cases will be added as needed
}
