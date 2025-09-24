package com.hazardhawk.security

import com.hazardhawk.security.PhotoEncryptionService

/**
 * Android implementation of PhotoEncryptionService
 * For now, implements pass-through (no encryption) for rapid development
 * TODO: Implement real encryption using Android Keystore
 */
class AndroidPhotoEncryptionService : PhotoEncryptionService {
    
    override suspend fun encryptData(data: ByteArray): ByteArray {
        // For now, return data as-is to get AI working quickly
        // TODO: Implement real encryption using Android Keystore
        return data
    }
    
    override suspend fun decryptData(encryptedData: ByteArray): ByteArray {
        // For now, return data as-is to get AI working quickly  
        // TODO: Implement real decryption using Android Keystore
        return encryptedData
    }
}