# HazardHawk Production Deployment Guide

## Overview

This guide provides comprehensive instructions for deploying the HazardHawk v3 Gemini Vision API integration to production with monitoring, cost management, and rollback capabilities.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Environment Setup](#environment-setup)
- [Production Configuration](#production-configuration)
- [Feature Flags Configuration](#feature-flags-configuration)
- [Monitoring Setup](#monitoring-setup)
- [Cost Management](#cost-management)
- [Rollback Procedures](#rollback-procedures)
- [Build and Deployment](#build-and-deployment)
- [Post-Deployment Verification](#post-deployment-verification)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Credentials and Keys
- [ ] Google Cloud Project with Gemini API access
- [ ] Gemini Vision API key (production)
- [ ] Firebase project (production)
- [ ] App Store/Play Store certificates
- [ ] Slack/webhook URL for alerts (optional)

### Required Tools
- [ ] Android Studio Arctic Fox or newer
- [ ] Kotlin 1.9.22+
- [ ] Gradle 8.5+
- [ ] Git access to repository

### System Requirements
- [ ] Minimum 8GB RAM for build process
- [ ] 10GB available disk space
- [ ] Network access for API calls

## Environment Setup

### 1. Production Environment Configuration

First, initialize the production configuration in your application:

```kotlin
// In your Application class or initialization code
val productionConfig = ProductionConfig()
productionConfig.initialize(
    environment = Environment.PRODUCTION,
    customConfig = EnvironmentConfig.production().copy(
        apiConfig = ApiConfiguration.production().copy(
            geminiApiKey = "YOUR_PRODUCTION_GEMINI_API_KEY",
            backendBaseUrl = "https://api.hazardhawk.com"
        ),
        securityConfig = SecurityConfiguration.production().copy(
            encryptionKey = "YOUR_PRODUCTION_ENCRYPTION_KEY"
        )
    )
)
```

### 2. Secure Key Management

**IMPORTANT**: Never hardcode production keys in source code.

#### Option A: Environment Variables (Recommended)
```bash
export HAZARDHAWK_GEMINI_API_KEY="your-production-api-key"
export HAZARDHAWK_ENCRYPTION_KEY="your-encryption-key"
export HAZARDHAWK_WEBHOOK_URL="your-slack-webhook-url"
```

#### Option B: Android Keystore (Alternative)
Store sensitive keys in Android Keystore and retrieve at runtime.

#### Option C: Build-time Injection
Add to `local.properties` (not committed to git):
```properties
hazardhawk.gemini.api.key=your-production-api-key
hazardhawk.encryption.key=your-encryption-key
```

## Production Configuration

### 1. Gemini API Configuration

```kotlin
// Production settings for Gemini integration
val geminiConfig = GeminiConfig(
    apiKey = getSecureApiKey(), // Retrieved from secure storage
    modelVersion = "gemini-2.0-flash-exp",
    enabled = false, // Start disabled, enable via feature flags
    rateLimitPerMinute = 60,
    maxRetries = 3,
    timeoutMs = 30000L
)
```

### 2. Cost Management Configuration

```kotlin
val costConfig = CostConfiguration(
    dailyBudgetUsd = 100.0,
    monthlyBudgetUsd = 2500.0,
    budgetAlertThreshold = 0.8,
    emergencyStopThreshold = 0.95,
    enableCostOptimization = true,
    preferLocalAnalysis = false
)

val costMonitoring = CostMonitoringConfig()
costMonitoring.initialize(
    config = costConfig,
    costCalculator = DefaultCostCalculator(),
    alertHandler = ProductionCostAlertHandler()
)
```

### 3. Monitoring Configuration

```kotlin
val monitoringConfig = MonitoringConfiguration(
    enableAnalytics = true,
    enableCrashReporting = true,
    enablePerformanceMonitoring = true,
    enableCostTracking = true,
    alertingEnabled = true,
    alertWebhookUrl = "YOUR_SLACK_WEBHOOK_URL",
    metricsCollectionIntervalMs = 60000L
)

val monitoring = MonitoringConfig()
monitoring.initialize(
    config = monitoringConfig,
    alertHandler = WebhookAlertHandler("YOUR_WEBHOOK_URL")
)
```

## Feature Flags Configuration

### 1. Initialize Feature Flags

```kotlin
val featureFlags = FeatureFlags()
featureFlags.initialize(DefaultUserSegmentProvider())

// Set production-safe initial state
val productionFlags = FeatureFlagPresets.productionFlags()
productionFlags.forEach { (name, flag) ->
    featureFlags.updateFlag(name, flag)
}
```

### 2. Key Feature Flags for Production

| Flag Name | Initial Value | Rollout Strategy |
|-----------|---------------|------------------|
| `enable_gemini_integration` | `false` | Start at 0%, gradually increase to 10% over 1 week |
| `force_local_analysis` | `true` | Emergency fallback, keep at 0% normally |
| `enable_cost_optimization` | `true` | 100% enabled |
| `enable_realtime_monitoring` | `true` | 100% enabled |
| `beta_features` | `false` | 5% for beta testers only |

### 3. Safe Rollout Schedule

**Week 1: Limited Beta (5% of beta users)**
```kotlin
featureFlags.updateFlag("enable_gemini_integration", 
    FeatureFlag(
        enabled = true,
        rolloutPercentage = 5.0f,
        targetSegments = listOf(UserSegment.BETA_TESTERS)
    )
)
```

**Week 2-3: Gradual Expansion (10% → 25%)**
```kotlin
// Monitor metrics and gradually increase
featureFlags.graduateRollout("enable_gemini_integration", 10.0f)
// Later...
featureFlags.graduateRollout("enable_gemini_integration", 25.0f)
```

**Week 4+: Full Production (100%)**
```kotlin
// Only after confirming stability
featureFlags.graduateRollout("enable_gemini_integration", 100.0f)
```

## Monitoring Setup

### 1. Health Monitoring

The system automatically monitors:
- API error rates (threshold: 5%)
- Response latency (threshold: 5 seconds)
- Cost per hour (threshold: $20)
- API availability (threshold: 95%)

### 2. Custom Alerts

```kotlin
// Add custom monitoring thresholds
monitoring.createAlert(
    name = "High Analysis Volume",
    metricName = "api.gemini.calls_per_minute",
    threshold = 50.0,
    condition = AlertCondition.GREATER_THAN,
    severity = Severity.WARNING,
    description = "Unusually high analysis volume detected"
)
```

### 3. Dashboard Metrics

Key metrics to monitor on your dashboard:
- Total API calls per hour/day
- Error rate percentage
- Average response time
- Cost per analysis
- User satisfaction (via feedback)
- Feature flag rollout percentages

## Cost Management

### 1. Budget Configuration

Set up budgets for different scopes:

```kotlin
// Monthly total budget
costMonitoring.createBudget(
    name = "Production Monthly Total",
    limitUsd = 2500.0,
    startDate = monthStart,
    endDate = monthEnd,
    service = null, // All services
    enforced = true
)

// Gemini-specific budget
costMonitoring.createBudget(
    name = "Gemini API Budget",
    limitUsd = 2000.0,
    startDate = monthStart,
    endDate = monthEnd,
    service = AIServiceType.GEMINI,
    enforced = true
)
```

### 2. Cost Optimization

Enable automatic cost optimization:

```kotlin
val optimization = costMonitoring.getOptimizationSuggestions()
optimization.forEach { suggestion ->
    when (suggestion.type) {
        OptimizationType.ENABLE_LOCAL_ANALYSIS -> {
            // Automatically switch to local analysis if costs are high
            if (suggestion.potentialSavingsUsd > 100.0) {
                featureFlags.updateFlag("force_local_analysis", 
                    FeatureFlag(enabled = true, rolloutPercentage = 100.0f)
                )
            }
        }
        OptimizationType.BATCH_PROCESSING -> {
            // Enable batching for cost savings
            featureFlags.updateFlag("enable_batch_analysis",
                FeatureFlag(enabled = true, rolloutPercentage = 100.0f)
            )
        }
    }
}
```

## Rollback Procedures

### 1. Automated Rollback Triggers

The system will automatically rollback if:
- Error rate exceeds 10%
- API latency exceeds 10 seconds
- Hourly cost exceeds $50
- API availability drops below 50%

### 2. Manual Rollback

```kotlin
val rollbackProcedures = RollbackProcedures()

// Emergency rollback
val result = rollbackProcedures.executeEmergencyRollback(
    trigger = RollbackTrigger.HIGH_ERROR_RATE,
    reason = "Production issues detected",
    severity = IncidentSeverity.CRITICAL,
    affectedSystems = listOf("gemini_integration")
)
```

### 3. Gradual Rollback

```kotlin
// Gradual rollback for planned changes
val result = rollbackProcedures.executeGradualRollback(
    targetFeatures = listOf("enable_gemini_integration"),
    rollbackPercentage = 0.0f, // Roll back to 0%
    reason = "Planned maintenance"
)
```

## Build and Deployment

### 1. Production Build Commands

```bash
# Clean build
./gradlew clean

# Build production APK
./gradlew assembleProductionStandardRelease

# Build production AAB (for Play Store)
./gradlew bundleProductionStandardRelease

# Run tests before deployment
./gradlew testProductionStandardReleaseUnitTest
./gradlew connectedProductionStandardDebugAndroidTest
```

### 2. Build Variants

The system supports multiple build variants:

| Variant | Description | Use Case |
|---------|-------------|----------|
| `developmentStandardDebug` | Dev environment, all features enabled | Development testing |
| `stagingStandardDebug` | Staging environment, production-like | Pre-production testing |
| `productionStandardRelease` | Production environment, conservative settings | App Store release |
| `productionEnterpriseRelease` | Production with enterprise features | Enterprise customers |

### 3. Deployment Checklist

Before deploying to production:

- [ ] All unit tests pass
- [ ] Integration tests pass
- [ ] Feature flags configured correctly
- [ ] Monitoring and alerting tested
- [ ] Cost budgets set up
- [ ] Rollback procedures tested
- [ ] API keys configured in secure storage
- [ ] Certificates and signing configured
- [ ] Crash reporting enabled
- [ ] Analytics configured

## Post-Deployment Verification

### 1. Immediate Checks (First 30 minutes)

```bash
# Check system status
adb shell am broadcast -a com.hazardhawk.CHECK_SYSTEM_STATUS

# Monitor logs
adb logcat | grep -i "hazardhawk\|gemini\|error"
```

Verify:
- [ ] App starts successfully
- [ ] Feature flags loading correctly
- [ ] Local analysis working
- [ ] Cost monitoring active
- [ ] Monitoring metrics being collected

### 2. Extended Monitoring (First 24 hours)

Monitor dashboard for:
- [ ] No unusual error spikes
- [ ] Response times within normal range
- [ ] Cost tracking functioning
- [ ] User feedback positive
- [ ] No crashes reported

### 3. Weekly Reviews

- [ ] Review feature flag rollout progress
- [ ] Analyze cost trends
- [ ] Review any incidents or alerts
- [ ] Update rollout percentages if metrics are good
- [ ] Plan next phase of rollout

## Troubleshooting

### Common Issues and Solutions

#### 1. High API Error Rates

**Symptoms**: Error rate > 5%, user complaints about analysis failures

**Investigation**:
```kotlin
val metrics = monitoring.getMetrics("api.gemini")
val errorBreakdown = monitoring.getMetrics("error_type")
```

**Solutions**:
- Check API key validity
- Verify network connectivity
- Review rate limiting
- Enable local fallback temporarily

#### 2. High Costs

**Symptoms**: Daily budget alerts, rapid cost increases

**Investigation**:
```kotlin
val spending = costMonitoring.getSpending(TimeRange.Last24Hours)
val suggestions = costMonitoring.getOptimizationSuggestions()
```

**Solutions**:
- Enable local analysis fallback
- Implement request batching
- Review usage patterns
- Adjust image compression settings

#### 3. Performance Issues

**Symptoms**: Slow response times, user complaints about app speed

**Investigation**:
```kotlin
val performance = monitoring.getMetrics("performance")
val latency = monitoring.getMetrics("response_time_ms")
```

**Solutions**:
- Check API endpoint health
- Optimize image processing
- Enable caching
- Reduce concurrent requests

### Emergency Contacts

- **Engineering Lead**: [Contact Information]
- **DevOps Team**: [Contact Information]
- **Product Manager**: [Contact Information]
- **On-call Engineer**: [Contact Information]

### Support Resources

- **Monitoring Dashboard**: [Dashboard URL]
- **Logs**: [Log Management URL]
- **Incident Management**: [Incident Tracking URL]
- **API Documentation**: [API Docs URL]

## Security Considerations

### Production Security Checklist

- [ ] API keys stored in secure key management
- [ ] Certificate pinning enabled
- [ ] TLS 1.3 enforced
- [ ] Network security config applied
- [ ] ProGuard/R8 obfuscation enabled
- [ ] Debug logging disabled
- [ ] App attestation enabled (if available)

### Data Protection

- [ ] User data encrypted at rest
- [ ] API communications encrypted in transit
- [ ] No sensitive data in logs
- [ ] GDPR/privacy compliance verified
- [ ] Data retention policies configured

## Maintenance and Updates

### Regular Maintenance Tasks

**Weekly**:
- Review monitoring metrics and alerts
- Check cost trends and budget utilization
- Update feature flag rollout percentages
- Review any incidents or issues

**Monthly**:
- Review and update cost budgets
- Analyze usage patterns and optimization opportunities
- Update rollback procedures based on learnings
- Security review and key rotation

**Quarterly**:
- Full system security audit
- Performance optimization review
- Update monitoring thresholds based on historical data
- Plan feature flag cleanup (remove unused flags)

---

## Appendix

### A. Configuration File Locations

```
shared/src/commonMain/kotlin/com/hazardhawk/ai/
├── ProductionConfig.kt
├── FeatureFlags.kt
├── MonitoringConfig.kt
├── CostMonitoringConfig.kt
└── RollbackProcedures.kt
```

### B. Build Configuration

```
androidApp/
├── build.gradle.kts (updated with build variants)
└── src/
    ├── productionStandardRelease/
    ├── stagingStandardDebug/
    └── developmentStandardDebug/
```

### C. Monitoring Endpoints

- Health Check: `/health`
- Metrics: `/metrics`
- Cost Summary: `/cost-summary`
- Feature Flags: `/feature-flags`

This deployment guide ensures a safe, monitored, and cost-effective production deployment of the Gemini Vision API integration with comprehensive rollback capabilities and monitoring systems.