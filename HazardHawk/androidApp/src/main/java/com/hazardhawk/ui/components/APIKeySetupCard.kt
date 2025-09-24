package com.hazardhawk.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.hazardhawk.ui.theme.ConstructionColors

/**
 * API Key Setup Card for HazardHawk Gemini Vision Integration
 * 
 * Construction-optimized design with:
 * - QR code sharing for easy mobile setup
 * - Manual entry with validation
 * - Educational guidance and security best practices
 * - Large touch targets for safety compliance
 */

@Composable
fun APIKeySetupCard(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    isValid: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var setupMethod by remember { mutableStateOf(APIKeySetupMethod.MANUAL) }
    var showApiKey by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    
    // Validate API key format
    LaunchedEffect(apiKey) {
        validationMessage = validateAPIKey(apiKey)
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isValid) {
                ConstructionColors.SafetyGreen.copy(alpha = 0.05f)
            } else if (apiKey.isNotEmpty()) {
                ConstructionColors.CautionRed.copy(alpha = 0.05f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(
            width = 2.dp,
            color = when {
                isValid -> ConstructionColors.SafetyGreen
                apiKey.isNotEmpty() && !isValid -> ConstructionColors.CautionRed
                else -> ConstructionColors.SafetyOrange
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with toggle
            APIKeySetupHeader(
                isValid = isValid,
                expanded = expanded,
                onToggle = { onExpandedChange(!expanded) }
            )
            
            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Setup method selector
                    APIKeySetupMethodSelector(
                        currentMethod = setupMethod,
                        onMethodChange = { setupMethod = it }
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    when (setupMethod) {
                        APIKeySetupMethod.MANUAL -> {
                            ManualAPIKeyEntry(
                                apiKey = apiKey,
                                onApiKeyChange = onApiKeyChange,
                                showApiKey = showApiKey,
                                onToggleVisibility = { showApiKey = !showApiKey },
                                isValid = isValid,
                                validationMessage = validationMessage
                            )
                        }
                        APIKeySetupMethod.QR_CODE -> {
                            QRCodeAPIKeySharing(apiKey = apiKey)
                        }
                        APIKeySetupMethod.HELP -> {
                            APIKeyHelpSection()
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Security notice
                    APIKeySecurityNotice()
                }
            }
        }
    }
}

/**
 * Setup card header with status indicator
 */
@Composable
fun APIKeySetupHeader(
    isValid: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon
            Icon(
                imageVector = when {
                    isValid -> Icons.Default.CheckCircle
                    else -> Icons.Default.Key
                },
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = when {
                    isValid -> ConstructionColors.SafetyGreen
                    else -> ConstructionColors.SafetyOrange
                }
            )
            
            Column {
                Text(
                    text = "Google AI API Key Setup",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ConstructionColors.SafetyOrange
                )
                
                Text(
                    text = when {
                        isValid -> "Ready for cloud AI analysis"
                        else -> "Required for advanced AI features"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Expand/collapse icon
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier.size(32.dp),
            tint = ConstructionColors.SafetyOrange
        )
    }
}

/**
 * Setup method selector tabs
 */
@Composable
fun APIKeySetupMethodSelector(
    currentMethod: APIKeySetupMethod,
    onMethodChange: (APIKeySetupMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        APIKeySetupMethod.entries.forEach { method ->
            val isSelected = currentMethod == method
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) {
                            ConstructionColors.SafetyOrange
                        } else {
                            Color.Transparent
                        }
                    )
                    .clickable { onMethodChange(method) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = method.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = method.label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Manual API key entry form
 */
@Composable
fun ManualAPIKeyEntry(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleVisibility: () -> Unit,
    isValid: Boolean,
    validationMessage: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // API Key input field
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("Google AI API Key") },
            placeholder = { Text("AIzaSy... or AQ...") },
            leadingIcon = {
                Icon(
                    Icons.Default.Key,
                    contentDescription = "API Key",
                    tint = ConstructionColors.SafetyOrange
                )
            },
            trailingIcon = {
                Row {
                    // Paste button
                    IconButton(
                        onClick = {
                            val clipboardText = clipboardManager.getText()?.text
                            if (!clipboardText.isNullOrBlank()) {
                                onApiKeyChange(clipboardText.trim())
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            modifier = Modifier.size(20.dp),
                            tint = ConstructionColors.WorkZoneBlue
                        )
                    }
                    
                    // Visibility toggle
                    IconButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showApiKey) "Hide key" else "Show key",
                            modifier = Modifier.size(20.dp),
                            tint = ConstructionColors.SafetyOrange
                        )
                    }
                }
            },
            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
            isError = apiKey.isNotEmpty() && !isValid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Validation status
        if (apiKey.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isValid) ConstructionColors.SafetyGreen else ConstructionColors.CautionRed
                )
                
                Text(
                    text = if (isValid) "Valid API key format" else validationMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isValid) ConstructionColors.SafetyGreen else ConstructionColors.CautionRed
                )
            }
        }
        
        // Quick instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = ConstructionColors.WorkZoneBlue.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = ConstructionColors.WorkZoneBlue
                    )
                    
                    Text(
                        text = "Quick Setup",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ConstructionColors.WorkZoneBlue
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "For Gemini API: Visit console.cloud.google.com\nFor Vertex AI: Visit console.cloud.google.com/vertex-ai\n\n1. Create/select project\n2. Enable API service\n3. Create API key\n4. Paste key above",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * QR code sharing section
 */
@Composable
fun QRCodeAPIKeySharing(
    apiKey: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (apiKey.isNotEmpty()) {
            // Generate QR code for API key
            val qrBitmap by remember(apiKey) {
                derivedStateOf {
                    generateQRCode(
                        text = "hazardhawk-api:$apiKey",
                        size = 200
                    )
                }
            }
            
            qrBitmap?.let { bitmap ->
                Card(
                    modifier = Modifier.size(220.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "API Key QR Code",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                Text(
                    text = "Scan with another device to transfer API key",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Placeholder for when no API key is entered
            Card(
                modifier = Modifier.size(220.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                border = BorderStroke(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Enter API key\nto generate QR code",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // QR Code instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = ConstructionColors.SafetyGreen.copy(alpha = 0.08f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.QrCode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = ConstructionColors.SafetyGreen
                    )
                    
                    Text(
                        text = "Team Sharing",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = ConstructionColors.SafetyGreen
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Share API access with your team by scanning the QR code. Perfect for setting up multiple devices quickly and securely.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 18.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * API key help and setup guide
 */
@Composable
fun APIKeyHelpSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // What is an API Key section
        APIKeyHelpCard(
            title = "What is an API Key?",
            icon = Icons.Default.Key,
            content = "An API key is like a password that allows HazardHawk to access Google's advanced Gemini AI for analyzing construction photos. It enables more accurate hazard detection and detailed safety reports."
        )
        
        // Getting API Key section
        APIKeyHelpCard(
            title = "How to Get Your API Key",
            icon = Icons.Default.CloudDownload,
            content = "1. Visit console.cloud.google.com\n2. Create a Google Cloud account (free tier available)\n3. Create a new project for HazardHawk\n4. Enable the Gemini API\n5. Go to 'Credentials' and create an API key\n6. Copy the key starting with 'AIzaSy...'"
        )
        
        // Security section
        APIKeyHelpCard(
            title = "Security & Privacy",
            icon = Icons.Default.Security,
            content = "Your API key is stored locally on your device and never shared with third parties. Photos are analyzed securely through Google's infrastructure with enterprise-grade security."
        )
        
        // Cost information
        APIKeyHelpCard(
            title = "Usage & Costs",
            icon = Icons.Default.AttachMoney,
            content = "Google Gemini API offers generous free tier limits. Most construction teams stay within free limits. You can set spending limits and monitor usage in the Google Cloud Console."
        )
    }
}

/**
 * Individual help card component
 */
@Composable
fun APIKeyHelpCard(
    title: String,
    icon: ImageVector,
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = ConstructionColors.SafetyOrange
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = ConstructionColors.SafetyOrange
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Security notice at the bottom of the card
 */
@Composable
fun APIKeySecurityNotice(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ConstructionColors.HighVisYellow.copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = ConstructionColors.HighVisYellow.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = ConstructionColors.HighVisYellow
            )
            
            Text(
                text = "Your API key is encrypted and stored locally. Never share your key with unauthorized users.",
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * API Key Setup Methods
 */
enum class APIKeySetupMethod(
    val label: String,
    val icon: ImageVector
) {
    MANUAL("Manual", Icons.Default.Edit),
    QR_CODE("QR Code", Icons.Default.QrCode),
    HELP("Help", Icons.Default.Help)
}

/**
 * Validate API key format for both Gemini and Vertex AI
 */
fun validateAPIKey(apiKey: String): String {
    return try {
        when {
            apiKey.isEmpty() -> "API key is required"
            apiKey.startsWith("AIzaSy") -> {
                // Gemini API key validation
                when {
                    apiKey.length < 35 -> "Gemini API key is too short"
                    apiKey.length > 45 -> "Gemini API key is too long"
                    else -> ""
                }
            }
            apiKey.startsWith("AQ.") -> {
                // Vertex AI API key validation - currently not fully supported by Firebase
                // This key format is accepted but may need additional configuration
                when {
                    apiKey.length < 20 -> "Vertex AI API key is too short"
                    apiKey.length > 100 -> "Vertex AI API key is too long"
                    !apiKey.contains('.') -> "Invalid Vertex AI API key format"
                    else -> "Vertex AI key format detected (experimental support)"
                }
            }
            else -> "Invalid API key format. Should start with 'AIzaSy' (Gemini) or 'AQ.' (Vertex AI)"
        }
    } catch (e: Exception) {
        "Error validating API key: ${e.message}"
    }
}

/**
 * Legacy function for backward compatibility
 */
@Deprecated("Use validateAPIKey instead", ReplaceWith("validateAPIKey(apiKey)"))
fun validateGeminiAPIKey(apiKey: String): String = validateAPIKey(apiKey)

/**
 * Generate QR code bitmap
 */
fun generateQRCode(text: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.MARGIN to 0
        )
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
