#!/bin/bash

# HazardHawk Production Build Script
# This script builds the production version of HazardHawk with all safety checks and monitoring

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BUILD_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_FILE="$BUILD_DIR/build-production.log"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo -e "${BLUE}🏗️  HazardHawk Production Build Script${NC}"
echo -e "${BLUE}======================================${NC}"
echo "Build directory: $BUILD_DIR"
echo "Log file: $LOG_FILE"
echo "Timestamp: $TIMESTAMP"
echo ""

# Function to log messages
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# Function to check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check Java version
    if ! java -version 2>&1 | grep -q "version.*1.8\|version.*11\|version.*17"; then
        echo -e "${RED}❌ Java 8, 11, or 17 required${NC}"
        exit 1
    fi
    
    # Check Android SDK
    if [ -z "$ANDROID_HOME" ]; then
        echo -e "${RED}❌ ANDROID_HOME environment variable not set${NC}"
        exit 1
    fi
    
    # Check if gradle wrapper exists
    if [ ! -f "$BUILD_DIR/gradlew" ]; then
        echo -e "${RED}❌ Gradle wrapper not found${NC}"
        exit 1
    fi
    
    # Check if required files exist
    local required_files=(
        "shared/src/commonMain/kotlin/com/hazardhawk/ai/ProductionConfig.kt"
        "shared/src/commonMain/kotlin/com/hazardhawk/ai/FeatureFlags.kt"
        "shared/src/commonMain/kotlin/com/hazardhawk/ai/MonitoringConfig.kt"
        "shared/src/commonMain/kotlin/com/hazardhawk/ai/CostMonitoringConfig.kt"
        "shared/src/commonMain/kotlin/com/hazardhawk/ai/RollbackProcedures.kt"
    )
    
    for file in "${required_files[@]}"; do
        if [ ! -f "$BUILD_DIR/$file" ]; then
            echo -e "${RED}❌ Required file not found: $file${NC}"
            exit 1
        fi
    done
    
    echo -e "${GREEN}✅ Prerequisites check passed${NC}"
}

# Function to run tests
run_tests() {
    log "Running tests..."
    echo -e "${YELLOW}🧪 Running unit tests${NC}"
    
    cd "$BUILD_DIR"
    
    # Run shared module tests
    ./gradlew :shared:testDebugUnitTest --stacktrace
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Shared module tests failed${NC}"
        exit 1
    fi
    
    # Run Android app tests
    ./gradlew :androidApp:testProductionStandardDebugUnitTest --stacktrace
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Android app tests failed${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ All tests passed${NC}"
}

# Function to perform security checks
security_checks() {
    log "Performing security checks..."
    echo -e "${YELLOW}🔒 Running security checks${NC}"
    
    # Check for hardcoded secrets
    echo "Checking for potential hardcoded secrets..."
    if grep -r "api_key\|secret\|password\|token" --include="*.kt" "$BUILD_DIR/shared/src" | grep -v "// TODO\|example\|placeholder"; then
        echo -e "${RED}❌ Potential hardcoded secrets found${NC}"
        exit 1
    fi
    
    # Check for debug logs in production code
    if grep -r "Log\.\|println\|System\.out" --include="*.kt" "$BUILD_DIR/shared/src/commonMain" | grep -v "TODO\|example"; then
        echo -e "${YELLOW}⚠️  Debug logging found in production code${NC}"
        echo "Please review and ensure this is intentional"
    fi
    
    echo -e "${GREEN}✅ Security checks passed${NC}"
}

# Function to build production APK
build_production_apk() {
    log "Building production APK..."
    echo -e "${YELLOW}📱 Building production APK${NC}"
    
    cd "$BUILD_DIR"
    
    # Clean previous builds
    ./gradlew clean
    
    # Build production APK
    ./gradlew assembleProductionStandardRelease --stacktrace
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Production APK build failed${NC}"
        exit 1
    fi
    
    # Verify APK was created
    APK_PATH="$BUILD_DIR/androidApp/build/outputs/apk/productionStandard/release/androidApp-productionStandard-release.apk"
    if [ ! -f "$APK_PATH" ]; then
        echo -e "${RED}❌ Production APK not found at expected location${NC}"
        exit 1
    fi
    
    # Get APK size
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo -e "${GREEN}✅ Production APK built successfully${NC}"
    echo -e "${BLUE}📦 APK size: $APK_SIZE${NC}"
    echo -e "${BLUE}📍 APK location: $APK_PATH${NC}"
}

# Function to build production AAB
build_production_aab() {
    log "Building production AAB..."
    echo -e "${YELLOW}📦 Building production AAB (App Bundle)${NC}"
    
    cd "$BUILD_DIR"
    
    # Build production AAB
    ./gradlew bundleProductionStandardRelease --stacktrace
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Production AAB build failed${NC}"
        exit 1
    fi
    
    # Verify AAB was created
    AAB_PATH="$BUILD_DIR/androidApp/build/outputs/bundle/productionStandardRelease/androidApp-productionStandard-release.aab"
    if [ ! -f "$AAB_PATH" ]; then
        echo -e "${RED}❌ Production AAB not found at expected location${NC}"
        exit 1
    fi
    
    # Get AAB size
    AAB_SIZE=$(du -h "$AAB_PATH" | cut -f1)
    echo -e "${GREEN}✅ Production AAB built successfully${NC}"
    echo -e "${BLUE}📦 AAB size: $AAB_SIZE${NC}"
    echo -e "${BLUE}📍 AAB location: $AAB_PATH${NC}"
}

# Function to validate build outputs
validate_build() {
    log "Validating build outputs..."
    echo -e "${YELLOW}🔍 Validating build outputs${NC}"
    
    APK_PATH="$BUILD_DIR/androidApp/build/outputs/apk/productionStandard/release/androidApp-productionStandard-release.apk"
    AAB_PATH="$BUILD_DIR/androidApp/build/outputs/bundle/productionStandardRelease/androidApp-productionStandard-release.aab"
    
    # Validate APK
    if command -v aapt &> /dev/null; then
        echo "Validating APK..."
        aapt dump badging "$APK_PATH" | grep -E "application-label|package|versionCode|versionName"
    fi
    
    # Check ProGuard mapping files
    MAPPING_FILE="$BUILD_DIR/androidApp/build/outputs/mapping/productionStandardRelease/mapping.txt"
    if [ -f "$MAPPING_FILE" ]; then
        echo -e "${GREEN}✅ ProGuard mapping file generated${NC}"
        echo -e "${BLUE}📍 Mapping file: $MAPPING_FILE${NC}"
    else
        echo -e "${YELLOW}⚠️  ProGuard mapping file not found${NC}"
    fi
    
    echo -e "${GREEN}✅ Build validation completed${NC}"
}

# Function to generate build report
generate_build_report() {
    log "Generating build report..."
    echo -e "${YELLOW}📄 Generating build report${NC}"
    
    REPORT_FILE="$BUILD_DIR/build-report-$TIMESTAMP.txt"
    
    cat > "$REPORT_FILE" << EOF
HazardHawk Production Build Report
==================================
Build Timestamp: $TIMESTAMP
Build Directory: $BUILD_DIR
Gradle Version: $(cd "$BUILD_DIR" && ./gradlew --version | grep "Gradle" | head -1)
Java Version: $(java -version 2>&1 | head -1)

Build Configuration:
- Environment: Production
- Variant: productionStandardRelease
- Minification: Enabled
- Resource Shrinking: Enabled
- Monitoring: Enabled
- Cost Tracking: Enabled
- Feature Flags: Enabled

Output Files:
- APK: $(ls -lh "$BUILD_DIR/androidApp/build/outputs/apk/productionStandard/release/"*.apk 2>/dev/null || echo "Not found")
- AAB: $(ls -lh "$BUILD_DIR/androidApp/build/outputs/bundle/productionStandardRelease/"*.aab 2>/dev/null || echo "Not found")
- Mapping: $(ls -lh "$BUILD_DIR/androidApp/build/outputs/mapping/productionStandardRelease/mapping.txt" 2>/dev/null || echo "Not found")

Production Configuration Validation:
- ProductionConfig.kt: $([ -f "$BUILD_DIR/shared/src/commonMain/kotlin/com/hazardhawk/ai/ProductionConfig.kt" ] && echo "✅ Present" || echo "❌ Missing")
- FeatureFlags.kt: $([ -f "$BUILD_DIR/shared/src/commonMain/kotlin/com/hazardhawk/ai/FeatureFlags.kt" ] && echo "✅ Present" || echo "❌ Missing")
- MonitoringConfig.kt: $([ -f "$BUILD_DIR/shared/src/commonMain/kotlin/com/hazardhawk/ai/MonitoringConfig.kt" ] && echo "✅ Present" || echo "❌ Missing")
- CostMonitoringConfig.kt: $([ -f "$BUILD_DIR/shared/src/commonMain/kotlin/com/hazardhawk/ai/CostMonitoringConfig.kt" ] && echo "✅ Present" || echo "❌ Missing")
- RollbackProcedures.kt: $([ -f "$BUILD_DIR/shared/src/commonMain/kotlin/com/hazardhawk/ai/RollbackProcedures.kt" ] && echo "✅ Present" || echo "❌ Missing")

Build Steps Completed:
✅ Prerequisites check
✅ Security validation
✅ Unit tests
✅ Production APK build
✅ Production AAB build
✅ Build validation
✅ Build report generation

Next Steps:
1. Upload AAB to Google Play Console
2. Configure production API keys securely
3. Set up monitoring dashboards
4. Configure feature flag initial states
5. Test deployment in staging environment first
6. Monitor production metrics closely after release

EOF
    
    echo -e "${GREEN}✅ Build report generated${NC}"
    echo -e "${BLUE}📍 Report location: $REPORT_FILE${NC}"
}

# Function to show deployment instructions
show_deployment_instructions() {
    echo ""
    echo -e "${BLUE}🚀 DEPLOYMENT INSTRUCTIONS${NC}"
    echo -e "${BLUE}============================${NC}"
    echo ""
    echo "1. 📋 Review the build report for any issues"
    echo "2. 🔐 Configure production API keys using secure key management"
    echo "3. 📊 Set up monitoring dashboards and alerts"
    echo "4. 🎛️  Configure initial feature flag states (conservative settings)"
    echo "5. 🧪 Test in staging environment with production build"
    echo "6. 📱 Upload AAB to Google Play Console (internal track first)"
    echo "7. 👀 Monitor metrics closely during rollout"
    echo "8. 📈 Gradually increase feature flag percentages based on health metrics"
    echo ""
    echo -e "${YELLOW}⚠️  IMPORTANT REMINDERS:${NC}"
    echo "- Start with Gemini integration DISABLED (0% rollout)"
    echo "- Use local analysis by default initially"
    echo "- Set up cost budgets and alerts before enabling cloud features"
    echo "- Have rollback procedures ready and tested"
    echo "- Monitor user feedback and crash reports closely"
    echo ""
    echo -e "${GREEN}📖 For detailed instructions, see: DeploymentGuide.md${NC}"
}

# Main execution
main() {
    log "Starting HazardHawk production build..."
    
    # Run build steps
    check_prerequisites
    security_checks
    run_tests
    build_production_apk
    build_production_aab
    validate_build
    generate_build_report
    
    echo ""
    echo -e "${GREEN}🎉 PRODUCTION BUILD COMPLETED SUCCESSFULLY!${NC}"
    echo -e "${GREEN}=============================================${NC}"
    
    show_deployment_instructions
    
    log "Production build completed successfully"
}

# Run main function
main "$@"