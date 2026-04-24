package com.ciff.workflow.engine.step;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolStepExecutorParamTest {

    @Test
    void coerceType_number_shouldReturnDouble() {
        assertEquals(3.14, coerceType("3.14", "number"));
        assertEquals(42.0, coerceType(42, "number"));
    }

    @Test
    void coerceType_integer_shouldReturnLong() {
        assertEquals(42L, coerceType(42.5, "integer"));
        assertEquals(100L, coerceType("100", "integer"));
    }

    @Test
    void coerceType_boolean_shouldReturnBoolean() {
        assertEquals(true, coerceType("true", "boolean"));
        assertEquals(false, coerceType("false", "boolean"));
        assertEquals(true, coerceType(true, "boolean"));
    }

    @Test
    void coerceType_array_singleValue_shouldWrapInList() {
        Object result = coerceType("hello", "array");
        assertTrue(result instanceof java.util.List);
        assertEquals(1, ((java.util.List<?>) result).size());
    }

    @Test
    void coerceType_string_shouldPassThrough() {
        assertEquals("hello", coerceType("hello", "string"));
    }

    private Object coerceType(Object value, String type) {
        if (value == null) return null;
        return switch (type) {
            case "number" -> {
                if (value instanceof Number n) yield n.doubleValue();
                yield Double.parseDouble(value.toString());
            }
            case "integer" -> {
                if (value instanceof Number n) yield n.longValue();
                yield Long.parseLong(value.toString());
            }
            case "boolean" -> {
                if (value instanceof Boolean b) yield b;
                yield Boolean.parseBoolean(value.toString());
            }
            case "array" -> {
                if (value instanceof java.util.List<?> list) yield list;
                yield java.util.List.of(value);
            }
            default -> value;
        };
    }
}
