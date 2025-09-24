#!/usr/bin/env python3
"""
HazardHawk - Gemma to ONNX Model Conversion Script (Optimum-based)

Uses Hugging Face Optimum for better ONNX compatibility with Gemma models.
"""

import argparse
import logging
import os
import sys
from pathlib import Path
from typing import Optional

from optimum.onnxruntime import ORTModelForCausalLM
from transformers import AutoTokenizer, AutoConfig
import onnxruntime as ort

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class OptimumGemmaConverter:
    """Converts Gemma models to ONNX using Optimum library for better compatibility."""
    
    def __init__(self, model_name: str, cache_dir: str = "./model_cache"):
        self.model_name = model_name
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(exist_ok=True)
        
    def convert_and_save(self, output_dir: str) -> bool:
        """
        Convert model to ONNX using Optimum and save to directory.
        
        Args:
            output_dir: Directory to save the ONNX model
            
        Returns:
            True if successful, False otherwise
        """
        logger.info(f"Converting {self.model_name} to ONNX using Optimum")
        
        try:
            output_path = Path(output_dir)
            output_path.mkdir(exist_ok=True, parents=True)
            
            # Load and convert model using Optimum with memory optimizations
            logger.info("Loading and converting model (this may take several minutes)...")
            ort_model = ORTModelForCausalLM.from_pretrained(
                self.model_name,
                export=True,  # Export to ONNX
                cache_dir=str(self.cache_dir),
                use_cache=False,  # Disable KV cache for simpler ONNX model
                trust_remote_code=True,
                torch_dtype="float16",  # Use half precision to reduce memory
                device_map="cpu",  # Force CPU to avoid GPU memory issues
                low_cpu_mem_usage=True,  # Enable memory optimization
            )
            
            # Save the ONNX model
            ort_model.save_pretrained(str(output_path))
            
            # Also save the tokenizer
            tokenizer = AutoTokenizer.from_pretrained(
                self.model_name,
                cache_dir=str(self.cache_dir),
                trust_remote_code=True
            )
            tokenizer.save_pretrained(str(output_path))
            
            logger.info(f"✅ Model successfully converted and saved to: {output_path}")
            
            # Validate the converted model
            return self._validate_onnx_model(output_path / "model.onnx")
            
        except Exception as e:
            logger.error(f"❌ Conversion failed: {str(e)}")
            return False
    
    def _validate_onnx_model(self, onnx_path: Path) -> bool:
        """
        Validate the converted ONNX model.
        
        Args:
            onnx_path: Path to the ONNX model file
            
        Returns:
            True if validation passes, False otherwise
        """
        logger.info(f"Validating ONNX model: {onnx_path}")
        
        try:
            # Test with ONNX Runtime
            ort_session = ort.InferenceSession(
                str(onnx_path), 
                providers=['CPUExecutionProvider']
            )
            
            # Get input/output info
            inputs = ort_session.get_inputs()
            outputs = ort_session.get_outputs()
            
            logger.info(f"✅ ONNX model validation successful")
            logger.info(f"   Inputs: {[inp.name for inp in inputs]}")
            logger.info(f"   Outputs: {[out.name for out in outputs]}")
            
            return True
            
        except Exception as e:
            logger.error(f"❌ ONNX model validation failed: {str(e)}")
            return False

def main():
    """Main function to run the conversion process."""
    parser = argparse.ArgumentParser(description="Convert Gemma models to ONNX using Optimum")
    parser.add_argument(
        "--model", 
        type=str, 
        default="google/gemma-2b",
        help="Hugging Face model name to convert"
    )
    parser.add_argument(
        "--output-dir", 
        type=str, 
        default="./models/gemma2b_onnx",
        help="Output directory for ONNX model"
    )
    parser.add_argument(
        "--cache-dir", 
        type=str, 
        default="./model_cache",
        help="Model cache directory"
    )
    
    args = parser.parse_args()
    
    try:
        # Initialize converter
        converter = OptimumGemmaConverter(args.model, args.cache_dir)
        
        # Convert the model
        success = converter.convert_and_save(args.output_dir)
        
        if success:
            # Print model information
            output_path = Path(args.output_dir)
            model_file = output_path / "model.onnx"
            if model_file.exists():
                model_size = model_file.stat().st_size / (1024 * 1024)  # MB
                logger.info(f"🎉 Conversion completed successfully!")
                logger.info(f"📁 Model saved to: {args.output_dir}")
                logger.info(f"📏 Model size: {model_size:.2f} MB")
                logger.info(f"🚀 Ready for deployment in HazardHawk!")
            else:
                logger.warning(f"⚠️  Model file not found at expected location: {model_file}")
        else:
            logger.error(f"❌ Conversion failed")
            sys.exit(1)
        
    except Exception as e:
        logger.error(f"❌ Conversion failed: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()