# iOS Accessibility and Field-Condition Optimization Parity

This document outlines how to implement the same accessibility and field-condition optimizations on iOS that we've created for Android in HazardHawk.

## Overview

The Android implementation provides:
- Sensor-based accessibility adaptations
- Glove mode detection and touch optimization
- Sunlight adaptive UI with automatic contrast
- Voice command system with "Hey HazardHawk" activation
- Weather adaptations (rain mode, cold weather, wind resistance)
- One-handed operation optimizations
- VoiceOver/TalkBack compatibility

## iOS Implementation Strategy

### 1. Sensor-Based Accessibility Manager

**Android Implementation:** `AccessibilitySensorManager.kt`

**iOS Equivalent:**
```swift
import CoreMotion
import UIKit

class AccessibilitySensorManager: ObservableObject {
    @Published var accessibilityState = AccessibilityState()
    
    private let motionManager = CMMotionManager()
    private var lightSensorTimer: Timer?
    
    struct AccessibilityState {
        var isInBrightLight: Bool = false
        var isGloveModeDetected: Bool = false
        var isRainModeActive: Bool = false
        var isColdWeatherDetected: Bool = false
        var lightLevel: Float = 0.0
        var lastTouchPressure: Float = 0.0
        var lastTouchSize: Float = 0.0
    }
    
    func startMonitoring() {
        // Light sensor monitoring (iOS doesn't have direct light sensor access)
        // Use screen brightness and camera exposure as proxy
        monitorScreenBrightness()
        
        // Motion data for environmental conditions
        startMotionMonitoring()
        
        // Touch pattern analysis
        setupTouchMonitoring()
    }
    
    private func monitorScreenBrightness() {
        lightSensorTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            let brightness = UIScreen.main.brightness
            // Proxy for ambient light - when user increases brightness, likely bright environment
            self.accessibilityState.isInBrightLight = brightness > 0.7
        }
    }
    
    private func startMotionMonitoring() {
        if motionManager.isDeviceMotionAvailable {
            motionManager.deviceMotionUpdateInterval = 0.1
            motionManager.startDeviceMotionUpdates(to: .main) { motion, error in
                guard let motion = motion else { return }
                
                // Analyze device motion for environmental conditions
                self.analyzeMotionForWeatherConditions(motion)
            }
        }
    }
    
    func analyzeTouchEvent(_ touch: UITouch, in view: UIView) {
        let pressure = touch.force // 3D Touch/Force Touch
        let size = touch.majorRadius
        
        // Similar logic to Android for glove detection
        let isGloveTouch = pressure < 0.3 && size > 15.0
        
        if isGloveTouch {
            accessibilityState.isGloveModeDetected = true
        }
        
        // Rain mode detection based on light touches
        let isRainTouch = pressure < 0.1 && size < 5.0
        if isRainTouch {
            accessibilityState.isRainModeActive = true
        }
    }
}
```

### 2. Sunlight Adaptive UI

**Android Implementation:** `SunlightAdaptiveUI.kt`

**iOS Equivalent:**
```swift
import SwiftUI

struct FieldOptimizedUIProvider<Content: View>: View {
    @ObservedObject var sensorManager: AccessibilitySensorManager
    let content: Content
    
    var adaptiveColorScheme: ColorScheme {
        sensorManager.accessibilityState.isInBrightLight ? .light : .dark
    }
    
    var body: some View {
        content
            .preferredColorScheme(adaptiveColorScheme)
            .environment(\.fieldUIState, FieldUIState(
                isHighContrast: sensorManager.accessibilityState.isInBrightLight,
                isGloveMode: sensorManager.accessibilityState.isGloveModeDetected
            ))
    }
}

// High contrast color scheme
extension Color {
    static let highContrastPrimary = Color.black
    static let highContrastBackground = Color.white
    static let highContrastSurface = Color.white
}

struct FieldOptimizedText: View {
    let text: String
    @Environment(\.fieldUIState) var fieldUIState
    
    var body: some View {
        Text(text)
            .font(fieldUIState.isGloveMode ? .title2 : .body)
            .fontWeight(fieldUIState.isHighContrast ? .bold : .medium)
            .foregroundColor(fieldUIState.isHighContrast ? .highContrastPrimary : .primary)
            .shadow(radius: fieldUIState.isHighContrast ? 2 : 0)
    }
}
```

### 3. Voice Command System

**Android Implementation:** `VoiceCommandSystem.kt`

**iOS Equivalent:**
```swift
import Speech
import AVFoundation

class VoiceCommandSystem: ObservableObject {
    @Published var isListening = false
    @Published var lastCommand: String?
    
    private let speechRecognizer = SFSpeechRecognizer(locale: Locale.current)
    private let audioEngine = AVAudioEngine()
    private let speechSynthesizer = AVSpeechSynthesizer()
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    
    func startVoiceActivation() {
        guard let speechRecognizer = speechRecognizer,
              speechRecognizer.isAvailable else {
            print("Speech recognition not available")
            return
        }
        
        startListening()
    }
    
    private func startListening() {
        // Configure audio session
        let audioSession = AVAudioSession.sharedInstance()
        try? audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        try? audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else { return }
        
        recognitionRequest.shouldReportPartialResults = true
        
        let inputNode = audioEngine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)
        
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            recognitionRequest.append(buffer)
        }
        
        audioEngine.prepare()
        try? audioEngine.start()
        
        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { result, error in
            if let result = result {
                let command = result.bestTranscription.formattedString.lowercased()
                
                if command.contains("hey hazardhawk") {
                    self.handleVoiceActivation()
                } else if command.contains("take photo") {
                    self.handleCommand(.capturePhoto)
                }
                
                self.lastCommand = command
            }
        }
        
        isListening = true
    }
    
    private func handleVoiceActivation() {
        speak("How can I help you?")
    }
    
    private func speak(_ text: String) {
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "en-US")
        speechSynthesizer.speak(utterance)
    }
}
```

### 4. Weather Adaptation System

**Android Implementation:** `WeatherAdaptationSystem.kt`

**iOS Equivalent:**
```swift
import UIKit

class WeatherAdaptationSystem: ObservableObject {
    @Published var weatherConditions = WeatherConditions()
    @Published var isWaitingForConfirmation = false
    
    private let hapticFeedback = UIImpactFeedbackGenerator()
    
    struct WeatherConditions {
        var isRaining: Bool = false
        var isCold: Bool = false
        var isWindy: Bool = false
    }
    
    func handleGesture(_ gesture: UIGestureRecognizer) {
        // Apply weather-based gesture modifications
        if weatherConditions.isRaining {
            // Require confirmation for important gestures
            requestConfirmation(for: gesture)
        } else if weatherConditions.isCold {
            // Add delay for cold weather
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                self.executeGesture(gesture)
            }
        } else {
            executeGesture(gesture)
        }
    }
    
    private func requestConfirmation(for gesture: UIGestureRecognizer) {
        isWaitingForConfirmation = true
        hapticFeedback.impactOccurred()
        
        // Auto-confirm after timeout
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            if self.isWaitingForConfirmation {
                self.confirmGesture(gesture)
            }
        }
    }
    
    private func confirmGesture(_ gesture: UIGestureRecognizer) {
        isWaitingForConfirmation = false
        executeGesture(gesture)
        hapticFeedback.impactOccurred(intensity: 0.5)
    }
    
    private func executeGesture(_ gesture: UIGestureRecognizer) {
        // Execute the actual gesture
        hapticFeedback.impactOccurred(intensity: 0.3)
    }
}
```

### 5. One-Handed Operation System

**Android Implementation:** `OneHandedOperationSystem.kt`

**iOS Equivalent:**
```swift
import SwiftUI

class OneHandedOperationSystem: ObservableObject {
    @Published var configuration = HandedConfiguration()
    
    struct HandedConfiguration {
        var isLeftHanded: Bool = false
        var thumbReachRadius: CGFloat = 120
        var oneHandedModeEnabled: Bool = false
    }
    
    func calculateThumbZone(for screenSize: CGSize) -> ThumbZone {
        let thumbBaseX: CGFloat = configuration.isLeftHanded ? 30 : screenSize.width - 30
        let thumbBaseY: CGFloat = screenSize.height - 120
        
        return ThumbZone(
            centerX: thumbBaseX,
            centerY: thumbBaseY,
            easyReachRadius: configuration.thumbReachRadius
        )
    }
    
    func getFABPosition(for screenSize: CGSize) -> CGPoint {
        let thumbZone = calculateThumbZone(for: screenSize)
        let angle = configuration.isLeftHanded ? 45.0 : 135.0
        let radius = thumbZone.easyReachRadius - 30
        
        let x = thumbZone.centerX + radius * cos(angle * .pi / 180)
        let y = thumbZone.centerY + radius * sin(angle * .pi / 180)
        
        return CGPoint(x: x, y: y)
    }
}

struct ThumbFriendlyLayout<Content: View>: View {
    @ObservedObject var oneHandedSystem: OneHandedOperationSystem
    let content: Content
    
    var body: some View {
        GeometryReader { geometry in
            ZStack {
                content
                
                if oneHandedSystem.configuration.oneHandedModeEnabled {
                    ThumbZoneOverlay(
                        thumbZone: oneHandedSystem.calculateThumbZone(for: geometry.size)
                    )
                }
            }
        }
    }
}
```

### 6. VoiceOver Compatibility

**Android Implementation:** `TalkBackCompatibility.kt`

**iOS Equivalent:**
```swift
import SwiftUI

struct AccessibleButton: View {
    let title: String
    let action: () -> Void
    let isEnabled: Bool
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.body)
                .padding()
                .background(Color.blue)
                .foregroundColor(.white)
                .cornerRadius(8)
        }
        .disabled(!isEnabled)
        .accessibilityLabel(title)
        .accessibilityHint(isEnabled ? "Double tap to activate" : "Disabled")
        .accessibilityAddTraits(isEnabled ? [] : .notEnabled)
    }
}

struct AccessibleCard<Content: View>: View {
    let title: String
    let subtitle: String?
    let content: Content
    let action: (() -> Void)?
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            
            if let subtitle = subtitle {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            
            content
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(buildAccessibilityLabel())
        .onTapGesture {
            action?()
        }
    }
    
    private func buildAccessibilityLabel() -> String {
        var label = title
        if let subtitle = subtitle {
            label += ", " + subtitle
        }
        if action != nil {
            label += ", button"
        }
        return label
    }
}
```

## Key iOS Framework Differences

### Sensors
- **Android**: Direct access to light, proximity, and temperature sensors
- **iOS**: Limited sensor access, use screen brightness and camera as proxies

### Speech Recognition
- **Android**: SpeechRecognizer with detailed configuration
- **iOS**: Speech framework with SFSpeechRecognizer

### Accessibility
- **Android**: TalkBack with detailed semantic properties
- **iOS**: VoiceOver with accessibility modifiers and traits

### Haptic Feedback
- **Android**: Vibrator service with custom patterns
- **iOS**: UIFeedbackGenerator with predefined intensities

### Touch Analysis
- **Android**: MotionEvent with pressure and size data
- **iOS**: UITouch with force (3D Touch) and majorRadius

## Implementation Priority for iOS

1. **Voice Command System** - Most impactful for construction workers
2. **Sunlight Adaptive UI** - Critical for outdoor visibility
3. **One-Handed Operation** - Important for field use
4. **VoiceOver Compatibility** - Accessibility compliance
5. **Weather Adaptations** - Nice-to-have for harsh conditions

## Additional iOS Considerations

### App Store Guidelines
- Accessibility features may help with App Store approval
- Voice recording requires privacy usage descriptions
- Background audio usage needs proper justification

### iOS-Specific Enhancements
- **Siri Shortcuts** integration for voice commands
- **Control Center** widget for quick access
- **Dynamic Type** support for better text scaling
- **Haptic Patterns** using Core Haptics for rich feedback
- **Dark Mode** automatic switching based on time/location

### Testing Strategy
- Use **Accessibility Inspector** (equivalent to Android's Accessibility Scanner)
- Test with **VoiceOver** enabled throughout development
- Use **Simulator** conditions to test different scenarios
- Test on various **device sizes** for thumb zone accuracy

This implementation provides feature parity with the Android accessibility system while leveraging iOS-specific capabilities and following platform conventions.