package com.hazardhawk.ui.glass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hazardhawk.ui.glass.components.*

/**
 * Comprehensive example demonstrating glass morphism components
 * optimized for construction environments with HazardHawk app.
 */
@Composable
fun GlassComponentsExample() {
    val context = LocalContext.current
    val glassState = remember { GlassState.getInstance(context) }
    val performanceMonitor = rememberGlassPerformanceMonitor(context)
    val environmentAdapter = rememberConstructionEnvironmentAdapter(context)
    
    var emergencyMode by remember { mutableStateOf(false) }
    var showPerformanceMetrics by remember { mutableStateOf(false) }
    var showEnvironmentInfo by remember { mutableStateOf(false) }
    var selectedCardType by remember { mutableStateOf(ConstructionCardType.STANDARD) }
    var selectedButtonType by remember { mutableStateOf(ConstructionButtonType.PRIMARY) }
    
    // Start performance monitoring
    LaunchedEffect(Unit) {
        performanceMonitor.startMonitoring()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            performanceMonitor.stopMonitoring()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Background for glass effects to blur against
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A), // Construction blue
                            Color(0xFF059669), // Safety green
                            Color(0xFFFF6B35)  // Safety orange
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Section
            ConstructionGlassCard(
                cardType = ConstructionCardType.STANDARD,
                emergencyMode = emergencyMode
            ) {
                Text(
                    text = "HazardHawk Glass Components",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Construction-optimized glass morphism UI components with safety-focused design",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Control Panel
            ConstructionGlassCard(
                cardType = ConstructionCardType.OUTDOOR,
                emergencyMode = emergencyMode
            ) {
                Text(
                    text = "Control Panel",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ConstructionGlassButton(
                        onClick = { emergencyMode = !emergencyMode },
                        buttonType = if (emergencyMode) ConstructionButtonType.EMERGENCY 
                                   else ConstructionButtonType.SECONDARY,
                        emergencyMode = emergencyMode
                    ) {
                        Icon(
                            imageVector = if (emergencyMode) Icons.Filled.Warning else Icons.Filled.Security,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (emergencyMode) "Exit Emergency" else "Emergency Mode")
                    }
                    
                    ConstructionGlassButton(
                        onClick = { showPerformanceMetrics = !showPerformanceMetrics },
                        buttonType = ConstructionButtonType.STANDARD
                    ) {
                        Icon(imageVector = Icons.Filled.Analytics, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Metrics")
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ConstructionGlassButton(
                        onClick = { showEnvironmentInfo = !showEnvironmentInfo },
                        buttonType = ConstructionButtonType.STANDARD
                    ) {
                        Icon(imageVector = Icons.Filled.Thermostat, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Environment")
                    }
                    
                    ConstructionGlassButton(
                        onClick = { 
                            // Simulate manual glove detection toggle
                            val currentEnv = environmentAdapter.currentEnvironment.value
                            environmentAdapter.setGloveWearing(
                                !currentEnv.isWearingGloves,
                                if (!currentEnv.isWearingGloves) 5 else 0
                            )
                        },
                        buttonType = ConstructionButtonType.STANDARD
                    ) {
                        Icon(imageVector = Icons.Filled.PanTool, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Toggle Gloves")
                    }
                }
            }
            
            // Glass Card Examples
            Text(
                text = "Glass Card Examples",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    ConstructionCardType.STANDARD to "Standard",
                    ConstructionCardType.CRITICAL to "Critical", 
                    ConstructionCardType.OUTDOOR to "Outdoor"
                ).forEach { (type, label) ->
                    ConstructionGlassButton(
                        onClick = { selectedCardType = type },
                        buttonType = if (selectedCardType == type) ConstructionButtonType.PRIMARY 
                                   else ConstructionButtonType.SECONDARY,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(label)
                    }
                    
                    if (type != ConstructionCardType.OUTDOOR) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
            
            ConstructionGlassCard(
                cardType = selectedCardType,
                emergencyMode = emergencyMode
            ) {
                Text(
                    text = "${selectedCardType.name.lowercase().replaceFirstChar { it.uppercase() }} Card Example",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Text(
                    text = when (selectedCardType) {
                        ConstructionCardType.STANDARD -> "Standard construction card with safety orange accents and professional styling."
                        ConstructionCardType.CRITICAL -> "Critical information card with high contrast and emergency styling."
                        ConstructionCardType.OUTDOOR -> "Outdoor-optimized card with enhanced visibility for bright environments."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    SafetyGlassButton(
                        onClick = { /* Action */ },
                        safetyLevel = when (selectedCardType) {
                            ConstructionCardType.STANDARD -> SafetyButtonLevel.STANDARD
                            ConstructionCardType.CRITICAL -> SafetyButtonLevel.CRITICAL
                            ConstructionCardType.OUTDOOR -> SafetyButtonLevel.WARNING
                        },
                        emergencyMode = emergencyMode,
                        accessibilityLabel = "Example action button"
                    ) {
                        Icon(imageVector = Icons.Filled.Done, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Action")
                    }
                }
            }
            
            // Safety Glass Cards
            Text(
                text = "Safety-Focused Cards",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SafetyGlassCard(
                    safetyLevel = SafetyLevel.WARNING,
                    emergencyMode = emergencyMode,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFB020),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Caution",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Wear PPE",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                
                SafetyGlassCard(
                    safetyLevel = SafetyLevel.CRITICAL,
                    emergencyMode = emergencyMode,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Dangerous,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Danger",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Keep Out",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            
            // Glass Overlay Example
            Text(
                text = "Camera Glass Overlay Example",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Simulated camera viewfinder background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
                
                CameraGlassOverlay(
                    cameraMode = CameraOverlayMode.PHOTO,
                    showViewfinderMask = true,
                    emergencyMode = emergencyMode
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        
                        Text(
                            text = "Camera Viewfinder Overlay",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "With safety orange corner indicators",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Performance and Environment Monitoring
            if (showPerformanceMetrics) {
                GlassPerformanceDisplay(
                    monitor = performanceMonitor,
                    visible = true
                )
            }
            
            if (showEnvironmentInfo) {
                ConstructionEnvironmentDisplay(
                    adapter = environmentAdapter,
                    visible = true
                )
            }
            
            // Additional Space for scrolling
            Spacer(modifier = Modifier.height(100.dp))
        }
        
        // Bottom Bar Example
        ConstructionGlassBottomBar(
            barType = if (emergencyMode) ConstructionBarType.EMERGENCY 
                     else ConstructionBarType.STANDARD,
            cameraOverlayMode = false,
            emergencyMode = emergencyMode,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            listOf(
                Icons.Filled.Home to "Home",
                Icons.Filled.CameraAlt to "Camera", 
                Icons.Filled.Report to "Reports",
                Icons.Filled.Settings to "Settings"
            ).forEach { (icon, label) ->
                ConstructionGlassButton(
                    onClick = { /* Navigate */ },
                    buttonType = ConstructionButtonType.SECONDARY,
                    emergencyMode = emergencyMode,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Usage example for camera interface with glass components
 */
@Composable
fun CameraInterfaceExample() {
    var isRecording by remember { mutableStateOf(false) }
    var emergencyMode by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Simulated camera preview background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        
        // Camera overlay with glass effects
        CameraGlassOverlay(
            cameraMode = if (isRecording) CameraOverlayMode.VIDEO else CameraOverlayMode.PHOTO,
            showViewfinderMask = true,
            emergencyMode = emergencyMode
        ) {
            // Camera UI overlay content
        }
        
        // Camera bottom bar
        CameraGlassBottomBar(
            cameraMode = if (isRecording) CameraMode.VIDEO else CameraMode.PHOTO,
            emergencyMode = emergencyMode,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            // Camera shutter button
            ConstructionGlassButton(
                onClick = { isRecording = !isRecording },
                buttonType = if (isRecording) ConstructionButtonType.EMERGENCY 
                           else ConstructionButtonType.PRIMARY,
                emergencyMode = emergencyMode,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.CameraAlt,
                    contentDescription = if (isRecording) "Stop recording" else "Take photo",
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Gallery button
            ConstructionGlassButton(
                onClick = { /* Open gallery */ },
                buttonType = ConstructionButtonType.SECONDARY,
                emergencyMode = emergencyMode
            ) {
                Icon(
                    imageVector = Icons.Filled.Photo,
                    contentDescription = "Gallery",
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Settings button  
            ConstructionGlassButton(
                onClick = { /* Open settings */ },
                buttonType = ConstructionButtonType.SECONDARY,
                emergencyMode = emergencyMode
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Emergency mode toggle (for demo purposes)
        ConstructionGlassButton(
            onClick = { emergencyMode = !emergencyMode },
            buttonType = if (emergencyMode) ConstructionButtonType.EMERGENCY else ConstructionButtonType.STANDARD,
            emergencyMode = emergencyMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (emergencyMode) Icons.Filled.Warning else Icons.Filled.Security,
                contentDescription = if (emergencyMode) "Exit emergency mode" else "Enter emergency mode"
            )
        }
    }
}