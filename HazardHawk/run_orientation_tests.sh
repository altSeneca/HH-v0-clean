#!/bin/bash

# HazardHawk Image Orientation Testing Script
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

APK_PATH="androidApp/build/outputs/apk/debug/androidApp-debug.apk"
TEST_APK_PATH="androidApp/build/outputs/apk/androidTest/debug/androidApp-debug-androidTest.apk"
PACKAGE_NAME="com.hazardhawk"
TEST_PACKAGE_NAME="com.hazardhawk.test"

DEVICE_ID=""
TEST_OUTPUT_DIR="./test-results/orientation-tests"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$TEST_OUTPUT_DIR/test-log-$TIMESTAMP.txt"

print_header() {
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE} HazardHawk Orientation Testing Suite${NC}"
    echo -e "${BLUE}======================================${NC}"
}

print_section() {
    echo -e "\n${YELLOW}>>> $1 <<<${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

setup_test_environment() {
    print_section "Setting up test environment"
    
    mkdir -p "$TEST_OUTPUT_DIR"
    
    echo "HazardHawk Orientation Test Run - $TIMESTAMP" > "$LOG_FILE"
    echo "========================================" >> "$LOG_FILE"
    
    CONNECTED_DEVICES=$(adb devices | grep -v "List of devices attached" | grep "device" | wc -l)
    if [ "$CONNECTED_DEVICES" -eq 0 ]; then
        print_error "No Android device connected"
        exit 1
    fi
    
    if [ -z "$DEVICE_ID" ]; then
        DEVICE_ID=$(adb devices | grep "device" | head -n 1 | cut -f1)
    fi
    
    print_success "Connected to device: $DEVICE_ID"
    
    DEVICE_MODEL=$(adb -s "$DEVICE_ID" shell getprop ro.product.model)
    ANDROID_VERSION=$(adb -s "$DEVICE_ID" shell getprop ro.build.version.release)
    
    echo "Device Model: $DEVICE_MODEL" >> "$LOG_FILE"
    echo "Android Version: $ANDROID_VERSION" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
    
    print_success "Test environment ready"
}

build_application() {
    print_section "Building application and tests"
    
    ./gradlew clean >> "$LOG_FILE" 2>&1
    if [ $? -ne 0 ]; then
        print_error "Clean build failed"
        exit 1
    fi
    
    ./gradlew :androidApp:assembleDebug >> "$LOG_FILE" 2>&1
    if [ $? -ne 0 ]; then
        print_error "Debug APK build failed"
        exit 1
    fi
    
    ./gradlew :androidApp:assembleDebugAndroidTest >> "$LOG_FILE" 2>&1
    if [ $? -ne 0 ]; then
        print_error "Test APK build failed"
        exit 1
    fi
    
    print_success "Application and tests built successfully"
}

install_application() {
    print_section "Installing application on device"
    
    adb -s "$DEVICE_ID" uninstall "$PACKAGE_NAME" >> "$LOG_FILE" 2>&1 || true
    adb -s "$DEVICE_ID" uninstall "$TEST_PACKAGE_NAME" >> "$LOG_FILE" 2>&1 || true
    
    adb -s "$DEVICE_ID" install "$APK_PATH" >> "$LOG_FILE" 2>&1
    if [ $? -ne 0 ]; then
        print_error "Failed to install main APK"
        exit 1
    fi
    
    adb -s "$DEVICE_ID" install "$TEST_APK_PATH" >> "$LOG_FILE" 2>&1
    if [ $? -ne 0 ]; then
        print_error "Failed to install test APK"
        exit 1
    fi
    
    print_success "Application installed successfully"
}

run_unit_tests() {
    print_section "Running unit tests"
    
    echo "=== UNIT TESTS ===" >> "$LOG_FILE"
    
    ./gradlew :androidApp:testDebugUnitTest --tests "com.hazardhawk.camera.orientation.PhotoOrientationManagerTest" >> "$LOG_FILE" 2>&1
    if [ $? -eq 0 ]; then
        print_success "PhotoOrientationManager unit tests passed"
    else
        print_error "PhotoOrientationManager unit tests failed"
    fi
    
    ./gradlew :androidApp:testDebugUnitTest --tests "com.hazardhawk.camera.orientation.MetadataEmbedderOrientationTest" >> "$LOG_FILE" 2>&1
    if [ $? -eq 0 ]; then
        print_success "MetadataEmbedder unit tests passed"
    else
        print_error "MetadataEmbedder unit tests failed"
    fi
    
    echo "" >> "$LOG_FILE"
}

run_integration_tests() {
    print_section "Running integration tests on device"
    
    echo "=== INTEGRATION TESTS ===" >> "$LOG_FILE"
    
    adb -s "$DEVICE_ID" shell am instrument -w \
        -e class com.hazardhawk.camera.orientation.OrientationIntegrationTest \
        "$TEST_PACKAGE_NAME/androidx.test.runner.AndroidJUnitRunner" >> "$LOG_FILE" 2>&1
    
    if [ $? -eq 0 ]; then
        print_success "Orientation integration tests passed"
    else
        print_error "Orientation integration tests failed"
    fi
    
    echo "" >> "$LOG_FILE"
}

run_performance_tests() {
    print_section "Running performance benchmarks"
    
    echo "=== PERFORMANCE TESTS ===" >> "$LOG_FILE"
    
    adb -s "$DEVICE_ID" shell am force-stop "$PACKAGE_NAME" >> "$LOG_FILE" 2>&1 || true
    sleep 2
    
    adb -s "$DEVICE_ID" shell am instrument -w \
        -e class com.hazardhawk.camera.orientation.OrientationPerformanceBenchmarkTest \
        "$TEST_PACKAGE_NAME/androidx.test.runner.AndroidJUnitRunner" >> "$LOG_FILE" 2>&1
    
    if [ $? -eq 0 ]; then
        print_success "Performance benchmarks completed"
    else
        print_error "Performance benchmarks failed"
    fi
    
    echo "" >> "$LOG_FILE"
}

run_visual_regression_tests() {
    print_section "Running visual regression tests"
    
    echo "=== VISUAL REGRESSION TESTS ===" >> "$LOG_FILE"
    
    adb -s "$DEVICE_ID" shell am instrument -w \
        -e class com.hazardhawk.camera.orientation.VisualRegressionTest \
        "$TEST_PACKAGE_NAME/androidx.test.runner.AndroidJUnitRunner" >> "$LOG_FILE" 2>&1
    
    if [ $? -eq 0 ]; then
        print_success "Visual regression tests passed"
    else
        print_warning "Visual regression tests completed (may require manual review)"
    fi
    
    echo "" >> "$LOG_FILE"
}

collect_test_artifacts() {
    print_section "Collecting test artifacts"
    
    DEVICE_ARTIFACTS_DIR="/sdcard/Android/data/$PACKAGE_NAME/files"
    LOCAL_ARTIFACTS_DIR="$TEST_OUTPUT_DIR/artifacts-$TIMESTAMP"
    mkdir -p "$LOCAL_ARTIFACTS_DIR"
    
    adb -s "$DEVICE_ID" pull "$DEVICE_ARTIFACTS_DIR" "$LOCAL_ARTIFACTS_DIR" >> "$LOG_FILE" 2>&1 || true
    
    adb -s "$DEVICE_ID" logcat -d > "$TEST_OUTPUT_DIR/logcat-$TIMESTAMP.txt"
    
    if [ -d "androidApp/build/reports/tests" ]; then
        cp -r "androidApp/build/reports/tests" "$TEST_OUTPUT_DIR/gradle-reports-$TIMESTAMP"
    fi
    
    print_success "Test artifacts collected"
}

generate_test_report() {
    print_section "Generating test report"
    
    REPORT_FILE="$TEST_OUTPUT_DIR/test-report-$TIMESTAMP.html"
    
    cat > "$REPORT_FILE" << 'EOFHTML'
<!DOCTYPE html>
<html>
<head>
    <title>HazardHawk Orientation Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .header { background-color: #f0f0f0; padding: 20px; border-radius: 5px; }
        .section { margin: 20px 0; }
        .success { color: green; }
        .error { color: red; }
        .warning { color: orange; }
        pre { background-color: #f8f8f8; padding: 10px; border-radius: 3px; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="header">
        <h1>HazardHawk Image Orientation Test Report</h1>
        <p><strong>Test Run:</strong> TIMESTAMP_PLACEHOLDER</p>
        <p><strong>Device:</strong> DEVICE_MODEL_PLACEHOLDER</p>
        <p><strong>Device ID:</strong> DEVICE_ID_PLACEHOLDER</p>
    </div>

    <div class="section">
        <h2>Test Results Summary</h2>
        <p>Detailed results are available in the log file: <code>test-log-TIMESTAMP_PLACEHOLDER.txt</code></p>
    </div>

    <div class="section">
        <h2>Test Categories</h2>
        <ul>
            <li>Unit Tests - Core orientation and watermark logic</li>
            <li>Integration Tests - End-to-end orientation workflows</li>
            <li>Performance Tests - Processing time and memory benchmarks</li>
            <li>Visual Regression Tests - Watermark consistency validation</li>
        </ul>
    </div>

    <div class="section">
        <h2>Artifacts</h2>
        <ul>
            <li><a href="test-log-TIMESTAMP_PLACEHOLDER.txt">Complete Test Log</a></li>
            <li><a href="logcat-TIMESTAMP_PLACEHOLDER.txt">Device Logcat</a></li>
            <li><a href="artifacts-TIMESTAMP_PLACEHOLDER/">Test Screenshots and Images</a></li>
        </ul>
    </div>

    <div class="section">
        <h2>Performance Benchmarks</h2>
        <p>Check the log file for detailed performance metrics including:</p>
        <ul>
            <li>Photo processing times by image size</li>
            <li>Memory usage during orientation transforms</li>
            <li>Batch processing performance</li>
            <li>Memory leak detection results</li>
        </ul>
    </div>
</body>
</html>
EOFHTML

    # Replace placeholders
    sed -i "s/TIMESTAMP_PLACEHOLDER/$TIMESTAMP/g" "$REPORT_FILE"
    sed -i "s/DEVICE_MODEL_PLACEHOLDER/$DEVICE_MODEL/g" "$REPORT_FILE" 
    sed -i "s/DEVICE_ID_PLACEHOLDER/$DEVICE_ID/g" "$REPORT_FILE"
    
    print_success "Test report generated: $REPORT_FILE"
}

cleanup() {
    print_section "Cleaning up"
    
    adb -s "$DEVICE_ID" shell am force-stop "$PACKAGE_NAME" >> "$LOG_FILE" 2>&1 || true
    
    print_success "Cleanup completed"
}

main() {
    print_header
    
    while [[ $# -gt 0 ]]; do
        case $1 in
            -d|--device)
                DEVICE_ID="$2"
                shift 2
                ;;
            -s|--skip-build)
                SKIP_BUILD=true
                shift
                ;;
            -u|--unit-only)
                UNIT_ONLY=true
                shift
                ;;
            -h|--help)
                echo "Usage: $0 [OPTIONS]"
                echo "Options:"
                echo "  -d, --device ID     Specify device ID"
                echo "  -s, --skip-build    Skip building APKs"
                echo "  -u, --unit-only     Run only unit tests"
                echo "  -h, --help          Show this help message"
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                exit 1
                ;;
        esac
    done
    
    setup_test_environment
    
    if [ "$SKIP_BUILD" != "true" ]; then
        build_application
        install_application
    fi
    
    run_unit_tests
    
    if [ "$UNIT_ONLY" != "true" ]; then
        run_integration_tests
        run_performance_tests
        run_visual_regression_tests
        collect_test_artifacts
    fi
    
    generate_test_report
    cleanup
    
    print_section "Test run completed!"
    echo -e "Results available in: ${BLUE}$TEST_OUTPUT_DIR${NC}"
    echo -e "View report: ${BLUE}$TEST_OUTPUT_DIR/test-report-$TIMESTAMP.html${NC}"
}

main "$@"
