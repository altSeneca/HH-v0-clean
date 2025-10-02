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

            // Page 1: Header + Project Info + Work Scope + Hazards (part 1)
            val page1 = document.startPage(pageInfo)
            var yPosition = drawHeader(page1.canvas, metadata, currentPage)
            yPosition = drawProjectInfo(page1.canvas, ptp, metadata, yPosition)
            yPosition = drawWorkScope(page1.canvas, ptp, yPosition)

            val content = ptp.userModifiedContent ?: ptp.aiGeneratedContent
            val hazards = content?.hazards ?: emptyList()

            if (hazards.isNotEmpty()) {
                yPosition = drawHazardsSection(page1.canvas, hazards, yPosition, page1.info.pageHeight.toFloat())
            }

            drawFooter(page1.canvas, currentPage, metadata)
            document.finishPage(page1)
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
     * Draw a list of hazards with color-coded severity.
     */
    private fun drawHazardList(canvas: Canvas, hazards: List<PtpHazard>, startY: Float, pageHeight: Float): Float {
        var y = startY
        val maxY = pageHeight - PDFLayoutConfig.MARGIN_BOTTOM - 40f // Leave room for footer

        for (hazard in hazards) {
            if (y > maxY - 100f) break // Not enough space for hazard

            // Draw hazard box with colored border
            val boxPaint = Paint().apply {
                color = when (hazard.severity) {
                    HazardSeverity.CRITICAL -> PDFLayoutConfig.COLOR_CRITICAL
                    HazardSeverity.MAJOR -> PDFLayoutConfig.COLOR_MAJOR
                    HazardSeverity.MINOR -> PDFLayoutConfig.COLOR_MINOR
                }
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

            val boxTop = y
            val boxHeight = calculateHazardBoxHeight(hazard)
            val boxRect = RectF(
                PDFLayoutConfig.MARGIN_LEFT,
                boxTop,
                PDFLayoutConfig.PAGE_WIDTH - PDFLayoutConfig.MARGIN_RIGHT,
                boxTop + boxHeight
            )
            canvas.drawRoundRect(boxRect, 8f, 8f, boxPaint)

            // Background fill
            val bgPaint = Paint().apply {
                color = 0xFFF5F5F5.toInt() // Light gray background
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(boxRect, 8f, 8f, bgPaint)

            // Draw border again on top
            canvas.drawRoundRect(boxRect, 8f, 8f, boxPaint)

            // Content inside box
            var boxY = boxTop + 15f
            val contentX = PDFLayoutConfig.MARGIN_LEFT + 10f

            // OSHA Code and Severity
            val headerPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, true)
            canvas.drawText("OSHA ${hazard.oshaCode}", contentX, boxY, headerPaint)

            val severityPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY,
                when (hazard.severity) {
                    HazardSeverity.CRITICAL -> PDFLayoutConfig.COLOR_CRITICAL
                    HazardSeverity.MAJOR -> PDFLayoutConfig.COLOR_MAJOR
                    HazardSeverity.MINOR -> PDFLayoutConfig.COLOR_MINOR
                }, true)
            canvas.drawText(hazard.severity.name, contentX + 250f, boxY, severityPaint)
            boxY += PDFLayoutConfig.LINE_SPACING_BODY + 2f

            // Description
            val bodyPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)
            boxY = drawMultilineText(canvas, hazard.description, contentX, boxY,
                PDFLayoutConfig.CONTENT_WIDTH - 20f, bodyPaint, PDFLayoutConfig.LINE_SPACING_BODY)
            boxY += PDFLayoutConfig.SUBSECTION_SPACING

            // Controls
            if (hazard.controls.isNotEmpty()) {
                val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_DARK_GRAY, true)
                canvas.drawText("Controls:", contentX, boxY, labelPaint)
                boxY += PDFLayoutConfig.LINE_SPACING_SMALL

                for (control in hazard.controls) {
                    val smallPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_BLACK, false)
                    canvas.drawText("• $control", contentX + 10f, boxY, smallPaint)
                    boxY += PDFLayoutConfig.LINE_SPACING_SMALL
                }
            }

            // Required PPE
            if (hazard.requiredPpe.isNotEmpty()) {
                boxY += 2f
                val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_DARK_GRAY, true)
                canvas.drawText("Required PPE: ${hazard.requiredPpe.joinToString(", ")}",
                    contentX, boxY, labelPaint)
                boxY += PDFLayoutConfig.LINE_SPACING_SMALL
            }

            y = boxTop + boxHeight + PDFLayoutConfig.SUBSECTION_SPACING
        }

        return y
    }

    /**
     * Calculate approximate height needed for a hazard box.
     */
    private fun calculateHazardBoxHeight(hazard: PtpHazard): Float {
        var height = 50f // Base height for header and description
        height += hazard.controls.size * PDFLayoutConfig.LINE_SPACING_SMALL
        if (hazard.requiredPpe.isNotEmpty()) height += PDFLayoutConfig.LINE_SPACING_SMALL + 4f
        return height.coerceAtLeast(80f)
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
     * Draw signatures section.
     */
    private fun drawSignatures(canvas: Canvas, ptp: PreTaskPlan, startY: Float): Float {
        var y = drawSectionHeader(canvas, "Signatures & Approval", startY)

        val labelPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_DARK_GRAY, true)
        val bodyPaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_BODY, PDFLayoutConfig.COLOR_BLACK, false)

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
                        y + 60f
                    )
                    canvas.drawBitmap(bitmap, null, sigRect, null)
                    bitmap.recycle()
                    y += 70f
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
            y += PDFLayoutConfig.LINE_SPACING_BODY
        }

        // Crew acknowledgment note
        y += PDFLayoutConfig.SUBSECTION_SPACING
        val notePaint = createTextPaint(PDFLayoutConfig.FONT_SIZE_SMALL, PDFLayoutConfig.COLOR_DARK_GRAY, false)
        canvas.drawText("Crew Acknowledgment: This document is to be printed, reviewed, and signed by all crew members on-site.",
            PDFLayoutConfig.MARGIN_LEFT, y, notePaint)

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
                if (currentLine.isNotEmpty()) {
                    canvas.drawText(currentLine, x, y, paint)
                    y += lineSpacing
                }
                currentLine = word
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
