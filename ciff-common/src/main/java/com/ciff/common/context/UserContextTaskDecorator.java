package com.ciff.common.context;

import org.springframework.core.task.TaskDecorator;

/**
 * Propagates UserContext userId from the submitting thread to the executing thread.
 * Acts as a safety net; prefer explicit userId parameter passing when possible.
 */
public class UserContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Long userId = UserContext.getUserId();
        return () -> {
            try {
                if (userId != null) {
                    UserContext.setUserId(userId);
                }
                runnable.run();
            } finally {
                UserContext.clear();
            }
        };
    }
}
