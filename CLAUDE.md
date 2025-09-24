# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

HazardHawk is an AI-powered construction safety platform designed for Android with multi-platform support (phones, tablets, Android TV). The app enables construction workers to capture and analyze safety hazards using AI, generate safety documentation, and maintain OSHA compliance.

## Core Product Requirements

### Key Features Priority
1. **Photo Capture & AI Analysis** - Primary feature for hazard detection
2. **Pre-Shift Meeting Tool** - Daily safety briefings
3. **Incident Reporting** - Documentation and compliance
4. **Toolbox Talks Generator** - Weekly safety discussions
5. **Pre-Task Plans (PTPs)** - Job hazard analysis

### User Tiers
- **Field Access**: Photo upload, view analysis, read-only docs
- **Safety Lead**: Generate PTPs, Toolbox Talks, Incident Reports
- **Project Admin**: Full access including analytics, user management

## Technical Architecture

### Cross-Platform Development with Kotlin Multiplatform
- **Framework**: Kotlin Multiplatform (KMP)
  - Share business logic across all platforms
  - Native performance with platform-specific UI
  - Supports: Android, iOS, Desktop (JVM), Web (JS/WASM), Windows, macOS, Linux
- **Languages**: 
  - Shared Code: Kotlin
  - Android UI: Jetpack Compose
  - iOS UI: SwiftUI (with KMP shared logic)
  - Desktop UI: Compose Multiplatform
  - Web UI: Compose for Web or React (via Kotlin/JS)
- **Architecture**: Clean Architecture with shared domain/data layers
- **Dependency Injection**: Koin (works across all platforms)

### Platform-Specific Implementation
- **Shared Module (commonMain)**:
  - Business logic and use cases
  - Data repositories and models
  - Network client (Ktor)
  - Local database (SQLDelight)
  - AI analysis processing
  
- **Android (androidMain)**:
  - Jetpack Compose UI
  - CameraX for photo capture
  - Location Services API
  
- **iOS (iosMain)**:
  - SwiftUI views consuming KMP ViewModels
  - AVFoundation for camera
  - CoreLocation for GPS
  
- **Desktop (desktopMain)**:
  - Compose Multiplatform UI
  - Java Desktop API for file access
  - Webcam capture via native libraries
  
- **Web (jsMain/wasmMain)**:
  - Compose for Web or Kotlin/JS with React
  - WebRTC for camera access
  - Browser Geolocation API

### Backend Integration
- **HTTP Client**: Ktor (works across all platforms)
- **Serialization**: Kotlinx.serialization
- **Authentication**: AWS Cognito SDK integration per platform
- **File Storage**: AWS S3 via multiplatform client
- **AI Processing**: Google Gemini Vision Pro 2.5
- **Database**: PostgreSQL (via backend API)
- **Real-time Updates**: WebSockets via Ktor

## Development Commands

### Kotlin Multiplatform Commands
```bash
# Build all targets
./gradlew build

# Run tests for all platforms
./gradlew allTests

# Android specific
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
./gradlew :androidApp:test

# iOS specific (requires macOS)
./gradlew :shared:iosX64Test
./gradlew :shared:iosSimulatorArm64Test
cd iosApp && xcodebuild -project HazardHawk.xcodeproj -scheme HazardHawk -destination 'platform=iOS Simulator,name=iPhone 15'

# Desktop (JVM) specific
./gradlew :desktopApp:run
./gradlew :desktopApp:packageDistributionForCurrentOS
./gradlew :desktopApp:test

# Web specific
./gradlew :webApp:jsBrowserRun
./gradlew :webApp:jsBrowserProductionWebpack
./gradlew :webApp:wasmJsBrowserRun

# Shared module tests
./gradlew :shared:test
./gradlew :shared:testDebugUnitTest
./gradlew :shared:testReleaseUnitTest

# Code quality
./gradlew ktlintCheck
./gradlew ktlintFormat
./gradlew detekt

# Clean build
./gradlew clean

# Publish to local Maven
./gradlew publishToMavenLocal
```

## Project Structure

```
HazardHawk/
├── shared/                          # Kotlin Multiplatform shared module
│   ├── src/
│   │   ├── commonMain/             # Shared code for all platforms
│   │   │   └── kotlin/com/hazardhawk/
│   │   │       ├── domain/         # Use cases, entities, repositories
│   │   │       ├── data/           # Data sources, API clients
│   │   │       ├── models/         # Data models
│   │   │       └── utils/          # Common utilities
│   │   ├── androidMain/            # Android-specific implementations
│   │   ├── iosMain/                # iOS-specific implementations
│   │   ├── desktopMain/            # Desktop-specific implementations
│   │   ├── jsMain/                 # JavaScript/Web-specific
│   │   └── wasmJsMain/             # WebAssembly-specific
├── androidApp/                      # Android application
│   └── src/main/java/com/hazardhawk/
│       ├── ui/                     # Jetpack Compose UI
│       │   ├── camera/
│       │   ├── gallery/
│       │   ├── analysis/
│       │   └── reports/
│       └── MainActivity.kt
├── iosApp/                          # iOS application (Xcode project)
│   ├── HazardHawk/
│   │   ├── Views/                  # SwiftUI views
│   │   ├── ViewModels/             # iOS ViewModels wrapping KMP
│   │   └── ContentView.swift
├── desktopApp/                      # Desktop application
│   └── src/jvmMain/kotlin/
│       ├── ui/                     # Compose Multiplatform UI
│       └── Main.kt
├── webApp/                          # Web application
│   └── src/jsMain/kotlin/
│       ├── ui/                     # Compose for Web or React
│       └── App.kt
├── backend/                         # Go backend (optional, for reference)
│   ├── handlers/
│   ├── models/
│   └── main.go
└── gradle/                          # Gradle configuration
```

## Key Implementation Guidelines

### Camera Module Implementation
- **Android**: CameraX API with Compose integration
- **iOS**: AVFoundation with SwiftUI camera view
- **Desktop**: Webcam capture via platform-specific libraries
- **Web**: WebRTC getUserMedia API
- **Shared Logic**: Photo metadata handling, compression, queue management
- Implement metadata overlay (GPS, timestamp, direction) in shared code
- Support bulk photo selection from platform galleries
- Queue photos for S3 upload with retry logic in shared module

### AI Analysis Flow
1. Capture/select photos
2. Upload to S3 with progress tracking
3. Send S3 URLs to backend for AI processing
4. Receive and parse JSON analysis results
5. Display hazards with OSHA codes and severity
6. Enable PDF export with photos and analysis

### Offline Support (Shared Module)
- **SQLDelight** for cross-platform local database
- Cache safety documents in platform-appropriate storage
- Queue photo uploads when offline using Ktor client
- Automatic sync when connection restored
- Store user tier/permissions in encrypted local storage
- Platform-specific storage:
  - Android: Internal storage / Room
  - iOS: Core Data / Documents directory  
  - Desktop: User home directory
  - Web: IndexedDB

### UI/UX Requirements
- Construction-friendly design (high contrast, large touch targets)
- Simple navigation - everything accessible in 2 taps
- Viewfinder with visible borders and metadata overlay
- Bottom bar: gallery, capture, settings
- Top-left hamburger menu for navigation

### PDF Generation (Platform-Specific)
- **Android**: Android PDF APIs or iText
- **iOS**: PDFKit framework
- **Desktop**: Apache PDFBox or iText
- **Web**: PDF.js or server-side generation
- **Shared Logic**: Document structure, content formatting
- Include company branding, timestamps from shared module
- Support signature capture per platform:
  - Mobile: Touch/stylus input
  - Desktop: Mouse drawing
  - Web: Canvas API
- Export to platform-appropriate storage and share

## Testing Approach

### Shared Module Testing
- Unit tests for all business logic in commonTest
- Platform-specific tests in respective test source sets
- Mock implementations for platform APIs

### Platform Testing
- **Android**: Espresso/Compose Testing for UI
- **iOS**: XCTest and XCUITest
- **Desktop**: Compose Multiplatform test APIs
- **Web**: Karma/Jasmine or Jest
- Integration tests for API and database operations
- Screenshot/snapshot tests per platform

## Performance Considerations

### Shared Optimizations
- Coroutines for async operations across all platforms
- Image compression algorithms in shared module
- Pagination logic in repository layer
- Flow/StateFlow for reactive data streams

### Platform-Specific Optimizations
- **Android**: WorkManager for background uploads
- **iOS**: Background tasks and URLSession
- **Desktop**: Thread pools for bulk operations
- **Web**: Web Workers for heavy processing
- Memory management strategies per platform
- Lazy loading implementations using platform UI frameworks

## Context Management Rules

### Critical Context (Always Load)
```yaml
priority_1_always:
  - current_task_definition
  - active_file_paths
  - unresolved_errors
  - user_requirements
```

### Dynamic Context (Load When Needed)
```yaml
priority_2_conditional:
  file_operations: project_structure
  debugging: error_traces, logs
  refactoring: design_patterns
  testing: test_framework
```

### Context Limits
- **Working**: <8000 tokens
- **Prune at**: 70% capacity
- **Summarize at**: 50% capacity
- **Reset if**: hallucination detected

### Failure Prevention
1. **No Poisoning**: Remove incorrect info immediately
2. **No Distraction**: Limit to essential context
3. **No Confusion**: Filter with semantic relevance
4. **No Clash**: Single source of truth

### Multi-Agent Rules
- Each agent: Isolated context
- Share: Summaries only
- Max tools: 100 per agent
- Parallel ops: Separate threads

### Quick Checks
- [ ] Is every token earning its place?
- [ ] Can I remove 30% without losing functionality?
- [ ] Am I storing info that could be retrieved?
- [ ] Is my context fresh (<5 interactions old)?

### Emergency Reset
If context degraded: STOP → SNAPSHOT → RESET → RELOAD minimal → RESUME

### Token Budget
| Type | Allocation | Example |
|------|------------|---------|
| Task | 20% | Current operation |
| State | 40% | Files, variables |
| Docs | 30% | Patterns, examples |
| Buffer | 10% | Unexpected needs |

**Remember**: Context is not free. Quality > Quantity.