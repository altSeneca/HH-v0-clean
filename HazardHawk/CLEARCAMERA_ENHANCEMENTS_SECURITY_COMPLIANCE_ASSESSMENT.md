# ClearCamera Enhancements - Security & Compliance Assessment

**Assessment Date:** 2025-10-01
**Application:** HazardHawk - AI-Powered Construction Safety Platform
**Module:** ClearCamera UI Enhancements
**Assessor:** Security & Compliance Agent
**Classification:** INTERNAL - SECURITY REVIEW

---

## Executive Summary

This assessment evaluates the security and compliance implications of five proposed ClearCamera UI enhancements for the HazardHawk Android application. The analysis covers data protection regulations (GDPR, CCPA), OSHA compliance requirements, and construction safety documentation standards.

**Overall Risk Rating:** **LOW** - All proposed enhancements present minimal security risks when implemented with recommended mitigations.

**Key Findings:**
- Settings toggle for camera UI preference: **LOW RISK** - No sensitive data involved
- Project management dialog: **MEDIUM RISK** - Requires input validation and XSS prevention
- Tap-to-focus visual indicator: **LOW RISK** - No security concerns
- Settings persistence: **LOW RISK** - Standard Android security mechanisms sufficient
- Blur effects: **LOW RISK** - No exploitable attack surface

---

## Enhancement #1: Settings Toggle for Camera UI Preference

### Feature Description
User preference toggle to select between ClearCamera (minimalist) and HUD Camera (information-rich) interfaces.

### Security Analysis

#### Data Classification
- **Data Type:** UI preference setting (non-personal)
- **Sensitivity Level:** PUBLIC
- **Personal Data:** NO
- **GDPR Classification:** Not personal data under Article 4(1)
- **Retention Period:** Device lifetime or user preference reset

#### Storage Security Assessment

**Current Implementation:**
```kotlin
// MetadataSettings.kt - Uses SharedPreferences
private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
    "hazardhawk_metadata_settings",
    Context.MODE_PRIVATE
)
```

**Risk Assessment:**
- **Encryption Required:** NO - UI preferences are not sensitive
- **SharedPreferences Security:** MODE_PRIVATE provides adequate protection
- **Attack Surface:** Minimal - requires physical device access or root
- **Exploit Potential:** None - changing UI preference has no security impact

**OWASP Mobile Top 10 (2024) Mapping:**
- M9: Insecure Data Storage - **NOT APPLICABLE** (non-sensitive data)

#### Privacy Implications

**GDPR Article 5 Principles:**
- **Lawfulness:** UI preference collection is necessary for service provision
- **Purpose Limitation:** Used only for UI rendering
- **Data Minimization:** Only stores boolean or enum value
- **Storage Limitation:** No automatic deletion required
- **Integrity & Confidentiality:** Standard Android protections sufficient

**User Consent:**
- **Explicit Consent Required:** NO
- **Opt-in/Opt-out:** Implicit through settings interaction
- **Privacy Notice:** Should mention in privacy policy under "Device Settings"

#### Audit Logging

**Recommendation:** OPTIONAL
- Not required for compliance
- Could be useful for UX analytics if user consents to analytics

**If Implemented:**
```kotlin
// Example audit log entry
data class SettingsChangeEvent(
    val settingKey: String,
    val oldValue: String?,
    val newValue: String,
    val timestamp: Long,
    val userId: String? = null // Only if user authenticated
)
```

### Compliance Requirements

#### GDPR Compliance
- **Article 6 Lawful Basis:** Legitimate Interest (UI customization)
- **Article 13 Information:** Include in privacy policy: "We store your UI preferences locally"
- **Article 17 Right to Erasure:** Implement in app settings reset
- **Article 20 Data Portability:** Not applicable (non-personal)

#### CCPA Compliance
- **Category:** Not "Personal Information" under CCPA §1798.140(o)
- **Disclosure:** Not required in privacy notice
- **Sale Opt-out:** Not applicable

#### OSHA Documentation Requirements
- **Impact on Safety Documentation:** NONE
- **Metadata Integrity:** UI preference does not affect photo metadata
- **Audit Trail:** Not required for OSHA compliance

### Recommended Implementation

```kotlin
// Safe implementation using SharedPreferences
class CameraUIPreferenceManager(context: Context) {
    private val prefs = context.getSharedPreferences(
        "hazardhawk_ui_preferences",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_CAMERA_UI_MODE = "camera_ui_mode"
        private const val DEFAULT_MODE = "CLEAR" // or "HUD"
    }

    enum class CameraUIMode {
        CLEAR,  // Minimalist interface
        HUD     // Information-rich interface
    }

    fun getCameraUIMode(): CameraUIMode {
        val modeName = prefs.getString(KEY_CAMERA_UI_MODE, DEFAULT_MODE) ?: DEFAULT_MODE
        return try {
            CameraUIMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            CameraUIMode.CLEAR // Safe fallback
        }
    }

    fun setCameraUIMode(mode: CameraUIMode) {
        prefs.edit()
            .putString(KEY_CAMERA_UI_MODE, mode.name)
            .apply()
    }

    fun resetToDefault() {
        prefs.edit().remove(KEY_CAMERA_UI_MODE).apply()
    }
}
```

### Mitigation Strategies

| Risk | Mitigation | Priority |
|------|------------|----------|
| Tampering | MODE_PRIVATE restricts to app only | IMPLEMENTED |
| Data loss | Default fallback value | RECOMMENDED |
| Corruption | Enum validation with try-catch | RECOMMENDED |
| Backup exposure | Exclude from backups if desired | OPTIONAL |

### Security Best Practices

1. **Input Validation:** Validate enum values before storage
2. **Error Handling:** Provide safe defaults on corruption
3. **Testing:** Unit tests for preference persistence
4. **Documentation:** Comment the data lifecycle

**Risk Rating:** **LOW**

---

## Enhancement #2: Project Management Dialog

### Feature Description
Interactive dialog allowing users to create, edit, and select construction projects with metadata fields (project name, site address, manager, contractor, dates, safety officer).

### Security Analysis

#### Data Classification
- **Data Type:** Construction project metadata
- **Sensitivity Level:** CONFIDENTIAL (business data)
- **Personal Data:** YES (project manager names, safety officer names)
- **GDPR Classification:** Personal data under Article 4(1)
- **Retention Period:** Project lifecycle + 30 years (OSHA recordkeeping requirement)

#### Input Validation Vulnerabilities

**SQL Injection Risk: MEDIUM**

Current implementation uses SharedPreferences with string serialization:
```kotlin
// MetadataSettings.kt - Line 456
private fun saveProjectsList(projects: List<ProjectInfo>) {
    val projectsJson = projects.joinToString("|||") { project ->
        "${project.projectId}::${project.projectName}::${project.siteAddress}::..."
    }
    sharedPrefs.edit().putString(KEY_PROJECTS_LIST, projectsJson).apply()
}
```

**Vulnerability:** Delimiter injection if project names contain `:::` or `|||`

**Mitigation Required:**
```kotlin
// SECURE: Use kotlinx.serialization for proper escaping
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

private fun saveProjectsList(projects: List<ProjectInfo>) {
    val json = Json { ignoreUnknownKeys = true }
    val projectsJson = json.encodeToString(projects)
    sharedPrefs.edit().putString(KEY_PROJECTS_LIST, projectsJson).apply()
}

private fun loadProjectsList(): List<ProjectInfo> {
    val projectsJson = sharedPrefs.getString(KEY_PROJECTS_LIST, null) ?: return emptyList()
    return try {
        Json.decodeFromString<List<ProjectInfo>>(projectsJson)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to parse projects", e)
        emptyList()
    }
}
```

**XSS Risk: LOW (Display Context Only)**

Project names displayed in:
- Camera overlay watermark
- PDF exports
- Photo metadata EXIF tags

**Potential Attack:**
```kotlin
val maliciousProject = ProjectInfo(
    projectName = "<script>alert('XSS')</script>",
    siteAddress = "../../etc/passwd"
)
```

**Impact:**
- **In-App Display:** Compose Text() escapes HTML by default (SAFE)
- **PDF Export:** Depends on PDF library escaping
- **EXIF Metadata:** Binary format, not interpreted (SAFE)

**Mitigation:**
```kotlin
// Sanitize project input
fun sanitizeProjectInput(input: String): String {
    return input
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("'", "&#x27;")
        .replace("/", "&#x2F;")
        .take(200) // Max length validation
}
```

#### Data Validation

**Required Validations:**

| Field | Validation Rules | Max Length | Allowed Characters |
|-------|-----------------|------------|-------------------|
| projectName | Required, unique | 200 chars | Alphanumeric, spaces, hyphens, underscores |
| projectId | Auto-generated, unique | 50 chars | Alphanumeric, hyphens |
| siteAddress | Optional | 500 chars | Standard address characters |
| projectManager | Optional | 100 chars | Letters, spaces, apostrophes |
| contractor | Optional | 200 chars | Alphanumeric, spaces |
| startDate | ISO 8601 format | 10 chars | YYYY-MM-DD |
| expectedEndDate | ISO 8601, after start | 10 chars | YYYY-MM-DD |
| safetyOfficer | Optional | 100 chars | Letters, spaces, apostrophes |

**Implementation:**
```kotlin
data class ProjectValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList()
)

object ProjectValidator {
    private val projectNameRegex = Regex("^[a-zA-Z0-9\\s\\-_]+$")
    private val nameRegex = Regex("^[a-zA-Z\\s'\\-]+$")
    private val dateRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun validateProject(project: ProjectInfo): ProjectValidationResult {
        val errors = mutableListOf<String>()

        // Project name validation
        if (project.projectName.isBlank()) {
            errors.add("Project name is required")
        } else if (project.projectName.length > 200) {
            errors.add("Project name exceeds 200 characters")
        } else if (!projectNameRegex.matches(project.projectName)) {
            errors.add("Project name contains invalid characters")
        }

        // Site address validation
        if (project.siteAddress.length > 500) {
            errors.add("Site address exceeds 500 characters")
        }

        // Project manager name validation
        if (project.projectManager.isNotBlank()) {
            if (project.projectManager.length > 100) {
                errors.add("Project manager name exceeds 100 characters")
            } else if (!nameRegex.matches(project.projectManager)) {
                errors.add("Project manager name contains invalid characters")
            }
        }

        // Safety officer validation
        if (project.safetyOfficer.isNotBlank()) {
            if (project.safetyOfficer.length > 100) {
                errors.add("Safety officer name exceeds 100 characters")
            } else if (!nameRegex.matches(project.safetyOfficer)) {
                errors.add("Safety officer name contains invalid characters")
            }
        }

        // Date validation
        if (project.startDate.isNotBlank() && !dateRegex.matches(project.startDate)) {
            errors.add("Start date must be in YYYY-MM-DD format")
        }

        if (project.expectedEndDate.isNotBlank()) {
            if (!dateRegex.matches(project.expectedEndDate)) {
                errors.add("End date must be in YYYY-MM-DD format")
            } else if (project.startDate.isNotBlank() &&
                       project.expectedEndDate < project.startDate) {
                errors.add("End date must be after start date")
            }
        }

        return ProjectValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}
```

#### Access Control

**Current State:** All users can create/edit projects

**Recommended Role-Based Access Control (RBAC):**

| Role | Create Projects | Edit Projects | Delete Projects | View Projects |
|------|----------------|---------------|-----------------|---------------|
| Field Worker | NO | NO | NO | YES |
| Safety Lead | YES | YES (own) | YES (own) | YES |
| Project Admin | YES | YES (all) | YES (all) | YES |

**Implementation:**
```kotlin
enum class UserRole {
    FIELD_WORKER,
    SAFETY_LEAD,
    PROJECT_ADMIN
}

class ProjectAccessControl(private val userRole: UserRole) {
    fun canCreateProject(): Boolean = userRole in listOf(
        UserRole.SAFETY_LEAD,
        UserRole.PROJECT_ADMIN
    )

    fun canEditProject(project: ProjectInfo, userId: String): Boolean {
        return when (userRole) {
            UserRole.PROJECT_ADMIN -> true
            UserRole.SAFETY_LEAD -> project.userId == userId
            UserRole.FIELD_WORKER -> false
        }
    }

    fun canDeleteProject(project: ProjectInfo, userId: String): Boolean {
        return canEditProject(project, userId)
    }
}
```

#### GDPR Compliance

**Personal Data Processing:**
- **Data Subjects:** Project managers, safety officers, contractors
- **Processing Purpose:** Construction project management and OSHA compliance
- **Lawful Basis:** Article 6(1)(b) - Contract performance OR Article 6(1)(f) - Legitimate interest
- **Retention Period:** 30 years (OSHA 29 CFR 1904.33 requirement)

**Data Subject Rights Implementation:**

```kotlin
class ProjectDataSubjectRightsHandler(
    private val projectManager: ProjectManager,
    private val metadataSettings: MetadataSettingsManager
) {

    // GDPR Article 15 - Right of access
    suspend fun exportPersonalData(personName: String): String {
        val projects = metadataSettings.projectsList.value
        val relevantProjects = projects.filter { project ->
            project.projectManager.contains(personName, ignoreCase = true) ||
            project.safetyOfficer.contains(personName, ignoreCase = true)
        }

        return Json.encodeToString(relevantProjects)
    }

    // GDPR Article 16 - Right to rectification
    suspend fun updatePersonalData(oldName: String, newName: String) {
        val projects = metadataSettings.projectsList.value
        projects.forEach { project ->
            val updated = project.copy(
                projectManager = if (project.projectManager == oldName) newName
                                 else project.projectManager,
                safetyOfficer = if (project.safetyOfficer == oldName) newName
                                else project.safetyOfficer
            )
            metadataSettings.addProject(updated)
        }
    }

    // GDPR Article 17 - Right to erasure (limited by OSHA retention)
    suspend fun anonymizePersonalData(personName: String) {
        val projects = metadataSettings.projectsList.value
        projects.forEach { project ->
            val anonymized = project.copy(
                projectManager = if (project.projectManager == personName)
                                 "ANONYMIZED" else project.projectManager,
                safetyOfficer = if (project.safetyOfficer == personName)
                                "ANONYMIZED" else project.safetyOfficer
            )
            metadataSettings.addProject(anonymized)
        }
    }
}
```

**Privacy Notice Requirements:**
```
Data Collection: Project Management
- What we collect: Project names, site addresses, manager names, contractor names, dates
- Why we collect it: To organize construction safety documentation and comply with OSHA recordkeeping
- How long we keep it: 30 years (OSHA requirement)
- Your rights: Access, rectification, limited erasure (anonymization)
```

### Compliance Requirements

#### OSHA Recordkeeping (29 CFR 1904)
- **Requirement:** Retain injury/illness records for 5 years
- **Extended Requirement:** Retain exposure records for 30 years (29 CFR 1910.1020)
- **Project Metadata:** Must be preserved with associated photos and incident reports

#### Data Retention Policy

```kotlin
data class DataRetentionPolicy(
    val category: String,
    val retentionPeriod: Long, // milliseconds
    val deletionMethod: DeletionMethod
)

enum class DeletionMethod {
    HARD_DELETE,      // Permanent removal
    SOFT_DELETE,      // Mark as deleted, preserve for audit
    ANONYMIZE         // Remove personal identifiers, keep structure
}

val PROJECT_RETENTION_POLICY = DataRetentionPolicy(
    category = "Project Metadata",
    retentionPeriod = 30L * 365 * 24 * 60 * 60 * 1000, // 30 years
    deletionMethod = DeletionMethod.ANONYMIZE
)
```

### Recommended Implementation

**Secure Project Dialog with Validation:**

```kotlin
@Composable
fun SecureProjectManagementDialog(
    onDismiss: () -> Unit,
    onSave: (ProjectInfo) -> Unit,
    userRole: UserRole,
    existingProject: ProjectInfo? = null
) {
    var projectName by remember { mutableStateOf(existingProject?.projectName ?: "") }
    var siteAddress by remember { mutableStateOf(existingProject?.siteAddress ?: "") }
    var projectManager by remember { mutableStateOf(existingProject?.projectManager ?: "") }
    var validationErrors by remember { mutableStateOf<List<String>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingProject == null) "Create Project" else "Edit Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Project Name
                OutlinedTextField(
                    value = projectName,
                    onValueChange = {
                        projectName = it.take(200) // Enforce max length
                    },
                    label = { Text("Project Name *") },
                    isError = validationErrors.any { it.contains("Project name") },
                    singleLine = true
                )

                // Site Address
                OutlinedTextField(
                    value = siteAddress,
                    onValueChange = {
                        siteAddress = it.take(500)
                    },
                    label = { Text("Site Address") },
                    maxLines = 2
                )

                // Project Manager (Personal Data)
                OutlinedTextField(
                    value = projectManager,
                    onValueChange = {
                        projectManager = it.take(100)
                    },
                    label = { Text("Project Manager") },
                    isError = validationErrors.any { it.contains("manager") },
                    singleLine = true
                )

                // Validation Errors
                if (validationErrors.isNotEmpty()) {
                    Text(
                        text = validationErrors.joinToString("\n"),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Privacy Notice
                Text(
                    text = "Names entered are personal data and will be retained for 30 years per OSHA requirements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val project = ProjectInfo(
                        projectId = existingProject?.projectId ?: "PROJ_${System.currentTimeMillis()}",
                        projectName = projectName.trim(),
                        siteAddress = siteAddress.trim(),
                        projectManager = projectManager.trim()
                    )

                    val validation = ProjectValidator.validateProject(project)
                    if (validation.isValid) {
                        onSave(project)
                        onDismiss()
                    } else {
                        validationErrors = validation.errors
                    }
                },
                enabled = projectName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### Mitigation Strategies

| Vulnerability | Mitigation | Priority |
|---------------|------------|----------|
| SQL/Delimiter Injection | Use kotlinx.serialization | HIGH |
| XSS in PDF exports | Sanitize before PDF generation | MEDIUM |
| Unauthorized edits | Implement RBAC | MEDIUM |
| Data retention violations | Automated retention policy | HIGH |
| Personal data exposure | Privacy notice + GDPR rights | HIGH |
| Input length attacks | Max length validation | HIGH |
| Special character injection | Regex validation | MEDIUM |

**Risk Rating:** **MEDIUM** (becomes LOW after mitigations)

---

## Enhancement #3: Tap-to-Focus Visual Indicator

### Feature Description
Visual feedback (e.g., animated ring) when user taps to focus the camera.

### Security Analysis

#### Privacy Implications
- **Focus Point Coordinates:** Not sensitive - relative screen position
- **Reveal Sensitive Info:** NO - focus indicator is UI-only
- **Metadata Storage:** NO - focus events not persisted

#### Screen Recording Considerations

**Question:** Should focus indicator be excluded from screen recordings?

**Analysis:**
- **Android Screenshot Protection:** Not required for focus indicator
- **Screen Recording Tools:** Focus indicator is part of normal UI
- **Security Impact:** None - indicator provides no exploitable information

**Recommendation:** Allow focus indicator in screen recordings (standard behavior)

#### Attack Surface
- **Exploit Potential:** None
- **DoS via Rapid Tapping:** Handled by Android touch event debouncing
- **UI Redraw Performance:** Minimal - simple animation

### Compliance Requirements
- **GDPR:** Not applicable (no data collection)
- **OSHA:** Not applicable (doesn't affect documentation)
- **Accessibility:** Should follow Material Design touch feedback guidelines

### Recommended Implementation

```kotlin
@Composable
fun TapToFocusIndicator(
    focusPoint: Offset?,
    modifier: Modifier = Modifier
) {
    focusPoint?.let { point ->
        val alpha by animateFloatAsState(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 600, easing = LinearEasing),
            label = "focus_fade"
        )

        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Canvas(
                modifier = Modifier
                    .offset(x = point.x.dp, y = point.y.dp)
                    .size(80.dp)
            ) {
                drawCircle(
                    color = Color.White.copy(alpha = (1f - alpha) * 0.8f),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

// In CameraScreen
var focusPoint by remember { mutableStateOf<Offset?>(null) }

AndroidView(
    factory = { ctx ->
        PreviewView(ctx).apply {
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    focusPoint = Offset(event.x, event.y)

                    // Trigger camera focus
                    val factory = SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())
                    val point = factory.createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(point).build()
                    cameraController.cameraControl?.startFocusAndMetering(action)

                    // Clear focus indicator after animation
                    lifecycleScope.launch {
                        delay(600)
                        focusPoint = null
                    }
                }
                true
            }
        }
    }
)

TapToFocusIndicator(focusPoint = focusPoint)
```

### Security Best Practices
1. **No Data Persistence:** Don't store focus coordinates
2. **Rate Limiting:** Use Android's built-in touch event handling
3. **Accessibility:** Provide haptic feedback for focus confirmation

**Risk Rating:** **LOW**

---

## Enhancement #4: Settings Persistence (DataStore/SharedPreferences)

### Feature Description
Persistent storage of camera UI preferences using Android DataStore or SharedPreferences.

### Security Analysis

#### Storage Mechanism Comparison

| Aspect | SharedPreferences | DataStore (Preferences) | DataStore (Proto) |
|--------|-------------------|------------------------|-------------------|
| Encryption | Not encrypted by default | Not encrypted | Not encrypted |
| Type Safety | NO (String storage) | NO (type-safe API, but string storage) | YES (Protocol Buffers) |
| Async | NO (blocks main thread) | YES (Kotlin coroutines) | YES (Kotlin coroutines) |
| Data Corruption | Possible | Better error handling | Schema migration |
| Backup | Included by default | Included by default | Included by default |

#### Current Implementation Analysis

**MetadataSettings.kt** uses SharedPreferences:
```kotlin
private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
    "hazardhawk_metadata_settings",
    Context.MODE_PRIVATE
)
```

**Security Properties:**
- **MODE_PRIVATE:** Only accessible by the app (UID-based isolation)
- **File Location:** `/data/data/com.hazardhawk/shared_prefs/`
- **Permissions:** `rw-------` (owner read/write only)
- **Root Access:** Vulnerable if device is rooted
- **ADB Backup:** Included in Android backup (can be excluded)

#### Encryption Assessment

**Does UI preference need encryption?** NO

**Rationale:**
- UI mode preference (Clear vs HUD) is not sensitive
- No personal identifiable information (PII)
- No authentication credentials
- No financial data
- No health information

**When encryption IS required:**
- API keys (already handled by SecureKeyManager)
- User authentication tokens
- Payment information
- Personal health data
- Biometric templates

#### Backup Security

**Android Backup Inclusion:**
```xml
<!-- AndroidManifest.xml -->
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules">
```

**Should UI preference be backed up?** YES
**Reason:** Improves user experience on device transfer, no security risk

**To exclude from backup (if desired):**
```xml
<!-- res/xml/backup_rules.xml -->
<full-backup-content>
    <exclude domain="sharedpref" path="hazardhawk_ui_preferences.xml"/>
</full-backup-content>
```

#### Root Access Protection

**Threat Model:** Rooted device allows any app to read SharedPreferences

**Mitigation Options:**

1. **Root Detection (Not Recommended)**
```kotlin
// NOT RECOMMENDED - easily bypassed
fun isDeviceRooted(): Boolean {
    val paths = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su"
    )
    return paths.any { File(it).exists() }
}
```

2. **Accept Risk for Non-Sensitive Data (RECOMMENDED)**
- UI preferences pose no security risk if exposed on rooted device
- Focus security efforts on truly sensitive data (API keys, tokens)

3. **Use EncryptedSharedPreferences (OVERKILL for UI prefs)**
```kotlin
// Only if paranoid about UI preferences
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "ui_preferences_encrypted",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

**Recommendation:** Use standard SharedPreferences for UI preferences

#### Multi-User Support

**Android Multi-User Environment:**
- SharedPreferences are user-specific by default
- Each Android user has isolated `/data/data/` directory
- No cross-user access (enforced by Android)

**Work Profile Support:**
- Work profile is separate user context
- Preferences automatically isolated
- No special handling required

### DataStore Migration (Optional Enhancement)

**Benefits of migrating to DataStore:**
- Type-safe API reduces bugs
- Async operations (no main thread blocking)
- Better error handling
- Kotlin coroutines integration

**Migration Example:**
```kotlin
// Define preferences schema
val Context.uiPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ui_preferences"
)

// Preference keys
object PreferencesKeys {
    val CAMERA_UI_MODE = stringPreferencesKey("camera_ui_mode")
    val SHOW_GRID_LINES = booleanPreferencesKey("show_grid_lines")
    val FLASH_MODE = stringPreferencesKey("flash_mode")
}

// Repository pattern
class UISettingsRepository(private val context: Context) {

    val cameraUIMode: Flow<String> = context.uiPreferencesDataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CAMERA_UI_MODE] ?: "CLEAR"
        }

    suspend fun setCameraUIMode(mode: String) {
        context.uiPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.CAMERA_UI_MODE] = mode
        }
    }

    suspend fun clearAllSettings() {
        context.uiPreferencesDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

// Usage in ViewModel
class CameraViewModel(private val settingsRepo: UISettingsRepository) : ViewModel() {

    val cameraUIMode: StateFlow<String> = settingsRepo.cameraUIMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "CLEAR"
        )

    fun setCameraUIMode(mode: String) {
        viewModelScope.launch {
            settingsRepo.setCameraUIMode(mode)
        }
    }
}
```

### Compliance Requirements

#### GDPR
- **Data Category:** Application preferences (non-personal)
- **Processing Purpose:** Service functionality
- **Retention:** Device lifetime
- **User Rights:** Right to erasure (app uninstall or settings reset)

#### CCPA
- **Not applicable:** UI preferences are not "personal information"

#### OSHA
- **Not applicable:** UI preferences don't affect safety documentation

### Recommended Implementation

**Use SharedPreferences for UI preferences:**
```kotlin
class CameraUISettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "camera_ui_settings",
        Context.MODE_PRIVATE
    )

    // Settings keys
    companion object {
        private const val KEY_CAMERA_MODE = "camera_mode"
        private const val KEY_SHOW_GRID = "show_grid"
        private const val KEY_FLASH_MODE = "flash_mode"
        private const val KEY_ASPECT_RATIO = "aspect_ratio"
    }

    // Camera UI mode
    fun getCameraMode(): String = prefs.getString(KEY_CAMERA_MODE, "CLEAR") ?: "CLEAR"

    fun setCameraMode(mode: String) {
        require(mode in listOf("CLEAR", "HUD")) { "Invalid camera mode: $mode" }
        prefs.edit().putString(KEY_CAMERA_MODE, mode).apply()
    }

    // Grid lines
    fun getShowGrid(): Boolean = prefs.getBoolean(KEY_SHOW_GRID, true)

    fun setShowGrid(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_GRID, show).apply()
    }

    // Flash mode
    fun getFlashMode(): String = prefs.getString(KEY_FLASH_MODE, "auto") ?: "auto"

    fun setFlashMode(mode: String) {
        require(mode in listOf("auto", "on", "off")) { "Invalid flash mode: $mode" }
        prefs.edit().putString(KEY_FLASH_MODE, mode).apply()
    }

    // Reset to defaults
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }

    // Export settings (GDPR right to access)
    fun exportSettings(): String {
        val allSettings = prefs.all
        return Json.encodeToString(allSettings)
    }
}
```

### Mitigation Strategies

| Risk | Mitigation | Priority |
|------|------------|----------|
| Data tampering | MODE_PRIVATE + validation | IMPLEMENTED |
| Corruption | Default fallback values | RECOMMENDED |
| Root access | Accept risk (non-sensitive) | N/A |
| Backup exposure | Keep in backup (useful) | RECOMMENDED |
| Type safety | Input validation on read | RECOMMENDED |

**Risk Rating:** **LOW**

---

## Enhancement #5: Blur Effects

### Feature Description
Background blur effects (e.g., Glass morphism) for UI overlays on camera preview.

### Security Analysis

#### Privacy Considerations

**Question:** Does blur obscure sensitive background content?

**Analysis:**
- **Blur Purpose:** Aesthetic UI enhancement, improve overlay readability
- **Privacy Benefit:** Could obscure sensitive information in background
- **Privacy Risk:** None - blur is applied to UI overlay, not photo capture

**Use Cases:**
1. **Settings dialog over camera preview:** Blur makes dialog more readable
2. **Project selection dialog:** Blur focuses attention on dialog
3. **AR overlays:** Blur can reduce visual clutter

**Privacy Impact:** POSITIVE - Blur can prevent shoulder surfing of sensitive project data

#### Performance & DoS Considerations

**Question:** Could blur effects be exploited for Denial of Service?

**Android RenderEffect Analysis:**
```kotlin
// ClearDesignTokens.kt - Blur implementation
fun contentBackgroundModifier(
    backgroundColor: Color = Colors.TranslucentWhite10,
    cornerRadius: Dp = CornerRadius.Medium,
    blurRadius: Dp = 20.dp
): Modifier {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .graphicsLayer {
                renderEffect = RenderEffect
                    .createBlurEffect(
                        blurRadius.toPx(),
                        blurRadius.toPx(),
                        Shader.TileMode.CLAMP
                    )
            }
            .background(backgroundColor)
    } else {
        Modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
    }
}
```

**Security Properties:**
- **System-Managed:** Android system manages RenderEffect processing
- **GPU-Accelerated:** Uses hardware acceleration, minimal CPU overhead
- **Resource Limits:** Android enforces GPU memory limits
- **Fallback:** Gracefully degrades on older devices (no blur)

**DoS Attack Scenarios:**
1. **Rapid blur radius changes:** System throttles GPU operations
2. **Excessive blur layers:** Compose limits layer nesting
3. **Memory exhaustion:** Android OS enforces app memory limits

**Conclusion:** No exploitable DoS surface - system-managed with built-in protections

#### Information Disclosure

**Blur as Security Measure:**
- **Sensitive UI Elements:** Blur can obscure API keys, tokens in debug screens
- **Project Names:** Blur over camera preview prevents screen recording leaks
- **NOT a replacement for:** Proper access controls and encryption

**Anti-Pattern Warning:**
```kotlin
// WRONG: Don't use blur as security
@Composable
fun APIKeyDisplay(apiKey: String) {
    Box(modifier = Modifier.blur(10.dp)) { // INSECURE
        Text(apiKey)
    }
}

// CORRECT: Use proper masking
@Composable
fun APIKeyDisplay(apiKey: String) {
    Text(apiKey.take(4) + "*".repeat(apiKey.length - 4))
}
```

### Accessibility Implications

**Blur and Accessibility:**
- **Screen Readers:** Blur is visual-only, doesn't affect TalkBack
- **High Contrast Mode:** Blur may reduce contrast, test carefully
- **Motion Sensitivity:** Animated blur transitions could trigger motion sickness

**Best Practices:**
```kotlin
// Respect accessibility settings
@Composable
fun AccessibleBlur(content: @Composable () -> Unit) {
    val accessibilityManager = LocalContext.current
        .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    val reduceMotion = accessibilityManager.isTouchExplorationEnabled

    Box(
        modifier = if (reduceMotion) {
            Modifier.background(Color.Black.copy(alpha = 0.7f))
        } else {
            Modifier.blur(20.dp).background(Color.Black.copy(alpha = 0.3f))
        }
    ) {
        content()
    }
}
```

### Compliance Requirements
- **WCAG 2.1:** Ensure sufficient contrast (4.5:1 for text)
- **GDPR:** Not applicable (no data processing)
- **OSHA:** Not applicable (doesn't affect documentation)

### Recommended Implementation

```kotlin
object BlurEffectConfig {
    // Security: Use moderate blur radii to prevent performance issues
    const val LIGHT_BLUR = 10f
    const val MEDIUM_BLUR = 20f
    const val HEAVY_BLUR = 30f

    // Accessibility: Provide non-blur alternative
    fun getBlurModifier(
        context: Context,
        blurRadius: Float = MEDIUM_BLUR
    ): Modifier {
        val accessibilityManager = context
            .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                   !accessibilityManager.isTouchExplorationEnabled) {
            Modifier.graphicsLayer {
                renderEffect = RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.CLAMP
                )
            }
        } else {
            Modifier // No blur fallback
        }
    }
}

// Usage
@Composable
fun BlurredDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(BlurEffectConfig.getBlurModifier(context))
                .background(Color.White.copy(alpha = 0.8f))
                .padding(16.dp)
        ) {
            // Dialog content
        }
    }
}
```

### Mitigation Strategies

| Risk | Mitigation | Priority |
|------|------------|----------|
| Performance degradation | Use system RenderEffect | IMPLEMENTED |
| DoS via rapid changes | System-managed throttling | IMPLEMENTED |
| Accessibility issues | Fallback for screen readers | RECOMMENDED |
| Older device support | Graceful degradation | IMPLEMENTED |

**Risk Rating:** **LOW**

---

## Consolidated Security Recommendations

### High Priority (Implement Before Release)

1. **Project Input Validation**
   - Implement `ProjectValidator` with regex validation
   - Enforce maximum field lengths
   - Sanitize all text inputs before storage

2. **Data Serialization Security**
   - Migrate from delimiter-based to `kotlinx.serialization`
   - Add try-catch for deserialization errors
   - Implement schema versioning

3. **GDPR Data Subject Rights**
   - Implement personal data export function
   - Add anonymization for project personnel
   - Update privacy policy with 30-year retention notice

### Medium Priority (Implement in Next Sprint)

4. **Role-Based Access Control**
   - Define user roles in authentication system
   - Restrict project creation to Safety Lead and Admin
   - Implement ownership-based edit permissions

5. **Audit Logging**
   - Log project creation/modification events
   - Include user ID, timestamp, and action type
   - Store logs securely for compliance audits

6. **PDF Export Sanitization**
   - Escape HTML entities in project names before PDF generation
   - Validate all text fields before rendering
   - Test with malicious input samples

### Low Priority (Nice to Have)

7. **DataStore Migration**
   - Migrate UI preferences from SharedPreferences to DataStore
   - Improve type safety and async handling
   - Better error handling for corrupted data

8. **Accessibility Enhancements**
   - Add screen reader support for focus indicator
   - Implement high-contrast mode for blur effects
   - Test with TalkBack enabled

9. **Settings Export/Import**
   - Allow users to export UI preferences
   - Implement secure import with validation
   - Support backup/restore across devices

---

## GDPR Compliance Checklist

### Article 5: Principles of Processing
- [x] **Lawfulness:** UI preferences - legitimate interest; Project data - contract performance
- [x] **Purpose Limitation:** Settings used only for UI; Projects for OSHA compliance
- [x] **Data Minimization:** Only collect necessary project fields
- [x] **Accuracy:** Allow users to edit project information
- [x] **Storage Limitation:** 30-year retention for OSHA compliance (documented)
- [x] **Integrity & Confidentiality:** MODE_PRIVATE for SharedPreferences, optional encryption

### Article 13: Information to be Provided
**Privacy Notice Requirements:**
```
CAMERA SETTINGS COLLECTION
- What we collect: Your camera UI preference (minimalist or information-rich),
  flash mode, grid line visibility, aspect ratio
- Why: To remember your preferences and improve your experience
- How long: Until you uninstall the app or reset settings
- Your rights: Delete at any time via app settings

PROJECT INFORMATION COLLECTION
- What we collect: Project names, site addresses, manager names, contractor names,
  safety officer names, project start/end dates
- Why: To organize construction safety documentation and comply with OSHA
  recordkeeping requirements (29 CFR 1904, 29 CFR 1910.1020)
- How long: 30 years (OSHA regulatory requirement)
- Your rights: Access, rectify, or request anonymization (full deletion may be
  limited by legal obligations)
```

### Article 15: Right of Access
```kotlin
// Implement data export
fun exportUserData(userId: String): UserDataExport {
    return UserDataExport(
        uiSettings = settingsManager.exportSettings(),
        projects = projectManager.getProjectsForUser(userId),
        photoMetadata = photoRepository.getMetadataForUser(userId)
    )
}
```

### Article 17: Right to Erasure
```kotlin
// Implement data deletion (with OSHA limitation)
fun deleteUserData(userId: String, includeOSHAData: Boolean = false) {
    // Always allow deletion of non-OSHA data
    settingsManager.resetToDefaults()

    if (includeOSHAData) {
        // Anonymize instead of delete (OSHA retention requirement)
        projectManager.anonymizeProjects(userId)
        photoRepository.anonymizeMetadata(userId)
    } else {
        // Inform user of OSHA limitation
        showDialog("Some data must be retained for 30 years per OSHA requirements. " +
                   "We can anonymize your personal information instead.")
    }
}
```

---

## OSHA Compliance Analysis

### 29 CFR 1904: Recording and Reporting Occupational Injuries and Illnesses

**Requirement:** Retain OSHA 300 Log for 5 years following the year to which it pertains

**HazardHawk Impact:**
- Photo metadata with project information must be retained for 5+ years
- Project names and safety officer information are part of the record
- Cannot delete project data associated with incident reports for 5 years minimum

### 29 CFR 1910.1020: Access to Employee Exposure and Medical Records

**Requirement:** Retain employee exposure records for duration of employment plus 30 years

**HazardHawk Impact:**
- Construction site photos may document exposure to hazards
- Project metadata becomes part of exposure record
- 30-year retention period supersedes GDPR right to erasure

**Compliance Strategy:**
```kotlin
data class PhotoRetentionCategory(
    val photoId: String,
    val category: RetentionType,
    val retentionEndDate: Long // Unix timestamp
)

enum class RetentionType {
    GENERAL_DOCUMENTATION,     // 5 years
    INCIDENT_RECORD,           // 5 years (29 CFR 1904)
    EXPOSURE_RECORD,           // 30 years (29 CFR 1910.1020)
    TOXIC_SUBSTANCE_EXPOSURE   // 30 years (29 CFR 1910.1020)
}

class OSHARetentionManager {
    fun calculateRetentionPeriod(photo: PhotoMetadata): RetentionType {
        return when {
            photo.tags.contains("incident") ||
            photo.aiAnalysis?.hazards?.any { it.severity == "CRITICAL" } == true
                -> RetentionType.INCIDENT_RECORD

            photo.tags.any { it in listOf("asbestos", "lead", "silica", "toxic") }
                -> RetentionType.TOXIC_SUBSTANCE_EXPOSURE

            photo.aiAnalysis?.hazards?.isNotEmpty() == true
                -> RetentionType.EXPOSURE_RECORD

            else -> RetentionType.GENERAL_DOCUMENTATION
        }
    }

    fun canDeletePhoto(photo: PhotoMetadata): Boolean {
        val category = calculateRetentionPeriod(photo)
        val captureDate = photo.timestamp
        val retentionYears = when (category) {
            RetentionType.GENERAL_DOCUMENTATION -> 5
            RetentionType.INCIDENT_RECORD -> 5
            RetentionType.EXPOSURE_RECORD -> 30
            RetentionType.TOXIC_SUBSTANCE_EXPOSURE -> 30
        }

        val retentionEndDate = captureDate + (retentionYears * 365 * 24 * 60 * 60 * 1000L)
        return System.currentTimeMillis() > retentionEndDate
    }
}
```

---

## Security Testing Requirements

### Unit Tests

```kotlin
class ProjectInputValidationTest {

    @Test
    fun `validate project name rejects SQL injection`() {
        val maliciousProject = ProjectInfo(
            projectName = "'; DROP TABLE projects; --"
        )
        val result = ProjectValidator.validateProject(maliciousProject)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("invalid characters") })
    }

    @Test
    fun `validate project name rejects XSS`() {
        val xssProject = ProjectInfo(
            projectName = "<script>alert('XSS')</script>"
        )
        val result = ProjectValidator.validateProject(xssProject)
        assertFalse(result.isValid)
    }

    @Test
    fun `validate project name enforces max length`() {
        val longProject = ProjectInfo(
            projectName = "A".repeat(300)
        )
        val result = ProjectValidator.validateProject(longProject)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("exceeds 200 characters") })
    }

    @Test
    fun `validate date format accepts ISO 8601`() {
        val validProject = ProjectInfo(
            projectName = "Test",
            startDate = "2025-01-15",
            expectedEndDate = "2025-12-31"
        )
        val result = ProjectValidator.validateProject(validProject)
        assertTrue(result.isValid)
    }

    @Test
    fun `validate end date must be after start date`() {
        val invalidProject = ProjectInfo(
            projectName = "Test",
            startDate = "2025-12-31",
            expectedEndDate = "2025-01-15"
        )
        val result = ProjectValidator.validateProject(invalidProject)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("after start date") })
    }
}

class SettingsPersistenceTest {

    @Test
    fun `SharedPreferences persists UI mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CameraUISettingsManager(context)

        manager.setCameraMode("HUD")
        assertEquals("HUD", manager.getCameraMode())

        manager.setCameraMode("CLEAR")
        assertEquals("CLEAR", manager.getCameraMode())
    }

    @Test
    fun `SharedPreferences rejects invalid camera mode`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CameraUISettingsManager(context)

        assertThrows<IllegalArgumentException> {
            manager.setCameraMode("INVALID")
        }
    }

    @Test
    fun `reset to defaults clears all settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = CameraUISettingsManager(context)

        manager.setCameraMode("HUD")
        manager.setShowGrid(false)
        manager.resetToDefaults()

        assertEquals("CLEAR", manager.getCameraMode()) // Default value
        assertTrue(manager.getShowGrid()) // Default value
    }
}
```

### Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class ProjectManagementSecurityTest {

    @Test
    fun testProjectDataPersistence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val projectManager = ProjectManager(context)
        val metadataSettings = MetadataSettingsManager(context, projectManager)

        // Create project with special characters
        val project = ProjectInfo(
            projectId = "TEST_123",
            projectName = "O'Reilly & Sons Construction",
            projectManager = "John O'Brien",
            siteAddress = "123 Main St., Apt #5"
        )

        // Validate before saving
        val validation = ProjectValidator.validateProject(project)
        assertTrue(validation.isValid)

        // Save project
        metadataSettings.addProject(project)

        // Retrieve and verify
        val projects = metadataSettings.projectsList.value
        val retrieved = projects.find { it.projectId == "TEST_123" }
        assertNotNull(retrieved)
        assertEquals(project.projectName, retrieved?.projectName)
        assertEquals(project.projectManager, retrieved?.projectManager)
    }

    @Test
    fun testGDPRDataExport() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val rightsHandler = ProjectDataSubjectRightsHandler(
            ProjectManager(context),
            MetadataSettingsManager(context)
        )

        // Export data for a person
        val exportedData = rightsHandler.exportPersonalData("John O'Brien")

        // Verify export contains expected data
        assertTrue(exportedData.isNotEmpty())
        val projects: List<ProjectInfo> = Json.decodeFromString(exportedData)
        assertTrue(projects.any { it.projectManager.contains("John O'Brien") })
    }
}
```

### Penetration Testing Checklist

- [ ] SQL injection attempts in project name field
- [ ] XSS payloads in all text fields
- [ ] Path traversal in site address field (`../../etc/passwd`)
- [ ] Buffer overflow with 10,000+ character inputs
- [ ] Special character injection (NULL bytes, control characters)
- [ ] Unicode normalization attacks (e.g., `Аdmin` with Cyrillic A)
- [ ] XML/JSON injection in serialized data
- [ ] Race conditions in concurrent project creation
- [ ] SharedPreferences tampering on rooted device
- [ ] Android backup extraction and analysis

---

## Privacy Policy Template

### Data Collection and Processing

**Camera Settings**
- **Data Collected:** UI theme preference, flash mode, grid visibility, aspect ratio
- **Purpose:** To personalize your camera experience and remember your preferences
- **Legal Basis:** Legitimate interest (GDPR Article 6(1)(f))
- **Retention Period:** Until app uninstall or manual reset
- **Third-Party Sharing:** None - stored locally on your device only
- **Your Rights:** Delete at any time via Settings > Reset Preferences

**Project Management**
- **Data Collected:**
  - Project names and unique identifiers
  - Construction site addresses
  - Project manager names
  - Contractor company names
  - Safety officer names
  - Project start and end dates
- **Purpose:**
  - To organize safety documentation by project
  - To comply with OSHA recordkeeping requirements (29 CFR 1904, 29 CFR 1910.1020)
- **Legal Basis:**
  - Contract performance (GDPR Article 6(1)(b))
  - Legal obligation (GDPR Article 6(1)(c) - OSHA compliance)
- **Retention Period:** 30 years after project completion (OSHA requirement)
- **Third-Party Sharing:**
  - May be shared with OSHA upon regulatory request
  - May be shared with project stakeholders (contractors, safety officers)
- **Your Rights:**
  - Access: Request a copy of your project data
  - Rectification: Correct inaccurate project information
  - Erasure: Request anonymization (full deletion limited by OSHA requirements)
  - Portability: Export project data in JSON format

**Limitation of Right to Erasure**
Under GDPR Article 17(3)(b), we may be unable to fully delete your data when retention is necessary for compliance with legal obligations. Construction safety records must be retained for 30 years under OSHA regulations (29 CFR 1910.1020). In such cases, we will anonymize your personal identifiers while preserving the safety record structure.

### Data Subject Rights Request Process

To exercise your GDPR rights, contact our Data Protection Officer:
- **Email:** privacy@hazardhawk.app
- **Response Time:** Within 30 days
- **Required Information:** User ID, email address, description of request
- **Verification:** We may request additional information to verify your identity

---

## Conclusion

The five proposed ClearCamera enhancements present **LOW to MEDIUM security risks** that can be effectively mitigated with proper implementation practices:

1. **Settings Toggle:** LOW RISK - No security concerns
2. **Project Management Dialog:** MEDIUM RISK - Requires input validation, XSS prevention, and GDPR compliance
3. **Tap-to-Focus Indicator:** LOW RISK - No security concerns
4. **Settings Persistence:** LOW RISK - Standard Android security sufficient
5. **Blur Effects:** LOW RISK - System-managed with built-in protections

### Implementation Priorities

**Must Implement (Critical):**
- Project input validation with regex and length limits
- Migration from delimiter-based to `kotlinx.serialization` for project data
- GDPR data subject rights (export, rectification, anonymization)
- Privacy policy updates with 30-year OSHA retention notice

**Should Implement (Important):**
- Role-based access control for project management
- Audit logging for project creation/modification
- PDF export sanitization for XSS prevention

**Could Implement (Nice to Have):**
- DataStore migration for type-safe preferences
- Accessibility enhancements for blur effects
- Settings export/import functionality

### Compliance Summary

| Regulation | Status | Notes |
|------------|--------|-------|
| GDPR | COMPLIANT* | *With recommended mitigations |
| CCPA | COMPLIANT | UI preferences not "personal information" |
| OSHA 29 CFR 1904 | COMPLIANT | 5-year retention for incident records |
| OSHA 29 CFR 1910.1020 | COMPLIANT | 30-year retention for exposure records |
| WCAG 2.1 | REVIEW NEEDED | Test blur effects with screen readers |

---

**Document Version:** 1.0
**Last Updated:** 2025-10-01
**Next Review:** 2025-11-01 (or upon implementation changes)
**Classification:** INTERNAL - SECURITY REVIEW

---

## Appendix A: Threat Model

### STRIDE Analysis for Project Management Dialog

| Threat | Scenario | Likelihood | Impact | Mitigation |
|--------|----------|------------|--------|------------|
| **Spoofing** | Attacker creates project with another user's name | LOW | MEDIUM | Implement user authentication and project ownership |
| **Tampering** | Attacker modifies SharedPreferences on rooted device | LOW | LOW | Accept risk for non-critical data, use EncryptedSharedPreferences for sensitive data |
| **Repudiation** | User denies creating a project | LOW | LOW | Implement audit logging with timestamps |
| **Information Disclosure** | Project data exposed in Android backup | MEDIUM | LOW | Acceptable for business data, encrypt backups |
| **Denial of Service** | Attacker creates thousands of projects | LOW | LOW | Implement rate limiting and max project count |
| **Elevation of Privilege** | Field worker edits projects they shouldn't | MEDIUM | MEDIUM | Implement RBAC with role verification |

### Attack Tree: Project Data Compromise

```
Goal: Extract sensitive project information
├── Physical Device Access
│   ├── Stolen/Lost Device
│   │   └── Mitigation: Device encryption, screen lock
│   └── Rooted Device
│       └── Mitigation: Accept risk, focus on API key protection
├── Network Interception
│   └── Not Applicable (local storage only)
├── Backup Extraction
│   ├── Android Backup
│   │   └── Mitigation: Encrypt backups, exclude sensitive data
│   └── Google Drive Backup
│       └── Mitigation: User's Google account security
└── Social Engineering
    ├── Phishing for Project Names
    │   └── Mitigation: User education
    └── Insider Threat
        └── Mitigation: Audit logging, RBAC

Risk Score: LOW-MEDIUM (depends on device security posture)
```

---

## Appendix B: Code Review Checklist

**For Project Management Implementation:**
- [ ] All text inputs validated with regex before storage
- [ ] Maximum field lengths enforced (200 chars for project name, etc.)
- [ ] Special characters sanitized or escaped
- [ ] Date fields validated for ISO 8601 format
- [ ] kotlinx.serialization used instead of manual string parsing
- [ ] Try-catch blocks around deserialization with error handling
- [ ] Default/fallback values for corrupted data
- [ ] Unit tests for malicious input (SQL injection, XSS, buffer overflow)
- [ ] Integration tests for data persistence and retrieval
- [ ] GDPR data export function implemented
- [ ] GDPR data anonymization function implemented
- [ ] Privacy policy updated with project data collection notice
- [ ] 30-year OSHA retention period documented
- [ ] Role-based access control implemented (if multi-user)
- [ ] Audit logging for project creation/modification
- [ ] Error messages don't leak sensitive information
- [ ] No hardcoded project data in source code

**For Settings Persistence:**
- [ ] SharedPreferences uses MODE_PRIVATE
- [ ] Input validation for all stored values
- [ ] Enum validation with safe fallbacks
- [ ] Reset to defaults functionality implemented
- [ ] No sensitive data (API keys, tokens) in regular SharedPreferences
- [ ] Backup rules configured appropriately
- [ ] Data export function for GDPR compliance
- [ ] Unit tests for persistence and retrieval
- [ ] Concurrent access handled safely

**For UI Components:**
- [ ] Tap-to-focus indicator has no data persistence
- [ ] Blur effects use system RenderEffect (Android S+)
- [ ] Accessibility fallbacks for blur (high contrast mode)
- [ ] No performance degradation from excessive blur
- [ ] UI preferences don't affect safety documentation integrity

---

**END OF SECURITY ASSESSMENT**
