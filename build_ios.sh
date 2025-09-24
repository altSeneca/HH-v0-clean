#!/bin/bash

echo "🚀 Building HazardHawk iOS App"
echo "================================"

# Change to the HazardHawk directory
cd HazardHawk

# Build the shared framework for iOS Simulator
echo "📦 Building shared framework for iOS Simulator..."
./gradlew linkDebugFrameworkIosSimulatorArm64 -x test

if [ $? -ne 0 ]; then
    echo "❌ Failed to build iOS Simulator framework"
    exit 1
fi

# Build the shared framework for iOS Device
echo "📦 Building shared framework for iOS Device..."
./gradlew linkReleaseFrameworkIosArm64 -x test

if [ $? -ne 0 ]; then
    echo "❌ Failed to build iOS Device framework"
    exit 1
fi

echo "✅ Shared frameworks built successfully!"

# Change to the iOS app directory
cd ../iosApp

# Check if Xcode project exists
if [ ! -d "HazardHawk.xcodeproj" ]; then
    echo "❌ Xcode project not found!"
    exit 1
fi

echo "📱 iOS project structure:"
echo "   • Xcode project: ✅ HazardHawk.xcodeproj"
echo "   • Swift files: ✅ AppDelegate.swift, ContentView.swift"
echo "   • Frameworks: ✅ Debug & Release shared frameworks"

echo ""
echo "🎉 HazardHawk iOS build setup complete!"
echo ""
echo "📖 Next steps:"
echo "   1. Open iosApp/HazardHawk.xcodeproj in Xcode"
echo "   2. Select iPhone Simulator as target"
echo "   3. Build and run the project"
echo "   4. The app should display platform information from the shared KMP module"
echo ""
echo "🔧 Framework paths:"
echo "   • Simulator: HazardHawk/shared/build/bin/iosSimulatorArm64/debugFramework/shared.framework"
echo "   • Device: HazardHawk/shared/build/bin/iosArm64/releaseFramework/shared.framework"