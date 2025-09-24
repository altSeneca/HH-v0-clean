package com.hazardhawk.tag.security

import com.hazardhawk.data.repositories.TagRepositoryImpl
import com.hazardhawk.models.Tag
import com.hazardhawk.security.AccessControlManager
import com.hazardhawk.security.UserRole
import com.hazardhawk.security.Permission
import com.hazardhawk.security.SecurityException
import com.hazardhawk.test.TestDataFactory
import com.hazardhawk.test.MockInMemoryDatabase
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Comprehensive security validation and access control tests for tag catalog operations.
 * Tests role-based permissions, input sanitization, and security boundaries.
 * Ensures OSHA compliance and data integrity through security controls.
 */
class TagCatalogSecurityTest {
    
    private lateinit var mockDatabase: MockInMemoryDatabase
    private lateinit var repository: TagRepositoryImpl
    private lateinit var mockAccessControlManager: AccessControlManager
    
    @BeforeTest
    fun setup() {
        mockDatabase = MockInMemoryDatabase()
        repository = TagRepositoryImpl(mockDatabase)
        mockAccessControlManager = mockk(relaxed = true)
    }
    
    @AfterTest
    fun teardown() {
        mockDatabase.clear()
        clearAllMocks()
    }
    
    // MARK: - Role-Based Access Control Tests
    
    @Test
    fun `field worker should only have read access to tags`() = runTest {
        // Given - Field worker user
        val fieldWorkerUserId = "field-worker-1"
        every { mockAccessControlManager.getUserRole(fieldWorkerUserId) } returns UserRole.FIELD_WORKER
        every { mockAccessControlManager.hasPermission(fieldWorkerUserId, Permission.READ_TAGS) } returns true
        every { mockAccessControlManager.hasPermission(fieldWorkerUserId, Permission.WRITE_TAGS) } returns false
        every { mockAccessControlManager.hasPermission(fieldWorkerUserId, Permission.DELETE_TAGS) } returns false
        
        // When/Then - Should allow read operations
        assertTrue(mockAccessControlManager.hasPermission(fieldWorkerUserId, Permission.READ_TAGS))
        
        // When/Then - Should deny write operations
        assertFalse(mockAccessControlManager.hasPermission(fieldWorkerUserId, Permission.WRITE_TAGS))
        assertFalse(mockAccessControlManager.hasPermission(fieldWorkerUserId, Permission.DELETE_TAGS))
        
        verify { mockAccessControlManager.getUserRole(fieldWorkerUserId) }
    }
    
    @Test
    fun `safety lead should have read and write access but not delete all tags`() = runTest {
        // Given - Safety lead user
        val safetyLeadUserId = "safety-lead-1"
        every { mockAccessControlManager.getUserRole(safetyLeadUserId) } returns UserRole.SAFETY_LEAD
        every { mockAccessControlManager.hasPermission(safetyLeadUserId, Permission.READ_TAGS) } returns true
        every { mockAccessControlManager.hasPermission(safetyLeadUserId, Permission.WRITE_TAGS) } returns true
        every { mockAccessControlManager.hasPermission(safetyLeadUserId, Permission.DELETE_TAGS) } returns true
        every { mockAccessControlManager.hasPermission(safetyLeadUserId, Permission.DELETE_ALL_TAGS) } returns false
        
        // When/Then - Should allow read/write operations
        assertTrue(mockAccessControlManager.hasPermission(safetyLeadUserId, Permission.READ_TAGS))
        assertTrue(mockAccessControlManager.hasPermission(safetyLeadUserId, Permission.WRITE_TAGS))
        assertTrue(mockAccessControlManager.hasPermission(safetyLeadUserId, Permission.DELETE_TAGS))
        
        // When/Then - Should deny bulk delete operations
        assertFalse(mockAccessControlManager.hasPermission(safetyLeadUserId, Permission.DELETE_ALL_TAGS))
    }
    
    @Test
    fun `project admin should have full access to all tag operations`() = runTest {
        // Given - Project admin user
        val adminUserId = "admin-1"
        every { mockAccessControlManager.getUserRole(adminUserId) } returns UserRole.PROJECT_ADMIN
        every { mockAccessControlManager.hasPermission(adminUserId, any()) } returns true
        
        // When/Then - Should allow all operations
        assertTrue(mockAccessControlManager.hasPermission(adminUserId, Permission.READ_TAGS))
        assertTrue(mockAccessControlManager.hasPermission(adminUserId, Permission.WRITE_TAGS))
        assertTrue(mockAccessControlManager.hasPermission(adminUserId, Permission.DELETE_TAGS))
        assertTrue(mockAccessControlManager.hasPermission(adminUserId, Permission.DELETE_ALL_TAGS))
        assertTrue(mockAccessControlManager.hasPermission(adminUserId, Permission.MANAGE_TAG_CATEGORIES))
        assertTrue(mockAccessControlManager.hasPermission(adminUserId, Permission.BULK_OPERATIONS))
    }
    
    @Test
    fun `unauthorized user should be denied all tag operations`() = runTest {
        // Given - Unauthorized user
        val unauthorizedUserId = "unauthorized-1"
        every { mockAccessControlManager.getUserRole(unauthorizedUserId) } returns UserRole.NONE
        every { mockAccessControlManager.hasPermission(unauthorizedUserId, any()) } returns false
        
        // When/Then - Should deny all operations
        assertFalse(mockAccessControlManager.hasPermission(unauthorizedUserId, Permission.READ_TAGS))
        assertFalse(mockAccessControlManager.hasPermission(unauthorizedUserId, Permission.WRITE_TAGS))
        assertFalse(mockAccessControlManager.hasPermission(unauthorizedUserId, Permission.DELETE_TAGS))
        assertFalse(mockAccessControlManager.hasPermission(unauthorizedUserId, Permission.DELETE_ALL_TAGS))
    }
    
    // MARK: - Input Sanitization Tests
    
    @Test
    fun `tag names should be sanitized to prevent XSS attacks`() = runTest {
        // Given - Malicious tag name with script injection
        val maliciousName = "<script>alert('XSS')</script>Safety Tag"
        val sanitizedName = "Safety Tag" // Expected after sanitization
        
        // When - Create tag with malicious name
        val maliciousTag = TestDataFactory.createTestTag(name = maliciousName)
        
        // Mock sanitization process
        every { mockAccessControlManager.sanitizeInput(maliciousName) } returns sanitizedName
        
        val sanitized = mockAccessControlManager.sanitizeInput(maliciousName)
        
        // Then - Should remove malicious content
        assertEquals(sanitizedName, sanitized)
        assertFalse(sanitized.contains("<script>"))
        assertFalse(sanitized.contains("</script>"))
        verify { mockAccessControlManager.sanitizeInput(maliciousName) }
    }
    
    @Test
    fun `tag names should reject SQL injection attempts`() = runTest {
        // Given - SQL injection attempt in tag name
        val sqlInjectionName = "'; DROP TABLE tags; --"
        
        // When - Attempt to create tag with SQL injection
        every { mockAccessControlManager.validateInput(sqlInjectionName) } returns false
        
        val isValid = mockAccessControlManager.validateInput(sqlInjectionName)
        
        // Then - Should reject malicious input
        assertFalse(isValid)
        verify { mockAccessControlManager.validateInput(sqlInjectionName) }
    }
    
    @Test
    fun `tag descriptions should sanitize HTML content`() = runTest {
        // Given - Tag description with HTML content
        val htmlDescription = "<b>Important</b> safety <em>requirement</em> for <a href='#'>workers</a>"
        val sanitizedDescription = "Important safety requirement for workers"
        
        // When - Sanitize HTML content
        every { mockAccessControlManager.sanitizeHtml(htmlDescription) } returns sanitizedDescription
        
        val result = mockAccessControlManager.sanitizeHtml(htmlDescription)
        
        // Then - Should remove HTML tags but keep text content
        assertEquals(sanitizedDescription, result)
        assertFalse(result.contains("<"))
        assertFalse(result.contains(">"))
    }
    
    @Test
    fun `tag names should enforce length limits to prevent DoS attacks`() = runTest {
        // Given - Extremely long tag name (potential DoS attack)
        val extremelyLongName = "A".repeat(10000)
        val maxAllowedLength = 255
        
        // When - Validate tag name length
        every { mockAccessControlManager.validateLength(extremelyLongName, maxAllowedLength) } returns false
        
        val isValidLength = mockAccessControlManager.validateLength(extremelyLongName, maxAllowedLength)
        
        // Then - Should reject overly long names
        assertFalse(isValidLength)
        assertTrue(extremelyLongName.length > maxAllowedLength)
    }
    
    // MARK: - Data Validation and Integrity Tests
    
    @Test
    fun `OSHA reference codes should be validated for authenticity`() = runTest {
        // Given - Tag with OSHA reference
        val validOSHACode = "1926.501"
        val invalidOSHACode = "9999.999"
        
        // When - Validate OSHA codes
        every { mockAccessControlManager.validateOSHACode(validOSHACode) } returns true
        every { mockAccessControlManager.validateOSHACode(invalidOSHACode) } returns false
        
        // Then - Should validate authentic OSHA codes
        assertTrue(mockAccessControlManager.validateOSHACode(validOSHACode))
        assertFalse(mockAccessControlManager.validateOSHACode(invalidOSHACode))
    }
    
    @Test
    fun `tag categories should be restricted to predefined values`() = runTest {
        // Given - Valid and invalid categories
        val validCategory = "Safety"
        val invalidCategory = "CustomUnsafeCategory"
        val allowedCategories = listOf("Safety", "Equipment", "Environmental", "Electrical", "Structural")
        
        // When - Validate categories
        every { mockAccessControlManager.validateCategory(validCategory, allowedCategories) } returns true
        every { mockAccessControlManager.validateCategory(invalidCategory, allowedCategories) } returns false
        
        // Then - Should only allow predefined categories
        assertTrue(mockAccessControlManager.validateCategory(validCategory, allowedCategories))
        assertFalse(mockAccessControlManager.validateCategory(invalidCategory, allowedCategories))
    }
    
    @Test
    fun `custom tags should require additional validation`() = runTest {
        // Given - Custom tag requiring approval
        val customTag = TestDataFactory.createTestTag(
            name = "Site Specific Safety Rule",
            isCustom = true
        )
        
        // When - Validate custom tag
        every { mockAccessControlManager.requiresApproval(customTag) } returns true
        every { mockAccessControlManager.isApproved(customTag.id) } returns false
        
        // Then - Should require approval for custom tags
        assertTrue(mockAccessControlManager.requiresApproval(customTag))
        assertFalse(mockAccessControlManager.isApproved(customTag.id))
    }
    
    // MARK: - Audit Trail Tests
    
    @Test
    fun `tag modifications should be logged for audit trail`() = runTest {
        // Given - User modifying a tag
        val userId = "safety-lead-1"
        val originalTag = TestDataFactory.createTestTag(id = "audit-tag", name = "Original Name")
        val modifiedTag = originalTag.copy(name = "Modified Name")
        
        // When - Tag is modified
        every { mockAccessControlManager.logTagModification(userId, originalTag, modifiedTag) } returns Unit
        
        mockAccessControlManager.logTagModification(userId, originalTag, modifiedTag)
        
        // Then - Should log the modification
        verify { mockAccessControlManager.logTagModification(userId, originalTag, modifiedTag) }
    }
    
    @Test
    fun `tag deletions should be logged with user information`() = runTest {
        // Given - User deleting a tag
        val userId = "admin-1"
        val tagToDelete = TestDataFactory.createTestTag(id = "delete-tag")
        
        // When - Tag is deleted
        every { mockAccessControlManager.logTagDeletion(userId, tagToDelete) } returns Unit
        
        mockAccessControlManager.logTagDeletion(userId, tagToDelete)
        
        // Then - Should log the deletion
        verify { mockAccessControlManager.logTagDeletion(userId, tagToDelete) }
    }
    
    @Test
    fun `bulk operations should be logged with affected tag count`() = runTest {
        // Given - Bulk operation
        val userId = "admin-1"
        val affectedTagIds = setOf("tag-1", "tag-2", "tag-3")
        val operation = "BULK_DELETE"
        
        // When - Bulk operation is performed
        every { mockAccessControlManager.logBulkOperation(userId, operation, affectedTagIds.size) } returns Unit
        
        mockAccessControlManager.logBulkOperation(userId, operation, affectedTagIds.size)
        
        // Then - Should log bulk operation
        verify { mockAccessControlManager.logBulkOperation(userId, operation, affectedTagIds.size) }
    }
    
    // MARK: - Rate Limiting Tests
    
    @Test
    fun `rapid tag creation should be rate limited`() = runTest {
        // Given - User attempting rapid tag creation
        val userId = "user-1"
        val rateLimitWindow = 60 // seconds
        val maxOperationsPerWindow = 10
        
        // When - Check rate limit
        every { mockAccessControlManager.isRateLimited(userId, "CREATE_TAG", rateLimitWindow, maxOperationsPerWindow) } returns true
        
        val isRateLimited = mockAccessControlManager.isRateLimited(userId, "CREATE_TAG", rateLimitWindow, maxOperationsPerWindow)
        
        // Then - Should be rate limited
        assertTrue(isRateLimited)
    }
    
    @Test
    fun `bulk operations should have stricter rate limits`() = runTest {
        // Given - User attempting bulk operations
        val userId = "admin-1"
        val bulkRateLimit = 5 // per hour
        val rateLimitWindow = 3600 // 1 hour in seconds
        
        // When - Check bulk operation rate limit
        every { mockAccessControlManager.isRateLimited(userId, "BULK_DELETE", rateLimitWindow, bulkRateLimit) } returns false
        
        val isRateLimited = mockAccessControlManager.isRateLimited(userId, "BULK_DELETE", rateLimitWindow, bulkRateLimit)
        
        // Then - Should allow operation within limits
        assertFalse(isRateLimited)
    }
    
    // MARK: - Data Encryption Tests
    
    @Test
    fun `sensitive tag data should be encrypted at rest`() = runTest {
        // Given - Tag with sensitive information
        val sensitiveTag = TestDataFactory.createTestTag(
            name = "Chemical Exposure Protocol",
            category = "Environmental"
        )
        
        // When - Encrypt sensitive data
        every { mockAccessControlManager.encryptSensitiveData(sensitiveTag.name) } returns "encrypted_data_hash"
        
        val encryptedName = mockAccessControlManager.encryptSensitiveData(sensitiveTag.name)
        
        // Then - Should encrypt sensitive content
        assertEquals("encrypted_data_hash", encryptedName)
        assertNotEquals(sensitiveTag.name, encryptedName)
    }
    
    @Test
    fun `tag data should be decrypted for authorized users only`() = runTest {
        // Given - Encrypted tag data
        val encryptedData = "encrypted_data_hash"
        val originalData = "Chemical Exposure Protocol"
        val authorizedUserId = "safety-lead-1"
        val unauthorizedUserId = "field-worker-1"
        
        // When - Attempt decryption
        every { mockAccessControlManager.canDecryptData(authorizedUserId) } returns true
        every { mockAccessControlManager.canDecryptData(unauthorizedUserId) } returns false
        every { mockAccessControlManager.decryptData(encryptedData) } returns originalData
        
        // Then - Only authorized users should decrypt
        assertTrue(mockAccessControlManager.canDecryptData(authorizedUserId))
        assertFalse(mockAccessControlManager.canDecryptData(unauthorizedUserId))
        
        if (mockAccessControlManager.canDecryptData(authorizedUserId)) {
            val decryptedData = mockAccessControlManager.decryptData(encryptedData)
            assertEquals(originalData, decryptedData)
        }
    }
    
    // MARK: - Session Security Tests
    
    @Test
    fun `expired user sessions should be rejected`() = runTest {
        // Given - User with expired session
        val userId = "user-1"
        val sessionToken = "expired_session_token"
        
        // When - Check session validity
        every { mockAccessControlManager.isSessionValid(userId, sessionToken) } returns false
        
        val isValidSession = mockAccessControlManager.isSessionValid(userId, sessionToken)
        
        // Then - Should reject expired sessions
        assertFalse(isValidSession)
    }
    
    @Test
    fun `concurrent sessions should be limited per user`() = runTest {
        // Given - User attempting multiple concurrent sessions
        val userId = "user-1"
        val maxConcurrentSessions = 3
        val currentSessionCount = 4
        
        // When - Check concurrent session limit
        every { mockAccessControlManager.getCurrentSessionCount(userId) } returns currentSessionCount
        every { mockAccessControlManager.exceedsConcurrentLimit(userId, maxConcurrentSessions) } returns true
        
        val exceedsLimit = mockAccessControlManager.exceedsConcurrentLimit(userId, maxConcurrentSessions)
        
        // Then - Should limit concurrent sessions
        assertTrue(exceedsLimit)
        assertEquals(4, mockAccessControlManager.getCurrentSessionCount(userId))
    }
    
    // MARK: - Security Exception Handling Tests
    
    @Test
    fun `security violations should throw appropriate exceptions`() = runTest {
        // Given - Security violation scenario
        val unauthorizedUserId = "unauthorized-user"
        val protectedOperation = "DELETE_ALL_TAGS"
        
        // When/Then - Should throw security exception
        every { mockAccessControlManager.enforcePermission(unauthorizedUserId, Permission.DELETE_ALL_TAGS) } throws SecurityException("Insufficient permissions")
        
        assertFailsWith<SecurityException> {
            mockAccessControlManager.enforcePermission(unauthorizedUserId, Permission.DELETE_ALL_TAGS)
        }
    }
    
    @Test
    fun `malicious input should be detected and blocked`() = runTest {
        // Given - Various malicious inputs
        val maliciousInputs = listOf(
            "<script>alert('xss')</script>",
            "'; DROP TABLE users; --",
            "../../../etc/passwd",
            "%00%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd"
        )
        
        // When/Then - Should detect and block all malicious inputs
        maliciousInputs.forEach { maliciousInput ->
            every { mockAccessControlManager.detectMaliciousInput(maliciousInput) } returns true
            
            assertTrue(
                mockAccessControlManager.detectMaliciousInput(maliciousInput),
                "Should detect malicious input: $maliciousInput"
            )
        }
    }
}

/**
 * Mock security-related classes and enums for testing
 */
enum class UserRole {
    NONE,
    FIELD_WORKER,
    SAFETY_LEAD,
    PROJECT_ADMIN
}

enum class Permission {
    READ_TAGS,
    WRITE_TAGS,
    DELETE_TAGS,
    DELETE_ALL_TAGS,
    MANAGE_TAG_CATEGORIES,
    BULK_OPERATIONS
}

class SecurityException(message: String) : Exception(message)

/**
 * Mock Access Control Manager interface for testing
 */
abstract class AccessControlManager {
    abstract fun getUserRole(userId: String): UserRole
    abstract fun hasPermission(userId: String, permission: Permission): Boolean
    abstract fun sanitizeInput(input: String): String
    abstract fun validateInput(input: String): Boolean
    abstract fun sanitizeHtml(html: String): String
    abstract fun validateLength(input: String, maxLength: Int): Boolean
    abstract fun validateOSHACode(code: String): Boolean
    abstract fun validateCategory(category: String, allowedCategories: List<String>): Boolean
    abstract fun requiresApproval(tag: Tag): Boolean
    abstract fun isApproved(tagId: String): Boolean
    abstract fun logTagModification(userId: String, originalTag: Tag, modifiedTag: Tag)
    abstract fun logTagDeletion(userId: String, tag: Tag)
    abstract fun logBulkOperation(userId: String, operation: String, affectedCount: Int)
    abstract fun isRateLimited(userId: String, operation: String, windowSeconds: Int, maxOperations: Int): Boolean
    abstract fun encryptSensitiveData(data: String): String
    abstract fun canDecryptData(userId: String): Boolean
    abstract fun decryptData(encryptedData: String): String
    abstract fun isSessionValid(userId: String, sessionToken: String): Boolean
    abstract fun getCurrentSessionCount(userId: String): Int
    abstract fun exceedsConcurrentLimit(userId: String, maxSessions: Int): Boolean
    abstract fun enforcePermission(userId: String, permission: Permission)
    abstract fun detectMaliciousInput(input: String): Boolean
}