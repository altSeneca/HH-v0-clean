# First Launch Camera Implementation

## Overview
HazardHawk now launches directly to the camera screen with an automatic first-launch setup dialog for company and project configuration.

## Implementation Summary

### 1. Direct Camera Launch
- **File**: `MainActivity.kt`
- **Change**: Modified `startDestination` from "home" to "camera" in NavHost
- Users now go directly to the camera screen when opening the app

### 2. First Launch Setup Dialog
- **File**: `FirstLaunchSetupDialog.kt` (new)
- **Features**:
  - Non-dismissible modal dialog on first launch
  - Company name input field
  - Project selection with quick presets or custom entry
  - Saves data using existing `MetadataSettingsManager`
  - Sets "first_launch_complete" flag in SharedPreferences

### 3. Camera Screen Integration
- **File**: `CameraScreen.kt`
- **Changes**:
  - Detects first launch via SharedPreferences check
  - Loads saved company and project from `MetadataSettingsManager`
  - Shows `FirstLaunchSetupDialog` on first launch
  - Updates metadata overlay with saved company name
  - Project changes are persisted to settings

### 4. Data Persistence
- **Utilizes**: Existing `MetadataSettingsManager`
- **Storage**: SharedPreferences with key "hazardhawk_metadata_settings"
- **Persisted Data**:
  - Company name (UserProfile.company)
  - Project name (ProjectInfo.projectName)
  - First launch completion flag

## User Flow

### First Launch
1. App opens directly to camera screen
2. First launch dialog appears automatically
3. User enters company name
4. User selects/enters project name
5. Dialog closes, camera is ready with metadata overlay showing entered info

### Subsequent Launches
1. App opens directly to camera screen
2. Camera is immediately ready
3. Last used company and project are pre-loaded in metadata overlay
4. User can change project via dropdown in top bar

## Key Features
- ✅ Camera opens automatically on launch
- ✅ First-time setup dialog for company/project
- ✅ Data persistence across app sessions
- ✅ Last project automatically selected on subsequent launches
- ✅ Company name saved from first launch
- ✅ Project changes are saved immediately

## Files Modified
1. `MainActivity.kt` - Changed navigation start destination
2. `CameraScreen.kt` - Added first launch detection and settings integration
3. `FirstLaunchSetupDialog.kt` - Created new setup dialog component

## Testing Recommendations
1. Test fresh install flow (first launch)
2. Verify data persistence after app restart
3. Test project switching and persistence
4. Verify metadata overlay shows correct company/project
5. Test permission handling flow
6. Validate that dialog is non-dismissible on first launch

## Future Enhancements
- Add user profile setup (name, role, etc.)
- Support multiple companies
- Project history/favorites
- Cloud sync for settings
- Offline project management