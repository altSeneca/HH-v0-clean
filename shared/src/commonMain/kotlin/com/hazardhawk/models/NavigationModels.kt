package com.hazardhawk.models

/**
 * Navigation destination system for type-safe routing throughout HazardHawk
 * Provides consistent navigation structure across all platforms
 */
sealed class HazardHawkDestination(val route: String) {
    object Dashboard : HazardHawkDestination("dashboard")
    object Camera : HazardHawkDestination("camera")
    object Gallery : HazardHawkDestination("gallery")
    
    data class PDFExport(
        val documentId: String,
        val documentType: DocumentType
    ) : HazardHawkDestination("pdf_export/{documentId}/{documentType}") {
        companion object {
            const val ROUTE_TEMPLATE = "pdf_export/{documentId}/{documentType}"
            
            /**
             * Create actual route with parameters substituted
             */
            fun createRoute(documentId: String, documentType: DocumentType): String {
                return "pdf_export/$documentId/${documentType.name}"
            }
        }
    }
    
    data class DocumentGeneration(
        val documentType: DocumentType
    ) : HazardHawkDestination("document_generation/{documentType}") {
        companion object {
            const val ROUTE_TEMPLATE = "document_generation/{documentType}"
            
            /**
             * Create actual route with parameters substituted
             */
            fun createRoute(documentType: DocumentType): String {
                return "document_generation/${documentType.name}"
            }
        }
    }
    
    data class PhotoDetail(
        val photoId: String
    ) : HazardHawkDestination("photo_detail/{photoId}") {
        companion object {
            const val ROUTE_TEMPLATE = "photo_detail/{photoId}"
            
            /**
             * Create actual route with parameters substituted
             */
            fun createRoute(photoId: String): String {
                return "photo_detail/$photoId"
            }
        }
    }
    
    object Settings : HazardHawkDestination("settings")
    object UserProfile : HazardHawkDestination("user_profile")
    
    /**
     * Get the base route without parameters for navigation setup
     */
    fun getBaseRoute(): String {
        return when (this) {
            is PDFExport -> ROUTE_TEMPLATE
            is DocumentGeneration -> ROUTE_TEMPLATE
            is PhotoDetail -> ROUTE_TEMPLATE
            else -> route
        }
    }
}

/**
 * Document types for generation and PDF export
 * Maps to OSHA-compliant construction safety documentation
 */
enum class DocumentType(val displayName: String) {
    PTP("Pre-Task Plan"),
    TOOLBOX_TALK("Toolbox Talk"),
    INCIDENT_REPORT("Incident Report");
    
    /**
     * Get the file extension typically used for this document type
     */
    fun getFileExtension(): String = "pdf"
    
    /**
     * Get the MIME type for this document type
     */
    fun getMimeType(): String = "application/pdf"
    
    /**
     * Check if this document type requires photos
     */
    fun requiresPhotos(): Boolean {
        return when (this) {
            INCIDENT_REPORT -> true
            PTP -> false
            TOOLBOX_TALK -> false
        }
    }
    
    /**
     * Check if this document type requires signatures
     */
    fun requiresSignature(): Boolean {
        return when (this) {
            PTP -> true
            TOOLBOX_TALK -> true
            INCIDENT_REPORT -> true
        }
    }
}

/**
 * Navigation parameters for type-safe parameter passing
 */
data class NavigationParams(
    val documentId: String? = null,
    val documentType: DocumentType? = null,
    val photoId: String? = null,
    val preselectedPhotoIds: List<String>? = null,
    val returnDestination: HazardHawkDestination? = null
) {
    companion object {
        /**
         * Create parameters for PDF export navigation
         */
        fun forPDFExport(
            documentId: String, 
            documentType: DocumentType,
            returnTo: HazardHawkDestination? = null
        ): NavigationParams {
            return NavigationParams(
                documentId = documentId,
                documentType = documentType,
                returnDestination = returnTo
            )
        }
        
        /**
         * Create parameters for document generation navigation
         */
        fun forDocumentGeneration(
            documentType: DocumentType,
            returnTo: HazardHawkDestination? = null
        ): NavigationParams {
            return NavigationParams(
                documentType = documentType,
                returnDestination = returnTo
            )
        }
        
        /**
         * Create parameters for photo detail navigation
         */
        fun forPhotoDetail(
            photoId: String,
            returnTo: HazardHawkDestination? = null
        ): NavigationParams {
            return NavigationParams(
                photoId = photoId,
                returnDestination = returnTo
            )
        }
        
        /**
         * Create parameters for gallery navigation with preselected photos
         */
        fun forGallery(
            preselectedPhotoIds: List<String>? = null,
            returnTo: HazardHawkDestination? = null
        ): NavigationParams {
            return NavigationParams(
                preselectedPhotoIds = preselectedPhotoIds,
                returnDestination = returnTo
            )
        }
    }
}