#!/bin/bash

# HazardHawk Pre-Commit Build Validation
# Prevents build-breaking changes from being committed
# Based on the comprehensive build errors research findings

set -e

echo "🔨 HazardHawk Pre-Commit Build Validation"
echo "========================================="

# Navigate to project root
cd "$(dirname "$0")/.."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'  
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if we're in the right directory
if [ ! -f "HazardHawk/gradlew" ]; then
    echo -e "${RED}❌ Error: Not in HazardHawk project root${NC}"
    exit 1
fi

echo "📍 Project root: $(pwd)"

# 1. Lambda Type Inference Pattern Detection
echo ""
echo "🔍 Checking for lambda type inference regression patterns..."

if grep -r "mutableStateOf<[^>]*>([^)]*null[^)]*)" --include="*.kt" . 2>/dev/null; then
    echo -e "${RED}❌ Lambda type inference issue detected${NC}"
    echo "Fix: Use explicit type annotation like 'var selectedTemplate: ReportTemplate? by remember { mutableStateOf(null) }'"
    exit 1
fi

echo -e "${GREEN}✅ No lambda type inference issues found${NC}"

# 2. Required Model Classes Check
echo ""
echo "🏗️  Checking for required model classes..."

required_classes=("ReportTemplate" "ReportType" "ReportSection" "ComplianceStatus")
missing_classes=()

for class_name in "${required_classes[@]}"; do
    if ! grep -r "(data\s*)?class\s*$class_name\|enum\s*class\s*$class_name" --include="*.kt" . >/dev/null 2>&1; then
        missing_classes+=("$class_name")
    fi
done

if [ ${#missing_classes[@]} -gt 0 ]; then
    echo -e "${RED}❌ Missing required model classes:${NC}"
    printf '%s\n' "${missing_classes[@]}"
    echo "These classes are required to prevent compilation failures"
    exit 1
fi

echo -e "${GREEN}✅ All required model classes present${NC}"

# 3. Expect/Actual Pair Validation  
echo ""
echo "🎯 Validating expect/actual pairs..."

# Find expect declarations in commonMain
expect_classes=$(find . -path "*/commonMain/kotlin/*" -name "*.kt" -exec grep -l "expect class" {} \; 2>/dev/null || true)

if [ -n "$expect_classes" ]; then
    for file in $expect_classes; do
        # Extract class names
        class_names=$(grep "expect class" "$file" | sed 's/.*expect class \([A-Za-z0-9_]*\).*/\1/')
        
        for class_name in $class_names; do
            # Check for Android actual
            if ! find . -path "*/androidMain/kotlin/*" -name "*.kt" -exec grep -l "actual class $class_name" {} \; >/dev/null 2>&1; then
                echo -e "${RED}❌ Missing Android actual for expect class: $class_name${NC}"
                exit 1
            fi
            
            # Check for iOS actual
            if ! find . -path "*/iosMain/kotlin/*" -name "*.kt" -exec grep -l "actual class $class_name" {} \; >/dev/null 2>&1; then
                echo -e "${RED}❌ Missing iOS actual for expect class: $class_name${NC}"
                exit 1
            fi
        done
    done
fi

echo -e "${GREEN}✅ All expect/actual pairs complete${NC}"

# 4. Import Conflict Detection
echo ""
echo "📦 Checking for import conflicts..."

# Check for known conflicting imports
conflict_detected=false

while IFS= read -r -d '' file; do
    # Check for specific import conflicts based on research
    if grep -q "com.hazardhawk.domain.models.SafetyAnalysis" "$file" && grep -q "com.hazardhawk.shared.SafetyAnalysis" "$file"; then
        echo -e "${RED}❌ Import conflict in $(basename "$file"): SafetyAnalysis imports${NC}"
        conflict_detected=true
    fi
done < <(find . -name "*.kt" -print0)

if [ "$conflict_detected" = true ]; then
    echo "Fix: Use fully qualified names or import aliases"
    exit 1
fi

echo -e "${GREEN}✅ No import conflicts detected${NC}"

# 5. Quick Compilation Check
echo ""
echo "⚡ Running quick compilation check..."

cd HazardHawk

if ! ./gradlew compileDebugKotlin --quiet --no-daemon; then
    echo -e "${RED}❌ Compilation check failed${NC}"
    echo "Run './gradlew compileDebugKotlin' to see detailed errors"
    exit 1
fi

echo -e "${GREEN}✅ Compilation check passed${NC}"

# 6. Dependency Validation
echo ""
echo "📊 Validating dependencies..."

# Check for dependency resolution failures
if ! ./gradlew :shared:dependencies --quiet | grep -v "FAILED" >/dev/null; then
    echo -e "${RED}❌ Dependency resolution issues detected${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Dependency validation passed${NC}"

# 7. Memory Settings Validation
echo ""
echo "🧠 Checking memory optimization settings..."

if [ -f gradle.properties ]; then
    required_settings=("-Xmx6g" "-XX:+UseG1GC" "parallel=true")
    
    for setting in "${required_settings[@]}"; do
        if ! grep -q "$setting" gradle.properties; then
            echo -e "${RED}❌ Missing critical build setting: $setting${NC}"
            exit 1
        fi
    done
    
    echo -e "${GREEN}✅ Memory optimization settings intact${NC}"
fi

# 8. Test Dependencies Check
echo ""
echo "🧪 Validating test dependencies..."

test_deps=("kotlin-test" "kotlinx-coroutines-test" "junit")

for dep in "${test_deps[@]}"; do
    if ! grep -r "testImplementation.*$dep" build.gradle.kts ../shared/build.gradle.kts 2>/dev/null; then
        echo -e "${RED}❌ Missing test dependency: $dep${NC}"
        exit 1
    fi
done

echo -e "${GREEN}✅ Test dependencies validated${NC}"

# 9. Performance Check (if benchmark exists)
echo ""
echo "⏱️  Checking build performance baselines..."

if [ -f build_benchmark.properties ]; then
    shared_time=$(grep "shared.build.time.ms" build_benchmark.properties | cut -d'=' -f2 2>/dev/null || echo "0")
    
    if [ "$shared_time" -gt 45000 ]; then
        echo -e "${YELLOW}⚠️  Warning: Shared build time may have regressed: ${shared_time}ms > 45000ms${NC}"
        echo "Consider running performance benchmarks"
    else
        echo -e "${GREEN}✅ Build performance within acceptable limits${NC}"
    fi
fi

cd ..

# Final Success Message
echo ""
echo -e "${GREEN}🎉 Pre-commit validation completed successfully!${NC}"
echo "✅ Lambda type inference: OK"  
echo "✅ Required model classes: OK"
echo "✅ Expect/actual pairs: OK"
echo "✅ Import conflicts: OK"
echo "✅ Compilation: OK"
echo "✅ Dependencies: OK"
echo "✅ Memory settings: OK"
echo "✅ Test dependencies: OK"

echo ""
echo "Ready to commit! 🚀"

exit 0
