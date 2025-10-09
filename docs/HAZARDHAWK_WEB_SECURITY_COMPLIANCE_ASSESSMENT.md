# HazardHawk Web Portal Security & Compliance Assessment

**Document Version**: 1.0
**Date**: October 9, 2025
**Scope**: Web Certification Portal (Next.js Application)
**Assessment Type**: Comprehensive Security & Compliance Review
**Status**: Pre-Production Security Analysis

---

## Executive Summary

This document provides a comprehensive security and compliance assessment for the HazardHawk web certification portal, focusing on data privacy (GDPR/CCPA), form security, infrastructure hardening, third-party integrations, construction industry compliance, and vulnerability prevention.

### Current Security Posture

**Status**: ⚠️ **NEEDS ENHANCEMENT** - The application has basic security measures but requires additional hardening before production deployment.

**Risk Level**: MEDIUM-HIGH (construction industry with PII/certification data)

**Key Findings**:
- ✅ Basic input validation implemented (Zod schemas)
- ✅ File upload restrictions in place (type, size)
- ✅ HTTPS-only configuration (Next.js default)
- ⚠️ Missing CSRF protection
- ⚠️ No rate limiting implemented
- ⚠️ Missing Content Security Policy headers
- ⚠️ No cookie consent mechanism
- ⚠️ Authentication system not implemented
- ⚠️ Missing audit logging

---

## 1. Data Privacy Compliance

### 1.1 GDPR Compliance (EU Construction Companies)

#### Current Status: ⚠️ PARTIALLY COMPLIANT

**Legal Basis for Processing**: Not documented
**Data Subject Rights**: Not implemented
**Privacy by Design**: Partially implemented

#### Required Implementations:

##### 1.1.1 Privacy Policy & Notices

**File**: `/hazardhawk-web/src/app/privacy/page.tsx` (NOT CREATED)

```typescript
// Required privacy policy sections for GDPR
export default function PrivacyPolicyPage() {
  return (
    <LegalDocument title="Privacy Policy">
      {/* 1. Data Controller Information */}
      <Section id="controller">
        <p>HazardHawk, Inc. (Data Controller)</p>
        <p>Address: [Company Address]</p>
        <p>DPO Contact: privacy@hazardhawk.com</p>
      </Section>

      {/* 2. Data Collection - What We Collect */}
      <Section id="data-collected">
        <h2>Information We Collect</h2>
        <ul>
          <li>Personal identification: Name, worker ID, SST number</li>
          <li>Certification documents: Images, PDFs, OCR-extracted text</li>
          <li>Employment data: Company ID, project ID, role</li>
          <li>Technical data: IP address, browser type, device info</li>
          <li>Usage data: Upload times, verification actions, session IDs</li>
        </ul>
      </Section>

      {/* 3. Legal Basis (GDPR Article 6) */}
      <Section id="legal-basis">
        <h2>Legal Basis for Processing</h2>
        <ul>
          <li><strong>Contractual necessity</strong>: Processing certifications for employment verification</li>
          <li><strong>Legal obligation</strong>: OSHA compliance, construction site safety requirements</li>
          <li><strong>Legitimate interests</strong>: Workplace safety, fraud prevention</li>
        </ul>
      </Section>

      {/* 4. Data Retention */}
      <Section id="retention">
        <h2>Data Retention</h2>
        <p>Certification data: Retained for duration of employment + 7 years (regulatory requirement)</p>
        <p>Usage logs: 90 days</p>
        <p>Marketing analytics: 13 months (with consent)</p>
      </Section>

      {/* 5. Data Subject Rights (GDPR Articles 15-22) */}
      <Section id="rights">
        <h2>Your Rights Under GDPR</h2>
        <ul>
          <li><strong>Right to Access</strong>: Request a copy of your personal data</li>
          <li><strong>Right to Rectification</strong>: Correct inaccurate data</li>
          <li><strong>Right to Erasure</strong>: Delete your data (with exceptions)</li>
          <li><strong>Right to Restriction</strong>: Limit how we use your data</li>
          <li><strong>Right to Data Portability</strong>: Receive your data in machine-readable format</li>
          <li><strong>Right to Object</strong>: Object to certain processing activities</li>
        </ul>
        <p>To exercise these rights: <a href="mailto:privacy@hazardhawk.com">privacy@hazardhawk.com</a></p>
      </Section>

      {/* 6. International Transfers */}
      <Section id="transfers">
        <h2>International Data Transfers</h2>
        <p>Data stored in AWS US-EAST-1 (Virginia). For EU users, we use Standard Contractual Clauses (SCCs) approved by the EU Commission.</p>
      </Section>

      {/* 7. Third-Party Processors */}
      <Section id="processors">
        <h2>Third-Party Data Processors</h2>
        <ul>
          <li>AWS S3 (document storage) - DPA signed</li>
          <li>Google Document AI (OCR processing) - DPA signed</li>
          <li>NYC DOB Training Connect (certification verification) - Government entity</li>
        </ul>
      </Section>

      {/* 8. Cookies and Tracking */}
      <Section id="cookies">
        <h2>Cookies and Tracking</h2>
        <p>Essential cookies: Session management (strictly necessary)</p>
        <p>Analytics cookies: With explicit consent only</p>
        <p>Cookie banner: Displayed on first visit</p>
      </Section>

      {/* 9. Data Breach Notification */}
      <Section id="breach">
        <h2>Data Breach Notification</h2>
        <p>We will notify supervisory authorities within 72 hours of becoming aware of a breach affecting EU residents.</p>
        <p>Affected individuals will be notified without undue delay if high risk to rights and freedoms.</p>
      </Section>

      {/* 10. Contact Information */}
      <Section id="contact">
        <h2>Questions or Concerns</h2>
        <p>Data Protection Officer: dpo@hazardhawk.com</p>
        <p>EU Representative: [If applicable]</p>
        <p>Supervisory Authority: [Relevant EU data protection authority]</p>
      </Section>

      <p className="text-sm text-gray-600 mt-8">
        Last updated: October 9, 2025<br />
        Effective date: [Production launch date]
      </p>
    </LegalDocument>
  );
}
```

##### 1.1.2 Cookie Consent Banner

**File**: `/hazardhawk-web/src/components/compliance/cookie-consent.tsx` (NOT CREATED)

```typescript
'use client';

import { useState, useEffect } from 'react';
import { Cookie, X, Settings } from 'lucide-react';
import { Button } from '@/components/ui/button';

interface CookieConsent {
  necessary: boolean; // Always true
  analytics: boolean;
  marketing: boolean;
}

export function CookieConsentBanner() {
  const [showBanner, setShowBanner] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [consent, setConsent] = useState<CookieConsent>({
    necessary: true,
    analytics: false,
    marketing: false,
  });

  useEffect(() => {
    // Check if user has already consented
    const savedConsent = localStorage.getItem('cookie-consent');
    if (!savedConsent) {
      setShowBanner(true);
    } else {
      setConsent(JSON.parse(savedConsent));
    }
  }, []);

  const saveConsent = (newConsent: CookieConsent) => {
    localStorage.setItem('cookie-consent', JSON.stringify(newConsent));
    localStorage.setItem('cookie-consent-timestamp', new Date().toISOString());
    setConsent(newConsent);
    setShowBanner(false);
    setShowSettings(false);

    // Initialize analytics based on consent
    if (newConsent.analytics) {
      initializeAnalytics();
    }
    if (newConsent.marketing) {
      initializeMarketing();
    }
  };

  const acceptAll = () => {
    saveConsent({ necessary: true, analytics: true, marketing: true });
  };

  const rejectAll = () => {
    saveConsent({ necessary: true, analytics: false, marketing: false });
  };

  const acceptSelected = () => {
    saveConsent(consent);
  };

  if (!showBanner) return null;

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 p-4 bg-white border-t-2 border-gray-200 shadow-2xl">
      <div className="max-w-7xl mx-auto">
        {!showSettings ? (
          // Simple banner
          <div className="flex items-start gap-4">
            <Cookie className="w-8 h-8 text-yellow-600 flex-shrink-0 mt-1" />
            <div className="flex-1">
              <h3 className="text-lg font-bold text-gray-900 mb-2">
                We Value Your Privacy
              </h3>
              <p className="text-sm text-gray-700 mb-4">
                We use cookies to enhance your experience, analyze site usage, and improve our services.
                By clicking "Accept All", you consent to our use of cookies.
                You can customize your preferences by clicking "Cookie Settings".
              </p>
              <p className="text-xs text-gray-600 mb-4">
                Read our{' '}
                <a href="/privacy" className="text-blue-600 underline">
                  Privacy Policy
                </a>{' '}
                and{' '}
                <a href="/cookie-policy" className="text-blue-600 underline">
                  Cookie Policy
                </a>
                .
              </p>
              <div className="flex flex-wrap gap-3">
                <Button
                  onClick={acceptAll}
                  size="lg"
                  className="bg-green-600 hover:bg-green-700"
                >
                  Accept All
                </Button>
                <Button
                  onClick={rejectAll}
                  variant="outline"
                  size="lg"
                >
                  Reject All
                </Button>
                <Button
                  onClick={() => setShowSettings(true)}
                  variant="ghost"
                  size="lg"
                >
                  <Settings className="w-4 h-4 mr-2" />
                  Cookie Settings
                </Button>
              </div>
            </div>
            <Button
              onClick={() => setShowBanner(false)}
              variant="ghost"
              size="icon"
            >
              <X className="w-5 h-5" />
            </Button>
          </div>
        ) : (
          // Detailed settings
          <div>
            <h3 className="text-xl font-bold text-gray-900 mb-4">
              Cookie Preferences
            </h3>
            <div className="space-y-4 mb-6">
              {/* Necessary Cookies */}
              <div className="flex items-start gap-3 p-4 bg-gray-50 rounded-lg">
                <input
                  type="checkbox"
                  checked={true}
                  disabled
                  className="mt-1"
                />
                <div className="flex-1">
                  <h4 className="font-semibold text-gray-900">Strictly Necessary</h4>
                  <p className="text-sm text-gray-600">
                    Essential for the website to function. Cannot be disabled.
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    Examples: Session authentication, security tokens, form submissions
                  </p>
                </div>
              </div>

              {/* Analytics Cookies */}
              <div className="flex items-start gap-3 p-4 bg-gray-50 rounded-lg">
                <input
                  type="checkbox"
                  checked={consent.analytics}
                  onChange={(e) =>
                    setConsent({ ...consent, analytics: e.target.checked })
                  }
                  className="mt-1"
                />
                <div className="flex-1">
                  <h4 className="font-semibold text-gray-900">Analytics</h4>
                  <p className="text-sm text-gray-600">
                    Help us understand how visitors use our site to improve performance.
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    Examples: Google Analytics (anonymized IP), page views, error tracking
                  </p>
                </div>
              </div>

              {/* Marketing Cookies */}
              <div className="flex items-start gap-3 p-4 bg-gray-50 rounded-lg">
                <input
                  type="checkbox"
                  checked={consent.marketing}
                  onChange={(e) =>
                    setConsent({ ...consent, marketing: e.target.checked })
                  }
                  className="mt-1"
                />
                <div className="flex-1">
                  <h4 className="font-semibold text-gray-900">Marketing</h4>
                  <p className="text-sm text-gray-600">
                    Used to deliver relevant ads and measure campaign effectiveness.
                  </p>
                  <p className="text-xs text-gray-500 mt-1">
                    Examples: LinkedIn Pixel, Google Ads conversion tracking
                  </p>
                </div>
              </div>
            </div>

            <div className="flex gap-3">
              <Button onClick={acceptSelected} size="lg" className="flex-1">
                Save Preferences
              </Button>
              <Button
                onClick={() => setShowSettings(false)}
                variant="outline"
                size="lg"
              >
                Cancel
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

function initializeAnalytics() {
  // Initialize Google Analytics with anonymized IP
  if (typeof window !== 'undefined' && window.gtag) {
    window.gtag('consent', 'update', {
      analytics_storage: 'granted',
    });
  }
}

function initializeMarketing() {
  // Initialize marketing pixels
  if (typeof window !== 'undefined' && window.gtag) {
    window.gtag('consent', 'update', {
      ad_storage: 'granted',
      ad_user_data: 'granted',
      ad_personalization: 'granted',
    });
  }
}
```

##### 1.1.3 Data Subject Rights Implementation

**File**: `/hazardhawk-web/src/app/api/privacy/data-request/route.ts` (NOT CREATED)

```typescript
import { NextRequest, NextResponse } from 'next/server';
import { z } from 'zod';

const dataRequestSchema = z.object({
  requestType: z.enum(['access', 'erasure', 'portability', 'rectification', 'restriction']),
  email: z.string().email(),
  workerId: z.string().optional(),
  description: z.string().min(10).max(500),
});

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const validation = dataRequestSchema.safeParse(body);

    if (!validation.success) {
      return NextResponse.json(
        { error: 'Invalid request data', details: validation.error.issues },
        { status: 400 }
      );
    }

    const { requestType, email, workerId, description } = validation.data;

    // Log the data request for compliance
    await logDataRequest({
      requestType,
      email,
      workerId,
      description,
      timestamp: new Date().toISOString(),
      ipAddress: request.headers.get('x-forwarded-for') || 'unknown',
    });

    // Send notification to DPO
    await notifyDPO({
      requestType,
      email,
      workerId,
      description,
    });

    // Auto-response to user
    await sendConfirmationEmail(email, requestType);

    return NextResponse.json({
      success: true,
      message: `Your ${requestType} request has been received. We will respond within 30 days as required by GDPR.`,
      referenceId: generateRequestId(),
    });
  } catch (error) {
    console.error('Data request error:', error);
    return NextResponse.json(
      { error: 'Failed to process data request' },
      { status: 500 }
    );
  }
}

async function logDataRequest(request: any) {
  // Store in compliance database (separate from operational data)
  // Required for GDPR Article 30 (Records of Processing Activities)
}

async function notifyDPO(request: any) {
  // Email DPO with request details
  // Subject: [URGENT] GDPR Data Subject Request - [requestType]
}

async function sendConfirmationEmail(email: string, requestType: string) {
  // Auto-response to user confirming receipt
  // Include reference ID, expected response time (30 days)
}

function generateRequestId(): string {
  return `DSR-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}
```

### 1.2 CCPA Compliance (California Contractors)

#### Current Status: ⚠️ NOT COMPLIANT

**Required Implementations**:

##### 1.2.1 "Do Not Sell My Personal Information" Link

**File**: `/hazardhawk-web/src/components/layout/footer.tsx`

```typescript
export function Footer() {
  return (
    <footer className="bg-gray-900 text-white py-12">
      <div className="max-w-7xl mx-auto px-4">
        {/* California Residents - CCPA */}
        <div className="border-t border-gray-700 pt-6 mt-6">
          <p className="text-sm text-gray-400">
            California Residents:{' '}
            <a
              href="/ccpa/do-not-sell"
              className="text-yellow-500 underline hover:text-yellow-400"
            >
              Do Not Sell My Personal Information
            </a>
            {' | '}
            <a
              href="/ccpa/data-rights"
              className="text-yellow-500 underline hover:text-yellow-400"
            >
              Your California Privacy Rights
            </a>
          </p>
        </div>

        {/* Privacy Links */}
        <div className="mt-4 text-sm text-gray-400 space-x-4">
          <a href="/privacy" className="hover:text-white">Privacy Policy</a>
          <a href="/terms" className="hover:text-white">Terms of Service</a>
          <a href="/cookie-policy" className="hover:text-white">Cookie Policy</a>
          <a href="/accessibility" className="hover:text-white">Accessibility</a>
        </div>
      </div>
    </footer>
  );
}
```

##### 1.2.2 CCPA Data Rights Page

**File**: `/hazardhawk-web/src/app/ccpa/data-rights/page.tsx` (NOT CREATED)

```typescript
export default function CCPADataRightsPage() {
  return (
    <LegalDocument title="California Privacy Rights (CCPA)">
      <Section id="intro">
        <p>
          If you are a California resident, the California Consumer Privacy Act (CCPA)
          provides you with specific rights regarding your personal information.
        </p>
      </Section>

      <Section id="rights">
        <h2>Your CCPA Rights</h2>
        <ul>
          <li><strong>Right to Know</strong>: Request disclosure of personal information collected</li>
          <li><strong>Right to Delete</strong>: Request deletion of your personal information</li>
          <li><strong>Right to Opt-Out</strong>: Opt-out of the sale of personal information</li>
          <li><strong>Right to Non-Discrimination</strong>: Not be discriminated against for exercising CCPA rights</li>
        </ul>
      </Section>

      <Section id="categories">
        <h2>Categories of Personal Information Collected</h2>
        <table>
          <thead>
            <tr>
              <th>Category</th>
              <th>Examples</th>
              <th>Collected?</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Identifiers</td>
              <td>Name, email, worker ID, SST number</td>
              <td>YES</td>
            </tr>
            <tr>
              <td>Professional Information</td>
              <td>Certifications, training records</td>
              <td>YES</td>
            </tr>
            <tr>
              <td>Internet Activity</td>
              <td>IP address, browser type, pages visited</td>
              <td>YES</td>
            </tr>
            <tr>
              <td>Sensitive Personal Information</td>
              <td>Social Security Number</td>
              <td>NO</td>
            </tr>
          </tbody>
        </table>
      </Section>

      <Section id="sale">
        <h2>Do We Sell Your Personal Information?</h2>
        <p>
          <strong>NO</strong>. HazardHawk does not sell personal information to third parties.
          We share data only with service providers necessary for business operations
          (AWS for storage, Google for OCR processing).
        </p>
      </Section>

      <Section id="exercise-rights">
        <h2>How to Exercise Your Rights</h2>
        <p>To submit a verifiable consumer request:</p>
        <ol>
          <li>Call us toll-free: 1-800-XXX-XXXX</li>
          <li>Email: privacy@hazardhawk.com</li>
          <li>Submit online form: <a href="/ccpa/request-form">Request Form</a></li>
        </ol>
        <p>We will respond within 45 days.</p>
      </Section>

      <Section id="verification">
        <h2>Verification Process</h2>
        <p>
          To protect your privacy, we verify your identity before processing requests:
        </p>
        <ul>
          <li>Email verification (for low-risk requests)</li>
          <li>Worker ID + date of birth (for access/deletion requests)</li>
          <li>Notarized affidavit (for sensitive information requests)</li>
        </ul>
      </Section>
    </LegalDocument>
  );
}
```

### 1.3 Data Minimization & Transparency

#### Recommendations:

1. **Collect Only What's Needed**:
   - Review all form fields in `/src/lib/schemas/certification.ts`
   - Remove optional fields that aren't critical
   - Justify each data point collected

2. **Clear Purpose Statements**:
   ```typescript
   // Add to upload form
   <div className="bg-blue-50 border border-blue-200 p-4 rounded-lg mb-6">
     <h3 className="font-semibold text-blue-900 mb-2">
       Why We Need This Information
     </h3>
     <ul className="text-sm text-blue-800 space-y-1">
       <li>• <strong>Your Name</strong>: To match certifications to your worker profile</li>
       <li>• <strong>Certification Details</strong>: To verify OSHA compliance</li>
       <li>• <strong>Photos/PDFs</strong>: To maintain proof of certification</li>
       <li>• <strong>SST Number</strong>: To verify with NYC DOB (NYC projects only)</li>
     </ul>
   </div>
   ```

3. **Privacy-Friendly Defaults**:
   ```typescript
   // Default to most privacy-preserving options
   const DEFAULT_PRIVACY_SETTINGS = {
     shareWithThirdParties: false,
     includeInAnalytics: false,
     retainAfterEmployment: false, // Prompt for decision
   };
   ```

---

## 2. Form Security

### 2.1 CSRF Protection

#### Current Status: ❌ NOT IMPLEMENTED

**Risk**: Cross-Site Request Forgery attacks could submit fake certifications

**Implementation Required**:

**File**: `/hazardhawk-web/src/lib/security/csrf.ts` (NOT CREATED)

```typescript
import { cookies } from 'next/headers';
import crypto from 'crypto';

export class CSRFProtection {
  private static readonly TOKEN_NAME = 'csrf_token';
  private static readonly HEADER_NAME = 'x-csrf-token';

  /**
   * Generate a new CSRF token
   */
  static generateToken(): string {
    return crypto.randomBytes(32).toString('hex');
  }

  /**
   * Set CSRF token in cookie (server-side)
   */
  static async setToken(): Promise<string> {
    const token = this.generateToken();
    const cookieStore = await cookies();

    cookieStore.set(this.TOKEN_NAME, token, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: 60 * 60, // 1 hour
      path: '/',
    });

    return token;
  }

  /**
   * Verify CSRF token from request
   */
  static async verifyToken(requestToken: string): Promise<boolean> {
    const cookieStore = await cookies();
    const cookieToken = cookieStore.get(this.TOKEN_NAME)?.value;

    if (!cookieToken || !requestToken) {
      return false;
    }

    // Constant-time comparison to prevent timing attacks
    return crypto.timingSafeEqual(
      Buffer.from(cookieToken),
      Buffer.from(requestToken)
    );
  }

  /**
   * Middleware for API routes
   */
  static async middleware(request: Request): Promise<Response | null> {
    // Skip CSRF for GET, HEAD, OPTIONS (safe methods)
    if (['GET', 'HEAD', 'OPTIONS'].includes(request.method)) {
      return null;
    }

    const token = request.headers.get(this.HEADER_NAME);

    if (!token) {
      return new Response(
        JSON.stringify({ error: 'CSRF token missing' }),
        { status: 403, headers: { 'Content-Type': 'application/json' } }
      );
    }

    const isValid = await this.verifyToken(token);

    if (!isValid) {
      return new Response(
        JSON.stringify({ error: 'Invalid CSRF token' }),
        { status: 403, headers: { 'Content-Type': 'application/json' } }
      );
    }

    return null; // Token valid, proceed
  }
}
```

**Usage in API routes**:

```typescript
// /src/app/api/certifications/route.ts
import { CSRFProtection } from '@/lib/security/csrf';

export async function POST(request: Request) {
  // Verify CSRF token
  const csrfError = await CSRFProtection.middleware(request);
  if (csrfError) return csrfError;

  // Process certification submission...
}
```

**Usage in forms**:

```typescript
// /src/components/certifications/upload-wizard.tsx
import { useCSRFToken } from '@/lib/hooks/use-csrf-token';

export function UploadWizard() {
  const csrfToken = useCSRFToken(); // Fetch from cookie or API

  const handleSubmit = async (data) => {
    await fetch('/api/certifications', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-Token': csrfToken, // Include in all mutations
      },
      body: JSON.stringify(data),
    });
  };

  // ...
}
```

### 2.2 Rate Limiting

#### Current Status: ❌ NOT IMPLEMENTED

**Risk**: API abuse, DDoS attacks, spam submissions

**Implementation Required**:

**File**: `/hazardhawk-web/src/lib/security/rate-limiter.ts` (NOT CREATED)

```typescript
import { LRUCache } from 'lru-cache';

interface RateLimitOptions {
  interval: number; // Time window in milliseconds
  maxRequests: number; // Max requests per interval
}

const rateLimiters = new Map<string, LRUCache<string, number>>();

export class RateLimiter {
  private cache: LRUCache<string, number>;
  private maxRequests: number;
  private interval: number;

  constructor(name: string, options: RateLimitOptions) {
    this.maxRequests = options.maxRequests;
    this.interval = options.interval;

    if (!rateLimiters.has(name)) {
      rateLimiters.set(
        name,
        new LRUCache<string, number>({
          max: 10000, // Max number of unique IPs/users to track
          ttl: options.interval,
        })
      );
    }

    this.cache = rateLimiters.get(name)!;
  }

  /**
   * Check if request should be rate limited
   * @param identifier - IP address or user ID
   * @returns true if should be blocked, false if allowed
   */
  check(identifier: string): boolean {
    const now = Date.now();
    const count = this.cache.get(identifier) || 0;

    if (count >= this.maxRequests) {
      return true; // Rate limit exceeded
    }

    this.cache.set(identifier, count + 1);
    return false; // Request allowed
  }

  /**
   * Get remaining requests for identifier
   */
  remaining(identifier: string): number {
    const count = this.cache.get(identifier) || 0;
    return Math.max(0, this.maxRequests - count);
  }

  /**
   * Reset rate limit for identifier
   */
  reset(identifier: string): void {
    this.cache.delete(identifier);
  }
}

// Preset rate limiters
export const uploadRateLimiter = new RateLimiter('upload', {
  interval: 15 * 60 * 1000, // 15 minutes
  maxRequests: 10, // Max 10 uploads per 15 minutes
});

export const apiRateLimiter = new RateLimiter('api', {
  interval: 60 * 1000, // 1 minute
  maxRequests: 60, // Max 60 requests per minute
});

export const ocrRateLimiter = new RateLimiter('ocr', {
  interval: 60 * 60 * 1000, // 1 hour
  maxRequests: 20, // Max 20 OCR requests per hour (expensive operation)
});

export const dobLookupRateLimiter = new RateLimiter('dob', {
  interval: 60 * 60 * 1000, // 1 hour
  maxRequests: 50, // Max 50 DOB lookups per hour
});

/**
 * Middleware helper
 */
export async function checkRateLimit(
  request: Request,
  limiter: RateLimiter
): Promise<Response | null> {
  const identifier =
    request.headers.get('x-forwarded-for') ||
    request.headers.get('x-real-ip') ||
    'unknown';

  if (limiter.check(identifier)) {
    return new Response(
      JSON.stringify({
        error: 'Rate limit exceeded',
        retryAfter: Math.ceil(limiter.interval / 1000),
      }),
      {
        status: 429,
        headers: {
          'Content-Type': 'application/json',
          'Retry-After': String(Math.ceil(limiter.interval / 1000)),
          'X-RateLimit-Remaining': '0',
        },
      }
    );
  }

  return null; // Not rate limited
}
```

**Usage**:

```typescript
// /src/app/api/certifications/route.ts
import { checkRateLimit, uploadRateLimiter } from '@/lib/security/rate-limiter';

export async function POST(request: Request) {
  // Check rate limit
  const rateLimitError = await checkRateLimit(request, uploadRateLimiter);
  if (rateLimitError) return rateLimitError;

  // Process upload...
}
```

### 2.3 Input Validation & Sanitization

#### Current Status: ✅ PARTIALLY IMPLEMENTED

**Existing**: Zod schemas in `/src/lib/schemas/certification.ts`

**Enhancements Needed**:

```typescript
// Enhanced validation with security focus
export const certificationFormSchema = z.object({
  workerId: z.string()
    .min(1, 'Worker ID is required')
    .regex(/^[A-Z0-9-]+$/i, 'Invalid worker ID format')
    .transform(val => val.trim().toUpperCase()), // Normalize

  holderName: z.string()
    .min(2, 'Name must be at least 2 characters')
    .max(100, 'Name must be less than 100 characters')
    .regex(/^[a-zA-Z\s'-]+$/, 'Name contains invalid characters')
    .transform(val => {
      // Sanitize: Remove multiple spaces, trim, title case
      return val.trim().replace(/\s+/g, ' ').split(' ')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
    }),

  certificationNumber: z.string()
    .min(3, 'Certification number must be at least 3 characters')
    .max(50, 'Certification number must be less than 50 characters')
    .regex(/^[A-Z0-9-]+$/i, 'Invalid certification number')
    .transform(val => val.trim().toUpperCase().replace(/\s+/g, '')),

  // Strict date validation to prevent injection
  expirationDate: z.string()
    .regex(/^\d{4}-\d{2}-\d{2}$/, 'Date must be YYYY-MM-DD format')
    .refine(val => {
      const date = new Date(val);
      return !isNaN(date.getTime()); // Valid date
    }, 'Invalid date')
    .refine(val => {
      const date = new Date(val);
      const minDate = new Date('1900-01-01');
      const maxDate = new Date('2100-12-31');
      return date >= minDate && date <= maxDate;
    }, 'Date out of acceptable range'),

  // Prevent script injection in optional fields
  notes: z.string()
    .max(500, 'Notes must be less than 500 characters')
    .optional()
    .transform(val => {
      if (!val) return val;
      // Strip HTML tags, encode special characters
      return val.replace(/<[^>]*>/g, '').trim();
    }),
});
```

### 2.4 Email Validation (Anti-Spam)

**Implementation**:

```typescript
// /src/lib/security/email-validator.ts
import dns from 'dns/promises';

export async function validateEmail(email: string): Promise<{
  valid: boolean;
  reason?: string;
}> {
  // Basic format check
  const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
  if (!emailRegex.test(email)) {
    return { valid: false, reason: 'Invalid email format' };
  }

  // Extract domain
  const domain = email.split('@')[1];

  // Check for disposable email domains
  const disposableDomains = [
    'tempmail.com', 'guerrillamail.com', '10minutemail.com',
    'throwaway.email', 'mailinator.com', // ... add more
  ];
  if (disposableDomains.includes(domain.toLowerCase())) {
    return { valid: false, reason: 'Disposable email addresses not allowed' };
  }

  // Check MX records (domain has mail server)
  try {
    const mxRecords = await dns.resolveMx(domain);
    if (mxRecords.length === 0) {
      return { valid: false, reason: 'Domain has no mail server' };
    }
  } catch (error) {
    return { valid: false, reason: 'Domain does not exist' };
  }

  return { valid: true };
}
```

### 2.5 Spam Prevention (reCAPTCHA Alternative)

**Recommendation**: Use **Cloudflare Turnstile** (GDPR-friendly, no Google tracking)

**File**: `/hazardhawk-web/src/components/security/turnstile.tsx` (NOT CREATED)

```typescript
'use client';

import { useEffect, useRef } from 'react';

interface TurnstileProps {
  siteKey: string;
  onVerify: (token: string) => void;
  onError?: () => void;
}

export function Turnstile({ siteKey, onVerify, onError }: TurnstileProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!containerRef.current) return;

    // Load Turnstile script
    const script = document.createElement('script');
    script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js';
    script.async = true;
    document.body.appendChild(script);

    script.onload = () => {
      if (window.turnstile && containerRef.current) {
        window.turnstile.render(containerRef.current, {
          sitekey: siteKey,
          callback: onVerify,
          'error-callback': onError,
          theme: 'light',
          size: 'normal',
        });
      }
    };

    return () => {
      document.body.removeChild(script);
    };
  }, [siteKey, onVerify, onError]);

  return <div ref={containerRef} className="cf-turnstile" />;
}
```

**Usage in forms**:

```typescript
// Require Turnstile verification before form submission
const [turnstileToken, setTurnstileToken] = useState<string | null>(null);

const handleSubmit = async (data) => {
  if (!turnstileToken) {
    alert('Please complete the verification challenge');
    return;
  }

  await fetch('/api/certifications', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Turnstile-Token': turnstileToken,
    },
    body: JSON.stringify(data),
  });
};

// In form JSX
<Turnstile
  siteKey={process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY}
  onVerify={setTurnstileToken}
/>
```

---

## 3. Infrastructure Security

### 3.1 HTTPS Enforcement

#### Current Status: ✅ IMPLEMENTED (Next.js default)

**Verification**:
- Next.js automatically redirects HTTP to HTTPS in production
- Vercel/AWS Amplify provide SSL certificates automatically

**Additional Headers**:

**File**: `/hazardhawk-web/next.config.js` (NEEDS UPDATE)

```javascript
/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,

  // Environment variables
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080',
    NEXT_PUBLIC_APP_URL: process.env.NEXT_PUBLIC_APP_URL || 'http://localhost:3000',
  },

  // Image domains
  images: {
    domains: [
      'hazardhawk-certifications.s3.amazonaws.com',
      'dob-trainingconnect.cityofnewyork.us',
    ],
  },

  // Security Headers
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          // HTTPS Strict Transport Security
          {
            key: 'Strict-Transport-Security',
            value: 'max-age=63072000; includeSubDomains; preload',
          },
          // Prevent clickjacking
          {
            key: 'X-Frame-Options',
            value: 'DENY',
          },
          // Prevent MIME sniffing
          {
            key: 'X-Content-Type-Options',
            value: 'nosniff',
          },
          // Referrer policy
          {
            key: 'Referrer-Policy',
            value: 'strict-origin-when-cross-origin',
          },
          // Permissions policy
          {
            key: 'Permissions-Policy',
            value: 'camera=(self), microphone=(), geolocation=(), interest-cohort=()',
          },
        ],
      },
    ];
  },
}

module.exports = nextConfig;
```

### 3.2 Content Security Policy (CSP)

#### Current Status: ❌ NOT IMPLEMENTED

**File**: `/hazardhawk-web/src/middleware.ts` (NOT CREATED)

```typescript
import { NextRequest, NextResponse } from 'next/server';

export function middleware(request: NextRequest) {
  const nonce = Buffer.from(crypto.randomUUID()).toString('base64');

  const cspHeader = `
    default-src 'self';
    script-src 'self' 'nonce-${nonce}' 'strict-dynamic' https://challenges.cloudflare.com;
    style-src 'self' 'unsafe-inline';
    img-src 'self' blob: data: https://hazardhawk-certifications.s3.amazonaws.com;
    font-src 'self';
    object-src 'none';
    base-uri 'self';
    form-action 'self';
    frame-ancestors 'none';
    upgrade-insecure-requests;
    connect-src 'self' https://*.hazardhawk.com ${process.env.NEXT_PUBLIC_API_URL};
  `.replace(/\s{2,}/g, ' ').trim();

  const response = NextResponse.next();
  response.headers.set('Content-Security-Policy', cspHeader);
  response.headers.set('X-Nonce', nonce);

  return response;
}

export const config = {
  matcher: [
    /*
     * Match all request paths except:
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     * - public folder
     */
    '/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|jpg|jpeg|gif|webp)$).*)',
  ],
};
```

### 3.3 DDoS Protection Recommendations

**Cloudflare Integration** (Recommended):

1. **Setup**:
   - Add HazardHawk web domain to Cloudflare
   - Enable "Under Attack Mode" for emergency DDoS
   - Configure rate limiting rules:
     - `/api/*`: 100 requests/min per IP
     - `/upload/*`: 20 requests/min per IP
     - `/api/ocr/*`: 10 requests/min per IP

2. **Bot Management**:
   - Enable Cloudflare Bot Management
   - Challenge suspicious traffic
   - Block known bad bots

3. **Firewall Rules**:
   ```
   # Block countries with no construction workers (if applicable)
   (ip.geoip.country ne "US" and ip.geoip.country ne "CA")

   # Block automated tools
   (http.user_agent contains "curl" or http.user_agent contains "wget")

   # Require valid referer for uploads
   (http.request.uri.path contains "/upload" and not http.referer contains "hazardhawk.com")
   ```

### 3.4 CDN Security

**AWS CloudFront Configuration**:

```yaml
# CloudFront Distribution Settings
PriceClass: PriceClass_100 # US, Canada, Europe
ViewerProtocolPolicy: redirect-to-https
AllowedMethods:
  - GET
  - HEAD
  - OPTIONS
  - PUT
  - POST
  - PATCH
  - DELETE
CachedMethods:
  - GET
  - HEAD
  - OPTIONS
Compress: true
WAFWebACLId: [WAF ACL ID] # Enable AWS WAF

# Geo Restrictions (if applicable)
GeoRestriction:
  RestrictionType: whitelist
  Locations:
    - US
    - CA
    - MX # North America only

# Custom Headers
CustomHeaders:
  - HeaderName: X-Content-Type-Options
    HeaderValue: nosniff
  - HeaderName: X-Frame-Options
    HeaderValue: DENY
  - HeaderName: Strict-Transport-Security
    HeaderValue: max-age=63072000
```

---

## 4. Third-Party Integrations

### 4.1 Analytics Tools (Privacy-Friendly Options)

#### Current Status: ⚠️ NOT IMPLEMENTED

**Recommendations**:

**Option 1: Plausible Analytics** (GDPR-compliant, no cookies)

```typescript
// /src/app/layout.tsx
export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <head>
        {/* Plausible Analytics - Privacy-friendly */}
        <script
          defer
          data-domain="hazardhawk.com"
          src="https://plausible.io/js/script.js"
        ></script>
      </head>
      <body>{children}</body>
    </html>
  );
}
```

**Features**:
- No cookies, no tracking
- GDPR/CCPA compliant by default
- Open-source
- Lightweight (< 1 kB)
- No cookie consent needed

**Option 2: Google Analytics 4 (with privacy settings)**

```typescript
// Only if user consents to analytics cookies
import Script from 'next/script';

export function GoogleAnalytics({ consentGiven }: { consentGiven: boolean }) {
  if (!consentGiven) return null;

  return (
    <>
      <Script
        src={`https://www.googletagmanager.com/gtag/js?id=${process.env.NEXT_PUBLIC_GA_ID}`}
        strategy="afterInteractive"
      />
      <Script id="google-analytics" strategy="afterInteractive">
        {`
          window.dataLayer = window.dataLayer || [];
          function gtag(){dataLayer.push(arguments);}
          gtag('js', new Date());

          // Privacy-friendly settings
          gtag('config', '${process.env.NEXT_PUBLIC_GA_ID}', {
            anonymize_ip: true, // Anonymize IP addresses
            allow_google_signals: false, // Disable advertising features
            allow_ad_personalization_signals: false,
            cookie_flags: 'SameSite=None;Secure', // Secure cookies
          });
        `}
      </Script>
    </>
  );
}
```

### 4.2 Marketing Automation Platforms

**Recommendation**: **HubSpot** (construction industry-focused)

**Privacy Considerations**:

1. **Data Processing Agreement**: Sign DPA with HubSpot
2. **Cookie Consent**: Require opt-in for tracking cookies
3. **Data Retention**: Configure to match privacy policy (13 months)
4. **Unsubscribe**: One-click unsubscribe in all emails

**Implementation**:

```typescript
// /src/lib/integrations/hubspot.ts
export async function trackContact(email: string, properties: object) {
  // Only if user consented to marketing cookies
  const consent = getCookieConsent();
  if (!consent.marketing) return;

  await fetch('https://api.hubapi.com/crm/v3/objects/contacts', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${process.env.HUBSPOT_API_KEY}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      properties: {
        email,
        ...properties,
        gdpr_consent: true,
        consent_timestamp: new Date().toISOString(),
      },
    }),
  });
}
```

### 4.3 CRM Integrations

**Options**:
- **Salesforce** (Enterprise)
- **HubSpot CRM** (SMB)
- **Zoho CRM** (Budget-friendly)

**Security Requirements**:

1. **OAuth 2.0**: Use OAuth instead of API keys
2. **Field-Level Encryption**: Encrypt SSN, certifications in CRM
3. **Role-Based Access**: Restrict who can view certification data
4. **Audit Logging**: Log all access to worker data
5. **Data Retention**: Sync with privacy policy retention periods

### 4.4 Payment Processors (If Applicable)

**Status**: Not currently applicable (no e-commerce)

**If Added in Future**:
- Use **Stripe** (PCI DSS Level 1 compliant)
- Never store credit card numbers
- Use Stripe Checkout (hosted payment page)
- Implement SCA (Strong Customer Authentication) for EU

### 4.5 Security Implications Summary

| Integration | Data Shared | DPA Required | Cookie Consent Required | Risk Level |
|-------------|-------------|--------------|-------------------------|------------|
| AWS S3 | Certification images | ✅ Yes | ❌ No (operational) | LOW |
| Google Document AI | Document images, OCR text | ✅ Yes | ❌ No (operational) | MEDIUM |
| NYC DOB API | SST numbers, names | ❌ No (gov) | ❌ No (operational) | LOW |
| Plausible Analytics | Pageviews (no PII) | ❌ No | ❌ No | LOW |
| Google Analytics | Pageviews, IP (anonymized) | ✅ Yes | ✅ Yes | MEDIUM |
| HubSpot | Email, company, usage | ✅ Yes | ✅ Yes | MEDIUM |
| Cloudflare | Traffic data | ✅ Yes | ❌ No (security) | LOW |

---

## 5. Construction Industry Compliance

### 5.1 OSHA Data Handling

#### Current Status: ⚠️ NEEDS REVIEW

**Considerations**:

1. **OSHA 300 Logs**: If app displays real incident data
   - Must protect worker privacy (29 CFR 1904.29(b)(7))
   - Remove names from publicly displayed data
   - Only aggregate statistics for marketing

2. **Certification Authenticity**:
   - Never display fake/demo certifications on marketing site
   - Use blurred or watermarked examples only
   - Include disclaimer: "Example for demonstration purposes"

3. **Safety Claims**:
   - Avoid absolute claims ("100% safe", "prevents all incidents")
   - Use qualified language ("helps reduce", "improves safety")
   - Include disclaimers about app limitations

**Recommended Disclaimer** (for marketing site):

```html
<!-- Footer disclaimer -->
<div class="bg-gray-100 text-xs text-gray-600 p-4 text-center">
  <p>
    HazardHawk is a safety management tool designed to assist in OSHA compliance.
    It does not replace professional safety training, site-specific safety plans,
    or legal compliance requirements. Always consult with qualified safety professionals.
  </p>
  <p class="mt-2">
    All certifications displayed are examples for demonstration purposes only.
  </p>
</div>
```

### 5.2 Construction Project Confidentiality

**Risk**: Exposing project details via uploaded certifications

**Mitigations**:

1. **Redact Project Info**:
   - Strip EXIF metadata from uploaded images (location, timestamp)
   - Remove project names from OCR results before public display
   - Never show project addresses on marketing site

2. **NDA Compliance**:
   - Privacy policy should cover confidentiality
   - Data Processing Agreements with all processors
   - Require contractor consent before using as case study

3. **Testimonial Permissions**:
   ```typescript
   // Before displaying testimonials
   interface Testimonial {
     quote: string;
     author: string;
     company?: string; // Optional, with written permission only
     projectType: string; // Generic: "High-rise", not "123 Main St"
     permissionGranted: boolean;
     signedReleaseDate: string;
   }
   ```

### 5.3 Insurance/Liability Considerations

**Risk**: Claims that app "guarantees" safety compliance

**Recommendations**:

1. **Terms of Service** (Liability Limitation):

```markdown
## Limitation of Liability

HazardHawk provides software tools to assist with safety management but does not:
- Guarantee OSHA compliance
- Prevent workplace incidents
- Replace professional safety expertise
- Substitute for required training or certifications
- Ensure accuracy of user-submitted data

Users are solely responsible for:
- Verifying authenticity of uploaded certifications
- Maintaining OSHA compliance
- Implementing site-specific safety plans
- Training workers on proper safety procedures

TO THE MAXIMUM EXTENT PERMITTED BY LAW, HAZARDHAWK SHALL NOT BE LIABLE FOR
ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES ARISING
FROM USE OF THE SERVICE, INCLUDING BUT NOT LIMITED TO WORKPLACE INJURIES,
REGULATORY FINES, OR PROJECT DELAYS.
```

2. **Professional Liability Insurance**:
   - Obtain Errors & Omissions (E&O) insurance
   - Cyber liability insurance (data breach coverage)
   - Minimum $2M coverage recommended

3. **Certification Disclaimers**:
   - Display "User-Submitted" badge on certifications
   - Remind admins to verify authenticity
   - Log all verification actions for audit trail

---

## 6. Vulnerability Prevention

### 6.1 XSS Protection

#### Current Status: ✅ MOSTLY PROTECTED (React escaping)

**React Default Protection**:
- React automatically escapes all rendered text
- Prevents XSS in most cases

**Vulnerabilities to Watch**:

1. **Dangerous HTML Rendering**:
```typescript
// VULNERABLE - Never do this
<div dangerouslySetInnerHTML={{ __html: userInput }} />

// SAFE - Use React components
<div>{userInput}</div>
```

2. **URL Injection**:
```typescript
// VULNERABLE
<a href={userProvidedUrl}>Link</a>

// SAFE - Validate URL
function SafeLink({ href, children }: { href: string; children: React.ReactNode }) {
  const safeUrl = useMemo(() => {
    try {
      const url = new URL(href);
      if (!['http:', 'https:'].includes(url.protocol)) {
        return '#'; // Block javascript:, data:, etc.
      }
      return href;
    } catch {
      return '#';
    }
  }, [href]);

  return <a href={safeUrl} rel="noopener noreferrer">{children}</a>;
}
```

3. **SVG Injection**:
```typescript
// VULNERABLE - SVG can contain scripts
<img src={userUploadedSVG} />

// SAFE - Validate file type, sanitize SVG
async function sanitizeSVG(file: File): Promise<string> {
  const text = await file.text();
  // Use DOMPurify or similar to strip scripts
  const clean = DOMPurify.sanitize(text, {
    USE_PROFILES: { svg: true, svgFilters: true },
  });
  return clean;
}
```

### 6.2 SQL Injection

#### Current Status: ✅ NOT APPLICABLE (Frontend only)

**Backend Responsibility**:
- Backend must use parameterized queries
- Never concatenate user input into SQL
- Use ORM (Sequelize, Prisma, etc.)

**Frontend Responsibility**:
- Validate all input before sending to API
- Zod schemas prevent malformed data
- Don't trust client-side validation alone

### 6.3 Dependency Vulnerability Scanning

#### Current Status: ⚠️ NOT AUTOMATED

**Implementation**:

**File**: `/.github/workflows/security-scan.yml` (NOT CREATED)

```yaml
name: Security Scan

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  schedule:
    # Run weekly on Mondays at 9 AM
    - cron: '0 9 * * 1'

jobs:
  dependency-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Node.js
        uses: actions/setup-node@v3
        with:
          node-version: '18'

      - name: Install dependencies
        run: cd hazardhawk-web && npm ci

      - name: Run npm audit
        run: cd hazardhawk-web && npm audit --audit-level=moderate

      - name: Run Snyk security scan
        uses: snyk/actions/node@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
        with:
          args: --severity-threshold=high

  code-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Run CodeQL analysis
        uses: github/codeql-action/init@v2
        with:
          languages: javascript, typescript

      - name: Autobuild
        uses: github/codeql-action/autobuild@v2

      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v2
```

**Package Updates**:

```json
// package.json - Add scripts
{
  "scripts": {
    "audit": "npm audit --audit-level=moderate",
    "audit:fix": "npm audit fix",
    "outdated": "npm outdated",
    "update:deps": "npm update && npm audit fix"
  }
}
```

**Dependabot Configuration**:

**File**: `/.github/dependabot.yml` (NOT CREATED)

```yaml
version: 2
updates:
  - package-ecosystem: "npm"
    directory: "/hazardhawk-web"
    schedule:
      interval: "weekly"
      day: "monday"
      time: "09:00"
    open-pull-requests-limit: 10
    reviewers:
      - "security-team"
    labels:
      - "dependencies"
      - "security"
    # Auto-merge patch updates
    automerge: true
    versioning-strategy: increase
```

### 6.4 Secret Management

#### Current Status: ⚠️ ENVIRONMENT VARIABLES ONLY

**Current Implementation**:
- `.env.local` for development
- Vercel environment variables for production

**Enhancements Needed**:

1. **Never Commit Secrets**:

**File**: `/.gitignore` (VERIFY)

```gitignore
# Environment variables
.env
.env.local
.env.*.local
.env.production

# AWS credentials
.aws/

# Google Cloud credentials
gcloud-credentials.json
service-account-*.json

# API keys
*-api-key.txt
secrets.yml
```

2. **Secrets Rotation**:

```typescript
// /src/lib/security/secrets-manager.ts
import { SecretsManagerClient, GetSecretValueCommand } from '@aws-sdk/client-secrets-manager';

const client = new SecretsManagerClient({ region: 'us-east-1' });

export async function getSecret(secretName: string): Promise<string> {
  const command = new GetSecretValueCommand({ SecretId: secretName });
  const response = await client.send(command);
  return response.SecretString || '';
}

// Usage
const dbPassword = await getSecret('hazardhawk/prod/db-password');
const apiKey = await getSecret('hazardhawk/prod/google-document-ai-key');
```

3. **Environment Variable Validation**:

**File**: `/hazardhawk-web/src/lib/config/env.ts` (NOT CREATED)

```typescript
import { z } from 'zod';

const envSchema = z.object({
  // Public variables
  NEXT_PUBLIC_API_URL: z.string().url(),
  NEXT_PUBLIC_APP_URL: z.string().url(),
  NEXT_PUBLIC_S3_BUCKET: z.string().min(1),
  NEXT_PUBLIC_S3_REGION: z.string().min(1),
  NEXT_PUBLIC_DOB_API_URL: z.string().url().optional(),

  // Server-only variables (never exposed to client)
  DOB_API_KEY: z.string().min(1).optional(),
  GOOGLE_DOCUMENT_AI_PROJECT_ID: z.string().min(1).optional(),
  GOOGLE_DOCUMENT_AI_PROCESSOR_ID: z.string().min(1).optional(),
  AWS_ACCESS_KEY_ID: z.string().min(1).optional(),
  AWS_SECRET_ACCESS_KEY: z.string().min(1).optional(),

  // Node environment
  NODE_ENV: z.enum(['development', 'production', 'test']),
});

export const env = envSchema.parse(process.env);

// Type-safe environment variables
export type Env = z.infer<typeof envSchema>;
```

**Usage**:

```typescript
import { env } from '@/lib/config/env';

// Type-safe, validated at startup
console.log(env.NEXT_PUBLIC_API_URL); // ✅ Validated URL
console.log(env.RANDOM_VAR); // ❌ TypeScript error
```

4. **Prevent Secret Leakage in Logs**:

```typescript
// /src/lib/utils/logger.ts
export function sanitizeLog(data: any): any {
  const secretKeys = [
    'password', 'token', 'apiKey', 'secret', 'authorization',
    'ssn', 'creditCard', 'cvv',
  ];

  if (typeof data === 'string') {
    secretKeys.forEach(key => {
      const regex = new RegExp(`${key}[=:]\\s*[^\\s,}]+`, 'gi');
      data = data.replace(regex, `${key}=***REDACTED***`);
    });
    return data;
  }

  if (typeof data === 'object' && data !== null) {
    const sanitized = { ...data };
    Object.keys(sanitized).forEach(key => {
      if (secretKeys.some(secret => key.toLowerCase().includes(secret))) {
        sanitized[key] = '***REDACTED***';
      } else if (typeof sanitized[key] === 'object') {
        sanitized[key] = sanitizeLog(sanitized[key]);
      }
    });
    return sanitized;
  }

  return data;
}

// Usage
console.log(sanitizeLog({ apiKey: 'sk_live_1234', name: 'John' }));
// Output: { apiKey: '***REDACTED***', name: 'John' }
```

---

## 7. Ongoing Security Monitoring

### 7.1 Error Tracking & Monitoring

**Recommendation**: **Sentry** (Error tracking + Performance monitoring)

**Setup**:

```bash
cd hazardhawk-web
npm install @sentry/nextjs
npx @sentry/wizard -i nextjs
```

**Configuration**:

**File**: `/hazardhawk-web/sentry.client.config.js`

```javascript
import * as Sentry from '@sentry/nextjs';

Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
  environment: process.env.NODE_ENV,

  // Tracing
  tracesSampleRate: 0.1, // 10% of transactions

  // Session Replay (privacy-friendly)
  replaysSessionSampleRate: 0.01, // 1% of sessions
  replaysOnErrorSampleRate: 1.0, // 100% of errors

  // Privacy settings
  beforeSend(event, hint) {
    // Scrub sensitive data
    if (event.request) {
      delete event.request.cookies;
      delete event.request.headers?.Authorization;
    }

    // Remove PII from breadcrumbs
    if (event.breadcrumbs) {
      event.breadcrumbs = event.breadcrumbs.map(crumb => {
        if (crumb.data) {
          delete crumb.data.ssn;
          delete crumb.data.certificationNumber;
        }
        return crumb;
      });
    }

    return event;
  },

  // Ignore non-critical errors
  ignoreErrors: [
    'ResizeObserver loop limit exceeded',
    'Non-Error promise rejection captured',
  ],
});
```

### 7.2 Security Headers Monitoring

**Tool**: **Mozilla Observatory** (Free security scanner)

**Check**: https://observatory.mozilla.org/analyze/hazardhawk.com

**Target Score**: A+ (90+)

**Required Headers**:
- ✅ Content-Security-Policy
- ✅ Strict-Transport-Security
- ✅ X-Frame-Options
- ✅ X-Content-Type-Options
- ✅ Referrer-Policy

### 7.3 Uptime & Performance Monitoring

**Recommendation**: **UptimeRobot** (Free tier: 50 monitors)

**Monitors**:
1. Website uptime (https://hazardhawk.com) - Check every 5 minutes
2. Upload API (https://api.hazardhawk.com/health) - Check every 5 minutes
3. SSL certificate expiry - Alert 30 days before

**Alerts**:
- Email: ops@hazardhawk.com
- SMS: Critical alerts only
- Slack: #alerts channel

### 7.4 Compliance Auditing

**Schedule**:

| Activity | Frequency | Responsible |
|----------|-----------|-------------|
| Privacy Policy Review | Quarterly | Legal + DPO |
| Dependency Security Scan | Weekly (automated) | DevOps |
| Penetration Testing | Annually | External firm |
| GDPR Compliance Audit | Annually | DPO |
| Access Control Review | Quarterly | Security team |
| Data Retention Enforcement | Monthly (automated) | Backend |
| Incident Response Drill | Semi-annually | All teams |

**Audit Checklist**:

```markdown
## Quarterly Security Audit

### Access Control
- [ ] Review admin users (remove inactive)
- [ ] Verify MFA enabled for all admins
- [ ] Check API key rotation (90-day max)
- [ ] Review AWS IAM policies

### Data Protection
- [ ] Verify encryption at rest (S3)
- [ ] Verify encryption in transit (HTTPS)
- [ ] Check data retention policies enforced
- [ ] Review data processing agreements (DPAs)

### Vulnerability Management
- [ ] Run npm audit (no high/critical)
- [ ] Check Snyk scan results
- [ ] Review Dependabot PRs (merge pending)
- [ ] Update dependencies (patch versions)

### Compliance
- [ ] Privacy policy up to date
- [ ] Cookie consent functioning
- [ ] GDPR data requests processed (30-day SLA)
- [ ] CCPA opt-out links working
- [ ] Data breach procedures tested

### Monitoring
- [ ] Review Sentry error rates
- [ ] Check uptime reports (99.9%+ target)
- [ ] Verify backup integrity
- [ ] Test incident response plan

### Documentation
- [ ] Update security assessment (this document)
- [ ] Document new threats/vulnerabilities
- [ ] Update incident response runbook
- [ ] Review employee security training
```

---

## 8. Incident Response Plan

### 8.1 Data Breach Response

**Trigger**: Unauthorized access to certification data, user accounts, or PII

**Response Timeline**:

| Time | Action | Responsible |
|------|--------|-------------|
| T+0 | Detect breach (monitoring alerts) | Security team |
| T+1h | Confirm breach, assess scope | Security lead |
| T+4h | Contain breach (revoke access, rotate keys) | DevOps |
| T+8h | Notify executive team | CTO |
| T+24h | Notify DPO, legal counsel | CTO |
| T+48h | Prepare user notification | Legal + Marketing |
| T+72h | Notify supervisory authority (GDPR) | DPO |
| T+7d | Public disclosure (if required) | Legal |

**Notification Template**:

```markdown
Subject: Important Security Notice - HazardHawk Data Incident

Dear [Worker Name],

We are writing to inform you of a security incident that may have affected your personal information.

**What Happened:**
On [Date], we discovered unauthorized access to our certification database. We immediately took steps to contain the incident and began an investigation.

**What Information Was Involved:**
The following information may have been accessed:
- Name
- Worker ID
- Certification types and numbers
- Certification expiration dates
- [Other data as applicable]

**What We Are Doing:**
- We have secured the affected system
- We are working with cybersecurity experts to investigate
- We have notified law enforcement and regulatory authorities
- We have implemented additional security measures

**What You Can Do:**
- Monitor your accounts for suspicious activity
- Consider placing a fraud alert on your credit file (if SSN exposed)
- Report any suspicious activity to us immediately

**For More Information:**
Contact our Data Protection Officer at privacy@hazardhawk.com or call 1-800-XXX-XXXX.

We sincerely apologize for this incident and any inconvenience it may cause.

Sincerely,
HazardHawk Security Team
```

### 8.2 DDoS Attack Response

1. **Enable Cloudflare "Under Attack Mode"**
2. **Implement emergency rate limiting**
3. **Scale infrastructure (auto-scaling)**
4. **Analyze attack pattern (block IPs/countries)**
5. **Communicate with users (status page)**

### 8.3 Vulnerability Disclosure

**Contact**: security@hazardhawk.com

**Process**:
1. Researcher reports vulnerability
2. Acknowledge within 24 hours
3. Validate and assess severity
4. Fix critical issues within 7 days
5. Credit researcher (if desired)

---

## 9. Implementation Roadmap

### Priority 1: CRITICAL (Before Production)

| Task | Effort | Status |
|------|--------|--------|
| Implement CSRF protection | 4 hours | ⚠️ TODO |
| Add rate limiting | 6 hours | ⚠️ TODO |
| Configure CSP headers | 3 hours | ⚠️ TODO |
| Add cookie consent banner | 4 hours | ⚠️ TODO |
| Create privacy policy | 8 hours | ⚠️ TODO |
| Implement authentication | 16 hours | ⚠️ TODO |
| Add error tracking (Sentry) | 2 hours | ⚠️ TODO |
| Security headers (HSTS, etc.) | 2 hours | ⚠️ TODO |

**Total**: ~45 hours (1 week sprint)

### Priority 2: HIGH (First Month)

| Task | Effort | Status |
|------|--------|--------|
| GDPR data subject rights API | 12 hours | ⚠️ TODO |
| CCPA compliance pages | 6 hours | ⚠️ TODO |
| Cloudflare Turnstile integration | 4 hours | ⚠️ TODO |
| Dependency scanning automation | 4 hours | ⚠️ TODO |
| Secrets management (AWS Secrets Manager) | 8 hours | ⚠️ TODO |
| Audit logging system | 12 hours | ⚠️ TODO |
| Incident response procedures | 6 hours | ⚠️ TODO |

**Total**: ~52 hours (1 week sprint)

### Priority 3: MEDIUM (First Quarter)

| Task | Effort | Status |
|------|--------|--------|
| Analytics integration (Plausible) | 3 hours | ⚠️ TODO |
| Marketing automation (HubSpot) | 8 hours | ⚠️ TODO |
| Cloudflare CDN setup | 6 hours | ⚠️ TODO |
| Penetration testing | 40 hours | ⚠️ TODO |
| Compliance documentation | 16 hours | ⚠️ TODO |
| Security training for team | 8 hours | ⚠️ TODO |

**Total**: ~81 hours (2 week sprint)

---

## 10. Compliance Checklist

### Pre-Launch Checklist

#### Legal
- [ ] Privacy policy published
- [ ] Terms of service published
- [ ] Cookie policy published
- [ ] CCPA opt-out page created
- [ ] GDPR data request form created
- [ ] Data Processing Agreements signed (AWS, Google)
- [ ] Professional liability insurance obtained

#### Security
- [ ] HTTPS enforced (HSTS)
- [ ] Security headers configured (CSP, X-Frame-Options, etc.)
- [ ] CSRF protection implemented
- [ ] Rate limiting enabled
- [ ] Input validation on all forms (Zod)
- [ ] File upload restrictions enforced
- [ ] XSS protection verified
- [ ] SQL injection protection (backend)
- [ ] Authentication system implemented
- [ ] MFA enabled for admins
- [ ] Secrets stored in AWS Secrets Manager
- [ ] Error tracking configured (Sentry)
- [ ] Uptime monitoring configured

#### Privacy
- [ ] Cookie consent banner displayed
- [ ] Analytics opt-in only (no auto-tracking)
- [ ] Data minimization verified
- [ ] Retention policies documented
- [ ] Data subject rights workflow tested
- [ ] Breach notification procedures documented
- [ ] DPO contact information published

#### Infrastructure
- [ ] Cloudflare DDoS protection enabled
- [ ] CDN configured (CloudFront or Cloudflare)
- [ ] WAF rules configured
- [ ] Backups automated (daily)
- [ ] Disaster recovery plan tested
- [ ] Auto-scaling configured
- [ ] Monitoring alerts configured

#### Compliance
- [ ] GDPR compliance verified
- [ ] CCPA compliance verified
- [ ] OSHA data handling reviewed
- [ ] Construction industry disclaimers added
- [ ] Testimonial permissions documented
- [ ] Incident response plan documented
- [ ] Security audit completed
- [ ] Penetration test completed

---

## 11. Resources

### Documentation
- **GDPR Full Text**: https://gdpr-info.eu/
- **CCPA Law**: https://oag.ca.gov/privacy/ccpa
- **OSHA Regulations**: https://www.osha.gov/laws-regs
- **Next.js Security**: https://nextjs.org/docs/advanced-features/security-headers
- **OWASP Top 10**: https://owasp.org/www-project-top-ten/

### Tools
- **Cloudflare**: https://www.cloudflare.com/
- **Sentry**: https://sentry.io/
- **Snyk**: https://snyk.io/
- **Mozilla Observatory**: https://observatory.mozilla.org/
- **SSL Labs**: https://www.ssllabs.com/ssltest/
- **Plausible Analytics**: https://plausible.io/
- **Cloudflare Turnstile**: https://www.cloudflare.com/products/turnstile/

### Legal
- **GDPR DPO Directory**: https://edpb.europa.eu/
- **CCPA Resources**: https://oag.ca.gov/privacy/ccpa
- **IAPP**: https://iapp.org/ (Privacy professional organization)

---

## 12. Conclusion

The HazardHawk web certification portal has a **solid foundation** with basic security measures (input validation, HTTPS, file upload restrictions), but requires **significant enhancements** before production deployment.

### Key Risks Identified

1. **CRITICAL**: No CSRF protection
2. **CRITICAL**: No authentication system
3. **HIGH**: No rate limiting (DDoS vulnerability)
4. **HIGH**: Missing privacy compliance (GDPR/CCPA)
5. **MEDIUM**: No Content Security Policy
6. **MEDIUM**: No cookie consent mechanism

### Recommended Next Steps

1. **Week 1**: Implement Priority 1 tasks (CSRF, rate limiting, CSP, auth)
2. **Week 2-3**: Add privacy compliance (cookie consent, privacy policy, GDPR/CCPA)
3. **Week 4**: Security testing and monitoring setup
4. **Month 2**: Marketing integrations and analytics (with consent)
5. **Ongoing**: Quarterly audits, dependency updates, penetration testing

### Success Metrics

- **Security Score**: A+ on Mozilla Observatory
- **Uptime**: 99.9%+ availability
- **GDPR Compliance**: All data requests processed within 30 days
- **Vulnerability Management**: Zero high/critical npm audit issues
- **Incident Response**: < 24 hour detection-to-containment time

---

**Document Owner**: Security & Compliance Team
**Last Reviewed**: October 9, 2025
**Next Review**: January 9, 2026 (Quarterly)
**Status**: ⚠️ ACTION REQUIRED - Pre-Production Assessment

---

**Approvals Required**:

- [ ] CTO (Technical implementation)
- [ ] Data Protection Officer (Privacy compliance)
- [ ] Legal Counsel (Terms, policies, disclaimers)
- [ ] DevOps Lead (Infrastructure security)
- [ ] Product Manager (Feature prioritization)

**Contact**: security@hazardhawk.com
