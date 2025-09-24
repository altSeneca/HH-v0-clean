#!/bin/bash

# Comprehensive AI Testing Execution Script
# Runs all AI tests with proper setup and reporting

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
REPORTS_DIR="$ROOT_DIR/build/reports/ai-tests"
TEST_DATA_DIR="$ROOT_DIR/shared/src/commonTest/resources/ai-test-data"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
TEST_SUITE="all"
GENERATE_TEST_DATA=false
RUN_PERFORMANCE_TESTS=true
RUN_E2E_TESTS=false
VERBOSE=false
COVERAGE=false
PARALLEL_JOBS=4

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --suite|-s)
            TEST_SUITE="$2"
            shift 2
            ;;
        --generate-data)
            GENERATE_TEST_DATA=true
            shift
            ;;
        --no-performance)
            RUN_PERFORMANCE_TESTS=false
            shift
            ;;
        --e2e)
            RUN_E2E_TESTS=true
            shift
            ;;
        --verbose|-v)
            VERBOSE=true
            shift
            ;;
        --coverage)
            COVERAGE=true
            shift
            ;;
        --parallel|-j)
            PARALLEL_JOBS="$2"
            shift 2
            ;;
        --help|-h)
            cat << EOF
Usage: $0 [OPTIONS]

Options:
    --suite, -s SUITE       Test suite to run (all, unit, integration, performance, e2e)
    --generate-data         Generate fresh test data before running tests
    --no-performance        Skip performance benchmark tests
    --e2e                   Include E2E tests (requires Android emulator)
    --verbose, -v           Enable verbose output
    --coverage              Generate code coverage reports
    --parallel, -j JOBS     Number of parallel test jobs (default: 4)
    --help, -h              Show this help message

Test Suites:
    all                     Run all AI tests (default)
    unit                    Unit tests only
    integration             Integration tests only
    performance             Performance benchmark tests only
    e2e                     End-to-end tests only

Examples:
    $0                      # Run all tests
    $0 --suite unit         # Run only unit tests
    $0 --generate-data      # Generate test data and run all tests
    $0 --e2e --verbose      # Run E2E tests with verbose output
    $0 --coverage           # Run tests with coverage reporting
EOF
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# Logging functions
log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}❌ $1${NC}"
}

log_section() {
    echo -e "\n${BLUE}=== $1 ===${NC}\n"
}

# Check prerequisites
check_prerequisites() {
    log_section "Checking Prerequisites"
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed or not in PATH"
        exit 1
    fi
    
    local java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f 2 | cut -d'.' -f 1)
    if [[ "$java_version" -lt 17 ]]; then
        log_error "Java 17 or higher is required (found: $java_version)"
        exit 1
    fi
    log_success "Java $java_version detected"
    
    # Check Gradle
    if [[ -f "$ROOT_DIR/gradlew" ]]; then
        log_success "Gradle wrapper found"
    else
        log_error "Gradle wrapper not found in $ROOT_DIR"
        exit 1
    fi
    
    # Check test data
    if [[ ! -d "$TEST_DATA_DIR" ]] || [[ "$GENERATE_TEST_DATA" == true ]]; then
        log_info "Test data missing or regeneration requested"
        GENERATE_TEST_DATA=true
    else
        log_success "Test data directory found"
    fi
    
    # Check for Android SDK if E2E tests are requested
    if [[ "$RUN_E2E_TESTS" == true ]]; then
        if [[ -z "$ANDROID_HOME" ]]; then
            log_error "ANDROID_HOME environment variable not set (required for E2E tests)"
            exit 1
        fi
        log_success "Android SDK detected at $ANDROID_HOME"
    fi
}

# Generate test data
generate_test_data() {
    if [[ "$GENERATE_TEST_DATA" == true ]]; then
        log_section "Generating AI Test Data"
        
        if [[ -f "$SCRIPT_DIR/generate_ai_test_data.sh" ]]; then
            "$SCRIPT_DIR/generate_ai_test_data.sh"
            log_success "Test data generated successfully"
        else
            log_error "Test data generation script not found"
            exit 1
        fi
    fi
}

# Setup test environment
setup_test_environment() {
    log_section "Setting up Test Environment"
    
    # Create reports directory
    mkdir -p "$REPORTS_DIR"
    
    # Set environment variables
    export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
    export AI_TEST_DATA_PATH="$TEST_DATA_DIR"
    export AI_MOCK_MODELS=true
    export AI_PERFORMANCE_TESTING="$RUN_PERFORMANCE_TESTS"
    
    if [[ "$VERBOSE" == true ]]; then
        export AI_TEST_VERBOSE=true
    fi
    
    if [[ "$COVERAGE" == true ]]; then
        export AI_TEST_COVERAGE=true
    fi
    
    log_success "Test environment configured"
}

# Run unit tests
run_unit_tests() {
    log_section "Running AI Unit Tests"
    
    local gradle_args=()
    gradle_args+=(":shared:testDebugUnitTest")
    
    # Add test filters based on verbose mode
    if [[ "$VERBOSE" == false ]]; then
        gradle_args+=("--quiet")
    fi
    
    # Add coverage if requested
    if [[ "$COVERAGE" == true ]]; then
        gradle_args+=("jacocoTestReport")
    fi
    
    # Add parallel execution
    gradle_args+=("--parallel" "--max-workers=$PARALLEL_JOBS")
    
    # Add specific test patterns
    gradle_args+=(
        "--tests" "*GemmaVisionAnalyzerTest*"
        "--tests" "*AIServiceFacadeTest*"
        "--tests" "*EnhancedAIServiceFacadeTest*"
        "--tests" "*AIErrorHandlingTest*"
        "--tests" "*TagRecommendationEngineTest*"
        "--tests" "*HazardTagMapperTest*"
        "--tests" "*ModelValidationTest*"
    )
    
    # Set test properties
    gradle_args+=(
        "-Ptest.mock.models=true"
        "-Ptest.include.performance=false"
        "-Ptest.timeout.extended=true"
    )
    
    cd "$ROOT_DIR"
    ./gradlew "${gradle_args[@]}"
    
    local exit_code=$?
    if [[ $exit_code -eq 0 ]]; then
        log_success "Unit tests passed"
    else
        log_error "Unit tests failed with exit code $exit_code"
        return $exit_code
    fi
}

# Run integration tests
run_integration_tests() {
    log_section "Running AI Integration Tests"
    
    local gradle_args=()
    gradle_args+=(":shared:testDebugUnitTest")
    
    if [[ "$VERBOSE" == false ]]; then
        gradle_args+=("--quiet")
    fi
    
    gradle_args+=("--parallel" "--max-workers=$PARALLEL_JOBS")
    
    # Integration test patterns
    gradle_args+=(
        "--tests" "*MultimodalAIIntegrationTest*"
        "--tests" "*CrossPlatformAITest*"
        "--tests" "*AIAnalysisIntegrationTest*"
        "--tests" "*YOLOToTagIntegrationTest*"
        "--tests" "*EnhancedTagSyncIntegrationTest*"
        "--tests" "*OSHAComplianceWorkflowIntegrationTest*"
    )
    
    gradle_args+=(
        "-Ptest.include.multimodal=true"
        "-Ptest.include.serialization=true"
        "-Ptest.mock.cloud.services=true"
        "-Ptest.timeout.extended=true"
    )
    
    cd "$ROOT_DIR"
    ./gradlew "${gradle_args[@]}"
    
    local exit_code=$?
    if [[ $exit_code -eq 0 ]]; then
        log_success "Integration tests passed"
    else
        log_error "Integration tests failed with exit code $exit_code"
        return $exit_code
    fi
}

# Run performance tests
run_performance_tests() {
    if [[ "$RUN_PERFORMANCE_TESTS" == false ]]; then
        log_warning "Performance tests skipped"
        return 0
    fi
    
    log_section "Running AI Performance Benchmark Tests"
    
    local gradle_args=()
    gradle_args+=(":shared:testDebugUnitTest")
    
    if [[ "$VERBOSE" == false ]]; then
        gradle_args+=("--quiet")
    fi
    
    # Performance tests need more time
    gradle_args+=("--max-workers=2") # Limit parallelism for accurate performance measurement
    
    # Performance test patterns
    gradle_args+=(
        "--tests" "*AIPerformanceBenchmarkTest*"
        "--tests" "*PerformanceBenchmarkTest*"
        "--tests" "*AIPerformanceTest*"
    )
    
    gradle_args+=(
        "-Ptest.performance.speed=true"
        "-Ptest.performance.memory=true"
        "-Ptest.performance.scalability=true"
        "-Ptest.performance.battery=true"
        "-Ptest.timeout.performance=300000" # 5 minutes
        "-Ptest.memory.monitoring=true"
        "-Ptest.load.testing=true"
        "-Ptest.battery.monitoring=true"
    )
    
    cd "$ROOT_DIR"
    ./gradlew "${gradle_args[@]}"
    
    local exit_code=$?
    if [[ $exit_code -eq 0 ]]; then
        log_success "Performance tests passed"
    else
        log_error "Performance tests failed with exit code $exit_code"
        return $exit_code
    fi
}

# Run E2E tests
run_e2e_tests() {
    if [[ "$RUN_E2E_TESTS" == false ]]; then
        log_warning "E2E tests skipped"
        return 0
    fi
    
    log_section "Running AI End-to-End Tests"
    
    # Check if emulator is running
    if ! adb devices | grep -q "device"; then
        log_error "No Android device/emulator detected. Please start an emulator or connect a device."
        return 1
    fi
    
    local gradle_args=()
    gradle_args+=(":HazardHawk:androidApp:connectedDebugAndroidTest")
    
    if [[ "$VERBOSE" == false ]]; then
        gradle_args+=("--quiet")
    fi
    
    gradle_args+=("--parallel" "--max-workers=1") # E2E tests should not be parallelized
    
    # E2E test patterns
    gradle_args+=(
        "--tests" "*CameraToAIWorkflowE2ETest*"
        "--tests" "*AIAnalysisUIComponentsTest*"
        "--tests" "*CameraAIIntegrationTest*"
    )
    
    gradle_args+=(
        "-Pandroid.testInstrumentationRunnerArguments.notAnnotation=androidx.test.filters.FlakyTest"
    )
    
    cd "$ROOT_DIR"
    ./gradlew "${gradle_args[@]}"
    
    local exit_code=$?
    if [[ $exit_code -eq 0 ]]; then
        log_success "E2E tests passed"
    else
        log_error "E2E tests failed with exit code $exit_code"
        return $exit_code
    fi
}

# Generate test report
generate_test_report() {
    log_section "Generating Test Reports"
    
    local report_file="$REPORTS_DIR/ai_test_summary.md"
    local timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
    
    cat > "$report_file" << EOF
# AI Testing Summary Report

Generated: $timestamp  
Test Suite: $TEST_SUITE  
Version: $(git rev-parse --short HEAD 2>/dev/null || echo "unknown")

## Test Configuration

- Parallel Jobs: $PARALLEL_JOBS
- Performance Tests: $RUN_PERFORMANCE_TESTS
- E2E Tests: $RUN_E2E_TESTS
- Coverage: $COVERAGE
- Verbose: $VERBOSE

## Test Results Summary

EOF
    
    # Count test results if available
    if find "$ROOT_DIR" -name "TEST-*.xml" -type f >/dev/null 2>&1; then
        local total_tests=$(find "$ROOT_DIR" -name "TEST-*.xml" -exec grep -o "testcase" {} \; | wc -l | tr -d ' ')
        local failed_tests=$(find "$ROOT_DIR" -name "TEST-*.xml" -exec grep -o 'failure\|error' {} \; | wc -l | tr -d ' ')
        local passed_tests=$((total_tests - failed_tests))
        
        cat >> "$report_file" << EOF
- **Total Tests**: $total_tests
- **Passed**: $passed_tests
- **Failed**: $failed_tests
- **Success Rate**: $(( passed_tests * 100 / total_tests ))%

EOF
    else
        echo "- **Test Results**: XML files not found" >> "$report_file"
    fi
    
    # Add coverage information if available
    if [[ "$COVERAGE" == true ]]; then
        echo "## Coverage Information" >> "$report_file"
        echo "" >> "$report_file"
        
        local coverage_file="$ROOT_DIR/shared/build/reports/jacoco/test/html/index.html"
        if [[ -f "$coverage_file" ]]; then
            echo "Coverage report generated: [View HTML Report](../shared/build/reports/jacoco/test/html/index.html)" >> "$report_file"
        else
            echo "Coverage report not found" >> "$report_file"
        fi
        echo "" >> "$report_file"
    fi
    
    # Add performance results if available
    if [[ "$RUN_PERFORMANCE_TESTS" == true ]]; then
        echo "## Performance Results" >> "$report_file"
        echo "" >> "$report_file"
        echo "Performance benchmark results are available in the individual test reports." >> "$report_file"
        echo "" >> "$report_file"
    fi
    
    log_success "Test report generated: $report_file"
}

# Cleanup function
cleanup() {
    local exit_code=$?
    
    if [[ $exit_code -eq 0 ]]; then
        log_success "All tests completed successfully!"
    else
        log_error "Tests failed with exit code $exit_code"
    fi
    
    # Archive test results
    if [[ -d "$ROOT_DIR/shared/build/test-results" ]]; then
        cp -r "$ROOT_DIR/shared/build/test-results" "$REPORTS_DIR/" 2>/dev/null || true
    fi
    
    if [[ -d "$ROOT_DIR/HazardHawk/androidApp/build/reports/androidTests" ]]; then
        cp -r "$ROOT_DIR/HazardHawk/androidApp/build/reports/androidTests" "$REPORTS_DIR/android-tests/" 2>/dev/null || true
    fi
    
    exit $exit_code
}

# Set trap for cleanup
trap cleanup EXIT

# Main execution
main() {
    log_section "AI Testing Execution Started"
    log_info "Test Suite: $TEST_SUITE"
    log_info "Root Directory: $ROOT_DIR"
    log_info "Reports Directory: $REPORTS_DIR"
    
    check_prerequisites
    generate_test_data
    setup_test_environment
    
    local overall_exit_code=0
    
    # Run tests based on suite selection
    case "$TEST_SUITE" in
        "all")
            run_unit_tests || overall_exit_code=1
            run_integration_tests || overall_exit_code=1
            run_performance_tests || overall_exit_code=1
            run_e2e_tests || overall_exit_code=1
            ;;
        "unit")
            run_unit_tests || overall_exit_code=1
            ;;
        "integration")
            run_integration_tests || overall_exit_code=1
            ;;
        "performance")
            run_performance_tests || overall_exit_code=1
            ;;
        "e2e")
            run_e2e_tests || overall_exit_code=1
            ;;
        *)
            log_error "Unknown test suite: $TEST_SUITE"
            log_info "Valid options: all, unit, integration, performance, e2e"
            exit 1
            ;;
    esac
    
    generate_test_report
    
    return $overall_exit_code
}

# Execute main function
main "$@"
