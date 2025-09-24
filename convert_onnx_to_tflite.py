#!/usr/bin/env python3
"""
Convert ONNX YOLOv8 model to TensorFlow Lite format for Android deployment
"""

import sys
import os
from pathlib import Path

def install_requirements():
    """Install required packages"""
    import subprocess
    packages = [
        'onnx',
        'onnx-tf', 
        'tensorflow',
        'ultralytics'
    ]
    
    for package in packages:
        try:
            subprocess.check_call([sys.executable, '-m', 'pip', 'install', package])
            print(f"✅ Installed {package}")
        except subprocess.CalledProcessError:
            print(f"❌ Failed to install {package}")
            
def convert_onnx_to_tflite():
    """Convert ONNX model to TensorFlow Lite"""
    try:
        import onnx
        import tensorflow as tf
        from onnx_tf.backend import prepare
        
        # Paths
        onnx_model_path = "models/yolo_hazard/hazard_detection_model.onnx"
        tflite_output_path = "HazardHawk/androidApp/src/main/assets/hazard_detection_model.tflite"
        
        if not os.path.exists(onnx_model_path):
            print(f"❌ ONNX model not found at {onnx_model_path}")
            return False
            
        print("🔄 Loading ONNX model...")
        onnx_model = onnx.load(onnx_model_path)
        
        print("🔄 Converting ONNX to TensorFlow...")
        tf_rep = prepare(onnx_model)
        
        print("🔄 Converting to TensorFlow Lite...")
        converter = tf.lite.TFLiteConverter.from_concrete_functions([tf_rep.export_graph()])
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]
        
        tflite_model = converter.convert()
        
        # Create output directory if it doesn't exist
        Path(tflite_output_path).parent.mkdir(parents=True, exist_ok=True)
        
        # Save the TensorFlow Lite model
        with open(tflite_output_path, 'wb') as f:
            f.write(tflite_model)
            
        print(f"✅ Successfully converted to {tflite_output_path}")
        print(f"📊 Model size: {len(tflite_model) / 1024 / 1024:.2f} MB")
        
        return True
        
    except ImportError as e:
        print(f"❌ Missing dependencies: {e}")
        print("Installing required packages...")
        install_requirements()
        return False
    except Exception as e:
        print(f"❌ Conversion failed: {e}")
        return False

if __name__ == "__main__":
    print("🚀 Converting ONNX YOLOv8 model to TensorFlow Lite...")
    
    if convert_onnx_to_tflite():
        print("✅ Conversion completed successfully!")
    else:
        print("❌ Conversion failed. Please check the logs above.")