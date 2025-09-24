#!/bin/bash

# AI Testing Automation Script for HazardHawk ONNX Gemma Implementation
# This script orchestrates comprehensive testing of the AI model including
# unit tests, integration tests, validation tests, and performance benchmarks.

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEST_RESULTS_DIR="$PROJECT_ROOT/build/test-results/ai-tests"
REPORTS_DIR="$PROJECT_ROOT/build/reports/ai-tests"
ONNX_CACHE_DIR="$HOME/.cache/onnx-models"
LOG_FILE="$TEST_RESULTS_DIR/ai-test-execution.log"

# Default configuration
TEST_LEVEL="unit"
PARALLEL_EXECUTION="false"
GENERATE_REPORT="true"
CLEANUP_AFTER="true"
VERBOSE="false"
MEMORY_MONITORING="false"
PERFORMANCE_PROFILING="false"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

# Help function
show_help() {
    cat << EOF
HazardHawk AI Testing Script

Usage: $0 [OPTIONS]

Options:
    -l, --level LEVEL          Test level: unit|integration|validation|performance|full (default: unit)
    -p, --parallel             Enable parallel test execution
    -r, --no-report           Skip generating HTML reports
    -c, --no-cleanup          Skip cleanup after tests
    -v, --verbose             Enable verbose output
    -m, --memory-monitoring   Enable memory usage monitoring
    -f, --performance-profile Enable performance profiling
    -h, --help                Show this help message

Test Levels:
    unit                       Run only unit tests (fast)
    integration               Run unit and integration tests
    validation                Run unit, integration, and model validation tests
    performance               Run performance and benchmark tests only
    full                      Run all tests (slowest, most comprehensive)

Examples:
    $0 --level unit                    # Quick unit tests
    $0 --level validation --parallel   # Validation tests with parallel execution
    $0 --level performance -m -f       # Performance tests with monitoring and profiling
    $0 --level full --verbose          # Complete test suite with verbose output

Environment Variables:
    ONNX_MODEL_PATH                    Path to ONNX models (default: ~/.cache/onnx-models)
    AI_TEST_TIMEOUT                    Test timeout in minutes (default: 60)
    AI_TEST_MEMORY_LIMIT              Memory limit in MB (default: 2048)
    GRADLE_OPTS                       Additional Gradle options
EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -l|--level)
            TEST_LEVEL="$2"
            shift 2
            ;;
        -p|--parallel)
            PARALLEL_EXECUTION="true"
            shift
            ;;
        -r|--no-report)
            GENERATE_REPORT="false"
            shift
            ;;
        -c|--no-cleanup)
            CLEANUP_AFTER="false"
            shift
            ;;
        -v|--verbose)
            VERBOSE="true"
            shift
            ;;
        -m|--memory-monitoring)
            MEMORY_MONITORING="true"
            shift
            ;;
        -f|--performance-profile)
            PERFORMANCE_PROFILING="true"
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Validate test level
case $TEST_LEVEL in
    unit|integration|validation|performance|full)
        ;;
    *)
        log_error "Invalid test level: $TEST_LEVEL"
        show_help
        exit 1
        ;;
esac

# Setup functions
setup_directories() {
    log_info "Setting up test directories..."
    mkdir -p "$TEST_RESULTS_DIR"
    mkdir -p "$REPORTS_DIR"
    mkdir -p "$ONNX_CACHE_DIR"
    
    # Initialize log file
    echo "AI Test Execution Log - $(date)" > "$LOG_FILE"
    echo "Test Level: $TEST_LEVEL" >> "$LOG_FILE"
    echo "Project Root: $PROJECT_ROOT" >> "$LOG_FILE"
    echo "========================================" >> "$LOG_FILE"
}

setup_test_models() {
    log_info "Setting up test models..."
    
    local model_info_file="$PROJECT_ROOT/shared/src/commonTest/resources/test-data/models/test_models_info.json"
    
    if [[ ! -f "$model_info_file" ]]; then
        log_error "Test model info file not found: $model_info_file"
        return 1
    fi
    
    # Create mock ONNX models for testing
    if [[ ! -f "$ONNX_CACHE_DIR/test_gemma_model.onnx" ]]; then
        log_info "Creating mock test models..."
        echo "Mock ONNX Gemma model for testing" > "$ONNX_CACHE_DIR/test_gemma_model.onnx"
        echo "Corrupted model content" > "$ONNX_CACHE_DIR/corrupted_model.onnx"
        # Create a larger mock model for memory testing
        dd if=/dev/zero of="$ONNX_CACHE_DIR/large_test_model.onnx" bs=1M count=50 2>/dev/null
        log_success "Mock test models created"
    else
        log_info "Using existing test models"
    fi
}

setup_memory_monitoring() {
    if [[ "$MEMORY_MONITORING" == "true" ]]; then
        log_info "Starting memory monitoring..."
        
        # Check if monitoring tools are available
        if command -v sar >/dev/null 2>&1; then
            sar -r 5 > "$TEST_RESULTS_DIR/memory_usage.log" 2>&1 &
            echo $! > "$TEST_RESULTS_DIR/memory_monitor.pid"
            log_success "Memory monitoring started"
        else
            log_warning "sar command not available, skipping memory monitoring"
        fi
    fi
}

stop_memory_monitoring() {
    if [[ -f "$TEST_RESULTS_DIR/memory_monitor.pid" ]]; then
        local pid=$(cat "$TEST_RESULTS_DIR/memory_monitor.pid")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" || true
            log_info "Memory monitoring stopped"
        fi
        rm -f "$TEST_RESULTS_DIR/memory_monitor.pid"
    fi
}

# Test execution functions
run_gradle_tests() {
    local test_type="$1"
    local gradle_task="$2"
    local timeout_minutes="${3:-60}"
    
    log_info "Running $test_type tests with task: $gradle_task (timeout: ${timeout_minutes}m)"
    
    # Prepare Gradle options
    local gradle_opts="$GRADLE_OPTS"
    gradle_opts="$gradle_opts -Dtest.type=$test_type"
    gradle_opts="$gradle_opts -Donnx.model.path=$ONNX_CACHE_DIR"
    gradle_opts="$gradle_opts -Dtest.output.dir=$TEST_RESULTS_DIR"
    
    if [[ "$VERBOSE" == "true" ]]; then
        gradle_opts="$gradle_opts --info --stacktrace"
    fi
    
    if [[ "$PARALLEL_EXECUTION" == "true" ]]; then
        gradle_opts="$gradle_opts --parallel"
    fi
    
    # Set environment variables
    export GRADLE_OPTS="$gradle_opts"
    export ONNX_MODEL_CACHE_PATH="$ONNX_CACHE_DIR"
    export AI_TEST_OUTPUT_DIR="$TEST_RESULTS_DIR"
    
    # Run the tests with timeout
    local start_time=$(date +%s)
    
    if timeout "${timeout_minutes}m" "$PROJECT_ROOT/gradlew" \
        -p "$PROJECT_ROOT" \
        clean "$gradle_task" \
        --continue \
        --no-daemon \
        $gradle_opts; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        log_success "$test_type tests completed successfully in ${duration}s"
        return 0
    else
        local exit_code=$?
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        if [[ $exit_code -eq 124 ]]; then
            log_error "$test_type tests timed out after ${timeout_minutes} minutes"
        else
            log_error "$test_type tests failed with exit code $exit_code after ${duration}s"
        fi
        return $exit_code
    fi
}

run_unit_tests() {
    run_gradle_tests "unit" "testDebugUnitTest" "30"
}

run_integration_tests() {
    run_gradle_tests "integration" "testIntegration" "60"
}

run_validation_tests() {
    run_gradle_tests "validation" "testValidation" "120"
}

run_performance_tests() {
    if [[ "$PERFORMANCE_PROFILING" == "true" ]]; then
        log_info "Enabling performance profiling"
        export GRADLE_OPTS="$GRADLE_OPTS -Dtest.profiling.enabled=true"
    fi
    
    run_gradle_tests "performance" "testPerformance" "180"
}

# Report generation
generate_test_report() {
    if [[ "$GENERATE_REPORT" != "true" ]]; then
        log_info "Skipping report generation"
        return 0
    fi
    
    log_info "Generating test reports..."
    
    local report_file="$REPORTS_DIR/ai-test-summary.html"
    local summary_file="$REPORTS_DIR/test-summary.md"
    
    # Create markdown summary
    cat > "$summary_file" << EOF
# AI Test Execution Summary

**Execution Date**: $(date -u)
**Test Level**: $TEST_LEVEL
**Project**: HazardHawk ONNX Gemma AI
**Commit**: $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

## Configuration
- Test Level: $TEST_LEVEL
- Parallel Execution: $PARALLEL_EXECUTION
- Memory Monitoring: $MEMORY_MONITORING
- Performance Profiling: $PERFORMANCE_PROFILING
- Verbose Output: $VERBOSE

## Results
EOF
    
    # Analyze test results
    local total_tests=0
    local failed_tests=0
    local skipped_tests=0
    local test_files_found=0
    
    for xml_file in $(find "$TEST_RESULTS_DIR" -name "TEST-*.xml" 2>/dev/null); do
        if [[ -f "$xml_file" ]]; then
            test_files_found=$((test_files_found + 1))
            local file_tests=$(grep -o 'tests="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")
            local file_failures=$(grep -o 'failures="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")
            local file_skipped=$(grep -o 'skipped="[0-9]*"' "$xml_file" | grep -o '[0-9]*' || echo "0")
            
            total_tests=$((total_tests + file_tests))
            failed_tests=$((failed_tests + file_failures))
            skipped_tests=$((skipped_tests + file_skipped))
        fi
    done
    
    local passed_tests=$((total_tests - failed_tests - skipped_tests))
    
    # Add results to summary
    cat >> "$summary_file" << EOF
- **Total Tests**: $total_tests
- **Passed**: $passed_tests
- **Failed**: $failed_tests
- **Skipped**: $skipped_tests
- **Test Files**: $test_files_found

EOF
    
    if [[ $failed_tests -eq 0 ]]; then
        echo "✅ **All tests passed successfully!**" >> "$summary_file"
    else
        echo "❌ **$failed_tests test(s) failed**" >> "$summary_file"
    fi
    
    # Add memory usage information if available
    if [[ -f "$TEST_RESULTS_DIR/memory_usage.log" ]]; then
        echo "" >> "$summary_file"
        echo "## Memory Usage" >> "$summary_file"
        echo "\`\`\`" >> "$summary_file"
        tail -10 "$TEST_RESULTS_DIR/memory_usage.log" >> "$summary_file" 2>/dev/null || echo "No memory data available" >> "$summary_file"
        echo "\`\`\`" >> "$summary_file"
    fi
    
    # Convert markdown to HTML (if pandoc is available)
    if command -v pandoc >/dev/null 2>&1; then
        pandoc "$summary_file" -o "$report_file" --standalone --metadata title="AI Test Report"
        log_success "HTML report generated: $report_file"
    else
        log_info "Pandoc not available, markdown report only: $summary_file"
    fi
    
    # Display summary
    echo ""
    log_info "Test Summary:"
    echo "  Total Tests: $total_tests"
    echo "  Passed: $passed_tests"
    echo "  Failed: $failed_tests"
    echo "  Skipped: $skipped_tests"
    
    return $failed_tests
}

# Cleanup function
cleanup() {
    log_info "Performing cleanup..."
    
    # Stop memory monitoring
    stop_memory_monitoring
    
    # Clean up temporary files if requested
    if [[ "$CLEANUP_AFTER" == "true" ]]; then
        log_info "Cleaning up temporary test files..."
        find "$PROJECT_ROOT" -name "*.tmp" -delete 2>/dev/null || true
        find "$PROJECT_ROOT" -name "hs_err_pid*.log" -delete 2>/dev/null || true
    fi
    
    log_info "Cleanup completed"
}

# Error handling
error_handler() {
    local exit_code=$?
    log_error "Script failed with exit code $exit_code"
    cleanup
    exit $exit_code
}

trap error_handler ERR
trap cleanup EXIT

# Main execution
main() {
    log_info "Starting AI test execution..."
    log_info "Test level: $TEST_LEVEL"
    log_info "Project root: $PROJECT_ROOT"
    
    # Setup
    setup_directories
    setup_test_models
    setup_memory_monitoring
    
    # Check if we're in a Kotlin Multiplatform project
    if [[ ! -f "$PROJECT_ROOT/gradlew" ]]; then
        log_error "No Gradle wrapper found. Are you in the correct directory?"
        exit 1
    fi
    
    local overall_result=0
    
    # Run tests based on level
    case $TEST_LEVEL in
        "unit")
            run_unit_tests || overall_result=$?
            ;;
        "integration")
            run_unit_tests || overall_result=$?
            if [[ $overall_result -eq 0 ]]; then
                run_integration_tests || overall_result=$?
            fi
            ;;
        "validation")
            run_unit_tests || overall_result=$?
            if [[ $overall_result -eq 0 ]]; then
                run_integration_tests || overall_result=$?
            fi
            if [[ $overall_result -eq 0 ]]; then
                run_validation_tests || overall_result=$?
            fi
            ;;
        "performance")
            run_performance_tests || overall_result=$?
            ;;
        "full")
            run_unit_tests || overall_result=$?
            if [[ $overall_result -eq 0 ]]; then
                run_integration_tests || overall_result=$?
            fi
            if [[ $overall_result -eq 0 ]]; then
                run_validation_tests || overall_result=$?
            fi
            if [[ $overall_result -eq 0 ]]; then
                run_performance_tests || overall_result=$?
            fi
            ;;
    esac
    
    # Generate reports
    generate_test_report
    
    if [[ $overall_result -eq 0 ]]; then
        log_success "All AI tests completed successfully!"
    else
        log_error "Some AI tests failed. Check the logs for details."
    fi
    
    return $overall_result
}

# Run main function
main "$@"