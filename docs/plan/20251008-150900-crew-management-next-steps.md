# Crew Management System - Next Steps Implementation Plan

**Date**: October 8, 2025 (15:09:00)
**Status**: Phase 1 & 5 Complete ✅ | Phases 2-4 Planning Complete
**Previous Docs**:
- [Implementation Plan](/docs/implementation/crew-management-implementation-plan.md)
- [Implementation Log](/docs/implementation/20251008-142500-crew-management-implementation-log.md)

---

## Executive Summary

This document consolidates findings from parallel planning agents (simple-architect, refactor-master, test-guardian, loveable-ux) to create a unified roadmap for completing the crew management system. The plan prioritizes **Phase 2 (Certification Management)** as the next implementation target, followed by Phases 4 and 3.

**Current Progress**: 40% Complete (Phases 1 & 5 ✅)
**Remaining Work**: Phases 2, 3, 4, 6 (60%)
**Estimated Timeline**: 5.5 weeks to production-ready

---

## Table of Contents

1. [Current State Review](#current-state-review)
2. [Critical Technical Debt](#critical-technical-debt)
3. [Priority 1: Phase 2 - Certification Management](#phase-2-certification-management)
4. [Priority 2: Phase 4 - Crew Assignment UI](#phase-4-crew-assignment-ui)
5. [Priority 3: Phase 3 - Worker Onboarding](#phase-3-worker-onboarding)
6. [API Client Architecture](#api-client-architecture)
7. [Testing Strategy](#testing-strategy)
8. [Implementation Roadmap](#implementation-roadmap)
9. [Success Metrics](#success-metrics)

---

## Current State Review

### ✅ Completed (40%)

**Phase 1: Foundation**
- Complete database schema (12 tables, 40+ RLS policies)
- 5 repository interfaces + implementations (in-memory)
- 15 Kotlin data models with validation
- 5 reusable UI components (Material 3)
- 8 admin screens (company, project, worker management)

**Phase 5: PTP Integration**
- `PTPCrewIntegrationService` auto-populates crew data
- Centralized company/project info eliminates duplicate entry
- 4-page PDF with crew roster sign-in sheet
- Flexible foreman selection per PTP

**Total Code**: ~10,500 lines production code + 1,858 lines SQL

### ⏳ Remaining (60%)

**Phase 2: Certification Management** (2 weeks)
- OCR document extraction
- Admin verification workflow
- Expiration tracking + automated alerts
- S3 file storage integration

**Phase 3: Worker Onboarding** (2 weeks)
- Magic link passwordless authentication
- Multi-step mobile-optimized wizard
- E-signature capture
- Admin approval workflow

**Phase 4: Crew Assignment** (1.5 weeks)
- Drag-and-drop crew builder (desktop/tablet)
- Real-time WebSocket updates
- PDF roster generation
- Mobile-optimized assignment UI

**Phase 6: Polish & Optimization** (1 week)
- Performance optimization
- Comprehensive testing
- Accessibility audit
- Production deployment

---

## Critical Technical Debt

### PRIORITY 1 - Must Fix Before Production

#### 1. Duplicate Model Definitions
**Location**: `/shared/src/commonMain/kotlin/com/hazardhawk/models/crew/`

**Issue**: `CrewModels.kt` (337 lines) contains ALL models, while individual files duplicate definitions.

**Impact**:
- Compilation ambiguity risk
- Two incompatible pagination models exist (cursor-based vs page-based)
- Future maintenance nightmare

**Fix** (1 day):
```kotlin
// Step 1: Create single pagination model
// /shared/src/commonMain/kotlin/com/hazardhawk/models/common/Pagination.kt
@Serializable
data class PaginationRequest(
    val cursor: String? = null,
    val pageSize: Int = 20,
    val sortBy: String? = null,
    val sortDirection: SortDirection = SortDirection.ASC
)

// Step 2: Delete CrewModels.kt entirely
// Step 3: Keep individual model files (Crew.kt, Company.kt, etc.)
// Step 4: Update all imports
```

#### 2. In-Memory Repository Implementations
**Location**: `/shared/src/commonMain/kotlin/com/hazardhawk/data/repositories/crew/`

**Issue**: All repositories use `mutableMapOf<String, T>()` with mock data. No API integration.

**Impact**: Cannot deploy to production without backend connectivity.

**Fix** (2 weeks - see [API Client Architecture](#api-client-architecture)):
- Implement API client layer with Ktor
- Migrate repositories one-by-one
- Add offline queue for network failures
- Maintain feature flags for rollback

#### 3. S3 Upload Integration Missing
**Locations**:
- `WorkerRepositoryImpl.kt:291` - `uploadWorkerPhoto()` returns `NotImplementedError`
- `CompanyRepositoryImpl.kt:69` - `uploadCompanyLogo()` returns `NotImplementedError`

**Impact**: Core feature (photos, certifications) completely non-functional.

**Fix** (3 days):
```kotlin
// /shared/src/commonMain/kotlin/com/hazardhawk/data/storage/S3Client.kt
interface S3Client {
    suspend fun uploadFile(
        bucket: String,
        key: String,
        data: ByteArray,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<String> // Returns CDN URL
}
```

---

## Phase 2: Certification Management

**Priority**: 🔥 HIGHEST
**Timeline**: 2 weeks
**Why First**: Self-contained, high business value (compliance), reusable services

### Architecture Components

#### New Services (3 files)

**1. FileUploadService** (`/shared/src/commonMain/kotlin/com/hazardhawk/domain/services/FileUploadService.kt`)
```kotlin
interface FileUploadService {
    suspend fun uploadFile(
        file: ByteArray,
        fileName: String,
        contentType: String,
        onProgress: (Float) -> Unit = {}
    ): Result<UploadResult>

    suspend fun compressImage(
        imageData: ByteArray,
        maxSizeKB: Int = 500
    ): Result<ByteArray>
}

data class UploadResult(
    val url: String,
    val thumbnailUrl: String? = null,
    val sizeBytes: Long
)
```

**2. OCRService** (`/shared/src/commonMain/kotlin/com/hazardhawk/domain/services/OCRService.kt`)
```kotlin
interface OCRService {
    suspend fun extractCertificationData(
        documentUrl: String
    ): Result<ExtractedCertification>
}

data class ExtractedCertification(
    val holderName: String,
    val certificationType: String, // Mapped to standard codes
    val certificationNumber: String?,
    val issueDate: LocalDate?,
    val expirationDate: LocalDate?,
    val issuingAuthority: String?,
    val confidence: Float, // 0.0 to 1.0
    val needsReview: Boolean // True if confidence < 85%
)
```

**3. NotificationService** (`/shared/src/commonMain/kotlin/com/hazardhawk/domain/services/NotificationService.kt`)
```kotlin
interface NotificationService {
    suspend fun sendCertificationExpirationAlert(
        workerId: String,
        certification: WorkerCertification,
        daysUntilExpiration: Int
    ): Result<Unit>

    suspend fun sendEmail(
        to: String,
        subject: String,
        body: String
    ): Result<Unit>

    suspend fun sendSMS(
        to: String,
        message: String
    ): Result<Unit>

    suspend fun sendPushNotification(
        userId: String,
        title: String,
        body: String
    ): Result<Unit>
}
```

#### New Screens (2 files)

**1. CertificationUploadScreen** (`/HazardHawk/androidApp/src/main/java/com/hazardhawk/ui/crew/CertificationUploadScreen.kt`)

**Flow**:
```
Camera Capture → Compress → Upload → OCR Processing → Pre-fill Form → User Confirms → Submit
       ↓
   [Loading indicator with progress]
       ↓
   [OCR Results Preview]
   - Holder Name: John Doe ✓
   - Type: OSHA 10 ✓
   - Cert #: 12345 ⚠️ (Low confidence - review)
   - Expiration: 2026-05-15 ✓
       ↓
   [Edit if needed] → [Submit for Verification]
```

**UI Features**:
- Camera with document frame overlay
- Real-time upload progress (0-100%)
- OCR confidence badges (✓ High, ⚠️ Medium, ❌ Low)
- Smart field suggestions (certification types)
- Haptic feedback on completion
- Error recovery (retry upload, skip OCR)

**2. CertificationVerificationScreen** (`/HazardHawk/androidApp/src/main/java/com/hazardhawk/ui/crew/CertificationVerificationScreen.kt`)

**Admin Workflow**:
```
Pending List → Select Cert → View Document + Extracted Data → Approve/Reject → Notify Worker
```

**UI Features**:
- Swipe cards (left = reject, right = approve)
- Document zoom viewer
- Side-by-side comparison (OCR vs image)
- Bulk actions (approve all, reject with reason)
- Queue statistics (23 pending, 2 flagged)

#### Background Job

**ExpirationCheckJob** (`/backend/jobs/ExpirationCheckJob.go` or Kotlin)

**Schedule**: Daily at 2:00 AM
**Logic**:
```
For each threshold (90, 60, 30, 14, 7, 3, 0 days):
    Find certs expiring on target date
    Send notifications via appropriate channels
    Mark alert as sent to avoid duplicates

On expiration day:
    Mark cert as EXPIRED
    Notify worker, safety manager, project manager
    Remove worker from tasks requiring cert
```

### Implementation Steps (Week-by-Week)

**Week 1: Services & Backend**
- Day 1-2: Implement `FileUploadService` with S3 integration
- Day 3: Add image compression utilities
- Day 4-5: Implement `OCRService` (backend integration with Google Document AI)

**Week 2: UI & Automation**
- Day 1-2: Build `CertificationUploadScreen` with camera + OCR workflow
- Day 3: Implement `NotificationService` (SendGrid + Twilio)
- Day 4: Build `CertificationVerificationScreen` (admin UI)
- Day 5: Create background expiration check job

### Testing Coverage (90 tests planned)

**Unit Tests** (60 tests):
- OCR extraction accuracy (30+ scenarios)
- Upload retry logic (5 scenarios)
- Notification delivery (10 scenarios)
- Expiration calculation (15 edge cases)

**Integration Tests** (20 tests):
- End-to-end upload → OCR → approval flow
- Expiration alert delivery at each threshold
- Multi-file upload concurrency
- Error recovery scenarios

**UI Tests** (10 tests):
- Camera capture and preview
- Form validation with OCR pre-fill
- Admin approval workflow
- Error state displays

### Success Criteria

- ✅ Upload success rate >95%
- ✅ OCR accuracy >85% (auto-extraction)
- ✅ Notification delivery >99%
- ✅ Upload time <10s for 5MB file
- ✅ OCR processing <15s
- ✅ Workers can upload cert in <3 minutes

---

## Phase 4: Crew Assignment UI

**Priority**: 🔥 HIGH (after Phase 2)
**Timeline**: 1.5 weeks
**Why Second**: Visible impact, leverages existing repositories

### Architecture Components

#### New Service (1 file)

**WebSocketService** (`/shared/src/commonMain/kotlin/com/hazardhawk/domain/services/WebSocketService.kt`)
```kotlin
interface WebSocketService {
    suspend fun connect(): Result<Unit>
    fun disconnect()
    fun subscribeToCrewUpdates(companyId: String): Flow<CrewUpdateEvent>
    suspend fun broadcastCrewUpdate(event: CrewUpdateEvent): Result<Unit>
}

sealed class CrewUpdateEvent {
    data class MemberAdded(val crewId: String, val workerId: String) : CrewUpdateEvent()
    data class MemberRemoved(val crewId: String, val workerId: String) : CrewUpdateEvent()
    data class CrewCreated(val crew: Crew) : CrewUpdateEvent()
    data class CrewUpdated(val crew: Crew) : CrewUpdateEvent()
}
```

#### New Screens (2 files)

**1. CrewBuilderScreen** (`/HazardHawk/androidApp/src/main/java/com/hazardhawk/ui/crew/CrewBuilderScreen.kt`)

**Desktop/Tablet Layout** (Drag-and-Drop):
```
┌────────────────────────────────────────────────────────────┐
│  AVAILABLE WORKERS (32)        │  CREWS                    │
│  [Search: _______]             │                           │
│                                │  ┌──────────────────────┐ │
│  ☐ Show All                    │  │ Concrete Crew #1     │ │
│  ☑ Show Unassigned             │  │ 8 members • Floor 3  │ │
│                                │  │ [+ Add] [⋮]          │ │
│  [📋 John Doe]                 │  └──────────────────────┘ │
│   E-1234 | Laborer             │                           │
│   ✅ OSHA 10 ✅ CPR            │  [Drop workers here]      │
│                                │                           │
│  [📋 Jane Smith] ← draggable   │  ┌──────────────────────┐ │
│   E-1235 | Foreman             │  │ Framing Crew A       │ │
│   ✅ OSHA 30 ⚠️ CPR            │  │ 6 members • Floor 5  │ │
│    (expires 11/1)              │  │ [+ Add] [⋮]          │ │
│                                │  └──────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

**Interactions**:
- Drag worker from left → Drop on crew → Confirmation dialog
- Real-time updates (other users' changes appear instantly)
- Optimistic updates (UI responds immediately, syncs in background)
- Conflict resolution (last write wins with notification)

**2. MobileCrewAssignmentScreen** (`/HazardHawk/androidApp/src/main/java/com/hazardhawk/ui/crew/MobileCrewAssignmentScreen.kt`)

**Mobile Layout** (List + Tap):
```
┌───────────────────────────────┐
│ Crews  [+ Create]  [Filter ▼]│
├───────────────────────────────┤
│ Concrete Crew #1              │
│ 8 members • Floor 3           │
│ [+ Add Member]                │
├───────────────────────────────┤
│ Framing Crew A                │
│ 6 members • Floor 5           │
│ [+ Add Member]                │
└───────────────────────────────┘
     ↓ Tap [+ Add Member]
┌───────────────────────────────┐
│ Select Workers                │
│ [Search: _______]             │
├───────────────────────────────┤
│ ☐ John Doe (E-1234)           │
│ ☐ Jane Smith (E-1235)         │
│ ☑ Mike Johnson (E-1236)       │
│                               │
│ [Cancel]  [Assign (1)]        │
└───────────────────────────────┘
```

### Real-Time Sync Architecture

```
User A drags worker → Optimistic UI update → API call → WebSocket broadcast
                                                                ↓
User B's screen ← WebSocket event ← Redis PubSub ← Backend receives
```

**Conflict Resolution**:
- Last Write Wins (LWW) with timestamp
- Show toast notification: "Crew updated by [User Name]"
- Auto-refresh affected crew cards
- No data loss (audit trail in `crew_member_history` table)

### Implementation Steps

**Week 1: Core Drag-and-Drop**
- Day 1-2: Implement drag-and-drop with Compose (desktop/tablet)
- Day 3: Build mobile list-based assignment
- Day 4: Add crew filtering and search
- Day 5: Implement assignment confirmation dialogs

**Week 2 (First Half): Real-Time Sync**
- Day 1-2: Implement `WebSocketService` with Ktor
- Day 3: Add optimistic updates + conflict resolution

### Testing Coverage (45 tests planned)

**Unit Tests** (25 tests):
- Drag-and-drop state management
- Assignment validation rules
- Conflict resolution logic
- WebSocket connection handling

**Integration Tests** (15 tests):
- Multi-user concurrent assignment
- WebSocket event delivery
- Optimistic update + sync flow
- Network failure recovery

**UI Tests** (5 tests):
- Drag-and-drop latency <50ms
- Real-time update appearance <500ms
- Search and filter responsiveness

### Success Criteria

- ✅ Drag-and-drop latency <50ms
- ✅ WebSocket update delivery <500ms
- ✅ Concurrent user assignment works correctly
- ✅ Workers can assign 10 members in <2 minutes

---

## Phase 3: Worker Onboarding

**Priority**: ⚠️ MEDIUM (after Phases 2 & 4)
**Timeline**: 2 weeks
**Why Third**: Complex security requirements, depends on Phase 2 services

### Architecture Components

#### New Services (2 files)

**1. MagicLinkService** (`/shared/src/commonMain/kotlin/com/hazardhawk/domain/services/MagicLinkService.kt`)
```kotlin
interface MagicLinkService {
    suspend fun generateMagicLink(
        email: String,
        companyId: String,
        role: WorkerRole
    ): Result<MagicLinkInvitation>

    suspend fun verifyToken(token: String): Result<OnboardingSession>

    suspend fun invalidateToken(token: String): Result<Unit>
}

data class MagicLinkInvitation(
    val invitationId: String,
    val magicLink: String,
    val expiresAt: Instant // 24 hours from creation
)

data class OnboardingSession(
    val sessionId: String,
    val companyName: String,
    val email: String,
    val currentStep: OnboardingStep
)

enum class OnboardingStep {
    BASIC_INFO, PHOTO_ID, SELFIE, CERTIFICATIONS, SIGNATURE, COMPLETE
}
```

**Security Requirements**:
- 256-bit entropy tokens (cryptographically secure)
- 24-hour expiration
- Single-use (invalidated after verification)
- Rate limiting: 5 invitations per hour per admin
- Email domain validation (no disposable emails)

**2. OnboardingRepository** (`/shared/src/commonMain/kotlin/com/hazardhawk/domain/repositories/OnboardingRepository.kt`)
```kotlin
interface OnboardingRepository {
    suspend fun createSession(
        companyId: String,
        email: String,
        phone: String?
    ): Result<OnboardingSession>

    suspend fun updateSessionStep(
        sessionId: String,
        step: OnboardingStep,
        data: Map<String, Any>
    ): Result<OnboardingSession>

    suspend fun submitForApproval(
        sessionId: String
    ): Result<Unit>

    suspend fun approveSession(
        sessionId: String,
        adminId: String
    ): Result<CompanyWorker>

    suspend fun rejectSession(
        sessionId: String,
        adminId: String,
        reason: String
    ): Result<Unit>
}
```

#### New Screens (2 files)

**1. OnboardingLandingScreen** (`/HazardHawk/androidApp/src/main/java/com/hazardhawk/ui/onboarding/OnboardingLandingScreen.kt`)

**Welcome Flow**:
```
┌───────────────────────────────┐
│  Welcome to ABC Construction! │
│                               │
│  You've been invited as:      │
│  Construction Laborer         │
│                               │
│  This will take about 5 min.  │
│                               │
│  [Start Onboarding]           │
└───────────────────────────────┘
```

**2. OnboardingFlowScreen** (`/HazardHawk/androidApp/src/main/java/com/hazardhawk/ui/onboarding/OnboardingFlowScreen.kt`)

**5-Step Wizard**:

**Step 1: Basic Info**
- Full Name, Date of Birth, Email (pre-filled), Phone
- Emergency Contact (name + phone)
- Progress: ●○○○○ (1/5)

**Step 2: Photo ID Upload**
- Camera with document frame overlay
- OCR extraction (name, DOB, license number)
- User confirms extracted data
- Progress: ●●○○○ (2/5)

**Step 3: Selfie Capture**
- Live photo (prevent old uploads)
- Basic liveness detection (blink prompt)
- Progress: ●●●○○ (3/5)

**Step 4: Certifications** (Optional)
- "Do you have OSHA 10 or 30?" → Yes/No
- Multi-file upload if yes
- Skip if no certs
- Progress: ●●●●○ (4/5)

**Step 5: E-Signature**
- Safety policy acknowledgment
- Photo release agreement
- Signature canvas (finger/stylus)
- Progress: ●●●●● (5/5)

**Completion Screen**:
```
┌───────────────────────────────┐
│  ✅ Profile Submitted!        │
│                               │
│  Your profile is under review.│
│  You'll be notified in 24h.   │
│                               │
│  [Close]                      │
└───────────────────────────────┘
```

### Implementation Steps

**Week 1: Magic Link + Multi-Step Form**
- Day 1-2: Implement `MagicLinkService` with secure token generation
- Day 3: Build `OnboardingLandingScreen`
- Day 4-5: Build `OnboardingFlowScreen` with steps 1-3

**Week 2: Advanced Features + Admin Review**
- Day 1: Add e-signature canvas (Step 5)
- Day 2: Implement session state persistence (offline support)
- Day 3-4: Build admin approval screen
- Day 5: Integration testing + security audit

### Testing Coverage (80 tests planned)

**Security Tests** (30 tests):
- Token entropy validation (256-bit minimum)
- Token expiration enforcement
- Single-use token validation
- Rate limiting (5/hour)
- Email domain blacklist (disposable emails)
- XSS/CSRF protection

**Unit Tests** (30 tests):
- Multi-step form navigation
- Session state management
- Signature validation
- OCR extraction for ID documents

**Integration Tests** (15 tests):
- End-to-end onboarding flow
- Admin approval workflow
- Email delivery + link verification
- Error recovery (expired token, network failure)

**UI Tests** (5 tests):
- Form field validation
- Camera capture workflow
- Signature canvas interaction

### Success Criteria

- ✅ >90% workers complete onboarding without assistance
- ✅ <5 minute average completion time
- ✅ <24 hour average admin approval time
- ✅ Zero security vulnerabilities (penetration test)
- ✅ Zero disposable email signups

---

## API Client Architecture

### Repository-ApiClient-Network Layers

```
UI (ViewModels) → Repository Interface → Repository Impl → API Client → Network Layer
                       ↓ (cache)
                  LocalCache (SQLDelight)
                       ↓ (offline)
                  OfflineQueue
```

### Migration Strategy (6 Weeks)

#### Week 1: Foundation
**Goal**: Set up API client infrastructure

**Deliverables**:
- `ApiClient.kt` interface
- `HttpApiClient.kt` implementation (Ktor)
- `AuthProvider.kt` (AWS Cognito integration)
- Error mapping utilities (`DomainError.kt`)

**Files to Create**:
```
/shared/src/commonMain/kotlin/com/hazardhawk/data/
├── network/
│   ├── ApiClient.kt
│   ├── HttpApiClient.kt
│   └── AuthProvider.kt
└── errors/
    └── DomainError.kt
```

#### Week 2-3: API Clients + S3
**Goal**: Create API wrappers for each domain

**Deliverables**:
- 5 API clients (Worker, Crew, Company, Project, Certification)
- S3 client for file uploads
- Image compression utilities

**Files to Create**:
```
/shared/src/commonMain/kotlin/com/hazardhawk/data/
├── api/
│   ├── WorkerApiClient.kt
│   ├── CrewApiClient.kt
│   ├── CompanyApiClient.kt
│   ├── ProjectApiClient.kt
│   └── CertificationApiClient.kt
└── storage/
    ├── S3Client.kt
    └── ImageCompression.kt
```

#### Week 3-5: Repository Migration
**Goal**: Replace in-memory repositories with API-backed versions

**Approach**:
1. Create `WorkerRepositoryImpl_New.kt` alongside existing
2. Inject API client + S3 client + local cache
3. Implement all methods with API calls
4. Add feature flag: `FeatureFlags.useApiBackedRepositories = true`
5. Test in parallel with old implementation
6. Switch DI to use new implementation
7. Delete old implementation
8. Repeat for remaining repositories

**Risk Mitigation**:
- Feature flags for instant rollback
- Keep old implementations during transition
- Parallel testing (compare results)
- Gradual rollout (10% → 50% → 100%)

#### Week 5-6: Offline Support
**Goal**: Queue operations when offline, sync when online

**Deliverables**:
- `OfflineQueue.kt` (SQLDelight-backed)
- `SyncService.kt` (background sync)
- Conflict resolution logic

---

## Testing Strategy

### Test Distribution (~325 tests total)

| Phase | Unit | Integration | UI | Performance | Security |
|-------|------|-------------|----|-----------|---------|
| Phase 2 | 60 | 20 | 10 | 5 | 5 |
| Phase 3 | 30 | 15 | 5 | 5 | 30 |
| Phase 4 | 25 | 15 | 5 | 10 | 0 |
| API Migration | 50 | 20 | 0 | 10 | 5 |
| **TOTAL** | **165** | **70** | **20** | **30** | **40** |

### Test Pyramid

```
       E2E (5%)
     Integration (25%)
   Unit Tests (70%)
```

### Coverage Targets

- **Unit Tests**: 90%+ line coverage
- **Integration Tests**: 80%+ of critical paths
- **Critical Features**: 100% coverage (magic links, file uploads, WebSocket sync)

### CI/CD Pipeline

**GitHub Actions Workflow** (`/.github/workflows/crew-management-tests.yml`):
```yaml
name: Crew Management Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Unit Tests
        run: ./gradlew :shared:testDebugUnitTest

  integration-tests:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: hazardhawk_test
    steps:
      - name: Run Integration Tests
        run: ./gradlew :shared:testIntegration

  android-ui-tests:
    runs-on: macos-latest
    steps:
      - name: Run UI Tests on Emulator
        run: ./gradlew :androidApp:connectedAndroidTest

  performance-tests:
    runs-on: ubuntu-latest
    steps:
      - name: Run Load Tests
        run: k6 run load-tests/crew-management.js
```

### Performance Benchmarks

**API Response Times** (p95):
| Endpoint | Target | Test Method |
|----------|--------|-------------|
| List Workers (100) | <100ms | k6 load test |
| Worker Detail | <50ms | AndroidX Benchmark |
| Create Worker | <150ms | Integration test |
| Upload Photo (5MB) | <10s | S3 upload test |
| OCR Processing | <15s | Mock Google Document AI |
| WebSocket Latency | <500ms | Real-time sync test |

---

## Implementation Roadmap

### Timeline Overview (5.5 Weeks Total)

```
Week 1:     Phase 2 Start (Services + Backend)
Week 2:     Phase 2 Complete (UI + Automation)
Week 3:     API Migration Start (Foundation + Clients)
Week 4:     Phase 4 Start (Crew Builder + WebSocket)
Week 5:     Phase 4 Complete + Phase 3 Start
Week 6:     Phase 3 Complete
Week 6-7:   Polish, Testing, Production Deployment
```

### Detailed Schedule

#### Week 1: Phase 2 - Services & Backend
- **Mon**: Implement `FileUploadService` (S3 integration)
- **Tue**: Add image compression utilities + upload progress
- **Wed**: Implement `OCRService` (backend Google Document AI integration)
- **Thu**: Build `NotificationService` (SendGrid email)
- **Fri**: Add SMS notifications (Twilio) + testing

**Deliverables**: 3 services fully functional, unit tested

---

#### Week 2: Phase 2 - UI & Automation
- **Mon**: Build `CertificationUploadScreen` (camera + OCR workflow)
- **Tue**: Add OCR result preview + form pre-fill
- **Wed**: Build `CertificationVerificationScreen` (admin UI)
- **Thu**: Implement background expiration check job
- **Fri**: Integration testing + bug fixes

**Deliverables**: Phase 2 COMPLETE ✅, ready for beta testing

---

#### Week 3: API Migration - Foundation
- **Mon**: Create `ApiClient` interface + `HttpApiClient` with Ktor
- **Tue**: Implement authentication provider (AWS Cognito)
- **Wed**: Create 5 API client wrappers (Worker, Crew, Company, Project, Cert)
- **Thu**: Implement `S3Client` for file uploads
- **Fri**: Unit tests for network layer

**Deliverables**: API infrastructure ready, no repository changes yet

---

#### Week 4: Phase 4 - Crew Builder
- **Mon**: Implement drag-and-drop UI (Compose desktop/tablet)
- **Tue**: Build mobile list-based assignment
- **Wed**: Implement `WebSocketService` (Ktor WebSocket)
- **Thu**: Add real-time sync + optimistic updates
- **Fri**: Testing + conflict resolution scenarios

**Deliverables**: Drag-and-drop working, real-time sync functional

---

#### Week 5: Phase 4 Complete + Phase 3 Start
- **Mon**: Phase 4 polish + integration tests
- **Tue**: Phase 4 COMPLETE ✅
- **Wed**: Implement `MagicLinkService` (secure token generation)
- **Thu**: Build `OnboardingLandingScreen`
- **Fri**: Start multi-step form (Steps 1-2)

**Deliverables**: Phase 4 done, Phase 3 50% complete

---

#### Week 6: Phase 3 Complete
- **Mon**: Finish multi-step form (Steps 3-5)
- **Tue**: Add e-signature canvas
- **Wed**: Build admin approval screen
- **Thu**: Security testing + penetration test
- **Fri**: Phase 3 COMPLETE ✅

**Deliverables**: All phases 2-4 COMPLETE, ready for production

---

#### Week 6-7: Polish & Production
- **Mon**: API repository migration (WorkerRepository)
- **Tue**: API repository migration (CrewRepository)
- **Wed**: Performance optimization + load testing
- **Thu**: Accessibility audit (WCAG AA)
- **Fri**: Production deployment + monitoring

**Deliverables**: PRODUCTION READY 🚀

---

## Success Metrics

### Technical Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Unit Test Coverage** | 90%+ | JaCoCo report |
| **Integration Test Coverage** | 80%+ | Manual tracking |
| **API Response Time (p95)** | <200ms | k6 load test |
| **Upload Success Rate** | >95% | Application logs |
| **OCR Accuracy** | >85% | Manual validation |
| **Notification Delivery** | >99% | SendGrid/Twilio metrics |
| **WebSocket Latency** | <500ms | Real-time monitoring |
| **Build Time** | <5 minutes | CI/CD pipeline |

### User Experience Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Time to Upload Cert** | <3 minutes | User analytics |
| **Time to Create Worker** | <2 minutes | User analytics |
| **Time to Assign Crew** | <1 minute | User analytics |
| **Onboarding Completion** | >90% | Session analytics |
| **Onboarding Time** | <5 minutes | Session analytics |
| **Admin Approval Time** | <24 hours | Database query |
| **User Satisfaction** | >4.5/5 | In-app survey |

### Business Impact Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Document Generation Speed** | 3x faster | Before/after comparison |
| **Data Entry Reduction** | 80% less | Time tracking |
| **Compliance Rate** | 100% | Audit reports |
| **Certification Expiration Incidents** | 0 per quarter | Incident tracking |
| **Worker Onboarding Cost** | 50% reduction | HR metrics |

---

## Risk Assessment & Mitigation

### HIGH RISK Items

#### 1. API Migration Breaking Changes
**Risk**: Repositories behave differently with API vs in-memory
**Likelihood**: Medium | **Impact**: High
**Mitigation**:
- Feature flags for instant rollback
- Parallel implementation testing
- Comprehensive integration tests
- Gradual rollout (10% → 100%)

#### 2. Magic Link Security Vulnerabilities
**Risk**: Token prediction, replay attacks, phishing
**Likelihood**: Low | **Impact**: Critical
**Mitigation**:
- 256-bit cryptographically secure tokens
- Single-use + 24-hour expiration
- Rate limiting (5/hour per admin)
- Security audit + penetration testing
- Email domain validation

#### 3. WebSocket Concurrency Conflicts
**Risk**: Lost updates when multiple users edit same crew
**Likelihood**: Medium | **Impact**: Medium
**Mitigation**:
- Optimistic locking with timestamps
- Last Write Wins (LWW) conflict resolution
- Audit trail in `crew_member_history`
- User notifications on conflicts
- Comprehensive concurrency tests

### MEDIUM RISK Items

#### 4. OCR Extraction Accuracy
**Risk**: Low confidence extractions cause manual review burden
**Likelihood**: Medium | **Impact**: Medium
**Mitigation**:
- Threshold: 85% confidence required for auto-fill
- Manual review queue for low confidence
- User can override any field
- Continuous improvement with training data

#### 5. S3 Upload Failures
**Risk**: Large files fail to upload on poor connections
**Likelihood**: Medium | **Impact**: Medium
**Mitigation**:
- Image compression (500KB max)
- Chunked uploads with retry
- Offline queue for failed uploads
- Progress indicators + resumable uploads

### LOW RISK Items

#### 6. Notification Delivery Failures
**Risk**: Workers miss expiration alerts
**Likelihood**: Low | **Impact**: Low
**Mitigation**:
- Multi-channel (email + SMS + push)
- Retry logic (3 attempts)
- Monitor delivery rates (>99% target)
- Escalate to safety manager if worker unreachable

---

## Production Readiness Checklist

### Before Deployment

**Infrastructure**:
- [ ] PostgreSQL production database configured
- [ ] AWS S3 buckets created (worker-photos, certifications)
- [ ] Google Document AI API key provisioned
- [ ] SendGrid account set up (email)
- [ ] Twilio account set up (SMS)
- [ ] Redis instance configured (WebSocket PubSub)
- [ ] CloudFront CDN configured (S3 assets)
- [ ] AWS Cognito user pools configured

**Security**:
- [ ] RLS policies tested with multiple companies
- [ ] Magic link token security audited
- [ ] File upload virus scanning enabled
- [ ] API rate limiting configured
- [ ] HTTPS enforced (no HTTP)
- [ ] Secrets stored in environment variables (never in code)
- [ ] Penetration test completed (zero critical vulnerabilities)

**Testing**:
- [ ] Unit tests passing (90%+ coverage)
- [ ] Integration tests passing (80%+ coverage)
- [ ] Load tests passing (API <200ms p95)
- [ ] UI tests passing (all critical flows)
- [ ] Security tests passing (magic links, file uploads)
- [ ] Accessibility audit passed (WCAG AA)

**Monitoring**:
- [ ] Application logs configured (Loki/CloudWatch)
- [ ] Error tracking (Sentry)
- [ ] Performance monitoring (Prometheus/Grafana)
- [ ] Uptime monitoring (PagerDuty)
- [ ] Alerts configured (error rate, latency, uptime)

**Documentation**:
- [ ] API documentation published (Swagger/OpenAPI)
- [ ] User guides written (worker onboarding, cert upload)
- [ ] Admin guides written (verification workflow, crew management)
- [ ] Deployment runbook created
- [ ] Incident response playbook created

**Deployment**:
- [ ] Staging environment tested end-to-end
- [ ] Database migration scripts tested
- [ ] Rollback plan documented
- [ ] Feature flags configured (gradual rollout)
- [ ] Beta testing with 3+ construction companies

---

## Next Immediate Actions

### This Week (Priority 1)

**1. Fix Technical Debt** (2 days):
- [ ] Consolidate pagination models (delete duplicates)
- [ ] Remove `CrewModels.kt` (use individual model files)
- [ ] Fix shared module compilation errors
- [ ] Update all imports

**2. Start Phase 2** (3 days):
- [ ] Implement `FileUploadService` interface
- [ ] Set up AWS S3 bucket for testing
- [ ] Create `CertificationUploadScreen` skeleton
- [ ] Write unit tests for file upload logic

**3. Backend Setup** (concurrent):
- [ ] Provision Google Document AI API key
- [ ] Set up SendGrid account (email notifications)
- [ ] Configure backend endpoints for certification upload
- [ ] Deploy backend to staging environment

### Next Week (Priority 2)

**1. Complete Phase 2 Services** (3 days):
- [ ] Implement `OCRService` (backend integration)
- [ ] Implement `NotificationService` (SendGrid + Twilio)
- [ ] Write integration tests for OCR extraction

**2. Build Phase 2 UI** (2 days):
- [ ] Complete `CertificationUploadScreen` (camera + OCR workflow)
- [ ] Build `CertificationVerificationScreen` (admin UI)
- [ ] Add haptic feedback and animations

### Week 3+ (Priority 3)

- [ ] Start API client layer implementation
- [ ] Begin Phase 4 (Crew Builder)
- [ ] Run beta testing with first construction company
- [ ] Iterate based on feedback

---

## Conclusion

This plan provides a clear, actionable roadmap to complete the crew management system. By prioritizing **Phase 2 (Certification Management)** first, we deliver immediate business value (compliance tracking) while building reusable services (FileUpload, Notification) that benefit future phases.

**Key Success Factors**:
1. **Incremental Delivery**: Each phase is self-contained and deliverable
2. **Technical Excellence**: 90%+ test coverage, comprehensive error handling
3. **User-Centric**: Fast, intuitive workflows optimized for construction workers
4. **Production-Ready**: Security, monitoring, rollback plans all addressed

**Estimated Timeline**: 5.5 weeks to production-ready system (60% remaining work)

---

**Document Version**: 1.0
**Created**: October 8, 2025 15:09:00
**Next Review**: After Phase 2 completion (Week 2)
**Owner**: Development Team

---

## Related Documentation

- [Original Implementation Plan](/docs/implementation/crew-management-implementation-plan.md)
- [Phase 1 & 5 Completion Log](/docs/implementation/20251008-142500-crew-management-implementation-log.md)
- [Testing Strategy](/docs/testing/crew-management-testing-strategy.md)
- [Database Schema](/database/README.md)
- [Quick Reference](/database/QUICK_REFERENCE.md)
