#!/usr/bin/env python3
"""
HazardHawk - Gemma to ONNX Model Conversion Script

Converts Google Gemma models to ONNX format for cross-platform deployment
in the HazardHawk construction safety application.

Usage:
    python convert_gemma_to_onnx.py --model google/gemma-2b --output gemma2b_construction_safety.onnx
"""

import argparse
import logging
import os
import sys
from pathlib import Path
from typing import Optional, Tuple

import torch
from transformers import (
    AutoTokenizer, 
    AutoModelForCausalLM, 
    AutoConfig,
    GemmaForCausalLM
)
import onnx
import onnxruntime as ort
from onnxruntime.tools import convert_onnx_models_to_ort

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class GemmaToONNXConverter:
    """Converts Gemma models to ONNX format optimized for construction safety analysis."""
    
    def __init__(self, model_name: str, cache_dir: str = "./model_cache"):
        """
        Initialize the converter.
        
        Args:
            model_name: Hugging Face model name (e.g., "google/gemma-2b")
            cache_dir: Directory to cache downloaded models
        """
        self.model_name = model_name
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(exist_ok=True)
        
        self.tokenizer = None
        self.model = None
        self.config = None
        
    def load_model(self) -> None:
        """Load the Gemma model and tokenizer."""
        logger.info(f"Loading model: {self.model_name}")
        
        try:
            # Load tokenizer
            self.tokenizer = AutoTokenizer.from_pretrained(
                self.model_name,
                cache_dir=self.cache_dir,
                trust_remote_code=True
            )
            
            # Add padding token if not present
            if self.tokenizer.pad_token is None:
                self.tokenizer.pad_token = self.tokenizer.eos_token
            
            # Load configuration
            self.config = AutoConfig.from_pretrained(
                self.model_name,
                cache_dir=self.cache_dir,
                trust_remote_code=True
            )
            
            # Load model in evaluation mode
            self.model = AutoModelForCausalLM.from_pretrained(
                self.model_name,
                config=self.config,
                cache_dir=self.cache_dir,
                torch_dtype=torch.float32,  # Use float32 for better ONNX compatibility
                device_map="cpu",  # Keep on CPU for conversion
                trust_remote_code=True
            )
            
            self.model.eval()
            logger.info("Model loaded successfully")
            
        except Exception as e:
            logger.error(f"Failed to load model: {str(e)}")
            raise
    
    def create_construction_safety_prompt(self) -> str:
        """Create a construction safety analysis prompt for model preparation."""
        return """
        You are an OSHA-certified construction safety expert. Analyze construction site images for:
        1. PPE compliance (hard hats, safety vests, boots, gloves, eye protection)
        2. Fall protection hazards
        3. Electrical safety violations
        4. Equipment safety issues
        5. Site housekeeping problems
        
        Respond with JSON containing detected hazards, PPE compliance status, and safety recommendations.
        """
    
    def prepare_sample_inputs(self) -> Tuple[torch.Tensor, torch.Tensor]:
        """
        Prepare sample inputs for ONNX conversion.
        
        Returns:
            Tuple of (input_ids, attention_mask) tensors
        """
        logger.info("Preparing sample inputs for conversion")
        
        # Create construction safety prompt
        prompt = self.create_construction_safety_prompt()
        
        # Tokenize the prompt
        inputs = self.tokenizer(
            prompt,
            return_tensors="pt",
            padding=True,
            truncation=True,
            max_length=512  # Reasonable length for construction safety prompts
        )
        
        return inputs["input_ids"], inputs["attention_mask"]
    
    def convert_to_onnx(
        self, 
        output_path: str, 
        opset_version: int = 17,
        optimize_for_mobile: bool = True
    ) -> None:
        """
        Convert the model to ONNX format.
        
        Args:
            output_path: Path to save the ONNX model
            opset_version: ONNX opset version to use
            optimize_for_mobile: Whether to optimize for mobile deployment
        """
        logger.info(f"Converting model to ONNX format: {output_path}")
        
        if self.model is None:
            raise ValueError("Model not loaded. Call load_model() first.")
        
        try:
            # Prepare sample inputs
            input_ids, attention_mask = self.prepare_sample_inputs()
            
            # Define input and output names
            input_names = ["input_ids", "attention_mask"]
            output_names = ["logits"]
            
            # Dynamic axes for variable sequence length
            dynamic_axes = {
                "input_ids": {0: "batch_size", 1: "sequence_length"},
                "attention_mask": {0: "batch_size", 1: "sequence_length"},
                "logits": {0: "batch_size", 1: "sequence_length"}
            }
            
            # Convert to ONNX
            torch.onnx.export(
                self.model,
                (input_ids, attention_mask),
                output_path,
                export_params=True,
                opset_version=opset_version,
                do_constant_folding=True,
                input_names=input_names,
                output_names=output_names,
                dynamic_axes=dynamic_axes,
                verbose=False
            )
            
            logger.info(f"ONNX model saved to: {output_path}")
            
            # Optimize the model if requested
            if optimize_for_mobile:
                self._optimize_onnx_model(output_path)
            
        except Exception as e:
            logger.error(f"Failed to convert model to ONNX: {str(e)}")
            raise
    
    def _optimize_onnx_model(self, model_path: str) -> None:
        """
        Optimize ONNX model for mobile deployment using built-in optimizations.
        
        Args:
            model_path: Path to the ONNX model file
        """
        logger.info("Optimizing ONNX model for mobile deployment")
        
        try:
            # Load the ONNX model
            onnx_model = onnx.load(model_path)
            
            # Apply basic shape inference optimization
            onnx_model = onnx.shape_inference.infer_shapes(onnx_model)
            
            # Save optimized model
            optimized_path = model_path.replace('.onnx', '_optimized.onnx')
            onnx.save(onnx_model, optimized_path)
            
            # Replace original with optimized version
            os.replace(optimized_path, model_path)
            
            logger.info("Model optimization completed")
            
        except Exception as e:
            logger.warning(f"Model optimization failed: {str(e)}")
            # Continue without optimization if it fails
    
    def validate_onnx_model(self, onnx_path: str) -> bool:
        """
        Validate the converted ONNX model.
        
        Args:
            onnx_path: Path to the ONNX model file
            
        Returns:
            True if validation passes, False otherwise
        """
        logger.info(f"Validating ONNX model: {onnx_path}")
        
        try:
            # Load and check ONNX model
            onnx_model = onnx.load(onnx_path)
            onnx.checker.check_model(onnx_model)
            
            # Test with ONNX Runtime
            ort_session = ort.InferenceSession(onnx_path, providers=['CPUExecutionProvider'])
            
            # Prepare test inputs
            input_ids, attention_mask = self.prepare_sample_inputs()
            
            # Run inference
            ort_inputs = {
                "input_ids": input_ids.numpy(),
                "attention_mask": attention_mask.numpy()
            }
            
            ort_outputs = ort_session.run(None, ort_inputs)
            
            logger.info(f"ONNX model validation successful. Output shape: {ort_outputs[0].shape}")
            return True
            
        except Exception as e:
            logger.error(f"ONNX model validation failed: {str(e)}")
            return False
    
    def convert_to_ort_format(self, onnx_path: str, output_dir: str) -> str:
        """
        Convert ONNX model to ORT format for optimized mobile deployment.
        
        Args:
            onnx_path: Path to the ONNX model file
            output_dir: Directory to save the ORT model
            
        Returns:
            Path to the converted ORT model
        """
        logger.info(f"Converting ONNX model to ORT format")
        
        try:
            output_dir = Path(output_dir)
            output_dir.mkdir(exist_ok=True)
            
            # Convert to ORT format
            convert_onnx_models_to_ort(
                model_path=onnx_path,
                output_dir=str(output_dir),
                optimization_style='Fixed',
                target_device='mobile'
            )
            
            # Find the generated ORT file
            ort_files = list(output_dir.glob('*.ort'))
            if ort_files:
                ort_path = str(ort_files[0])
                logger.info(f"ORT model saved to: {ort_path}")
                return ort_path
            else:
                raise FileNotFoundError("ORT conversion did not produce expected output file")
                
        except Exception as e:
            logger.error(f"Failed to convert to ORT format: {str(e)}")
            raise

def main():
    """Main function to run the conversion process."""
    parser = argparse.ArgumentParser(description="Convert Gemma models to ONNX for HazardHawk")
    parser.add_argument(
        "--model", 
        type=str, 
        default="google/gemma-2b",
        help="Hugging Face model name to convert"
    )
    parser.add_argument(
        "--output", 
        type=str, 
        default="gemma2b_construction_safety.onnx",
        help="Output ONNX model path"
    )
    parser.add_argument(
        "--cache-dir", 
        type=str, 
        default="./model_cache",
        help="Model cache directory"
    )
    parser.add_argument(
        "--opset-version", 
        type=int, 
        default=17,
        help="ONNX opset version"
    )
    parser.add_argument(
        "--skip-optimization", 
        action="store_true",
        help="Skip model optimization for mobile"
    )
    parser.add_argument(
        "--create-ort", 
        action="store_true",
        help="Also create ORT format for mobile deployment"
    )
    parser.add_argument(
        "--validate-only", 
        type=str,
        help="Only validate an existing ONNX model (provide path)"
    )
    
    args = parser.parse_args()
    
    # Validation only mode
    if args.validate_only:
        converter = GemmaToONNXConverter("google/gemma-2b")  # Dummy for validation
        if converter.validate_onnx_model(args.validate_only):
            logger.info("✅ ONNX model validation passed")
            sys.exit(0)
        else:
            logger.error("❌ ONNX model validation failed")
            sys.exit(1)
    
    # Full conversion process
    try:
        # Initialize converter
        converter = GemmaToONNXConverter(args.model, args.cache_dir)
        
        # Load the model
        converter.load_model()
        
        # Convert to ONNX
        converter.convert_to_onnx(
            args.output, 
            args.opset_version, 
            optimize_for_mobile=not args.skip_optimization
        )
        
        # Validate the converted model
        if converter.validate_onnx_model(args.output):
            logger.info("✅ Model conversion and validation successful")
        else:
            logger.error("❌ Model validation failed")
            sys.exit(1)
        
        # Create ORT format if requested
        if args.create_ort:
            ort_dir = Path(args.output).parent / "ort_models"
            converter.convert_to_ort_format(args.output, str(ort_dir))
        
        # Print model information
        model_size = Path(args.output).stat().st_size / (1024 * 1024)  # MB
        logger.info(f"🎉 Conversion completed successfully!")
        logger.info(f"📁 Model saved to: {args.output}")
        logger.info(f"📏 Model size: {model_size:.2f} MB")
        logger.info(f"🚀 Ready for deployment in HazardHawk!")
        
    except Exception as e:
        logger.error(f"❌ Conversion failed: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()