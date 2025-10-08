package com.hazardhawk.documents

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.hazardhawk.domain.models.ptp.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.ByteArrayOutputStream

/**
 * Android implementation of PTPPDFGenerator using Android's PdfDocument API.
 *
 * This implementation generates OSHA-compliant Pre-Task Plan PDFs with:
 * - Professional layout with company branding
 * - Structured sections (Project Info, Hazards, Job Steps, Photos, etc.)
 * - Color-coded hazard severity
 * - Photo documentation with metadata
 * - Digital signatures
 * - Page numbers and footers
 *
 * Performance targets:
 * - Generation time: < 5 seconds for 10-photo PTP
 * - File size: < 10MB with full-resolution photos
 * - Memory usage: < 200MB during generation
 */
class AndroidPTPPDFGenerator : PTPPDFGenerator {

    override suspend fun generatePDF(ptp: PreTaskPlan, photos: List<PhotoData>): Result<ByteArray> {
        // Use default metadata
        val metadata = PDFMetadata(
            companyName = "Construction Safety",
            projectName = ptp.projectId ?: "Unknown Project",
            projectLocation = "Project Site"
        )
        return generatePDFWithMetadata(ptp, photos, metadata)
    }

    override suspend fun generatePDFWithMetadata(
        ptp: PreTaskPlan,
        photos: List<PhotoData>,
        metadata: PDFMetadata
    ): Result<ByteArray> = withContext(Dispatchers.IO) {

        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            // Optimize photos upfront if there are many
            val optimizedPhotos = if (photos.size > 5) {
                optimizePhotoData(photos)
            } else {
                photos
            }

            // Early validation
            val content = ptp.userModifiedContent ?: ptp.aiGeneratedContent
            if (content == null) {
                paintCache.clear()
                return@withContext Result.failure(
                    IllegalStateException("PTP has no content to generate PDF")
                )
            }

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(
                PDFLayoutConfig.PAGE_WIDTH.toInt(),
                PDFLayoutConfig.PAGE_HEIGHT.toInt(),
                1
            ).create()

            var currentPage = 1
            val hazards = content.hazards

            // Page 1: Executive Summary - 5 minute overview (critical)
            try {
                val summaryPage = document.startPage(pageInfo)
                var yPosition = drawHeader(summaryPage.canvas, metadata, currentPage)
                yPosition = drawExecutiveSummary(summaryPage.canvas, ptp, hazards, metadata, yPosition)
                drawFooter(summaryPage.canvas, currentPage, metadata)
                document.finishPage(summaryPage)
                currentPage++
            } catch (e: Exception) {
                errors.add("Executive summary page failed: ${e.message}")
                // Critical section - log but continue with remaining pages
            }

            // Page 2: Full Project Info + Work Scope + Hazards (part 1) (critical)
            try {
                val page2 = document.startPage(pageInfo)
                var yPosition = drawHeader(page2.canvas, metadata, currentPage)
                yPosition = drawProjectInfo(page2.canvas, ptp, metadata, yPosition)
                yPosition = drawWorkScope(page2.canvas, ptp, yPosition)

                if (hazards.isNotEmpty()) {
                    yPosition = drawHazardsSection(page2.canvas, hazards, yPosition, page2.info.pageHeight.toFloat())
                }

                drawFooter(page2.canvas, currentPage, metadata)
                document.finishPage(page2)
                currentPage++
            } catch (e: Exception) {
                errors.add("Project info/hazards page failed: ${e.message}")
            }

            // Additional pages for remaining hazards if needed (critical)
            try {
                if (hazards.isNotEmpty()) {
                    val hazardsPerPage = calculateHazardsPerPage()
                    val remainingHazards = hazards.drop(hazardsPerPage)

                    for (hazardChunk in remainingHazards.chunked(hazardsPerPage + 2)) {
                        val hazardPage = document.startPage(pageInfo)
                        var hazardY = PDFLayoutConfig.MARGIN_TOP
                        hazardY = drawSectionHeader(hazardPage.canvas, "Identified Hazards (continued)", hazardY)
                        drawHazardList(hazardPage.canvas, hazardChunk, hazardY, hazardPage.info.pageHeight.toFloat())
                        drawFooter(hazardPage.canvas, currentPage, metadata)
                        document.finishPage(hazardPage)
                        currentPage++
                    }
                }
            } catch (e: Exception) {
                warnings.add("Some hazards pages skipped: ${e.message}")
            }

            // Job Steps pages (non-critical)
            try {
                val jobSteps = content.jobSteps
                if (jobSteps.isNotEmpty()) {
                    val stepsPerPage = 4
                    for (stepsChunk in jobSteps.chunked(stepsPerPage)) {
                        val stepsPage = document.startPage(pageInfo)
                        var stepsY = PDFLayoutConfig.MARGIN_TOP
                        stepsY = drawSectionHeader(stepsPage.canvas, "Job Steps & Safety Controls", stepsY)
                        drawJobSteps(stepsPage.canvas, stepsChunk, stepsY)
                        drawFooter(stepsPage.canvas, currentPage, metadata)
                        document.finishPage(stepsPage)
                        currentPage++
                    }
                }
            } catch (e: Exception) {
                warnings.add("Job steps section skipped: ${e.message}")
            }

            // Photo pages (2 photos per page) (non-critical)
            try {
                if (optimizedPhotos.isNotEmpty()) {
                    for (photoGroup in optimizedPhotos.chunked(PDFLayoutConfig.PHOTOS_PER_PAGE)) {
                        val photoPage = document.startPage(pageInfo)
                        var photoY = PDFLayoutConfig.MARGIN_TOP
                        photoY = drawSectionHeader(photoPage.canvas, "Photo Documentation", photoY)
                        drawPhotos(photoPage.canvas, photoGroup, photoY)
                        drawFooter(photoPage.canvas, currentPage, metadata)
                        document.finishPage(photoPage)
                        currentPage++
                    }
                }
            } catch (e: Exception) {
                warnings.add("Photo section skipped: ${e.message}")
            }

            // Final page: Emergency Procedures + Signatures (non-critical)
            try {
                val finalPage = document.startPage(pageInfo)
                var finalY = PDFLayoutConfig.MARGIN_TOP
                finalY = drawEmergencyProcedures(finalPage.canvas, ptp, content, finalY)
                drawSignatures(finalPage.canvas, ptp, finalY)
                drawFooter(finalPage.canvas, currentPage, metadata)
                document.finishPage(finalPage)
            } catch (e: Exception) {
                warnings.add("Emergency/signatures page skipped: ${e.message}")
            }

            // Convert to ByteArray
            val outputStream = ByteArrayOutputStream()
            document.writeTo(outputStream)
            document.close()

            // Clear paint cache after generation
            paintCache.clear()

            // Log errors/warnings if any
            if (errors.isNotEmpty() || warnings.isNotEmpty()) {
                println("PDF generated with issues:")
                errors.forEach { println("ERROR: $it") }
                warnings.forEach { println("WARNING: $it") }
            }

            Result.success(outputStream.toByteArray())
        } catch (e: Exception) {
            paintCache.clear()
            Result.failure(e)
        }
    }

    /**
     * Draw executive summary - one page, 5-minute overview for workers
     */
    private fun drawExecutiveSummary(
        canvas: Canvas,
        ptp: PreTaskPlan,
        hazards: List<PtpHazard>,
        metadata: PDFMetadata,
        startY: Float
    ): Float {
        var y = startY

        // Big, bold title
        val titlePaint = createTextPaint(24f, PDFLayoutConfig.COLOR_HEADER, true)
        canvas.drawText("DAILY SAFETY PLAN - READ THIS FIRST", PDFLayoutConfig.MARGIN_LEFT, y, titlePaint)
        y += 40f

        // Quick info box
        val infoPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_LARGE, PDFLayoutConfig.COLOR_BLACK, false)
        val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_LARGE, PDFLayoutConfig.COLOR_BLACK, true)

        canvas.drawText("Today's Work:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
        canvas.drawText(ptp.workType, PDFLayoutConfig.MARGIN_LEFT + 140f, y, infoPaint)
        y += PDFLayoutConfig.LINE_SPACING_BODY + 5f

        canvas.drawText("Crew Size:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
        canvas.drawText("${ptp.crewSize ?: "TBD"} workers", PDFLayoutConfig.MARGIN_LEFT + 140f, y, infoPaint)
        y += PDFLayoutConfig.LINE_SPACING_BODY + 5f

        canvas.drawText("Location:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
        canvas.drawText(metadata.projectLocation, PDFLayoutConfig.MARGIN_LEFT + 140f, y, infoPaint)
        y += 35f

        // Critical hazards callout box
        val criticalHazards = hazards.filter { it.severity == HazardSeverity.CRITICAL }
        if (criticalHazards.isNotEmpty()) {
            // Red warning box
            val boxPaint = Paint().apply {
                color = 0xFFFFE5E5.toInt() // Light red background
                style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply {
                color = PDFLayoutConfig.COLOR_CRITICAL
                strokeWidth = 4f
                style = Paint.Style.STROKE
            }

            val boxHeight = 60f + (criticalHazards.size * 22f)
            val boxRect = RectF(
                PDFLayoutConfig.MARGIN_LEFT,
                y,
                PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT,
                y + boxHeight
            )
            canvas.drawRoundRect(boxRect, 8f, 8f, boxPaint)
            canvas.drawRoundRect(boxRect, 8f, 8f, borderPaint)

            y += 25f
            val warningPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_LARGE, PDFLayoutConfig.COLOR_CRITICAL, true)
            canvas.drawText("⚠ CRITICAL DANGERS - PAY ATTENTION!", PDFLayoutConfig.MARGIN_LEFT + 15f, y, warningPaint)
            y += 25f

            val criticalTextPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)
            for (hazard in criticalHazards) {
                canvas.drawText("• ${simplifyForWorkers(hazard.description)}", PDFLayoutConfig.MARGIN_LEFT + 20f, y, criticalTextPaint)
                y += 22f
            }
            y += 20f
        }

        // Top 3 hazards summary
        y += 10f
        val sectionPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_HEADING, PDFLayoutConfig.COLOR_HEADER, true)
        canvas.drawText("TOP DANGERS TO WATCH FOR:", PDFLayoutConfig.MARGIN_LEFT, y, sectionPaint)
        y += 30f

        val topHazards = hazards.sortedByDescending {
            when(it.severity) {
                HazardSeverity.CRITICAL -> 3
                HazardSeverity.MAJOR -> 2
                HazardSeverity.MINOR -> 1
            }
        }.take(3)

        for ((index, hazard) in topHazards.withIndex()) {
            val icon = when(hazard.severity) {
                HazardSeverity.CRITICAL -> "⚠"
                HazardSeverity.MAJOR -> "⚡"
                HazardSeverity.MINOR -> "ℹ"
            }
            val severityColor = when(hazard.severity) {
                HazardSeverity.CRITICAL -> PDFLayoutConfig.COLOR_CRITICAL
                HazardSeverity.MAJOR -> PDFLayoutConfig.COLOR_MAJOR
                HazardSeverity.MINOR -> PDFLayoutConfig.COLOR_MINOR
            }

            // Hazard number and icon
            val numPaint = createTextPaint(18f, severityColor, true)
            canvas.drawText("${index + 1}. $icon", PDFLayoutConfig.MARGIN_LEFT, y, numPaint)

            // Simple description
            val descPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)
            val descResult = drawMultilineText(
                canvas,
                simplifyForWorkers(hazard.description),
                PDFLayoutConfig.MARGIN_LEFT + 50f,
                y,
                PDFLayoutConfig.CONTENT_WIDTH - 50f,
                descPaint,
                PDFLayoutConfig.LINE_SPACING_BODY
            )
            y = descResult.endY + 5f

            // Top 2 controls as action steps
            val controlPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, true)
            canvas.drawText("What to do:", PDFLayoutConfig.MARGIN_LEFT + 50f, y, controlPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY

            val actionPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, 0xFF006600.toInt(), false)
            for (control in hazard.controls.take(2)) {
                canvas.drawText("✓ ${makeActionOriented(control)}", PDFLayoutConfig.MARGIN_LEFT + 60f, y, actionPaint)
                y += PDFLayoutConfig.LINE_SPACING_BODY
            }
            y += 20f
        }

        // Simple next steps
        y += 10f
        canvas.drawText("BEFORE YOU START WORK:", PDFLayoutConfig.MARGIN_LEFT, y, sectionPaint)
        y += 25f

        val stepPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)
        val steps = listOf(
            "1. Read all danger warnings above",
            "2. Put on required safety gear (PPE)",
            "3. Check your tools and equipment",
            "4. Ask questions if anything is unclear",
            "5. Sign the back page to show you understand"
        )
        for (step in steps) {
            canvas.drawText(step, PDFLayoutConfig.MARGIN_LEFT + 20f, y, stepPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY + 3f
        }

        return y
    }

    /**
     * Simplify technical language to 6th grade reading level
     */
    private fun simplifyForWorkers(text: String): String {
        return text
            .replace("elevated", "high up")
            .replace("confined space", "tight space")
            .replace("electrocution", "electric shock")
            .replace("excavation", "digging area")
            .replace("hazardous", "dangerous")
            .replace("proximity", "close to")
            .replace("adequate", "enough")
            .replace("utilize", "use")
            .replace("implement", "use")
            .replace("personnel", "workers")
            .replace("verify", "check")
            .replace("ensure", "make sure")
            .replace("maintain", "keep")
    }

    /**
     * Make control statements action-oriented with strong verbs
     */
    private fun makeActionOriented(control: String): String {
        val actionWords = listOf("Wear", "Use", "Check", "Inspect", "Keep", "Stay", "Avoid", "Stop", "Watch")
        val lowerControl = control.trim().lowercase()

        // If it already starts with an action word, return as is
        if (actionWords.any { lowerControl.startsWith(it.lowercase()) }) {
            return control
        }

        // Otherwise, try to add an action verb
        return when {
            lowerControl.contains("ppe") || lowerControl.contains("protective") -> "Wear $control"
            lowerControl.contains("distance") || lowerControl.contains("away") -> "Stay $control"
            lowerControl.contains("inspect") || lowerControl.contains("monitor") -> "Check $control"
            lowerControl.contains("barrier") || lowerControl.contains("guard") -> "Use $control"
            else -> control
        }
    }

    /**
     * Draw the document header with company logo and title.
     * Enhanced with colored header band and professional branding.
     */
    private fun drawHeader(canvas: Canvas, metadata: PDFMetadata, pageNumber: Int): Float {
        var y = 0f

        // Colored header band
        val headerBarPaint = Paint().apply {
            color = PDFLayoutConfig.COLOR_PRIMARY
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            0f, 0f,
            PDFLayoutConfig.PAGE_WIDTH,
            60f,
            headerBarPaint
        )

        // Company logo (if provided)
        metadata.companyLogo?.let { logoBytes ->
            try {
                // White circle background for logo
                val circlePaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawCircle(
                    PDFLayoutConfig.MARGIN_LEFT + 20f,
                    30f,
                    22f,
                    circlePaint
                )

                // Draw logo
                drawBitmapSafe(
                    canvas,
                    logoBytes,
                    RectF(
                        PDFLayoutConfig.MARGIN_LEFT + 2f,
                        12f,
                        PDFLayoutConfig.MARGIN_LEFT + 38f,
                        48f
                    ),
                    placeholder = null
                )
            } catch (e: Exception) {
                // Fallback: Company initials
                val initialsPaint = createTextPaint(16f, Color.WHITE, true)
                val initials = metadata.companyName
                    .split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull() }
                    .joinToString("")
                canvas.drawText(
                    initials,
                    PDFLayoutConfig.MARGIN_LEFT + 12f,
                    35f,
                    initialsPaint
                )
            }
        }

        // Title (white text on colored bar)
        val titlePaint = createTextPaint(
            PDFLayoutConfig.FONT_SIZE_TITLE,
            Color.WHITE,
            true
        )
        canvas.drawText(
            "PRE-TASK SAFETY PLAN",
            PDFLayoutConfig.MARGIN_LEFT + 60f,
            35f,
            titlePaint
        )

        y = 70f

        // Metadata line (below header bar)
        val metaPaint = createTextPaint(
            PDFLayoutConfig.FONT_SIZE_SMALL,
            PDFLayoutConfig.COLOR_DARK_GRAY,
            false
        )

        val metadataText = buildString {
            append(metadata.companyName)
            append(" | ")
            append(metadata.projectName)
            append(" | ")
            append(metadata.projectLocation)
        }

        canvas.drawText(
            metadataText,
            PDFLayoutConfig.MARGIN_LEFT,
            y,
            metaPaint
        )

        y += PDFLayoutConfig.LINE_SPACING_SMALL + PDFLayoutConfig.SECTION_SPACING

        return y
    }

    /**
     * Draw project information section.
     */
    private fun drawProjectInfo(canvas: Canvas, ptp: PreTaskPlan, metadata: PDFMetadata, startY: Float): Float {
        var y = drawSectionHeader(canvas, "Project Information", startY)

        val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_DARK_GRAY, true)
        val valuePaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)

        // Project details
        val details = listOf(
            "Project Name" to metadata.projectName,
            "Location" to metadata.projectLocation,
            "Work Type" to ptp.workType,
            "Crew Size" to (ptp.crewSize?.toString() ?: "Not specified"),
            "Status" to ptp.status.name,
            "Created" to formatDate(ptp.createdAt),
            "Created By" to ptp.createdBy
        )

        for ((label, value) in details) {
            canvas.drawText("$label:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
            canvas.drawText(value, PDFLayoutConfig.MARGIN_LEFT + 150f, y, valuePaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY
        }

        y += PDFLayoutConfig.SECTION_SPACING
        return y
    }

    /**
     * Draw work scope section.
     */
    private fun drawWorkScope(canvas: Canvas, ptp: PreTaskPlan, startY: Float): Float {
        var y = drawSectionHeader(canvas, "Work Scope & Description", startY)

        val bodyPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)

        // Work scope description
        val scopeResult = drawMultilineText(canvas, ptp.workScope, PDFLayoutConfig.MARGIN_LEFT, y,
            PDFLayoutConfig.CONTENT_WIDTH, bodyPaint, PDFLayoutConfig.LINE_SPACING_BODY)
        y = scopeResult.endY

        y += PDFLayoutConfig.SUBSECTION_SPACING

        // Tools & Equipment
        if (ptp.toolsEquipment.isNotEmpty()) {
            val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_DARK_GRAY, true)
            canvas.drawText("Tools & Equipment:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY

            for (tool in ptp.toolsEquipment) {
                canvas.drawText("• $tool", PDFLayoutConfig.MARGIN_LEFT + 20f, y, bodyPaint)
                y += PDFLayoutConfig.LINE_SPACING_BODY
            }
            y += PDFLayoutConfig.SUBSECTION_SPACING
        }

        y += PDFLayoutConfig.SECTION_SPACING
        return y
    }

    /**
     * Draw hazards section header and first batch of hazards.
     */
    private fun drawHazardsSection(canvas: Canvas, hazards: List<PtpHazard>, startY: Float, pageHeight: Float): Float {
        var y = drawSectionHeader(canvas, "Identified Hazards", startY)

        // Draw hazards with overflow detection
        val result = drawHazardList(canvas, hazards, y, pageHeight)
        return result.endY
    }

    /**
     * Draw a list of hazards with color-coded severity and proper page handling.
     *
     * @return DrawResult containing final Y position, count of drawn hazards, and overflow items
     */
    private fun drawHazardList(canvas: Canvas, hazards: List<PtpHazard>, startY: Float, pageHeight: Float): DrawResult {
        var y = startY
        val maxY = pageHeight - PDFLayoutConfig.MARGIN_BOTTOM - 50f // Leave room for footer
        val drawnHazards = mutableListOf<PtpHazard>()
        val overflowHazards = mutableListOf<PtpHazard>()

        for (hazard in hazards) {
            // Pre-calculate exact height needed for this hazard
            val boxHeight = calculateHazardBoxHeight(hazard)

            // Check if hazard fits on page with buffer - if not, add to overflow
            if (y + boxHeight + PDFLayoutConfig.SUBSECTION_SPACING > maxY) {
                overflowHazards.add(hazard)
                continue
            }

            val boxTop = y
            val boxLeft = PDFLayoutConfig.MARGIN_LEFT
            val boxRight = PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT
            val boxBottom = boxTop + boxHeight

            // Background color based on severity
            val bgPaint = Paint().apply {
                color = when (hazard.severity) {
                    HazardSeverity.CRITICAL -> PDFLayoutConfig.COLOR_BG_CRITICAL
                    HazardSeverity.MAJOR -> PDFLayoutConfig.COLOR_BG_MAJOR
                    HazardSeverity.MINOR -> PDFLayoutConfig.COLOR_BG_MINOR
                }
                style = Paint.Style.FILL
            }
            canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, bgPaint)

            // Colored left edge badge (8f width)
            val badgePaint = Paint().apply {
                color = when (hazard.severity) {
                    HazardSeverity.CRITICAL -> PDFLayoutConfig.COLOR_CRITICAL
                    HazardSeverity.MAJOR -> PDFLayoutConfig.COLOR_MAJOR
                    HazardSeverity.MINOR -> PDFLayoutConfig.COLOR_MINOR
                }
                style = Paint.Style.FILL
            }
            canvas.drawRect(
                boxLeft,
                boxTop,
                boxLeft + 8f,
                boxBottom,
                badgePaint
            )

            // Border
            val borderPaint = Paint().apply {
                color = when (hazard.severity) {
                    HazardSeverity.CRITICAL -> PDFLayoutConfig.COLOR_CRITICAL
                    HazardSeverity.MAJOR -> PDFLayoutConfig.COLOR_MAJOR
                    HazardSeverity.MINOR -> PDFLayoutConfig.COLOR_MINOR
                }
                style = Paint.Style.STROKE
                strokeWidth = PDFLayoutConfig.HAZARD_BOX_BORDER_WIDTH
            }
            canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, borderPaint)

            // Content (indented from left edge)
            val contentLeft = boxLeft + 20f
            val contentRight = boxRight - PDFLayoutConfig.HAZARD_BOX_PADDING
            val contentWidth = contentRight - contentLeft

            var boxY = boxTop + PDFLayoutConfig.HAZARD_BOX_PADDING

            // Severity badge text
            val severityPaint = createTextPaint(
                PDFLayoutConfig.FONT_SIZE_SMALL,
                when (hazard.severity) {
                    HazardSeverity.CRITICAL -> PDFLayoutConfig.COLOR_CRITICAL
                    HazardSeverity.MAJOR -> PDFLayoutConfig.COLOR_MAJOR
                    HazardSeverity.MINOR -> PDFLayoutConfig.COLOR_MINOR
                },
                true
            )

            val severityText = when (hazard.severity) {
                HazardSeverity.CRITICAL -> "⚠️ CRITICAL HAZARD"
                HazardSeverity.MAJOR -> "⚡ MAJOR HAZARD"
                HazardSeverity.MINOR -> "ℹ️ MINOR HAZARD"
            }

            canvas.drawText(severityText, contentLeft, boxY, severityPaint)
            boxY += PDFLayoutConfig.LINE_SPACING_HEADING

            // OSHA code
            hazard.oshaCode?.let { code ->
                val codePaint = createTextPaint(
                    PDFLayoutConfig.FONT_SIZE_HEADING,
                    PDFLayoutConfig.COLOR_BLACK,
                    true
                )
                canvas.drawText(code, contentLeft, boxY, codePaint)
                boxY += PDFLayoutConfig.LINE_SPACING_HEADING
            }

            // Hazard description label removed - going straight to description content

            // Description section
            val descLabelPaint = createTextPaint(
                PDFLayoutConfig.FONT_SIZE_BODY,
                PDFLayoutConfig.COLOR_DARK_GRAY,
                true
            )
            canvas.drawText("📋 HAZARD:", contentLeft, boxY, descLabelPaint)
            boxY += PDFLayoutConfig.LINE_SPACING_BODY

            val descPaint = createTextPaint(
                PDFLayoutConfig.FONT_SIZE_BODY,
                PDFLayoutConfig.COLOR_BLACK,
                false
            )

            val descResult = drawMultilineText(
                canvas,
                hazard.description,
                contentLeft,
                boxY,
                contentWidth,
                descPaint,
                PDFLayoutConfig.LINE_SPACING_BODY
            )
            boxY = descResult.endY + 12f

            // Controls section
            if (hazard.controls.isNotEmpty()) {
                canvas.drawText("✅ CONTROLS:", contentLeft, boxY, descLabelPaint)
                boxY += PDFLayoutConfig.LINE_SPACING_BODY

                for ((index, control) in hazard.controls.withIndex()) {
                    val controlText = "${index + 1}. $control"
                    val controlResult = drawMultilineText(
                        canvas,
                        controlText,
                        contentLeft,
                        boxY,
                        contentWidth,
                        descPaint,
                        PDFLayoutConfig.LINE_SPACING_BODY
                    )
                    boxY = controlResult.endY
                }
                boxY += 12f
            }

            // PPE section
            if (hazard.requiredPpe.isNotEmpty()) {
                canvas.drawText("🦺 REQUIRED PPE:", contentLeft, boxY, descLabelPaint)
                boxY += PDFLayoutConfig.LINE_SPACING_BODY

                val ppeText = hazard.requiredPpe.joinToString("\n") { "• $it" }
                val ppeResult = drawMultilineText(
                    canvas,
                    ppeText,
                    contentLeft,
                    boxY,
                    contentWidth,
                    descPaint,
                    PDFLayoutConfig.LINE_SPACING_BODY
                )
                boxY = ppeResult.endY + 8f
            }

            y = boxTop + boxHeight + PDFLayoutConfig.SUBSECTION_SPACING
            drawnHazards.add(hazard)
        }

        return DrawResult(
            endY = y,
            itemsDrawn = drawnHazards.size,
            overflowItems = overflowHazards
        )
    }

    /**
     * Result of drawing operation with overflow tracking.
     */
    private data class DrawResult(
        val endY: Float,
        val itemsDrawn: Int,
        val overflowItems: List<PtpHazard> = emptyList()
    )

    /**
     * Calculate accurate height needed for a hazard box by measuring actual text.
     */
    private fun calculateHazardBoxHeight(hazard: PtpHazard): Float {
        var height = PDFLayoutConfig.HAZARD_BOX_PADDING_TOP

        // Header line (OSHA code + severity)
        height += PDFLayoutConfig.LINE_SPACING_BODY + 2f

        // Description - measure actual wrapped text height
        val bodyPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)
        val descriptionWidth = PDFLayoutConfig.CONTENT_WIDTH - PDFLayoutConfig.HAZARD_BOX_PADDING_LEFT - PDFLayoutConfig.HAZARD_BOX_PADDING_RIGHT
        val descriptionLines = measureMultilineTextHeight(hazard.description, descriptionWidth, bodyPaint, PDFLayoutConfig.LINE_SPACING_BODY)
        height += descriptionLines
        height += PDFLayoutConfig.SUBSECTION_SPACING

        // Controls section
        if (hazard.controls.isNotEmpty()) {
            height += PDFLayoutConfig.LINE_SPACING_SMALL // "Controls:" label

            val smallPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_BLACK, false)
            val controlWidth = descriptionWidth - 10f // Account for bullet indent
            for (control in hazard.controls) {
                val controlText = "• $control"
                val controlLines = measureMultilineTextHeight(controlText, controlWidth, smallPaint, PDFLayoutConfig.LINE_SPACING_SMALL)
                height += controlLines
            }
        }

        // PPE line
        if (hazard.requiredPpe.isNotEmpty()) {
            height += 2f + PDFLayoutConfig.LINE_SPACING_SMALL
        }

        height += PDFLayoutConfig.HAZARD_BOX_PADDING_BOTTOM
        return height.coerceAtLeast(PDFLayoutConfig.HAZARD_BOX_MIN_HEIGHT)
    }

    /**
     * Measure the actual height needed for multiline text with word wrapping.
     */
    private fun measureMultilineTextHeight(
        text: String,
        maxWidth: Float,
        paint: Paint,
        lineSpacing: Float
    ): Float {
        val words = text.split(" ")
        var lineCount = 0
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)

            if (textWidth > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lineCount++
                }
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lineCount++
        }

        return lineCount * lineSpacing
    }

    /**
     * Calculate how many hazards fit on a page.
     */
    private fun calculateHazardsPerPage(): Int {
        // Conservative estimate: average 100px per hazard
        val availableHeight = PDFLayoutConfig.CONTENT_HEIGHT - 200f // Reserve space for header
        return (availableHeight / 100f).toInt().coerceAtLeast(2)
    }

    /**
     * Draw job steps section.
     */
    private fun drawJobSteps(canvas: Canvas, steps: List<JobStep>, startY: Float): Float {
        var y = startY + PDFLayoutConfig.SUBSECTION_SPACING

        for (step in steps) {
            // Step number and description
            val stepPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_HEADER, true)
            canvas.drawText("Step ${step.stepNumber}: ${step.description}",
                PDFLayoutConfig.MARGIN_LEFT, y, stepPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY

            val bodyPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_BLACK, false)
            val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_DARK_GRAY, true)

            // Hazards
            if (step.hazards.isNotEmpty()) {
                canvas.drawText("Hazards:", PDFLayoutConfig.MARGIN_LEFT + 20f, y, labelPaint)
                y += PDFLayoutConfig.LINE_SPACING_SMALL
                for (hazard in step.hazards) {
                    canvas.drawText("• $hazard", PDFLayoutConfig.MARGIN_LEFT + 40f, y, bodyPaint)
                    y += PDFLayoutConfig.LINE_SPACING_SMALL
                }
            }

            // Controls
            if (step.controls.isNotEmpty()) {
                canvas.drawText("Controls:", PDFLayoutConfig.MARGIN_LEFT + 20f, y, labelPaint)
                y += PDFLayoutConfig.LINE_SPACING_SMALL
                for (control in step.controls) {
                    canvas.drawText("• $control", PDFLayoutConfig.MARGIN_LEFT + 40f, y, bodyPaint)
                    y += PDFLayoutConfig.LINE_SPACING_SMALL
                }
            }

            // PPE
            if (step.ppe.isNotEmpty()) {
                canvas.drawText("PPE: ${step.ppe.joinToString(", ")}",
                    PDFLayoutConfig.MARGIN_LEFT + 20f, y, labelPaint)
                y += PDFLayoutConfig.LINE_SPACING_SMALL
            }

            y += PDFLayoutConfig.SUBSECTION_SPACING
        }

        return y
    }

    /**
     * Draw photos with metadata.
     */
    private fun drawPhotos(canvas: Canvas, photos: List<PhotoData>, startY: Float): Float {
        var y = startY + PDFLayoutConfig.SUBSECTION_SPACING

        for (photo in photos) {
            // Draw photo on left side with safe bitmap handling
            val photoRect = RectF(
                PDFLayoutConfig.MARGIN_LEFT,
                y,
                PDFLayoutConfig.MARGIN_LEFT + PDFLayoutConfig.PHOTO_WIDTH,
                y + PDFLayoutConfig.PHOTO_HEIGHT
            )

            val photoDrawn = drawBitmapSafe(
                canvas,
                photo.imageBytes,
                photoRect,
                "Photo"
            )

            if (photoDrawn) {

                // Draw metadata on right side
                val metadataX = PDFLayoutConfig.MARGIN_LEFT + PDFLayoutConfig.PHOTO_WIDTH + 20f
                var metadataY = y + 15f

                val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_DARK_GRAY, true)
                val valuePaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_BLACK, false)

                // Location
                photo.metadata.location?.let {
                    canvas.drawText("Location:", metadataX, metadataY, labelPaint)
                    metadataY += PDFLayoutConfig.LINE_SPACING_SMALL
                    canvas.drawText(it, metadataX, metadataY, valuePaint)
                    metadataY += PDFLayoutConfig.LINE_SPACING_SMALL + 4f
                }

                // GPS Coordinates
                photo.metadata.gpsCoordinates?.let {
                    canvas.drawText("GPS:", metadataX, metadataY, labelPaint)
                    metadataY += PDFLayoutConfig.LINE_SPACING_SMALL
                    canvas.drawText(it, metadataX, metadataY, valuePaint)
                    metadataY += PDFLayoutConfig.LINE_SPACING_SMALL + 4f
                }

                // Timestamp
                canvas.drawText("Date:", metadataX, metadataY, labelPaint)
                metadataY += PDFLayoutConfig.LINE_SPACING_SMALL
                canvas.drawText(formatDate(photo.metadata.timestamp), metadataX, metadataY, valuePaint)
                metadataY += PDFLayoutConfig.LINE_SPACING_SMALL + 4f

                // AI Analysis Summary
                if (photo.metadata.aiAnalysisSummary.isNotEmpty()) {
                    canvas.drawText("AI Analysis:", metadataX, metadataY, labelPaint)
                    metadataY += PDFLayoutConfig.LINE_SPACING_SMALL
                    for (finding in photo.metadata.aiAnalysisSummary.take(3)) { // Show max 3
                        canvas.drawText("• $finding", metadataX, metadataY, valuePaint)
                        metadataY += PDFLayoutConfig.LINE_SPACING_SMALL
                    }
                }

                // Caption
                photo.metadata.caption?.let {
                    metadataY += 4f
                    canvas.drawText("Caption:", metadataX, metadataY, labelPaint)
                    metadataY += PDFLayoutConfig.LINE_SPACING_SMALL
                    val captionResult = drawMultilineText(canvas, it, metadataX, metadataY,
                        PDFLayoutConfig.PHOTO_METADATA_WIDTH, valuePaint, PDFLayoutConfig.LINE_SPACING_SMALL)
                }
            }

            y += PDFLayoutConfig.PHOTO_HEIGHT + PDFLayoutConfig.SECTION_SPACING
        }

        return y
    }

    /**
     * Safely draw a bitmap with automatic resource cleanup.
     *
     * @param canvas Canvas to draw on
     * @param imageBytes Raw image bytes
     * @param destRect Destination rectangle for the bitmap
     * @param placeholder Optional placeholder text if bitmap fails to load
     * @return true if bitmap was drawn successfully, false otherwise
     */
    private fun drawBitmapSafe(
        canvas: Canvas,
        imageBytes: ByteArray,
        destRect: RectF,
        placeholder: String? = null
    ): Boolean {
        var bitmap: Bitmap? = null
        return try {
            // Validate image bytes
            if (imageBytes.isEmpty()) {
                drawPlaceholder(canvas, destRect, placeholder ?: "⚠ Photo data missing")
                return false
            }

            // Decode bitmap
            bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            if (bitmap == null) {
                drawPlaceholder(canvas, destRect, placeholder ?: "⚠ Photo corrupted")
                return false
            }

            // Draw bitmap
            canvas.drawBitmap(bitmap, null, destRect, null)
            true

        } catch (e: OutOfMemoryError) {
            drawPlaceholder(canvas, destRect, "⚠ Photo too large")
            false
        } catch (e: Exception) {
            drawPlaceholder(canvas, destRect, "⚠ Photo error")
            false
        } finally {
            bitmap?.recycle()
        }
    }

    /**
     * Draw a placeholder rectangle when an image fails to load.
     */
    private fun drawPlaceholder(
        canvas: Canvas,
        rect: RectF,
        message: String
    ) {
        val paint = Paint().apply {
            color = PDFLayoutConfig.COLOR_LIGHT_GRAY
            style = Paint.Style.FILL
        }
        canvas.drawRect(rect, paint)

        val textPaint = createTextPaint(
            PDFLayoutConfig.FONT_SIZE_BODY,
            PDFLayoutConfig.COLOR_DARK_GRAY,
            false
        )

        val textWidth = textPaint.measureText(message)
        val textX = rect.centerX() - (textWidth / 2)
        val textY = rect.centerY()

        canvas.drawText(message, textX, textY, textPaint)
    }

    /**
     * Draw emergency procedures section.
     */
    private fun drawEmergencyProcedures(canvas: Canvas, ptp: PreTaskPlan, content: PtpContent?, startY: Float): Float {
        var y = drawSectionHeader(canvas, "Emergency Procedures", startY)

        val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_DARK_GRAY, true)
        val bodyPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)

        // Emergency contacts
        if (ptp.emergencyContacts.isNotEmpty()) {
            canvas.drawText("Emergency Contacts:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY

            for (contact in ptp.emergencyContacts) {
                val contactText = "${contact.name} (${contact.role}): ${contact.phoneNumber}"
                canvas.drawText("• $contactText", PDFLayoutConfig.MARGIN_LEFT + 20f, y, bodyPaint)
                y += PDFLayoutConfig.LINE_SPACING_BODY
            }
            y += PDFLayoutConfig.SUBSECTION_SPACING
        }

        // Nearest hospital
        ptp.nearestHospital?.let {
            canvas.drawText("Nearest Hospital:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY
            canvas.drawText(it, PDFLayoutConfig.MARGIN_LEFT + 20f, y, bodyPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY + PDFLayoutConfig.SUBSECTION_SPACING
        }

        // Evacuation routes
        ptp.evacuationRoutes?.let {
            canvas.drawText("Evacuation Routes:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY
            val evacResult = drawMultilineText(canvas, it, PDFLayoutConfig.MARGIN_LEFT + 20f, y,
                PDFLayoutConfig.CONTENT_WIDTH - 20f, bodyPaint, PDFLayoutConfig.LINE_SPACING_BODY)
            y = evacResult.endY + PDFLayoutConfig.SUBSECTION_SPACING
        }

        // Emergency procedures from AI
        content?.emergencyProcedures?.let { procedures ->
            if (procedures.isNotEmpty()) {
                canvas.drawText("Emergency Response Procedures:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
                y += PDFLayoutConfig.LINE_SPACING_BODY

                for (procedure in procedures) {
                    canvas.drawText("• $procedure", PDFLayoutConfig.MARGIN_LEFT + 20f, y, bodyPaint)
                    y += PDFLayoutConfig.LINE_SPACING_BODY
                }
            }
        }

        y += PDFLayoutConfig.SECTION_SPACING
        return y
    }

    /**
     * Draw signatures section with lines for supervisor and all crew members.
     */
    private fun drawSignatures(canvas: Canvas, ptp: PreTaskPlan, startY: Float): Float {
        var y = drawSectionHeader(canvas, "Signatures & Approval", startY)

        val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_DARK_GRAY, true)
        val bodyPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)
        val linePaint = Paint().apply {
            color = PDFLayoutConfig.COLOR_DARK_GRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        // Supervisor signature
        ptp.signatureSupervisor?.let { signature ->
            canvas.drawText("Supervisor:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY

            // If signature blob exists, try to draw it
            signature.signatureBlob?.let { blob ->
                val sigRect = RectF(
                    PDFLayoutConfig.MARGIN_LEFT + 20f,
                    y,
                    PDFLayoutConfig.MARGIN_LEFT + 220f,
                    y + PDFLayoutConfig.SIGNATURE_LINE_HEIGHT
                )
                val sigDrawn = drawBitmapSafe(canvas, blob, sigRect, signature.supervisorName)
                if (sigDrawn) {
                    y += PDFLayoutConfig.SIGNATURE_LINE_HEIGHT + 10f
                } else {
                    // Fall back to text signature
                    canvas.drawText("Signature: ${signature.supervisorName}",
                        PDFLayoutConfig.MARGIN_LEFT + 20f, y, bodyPaint)
                    y += PDFLayoutConfig.LINE_SPACING_BODY
                }
            } ?: run {
                // Text signature
                canvas.drawText("Name: ${signature.supervisorName}",
                    PDFLayoutConfig.MARGIN_LEFT + 20f, y, bodyPaint)
                y += PDFLayoutConfig.LINE_SPACING_BODY
            }

            canvas.drawText("Date: ${formatDate(signature.signatureDate)}",
                PDFLayoutConfig.MARGIN_LEFT + 20f, y, bodyPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY + PDFLayoutConfig.SUBSECTION_SPACING
        }

        // Crew member signatures
        y += PDFLayoutConfig.SUBSECTION_SPACING
        canvas.drawText("Crew Members:", PDFLayoutConfig.MARGIN_LEFT, y, labelPaint)
        y += PDFLayoutConfig.LINE_SPACING_BODY

        val notePaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_DARK_GRAY, false)
        canvas.drawText("All crew members must sign to acknowledge they have reviewed this PTP and understand the hazards and safety procedures.",
            PDFLayoutConfig.MARGIN_LEFT, y, notePaint)
        y += PDFLayoutConfig.LINE_SPACING_BODY + PDFLayoutConfig.SUBSECTION_SPACING

        // Calculate number of signature lines (crew size + 2 additional workers)
        val crewSize = ptp.crewSize ?: 3
        val totalSignatureLines = crewSize + 2

        // Draw signature lines for crew members
        for (i in 1..totalSignatureLines) {
            // Worker number
            canvas.drawText("Worker $i:", PDFLayoutConfig.MARGIN_LEFT, y, bodyPaint)
            y += PDFLayoutConfig.LINE_SPACING_BODY

            // Name line
            val nameLineY = y + 10f
            canvas.drawText("Name:", PDFLayoutConfig.MARGIN_LEFT + 20f, nameLineY, bodyPaint)
            canvas.drawLine(
                PDFLayoutConfig.MARGIN_LEFT + 80f,
                nameLineY,
                PDFLayoutConfig.MARGIN_LEFT + PDFLayoutConfig.SIGNATURE_LINE_WIDTH,
                nameLineY,
                linePaint
            )

            // Date line (next to name)
            canvas.drawText("Date:", PDFLayoutConfig.MARGIN_LEFT + PDFLayoutConfig.SIGNATURE_LINE_WIDTH + 20f, nameLineY, bodyPaint)
            canvas.drawLine(
                PDFLayoutConfig.MARGIN_LEFT + PDFLayoutConfig.SIGNATURE_LINE_WIDTH + 70f,
                nameLineY,
                PDFLayoutConfig.MARGIN_LEFT + PDFLayoutConfig.SIGNATURE_LINE_WIDTH + 170f,
                nameLineY,
                linePaint
            )

            y = nameLineY + PDFLayoutConfig.LINE_SPACING_BODY

            // Signature line
            canvas.drawText("Signature:", PDFLayoutConfig.MARGIN_LEFT + 20f, y, bodyPaint)
            canvas.drawLine(
                PDFLayoutConfig.MARGIN_LEFT + 90f,
                y,
                PDFLayoutConfig.MARGIN_LEFT + PDFLayoutConfig.SIGNATURE_LINE_WIDTH + 100f,
                y,
                linePaint
            )

            y += PDFLayoutConfig.SIGNATURE_SPACING
        }

        return y
    }

    /**
     * Draw page footer with page number and generation info.
     * Enhanced with professional layout and branding.
     */
    private fun drawFooter(canvas: Canvas, pageNumber: Int, metadata: PDFMetadata) {
        val footerY = PDFLayoutConfig.PAGE_HEIGHT - PDFLayoutConfig.MARGIN_BOTTOM + 15f

        // Divider line (thicker, colored)
        val linePaint = Paint().apply {
            color = PDFLayoutConfig.COLOR_PRIMARY
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(
            PDFLayoutConfig.MARGIN_LEFT,
            footerY - 15f,
            PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT,
            footerY - 15f,
            linePaint
        )

        val footerPaint = createTextPaint(
            PDFLayoutConfig.FONT_SIZE_SMALL,
            PDFLayoutConfig.COLOR_DARK_GRAY,
            false
        )

        val boldFooterPaint = createTextPaint(
            PDFLayoutConfig.FONT_SIZE_SMALL,
            PDFLayoutConfig.COLOR_PRIMARY,
            true
        )

        // Left: Project name and task description
        val projectText = "${metadata.projectName} - ${metadata.taskDescription ?: "Pre-Task Plan"}"
        canvas.drawText(
            projectText,
            PDFLayoutConfig.MARGIN_LEFT,
            footerY,
            footerPaint
        )

        // Center: Page number (bold, colored)
        val pageText = "Page $pageNumber"
        val pageTextWidth = boldFooterPaint.measureText(pageText)
        canvas.drawText(
            pageText,
            (PDFLayoutConfig.PAGE_WIDTH - pageTextWidth) / 2,
            footerY,
            boldFooterPaint
        )

        // Right: Generation date
        val dateText = formatDate(metadata.generatedAt)
        val dateTextWidth = footerPaint.measureText(dateText)
        canvas.drawText(
            dateText,
            PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT - dateTextWidth,
            footerY,
            footerPaint
        )

        // Bottom right: Branding (subtle)
        val brandingY = footerY + 12f
        val brandingPaint = createTextPaint(
            PDFLayoutConfig.FONT_SIZE_SMALL - 2f,
            PDFLayoutConfig.COLOR_MEDIUM_GRAY,
            false
        )
        val brandingText = "Powered by HazardHawk AI"
        val brandingWidth = brandingPaint.measureText(brandingText)
        canvas.drawText(
            brandingText,
            PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT - brandingWidth,
            brandingY,
            brandingPaint
        )
    }

    /**
     * Draw a section header.
     */
    private fun drawSectionHeader(canvas: Canvas, title: String, y: Float): Float {
        val headerPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_HEADING, PDFLayoutConfig.COLOR_HEADER, true)
        canvas.drawText(title, PDFLayoutConfig.MARGIN_LEFT, y, headerPaint)

        // Underline
        val linePaint = Paint().apply {
            color = PDFLayoutConfig.COLOR_HEADER
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val lineY = y + 4f
        canvas.drawLine(PDFLayoutConfig.MARGIN_LEFT, lineY,
            PDFLayoutConfig.MARGIN_LEFT + headerPaint.measureText(title), lineY, linePaint)

        return y + PDFLayoutConfig.LINE_SPACING_HEADING + PDFLayoutConfig.SUBSECTION_SPACING
    }

    /**
     * Draw multiline text with word wrapping and enhanced edge case handling.
     *
     * @param canvas Canvas to draw on
     * @param text Text to draw (supports empty strings, long words, multiple spaces)
     * @param x X position to start drawing
     * @param startY Y position to start drawing
     * @param maxWidth Maximum width before wrapping
     * @param paint Paint to use for text
     * @param lineSpacing Line spacing between lines
     * @param maxLines Maximum number of lines to draw (optional)
     * @return TextDrawResult containing final Y position, lines drawn, and truncation info
     */
    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineSpacing: Float,
        maxLines: Int = Int.MAX_VALUE
    ): TextDrawResult {
        // Handle empty/blank text
        if (text.isBlank()) {
            return TextDrawResult(
                endY = startY,
                linesDrawn = 0,
                truncated = false,
                drawnText = ""
            )
        }

        var y = startY
        val words = text.trim().split(Regex("\\s+")) // Handle multiple spaces
        var currentLine = ""
        var linesDrawn = 0
        val drawnLines = mutableListOf<String>()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth > maxWidth) {
                if (currentLine.isEmpty()) {
                    // Word too long - break it character by character
                    val (brokenWord, remainder) = breakLongWord(word, maxWidth, paint)
                    if (linesDrawn < maxLines) {
                        canvas.drawText(brokenWord, x, y, paint)
                        drawnLines.add(brokenWord)
                        y += lineSpacing
                        linesDrawn++
                    }
                    currentLine = remainder
                } else {
                    // Draw current line
                    if (linesDrawn < maxLines) {
                        canvas.drawText(currentLine, x, y, paint)
                        drawnLines.add(currentLine)
                        y += lineSpacing
                        linesDrawn++
                    }
                    currentLine = word
                }
            } else {
                currentLine = testLine
            }

            // Check if we've reached max lines
            if (linesDrawn >= maxLines) {
                break
            }
        }

        // Draw last line
        if (currentLine.isNotEmpty() && linesDrawn < maxLines) {
            canvas.drawText(currentLine, x, y, paint)
            drawnLines.add(currentLine)
            y += lineSpacing
            linesDrawn++
        }

        val truncated = linesDrawn >= maxLines &&
                        drawnLines.joinToString(" ").length < text.length

        return TextDrawResult(
            endY = y,
            linesDrawn = linesDrawn,
            truncated = truncated,
            drawnText = drawnLines.joinToString("\n")
        )
    }

    /**
     * Result of drawing multiline text.
     */
    private data class TextDrawResult(
        val endY: Float,
        val linesDrawn: Int,
        val truncated: Boolean,
        val drawnText: String = ""
    )

    /**
     * Break a long word that doesn't fit on a single line.
     *
     * @param word Word to break
     * @param maxWidth Maximum width available
     * @param paint Paint to measure text with
     * @return Pair of (fitting part, remainder)
     */
    private fun breakLongWord(
        word: String,
        maxWidth: Float,
        paint: Paint
    ): Pair<String, String> {
        var breakPoint = word.length

        while (breakPoint > 0 && paint.measureText(word.substring(0, breakPoint)) > maxWidth) {
            breakPoint--
        }

        return if (breakPoint > 0) {
            word.substring(0, breakPoint) to word.substring(breakPoint)
        } else {
            word to ""
        }
    }

    /**
     * Paint cache to reduce object allocation during PDF generation.
     * Reuses Paint objects for common text styles.
     */
    private class PaintCache {
        private val cache = mutableMapOf<PaintKey, Paint>()

        data class PaintKey(
            val size: Float,
            val color: Int,
            val bold: Boolean
        )

        fun getPaint(size: Float, color: Int, bold: Boolean): Paint {
            val key = PaintKey(size, color, bold)
            return cache.getOrPut(key) {
                Paint().apply {
                    textSize = size
                    this.color = color
                    typeface = if (bold) {
                        Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    } else {
                        Typeface.DEFAULT
                    }
                    isAntiAlias = true
                }
            }
        }

        fun clear() {
            cache.clear()
        }
    }

    private val paintCache = PaintCache()

    /**
     * Create a Paint object for text with specified properties.
     * Uses cache to reduce object allocation.
     */
    private fun createTextPaint(size: Float, color: Int, bold: Boolean): Paint {
        return paintCache.getPaint(size, color, bold)
    }

    /**
     * Optimize photo data by downsampling large images.
     * Reduces memory usage while maintaining acceptable print quality.
     */
    private fun optimizePhotoData(photos: List<PhotoData>): List<PhotoData> {
        return photos.map { photo ->
            try {
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(photo.imageBytes, 0, photo.imageBytes.size, options)

                val maxDimension = 1200
                val sampleSize = calculateInSampleSize(
                    options.outWidth,
                    options.outHeight,
                    maxDimension,
                    maxDimension
                )

                if (sampleSize == 1) return@map photo

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }

                val bitmap = BitmapFactory.decodeByteArray(
                    photo.imageBytes, 0, photo.imageBytes.size, decodeOptions
                ) ?: return@map photo

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                bitmap.recycle()

                photo.copy(imageBytes = outputStream.toByteArray())
            } catch (e: Exception) {
                photo
            }
        }
    }

    /**
     * Calculate appropriate sample size for image downsampling.
     */
    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                   (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Format epoch milliseconds to human-readable date.
     */
    private fun formatDate(epochMillis: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.month.name.take(3)} ${dateTime.dayOfMonth}, ${dateTime.year}"
    }

    /**
     * Format epoch milliseconds to human-readable date and time.
     */
    private fun formatDateTime(epochMillis: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = if (dateTime.hour > 12) dateTime.hour - 12 else if (dateTime.hour == 0) 12 else dateTime.hour
        val amPm = if (dateTime.hour >= 12) "PM" else "AM"
        return "${dateTime.month.name.take(3)} ${dateTime.dayOfMonth}, ${dateTime.year} at $hour:${dateTime.minute.toString().padStart(2, '0')} $amPm"
    }

    /**
     * Draw professional cover page for PTP document.
     */
    private fun drawCoverPage(
        canvas: Canvas,
        ptp: PreTaskPlan,
        metadata: PDFMetadata
    ) {
        var y = PDFLayoutConfig.PAGE_HEIGHT * 0.15f

        // Company logo (larger, centered)
        metadata.companyLogo?.let { logoBytes ->
            val logoSize = 80f
            val logoX = (PDFLayoutConfig.PAGE_WIDTH - logoSize) / 2

            drawBitmapSafe(
                canvas,
                logoBytes,
                RectF(logoX, y, logoX + logoSize, y + logoSize),
                placeholder = null
            )

            y += logoSize + 30f
        }

        // Title
        val titlePaint = createTextPaint(
            28f,
            PDFLayoutConfig.COLOR_PRIMARY,
            true
        )
        titlePaint.textAlign = Paint.Align.CENTER

        canvas.drawText(
            "PRE-TASK SAFETY PLAN",
            PDFLayoutConfig.PAGE_WIDTH / 2,
            y,
            titlePaint
        )

        y += 50f

        // Divider
        val linePaint = Paint().apply {
            color = PDFLayoutConfig.COLOR_PRIMARY
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(
            PDFLayoutConfig.PAGE_WIDTH * 0.25f,
            y,
            PDFLayoutConfig.PAGE_WIDTH * 0.75f,
            y,
            linePaint
        )

        y += 40f

        // Project details
        val detailsPaint = createTextPaint(
            14f,
            PDFLayoutConfig.COLOR_BLACK,
            false
        )
        detailsPaint.textAlign = Paint.Align.CENTER

        val centerX = PDFLayoutConfig.PAGE_WIDTH / 2

        canvas.drawText("Project: ${metadata.projectName}", centerX, y, detailsPaint)
        y += 24f

        canvas.drawText("Work Type: ${ptp.workType}", centerX, y, detailsPaint)
        y += 24f

        canvas.drawText("Date: ${formatDate(ptp.createdAt)}", centerX, y, detailsPaint)
        y += 24f

        canvas.drawText("Crew Size: ${ptp.crewSize ?: "TBD"}", centerX, y, detailsPaint)
        y += 50f

        // Competent person
        val boldDetailsPaint = createTextPaint(14f, PDFLayoutConfig.COLOR_BLACK, true)
        boldDetailsPaint.textAlign = Paint.Align.CENTER

        canvas.drawText("Competent Person:", centerX, y, boldDetailsPaint)
        y += 20f

        canvas.drawText(
            metadata.competentPerson ?: "TBD",
            centerX,
            y,
            detailsPaint
        )

        y += 50f

        // Hazard summary
        val content = ptp.userModifiedContent ?: ptp.aiGeneratedContent
        content?.let {
            val hazardCounts = it.hazards.groupBy { h -> h.severity }

            // Critical hazards
            hazardCounts[HazardSeverity.CRITICAL]?.size?.let { count ->
                val criticalPaint = createTextPaint(16f, PDFLayoutConfig.COLOR_CRITICAL, true)
                criticalPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("⚠️ CRITICAL HAZARDS: $count", centerX, y, criticalPaint)
                y += 28f
            }

            // Major hazards
            hazardCounts[HazardSeverity.MAJOR]?.size?.let { count ->
                val majorPaint = createTextPaint(16f, PDFLayoutConfig.COLOR_MAJOR, true)
                majorPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("⚡ MAJOR HAZARDS: $count", centerX, y, majorPaint)
                y += 28f
            }

            // Minor hazards
            hazardCounts[HazardSeverity.MINOR]?.size?.let { count ->
                val minorPaint = createTextPaint(16f, PDFLayoutConfig.COLOR_MINOR, true)
                minorPaint.textAlign = Paint.Align.CENTER
                canvas.drawText("ℹ️ MINOR HAZARDS: $count", centerX, y, minorPaint)
                y += 28f
            }

            y += 20f

            // Document stats
            canvas.drawText("📋 Total Job Steps: ${it.jobSteps.size}", centerX, y, detailsPaint)
            y += 24f
        }

        // Footer with generation info
        val footerY = PDFLayoutConfig.PAGE_HEIGHT - 80f
        val footerPaint = createTextPaint(
            PDFLayoutConfig.FONT_SIZE_SMALL,
            PDFLayoutConfig.COLOR_MEDIUM_GRAY,
            false
        )
        footerPaint.textAlign = Paint.Align.CENTER

        canvas.drawText(
            "Generated by HazardHawk AI",
            centerX,
            footerY,
            footerPaint
        )

        canvas.drawText(
            formatDateTime(metadata.generatedAt),
            centerX,
            footerY + 16f,
            footerPaint
        )
    }
}
