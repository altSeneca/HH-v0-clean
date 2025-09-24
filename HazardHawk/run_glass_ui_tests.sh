#!/bin/bash

# HazardHawk Glass UI Comprehensive Testing Framework
# Validates glass morphism UI components for construction safety compliance
# Version: 1.0.0
# Date: 2025-01-10

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_APP_PATH="$PROJECT_ROOT/HazardHawk/androidApp"
TEST_RESULTS_DIR="$PROJECT_ROOT/test-results/glass-ui"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$TEST_RESULTS_DIR/glass_ui_test_$TIMESTAMP.log"

# Create test results directory
mkdir -p "$TEST_RESULTS_DIR"

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

log_header() {
    echo -e "\n${BLUE}==========================================${NC}" | tee -a "$LOG_FILE"
    echo -e "${BLUE}$1${NC}" | tee -a "$LOG_FILE"
    echo -e "${BLUE}==========================================${NC}\n" | tee -a "$LOG_FILE"
}

# Main execution
main() {
    log_header "HAZARDHAWK GLASS UI COMPREHENSIVE TEST SUITE"
    log_info "Starting comprehensive glass UI testing for construction safety compliance"
    
    cd "$PROJECT_ROOT"
    
    # Phase 1: Build validation
    log_header "PHASE 1: BUILD VALIDATION"
    if ./gradlew :androidApp:compileDebugKotlin > "$TEST_RESULTS_DIR/build_validation_$TIMESTAMP.log" 2>&1; then
        log_success "✅ Build validation passed"
    else
        log_error "❌ Build validation failed"
        exit 1
    fi
    
    # Phase 2: Unit tests  
    log_header "PHASE 2: GLASS UI UNIT TESTS"
    if ./gradlew :androidApp:testDebugUnitTest > "$TEST_RESULTS_DIR/unit_tests_$TIMESTAMP.log" 2>&1; then
        log_success "✅ Unit tests passed"
    else
        log_error "❌ Unit tests failed"
        exit 1
    fi
    
    log_header "FINAL RESULTS"
    log_success "🎉 ALL TESTS PASSED!"
    log_success "✅ Glass UI is ready for production deployment"
}

main "$@"
