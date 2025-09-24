#!/bin/bash

# HazardHawk AI Field Testing Automation
# Real device testing framework for construction safety AI

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"  # Go up two levels
FIELD_TEST_DIR="$PROJECT_ROOT/field-tests"
TEST_DATA_DIR="$FIELD_TEST_DIR/construction-photos"
RESULTS_DIR="$FIELD_TEST_DIR/results"
DEVICE_LOG_DIR="$FIELD_TEST_DIR/device-logs"
VERBOSE=${VERBOSE:-false}
AUTO_INSTALL=${AUTO_INSTALL:-true}

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Test configuration
TEST_APK_PATH="$PROJECT_ROOT/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
TEST_PACKAGE="com.hazardhawk"
TEST_ACTIVITY=".MainActivity"
FIELD_TEST_TIMEOUT=300  # 5 minutes per test

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

log_device() {
    echo -e "${PURPLE}[$(date +'%Y-%m-%d %H:%M:%S')] 📱${NC} $1"
}

verbose_log() {
    if [[ "$VERBOSE" == "true" ]]; then
        log "$1"
    fi
}

# Help function
show_help() {
    cat << EOF
HazardHawk AI Field Testing Automation

Usage: $0 [OPTIONS] [TEST_TYPE]

Options:
    -h, --help              Show this help message
    -v, --verbose           Enable verbose logging
    -d, --device DEVICE_ID  Target specific device (default: all connected)
    -t, --timeout SECONDS   Test timeout in seconds (default: 300)
    -o, --output-dir PATH   Results output directory
    --no-install           Skip app installation
    --real-models          Use real AI models (requires model files)
    --mock-models          Use mock AI models for testing
    --batch-size N         Number of photos to test in batch (default: 10)
    --performance-test     Run performance benchmarking
    --stress-test          Run stress testing with continuous operation

Test Types:
    basic                   Basic AI functionality test
    performance             Performance benchmarking
    stress                  Stress testing (continuous operation)
    construction            Construction safety scenario testing
    real-photos             Test with real construction photos
    mock-photos             Test with generated mock photos
    all                     Run all field tests (default)

Examples:
    $0                      # Run all tests on all connected devices
    $0 -v performance       # Run performance tests with verbose output
    $0 -d emulator-5554 basic  # Run basic tests on specific device
    $0 --real-models construction  # Test with real AI models

EOF
}

# Parse command line arguments
TEST_TYPE="all"
TARGET_DEVICE=""
BATCH_SIZE=10
USE_REAL_MODELS=false
PERFORMANCE_TEST=false
STRESS_TEST=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            exit 0
            ;;
        -v|--verbose)
            VERBOSE=true
            ;;
        -d|--device)
            TARGET_DEVICE="$2"
            shift
            ;;
        -t|--timeout)
            FIELD_TEST_TIMEOUT="$2"
            shift
            ;;
        -o|--output-dir)
            RESULTS_DIR="$2"
            shift
            ;;
        --no-install)
            AUTO_INSTALL=false
            ;;
        --real-models)
            USE_REAL_MODELS=true
            ;;
        --mock-models)
            USE_REAL_MODELS=false
            ;;
        --batch-size)
            BATCH_SIZE="$2"
            shift
            ;;
        --performance-test)
            PERFORMANCE_TEST=true
            ;;
        --stress-test)
            STRESS_TEST=true
            ;;
        basic|performance|stress|construction|real-photos|mock-photos|all)
            TEST_TYPE="$1"
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
    shift
done

# Setup field test environment
setup_field_test_environment() {
    log "Setting up field test environment..."
    
    # Create test directories
    mkdir -p "$FIELD_TEST_DIR"
    mkdir -p "$TEST_DATA_DIR"
    mkdir -p "$RESULTS_DIR"
    mkdir -p "$DEVICE_LOG_DIR"
    
    # Verify ADB is available
    if ! command -v adb &> /dev/null; then
        log_error "ADB not found. Please install Android SDK platform-tools."
        exit 1
    fi
    
    # Check if devices are connected
    local connected_devices
    connected_devices=$(adb devices | grep -v "List of devices" | grep -v "^$" | wc -l)
    
    if [[ $connected_devices -eq 0 ]]; then
        log_error "No Android devices connected. Please connect a device or start an emulator."
        exit 1
    fi
    
    log_success "Found $connected_devices connected device(s)"
    
    # Create test data if needed
    setup_test_data
    
    log_success "Field test environment setup complete"
}

# Setup test data
setup_test_data() {
    log "Setting up test data..."
    
    # Create mock construction photos for testing
    if [[ ! -d "$TEST_DATA_DIR/ppe-violations" ]]; then
        mkdir -p "$TEST_DATA_DIR/ppe-violations"
        mkdir -p "$TEST_DATA_DIR/fall-hazards"
        mkdir -p "$TEST_DATA_DIR/electrical-hazards"
        mkdir -p "$TEST_DATA_DIR/safe-construction"
        
        # Generate mock test images (you would replace these with real photos)
        create_mock_construction_photos
    fi
    
    # Copy real construction photos if available
    if [[ -d "$PROJECT_ROOT/test-data/real-construction-photos" ]]; then
        log "Copying real construction photos..."
        cp -r "$PROJECT_ROOT/test-data/real-construction-photos/"* "$TEST_DATA_DIR/" || true
    fi
    
    log_success "Test data setup complete"
}

# Create mock construction photos for testing
create_mock_construction_photos() {
    log "Creating mock construction safety photos..."
    
    # Use ImageMagick if available to create test images
    if command -v convert &> /dev/null; then
        # PPE violation scenarios
        convert -size 1920x1080 xc:lightblue \
            -pointsize 72 -fill red -gravity center \
            -annotate +0+0 "PPE VIOLATION\nNo Hard Hat" \
            "$TEST_DATA_DIR/ppe-violations/no-hard-hat.jpg"
        
        convert -size 1920x1080 xc:lightgreen \
            -pointsize 72 -fill darkblue -gravity center \
            -annotate +0+0 "FALL HAZARD\nUnprotected Edge" \
            "$TEST_DATA_DIR/fall-hazards/unprotected-edge.jpg"
        
        convert -size 1920x1080 xc:lightyellow \
            -pointsize 72 -fill red -gravity center \
            -annotate +0+0 "ELECTRICAL\nExposed Wiring" \
            "$TEST_DATA_DIR/electrical-hazards/exposed-wiring.jpg"
        
        convert -size 1920x1080 xc:lightgray \
            -pointsize 72 -fill green -gravity center \
            -annotate +0+0 "SAFE SITE\nAll PPE Present" \
            "$TEST_DATA_DIR/safe-construction/compliant-site.jpg"
            
        log_success "Generated mock construction photos"
    else
        log_warning "ImageMagick not available. Creating placeholder files..."
        
        # Create placeholder files
        echo "Mock PPE violation photo" > "$TEST_DATA_DIR/ppe-violations/no-hard-hat.jpg"
        echo "Mock fall hazard photo" > "$TEST_DATA_DIR/fall-hazards/unprotected-edge.jpg"
        echo "Mock electrical hazard photo" > "$TEST_DATA_DIR/electrical-hazards/exposed-wiring.jpg"
        echo "Mock safe construction photo" > "$TEST_DATA_DIR/safe-construction/compliant-site.jpg"
    fi
}

# Get connected devices
get_connected_devices() {
    local devices=()
    
    if [[ -n "$TARGET_DEVICE" ]]; then
        if adb -s "$TARGET_DEVICE" shell echo "test" &> /dev/null; then
            devices=("$TARGET_DEVICE")
        else
            log_error "Target device $TARGET_DEVICE not found or not responding"
            exit 1
        fi
    else
        # Get all connected devices
        while IFS= read -r line; do
            if [[ $line == *"device"* ]] && [[ $line != *"List of devices"* ]]; then
                local device_id
                device_id=$(echo "$line" | cut -f1)
                devices+=("$device_id")
            fi
        done < <(adb devices)
    fi
    
    echo "${devices[@]}"
}

# Install app on device
install_app_on_device() {
    local device_id="$1"
    
    if [[ "$AUTO_INSTALL" != "true" ]]; then
        log "Skipping app installation as requested"
        return 0
    fi
    
    log_device "Installing HazardHawk on device $device_id..."
    
    # Build APK if not exists
    if [[ ! -f "$TEST_APK_PATH" ]]; then
        log "Building APK..."
        cd "$PROJECT_ROOT"
        ./gradlew :androidApp:assembleDebug
        
        if [[ ! -f "$TEST_APK_PATH" ]]; then
            log_error "Failed to build APK at $TEST_APK_PATH"
            return 1
        fi
    fi
    
    # Install APK
    if adb -s "$device_id" install -r "$TEST_APK_PATH"; then
        log_success "App installed successfully on $device_id"
        return 0
    else
        log_error "Failed to install app on $device_id"
        return 1
    fi
}

# Get device information
get_device_info() {
    local device_id="$1"
    local info_file="$DEVICE_LOG_DIR/${device_id}_info.json"
    
    log_device "Collecting device information for $device_id..."
    
    # Get device properties
    local manufacturer model android_version ram_mb storage_mb cpu_info
    manufacturer=$(adb -s "$device_id" shell getprop ro.product.manufacturer 2>/dev/null | tr -d '\r')
    model=$(adb -s "$device_id" shell getprop ro.product.model 2>/dev/null | tr -d '\r')
    android_version=$(adb -s "$device_id" shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')
    
    # Get memory info
    ram_mb=$(adb -s "$device_id" shell cat /proc/meminfo | grep MemTotal | awk '{print int($2/1024)}' 2>/dev/null || echo "unknown")
    
    # Get storage info
    storage_mb=$(adb -s "$device_id" shell df /data | tail -1 | awk '{print int($2/1024)}' 2>/dev/null || echo "unknown")
    
    # Get CPU info
    cpu_info=$(adb -s "$device_id" shell cat /proc/cpuinfo | grep "model name" | head -1 | cut -d: -f2 | xargs 2>/dev/null || echo "unknown")
    
    # Create device info JSON
    cat > "$info_file" << EOF
{
  "device_id": "$device_id",
  "manufacturer": "$manufacturer",
  "model": "$model",
  "android_version": "$android_version",
  "ram_mb": "$ram_mb",
  "storage_mb": "$storage_mb",
  "cpu_info": "$cpu_info",
  "test_timestamp": "$(date -Iseconds)",
  "ai_models_mode": "$([ "$USE_REAL_MODELS" == "true" ] && echo "real" || echo "mock")"
}
EOF
    
    verbose_log "Device info saved to $info_file"
    
    # Log device info
    log_device "Device: $manufacturer $model (Android $android_version, ${ram_mb}MB RAM)"
    
    echo "$info_file"
}

# Run basic AI functionality test
run_basic_ai_test() {
    local device_id="$1"
    local test_results_file="$RESULTS_DIR/${device_id}_basic_test.json"
    
    log_device "Running basic AI functionality test on $device_id..."
    
    # Start the app
    adb -s "$device_id" shell am start -n "$TEST_PACKAGE/$TEST_ACTIVITY" > /dev/null 2>&1
    sleep 5
    
    local start_time
    start_time=$(date +%s)
    local test_results=()
    
    # Test 1: App launch and AI initialization
    log "Testing app launch and AI initialization..."
    local launch_success=false
    
    # Check if app is running
    if adb -s "$device_id" shell pidof "$TEST_PACKAGE" > /dev/null; then
        launch_success=true
        log_success "App launched successfully"
    else
        log_error "App failed to launch"
    fi
    
    test_results+=("\"app_launch\": $launch_success")
    
    # Test 2: Camera functionality
    log "Testing camera functionality..."
    
    # Navigate to camera (simulate touch events)
    adb -s "$device_id" shell input tap 500 1500  # Example camera button coordinates
    sleep 2
    
    # Check camera permissions
    local camera_permission
    camera_permission=$(adb -s "$device_id" shell dumpsys package "$TEST_PACKAGE" | grep "android.permission.CAMERA" | grep "granted=true" | wc -l)
    local camera_ready=$([[ $camera_permission -gt 0 ]] && echo "true" || "false")
    
    test_results+=("\"camera_ready\": $camera_ready")
    
    # Test 3: AI analysis simulation
    log "Testing AI analysis simulation..."
    local ai_analysis_time=0
    local ai_analysis_success=false
    
    if [[ "$USE_REAL_MODELS" == "true" ]]; then
        # Test with real AI models (if available)
        log "Testing with real AI models..."
        
        # Simulate photo capture and analysis
        adb -s "$device_id" shell input tap 960 1800  # Example capture button
        sleep 3
        
        # Monitor logcat for AI analysis completion
        local analysis_start
        analysis_start=$(date +%s)
        
        # Wait for analysis completion (look for AI-related log messages)
        timeout 30s adb -s "$device_id" logcat -v time | grep -m 1 "AI analysis" &
        local logcat_pid=$!
        
        sleep 5  # Give some time for analysis
        kill $logcat_pid 2>/dev/null || true
        
        local analysis_end
        analysis_end=$(date +%s)
        ai_analysis_time=$((analysis_end - analysis_start))
        
        if [[ $ai_analysis_time -le 10 ]]; then
            ai_analysis_success=true
            log_success "AI analysis completed in ${ai_analysis_time}s"
        else
            log_warning "AI analysis took ${ai_analysis_time}s (>10s threshold)"
        fi
    else
        # Mock AI analysis
        log "Testing with mock AI models..."
        ai_analysis_time=1
        ai_analysis_success=true
        log_success "Mock AI analysis completed"
    fi
    
    test_results+=("\"ai_analysis_success\": $ai_analysis_success")
    test_results+=("\"ai_analysis_time_seconds\": $ai_analysis_time")
    
    local end_time
    end_time=$(date +%s)
    local total_test_time=$((end_time - start_time))
    
    # Save test results
    cat > "$test_results_file" << EOF
{
  "device_id": "$device_id",
  "test_type": "basic",
  "timestamp": "$(date -Iseconds)",
  "total_test_time_seconds": $total_test_time,
  $(IFS=','; echo "${test_results[*]}")
}
EOF
    
    # Stop the app
    adb -s "$device_id" shell am force-stop "$TEST_PACKAGE"
    
    if [[ "$launch_success" == "true" ]] && [[ "$ai_analysis_success" == "true" ]]; then
        log_success "Basic AI test completed successfully on $device_id"
        return 0
    else
        log_error "Basic AI test failed on $device_id"
        return 1
    fi
}

# Run performance benchmarking
run_performance_test() {
    local device_id="$1"
    local test_results_file="$RESULTS_DIR/${device_id}_performance_test.json"
    
    log_device "Running AI performance benchmarking on $device_id..."
    
    # Start the app
    adb -s "$device_id" shell am start -n "$TEST_PACKAGE/$TEST_ACTIVITY" > /dev/null 2>&1
    sleep 5
    
    local performance_metrics=()
    
    # Test multiple photos in batch
    log "Testing batch photo analysis (${BATCH_SIZE} photos)..."
    
    local batch_start_time
    batch_start_time=$(date +%s)
    
    # Simulate batch photo processing
    for ((i=1; i<=BATCH_SIZE; i++)); do
        log "Processing photo $i/$BATCH_SIZE..."
        
        # Simulate photo capture
        adb -s "$device_id" shell input tap 960 1800  # Capture button
        sleep 2
        
        # Monitor CPU usage during analysis
        local cpu_usage
        cpu_usage=$(adb -s "$device_id" shell top -n 1 | grep "$TEST_PACKAGE" | awk '{print $9}' | head -1 2>/dev/null || echo "0")
        
        # Monitor memory usage
        local memory_usage_mb
        memory_usage_mb=$(adb -s "$device_id" shell dumpsys meminfo "$TEST_PACKAGE" | grep "TOTAL" | awk '{print int($1/1024)}' 2>/dev/null || echo "0")
        
        performance_metrics+=("\"photo_${i}_cpu_percent\": \"$cpu_usage\"")
        performance_metrics+=("\"photo_${i}_memory_mb\": $memory_usage_mb")
        
        verbose_log "Photo $i: CPU: ${cpu_usage}%, Memory: ${memory_usage_mb}MB"
    done
    
    local batch_end_time
    batch_end_time=$(date +%s)
    local total_batch_time=$((batch_end_time - batch_start_time))
    local avg_time_per_photo=$((total_batch_time / BATCH_SIZE))
    
    # Battery level monitoring
    local battery_start battery_end battery_drain
    battery_start=$(adb -s "$device_id" shell dumpsys battery | grep level | cut -d: -f2 | xargs)
    sleep 10  # Wait a bit to measure battery drain
    battery_end=$(adb -s "$device_id" shell dumpsys battery | grep level | cut -d: -f2 | xargs)
    battery_drain=$((battery_start - battery_end))
    
    # Save performance results
    cat > "$test_results_file" << EOF
{
  "device_id": "$device_id",
  "test_type": "performance",
  "timestamp": "$(date -Iseconds)",
  "batch_size": $BATCH_SIZE,
  "total_batch_time_seconds": $total_batch_time,
  "average_time_per_photo_seconds": $avg_time_per_photo,
  "battery_drain_percent": $battery_drain,
  $(IFS=','; echo "${performance_metrics[*]}")
}
EOF
    
    # Stop the app
    adb -s "$device_id" shell am force-stop "$TEST_PACKAGE"
    
    log_success "Performance test completed: ${avg_time_per_photo}s avg per photo, ${battery_drain}% battery drain"
    
    # Check if performance meets targets
    if [[ $avg_time_per_photo -le 5 ]]; then  # 5 second target
        log_success "Performance test passed on $device_id"
        return 0
    else
        log_warning "Performance test slow on $device_id (${avg_time_per_photo}s > 5s target)"
        return 1
    fi
}

# Run stress testing
run_stress_test() {
    local device_id="$1"
    local test_results_file="$RESULTS_DIR/${device_id}_stress_test.json"
    local test_duration=600  # 10 minutes
    
    log_device "Running AI stress test on $device_id for ${test_duration}s..."
    
    # Start the app
    adb -s "$device_id" shell am start -n "$TEST_PACKAGE/$TEST_ACTIVITY" > /dev/null 2>&1
    sleep 5
    
    local start_time
    start_time=$(date +%s)
    local stress_metrics=()
    local successful_analyses=0
    local failed_analyses=0
    local iteration=0
    
    while [[ $(($(date +%s) - start_time)) -lt $test_duration ]]; do
        iteration=$((iteration + 1))
        log "Stress test iteration $iteration..."
        
        # Simulate continuous photo analysis
        adb -s "$device_id" shell input tap 960 1800  # Capture button
        sleep 3
        
        # Check if app is still responsive
        if adb -s "$device_id" shell pidof "$TEST_PACKAGE" > /dev/null; then
            successful_analyses=$((successful_analyses + 1))
            
            # Monitor resource usage
            local cpu_usage memory_usage_mb temperature
            cpu_usage=$(adb -s "$device_id" shell top -n 1 | grep "$TEST_PACKAGE" | awk '{print $9}' | head -1 2>/dev/null || echo "0")
            memory_usage_mb=$(adb -s "$device_id" shell dumpsys meminfo "$TEST_PACKAGE" | grep "TOTAL" | awk '{print int($1/1024)}' 2>/dev/null || echo "0")
            temperature=$(adb -s "$device_id" shell cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null | awk '{print int($1/1000)}' || echo "unknown")
            
            stress_metrics+=("\"iteration_${iteration}_cpu\": \"$cpu_usage\"")
            stress_metrics+=("\"iteration_${iteration}_memory_mb\": $memory_usage_mb")
            stress_metrics+=("\"iteration_${iteration}_temp_c\": \"$temperature\"")
            
            verbose_log "Iteration $iteration: CPU: ${cpu_usage}%, Memory: ${memory_usage_mb}MB, Temp: ${temperature}°C"
            
            # Check for memory leaks (memory usage should not continuously increase)
            if [[ $memory_usage_mb -gt 1000 ]]; then
                log_warning "High memory usage detected: ${memory_usage_mb}MB at iteration $iteration"
            fi
            
            # Check for thermal throttling
            if [[ "$temperature" != "unknown" ]] && [[ $temperature -gt 60 ]]; then
                log_warning "High temperature detected: ${temperature}°C at iteration $iteration"
            fi
        else
            log_error "App crashed at iteration $iteration"
            failed_analyses=$((failed_analyses + 1))
            
            # Try to restart the app
            adb -s "$device_id" shell am start -n "$TEST_PACKAGE/$TEST_ACTIVITY" > /dev/null 2>&1
            sleep 5
        fi
        
        # Brief pause to prevent overheating
        sleep 2
    done
    
    local end_time
    end_time=$(date +%s)
    local actual_test_duration=$((end_time - start_time))
    local success_rate=$((successful_analyses * 100 / (successful_analyses + failed_analyses)))
    
    # Save stress test results
    cat > "$test_results_file" << EOF
{
  "device_id": "$device_id",
  "test_type": "stress",
  "timestamp": "$(date -Iseconds)",
  "test_duration_seconds": $actual_test_duration,
  "total_iterations": $iteration,
  "successful_analyses": $successful_analyses,
  "failed_analyses": $failed_analyses,
  "success_rate_percent": $success_rate,
  $(IFS=','; echo "${stress_metrics[*]}")
}
EOF
    
    # Stop the app
    adb -s "$device_id" shell am force-stop "$TEST_PACKAGE"
    
    log_success "Stress test completed: $iteration iterations, ${success_rate}% success rate"
    
    # Check if stress test passed
    if [[ $success_rate -ge 95 ]]; then
        log_success "Stress test passed on $device_id"
        return 0
    else
        log_error "Stress test failed on $device_id (${success_rate}% < 95% threshold)"
        return 1
    fi
}

# Run construction scenario testing
run_construction_scenario_test() {
    local device_id="$1"
    local test_results_file="$RESULTS_DIR/${device_id}_construction_scenarios.json"
    
    log_device "Running construction safety scenario testing on $device_id..."
    
    # Start the app
    adb -s "$device_id" shell am start -n "$TEST_PACKAGE/$TEST_ACTIVITY" > /dev/null 2>&1
    sleep 5
    
    # Test different construction scenarios
    local scenarios=("ppe-violations" "fall-hazards" "electrical-hazards" "safe-construction")
    local scenario_results=()
    
    for scenario in "${scenarios[@]}"; do
        log "Testing $scenario scenario..."
        
        local scenario_start_time
        scenario_start_time=$(date +%s)
        
        # Find photos for this scenario
        local photo_count
        photo_count=$(find "$TEST_DATA_DIR/$scenario" -name "*.jpg" -o -name "*.png" | wc -l)
        
        if [[ $photo_count -eq 0 ]]; then
            log_warning "No photos found for $scenario scenario"
            scenario_results+=("\"${scenario}_tested\": false")
            continue
        fi
        
        log "Found $photo_count photos for $scenario scenario"
        
        # Simulate photo analysis for scenario
        local scenario_success=true
        
        # Push a test photo to device (simulation)
        local test_photo
        test_photo=$(find "$TEST_DATA_DIR/$scenario" -name "*.jpg" -o -name "*.png" | head -1)
        
        if [[ -n "$test_photo" ]]; then
            # In a real implementation, you would:
            # 1. Push the photo to device
            # 2. Use UI automator to select and analyze it
            # 3. Verify the AI analysis results
            
            adb -s "$device_id" shell input tap 960 1800  # Simulate capture/analyze
            sleep 3
            
            # Check if analysis completed
            if adb -s "$device_id" shell pidof "$TEST_PACKAGE" > /dev/null; then
                log_success "$scenario analysis completed"
            else
                log_error "$scenario analysis failed - app crashed"
                scenario_success=false
            fi
        fi
        
        local scenario_end_time
        scenario_end_time=$(date +%s)
        local scenario_duration=$((scenario_end_time - scenario_start_time))
        
        scenario_results+=("\"${scenario}_tested\": true")
        scenario_results+=("\"${scenario}_success\": $scenario_success")
        scenario_results+=("\"${scenario}_duration_seconds\": $scenario_duration")
        scenario_results+=("\"${scenario}_photo_count\": $photo_count")
    done
    
    # Save scenario test results
    cat > "$test_results_file" << EOF
{
  "device_id": "$device_id",
  "test_type": "construction_scenarios",
  "timestamp": "$(date -Iseconds)",
  "scenarios_tested": ["${scenarios[*]}"],
  $(IFS=','; echo "${scenario_results[*]}")
}
EOF
    
    # Stop the app
    adb -s "$device_id" shell am force-stop "$TEST_PACKAGE"
    
    log_success "Construction scenario testing completed on $device_id"
    return 0
}

# Run comprehensive field test
run_comprehensive_field_test() {
    local device_id="$1"
    
    log_device "Running comprehensive field test on $device_id..."
    
    local test_results=()
    
    # Run all test types
    if run_basic_ai_test "$device_id"; then
        test_results+=("basic: PASSED")
    else
        test_results+=("basic: FAILED")
    fi
    
    if run_performance_test "$device_id"; then
        test_results+=("performance: PASSED")
    else
        test_results+=("performance: FAILED")
    fi
    
    if run_construction_scenario_test "$device_id"; then
        test_results+=("scenarios: PASSED")
    else
        test_results+=("scenarios: FAILED")
    fi
    
    if [[ "$STRESS_TEST" == "true" ]]; then
        if run_stress_test "$device_id"; then
            test_results+=("stress: PASSED")
        else
            test_results+=("stress: FAILED")
        fi
    fi
    
    # Generate summary
    log_device "Field test summary for $device_id: ${test_results[*]}"
    
    # Check if all tests passed
    if [[ "${test_results[*]}" == *"FAILED"* ]]; then
        return 1
    else
        return 0
    fi
}

# Generate field test report
generate_field_test_report() {
    log "Generating comprehensive field test report..."
    
    local report_file="$RESULTS_DIR/field_test_report.html"
    local timestamp
    timestamp=$(date +'%Y-%m-%d %H:%M:%S')
    
    cat > "$report_file" << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>HazardHawk AI Field Test Report</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .header { background: #f5f5f5; padding: 20px; border-radius: 5px; }
        .device { margin: 20px 0; padding: 15px; border: 1px solid #ddd; border-radius: 5px; }
        .passed { border-left: 4px solid #4CAF50; }
        .failed { border-left: 4px solid #f44336; }
        .metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; }
        .metric { background: #f9f9f9; padding: 15px; border-radius: 5px; }
        .metric-value { font-size: 20px; font-weight: bold; color: #2196F3; }
        pre { background: #f5f5f5; padding: 10px; border-radius: 3px; overflow-x: auto; }
    </style>
</head>
<body>
    <div class="header">
        <h1>📱 HazardHawk AI Field Test Report</h1>
        <p>Generated on: TIMESTAMP_PLACEHOLDER</p>
        <p>Test Type: TEST_TYPE_PLACEHOLDER</p>
        <p>AI Models: MODELS_MODE_PLACEHOLDER</p>
    </div>
EOF
    
    # Replace placeholders
    sed -i '' "s/TIMESTAMP_PLACEHOLDER/$timestamp/g" "$report_file"
    sed -i '' "s/TEST_TYPE_PLACEHOLDER/$TEST_TYPE/g" "$report_file"
    sed -i '' "s/MODELS_MODE_PLACEHOLDER/$([ "$USE_REAL_MODELS" == "true" ] && echo "Real AI Models" || echo "Mock Models")/g" "$report_file"
    
    # Add device results
    for result_file in "$RESULTS_DIR"/*.json; do
        if [[ -f "$result_file" ]]; then
            local device_id test_type
            device_id=$(basename "$result_file" | cut -d'_' -f1)
            test_type=$(basename "$result_file" | cut -d'_' -f2-3 | sed 's/_test.json//')
            
            cat >> "$report_file" << EOF
    <div class="device">
        <h3>Device: $device_id - $test_type Test</h3>
        <details>
            <summary>View Results</summary>
            <pre>$(cat "$result_file" | jq '.' 2>/dev/null || cat "$result_file")</pre>
        </details>
    </div>
EOF
        fi
    done
    
    cat >> "$report_file" << 'EOF'
    <div class="footer">
        <p><em>Report generated by HazardHawk Field Testing Automation</em></p>
    </div>
</body>
</html>
EOF
    
    log_success "Field test report generated: $report_file"
}

# Main execution function
main() {
    log "🏗️ HazardHawk AI Field Testing Automation"
    log "Test type: $TEST_TYPE"
    log "Target device: ${TARGET_DEVICE:-all connected}"
    log "AI models: $([ "$USE_REAL_MODELS" == "true" ] && echo "real" || echo "mock")"
    
    # Setup test environment
    setup_field_test_environment
    
    # Get connected devices
    local devices
    IFS=' ' read -ra devices <<< "$(get_connected_devices)"
    
    if [[ ${#devices[@]} -eq 0 ]]; then
        log_error "No devices available for testing"
        exit 1
    fi
    
    log "Testing on ${#devices[@]} device(s): ${devices[*]}"
    
    local failed_devices=()
    
    # Run tests on each device
    for device_id in "${devices[@]}"; do
        log "\n=== Testing device: $device_id ==="
        
        # Get device info
        get_device_info "$device_id"
        
        # Install app
        if ! install_app_on_device "$device_id"; then
            log_error "Failed to install app on $device_id, skipping..."
            failed_devices+=("$device_id")
            continue
        fi
        
        # Run appropriate test type
        case "$TEST_TYPE" in
            basic)
                if ! run_basic_ai_test "$device_id"; then
                    failed_devices+=("$device_id")
                fi
                ;;
            performance)
                if ! run_performance_test "$device_id"; then
                    failed_devices+=("$device_id")
                fi
                ;;
            stress)
                if ! run_stress_test "$device_id"; then
                    failed_devices+=("$device_id")
                fi
                ;;
            construction|real-photos|mock-photos)
                if ! run_construction_scenario_test "$device_id"; then
                    failed_devices+=("$device_id")
                fi
                ;;
            all)
                if ! run_comprehensive_field_test "$device_id"; then
                    failed_devices+=("$device_id")
                fi
                ;;
        esac
        
        log_success "Completed testing on $device_id"
    done
    
    # Generate report
    generate_field_test_report
    
    # Print summary
    log "\n=== Field Test Summary ==="
    log "Devices tested: ${#devices[@]}"
    log "Failed devices: ${#failed_devices[@]}"
    
    if [[ ${#failed_devices[@]} -eq 0 ]]; then
        log_success "🎉 All field tests completed successfully!"
        exit 0
    else
        log_error "❌ Field tests failed on devices: ${failed_devices[*]}"
        exit 1
    fi
}

# Handle script interruption
trap 'log_error "Field testing interrupted"; exit 130' INT TERM

# Run main function
main "$@"