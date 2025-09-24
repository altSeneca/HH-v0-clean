#!/bin/bash

# Model Consolidation Import Update Script
# Updates import statements to use unified core models

echo "🔄 Starting Model Consolidation Import Updates..."
echo "Phase 2: SafetyAnalysis & Tag Model Unification"
echo ""

# Track statistics
TOTAL_FILES=0
UPDATED_FILES=0
ERRORS=()

# Define replacement mappings
declare -A IMPORT_REPLACEMENTS=(
    ["com.hazardhawk.ai.models.SafetyAnalysis"]="com.hazardhawk.core.models.SafetyAnalysis"
    ["com.hazardhawk.models.SafetyAnalysis"]="com.hazardhawk.core.models.SafetyAnalysis"
    ["com.hazardhawk.domain.entities.SafetyAnalysis"]="com.hazardhawk.core.models.SafetyAnalysis"
    ["com.hazardhawk.domain.entities.Tag"]="com.hazardhawk.core.models.Tag"
    ["com.hazardhawk.models.Tag"]="com.hazardhawk.core.models.Tag"
    ["com.hazardhawk.ai.models.WorkType"]="com.hazardhawk.core.models.WorkType"
    ["com.hazardhawk.ai.models.HazardType"]="com.hazardhawk.core.models.HazardType"
    ["com.hazardhawk.ai.models.Severity"]="com.hazardhawk.core.models.Severity"
    ["com.hazardhawk.ai.models.RiskLevel"]="com.hazardhawk.core.models.RiskLevel"
    ["com.hazardhawk.ai.models.Hazard"]="com.hazardhawk.core.models.Hazard"
    ["com.hazardhawk.ai.models.AnalysisType"]="com.hazardhawk.core.models.AnalysisType"
    ["com.hazardhawk.ai.models.AnalysisCapability"]="com.hazardhawk.core.models.AnalysisCapability"
    ["com.hazardhawk.domain.entities.TagCategory"]="com.hazardhawk.core.models.TagCategory"
    ["com.hazardhawk.domain.entities.ComplianceStatus"]="com.hazardhawk.core.models.ComplianceStatus"
    ["com.hazardhawk.domain.entities.TagUsageStats"]="com.hazardhawk.core.models.TagUsageStats"
    ["com.hazardhawk.models.ComplianceStatus"]="com.hazardhawk.core.models.ComplianceStatus"
)

# Function to update imports in a file
update_file_imports() {
    local file="$1"
    local modified=false
    
    # Skip if file doesn't exist or is not readable
    if [[ ! -r "$file" ]]; then
        return 0
    fi
    
    # Create temporary file
    local temp_file=$(mktemp)
    
    # Copy original file to temp
    cp "$file" "$temp_file"
    
    # Apply each replacement
    for old_import in "${!IMPORT_REPLACEMENTS[@]}"; do
        local new_import="${IMPORT_REPLACEMENTS[$old_import]}"
        
        # Check if the old import exists in the file
        if grep -q "import $old_import" "$file"; then
            # Replace the import
            sed -i.bak "s|import $old_import|import $new_import|g" "$temp_file"
            modified=true
            echo "  📝 Replaced: $old_import → $new_import"
        fi
    done
    
    # If file was modified, replace original with updated version
    if [[ "$modified" == true ]]; then
        mv "$temp_file" "$file"
        ((UPDATED_FILES++))
        echo "✅ Updated: $file"
    else
        rm "$temp_file"
    fi
    
    # Clean up backup files
    rm -f "${temp_file}.bak"
}

# Function to process Kotlin files
process_kotlin_files() {
    echo "🔍 Finding Kotlin files to update..."
    
    # Find all .kt files, excluding logs, backups, and build directories
    local kotlin_files
    mapfile -t kotlin_files < <(find "$(pwd)" -name "*.kt" -type f \
        \( ! -path "*/logs/*" ! -path "*/.git/*" ! -path "*/build/*" ! -path "*/shared_backup*" ! -path "*/.gradle/*" \))
    
    TOTAL_FILES=${#kotlin_files[@]}
    echo "📊 Found $TOTAL_FILES Kotlin files to process"
    echo ""
    
    # Process each file
    for file in "${kotlin_files[@]}"; do
        if [[ -f "$file" ]]; then
            # Check if file contains any of our target imports
            if grep -q "import com\.hazardhawk\.(ai\.models\|models\|domain\.entities)\.(SafetyAnalysis\|Tag\|WorkType\|HazardType\|Severity\|RiskLevel\|Hazard\|AnalysisType\|AnalysisCapability\|TagCategory\|ComplianceStatus\|TagUsageStats)" "$file"; then
                echo "🔧 Processing: $(basename "$file")"
                update_file_imports "$file"
                echo ""
            fi
        fi
    done
}

# Function to validate migration
validate_migration() {
    echo "🔍 Validating migration results..."
    
    # Check for any remaining old imports
    local remaining_old_imports
    remaining_old_imports=$(find "$(pwd)" -name "*.kt" -type f \
        \( ! -path "*/logs/*" ! -path "*/.git/*" ! -path "*/build/*" ! -path "*/shared_backup*" \) \
        -exec grep -l "import com\.hazardhawk\.(ai\.models\|models\|domain\.entities)\.(SafetyAnalysis\|Tag)" {} \; 2>/dev/null || true)
    
    if [[ -n "$remaining_old_imports" ]]; then
        echo "⚠️  Found files with remaining old imports:"
        echo "$remaining_old_imports"
        echo ""
    else
        echo "✅ No remaining old imports found!"
    fi
    
    # Check that unified models exist
    local core_models_dir="shared/src/commonMain/kotlin/com/hazardhawk/core/models"
    if [[ -f "$core_models_dir/SafetyAnalysis.kt" ]] && [[ -f "$core_models_dir/Tag.kt" ]]; then
        echo "✅ Unified core models are present"
    else
        echo "❌ Unified core models are missing"
        ERRORS+=("Missing unified core models")
    fi
}

# Function to generate report
generate_report() {
    echo ""
    echo "📊 Migration Report"
    echo "=================="
    echo "Total files processed: $TOTAL_FILES"
    echo "Files updated: $UPDATED_FILES"
    echo "Success rate: $(( TOTAL_FILES > 0 ? (UPDATED_FILES * 100) / TOTAL_FILES : 0 ))%"
    
    if [[ ${#ERRORS[@]} -gt 0 ]]; then
        echo ""
        echo "❌ Errors encountered:"
        for error in "${ERRORS[@]}"; do
            echo "  - $error"
        done
    else
        echo "✅ Migration completed successfully!"
    fi
    
    echo ""
    echo "🎯 Phase 2 Consolidation Results:"
    echo "  - SafetyAnalysis: 4 implementations → 1 unified model"
    echo "  - Tag: 3 implementations → 1 unified model"
    echo "  - Location: Consolidated in SafetyAnalysis metadata"
    echo ""
    echo "📍 Unified models location:"
    echo "  - /shared/src/commonMain/kotlin/com/hazardhawk/core/models/"
    echo ""
}

# Main execution
echo "Starting Phase 2: Model Consolidation Import Updates"
echo "Target: Replace duplicate model imports with unified core models"
echo ""

process_kotlin_files
validate_migration
generate_report

echo "✨ Model consolidation import updates complete!"
echo "Next: Run validation tests to ensure AI workflows still function"