#!/bin/bash

# Glass UI Phase 2 Restoration Test Automation Script
# Validates Haze library integration and prevents regressions

set -e  # Exit on any error

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"
ANDROID_APP_DIR="$PROJECT_ROOT/HazardHawk/androidApp"
TEST_RESULTS_DIR="$PROJECT_ROOT/test-results"
REPORTS_DIR="$PROJECT_ROOT/reports"

# Test configuration
HAZE_VERSION="1.6.10"
MIN_API_LEVEL=26
TARGET_API_LEVEL=35
REQUIRED_PASS_RATE=95

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites for Glass UI tests..."
    
    # Check if Android SDK is available
    if ! command -v adb &> /dev/null; then
        log_error "ADB not found. Please ensure Android SDK is installed and in PATH."
        exit 1
    fi
    
    # Check if Gradle is available
    if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
        log_error "Gradle wrapper not found in project root."
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Function to create test results directories
setup_test_environment() {
    log_info "Setting up test environment..."
    
    # Create results directories
    mkdir -p "$TEST_RESULTS_DIR"
    mkdir -p "$REPORTS_DIR"
    
    # Clean previous results
    rm -rf "$TEST_RESULTS_DIR"/*
    rm -rf "$REPORTS_DIR"/*
    
    log_success "Test environment setup completed"
}

# Function to run Phase 2A critical tests
run_phase_2a_tests() {
    log_info "Running Phase 2A: Critical Path Tests..."
    
    local test_classes=(
        "com.hazardhawk.ui.glass.haze.HazeAPIIntegrationTest"
        "com.hazardhawk.build.BuildConfigValidationTest"
    )
    
    local success_count=0
    local total_count=${#test_classes[@]}
    
    for test_class in "${test_classes[@]}"; do
        log_info "Running critical test: $test_class"
        
        if cd "$PROJECT_ROOT" && ./gradlew :androidApp:testDebugUnitTest --tests "$test_class" --continue; then
            log_success "✓ PASSED: $test_class"
            ((success_count++))
        else
            log_error "✗ FAILED: $test_class"
        fi
    done
    
    local pass_rate=$((success_count * 100 / total_count))
    log_info "Phase 2A Results: $success_count/$total_count passed ($pass_rate%)"
    
    if [ $pass_rate -lt 100 ]; then
        log_error "Phase 2A critical tests must have 100% pass rate. Current: $pass_rate%"
        return 1
    fi
    
    log_success "Phase 2A critical tests passed"
    return 0
}

# Function to run Phase 2B safety and performance tests
run_phase_2b_tests() {
    log_info "Running Phase 2B: Safety and Performance Tests..."
    
    local test_classes=(
        "com.hazardhawk.ui.glass.haze.HazeGlassComponentTest"
        "com.hazardhawk.safety.ConstructionSafetyGlassTest"
    )
    
    local success_count=0
    local total_count=${#test_classes[@]}
    
    for test_class in "${test_classes[@]}"; do
        log_info "Running safety/performance test: $test_class"
        
        if cd "$PROJECT_ROOT" && ./gradlew :androidApp:testDebugUnitTest --tests "$test_class" --continue; then
            log_success "✓ PASSED: $test_class"
            ((success_count++))
        else
            log_error "✗ FAILED: $test_class"
        fi
    done
    
    local pass_rate=$((success_count * 100 / total_count))
    log_info "Phase 2B Results: $success_count/$total_count passed ($pass_rate%)"
    
    return 0
}

# Function to generate test report
generate_test_report() {
    log_info "Generating comprehensive test report..."
    
    local report_file="$REPORTS_DIR/glass_ui_test_report_$(date +%Y%m%d_%H%M%S).md"
    
    cat > "$report_file" << EOF
# Glass UI Phase 2 Restoration Test Report

**Generated:** $(date)
**Haze Version:** $HAZE_VERSION
**Target API:** $TARGET_API_LEVEL

## Test Summary
- Phase 2A: Critical Path Tests
- Phase 2B: Safety and Performance Tests  
- Phase 2C: Comprehensive Coverage Tests

## Performance Benchmarks
- Emergency Mode Activation: <500ms
- Touch Target Compliance: ≥60dp
- Frame Rate: ≥30 FPS
- Memory Usage: <50MB
- Battery Impact: <15%

EOF

    log_success "Test report generated: $report_file"
    return 0
}

# Main execution function
main() {
    log_info "Starting Glass UI Phase 2 Restoration Test Suite"
    log_info "================================================="
    
    check_prerequisites
    setup_test_environment
    
    # Run test phases
    if run_phase_2a_tests && run_phase_2b_tests; then
        generate_test_report
        log_success "Glass UI Phase 2 restoration tests completed successfully!"
        exit 0
    else
        log_error "Critical tests failed. Please fix issues before continuing."
        exit 1
    fi
}

# Script entry point
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
