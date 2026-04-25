package com.ciff.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {

    @Test
    void encode_producesBcryptHash() {
        String hash = PasswordUtil.encode("password123");

        assertThat(hash).isNotNull();
        assertThat(hash).startsWith("$2a$");
        assertThat(hash).hasSize(60);
    }

    @Test
    void matches_returnsTrueForCorrectPassword() {
        String hash = PasswordUtil.encode("password123");

        assertThat(PasswordUtil.matches("password123", hash)).isTrue();
    }

    @Test
    void matches_returnsFalseForWrongPassword() {
        String hash = PasswordUtil.encode("password123");

        assertThat(PasswordUtil.matches("wrongPassword", hash)).isFalse();
    }

    @Test
    void encode_producesDifferentHashesDueToSalt() {
        String hash1 = PasswordUtil.encode("samePassword");
        String hash2 = PasswordUtil.encode("samePassword");

        // different salts -> different hashes
        assertThat(hash1).isNotEqualTo(hash2);
        // but both match the same raw password
        assertThat(PasswordUtil.matches("samePassword", hash1)).isTrue();
        assertThat(PasswordUtil.matches("samePassword", hash2)).isTrue();
    }
}
