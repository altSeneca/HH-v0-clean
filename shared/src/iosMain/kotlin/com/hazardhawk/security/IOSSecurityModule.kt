package com.hazardhawk.security

import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS-specific dependency injection module for security services.
 * Provides iOS platform implementations of security interfaces.
 */
val iosSecurityModule: Module = module {
    
    // iOS Keychain-based secure storage service
    single<SecureStorageService> {
        SecureStorageServiceImpl()
    }
    
    // iOS Security framework-based photo encryption service
    single<PhotoEncryptionService> {
        PhotoEncryptionServiceImpl()
    }
    
    // iOS security configuration and utilities
    single {
        IOSSecurityConfig
    }
    
    // Security validator for iOS compliance checking
    single {
        IOSSecurityConfig.SecurityValidator
    }
    
    // Device capability detector
    single {
        IOSSecurityConfig.CapabilityDetector
    }
}