//
//  CameraViewModel.swift
//  HazardHawk
//
//  Created by Claude Code on 2025-08-27.
//  Copyright © 2025 HazardHawk. All rights reserved.
//

import SwiftUI
import AVFoundation
import CoreLocation
import PhotosUI
import Combine

// Import KMP shared module
import shared

/// Camera view model integrating with KMP shared business logic
/// Handles photo capture, location services, and AI analysis coordination
@MainActor
class CameraViewModel: NSObject, ObservableObject {
    
    // MARK: - Published Properties
    
    @Published var isCapturing = false
    @Published var captureError: String?
    @Published var showSuccessFeedback = false
    @Published var showErrorFeedback = false
    @Published var showTagDialog = false
    @Published var currentPhoto: Photo?
    @Published var recommendedTags: [Tag] = []
    @Published var cameraPermissionStatus: AVAuthorizationStatus = .notDetermined
    @Published var locationPermissionStatus: CLAuthorizationStatus = .notDetermined
    @Published var recentPhotos: [Photo] = []
    @Published var isAnalyzing = false
    @Published var analysisProgress: Double = 0.0
    
    // MARK: - KMP Integration Properties
    
    private let capturePhotoUseCase: CapturePhotoUseCase
    private let analyzePhotoUseCase: AnalyzePhotoUseCase
    private let getRecommendedTagsUseCase: GetRecommendedTagsUseCase
    private let applyTagsUseCase: ApplyTagsUseCase
    
    // MARK: - Camera Properties
    
    private let captureSession = AVCaptureSession()
    private var photoOutput: AVCapturePhotoOutput?
    private var deviceInput: AVCaptureDeviceInput?
    
    // MARK: - Location Properties
    
    private let locationManager = CLLocationManager()
    private var currentLocation: CLLocation?
    
    // MARK: - Private Properties
    
    private var cancellables = Set<AnyCancellable>()
    private let hapticFeedback = HapticFeedbackManager.shared
    private let fileManager = FileManager.default
    private var photoCounter = 0
    
    // MARK: - Initialization
    
    init(
        capturePhotoUseCase: CapturePhotoUseCase,
        analyzePhotoUseCase: AnalyzePhotoUseCase,
        getRecommendedTagsUseCase: GetRecommendedTagsUseCase,
        applyTagsUseCase: ApplyTagsUseCase
    ) {
        self.capturePhotoUseCase = capturePhotoUseCase
        self.analyzePhotoUseCase = analyzePhotoUseCase
        self.getRecommendedTagsUseCase = getRecommendedTagsUseCase
        self.applyTagsUseCase = applyTagsUseCase
        
        super.init()
        
        setupCamera()
        setupLocationServices()
        checkPermissions()
    }
    
    // MARK: - Public Methods
    
    func capturePhoto() {
        Task {
            await performPhotoCapture()
        }
    }
    
    func retryLastCapture() {
        capturePhoto()
    }
    
    func clearError() {
        captureError = nil
        showErrorFeedback = false
    }
    
    func dismissTagDialog() {
        showTagDialog = false
        currentPhoto = nil
    }
    
    func applyTagsToPhoto(complianceStatus: ComplianceStatus, tags: [Tag]) {
        guard let photo = currentPhoto else { return }
        
        Task {
            do {
                // Apply tags using KMP use case
                let params = ApplyTagsUseCaseParams(
                    photoId: photo.id,
                    complianceStatus: complianceStatus,
                    tags: tags
                )
                
                let _ = try await applyTagsUseCase.invoke(params: params)
                
                // Update UI
                showSuccessFeedback = true
                dismissTagDialog()
                
                // Refresh recent photos
                await loadRecentPhotos()
                
            } catch {
                captureError = "Failed to apply tags: \(error.localizedDescription)"
                showErrorFeedback = true
            }
        }
    }
    
    func requestCameraPermission() {
        AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
            DispatchQueue.main.async {
                self?.cameraPermissionStatus = granted ? .authorized : .denied
                if granted {
                    self?.setupCamera()
                }
            }
        }
    }
    
    func requestLocationPermission() {
        locationManager.requestWhenInUseAuthorization()
    }
    
    // MARK: - Private Methods
    
    private func setupCamera() {
        guard cameraPermissionStatus == .authorized else { return }
        
        captureSession.sessionPreset = .photo
        
        // Setup camera input
        guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
              let input = try? AVCaptureDeviceInput(device: camera) else {
            captureError = "Unable to access camera"
            return
        }
        
        if captureSession.canAddInput(input) {
            captureSession.addInput(input)
            deviceInput = input
        }
        
        // Setup photo output
        let output = AVCapturePhotoOutput()
        if captureSession.canAddOutput(output) {
            captureSession.addOutput(output)
            photoOutput = output
        }
        
        // Start session on background queue
        DispatchQueue.global(qos: .background).async {
            self.captureSession.startRunning()
        }
    }
    
    private func setupLocationServices() {
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }
    
    private func checkPermissions() {
        cameraPermissionStatus = AVCaptureDevice.authorizationStatus(for: .video)
        locationPermissionStatus = locationManager.authorizationStatus
    }
    
    private func performPhotoCapture() async {
        guard !isCapturing else { return }
        
        isCapturing = true
        captureError = nil
        
        do {
            // Capture photo with AVFoundation
            let photoData = try await capturePhotoData()
            
            // Save to local storage
            let filePath = try await savePhotoToDocuments(photoData)
            
            // Create photo metadata
            let metadata = createPhotoMetadata()
            
            // Create capture parameters for KMP
            let location = currentLocation.map { loc in
                Location(
                    latitude: loc.coordinate.latitude,
                    longitude: loc.coordinate.longitude,
                    address: nil,
                    accuracy: Float(loc.horizontalAccuracy)
                )
            }
            
            let captureParams = CaptureParams(
                filePath: filePath,
                location: location,
                project: nil, // TODO: Get current project from user session
                metadata: metadata
            )
            
            // Save photo using KMP use case
            let result = try await capturePhotoUseCase.invoke(params: captureParams)
            
            if case .success(let photo) = result {
                currentPhoto = photo
                
                // Start AI analysis in background
                Task {
                    await analyzePhoto(photo)
                }
                
                // Get recommended tags
                await loadRecommendedTags(for: photo)
                
                // Show success feedback
                showSuccessFeedback = true
                hapticFeedback.captureSuccessFeedback()
                
                // Auto-show tag dialog after brief delay
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                    self.showTagDialog = true
                }
                
            } else {
                throw NSError(
                    domain: "CameraViewModel",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Failed to save photo"]
                )
            }
            
        } catch {
            captureError = error.localizedDescription
            showErrorFeedback = true
            hapticFeedback.captureErrorFeedback()
        }
        
        isCapturing = false
    }
    
    private func capturePhotoData() async throws -> Data {
        return try await withCheckedThrowingContinuation { continuation in
            guard let photoOutput = photoOutput else {
                continuation.resume(throwing: NSError(
                    domain: "CameraViewModel",
                    code: -1,
                    userInfo: [NSLocalizedDescriptionKey: "Photo output not available"]
                ))
                return
            }
            
            let settings = AVCapturePhotoSettings()
            settings.flashMode = .auto
            
            let delegate = PhotoCaptureDelegate { result in
                continuation.resume(with: result)
            }
            
            photoOutput.capturePhoto(with: settings, delegate: delegate)
        }
    }
    
    private func savePhotoToDocuments(_ data: Data) async throws -> String {
        let documentsPath = fileManager.urls(for: .documentDirectory, in: .userDomainMask)[0]
        let fileName = "hazard_photo_\(Date().timeIntervalSince1970)_\(photoCounter).jpg"
        photoCounter += 1
        
        let filePath = documentsPath.appendingPathComponent(fileName)
        
        try data.write(to: filePath)
        return filePath.path
    }
    
    private func createPhotoMetadata() -> PhotoMetadata {
        return PhotoMetadata(
            cameraModel: deviceInput?.device.modelID,
            orientation: nil,
            flash: nil,
            exposureTime: nil,
            focalLength: nil,
            iso: nil,
            whiteBalance: nil
        )
    }
    
    private func analyzePhoto(_ photo: Photo) async {
        isAnalyzing = true
        analysisProgress = 0.0
        
        do {
            // Simulate progress updates
            let progressTimer = Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { _ in
                DispatchQueue.main.async {
                    if self.analysisProgress < 0.9 {
                        self.analysisProgress += 0.05
                    }
                }
            }
            
            let analysisResult = try await analyzePhotoUseCase.invoke(params: AnalyzePhotoUseCaseParams(photoId: photo.id))
            
            progressTimer.invalidate()
            analysisProgress = 1.0
            
            if case .success(let analysis) = analysisResult {
                // Analysis completed successfully
                hapticFeedback.analysisCompleteFeedback()
            }
            
        } catch {
            print("Analysis failed: \(error)")
        }
        
        isAnalyzing = false
        analysisProgress = 0.0
    }
    
    private func loadRecommendedTags(for photo: Photo) async {
        do {
            let result = try await getRecommendedTagsUseCase.invoke(params: GetRecommendedTagsUseCaseParams(photoId: photo.id))
            
            if case .success(let tags) = result {
                recommendedTags = tags
            }
        } catch {
            print("Failed to load recommended tags: \(error)")
        }
    }
    
    private func loadRecentPhotos() async {
        // TODO: Implement recent photos loading from repository
        // This would use a GetRecentPhotosUseCase from KMP
    }
}

// MARK: - CLLocationManagerDelegate

extension CameraViewModel: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        currentLocation = locations.last
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        locationPermissionStatus = status
        
        switch status {
        case .authorizedWhenInUse, .authorizedAlways:
            locationManager.startUpdatingLocation()
        case .denied, .restricted:
            currentLocation = nil
        case .notDetermined:
            break
        @unknown default:
            break
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("Location error: \(error.localizedDescription)")
    }
}

// MARK: - Photo Capture Delegate

private class PhotoCaptureDelegate: NSObject, AVCapturePhotoCaptureDelegate {
    private let completion: (Result<Data, Error>) -> Void
    
    init(completion: @escaping (Result<Data, Error>) -> Void) {
        self.completion = completion
        super.init()
    }
    
    func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        if let error = error {
            completion(.failure(error))
            return
        }
        
        guard let photoData = photo.fileDataRepresentation() else {
            completion(.failure(NSError(
                domain: "PhotoCaptureDelegate",
                code: -1,
                userInfo: [NSLocalizedDescriptionKey: "Failed to get photo data"]
            )))
            return
        }
        
        completion(.success(photoData))
    }
}

// MARK: - KMP Integration Data Classes

// Note: These would typically be generated by KMP or defined in shared module
// Shown here for illustration of the integration pattern

struct AnalyzePhotoUseCaseParams {
    let photoId: String
}

struct GetRecommendedTagsUseCaseParams {
    let photoId: String
}

struct ApplyTagsUseCaseParams {
    let photoId: String
    let complianceStatus: ComplianceStatus
    let tags: [Tag]
}
