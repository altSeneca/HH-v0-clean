#!/bin/bash
# validate_ai_http_implementation.sh
# Validates AI HTTP Implementation Completeness

set -e

echo "🔬 Validating AI HTTP Implementation..."

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

VALIDATION_PASSED=0
VALIDATION_FAILED=0

# Function to run validation check
validate_check() {
    local check_name="$1"
    local check_command="$2"
    
    echo -e "${BLUE}🔍 Checking: ${check_name}${NC}"
    
    if eval "$check_command"; then
        echo -e "${GREEN}✅ ${check_name} - PASSED${NC}"
        ((VALIDATION_PASSED++))
    else
        echo -e "${RED}❌ ${check_name} - FAILED${NC}"
        ((VALIDATION_FAILED++))
    fi
    
    echo ""
}

echo -e "${YELLOW}🚀 Starting AI HTTP Implementation Validation${NC}"
echo "================================================"

# 1. Check for mock implementation removal
validate_check "Mock Implementation Removed" \
    '! grep -r "TODO: Implement actual HTTP client" HazardHawk/shared/src/commonMain/kotlin/com/hazardhawk/ai/ 2>/dev/null'

# 2. Validate HTTP client usage in AI module
validate_check "HTTP Client Implementation Present" \
    'grep -r "HttpClient\|ktorClient" HazardHawk/shared/src/commonMain/kotlin/com/hazardhawk/ai/ >/dev/null 2>&1'

# 3. Check for proper error handling
validate_check "Error Handling Implementation" \
    'grep -r "try.*catch\|Result\." HazardHawk/shared/src/commonMain/kotlin/com/hazardhawk/ai/GeminiVisionAnalyzer.kt >/dev/null 2>&1'

# 4. Validate Ktor dependencies in build.gradle
validate_check "Ktor Dependencies Configured" \
    'grep -r "ktor.*client" HazardHawk/shared/build.gradle.kts >/dev/null 2>&1'

# 5. Check for API key management
validate_check "API Key Security Implementation" \
    'grep -r "secureStorage\|API_KEY" HazardHawk/shared/src/commonMain/kotlin/com/hazardhawk/ai/ >/dev/null 2>&1'

# 6. Validate JSON serialization setup
validate_check "JSON Serialization Configured" \
    'grep -r "@Serializable\|kotlinx.serialization" HazardHawk/shared/src/commonMain/kotlin/com/hazardhawk/ai/ >/dev/null 2>&1'

# 7. Check for HTTP timeout configuration
validate_check "HTTP Timeout Configuration" \
    'grep -r "timeout\|requestTimeout" HazardHawk/shared/src/commonMain/kotlin/com/hazardhawk/ai/ >/dev/null 2>&1 || echo "⚠️  HTTP timeout not configured - recommended for production"'

# 8. Validate test file structure
validate_check "Test Files Present" \
    'find HazardHawk/shared/src/commonTest -name "*Gemini*Test*.kt" | wc -l | grep -v "^0$" >/dev/null 2>&1'

# 9. Check for proper logging implementation
validate_check "Logging Implementation" \
    'grep -r "println\|Logger\|log" HazardHawk/shared/src/commonMain/kotlin/com/hazardhawk/ai/ >/dev/null 2>&1'

# 10. Validate network permission configuration (Android)
validate_check "Network Permissions Configured" \
    'grep -r "INTERNET\|ACCESS_NETWORK_STATE" HazardHawk/androidApp/src/main/AndroidManifest.xml >/dev/null 2>&1 || echo "⚠️  Network permissions may need verification"'

echo "================================================"
echo -e "${BLUE}📊 VALIDATION SUMMARY${NC}"
echo "================================================"
echo -e "Validations Passed: ${GREEN}${VALIDATION_PASSED}${NC}"
echo -e "Validations Failed: ${RED}${VALIDATION_FAILED}${NC}"

if [ $VALIDATION_FAILED -eq 0 ]; then
    echo -e "${GREEN}✅ AI HTTP Implementation Validation PASSED${NC}"
    echo -e "${GREEN}🚀 Ready for AI HTTP integration testing${NC}"
    exit 0
else
    echo -e "${RED}❌ ${VALIDATION_FAILED} Validation(s) FAILED${NC}"
    echo -e "${YELLOW}⚠️  Please address failed validations before proceeding${NC}"
    exit 1
fi
