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
        var error: NSError?
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
            val existingItem = SecItemCopyMatching(query, null)
            
            val attributes = NSMutableDictionary()
            attributes[kSecValueData] = value.encodeToNSData()
            
            // Add metadata if provided
            metadata?.let {
                val metadataJson = json.encodeToString(it)
                attributes[kSecAttrComment] = metadataJson
            }
            
            // Use Secure Enclave and biometric authentication for enhanced security
            if (hasSecureEnclave && metadata?.complianceLevel in listOf(ComplianceLevel.Enhanced, ComplianceLevel.Critical, ComplianceLevel.OSHA_Compliant)) {
                attributes[kSecAttrTokenID] = kSecAttrTokenIDSecureEnclave
                attributes[kSecAttrAccessControl] = createAccessControl()
            }
            
            val status = if (existingItem == errSecSuccess) {
                // Update existing item
                SecItemUpdate(query, attributes)
            } else {
                // Add new item
                attributes.addEntriesFromDictionary(query)
                SecItemAdd(attributes, null)
            }
            
            if (status == errSecSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Keychain operation failed with status: $status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getApiKey(key: String): Result<String?> {
        return try {
            val query = createKeychainQuery(key).apply {
                this[kSecReturnData] = kCFBooleanTrue
                this[kSecMatchLimit] = kSecMatchLimitOne
            }
            
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query, result.ptr)
                
                when (status) {
                    errSecSuccess -> {
                        val data = result.value as CFDataRef
                        val nsData = data as NSData
                        val value = nsData.decodeToString()
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
            val status = SecItemDelete(query)
            
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
            val query = NSMutableDictionary().apply {
                this[kSecClass] = kSecClassGenericPassword
                this[kSecAttrService] = serviceName
            }
            
            val status = SecItemDelete(query)
            
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
            val query = NSMutableDictionary().apply {
                this[kSecClass] = kSecClassGenericPassword
                this[kSecAttrService] = serviceName
                this[kSecReturnAttributes] = kCFBooleanTrue
                this[kSecMatchLimit] = kSecMatchLimitAll
            }
            
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query, result.ptr)
                
                when (status) {
                    errSecSuccess -> {
                        val items = result.value as CFArrayRef
                        val nsArray = items as NSArray
                        val keys = mutableListOf<String>()
                        
                        for (i in 0 until nsArray.count.toInt()) {
                            val item = nsArray.objectAtIndex(i.toULong()) as NSDictionary
                            val account = item.objectForKey(kSecAttrAccount) as? NSString
                            account?.let { keys.add(it.toString()) }
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
            val query = NSMutableDictionary().apply {
                this[kSecClass] = kSecClassGenericPassword
                this[kSecAttrService] = serviceName
                this[kSecMatchLimit] = kSecMatchLimitOne
            }
            
            val status = SecItemCopyMatching(query, null)
            status == errSecSuccess || status == errSecItemNotFound
        } catch (e: Exception) {
            false
        }
    }
    
    override suspend fun getCredentialMetadata(key: String): Result<CredentialMetadata?> {
        return try {
            val query = createKeychainQuery(key).apply {
                this[kSecReturnAttributes] = kCFBooleanTrue
                this[kSecMatchLimit] = kSecMatchLimitOne
            }
            
            memScoped {
                val result = alloc<CFTypeRefVar>()
                val status = SecItemCopyMatching(query, result.ptr)
                
                when (status) {
                    errSecSuccess -> {
                        val attributes = result.value as NSDictionary
                        val comment = attributes.objectForKey(kSecAttrComment) as? NSString
                        
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
            
            val attributes = NSMutableDictionary().apply {
                this[kSecAttrComment] = metadataJson
            }
            
            val status = SecItemUpdate(query, attributes)
            
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
    private fun createKeychainQuery(key: String): NSMutableDictionary {
        return NSMutableDictionary().apply {
            this[kSecClass] = kSecClassGenericPassword
            this[kSecAttrService] = serviceName
            this[kSecAttrAccount] = key
            this[kSecAttrAccessible] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        }
    }
    
    /**
     * Create access control for Secure Enclave protected items
     */
    private fun createAccessControl(): SecAccessControlRef? {
        var error: Unmanaged<CFErrorRef>? = null
        
        val accessControl = SecAccessControlCreateWithFlags(
            kCFAllocatorDefault,
            kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            kSecAccessControlTouchIDAny or kSecAccessControlPrivateKeyUsage,
            error?.takeRetainedValue()?.autorelease()
        )
        
        return accessControl
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