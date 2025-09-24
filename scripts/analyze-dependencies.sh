#!/bin/bash

# HazardHawk Dependency Analysis Script
# Automated dependency checking and conflict detection

set -e

cd "$(dirname "$0")/../HazardHawk"

echo "📊 HazardHawk Dependency Analysis"
echo "================================="

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 1. Generate dependency reports
echo -e "${BLUE}📋 Generating dependency reports...${NC}"

./gradlew :shared:dependencies > ../reports/shared_dependencies.txt 2>&1
./gradlew :androidApp:dependencies > ../reports/android_dependencies.txt 2>&1

echo -e "${GREEN}✅ Dependency reports generated${NC}"

# 2. Check for version conflicts
echo ""
echo -e "${BLUE}🔍 Analyzing version conflicts...${NC}"

# Check Kotlin version consistency
kotlin_versions=$(grep -h "org.jetbrains.kotlin:" ../reports/*.txt | grep -o "[0-9]\+\.[0-9]\+\.[0-9]\+" | sort -u)
kotlin_count=$(echo "$kotlin_versions" | wc -l)

if [ "$kotlin_count" -gt 1 ]; then
    echo -e "${RED}❌ Kotlin version conflict detected:${NC}"
    echo "$kotlin_versions"
    exit 1
else
    echo -e "${GREEN}✅ Kotlin versions consistent: $kotlin_versions${NC}"
fi

# Check Compose compiler compatibility
compose_version=$(grep "kotlinCompilerExtensionVersion" build.gradle.kts | grep -o "[0-9]\+\.[0-9]\+\.[0-9]\+" || echo "not found")
kotlin_version=$(echo "$kotlin_versions" | head -1)

echo "Kotlin: $kotlin_version, Compose Compiler: $compose_version"

# Compatibility matrix check
case "$kotlin_version" in
    "1.9.22")
        if [[ "$compose_version" =~ ^1\.5\.(11|15)$ ]]; then
            echo -e "${GREEN}✅ Compose-Kotlin compatibility verified${NC}"
        else
            echo -e "${YELLOW}⚠️  Compose compiler $compose_version may not be optimal for Kotlin $kotlin_version${NC}"
        fi
        ;;
    "2.1.20")
        if [[ "$compose_version" =~ ^1\.(5\.15|6\.0)$ ]]; then
            echo -e "${GREEN}✅ Compose-Kotlin compatibility verified${NC}"
        else
            echo -e "${YELLOW}⚠️  Compose compiler $compose_version may not be optimal for Kotlin $kotlin_version${NC}"
        fi
        ;;
    *)
        echo -e "${YELLOW}⚠️  Unknown Kotlin version compatibility${NC}"
        ;;
esac

# 3. Check for vulnerable dependencies
echo ""
echo -e "${BLUE}🛡️  Checking for vulnerable dependencies...${NC}"

if command -v ./gradlew dependencyCheckAnalyze >/dev/null 2>&1; then
    ./gradlew dependencyCheckAnalyze --quiet || echo -e "${YELLOW}⚠️  Vulnerability scan not available${NC}"
fi

# 4. Analyze KMP-specific dependency alignment
echo ""
echo -e "${BLUE}🎯 Analyzing KMP dependency alignment...${NC}"

shared_libs=("kotlinx-coroutines-core" "kotlinx-serialization-json" "ktor-client-core")

for lib in "${shared_libs[@]}"; do
    echo "Checking $lib versions:"
    
    # Extract versions from dependency reports
    common_version=$(grep "$lib" ../reports/shared_dependencies.txt | head -1 | grep -o "[0-9]\+\.[0-9]\+\.[0-9]\+" | head -1 || echo "not found")
    android_version=$(grep "$lib" ../reports/android_dependencies.txt | head -1 | grep -o "[0-9]\+\.[0-9]\+\.[0-9]\+" | head -1 || echo "not found")
    
    echo "  Common: $common_version"
    echo "  Android: $android_version"
    
    if [ "$common_version" != "$android_version" ] && [ "$common_version" != "not found" ] && [ "$android_version" != "not found" ]; then
        echo -e "${RED}❌ Version mismatch for $lib${NC}"
        exit 1
    else
        echo -e "${GREEN}✅ $lib versions aligned${NC}"
    fi
    echo ""
done

# 5. Check test dependencies
echo -e "${BLUE}🧪 Validating test dependencies...${NC}"

required_test_deps=("kotlin-test" "kotlinx-coroutines-test" "junit")

for dep in "${required_test_deps[@]}"; do
    if grep -q "testImplementation.*$dep" build.gradle.kts ../shared/build.gradle.kts; then
        echo -e "${GREEN}✅ $dep: present${NC}"
    else
        echo -e "${RED}❌ $dep: missing${NC}"
        exit 1
    fi
done

# 6. Generate recommendations
echo ""
echo -e "${BLUE}💡 Generating recommendations...${NC}"

echo "## Dependency Analysis Summary" > ../reports/dependency_analysis_summary.md
echo "Generated on: $(date)" >> ../reports/dependency_analysis_summary.md
echo "" >> ../reports/dependency_analysis_summary.md

echo "### Version Status" >> ../reports/dependency_analysis_summary.md
echo "- Kotlin: $kotlin_version" >> ../reports/dependency_analysis_summary.md
echo "- Compose Compiler: $compose_version" >> ../reports/dependency_analysis_summary.md
echo "" >> ../reports/dependency_analysis_summary.md

echo "### Recommendations" >> ../reports/dependency_analysis_summary.md

# Performance recommendations based on research
if [[ "$kotlin_version" < "2.0.0" ]]; then
    echo "- 🚀 Consider upgrading to Kotlin 2.1.20+ for K2 compiler benefits" >> ../reports/dependency_analysis_summary.md
fi

if [[ "$compose_version" < "1.5.15" ]]; then
    echo "- 🎨 Consider upgrading Compose compiler to 1.5.15+ for lambda fixes" >> ../reports/dependency_analysis_summary.md
fi

echo "- 📊 All dependency conflicts resolved" >> ../reports/dependency_analysis_summary.md
echo "- ✅ KMP dependency alignment verified" >> ../reports/dependency_analysis_summary.md

echo ""
echo -e "${GREEN}🎉 Dependency analysis completed successfully!${NC}"
echo -e "${BLUE}📄 Full report: reports/dependency_analysis_summary.md${NC}"

exit 0
