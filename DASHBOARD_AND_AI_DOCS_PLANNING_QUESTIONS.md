# Dashboard and AI Safety Documentation - Planning Questions

**Date:** October 2, 2025
**Purpose:** Gather requirements for Dashboard Analytics and AI-Generated Safety Documentation features
**Target Release:** Q2-Q4 2026 per PRD

---

## Dashboard Analytics Questions

### 1. Data Sources & Metrics

**Current Data Collection:**
- What data is currently being collected/stored that can feed the dashboard?
- Are hazard detections being stored with timestamps, severity levels, OSHA codes?
- Do we have project-level data (project IDs, names, active users)?
- Are photo captures being logged with metadata (user, timestamp, project, analysis status)?

### 2. Dashboard User Experience (I'm less concerned with this right now and more concerned with the ease of navigation to the available functions like take photos, review photos in the gallery, generate pre task plans, generate job hazard analysis, generate checklists / peform an inspection [save, print, submit], etc. This app is to be the ultimate tool for safety professionals and supervisors making it easy for them to create the documentation they need, use it, submit it, store it, etc. I don't want to do all the storage on my end. I would rather users set up S3 accounts or integrate the documents with ProCore. If I have to do the storage then that will have to be factored into the pricing and at what point users have to pay more for storage based on how long the documentation is, assuming the documents are stored for the required amount of OSHA time.)

**Access & Presentation:**
- Which user tiers should see which dashboard features?
  - Field Access: Limited view? Photo capture & ai analysis, Create Pre Task Plans, Create Toolbox Talks, Create Incident Reports, Create JHA. I also want to add the ability to link a photo taken with an indentified hazard with the photo that shows the correction. There needs to be some workflow created to optimize the UX.
  - Safety Lead: Project-level analytics? All of the above plus Project-level Analytics; Human Identified Hazards, AI Identified Hazards, Hazards Mitigated, Hazards Outstanding, Incident Reports, etc. Also there needs to be an easy way to print documents to pdf, save them, share them or upload them especially for apps like ProCore.
  - Project Admin: Full analytics + multi-project views? Everything above
- Should the dashboard be the landing screen or accessible via navigation menu?
- Desktop/tablet optimized view for Project Admins reviewing data in office? Phone and tablet view obtimized for the Field Access. Also have a desktop / Tablet optimized view for project admin.
- Real-time updates or periodic refresh mechanism? Start with periodic refresh mechanism with the eventual goal to support real time updates.

### 3. Priority Metrics for MVP

**Metrics Selection:** Metrics are not a concern yet. Ability to generate, print, share, save, documentation is far more important at this point. In addition to hazard analysis of the photos.
- Of the listed metrics, which are highest priority for v1?
  - Hazards identified by type and severity
  - OSHA violations trends over time
  - Incident rate tracking
  - Project safety scores
  - Team compliance metrics
- Should we start with project-level or company-level aggregation?
- Historical trend timeframes: Last 7 days? 30 days? Custom date ranges?

### 4. Visualization Preferences

**Charts & Exports:**
- Preferred chart types: Bar charts, line graphs, pie charts, heat maps?
- Color coding consistent with existing severity levels (red/orange/yellow/green)?
- Export capabilities needed:
  - PDF reports? yes
  - CSV data export? yes
  - Scheduled email reports? yes

---

## AI-Generated Safety Documentation Questions

### 5. Document Types Priority

**Implementation Order:**
- The PRD shows all planned for Q2 2026:
  - Pre-Task Plans (PTPs)
  - Toolbox Talks Generator
  - Pre-Shift Meeting Tool
  - Incident Reports
- Which document type should be implemented first? all
- Are there existing templates/formats to follow? research the required OSHA 1926 information and create a rules document and types of documents / checklist required to be kept for construction projects. We will be creating the templates based on what is legally required first and foremost.
  - You mentioned 'All PTPs.docx' - is this available for reference? I don't rembember mentioning this document. It is not available.'
  - OSHA-compliant formats for other document types? All document generated should be osha-compliant formats.

### 6. AI Generation Scope

**AI Capabilities:**
- Should AI suggest hazards AND mitigations, or just hazards? Hazards and mitigations
- How much user editing should be allowed after AI generation? part of the AI generation should include a questionaire of type of work, tools, mechanical equipment, etc to pass as much info as possible to the ai for more accurate docs. The user should be able to request a change to the doc.
- Should AI learn from user modifications to improve future suggestions? yes.
- Confidence scoring for AI suggestions? do this internally, but users don't need to see this.

### 7. Document Workflow

**Creation & Approval:**
- Input method:
  - Forms with structured fields? forms with structured fields and checkboxes for things like tools used. type of work, steps, etc. 
  - Voice dictation support? yes
  - Photo-based generation (AI analyzes photo, generates document)? Yes this is done through Photo gallery right now.
- Review/approval workflow before finalizing? user should be able to review and approve before sending anything.
- Digital signatures:
  - Who signs what? (Crew members, supervisors, safety leads?) Supervisors and safety leads sign. I don't have a good way for every worker to digitally sign the pre task plan or job hazard analysis. The documents will be printed and signed by workers on site. then scanned in to a filing system.
  - Signature capture method (touch, stylus)? both
  - Multiple signature pages for large crews? 
- Storage strategy:
  - Local SQLDelight only?
  - Cloud backend sync?
  - Hybrid approach? Hybrid

### 8. PDF Generation

**Document Formatting:**
- Company branding elements:
  - Logo upload location in app? yes and web backend
  - Color scheme customization? later
  - Custom header/footer templates? later
- Standard format across all document types or unique per type? as much as possible
- Multi-page signatures:
  - Toolbox talks mention "optional extra signature page" Toolbox talk need a signature page
  - How many signatures per page? up to 20
  - Overflow handling for large crews?
- Photo embedding:
  - Full resolution or compressed thumbnails? full resolution
  - Maximum photos per document? up to 25
  - Photo placement (inline vs appendix)? inline. Should set up like an invisible table with the photo in one column and then all the metadata and ai analysis in a different column in the row.

### 9. Integration with Existing Features

**Cross-Feature Connections:**
- Can AI documentation reference specific photos from gallery with analysis results? Yes. AI analysis should be done per photo and the results saved with that photo in the database until deleted.
- Should hazards detected in photos auto-populate into PTPs/Incident Reports? No.
- Link between daily Pre-Shift Meetings and captured photos from that day? No.
- Automatic document suggestions:
  - "3 critical hazards detected today - create toolbox talk to address?" 
  - "5 workers in photo - generate AI hazard analysis?"

### 10. Offline/Online Requirements

**Connectivity Strategy:**
- Must AI document generation work offline, or acceptable to require connectivity? require connectivity
- Template library:
  - Stored locally in app? yes and companies may want to have templates stored in the backend.
  - Fetched from cloud on-demand?
  - Hybrid with local cache? This is likely best.
- Sync strategy for documents created offline? create one.
- Conflict resolution if same document edited on multiple devices? That should never happen. Only templates should be editable, and reports before being submitted. once submitted no editing.

---

## Additional Considerations

### Technical Architecture

**Backend Requirements:**
- Do we need separate microservices for document generation vs analytics? probably.
- PDF generation: Server-side or client-side (Android PDF APIs)? what makes the most sense and will create the most consistent results.
- Real-time analytics: WebSocket connections or polling? sure.

### Testing & Validation

**Quality Assurance:**
- OSHA compliance review process for AI-generated content? What does that mean?
- Legal review needed for incident reports? incident reports should capture as much detail as required by OSHA or by a company if they have stricter than OSHA policies.
- User acceptance criteria for document quality? yes. Users should review before saving, sharing, printing or otherwise. 
- Performance benchmarks for PDF generation (time, file size)?

### Migration & Rollout

**Phased Release:**
- Beta testing scope: Which user tier tests first? I'm testing it now as a field worker. optimize for that.'
- Gradual rollout by document type or all at once? Let's get Pre task plans working and then rollout the rest afterwards.'
- Training materials needed for each document type? Maybe information dialogs to walk users through. Use progessive discloure to minimize cognitive overload.
- Backwards compatibility with existing data? yes.

---

## Priority Recommendations

Based on PRD analysis, suggested focus order:

1. **Dashboard Analytics** (Q4 2026)
   - Start with basic project-level metrics
   - Hazard detection trends and OSHA violation tracking
   - Simple visualizations (bar/line charts)
   - Project Admin tier only initially

2. **Pre-Task Plans (PTPs)** (Q2 2026)
   - Most structured format, easiest to template
   - High safety impact (proactive hazard planning)
   - Leverages existing AI hazard detection
   - Safety Lead tier feature

3. **Incident Reports** (Q2 2026)
   - Critical for compliance
   - Photo attachment integration natural fit
   - OSHA reporting requirements

4. **Toolbox Talks** (Q2 2026)
   - Weekly cadence fits existing workflow
   - Template library can be pre-built
   - Signature collection workflow reusable

5. **Pre-Shift Meetings** (Q2 2026)
   - Daily frequency, simpler format
   - Weather/environmental integration later phase

---

## Next Steps

Please review and provide answers to prioritized questions:

**Immediate (for Dashboard MVP planning):**
- Questions 1-4 (Dashboard data, UX, metrics, visualization)

**Near-term (for AI Docs planning):**
- Questions 5-7 (Document priority, AI scope, workflow)

**Future (for detailed implementation):**
- Questions 8-10 (PDF formatting, integration, offline support)

---

**Document Owner:** Aaron Burroughs
**Created by:** Claude Code
**Status:** Awaiting stakeholder input
