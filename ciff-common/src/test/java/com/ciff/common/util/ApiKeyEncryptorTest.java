package com.ciff.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyEncryptorTest {

    private static final String SECRET = "test-secret-1234"; // 16 chars

    private ApiKeyEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new ApiKeyEncryptor(SECRET);
    }

    @Test
    void encrypt_and_decrypt_roundtrip() {
        String original = "sk-proj-abc123xyz456";
        String encrypted = encryptor.encrypt(original);

        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = encryptor.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    void encrypt_null_returns_empty() {
        assertEquals("", encryptor.encrypt(null));
    }

    @Test
    void encrypt_empty_returns_empty() {
        assertEquals("", encryptor.encrypt(""));
    }

    @Test
    void decrypt_null_returns_empty() {
        assertEquals("", encryptor.decrypt(null));
    }

    @Test
    void decrypt_empty_returns_empty() {
        assertEquals("", encryptor.decrypt(""));
    }

    @Test
    void encrypt_produces_different_ciphertext_each_time() {
        String plaintext = "sk-same-key";
        String encrypted1 = encryptor.encrypt(plaintext);
        String encrypted2 = encryptor.encrypt(plaintext);

        assertNotEquals(encrypted1, encrypted2, "Random IV should produce different ciphertext");
        assertEquals(encryptor.decrypt(encrypted1), encryptor.decrypt(encrypted2));
    }

    @Test
    void decrypt_with_invalid_data_throws() {
        assertThrows(IllegalStateException.class, () -> encryptor.decrypt("not-valid-base64!!!"));
    }

    @Test
    void decrypt_with_truncated_data_throws() {
        String tooShort = java.util.Base64.getEncoder()
                .encodeToString(new byte[]{1, 2, 3}); // only 3 bytes, less than IV_LENGTH
        assertThrows(IllegalStateException.class, () -> encryptor.decrypt(tooShort));
    }

    @Test
    void encrypt_long_api_key() {
        String longKey = "sk-proj-" + "a".repeat(200);
        String encrypted = encryptor.encrypt(longKey);
        assertEquals(longKey, encryptor.decrypt(encrypted));
    }
}
