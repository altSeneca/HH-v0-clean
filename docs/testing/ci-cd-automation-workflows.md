# CI/CD Testing Automation Workflows

Complete CI/CD pipeline configuration for automated testing of the HazardHawk marketing website.

---

## Table of Contents

1. [GitHub Actions Workflows](#github-actions-workflows)
2. [Quality Gate Configuration](#quality-gate-configuration)
3. [Pre-commit Hooks](#pre-commit-hooks)
4. [Monitoring & Alerting](#monitoring--alerting)
5. [Deployment Strategies](#deployment-strategies)

---

## GitHub Actions Workflows

### Main Test Suite Workflow

**File:** `.github/workflows/test-suite.yml`

```yaml
name: Test Suite

on:
  push:
    branches: [main, develop, 'feature/**']
  pull_request:
    branches: [main, develop]

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

env:
  NODE_VERSION: '18'
  PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD: '0'

jobs:
  # ============================================
  # Job 1: Unit Tests
  # ============================================
  unit-tests:
    name: Unit Tests
    runs-on: ubuntu-latest
    timeout-minutes: 10
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run unit tests
        run: npm test -- --coverage --run
      
      - name: Generate coverage report
        run: npm run test:coverage
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./coverage/coverage-final.json
          flags: unittests
          name: unit-tests
          fail_ci_if_error: true
      
      - name: Comment PR with coverage
        if: github.event_name == 'pull_request'
        uses: romeovs/lcov-reporter-action@v0.3.1
        with:
          github-token: ${{ secrets.GITHUB_TOKEN }}
          lcov-file: ./coverage/lcov.info
          pr-number: ${{ github.event.pull_request.number }}
      
      - name: Check coverage thresholds
        run: |
          COVERAGE=$(jq '.total.lines.pct' coverage/coverage-summary.json)
          if (( $(echo "$COVERAGE < 80" | bc -l) )); then
            echo "Coverage is below 80%: $COVERAGE%"
            exit 1
          fi
          echo "Coverage: $COVERAGE%"
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: unit-test-results
          path: |
            coverage/
            test-results/
          retention-days: 30

  # ============================================
  # Job 2: E2E Tests (Matrix Strategy)
  # ============================================
  e2e-tests:
    name: E2E Tests (${{ matrix.browser }})
    runs-on: ubuntu-latest
    timeout-minutes: 30
    
    strategy:
      fail-fast: false
      matrix:
        browser: [chromium, firefox, webkit]
        shard: [1, 2, 3]
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Install Playwright browsers
        run: npx playwright install --with-deps ${{ matrix.browser }}
      
      - name: Build application
        run: npm run build
        env:
          NEXT_PUBLIC_API_URL: http://localhost:3000
      
      - name: Run E2E tests
        run: npx playwright test --project=${{ matrix.browser }} --shard=${{ matrix.shard }}/${{ strategy.job-total }}
        env:
          CI: true
      
      - name: Upload Playwright report
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: playwright-report-${{ matrix.browser }}-shard-${{ matrix.shard }}
          path: playwright-report/
          retention-days: 30
      
      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results-${{ matrix.browser }}-shard-${{ matrix.shard }}
          path: test-results/
          retention-days: 30

  # ============================================
  # Job 3: Lighthouse Performance Audit
  # ============================================
  lighthouse:
    name: Lighthouse Performance Audit
    runs-on: ubuntu-latest
    timeout-minutes: 15
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build application
        run: npm run build
      
      - name: Start server
        run: npm start &
        env:
          PORT: 3000
      
      - name: Wait for server
        run: npx wait-on http://localhost:3000 --timeout 60000
      
      - name: Run Lighthouse CI
        run: |
          npm install -g @lhci/cli@0.13.x
          lhci autorun
        env:
          LHCI_GITHUB_APP_TOKEN: ${{ secrets.LHCI_GITHUB_APP_TOKEN }}
      
      - name: Upload Lighthouse results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: lighthouse-results
          path: .lighthouseci/
          retention-days: 30
      
      - name: Comment PR with Lighthouse scores
        if: github.event_name == 'pull_request'
        uses: treosh/lighthouse-ci-action@v9
        with:
          urls: |
            http://localhost:3000
            http://localhost:3000/features
            http://localhost:3000/pricing
          uploadArtifacts: true
          temporaryPublicStorage: true

  # ============================================
  # Job 4: Visual Regression Testing
  # ============================================
  visual-regression:
    name: Visual Regression Tests
    runs-on: ubuntu-latest
    timeout-minutes: 20
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build application
        run: npm run build
      
      - name: Run Percy visual tests
        run: npx percy exec -- npm run test:e2e:visual
        env:
          PERCY_TOKEN: ${{ secrets.PERCY_TOKEN }}
          PERCY_BRANCH: ${{ github.head_ref || github.ref_name }}
          PERCY_TARGET_BRANCH: ${{ github.base_ref || 'main' }}

  # ============================================
  # Job 5: SEO Audit
  # ============================================
  seo-audit:
    name: SEO Validation
    runs-on: ubuntu-latest
    timeout-minutes: 10
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
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
      
      - name: Validate sitemap
        run: |
          curl http://localhost:3000/sitemap.xml -o sitemap.xml
          xmllint --noout sitemap.xml
      
      - name: Validate robots.txt
        run: |
          curl http://localhost:3000/robots.txt -o robots.txt
          grep "Sitemap:" robots.txt

  # ============================================
  # Job 6: Accessibility Audit
  # ============================================
  accessibility:
    name: Accessibility Audit
    runs-on: ubuntu-latest
    timeout-minutes: 15
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build application
        run: npm run build
      
      - name: Start server
        run: npm start &
      
      - name: Wait for server
        run: npx wait-on http://localhost:3000
      
      - name: Run Pa11y accessibility tests
        run: npx pa11y-ci --config .pa11yci.json
      
      - name: Run axe-core tests
        run: npm run test:a11y
      
      - name: Upload accessibility report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: accessibility-report
          path: |
            pa11y-report/
            accessibility-report/
          retention-days: 30

  # ============================================
  # Job 7: Bundle Size Check
  # ============================================
  bundle-size:
    name: Bundle Size Analysis
    runs-on: ubuntu-latest
    timeout-minutes: 10
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build application
        run: npm run build
      
      - name: Analyze bundle size
        uses: andresz1/size-limit-action@v1
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Generate bundle analysis
        run: |
          npx next-bundle-analyzer
          echo "## Bundle Size Report" >> $GITHUB_STEP_SUMMARY
          cat .next/analyze/bundle-sizes.txt >> $GITHUB_STEP_SUMMARY

  # ============================================
  # Job 8: Security Scan
  # ============================================
  security:
    name: Security Scan
    runs-on: ubuntu-latest
    timeout-minutes: 10
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ env.NODE_VERSION }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run npm audit
        run: npm audit --audit-level=moderate
      
      - name: Run Snyk security scan
        uses: snyk/actions/node@master
        continue-on-error: true
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
        with:
          args: --severity-threshold=high
      
      - name: Upload Snyk report
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: snyk-report
          path: snyk-report.json
          retention-days: 30

  # ============================================
  # Job 9: Dependency Review (PRs only)
  # ============================================
  dependency-review:
    name: Dependency Review
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'
    timeout-minutes: 5
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Dependency Review
        uses: actions/dependency-review-action@v3
        with:
          fail-on-severity: moderate

  # ============================================
  # Job 10: Quality Gate Summary
  # ============================================
  quality-gate:
    name: Quality Gate
    runs-on: ubuntu-latest
    needs: 
      - unit-tests
      - e2e-tests
      - lighthouse
      - accessibility
      - bundle-size
      - security
    if: always()
    
    steps:
      - name: Check test results
        run: |
          echo "## Quality Gate Summary" >> $GITHUB_STEP_SUMMARY
          echo "" >> $GITHUB_STEP_SUMMARY
          
          if [[ "${{ needs.unit-tests.result }}" == "success" ]]; then
            echo "✅ Unit Tests: PASSED" >> $GITHUB_STEP_SUMMARY
          else
            echo "❌ Unit Tests: FAILED" >> $GITHUB_STEP_SUMMARY
          fi
          
          if [[ "${{ needs.e2e-tests.result }}" == "success" ]]; then
            echo "✅ E2E Tests: PASSED" >> $GITHUB_STEP_SUMMARY
          else
            echo "❌ E2E Tests: FAILED" >> $GITHUB_STEP_SUMMARY
          fi
          
          if [[ "${{ needs.lighthouse.result }}" == "success" ]]; then
            echo "✅ Lighthouse: PASSED" >> $GITHUB_STEP_SUMMARY
          else
            echo "❌ Lighthouse: FAILED" >> $GITHUB_STEP_SUMMARY
          fi
          
          if [[ "${{ needs.accessibility.result }}" == "success" ]]; then
            echo "✅ Accessibility: PASSED" >> $GITHUB_STEP_SUMMARY
          else
            echo "❌ Accessibility: FAILED" >> $GITHUB_STEP_SUMMARY
          fi
          
          if [[ "${{ needs.bundle-size.result }}" == "success" ]]; then
            echo "✅ Bundle Size: PASSED" >> $GITHUB_STEP_SUMMARY
          else
            echo "❌ Bundle Size: FAILED" >> $GITHUB_STEP_SUMMARY
          fi
          
          if [[ "${{ needs.security.result }}" == "success" ]]; then
            echo "✅ Security: PASSED" >> $GITHUB_STEP_SUMMARY
          else
            echo "⚠️ Security: WARNING" >> $GITHUB_STEP_SUMMARY
          fi
      
      - name: Fail if required checks failed
        if: |
          needs.unit-tests.result != 'success' ||
          needs.e2e-tests.result != 'success' ||
          needs.lighthouse.result != 'success' ||
          needs.accessibility.result != 'success'
        run: |
          echo "One or more required checks failed"
          exit 1
```

---

## Quality Gate Configuration

### Branch Protection Rules

**File:** `.github/settings.yml` (using Probot Settings app)

```yaml
repository:
  name: hazardhawk-marketing
  description: HazardHawk Marketing Website
  homepage: https://hazardhawk.com
  topics: construction, safety, marketing, nextjs
  private: false
  has_issues: true
  has_projects: true
  has_wiki: false
  has_downloads: true
  default_branch: main
  allow_squash_merge: true
  allow_merge_commit: false
  allow_rebase_merge: true
  delete_branch_on_merge: true

branches:
  - name: main
    protection:
      required_pull_request_reviews:
        required_approving_review_count: 1
        dismiss_stale_reviews: true
        require_code_owner_reviews: true
      required_status_checks:
        strict: true
        contexts:
          - "Unit Tests"
          - "E2E Tests (chromium)"
          - "E2E Tests (firefox)"
          - "E2E Tests (webkit)"
          - "Lighthouse Performance Audit"
          - "Accessibility Audit"
          - "Bundle Size Analysis"
          - "Quality Gate"
      enforce_admins: false
      required_linear_history: true
      restrictions: null

  - name: develop
    protection:
      required_pull_request_reviews:
        required_approving_review_count: 1
      required_status_checks:
        strict: false
        contexts:
          - "Unit Tests"
          - "E2E Tests (chromium)"
      enforce_admins: false
```

---

## Pre-commit Hooks

### Husky Configuration

**Install Husky:**

```bash
npm install -D husky lint-staged
npx husky install
```

**File:** `.husky/pre-commit`

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

echo "🔍 Running pre-commit checks..."

# Run lint-staged
npx lint-staged

# Type check
echo "📝 Type checking..."
npm run type-check

# Run quick tests on changed files
echo "🧪 Running quick tests..."
npm test -- --run --changed --bail

echo "✅ Pre-commit checks passed!"
```

**File:** `.husky/commit-msg`

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

# Validate commit message format
npx commitlint --edit "$1"
```

**File:** `.husky/pre-push`

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

echo "🚀 Running pre-push checks..."

# Run all tests
echo "🧪 Running full test suite..."
npm test -- --run

# Check bundle size
echo "📦 Checking bundle size..."
npm run build

echo "✅ Pre-push checks passed!"
```

### Lint-staged Configuration

**File:** `.lintstagedrc.js`

```javascript
module.exports = {
  '*.{js,jsx,ts,tsx}': [
    'eslint --fix',
    'prettier --write',
    () => 'npm run type-check'
  ],
  '*.{json,md,yml,yaml}': [
    'prettier --write'
  ],
  '*.{css,scss}': [
    'prettier --write'
  ],
  '*.test.{js,jsx,ts,tsx}': [
    'npm test -- --run --findRelatedTests'
  ]
};
```

### Commitlint Configuration

**File:** `.commitlintrc.js`

```javascript
module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      [
        'feat',     // New feature
        'fix',      // Bug fix
        'docs',     // Documentation
        'style',    // Formatting
        'refactor', // Code restructuring
        'perf',     // Performance improvement
        'test',     // Adding tests
        'chore',    // Maintenance
        'revert',   // Revert commit
        'ci',       // CI/CD changes
        'build'     // Build system changes
      ]
    ],
    'subject-case': [2, 'always', 'sentence-case']
  }
};
```

---

## Monitoring & Alerting

### Synthetic Monitoring

**File:** `.github/workflows/synthetic-monitoring.yml`

```yaml
name: Synthetic Monitoring

on:
  schedule:
    # Run every 15 minutes
    - cron: '*/15 * * * *'
  workflow_dispatch:

jobs:
  uptime-check:
    name: Uptime & Performance Check
    runs-on: ubuntu-latest
    
    steps:
      - name: Check homepage availability
        run: |
          RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" https://hazardhawk.com)
          if [ $RESPONSE -ne 200 ]; then
            echo "❌ Homepage is down! Status: $RESPONSE"
            exit 1
          fi
          echo "✅ Homepage is up (Status: $RESPONSE)"
      
      - name: Check response time
        run: |
          TIME=$(curl -s -o /dev/null -w "%{time_total}" https://hazardhawk.com)
          if (( $(echo "$TIME > 3.0" | bc -l) )); then
            echo "⚠️ Slow response time: ${TIME}s"
            exit 1
          fi
          echo "✅ Response time: ${TIME}s"
      
      - name: Notify on failure
        if: failure()
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Production website is down or slow!'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### Performance Monitoring

**File:** `.github/workflows/performance-monitoring.yml`

```yaml
name: Performance Monitoring

on:
  schedule:
    # Run daily at 2 AM
    - cron: '0 2 * * *'
  workflow_dispatch:

jobs:
  lighthouse-production:
    name: Production Lighthouse Audit
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Run Lighthouse
        uses: treosh/lighthouse-ci-action@v9
        with:
          urls: |
            https://hazardhawk.com
            https://hazardhawk.com/features
            https://hazardhawk.com/pricing
          uploadArtifacts: true
          temporaryPublicStorage: true
      
      - name: Check performance thresholds
        run: |
          # Parse Lighthouse results and alert if below threshold
          PERF_SCORE=$(jq '.categories.performance.score' lhci_reports/manifest.json)
          if (( $(echo "$PERF_SCORE < 0.9" | bc -l) )); then
            echo "⚠️ Performance degraded: ${PERF_SCORE}"
            # Send alert
          fi
```

### Error Tracking Integration

**File:** `src/lib/monitoring.ts`

```typescript
// Sentry integration
import * as Sentry from '@sentry/nextjs';

Sentry.init({
  dsn: process.env.NEXT_PUBLIC_SENTRY_DSN,
  environment: process.env.NODE_ENV,
  tracesSampleRate: 0.1,
  beforeSend(event) {
    // Don't send events in development
    if (process.env.NODE_ENV === 'development') {
      return null;
    }
    return event;
  }
});

// Custom error tracking
export function trackError(error: Error, context?: Record<string, any>) {
  Sentry.captureException(error, {
    extra: context
  });
}

// Performance monitoring
export function trackWebVitals(metric: any) {
  Sentry.captureMessage(`Web Vital: ${metric.name}`, {
    level: 'info',
    extra: {
      value: metric.value,
      id: metric.id
    }
  });
}
```

---

## Deployment Strategies

### Vercel Deployment

**File:** `vercel.json`

```json
{
  "buildCommand": "npm run build",
  "devCommand": "npm run dev",
  "installCommand": "npm ci",
  "framework": "nextjs",
  "regions": ["iad1"],
  "env": {
    "NEXT_PUBLIC_API_URL": "@api-url"
  },
  "build": {
    "env": {
      "NODE_ENV": "production"
    }
  },
  "headers": [
    {
      "source": "/(.*)",
      "headers": [
        {
          "key": "X-Content-Type-Options",
          "value": "nosniff"
        },
        {
          "key": "X-Frame-Options",
          "value": "DENY"
        },
        {
          "key": "X-XSS-Protection",
          "value": "1; mode=block"
        },
        {
          "key": "Referrer-Policy",
          "value": "origin-when-cross-origin"
        }
      ]
    },
    {
      "source": "/static/(.*)",
      "headers": [
        {
          "key": "Cache-Control",
          "value": "public, max-age=31536000, immutable"
        }
      ]
    }
  ],
  "redirects": [
    {
      "source": "/home",
      "destination": "/",
      "permanent": true
    }
  ]
}
```

### Deployment Workflow

**File:** `.github/workflows/deploy.yml`

```yaml
name: Deploy to Production

on:
  push:
    branches: [main]
  workflow_dispatch:

jobs:
  deploy:
    name: Deploy to Vercel
    runs-on: ubuntu-latest
    environment: production
    
    steps:
      - name: Checkout code
        uses: actions/checkout@v4
      
      - name: Deploy to Vercel
        uses: amondnet/vercel-action@v25
        with:
          vercel-token: ${{ secrets.VERCEL_TOKEN }}
          vercel-org-id: ${{ secrets.VERCEL_ORG_ID }}
          vercel-project-id: ${{ secrets.VERCEL_PROJECT_ID }}
          vercel-args: '--prod'
      
      - name: Run smoke tests
        run: |
          sleep 30  # Wait for deployment
          curl -f https://hazardhawk.com || exit 1
      
      - name: Notify deployment
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Deployed to production: https://hazardhawk.com'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

---

## Summary

This CI/CD setup provides:

1. ✅ **Comprehensive Testing** - Unit, E2E, Performance, A11y, SEO
2. ✅ **Quality Gates** - Automated checks before merge
3. ✅ **Pre-commit Hooks** - Fast feedback during development
4. ✅ **Monitoring** - Continuous production monitoring
5. ✅ **Automated Deployment** - Safe, tested releases

### Setup Instructions

```bash
# 1. Install Husky
npm install -D husky lint-staged @commitlint/cli @commitlint/config-conventional
npx husky install

# 2. Create pre-commit hooks
npx husky add .husky/pre-commit "npx lint-staged"
npx husky add .husky/commit-msg 'npx commitlint --edit "$1"'
npx husky add .husky/pre-push "npm test -- --run"

# 3. Copy GitHub Actions workflows
mkdir -p .github/workflows
cp docs/testing/ci-cd/test-suite.yml .github/workflows/

# 4. Configure secrets in GitHub
# - LHCI_GITHUB_APP_TOKEN
# - PERCY_TOKEN
# - SNYK_TOKEN
# - VERCEL_TOKEN
# - SLACK_WEBHOOK
```

### Best Practices

1. **Run tests locally** before pushing
2. **Use conventional commits** for clear history
3. **Monitor CI/CD metrics** (duration, flakiness)
4. **Keep workflows fast** (<10 min total)
5. **Review failed tests** immediately

---

**Last Updated:** 2025-10-09  
**Maintained By:** Test Guardian
