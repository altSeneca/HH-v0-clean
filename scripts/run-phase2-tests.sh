#!/bin/bash
# Phase 2 Test Execution Script
# Version: 1.0
# Date: October 9, 2025
# Owner: test-guardian agent

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Change to HazardHawk directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT/HazardHawk"

echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}Phase 2 Test Execution Script${NC}"
echo -e "${BLUE}=====================================${NC}"
echo ""

# Track results
PHASE1_RESULT=0
PHASE2_RESULT=0
PHASE3_RESULT=0
PHASE4_RESULT=0

# Phase 1: Smoke Tests
echo -e "${YELLOW}[Phase 1] Running smoke tests...${NC}"
if ./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.test.BasicFrameworkTest" \
  --no-daemon; then
  echo -e "${GREEN}✅ Phase 1: Smoke tests passed${NC}"
  PHASE1_RESULT=0
else
  echo -e "${RED}❌ Phase 1: Smoke tests failed${NC}"
  PHASE1_RESULT=1
  echo -e "${RED}HALTING: Fix smoke tests before proceeding${NC}"
  exit 1
fi
echo ""

# Phase 2: Service Tests
echo -e "${YELLOW}[Phase 2] Running service tests...${NC}"
if ./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.domain.services.*" \
  --continue \
  --no-daemon; then
  echo -e "${GREEN}✅ Phase 2: Service tests passed${NC}"
  PHASE2_RESULT=0
else
  echo -e "${RED}⚠️  Phase 2: Some service tests failed (continuing)${NC}"
  PHASE2_RESULT=1
fi
echo ""

# Phase 3: Repository Tests
echo -e "${YELLOW}[Phase 3] Running repository tests...${NC}"
if ./gradlew :shared:testDebugUnitTest \
  --tests "com.hazardhawk.data.repositories.*" \
  --continue \
  --no-daemon; then
  echo -e "${GREEN}✅ Phase 3: Repository tests passed${NC}"
  PHASE3_RESULT=0
else
  echo -e "${RED}⚠️  Phase 3: Some repository tests failed (continuing)${NC}"
  PHASE3_RESULT=1
fi
echo ""

# Phase 4: Integration Tests
echo -e "${YELLOW}[Phase 4] Running integration tests...${NC}"
if ./gradlew :shared:integrationTest --no-daemon 2>/dev/null || \
   ./gradlew :shared:testDebugUnitTest \
     --tests "com.hazardhawk.integration.*" \
     --no-daemon; then
  echo -e "${GREEN}✅ Phase 4: Integration tests passed${NC}"
  PHASE4_RESULT=0
else
  echo -e "${RED}⚠️  Phase 4: Some integration tests failed${NC}"
  PHASE4_RESULT=1
fi
echo ""

# Coverage Report
echo -e "${YELLOW}[Coverage] Generating coverage report...${NC}"
if ./gradlew :shared:jacocoTestReport --no-daemon 2>/dev/null; then
  echo -e "${GREEN}✅ Coverage report generated${NC}"
  echo -e "${BLUE}Report: HazardHawk/shared/build/reports/jacoco/test/html/index.html${NC}"
else
  echo -e "${YELLOW}⚠️  Coverage report generation not available${NC}"
fi
echo ""

# Summary
echo -e "${BLUE}=====================================${NC}"
echo -e "${BLUE}Test Execution Summary${NC}"
echo -e "${BLUE}=====================================${NC}"
echo -e "Phase 1 (Smoke):       $( [ $PHASE1_RESULT -eq 0 ] && echo -e "${GREEN}PASS${NC}" || echo -e "${RED}FAIL${NC}" )"
echo -e "Phase 2 (Services):    $( [ $PHASE2_RESULT -eq 0 ] && echo -e "${GREEN}PASS${NC}" || echo -e "${YELLOW}WARN${NC}" )"
echo -e "Phase 3 (Repositories):$( [ $PHASE3_RESULT -eq 0 ] && echo -e "${GREEN}PASS${NC}" || echo -e "${YELLOW}WARN${NC}" )"
echo -e "Phase 4 (Integration): $( [ $PHASE4_RESULT -eq 0 ] && echo -e "${GREEN}PASS${NC}" || echo -e "${YELLOW}WARN${NC}" )"
echo -e "${BLUE}=====================================${NC}"

# Exit with failure if critical phases failed
TOTAL_FAILURES=$((PHASE1_RESULT + PHASE2_RESULT + PHASE3_RESULT + PHASE4_RESULT))
if [ $TOTAL_FAILURES -eq 0 ]; then
  echo -e "${GREEN}✅ All test phases passed!${NC}"
  exit 0
elif [ $PHASE1_RESULT -ne 0 ]; then
  echo -e "${RED}❌ Critical failure: Smoke tests failed${NC}"
  exit 1
else
  echo -e "${YELLOW}⚠️  Some tests failed. Review output above.${NC}"
  exit $TOTAL_FAILURES
fi
