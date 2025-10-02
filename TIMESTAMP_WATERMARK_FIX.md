# Timestamp Watermark Fix

## Issue
Photos taken with ClearCamera were missing timestamp overlays in the watermark.

## Root Cause
The `createWatermarkLines()` method in `MetadataEmbedder.kt` was passing `timestamp = null` to the `createMetadataLines()` function, causing the timestamp to be omitted from the visual watermark.

## Solution

### 1. Fixed Timestamp Overlay (Line 663 in MetadataEmbedder.kt)
- Added timestamp formatting: `SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(metadata.timestamp))`
- Updated watermark lines to include timestamp between project name and location
- New watermark format:
  1. Company name
  2. Project name
  3. **Timestamp** (e.g., "2025-10-01 17:02:36") ← **NEW**
  4. GPS location (if enabled)
  5. Branding ("Taken with HazardHawk")

### 2. Batch Reprocessing Feature
Created utility to add timestamps to existing photos without watermarks:

#### New Files:
- **`BatchReprocessDialog.kt`**: UI dialog for batch reprocessing photos
- **`batchReprocessPhotosWithTimestamp()`** method in `MetadataEmbedder.kt`: Backend function to reprocess photos

#### Usage:
1. Open Photo Gallery
2. Long-press to select photos without timestamps
3. Tap the **Update icon** (⟳) in the top bar
4. Confirm in the dialog
5. Photos will be reprocessed with timestamp overlays added

#### Features:
- Extracts existing metadata from EXIF data
- Preserves all photo information (GPS, project, company)
- Shows progress during batch processing
- Success/failure reporting

## Files Modified

1. **`HazardHawk/androidApp/src/main/java/com/hazardhawk/camera/MetadataEmbedder.kt`**
   - Fixed `createWatermarkLines()` to include timestamp
   - Added `batchReprocessPhotosWithTimestamp()` method

2. **`HazardHawk/androidApp/src/main/java/com/hazardhawk/ui/gallery/PhotoGallery.kt`**
   - Added reprocess button to GalleryTopBar
   - Integrated BatchReprocessDialog

3. **`HazardHawk/androidApp/src/main/java/com/hazardhawk/ui/gallery/BatchReprocessDialog.kt`** (NEW)
   - Dialog UI for batch reprocessing

## Testing

### Test New Photos:
1. Take a new photo with ClearCamera
2. Check that timestamp appears in watermark (bottom-left overlay)
3. Format should be: "YYYY-MM-DD HH:MM:SS"

### Test Batch Reprocessing:
1. Navigate to Photo Gallery
2. Long-press to select photos without timestamps
3. Tap Update icon (⟳) in top bar
4. Confirm in dialog
5. Verify timestamp overlay is added to selected photos

## Next Steps

If you want to automatically reprocess ALL existing photos without timestamps, you can:

1. Select all photos in gallery (tap "All" button when in selection mode)
2. Tap the Update icon
3. Confirm batch reprocessing

The utility will extract the original capture time from EXIF metadata and add it to the visual overlay.
