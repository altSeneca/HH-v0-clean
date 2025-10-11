@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hazardhawk.security

import kotlinx.cinterop.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.LocalAuthentication.*
import platform.darwin.NSInteger

/**
 * iOS implementation of SecureStorageService using iOS Keychain Services.
 * Provides secure credential storage with Secure Enclave integration when available.
 * Supports Touch ID/Face ID protected keychain items for enhanced security.
 */
class SecureStorageServiceImpl : SecureStorageService {
    
    private val json = Json { ignoreUnknownKeys = true }
    private val serviceName = "com.hazardhawk.credentials"
    
    // Check if device supports Secure Enclave
    private val hasSecureEnclave: Boolean by lazy {
        val context = LAContext()
        var error: NSError? = null
        context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)
    }
    
    override suspend fun storeApiKey(
        key: String, 
        value: String, 
        metadata: CredentialMetadata?
    ): Result<Unit> {
        return try {
            val query = createKeychainQuery(key)
            
            // Check if item already exists
            memScoped {
                val existingResult = alloc<CFTypeRefVar>()
                val existingStatus = SecItemCopyMatching(query as CFDictionaryRef, existingResult.ptr)
                
                val attributes = CFDictionaryCreateMutable(null, 0, null, null)
                CFDictionarySetValue(attributes, kSecValueData, value.encodeToNSData() as CFTypeRef)
                
                // Add metadata if provided
                metadata?.let {
                    val metadataJson = json.encodeToString(it)
                    CFDictionarySetValue(attributes, kSecAttrComment, metadataJson.toNSString() as CFTypeRef)
                }
                
                // Use Secure Enclave and biometric authentication for enhanced security
                if (hasSecureEnclave && metadata?.complianceLevel in listOf(ComplianceLevel.Enhanced, ComplianceLevel.Critical, ComplianceLevel.OSHA_Compliant)) {
                    CFDictionarySetValue(attributes, kSecAttrTokenID, kSecAttrTokenIDSecureEnclave)
                    createAccessControl()?.let { accessControl ->
                        CFDictionarySetValue(attributes, kSecAttrAccessControl, accessControl)
                    }
                }
                
                val status = if (existingStatus == errSecSuccess) {
                    // Update existing item
                    SecItemUpdate(query as CFDictionaryRef, attributes as CFDictionaryRef)
                } else {
                    // Add new item - merge query and attributes
                    val addDict = CFDictionaryCreateMutableCopy(null, 0, query as CFDictionaryRef)
                    CFDictionaryAddValue(addDict, kSecValueData, value.encodeToNSData() as CFTypeRef)
                    metadata?.let {
                        val metadataJson = json.encodeToString(it)
                        CFDictionaryAddValue(addDict, kSecAttrComment, metadataJson.toNSString() as CFTypeRef)
                    }
                    SecItemAdd(addDict as CFDictionaryRef, null)
                }
                
                if (status == errSecSuccess) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Keychain operation failed with status: $status"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getApiKey(key: String): Result<String?> {
        return try {
            val query = CFDictionaryCreateMutableCopy(null, 0, createKeychainQuery(key) as CFDictionaryRef)
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
                
                when (status) {
                    errSecSuccess -> {
                        val data = CFBridgingRelease(result.value) as? NSData
                        val value = data?.decodeToString()
                        Result.success(value)
                    }
                    errSecItemNotFound -> Result.success(null)
                    else -> Result.failure(Exception("Keychain query failed with status: $status"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeApiKey(key: String): Result<Unit> {
        return try {
            val query = createKeychainQuery(key)
            val status = SecItemDelete(query as CFDictionaryRef)
            
            when (status) {
                errSecSuccess, errSecItemNotFound -> Result.success(Unit)
                else -> Result.failure(Exception("Keychain delete failed with status: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearAllCredentials(): Result<Unit> {
        return try {
            val query = CFDictionaryCreateMutable(null, 2, null, null)
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceName.toNSString() as CFTypeRef)
            
            val status = SecItemDelete(query as CFDictionaryRef)
            
            when (status) {
                errSecSuccess, errSecItemNotFound -> Result.success(Unit)
                else -> Result.failure(Exception("Keychain clear all failed with status: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun listCredentialKeys(): Result<List<String>> {
        return try {
            val query = CFDictionaryCreateMutable(null, 4, null, null)
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceName.toNSString() as CFTypeRef)
            CFDictionarySetValue(query, kSecReturnAttributes, kCFBooleanTrue)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitAll)
            
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
                
                when (status) {
                    errSecSuccess -> {
                        val items = CFBridgingRelease(result.value) as? NSArray
                        val keys = mutableListOf<String>()
                        
                        items?.let { array ->
                            for (i in 0 until array.count.toInt()) {
                                val item = array.objectAtIndex(i.toULong()) as? NSDictionary
                                val account = item?.objectForKey(kSecAttrAccount) as? NSString
                                account?.let { keys.add(it.toString()) }
                            }
                        }
                        
                        Result.success(keys)
                    }
                    errSecItemNotFound -> Result.success(emptyList())
                    else -> Result.failure(Exception("Keychain list query failed with status: $status"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun isAvailable(): Boolean {
        return try {
            // Test keychain availability by attempting a simple query
            val query = CFDictionaryCreateMutable(null, 3, null, null)
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceName.toNSString() as CFTypeRef)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            
            val status = SecItemCopyMatching(query as CFDictionaryRef, null)
            status == errSecSuccess || status == errSecItemNotFound
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getCredentialMetadata(key: String): Result<CredentialMetadata?> {
        return try {
            val query = CFDictionaryCreateMutableCopy(null, 0, createKeychainQuery(key) as CFDictionaryRef)
            CFDictionarySetValue(query, kSecReturnAttributes, kCFBooleanTrue)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
                
                when (status) {
                    errSecSuccess -> {
                        val attributes = CFBridgingRelease(result.value) as? NSDictionary
                        val comment = attributes?.objectForKey(kSecAttrComment) as? NSString
                        
                        val metadata = comment?.let {
                            try {
                                json.decodeFromString<CredentialMetadata>(it.toString())
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        Result.success(metadata)
                    }
                    errSecItemNotFound -> Result.success(null)
                    else -> Result.failure(Exception("Keychain metadata query failed with status: $status"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCredentialMetadata(
        key: String, 
        metadata: CredentialMetadata
    ): Result<Unit> {
        return try {
            val query = createKeychainQuery(key)
            val metadataJson = json.encodeToString(metadata)
            
            val attributes = CFDictionaryCreateMutable(null, 1, null, null)
            CFDictionarySetValue(attributes, kSecAttrComment, metadataJson.toNSString() as CFTypeRef)
            
            val status = SecItemUpdate(query as CFDictionaryRef, attributes as CFDictionaryRef)
            
            when (status) {
                errSecSuccess -> Result.success(Unit)
                errSecItemNotFound -> Result.failure(Exception("Credential not found for key: $key"))
                else -> Result.failure(Exception("Keychain metadata update failed with status: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create a basic keychain query dictionary for a given key
     */
    private fun createKeychainQuery(key: String): CFMutableDictionaryRef? {
        val query = CFDictionaryCreateMutable(null, 4, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, serviceName.toNSString() as CFTypeRef)
        CFDictionarySetValue(query, kSecAttrAccount, key.toNSString() as CFTypeRef)
        CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleWhenUnlockedThisDeviceOnly)
        return query
    }
    
    /**
     * Create access control for Secure Enclave protected items
     */
    private fun createAccessControl(): SecAccessControlRef? {
        return memScoped {
            val error = alloc<CFErrorRefVar>()
            
            val accessControl = SecAccessControlCreateWithFlags(
                kCFAllocatorDefault,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
                kSecAccessControlTouchIDAny or kSecAccessControlPrivateKeyUsage,
                error.ptr
            )
            
            accessControl
        }
    }
    
    /**
     * Convert String to NSData for keychain storage
     */
    private fun String.encodeToNSData(): NSData {
        return this.encodeToByteArray().usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = pinned.get().size.toULong())
        }
    }
    
    /**
     * Helper to convert String to NSString
     */
    private fun String.toNSString(): NSString {
        return NSString.create(string = this)
    }
    
    /**
     * Convert NSData to String for keychain retrieval
     */
    private fun NSData.decodeToString(): String {
        val bytes = ByteArray(this.length.toInt())
        bytes.usePinned { pinned ->
            this.getBytes(pinned.addressOf(0), this.length)
        }
        return bytes.decodeToString()
    }
}
