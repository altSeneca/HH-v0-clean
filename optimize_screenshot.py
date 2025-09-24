#!/usr/bin/env python3

from PIL import Image
import sys
import os

def optimize_image(input_path, output_path=None, quality=80, max_width=1568, max_height=1176):
    """
    Optimize an image by resizing and compressing it
    """
    if output_path is None:
        name, ext = os.path.splitext(input_path)
        output_path = f"{name}_claude_optimized{ext}"
    
    try:
        with Image.open(input_path) as img:
            # Get original dimensions
            original_width, original_height = img.size
            original_size = os.path.getsize(input_path)
            
            print(f"Original: {original_width}x{original_height}, {original_size/1024:.1f} KB")
            
            # Calculate new dimensions while maintaining aspect ratio
            if original_width > max_width or original_height > max_height:
                ratio = min(max_width / original_width, max_height / original_height)
                new_width = int(original_width * ratio)
                new_height = int(original_height * ratio)
                img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
                print(f"Resized to: {new_width}x{new_height}")
            
            # Convert to RGB if necessary
            if img.mode in ('RGBA', 'LA', 'P'):
                background = Image.new('RGB', img.size, (255, 255, 255))
                if img.mode == 'P':
                    img = img.convert('RGBA')
                background.paste(img, mask=img.split()[-1] if 'A' in img.mode else None)
                img = background
            
            # Save with optimization
            img.save(output_path, 'JPEG', quality=quality, optimize=True)
            
            # Show results
            optimized_size = os.path.getsize(output_path)
            compression_ratio = (original_size - optimized_size) / original_size * 100
            
            print(f"Optimized: {optimized_size/1024:.1f} KB")
            print(f"Compression: {compression_ratio:.1f}% reduction")
            print(f"Saved as: {output_path}")
            
            return output_path
            
    except Exception as e:
        print(f"Error optimizing image: {e}")
        return None

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python optimize_screenshot.py <image_path> [output_path] [quality]")
        sys.exit(1)
    
    input_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else None
    quality = int(sys.argv[3]) if len(sys.argv) > 3 else 80
    
    if not os.path.exists(input_path):
        print(f"Error: File {input_path} not found")
        sys.exit(1)
    
    optimize_image(input_path, output_path, quality)