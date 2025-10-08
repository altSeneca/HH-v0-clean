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
        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(
                PDFLayoutConfig.PAGE_WIDTH.toInt(),
                PDFLayoutConfig.PAGE_HEIGHT.toInt(),
                1
            ).create()

            var currentPage = 1

            val content = ptp.userModifiedContent ?: ptp.aiGeneratedContent
            val hazards = content?.hazards ?: emptyList()

            // Page 1: Executive Summary - 5 minute overview
            val summaryPage = document.startPage(pageInfo)
            var yPosition = drawHeader(summaryPage.canvas, metadata, currentPage)
            yPosition = drawExecutiveSummary(summaryPage.canvas, ptp, hazards, metadata, yPosition)
            drawFooter(summaryPage.canvas, currentPage, metadata)
            document.finishPage(summaryPage)
            currentPage++

            // Page 2: Full Project Info + Work Scope + Hazards (part 1)
            val page2 = document.startPage(pageInfo)
            yPosition = drawHeader(page2.canvas, metadata, currentPage)
            yPosition = drawProjectInfo(page2.canvas, ptp, metadata, yPosition)
            yPosition = drawWorkScope(page2.canvas, ptp, yPosition)

            if (hazards.isNotEmpty()) {
                yPosition = drawHazardsSection(page2.canvas, hazards, yPosition, page2.info.pageHeight.toFloat())
            }

            drawFooter(page2.canvas, currentPage, metadata)
            document.finishPage(page2)
            currentPage++

            // Additional pages for remaining hazards if needed
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

            // Job Steps pages
            val jobSteps = content?.jobSteps ?: emptyList()
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

            // Photo pages (2 photos per page)
            if (photos.isNotEmpty()) {
                for (photoGroup in photos.chunked(PDFLayoutConfig.PHOTOS_PER_PAGE)) {
                    val photoPage = document.startPage(pageInfo)
                    var photoY = PDFLayoutConfig.MARGIN_TOP
                    photoY = drawSectionHeader(photoPage.canvas, "Photo Documentation", photoY)
                    drawPhotos(photoPage.canvas, photoGroup, photoY)
                    drawFooter(photoPage.canvas, currentPage, metadata)
                    document.finishPage(photoPage)
                    currentPage++
                }
            }

            // Final page: Emergency Procedures + Signatures
            val finalPage = document.startPage(pageInfo)
            var finalY = PDFLayoutConfig.MARGIN_TOP
            finalY = drawEmergencyProcedures(finalPage.canvas, ptp, content, finalY)
            drawSignatures(finalPage.canvas, ptp, finalY)
            drawFooter(finalPage.canvas, currentPage, metadata)
            document.finishPage(finalPage)

            // Convert to ByteArray
            val outputStream = ByteArrayOutputStream()
            document.writeTo(outputStream)
            document.close()

            Result.success(outputStream.toByteArray())
        } catch (e: Exception) {
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
            y = drawMultilineText(
                canvas,
                simplifyForWorkers(hazard.description),
                PDFLayoutConfig.MARGIN_LEFT + 50f,
                y,
                PDFLayoutConfig.CONTENT_WIDTH - 50f,
                descPaint,
                PDFLayoutConfig.LINE_SPACING_BODY
            )
            y += 5f

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
     */
    private fun drawHeader(canvas: Canvas, metadata: PDFMetadata, pageNumber: Int): Float {
        var y = PDFLayoutConfig.MARGIN_TOP

        // Company logo (if provided)
        metadata.companyLogo?.let { logoBytes ->
            try {
                val bitmap = BitmapFactory.decodeByteArray(logoBytes, 0, logoBytes.size)
                val logoHeight = 40f
                val logoWidth = (logoHeight / bitmap.height) * bitmap.width
                val destRect = RectF(PDFLayoutConfig.MARGIN_LEFT, y,
                    PDFLayoutConfig.MARGIN_LEFT + logoWidth, y + logoHeight)
                canvas.drawBitmap(bitmap, null, destRect, null)
                bitmap.recycle()
            } catch (e: Exception) {
                // Skip logo if decoding fails
            }
        }

        // Title
        val titlePaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_TITLE, PDFLayoutConfig.COLOR_HEADER, true)
        val titleX = if (metadata.companyLogo != null) {
            PDFLayoutConfig.MARGIN_LEFT + 120f
        } else {
            PDFLayoutConfig.MARGIN_LEFT
        }
        canvas.drawText("PRE-TASK PLAN", titleX, y + 24f, titlePaint)
        y += 50f

        // Company and project info
        val infoPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_DARK_GRAY, false)
        canvas.drawText("${metadata.companyName} | ${metadata.projectName}", PDFLayoutConfig.MARGIN_LEFT, y, infoPaint)
        y += PDFLayoutConfig.LINE_SPACING_SMALL

        // Divider line
        val linePaint = Paint().apply {
            color = PDFLayoutConfig.COLOR_LIGHT_GRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(PDFLayoutConfig.MARGIN_LEFT, y,
            PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT, y, linePaint)
        y += PDFLayoutConfig.SECTION_SPACING

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
        y = drawMultilineText(canvas, ptp.workScope, PDFLayoutConfig.MARGIN_LEFT, y,
            PDFLayoutConfig.CONTENT_WIDTH, bodyPaint, PDFLayoutConfig.LINE_SPACING_BODY)

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

        // Calculate how many hazards fit on this page
        val hazardsPerPage = calculateHazardsPerPage()
        val hazardsToShow = hazards.take(hazardsPerPage)

        return drawHazardList(canvas, hazardsToShow, y, pageHeight)
    }

    /**
     * Draw a list of hazards with color-coded severity and proper page handling.
     */
    private fun drawHazardList(canvas: Canvas, hazards: List<PtpHazard>, startY: Float, pageHeight: Float): Float {
        var y = startY
        val maxY = pageHeight - PDFLayoutConfig.MARGIN_BOTTOM - 50f // Leave room for footer

        for (hazard in hazards) {
            // Calculate exact height needed for this hazard
            val boxHeight = calculateHazardBoxHeight(hazard)

            // Check if hazard fits on current page - if not, stop (will continue on next page)
            if (y + boxHeight > maxY) {
                break
            }

            // Draw hazard box with colored border
            val boxPaint = Paint().apply {
                color = when (hazard.severity) {
                    HazardSeverity.CRITICAL -> PDFLayoutConfig.COLOR_CRITICAL
                    HazardSeverity.MAJOR -> PDFLayoutConfig.COLOR_MAJOR
                    HazardSeverity.MINOR -> PDFLayoutConfig.COLOR_MINOR
                }
                style = Paint.Style.STROKE
                strokeWidth = PDFLayoutConfig.HAZARD_BOX_BORDER_WIDTH
            }

            val boxTop = y
            val boxRect = RectF(
                PDFLayoutConfig.MARGIN_LEFT,
                boxTop,
                PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT,
                boxTop + boxHeight
            )

            // Background fill
            val bgPaint = Paint().apply {
                color = 0xFFF5F5F5.toInt() // Light gray background
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(boxRect, PDFLayoutConfig.HAZARD_BOX_BORDER_RADIUS, PDFLayoutConfig.HAZARD_BOX_BORDER_RADIUS, bgPaint)

            // Draw colored border on top
            canvas.drawRoundRect(boxRect, PDFLayoutConfig.HAZARD_BOX_BORDER_RADIUS, PDFLayoutConfig.HAZARD_BOX_BORDER_RADIUS, boxPaint)

            // Content inside box with proper padding
            var boxY = boxTop + PDFLayoutConfig.HAZARD_BOX_PADDING_TOP
            val contentX = PDFLayoutConfig.MARGIN_LEFT + PDFLayoutConfig.HAZARD_BOX_PADDING_LEFT

            // Visual severity icon + OSHA Code
            val icon = when(hazard.severity) {
                HazardSeverity.CRITICAL -> "⚠"
                HazardSeverity.MAJOR -> "⚡"
                HazardSeverity.MINOR -> "ℹ"
            }
            val headerPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_LARGE, PDFLayoutConfig.COLOR_BLACK, true)
            canvas.drawText("$icon OSHA ${hazard.oshaCode}", contentX, boxY, headerPaint)

            val severityPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_LARGE,
                when (hazard.severity) {
                    HazardSeverity.CRITICAL -> PDFLayoutConfig.COLOR_CRITICAL
                    HazardSeverity.MAJOR -> PDFLayoutConfig.COLOR_MAJOR
                    HazardSeverity.MINOR -> PDFLayoutConfig.COLOR_MINOR
                }, true)
            canvas.drawText(hazard.severity.name, contentX + 250f, boxY, severityPaint)
            boxY += PDFLayoutConfig.LINE_SPACING_BODY + 5f

            // Description with proper text wrapping and simplified language
            val bodyPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)
            val descriptionWidth = PDFLayoutConfig.CONTENT_WIDTH - PDFLayoutConfig.HAZARD_BOX_PADDING_LEFT - PDFLayoutConfig.HAZARD_BOX_PADDING_RIGHT
            boxY = drawMultilineText(canvas, simplifyForWorkers(hazard.description), contentX, boxY,
                descriptionWidth, bodyPaint, PDFLayoutConfig.LINE_SPACING_BODY)
            boxY += PDFLayoutConfig.SUBSECTION_SPACING

            // Controls as numbered action steps
            if (hazard.controls.isNotEmpty()) {
                val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, 0xFF006600.toInt(), true)
                canvas.drawText("SAFETY STEPS - DO THESE:", contentX, boxY, labelPaint)
                boxY += PDFLayoutConfig.LINE_SPACING_BODY

                val smallPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)
                val controlWidth = descriptionWidth - 30f // Account for number indent
                for ((index, control) in hazard.controls.withIndex()) {
                    val actionControl = makeActionOriented(simplifyForWorkers(control))
                    boxY = drawMultilineText(canvas, "${index + 1}. $actionControl", contentX + 10f, boxY,
                        controlWidth, smallPaint, PDFLayoutConfig.LINE_SPACING_BODY)
                }
            }

            // Required PPE
            if (hazard.requiredPpe.isNotEmpty()) {
                boxY += 2f
                val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_DARK_GRAY, true)
                canvas.drawText("Required PPE: ${hazard.requiredPpe.joinToString(", ")}",
                    contentX, boxY, labelPaint)
            }

            y = boxTop + boxHeight + PDFLayoutConfig.SUBSECTION_SPACING
        }

        return y
    }

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
            // Draw photo on left side
            try {
                val bitmap = BitmapFactory.decodeByteArray(photo.imageBytes, 0, photo.imageBytes.size)
                val scaledHeight = PDFLayoutConfig.PHOTO_HEIGHT
                val scaledWidth = (scaledHeight / bitmap.height) * bitmap.width
                val photoRect = RectF(
                    PDFLayoutConfig.MARGIN_LEFT,
                    y,
                    PDFLayoutConfig.MARGIN_LEFT + scaledWidth.coerceAtMost(PDFLayoutConfig.PHOTO_WIDTH),
                    y + scaledHeight
                )
                canvas.drawBitmap(bitmap, null, photoRect, null)
                bitmap.recycle()

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
                    drawMultilineText(canvas, it, metadataX, metadataY,
                        PDFLayoutConfig.PHOTO_METADATA_WIDTH, valuePaint, PDFLayoutConfig.LINE_SPACING_SMALL)
                }

                y += PDFLayoutConfig.PHOTO_HEIGHT + PDFLayoutConfig.SECTION_SPACING
            } catch (e: Exception) {
                // Skip photo if decoding fails
                y += 20f
            }
        }

        return y
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
            y = drawMultilineText(canvas, it, PDFLayoutConfig.MARGIN_LEFT + 20f, y,
                PDFLayoutConfig.CONTENT_WIDTH - 20f, bodyPaint, PDFLayoutConfig.LINE_SPACING_BODY)
            y += PDFLayoutConfig.SUBSECTION_SPACING
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
                try {
                    val bitmap = BitmapFactory.decodeByteArray(blob, 0, blob.size)
                    val sigRect = RectF(
                        PDFLayoutConfig.MARGIN_LEFT + 20f,
                        y,
                        PDFLayoutConfig.MARGIN_LEFT + 220f,
                        y + PDFLayoutConfig.SIGNATURE_LINE_HEIGHT
                    )
                    canvas.drawBitmap(bitmap, null, sigRect, null)
                    bitmap.recycle()
                    y += PDFLayoutConfig.SIGNATURE_LINE_HEIGHT + 10f
                } catch (e: Exception) {
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
     */
    private fun drawFooter(canvas: Canvas, pageNumber: Int, metadata: PDFMetadata) {
        val footerY = PDFLayoutConfig.PAGE_HEIGHT - PDFLayoutConfig.MARGIN_BOTTOM + 10f

        // Divider line
        val linePaint = Paint().apply {
            color = PDFLayoutConfig.COLOR_LIGHT_GRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(
            PDFLayoutConfig.MARGIN_LEFT,
            footerY - 10f,
            PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT,
            footerY - 10f,
            linePaint
        )

        val footerPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_DARK_GRAY, false)

        // Page number (center)
        val pageText = "Page $pageNumber"
        canvas.drawText(pageText, PDFLayoutConfig.PAGE_WIDTH / 2 - 30f, footerY, footerPaint)

        // Generated by (left)
        canvas.drawText("Generated by ${metadata.generatedBy}", PDFLayoutConfig.MARGIN_LEFT, footerY, footerPaint)

        // Generation date (right)
        val dateText = formatDate(metadata.generatedAt)
        val dateTextWidth = footerPaint.measureText(dateText)
        canvas.drawText(dateText, PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT - dateTextWidth,
            footerY, footerPaint)
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
     * Draw multiline text with word wrapping.
     */
    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineSpacing: Float
    ): Float {
        var y = startY
        val words = text.split(" ")
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val textWidth = paint.measureText(testLine)

            if (textWidth > maxWidth) {
                // Draw current line if not empty
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, x, y, paint)
                    y += lineSpacing
                }

                // Handle word that's too long - break at character level
                if (paint.measureText(word) > maxWidth) {
                    var remainingWord = word
                    while (remainingWord.isNotEmpty()) {
                        var charCount = 0
                        var fittingText = ""

                        // Find max characters that fit
                        for (char in remainingWord) {
                            val test = fittingText + char
                            if (paint.measureText(test) <= maxWidth) {
                                fittingText = test
                                charCount++
                            } else {
                                break
                            }
                        }

                        if (charCount > 0) {
                            canvas.drawText(fittingText, x, y, paint)
                            y += lineSpacing
                            remainingWord = remainingWord.substring(charCount)
                        } else {
                            // Single character exceeds width - force draw anyway
                            canvas.drawText(remainingWord.take(1), x, y, paint)
                            y += lineSpacing
                            remainingWord = remainingWord.drop(1)
                        }
                    }
                    currentLine = ""
                } else {
                    currentLine = word
                }
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, y, paint)
            y += lineSpacing
        }

        return y
    }

    /**
     * Create a Paint object for text with specified properties.
     */
    private fun createTextPaint(size: Float, color: Int, bold: Boolean): Paint {
        return Paint().apply {
            textSize = size
            this.color = color
            isAntiAlias = true
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    /**
     * Format epoch milliseconds to human-readable date.
     */
    private fun formatDate(epochMillis: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.month.name.take(3)} ${dateTime.dayOfMonth}, ${dateTime.year}"
    }
}
