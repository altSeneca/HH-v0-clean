package com.hazardhawk.data.models

import kotlinx.serialization.Serializable

/**
 * Camera settings data model for persistent storage
 * Manages all user preferences for camera functionality
 */
@Serializable
data class CameraSettings(
    // Aspect ratio settings
    val selectedAspectRatio: String = "full", // Default to full frame
    val selectedAspectRatioIndex: Int = 0,
    
    // Zoom settings
    val selectedZoom: Float = 1.0f,
    val selectedZoomIndex: Int = 1, // Default to 1x zoom
    val minZoom: Float = 0.5f,
    val maxZoom: Float = 10.0f,
    
    // UI settings
    val showGrid: Boolean = false,
    val gridType: String = "RULE_OF_THIRDS",
    val showMetadata: Boolean = true,
    val metadataPosition: String = "BOTTOM_LEFT",
    
    // Camera behavior
    val hapticFeedbackEnabled: Boolean = true,
    val volumeButtonCapture: Boolean = true,
    val autoFocusEnabled: Boolean = true,
    val flashMode: String = "AUTO", // AUTO, ON, OFF
    
    // Performance settings
    val imageQuality: Int = 95, // JPEG quality 0-100
    val imageFormat: String = "JPEG", // JPEG, RAW, HEIF
    val videoQuality: String = "1080P", // 720P, 1080P, 4K
    
    // Safety and compliance
    val burnInPrevention: Boolean = true,
    val safeAreaMargin: Float = 16f,
    val requireGPSForPhotos: Boolean = false,
    
    // AI Analysis Configuration
    val aiMode: String = "OFF", // OFF, ON_DEVICE, CLOUD, SMART
    val aiSetupCompleted: Boolean = false,
    val aiSetupSkippedDate: Long? = null,
    val lastAIPromptDate: Long? = null,
    val neverShowAIPrompt: Boolean = false,

    // On-Device AI Settings
    val selectedModel: String = "lite", // lite, standard, full
    val modelDownloadWifiOnly: Boolean = true,
    val showModelStorageWarning: Boolean = true,
    val autoAnalyzePhotos: Boolean = true,

    // Cloud AI Settings
    val geminiApiKeyConfigured: Boolean = false,
    val cloudAnalysisEnabled: Boolean = false,

    // AI Analysis Behavior
    val confidenceThreshold: Float = 0.75f,
    val autoTaggingEnabled: Boolean = true,

    // AR Configuration
    val arEnabled: Boolean = false,
    val arPrivacyMode: String = "STANDARD", // STANDARD, ENHANCED
    val arPerformanceMode: String = "BALANCED", // BATTERY_SAVER, BALANCED, MAXIMUM

    // Version for settings migration
    val settingsVersion: Int = 2
)

/**
 * Grid type enumeration
 */
enum class GridType(val value: String) {
    RULE_OF_THIRDS("RULE_OF_THIRDS"),
    CENTER_LINES("CENTER_LINES"),
    DIAGONAL("DIAGONAL"),
    GOLDEN_RATIO("GOLDEN_RATIO"),
    SAFETY_ZONES("SAFETY_ZONES");
    
    companion object {
        fun fromString(value: String): GridType {
            return values().find { it.value == value } ?: RULE_OF_THIRDS
        }
    }
}

/**
 * Metadata position enumeration
 */
enum class MetadataPosition(val value: String) {
    TOP_LEFT("TOP_LEFT"),
    TOP_RIGHT("TOP_RIGHT"),
    BOTTOM_LEFT("BOTTOM_LEFT"),
    BOTTOM_RIGHT("BOTTOM_RIGHT");
    
    companion object {
        fun fromString(value: String): MetadataPosition {
            return values().find { it.value == value } ?: BOTTOM_LEFT
        }
    }
}

/**
 * Flash mode enumeration
 */
enum class FlashMode(val value: String) {
    AUTO("AUTO"),
    ON("ON"),
    OFF("OFF"),
    TORCH("TORCH");
    
    companion object {
        fun fromString(value: String): FlashMode {
            return values().find { it.value == value } ?: AUTO
        }
    }
}

/**
 * Image format enumeration
 */
enum class ImageFormat(val value: String) {
    JPEG("JPEG"),
    RAW("RAW"),
    HEIF("HEIF");
    
    companion object {
        fun fromString(value: String): ImageFormat {
            return values().find { it.value == value } ?: JPEG
        }
    }
}

/**
 * Video quality enumeration
 */
enum class VideoQuality(val value: String) {
    HD_720P("720P"),
    FULL_HD_1080P("1080P"),
    UHD_4K("4K");

    companion object {
        fun fromString(value: String): VideoQuality {
            return values().find { it.value == value } ?: FULL_HD_1080P
        }
    }
}

/**
 * AI analysis mode enumeration
 */
enum class AIMode(val value: String, val displayName: String) {
    OFF("OFF", "Disabled"),
    ON_DEVICE("ON_DEVICE", "On-Device AI"),
    CLOUD("CLOUD", "Cloud AI"),
    SMART("SMART", "Smart Mode");

    companion object {
        fun fromString(value: String): AIMode {
            return values().find { it.value == value } ?: OFF
        }
    }
}

/**
 * AI model quality enumeration
 */
enum class AIModelQuality(val value: String, val displayName: String, val sizeMB: Int) {
    LITE("lite", "Lite (Fast)", 100),
    STANDARD("standard", "Standard (Balanced)", 200),
    FULL("full", "Full (Accurate)", 300);

    companion object {
        fun fromString(value: String): AIModelQuality {
            return values().find { it.value == value } ?: LITE
        }
    }
}

/**
 * AR privacy mode enumeration
 */
enum class ARPrivacyMode(val value: String) {
    STANDARD("STANDARD"),
    ENHANCED("ENHANCED");

    companion object {
        fun fromString(value: String): ARPrivacyMode {
            return values().find { it.value == value } ?: STANDARD
        }
    }
}

/**
 * AR performance mode enumeration
 */
enum class ARPerformanceMode(val value: String) {
    BATTERY_SAVER("BATTERY_SAVER"),
    BALANCED("BALANCED"),
    MAXIMUM("MAXIMUM");

    companion object {
        fun fromString(value: String): ARPerformanceMode {
            return values().find { it.value == value } ?: BALANCED
        }
    }
}