//
//  EnhancedCameraView.swift
//  HazardHawk
//
//  Created by Claude Code on 2025-08-27.
//  Copyright © 2025 HazardHawk. All rights reserved.
//

import SwiftUI
import AVFoundation
import PhotosUI

/// Enhanced camera view with professional construction safety UI
/// Matches Android CameraScreen.kt implementation with glove-friendly design
struct EnhancedCameraView: View {
    
    // MARK: - Properties
    
    @StateObject private var viewModel: CameraViewModel
    @Environment(\.hapticFeedback) private var hapticFeedback
    @Environment(\.hazardHawkTheme) private var theme
    
    // MARK: - State
    
    @State private var showingPhotoPicker = false
    @State private var showingSettings = false
    @State private var showingGallery = false
    @State private var cameraViewHeight: CGFloat = 0
    @State private var orientationChanged = false
    
    // MARK: - Initialization
    
    init(viewModel: CameraViewModel) {
        self._viewModel = StateObject(wrappedValue: viewModel)
    }
    
    // MARK: - Body
    
    var body: some View {
        ZStack {
            // Background
            HazardHawkTheme.primaryBackground
                .ignoresSafeArea()
            
            if viewModel.cameraPermissionStatus == .authorized {
                cameraInterface
            } else {
                permissionRequestView
            }
            
            // Overlay feedback views
            overlayViews
        }
        .onAppear {
            if viewModel.cameraPermissionStatus == .notDetermined {
                viewModel.requestCameraPermission()
            }
            if viewModel.locationPermissionStatus == .notDetermined {
                viewModel.requestLocationPermission()
            }
        }
    }
    
    // MARK: - Camera Interface
    
    private var cameraInterface: some View {
        GeometryReader { geometry in
            ZStack {
                // Camera preview
                CameraPreviewView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .clipped()
                
                // Professional overlay content
                VStack {
                    // Top metadata overlay
                    HStack {
                        metadataOverlay
                        Spacer()
                        analysisProgressOverlay
                    }
                    .padding(.horizontal, HazardHawkTheme.standardPadding)
                    .padding(.top, 20)
                    
                    Spacer()
                    
                    // Bottom controls
                    cameraControls
                        .padding(.bottom, 40)
                }
                
                // Grid overlay for composition
                GridOverlay()
                    .opacity(0.3)
            }
        }
    }
    
    // MARK: - Metadata Overlay
    
    private var metadataOverlay: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Image(systemName: "viewfinder")
                    .foregroundColor(HazardHawkTheme.safetyOrange)
                    .font(.system(size: 16, weight: .medium))
                
                Text("HazardHawk Smart Camera")
                    .font(.safetyCaption)
                    .fontWeight(.semibold)
                    .foregroundColor(HazardHawkTheme.primaryText)
            }
            
            Text(statusMessage)
                .font(.safetyCaption)
                .foregroundColor(HazardHawkTheme.secondaryText)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(
            RoundedRectangle(cornerRadius: HazardHawkTheme.cornerRadius)
                .fill(HazardHawkTheme.secondaryBackground.opacity(0.9))
                .constructionShadow()
        )
    }
    
    private var statusMessage: String {
        if viewModel.isCapturing {
            return "Capturing safety photo..."
        } else if viewModel.isAnalyzing {
            return "Analyzing for hazards..."
        } else {
            return "Ready to capture hazards"
        }
    }
    
    // MARK: - Analysis Progress Overlay
    
    private var analysisProgressOverlay: some View {
        Group {
            if viewModel.isAnalyzing {
                VStack(spacing: 4) {
                    HStack(spacing: 6) {
                        ProgressView()
                            .progressViewStyle(
                                CircularProgressViewStyle(
                                    tint: HazardHawkTheme.safetyOrange
                                )
                            )
                            .scaleEffect(0.8)
                        
                        Text("\(Int(viewModel.analysisProgress * 100))%")
                            .font(.safetyCaption)
                            .fontWeight(.medium)
                            .foregroundColor(HazardHawkTheme.primaryText)
                    }
                    
                    Text("AI Analysis")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(HazardHawkTheme.secondaryText)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 6)
                .background(
                    RoundedRectangle(cornerRadius: 8)
                        .fill(HazardHawkTheme.secondaryBackground.opacity(0.9))
                        .constructionShadow()
                )
            }
        }
    }
    
    // MARK: - Camera Controls
    
    private var cameraControls: some View {
        HStack(spacing: 60) {
            // Gallery button
            ControlButton(
                icon: "photo.on.rectangle.angled",
                label: "Gallery",
                action: {
                    hapticFeedback.navigationFeedback()
                    showingGallery = true
                },
                isEnabled: true
            )
            
            // Main capture button
            SafetyCaptureButton(
                onCapture: {
                    viewModel.capturePhoto()
                },
                isCapturing: viewModel.isCapturing
            )
            
            // Settings button
            ControlButton(
                icon: "gearshape.fill",
                label: "Settings",
                action: {
                    hapticFeedback.navigationFeedback()
                    showingSettings = true
                },
                isEnabled: true
            )
        }
        .padding(.horizontal, HazardHawkTheme.standardPadding)
    }
    
    // MARK: - Permission Request View
    
    private var permissionRequestView: some View {
        VStack(spacing: 24) {
            // Icon
            Image(systemName: "camera.fill")
                .font(.system(size: 80))
                .foregroundColor(HazardHawkTheme.mediumNeutral)
            
            // Title and message
            VStack(spacing: 12) {
                Text("Camera Access Required")
                    .font(.safetyTitle)
                    .fontWeight(.bold)
                    .foregroundColor(HazardHawkTheme.primaryText)
                
                Text("HazardHawk needs camera access to capture and analyze safety photos for hazard detection.")
                    .font(.safetyBody)
                    .foregroundColor(HazardHawkTheme.secondaryText)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, HazardHawkTheme.standardPadding)
            }
            
            // Permission buttons
            VStack(spacing: 12) {
                Button("Grant Camera Permission") {
                    viewModel.requestCameraPermission()
                }
                .font(.safetyButton)
                .foregroundColor(.white)
                .frame(maxWidth: .infinity)
                .frame(height: HazardHawkTheme.minimumTouchTarget)
                .background(
                    RoundedRectangle(cornerRadius: HazardHawkTheme.cornerRadius)
                        .fill(HazardHawkTheme.safetyOrange)
                )
                .constructionHeavyShadow()
                .constructionHaptics()
                
                if viewModel.locationPermissionStatus != .authorizedWhenInUse &&
                   viewModel.locationPermissionStatus != .authorizedAlways {
                    Button("Grant Location Permission") {
                        viewModel.requestLocationPermission()
                    }
                    .font(.safetyButton)
                    .foregroundColor(HazardHawkTheme.constructionBlue)
                    .frame(maxWidth: .infinity)
                    .frame(height: HazardHawkTheme.minimumTouchTarget)
                    .background(
                        RoundedRectangle(cornerRadius: HazardHawkTheme.cornerRadius)
                            .stroke(HazardHawkTheme.constructionBlue, lineWidth: 2)
                    )
                    .constructionHaptics()
                }
            }
            .padding(.horizontal, HazardHawkTheme.standardPadding)
        }
        .padding(HazardHawkTheme.largePadding)
    }
    
    // MARK: - Overlay Views
    
    private var overlayViews: some View {
        ZStack {
            // Success feedback
            SafetySuccessFeedback(
                isVisible: $viewModel.showSuccessFeedback,
                message: "Safety photo captured successfully"
            )
            
            // Error feedback
            if let error = viewModel.captureError {
                SafetyErrorFeedback(
                    isVisible: $viewModel.showErrorFeedback,
                    errorMessage: error,
                    retryAction: {
                        viewModel.retryLastCapture()
                    }
                )
            }
        }
    }
}

// MARK: - Control Button Component

struct ControlButton: View {
    let icon: String
    let label: String
    let action: () -> Void
    let isEnabled: Bool
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 24, weight: .medium))
                    .foregroundColor(
                        isEnabled ? HazardHawkTheme.primaryText : HazardHawkTheme.mediumNeutral
                    )
                
                Text(label)
                    .font(.safetyCaption)
                    .foregroundColor(
                        isEnabled ? HazardHawkTheme.secondaryText : HazardHawkTheme.mediumNeutral
                    )
            }
            .frame(width: HazardHawkTheme.minimumTouchTarget, height: HazardHawkTheme.minimumTouchTarget)
        }
        .disabled(!isEnabled)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isEnabled ? [.isButton] : [.notEnabled])
    }
}

// MARK: - Camera Preview Component

struct CameraPreviewView: UIViewRepresentable {
    
    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: UIScreen.main.bounds)
        view.backgroundColor = .black
        return view
    }
    
    func updateUIView(_ uiView: UIView, context: Context) {
        // Camera preview layer would be added here
        // This is a simplified implementation
    }
}

// MARK: - Grid Overlay Component

struct GridOverlay: View {
    var body: some View {
        GeometryReader { geometry in
            Path { path in
                let width = geometry.size.width
                let height = geometry.size.height
                
                // Vertical lines
                path.move(to: CGPoint(x: width / 3, y: 0))
                path.addLine(to: CGPoint(x: width / 3, y: height))
                
                path.move(to: CGPoint(x: 2 * width / 3, y: 0))
                path.addLine(to: CGPoint(x: 2 * width / 3, y: height))
                
                // Horizontal lines
                path.move(to: CGPoint(x: 0, y: height / 3))
                path.addLine(to: CGPoint(x: width, y: height / 3))
                
                path.move(to: CGPoint(x: 0, y: 2 * height / 3))
                path.addLine(to: CGPoint(x: width, y: 2 * height / 3))
            }
            .stroke(Color.white, lineWidth: 0.5)
        }
    }
}

// MARK: - Preview

#if DEBUG
struct EnhancedCameraView_Previews: PreviewProvider {
    static var previews: some View {
        // Mock view model for preview
        let mockViewModel = CameraViewModel(
            capturePhotoUseCase: CapturePhotoUseCase(photoRepository: MockPhotoRepository()),
            analyzePhotoUseCase: AnalyzePhotoUseCase(),
            getRecommendedTagsUseCase: GetRecommendedTagsUseCase(),
            applyTagsUseCase: ApplyTagsUseCase()
        )
        
        EnhancedCameraView(viewModel: mockViewModel)
            .preferredColorScheme(.light)
        
        EnhancedCameraView(viewModel: mockViewModel)
            .preferredColorScheme(.dark)
    }
}

// Mock repository for preview
class MockPhotoRepository: PhotoRepository {
    func savePhoto(_ photo: Photo) async throws -> Photo {
        return photo
    }
    
    func getPhoto(id: String) async throws -> Photo? {
        return nil
    }
    
    func getAllPhotos() async throws -> [Photo] {
        return []
    }
    
    func deletePhoto(id: String) async throws {
        // Mock implementation
    }
}
#endif
