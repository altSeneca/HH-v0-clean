# HazardHawk LiteRT Production Deployment Runbook

## Overview

This runbook provides comprehensive procedures for safely deploying, monitoring, and managing HazardHawk's LiteRT integration in production environments.

## Table of Contents

1. [Pre-Deployment Checklist](#pre-deployment-checklist)
2. [Deployment Procedures](#deployment-procedures)
3. [Feature Flag Rollout Strategy](#feature-flag-rollout-strategy)
4. [Monitoring and Alerting](#monitoring-and-alerting)
5. [Emergency Procedures](#emergency-procedures)
6. [Performance Validation](#performance-validation)
7. [Security Compliance](#security-compliance)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Rollback Procedures](#rollback-procedures)
10. [Post-Deployment Validation](#post-deployment-validation)

---

## Pre-Deployment Checklist

### Environment Preparation

- [ ] **Staging Environment Validation**
  - [ ] All LiteRT backends tested (CPU/GPU/NPU)
  - [ ] Performance targets validated (243/1876/5836 tokens/sec)
  - [ ] Memory usage under 2GB confirmed
  - [ ] Backend switching works correctly
  - [ ] Fallback to cloud AI functional

- [ ] **Security Requirements**
  - [ ] Model files digitally signed and validated
  - [ ] Encryption enabled for sensitive data
  - [ ] Audit logging configured and tested
  - [ ] Security scanning completed
  - [ ] Compliance requirements verified

- [ ] **Infrastructure Readiness**
  - [ ] Monitoring systems operational
  - [ ] Alerting endpoints configured
  - [ ] Log aggregation working
  - [ ] Crash reporting enabled
  - [ ] Performance metrics collection active

- [ ] **Feature Flag Configuration**
  - [ ] All LiteRT flags set to 0% initially
  - [ ] Emergency kill switches tested
  - [ ] A/B testing framework operational
  - [ ] User hash-based assignment working

### Code Quality Gates

- [ ] **Performance Tests**
  ```bash
  ./run_litert_performance_tests.sh
  ```
  - Expected: All performance targets met
  - CPU: ≥243 tokens/sec
  - GPU: ≥1876 tokens/sec (where available)
  - NPU: ≥5836 tokens/sec (where available)
  - Memory: <2GB peak usage

- [ ] **Security Validation**
  ```bash
  ./verify_android_security.sh
  ```
  - Model validation passing
  - Encryption working
  - Audit trails intact

- [ ] **Integration Tests**
  ```bash
  ./run_comprehensive_ai_tests.sh
  ```
  - All AI workflows functional
  - Error handling working
  - Graceful degradation tested

---

## Deployment Procedures

### Phase 1: Infrastructure Deployment

1. **Deploy Monitoring Infrastructure**
   ```bash
   # Deploy production monitoring systems
   kubectl apply -f monitoring/production-monitoring.yaml
   
   # Verify monitoring stack
   ./scripts/verify-monitoring-stack.sh
   ```

2. **Configure Environment Settings**
   ```kotlin
   // Set production environment
   val config = ProductionEnvironmentConfig.productionConfig()
   EnvironmentConfigManager().loadConfiguration(Environment.PRODUCTION)
   ```

3. **Initialize Security Systems**
   ```bash
   # Deploy security validation services
   ./scripts/deploy-security-stack.sh
   
   # Validate security configuration
   ./scripts/validate-security-config.sh
   ```

### Phase 2: Application Deployment

1. **Deploy with LiteRT Disabled**
   ```bash
   # Build production APK with LiteRT disabled
   ./gradlew :androidApp:assembleRelease -Penvironment=production
   
   # Deploy to Play Store (internal testing track first)
   ./scripts/deploy-to-play-store.sh --track=internal
   ```

2. **Verify Baseline Functionality**
   ```bash
   # Run smoke tests on production build
   ./scripts/production-smoke-tests.sh
   
   # Verify core AI functionality (cloud-based)
   ./scripts/verify-core-ai.sh
   ```

### Phase 3: Feature Flag Preparation

1. **Initialize Feature Flags**
   ```kotlin
   // Set all LiteRT flags to 0%
   featureFlagManager.setFlag("litert_enabled", 0f)
   featureFlagManager.setFlag("litert_gpu_backend", 0f)
   featureFlagManager.setFlag("litert_npu_backend", 0f)
   featureFlagManager.setFlag("litert_adaptive_switching", 0f)
   ```

2. **Test Emergency Controls**
   ```kotlin
   // Test emergency kill switch
   featureFlagManager.setEmergencyOverride("litert_enabled", false, "Pre-deployment test")
   ```

---

## Feature Flag Rollout Strategy

### Rollout Phases

#### Phase 1: Internal Testing (0% → 1%)
**Duration:** 24-48 hours  
**Target:** Internal team devices only

```kotlin
// Enable for internal testers
featureFlagManager.updateFlag("litert_enabled", 1f, targetGroups = ["internal_testers"])
```

**Success Criteria:**
- No crashes related to LiteRT
- Performance targets met on test devices
- Memory usage within limits
- No security incidents

#### Phase 2: Limited Beta (1% → 5%)
**Duration:** 3-7 days  
**Target:** Beta testers and high-engagement users

```kotlin
// Gradual rollout to beta users
featureFlagManager.updateFlag("litert_enabled", 5f, strategy = RolloutStrategy.HASH_BASED)
```

**Success Criteria:**
- User satisfaction metrics stable or improving
- Error rate <1%
- Performance improvement visible in analytics
- No backend switching issues

#### Phase 3: Broader Rollout (5% → 25%)
**Duration:** 1-2 weeks  
**Target:** Users with capable devices

```kotlin
// Expand based on device capabilities
featureFlagManager.updateFlag("litert_enabled", 25f, strategy = RolloutStrategy.CAPABILITY_BASED)
```

**Success Criteria:**
- 3x+ performance improvement confirmed
- Memory usage stable
- User engagement metrics positive
- Support ticket volume unchanged

#### Phase 4: Full Rollout (25% → 100%)
**Duration:** 2-4 weeks  
**Target:** All compatible devices

```kotlin
// Full rollout with monitoring
featureFlagManager.updateFlag("litert_enabled", 100f, strategy = RolloutStrategy.GRADUAL)
```

**Success Criteria:**
- All performance targets consistently met
- User satisfaction improved
- System stability maintained
- Business metrics positive

---

## Monitoring and Alerting

### Key Metrics to Monitor

#### Performance Metrics
```yaml
metrics:
  litert_inference_latency:
    threshold: 5000ms
    severity: warning
  
  memory_usage:
    threshold: 1800MB
    severity: critical
  
  tokens_per_second:
    cpu_min: 243
    gpu_min: 1876
    npu_min: 5836
    severity: warning

  backend_switch_frequency:
    threshold: 10_per_hour
    severity: warning
```

#### Health Metrics
```yaml
health_checks:
  litert_availability:
    interval: 30s
    timeout: 10s
  
  model_validation:
    interval: 5m
    timeout: 30s
  
  fallback_functionality:
    interval: 10m
    timeout: 60s
```

### Alert Escalation Matrix

| Severity | Response Time | Escalation |
|----------|---------------|------------|
| **Critical** | 15 minutes | On-call engineer → Team lead → Engineering manager |
| **Warning** | 1 hour | Team notification → Investigation within 4 hours |
| **Info** | 24 hours | Log review during business hours |

### Dashboard URLs

- **Production Monitoring**: `https://monitoring.hazardhawk.com/production`
- **LiteRT Performance**: `https://monitoring.hazardhawk.com/litert`
- **User Analytics**: `https://analytics.hazardhawk.com/production`
- **Security Dashboard**: `https://security.hazardhawk.com/production`

---

## Emergency Procedures

### Immediate Rollback (< 5 minutes)

1. **Disable LiteRT Processing**
   ```kotlin
   // Emergency kill switch
   featureFlagManager.setEmergencyOverride("litert_enabled", false, "Production incident")
   ```

2. **Force Cloud AI Fallback**
   ```kotlin
   featureFlagManager.setEmergencyOverride("ai_fallback_to_cloud", true, "Emergency fallback")
   ```

3. **Notify Stakeholders**
   ```bash
   # Send alert to incident channel
   curl -X POST "$SLACK_WEBHOOK" -d '{"text":"🚨 LiteRT Emergency Rollback Initiated"}'
   ```

### Performance Degradation Response

1. **Identify Affected Backend**
   ```bash
   # Check backend performance
   ./scripts/check-backend-performance.sh
   ```

2. **Disable Problematic Backend**
   ```kotlin
   // Disable specific backend
   when (degradedBackend) {
       "GPU" -> featureFlagManager.setEmergencyOverride("litert_gpu_backend", false, "Performance degradation")
       "NPU" -> featureFlagManager.setEmergencyOverride("litert_npu_backend", false, "Performance degradation")
   }
   ```

3. **Monitor Recovery**
   ```bash
   # Monitor system recovery
   watch -n 10 './scripts/system-health-check.sh'
   ```

### Security Incident Response

1. **Activate Security Lockdown**
   ```kotlin
   securityValidator.emergencyLockdown("Security incident detected", SecuritySeverity.HIGH)
   ```

2. **Collect Security Evidence**
   ```bash
   # Collect security logs
   ./scripts/collect-security-evidence.sh --incident-id=$INCIDENT_ID
   ```

3. **Notify Security Team**
   ```bash
   # Alert security team
   ./scripts/notify-security-team.sh --severity=high --type=litert_incident
   ```

---

## Performance Validation

### Automated Performance Tests

Run these tests after each rollout phase:

```bash
#!/bin/bash
# performance-validation-suite.sh

echo "🔧 Running LiteRT Performance Validation Suite..."

# Test CPU backend performance
echo "Testing CPU backend..."
cpu_performance=$(./scripts/test-cpu-performance.sh)
if [ "$cpu_performance" -lt 243 ]; then
    echo "❌ CPU performance below threshold: $cpu_performance tokens/sec"
    exit 1
fi

# Test GPU backend performance (if available)
echo "Testing GPU backend..."
gpu_performance=$(./scripts/test-gpu-performance.sh)
if [ "$gpu_performance" -gt 0 ] && [ "$gpu_performance" -lt 1876 ]; then
    echo "❌ GPU performance below threshold: $gpu_performance tokens/sec"
    exit 1
fi

# Test memory usage
echo "Testing memory usage..."
memory_usage=$(./scripts/test-memory-usage.sh)
if [ "$memory_usage" -gt 2000 ]; then
    echo "❌ Memory usage exceeds threshold: $memory_usage MB"
    exit 1
fi

# Test backend switching
echo "Testing backend switching..."
switch_time=$(./scripts/test-backend-switching.sh)
if [ "$switch_time" -gt 500 ]; then
    echo "❌ Backend switching too slow: $switch_time ms"
    exit 1
fi

echo "✅ All performance tests passed!"
```

### Performance Benchmarking

```bash
# Run comprehensive benchmark suite
./run_litert_performance_tests.sh --environment=production --duration=60m

# Expected output:
# ✅ CPU Backend: 243+ tokens/sec
# ✅ GPU Backend: 1876+ tokens/sec (where available)  
# ✅ NPU Backend: 5836+ tokens/sec (where available)
# ✅ Memory Usage: <2GB peak
# ✅ Backend Switching: <500ms
# ✅ Fallback Latency: <2s
```

---

## Security Compliance

### Security Validation Checklist

- [ ] **Model Integrity**
  ```bash
  # Validate all model files
  ./scripts/validate-model-integrity.sh --environment=production
  ```

- [ ] **Encryption Verification**
  ```bash
  # Verify encryption is working
  ./scripts/verify-encryption.sh --check-all-components
  ```

- [ ] **Audit Trail Validation**
  ```bash
  # Check audit logging
  ./scripts/validate-audit-trails.sh --last-24h
  ```

- [ ] **Compliance Checks**
  ```bash
  # Run compliance validation
  ./scripts/run-compliance-checks.sh --standard=SOC2 --standard=GDPR
  ```

### Security Monitoring

Monitor these security metrics continuously:

```yaml
security_metrics:
  failed_model_validations:
    threshold: 5_per_hour
    action: alert_security_team
  
  encryption_failures:
    threshold: 1_per_day
    action: investigate_immediately
  
  audit_log_gaps:
    threshold: 1_minute
    action: critical_alert
  
  suspicious_processing_patterns:
    threshold: 1_occurrence
    action: security_review
```

---

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue: LiteRT Models Not Loading
**Symptoms:** Models fail to initialize, fallback to cloud AI

**Diagnosis:**
```bash
# Check model file integrity
./scripts/check-model-integrity.sh

# Verify storage permissions
./scripts/check-storage-permissions.sh

# Check available storage space
df -h /data/data/com.hazardhawk
```

**Solution:**
```bash
# Re-download and validate models
./scripts/redownload-models.sh --verify-integrity

# Clear model cache if corrupted
./scripts/clear-model-cache.sh --force
```

#### Issue: High Memory Usage
**Symptoms:** Memory usage >2GB, app crashes, system slowdown

**Diagnosis:**
```bash
# Monitor memory usage
./scripts/monitor-memory-usage.sh --duration=10m

# Check for memory leaks
./scripts/check-memory-leaks.sh --component=litert
```

**Solution:**
```kotlin
// Reduce concurrent inferences
liteRTConfig.maxConcurrentInferences = 2

// Clear unused models from cache
modelCache.clearUnusedModels()

// Enable aggressive garbage collection
memoryManager.enableAggressiveGC()
```

#### Issue: Frequent Backend Switching
**Symptoms:** Backends switching every few seconds, unstable performance

**Diagnosis:**
```bash
# Analyze switching patterns
./scripts/analyze-backend-switching.sh --last-1h
```

**Solution:**
```kotlin
// Increase switching thresholds
backendSwitcher.setPerformanceThreshold(0.3) // 30% degradation required
backendSwitcher.setSwitchCooldown(60_000) // 60 second cooldown
```

#### Issue: Poor GPU Performance
**Symptoms:** GPU backend performing worse than CPU

**Diagnosis:**
```bash
# Check GPU driver version
./scripts/check-gpu-drivers.sh

# Verify GPU memory availability
./scripts/check-gpu-memory.sh
```

**Solution:**
```kotlin
// Disable GPU backend for problematic drivers
if (gpuDriverVersion < "required_version") {
    featureFlagManager.setEmergencyOverride("litert_gpu_backend", false, "Incompatible GPU driver")
}
```

### Log Analysis

#### Key Log Patterns to Monitor

```bash
# Monitor for performance issues
grep -E "LITERT.*PERFORMANCE_DEGRADATION" /var/log/hazardhawk/app.log

# Check for memory warnings
grep -E "MEMORY.*WARNING|OOM" /var/log/hazardhawk/app.log

# Monitor backend switching
grep -E "BACKEND_SWITCH" /var/log/hazardhawk/app.log

# Check security incidents
grep -E "SECURITY.*INCIDENT|VALIDATION_FAILED" /var/log/hazardhawk/security.log
```

---

## Rollback Procedures

### Gradual Rollback Strategy

#### Level 1: Reduce Rollout Percentage
```kotlin
// Reduce from current percentage to 50%
featureFlagManager.updateFlag("litert_enabled", currentPercentage * 0.5f)
```

#### Level 2: Disable Problematic Features
```kotlin
// Disable specific problematic backends
featureFlagManager.setEmergencyOverride("litert_gpu_backend", false, "Stability issues")
featureFlagManager.setEmergencyOverride("litert_adaptive_switching", false, "Stability issues")
```

#### Level 3: Complete LiteRT Disable
```kotlin
// Full rollback to cloud AI
featureFlagManager.setEmergencyOverride("litert_enabled", false, "Production incident")
featureFlagManager.setEmergencyOverride("ai_fallback_to_cloud", true, "Emergency fallback")
```

#### Level 4: Application Rollback
```bash
# Rollback to previous app version
./scripts/rollback-app-version.sh --version=previous --track=production
```

### Rollback Validation

After any rollback:

1. **Verify System Stability**
   ```bash
   ./scripts/verify-system-stability.sh --duration=30m
   ```

2. **Check Performance Metrics**
   ```bash
   ./scripts/check-performance-recovery.sh
   ```

3. **Validate User Experience**
   ```bash
   ./scripts/validate-user-experience.sh --metrics=satisfaction,engagement
   ```

---

## Post-Deployment Validation

### Success Metrics

Track these metrics for 48 hours after each rollout phase:

#### Performance Metrics
- [ ] **CPU Performance**: ≥243 tokens/sec (95th percentile)
- [ ] **GPU Performance**: ≥1876 tokens/sec where available
- [ ] **Memory Usage**: <1.8GB average, <2GB peak
- [ ] **Inference Latency**: <3s (95th percentile)
- [ ] **Backend Switch Time**: <500ms average

#### User Experience Metrics
- [ ] **Task Completion Rate**: ≥95%
- [ ] **User Session Duration**: Stable or increased
- [ ] **App Crash Rate**: <0.1%
- [ ] **User Satisfaction**: ≥4.5/5.0 (if available)

#### System Health Metrics
- [ ] **API Response Time**: <2s (95th percentile)
- [ ] **Error Rate**: <1%
- [ ] **Support Ticket Volume**: No significant increase
- [ ] **Resource Utilization**: Within normal ranges

### Validation Scripts

```bash
#!/bin/bash
# post-deployment-validation.sh

echo "🔍 Running Post-Deployment Validation..."

# Check performance metrics
./scripts/validate-performance-metrics.sh --threshold-file=production-thresholds.yaml

# Verify user experience metrics
./scripts/validate-user-metrics.sh --duration=48h

# Check system health
./scripts/validate-system-health.sh --comprehensive

# Generate validation report
./scripts/generate-validation-report.sh --output=validation-report-$(date +%Y%m%d).html

echo "✅ Post-deployment validation completed"
```

### Go/No-Go Decision Criteria

**Proceed to Next Phase if:**
- All performance targets met for 48+ hours
- No critical incidents or rollbacks
- User satisfaction metrics stable or improved
- System health metrics within acceptable ranges
- Security compliance maintained

**Hold/Rollback if:**
- Any performance target missed by >20%
- Critical incidents requiring intervention
- User satisfaction significantly decreased
- System instability or increased crash rates
- Security compliance violations detected

---

## Contact Information

### Escalation Contacts

| Role | Primary | Secondary | Emergency |
|------|---------|-----------|-----------|
| **On-Call Engineer** | +1-555-0101 | +1-555-0102 | Slack: #incidents |
| **Engineering Manager** | +1-555-0201 | +1-555-0202 | Slack: @eng-mgr |
| **Security Team** | security@hazardhawk.com | +1-555-0301 | Slack: #security |
| **DevOps Team** | devops@hazardhawk.com | +1-555-0401 | Slack: #devops |

### Communication Channels

- **Incidents**: Slack #production-incidents
- **Deployments**: Slack #deployments
- **Security**: Slack #security-alerts
- **General**: Slack #engineering

### Emergency Procedures

For **CRITICAL** incidents affecting >10% of users:
1. Page on-call engineer immediately
2. Create incident in PagerDuty
3. Post to #production-incidents channel
4. Engage incident commander if needed

---

## Document Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2025-01-09 | AI Safety Engineer | Initial production runbook |
| 1.0.1 | TBD | TBD | Updates based on initial deployment feedback |

---

**Last Updated:** January 9, 2025  
**Next Review Date:** February 9, 2025  
**Document Owner:** Engineering Team  
**Approvers:** Engineering Manager, Security Team, DevOps Team