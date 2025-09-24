package com.hazardhawk.core.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.datetime.Instant
import kotlinx.datetime.serializers.InstantSerializer

/**
 * Centralized JSON configuration for the entire application
 * Ensures consistent serialization behavior across all modules
 */
object JsonConfig {
    
    /**
     * Primary JSON configuration for API communication
     */
    val apiJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        isLenient = true
        allowSpecialFloatingPointValues = true
        allowStructuredMapKeys = true
        prettyPrint = false
        useAlternativeNames = true
        
        serializersModule = SerializersModule {
            contextual(InstantSerializer)
            contextual(LocationSerializer)
            contextual(BoundingBoxSerializer)
        }
    }
    
    /**
     * JSON configuration for database storage
     * More strict for data integrity
     */
    val databaseJson = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
        isLenient = false
        allowSpecialFloatingPointValues = false
        
        serializersModule = SerializersModule {
            contextual(InstantSerializer)
            contextual(LocationSerializer)
            contextual(BoundingBoxSerializer)
        }
    }
    
    /**
     * JSON configuration for debug/logging
     */
    val debugJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        
        serializersModule = SerializersModule {
            contextual(InstantSerializer)
        }
    }
}

/**
 * Common serialization utilities
 */
object SerializationUtils {
    
    /**
     * Safe JSON encoding with error handling
     */
    inline fun <reified T> safeEncode(value: T, json: Json = JsonConfig.apiJson): Result<String> {
        return try {
            Result.success(json.encodeToString(value))
        } catch (e: Exception) {
            Result.failure(SerializationException("Failed to encode ${T::class.simpleName}", e))
        }
    }
    
    /**
     * Safe JSON decoding with error handling
     */
    inline fun <reified T> safeDecode(jsonString: String, json: Json = JsonConfig.apiJson): Result<T> {
        return try {
            Result.success(json.decodeFromString<T>(jsonString))
        } catch (e: Exception) {
            Result.failure(SerializationException("Failed to decode ${T::class.simpleName}", e))
        }
    }
    
    /**
     * Convert map to JSON string safely
     */
    fun mapToJson(map: Map<String, Any>): String {
        return try {
            JsonConfig.databaseJson.encodeToString(map)
        } catch (e: Exception) {
            "{}"
        }
    }
    
    /**
     * Parse JSON string to map safely
     */
    fun jsonToMap(json: String): Map<String, Any> {
        return try {
            JsonConfig.databaseJson.decodeFromString(json)
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Convert list to JSON array string
     */
    inline fun <reified T> listToJson(list: List<T>): String {
        return try {
            JsonConfig.databaseJson.encodeToString(list)
        } catch (e: Exception) {
            "[]"
        }
    }
    
    /**
     * Parse JSON array string to list
     */
    inline fun <reified T> jsonToList(json: String): List<T> {
        return try {
            JsonConfig.databaseJson.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Custom serialization exception
 */
class SerializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Custom serializers for complex types
 */
class LocationSerializer : kotlinx.serialization.KSerializer<com.hazardhawk.core.models.Location> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("Location") {
        element("latitude", kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("latitude", kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE))
        element("longitude", kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("longitude", kotlinx.serialization.descriptors.PrimitiveKind.DOUBLE))
        element("accuracy", kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("accuracy", kotlinx.serialization.descriptors.PrimitiveKind.FLOAT), isOptional = true)
        element("address", kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("address", kotlinx.serialization.descriptors.PrimitiveKind.STRING), isOptional = true)
    }
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: com.hazardhawk.core.models.Location) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeDoubleElement(descriptor, 0, value.latitude)
        composite.encodeDoubleElement(descriptor, 1, value.longitude)
        value.accuracy?.let { composite.encodeFloatElement(descriptor, 2, it) }
        value.address?.let { composite.encodeStringElement(descriptor, 3, it) }
        composite.endStructure(descriptor)
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): com.hazardhawk.core.models.Location {
        val composite = decoder.beginStructure(descriptor)
        var latitude = 0.0
        var longitude = 0.0
        var accuracy: Float? = null
        var address: String? = null
        
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                0 -> latitude = composite.decodeDoubleElement(descriptor, 0)
                1 -> longitude = composite.decodeDoubleElement(descriptor, 1)
                2 -> accuracy = composite.decodeFloatElement(descriptor, 2)
                3 -> address = composite.decodeStringElement(descriptor, 3)
                kotlinx.serialization.encoding.CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        composite.endStructure(descriptor)
        
        return com.hazardhawk.core.models.Location(latitude, longitude, accuracy, address)
    }
}

class BoundingBoxSerializer : kotlinx.serialization.KSerializer<com.hazardhawk.core.models.BoundingBox> {
    override val descriptor = kotlinx.serialization.descriptors.buildClassSerialDescriptor("BoundingBox") {
        element("left", kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("left", kotlinx.serialization.descriptors.PrimitiveKind.FLOAT))
        element("top", kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("top", kotlinx.serialization.descriptors.PrimitiveKind.FLOAT))
        element("width", kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("width", kotlinx.serialization.descriptors.PrimitiveKind.FLOAT))
        element("height", kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("height", kotlinx.serialization.descriptors.PrimitiveKind.FLOAT))
    }
    
    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: com.hazardhawk.core.models.BoundingBox) {
        val composite = encoder.beginStructure(descriptor)
        composite.encodeFloatElement(descriptor, 0, value.left)
        composite.encodeFloatElement(descriptor, 1, value.top)
        composite.encodeFloatElement(descriptor, 2, value.width)
        composite.encodeFloatElement(descriptor, 3, value.height)
        composite.endStructure(descriptor)
    }
    
    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): com.hazardhawk.core.models.BoundingBox {
        val composite = decoder.beginStructure(descriptor)
        var left = 0f
        var top = 0f
        var width = 0f
        var height = 0f
        
        while (true) {
            when (val index = composite.decodeElementIndex(descriptor)) {
                0 -> left = composite.decodeFloatElement(descriptor, 0)
                1 -> top = composite.decodeFloatElement(descriptor, 1)
                2 -> width = composite.decodeFloatElement(descriptor, 2)
                3 -> height = composite.decodeFloatElement(descriptor, 3)
                kotlinx.serialization.encoding.CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        composite.endStructure(descriptor)
        
        return com.hazardhawk.core.models.BoundingBox(left, top, width, height)
    }
}
