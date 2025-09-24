#!/bin/bash

# HazardHawk Validation Test Runner
# Executes comprehensive validation tests for critical functions

set -e  # Exit on any error

echo "========================================"
echo "🔧 HazardHawk Validation Test Suite"
echo "========================================"
echo ""

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
TEST_TIMEOUT=300  # 5 minutes timeout per test
REPORT_DIR="test_reports/$(date +'%Y%m%d_%H%M%S')"
ERROR_COUNT=0
TEST_COUNT=0

# Create report directory
mkdir -p "$REPORT_DIR"

echo -e "${BLUE}📁 Test reports will be saved to: $REPORT_DIR${NC}"
echo ""

# Function to log test results
log_test_result() {
    local test_name="$1"
    local result="$2"
    local details="$3"
    
    TEST_COUNT=$((TEST_COUNT + 1))
    
    if [ "$result" = "PASS" ]; then
        echo -e "${GREEN}✅ $test_name: PASS${NC}"
    else
        echo -e "${RED}❌ $test_name: FAIL - $details${NC}"
        ERROR_COUNT=$((ERROR_COUNT + 1))
    fi
    
    echo "$test_name,$result,$details" >> "$REPORT_DIR/test_results.csv"
}

# Function to check if Android device is connected
check_device() {
    echo -e "${BLUE}🔍 Checking for connected Android device...${NC}"
    
    if ! command -v adb &> /dev/null; then
        echo -e "${RED}❌ ADB not found. Please install Android SDK tools.${NC}"
        exit 1
    fi
    
    DEVICE_COUNT=$(adb devices | grep -c "device$" || true)
    
    if [ "$DEVICE_COUNT" -eq 0 ]; then
        echo -e "${RED}❌ No Android device connected. Please connect a device or start an emulator.${NC}"
        exit 1
    fi
    
    DEVICE_ID=$(adb devices | grep "device$" | head -1 | cut -f1)
    echo -e "${GREEN}✅ Device connected: $DEVICE_ID${NC}"
    
    # Check if device is ready
    adb -s "$DEVICE_ID" wait-for-device
    echo -e "${GREEN}✅ Device ready${NC}"
    echo ""
}

# Function to build the app
build_app() {
    echo -e "${BLUE}🔨 Building HazardHawk app...${NC}"
    
    cd HazardHawk
    
    # Clean and build debug version
    if ./gradlew clean assembleDevelopmentStandardDebug; then
        log_test_result "Build Success" "PASS" "App built successfully"
    else
        log_test_result "Build Success" "FAIL" "Build failed"
        echo -e "${RED}❌ Build failed. Cannot proceed with testing.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Build completed successfully${NC}"
    echo ""
    
    cd ..
}

# Function to install the app
install_app() {
    echo -e "${BLUE}📱 Installing HazardHawk app...${NC}"
    
    APK_PATH="HazardHawk/androidApp/build/outputs/apk/developmentStandard/debug/androidApp-developmentStandard-debug.apk"
    
    if [ ! -f "$APK_PATH" ]; then
        echo -e "${RED}❌ APK not found at $APK_PATH${NC}"
        exit 1
    fi
    
    # Uninstall previous version if exists
    adb uninstall com.hazardhawk.dev 2>/dev/null || true
    
    # Install new version
    if adb install "$APK_PATH"; then
        log_test_result "App Installation" "PASS" "App installed successfully"
    else
        log_test_result "App Installation" "FAIL" "Installation failed"
        exit 1
    fi
    
    echo -e "${GREEN}✅ App installed successfully${NC}"
    echo ""
}

# Function to run unit tests
run_unit_tests() {
    echo -e "${BLUE}🧪 Running Unit Tests...${NC}"
    
    cd HazardHawk
    
    # Run specific validation tests
    if timeout $TEST_TIMEOUT ./gradlew testDevelopmentStandardDebugUnitTest --tests "*CriticalFunctionsValidationTest*" --continue; then
        log_test_result "Unit Tests" "PASS" "All unit tests passed"
    else
        log_test_result "Unit Tests" "FAIL" "Some unit tests failed"
    fi
    
    # Copy test reports
    if [ -d "androidApp/build/reports/tests" ]; then
        cp -r androidApp/build/reports/tests/* "../$REPORT_DIR/"
    fi
    
    cd ..
    echo ""
}

# Function to run instrumentation tests
run_instrumentation_tests() {
    echo -e "${BLUE}📱 Running Instrumentation Tests...${NC}"
    
    cd HazardHawk
    
    # Grant permissions before testing
    echo -e "${YELLOW}⚙️  Granting permissions...${NC}"
    adb shell pm grant com.hazardhawk.dev android.permission.CAMERA || true
    adb shell pm grant com.hazardhawk.dev android.permission.WRITE_EXTERNAL_STORAGE || true
    adb shell pm grant com.hazardhawk.dev android.permission.READ_EXTERNAL_STORAGE || true
    adb shell pm grant com.hazardhawk.dev android.permission.ACCESS_FINE_LOCATION || true
    adb shell pm grant com.hazardhawk.dev android.permission.ACCESS_COARSE_LOCATION || true
    
    # Run instrumentation tests
    if timeout $TEST_TIMEOUT ./gradlew connectedDevelopmentStandardDebugAndroidTest --tests "*CriticalFunctionsInstrumentationTest*" --continue; then
        log_test_result "Instrumentation Tests" "PASS" "All instrumentation tests passed"
    else
        log_test_result "Instrumentation Tests" "FAIL" "Some instrumentation tests failed"
    fi
    
    # Copy test reports
    if [ -d "androidApp/build/reports/androidTests" ]; then
        cp -r androidApp/build/reports/androidTests/* "../$REPORT_DIR/"
    fi
    
    cd ..
    echo ""
}

# Function to test app launch performance
test_app_launch_performance() {
    echo -e "${BLUE}🚀 Testing App Launch Performance...${NC}"
    
    PACKAGE="com.hazardhawk.dev"
    ACTIVITY="$PACKAGE/com.hazardhawk.MainActivity"
    
    # Kill app if running
    adb shell am force-stop "$PACKAGE" 2>/dev/null || true
    sleep 2
    
    # Measure launch time
    LAUNCH_START=$(date +%s%N)
    adb shell am start -n "$ACTIVITY" -W > "$REPORT_DIR/launch_output.txt"
    LAUNCH_END=$(date +%s%N)
    
    LAUNCH_TIME=$((($LAUNCH_END - $LAUNCH_START) / 1000000))  # Convert to milliseconds
    
    echo "Launch time: ${LAUNCH_TIME}ms"
    
    if [ $LAUNCH_TIME -lt 2000 ]; then
        log_test_result "App Launch Performance" "PASS" "Launch time: ${LAUNCH_TIME}ms"
    else
        log_test_result "App Launch Performance" "FAIL" "Launch time too slow: ${LAUNCH_TIME}ms"
    fi
    
    echo ""
}

# Function to test basic app functionality
test_basic_functionality() {
    echo -e "${BLUE}📋 Testing Basic App Functionality...${NC}"
    
    PACKAGE="com.hazardhawk.dev"
    
    # Check if app is running
    if adb shell pidof "$PACKAGE" > /dev/null; then
        log_test_result "App Running Check" "PASS" "App is running after launch"
    else
        log_test_result "App Running Check" "FAIL" "App is not running"
        return
    fi
    
    # Test if app responds to input (back button)
    sleep 3
    adb shell input keyevent KEYCODE_BACK
    sleep 1
    
    # Check basic UI elements (this is simplified - real test would be more comprehensive)
    # For now, just verify app doesn't crash
    if adb shell pidof "$PACKAGE" > /dev/null; then
        log_test_result "Basic UI Interaction" "PASS" "App responds to input without crashing"
    else
        log_test_result "Basic UI Interaction" "FAIL" "App crashed on basic interaction"
    fi
    
    echo ""
}

# Function to collect system information
collect_system_info() {
    echo -e "${BLUE}📊 Collecting System Information...${NC}"
    
    # Device info
    adb shell getprop ro.product.model > "$REPORT_DIR/device_model.txt"
    adb shell getprop ro.build.version.release > "$REPORT_DIR/android_version.txt"
    adb shell getprop ro.build.version.sdk > "$REPORT_DIR/api_level.txt"
    
    # Memory info
    adb shell cat /proc/meminfo > "$REPORT_DIR/memory_info.txt"
    
    # Storage info  
    adb shell df > "$REPORT_DIR/storage_info.txt"
    
    echo -e "${GREEN}✅ System information collected${NC}"
    echo ""
}

# Function to generate final report
generate_report() {
    echo -e "${BLUE}📝 Generating Test Report...${NC}"
    
    REPORT_FILE="$REPORT_DIR/validation_report.txt"
    
    cat > "$REPORT_FILE" << EOF
HazardHawk Validation Test Report
================================

Date: $(date)
Device: $(cat "$REPORT_DIR/device_model.txt" 2>/dev/null || echo "Unknown")
Android Version: $(cat "$REPORT_DIR/android_version.txt" 2>/dev/null || echo "Unknown")
API Level: $(cat "$REPORT_DIR/api_level.txt" 2>/dev/null || echo "Unknown")

Test Summary:
- Total Tests: $TEST_COUNT
- Passed: $((TEST_COUNT - ERROR_COUNT))
- Failed: $ERROR_COUNT
- Success Rate: $(( (TEST_COUNT - ERROR_COUNT) * 100 / TEST_COUNT ))%

Detailed Results:
================

EOF

    if [ -f "$REPORT_DIR/test_results.csv" ]; then
        while IFS=, read -r test_name result details; do
            echo "$test_name: $result - $details" >> "$REPORT_FILE"
        done < "$REPORT_DIR/test_results.csv"
    fi
    
    echo -e "${GREEN}✅ Report generated: $REPORT_FILE${NC}"
    echo ""
}

# Main execution
main() {
    echo "Test execution started at: $(date)"
    echo ""
    
    # Initialize CSV report
    echo "Test Name,Result,Details" > "$REPORT_DIR/test_results.csv"
    
    # Run validation steps
    check_device
    collect_system_info
    build_app
    install_app
    test_app_launch_performance
    test_basic_functionality
    run_unit_tests
    run_instrumentation_tests
    
    generate_report
    
    echo "========================================"
    echo -e "${BLUE}📊 VALIDATION TEST SUMMARY${NC}"
    echo "========================================"
    echo "Total Tests: $TEST_COUNT"
    echo -e "Passed: ${GREEN}$((TEST_COUNT - ERROR_COUNT))${NC}"
    echo -e "Failed: ${RED}$ERROR_COUNT${NC}"
    
    if [ $ERROR_COUNT -eq 0 ]; then
        echo -e "${GREEN}🎉 ALL TESTS PASSED! App is ready for validation.${NC}"
        exit 0
    else
        echo -e "${RED}⚠️  Some tests failed. Review the report for details.${NC}"
        echo -e "${YELLOW}📋 Full report available at: $REPORT_DIR/validation_report.txt${NC}"
        exit 1
    fi
}

# Execute main function
main "$@"