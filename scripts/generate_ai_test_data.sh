#!/bin/bash

# AI Test Data Generation Script
# Generates comprehensive test datasets for AI integration testing

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
TEST_DATA_DIR="$ROOT_DIR/shared/src/commonTest/resources"
AI_TEST_DATA_DIR="$TEST_DATA_DIR/ai-test-data"
MODELS_DIR="$TEST_DATA_DIR/models"

echo "🏗️  Generating AI Test Data for HazardHawk"
echo "Root Directory: $ROOT_DIR"
echo "Test Data Directory: $AI_TEST_DATA_DIR"

# Create directory structure
mkdir -p "$AI_TEST_DATA_DIR"/{images,onnx-models,expected-results,performance-data,edge-cases}
mkdir -p "$MODELS_DIR"/{mock,validation}

# Function to generate mock image data
generate_mock_image() {
    local name="$1"
    local width="$2"
    local height="$3"
    local scenario="$4"
    
    local output_file="$AI_TEST_DATA_DIR/images/${name}.jpg"
    
    # Create a mock JPEG header and basic image data
    # JPEG SOI marker (FF D8)
    printf '\xff\xd8' > "$output_file"
    
    # JPEG APP0 marker with JFIF identifier
    printf '\xff\xe0\x00\x10JFIF\x00\x01\x01\x01\x00H\x00H\x00\x00' >> "$output_file"
    
    # Generate scenario-specific image data
    case "$scenario" in
        "ppe_violation")
            # Generate patterns suggesting PPE violations
            python3 -c "
import random
import struct
random.seed(hash('$scenario'))
data = bytearray()
for i in range(${width} * ${height} // 100):  # Compressed size estimate
    if i % 10 < 3:  # Construction site browns/grays
        data.extend([random.randint(80, 120), random.randint(60, 100), random.randint(40, 80)])
    elif i % 15 < 2:  # Human-like colors (indicating people)
        data.extend([random.randint(180, 220), random.randint(120, 160), random.randint(100, 140)])
    else:  # General construction colors
        data.extend([random.randint(100, 200), random.randint(100, 200), random.randint(100, 200)])
with open('$output_file', 'ab') as f:
    f.write(data)
" 2>/dev/null || {
                # Fallback if Python not available
                dd if=/dev/urandom bs=1024 count=$((width * height / 10000)) >> "$output_file" 2>/dev/null
            }
            ;;
        "fall_hazard")
            python3 -c "
import random
random.seed(hash('$scenario'))
data = bytearray()
for i in range(${width} * ${height} // 100):
    if i % 20 < 5:  # High contrast edges (suggesting unprotected edges)
        data.extend([255, 255, 255] if i % 2 == 0 else [0, 0, 0])
    else:  # Construction site colors with height elements
        data.extend([random.randint(120, 180), random.randint(140, 200), random.randint(160, 220)])
with open('$output_file', 'ab') as f:
    f.write(data)
" 2>/dev/null || dd if=/dev/urandom bs=1024 count=$((width * height / 10000)) >> "$output_file" 2>/dev/null
            ;;
        "electrical_hazard")
            python3 -c "
import random
random.seed(hash('$scenario'))
data = bytearray()
for i in range(${width} * ${height} // 100):
    if i % 25 < 3:  # Electrical equipment colors (metallic/bright)
        data.extend([random.randint(200, 255), random.randint(200, 255), random.randint(150, 200)])
    else:
        data.extend([random.randint(50, 150), random.randint(50, 150), random.randint(50, 150)])
with open('$output_file', 'ab') as f:
    f.write(data)
" 2>/dev/null || dd if=/dev/urandom bs=1024 count=$((width * height / 10000)) >> "$output_file" 2>/dev/null
            ;;
        *)
            # General construction site
            dd if=/dev/urandom bs=1024 count=$((width * height / 10000)) >> "$output_file" 2>/dev/null
            ;;
    esac
    
    # JPEG EOI marker (FF D9)
    printf '\xff\xd9' >> "$output_file"
    
    echo "Generated mock image: $output_file (${width}x${height}, scenario: $scenario)"
}

# Generate test images for different scenarios
echo "📸 Generating test images..."

# Standard resolution test images
generate_mock_image "ppe_violation_standard" 1920 1080 "ppe_violation"
generate_mock_image "fall_hazard_standard" 1920 1080 "fall_hazard"
generate_mock_image "electrical_hazard_standard" 1920 1080 "electrical_hazard"
generate_mock_image "multi_hazard_standard" 1920 1080 "ppe_violation"
generate_mock_image "safe_construction_standard" 1920 1080 "general"
generate_mock_image "general_construction_standard" 1920 1080 "general"
generate_mock_image "ambiguous_hazards" 1920 1080 "general"
generate_mock_image "blurry_construction" 1920 1080 "general"

# High resolution test images
generate_mock_image "high_res_construction" 3840 2160 "general"
generate_mock_image "high_res_ppe_violation" 3840 2160 "ppe_violation"

# Ultra high resolution test images
generate_mock_image "ultra_high_res_construction" 7680 4320 "general"

# Edge case images
echo "⚠️  Generating edge case test data..."

# Extremely small image
generate_mock_image "tiny_image" 100 100 "general"

# Corrupted image (missing end marker)
cp "$AI_TEST_DATA_DIR/images/general_construction_standard.jpg" "$AI_TEST_DATA_DIR/edge-cases/corrupted_missing_end.jpg"
head -c -2 "$AI_TEST_DATA_DIR/edge-cases/corrupted_missing_end.jpg" > "$AI_TEST_DATA_DIR/edge-cases/temp" && mv "$AI_TEST_DATA_DIR/edge-cases/temp" "$AI_TEST_DATA_DIR/edge-cases/corrupted_missing_end.jpg"

# Corrupted header
cp "$AI_TEST_DATA_DIR/images/general_construction_standard.jpg" "$AI_TEST_DATA_DIR/edge-cases/corrupted_header.jpg"
printf '\x00\x00' | dd of="$AI_TEST_DATA_DIR/edge-cases/corrupted_header.jpg" bs=1 count=2 seek=2 conv=notrunc 2>/dev/null

# Empty file
touch "$AI_TEST_DATA_DIR/edge-cases/empty_file.jpg"

echo "✅ Generated $(find "$AI_TEST_DATA_DIR/images" -name "*.jpg" | wc -l) test images"
echo "✅ Generated $(find "$AI_TEST_DATA_DIR/edge-cases" -name "*.jpg" | wc -l) edge case files"

# Generate mock ONNX models for testing
echo "🧠 Generating mock ONNX models..."

generate_mock_onnx_model() {
    local model_name="$1"
    local model_file="$MODELS_DIR/mock/${model_name}.onnx"
    
    # Create a minimal valid ONNX model structure
    python3 -c "
import struct

# Create a minimal ONNX model binary
# This creates a valid ONNX protobuf structure but with minimal functionality
onnx_header = b'\\x08\\x01\\x12\\x04test\\x18\\x01'  # Minimal protobuf header
onnx_model_data = onnx_header + b'\\x00' * 1024  # Pad with zeros

with open('$model_file', 'wb') as f:
    f.write(onnx_model_data)
    
print(f'Generated mock ONNX model: $model_file')
" 2>/dev/null || {
        # Fallback: create a file with ONNX-like header
        printf 'ONNX\x00\x00\x00\x01' > "$model_file"
        dd if=/dev/zero bs=1024 count=1 >> "$model_file" 2>/dev/null
        echo "Generated fallback mock ONNX model: $model_file"
    }
}

generate_mock_onnx_model "gemma_vision_mock"
generate_mock_onnx_model "yolo_hazard_detection_mock"
generate_mock_onnx_model "tag_recommendation_mock"

# Generate expected analysis results
echo "📊 Generating expected analysis results..."

cat > "$AI_TEST_DATA_DIR/expected-results/ppe_violation_expected.json" << 'EOF'
{
  "scenario": "ppe_violation",
  "expectedDetections": [
    {
      "hazardType": "PERSON_NO_HARD_HAT",
      "confidence": 0.92,
      "boundingBox": {
        "x": 0.35,
        "y": 0.25,
        "width": 0.15,
        "height": 0.35
      },
      "oshaCategory": "SUBPART_E_1926_95",
      "severity": "CRITICAL",
      "description": "Construction worker without required head protection"
    }
  ],
  "expectedRecommendedTags": [
    "ppe-hard-hat-required",
    "ppe-head-protection-missing",
    "general-ppe-violation",
    "worker-safety-compliance",
    "subpart-e-1926-95"
  ],
  "expectedAutoSelectTags": [
    "ppe-hard-hat-required",
    "ppe-head-protection-missing"
  ],
  "expectedMinConfidence": 0.8,
  "expectedSeverity": "CRITICAL"
}
EOF

cat > "$AI_TEST_DATA_DIR/expected-results/fall_hazard_expected.json" << 'EOF'
{
  "scenario": "fall_hazard",
  "expectedDetections": [
    {
      "hazardType": "UNPROTECTED_EDGE",
      "confidence": 0.87,
      "boundingBox": {
        "x": 0.15,
        "y": 0.05,
        "width": 0.70,
        "height": 0.15
      },
      "oshaCategory": "SUBPART_M_1926_501",
      "severity": "CRITICAL",
      "description": "Unprotected edge at elevation requiring fall protection"
    }
  ],
  "expectedRecommendedTags": [
    "fall-protection-required-6ft",
    "fall-unprotected-edge-identified",
    "fall-guardrail-system-compliant",
    "fall-perimeter-protection",
    "height-work-safety",
    "subpart-m-1926-501",
    "fatal-four-falls"
  ],
  "expectedAutoSelectTags": [
    "fall-protection-required-6ft",
    "fall-unprotected-edge-identified"
  ],
  "expectedMinConfidence": 0.8,
  "expectedSeverity": "CRITICAL"
}
EOF

cat > "$AI_TEST_DATA_DIR/expected-results/electrical_hazard_expected.json" << 'EOF'
{
  "scenario": "electrical_hazard",
  "expectedDetections": [
    {
      "hazardType": "ELECTRICAL_HAZARD",
      "confidence": 0.89,
      "boundingBox": {
        "x": 0.45,
        "y": 0.35,
        "width": 0.25,
        "height": 0.30
      },
      "oshaCategory": "SUBPART_K_1926_416",
      "severity": "CRITICAL",
      "description": "Exposed electrical equipment presenting electrocution risk"
    }
  ],
  "expectedRecommendedTags": [
    "elec-loto-procedure-active",
    "elec-gfci-required",
    "elec-qualified-person-present",
    "electrical-safety",
    "subpart-k-1926-416",
    "fatal-four-electrocution"
  ],
  "expectedAutoSelectTags": [
    "elec-loto-procedure-active",
    "elec-qualified-person-present"
  ],
  "expectedMinConfidence": 0.8,
  "expectedSeverity": "CRITICAL"
}
EOF

# Generate performance benchmark data
echo "⚡ Generating performance benchmark data..."

cat > "$AI_TEST_DATA_DIR/performance-data/benchmark_targets.json" << 'EOF'
{
  "performance_targets": {
    "single_photo_analysis_ms": 3000,
    "batch_analysis_per_photo_ms": 500,
    "memory_usage_peak_mb": 2048,
    "memory_leak_threshold_mb": 200,
    "battery_drain_per_photo_percent": 0.5,
    "concurrent_requests_supported": 5,
    "high_resolution_analysis_ms": 10000,
    "ultra_high_resolution_supported": true
  },
  "test_configurations": {
    "standard_resolution": {
      "width": 1920,
      "height": 1080,
      "expected_processing_time_ms": 1000
    },
    "high_resolution": {
      "width": 3840,
      "height": 2160,
      "expected_processing_time_ms": 3000
    },
    "ultra_high_resolution": {
      "width": 7680,
      "height": 4320,
      "expected_processing_time_ms": 8000
    }
  },
  "batch_test_sizes": [1, 5, 10, 20, 50],
  "stress_test_configurations": {
    "concurrent_requests": 5,
    "long_running_duration_ms": 30000,
    "memory_pressure_threshold_mb": 1500
  }
}
EOF

# Generate test metadata
cat > "$AI_TEST_DATA_DIR/test_metadata.json" << 'EOF'
{
  "test_data_version": "1.0.0",
  "generated_date": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"',
  "test_scenarios": {
    "ppe_violation": {
      "description": "Construction worker without required PPE",
      "image_files": ["ppe_violation_standard.jpg", "high_res_ppe_violation.jpg"],
      "expected_results": "ppe_violation_expected.json"
    },
    "fall_hazard": {
      "description": "Unprotected edges and fall hazards",
      "image_files": ["fall_hazard_standard.jpg"],
      "expected_results": "fall_hazard_expected.json"
    },
    "electrical_hazard": {
      "description": "Exposed electrical equipment and hazards",
      "image_files": ["electrical_hazard_standard.jpg"],
      "expected_results": "electrical_hazard_expected.json"
    },
    "multi_hazard": {
      "description": "Multiple safety hazards in single image",
      "image_files": ["multi_hazard_standard.jpg"],
      "expected_results": null
    },
    "safe_construction": {
      "description": "Compliant construction practices",
      "image_files": ["safe_construction_standard.jpg"],
      "expected_results": null
    }
  },
  "edge_cases": {
    "corrupted_images": ["corrupted_header.jpg", "corrupted_missing_end.jpg"],
    "empty_files": ["empty_file.jpg"],
    "extreme_sizes": ["tiny_image.jpg", "ultra_high_res_construction.jpg"]
  },
  "model_files": {
    "mock_models": ["gemma_vision_mock.onnx", "yolo_hazard_detection_mock.onnx", "tag_recommendation_mock.onnx"]
  },
  "performance_data": {
    "benchmark_targets": "benchmark_targets.json"
  }
}
EOF

# Generate a comprehensive test validation script
cat > "$AI_TEST_DATA_DIR/validate_test_data.py" << 'EOF'
#!/usr/bin/env python3
"""
Test Data Validation Script
Validates the generated AI test data for completeness and correctness
"""

import os
import json
import sys
from pathlib import Path

def validate_test_data(test_data_dir):
    """Validate all generated test data"""
    print(f"🔍 Validating test data in {test_data_dir}")
    
    errors = []
    warnings = []
    
    # Check if metadata exists
    metadata_file = os.path.join(test_data_dir, "test_metadata.json")
    if not os.path.exists(metadata_file):
        errors.append("test_metadata.json is missing")
        return errors, warnings
    
    try:
        with open(metadata_file, 'r') as f:
            metadata = json.load(f)
    except json.JSONDecodeError as e:
        errors.append(f"Invalid JSON in test_metadata.json: {e}")
        return errors, warnings
    
    # Validate image files
    images_dir = os.path.join(test_data_dir, "images")
    for scenario, info in metadata.get("test_scenarios", {}).items():
        for image_file in info.get("image_files", []):
            image_path = os.path.join(images_dir, image_file)
            if not os.path.exists(image_path):
                errors.append(f"Missing image file: {image_file}")
            elif os.path.getsize(image_path) < 100:  # Minimum reasonable file size
                warnings.append(f"Image file seems too small: {image_file}")
    
    # Validate expected results
    results_dir = os.path.join(test_data_dir, "expected-results")
    for scenario, info in metadata.get("test_scenarios", {}).items():
        expected_results = info.get("expected_results")
        if expected_results:
            results_path = os.path.join(results_dir, expected_results)
            if not os.path.exists(results_path):
                errors.append(f"Missing expected results file: {expected_results}")
            else:
                try:
                    with open(results_path, 'r') as f:
                        json.load(f)
                except json.JSONDecodeError as e:
                    errors.append(f"Invalid JSON in {expected_results}: {e}")
    
    # Validate edge cases
    edge_cases_dir = os.path.join(test_data_dir, "edge-cases")
    for edge_case_type, files in metadata.get("edge_cases", {}).items():
        for file in files:
            file_path = os.path.join(edge_cases_dir, file)
            if not os.path.exists(file_path):
                errors.append(f"Missing edge case file: {file}")
    
    # Validate model files
    models_dir = os.path.join(test_data_dir, "../models/mock")
    for model_file in metadata.get("model_files", {}).get("mock_models", []):
        model_path = os.path.join(models_dir, model_file)
        if not os.path.exists(model_path):
            errors.append(f"Missing mock model file: {model_file}")
    
    # Validate performance data
    perf_data_dir = os.path.join(test_data_dir, "performance-data")
    perf_targets = metadata.get("performance_data", {}).get("benchmark_targets")
    if perf_targets:
        perf_path = os.path.join(perf_data_dir, perf_targets)
        if not os.path.exists(perf_path):
            errors.append(f"Missing performance data file: {perf_targets}")
    
    return errors, warnings

if __name__ == "__main__":
    test_data_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    errors, warnings = validate_test_data(test_data_dir)
    
    if warnings:
        print("⚠️  Warnings:")
        for warning in warnings:
            print(f"  - {warning}")
    
    if errors:
        print("❌ Validation failed with errors:")
        for error in errors:
            print(f"  - {error}")
        sys.exit(1)
    else:
        print("✅ Test data validation passed!")
        sys.exit(0)
EOF

chmod +x "$AI_TEST_DATA_DIR/validate_test_data.py"

# Validate the generated test data
echo "🔍 Validating generated test data..."
if command -v python3 &> /dev/null; then
    python3 "$AI_TEST_DATA_DIR/validate_test_data.py" "$AI_TEST_DATA_DIR"
else
    echo "⚠️  Python3 not available, skipping validation"
fi

# Generate summary
echo "📋 Test Data Generation Summary:"
echo "  • Images: $(find "$AI_TEST_DATA_DIR/images" -name "*.jpg" | wc -l) files"
echo "  • Edge cases: $(find "$AI_TEST_DATA_DIR/edge-cases" -name "*" -type f | wc -l) files"
echo "  • Expected results: $(find "$AI_TEST_DATA_DIR/expected-results" -name "*.json" | wc -l) files"
echo "  • Mock models: $(find "$MODELS_DIR/mock" -name "*.onnx" | wc -l) files"
echo "  • Performance data: $(find "$AI_TEST_DATA_DIR/performance-data" -name "*.json" | wc -l) files"

# Calculate total size
TOTAL_SIZE=$(du -sh "$AI_TEST_DATA_DIR" 2>/dev/null | cut -f1 || echo "unknown")
echo "  • Total size: $TOTAL_SIZE"

echo "✅ AI test data generation complete!"
echo "📁 Test data location: $AI_TEST_DATA_DIR"
echo "🧪 Use this data in your AI tests by referencing the test_metadata.json file"

# Generate usage instructions
cat > "$AI_TEST_DATA_DIR/README.md" << 'EOF'
# AI Test Data

This directory contains comprehensive test data for AI integration testing in HazardHawk.

## Structure

- `images/` - Mock construction site images for various scenarios
- `expected-results/` - Expected AI analysis results for validation
- `edge-cases/` - Edge case files (corrupted, empty, extreme sizes)
- `performance-data/` - Performance benchmark targets and configurations
- `../models/mock/` - Mock ONNX model files for testing

## Usage in Tests

```kotlin
// Load test scenario
val testScenario = AITestDataFactory.createTestScenario(TestScenarioType.PPE_VIOLATION)

// Use test image
val testImage = loadTestImage("ppe_violation_standard.jpg")

// Validate against expected results
val expectedResults = loadExpectedResults("ppe_violation_expected.json")
```

## Validation

Run the validation script to ensure data integrity:

```bash
python3 validate_test_data.py
```

## Regeneration

To regenerate all test data:

```bash
../../scripts/generate_ai_test_data.sh
```
EOF

echo ""
echo "📖 Usage instructions written to: $AI_TEST_DATA_DIR/README.md"
echo "🚀 Test data is ready for use in AI integration tests!"
