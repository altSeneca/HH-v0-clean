# PTP PDF Condensed Layout Plan
**Date:** October 8, 2025 08:45:00
**Goal:** Reduce PTP from 7 pages to maximum 4 pages (2 sheets front/back)
**Priority:** HIGH - Field usability requirement

---

## 🎯 Design Objectives

1. **4-Page Maximum** - Fits on 2 sheets of paper (front/back printing)
2. **Field Readable** - Maintain readability in outdoor conditions
3. **Meeting Friendly** - Quick to review during pre-task meetings
4. **Signature Ready** - Easy for crew to sign off
5. **Professional** - Maintains safety credibility

---

## 📋 Current State Analysis (7 Pages)

### Page Breakdown:
- **Page 1:** Cover page with summary (mostly redundant)
- **Pages 2-4:** Detailed hazards with thick colored borders
- **Pages 5-6:** Job steps (verbose, single column)
- **Page 7:** Emergency procedures + signatures

### Space Wasters Identified:
1. ❌ Thick colored borders (4f width) around every hazard box
2. ❌ Redundant critical hazard summary on page 1
3. ❌ Single-column layout for controls (wastes 50% of width)
4. ❌ Large margins (40f top/bottom, 30f left/right)
5. ❌ Excessive line spacing (18f body text)
6. ❌ Verbose job step descriptions
7. ❌ Full OSHA code citations in controls (redundant with hazard header)

---

## 🔧 Space-Saving Strategies

### Strategy 1: Minimal Borders (Saves ~30% space on hazard pages)
**Current:**
```
┌─────────────────────────────────────────┐
│ ⚠ CRITICAL HAZARD          (4f border) │
│ 1926.501(b)(4)                         │
│                                         │
│ 📋 HAZARD: Fall hazard...              │
│                                         │
│ ✅ CONTROLS:                           │
│ 1. Control measure...                  │
│ 2. Control measure...                  │
└─────────────────────────────────────────┘
```

**Proposed:**
```
║ ⚠ CRITICAL 1926.501(b)(4)
║ Fall hazard through holes in walking/working surfaces
║ Controls: 1) Cover all floor holes (1926.502(i)) 2) Mark 'HOLE' or 'COVER'
```

- **Left edge only:** Colored bar (2f width) for severity indication
- **No box:** Remove top/bottom/right borders entirely
- **Inline layout:** Controls in numbered list format
- **Space saved:** ~40-50% vertical space per hazard

### Strategy 2: Two-Column Layout (Saves ~40% space)
**Apply to:**
- Hazard controls (split long lists into 2 columns)
- Job steps (hazards and controls side by side)
- Emergency procedures (compact format)

**Example:**
```
┌─────────────────────┬─────────────────────┐
│ HAZARDS:            │ CONTROLS:           │
│ • Falls from height │ • 100% tie-off      │
│ • Struck-by objects │ • Tool lanyards     │
│ • Electric shock    │ • Inspect equipment │
└─────────────────────┴─────────────────────┘
```

### Strategy 3: Condensed Typography
**Font Size Reductions:**
- TITLE: 22f → **18f** (-4pt)
- HEADING: 18f → **14f** (-4pt)
- BODY: 13f → **11f** (-2pt)
- SMALL: 11f → **9f** (-2pt)

**Line Spacing Reductions:**
- TITLE: 28f → **22f**
- HEADING: 24f → **18f**
- BODY: 18f → **14f**
- SMALL: 15f → **12f**

**Margin Reductions:**
- Left/Right: 30f → **20f**
- Top/Bottom: 40f → **25f**
- Section spacing: 20f → **12f**

### Strategy 4: Job Steps Table Format
**Current:** Each step takes ~150-200 vertical pixels
**Proposed:** Table format takes ~50-70 vertical pixels

| Step | Hazards | Controls | PPE |
|------|---------|----------|-----|
| 1. Pre-Task Planning | • Misunderstanding scope<br>• Equipment malfunction | • Conduct PTP meeting<br>• Inspect equipment | Hard hat, Safety glasses, Vest, Boots |
| 2. Receive Deliveries | • Struck-by equipment<br>• Falling materials | • Clear laydown area<br>• Certified operator | Hard hat, Gloves, Vest, Boots |

**Space saved:** ~60% reduction on job steps section

### Strategy 5: Remove Redundant Content
**Eliminate:**
1. ❌ "CRITICAL DANGERS" summary box on page 1 (duplicates pages 2-4)
2. ❌ Full OSHA citations in control measures (keep in hazard header only)
3. ❌ Verbose "What to do:" labels
4. ❌ Repeated competent person name in every job step

**Replace page 1 with:**
- Company/Project header (compact)
- Work type, crew size, location (1 line)
- Brief scope statement (2-3 lines)
- Jump directly to hazards

---

## 📐 New 4-Page Structure

### **Page 1: Project Info + Critical/Major Hazards**
```
┌────────────────────────────────────────┐
│ PRE-TASK SAFETY PLAN                   │ (Blue header, 14f)
│ Metro Walters | Regeneron | Tarrytown  │ (9f, gray)
├────────────────────────────────────────┤
│ Work: Steel Erection | Crew: 10        │ (Compact info block)
│ Scope: Receive deliveries, install...  │
├────────────────────────────────────────┤
│ IDENTIFIED HAZARDS                     │
│                                         │
│ ║ ⚠ CRITICAL 1926.501(b)(4)            │ (Red left edge, 2f)
│ ║ Fall hazard through holes...         │
│ ║ Controls: 1) Cover holes 2) Mark... │
│                                         │
│ ║ ⚡ MAJOR 1926.501(c)                 │ (Orange left edge)
│ ║ Struck-by falling objects...         │
│ ║ Controls: 1) Controlled access...   │
│                                         │
│ [... 3-4 more hazards ...]             │
└────────────────────────────────────────┘
```

**Content:** 4-5 CRITICAL/MAJOR hazards (most important ones)

### **Page 2: Remaining Hazards + Job Steps (Part 1)**
```
┌────────────────────────────────────────┐
│ HAZARDS (continued)                    │
│                                         │
│ ║ ℹ MINOR 1926.25(a) - Slips/trips    │
│ ║ Controls: 1) Clear walkways 2)...   │
│                                         │
├────────────────────────────────────────┤
│ JOB STEPS & CONTROLS                   │
│                                         │
│ ┌──┬─────────┬──────────┬──────────┐  │
│ │# │ Hazards │ Controls │ PPE      │  │
│ ├──┼─────────┼──────────┼──────────┤  │
│ │1 │• Scope  │• PTP mtg │ Hat,vest │  │
│ │  │• Inspect│• Inspect │ glasses  │  │
│ └──┴─────────┴──────────┴──────────┘  │
│                                         │
│ [... Steps 2-4 ...]                    │
└────────────────────────────────────────┘
```

**Content:** Minor hazards + first 3-4 job steps in table format

### **Page 3: Job Steps (Part 2) + Emergency Procedures**
```
┌────────────────────────────────────────┐
│ JOB STEPS (continued)                  │
│                                         │
│ ┌──┬─────────┬──────────┬──────────┐  │
│ │5 │• Falls  │• Harness │ PFAS,hat │  │
│ │6 │• Welding│• Inspect │ Helmet,  │  │
│ │7 │• Cleanup│• Remove  │ gloves   │  │
│ └──┴─────────┴──────────┴──────────┘  │
│                                         │
├────────────────────────────────────────┤
│ EMERGENCY PROCEDURES                   │
│                                         │
│ • Fall: Call 911, notify Jon Pariot    │
│ • Medical: Call 911, first aid         │
│ • Fire/Arc: Alert others, extinguish   │
│ • Struck-by: Assess scene, call 911    │
│ • Evacuation: Follow site routes       │
└────────────────────────────────────────┘
```

**Content:** Remaining job steps + condensed emergency procedures

### **Page 4: Signatures**
```
┌────────────────────────────────────────┐
│ SIGNATURES & APPROVAL                  │
│                                         │
│ All crew members must sign to          │
│ acknowledge review and understanding.  │
│                                         │
│ ┌─────────────┬──────┬─────────────┐  │
│ │ Name        │ Date │ Signature   │  │
│ ├─────────────┼──────┼─────────────┤  │
│ │ Worker 1    │      │             │  │
│ │ Worker 2    │      │             │  │
│ │ Worker 3    │      │             │  │
│ │ Worker 4    │      │             │  │
│ │ Worker 5    │      │             │  │
│ │ Worker 6    │      │             │  │
│ │ Worker 7    │      │             │  │
│ │ Worker 8    │      │             │  │
│ │ Worker 9    │      │             │  │
│ │ Worker 10   │      │             │  │
│ ├─────────────┴──────┴─────────────┤  │
│ │ Competent Person: Jon Pariot      │  │
│ │ Date: ____________                 │  │
│ │ Signature: _______________________│  │
│ └───────────────────────────────────┘  │
│                                         │
│ Footer: Regeneron PTP | Page 4 | Date │
└────────────────────────────────────────┘
```

**Content:** Signature table (3 columns) + competent person approval

---

## 🛠️ Implementation Steps

### Step 1: Update PDFLayoutConfig.kt
**File:** `shared/src/androidMain/kotlin/com/hazardhawk/documents/PDFLayoutConfig.kt`

**Changes:**
```kotlin
// Font sizes (reduce all by 2-4pt)
const val FONT_SIZE_TITLE = 18f      // was 22f
const val FONT_SIZE_HEADING = 14f    // was 18f
const val FONT_SIZE_BODY = 11f       // was 13f
const val FONT_SIZE_SMALL = 9f       // was 11f

// Line spacing (reduce proportionally)
const val LINE_SPACING_TITLE = 22f   // was 28f
const val LINE_SPACING_HEADING = 18f // was 24f
const val LINE_SPACING_BODY = 14f    // was 18f
const val LINE_SPACING_SMALL = 12f   // was 15f

// Margins (reduce for compact layout)
const val MARGIN_LEFT = 20f          // was 30f
const val MARGIN_RIGHT = 20f         // was 30f
const val MARGIN_TOP = 25f           // was 40f
const val MARGIN_BOTTOM = 25f        // was 40f

// Section spacing
const val SECTION_SPACING = 12f      // was 20f
const val SUBSECTION_SPACING = 8f    // was 12f

// Hazard box styling (minimal borders)
const val HAZARD_LEFT_EDGE_WIDTH = 2f  // was 4f (full border)
const val HAZARD_BOX_PADDING = 6f      // was 10f
const val HAZARD_VERTICAL_SPACING = 8f // was 15f

// Remove these (no longer needed):
// - HAZARD_BOX_BORDER_WIDTH
// - Full box drawing logic
```

### Step 2: Modify AndroidPTPPDFGenerator.kt - Hazard Drawing
**File:** `shared/src/androidMain/kotlin/com/hazardhawk/documents/AndroidPTPPDFGenerator.kt`

**Method:** `drawHazardBox()` (lines ~506-600)

**Replace with minimal left-edge design:**
```kotlin
private fun drawHazardMinimal(
    canvas: Canvas,
    hazard: PtpHazard,
    y: Float,
    maxY: Float
): Float {
    var currentY = y

    // Draw colored left edge only (2f width)
    val edgeColor = when (hazard.severity) {
        HazardSeverity.CRITICAL -> COLOR_CRITICAL
        HazardSeverity.MAJOR -> COLOR_MAJOR
        HazardSeverity.MINOR -> COLOR_MINOR
    }

    val edgePaint = Paint().apply {
        color = edgeColor
        style = Paint.Style.FILL
    }

    canvas.drawRect(
        MARGIN_LEFT,
        currentY,
        MARGIN_LEFT + 2f,
        currentY + 60f, // Approximate height, will adjust
        edgePaint
    )

    // Draw severity icon + OSHA code (inline, single line)
    val icon = when (hazard.severity) {
        HazardSeverity.CRITICAL -> "⚠"
        HazardSeverity.MAJOR -> "⚡"
        HazardSeverity.MINOR -> "ℹ"
    }

    val headerText = "$icon ${hazard.severity.name} ${hazard.oshaCode}"
    canvas.drawText(
        headerText,
        MARGIN_LEFT + 8f,
        currentY + FONT_SIZE_BODY,
        createTextPaint(FONT_SIZE_BODY, bold = true, color = edgeColor)
    )

    currentY += LINE_SPACING_BODY + 4f

    // Draw hazard description (compact, no "HAZARD:" label)
    val descResult = drawMultilineText(
        canvas = canvas,
        text = hazard.description,
        x = MARGIN_LEFT + 8f,
        y = currentY,
        maxWidth = pageWidth - MARGIN_LEFT - MARGIN_RIGHT - 12f,
        paint = createTextPaint(FONT_SIZE_BODY),
        lineSpacing = LINE_SPACING_BODY
    )

    currentY = descResult.endY + 6f

    // Draw controls (inline numbered list, no "CONTROLS:" label)
    val controlsText = "Controls: " + hazard.controlMeasures
        .mapIndexed { index, control ->
            "${index + 1}) ${control.replace(Regex("\\(1926\\.\\d+.*?\\)"), "").trim()}"
        }
        .joinToString(" ")

    val controlsResult = drawMultilineText(
        canvas = canvas,
        text = controlsText,
        x = MARGIN_LEFT + 8f,
        y = currentY,
        maxWidth = pageWidth - MARGIN_LEFT - MARGIN_RIGHT - 12f,
        paint = createTextPaint(FONT_SIZE_SMALL, color = COLOR_DARK_GRAY),
        lineSpacing = LINE_SPACING_SMALL
    )

    currentY = controlsResult.endY + HAZARD_VERTICAL_SPACING

    return currentY
}
```

**Space Savings:** ~40-50% reduction in vertical space per hazard

### Step 3: Create Job Steps Table Method
**File:** `shared/src/androidMain/kotlin/com/hazardhawk/documents/AndroidPTPPDFGenerator.kt`

**Add new method:**
```kotlin
private fun drawJobStepsTable(
    canvas: Canvas,
    jobSteps: List<PtpJobStep>,
    y: Float,
    maxY: Float
): DrawResult {
    var currentY = y
    val overflowSteps = mutableListOf<PtpJobStep>()

    // Table column widths
    val colStep = 30f
    val colHazards = 180f
    val colControls = 180f
    val colPPE = 150f
    val tableWidth = colStep + colHazards + colControls + colPPE

    // Draw table header
    canvas.drawRect(
        MARGIN_LEFT,
        currentY,
        MARGIN_LEFT + tableWidth,
        currentY + 20f,
        createTextPaint(color = COLOR_LIGHT_GRAY).apply { style = Paint.Style.FILL }
    )

    canvas.drawText("#", MARGIN_LEFT + 5f, currentY + 15f,
        createTextPaint(FONT_SIZE_SMALL, bold = true))
    canvas.drawText("Hazards", MARGIN_LEFT + colStep + 5f, currentY + 15f,
        createTextPaint(FONT_SIZE_SMALL, bold = true))
    canvas.drawText("Controls", MARGIN_LEFT + colStep + colHazards + 5f, currentY + 15f,
        createTextPaint(FONT_SIZE_SMALL, bold = true))
    canvas.drawText("PPE", MARGIN_LEFT + colStep + colHazards + colControls + 5f, currentY + 15f,
        createTextPaint(FONT_SIZE_SMALL, bold = true))

    currentY += 22f

    // Draw table rows
    jobSteps.forEachIndexed { index, step ->
        // Check if step fits on page
        val estimatedHeight = 40f // Minimum row height
        if (currentY + estimatedHeight > maxY - MARGIN_BOTTOM) {
            overflowSteps.addAll(jobSteps.drop(index))
            return DrawResult(currentY, index, overflowSteps)
        }

        val rowStartY = currentY

        // Column 1: Step number
        canvas.drawText(
            "${index + 1}",
            MARGIN_LEFT + 5f,
            currentY + 12f,
            createTextPaint(FONT_SIZE_SMALL, bold = true)
        )

        // Column 2: Hazards (bulleted list)
        val hazardsText = step.hazards.joinToString("\n") { "• $it" }
        val hazardsResult = drawMultilineText(
            canvas, hazardsText,
            MARGIN_LEFT + colStep + 5f,
            currentY,
            colHazards - 10f,
            createTextPaint(FONT_SIZE_SMALL),
            LINE_SPACING_SMALL,
            maxLines = 5
        )

        // Column 3: Controls (bulleted list)
        val controlsText = step.controls.joinToString("\n") { "• $it" }
        val controlsResult = drawMultilineText(
            canvas, controlsText,
            MARGIN_LEFT + colStep + colHazards + 5f,
            currentY,
            colControls - 10f,
            createTextPaint(FONT_SIZE_SMALL),
            LINE_SPACING_SMALL,
            maxLines = 5
        )

        // Column 4: PPE (comma-separated)
        val ppeText = step.requiredPPE.joinToString(", ")
        val ppeResult = drawMultilineText(
            canvas, ppeText,
            MARGIN_LEFT + colStep + colHazards + colControls + 5f,
            currentY,
            colPPE - 10f,
            createTextPaint(FONT_SIZE_SMALL),
            LINE_SPACING_SMALL,
            maxLines = 5
        )

        // Calculate row height (tallest column)
        val rowHeight = maxOf(
            hazardsResult.endY - currentY,
            controlsResult.endY - currentY,
            ppeResult.endY - currentY,
            25f // Minimum row height
        )

        // Draw table borders
        val paint = createTextPaint(color = COLOR_LIGHT_GRAY)
        paint.strokeWidth = 0.5f
        paint.style = Paint.Style.STROKE

        canvas.drawRect(
            MARGIN_LEFT,
            rowStartY,
            MARGIN_LEFT + tableWidth,
            rowStartY + rowHeight,
            paint
        )

        // Vertical dividers
        canvas.drawLine(MARGIN_LEFT + colStep, rowStartY,
            MARGIN_LEFT + colStep, rowStartY + rowHeight, paint)
        canvas.drawLine(MARGIN_LEFT + colStep + colHazards, rowStartY,
            MARGIN_LEFT + colStep + colHazards, rowStartY + rowHeight, paint)
        canvas.drawLine(MARGIN_LEFT + colStep + colHazards + colControls, rowStartY,
            MARGIN_LEFT + colStep + colHazards + colControls, rowStartY + rowHeight, paint)

        currentY += rowHeight
    }

    return DrawResult(currentY, jobSteps.size, emptyList())
}
```

### Step 4: Modify Main Generation Method
**File:** `shared/src/androidMain/kotlin/com/hazardhawk/documents/AndroidPTPPDFGenerator.kt`

**Method:** `generatePDFWithMetadata()` (lines ~45-184)

**Replace with 4-page structure:**
```kotlin
override fun generatePDFWithMetadata(
    content: PtpContent,
    metadata: PDFMetadata
): Result<ByteArray> {
    return try {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val document = PdfDocument()
        val paintCache = PaintCache()

        // PAGE 1: Project Info + Critical/Major Hazards
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 1).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = MARGIN_TOP

        // Compact header
        currentY = drawHeader(canvas, metadata, currentY)
        currentY += 10f

        // Compact project info (single line)
        canvas.drawText(
            "Work: ${metadata.workType} | Crew: ${metadata.crewSize} | ${metadata.projectLocation}",
            MARGIN_LEFT,
            currentY + FONT_SIZE_BODY,
            createTextPaint(FONT_SIZE_BODY, color = COLOR_DARK_GRAY)
        )
        currentY += LINE_SPACING_BODY + 8f

        // Brief scope
        val scopeResult = drawMultilineText(
            canvas, "Scope: ${content.workScope}",
            MARGIN_LEFT, currentY,
            pageWidth - MARGIN_LEFT - MARGIN_RIGHT,
            createTextPaint(FONT_SIZE_SMALL),
            LINE_SPACING_SMALL
        )
        currentY = scopeResult.endY + 15f

        // Section header
        canvas.drawText(
            "IDENTIFIED HAZARDS",
            MARGIN_LEFT,
            currentY + FONT_SIZE_HEADING,
            createTextPaint(FONT_SIZE_HEADING, bold = true, color = COLOR_PRIMARY)
        )
        currentY += LINE_SPACING_HEADING + 10f

        // Draw CRITICAL and MAJOR hazards only (compact format)
        val criticalMajorHazards = content.hazards.filter {
            it.severity == HazardSeverity.CRITICAL || it.severity == HazardSeverity.MAJOR
        }

        criticalMajorHazards.forEach { hazard ->
            currentY = drawHazardMinimal(canvas, hazard, currentY, pageHeight - MARGIN_BOTTOM)
        }

        drawFooter(canvas, pageHeight - 30f, metadata.projectName, 1)
        document.finishPage(page)

        // PAGE 2: Minor Hazards + Job Steps (Part 1)
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 2).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
        currentY = MARGIN_TOP

        // Draw MINOR hazards
        val minorHazards = content.hazards.filter { it.severity == HazardSeverity.MINOR }

        if (minorHazards.isNotEmpty()) {
            canvas.drawText(
                "HAZARDS (continued)",
                MARGIN_LEFT,
                currentY + FONT_SIZE_HEADING,
                createTextPaint(FONT_SIZE_HEADING, bold = true, color = COLOR_PRIMARY)
            )
            currentY += LINE_SPACING_HEADING + 10f

            minorHazards.forEach { hazard ->
                currentY = drawHazardMinimal(canvas, hazard, currentY, pageHeight - MARGIN_BOTTOM)
            }

            currentY += 15f
        }

        // Job Steps table
        canvas.drawText(
            "JOB STEPS & CONTROLS",
            MARGIN_LEFT,
            currentY + FONT_SIZE_HEADING,
            createTextPaint(FONT_SIZE_HEADING, bold = true, color = COLOR_PRIMARY)
        )
        currentY += LINE_SPACING_HEADING + 10f

        val jobStepsResult = drawJobStepsTable(
            canvas,
            content.jobSteps,
            currentY,
            pageHeight - MARGIN_BOTTOM
        )

        drawFooter(canvas, pageHeight - 30f, metadata.projectName, 2)
        document.finishPage(page)

        // PAGE 3: Job Steps (continued) + Emergency Procedures
        if (jobStepsResult.overflowItems.isNotEmpty()) {
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 3).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = MARGIN_TOP

            canvas.drawText(
                "JOB STEPS (continued)",
                MARGIN_LEFT,
                currentY + FONT_SIZE_HEADING,
                createTextPaint(FONT_SIZE_HEADING, bold = true, color = COLOR_PRIMARY)
            )
            currentY += LINE_SPACING_HEADING + 10f

            val remainingStepsResult = drawJobStepsTable(
                canvas,
                jobStepsResult.overflowItems as List<PtpJobStep>,
                currentY,
                pageHeight - 200f // Leave room for emergency procedures
            )

            currentY = remainingStepsResult.endY + 20f
        } else {
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 3).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = MARGIN_TOP + 50f
        }

        // Emergency procedures (compact)
        canvas.drawText(
            "EMERGENCY PROCEDURES",
            MARGIN_LEFT,
            currentY + FONT_SIZE_HEADING,
            createTextPaint(FONT_SIZE_HEADING, bold = true, color = COLOR_PRIMARY)
        )
        currentY += LINE_SPACING_HEADING + 10f

        val emergencyProcs = listOf(
            "• Fall: Call 911, notify ${content.competentPerson ?: "competent person"}",
            "• Medical: Call 911, provide first aid",
            "• Fire/Arc: Alert others, use extinguisher if safe",
            "• Struck-by: Assess scene, call 911",
            "• Evacuation: Follow site-specific routes"
        )

        emergencyProcs.forEach { proc ->
            canvas.drawText(
                proc,
                MARGIN_LEFT,
                currentY + FONT_SIZE_BODY,
                createTextPaint(FONT_SIZE_BODY)
            )
            currentY += LINE_SPACING_BODY
        }

        drawFooter(canvas, pageHeight - 30f, metadata.projectName, 3)
        document.finishPage(page)

        // PAGE 4: Signatures
        pageInfo = PdfDocument.PageInfo.Builder(pageWidth.toInt(), pageHeight.toInt(), 4).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
        currentY = MARGIN_TOP

        currentY = drawSignaturesCompact(canvas, currentY, metadata)

        drawFooter(canvas, pageHeight - 30f, metadata.projectName, 4)
        document.finishPage(page)

        // Write to byte array
        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        paintCache.clear()

        Result.success(outputStream.toByteArray())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### Step 5: Create Compact Signatures Method
**Add new method:**
```kotlin
private fun drawSignaturesCompact(
    canvas: Canvas,
    y: Float,
    metadata: PDFMetadata
): Float {
    var currentY = y

    canvas.drawText(
        "SIGNATURES & APPROVAL",
        MARGIN_LEFT,
        currentY + FONT_SIZE_HEADING,
        createTextPaint(FONT_SIZE_HEADING, bold = true, color = COLOR_PRIMARY)
    )
    currentY += LINE_SPACING_HEADING + 10f

    canvas.drawText(
        "All crew members must sign to acknowledge review and understanding.",
        MARGIN_LEFT,
        currentY + FONT_SIZE_SMALL,
        createTextPaint(FONT_SIZE_SMALL, color = COLOR_DARK_GRAY)
    )
    currentY += LINE_SPACING_SMALL + 15f

    // Table dimensions
    val colName = 200f
    val colDate = 120f
    val colSignature = 200f
    val tableWidth = colName + colDate + colSignature
    val rowHeight = 30f

    // Draw table header
    val headerPaint = createTextPaint(color = COLOR_LIGHT_GRAY).apply {
        style = Paint.Style.FILL
    }
    canvas.drawRect(
        MARGIN_LEFT,
        currentY,
        MARGIN_LEFT + tableWidth,
        currentY + rowHeight,
        headerPaint
    )

    canvas.drawText("Name", MARGIN_LEFT + 5f, currentY + 20f,
        createTextPaint(FONT_SIZE_SMALL, bold = true))
    canvas.drawText("Date", MARGIN_LEFT + colName + 5f, currentY + 20f,
        createTextPaint(FONT_SIZE_SMALL, bold = true))
    canvas.drawText("Signature", MARGIN_LEFT + colName + colDate + 5f, currentY + 20f,
        createTextPaint(FONT_SIZE_SMALL, bold = true))

    currentY += rowHeight

    // Draw worker rows (10 workers)
    val borderPaint = createTextPaint(color = COLOR_LIGHT_GRAY).apply {
        strokeWidth = 0.5f
        style = Paint.Style.STROKE
    }

    for (i in 1..10) {
        canvas.drawRect(
            MARGIN_LEFT,
            currentY,
            MARGIN_LEFT + tableWidth,
            currentY + rowHeight,
            borderPaint
        )

        canvas.drawText(
            "Worker $i",
            MARGIN_LEFT + 5f,
            currentY + 20f,
            createTextPaint(FONT_SIZE_SMALL, color = COLOR_DARK_GRAY)
        )

        // Vertical dividers
        canvas.drawLine(MARGIN_LEFT + colName, currentY,
            MARGIN_LEFT + colName, currentY + rowHeight, borderPaint)
        canvas.drawLine(MARGIN_LEFT + colName + colDate, currentY,
            MARGIN_LEFT + colName + colDate, currentY + rowHeight, borderPaint)

        currentY += rowHeight
    }

    currentY += 20f

    // Competent Person signature
    canvas.drawRect(
        MARGIN_LEFT,
        currentY,
        MARGIN_LEFT + tableWidth,
        currentY + rowHeight + 10f,
        headerPaint
    )

    canvas.drawText(
        "Competent Person: ${metadata.competentPerson ?: "_______________________"}",
        MARGIN_LEFT + 5f,
        currentY + 15f,
        createTextPaint(FONT_SIZE_BODY, bold = true)
    )

    currentY += rowHeight + 10f

    canvas.drawText(
        "Date: _______________    Signature: _______________________",
        MARGIN_LEFT + 5f,
        currentY + 15f,
        createTextPaint(FONT_SIZE_SMALL)
    )

    return currentY + 30f
}
```

---

## ✅ Expected Results

### Before (Current):
- **7 pages total**
- Thick colored borders on every hazard
- Single-column layout
- Large margins and spacing
- Verbose job step descriptions
- Full emergency procedure details

### After (Target):
- **4 pages maximum** (2 sheets front/back)
- Minimal left-edge color indicators
- Two-column and table layouts
- Compact margins and spacing
- Condensed job step tables
- Abbreviated emergency procedures

### Space Savings Breakdown:
1. **Hazards:** 7 hazards × 150px each = 1050px → 7 hazards × 60px = 420px (**60% reduction**)
2. **Job Steps:** 7 steps × 180px each = 1260px → 7 steps × 60px = 420px (**67% reduction**)
3. **Margins:** 140px per page × 7 pages = 980px → 90px × 4 pages = 360px (**63% reduction**)
4. **Redundant Content:** Page 1 summary removed = **1 full page saved**

**Total:** ~57% reduction in overall document length

---

## 🧪 Testing Checklist

- [ ] Generate PTP with 3 CRITICAL hazards, verify fits on page 1
- [ ] Generate PTP with 10 total hazards, verify all fit on pages 1-2
- [ ] Generate PTP with 7 job steps, verify table format on pages 2-3
- [ ] Verify emergency procedures fit on page 3
- [ ] Verify signature table fits all 10 workers on page 4
- [ ] Test with maximum content (15 hazards, 10 job steps) - should still be ≤4 pages
- [ ] Print test on actual paper - verify readability at arms length
- [ ] Test outdoor readability in sunlight
- [ ] Verify all OSHA codes still visible
- [ ] Verify colored left edges clear for severity identification

---

## 📝 Notes

- **Print Quality:** Font sizes reduced but should remain readable when printed at actual size
- **Color Coding:** Left edge color bars (2f width) provide quick visual severity identification
- **Field Use:** 4 pages = 2 sheets front/back, easy to staple and hand out at meetings
- **Regulatory:** All OSHA codes, controls, and required content still present, just formatted compactly
- **Signatures:** All 10 crew members + competent person fit on single page

---

**Estimated Implementation Time:** 3-4 hours
**Testing Time:** 1 hour
**Total:** 4-5 hours

**Priority:** HIGH - Directly impacts field usability and meeting efficiency
