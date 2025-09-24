//
//  SafetySuccessFeedback.swift
//  HazardHawk
//
//  Created by Claude Code on 2025-08-27.
//  Copyright © 2025 HazardHawk. All rights reserved.
//

import SwiftUI

/// Success feedback overlay for safety photo capture operations
/// Provides clear visual confirmation with professional animations
struct SafetySuccessFeedback: View {
    
    // MARK: - Properties
    
    /// Controls visibility of the success overlay
    @Binding var isVisible: Bool
    
    /// Success message to display
    let message: String
    
    /// Optional icon to show with the success message
    let icon: String?
    
    /// Background blur effect
    let hasBlurBackground: Bool
    
    /// Auto-dismiss duration (nil for manual dismiss)
    let autoDismissAfter: TimeInterval?
    
    // MARK: - State
    
    @State private var animationOffset: CGFloat = -100
    @State private var animationOpacity: Double = 0
    @State private var animationScale: CGFloat = 0.8
    @State private var checkmarkScale: CGFloat = 0
    @State private var checkmarkRotation: Double = 0
    
    @Environment(\.hapticFeedback) private var hapticFeedback
    
    // MARK: - Initialization
    
    init(
        isVisible: Binding<Bool>,
        message: String = "Photo Captured Successfully",
        icon: String? = "checkmark.circle.fill",
        hasBlurBackground: Bool = true,
        autoDismissAfter: TimeInterval? = 2.0
    ) {
        self._isVisible = isVisible
        self.message = message
        self.icon = icon
        self.hasBlurBackground = hasBlurBackground
        self.autoDismissAfter = autoDismissAfter
    }
    
    // MARK: - Body
    
    var body: some View {
        ZStack {
            // Background overlay
            if hasBlurBackground {
                Color.black.opacity(0.3)
                    .ignoresSafeArea()
                    .opacity(animationOpacity)
            }
            
            // Success card
            VStack(spacing: 20) {
                // Success icon with animation
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 60, weight: .bold))
                        .foregroundColor(HazardHawkTheme.safetyGreen)
                        .scaleEffect(checkmarkScale)
                        .rotationEffect(.degrees(checkmarkRotation))
                }
                
                // Success message
                VStack(spacing: 8) {
                    Text("Success!")
                        .font(.safetyTitle)
                        .fontWeight(.bold)
                        .foregroundColor(HazardHawkTheme.primaryText)
                    
                    Text(message)
                        .font(.safetyBody)
                        .foregroundColor(HazardHawkTheme.secondaryText)
                        .multilineTextAlignment(.center)
                        .lineLimit(3)
                }
                
                // Progress indicator for auto-dismiss
                if autoDismissAfter != nil {
                    ProgressView()
                        .progressViewStyle(
                            CircularProgressViewStyle(
                                tint: HazardHawkTheme.safetyGreen
                            )
                        }
                        .scaleEffect(0.8)
                }
            }
            .padding(HazardHawkTheme.largePadding)
            .background(
                RoundedRectangle(cornerRadius: HazardHawkTheme.heavyCornerRadius)
                    .fill(HazardHawkTheme.secondaryBackground)
                    .constructionShadow()
            )
            .scaleEffect(animationScale)
            .offset(y: animationOffset)
            .opacity(animationOpacity)
        }
        .opacity(isVisible ? 1 : 0)
        .animation(.easeInOut(duration: 0.3), value: isVisible)
        .onChange(of: isVisible) { visible in
            if visible {
                showSuccessAnimation()
            } else {
                resetAnimation()
            }
        }
    }
    
    // MARK: - Private Methods
    
    private func showSuccessAnimation() {
        // Trigger haptic feedback
        hapticFeedback.captureSuccessFeedback()
        
        // Animate overlay appearance
        withAnimation(.easeOut(duration: 0.3)) {
            animationOffset = 0
            animationOpacity = 1
            animationScale = 1.0
        }
        
        // Animate checkmark with delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            withAnimation(.spring(response: 0.4, dampingFraction: 0.6)) {
                checkmarkScale = 1.0
                checkmarkRotation = 360
            }
        }
        
        // Auto-dismiss if configured
        if let dismissDelay = autoDismissAfter {
            DispatchQueue.main.asyncAfter(deadline: .now() + dismissDelay) {
                dismissSuccess()
            }
        }
    }
    
    private func dismissSuccess() {
        withAnimation(.easeIn(duration: 0.2)) {
            animationOffset = -50
            animationOpacity = 0
            animationScale = 0.9
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            isVisible = false
        }
    }
    
    private func resetAnimation() {
        animationOffset = -100
        animationOpacity = 0
        animationScale = 0.8
        checkmarkScale = 0
        checkmarkRotation = 0
    }
}

// MARK: - Success Feedback Variants

struct SafetyAnalysisSuccessFeedback: View {
    @Binding var isVisible: Bool
    let hazardCount: Int
    
    var body: some View {
        SafetySuccessFeedback(
            isVisible: $isVisible,
            message: "Analysis Complete - \(hazardCount) hazards detected",
            icon: "eye.circle.fill",
            autoDismissAfter: 3.0
        )
    }
}

struct SafetyUploadSuccessFeedback: View {
    @Binding var isVisible: Bool
    
    var body: some View {
        SafetySuccessFeedback(
            isVisible: $isVisible,
            message: "Photos uploaded to cloud storage",
            icon: "icloud.and.arrow.up.fill",
            autoDismissAfter: 2.0
        )
    }
}

struct SafetyExportSuccessFeedback: View {
    @Binding var isVisible: Bool
    let fileName: String
    
    var body: some View {
        SafetySuccessFeedback(
            isVisible: $isVisible,
            message: "Report exported: \(fileName)",
            icon: "doc.circle.fill",
            autoDismissAfter: 2.5
        )
    }
}

struct SafetyComplianceSuccessFeedback: View {
    @Binding var isVisible: Bool
    
    var body: some View {
        SafetySuccessFeedback(
            isVisible: $isVisible,
            message: "Compliance status updated",
            icon: "shield.checkered",
            autoDismissAfter: 2.0
        )
    }
}

// MARK: - Error Feedback Overlay

struct SafetyErrorFeedback: View {
    @Binding var isVisible: Bool
    let errorMessage: String
    let retryAction: (() -> Void)?
    
    @State private var animationOffset: CGFloat = -100
    @State private var animationOpacity: Double = 0
    @State private var animationScale: CGFloat = 0.8
    
    @Environment(\.hapticFeedback) private var hapticFeedback
    
    init(
        isVisible: Binding<Bool>,
        errorMessage: String,
        retryAction: (() -> Void)? = nil
    ) {
        self._isVisible = isVisible
        self.errorMessage = errorMessage
        self.retryAction = retryAction
    }
    
    var body: some View {
        ZStack {
            Color.black.opacity(0.3)
                .ignoresSafeArea()
                .opacity(animationOpacity)
            
            VStack(spacing: 20) {
                // Error icon
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 60, weight: .bold))
                    .foregroundColor(HazardHawkTheme.dangerRed)
                
                // Error message
                VStack(spacing: 8) {
                    Text("Error")
                        .font(.safetyTitle)
                        .fontWeight(.bold)
                        .foregroundColor(HazardHawkTheme.primaryText)
                    
                    Text(errorMessage)
                        .font(.safetyBody)
                        .foregroundColor(HazardHawkTheme.secondaryText)
                        .multilineTextAlignment(.center)
                        .lineLimit(5)
                }
                
                // Action buttons
                HStack(spacing: 16) {
                    Button("Dismiss") {
                        isVisible = false
                    }
                    .font(.safetyButton)
                    .foregroundColor(HazardHawkTheme.secondaryText)
                    
                    if let retryAction = retryAction {
                        Button("Retry") {
                            retryAction()
                            isVisible = false
                        }
                        .font(.safetyButton)
                        .foregroundColor(HazardHawkTheme.safetyOrange)
                        .fontWeight(.semibold)
                    }
                }
            }
            .padding(HazardHawkTheme.largePadding)
            .background(
                RoundedRectangle(cornerRadius: HazardHawkTheme.heavyCornerRadius)
                    .fill(HazardHawkTheme.secondaryBackground)
                    .constructionShadow()
            )
            .scaleEffect(animationScale)
            .offset(y: animationOffset)
            .opacity(animationOpacity)
        }
        .opacity(isVisible ? 1 : 0)
        .onChange(of: isVisible) { visible in
            if visible {
                showErrorAnimation()
            } else {
                resetErrorAnimation()
            }
        }
    }
    
    private func showErrorAnimation() {
        hapticFeedback.captureErrorFeedback()
        
        withAnimation(.easeOut(duration: 0.3)) {
            animationOffset = 0
            animationOpacity = 1
            animationScale = 1.0
        }
    }
    
    private func resetErrorAnimation() {
        animationOffset = -100
        animationOpacity = 0
        animationScale = 0.8
    }
}

// MARK: - Preview

#if DEBUG
struct SafetySuccessFeedback_Previews: PreviewProvider {
    static var previews: some View {
        VStack {
            SafetySuccessFeedback(
                isVisible: .constant(true),
                message: "Safety photo captured and analyzed"
            )
        }
        .background(Color.gray.opacity(0.2))
    }
}
#endif
