package com.hazardhawk.camera

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.hazardhawk.data.PhotoStorageManagerCompat
import android.os.Handler
import android.os.Looper
import java.io.File

/**
 * Enhanced camera capture system - DISABLED
 * 
 * This module has been temporarily disabled due to threading and architecture issues.
 * The functionality has been moved to SafetyHUDCameraScreen for better integration.
 * 
 * TODO: Refactor and re-enable when threading issues are resolved
 */
