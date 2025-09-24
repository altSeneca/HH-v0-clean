#!/bin/bash

# Quick State Validation Test Runner
# For rapid development feedback - runs only critical safety tests

set -e

echo "⚡ HazardHawk Quick State Validation"
echo "===================================="

PROJECT_DIR="/Users/aaron/Apps-Coded/HH-v0"  
ANDROID_PROJECT_DIR="$PROJECT_DIR/HazardHawk/androidApp"

cd "$ANDROID_PROJECT_DIR"

echo "🚨 Running CRITICAL SAFETY tests only..."
echo ""

# Test the most important safety validation
echo "🔍 Test 1: AI Analysis defaults to FALSE on fresh install"
if ./gradlew test --tests "com.hazardhawk.state.AIAnalysisDefaultValidationTest.CRITICAL - ai analysis defaults to false on fresh install" 2>/dev/null; then
    echo "✅ PASSED: AI Analysis safe default validated"
else
    echo "❌ FAILED: AI Analysis does NOT default to safe FALSE value!"
    echo "🚨 CRITICAL SAFETY ISSUE - Must be fixed before deployment"
    exit 1
fi

echo ""
echo "🔍 Test 2: AI Analysis setting persistence across restarts"
if ./gradlew test --tests "com.hazardhawk.state.AIAnalysisDefaultValidationTest.ai analysis setting persists across app restarts" 2>/dev/null; then
    echo "✅ PASSED: AI Analysis persistence validated" 
else
    echo "❌ FAILED: AI Analysis settings do not persist correctly!"
    exit 1
fi

echo ""
echo "🔍 Test 3: Camera aspect ratio persistence"  
if ./gradlew test --tests "com.hazardhawk.state.CameraStatePersistenceTest.camera aspect ratio survives app kill and restart" 2>/dev/null; then
    echo "✅ PASSED: Camera state persistence validated"
else
    echo "❌ FAILED: Camera settings do not persist correctly!"
    exit 1
fi

echo ""
echo "🔍 Test 4: Project name persistence"
if ./gradlew test --tests "com.hazardhawk.state.ProjectManagementPersistenceTest.user entered project names persist across app restarts" 2>/dev/null; then
    echo "✅ PASSED: Project persistence validated"
else
    echo "❌ FAILED: Project data does not persist correctly!"
    exit 1
fi

echo ""
echo "🎉 QUICK VALIDATION COMPLETE!"
echo "============================"
echo "✅ All critical safety tests PASSED"
echo "✅ Core state persistence validated"
echo "✅ Safe for continued development"
echo ""
echo "💡 Run './run_state_persistence_tests.sh' for comprehensive testing"
