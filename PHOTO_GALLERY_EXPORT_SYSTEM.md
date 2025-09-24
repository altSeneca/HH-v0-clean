# HazardHawk Photo Gallery & Export System

## Overview
Complete photo management system with gallery views, detailed analysis display, and comprehensive export capabilities for safety documentation.

## Photo Gallery System

### Gallery Views

#### 1. List View (Primary)
```kotlin
@Composable
fun PhotoGalleryListView() {
    LazyColumn {
        // Group by date with headers
        items(photosByDate) { dateGroup ->
            DateHeader(date = dateGroup.date)
            
            dateGroup.photos.forEach { photo ->
                PhotoListItem(
                    photo = photo,
                    thumbnail = photo.thumbnailUrl,
                    timestamp = photo.timestamp,
                    project = photo.project.name,
                    complianceStatus = photo.complianceStatus,
                    tags = photo.tags,
                    hasAIAnalysis = photo.analysis != null,
                    onClick = { navigateToDetail(photo.id) }
                )
            }
        }
    }
}
```

**List Item Display:**
- Thumbnail (left side, 80x80dp)
- Project name and timestamp
- Compliance badge (✓ Compliant or ⚠ Needs Improvement)
- Tags displayed as chips
- AI analysis indicator (if processed)
- Sync status icon

#### 2. Grid View (Optional Toggle)
```kotlin
@Composable
fun PhotoGalleryGridView() {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp)
    ) {
        items(photos) { photo ->
            PhotoGridItem(
                photo = photo,
                showBadges = true, // Compliance & AI status
                onClick = { navigateToDetail(photo.id) }
            )
        }
    }
}
```

#### 3. Filter & Search Bar
```kotlin
@Composable
fun GalleryFilterBar() {
    Row {
        // Project Filter
        ProjectFilterChip(
            selected = selectedProject,
            onSelectionChange = { filterByProject(it) }
        )
        
        // Date Range
        DateRangeChip(
            startDate = filterStartDate,
            endDate = filterEndDate,
            onDateRangeSelected = { start, end -> 
                filterByDateRange(start, end)
            }
        )
        
        // Compliance Status
        ComplianceFilterChip(
            showCompliant = showCompliant,
            showNeedsImprovement = showNeedsImprovement,
            onToggle = { updateComplianceFilter(it) }
        )
        
        // Tag Filter
        TagFilterChip(
            selectedTags = selectedTags,
            onTagsSelected = { filterByTags(it) }
        )
        
        // Search
        SearchField(
            query = searchQuery,
            onQueryChange = { searchPhotos(it) }
        )
    }
}
```

### Photo Detail View

#### Full Screen Photo Viewer
```kotlin
@Composable
fun PhotoDetailScreen(photoId: String) {
    val photo = viewModel.getPhoto(photoId)
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar with Actions
        TopAppBar(
            title = { Text("Photo Details") },
            navigationIcon = { BackButton() },
            actions = {
                IconButton(onClick = { sharePhoto(photo) }) {
                    Icon(Icons.Default.Share)
                }
                IconButton(onClick = { exportPhoto(photo) }) {
                    Icon(Icons.Default.Download)
                }
                IconButton(onClick = { deletePhoto(photo) }) {
                    Icon(Icons.Default.Delete)
                }
            }
        )
        
        // Zoomable Photo
        ZoomablePhotoView(
            photo = photo,
            modifier = Modifier.weight(1f)
        )
        
        // Swipeable Info Panels
        PhotoInfoPager(
            panels = listOf(
                MetadataPanel(photo),
                TagsPanel(photo),
                AIAnalysisPanel(photo),
                OSHACodesPanel(photo)
            )
        )
    }
}
```

#### Info Panels

**1. Metadata Panel**
```
📍 Location: Michie Stadium, West Point, NY
📅 Date: January 26, 2025, 9:30 AM
👷 Captured by: Aaron (Safety Manager)
🏗️ Project: Stadium Renovation Phase 2
📱 Device: Pixel 8 Pro
```

**2. Tags & Compliance Panel**
```
Status: ⚠ Needs Improvement

Applied Tags:
[PPE] [Fall Protection] [Scaffolding] [Missing Guardrail]

AI Suggested Tags:
[Working at Height] [OSHA 1926.451]
```

**3. AI Analysis Panel**
```
🤖 AI Safety Analysis

Hazards Detected:
• Missing guardrail on scaffold platform (HIGH RISK)
• Worker without fall arrest system at 15ft
• Improper scaffold access point

Severity: HIGH

Recommended Actions:
1. Stop work immediately
2. Install guardrails meeting 1926.451(g)(4) requirements
3. Ensure 100% tie-off above 6 feet
```

**4. OSHA Codes Panel**
```
📋 Applicable OSHA Standards

1926.451(g)(4) - Guardrail Requirements
"Guardrail systems shall be installed along all open sides 
and ends of platforms more than 10 feet above ground."

1926.502(d) - Personal Fall Arrest Systems
"Personal fall arrest systems shall be rigged such that 
an employee can neither free fall more than 6 feet..."

[Tap code for full text] [Link to OSHA.gov]
```

## Export & Reporting System

### Export Options

#### 1. Daily Safety Report (HTML)
```kotlin
fun generateDailyReport(date: Date): File {
    val photos = photoRepository.getPhotosByDate(date)
    val html = buildString {
        append("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Safety Report - ${date.format()}</title>
                <style>
                    body { font-family: Arial, sans-serif; max-width: 1200px; margin: 0 auto; }
                    .header { background: #2c3e50; color: white; padding: 20px; }
                    .photo-section { margin: 20px 0; border: 1px solid #ddd; padding: 15px; }
                    .photo-img { max-width: 600px; }
                    .compliance-badge { padding: 5px 10px; border-radius: 5px; }
                    .compliant { background: #27ae60; color: white; }
                    .needs-improvement { background: #f39c12; color: white; }
                    .hazard-list { background: #fee; padding: 10px; margin: 10px 0; }
                    .osha-code { background: #f0f0f0; padding: 8px; margin: 5px 0; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>Safety Inspection Report</h1>
                    <p>Project: ${project.name}</p>
                    <p>Date: ${date.format()}</p>
                    <p>Inspector: ${user.name}</p>
                </div>
        """)
        
        photos.forEach { photo ->
            append("""
                <div class="photo-section">
                    <h2>Photo ${photo.number} - ${photo.timestamp}</h2>
                    <img src="${photo.base64Image}" class="photo-img" />
                    <div class="metadata">
                        <p><strong>Location:</strong> ${photo.location}</p>
                        <span class="compliance-badge ${photo.complianceClass}">
                            ${photo.complianceStatus}
                        </span>
                    </div>
                    <div class="tags">
                        <strong>Tags:</strong> ${photo.tags.joinToString(", ")}
                    </div>
                    ${if (photo.analysis != null) """
                        <div class="analysis">
                            <h3>AI Safety Analysis</h3>
                            <div class="hazard-list">
                                ${photo.analysis.hazards.joinToString("<br>")}
                            </div>
                            <h4>OSHA Codes:</h4>
                            ${photo.analysis.oshaCodes.map { code ->
                                """<div class="osha-code">
                                    <strong>${code.number}</strong>: ${code.description}
                                </div>"""
                            }.joinToString("")}
                        </div>
                    """ else ""}
                </div>
            """)
        }
        
        append("""
                <div class="summary">
                    <h2>Summary</h2>
                    <p>Total Photos: ${photos.size}</p>
                    <p>Compliant: ${photos.count { it.isCompliant }}</p>
                    <p>Needs Improvement: ${photos.count { !it.isCompliant }}</p>
                    <p>High Risk Issues: ${photos.count { it.severity == "HIGH" }}</p>
                </div>
            </body>
            </html>
        """)
    }
    
    return saveToFile(html, "safety_report_${date}.html")
}
```

#### 2. PDF Export
```kotlin
fun exportAsPDF(photos: List<Photo>, options: ExportOptions): File {
    val document = PdfDocument()
    
    photos.forEach { photo ->
        val page = document.startPage(PageInfo(595, 842)) // A4
        val canvas = page.canvas
        
        // Draw photo
        canvas.drawBitmap(photo.bitmap, null, photoRect, paint)
        
        // Add metadata overlay
        canvas.drawText("Project: ${photo.project}", 50, 100, textPaint)
        canvas.drawText("Date: ${photo.timestamp}", 50, 120, textPaint)
        
        // Add compliance status
        drawComplianceBadge(canvas, photo.complianceStatus)
        
        // Add tags
        drawTags(canvas, photo.tags)
        
        // Add AI analysis if available
        photo.analysis?.let {
            drawAnalysis(canvas, it)
        }
        
        document.finishPage(page)
    }
    
    return savePDF(document, "safety_photos_${Date.now()}.pdf")
}
```

#### 3. Excel/CSV Export
```kotlin
fun exportToCSV(photos: List<Photo>): File {
    val csv = buildString {
        // Header row
        appendLine("Date,Time,Project,Location,Compliance,Tags,Hazards,OSHA Codes,Severity")
        
        // Data rows
        photos.forEach { photo ->
            append("${photo.date},")
            append("${photo.time},")
            append("\"${photo.project}\",")
            append("\"${photo.location}\",")
            append("${photo.complianceStatus},")
            append("\"${photo.tags.joinToString(";")}\",")
            append("\"${photo.analysis?.hazards?.joinToString(";") ?: ""}\",")
            append("\"${photo.analysis?.oshaCodes?.joinToString(";") ?: ""}\",")
            appendLine("${photo.analysis?.severity ?: ""}")
        }
    }
    
    return saveToFile(csv, "safety_data_${Date.now()}.csv")
}
```

#### 4. Share Options
```kotlin
fun sharePhoto(photo: Photo, format: ShareFormat) {
    when (format) {
        ShareFormat.QUICK -> {
            // Share photo with basic info via messaging apps
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, photo.uri)
                putExtra(Intent.EXTRA_TEXT, """
                    Safety Inspection Photo
                    Project: ${photo.project}
                    Status: ${photo.complianceStatus}
                    Tags: ${photo.tags.joinToString()}
                    ${photo.analysis?.summary ?: ""}
                """.trimIndent())
            }
            startActivity(Intent.createChooser(intent, "Share Photo"))
        }
        
        ShareFormat.DETAILED_HTML -> {
            // Generate single photo HTML report
            val html = generatePhotoReport(photo)
            shareFile(html, "text/html")
        }
        
        ShareFormat.EMAIL -> {
            // Compose email with photo and analysis
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_SUBJECT, "Safety Issue - ${photo.project}")
                putExtra(Intent.EXTRA_TEXT, composeEmailBody(photo))
                putExtra(Intent.EXTRA_STREAM, photo.uri)
            }
            startActivity(emailIntent)
        }
    }
}
```

### Interactive Web Report

#### Generated HTML with JavaScript Navigation
```html
<!DOCTYPE html>
<html>
<head>
    <title>HazardHawk Safety Report</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        /* Modern, responsive design */
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #f5f5f5;
        }
        .sidebar {
            position: fixed;
            left: 0;
            top: 0;
            width: 300px;
            height: 100vh;
            background: white;
            overflow-y: auto;
            border-right: 1px solid #ddd;
        }
        .main-content {
            margin-left: 300px;
            padding: 20px;
        }
        .photo-card {
            background: white;
            border-radius: 8px;
            padding: 20px;
            margin-bottom: 20px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }
        .photo-img {
            width: 100%;
            max-width: 800px;
            cursor: zoom-in;
        }
        .lightbox {
            display: none;
            position: fixed;
            z-index: 999;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.9);
        }
        .lightbox img {
            max-width: 90%;
            max-height: 90%;
            margin: auto;
            display: block;
            position: relative;
            top: 50%;
            transform: translateY(-50%);
        }
        .compliance-badge {
            display: inline-block;
            padding: 6px 12px;
            border-radius: 4px;
            font-weight: bold;
        }
        .compliant { background: #4CAF50; color: white; }
        .needs-improvement { background: #FF9800; color: white; }
        .tag {
            display: inline-block;
            padding: 4px 8px;
            background: #e0e0e0;
            border-radius: 4px;
            margin: 2px;
        }
        .hazard-alert {
            background: #ffebee;
            border-left: 4px solid #f44336;
            padding: 12px;
            margin: 10px 0;
        }
        .osha-code {
            background: #f5f5f5;
            padding: 10px;
            margin: 8px 0;
            border-left: 3px solid #2196F3;
        }
        .filter-section {
            padding: 15px;
            border-bottom: 1px solid #eee;
        }
        @media (max-width: 768px) {
            .sidebar { 
                width: 100%; 
                height: auto;
                position: relative;
            }
            .main-content { 
                margin-left: 0; 
            }
        }
    </style>
</head>
<body>
    <div class="sidebar">
        <div class="filter-section">
            <h3>Safety Report</h3>
            <p>Project: <strong id="project-name"></strong></p>
            <p>Date: <strong id="report-date"></strong></p>
            <p>Total Photos: <strong id="photo-count"></strong></p>
        </div>
        
        <div class="filter-section">
            <h4>Quick Filters</h4>
            <label><input type="checkbox" id="show-compliant" checked> Compliant</label><br>
            <label><input type="checkbox" id="show-issues" checked> Needs Improvement</label><br>
            <label><input type="checkbox" id="high-risk-only"> High Risk Only</label>
        </div>
        
        <div class="filter-section">
            <h4>Photo Navigation</h4>
            <div id="photo-list"></div>
        </div>
    </div>
    
    <div class="main-content" id="photos-container">
        <!-- Photos will be inserted here -->
    </div>
    
    <div class="lightbox" id="lightbox" onclick="this.style.display='none'">
        <img id="lightbox-img" src="">
    </div>
    
    <script>
        // Photo data embedded by generator
        const photoData = [/* JSON data */];
        
        // Initialize report
        document.getElementById('project-name').textContent = photoData[0].project;
        document.getElementById('report-date').textContent = new Date().toLocaleDateString();
        document.getElementById('photo-count').textContent = photoData.length;
        
        // Render photos
        function renderPhotos() {
            const container = document.getElementById('photos-container');
            const showCompliant = document.getElementById('show-compliant').checked;
            const showIssues = document.getElementById('show-issues').checked;
            const highRiskOnly = document.getElementById('high-risk-only').checked;
            
            container.innerHTML = '';
            
            photoData
                .filter(photo => {
                    if (highRiskOnly && photo.severity !== 'HIGH') return false;
                    if (!showCompliant && photo.compliant) return false;
                    if (!showIssues && !photo.compliant) return false;
                    return true;
                })
                .forEach((photo, index) => {
                    container.innerHTML += generatePhotoCard(photo, index);
                });
        }
        
        function generatePhotoCard(photo, index) {
            return `
                <div class="photo-card" id="photo-${index}">
                    <h2>Photo ${index + 1} - ${photo.timestamp}</h2>
                    <img src="${photo.dataUrl}" class="photo-img" onclick="openLightbox('${photo.dataUrl}')">
                    
                    <div style="margin: 15px 0;">
                        <span class="compliance-badge ${photo.compliant ? 'compliant' : 'needs-improvement'}">
                            ${photo.complianceStatus}
                        </span>
                    </div>
                    
                    <div class="metadata">
                        <p><strong>Location:</strong> ${photo.location}</p>
                        <p><strong>GPS:</strong> ${photo.lat}, ${photo.lng}</p>
                    </div>
                    
                    <div class="tags">
                        <strong>Tags:</strong>
                        ${photo.tags.map(tag => `<span class="tag">${tag}</span>`).join('')}
                    </div>
                    
                    ${photo.analysis ? `
                        <div class="analysis">
                            <h3>Safety Analysis</h3>
                            ${photo.analysis.hazards.map(hazard => `
                                <div class="hazard-alert">
                                    <strong>${hazard.severity}</strong>: ${hazard.description}
                                </div>
                            `).join('')}
                            
                            <h4>OSHA Standards</h4>
                            ${photo.analysis.oshaCodes.map(code => `
                                <div class="osha-code">
                                    <strong>${code.number}</strong><br>
                                    ${code.description}<br>
                                    <small>${code.requirement}</small>
                                </div>
                            `).join('')}
                        </div>
                    ` : ''}
                </div>
            `;
        }
        
        function openLightbox(src) {
            document.getElementById('lightbox').style.display = 'block';
            document.getElementById('lightbox-img').src = src;
        }
        
        // Setup navigation list
        const navList = document.getElementById('photo-list');
        photoData.forEach((photo, index) => {
            navList.innerHTML += `
                <a href="#photo-${index}" style="display: block; padding: 5px;">
                    Photo ${index + 1} - ${photo.complianceStatus}
                </a>
            `;
        });
        
        // Filter listeners
        document.getElementById('show-compliant').addEventListener('change', renderPhotos);
        document.getElementById('show-issues').addEventListener('change', renderPhotos);
        document.getElementById('high-risk-only').addEventListener('change', renderPhotos);
        
        // Initial render
        renderPhotos();
    </script>
</body>
</html>
```

### Export Features Summary

#### Quick Export Actions
1. **Share to Superintendent** - Single photo with key issues
2. **Email to GC** - Formal report with all documentation  
3. **Text to Foreman** - Quick alert with photo and action required
4. **Upload to Procore** - Direct integration with project management

#### Batch Export Options
1. **Daily Report** - All photos from today with analysis
2. **Weekly Summary** - Trending issues and compliance stats
3. **Project Documentation** - Complete photo archive for project file
4. **Incident Package** - Specific incident with all related photos

#### Export Formats
- **HTML** - Interactive web report (can open on any device)
- **PDF** - Formal documentation for filing
- **Excel/CSV** - Data analysis and tracking
- **ZIP Package** - All photos + report + raw data

### Navigation & Search in Gallery

#### Smart Search
```kotlin
fun searchPhotos(query: String): List<Photo> {
    return photoRepository.searchPhotos(
        query = query,
        searchIn = listOf(
            SearchField.TAGS,
            SearchField.LOCATION,
            SearchField.ANALYSIS_TEXT,
            SearchField.OSHA_CODES,
            SearchField.PROJECT_NAME
        )
    )
}
```

#### Quick Actions from Gallery
- **Swipe right**: Mark as reviewed
- **Swipe left**: Delete (with confirmation)
- **Long press**: Multi-select mode
- **Double tap**: Quick share

#### Bulk Operations
- Select multiple photos
- Bulk tag editing
- Batch export
- Group by project/date/compliance
- Bulk AI reanalysis

## Implementation Priority

### Phase 1: Core Gallery (Week 1)
- List view with thumbnails
- Basic filtering (date, project)
- Photo detail view
- Simple share functionality

### Phase 2: Advanced Features (Week 2)
- AI analysis display
- OSHA code details
- HTML report generation
- PDF export

### Phase 3: Polish (Week 3)
- Grid view toggle
- Advanced search
- Bulk operations
- Excel export
- Interactive web reports

## Success Metrics
- Gallery load time: < 1 second for 100 photos
- Export generation: < 5 seconds for daily report
- Search response: < 500ms
- User can find any photo in < 3 taps
- Reports readable on any device without app