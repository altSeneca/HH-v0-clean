#!/bin/bash

# HazardHawk AI Analysis & Dialog UX Testing Suite Runner
# Executes comprehensive testing strategy for AI integration and UX improvements

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
PROJECT_DIR="/Users/aaron/Apps-Coded/HH-v0/HazardHawk"
TEST_RESULTS_DIR="test-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo -e "${BLUE}🔬 HazardHawk AI Analysis & Dialog UX Testing Suite${NC}"
echo -e "${BLUE}==========================================================${NC}"
echo "Timestamp: $TIMESTAMP"
echo "Project Directory: $PROJECT_DIR"
echo ""

# Create results directory
mkdir -p "$TEST_RESULTS_DIR"

# Function to run test category
run_test_category() {
    local category=$1
    local test_pattern=$2
    local description=$3
    
    echo -e "${YELLOW}📋 $description${NC}"
    echo "Pattern: $test_pattern"
    
    cd "$PROJECT_DIR"
    
    if ./gradlew test --tests "$test_pattern" --continue; then
        echo -e "${GREEN}✅ $category tests passed${NC}"
        return 0
    else
        echo -e "${RED}❌ $category tests failed${NC}"
        return 1
    fi
}

# Function to run performance benchmarks
run_performance_tests() {
    echo -e "${YELLOW}⚡ Running Performance Tests${NC}"
    
    cd "$PROJECT_DIR"
    
    # Run with performance monitoring enabled
    if ./gradlew test --tests "*Performance*" -Pperformance.monitoring=true --continue; then
        echo -e "${GREEN}✅ Performance tests passed${NC}"
        return 0
    else
        echo -e "${RED}❌ Performance tests failed${NC}"
        return 1
    fi
}

# Function to generate test report
generate_test_report() {
    echo -e "${BLUE}📊 Generating Test Reports${NC}"
    
    cd "$PROJECT_DIR"
    
    # Generate JaCoCo coverage report
    ./gradlew jacocoTestReport
    
    # Generate performance report
    if [ -f "build/reports/performance/performance-report.html" ]; then
        cp "build/reports/performance/performance-report.html" "../$TEST_RESULTS_DIR/performance-$TIMESTAMP.html"
    fi
    
    # Copy test results
    if [ -d "build/reports/tests" ]; then
        cp -r "build/reports/tests" "../$TEST_RESULTS_DIR/test-reports-$TIMESTAMP"
    fi
    
    if [ -d "build/reports/jacoco" ]; then
        cp -r "build/reports/jacoco" "../$TEST_RESULTS_DIR/coverage-$TIMESTAMP"
    fi
    
    echo -e "${GREEN}✅ Reports generated in $TEST_RESULTS_DIR${NC}"
}

# Main test execution
main() {
    local exit_code=0
    
    echo -e "${BLUE}🚀 Starting Comprehensive Test Suite${NC}"
    echo ""
    
    # 1. AI Service Integration Tests
    echo -e "${BLUE}=== AI SERVICE INTEGRATION TESTS ===${NC}"
    if ! run_test_category "AI_Integration" "*AIAnalysisIntegration*" "Testing AI service stub replacement and actual integration"; then
        exit_code=1
    fi
    echo ""
    
    # 2. API Key Configuration Tests
    echo -e "${BLUE}=== API KEY CONFIGURATION TESTS ===${NC}"
    if ! run_test_category "API_Config" "*AIConfig*,*Security*" "Testing API key storage, validation, and rotation"; then
        exit_code=1
    fi
    echo ""
    
    # 3. Dialog UX Tests
    echo -e "${BLUE}=== DIALOG UX RESPONSIVENESS TESTS ===${NC}"
    if ! run_test_category "Dialog_UX" "*ConstructionDialog*" "Testing dialog touch targets, responsiveness, and progressive disclosure"; then
        exit_code=1
    fi
    echo ""
    
    # 4. Error Handling Tests  
    echo -e "${BLUE}=== ERROR HANDLING & FALLBACK TESTS ===${NC}"
    if ! run_test_category "Error_Handling" "*Fallback*,*Error*" "Testing graceful error handling and fallback mechanisms"; then
        exit_code=1
    fi
    echo ""
    
    # 5. Construction Usability Tests
    echo -e "${BLUE}=== CONSTRUCTION USABILITY TESTS ===${NC}"
    if ! run_test_category "Usability" "*ConstructionUsability*" "Testing outdoor visibility, glove operation, and safety workflows"; then
        exit_code=1
    fi
    echo ""
    
    # 6. Performance Tests
    echo -e "${BLUE}=== PERFORMANCE & MEMORY TESTS ===${NC}"
    if ! run_performance_tests; then
        exit_code=1
    fi
    echo ""
    
    # 7. Generate Reports
    generate_test_report
    echo ""
    
    # Summary
    echo -e "${BLUE}=== TEST EXECUTION SUMMARY ===${NC}"
    if [ $exit_code -eq 0 ]; then
        echo -e "${GREEN}🎉 All test suites passed successfully!${NC}"
        echo -e "${GREEN}✅ AI Analysis Working: Actual responses replace stub data${NC}"
        echo -e "${GREEN}✅ API Key Configuration: Users can enter and validate keys${NC}"
        echo -e "${GREEN}✅ Dialog Responsiveness: All touch targets minimum 48dp${NC}"
        echo -e "${GREEN}✅ Error Handling: Graceful fallbacks when API unavailable${NC}"
        echo -e "${GREEN}✅ Construction Usability: Outdoor visibility and gloved operation${NC}"
    else
        echo -e "${RED}❌ Some test suites failed. Check individual results above.${NC}"
    fi
    
    echo ""
    echo "Test results saved to: $TEST_RESULTS_DIR"
    echo "Timestamp: $TIMESTAMP"
    
    return $exit_code
}

# Run specific test category if provided
if [ $# -eq 1 ]; then
    case $1 in
        "ai")
            run_test_category "AI_Integration" "*AIAnalysisIntegration*" "AI Service Integration Tests"
            ;;
        "dialog")
            run_test_category "Dialog_UX" "*ConstructionDialog*" "Dialog UX Tests"
            ;;
        "usability")
            run_test_category "Usability" "*ConstructionUsability*" "Construction Usability Tests"
            ;;
        "performance")
            run_performance_tests
            ;;
        "all")
            main
            ;;
        *)
            echo "Usage: $0 [ai|dialog|usability|performance|all]"
            echo "  ai         - Run AI integration tests only"
            echo "  dialog     - Run dialog UX tests only"
            echo "  usability  - Run construction usability tests only"
            echo "  performance - Run performance tests only"
            echo "  all        - Run all test suites (default)"
            exit 1
            ;;
    esac
else
    # Run all tests by default
    main
fi

exit $?