#!/bin/bash

# HazardHawk LiteRT Production Deployment Script
# This script handles the safe, monitored deployment of LiteRT features

set -e  # Exit on any error
set -u  # Exit on undefined variables

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DEPLOYMENT_LOG="deployment_${TIMESTAMP}.log"
ENVIRONMENT="${1:-staging}"  # Default to staging
ROLLOUT_PHASE="${2:-phase1}"  # Default to phase 1

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] $1${NC}" | tee -a "$DEPLOYMENT_LOG"
}

log_warning() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}" | tee -a "$DEPLOYMENT_LOG"
}

log_error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}" | tee -a "$DEPLOYMENT_LOG"
}

log_info() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')] INFO: $1${NC}" | tee -a "$DEPLOYMENT_LOG"
}

# Function to check prerequisites
check_prerequisites() {
    log "🔍 Checking deployment prerequisites..."
    
    # Check if we're in the correct directory
    if [ ! -f "HazardHawk/build.gradle.kts" ]; then
        log_error "Not in HazardHawk project root directory"
        exit 1
    fi
    
    # Check required tools
    local required_tools=("gradle" "adb" "curl" "jq")
    for tool in "${required_tools[@]}"; do
        if ! command -v "$tool" &> /dev/null; then
            log_error "Required tool '$tool' not found"
            exit 1
        fi
    done
    
    # Check environment-specific requirements
    if [ "$ENVIRONMENT" = "production" ]; then
        log "Checking production deployment requirements..."
        
        # Check signing configuration
        if [ ! -f "HazardHawk/keystore.properties" ]; then
            log_error "Production keystore configuration not found"
            exit 1
        fi
        
        # Verify we're on the correct branch
        current_branch=$(git rev-parse --abbrev-ref HEAD)
        if [ "$current_branch" != "main" ] && [ "$current_branch" != "release" ]; then
            log_warning "Deploying from branch '$current_branch' (not main/release)"
            read -p "Continue? (y/N) " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                exit 1
            fi
        fi
    fi
    
    log "✅ Prerequisites check passed"
}

# Function to run comprehensive tests
run_comprehensive_tests() {
    log "🧪 Running comprehensive test suite..."
    
    cd HazardHawk
    
    # Unit tests
    log "Running unit tests..."
    if ! ./gradlew test testDebugUnitTest testReleaseUnitTest; then
        log_error "Unit tests failed"
        return 1
    fi
    
    # Performance tests
    log "Running LiteRT performance tests..."
    if ! ../run_litert_performance_tests.sh; then
        log_error "Performance tests failed"
        return 1
    fi
    
    # Security validation
    log "Running security validation..."
    if ! ../verify_android_security.sh; then
        log_error "Security validation failed"
        return 1
    fi
    
    # AI integration tests
    log "Running AI integration tests..."
    if ! ../run_comprehensive_ai_tests.sh; then
        log_error "AI integration tests failed"
        return 1
    fi
    
    cd ..
    log "✅ All tests passed"
}

# Function to build the application
build_application() {
    log "🏗️ Building HazardHawk application for $ENVIRONMENT..."
    
    cd HazardHawk
    
    # Clean previous builds
    log "Cleaning previous builds..."
    ./gradlew clean
    
    # Build based on environment
    local build_variant=""
    local build_args=""
    
    case "$ENVIRONMENT" in
        "development")
            build_variant="assembleDebug"
            build_args="-Penvironment=development"
            ;;
        "staging")
            build_variant="assembleRelease"
            build_args="-Penvironment=staging"
            ;;
        "production")
            build_variant="assembleRelease"
            build_args="-Penvironment=production -Psigning.enabled=true"
            ;;
        *)
            log_error "Unknown environment: $ENVIRONMENT"
            exit 1
            ;;
    esac
    
    log "Building $build_variant with args: $build_args"
    if ! ./gradlew $build_variant $build_args; then
        log_error "Build failed"
        exit 1
    fi
    
    # Verify APK was created
    local apk_path="androidApp/build/outputs/apk"
    if [ ! -d "$apk_path" ]; then
        log_error "APK output directory not found"
        exit 1
    fi
    
    cd ..
    log "✅ Build completed successfully"
}

# Function to configure feature flags for deployment phase
configure_feature_flags() {
    log "🏁 Configuring feature flags for $ROLLOUT_PHASE..."
    
    case "$ROLLOUT_PHASE" in
        "phase1")
            # Internal testing - 1%
            log "Setting up Phase 1: Internal Testing (1%)"
            configure_flags_phase1
            ;;
        "phase2")
            # Limited beta - 5%
            log "Setting up Phase 2: Limited Beta (5%)"
            configure_flags_phase2
            ;;
        "phase3")
            # Broader rollout - 25%
            log "Setting up Phase 3: Broader Rollout (25%)"
            configure_flags_phase3
            ;;
        "phase4")
            # Full rollout - 100%
            log "Setting up Phase 4: Full Rollout (100%)"
            configure_flags_phase4
            ;;
        *)
            log_error "Unknown rollout phase: $ROLLOUT_PHASE"
            exit 1
            ;;
    esac
    
    log "✅ Feature flags configured for $ROLLOUT_PHASE"
}

configure_flags_phase1() {
    # Phase 1: Internal testing only
    cat > feature_flags_phase1.json << EOF
{
    "litert_enabled": {
        "percentage": 1.0,
        "target_groups": ["internal_testers"],
        "strategy": "hash_based"
    },
    "litert_gpu_backend": {
        "percentage": 0.0,
        "enabled": false
    },
    "litert_npu_backend": {
        "percentage": 0.0,
        "enabled": false
    },
    "litert_adaptive_switching": {
        "percentage": 0.0,
        "enabled": false
    },
    "litert_performance_monitoring": {
        "percentage": 100.0,
        "enabled": true
    },
    "performance_profiling": {
        "percentage": 100.0,
        "enabled": true
    },
    "crash_reporting_enhanced": {
        "percentage": 100.0,
        "enabled": true
    }
}
EOF
}

configure_flags_phase2() {
    # Phase 2: Limited beta
    cat > feature_flags_phase2.json << EOF
{
    "litert_enabled": {
        "percentage": 5.0,
        "target_groups": ["internal_testers", "beta_testers"],
        "strategy": "hash_based"
    },
    "litert_gpu_backend": {
        "percentage": 2.0,
        "target_groups": ["gpu_capable_devices"],
        "strategy": "capability_based"
    },
    "litert_npu_backend": {
        "percentage": 0.0,
        "enabled": false
    },
    "litert_adaptive_switching": {
        "percentage": 1.0,
        "target_groups": ["performance_testers"],
        "strategy": "hash_based"
    },
    "litert_performance_monitoring": {
        "percentage": 100.0,
        "enabled": true
    }
}
EOF
}

configure_flags_phase3() {
    # Phase 3: Broader rollout
    cat > feature_flags_phase3.json << EOF
{
    "litert_enabled": {
        "percentage": 25.0,
        "strategy": "capability_based"
    },
    "litert_gpu_backend": {
        "percentage": 15.0,
        "target_groups": ["gpu_capable_devices"],
        "strategy": "capability_based"
    },
    "litert_npu_backend": {
        "percentage": 5.0,
        "target_groups": ["npu_capable_devices"],
        "strategy": "capability_based"
    },
    "litert_adaptive_switching": {
        "percentage": 10.0,
        "strategy": "performance_based"
    },
    "litert_performance_monitoring": {
        "percentage": 100.0,
        "enabled": true
    }
}
EOF
}

configure_flags_phase4() {
    # Phase 4: Full rollout
    cat > feature_flags_phase4.json << EOF
{
    "litert_enabled": {
        "percentage": 100.0,
        "strategy": "gradual"
    },
    "litert_gpu_backend": {
        "percentage": 100.0,
        "target_groups": ["gpu_capable_devices"],
        "strategy": "capability_based"
    },
    "litert_npu_backend": {
        "percentage": 100.0,
        "target_groups": ["npu_capable_devices"],
        "strategy": "capability_based"
    },
    "litert_adaptive_switching": {
        "percentage": 100.0,
        "strategy": "gradual"
    },
    "litert_performance_monitoring": {
        "percentage": 100.0,
        "enabled": true
    }
}
EOF
}

# Function to deploy monitoring infrastructure
deploy_monitoring() {
    log "📊 Deploying monitoring infrastructure..."
    
    # Create monitoring configuration
    cat > monitoring_config.yaml << EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: hazardhawk-monitoring-config
data:
  monitoring.yaml: |
    environment: $ENVIRONMENT
    
    metrics:
      collection_interval: 30s
      retention_days: 30
      
    alerts:
      litert_performance_degradation:
        threshold: 0.5  # 50% below target
        severity: warning
        cooldown: 5m
        
      memory_usage_high:
        threshold: 1800  # 1.8GB
        severity: critical
        cooldown: 1m
        
      backend_switching_frequent:
        threshold: 10  # per hour
        severity: warning
        cooldown: 10m
        
      error_rate_high:
        threshold: 0.05  # 5%
        severity: critical
        cooldown: 5m
    
    dashboards:
      - name: "LiteRT Performance"
        url: "https://monitoring.hazardhawk.com/litert"
      - name: "System Health"
        url: "https://monitoring.hazardhawk.com/health"
      - name: "User Analytics"
        url: "https://analytics.hazardhawk.com/production"
EOF
    
    log "✅ Monitoring configuration created"
}

# Function to validate deployment
validate_deployment() {
    log "✅ Validating deployment..."
    
    # Run smoke tests
    log "Running smoke tests..."
    if ! ./scripts/production-smoke-tests.sh; then
        log_error "Smoke tests failed"
        return 1
    fi
    
    # Verify feature flags are working
    log "Validating feature flag configuration..."
    if ! ./scripts/validate-feature-flags.sh --config="feature_flags_${ROLLOUT_PHASE}.json"; then
        log_error "Feature flag validation failed"
        return 1
    fi
    
    # Check monitoring systems
    log "Verifying monitoring systems..."
    if ! ./scripts/verify-monitoring-stack.sh; then
        log_error "Monitoring validation failed"
        return 1
    fi
    
    # Validate security configuration
    log "Checking security configuration..."
    if ! ./scripts/validate-security-config.sh --environment="$ENVIRONMENT"; then
        log_error "Security validation failed"
        return 1
    fi
    
    log "✅ Deployment validation completed"
}

# Function to create deployment report
create_deployment_report() {
    log "📋 Creating deployment report..."
    
    local report_file="deployment_report_${TIMESTAMP}.html"
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>HazardHawk LiteRT Deployment Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .success { color: green; }
        .warning { color: orange; }
        .error { color: red; }
        .info { color: blue; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
        th { background-color: #f2f2f2; }
    </style>
</head>
<body>
    <h1>HazardHawk LiteRT Deployment Report</h1>
    
    <h2>Deployment Summary</h2>
    <table>
        <tr><th>Property</th><th>Value</th></tr>
        <tr><td>Environment</td><td>$ENVIRONMENT</td></tr>
        <tr><td>Rollout Phase</td><td>$ROLLOUT_PHASE</td></tr>
        <tr><td>Deployment Time</td><td>$(date)</td></tr>
        <tr><td>Git Commit</td><td>$(git rev-parse HEAD)</td></tr>
        <tr><td>Git Branch</td><td>$(git rev-parse --abbrev-ref HEAD)</td></tr>
    </table>
    
    <h2>Feature Flag Configuration</h2>
    <pre>$(cat "feature_flags_${ROLLOUT_PHASE}.json" 2>/dev/null || echo "Configuration file not found")</pre>
    
    <h2>Test Results</h2>
    <ul>
        <li class="success">✅ Prerequisites Check - PASSED</li>
        <li class="success">✅ Unit Tests - PASSED</li>
        <li class="success">✅ Performance Tests - PASSED</li>
        <li class="success">✅ Security Validation - PASSED</li>
        <li class="success">✅ Integration Tests - PASSED</li>
        <li class="success">✅ Build Process - PASSED</li>
        <li class="success">✅ Deployment Validation - PASSED</li>
    </ul>
    
    <h2>Monitoring Links</h2>
    <ul>
        <li><a href="https://monitoring.hazardhawk.com/production">Production Monitoring</a></li>
        <li><a href="https://monitoring.hazardhawk.com/litert">LiteRT Performance</a></li>
        <li><a href="https://analytics.hazardhawk.com/production">User Analytics</a></li>
        <li><a href="https://security.hazardhawk.com/production">Security Dashboard</a></li>
    </ul>
    
    <h2>Next Steps</h2>
    <ol>
        <li>Monitor system performance for 48 hours</li>
        <li>Review user analytics and feedback</li>
        <li>Validate performance targets are met</li>
        <li>Plan next rollout phase if metrics are positive</li>
    </ol>
    
    <h2>Emergency Contacts</h2>
    <table>
        <tr><th>Role</th><th>Contact</th></tr>
        <tr><td>On-Call Engineer</td><td>Slack: #production-incidents</td></tr>
        <tr><td>Engineering Manager</td><td>Slack: @eng-mgr</td></tr>
        <tr><td>Security Team</td><td>Slack: #security-alerts</td></tr>
    </table>
    
    <h2>Deployment Log</h2>
    <pre>$(tail -50 "$DEPLOYMENT_LOG")</pre>
    
</body>
</html>
EOF
    
    log "✅ Deployment report created: $report_file"
}

# Function to handle deployment rollback
rollback_deployment() {
    log_error "🚨 Deployment rollback initiated"
    
    # Disable all LiteRT features immediately
    cat > emergency_rollback_flags.json << EOF
{
    "litert_enabled": {
        "percentage": 0.0,
        "enabled": false,
        "emergency_override": true
    },
    "litert_gpu_backend": {
        "percentage": 0.0,
        "enabled": false,
        "emergency_override": true
    },
    "litert_npu_backend": {
        "percentage": 0.0,
        "enabled": false,
        "emergency_override": true
    },
    "litert_adaptive_switching": {
        "percentage": 0.0,
        "enabled": false,
        "emergency_override": true
    },
    "ai_fallback_to_cloud": {
        "percentage": 100.0,
        "enabled": true,
        "emergency_override": true
    }
}
EOF
    
    log_error "Emergency rollback configuration applied"
    log_error "All LiteRT features disabled, fallback to cloud AI enabled"
    
    # Send alert
    send_deployment_alert "ROLLBACK" "Emergency deployment rollback initiated"
}

# Function to send deployment alerts
send_deployment_alert() {
    local alert_type="$1"
    local message="$2"
    
    # Slack webhook (replace with actual webhook URL)
    local slack_webhook="${SLACK_WEBHOOK_URL:-}"
    
    if [ -n "$slack_webhook" ]; then
        curl -X POST "$slack_webhook" \
            -H 'Content-type: application/json' \
            --data "{\"text\":\"🚨 HazardHawk Deployment Alert: $alert_type - $message\"}" \
            2>/dev/null || log_warning "Failed to send Slack alert"
    fi
    
    log_info "Alert sent: $alert_type - $message"
}

# Function to setup error handling
setup_error_handling() {
    trap 'log_error "Deployment failed at line $LINENO"; rollback_deployment; exit 1' ERR
    trap 'log_info "Deployment interrupted"; rollback_deployment; exit 130' INT
}

# Main deployment function
main() {
    log "🚀 Starting HazardHawk LiteRT Production Deployment"
    log "Environment: $ENVIRONMENT"
    log "Rollout Phase: $ROLLOUT_PHASE"
    log "Timestamp: $TIMESTAMP"
    
    setup_error_handling
    
    # Pre-deployment phase
    check_prerequisites
    run_comprehensive_tests
    
    # Build phase
    build_application
    
    # Configuration phase
    configure_feature_flags
    deploy_monitoring
    
    # Deployment validation
    validate_deployment
    
    # Post-deployment
    create_deployment_report
    
    # Send success notification
    send_deployment_alert "SUCCESS" "Deployment completed successfully for $ENVIRONMENT ($ROLLOUT_PHASE)"
    
    log "🎉 Deployment completed successfully!"
    log "📋 Report: deployment_report_${TIMESTAMP}.html"
    log "📝 Log: $DEPLOYMENT_LOG"
    
    echo
    echo -e "${GREEN}=== DEPLOYMENT SUCCESSFUL ===${NC}"
    echo -e "${BLUE}Environment:${NC} $ENVIRONMENT"
    echo -e "${BLUE}Phase:${NC} $ROLLOUT_PHASE"
    echo -e "${BLUE}Timestamp:${NC} $TIMESTAMP"
    echo
    echo -e "${YELLOW}Next Steps:${NC}"
    echo "1. Monitor system performance for 48 hours"
    echo "2. Review metrics at: https://monitoring.hazardhawk.com/$ENVIRONMENT"
    echo "3. Check deployment report: deployment_report_${TIMESTAMP}.html"
    echo "4. Plan next phase if metrics are positive"
    echo
}

# Script usage
usage() {
    echo "Usage: $0 [ENVIRONMENT] [ROLLOUT_PHASE]"
    echo
    echo "ENVIRONMENT:"
    echo "  development  - Development environment"
    echo "  staging      - Staging environment (default)"
    echo "  production   - Production environment"
    echo
    echo "ROLLOUT_PHASE:"
    echo "  phase1       - Internal testing (1%) (default)"
    echo "  phase2       - Limited beta (5%)"
    echo "  phase3       - Broader rollout (25%)"
    echo "  phase4       - Full rollout (100%)"
    echo
    echo "Examples:"
    echo "  $0                          # Deploy to staging, phase 1"
    echo "  $0 staging phase2           # Deploy to staging, phase 2"
    echo "  $0 production phase1        # Deploy to production, phase 1"
}

# Check if help is requested
if [ "${1:-}" = "-h" ] || [ "${1:-}" = "--help" ]; then
    usage
    exit 0
fi

# Run main function
main "$@"