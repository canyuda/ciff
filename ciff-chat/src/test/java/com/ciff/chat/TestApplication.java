package com.ciff.chat;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test configuration for ciff-chat module @WebMvcTest slice tests.
 * Only used as @SpringBootConfiguration anchor — not a real application.
 */
@SpringBootApplication(scanBasePackages = "com.ciff.chat")
class TestApplication {
}
