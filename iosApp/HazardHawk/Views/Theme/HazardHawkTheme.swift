//
//  HazardHawkTheme.swift
//  HazardHawk
//
//  Created by Claude Code on 2025-08-27.
//  Copyright © 2025 HazardHawk. All rights reserved.
//

import SwiftUI

/// HazardHawk Construction Safety Theme
/// Matches Android Color.kt implementation for cross-platform consistency
struct HazardHawkTheme {
    
    // MARK: - Construction Safety Colors (matching Android)
    
    /// Primary safety orange for capture buttons and primary actions
    static let safetyOrange = Color(red: 1.0, green: 0.4, blue: 0.0) // #FF6600
    
    /// High-contrast danger red for critical warnings
    static let dangerRed = Color(red: 0.863, green: 0.208, blue: 0.271) // #DC3545
    
    /// High-visibility warning yellow for cautions
    static let warningYellow = Color(red: 1.0, green: 0.8, blue: 0.0) // #FFCC00
    
    /// Safety green for success states and compliance
    static let safetyGreen = Color(red: 0.157, green: 0.655, blue: 0.271) // #28A745
    
    /// Professional construction blue for secondary actions
    static let constructionBlue = Color(red: 0.0, green: 0.337, blue: 0.702) // #0056B3
    
    // MARK: - Neutral Colors
    
    /// Dark neutral for text on light backgrounds
    static let darkNeutral = Color(red: 0.2, green: 0.2, blue: 0.2) // #333333
    
    /// Light neutral for text on dark backgrounds
    static let lightNeutral = Color(red: 0.95, green: 0.95, blue: 0.95) // #F2F2F2
    
    /// Medium neutral for disabled states
    static let mediumNeutral = Color(red: 0.6, green: 0.6, blue: 0.6) // #999999
    
    // MARK: - Environment Adaptive Colors
    
    /// Primary background that adapts to environment brightness
    static let primaryBackground = Color(light: Color.white, dark: darkNeutral)
    
    /// Secondary background for cards and overlays
    static let secondaryBackground = Color(light: Color(white: 0.98), dark: Color(white: 0.15))
    
    /// Primary text color that adapts to background
    static let primaryText = Color(light: darkNeutral, dark: lightNeutral)
    
    /// Secondary text color for less important content
    static let secondaryText = Color(light: mediumNeutral, dark: Color(white: 0.7))
    
    // MARK: - Construction-Specific UI Constants
    
    /// Minimum touch target size for glove-friendly interaction
    static let minimumTouchTarget: CGFloat = 64
    
    /// Enhanced capture button size for safety-critical actions
    static let captureButtonSize: CGFloat = 88
    
    /// Standard corner radius for construction app UI elements
    static let cornerRadius: CGFloat = 12
    
    /// Heavy corner radius for major UI elements
    static let heavyCornerRadius: CGFloat = 20
    
    /// Standard padding for construction-friendly spacing
    static let standardPadding: CGFloat = 16
    
    /// Large padding for major UI sections
    static let largePadding: CGFloat = 24
    
    /// Animation duration for feedback animations
    static let feedbackAnimationDuration: Double = 0.3
    
    /// Success animation scale factor
    static let successAnimationScale: CGFloat = 1.15
}

// MARK: - Color Extensions

extension Color {
    /// Creates a color that adapts to light/dark appearance
    init(light: Color, dark: Color) {
        self = Color(.init { traits in
            switch traits.userInterfaceStyle {
            case .dark:
                return UIColor(dark)
            default:
                return UIColor(light)
            }
        })
    }
}

// MARK: - SwiftUI Environment Integration

struct HazardHawkThemeKey: EnvironmentKey {
    static let defaultValue = HazardHawkTheme.self
}

extension EnvironmentValues {
    var hazardHawkTheme: HazardHawkTheme.Type {
        get { self[HazardHawkThemeKey.self] }
        set { self[HazardHawkThemeKey.self] = newValue }
    }
}

// MARK: - Professional Typography

extension Font {
    /// Large, bold headline for safety-critical information
    static let safetyHeadline = Font.system(size: 28, weight: .bold, design: .default)
    
    /// Medium title for section headers
    static let safetyTitle = Font.system(size: 20, weight: .semibold, design: .default)
    
    /// Body text optimized for readability in construction environments
    static let safetyBody = Font.system(size: 16, weight: .regular, design: .default)
    
    /// Small caption text for metadata and secondary information
    static let safetyCaption = Font.system(size: 12, weight: .medium, design: .default)
    
    /// Button text with appropriate weight for touch targets
    static let safetyButton = Font.system(size: 16, weight: .semibold, design: .default)
}

// MARK: - Professional Shadows

extension View {
    /// Professional shadow for elevated construction app elements
    func constructionShadow() -> some View {
        self.shadow(
            color: Color.black.opacity(0.15),
            radius: 8,
            x: 0,
            y: 4
        )
    }
    
    /// Heavy shadow for floating action buttons and critical elements
    func constructionHeavyShadow() -> some View {
        self.shadow(
            color: Color.black.opacity(0.25),
            radius: 12,
            x: 0,
            y: 6
        )
    }
}
