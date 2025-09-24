#!/bin/bash

# HazardHawk Production Monitoring Script
# This script monitors the production deployment health and metrics

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_FILE="$SCRIPT_DIR/../production-monitoring.log"
PACKAGE_NAME="com.hazardhawk"

echo -e "${BLUE}📊 HazardHawk Production Monitoring${NC}"
echo -e "${BLUE}===================================${NC}"
echo ""

# Function to log messages
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" | tee -a "$LOG_FILE"
}

# Function to check if device is connected
check_device() {
    if ! adb devices | grep -q "device$"; then
        echo -e "${RED}❌ No Android device connected${NC}"
        echo "Please connect a device or start an emulator"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Android device connected${NC}"
}

# Function to check if app is installed
check_app_installed() {
    if ! adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
        echo -e "${RED}❌ HazardHawk app not installed on device${NC}"
        echo "Please install the production APK first"
        exit 1
    fi
    
    echo -e "${GREEN}✅ HazardHawk app is installed${NC}"
}

# Function to monitor app logs
monitor_app_logs() {
    echo -e "${YELLOW}📱 Monitoring app logs...${NC}"
    echo "Press Ctrl+C to stop monitoring"
    echo ""
    
    # Clear log buffer and start monitoring
    adb logcat -c
    adb logcat | grep -E "HazardHawk|ProductionAI|FeatureFlag|CostMonitor|Gemini|Error|Exception" \
        --line-buffered \
        | while read line; do
            # Color-code log lines based on content
            if [[ $line == *"ERROR"* ]] || [[ $line == *"Exception"* ]]; then
                echo -e "${RED}$line${NC}"
            elif [[ $line == *"WARNING"* ]] || [[ $line == *"WARN"* ]]; then
                echo -e "${YELLOW}$line${NC}"
            elif [[ $line == *"FeatureFlag"* ]] || [[ $line == *"ProductionAI"* ]]; then
                echo -e "${PURPLE}$line${NC}"
            elif [[ $line == *"CostMonitor"* ]] || [[ $line == *"Budget"* ]]; then
                echo -e "${BLUE}$line${NC}"
            else
                echo "$line"
            fi
        done
}

# Function to check app health
check_app_health() {
    echo -e "${YELLOW}🏥 Checking app health...${NC}"
    
    # Check if app is running
    if adb shell ps | grep -q "$PACKAGE_NAME"; then
        echo -e "${GREEN}✅ App is running${NC}"
    else
        echo -e "${RED}❌ App is not running${NC}"
        return 1
    fi
    
    # Check app memory usage
    MEMORY_INFO=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL" | head -1)
    if [ -n "$MEMORY_INFO" ]; then
        echo -e "${BLUE}💾 Memory usage: $MEMORY_INFO${NC}"
    fi
    
    # Check for recent crashes
    CRASH_COUNT=$(adb logcat -d | grep -c "FATAL EXCEPTION.*$PACKAGE_NAME" || echo "0")
    if [ "$CRASH_COUNT" -gt 0 ]; then
        echo -e "${RED}💥 Recent crashes detected: $CRASH_COUNT${NC}"
    else
        echo -e "${GREEN}✅ No recent crashes${NC}"
    fi
}

# Function to monitor network activity
monitor_network() {
    echo -e "${YELLOW}🌐 Monitoring network activity...${NC}"
    
    # Get app UID
    APP_UID=$(adb shell pm list packages -U | grep "$PACKAGE_NAME" | cut -d':' -f3 | cut -d' ' -f1)
    
    if [ -n "$APP_UID" ]; then
        echo "Monitoring network usage for UID: $APP_UID"
        adb shell cat /proc/net/xt_qtaguid/stats | grep "$APP_UID" | head -5
    else
        echo -e "${YELLOW}⚠️  Could not determine app UID${NC}"
    fi
}

# Function to check feature flags status
check_feature_flags() {
    echo -e "${YELLOW}🎛️  Checking feature flags status...${NC}"
    
    # This would typically query your backend API or check app logs
    # For now, we'll look for feature flag logs
    FEATURE_FLAG_LOGS=$(adb logcat -d | grep "FeatureFlag" | tail -10)
    
    if [ -n "$FEATURE_FLAG_LOGS" ]; then
        echo -e "${PURPLE}Recent feature flag activity:${NC}"
        echo "$FEATURE_FLAG_LOGS"
    else
        echo -e "${YELLOW}⚠️  No recent feature flag activity${NC}"
    fi
}

# Function to monitor costs (simulated)
check_cost_metrics() {
    echo -e "${YELLOW}💰 Checking cost metrics...${NC}"
    
    # Look for cost monitoring logs
    COST_LOGS=$(adb logcat -d | grep -E "CostMonitor|Budget|cost.*USD" | tail -5)
    
    if [ -n "$COST_LOGS" ]; then
        echo -e "${BLUE}Recent cost monitoring activity:${NC}"
        echo "$COST_LOGS"
    else
        echo -e "${YELLOW}⚠️  No recent cost monitoring logs${NC}"
    fi
}

# Function to check API health
check_api_health() {
    echo -e "${YELLOW}🔌 Checking API health...${NC}"
    
    # Look for API call logs
    API_LOGS=$(adb logcat -d | grep -E "Gemini|API.*response|HTTP" | tail -5)
    
    if [ -n "$API_LOGS" ]; then
        echo -e "${GREEN}Recent API activity:${NC}"
        echo "$API_LOGS"
    else
        echo -e "${YELLOW}⚠️  No recent API activity${NC}"
    fi
    
    # Check for error patterns
    ERROR_LOGS=$(adb logcat -d | grep -E "ERROR.*API|HTTP.*[45][0-9][0-9]|timeout|failed" | tail -3)
    
    if [ -n "$ERROR_LOGS" ]; then
        echo -e "${RED}Recent API errors:${NC}"
        echo "$ERROR_LOGS"
    else
        echo -e "${GREEN}✅ No recent API errors${NC}"
    fi
}

# Function to perform comprehensive health check
comprehensive_health_check() {
    echo -e "${BLUE}🔍 Comprehensive Health Check${NC}"
    echo -e "${BLUE}=============================${NC}"
    echo ""
    
    check_app_health
    echo ""
    
    check_api_health
    echo ""
    
    check_feature_flags
    echo ""
    
    check_cost_metrics
    echo ""
    
    monitor_network
    echo ""
    
    echo -e "${GREEN}✅ Health check completed${NC}"
}

# Function to show monitoring dashboard
show_dashboard() {
    while true; do
        clear
        echo -e "${BLUE}📊 HazardHawk Production Dashboard${NC}"
        echo -e "${BLUE}==================================${NC}"
        echo "Last updated: $(date)"
        echo ""
        
        comprehensive_health_check
        
        echo ""
        echo -e "${YELLOW}Press 'q' to quit, 'r' to refresh, or wait 30 seconds for auto-refresh${NC}"
        
        # Wait for user input or timeout
        read -t 30 -n 1 key
        case $key in
            q|Q)
                echo ""
                echo -e "${GREEN}Monitoring stopped${NC}"
                break
                ;;
            r|R)
                continue
                ;;
            *)
                continue
                ;;
        esac
    done
}

# Function to analyze logs for issues
analyze_logs() {
    echo -e "${YELLOW}🔍 Analyzing logs for issues...${NC}"
    
    # Get recent logs
    TEMP_LOG="/tmp/hazardhawk_logs.txt"
    adb logcat -d > "$TEMP_LOG"
    
    # Count errors
    ERROR_COUNT=$(grep -c "ERROR\|Exception\|crash" "$TEMP_LOG" 2>/dev/null || echo "0")
    
    # Count warnings
    WARNING_COUNT=$(grep -c "WARNING\|WARN" "$TEMP_LOG" 2>/dev/null || echo "0")
    
    # Look for specific issues
    MEMORY_ISSUES=$(grep -c "OutOfMemoryError\|GC_" "$TEMP_LOG" 2>/dev/null || echo "0")
    NETWORK_ISSUES=$(grep -c "NetworkException\|timeout\|ConnectException" "$TEMP_LOG" 2>/dev/null || echo "0")
    
    echo ""
    echo -e "${BLUE}📈 Log Analysis Results${NC}"
    echo -e "${BLUE}=====================${NC}"
    echo "Total errors: $ERROR_COUNT"
    echo "Total warnings: $WARNING_COUNT"
    echo "Memory issues: $MEMORY_ISSUES"
    echo "Network issues: $NETWORK_ISSUES"
    
    if [ "$ERROR_COUNT" -gt 10 ]; then
        echo -e "${RED}⚠️  High error count detected!${NC}"
    elif [ "$ERROR_COUNT" -gt 0 ]; then
        echo -e "${YELLOW}⚠️  Some errors detected${NC}"
    else
        echo -e "${GREEN}✅ No significant errors${NC}"
    fi
    
    # Clean up
    rm -f "$TEMP_LOG"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  health     - Perform comprehensive health check"
    echo "  logs       - Monitor app logs in real-time"
    echo "  dashboard  - Show interactive monitoring dashboard"
    echo "  analyze    - Analyze logs for issues"
    echo "  network    - Monitor network activity"
    echo "  flags      - Check feature flags status"
    echo "  costs      - Check cost monitoring"
    echo "  api        - Check API health"
    echo ""
    echo "If no command is provided, the interactive dashboard will be shown."
}

# Main execution
main() {
    # Check prerequisites
    check_device
    check_app_installed
    
    case "${1:-dashboard}" in
        health)
            comprehensive_health_check
            ;;
        logs)
            monitor_app_logs
            ;;
        dashboard)
            show_dashboard
            ;;
        analyze)
            analyze_logs
            ;;
        network)
            monitor_network
            ;;
        flags)
            check_feature_flags
            ;;
        costs)
            check_cost_metrics
            ;;
        api)
            check_api_health
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            echo -e "${RED}Unknown command: $1${NC}"
            echo ""
            show_usage
            exit 1
            ;;
    esac
}

# Handle Ctrl+C gracefully
trap 'echo -e "\n${YELLOW}Monitoring stopped by user${NC}"; exit 0' INT

# Run main function
main "$@"