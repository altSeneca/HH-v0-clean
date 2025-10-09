# Test Configuration Files & Examples

This document provides ready-to-use configuration files and test examples for the HazardHawk marketing website.

---

## Table of Contents

1. [Vitest Configuration](#vitest-configuration)
2. [Playwright Configuration](#playwright-configuration)
3. [Lighthouse CI Configuration](#lighthouse-ci-configuration)
4. [Example Test Files](#example-test-files)
5. [Package.json Scripts](#packagejson-scripts)
6. [Helper Utilities](#helper-utilities)

---

## Vitest Configuration

### vitest.config.ts

```typescript
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './test/setup.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      exclude: [
        'node_modules/',
        'test/',
        '**/*.test.{js,jsx,ts,tsx}',
        '**/*.spec.{js,jsx,ts,tsx}',
        '**/*.config.{js,ts}',
        '**/dist/**',
        '**/.next/**'
      ],
      lines: 80,
      functions: 80,
      branches: 75,
      statements: 80
    },
    include: ['**/*.test.{js,jsx,ts,tsx}'],
    exclude: ['node_modules', 'dist', '.next', 'e2e'],
    testTimeout: 10000,
    hookTimeout: 10000
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      '@components': path.resolve(__dirname, './src/components'),
      '@lib': path.resolve(__dirname, './src/lib'),
      '@styles': path.resolve(__dirname, './src/styles')
    }
  }
});
```

### test/setup.ts

```typescript
import { expect, afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/react';
import * as matchers from '@testing-library/jest-dom/matchers';

// Extend Vitest matchers
expect.extend(matchers);

// Cleanup after each test
afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

// Mock IntersectionObserver
global.IntersectionObserver = class IntersectionObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  takeRecords() {
    return [];
  }
  unobserve() {}
} as any;

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
  constructor() {}
  disconnect() {}
  observe() {}
  unobserve() {}
} as any;

// Mock Google Analytics
window.gtag = vi.fn();
window.dataLayer = [];

// Mock fetch if not available
if (!global.fetch) {
  global.fetch = vi.fn();
}
```

---

## Playwright Configuration

### playwright.config.ts

```typescript
import { defineConfig, devices } from '@playwright/test';

const PORT = process.env.PORT || 3000;
const baseURL = `http://localhost:${PORT}`;

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html', { outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'test-results/results.json' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
    ['list']
  ],
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    // Desktop browsers
    {
      name: 'chromium',
      use: { 
        ...devices['Desktop Chrome'],
        viewport: { width: 1920, height: 1080 }
      },
    },
    {
      name: 'firefox',
      use: { 
        ...devices['Desktop Firefox'],
        viewport: { width: 1920, height: 1080 }
      },
    },
    {
      name: 'webkit',
      use: { 
        ...devices['Desktop Safari'],
        viewport: { width: 1920, height: 1080 }
      },
    },
    // Mobile browsers
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 13'] },
    },
    // Tablet
    {
      name: 'tablet',
      use: { ...devices['iPad Pro'] },
    }
  ],
  webServer: {
    command: process.env.CI ? 'npm run start' : 'npm run dev',
    url: baseURL,
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});
```

### e2e/helpers/fixtures.ts

```typescript
import { test as base } from '@playwright/test';
import { injectAxe, checkA11y } from 'axe-playwright';

type Fixtures = {
  makeAxeBuilder: () => Promise<void>;
  trackAnalytics: () => Promise<any[]>;
};

export const test = base.extend<Fixtures>({
  makeAxeBuilder: async ({ page }, use) => {
    await injectAxe(page);
    await use(async () => {
      await checkA11y(page, null, {
        detailedReport: true,
        detailedReportOptions: { html: true }
      });
    });
  },

  trackAnalytics: async ({ page }, use) => {
    const events: any[] = [];
    
    await page.addInitScript(() => {
      window.dataLayer = window.dataLayer || [];
      const originalPush = window.dataLayer.push;
      window.dataLayer.push = function(...args) {
        // Store for test verification
        (window as any).__testEvents = (window as any).__testEvents || [];
        (window as any).__testEvents.push(...args);
        return originalPush.apply(this, args);
      };
    });

    await use(async () => {
      return page.evaluate(() => (window as any).__testEvents || []);
    });
  }
});

export { expect } from '@playwright/test';
```

---

## Lighthouse CI Configuration

### lighthouserc.js

```javascript
module.exports = {
  ci: {
    collect: {
      url: [
        'http://localhost:3000/',
        'http://localhost:3000/features',
        'http://localhost:3000/pricing',
        'http://localhost:3000/demo',
        'http://localhost:3000/blog'
      ],
      numberOfRuns: 3,
      settings: {
        onlyCategories: ['performance', 'accessibility', 'seo', 'best-practices'],
        // Emulate mobile
        emulatedFormFactor: 'mobile',
        throttling: {
          rttMs: 150,
          throughputKbps: 1638.4,
          cpuSlowdownMultiplier: 4
        }
      }
    },
    assert: {
      preset: 'lighthouse:recommended',
      assertions: {
        // Category scores
        'categories:performance': ['error', { minScore: 0.9 }],
        'categories:accessibility': ['error', { minScore: 1.0 }],
        'categories:seo': ['error', { minScore: 1.0 }],
        'categories:best-practices': ['error', { minScore: 0.9 }],
        
        // Core Web Vitals
        'largest-contentful-paint': ['error', { maxNumericValue: 2500 }],
        'first-contentful-paint': ['error', { maxNumericValue: 1800 }],
        'cumulative-layout-shift': ['error', { maxNumericValue: 0.1 }],
        'total-blocking-time': ['error', { maxNumericValue: 200 }],
        'speed-index': ['error', { maxNumericValue: 3400 }],
        'interactive': ['error', { maxNumericValue: 3800 }],
        
        // Resource sizes
        'total-byte-weight': ['warn', { maxNumericValue: 1000000 }],
        'dom-size': ['warn', { maxNumericValue: 800 }],
        
        // Images
        'uses-optimized-images': 'error',
        'uses-responsive-images': 'error',
        'modern-image-formats': 'warn',
        
        // JavaScript
        'unused-javascript': ['warn', { maxNumericValue: 50000 }],
        'bootup-time': ['warn', { maxNumericValue: 2000 }],
        
        // Network
        'uses-text-compression': 'error',
        'uses-long-cache-ttl': 'warn',
        
        // SEO
        'meta-description': 'error',
        'document-title': 'error',
        'link-text': 'error',
        'crawlable-anchors': 'error',
        'canonical': 'error',
        'robots-txt': 'error',
        
        // Accessibility
        'color-contrast': 'error',
        'image-alt': 'error',
        'label': 'error',
        'valid-lang': 'error',
        'aria-allowed-attr': 'error',
        'aria-valid-attr': 'error'
      }
    },
    upload: {
      target: 'temporary-public-storage',
      // Or use a LHCI server
      // target: 'lhci',
      // serverBaseUrl: 'https://your-lhci-server.com'
    }
  }
};
```

---

## Example Test Files

### test/unit/validation.test.ts

```typescript
import { describe, test, expect } from 'vitest';
import { 
  validateEmail, 
  validatePhone, 
  sanitizeInput,
  validateForm
} from '@/lib/validation';

describe('Email Validation', () => {
  test('should accept valid email addresses', () => {
    const validEmails = [
      'test@example.com',
      'user+tag@domain.co.uk',
      'name.surname@company.com'
    ];
    
    validEmails.forEach(email => {
      expect(validateEmail(email)).toBe(true);
    });
  });
  
  test('should reject invalid email addresses', () => {
    const invalidEmails = [
      'not-an-email',
      '@domain.com',
      'user@',
      'user @domain.com',
      ''
    ];
    
    invalidEmails.forEach(email => {
      expect(validateEmail(email)).toBe(false);
    });
  });
});

describe('Phone Validation', () => {
  test('should accept valid US phone numbers', () => {
    const validPhones = [
      '+1-555-123-4567',
      '(555) 123-4567',
      '555-123-4567',
      '5551234567'
    ];
    
    validPhones.forEach(phone => {
      expect(validatePhone(phone)).toBe(true);
    });
  });
  
  test('should reject invalid phone numbers', () => {
    const invalidPhones = [
      '123',
      'not-a-phone',
      ''
    ];
    
    invalidPhones.forEach(phone => {
      expect(validatePhone(phone)).toBe(false);
    });
  });
});

describe('Input Sanitization', () => {
  test('should escape HTML tags', () => {
    const input = '<script>alert("XSS")</script>';
    const sanitized = sanitizeInput(input);
    
    expect(sanitized).not.toContain('<script>');
    expect(sanitized).toContain('&lt;script&gt;');
  });
  
  test('should remove null bytes', () => {
    const input = 'test\0string';
    const sanitized = sanitizeInput(input);
    
    expect(sanitized).toBe('teststring');
  });
  
  test('should trim whitespace', () => {
    const input = '  test  ';
    const sanitized = sanitizeInput(input);
    
    expect(sanitized).toBe('test');
  });
});
```

### test/integration/newsletter.test.tsx

```typescript
import { describe, test, expect, beforeAll, afterAll, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import NewsletterForm from '@/components/NewsletterForm';

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Newsletter Form Integration', () => {
  test('should submit email to API successfully', async () => {
    let submittedEmail = '';
    
    server.use(
      http.post('/api/newsletter', async ({ request }) => {
        const body = await request.json();
        submittedEmail = body.email;
        return HttpResponse.json({ 
          success: true,
          message: 'Thank you for subscribing!' 
        });
      })
    );
    
    render(<NewsletterForm />);
    const user = userEvent.setup();
    
    await user.type(
      screen.getByLabelText(/email/i), 
      'test@example.com'
    );
    await user.click(
      screen.getByRole('button', { name: /subscribe/i })
    );
    
    await waitFor(() => {
      expect(screen.getByText(/thank you/i)).toBeInTheDocument();
    });
    
    expect(submittedEmail).toBe('test@example.com');
  });
  
  test('should handle API errors gracefully', async () => {
    server.use(
      http.post('/api/newsletter', () => {
        return HttpResponse.json(
          { error: 'Service temporarily unavailable' },
          { status: 503 }
        );
      })
    );
    
    render(<NewsletterForm />);
    const user = userEvent.setup();
    
    await user.type(
      screen.getByLabelText(/email/i),
      'test@example.com'
    );
    await user.click(
      screen.getByRole('button', { name: /subscribe/i })
    );
    
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/try again/i);
    });
  });
  
  test('should handle duplicate email submissions', async () => {
    server.use(
      http.post('/api/newsletter', () => {
        return HttpResponse.json(
          { error: 'Email already subscribed' },
          { status: 409 }
        );
      })
    );
    
    render(<NewsletterForm />);
    const user = userEvent.setup();
    
    await user.type(
      screen.getByLabelText(/email/i),
      'existing@example.com'
    );
    await user.click(
      screen.getByRole('button', { name: /subscribe/i })
    );
    
    await waitFor(() => {
      expect(screen.getByText(/already subscribed/i)).toBeInTheDocument();
    });
  });
});
```

### e2e/demo-request.spec.ts

```typescript
import { test, expect } from './helpers/fixtures';

test.describe('Demo Request Flow', () => {
  test('should complete full demo request journey', async ({ page, trackAnalytics }) => {
    // Step 1: Navigate to demo page
    await page.goto('/');
    await page.click('text=Schedule Demo');
    
    await expect(page).toHaveURL(/.*demo/);
    await expect(page).toHaveTitle(/Demo.*HazardHawk/);
    
    // Step 2: Fill out form
    await page.fill('[name="firstName"]', 'John');
    await page.fill('[name="lastName"]', 'Doe');
    await page.fill('[name="email"]', 'john.doe@construction.com');
    await page.fill('[name="company"]', 'BuildRight Construction');
    await page.fill('[name="phone"]', '+1-555-123-4567');
    await page.selectOption('[name="companySize"]', '50-200');
    await page.selectOption('[name="projectType"]', 'commercial');
    await page.fill('[name="message"]', 'Interested in enterprise plan for 100+ users');
    
    // Step 3: Submit form
    await page.click('button[type="submit"]');
    
    // Step 4: Verify success message
    await expect(page.locator('[data-success]')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('[data-success]')).toContainText(
      /we'll contact you within 24 hours/i
    );
    
    // Step 5: Verify analytics tracking
    const events = await trackAnalytics();
    const formEvent = events.find(e => e.event === 'form_submit');
    
    expect(formEvent).toBeDefined();
    expect(formEvent.form_name).toBe('demo_request');
    expect(formEvent.form_success).toBe(true);
  });
  
  test('should validate required fields', async ({ page }) => {
    await page.goto('/demo');
    
    // Try to submit empty form
    await page.click('button[type="submit"]');
    
    // Should show validation errors
    await expect(page.locator('[data-error="firstName"]')).toBeVisible();
    await expect(page.locator('[data-error="email"]')).toBeVisible();
    await expect(page.locator('[data-error="company"]')).toBeVisible();
  });
  
  test('should be accessible via keyboard', async ({ page }) => {
    await page.goto('/demo');
    
    // Tab through form fields
    await page.keyboard.press('Tab'); // First name
    await page.keyboard.type('John');
    
    await page.keyboard.press('Tab'); // Last name
    await page.keyboard.type('Doe');
    
    await page.keyboard.press('Tab'); // Email
    await page.keyboard.type('john@example.com');
    
    // Verify values entered
    expect(await page.inputValue('[name="firstName"]')).toBe('John');
    expect(await page.inputValue('[name="lastName"]')).toBe('Doe');
    expect(await page.inputValue('[name="email"]')).toBe('john@example.com');
  });
});
```

### e2e/seo.spec.ts

```typescript
import { test, expect } from '@playwright/test';

test.describe('SEO Validation', () => {
  test('should have complete meta tags on all pages', async ({ page }) => {
    const pages = ['/', '/features', '/pricing', '/demo'];
    
    for (const pagePath of pages) {
      await page.goto(pagePath);
      
      // Title
      const title = await page.title();
      expect(title).toBeTruthy();
      expect(title.length).toBeGreaterThan(30);
      expect(title.length).toBeLessThan(60);
      
      // Meta description
      const description = await page.getAttribute(
        'meta[name="description"]',
        'content'
      );
      expect(description).toBeTruthy();
      expect(description!.length).toBeGreaterThan(120);
      expect(description!.length).toBeLessThan(160);
      
      // Open Graph
      const ogTitle = await page.getAttribute(
        'meta[property="og:title"]',
        'content'
      );
      const ogImage = await page.getAttribute(
        'meta[property="og:image"]',
        'content'
      );
      const ogUrl = await page.getAttribute(
        'meta[property="og:url"]',
        'content'
      );
      
      expect(ogTitle).toBeTruthy();
      expect(ogImage).toMatch(/^https?:\/\//);
      expect(ogUrl).toContain('hazardhawk.com');
      
      // Canonical URL
      const canonical = await page.getAttribute(
        'link[rel="canonical"]',
        'href'
      );
      expect(canonical).toContain('hazardhawk.com');
    }
  });
  
  test('should have valid structured data', async ({ page }) => {
    await page.goto('/');
    
    const jsonLd = await page.evaluate(() => {
      const script = document.querySelector('script[type="application/ld+json"]');
      return script ? JSON.parse(script.textContent || '{}') : null;
    });
    
    expect(jsonLd).toBeTruthy();
    expect(jsonLd['@context']).toBe('https://schema.org');
    expect(jsonLd['@type']).toBe('Organization');
    expect(jsonLd.name).toBe('HazardHawk');
  });
  
  test('should have working sitemap', async ({ request }) => {
    const response = await request.get('/sitemap.xml');
    expect(response.status()).toBe(200);
    
    const contentType = response.headers()['content-type'];
    expect(contentType).toContain('xml');
    
    const text = await response.text();
    expect(text).toContain('<?xml');
    expect(text).toContain('<urlset');
    expect(text).toContain('https://hazardhawk.com');
  });
  
  test('should have robots.txt configured', async ({ request }) => {
    const response = await request.get('/robots.txt');
    expect(response.status()).toBe(200);
    
    const text = await response.text();
    expect(text).toContain('User-agent: *');
    expect(text).toContain('Sitemap:');
    expect(text).not.toContain('Disallow: /'); // Not blocking everything
  });
});
```

### e2e/performance.spec.ts

```typescript
import { test, expect } from '@playwright/test';

test.describe('Performance', () => {
  test('should load homepage quickly', async ({ page }) => {
    const start = Date.now();
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const loadTime = Date.now() - start;
    
    expect(loadTime).toBeLessThan(3000); // 3 seconds
  });
  
  test('should have optimized images', async ({ page }) => {
    await page.goto('/');
    
    // Get all images
    const images = await page.$$eval('img', imgs =>
      imgs.map(img => ({
        src: img.src,
        loading: img.loading,
        width: img.width,
        height: img.height
      }))
    );
    
    // Should use lazy loading
    const lazyImages = images.filter(img => img.loading === 'lazy');
    expect(lazyImages.length).toBeGreaterThan(3);
    
    // Should have dimensions
    images.forEach(img => {
      expect(img.width).toBeGreaterThan(0);
      expect(img.height).toBeGreaterThan(0);
    });
  });
  
  test('should have small bundle size', async ({ page }) => {
    const response = await page.goto('/');
    
    // Collect all script sizes
    const scripts = await page.$$eval('script[src]', scripts =>
      scripts.map(s => s.src)
    );
    
    // First-party scripts should be reasonable
    const localScripts = scripts.filter(src => 
      src.includes('hazardhawk.com') || src.startsWith('/')
    );
    
    expect(localScripts.length).toBeLessThan(10);
  });
});
```

### e2e/accessibility.spec.ts

```typescript
import { test, expect } from './helpers/fixtures';

test.describe('Accessibility', () => {
  test('should pass axe accessibility checks', async ({ page, makeAxeBuilder }) => {
    await page.goto('/');
    await makeAxeBuilder();
  });
  
  test('should be keyboard navigable', async ({ page }) => {
    await page.goto('/');
    
    // Tab through navigation
    await page.keyboard.press('Tab');
    let focused = await page.evaluate(() => document.activeElement?.tagName);
    expect(focused).toBe('A'); // First link
    
    // Continue tabbing
    await page.keyboard.press('Tab');
    focused = await page.evaluate(() => document.activeElement?.textContent);
    expect(focused).toBeTruthy();
  });
  
  test('should have proper ARIA labels', async ({ page }) => {
    await page.goto('/');
    
    // Check for buttons without labels
    const unlabeledButtons = await page.$$eval(
      'button:not([aria-label]):not(:has(*))',
      buttons => buttons.filter(btn => !btn.textContent?.trim()).length
    );
    
    expect(unlabeledButtons).toBe(0);
  });
  
  test('should have sufficient color contrast', async ({ page }) => {
    await page.goto('/');
    
    // This would typically use axe-core's color-contrast check
    // which is included in the makeAxeBuilder fixture
    const violations = await page.evaluate(async () => {
      // @ts-ignore
      const results = await axe.run();
      return results.violations.filter(v => v.id === 'color-contrast');
    });
    
    expect(violations.length).toBe(0);
  });
});
```

---

## Package.json Scripts

### package.json

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "start": "next start",
    "lint": "next lint",
    "type-check": "tsc --noEmit",
    
    "test": "vitest",
    "test:watch": "vitest --watch",
    "test:ui": "vitest --ui",
    "test:coverage": "vitest --coverage",
    
    "test:e2e": "playwright test",
    "test:e2e:ui": "playwright test --ui",
    "test:e2e:headed": "playwright test --headed",
    "test:e2e:debug": "playwright test --debug",
    "test:e2e:chromium": "playwright test --project=chromium",
    "test:e2e:firefox": "playwright test --project=firefox",
    "test:e2e:webkit": "playwright test --project=webkit",
    "test:e2e:mobile": "playwright test --project=mobile-chrome --project=mobile-safari",
    
    "test:seo": "playwright test e2e/seo.spec.ts",
    "test:a11y": "playwright test e2e/accessibility.spec.ts",
    "test:perf": "playwright test e2e/performance.spec.ts",
    
    "lighthouse": "lhci autorun",
    "lighthouse:ci": "lhci autorun --config=lighthouserc.js",
    
    "test:all": "npm run test:coverage && npm run test:e2e && npm run lighthouse",
    
    "visual:update": "playwright test --update-snapshots",
    "visual:percy": "percy exec -- playwright test",
    
    "security:audit": "npm audit --audit-level=moderate",
    "security:check": "npx snyk test"
  }
}
```

---

## Helper Utilities

### test/helpers/analytics.ts

```typescript
export function setupAnalyticsMock() {
  const events: any[] = [];
  
  // @ts-ignore
  window.gtag = (command: string, event: string, params: any) => {
    events.push({ command, event, ...params });
  };
  
  window.dataLayer = window.dataLayer || [];
  
  return {
    events,
    getEvent(eventName: string) {
      return events.find(e => e.event === eventName);
    },
    getEvents(eventName: string) {
      return events.filter(e => e.event === eventName);
    },
    clear() {
      events.length = 0;
    }
  };
}

export async function waitForAnalyticsEvent(
  page: any,
  eventName: string,
  timeout = 5000
) {
  return page.waitForFunction(
    (name: string) => {
      return window.dataLayer?.some((event: any) => event.event === name);
    },
    eventName,
    { timeout }
  );
}
```

### test/helpers/render.tsx

```typescript
import React from 'react';
import { render as rtlRender, RenderOptions } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
      cacheTime: 0
    }
  }
});

interface WrapperProps {
  children: React.ReactNode;
}

function Wrapper({ children }: WrapperProps) {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}

export function render(ui: React.ReactElement, options?: RenderOptions) {
  return rtlRender(ui, { wrapper: Wrapper, ...options });
}

export * from '@testing-library/react';
export { default as userEvent } from '@testing-library/user-event';
```

### test/helpers/msw-handlers.ts

```typescript
import { http, HttpResponse } from 'msw';

export const handlers = [
  // Newsletter signup
  http.post('/api/newsletter', async ({ request }) => {
    const body = await request.json();
    
    if (!body.email) {
      return HttpResponse.json(
        { error: 'Email is required' },
        { status: 400 }
      );
    }
    
    return HttpResponse.json({
      success: true,
      message: 'Thank you for subscribing!'
    });
  }),
  
  // Demo request
  http.post('/api/demo-request', async ({ request }) => {
    const body = await request.json();
    
    const required = ['firstName', 'lastName', 'email', 'company'];
    const missing = required.filter(field => !body[field]);
    
    if (missing.length > 0) {
      return HttpResponse.json(
        { error: `Missing fields: ${missing.join(', ')}` },
        { status: 400 }
      );
    }
    
    return HttpResponse.json({
      success: true,
      message: 'Demo request submitted successfully',
      requestId: 'demo-123'
    });
  }),
  
  // Contact form
  http.post('/api/contact', async ({ request }) => {
    const body = await request.json();
    
    return HttpResponse.json({
      success: true,
      message: 'Message sent successfully'
    });
  })
];
```

### e2e/helpers/network.ts

```typescript
import { Page } from '@playwright/test';

export async function setupNetworkThrottling(
  page: Page,
  profile: 'Slow 3G' | 'Fast 3G' | '4G'
) {
  const profiles = {
    'Slow 3G': {
      downloadThroughput: (500 * 1024) / 8,
      uploadThroughput: (500 * 1024) / 8,
      latency: 400
    },
    'Fast 3G': {
      downloadThroughput: (1.6 * 1024 * 1024) / 8,
      uploadThroughput: (750 * 1024) / 8,
      latency: 150
    },
    '4G': {
      downloadThroughput: (4 * 1024 * 1024) / 8,
      uploadThroughput: (3 * 1024 * 1024) / 8,
      latency: 50
    }
  };
  
  const client = await page.context().newCDPSession(page);
  await client.send('Network.emulateNetworkConditions', {
    offline: false,
    ...profiles[profile]
  });
}

export async function blockResources(
  page: Page,
  resourceTypes: string[]
) {
  await page.route('**/*', route => {
    if (resourceTypes.includes(route.request().resourceType())) {
      route.abort();
    } else {
      route.continue();
    }
  });
}
```

---

## Size Limit Configuration

### .size-limit.json

```json
[
  {
    "name": "Homepage JS",
    "path": ".next/static/chunks/pages/index-*.js",
    "limit": "200 KB"
  },
  {
    "name": "Homepage CSS",
    "path": ".next/static/css/*.css",
    "limit": "50 KB"
  },
  {
    "name": "All JS",
    "path": ".next/static/chunks/**/*.js",
    "limit": "500 KB"
  }
]
```

---

## Pa11y Configuration

### .pa11yci.json

```json
{
  "defaults": {
    "standard": "WCAG2AA",
    "runners": ["axe", "htmlcs"],
    "timeout": 30000,
    "wait": 1000,
    "chromeLaunchConfig": {
      "args": ["--no-sandbox"]
    }
  },
  "urls": [
    "http://localhost:3000/",
    "http://localhost:3000/features",
    "http://localhost:3000/pricing",
    "http://localhost:3000/demo",
    "http://localhost:3000/contact"
  ]
}
```

---

## Summary

These configuration files and examples provide a complete testing setup for the HazardHawk marketing website. Copy these into your project and customize as needed.

### Quick Start

```bash
# Install dependencies
npm install -D vitest @vitest/ui @vitest/coverage-v8 \
  @testing-library/react @testing-library/jest-dom @testing-library/user-event \
  @playwright/test axe-playwright \
  @lhci/cli lighthouse \
  msw

# Run tests
npm test                  # Unit tests
npm run test:e2e          # E2E tests
npm run lighthouse        # Performance audit
npm run test:all          # Everything
```

### Next Steps

1. Copy configuration files to your project
2. Implement critical E2E tests first
3. Add unit tests for business logic
4. Set up CI/CD pipeline
5. Configure monitoring and alerts
