//
//  HapticFeedback+Extensions.swift
//  HazardHawk
//
//  Created by Claude Code on 2025-08-27.
//  Copyright © 2025 HazardHawk. All rights reserved.
//

import UIKit
import SwiftUI

/// Haptic feedback manager for construction-friendly tactile responses
/// Provides appropriate haptic patterns for safety-critical interactions
class HapticFeedbackManager: ObservableObject {
    
    // MARK: - Singleton
    
    static let shared = HapticFeedbackManager()
    
    // MARK: - Private Properties
    
    private let lightImpactGenerator = UIImpactFeedbackGenerator(style: .light)
    private let mediumImpactGenerator = UIImpactFeedbackGenerator(style: .medium)
    private let heavyImpactGenerator = UIImpactFeedbackGenerator(style: .heavy)
    private let notificationGenerator = UINotificationFeedbackGenerator()
    private let selectionGenerator = UISelectionFeedbackGenerator()
    
    // MARK: - Initialization
    
    private init() {
        // Pre-prepare generators for better performance
        prepareGenerators()
    }
    
    // MARK: - Public Methods
    
    /// Provides impact feedback with specified intensity
    /// - Parameter style: The intensity of the haptic feedback
    func impactFeedback(_ style: UIImpactFeedbackGenerator.FeedbackStyle) {
        guard UIDevice.current.userInterfaceIdiom == .phone else { return }
        
        switch style {
        case .light:
            lightImpactGenerator.impactOccurred()
        case .medium:
            mediumImpactGenerator.impactOccurred()
        case .heavy:
            heavyImpactGenerator.impactOccurred()
        @unknown default:
            mediumImpactGenerator.impactOccurred()
        }
    }
    
    /// Provides notification feedback for system events
    /// - Parameter type: The type of notification feedback
    func notificationFeedback(_ type: UINotificationFeedbackGenerator.FeedbackType) {
        guard UIDevice.current.userInterfaceIdiom == .phone else { return }
        notificationGenerator.notificationOccurred(type)
    }
    
    /// Provides selection feedback for UI navigation
    func selectionFeedback() {
        guard UIDevice.current.userInterfaceIdiom == .phone else { return }
        selectionGenerator.selectionChanged()
    }
    
    // MARK: - Safety-Specific Haptic Patterns
    
    /// Haptic feedback for successful photo capture
    func captureSuccessFeedback() {
        notificationFeedback(.success)
    }
    
    /// Haptic feedback for capture failure or error
    func captureErrorFeedback() {
        notificationFeedback(.error)
    }
    
    /// Haptic feedback for hazard detection completion
    func hazardDetectedFeedback() {
        // Double impact for hazard detection
        impactFeedback(.heavy)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.impactFeedback(.medium)
        }
    }
    
    /// Haptic feedback for safety compliance confirmation
    func complianceConfirmedFeedback() {
        notificationFeedback(.success)
    }
    
    /// Haptic feedback for critical safety warning
    func criticalWarningFeedback() {
        // Triple impact pattern for critical warnings
        impactFeedback(.heavy)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            self.impactFeedback(.heavy)
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                self.impactFeedback(.heavy)
            }
        }
    }
    
    /// Haptic feedback for button press in construction environment
    func constructionButtonFeedback() {
        // Slightly stronger feedback for glove-wearing users
        impactFeedback(.medium)
    }
    
    /// Haptic feedback for navigation and menu interactions
    func navigationFeedback() {
        selectionFeedback()
    }
    
    /// Haptic feedback for drag and drop operations
    func dragStartFeedback() {
        impactFeedback(.light)
    }
    
    /// Haptic feedback for successful drop operation
    func dropSuccessFeedback() {
        impactFeedback(.medium)
    }
    
    // MARK: - Private Methods
    
    private func prepareGenerators() {
        lightImpactGenerator.prepare()
        mediumImpactGenerator.prepare()
        heavyImpactGenerator.prepare()
        notificationGenerator.prepare()
        selectionGenerator.prepare()
    }
}

// MARK: - SwiftUI View Extensions

extension View {
    /// Adds haptic feedback to button taps
    /// - Parameter style: The haptic feedback style
    /// - Returns: Modified view with haptic feedback
    func hapticFeedback(_ style: UIImpactFeedbackGenerator.FeedbackStyle = .medium) -> some View {
        self.onTapGesture {
            HapticFeedbackManager.shared.impactFeedback(style)
        }
    }
    
    /// Adds construction-appropriate haptic feedback
    /// - Returns: Modified view with construction haptic feedback
    func constructionHaptics() -> some View {
        self.onTapGesture {
            HapticFeedbackManager.shared.constructionButtonFeedback()
        }
    }
    
    /// Adds navigation haptic feedback for menu items and navigation
    /// - Returns: Modified view with navigation haptic feedback
    func navigationHaptics() -> some View {
        self.onTapGesture {
            HapticFeedbackManager.shared.navigationFeedback()
        }
    }
}

// MARK: - Haptic Feedback Environment

struct HapticFeedbackKey: EnvironmentKey {
    static let defaultValue = HapticFeedbackManager.shared
}

extension EnvironmentValues {
    var hapticFeedback: HapticFeedbackManager {
        get { self[HapticFeedbackKey.self] }
        set { self[HapticFeedbackKey.self] = newValue }
    }
}

// MARK: - Haptic Patterns for Specific UI Elements

extension HapticFeedbackManager {
    
    /// Feedback pattern for photo gallery selection
    func gallerySelectionFeedback() {
        selectionFeedback()
    }
    
    /// Feedback pattern for bulk photo operations
    func bulkOperationFeedback() {
        impactFeedback(.medium)
    }
    
    /// Feedback pattern for AI analysis completion
    func analysisCompleteFeedback() {
        // Gentle success pattern
        impactFeedback(.light)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
            self.impactFeedback(.light)
        }
    }
    
    /// Feedback pattern for tag application
    func tagAppliedFeedback() {
        impactFeedback(.light)
    }
    
    /// Feedback pattern for settings changes
    func settingsChangeFeedback() {
        impactFeedback(.light)
    }
    
    /// Feedback pattern for export operations
    func exportStartFeedback() {
        impactFeedback(.medium)
    }
    
    /// Feedback pattern for successful export
    func exportCompleteFeedback() {
        notificationFeedback(.success)
    }
}
