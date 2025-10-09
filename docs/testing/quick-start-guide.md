# Testing Quick Start Guide

Fast-track guide to implementing the comprehensive testing strategy for HazardHawk marketing website.

---

## 30-Minute Quick Start

### Step 1: Install Dependencies (5 min)

```bash
cd hazardhawk-web  # or website directory

# Core testing dependencies
npm install -D \
  vitest @vitest/ui @vitest/coverage-v8 \
  @testing-library/react @testing-library/jest-dom @testing-library/user-event \
  @playwright/test axe-playwright \
  @lhci/cli lighthouse \
  msw \
  husky lint-staged @commitlint/cli @commitlint/config-conventional

# Install Playwright browsers
npx playwright install --with-deps
```

### Step 2: Copy Configuration Files (5 min)

```bash
# Create test directory structure
mkdir -p test/helpers e2e/helpers

# Copy configurations from docs/testing/test-configurations-examples.md
# - vitest.config.ts
# - playwright.config.ts
# - lighthouserc.js
# - .pa11yci.json
# - .size-limit.json
```

### Step 3: Set Up Pre-commit Hooks (5 min)

```bash
# Initialize Husky
npx husky install
npm pkg set scripts.prepare="husky install"

# Create hooks
npx husky add .husky/pre-commit "npx lint-staged"
npx husky add .husky/commit-msg 'npx commitlint --edit "$1"'
npx husky add .husky/pre-push "npm test -- --run"

# Create .lintstagedrc.js and .commitlintrc.js
```

### Step 4: Write First Tests (10 min)

**Create test/unit/validation.test.ts:**

```typescript
import { describe, test, expect } from 'vitest';
import { validateEmail } from '@/lib/validation';

describe('Email Validation', () => {
  test('should accept valid email', () => {
    expect(validateEmail('test@example.com')).toBe(true);
  });
  
  test('should reject invalid email', () => {
    expect(validateEmail('not-an-email')).toBe(false);
  });
});
```

**Create e2e/homepage.spec.ts:**

```typescript
import { test, expect } from '@playwright/test';

test('homepage loads successfully', async ({ page }) => {
  await page.goto('/');
  await expect(page).toHaveTitle(/HazardHawk/);
  await expect(page.locator('h1')).toBeVisible();
});
```

### Step 5: Run Tests (5 min)

```bash
# Run unit tests
npm test

# Run E2E tests
npm run test:e2e

# Run Lighthouse
npm run lighthouse

# Run all tests
npm run test:all
```

---

## Implementation Checklist

### Phase 1: Foundation (Week 1)

- [ ] Install all dependencies
- [ ] Set up test configurations (Vitest, Playwright, Lighthouse)
- [ ] Create test directory structure
- [ ] Set up pre-commit hooks with Husky
- [ ] Configure lint-staged and commitlint
- [ ] Add test scripts to package.json
- [ ] Create test helper utilities
- [ ] Document testing conventions in README

### Phase 2: Critical Tests (Week 2)

- [ ] Write unit tests for validation functions
- [ ] Write unit tests for utility functions
- [ ] Write integration tests for newsletter signup
- [ ] Write integration tests for demo request form
- [ ] Write E2E test for homepage journey
- [ ] Write E2E test for demo request flow
- [ ] Write E2E test for mobile experience
- [ ] Achieve 50%+ test coverage

### Phase 3: SEO & Performance (Week 3)

- [ ] Configure Lighthouse CI
- [ ] Write SEO validation tests (meta tags, structured data)
- [ ] Write performance tests (Core Web Vitals)
- [ ] Set up bundle size monitoring
- [ ] Implement image optimization tests
- [ ] Configure caching strategy tests
- [ ] Set performance baselines
- [ ] Achieve Lighthouse scores: Perf 90+, SEO 100, A11y 100

### Phase 4: Accessibility & Cross-Browser (Week 4)

- [ ] Configure Pa11y for accessibility testing
- [ ] Write keyboard navigation tests
- [ ] Write ARIA label validation tests
- [ ] Write color contrast tests
- [ ] Configure multi-browser testing (Chrome, Firefox, Safari)
- [ ] Test on mobile devices (iOS, Android)
- [ ] Test on tablets
- [ ] Verify all critical paths work across browsers

### Phase 5: CI/CD Integration (Week 5)

- [ ] Create GitHub Actions workflow for unit tests
- [ ] Create GitHub Actions workflow for E2E tests
- [ ] Create GitHub Actions workflow for Lighthouse
- [ ] Create GitHub Actions workflow for accessibility
- [ ] Set up quality gate job
- [ ] Configure branch protection rules
- [ ] Set up test result artifacts
- [ ] Configure PR comments with test results

### Phase 6: Monitoring & Maintenance (Week 6)

- [ ] Set up synthetic monitoring (uptime checks)
- [ ] Configure performance monitoring
- [ ] Set up error tracking (Sentry)
- [ ] Create alerting for failed tests
- [ ] Set up visual regression testing (Percy)
- [ ] Document flaky test management process
- [ ] Create test maintenance schedule
- [ ] Train team on testing practices

---

## Priority Tests to Implement First

### Critical Path Tests (Implement Immediately)

1. **Homepage Loads**
   ```typescript
   test('homepage loads successfully', async ({ page }) => {
     await page.goto('/');
     await expect(page).toHaveTitle(/HazardHawk/);
     await expect(page.locator('.hero')).toBeVisible();
   });
   ```

2. **Newsletter Signup Works**
   ```typescript
   test('newsletter signup submits successfully', async ({ page }) => {
     await page.goto('/');
     await page.fill('#email', 'test@example.com');
     await page.click('button[type="submit"]');
     await expect(page.locator('[data-success]')).toBeVisible();
   });
   ```

3. **Demo Request Form Works**
   ```typescript
   test('demo request form submits', async ({ page }) => {
     await page.goto('/demo');
     // Fill form...
     await page.click('button[type="submit"]');
     await expect(page.locator('[data-success]')).toBeVisible();
   });
   ```

4. **SEO Meta Tags Present**
   ```typescript
   test('has required meta tags', async ({ page }) => {
     await page.goto('/');
     const title = await page.title();
     expect(title.length).toBeGreaterThan(30);
     
     const desc = await page.getAttribute('meta[name="description"]', 'content');
     expect(desc).toBeTruthy();
   });
   ```

5. **Mobile Responsive**
   ```typescript
   test('is mobile responsive', async ({ page }) => {
     await page.setViewportSize({ width: 375, height: 667 });
     await page.goto('/');
     await expect(page.locator('nav')).toBeVisible();
   });
   ```

---

## Common Commands

```bash
# Development
npm run dev                          # Start dev server
npm test -- --watch                  # Watch mode for unit tests
npm run test:e2e -- --ui             # E2E tests with UI

# Testing
npm test                             # Run unit tests
npm run test:coverage                # With coverage report
npm run test:e2e                     # Run all E2E tests
npm run test:e2e:chromium            # Run E2E in Chrome only
npm run test:e2e -- --headed         # Run E2E with browser visible
npm run test:e2e -- --debug          # Debug E2E tests

# Performance & SEO
npm run lighthouse                   # Run Lighthouse locally
npm run test:seo                     # SEO validation tests
npm run test:a11y                    # Accessibility tests

# CI/CD
npm run test:all                     # Run all tests
npm run test:ci                      # Run tests in CI mode

# Maintenance
npm run test:e2e -- --update-snapshots  # Update visual snapshots
```

---

## Troubleshooting

### Tests Failing Locally

**Problem:** Tests pass in CI but fail locally

**Solution:**
```bash
# Clear caches
rm -rf node_modules .next coverage test-results
npm install
npm run build
npm test
```

### Playwright Browser Issues

**Problem:** Playwright can't find browsers

**Solution:**
```bash
# Reinstall browsers
npx playwright install --with-deps

# Or specific browser
npx playwright install chromium
```

### Lighthouse Fails to Connect

**Problem:** Lighthouse CI can't connect to server

**Solution:**
```bash
# Ensure server is running
npm run build
npm start &

# Wait for server
npx wait-on http://localhost:3000

# Run Lighthouse
npm run lighthouse
```

### Coverage Not Generated

**Problem:** No coverage report

**Solution:**
```bash
# Ensure coverage is configured in vitest.config.ts
npm test -- --coverage --run

# Check coverage directory
ls -la coverage/
```

### Flaky E2E Tests

**Problem:** E2E tests randomly fail

**Solution:**
```typescript
// Add explicit waits
await page.waitForLoadState('networkidle');
await page.waitForSelector('[data-testid="component"]');

// Increase timeout
test.setTimeout(30000);

// Use retry
test.describe.configure({ retries: 2 });
```

---

## Team Best Practices

### 1. Test-Driven Development (TDD)

```bash
# Write test first
npm test -- --watch path/to/test.test.ts

# Implement feature
# Watch test pass
```

### 2. Commit Message Convention

```
feat: add newsletter signup form
fix: resolve mobile menu bug
test: add E2E test for demo request
docs: update testing guide
```

### 3. PR Workflow

1. Create feature branch
2. Write tests first
3. Implement feature
4. Run tests locally (`npm run test:all`)
5. Commit with conventional message
6. Push and create PR
7. Wait for CI to pass
8. Request review

### 4. Code Review Checklist

- [ ] All tests passing?
- [ ] Coverage maintained or increased?
- [ ] No new accessibility issues?
- [ ] Performance not degraded?
- [ ] Tests are clear and maintainable?

---

## Resources

### Documentation
- [Full Testing Strategy](./marketing-website-testing-strategy.md)
- [Configuration Examples](./test-configurations-examples.md)
- [CI/CD Workflows](./ci-cd-automation-workflows.md)

### External Resources
- [Vitest Documentation](https://vitest.dev/)
- [Playwright Documentation](https://playwright.dev/)
- [Testing Library](https://testing-library.com/)
- [Lighthouse CI](https://github.com/GoogleChrome/lighthouse-ci)
- [Web Vitals](https://web.dev/vitals/)

### Tools
- [Playwright Inspector](https://playwright.dev/docs/inspector)
- [Vitest UI](https://vitest.dev/guide/ui.html)
- [Chrome DevTools](https://developer.chrome.com/docs/devtools/)
- [Lighthouse CLI](https://github.com/GoogleChrome/lighthouse)

---

## Support

### Getting Help

1. **Check documentation** in `/docs/testing/`
2. **Search issues** on GitHub
3. **Ask team** in #testing channel
4. **Review examples** in test files

### Reporting Issues

```markdown
**Test Type:** Unit / E2E / Performance
**Browser:** Chrome 120 / Firefox 119 / Safari 17
**Environment:** Local / CI / Production

**Description:**
What is failing?

**Steps to Reproduce:**
1. Run `npm test`
2. See error

**Expected:** Should pass
**Actual:** Test fails with error X

**Logs:**
```
paste error logs
```
```

---

## Success Metrics

Track these metrics weekly:

| Metric | Target | Current |
|--------|--------|---------|
| Test Coverage | 80%+ | ___ |
| E2E Pass Rate | 100% | ___ |
| Lighthouse Performance | 90+ | ___ |
| Lighthouse SEO | 100 | ___ |
| Lighthouse A11y | 100 | ___ |
| CI Build Time | <10 min | ___ |
| Flaky Test Rate | <2% | ___ |

---

**Last Updated:** 2025-10-09  
**Quick Start Version:** 1.0  
**Next Review:** 2025-11-09
