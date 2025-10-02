# HazardHawk Safety Documentation Implementation Plan

**Date:** October 2, 2025
**Version:** 1.0
**Owner:** Aaron Burroughs
**Status:** Planning Phase

---

## Executive Summary

This plan outlines the implementation of AI-powered safety documentation features for HazardHawk, focusing on **Pre-Task Plans (PTPs)** as the initial rollout, followed by Job Hazard Analysis (JHA), Toolbox Talks, and Incident Reports. The goal is to create the ultimate tool for safety professionals and supervisors, making it easy to create, use, submit, and store OSHA-compliant documentation.

### Key Priorities

1. **Documentation First, Analytics Later** - Focus on generating, printing, sharing, and saving documents before building analytics dashboards
2. **Field Access Optimized** - Phone and tablet views optimized for field workers with progressive disclosure to minimize cognitive overload
3. **OSHA Compliance** - All documents based on legally required OSHA 1926 standards
4. **Storage Flexibility** - User-controlled storage (S3, ProCore integration) rather than proprietary storage
5. **Progressive Rollout** - Start with Pre-Task Plans, then expand to other document types

---

## Phase 1: OSHA Research & Requirements Definition

### OSHA 1926 Required Documentation Summary

Based on research of OSHA 1926 construction standards, the following documentation is either required or strongly recommended:

#### 1. **Job Hazard Analysis (JHA) / Pre-Task Plans (PTP)**
- **Regulatory Status:** Not explicitly mandated, but OSHA requires "Pre-Job Briefings" (1926.952) to recognize, discuss, and mitigate hazards before work begins
- **OSHA Recommendation:** "If it's not written down, it never happened" - documentation strongly recommended
- **Priority Jobs:** High injury/illness rates, severe/disabling injuries, new processes, human error-prone tasks
- **Required Elements:**
  - Work scope and job steps
  - Hazardous conditions identification
  - Root cause determination
  - Control measures and hazard elimination
  - PPE requirements
  - Emergency procedures

#### 2. **Toolbox Talks / Safety Meetings**
- **Regulatory Status:** Not specifically mandated by OSHA
- **Related Requirement:** OSHA 1926.21(b)(2) requires employers to "instruct each employee in the recognition and avoidance of unsafe conditions"
- **OSHA 1926.952:** Job briefing required before work begins covering hazards, procedures, precautions, controls, and PPE
- **Best Practice:** 15-minute safety talks daily or weekly before shift
- **Required Elements:**
  - Topic summary and discussion points
  - Hazards specific to the work
  - Key safety reminders
  - Worker acknowledgment (signatures)

#### 3. **Incident Reports (OSHA 300 Log System)**
- **Regulatory Status:** **REQUIRED** for employers with 10+ employees (OSHA Part 1904)
- **Forms Required:**
  - Form 300: Log of Work-Related Injuries and Illnesses
  - Form 300A: Summary (posted Feb 1 - April 30 annually)
  - Form 301: Injury and Illness Incident Report
- **Retention:** 5 years minimum
- **Immediate Reporting:**
  - Fatalities: 8 hours
  - Hospitalization, amputation, eye loss: 24 hours
- **Required Elements:**
  - Incident type and classification
  - Involved personnel and witnesses
  - Timeline of events
  - Actions taken and corrective measures
  - Photo documentation
  - OSHA references

#### 4. **Additional Required Programs (Context-Dependent)**
- Fall Protection Program (Subpart M 1926.500)
- Emergency Action Plans (1926.35)
- Trenching/Excavation Plans (1926.651-652)
- Lock-out/Tag-out Procedures (1926.417)
- Crane/Hoisting Inspection Programs
- Fire Protection Programs
- Accident Prevention Program (1926.20(b))

### HazardHawk Document Priority

Based on OSHA requirements and field needs:

1. **Pre-Task Plans (PTP) / Job Hazard Analysis** - Phase 1 (Immediate)
2. **Incident Reports** - Phase 1 (Immediate - legally required)
3. **Toolbox Talks** - Phase 2 (High value, not legally required)
4. **Daily Pre-Shift Briefings** - Phase 3 (Simplified toolbox talk)
5. **Specialized Programs** - Phase 4 (Fall protection, excavation, etc.)

---

## Phase 2: User Experience & Navigation Design

### Dashboard / Home Screen Concept

**Philosophy:** "Less, but Better" - Easy access to all functions without overwhelming the user

#### Navigation Structure

```
┌─────────────────────────────────────┐
│  ☰ HazardHawk        [Project ▼]    │  ← Top Bar
├─────────────────────────────────────┤
│                                     │
│  🎯 Quick Actions (Always Visible)  │
│  ┌─────────────────────────────┐   │
│  │  📸 Take Photo              │   │  ← Primary action
│  └─────────────────────────────┘   │
│                                     │
│  📋 Safety Documents                │
│  ┌─────┬─────┬─────┬─────┐         │
│  │ PTP │ JHA │Talk │Rept │         │  ← Document cards
│  └─────┴─────┴─────┴─────┘         │
│                                     │
│  📊 Recent Activity                 │
│  • 3 photos captured today          │
│  • 1 hazard identified (critical)   │
│  • PTP due for roofing work         │
│                                     │
├─────────────────────────────────────┤
│  [Camera] [Gallery] [Docs] [More]   │  ← Bottom Nav
└─────────────────────────────────────┘
```

#### Tier-Based Feature Access

**Field Access Tier:**
- ✅ Photo capture & AI analysis
- ✅ Create Pre-Task Plans
- ✅ Create Job Hazard Analysis
- ✅ Create Toolbox Talks
- ✅ Create Incident Reports
- ✅ Hazard correction workflow (before/after photo linking)
- ✅ View gallery and analysis results
- ✅ Print, save, share documents
- ❌ Project-level analytics

**Safety Lead Tier:**
- ✅ All Field Access features
- ✅ Project-level analytics:
  - Human-identified hazards
  - AI-identified hazards
  - Hazards mitigated vs outstanding
  - Incident report tracking
- ✅ Advanced document management
- ✅ ProCore integration for uploads
- ✅ Team hazard tracking

**Project Admin Tier:**
- ✅ All Safety Lead features
- ✅ Multi-project views
- ✅ Company-wide analytics
- ✅ User management
- ✅ Template customization
- ✅ Storage configuration (S3, ProCore)

### Hazard Correction Workflow (NEW FEATURE)

**User Story:** "I identified a hazard, took a photo, had it corrected, and need to document the fix"

#### Workflow Steps:

1. **Identify Hazard**
   - Capture photo → AI analysis detects violation
   - Hazard flagged as "Outstanding"

2. **Link Correction Photo**
   - Return to gallery → Select original hazard photo
   - Tap "Link Correction" button
   - Capture new photo showing fixed hazard
   - System auto-links photos with timestamps

3. **Verification & Closure**
   - AI analyzes correction photo
   - Compares before/after hazard status
   - User confirms hazard resolved
   - Status changed to "Mitigated"

4. **Documentation**
   - Before/after photos linked in database
   - Available for incident reports, PTPs, analytics
   - Export as PDF with correction timeline

#### Database Schema Addition:

```sql
CREATE TABLE hazard_corrections (
    id INTEGER PRIMARY KEY,
    original_photo_id INTEGER,  -- Photo showing hazard
    correction_photo_id INTEGER, -- Photo showing fix
    hazard_osha_code TEXT,
    date_identified TIMESTAMP,
    date_corrected TIMESTAMP,
    verified_by TEXT,
    notes TEXT,
    status TEXT CHECK(status IN ('Outstanding', 'In Progress', 'Mitigated')),
    FOREIGN KEY (original_photo_id) REFERENCES photos(id),
    FOREIGN KEY (correction_photo_id) REFERENCES photos(id)
);
```

---

## Phase 3: Pre-Task Plan (PTP) Implementation

### PTP Document Structure (OSHA-Compliant)

Based on OSHA 3071 Job Hazard Analysis guidelines:

#### Section 1: Project Information
- Company Name & Logo
- Project Name & Location
- Work Date & Time
- Supervisor/Safety Lead (digital signature)
- Crew Size & Members (printed, signed on-site)

#### Section 2: Work Scope Questionnaire

**AI-Assisted Input Form:**
- Work type (dropdown): Roofing, Electrical, Plumbing, Excavation, etc.
- Specific tasks (checkboxes + free text)
- Tools & equipment (checkboxes): Power tools, ladders, scaffolding, etc.
- Mechanical equipment (checkboxes): Forklifts, cranes, excavators, etc.
- Environmental conditions: Weather, confined space, heights, etc.
- Materials involved: Chemicals, heavy materials, sharp objects, etc.

**Progressive Disclosure:**
- Start with basic questions (5-7)
- AI suggests additional questions based on work type
- User can skip non-applicable sections

#### Section 3: AI-Generated Hazard Analysis

**Input to AI:**
- Work scope from questionnaire
- Selected photos from gallery (up to 25)
- Project history (previous hazards for similar work)

**AI Output:**
- Identified hazards with OSHA codes
- Severity levels (Critical, Major, Minor)
- Suggested mitigations and controls
- Required PPE
- Emergency procedures

**User Review:**
- Edit AI suggestions
- Add manual hazards
- Approve or request AI regeneration
- AI learns from user modifications

#### Section 4: Job Steps & Controls

**Table Format:**

| Job Step | Potential Hazards | Controls/Mitigations | PPE Required |
|----------|-------------------|----------------------|--------------|
| 1. Set up scaffolding | Fall from height (OSHA 1926.451) | Install guardrails, inspect scaffold, tie-off at 6ft+ | Hard hat, safety harness, steel-toed boots |
| 2. Electrical work | Electrocution (1926.416) | Lock-out/tag-out, test circuits, use insulated tools | Insulated gloves, safety glasses, arc-rated clothing |

- AI populates based on work scope
- User edits each row
- Add/remove steps as needed

#### Section 5: Photo Documentation

**Layout (Invisible Table):**

```
┌─────────────────────────────────────────────────┐
│ Photo 1: Worksite Overview                      │
│ ┌─────────────┐  ┌──────────────────────────┐   │
│ │             │  │ Location: Building 3, Roof │   │
│ │    PHOTO    │  │ Date: Oct 2, 2025 8:30am  │   │
│ │             │  │ GPS: 40.7128, -74.0060    │   │
│ │  (Full Res) │  │                           │   │
│ │             │  │ AI Analysis:              │   │
│ │             │  │ • Fall hazard detected    │   │
│ │             │  │ • No guardrails (CRITICAL)│   │
│ │             │  │ • Workers at 12ft height  │   │
│ └─────────────┘  └──────────────────────────┘   │
└─────────────────────────────────────────────────┘
```

- Up to 25 photos per document
- Full resolution embedding
- Metadata in adjacent column
- AI analysis summary per photo

#### Section 6: Emergency Contacts & Procedures
- Site emergency contacts
- Nearest hospital
- Evacuation routes
- Emergency equipment locations

#### Section 7: Signatures
- Supervisor/Safety Lead: Digital signature (touch/stylus)
- Crew acknowledgment: "Printed and signed on-site by crew"
- Date & time of briefing

### PTP Workflow

```
User Flow:
1. Dashboard → "Create PTP" button
2. Select work type (dropdown)
3. Answer questionnaire (5-10 questions, progressive disclosure)
4. Optional: Select photos from gallery (AI pre-filters relevant photos)
5. AI generates PTP (5-10 seconds)
6. Review & edit document
   - Modify hazards/mitigations
   - Add/remove job steps
   - Request AI regeneration with feedback
7. Add supervisor signature
8. Save/Print/Share:
   - Save to local storage (SQLDelight)
   - Print to PDF
   - Share via email/messaging
   - Upload to ProCore/S3
9. On-site: Print, crew signs, scan and attach
```

### AI Prompt Template for PTP Generation

```
You are an OSHA-certified construction safety expert. Generate a Pre-Task Plan (Job Hazard Analysis) based on the following information:

WORK DETAILS:
- Work Type: {work_type}
- Specific Tasks: {tasks}
- Tools: {tools}
- Equipment: {equipment}
- Environmental Conditions: {conditions}
- Materials: {materials}

PHOTO ANALYSIS:
{photo_analysis_results}

PROJECT HISTORY:
{previous_hazards_for_similar_work}

Generate a comprehensive PTP with:
1. All potential hazards with OSHA 1926 code references
2. Severity classification (Critical/Major/Minor)
3. Specific control measures and mitigations
4. Required PPE for each hazard
5. Job steps breakdown with hazards per step
6. Emergency procedures relevant to this work

Format as JSON following this schema:
{
  "hazards": [
    {
      "oshaCode": "1926.501(b)(1)",
      "description": "Fall hazard from heights greater than 6 feet",
      "severity": "CRITICAL",
      "controls": ["Install guardrails", "Use personal fall arrest system", "Inspect anchor points"],
      "requiredPPE": ["Safety harness", "Hard hat", "Steel-toed boots"]
    }
  ],
  "jobSteps": [
    {
      "step": 1,
      "description": "Set up scaffolding",
      "hazards": ["Fall from height", "Struck by falling objects"],
      "controls": ["Inspect scaffold components", "Install toe boards", "Secure tools"],
      "ppe": ["Hard hat", "Safety harness", "Steel-toed boots"]
    }
  ],
  "emergencyProcedures": ["Immediate supervisor notification", "Call 911 for serious injuries", "Activate emergency stop if equipment involved"],
  "requiredTraining": ["Fall protection certification", "Scaffold competent person"]
}
```

### Technical Implementation

#### New UI Components

1. **PTPCreationScreen.kt**
   - Work scope questionnaire
   - Photo selection from gallery
   - AI generation trigger
   - Review & edit interface

2. **PTPDocumentEditor.kt**
   - Editable hazard table
   - Job steps editor
   - Signature capture component
   - Photo attachment manager

3. **PTPPDFGenerator.kt**
   - Server-side generation (Go backend with PDF library)
   - Client-side fallback (Android PDF APIs)
   - Invisible table layout for photos
   - Company branding (logo from settings/backend)

4. **PTPViewModel.kt**
   - AI prompt construction
   - Gemini API integration
   - User modification tracking (for AI learning)
   - Document state management

#### Database Schema

```sql
-- Pre-Task Plans
CREATE TABLE pre_task_plans (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    project_id INTEGER,
    created_by TEXT,
    created_at TIMESTAMP,
    work_type TEXT,
    work_scope TEXT,
    status TEXT CHECK(status IN ('Draft', 'Approved', 'Submitted')),
    ai_generated_content TEXT,  -- JSON
    user_modified_content TEXT, -- JSON with edits
    pdf_path TEXT,
    cloud_storage_url TEXT,
    signature_supervisor_blob BLOB,
    signature_date TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(id)
);

-- PTP Photos (many-to-many)
CREATE TABLE ptp_photos (
    ptp_id INTEGER,
    photo_id INTEGER,
    display_order INTEGER,
    FOREIGN KEY (ptp_id) REFERENCES pre_task_plans(id),
    FOREIGN KEY (photo_id) REFERENCES photos(id),
    PRIMARY KEY (ptp_id, photo_id)
);

-- AI Learning: Track user modifications
CREATE TABLE ai_learning_feedback (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    document_type TEXT, -- 'PTP', 'JHA', 'IncidentReport', etc.
    work_type TEXT,
    ai_suggestion TEXT,
    user_modification TEXT,
    feedback_type TEXT CHECK(feedback_type IN ('Accept', 'Edit', 'Reject')),
    timestamp TIMESTAMP
);
```

---

## Phase 4: Storage & Integration

### Storage Strategy

**User-Controlled Storage (Priority 1):**

1. **AWS S3 Integration**
   - User provides their own S3 credentials
   - Bucket per project or company
   - App uploads PDFs to user's S3
   - No storage costs for HazardHawk
   - User manages retention policies

2. **ProCore Integration**
   - OAuth connection to ProCore account
   - Upload documents to project folders
   - Metadata sync (project, date, type)
   - Automatic filing in ProCore document management

3. **Local Storage Only**
   - Free tier option
   - Limited to device storage
   - Manual export/backup
   - No cloud sync

**Optional: HazardHawk-Managed Storage (Phase 2)**

- For users without S3/ProCore
- Tiered pricing based on storage duration
- OSHA 5-year retention compliance
- Storage tiers:
  - 90 days: Included in subscription
  - 1 year: +$10/month
  - 5 years (OSHA): +$25/month
  - Unlimited: +$50/month

### Integration Architecture

```
Mobile App (Android)
    ↓
Local SQLDelight DB
    ↓
[User Choice]
    ├→ AWS S3 (User's account)
    ├→ ProCore API (OAuth)
    ├→ HazardHawk Backend (Optional paid storage)
    └→ Local only (Free, no cloud)
```

### ProCore Integration Spec

**Endpoints Used:**
- POST `/rest/v1.0/documents` - Upload document
- GET `/rest/v1.0/projects/{id}/folders` - Get project folders
- POST `/rest/v1.0/folders` - Create safety folder if needed

**Document Metadata:**
```json
{
  "document": {
    "name": "Pre-Task Plan - Roofing Work - 2025-10-02.pdf",
    "description": "AI-generated PTP with 3 hazards identified",
    "folder_id": 12345,
    "private": false,
    "custom_fields": {
      "document_type": "Pre-Task Plan",
      "work_type": "Roofing",
      "hazard_count": 3,
      "ai_generated": true,
      "osha_codes": "1926.501, 1926.760"
    }
  }
}
```

**UI Flow:**
1. User connects ProCore in Settings
2. OAuth flow → Store access token securely
3. When saving PTP: "Upload to ProCore" toggle
4. Select project from ProCore project list
5. App uploads PDF + metadata
6. Confirmation: "Document uploaded to ProCore: Building 3 Roofing"

---

## Phase 5: Additional Document Types

### Job Hazard Analysis (JHA)

**Differences from PTP:**
- More detailed risk scoring (probability × severity)
- Formal approval workflow
- Regulatory compliance focus
- Less frequent updates (annual or when process changes)

**Implementation:** Extend PTP codebase with additional fields

### Toolbox Talks

**Structure:**
- Topic selection (100+ pre-built topics)
- Custom topic creation
- Discussion points (AI-suggested based on recent hazards)
- Worker attendance (names, no digital signatures)
- Signature page (up to 20 signatures per page, overflow to new pages)

**AI Prompt:**
```
Generate a 15-minute toolbox talk on "{topic}" for construction workers.

Recent project hazards:
{recent_hazards_from_photos}

Include:
1. Topic summary (2-3 sentences, 6th grade reading level)
2. 5-7 key discussion points
3. Real-world examples
4. Questions to ask the crew
5. Action items

Keep language simple and conversational.
```

### Incident Reports (OSHA 300/301)

**OSHA Form 301 Digital Version:**

**Required Fields (OSHA 301):**
- Employee information (name, DOB, hire date, job title)
- Incident date, time, location
- What was employee doing?
- What happened?
- Injury/illness description
- Body part affected
- Object/substance that harmed
- Physician/facility name
- Treated in emergency room? (Y/N)
- Hospitalized overnight? (Y/N)

**HazardHawk Enhancements:**
- Attach up to 25 photos from gallery
- AI analysis of incident scene
- Automatic OSHA code suggestions
- Witness statements (text input)
- Corrective actions taken
- Root cause analysis (5 Whys)

**Immediate Reporting Triggers:**
- Fatality → Alert to report within 8 hours
- Hospitalization/amputation/eye loss → Alert to report within 24 hours
- Auto-generate notification to safety lead

---

## Phase 6: Analytics Dashboard (Future - Q4 2026)

**Note:** Analytics are deprioritized per user feedback. Focus on document generation first.

**When implemented, Safety Lead tier will include:**

- Hazards identified (human vs AI)
- Hazards mitigated vs outstanding
- Incident report tracking
- Trend analysis (optional, later phase)

**Export capabilities:**
- PDF reports
- CSV data export
- Scheduled email reports

---

## Implementation Timeline

### Phase 1: Foundation (Weeks 1-2)
- ✅ Complete OSHA research
- ✅ Define document structures
- ⏳ Create OSHA compliance rules document
- ⏳ Design database schema updates
- ⏳ Design UI mockups for PTP workflow

### Phase 2: PTP MVP (Weeks 3-6)
- ⏳ Build PTPCreationScreen (questionnaire)
- ⏳ Integrate photo selection from gallery
- ⏳ Implement AI PTP generation (Gemini API)
- ⏳ Build PTPDocumentEditor (review & edit)
- ⏳ Signature capture component
- ⏳ PDF generation (server-side)
- ⏳ Local storage (SQLDelight)

### Phase 3: Storage Integration (Weeks 7-8)
- ⏳ AWS S3 credential management
- ⏳ ProCore OAuth & API integration
- ⏳ Upload workflows (S3, ProCore, local)
- ⏳ Settings UI for storage configuration

### Phase 4: Hazard Correction Workflow (Weeks 9-10)
- ⏳ Before/after photo linking
- ⏳ Hazard status tracking (Outstanding → Mitigated)
- ⏳ UI for linking correction photos
- ⏳ Correction timeline view

### Phase 5: Toolbox Talks (Weeks 11-12)
- ⏳ Template library (100+ topics)
- ⏳ AI talk generation
- ⏳ Signature page (20 signatures, overflow)
- ⏳ PDF generation

### Phase 6: Incident Reports (Weeks 13-14)
- ⏳ OSHA 301 form implementation
- ⏳ Photo attachment (up to 25)
- ⏳ Immediate reporting alerts
- ⏳ Witness statement collection

### Phase 7: Testing & Refinement (Weeks 15-16)
- ⏳ Field testing by Aaron (Field Access tier)
- ⏳ OSHA compliance review
- ⏳ Progressive disclosure improvements
- ⏳ Performance optimization (PDF generation time)

---

## Technical Specifications

### PDF Generation: Server-Side vs Client-Side

**Recommendation: Server-Side (Go Backend)**

**Pros:**
- Consistent rendering across devices
- No memory constraints (Android devices vary)
- Complex layouts easier (invisible tables for photos)
- Company logo/branding from backend
- Batch generation for multiple documents

**Cons:**
- Requires internet connectivity
- Backend dependency

**Implementation:**
- Go library: `gofpdf` or `gopdf`
- Endpoint: `POST /api/v1/documents/generate-pdf`
- Input: JSON (document type, content, photos as URLs)
- Output: PDF binary or S3 URL

**Client-Side Fallback:**
- Android `PdfDocument` API for offline mode
- Simplified layout (no complex tables)
- Lower quality logo rendering
- Stored locally only (no upload)

### Voice Dictation

**Android Speech-to-Text:**
- Google Speech Recognition API
- Used for:
  - Work scope description
  - Incident report "What happened?" field
  - Witness statements
  - Custom hazard descriptions

**UI Pattern:**
- Microphone icon next to text fields
- Real-time transcription display
- Edit transcribed text before saving
- Offline support via on-device model

### Progressive Disclosure Examples

**Questionnaire Flow:**

```
Step 1 (Always shown):
❓ What type of work are you doing?
   [Roofing ▼]

Step 2 (Always shown):
❓ How many workers on this job?
   [5]

Step 3 (Conditional - only if height work):
❓ Maximum working height?
   [12 feet]

Step 4 (Conditional - only if 6+ feet):
❓ Fall protection in place?
   ◯ Guardrails
   ◯ Personal fall arrest
   ◯ Safety nets
   ◯ None (CRITICAL WARNING shown)

Step 5 (AI-suggested based on work type):
❓ Are you working near power lines?
   [AI detected electrical hazard in photo - please confirm]
   ◯ Yes ◯ No
```

**Benefits:**
- Start simple (2-3 questions)
- Expand based on answers
- AI suggests relevant questions
- User never sees irrelevant fields
- Reduces cognitive overload

---

## Testing & Validation

### OSHA Compliance Validation

**Process:**
1. Generate sample documents for each type
2. Cross-reference with OSHA requirements:
   - Form 301 field mapping
   - 1926 code references accuracy
   - Recordkeeping retention rules
3. Consult with construction safety professional
4. Iterate based on feedback

**Acceptance Criteria:**
- All OSHA 301 required fields present
- Correct OSHA code format (1926.XXX)
- 5-year retention capability
- Immediate reporting triggers functional

### User Acceptance Testing

**Field Worker Perspective (Aaron's Testing):**
- [ ] Can create PTP in under 3 minutes
- [ ] AI suggestions are relevant and accurate
- [ ] Easy to edit AI-generated content
- [ ] PDF looks professional and OSHA-compliant
- [ ] Photo embedding works correctly
- [ ] Signature capture is smooth
- [ ] Print/share/upload works reliably

**Performance Benchmarks:**
- PTP creation: < 3 minutes total
- AI generation: < 10 seconds
- PDF generation: < 5 seconds (up to 10 photos)
- PDF file size: < 10MB (with 10 full-res photos)
- Upload to ProCore: < 10 seconds

---

## Risk Mitigation

### Risks & Mitigation Strategies

| Risk | Impact | Mitigation |
|------|--------|------------|
| AI generates incorrect OSHA codes | High | User review required before submission; AI confidence scoring; manual editing allowed |
| PDF generation fails on low-end devices | Medium | Server-side generation (primary); client-side fallback for offline |
| ProCore API changes break integration | Medium | Versioned API; graceful degradation; fallback to S3/local |
| User provides incorrect S3 credentials | Low | Credential validation before first upload; test upload functionality |
| Large PDFs (25 photos) take too long | Medium | Progressive upload (photos first, then PDF); compression options; pagination for >10 photos |
| Users don't review AI content | High | Mandatory review screen before saving; highlight AI-generated sections; require acknowledgment checkbox |

---

## Success Metrics

### Phase 1 (PTP Launch) - Success Criteria

**Adoption:**
- 80% of field workers create at least 1 PTP per week
- Average PTP creation time < 3 minutes
- 90% of users approve AI content with minor/no edits

**Quality:**
- OSHA compliance review: 0 critical issues
- User-reported inaccuracies: < 5% of generated content
- PDF generation success rate: > 99%

**Performance:**
- AI response time: < 10 seconds (p95)
- PDF generation: < 5 seconds (p95)
- ProCore upload success: > 98%

**User Satisfaction:**
- NPS score: > 50
- Feature request: "This saves me 30 minutes per day"
- Retention: 90% of users continue using after 30 days

---

## Next Steps

### Immediate Actions (This Week)

1. **Create OSHA Compliance Rules Document**
   - Detail all OSHA 1926 requirements
   - Map to HazardHawk features
   - Define validation rules

2. **Design Database Schema Updates**
   - Pre-Task Plans table
   - PTP Photos junction table
   - Hazard Corrections table
   - AI Learning Feedback table

3. **UI/UX Mockups**
   - PTP creation flow (Figma or hand-drawn)
   - Hazard correction linking flow
   - Storage configuration screens

4. **Backend Planning**
   - PDF generation service design
   - ProCore OAuth flow
   - S3 upload architecture

### Week 2 Deliverables

- [ ] OSHA compliance rules document
- [ ] Database schema implementation
- [ ] UI mockups approved
- [ ] Backend API spec defined
- [ ] Begin PTPCreationScreen development

---

## Questions for Aaron

1. **PTP Questionnaire:** Should the initial version have ~10 questions, or start simpler with 5 and expand based on AI suggestions? Let's start with a simpler 5 question and expand it later, ask the task, steps, tools used, working from height, etc.

2. **Photo Selection:** When creating a PTP, should the app auto-suggest recent photos from the current project, or require manual selection? No photos are not necesary for Pre Task Plans.

3. **Signature Workflow:** For supervisor digital signature, do you want a full signature pad (draw signature) or typed name + date? Let's offer both and we'll monitor usage to see what customer prefer.

4. **ProCore Folders:** Should the app auto-create a "HazardHawk Safety Docs" folder in ProCore, or let users choose existing folders? Auto create in the HazardHawk/Safety-Docs/ folder and Let users choose existing folders.

5. **Offline PTP Creation:** If no internet for AI generation, should the app:
   - Block creation until online, OR
   - Allow manual creation (skip AI), sync later? BLOCK CREATION. AI only works with internet access.

6. **Template Customization:** Should companies be able to create custom PTP templates (e.g., specific to their processes), or start with universal OSHA-based templates only? Start with universal OSHA-based templates, knowing that in the future companies will be able to create custom ptp template.

7. **Multi-Language Support:** Priority for Spanish translations? Construction industry has many Spanish-speaking workers. yes.

8. **Printing:** Is the goal for users to:
   - Print directly from phone (Bluetooth printer), OR
   - Share PDF to desktop/laptop for printing, OR
   - Both? both

---

**Document Status:** Ready for Review
**Next Review Date:** October 3, 2025
**Approvers:** Aaron Burroughs
