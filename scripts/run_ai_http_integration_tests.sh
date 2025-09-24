#!/bin/bash
# run_ai_http_integration_tests.sh
# Comprehensive AI HTTP Integration Test Suite

set -e

echo "🔄 Running AI HTTP Integration Tests for HazardHawk..."

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test results tracking
TESTS_PASSED=0
TESTS_FAILED=0
TEST_START_TIME=$(date +%s)

# Function to run test category
run_test_category() {
    local category_name="$1"
    local test_pattern="$2"
    
    echo -e "${BLUE}📋 Running ${category_name}...${NC}"
    
    if cd HazardHawk && ./gradlew :shared:testDebugUnitTest --tests "*${test_pattern}*" --info; then
        echo -e "${GREEN}✅ ${category_name} PASSED${NC}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ ${category_name} FAILED${NC}"
        ((TESTS_FAILED++))
    fi
    
    echo ""
}

# Change to project directory
if [ ! -d "HazardHawk" ]; then
    echo -e "${RED}❌ HazardHawk directory not found. Please run from project root.${NC}"
    exit 1
fi

echo -e "${YELLOW}🧹 Cleaning previous build artifacts...${NC}"
cd HazardHawk && ./gradlew clean

echo -e "${YELLOW}🔨 Building project...${NC}"
cd HazardHawk && ./gradlew build

echo ""
echo -e "${BLUE}🚀 Starting AI HTTP Integration Test Suite${NC}"
echo "======================================="

# 1. HTTP Client Unit Tests
run_test_category "HTTP Client Unit Tests" "GeminiVisionAnalyzer*Http"

# 2. Mock Server Integration Tests  
run_test_category "Mock Server Integration Tests" "GeminiMockServer"

# 3. Error Scenario Tests
run_test_category "Error Scenario Tests" "GeminiVisionAnalyzer*Error"

# 4. Performance Tests
run_test_category "Performance Tests" "GeminiVisionAnalyzer*Performance"

# 5. Network Activity Validation
run_test_category "Network Activity Validation" "GeminiNetwork"

# 6. Security Validation Tests
run_test_category "Security Validation Tests" "GeminiVisionAnalyzer*Security"

# 7. Integration Workflow Tests
run_test_category "Integration Workflow Tests" "GeminiVisionAnalyzer*Integration"

# Calculate test duration
TEST_END_TIME=$(date +%s)
TEST_DURATION=$((TEST_END_TIME - TEST_START_TIME))

echo ""
echo "========================================="
echo -e "${BLUE}📊 TEST SUITE SUMMARY${NC}"
echo "========================================="
echo -e "Tests Passed: ${GREEN}${TESTS_PASSED}${NC}"
echo -e "Tests Failed: ${RED}${TESTS_FAILED}${NC}"
echo -e "Duration: ${YELLOW}${TEST_DURATION}s${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ All AI HTTP Integration Tests PASSED${NC}"
    exit 0
else
    echo -e "${RED}❌ ${TESTS_FAILED} Test Categories FAILED${NC}"
    exit 1
fi
