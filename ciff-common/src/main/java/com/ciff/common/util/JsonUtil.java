package com.ciff.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;

/**
 * JSON utility class.
 * Encapsulates Jackson ObjectMapper operations, business code should not use ObjectMapper directly.
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private static final ObjectMapper SNAKE_CASE_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private JsonUtil() {
    }

    // ==================== Serialize ====================

    /**
     * Serialize object to JSON string.
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to serialize object to JSON", e);
        }
    }

    /**
     * Serialize object to JSON string with snake_case naming strategy.
     */
    public static String toJsonSnakeCase(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return SNAKE_CASE_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to serialize object to JSON (snake_case)", e);
        }
    }

    // ==================== Deserialize ====================

    /**
     * Deserialize JSON string to object.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Deserialize JSON string to object with snake_case naming strategy.
     */
    public static <T> T fromJsonSnakeCase(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return SNAKE_CASE_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to " + clazz.getSimpleName() + " (snake_case)", e);
        }
    }

    /**
     * Deserialize JSON string to generic type (e.g., List, Map).
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to " + typeRef.getType(), e);
        }
    }

    /**
     * Deserialize JSON string to generic type with snake_case naming strategy.
     */
    public static <T> T fromJsonSnakeCase(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return SNAKE_CASE_MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to " + typeRef.getType() + " (snake_case)", e);
        }
    }

    // ==================== Convert to Map/List ====================

    /**
     * Convert JSON string to Map.
     */
    public static Map<String, Object> toMap(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new JsonException("Failed to deserialize JSON to Map", e);
        }
    }

    /**
     * Convert object to Map.
     */
    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(obj, new TypeReference<>() {});
        } catch (Exception e) {
            throw new JsonException("Failed to convert object to Map", e);
        }
    }

    /**
     * Convert object to target type.
     */
    public static <T> T convert(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(obj, clazz);
        } catch (Exception e) {
            throw new JsonException("Failed to convert object to " + clazz.getSimpleName(), e);
        }
    }

    /**
     * Convert object to generic type.
     */
    public static <T> T convert(Object obj, TypeReference<T> typeRef) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.convertValue(obj, typeRef);
        } catch (Exception e) {
            throw new JsonException("Failed to convert object to " + typeRef.getType(), e);
        }
    }

    // ==================== Validation ====================

    /**
     * Check if the given string is valid JSON.
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.isEmpty()) {
            return false;
        }
        try {
            MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    // ==================== Exception ====================

    public static class JsonException extends RuntimeException {
        public JsonException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
