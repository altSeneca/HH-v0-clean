#!/usr/bin/env python3
"""
HazardHawk - Lightweight Gemma to ONNX Conversion Script

Uses memory-efficient approaches for smaller models or quantized versions.
"""

import argparse
import logging
import os
import sys
from pathlib import Path
import gc

import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, AutoConfig
from optimum.onnxruntime import ORTModelForCausalLM
from optimum.onnxruntime.configuration import ORTConfig

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def convert_lightweight_model(model_name: str, output_dir: str, cache_dir: str = "./model_cache"):
    """Convert model with aggressive memory optimization."""
    
    logger.info(f"Starting lightweight conversion of {model_name}")
    cache_path = Path(cache_dir)
    cache_path.mkdir(exist_ok=True)
    
    try:
        # Clear GPU cache if available
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
        
        # Force garbage collection
        gc.collect()
        
        logger.info("Loading tokenizer...")
        tokenizer = AutoTokenizer.from_pretrained(
            model_name,
            cache_dir=cache_dir,
            trust_remote_code=True
        )
        
        logger.info("Converting model to ONNX with aggressive optimizations...")
        
        # Use Optimum's ORTModelForCausalLM with maximum optimization
        ort_model = ORTModelForCausalLM.from_pretrained(
            model_name,
            export=True,
            cache_dir=cache_dir,
            use_cache=False,  # Disable KV cache
            trust_remote_code=True,
            torch_dtype=torch.float16,  # Half precision
            device_map="cpu",  # CPU only
            low_cpu_mem_usage=True,  # Memory optimization
            use_safetensors=True,  # Use safetensors format
        )
        
        logger.info("Saving converted model...")
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True, parents=True)
        
        ort_model.save_pretrained(str(output_path))
        tokenizer.save_pretrained(str(output_path))
        
        # Clean up memory
        del ort_model
        gc.collect()
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
            
        logger.info(f"✅ Conversion completed successfully: {output_path}")
        
        # Check model file
        onnx_file = output_path / "model.onnx"
        if onnx_file.exists():
            size_mb = onnx_file.stat().st_size / (1024 * 1024)
            logger.info(f"📏 Model size: {size_mb:.2f} MB")
            return True
        else:
            logger.error("❌ ONNX model file not found after conversion")
            return False
            
    except Exception as e:
        logger.error(f"❌ Conversion failed: {str(e)}")
        return False

def main():
    parser = argparse.ArgumentParser(description="Lightweight Gemma to ONNX conversion")
    parser.add_argument("--model", default="google/gemma-2b-it", help="Model to convert")
    parser.add_argument("--output-dir", default="./models/gemma_lightweight_onnx", help="Output directory")
    parser.add_argument("--cache-dir", default="./model_cache", help="Cache directory")
    
    args = parser.parse_args()
    
    success = convert_lightweight_model(args.model, args.output_dir, args.cache_dir)
    
    if success:
        logger.info("🎉 Lightweight conversion completed!")
        print(f"Model saved to: {args.output_dir}")
    else:
        logger.error("❌ Conversion failed")
        sys.exit(1)

if __name__ == "__main__":
    main()