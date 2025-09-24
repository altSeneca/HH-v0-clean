#!/bin/bash

# HazardHawk State Persistence Testing Pipeline
# Comprehensive testing framework for state management fixes
# CRITICAL: Validates AI Analysis safe default and persistence reliability

set -e

echo "🔧 HazardHawk State Persistence Testing Pipeline"
echo "================================================="

# Configuration
PROJECT_DIR="/Users/aaron/Apps-Coded/HH-v0"
ANDROID_PROJECT_DIR="$PROJECT_DIR/HazardHawk/androidApp"
TEST_RESULTS_DIR="$PROJECT_DIR/test-results/state-persistence"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
CRITICAL_TESTS_LOG="$TEST_RESULTS_DIR/critical_safety_tests_$TIMESTAMP.log"
FULL_TESTS_LOG="$TEST_RESULTS_DIR/full_test_suite_$TIMESTAMP.log"

# Create test results directory
mkdir -p "$TEST_RESULTS_DIR"

echo "📁 Test results will be saved to: $TEST_RESULTS_DIR"
echo "📅 Test run timestamp: $TIMESTAMP"
echo ""

cd "$ANDROID_PROJECT_DIR"

# Function to run tests with proper reporting
run_test_suite() {
    local suite_name="$1"
    local log_file="$2"
    local description="$3"
    
    echo "🧪 Running $description..."
    echo "📝 Logging to: $log_file"
    
    {
        echo "========================================="
        echo "Test Suite: $suite_name"
        echo "Description: $description"
        echo "Started at: $(date)"
        echo "========================================="
        echo ""
        
        if ./gradlew test --tests "$suite_name" 2>&1; then
            echo ""
            echo "✅ $description - PASSED"
            echo "Completed at: $(date)"
            return 0
        else
            echo ""
            echo "❌ $description - FAILED"
            echo "Completed at: $(date)"
            return 1
        fi
        
    } | tee -a "$log_file"
}

# Function to run individual test method
run_individual_test() {
    local test_class="$1"
    local test_method="$2" 
    local description="$3"
    local log_file="$4"
    
    echo "🔍 Running individual test: $description"
    
    {
        echo "-----------------------------------------"
        echo "Individual Test: $test_class.$test_method"
        echo "Description: $description"
        echo "Started at: $(date)"
        echo "-----------------------------------------"
        
        if ./gradlew test --tests "$test_class.$test_method" 2>&1; then
            echo "✅ $description - PASSED"
            return 0
        else
            echo "❌ $description - FAILED"
            return 1
        fi
        
    } | tee -a "$log_file"
}

# Phase 1: CRITICAL SAFETY TESTS (Must pass for deployment)
echo "🚨 PHASE 1: CRITICAL SAFETY TESTS"
echo "=================================="
echo "These tests MUST pass for deployment. They validate the"
echo "CRITICAL SAFETY ISSUE where AI Analysis defaults to FALSE."
echo ""

CRITICAL_TESTS_PASSED=0
CRITICAL_TESTS_FAILED=0

# Test 1: AI Analysis defaults to FALSE on fresh install
if run_individual_test \
    "com.hazardhawk.state.AIAnalysisDefaultValidationTest" \
    "CRITICAL - ai analysis defaults to false on fresh install" \
    "AI Analysis Safe Default Validation" \
    "$CRITICAL_TESTS_LOG"; then
    CRITICAL_TESTS_PASSED=$((CRITICAL_TESTS_PASSED + 1))
else
    CRITICAL_TESTS_FAILED=$((CRITICAL_TESTS_FAILED + 1))
    echo "🚨 CRITICAL FAILURE: AI Analysis does not default to safe FALSE value!"
fi

# Test 2: AI Analysis setting persistence
if run_individual_test \
    "com.hazardhawk.state.AIAnalysisDefaultValidationTest" \
    "ai analysis setting persists across app restarts" \
    "AI Analysis Persistence Validation" \
    "$CRITICAL_TESTS_LOG"; then
    CRITICAL_TESTS_PASSED=$((CRITICAL_TESTS_PASSED + 1))
else
    CRITICAL_TESTS_FAILED=$((CRITICAL_TESTS_FAILED + 1))
fi

# Test 3: Regression prevention 
if run_individual_test \
    "com.hazardhawk.state.AIAnalysisDefaultValidationTest" \
    "prevent regression to unsafe true default" \
    "AI Analysis Regression Prevention" \
    "$CRITICAL_TESTS_LOG"; then
    CRITICAL_TESTS_PASSED=$((CRITICAL_TESTS_PASSED + 1))
else
    CRITICAL_TESTS_FAILED=$((CRITICAL_TESTS_FAILED + 1))
fi

# Test 4: Migration from unsafe default
if run_individual_test \
    "com.hazardhawk.state.AIAnalysisDefaultValidationTest" \
    "migration from old unsafe default to new safe default" \
    "AI Analysis Safe Migration" \
    "$CRITICAL_TESTS_LOG"; then
    CRITICAL_TESTS_PASSED=$((CRITICAL_TESTS_PASSED + 1))
else
    CRITICAL_TESTS_FAILED=$((CRITICAL_TESTS_FAILED + 1))
fi

echo ""
echo "📊 CRITICAL SAFETY TESTS SUMMARY:"
echo "   ✅ Passed: $CRITICAL_TESTS_PASSED"
echo "   ❌ Failed: $CRITICAL_TESTS_FAILED"

if [ $CRITICAL_TESTS_FAILED -gt 0 ]; then
    echo ""
    echo "🚨 DEPLOYMENT BLOCKED: Critical safety tests failed!"
    echo "   AI Analysis default safety is NOT validated."
    echo "   Review logs: $CRITICAL_TESTS_LOG"
    echo ""
    echo "🔧 Required fixes:"
    echo "   1. Ensure MetadataSettingsManager defaults aiAnalysisEnabled to FALSE"
    echo "   2. Verify persistence correctly saves and loads the FALSE default"
    echo "   3. Test migration properly converts old TRUE defaults to FALSE"
    echo "   4. Confirm no code paths accidentally set AI to TRUE by default"
    exit 1
fi

echo "✅ All critical safety tests PASSED! Deployment safety validated."
echo ""

# Phase 2: HIGH PRIORITY TESTS
echo "🔄 PHASE 2: HIGH PRIORITY TESTS"
echo "================================"
echo "Core functionality tests for camera and project state persistence."
echo ""

HIGH_PRIORITY_PASSED=0
HIGH_PRIORITY_FAILED=0

# Camera State Persistence Tests
if run_test_suite \
    "com.hazardhawk.state.CameraStatePersistenceTest" \
    "$FULL_TESTS_LOG" \
    "Camera State Persistence Test Suite"; then
    HIGH_PRIORITY_PASSED=$((HIGH_PRIORITY_PASSED + 1))
else
    HIGH_PRIORITY_FAILED=$((HIGH_PRIORITY_FAILED + 1))
fi

# Project Management Persistence Tests  
if run_test_suite \
    "com.hazardhawk.state.ProjectManagementPersistenceTest" \
    "$FULL_TESTS_LOG" \
    "Project Management Persistence Test Suite"; then
    HIGH_PRIORITY_PASSED=$((HIGH_PRIORITY_PASSED + 1))
else
    HIGH_PRIORITY_FAILED=$((HIGH_PRIORITY_FAILED + 1))
fi

echo ""
echo "📊 HIGH PRIORITY TESTS SUMMARY:"
echo "   ✅ Passed: $HIGH_PRIORITY_PASSED"
echo "   ❌ Failed: $HIGH_PRIORITY_FAILED"

# Phase 3: MEDIUM PRIORITY TESTS  
echo ""
echo "⚡ PHASE 3: MEDIUM PRIORITY TESTS"
echo "================================="
echo "Integration and performance validation tests."
echo ""

MEDIUM_PRIORITY_PASSED=0
MEDIUM_PRIORITY_FAILED=0

# State Synchronization Integration Tests
if run_test_suite \
    "com.hazardhawk.state.StateSynchronizationIntegrationTest" \
    "$FULL_TESTS_LOG" \
    "State Synchronization Integration Test Suite"; then
    MEDIUM_PRIORITY_PASSED=$((MEDIUM_PRIORITY_PASSED + 1))
else
    MEDIUM_PRIORITY_FAILED=$((MEDIUM_PRIORITY_FAILED + 1))
fi

# Performance and Stress Tests
if run_test_suite \
    "com.hazardhawk.state.PerformanceStressTestFramework" \
    "$FULL_TESTS_LOG" \
    "Performance and Stress Test Framework"; then
    MEDIUM_PRIORITY_PASSED=$((MEDIUM_PRIORITY_PASSED + 1))
else
    MEDIUM_PRIORITY_FAILED=$((MEDIUM_PRIORITY_FAILED + 1))
fi

echo ""
echo "📊 MEDIUM PRIORITY TESTS SUMMARY:"
echo "   ✅ Passed: $MEDIUM_PRIORITY_PASSED"  
echo "   ❌ Failed: $MEDIUM_PRIORITY_FAILED"

# Phase 4: COMPREHENSIVE COVERAGE TESTS
echo ""
echo "🛡️ PHASE 4: COMPREHENSIVE COVERAGE TESTS"
echo "========================================="
echo "Edge cases, error recovery, and end-to-end user scenarios."
echo ""

COMPREHENSIVE_PASSED=0
COMPREHENSIVE_FAILED=0

# Edge Case and Error Recovery Tests
if run_test_suite \
    "com.hazardhawk.state.EdgeCaseErrorRecoveryTest" \
    "$FULL_TESTS_LOG" \
    "Edge Case and Error Recovery Test Suite"; then
    COMPREHENSIVE_PASSED=$((COMPREHENSIVE_PASSED + 1))
else
    COMPREHENSIVE_FAILED=$((COMPREHENSIVE_FAILED + 1))
fi

# End-to-End User Scenario Tests
if run_test_suite \
    "com.hazardhawk.state.EndToEndUserScenarioTest" \
    "$FULL_TESTS_LOG" \
    "End-to-End User Scenario Test Suite"; then
    COMPREHENSIVE_PASSED=$((COMPREHENSIVE_PASSED + 1))
else
    COMPREHENSIVE_FAILED=$((COMPREHENSIVE_FAILED + 1))
fi

echo ""
echo "📊 COMPREHENSIVE TESTS SUMMARY:"
echo "   ✅ Passed: $COMPREHENSIVE_PASSED"
echo "   ❌ Failed: $COMPREHENSIVE_FAILED"

# Final Results Summary
echo ""
echo "🎯 FINAL TEST EXECUTION SUMMARY"
echo "================================="
echo "Timestamp: $TIMESTAMP"
echo ""
echo "🚨 CRITICAL SAFETY TESTS:     ✅ $CRITICAL_TESTS_PASSED   ❌ $CRITICAL_TESTS_FAILED"
echo "🔄 HIGH PRIORITY TESTS:       ✅ $HIGH_PRIORITY_PASSED   ❌ $HIGH_PRIORITY_FAILED" 
echo "⚡ MEDIUM PRIORITY TESTS:      ✅ $MEDIUM_PRIORITY_PASSED   ❌ $MEDIUM_PRIORITY_FAILED"
echo "🛡️ COMPREHENSIVE TESTS:       ✅ $COMPREHENSIVE_PASSED   ❌ $COMPREHENSIVE_FAILED"
echo ""

TOTAL_PASSED=$((CRITICAL_TESTS_PASSED + HIGH_PRIORITY_PASSED + MEDIUM_PRIORITY_PASSED + COMPREHENSIVE_PASSED))
TOTAL_FAILED=$((CRITICAL_TESTS_FAILED + HIGH_PRIORITY_FAILED + MEDIUM_PRIORITY_FAILED + COMPREHENSIVE_FAILED))
TOTAL_TESTS=$((TOTAL_PASSED + TOTAL_FAILED))

echo "📈 OVERALL SUMMARY:"
echo "   🧪 Total Tests: $TOTAL_TESTS"
echo "   ✅ Total Passed: $TOTAL_PASSED"  
echo "   ❌ Total Failed: $TOTAL_FAILED"
echo "   📊 Success Rate: $(( (TOTAL_PASSED * 100) / TOTAL_TESTS ))%"
echo ""
echo "📝 Test logs saved to:"
echo "   🚨 Critical Tests: $CRITICAL_TESTS_LOG"
echo "   📋 Full Test Suite: $FULL_TESTS_LOG"
echo ""

# Deployment recommendation
if [ $CRITICAL_TESTS_FAILED -eq 0 ]; then
    if [ $HIGH_PRIORITY_FAILED -eq 0 ]; then
        echo "🚀 DEPLOYMENT RECOMMENDATION: APPROVED"
        echo "   ✅ All critical safety tests passed"
        echo "   ✅ All high priority functionality tests passed"
        echo "   📝 Review medium/comprehensive test failures if any"
        exit 0
    else
        echo "⚠️ DEPLOYMENT RECOMMENDATION: REVIEW REQUIRED"
        echo "   ✅ Critical safety tests passed"
        echo "   ❌ High priority functionality tests failed"
        echo "   📝 Address core functionality issues before deployment"
        exit 2
    fi
else
    echo "🚨 DEPLOYMENT RECOMMENDATION: BLOCKED"
    echo "   ❌ Critical safety tests failed - DEPLOYMENT UNSAFE"
    echo "   🔧 Must fix AI Analysis default safety issues"
    exit 1
fi
