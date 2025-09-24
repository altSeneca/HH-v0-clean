
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
