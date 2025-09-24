#!/usr/bin/env python3
"""
Create Android app icons from SVG - using built-in libraries only
"""
import os
import shutil
from pathlib import Path

# Android icon sizes for different densities
DENSITIES = {
    'mdpi': 48,      # 1x
    'hdpi': 72,      # 1.5x  
    'xhdpi': 96,     # 2x
    'xxhdpi': 144,   # 3x
    'xxxhdpi': 192   # 4x
}

def create_svg_for_size(source_svg, output_svg, size):
    """Create an SVG file with specified dimensions"""
    try:
        with open(source_svg, 'r') as f:
            svg_content = f.read()
        
        # Replace the size attributes
        svg_content = svg_content.replace('width="192"', f'width="{size}"')
        svg_content = svg_content.replace('height="192"', f'height="{size}"')
        svg_content = svg_content.replace('viewBox="0 0 192 192"', f'viewBox="0 0 192 192"')  # Keep original viewBox
        
        with open(output_svg, 'w') as f:
            f.write(svg_content)
        
        print(f"✓ Created SVG: {output_svg} ({size}x{size})")
        return True
    except Exception as e:
        print(f"✗ Failed to create {output_svg}: {e}")
        return False

def main():
    print("🦅 Creating HazardHawk Android App Icons")
    
    # Paths
    source_svg = Path('superdesign/design_iterations/create_android_app_i_4.svg')
    android_res = Path('androidApp/src/main/res')
    
    if not source_svg.exists():
        print(f"❌ Source SVG not found: {source_svg}")
        return False
    
    print(f"📱 Source: {source_svg}")
    print(f"📂 Target: {android_res}")
    print()
    
    success_count = 0
    total_count = 0
    
    # Create icons for each density
    for density, size in DENSITIES.items():
        mipmap_dir = android_res / f'mipmap-{density}'
        
        print(f"📁 Processing {density} ({size}x{size})")
        
        # Check if directory exists
        if not mipmap_dir.exists():
            print(f"  ⚠️ Directory not found: {mipmap_dir}")
            continue
            
        # Create SVG files for this density (temporary)
        temp_svg = mipmap_dir / 'temp_icon.svg'
        
        if create_svg_for_size(str(source_svg), str(temp_svg), size):
            print(f"  📝 Created temporary SVG: {temp_svg}")
            
            # For now, we'll copy the SVG as a reference
            # In production, you'd convert this to PNG using external tools
            # Copy existing PNG and rename (placeholder approach)
            existing_png = mipmap_dir / 'ic_launcher.png'
            if existing_png.exists():
                # Backup original
                backup_png = mipmap_dir / 'ic_launcher_original_backup.png'
                if not backup_png.exists():
                    shutil.copy2(existing_png, backup_png)
                    print(f"  💾 Backed up original: {backup_png}")
            
            total_count += 1
            success_count += 1
            
            # Clean up temp SVG
            temp_svg.unlink()
            
        print()
    
    print(f"🎯 Icon preparation complete: {success_count}/{total_count} densities processed")
    print()
    print("📋 Next Steps:")
    print("1. Use online SVG to PNG converter or install image tools")
    print("2. Convert the SVG to PNG files for each density:")
    for density, size in DENSITIES.items():
        print(f"   • {density}: {size}x{size} pixels")
    print("3. Replace ic_launcher.png and ic_launcher_round.png in each mipmap folder")
    print("4. Build and test the app")
    print()
    print("🔗 Recommended online converter: https://convertio.co/svg-png/")
    print("📁 Source SVG: superdesign/design_iterations/create_android_app_i_4.svg")
    
    return True

if __name__ == '__main__':
    main()