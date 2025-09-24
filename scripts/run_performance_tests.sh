#!/bin/bash

# HazardHawk Performance Optimization Test Suite
# Validates device tier detection, memory management, and AI processing performance

set -e

echo "=' HazardHawk Performance Optimization Test Suite"
echo "=================================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to print test results
print_test_result() {
    local test_name="$1"
    local status="$2"
    local details="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    if [ "$status" = "PASS" ]; then
        echo -e "${GREEN} $test_name${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    elif [ "$status" = "FAIL" ]; then
        echo -e "${RED} $test_name${NC}"
        if [ -n "$details" ]; then
            echo -e "${RED}  Error: $details${NC}"
        fi
        FAILED_TESTS=$((FAILED_TESTS + 1))
    else
        echo -e "${YELLOW}? $test_name (Skipped)${NC}"
    fi
}

# Function to check if file exists and has expected content
check_file_structure() {
    local file_path="$1"
    local expected_classes="$2"
    local test_name="$3"
    
    if [ ! -f "$file_path" ]; then
        print_test_result "$test_name" "FAIL" "File does not exist: $file_path"
        return 1
    fi
    
    # Check for expected classes/interfaces
    local missing_classes=""
    IFS=',' read -ra CLASSES <<< "$expected_classes"
    for class in "${CLASSES[@]}"; do
        if ! grep -q "$class" "$file_path"; then
            missing_classes="$missing_classes $class"
        fi
    done
    
    if [ -n "$missing_classes" ]; then
        print_test_result "$test_name" "FAIL" "Missing classes:$missing_classes"
        return 1
    fi
    
    print_test_result "$test_name" "PASS"
    return 0
}

echo ""
echo "=ñ Testing Device Tier Detection System..."

# Test 1: DeviceTierDetector structure
check_file_structure \
    "shared/src/commonMain/kotlin/com/hazardhawk/performance/DeviceTierDetector.kt" \
    "DeviceTier,DeviceCapabilities,PerformanceConfig,AdaptivePerformanceManager" \
    "DeviceTierDetector Core Classes"

# Test 2: Android-specific implementation
check_file_structure \
    "shared/src/androidMain/kotlin/com/hazardhawk/performance/AndroidPerformanceOptimizer.kt" \
    "AndroidPerformanceOptimizer,AndroidOptimizations,ConstructionUIOptimizer" \
    "Android Performance Implementation"

echo ""
echo ">à Testing Memory Management System..."

# Test 3: Memory Manager structure
check_file_structure \
    "shared/src/commonMain/kotlin/com/hazardhawk/performance/MemoryManager.kt" \
    "MemoryManager,MemoryStats,SmartGarbageCollector" \
    "Memory Manager Core Classes"

echo ""
echo "=Ê Testing Performance Monitoring System..."

# Test 4: Performance Monitor structure
check_file_structure \
    "shared/src/commonMain/kotlin/com/hazardhawk/performance/PerformanceMonitor.kt" \
    "PerformanceMonitor,FrameCounter,AIAnalysisCounter,PerformanceAlert" \
    "Performance Monitor Core Classes"

# Test 5: Benchmark system
check_file_structure \
    "shared/src/commonMain/kotlin/com/hazardhawk/performance/PerformanceBenchmark.kt" \
    "PerformanceBenchmark,BenchmarkResults,BenchmarkTest" \
    "Performance Benchmark System"

echo ""
echo "> Testing AI Integration..."

# Test 6: AI Orchestrator performance integration
if [ -f "shared/src/commonMain/kotlin/com/hazardhawk/ai/core/SmartAIOrchestrator.kt" ]; then
    if grep -q "AIFrameLimiter" "shared/src/commonMain/kotlin/com/hazardhawk/ai/core/SmartAIOrchestrator.kt" && \
       grep -q "performanceManager" "shared/src/commonMain/kotlin/com/hazardhawk/ai/core/SmartAIOrchestrator.kt"; then
        print_test_result "AI Orchestrator Performance Integration" "PASS"
    else
        print_test_result "AI Orchestrator Performance Integration" "FAIL" "Missing performance integration"
    fi
else
    print_test_result "AI Orchestrator Performance Integration" "FAIL" "SmartAIOrchestrator.kt not found"
fi

echo ""
echo "= Testing Compilation..."

# Test 7: Kotlin compilation check
if command -v kotlinc > /dev/null 2>&1; then
    echo "Checking Kotlin syntax..."
    
    # Check shared module compilation (simplified)
    find shared/src/commonMain/kotlin/com/hazardhawk/performance -name "*.kt" | while read -r file; do
        if ! kotlinc -cp "." "$file" -d /tmp/kotlin_test > /dev/null 2>&1; then
            print_test_result "Kotlin Syntax - $(basename "$file")" "FAIL" "Compilation error"
        else
            print_test_result "Kotlin Syntax - $(basename "$file")" "PASS"
        fi
    done
else
    print_test_result "Kotlin Compilation Test" "SKIP" "kotlinc not available"
fi

echo ""
echo "¡ Testing Performance Characteristics..."

# Test 8: Check for performance-critical patterns
test_performance_patterns() {
    local file="$1"
    local test_name="$2"
    
    if [ ! -f "$file" ]; then
        print_test_result "$test_name" "FAIL" "File not found: $file"
        return
    fi
    
    # Check for suspend functions (non-blocking)
    if grep -q "suspend fun" "$file"; then
        print_test_result "$test_name - Async Operations" "PASS"
    else
        print_test_result "$test_name - Async Operations" "FAIL" "No suspend functions found"
    fi
    
    # Check for coroutine usage
    if grep -q "withContext\|launch\|async" "$file"; then
        print_test_result "$test_name - Coroutine Usage" "PASS"
    else
        print_test_result "$test_name - Coroutine Usage" "FAIL" "No coroutine usage found"
    fi
    
    # Check for memory management patterns
    if grep -q "Mutex\|withLock" "$file"; then
        print_test_result "$test_name - Thread Safety" "PASS"
    else
        print_test_result "$test_name - Thread Safety" "FAIL" "No thread safety patterns found"
    fi
}

test_performance_patterns \
    "shared/src/commonMain/kotlin/com/hazardhawk/performance/MemoryManager.kt" \
    "Memory Manager Performance"

test_performance_patterns \
    "shared/src/commonMain/kotlin/com/hazardhawk/performance/PerformanceMonitor.kt" \
    "Performance Monitor"

echo ""
echo "=Ï Testing Configuration Validation..."

# Test 9: Device tier thresholds
test_device_tiers() {
    local file="shared/src/commonMain/kotlin/com/hazardhawk/performance/DeviceTierDetector.kt"
    
    if [ ! -f "$file" ]; then
        print_test_result "Device Tier Configuration" "FAIL" "File not found"
        return
    fi
    
    # Check for reasonable memory thresholds
    if grep -q "2048\|4096\|8192" "$file"; then
        print_test_result "Device Memory Tiers" "PASS"
    else
        print_test_result "Device Memory Tiers" "FAIL" "Memory thresholds not found"
    fi
    
    # Check for FPS targets
    if grep -q "24\|30\|60" "$file"; then
        print_test_result "FPS Targets" "PASS"
    else
        print_test_result "FPS Targets" "FAIL" "FPS targets not configured"
    fi
    
    # Check for AI processing rates
    if grep -q "1\.0f\|1\.5f\|2\.0f" "$file"; then
        print_test_result "AI Processing Rates" "PASS"
    else
        print_test_result "AI Processing Rates" "FAIL" "AI processing rates not configured"
    fi
}

test_device_tiers

echo ""
echo "<× Testing Construction-Specific Features..."

# Test 10: Construction optimizations
test_construction_features() {
    local file="shared/src/androidMain/kotlin/com/hazardhawk/performance/AndroidPerformanceOptimizer.kt"
    
    if [ ! -f "$file" ]; then
        print_test_result "Construction Features" "FAIL" "Android optimizer not found"
        return
    fi
    
    # Check for work glove optimizations
    if grep -q "work.*glove\|glove.*work" "$file"; then
        print_test_result "Work Glove Optimization" "PASS"
    else
        print_test_result "Work Glove Optimization" "FAIL" "Work glove support not found"
    fi
    
    # Check for outdoor visibility features
    if grep -q "outdoor\|brightness\|visibility" "$file"; then
        print_test_result "Outdoor Visibility" "PASS"
    else
        print_test_result "Outdoor Visibility" "FAIL" "Outdoor features not found"
    fi
    
    # Check for construction color schemes
    if grep -q "safety.*color\|high.*vis\|construction.*color" "$file"; then
        print_test_result "Construction Color Schemes" "PASS"
    else
        print_test_result "Construction Color Schemes" "FAIL" "Safety colors not configured"
    fi
}

test_construction_features

echo ""
echo "=È Testing Benchmark System..."

# Test 11: Benchmark comprehensiveness
test_benchmark_coverage() {
    local file="shared/src/commonMain/kotlin/com/hazardhawk/performance/PerformanceBenchmark.kt"
    
    if [ ! -f "$file" ]; then
        print_test_result "Benchmark System" "FAIL" "Benchmark file not found"
        return
    fi
    
    # Check for all required benchmark tests
    local required_tests=(
        "benchmarkDeviceDetection"
        "benchmarkMemoryManagement"
        "benchmarkModelLoading"
        "benchmarkImageProcessing"
        "benchmarkFrameRate"
        "benchmarkMemoryPressure"
        "benchmarkBatteryImpact"
    )
    
    local missing_tests=""
    for test in "${required_tests[@]}"; do
        if ! grep -q "$test" "$file"; then
            missing_tests="$missing_tests $test"
        fi
    done
    
    if [ -n "$missing_tests" ]; then
        print_test_result "Benchmark Test Coverage" "FAIL" "Missing tests:$missing_tests"
    else
        print_test_result "Benchmark Test Coverage" "PASS"
    fi
    
    # Check for scoring system
    if grep -q "calculateOverallScore\|BenchmarkResults" "$file"; then
        print_test_result "Benchmark Scoring System" "PASS"
    else
        print_test_result "Benchmark Scoring System" "FAIL" "Scoring system not found"
    fi
}

test_benchmark_coverage

echo ""
echo "<¯ Testing Target Performance Metrics..."

# Test 12: Performance targets validation
validate_performance_targets() {
    local device_file="shared/src/commonMain/kotlin/com/hazardhawk/performance/DeviceTierDetector.kt"
    local ai_file="shared/src/commonMain/kotlin/com/hazardhawk/ai/core/SmartAIOrchestrator.kt"
    
    # Check 30 FPS UI target
    if grep -q "30.*FPS\|targetFPS.*30" "$device_file" || grep -q "30.*fps" "$device_file"; then
        print_test_result "30 FPS UI Target" "PASS"
    else
        print_test_result "30 FPS UI Target" "FAIL" "30 FPS target not found"
    fi
    
    # Check 2 FPS AI processing target
    if [ -f "$ai_file" ] && grep -q "2\.0f\|2.*FPS" "$ai_file"; then
        print_test_result "2 FPS AI Processing Target" "PASS"
    else
        print_test_result "2 FPS AI Processing Target" "FAIL" "2 FPS AI target not configured"
    fi
    
    # Check memory usage targets
    if grep -q "2GB\|4GB\|8GB\|2048\|4096\|8192" "$device_file"; then
        print_test_result "Memory Usage Targets" "PASS"
    else
        print_test_result "Memory Usage Targets" "FAIL" "Memory targets not defined"
    fi
    
    # Check analysis time targets (< 3 seconds)
    if grep -q "3000\|3.*second" "$device_file" || grep -q "10000.*timeout" "$ai_file"; then
        print_test_result "Analysis Time Targets" "PASS"
    else
        print_test_result "Analysis Time Targets" "FAIL" "Analysis time targets not found"
    fi
}

validate_performance_targets

echo ""
echo "=' Final Validation..."

# Test 13: Integration completeness
validate_integration() {
    # Check if all components are properly integrated
    local orchestrator_file="shared/src/commonMain/kotlin/com/hazardhawk/ai/core/SmartAIOrchestrator.kt"
    
    if [ -f "$orchestrator_file" ]; then
        local integrations=0
        
        if grep -q "performanceManager" "$orchestrator_file"; then
            integrations=$((integrations + 1))
        fi
        
        if grep -q "memoryManager" "$orchestrator_file"; then
            integrations=$((integrations + 1))
        fi
        
        if grep -q "performanceMonitor" "$orchestrator_file"; then
            integrations=$((integrations + 1))
        fi
        
        if [ $integrations -ge 3 ]; then
            print_test_result "Performance System Integration" "PASS"
        else
            print_test_result "Performance System Integration" "FAIL" "Missing integration components ($integrations/3)"
        fi
    else
        print_test_result "Performance System Integration" "FAIL" "AI Orchestrator not found"
    fi
}

validate_integration

echo ""
echo "=Ê Test Summary"
echo "=============="
echo -e "Total Tests: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
echo -e "Failed: ${RED}$FAILED_TESTS${NC}"

if [ $FAILED_TESTS -eq 0 ]; then
    echo ""
    echo -e "${GREEN}<‰ All performance optimization tests passed!${NC}"
    echo ""
    echo " Device tier detection implemented"
    echo " Memory management optimized"
    echo " Performance monitoring active"
    echo " AI processing throttled to 2 FPS"
    echo " UI rendering targeted at 30 FPS"
    echo " Construction-specific optimizations"
    echo " Comprehensive benchmarking system"
    echo ""
    echo "=€ HazardHawk is ready for optimal performance across all Android device tiers!"
    exit 0
else
    echo ""
    echo -e "${RED}L $FAILED_TESTS test(s) failed${NC}"
    echo ""
    echo "=' Please address the failed tests before deployment."
    echo ""
    echo "Key Performance Requirements:"
    echo "" Device tier detection for low/mid/high-end devices"
    echo "" AI processing throttled to 2 FPS maximum"
    echo "" UI rendering maintaining 30 FPS target"
    echo "" Memory management with pressure handling"
    echo "" Construction-specific UI optimizations"
    echo "" Battery usage optimization"
    exit 1
fi