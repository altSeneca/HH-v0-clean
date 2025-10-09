# Web Certification Upload - Next.js Implementation Plan

**Date**: October 8, 2025 (21:05:00)
**Priority**: HIGH (User Requirement)
**Estimated Timeline**: 1-2 weeks
**Status**: Planning Complete - Ready for Implementation

---

## Executive Summary

Create a Next.js web application for HazardHawk certification upload and verification, allowing workers to upload certifications from any device with a browser. This addresses the critical requirement that most employees won't have the Android/iOS app installed.

**Key Insight**: The backend services (FileUploadService, OCRService, NotificationService) are already implemented in the Kotlin Multiplatform shared module. The web app will use the same backend API endpoints documented in `phase2-backend-api-requirements.md`.

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Feature Parity Matrix](#feature-parity-matrix)
5. [Implementation Phases](#implementation-phases)
6. [Component Specifications](#component-specifications)
7. [API Integration](#api-integration)
8. [Mobile-First Design](#mobile-first-design)
9. [Testing Strategy](#testing-strategy)
10. [Deployment Strategy](#deployment-strategy)

---

## Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    HazardHawk Ecosystem                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │   Android    │  │   Next.js    │  │   iOS App    │    │
│  │   App (KMP)  │  │   Web App    │  │  (Future)    │    │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘    │
│         │                  │                  │            │
│         └──────────────────┼──────────────────┘            │
│                            │                               │
│                   ┌────────▼────────┐                      │
│                   │  Backend API    │                      │
│                   │  (Go/Kotlin)    │                      │
│                   └────────┬────────┘                      │
│                            │                               │
│         ┌──────────────────┼──────────────────┐            │
│         │                  │                  │            │
│  ┌──────▼──────┐  ┌────────▼────────┐  ┌─────▼─────┐     │
│  │ PostgreSQL  │  │  AWS S3 +       │  │  Google   │     │
│  │  Database   │  │  CloudFront     │  │ Document  │     │
│  └─────────────┘  └─────────────────┘  │    AI     │     │
│                                        └───────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Shared Infrastructure

Both Android and Web apps will use:
- **Same Backend API** endpoints
- **Same Database** (PostgreSQL with RLS)
- **Same S3 Buckets** for file storage
- **Same OCR Service** (Google Document AI)
- **Same Notification Service** (SendGrid/Twilio)

---

## Technology Stack

### Core Framework
- **Next.js 14** (App Router)
- **React 18** (Server Components + Client Components)
- **TypeScript** (strict mode)
- **Tailwind CSS** (utility-first styling)

### UI Components
- **shadcn/ui** (Accessible Radix UI primitives)
- **Framer Motion** (animations)
- **React Dropzone** (file upload)
- **React Webcam** (camera capture)

### State Management
- **Zustand** (lightweight, TypeScript-friendly)
- **TanStack Query** (server state management)
- **React Hook Form** (form validation)

### API & Data
- **Axios** (HTTP client)
- **Zod** (schema validation)
- **date-fns** (date utilities)

### Authentication
- **NextAuth.js** (v5 / Auth.js)
- **JWT** (matching backend auth)

### Deployment
- **Vercel** (Next.js optimized hosting)
- **Edge Functions** (global CDN)
- **Vercel Analytics** (web vitals tracking)

### Testing
- **Vitest** (unit tests)
- **Playwright** (E2E tests)
- **React Testing Library** (component tests)

---

## Project Structure

```
hazardhawk-web/
├── app/                          # Next.js 14 App Router
│   ├── (auth)/
│   │   ├── login/
│   │   └── register/
│   ├── (dashboard)/
│   │   ├── certifications/
│   │   │   ├── upload/          # Worker upload flow
│   │   │   └── verify/          # Admin verification
│   │   ├── workers/
│   │   └── settings/
│   ├── api/                      # API routes (proxy/auth)
│   │   ├── auth/
│   │   └── upload/
│   ├── layout.tsx
│   └── page.tsx
├── components/
│   ├── ui/                       # shadcn/ui primitives
│   ├── certifications/
│   │   ├── upload-wizard.tsx    # Multi-step upload
│   │   ├── camera-capture.tsx   # Web camera
│   │   ├── ocr-review.tsx       # OCR results
│   │   └── verification-queue.tsx
│   ├── layout/
│   │   ├── navbar.tsx
│   │   ├── sidebar.tsx
│   │   └── mobile-nav.tsx
│   └── shared/
│       ├── file-uploader.tsx
│       ├── loading-spinner.tsx
│       └── error-boundary.tsx
├── lib/
│   ├── api/
│   │   ├── client.ts            # Axios instance
│   │   ├── certifications.ts    # API methods
│   │   └── workers.ts
│   ├── hooks/
│   │   ├── use-upload.ts
│   │   └── use-camera.ts
│   ├── stores/
│   │   └── upload-store.ts      # Zustand store
│   ├── utils/
│   │   ├── file-validation.ts
│   │   └── image-compression.ts
│   └── schemas/
│       └── certification.ts     # Zod schemas
├── public/
│   ├── icons/
│   └── images/
├── styles/
│   └── globals.css
├── types/
│   ├── api.ts
│   └── certification.ts
├── .env.local                   # Environment variables
├── next.config.js
├── tailwind.config.ts
├── tsconfig.json
└── package.json
```

---

## Feature Parity Matrix

### Worker Upload Flow

| Feature | Android App | Web App | Implementation |
|---------|-------------|---------|----------------|
| **Source Selection** | Camera + Gallery | Camera + File Upload | ✅ Web: Webcam API + File Input |
| **Camera Capture** | CameraX | Webcam API | ✅ React Webcam component |
| **Document Frame Overlay** | Canvas animation | CSS overlay | ✅ Tailwind + Framer Motion |
| **File Upload** | S3 presigned URL | Same | ✅ Shared API endpoint |
| **Progress Tracking** | 0-100% | Same | ✅ Progress bar component |
| **OCR Processing** | Backend API | Same | ✅ Shared API endpoint |
| **Confidence Badges** | Material 3 | Tailwind badges | ✅ Color-coded badges |
| **Manual Entry** | Form fields | Same | ✅ React Hook Form |
| **Success Animation** | Compose animation | Framer Motion | ✅ Lottie/Framer Motion |
| **Haptic Feedback** | Android | N/A | ❌ Web limitation |

### Admin Verification Flow

| Feature | Android App | Web App | Implementation |
|---------|-------------|---------|----------------|
| **Statistics Dashboard** | Material 3 cards | Tailwind cards | ✅ Shared design system |
| **Queue Overview** | LazyColumn | Virtual scroll | ✅ TanStack Virtual |
| **Filter/Sort** | Chips + Dropdown | Same | ✅ shadcn/ui components |
| **Document Viewer** | Pinch zoom | Mouse zoom | ✅ React Zoom Pan Pinch |
| **60/40 Split Layout** | Compose Row | CSS Grid | ✅ Responsive grid |
| **Rejection Dialog** | Material Dialog | Modal | ✅ shadcn/ui Dialog |
| **Keyboard Shortcuts** | N/A | A/R/S keys | ✅ Web advantage |
| **Bulk Actions** | Future | ✅ Implemented | ✅ Web advantage |

---

## Implementation Phases

### Phase 1: Project Setup (Week 1, Day 1-2)

**Estimated Time**: 1-2 days

**Tasks**:
1. Initialize Next.js 14 project
2. Configure TypeScript + ESLint + Prettier
3. Install and configure dependencies
4. Set up Tailwind CSS + shadcn/ui
5. Configure environment variables
6. Set up API client with Axios
7. Create base layout components

**Deliverables**:
```bash
npx create-next-app@latest hazardhawk-web --typescript --tailwind --app
cd hazardhawk-web
npx shadcn-ui@latest init
npm install zustand @tanstack/react-query axios zod react-hook-form
npm install framer-motion react-dropzone react-webcam
npm install -D vitest @playwright/test
```

**Key Files**:
- `lib/api/client.ts` - Axios instance with auth interceptors
- `lib/stores/upload-store.ts` - Zustand state management
- `app/layout.tsx` - Root layout with providers

---

### Phase 2: Authentication & Layout (Week 1, Day 3)

**Estimated Time**: 1 day

**Tasks**:
1. Set up NextAuth.js with JWT
2. Create login/register pages
3. Implement protected routes
4. Build responsive layout (navbar, sidebar)
5. Create mobile navigation

**Components**:
```typescript
// app/(auth)/login/page.tsx
'use client'
import { signIn } from 'next-auth/react'

export default function LoginPage() {
  return (
    <div className="min-h-screen flex items-center justify-center">
      <form className="w-full max-w-md space-y-4">
        <Input type="email" placeholder="Email" />
        <Input type="password" placeholder="Password" />
        <Button onClick={() => signIn('credentials')} className="w-full">
          Sign In
        </Button>
      </form>
    </div>
  )
}
```

---

### Phase 3: Worker Upload Flow (Week 1, Day 4-5 + Week 2, Day 1)

**Estimated Time**: 3 days

**Tasks**:
1. Build multi-step upload wizard
2. Implement camera capture component
3. Create file upload with drag-and-drop
4. Add image compression (browser-based)
5. Build OCR review component
6. Create manual entry form
7. Add success/error states

**State Machine** (matches Android):
```typescript
// lib/stores/upload-store.ts
import { create } from 'zustand'

type UploadState =
  | { type: 'idle' }
  | { type: 'camera' }
  | { type: 'uploading', progress: number }
  | { type: 'processing' }
  | { type: 'ocr-review', data: OCRData }
  | { type: 'manual-entry' }
  | { type: 'submitting' }
  | { type: 'success' }
  | { type: 'error', message: string }

interface UploadStore {
  state: UploadState
  setState: (state: UploadState) => void
  reset: () => void
}

export const useUploadStore = create<UploadStore>((set) => ({
  state: { type: 'idle' },
  setState: (state) => set({ state }),
  reset: () => set({ state: { type: 'idle' } })
}))
```

**Key Components**:

1. **UploadWizard.tsx** (Main orchestrator)
```typescript
'use client'
import { useUploadStore } from '@/lib/stores/upload-store'

export function UploadWizard() {
  const { state } = useUploadStore()

  return (
    <div className="max-w-4xl mx-auto p-6">
      {state.type === 'idle' && <SourceSelection />}
      {state.type === 'camera' && <CameraCapture />}
      {state.type === 'uploading' && <UploadProgress progress={state.progress} />}
      {state.type === 'processing' && <ProcessingSpinner />}
      {state.type === 'ocr-review' && <OCRReview data={state.data} />}
      {state.type === 'manual-entry' && <ManualEntryForm />}
      {state.type === 'success' && <SuccessAnimation />}
      {state.type === 'error' && <ErrorDisplay message={state.message} />}
    </div>
  )
}
```

2. **CameraCapture.tsx** (Web camera)
```typescript
'use client'
import Webcam from 'react-webcam'
import { useCallback, useRef } from 'react'

export function CameraCapture() {
  const webcamRef = useRef<Webcam>(null)
  const { setState } = useUploadStore()

  const capture = useCallback(() => {
    const imageSrc = webcamRef.current?.getScreenshot()
    if (imageSrc) {
      // Convert to file and upload
      uploadFile(dataURLtoFile(imageSrc, 'certification.jpg'))
    }
  }, [])

  return (
    <div className="relative">
      <Webcam
        ref={webcamRef}
        screenshotFormat="image/jpeg"
        className="w-full rounded-lg"
      />
      {/* Document frame overlay */}
      <div className="absolute inset-0 pointer-events-none">
        <DocumentFrameOverlay />
      </div>
      <Button onClick={capture} className="mt-4 w-full h-20 text-xl">
        📸 Capture Photo
      </Button>
    </div>
  )
}
```

3. **OCRReview.tsx** (Confidence badges)
```typescript
import { Badge } from '@/components/ui/badge'

export function OCRReview({ data }: { data: OCRData }) {
  const confidenceColor = data.confidence >= 0.85
    ? 'bg-green-500'
    : data.confidence >= 0.60
    ? 'bg-amber-500'
    : 'bg-red-500'

  return (
    <div className="space-y-4">
      <Badge className={confidenceColor}>
        {data.confidence >= 0.85 ? '✓ High' : '⚠️ Low'} Confidence
        {` ${Math.round(data.confidence * 100)}%`}
      </Badge>

      <div className="grid gap-4">
        <ExtractedField label="Name" value={data.holderName} />
        <ExtractedField label="Type" value={data.certificationType} />
        <ExtractedField label="Number" value={data.certificationNumber} />
        <ExtractedField label="Expiration" value={data.expirationDate} />
      </div>

      <div className="flex gap-4">
        <Button onClick={confirm} className="flex-1 h-20 bg-green-600">
          ✓ Confirm & Submit
        </Button>
        <Button onClick={edit} variant="outline" className="flex-1 h-20">
          ✎ Edit Manually
        </Button>
      </div>
    </div>
  )
}
```

---

### Phase 4: Admin Verification (Week 2, Day 2-3)

**Estimated Time**: 2 days

**Tasks**:
1. Build statistics dashboard
2. Create filter/sort controls
3. Implement queue list with virtual scroll
4. Build document viewer with zoom
5. Create rejection dialog
6. Add keyboard shortcuts (A=Approve, R=Reject, S=Skip)

**Key Components**:

1. **VerificationQueue.tsx**
```typescript
'use client'
import { useVirtualizer } from '@tanstack/react-virtual'
import { useQuery } from '@tanstack/react-query'

export function VerificationQueue() {
  const { data: queue } = useQuery({
    queryKey: ['certifications', 'pending'],
    queryFn: () => api.certifications.getPending()
  })

  const parentRef = useRef<HTMLDivElement>(null)
  const virtualizer = useVirtualizer({
    count: queue?.length ?? 0,
    getScrollElement: () => parentRef.current,
    estimateSize: () => 100,
  })

  return (
    <div ref={parentRef} className="h-screen overflow-auto">
      {virtualizer.getVirtualItems().map((item) => (
        <CertificationCard
          key={item.key}
          cert={queue[item.index]}
          onClick={() => selectItem(queue[item.index])}
        />
      ))}
    </div>
  )
}
```

2. **DocumentViewer.tsx** (Zoom + Pan)
```typescript
'use client'
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch'

export function DocumentViewer({ url }: { url: string }) {
  return (
    <div className="bg-black h-full">
      <TransformWrapper
        initialScale={1}
        minScale={0.5}
        maxScale={5}
      >
        {({ zoomIn, zoomOut, resetTransform }) => (
          <>
            <TransformComponent>
              <img src={url} alt="Certification" className="w-full" />
            </TransformComponent>

            <div className="absolute bottom-4 right-4 flex flex-col gap-2">
              <Button onClick={() => zoomIn()} size="icon">+</Button>
              <Button onClick={() => zoomOut()} size="icon">−</Button>
              <Button onClick={() => resetTransform()} size="icon">⟲</Button>
            </div>
          </>
        )}
      </TransformWrapper>
    </div>
  )
}
```

3. **Keyboard Shortcuts**
```typescript
'use client'
import { useEffect } from 'react'

export function useVerificationShortcuts() {
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'a') approveItem()
      if (e.key === 'r') rejectItem()
      if (e.key === 's') skipItem()
    }

    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [])
}
```

---

### Phase 5: Mobile Optimization (Week 2, Day 4)

**Estimated Time**: 1 day

**Tasks**:
1. Optimize for mobile browsers
2. Add touch gestures
3. Implement pull-to-refresh
4. Test on iOS Safari, Chrome Mobile
5. Add PWA manifest

**Mobile-First CSS** (Tailwind):
```typescript
// components/certifications/upload-wizard.tsx
<div className="
  px-4 py-6           // Mobile padding
  md:px-8 md:py-8     // Tablet padding
  lg:px-12 lg:py-12   // Desktop padding
">
  <Button className="
    h-20 text-xl      // Large touch target
    w-full            // Full width on mobile
    md:w-auto         // Auto width on tablet+
  ">
    Upload Certification
  </Button>
</div>
```

**PWA Manifest**:
```json
// public/manifest.json
{
  "name": "HazardHawk",
  "short_name": "HazardHawk",
  "description": "Construction Safety Platform",
  "start_url": "/",
  "display": "standalone",
  "background_color": "#ffffff",
  "theme_color": "#1E40AF",
  "icons": [
    {
      "src": "/icon-192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/icon-512.png",
      "sizes": "512x512",
      "type": "image/png"
    }
  ]
}
```

---

### Phase 6: Testing & Deployment (Week 2, Day 5)

**Estimated Time**: 1 day

**Tasks**:
1. Write unit tests (Vitest)
2. Write E2E tests (Playwright)
3. Configure Vercel deployment
4. Set up environment variables
5. Test production build

**Example Tests**:
```typescript
// __tests__/upload-wizard.test.tsx
import { render, screen, fireEvent } from '@testing-library/react'
import { UploadWizard } from '@/components/certifications/upload-wizard'

describe('UploadWizard', () => {
  it('starts in idle state', () => {
    render(<UploadWizard />)
    expect(screen.getByText('Upload Certification')).toBeInTheDocument()
  })

  it('transitions to camera state on camera click', () => {
    render(<UploadWizard />)
    fireEvent.click(screen.getByText('Take Photo'))
    expect(screen.getByText('Capture')).toBeInTheDocument()
  })
})
```

---

## API Integration

### Backend API Endpoints (Reuse Existing)

All endpoints from `phase2-backend-api-requirements.md`:

```typescript
// lib/api/certifications.ts
import { apiClient } from './client'

export const certificationsApi = {
  // Upload flow
  getPresignedUrl: async (params: PresignedUrlParams) =>
    apiClient.post('/api/storage/presigned-url', params),

  extractOCR: async (documentUrl: string) =>
    apiClient.post('/api/ocr/extract-certification', { documentUrl }),

  submitCertification: async (data: CertificationData) =>
    apiClient.post('/api/certifications', data),

  // Verification flow
  getPendingQueue: async (filter?: FilterParams) =>
    apiClient.get('/api/certifications/pending', { params: filter }),

  approve: async (certId: string) =>
    apiClient.post(`/api/certifications/${certId}/approve`),

  reject: async (certId: string, reason: string) =>
    apiClient.post(`/api/certifications/${certId}/reject`, { reason }),
}
```

### Image Compression (Browser-Side)

```typescript
// lib/utils/image-compression.ts
import imageCompression from 'browser-image-compression'

export async function compressImage(file: File): Promise<File> {
  const options = {
    maxSizeMB: 0.5,
    maxWidthOrHeight: 1920,
    useWebWorker: true,
  }

  try {
    return await imageCompression(file, options)
  } catch (error) {
    console.error('Compression failed:', error)
    return file
  }
}
```

---

## Mobile-First Design

### Responsive Breakpoints

```typescript
// tailwind.config.ts
export default {
  theme: {
    screens: {
      'sm': '640px',   // Mobile landscape
      'md': '768px',   // Tablet
      'lg': '1024px',  // Desktop
      'xl': '1280px',  // Large desktop
    }
  }
}
```

### Touch-Friendly Components

**Minimum Touch Targets**: 48x48px (Apple/Google guidelines)

```typescript
// components/ui/button.tsx
const buttonVariants = cva(
  "inline-flex items-center justify-center rounded-md transition-colors",
  {
    variants: {
      size: {
        default: "h-12 px-6 text-base",     // 48px height
        lg: "h-20 px-8 text-xl",            // 80px height (construction)
        icon: "h-12 w-12",                  // 48px square
      }
    }
  }
)
```

### Mobile Camera Considerations

```typescript
// components/certifications/camera-capture.tsx
const videoConstraints = {
  facingMode: 'environment',  // Rear camera on mobile
  width: { ideal: 1920 },
  height: { ideal: 1080 },
}

<Webcam
  videoConstraints={videoConstraints}
  screenshotFormat="image/jpeg"
  screenshotQuality={0.92}
/>
```

---

## Testing Strategy

### Unit Tests (Vitest)
```typescript
// __tests__/api/certifications.test.ts
import { describe, it, expect, vi } from 'vitest'
import { certificationsApi } from '@/lib/api/certifications'

describe('certificationsApi', () => {
  it('uploads file with presigned URL', async () => {
    const mockUrl = await certificationsApi.getPresignedUrl({
      bucket: 'hazardhawk-certifications',
      key: 'test.pdf'
    })

    expect(mockUrl).toContain('s3.amazonaws.com')
  })
})
```

### E2E Tests (Playwright)
```typescript
// e2e/upload-flow.spec.ts
import { test, expect } from '@playwright/test'

test('worker can upload certification', async ({ page }) => {
  await page.goto('/certifications/upload')

  // Select file
  const fileInput = page.locator('input[type="file"]')
  await fileInput.setInputFiles('test-cert.pdf')

  // Wait for OCR
  await page.waitForSelector('[data-testid="ocr-results"]')

  // Verify confidence badge
  const badge = page.locator('[data-testid="confidence-badge"]')
  await expect(badge).toContainText('%')

  // Submit
  await page.click('button:has-text("Confirm & Submit")')
  await expect(page.locator('text=Success')).toBeVisible()
})
```

---

## Deployment Strategy

### Vercel Configuration

```javascript
// next.config.js
module.exports = {
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL,
  },
  images: {
    domains: ['hazardhawk-certifications.s3.amazonaws.com'],
  },
  experimental: {
    serverActions: true,
  },
}
```

### Environment Variables

```bash
# .env.local (development)
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-here

# Vercel (production)
NEXT_PUBLIC_API_URL=https://api.hazardhawk.com
NEXTAUTH_URL=https://app.hazardhawk.com
NEXTAUTH_SECRET=production-secret
```

### Deployment Pipeline

1. **GitHub Integration**
   - Push to `main` → Deploy to production
   - Push to `develop` → Deploy to staging

2. **Environment Branches**
   ```
   main → app.hazardhawk.com
   develop → staging.hazardhawk.com
   feature/* → Preview deployments
   ```

3. **Build Optimization**
   ```bash
   npm run build
   # Output: .next/static (CDN-optimized)
   # Edge Functions: API routes deployed globally
   ```

---

## Success Criteria

### Technical Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| **Initial Load Time** | <2s | Lighthouse |
| **Time to Interactive** | <3s | Web Vitals |
| **First Contentful Paint** | <1s | Web Vitals |
| **Mobile Score** | 90+ | Lighthouse |
| **Accessibility Score** | 100 | Lighthouse |
| **SEO Score** | 90+ | Lighthouse |

### Functional Metrics

| Feature | Target | Measurement |
|---------|--------|-------------|
| **Upload Success Rate** | >95% | Analytics |
| **OCR Accuracy** | >85% | Manual validation |
| **Mobile Upload Time** | <30s | User testing |
| **Browser Support** | 95%+ users | Can I Use |

### User Experience

- ✅ Workers can upload from any device
- ✅ Mobile-first responsive design
- ✅ Offline-capable (PWA)
- ✅ Touch-friendly (48px+ targets)
- ✅ Fast loading (<3s interactive)

---

## Implementation Checklist

### Week 1
- [ ] Day 1-2: Project setup + dependencies
- [ ] Day 3: Authentication + layout
- [ ] Day 4-5: Worker upload flow (camera + file upload)

### Week 2
- [ ] Day 1: Worker upload flow (OCR + manual entry)
- [ ] Day 2-3: Admin verification flow
- [ ] Day 4: Mobile optimization + PWA
- [ ] Day 5: Testing + deployment

---

## Cost Estimate

### Monthly Costs (100 users, 50 certifications/month)

| Service | Cost | Notes |
|---------|------|-------|
| **Vercel Pro** | $20/mo | Hosting + Edge Functions |
| **Existing Backend** | $0 | Already provisioned for Android |
| **Total** | **$20/mo** | Shared infrastructure with Android |

---

## Next Steps (Priority Order)

### Immediate
1. **Create Next.js Project** (1 day)
   ```bash
   npx create-next-app@latest hazardhawk-web
   cd hazardhawk-web
   npx shadcn-ui@latest init
   ```

2. **Set Up API Client** (1 day)
   - Configure Axios with auth
   - Create TypeScript types from backend spec
   - Test presigned URL endpoint

### Short-Term (This Week)
3. **Build Upload Wizard** (3 days)
   - Source selection
   - Camera capture
   - File upload
   - OCR review
   - Manual entry

4. **Mobile Testing** (1 day)
   - Test on iOS Safari
   - Test on Chrome Mobile
   - Optimize touch targets

### Medium-Term (Next Week)
5. **Admin Verification** (2 days)
   - Queue management
   - Document viewer
   - Approval workflow

6. **Deploy to Staging** (1 day)
   - Vercel deployment
   - Environment variables
   - Domain configuration

---

## Conclusion

This plan provides a comprehensive roadmap for creating a web-based certification upload system that achieves feature parity with the Android app while leveraging web-specific advantages (keyboard shortcuts, bulk actions, broader device support).

**Key Benefits**:
- ✅ **Zero app install required** - Workers use any browser
- ✅ **Shared infrastructure** - Same backend as Android
- ✅ **Mobile-first design** - Optimized for construction workers
- ✅ **Fast deployment** - 1-2 weeks to production
- ✅ **Cost-effective** - Only $20/mo additional cost

**Estimated Timeline**: 1-2 weeks to production-ready web app

---

**Document Version**: 1.0
**Created**: October 8, 2025 21:05:00
**Status**: Ready for Implementation
**Next Action**: Create Next.js project

---

## Related Documentation

- [Phase 2 Backend API Requirements](/docs/implementation/phase2-backend-api-requirements.md)
- [Phase 2 UI Implementation Complete](/docs/implementation/20251008-210000-phase2-ui-implementation-complete.md)
- [Crew Management Next Steps](/docs/plan/20251008-150900-crew-management-next-steps.md)
