#!/bin/bash

# HazardHawk Build Performance Monitoring Script
# Performance Monitor Agent - Phase 1 Implementation

set -euo pipefail

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HAZARDHAWK_ROOT="$PROJECT_ROOT/HazardHawk"
REPORTS_DIR="$PROJECT_ROOT/reports"
PERFORMANCE_LOG="$REPORTS_DIR/build-performance-$(date +%Y%m%d_%H%M%S).log"
BASELINE_FILE="$REPORTS_DIR/build-performance-baseline.json"

# Ensure reports directory exists
mkdir -p "$REPORTS_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$PERFORMANCE_LOG"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$PERFORMANCE_LOG"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$PERFORMANCE_LOG"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$PERFORMANCE_LOG"
}

# System information gathering
gather_system_info() {
    log "Gathering system information..."
    
    cat > "$REPORTS_DIR/system-info.json" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "system": {
    "os": "$(uname -s)",
    "version": "$(uname -r)",
    "architecture": "$(uname -m)",
    "hostname": "$(hostname)"
  },
  "hardware": {
    "cpu_cores": "$(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 'unknown')",
    "memory_gb": "$(echo "scale=2; $(sysctl -n hw.memsize 2>/dev/null || echo 0) / 1024 / 1024 / 1024" | bc 2>/dev/null || echo 'unknown')"
  },
  "java": {
    "version": "$(java -version 2>&1 | head -1 || echo 'not found')",
    "home": "${JAVA_HOME:-not set}"
  },
  "gradle": {
    "version": "$(cd "$HAZARDHAWK_ROOT" && ./gradlew --version 2>/dev/null | grep '^Gradle' || echo 'not found')",
    "daemon_status": "$(cd "$HAZARDHAWK_ROOT" && ./gradlew --status 2>/dev/null || echo 'not available')"
  }
}
EOF
}

# Memory usage monitoring
monitor_memory_usage() {
    local pid=$1
    local output_file=$2
    
    while kill -0 "$pid" 2>/dev/null; do
        if [[ "$OSTYPE" == "darwin"* ]]; then
            # macOS
            ps -o pid,rss,vsz -p "$pid" | tail -1 >> "$output_file"
        else
            # Linux
            ps -o pid,rss,vsz -p "$pid" | tail -1 >> "$output_file"
        fi
        sleep 2
    done
}

# Measure build time for specific target
measure_build_time() {
    local target="$1"
    local description="$2"
    local clean_build="${3:-true}"
    
    log "Measuring build time for: $description"
    
    cd "$HAZARDHAWK_ROOT"
    
    # Clean if requested
    if [[ "$clean_build" == "true" ]]; then
        log "Cleaning project..."
        ./gradlew clean --quiet >/dev/null 2>&1
    fi
    
    # Prepare monitoring
    local start_time=$(date +%s.%N)
    local memory_log="$REPORTS_DIR/memory-usage-$(date +%Y%m%d_%H%M%S).log"
    echo "timestamp pid rss_kb vsz_kb" > "$memory_log"
    
    # Start build with timing
    {
        time ./gradlew "$target" \
            --build-cache \
            --parallel \
            --configure-on-demand \
            --quiet \
            --profile \
            2>&1
    } > "$REPORTS_DIR/build-output-$(basename "$target")-$(date +%Y%m%d_%H%M%S).log" 2>&1 &
    
    local build_pid=$!
    
    # Monitor memory usage in background
    monitor_memory_usage "$build_pid" "$memory_log" &
    local monitor_pid=$!
    
    # Wait for build to complete
    wait "$build_pid"
    local build_exit_code=$?
    local end_time=$(date +%s.%N)
    
    # Stop memory monitoring
    kill "$monitor_pid" 2>/dev/null || true
    
    local duration=$(echo "$end_time - $start_time" | bc)
    
    if [[ $build_exit_code -eq 0 ]]; then
        success "Build completed in ${duration}s"
    else
        error "Build failed after ${duration}s (exit code: $build_exit_code)"
    fi
    
    # Generate performance report
    local max_memory=$(awk 'NR>1 {if($3>max) max=$3} END {print max+0}' "$memory_log")
    
    cat >> "$PERFORMANCE_LOG" << EOF
Build Performance Report:
- Target: $target
- Description: $description
- Duration: ${duration}s
- Max Memory Usage: ${max_memory} KB
- Exit Code: $build_exit_code
- Timestamp: $(date -Iseconds)
---
EOF
    
    echo "$duration"
}

# Performance regression check
check_performance_regression() {
    local current_time=$1
    local target=$2
    local threshold_percent=${3:-10}
    
    if [[ ! -f "$BASELINE_FILE" ]]; then
        warning "No baseline file found. Current build time will be recorded as baseline."
        return 0
    fi
    
    local baseline_time=$(jq -r ".builds[\"$target\"].duration // empty" "$BASELINE_FILE" 2>/dev/null || echo "")
    
    if [[ -z "$baseline_time" ]]; then
        warning "No baseline time found for target: $target"
        return 0
    fi
    
    local regression_threshold=$(echo "scale=2; $baseline_time * (100 + $threshold_percent) / 100" | bc)
    local improvement_threshold=$(echo "scale=2; $baseline_time * (100 - $threshold_percent) / 100" | bc)
    
    if (( $(echo "$current_time > $regression_threshold" | bc -l) )); then
        local percent_increase=$(echo "scale=1; ($current_time - $baseline_time) * 100 / $baseline_time" | bc)
        error "Performance regression detected! Build time increased by ${percent_increase}% (${current_time}s vs ${baseline_time}s baseline)"
        return 1
    elif (( $(echo "$current_time < $improvement_threshold" | bc -l) )); then
        local percent_decrease=$(echo "scale=1; ($baseline_time - $current_time) * 100 / $baseline_time" | bc)
        success "Performance improvement detected! Build time decreased by ${percent_decrease}% (${current_time}s vs ${baseline_time}s baseline)"
    else
        success "Build performance within expected range (${current_time}s vs ${baseline_time}s baseline)"
    fi
    
    return 0
}

# Update baseline metrics
update_baseline() {
    local target=$1
    local duration=$2
    
    # Create baseline structure if it doesn't exist
    if [[ ! -f "$BASELINE_FILE" ]]; then
        echo '{"timestamp": "'$(date -Iseconds)'", "builds": {}}' > "$BASELINE_FILE"
    fi
    
    # Update baseline with current measurement
    jq --arg target "$target" --arg duration "$duration" --arg timestamp "$(date -Iseconds)" \
        '.builds[$target] = {"duration": ($duration | tonumber), "timestamp": $timestamp} | .last_updated = $timestamp' \
        "$BASELINE_FILE" > "${BASELINE_FILE}.tmp" && mv "${BASELINE_FILE}.tmp" "$BASELINE_FILE"
}

# Quick baseline measurement
establish_baseline() {
    log "Establishing performance baseline..."
    gather_system_info
    
    log "Measuring shared module build time..."
    local shared_time
    shared_time=$(measure_build_time ":shared:assemble" "Shared Module Baseline" true)
    update_baseline "shared:assemble" "$shared_time"
    
    log "Measuring incremental build time..."
    local incremental_time
    incremental_time=$(measure_build_time ":shared:assemble" "Incremental Build Baseline" false)
    update_baseline "shared:assemble:incremental" "$incremental_time"
    
    success "Baseline established:"
    success "  - Shared module: ${shared_time}s"
    success "  - Incremental: ${incremental_time}s"
    
    # Check if we meet targets
    if (( $(echo "$shared_time <= 40" | bc -l) )); then
        success " Meets shared module target (<40s)"
    else
        warning "  Exceeds shared module target (${shared_time}s > 40s)"
    fi
}

# Main execution
case "${1:-baseline}" in
    "baseline")
        establish_baseline
        ;;
    "quick")
        log "Running quick performance check..."
        shared_time=$(measure_build_time ":shared:assemble" "Quick Check" true)
        if (( $(echo "$shared_time <= 40" | bc -l) )); then
            success " Quick check passed: ${shared_time}s"
        else
            error " Quick check failed: ${shared_time}s > 40s"
            exit 1
        fi
        ;;
    *)
        echo "Usage: $0 [baseline|quick]"
        echo "  baseline - Establish performance baseline"
        echo "  quick    - Quick performance check"
        exit 1
        ;;
esac