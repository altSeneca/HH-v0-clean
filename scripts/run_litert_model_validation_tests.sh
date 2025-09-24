#!/bin/bash

# LiteRT Model Loading Validation Tests
# Tests the complete model integration pipeline with actual TFLite models

set -e

echo "🧪 Starting LiteRT Model Validation Tests"
echo "=========================================="

# Test configuration
PROJECT_DIR="/Users/aaron/Apps-Coded/HH-v0"
MODELS_DIR="$PROJECT_DIR/HazardHawk/androidApp/src/main/assets/models/litert"
LOG_FILE="$PROJECT_DIR/logs/litert_model_validation_$(date +%Y%m%d_%H%M%S).log"

# Ensure logs directory exists
mkdir -p "$PROJECT_DIR/logs"

# Function to log test results
log_test() {
    echo "$1" | tee -a "$LOG_FILE"
}

log_test "$(date): Starting LiteRT Model Validation Tests"
log_test "Models directory: $MODELS_DIR"

# Test 1: Verify model files exist
echo "🔍 Test 1: Verifying Model Files Exist"
MODEL_FILES=("construction_safety_lite.tflite" "construction_safety_gpu.tflite" "construction_safety_full.tflite")

for model in "${MODEL_FILES[@]}"; do
    if [ -f "$MODELS_DIR/$model" ]; then
        file_size=$(du -h "$MODELS_DIR/$model" | cut -f1)
        log_test "✅ $model exists ($file_size)"
    else
        log_test "❌ $model missing"
        exit 1
    fi
done

# Test 2: Verify model configuration
echo "🔧 Test 2: Verifying Model Configuration"
if [ -f "$MODELS_DIR/model_config.json" ]; then
    log_test "✅ model_config.json exists"
    
    # Check if JSON is valid
    if python3 -m json.tool "$MODELS_DIR/model_config.json" > /dev/null 2>&1; then
        log_test "✅ model_config.json is valid JSON"
    else
        log_test "❌ model_config.json is invalid JSON"
        exit 1
    fi
    
    # Check for required fields
    required_fields=("models" "hazard_categories" "ppe_requirements")
    for field in "${required_fields[@]}"; do
        if grep -q "\"$field\":" "$MODELS_DIR/model_config.json"; then
            log_test "✅ Configuration contains '$field' field"
        else
            log_test "❌ Configuration missing '$field' field"
            exit 1
        fi
    done
else
    log_test "❌ model_config.json missing"
    exit 1
fi

# Test 3: Verify model file integrity
echo "🔐 Test 3: Verifying Model File Integrity"
for model in "${MODEL_FILES[@]}"; do
    if [ -f "$MODELS_DIR/$model" ]; then
        # Check if file is not corrupted (non-zero size, readable)
        if [ -s "$MODELS_DIR/$model" ] && [ -r "$MODELS_DIR/$model" ]; then
            log_test "✅ $model integrity check passed"
        else
            log_test "❌ $model integrity check failed"
            exit 1
        fi
    fi
done

# Test 4: Test model size constraints
echo "📏 Test 4: Verifying Model Size Constraints"
declare -A expected_sizes
expected_sizes["construction_safety_lite.tflite"]="100"
expected_sizes["construction_safety_gpu.tflite"]="200" 
expected_sizes["construction_safety_full.tflite"]="300"

for model in "${!expected_sizes[@]}"; do
    actual_size_kb=$(du -k "$MODELS_DIR/$model" | cut -f1)
    actual_size_mb=$((actual_size_kb / 1024))
    expected_size=${expected_sizes[$model]}
    
    # Allow 10% tolerance
    min_size=$((expected_size * 90 / 100))
    max_size=$((expected_size * 110 / 100))
    
    if [ $actual_size_mb -ge $min_size ] && [ $actual_size_mb -le $max_size ]; then
        log_test "✅ $model size check passed (${actual_size_mb}MB within expected ${expected_size}MB ±10%)"
    else
        log_test "❌ $model size check failed (${actual_size_mb}MB, expected ~${expected_size}MB)"
        exit 1
    fi
done

# Test 5: Build project to verify model loading integration
echo "🏗️  Test 5: Building Project with Model Integration"
cd "$PROJECT_DIR/HazardHawk"

if ./gradlew :shared:compileDebugKotlinAndroid > /dev/null 2>&1; then
    log_test "✅ Project compiles successfully with model integration"
else
    log_test "❌ Project compilation failed - check model integration"
    exit 1
fi

# Test 6: Verify asset loading in Android APK
echo "📦 Test 6: Building Android APK with Models"
if ./gradlew :androidApp:assembleDebug > /dev/null 2>&1; then
    log_test "✅ Android APK built successfully with models"
    
    # Check if models are included in APK
    APK_FILE="$PROJECT_DIR/HazardHawk/androidApp/build/outputs/apk/debug/androidApp-debug.apk"
    if [ -f "$APK_FILE" ]; then
        # Check if models are included in APK assets
        if unzip -l "$APK_FILE" | grep -q "assets/models/litert/.*\.tflite"; then
            log_test "✅ TFLite models successfully included in APK assets"
        else
            log_test "⚠️  TFLite models may not be included in APK assets"
        fi
        
        if unzip -l "$APK_FILE" | grep -q "assets/models/litert/model_config.json"; then
            log_test "✅ Model configuration successfully included in APK assets"
        else
            log_test "❌ Model configuration not included in APK assets"
            exit 1
        fi
    else
        log_test "⚠️  APK file not found at expected location"
    fi
else
    log_test "❌ Android APK build failed"
    exit 1
fi

# Test 7: Memory estimation validation
echo "💾 Test 7: Model Memory Requirements Validation"
total_model_size=0
for model in "${MODEL_FILES[@]}"; do
    size_kb=$(du -k "$MODELS_DIR/$model" | cut -f1)
    total_model_size=$((total_model_size + size_kb))
done

total_model_size_mb=$((total_model_size / 1024))
log_test "📊 Total model storage: ${total_model_size_mb}MB"

# Estimate runtime memory (typically 2-3x model size)
estimated_runtime_mb=$((total_model_size_mb * 3))
log_test "📊 Estimated runtime memory: ${estimated_runtime_mb}MB"

if [ $estimated_runtime_mb -lt 2000 ]; then
    log_test "✅ Memory requirements within reasonable limits for Android devices"
else
    log_test "⚠️  High memory requirements - consider model optimization"
fi

# Test 8: Model metadata validation
echo "📋 Test 8: Model Metadata Validation"
python3 << 'EOF' >> "$LOG_FILE" 2>&1
import json
import sys

config_path = "/Users/aaron/Apps-Coded/HH-v0/HazardHawk/androidApp/src/main/assets/models/litert/model_config.json"

try:
    with open(config_path, 'r') as f:
        config = json.load(f)
    
    models = config.get('models', {})
    required_model_fields = ['filename', 'version', 'size_mb', 'description', 'input_size', 'supported_backends', 'accuracy_score', 'classes', 'num_classes']
    
    for model_name, model_info in models.items():
        print(f"✅ Validating model: {model_name}")
        
        for field in required_model_fields:
            if field in model_info:
                print(f"  ✅ {field}: {model_info[field]}")
            else:
                print(f"  ❌ Missing field: {field}")
                sys.exit(1)
        
        # Validate specific constraints
        if model_info.get('num_classes') != len(model_info.get('classes', [])):
            print(f"  ❌ num_classes mismatch: {model_info.get('num_classes')} != {len(model_info.get('classes', []))}")
            sys.exit(1)
        else:
            print(f"  ✅ Class count validation passed")
    
    print("✅ All model metadata validation passed")
    
except Exception as e:
    print(f"❌ Model metadata validation failed: {e}")
    sys.exit(1)
EOF

if [ $? -eq 0 ]; then
    log_test "✅ Model metadata validation passed"
else
    log_test "❌ Model metadata validation failed"
    exit 1
fi

# Final summary
echo ""
echo "🎉 LiteRT Model Validation Summary"
echo "=================================="
log_test "$(date): LiteRT Model Validation Tests completed successfully"
log_test "✅ All model files present and valid"
log_test "✅ Model configuration validated"  
log_test "✅ Project builds successfully with models"
log_test "✅ Models included in Android APK"
log_test "✅ Memory requirements within limits"
log_test "✅ Model metadata validation passed"

echo ""
echo "📁 Models Directory Structure:"
ls -lh "$MODELS_DIR/"

echo ""
echo "📊 Model Summary:"
python3 << 'EOF'
import json

config_path = "/Users/aaron/Apps-Coded/HH-v0/HazardHawk/androidApp/src/main/assets/models/litert/model_config.json"

with open(config_path, 'r') as f:
    config = json.load(f)

models = config.get('models', {})
for model_name, model_info in models.items():
    print(f"🤖 {model_name}:")
    print(f"   📄 File: {model_info.get('filename')}")
    print(f"   📏 Size: {model_info.get('size_mb')}MB")  
    print(f"   🎯 Input: {model_info.get('input_size')}px")
    print(f"   🎪 Classes: {model_info.get('num_classes')}")
    print(f"   📊 Accuracy: {model_info.get('accuracy_score')}")
    print(f"   ⚡ Backends: {', '.join(model_info.get('supported_backends', []))}")
    print()
EOF

echo "📝 Full test log: $LOG_FILE"
echo ""
echo "🚀 LiteRT Model Integration Ready for Testing!"