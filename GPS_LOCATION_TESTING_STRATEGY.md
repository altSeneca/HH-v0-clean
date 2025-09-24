# GPS Location Metadata Testing Strategy

## Problem Analysis
Current implementation uses `getLastKnownLocation()` which returns cached/stale location data, causing all photos to have identical GPS coordinates instead of capturing live device location.

## Root Cause
In `CameraManager.android.kt:314-336`, the `getCurrentLocation()` method only retrieves the last known location from the system, not the current live location. This is why all photos show the same coordinates.

## Testing Strategy Overview

### 1. Location Permission States
- **Granted**: Fine and coarse location permissions available
- **Denied**: No location permissions granted
- **Partially Granted**: Only coarse location granted
- **Runtime Permission Changes**: Permission revoked during capture

### 2. GPS Accuracy and Freshness
- **Fresh Location**: Location data < 30 seconds old
- **Stale Location**: Location data > 5 minutes old
- **High Accuracy**: GPS provider with accuracy < 10m
- **Low Accuracy**: Network provider with accuracy > 50m

### 3. EXIF Metadata Validation
- **Coordinate Precision**: Latitude/longitude to 6 decimal places
- **Altitude Data**: Available when GPS has altitude fix
- **Timestamp Synchronization**: Location timestamp matches photo timestamp
- **Direction Data**: Compass bearing if available

### 4. Location Provider Scenarios
- **GPS Only**: High accuracy, slower acquisition
- **Network Only**: Lower accuracy, faster acquisition
- **Fused Provider**: Best available location
- **Provider Switching**: GPS unavailable, fallback to network

### 5. Edge Cases
- **Location Unavailable**: No GPS signal, no network
- **Location Services Disabled**: System location services off
- **Mock Locations**: Development/testing with fake GPS
- **Rapid Movement**: Photo burst while device is moving
- **Indoor/Outdoor Transitions**: Signal loss scenarios

## Test Implementation Categories

### Unit Tests (Fast, Isolated)
- Location service integration logic
- GPS coordinate validation
- EXIF metadata parsing
- Permission state handling
- Location accuracy calculations

### Integration Tests (Medium Speed, Android Environment)
- Camera + Location workflow
- Permission request flow
- Provider switching behavior
- Background location updates
- Real device GPS hardware

### E2E Tests (Slow, Full System)
- Complete photo capture with GPS
- Gallery verification of location data
- Multi-photo location variation
- Permission UX flow testing
- Real-world usage scenarios

## Performance Requirements
- Location acquisition: < 5 seconds for first fix
- Photo capture with GPS: < 3 seconds total
- Background location updates: Every 10 seconds
- Battery impact: < 2% per hour of active use

## Success Criteria
1. Each photo captures current device GPS coordinates (not cached)
2. Location data varies when device moves between photos
3. EXIF metadata contains accurate latitude/longitude with proper precision
4. Location timestamps match photo capture time within 1 second
5. Graceful fallback when GPS unavailable
6. No performance degradation in photo capture speed
7. Proper permission handling and user experience

## Test Data Requirements
- Mock GPS coordinates for consistent testing
- Real-world test routes with known coordinates
- Indoor/outdoor test locations
- Areas with poor GPS signal
- Movement patterns for accuracy testing
