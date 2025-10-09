# NYC DOB Training Connect Integration Options

## Current Status: No Public API Available

The NYC Department of Buildings Training Connect system (https://dob-trainingconnect.cityofnewyork.us/) does **not provide a public API**. The website is a public portal for manual lookups only.

## What Was Built

The web application includes **placeholder infrastructure** for DOB integration in these files:
- `src/lib/api/dob.ts` - DOB API client (placeholder)
- `src/components/certifications/dob-verification.tsx` - DOB lookup UI
- `src/lib/hooks/use-dob-lookup.ts` - DOB lookup hook
- `src/types/api.ts` - DOB type definitions

All of these are **ready for integration** but currently non-functional without a real API.

## Option 1: Manual Verification (Recommended for MVP)

**How it works:**
1. Workers upload SST certifications through the web app
2. Admins review in verification dashboard
3. Admin opens NYC DOB website in separate tab: https://dob-trainingconnect.cityofnewyork.us/
4. Admin manually enters SST number to verify status
5. Admin approves/rejects in HazardHawk based on manual check

**Pros:**
- ✅ Legal and compliant
- ✅ No API access needed
- ✅ Can launch immediately
- ✅ Simple workflow

**Cons:**
- ❌ Manual process (slower)
- ❌ No auto-population
- ❌ Admins must visit two websites

**Implementation:**
- Remove DOB verification component from worker upload flow
- Add "Verify on NYC DOB" link to admin dashboard
- Update docs to recommend manual verification

## Option 2: Request Official API Access from NYC

**How to request:**
1. Contact NYC DOB directly
2. Email: dob@buildings.nyc.gov
3. Explain your use case (construction safety platform)
4. Request API access for SST verification
5. May require:
   - Business registration
   - Use case justification
   - Data protection agreement
   - IP whitelisting

**Timeline:**
- Could take 3-6 months (government processes)
- No guarantee of approval

**Pros:**
- ✅ Official and legal
- ✅ Reliable data
- ✅ Automatic verification

**Cons:**
- ❌ Long approval process
- ❌ May not be granted
- ❌ Possible usage fees
- ❌ Strict rate limits likely

## Option 3: Partner with Licensed Data Provider

Some construction industry data providers may have aggregated NYC DOB data.

**Potential partners:**
- Procore
- BuildingConnected
- NYC construction industry associations

**Pros:**
- ✅ Legal access to data
- ✅ API likely available
- ✅ Faster than government approval

**Cons:**
- ❌ Subscription/licensing fees
- ❌ May not have real-time data
- ❌ Still requires partnership approval

## Option 4: Hybrid Approach (Recommended)

**Phase 1 (Now):** Manual verification by admins
**Phase 2 (3-6 months):** Pursue official API access from NYC
**Phase 3 (Future):** Fully automated verification if API granted

**Worker upload flow:**
```
Worker uploads SST certification
  ↓
Worker manually enters SST number (optional)
  ↓
Admin reviews upload
  ↓
Admin sees SST number (if provided)
  ↓
Admin clicks "Verify on NYC DOB" → opens portal in new tab
  ↓
Admin manually checks status
  ↓
Admin approves/rejects in HazardHawk
```

## Recommended Implementation Changes

### 1. Remove DOB Auto-Lookup from Worker Flow

**File:** `src/components/certifications/upload-wizard.tsx`

Remove the `dob-verification` state from the flow:
```typescript
// Remove this state:
| { type: 'dob-verification'; sstNumber: string }

// Keep flow simple:
idle → camera/file → uploading → processing → ocr-review → manual-entry → submitting → success
```

### 2. Add Manual Verification Helper to Admin Dashboard

**File:** `src/app/admin/verify/page.tsx`

Add a "Verify SST on NYC DOB" button:
```typescript
{selectedCert?.certificationType === 'sst' && selectedCert.sstNumber && (
  <a
    href={`https://dob-trainingconnect.cityofnewyork.us/verify?sst=${selectedCert.sstNumber}`}
    target="_blank"
    rel="noopener noreferrer"
    className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg"
  >
    <ExternalLink className="w-4 h-4" />
    Verify SST on NYC DOB
  </a>
)}
```

### 3. Update Environment Variables

**File:** `.env.local`

Remove DOB API variables (not needed):
```bash
# REMOVE THESE (no API available):
# NEXT_PUBLIC_DOB_API_URL=...
# DOB_API_KEY=...
```

### 4. Add Manual Verification Instructions

**File:** `src/app/admin/verify/page.tsx`

Add info banner:
```typescript
{selectedCert?.certificationType === 'sst' && (
  <div className="bg-amber-50 border border-amber-200 rounded-lg p-4">
    <p className="text-sm text-amber-800">
      <strong>NYC SST Verification:</strong> Click "Verify on NYC DOB" to manually check this card's status on the official NYC portal.
    </p>
  </div>
)}
```

## What to Tell Your Team

### For Backend Team:
- **Do not implement** the DOB API endpoints
- Focus on core certification submission/approval API
- We'll handle SST verification manually in Phase 1

### For Frontend Team:
- Keep the DOB verification component code (for future use)
- Disable it in the upload flow for now
- Add manual verification helper links to admin dashboard

### For Product Team:
- SST verification will be manual initially
- Admins will use NYC DOB website directly
- Plan to pursue official API access as Phase 2
- Estimated 3-6 month timeline for API access (if granted)

### For Compliance Team:
- Manual verification is the legally compliant approach
- Do not scrape the NYC DOB website (ToS violation)
- Document the manual verification process for audits

## Updated Workflow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Worker Upload Flow                        │
└─────────────────────────────────────────────────────────────┘
Worker uploads SST certification (photo/PDF)
  ↓
OCR extracts SST number (if visible)
  ↓
Worker confirms or manually enters SST number
  ↓
Certification submitted for admin review

┌─────────────────────────────────────────────────────────────┐
│                  Admin Verification Flow                     │
└─────────────────────────────────────────────────────────────┘
Admin opens certification in verification dashboard
  ↓
Admin views uploaded document with OCR data
  ↓
Admin sees SST number (if SST certification)
  ↓
Admin clicks "Verify on NYC DOB" (opens in new tab)
  ↓
Admin manually enters SST number on NYC portal
  ↓
Admin verifies status: Active / Expired / Not Found
  ↓
Admin returns to HazardHawk dashboard
  ↓
Admin approves (if valid) or rejects (if invalid/expired)
```

## Future Enhancement: Official API (If Granted)

If NYC grants API access in the future, the existing code can be activated:

1. Update `.env.local` with real API credentials
2. Update `src/lib/api/dob.ts` with real endpoint
3. Re-enable DOB verification component
4. Add back to upload wizard flow
5. Test with real SST numbers

The infrastructure is already built and ready!

## Summary

**Current State:**
- ❌ No public API available from NYC DOB
- ✅ Manual verification is the only legal option
- ✅ Infrastructure built for future API integration

**Recommended Action:**
- Remove auto-lookup from worker flow
- Add manual verification helper to admin dashboard
- Document manual verification process
- Pursue official API access as long-term goal

**Timeline:**
- Phase 1 (Now): Manual verification - **Launch ready**
- Phase 2 (3-6 months): Apply for official API access
- Phase 3 (Future): Automated verification if approved

## Contact Information

**To request NYC DOB API access:**
- Website: https://www1.nyc.gov/site/dob/contact/contact.page
- Email: dob@buildings.nyc.gov
- Phone: (212) 393-2000
- Address: NYC Department of Buildings, 280 Broadway, New York, NY 10007

**Be prepared to provide:**
1. Business name and registration
2. Use case description (construction safety platform)
3. Expected API usage volume
4. Data protection/privacy measures
5. Technical contact information
