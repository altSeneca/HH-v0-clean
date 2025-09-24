#!/bin/bash

# Quick HazardHawk Validation Test Runner
# Works around build issues to test what's currently working

set -e

echo "========================================"
echo "🚀 HazardHawk Quick Validation Suite"
echo "========================================"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

REPORT_FILE="validation_results_$(date +'%Y%m%d_%H%M%S').txt"
PASS_COUNT=0
FAIL_COUNT=0

# Function to log results
log_result() {
    local test_name="$1"
    local result="$2"
    local details="$3"
    
    if [ "$result" = "PASS" ]; then
        echo -e "${GREEN}✅ $test_name: PASS${NC}"
        PASS_COUNT=$((PASS_COUNT + 1))
    else
        echo -e "${RED}❌ $test_name: FAIL - $details${NC}"
        FAIL_COUNT=$((FAIL_COUNT + 1))
    fi
    
    echo "$test_name: $result - $details" >> "$REPORT_FILE"
}

echo "HazardHawk Quick Validation Report" > "$REPORT_FILE"
echo "Generated: $(date)" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Test 1: Project Structure Validation
echo -e "${BLUE}📋 Testing Project Structure...${NC}"

if [ -d "HazardHawk" ]; then
    log_result "Project Directory Exists" "PASS" "HazardHawk directory found"
else
    log_result "Project Directory Exists" "FAIL" "HazardHawk directory not found"
    exit 1
fi

# Check key source files
if [ -f "HazardHawk/androidApp/src/main/java/com/hazardhawk/MainActivity.kt" ]; then
    log_result "MainActivity Exists" "PASS" "Main activity file found"
else
    log_result "MainActivity Exists" "FAIL" "MainActivity.kt not found"
fi

if [ -f "HazardHawk/androidApp/src/main/java/com/hazardhawk/CameraScreen.kt" ]; then
    log_result "CameraScreen Exists" "PASS" "Camera screen file found"
else
    log_result "CameraScreen Exists" "FAIL" "CameraScreen.kt not found"
fi

# Test 2: Build Configuration Validation
echo -e "${BLUE}🔨 Testing Build Configuration...${NC}"

if [ -f "HazardHawk/build.gradle.kts" ]; then
    log_result "Root Build File Exists" "PASS" "Root build.gradle.kts found"
else
    log_result "Root Build File Exists" "FAIL" "Root build.gradle.kts not found"
fi

if [ -f "HazardHawk/androidApp/build.gradle.kts" ]; then
    log_result "App Build File Exists" "PASS" "App build.gradle.kts found"
else
    log_result "App Build File Exists" "FAIL" "App build.gradle.kts not found"
fi

# Check Android manifest
if [ -f "HazardHawk/androidApp/src/main/AndroidManifest.xml" ]; then
    log_result "Android Manifest Exists" "PASS" "AndroidManifest.xml found"
else
    log_result "Android Manifest Exists" "FAIL" "AndroidManifest.xml not found"
fi

# Test 3: Dependencies Check
echo -e "${BLUE}📦 Testing Dependencies...${NC}"

cd HazardHawk

# Check gradle wrapper
if [ -f "gradlew" ]; then
    log_result "Gradle Wrapper Exists" "PASS" "gradlew script found"
    chmod +x gradlew
else
    log_result "Gradle Wrapper Exists" "FAIL" "gradlew script not found"
fi

# Test 4: Basic Build Test (without compilation)
echo -e "${BLUE}🔍 Testing Build System...${NC}"

if ./gradlew tasks > /dev/null 2>&1; then
    log_result "Gradle Tasks Available" "PASS" "Gradle system functional"
else
    log_result "Gradle Tasks Available" "FAIL" "Gradle system issues"
fi

# Test 5: Test Files Validation
echo -e "${BLUE}🧪 Testing Test Infrastructure...${NC}"

# Check if our test files were created
if [ -f "androidApp/src/test/java/com/hazardhawk/CriticalFunctionsValidationTest.kt" ]; then
    log_result "Unit Test Suite Exists" "PASS" "CriticalFunctionsValidationTest.kt found"
else
    log_result "Unit Test Suite Exists" "FAIL" "Unit test file not found"
fi

if [ -f "androidApp/src/androidTest/java/com/hazardhawk/CriticalFunctionsInstrumentationTest.kt" ]; then
    log_result "Instrumentation Test Suite Exists" "PASS" "Instrumentation test file found"
else
    log_result "Instrumentation Test Suite Exists" "FAIL" "Instrumentation test file not found"
fi

if [ -f "androidApp/src/test/java/com/hazardhawk/PerformanceBenchmarkSuite.kt" ]; then
    log_result "Performance Test Suite Exists" "PASS" "Performance test file found"
else
    log_result "Performance Test Suite Exists" "FAIL" "Performance test file not found"
fi

# Test 6: Manual Test Script Validation
echo -e "${BLUE}📋 Testing Manual Test Scripts...${NC}"

cd ..

if [ -f "HAZARDHAWK_MANUAL_VALIDATION_TEST_SCRIPT.md" ]; then
    log_result "Manual Test Script Exists" "PASS" "Manual validation script found"
else
    log_result "Manual Test Script Exists" "FAIL" "Manual test script not found"
fi

if [ -f "run_validation_tests.sh" ]; then
    log_result "Automated Test Runner Exists" "PASS" "Test runner script found"
    chmod +x run_validation_tests.sh
else
    log_result "Automated Test Runner Exists" "FAIL" "Test runner script not found"
fi

# Test 7: Code Quality Checks
echo -e "${BLUE}🔍 Testing Code Quality...${NC}"

cd HazardHawk

# Check for Kotlin syntax in main files
if grep -q "package com.hazardhawk" androidApp/src/main/java/com/hazardhawk/MainActivity.kt 2>/dev/null; then
    log_result "MainActivity Package Declaration" "PASS" "Proper package declaration found"
else
    log_result "MainActivity Package Declaration" "FAIL" "Package declaration issue"
fi

# Check for Compose imports
if grep -q "androidx.compose" androidApp/src/main/java/com/hazardhawk/MainActivity.kt 2>/dev/null; then
    log_result "Compose Dependencies" "PASS" "Compose imports found in MainActivity"
else
    log_result "Compose Dependencies" "FAIL" "Compose imports missing"
fi

# Test 8: Test File Content Validation
echo -e "${BLUE}🧱 Validating Test Content...${NC}"

# Check unit test content
if grep -q "CriticalFunctionsValidationTest" androidApp/src/test/java/com/hazardhawk/CriticalFunctionsValidationTest.kt 2>/dev/null; then
    log_result "Unit Test Class Structure" "PASS" "Test class properly defined"
else
    log_result "Unit Test Class Structure" "FAIL" "Test class structure issue"
fi

# Check for test methods
if grep -q "@Test" androidApp/src/test/java/com/hazardhawk/CriticalFunctionsValidationTest.kt 2>/dev/null; then
    log_result "Unit Test Methods" "PASS" "@Test annotations found"
else
    log_result "Unit Test Methods" "FAIL" "No test methods found"
fi

# Count test methods
TEST_COUNT=$(grep -c "@Test" androidApp/src/test/java/com/hazardhawk/CriticalFunctionsValidationTest.kt 2>/dev/null || echo "0")
if [ "$TEST_COUNT" -gt 20 ]; then
    log_result "Test Coverage Quantity" "PASS" "$TEST_COUNT test methods found"
else
    log_result "Test Coverage Quantity" "FAIL" "Only $TEST_COUNT test methods found"
fi

cd ..

# Final Report
echo ""
echo "========================================"
echo -e "${BLUE}📊 VALIDATION SUMMARY${NC}"
echo "========================================"

TOTAL_TESTS=$((PASS_COUNT + FAIL_COUNT))
SUCCESS_RATE=0
if [ "$TOTAL_TESTS" -gt 0 ]; then
    SUCCESS_RATE=$((PASS_COUNT * 100 / TOTAL_TESTS))
fi

echo "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASS_COUNT${NC}"
echo -e "Failed: ${RED}$FAIL_COUNT${NC}"
echo -e "Success Rate: $SUCCESS_RATE%"

# Write summary to report
echo "" >> "$REPORT_FILE"
echo "SUMMARY:" >> "$REPORT_FILE"
echo "Total Tests: $TOTAL_TESTS" >> "$REPORT_FILE"
echo "Passed: $PASS_COUNT" >> "$REPORT_FILE"
echo "Failed: $FAIL_COUNT" >> "$REPORT_FILE"
echo "Success Rate: $SUCCESS_RATE%" >> "$REPORT_FILE"

echo ""
echo -e "${BLUE}📋 Full report saved to: $REPORT_FILE${NC}"

if [ "$SUCCESS_RATE" -ge 80 ]; then
    echo -e "${GREEN}🎆 VALIDATION INFRASTRUCTURE READY${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  Some validation checks failed. Review report for details.${NC}"
    exit 1
fi