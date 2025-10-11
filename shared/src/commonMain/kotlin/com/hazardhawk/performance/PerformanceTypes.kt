package com.hazardhawk.performance

/**
 * Types of workflows that can be monitored for performance tracking.
 * 
 * These enum values represent different end-to-end user journeys in the HazardHawk
 * safety inspection platform.
 */
enum class WorkflowType {
    /** Photo capture and AI analysis workflow */
    PHOTO_CAPTURE_ANALYSIS,
    
    /** Complete safety inspection workflow */
    SAFETY_INSPECTION,
    
    /** Incident report creation and submission */
    INCIDENT_REPORTING,
    
    /** Pre-Task Plan (PTP) generation */
    PTP_GENERATION,
    
    /** Toolbox talk document creation */
    TOOLBOX_TALK_CREATION,
    
    /** Batch analysis of multiple photos */
    BATCH_ANALYSIS,
    
    /** Report generation and export */
    REPORT_GENERATION
}

/**
 * Types of steps within a workflow for granular performance tracking.
 * 
 * These enum values represent individual operations that make up a complete workflow.
 */
enum class StepType {
    /** Capturing photo from camera or selecting from gallery */
    PHOTO_CAPTURE,
    
    /** Image preprocessing (compression, metadata extraction, etc.) */
    IMAGE_PREPROCESSING,
    
    /** AI model inference and analysis */
    AI_ANALYSIS,
    
    /** Processing and parsing AI analysis results */
    RESULTS_PROCESSING,
    
    /** Cache read/write operations */
    CACHE_OPERATIONS,
    
    /** Database queries and updates */
    DATABASE_OPERATIONS,
    
    /** Report generation (structure, formatting) */
    REPORT_GENERATION,
    
    /** PDF document creation and export */
    PDF_CREATION,
    
    /** File system operations (read, write, delete) */
    FILE_OPERATIONS,
    
    /** UI rendering and updates */
    UI_RENDERING,
    
    /** Network requests and responses */
    NETWORK_OPERATIONS
}
