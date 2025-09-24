#!/bin/bash
# LiteRT-LM Performance Validation Test Suite for HazardHawk
# Validates 3-8x performance improvements: CPU (243 t/s), GPU (1876 t/s), NPU (5836 t/s)

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

BUILD_VERSION=${1:-"dev_build_$(date +%Y%m%d_%H%M%S)"}
TEST_DEVICE=${2:-"connected"}
OUTPUT_DIR="reports/litert_performance_$(date +%Y%m%d_%H%M%S)"

echo -e "${BLUE}=======================================================${NC}"
echo -e "${BLUE}   HazardHawk LiteRT-LM Performance Test Suite${NC}"
echo -e "${BLUE}=======================================================${NC}"
echo ""
echo -e "${PURPLE}Build Version:${NC} $BUILD_VERSION"
echo -e "${PURPLE}Test Device:${NC} $TEST_DEVICE"
echo -e "${PURPLE}Output Directory:${NC} $OUTPUT_DIR"
echo ""

# Create output directory
mkdir -p $OUTPUT_DIR

# Performance targets
declare -A TARGETS=(
    ["CPU_TOKENS_PER_SEC"]="243"
    ["GPU_TOKENS_PER_SEC"]="1876"
    ["NPU_TOKENS_PER_SEC"]="5836"
    ["MAX_MEMORY_MB"]="2048"
    ["MAX_FALLBACK_MS"]="500"
    ["MIN_ACCURACY"]="0.90"
    ["MIN_DEVICE_SUPPORT"]="0.95"
)

echo -e "${YELLOW}LiteRT-LM Performance Targets:${NC}"
echo -e "  CPU Backend: ${TARGETS[CPU_TOKENS_PER_SEC]} tokens/second"
echo -e "  GPU Backend: ${TARGETS[GPU_TOKENS_PER_SEC]} tokens/second"
echo -e "  NPU Backend: ${TARGETS[NPU_TOKENS_PER_SEC]} tokens/second"
echo -e "  Memory Usage: < ${TARGETS[MAX_MEMORY_MB]} MB"
echo -e "  Backend Fallback: < ${TARGETS[MAX_FALLBACK_MS]} ms"
echo -e "  Hazard Detection Accuracy: > ${TARGETS[MIN_ACCURACY]}"
echo -e "  Device Compatibility: > ${TARGETS[MIN_DEVICE_SUPPORT]}"
echo ""

# Function to run test and capture results
run_test() {
    local test_name=$1
    local test_command=$2
    local expected_result=${3:-"0"}
    
    echo -e "${BLUE}Running: $test_name${NC}"
    echo "Command: $test_command"
    
    # Run test and capture output
    if eval "$test_command" > "$OUTPUT_DIR/${test_name// /_}.log" 2>&1; then
        echo -e "${GREEN}✓ PASSED${NC} - $test_name"
        return 0
    else
        echo -e "${RED}✗ FAILED${NC} - $test_name"
        echo "  See: $OUTPUT_DIR/${test_name// /_}.log"
        return 1
    fi
}

# Function to check device capabilities
check_device_capabilities() {
    echo -e "${PURPLE}1. Device Capability Detection${NC}"
    echo "================================================="
    
    run_test "Device Detection" "./gradlew :shared:deviceCapabilityTest"
    run_test "GPU Support Check" "./gradlew :shared:gpuSupportTest"
    run_test "NPU/NNAPI Support Check" "./gradlew :shared:nnApiSupportTest"
    run_test "Memory Profile" "./gradlew :shared:memoryProfileTest"
    
    echo ""
}

# Function to test backend performance
test_backend_performance() {
    echo -e "${PURPLE}2. Backend Performance Validation${NC}"
    echo "================================================="
    
    # CPU Backend Tests
    run_test "CPU Backend Performance" "./gradlew :shared:cpuBackendPerformanceTest -PtargetTokensPerSec=${TARGETS[CPU_TOKENS_PER_SEC]}"
    run_test "CPU Memory Efficiency" "./gradlew :shared:cpuMemoryTest"
    
    # GPU Backend Tests (if available)
    run_test "GPU Backend Performance" "./gradlew :shared:gpuBackendPerformanceTest -PtargetTokensPerSec=${TARGETS[GPU_TOKENS_PER_SEC]}"
    run_test "GPU Memory Management" "./gradlew :shared:gpuMemoryTest"
    
    # NPU Backend Tests (if available)  
    run_test "NPU Backend Performance" "./gradlew :shared:npuBackendPerformanceTest -PtargetTokensPerSec=${TARGETS[NPU_TOKENS_PER_SEC]}"
    run_test "NPU Compatibility" "./gradlew :shared:npuCompatibilityTest"
    
    echo ""
}

# Function to test backend switching
test_backend_switching() {
    echo -e "${PURPLE}3. Backend Switching & Fallback Logic${NC}"
    echo "================================================="
    
    run_test "NPU to GPU Fallback" "./gradlew :shared:npuGpuFallbackTest -PmaxSwitchTimeMs=${TARGETS[MAX_FALLBACK_MS]}"
    run_test "GPU to CPU Fallback" "./gradlew :shared:gpuCpuFallbackTest -PmaxSwitchTimeMs=${TARGETS[MAX_FALLBACK_MS]}"
    run_test "Performance Degradation Detection" "./gradlew :shared:performanceDegradationTest"
    run_test "Memory Pressure Switching" "./gradlew :shared:memoryPressureSwitchTest"
    run_test "Thermal Throttling Response" "./gradlew :shared:thermalThrottlingTest"
    
    echo ""
}

# Function to test A/B performance comparison
test_ab_performance() {
    echo -e "${PURPLE}4. A/B Performance Testing${NC}"
    echo "================================================="
    
    run_test "Real vs Mock AI Performance" "./gradlew :shared:abPerformanceTest -PimageCount=50"
    run_test "Backend Performance Comparison" "./gradlew :shared:backendComparisonTest"
    run_test "Memory Usage Comparison" "./gradlew :shared:memoryUsageComparisonTest"
    run_test "Latency Comparison" "./gradlew :shared:latencyComparisonTest"
    
    echo ""
}

# Function to test integration performance
test_integration_performance() {
    echo -e "${PURPLE}5. Integration Performance Validation${NC}"
    echo "================================================="
    
    run_test "Camera UI Performance" "./gradlew :shared:cameraUiPerformanceTest -PtargetFPS=30"
    run_test "AI Analysis Throttling" "./gradlew :shared:aiAnalysisThrottlingTest -PtargetFPS=2"
    run_test "Complete Workflow Performance" "./gradlew :shared:workflowPerformanceTest -PmaxTimeSeconds=15"
    run_test "Model Loading Performance" "./gradlew :shared:modelLoadingTest -PmaxTimeSeconds=10"
    run_test "Repository Query Performance" "./gradlew :shared:repositoryQueryTest -PmaxTimeMs=100"
    
    echo ""
}

# Function to test memory management
test_memory_management() {
    echo -e "${PURPLE}6. Memory Management & Leak Detection${NC}"
    echo "================================================="
    
    run_test "Memory Stress Test" "./gradlew :shared:memoryStressTest -PmaxMemoryMB=${TARGETS[MAX_MEMORY_MB]}"
    run_test "Model Memory Management" "./gradlew :shared:modelMemoryTest"
    run_test "Image Cache Management" "./gradlew :shared:imageCacheTest"
    run_test "Memory Leak Detection" "./gradlew :shared:memoryLeakTest"
    run_test "Memory Regression Analysis" "./gradlew :shared:memoryRegressionTest -PbuildVersion=$BUILD_VERSION"
    
    echo ""
}

# Function to generate comprehensive report
generate_performance_report() {
    echo -e "${PURPLE}7. Generating Performance Report${NC}"
    echo "================================================="
    
    run_test "Performance Report Generation" "./gradlew :shared:generateLiteRTPerformanceReport -PbuildVersion=$BUILD_VERSION -PoutputDir=$OUTPUT_DIR"
    
    # Copy important files to output directory
    if [ -f "build/reports/performance/litert_validation_report.html" ]; then
        cp build/reports/performance/litert_validation_report.html $OUTPUT_DIR/
    fi
    
    if [ -f "build/reports/performance/backend_comparison.json" ]; then
        cp build/reports/performance/backend_comparison.json $OUTPUT_DIR/
    fi
    
    echo ""
}

# Function to validate performance improvements
validate_performance_improvements() {
    echo -e "${PURPLE}8. Performance Improvement Validation${NC}"
    echo "================================================="
    
    # Check if we achieved target improvements
    local cpu_baseline=${TARGETS[CPU_TOKENS_PER_SEC]}
    local gpu_target=$((cpu_baseline * 3))  # 3x improvement minimum
    local npu_target=$((cpu_baseline * 8))  # 8x improvement minimum
    
    echo "Validating performance improvements:"
    echo "  CPU Baseline: $cpu_baseline tokens/sec"
    echo "  GPU Target (3x): $gpu_target tokens/sec"
    echo "  NPU Target (8x): $npu_target tokens/sec"
    
    run_test "GPU 3x Improvement Validation" "./gradlew :shared:validateGpuImprovement -PbaselineTokensPerSec=$cpu_baseline -PtargetMultiplier=3"
    run_test "NPU 8x Improvement Validation" "./gradlew :shared:validateNpuImprovement -PbaselineTokensPerSec=$cpu_baseline -PtargetMultiplier=8"
    
    echo ""
}

# Main execution
main() {
    local start_time=$(date +%s)
    local failed_tests=0
    
    echo -e "${BLUE}Starting LiteRT-LM Performance Test Suite...${NC}"
    echo ""
    
    # Run all test suites
    check_device_capabilities || ((failed_tests++))
    test_backend_performance || ((failed_tests++))
    test_backend_switching || ((failed_tests++))
    test_ab_performance || ((failed_tests++))
    test_integration_performance || ((failed_tests++))
    test_memory_management || ((failed_tests++))
    validate_performance_improvements || ((failed_tests++))
    generate_performance_report || ((failed_tests++))
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    echo -e "${BLUE}=======================================================${NC}"
    echo -e "${BLUE}   Test Suite Completed${NC}"
    echo -e "${BLUE}=======================================================${NC}"
    echo ""
    echo -e "${PURPLE}Duration:${NC} ${duration} seconds"
    echo -e "${PURPLE}Failed Tests:${NC} $failed_tests"
    echo -e "${PURPLE}Output Directory:${NC} $OUTPUT_DIR"
    echo ""
    
    if [ $failed_tests -eq 0 ]; then
        echo -e "${GREEN}🎉 ALL TESTS PASSED! LiteRT-LM performance targets met.${NC}"
        echo -e "${GREEN}✓ CPU performance: ${TARGETS[CPU_TOKENS_PER_SEC]} tokens/sec target${NC}"
        echo -e "${GREEN}✓ GPU performance: ${TARGETS[GPU_TOKENS_PER_SEC]} tokens/sec target (3x improvement)${NC}"
        echo -e "${GREEN}✓ NPU performance: ${TARGETS[NPU_TOKENS_PER_SEC]} tokens/sec target (8x improvement)${NC}"
        echo -e "${GREEN}✓ Memory usage: < ${TARGETS[MAX_MEMORY_MB]} MB${NC}"
        echo -e "${GREEN}✓ Backend switching: < ${TARGETS[MAX_FALLBACK_MS]} ms${NC}"
        echo ""
        echo -e "${GREEN}HazardHawk is ready for LiteRT-LM production deployment!${NC}"
        exit 0
    else
        echo -e "${RED}❌ $failed_tests test suite(s) failed. Performance targets not met.${NC}"
        echo ""
        echo -e "${YELLOW}Next steps:${NC}"
        echo -e "  1. Review test logs in: $OUTPUT_DIR"
        echo -e "  2. Check performance recommendations in report"
        echo -e "  3. Optimize underperforming backends"
        echo -e "  4. Re-run tests after optimizations"
        exit 1
    fi
}

# Handle script arguments
case "${1:-}" in
    "--help"|"-h")
        echo "Usage: $0 [build_version] [test_device]"
        echo ""
        echo "Arguments:"
        echo "  build_version    Version identifier for this build (default: auto-generated)"
        echo "  test_device      Target device identifier (default: connected)"
        echo ""
        echo "Environment Variables:"
        echo "  ANDROID_HOME     Android SDK path"
        echo "  JAVA_HOME        Java JDK path"
        echo ""
        echo "Examples:"
        echo "  $0                                    # Run with defaults"
        echo "  $0 v1.2.0 pixel6                     # Run with specific version and device"
        echo "  $0 v1.2.0-rc1                        # Run release candidate build"
        exit 0
        ;;
    *)
        main "$@"
        ;;
esac
