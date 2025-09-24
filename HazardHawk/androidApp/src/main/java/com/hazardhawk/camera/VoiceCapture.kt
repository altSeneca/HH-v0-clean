package com.hazardhawk.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*

/**
 * Voice recognition states for construction workers
 */
enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    COMMAND_RECOGNIZED,
    ERROR
}

/**
 * Voice command data
 */
data class VoiceCommand(
    val text: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Voice recognition service for hands-free camera operation
 * Designed for construction workers who may have their hands full
 */
class VoiceCapture(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : RecognitionListener {
    
    private val speechRecognizer: SpeechRecognizer? = if (SpeechRecognizer.isRecognitionAvailable(context)) {
        SpeechRecognizer.createSpeechRecognizer(context)
    } else {
        null
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    private val _lastCommand = MutableStateFlow<VoiceCommand?>(null)
    val lastCommand: StateFlow<VoiceCommand?> = _lastCommand.asStateFlow()
    
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var listeningJob: Job? = null
    private var isInitialized = false
    
    // Voice commands for photo capture (construction-friendly)
    private val captureCommands = setOf(
        "capture", "photo", "picture", "shoot", "take", "snap",
        "document", "record", "safety", "hazard", "evidence"
    )
    
    // Camera mode commands
    private val modeCommands = mapOf(
        "single" to CameraMode.SINGLE_SHOT,
        "burst" to CameraMode.BURST_MODE,
        "timer" to CameraMode.TIMER_MODE,
        "hdr" to CameraMode.HDR_MODE
    )
    
    // Callback for voice commands
    var onVoiceCommand: ((String) -> Unit)? = null
    var onModeCommand: ((CameraMode) -> Unit)? = null
    
    companion object {
        private const val TAG = "VoiceCapture"
        private const val LISTENING_TIMEOUT_MS = 10000L // 10 seconds
        private const val MIN_CONFIDENCE = 0.6f
    }
    
    init {
        initialize()
    }
    
    /**
     * Initialize voice recognition
     */
    private fun initialize() {
        if (speechRecognizer == null) {
            _errorMessage.value = "Speech recognition not available on this device"
            return
        }
        
        try {
            speechRecognizer.setRecognitionListener(this)
            isInitialized = true
            Log.d(TAG, "Voice capture initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize voice capture", e)
            _errorMessage.value = "Failed to initialize voice recognition: ${e.message}"
        }
    }
    
    /**
     * Check if microphone permissions are granted
     */
    fun hasPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Start continuous voice listening
     */
    fun startListening() {
        if (!isInitialized || !hasPermissions()) {
            _errorMessage.value = "Voice recognition not available or permissions missing"
            return
        }
        
        if (_isEnabled.value) {
            stopListening()
        }
        
        _isEnabled.value = true
        _errorMessage.value = null
        
        startVoiceRecognition()
        
        Log.d(TAG, "Started continuous voice listening")
    }
    
    /**
     * Stop voice listening
     */
    fun stopListening() {
        _isEnabled.value = false
        listeningJob?.cancel()
        
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping speech recognizer", e)
        }
        
        _voiceState.value = VoiceState.IDLE
        Log.d(TAG, "Stopped voice listening")
    }
    
    /**
     * Start voice recognition session
     */
    private fun startVoiceRecognition() {
        if (!_isEnabled.value || speechRecognizer == null) return
        
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                
                // Optimize for construction environment
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            }
            
            _voiceState.value = VoiceState.LISTENING
            speechRecognizer.startListening(intent)
            
            // Set timeout for listening session
            listeningJob = coroutineScope.launch {
                delay(LISTENING_TIMEOUT_MS)
                if (_voiceState.value == VoiceState.LISTENING) {
                    restartListening()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition", e)
            _voiceState.value = VoiceState.ERROR
            _errorMessage.value = "Voice recognition error: ${e.message}"
            
            // Retry after a delay
            coroutineScope.launch {
                delay(2000)
                if (_isEnabled.value) {
                    startVoiceRecognition()
                }
            }
        }
    }
    
    /**
     * Restart listening session (for continuous recognition)
     */
    private fun restartListening() {
        if (!_isEnabled.value) return
        
        coroutineScope.launch {
            delay(500) // Brief pause between sessions
            if (_isEnabled.value) {
                startVoiceRecognition()
            }
        }
    }
    
    /**
     * Process recognized speech for camera commands
     */
    private fun processVoiceCommand(text: String, confidence: Float) {
        val normalizedText = text.lowercase(Locale.getDefault()).trim()
        
        Log.d(TAG, "Processing voice command: '$normalizedText' (confidence: $confidence)")
        
        if (confidence < MIN_CONFIDENCE) {
            Log.d(TAG, "Command confidence too low, ignoring")
            return
        }
        
        val command = VoiceCommand(normalizedText, confidence)
        _lastCommand.value = command
        
        // Check for capture commands
        if (captureCommands.any { keyword -> normalizedText.contains(keyword) }) {
            _voiceState.value = VoiceState.COMMAND_RECOGNIZED
            onVoiceCommand?.invoke(normalizedText)
            
            Log.d(TAG, "Capture command recognized: '$normalizedText'")
            
            // Brief feedback pause before resuming listening
            coroutineScope.launch {
                delay(1000)
                if (_isEnabled.value) {
                    _voiceState.value = VoiceState.IDLE
                    startVoiceRecognition()
                }
            }
            return
        }
        
        // Check for mode change commands
        modeCommands.entries.forEach { (keyword, mode) ->
            if (normalizedText.contains(keyword)) {
                onModeCommand?.invoke(mode)
                Log.d(TAG, "Mode command recognized: '$normalizedText' -> $mode")
                
                coroutineScope.launch {
                    delay(1000)
                    if (_isEnabled.value) {
                        _voiceState.value = VoiceState.IDLE
                        startVoiceRecognition()
                    }
                }
                return
            }
        }
        
        // No relevant command found, continue listening
        Log.d(TAG, "No relevant command found in: '$normalizedText'")
    }
    
    /**
     * Test voice recognition (for debugging)
     */
    fun testVoiceRecognition() {
        if (!hasPermissions()) {
            _errorMessage.value = "Microphone permission required for voice commands"
            return
        }
        
        coroutineScope.launch {
            _voiceState.value = VoiceState.LISTENING
            delay(3000) // Simulate listening
            
            // Simulate command recognition
            processVoiceCommand("capture photo", 0.9f)
        }
    }
    
    // RecognitionListener implementation
    
    override fun onReadyForSpeech(params: Bundle?) {
        _voiceState.value = VoiceState.LISTENING
        Log.d(TAG, "Ready for speech")
    }
    
    override fun onBeginningOfSpeech() {
        _voiceState.value = VoiceState.LISTENING
        Log.d(TAG, "Beginning of speech detected")
    }
    
    override fun onRmsChanged(rmsdB: Float) {
        // Could be used for visual feedback of microphone level
    }
    
    override fun onBufferReceived(buffer: ByteArray?) {
        // Audio buffer received
    }
    
    override fun onEndOfSpeech() {
        _voiceState.value = VoiceState.PROCESSING
        Log.d(TAG, "End of speech")
    }
    
    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout"
            else -> "Unknown error: $error"
        }
        
        Log.w(TAG, "Speech recognition error: $errorMessage")
        
        // Don't treat "no match" as a real error in continuous mode
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            if (_isEnabled.value) {
                restartListening()
            }
        } else {
            _voiceState.value = VoiceState.ERROR
            _errorMessage.value = errorMessage
            
            // Try to restart after other errors
            coroutineScope.launch {
                delay(2000)
                if (_isEnabled.value) {
                    _voiceState.value = VoiceState.IDLE
                    startVoiceRecognition()
                }
            }
        }
    }
    
    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        
        if (!matches.isNullOrEmpty()) {
            val bestMatch = matches[0]
            val confidence = confidences?.get(0) ?: 1.0f
            
            Log.d(TAG, "Speech results: $matches")
            processVoiceCommand(bestMatch, confidence)
        } else {
            // No results, restart listening
            if (_isEnabled.value) {
                restartListening()
            }
        }
    }
    
    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            Log.d(TAG, "Partial results: ${matches[0]}")
        }
    }
    
    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "Speech event: $eventType")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopListening()
        listeningJob?.cancel()
        
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying speech recognizer", e)
        }
        
        Log.d(TAG, "Voice capture cleaned up")
    }
    
    /**
     * Get supported voice commands for user guidance
     */
    fun getSupportedCommands(): List<String> {
        return listOf(
            "Capture", "Photo", "Picture", "Take", "Shoot", "Snap",
            "Document", "Record", "Safety", "Hazard", "Evidence",
            "Single mode", "Burst mode", "Timer mode", "HDR mode"
        )
    }
    
    /**
     * Check if device supports voice recognition
     */
    fun isVoiceRecognitionSupported(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }
}