#!/usr/bin/env python3
"""
HazardHawk - YOLOv8 Construction Hazard Detection Setup

Downloads, trains, and converts YOLOv8 models for construction safety detection.
Optimized for mobile deployment with TensorFlow Lite export.
"""

import argparse
import logging
import os
import sys
from pathlib import Path
from typing import List, Optional, Tuple
import requests
import zipfile

from ultralytics import YOLO
import torch
import cv2
import numpy as np
from PIL import Image

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

class HazardDetectionSetup:
    """Setup YOLOv8 models for construction hazard detection."""
    
    def __init__(self, models_dir: str = "./models/yolo_hazard"):
        self.models_dir = Path(models_dir)
        self.models_dir.mkdir(exist_ok=True, parents=True)
        
        # Construction safety classes we want to detect
        self.safety_classes = {
            'person': 0,
            'hard_hat': 1,
            'safety_vest': 2,
            'no_hard_hat': 3,
            'no_safety_vest': 4,
            'machinery': 5,
            'excavator': 6,
            'crane': 7,
            'truck': 8,
            'fall_hazard': 9,
            'electrical_hazard': 10,
            'safety_cone': 11,
            'barrier': 12
        }
        
    def download_pretrained_model(self, model_size: str = "n") -> str:
        """
        Download YOLOv8 pretrained model.
        
        Args:
            model_size: Model size (n, s, m, l, x)
        
        Returns:
            Path to downloaded model
        """
        model_name = f"yolov8{model_size}.pt"
        model_path = self.models_dir / model_name
        
        if model_path.exists():
            logger.info(f"Model {model_name} already exists")
            return str(model_path)
        
        logger.info(f"Downloading YOLOv8{model_size} model...")
        
        try:
            # YOLO will automatically download the model
            model = YOLO(model_name)
            
            # Move to our models directory
            default_path = Path.home() / ".cache/ultralytics" / model_name
            if default_path.exists():
                import shutil
                shutil.move(str(default_path), str(model_path))
            
            logger.info(f"✅ Downloaded model: {model_path}")
            return str(model_path)
            
        except Exception as e:
            logger.error(f"❌ Failed to download model: {str(e)}")
            raise
    
    def create_construction_dataset_config(self) -> str:
        """
        Create dataset configuration for construction safety.
        
        Returns:
            Path to dataset config file
        """
        config_content = f"""
# HazardHawk Construction Safety Dataset Configuration
path: {self.models_dir / "dataset"}
train: images/train
val: images/val
test: images/test

# Classes
nc: {len(self.safety_classes)}
names:
"""
        
        for class_name, class_id in self.safety_classes.items():
            config_content += f"  {class_id}: {class_name}\n"
        
        config_path = self.models_dir / "construction_safety.yaml"
        with open(config_path, 'w') as f:
            f.write(config_content)
            
        logger.info(f"✅ Created dataset config: {config_path}")
        return str(config_path)
    
    def download_sample_dataset(self) -> Optional[str]:
        """
        Download sample construction safety dataset.
        
        Returns:
            Path to dataset directory or None if failed
        """
        dataset_dir = self.models_dir / "dataset"
        
        if dataset_dir.exists() and any(dataset_dir.iterdir()):
            logger.info("Sample dataset already exists")
            return str(dataset_dir)
        
        logger.info("Creating sample dataset structure...")
        
        # Create directory structure
        for split in ['train', 'val', 'test']:
            for subdir in ['images', 'labels']:
                (dataset_dir / subdir / split).mkdir(parents=True, exist_ok=True)
        
        # Create sample data info
        readme_content = """
# Construction Safety Dataset

This directory should contain:
- images/train/ - Training images
- images/val/ - Validation images  
- images/test/ - Test images
- labels/train/ - Training labels (YOLO format)
- labels/val/ - Validation labels
- labels/test/ - Test labels

## YOLO Label Format
Each .txt file should contain:
class_id center_x center_y width height

Where coordinates are normalized (0-1).

## Recommended Datasets
1. Construction Site Safety Dataset (Roboflow)
2. Hard Hat Detection Dataset
3. PPE Detection Dataset
4. Custom construction site images with annotations

## Getting Started
1. Download construction safety images
2. Annotate using tools like Roboflow, LabelImg, or CVAT
3. Export in YOLO format
4. Place in appropriate directories
5. Run training with: python setup_yolo_hazard_detection.py --train
"""
        
        with open(dataset_dir / "README.md", 'w') as f:
            f.write(readme_content)
        
        logger.info(f"✅ Created dataset structure: {dataset_dir}")
        logger.warning("⚠️  Add your construction safety dataset to complete setup")
        
        return str(dataset_dir)
    
    def fine_tune_model(
        self, 
        base_model: str, 
        dataset_config: str,
        epochs: int = 50,
        batch_size: int = 16,
        img_size: int = 640
    ) -> str:
        """
        Fine-tune YOLOv8 model for construction hazard detection.
        
        Args:
            base_model: Path to base model
            dataset_config: Path to dataset YAML
            epochs: Training epochs
            batch_size: Batch size
            img_size: Image size
            
        Returns:
            Path to trained model
        """
        logger.info(f"Fine-tuning YOLOv8 for construction hazard detection...")
        
        try:
            # Load base model
            model = YOLO(base_model)
            
            # Create training directory
            train_dir = self.models_dir / "training"
            train_dir.mkdir(exist_ok=True)
            
            # Train model
            results = model.train(
                data=dataset_config,
                epochs=epochs,
                batch=batch_size,
                imgsz=img_size,
                project=str(train_dir),
                name="hazard_detection",
                save=True,
                save_period=10,
                val=True,
                plots=True,
                verbose=True,
                device='cpu'  # Use CPU for compatibility
            )
            
            # Get best model path
            best_model = train_dir / "hazard_detection" / "weights" / "best.pt"
            
            if best_model.exists():
                logger.info(f"✅ Training completed: {best_model}")
                return str(best_model)
            else:
                raise FileNotFoundError("Best model not found after training")
                
        except Exception as e:
            logger.error(f"❌ Training failed: {str(e)}")
            raise
    
    def export_to_mobile_formats(self, model_path: str) -> dict:
        """
        Export trained model to mobile-friendly formats.
        
        Args:
            model_path: Path to trained model
            
        Returns:
            Dictionary of exported model paths
        """
        logger.info("Exporting model to mobile formats...")
        
        try:
            model = YOLO(model_path)
            exported_models = {}
            
            # Export to ONNX
            logger.info("Exporting to ONNX...")
            onnx_path = model.export(
                format="onnx",
                optimize=True,
                simplify=True,
                dynamic=False,
                imgsz=640
            )
            exported_models['onnx'] = onnx_path
            logger.info(f"✅ ONNX export: {onnx_path}")
            
            # Export to TensorFlow Lite
            logger.info("Exporting to TensorFlow Lite...")
            try:
                tflite_path = model.export(
                    format="tflite",
                    imgsz=640,
                    int8=True  # Quantization for smaller size
                )
                exported_models['tflite'] = tflite_path
                logger.info(f"✅ TFLite export: {tflite_path}")
            except Exception as e:
                logger.warning(f"⚠️  TFLite export failed: {str(e)}")
            
            # Export to CoreML (for iOS)
            logger.info("Exporting to CoreML...")
            try:
                coreml_path = model.export(
                    format="coreml",
                    imgsz=640
                )
                exported_models['coreml'] = coreml_path
                logger.info(f"✅ CoreML export: {coreml_path}")
            except Exception as e:
                logger.warning(f"⚠️  CoreML export failed: {str(e)}")
            
            return exported_models
            
        except Exception as e:
            logger.error(f"❌ Export failed: {str(e)}")
            raise
    
    def validate_model(self, model_path: str, test_image: Optional[str] = None) -> bool:
        """
        Validate exported model with test inference.
        
        Args:
            model_path: Path to model
            test_image: Optional test image path
            
        Returns:
            True if validation passes
        """
        logger.info(f"Validating model: {model_path}")
        
        try:
            model = YOLO(model_path)
            
            if test_image and Path(test_image).exists():
                # Test with provided image
                results = model(test_image)
                logger.info(f"✅ Validation passed with test image")
                
                # Print detections
                for r in results:
                    if len(r.boxes) > 0:
                        logger.info(f"Detected {len(r.boxes)} objects")
                    else:
                        logger.info("No objects detected")
            else:
                # Test with dummy image
                dummy_image = np.random.randint(0, 255, (640, 640, 3), dtype=np.uint8)
                results = model(dummy_image)
                logger.info(f"✅ Validation passed with dummy image")
            
            return True
            
        except Exception as e:
            logger.error(f"❌ Validation failed: {str(e)}")
            return False
    
    def create_deployment_assets(self, exported_models: dict, output_dir: str):
        """
        Create deployment assets for HazardHawk app.
        
        Args:
            exported_models: Dictionary of exported model paths
            output_dir: Output directory for assets
        """
        logger.info("Creating deployment assets...")
        
        output_path = Path(output_dir)
        output_path.mkdir(exist_ok=True, parents=True)
        
        # Copy models with standard names
        for format_name, model_path in exported_models.items():
            if Path(model_path).exists():
                dest_name = f"hazard_detection_model.{format_name}"
                if format_name == 'tflite':
                    dest_name = "hazard_detection_model.tflite"
                elif format_name == 'coreml':
                    dest_name = "hazard_detection_model.mlmodel"
                
                dest_path = output_path / dest_name
                
                import shutil
                shutil.copy2(model_path, dest_path)
                logger.info(f"✅ Deployed {format_name}: {dest_path}")
        
        # Create class mapping file
        classes_file = output_path / "hazard_classes.json"
        import json
        with open(classes_file, 'w') as f:
            json.dump(self.safety_classes, f, indent=2)
        logger.info(f"✅ Created classes file: {classes_file}")
        
        # Create model info file
        model_info = {
            "model_name": "YOLOv8 Construction Hazard Detection",
            "version": "1.0.0",
            "input_size": [640, 640],
            "classes": self.safety_classes,
            "description": "YOLOv8 model fine-tuned for construction safety hazard detection",
            "formats": list(exported_models.keys())
        }
        
        info_file = output_path / "model_info.json"
        with open(info_file, 'w') as f:
            json.dump(model_info, f, indent=2)
        logger.info(f"✅ Created model info: {info_file}")

def main():
    parser = argparse.ArgumentParser(description="Setup YOLOv8 for construction hazard detection")
    parser.add_argument("--model-size", choices=['n', 's', 'm', 'l', 'x'], default='n', 
                       help="YOLOv8 model size")
    parser.add_argument("--models-dir", default="./models/yolo_hazard", 
                       help="Models directory")
    parser.add_argument("--train", action="store_true", 
                       help="Train model (requires dataset)")
    parser.add_argument("--epochs", type=int, default=50, 
                       help="Training epochs")
    parser.add_argument("--batch-size", type=int, default=16, 
                       help="Batch size")
    parser.add_argument("--export-only", type=str, 
                       help="Only export existing model to mobile formats")
    parser.add_argument("--deploy-to", type=str, 
                       help="Deploy to specific directory")
    
    args = parser.parse_args()
    
    try:
        setup = HazardDetectionSetup(args.models_dir)
        
        if args.export_only:
            # Export existing model only
            if not Path(args.export_only).exists():
                logger.error(f"Model not found: {args.export_only}")
                sys.exit(1)
            
            exported = setup.export_to_mobile_formats(args.export_only)
            
            if args.deploy_to:
                setup.create_deployment_assets(exported, args.deploy_to)
            
        else:
            # Full setup process
            logger.info("🏗️  Starting YOLOv8 Construction Hazard Detection Setup")
            
            # Download base model
            base_model = setup.download_pretrained_model(args.model_size)
            
            # Create dataset config
            dataset_config = setup.create_construction_dataset_config()
            
            # Download/create sample dataset
            dataset_dir = setup.download_sample_dataset()
            
            if args.train:
                # Check if dataset has images
                train_images = Path(dataset_dir) / "images" / "train"
                if not any(train_images.glob("*")):
                    logger.error("❌ No training images found. Add dataset first.")
                    logger.info("See dataset/README.md for instructions")
                    sys.exit(1)
                
                # Fine-tune model
                trained_model = setup.fine_tune_model(
                    base_model, 
                    dataset_config, 
                    args.epochs, 
                    args.batch_size
                )
                
                # Export trained model
                exported = setup.export_to_mobile_formats(trained_model)
            else:
                # Export base model for testing
                logger.info("Exporting base model for testing...")
                exported = setup.export_to_mobile_formats(base_model)
            
            # Validate exports
            for format_name, model_path in exported.items():
                if Path(model_path).exists():
                    setup.validate_model(model_path)
            
            # Deploy if requested
            if args.deploy_to:
                setup.create_deployment_assets(exported, args.deploy_to)
            
            logger.info("🎉 YOLOv8 Construction Hazard Detection Setup Complete!")
            
            if not args.train:
                logger.info("\n📋 Next Steps:")
                logger.info("1. Add construction safety dataset to dataset/ directory")
                logger.info("2. Run with --train to fine-tune the model")
                logger.info("3. Deploy to HazardHawk app assets")
        
    except Exception as e:
        logger.error(f"❌ Setup failed: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    main()