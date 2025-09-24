#!/bin/bash

# HazardHawk Navigation Flow Integration Test Runner
# Validates the complete user workflow that was previously crashing

echo "🔧 HazardHawk Navigation Flow Test Suite"
echo "========================================="

cd "$(dirname "$0")"

echo "📱 Checking connected Android devices..."
ADB_DEVICES=$(adb devices -l | grep -E "(device|emulator)" | wc -l)

if [ "$ADB_DEVICES" -eq 0 ]; then
    echo "❌ No Android devices connected. Please connect a device or start an emulator."
    exit 1
fi

echo "✅ Found $ADB_DEVICES device(s) connected"

echo ""
echo "🧪 Building test APK..."
./gradlew :androidApp:assembleDebugAndroidTest

if [ $? -ne 0 ]; then
    echo "❌ Failed to build test APK"
    exit 1
fi

echo ""
echo "📦 Installing main app APK..."
./gradlew :androidApp:installDebug

if [ $? -ne 0 ]; then
    echo "❌ Failed to install main app"
    exit 1
fi

echo ""
echo "🧪 Installing test APK..."
./gradlew :androidApp:installDebugAndroidTest

if [ $? -ne 0 ]; then
    echo "❌ Failed to install test APK"
    exit 1
fi

echo ""
echo "🚀 Running Navigation Flow Integration Tests..."
echo "=============================================="

# Run the critical navigation flow test first
echo ""
echo "1️⃣ Testing Form Submission Navigation Flow (CRITICAL)"
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testFormSubmissionNavigatesToCamera \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

if [ $? -eq 0 ]; then
    echo "✅ CRITICAL TEST PASSED: Form submission navigation works!"
else
    echo "❌ CRITICAL TEST FAILED: Form submission navigation still broken"
    echo "🔍 Check logcat for crash details:"
    echo "   adb logcat | grep -E '(hazardhawk|HazardHawk|FATAL|crash|exception|ERROR)'"
fi

echo ""
echo "2️⃣ Testing Form Validation"
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testFormValidationPreventsEmptySubmission \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

echo ""
echo "3️⃣ Testing Data Class Instantiation (CRASH PREVENTION)"
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testDataClassInstantiationDuringNavigation \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

echo ""
echo "4️⃣ Testing Navigation Data Persistence"
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testNavigationPreservesFormData \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

echo ""
echo "5️⃣ Testing Camera Permission Handling"
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testCameraPermissionHandling \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

echo ""
echo "6️⃣ Testing Back Navigation"
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testBackNavigationHandling \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

echo ""
echo "7️⃣ Testing Gallery Navigation"
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testCameraToGalleryNavigation \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

echo ""
echo "8️⃣ Testing Settings Navigation"
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testCameraToSettingsNavigation \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

echo ""
echo "9️⃣ Running Stress Tests..."
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testRapidFormSubmissions \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testLargeFormInputs \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest#testSpecialCharacterInputs \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

echo ""
echo "🏁 Navigation Flow Test Suite Complete!"
echo "========================================"

# Run all tests together for summary
echo ""
echo "🔍 Running complete test suite for summary..."
adb shell am instrument -w -e class com.hazardhawk.NavigationFlowTest \
    com.hazardhawk.test/androidx.test.runner.AndroidJUnitRunner

echo ""
echo "📊 Test Results Summary:"
echo "========================"
echo "✅ If all tests passed, the navigation crash issue is RESOLVED"
echo "❌ If tests failed, check logcat output for specific error details"
echo ""
echo "🔧 To monitor real-time crashes:"
echo "   adb logcat | grep -E '(hazardhawk|HazardHawk|FATAL|crash|exception|ERROR)'"
echo ""
echo "🚀 To test manually:"
echo "   1. Launch app: adb shell am start -n com.hazardhawk/.MainActivity"
echo "   2. Enter company and project names"
echo "   3. Tap 'Start Safety Documentation'"
echo "   4. Verify camera screen appears without crash"

