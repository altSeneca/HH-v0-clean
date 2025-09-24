# HazardHawk Tag Management System

## Overview
Smart tagging system for construction safety photos that learns from usage patterns and adapts to project needs.

## Tag Hierarchy (Simple)

### 1. Quick Tags (Top 6-8 most used)
- PPE
- Fall Protection  
- Housekeeping
- Equipment
- Electrical
- Hot Work
- Crane/Lift
- +More

### 2. Compliance Status (Always visible)
- ✓ Compliant
- ⚠ Needs Improvement

### 3. Search/Custom (When needed)
- 🔍 Search or add tag...

## How Tags Work

### Storage & Transmission
- Tags embedded in photo EXIF metadata AND stored in local SQLite database
- When uploading: JSON payload includes photo + all tags + metadata
- AI receives context: "Photo tagged: PPE, Fall Protection, Compliant"
- AI can suggest additional tags based on what it sees

### Smart Tag Data Structure
```kotlin
data class Tag(
    val id: String,
    val name: String,
    val category: TagCategory, // SAFETY, EQUIPMENT, TRADE, CUSTOM
    val usageCount: Int,
    val lastUsed: DateTime,
    val projectSpecific: Boolean = false,
    val companyWide: Boolean = true,
    val oshaReferences: List<String> = emptyList()
)
```

## Three-Layer Tag System

### 1. Personal Layer (Learning)
- App learns YOUR most-used tags
- After 5 uses, tag gets promoted to your quick-access list
- Adapts to your current project (e.g., more "Steel Erection" tags on steel job)

### 2. Project Layer (Customizable)
- GC or Safety Manager can pre-load project-specific tags
- Example: "Michie Stadium" project always shows "Structural Steel", "Concrete", "Fall Protection"
- Imported via QR code or project code when joining

### 3. Master Layer (Industry Standard)
- Core safety tags based on OSHA Focus Four + common hazards
- Always available as fallback
- Updated quarterly based on all users' data (anonymized)

## User Experience Flow

### Taking a Photo
1. **Snap photo** → Overlay applied automatically

2. **Quick Dialog appears:**
   ```
   [✓ Compliant]  [⚠ Needs Improvement]
   
   Quick Tags:
   [PPE] [Fall Protection] [Housekeeping]
   [Equipment] [Electrical] [Scaffold]
   
   [+ Add specific tags]  [→ Skip & Save]
   ```

3. **Tap tags that apply** (multiple selection)

4. **Optional:** Tap "+" to search/add custom tag

5. **Photo saves** with all metadata

### Behind the Scenes
- Each tag selection updates usage counter
- Algorithm adjusts quick tags daily based on:
  - 40% your recent usage (last 7 days)
  - 30% project-wide usage
  - 30% standard safety tags

## AI Integration
- AI reads tags as "hints" for analysis
- If you tagged "PPE" + "Needs Improvement", AI focuses on PPE violations
- AI can auto-suggest missed tags: "Also detected: Missing fall arrest"

## Sync & Offline Capabilities
- Tags sync across your devices
- Offline mode: All tags available locally
- On sync: Merges tag usage stats, updates recommendations

## Future Enhancements to Consider
- **Severity levels:** High/Medium/Low risk classification
- **Location-based auto-tagging:** GPS triggers (e.g., near crane = suggest "Crane Ops")
- **Voice-to-tag:** Hands-free tagging for field use
- **Tag groups:** Bundle related tags (e.g., "Concrete Work" includes "Rebar", "Formwork", "Pour")
- **Regulatory mapping:** Direct link between tags and OSHA/building codes
- **Tag analytics:** Show which hazards are most common on your projects

## Implementation Priority
1. **Phase 1:** Basic tagging with compliance status and core safety tags
2. **Phase 2:** Learning algorithm and personal quick tags
3. **Phase 3:** Project-specific tags and team sharing
4. **Phase 4:** AI auto-suggestions and advanced features

## Technical Implementation Notes

### Local Storage (SQLDelight)
```sql
CREATE TABLE tags (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    usage_count INTEGER DEFAULT 0,
    last_used INTEGER,
    project_id TEXT,
    is_custom INTEGER DEFAULT 0
);

CREATE TABLE photo_tags (
    photo_id TEXT,
    tag_id TEXT,
    applied_at INTEGER,
    PRIMARY KEY (photo_id, tag_id)
);
```

### JSON Payload for Upload
```json
{
  "photo": {
    "id": "uuid",
    "timestamp": "2025-01-26T09:30:00Z",
    "location": {
      "lat": 40.7128,
      "lng": -74.0060,
      "address": "Michie Stadium, West Point, NY"
    },
    "metadata": {
      "project": "Michie Stadium Renovation",
      "user": "safety_manager_01",
      "device": "Android"
    }
  },
  "tags": [
    {
      "id": "ppe_001",
      "name": "PPE",
      "category": "SAFETY"
    },
    {
      "id": "compliance_good",
      "name": "Compliant",
      "category": "STATUS"
    }
  ],
  "ai_hints": {
    "focus_areas": ["PPE compliance", "Fall protection"],
    "context": "Steel erection work at 30ft elevation"
  }
}
```

## Design Principles
- **Simple:** Maximum 2 taps to tag a photo
- **Loveable:** Learns and adapts to user patterns
- **Complete:** Works offline, syncs seamlessly, integrates with AI