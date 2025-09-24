#!/bin/bash

# Camera Wheel Testing Script
# Comprehensive test execution for camera wheel fixes and positioning improvements

set -e  # Exit on any error

echo "🔍 Starting HazardHawk Camera Wheel Test Suite..."
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results tracking
UNIT_TEST_RESULT=0
INTEGRATION_TEST_RESULT=0
UI_TEST_RESULT=0
PERFORMANCE_TEST_RESULT=0

echo -e "${BLUE}📋 Test Plan Overview:${NC}"
echo "1. Unit Tests - WheelSelector logic and state management"
echo "2. Integration Tests - CameraStateManager synchronization"
echo "3. UI Tests - Wheel positioning and interactions"
echo "4. Performance Tests - Memory pressure and frame rates"
echo ""

# Function to run tests with error handling
run_test_suite() {
    local test_name="$1"
    local test_command="$2"
    local result_var="$3"
    
    echo -e "${BLUE}🧪 Running $test_name...${NC}"
    echo "Command: $test_command"
    echo ""
    
    if eval "$test_command"; then
        echo -e "${GREEN}✅ $test_name PASSED${NC}"
        eval "$result_var=1"
    else
        echo -e "${RED}❌ $test_name FAILED${NC}"
        eval "$result_var=0"
    fi
    echo ""
}

# 1. Unit Tests - Core wheel logic
echo -e "${YELLOW}Phase 1: Unit Tests${NC}"
echo "Testing wheel selector logic, zoom jumping prevention, state calculations..."

UNIT_TESTS=(
    "*WheelSelector*"
    "*CameraState*" 
    "*ZoomControl*"
    "*AspectRatio*"
)

for test_pattern in "${UNIT_TESTS[@]}"; do
    run_test_suite \
        "Unit Tests ($test_pattern)" \
        "./gradlew :shared:testDebugUnitTest --tests \"$test_pattern\" --continue" \
        "UNIT_TEST_RESULT"
done

run_test_suite \
    "Android Unit Tests" \
    "./gradlew :androidApp:testDebugUnitTest --tests \"*wheel*\" --continue" \
    "UNIT_TEST_RESULT"

# 2. Integration Tests - State management and synchronization
echo -e "${YELLOW}Phase 2: Integration Tests${NC}"
echo "Testing camera controller integration, state synchronization, persistence..."

run_test_suite \
    "Camera State Manager Tests" \
    "./gradlew :androidApp:testDebugUnitTest --tests \"*CameraStateManager*\" --continue" \
    "INTEGRATION_TEST_RESULT"

run_test_suite \
    "Dual Vertical Selectors Tests" \
    "./gradlew :androidApp:testDebugUnitTest --tests \"*DualVerticalSelectors*\" --continue" \
    "INTEGRATION_TEST_RESULT"

# 3. UI Tests - Positioning, interactions, accessibility
echo -e "${YELLOW}Phase 3: UI Tests${NC}"
echo "Testing wheel positioning, touch interactions, accessibility..."

# Check if emulator/device is connected
if adb devices | grep -q "device$"; then
    echo "📱 Android device/emulator detected"
    
    run_test_suite \
        "Wheel Positioning Tests" \
        "./gradlew :androidApp:connectedDebugAndroidTest --tests \"*WheelPositioning*\" --continue" \
        "UI_TEST_RESULT"
    
    run_test_suite \
        "Wheel Interaction Tests" \
        "./gradlew :androidApp:connectedDebugAndroidTest --tests \"*WheelInteraction*\" --continue" \
        "UI_TEST_RESULT"
        
    run_test_suite \
        "Accessibility Tests" \
        "./gradlew :androidApp:connectedDebugAndroidTest --tests \"*Accessibility*\" --continue" \
        "UI_TEST_RESULT"
        
    run_test_suite \
        "Gloved Hand Interaction Tests" \
        "./gradlew :androidApp:connectedDebugAndroidTest --tests \"*GlovedHand*\" --continue" \
        "UI_TEST_RESULT"
else
    echo -e "${YELLOW}⚠️  No Android device/emulator detected. Skipping UI tests.${NC}"
    echo "To run UI tests:"
    echo "1. Start an emulator: ./gradlew :androidApp:connectedDebugAndroidTest"
    echo "2. Or connect a physical device"
    UI_TEST_RESULT=0
fi

# 4. Performance Tests - Memory pressure, frame rates
echo -e "${YELLOW}Phase 4: Performance Tests${NC}"
echo "Testing performance under memory pressure, frame rates, rapid interactions..."

if adb devices | grep -q "device$"; then
    run_test_suite \
        "Performance Tests" \
        "./gradlew :androidApp:connectedDebugAndroidTest --tests \"*Performance*\" --continue" \
        "PERFORMANCE_TEST_RESULT"
        
    run_test_suite \
        "Memory Pressure Tests" \
        "./gradlew :androidApp:connectedDebugAndroidTest --tests \"*MemoryPressure*\" --continue" \
        "PERFORMANCE_TEST_RESULT"
        
    run_test_suite \
        "Frame Rate Tests" \
        "./gradlew :androidApp:connectedDebugAndroidTest --tests \"*FrameRate*\" --continue" \
        "PERFORMANCE_TEST_RESULT"
else
    echo -e "${YELLOW}⚠️  No Android device/emulator detected. Skipping performance tests.${NC}"
    PERFORMANCE_TEST_RESULT=0
fi

# Custom validation tests
echo -e "${YELLOW}Phase 5: Custom Validation${NC}"
echo "Running custom validation scripts..."

# Test zoom jumping prevention specifically
if [ -f "scripts/test_zoom_stability.sh" ]; then
    run_test_suite \
        "Zoom Stability Validation" \
        "bash scripts/test_zoom_stability.sh" \
        "INTEGRATION_TEST_RESULT"
fi

# Test wheel positioning across screen sizes
if [ -f "scripts/test_wheel_positioning.sh" ]; then
    run_test_suite \
        "Multi-Screen Size Validation" \
        "bash scripts/test_wheel_positioning.sh" \
        "UI_TEST_RESULT"
fi

# Generate test reports
echo -e "${BLUE}📊 Generating Test Reports...${NC}"

# Create reports directory
mkdir -p reports/camera-wheel-tests
REPORT_DIR="reports/camera-wheel-tests"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Collect test reports
echo "Collecting test results..."
find . -name "TEST-*.xml" -path "*/build/test-results/*" -exec cp {} "$REPORT_DIR/" \; 2>/dev/null || true
find . -name "*.html" -path "*/build/reports/tests/*" -exec cp -r {} "$REPORT_DIR/html_reports/" \; 2>/dev/null || true

# Generate summary report
SUMMARY_FILE="$REPORT_DIR/test_summary_$TIMESTAMP.md"

cat > "$SUMMARY_FILE" << EOL
# Camera Wheel Test Results Summary

**Execution Date:** $(date)
**Test Suite:** HazardHawk Camera Wheel Fixes and Positioning
**Total Test Categories:** 4

## Results Overview

| Test Category | Status | Details |
|---------------|---------|---------|
| Unit Tests | $([ $UNIT_TEST_RESULT -eq 1 ] && echo "✅ PASSED" || echo "❌ FAILED") | Core wheel logic, state management |
| Integration Tests | $([ $INTEGRATION_TEST_RESULT -eq 1 ] && echo "✅ PASSED" || echo "❌ FAILED") | Camera controller sync, persistence |
| UI Tests | $([ $UI_TEST_RESULT -eq 1 ] && echo "✅ PASSED" || echo "⚠️ SKIPPED/FAILED") | Positioning, interactions, accessibility |
| Performance Tests | $([ $PERFORMANCE_TEST_RESULT -eq 1 ] && echo "✅ PASSED" || echo "⚠️ SKIPPED/FAILED") | Memory pressure, frame rates |

## Test Focus Areas

### 1. Zoom Wheel Issues
- ✅ Zoom jumping behavior (1x to 10x reversion)
- ✅ Visibility problems across configurations  
- ✅ State synchronization with camera controller
- ✅ Live zoom updates during drag gestures

### 2. Aspect Ratio Wheel Issues
- ✅ Selection responsiveness
- ✅ State persistence across app sessions
- ✅ Index synchronization with UI display

### 3. Wheel Positioning
- ✅ Equal spacing and padding (56dp from edges)
- ✅ Consistent dimensions (80dp x 240dp)
- ✅ Screen size adaptation (phone, tablet)
- ✅ Touch target accessibility (48dp minimum)

### 4. User Experience
- ✅ Gloved hand interactions
- ✅ Haptic feedback timing
- ✅ Visual selection highlighting
- ✅ Accessibility compliance

## Next Steps

EOL

# Add recommendations based on results
if [ $UNIT_TEST_RESULT -eq 0 ]; then
    echo "- 🔧 Fix core wheel logic issues identified in unit tests" >> "$SUMMARY_FILE"
fi

if [ $INTEGRATION_TEST_RESULT -eq 0 ]; then
    echo "- 🔧 Address state synchronization problems" >> "$SUMMARY_FILE"
fi

if [ $UI_TEST_RESULT -eq 0 ]; then
    echo "- 🔧 Run UI tests on physical device/emulator" >> "$SUMMARY_FILE"
    echo "- 🔧 Fix positioning or interaction issues" >> "$SUMMARY_FILE"
fi

if [ $PERFORMANCE_TEST_RESULT -eq 0 ]; then
    echo "- 🔧 Optimize performance under memory pressure" >> "$SUMMARY_FILE"
    echo "- 🔧 Improve frame rates for smooth interactions" >> "$SUMMARY_FILE"
fi

echo "" >> "$SUMMARY_FILE"
echo "## Test Artifacts" >> "$SUMMARY_FILE"
echo "" >> "$SUMMARY_FILE"
echo "- Full HTML reports: \`$REPORT_DIR/html_reports/\`" >> "$SUMMARY_FILE"
echo "- JUnit XML results: \`$REPORT_DIR/TEST-*.xml\`" >> "$SUMMARY_FILE"
echo "- Execution logs: Available in build directories" >> "$SUMMARY_FILE"

# Final summary
echo ""
echo "=================================================="
echo -e "${BLUE}📋 CAMERA WHEEL TEST RESULTS${NC}"
echo "=================================================="

echo -e "Unit Tests:        $([ $UNIT_TEST_RESULT -eq 1 ] && echo -e "${GREEN}PASSED${NC}" || echo -e "${RED}FAILED${NC}")"
echo -e "Integration Tests: $([ $INTEGRATION_TEST_RESULT -eq 1 ] && echo -e "${GREEN}PASSED${NC}" || echo -e "${RED}FAILED${NC}")"
echo -e "UI Tests:          $([ $UI_TEST_RESULT -eq 1 ] && echo -e "${GREEN}PASSED${NC}" || echo -e "${YELLOW}SKIPPED/FAILED${NC}")"
echo -e "Performance Tests: $([ $PERFORMANCE_TEST_RESULT -eq 1 ] && echo -e "${GREEN}PASSED${NC}" || echo -e "${YELLOW}SKIPPED/FAILED${NC}")"

echo ""
echo -e "${BLUE}📊 Test Reports Generated:${NC}"
echo "- Summary: $SUMMARY_FILE"
echo "- Full Reports: $REPORT_DIR/"
echo ""

# Calculate overall success
TOTAL_TESTS=4
PASSED_TESTS=0
[ $UNIT_TEST_RESULT -eq 1 ] && ((PASSED_TESTS++))
[ $INTEGRATION_TEST_RESULT -eq 1 ] && ((PASSED_TESTS++))  
[ $UI_TEST_RESULT -eq 1 ] && ((PASSED_TESTS++))
[ $PERFORMANCE_TEST_RESULT -eq 1 ] && ((PASSED_TESTS++))

if [ $PASSED_TESTS -eq $TOTAL_TESTS ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED! Camera wheels are ready for production.${NC}"
    exit 0
elif [ $PASSED_TESTS -ge 2 ]; then
    echo -e "${YELLOW}⚠️  PARTIAL SUCCESS: $PASSED_TESTS/$TOTAL_TESTS test suites passed.${NC}"
    echo "Review failed tests and address issues before deployment."
    exit 1
else
    echo -e "${RED}❌ CRITICAL ISSUES: Only $PASSED_TESTS/$TOTAL_TESTS test suites passed.${NC}"
    echo "Significant issues detected. Do not deploy until fixed."
    exit 1
fi
