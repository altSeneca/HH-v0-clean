# HazardHawk Marketing Website - Comprehensive Testing Strategy

**Document Version:** 1.0  
**Last Updated:** 2025-10-09  
**Owner:** Test Guardian  
**Status:** Active

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Testing Philosophy](#testing-philosophy)
3. [Test Coverage Requirements](#test-coverage-requirements)
4. [SEO Testing](#seo-testing)
5. [Conversion Testing](#conversion-testing)
6. [Performance Testing](#performance-testing)
7. [Edge Cases & Compatibility](#edge-cases--compatibility)
8. [Quality Gates](#quality-gates)
9. [Testing Tools & Frameworks](#testing-tools--frameworks)
10. [Test Implementation Examples](#test-implementation-examples)
11. [CI/CD Integration](#cicd-integration)
12. [Maintenance & Monitoring](#maintenance--monitoring)

---

## Executive Summary

This document outlines a comprehensive testing strategy for the HazardHawk marketing website, focusing on three critical pillars:

1. **Reliability** - Ensure all user interactions work flawlessly
2. **Performance** - Deliver fast experiences, especially on construction site networks
3. **Conversion** - Maximize sign-ups and demo requests

### Key Metrics Targets

| Metric | Target | Critical? |
|--------|--------|-----------|
| Lighthouse Performance | 90+ | Yes |
| Lighthouse SEO | 100 | Yes |
| Lighthouse Accessibility | 100 | Yes |
| Test Coverage | 80%+ | Yes |
| E2E Pass Rate | 100% | Yes |
| Form Conversion Rate | >3% | No |
| Mobile Performance Score | 85+ | Yes |
| Time to Interactive (TTI) | <3.5s | Yes |

---

## Testing Philosophy

### Simple, Loveable, Complete Applied to Testing

#### Simple
- **Focused Test Cases**: Each test validates one specific behavior
- **Clear Naming**: `should_display_error_when_email_invalid` not `test1`
- **Minimal Setup**: DRY test utilities, but prioritize clarity
- **Fast Feedback**: Unit tests <1s, Integration <10s, E2E <2min

#### Loveable
- **Developer Experience**: Tests that are joy to write and debug
- **Confidence Building**: Tests catch real bugs, not implementation details
- **Documentation**: Tests serve as living documentation
- **Visual Feedback**: Screenshot diffs for UI regressions

#### Complete
- **Critical Path Coverage**: 100% of user journeys tested
- **Edge Case Handling**: Network failures, slow loads, browser quirks
- **Real-World Scenarios**: Construction site conditions (slow 3G, gloves, sun glare)
- **Cross-Browser**: Chrome, Safari, Firefox, Mobile browsers

---

## Test Coverage Requirements

### Test Pyramid Strategy

```
        /\
       /  \      E2E Tests (10%)
      /    \     - Critical user journeys
     /------\    - Cross-browser validation
    /        \   
   /          \  Integration Tests (20%)
  /            \ - API interactions
 /              \- Form submissions
/--------------\ 
|              | Unit Tests (70%)
|              | - Utility functions
|              | - Validation logic
+--------------+ - Business logic
```

### Unit Tests (70% of test suite)

**Target Coverage: 80%+**

#### What to Test
- **Utility Functions**
  - Email validation
  - Phone number formatting
  - Form data sanitization
  - Analytics event builders
  - URL parameter parsing

- **Business Logic**
  - Pricing calculations
  - Feature flag checks
  - User tier qualification logic
  - Discount code validation

- **Data Transformations**
  - Form data serialization
  - API response parsing
  - Date formatting
  - Error message generation

#### What NOT to Test
- Framework code (React, Next.js internals)
- Third-party libraries
- CSS styles (use visual regression instead)
- Implementation details (focus on behavior)

### Integration Tests (20% of test suite)

**Target Coverage: Critical Endpoints**

#### What to Test
- **Newsletter Signup**
  - POST /api/newsletter with valid email
  - Duplicate email handling
  - Invalid email rejection
  - Rate limiting behavior

- **Demo Request**
  - POST /api/demo-request with full form
  - Required field validation
  - Email notification triggers
  - CRM integration (if applicable)

- **Contact Form**
  - Message submission
  - File attachment handling
  - Spam protection (reCAPTCHA)
  - Auto-reply emails

- **Analytics Tracking**
  - Page view events
  - CTA click events
  - Form interaction events
  - Conversion events

### End-to-End Tests (10% of test suite)

**Target Coverage: Critical User Journeys**

#### Critical Paths (Must Test)
1. **Homepage to Trial Signup**
   - Land on homepage
   - Click "Start Free Trial"
   - Fill out form
   - Submit successfully
   - Receive confirmation

2. **Feature Exploration to Demo Request**
   - Navigate to Features section
   - Explore pricing tiers
   - Click "Schedule Demo"
   - Complete demo form
   - Confirm booking

3. **Mobile User Journey**
   - Access site on mobile
   - Browse features
   - Watch demo video
   - Sign up for newsletter
   - Verify mobile-optimized experience

4. **SEO Landing Page Flow**
   - Land from Google search
   - Read specific feature page
   - Navigate to pricing
   - Download resource (PDF)
   - Submit lead form

---

## SEO Testing

### Metadata Validation

#### Required Tests

**1. Title Tags**
```javascript
// Test: All pages have unique, descriptive titles
test('should have unique title tags on each page', async () => {
  const pages = ['/', '/features', '/pricing', '/demo'];
  const titles = new Set();
  
  for (const page of pages) {
    const response = await fetch(`${baseUrl}${page}`);
    const html = await response.text();
    const title = html.match(/<title>(.*?)<\/title>/)[1];
    
    expect(title).toBeTruthy();
    expect(title.length).toBeGreaterThan(30);
    expect(title.length).toBeLessThan(60);
    expect(titles.has(title)).toBe(false); // Unique
    titles.add(title);
  }
});
```

**2. Meta Descriptions**
```javascript
test('should have compelling meta descriptions', async () => {
  const pages = ['/', '/features', '/pricing'];
  
  for (const page of pages) {
    const metaDesc = await getMetaDescription(page);
    
    expect(metaDesc).toBeTruthy();
    expect(metaDesc.length).toBeGreaterThan(120);
    expect(metaDesc.length).toBeLessThan(160);
    expect(metaDesc).toContain('construction'); // Brand keyword
  }
});
```

**3. Open Graph Tags**
```javascript
test('should have complete Open Graph metadata', async () => {
  const ogTags = await getOpenGraphTags('/');
  
  expect(ogTags['og:title']).toBeTruthy();
  expect(ogTags['og:description']).toBeTruthy();
  expect(ogTags['og:image']).toMatch(/^https?:\/\//);
  expect(ogTags['og:type']).toBe('website');
  expect(ogTags['og:url']).toBe('https://hazardhawk.com/');
});
```

### Schema.org Markup Validation

```javascript
test('should have valid JSON-LD structured data', async () => {
  const html = await fetchPage('/');
  const jsonLd = extractJsonLd(html);
  
  // Validate Organization schema
  expect(jsonLd['@type']).toBe('Organization');
  expect(jsonLd.name).toBe('HazardHawk');
  expect(jsonLd.url).toBe('https://hazardhawk.com');
  expect(jsonLd.logo).toBeTruthy();
  
  // Validate SoftwareApplication schema
  const app = jsonLd.hasProduct;
  expect(app['@type']).toBe('SoftwareApplication');
  expect(app.applicationCategory).toBe('BusinessApplication');
  expect(app.offers).toBeDefined();
  expect(app.aggregateRating).toBeDefined();
});
```

### Sitemap & Robots Testing

```javascript
test('should have valid XML sitemap', async () => {
  const sitemap = await fetch(`${baseUrl}/sitemap.xml`);
  expect(sitemap.status).toBe(200);
  expect(sitemap.headers.get('content-type')).toContain('xml');
  
  const xml = await sitemap.text();
  const urls = extractUrls(xml);
  
  expect(urls.length).toBeGreaterThan(10);
  expect(urls).toContain('https://hazardhawk.com/');
  expect(urls).toContain('https://hazardhawk.com/pricing');
  
  // Validate all URLs are accessible
  for (const url of urls) {
    const response = await fetch(url);
    expect(response.status).toBe(200);
  }
});

test('should have properly configured robots.txt', async () => {
  const robots = await fetch(`${baseUrl}/robots.txt`);
  const text = await robots.text();
  
  expect(text).toContain('User-agent: *');
  expect(text).toContain('Allow: /');
  expect(text).toContain('Sitemap: https://hazardhawk.com/sitemap.xml');
  expect(text).not.toContain('Disallow: /'); // Not blocking all
});
```

### Canonical URL Testing

```javascript
test('should have canonical URLs to prevent duplicate content', async () => {
  const pages = ['/', '/features', '/pricing'];
  
  for (const page of pages) {
    const canonical = await getCanonicalUrl(page);
    const expected = `https://hazardhawk.com${page === '/' ? '' : page}`;
    
    expect(canonical).toBe(expected);
  }
});

test('should handle URL parameters with canonical tags', async () => {
  const canonical = await getCanonicalUrl('/?utm_source=google&utm_campaign=test');
  expect(canonical).toBe('https://hazardhawk.com/'); // Strips params
});
```

### Mobile-Friendliness Testing

```javascript
test('should pass Google Mobile-Friendly test', async () => {
  const result = await googleMobileFriendlyTest(baseUrl);
  
  expect(result.mobileFriendly).toBe(true);
  expect(result.issues).toHaveLength(0);
});

test('should have mobile viewport meta tag', async () => {
  const viewport = await getMetaTag('viewport', '/');
  expect(viewport).toContain('width=device-width');
  expect(viewport).toContain('initial-scale=1');
});
```

---

## Conversion Testing

### A/B Test Framework Setup

#### Recommended Tool: Google Optimize or Optimizely

**Test Configuration Example:**

```javascript
// lib/ab-testing.js
export const EXPERIMENTS = {
  HERO_CTA: {
    id: 'hero_cta_test',
    variants: {
      control: 'Start Free 30-Day Trial',
      variant_a: 'Get Started Free',
      variant_b: 'Try HazardHawk Free'
    }
  },
  PRICING_DISPLAY: {
    id: 'pricing_display_test',
    variants: {
      control: 'monthly',
      variant_a: 'annual_highlighted'
    }
  }
};

export function getVariant(experimentId) {
  // Implement variant selection logic
  // Store in localStorage or cookie
  // Send to analytics
}
```

**Test Implementation:**

```javascript
test('should track A/B test variant assignments', async () => {
  const { page } = await setupBrowser();
  await page.goto('/');
  
  // Check variant is assigned
  const variant = await page.evaluate(() => {
    return localStorage.getItem('hero_cta_variant');
  });
  
  expect(['control', 'variant_a', 'variant_b']).toContain(variant);
  
  // Verify analytics event fired
  const events = await getAnalyticsEvents(page);
  expect(events).toContainEqual({
    event: 'experiment_view',
    experiment_id: 'hero_cta_test',
    variant: variant
  });
});
```

### Analytics Tracking Verification

#### Google Analytics 4 Event Testing

```javascript
// test/analytics.test.js
import { setupAnalyticsMock } from './helpers/analytics-mock';

describe('Analytics Tracking', () => {
  test('should fire page_view event on load', async () => {
    const analytics = setupAnalyticsMock();
    await page.goto('/');
    
    expect(analytics.events).toContainEqual({
      event: 'page_view',
      page_path: '/',
      page_title: 'HazardHawk - AI-Powered Construction Safety'
    });
  });
  
  test('should track CTA clicks', async () => {
    const analytics = setupAnalyticsMock();
    await page.goto('/');
    await page.click('[data-testid="hero-cta-primary"]');
    
    expect(analytics.events).toContainEqual({
      event: 'cta_click',
      cta_text: 'Start Free Trial',
      cta_location: 'hero',
      destination_url: '/signup'
    });
  });
  
  test('should track video plays', async () => {
    await page.goto('/');
    await page.click('[data-testid="demo-video-play"]');
    
    await waitForAnalyticsEvent('video_start', {
      video_title: 'HazardHawk Product Demo',
      video_duration: 120
    });
  });
  
  test('should track form interactions', async () => {
    await page.goto('/demo');
    await page.type('#email', 'test@example.com');
    
    expect(analytics.events).toContainEqual({
      event: 'form_start',
      form_name: 'demo_request'
    });
    
    await page.click('button[type="submit"]');
    
    expect(analytics.events).toContainEqual({
      event: 'form_submit',
      form_name: 'demo_request',
      form_success: true
    });
  });
});
```

### Form Validation Testing

```javascript
describe('Form Validation', () => {
  test('should validate email format', async () => {
    await page.goto('/signup');
    await page.type('#email', 'invalid-email');
    await page.click('button[type="submit"]');
    
    const error = await page.textContent('[data-error="email"]');
    expect(error).toContain('valid email');
  });
  
  test('should require all mandatory fields', async () => {
    await page.goto('/demo');
    await page.click('button[type="submit"]');
    
    const errors = await page.$$('[data-error]');
    expect(errors.length).toBeGreaterThan(0);
  });
  
  test('should sanitize user input', async () => {
    await page.type('#message', '<script>alert("XSS")</script>');
    await page.click('button[type="submit"]');
    
    const submitted = await getSubmittedData();
    expect(submitted.message).not.toContain('<script>');
    expect(submitted.message).toContain('&lt;script&gt;');
  });
});
```

### CTA Click Tracking

```javascript
test('should track all CTA interactions', async () => {
  const ctas = [
    { selector: '.hero .btn-primary', location: 'hero' },
    { selector: '.pricing .btn-primary', location: 'pricing' },
    { selector: '.footer .btn-primary', location: 'footer' }
  ];
  
  for (const cta of ctas) {
    await page.goto('/');
    await page.click(cta.selector);
    
    const event = await getLastAnalyticsEvent();
    expect(event.event).toBe('cta_click');
    expect(event.cta_location).toBe(cta.location);
  }
});
```

### User Behavior Heatmaps

**Recommended Tools:**
- Hotjar
- Microsoft Clarity
- Lucky Orange

```javascript
test('should initialize heatmap tracking', async () => {
  await page.goto('/');
  
  const hotjarScript = await page.evaluate(() => {
    return window.hj !== undefined;
  });
  
  expect(hotjarScript).toBe(true);
});
```

---

## Performance Testing

### Core Web Vitals Benchmarks

#### Required Metrics

| Metric | Good | Needs Improvement | Poor |
|--------|------|-------------------|------|
| LCP (Largest Contentful Paint) | ≤2.5s | 2.5s-4.0s | >4.0s |
| FID (First Input Delay) | ≤100ms | 100ms-300ms | >300ms |
| CLS (Cumulative Layout Shift) | ≤0.1 | 0.1-0.25 | >0.25 |
| FCP (First Contentful Paint) | ≤1.8s | 1.8s-3.0s | >3.0s |
| TTI (Time to Interactive) | ≤3.8s | 3.8s-7.3s | >7.3s |
| TBT (Total Blocking Time) | ≤200ms | 200ms-600ms | >600ms |

#### Lighthouse CI Configuration

```javascript
// lighthouserc.js
module.exports = {
  ci: {
    collect: {
      url: [
        'http://localhost:3000/',
        'http://localhost:3000/features',
        'http://localhost:3000/pricing',
        'http://localhost:3000/demo'
      ],
      numberOfRuns: 3,
      settings: {
        preset: 'desktop'
      }
    },
    assert: {
      preset: 'lighthouse:recommended',
      assertions: {
        'categories:performance': ['error', { minScore: 0.9 }],
        'categories:accessibility': ['error', { minScore: 1.0 }],
        'categories:seo': ['error', { minScore: 1.0 }],
        'categories:best-practices': ['error', { minScore: 0.9 }],
        
        // Core Web Vitals
        'largest-contentful-paint': ['error', { maxNumericValue: 2500 }],
        'cumulative-layout-shift': ['error', { maxNumericValue: 0.1 }],
        'total-blocking-time': ['error', { maxNumericValue: 200 }],
        
        // Resource sizes
        'total-byte-weight': ['warn', { maxNumericValue: 1000000 }], // 1MB
        'dom-size': ['warn', { maxNumericValue: 800 }],
        'uses-optimized-images': 'error',
        'uses-text-compression': 'error',
        'uses-responsive-images': 'error'
      }
    },
    upload: {
      target: 'temporary-public-storage'
    }
  }
};
```

### Image Optimization Validation

```javascript
describe('Image Optimization', () => {
  test('should use next-gen formats (WebP, AVIF)', async () => {
    await page.goto('/');
    const images = await page.$$eval('img', imgs => 
      imgs.map(img => img.src)
    );
    
    const nextGenImages = images.filter(src => 
      src.includes('.webp') || src.includes('.avif')
    );
    
    expect(nextGenImages.length / images.length).toBeGreaterThan(0.8);
  });
  
  test('should have appropriate image dimensions', async () => {
    const images = await page.$$eval('img', imgs =>
      imgs.map(img => ({
        src: img.src,
        naturalWidth: img.naturalWidth,
        clientWidth: img.clientWidth
      }))
    );
    
    for (const img of images) {
      const ratio = img.naturalWidth / img.clientWidth;
      expect(ratio).toBeLessThan(2); // Not serving huge images
      expect(ratio).toBeGreaterThan(0.5); // Not too small either
    }
  });
  
  test('should lazy load below-the-fold images', async () => {
    await page.goto('/');
    const lazyImages = await page.$$('[loading="lazy"]');
    expect(lazyImages.length).toBeGreaterThan(5);
  });
});
```

### Bundle Size Monitoring

```javascript
// test/bundle-size.test.js
import { getPageSize } from './helpers/performance';

describe('Bundle Size', () => {
  test('should keep homepage JS bundle under 200KB', async () => {
    const size = await getPageSize('/', 'script');
    expect(size).toBeLessThan(200 * 1024); // 200KB
  });
  
  test('should keep CSS bundle under 50KB', async () => {
    const size = await getPageSize('/', 'stylesheet');
    expect(size).toBeLessThan(50 * 1024); // 50KB
  });
  
  test('should not load unnecessary third-party scripts', async () => {
    await page.goto('/');
    const scripts = await page.$$eval('script[src]', scripts =>
      scripts.map(s => s.src)
    );
    
    const thirdParty = scripts.filter(src => 
      !src.includes('hazardhawk.com')
    );
    
    // Only allow essential third-party (analytics, etc.)
    expect(thirdParty.length).toBeLessThan(5);
  });
});
```

### Caching Strategy Verification

```javascript
describe('Caching Strategy', () => {
  test('should have proper cache headers for static assets', async () => {
    const response = await fetch(`${baseUrl}/images/logo.svg`);
    const cacheControl = response.headers.get('cache-control');
    
    expect(cacheControl).toContain('public');
    expect(cacheControl).toMatch(/max-age=\d+/);
    expect(parseInt(cacheControl.match(/max-age=(\d+)/)[1]))
      .toBeGreaterThan(86400); // At least 1 day
  });
  
  test('should not cache HTML pages', async () => {
    const response = await fetch(`${baseUrl}/`);
    const cacheControl = response.headers.get('cache-control');
    
    expect(cacheControl).toMatch(/no-cache|must-revalidate/);
  });
  
  test('should use ETags for conditional requests', async () => {
    const response = await fetch(`${baseUrl}/styles.css`);
    const etag = response.headers.get('etag');
    
    expect(etag).toBeTruthy();
  });
});
```

### Load Time Testing Under Various Conditions

```javascript
// test/performance-conditions.test.js
import { setupNetworkThrottling } from './helpers/network';

describe('Performance Under Poor Network', () => {
  test('should load in under 5s on Slow 3G', async () => {
    await setupNetworkThrottling(page, 'Slow 3G');
    
    const start = Date.now();
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const duration = Date.now() - start;
    
    expect(duration).toBeLessThan(5000);
  });
  
  test('should show loading indicators', async () => {
    await setupNetworkThrottling(page, 'Slow 3G');
    await page.goto('/');
    
    // Check for skeleton screens or spinners
    const loading = await page.$('[data-loading]');
    expect(loading).toBeTruthy();
  });
  
  test('should cache resources for offline access', async () => {
    await page.goto('/');
    await page.context().setOffline(true);
    await page.reload();
    
    const isOffline = await page.evaluate(() => !navigator.onLine);
    expect(isOffline).toBe(true);
    
    // Page should still load with cached resources
    const title = await page.title();
    expect(title).toBeTruthy();
  });
});
```

---

## Edge Cases & Compatibility

### Browser Compatibility Testing

```javascript
// playwright.config.js
export default {
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
    { name: 'mobile-chrome', use: { ...devices['Pixel 5'] } },
    { name: 'mobile-safari', use: { ...devices['iPhone 13'] } },
    { name: 'tablet', use: { ...devices['iPad Pro'] } }
  ]
};
```

```javascript
describe('Cross-Browser Compatibility', () => {
  test('should render correctly on all browsers', async () => {
    await page.goto('/');
    
    // Take screenshot for visual comparison
    await page.screenshot({ 
      path: `screenshots/${browserName}-homepage.png`,
      fullPage: true 
    });
    
    // Check critical elements exist
    expect(await page.$('header nav')).toBeTruthy();
    expect(await page.$('.hero')).toBeTruthy();
    expect(await page.$('footer')).toBeTruthy();
  });
  
  test('should handle CSS features gracefully', async () => {
    await page.goto('/');
    
    // Check for fallbacks
    const hasGridSupport = await page.evaluate(() => 
      CSS.supports('display', 'grid')
    );
    
    if (!hasGridSupport) {
      // Verify fallback layout works
      const layout = await page.$eval('.grid', el => 
        window.getComputedStyle(el).display
      );
      expect(layout).toBe('flex'); // Fallback
    }
  });
});
```

### Network Condition Testing

```javascript
describe('Network Resilience', () => {
  test('should handle intermittent connectivity', async () => {
    await page.goto('/');
    
    // Simulate network failure during form submit
    await page.route('**/api/newsletter', route => 
      route.abort('failed')
    );
    
    await page.type('#email', 'test@example.com');
    await page.click('button[type="submit"]');
    
    // Should show error message
    const error = await page.textContent('[role="alert"]');
    expect(error).toContain('network');
  });
  
  test('should retry failed requests', async () => {
    let attempts = 0;
    await page.route('**/api/demo', route => {
      attempts++;
      if (attempts < 3) {
        route.abort('failed');
      } else {
        route.fulfill({ status: 200 });
      }
    });
    
    await submitDemoForm(page);
    await page.waitForSelector('[data-success]');
    
    expect(attempts).toBe(3);
  });
});
```

### Screen Size & Responsive Design

```javascript
describe('Responsive Design', () => {
  const viewports = [
    { name: 'mobile', width: 375, height: 667 },
    { name: 'tablet', width: 768, height: 1024 },
    { name: 'desktop', width: 1920, height: 1080 },
    { name: 'ultra-wide', width: 3440, height: 1440 }
  ];
  
  for (const viewport of viewports) {
    test(`should be usable on ${viewport.name}`, async () => {
      await page.setViewportSize(viewport);
      await page.goto('/');
      
      // Navigation should be accessible
      const nav = await page.$('nav');
      expect(await nav.isVisible()).toBe(true);
      
      // CTA should be visible without scrolling
      const cta = await page.$('.hero .btn-primary');
      const isInViewport = await cta.evaluate(el => {
        const rect = el.getBoundingClientRect();
        return rect.top >= 0 && rect.bottom <= window.innerHeight;
      });
      expect(isInViewport).toBe(true);
    });
  }
});
```

### Accessibility with Assistive Technologies

```javascript
describe('Assistive Technology Support', () => {
  test('should be navigable via keyboard', async () => {
    await page.goto('/');
    
    // Tab through interactive elements
    await page.keyboard.press('Tab'); // Logo/Brand
    await page.keyboard.press('Tab'); // Nav item 1
    await page.keyboard.press('Tab'); // Nav item 2
    
    const focused = await page.evaluate(() => 
      document.activeElement.textContent
    );
    expect(focused).toBe('Features');
  });
  
  test('should have proper ARIA labels', async () => {
    await page.goto('/');
    
    const missingLabels = await page.$$eval('button, a', elements =>
      elements
        .filter(el => !el.textContent.trim() && !el.getAttribute('aria-label'))
        .length
    );
    
    expect(missingLabels).toBe(0);
  });
  
  test('should announce form errors to screen readers', async () => {
    await page.goto('/signup');
    await page.click('button[type="submit"]');
    
    const errorRegion = await page.$('[role="alert"]');
    expect(errorRegion).toBeTruthy();
    
    const ariaLive = await errorRegion.getAttribute('aria-live');
    expect(ariaLive).toBe('polite');
  });
  
  test('should have sufficient color contrast', async () => {
    await page.goto('/');
    
    // Use axe-core for automated testing
    const violations = await checkA11y(page);
    const contrastIssues = violations.filter(v => 
      v.id === 'color-contrast'
    );
    
    expect(contrastIssues).toHaveLength(0);
  });
});
```

### Construction Site Specific Scenarios

```javascript
describe('Construction Site Conditions', () => {
  test('should be usable with gloves (large touch targets)', async () => {
    await page.goto('/');
    
    const buttons = await page.$$eval('button, a.btn', elements =>
      elements.map(el => {
        const rect = el.getBoundingClientRect();
        return {
          width: rect.width,
          height: rect.height
        };
      })
    );
    
    // Touch targets should be at least 44x44px
    for (const btn of buttons) {
      expect(btn.width).toBeGreaterThanOrEqual(44);
      expect(btn.height).toBeGreaterThanOrEqual(44);
    }
  });
  
  test('should have high contrast mode', async () => {
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.goto('/');
    
    // Verify dark mode styles applied
    const bgColor = await page.$eval('body', el =>
      window.getComputedStyle(el).backgroundColor
    );
    
    expect(bgColor).toMatch(/rgb\(.*,.*,.*\)/);
    // Should be dark color
  });
  
  test('should show critical info without images (slow network)', async () => {
    await page.route('**/*.{png,jpg,jpeg,svg}', route => route.abort());
    await page.goto('/');
    
    // All text content should still be readable
    const headingText = await page.textContent('h1');
    expect(headingText).toContain('HazardHawk');
    
    // CTAs should have text (not image buttons)
    const ctaText = await page.textContent('.hero .btn-primary');
    expect(ctaText.length).toBeGreaterThan(0);
  });
});
```

---

## Quality Gates

### Pre-Deployment Checklist

```yaml
quality_gates:
  must_pass:
    - name: "Unit Tests"
      command: "npm test"
      threshold: "100% pass"
      
    - name: "E2E Tests"
      command: "npm run test:e2e"
      threshold: "100% pass"
      
    - name: "Lighthouse Performance"
      threshold: "score >= 90"
      
    - name: "Lighthouse SEO"
      threshold: "score == 100"
      
    - name: "Lighthouse Accessibility"
      threshold: "score == 100"
      
    - name: "Bundle Size"
      threshold: "JS < 200KB, CSS < 50KB"
      
    - name: "Test Coverage"
      command: "npm run test:coverage"
      threshold: "lines >= 80%"
  
  should_pass:
    - name: "Visual Regression"
      tool: "Percy or Chromatic"
      threshold: "0 unreviewed changes"
      
    - name: "Security Scan"
      command: "npm audit"
      threshold: "0 high/critical vulnerabilities"
      
    - name: "Broken Links"
      threshold: "0 broken internal links"
```

### CI/CD Pipeline Requirements

```javascript
// .github/workflows/quality-gates.yml
name: Quality Gates

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - run: npm test -- --coverage
      - name: Upload coverage
        uses: codecov/codecov-action@v3
        
  e2e-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - run: npx playwright install --with-deps
      - run: npm run test:e2e
      - uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report
          path: playwright-report/
          
  lighthouse:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
      - run: npm ci
      - run: npm run build
      - run: npm start & npx wait-on http://localhost:3000
      - run: npm run lighthouse:ci
      - uses: treosh/lighthouse-ci-action@v9
        with:
          urls: |
            http://localhost:3000
            http://localhost:3000/features
            http://localhost:3000/pricing
          uploadArtifacts: true
          
  bundle-size:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: andresz1/size-limit-action@v1
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
```

### Coverage Thresholds

```javascript
// vitest.config.js
export default {
  test: {
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      lines: 80,
      functions: 80,
      branches: 75,
      statements: 80,
      exclude: [
        'node_modules/',
        'test/',
        '**/*.test.js',
        '**/*.config.js'
      ]
    }
  }
};
```

---

## Testing Tools & Frameworks

### Core Testing Stack

```json
{
  "devDependencies": {
    // Unit Testing
    "vitest": "^3.2.4",
    "@testing-library/react": "^16.3.0",
    "@testing-library/jest-dom": "^6.9.1",
    "@testing-library/user-event": "^14.5.0",
    
    // E2E Testing
    "@playwright/test": "^1.56.0",
    
    // Visual Regression
    "@percy/cli": "^1.28.0",
    "@percy/playwright": "^1.0.0",
    
    // Performance Testing
    "@lhci/cli": "^0.13.0",
    "lighthouse": "^11.0.0",
    
    // Accessibility Testing
    "@axe-core/playwright": "^4.8.0",
    "pa11y": "^7.0.0",
    
    // SEO Testing
    "next-seo": "^6.5.0",
    "schema-dts": "^1.1.0",
    
    // Coverage & Reporting
    "@vitest/coverage-v8": "^3.2.4",
    "playwright-html-reporter": "^1.0.0"
  }
}
```

### Testing Utilities

```javascript
// test/helpers/setup.js
import { expect, afterEach } from 'vitest';
import { cleanup } from '@testing-library/react';
import matchers from '@testing-library/jest-dom/matchers';

expect.extend(matchers);

afterEach(() => {
  cleanup();
});
```

```javascript
// test/helpers/render.js
import { render } from '@testing-library/react';
import { ThemeProvider } from '../src/context/theme';

export function renderWithTheme(component) {
  return render(
    <ThemeProvider>
      {component}
    </ThemeProvider>
  );
}
```

```javascript
// test/helpers/analytics-mock.js
export function setupAnalyticsMock() {
  const events = [];
  
  window.gtag = (type, event, params) => {
    events.push({ event, ...params });
  };
  
  return { events };
}
```

### Recommended Tools by Category

#### Unit Testing
- **Vitest** - Fast, ESM-first test runner
- **Testing Library** - User-centric testing utilities
- **MSW** - Mock Service Worker for API mocking

#### E2E Testing
- **Playwright** - Cross-browser automation
- **Cypress** (Alternative) - Developer-friendly E2E

#### Visual Regression
- **Percy** - Visual diffing service
- **Chromatic** (Alternative) - Storybook integration
- **BackstopJS** (Self-hosted) - Open source option

#### Performance
- **Lighthouse CI** - Automated performance testing
- **WebPageTest** - Real-world performance testing
- **SpeedCurve** - Continuous monitoring

#### SEO
- **Google Search Console** - Production monitoring
- **Screaming Frog** - Site crawling and audits
- **Ahrefs Site Audit** - Comprehensive SEO analysis

#### Accessibility
- **axe DevTools** - Browser extension
- **WAVE** - Web accessibility evaluation tool
- **Pa11y** - Automated accessibility testing

#### Analytics
- **Google Tag Manager** - Tag management
- **GA4** - Event tracking and analytics
- **Hotjar** - User behavior heatmaps

---

## Test Implementation Examples

### Example 1: Newsletter Signup Form

```javascript
// test/newsletter.test.js
import { describe, test, expect, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';
import NewsletterForm from '../src/components/NewsletterForm';

const server = setupServer();

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Newsletter Form', () => {
  test('should submit valid email successfully', async () => {
    server.use(
      http.post('/api/newsletter', async ({ request }) => {
        const body = await request.json();
        expect(body.email).toBe('test@example.com');
        return HttpResponse.json({ success: true });
      })
    );
    
    render(<NewsletterForm />);
    const user = userEvent.setup();
    
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.click(screen.getByRole('button', { name: /subscribe/i }));
    
    await waitFor(() => {
      expect(screen.getByText(/thank you/i)).toBeInTheDocument();
    });
  });
  
  test('should show error for invalid email', async () => {
    render(<NewsletterForm />);
    const user = userEvent.setup();
    
    await user.type(screen.getByLabelText(/email/i), 'invalid-email');
    await user.click(screen.getByRole('button', { name: /subscribe/i }));
    
    expect(screen.getByText(/valid email/i)).toBeInTheDocument();
  });
  
  test('should handle API errors gracefully', async () => {
    server.use(
      http.post('/api/newsletter', () => {
        return HttpResponse.json(
          { error: 'Service unavailable' },
          { status: 500 }
        );
      })
    );
    
    render(<NewsletterForm />);
    const user = userEvent.setup();
    
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    await user.click(screen.getByRole('button', { name: /subscribe/i }));
    
    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/try again/i);
    });
  });
  
  test('should prevent duplicate submissions', async () => {
    let requestCount = 0;
    server.use(
      http.post('/api/newsletter', () => {
        requestCount++;
        return HttpResponse.json({ success: true });
      })
    );
    
    render(<NewsletterForm />);
    const user = userEvent.setup();
    
    await user.type(screen.getByLabelText(/email/i), 'test@example.com');
    const button = screen.getByRole('button', { name: /subscribe/i });
    
    await user.click(button);
    await user.click(button); // Try to submit twice
    
    await waitFor(() => {
      expect(requestCount).toBe(1);
    });
  });
});
```

### Example 2: Demo Request E2E Test

```javascript
// e2e/demo-request.spec.js
import { test, expect } from '@playwright/test';

test.describe('Demo Request Flow', () => {
  test('should complete full demo request journey', async ({ page }) => {
    // 1. Land on homepage
    await page.goto('/');
    await expect(page).toHaveTitle(/HazardHawk/);
    
    // 2. Navigate to demo page
    await page.click('text=Schedule Demo');
    await expect(page).toHaveURL(/.*demo/);
    
    // 3. Fill out form
    await page.fill('[name="firstName"]', 'John');
    await page.fill('[name="lastName"]', 'Doe');
    await page.fill('[name="email"]', 'john.doe@construction.com');
    await page.fill('[name="company"]', 'BuildRight Construction');
    await page.fill('[name="phone"]', '+1-555-123-4567');
    await page.selectOption('[name="companySize"]', '50-200');
    await page.fill('[name="message"]', 'Interested in enterprise plan');
    
    // 4. Submit form
    await page.click('button[type="submit"]');
    
    // 5. Verify success
    await expect(page.locator('[data-success]')).toBeVisible();
    await expect(page.locator('[data-success]')).toContainText(
      'We\'ll contact you within 24 hours'
    );
    
    // 6. Verify analytics event
    const analyticsEvents = await page.evaluate(() => {
      return window.dataLayer || [];
    });
    
    const demoEvent = analyticsEvents.find(e => e.event === 'form_submit');
    expect(demoEvent).toBeDefined();
    expect(demoEvent.form_name).toBe('demo_request');
  });
  
  test('should handle partial form completion', async ({ page }) => {
    await page.goto('/demo');
    
    // Fill only some fields
    await page.fill('[name="email"]', 'test@example.com');
    await page.fill('[name="firstName"]', 'John');
    
    // Try to submit
    await page.click('button[type="submit"]');
    
    // Should show validation errors
    await expect(page.locator('[data-error="lastName"]')).toBeVisible();
    await expect(page.locator('[data-error="company"]')).toBeVisible();
  });
  
  test('should save form progress in localStorage', async ({ page }) => {
    await page.goto('/demo');
    
    await page.fill('[name="email"]', 'test@example.com');
    await page.fill('[name="firstName"]', 'John');
    
    // Reload page
    await page.reload();
    
    // Form should be pre-filled
    expect(await page.inputValue('[name="email"]')).toBe('test@example.com');
    expect(await page.inputValue('[name="firstName"]')).toBe('John');
  });
});
```

### Example 3: Performance Test

```javascript
// test/performance.test.js
import { test, expect } from '@playwright/test';
import { playAudit } from 'playwright-lighthouse';

test.describe('Performance', () => {
  test('should meet Core Web Vitals targets', async ({ page, context }) => {
    await page.goto('/');
    
    // Measure Web Vitals
    const vitals = await page.evaluate(() => {
      return new Promise((resolve) => {
        const values = {};
        
        new PerformanceObserver((list) => {
          for (const entry of list.getEntries()) {
            values[entry.name] = entry.value;
          }
        }).observe({ entryTypes: ['largest-contentful-paint', 'layout-shift'] });
        
        // Wait for metrics to be collected
        setTimeout(() => resolve(values), 3000);
      });
    });
    
    expect(vitals['largest-contentful-paint']).toBeLessThan(2500);
  });
  
  test('should achieve Lighthouse scores', async ({ page, port }) => {
    await page.goto(`http://localhost:${port}/`);
    
    const audit = await playAudit({
      page,
      thresholds: {
        performance: 90,
        accessibility: 100,
        'best-practices': 90,
        seo: 100
      }
    });
    
    expect(audit.lhr.categories.performance.score).toBeGreaterThanOrEqual(0.9);
    expect(audit.lhr.categories.seo.score).toBe(1.0);
  });
  
  test('should load quickly on slow network', async ({ page, context }) => {
    // Simulate Slow 3G
    await context.route('**/*', route => {
      setTimeout(() => route.continue(), 400); // 400ms delay
    });
    
    const start = Date.now();
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    const duration = Date.now() - start;
    
    expect(duration).toBeLessThan(5000); // 5 seconds max
  });
});
```

### Example 4: SEO Validation Test

```javascript
// test/seo.test.js
import { test, expect } from '@playwright/test';

test.describe('SEO Optimization', () => {
  test('should have complete meta tags', async ({ page }) => {
    await page.goto('/');
    
    // Title
    const title = await page.title();
    expect(title).toBeTruthy();
    expect(title.length).toBeGreaterThan(30);
    expect(title.length).toBeLessThan(60);
    
    // Description
    const description = await page.getAttribute(
      'meta[name="description"]',
      'content'
    );
    expect(description).toBeTruthy();
    expect(description.length).toBeGreaterThan(120);
    expect(description.length).toBeLessThan(160);
    
    // Open Graph
    const ogTitle = await page.getAttribute('meta[property="og:title"]', 'content');
    const ogImage = await page.getAttribute('meta[property="og:image"]', 'content');
    expect(ogTitle).toBeTruthy();
    expect(ogImage).toMatch(/^https?:\/\//);
    
    // Canonical
    const canonical = await page.getAttribute('link[rel="canonical"]', 'href');
    expect(canonical).toBe('https://hazardhawk.com/');
  });
  
  test('should have valid structured data', async ({ page }) => {
    await page.goto('/');
    
    const jsonLd = await page.evaluate(() => {
      const script = document.querySelector('script[type="application/ld+json"]');
      return JSON.parse(script.textContent);
    });
    
    expect(jsonLd['@context']).toBe('https://schema.org');
    expect(jsonLd['@type']).toBe('Organization');
    expect(jsonLd.name).toBe('HazardHawk');
    expect(jsonLd.url).toBe('https://hazardhawk.com');
  });
  
  test('should have semantic HTML structure', async ({ page }) => {
    await page.goto('/');
    
    // Should have one h1
    const h1Count = await page.locator('h1').count();
    expect(h1Count).toBe(1);
    
    // Should have header, main, footer
    expect(await page.locator('header').count()).toBe(1);
    expect(await page.locator('main').count()).toBe(1);
    expect(await page.locator('footer').count()).toBe(1);
    
    // Images should have alt text
    const imagesWithoutAlt = await page.locator('img:not([alt])').count();
    expect(imagesWithoutAlt).toBe(0);
  });
});
```

### Example 5: Accessibility Test

```javascript
// test/accessibility.test.js
import { test, expect } from '@playwright/test';
import { injectAxe, checkA11y } from 'axe-playwright';

test.describe('Accessibility', () => {
  test('should pass axe accessibility checks', async ({ page }) => {
    await page.goto('/');
    await injectAxe(page);
    
    await checkA11y(page, null, {
      detailedReport: true,
      detailedReportOptions: {
        html: true
      }
    });
  });
  
  test('should be keyboard navigable', async ({ page }) => {
    await page.goto('/');
    
    // Tab through navigation
    await page.keyboard.press('Tab');
    let focused = await page.evaluate(() => document.activeElement.tagName);
    expect(focused).toBe('A'); // First link
    
    // Enter should activate links
    await page.keyboard.press('Enter');
    await page.waitForNavigation();
    expect(page.url()).not.toBe('/'); // Navigated away
  });
  
  test('should have proper focus indicators', async ({ page }) => {
    await page.goto('/');
    
    await page.keyboard.press('Tab');
    const outline = await page.evaluate(() => {
      const el = document.activeElement;
      const styles = window.getComputedStyle(el);
      return styles.outline + styles.outlineColor;
    });
    
    expect(outline).not.toContain('none');
  });
  
  test('should have ARIA landmarks', async ({ page }) => {
    await page.goto('/');
    
    expect(await page.locator('header[role="banner"]').count()).toBeGreaterThan(0);
    expect(await page.locator('main[role="main"]').count()).toBeGreaterThan(0);
    expect(await page.locator('footer[role="contentinfo"]').count()).toBeGreaterThan(0);
  });
});
```

---

## CI/CD Integration

### GitHub Actions Workflow

```yaml
# .github/workflows/test-suite.yml
name: Comprehensive Test Suite

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]

jobs:
  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run unit tests
        run: npm test -- --coverage
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./coverage/coverage-final.json
          flags: unittests
      
      - name: Comment PR with coverage
        uses: romeovs/lcov-reporter-action@v0.3.1
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          lcov-file: ./coverage/lcov.info

  e2e-tests:
    name: E2E Tests
    runs-on: ubuntu-latest
    strategy:
      matrix:
        browser: [chromium, firefox, webkit]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Install Playwright browsers
        run: npx playwright install --with-deps ${{ matrix.browser }}
      
      - name: Build application
        run: npm run build
      
      - name: Run E2E tests
        run: npx playwright test --project=${{ matrix.browser }}
      
      - name: Upload test results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report-${{ matrix.browser }}
          path: playwright-report/
          retention-days: 30

  lighthouse:
    name: Lighthouse CI
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build application
        run: npm run build
      
      - name: Run Lighthouse CI
        run: |
          npm install -g @lhci/cli
          lhci autorun
        env:
          LHCI_GITHUB_APP_TOKEN: ${{ secrets.LHCI_GITHUB_APP_TOKEN }}
      
      - name: Upload Lighthouse results
        uses: actions/upload-artifact@v3
        with:
          name: lighthouse-results
          path: .lighthouseci/

  visual-regression:
    name: Visual Regression
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run Percy
        run: npx percy exec -- npm run test:e2e
        env:
          PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}

  seo-audit:
    name: SEO Audit
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build application
        run: npm run build
      
      - name: Start server
        run: npm start &
      
      - name: Wait for server
        run: npx wait-on http://localhost:3000
      
      - name: Run SEO tests
        run: npm run test:seo

  accessibility:
    name: Accessibility Audit
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build application
        run: npm run build
      
      - name: Run Pa11y
        run: |
          npm start &
          npx wait-on http://localhost:3000
          npx pa11y-ci --config .pa11yci.json

  bundle-size:
    name: Bundle Size Check
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Check bundle size
        uses: andresz1/size-limit-action@v1
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}

  security-scan:
    name: Security Scan
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
          cache: 'npm'
      
      - name: Run npm audit
        run: npm audit --audit-level=moderate
      
      - name: Run Snyk scan
        uses: snyk/actions/node@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}

  quality-gate:
    name: Quality Gate
    runs-on: ubuntu-latest
    needs: [unit-tests, e2e-tests, lighthouse, accessibility]
    steps:
      - name: Check all tests passed
        run: echo "All quality gates passed!"
```

### Pre-commit Hooks

```javascript
// .husky/pre-commit
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

# Run lint
npm run lint

# Run type check
npm run type-check

# Run quick tests
npm test -- --run --changed

# Run accessibility check on changed files
npm run test:a11y:changed
```

---

## Maintenance & Monitoring

### Continuous Monitoring

```javascript
// monitoring/synthetic-tests.js
// Run these in production every 15 minutes

const tests = [
  {
    name: 'Homepage loads',
    url: 'https://hazardhawk.com',
    assertions: [
      { selector: 'h1', exists: true },
      { selector: '.hero .btn-primary', exists: true }
    ]
  },
  {
    name: 'Newsletter signup works',
    url: 'https://hazardhawk.com',
    actions: [
      { type: 'type', selector: '#email', value: 'monitor@hazardhawk.com' },
      { type: 'click', selector: 'button[type="submit"]' }
    ],
    assertions: [
      { selector: '[data-success]', exists: true }
    ]
  }
];

// Send results to monitoring service (DataDog, New Relic, etc.)
```

### Real User Monitoring (RUM)

```javascript
// Track actual user experiences
// Using Google Analytics 4 Web Vitals

import { onCLS, onFID, onLCP } from 'web-vitals';

function sendToAnalytics({ name, delta, id }) {
  gtag('event', name, {
    event_category: 'Web Vitals',
    value: Math.round(name === 'CLS' ? delta * 1000 : delta),
    event_label: id,
    non_interaction: true
  });
}

onCLS(sendToAnalytics);
onFID(sendToAnalytics);
onLCP(sendToAnalytics);
```

### Test Maintenance Schedule

| Task | Frequency | Owner |
|------|-----------|-------|
| Review flaky tests | Weekly | Test Guardian |
| Update test data | Bi-weekly | QA Team |
| Accessibility audit | Monthly | Accessibility Lead |
| Performance baseline update | Monthly | Performance Team |
| Browser compatibility check | Quarterly | QA Team |
| Test coverage review | Quarterly | Engineering Manager |
| Testing strategy review | Annually | Leadership |

### Flaky Test Management

```javascript
// playwright.config.js
export default {
  retries: process.env.CI ? 2 : 0,
  reporter: [
    ['html'],
    ['json', { outputFile: 'test-results.json' }],
    ['junit', { outputFile: 'junit.xml' }]
  ],
  
  // Track flaky tests
  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  }
};

// Script to identify flaky tests
// analyze-flaky-tests.js
const results = require('./test-results.json');

const flakyTests = results.suites
  .flatMap(s => s.specs)
  .filter(spec => {
    const runs = spec.tests[0].results;
    return runs.some(r => r.status === 'passed') && 
           runs.some(r => r.status === 'failed');
  });

console.log('Flaky tests detected:', flakyTests.length);
flakyTests.forEach(test => {
  console.log(`- ${test.title}`);
});
```

---

## Appendix: Quick Reference

### Commands Cheat Sheet

```bash
# Unit tests
npm test                          # Run all unit tests
npm test -- --watch              # Watch mode
npm test -- --coverage           # With coverage
npm test -- path/to/test.js      # Single file

# E2E tests
npm run test:e2e                 # All E2E tests
npm run test:e2e -- --headed    # With browser visible
npm run test:e2e -- --debug     # Debug mode
npm run test:e2e -- --project=chromium  # Single browser

# Performance
npm run lighthouse               # Run Lighthouse locally
npm run lighthouse:ci            # CI mode with assertions

# SEO
npm run test:seo                 # SEO validation tests

# Accessibility
npm run test:a11y                # Accessibility tests
npm run test:a11y:ci             # CI mode

# All quality gates
npm run test:all                 # Run everything
```

### Test Naming Conventions

```javascript
// Good test names
describe('Newsletter Form', () => {
  test('should submit valid email successfully', ...);
  test('should show error for invalid email format', ...);
  test('should prevent duplicate submissions', ...);
  test('should handle network errors gracefully', ...);
});

// Bad test names
describe('Form', () => {
  test('test1', ...);  // Not descriptive
  test('it works', ...);  // Too vague
  test('should test the form', ...);  // Redundant
});
```

### When to Update Tests

| Scenario | Action |
|----------|--------|
| Feature added | Write new tests FIRST (TDD) |
| Bug fixed | Add regression test |
| UI changed | Update E2E tests, visual snapshots |
| API changed | Update integration tests |
| Performance degraded | Add performance test |
| Accessibility issue | Add a11y test |

---

## Summary

This comprehensive testing strategy ensures the HazardHawk marketing website is:

1. **Reliable** - All user interactions work flawlessly across browsers
2. **Fast** - Optimized for construction site networks (slow 3G)
3. **Accessible** - Usable by everyone, including those with disabilities
4. **SEO-Optimized** - Ranks well in search results
5. **Conversion-Focused** - Maximizes trial signups and demo requests
6. **Maintainable** - Tests are clear, focused, and easy to update

### Next Steps

1. Set up testing infrastructure (Vitest, Playwright, Lighthouse CI)
2. Implement critical E2E tests for user journeys
3. Add performance monitoring and alerting
4. Integrate quality gates into CI/CD pipeline
5. Train team on testing best practices
6. Establish regular review cadence

### Success Metrics

Track these metrics to measure testing effectiveness:

- Test pass rate: >99%
- Coverage: >80%
- Lighthouse scores: Performance 90+, SEO 100, A11y 100
- Flaky test rate: <2%
- Time to detect bugs: <1 hour
- Production incidents: <1 per month

---

**Document Owner:** Test Guardian  
**Last Review:** 2025-10-09  
**Next Review:** 2025-11-09
