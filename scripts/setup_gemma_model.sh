#!/bin/bash
# HazardHawk - Gemma Model Setup Script
# Automates the process of converting and deploying Gemma models for local AI

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MODELS_DIR="$PROJECT_ROOT/models"
ANDROID_ASSETS_DIR="$PROJECT_ROOT/HazardHawk/androidApp/src/main/assets"
IOS_BUNDLE_DIR="$PROJECT_ROOT/HazardHawk/iosApp/HazardHawk"

# Default values
MODEL_NAME="google/gemma-2b"
OUTPUT_NAME="gemma2b_construction_safety"
SKIP_CONVERSION=false
SKIP_DEPLOYMENT=false
CREATE_ORT=true
VALIDATE_ONLY=false

# Print usage information
print_usage() {
    cat << EOF
HazardHawk Gemma Model Setup Script

Usage: $0 [OPTIONS]

Options:
    -m, --model MODEL_NAME      Model to convert (default: google/gemma-2b)
    -o, --output OUTPUT_NAME    Output model name (default: gemma2b_construction_safety)
    -s, --skip-conversion       Skip model conversion (use existing ONNX)
    -d, --skip-deployment       Skip deployment to app assets
    -n, --no-ort               Skip ORT format creation
    -v, --validate-only PATH    Only validate existing ONNX model
    -h, --help                  Show this help message

Examples:
    $0                          # Convert default gemma-2b model
    $0 -m google/gemma-7b       # Convert gemma-7b model
    $0 -s -d                    # Only validate existing model
    $0 -v models/existing.onnx  # Validate specific model

EOF
}

# Print colored output
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -m|--model)
                MODEL_NAME="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_NAME="$2"
                shift 2
                ;;
            -s|--skip-conversion)
                SKIP_CONVERSION=true
                shift
                ;;
            -d|--skip-deployment)
                SKIP_DEPLOYMENT=true
                shift
                ;;
            -n|--no-ort)
                CREATE_ORT=false
                shift
                ;;
            -v|--validate-only)
                VALIDATE_ONLY=true
                VALIDATE_PATH="$2"
                shift 2
                ;;
            -h|--help)
                print_usage
                exit 0
                ;;
            *)
                print_error "Unknown option: $1"
                print_usage
                exit 1
                ;;
        esac
    done
}

# Check system requirements
check_requirements() {
    print_info "Checking system requirements..."
    
    # Check Python
    if ! command -v python3 &> /dev/null; then
        print_error "Python 3 is required but not installed"
        exit 1
    fi
    
    # Check Python version
    python_version=$(python3 -c "import sys; print('.'.join(map(str, sys.version_info[:2])))")
    if [[ $(echo "$python_version 3.8" | tr " " "\n" | sort -V | head -n1) != "3.8" ]]; then
        print_error "Python 3.8+ is required (found $python_version)"
        exit 1
    fi
    
    print_success "Python $python_version found"
}

# Setup Python environment
setup_python_env() {
    print_info "Setting up Python environment..."
    
    # Create virtual environment if it doesn't exist
    if [[ ! -d "$SCRIPT_DIR/venv" ]]; then
        print_info "Creating virtual environment..."
        python3 -m venv "$SCRIPT_DIR/venv"
    fi
    
    # Activate virtual environment
    source "$SCRIPT_DIR/venv/bin/activate"
    
    # Upgrade pip
    pip install --upgrade pip
    
    # Install requirements
    if [[ -f "$SCRIPT_DIR/requirements-gemma-conversion.txt" ]]; then
        print_info "Installing Python dependencies..."
        pip install -r "$SCRIPT_DIR/requirements-gemma-conversion.txt"
    else
        print_error "Requirements file not found: requirements-gemma-conversion.txt"
        exit 1
    fi
    
    print_success "Python environment ready"
}

# Validate existing model
validate_model() {
    local model_path="$1"
    
    print_info "Validating ONNX model: $model_path"
    
    source "$SCRIPT_DIR/venv/bin/activate"
    python3 "$SCRIPT_DIR/convert_gemma_to_onnx.py" --validate-only "$model_path"
    
    if [[ $? -eq 0 ]]; then
        print_success "Model validation passed"
        return 0
    else
        print_error "Model validation failed"
        return 1
    fi
}

# Convert model to ONNX
convert_model() {
    print_info "Converting $MODEL_NAME to ONNX format..."
    
    # Create models directory
    mkdir -p "$MODELS_DIR"
    
    # Activate virtual environment
    source "$SCRIPT_DIR/venv/bin/activate"
    
    # Prepare conversion arguments
    local args=(
        --model "$MODEL_NAME"
        --output "$MODELS_DIR/${OUTPUT_NAME}.onnx"
        --cache-dir "$MODELS_DIR/cache"
    )
    
    if [[ "$CREATE_ORT" == true ]]; then
        args+=(--create-ort)
    fi
    
    # Run conversion
    python3 "$SCRIPT_DIR/convert_gemma_to_onnx.py" "${args[@]}"
    
    if [[ $? -eq 0 ]]; then
        print_success "Model conversion completed"
        
        # Show model information
        local model_path="$MODELS_DIR/${OUTPUT_NAME}.onnx"
        local model_size=$(du -h "$model_path" | cut -f1)
        print_info "Model size: $model_size"
        
        return 0
    else
        print_error "Model conversion failed"
        return 1
    fi
}

# Deploy model to application assets
deploy_model() {
    local onnx_path="$MODELS_DIR/${OUTPUT_NAME}.onnx"
    local ort_path="$MODELS_DIR/ort_models/${OUTPUT_NAME}.ort"
    
    print_info "Deploying model to application assets..."
    
    # Check if model exists
    if [[ ! -f "$onnx_path" ]]; then
        print_error "ONNX model not found: $onnx_path"
        return 1
    fi
    
    # Deploy to Android assets
    if [[ -d "$PROJECT_ROOT/HazardHawk/androidApp" ]]; then
        print_info "Deploying to Android assets..."
        mkdir -p "$ANDROID_ASSETS_DIR"
        
        # Copy ONNX model
        cp "$onnx_path" "$ANDROID_ASSETS_DIR/gemma_safety_model.onnx"
        
        # Copy ORT model if available
        if [[ -f "$ort_path" ]]; then
            cp "$ort_path" "$ANDROID_ASSETS_DIR/gemma_safety_model.ort"
        fi
        
        print_success "Android deployment completed"
    else
        print_warning "Android project not found, skipping Android deployment"
    fi
    
    # Deploy to iOS bundle
    if [[ -d "$PROJECT_ROOT/HazardHawk/iosApp" ]]; then
        print_info "Deploying to iOS bundle..."
        mkdir -p "$IOS_BUNDLE_DIR"
        
        # Copy ONNX model
        cp "$onnx_path" "$IOS_BUNDLE_DIR/gemma_safety_model.onnx"
        
        # Copy ORT model if available
        if [[ -f "$ort_path" ]]; then
            cp "$ort_path" "$IOS_BUNDLE_DIR/gemma_safety_model.ort"
        fi
        
        print_success "iOS deployment completed"
    else
        print_warning "iOS project not found, skipping iOS deployment"
    fi
    
    return 0
}

# Cleanup temporary files
cleanup() {
    print_info "Cleaning up temporary files..."
    
    # Clean model cache if it's too large (>5GB)
    local cache_dir="$MODELS_DIR/cache"
    if [[ -d "$cache_dir" ]]; then
        local cache_size=$(du -s "$cache_dir" | cut -f1)
        # Convert to GB (du returns KB)
        local cache_size_gb=$((cache_size / 1024 / 1024))
        
        if [[ $cache_size_gb -gt 5 ]]; then
            print_warning "Model cache is ${cache_size_gb}GB, cleaning up..."
            rm -rf "$cache_dir"
            print_success "Cache cleaned"
        fi
    fi
}

# Print final summary
print_summary() {
    print_success "🎉 HazardHawk Gemma Model Setup Complete!"
    echo
    print_info "Model Information:"
    echo "  • Model: $MODEL_NAME"
    echo "  • Output: $OUTPUT_NAME"
    echo "  • Location: $MODELS_DIR"
    echo
    print_info "Next Steps:"
    echo "  1. Build the HazardHawk app"
    echo "  2. Test AI analysis functionality"
    echo "  3. Validate construction safety detection accuracy"
    echo
    print_info "For testing, use:"
    echo "  ./gradlew :shared:testONNXGemmaAll"
}

# Main execution
main() {
    echo
    print_info "🏗️  HazardHawk Gemma Model Setup Starting..."
    echo
    
    # Parse command line arguments
    parse_args "$@"
    
    # Validation only mode
    if [[ "$VALIDATE_ONLY" == true ]]; then
        if [[ -z "$VALIDATE_PATH" ]]; then
            print_error "Validation path not provided"
            exit 1
        fi
        
        setup_python_env
        validate_model "$VALIDATE_PATH"
        exit $?
    fi
    
    # Full setup process
    check_requirements
    setup_python_env
    
    # Convert model if not skipping
    if [[ "$SKIP_CONVERSION" != true ]]; then
        convert_model
        if [[ $? -ne 0 ]]; then
            print_error "Setup failed during model conversion"
            exit 1
        fi
    fi
    
    # Deploy model if not skipping
    if [[ "$SKIP_DEPLOYMENT" != true ]]; then
        deploy_model
        if [[ $? -ne 0 ]]; then
            print_error "Setup failed during model deployment"
            exit 1
        fi
    fi
    
    # Cleanup
    cleanup
    
    # Print summary
    print_summary
}

# Handle script interruption
trap 'print_error "Setup interrupted"; exit 1' INT TERM

# Run main function
main "$@"