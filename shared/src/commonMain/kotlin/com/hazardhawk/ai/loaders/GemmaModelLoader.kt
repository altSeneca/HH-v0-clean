package com.hazardhawk.ai.loaders

/**
 * Cross-platform model loader interface for Gemma 3N E2B ONNX models.
 * Platform-specific implementations will handle actual ONNX Runtime integration.
 */
expect class GemmaModelLoader() {
    
    /**
     * Check if ONNX models are available on this platform.
     */
    val isAvailable: Boolean
    
    /**
     * Load the Gemma 3N E2B models from the specified paths.
     * 
     * @param visionEncoderPath Path to vision encoder ONNX model
     * @param textDecoderPath Path to text decoder ONNX model  
     * @param tokenizerPath Path to tokenizer JSON file
     * @param configPath Path to model configuration JSON
     * @return True if models loaded successfully
     */
    suspend fun loadModels(
        visionEncoderPath: String,
        textDecoderPath: String,
        tokenizerPath: String,
        configPath: String
    ): Boolean
    
    /**
     * Encode an image into feature vectors using the vision encoder.
     * 
     * @param imageData Raw image bytes
     * @return Image feature vectors or null if encoding fails
     */
    suspend fun encodeImage(imageData: ByteArray): FloatArray?
    
    /**
     * Generate text using the multimodal decoder with image context.
     * 
     * @param prompt Text prompt for analysis
     * @param imageContext Image features from vision encoder
     * @param maxTokens Maximum tokens to generate
     * @param temperature Sampling temperature (0.0-1.0)
     * @return Generated text or null if generation fails
     */
    suspend fun generateText(
        prompt: String,
        imageContext: FloatArray,
        maxTokens: Int = 512,
        temperature: Float = 0.7f
    ): String?
    
    /**
     * Tokenize text input for the model.
     * 
     * @param text Input text to tokenize
     * @return Token IDs array
     */
    suspend fun tokenize(text: String): IntArray?
    
    /**
     * Detokenize model output back to text.
     * 
     * @param tokens Token IDs to convert to text
     * @return Decoded text string
     */
    suspend fun detokenize(tokens: IntArray): String?
    
    /**
     * Release model resources and cleanup.
     */
    suspend fun cleanup()
    
    /**
     * Get model information and status.
     */
    suspend fun getModelInfo(): GemmaModelInfo
}

/**
 * Model information and configuration.
 */
data class GemmaModelInfo(
    val visionEncoderLoaded: Boolean,
    val textDecoderLoaded: Boolean,
    val tokenizerLoaded: Boolean,
    val configLoaded: Boolean,
    val memoryUsageMB: Float,
    val modelVersion: String,
    val supportedFeatures: Set<String>
)