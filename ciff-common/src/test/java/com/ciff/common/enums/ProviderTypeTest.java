package com.ciff.common.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProviderTypeTest {

    @Test
    void allTypesHaveNonBlankFields() {
        for (ProviderType pt : ProviderType.values()) {
            assertFalse(pt.getType().isBlank(), pt.name() + " has blank type");
            assertFalse(pt.getDisplayName().isBlank(), pt.name() + " has blank displayName");
        }
    }

    @Test
    void typeValuesMatchFrontendList() {
        // Must match ciff-web providerTypes array
        String[] expected = {
            "openai", "claude", "gemini", "ollama",
            "deepseek", "qwen", "zhipu", "kimi",
            "wenxin", "doubao", "hunyuan", "yi", "minimax", "spark"
        };

        assertEquals(expected.length, ProviderType.values().length, "enum count mismatch");

        for (String type : expected) {
            boolean found = false;
            for (ProviderType pt : ProviderType.values()) {
                if (pt.getType().equals(type)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "missing enum for type: " + type);
        }
    }

    @Test
    void noDuplicateTypes() {
        long distinctCount = java.util.Arrays.stream(ProviderType.values())
                .map(ProviderType::getType)
                .distinct()
                .count();
        assertEquals(ProviderType.values().length, distinctCount);
    }
}
