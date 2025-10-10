#!/bin/bash
set -e

echo "🔄 Updating import statements in shared module..."
echo ""

# Counter for files changed
changed_count=0

# Find all Kotlin files and update imports
find shared/src -name "*.kt" -type f | while read file; do
    # Check if file contains any of the old imports
    if grep -q -E "import com\.(hazardhawk\.(models|domain\.entities)|hazardhawk\.ai\.models)" "$file"; then
        # Perform the replacements
        sed -i '' \
            -e 's/import com\.hazardhawk\.models\./import com.hazardhawk.core.models./g' \
            -e 's/import com\.hazardhawk\.ai\.models\./import com.hazardhawk.core.models./g' \
            -e 's/import com\.hazardhawk\.domain\.entities\./import com.hazardhawk.core.models./g' \
            "$file"

        echo "✓ Updated: $file"
        ((changed_count++))
    fi
done

echo ""
echo "✅ Import statements updated"
echo ""
echo "📊 Summary:"
echo "   - Old imports: com.hazardhawk.models.*"
echo "   - Old imports: com.hazardhawk.ai.models.*"  
echo "   - Old imports: com.hazardhawk.domain.entities.*"
echo "   - New imports: com.hazardhawk.core.models.*"
echo ""
echo "🔍 Next steps:"
echo "   1. Review changes: git diff"
echo "   2. Test build: ./gradlew :shared:compileKotlinMetadata"
echo "   3. If successful, delete old packages"
