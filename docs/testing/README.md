# HazardHawk Marketing Website - Testing Documentation

Comprehensive testing strategy, configurations, and guides for ensuring quality, performance, and reliability.

---

## Documentation Index

### Core Documents

1. **[Marketing Website Testing Strategy](./marketing-website-testing-strategy.md)** - START HERE
   - Complete testing philosophy and approach
   - Test coverage requirements (Unit, Integration, E2E)
   - SEO testing strategy
   - Conversion and analytics tracking
   - Performance testing with Core Web Vitals
   - Edge cases and cross-browser compatibility
   - Quality gates and success metrics

2. **[Test Configurations & Examples](./test-configurations-examples.md)**
   - Vitest configuration
   - Playwright configuration
   - Lighthouse CI setup
   - Ready-to-use test examples
   - Helper utilities and fixtures
   - Package.json scripts

3. **[CI/CD Automation Workflows](./ci-cd-automation-workflows.md)**
   - GitHub Actions workflows
   - Quality gate configuration
   - Pre-commit hooks (Husky)
   - Monitoring and alerting
   - Deployment strategies

4. **[Quick Start Guide](./quick-start-guide.md)** - 30-MINUTE SETUP
   - Fast-track implementation
   - Priority tests to implement first
   - Common commands
   - Troubleshooting guide
   - Team best practices

---

## Quick Links

### For Developers
- [30-Minute Quick Start](./quick-start-guide.md#30-minute-quick-start)
- [Common Commands](./quick-start-guide.md#common-commands)
- [Priority Tests](./quick-start-guide.md#priority-tests-to-implement-first)
- [Troubleshooting](./quick-start-guide.md#troubleshooting)

### For Test Engineers
- [Test Strategy Overview](./marketing-website-testing-strategy.md#testing-philosophy)
- [Test Pyramid](./marketing-website-testing-strategy.md#test-pyramid-strategy)
- [Example Tests](./test-configurations-examples.md#example-test-files)
- [Quality Gates](./marketing-website-testing-strategy.md#quality-gates)

### For DevOps
- [CI/CD Workflows](./ci-cd-automation-workflows.md#github-actions-workflows)
- [Branch Protection](./ci-cd-automation-workflows.md#quality-gate-configuration)
- [Monitoring Setup](./ci-cd-automation-workflows.md#monitoring--alerting)
- [Deployment Pipeline](./ci-cd-automation-workflows.md#deployment-strategies)

### For Managers
- [Success Metrics](./marketing-website-testing-strategy.md#key-metrics-targets)
- [Implementation Checklist](./quick-start-guide.md#implementation-checklist)
- [Quality Standards](./marketing-website-testing-strategy.md#quality-gates)

---

## Getting Started

### Option 1: Quick Start (30 minutes)

Perfect for getting tests running quickly:

```bash
# 1. Install dependencies
npm install -D vitest @testing-library/react @playwright/test

# 2. Copy config files
cp docs/testing/examples/vitest.config.ts .
cp docs/testing/examples/playwright.config.ts .

# 3. Write first test
cat > test/example.test.ts << 'EOF'
import { test, expect } from 'vitest';
test('example', () => {
  expect(1 + 1).toBe(2);
});
EOF

# 4. Run tests
npm test
```

Follow the [Full Quick Start Guide](./quick-start-guide.md)

### Option 2: Comprehensive Setup (6 weeks)

Follow the complete implementation plan:

- **Week 1:** Foundation (configs, dependencies, hooks)
- **Week 2:** Critical tests (unit, integration, E2E)
- **Week 3:** SEO & performance tests
- **Week 4:** Accessibility & cross-browser
- **Week 5:** CI/CD integration
- **Week 6:** Monitoring & maintenance

View the [Implementation Checklist](./quick-start-guide.md#implementation-checklist)

---

## Testing Philosophy: Simple, Loveable, Complete

### Simple
- Tests are easy to write, read, and maintain
- Clear naming conventions
- Focused test cases: one behavior per test
- Fast feedback: Unit tests <1s, E2E <2min

### Loveable
- Tests provide confidence, not friction
- Clear failure messages guide debugging
- Visual feedback with screenshots
- Tests document expected behavior

### Complete
- Critical path coverage: 100%
- Overall coverage: 80%+
- Edge cases: Network failures, browser quirks, mobile
- Real-world scenarios: Construction site conditions

---

## Test Types & Coverage

Test Pyramid:
- E2E Tests (10%): Critical user journeys, cross-browser validation
- Integration Tests (20%): API interactions, form submissions
- Unit Tests (70%): Utility functions, business logic, validation

---

## Quality Gates

All tests must pass before merging to main:

- Unit Tests: 100% pass (Critical)
- E2E Tests: 100% pass (Critical)
- Coverage: 80%+ (Critical)
- Lighthouse Performance: 90+ (Critical)
- Lighthouse SEO: 100 (Critical)
- Lighthouse Accessibility: 100 (Critical)
- Bundle Size: <200KB JS (High)
- Security Scan: 0 high/critical (High)

---

## Testing Stack

### Core Tools

- **Vitest** - Unit testing
- **Playwright** - E2E testing
- **Testing Library** - React component testing
- **Lighthouse CI** - Performance testing
- **axe-core** - Accessibility testing
- **MSW** - API mocking

### Additional Tools

- **Percy** - Visual regression testing
- **Pa11y** - Automated accessibility checks
- **Husky** - Git hooks
- **Codecov** - Coverage reporting
- **Snyk** - Security scanning

---

## Browser & Device Support

### Tested Browsers

- Chrome 90+ (Desktop & Mobile)
- Firefox 88+ (Desktop)
- Safari 14+ (Desktop & iOS)
- Edge 90+ (Desktop)

### Tested Devices

- Desktop (1920x1080, 1366x768)
- Mobile (iPhone 13, Pixel 5)
- Tablet (iPad Pro)

### Special Considerations

- Construction site conditions (slow 3G)
- Large touch targets for gloves
- High contrast for outdoor visibility
- Offline capability

---

## Quick Commands

```bash
# Run all tests
npm run test:all

# Run specific test types
npm test                    # Unit tests
npm run test:e2e           # E2E tests
npm run lighthouse         # Performance
npm run test:a11y          # Accessibility

# Development
npm test -- --watch        # Watch mode
npm run test:e2e -- --ui   # E2E with UI
npm run test:coverage      # With coverage

# Maintenance
npm run test:e2e -- --update-snapshots  # Update visuals
npm audit                                # Security scan
npm run type-check                       # TypeScript check
```

---

## Support

### Getting Help

1. Check this documentation
2. Search existing GitHub issues
3. Ask in #testing Slack channel
4. Review test examples in codebase

### Reporting Issues

Include: Test Type, Browser, Environment, Description, Steps to Reproduce, Expected vs Actual, Logs

---

## Quick Wins

Start with these high-impact, low-effort tests:

1. Homepage loads test (5 min)
2. Newsletter signup test (10 min)
3. SEO meta tags test (10 min)
4. Mobile responsive test (10 min)
5. Lighthouse performance test (15 min)

**Total: 50 minutes for 80% of value!**

---

## Remember

> "Tests are not about finding bugs. Tests are about preventing bugs, documenting behavior, and enabling confident refactoring."

Good tests make development faster, not slower. Invest time upfront for massive time savings later.

---

**Last Updated:** 2025-10-09  
**Status:** Active  
**Owner:** Test Guardian  
**Next Review:** 2025-11-09

**Need help?** Start with the [Quick Start Guide](./quick-start-guide.md)
