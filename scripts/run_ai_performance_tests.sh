#!/bin/bash
# run_ai_performance_tests.sh
# Comprehensive AI Performance Testing Suite

set -e

echo "⚡ Running AI Performance Tests for HazardHawk..."

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Performance test results tracking
PERFORMANCE_TESTS=0
PERFORMANCE_PASSED=0
PERFORMANCE_FAILED=0

# Performance thresholds (configurable)
MAX_RESPONSE_TIME_MS=10000  # 10 seconds
MAX_MEMORY_USAGE_MB=100     # 100 MB
MAX_NETWORK_USAGE_MB=5      # 5 MB per request
TARGET_RESPONSE_TIME_MS=5000 # 5 seconds target

# Function to run performance test
run_performance_test() {
    local test_name="$1"
    local test_command="$2"
    local success_criteria="$3"
    
    echo -e "${BLUE}🚀 Running: ${test_name}${NC}"
    echo "Success Criteria: ${success_criteria}"
    
    ((PERFORMANCE_TESTS++))
    
    if eval "$test_command"; then
        echo -e "${GREEN}✅ ${test_name} - PASSED${NC}"
        ((PERFORMANCE_PASSED++))
    else
        echo -e "${RED}❌ ${test_name} - FAILED${NC}"
        ((PERFORMANCE_FAILED++))
    fi
    
    echo ""
}

echo -e "${YELLOW}🚀 Starting AI Performance Test Suite${NC}"
echo "======================================"

# Change to project directory
if [ ! -d "HazardHawk" ]; then
    echo -e "${RED}❌ HazardHawk directory not found. Please run from project root.${NC}"
    exit 1
fi

# 1. API Response Time Tests
run_performance_test \
    "API Response Time Test" \
    'cd HazardHawk && ./gradlew :shared:testDebugUnitTest --tests "*GeminiVisionAnalyzer*ResponseTime*" --info' \
    "< ${MAX_RESPONSE_TIME_MS}ms response time"

# 2. Memory Usage Tests
run_performance_test \
    "Memory Usage Test" \
    'cd HazardHawk && ./gradlew :shared:testDebugUnitTest --tests "*GeminiVisionAnalyzer*Memory*" --info' \
    "< ${MAX_MEMORY_USAGE_MB}MB memory usage"

# 3. Large Photo Processing Tests
run_performance_test \
    "Large Photo Processing Test" \
    'cd HazardHawk && ./gradlew :shared:testDebugUnitTest --tests "*GeminiVisionAnalyzer*LargePhoto*" --info' \
    "Handle 20MB+ photos efficiently"

# 4. Concurrent Request Tests
run_performance_test \
    "Concurrent Request Test" \
    'cd HazardHawk && ./gradlew :shared:testDebugUnitTest --tests "*GeminiVisionAnalyzer*Concurrent*" --info' \
    "Handle 3+ simultaneous requests"

# 5. Network Bandwidth Tests
run_performance_test \
    "Network Bandwidth Test" \
    'cd HazardHawk && ./gradlew :shared:testDebugUnitTest --tests "*GeminiVisionAnalyzer*Bandwidth*" --info' \
    "< ${MAX_NETWORK_USAGE_MB}MB per request"

# 6. Cold Start Performance Tests
run_performance_test \
    "Cold Start Performance Test" \
    'cd HazardHawk && ./gradlew :shared:testDebugUnitTest --tests "*GeminiVisionAnalyzer*ColdStart*" --info' \
    "< 3s initialization time"

# 7. Battery Impact Tests (Android)
run_performance_test \
    "Battery Impact Test" \
    'cd HazardHawk && ./gradlew :androidApp:testDebugUnitTest --tests "*GeminiVisionAnalyzer*Battery*" --info' \
    "< 5% battery per analysis session"

# 8. Cache Performance Tests
run_performance_test \
    "Cache Performance Test" \
    'cd HazardHawk && ./gradlew :shared:testDebugUnitTest --tests "*GeminiVisionAnalyzer*Cache*" --info' \
    "90%+ cache hit rate for repeated requests"

echo "======================================"
echo -e "${BLUE}📊 PERFORMANCE TEST SUMMARY${NC}"
echo "======================================"
echo -e "Total Tests: ${BLUE}${PERFORMANCE_TESTS}${NC}"
echo -e "Tests Passed: ${GREEN}${PERFORMANCE_PASSED}${NC}"
echo -e "Tests Failed: ${RED}${PERFORMANCE_FAILED}${NC}"

# Performance recommendations
echo ""
echo -e "${YELLOW}📋 PERFORMANCE RECOMMENDATIONS${NC}"
echo "=================================="
echo "• Target API response time: < ${TARGET_RESPONSE_TIME_MS}ms"
echo "• Maximum API response time: < ${MAX_RESPONSE_TIME_MS}ms"
echo "• Memory usage limit: < ${MAX_MEMORY_USAGE_MB}MB"
echo "• Network usage limit: < ${MAX_NETWORK_USAGE_MB}MB per request"
echo "• Implement request caching for repeated analyses"
echo "• Use image compression before API upload"
echo "• Implement progressive loading for large images"

if [ $PERFORMANCE_FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All AI Performance Tests PASSED${NC}"
    echo -e "${GREEN}🚀 AI implementation meets performance requirements${NC}"
    exit 0
else
    echo -e "${RED}❌ ${PERFORMANCE_FAILED} Performance Test(s) FAILED${NC}"
    echo -e "${YELLOW}⚠️  Performance optimization required before production${NC}"
    exit 1
fi
