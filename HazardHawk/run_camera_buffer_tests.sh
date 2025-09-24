#!/bin/bash

# Camera Buffer Allocation Test Suite
# Tests camera stability, buffer management, and lifecycle handling

echo "=== HazardHawk Camera Buffer Allocation Test Suite ==="
echo "Testing camera stability and buffer management fixes"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test configuration
TEST_DURATION=30
CAPTURE_INTERVAL=2
APP_PACKAGE="com.hazardhawk"

# Function to check if device is connected
check_device() {
    if ! adb devices | grep -q "device$"; then
        echo -e "${RED}ERROR: No Android device connected${NC}"
        echo "Please connect your Android device and enable USB debugging"
        exit 1
    fi
    echo -e "${GREEN}✓ Android device connected${NC}"
}

# Function to install the app
install_app() {
    echo "Building and installing HazardHawk..."
    ./gradlew :androidApp:installDebug
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ App installed successfully${NC}"
    else
        echo -e "${RED}✗ App installation failed${NC}"
        exit 1
    fi
}

# Function to start monitoring camera logs
start_log_monitoring() {
    echo "Starting camera buffer monitoring..."
    
    # Clear old logs
    adb logcat -c
    
    # Start background monitoring for buffer issues
    adb logcat | grep -E "(ScalerNode|MultiAllocator|BufferQueue|CameraBufferManager|CameraLifecycleManager|allocation failure|preview_scaler|TNR)" > camera_buffer_test.log &
    LOG_PID=$!
    
    echo -e "${GREEN}✓ Log monitoring started (PID: $LOG_PID)${NC}"
}

# Function to test camera launch stability
test_camera_launch() {
    echo ""
    echo "=== Testing Camera Launch Stability ==="
    
    for i in {1..5}; do
        echo "Launch test $i/5..."
        
        # Start the camera activity
        adb shell am start -n "$APP_PACKAGE/.CameraScreen" -W
        sleep 3
        
        # Check if camera started successfully
        if adb shell dumpsys activity activities | grep -q "CameraScreen"; then
            echo -e "${GREEN}✓ Camera launch $i successful${NC}"
        else
            echo -e "${RED}✗ Camera launch $i failed${NC}"
        fi
        
        # Go back to home
        adb shell input keyevent KEYCODE_HOME
        sleep 2
    done
}

# Function to test rapid capture sequence
test_rapid_capture() {
    echo ""
    echo "=== Testing Rapid Capture Sequence ==="
    
    # Start camera
    adb shell am start -n "$APP_PACKAGE/.CameraScreen" -W
    sleep 3
    
    echo "Performing rapid captures..."
    for i in {1..10}; do
        echo "Capture $i/10..."
        
        # Simulate volume button capture
        adb shell input keyevent KEYCODE_VOLUME_DOWN
        sleep 0.5
        
        # Check for buffer allocation errors
        if grep -q "allocation failure\|ScalerNode.*drops\|MultiAllocator unable" camera_buffer_test.log; then
            echo -e "${RED}✗ Buffer allocation failure detected on capture $i${NC}"
        else
            echo -e "${GREEN}✓ Capture $i successful${NC}"
        fi
    done
    
    # Return to home
    adb shell input keyevent KEYCODE_HOME
}

# Function to test app lifecycle transitions
test_lifecycle_transitions() {
    echo ""
    echo "=== Testing App Lifecycle Transitions ==="
    
    # Start camera
    adb shell am start -n "$APP_PACKAGE/.CameraScreen" -W
    sleep 2
    
    for i in {1..3}; do
        echo "Lifecycle test $i/3..."
        
        # Background the app
        echo "  Backgrounding app..."
        adb shell input keyevent KEYCODE_HOME
        sleep 2
        
        # Resume the app
        echo "  Resuming app..."
        adb shell am start -n "$APP_PACKAGE/.CameraScreen" -W
        sleep 2
        
        # Test capture after resume
        echo "  Testing capture after resume..."
        adb shell input keyevent KEYCODE_VOLUME_DOWN
        sleep 1
        
        # Check for buffer queue abandonment
        if grep -q "BufferQueue has been abandoned\|Surface.*abandoned" camera_buffer_test.log; then
            echo -e "${RED}✗ Buffer queue abandonment detected on cycle $i${NC}"
        else
            echo -e "${GREEN}✓ Lifecycle transition $i successful${NC}"
        fi
    done
    
    # Return to home
    adb shell input keyevent KEYCODE_HOME
}

# Function to test orientation changes
test_orientation_changes() {
    echo ""
    echo "=== Testing Orientation Changes ==="
    
    # Start camera
    adb shell am start -n "$APP_PACKAGE/.CameraScreen" -W
    sleep 2
    
    for i in {1..2}; do
        echo "Orientation test $i/2..."
        
        # Rotate to landscape
        echo "  Rotating to landscape..."
        adb shell settings put system user_rotation 1
        sleep 2
        
        # Test capture in landscape
        adb shell input keyevent KEYCODE_VOLUME_DOWN
        sleep 1
        
        # Rotate back to portrait
        echo "  Rotating to portrait..."
        adb shell settings put system user_rotation 0
        sleep 2
        
        # Test capture in portrait
        adb shell input keyevent KEYCODE_VOLUME_DOWN
        sleep 1
        
        if grep -q "preview_scaler.*drops\|TNR.*fail" camera_buffer_test.log; then
            echo -e "${RED}✗ Preview scaling issues detected on rotation $i${NC}"
        else
            echo -e "${GREEN}✓ Orientation change $i successful${NC}"
        fi
    done
    
    # Return to home
    adb shell input keyevent KEYCODE_HOME
}

# Function to analyze results
analyze_results() {
    echo ""
    echo "=== Test Results Analysis ==="
    
    # Stop log monitoring
    kill $LOG_PID 2>/dev/null
    
    # Count different types of errors
    SCALER_ERRORS=$(grep -c "ScalerNode.*drops\|ScalerNode.*allocation failure" camera_buffer_test.log 2>/dev/null)
    ALLOCATOR_ERRORS=$(grep -c "MultiAllocator unable" camera_buffer_test.log 2>/dev/null)
    BUFFER_ERRORS=$(grep -c "BufferQueue.*abandon" camera_buffer_test.log 2>/dev/null)
    TNR_ERRORS=$(grep -c "TNR.*fail" camera_buffer_test.log 2>/dev/null)
    
    echo "Error Summary:"
    echo "  ScalerNode errors: $SCALER_ERRORS"
    echo "  MultiAllocator errors: $ALLOCATOR_ERRORS"
    echo "  BufferQueue errors: $BUFFER_ERRORS"
    echo "  TNR processing errors: $TNR_ERRORS"
    
    TOTAL_ERRORS=$((SCALER_ERRORS + ALLOCATOR_ERRORS + BUFFER_ERRORS + TNR_ERRORS))
    
    if [ $TOTAL_ERRORS -eq 0 ]; then
        echo -e "${GREEN}✓ SUCCESS: No camera buffer allocation errors detected!${NC}"
        echo -e "${GREEN}Camera stability improvements are working correctly.${NC}"
    elif [ $TOTAL_ERRORS -le 2 ]; then
        echo -e "${YELLOW}⚠ MINOR ISSUES: $TOTAL_ERRORS buffer allocation errors detected${NC}"
        echo -e "${YELLOW}Minor optimization may be needed.${NC}"
    else
        echo -e "${RED}✗ ISSUES DETECTED: $TOTAL_ERRORS buffer allocation errors found${NC}"
        echo -e "${RED}Further investigation required.${NC}"
    fi
    
    # Show buffer manager logs
    BUFFER_MANAGER_LOGS=$(grep -c "CameraBufferManager" camera_buffer_test.log 2>/dev/null)
    LIFECYCLE_MANAGER_LOGS=$(grep -c "CameraLifecycleManager" camera_buffer_test.log 2>/dev/null)
    
    echo ""
    echo "Buffer Management Activity:"
    echo "  CameraBufferManager logs: $BUFFER_MANAGER_LOGS"
    echo "  CameraLifecycleManager logs: $LIFECYCLE_MANAGER_LOGS"
    
    if [ $BUFFER_MANAGER_LOGS -gt 0 ] || [ $LIFECYCLE_MANAGER_LOGS -gt 0 ]; then
        echo -e "${GREEN}✓ Buffer management systems are active${NC}"
    else
        echo -e "${YELLOW}⚠ Buffer management systems not detected in logs${NC}"
    fi
}

# Function to cleanup
cleanup() {
    echo ""
    echo "Cleaning up..."
    kill $LOG_PID 2>/dev/null
    adb shell input keyevent KEYCODE_HOME
    echo "Test complete. Log saved to: camera_buffer_test.log"
}

# Trap to ensure cleanup on exit
trap cleanup EXIT

# Main execution
echo "Starting camera buffer allocation tests..."
check_device
install_app
start_log_monitoring

echo ""
echo "Running comprehensive camera stability tests..."
echo "This will test various scenarios that previously caused buffer allocation failures."
echo ""

test_camera_launch
test_rapid_capture
test_lifecycle_transitions
test_orientation_changes
analyze_results

echo ""
echo "=== Camera Buffer Test Suite Complete ==="
echo "Review the results above and check camera_buffer_test.log for detailed logs."
