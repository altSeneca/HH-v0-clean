#!/bin/bash

# HazardHawk AI Integration Test Suite Runner
# Comprehensive automated testing for AI model integration

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"  # Go up two levels from scripts/ai-testing
TEST_RESULTS_DIR="$PROJECT_ROOT/test-results/ai-integration"
AI_MODELS_DIR="$PROJECT_ROOT/ai-models/test"
CI_MODE=${CI:-false}
VERBOSE=${VERBOSE:-false}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✓${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠${NC} $1"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ✗${NC} $1"
}

verbose_log() {
    if [[ "$VERBOSE" == "true" ]]; then
        log "$1"
    fi
}

# Help function
show_help() {
    cat << EOF
HazardHawk AI Integration Test Suite Runner

Usage: $0 [OPTIONS] [TEST_CATEGORY]

Options:
    -h, --help              Show this help message
    -v, --verbose           Enable verbose logging
    -c, --ci                Run in CI mode (stricter validation)
    -m, --models-dir PATH   Specify AI models directory (default: ./ai-models/test)
    -o, --output-dir PATH   Specify test results output directory
    --skip-setup           Skip test environment setup
    --skip-cleanup         Skip test environment cleanup
    --fail-fast            Stop on first test failure
    --generate-report      Generate comprehensive test report

Test Categories:
    all                     Run all AI integration tests (default)
    unit                    Run AI unit tests only
    integration            Run AI integration tests only
    performance            Run AI performance tests only
    domain                 Run construction safety domain tests only
    e2e                    Run end-to-end workflow tests only
    model                  Run AI model validation tests only
    mock                   Run tests with mock AI models
    real                   Run tests with real AI models

Examples:
    $0                      # Run all tests
    $0 -v performance       # Run performance tests with verbose output
    $0 -c --fail-fast       # Run in CI mode, stop on first failure
    $0 model --verbose      # Run model validation tests with verbose output

EOF
}

# Parse command line arguments
TEST_CATEGORY="all"
SKIP_SETUP=false
SKIP_CLEANUP=false
FAIL_FAST=false
GENERATE_REPORT=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            ;;
        -c|--ci)
            CI_MODE=true
            ;;
        -m|--models-dir)
            AI_MODELS_DIR="$2"
            shift
            ;;
        -o|--output-dir)
            TEST_RESULTS_DIR="$2"
            shift
            ;;
        --skip-setup)
            SKIP_SETUP=true
            ;;
        --skip-cleanup)
            SKIP_CLEANUP=true
            ;;
        --fail-fast)
            FAIL_FAST=true
            ;;
        --generate-report)
            GENERATE_REPORT=true
            ;;
        all|unit|integration|performance|domain|e2e|model|mock|real)
            TEST_CATEGORY="$1"
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
    shift
done

# Test environment setup
setup_test_environment() {
    log "Setting up AI test environment..."
    
    # Create test results directory
    mkdir -p "$TEST_RESULTS_DIR"
    
    # Verify project structure
    if [[ ! -f "$PROJECT_ROOT/gradlew" ]]; then
        log_error "Gradle wrapper not found. Please run from HazardHawk project root."
        exit 1
    fi
    
    # Check AI models directory
    if [[ ! -d "$AI_MODELS_DIR" ]]; then
        log "Creating AI models test directory: $AI_MODELS_DIR"
        mkdir -p "$AI_MODELS_DIR"
        
        # Download or create mock models for testing
        create_mock_ai_models
    fi
    
    # Verify Android SDK for Android-specific AI tests
    if [[ -z "${ANDROID_HOME:-}" ]] && [[ "$CI_MODE" == "false" ]]; then
        log_warning "ANDROID_HOME not set. Android AI integration tests may fail."
    fi
    
    # Check available memory for AI model testing
    check_system_resources
    
    log_success "Test environment setup complete"
}

# Create mock AI models for testing
create_mock_ai_models() {
    log "Creating mock AI models for testing..."
    
    # Create mock ONNX model files
    cat > "$AI_MODELS_DIR/vision_encoder_mock.onnx" << 'EOF'
# Mock ONNX vision encoder model
# This is a placeholder for testing - not a real model
EOF
    
    cat > "$AI_MODELS_DIR/decoder_model_merged_mock.onnx" << 'EOF'
# Mock ONNX decoder model
# This is a placeholder for testing - not a real model
EOF
    
    # Create model metadata
    cat > "$AI_MODELS_DIR/model_metadata_mock.json" << 'EOF'
{
  "model_name": "gemma-3n-e2b-mock",
  "version": "1.0.0-test",
  "vision_input_size": [224, 224, 3],
  "max_sequence_length": 2048,
  "supports_multimodal": true,
  "memory_footprint_mb": 512,
  "confidence_threshold": 0.6,
  "test_mode": true
}
EOF
    
    log_success "Mock AI models created"
}

# Check system resources for AI testing
check_system_resources() {
    local available_memory_mb
    
    if command -v free > /dev/null 2>&1; then
        available_memory_mb=$(free -m | awk 'NR==2{printf "%d", $7}')
    elif command -v vm_stat > /dev/null 2>&1; then
        # macOS
        local free_pages
        free_pages=$(vm_stat | grep "Pages free" | awk '{print $3}' | tr -d '.')
        available_memory_mb=$((free_pages * 4096 / 1024 / 1024))
    else
        log_warning "Unable to determine available memory"
        return 0
    fi
    
    verbose_log "Available memory: ${available_memory_mb}MB"
    
    if [[ $available_memory_mb -lt 2048 ]]; then
        log_warning "Low available memory (${available_memory_mb}MB). AI model tests may fail."
        if [[ "$CI_MODE" == "true" ]]; then
            log_error "Insufficient memory for CI testing. Need at least 2GB available."
            exit 1
        fi
    fi
}

# Run specific test category
run_test_category() {
    local category="$1"
    local test_results_file="$TEST_RESULTS_DIR/${category}_results.xml"
    local gradle_args=()
    
    # Configure Gradle arguments based on category
    case "$category" in
        unit)
            gradle_args+=(
                ":shared:testDebugUnitTest"
                "--tests" "*AI*"
                "--tests" "*Gemma*"
                "--tests" "*ONNX*"
            )
            ;;
        integration)
            gradle_args+=(
                ":shared:testDebugUnitTest"
                "--tests" "*Integration*"
                "--tests" "*Workflow*"
            )
            ;;
        performance)
            gradle_args+=(
                ":shared:testDebugUnitTest"
                "--tests" "*Performance*"
                "--tests" "*Benchmark*"
                "--tests" "*Memory*"
            )
            ;;
        domain)
            gradle_args+=(
                ":shared:testDebugUnitTest"
                "--tests" "*ConstructionSafety*"
                "--tests" "*OSHA*"
                "--tests" "*Domain*"
            )
            ;;
        e2e)
            gradle_args+=(
                ":androidApp:connectedDebugAndroidTest"
                "--tests" "*E2E*"
                "--tests" "*Camera*"
            )
            ;;
        model)
            gradle_args+=(
                ":shared:testDebugUnitTest"
                "--tests" "*Model*"
                "--tests" "*ONNX*"
                "--tests" "*Gemma*"
            )
            ;;
        mock)
            # Run with mock models only
            export AI_TEST_MODE="mock"
            gradle_args+=(
                ":shared:testDebugUnitTest"
                "--tests" "*AI*"
            )
            ;;
        real)
            # Run with real models (if available)
            export AI_TEST_MODE="real"
            gradle_args+=(
                ":shared:testDebugUnitTest"
                "--tests" "*AI*"
            )
            ;;
        all)
            # Run comprehensive test suite
            run_comprehensive_test_suite
            return $?
            ;;
        *)
            log_error "Unknown test category: $category"
            return 1
            ;;
    esac
    
    # Add common Gradle arguments
    gradle_args+=("--continue")
    
    if [[ "$FAIL_FAST" == "true" ]]; then
        gradle_args+=("--fail-fast")
    fi
    
    if [[ "$VERBOSE" == "true" ]]; then
        gradle_args+=("--info")
    fi
    
    # Set environment variables for testing
    export AI_MODELS_DIR="$AI_MODELS_DIR"
    export TEST_RESULTS_DIR="$TEST_RESULTS_DIR"
    
    log "Running $category tests..."
    verbose_log "Gradle command: ./gradlew ${gradle_args[*]}"
    
    # Run the tests
    if cd "$PROJECT_ROOT" && ./gradlew "${gradle_args[@]}"; then
        log_success "$category tests completed successfully"
        return 0
    else
        log_error "$category tests failed"
        return 1
    fi
}

# Run comprehensive test suite (all categories)
run_comprehensive_test_suite() {
    local categories=("unit" "integration" "performance" "domain")
    local failed_categories=()
    local total_start_time
    total_start_time=$(date +%s)
    
    log "Running comprehensive AI test suite..."
    
    # Run Android E2E tests if not in CI or if Android environment is available
    if [[ "$CI_MODE" == "false" ]] || [[ -n "${ANDROID_HOME:-}" ]]; then
        categories+=("e2e")
    fi
    
    # Run each test category
    for category in "${categories[@]}"; do
        log "\n=== Running $category tests ==="
        
        if run_test_category "$category"; then
            log_success "$category tests: PASSED"
        else
            log_error "$category tests: FAILED"
            failed_categories+=("$category")
            
            if [[ "$FAIL_FAST" == "true" ]]; then
                break
            fi
        fi
    done
    
    local total_end_time
    total_end_time=$(date +%s)
    local total_duration=$((total_end_time - total_start_time))
    
    # Print summary
    log "\n=== Test Suite Summary ==="
    log "Total duration: ${total_duration}s"
    log "Categories tested: ${#categories[@]}"
    
    if [[ ${#failed_categories[@]} -eq 0 ]]; then
        log_success "All test categories passed! 🎉"
        return 0
    else
        log_error "Failed categories: ${failed_categories[*]}"
        log_error "${#failed_categories[@]}/${#categories[@]} test categories failed"
        return 1
    fi
}

# Generate comprehensive test report
generate_test_report() {
    log "Generating comprehensive test report..."
    
    local report_file="$TEST_RESULTS_DIR/ai_integration_test_report.html"
    local timestamp
    timestamp=$(date +'%Y-%m-%d %H:%M:%S')
    
    cat > "$report_file" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>HazardHawk AI Integration Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f5f5f5; padding: 20px; border-radius: 5px; }
        .summary { margin: 20px 0; }
        .test-category { margin: 15px 0; padding: 10px; border-left: 4px solid #ccc; }
        .passed { border-left-color: #4CAF50; }
        .failed { border-left-color: #f44336; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; }
        .metric { background: #f9f9f9; padding: 15px; border-radius: 5px; }
        .metric-value { font-size: 24px; font-weight: bold; color: #2196F3; }
    </style>
</head>
<body>
    <div class="header">
        <h1>HazardHawk AI Integration Test Report</h1>
        <p>Generated on: $timestamp</p>
        <p>Test Category: $TEST_CATEGORY</p>
        <p>CI Mode: $CI_MODE</p>
    </div>
    
    <div class="summary">
        <h2>Test Execution Summary</h2>
        <div class="metrics">
EOF
    
    # Add test metrics to report
    add_test_metrics_to_report "$report_file"
    
    cat >> "$report_file" << EOF
        </div>
    </div>
    
    <div class="details">
        <h2>Detailed Results</h2>
EOF
    
    # Add detailed test results
    add_detailed_results_to_report "$report_file"
    
    cat >> "$report_file" << EOF
    </div>
    
    <div class="footer">
        <p><em>Report generated by HazardHawk AI Test Suite Runner</em></p>
    </div>
</body>
</html>
EOF
    
    log_success "Test report generated: $report_file"
}

# Add test metrics to HTML report
add_test_metrics_to_report() {
    local report_file="$1"
    
    # Count test result files
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    
    if [[ -d "$TEST_RESULTS_DIR" ]]; then
        # Parse JUnit XML files for metrics
        for xml_file in "$TEST_RESULTS_DIR"/*.xml; do
            if [[ -f "$xml_file" ]]; then
                # Extract test counts from XML (simplified parsing)
                local file_tests
                file_tests=$(grep -o 'tests="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")
                local file_failures
                file_failures=$(grep -o 'failures="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")
                
                total_tests=$((total_tests + file_tests))
                failed_tests=$((failed_tests + file_failures))
            fi
        done
    fi
    
    passed_tests=$((total_tests - failed_tests))
    local success_rate=0
    
    if [[ $total_tests -gt 0 ]]; then
        success_rate=$((passed_tests * 100 / total_tests))
    fi
    
    cat >> "$report_file" << EOF
            <div class="metric">
                <div class="metric-value">$total_tests</div>
                <div>Total Tests</div>
            </div>
            <div class="metric">
                <div class="metric-value">$passed_tests</div>
                <div>Passed</div>
            </div>
            <div class="metric">
                <div class="metric-value">$failed_tests</div>
                <div>Failed</div>
            </div>
            <div class="metric">
                <div class="metric-value">$success_rate%</div>
                <div>Success Rate</div>
            </div>
EOF
}

# Add detailed results to HTML report
add_detailed_results_to_report() {
    local report_file="$1"
    
    # Add test category results
    for xml_file in "$TEST_RESULTS_DIR"/*.xml; do
        if [[ -f "$xml_file" ]]; then
            local category
            category=$(basename "$xml_file" .xml | sed 's/_results//')
            local status="passed"
            
            # Check if any failures in this file
            if grep -q 'failures="[1-9]' "$xml_file"; then
                status="failed"
            fi
            
            cat >> "$report_file" << EOF
        <div class="test-category $status">
            <h3>$category Tests</h3>
            <p>Status: <strong>$(echo $status | tr '[:lower:]' '[:upper:]')</strong></p>
            <details>
                <summary>View Details</summary>
                <pre>$(cat "$xml_file" | head -50)</pre>
            </details>
        </div>
EOF
        fi
    done
}

# Cleanup test environment
cleanup_test_environment() {
    if [[ "$SKIP_CLEANUP" == "true" ]]; then
        log "Skipping cleanup as requested"
        return 0
    fi
    
    log "Cleaning up test environment..."
    
    # Clean up Gradle build cache
    if cd "$PROJECT_ROOT"; then
        ./gradlew clean > /dev/null 2>&1 || true
    fi
    
    # Remove temporary test files (keep results)
    find "$PROJECT_ROOT" -name "*.tmp" -type f -delete 2>/dev/null || true
    
    log_success "Test environment cleanup complete"
}

# Main execution function
main() {
    local exit_code=0
    
    log "HazardHawk AI Integration Test Suite Runner"
    log "Test category: $TEST_CATEGORY"
    log "CI mode: $CI_MODE"
    log "Verbose: $VERBOSE"
    
    # Setup test environment
    if [[ "$SKIP_SETUP" == "false" ]]; then
        setup_test_environment
    fi
    
    # Run tests
    if ! run_test_category "$TEST_CATEGORY"; then
        exit_code=1
    fi
    
    # Generate report if requested
    if [[ "$GENERATE_REPORT" == "true" ]]; then
        generate_test_report
    fi
    
    # Cleanup
    cleanup_test_environment
    
    # Exit with appropriate code
    if [[ $exit_code -eq 0 ]]; then
        log_success "All tests completed successfully! 🎉"
    else
        log_error "Some tests failed. Check the output above for details."
    fi
    
    exit $exit_code
}

# Handle script interruption
trap 'log_error "Script interrupted"; cleanup_test_environment; exit 130' INT TERM

# Run main function
main "$@"