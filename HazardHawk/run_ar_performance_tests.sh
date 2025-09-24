#!/bin/bash

# AR Performance Test Suite Runner
# Comprehensive performance testing for AR safety monitoring system

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
TEST_DEVICE=${TEST_DEVICE:-""}
TEST_VARIANT=${TEST_VARIANT:-"debug"}
PERFORMANCE_THRESHOLD=${PERFORMANCE_THRESHOLD:-16.67} # 60 FPS target
MEMORY_THRESHOLD=${MEMORY_THRESHOLD:-500} # 500MB max
MIN_COVERAGE=${MIN_COVERAGE:-85} # 85% minimum coverage

# Logging
LOG_DIR="./test-results/ar-performance"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$LOG_DIR/ar_performance_test_$TIMESTAMP.log"

echo -e "${BLUE}=== HazardHawk AR Performance Test Suite ===${NC}"
echo "Started at: $(date)"
echo "Log file: $LOG_FILE"

# Create log directory
mkdir -p "$LOG_DIR"

# Logging function
log() {
    echo -e "$1" | tee -a "$LOG_FILE"
}

# Error handling
handle_error() {
    log "${RED}Error occurred in: $1${NC}"
    log "${RED}Exit code: $2${NC}"
    exit $2
}

# Check prerequisites
check_prerequisites() {
    log "${BLUE}Checking prerequisites...${NC}"
    
    # Check if Android SDK is available
    if ! command -v adb &> /dev/null; then
        log "${RED}ADB not found. Please install Android SDK.${NC}"
        exit 1
    fi
    
    # Check if device/emulator is connected
    if [ -z "$TEST_DEVICE" ]; then
        DEVICE_COUNT=$(adb devices | grep -v "List of devices" | grep "device" | wc -l)
        if [ "$DEVICE_COUNT" -eq 0 ]; then
            log "${RED}No Android devices connected. Please connect a device or start an emulator.${NC}"
            exit 1
        elif [ "$DEVICE_COUNT" -gt 1 ]; then
            log "${YELLOW}Multiple devices connected. Use TEST_DEVICE=<device_id> to specify.${NC}"
            adb devices
            exit 1
        fi
    else
        # Verify specified device exists
        if ! adb devices | grep -q "$TEST_DEVICE"; then
            log "${RED}Device $TEST_DEVICE not found.${NC}"
            adb devices
            exit 1
        fi
        ADB_OPTIONS="-s $TEST_DEVICE"
    fi
    
    # Check if gradlew exists
    if [ ! -f "./gradlew" ]; then
        log "${RED}gradlew not found. Please run from project root.${NC}"
        exit 1
    fi
    
    log "${GREEN}Prerequisites check passed.${NC}"
}

# Build the app
build_app() {
    log "${BLUE}Building HazardHawk AR test app...${NC}"
    
    ./gradlew clean || handle_error "Clean failed" $?
    ./gradlew :androidApp:assemble${TEST_VARIANT^} || handle_error "Build failed" $?
    ./gradlew :androidApp:assemble${TEST_VARIANT^}AndroidTest || handle_error "Test build failed" $?
    
    log "${GREEN}Build completed successfully.${NC}"
}

# Install the app
install_app() {
    log "${BLUE}Installing AR test app...${NC}"
    
    adb $ADB_OPTIONS install -r "./androidApp/build/outputs/apk/$TEST_VARIANT/androidApp-$TEST_VARIANT.apk" || handle_error "App install failed" $?
    adb $ADB_OPTIONS install -r "./androidApp/build/outputs/apk/androidTest/$TEST_VARIANT/androidApp-$TEST_VARIANT-androidTest.apk" || handle_error "Test APK install failed" $?
    
    log "${GREEN}App installation completed.${NC}"
}

# Run unit tests
run_unit_tests() {
    log "${BLUE}Running AR unit tests...${NC}"
    
    ./gradlew :androidApp:test${TEST_VARIANT^}UnitTest || handle_error "Unit tests failed" $?
    
    # Copy test results
    cp -r "./androidApp/build/reports/tests/test${TEST_VARIANT^}UnitTest" "$LOG_DIR/unit-test-reports/" 2>/dev/null || true
    
    log "${GREEN}Unit tests completed.${NC}"
}

# Run AR Camera performance tests
run_camera_performance_tests() {
    log "${BLUE}Running AR camera performance tests...${NC}"
    
    # Clear logcat
    adb $ADB_OPTIONS logcat -c
    
    # Run camera integration tests
    adb $ADB_OPTIONS shell am instrument -w \
        -e class "com.hazardhawk.ar.ARCameraIntegrationTest" \
        com.hazardhawk.debug.test/androidx.test.runner.AndroidJUnitRunner || handle_error "Camera tests failed" $?
    
    # Capture performance logs
    adb $ADB_OPTIONS logcat -d -s "ARPerformance" > "$LOG_DIR/camera_performance_$TIMESTAMP.log"
    
    log "${GREEN}Camera performance tests completed.${NC}"
}

# Run AR Overlay performance tests
run_overlay_performance_tests() {
    log "${BLUE}Running AR overlay performance tests...${NC}"
    
    # Clear logcat
    adb $ADB_OPTIONS logcat -c
    
    # Run overlay integration tests
    adb $ADB_OPTIONS shell am instrument -w \
        -e class "com.hazardhawk.ar.AROverlayIntegrationTest" \
        com.hazardhawk.debug.test/androidx.test.runner.AndroidJUnitRunner || handle_error "Overlay tests failed" $?
    
    # Capture performance logs
    adb $ADB_OPTIONS logcat -d -s "AROverlay" > "$LOG_DIR/overlay_performance_$TIMESTAMP.log"
    
    log "${GREEN}Overlay performance tests completed.${NC}"
}

# Run end-to-end AR tests
run_e2e_ar_tests() {
    log "${BLUE}Running end-to-end AR hazard detection tests...${NC}"
    
    # Clear logcat
    adb $ADB_OPTIONS logcat -c
    
    # Run hazard detection tests
    adb $ADB_OPTIONS shell am instrument -w \
        -e class "com.hazardhawk.ar.ARHazardDetectionTest" \
        com.hazardhawk.debug.test/androidx.test.runner.AndroidJUnitRunner || handle_error "E2E AR tests failed" $?
    
    # Capture performance logs
    adb $ADB_OPTIONS logcat -d -s "ARHazardDetection" > "$LOG_DIR/hazard_detection_$TIMESTAMP.log"
    
    log "${GREEN}End-to-end AR tests completed.${NC}"
}

# Run performance benchmarks
run_performance_benchmarks() {
    log "${BLUE}Running AR performance benchmarks...${NC}"
    
    # Clear logcat for benchmark logs
    adb $ADB_OPTIONS logcat -c
    
    # Run comprehensive performance tests
    adb $ADB_OPTIONS shell am instrument -w \
        -e class "com.hazardhawk.ar.ARPerformanceTest" \
        -e androidx.benchmark.output.enable true \
        com.hazardhawk.debug.test/androidx.test.runner.AndroidJUnitRunner || handle_error "Performance benchmarks failed" $?
    
    # Pull benchmark results
    adb $ADB_OPTIONS pull /storage/emulated/0/Android/data/com.hazardhawk.debug/files/benchmark-reports/ "$LOG_DIR/benchmark-reports/" 2>/dev/null || true
    
    # Capture benchmark logs
    adb $ADB_OPTIONS logcat -d -s "Benchmark" > "$LOG_DIR/benchmark_results_$TIMESTAMP.log"
    
    log "${GREEN}Performance benchmarks completed.${NC}"
}

# Analyze performance metrics
analyze_performance() {
    log "${BLUE}Analyzing performance metrics...${NC}"
    
    # Create performance report
    PERF_REPORT="$LOG_DIR/performance_analysis_$TIMESTAMP.md"
    
    cat > "$PERF_REPORT" << EOL
# AR Performance Test Analysis Report

**Test Run:** $TIMESTAMP  
**Device:** $(adb $ADB_OPTIONS shell getprop ro.product.model 2>/dev/null || echo "Unknown")  
**Android Version:** $(adb $ADB_OPTIONS shell getprop ro.build.version.release 2>/dev/null || echo "Unknown")  

## Performance Targets
- Frame Rate: >= 30 FPS (≤ 33.33ms per frame)
- Analysis Latency: ≤ 2000ms
- Memory Usage: ≤ ${MEMORY_THRESHOLD}MB
- Test Coverage: >= ${MIN_COVERAGE}%

## Test Results

EOL

    # Analyze frame rate performance
    if [ -f "$LOG_DIR/camera_performance_$TIMESTAMP.log" ]; then
        FRAME_TIMES=$(grep "FrameTime:" "$LOG_DIR/camera_performance_$TIMESTAMP.log" | awk '{print $2}' | sort -n)
        if [ ! -z "$FRAME_TIMES" ]; then
            AVG_FRAME_TIME=$(echo "$FRAME_TIMES" | awk '{sum+=$1} END {print sum/NR}')
            MAX_FRAME_TIME=$(echo "$FRAME_TIMES" | tail -1)
            
            echo "### Camera Performance" >> "$PERF_REPORT"
            echo "- Average Frame Time: ${AVG_FRAME_TIME}ms" >> "$PERF_REPORT"
            echo "- Maximum Frame Time: ${MAX_FRAME_TIME}ms" >> "$PERF_REPORT"
            
            # Check if performance meets targets
            if (( $(echo "$AVG_FRAME_TIME > $PERFORMANCE_THRESHOLD" | bc -l) )); then
                echo "- ❌ **PERFORMANCE ISSUE**: Average frame time exceeds target" >> "$PERF_REPORT"
                PERFORMANCE_ISSUES=true
            else
                echo "- ✅ Frame rate performance within target" >> "$PERF_REPORT"
            fi
            echo "" >> "$PERF_REPORT"
        fi
    fi
    
    # Analyze memory usage
    MEMORY_USAGE=$(adb $ADB_OPTIONS shell dumpsys meminfo com.hazardhawk.debug | grep "TOTAL" | awk '{print $2}' | head -1)
    if [ ! -z "$MEMORY_USAGE" ]; then
        MEMORY_MB=$((MEMORY_USAGE / 1024))
        echo "### Memory Usage" >> "$PERF_REPORT"
        echo "- Current Memory Usage: ${MEMORY_MB}MB" >> "$PERF_REPORT"
        
        if [ "$MEMORY_MB" -gt "$MEMORY_THRESHOLD" ]; then
            echo "- ❌ **MEMORY ISSUE**: Usage exceeds threshold" >> "$PERF_REPORT"
            PERFORMANCE_ISSUES=true
        else
            echo "- ✅ Memory usage within target" >> "$PERF_REPORT"
        fi
        echo "" >> "$PERF_REPORT"
    fi
    
    # Check test coverage
    if [ -f "./androidApp/build/reports/kover/htmlReport/index.html" ]; then
        COVERAGE=$(grep -o 'Total[^%]*%' "./androidApp/build/reports/kover/htmlReport/index.html" | grep -o '[0-9]*' | head -1)
        if [ ! -z "$COVERAGE" ]; then
            echo "### Test Coverage" >> "$PERF_REPORT"
            echo "- Overall Coverage: ${COVERAGE}%" >> "$PERF_REPORT"
            
            if [ "$COVERAGE" -lt "$MIN_COVERAGE" ]; then
                echo "- ❌ **COVERAGE ISSUE**: Below minimum threshold" >> "$PERF_REPORT"
                PERFORMANCE_ISSUES=true
            else
                echo "- ✅ Coverage meets requirements" >> "$PERF_REPORT"
            fi
            echo "" >> "$PERF_REPORT"
        fi
    fi
    
    # Summary
    echo "## Summary" >> "$PERF_REPORT"
    if [ "$PERFORMANCE_ISSUES" = true ]; then
        echo "❌ **Performance issues detected. Review details above.**" >> "$PERF_REPORT"
        log "${YELLOW}Performance analysis complete with issues. See: $PERF_REPORT${NC}"
    else
        echo "✅ **All performance targets met.**" >> "$PERF_REPORT"
        log "${GREEN}Performance analysis complete. All targets met. See: $PERF_REPORT${NC}"
    fi
}

# Generate test coverage report
generate_coverage_report() {
    log "${BLUE}Generating test coverage report...${NC}"
    
    # Generate Kover coverage report
    ./gradlew koverHtmlReport || log "${YELLOW}Coverage report generation failed${NC}"
    
    # Copy coverage reports
    cp -r "./androidApp/build/reports/kover/" "$LOG_DIR/coverage-reports/" 2>/dev/null || true
    
    log "${GREEN}Coverage report generated.${NC}"
}

# Clean up
cleanup() {
    log "${BLUE}Cleaning up...${NC}"
    
    # Clear logcat
    adb $ADB_OPTIONS logcat -c 2>/dev/null || true
    
    # Optionally uninstall test app (uncomment if needed)
    # adb $ADB_OPTIONS uninstall com.hazardhawk.debug 2>/dev/null || true
    # adb $ADB_OPTIONS uninstall com.hazardhawk.debug.test 2>/dev/null || true
    
    log "${GREEN}Cleanup completed.${NC}"
}

# Main execution
main() {
    log "${BLUE}Starting AR Performance Test Suite...${NC}"
    
    check_prerequisites
    build_app
    install_app
    
    # Run test suites
    run_unit_tests
    run_camera_performance_tests
    run_overlay_performance_tests
    run_e2e_ar_tests
    run_performance_benchmarks
    
    # Analysis and reporting
    generate_coverage_report
    analyze_performance
    
    cleanup
    
    log "${GREEN}=== AR Performance Test Suite Completed ===${NC}"
    log "Results available in: $LOG_DIR"
    
    # Exit with error if performance issues detected
    if [ "$PERFORMANCE_ISSUES" = true ]; then
        log "${RED}Performance issues detected. Check the analysis report.${NC}"
        exit 1
    fi
    
    log "${GREEN}All tests passed successfully!${NC}"
}

# Handle script interruption
trap 'log "${RED}Test suite interrupted.${NC}"; cleanup; exit 130' INT TERM

# Run main function
main "$@"
