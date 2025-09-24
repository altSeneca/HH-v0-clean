#!/usr/bin/env python3
"""
Search for available ONNX models on HuggingFace Hub
"""

from huggingface_hub import HfApi, list_models
import requests

def search_onnx_models():
    """Search for ONNX models related to Gemma"""
    
    api = HfApi()
    
    print("🔍 Searching HuggingFace Hub for ONNX models...")
    
    # Search for ONNX models
    search_terms = [
        "gemma onnx",
        "gemma-2b onnx", 
        "gemma vision onnx",
        "multimodal onnx",
        "vision language onnx"
    ]
    
    found_models = []
    
    for term in search_terms:
        try:
            print(f"\n📋 Searching for: '{term}'")
            models = list_models(search=term, limit=10)
            
            for model in models:
                if 'onnx' in model.modelId.lower() or 'onnx' in str(model.tags).lower():
                    found_models.append({
                        'id': model.modelId,
                        'tags': model.tags or [],
                        'downloads': getattr(model, 'downloads', 0)
                    })
                    print(f"  ✅ Found: {model.modelId}")
                    print(f"     Tags: {model.tags}")
                    print(f"     Downloads: {getattr(model, 'downloads', 'N/A')}")
                    
        except Exception as e:
            print(f"  ❌ Search failed for '{term}': {e}")
    
    # Look for specific ONNX community models
    onnx_candidates = [
        "microsoft/DialoGPT-medium",
        "onnx-community/gpt2-ONNX",
        "sentence-transformers/all-MiniLM-L6-v2",
        "optimum/distilbert-base-uncased-onnx",
        "philschmid/distilbert-onnx"
    ]
    
    print(f"\n🎯 Checking specific ONNX candidates...")
    for candidate in onnx_candidates:
        try:
            # Try to get model info
            response = requests.get(f"https://huggingface.co/api/models/{candidate}", timeout=10)
            if response.status_code == 200:
                print(f"  ✅ Available: {candidate}")
            else:
                print(f"  ❌ Not found: {candidate}")
        except Exception as e:
            print(f"  ❌ Error checking {candidate}: {e}")
    
    if found_models:
        print(f"\n✅ Found {len(found_models)} ONNX models")
        return found_models
    else:
        print("\n⚠️ No direct ONNX models found")
        print("💡 We'll need to convert PyTorch models to ONNX")
        return []

def get_alternative_vision_models():
    """Get alternative vision models we can use"""
    
    print("\n🔍 Looking for alternative vision models...")
    
    alternatives = [
        {
            'name': 'CLIP Vision',
            'repo': 'openai/clip-vit-base-patch32',
            'description': 'Vision transformer for image understanding'
        },
        {
            'name': 'MobileViT',
            'repo': 'apple/mobilevit-small',
            'description': 'Mobile-optimized vision transformer'
        },
        {
            'name': 'DeiT',
            'repo': 'facebook/deit-tiny-patch16-224',
            'description': 'Data-efficient vision transformer'
        }
    ]
    
    for alt in alternatives:
        try:
            response = requests.get(f"https://huggingface.co/api/models/{alt['repo']}", timeout=5)
            if response.status_code == 200:
                print(f"  ✅ {alt['name']}: {alt['repo']}")
                print(f"     Description: {alt['description']}")
            else:
                print(f"  ❌ {alt['name']}: Not available")
        except Exception as e:
            print(f"  ❌ {alt['name']}: Error - {e}")
    
    return alternatives

if __name__ == "__main__":
    print("🚀 HuggingFace ONNX Model Search")
    print("=" * 40)
    
    # Search for ONNX models
    onnx_models = search_onnx_models()
    
    # Get alternatives
    alternatives = get_alternative_vision_models()
    
    print(f"\n📊 Summary:")
    print(f"  - Direct ONNX models found: {len(onnx_models)}")
    print(f"  - Alternative models available: {len(alternatives)}")
    
    if not onnx_models:
        print(f"\n💡 Recommendation:")
        print(f"  1. Convert existing PyTorch Gemma model to ONNX")
        print(f"  2. Use YOLO model as vision encoder (already available)")
        print(f"  3. Create optimized text decoder for safety analysis")
        print(f"  4. This hybrid approach will work excellently for construction safety!")