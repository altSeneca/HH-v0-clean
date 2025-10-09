# HazardHawk Home Dashboard Redesign Plan

## Overview

Transform the home screen from a simple navigation menu into an intelligent **Safety Command Center** that provides clarity, control, and continuity for construction safety professionals.

## Design Philosophy

After the startup animation, users should instantly feel:
1. **Clarity** — they know exactly what this app does for them today
2. **Control** — key actions are one tap away
3. **Continuity** — progress from their last session is visible and actionable

---

## 1. Hero Bar - Welcome & Status Strip

**Purpose**: Reinforce identity and context immediately.

### Components

- **Personalized Greeting**: "Good morning, Alex — Site Safety Supervisor"
- **Project Selector**: Dropdown or breadcrumb for current jobsite
- **Live Site Conditions**: Weather, shift time, crew size
- **Date/Time Indicator**: Subtle timestamp with pulse animation

### Design Specifications

- **Height**: 80dp
- **Style**: Compact, pill-shaped banner with gradient background
- **Animation**: Shifting sky colors based on time of day (dawn orange → day blue → dusk purple)
- **Interactions**: Tap project name to switch jobsites

### Implementation Details

```kotlin
@Composable
fun HeroStatusBar(
    userName: String,
    userRole: String,
    currentProject: Project,
    liveConditions: SiteConditions,
    onProjectClick: () -> Unit
)
```

**Data Sources**:
- User profile from `UserProfileRepository`
- Current project from `ProjectManager`
- Weather from Weather API integration
- Time/shift from system clock

---

## 2. Command Center - Primary Action Buttons

**Purpose**: 90% of users will tap one of these within 10 seconds.

### Button Grid Layout

**2x3 Grid** with large touch targets (160dp height each):

| Row 1 | Button 1 | Button 2 |
|-------|----------|----------|
| | 📋 **Create Pre-Task Plan** | 🛠️ **Create Toolbox Talk** |
| **Row 2** | 📸 **Capture Photos** | 📊 **View Reports** |
| **Row 3** | 👥 **Assign Tasks/Crews** | 📁 **Photo Gallery** |

### Role-Based Visibility

**Field Access Tier**:
- ✅ Capture Photos
- ✅ Photo Gallery
- ❌ Create PTP (hidden)
- ❌ Create Toolbox Talk (hidden)
- ❌ Assign Tasks (hidden)
- ✅ View Reports (read-only)

**Safety Lead Tier**:
- ✅ All buttons visible
- ✅ Can generate PTPs and Toolbox Talks
- ✅ Can assign tasks
- ✅ Full report access

**Project Admin Tier**:
- ✅ All buttons visible
- ✅ Additional admin features
- ✅ Analytics dashboard access
- ✅ User management

### Design Specifications

- **Grid Spacing**: 12dp between cards
- **Card Height**: 140-160dp (ensures 60dp+ touch targets for gloved hands)
- **Corner Radius**: 16dp
- **Elevation**: 6dp with haptic feedback on press
- **Colors**:
  - PTP: Safety Orange (#FF6B35)
  - Toolbox Talk: Safety Green (#4CAF50)
  - Capture: OSHA Blue (#0066CC)
  - Reports: Material Purple (#9C27B0)
  - Tasks: Amber (#FFA500)
  - Gallery: Material Grey (#607D8B)

### Notification Badges

- Red badge with count for pending items
- Pulse animation for urgent items
- Position: Top-right corner of card

### Implementation Details

```kotlin
@Composable
fun CommandCenterGrid(
    userTier: UserTier,
    notifications: Map<String, Int>,
    onActionClick: (SafetyAction) -> Unit
)

enum class SafetyAction {
    CREATE_PTP,
    CREATE_TOOLBOX_TALK,
    CAPTURE_PHOTO,
    VIEW_REPORTS,
    ASSIGN_TASKS,
    OPEN_GALLERY
}
```

---

## 3. Recent Activity Feed

**Purpose**: Give continuity from the last session.

### Content Types

1. **PTPs Created/Signed**
   - Icon: 📋
   - Preview: PTP title + status badge (Draft/Approved/Active)
   - Actions: "View" | "Share" | "Export PDF"

2. **AI Hazard Detections**
   - Icon: ⚠️
   - Preview: Hazard type + severity (color-coded)
   - Severity colors:
     - 🔴 Critical (red)
     - 🟠 High (orange)
     - 🟡 Medium (yellow)
     - 🟢 Low (green)
   - Actions: "Review" | "Assign" | "Resolve"

3. **Toolbox Talks Completed**
   - Icon: 🛠️
   - Preview: Talk title + attendee count
   - Actions: "View" | "Download"

4. **Photos Awaiting Review**
   - Icon: 📸
   - Preview: Thumbnail + location + timestamp
   - Actions: "Analyze" | "Tag" | "Delete"

5. **System Alerts**
   - Icon: 🔔
   - Preview: Alert text (e.g., "New OSHA code update available")
   - Actions: "View" | "Dismiss"

### Design Specifications

- **Item Height**: 72dp each
- **Max Visible Items**: 10 (scrollable)
- **Refresh**: Pull-to-refresh gesture
- **Empty State**: "No recent activity. Start by capturing a photo."
- **Animations**: Fade-in sequence (50ms stagger per item)

### Implementation Details

```kotlin
@Composable
fun ActivityFeedList(
    activities: List<ActivityFeedItem>,
    onRefresh: () -> Unit,
    onActionClick: (ActivityFeedItem, ActionType) -> Unit
)

sealed class ActivityFeedItem {
    data class PTPActivity(val ptp: PTP, val status: PTPStatus) : ActivityFeedItem()
    data class HazardActivity(val hazard: Hazard, val severity: Severity) : ActivityFeedItem()
    data class ToolboxTalkActivity(val talk: ToolboxTalk, val attendees: Int) : ActivityFeedItem()
    data class PhotoActivity(val photo: Photo, val needsReview: Boolean) : ActivityFeedItem()
    data class SystemAlert(val message: String, val priority: Priority) : ActivityFeedItem()
}
```

**Data Sources**:
- `ActivityRepository` (new) - aggregates all activity types
- `PhotoRepository` - recent photos
- `PTPRepository` (new) - PTP activity
- System notifications service

---

## 4. Bottom Navigation Bar

**Purpose**: Persistent navigation across all major app sections.

### Navigation Items (Option A - 5 Items)

| Icon | Label | Route | Description |
|------|-------|-------|-------------|
| 🏠 | Home | `home` | Dashboard command center |
| 📸 | Capture | `camera` | Quick camera access |
| 📋 | Safety | `safety` | PTPs, Toolbox Talks, Incidents, Pre-Shift |
| 📁 | Gallery | `gallery` | Photo library |
| 👤 | Profile | `settings` | Settings, account, crew management |

### Design Specifications

- **Height**: 64dp
- **Style**: Material3 NavigationBar
- **Selected Indicator**: Pill-shaped with primary color
- **Badge Support**: Red dot for notifications
- **Animations**:
  - Pulse animation on badge
  - Ripple effect on tap
  - Smooth indicator transition (300ms)

### Notification Badges

- **Home**: Red badge if critical alerts exist
- **Capture**: None (always available)
- **Safety**: Badge count for pending PTPs/reports
- **Gallery**: Badge count for unreviewed photos
- **Profile**: Badge if settings require attention

### Screen Mapping

#### Included in Bottom Nav
- ✅ `home` - Dashboard
- ✅ `camera` / `clear_camera` - Camera capture
- ✅ `safety` - NEW hub screen (see section 6)
- ✅ `gallery` - Photo gallery
- ✅ `settings` - Profile & settings

#### Accessible But Not in Nav
- 🔗 `ar_camera` - Via camera mode toggle
- 🔗 `post_capture` - Modal after photo capture
- 🔗 `ptp/list`, `ptp/create`, `ptp/edit/{id}` - Via Safety hub
- 🔗 `company_project_entry` - Setup flow only
- 🔗 `storage_settings`, `ai_configuration` - Sub-settings
- 🔗 `live_detection` - Feature screen

### Implementation Details

```kotlin
@Composable
fun SafetyNavigationBar(
    currentRoute: String,
    notifications: NavigationNotifications,
    onNavigate: (String) -> Unit
)

data class NavigationNotifications(
    val homeAlerts: Int = 0,
    val safetyPending: Int = 0,
    val galleryUnreviewed: Int = 0,
    val profileAttention: Boolean = false
)
```

---

## 5. Startup Animation Sequence

**Purpose**: Professional, branded entry experience that leads smoothly into the command center.

### Timeline

| Time | Animation | Description |
|------|-----------|-------------|
| **0-1s** | Brand Intro | HazardHawk logo with "scanning site..." shimmer effect |
| **1-2s** | Hero Bar Entrance | Slide down from top with greeting fade-in |
| **2-3s** | Command Center | Buttons slide up in staggered sequence (50ms stagger) |
| **3-4s** | Activity Feed | List items fade in sequentially |
| **4s+** | Ready State | All elements visible, user can interact |

### Animation Specifications

- **Logo Animation**:
  - Scale from 0.8 to 1.0
  - Fade in from 0 to 1
  - Rotation shimmer effect (optional)
  - Duration: 800ms

- **Hero Bar**:
  - Slide from Y=-100 to Y=0
  - Spring animation with medium-low damping
  - Duration: 400ms

- **Command Center Buttons**:
  - Slide from Y=300 to Y=0
  - Stagger: 50ms per button
  - Spring animation
  - Duration per button: 300ms

- **Activity Feed**:
  - Fade in from alpha 0 to 1
  - Stagger: 30ms per item
  - Duration per item: 200ms

### Implementation Details

```kotlin
@Composable
fun StartupAnimationSequence(
    onComplete: () -> Unit
) {
    var stage by remember { mutableStateOf(AnimationStage.LOGO) }

    LaunchedEffect(Unit) {
        delay(1000)
        stage = AnimationStage.HERO_BAR
        delay(1000)
        stage = AnimationStage.COMMAND_CENTER
        delay(1000)
        stage = AnimationStage.ACTIVITY_FEED
        delay(1000)
        onComplete()
    }

    // Animation composables based on stage...
}

enum class AnimationStage {
    LOGO,
    HERO_BAR,
    COMMAND_CENTER,
    ACTIVITY_FEED,
    COMPLETE
}
```

---

## 6. New Safety Hub Screen

**Purpose**: Central hub for all safety documentation features.

### Screen: `SafetyHubScreen.kt`

#### Sections

1. **Pre-Task Plans (PTPs)**
   - Card showing active PTPs count
   - List of today's PTPs
   - "Create New PTP" button
   - Navigate to existing PTP screens

2. **Toolbox Talks** (To Be Built)
   - Card showing weekly talks
   - "Create Toolbox Talk" button
   - Template library
   - Attendance tracking

3. **Incident Reports** (To Be Built)
   - Card showing open incidents
   - "Report Incident" button (red, prominent)
   - Incident history
   - Follow-up tracking

4. **Pre-Shift Meetings** (To Be Built)
   - Card showing today's meeting status
   - "Start Pre-Shift Meeting" button
   - Attendance list
   - Meeting notes

#### Design

- Vertical scrolling list of feature cards
- Each card has:
  - Icon + title
  - Status indicator
  - Quick action button
  - "View All" link
- Empty states with "Coming Soon" badges for unbuilt features

#### Implementation

```kotlin
@Composable
fun SafetyHubScreen(
    onNavigateToPTP: () -> Unit,
    onNavigateToToolboxTalk: () -> Unit,
    onNavigateToIncidentReport: () -> Unit,
    onNavigateToPreShift: () -> Unit,
    onNavigateBack: () -> Unit
)
```

---

## 7. Files to Create

### New Composables

```
ui/home/components/
├── HeroStatusBar.kt
├── CommandCenterGrid.kt
├── CommandCenterButton.kt
├── ActivityFeedList.kt
├── ActivityFeedItem.kt
├── LiveConditionsWidget.kt
└── StartupAnimation.kt

ui/navigation/
└── SafetyNavigationBar.kt

ui/safety/
├── SafetyHubScreen.kt
└── SafetyFeatureCard.kt
```

### New Data Models

```
domain/models/
├── ActivityFeedItem.kt
├── SiteConditions.kt
├── NavigationNotifications.kt
└── SafetyAction.kt
```

### New ViewModels

```
ui/home/viewmodels/
├── DashboardViewModel.kt
├── ActivityFeedViewModel.kt
└── CommandCenterViewModel.kt
```

### New Repositories

```
data/repositories/
├── ActivityRepository.kt
├── UserProfileRepository.kt
└── WeatherRepository.kt
```

---

## 8. Files to Modify

### Major Changes

1. **`ui/home/HomeScreen.kt`**
   - Complete redesign
   - Replace existing components
   - Integrate new hero bar, command center, activity feed
   - Add startup animation

2. **`MainActivity.kt`**
   - Change `startDestination` from `"clear_camera"` to `"home"`
   - Update navigation flow
   - Remove company_project_entry redirect logic (handle in dashboard)
   - Add bottom navigation bar to NavHost

3. **`navigation/HazardHawkNavigation.kt`**
   - Add `SafetyNavigationBar` wrapper around NavHost
   - Add route for new `safety` hub screen
   - Update all screens to show/hide bottom nav appropriately

### Minor Changes

4. **All Screen Composables** (camera, gallery, settings, etc.)
   - Add `bottomBar` parameter to show navigation bar
   - Update back navigation to respect nav stack

---

## 9. Data Integration Requirements

### Required APIs/Services

1. **User Profile Service**
   - Get current user name, role, company
   - Get user tier (Field/Safety Lead/Admin)
   - Update last login time

2. **Project Management Service**
   - Get current project/jobsite
   - Switch between projects
   - Get crew member list
   - Get shift schedules

3. **Weather Service** (Optional)
   - Get current conditions for jobsite location
   - Temperature, precipitation, wind
   - Safety alerts (heat, cold, storms)

4. **Activity Aggregation Service** (New)
   - Fetch recent PTPs
   - Fetch recent hazard detections
   - Fetch recent photos
   - Fetch system notifications
   - Real-time updates via Flow

5. **Notification Service**
   - Badge counts for nav items
   - Push notifications
   - In-app alerts

### Repository Connections

```kotlin
// DashboardViewModel dependencies
class DashboardViewModel(
    private val userProfileRepository: UserProfileRepository,
    private val projectManager: ProjectManager,
    private val activityRepository: ActivityRepository,
    private val photoRepository: PhotoRepository,
    private val ptpRepository: PTPRepository, // When built
    private val weatherRepository: WeatherRepository,
    private val notificationService: NotificationService
) : ViewModel()
```

---

## 10. Implementation Phases

### Phase 1: Core Dashboard (Week 1)
- ✅ Hero Status Bar (without live conditions)
- ✅ Command Center Grid (basic 6 buttons)
- ✅ Bottom Navigation Bar
- ✅ Update MainActivity navigation
- ✅ Mock data for activity feed

### Phase 2: Data Integration (Week 2)
- ✅ Connect UserProfileRepository
- ✅ Connect PhotoRepository
- ✅ Connect PTPRepository (existing)
- ✅ Implement ActivityRepository
- ✅ Real-time activity feed

### Phase 3: Animations & Polish (Week 3)
- ✅ Startup animation sequence
- ✅ Hero bar animations (time-of-day colors)
- ✅ Button micro-interactions
- ✅ Activity feed transitions
- ✅ Pull-to-refresh

### Phase 4: Safety Hub (Week 4)
- ✅ Create SafetyHubScreen
- ✅ Integrate existing PTP navigation
- ✅ Add "Coming Soon" placeholders for Toolbox Talks, Incidents, Pre-Shift
- ✅ Wire up navigation from Command Center

### Phase 5: Advanced Features (Week 5+)
- ✅ Weather API integration
- ✅ Live site conditions widget
- ✅ Notification badges with real counts
- ✅ Role-based feature gating
- ✅ Analytics tracking

---

## 11. Design Assets Needed

### Icons
- Custom "Safety" icon for nav bar (shield with checkmark)
- Weather condition icons (sun, cloud, rain, snow, wind)
- Crew member avatars/placeholders
- Feature badges ("Coming Soon", "New", "Beta")

### Animations
- Logo shimmer/scan effect (Lottie file)
- Weather animation (optional - rain drops, snow, etc.)
- Notification pulse effect
- Loading skeletons for feed items

### Colors
- Time-of-day gradient colors:
  - Dawn: Orange to pink (#FF6B35 → #FF9A76)
  - Day: Blue to cyan (#0066CC → #4FC3F7)
  - Dusk: Purple to orange (#7B1FA2 → #FF6B35)
  - Night: Dark blue to navy (#1A237E → #0D47A1)

---

## 12. Testing Strategy

### Unit Tests
- ✅ DashboardViewModel state management
- ✅ ActivityRepository data aggregation
- ✅ Role-based button visibility logic
- ✅ Notification badge calculation

### UI Tests
- ✅ Bottom navigation switching
- ✅ Command center button clicks
- ✅ Activity feed item actions
- ✅ Pull-to-refresh behavior
- ✅ Startup animation sequence

### Integration Tests
- ✅ Navigation flow from dashboard to all screens
- ✅ Real-time activity updates
- ✅ Role-based feature access
- ✅ Notification delivery

### Manual Testing
- ✅ Test on construction site with gloves
- ✅ Test in bright sunlight (outdoor visibility)
- ✅ Test with slow network (activity feed loading)
- ✅ Test all user tiers (Field/Safety Lead/Admin)

---

## 13. Success Metrics

### User Engagement
- **Time to First Action**: < 5 seconds after app launch
- **Command Center Usage**: 90%+ of sessions use at least one button
- **Activity Feed Engagement**: 50%+ of users tap on feed items

### Performance
- **Startup Time**: < 2 seconds to fully interactive dashboard
- **Activity Feed Load**: < 1 second for 10 items
- **Navigation Transitions**: 60 FPS (no jank)

### User Satisfaction
- **Clarity**: Users can describe app purpose in 5 seconds
- **Control**: Users can reach any feature in ≤ 2 taps
- **Continuity**: Users can see their recent work immediately

---

## 14. Accessibility Considerations

### Touch Targets
- All buttons: Minimum 60dp × 60dp (for gloved hands)
- Spacing between buttons: Minimum 12dp

### Contrast
- All text: WCAG AA standard (4.5:1 for body text)
- High contrast mode support (future)

### Screen Readers
- All buttons have contentDescription
- Activity feed items have meaningful labels
- Navigation bar announces selected item

### Font Scaling
- Support up to 200% text scaling
- Layout adapts without clipping

---

## 15. Future Enhancements

### v3.2 Features
- Voice commands ("Take a photo", "Show recent PTPs")
- Widget for Android home screen (quick stats)
- Offline mode indicator in hero bar
- Dark mode with construction-optimized colors

### v3.3 Features
- Team collaboration (see what others are working on)
- Site map integration (show hazards on floor plan)
- Predictive alerts ("High heat expected at 2pm")
- Gamification (safety score leaderboard)

---

## Conclusion

This redesign transforms HazardHawk from a camera app into a comprehensive **Safety Command Center**. Users will immediately understand the app's purpose, have quick access to all critical functions, and see continuity from their previous work—all within the first 4 seconds of launch.

The modular design allows for incremental implementation while maintaining app functionality throughout development. The bottom navigation provides clear structure, and the command center puts the most important actions front and center.

**Result**: Users feel welcomed, informed, and ready to act.
