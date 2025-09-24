#!/bin/bash

# HazardHawk Build Diagnosis and Performance Baseline Script
# Performance Monitor Agent - Phase 1 Implementation

set -euo pipefail

# Configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HAZARDHAWK_ROOT="$PROJECT_ROOT/HazardHawk"
REPORTS_DIR="$PROJECT_ROOT/reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DIAGNOSIS_LOG="$REPORTS_DIR/build-diagnosis-$TIMESTAMP.log"

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
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$DIAGNOSIS_LOG"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$DIAGNOSIS_LOG"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$DIAGNOSIS_LOG"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$DIAGNOSIS_LOG"
}

# System information
gather_system_info() {
    log "=== System Information ==="
    log "OS: $(uname -s) $(uname -r) ($(uname -m))"
    log "CPU Cores: $(sysctl -n hw.ncpu 2>/dev/null || nproc 2>/dev/null || echo 'unknown')"
    log "Memory: $(echo "scale=1; $(sysctl -n hw.memsize 2>/dev/null || echo 0) / 1024 / 1024 / 1024" | bc 2>/dev/null || echo 'unknown') GB"
    log "Java: $(java -version 2>&1 | head -1 || echo 'not found')"
    log "Gradle: $(cd "$HAZARDHAWK_ROOT" && ./gradlew --version 2>/dev/null | grep '^Gradle' || echo 'not found')"
}

# Project structure analysis
analyze_project_structure() {
    log "=== Project Structure Analysis ==="
    
    cd "$HAZARDHAWK_ROOT"
    
    if [[ -f "build.gradle.kts" ]]; then
        success " Root build.gradle.kts found"
    else
        error " Root build.gradle.kts missing"
    fi
    
    if [[ -f "settings.gradle.kts" ]]; then
        success " settings.gradle.kts found"
    else
        error " settings.gradle.kts missing"
    fi
    
    if [[ -d "shared" ]]; then
        success " Shared module found"
        local kt_files=$(find shared/src -name "*.kt" -type f 2>/dev/null | wc -l)
        log "  - Kotlin files: $kt_files"
        
        if [[ -f "shared/build.gradle.kts" ]]; then
            success "   Shared build.gradle.kts found"
        else
            error "   Shared build.gradle.kts missing"
        fi
    else
        error " Shared module missing"
    fi
    
    if [[ -d "androidApp" ]]; then
        success " Android app module found"
        if [[ -f "androidApp/build.gradle.kts" ]]; then
            success "   Android build.gradle.kts found"
        else
            error "   Android build.gradle.kts missing"
        fi
    else
        warning "  Android app module missing"
    fi
}

# Gradle configuration check
check_gradle_configuration() {
    log "=== Gradle Configuration Check ==="
    
    cd "$HAZARDHAWK_ROOT"
    
    if [[ -f "gradle.properties" ]]; then
        success " gradle.properties found"
        local jvm_args=$(grep "org.gradle.jvmargs" gradle.properties | head -1)
        log "  JVM Args: $jvm_args"
        
        local parallel=$(grep "org.gradle.parallel" gradle.properties | head -1)
        log "  Parallel: $parallel"
        
        local cache=$(grep "org.gradle.caching" gradle.properties | head -1)
        log "  Caching: $cache"
    else
        warning "  gradle.properties missing - using defaults"
    fi
    
    # Check Gradle daemon status
    local daemon_status=$(./gradlew --status 2>/dev/null || echo "Error checking daemon status")
    log "Gradle Daemon Status:"
    log "$daemon_status"
}

# Compilation test
test_compilation() {
    log "=== Compilation Test ==="
    
    cd "$HAZARDHAWK_ROOT"
    
    local compile_output="$REPORTS_DIR/compile-test-$TIMESTAMP.log"
    
    # Test clean
    log "Testing clean operation..."
    if ./gradlew clean --quiet > "$compile_output" 2>&1; then
        success " Clean operation successful"
    else
        error " Clean operation failed"
        return 1
    fi
    
    # Test compilation phases
    log "Testing shared module compilation..."
    local start_time=$(date +%s.%N)
    
    if timeout 120 ./gradlew :shared:compileKotlinAndroid --quiet >> "$compile_output" 2>&1; then
        local end_time=$(date +%s.%N)
        local duration=$(echo "$end_time - $start_time" | bc)
        success " Shared module compilation successful (${duration}s)"
        echo "$duration" > "$REPORTS_DIR/shared-compile-time.txt"
    else
        local end_time=$(date +%s.%N)
        local duration=$(echo "$end_time - $start_time" | bc)
        error " Shared module compilation failed (${duration}s)"
        
        # Extract first few compilation errors
        log "First 10 compilation errors:"
        grep -E "^e: " "$compile_output" | head -10 | while read -r line; do
            error "  $line"
        done
        
        return 1
    fi
}

# Performance baseline without full build
establish_quick_baseline() {
    log "=== Quick Performance Baseline ==="
    
    cd "$HAZARDHAWK_ROOT"
    
    # Test 1: Clean operation time
    log "Measuring clean operation time..."
    local start_time=$(date +%s.%N)
    if ./gradlew clean --quiet 2>/dev/null; then
        local end_time=$(date +%s.%N)
        local clean_time=$(echo "$end_time - $start_time" | bc)
        success "Clean time: ${clean_time}s"
    else
        error "Clean operation failed"
        local clean_time="unknown"
    fi
    
    # Test 2: Configuration time
    log "Measuring configuration time..."
    start_time=$(date +%s.%N)
    if ./gradlew help --quiet 2>/dev/null; then
        local end_time=$(date +%s.%N)
        local config_time=$(echo "$end_time - $start_time" | bc)
        success "Configuration time: ${config_time}s"
    else
        error "Configuration failed"
        local config_time="unknown"
    fi
    
    # Create baseline report
    cat > "$REPORTS_DIR/performance-baseline-quick.json" << EOF
{
  "timestamp": "$(date -Iseconds)",
  "system": {
    "os": "$(uname -s)",
    "architecture": "$(uname -m)",
    "cpu_cores": "$(sysctl -n hw.ncpu 2>/dev/null || echo 'unknown')",
    "memory_gb": "$(echo "scale=1; $(sysctl -n hw.memsize 2>/dev/null || echo 0) / 1024 / 1024 / 1024" | bc 2>/dev/null || echo 'unknown')"
  },
  "baseline_metrics": {
    "clean_time_seconds": "${clean_time:-unknown}",
    "configuration_time_seconds": "${config_time:-unknown}"
  },
  "build_status": "$(test -f "$REPORTS_DIR/shared-compile-time.txt" && echo "compilable" || echo "has_issues")"
}
EOF
    
    success "Quick baseline established in performance-baseline-quick.json"
}

# Build issues identification
identify_build_issues() {
    log "=== Build Issues Identification ==="
    
    cd "$HAZARDHAWK_ROOT"
    
    local issues_found=0
    
    # Check version catalogs
    if [[ -f "gradle/libs.versions.toml" ]]; then
        success " Version catalog found"
    else
        error " Version catalog missing - dependency management issues expected"
        ((issues_found++))
    fi
    
    # Check for missing files that are referenced
    local compile_errors="$REPORTS_DIR/compile-test-$TIMESTAMP.log"
    if [[ -f "$compile_errors" ]]; then
        local unresolved_count=$(grep -c "Unresolved reference" "$compile_errors" 2>/dev/null || echo 0)
        if [[ $unresolved_count -gt 0 ]]; then
            warning "  $unresolved_count unresolved references found"
            ((issues_found++))
        fi
    fi
    
    log "Total issues identified: $issues_found"
    
    if [[ $issues_found -eq 0 ]]; then
        success "<¯ No major build issues detected!"
    else
        warning "  $issues_found build issues need attention"
    fi
    
    return $issues_found
}

# Generate recommendations
generate_performance_recommendations() {
    log "=== Performance Recommendations ==="
    
    # Based on current analysis
    local cpu_cores=$(sysctl -n hw.ncpu 2>/dev/null || echo 8)
    local memory_gb=$(echo "scale=0; $(sysctl -n hw.memsize 2>/dev/null || echo 8589934592) / 1024 / 1024 / 1024" | bc 2>/dev/null || echo 8)
    
    log "System has $cpu_cores cores and ${memory_gb}GB RAM"
    
    if [[ $cpu_cores -ge 8 && $memory_gb -ge 16 ]]; then
        success " System well-equipped for fast builds"
        success "  Recommended JVM settings: -Xmx6g -XX:MaxMetaspaceSize=2g"
        success "  Recommended Gradle settings: parallel=true, caching=true"
    else
        warning "  System may need optimization for best performance"
        warning "  Consider: Reduce parallel workers, lower memory allocation"
    fi
    
    success "Performance analysis complete"
}

# Main execution
main() {
    log "Starting HazardHawk Build Diagnosis"
    log "Target: <40s shared module build time"
    
    gather_system_info
    analyze_project_structure
    check_gradle_configuration
    test_compilation
    establish_quick_baseline
    identify_build_issues
    generate_performance_recommendations
    
    local exit_code=0
    
    if [[ -f "$REPORTS_DIR/shared-compile-time.txt" ]]; then
        local compile_time=$(cat "$REPORTS_DIR/shared-compile-time.txt")
        if (( $(echo "$compile_time <= 40" | bc -l) )); then
            success "<¯ Compilation target met: ${compile_time}s <= 40s"
        else
            warning "  Compilation exceeds target: ${compile_time}s > 40s"
            exit_code=1
        fi
    else
        error "L Compilation failed - build fixes required"
        exit_code=2
    fi
    
    log "Diagnosis complete. Reports in: $REPORTS_DIR"
    log "Next: Fix build issues then re-run performance baseline"
    
    exit $exit_code
}

main "$@"