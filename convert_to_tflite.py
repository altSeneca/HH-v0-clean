#!/usr/bin/env python3
"""
Convert YOLOv8 model to TensorFlow Lite format for Android
"""

import tensorflow as tf
import numpy as np
from pathlib import Path

def create_dummy_yolo_tflite():
    """
    Create a dummy TensorFlow Lite model with the correct YOLOv8 structure
    This is a placeholder until we can properly convert the real model
    """
    
    # Create a simple model that mimics YOLOv8 structure
    input_shape = (1, 640, 640, 3)  # NHWC format for TensorFlow
    
    # Create a simple neural network
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(640, 640, 3)),
        tf.keras.layers.Conv2D(32, 3, activation='relu', padding='same'),
        tf.keras.layers.MaxPooling2D(),
        tf.keras.layers.Conv2D(64, 3, activation='relu', padding='same'),
        tf.keras.layers.MaxPooling2D(),
        tf.keras.layers.Conv2D(128, 3, activation='relu', padding='same'),
        tf.keras.layers.GlobalAveragePooling2D(),
        tf.keras.layers.Dense(256, activation='relu'),
        tf.keras.layers.Dense(84 * 20 * 20),  # Approximate YOLOv8 output size
        tf.keras.layers.Reshape((20, 20, 84))  # Similar to YOLOv8 output structure
    ])
    
    # Convert to TensorFlow Lite
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # Enable quantization for smaller model size
    converter.representative_dataset = representative_dataset_gen
    converter.target_spec.supported_types = [tf.float16]
    
    tflite_model = converter.convert()
    
    # Save the model
    output_path = Path("HazardHawk/androidApp/src/main/assets/hazard_detection_model.tflite")
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"✅ Created dummy TensorFlow Lite model at {output_path}")
    print(f"📊 Model size: {len(tflite_model) / 1024:.1f} KB")
    
    return str(output_path)

def representative_dataset_gen():
    """Generate representative dataset for quantization"""
    for _ in range(100):
        # Generate random images similar to camera input
        yield [np.random.rand(1, 640, 640, 3).astype(np.float32)]

def update_model_metadata():
    """Update model metadata files"""
    
    # Update classes
    classes = {
        "classes": [
            "person", "hard_hat", "safety_vest", "no_hard_hat", "no_safety_vest",
            "machinery", "excavator", "crane", "truck", "fall_hazard",
            "electrical_hazard", "safety_cone", "barrier"
        ]
    }
    
    classes_path = Path("HazardHawk/androidApp/src/main/assets/hazard_classes.json")
    import json
    with open(classes_path, 'w') as f:
        json.dump(classes, f, indent=2)
    
    # Update model info
    model_info = {
        "name": "HazardHawk Construction Safety Detector",
        "version": "1.0.0-dummy",
        "format": "tflite",
        "input_size": [640, 640],
        "num_classes": 13,
        "description": "Placeholder model for testing. Replace with trained construction safety model."
    }
    
    info_path = Path("HazardHawk/androidApp/src/main/assets/model_info.json")
    with open(info_path, 'w') as f:
        json.dump(model_info, f, indent=2)
    
    print(f"✅ Updated model metadata")

if __name__ == "__main__":
    print("🔧 Creating dummy TensorFlow Lite model for testing...")
    
    try:
        model_path = create_dummy_yolo_tflite()
        update_model_metadata()
        
        print("✅ Successfully created TensorFlow Lite model!")
        print("🔄 This is a dummy model for testing the integration.")
        print("📝 Replace with a trained construction safety model for production use.")
        
    except Exception as e:
        print(f"❌ Error creating model: {e}")