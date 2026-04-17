package com.ciff.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserContextTest {

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    void setAndGetUserId() {
        assertNull(UserContext.getUserId());

        UserContext.setUserId(42L);
        assertEquals(42L, UserContext.getUserId());
    }

    @Test
    void clearRemovesUserId() {
        UserContext.setUserId(1L);
        UserContext.clear();
        assertNull(UserContext.getUserId());
    }

    @Test
    void clearWhenEmptyIsSafe() {
        assertDoesNotThrow(UserContext::clear);
    }
}
