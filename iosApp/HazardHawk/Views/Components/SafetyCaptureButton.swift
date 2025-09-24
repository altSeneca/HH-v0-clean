//
//  SafetyCaptureButton.swift
//  HazardHawk
//
//  Created by Claude Code on 2025-08-27.
//  Copyright © 2025 HazardHawk. All rights reserved.
//

import SwiftUI
import UIKit

/// Enhanced safety capture button with haptic feedback and professional construction styling
/// Matches Android implementation with 88pt minimum size for glove-friendly interaction
struct SafetyCaptureButton: View {
    
    // MARK: - Properties
    
    /// Capture action callback
    let onCapture: () -> Void
    
    /// Loading state for capture in progress
    let isCapturing: Bool
    
    /// Button size (defaults to construction-safe 88pt)
    let size: CGFloat
    
    /// Optional custom label
    let label: String?
    
    // MARK: - State
    
    @State private var isPressed = false
    @State private var animationScale: CGFloat = 1.0
    @Environment(\.hazardHawkTheme) private var theme
    
    // MARK: - Initialization
    
    init(
        onCapture: @escaping () -> Void,
        isCapturing: Bool = false,
        size: CGFloat = HazardHawkTheme.captureButtonSize,
        label: String? = nil
    ) {
        self.onCapture = onCapture
        self.isCapturing = isCapturing
        self.size = size
        self.label = label
    }
    
    // MARK: - Body
    
    var body: some View {
        Button(action: handleCapture) {
            ZStack {
                // Background circle with safety orange color
                Circle()
                    .fill(HazardHawkTheme.safetyOrange)
                    .frame(width: size, height: size)
                    .overlay(
                        Circle()
                            .stroke(
                                isPressed ? HazardHawkTheme.lightNeutral : Color.clear,
                                lineWidth: 3
                            )
                    )
                
                // Content overlay
                if isCapturing {
                    // Loading indicator
                    ProgressView()
                        .progressViewStyle(
                            CircularProgressViewStyle(
                                tint: HazardHawkTheme.lightNeutral
                            )
                        )
                        .scaleEffect(1.5)
                } else {
                    VStack(spacing: 4) {
                        // Camera icon
                        Image(systemName: "camera.fill")
                            .font(.system(size: size * 0.35, weight: .semibold))
                            .foregroundColor(HazardHawkTheme.lightNeutral)
                        
                        // Optional label
                        if let label = label {
                            Text(label)
                                .font(.safetyCaption)
                                .foregroundColor(HazardHawkTheme.lightNeutral)
                                .multilineTextAlignment(.center)
                        }
                    }
                }
            }
        }
        .buttonStyle(SafetyCaptureButtonStyle())
        .scaleEffect(animationScale)
        .disabled(isCapturing)
        .onChange(of: isPressed) { pressed in
            withAnimation(.easeInOut(duration: 0.1)) {
                animationScale = pressed ? 0.95 : 1.0
            }
        }
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHint("Capture safety photo with AI analysis")
        .accessibilityAddTraits(isCapturing ? [.notEnabled] : [.isButton])
    }
    
    // MARK: - Private Methods
    
    private func handleCapture() {
        guard !isCapturing else { return }
        
        // Trigger haptic feedback
        HapticFeedbackManager.shared.impactFeedback(.heavy)
        
        // Visual feedback animation
        withAnimation(.easeInOut(duration: HazardHawkTheme.feedbackAnimationDuration)) {
            animationScale = HazardHawkTheme.successAnimationScale
        }
        
        // Return to normal scale after animation
        DispatchQueue.main.asyncAfter(deadline: .now() + HazardHawkTheme.feedbackAnimationDuration) {
            withAnimation(.easeInOut(duration: HazardHawkTheme.feedbackAnimationDuration)) {
                animationScale = 1.0
            }
        }
        
        // Execute capture action
        onCapture()
    }
    
    private var accessibilityLabel: String {
        if isCapturing {
            return "Capturing safety photo"
        } else {
            return label ?? "Capture safety photo"
        }
    }
}

// MARK: - Custom Button Style

struct SafetyCaptureButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.95 : 1.0)
            .opacity(configuration.isPressed ? 0.9 : 1.0)
            .constructionHeavyShadow()
    }
}

// MARK: - Variants

struct CompactSafetyCaptureButton: View {
    let onCapture: () -> Void
    let isCapturing: Bool
    
    var body: some View {
        SafetyCaptureButton(
            onCapture: onCapture,
            isCapturing: isCapturing,
            size: HazardHawkTheme.minimumTouchTarget
        )
    }
}

struct LabeledSafetyCaptureButton: View {
    let onCapture: () -> Void
    let isCapturing: Bool
    let label: String
    
    var body: some View {
        SafetyCaptureButton(
            onCapture: onCapture,
            isCapturing: isCapturing,
            label: label
        )
    }
}

// MARK: - Preview

#if DEBUG
struct SafetyCaptureButton_Previews: PreviewProvider {
    static var previews: some View {
        VStack(spacing: 40) {
            // Standard capture button
            SafetyCaptureButton(
                onCapture: { print("Capture tapped") },
                isCapturing: false
            )
            
            // Loading state
            SafetyCaptureButton(
                onCapture: { },
                isCapturing: true
            )
            
            // Compact variant
            CompactSafetyCaptureButton(
                onCapture: { print("Compact capture") },
                isCapturing: false
            )
            
            // Labeled variant
            LabeledSafetyCaptureButton(
                onCapture: { print("Labeled capture") },
                isCapturing: false,
                label: "Capture Hazard"
            )
        }
        .padding()
        .background(HazardHawkTheme.primaryBackground)
        .preferredColorScheme(.light)
        
        VStack(spacing: 40) {
            SafetyCaptureButton(
                onCapture: { print("Dark mode capture") },
                isCapturing: false
            )
        }
        .padding()
        .background(HazardHawkTheme.primaryBackground)
        .preferredColorScheme(.dark)
    }
}
#endif
