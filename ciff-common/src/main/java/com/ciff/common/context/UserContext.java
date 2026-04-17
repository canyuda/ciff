package com.ciff.common.context;

/**
 * ThreadLocal-based user context.
 * Phase 6 JWT interceptor will replace the X-User-Id header source;
 * business layer reads from here and needs no change.
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
