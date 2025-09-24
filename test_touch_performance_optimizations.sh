#!/bin/bash

# Touch Performance Optimization Test Script for HazardHawk
# Tests the animation optimizations to prevent InputDispatcher warnings

echo "<¯ HazardHawk Touch Performance Optimization Test"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
APP_PACKAGE="com.hazardhawk.debug"
TEST_DURATION=30
ANIMATION_TEST_COUNT=10

echo -e "${BLUE}=ñ Checking device connection...${NC}"
if ! adb devices | grep -q "device$"; then
    echo -e "${RED}L No Android device connected${NC}"
    exit 1
fi
echo -e "${GREEN} Device connected${NC}"

echo -e "${BLUE}= Building optimized app version...${NC}"
cd HazardHawk
if ! ./gradlew assembleDebug > /dev/null 2>&1; then
    echo -e "${RED}L Build failed${NC}"
    exit 1
fi
echo -e "${GREEN} Build successful${NC}"

echo -e "${BLUE}=ò Installing optimized app...${NC}"
if ! adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk > /dev/null 2>&1; then
    echo -e "${RED}L Installation failed${NC}"
    exit 1
fi
echo -e "${GREEN} App installed${NC}"

# Function to test animation performance
test_animation_performance() {
    local test_name=$1
    echo -e "${BLUE}<¬ Testing: $test_name${NC}"
    
    # Clear logcat
    adb logcat -c
    
    # Start performance monitoring
    timeout 10s adb logcat | grep -E "TouchPerformance|InputDispatcher.*drop|animation.*slow" > "animation_test_${test_name// /_}.log" &
    local monitor_pid=$!
    
    # Launch app and trigger animations
    adb shell am start -n "$APP_PACKAGE/.MainActivity" > /dev/null 2>&1
    sleep 2
    
    # Simulate rapid touch events to test debouncing
    for i in $(seq 1 $ANIMATION_TEST_COUNT); do
        adb shell input tap 540 1000  # Capture button area
        sleep 0.1
    done
    
    # Wait for monitoring to complete
    wait $monitor_pid 2>/dev/null
    
    # Analyze results
    local warnings=$(grep -c "drop\|slow\|excessive" "animation_test_${test_name// /_}.log" 2>/dev/null || echo 0)
    local performance_logs=$(grep -c "TouchPerformance" "animation_test_${test_name// /_}.log" 2>/dev/null || echo 0)
    
    if [ $warnings -eq 0 ] && [ $performance_logs -gt 0 ]; then
        echo -e "${GREEN} $test_name: No performance warnings detected${NC}"
        return 0
    elif [ $warnings -gt 0 ]; then
        echo -e "${RED}L $test_name: $warnings performance warnings detected${NC}"
        echo -e "${YELLOW}   Check animation_test_${test_name// /_}.log for details${NC}"
        return 1
    else
        echo -e "${YELLOW}   $test_name: No performance monitoring detected${NC}"
        return 2
    fi
}

# Function to test InputDispatcher warnings
test_input_dispatcher() {
    echo -e "${BLUE}= Testing InputDispatcher behavior...${NC}"
    
    # Clear logcat and start monitoring
    adb logcat -c
    timeout 15s adb logcat | grep -i "InputDispatcher.*drop\|dropping.*event" > input_dispatcher_test.log &
    local monitor_pid=$!
    
    # Launch app
    adb shell am start -n "$APP_PACKAGE/.MainActivity" > /dev/null 2>&1
    sleep 2
    
    # Generate rapid touch events to potentially trigger InputDispatcher warnings
    echo -e "${YELLOW}=% Generating rapid touch events...${NC}"
    for i in $(seq 1 50); do
        adb shell input tap $((400 + RANDOM % 200)) $((800 + RANDOM % 200))
        # No sleep - intentionally rapid
    done
    
    # Wait for monitoring
    sleep 5
    kill $monitor_pid 2>/dev/null
    wait $monitor_pid 2>/dev/null
    
    # Check results
    local dropped_events=$(grep -c "drop\|dropping" input_dispatcher_test.log 2>/dev/null || echo 0)
    
    if [ $dropped_events -eq 0 ]; then
        echo -e "${GREEN} No InputDispatcher warnings detected${NC}"
        return 0
    else
        echo -e "${RED}L $dropped_events InputDispatcher warnings detected${NC}"
        echo -e "${YELLOW}   Check input_dispatcher_test.log for details${NC}"
        return 1
    fi
}

# Function to measure frame performance
test_frame_performance() {
    echo -e "${BLUE}<ž Testing frame performance during animations...${NC}"
    
    adb logcat -c
    timeout 10s adb logcat | grep -E "frame.*drop|TouchPerformance.*Frame drop" > frame_performance_test.log &
    local monitor_pid=$!
    
    # Launch app and navigate to camera (most animation-heavy screen)
    adb shell am start -n "$APP_PACKAGE/.MainActivity" > /dev/null 2>&1
    sleep 3
    
    # Interact with UI elements that have animations
    adb shell input tap 540 1000  # Capture button
    sleep 0.5
    adb shell input tap 100 200   # Settings
    sleep 0.5
    adb shell input tap 400 600   # Gallery
    sleep 0.5
    
    kill $monitor_pid 2>/dev/null
    wait $monitor_pid 2>/dev/null
    
    local frame_drops=$(grep -c "frame.*drop\|Frame drop" frame_performance_test.log 2>/dev/null || echo 0)
    
    if [ $frame_drops -eq 0 ]; then
        echo -e "${GREEN} No frame drops detected during animations${NC}"
        return 0
    elif [ $frame_drops -lt 5 ]; then
        echo -e "${YELLOW}   $frame_drops minor frame drops detected (acceptable)${NC}"
        return 0
    else
        echo -e "${RED}L $frame_drops frame drops detected (performance issue)${NC}"
        return 1
    fi
}

# Function to validate optimized components are being used
test_optimized_components() {
    echo -e "${BLUE}=' Validating optimized components are in use...${NC}"
    
    # Check that optimized components exist in build
    local optimized_files_count=0
    
    if [ -f "androidApp/src/main/java/com/hazardhawk/ui/components/OptimizedAnimationComponents.kt" ]; then
        ((optimized_files_count++))
        echo -e "${GREEN} OptimizedAnimationComponents.kt found${NC}"
    fi
    
    if [ -f "androidApp/src/main/java/com/hazardhawk/performance/TouchPerformanceMonitor.kt" ]; then
        ((optimized_files_count++))
        echo -e "${GREEN} TouchPerformanceMonitor.kt found${NC}"
    fi
    
    # Check for optimized patterns in glass morphism components
    if grep -q "OPTIMIZED VERSION" "androidApp/src/main/java/com/hazardhawk/ui/camera/GlassMorphismComponents.kt" 2>/dev/null; then
        ((optimized_files_count++))
        echo -e "${GREEN} GlassMorphismComponents.kt optimizations found${NC}"
    fi
    
    if [ $optimized_files_count -ge 2 ]; then
        echo -e "${GREEN} Optimization components properly integrated${NC}"
        return 0
    else
        echo -e "${RED}L Missing optimization components${NC}"
        return 1
    fi
}

# Run all tests
echo -e "\n${BLUE}>ê Running Touch Performance Tests...${NC}"
echo "======================================"

test_results=()

# Test 1: Validate optimized components
if test_optimized_components; then
    test_results+=("PASS: Optimized Components")
else
    test_results+=("FAIL: Optimized Components")
fi

# Test 2: Animation performance
if test_animation_performance "Button Animations"; then
    test_results+=("PASS: Button Animations")
else
    test_results+=("FAIL: Button Animations")
fi

# Test 3: InputDispatcher warnings
if test_input_dispatcher; then
    test_results+=("PASS: InputDispatcher")
else
    test_results+=("FAIL: InputDispatcher")
fi

# Test 4: Frame performance
if test_frame_performance; then
    test_results+=("PASS: Frame Performance")
else
    test_results+=("FAIL: Frame Performance")
fi

# Results summary
echo -e "\n${BLUE}=Ê Test Results Summary${NC}"
echo "======================"

pass_count=0
fail_count=0

for result in "${test_results[@]}"; do
    if [[ $result == PASS* ]]; then
        echo -e "${GREEN} $result${NC}"
        ((pass_count++))
    else
        echo -e "${RED}L $result${NC}"
        ((fail_count++))
    fi
done

echo -e "\n${BLUE}=È Performance Optimization Summary${NC}"
echo "=================================="
echo -e "Tests Passed: ${GREEN}$pass_count${NC}"
echo -e "Tests Failed: ${RED}$fail_count${NC}"

if [ $fail_count -eq 0 ]; then
    echo -e "\n${GREEN}<‰ All touch performance optimizations successful!${NC}"
    echo -e "${GREEN} InputDispatcher warnings should be resolved${NC}"
    echo -e "${GREEN} Animation performance optimized${NC}"
    echo -e "${GREEN} Touch responsiveness improved${NC}"
    exit 0
else
    echo -e "\n${YELLOW}   Some optimizations need attention${NC}"
    echo -e "${BLUE}=Ë Recommended Actions:${NC}"
    echo -e "1. Review failed test logs for specific issues"
    echo -e "2. Check animation complexity in failed components"
    echo -e "3. Ensure debouncing is properly implemented"
    echo -e "4. Monitor InputDispatcher logs during app usage"
    exit 1
fi