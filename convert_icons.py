#!/usr/bin/env python3
"""
Convert SVG to PNG icons for Android app
Requires: pip install pillow cairosvg
"""
import os
import sys
from pathlib import Path

# Check if required modules are available
try:
    import cairosvg
    from PIL import Image
    import io
except ImportError as e:
    print(f"Missing required module: {e}")
    print("Please install with: pip install pillow cairosvg")
    sys.exit(1)

# Android icon sizes for different densities
DENSITIES = {
    'mdpi': 48,      # 1x
    'hdpi': 72,      # 1.5x  
    'xhdpi': 96,     # 2x
    'xxhdpi': 144,   # 3x
    'xxxhdpi': 192   # 4x
}

def convert_svg_to_png(svg_path, output_path, size):
    """Convert SVG file to PNG with specified size"""
    try:
        # Convert SVG to PNG using cairosvg
        png_data = cairosvg.svg2png(url=svg_path, output_width=size, output_height=size)
        
        # Save PNG file
        with open(output_path, 'wb') as f:
            f.write(png_data)
        
        print(f"✓ Created {output_path} ({size}x{size})")
        return True
    except Exception as e:
        print(f"✗ Failed to create {output_path}: {e}")
        return False

def main():
    # Paths
    svg_file = Path('superdesign/design_iterations/create_android_app_i_4.svg')
    android_app_res = Path('HazardHawk/androidApp/src/main/res')
    
    if not svg_file.exists():
        print(f"SVG file not found: {svg_file}")
        return False
    
    print(f"🦅 Converting HazardHawk icon from {svg_file}")
    print(f"📱 Target: Android app in {android_app_res}")
    
    success_count = 0
    total_count = 0
    
    # Create icons for each density
    for density, size in DENSITIES.items():
        mipmap_dir = android_app_res / f'mipmap-{density}'
        mipmap_dir.mkdir(parents=True, exist_ok=True)
        
        # Create both square and round variants
        for variant in ['ic_launcher.png', 'ic_launcher_round.png']:
            output_path = mipmap_dir / variant
            total_count += 1
            
            if convert_svg_to_png(str(svg_file), str(output_path), size):
                success_count += 1
    
    print(f"\n🎯 Conversion complete: {success_count}/{total_count} icons created")
    
    if success_count == total_count:
        print("✅ All Android app icons successfully created!")
        return True
    else:
        print("⚠️ Some icons failed to convert")
        return False

if __name__ == '__main__':
    success = main()
    sys.exit(0 if success else 1)